/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution 
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at src/com/vodafone/people/VODAFONE.LICENSE.txt or 
 * ###TODO:URL_PLACEHOLDER###
 * See the License for the specific language governing permissions and limitations under the 
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License 
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the fields enclosed by brackets 
 * "[]" replaced with your own identifying information: Portions Copyright [yyyy] [name of 
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.engine.contactsync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.ThumbnailInfo;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ExternalResponseObject;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.ThumbnailUtils;

/**
 * Handles download of Contact thumbnails from the People server. Thumbnails are
 * downloaded via RPG external requests. Each contact retrieved from the server
 * has a URL path to its thumbnail if one exists - an RPG external request is
 * created for each thumbnail specifying the URL for the thumbnail. Internally
 * the People client associates downloaded thumbnails with contacts via the
 * local contact id.
 */
public class DownloadServerThumbnails_old extends BaseSyncProcessor {

    /**
     * Number of thumbnails to fetch in a single RPG request batch
     */
    private static final int MAX_THUMBS_FETCHED_PER_PAGE = 15;

    /**
     * Total number of thumbnails we need to fetch (mainly used for monitoring
     * progress)
     */
    private int mTotalThumbnailsToFetch;

    /**
     * Number of thumbnails fetched so far.
     */
    private int mThumbnailsFetchedCount;

    /**
     * Total no of thumbnails requested in the current batch
     */
    private int mBatchNoOfThumbs;

    /**
     * Number of thumbnails received so far in the current batch
     */
    private int mBatchThumbsReceived;

    /**
     * Maps request IDs to local contact IDs so that when a response is received
     * we can link the response with a contact.
     */
    private final Map<Integer, Long> mContIds = new HashMap<Integer, Long>();

    /**
     * Processor constructer.
     * 
     * @param callback Provides access to the contact sync engine processor
     *            methods.
     * @param db Database for finding contacts that require thumbanils and
     *            updating thumbnail state flag for each contact.
     * @param context Context for file operations
     */
    public DownloadServerThumbnails_old(IContactSyncCallback callback, DatabaseHelper db,
            Context context) {
        super(callback, db);
    }

    /**
     * Called by framework to start the processor running. Issues a query to
     * determine the total number of contacts which need thumbnails, then sends
     * the first batch of thumbnail requests to the server.
     */
    @Override
    protected void doStart() {
        // mContext.getDir(THUMB_PATH, 0); // Creates the thumbnail folder if it
        // does not exist.
        mTotalThumbnailsToFetch = mDb.fetchThumbnailUrlCount();
        fetchNextPage();
    }

    /**
     * Called by framework when the contact sync is cancelled. No implementation
     * required, simply ignores the responses from the server.
     */
    @Override
    protected void doCancel() {
    }

    /**
     * Sends next batch of thumbnail requests to the server.
     */
    private void fetchNextPage() {
        if (mThumbnailsFetchedCount >= mTotalThumbnailsToFetch) {
            complete(ServiceStatus.SUCCESS);
            return;
        }
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        List<ThumbnailInfo> thumbInfoList = new ArrayList<ThumbnailInfo>();
        ServiceStatus status = mDb.fetchThumbnailUrls(thumbInfoList, mThumbnailsFetchedCount,
                Math.min(mTotalThumbnailsToFetch - mThumbnailsFetchedCount,
                        MAX_THUMBS_FETCHED_PER_PAGE));
        if (ServiceStatus.SUCCESS != status || thumbInfoList.size() == 0) {
            complete(status);
            return;
        }

        mBatchNoOfThumbs = thumbInfoList.size();
        mBatchThumbsReceived = 0;
        mContIds.clear();

        Request[] requests = new Request[mBatchNoOfThumbs];

        for (int i = 0; i < mBatchNoOfThumbs; i++) {
            requests[i] = new Request(thumbInfoList.get(i).photoServerUrl,
                    ThumbnailUtils.REQUEST_THUMBNAIL_URI, getEngine().engineId());
        }

        int[] reqIds = QueueManager.getInstance().addRequest(requests); // add
        // batch of thumbnail
        // request to queue
        for (int i = 0; i < mBatchNoOfThumbs; i++) {
            mContIds.put(reqIds[i], thumbInfoList.get(i).localContactId);
        }

        // TODO: AA: see if we can do that inside the Queue
        QueueManager.getInstance().fireQueueStateChanged();
    }

    /**
     * Called by framework when a response to one of the thumbnail requests is
     * received. The thumbnail is saved in the local file system and the local
     * database updated to indicate that the thumbnail has been sync'ed.
     */
    @Override
    public void processCommsResponse(Response resp) {
        if (resp.mReqId == null || resp.mReqId == 0) {
            if (resp.mDataTypes.size() > 0) {
                BaseDataType data = resp.mDataTypes.get(0);
                if (data.name().equals(SystemNotification.class.getSimpleName())) {
                    if (((SystemNotification)data).getSysCode() == SystemNotification.SysNotificationCode.EXTERNAL_HTTP_ERROR) {
                        LogUtils
                                .logE("DownloadServerThumbnails.processCommsResponse() - System Notification: External HTTP request failed");
                        increaseCount();
                        return;
                    }
                }
                increaseCount();
                return;
            }
            ServiceStatus status = BaseEngine.getResponseStatus("PushEvent",
                    resp.mDataTypes);
            if (ServiceStatus.SUCCESS != status) {
                increaseCount();
                return;
            }
        }
        Long localContactId = mContIds.remove(resp.mReqId);
        if (localContactId == null) {
            LogUtils
                    .logE("DownloadServerthumbnals.processCommsResponse: Req ID not known - localContactId["
                            + localContactId + "]");
            increaseCount();
            return;
        }
        LogUtils.logI("DownloadServerthumbnails.processCommsResponse() - localContactId["
                + localContactId + "]");
        ServiceStatus status = BaseEngine.getResponseStatus("ExternalResponseObject",
                resp.mDataTypes);
        if (status != ServiceStatus.SUCCESS) {
            increaseCount();
            return;
        }

        if (resp.mDataTypes == null || resp.mDataTypes.isEmpty()) {
            LogUtils.logE("DownloadServerthumbnails.processCommsResponse() "
                    + "Null pointers while trying to read the response had no body local contact ["
                    + localContactId + "] ########################");
            increaseCount();
            return;
        }

        ExternalResponseObject ext = (ExternalResponseObject)resp.mDataTypes.get(0);
        if (ext.mBody == null) {
            LogUtils.logE("DownloadServerthumbnails.processCommsResponse()"
                    + " - response had no body for contact " + localContactId);
            increaseCount();
            return;
        }

        /*
         * try { // saveExternalResponseObjectToFile(localContactId, ext);
         * ContactSummaryTable.modifyPictureLoadedFlag(localContactId, true, mDb
         * .getWritableDatabase()); // AA:move it from here markDbChanged(); }
         * catch (IOException e) {LogUtils.logE(
         * "DownloadServerthumbnails.processCommsResponse() Thumbnail not saved. "
         * + "IOException while processing localContactId[" + localContactId +
         * "]", e); }
         */
        increaseCount();
    }

    private void increaseCount() {
        mThumbnailsFetchedCount++;
        mBatchThumbsReceived++;

        // LogUtils.logWithName("THUMBNAILS:", "mThumbnailsFetchedCount=" +
        // mThumbnailsFetchedCount);
        // LogUtils.logWithName("THUMBNAILS:", "mTotalThumbnailsToFetch=" +
        // mTotalThumbnailsToFetch);
        //
        // LogUtils.logWithName("THUMBNAILS:", "mBatchThumbsReceived=" +
        // mBatchThumbsReceived);
        // LogUtils.logWithName("THUMBNAILS:", "mBatchNoOfThumbs=" +
        // mBatchNoOfThumbs);
        if (mBatchThumbsReceived == mBatchNoOfThumbs) {
            // AA: added
            markDbChanged();
            fetchNextPage();
        }
    }

}
