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

package com.vodafone360.people.service.agent;

import java.security.InvalidParameterException;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;

import com.vodafone360.people.Intents;
import com.vodafone360.people.MainApplication;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.service.interfaces.IConnectionManagerInterface;
import com.vodafone360.people.service.interfaces.IWorkerThreadControl;
import com.vodafone360.people.utils.LogUtils;

/**
 * The network Agent monitors the connectivity status of the device and makes
 * decisions about the communication strategy. The Agent has the following
 * states {connected | disconnected}, with changes reported to various listeners
 * in the service.
 */
public class NetworkAgent {

    /** Roaming notification is on */
    public static final int ROAMING_DIALOG_GLOBAL_ON = 0;

    /** Roaming notification is off */
    public static final int ROAMING_DIALOG_GLOBAL_OFF = 1;

    private static final int TYPE_WIFI = 1;

    private static AgentState mAgentState = AgentState.UNKNOWN;

    private ConnectivityManager mConnectivityManager;

    private ContentResolver mContentResolver;

    private AgentDisconnectReason mDisconnectReason = AgentDisconnectReason.UNKNOWN;

    private SettingsContentObserver mDataRoamingSettingObserver;

    private boolean mInternetConnected;

    private boolean mDataRoaming;

    private boolean mBackgroundData;

    private boolean mIsRoaming;

    private boolean mIsInBackground;

    private boolean mNetworkWorking = true;

    // dateTime value in milliseconds
    private Long mDisableRoamingNotificationUntil = null;

    private IWorkerThreadControl mWorkerThreadControl;

    private IConnectionManagerInterface mConnectionMgrIf;

    private Context mContext;

    public enum AgentState {
        CONNECTED,
        DISCONNECTED,
        UNKNOWN
    };
    
    private boolean mWifiNetworkAvailable;
    private boolean mMobileNetworkAvailable;

    /**
     * Reasons for Service Agent changing state to disconnected
     */
    public enum AgentDisconnectReason {
        AGENT_IS_CONNECTED, // Sanity check
        NO_INTERNET_CONNECTION,
        NO_WORKING_NETWORK,
        DATA_SETTING_SET_TO_MANUAL_CONNECTION,
        DATA_ROAMING_DISABLED,
        BACKGROUND_CONNECTION_DISABLED,
        // WIFI_INACTIVE,
        UNKNOWN
    }

    public enum StatesOfService {
        IS_CONNECTED_TO_INTERNET,
        IS_NETWORK_WORKING,
        IS_ROAMING,
        IS_ROAMING_ALLOWED,
        IS_INBACKGROUND,
        IS_BG_CONNECTION_ALLOWED,
        IS_WIFI_ACTIVE
    };

    /**
     * Listens for changes made to People client's status. The NetworkAgent is
     * specifically interested in changes to the data settings (e.g. data
     * disabled, only in home network or roaming).
     */
    private class SettingsContentObserver extends ContentObserver {
        private String mSettingName;

        private SettingsContentObserver(String settingName) {
            super(new Handler());
            mSettingName = settingName;
        }

        /**
         * Start content observer.
         */
        private void start() {
            if (mContentResolver != null) {
                mContentResolver.registerContentObserver(Settings.Secure.getUriFor(mSettingName),
                        true, this);
            }
        }

