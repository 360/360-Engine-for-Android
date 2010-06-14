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
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.datatypes.IdentityCapability.CapabilityID;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.service.io.api.Identities;
import com.vodafone360.people.utils.LogUtils;

/**
 * Engine responsible for handling retrieval and validation of Identities (e.g.
 * 3rd party/web accounts). The Identities engine can fetch a list of available
 * accounts, and set required capabilities and validate credentials for
 * specified accounts.
 */
public class IdentityEngine extends BaseEngine {

    /**
     * States for IdentitiesEngine. States are based on the requests that the
     * engine needs to handle.
     */
    private enum State {
        IDLE,
        FETCHING_IDENTITIES,
        VALIDATING_IDENTITY_CREDENTIALS,
        SETTING_IDENTITY_CAPABILITY_STATUS,
        GETTING_MY_IDENTITIES,
        GETTING_MY_CHATABLE_IDENTITIES
    }

    /**
     * Mutex for thread synchronisation
     */
    private final Object mMutex = new Object();

    // /**
    // * Container class for Identity Capability Status request.
    // * Consists of a network, identity id and a filter containing the required
    // * capabilities.
    // */
    // private class IdentityCapabilityStatusRequest {
    // private String mNetwork;
    // private String mIdentityId;
    // private Map<String, Object> mStatus = null;
    //		
    // /**
    // * Supply filter containing required capabilities.
    // * @param filter Bundle containing capabilities filter.
    // */
    // public void setCapabilityStatus(Bundle filter){
    // mStatus = prepareBoolFilter(filter);
    // }
    // }

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
        private boolean mDryRun;

        private String mNetwork;

        private String mUserName;

        private String mPassword;

        private Map<String, Object> mStatus = null;

