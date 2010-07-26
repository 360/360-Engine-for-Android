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

package com.vodafone360.people.engine.meprofile;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ExternalResponseObject;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.api.Contacts;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.ThumbnailUtils;
import com.vodafone360.people.utils.WidgetUtils;

/**
 * This is an implementation for an engine to synchronize 
 * Me profile data. 
 *
 */
public class SyncMeEngine extends BaseEngine {

    /**
     * Current engine state.
     */
    private State mState = State.IDLE;
    /**
     * Database.
     */
    private DatabaseHelper mDbHelper;
    /**
     * The latest revision of Me Profile.
     */
    private long mFromRevision;
    /**
     * The Me Profile, as it was uploaded.
     */
    private ArrayList<ContactDetail> mUploadedMeDetails;

    /**
     * Indicates if the first time sync has been ever initiated.
     */
    private boolean mFirstTimeSyncStarted;

    /**
     * Indicates if the first time sync has been completed.
     */
    private boolean mFirstTimeMeSyncComplete;
    
    /**
     * UiAgent reference to update progress bar.
     */
    private final UiAgent mUiAgent;
    /**
     * ApplicationCache reference to update progress bar.
     */
    private final ApplicationCache mCache;

    /**
     * Defines the contact sync mode. The mode determines the sequence in which
     * the contact sync processors are run.
     */
    private enum State {
        /**
         * The state when the engine is not running and has nothing on the todo list.
         */
        IDLE,
        /**
         * The state when the engine is downloading Me Profile from server.
         */
        FETCHING_ME_PROFILE_CHANGES,
        /**
         * The state when the engine is uploading Me Profile to server.
         */
        UPDATING_ME_PROFILE,
        /**
         * The state when the engine is uploading Me Profile status message.
         */
        UPDATING_ME_PRESENCE_TEXT,
        /**
         * The state when the engine is downloading Me Profile thumbnail.
         */
        FETCHING_ME_PROFILE_THUMBNAIL,
    }

    /**
     * Percentage used to show progress when the me sync is half complete.
     */
    private static final int PROGRESS_50 = 50;
    
    /**
     * The service context.
     */
    private Context mContext;

    /**
     * The constructor.
     * @param eventCallback IEngineEventCallback
     * @param db DatabaseHelper - database.
     */
    public SyncMeEngine(final Context context, final IEngineEventCallback eventCallback, DatabaseHelper db) {
        super(eventCallback);
        mEngineId = EngineId.SYNCME_ENGINE;
        mDbHelper = db;
        mContext = context;
        
        mFromRevision = StateTable.fetchMeProfileRevision(mDbHelper.getReadableDatabase());
        mUiAgent = mEventCallback.getUiAgent();
        mCache = mEventCallback.getApplicationCache();
    }

    @Override
    public long getNextRunTime() {
        if (!isReady()) {
            return -1;
        }
        if (mFirstTimeSyncStarted && !mFirstTimeMeSyncComplete && (mState == State.IDLE)) {
            return 0;
        }
        if (isCommsResponseOutstanding()) {
            return 0;
        }
        if (isUiRequestOutstanding()) {
            return 0;
        }
        return getCurrentTimeout();
    }

    /**
     * The condition for the sync me engine run.
     * @return boolean - TRUE when the engine is ready to run.
     */
    private boolean isReady() {
        return EngineManager.getInstance().getLoginEngine().isLoggedIn() && checkConnectivity()
                && mFirstTimeSyncStarted;
    }

    @Override
    public void run() {
        LogUtils.logD("SyncMeEngine run");
        processTimeout();
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }
        if (isUiRequestOutstanding()) {
            processUiQueue();
        }

