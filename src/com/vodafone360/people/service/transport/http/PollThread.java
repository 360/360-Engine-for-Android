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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
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
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.rpg.RpgHeader;
import com.vodafone360.people.service.io.rpg.RpgHelper;
import com.vodafone360.people.service.io.rpg.RpgMessage;
import com.vodafone360.people.service.io.rpg.RpgMessageTypes;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.DecoderThread.RawResponse;
import com.vodafone360.people.service.utils.AuthUtils;
import com.vodafone360.people.service.utils.hessian.HessianEncoder;
import com.vodafone360.people.utils.LogUtils;

public class PollThread implements Runnable {
    private static final String RPG_MODE_KEY = "mode", RPG_MODE_ACTIVE = "active",
            RPG_BATCH_SIZE_KEY = "batchsize", RPG_POLLING_INTERVAL_KEY = "pollinterval";

    protected static final byte ACTIVE_MODE = 1;

    private static final byte IDLE_MODE = 2;

    private static final int LONG_POLLING_INTERVAL = 30;

    protected static final int SHORT_POLLING_INTERVAL = 1;

    protected static final int DEFAULT_BATCHSIZE = 24000;

    private static final int NETWORK_NO_COVERAGE_RETRIES = 6;

    private static final long NETWORK_RETRY_INTERVAL = 30000;

    private HttpConnectionThread mRpgRequesterThread;

    private DecoderThread mDecoder;

    private boolean mIsConnectionRunning;

    private boolean mHasErrorOccured;

    private int mBlankHeaderCount;

    private int mRetryCount;

    private byte mMode;

    private int mBatchsize;

    private URI mUrl;

    private HttpClient mHttpClient;

    private RpgHeader mHeader;

    private final Object mPollLock = new Object();

    private final Object mRunLock = new Object();

    protected PollThread(HttpConnectionThread connThread) {
        mBatchsize = DEFAULT_BATCHSIZE;
        mRpgRequesterThread = connThread;
        mHasErrorOccured = false;
    }

