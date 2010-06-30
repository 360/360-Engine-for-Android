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

import android.test.InstrumentationTestCase;

import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.presence.PresenceEngine;

public class PresenceEngineTest extends InstrumentationTestCase implements  IEngineTestFrameworkObserver{

	/**
	 * States for test harness
	 */
	enum PresenceTestState {
		IDLE,
		GET_PRESENCE,
		GET_PRESENCE_FAIL,
		SET_AVAILBAILITY,
		GET_NEXT_RUNTIME
	}
	
	private EngineTestFramework mEngineTester = null;
	private PresenceEngine mEng = null;
	//private MainApplication mApplication = null;
	//private PresenceTestState mState = PresenceTestState.IDLE;
	
	@Override
    protected void setUp() throws Exception {
        super.setUp();
        mEngineTester = new EngineTestFramework(this);
        mEng = new PresenceEngine(mEngineTester, null);
        //mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class, getInstrumentation().getTargetContext());
        mEngineTester.setEngine(mEng);
        //mState = PresenceTestState.IDLE;
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
    
//    @MediumTest
//    public void testFetchIdentities(){
//    	
//    	mState = IdentityTestState.FETCH_IDENTITIES;
//    	Bundle fbund = new Bundle();
//    	
//    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
//    	mEng.addUiFetchIdentities(fbund);
//    	//mEng.run();
//    	ServiceStatus status = mEngineTester.waitForEvent();
//    	if(status != ServiceStatus.SUCCESS){
//    		throw(new RuntimeException("Expected SUCCESS"));
//    	}
//    	
//    	Object data = mEngineTester.data();
//    	assertTrue(data!=null);
//    	try{
//    		ArrayList<Identity> identityList = ((Bundle) data).getParcelableArrayList("data");
//    		assertTrue(identityList.size()==1);
//    	}
//    	catch(Exception e){
//    		throw(new RuntimeException("Expected identity list with 1 item"));
//    	}
//    	
//    }
    
    
    
//    @MediumTest
//    public void testFetchIdentitiesFail(){
//    	mState = IdentityTestState.FETCH_IDENTITIES_FAIL;
//    	Bundle fbund = new Bundle();
//    	
//    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
//    	mEng.addUiFetchIdentities(fbund);
//    	//mEng.run();
//    	ServiceStatus status = mEngineTester.waitForEvent();
//    	if(status == ServiceStatus.SUCCESS){
//    		throw(new RuntimeException("Expected FAILURE"));
//    	}
//    	
//    	Object data = mEngineTester.data();
//    	assertTrue(data==null);
//    }
//  
//    
//    @MediumTest
//    public void testSetIdentityCapability(){
//    	mState = IdentityTestState.SET_IDENTITY_CAPABILTY;
//
//    	String network = "facebook";
//    	Bundle fbund = new Bundle();
//    	fbund.putBoolean("sync_contacts", true);
//    	String identityId = "mikeyb";
//    	
//    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
//		mEng.addUiSetIdentityCapabilityStatus(network, identityId, fbund);
//		ServiceStatus status = mEngineTester.waitForEvent();
//		if(status != ServiceStatus.SUCCESS){
//    		throw(new RuntimeException("Expected SUCCESS"));
//    	}
//		
//		Object data = mEngineTester.data();
//    	assertTrue(data!=null);
//    	try{
//    		ArrayList<StatusMsg> identityList = ((Bundle) data).getParcelableArrayList("data");
//    		assertTrue(identityList.size()==1);
//    	}
//    	catch(Exception e){
//    		throw(new RuntimeException("Expected identity list with 1 item"));
//    	}
//    }
// 
//    
//    @MediumTest
//    public void testValidateIDCredentialsSuccess(){
//    	mState = IdentityTestState.VALIDATE_ID_CREDENTIALS_SUCCESS;
//    	
//    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
//    	mEng.addUiValidateIdentityCredentials(false, "bob", "password", "", new Bundle());
//		ServiceStatus status = mEngineTester.waitForEvent();
//		if(status != ServiceStatus.SUCCESS){
//    		throw(new RuntimeException("Expected SUCCESS"));
//    	}
//		
//		Object data = mEngineTester.data();
//    	assertTrue(data!=null);
//    }
//    
//   
//    @MediumTest
//    public void testValidateIDCredentialsFail(){
//    	mState = IdentityTestState.VALIDATE_ID_CREDENTIALS_FAIL;
//
//    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
//    	mEng.addUiValidateIdentityCredentials(false, "bob", "password", "", new Bundle());
//
//		ServiceStatus status = mEngineTester.waitForEvent();
//		if(status == ServiceStatus.SUCCESS){
//    		throw(new RuntimeException("Expected SUCCESS"));
//    	}
//		
//		Object data = mEngineTester.data();
//    	assertTrue(data==null);
//    }
//
//
//    @MediumTest
//    public void testGetNextRuntime(){
//    	mState = IdentityTestState.GET_NEXT_RUNTIME;
//    	long runtime = mEng.getNextRunTime();
//    }
//    
//	@Override
//	public void reportBackToEngine(int reqId, EngineId engine) {
//		Log.d("TAG","IdentityEngineTest.reportBackToEngine");
//    	ResponseQueue respQueue = ResponseQueue.getInstance();
//		List<BaseDataType> data = new ArrayList<BaseDataType>();
//		
//		switch(mState){
//		case IDLE:
//			break;
//		case FETCH_IDENTITIES:
//			Log.d("TAG","IdentityEngineTest.reportBackToEngine FETCH ids");
//			Identity id = new Identity();
//			data.add(id);
//			respQueue.addToResponseQueue(reqId, data, engine);
//			Log.d("TAG","IdentityEngineTest.reportBackToEngine add to Q");
//			mEng.onCommsInMessage();
//			break;
//		case FETCH_IDENTITIES_FAIL:
//			ServerError err = new ServerError();
//			err.errorType = "Catastrophe";
//			err.errorValue = "Fail";
//			data.add(err);
//			respQueue.addToResponseQueue(reqId, data, engine);
//			mEng.onCommsInMessage();
//			break;
//		case SET_IDENTITY_CAPABILTY:
//			StatusMsg msg = new StatusMsg();
//			msg.mCode = "ok";
//			msg.mDryRun = false;
//			msg.mStatus = true;
//			data.add(msg);
//			respQueue.addToResponseQueue(reqId, data, engine);
//			mEng.onCommsInMessage();
//			break;
//		case VALIDATE_ID_CREDENTIALS_SUCCESS:
//			StatusMsg msg2 = new StatusMsg();
//			msg2.mCode = "ok";
//			msg2.mDryRun = false;
//			msg2.mStatus = true;
//			data.add(msg2);
//			respQueue.addToResponseQueue(reqId, data, engine);
//			mEng.onCommsInMessage();
//			break;
//		case VALIDATE_ID_CREDENTIALS_FAIL:
//			ServerError err2 = new ServerError();
//			err2.errorType = "Catastrophe";
//			err2.errorValue = "Fail";
//			data.add(err2);
//			respQueue.addToResponseQueue(reqId, data, engine);
//			mEng.onCommsInMessage();
//			break;
//		case GET_NEXT_RUNTIME:
//			break;
//		default:
//		}
//		
//	}


	@Override
	public void reportBackToEngine(int reqId, EngineId engine) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEngineException(Exception exp) {
		// TODO Auto-generated method stub
		
	}
}
