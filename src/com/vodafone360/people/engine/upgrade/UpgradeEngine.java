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

package com.vodafone360.people.engine.upgrade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * Checks Settings.UPGRADE_CHECK_URL for information on a software upgrade .
 * Triggers a dialog in the UI when a newer application version is available
 * Throttles UI events
 */
public class UpgradeEngine extends BaseEngine {

    private Context mContext;

    private UpgradeStatus mUpgradeStatus;

    private long mCheckFrequencyMillis;

    private long mNextRunTime = 0; // Run once

    private boolean mForceOnce = false;

    private boolean mActivated = false;

    private UpgradeEngineNetworkThread mUpgradeEngineNetworkThread;

    /**
     * Constructor.
     * 
     * @param context - the context to use
     * @param eventCallback - the engine event client to use
     */
    public UpgradeEngine(Context context, IEngineEventCallback eventCallback) {
        super(eventCallback);
        mActivated = SettingsManager.getProperty(Settings.UPGRADE_CHECK_URL_KEY) != null;
        mEngineId = EngineId.UPGRADE_ENGINE;
        mContext = context;
        mCheckFrequencyMillis = UpgradeUtils.getCheckFrequency(context);
    }

    /**
     * Return the mNextRunTime value if the feature is enabled.
     * 
     * @return the mNextRunTime value if the feature is enabled.
     */
    @Override
    public long getNextRunTime() {
        if (!mActivated) {
            LogUtils.logV("UpgradeEngine.getNextRunTime() Not currently activated");
            return -1;
        }
        if (isUiRequestOutstanding()) {
            return 0;
        }
        if (NetworkAgent.getAgentState() != AgentState.CONNECTED) {
            LogUtils.logD("UpgradeEngine.getNextRunTime() Not currently connected");
            return -1;
        }

        if (mForceOnce) {
            LogUtils.logD("UpgradeEngine.getNextRunTime() Engine forced to run");
            return 0;

        } else if (mCheckFrequencyMillis == -1) {
            LogUtils.logD("UpgradeEngine.getNextRunTime() Upgrade feature disabled");
            return -1;

        } else {
            // LogUtils.logV("UpgradeEngine.getNextRunTime() Returning [" +
            // mNextRunTime + "] or in ["+(mNextRunTime -
            // System.currentTimeMillis())+"ms]");
            return mNextRunTime;
        }
    }

    /**
     * Trigger the UpgradeEngineNetworkThread if the feature is enabled.
     */
    @Override
    public void run() {
        if (!mActivated) {
            LogUtils.logE("UpgradeEngine.getNextRunTime() Not currently activated");
            return;
        }
        if (NetworkAgent.getAgentState() != AgentState.CONNECTED) {
            LogUtils.logE("UpgradeEngine.getNextRunTime() Not currently connected");
            return;
        }

        if (isUiRequestOutstanding() && processUiQueue()) {
            return;
        }
        if (!mForceOnce && mCheckFrequencyMillis == -1) {
            LogUtils.logD("UpgradeEngine.run() Upgrade feature disabled");
            return;
        } else {
            if (mUpgradeEngineNetworkThread == null || !mUpgradeEngineNetworkThread.isRunning()) {
                // LogUtils.logV("UpgradeEngine.run() Start the UpgradeEngineNetworkThread mForceOnce["
                // + mForceOnce + "]");
                mUpgradeEngineNetworkThread = new UpgradeEngineNetworkThread();
                mUpgradeEngineNetworkThread.start();
            } else {
                LogUtils.logD("UpgradeEngine.run() UpgradeEngineNetworkThread already running["
                        + mForceOnce + "]");
            }
        }

        setNextRuntime();
    }

    private void setNextRuntime() {
        mForceOnce = false;
        if (mCheckFrequencyMillis != -1) {
            mNextRunTime = System.currentTimeMillis() + mCheckFrequencyMillis;
            // LogUtils.logV("UpgradeEngine.setNextRuntime() Run again at [" +
            // mNextRunTime + "] or in ["+(mNextRunTime -
            // System.currentTimeMillis())+"ms], mCheckFrequencyMillis["+mCheckFrequencyMillis+"ms]");
        } else {
            LogUtils.logV("UpgradeEngine.setNextRuntime() mCheckFrequencyMillis["
                    + mCheckFrequencyMillis + "] so return -1");
            mNextRunTime = -1;
        }
    }

    /**
     * Thread for running an upgrade check without blocking the WorkerThread.
     */
    private class UpgradeEngineNetworkThread extends Thread {
        private static final String UPGRADE_THREAD_NAME = "UpgradeThread";

        private boolean mRunning = true;

        public UpgradeEngineNetworkThread() {
            setName(UPGRADE_THREAD_NAME);
        }

