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

package com.vodafone360.people.tests.engine.contactsync;

import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.BaseSyncProcessor;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.engine.contactsync.SyncStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.io.ResponseQueue.Response;

public class DummyContactSyncEngine extends BaseEngine implements IContactSyncCallback {

	BaseSyncProcessor mProcessor = null;
	boolean mDataChangedFlag;
	boolean mProcessorCompleteFlag;
	ServiceStatus mCompleteStatus;
	Integer mActiveReqId;
	SyncStatus mSyncStatus;
	Object mProcessorCompleteLock = new Object();
	Object mWaitForReqIdLock = new Object();
	
	public DummyContactSyncEngine(IEngineEventCallback eventCallback) {
		super(eventCallback);
		mEngineId = EngineId.CONTACT_SYNC_ENGINE;
	}

	public void setProcessor(BaseSyncProcessor processor) {
		mProcessor = processor;		
	}
	
	@Override
	public long getNextRunTime() {
		if (isCommsResponseOutstanding()) {
			return 0;
		}
		if (mCurrentTimeout != null) {
			return 0;
		}
		return -1;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
	}

	@Override
	protected void onRequestComplete() {
	}

	@Override
	protected void onTimeoutEvent() {
	}

	@Override
	protected void processCommsResponse(Response resp) {
		if (mProcessor != null) {
			mProcessor.processCommsResponse(resp);
		}
	}

	@Override
	protected void processUiRequest(ServiceUiRequest requestId, Object data) {
	}

	@Override
	public void run() {
		if (mCurrentTimeout != null) {
			mCurrentTimeout = null;
			mProcessor.onTimeoutEvent();
			return;
		}
		if (isCommsResponseOutstanding() && processCommsInQueue()) {
			return;
		}
	}

	@Override
	public BaseEngine getEngine() {
		return this;
	}

	@Override
	public void setTimeout(long timeout) {
		super.setTimeout(timeout);
	}

	@Override
	public void onDatabaseChanged() {
		mDataChangedFlag = true;
	}

	@Override
	public void onProcessorComplete(ServiceStatus status, String failureList,
			Object data) {
		mProcessorCompleteFlag = true;
		mCompleteStatus = status;
		synchronized(mProcessorCompleteLock) {
			mProcessorCompleteLock.notifyAll();
		}
	}

	@Override
	public void setActiveRequestId(int reqId) {
		synchronized(mWaitForReqIdLock) {
			mActiveReqId = reqId;
			mWaitForReqIdLock.notifyAll();
		}
	}

	@Override
	public void setSyncStatus(SyncStatus syncStatus) {
	    mSyncStatus = syncStatus;
	}
	
	long getRelativeTimeout() {
		long timeout = getCurrentTimeout();
		if (timeout == -1) {
			return -1;
		}
		return Math.max(0, timeout - System.currentTimeMillis());
	}

	public ServiceStatus waitForProcessorComplete(Long timeout){
		Long timeOfComplete = null;
		if (timeout != null) {
			timeOfComplete = System.nanoTime() + timeout.longValue() * 1000000;
		}
		synchronized(mProcessorCompleteLock) {
			while (!mProcessorCompleteFlag) {
				try {
					if (timeout != null) {
						mProcessorCompleteLock.wait(timeout.longValue());
					} else {
						mProcessorCompleteLock.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (timeOfComplete != null) {
					if (timeOfComplete < System.nanoTime()) {
						return ServiceStatus.ERROR_UNKNOWN;
					}
				}
			}
		}
		return mCompleteStatus;
	}
}
