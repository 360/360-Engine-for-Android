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

package com.vodafone360.people.engine;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.activities.ActivitiesEngine;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine;
import com.vodafone360.people.engine.groups.GroupsEngine;
import com.vodafone360.people.engine.content.ContentEngine;
import com.vodafone360.people.engine.identities.IdentityEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.engine.meprofile.SyncMeEngine;
import com.vodafone360.people.engine.presence.PresenceEngine;
import com.vodafone360.people.engine.upgrade.UpgradeEngine;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.WorkerThread;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.engine.photoupload.PhotoUploadEngine;
/**
 * EngineManager class is responsible for creating, handling and deletion of
 * engines in the People client. The EngineManager determine when each engine
 * should be run based on the engine's next run time or whether there is a
 * waiting request for that engine. The EngineManager routes received responses
 * to the appropriate engine.
 */
public class EngineManager {

    /**
     * Identifiers for engines.
     */
    public enum EngineId {
        LOGIN_ENGINE,
        CONTACT_SYNC_ENGINE,
        GROUPS_ENGINE,
        ACTIVITIES_ENGINE,
        IDENTITIES_ENGINE,
        PRESENCE_ENGINE,
        UPGRADE_ENGINE,
        CONTENT_ENGINE,
        SYNCME_ENGINE,
        /**
         * Used for uploading photo.
         */
        PHOTO_UPLOAD_SHARING,
        UNDEFINED
        // add ids as we progress
        

    }

    /**
     * {@link EngineManager} is a singleton, so this is the static reference.
     */
    private static EngineManager sEngineManager;

    /**
     * Engine manager maintains a list of all engines in the system. This is a
     * map between the engine ID and the engine reference.
     */
    private final HashMap<Integer, BaseEngine> mEngineList = new HashMap<Integer, BaseEngine>();

    /**
     * Reference to the {@RemoteService} object which provides
     * access to the {@link WorkerThread}.
     */
    private RemoteService mService;

    /**
     * Engines require access the {@link IEngineEventCallback} interface.
     * Implements several useful methods for engines such as UI request
     * complete.
     */
    private IEngineEventCallback mUiEventCallback;

    /**
     * @see LoginEngine
     */
    private LoginEngine mLoginEngine;

    /**
     * @see UpgradeEngine
     */
    private UpgradeEngine mUpgradeEngine;

    /**
     * @see ActivitiesEngine
     */
    private ActivitiesEngine mActivitiesEngine;

    /**
     * @see SyncMeEngine
     */
    private SyncMeEngine mSyncMeEngine;

    /**
     * @see PresenceEngine
     */
    private PresenceEngine mPresenceEngine;

    /**
     * @see IdentityEngine
     */
    private IdentityEngine mIdentityEngine;

    /**
     * @see ContactSyncEngine
     */
    private ContactSyncEngine mContactSyncEngine;
    
    /**
     * @see GroupsEngine
     */
    private GroupsEngine mGroupsEngine;

    /**
     * @see ContentEngine
     */
    private ContentEngine mContentEngine;

    /**
     * @see PhotoUploadEngine.
     */
    private PhotoUploadEngine mPhotoUploadEngine;
    
    /**
     * Maximum time the run function for an engine is allowed to run before a
     * warning message will be displayed (debug only)
     */
    private static final long ENGINE_RUN_TIME_THRESHOLD = 3000;

    /**
     * Engine Manager Constructor
     * 
     * @param service {@link RemoteService} reference
     * @param uiCallback Provides useful engine callback functionality.
     */
    private EngineManager(RemoteService service, IEngineEventCallback uiCallback) {
        mService = service;
        mUiEventCallback = uiCallback;
    }

    /**
     * Create instance of EngineManager.
     * 
     * @param service {@link RemoteService} reference
     * @param uiCallback Provides useful engine callback functionality.
     */
    public static void createEngineManager(RemoteService service, IEngineEventCallback uiCallback) {
        sEngineManager = new EngineManager(service, uiCallback);
        sEngineManager.onCreate();
    }

    /**
     * Destroy EngineManager.
     */
    public static void destroyEngineManager() {
        if (sEngineManager != null) {
            sEngineManager.onDestroy();
            sEngineManager = null;
        }
    }