    /**
     * Starts the connection.
     */
    protected synchronized void startConnection(DecoderThread decoder) {
        mDecoder = decoder;
        setHttpClient();

        mIsConnectionRunning = true;
        try {
            URL url = new URL(SettingsManager.getProperty(Settings.RPG_SERVER_KEY)
                    + LoginEngine.getSession().userID);
            mUrl = url.toURI();
            mHeader = new RpgHeader();
            mMode = ACTIVE_MODE;

            Thread t = new Thread(this);
            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setHttpClient() {
        int connectionTimeout = 2 * Settings.HTTP_CONNECTION_TIMEOUT;
        HttpParams myHttpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(myHttpParams, connectionTimeout);
        HttpConnectionParams.setSoTimeout(myHttpParams, connectionTimeout);
        mHttpClient = new DefaultHttpClient(myHttpParams);
    }

    /**
     * Attempts to stop the connection. This method might only take effect after
     * a maximum of 30 seconds when the poll returns.
     */
    protected synchronized void stopConnection() {
        mIsConnectionRunning = false;
        synchronized (mRunLock) {
            mMode = IDLE_MODE;
            mRunLock.notify();
        }
        mDecoder = null;
        mHeader = null;
        if (mHttpClient != null) {
            mHttpClient.getConnectionManager().shutdown();
            mHttpClient = null;
        }
    }

    /**
     * Carries out an initial poll with a short interval to have the RPG set up
     * the presence roosters then does poll after poll with the default polling
     * interval to keep the connection alive.
     */
    public void run() {
        try {
            invokePoll(SHORT_POLLING_INTERVAL, mBatchsize, ACTIVE_MODE);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        while (mIsConnectionRunning) {
            if (mMode == ACTIVE_MODE) {
                synchronized (mPollLock) {
                    mRetryCount = 0;
                    if (mMode == ACTIVE_MODE) {
                        try {
                            invokePoll(LONG_POLLING_INTERVAL, mBatchsize, mMode);
                        } catch (ClientProtocolException cpe) {
                            mHasErrorOccured = true;
                        } catch (IOException ioe) {
                            mHasErrorOccured = true;
                        } catch (Exception e) {
                            QueueManager.getInstance().clearActiveRequests(true);
                            LogUtils.logE("POLLTIMETEST", e);
                        } finally {
                            if (mIsConnectionRunning)
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ie) {
                                }
                        }

                        if (mHasErrorOccured) {
                            retryConnection();
                        }
                    }
                }
            } else { // ensure the wake-up
                synchronized (mRunLock) {
                    try {
                        mRunLock.wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
    }

    /**
     * Invokes a poll on the RPG with the passed arguments.
     * 
     * @param pollInterval The polling interval the server takes as an argument.
     *            LONG_POLLING_INTERVAL should be used for all normal polls,
     *            SHORT_POLLING_INTERVAL for the initial poll.
     * @param batchSize The maximum batch size of the client. DEFAULT_BATCHSIZE
     *            should be used by default.
     * @param mode The mode to use. ACTIVE_MODE and IDLE_MODE are available.
     */
    protected void invokePoll(int pollInterval, int batchSize, byte mode) throws Exception {
        if (mIsConnectionRunning) {
            byte[] pollData = serializeRPGPoll(pollInterval, batchSize, mode);
            if (pollData != null) {
                HttpResponse response = postHTTPRequest(pollData, mUrl,
                        Settings.HTTP_HEADER_CONTENT_TYPE);
                if (mMode == ACTIVE_MODE)
                    handleResponse(response);
            }
        }
    }

    /**
     * Posts an HTTP request with data to a URL under a certain content type.
     * The method will retry HTTP_MAX_RETRY_COUNT number of times until it
     * throws an exception.
     * 
     * @param postData A byte array representing the data to be posted.
     * @param uri The URI to post the data to.
     * @param contentType The content type to post under.
     * @return The response of the server.
     * @throws Exception Thrown if the request failed for HTTP_MAX_RETRY_COUNT
     *             number of times.
     */
    private HttpResponse postHTTPRequest(byte[] postData, URI uri, String contentType)
            throws Exception {

        HttpResponse response = null;

        if (null == postData) {
            return response;
        }
        mRetryCount++;
        if (uri != null) {
            Log.d("POLLTIMETEST", "POLL Requesting URI " + uri.toString());
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", contentType);
            httpPost.setEntity(new ByteArrayEntity(postData));
            Log.d("POLLTIMETEST", "POLL Requesting URI " + httpPost.getRequestLine());
            try {
                response = mHttpClient.execute(httpPost);
            } catch (Exception e) {
                e.printStackTrace();
                if (mRetryCount < Settings.HTTP_MAX_RETRY_COUNT) {
                    postHTTPRequest(postData, uri, contentType);
                } else {
                    throw e;
                }
            }
        }
        return response;
    }

    /**
     * <p>
     * Looks at the response and adds it to the necessary decoder.
     * </p>
     * TODO: this method should be worked on. The decoder should take care of
     * deciding which methods are decoded in which way.
     * 
     * @param response The server response to decode.
     * @throws Exception Thrown if the returned status line was null or if the
     *             response was null.
     */
    private void handleResponse(HttpResponse response) throws Exception {
        InputStream mDataStream = null;
        if (response != null) {
            if (null != response.getStatusLine()) {
                int respCode = response.getStatusLine().getStatusCode();
                Log.d("POLLTIMETEST", "POLL Got response status: " + respCode);
                switch (respCode) {
                    case HttpStatus.SC_OK:
                        try {
                            mDataStream = response.getEntity().getContent();
                            List<RpgMessage> mRpgMessages = new ArrayList<RpgMessage>();
                            // Get array of RPG messages
                            // throws IO Exception, we pass it to the calling
                            // method
                            RpgHelper.splitRpgResponse(mDataStream, mRpgMessages);
                            byte[] body = null;
                            RpgHeader rpgHeader = null;
                            // Process each header
                            for (RpgMessage mRpgMessage : mRpgMessages) {
                                body = mRpgMessage.body();
                                rpgHeader = mRpgMessage.header();
                                // Determine RPG mssageType (internal response,
                                // push
                                // etc)
                                int mMessageType = rpgHeader.reqType();
                                if (mMessageType == RpgMessageTypes.RPG_POLL_MESSAGE) {
                                    mBlankHeaderCount++;
                                    Log.e("POLLTIMETEST",
                                            "POLL handleResponse(): blank poll responses");
                                    if (mBlankHeaderCount == Settings.BLANK_RPG_HEADER_COUNT) {
                                        Log.e("POLLTIMETEST", "POLL handleResponse(): "
                                                + Settings.BLANK_RPG_HEADER_COUNT
                                                + " blank poll responses");
                                        stopRpgPolling();
                                    }
                                } else {
                                    Log.d("POLLTIMETEST",
                                            "POLL handleResponse() Non-RPG_POLL_MESSAGE");
                                    // Reset blank header counter
                                    mBlankHeaderCount = 0;
                                    boolean mZipped = mRpgMessage.header().compression();
                                    if (body != null && (body.length > 0)) {
                                        switch (mMessageType) {
                                            case RpgMessageTypes.RPG_EXT_RESP:
                                                // External message response
                                                Log.d("POLLTIMETEST",
                                                      "POLLhandleResponse() RpgMessageTypes.RPG_EXT_RESP - "
                                                      + "Add External Message RawResponse to Decode queue:"
                                                      + rpgHeader.reqId()
                                                      + "mBody.len="
                                                      + body.length);
                                                mDecoder.addToDecode(new RawResponse(
                                                        rpgHeader.reqId(), body, mZipped, false));
                                                break;
                                            case RpgMessageTypes.RPG_PUSH_MSG:
                                                // Define push message callback
                                                // to
                                                // notify controller
                                                Log.d("POLLTIMETEST",
                                                      "POLLhandleResponse() "
                                                      + "RpgMessageTypes.RPG_PUSH_MSG - "
                                                      + "Add Push Message RawResponse to Decode queue:"
                                                      + 0 + "mBody.len="
                                                      + body.length);
                                                mDecoder.addToDecode(new RawResponse(0,
                                                        body, mZipped, true));
                                                break;
                                            case RpgMessageTypes.RPG_INT_RESP:
                                                // Internal message response
                                                Log.d("POLLTIMETEST",
                                                      "POLLhandleResponse()" +
                                                      " RpgMessageTypes.RPG_INT_RESP -" +
                                                      " Add RawResponse to Decode queue:"
                                                      + rpgHeader.reqId() + "mBody.len="
                                                      + body.length);
                                                mDecoder.addToDecode(new RawResponse(
                                                        rpgHeader.reqId(), body, mZipped, false));
                                                break;
                                            case RpgMessageTypes.RPG_PRESENCE_RESPONSE:
                                                Log.d("POLLTIMETEST",
                                                      "POLLhandleResponse() " +
                                                      "RpgMessageTypes.RPG_PRESENCE_RESPONSE" +
                                                      " - Add RawResponse to Decode queue - mZipped["
                                                      + mZipped
                                                      + "]"
                                                      + "mBody.len="
                                                      + body.length);
                                                mDecoder.addToDecode(new RawResponse(
                                                        rpgHeader.reqId(), body, mZipped, false));
                                                break;
                                                
                                            default:
                                                // Do nothing.
                                                break;
                                        }
                                    }
                                }
                            }
                        } finally {
                            if (mDataStream != null)
                                try {
                                    mDataStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    mDataStream = null;
                                }
                        }
                        break;
                    default:
                        stopRpgPolling();
                        Log.e("POLLTIMETEST", "POLL handleResponse() not OK status code:"
                                + respCode);
                }
                consumeResponse(response);
            } else {
                mMode = IDLE_MODE;
                throw new Exception("POLL Response status line was null.");
            }
        } else {
            mMode = IDLE_MODE;
            throw new Exception("POLL Response was null.");
        }
    }

    private void consumeResponse(HttpResponse response) {
        HttpEntity entity = response.getEntity();
        if (entity != null)
            try {
                entity.consumeContent();
            } catch (IOException e) {
                LogUtils.logE("RpgHttpPollThread.consumeResponse()", e);
            }
    }

    /**
     * Serializes a poll to hessian and includes the RPG header.
     * 
     * @param pollInterval The polling interval to use. LONG_POLLING_INTERVAL or
     *            SHORT_POLLING_INTERVAL.
     * @param batchSize The batch size to use on the RPG.
     * @param mode The mode to use. Possible values: RPG_MODE_ACTIVE,
     *            RPG_MODE_IDLE
     * @return The serialized hessian-data as a byte-array.
     * @throws IOException
     */
    private byte[] serializeRPGPoll(int pollInterval, int batchSize, byte mode) throws IOException {
        LogUtils.logD("PollThread.pollRpg()");

        // hash table for parameters to Hessian encode
        Hashtable<String, Object> ht = new Hashtable<String, Object>();
        ht.put("auth", AuthUtils.calculateAuth("", new Hashtable<String, Object>(), ""
                + ((long)System.currentTimeMillis() / 1000), LoginEngine.getSession()));

        // we only put in the polling interval if it is not the default interval
        if (LONG_POLLING_INTERVAL != pollInterval) {
            ht.put(RPG_POLLING_INTERVAL_KEY, pollInterval);
        }
        // TODO if we want to support sms wakeup in the future we should change
        // this to support RPG_MODE_IDLE
        ht.put(RPG_MODE_KEY, RPG_MODE_ACTIVE);
        ht.put(RPG_BATCH_SIZE_KEY, DEFAULT_BATCHSIZE);

        // do Hessian encoding
        byte[] payload = HessianEncoder.createHessianByteArray("", ht);
        payload[1] = (byte)1;
        payload[2] = (byte)0;

        int reqLength = RpgHeader.HEADER_LENGTH;
        reqLength += payload.length;
        mHeader.setPayloadLength(payload.length);

        byte[] rpgMsg = new byte[reqLength];
        System.arraycopy(mHeader.createHeader(), 0, rpgMsg, 0, RpgHeader.HEADER_LENGTH);

        if (null != payload) {
            System.arraycopy(payload, 0, rpgMsg, RpgHeader.HEADER_LENGTH, payload.length);
        }
        return rpgMsg;
    }

    /**
     * We need to inform controller about the error here. EngineManager should
     * retry it's requests.
     */
    private void stopRpgPolling() {
        synchronized (mRunLock) {
            LogUtils.logD("PollThread.stopRpgPolling()");
            mMode = IDLE_MODE;
            // inform controller
            QueueManager.getInstance().clearActiveRequests(true);
            // reset counter
            mBlankHeaderCount = 0;
        }
    }

    /**
     * Starts the RPG polling in active mode.
     * 
     * @throws MalformedURLException Thrown if the URL is not in the correct
     *             format.
     * @throws URISyntaxException Thrown if the URL is not in the correct
     *             format.
     */
    protected void startRpgPolling() throws MalformedURLException, URISyntaxException {
        if (mMode != ACTIVE_MODE) {
            LogUtils.logD("PollThread.startRpgPolling()");
            URL url = new URL(SettingsManager.getProperty(Settings.RPG_SERVER_KEY)
                    + LoginEngine.getSession().userID);
            mUrl = url.toURI();
            if (mIsConnectionRunning) {
                synchronized (mRunLock) {
                    mMode = ACTIVE_MODE;
                    mRunLock.notify();
                }
            }
        }
    }

    /**
     * Retries to connect to the backend (if in active mode) every
     * NETWORK_RETRY_INTERVAL seconds.
     */
    private void retryConnection() {
        int numberOfRetries = 0;

        while (mHasErrorOccured && (numberOfRetries <= NETWORK_NO_COVERAGE_RETRIES)) {
            try {
                invokePoll(SHORT_POLLING_INTERVAL, DEFAULT_BATCHSIZE, ACTIVE_MODE);
                mHasErrorOccured = false;
            } catch (Exception e) {
                try {
                    Thread.sleep(NETWORK_RETRY_INTERVAL);
                } catch (Exception ee) {
                    LogUtils.logE("PollThread.retryConnection()" + " exception thrown " + ee.toString());
                }
            }
        }

        if (!mHasErrorOccured) {
            // notify requester that we have coverage again
            mRpgRequesterThread.notifyOfRegainedNetworkCoverage();
        } else {
            // no coverage after several retries. let's go into idle mode
            mMode = IDLE_MODE;
        }
    }

    /**
     * Returns true if the device has coverage and the servers are not down.
     * 
     * @return True if there is coverage.
     */
    public boolean getHasCoverage() {
        return !mHasErrorOccured;
    }
}
