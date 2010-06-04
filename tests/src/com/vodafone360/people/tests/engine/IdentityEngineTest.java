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

package com.vodafone360.people.tests.engine;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.IdentityCapability;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.identities.IdentityEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;

public class IdentityEngineTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver {

    /**
     * States for test harness
     */
    enum IdentityTestState {
        IDLE, FETCH_IDENTITIES, GET_MY_IDENTITIES, FETCH_IDENTITIES_FAIL, FETCH_IDENTITIES_POPULATED, GET_CHATABLE_IDENTITIES, SET_IDENTITY_CAPABILTY, VALIDATE_ID_CREDENTIALS_SUCCESS, VALIDATE_ID_CREDENTIALS_FAIL, GET_NEXT_RUNTIME
    }

    EngineTestFramework mEngineTester = null;

    IdentityEngine mEng = null;

    IdentityTestState mState = IdentityTestState.IDLE;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mEngineTester = new EngineTestFramework(this);
        mEng = new IdentityEngine(mEngineTester);
        mEngineTester.setEngine(mEng);
        mState = IdentityTestState.IDLE;
    }

    @Override
    protected void tearDown() throws Exception {

        // stop our dummy thread?
        mEngineTester.stopEventThread();
        mEngineTester = null;
        mEng = null;

        // call at the end!!!
        super.tearDown();
    }

    @Suppress // Takes too long
    @MediumTest
    public void testFetchIdentities() {

        mState = IdentityTestState.FETCH_IDENTITIES;
        Bundle fbund = new Bundle();

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addUiFetchIdentities(fbund);
        // mEng.run();
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals(ServiceStatus.SUCCESS, status);

        Object data = mEngineTester.data();
        assertTrue(data != null);
        try {
            ArrayList<Identity> identityList = ((Bundle)data).getParcelableArrayList("data");
            assertTrue(identityList.size() == 1);
        } catch (Exception e) {
            throw (new RuntimeException("Expected identity list with 1 item"));
        }

    }

    @MediumTest
    @Suppress // Takes too long.
    public void testAddUiGetMyIdentities() {
        mState = IdentityTestState.GET_MY_IDENTITIES;
        Bundle getBund = new Bundle();

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addUiGetMyIdentities(getBund);
        // mEng.run();
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals(ServiceStatus.SUCCESS, status);

        Object data = mEngineTester.data();
        assertNull(null);
        try {
            ArrayList<Identity> identityList = ((Bundle)data).getParcelableArrayList("data");
            assertEquals(identityList.size(), 1);
        } catch (Exception e) {
            throw (new RuntimeException("Expected identity list with 1 item"));
        }
    }

    @MediumTest
    @Suppress // Takes to long
    public void testFetchIdentitiesFail() {
        mState = IdentityTestState.FETCH_IDENTITIES_FAIL;
        Bundle fbund = new Bundle();

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addUiFetchIdentities(fbund);
        // mEng.run();
        ServiceStatus status = mEngineTester.waitForEvent();
        assertFalse(ServiceStatus.SUCCESS == status);

        Object data = mEngineTester.data();
        assertNull(data);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testFetchIdentitiesPopulated() {
        mState = IdentityTestState.FETCH_IDENTITIES_POPULATED;
        Bundle fbund = new Bundle();

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addUiFetchIdentities(fbund);
        // mEng.run();
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals(ServiceStatus.SUCCESS, status);
        Object data = mEngineTester.data();
        assertTrue(data != null);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testSetIdentityCapability() {
        mState = IdentityTestState.SET_IDENTITY_CAPABILTY;

        String network = "facebook";
        // Bundle fbund = new Bundle();
        // fbund.putBoolean("sync_contacts", true);
        String identityId = "mikeyb";

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        // AA mEng.addUiSetIdentityCapabilityStatus(network, identityId, fbund);
        mEng.addUiSetIdentityStatus(network, identityId, true);
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals(ServiceStatus.SUCCESS, status);

        Object data = mEngineTester.data();
        assertTrue(data != null);
        try {
            ArrayList<StatusMsg> identityList = ((Bundle)data).getParcelableArrayList("data");
            assertTrue(identityList.size() == 1);
        } catch (Exception e) {
            throw (new RuntimeException("Expected identity list with 1 item"));
        }
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testValidateIDCredentialsSuccess() {
        mState = IdentityTestState.VALIDATE_ID_CREDENTIALS_SUCCESS;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addUiValidateIdentityCredentials(false, "bob", "password", "", new Bundle());
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals(ServiceStatus.SUCCESS, status);
        Object data = mEngineTester.data();
        assertTrue(data != null);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testGetMyChatableIdentities() {
        mState = IdentityTestState.GET_CHATABLE_IDENTITIES;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.getMyChatableIdentities();
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals(ServiceStatus.SUCCESS, status);

        Object data = mEngineTester.data();
        assertTrue(data != null);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testValidateIDCredentialsFail() {
        mState = IdentityTestState.VALIDATE_ID_CREDENTIALS_FAIL;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addUiValidateIdentityCredentials(false, "bob", "password", "", new Bundle());

        ServiceStatus status = mEngineTester.waitForEvent();

        assertFalse(ServiceStatus.SUCCESS == status);
        Object data = mEngineTester.data();
        assertTrue(data == null);
    }

    @MediumTest
    public void testGetNextRuntime() {
        mState = IdentityTestState.GET_NEXT_RUNTIME;
        // long runtime = mEng.getNextRunTime();
    }

    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        Log.d("TAG", "IdentityEngineTest.reportBackToEngine");
        ResponseQueue respQueue = ResponseQueue.getInstance();
        List<BaseDataType> data = new ArrayList<BaseDataType>();

        switch (mState) {
            case IDLE:
                break;
            case FETCH_IDENTITIES:
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine FETCH ids");
                Identity id = new Identity();
                data.add(id);
                respQueue.addToResponseQueue(reqId, data, engine);
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine add to Q");
                mEng.onCommsInMessage();
                break;
            case GET_MY_IDENTITIES:
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine Get ids");
                Identity myId = new Identity();
                data.add(myId);
                respQueue.addToResponseQueue(reqId, data, engine);
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine add to Q");
                mEng.onCommsInMessage();
                break;
            case FETCH_IDENTITIES_FAIL:
                ServerError err = new ServerError();
                err.errorType = "Catastrophe";
                err.errorValue = "Fail";
                data.add(err);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case SET_IDENTITY_CAPABILTY:
                StatusMsg msg = new StatusMsg();
                msg.mCode = "ok";
                msg.mDryRun = false;
                msg.mStatus = true;
                data.add(msg);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case VALIDATE_ID_CREDENTIALS_SUCCESS:
                StatusMsg msg2 = new StatusMsg();
                msg2.mCode = "ok";
                msg2.mDryRun = false;
                msg2.mStatus = true;
                data.add(msg2);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case VALIDATE_ID_CREDENTIALS_FAIL:
                ServerError err2 = new ServerError();
                err2.errorType = "Catastrophe";
                err2.errorValue = "Fail";
                data.add(err2);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_NEXT_RUNTIME:
                break;
            case GET_CHATABLE_IDENTITIES:
            case FETCH_IDENTITIES_POPULATED:
                Identity id2 = new Identity();
                id2.mActive = true;
                id2.mAuthType = "auth";
                List<String> clist = new ArrayList<String>();
                clist.add("uk");
                clist.add("fr");
                id2.mCountryList = clist;
                id2.mCreated = new Long(0);
                id2.mDisplayName = "Facebook";
                // id2.mIcon2Mime = "jpeg";
                id2.mIconMime = "jpeg";
                try {
                    id2.mIcon2Url = new URL("url2");
                    id2.mIconUrl = new URL("url");
                    id2.mNetworkUrl = new URL("network");
                } catch (Exception e) {

                }
                id2.mIdentityId = "fb";
                id2.mIdentityType = "type";
                id2.mName = "Facebook";
                id2.mNetwork = "Facebook";
                id2.mOrder = 0;
                id2.mPluginId = "";
                id2.mUpdated = new Long(0);
                id2.mUserId = 23;
                id2.mUserName = "user";

                data.add(id2);

                List<IdentityCapability> capList = new ArrayList<IdentityCapability>();
                IdentityCapability idcap = new IdentityCapability();
                idcap.mCapability = IdentityCapability.CapabilityID.sync_contacts;
                idcap.mDescription = "sync cont";
                idcap.mName = "sync cont";
                idcap.mValue = true;
                capList.add(idcap);

                id2.mCapabilities = capList;
                data.add(id2);

                respQueue.addToResponseQueue(reqId, data, engine);
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine add to Q");
                mEng.onCommsInMessage();
                break;
            default:
        }

    }

    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub

    }

}