    /**
     * Get single instance of {@link EngineManager}.
     * 
     * @return {@link EngineManager} singleton instance.
     */
    public static EngineManager getInstance() {
        if (sEngineManager == null) {
            throw new InvalidParameterException("Please call EngineManager.createEngineManager() "
                    + "before EngineManager.getInstance()");
        }
        return sEngineManager;
    }

    /**
     * Add a new engine to the EngineManager.
     * 
     * @param newEngine Engine to be added.
     */
    private synchronized void addEngine(BaseEngine newEngine) {
        final String newName = newEngine.getClass().getSimpleName();
        String[] deactivatedEngines = SettingsManager
                .getStringArrayProperty(Settings.DEACTIVATE_ENGINE_LIST_KEY);
        for (String engineName : deactivatedEngines) {
            if (engineName.equals(newName)) {
                LogUtils.logW("DEACTIVATE ENGINE:  " + engineName);
                newEngine.deactivateEngine();
            }
        }
        if (!newEngine.isDeactivated()) {
            newEngine.onCreate();
            mEngineList.put(newEngine.mEngineId.ordinal(), newEngine);
        }
        mService.kickWorkerThread();
    }

    /**
     * Closes an engine and removes it from the list.
     * 
     * @param engine Reference of engine by base class {@link BaseEngine} to
     *            close
     */
    private synchronized void closeEngine(BaseEngine engine) {
        mEngineList.remove(engine.engineId().ordinal());
        if (!engine.isDeactivated()) {
            engine.onDestroy();
        }
    }

    /**
     * Called immediately after manager has been created. Starts the necessary
     * engines
     */
    private synchronized void onCreate() {
        // LogUtils.logV("EngineManager.onCreate()");
        createLoginEngine();
        createIdentityEngine();
        createSyncMeEngine();
        createContactSyncEngine();
        createGroupsEngine();
        if (SettingsManager.getProperty(Settings.UPGRADE_CHECK_URL_KEY) != null) {
            createUpgradeEngine();
        }
        createActivitiesEngine();
        createPresenceEngine();
        createContentEngine();
        createPhotoUploadEngine();
    }

    /**
     * Called just before the service is stopped. Shuts down all the engines
     */
    private synchronized void onDestroy() {
        final int engineCount = mEngineList.values().size();
        BaseEngine[] engineList = new BaseEngine[engineCount];
        mEngineList.values().toArray(engineList);
        for (int i = 0; i < engineCount; i++) {
            closeEngine(engineList[i]);
        }
        mLoginEngine = null;
        mUpgradeEngine = null;
        mActivitiesEngine = null;
        mPresenceEngine = null;
        mIdentityEngine = null;
        mContactSyncEngine = null;
        mGroupsEngine = null;
        mContentEngine = null;
        mPhotoUploadEngine = null;
    }

    /**
     * Obtains a reference to the login engine
     * 
     * @return a reference to the LoginEngine
     */
    public LoginEngine getLoginEngine() {
        assert mLoginEngine != null;
        return mLoginEngine;
    }

