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

package com.vodafone360.people.tests.service;

import java.util.ArrayList;
import java.util.List;

import android.R;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.IdentityCapability;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.NetworkAgentState;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.interfaces.IPeopleService;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.tests.IPeopleTestFramework;
import com.vodafone360.people.tests.PeopleTestConnectionThread;

@Suppress
public class PeopleServiceTest extends ServiceTestCase<RemoteService> implements IPeopleTestFramework {

	private final static String LOG_TAG = "PeopleServiceTest";
	private final static boolean ENABLE_REGISTRATION_TEST = true;
 	private final static int TEST_RESPONSE = 1;
 	private final static int WAIT_EVENT_TIMEOUT_MS = 30000;
	
 	private IPeopleService mPeopleService = null;
 	private MainApplication mApplication = null;
 	private Thread mEventWatcherThread = null;
 	private Handler mHandler;
 	private Object mUiRequestData = null;
	private ServiceStatus mStatus = null;
	private PeopleServiceTest mParent = null;
	private PeopleTestConnectionThread mTestConn = new PeopleTestConnectionThread(this);
 	
 	private boolean mActive = false;
	private boolean mEventThreadStarted = false;
	
	/*
	 * TODO: Fix NullPointerException caused by EngineManager not yet created at this point.
	 * Solution: call RemoteService.onCreate() first.
	 */
	private ConnectionManager mConnMgr;
	//private ConnectionManager mConnMgr = ConnectionManager.getInstance(); 

	public PeopleServiceTest() {
		super(RemoteService.class);
		lazyLoadPeopleService();
		mConnMgr = ConnectionManager.getInstance(); // Must be called after createService()
		Log.i(LOG_TAG, "PeopleServiceTest()");
		mParent = this;
	}
	
	@Override
	protected void setUp() throws Exception {
		mConnMgr.setTestConnection(mTestConn);
		mTestConn.startThread();
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (mPeopleService != null && mHandler != null) {
			mPeopleService.removeEventCallback(mHandler);
		}
		mConnMgr.free();
		super.tearDown();
	}

	protected void eventWatcherThreadMain() {
		Looper.prepare();
		synchronized (this) {
			mHandler = new Handler() {
				@Override 
				public void handleMessage(Message msg) {
					mActive = false;
					synchronized (mParent) {
						processMsg(msg);
						mParent.notify();
					}
				}
			};
			mEventThreadStarted = true;
			mParent.notify();
		}
		Looper.loop();
	}

	protected void processMsg(Message msg) {
		mStatus = ServiceStatus.fromInteger(msg.arg2);
		mUiRequestData = msg.obj;
	}
	
	private boolean matchResponse(int expectedType) {
		switch (expectedType) {
		case TEST_RESPONSE:
			return (mUiRequestData == null);
		default:
			return false;
		}
	}
	
	private synchronized ServiceStatus waitForEvent(long timeout, int respType) {
		long timeToFinish = System.nanoTime() + (timeout * 1000000);
		long remainingTime = timeout;
		mActive = true;
		while (mActive == true &&  remainingTime > 0) {
			try {
				wait(remainingTime);
			} catch (InterruptedException e) {
				// Do nothing.
			}
			remainingTime = (timeToFinish - System.nanoTime()) / 1000000;
		}
		if (!(matchResponse(respType))) {
			return ServiceStatus.ERROR_UNKNOWN;
		}

		return mStatus;
	}
	
