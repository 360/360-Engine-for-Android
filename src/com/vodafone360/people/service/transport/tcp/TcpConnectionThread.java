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

package com.vodafone360.people.service.transport.tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.TimeOutWatcher;
import com.vodafone360.people.service.utils.hessian.HessianUtils;
import com.vodafone360.people.utils.LogUtils;

public class TcpConnectionThread implements Runnable, IConnection {
    private static final String RPG_FALLBACK_TCP_URL = "rpg.vodafone360.com";

    private static final int RPG_DEFAULT_TCP_PORT = 9900;

    private static final int TCP_DEFAULT_TIMEOUT = 120000;

    private static final int ERROR_RETRY_INTERVAL = 10000;

    /**
     * If we have a connection error we try to restart after 60 seconds earliest
     */
    private static final int CONNECTION_RESTART_INTERVAL = 60000;

    /**
     * The maximum number of retries to reestablish a connection until we sleep
     * until the user uses the UI or
     * Settings.TCP_RETRY_BROKEN_CONNECTION_INTERVAL calls another retry.
     */
    private static final int MAX_NUMBER_RETRIES = 3;
    
    private static final int BYTE_ARRAY_OUTPUT_STREAM_SIZE = 2048; // bytes

    private Thread mThread;

    private RemoteService mService;

    private DecoderThread mDecoder;

    private boolean mConnectionShouldBeRunning;

    private boolean mDidCriticalErrorOccur;

    private InputStream mIs;

    private OutputStream mOs;

    private String mRpgTcpUrl;

    private int mRpgTcpPort;

    private Socket mSocket;

    private HeartbeatSenderThread mHeartbeatSender;

    private ResponseReaderThread mResponseReader;

    private long mLastErrorRetryTime, mLastErrorTimestamp;
    
    private ByteArrayOutputStream mBaos;
    
    public TcpConnectionThread(DecoderThread decoder, RemoteService service) {
        mSocket = new Socket();
        mBaos = new ByteArrayOutputStream(BYTE_ARRAY_OUTPUT_STREAM_SIZE);

        mConnectionShouldBeRunning = true;

        mDecoder = decoder;
        mService = service;

        mLastErrorRetryTime = System.currentTimeMillis();
        mLastErrorTimestamp = 0;

        try {
            mRpgTcpUrl = SettingsManager.getProperty(Settings.TCP_RPG_URL_KEY);
            mRpgTcpPort = Integer.parseInt(SettingsManager.getProperty(Settings.TCP_RPG_PORT_KEY));
        } catch (Exception e) {
            HttpConnectionThread.logE("TcpConnectionThread()", "Could not parse URL or Port!", e);
            mRpgTcpUrl = RPG_FALLBACK_TCP_URL;
            mRpgTcpPort = RPG_DEFAULT_TCP_PORT;
        }
    }

