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

package com.vodafone360.people.service.io;

import java.util.ArrayList;
import java.util.List;

import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;

/**
 * Queue of responses received from server. These may be either responses to
 * requests issued by the People client or unsolicited ('Push') messages. The
 * content of these Responses will have already been decoded by the
 * DecoderThread and converted to data-types understood by the People Client.
 */
public class ResponseQueue {

    /**
     * The list of responses held by this queue. An array-list of Responses.
     */
    private final List<Response> mResponses = new ArrayList<Response>();

    /**
     * The engine manager holding the various engines to cross check where
     * responses belong to.
     */
    private EngineManager mEngMgr = null;

    /**
     * Class encapsulating a decoded response from the server A Response
     * contains; a request id which should match a request id from a request
     * issued by the People Client (responses to non-RPG requests or
     * non-solicited messages will not have a request id), the request data, a
     * list of decoded BaseDataTypes generated from the response content and an
     * engine id informing the framework which engine the response should be
     * routed to.
     */
    public static class Response {
        public Integer mReqId;

        public List<BaseDataType> mDataTypes = new ArrayList<BaseDataType>();

        public EngineId mSource;

        /**
         * Constructs a response object with request ID, the data and the engine
         * ID the response belongs to.
         * 
         * @param reqId The corresponding request ID for the response.
         * @param data The data of the response.
         * @param source The originating engine ID.
         */
        public Response(Integer reqId, List<BaseDataType> data, EngineId source) {
            mReqId = reqId;
            mDataTypes = data;
            mSource = source;
        }
    }

    /**
     * Protected constructor to highlight the singleton nature of this class.
     */
    protected ResponseQueue() {
    }

    /**
     * Gets an instance of the ResponseQueue as part of the singleton pattern.
     * 
     * @return The instance of ResponseQueue.
     */
    public static ResponseQueue getInstance() {
        return ResponseQueueHolder.rQueue;
    }

    /**
     * Use Initialization on demand holder pattern
     */
    private static class ResponseQueueHolder {
        private static final ResponseQueue rQueue = new ResponseQueue();
    }

    /**
     * Adds a response item to the queue.
     * 
     * @param reqId The request ID to add the response for.
     * @param data The response data to add to the queue.
     * @param source The corresponding engine that fired off the request for the
     *            response.
     */
    public void addToResponseQueue(Integer reqId, List<BaseDataType> data, EngineId source) {
        synchronized (QueueManager.getInstance().lock) {
            ServiceStatus status = BaseEngine.genericHandleResponseType("", data);
            if (status == ServiceStatus.ERROR_INVALID_SESSION) {
                EngineManager em = EngineManager.getInstance();
                if (em != null) {
                    LogUtils.logE("Logging out the current user because of invalide session");
                    em.getLoginEngine().logoutAndRemoveUser();
                    return;
                }
            }
            mResponses.add(new Response(reqId, data, source));
            final RequestQueue rQ = RequestQueue.getInstance();
            rQ.removeRequest(reqId);
            mEngMgr = EngineManager.getInstance();
            if (mEngMgr != null) {
                mEngMgr.onCommsInMessage(source);
            }
        }
    }

    /**
     * Retrieves the next response in the list if there is one.
     * 
     * @param source The originating engine id that requested this response.
     * @return Response The first response that matches the given engine or null
     *         if no response was found.
     */
    public Response getNextResponse(EngineId source) {

        Response resp = null;
        for (int i = 0; i < mResponses.size(); i++) {
            resp = mResponses.get(i);
            if (resp.mSource == source) {
                mResponses.remove(i);

                if (source != null) {
                    LogUtils.logV("ResponseQueue.getNextResponse() Returning a response to engine["
                            + source.name() + "]");
                }
                return resp;
            }
        }
        return null;
    }

    /**
     * Get number of items currently in the response queue.
     * 
     * @return number of items currently in the response queue.
     */
    private int responseCount() {
        return mResponses.size();
    }

    /**
     * Test if we have response for the specified request id.
     * 
     * @param reqId Request ID.
     * @return true If we have a response for this ID.
     */
    protected synchronized boolean responseExists(int reqId) {
        boolean exists = false;
        for (int i = 0; i < responseCount(); i++) {
            if (mResponses.get(i).mReqId != null && mResponses.get(i).mReqId.intValue() == reqId) {
                exists = true;
                break;
            }
        }
        return exists;
    }
}