	private boolean testContext(Context context) {
		try {
			context.getString(R.string.cancel);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	synchronized private boolean lazyLoadPeopleService() {
		if (mPeopleService != null) {
			return true;
		}
		try {
			mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
					getSystemContext() // Not good enough, must be Target Context??
					);

			Log.e(LOG_TAG, "Test 1 ["+testContext(getContext())+"]");
			Log.e(LOG_TAG, "Test 2 ["+testContext(getSystemContext())+"]");
			Log.e(LOG_TAG, "Test 3 ["+testContext(mApplication.getApplicationContext())+"]");
			Log.e(LOG_TAG, "Test 4 ["+testContext(mApplication)+"]");
			
			Instrumentation i = new Instrumentation();
			i.getTargetContext();
			Log.e(LOG_TAG, "Test 5 ["+testContext(i.getTargetContext())+"]");
			//Tests: maybe have to get getInstrumentation().getTargetContext() ??
			//Log.e(LOG_TAG, "Test 5 ["+testContext(getInstrumentation().getTargetContext())+"]");
			
			
			mApplication.onCreate();
			setApplication(mApplication); //Call before startService()
			
		} catch (ClassNotFoundException e){
			Log.e(LOG_TAG, "ClassNotFoundException " + e.toString());
		} catch (IllegalAccessException e){
			Log.e(LOG_TAG, "IllegalAccessException " + e.toString());
		} catch (InstantiationException e){
			Log.e(LOG_TAG, "InstantiationException " + e.toString());
		}
			
		// Explicitly start the service.
		Intent serviceIntent = new Intent();
		serviceIntent.setClassName("com.vodafone360.people", "com.vodafone360.people.service.RemoteService");
		startService(serviceIntent);
		
		mPeopleService = mApplication.getServiceInterface();
		if (mPeopleService == null) {
			return false;
		}
		mEventWatcherThread = new Thread(new Runnable() {
			@Override
			public void run() {
				eventWatcherThreadMain();
			}
		});
		mEventWatcherThread.start();
		while (!mEventThreadStarted) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// Add service handler.
		mPeopleService.addEventCallback(mHandler);
		
		// Restart thread.
		mTestConn.startThread();
		return true;
	}

    /*
     * List of APIs that are called in tests within this class:
     * mPeopleService.fetchUsernameState(name);
     * mPeopleService.fetchTermsOfService();
     * mPeopleService.fetchPrivacyStatement();
     * mPeopleService.register(details);
     * mPeopleService.addEventCallback(mHandler);
     * mPeopleService.removeEventCallback(mHandler);
     * mPeopleService.getLoginRequired();
     * mPeopleService.logon(loginDetails);
     * mPeopleService.fetchAvailableIdentities(filter);
     * mPeopleService.fetchMyIdentities(filter);
     * mPeopleService.validateIdentityCredentials(false, "facebook", "testUser", "testPass", null);
     * mPeopleService.setIdentityCapabilityStatus("facebook", "testUser", stat);
     * mPeopleService.getRoamingNotificationType();
     * mPeopleService.setForceConnection(true);
     * mPeopleService.getForceConnection()
     * mPeopleService.getRoamingDeviceSetting();
     * mPeopleService.notifyDataSettingChanged(InternetAvail.ALWAYS_CONNECT);
     * mPeopleService.setShowRoamingNotificationAgain(true);
     * mPeopleService.setNetworkAgentState(sas);
     * mPeopleService.getNetworkAgentState();
     * mPeopleService.startActivitySync();
     * mPeopleService.startBackgroundContactSync();
     * mPeopleService.startContactSync();
     * 
     * void checkForUpdates();
     * void setNewUpdateFrequency();
     * PresenceList getPresenceList();
     * 
     */
	
    public void testRegistration() {
    	if (!ENABLE_REGISTRATION_TEST) {
            Log.i(LOG_TAG, "Skipping registration tests...");
    		return;
    	}
    	Log.i(LOG_TAG, "**** testRegistration ****\n");
    	assertTrue("Unable to create People service", lazyLoadPeopleService());
    	
        String name = "scottkennedy1111";
    	Log.i(LOG_TAG, "Fetching username state for a name (" + name + ") - checking correct state is returned");
        mPeopleService.fetchUsernameState(name);
        ServiceStatus status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);
        assertEquals("fetchUsernameState() failed with status = "
				+ status.name(), ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);
               
        Log.i(LOG_TAG, "Fetching terms of service...");
        mPeopleService.fetchTermsOfService();
        status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);
        assertEquals("fetchTermsOfService() failed with status = "
				+ status.name(), ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);
        Log.i(LOG_TAG, "Fetching privacy statement...");
        mPeopleService.fetchPrivacyStatement();
        status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);
        assertEquals("fetchPrivacyStatement() failed with status = "
				+ status.name(), ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);
        
        Log.i(LOG_TAG, "Trying to register new account (username: " + name + ")");
        RegistrationDetails details = new RegistrationDetails();
		details.mFullname = "Gerry Rafferty";
		details.mUsername = name;
		details.mPassword = "TestTestTest";
		details.mAcceptedTAndC = true;
		details.mBirthdayDate = "1978-01-01";
		details.mCountrycode = "En";
		details.mEmail = "test@test.com";
		details.mLanguage = "English";
		details.mMobileModelId = 1L;
		details.mMobileOperatorId = 1L;
		details.mMsisdn = "447775128930";
		details.mSendConfirmationMail = false;
		details.mSendConfirmationSms = true;
		details.mSubscribeToNewsLetter = false;
		details.mTimezone = "GMT";
		mPeopleService.register(details);
		status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);
		assertEquals("register() failed with status = " + status.name(),
				ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);

		Log.i(LOG_TAG, "**** testRegistration (SUCCESS) ****\n");
    }
    
    public void testLogin(){
    	Log.i(LOG_TAG, "**** testLogin ****\n");
    	
    	if (!lazyLoadPeopleService()) {
			throw(new RuntimeException("Unable to create People service"));
		}
    	
    	try {
    		boolean ret = mPeopleService.getLoginRequired();
    		Log.i(LOG_TAG, "getLoginRequired returned = "+ret);
    	} catch(Exception e) {
    		Log.e(LOG_TAG, "getLoginRequired() failed with exception = "+e.getLocalizedMessage());
    		e.printStackTrace();
    	}
    	
    	Log.i(LOG_TAG, "logon with test credentials");
    	LoginDetails loginDetails = new LoginDetails();
    	loginDetails.mUsername = "testUser";
    	loginDetails.mPassword = "testPass";
    	loginDetails.mMobileNo = "+447502272981";
    	
    	mPeopleService.logon(loginDetails);
    	ServiceStatus status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);

    	assertEquals("logon() failed with status = " + status.name(),
				ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);
		Log.i(LOG_TAG, "**** testLogin (SUCCESS) ****\n");
    }

    public void testIdentities(){
    	Log.i(LOG_TAG, "**** testIdentities ****\n");
    	
    	assertTrue("Unable to create People service", lazyLoadPeopleService());
    	
		AuthSessionHolder ash = new AuthSessionHolder();
		ash.sessionID = "adf32419086bc23";
		ash.sessionSecret = "234789123678234";
		ash.userID = 10;
		ash.userName = "testUser";
		
		EngineManager em = EngineManager.getInstance();
    	if (em!=null) {
    		Log.i(LOG_TAG, "Creating login Engine");
    		em.getLoginEngine().setActivatedSession(ash);
    	} else {
    		Log.e(LOG_TAG, "Failed to get EngineManager");
			assertTrue("Failed to get EngineManager", false);
    	}
		
		if (LoginEngine.getSession() == null) {
			Log.e(LOG_TAG, "Failed to set fake session");
			assertTrue("Can't set fake session", false);
		}
    	
    	Bundle filter = new Bundle();
    	ArrayList<String> l = new ArrayList<String>();
		l.add(IdentityCapability.CapabilityID.chat.name());
		l.add(IdentityCapability.CapabilityID.get_own_status.name());
		filter.putStringArrayList("capability", l);
		mPeopleService.getAvailableThirdPartyIdentities();
		ServiceStatus status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);

        assertEquals("fetchAvailableIdentities() failed with status = "
				+ status.name(), ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);
        
        mPeopleService.getMyThirdPartyIdentities();
		status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, TEST_RESPONSE);
		
        assertEquals("fetchMyIdentities() failed with status = "
				+ status.name(), ServiceStatus.ERROR_INTERNAL_SERVER_ERROR, status);
        
		Log.i(LOG_TAG, "**** testIdentities (SUCCESS) ****\n");
    }

    public void testSettingGettingRoaming(){
    	boolean testPass = true;
    	Log.i(LOG_TAG, "**** testSettingGettingRoaming ****\n");
    	
    	if (!lazyLoadPeopleService()) {
			throw(new RuntimeException("Unable to create People service"));
		}
    	
    	try {
    		int type = mPeopleService.getRoamingNotificationType();
    		Log.i(LOG_TAG,"getRoamingNotificationType() = "+type);
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "getRoamingNotificationType() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	try {
    		boolean roamPermited = mPeopleService.getRoamingDeviceSetting();
    		Log.i(LOG_TAG,"getRoamingDeviceSetting() roaming permited = "+roamPermited);
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "getRoamingDeviceSetting() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	try {
    		mPeopleService.notifyDataSettingChanged(InternetAvail.ALWAYS_CONNECT);
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "notifyDataSettingChanged() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	try {
    		mPeopleService.setShowRoamingNotificationAgain(true);
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "setShowRoamingNotificationAgain() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	if (!testPass) {
			Log.e(LOG_TAG, "**** testSettingGettingRoaming (FAILED) ****\n");
		}
		assertTrue("testSettingGettingRoaming() failed", testPass);
		Log.i(LOG_TAG, "**** testSettingGettingRoaming (SUCCESS) ****\n");
    }
    
    public void testNetworkAgent(){
    	boolean testPass = true;
    	Log.i(LOG_TAG, "**** testNetworkAgent ****\n");
    	
    	if (!lazyLoadPeopleService()) {
			throw(new RuntimeException("Unable to create People service"));
		}
    	
    	try {
    		NetworkAgentState sas = new NetworkAgentState();
    		boolean[] changes = new boolean[NetworkAgent.StatesOfService.values().length];
    		for(int i=0; i<changes.length;i++){
    			changes[i] = true;
    		}
    			
    		sas.setChanges(changes);
    		sas.setInternetConnected(false);
    		mPeopleService.setNetworkAgentState(sas);
    		if (mPeopleService.getNetworkAgentState().getAgentState() != AgentState.DISCONNECTED ) {
    			testPass = false;
    			Log.e(LOG_TAG,"get/set NetworkAgentState() failed for setting DISCONNECTED");
    		}
    		
    		sas.setInternetConnected(true);
    		sas.setNetworkWorking(true);
    		mPeopleService.setNetworkAgentState(sas);
    		if (mPeopleService.getNetworkAgentState().getAgentState() != AgentState.CONNECTED) {
    			testPass = false;	
    			Log.e(LOG_TAG,"get/set NetworkAgentState() failed for setting CONNECTED");
    		}
    		
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "testNetworkAgent() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	if (!testPass) {
			Log.e(LOG_TAG, "**** testNetworkAgent (FAILED) ****\n");
		}
		assertTrue("testNetworkAgent() failed", testPass);
		Log.i(LOG_TAG, "**** testNetworkAgent (SUCCESS) ****\n");
    }

    public void testStartingVariousSyncs(){
    	boolean testPass = true;
    	Log.i(LOG_TAG, "**** testStartingVariousSyncs ****\n");
    	
    	if (!lazyLoadPeopleService()) {
			throw(new RuntimeException("Unable to create People service"));
		}
    	
    	try{
    		mPeopleService.startStatusesSync();
    		mPeopleService.startBackgroundContactSync(0);
    		mPeopleService.startContactSync();
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "testStartingVariousSyncs() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	if (!testPass) {
			Log.e(LOG_TAG, "**** testStartingVariousSyncs (FAILED) ****\n");
		}
		assertTrue("testStartingVariousSyncs() failed", testPass);
		Log.i(LOG_TAG, "**** testStartingVariousSyncs (SUCCESS) ****\n");
    }

    public void testUpdates() {
    	boolean testPass = true;
    	Log.i(LOG_TAG, "**** testUpdates ****\n");
    	
    	if (!lazyLoadPeopleService()) {
			throw(new RuntimeException("Unable to create People service"));
		}
    	
    	try {
    		mPeopleService.checkForUpdates();
    		mPeopleService.setNewUpdateFrequency();
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "testUpdates() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	if (!testPass) {
			Log.e(LOG_TAG, "**** testUpdates (FAILED) ****\n");
		}
		assertTrue("testUpdates() failed", testPass);
		Log.i(LOG_TAG, "**** testUpdates (SUCCESS) ****\n");
    }
    
    //TODO: to be updated
    public void testPresence() {
    	boolean testPass = true;
    	Log.i(LOG_TAG, "**** testPresence ****\n");
    	if (!lazyLoadPeopleService()) {
			throw(new RuntimeException("Unable to create People service"));
		}
    	
    	try {
    		mPeopleService.getPresenceList(-1L);
    	} catch(Exception e) {
    		testPass = false;
        	Log.e(LOG_TAG, "testPresence() failed = "+e.getMessage());
        	e.printStackTrace();
    	}
    	
    	if (!testPass) {
			Log.e(LOG_TAG, "**** testPresence (FAILED) ****\n");
		}
		assertTrue("testPresence() failed", testPass);
		Log.i(LOG_TAG, "**** testPresence (SUCCESS) ****\n");
    }
    
    @Override
	public IConnection testConnectionThread() {
		return null;
	}

	@Override
	public void reportBackToFramework(int reqId, EngineId engine) {
		/*
		 * We are not interested in testing specific engines in those tests then for all kind of 
		 * requests we will return error because it is handled by all engines, just to finish flow.
		 */
		ResponseQueue respQueue = ResponseQueue.getInstance();
		List<BaseDataType> data = new ArrayList<BaseDataType>();
		
		ServerError se1 = new ServerError(ServerError.ErrorType.INTERNALERROR);
		se1.errorDescription = "Test error produced by test framework, ignore it";
		data.add(se1);
		respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
	}
}