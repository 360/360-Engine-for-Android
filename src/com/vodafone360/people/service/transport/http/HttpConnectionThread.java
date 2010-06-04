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

package com.vodafone360.people.service.transport.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.DecoderThread.RawResponse;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.service.transport.http.authentication.AuthenticationManager;
import com.vodafone360.people.service.utils.hessian.HessianUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * HTTP connection thread handles issuing of RPG requests over HTTP to server.
 */
public class HttpConnectionThread implements Runnable, IConnection {
    private Thread mThread;

    private volatile boolean mIsConnectionRunning;

    private PollThread mPollThread;

    private DecoderThread mDecoder;

    private HttpClient mHttpClient;

    private int mRetryCount;

    private URI mRpgUrl;

    private boolean mIsPolling, mIsFirstTimePoll;

    //protected static final int E_HTTP_PROTOCOL = 2;

    private final Object mSendLock = new Object();

    private final Object mRunLock = new Object();

    public HttpConnectionThread(DecoderThread decoder) {
        super();
        mIsPolling = false;
        mIsFirstTimePoll = true;

        mDecoder = decoder;
    }

    /**
     * Starts the RPG connection as a thread and also launches the polling
     * thread.
     */
    public synchronized void startThread() {
        logI("RpgHttpConnectionThread.startThread()", "Starting Thread");

        setHttpClient();
        mIsConnectionRunning = true;

        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * Sets HTTP settings.
     */
    public void setHttpClient() {
        int connectionTimeout = Settings.HTTP_CONNECTION_TIMEOUT;
        HttpParams myHttpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(myHttpParams, connectionTimeout);
        HttpConnectionParams.setSoTimeout(myHttpParams, connectionTimeout);
        mHttpClient = new DefaultHttpClient(myHttpParams); // get http
    }

    /**
     * Stops the current thread.
     */
    public synchronized void stopThread() {
        mIsConnectionRunning = false;
        notifyOfItemInRequestQueue();// notify to make sure the thread exits if
                                     // it's waiting
        if (mPollThread != null) {
            mPollThread.stopConnection();
        }
        if (mHttpClient != null) {
            mHttpClient.getConnectionManager().shutdown();
            mHttpClient = null;
        }
    }

    /**
     * <p>
     * Sends out synchronous requests (for authentication) to the API and
     * asynchronous calls to the RPG as soon as there are requests on the
     * request queue.
     * </p>
     * <p>
     * If there are no requests the thread is set to wait().
     * </p>
     */
    public void run() {
        AuthenticationManager authMgr = null;

        authMgr = new AuthenticationManager(this);

        while (mIsConnectionRunning) { // loops through requests and sends them
                                       // out as needed
            authMgr.handleAuthRequests(); // TODO move this out. this should be
                                          // done by the LoginEngine directly

            if (null != mPollThread) {
                if (mIsFirstTimePoll) {
                    try {
                        mPollThread.invokePoll(PollThread.SHORT_POLLING_INTERVAL,
                                PollThread.DEFAULT_BATCHSIZE, PollThread.ACTIVE_MODE);
                    } catch (Exception e) {
                        // we do not do anything here as it is not a critical
                        // error
                        logI("RpgHttpConnection.run()", "Exception while doing 1st time poll!!");
                    } finally {
                        mIsFirstTimePoll = false;
                    }
                }

                List<Request> requests = QueueManager.getInstance().getApiRequests();
                if ((requests.size() > 0) && mPollThread.getHasCoverage()) {
                    if (null == mRpgUrl) { // TODO move this out of the loop
                                           // once we have a proper authMgr
                        try {
                            mRpgUrl = new URL(SettingsManager.getProperty(Settings.RPG_SERVER_KEY)
                                    + LoginEngine.getSession().userID).toURI();
                        } catch (Exception e) {
                            logE("RpgHttpConnectionThread.run()", "Could not set up URL", e);
                        }
                    }

                    mRetryCount = 0;
                    List<Integer> reqIds = new ArrayList<Integer>();
                    try {
                        byte[] reqData = prepareRPGRequests(requests, reqIds);

                        if (null != LoginEngine.getSession()) {
                            synchronized (mSendLock) {
                                if (mIsConnectionRunning) {
                                    if (Settings.ENABLED_TRANSPORT_TRACE) {
                                        HttpConnectionThread.logI("RpgTcpConnectionThread.run()",
                                                "\n \n \nSending a request: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                                                        + HessianUtils.getInHessian(
                                                                new ByteArrayInputStream(reqData),
                                                                true));
                                    }
                                    HttpResponse response = postHTTPRequest(reqData, mRpgUrl,
                                            Settings.HTTP_HEADER_CONTENT_TYPE);
                                    if (mIsConnectionRunning
                                            && SettingsManager
                                                    .getBooleanProperty(Settings.ENABLE_RPG_KEY)
                                            && handleRpgResponse(response, reqIds)) {
                                        mPollThread.startRpgPolling();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        addErrorToResponseQueue(reqIds);// add error to the
                                                        // response queue
                    }
                }
            }

            try {
                synchronized (mRunLock) {
                    mRunLock.wait();
                }
            } catch (InterruptedException ie) {
                LogUtils.logE("HttpConnectionThread.run() Wait was interrupted", ie);
            }
        }
    }

    /**
     * Takes all requests objects and writes its serialized data to a byte array
     * for further posting to the RPG.
     * 
     * @param requests A list of requests to serialize.
     * @param reqIds
     * @return The serialized requests with RPG headers. Returns NULL id list of requests is NULL.
     */
    private byte[] prepareRPGRequests(List<Request> requests, List<Integer> reqIds) {
        if (null == requests) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (Request request : requests) {
                request.writeToOutputStream(baos, true);
                request.setActive(true);
                reqIds.add(request.getRequestId());
            }
            baos.flush();
        } catch (IOException ioe) {
            LogUtils.logE("HttpConnectionThread.prepareRPGRequests() Failed writing to BAOS", ioe);
        } finally {
            try {
                baos.close();
            } catch (IOException ioe) {
                LogUtils.logE("HttpConnectionThread.prepareRPGRequests() Failed closing BAOS", ioe);
            }
        }
        byte[] reqData = baos.toByteArray();

        return reqData;
    }

    /**
     * Posts the serialized data to the RPG and synchronously grabs the
     * response.
     * 
     * @param postData The post data to send to the RPG.
     * @param uri The URL to send the request to.
     * @param contentType The content type to send as, usually
     *            "application/binary)
     * @return The response data as a byte array.
     * @throws An exception if the request went wrong after HTTP_MAX_RETRY_COUNT
     *             retries.
     */
    public HttpResponse postHTTPRequest(byte[] postData, URI uri, String contentType)
            throws Exception {
        
        HttpResponse response = null;

        if (null == postData) {
            return response;
        }
        mRetryCount++;
        if (uri != null) {
            logI("RpgHttpConnectionThread.postHTTPRequest()", "HTTP Requesting URI " + uri.toString()
                    + " " + contentType);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", contentType);
            httpPost.addHeader("User-Agent", "PeopleRPGClient/1.0");
            httpPost.addHeader("Cache-Control", "no-cache");
            httpPost.setEntity(new ByteArrayEntity(postData));
            try {
                response = mHttpClient.execute(httpPost);
            } catch (Exception e) {
                e.printStackTrace();
                // repeat the request N times
                if (mRetryCount < Settings.HTTP_MAX_RETRY_COUNT) {
                    return postHTTPRequest(postData, uri, contentType);
                } else {
                    throw new Exception("Could not post request " + e);
                }
            }
        }
        return response;
    }

    /**
     * Checks if the response to an RPG request was fired off correctly.
     * Basically this method only checks whether the response is returned under
     * a HTTP 200 status.
     * 
     * @param response The response to check for.
     * @param reqIds The request IDs for the response.
     * @return True if the RPG response returned correctly.
     * @throws Exception Thrown if the response was null or the status line could
     *             not be fetched.
     */
    private boolean handleRpgResponse(HttpResponse response, List<Integer> reqIds) throws Exception {
        boolean ret = false;
        if (null != response) {
            if (null != response.getStatusLine()) {
                int respCode = response.getStatusLine().getStatusCode();
                logI("RpgHttpConnectionThread.handleRpgResponse()",
                        "HTTP BINARY Got response status: " + respCode);
                switch (respCode) {
                    case HttpStatus.SC_OK:
                    case HttpStatus.SC_CONTINUE:
                    case HttpStatus.SC_CREATED:
                    case HttpStatus.SC_ACCEPTED:
                    case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
                    case HttpStatus.SC_NO_CONTENT:
                    case HttpStatus.SC_RESET_CONTENT:
                    case HttpStatus.SC_PARTIAL_CONTENT:
                    case HttpStatus.SC_MULTI_STATUS:
                        ret = true;
                        break;
                    default:
                        addErrorToResponseQueue(reqIds);
                }
                finishResponse(response);
            } else {
                throw new Exception("Status line of response was null.");
            }
        } else {
            throw new Exception("Response was null.");
        }
        return ret;
    }

    /**
     * Finishes reading the response in order to unblock the current connection.
     * 
     * @param response The response to finish reading on.
     */
    private void finishResponse(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                // this is important! otherwise the connection remains
                // unusable!!
                entity.consumeContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles the synchronous responses for the authentication calls which go
     * against the API directly by adding it to the queue and checking if the
     * response code was a HTTP 200. TODO: this should be refactored into a
     * AuthenticationManager class.
     * 
     * @param response The response to add to the decoder.
     * @param reqIds The request IDs the response is to be decoded for.
     * @throws Exception Thrown if the status line could not be read or the
     *             response is null.
     */
    public void handleApiResponse(HttpResponse response, List<Integer> reqIds) throws Exception {
        byte[] ret = null;
        if (null != response) {
            if (null != response.getStatusLine()) {
                int respCode = response.getStatusLine().getStatusCode();
                logI("RpgHttpConnectionThread.handleApiResponse()", "HTTP Got response status: "
                        + respCode);
                switch (respCode) {
                    case HttpStatus.SC_OK:
                    case HttpStatus.SC_CONTINUE:
                    case HttpStatus.SC_CREATED:
                    case HttpStatus.SC_ACCEPTED:
                    case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
                    case HttpStatus.SC_NO_CONTENT:
                    case HttpStatus.SC_RESET_CONTENT:
                    case HttpStatus.SC_PARTIAL_CONTENT:
                    case HttpStatus.SC_MULTI_STATUS:
                        HttpEntity entity = response.getEntity();
                        if (null != entity) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();

                            InputStream is = entity.getContent();
                            if (null != is) {
                                int nextByte = 0;
                                while ((nextByte = is.read()) != -1) {
                                    baos.write(nextByte);
                                }
                                baos.flush();
                                ret = baos.toByteArray();
                                baos.close();
                                baos = null;
                            }
                            entity.consumeContent();
                        }

                        if (Settings.ENABLED_TRANSPORT_TRACE) {
                            int length = 0;
                            if (ret != null) {
                                length = ret.length;
                            }
                            HttpConnectionThread.logI("ResponseReader.handleApiResponse()",
                                    "\n \n \n"
                                            + "Response with length "
                                            + length
                                            + " bytes received "
                                            + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
                                            + (length == 0 ? "" : HessianUtils.getInHessian(
                                                    new ByteArrayInputStream(ret), false)));

                        }
                        addToDecoder(ret, reqIds);
                        break;
                    default:
                        addErrorToResponseQueue(reqIds);
                }
            } else {
                throw new Exception("Status line of response was null.");
            }
        } else {
            throw new Exception("Response was null.");
        }
    }

    /**
     * Adds a response to the response decoder.
     * 
     * @param input The data of the response.
     * @param reqIds The request IDs that a response was received for.
     */
    private void addToDecoder(byte[] input, List<Integer> reqIds) {
        if (input != null && mDecoder != null) {
            int reqId = reqIds.size() > 0 ? reqIds.get(0) : 0;
            mDecoder.addToDecode(new RawResponse(reqId, input, false, false));
            logI("RpgHttpConnectionThread.handleApiResponse()", "Added response(s) to decoder: "
                    + reqIds.toString());
        }
    }

    /**
     * Adds errors to the response queue whenever there is an HTTP error on the
     * backend.
     * 
     * @param reqIds The request IDs the error happened for.
     */
    public void addErrorToResponseQueue(List<Integer> reqIds) {
        EngineId source = null;
        QueueManager requestQueue = QueueManager.getInstance();
        ResponseQueue responseQueue = ResponseQueue.getInstance();
        for (Integer reqId : reqIds) {
            // attempt to get type from request
            Request req = requestQueue.getRequest(reqId);
            if (req != null)
                source = req.mEngineId;
            responseQueue.addToResponseQueue(reqId, null, source);
        }
    }

    /**
     * Kicks the request queue as soon as there are more requests on the queue.
     */
    @Override
    public void notifyOfItemInRequestQueue() {
        HttpConnectionThread.logI("HttpConnectionThread.notifyOfItemInRequestQueue()",
                "NEW REQUEST AVAILABLE!");
        synchronized (mRunLock) {
            mRunLock.notify();
        }
    }

    /**
     * Called whenever the device regains network coverage. It will kick the
     * request queue to see if there are more requests to send.
     */
    @Override
    public void notifyOfRegainedNetworkCoverage() {
        synchronized (mRunLock) {
            mRunLock.notify();
        }
    }

    /**
     * This method is called when log-in is detected. Polling should not
     * normally start before we are logged in as the backend needs a correct
     * user id.
     */
    private synchronized void startPollThread() {
        if (null == mPollThread) {
            mPollThread = new PollThread(this);
        }

        if (mIsConnectionRunning) {
            mIsPolling = true;
            mPollThread.startConnection(mDecoder);
        }
    }

    /**
     * Stops the polling thread. This method is implemented from the
     * IQueueListener-interface. It is called when log-out of the account is
     * detected but a connection is still ongoing. In this case polling needs to
     * stop.
     */
    private synchronized void stopPollThread() {
        if (mPollThread != null) {
            mIsPolling = false;
            mPollThread.stopConnection();
        }
        // clear all requests in RequestQueue
        QueueManager.getInstance().clearActiveRequests(false);
    }

    @Override
    public void onLoginStateChanged(boolean isLoggedIn) {
        if (!isLoggedIn && mIsConnectionRunning) {
            logI("RpgHttpConnectionThread.onLoginStateChanged()", "Stopping to poll.");
            stopPollThread();
            mIsPolling = false;
        } else if (!mIsConnectionRunning) {
            startThread();
        } else if (isLoggedIn && !mIsPolling) { // if connected but not Polling
            if (mIsConnectionRunning) {
                logI("RpgHttpConnectionThread.onLoginStateChanged()", "Starting Poll");
                startPollThread();
                mIsPolling = true;
            }
        }
    }

    @Override
    public boolean getIsConnected() {
        return mIsConnectionRunning;
    }

    public static void logE(String tag, String message, Throwable error) {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            Thread t = Thread.currentThread();
            if (null != error) {
                Log.e("(PROTOCOL)",
                        "\n \n \n ################################################## \n" + tag
                                + "[" + t.getName() + "]" + " : " + message, error);
            } else {
                Log.e("(PROTOCOL)",
                        "\n \n \n ################################################## \n" + tag
                                + "[" + t.getName() + "]" + " : " + message);
            }
        }

        if (null != error) {
            LogUtils.logE(message + " : " + error.toString());
        } else {
            LogUtils.logE(message
                    + " (Note: Settings.ENABLED_TRANSPORT_TRACE might give you more details!)");
        }
    }

    public static void logW(String tag, String message) {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            Thread t = Thread.currentThread();
            Log.w("(PROTOCOL)", tag + "[" + t.getName() + "] ################### " + " : "
                    + message);
        }
    }

    public static void logI(String tag, String message) {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            Thread t = Thread.currentThread();
            Log.i("(PROTOCOL)", tag + "[" + t.getName() + "]" + " : " + message);
        }
    }

    public static void logD(String tag, String message) {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            Thread t = Thread.currentThread();
            Log.d("(PROTOCOL)", tag + "[" + t.getName() + "]" + " : " + message);
        }
    }

    public static void logV(String tag, String message) {
        if (Settings.ENABLED_TRANSPORT_TRACE) {
            Thread t = Thread.currentThread();
            Log.v("(PROTOCOL)", tag + "[" + t.getName() + "]" + " : " + message);
        }
    }

    @Override
    public boolean getIsRpgConnectionActive() {
        return mIsPolling;
    }

    @Override
    public void notifyOfUiActivity() {
        // TODO Auto-generated method stub

    }
}
