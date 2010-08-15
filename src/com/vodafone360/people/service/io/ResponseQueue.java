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
import java.util.Iterator;
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
    private final List<DecodedResponse> mResponses = new ArrayList<DecodedResponse>();

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
    public static class DecodedResponse {
    	public static enum ResponseType {
    		/** An unknown response in case anything went wrong. */
    		UNKNOWN,
    		/** A response for a request that timed out. */
    		SERVER_ERROR,
    		/** A response that timed out before it arrived from the server. */
    		TIMED_OUT_RESPONSE,
    		
    		/** Represents a push message that was pushed by the server */
    		PUSH_MESSAGE,
    		
    		/** The response type for available identities. */
    		GET_AVAILABLE_IDENTITIES_RESPONSE,
    		/** The response type for my identities. */
    		GET_MY_IDENTITIES_RESPONSE,
    		/** The response type for set identity capability */
    		SET_IDENTITY_CAPABILITY_RESPONSE,
    		/** The response type for validate identity credentials */
    		VALIDATE_IDENTITY_CREDENTIALS_RESPONSE,
    		/** The response type for get activities calls. */
    		GET_ACTIVITY_RESPONSE,
    		/** The response type for get session by credentials calls. */
    		LOGIN_RESPONSE,
    		/** The response type for bulkupdate contacts calls. */
    		BULKUPDATE_CONTACTS_RESPONSE,
    		/** The response type for get contacts changes calls. */
    		GET_CONTACTCHANGES_RESPONSE,
    		/** The response type for get get me calls. */
    		GETME_RESPONSE,
    		/** The response type for get contact group relation calls. */
    		GET_CONTACT_GROUP_RELATIONS_RESPONSE, 
    		/** The response type for get groups calls. */
    		GET_GROUPS_RESPONSE, 
    		/** The response type for add contact calls. */
    		ADD_CONTACT_RESPONSE, 
    		/** The response type for signup user crypted calls. */
    		SIGNUP_RESPONSE, 
    		/** The response type for get public key calls. */
    		RETRIEVE_PUBLIC_KEY_RESPONSE, 
    		/** The response type for delete contacts calls. */
    		DELETE_CONTACT_RESPONSE, 
    		/** The response type for delete contact details calls. */
    		DELETE_CONTACT_DETAIL_RESPONSE, 
    		/** The response type for get presence calls. */
    		GET_PRESENCE_RESPONSE, 
    		/** The response type for create conversation calls. */
    		CREATE_CONVERSATION_RESPONSE,
    		/** The response type for get t&cs. */
    		GET_T_AND_C_RESPONSE,
    		/** The response type for get privacy statement. */
    		GET_PRIVACY_STATEMENT_RESPONSE,
    		/** The response type for removing the identity. */
    		DELETE_IDENTITY_RESPONSE;
    		
    		// TODO add more types here and remove them from the BaseDataType. Having them in ONE location is better than in dozens!!!
    	}
    	
    	/** The type of the response (e.g. GET_AVAILABLE_IDENTITIES_RESPONSE). */
    	private int mResponseType;
    	/** The request ID the response came in for. */
        public Integer mReqId;
        /** The response items (e.g. identities of a getAvailableIdentities call) to store for the response. */
        public List<BaseDataType> mDataTypes = new ArrayList<BaseDataType>();
        /** The ID of the engine the response should be worked off in. */
        public EngineId mSource;

        /**
         * Constructs a response object with request ID, the data and the engine
         * ID the response belongs to.
         * 
         * @param reqId The corresponding request ID for the response.
         * @param data The data of the response.
         * @param source The originating engine ID.
         * @param responseType The response type. Values can be found in Response.ResponseType.
         * 
         */
        public DecodedResponse(Integer reqId, List<BaseDataType> data, EngineId source, final int responseType) {
            mReqId = reqId;
            mDataTypes = data;
            mSource = source;
            mResponseType = responseType;
        }
        
        /**
         * The response type for this response. The types are defined in Response.ResponseType.
         * 
         * @return The response type of this response (e.g. GET_AVAILABLE_IDENTITIES_RESPONSE).
         */
        public int getResponseType() {
        	return mResponseType;
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
    public void addToResponseQueue(final DecodedResponse response) {
        synchronized (QueueManager.getInstance().lock) {
            ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.UNKNOWN_DATA_TYPE, response.mDataTypes);
            if (status == ServiceStatus.ERROR_INVALID_SESSION) {
                EngineManager em = EngineManager.getInstance();
                if (em != null) {
                    LogUtils.logE("Logging out the current user because of invalide session");
                    em.getLoginEngine().logoutAndRemoveUser();
                    return;
                }
            }
            synchronized (mResponses) {
            	mResponses.add(response);
            }

            Request request = RequestQueue.getInstance().removeRequest(response.mReqId);
            if (request != null) {
                // we suppose the response being handled by the same engine 
                // that issued the request with the given id
                response.mSource = request.mEngineId;
            }
            
            mEngMgr = EngineManager.getInstance();
            if (mEngMgr != null) {
                mEngMgr.onCommsInMessage(response.mSource);
            }
        }
    }


   /**
     * Adds a response item to the queue. Same as addToResponseQueue
     * This is used by JUnit and does not use Engine Manager
     * 
     * @param response Contains request ID to add the response for.
     *                          The response data to add to the queue.
     *                          The corresponding engine that fired off the request for the
     *                          response.
     */
    public void addToResponseQueueFromTest(final DecodedResponse response)
    {

    	synchronized (QueueManager.getInstance().lock) {
            ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.UNKNOWN_DATA_TYPE, response.mDataTypes);
            if (status == ServiceStatus.ERROR_INVALID_SESSION) {
                    return;
                }
            
            mResponses.add(response);

            Request request = RequestQueue.getInstance().removeRequest(response.mReqId);
            if (request != null) {
                // we suppose the response being handled by the same engine 
                // that issued the request with the given id
                response.mSource = request.mEngineId;
            }
        }
    }

    /**
     * Retrieves the next response from the response list if there is one and it is equal to the 
     * passed engine ID.
     * 
     * @param source The originating engine id that requested this response.
     * @return Response The first response that matches the given engine or null
     *         if no response was found.
     */
    public DecodedResponse getNextResponse(EngineId source) {

        DecodedResponse resp = null;
        
        synchronized (mResponses) {
	        Iterator<DecodedResponse> iterator = mResponses.iterator();
	        
	        while (iterator.hasNext()) {
	            resp = iterator.next();
	            
	            if ((null != resp) && (resp.mSource == source)) {
	            	// remove response if the source engine is equal to the response's engine
	            	iterator.remove();
	            	
	                if (source != null) {
	                    LogUtils.logV("ResponseQueue.getNextResponse() Returning a response to engine["
	                            + source.name() + "]");
	                }
	                
	                return resp;
	            } else if ((null == resp) || (null == resp.mSource)) {
	            	LogUtils.logE("Either the response or its source was null. Response: " + resp);
	            }
	        }
        }
        return null;
    }

    /**
     * Test if we have response for the specified request id.
     * 
     * @param reqId Request ID.
     * @return true If we have a response for this ID.
     */
    protected synchronized boolean responseExists(int reqId) {
        boolean exists = false;
        
        synchronized (mResponses) {
        	int responseCount = mResponses.size();
        	
	        for (int i = 0; i < responseCount; i++) {
	            if (mResponses.get(i).mReqId != null && mResponses.get(i).mReqId.intValue() == reqId) {
	                exists = true;
	                break;
	            }
	        }
        }
        return exists;
    }
}
