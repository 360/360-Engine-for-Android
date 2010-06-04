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
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.DatabaseChangeType;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.content.ThumbnailHandler;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.utils.LogUtils;

/**
 * Implementation of engine handling Contact-sync. Contact sync is a multi-stage
 * process, involving sync of contacts from the native database, sync of server
 * contacts, fetching of groups and thumbnails. Each phase is handled by a
 * separate processor created and managed by the Contact sync engine.
 */
public class ContactSyncEngine extends BaseEngine implements IContactSyncCallback, NativeContactsApi.ContactsObserver {

    /**
     * Definition of states for Contact sync.
     */
    public static enum State {
        IDLE,
        FETCHING_NATIVE_CONTACTS,
        UPDATING_NATIVE_CONTACTS,
        FETCHING_SERVER_CONTACTS,
        UPDATING_SERVER_CONTACTS,
        FETCHING_SERVER_THUMBNAILS,
        UPDATING_SERVER_THUMBNAILS
    }

    /**
     * Defines the contact sync mode. The mode determines the sequence in which
     * the contact sync processors are run.
     */
    public static enum Mode {
        NONE,
        FULL_SYNC_FIRST_TIME,
        FULL_SYNC,
        SERVER_SYNC,
        THUMBNAIL_SYNC,
        FETCH_NATIVE_SYNC,
        UPDATE_NATIVE_SYNC
    }

    /**
     * Mutex for thread synchronization
     */
    private final Object mMutex = new Object();

    private final UiAgent mUiAgent = mEventCallback.getUiAgent();

    private final ApplicationCache mCache = mEventCallback.getApplicationCache();

    /**
     * Holds parameters for the UI sync request
     */
    private static class SyncParams {
        /**
         * the sync type
         */
        public boolean isFull;

        /**
         * the delay before executing the request
         */
        public long delay;

        /**
         * Constructor.
         * 
         * @param isFull true if full sync, false if only a server sync
         * @param delay in milliseconds before executing the request
         */
        public SyncParams(boolean isFull, long delay) {
            this.isFull = isFull;
            this.delay = delay;
        }
    }

    /**
     * Observer interface allowing interested parties to receive notification of
     * changes in Contact sync state.
     */
    public static interface IContactSyncObserver {
        /**
         * Called a contact sync finishes.
         * 
         * @param status SUCCESS if the sync was successful, a suitable error
         *            code otherwise.
         */
        void onSyncComplete(ServiceStatus status);

        /**
         * Called when the contact sync engine changes state or mode
         * 
         * @param mode Current mode
         * @param oldState Previous state
         * @param newState New state
         */
        void onContactSyncStateChange(Mode mode, State oldState, State newState);

        /**
         * Called to update interested parties on contact sync progress. This is
         * made up of two parts the state and the percentage. Each time the
         * state changes the percentage value will go back to 0.
         * 
         * @param currentState What the contact sync engine is currently doing
         * @param percent Percentage complete for the current task
         */
        void onProgressEvent(State currentState, int percent);
    }

    /**
     * Number of retries when first time sync fails
     */
    private static final long FULL_SYNC_RETRIES = 3;

    /**
     * Counter for first time sync failures
     */
    private int mFullSyncRetryCount = 0;

    /**
     * Current state of the contact sync engine (determines which processor is
     * currently active)
     */
    private State mState = State.IDLE;

    /**
     * Current mode (or stragegy) the contact sync engine is in. The mode
     * determines the order which the processors are run.
     */
    private Mode mMode = Mode.NONE;

    /**
     * A failure list (currently containing unlocalised English strings) of
     * contacts which could not be sync'ed to the server.
     */
    private String mFailureList;

    /**
     * Database changed flag. Will be set to true if at any stage of the contact
     * sync the NowPlus database is changed.
     */
    private boolean mDatabaseChanged;

    /**
     * Last time the database was updated (in milliseconds)
     */
    private Long mLastDbUpdateTime;

    /**
     * DatabaseHelper object used for accessing NowPlus database.
     */
    private DatabaseHelper mDb;

    /**
     * Currently active processor (the processor which is running) or null
     */
    private BaseSyncProcessor mActiveProcessor;

    /**
     * The factory class which is used for creating processors for a particular
     * state.
     */
    private ProcessorFactory mProcessorFactory;

    /**
     * Time to wait after the user modifies a contact before a contact sync with
     * the server will be initiated (in milliseconds). The timer will be reset
     * each time a modification takes place.
     */
    private static final long SERVER_CONTACT_SYNC_TIMEOUT_MS = 30000L;

    /**
     * Time to wait after the user modifies a contact before a contact sync with
     * the native database will be initiated (in milliseconds). The timer will
     * be reset each time a modification takes place.
     */
    private static final long NATIVE_CONTACT_SYNC_TIMEOUT_MS = 30000L;

    /**
     * The time to wait before requesting a new server sync when the user is
     * using the application.
     */
    private static final long USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS = 20 * 60 * 1000;

    /**
     * Determines the time that should be waited between publishing database
     * change events. This is to prevent the UI updating too frequently during a
     * contact sync. The time is specified in nanoseconds.
     */
    private static final long UI_REFRESH_WAIT_TIME_NANO = 5000000000L;

    /**
     * Specifies the time that a server sync should be started relative to
     * current time in milliseconds. Will be NULL when a server sync timeout is
     * not required.
     */
    private volatile Long mServerSyncTimeout;

    /**
     * Specifies the time that a fetch native sync should be started relative to
     * current time in milliseconds. Will be NULL when a fetch native sync
     * timeout is not required.
     */
    private volatile Long mFetchNativeSyncTimeout;

    /**
     * Specifies the time that a update native sync should be started relative
     * to current time in milliseconds. Will be NULL when a update native sync
     * timeout is not required.
     */
    private volatile Long mUpdateNativeSyncTimeout;

    /**
     * Keeps track of the last time a server sync happened.
     */
    private Long mLastServerSyncTime = 0L;

    /**
     * The content resolver object mainly used for accessing the native database
     */
    private ContentResolver mCr = null;

    /**
     * The context of the People service
     */
    private Context mContext = null;

    /**
     * Flag which matches the persisted equivalent in the NowPlus database state
     * table. Will be set to true when the first time sync is completed and will
     * remain true until a remove user data is performed.
     */
    private boolean mFirstTimeSyncComplete;

    /**
     * Flag which matches the persisted equivalent in the NowPlus database state
     * table. Will be set to true when the first time sync is started and will
     * remain true until a remove user data is performed.
     */
    private boolean mFirstTimeSyncStarted;

