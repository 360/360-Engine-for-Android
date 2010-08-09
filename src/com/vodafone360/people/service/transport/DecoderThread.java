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

package com.vodafone360.people.service.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.Request.Type;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.rpg.RpgHeader;
import com.vodafone360.people.service.io.rpg.RpgHelper;
import com.vodafone360.people.service.io.rpg.RpgMessage;
import com.vodafone360.people.service.io.rpg.RpgMessageTypes;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.hessian.HessianDecoder;
import com.vodafone360.people.utils.LogUtils;

/**
 * Responsible for decoding 'raw' Hessian response data. Data is decoded into
 * specific data types and added to the response queue. The Response queue
 * stores request id (except for unsolicited Push msgs) and a source/destination
 * engine to allow appropriate routing.
 */
public class DecoderThread implements Runnable {

    private static final String THREAD_NAME = "DecoderThread";

    private static final long THREAD_SLEEP_TIME = 300; // ms

    private volatile boolean mRunning = false;

    private final List<RawResponse> mResponses = new ArrayList<RawResponse>();

    private ResponseQueue mRespQueue = null;

    /**
     * The hessian decoder is here declared as member and will be reused instead
     * of making new instances on every need
     */
    private HessianDecoder mHessianDecoder = new HessianDecoder();

    /**
     * Container class for raw undecoded response data. Holds a request id
     * (obtained from outgoing request or 0 for unsolicited Push message) and
     * whether data is GZip compressed or unsolicited.
     */
    public static class RawResponse {
        public int mReqId;

        public byte[] mData;

        public boolean mIsCompressed = false;

        public boolean mIsPushMessage = false;
        
        public long mTimeStamp = 0;

        public RawResponse(int reqId, byte[] data, boolean isCompressed, boolean isPushMessage) {
            mReqId = reqId;
            mData = data;
            mIsCompressed = isCompressed;
            mIsPushMessage = isPushMessage;
            mTimeStamp = System.currentTimeMillis();
        }
    }

    /**
     * Start decoder thread
     */
    protected void startThread() {
        mRunning = true;
        Thread decoderThread = new Thread(this);
        decoderThread.setName(THREAD_NAME);
        decoderThread.start();
    }

    /**
     * Stop decoder thread
     */
    protected synchronized void stopThread() {
        this.mRunning = false;
        this.notify();
    }

    public DecoderThread() {
        mRespQueue = ResponseQueue.getInstance();
    }

    /**
     * Add raw response to decoding queue
     * 
     * @param resp raw data
     */
    public void addToDecode(RawResponse resp) {
        synchronized (this) {
            mResponses.add(resp);
            this.notify();
        }
    }

    public synchronized boolean getIsRunning() {
        return mRunning;
    }

