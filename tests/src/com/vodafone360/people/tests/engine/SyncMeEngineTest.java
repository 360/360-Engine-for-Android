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

import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.meprofile.SyncMeEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.tests.engine.contactsync.HelperClasses;

/**
 * JUnit testing for the SyncMeProfile Processor class.
 */
public class SyncMeEngineTest extends InstrumentationTestCase implements  IEngineTestFrameworkObserver {
    
 private static final String LOG_TAG = "SyncMeEngineTest";
    
    /**
     * The main application handle.
     */
    private MainApplication mApplication;
    /**
     * 
     */
    private SyncMeEngine mEngine;
    /**
     * The engine test framework handle.
     */
    private EngineTestFramework mEngineTester;
    /**
     * 
     */
    private TestModule mTestModule = new TestModule();
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // delete the database
        getInstrumentation().getTargetContext().deleteDatabase(HelperClasses.getDatabaseName());
        // create an application instance
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class, getInstrumentation().getTargetContext());
        mApplication.onCreate();
        
        mEngineTester = new EngineTestFramework(this);
        mEngine = new SyncMeEngine(mEngineTester, mApplication.getDatabase());
        mEngineTester.setEngine(mEngine);
        
        Log.i(LOG_TAG, "**** setUp() end ****");
        
    }

    @Override
    protected void tearDown() throws Exception {
        if (mApplication != null) {
            mApplication.onTerminate();
        }
        mApplication = null;
        mEngine.onReset();
        mEngine.onDestroy();
        
        super.tearDown();
        Log.i(LOG_TAG, "**** tearDown() end ****");
    }

    
    /**
     * Sets up the test framework.
     * 
     * @param factory the factory used by the ContactSyncEngine
     * @param observer the test framework observer
     */
    private void setUpContactSyncMeEngineTestFramework() {
        mEngine.onCreate();

    }
    
    
    @MediumTest
    @Suppress// framework needs to be fixed.
    public void testGetMeProfileContactFirstTimeRequest() {

        setUpContactSyncMeEngineTestFramework();
        // re-test with valid Me profile
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(),meProfile));
        

        mEngine.addGetMeProfileContactFirstTimeRequest();
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        Object data = mEngineTester.data();
        assertTrue(data == null);

        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @MediumTest
    @Suppress// framework needs to be fixed.
    public void testUpdateMeProfileContactRequest() {
        
        setUpContactSyncMeEngineTestFramework();
        // re-test with valid Me profile
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(),meProfile));
        

        mEngine.addUpdateMeProfileContactRequest();
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        Object data = mEngineTester.data();
        assertTrue(data == null);

        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @MediumTest
    @Suppress// framework needs to be fixed.
    public void testUpdateMyStatusRequest() {

        setUpContactSyncMeEngineTestFramework();
        // re-test with valid Me profile
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(),meProfile));
        

        mEngine.addUpdateMyStatusRequest("YELLO");
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        Object data = mEngineTester.data();
        assertTrue(data == null);

        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @Suppress
    @MediumTest
    public void testPushMessage() {
        
        setUpContactSyncMeEngineTestFramework();
        PushEvent evt = new PushEvent();
        evt.mMessageType = PushMessageTypes.PROFILE_CHANGE;
        List<BaseDataType> data = new ArrayList<BaseDataType>();
        data.add(evt);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        ResponseQueue.getInstance().addToResponseQueue(0, data, mEngine.engineId());
        mEngine.onCommsInMessage();

        // see if anything happens
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());
        
        Object retdata = mEngineTester.data();
        assertTrue(retdata == null);

        Log.i(LOG_TAG, "**** testPushMessage (SUCCESS) ****\n");
    }
    
    @Suppress
    @MediumTest
    public void testGetNextRuntime() {
        
        setUpContactSyncMeEngineTestFramework();
        assertTrue("testGetNextRuntime() failed", mEngine.getNextRunTime() == -1);
        Log.i(LOG_TAG, "**** testGetNextRuntime (SUCCESS) ****\n");
    }

    @MediumTest
    public void testOnCreate() {
        boolean testPass = true;
        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);
        try {
            mEngine.onCreate();
        } catch (Exception e) {
            testPass = false;
        }

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            mEngine.onCreate();
        } catch (Exception e) {
            testPass = false;
        }

        if (!testPass) {
            Log.e(LOG_TAG, "**** testUpdates (FAILED) ****\n");
        }
        assertTrue("testOnCreate() failed", testPass);
        Log.i(LOG_TAG, "**** testOnCreate (SUCCESS) ****\n");
    }
    
