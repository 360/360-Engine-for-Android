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

package com.vodafone360.people.engine.content;

import java.util.Hashtable;
import java.util.List;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ExternalResponseObject;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue.Response;

/**
 * Content engine for downloading and uploading all kind of content (pictures,
 * videos, files)
 */
public class ContentEngine extends BaseEngine {

 
	/**
     * Constructor for the ContentEngine.
     * 
     * @param eventCallback IEngineEventCallback for calling the constructor of
     *            the super class
     * @param dbHelper Instance of DatabaseHelper
     */
    public ContentEngine(final IEngineEventCallback eventCallback, final DatabaseHelper dbHelper) {
        super(eventCallback);
        this.mDbHelper = dbHelper;
        mEngineId = EngineId.CONTENT_ENGINE;
    }

    /**
     * Queue with unprocessed ContentObjects.
     */
    private FiFoQueue mUnprocessedQueue = new FiFoQueue();

    /**
     * Queue with ContentObjects for downloads.
     */
    private FiFoQueue mDownloadQueue = new FiFoQueue();

    /**
     * Queue with ContentObjects for uploads.
     */
    private FiFoQueue mUploadQueue = new FiFoQueue();

    /**
     * Instance of DatabaseHelper.
     */
    private DatabaseHelper mDbHelper;

    /**
     * Hashtable to match requests to ContentObjects.
     */
    private Hashtable<Integer, ContentObject> requestContentObjectMatchTable = new Hashtable<Integer, ContentObject>();

    /**
     * Getter for the local instance of DatabaseHelper.
     * 
     * @return local instance of DatabaseHelper
     */
    public final DatabaseHelper getDatabaseHelper() {
        return mDbHelper;
    }

    /**
     * Processes one ContentObject.
     * 
     * @param co ContentObject to be processed
     */
    public final void processContentObject(final ContentObject co) {
        mUnprocessedQueue.add(co);
    }

    /**
     * Iterates over the ContentObject list and processes every element.
     * 
     * @param list List with ContentObjects which are to be processed
     */
    public final void processContentObjects(final List<ContentObject> list) {
        for (ContentObject co : list) {
            processContentObject(co);
        }
    }

    /**
     * Processes the main queue and splits it into the download and upload
     * queues.
     */
    private void processQueue() {
        ContentObject co;
        // picking unprocessed ContentObjects
        while ((co = mUnprocessedQueue.poll()) != null) {
            // putting them to downloadqueue ...
            if (co.getDirection() == ContentObject.TransferDirection.DOWNLOAD) {
                mDownloadQueue.add(co);
            } else {
                // ... or the uploadqueue
                mUploadQueue.add(co);
            }
        }
    }

    /**
     * Determines the next RunTime of this Engine It first processes the
     * in-queue and then look.
     * 
     * @return time in milliseconds from now when the engine should be run
     */
    @Override
    public final long getNextRunTime() {
        processQueue();
        // if there are CommsResponses outstanding, run now
        if (isCommsResponseOutstanding()) {
            return 0;
        }
        return (mDownloadQueue.size() + mUploadQueue.size() > 0) ? 0 : -1;
    }

    /**
     * Empty implementation without function at the moment.
     */
    @Override
    public void onCreate() {
    }

    /**
     * Empty implementation without function at the moment.
     */
    @Override
    public void onDestroy() {
    }

    /**
     * Empty implementation without function at the moment.
     */
    @Override
    protected void onRequestComplete() {
    }

    /**
     * Empty implementation without function at the moment.
     */
    @Override
    protected void onTimeoutEvent() {
    }

    /**
     * Processes the response Finds the matching contentobject for the repsonse
     * using the id of the response and sets its status to done. At last the
     * TransferComplete method of the ContentObject is called.
     * 
     * @param resp Response object that has been processed
     */
    @Override
    protected final void processCommsResponse(final Response resp) {
        ContentObject co = requestContentObjectMatchTable.remove(resp.mReqId);

        if (co == null) { // check if we have an invalid response
            return;
        }
        
        List<BaseDataType> mDataTypes = resp.mDataTypes;
        // Sometimes it is null or empty
        if (mDataTypes == null || mDataTypes.size()==0) {
            co.setTransferStatus(ContentObject.TransferStatus.ERROR);
            RuntimeException exc = new RuntimeException("Empty response returned");
            co.getTransferListener().transferError(co, exc);
            return;
        }

        Object data = mDataTypes.get(0);
        if (mDataTypes.get(0).getType() == BaseDataType.SERVER_ERROR_DATA_TYPE
                || mDataTypes.get(0).getType() == BaseDataType.SYSTEM_NOTIFICATION_DATA_TYPE) {
            co.setTransferStatus(ContentObject.TransferStatus.ERROR);
            RuntimeException exc = new RuntimeException(data.toString());
            co.getTransferListener().transferError(co, exc);
        } else {
            co.setTransferStatus(ContentObject.TransferStatus.DONE);
            co.setExtResponse((ExternalResponseObject) data);
            co.getTransferListener().transferComplete(co);
        }
    }

    /**
     * Empty implementation of abstract method from BaseEngine.
     */
    @Override
    protected void processUiRequest(final ServiceUiRequest requestId, final Object data) {

    }

    /**
     * run method of this engine iterates over the downloadqueue, makes requests
     * out of ContentObjects and puts them into QueueManager queue.
     */
    @Override
    public final void run() {
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }

        ContentObject co; 
        boolean queueChanged = false;
        while ((co = mDownloadQueue.poll()) != null) {
            queueChanged = true;
            // set the status of this contentobject to transferring
            co.setTransferStatus(ContentObject.TransferStatus.TRANSFERRING);
            Request request = new Request(co.getUrl().toString(), co.getUrlParams(), engineId());
            QueueManager.getInstance().addRequest(request);
            // important: later we will match done requests back to the
            // contentobject using this map
            requestContentObjectMatchTable.put(request.getRequestId(), co);
        }
        if (queueChanged) {
            QueueManager.getInstance().fireQueueStateChanged();
        }
    }

}