    /**
     * Flag which matches the persisted equivalent in the NowPlus database state
     * table. Will be set to true when the part of the first time sync to fetch
     * native contacts is started and will remain true until a remove user data
     * is performed. Once this flag has been set to true, the next time a full
     * sync is started the full sync normal mode will be used instead of full
     * sync first time.
     */
    private volatile boolean mFirstTimeNativeSyncComplete;

    /**
     * True if a full sync should be started as soon as possible
     */
    private boolean mFullSyncRequired;

    /**
     * True if a server sync should be started as soon as possible
     */
    private boolean mServerSyncRequired;

    /**
     * True if a native fetch sync should be started as soon as possible
     */
    private boolean mNativeFetchSyncRequired;

    /**
     * True if a native update sync should be started as soon as possible
     */
    private boolean mNativeUpdateSyncRequired;

    /**
     * True if a server thumbnail (avatar) sync should be started as soon as
     * possible
     */
    private boolean mThumbnailSyncRequired;

    /**
     * True if the current contact sync is being cancelled. This can happen when
     * the user performs a remove user data to prevent the database being
     * updated by a contact sync during deletion.
     */
    private boolean mCancelSync = false;

    /**
     * The following flag is set when the user initiates a remove user data. The
     * contact sync must be stopped.
     */
    private boolean mRemoveUserData = false;

    /**
     * Maintains a list of contact sync observers
     */
    private final ArrayList<IContactSyncObserver> mEventCallbackList = new ArrayList<IContactSyncObserver>();

    /**
     * Current progress value (used to check if the progress has changed)
     */
    private int mCurrentProgressPercent = 0;

    /**
     * Flag which is set when the current processor changes the database
     */
    private boolean mDbChangedByProcessor;

    /**
     * Backup of the previous active request before processing the new one.
     */
    private ServiceUiRequest mActiveUiRequestBackup = null;

    /**
     * Native Contacts API access. The appropriate version should be used
     * depending on the SDK.
     */
    private final NativeContactsApi mNativeContactsApi = NativeContactsApi.getInstance();

    /**
     * Used to listen for NowPlus database change events. Such events will be
     * received when the user modifies a contact in the people application.
     */
    private final Handler mDbChangeHandler = new Handler() {
        /**
         * Processes a database change event
         */
        @Override
        public void handleMessage(Message msg) {
            processDbMessage(msg);
        }
    };

    /**
     * ContactSyncEngine constructor.
     * 
     * @param eventCallback Engine-event callback interface allowing engine to
     *            report back to client on request completion.
     * @param context Context.
     * @param db Handle to People database.
     * @param processorFactory the processor factory
     */
    public ContactSyncEngine(IEngineEventCallback eventCallback, Context context,
            DatabaseHelper db, ProcessorFactory processorFactory) {
        super(eventCallback);
        mDb = db;
        mEngineId = EngineId.CONTACT_SYNC_ENGINE;
        mContext = context;
        // use standard processor factory if provided one is null
        mProcessorFactory = (processorFactory == null) ? new DefaultProcessorFactory()
                : processorFactory;
    }

    /**
     * Called after the engine has been created to do some extra initialisation.
     */
    @Override
    public void onCreate() {
        mDb.addEventCallback(mDbChangeHandler);
        mCr = mContext.getContentResolver();
        PersistSettings setting1 = mDb.fetchOption(PersistSettings.Option.FIRST_TIME_SYNC_STARTED);
        PersistSettings setting2 = mDb.fetchOption(PersistSettings.Option.FIRST_TIME_SYNC_COMPLETE);
        PersistSettings setting3 = mDb
                .fetchOption(PersistSettings.Option.FIRST_TIME_NATIVE_SYNC_COMPLETE);
        if (setting1 != null) {
            mFirstTimeSyncStarted = setting1.getFirstTimeSyncStarted();
        }
        if (setting2 != null) {
            mFirstTimeSyncComplete = setting2.getFirstTimeSyncComplete();
        }
        if (setting3 != null) {
            mFirstTimeNativeSyncComplete = setting3.getFirstTimeNativeSyncComplete();
        }

        if (Settings.ENABLE_FETCH_NATIVE_CONTACTS_ON_CHANGE) {
            mNativeContactsApi.registerObserver(this);
        }
        if (mFirstTimeSyncComplete) {
            // native sync shall be performed only if the first time sync has
            // been completed
            startUpdateNativeContactSyncTimer();
            startFetchNativeContactSyncTimer();
        }
    }

    /**
     * Called just before engine is about to be closed. Cleans up resources.
     */
    @Override
    public void onDestroy() {
        if (Settings.ENABLE_FETCH_NATIVE_CONTACTS_ON_CHANGE) {
            mNativeContactsApi.unregisterObserver();
        }
        mDb.removeEventCallback(mDbChangeHandler);
    }