        if (mFromRevision == 0 && (mState == State.IDLE)) {
            addGetMeProfileContactRequest();
        }

    }

    @Override
    public void onCreate() {
        PersistSettings setting1 = mDbHelper
                .fetchOption(PersistSettings.Option.FIRST_TIME_MESYNC_STARTED);
        PersistSettings setting2 = mDbHelper
                .fetchOption(PersistSettings.Option.FIRST_TIME_MESYNC_COMPLETE);
        if (setting1 != null) {
            mFirstTimeSyncStarted = setting1.getFirstTimeMeSyncStarted();
        }
        if (setting2 != null) {
            mFirstTimeMeSyncComplete = setting2.getFirstTimeMeSyncComplete();
        }

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
    protected final void processUiRequest(final ServiceUiRequest requestId, Object data) {
        switch (requestId) {
            case UPDATE_ME_PROFILE:
                uploadMeProfile();
                break;
            case GET_ME_PROFILE:
                getMeProfileChanges();
                break;
            case UPLOAD_ME_STATUS:
                uploadStatusUpdate(SyncMeDbUtils.updateStatus(mDbHelper, (String)data));
                break;
            default:
                // do nothing.
                break;
        }

    }

    /**
     * Sends a GetMyChanges request to the server, with the current version of
     * the me profile used as a parameter.
     */
    private void getMeProfileChanges() {
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            return;
        }
        newState(State.FETCHING_ME_PROFILE_CHANGES);
        setReqId(Contacts.getMyChanges(this, mFromRevision));
    }

    /**
     * The call to download the thumbnail picture for the me profile.
     * @param url String - picture url of Me Profile (comes with getMyChanges())
     * @param localContactId long - local contact id of Me Profile
     */
    private void downloadMeProfileThumbnail(final String url, final long localContactId) {
        if (NetworkAgent.getAgentState() == NetworkAgent.AgentState.CONNECTED) {
            Request request = new Request(url, ThumbnailUtils.REQUEST_THUMBNAIL_URI, engineId());
            newState(State.FETCHING_ME_PROFILE_THUMBNAIL);
            setReqId(QueueManager.getInstance().addRequestAndNotify(request));
        }
    }

    /**
     * Starts uploading a status update to the server and ignores all other
     * @param statusDetail - status ContactDetail
     */
    private void uploadStatusUpdate(final ContactDetail statusDetail) {
        LogUtils.logE("SyncMeProfile uploadStatusUpdate()");

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            LogUtils.logE("SyncMeProfile uploadStatusUpdate: no internet connection");
            return;
        }
        if (statusDetail == null) {
            LogUtils.logE("SyncMeProfile uploadStatusUpdate: null status can't be posted");
            return;
        }

        newState(State.UPDATING_ME_PRESENCE_TEXT);
        List<ContactDetail> details = new ArrayList<ContactDetail>();
        statusDetail.updated = null;
        details.add(statusDetail);

        setReqId(Contacts.setMe(this, details, null, null));
    }

    /**
     * * Sends a SetMe request to the server in the case that the me profile has
     * been changed locally. If the me profile thumbnail has always been changed
     * it will also be uploaded to the server.
     */
    private void uploadMeProfile() {
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            return;
        }

        newState(State.UPDATING_ME_PROFILE);

        Contact meProfile = new Contact();
        mDbHelper.fetchContact(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper), meProfile);

        mUploadedMeDetails = SyncMeDbUtils.saveContactDetailChanges(mDbHelper, meProfile);

        setReqId(Contacts.setMe(this, mUploadedMeDetails, meProfile.aboutMe, meProfile.gender));
    }

    /**
     * Get current connectivity state from the NetworkAgent. If not connected
     * completed UI request with COMMs error.
     * @return true NetworkAgent reports we are connected, false otherwise.
     */
    private boolean checkConnectivity() {
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return false;
        }
        return true;
    }

    /**
     * Changes the state of the engine.
     * @param newState The new state
     */
    private void newState(final State newState) {
        State oldState = mState;
        mState = newState;
        LogUtils.logV("SyncMeEngine newState(): " + oldState + " -> " + mState);
    }

    /**
     * Called by framework when a response to a server request is received.
     * @param resp The response received
     */
    public final void processCommsResponse(final DecodedResponse resp) {
        if (!processPushEvent(resp)) {
            switch (mState) {
                case FETCHING_ME_PROFILE_CHANGES:
                    processGetMyChangesResponse(resp);
                    break;
                case UPDATING_ME_PRESENCE_TEXT:
                    processUpdateStatusResponse(resp);
                    break;
                case UPDATING_ME_PROFILE:
                    processSetMeResponse(resp);
                    break;
                case FETCHING_ME_PROFILE_THUMBNAIL:
                    processMeProfileThumbnailResponse(resp);
                    break;
                default:
                    // do nothing.
                    break;
            }
        }
    }

    /**
     * This method stores the thumbnail picture for the me profile
     * @param resp Response - normally contains ExternalResponseObject for the
     *            picture
     */
    private void processMeProfileThumbnailResponse(final DecodedResponse resp) {
        Contact currentMeProfile = new Contact();
        ServiceStatus status = SyncMeDbUtils.fetchMeProfile(mDbHelper, currentMeProfile);
        if (status == ServiceStatus.SUCCESS) {
            if (resp.mReqId == null || resp.mReqId == 0) {
                if (resp.mDataTypes.size() > 0
                        && resp.mDataTypes.get(0).getType() == BaseDataType.SYSTEM_NOTIFICATION_DATA_TYPE
                        && ((SystemNotification)resp.mDataTypes.get(0)).getSysCode() == SystemNotification.SysNotificationCode.EXTERNAL_HTTP_ERROR) {
                    LogUtils.logE("SyncMeProfile processMeProfileThumbnailResponse():"
                            + SystemNotification.SysNotificationCode.EXTERNAL_HTTP_ERROR);

                }
                completeUiRequest(status);
                return;
            } else if (resp.mDataTypes.get(0).getType() == BaseDataType.SYSTEM_NOTIFICATION_DATA_TYPE) {
                if (((SystemNotification)resp.mDataTypes.get(0)).getSysCode() == SystemNotification.SysNotificationCode.EXTERNAL_HTTP_ERROR) {
                    LogUtils.logE("SyncMeProfile processMeProfileThumbnailResponse():"
                            + SystemNotification.SysNotificationCode.EXTERNAL_HTTP_ERROR);
                }
                completeUiRequest(status);
                return;

            }
            status = BaseEngine
                    .getResponseStatus(BaseDataType.EXTERNAL_RESPONSE_OBJECT_DATA_TYPE, resp.mDataTypes);
            if (status != ServiceStatus.SUCCESS) {
                completeUiRequest(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                LogUtils
                        .logE("SyncMeProfile processMeProfileThumbnailResponse() - Can't read response");

                return;
            }

            if (resp.mDataTypes == null || resp.mDataTypes.isEmpty()) {
                LogUtils
                        .logE("SyncMeProfile processMeProfileThumbnailResponse() - Datatypes are null");
                completeUiRequest(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                return;
            }
            // finally save the thumbnails
            ExternalResponseObject ext = (ExternalResponseObject)resp.mDataTypes.get(0);
            if (ext.mBody == null) {
                LogUtils.logE("SyncMeProfile processMeProfileThumbnailResponse() - no body");
                completeUiRequest(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                return;
            }

            try {
                ThumbnailUtils.saveExternalResponseObjectToFile(currentMeProfile.localContactID,
                        ext);
                ContactSummaryTable.modifyPictureLoadedFlag(currentMeProfile.localContactID, true,
                        mDbHelper.getWritableDatabase());
                mDbHelper.markMeProfileAvatarChanged();
            } catch (IOException e) {
                LogUtils.logE("SyncMeProfile processMeProfileThumbnailResponse()", e);
                completeUiRequest(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
            }
        }
        completeUiRequest(status);
    }

    /**
     * Processes the response from a GetMyChanges request. The me profile data
     * will be merged in the local database if the response is successful.
     * Otherwise the processor will complete with a suitable error.
     * @param resp Response from server.
     */
    private void processGetMyChangesResponse(final DecodedResponse resp) {
        LogUtils.logD("SyncMeEngine processGetMyChangesResponse()");
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_CHANGES_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactChanges changes = (ContactChanges)resp.mDataTypes.get(0);
            Contact currentMeProfile = new Contact();
            status = SyncMeDbUtils.fetchMeProfile(mDbHelper, currentMeProfile);
            switch (status) {
                case SUCCESS:
                    String url = SyncMeDbUtils.updateMeProfile(mDbHelper, currentMeProfile,
                            changes.mUserProfile);
                    if (url != null) {
                        downloadMeProfileThumbnail(url, currentMeProfile.localContactID);
                    } else {
                        completeUiRequest(status);
                    }
                    break;
                case ERROR_NOT_FOUND: // this is the 1st time sync
                    currentMeProfile.copy(changes.mUserProfile);
                    status = SyncMeDbUtils.setMeProfile(mDbHelper, currentMeProfile);
                    mFromRevision = changes.mCurrentServerVersion;
                    StateTable.modifyMeProfileRevision(mFromRevision, mDbHelper
                            .getWritableDatabase());
                    setFirstTimeMeSyncComplete(true);
                    completeUiRequest(status);
                    break;
                default:
                    completeUiRequest(status);
            }
        } else {
            completeUiRequest(status);
        }
    }

    /**
     * Processes the response from a SetMe request. If successful, the server
     * IDs will be stored in the local database if they have changed. Otherwise
     * the processor will complete with a suitable error.
     * @param resp Response from server.
     */
    private void processSetMeResponse(final DecodedResponse resp) {
        LogUtils.logD("SyncMeProfile.processMeProfileUpdateResponse()");

        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_CHANGES_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactChanges result = (ContactChanges) resp.mDataTypes.get(0);
            SyncMeDbUtils.updateMeProfileDbDetailIds(mDbHelper, mUploadedMeDetails, result);
            if (updateRevisionPostUpdate(result.mServerRevisionBefore, result.mServerRevisionAfter,
                    mFromRevision, mDbHelper)) {
                mFromRevision = result.mServerRevisionAfter;
            }
        }
        completeUiRequest(status);

    }

    /**
     * This method processes the response to status update by setMe() method
     * @param resp Response - the expected response datatype is ContactChanges
     */
    private void processUpdateStatusResponse(final DecodedResponse resp) {
        LogUtils.logD("SyncMeDbUtils processUpdateStatusResponse()");

        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_CHANGES_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactChanges result = (ContactChanges) resp.mDataTypes.get(0);
            LogUtils.logI("SyncMeProfile.processUpdateStatusResponse() - Me profile userId = "
                    + result.mUserProfile.userID);
            SyncMeDbUtils.savePresenceStatusResponse(mDbHelper, result);
        }
        completeUiRequest(status);
    }
    
    @Override
    protected void completeUiRequest(ServiceStatus status) {
        super.completeUiRequest(status);
        newState(State.IDLE);

        WidgetUtils.kickWidgetUpdateNow(mContext);
     }

    /**
     * Updates the revision of the me profile in the local state table after the
     * SetMe has completed. This will only happen if the version of the me
     * profile on the server before the update matches our previous version.
     * @param before Version before the update
     * @param after Version after the update
     * @param currentFromRevision Current version from our database
     * @param db Database helper used for storing the change
     * @return true if the update was done, false otherwise.
     */
    private static boolean updateRevisionPostUpdate(final Integer before, final Integer after,
            final long currentFromRevision, final DatabaseHelper db) {
        if (before == null || after == null) {
            return false;
        }
        if (!before.equals(currentFromRevision)) {
            LogUtils
                    .logW("SyncMeProfile.updateRevisionPostUpdate - Previous version is not as expected, current version="
                            + currentFromRevision + ", server before=" + before + ", server after=" + after);
            return false;
        } else {
            StateTable.modifyMeProfileRevision(after, db.getWritableDatabase());
            return true;
        }
    }

    /**
     * This method adds an external request to Contacts/setMe() method to update
     * the Me Profile...
     * @param meProfile Contact - contact to be pushed to the server
     */
    public void addUpdateMeProfileContactRequest() {
        LogUtils.logV("SyncMeEngine addUpdateMeProfileContactRequest()");
        addUiRequestToQueue(ServiceUiRequest.UPDATE_ME_PROFILE, null);
    }

    /**
     * This method adds an external request to Contacts/setMe() method to update
     * the Me Profile status...
     * @param textStatus String - the new me profile status to be pushed to the
     *            server
     */
    public void addUpdateMyStatusRequest(String textStatus) {
        LogUtils.logV("SyncMeEngine addUpdateMyStatusRequest()");
        addUiRequestToQueue(ServiceUiRequest.UPLOAD_ME_STATUS, textStatus);
    }

    /**
     * This method adds an external request to Contacts/getMyChanges() method to
     * update the Me Profile status server. Is called when "pc "push message is
     * received
     */
    private void addGetMeProfileContactRequest() {
        LogUtils.logV("SyncMeEngine addGetMeProfileContactRequest()");
        addUiRequestToQueue(ServiceUiRequest.GET_ME_PROFILE, null);
    }

    /**
     * This method adds an external request to Contacts/getMyChanges() method to
     * update the Me Profile status server, is called by the UI at the 1st sync.
     */
    public void addGetMeProfileContactFirstTimeRequest() {
        LogUtils.logV("SyncMeEngine addGetMeProfileContactFirstTimeRequest()");
        setFirstTimeSyncStarted(true);
        addUiRequestToQueue(ServiceUiRequest.GET_ME_PROFILE, null);
    }

    /**
     * This method process the "pc" push event.
     * @param resp Response - server response normally containing a "pc"
     *            PushEvent data type
     * @return boolean - TRUE if a push event was found in the response
     */
    private boolean processPushEvent(final DecodedResponse resp) {
        if (resp.mDataTypes == null || resp.mDataTypes.size() == 0) {
            return false;
        }
        BaseDataType dataType = resp.mDataTypes.get(0);
        if ((dataType == null) || dataType.getType() != BaseDataType.PUSH_EVENT_DATA_TYPE) {
            return false;
        }
        PushEvent pushEvent = (PushEvent) dataType;
        LogUtils.logV("SyncMeEngine processPushMessage():" + pushEvent.mMessageType);
        switch (pushEvent.mMessageType) {
            case PROFILE_CHANGE:
                addGetMeProfileContactRequest();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Helper function to update the database when the state of the
     * {@link #mFirstTimeMeSyncStarted} flag changes.
     * @param value New value to the flag. True indicates that first time sync
     *            has been started. The flag is never set to false again by the
     *            engine, it will be only set to false when a remove user data
     *            is done (and the database is deleted).
     * @return SUCCESS or a suitable error code if the database could not be
     *         updated.
     */
    private ServiceStatus setFirstTimeSyncStarted(final boolean value) {
        if (mFirstTimeSyncStarted == value) {
            return ServiceStatus.SUCCESS;
        }
        PersistSettings setting = new PersistSettings();
        setting.putFirstTimeMeSyncStarted(value);
        ServiceStatus status = mDbHelper.setOption(setting);
        if (ServiceStatus.SUCCESS == status) {
            synchronized (this) {
                mFirstTimeSyncStarted = value;
            }
        }
        return status;
    }

    /**
     * Helper function to update the database when the state of the
     * {@link #mFirstTimeMeSyncComplete} flag changes.
     * @param value New value to the flag. True indicates that first time sync
     *            has been completed. The flag is never set to false again by
     *            the engine, it will be only set to false when a remove user
     *            data is done (and the database is deleted).
     * @return SUCCESS or a suitable error code if the database could not be
     *         updated.
     */
    private ServiceStatus setFirstTimeMeSyncComplete(final boolean value) {
        if (mFirstTimeMeSyncComplete == value) {
            return ServiceStatus.SUCCESS;
        }
        PersistSettings setting = new PersistSettings();
        setting.putFirstTimeMeSyncComplete(value);
        ServiceStatus status = mDbHelper.setOption(setting);
        if (ServiceStatus.SUCCESS == status) {
            synchronized (this) {
                mFirstTimeMeSyncComplete = value;
            }
        }
        return status;
    }

    /**
     * This method needs to be called as part of removeAllData()/changeUser()
     * routine.
     */
    /** {@inheritDoc} */
    @Override
    public final void onReset() {
        mFirstTimeMeSyncComplete = false;
        mFirstTimeSyncStarted = false;
        mFromRevision = 0;
        super.onReset();
    }

    /**
     * This method TRUE if the Me Profile has been synced once.
     * @return boolean - TRUE if the Me Profile has been synced once.
     */
    public final boolean isFirstTimeMeSyncComplete() {
        return mFirstTimeMeSyncComplete;
    }
}
