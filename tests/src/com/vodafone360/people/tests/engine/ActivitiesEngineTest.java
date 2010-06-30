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
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;
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
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.activities.ActivitiesEngine;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
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

    TestModule mTestModule = new TestModule();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
                getInstrumentation().getTargetContext());
        mApplication.onCreate();

        // EngineManager.createEngineManager(getInstrumentation().getTargetContext(),
        // null);

        mEngineTester = new EngineTestFramework(this);
        mEng = new ActivitiesEngine(getInstrumentation().getTargetContext(), mEngineTester,
                mApplication.getDatabase());
        mEngineTester.setEngine(mEng);
        mState = ActivityTestState.IDLE;
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
    @Suppress // Takes too long.
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
    @Suppress // Takes too long.
    public void testGetActivitiesGood() {

        boolean testPass = true;
        mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
        // re-test with valid Me profile
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(),meProfile));
        

        mEng.addStatusesSyncRequest();
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        Object data = mEngineTester.data();
        assertTrue(data == null);

        assertTrue("testGetActivities() failed", testPass);
        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @MediumTest
    @Suppress // Takes too long.
    public void testGetActivitiesServerErr() {
        boolean testPass = true;
        mState = ActivityTestState.GET_ACTIVITIES_SERVER_ERR;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addStatusesSyncRequest();
        ServiceStatus status = mEngineTester.waitForEvent();
        if (status == ServiceStatus.SUCCESS) {
            throw (new RuntimeException("Did not expect SUCCESS"));
        }

        Object data = mEngineTester.data();
        assertTrue(data == null);

        assertTrue("testGetActivitiesServerErr() failed", testPass);
        Log.i(LOG_TAG, "**** testGetActivitiesServerErr (SUCCESS) ****\n");
    }

    @MediumTest
    @Suppress // Takes too long.
    public void testGetActivitiesUnexpectedResponse() {
        boolean testPass = true;
        mState = ActivityTestState.GET_ACTIVITIES_UNEXPECTED_RESPONSE;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.addStatusesSyncRequest();

        assertEquals("Expected ERROR_COMMS, not timeout", ServiceStatus.ERROR_COMMS, mEngineTester.waitForEvent());

        Object data = mEngineTester.data();
        assertTrue(data == null);

        assertTrue("testGetActivitiesUnexpectedResponse() failed", testPass);
        Log.i(LOG_TAG, "**** testGetActivitiesUnexpectedResponse (SUCCESS) ****\n");
    }

    /*
     * @MediumTest public void testSetStatus(){ boolean testPass = true; mState
     * = ActivityTestState.SET_STATUS; List<ActivityItem> actList = new
     * ArrayList<ActivityItem>(); ActivityItem actItem = createActivityItem();
     * actList.add(actItem);
     * NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
     * mEng.addUiSetStatusRequest(actList); ServiceStatus status =
     * mEngineTester.waitForEvent(); if(status != ServiceStatus.SUCCESS){
     * throw(new RuntimeException("Expected SUCCESS")); }
     * assertTrue("testSetStatus() failed", testPass); Log.i(LOG_TAG,
     * "**** testSetStatus (SUCCESS) ****\n"); }
     */

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
        long runtime = mEng.getNextRunTime();
        if (runtime != -1) {
            testPass = false;
        }

        assertTrue("testGetNextRuntime() failed", testPass);
        Log.i(LOG_TAG, "**** testGetNextRuntime (SUCCESS) ****\n");
    }

    @Suppress
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
        ResponseQueue.getInstance().addToResponseQueue(0, data, mEng.engineId());
        mEng.onCommsInMessage();

        // see if anything happens
        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());
        
        Object retdata = mEngineTester.data();
        assertTrue(retdata == null);

        assertTrue("testPushMessage() failed", testPass);
        Log.i(LOG_TAG, "**** testPushMessage (SUCCESS) ****\n");
    }

    /*
     * @MediumTest public void testGetTimelineEvent(){ boolean testPass = true;
     * mState = ActivityTestState.GET_TIMELINE_EVENT_FROM_SERVER; Contact
     * meProfile = mTestModule.createDummyContactData(); ServiceStatus status =
     * mApplication.getDatabase().setMeProfile(meProfile); if(status !=
     * ServiceStatus.SUCCESS){ throw(new
     * RuntimeException("Could not access db")); }
     * mEng.addUiGetActivitiesRequest(); status = mEngineTester.waitForEvent();
     * if(status != ServiceStatus.SUCCESS){ throw(new
     * RuntimeException("Expected SUCCESS")); } Object data =
     * mEngineTester.data(); assertTrue(data==null);
     * mEng.addUiGetActivitiesRequest(); status = mEngineTester.waitForEvent();
     * if(status != ServiceStatus.SUCCESS){ throw(new
     * RuntimeException("Expected SUCCESS")); } data = mEngineTester.data();
     * assertTrue(data==null); assertTrue("testPushMessage() failed", testPass);
     * Log.i(LOG_TAG, "**** testGetTimelineEvent (SUCCESS) ****\n"); }
     */

    @MediumTest
    @Suppress // Takes too long.
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
        /* Uri inserted = */mApplication.getContentResolver().insert(Uri.parse("content://sms"),
                values);

        mState = ActivityTestState.GET_ACTIVITIES_SUCCESS;
        // re-test with valid Me profile
        Contact meProfile = mTestModule.createDummyContactData();
        assertEquals("Could not access db", ServiceStatus.SUCCESS, SyncMeDbUtils.setMeProfile(mApplication.getDatabase(),meProfile));

        mEng.addStatusesSyncRequest();

        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());
        
        Object data = mEngineTester.data();
        assertTrue(data == null);

        values.put(DATE, "1650000000000");
        /* inserted = */mApplication.getContentResolver()
                .insert(Uri.parse("content://mms"), values);

        mEng.addStatusesSyncRequest();

        assertEquals("Could not access db", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        Log.i(LOG_TAG, "**** testGetActivities (SUCCESS) ****\n");
    }

    @Suppress
    public void testPopulatedActivities() {
        boolean testPass = true;
        mState = ActivityTestState.GET_POPULATED_ACTIVITIES;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        assertEquals("Expected SUCCESS, not timeout", ServiceStatus.SUCCESS, mEngineTester.waitForEvent());

        assertTrue("testPopulatedActivities() failed", testPass);
        Log.i(LOG_TAG, "**** testPopulatedActivities (SUCCESS) ****\n");
    }

    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        Log.d("TAG", "IdentityEngineTest.reportBackToEngine");
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
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_TIMELINE_EVENT_FROM_SERVER:
                ActivityItem item2 = new ActivityItem();
                ActivityContact act = new ActivityContact();
                act.mName = "Bill Fleege";
                act.mLocalContactId = new Long(8);
                List<ActivityContact> clist = new ArrayList<ActivityContact>();
                clist.add(act);
                item2.mContactList = clist;
                item2.mActivityFlags = 2;
                item2.mType = ActivityItem.Type.CONTACT_JOINED;
                item2.mTime = System.currentTimeMillis();
                data.add(item2);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_POPULATED_ACTIVITIES:
                ActivityItem item3 = new ActivityItem();
                ActivityContact act2 = new ActivityContact();
                act2.mName = "Bill Fleege";
                act2.mLocalContactId = new Long(8);
                List<ActivityContact> clist2 = new ArrayList<ActivityContact>();
                clist2.add(act2);
                item3.mContactList = clist2;
                item3.mActivityFlags = 2;
                item3.mType = ActivityItem.Type.CONTACT_JOINED;
                item3.mTime = System.currentTimeMillis();
                item3.mTitle = "bills new status";
                item3.mDescription = "a description";
                data.add(item3);

                ActivityItem item4 = new ActivityItem();
                item4.mContactList = clist2;
                item4.mActivityFlags = 5;
                item4.mType = ActivityItem.Type.CONTACT_JOINED;
                item4.mTime = System.currentTimeMillis();
                item4.mTitle = "bills new status";
                item4.mDescription = "a description";
                item4.mActivityId = new Long(23);
                item4.mHasChildren = false;
                item4.mUri = "uri";
                item4.mParentActivity = new Long(0);
                item4.mPreview = ByteBuffer.allocate(46);
                item4.mPreview.position(0);
                item4.mPreview.rewind();
                for (int i = 0; i < 23; i++) {
                    item4.mPreview.putChar((char)i);
                }
                item4.mPreviewMime = "jepg";
                item4.mPreviewUrl = "storeurl";
                item4.mStore = "google";
                item4.mVisibilityFlags = 0;
                data.add(item4);

                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_ACTIVITIES_SERVER_ERR:
                ServerError err = new ServerError();
                err.errorType = "Catastrophe";
                err.errorValue = "Fail";
                data.add(err);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_ACTIVITIES_UNEXPECTED_RESPONSE:
                StatusMsg msg = new StatusMsg();
                msg.mCode = "ok";
                msg.mDryRun = false;
                msg.mStatus = true;
                data.add(msg);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case SET_STATUS:
                Identity id3 = new Identity();
                data.add(id3);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case ON_SYNC_COMPLETE:
                ServerError err2 = new ServerError();
                err2.errorType = "Catastrophe";
                err2.errorValue = "Fail";
                data.add(err2);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_NEXT_RUNTIME:
                break;
            default:
        }

    }

    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub

    }

}