    /**
     * Triggers a full contact sync from the UI (via the service interface).
     * Will start a first time sync if necessary, otherwise a normal full sync
     * will be executed. A {@link ServiceUiRequest#NOWPLUSSYNC} event will be
     * sent to notify the UI when the sync has completed.
     */
    public void addUiStartFullSync() {
        LogUtils.logI("ContactSyncEngine.addUiStartFullSync()");
        final SyncParams params = new SyncParams(true, 0);
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.NOWPLUSSYNC, params);
    }

    
    /**
     * Triggers a server contact sync from the UI (via service interface). Only
     * the contacts will be updated, not the me profile.
     * 
     * @param delay the delay in milliseconds from now when the sync should
     *            start
     */
    public void addUiStartServerSync(long delay) {
        LogUtils.logI("ContactSyncEngine.addUiStartServerSync(delay=" + delay + ")");
        synchronized (this) {
            if ((mMode == Mode.FULL_SYNC_FIRST_TIME || mMode == Mode.FULL_SYNC || mMode == Mode.SERVER_SYNC)
                    && mState != State.IDLE) {
                // Already performing a server sync or full sync, just ignore
                // the request
                return;
            } else if (mServerSyncTimeout != null) {
                final long currentDelay = mServerSyncTimeout.longValue()
                        - System.currentTimeMillis();
                LogUtils.logD("delay=" + delay + ", currentDelay=" + currentDelay);
                if (currentDelay < delay) {
                    // a timer on the server sync with an earlier time is
                    // already set, just ignore the request
                    return;
                }
            }
        }
        final SyncParams params = new SyncParams(false, delay);
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.NOWPLUSSYNC, params);
    }

    /**
     * Tells the ContactSyncEngine that the user is actively using the service
     * and adjust sync timeout accordingly. Note: A server sync should occur
     * every 20 minutes during application intensive usage or immediately if the
     * application is used again after sleeping more than 20 minutes.
     */
    public void pingUserActivity() {
        LogUtils.logI("ContactSyncEngine.pingUserActivity()");
        long delay;
        synchronized (this) {
            final long currentDelay = System.currentTimeMillis() - mLastServerSyncTime.longValue();
            if ((mMode == Mode.FULL_SYNC_FIRST_TIME || mMode == Mode.FULL_SYNC || mMode == Mode.SERVER_SYNC)
                    && mState != State.IDLE) {
                // Already performing a sync, scheduling a new one in
                // USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS
                delay = USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS;
            } else if (currentDelay >= USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS) {
                // Last sync timeout has passed, schedule a new one now
                delay = 0;
            } else if ((currentDelay < USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS)
                    && (mServerSyncTimeout == null)) {
                // Last sync timeout has not passed but no new one is scheduled,
                // schedule one to happen accordingly with the timeout
                delay = USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS - currentDelay;
            } else {
                // Nothing to do, a timeout will trigger the new sync
                LogUtils.logD("A new sync is already scheduled in "
                        + (USER_ACTIVITY_SERVER_SYNC_TIMEOUT_MS - currentDelay) + " milliseconds");
                return;
            }
        }

        // TODO: check if we should call directly
        // startServerContactSyncTimer(delay) instead
        LogUtils.logD("Scheduling a new sync in " + delay + " milliseconds");
        final SyncParams params = new SyncParams(false, delay);
        if (delay == 0) { // AA: I added it
            emptyUiRequestQueue();
        }

        addUiRequestToQueue(ServiceUiRequest.NOWPLUSSYNC, params);
    }

    /**
     * Determines if the first time contact sync has been completed.
     * 
     * @return true if completed.
     */
    public synchronized boolean isFirstTimeSyncComplete() {
        return mFirstTimeSyncComplete;
    }
    
    
 // TODO: RE-ENABLE SYNC VIA SYSTEM
