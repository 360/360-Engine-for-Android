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

package com.vodafone360.people.engine.activities;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.IContactSyncObserver;
import com.vodafone360.people.engine.login.LoginEngine.ILoginEventsListener;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.service.io.api.Activities;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.utils.LogUtils;

/**
 * Engine response for handling of Activities (i.e Timeline and Status events).
 * It also interfaces to the native Call and Message logs to allow these events
 * to be handled within the People Client's Timeline.
 */
public class ActivitiesEngine extends BaseEngine implements IContactSyncObserver,
        ILoginEventsListener {

    /**
     * below are the constants to communicate the engine state states to UI: 3
     * buttons (update statuses, load older statuses, older timelines)
     */
    public static final String UPDATING_STATUSES = "updating_statuses";

    public static final String FETCHING_OLDER_STATUSES = "fetching_older_statuses";

    public static final String FETCHING_OLDER_TIMELINE = "fecthing_older_timelines";

    /** filter definitions */
    private static final String FILTER_UPDATED = "f.updated";

    private static final String FILTER_GT = ">";

    /** Identifier for filtering against local ids (LIDs) */
    private static final String FILTER_LIDS = "lids";

    /** maximum number of identities fetched at the same time */
    private static final String FILTER_NUM = "0-150";

    private static final String FILTER_SORT = "sort";

    private static final String FILTER_UPDATED_REV = "updated?rev";

    // filter out timeline events and only retrieve status activities
    private static final String FILTER_STATUS = "status";
    private static final String FILTER_TRUE = "true";
    
    private static final long MS_IN_SECOND = 1000L;

    private static final long STATUSES_SYNC_TIMEOUT_MILLIS = 10 * 60 * 1000;

    private static final long READ_TIMELINES_TIMEOUT_MILLS = 0;

    private final static long WEEK_OLD_MILLIS = 7 * 24 * 60 * 60 * 1000;

    private static final String ACTIVITY_ITEM = "ActivityItem";

    /** Timestamp for most recent Status event update */
    private long mLastStatusUpdated;

    /** Timestamp for oldest Status event update */
    private long mOldestStatusUpdated;

    private Context mContext;

    /** engine's current state **/
    private State mState = State.IDLE;

    private boolean mRequestActivitiesRequired;

    private final Hashtable<Integer, String> mActiveRequests = new Hashtable<Integer, String>();

    /**
     * Instance of TimelineEventWatcher which listens for native call and
     * message logs events that ActivitiesEngine should sync.
     */
    private TimelineEventWatcher mTimelineEventWatcher;

    private DatabaseHelper mDb;

    private final Object mQueueMutex = new Object();
    
    private boolean mTimelinesUpdated;

    /**
     * Definitions of Activities engines states; IDLE - engine is inactive
     * FETCH_STATUSES_FIRST_TIME: 1st time sync of statuses,
     * FETCH_CALLOG_FIRST_TIME: first time sync of phone calls, UPDATE_STATUSES:
     * updating statuses from server (user has pressed the "refresh button" or
     * push event's triggered the sync), UPDATE_CALLOG_FROM_NATIVE: new timeline
     * item appeared on the phone, FETCH_OLDER_CALLLOG_FROM_NATIVE_DB: user has
     * pressed "more timelines" button, FETCH_SMS_FROM_NATIVE_DB: fetching
     * SMS/MMS
     */
    private enum State {
        IDLE,
        FETCH_STATUSES_FIRST_TIME,
        FETCH_CALLOG_FIRST_TIME,
        UPDATE_STATUSES,
        UPDATE_CALLOG_FROM_NATIVE,
        FETCH_OLDER_CALLLOG_FROM_NATIVE_DB,
        FETCH_SMS_FROM_NATIVE_DB
    }

    /**
     * Interface for Native call and message log sync classes.
     */
    static interface ISyncHelper {
        void run();

        void cancel();
    }

    /**
     * current processor (fetching timelines), is null if no action for
     * timelines is taken
     */
    private ISyncHelper mActiveSyncHelper;

    /**
     * mutex for thread synchronization
     */
    private final Object mMutex = new Object();

    /**
     * Constructor
     * 
     * @param context - valid context
     * @param eventCallback - callback object
     * @param db - DatabaseHepler object
     */
    public ActivitiesEngine(Context context, IEngineEventCallback eventCallback, DatabaseHelper db) {
        super(eventCallback);
        mEngineId = EngineId.ACTIVITIES_ENGINE;
        mDb = db;
        mContext = context;
        if (isContactSyncReady()) {
            EngineManager.getInstance().getContactSyncEngine().addEventCallback(this);
        }
        mTimelineEventWatcher = new TimelineEventWatcher(mContext, this);
    }

    /**
     * Return next run time for ActivitiesEngine. Determined by whether we have
     * a request we wish to issue, or there is a response that needs processing.
     */
    @Override
    public long getNextRunTime() {
        if (!isContactSyncReady()
                || !EngineManager.getInstance().getContactSyncEngine().isFirstTimeSyncComplete()) {
            return -1;
        }
        if (isCommsResponseOutstanding()) {
            return 0;
        }
        if (isUiRequestOutstanding()) {
            return 0;
        }
        if (mRequestActivitiesRequired && checkConnectivity()) {
            return 0;
        }
        return getCurrentTimeout();

    }

    /**
     * onCreate. Instruct the timeline event watcher to start watching for
     * native events the Activities engine may need to handle (call and message
     * events).
     */
    @Override
    public void onCreate() {
        mTimelineEventWatcher.startWatching();
    }

    /**
     * On destruction of ActivitiesEngine stop the timeline event watcher.
     */
    @Override
    public void onDestroy() {
        mTimelineEventWatcher.stopWatching();
        if (isContactSyncReady()) {
            EngineManager.getInstance().getLoginEngine().removeListener(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onRequestComplete() {
    }

    /**
     * Drive state machine for Activities sync. If we are currently fetching
     * native events run the appropriated ISyncHelper. Otherwise request
     * retrieval of Activities from Server.
     */

    @Override
    protected void onTimeoutEvent() {
        // run now
        LogUtils.logV("ActivitiesEngine onTimeoutEvent:" + mState);
        switch (mState) {
            case FETCH_CALLOG_FIRST_TIME:
            case FETCH_OLDER_CALLLOG_FROM_NATIVE_DB:
            case FETCH_SMS_FROM_NATIVE_DB:
            case UPDATE_CALLOG_FROM_NATIVE:
                mActiveSyncHelper.run();
                if (mActiveSyncHelper != null) {
                    // will be null when the last batch is read, so no need to
                    // fire timeout event - onSyncComplete sets it to null
                    setTimeout(READ_TIMELINES_TIMEOUT_MILLS);
                }
                break;
            case IDLE:
                addStatusesSyncRequest();
                setTimeout(STATUSES_SYNC_TIMEOUT_MILLIS);
                break;
            default:
                // do nothing
        }
    }

    /**
     * Handle response received from transport layer (via EngineManager)
     * 
     * @param resp Received Response item either a Status/Timeline related push
     *            message or a response to a get activities request.
     */
    @Override
    protected void processCommsResponse(Response resp) {
        LogUtils.logD("ActivitiesEngine processCommsResponse");
        // handle push response
        if (resp.mReqId == 0 && resp.mDataTypes.size() > 0) {
            PushEvent evt = (PushEvent)resp.mDataTypes.get(0);
            handlePushRequest(evt.mMessageType);
        } else {
            dequeueRequest(resp.mReqId);
            handleGetActivitiesResponse(resp.mDataTypes);
        }
    }

    /**
     * Handle an outstanding UI request.
     */
    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        LogUtils.logD("ActivityEngine processUiRequest:" + requestId);
        switch (requestId) {
            case UPDATE_STATUSES:
                // this is full sync or push, or "refresh" button
                requestStatusesFromServer(true);
                break;
            case FETCH_STATUSES:
                // "more" button
                requestStatusesFromServer(false);
                break;
            case FETCH_TIMELINES:
                // "more" button - we only need time lines
                enqueueRequest(ServiceUiRequest.FETCH_TIMELINES.ordinal(), FETCHING_OLDER_TIMELINE);
                newState(State.FETCH_OLDER_CALLLOG_FROM_NATIVE_DB);
                startCallLogSync(false);
                break;
            case UPDATE_PHONE_CALLS:
                // something on the NAB has changed
                newState(State.UPDATE_CALLOG_FROM_NATIVE);
                startCallLogSync(true);
                break;
            case UPDATE_SMS:
                newState(State.FETCH_SMS_FROM_NATIVE_DB);
                mActiveSyncHelper = new FetchSmsLogEvents(mContext, this, mDb, true);
                setTimeout(READ_TIMELINES_TIMEOUT_MILLS);
                break;
            default:
                break;
        }
    }

    /**
     * ActivitiesEngine run implementation Processes a response if one is
     * available, Processes any events in engine's UI queue. Issues
     * get-activities request to server as part of sync.
     */
    @Override
    public void run() {
        LogUtils.logD("ActivityEngine run");
        processTimeout();
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }
        if (isUiRequestOutstanding()) {
            processUiQueue();
        }
        if (mRequestActivitiesRequired) {
            requestStatusesFromServer(true);
        }
    }

    /**
     * Start sync of call log from native call log data base
     */
    private void startCallLogSync(boolean refresh) {

        mActiveSyncHelper = new FetchCallLogEvents(mContext, this, mDb, refresh);
        setTimeout(READ_TIMELINES_TIMEOUT_MILLS);
    }

    /**
     * This method is necessary for the tests. It catches the
     * InvalidParameterException coming from EngineManager.getInstance(), when
     * the instance is null
     * 
     * @return boolean - TRUE if ContactSyncEngine is not null
     */
    private boolean isContactSyncReady() {
        try {
            return (EngineManager.getInstance() != null)
                    && (EngineManager.getInstance().getContactSyncEngine() != null);
        } catch (InvalidParameterException ipe) {
            LogUtils.logE(ipe.toString());
            return false;
        }

    }

    /**
     * Request Activities (Status/Timeline) events from Server.
     * 
     * @param refresh boolean - is true when fetching latest statuses, false -
     *            when older
     */
    private void requestStatusesFromServer(boolean refresh) {
        if (!checkConnectivity()) {
            mRequestActivitiesRequired = true;
            return;
        }
        mRequestActivitiesRequired = false;
        if (!isContactSyncReady()
                || !EngineManager.getInstance().getContactSyncEngine().isFirstTimeSyncComplete()) {
            // this method will then call completeUiRequest(status, null);
            onSyncHelperComplete(ServiceStatus.ERROR_NOT_READY);
            return;
        }
        mLastStatusUpdated = StateTable.fetchLatestStatusUpdateTime(mDb.getReadableDatabase());
        mOldestStatusUpdated = StateTable.fetchOldestStatusUpdate(mDb.getReadableDatabase());

        LogUtils.logD("ActivityEngine getActivites last update = " + mLastStatusUpdated);

        int reqId = Activities.getActivities(this, null, applyActivitiesFilter(refresh));
        if (reqId > 0) {
            setReqId(reqId);
            enqueueRequest(reqId, refresh ? UPDATING_STATUSES : FETCHING_OLDER_STATUSES);

            if (mLastStatusUpdated == 0) {
                newState(State.FETCH_STATUSES_FIRST_TIME);
            } else {
                newState(State.UPDATE_STATUSES);
            }
        }
    }

    private Map<String, List<String>> applyActivitiesFilter(boolean refresh) {

        Map<String, List<String>> filter = new Hashtable<String, List<String>>();
        
        // filter out types we're not interested in
        List<String> statusFilter = new ArrayList<String>();
        statusFilter.add(FILTER_TRUE);
        filter.put(FILTER_STATUS, statusFilter);    
        
        if (mLastStatusUpdated > 0) {
            if (refresh) {
                List<String> updateFilter = new ArrayList<String>();
                LogUtils.logD("ActivityEngine TimeFilter newer= '" + FILTER_GT
                        + (mLastStatusUpdated / MS_IN_SECOND) + "'");
                updateFilter.add(FILTER_GT + mLastStatusUpdated / MS_IN_SECOND);
                filter.put(FILTER_UPDATED, updateFilter);
            } else {
                List<String> updateFilter = new ArrayList<String>();
                LogUtils.logD("ActivityEngine TimeFilter older= '" + FILTER_GT
                        + (mOldestStatusUpdated / MS_IN_SECOND) + "'");
                updateFilter.add(FILTER_GT + mOldestStatusUpdated / MS_IN_SECOND);
                filter.put(FILTER_UPDATED, updateFilter);
            }
        } else { // 1st time
            List<String> fNum = new ArrayList<String>();
            fNum.add(FILTER_NUM);
            filter.put(FILTER_LIDS, fNum);
            List<String> sort = new ArrayList<String>();
            sort.add(FILTER_UPDATED_REV);
            filter.put(FILTER_SORT, sort);

            mOldestStatusUpdated = (System.currentTimeMillis() - WEEK_OLD_MILLIS) / MS_IN_SECOND;
        }
        return filter;
    }

    /**
     * Trigger the ActivitiesTable cleanup when the Engine is in
     * "CLEANUP_DATABASE" state.
     */
    private void cleanDatabase() {
        ActivitiesTable.cleanupActivityTable(mDb.getWritableDatabase());
    }

    /**
     * Handle Status or Timeline Activity change Push message
     * 
     * @param evt Push message type (Status change or Timeline change).
     */
    private void handlePushRequest(PushMessageTypes evt) {
        LogUtils.logD("ActivityEngine handlePushRequest");
        switch (evt) {
            case STATUS_ACTIVITY_CHANGE:
            case TIMELINE_ACTIVITY_CHANGE:
                addUiRequestToQueue(ServiceUiRequest.UPDATE_STATUSES, null);
                break;
            default:
                // do nothing
        }
    }

    /**
     * Handle GetActivities response message received from Server
     * 
     * @param reqId Request ID contained in response. This should match an ID of
     *            a request we have issued to the Server.
     * @param data List array of ActivityItem items returned from Server.
     */
    private void handleGetActivitiesResponse(List<BaseDataType> data) {
        /** Array of Activities retrieved from Server. */
        ArrayList<ActivityItem> activityList = new ArrayList<ActivityItem>();
        ServiceStatus errorStatus = genericHandleResponseType(ACTIVITY_ITEM, data);
        LogUtils.logE("ActivityEngine.handleGetActivitiesResponse status from generic = "
                + errorStatus);
        if (ServiceStatus.SUCCESS == errorStatus) {
            for (BaseDataType item : data) {
                if (ACTIVITY_ITEM.equals(item.name())) {
                    activityList.add((ActivityItem)item);
                } else {
                    LogUtils
                            .logE("ActivityEngine.handleGetActivitiesResponse will not handle strange type = "
                                    + item.name());
                }
            }
            errorStatus = updateDatabase(activityList);
            // we set timeout for the next execution
        }
        // this method will then call completeUiRequest(status, null);
        onSyncHelperComplete(errorStatus);
    }

    private ServiceStatus updateDatabase(ArrayList<ActivityItem> activityList) {
        ServiceStatus errorStatus = ServiceStatus.SUCCESS;

        // add retrieved items to Activities table in db
        removeDuplicates(activityList);

        // update the newest activity
        Long temp = findLastStatusUpdateTime(activityList);
        if (temp != Long.MIN_VALUE) {
            mLastStatusUpdated = temp;
        }
        if (activityList.size() > 0) {
            LogUtils.logD("ActivityEngine Added ActivityItems = " + activityList.size());
            // update database
            errorStatus = mDb.addActivities(activityList);
            if (errorStatus == ServiceStatus.SUCCESS) {
                updateLatestStatusUpdateTime();
                updateOldestStatusUpdateTime();
            }
        }
        return errorStatus;
    }

    private void updateLatestStatusUpdateTime() {
        long tluStatus = StateTable.fetchLatestStatusUpdateTime(mDb.getReadableDatabase());
        // modify the timelines dates
        if (mLastStatusUpdated > tluStatus) {
            StateTable.modifyLatestStatusUpdateTime(mLastStatusUpdated, mDb.getWritableDatabase());
            LogUtils.logD("ActivityEngine: last status update set to = " + mLastStatusUpdated);
        }
    }

    private void updateOldestStatusUpdateTime() {
        long tloStatus = StateTable.fetchLatestStatusUpdateTime(mDb.getReadableDatabase());
        // modify the timelines dates
        if (mOldestStatusUpdated < tloStatus) {
            StateTable.modifyOldestStatusTime(mOldestStatusUpdated, mDb.getWritableDatabase());
            LogUtils.logD("ActivityEngine: oldest status update set to = " + mOldestStatusUpdated);
        }
    }

    private Long findLastStatusUpdateTime(ArrayList<ActivityItem> activityList) {
        Long ret = Long.MIN_VALUE;
        for (ActivityItem ai : activityList) {
            if (ai.mTime != null && (ai.mTime.compareTo(ret) > 0)) {
                ret = ai.mTime;
            }
        }
        return ret;
    }

    /**
     * Find oldest item in list of Activities retrieved from server.
     * 
     * @return Time-stamp of oldest event.
     */
    private Long findFirstStatusUpdateTime(ArrayList<ActivityItem> activityList) {
        Long ret = Long.MAX_VALUE;
        for (ActivityItem ai : activityList) {
            if (ai.mTime != null && ai.mTime.compareTo(ret) < 0) {
                ret = ai.mTime;
            }
        }
        return ret;
    }

    /**
     * TODO: investigate why duplicates here can appear. this method might be
     * not necessary. Remove Activities from list of Activities with Activity
     * IDS matching ones we have already retrieved (i.e .duplicates).
     */
    private void removeDuplicates(ArrayList<ActivityItem> activityList) {
        if (activityList.size() == 0) {
            return;
        }
        int dupCount = 0;
        List<Long> actIdList = new ArrayList<Long>();
        mDb.fetchActivitiesIds(actIdList, findFirstStatusUpdateTime(activityList));
        for (int i = 0; i < activityList.size();) {
            boolean inc = true;
            Long id = activityList.get(i).mActivityId;
            if (id != null) {
                for (Long l : actIdList) {
                    if (l.compareTo(id) == 0) {
                        activityList.remove(i);
                        inc = false;
                        dupCount++;
                        break;
                    }
                }
            }
            if (inc) {
                i++;
            }
        }
        LogUtils.logD("ActivityEngine removeDuplicates. Count dups = " + dupCount);
    }

    /** {@inheritDoc} */
    @Override
    public void onContactSyncStateChange(ContactSyncEngine.Mode mode,
            ContactSyncEngine.State oldState, ContactSyncEngine.State newState) {
        LogUtils.logD("ActivityEngine onContactSyncStateChange called.");
    }

    /**
     * Receive notification from ContactSyncEngine that a Contact sync has
     * completed. Start Activity sync if we have not previously synced.
     */
    @Override
    public void onSyncComplete(ServiceStatus status) {
        LogUtils.logD("ActivityEngine onSyncComplete, time:" + mLastStatusUpdated + ", state="
                + mState);
        // fire off background grab of activities
        if (ServiceStatus.SUCCESS == status && (mLastStatusUpdated == 0) && (mState == State.IDLE)) {
            addStatusesSyncRequest();
            LogUtils.logD("ActivityEngine onSyncComplete FULL_SYNC_FIRST_TIME.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onProgressEvent(ContactSyncEngine.State currentState, int percent) {
        LogUtils.logD("ActivityEngine onProgressEvent");
    }

    /**
     * Get current connectivity state from the NetworkAgent. If not connected
     * completed UI request with COMMs error.
     * 
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
     * Function called when helper classes ends their work. Currently supported
     * helpers are FetchCallLogEvents and FetchSmsLogEvents
     * 
     * @param status - ServiceStatus of completed operation.
     */
    protected void onSyncHelperComplete(ServiceStatus status) {
        switch (mState) {
            case FETCH_STATUSES_FIRST_TIME:
                // 1st/full sync
                newState(State.FETCH_CALLOG_FIRST_TIME);
                startCallLogSync(true);
                break;
            case FETCH_OLDER_CALLLOG_FROM_NATIVE_DB:
                // button "more"
                newState(State.FETCH_SMS_FROM_NATIVE_DB);
                mActiveSyncHelper = new FetchSmsLogEvents(mContext, this, mDb, false);
                setTimeout(READ_TIMELINES_TIMEOUT_MILLS);
                break;
            case FETCH_CALLOG_FIRST_TIME:
                // 1st sync
            case UPDATE_CALLOG_FROM_NATIVE:
                // NAB changed.
                newState(State.FETCH_SMS_FROM_NATIVE_DB);
                mActiveSyncHelper = new FetchSmsLogEvents(mContext, this, mDb, true);
                setTimeout(READ_TIMELINES_TIMEOUT_MILLS);
                break;
            case FETCH_SMS_FROM_NATIVE_DB:
                dequeueRequest(ServiceUiRequest.FETCH_TIMELINES.ordinal());
            case UPDATE_STATUSES:
                setTimeout(STATUSES_SYNC_TIMEOUT_MILLIS);
                // activities sync is over
                mActiveSyncHelper = null;
                newState(State.IDLE);
                cleanDatabase();
                completeUiRequest(status, null);
                break;
            default:
                // Do nothing.
                completeUiRequest(status, null);
                break;
        }
    }


   /**
    * This method adds a request to get latest timelines.
    */
   protected void addGetNewPhonesCallsRequest() {
       LogUtils.logD("ActivitiesEngine addGetNewTimelinesRequest()");
       addUiRequestToQueue(ServiceUiRequest.UPDATE_PHONE_CALLS, null);
   }
   
   /**
    * This method adds a request to get latest timelines.
    */
   protected void addGetNewSMSRequest() {
       LogUtils.logD("ActivitiesEngine addGetNewTimelinesRequest()");
       addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
   }

    /**
     * This method adds a request to get older timelines.
     */
    public void addOlderTimelinesRequest() {
        LogUtils.logD("ActivitiesEngine addOlderTimelinesRequest()");
        addUiRequestToQueue(ServiceUiRequest.FETCH_TIMELINES, null);
    }

    /**
     * This method adds a request to start sync of the most recent statuses from
     * Now+ server.
     */
    public void addStatusesSyncRequest() {
        LogUtils.logD("ActivityEngine addStatusesSyncRequest()");
        addUiRequestToQueue(ServiceUiRequest.UPDATE_STATUSES, null);
    }

    /**
     * This method adds a request to start sync of older statuses from Now+
     * server.
     */
    public void addGetOlderStatusesRequest() {
        LogUtils.logD("ActivityEngine addGetOlderStatusesRequest");
        addUiRequestToQueue(ServiceUiRequest.FETCH_STATUSES, null);
    }

    /**
     * Changes the state of the engine.
     * 
     * @param newState The new state
     */
    private void newState(State newState) {
        State oldState = mState;
        synchronized (mMutex) {
            mState = newState;
        }
        switch (mState) {
            case FETCH_OLDER_CALLLOG_FROM_NATIVE_DB:
            case UPDATE_STATUSES:
            case FETCH_STATUSES_FIRST_TIME:
                fireNewState(ServiceUiRequest.UPDATING_UI);
                break;
            case IDLE:
                fireNewState(ServiceUiRequest.UPDATING_UI_FINISHED);
                break;
            default:
                // nothing to do
        }
        LogUtils.logV("ActivitiesEngine.newState(): " + oldState + " -> " + mState);
    }

    /**
     * This method fires states change of an engine to UI (between "busy" and
     * IDLE). This method is normally called after the new state has been
     * changed and published to ApplicationCache. The UI should refer to
     * ApplicationCache when processing this new ServiceRequest
     * 
     * @param request ServiceUIRequest UPDATING_UI or UPDATING_UI_FINISHED
     */
    private void fireNewState(ServiceUiRequest request) {
        UiAgent uiAgent = mEventCallback.getUiAgent();
        if (uiAgent != null && uiAgent.isSubscribed()) {
            uiAgent.sendUnsolicitedUiEvent(request, null);
        }
    }

    /**
     * This method stores the current busy state of engine in ApplicationCache
     * to make it available to UI. This method is normally called before setting
     * new state of the engine.
     * 
     * @param requestId int - the request Id for network communications, or the
     *            ServiceUIRequest ordinal for fetching/updating timelines
     * @param requestType one of UPDATING_STATUSES, FETCHING_OLDER_STATUSES,
     *            FETCHING_OLDER_TIMELINE
     */
    private void enqueueRequest(int requestId, String requestType) {
        synchronized (mQueueMutex) {
            if (!mActiveRequests.containsKey(requestId)) {
                LogUtils.logE("ActivityEngine.enqueueRequest:" + requestId + ", " + requestType);
                mActiveRequests.put(requestId, requestType);
                ApplicationCache.setBooleanValue(mContext, requestType, true);
            } else {
                LogUtils.logE("ActivityEngine.enqueueRequest: already have this type!" + requestId
                        + ", " + requestType);
            }
        }
    }

    /**
     * This method stores removes a busy state of engine and males the change
     * available to UI through the ApplicationCache. This method is normally
     * called before setting new state of the engine.
     * 
     * @param requestId int - the request Id for network communications, or the
     *            ServiceUIRequest ordinal for fetching/updating timelines
     * @param requestType one of UPDATING_STATUSES, FETCHING_OLDER_STATUSES,
     *            FETCHING_OLDER_TIMELINE
     */

    private void dequeueRequest(int requestId) {
        synchronized (mQueueMutex) {
            String requestType = mActiveRequests.get(requestId);
            if (requestType != null) {
                mActiveRequests.remove(requestId);
                LogUtils.logE("ActivityEngine.dequeueRequest:" + requestId + ", " + requestType);
                ApplicationCache.setBooleanValue(mContext, requestType, false);
            } else {
                LogUtils.logE("ActivityEngine.dequeueRequest: the request is not in the queue!"
                        + requestId + ", " + requestType);
            }
        }
    }

    @Override
    public void onLoginStateChanged(boolean loggedIn) {
        mLastStatusUpdated = 0;
        mOldestStatusUpdated = 0;
    }
    

    /**
     * This method returns if the timelines have been updated with the previous "show more" call or push message. 
     * @return boolean TRUE if they timelines have been updated with the previous "show more" call or push message.
     */
    public boolean isTimelinesUpdated() {
        return mTimelinesUpdated;
    }
    
    /**
     * This method sets the state - if the timelines have been updated or not.  
     * @param timelinesUpdated boolean 
     */
    public void setTimelinesUpdated(boolean timelinesUpdated) {
        this.mTimelinesUpdated = timelinesUpdated;
    }

}
