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

import android.os.Handler;

import com.vodafone360.people.Settings;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.utils.LogUtils;

/**
 * The worker thread is the main thread of execution within the People Client
 * service. It is responsible for running engines via the EngineManager The
 * WorkerThread remains active while there are engines with pending run times,
 * becoming dormant otherwise The WorkerThread can be woken up when a request is
 * issued or via an Alarm set to trigger an engine runtime update check It is
 * extremely important that the WorkerThread does not become blocked.
 */
public class WorkerThread extends Thread {
    
    /**
     * Name of alarm for waking up worker thread
     */
    public final static String ALARM_WORKER_THREAD = "worker_thread";

    /**
     * Worker thread is renamed to this string
     */
    private final static String WORKER_THREAD_NAME = "WorkerThread";

    /**
     * Small wait time before looping around the engines again
     */
    private final static long LOOP_SLEEP_TIME_MS = 300;

    /**
     * Maximum loop iterations before allowing the worker thread to sleep for a
     * while to preserve battery.
     * 
     * @note Maybe this should be >1000 to avoid breaking startup sync TODO:
     *       Make this stricter as the code base improves.
     */
    private final static int MAX_RUNS_WITHOUT_PAUSE = 1000;

    /**
     * Period of time to put worker thread to sleep after it has run
     * {@link #MAX_RUNS_WITHOUT_PAUSE} times without any pause. TODO: Make this
     * stricter as the code base improves.
     */
    private final static int FORCE_PAUSE_MS = 10 * 1000; // Ten seconds

    /**
     * Object is used to control the worker thread. When no work is needed the
     * thread will wait on this object.
     */
    private final Object mWakeLock = new Object();

    /**
     * Reference to the {@link EngineManager}.
     */
    private EngineManager mEngineManager;

    /**
     * {@link Handler} used so that threads can wake up the worker thread by
     * sending a message.
     */
    private Handler mHandler;

    /**
     * Set to true when any of the engines run at least once.
     */
    private Boolean mLoopOneMoreTime = false;

    /**
     * Set to true when the worker thread is about to stop running.
     */
    private boolean mShutdownThread = false;

    /**
     * @param context Context Android Application Context.
     */
    protected WorkerThread(Handler handler) {
        setName(WORKER_THREAD_NAME);
        mHandler = handler;
        mEngineManager = EngineManager.getInstance();
        if (mEngineManager == null) {
            throw new RuntimeException("WorkerThread - The EngineManager cannot be null");
        }
    }

    /**
     * Wake the worker thread.
     * 
     * @return TRUE if thread is awakened successfully.
     */
    protected boolean wakeUp() {
        LogUtils.logD("WorkerThread.wakeUp()");
        synchronized (mWakeLock) {
            if (mShutdownThread) {
                return false;
            }
            mLoopOneMoreTime = true;
            mWakeLock.notifyAll();
            return true;
        }
    }

    /***
     * Loop through all the engines until no more work can be done right now,
     * then set an Alarm if more work is required.
     */
    @Override
    public void run() {
        LogUtils.logD("WorkerThread.run() [Start Thread]");
        int numberOfRunsWithoutPause = 0;
        synchronized (mWakeLock) {
            mShutdownThread = false;
        }

        do {
            numberOfRunsWithoutPause++;
            boolean prepareForShutdown = false;
            long nextRunTime = mEngineManager.runEngines();
            long currentTime = System.currentTimeMillis();
            if (Settings.ENABLED_ENGINE_TRACE) {
                LogUtils.logV("WorkerThread.run() mNextRunTime is [" + nextRunTime + "] or in "
                        + "[" + (nextRunTime - currentTime) + "ms] - mNumberOfRunsWithoutPause "
                        + "[" + numberOfRunsWithoutPause + "]");
            }

            if (nextRunTime == -1) {
                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("WorkerThread.run() No Engines waiting, so stop thread without "
                            + "creating a new Alarm");
                }
                prepareForShutdown = true;

            } else if (nextRunTime == 0) {
                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("WorkerThread.run() Engine waiting to run, so loop thread again");
                }

            } else if (nextRunTime < currentTime) {
                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("WorkerThread.run() Next process is already ["
                            + (currentTime - nextRunTime) + "ms] late, so loop again");
                }

            } else {
                if (Settings.ENABLED_ENGINE_TRACE) {
                    LogUtils.logV("WorkerThread.run() No Engines ready yet, so set Alarm and then"
                            + " end thread");
                }
                prepareForShutdown = true;
            }

            if (numberOfRunsWithoutPause > MAX_RUNS_WITHOUT_PAUSE) {
                LogUtils.logE("WorkerThread.run() WorkerThread is looping like crazy, pausing for"
                        + " [" + FORCE_PAUSE_MS + "ms] to save device resources.");
                prepareForShutdown = true;
                mLoopOneMoreTime = false;
                nextRunTime = System.currentTimeMillis() + FORCE_PAUSE_MS;
            }

            LogUtils.logV("WorkerThread.run() prepareForShutdown[" + prepareForShutdown + "]"
                    + " mLoopOneMoreTime[" + mLoopOneMoreTime + "]");
            if (prepareForShutdown && !mLoopOneMoreTime) {
                setWakeupTime(nextRunTime);
                mShutdownThread = true;
                mLoopOneMoreTime = false;
            } else {
                mLoopOneMoreTime = false;
                threadLoopSleep();
            }

        } while (!mShutdownThread);
        LogUtils.logD("WorkerThread.run() [End Thread]");
    }

    /***
     * Pause the thread between cycles to stop it overloading the device and
     * possibly blocking the UI.
     */
    private void threadLoopSleep() {
        synchronized (mWakeLock) {
            try {
                mWakeLock.wait(LOOP_SLEEP_TIME_MS);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    /***
     * Wake up the WorkerThread at the given time.
     * 
     * @param set TRUE sets the restart time, FALSE cancel any pending restarts.
     */
    private void setWakeupTime(long nextRunTime) {

        if (nextRunTime > System.currentTimeMillis()) {
            LogUtils.logV("WorkerThread.setWakeupTime() Run again in ["
                    + (nextRunTime - System.currentTimeMillis()) + "ms]");
            mHandler.sendEmptyMessageDelayed(1, nextRunTime - System.currentTimeMillis());
        } else if (nextRunTime != -1) {
            LogUtils.logV("WorkerThread.setWakeupTime() Run again now, nextRunTime[" + nextRunTime
                    + "]");
            mHandler.sendEmptyMessage(1);
        }
    }

    /**
     * Shut down and join the running thread.
     */
    protected void close() {
        LogUtils.logV("WorkerThread.close()");
        mShutdownThread = true;
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
