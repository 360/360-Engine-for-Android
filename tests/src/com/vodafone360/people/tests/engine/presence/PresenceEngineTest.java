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

package com.vodafone360.people.tests.engine.presence;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.app.Instrumentation;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Conversation;
import com.vodafone360.people.datatypes.PresenceList;
import com.vodafone360.people.datatypes.PushChatMessageEvent;
import com.vodafone360.people.datatypes.PushClosedConversationEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.datatypes.ServerError.ErrorType;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.identities.IdentityEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.presence.PresenceEngine;
import com.vodafone360.people.engine.presence.User;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.rpg.RpgPushMessage;
import com.vodafone360.people.tests.engine.EngineTestFramework;
import com.vodafone360.people.tests.engine.IEngineTestFrameworkObserver;

public class PresenceEngineTest extends InstrumentationTestCase implements  IEngineTestFrameworkObserver{

	/**
	 * States for test harness
	 */
	enum PresenceTestState {
		IDLE,
		GET_PRESENCE,
		GET_PRESENCE_FAIL,
		SET_AVAILBAILITY,
		GET_NEXT_RUNTIME,
		SEND_MESSAGE,
		GET_PUSH_EVENT,
		GET_PUSH_EVENT_CLOSED_CONVERSATION,
		GET_SYSTEM_NOTIFICATION
		
	}
	
	private EngineTestFramework mEngineTester = null;
	private PresenceEngine mEng = null;
	private MainApplication mApplication = null;
	private PresenceTestState mState = PresenceTestState.IDLE;
	
	private EngineManager mEngineManager = null;
	private LoginEngine mLoginEngine = null; 
	private IdentityEngine mIdentityEngine = null;
	
	@Override
    protected void setUp() throws Exception {
        super.setUp();
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class, getInstrumentation().getTargetContext());
        mApplication.onCreate();
                
        mEngineTester = new EngineTestFramework(this);
        mEng = new PresenceEngine(mEngineTester, mApplication.getDatabase());
        mEng.setJunitMode(true);
        mEngineTester.setEngine(mEng);
        mState = PresenceTestState.IDLE;
        
        mEngineManager = EngineManager.createEngineManagerForTest(null , mEngineTester);
        mEngineManager.addEngineForTest(mEng);
        
        mLoginEngine = new LoginEngine(getInstrumentation().getTargetContext(), mEngineTester, mApplication.getDatabase());
        mEngineManager.addEngineForTest(mLoginEngine);
        
        final AuthSessionHolder session = new AuthSessionHolder();
        session.userID = 0;
        session.sessionSecret = new String("sssh");
        session.userName = new String("bob");
        session.sessionID = new String("session");

        mLoginEngine.setTestSession(session);

        
        mIdentityEngine = new IdentityEngine(mEngineTester, mApplication.getDatabase());
        mEngineManager.addEngineForTest(mIdentityEngine);
        
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
    @SmallTest
    public void testGetNextRunTime() {
    	
    	long runtime = mEng.getNextRunTime();
    	assertEquals(-1 ,runtime);
    	
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	
    	runtime = mEng.getNextRunTime();
    }
    
    @MediumTest
    public void testSetMyStatus() {
    	
    	mState = PresenceTestState.IDLE;
    	OnlineStatus myStatus = OnlineStatus.ONLINE;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	final long localContactId = 1234;
    	SyncMeDbUtils.setMeProfileId(localContactId);
    	
    	try {
    	mEng.setMyAvailability(myStatus);
    	//ServiceStatus status = mEngineTester.waitForEvent();
        //assertEquals(ServiceStatus.SUCCESS, status);
    	
    	Hashtable<String, String> presence = new Hashtable<String, String>();
		presence.put("facebook.com", "online");
		mEng.setMyAvailability(presence);
		
		mEng.setMyAvailability(SocialNetwork.GOOGLE ,myStatus);
    	} catch (Exception e) {
    		fail("SetMyAvailability failed");
    	}
    	
		
    }
    
    @MediumTest
    public void testGetPresenceList() {
    	mState = PresenceTestState.GET_PRESENCE;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	final long localContactId = 1234;
    	SyncMeDbUtils.setMeProfileId(localContactId);
    	mEng.getPresenceList();
    	
    	//getPresenceList() returns immediately without waiting for handling the response.
    	//The instrumentation thread is made to wait so that response is handled before teardown.
    	
    	ServiceStatus status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
    	
    }
    
    @LargeTest
    public void testSendMessage() {
    	mState = PresenceTestState.SEND_MESSAGE;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	long localCntId = 1234;
    	String message = "hi test";
    	final SocialNetwork network = SocialNetwork.GOOGLE;
    	mEng.sendMessage (localCntId, message, network.ordinal() );
    	ServiceStatus status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
        String message1 = "test reply";
        mEng.sendMessage (localCntId, message1, network.ordinal() );
    }
    
    @MediumTest
    public void testGetPushEvent() {
    	
    	mState = PresenceTestState.GET_PUSH_EVENT;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	long localCntId = 1234;
    	String message = "hi test";
    	final SocialNetwork network = SocialNetwork.GOOGLE;
    	mEng.sendMessage (localCntId, message, network.ordinal() );
    	ServiceStatus status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
        
        mState = PresenceTestState.GET_PUSH_EVENT_CLOSED_CONVERSATION;
        mEng.sendMessage (localCntId, message, network.ordinal() );
    	status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
    	
    	
    }
    

