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

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.MyIdentitiesCacheTable;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.IdentityCapability;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.IEngineEventCallback;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.api.Identities;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.tcp.ITcpConnectionListener;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.ThirdPartyAccount;

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
        GETTING_MY_CHATABLE_IDENTITIES,
        DELETE_IDENTITY
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
    
    /**
     * 
     * Container class for Delete Identity request. Consist network and identity id.
     *
     */
    private static class DeleteIdentityRequest {
    	/** Network to delete.*/
    	private String mNetwork;
    	/** IdentityID which needs to be Deleted.*/
    	private String mIdentityId;
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
    private final ArrayList<Identity> mAvailableIdentityList;
    
    /** List array of Identities retrieved from Server. */
    private final ArrayList<Identity> mMyIdentityList;

    /** Holds the status messages of the setIdentityCapability-request. */
    private final ArrayList<StatusMsg> mStatusList = new ArrayList<StatusMsg>();

    /** The key for setIdentityCapability data type for the push ui message. */
    public static final String KEY_DATA = "data";
    /** The key for available identities for the push ui message. */
	public static final String KEY_AVAILABLE_IDS = "availableids";
	/** The key for my identities for the push ui message. */
	public static final String KEY_MY_IDS = "myids";
	/**
	 * Maintaining DeleteIdentityRequest so that it can be later removed from
	 * maintained cache.
	 **/
	private DeleteIdentityRequest identityToBeDeleted;
	
    /**
     * The hard coded list of capabilities we use to getAvailableIdentities(): chat and status.
     */
    private final Map<String, List<String>> mGetAvailableIdentitiesFilter;
    
    /**
     * The hard coded list of capabilities we use to getMyIdentities(): chat and status.
     */
    private final Map<String, List<String>> mGetMyIdentitiesFilter;
    
    /**
     * The DatabaseHelper used to access the client database.
     */
    private final DatabaseHelper mDatabaseHelper;

    /**
     * Constructor
     * 
     * @param eventCallback IEngineEventCallback allowing engine to report back.
     */
    public IdentityEngine(IEngineEventCallback eventCallback, DatabaseHelper databaseHelper) {
        super(eventCallback);
        mEngineId = EngineId.IDENTITIES_ENGINE;
        mDatabaseHelper = databaseHelper;
        
        mMyIdentityList = new ArrayList<Identity>();
        // restore cached identities
        MyIdentitiesCacheTable.getCachedIdentities(databaseHelper.getReadableDatabase(),
                                                   mMyIdentityList);
        mAvailableIdentityList = new ArrayList<Identity>();
                
        mLastMyIdentitiesRequestTimestamp = 0;
        mLastAvailableIdentitiesRequestTimestamp = 0;
        
        // initialize identity capabilities filter
        mGetAvailableIdentitiesFilter = new Hashtable<String, List<String>>();
        final List<String> capabilities = new ArrayList<String>();
        capabilities.add(IdentityCapability.CapabilityID.chat.name());
        capabilities.add(IdentityCapability.CapabilityID.get_own_status.name());
        
        mGetAvailableIdentitiesFilter.put(Identity.CAPABILITY, capabilities);
        
        final List<String> authType = new ArrayList<String>();
        authType.add(Identity.AUTH_TYPE_URL);
        authType.add(Identity.AUTH_TYPE_CREDENTIALS);
                
        mGetAvailableIdentitiesFilter.put(Identity.AUTH_TYPE, authType);
        
        mGetMyIdentitiesFilter = new Hashtable<String, List<String>>();
        mGetMyIdentitiesFilter.put(Identity.CAPABILITY, capabilities);
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
        
        final ArrayList<Identity> availableIdentityList;
        
        synchronized(mAvailableIdentityList) {
            // make a shallow copy
            availableIdentityList = new ArrayList<Identity>(mAvailableIdentityList);
        }
        
    	if ((availableIdentityList.size() == 0) && (
    			(System.currentTimeMillis() - mLastAvailableIdentitiesRequestTimestamp)
    				> MIN_REQUEST_INTERVAL)) {
    		sendGetAvailableIdentitiesRequest();
    	}
    	    	
    	return availableIdentityList;
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
        
        final ArrayList<Identity> myIdentityList;
        
        synchronized(mMyIdentityList) {
            // make a shallow copy
            myIdentityList = new ArrayList<Identity>(mMyIdentityList);
        }
        
        if ((myIdentityList.size() == 0) && (
            (System.currentTimeMillis() - mLastMyIdentitiesRequestTimestamp)
    				> MIN_REQUEST_INTERVAL)) {
    		sendGetMyIdentitiesRequest();
    	}
        
    	return myIdentityList;
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
    	ArrayList<Identity> chattableIdentities = getMyThirdPartyChattableIdentities();
    	
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
    	chattableIdentities.add(mobileIdentity);
    	// end: add mobile identity to support 360 chat
    	
    	return chattableIdentities;
    }
    
    /**
     * 
     * Takes all third party identities that have a chat capability set to true.
     * 
     * @return A list of chattable 3rd party identities the user is signed in to. If the retrieval identities failed the returned list will be empty.
     * 
     */
    public ArrayList<Identity> getMyThirdPartyChattableIdentities() {
    	final ArrayList<Identity> chattableIdentities = new ArrayList<Identity>();
    	final ArrayList<Identity> myIdentityList;
    	final int identityListSize;
        
        synchronized(mMyIdentityList) {
            // make a shallow copy
            myIdentityList = new ArrayList<Identity>(mMyIdentityList);
        }
    	
    	identityListSize = myIdentityList.size(); 
    	
    	// checking each identity for its chat capability and adding it to the
    	// list if it does
    	for (int i = 0; i < identityListSize; i++) {
    		Identity identity = myIdentityList.get(i);
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
    					(capability.mValue)) {
    				chattableIdentities.add(identity);
    				break;
    			}
    		}
    	}
    	
    	return chattableIdentities;
    }
    
    
    /**
     * Sends a get my identities request to the server which will be handled
     * by onProcessCommsResponse once a response comes in.
     */
    private void sendGetMyIdentitiesRequest() {
        Identities.getMyIdentities(this, mGetMyIdentitiesFilter);
    }
    
    /**
     * Send a get available identities request to the backend which will be 
     * handled by onProcessCommsResponse once a response comes in.
     */
    private void sendGetAvailableIdentitiesRequest() {
    	Identities.getAvailableIdentities(this, mGetAvailableIdentitiesFilter);
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
     * TODO: re-factor the method in the way that the UI doesn't pass the Bundle with capabilities
     * list to the Engine, but the Engine makes the list itself (UI/Engine separation).
     *  
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
     * Delete the given social network.
     *
     * @param network Name of the identity,
     * @param identityId Id of identity.
     */
    public final void addUiDeleteIdentityRequest(final String network, final String identityId) {
    	LogUtils.logD("IdentityEngine.addUiRemoveIdentity()");
    	DeleteIdentityRequest data = new DeleteIdentityRequest();

    	data.mNetwork = network;
    	data.mIdentityId = identityId;

    	/**maintaining the sent object*/
    	setIdentityToBeDeleted(data);

    	addUiRequestToQueue(ServiceUiRequest.DELETE_IDENTITY, data);
    }

	/**
	 * Setting the DeleteIdentityRequest object.
	 *
	 * @param data
	 */
	private void setIdentityToBeDeleted(final DeleteIdentityRequest data) {
		identityToBeDeleted = data;
	}

	/**
	 * Return the DeleteIdentityRequest object.
	 *
	 */
	private DeleteIdentityRequest getIdentityToBeDeleted() {
		return identityToBeDeleted;
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
            case GET_MY_IDENTITIES:
                sendGetMyIdentitiesRequest();
                completeUiRequest(ServiceStatus.SUCCESS);
                break;
            case VALIDATE_IDENTITY_CREDENTIALS:
                executeValidateIdentityCredentialsRequest(data);
                break;
            case SET_IDENTITY_CAPABILITY_STATUS:
                executeSetIdentityStatusRequest(data);
                break;
            case DELETE_IDENTITY:
            	executeDeleteIdentityRequest(data);
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
     * Issue request to delete the identity as specified . (Request is not issued if there
     * is currently no connectivity).
     *
     * @param data bundled request data containing network and identityId.
     */

	private void executeDeleteIdentityRequest(final Object data) {
		if (!isConnected()) {
			completeUiRequest(ServiceStatus.ERROR_NO_INTERNET);
		}
		newState(State.DELETE_IDENTITY);
		DeleteIdentityRequest reqData = (DeleteIdentityRequest) data;

		if (!setReqId(Identities.deleteIdentity(this, reqData.mNetwork,
				reqData.mIdentityId))) {
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
    protected void processCommsResponse(DecodedResponse resp) {  	    	
    	LogUtils.logD("IdentityEngine.processCommsResponse() - resp = " + resp);
    	
    	if ((null == resp) || (null == resp.mDataTypes)) {
    		LogUtils.logE("Response objects or its contents were null. Aborting...");
    		return;
    	}
        
        // TODO replace this whole block with the response type in the DecodedResponse class in the future!
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
	        	case BaseDataType.IDENTITY_DELETION_DATA_TYPE:
	        		handleDeleteIdentity(resp.mDataTypes);
	        		break;
	            default:
	                LogUtils.logW("IdentityEngine.processCommsResponse DEFAULT should never happened.");
	                break;
	        }
        } else {		// responses data list is 0, that means e.g. no identities in an identities response
        	LogUtils.logW("IdentityEngine.processCommsResponse List was empty!");
        	
        	if (resp.getResponseType() == DecodedResponse.ResponseType.GET_MY_IDENTITIES_RESPONSE.ordinal()) {
        		pushIdentitiesToUi(ServiceUiRequest.GET_MY_IDENTITIES);
        	} else if (resp.getResponseType() == DecodedResponse.ResponseType.GET_AVAILABLE_IDENTITIES_RESPONSE.ordinal()) {
        		pushIdentitiesToUi(ServiceUiRequest.GET_AVAILABLE_IDENTITIES);
        	}        	
        } // end: replace this whole block with the response type in the DecodedResponse class in the future!
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
            	mMyIdentityList.clear();
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
	                Identity identity = (Identity)item;
	            	mMyIdentityList.add(identity);
	            }
	            // cache the identities
	            MyIdentitiesCacheTable.setCachedIdentities(mDatabaseHelper.getWritableDatabase(),
	                                                       mMyIdentityList);
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
        
        if (errorStatus == ServiceStatus.SUCCESS) {
            addUiRequestToQueue(ServiceUiRequest.GET_MY_IDENTITIES, null);
        }

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
	 * Handle Server response of request to delete the identity. The response
	 * should be a status that whether the operation is succeeded or not. The
	 * response will be a status result otherwise ERROR_UNEXPECTED_RESPONSE if
	 * the response is not as expected.
	 *
	 * @param data
	 *            List of BaseDataTypes generated from Server response.
	 */
	private void handleDeleteIdentity(final List<BaseDataType> data) {
		Bundle bu = null;
		ServiceStatus errorStatus = getResponseStatus(
				BaseDataType.IDENTITY_DELETION_DATA_TYPE, data);
		if (errorStatus == ServiceStatus.SUCCESS) {
			for (BaseDataType item : data) {
				if (item.getType() == BaseDataType.IDENTITY_DELETION_DATA_TYPE) {
					
				    synchronized(mMyIdentityList) {
				        // iterating through the subscribed identities
    					for (Identity identity : mMyIdentityList) {
    						if (identity.mIdentityId
    								.equals(getIdentityToBeDeleted().mIdentityId)) {
    							mMyIdentityList.remove(identity);
    							break;
    						}
    					}
    					// cache the new set of identities
    					MyIdentitiesCacheTable.setCachedIdentities(mDatabaseHelper.getWritableDatabase(),
    					                                           mMyIdentityList);
				    }

					completeUiRequest(ServiceStatus.SUCCESS);
					return;
				} else {
					completeUiRequest(ServiceStatus.ERROR_UNEXPECTED_RESPONSE);
					return;
				}
			}
		}
		completeUiRequest(errorStatus, bu);
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
    		synchronized (mAvailableIdentityList) {
    		    // provide a shallow copy
    		    idBundle = new ArrayList<Identity>(mAvailableIdentityList);
    		}
    	} else {
    		requestKey = KEY_MY_IDS;
    		synchronized (mMyIdentityList) {
    		    // provide a shallow copy
    		    idBundle = new ArrayList<Identity>(mMyIdentityList);
    		}
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
     * This method needs to be called as part of removeAllData()/changeUser()
     * routine.
     */
    /** {@inheritDoc} */
    @Override
    public final void onReset() {
        
        super.onReset();
        mMyIdentityList.clear();
        mAvailableIdentityList.clear();
        mStatusList.clear();
        mState = State.IDLE;
    }
    
    /***
     * Return TRUE if the given ThirdPartyAccount contains a Facebook account.
     * 
     * @param list List of Identity objects, can be NULL.
     * @return TRUE if the given Identity contains a Facebook account.
     */
    public boolean isFacebookInThirdPartyAccountList() {
        if (mMyIdentityList != null) {
            synchronized(mMyIdentityList) {
                for (Identity identity : mMyIdentityList) {
                    if (identity.mName.toLowerCase().contains(ThirdPartyAccount.SNS_TYPE_FACEBOOK)) {
                        return true;
                    }
                }
            }
        }
        LogUtils.logV("ApplicationCache."
                + "isFacebookInThirdPartyAccountList() Facebook not found in list");
        return false;
    }

    /***
     * Return TRUE if the given Identity contains a Hyves account.
     * 
     * @param list List of ThirdPartyAccount objects, can be NULL.
     * @return TRUE if the given Identity contains a Hyves account.
     */
    public boolean isHyvesInThirdPartyAccountList() {
        if (mMyIdentityList != null) {
            synchronized(mMyIdentityList) {
                for (Identity identity : mMyIdentityList) {
                    if (identity.mName.toLowerCase().contains(ThirdPartyAccount.SNS_TYPE_HYVES)) {
                        return true;
                    }
                }
            }
        }
        LogUtils.logV("ApplicationCache."
                + "isFacebookInThirdPartyAccountList() Hyves not found in list");
        return false;
    }
}
