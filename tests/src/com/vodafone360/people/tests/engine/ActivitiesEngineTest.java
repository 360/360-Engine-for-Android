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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.CallLog.Calls;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.ActivityContact;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.activities.ActivitiesEngine;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.rpg.PushMessageTypes;
import com.vodafone360.people.tests.TestModule;

public class ActivitiesEngineTest extends InstrumentationTestCase implements
    IEngineTestFrameworkObserver {

    /**
     * States for test harness
     */
    enum ActivityTestState {
        IDLE,
        ON_CREATE,
        ON_DESTROY,
        GET_ACTIVITIES_SUCCESS,
        GET_ACTIVITIES_SERVER_ERR,
        GET_ACTIVITIES_UNEXPECTED_RESPONSE,
        GET_POPULATED_ACTIVITIES,
        SET_STATUS,
        ON_SYNC_COMPLETE,
        GET_NEXT_RUNTIME,
        HANDLE_PUSH_MSG,
        GET_TIMELINE_EVENT_FROM_SERVER
    }

    private static final String LOG_TAG = "ActivitiesEngineTest";

    EngineTestFramework mEngineTester = null;

    ActivitiesEngine mEng = null;

    ActivityTestState mState = ActivityTestState.IDLE;

    MainApplication mApplication = null;

    TestModule mTestModule;
    
    EngineManager mEngineManager = null;
    
    // The complete list from MMS Decoder
    
    private static final int ANY_CHARSET = 0x00;

    private static final int US_ASCII = 0x03;

    private static final int ISO_8859_1 = 0x04;

    private static final int ISO_8859_2 = 0x05;

    private static final int ISO_8859_3 = 0x06;

    private static final int ISO_8859_4 = 0x07;

    private static final int ISO_8859_5 = 0x08;

    private static final int ISO_8859_6 = 0x09;

    private static final int ISO_8859_7 = 0x0A;

    private static final int ISO_8859_8 = 0x0B;

    private static final int ISO_8859_9 = 0x0C;

    private static final int SHIFT_JIS = 0x11;

    private static final int UTF_8 = 0x6A;

    private static final int BIG5 = 0x07EA;

    private static final int UCS2 = 0x03E8;

    private static final int UTF_16 = 0x03F7;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestModule = new TestModule();
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
                getInstrumentation().getTargetContext());
        mApplication.onCreate();

        
        mEngineTester = new EngineTestFramework(this);
        mEngineManager = EngineManager.createEngineManagerForTest(null , mEngineTester);
        mEng = new ActivitiesEngine(getInstrumentation().getTargetContext(), mEngineTester,
                mApplication.getDatabase());
        mEng.setTestMode(true);
        mEngineTester.setEngine(mEng);
        mState = ActivityTestState.IDLE;
        
        mEngineManager.addEngineForTest(mEng);
        
        // This statement is just to make sure that first sync completes before starting
        // other test cases.
        mEng.addStatusesSyncRequest();
        mEngineTester.waitForEvent();
        
    }

    @Override
    protected void tearDown() throws Exception {

        // stop our dummy thread?
        mEngineTester.stopEventThread();
        mEngineTester = null;
        mEng = null;
        EngineManager.destroyEngineManager();
        mEngineManager = null;
        mTestModule = null;
        
        // call at the end!!!
        super.tearDown();
    }

    @MediumTest
    public void testOnCreate() {
        boolean testPass = true;
        mState = ActivityTestState.ON_CREATE;
        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);
        try {
            mEng.onCreate();
        } catch (Exception e) {
            testPass = false;
        }

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            mEng.onCreate();
        } catch (Exception e) {
            testPass = false;
        }

        if (!testPass) {
            Log.e(LOG_TAG, "**** testUpdates (FAILED) ****\n");
        }
        assertTrue("testOnCreate() failed", testPass);
        Log.i(LOG_TAG, "**** testOnCreate (SUCCESS) ****\n");
    }

    @MediumTest
    public void testOnDestroy() {
        boolean testPass = true;
        mState = ActivityTestState.ON_DESTROY;
        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);
        try {
            mEng.onDestroy();
        } catch (Exception e) {
            testPass = false;
        }

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            mEng.onDestroy();
        } catch (Exception e) {
            testPass = false;
        }
        assertTrue("testOnDestroy() failed", testPass);
        Log.i(LOG_TAG, "**** testOnDestroy (SUCCESS) ****\n");
    }

    @MediumTest
    public void testGetActivitiesGoodNoMeProfile() {
        boolean testPass = true;
        mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addStatusesSyncRequest();

        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        Object data = mEngineTester.data();
        assertTrue(data == null);

        assertTrue("testGetActivities() failed", testPass);
        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @MediumTest   
    public void testGetActivitiesGood() {

        boolean testPass = true;
        mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
        // re-test with valid Me profile
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(), meProfile));
      
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addStatusesSyncRequest();

        ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        //assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());
        
        Object data = mEngineTester.data();
        assertTrue(data == null);

        assertTrue("testGetActivities() failed", testPass);
        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @MediumTest
    public void testOnSyncComplete() {
        boolean testPass = true;
        mState = ActivityTestState.ON_SYNC_COMPLETE;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            mEng.onSyncComplete(ServiceStatus.SUCCESS);
        } catch (Exception e) {
            testPass = false;
        }

        assertTrue("testOnSyncComplete() failed", testPass);
        Log.i(LOG_TAG, "**** testOnSyncComplete (SUCCESS) ****\n");
    }

       
    @MediumTest
    public void testGetNextRuntime() {
        boolean testPass = true;
        mState = ActivityTestState.GET_NEXT_RUNTIME;
        mEng.run();
        long runtime = mEng.getNextRunTime();
        if (runtime != -1) {
            testPass = false;
        }

        assertTrue("testGetNextRuntime() failed", testPass);
        Log.i(LOG_TAG, "**** testGetNextRuntime (SUCCESS) ****\n");
    }


    @MediumTest
    
    public void testPushMessage() {
        boolean testPass = true;
        mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;

        // create test Push msg and put it is response Q
        PushEvent evt = new PushEvent();
        evt.mMessageType = PushMessageTypes.STATUS_ACTIVITY_CHANGE;
        List<BaseDataType> data = new ArrayList<BaseDataType>();
        data.add(evt);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        ResponseQueue.getInstance().addToResponseQueue(new DecodedResponse(0, data, mEng.engineId(), DecodedResponse.ResponseType.PUSH_MESSAGE.ordinal()));
        mEng.onCommsInMessage();

        // see if anything happens
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());
        
        Object retdata = mEngineTester.data();
        assertTrue(retdata == null);

        assertTrue("testPushMessage() failed", testPass);
        Log.i(LOG_TAG, "**** testPushMessage (SUCCESS) ****\n");
    }

    @MediumTest    
    public void testUpdatePhoneCalls() {
    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
    	addNativeCall();
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_PHONE_CALLS, null);
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        deleteNativeCall();
    }
    
    @MediumTest
    public void testPopulatedActivities() {
        boolean testPass = true;
        mState = ActivityTestState.GET_POPULATED_ACTIVITIES;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addStatusesSyncRequest();

        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        assertTrue("testPopulatedActivities() failed", testPass);
        Log.i(LOG_TAG, "**** testPopulatedActivities (SUCCESS) ****\n");
    }
    
    
    @MediumTest
    public void testFetchTimelines()
    {
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	addNativeCall();
    	mState = ActivityTestState.GET_TIMELINE_EVENT_FROM_SERVER;
    	mEng.addOlderTimelinesRequest();
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
         						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        deleteNativeCall();
    }
    
    @MediumTest
    public void testFetchStatuses()
    {
    	NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
    	mEng.addGetOlderStatusesRequest();
    	
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
         						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
         	fail("Expected SUCCESS");
         }
    }
    
    @MediumTest
    public void testUpdateSMS() {
    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
		sendSMSToPhone("+919877756765");
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        deleteSMS();
        
    }
    
    public void testUpdateSMSWithContact()
    {
    	addNativeContact();
    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
		sendSMSToPhone(mDummyContactNumber);
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        deleteSMS();
        
    	
    	deleteContact();
    	
    }
    
   
    
    private static final int INBOX = 1;
    private static final int SENT=2;
    public void testUpdateMMS()
    {

    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
    	insertMMS(INBOX,UTF_8,"12312345");
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        
        
        deleteMMS();
        
        
        insertMMS(SENT,UTF_8,"1111");
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        
        deleteMMS();
        
    }
    
    public void testUpdateMMSNoSubject()
    {

    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
    	insertMMSNoSubject(INBOX,UTF_8,"12312345");
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        
        
        deleteMMS();
        
        
        insertMMSNoSubject(SENT,UTF_8,"1111");
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        
        deleteMMS();
        
    }
    
    public void testUpdateMMSWithContact()
    {
    	addNativeContact();
    	mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
    	insertMMS(INBOX,UTF_8,mDummyContactNumber);
    	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
    	
    	ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        
        
        deleteMMS();
    	
    	deleteContact();
    }
    
    
    @MediumTest 
    public void testMMSCharSet()
    {
    	
    	int charSetList[] = {ANY_CHARSET,US_ASCII,ISO_8859_1,ISO_8859_2,
    			             ISO_8859_3,ISO_8859_4,ISO_8859_5,ISO_8859_6,
    			             ISO_8859_7,ISO_8859_8,ISO_8859_9,SHIFT_JIS,
    			             UTF_8,BIG5,UCS2,UTF_16};
    	
    	for (int i = 0; i < charSetList.length;i++) {
    		NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    		insertMMS(INBOX,charSetList[i],"3444123");
        	mEng.addUiRequestToQueue(ServiceUiRequest.UPDATE_SMS, null);
        	
        	ServiceStatus status = mEngineTester.waitForEvent();
            if (status != ServiceStatus.SUCCESS &&
            						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
            	fail("Expected SUCCESS");
            }
            deleteMMS();
    	}
    	
    }
   
    @MediumTest
    
    public void testMessageLog() {
    	
    	
        final String ADDRESS = "address";
        // final String PERSON = "person";
        final String DATE = "date";
        final String READ = "read";
        final String STATUS = "status";
        final String TYPE = "type";
        final String BODY = "body";

      
        ContentValues values = new ContentValues();
        values.put(ADDRESS, "+61408219690");
        values.put(DATE, "1630000000000");
        values.put(READ, 1);
        values.put(STATUS, -1);
        values.put(TYPE, 2);
        values.put(BODY, "SMS inserting test");
        mApplication.getContentResolver().insert(Uri.parse("content://sms"),values);

        mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        // re-test with valid Me profile
        SyncMeDbUtils.setMeProfileId(null);
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(),meProfile));

        mEng.addStatusesSyncRequest();
        
        ServiceStatus status = mEngineTester.waitForEvent();
        if (status != ServiceStatus.SUCCESS &&
        						status != ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE){
        	fail("Expected SUCCESS");
        }
        
        Object data = mEngineTester.data();
        assertTrue(data == null);
        values.put(ADDRESS, "+61408219691");
        values.put(DATE, "1650000000000");
        mApplication.getContentResolver().insert(Uri.parse("content://mms"), values);

        mEng.addStatusesSyncRequest();

        assertEquals("Could not access db", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());
        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
     
        
    }
    
    @MediumTest
    public void testPublicGetterMethods()
    {
    	// This test case is not very useful. Just calls the methods but don't do much
    	mEng.onLoginStateChanged(false);
    	mEng.isTimelinesUpdated();
    	mEng.setTimelinesUpdated(true);
    	mEng.onProgressEvent(null, 0);
    	mEng.onContactSyncStateChange(null, null, null);
    	mEng.onReset();
    	
    }

    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        Log.d("TAG", "ActivityEngineTest.reportBackToEngine");
        ResponseQueue respQueue = ResponseQueue.getInstance();
        List<BaseDataType> data = new ArrayList<BaseDataType>();

        switch (mState) {
            case IDLE:
                break;
            case ON_CREATE:
            case ON_DESTROY:
                break;
            case GET_ACTIVITIES_SUCCESS:
                ActivityItem item = new ActivityItem();
                data.add(item);
                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.GET_ACTIVITY_RESPONSE.ordinal()));
                mEng.onCommsInMessage();
                break;
            case GET_TIMELINE_EVENT_FROM_SERVER:
                ActivityItem item2 = new ActivityItem();
                ActivityContact act = new ActivityContact();
                act.mName = "Bill Fleege";
                act.mLocalContactId = new Long(8);
                List<ActivityContact> clist = new ArrayList<ActivityContact>();
                clist.add(act);
                item2.contactList = clist;
                item2.activityFlags = 2;
                item2.type = ActivityItem.Type.CONTACT_JOINED;
                item2.time = System.currentTimeMillis();
                data.add(item2);
                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.GET_ACTIVITY_RESPONSE.ordinal()));
                mEng.onCommsInMessage();
                break;
            case GET_POPULATED_ACTIVITIES:
                ActivityItem item3 = new ActivityItem();
                ActivityContact act2 = new ActivityContact();
                act2.mName = "Bill Fleege";
                act2.mLocalContactId = new Long(8);
                List<ActivityContact> clist2 = new ArrayList<ActivityContact>();
                clist2.add(act2);
                item3.contactList = clist2;
                item3.activityFlags = 2;
                item3.type = ActivityItem.Type.CONTACT_JOINED;
                item3.time = System.currentTimeMillis();
                item3.title = "bills new status";
                item3.description = "a description";
                data.add(item3);

                ActivityItem item4 = new ActivityItem();
                item4.contactList = clist2;
                item4.activityFlags = 5;
                item4.type = ActivityItem.Type.CONTACT_JOINED;
                item4.time = System.currentTimeMillis();
                item4.title = "bills new status";
                item4.description = "a description";
                item4.activityId = new Long(23);
                item4.hasChildren = false;
                item4.uri = "uri";
                item4.parentActivity = new Long(0);
                item4.preview = ByteBuffer.allocate(46);
                item4.preview.position(0);
                item4.preview.rewind();
                for (int i = 0; i < 23; i++) {
                    item4.preview.putChar((char)i);
                }
                item4.previewMime = "jepg";
                item4.previewUrl = "storeurl";
                item4.store = "google";
                item4.visibilityFlags = 0;
                data.add(item4);

                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.GET_ACTIVITY_RESPONSE.ordinal()));
                mEng.onCommsInMessage();
                break;
            case GET_ACTIVITIES_SERVER_ERR:
                ServerError err = new ServerError("Catastrophe");
                err.errorDescription = "Fail";
                data.add(err);
                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
                if(mEng != null)
                {
                    mEng.onCommsInMessage();
                }
                break;
            case GET_ACTIVITIES_UNEXPECTED_RESPONSE:
                StatusMsg msg = new StatusMsg();
                msg.mCode = "ok";
                msg.mDryRun = false;
                msg.mStatus = true;
                data.add(msg);
                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.LOGIN_RESPONSE.ordinal()));
                mEng.onCommsInMessage();
                break;
            case SET_STATUS:
                Identity id3 = new Identity();
                data.add(id3);
                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.GET_AVAILABLE_IDENTITIES_RESPONSE.ordinal()));
                mEng.onCommsInMessage();
                break;
            case ON_SYNC_COMPLETE:
                ServerError err2 = new ServerError("Catastrophe");
                err2.errorDescription = "Fail";
                data.add(err2);
                respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
                mEng.onCommsInMessage();
                break;
            case GET_NEXT_RUNTIME:
                break;
            default:
        }

    }

    protected static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    private static final int PDU_FROM_FIELD = 0x89;
    private static final int PDU_TO_FIELD = 0x97;
    private void insertMMS(int msg_box, int char_set, String addr)
    {
    	
    	  	
    	final String DATE = "date";
    	final String SUBJECT = "sub";
    	final String SUBJECT_CS = "sub_cs" ;
    	final String MSG_BOX = "msg_box";
    	final String FROM = "address";
    	ContentValues values = new ContentValues();
    	values.put(DATE, "1630000000000");
    	values.put(SUBJECT, "Hello");
    	values.put(SUBJECT_CS, char_set);

    	// MSG_BOX 1 means in Inbox
    	// MSG_BOX 2 menas in Sent
    
    	values.put(MSG_BOX, msg_box);
    	
    	Uri uri = mApplication.getContentResolver().insert(Uri.parse("content://mms"),values);
    	Uri.Builder builder = MMS_CONTENT_URI.buildUpon();
    	String msgId = uri.getLastPathSegment();
        builder.appendPath(msgId).appendPath("addr");
    	ContentValues val2 = new ContentValues();
    	val2.put("address", addr);
    	val2.put("charset",UTF_8);
    	if(msg_box == 1)
    	{
    		val2.put("type", PDU_FROM_FIELD);
    	}
    	else
    	{
    		val2.put("type", PDU_TO_FIELD);
    	}
    	Uri uriPart = builder.build();
    	mApplication.getContentResolver().insert(uriPart,val2);
    	
    }
    
    private void insertMMSNoSubject(int msg_box, int char_set, String addr)
    {
    	final String DATE = "date";

    	final String MSG_BOX = "msg_box";
    	ContentValues values = new ContentValues();
    	values.put(DATE, "1630000000000");

    	// MSG_BOX 1 means in Inbox
    	// MSG_BOX 2 menas in Sent
    
    	values.put(MSG_BOX, msg_box);
    	
    	mApplication.getContentResolver().insert(Uri.parse("content://mms"),values);
    	
    }
    
    private int deleteMMS()
    {
    	return mApplication.getContentResolver().delete(Uri.parse("content://mms"), "date=?", new String[] {"1630000000000"});
    	
    }
    
    private void sendSMSToPhone(String from)
    {
    	final String ADDRESS = "address";
        final String DATE = "date";
        final String READ = "read";
        final String STATUS = "status";
        final String TYPE = "type";
        final String BODY = "body";

      
        ContentValues values = new ContentValues();
        values.put(ADDRESS, from);
        values.put(DATE, "1630000000000");
        values.put(READ, 1);
        values.put(STATUS, -1);
        values.put(TYPE, 2);
        values.put(BODY, "SMS inserting test");
        mApplication.getContentResolver().insert(Uri.parse("content://sms"),values);
    }
    
    private int deleteSMS()
    {
    	return mApplication.getContentResolver().delete(Uri.parse("content://sms"), "date=?", new String[] {"1630000000000"});
    }
    
    private static final String mDummyContactName = "Abc";
    private static final String mDummyContactNumber = "123123"; 
    private void addNativeContact()
    {

    	ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME,mDummyContactName );
        peopleValues.put(Contacts.Phones.NUMBER, mDummyContactNumber);
        peopleValues.put(Contacts.Phones.TYPE, Contacts.Phones.TYPE_MOBILE);
        mApplication.getContentResolver().insert(Contacts.People.CONTENT_URI, peopleValues);


    }
    
    private static long date;
    private void addNativeCall()
    {
    	ContentValues peopleValues = new ContentValues();
    	peopleValues.put(Calls._ID,12345 );
        peopleValues.put(Calls.NUMBER,mDummyContactNumber );
        peopleValues.put(Calls.DATE, "1650000000000");
        peopleValues.put(Calls.TYPE, 1);
        mApplication.getContentResolver().insert(Calls.CONTENT_URI, peopleValues);
        peopleValues.put(Calls._ID,12346 );
        peopleValues.put(Calls.TYPE, 2);
        mApplication.getContentResolver().insert(Calls.CONTENT_URI, peopleValues);
        peopleValues.put(Calls._ID,12347 );
        peopleValues.put(Calls.TYPE, 3);
        mApplication.getContentResolver().insert(Calls.CONTENT_URI, peopleValues);
    }
    
    private int deleteNativeCall()
    {
    	return mApplication.getContentResolver().delete(Calls.CONTENT_URI, "date=?", new String[] {"1650000000000"});
    }
    
    private int deleteContact()
    {
    	return mApplication.getContentResolver().delete(Contacts.People.CONTENT_URI, "name=?", new String[] {mDummyContactName});
    }
    
    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub

    }

}
