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

package com.vodafone360.people.service;

import java.security.InvalidParameterException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.interfaces.IConnectionManagerInterface;
import com.vodafone360.people.service.interfaces.IPeopleServiceImpl;
import com.vodafone360.people.service.interfaces.IWorkerThreadControl;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.IWakeupListener;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * Implementation of People client's Service class. Loads properties from
 * SettingsManager. Creates NetworkAgent. Connects to ConnectionManager enabling
 * transport layer. Activates service's worker thread when required.
 */
public class RemoteService extends Service implements IWorkerThreadControl,
        IConnectionManagerInterface {
    /**
     * Intent received when service is started is tested against the value
     * stored in this key to determine if the service has been started because
     * of an alarm.
     */
    public static final String ALARM_KEY = "alarm_key";
    
    /**
     * Action for an Authenticator.
     * Straight copy from AccountManager.ACTION_AUTHENTICATOR_INTENT in 2.X platform
     */
    public static final String ACTION_AUTHENTICATOR_INTENT = "android.accounts.AccountAuthenticator";
    /**
     * Sync Adapter System intent action received on Bind.
     */
    public static final String ACTION_SYNC_ADAPTER_INTENT = "android.content.SyncAdapter";

    /**
     * Main reference to network agent
     * 
     * @see NetworkAgent
     */
    private NetworkAgent mNetworkAgent;

    /**
     * Worker thread reference
     * 
     * @see WorkerThread
     */
    private WorkerThread mWorkerThread;

    /**
     * The following object contains the implementation of the
     * {@link com.vodafone360.people.service.interfaces.IPeopleService}
     * interface.
     */
    private IPeopleServiceImpl mIPeopleServiceImpl;

    /**
     * Used by comms when waking up the CPI at regular intervals and sending a
     * heartbeat is necessary
     */
    private IWakeupListener mWakeListener;

    /**
     * true when the service has been fully initialised
     */
    private boolean mIsStarted = false;

    /**
     * Stores the previous network connection state (true = connected)
     */
    private boolean mIsConnected = true;

    private NativeAccountObjectsHolder mAccountsObjectsHolder = null;
    
    /**
     * Creation of RemoteService. Loads properties (i.e. supported features,
     * server URLs etc) from SettingsManager. Creates IPeopleServiceImpl,
     * NetworkAgent. Connects ConnectionManager creating Connection thread(s)
     * and DecoderThread 'Kicks' worker thread.
     */
    @Override
    public void onCreate() {
        LogUtils.logV("RemoteService.onCreate()");
        SettingsManager.loadProperties(this);
        mIPeopleServiceImpl = new IPeopleServiceImpl(this, this);
        mNetworkAgent = new NetworkAgent(this, this, this);
        // Create NativeContactsApi here to access Application Context
        NativeContactsApi.createInstance(getApplicationContext());
        EngineManager.createEngineManager(this, mIPeopleServiceImpl);
        mNetworkAgent.onCreate();
        mIPeopleServiceImpl.setNetworkAgent(mNetworkAgent);
        ConnectionManager.getInstance().connect(this);

        /** The service has now been fully initialised. **/
        mIsStarted = true;
        kickWorkerThread();

        ((MainApplication)getApplication()).setServiceInterface(mIPeopleServiceImpl);
        if(VersionUtils.is2XPlatform()) {
            mAccountsObjectsHolder = new 
            NativeAccountObjectsHolder(((MainApplication)getApplication()));
        }
    }

    /**
     * Called on start of RemoteService. Check if we need to kick the worker
     * thread or 'wake' the TCP connection thread.
     */
    @Override
    public void onStart(Intent intent, int startId) {
        Bundle mBundle = intent.getExtras();
        LogUtils.logI("RemoteService.onStart() Intent action["
                + intent.getAction() + "] data[" + mBundle + "]");

        if ((null == mBundle) || (null == mBundle.getString(ALARM_KEY))) {
            LogUtils.logV("RemoteService.onStart() mBundle is null. Returning.");
            return;
        }

        if (mBundle.getString(ALARM_KEY).equals(WorkerThread.ALARM_WORKER_THREAD)) {
            LogUtils.logV("RemoteService.onStart() ALARM_WORKER_THREAD Alarm thrown");
            kickWorkerThread();

        } else if (mBundle.getString(ALARM_KEY).equals(IWakeupListener.ALARM_HB_THREAD)) {
            LogUtils.logV("RemoteService.onStart() ALARM_HB_THREAD Alarm thrown");
            if (null != mWakeListener) {
                mWakeListener.notifyOfWakeupAlarm();
            }
        }
    }

    /**
     * Destroy RemoteService Close WorkerThread, destroy EngineManger and
     * NetworkAgent.
     */
    @Override
    public void onDestroy() {
        LogUtils.logV("RemoteService.onDestroy()");
        ((MainApplication)getApplication()).setServiceInterface(null);
        mIsStarted = false;
        synchronized (this) {
            if (mWorkerThread != null) {
                mWorkerThread.close();
            }
        }
        EngineManager.destroyEngineManager();
        // No longer need NativeContactsApi
        NativeContactsApi.destroyInstance();
        mNetworkAgent.onDestroy();
    }

    /**
     * Service binding is not used internally by this Application, but called
     * externally by the system when it needs an Authenticator or Sync
     * Adapter.  This method will throw an InvalidParameterException if it is
     * not called with the expected intent (or called on a 1.x platform).
     */
    @Override
    public IBinder onBind(Intent intent) {
        final String action = intent.getAction();
        if (VersionUtils.is2XPlatform() && action != null) {
            if (action.equals(ACTION_AUTHENTICATOR_INTENT)) {
                return mAccountsObjectsHolder.getAuthenticatorBinder();
                
            } else if (action.equals(ACTION_SYNC_ADAPTER_INTENT)) {
                return mAccountsObjectsHolder.getSyncAdapterBinder();
            }
        }

        throw new InvalidParameterException("RemoteService.action() "
                + "There are no Binders for the given Intent");
    }

    /***
     * Ensures that the WorkerThread runs at least once.
     */
    @Override
    public void kickWorkerThread() {
        synchronized (this) {
            if (!mIsStarted) {
                // Thread will be kicked anyway once we have finished
                // initialisation.
                return;
            }
            if (mWorkerThread == null || !mWorkerThread.wakeUp()) {
                LogUtils.logV("RemoteService.kickWorkerThread() Start thread");
                mWorkerThread = new WorkerThread(mHandler);
                mWorkerThread.start();
            }
        }
    }

    /***
     * Handler for remotely calling the kickWorkerThread() method.
     */
    private final Handler mHandler = new Handler() {
        /**
         * Process kick worker thread message
         */
        @Override
        public void handleMessage(Message msg) {
            kickWorkerThread();
        }
    };

    /**
     * Called by NetworkAgent to notify whether device has become connected or
     * disconnected. The ConnectionManager connects or disconnects
     * appropriately. We kick the worker thread if our internal connection state
     * is changed.
     * 
     * @param connected true if device has become connected, false if device is
     *            disconnected.
     */
    @Override
    public void signalConnectionManager(boolean connected) {
        // if service agent becomes connected start conn mgr thread

        LogUtils.logI("RemoteService.signalConnectionManager()"
                + "Signalling Connection Manager to " + (connected ? "connect." : "disconnect."));

        if (connected) {
            ConnectionManager.getInstance().connect(this);
        } else {// SA is disconnected stop conn thread (and close connections?)
            ConnectionManager.getInstance().disconnect();
        }
        // kick EngineManager to run() and apply CONNECTED/DISCONNECTED changes
        if (connected != mIsConnected) {
            if (mIPeopleServiceImpl != null) {
                mIPeopleServiceImpl.kickWorkerThread();
            }
        }
        mIsConnected = connected;
    }

    /**
     * <p>
     * Registers a listener (e.g. the HeartbeatSender for TCP) that will be
     * notified whenever an intent for a new alarm is received.
     * </p>
     * <p>
     * This is desperately needed as the CPU of Android devices will halt when
     * the user turns off the screen and all CPU related activity is suspended
     * for that time. The wake up alarm is one simple way of achieving the CPU
     * to wake up and send out data (e.g. the heartbeat sender).
     * </p>
     */
    public void registerCpuWakeupListener(IWakeupListener wakeListener) {
        mWakeListener = wakeListener;
    }

    /**
     * Return handle to {@link NetworkAgent}
     * 
     * @return handle to {@link NetworkAgent}
     */
    public NetworkAgent getNetworkAgent() {
        return mNetworkAgent;
    }

    /**
     * Set an Alarm with the AlarmManager to trigger the next update check.
     * 
     * @param set Set or cancel the Alarm
     * @param realTime Time when the Alarm should be triggered
     */
    public void setAlarm(boolean set, long realTime) {
        Intent mRemoteServiceIntent = new Intent(this, this.getClass());
        mRemoteServiceIntent.putExtra(RemoteService.ALARM_KEY, IWakeupListener.ALARM_HB_THREAD);
        PendingIntent mAlarmSender = PendingIntent.getService(this, 0, mRemoteServiceIntent, 0);
        AlarmManager mAlarmManager = (AlarmManager)getSystemService(RemoteService.ALARM_SERVICE);

        if (set) {
            if (Settings.ENABLED_ENGINE_TRACE) {
                LogUtils.logV("WorkerThread.setAlarm() Check for the next update at [" + realTime
                        + "] or in [" + (realTime - System.currentTimeMillis()) + "ms]");
            }
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, realTime, mAlarmSender);

            /*
             * mAlarmManager.set(AlarmManager.RTC, realTime, mAlarmSender);
             * TODO: Optimisation suggestion - Consider only doing work when the
             * device is already awake
             */
        } else {
            mAlarmManager.cancel(mAlarmSender);
        }
    }
}