//    /**
//     * Tests the me profile sync with a populated DB.
//     */
//    @Suppress // Breaks tests
//    public void testMeProfileSync_withPopulatedDb() {
//        
//        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
//        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(engineEventCallback);
//        final SyncMeProfile meProfile = new SyncMeProfile(contactSyncCallback, mApplication.getDatabase(), false);
//        
//        final ResponseQueue respQueue = ResponseQueue.getInstance();
//        final List<BaseDataType> data = new ArrayList<BaseDataType>();
//        
//        // set the connection to be fine
//        NetworkAgent.setAgentState(AgentState.CONNECTED);
//        
//        // start me profile sync processor
//        meProfile.start();
//        
//        // set the response from the server to be a user profile update
//        UserProfile userProfile = new UserProfile();
//        userProfile.aboutMe = "aboutMe";
//        userProfile.contactID = 1L;
//        userProfile.gender = 0;
//        data.add(userProfile);
//        respQueue.addToResponseQueue(0, data, EngineId.CONTACT_SYNC_ENGINE);
//        meProfile.processCommsResponse(respQueue.getNextResponse(EngineId.CONTACT_SYNC_ENGINE));
//        
//        // processor should finish with a success 
//        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
//        
//        // the DB should now be populated with the dummy user profile
//        // start again the sync, it should fetch again from the server
//        meProfile.start();
//        
//        data.add(userProfile);
//        respQueue.addToResponseQueue(0, data, EngineId.CONTACT_SYNC_ENGINE);
//        meProfile.processCommsResponse(respQueue.getNextResponse(EngineId.CONTACT_SYNC_ENGINE));
//        
//        // processor should finish with a success 
//        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
//    }
//    
//    /**
//     * Tests the me profile sync when an update happens and has to be uploaded.
//     */
//    @Suppress // Breaks tests
//    public void testMeProfileSync_simpleUpdate() {
//        
//        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
//        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(engineEventCallback);
//        final SyncMeProfile meProfile = new SyncMeProfile(contactSyncCallback, mApplication.getDatabase(), false);
//        
//        final ResponseQueue respQueue = ResponseQueue.getInstance();
//        final List<BaseDataType> data = new ArrayList<BaseDataType>();
//        
//        // set the connection to be fine
//        NetworkAgent.setAgentState(AgentState.CONNECTED);
//        
//        DatabaseHelper dbHelper = mApplication.getDatabase();
//        Contact contact = new Contact();
//        contact.aboutMe = "aboutMe";
//        contact.localContactID = 1L;
//        contact.gender = 0;
//        dbHelper.setMeProfile(contact);
//        
//        // starting the sync should  upload the new Me profile
//        meProfile.start();
//        
//        // set the response from the server to be a dummy ContactChanges
//        ContactChanges contactChanges = new ContactChanges();
//        contactChanges.mUserProfile = new UserProfile();
//        data.add(contactChanges);
//        respQueue.addToResponseQueue(0, data, EngineId.CONTACT_SYNC_ENGINE);
//        meProfile.processCommsResponse(respQueue.getNextResponse(EngineId.CONTACT_SYNC_ENGINE));
//        
//        // processor should finish with a success 
//        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
//    }
//    
//    /**
//     * Tests fetching complex changes from the server.
//     */
//    @Suppress // Breaks tests
//    public void testMeProfileSync_fetchingComplexChanges() {
//        
//        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
//        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(engineEventCallback);
//        final SyncMeProfile meProfile = new SyncMeProfile(contactSyncCallback, mApplication.getDatabase(), false);
//        
//        final ResponseQueue respQueue = ResponseQueue.getInstance();
//        final List<BaseDataType> data = new ArrayList<BaseDataType>();
//        
//        // set the connection to be fine
//        NetworkAgent.setAgentState(AgentState.CONNECTED);
//        
//        // start me profile sync processor
//        meProfile.start();
//        
//        // set the response from the server to be a more realistic user profile update
//        UserProfile userProfile = new UserProfile();
//        userProfile.userID = 50L;
//        userProfile.aboutMe = "newAboutMe";
//        userProfile.contactID = 10L;
//        userProfile.gender = 1;
//        userProfile.profilePath = "foo";
//        userProfile.sources = new ArrayList<String>();
//        userProfile.updated = 2L;
//        ContactDetail contactDetail = new ContactDetail();
//        contactDetail.key = DetailKeys.VCARD_PHONE;
//        contactDetail.keyType = DetailKeyTypes.CELL;
//        contactDetail.value = "00000000";
//        userProfile.details.add(contactDetail);
//        data.add(userProfile);
//        respQueue.addToResponseQueue(0, data, EngineId.CONTACT_SYNC_ENGINE);
//        meProfile.processCommsResponse(respQueue.getNextResponse(EngineId.CONTACT_SYNC_ENGINE));
//        
//        // processor should finish with a success 
//        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
//    }
//    
//    /**
//     * Tests the me profile sync when a complex update happens and has to be uploaded.
//     */
//    @Suppress // Breaks tests
//    public void testMeProfileSync_complexUpdate() {
//        
//        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
//        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(engineEventCallback);
//        final SyncMeProfile meProfile = new SyncMeProfile(contactSyncCallback, mApplication.getDatabase(), false);
//        
//        final ResponseQueue respQueue = ResponseQueue.getInstance();
//        final List<BaseDataType> data = new ArrayList<BaseDataType>();
//        
//        // set the connection to be fine
//        NetworkAgent.setAgentState(AgentState.CONNECTED);
//        
//        DatabaseHelper dbHelper = mApplication.getDatabase();
//        Contact contact = new Contact();
//        contact.aboutMe = "aboutMe";
//        contact.localContactID = 1L;
//        contact.gender = 0;
//        contact.gender = 1;
//        contact.profilePath = "foo";
//        contact.sources = new ArrayList<String>();
//        contact.updated = 2L;
//        ContactDetail contactDetail = new ContactDetail();
//        contactDetail.key = DetailKeys.VCARD_PHONE;
//        contactDetail.keyType = DetailKeyTypes.CELL;
//        contactDetail.value = "00000000";
//        contact.details.add(contactDetail);
//        contactDetail = new ContactDetail();
//        contactDetail.key = DetailKeys.PHOTO;
//        contactDetail.photo_url = "foo";
//        contact.details.add(contactDetail);
//        dbHelper.setMeProfile(contact
//                );
//        dbHelper.markMeProfileAvatarChanged();
//        
//        // starting the sync should  upload the new Me profile
//        meProfile.start();
//        
//        // set the response from the server to be a dummy ContactChanges
//        ContactChanges contactChanges = new ContactChanges();
//        contactChanges.mUserProfile = new UserProfile();
//        data.add(contactChanges);
//        respQueue.addToResponseQueue(0, data, EngineId.CONTACT_SYNC_ENGINE);
//        meProfile.processCommsResponse(respQueue.getNextResponse(EngineId.CONTACT_SYNC_ENGINE));
//        
//        // processor should finish with a success 
//        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
//    }



    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub
        
    }



    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        // TODO Auto-generated method stub
        
    }
}