        /**
         * Check the Settings.UPGRADE_CHECK_URL file for information on a new
         * software upgrade.
         */
        @Override
        public void run() {
            LogUtils.logI("UpgradeEngine.UpgradeEngineNetworkThread.run() [Start Thread]");

            mUpgradeStatus = new UpgradeStatus();
            BufferedReader mBufferedReader = null;
            try {
                URL mUrl = new URL(SettingsManager.getProperty(Settings.UPGRADE_CHECK_URL_KEY)
                        + "?currentversion=" + VersionUtils.getPackageVersionCode(mContext));
                LogUtils.logV("UpgradeEngine.UpgradeEngineNetworkThread.run() Checking URL ["
                        + mUrl + "]");
                URLConnection mURLConnection = mUrl.openConnection();
                mBufferedReader = new BufferedReader(new InputStreamReader(mURLConnection
                        .getInputStream(), "UTF-8"));
                String mLine;
                while ((mLine = mBufferedReader.readLine()) != null) {
                    mUpgradeStatus.addSetting(mLine);
                }
                mBufferedReader.close();

                if (mUpgradeStatus.getLatestVersion() > VersionUtils.getPackageVersionCode(mContext)) {

                    // Show upgrade dialog
                    LogUtils.logD("UpgradeEngine.UpgradeEngineNetworkThread.run() New Version ["
                            + mUpgradeStatus.getLatestVersion() + "] better than current ["
                            + VersionUtils.getPackageVersionCode(mContext) + "]");

                } else {
                    // Do nothing
                    LogUtils.logV("UpgradeEngine.UpgradeEngineNetworkThread.run() New Version ["
                            + mUpgradeStatus.getLatestVersion() + "] is not better than current ["
                            + VersionUtils.getPackageVersionCode(mContext) + "]");
                    mUpgradeStatus = null;
                }
                UpgradeUtils.cacheUpdate(mContext, mUpgradeStatus);

                // Request to update the UI
                completeUiRequest(ServiceStatus.SUCCESS, null);

                if (mUpgradeStatus != null) {
                    mEventCallback.getUiAgent().sendUnsolicitedUiEvent(
                            ServiceUiRequest.UNSOLICITED_DIALOG_UPGRADE, null);
                }

            } catch (MalformedURLException e) {
                LogUtils.logE(
                        "UpgradeEngine.UpgradeEngineNetworkThread.run() MalformedURLException", e);

            } catch (IOException e) {
                LogUtils.logE("UpgradeEngine.UpgradeEngineNetworkThread.run() IOException", e);

            } finally {
                CloseUtils.close(mBufferedReader);
            }
            mRunning = false;
            LogUtils.logI("UpgradeEngine.UpgradeEngineNetworkThread.run() [Stop Thread]");
        }

        public boolean isRunning() {
            return mRunning;
        }
    }

    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        switch (requestId) {
            case UPGRADE_CHECK_NOW:
                // Ensure that the UpgradeEngine will run the next time the
                // WorkerThread is kicked
                LogUtils.logW("UpgradeEngine.processUiRequest() UPGRADE_CHECK_NOW");
                mForceOnce = true;
                break;
            case UPGRADE_CHANGE_FREQUENCY:
                mCheckFrequencyMillis = UpgradeUtils.getCheckFrequency(mContext);
                mNextRunTime = System.currentTimeMillis() + mCheckFrequencyMillis;
                LogUtils.logW("UpgradeEngine.processUiRequest() mCheckFrequencyMillis["
                        + mCheckFrequencyMillis + "] mNextRunTime[" + mNextRunTime + "]");
                break;
            default:
                // Do nothing.
                break;
        }
    }

    @Override
    public void onCreate() {
        // Do nothing
    }

    @Override
    public void onDestroy() {
        // Do nothing
    }

    @Override
    protected void onRequestComplete() {
        // Do nothing
    }

    @Override
    protected void onTimeoutEvent() {
        // Do nothing
    }

    @Override
    protected void processCommsResponse(DecodedResponse resp) {
        // Do nothing
    }

    /**
     * Sets a new value for the frequency with which to check for application
     * updates.
     */
    public void setNewUpdateFrequency() {
        LogUtils.logD("UpgradeEngine.setNewUpdateFrequency()");
        addUiRequestToQueue(ServiceUiRequest.UPGRADE_CHANGE_FREQUENCY, null);
    }

    /**
     * Add request to check for application update.
     */
    public void checkForUpdates() {
        LogUtils.logD("UpgradeEngine.checkForUpdates()");
        addUiRequestToQueue(ServiceUiRequest.UPGRADE_CHECK_NOW, null);
    }
}