//    /**
//     * Check if syncing is ongoing
//     * @return true if syncing is ongoing, false if it is not
//     */
//    public synchronized boolean isSyncing() {
//        return mMode != Mode.NONE;
//    }

    /**
     * Add observer of Contact-sync.
     * 
     * @param observer IContactSyncObserver handle.
     */
    public synchronized void addEventCallback(IContactSyncObserver observer) {
        if (!mEventCallbackList.contains(observer)) {
            mEventCallbackList.add(observer);
        }
    }
    
    /**
     * Starts a timer to trigger a server contact sync in a short while
     * (normally around 30 seconds).
     */
    private void startServerContactSyncTimer(long delay) {
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            return;
        }
        synchronized (this) {
            if (mServerSyncTimeout == null) {
                LogUtils
                        .logI("ContactSyncEngine - will sync contacts with server shortly... (in about "
                                + delay + " milliseconds)");
            }
            mServerSyncTimeout = System.currentTimeMillis() + delay;
            if (mCurrentTimeout == null || mCurrentTimeout.compareTo(mServerSyncTimeout) > 0) {
                mCurrentTimeout = mServerSyncTimeout;
            }
        }
        if (mCurrentTimeout.equals(mServerSyncTimeout)) {
            mEventCallback.kickWorkerThread();
        }
    }

    /**
     * Starts a timer to trigger a fetch native contact sync in a short while
     * (normally around 30 seconds).
     */
    private void startFetchNativeContactSyncTimer() {
        if (!Settings.ENABLE_FETCH_NATIVE_CONTACTS) {
            return;
        }
        synchronized (this) {
            if (mFetchNativeSyncTimeout == null) {
                LogUtils.logI("ContactSyncEngine - will fetch native contacts shortly...");
            }
            mFetchNativeSyncTimeout = System.currentTimeMillis() + NATIVE_CONTACT_SYNC_TIMEOUT_MS;
            if (mCurrentTimeout == null || mCurrentTimeout.compareTo(mFetchNativeSyncTimeout) > 0) {
                mCurrentTimeout = mFetchNativeSyncTimeout;
                mEventCallback.kickWorkerThread();
            }
        }
    }

    /**
     * Starts a timer to trigger a update native contact sync in a short while
     * (normally around 30 seconds).
     */
    private void startUpdateNativeContactSyncTimer() {
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            return;
        }
        synchronized (this) {
            if (mUpdateNativeSyncTimeout == null) {
                LogUtils.logI("ContactSyncEngine - will update native contacts shortly...");
            }
            mUpdateNativeSyncTimeout = System.currentTimeMillis() + NATIVE_CONTACT_SYNC_TIMEOUT_MS;
            if (mCurrentTimeout == null || mCurrentTimeout.compareTo(mUpdateNativeSyncTimeout) > 0) {
                mCurrentTimeout = mUpdateNativeSyncTimeout;
            }
        }
        if (mCurrentTimeout.equals(mUpdateNativeSyncTimeout)) {
            mEventCallback.kickWorkerThread();
        }
    }

    /**
     * Helper function to start a processor running.
     * 
     * @param processor Processor which was created by processor factory.
     */
    private void startProcessor(BaseSyncProcessor processor) {
        if (mActiveProcessor != null) {
            LogUtils.logE("ContactSyncEngine.startProcessor - Cannot start " + processor.getClass()
                    + ", because the processor " + mActiveProcessor.getClass() + " is running");
            throw new RuntimeException(
                    "ContactSyncEngine - Cannot start processor while another is active");
        }
        mActiveProcessor = processor;
        mCurrentProgressPercent = -1;
        mDbChangedByProcessor = false;
        processor.start();
    }

    /**
     * Framework function to determine when the contact sync engine next needs
     * to run.
     * 
     * @return -1 if the engine does not need to run 0 if the engine needs to
     *         run as soon as possible x where x > 0, the engine needs to run
     *         when current time in milliseconds >= x
     */
    @Override
    public long getNextRunTime() {

        if (isCommsResponseOutstanding()) {
            return 0;
        }
        if (isUiRequestOutstanding() && mActiveUiRequest == null) {
            return 0;
        }
        if (readyToStartServerSync()) {
            if (mFullSyncRequired || mServerSyncRequired || mThumbnailSyncRequired) {
                return 0;
            } else if (mFirstTimeSyncStarted && !mFirstTimeSyncComplete
                    && mFullSyncRetryCount < FULL_SYNC_RETRIES) {
                mFullSyncRetryCount++;
                mFullSyncRequired = true;
                return 0;
            }
        }
        if (mNativeFetchSyncRequired && readyToStartFetchNativeSync()) {
            return 0;
        }
        if (mNativeUpdateSyncRequired && readyToStartUpdateNativeSync()) {
            return 0;
        }
        synchronized (this) {
            if (mCancelSync) {
                return 0;
            }
        }
        return getCurrentTimeout();
    }

    /**
     * Called by framework when {@link #getNextRunTime()} reports that the
     * engine needs to run, to carry out the next task. Each task should not
     * take longer than a second to complete.
     */
    @Override
    public void run() {
        synchronized (this) {
            if (mCancelSync) {
                cancelSync();
                mCancelSync = false;
                if (mRemoveUserData) {
                    mFirstTimeSyncStarted = false;
                    mFirstTimeSyncComplete = false;
                    mFirstTimeNativeSyncComplete = false;
                    mRemoveUserData = false;
                    super.onReset();
                }
                return;
            }
        }
        if (processTimeout()) {
            return;
        }
        if (isUiRequestOutstanding()) {
            mActiveUiRequestBackup = mActiveUiRequest;
            if (processUiQueue()) {
                return;
            }
        }
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }
        if (readyToStartServerSync()) {
            if (mThumbnailSyncRequired) {
                startThumbnailSync();
                return;
            }
            if (mFullSyncRequired) {
                startFullSync();
                return;
            }
            if (mServerSyncRequired) {
                startServerSync();
                return;
            }
        }

        if (mNativeFetchSyncRequired && readyToStartFetchNativeSync()) {
            startFetchNativeSync();
            return;
        }
        if (mNativeUpdateSyncRequired && readyToStartUpdateNativeSync()) {
            startUpdateNativeSync();
            return;
        }
    }

    /**
     * Called by base class when a contact sync UI request has been completed.
     * Not currently used.
     */
    @Override
    protected void onRequestComplete() {
    }

    /**
     * Called by base class when a timeout has been completed. If there is an
     * active processor the timeout event will be passed to it, otherwise the
     * engine will check if it needs to schedule a new sync and set the next
     * pending timeout.
     */
    @Override
    protected void onTimeoutEvent() {
        if (mActiveProcessor != null) {
            mActiveProcessor.onTimeoutEvent();
        } else {
            startSyncIfRequired();
            setTimeoutIfRequired();
        }
    }

    /**
     * Based on current timeout values schedules a new sync if required.
     */
    private void startSyncIfRequired() {
        if (mFirstTimeSyncStarted && !mFirstTimeSyncComplete) {
            mFullSyncRequired = true;
            mFullSyncRetryCount = 0;
        }
        long currentTimeMs = System.currentTimeMillis();
        if (mServerSyncTimeout != null && mServerSyncTimeout.longValue() < currentTimeMs) {
            mServerSyncRequired = true;
            mServerSyncTimeout = null;
        } else if (mFetchNativeSyncTimeout != null
                && mFetchNativeSyncTimeout.longValue() < currentTimeMs) {
            mNativeFetchSyncRequired = true;
            mFetchNativeSyncTimeout = null;
        } else if (mUpdateNativeSyncTimeout != null
                && mUpdateNativeSyncTimeout.longValue() < currentTimeMs) {
            mNativeUpdateSyncRequired = true;
            mUpdateNativeSyncTimeout = null;
        }
    }

    /**
     * Called when a response to a request or a push message is received from
     * the server. Push messages are processed by the engine, responses are
     * passed to the active processor.
     * 
     * @param resp Response or push message received
     */
    @Override
    protected void processCommsResponse(Response resp) {
        if (processPushEvent(resp)) {
            return;
        }
        if (resp.mDataTypes != null && resp.mDataTypes.size() > 0) {
            LogUtils.logD("ContactSyncEngine.processCommsResponse: Req ID = " + resp.mReqId
                    + ", type = " + resp.mDataTypes.get(0).name());
        } else {
            LogUtils.logD("ContactSyncEngine.processCommsResponse: Req ID = " + resp.mReqId
                    + ", type = NULL");
        }
        if (mActiveProcessor != null) {
            mActiveProcessor.processCommsResponse(resp);
        }
    }

    /**
     * Determines if a given response is a push message and processes in this
     * case TODO: we need the check for Me Profile be migrated to he new engine
     * 
     * @param resp Response to check and process
     * @return true if the response was processed
     */
    private boolean processPushEvent(Response resp) {
        if (resp.mDataTypes == null || resp.mDataTypes.size() == 0) {
            return false;
        }
        BaseDataType dataType = resp.mDataTypes.get(0);
        if ((dataType == null) || !dataType.name().equals("PushEvent")) {
            return false;
        }
        PushEvent pushEvent = (PushEvent)dataType;
        LogUtils.logV("Push Event Type = " + pushEvent.mMessageType);
        switch (pushEvent.mMessageType) {
            case CONTACTS_CHANGE:
                LogUtils
                        .logI("ContactSyncEngine.processCommsResponse - Contacts changed push message received");
                mServerSyncRequired = true;
                EngineManager.getInstance().getGroupsEngine().addUiGetGroupsRequest(); // fetch the newest groups
                mEventCallback.kickWorkerThread();
                break;
            case SYSTEM_NOTIFICATION:
                LogUtils
                        .logI("ContactSyncEngine.processCommsResponse - System notification push message received");
                if (mState == State.FETCHING_SERVER_THUMBNAILS) {
                    mActiveProcessor.processCommsResponse(resp);
                }
                break;
            default:
                // do nothing.
                break;
        }
        return true;
    }

    /**
     * Called by base class to process a NOWPLUSSYNC UI request. This will be
     * called to process a full sync or server sync UI request.
     * 
     * @param requestId ID of the request to process, only
     *            ServiceUiRequest.NOWPLUSSYNC is currently supported.
     * @param data Type is determined by request ID, in case of NOWPLUSSYNC this
     *            is a flag which determines if a full sync is required (true =
     *            full sync, false = server sync).
     */
    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        switch (requestId) {
            case NOWPLUSSYNC:
                final SyncParams params = (SyncParams)data;
                if (params.isFull) {
                    // delayed full sync is not supported currently, start it
                    // immediately
                    clearCurrentSyncAndPatchBaseEngine();
                    mFullSyncRetryCount = 0;
                    startFullSync();
                } else {
                    if (params.delay <= 0) {
                        clearCurrentSyncAndPatchBaseEngine();
                        if (readyToStartServerSync()) {
                            startServerSync();
                        } else {
                            mServerSyncRequired = true;
                        }
                    } else {
                        startServerContactSyncTimer(params.delay);
                    }
                }
                break;
            default:
                // do nothing.
                break;
        }
    }

    /**
     * Called by the run function when the {@link #mCancelSync} flag is set to
     * true. Cancels the active processor and completes the current UI request.
     */
    private void cancelSync() {
        if (mActiveProcessor != null) {
            mActiveProcessor.cancel();
            mActiveProcessor = null;
        }
        completeSync(ServiceStatus.USER_CANCELLED);
    }

    /**
     * Clears the current sync and make sure that if we cancel a previous sync,
     * it doesn't notify a wrong UI request. TODO: Find another way to not have
     * to hack the BaseEngine!
     */
    private void clearCurrentSyncAndPatchBaseEngine() {
        // Cancel background sync
        if (mActiveProcessor != null) {
            // the mActiveUiRequest is already the new one so if
            // onCompleteUiRequest(Error) is called,
            // this will reset it to null even if we didn't start to process it.
            ServiceUiRequest newActiveUiRequest = mActiveUiRequest;
            mActiveUiRequest = mActiveUiRequestBackup;
            mActiveProcessor.cancel();
            // restore the active UI request...
            mActiveUiRequest = newActiveUiRequest;
            mActiveProcessor = null;
        }
        newState(State.IDLE);
    }

    /**
     * Checks if a server sync can be started based on network conditions and
     * engine state
     * 
     * @return true if a sync can be started, false otherwise.
     */
    private boolean readyToStartServerSync() {
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            return false;
        }
        if (!EngineManager.getInstance().getSyncMeEngine().isFirstTimeMeSyncComplete()) {
            return false;
        }
        if (!mFirstTimeSyncStarted) {
            return false;
        }
        if (mState != State.IDLE || NetworkAgent.getAgentState() != AgentState.CONNECTED) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a fetch native sync can be started based on network conditions
     * and engine state
     * 
     * @return true if a sync can be started, false otherwise.
     */
    private boolean readyToStartFetchNativeSync() {
        if (!Settings.ENABLE_FETCH_NATIVE_CONTACTS) {
            return false;
        }
        if (!mFirstTimeSyncStarted) {
            return false;
        }
        if (mState != State.IDLE) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a update native sync can be started based on network conditions
     * and engine state
     * 
     * @return true if a sync can be started, false otherwise.
     */
    private boolean readyToStartUpdateNativeSync() {
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            return false;
        }
        if (!mFirstTimeSyncStarted) {
            return false;
        }
        if (mState != State.IDLE) {
            return false;
        }
        return true;
    }

    /**
     * Starts a full sync. If the native contacts haven't yet been fetched then
     * a first time sync will be started, otherwise will start a normal full
     * sync. Full syncs are always initiated from the UI (via UI request).
     */
    public void startFullSync() {
        mFailureList = "";
        mDatabaseChanged = false;
        setFirstTimeSyncStarted(true);
        mServerSyncTimeout = null;
        mFullSyncRequired = false;
        if (mFirstTimeNativeSyncComplete) {
            LogUtils.logI("ContactSyncEngine.startFullSync - user triggered sync");
            mMode = Mode.FULL_SYNC;
            nextTaskFullSyncNormal();
        } else {
            LogUtils.logI("ContactSyncEngine.startFullSync - first time sync");
            mMode = Mode.FULL_SYNC_FIRST_TIME;
            nextTaskFullSyncFirstTime();
        }
    }

    /**
     * Starts a server sync. This will only sync contacts with the server and
     * will not include the me profile. Thumbnails are not fetched as part of
     * this sync, but will be fetched as part of a background sync afterwards.
     */
    private void startServerSync() {
        mServerSyncRequired = false;
        mFailureList = "";
        mDatabaseChanged = false;
        mMode = Mode.SERVER_SYNC;
        mServerSyncTimeout = null;
        setTimeoutIfRequired();
        nextTaskServerSync();
    }

    /**
     * Starts a background thumbnail sync
     */
    private void startThumbnailSync() {
        mThumbnailSyncRequired = false;
        mFailureList = "";
        mDatabaseChanged = false;
        mMode = Mode.THUMBNAIL_SYNC;
        nextTaskThumbnailSync();
    }

    /**
     * Starts a background fetch native contacts sync
     */
    private void startFetchNativeSync() {
        mNativeFetchSyncRequired = false;
        mFailureList = "";
        mDatabaseChanged = false;
        mMode = Mode.FETCH_NATIVE_SYNC;
        mFetchNativeSyncTimeout = null;
        setTimeoutIfRequired();
        nextTaskFetchNativeContacts();
    }

    /**
     * Starts a background update native contacts sync
     */
    private void startUpdateNativeSync() {
        mNativeUpdateSyncRequired = false;
        mFailureList = "";
        mDatabaseChanged = false;
        mMode = Mode.UPDATE_NATIVE_SYNC;
        mUpdateNativeSyncTimeout = null;
        setTimeoutIfRequired();
        nextTaskUpdateNativeContacts();
    }

    /**
     * Helper function to start the fetch native contacts processor
     * 
     * @return if this type of sync is enabled in the settings, false otherwise.
     */
    private boolean startFetchNativeContacts() {
        if (Settings.ENABLE_FETCH_NATIVE_CONTACTS) {
            newState(State.FETCHING_NATIVE_CONTACTS);
            startProcessor(mProcessorFactory.create(ProcessorFactory.FETCH_NATIVE_CONTACTS, this,
                    mDb, mContext, mCr));
            return true;
        }
        return false;
    }

    /**
     * Helper function to start the update native contacts processor
     * 
     * @return if this type of sync is enabled in the settings, false otherwise.
     */
    private boolean startUpdateNativeContacts() {
        if (Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            newState(State.UPDATING_NATIVE_CONTACTS);
            startProcessor(mProcessorFactory.create(ProcessorFactory.UPDATE_NATIVE_CONTACTS, this,
                    mDb, null, mCr));
            return true;
        }
        return false;
    }

    /**
     * Helper function to start the download server contacts processor
     * 
     * @return if this type of sync is enabled in the settings, false otherwise.
     */
    private boolean startDownloadServerContacts() {
        if (Settings.ENABLE_SERVER_CONTACT_SYNC) {
            newState(State.FETCHING_SERVER_CONTACTS);
            startProcessor(mProcessorFactory.create(ProcessorFactory.DOWNLOAD_SERVER_CONTACTS,
                    this, mDb, mContext, null));
            return true;
        }
        return false;
    }

    /**
     * Helper function to start the upload server contacts processor
     * 
     * @return if this type of sync is enabled in the settings, false otherwise.
     */
    private boolean startUploadServerContacts() {
        if (Settings.ENABLE_SERVER_CONTACT_SYNC) {
            newState(State.UPDATING_SERVER_CONTACTS);
            startProcessor(mProcessorFactory.create(ProcessorFactory.UPLOAD_SERVER_CONTACTS, this,
                    mDb, mContext, null));
            return true;
        }
        return false;
    }

    /**
     * Helper function to start the download thumbnails processor
     * 
     * @return if this type of sync is enabled in the settings, false otherwise.
     */
    private boolean startDownloadServerThumbnails() {
        if (Settings.ENABLE_THUMBNAIL_SYNC) {
            newState(State.FETCHING_SERVER_THUMBNAILS);
            ThumbnailHandler.getInstance().downloadContactThumbnails(mDb.fetchContactList());
            return true;
        }
        return false;
    }

    /**
     * Helper function to start the upload thumbnails processor
     * 
     * @return if this type of sync is enabled in the settings, false otherwise.
     */
    private boolean startUploadServerThumbnails() {
        if (Settings.ENABLE_THUMBNAIL_SYNC) {
            newState(State.UPDATING_SERVER_THUMBNAILS);
            startProcessor(mProcessorFactory.create(ProcessorFactory.UPLOAD_SERVER_THUMBNAILS,
                    this, mDb, mContext, null));
            return true;
        }
        return false;
    }

    /**
     * Called by a processor when it has completed. Will move to the next task.
     * When the active contact sync has totally finished, will complete any
     * pending UI request.
     * 
     * @param status Status of the sync from the processor, any error codes will
     *            stop the sync.
     * @param failureList Contains a list of sync failure information which can
     *            be used as a summary at the end. Otherwise should be an empty
     *            string.
     * @param data Any processor specific data to pass back to the engine. Not
     *            currently used.
     */
    @Override
    public void onProcessorComplete(ServiceStatus status, String failureList, Object data) {
        if (mState == State.IDLE) {
            return;
        }
        if (mActiveProcessor != null) {
            mActiveProcessor.onComplete();
        }
        mActiveProcessor = null;
        mFailureList += failureList;
        if (status != ServiceStatus.SUCCESS) {
            LogUtils.logE("ContactSyncEngine.onProcessorComplete - Failed during " + mState
                    + " with error " + status);
            completeSync(status);
            return;
        }
        if (mDbChangedByProcessor) {
            switch (mState) {
                case FETCHING_NATIVE_CONTACTS:
                    mServerSyncRequired = true;
                    break;
                case FETCHING_SERVER_CONTACTS:
                    mThumbnailSyncRequired = true;
                    mNativeUpdateSyncRequired = true;
                    break;
                default:
                    // Do nothing.
                    break;
            }
        }
        switch (mMode) {
            case FULL_SYNC_FIRST_TIME:
                nextTaskFullSyncFirstTime();
                break;
            case FULL_SYNC:
                nextTaskFullSyncNormal();
                break;
            case SERVER_SYNC:
                nextTaskServerSync();
                break;
            case FETCH_NATIVE_SYNC:
                nextTaskFetchNativeContacts();
                break;
            case UPDATE_NATIVE_SYNC:
                nextTaskUpdateNativeContacts();
                break;
            case THUMBNAIL_SYNC:
                nextTaskThumbnailSync();
                break;
            default:
                LogUtils.logE("ContactSyncEngine.onProcessorComplete - Unexpected mode: " + mMode);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Moves to the next state for the full sync first time mode, and runs the
     * appropriate processor. Completes the UI request when the sync is complete
     * (if one is pending).
     */
    private void nextTaskFullSyncFirstTime() {
        switch (mState) {
            case IDLE:
                if (startDownloadServerContacts()) {
                    return;
                }
                // Fall through
            case FETCHING_SERVER_CONTACTS:
                if (startFetchNativeContacts()) {
                    return;
                }
                // Fall through
            case FETCHING_NATIVE_CONTACTS:
                 setFirstTimeNativeSyncComplete(true);
                if (startUploadServerContacts()) {
                    return;
                }
                // Fall through
            case UPDATING_SERVER_CONTACTS:
                // force a thumbnail sync in case nothing in the database
                // changed but we still have failing
                // thumbnails that we should retry to download
                mThumbnailSyncRequired = true;
                mLastServerSyncTime = System.currentTimeMillis();
                setFirstTimeSyncComplete(true);
                completeSync(ServiceStatus.SUCCESS);
                return;
            default:
                LogUtils.logE("ContactSyncEngine.nextTaskFullSyncFirstTime - Unexpected state: "
                        + mState);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Moves to the next state for the full sync normal mode, and runs the
     * appropriate processor. Completes the UI request when the sync is complete
     * (if one is pending).
     */
    private void nextTaskFullSyncNormal() {
        switch (mState) {
            case IDLE:
                if (startDownloadServerContacts()) {
                    return;
                }
                // Fall through
            case FETCHING_SERVER_CONTACTS:
                if (startUploadServerContacts()) {
                    return;
                }
                // Fall through
            case UPDATING_SERVER_CONTACTS:
                // force a thumbnail sync in case nothing in the database
                // changed but we still have failing
                // thumbnails that we should retry to download
                mThumbnailSyncRequired = true;
                mLastServerSyncTime = System.currentTimeMillis();
                setFirstTimeSyncComplete(true);
                completeSync(ServiceStatus.SUCCESS);
                return;
            default:
                LogUtils.logE("ContactSyncEngine.nextTaskFullSyncNormal - Unexpected state: "
                        + mState);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Moves to the next state for the fetch native contacts mode, and runs the
     * appropriate processor. Completes the UI request when the sync is complete
     * (if one is pending).
     */
    private void nextTaskFetchNativeContacts() {
        switch (mState) {
            case IDLE:
                if (startFetchNativeContacts()) {
                    return;
                }
                // Fall through
            case FETCHING_NATIVE_CONTACTS:
                if (startUploadServerContacts()) {
                    return;
                }
                // Fall through
            case UPDATING_SERVER_CONTACTS:
                completeSync(ServiceStatus.SUCCESS);
                return;
            default:
                LogUtils.logE("ContactSyncEngine.nextTaskFetchNativeContacts - Unexpected state: "
                        + mState);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Moves to the next state for the update native contacts mode, and runs the
     * appropriate processor. Completes the UI request when the sync is complete
     * (if one is pending).
     */
    private void nextTaskUpdateNativeContacts() {
        switch (mState) {
            case IDLE:
                if (startUpdateNativeContacts()) {
                    return;
                }
                completeSync(ServiceStatus.SUCCESS);
                return;
                // Fall through
            case UPDATING_NATIVE_CONTACTS:
                completeSync(ServiceStatus.SUCCESS);
                return;
            default:
                LogUtils.logE("ContactSyncEngine.nextTaskUpdateNativeContacts - Unexpected state: "
                        + mState);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Moves to the next state for the server sync mode, and runs the
     * appropriate processor. Completes the UI request when the sync is complete
     * (if one is pending).
     */
    private void nextTaskServerSync() {
        switch (mState) {
            case IDLE:
                if (startDownloadServerContacts()) {
                    return;
                }
                // Fall through
            case FETCHING_SERVER_CONTACTS:
                if (startUploadServerContacts()) {
                    return;
                }
                // Fall through
            case UPDATING_SERVER_CONTACTS:
                // force a thumbnail sync in case nothing in the database
                // changed but we still have failing
                // thumbnails that we should retry to download
                mThumbnailSyncRequired = true;
                mLastServerSyncTime = System.currentTimeMillis();
                completeSync(ServiceStatus.SUCCESS);
                return;
            default:
                LogUtils.logE("ContactSyncEngine.nextTaskServerSync - Unexpected state: " + mState);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Moves to the next state for the thumbnail sync mode, and runs the
     * appropriate processor. Completes the UI request when the sync is complete
     * (if one is pending).
     */
    private void nextTaskThumbnailSync() {
        switch (mState) {
            case IDLE:
                if (startDownloadServerThumbnails()) {
                    return;
                }
                // Fall through
            case FETCHING_SERVER_THUMBNAILS:
                if (startUploadServerThumbnails()) {
                    return;
                }
                // Fall through
            case UPDATING_SERVER_THUMBNAILS:
                completeSync(ServiceStatus.SUCCESS);
                return;
            default:
                LogUtils.logE("ContactSyncEngine.nextTaskThumbnailSync - Unexpected state: "
                        + mState);
                completeSync(ServiceStatus.ERROR_SYNC_FAILED);
        }
    }

    /**
     * Changes the state of the engine and informs the observers.
     * 
     * @param newState The new state
     */
    private void newState(State newState) {
        State oldState = mState;
        synchronized (mMutex) {
            if (newState == mState) {
                return;
            }
            mState = newState;
        }
        LogUtils.logV("ContactSyncEngine.newState: " + oldState + " -> " + mState);
        fireStateChangeEvent(mMode, oldState, mState);
    }

    /**
     * Called when the current mode has finished all the sync tasks. Completes
     * the UI request if one is pending, sends an event to the observer and a
     * database change event if necessary.
     * 
     * @param status The overall status of the contact sync.
     */
    private void completeSync(ServiceStatus status) {
        if (mState == State.IDLE) {
            return;
        }
        if (mDatabaseChanged) {
            LogUtils.logD("ContactSyncEngine.completeSync - Firing Db changed event");
            mDb.fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, true);
            mDatabaseChanged = false;
        }
        mActiveProcessor = null;
        newState(State.IDLE);
        mMode = Mode.NONE;
        completeUiRequest(status, mFailureList);

        mCache.setSyncStatus(new SyncStatus(status));
        mUiAgent.sendUnsolicitedUiEvent(ServiceUiRequest.UPDATE_SYNC_STATE, null);

        if (mFirstTimeSyncComplete) {
            fireSyncCompleteEvent(status);
        }

        if (ServiceStatus.SUCCESS == status) {
            startSyncIfRequired();
        }
        setTimeoutIfRequired();
    }

    /**
     * Sets the current timeout to the next pending timer and kicks the engine
     * if necessary.
     */
    private synchronized void setTimeoutIfRequired() {
        Long initTimeout = mCurrentTimeout;
        if (mCurrentTimeout == null
                || (mServerSyncTimeout != null && mServerSyncTimeout.compareTo(mCurrentTimeout) < 0)) {
            mCurrentTimeout = mServerSyncTimeout;
        }
        if (mCurrentTimeout == null
                || (mFetchNativeSyncTimeout != null && mFetchNativeSyncTimeout
                        .compareTo(mCurrentTimeout) < 0)) {
            mCurrentTimeout = mFetchNativeSyncTimeout;
        }
        if (mCurrentTimeout == null
                || (mUpdateNativeSyncTimeout != null && mUpdateNativeSyncTimeout
                        .compareTo(mCurrentTimeout) < 0)) {
            mCurrentTimeout = mUpdateNativeSyncTimeout;
        }
        if (mCurrentTimeout != null && !mCurrentTimeout.equals(initTimeout)) {
            mEventCallback.kickWorkerThread();
        }
    }

    /**
     * Called by the active processor to indicate that the NowPlus database has
     * changed.
     */
    @Override
    public void onDatabaseChanged() {
        mDatabaseChanged = true;
        mDbChangedByProcessor = true;
        final long currentTime = System.nanoTime();
        if (mLastDbUpdateTime == null
                || mLastDbUpdateTime.longValue() + UI_REFRESH_WAIT_TIME_NANO < currentTime) {
            LogUtils.logD("ContactSyncEngine.onDatabaseChanged - Updating UI...");
            mDatabaseChanged = false;
            mLastDbUpdateTime = currentTime;
            mDb.fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, true);
        }
    }

    /**
     * Used by processors to fetch this engine. The BaseEngine reference is
     * needed to send requests to the server.
     * 
     * @return The BaseEngine reference of this engine.
     */
    @Override
    public BaseEngine getEngine() {
        return this;
    }

    /**
     * Used by active processor to set a timeout.
     * 
     * @param timeout Timeout value based on current time in milliseconds
     */
    @Override
    public void setTimeout(long timeout) {
        super.setTimeout(timeout);
    }

    /**
     * Used by active processor to set the current progress.
     * 
     * @param SyncStatus Status of the processor, must not be NULL.
     * @throws InvalidParameterException when SyncStatus is NULL.
     */
    @Override
    public void setSyncStatus(final SyncStatus syncStatus) {
        if (syncStatus == null) {
            throw new InvalidParameterException(
                    "ContactSyncEngine.setSyncStatus() SyncStatus cannot be NULL");
        }

        mCache.setSyncStatus(syncStatus);
        mUiAgent.sendUnsolicitedUiEvent(ServiceUiRequest.UPDATE_SYNC_STATE, null);

        if (mState != State.IDLE && syncStatus.getProgress() != mCurrentProgressPercent) {
            mCurrentProgressPercent = syncStatus.getProgress();
            LogUtils.logI("ContactSyncEngine: Task " + mState + " is " + syncStatus.getProgress()
                    + "% complete");
            fireProgressEvent(mState, syncStatus.getProgress());
        }
    }

    /**
     * Called by active processor when issuing a request to store the request id
     * in the base engine class.
     */
    @Override
    public void setActiveRequestId(int reqId) {
        setReqId(reqId);
    }

    /**
     * Helper function to update the database when the state of the
     * {@link #mFirstTimeSyncStarted} flag changes.
     * 
     * @param value New value to the flag. True indicates that first time sync
     *            has been started. The flag is never set to false again by the
     *            engine, it will be only set to false when a remove user data
     *            is done (and the database is deleted).
     * @return SUCCESS or a suitable error code if the database could not be
     *         updated.
     */
    private ServiceStatus setFirstTimeSyncStarted(boolean value) {
        if (mFirstTimeSyncStarted == value) {
            return ServiceStatus.SUCCESS;
        }
        PersistSettings setting = new PersistSettings();
        setting.putFirstTimeSyncStarted(value);
        ServiceStatus status = mDb.setOption(setting);
        if (ServiceStatus.SUCCESS == status) {
            synchronized (this) {
                mFirstTimeSyncStarted = value;
            }
        }
        return status;
    }

    /**
     * Helper function to update the database when the state of the
     * {@link #mFirstTimeSyncComplete} flag changes.
     * 
     * @param value New value to the flag. True indicates that first time sync
     *            has been completed. The flag is never set to false again by
     *            the engine, it will be only set to false when a remove user
     *            data is done (and the database is deleted).
     * @return SUCCESS or a suitable error code if the database could not be
     *         updated.
     */
    private ServiceStatus setFirstTimeSyncComplete(boolean value) {
        if (mFirstTimeSyncComplete == value) {
            return ServiceStatus.SUCCESS;
        }
        PersistSettings setting = new PersistSettings();
        setting.putFirstTimeSyncComplete(value);
        ServiceStatus status = mDb.setOption(setting);
        if (ServiceStatus.SUCCESS == status) {
            synchronized (this) {
                mFirstTimeSyncComplete = value;
            }
        }
        return status;
    }

    /**
     * Helper function to update the database when the state of the
     * {@link #mFirstTimeNativeSyncComplete} flag changes.
     * 
     * @param value New value to the flag. True indicates that the native fetch
     *            part of the first time sync has been completed. The flag is
     *            never set to false again by the engine, it will be only set to
     *            false when a remove user data is done (and the database is
     *            deleted).
     * @return SUCCESS or a suitable error code if the database could not be
     *         updated.
     */
    private ServiceStatus setFirstTimeNativeSyncComplete(boolean value) {
        if (mFirstTimeSyncComplete == value) {
            return ServiceStatus.SUCCESS;
        }
        PersistSettings setting = new PersistSettings();
        setting.putFirstTimeNativeSyncComplete(value);
        ServiceStatus status = mDb.setOption(setting);
        if (ServiceStatus.SUCCESS == status) {
            synchronized (this) {
                mFirstTimeNativeSyncComplete = value;
            }
        }
        return status;
    }

    /**
     * Called when a database change event is received from the DatabaseHelper.
     * Only internal database change events are processed, external change
     * events are generated by the contact sync engine.
     * 
     * @param msg The message indicating the type of event
     */
    private void processDbMessage(Message message) {
        final ServiceUiRequest event = ServiceUiRequest.getUiEvent(message.what);
        switch (event) {
            case DATABASE_CHANGED_EVENT:
                if (message.arg1 == DatabaseHelper.DatabaseChangeType.CONTACTS.ordinal()
                        && message.arg2 == 0) {
                    LogUtils.logV("ContactSyncEngine.processDbMessage - Contacts have changed");
                    // startMeProfileSyncTimer();
                    startServerContactSyncTimer(SERVER_CONTACT_SYNC_TIMEOUT_MS);
                    startUpdateNativeContactSyncTimer();
                } 
                break;
            default:
                // Do nothing.
                break;
        }
    }

    /**
     * Notifies observers when a state or mode change occurs.
     * 
     * @param mode Current mode
     * @param previousState State before the change
     * @param newState State after the change.
     */
    private void fireStateChangeEvent(Mode mode, State previousState, State newState) {
        ArrayList<IContactSyncObserver> tempList = new ArrayList<IContactSyncObserver>();
        synchronized (this) {
            tempList.addAll(mEventCallbackList);
        }
        for (IContactSyncObserver observer : tempList) {
            observer.onContactSyncStateChange(mode, previousState, newState);
        }

        if (Settings.ENABLED_DATABASE_TRACE) {
            // DatabaseHelper.trace(false, "State newState[" + newState + "]" +
            // mDb.copyDatabaseToSd(""));
        }
    }

    /**
     * Notifies observers when a contact sync complete event occurs.
     * 
     * @param status SUCCESS or a suitable error code
     */
    private void fireSyncCompleteEvent(ServiceStatus status) {
        ArrayList<IContactSyncObserver> tempList = new ArrayList<IContactSyncObserver>();
        synchronized (this) {
            tempList.addAll(mEventCallbackList);
        }
        for (IContactSyncObserver observer : tempList) {
            observer.onSyncComplete(status);
        }
    }

    /**
     * Notifies observers when the progress value changes for the current sync.
     * 
     * @param currentState Current sync task being processed
     * @param percent Progress of task (between 0 and 100 percent)
     */
    private void fireProgressEvent(State currentState, int percent) {
        ArrayList<IContactSyncObserver> tempList = new ArrayList<IContactSyncObserver>();
        synchronized (this) {
            tempList.addAll(mEventCallbackList);
        }
        for (IContactSyncObserver observer : tempList) {
            observer.onProgressEvent(currentState, percent);
        }
    }

    /**
     * Called by framework to warn the engine that a remove user data is about
     * to start. Sets flags and kicks the engine.
     */
    @Override
    public void onReset() {
        synchronized (this) {
            mCancelSync = true;
            mRemoveUserData = true;
        }
        mEventCallback.kickWorkerThread();
    }

    /**
     * @see NativeContactsApi.ContactsObserver#onChange()
     */
    @Override
    public void onChange() {
        
        LogUtils.logD("ContactSyncEngine.onChange(): changes detected on native side.");
        // changes detected on native side, start the timer for the
        // FetchNativeContacts processor.
        startFetchNativeContactSyncTimer();
    }
}