        /**
         * De-activate content observer.
         */
        private void close() {
            if (mContentResolver != null) {
                mContentResolver.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            onDataSettingChanged(mSettingName);
        }

        public boolean getBooleanValue() {
            if (mContentResolver != null) {
                try {
                    return (Settings.Secure.getInt(mContentResolver, mSettingName) != 0);
                } catch (SettingNotFoundException e) {
                    LogUtils.logE("NetworkAgent.SettingsContentObserver.getBooleanValue() "
                            + "SettingNotFoundException", e);
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * The constructor.
     * 
     * @param context Android context.
     * @param workerThreadControl Handle to kick the worker thread.
     * @param connMgrIf Handler to signal the connection manager.
     * @throws InvalidParameterException Context is NULL.
     * @throws InvalidParameterException IWorkerThreadControl is NULL.
     * @throws InvalidParameterException IConnectionManagerInterface is NULL.
     */
    public NetworkAgent(Context context, IWorkerThreadControl workerThreadControl,
            IConnectionManagerInterface connMgrIf) {
        if (context == null) {
            throw new InvalidParameterException("NetworkAgent() Context canot be NULL");
        }
        if (workerThreadControl == null) {
            throw new InvalidParameterException("NetworkAgent() IWorkerThreadControl canot be NULL");
        }
        if (connMgrIf == null) {
            throw new InvalidParameterException(
                    "NetworkAgent() IConnectionManagerInterface canot be NULL");
        }
        mContext = context;
        mWorkerThreadControl = workerThreadControl;
        mConnectionMgrIf = connMgrIf;
        mContentResolver = context.getContentResolver();
        mConnectivityManager = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        mDataRoamingSettingObserver = new SettingsContentObserver(Settings.Secure.DATA_ROAMING);
    }

    /**
     * Create NetworkAgent and start observers of device connectivity state.
     * 
     * @throws InvalidParameterException DataRoamingSettingObserver is NULL.
     * @throws InvalidParameterException Context is NULL.
     * @throws InvalidParameterException ConnectivityManager is NULL.
     */
    public void onCreate() {
        if (mDataRoamingSettingObserver == null) {
            throw new InvalidParameterException(
                    "NetworkAgent.onCreate() DataRoamingSettingObserver canot be NULL");
        }
        if (mContext == null) {
            throw new InvalidParameterException("NetworkAgent.onCreate() Context canot be NULL");
        }
        if (mConnectivityManager == null) {
            throw new InvalidParameterException(
                    "NetworkAgent.onCreate() ConnectivityManager canot be NULL");
        }

        mDataRoamingSettingObserver.start();
        mDataRoaming = mDataRoamingSettingObserver.getBooleanValue();
        mContext.registerReceiver(mBackgroundDataBroadcastReceiver, new IntentFilter(
                ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
        mContext.registerReceiver(mInternetConnectivityReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        mContext.registerReceiver(mServiceStateRoamingReceiver, new IntentFilter(
                "android.intent.action.SERVICE_STATE"));
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if (info != null) {
            mInternetConnected = (info.getState() == NetworkInfo.State.CONNECTED);
            mWifiNetworkAvailable = (info.getType() == TYPE_WIFI);
            mIsRoaming = info.isRoaming();
        }
        mBackgroundData = mConnectivityManager.getBackgroundDataSetting();
        onConnectionStateChanged();
    }

    /**
     * Destroy NetworkAgent and un-register observers.
     * 
     * @throws InvalidParameterException Context is NULL.
     * @throws InvalidParameterException DataRoamingSettingObserver is NULL.
     */
    public void onDestroy() {
        if (mContext == null) {
            throw new InvalidParameterException("NetworkAgent.onCreate() Context canot be NULL");
        }
        if (mDataRoamingSettingObserver == null) {
            throw new InvalidParameterException(
                    "NetworkAgent.onDestroy() DataRoamingSettingObserver canot be NULL");
        }

        mContext.unregisterReceiver(mInternetConnectivityReceiver);
        mContext.unregisterReceiver(mBackgroundDataBroadcastReceiver);
        mContext.unregisterReceiver(mServiceStateRoamingReceiver);
        mDataRoamingSettingObserver.close();
        mDataRoamingSettingObserver = null;
    }

    /**
     * Receive notification from
     * ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED
     */
    private final BroadcastReceiver mBackgroundDataBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtils
                    .logV("NetworkAgent.broadcastReceiver.onReceive() ACTION_BACKGROUND_DATA_SETTING_CHANGED");
            synchronized (NetworkAgent.this) {
                if (mConnectivityManager != null) {
                    mBackgroundData = mConnectivityManager.getBackgroundDataSetting();
                    onConnectionStateChanged();
                }
            }
        }
    };

    /**
     * Receive notification from ConnectivityManager.CONNECTIVITY_ACTION
     */
    private final BroadcastReceiver mInternetConnectivityReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() CONNECTIVITY_ACTION");
            synchronized (NetworkAgent.this) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                
                if (info == null) {
                	LogUtils.logW("NetworkAgent.broadcastReceiver.onReceive() EXTRA_NETWORK_INFO not found.");
                } else {
                	if (info.getType() == TYPE_WIFI) {
                		mWifiNetworkAvailable = (info.getState() == NetworkInfo.State.CONNECTED);
                	} else {
                		mMobileNetworkAvailable = (info.getState() == NetworkInfo.State.CONNECTED);
                	}
                }
                
                if (noConnectivity) {
                	LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() EXTRA_NO_CONNECTIVITY found!");
                	mInternetConnected = false;
                } else {
                	mInternetConnected = mWifiNetworkAvailable || mMobileNetworkAvailable;
                }
                
                LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() mInternetConnected = " + mInternetConnected +
                								", mWifiNetworkAvailable = " + mWifiNetworkAvailable +
                								", mMobileNetworkAvailable = " + mMobileNetworkAvailable);
                
                onConnectionStateChanged();
            }
        }
    };

    /**
     * Receive notification from android.intent.action.SERVICE_STATE
     */
    private final BroadcastReceiver mServiceStateRoamingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() SERVICE_STATE");
            synchronized (NetworkAgent.this) {
                // //ConnectivityManager provides wrong information about
                // roaming
                // NetworkInfo info =
                // mConnectivityManager.getActiveNetworkInfo();
                // if (info != null) {
                // mIsRoaming = info.isRoaming();
                // }

                LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() Extras are: "
                        + intent.getExtras());
                Bundle bu = intent.getExtras();
                // int state = bu.getInt("state");
                boolean roam = bu.getBoolean("roaming");
                mIsRoaming = roam;
                onConnectionStateChanged();

                LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() Network Roaming = "
                        + mIsRoaming);
                LogUtils.logV("NetworkAgent.broadcastReceiver.onReceive() WiFi active	   = "
                        + mWifiNetworkAvailable);
            }
            processRoaming(null);
        }
    };

    /**
     * Notify interested parties of changes in Internet setting.
     * 
     * @param val updated InternetAvail value.
     */
    public void notifyDataSettingChanged(InternetAvail val) {
        processRoaming(val);
        onConnectionStateChanged();
    }

    /**
     * Displaying notification to the user about roaming
     * 
     * @param InternetAvail value.
     */
    private void processRoaming(InternetAvail val) {
        InternetAvail internetAvail;
        if (val != null) {
            internetAvail = val;
        } else {
            internetAvail = getInternetAvailSetting();
        }

        Intent intent = new Intent();

        if (mContext != null
                && mIsRoaming
                && (internetAvail == InternetAvail.ALWAYS_CONNECT)
                && (mDisableRoamingNotificationUntil == null || mDisableRoamingNotificationUntil < System
                        .currentTimeMillis())) {
            LogUtils.logV("NetworkAgent.processRoaming() "
                    + "Displaying notification - DisplayRoaming[" + mIsRoaming + "]");
           
            intent.setAction(Intents.ROAMING_ON);
        } else {
            /*
             * We are not roaming then we should remove notification, if no
             * notification were before nothing happens
             */
            LogUtils.logV("NetworkAgent.processRoaming() Removing notification - "
                    + " DisplayRoaming[" + mIsRoaming + "]");

            intent.setAction(Intents.ROAMING_OFF);
        }
        
        mContext.sendBroadcast(intent);
    }

    private InternetAvail getInternetAvailSetting() {
        if (mContext != null) {
            PersistSettings setting = ((MainApplication)((RemoteService)mContext).getApplication())
                    .getDatabase().fetchOption(PersistSettings.Option.INTERNETAVAIL);
            if (setting != null) {
                return setting.getInternetAvail();
            }
        }
        return null;
    }

    public int getRoamingNotificationType() {
        int type;
        if (mDataRoaming) {
            type = ROAMING_DIALOG_GLOBAL_ON;
        } else {
            type = ROAMING_DIALOG_GLOBAL_OFF;
        }
        return type;
    }

    /**
     * Get current device roaming setting.
     * 
     * @return current device roaming setting.
     */
    public boolean getRoamingDeviceSetting() {
        return mDataRoaming;
    }

    public void setShowRoamingNotificationAgain(boolean showAgain) {
        LogUtils.logV("NetworkAgent.setShowRoamingNotificationAgain() " + "showAgain[" + showAgain
                + "]");
        if (showAgain) {
            mDisableRoamingNotificationUntil = null;
        } else {
            mDisableRoamingNotificationUntil = System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS;
            if (mContext != null) {
                LogUtils.logV("NetworkAgent.setShowRoamingNotificationAgain() "
                        + "Next notification on ["
                        + DateUtils.formatDateTime(mContext, mDisableRoamingNotificationUntil,
                                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                                        | DateUtils.FORMAT_SHOW_YEAR) + "]");
            }
        }
        processRoaming(null);
    }

    /**
     * Received when user modifies one of the system settings
     */
    private synchronized void onDataSettingChanged(String settingName) {
        LogUtils.logV("NetworkAgent.onDataSettingChanged() settingName[" + settingName + "]"
                + " has changed");
        if (settingName.equals(Settings.Secure.DATA_ROAMING)) {
            if (mDataRoamingSettingObserver != null) {
                mDataRoaming = mDataRoamingSettingObserver.getBooleanValue();
                onConnectionStateChanged();
            }
        }
    }

    /**
     * Contains the main logic that determines the agent state for network
     * access
     */
    private void onConnectionStateChanged() {
        if (!mInternetConnected) {
            LogUtils.logV("NetworkAgent.onConnectionStateChanged() No internet connection");
            mDisconnectReason = AgentDisconnectReason.NO_INTERNET_CONNECTION;
            setNewState(AgentState.DISCONNECTED);
            return;
        } else {
            if (mWifiNetworkAvailable) {
                LogUtils.logV("NetworkAgent.onConnectionStateChanged() WIFI connected");
            } else {
                LogUtils.logV("NetworkAgent.onConnectionStateChanged() Cellular connected");
            }
        }
        if (mContext != null) {
            MainApplication app = (MainApplication)((RemoteService)mContext).getApplication();
            if ((app.getInternetAvail() == InternetAvail.MANUAL_CONNECT)/*
                                                                         * AA: I
                                                                         * commented
                                                                         * it -
                                                                         * TBD
                                                                         * &&!
                                                                         * mWifiNetworkAvailable
                                                                         */) {
                LogUtils.logV("NetworkAgent.onConnectionStateChanged()"
                        + " Internet allowed only in manual mode");
                mDisconnectReason = AgentDisconnectReason.DATA_SETTING_SET_TO_MANUAL_CONNECTION;
                setNewState(AgentState.DISCONNECTED);
                return;
            }
        }
        if (!mNetworkWorking) {
            LogUtils.logV("NetworkAgent.onConnectionStateChanged() Network is not working");
            mDisconnectReason = AgentDisconnectReason.NO_WORKING_NETWORK;
            setNewState(AgentState.DISCONNECTED);
            return;
        }
        if (mIsRoaming && !mDataRoaming) {
            LogUtils.logV("NetworkAgent.onConnectionStateChanged() "
                    + "Connect while roaming not allowed");
            mDisconnectReason = AgentDisconnectReason.DATA_ROAMING_DISABLED;
            setNewState(AgentState.DISCONNECTED);
            return;
        }
        if (mIsInBackground && !mBackgroundData) {
            LogUtils
                    .logV("NetworkAgent.onConnectionStateChanged() Background connection not allowed");
            mDisconnectReason = AgentDisconnectReason.BACKGROUND_CONNECTION_DISABLED;
            setNewState(AgentState.DISCONNECTED);
            return;
        }
        LogUtils.logV("NetworkAgent.onConnectionStateChanged() Connection available");
        setNewState(AgentState.CONNECTED);
    }

    public static AgentState getAgentState() {
        LogUtils.logV("NetworkAgent.getAgentState() mAgentState[" + mAgentState.name() + "]");
        return mAgentState;
    }

    private void setNewState(AgentState newState) {
        if (newState == mAgentState) {
            return;
        }
        LogUtils.logI("NetworkAgent.setNewState(): " + mAgentState + " -> " + newState);
        mAgentState = newState;

        if (newState == AgentState.CONNECTED) {
            mDisconnectReason = AgentDisconnectReason.AGENT_IS_CONNECTED;
            onConnected();
        } else if (newState == AgentState.DISCONNECTED) {
            onDisconnected();
        }
    }

    private void onConnected() {
        checkActiveNetworkState();
        if (mWorkerThreadControl != null) {
            mWorkerThreadControl.kickWorkerThread();
        }
        if (mConnectionMgrIf != null) {
            mConnectionMgrIf.signalConnectionManager(true);
        }
    }

    private void onDisconnected() {
        // AA:need to kick it to make engines run and set the
        if (mWorkerThreadControl != null) {
            mWorkerThreadControl.kickWorkerThread();
        }
        if (mConnectionMgrIf != null) {
            mConnectionMgrIf.signalConnectionManager(false);
        }
    }

    private void checkActiveNetworkState() {
        if (mConnectivityManager != null) {
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();

            if (mNetworkInfo == null) {
                LogUtils.logW("NetworkAgent.checkActiveNetworkInfoy() "
                        + "mConnectivityManager.getActiveNetworkInfo() Returned null");
                return;
            } else {
                if (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    LogUtils.logV("NetworkAgent.checkActiveNetworkInfoy() WIFI network");
                    // TODO: Do something

                } else if (mNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {

                    LogUtils.logV("NetworkAgent.checkActiveNetworkInfoy() MOBILE and ROAMING");
                    // TODO: Do something
                    // Only works when you are registering with network

                    switch (mNetworkInfo.getSubtype()) {
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                            LogUtils
                                    .logV("NetworkAgent.checkActiveNetworkInfoy() MOBILE EDGE network");
                            // TODO: Do something
                            break;

                        case TelephonyManager.NETWORK_TYPE_GPRS:
                            LogUtils
                                    .logV("NetworkAgent.checkActiveNetworkInfoy() MOBILE GPRS network");
                            // TODO: Do something
                            break;

                        case TelephonyManager.NETWORK_TYPE_UMTS:
                            LogUtils
                                    .logV("NetworkAgent.checkActiveNetworkInfoy() MOBILE UMTS network");
                            // TODO: Do something
                            break;

                        case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                            LogUtils
                                    .logV("NetworkAgent.checkActiveNetworkInfoy() MOBILE UNKNOWN network");
                            // TODO: Do something
                            break;
                            
                        default:
                            // Do nothing.
                            break;
                    }
                    ;
                }
            }
        } else {
            LogUtils.logW("NetworkAgent.checkActiveNetworkInfoy() mConnectivityManager is null");
        }
    }

    public void setNetworkAgentState(NetworkAgentState state) {
        LogUtils.logD("NetworkAgent.setNetworkAgentState() state[" + state + "]");
        // TODO: make assignments if any changes
        boolean changes[] = state.getChanges();

        if (changes[StatesOfService.IS_CONNECTED_TO_INTERNET.ordinal()])
            mInternetConnected = state.isInternetConnected();
        if (changes[StatesOfService.IS_NETWORK_WORKING.ordinal()])
            mNetworkWorking = state.isNetworkWorking();
        if (changes[StatesOfService.IS_ROAMING_ALLOWED.ordinal()])
            mDataRoaming = state.isRoamingAllowed();
        if (changes[StatesOfService.IS_INBACKGROUND.ordinal()])
            mIsInBackground = state.isInBackGround();
        if (changes[StatesOfService.IS_BG_CONNECTION_ALLOWED.ordinal()])
            mBackgroundData = state.isBackDataAllowed();
        if (changes[StatesOfService.IS_WIFI_ACTIVE.ordinal()])
            mWifiNetworkAvailable = state.isWifiActive();
        if (changes[StatesOfService.IS_ROAMING.ordinal()]) {// special case for
                                                            // roaming
            mIsRoaming = state.isRoaming();
            // This method sets the mAgentState, and mDisconnectReason as well
            // by calling setNewState();
            onConnectionStateChanged();
            processRoaming(null);
        } else
            // This method sets the mAgentState, and mDisconnectReason as well
            // by calling setNewState();
            onConnectionStateChanged();
    }

    public NetworkAgentState getNetworkAgentState() {

        NetworkAgentState state = new NetworkAgentState();
        state.setRoaming(mIsRoaming);
        state.setRoamingAllowed(mDataRoaming);

        state.setBackgroundDataAllowed(mBackgroundData);
        state.setInBackGround(mIsInBackground);

        state.setInternetConnected(mInternetConnected);
        state.setNetworkWorking(mNetworkWorking);
        state.setWifiActive(mWifiNetworkAvailable);

        state.setDisconnectReason(mDisconnectReason);
        state.setAgentState(mAgentState);

        LogUtils.logD("NetworkAgent.getNetworkAgentState() state[" + state + "]");

        return state;
    }

    // /////////////////////////////
    // FOR TESTING PURPOSES ONLY //
    // /////////////////////////////

    /**
     * Forces the AgentState to a specific value.
     * 
     * @param newState the state to set Note: to be used only for test purposes
     */
    public static void setAgentState(AgentState newState) {
        mAgentState = newState;
    }

}
