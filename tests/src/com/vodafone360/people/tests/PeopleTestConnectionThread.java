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

package com.vodafone360.people.tests;

import java.util.List;

import android.util.Log;

import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.service.transport.IQueueListener;

public class PeopleTestConnectionThread implements Runnable, IQueueListener, IConnection{
	
	private volatile boolean mIsConnectionRunning;
	private Object mRunLock = new Object();
	IPeopleTestFramework mTestFramework;
	
	public PeopleTestConnectionThread(IPeopleTestFramework testFramework){
		mTestFramework = testFramework;
	}
	
	public synchronized void startThread() {
		Log.d("TAG", "PeopleTestConnectionThread.startThread");
		mIsConnectionRunning = true;
		Thread t = new Thread(this);
		t.start();
		
	}

	public synchronized void stopThread() {
		synchronized(mRunLock) {
			mIsConnectionRunning = false;
			mRunLock.notify();
		}
	}
	
	public void run() {
		Log.d("TAG", "PeopleTestConnectionThread.run");
		while (mIsConnectionRunning) {
			Log.d("TAG", "PeopleTestConnectionThread.run running");
                        // Getting both RPG and HTTP Request
			List<Request> requests = QueueManager.getInstance().getAllRequests();
			if (requests.size() > 0) {
				// report back to test frame work
				Log.d("TAG", "PeopleTestConnectionThread.run report back");
				Request req = requests.get(0);
				mTestFramework.reportBackToFramework(req.getRequestId(), req.mEngineId);
			}
			else {
				try {	
					Log.d("TAG", "PeopleTestConnectionThread.run wait");
					synchronized(mRunLock) {
						if (mIsConnectionRunning) {
							mRunLock.wait();
						}
					}
				} catch (InterruptedException ie) {
					Log.e(getClass().getName(), "Wait was interrupted: " + ie);
				}
			}
		}
	}
	
	@Override
	public void notifyOfItemInRequestQueue() {
		Log.d("TAG", "PeopleTestConnectionThread.notifyOfItemInRequestQueue");
		synchronized(mRunLock) {
			mRunLock.notify();	
		}
	}

	@Override
	public boolean getIsConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getIsRpgConnectionActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void notifyOfRegainedNetworkCoverage() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLoginStateChanged(boolean isLoggedIn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyOfUiActivity() {
		// TODO Auto-generated method stub
		
	}
}



