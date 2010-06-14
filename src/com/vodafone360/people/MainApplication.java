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

package com.vodafone360.people;

import android.app.Application;
import android.os.Handler;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.service.interfaces.IPeopleService;
import com.vodafone360.people.utils.LoginPreferences;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.WidgetUtils;

/**
 * Application class used to create the connection to the service and cache
 * state between Activities.
 */
public class MainApplication extends Application {

    private IPeopleService mServiceInterface;

    private Handler mServiceLoadedHandler;

    private DatabaseHelper mDatabaseHelper;

    private final ApplicationCache mApplicationCache = new ApplicationCache();
    
    /**
     * Called when the Application is created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        SettingsManager.loadProperties(this);
        mDatabaseHelper = new DatabaseHelper(this);
        mDatabaseHelper.start();
        LoginPreferences.getCurrentLoginActivity(this);
    }

    /**
     * Called when the Application is exited.
     */
    @Override
    public void onTerminate() {
        // FlurryAgent.onEndSession(this);
        if (mDatabaseHelper != null) {
            mDatabaseHelper.close();
        }
        super.onTerminate();
    }

    /**
     * Return handle to DatabaseHelper, currently provides main point of access
     * to People client's database tables.
     * 
     * @return Handle to DatabaseHelper.
     */
    public DatabaseHelper getDatabase() {
        return mDatabaseHelper;
    }

    /**
     * Return handle to ApplicationCache.
     * 
     * @return handle to ApplicationCache.
     */
    public ApplicationCache getCache() {
        return mApplicationCache;
    }
    
    /**
     * Remove all user data from People client, this includes all account
     * information (downloaded contacts, login credentials etc.) and all cached
     * settings.
     */
    public synchronized void removeUserData() {
        EngineManager mEngineManager = EngineManager.getInstance();
        if (mEngineManager != null) {
            // Resets all the engines, the call will block until every engines
            // have been reset.
            mEngineManager.resetAllEngines();
        }
        mDatabaseHelper.removeUserData();
        // Before clearing the Application cache, kick the widget update. Pref's
        // file contain the widget ID.
        WidgetUtils.kickWidgetUpdateNow(this);
        mApplicationCache.clearCachedData(this);
    }

    /**
     * Register a Handler to receive notification when the People service has
     * loaded. If mServiceInterface == NULL then this means that the UI is
     * starting before the service has loaded - in this case the UI registers to
     * be notified when the service is loaded using the serviceLoadedHandler.
     * TODO: consider any pitfalls in this situation.
     * 
     * @param serviceLoadedHandler Handler that receives notification of service
     *            being loaded.
     */
    public synchronized void registerServiceLoadHandler(Handler serviceLoadedHandler) {
        if (mServiceInterface != null) {
            onServiceLoaded();
        } else {
            mServiceLoadedHandler = serviceLoadedHandler;
            LogUtils.logI("MainApplication.registerServiceLoadHandler() mServiceInterface is NULL "
                    + "- need to wait for service to be loaded");
        }
    }

    /**
     * Un-register People service loading handler.
     */
    public synchronized void unRegisterServiceLoadHandler() {
        mServiceLoadedHandler = null;
    }

    private void onServiceLoaded() {
        if (mServiceLoadedHandler != null) {
            mServiceLoadedHandler.sendEmptyMessage(0);
        }
    }

    /**
     * Set IPeopleService interface - this is the interface by which we
     * interface to People service.
     * 
     * @param serviceInterface IPeopleService handle.
     */
    public synchronized void setServiceInterface(IPeopleService serviceInterface) {
        if (serviceInterface == null) {
            LogUtils.logE("MainApplication.setServiceInterface() "
                    + "New serviceInterface should not be NULL");
        }
        mServiceInterface = serviceInterface;
        onServiceLoaded();
    }

    /**
     * Return current IPeopleService interface. TODO: The case where
     * mServiceInterface = NULL needs to be considered.
     * 
     * @return current IPeopleService interface (can be null).
     */
    public synchronized IPeopleService getServiceInterface() {
        if (mServiceInterface == null) {
            LogUtils.logE("MainApplication.getServiceInterface() "
                    + "mServiceInterface should not be NULL");
        }
        return mServiceInterface;
    }

    /**
     * Set Internet availability - always makes Internet available, only
     * available in home network, allow manual connection only. This setting is
     * stored in People database.
     * 
     * @param internetAvail Internet availability setting.
     * @return SerivceStatus indicating whether the Internet availability
     *         setting has been successfully updated in the database.
     */
    public ServiceStatus setInternetAvail(InternetAvail internetAvail) {
        PersistSettings mPersistSettings = new PersistSettings();
        mPersistSettings.putInternetAvail(internetAvail);
        ServiceStatus ss = mDatabaseHelper.setOption(mPersistSettings);
        synchronized (this) {
            if (mServiceInterface != null) {
                mServiceInterface.notifyDataSettingChanged(internetAvail);
            } else {
                LogUtils.logE("MainApplication.setInternetAvail() "
                       +  "mServiceInterface should not be NULL");
            }
        }
        return ss;
    }

    /**
     * Retrieve Internet availability setting from People database.
     * 
     * @return current Internet availability setting.
     */
    public InternetAvail getInternetAvail() {
        return mDatabaseHelper.fetchOption(PersistSettings.Option.INTERNETAVAIL).getInternetAvail();
    }
}
