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

package com.vodafone360.people.engine.identities;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.os.Bundle;

import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.IdentityCapability;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.service.io.api.Identities;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.tcp.ITcpConnectionListener;
import com.vodafone360.people.utils.LogUtils;

/**
 * Engine responsible for handling retrieval and validation of Identities (e.g.
 * 3rd party/web accounts). The Identities engine can fetch a list of available
 * accounts, and set required capabilities and validate credentials for
 * specified accounts.
 */
public class IdentityEngine extends BaseEngine implements ITcpConnectionListener {

    /**
     * States for IdentitiesEngine. States are based on the requests that the
     * engine needs to handle.
     */
    private enum State {
        IDLE,
        FETCHING_IDENTITIES,
        VALIDATING_IDENTITY_CREDENTIALS,
        SETTING_IDENTITY_STATUS,
        GETTING_MY_IDENTITIES,
        GETTING_MY_CHATABLE_IDENTITIES
    }

    /**
     * Mutex for thread synchronisation
     */
    private final Object mMutex = new Object();

    /**
     * Container class for Identity Capability Status request. Consists of a
     * network, identity id and a filter containing the required capabilities.
     */
    private static class IdentityStatusRequest {
        private String mNetwork;

        private String mIdentityId;

        private String mIdentityStatus = null;

        /**
         * Supply filter containing required capabilities.
         * 
         * @param filter Bundle containing capabilities filter.
         */
        public void setIdentityStatus(String status) {
            mIdentityStatus = status;
        }
    }

    /**
     * Container class encapsulating an Identity validation request containing:
     * dry-run flag, network, user-name, password, set of required capabilities.
     */
    private static class IdentityValidateCredentialsRequest {
    	/** Performs a dry run if true. */
        private boolean mDryRun;
        /** Network to sign into. */
        private String mNetwork;
        /** Username to sign into identity with. */
        private String mUserName;
        /** Password to sign into identity with. */
        private String mPassword;

        private Map<String, Boolean> mStatus = null;

        /**
         * Supply filter containing required capabilities.
         * 
         * @param filter Bundle containing capabilities filter.
         */
        public void setCapabilityStatus(Bundle filter) {
            mStatus = prepareBoolFilter(filter);
        }
    }

    /** The minimum interval between identity requests. */
    private static final long MIN_REQUEST_INTERVAL = 60 * 60 * 1000;
    /** The timestamp of which my identities were last requested. */
    private long mLastMyIdentitiesRequestTimestamp;
    /** The timestamp of which available identities were last requested. */
    private long mLastAvailableIdentitiesRequestTimestamp;
    
    /** The state of the state machine handling ui requests. */
    private State mState = State.IDLE;

    /** List array of Identities retrieved from Server. */
    private ArrayList<Identity> mAvailableIdentityList;
    
    /** List array of Identities retrieved from Server. */
    private ArrayList<Identity> mMyIdentityList;

    /** List array of Identities supporting chat retrieved from Server. */
    private ArrayList<String> mMyChatableIdentityList = null;

    /** Holds the status messages of the setIdentityCapability-request. */
    private final ArrayList<StatusMsg> mStatusList = new ArrayList<StatusMsg>();

    /** The key for setIdentityCapability data type for the push ui message. */
    public static final String KEY_DATA = "data";
    /** The key for available identities for the push ui message. */
	public static final String KEY_AVAILABLE_IDS = "availableids";
	/** The key for my identities for the push ui message. */
	public static final String KEY_MY_IDS = "myids";

    /**
     * Constructor
     * 
     * @param eventCallback IEngineEventCallback allowing engine to report back.
     */
    public IdentityEngine(IEngineEventCallback eventCallback) {
        super(eventCallback);
        mEngineId = EngineId.IDENTITIES_ENGINE;
        
        mMyIdentityList = new ArrayList<Identity>();
        mAvailableIdentityList = new ArrayList<Identity>();
        
        mLastMyIdentitiesRequestTimestamp = 0;
        mLastAvailableIdentitiesRequestTimestamp = 0;
    }
    