    public void run() {
        QueueManager requestQueue = QueueManager.getInstance();
        mDidCriticalErrorOccur = false;

        try { // start the initial connection
            reconnectSocket();
            HeartbeatSenderThread hbSender = new HeartbeatSenderThread(this, mService, mSocket);
            hbSender.setOutputStream(mOs);
            hbSender.sendHeartbeat();
            hbSender = null;
            // TODO run this when BE supports it but keep HB in front!
            /*
             * ConnectionTester connTester = new ConnectionTester(mIs, mOs); if
             * (connTester.runTest()) { } else {}
             */

            startHelperThreads();

            ConnectionManager.getInstance().onConnectionStateChanged(ITcpConnectionListener.STATE_CONNECTED);
            
        } catch (IOException e) {
            haltAndRetryConnection(1);
        } catch (Exception e) {
            haltAndRetryConnection(1);
        }

        while (mConnectionShouldBeRunning) {
            try {
                if ((null != mOs) && (!mDidCriticalErrorOccur)) {
                    List<Request> reqs = QueueManager.getInstance().getRpgRequests();
                    int reqNum = reqs.size();

                    List<Integer> reqIdList = null;
                    if (Settings.ENABLED_TRANSPORT_TRACE ||
                            Settings.sEnableSuperExpensiveResponseFileLogging) {
                        reqIdList = new ArrayList<Integer>();
                    }

                    if (reqNum > 0) {
                        mBaos.reset();

                        // batch payloads
                        for (int i = 0; i < reqNum; i++) {
                            Request req = reqs.get(i);

                            if ((null == req) || (req.getAuthenticationType() == Request.USE_API)) {
                                HttpConnectionThread.logV("TcpConnectionThread.run()",
                                        "Ignoring non-RPG method");
                                continue;
                            }

                            HttpConnectionThread.logD("TcpConnectionThread.run()", "Preparing ["
                                    + req.getRequestId() + "] for sending via RPG...");

                            req.setActive(true);
                            req.writeToOutputStream(mBaos, true);

                            if (req.isFireAndForget()) { // f-a-f, no response,
                                                         // remove from queue
                                HttpConnectionThread.logD("TcpConnectionThread.run()",
                                        "Removed F&F-Request: " + req.getRequestId());
                                requestQueue.removeRequest(req.getRequestId());
                            }

                            if (Settings.ENABLED_TRANSPORT_TRACE) {
                                reqIdList.add(req.getRequestId());
                                HttpConnectionThread.logD("HttpConnectionThread.run()", "Req ID: "
                                        + req.getRequestId() + " <-> Auth: " + req.getAuth());
                            }
                        }

                        mBaos.flush();
                        byte[] payload = mBaos.toByteArray();

                        if (null != payload) {
                            // log file containing response to SD card
                            if (Settings.sEnableSuperExpensiveResponseFileLogging) {
                                StringBuffer sb = new StringBuffer();
                                for (int i = 0; i < reqIdList.size(); i++) {
                                    sb.append(reqIdList.get(i));
                                    sb.append("_");
                                }
                                
                                LogUtils.logE("XXXXXXYYYXXXXXX Do not Remove this!");
                                LogUtils.logToFile(payload, "people_" 
                                            + reqIdList.get(0) + "_"
                                            + System.currentTimeMillis()
                                            + "_req_" + ((int) payload[2])  // message type
                                            + ".txt");
                            } // end log file containing response to SD card
                            
                            if (Settings.ENABLED_TRANSPORT_TRACE) {
                                Long userID = null;
                                AuthSessionHolder auth = LoginEngine.getSession();
                                if (auth != null) {
                                    userID = auth.userID;
                                }
                                
                                HttpConnectionThread.logI("TcpConnectionThread.run()",
                                        "\n  > Sending request(s) "
                                                + reqIdList.toString()
                                                + ", for user ID "
                                                + userID
                                                + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                                                + HessianUtils
                                                        .getInHessian(new ByteArrayInputStream(
                                                                payload), true) + "\n  ");
                            }

                            try {
                                synchronized (mOs) {
                                    mOs.write(payload);
                                    mOs.flush();
                                }
                            } catch (IOException ioe) {
                                HttpConnectionThread.logE("TcpConnectionThread.run()",
                                        "Could not send request", ioe);
                                notifyOfNetworkProblems();
                            }
                            payload = null;
                        }
                    }
                }

                synchronized (this) {
                    if (!mDidCriticalErrorOccur) {
                        wait();
                    } else {
                        while (mDidCriticalErrorOccur) { // loop until a retry
                                                         // succeeds
                            HttpConnectionThread.logI("TcpConnectionThread.run()",
                                    "Wait() for next connection retry has started.");
                            wait(Settings.TCP_RETRY_BROKEN_CONNECTION_INTERVAL);
                            if (mConnectionShouldBeRunning) {
                                haltAndRetryConnection(1);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // FlurryAgent.onError("ERROR_TCP_THREAD_CRASHED", "run()",
                // "TcpConnectionThread");
                HttpConnectionThread.logE("TcpConnectionThread.run()", "Unknown Error: ", t);
            }
        }

        stopConnection();
    }

    /**
     * Gets notified whenever the user is using the UI. In this special case
     * whenever an error occured with the connection and this method was not
     * called a short time before
     */
    @Override
    public void notifyOfUiActivity() {
        if (mDidCriticalErrorOccur) {
            if ((System.currentTimeMillis() - mLastErrorRetryTime) >= CONNECTION_RESTART_INTERVAL) {
                synchronized (this) {
                    notify();
                }

                mLastErrorRetryTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void notifyOfRegainedNetworkCoverage() {
        synchronized (this) {
            notify();
        }
    }

    /**
     * Called back by the response reader, which should notice network problems
     * first
     */
    protected void notifyOfNetworkProblems() {
        HttpConnectionThread.logE("TcpConnectionThread.notifyOfNetworkProblems()",
                "Houston, we have a network problem!", null);
        haltAndRetryConnection(1);
    }

    /**
     * Attempts to reconnect the socket if it has been closed for some reason.
     * 
     * @throws IOException Thrown if something goes wrong while reconnecting the
     *             socket.
     */
    private void reconnectSocket() throws IOException {
        HttpConnectionThread.logI("TcpConnectionThread.reconnectSocket()",
                "Reconnecting Socket on " + mRpgTcpUrl + ":" + mRpgTcpPort);
        mSocket = null;
        mSocket = new Socket();
        mSocket.connect(new InetSocketAddress(mRpgTcpUrl, mRpgTcpPort), TCP_DEFAULT_TIMEOUT);

        mIs = mSocket.getInputStream();
        mOs = mSocket.getOutputStream();
        HttpConnectionThread.logI("TcpConnectionThread.reconnectSocket()", "Socket started: "
                + mRpgTcpUrl + ":" + mRpgTcpPort);
    }

    /**
     * <p>
     * Retries to establish a network connection after a network error has
     * occurred or the coverage of the network was lost. The amount of retries
     * depends on MAX_NUMBER_RETRIES.
     * </p>
     * <p>
     * A new retry is carried out each time an exception is thrown until the
     * limit of retries has been reached.
     * </p>
     * 
     * @param numberOfRetries The amount of retries carried out until the
     *            connection is given up.
     */
    synchronized private void haltAndRetryConnection(int numberOfRetries) {
        HttpConnectionThread.logI("TcpConnectionThread.haltAndRetryConnection()",
                "\n \n \nRETRYING CONNECTION: " + numberOfRetries + " tries.");
        
        ConnectionManager.getInstance().onConnectionStateChanged(ITcpConnectionListener.STATE_CONNECTING);

        if (!mConnectionShouldBeRunning) { // connection was killed by service
                                           // agent
            HttpConnectionThread.logI("TcpConnectionThread.haltAndRetryConnection()", "Connection "
                    + "was disconnected by Service Agent. Stopping retries!");
            return;
        }

        stopConnection(); // stop to kill anything that might cause further IOEs

        // if we retried enough, we just return and end further retries
        if (numberOfRetries > MAX_NUMBER_RETRIES) {
            invalidateRequests();
            mDidCriticalErrorOccur = true;

            mLastErrorTimestamp = System.currentTimeMillis();
            /*
             * FlurryAgent.onError("ERROR_TCP_FAILED",
             * "Failed reconnecting for the 3rd time. Stopping!" +
             * mLastErrorTimestamp, "TcpConnectionThread");
             */
            synchronized (this) {
                notify(); // notify as we might be currently blocked on a
                          // request's wait()
            }

            ConnectionManager.getInstance().onConnectionStateChanged(ITcpConnectionListener.STATE_DISCONNECTED);

            return;
        }

        try { // sleep a while to let the connection recover
            int sleepVal = (ERROR_RETRY_INTERVAL / 2) * numberOfRetries;
            Thread.sleep(sleepVal);
        } catch (InterruptedException ie) {
        }

        if (!mConnectionShouldBeRunning) {
            return;
        }
        try {
            reconnectSocket();

            // TODO switch this block with the test connection block below
            // once the RPG implements this correctly.
            HeartbeatSenderThread hbSender = new HeartbeatSenderThread(this, mService, mSocket);
            hbSender.setOutputStream(mOs);
            hbSender.sendHeartbeat();
            hbSender = null;
            mDidCriticalErrorOccur = false;
            if (!mConnectionShouldBeRunning) {
                return;
            }
            startHelperThreads(); // restart our connections

            // TODO add this once the BE supports it!
            /*
             * ConnectionTester connTester = new ConnectionTester(mIs, mOs); if
             * (connTester.runTest()) { mDidCriticalErrorOccur = false;
             * startHelperThreads(); // restart our connections Map<String,
             * String> map = new HashMap<String, String>();
             * map.put("Last Error Timestamp", "" + mLastErrorTimestamp);
             * map.put("Last Error Retry Timestamp", "" + mLastErrorRetryTime);
             * map.put("Current Timestamp", "" + System.currentTimeMillis());
             * map.put("Number of Retries", "" + numberOfRetries);
             * FlurryAgent.onEvent("RecoveredFromTCPError", map); } else {
             * HttpConnectionThread
             * .log("TcpConnectionThread.haltAndRetryConnection()",
             * "Could not receive TCP test response. need to retry...");
             * haltAndRetryConnection(++numberOfRetries); }
             */

            Map<String, String> map = new HashMap<String, String>();
            map.put("Last Error Timestamp", "" + mLastErrorTimestamp);
            map.put("Last Error Retry Timestamp", "" + mLastErrorRetryTime);
            map.put("Current Timestamp", "" + System.currentTimeMillis());
            map.put("Number of Retries", "" + numberOfRetries);
            // FlurryAgent.onEvent("RecoveredFromTCPError", map);

            ConnectionManager.getInstance().onConnectionStateChanged(ITcpConnectionListener.STATE_CONNECTED);

        } catch (IOException ioe) {
            HttpConnectionThread.logI("TcpConnectionThread.haltAndRetryConnection()",
                    "Failed sending heartbeat. Need to retry...");
            haltAndRetryConnection(++numberOfRetries);
        } catch (Exception e) {
            HttpConnectionThread.logE("TcpConnectionThread.haltAndRetryConnection()",
                    "An unknown error occured: ", e);
            haltAndRetryConnection(++numberOfRetries);
        }
    }

    /**
     * Invalidates all the requests so that the engines can either resend or
     * post an error message for the user.
     */
    private void invalidateRequests() {
        QueueManager reqQueue = QueueManager.getInstance();

        if (null != reqQueue) {
            TimeOutWatcher timeoutWatcher = reqQueue.getRequestTimeoutWatcher();

            if (null != timeoutWatcher) {
                timeoutWatcher.invalidateAllRequests();
            }
        }
    }

    @Override
    public void startThread() {
        if ((null != mThread) && (mThread.isAlive()) && (mConnectionShouldBeRunning)) {
            HttpConnectionThread.logI("TcpConnectionThread.startThread()",
                    "No need to start Thread. " + "Already there. Returning");
            return;
        }

        mConnectionShouldBeRunning = true;
        mThread = new Thread(this);
        mThread.start();
    }
    
    @Override
    public void stopThread() {
        HttpConnectionThread.logI("TcpConnectionThread.stopThread()", "Stop Thread was called!");

        mConnectionShouldBeRunning = false;
        stopConnection();

        synchronized (this) {
            notify();
        }
    }

    /**
     * Starts the helper threads in order to be able to read responses and send
     * heartbeats and passes them the needed input and output streams.
     */
    private void startHelperThreads() {
        HttpConnectionThread.logI("TcpConnectionThread.startHelperThreads()",
                "STARTING HELPER THREADS.");

        if (null == mHeartbeatSender) {
            mHeartbeatSender = new HeartbeatSenderThread(this, mService, mSocket);
            HeartbeatSenderThread.mCurrentThread = mHeartbeatSender;
        } else {
            HttpConnectionThread.logE("TcpConnectionThread.startHelperThreads()",
                    "HeartbeatSenderThread was not null!", null);
        }

        if (null == mResponseReader) {
            mResponseReader = new ResponseReaderThread(this, mDecoder, mSocket);
            ResponseReaderThread.mCurrentThread = mResponseReader;
        } else {
            HttpConnectionThread.logE("TcpConnectionThread.startHelperThreads()",
                    "ResponseReaderThread was not null!", null);
        }

        mHeartbeatSender.setOutputStream(mOs);
        mResponseReader.setInputStream(mIs);

        if (!mHeartbeatSender.getIsActive()) {
            mHeartbeatSender.startConnection();
            mResponseReader.startConnection();
        }
    }

    /**
     * Stops the helper threads and closes the input and output streams. As the
     * response reader is at this point in time probably in a blocking
     * read()-state an IOException will need to be caught.
     */
    private void stopHelperThreads() {
        HttpConnectionThread.logI("TcpConnectionThread.stopHelperThreads()",
                "STOPPING HELPER THREADS: "
                        + ((null != mHeartbeatSender) ? mHeartbeatSender.getIsActive() : false));

        if (null != mResponseReader) {
            mResponseReader.stopConnection();
        }
        if (null != mHeartbeatSender) {
            mHeartbeatSender.stopConnection();
        }

        mOs = null;
        mIs = null;
        mHeartbeatSender = null;
        mResponseReader = null;
    }

    /**
     * Stops the connection and its underlying socket implementation. Keeps the
     * thread running to allow further logins from the user.
     */
    synchronized private void stopConnection() {
        HttpConnectionThread.logI("TcpConnectionThread.stopConnection()", "Closing socket...");
        stopHelperThreads();

        if (null != mSocket) {
            try {
                mSocket.close();
            } catch (IOException ioe) {
                HttpConnectionThread.logE("TcpConnectionThread.stopConnection()",
                        "Could not close Socket!", ioe);
            } finally {
                mSocket = null;
            }
        }

        QueueManager.getInstance().clearAllRequests();
    }

    @Override
    public void notifyOfItemInRequestQueue() {
        HttpConnectionThread.logV("TcpConnectionThread.notifyOfItemInRequestQueue()",
                "NEW REQUEST AVAILABLE!");
        synchronized (this) {
            notify();
        }
    }

    @Override
    public boolean getIsConnected() {
        return mConnectionShouldBeRunning;
    }

    @Override
    public boolean getIsRpgConnectionActive() {
        if ((null != mHeartbeatSender) && (mHeartbeatSender.getIsActive())) {
            return true;
        }

        return false;
    }

    @Override
    public void onLoginStateChanged(boolean isLoggedIn) {
    }
}
