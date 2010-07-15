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

package com.vodafone360.people.service.agent;

/**
 * Stores information about the current status of the NetworkAgent, i.e. whether
 * we are connected to the Internet, whether the network is available, if Wifi
 * is active, whether roaming or data roaming are enabled etc.
 */
public class NetworkAgentState {

    private NetworkAgent.AgentState mAgentState;

    private NetworkAgent.AgentDisconnectReason mDisconnectReason = NetworkAgent.AgentDisconnectReason.UNKNOWN;

    private boolean mInternetConnected;

    private boolean mIsRoaming; // actual state

    private boolean mDataRoamingAllowed; // settings

    private boolean mIsInBackground; // actual state

    private boolean mBackgroundDataAllowed;// settings

    private boolean mWifiActive;

    private boolean mNetworkWorking;

    private boolean[] changes;

    /**
     * Get current NetworkAgent state.
     * 
     * @return current NetworkAgent state {connected | disconnected | unknown}
     */
    public NetworkAgent.AgentState getAgentState() {
        return mAgentState;
    }

    /**
     * Sets a new NetworkAgent state.
     * 
     * @param mAgentState current NetworkAgent state {connected | disconnected |
     *            unknown}
     */
    public void setAgentState(NetworkAgent.AgentState mAgentState) {
        this.mAgentState = mAgentState;
    }

    /**
     * Return whether we are connected to the Internet.
     * 
     * @return TRUE if we are connected to Internet.
     */
    public boolean isInternetConnected() {
        return mInternetConnected;
    }

    /**
     * Sets a new Internet connection state.
     * 
     * @param internetConnected TRUE if connected to Internet, FALSE otherwise.
     */
    public void setInternetConnected(boolean internetConnected) {
        this.mInternetConnected = internetConnected;
    }

    /**
     * Return whether roaming is currently enabled.
     * 
     * @return TrUE if roaming is enabled, FALSE otherwise.
     */
    public boolean isRoaming() {
        return mIsRoaming;
    }

    /**
     * Set roaming state.
     * 
     * @param dataRoaming TRUE if enabled, FALSE otherwise.
     */
    public void setRoaming(boolean dataRoaming) {
        this.mIsRoaming = dataRoaming;
    }

    /**
     * Return data roaming state.
     * 
     * @return TRUE if data roaming enabled.
     */
    public boolean isRoamingAllowed() {
        return mDataRoamingAllowed;
    }

    /**
     * Set whether roaming is allowed.
     * 
     * @param dataRoamingAllowed TRUE if roaming allowed.
     */
    public void setRoamingAllowed(boolean dataRoamingAllowed) {
        this.mDataRoamingAllowed = dataRoamingAllowed;
    }

    /**
     * Return whether application is in the background.
     * 
     * @return TRUE if application is in the background.
     */
    public boolean isInBackGround() {
        return mIsInBackground;
    }

    /**
     * Set application background state.
     * 
     * @param isBackGround TRUE if application is in the background.
     */
    public void setInBackGround(boolean isBackGround) {
        this.mIsInBackground = isBackGround;
    }

    /**
     * Return whether Wifi is active.
     * 
     * @return TRUE if Wifi is active, FALSE otherwise.
     */
    public boolean isWifiActive() {
        return mWifiActive;
    }

    /**
     * Set Wifi active state.
     * 
     * @param wifiActive TRUE if Wifi is active.
     */
    public void setWifiActive(boolean wifiActive) {
        this.mWifiActive = wifiActive;
    }

    /**
     * Return whether the network is currently available.
     * 
     * @return TRUE if network is available.
     */
    public boolean isNetworkWorking() {
        return mNetworkWorking;
    }

    /**
     * Set whether network is currently available.
     * 
     * @param networkWorking TRUE if network is available, FALSE otherwise.
     */
    public void setNetworkWorking(boolean networkWorking) {
        this.mNetworkWorking = networkWorking;
    }

    /**
     * Return reason for NetworkAgent entering disconnected state.
     * 
     * @return AgentDisconnectReason object, i.e. reason for disconnection.
     */
    public NetworkAgent.AgentDisconnectReason getDisconnectReason() {
        return mDisconnectReason;
    }

    /**
     * Set reason for setting NetworkAgent to disconnected state.
     * 
     * @param disconnectReason NetworkAgent disconnect reason.
     */
    public void setDisconnectReason(NetworkAgent.AgentDisconnectReason disconnectReason) {
        this.mDisconnectReason = disconnectReason;
    }

    /***
     * Set if background data connections are allowed.
     * 
     * @param backgroundDataAllowed TRUE if background data connections are
     *            permitted, FALSE otherwise.
     */
    public void setBackgroundDataAllowed(boolean backgroundDataAllowed) {
        this.mBackgroundDataAllowed = backgroundDataAllowed;
    }

    /***
     * Return if background data connections are allowed.
     * 
     * @return TRUE if background data connections are permitted, FALSE
     *         otherwise.
     */
    public boolean isBackDataAllowed() {
        return mBackgroundDataAllowed;
    }

    /***
     * Get which values should be overridden on the Network Agent.
     * 
     * @return Array of StatesOfService values.
     */
    public boolean[] getChanges() {
        return changes;
    }

    /***
     * Set which values should be overridden on the Network Agent.
     * 
     * @param changes Array of StatesOfService values.
     */
    public void setChanges(boolean[] changes) {
        this.changes = changes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NetworkAgentState [");
        sb.append("agentState["); sb.append(mAgentState); sb.append("] "); 
        sb.append("isInBackground["); sb.append(mIsInBackground); sb.append("] "); 
        sb.append("isRoaming["); sb.append(mIsRoaming); sb.append("] "); 
        sb.append("disconnectReason["); sb.append(mDisconnectReason); sb.append("]"); 
        sb.append("internetConnected["); sb.append(mInternetConnected); sb.append("] ");
        sb.append("networkWorking["); sb.append(mNetworkWorking); sb.append("] "); 
        sb.append("wifiActive["); sb.append(mWifiActive); sb.append("]");
        sb.append("]");
        return sb.toString();
    }
}