    @MediumTest
    public void testGetSystemNotification() {
    	mState = PresenceTestState.GET_SYSTEM_NOTIFICATION;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	long localCntId = 1234;
    	String message = "hi test";
    	final SocialNetwork network = SocialNetwork.GOOGLE;
    	mEng.sendMessage (localCntId, message, network.ordinal() );
    	ServiceStatus status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
    	
    }

    
    @MediumTest
    public void testServerError() {
    	mState = PresenceTestState.GET_PRESENCE_FAIL;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	final long localContactId = 1234;
    	SyncMeDbUtils.setMeProfileId(localContactId);
    	mEng.getPresenceList();
    	ServiceStatus status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
    	    	
    }
    
    @MediumTest
    public void testPublicMethods() {
    	mState = PresenceTestState.GET_PRESENCE_FAIL;
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	final long localContactId = 1234;
    	SyncMeDbUtils.setMeProfileId(localContactId);
    	mEng.onLoginStateChanged(true);
    	mEng.onConnectionStateChanged(0);//connected
    	ServiceStatus status = mEngineTester.simpleWait(5000);
        assertEquals(ServiceStatus.ERROR_COMMS_TIMEOUT, status);
        mEng.onConnectionStateChanged(1);//disconnected
    }
    
	@Override
	public void reportBackToEngine(int reqId, EngineId engine) {
		Log.d("TAG","PresenceEngineTest.reportBackToEngine");
		ResponseQueue respQueue = ResponseQueue.getInstance();
		List<BaseDataType> data = new ArrayList<BaseDataType>();
		Hashtable<String, Object> hash = new Hashtable<String, Object>();
		Hashtable<String, Object> payload = new Hashtable<String, Object>();
		switch(mState){
		case IDLE:
			break;
		
		case SET_AVAILBAILITY:
			
			StatusMsg msg = new StatusMsg();
            msg.mCode = "ok";
            msg.mDryRun = false;
            msg.mStatus = true;
            data.add(msg);
			respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.GET_PRESENCE_RESPONSE.ordinal()));
			//mEng.onCommsInMessage();
			break;
		case GET_PRESENCE:
						
			long userId = 1234;
    		hash.put("userid", userId);
    		Hashtable<String, String> presence = new Hashtable<String, String>();
    		presence.put("facebook.com", "online");
    		List<User> users = new ArrayList<User> ();
    		User user1 = new User("1234" ,presence); 
    		users.add(user1);
    		payload.put("1234" ,presence);
    		hash.put("payload", payload);
    		
    		PresenceList list = new PresenceList(); 
    		list.createFromHashtable(hash);
			data.add(list);
			respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.GET_PRESENCE_RESPONSE.ordinal()));
			
			break;
		
		case GET_PRESENCE_FAIL:
			
			ServerError err2 = new ServerError(ErrorType.REQUEST_TIMEOUT);
            err2.errorDescription = "Fail";
            data.add(err2);
            respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
            break;
            
		case SEND_MESSAGE:
			
			long userId1 = 1234;
    		payload.put("conversation", "1");
    		List<String> users1 = new ArrayList<String> ();
    		users1.add("1234");
    		payload.put("tos" ,users1);
    		hash.put("userid", userId1);
    		hash.put("payload", payload);
    		
    		Conversation conv = new Conversation(); 
    		conv.createFromHashtable(hash);
			data.add(conv);
			respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.CREATE_CONVERSATION_RESPONSE.ordinal()));
			//mEng.onCommsInMessage();
			break;
		
		case GET_PUSH_EVENT:
			payload.put("body", "test");
    		payload.put("conversation", "2");
    		payload.put("from", "12345");
    				
    		hash.put("type", "cm");
    		hash.put("payload", payload);
    		RpgPushMessage rpgMsg = RpgPushMessage.createFromHashtable(hash);
    		BaseDataType pushData = new PushChatMessageEvent(rpgMsg, EngineId.PRESENCE_ENGINE); 
    		data.add(pushData);
			respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.PUSH_MESSAGE.ordinal()));
			break;
		
		case GET_PUSH_EVENT_CLOSED_CONVERSATION:
			
    		hash.put("type", "c0");
    		payload.put("body", "test");
    		payload.put("conversation", "2");
    		payload.put("from", "12345");
    		hash.put("payload", payload);
    		RpgPushMessage rpgMsg1 = RpgPushMessage.createFromHashtable(hash);
    		BaseDataType pushData1 = new PushClosedConversationEvent(rpgMsg1, EngineId.PRESENCE_ENGINE); 
    		data.add(pushData1);
			respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.PUSH_MESSAGE.ordinal()));
			break;
		case GET_SYSTEM_NOTIFICATION:
			
    		hash.put("type", "c0");
    		List<String> tolist = new Vector<String> ();
    		tolist.add("1234");
    		hash.put("tos" ,tolist);
    		hash.put("code", "201");
    		
    		BaseDataType pushData3 = SystemNotification.createFromHashtable(hash, EngineId.PRESENCE_ENGINE); 
    		data.add(pushData3);
			respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.PUSH_MESSAGE.ordinal()));
			break;
		
		
		}
		
		
		
	}

	@Override
	public void onEngineException(Exception exp) {
		// TODO Auto-generated method stub
		
	}
}
