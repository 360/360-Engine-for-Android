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

package com.vodafone360.people.engine.contactsync;

import java.security.InvalidParameterException;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;

/**
 * Base-class for processor handling for the various stages of Contact sync.
 */
public abstract class BaseSyncProcessor {
    /**
     * Provides useful callback methods in the contact sync engine
     */
    protected IContactSyncCallback mCallback;

    /**
     * String used by processors to compile extra information when a contact
     * sync isn't successful. Currently used to list contacts which the server
     * does not accept during the sync.
     */
    protected String mFailureList;

    /**
     * Provides processors with access to the people database.
     */
    protected DatabaseHelper mDb;

    /**
     * Base constructor.
     * 
     * @param callback Provides access to the contact sync engine
     * @param db The database helper reference
     */
    protected BaseSyncProcessor(IContactSyncCallback callback, DatabaseHelper db) {
        mCallback = callback;
        mDb = db;
    }

    /**
     * Start the processor. Once started the processors is in the active state
     * until either it is cancelled by engine, or the processor completes the
     * sync (see {@link #complete(ServiceStatus)}).
     */
    public void start() {
        mFailureList = "";
        doStart();
    }

    /**
     * Cancel processor
     */
    protected void cancel() {
        doCancel();
        complete(ServiceStatus.USER_CANCELLED);
    }

    /**
     * Implemented by all contact sync processors to initiate the sync. Once
     * called, the processor needs to call the
     * {@link IContactSyncCallback#onProcessorComplete(ServiceStatus, String, Object)}
     * method when the sync is finished (the usual way to do this is by calling
     * {@link #complete(ServiceStatus)}).
     */
    protected abstract void doStart();

    /**
     * Can be called anytime when the processor is active, to cancel the sync.
     * Should not complete the sync here because the above method
     * {@link #cancel()} will take care of this.
     */
    protected abstract void doCancel();

    /**
     * Can be overriden by processors which are interested in timeout events. A
     * processor will only receive timeout events while it is active.
     */
    public void onTimeoutEvent() {
    }

    /**
     * Called when a comms response is received from the server. This may be a
     * response from a request issued by the processor or a push message.
     * 
     * @param resp The response data
     */
    public abstract void processCommsResponse(DecodedResponse resp);

    /**
     * Helper method to complete the processor.
     * 
     * @param status The result of the sync
     */
    protected void complete(ServiceStatus status) {
        mCallback.onProcessorComplete(status, mFailureList, null);
    }

    /**
     * Provides access to the contact sync engine object. This is normally only
     * used for processor to issue comms requests.
     * 
     * @return The contact sync engine
     */
    protected BaseEngine getEngine() {
        return mCallback.getEngine();
    }

    /**
     * Called by processors when they make changes to the people database. This
     * is implemented outside the database framework to ensure UI updates happen
     * more efficiently during a contact sync.
     */
    protected void markDbChanged() {
        mCallback.onDatabaseChanged();
    }

    /**
     * Helper function that can be used by processors to set a timeout. The
     * {@link #onTimeoutEvent()} will be called once the timeout completes.
     * 
     * @note Only a single timeout is allowed at any time, so setting a timeout
     *       will cancel any previous pending timeout.
     * @param timeout The timeout value in milliseconds.
     */
    protected void setTimeout(long timeout) {
        mCallback.setTimeout(timeout);
    }
    
    /**
     * Used by processors to provide an indication of their current progress.
     * 
     * @param SyncStatus Status of the processor, must not be NULL.
     * @throws InvalidParameterException when SyncStatus is NULL.
     */
    protected void setSyncStatus(SyncStatus syncStatus) {
        mCallback.setSyncStatus(syncStatus);
    }

    /**
     * Processors can call this method when they issue a comms requests. The
     * framework will then only notify the processor when a response is received
     * which matches the request ID.
     * 
     * @param reqId The request ID returned by the comms framework.
     */
    protected void setReqId(int reqId) {
        if (reqId == -1) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }
        mCallback.setActiveRequestId(reqId);
    }

    /**
     * Processors may override this function is they have any additional cleanup
     * to do when the processor has finished. Similiar to finalize except it is
     * always called immediately the processor goes inactive.
     */
    public void onComplete() {

    }
}