    /**
     * Create instance of LoginEngine.
     */
    private synchronized void createLoginEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mLoginEngine = new LoginEngine(mService, mUiEventCallback, app.getDatabase());
        addEngine(mLoginEngine);
    }

    /**
     * Fetch upgrade engine
     * 
     * @return UpgradeEngine object
     */
    public UpgradeEngine getUpgradeEngine() {
        assert mUpgradeEngine != null;
        return mUpgradeEngine;
    }

    /**
     * Create instance of UpgradeEngine.
     */
    private synchronized void createUpgradeEngine() {
        mUpgradeEngine = new UpgradeEngine(mService, mUiEventCallback);
        addEngine(mUpgradeEngine);
    }

    /**
     * Fetch activities engine
     * 
     * @return a ActivitiesEngine object
     */
    public ActivitiesEngine getActivitiesEngine() {
        assert mActivitiesEngine != null;
        return mActivitiesEngine;
    }

    /**
     * Create instance of ActivitiesEngine.
     */
    private synchronized void createActivitiesEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mActivitiesEngine = new ActivitiesEngine(mService, mUiEventCallback, app.getDatabase());
        getLoginEngine().addListener(mActivitiesEngine);
        addEngine(mActivitiesEngine);
    }

    /**
     * Fetch sync me engine, starting it if necessary.
     * 
     * @return The SyncMeEngine
     */
    public SyncMeEngine getSyncMeEngine() {
        assert mSyncMeEngine != null;
        return mSyncMeEngine;
    }

    /**
     * Create instance of SyncMeEngine.
     */
    private synchronized void createSyncMeEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mSyncMeEngine = new SyncMeEngine(mService, mUiEventCallback, app.getDatabase());
        addEngine(mSyncMeEngine);
    }

    /**
     * Fetch presence engine
     * 
     * @return Presence Engine object
     */
    public PresenceEngine getPresenceEngine() {
        assert mPresenceEngine != null;
        return mPresenceEngine;
    }

    /**
     * Fetch identity engine
     * 
     * @return IdentityEngine object
     */
    public IdentityEngine getIdentityEngine() {
        assert mIdentityEngine != null;
        return mIdentityEngine;
    }

    /**
     * Fetch content engine
     * 
     * @return ContentEngine object
     */
    public ContentEngine getContentEngine() {
        assert mContentEngine != null;
        return mContentEngine;
    }

    /**
     * Fetch contact sync engine
     * 
     * @return ContactSyncEngine object
     */
    public ContactSyncEngine getContactSyncEngine() {
        assert mContactSyncEngine != null;
        return mContactSyncEngine;
    }

    private synchronized void createContactSyncEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mContactSyncEngine = new ContactSyncEngine(mUiEventCallback, mService, app.getDatabase(),
                null);
        addEngine(mContactSyncEngine);
    }
    
    /**
     * Fetch groups engine
     * 
     * @return GroupEngine object
     */
    public GroupsEngine getGroupsEngine() {
        assert mGroupsEngine != null;
        return mGroupsEngine;
    }

    private synchronized void createGroupsEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mGroupsEngine = new GroupsEngine(mService, mUiEventCallback, app.getDatabase());
        addEngine(mGroupsEngine);
    }

    /**
     * Create instance of IdentityEngine.
     */
    private synchronized void createIdentityEngine() {
        mIdentityEngine = new IdentityEngine(mUiEventCallback);
        addEngine(mIdentityEngine);
    }

    /**
     * Create instance of ContentEngine.
     */
    private synchronized void createContentEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mContentEngine = new ContentEngine(mUiEventCallback, app.getDatabase());
        addEngine(mContentEngine);
    }

    /**
     * Create instance of PresenceEngine.
     */
    private synchronized void createPresenceEngine() {
        final MainApplication app = (MainApplication)mService.getApplication();
        mPresenceEngine = new PresenceEngine(mUiEventCallback, app.getDatabase());
        ConnectionManager.getInstance().addConnectionListener(mPresenceEngine);
        getLoginEngine().addListener(mPresenceEngine);
        addEngine(mPresenceEngine);
    }

    /**
     * Creates photoupload engine.
     */
    private void createPhotoUploadEngine() {
            mPhotoUploadEngine = new PhotoUploadEngine(mUiEventCallback);
            addEngine(mPhotoUploadEngine);

    }

    /**
     * gets the photo uplaod engine.
     * @return mPhotoUploadEngine.Instance of the engine.
     */
    public PhotoUploadEngine getPhotoUploadEngine() {
          return mPhotoUploadEngine;
    }

    /**
     * Respond to incoming message received from Comms layer. If this message
     * has a valid engine id it is routed to that engine, otherwise The
     * {@link EngineManager} will try to get the next response.
     * 
     * @param source EngineId associated with incoming message.
     */
    public void onCommsInMessage(EngineId source) {
        BaseEngine engine = null;
        if (source != null) {
            engine = mEngineList.get(source.ordinal());
        }
        if (engine != null) {
            engine.onCommsInMessage();
        } else {
            LogUtils.logE("EngineManager.onCommsInMessage - "
                    + "Cannot dispatch message, unknown source " + source);
            final ResponseQueue queue = ResponseQueue.getInstance();
            queue.getNextResponse(source);
        }
    }

    /**
     * Run any waiting engines and return the time in milliseconds from now when
     * this method needs to be called again.
     * 
     * @return -1 never needs to run, 0 needs to run as soon as possible,
     *         CurrentTime + 60000 in 1 minute, etc.
     */
    public synchronized long runEngines() { 
        long nextRuntime = -1;
        Set<Integer> e = mEngineList.keySet();
        Iterator<Integer> i = e.iterator();
        while (i.hasNext()) {
            int engineId = i.next();
            BaseEngine engine = mEngineList.get(engineId);
            long currentTime = System.currentTimeMillis();
            long tempRuntime = engine.getNextRunTime(); // TODO: Pass
            // mCurrentTime to
            // getNextRunTime() to
            // help with Unit
            // tests
            if (Settings.ENABLED_ENGINE_TRACE) {
                LogUtils.logV("EngineManager.runEngines() " + "engine["
                        + engine.getClass().getSimpleName() + "] " + "nextRunTime["
                        + getHumanReadableTime(tempRuntime, currentTime) + "] " + "current["
                        + getHumanReadableTime(nextRuntime, currentTime) + "]");
            } else {
                if (tempRuntime > 0 && tempRuntime < currentTime) {
                    LogUtils.logD("Engine[" + engine.getClass().getSimpleName() + "] run pending");
                }
            }
            if (tempRuntime < 0) {
                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("EngineManager.runEngines() Engine is off, so ignore");
                }

            } else if (tempRuntime <= currentTime) {
                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("EngineManager.runEngines() Run Engine ["
                            + engine.getClass().getSimpleName()
                            + "] and make sure we check it once more before sleeping");
                }

                /** TODO: Consider passing mCurrentTime to mEngine.run(). **/ 
                engine.run();
                nextRuntime = 0;
                final long timeForRun = System.currentTimeMillis() - currentTime;
                if (timeForRun > ENGINE_RUN_TIME_THRESHOLD) {
                    LogUtils.logE("EngineManager.runEngines() Engine ["
                            + engine.getClass().getSimpleName() + "] took " + timeForRun
                            + "ms to run");
                }
                if (Settings.ENABLED_PROFILE_ENGINES) {
                    StringBuilder string = new StringBuilder();
                    string.append(System.currentTimeMillis());
                    string.append("|");
                    string.append(engine.getClass().getSimpleName());
                    string.append("|");
                    string.append(timeForRun);
                    LogUtils.profileToFile(string.toString());
                }
            } else {
                if (nextRuntime != -1) {
                    nextRuntime = Math.min(nextRuntime, tempRuntime);
                } else {
                    nextRuntime = tempRuntime;
                }

                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("EngineManager.runEngines() Set mNextRuntime to ["
                            + getHumanReadableTime(nextRuntime, currentTime) + "]");
                }
            }
        }
        if (Settings.ENABLED_ENGINE_TRACE) {
            LogUtils.logI("EngineManager.getNextRunTime() Return ["
                    + getHumanReadableTime(nextRuntime, System.currentTimeMillis()) + "]");
        }
        return nextRuntime;
    }

    /***
     * Display the Absolute Time in a human readable format (for testing only).
     * 
     * @param absoluteTime Time to convert
     * @param currentTime Current time, for creating all relative times
     * @return Absolute time in human readable form
     */
    private static String getHumanReadableTime(long absoluteTime, long currentTime) {
        if (absoluteTime == -1) {
            return "OFF";
        } else if ((absoluteTime == 0)) {
            return "NOW";
        } else if (absoluteTime >= currentTime) {
            return (absoluteTime - currentTime) + "ms";
        } else {
            return (currentTime - absoluteTime) + "ms LATE";
        }
    }

    /**
     * Resets all the engines. Note: the method will block until all the engines
     * have performed the reset.
     */
    public void resetAllEngines() {
        LogUtils.logV("EngineManager.resetAllEngines() - begin");
        synchronized (mEngineList) {
            // Propagate the reset event to all engines
            for (BaseEngine engine : mEngineList.values()) {
                engine.onReset();
            }
        }

        // block the thread until all engines have been reset
        boolean allEngineAreReset = false;
        while (!allEngineAreReset) {
            synchronized (mEngineList) {
                boolean engineNotReset = false;
                for (BaseEngine engine : mEngineList.values()) {
                    if (!engine.getReset()) {
                        engineNotReset = true;
                    }
                }
                if (!engineNotReset) {
                    allEngineAreReset = true;
                    for (BaseEngine engine : mEngineList.values()) {
                        engine.clearReset();
                    }
                }
            }
            Thread.yield();
        }
        LogUtils.logV("EngineManager.resetAllEngines() - end");
    }
}