    /**
     * Thread's run function If the decoding queue contains any entries we
     * decode the first response and add the decoded data to the response queue.
     * If the decode queue is empty, the thread will become inactive. It is
     * resumed when a raw data entry is added to the decode queue.
     */
    public void run() {
        LogUtils.logI("DecoderThread.run() [Start thread]");
        while (mRunning) {
            EngineId engineId = EngineId.UNDEFINED;
            Type type = Type.PUSH_MSG;
            int reqId = -1;
            try {
                if (mResponses.size() > 0) {
                    LogUtils.logI("DecoderThread.run() Decoding [" + mResponses.size()
                            + "x] responses");

                    // Decode first entry in queue
                    RawResponse decode = mResponses.get(0);
                    reqId = decode.mReqId;

                    if (!decode.mIsPushMessage) {
                        // Attempt to get type from request
                        Request request = QueueManager.getInstance().getRequest(reqId);
                        if (request != null) {
                            type = request.mType;
                            engineId = request.mEngineId;
                            
                            long backendResponseTime = decode.mTimeStamp - request.getAuthTimestamp();
                            
                            LogUtils.logD("Backend response time was " + backendResponseTime + "ms");
                        } else {
                            type = Type.COMMON;
                        }
                    }
                    
                    DecodedResponse response = mHessianDecoder.decodeHessianByteArray(reqId, decode.mData, type, decode.mIsCompressed, engineId);

                    // if we have a push message let's try to find out to which engine it should be routed
                    if ((response.getResponseType() == DecodedResponse.ResponseType.PUSH_MESSAGE.ordinal()) && (response.mDataTypes.get(0) != null)) {
                    	// for push messages we have to override the engine id as it is parsed inside the hessian decoder 
                    	engineId = ((PushEvent) response.mDataTypes.get(0)).mEngineId;
                        response.mSource = engineId;
                        // TODO mSource should get the engineId inside the decoder once types for mDataTypes is out. see PAND-1805.
                    }

                    // This is usually the case for SYSTEM_NOTIFICATION messages
                    // or where the server is returning an error for requests
                    // sent by the engines. IN this case, if there is no special
                    // handling for the engine, we get the engine ID based on
                    // the request ID.
                    if (type == Type.PUSH_MSG && reqId != 0 && engineId == EngineId.UNDEFINED) {
                        Request request = QueueManager.getInstance().getRequest(reqId);
                        if (request != null) {
                            engineId = request.mEngineId;
                        }
                    }

                    if (engineId == EngineId.UNDEFINED) {
                        LogUtils.logE("DecoderThread.run() Unknown engine for message with type["
                                + type.name() + "]");
                        // TODO: Throw Exception for undefined messages, as
                        // otherwise they might always remain on the Queue?
                    }

                    // Add data to response queue
                    HttpConnectionThread.logV("DecoderThread.run()", "Add message[" + decode.mReqId
                            + "] to ResponseQueue for engine[" + engineId + "] with data [" + response.mDataTypes
                            + "]");
                    mRespQueue.addToResponseQueue(response);

                    // Remove item from our list of responses.
                    mResponses.remove(0);

                    // be nice to the other threads
                    Thread.sleep(THREAD_SLEEP_TIME);
                } else {
                    synchronized (this) {
                        // No waiting responses, so the thread should sleep.
                        try {
                            LogUtils.logV("DecoderThread.run() [Waiting for more responses]");
                            wait();
                        } catch (InterruptedException ie) {
                            // Do nothing
                        }
                    }
                }
            } catch (Throwable t) {
                /*
                 * Keep thread running regardless of error. When something goes
                 * wrong we should remove response from queue and report error
                 * back to engine.
                 */
                if (mResponses.size() > 0) {
                    mResponses.remove(0);
                }
                if (type != Type.PUSH_MSG && engineId != EngineId.UNDEFINED) {
                    List<BaseDataType> list = new ArrayList<BaseDataType>();
                    // this error type was chosen to make engines remove request
                    // or retry
                    // we may consider using other error code later
                    ServerError error = new ServerError(ServerError.ErrorType.INTERNALERROR);
                    error.errorDescription = "Decoder thread was unable to decode server message";
                    list.add(error);
                    mRespQueue.addToResponseQueue(new DecodedResponse(reqId, list, engineId, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
                }
                LogUtils.logE("DecoderThread.run() Throwable on reqId[" + reqId + "]", t);
            }
        }
        LogUtils.logI("DecoderThread.run() [End thread]");
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
    public void handleResponse(byte[] response) throws Exception {
        InputStream is = null;
        if (response != null) {
            try {
                is = new ByteArrayInputStream(response);
                final List<RpgMessage> mRpgMessages = new ArrayList<RpgMessage>();
                // Get array of RPG messages
                // throws IO Exception, we pass it to the calling method
                RpgHelper.splitRpgResponse(is, mRpgMessages);
                byte[] body = null;
                RpgHeader rpgHeader = null;
                // Process each header
                for (RpgMessage mRpgMessage : mRpgMessages) {
                    body = mRpgMessage.body();
                    rpgHeader = mRpgMessage.header();
                    // Determine RPG mssageType (internal response, push
                    // etc)
                    final int mMessageType = rpgHeader.reqType();
                    HttpConnectionThread.logD("DecoderThread.handleResponse()",
                            "Non-RPG_POLL_MESSAGE");
                    // Reset blank header counter
                    final boolean mZipped = mRpgMessage.header().compression();
                    if (body != null && (body.length > 0)) {
                        switch (mMessageType) {
                            case RpgMessageTypes.RPG_EXT_RESP:
                                // External message response
                                HttpConnectionThread
                                        .logD(
                                                "DecoderThread.handleResponse()",
                                                "RpgMessageTypes.RPG_EXT_RESP - "
                                                        + "Add External Message RawResponse to Decode queue:"
                                                        + rpgHeader.reqId() + "mBody.len="
                                                        + body.length);
                                addToDecode(new RawResponse(rpgHeader.reqId(), body, mZipped, false));
                                break;
                            case RpgMessageTypes.RPG_PUSH_MSG:
                                // Define push message callback to
                                // notify controller
                                HttpConnectionThread.logD("DecoderThread.handleResponse()",
                                        "RpgMessageTypes.RPG_PUSH_MSG - Add Push "
                                                + "Message RawResponse to Decode queue:" + 0
                                                + "mBody.len=" + body.length);
                                addToDecode(new RawResponse(rpgHeader.reqId(), body, mZipped, true));
                                break;
                            case RpgMessageTypes.RPG_INT_RESP:
                                // Internal message response
                                HttpConnectionThread.logD("DecoderThread.handleResponse()",
                                        "RpgMessageTypes.RPG_INT_RESP - Add RawResponse to Decode queue:"
                                                + rpgHeader.reqId() + "mBody.len=" + body.length);
                                addToDecode(new RawResponse(rpgHeader.reqId(), body, mZipped, false));
                                break;
                            case RpgMessageTypes.RPG_PRESENCE_RESPONSE:
                                HttpConnectionThread.logD("DecoderThread.handleResponse()",
                                        "RpgMessageTypes.RPG_PRESENCE_RESPONSE - "
                                                + "Add RawResponse to Decode queue - mZipped["
                                                + mZipped + "]" + "mBody.len=" + body.length);
                                addToDecode(new RawResponse(rpgHeader.reqId(), body, mZipped, false));
                                break;
                            default:
                                // FIXME after the refactoring we need to add an
                                // error to the responsedecoder
                                break;
                        }
                    }
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                        HttpConnectionThread.logE("DecoderThread.handleResponse()",
                                "Could not close IS: ", ioe);
                    } finally {
                        is = null;
                    }
                }
            }
        }
    }
}