    /**
     * 
     * Gets all third party identities and adds the mobile identity
     * from 360 to them.
     * 
     * @return A list of all 3rd party identities the user is signed in to plus 
     * the 360 identity mobile. If the retrieval failed the list will
     * be empty.
     * 
     */
    public ArrayList<Identity> getAvailableThirdPartyIdentities() {
    	if ((mAvailableIdentityList.size() == 0) && (
    			(System.currentTimeMillis() - mLastAvailableIdentitiesRequestTimestamp)
    				> MIN_REQUEST_INTERVAL)) {
    		sendGetAvailableIdentitiesRequest();
    	}
    	    	
    	return mAvailableIdentityList;
    }
    
    /**
     * 
     * Gets all third party identities the user is currently signed up for. 
     * 
     * @return A list of 3rd party identities the user is signed in to or an 
     * empty list if something  went wrong retrieving the identities. 
     * 
     */
    public ArrayList<Identity> getMyThirdPartyIdentities() {
    	if ((mMyIdentityList.size() == 0) && (
    			(System.currentTimeMillis() - mLastMyIdentitiesRequestTimestamp)
    				> MIN_REQUEST_INTERVAL)) {
    		sendGetMyIdentitiesRequest();
    	}
    	    	
    	return mMyIdentityList;
	}
    
    /**
     * 
     * Takes all third party identities that have a chat capability set to true.
     * It also includes the 360 identity mobile.
     * 
     * @return A list of chattable 3rd party identities the user is signed in to
     * plus the mobile 360 identity. If the retrieval identities failed the 
     * returned list will be empty.
     * 
     */
    public ArrayList<Identity> getMy360AndThirdPartyChattableIdentities() {
    	ArrayList<Identity> chatableIdentities = new ArrayList<Identity>();
    	int identityListSize = mMyIdentityList.size(); 
    	
    	// checking each identity for its chat capability and adding it to the
    	// list if it does
    	for (int i = 0; i < identityListSize; i++) {
    		Identity identity = mMyIdentityList.get(i);
    		List<IdentityCapability> capabilities = identity.mCapabilities;
    		
    		if (null == capabilities) {
    			continue;	// if the capabilties are null skip to next identity
    		}
    		
    		// run through capabilties and check for chat
    		for (int j = 0; j < capabilities.size(); j++) {
    			IdentityCapability capability = capabilities.get(j);
    			
    			if (null == capability) {
    				continue;	// skip null capabilities
    			}
    			
    			if ((capability.mCapability == IdentityCapability.CapabilityID.chat) &&
    					(capability.mValue == true)) {
    				chatableIdentities.add(identity);
    				break;
    			}
    		}
    	}
    	
    	// add mobile identity to support 360 chat
    	IdentityCapability capability = new IdentityCapability();
    	capability.mCapability = IdentityCapability.CapabilityID.chat;
    	capability.mValue = new Boolean(true);
    	ArrayList<IdentityCapability> mobileCapabilities = 
    								new ArrayList<IdentityCapability>();
    	mobileCapabilities.add(capability);
    	
    	Identity mobileIdentity = new Identity();
    	mobileIdentity.mNetwork = SocialNetwork.MOBILE.toString();
    	mobileIdentity.mName = "Vodafone";
    	mobileIdentity.mCapabilities = mobileCapabilities;
    	chatableIdentities.add(mobileIdentity);
    	// end: add mobile identity to support 360 chat
    	
    	return chatableIdentities;
    }
    
    
    /**
     * Sends a get my identities request to the server which will be handled
     * by onProcessCommsResponse once a response comes in.
     */
    private void sendGetMyIdentitiesRequest() {
    	Identities.getMyIdentities(this, prepareStringFilter(getIdentitiesFilter()));
    }
    
    /**
     * Send a get available identities request to the backend which will be 
     * handled by onProcessCommsResponse once a response comes in.
     */
    private void sendGetAvailableIdentitiesRequest() {
    	Identities.getAvailableIdentities(this, prepareStringFilter(getIdentitiesFilter()));
    }

