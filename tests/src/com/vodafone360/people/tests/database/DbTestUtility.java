/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone360/people/VODAFONE.LICENSE.txt or
 * http://github.com/360/360-Engine-for-Android
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at src/com/vodafone360/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2010 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.tests.database;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.ServiceStatus;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class DbTestUtility {
	final static int ACTIVITIES_INT_EVENT_MASK = 1;
	final static int ACTIVITIES_EXT_EVENT_MASK = 2;
	final static int CONTACTS_INT_EVENT_MASK = 4;
	final static int CONTACTS_EXT_EVENT_MASK = 8;
	final static int ME_PROFILE_INT_EVENT_MASK = 16;
	final static int ME_PROFILE_EXT_EVENT_MASK = 32;
	final static int ALL_INT_EVENTS_MASK  = 21;
	final static int ALL_EXT_EVENTS_MASK  = 42;
	final static int ALL_EVENTS_MASK = 63;

	int mEventStatusBitmap;
	Thread mEventWatcherThread = null;
	Handler mDbChangeEventHandler;
	Handler mStopEventThread;
	boolean mEventThreadStarted = false;
	DatabaseHelper mDatabase;
	
	DbTestUtility(Context context) {
		Intent serviceIntent = new Intent();
		serviceIntent.setClassName(RemoteService.class.getPackage().getName(), RemoteService.class.getName());
		context.stopService(serviceIntent);
	}
	
	synchronized public void startEventWatcher(DatabaseHelper db) {
		mEventWatcherThread = new Thread(new Runnable() {
			@Override
			public void run() {
				eventWatcherThreadMain();
			}
		});
		mEventWatcherThread.start();
		while (!mEventThreadStarted) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mDatabase = db;
		mDatabase.addEventCallback(mDbChangeEventHandler);
		clearAllEvents();
	}

	public void stopEventWatcher() {
		if (mDatabase != null) {
			mDatabase.removeEventCallback(mDbChangeEventHandler);
			mDatabase = null;
		}
		mStopEventThread.sendEmptyMessage(0);
	}

	private void eventWatcherThreadMain() {
		Looper.prepare();
		synchronized (this) {
			mDbChangeEventHandler = new Handler() {
				@Override 
				public void handleMessage(Message msg) {
					onDatabaseChangeEvent(msg);
				}
			};
			mStopEventThread = new Handler() {
				@Override 
				public void handleMessage(Message msg) {
					Looper looper = Looper.myLooper();
					if (looper != null) {
						looper.quit();
					}
				}
			};
			mEventThreadStarted = true;
			notifyAll();
		}
		Looper.loop();
	}

	private void onDatabaseChangeEvent(Message msg) {
		if (msg.arg1 >= DatabaseHelper.DatabaseChangeType.values().length) {
			// Unknown event
			return;
		}
		final DatabaseHelper.DatabaseChangeType type = DatabaseHelper.DatabaseChangeType.values()[msg.arg1];
		int eventStatus;
		
		switch (type) {
			case ACTIVITIES:
				eventStatus = ACTIVITIES_INT_EVENT_MASK;
				break;
			case CONTACTS:
				eventStatus = CONTACTS_INT_EVENT_MASK;
				break;
			case ME_PROFILE:
				eventStatus = ME_PROFILE_INT_EVENT_MASK;
				break;
			default:
				return;
		}

		if (msg.arg2 != 0) {
			eventStatus <<= 1;
		}
		synchronized (this) {
			mEventStatusBitmap |= eventStatus;
			notifyAll();
		}
	}

	public synchronized void clearAllEvents() {
		mEventStatusBitmap = 0;
	}

	public synchronized ServiceStatus waitForEvent(long timeout, int bitmask) {
		long timeToFinish = System.nanoTime() + (timeout * 1000000);
		long remainingTime = timeout;
		while ((mEventStatusBitmap & bitmask) != bitmask && remainingTime > 0) {
			try {
				wait(remainingTime);
			} catch (InterruptedException e) {
			}
			remainingTime = (System.nanoTime() - timeToFinish) / 1000000;
		}
		if ((mEventStatusBitmap & bitmask) != bitmask) {
			return ServiceStatus.ERROR_UNKNOWN;
		}
		return ServiceStatus.SUCCESS;
	}
}
