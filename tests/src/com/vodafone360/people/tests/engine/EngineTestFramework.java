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

package com.vodafone360.people.tests.engine;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.tests.IPeopleTestFramework;
import com.vodafone360.people.tests.PeopleTestConnectionThread;

public class EngineTestFramework implements IEngineEventCallback, IPeopleTestFramework {

    private Thread mEngineWorkerThread = null;

    private BaseEngine mEngine = null;

    private boolean mActive = false;

    private IEngineTestFrameworkObserver mObserver;

    private PeopleTestConnectionThread mConnThread = null;

    private Object mEngReqLock = new Object();

    private Object mObjectLock = new Object();

    private AuthSessionHolder mSession = new AuthSessionHolder();

    private int mStatus;

    private Object mData = null;

    private boolean mRequestCompleted;

    private static int K_REQ_TIMEOUT_MSA = 60000;

    public EngineTestFramework(IEngineTestFrameworkObserver observer) {
        Log.d("TAG", "EngineTestFramework.EngineTestFramework");
        mObserver = observer;

        // setup dummy session
        mSession.userID = 0;
        mSession.sessionSecret = new String("sssh");
        mSession.userName = new String("bob");
        mSession.sessionID = new String("session");

        LoginEngine.setTestSession(mSession);

        // create a 'worker' thread for engines to kick
        mEngineWorkerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d("TAG", "run");
                while (mActive) {
                    // Log.d("TAG", "eng framework active");
                    long mCurrentTime = System.currentTimeMillis();
                    long nextRunTime = -1;
                    try {
                        nextRunTime = mEngine.getNextRunTime();
                    } catch (Exception e) {
                        onEngineException(e);
                    }
                    if (nextRunTime != -1 && nextRunTime <= mCurrentTime) {
                        Log.d("TAG", "run the engine");
                        try {
                            mEngine.run();
                        } catch (Exception e) {
                            onEngineException(e);
                        }
                    } else {
                        // Log.d("TAG", "eng framework inactive");
                        synchronized (mObjectLock) {
                            try {
                                // Log.d("TAG", "lock the engine");
                                mObjectLock.wait(500);
                            } catch (Exception e) {
                            }
                        }

                    }

                }
            }
        });

    }

    public void setEngine(BaseEngine eng) {
        Log.d("TAG", "enginetestframework.setEngine");
        if (eng == null) {
            throw (new RuntimeException("Engine is null"));
        }
        mEngine = eng;
        // start our 'worker' thread
        mActive = true;
        // start the connection thread
        mConnThread = new PeopleTestConnectionThread(this);
        mConnThread.startThread();
        // start the worker thread
        mEngineWorkerThread.start();

        QueueManager.getInstance().addQueueListener(mConnThread);
    }

    public ServiceStatus waitForEvent() {
        return waitForEvent(K_REQ_TIMEOUT_MSA);
    }

    public ServiceStatus waitForEvent(int ts) {
        Log.d("TAG", "EngineTestFramework waitForEvent");
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        kickWorkerThread();

        long endTime = System.nanoTime() + (((long)ts) * 1000000);
        ServiceStatus returnStatus = ServiceStatus.ERROR_UNEXPECTED_RESPONSE;
        mStatus = 5; // ERROR_COMMS_TIMEOUT
        synchronized (mEngReqLock) {
            while (!mRequestCompleted && System.nanoTime() < endTime) {
                try {
                    mEngReqLock.wait(ts);
                } catch (InterruptedException e) {
                }
            }
            returnStatus = ServiceStatus.fromInteger(mStatus);
        }
        mRequestCompleted = false;

        return returnStatus;
    }

    @Override
    public void kickWorkerThread() {
        Log.d("TAG", "kickWorkerThread");
        synchronized (mObjectLock) {
            mObjectLock.notify();
        }
    }

    @Override
    public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {
        mRequestCompleted = true;
        // mActive = false;
        mStatus = status;
        mData = data;

        synchronized (mEngReqLock) {
            mEngReqLock.notify();
        }
    }

    public Object data() {
        return mData;
    }

    @Override
    public void reportBackToFramework(int reqId, EngineId engine) {

        Log.d("TAG", "EngineTestFramework.reportBackToFramework");
        mObserver.reportBackToEngine(reqId, engine);
        final QueueManager reqQ = QueueManager.getInstance();
        final ResponseQueue respQ = ResponseQueue.getInstance();
        if (reqQ.getRequest(reqId) != null) {
            List<BaseDataType> dataTypeList = new ArrayList<BaseDataType>();
            ServerError err = new ServerError();
            err.errorType = ServerError.ErrorTypes.UNKNOWN.toString();
            dataTypeList.add(err);
            respQ.addToResponseQueue(reqId, dataTypeList, engine);
        }
    }

    @Override
    public IConnection testConnectionThread() {
        // TODO Auto-generated method stub
        return null;
    }

    public void callRun(int reqId, List<BaseDataType> data) {
        ResponseQueue.getInstance().addToResponseQueue(reqId, data, mEngine.engineId());
        try {
            mEngine.onCommsInMessage();
            mEngine.run();
        } catch (Exception e) {
            onEngineException(e);
        }
    }

    public void stopEventThread() {
        QueueManager.getInstance().removeQueueListener(mConnThread);
        synchronized (mObjectLock) {
            mActive = false;
            mObjectLock.notify();
        }
        mConnThread.stopThread();
    }

    private void onEngineException(Exception e) {
        String strExceptionInfo = e + "\n";
        for (int i = 0; i < e.getStackTrace().length; i++) {
            StackTraceElement v = e.getStackTrace()[i];
            strExceptionInfo += "\t" + v + "\n";
        }

        Log.e("TAG", "Engine exception occurred\n" + strExceptionInfo);
        mObserver.onEngineException(e);
        synchronized (mEngReqLock) {
            mActive = false;
            mStatus = ServiceStatus.ERROR_UNKNOWN.ordinal();
            mData = e;
            mEngReqLock.notify();
        }
    }

    @Override
    public UiAgent getUiAgent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApplicationCache getApplicationCache() {
        // TODO Auto-generated method stub
        return null;
    }
}