    /**
     * Enables or disables the given social network.
     * 
     * @param network Name of the identity,
     * @param identityId Id of identity.
     * @param identityStatus True if identity should be enabled, false otherwise.
     */
    public void addUiSetIdentityStatus(String network, String identityId, boolean identityStatus) {
        LogUtils.logD("IdentityEngine.addUiSetIdentityCapabilityStatus()");
        IdentityStatusRequest data = new IdentityStatusRequest();
        data.mIdentityId = identityId;
        data.mNetwork = network;
        data.setIdentityStatus(identityStatus ? Identities.ENABLE_IDENTITY
                : Identities.DISABLE_IDENTITY);
        // do not empty reqQueue here, ui can put many at one time
        addUiRequestToQueue(ServiceUiRequest.SET_IDENTITY_CAPABILITY_STATUS, data);
    }

    /**
     * Add request to validate user credentials for a specified identity.
     * 
     * @param dryRun True if this is a dry-run.
     * @param network Name of the network/identity.
     * @param username User-name for login for this identity.
     * @param password Password for login for this identity.
     * @param identityCapabilityStatus Bundle containing capability details for
     *            this identity.
     */
    public void addUiValidateIdentityCredentials(boolean dryRun, String network, String username,
            String password, Bundle identityCapabilityStatus) {
        LogUtils.logD("IdentityEngine.addUiValidateIdentityCredentials()");
        IdentityValidateCredentialsRequest data = new IdentityValidateCredentialsRequest();
        data.mDryRun = dryRun;
        data.mNetwork = network;
        data.mPassword = password;
        data.mUserName = username;
        data.setCapabilityStatus(identityCapabilityStatus);
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.VALIDATE_IDENTITY_CREDENTIALS, data);
    }

    /**
     * Issue any outstanding UI request.
     * 
     * @param requestType Request to be issued.
     * @param dara Data associated with the request.
     */
    @Override
    protected void processUiRequest(ServiceUiRequest requestType, Object data) {
        LogUtils.logD("IdentityEngine.processUiRequest() - reqID = " + requestType);
        switch (requestType) {
            case VALIDATE_IDENTITY_CREDENTIALS:
                executeValidateIdentityCredentialsRequest(data);
                break;
            case SET_IDENTITY_CAPABILITY_STATUS:
                executeSetIdentityStatusRequest(data);
                break;
            default:
                completeUiRequest(ServiceStatus.ERROR_NOT_FOUND, null);
                break;
        }
    }

    /**
     * Issue request to set capabilities for a given Identity. (Request is not
     * issued if there is currently no connectivity).
     * 
     * @param data Bundled request data.
     */
    private void executeSetIdentityStatusRequest(Object data) {
        if (!isConnected()) {
            return;
        }
        newState(State.SETTING_IDENTITY_STATUS);
        IdentityStatusRequest reqData = (IdentityStatusRequest)data;
        if (!setReqId(Identities.setIdentityStatus(this, reqData.mNetwork, reqData.mIdentityId,
                reqData.mIdentityStatus))) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER);
        }
        // invalidate 'chat-able' identities cache
        if (mMyChatableIdentityList != null) {
            mMyChatableIdentityList.clear();
        }

    }

    /**
     * Issue request to validate the user credentials for an Identity. (Request
     * is not issued if there is currently no connectivity).
     * 
     * @param data Bundled request data.
     */
    private void executeValidateIdentityCredentialsRequest(Object data) {
        if (!isConnected()) {
            return;
        }
        newState(State.VALIDATING_IDENTITY_CREDENTIALS);
        IdentityValidateCredentialsRequest reqData = (IdentityValidateCredentialsRequest)data;
        if (!setReqId(Identities
                .validateIdentityCredentials(this, reqData.mDryRun, reqData.mNetwork,
                        reqData.mUserName, reqData.mPassword, null, null, reqData.mStatus))) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER);
        }
    }
    
    /**
     * Process a response received from Server. The response is handled
     * according to the current IdentityEngine state.
     * 
     * @param resp The decoded response.
     */
    @Override
    protected void processCommsResponse(Response resp) {
        LogUtils.logD("IdentityEngine.processCommsResponse() - resp = " + resp);
        
        if (resp.mReqId == 0 && resp.mDataTypes.size() > 0) {	// push msg
            PushEvent evt = (PushEvent)resp.mDataTypes.get(0);
            handlePushResponse(evt.mMessageType);
        } else if (resp.mDataTypes.size() > 0) {				// regular response
	        switch (resp.mDataTypes.get(0).getType()) {
	        	case BaseDataType.MY_IDENTITY_DATA_TYPE:
	        		handleGetMyIdentitiesResponse(resp.mDataTypes);
	        		break;
	        	case BaseDataType.AVAILABLE_IDENTITY_DATA_TYPE:
	        		handleGetAvailableIdentitiesResponse(resp.mDataTypes);
	        		break;
	        	case BaseDataType.IDENTITY_CAPABILITY_DATA_TYPE:
	        		handleSetIdentityStatus(resp.mDataTypes);
	        		break;
	        	case BaseDataType.STATUS_MSG_DATA_TYPE:
	        		handleValidateIdentityCredentials(resp.mDataTypes);
	        		break;
	            default:
	                LogUtils.logW("IdentityEngine.processCommsResponse DEFAULT should never happened.");
	                break;
	        }
        } else {
        	LogUtils.logW("IdentityEngine.processCommsResponse List was empty!");
        }
    }

    /**
     * Handle Status or Timeline Activity change Push message
     * 
     * @param evt Push message type (Status change or Timeline change).
     */
    private void handlePushResponse(PushMessageTypes evt) {
        LogUtils.logD("IdentityEngine handlePushRequest");
        switch (evt) {
            case IDENTITY_NETWORK_CHANGE:
                sendGetAvailableIdentitiesRequest();
                break;
            case IDENTITY_CHANGE:
            	EngineManager.getInstance().getPresenceEngine().setMyAvailability();
                sendGetMyIdentitiesRequest();
                mEventCallback.kickWorkerThread();
            	break;
            default:
                // do nothing
        }
    }

    /**
     * Run function called via EngineManager. Should have a UI, Comms response
     * or timeout event to handle.
     */
    @Override
    public void run() {
        LogUtils.logD("IdentityEngine.run()");
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            LogUtils.logD("IdentityEngine.ResponseOutstanding and processCommsInQueue. mState = "
                    + mState.name());
            return;
        }
        if (processTimeout()) {
            return;
        }
        if (isUiRequestOutstanding()) {
            processUiQueue();
        }
    }

    /**
     * Change current IdentityEngine state.
     * 
     * @param newState new state.
     */
    private void newState(State newState) {
        State oldState = mState;
        synchronized (mMutex) {
            if (newState == mState) {
                return;
            }
            mState = newState;
        }
        LogUtils.logV("IdentityEngine.newState: " + oldState + " -> " + mState);
    }


    /**
     * Handle Server response to request for available Identities. The response
     * should be a list of Identity items. The request is completed with
     * ServiceStatus.SUCCESS or ERROR_UNEXPECTED_RESPONSE if the data-type
     * retrieved are not Identity items.
     * 
     * @param data List of BaseDataTypes generated from Server response.
     */
    private void handleGetAvailableIdentitiesResponse(List<BaseDataType> data) {    	
        LogUtils.logD("IdentityEngine: handleServerGetAvailableIdentitiesResponse");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.AVAILABLE_IDENTITY_DATA_TYPE, data);
        
        if (errorStatus == ServiceStatus.SUCCESS) {
        	synchronized (mAvailableIdentityList) {
	            mAvailableIdentityList.clear();
	            
	            for (BaseDataType item : data) {
	            	mAvailableIdentityList.add((Identity)item);
	            }
        	}
        }
        
        pushIdentitiesToUi(ServiceUiRequest.GET_AVAILABLE_IDENTITIES);
        
        LogUtils.logD("IdentityEngine: handleGetAvailableIdentitiesResponse complete request.");
    }
    
    /**
     * Handle Server response to request for available Identities. The response
     * should be a list of Identity items. The request is completed with
     * ServiceStatus.SUCCESS or ERROR_UNEXPECTED_RESPONSE if the data-type
     * retrieved are not Identity items.
     * 
     * @param data List of BaseDataTypes generated from Server response.
     */
    private void handleGetMyIdentitiesResponse(List<BaseDataType> data) {    	
        LogUtils.logD("IdentityEngine: handleGetMyIdentitiesResponse");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.MY_IDENTITY_DATA_TYPE, data);
        
        if (errorStatus == ServiceStatus.SUCCESS) {        	
        	synchronized (mMyIdentityList) {
	            mMyIdentityList.clear();
	            
	            for (BaseDataType item : data) {
	            	mMyIdentityList.add((Identity)item);
	            }
        	}
        }
        
        pushIdentitiesToUi(ServiceUiRequest.GET_MY_IDENTITIES);
        
        LogUtils.logD("IdentityEngine: handleGetMyIdentitiesResponse complete request.");
    }

    /**
     * Handle Server response to set validate credentials request. The response
     * should be a Status-msg indicating whether the request has succeeded or
     * failed. The request is completed with the status result (or
     * ERROR_UNEXPECTED_RESPONSE if the data-type retrieved is not a
     * Status-msg).
     * 
     * @param data List of BaseDataTypes generated from Server response.
     */
    private void handleValidateIdentityCredentials(List<BaseDataType> data) {
        Bundle bu = null;
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.STATUS_MSG_DATA_TYPE, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            mStatusList.clear();
            for (BaseDataType item : data) {
                if (item.getType() == BaseDataType.STATUS_MSG_DATA_TYPE) {
                    mStatusList.add((StatusMsg)item);
                } else {
                    completeUiRequest(ServiceStatus.ERROR_UNEXPECTED_RESPONSE);
                    return;
                }
            }
            bu = new Bundle();
            if (mStatusList.size() == 1) {
                bu.putBoolean("status", mStatusList.get(0).mStatus);
            } else {
                LogUtils.logW("Status list sould have one item. It has " + mStatusList.size());
                bu.putParcelableArrayList(KEY_DATA, mStatusList);
            }
        }
        completeUiRequest(errorStatus, bu);

        newState(State.IDLE);

    }

    /**
     * Handle Server response to set capability status request. The response
     * should be a Status-msg indicating whether the request has succeeded or
     * failed. The request is completed with the status result (or
     * ERROR_UNEXPECTED_RESPONSE if the data-type retrieved is not a
     * Status-msg).
     * 
     * @param data List of BaseDataTypes generated from Server response.
     */
    private void handleSetIdentityStatus(List<BaseDataType> data) {
        Bundle bu = null;
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.STATUS_MSG_DATA_TYPE, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            mStatusList.clear();
            for (BaseDataType item : data) {
                if (item.getType() == BaseDataType.STATUS_MSG_DATA_TYPE) {
                    mStatusList.add((StatusMsg)item);
                } else {
                    completeUiRequest(ServiceStatus.ERROR_UNEXPECTED_RESPONSE);
                    return;
                }
            }
            bu = new Bundle();
            bu.putParcelableArrayList(KEY_DATA, mStatusList);
        }
        completeUiRequest(errorStatus, bu);
        newState(State.IDLE);
    }

    /**
     * 
     * Pushes the identities retrieved by get my identities or by get available identities
     * to the ui.
     * 
     * @param request The request type: either get my identities, or get available identities.
     */
    private void pushIdentitiesToUi(ServiceUiRequest request) {
    	String requestKey = null;
    	ArrayList<Identity> idBundle = null;
    	if (request == ServiceUiRequest.GET_AVAILABLE_IDENTITIES) {
    		requestKey = KEY_AVAILABLE_IDS;
    		idBundle = mAvailableIdentityList;
    	} else {
    		requestKey = KEY_MY_IDS;
    		idBundle = mMyIdentityList;
    	}
    	
        // send update to 3rd party identities ui if it is up
        Bundle b = new Bundle();
        b.putParcelableArrayList(requestKey, idBundle);
        
        UiAgent uiAgent = mEventCallback.getUiAgent();
        if (uiAgent != null && uiAgent.isSubscribed()) {
            uiAgent.sendUnsolicitedUiEvent(request, b);
        } // end: send update to 3rd party identities ui if it is up
    }
    
    /**
     * Get Connectivity status from the connection manager.
     * 
     * @return true True if the connection is active, false otherwise. 
     * 
     */
    private boolean isConnected() {
    	int connState = ConnectionManager.getInstance().getConnectionState();
        return (connState == ITcpConnectionListener.STATE_CONNECTED);
    }
    
    /**
     * 
     * Retrieves the filter for the getAvailableIdentities and getMyIdentities
     * calls.
     * 
     * @return The identities filter in form of a bundle.
     * 
     */
    private Bundle getIdentitiesFilter() {
    	Bundle b = new Bundle();
        ArrayList<String> l = new ArrayList<String>();
        l.add(IdentityCapability.CapabilityID.chat.name());
        l.add(IdentityCapability.CapabilityID.get_own_status.name());
        b.putStringArrayList("capability", l);
        return b;
    }

	@Override
	public void onConnectionStateChanged(int state) {
		if (state == ITcpConnectionListener.STATE_CONNECTED) {
	        emptyUiRequestQueue();
	        sendGetAvailableIdentitiesRequest();
	        sendGetMyIdentitiesRequest();
		}
	}
	
    /**
     * Return the next run-time for the IdentitiesEngine. Will run as soon as
     * possible if we need to issue a request, or we have a resonse waiting.
     * 
     * @return next run-time.
     */
    @Override
    public long getNextRunTime() {
        if (isUiRequestOutstanding()) {
            return 0;
        }
        if (isCommsResponseOutstanding()) {
            return 0;
        }
        return getCurrentTimeout();
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate() {
    }

    /** {@inheritDoc} */
    @Override
    public void onDestroy() {
    }

    /** {@inheritDoc} */
    @Override
    protected void onRequestComplete() {
    }

    /** {@inheritDoc} */
    @Override
    protected void onTimeoutEvent() {
    }

    /**
     * Generate Map containing boolean capability filters for supplied Bundle.
     * 
     * @param filter Bundle containing filter.
     * @return Map containing set of capabilities.
     */
    private static Map<String, Boolean> prepareBoolFilter(Bundle filter) {
        Map<String, Boolean> objectFilter = null;
        if (filter != null && (filter.keySet().size() > 0)) {
            objectFilter = new Hashtable<String, Boolean>();
            for (String key : filter.keySet()) {
                objectFilter.put(key, filter.getBoolean(key));
            }
        } else {
            objectFilter = null;
        }
        return objectFilter;
    }

    /**
     * Generate Map containing String capability filters for m supplied Bundle.
     * 
     * @param filter Bundle containing filter.
     * @return Map containing set of capabilities.
     */
    private static Map<String, List<String>> prepareStringFilter(Bundle filter) {
        Map<String, List<String>> returnFilter = null;
        if (filter != null && filter.keySet().size() > 0) {
            returnFilter = new Hashtable<String, List<String>>();
            for (String key : filter.keySet()) {
                returnFilter.put(key, filter.getStringArrayList(key));
            }
        } else {
            returnFilter = null;
        }
        return returnFilter;
    }
    
    /**
     * This method needs to be called as part of removeAllData()/changeUser()
     * routine.
     */
    public final void onReset() {
        clearCachedIdentities();
        super.onReset();
    }

    /**
     * 
     * Clears all cached identities that belong to a user (my identities). Available identities will stay untouched as they do not raise any privacy
     * concerns and can stay cached.
     * 
     */
    public void clearCachedIdentities() {
        if (null != mMyIdentityList) {
            mMyIdentityList.clear();
        }
    }
}