        /**
         * Supply filter containing required capabilities.
         * 
         * @param filter Bundle containing capabilities filter.
         */
        public void setCapabilityStatus(Bundle filter) {
            mStatus = prepareBoolFilter(filter);
        }
    }

    private State mState = State.IDLE;

    /** List array of Identities retrieved from Server. */
    private final ArrayList<Identity> mIdentityList = new ArrayList<Identity>();

    /** List array of Identities supporting chat retrieved from Server. */
    private ArrayList<String> mMyChatableIdentityList = null;

    private final ArrayList<StatusMsg> mStatusList = new ArrayList<StatusMsg>();

    /** Definitions for expected data-types returned from Server. */
    private static final String TYPE_IDENTITY = "Identity";

    private static final String TYPE_STATUS_MSG = "StatusMsg";

    public static final String KEY_DATA = "data";

    private static final String KEY_DATA_CHATABLE_IDENTITIES = "chatable_data";

    private static final String LOG_STATUS_MSG = TYPE_STATUS_MSG + ": ";

    /**
     * Generate Map containing boolean capability filters for supplied Bundle.
     * 
     * @param filter Bundle containing filter.
     * @return Map containing set of capabilities.
     */
    private static Map<String, Object> prepareBoolFilter(Bundle filter) {
        Map<String, Object> objectFilter = null;
        if (filter != null && (filter.keySet().size() > 0)) {
            objectFilter = new Hashtable<String, Object>();
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
     * Constructor
     * 
     * @param eventCallback IEngineEventCallback allowing engine to report back.
     */
    public IdentityEngine(IEngineEventCallback eventCallback) {
        super(eventCallback);
        mEngineId = EngineId.IDENTITIES_ENGINE;
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
     * Process a response received from Server. The response is handled
     * according to the current IdentityEngine state.
     * 
     * @param resp The decoded response.
     */
    @Override
    protected void processCommsResponse(Response resp) {
        LogUtils.logD("IdentityEngine.processCommsResponse() - resp = " + resp);
        switch (mState) {
            case IDLE:
                LogUtils.logW("IDLE should never happend");
                break;
            case FETCHING_IDENTITIES:
            case GETTING_MY_IDENTITIES:
            case GETTING_MY_CHATABLE_IDENTITIES:
                handleServerGetAvailableIdentitiesResponse(resp.mDataTypes);
                break;
            case SETTING_IDENTITY_CAPABILITY_STATUS:
                handleSetIdentityCapabilityStatus(resp.mDataTypes);
                break;
            case VALIDATING_IDENTITY_CREDENTIALS:
                handleValidateIdentityCredentials(resp.mDataTypes);
                break;
            default:
                LogUtils.logW("default should never happend");
                break;
        }

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
            case FETCH_IDENTITIES:
                startFetchIdentities(data);
                break;
            case GET_MY_IDENTITIES:
                startGetMyIdentities(data);
                break;
            case GET_MY_CHATABLE_IDENTITIES:
                startGetMyChatableIdentities();
                break;
            case VALIDATE_IDENTITY_CREDENTIALS:
                startValidateIdentityCredentials(data);
                break;
            case SET_IDENTITY_CAPABILITY_STATUS:
                // changed the method called
                // startSetIdentityCapabilityStatus(data);
                startSetIdentityStatus(data);
                break;
            default:
                completeUiRequest(ServiceStatus.ERROR_NOT_FOUND, null);
                break;
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
     * Add request to fetch available identities. The request is added to the UI
     * request and processed when the engine is ready.
     * 
     * @param filter Bundle containing parameters for fetch identities request.
     *            This contains the set of filters applied to GetIdentities API
     *            call.
     */
    public void addUiFetchIdentities(Bundle filter) {
        LogUtils.logD("IdentityEngine.addUiFetchIdentities()");
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.FETCH_IDENTITIES, filter);
    }

    /**
     * Add request to set the capabilities we wish to support for the specified
     * identity (such as support sync of contacts, receipt of status updates,
     * chat etc.)
     * 
     * @param network Name of the identity,
     * @param identityId Id of identity.
     * @param identityStatus Bundle containing the capability information for
     *            this identity.
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
     * Add request to fetch the current user's identities.
     * 
     * @param filter Bundle containing parameters for fetch identities request.
     *            This contains the set of filters applied to GetMyIdentities
     *            API call.
     */
    public void addUiGetMyIdentities(Bundle filter) {
        LogUtils.logD("IdentityEngine.addUiGetMyIdentities()");
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.GET_MY_IDENTITIES, filter);
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
     * Issue request to retrieve 'My' Identities. (Request is not issued if
     * there is currently no connectivity).
     * 
     * @param data Bundled request data.
     */
    private void startGetMyIdentities(Object data) {
        if (!checkConnectivity()) {
            return;
        }
        newState(State.GETTING_MY_IDENTITIES);
        if (!setReqId(Identities.getMyIdentities(this, prepareStringFilter((Bundle)data)))) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER);
        }
    }

    /**
     * Issue request to retrieve 'My chat-able' Identities. The request is
     * filtered to retrieve only identities with the chat capability enabled.
     * (Request is not issued if there is currently no connectivity).
     */
    private void startGetMyChatableIdentities() {
        if (!checkConnectivity()) {
            return;
        }
        Bundle filter = new Bundle();
        ArrayList<String> l = new ArrayList<String>();
        l.add(IdentityCapability.CapabilityID.chat.name());
        filter.putStringArrayList("capability", l);

        newState(State.GETTING_MY_CHATABLE_IDENTITIES);
        if (!setReqId(Identities.getMyIdentities(this, prepareStringFilter(filter)))) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER);
        }
    }


    /**
     * Issue request to set capabilities for a given Identity. (Request is not
     * issued if there is currently no connectivity).
     * 
     * @param data Bundled request data.
     */
    private void startSetIdentityStatus(Object data) {
        if (!checkConnectivity()) {
            return;
        }
        newState(State.SETTING_IDENTITY_CAPABILITY_STATUS);
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
    private void startValidateIdentityCredentials(Object data) {
        if (!checkConnectivity()) {
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
     * Issue request to retrieve available Identities. (Request is not issued if
     * there is currently no connectivity).
     * 
     * @param data Bundled request data.
     */
    private void startFetchIdentities(Object data) {
        if (!checkConnectivity()) {
            return;
        }
        newState(State.FETCHING_IDENTITIES);
        mIdentityList.clear();
        if (!setReqId(Identities.getAvailableIdentities(this, prepareStringFilter((Bundle)data)))) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER);
        }
    }

    /**
     * Handle Server response to request for available Identities. The response
     * should be a list of Identity items. The request is completed with
     * ServiceStatus.SUCCESS or ERROR_UNEXPECTED_RESPONSE if the data-type
     * retrieved are not Identity items.
     * 
     * @param data List of BaseDataTypes generated from Server response.
     */
    private void handleServerGetAvailableIdentitiesResponse(List<BaseDataType> data) {
        Bundle bu = null;
        LogUtils.logD("IdentityEngine: handleServerGetAvailableIdentitiesResponse");
        ServiceStatus errorStatus = genericHandleResponseType(TYPE_IDENTITY, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            mIdentityList.clear();
            for (BaseDataType item : data) {
                if (TYPE_IDENTITY.equals(item.name())) {
                    mIdentityList.add((Identity)item);
                    LogUtils.logD("Identity: " + item.name());
                } else {
                    completeUiRequest(ServiceStatus.ERROR_UNEXPECTED_RESPONSE);
                    return;
                }
            }
            bu = new Bundle();
            if (mState == State.GETTING_MY_IDENTITIES
                    || (mState == State.GETTING_MY_CHATABLE_IDENTITIES)) {
                // store local copy of my identities
                makeChatableIdentitiesCache(mIdentityList);
                bu.putStringArrayList(KEY_DATA_CHATABLE_IDENTITIES, mMyChatableIdentityList);
            }
            bu.putParcelableArrayList(KEY_DATA, mIdentityList);
        }
        LogUtils.logD("IdentityEngine: handleServerGetAvailableIdentitiesResponse completw UI req");
        completeUiRequest(errorStatus, bu);
        newState(State.IDLE);
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
        ServiceStatus errorStatus = genericHandleResponseType(TYPE_STATUS_MSG, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            mStatusList.clear();
            for (BaseDataType item : data) {
                if (TYPE_STATUS_MSG.equals(item.name())) {
                    mStatusList.add((StatusMsg)item);
                    LogUtils.logD(LOG_STATUS_MSG + item.name());
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
    private void handleSetIdentityCapabilityStatus(List<BaseDataType> data) {
        Bundle bu = null;
        ServiceStatus errorStatus = genericHandleResponseType(TYPE_STATUS_MSG, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            mStatusList.clear();
            for (BaseDataType item : data) {
                if (TYPE_STATUS_MSG.equals(item.name())) {
                    mStatusList.add((StatusMsg)item);
                    LogUtils.logD(LOG_STATUS_MSG + item.name());
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
     * Get Connectivity status from NetworkAgent.
     * 
     * @return true if NetworkAgent reports we have connectivity, false
     *         otherwise (complete outstanding request with ERROR_COMMS).
     */
    private boolean checkConnectivity() {
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return false;
        }
        return true;
    }

    /**
     * Create cache of 'chat-able' identities.
     * 
     * @param list List array of retrieved Identities.
     */
    private void makeChatableIdentitiesCache(List<Identity> list) {
        if (mMyChatableIdentityList == null) {
            mMyChatableIdentityList = new ArrayList<String>();
        } else {
            mMyChatableIdentityList.clear();
        }

        for (Identity id : list) {
            if (id.mActive && id.mCapabilities != null) {
                for (IdentityCapability ic : id.mCapabilities) {
                    if (ic.mCapability == CapabilityID.chat && ic.mValue) {
                        mMyChatableIdentityList.add(id.mNetwork);
                    }
                }
            }
        }
    }

    /**
     * Add request to fetch the current user's 'chat-able' identities. This will
     * automatically apply set of filters applied to GetMyIdentities API call to
     * get Identities with chat capability.
     *
     * Note: Only called from tests.
     */
    public void getMyChatableIdentities() {
        LogUtils.logD("IdentityEngine.getMyChatableIdentities()");
        if (mMyChatableIdentityList != null) {
            Bundle bu = new Bundle();
            bu.putStringArrayList(KEY_DATA_CHATABLE_IDENTITIES, mMyChatableIdentityList);
            completeUiRequest(ServiceStatus.SUCCESS, bu);
            return;
        }

        addUiRequestToQueue(ServiceUiRequest.GET_MY_CHATABLE_IDENTITIES, null);
    }
}
