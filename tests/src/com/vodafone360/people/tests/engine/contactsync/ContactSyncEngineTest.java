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

package com.vodafone360.people.tests.engine.contactsync;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.BaseSyncProcessor;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.engine.contactsync.ProcessorFactory;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.IContactSyncObserver;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.Mode;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.State;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.tests.engine.EngineTestFramework;
import com.vodafone360.people.tests.engine.IEngineTestFrameworkObserver;

public class ContactSyncEngineTest extends InstrumentationTestCase {

    private static final String LOG_TAG = "ContactSyncEngineTest";

    /**
     * The main application handle.
     */
    private MainApplication mApplication;

    /**
     * The engine test framework handle.
     */
    private EngineTestFramework mEngineTester;

    /**
     * The contact sync engine handle.
     */
    private ContactSyncEngine mContactSyncEngine;

    @Override
    protected void setUp() throws Exception {

        Log.i(LOG_TAG, "**** setUp() begin ****");

        super.setUp();

        // delete the database
        getInstrumentation().getTargetContext().deleteDatabase(getDatabaseName());

        // create an application instance
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
                getInstrumentation().getTargetContext());
        mApplication.onCreate();

        Log.i(LOG_TAG, "**** setUp() end ****");
    }

    @Override
    protected void tearDown() throws Exception {

        Log.i(LOG_TAG, "**** tearDown() begin ****");

        mContactSyncEngine.onDestroy();
        if (mEngineTester != null)
            mEngineTester.stopEventThread();

        if (mApplication != null) {
            mApplication.onTerminate();
        }

        mApplication = null;
        mEngineTester = null;
        mContactSyncEngine = null;

        // always call at the end
        super.tearDown();

        Log.i(LOG_TAG, "**** tearDown() end ****");
    }

    /**
     * Sets up the test framework.
     * 
     * @param factory the factory used by the ContactSyncEngine
     * @param observer the test framework observer
     */
    private void setUpContactSyncEngineTestFramework(ProcessorFactory factory,
            IEngineTestFrameworkObserver observer) {
        mEngineTester = new EngineTestFramework(observer);
        mContactSyncEngine = new ContactSyncEngine(mEngineTester, mApplication, mApplication
                .getDatabase(), factory);
        mContactSyncEngine.onCreate();

        mEngineTester.setEngine(mContactSyncEngine);
    }

    /**
     * Sets up the ContactSyncEngine without the test framework.
     * 
     * @param eventCallback the engine event callback
     * @param factory the factory used by the ContactSyncEngine
     */
    private void minimalEngineSetup(IEngineEventCallback eventCallback, ProcessorFactory factory) {

        mContactSyncEngine = new ContactSyncEngine(eventCallback, mApplication, mApplication
                .getDatabase(), factory);
        mContactSyncEngine.onCreate();
    }

    /**
     * Checks that life cycle methods do not crash.
     */
    public void testLifecycle() {

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        mContactSyncEngine.onDestroy();
    }

    /**
     * Verifies that the first time sync can perform correctly with dummy
     * replies doing no modifications.
     */
    @Suppress // Breaks tests.
    public void testFirstTimeSync_dummyReplies() {

        Log.i(LOG_TAG, "**** testFirstTimeSync_dummyReplies ****");

        final FirstTimeSyncFrameworkHandler handler = new FirstTimeSyncFrameworkHandler();
        setUpContactSyncEngineTestFramework(null, handler);
        handler.setContactSyncEngine(mContactSyncEngine);

        NetworkAgent.setAgentState(AgentState.CONNECTED);

        mContactSyncEngine.addUiStartFullSync();
        ServiceStatus status = mEngineTester.waitForEvent();
        Log.d(LOG_TAG, "AFTER waitForEvent()");
        assertEquals(ServiceStatus.SUCCESS, status);
    }

    /**
     * Verifies that the first time sync triggers a call to the correct
     * processors and in the right order.
     */
    @Suppress // Breaks tests.
    public void testFirstTimeSync_dummyProcessors() {

        // list of the processors in calling order
        final ArrayList<Integer> processorTypeList = new ArrayList<Integer>();
        // list of expected processors in the right calling order
        final ArrayList<Integer> expectedTypeList = new ArrayList<Integer>();

        // set the expected processors
        expectedTypeList.add(new Integer(ProcessorFactory.DOWNLOAD_SERVER_CONTACTS));
        expectedTypeList.add(new Integer(ProcessorFactory.FETCH_NATIVE_CONTACTS));
        expectedTypeList.add(new Integer(ProcessorFactory.SYNC_ME_PROFILE));
        expectedTypeList.add(new Integer(ProcessorFactory.UPLOAD_SERVER_CONTACTS));

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);
                processorTypeList.add(new Integer(type));

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
        minimalEngineSetup(engineEventCallback, factory);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        mContactSyncEngine.addUiStartFullSync();

        mContactSyncEngine.run();

        // check the processors order
        assertTrue(processorTypeList.equals(expectedTypeList));
    }

    /**
     * Verifies that events are fired after UI requests.
     */
    @Suppress // Breaks tests.
    public void testUiRequestCompleteEvent_fullSync() {

        Log.i(LOG_TAG, "**** testUiRequestCompleteEvent_fullSync() begin ****");

        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, nextRuntime);

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // ask for a full sync
        mContactSyncEngine.addUiStartFullSync();
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        Log.i(LOG_TAG, "**** testUiRequestCompleteEvent_fullSync() end ****");
    }

    /**
     * Verifies that server sync completes correctly.
     */
    @Suppress // Breaks tests.
    public void testUiRequestCompleteEvent_serverSync() {

        Log.i(LOG_TAG, "**** testUiRequestCompleteEvent_serverSync() begin ****");

        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, nextRuntime);

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // ask for a full sync
        mContactSyncEngine.addUiStartFullSync();
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        // ask for a server sync
        uiEventCall.reset();
        mContactSyncEngine.addUiStartServerSync(0);
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        Log.i(LOG_TAG, "**** testUiRequestCompleteEvent_serverSync() end ****");
    }

    /**
     * Checks that the engine events are correctly sent to listeners.
     */
    @Suppress // Breaks tests.
    public void testEventCallback() {

        Log.i(LOG_TAG, "**** testEventCallback() begin ****");

        // Set up the expected events
        final ArrayList<ContactSyncObserver.ContactSyncStateChanged> expectedCssc = new ArrayList<ContactSyncObserver.ContactSyncStateChanged>();
        final ArrayList<ContactSyncObserver.ProgressEvent> expectedPe = new ArrayList<ContactSyncObserver.ProgressEvent>();
        final ArrayList<ContactSyncObserver.SyncComplete> expectedSc = new ArrayList<ContactSyncObserver.SyncComplete>();

        setupExpectedFirstTimeSyncObserverCalls(expectedCssc, expectedPe, expectedSc);

        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        final ContactSyncObserver observer = new ContactSyncObserver();

        minimalEngineSetup(engineEventCallback, factory);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        // add a listener
        mContactSyncEngine.addEventCallback(observer);

        // ask for a full sync
        mContactSyncEngine.addUiStartFullSync();

        // perform the sync
        mContactSyncEngine.run();

        // compare the retrieved events with the expected ones
        assertTrue(expectedCssc.equals(observer.mCsscList));
        assertTrue(expectedPe.equals(observer.mPeList));
        assertTrue(expectedSc.equals(observer.mScList));

        Log.i(LOG_TAG, "**** testEventCallback() end ****");
    }

    /**
     * Checks the the method isFirstTimeSyncComplete() returns the correct
     * values.
     */
    @Suppress // Breaks tests.
    public void testIsFirstTimeSyncComplete() {
        Log.i(LOG_TAG, "**** testIsFirstTimeSyncComplete() begin ****");

        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, nextRuntime);

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // first time sync has not been done yet
        assertEquals(false, mContactSyncEngine.isFirstTimeSyncComplete());

        // ask for a full sync
        mContactSyncEngine.addUiStartFullSync();
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        // first time sync has still not been done yet
        assertEquals(false, mContactSyncEngine.isFirstTimeSyncComplete());

        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        // first time sync has been done
        assertEquals(true, mContactSyncEngine.isFirstTimeSyncComplete());

        Log.i(LOG_TAG, "**** testIsFirstTimeSyncComplete() end ****");
    }

    /**
     * Checks that nothing is scheduled before the first time sync has been
     * completed.
     */
    public void testAutoSyncTimer() {

        Log.i(LOG_TAG, "**** testAutoSyncTimer() begin ****");

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        // set the connection to be fine
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, mContactSyncEngine.getNextRunTime());

        mContactSyncEngine.run();

        assertEquals(-1, mContactSyncEngine.getNextRunTime());

        mContactSyncEngine.run();

        assertEquals(-1, mContactSyncEngine.getNextRunTime());

        Log.i(LOG_TAG, "**** testAutoSyncTimer() end ****");
    }

    /**
     * Checks that background sync is performed after the first time sync.
     */
    @Suppress // Breaks tests.
    public void testBackgroundSync() {

        Log.i(LOG_TAG, "**** testBackgroundSync() begin ****");

        final ArrayList<ProcessorLog> processorLogs = new ArrayList<ProcessorLog>();
        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);
                ProcessorLog log = new ProcessorLog();
                log.type = type;
                log.time = System.currentTimeMillis();

                processorLogs.add(log);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        // set the connection to be fine
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, mContactSyncEngine.getNextRunTime());

        // force a first time sync
        mContactSyncEngine.addUiStartFullSync();

        nextRuntime = mContactSyncEngine.getNextRunTime();
        // next runtime should be now
        assertEquals(0, nextRuntime);

        // perform the first time sync
        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        // check that a thumbnail sync is scheduled for now
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        // reset the processor logs
        processorLogs.clear();

        // get the thumbnail sync to be run
        mContactSyncEngine.run();

        // check processor calls
        ProcessorLog log;
        assertEquals(2, processorLogs.size());
        log = processorLogs.get(0);
        assertEquals(ProcessorFactory.DOWNLOAD_SERVER_THUMBNAILS, log.type);
        log = processorLogs.get(1);
        assertEquals(ProcessorFactory.UPLOAD_SERVER_THUMBNAILS, log.type);

        // check that native sync is scheduled for now
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        // reset the processor logs
        processorLogs.clear();

        // get the native sync to be run
        mContactSyncEngine.run();

        // check processor calls
        assertEquals(1, processorLogs.size());
        log = processorLogs.get(0);
        assertEquals(ProcessorFactory.UPDATE_NATIVE_CONTACTS, log.type);

        // check that nothing else is scheduled
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(-1, nextRuntime);

        /*
         * long startingTime = System.currentTimeMillis(); long duration =
         * 60000; long currentTime, timeToWait; while( (currentTime =
         * System.currentTimeMillis()) < (startingTime + duration) ) {
         * nextRuntime = mContactSyncEngine.getNextRunTime(); timeToWait =
         * nextRuntime > currentTime ? (nextRuntime-currentTime) : 0;
         * Log.e(LOG_TAG,
         * "testBackgroundSyncAfterFirstTimeSync(), timeToWait ="+timeToWait);
         * if (timeToWait > 0) { try { synchronized (this) { wait(timeToWait); }
         * } catch(Exception e) { Log.e(LOG_TAG,
         * "testBackgroundSyncAfterFirstTimeSync(), error while waiting: "+e); }
         * } Log.e(LOG_TAG,
         * "testBackgroundSyncAfterFirstTimeSync(), calling run()");
         * mContactSyncEngine.run(); }
         */

        Log.i(LOG_TAG, "**** testBackgroundSync() end ****");
    }

    /**
     * Checks different sync request that come from: -first time sync -fake
     * database change event -full sync -server sync
     */
    @Suppress // Breaks tests.
    public void testAllSyncs() {

        Log.i(LOG_TAG, "**** testAllSyncs() begin ****");

        final ArrayList<ProcessorLog> processorLogs = new ArrayList<ProcessorLog>();
        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);
                ProcessorLog log = new ProcessorLog();
                log.type = type;
                log.time = System.currentTimeMillis();

                processorLogs.add(log);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        // set the connection to be fine
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, mContactSyncEngine.getNextRunTime());

        // force a first time sync
        mContactSyncEngine.addUiStartFullSync();

        nextRuntime = mContactSyncEngine.getNextRunTime();
        // next runtime should be now
        assertEquals(0, nextRuntime);

        // perform the first time sync
        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        // check that a thumbnail sync is scheduled for now
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        // reset the processor logs
        processorLogs.clear();

        // get the thumbnail sync to be run
        mContactSyncEngine.run();

        // check processor calls
        ProcessorLog log;
        assertEquals(2, processorLogs.size());
        log = processorLogs.get(0);
        assertEquals(ProcessorFactory.DOWNLOAD_SERVER_THUMBNAILS, log.type);
        log = processorLogs.get(1);
        assertEquals(ProcessorFactory.UPLOAD_SERVER_THUMBNAILS, log.type);

        // check that native sync is scheduled for now
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(0, nextRuntime);

        // reset the processor logs
        processorLogs.clear();

        // get the native sync to be run
        mContactSyncEngine.run();

        // check processor calls
        assertEquals(1, processorLogs.size());
        log = processorLogs.get(0);
        assertEquals(ProcessorFactory.UPDATE_NATIVE_CONTACTS, log.type);

        // reset the processor logs
        processorLogs.clear();

        // request a full sync
        mContactSyncEngine.addUiStartFullSync();

        nextRuntime = mContactSyncEngine.getNextRunTime();
        // next runtime should be now
        assertEquals(0, nextRuntime);

        // perform the full sync
        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        // get the thumbnail sync to be run
        mContactSyncEngine.run();

        // get the native sync to be run
        mContactSyncEngine.run();

        // request a server sync
        mContactSyncEngine.addUiStartServerSync(0);

        nextRuntime = mContactSyncEngine.getNextRunTime();
        // next runtime should be now
        assertEquals(0, nextRuntime);

        // perform the server sync
        mContactSyncEngine.run();

        // get the thumbnail sync to be run
        mContactSyncEngine.run();

        // check nothing to be done
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertEquals(-1, nextRuntime);

        // fake a database change notification
        Handler handler = getContactSyncEngineHandler(mContactSyncEngine);

        Message msg = new Message();
        msg.what = ServiceUiRequest.DATABASE_CHANGED_EVENT.ordinal();
        msg.arg1 = DatabaseHelper.DatabaseChangeType.CONTACTS.ordinal();
        msg.arg2 = 0;
        handler.sendMessage(msg);

        final Looper looper = Looper.myLooper();
        final MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {

            @Override
            public boolean queueIdle() {
                // message has been processed and the looper is now in idle
                // state
                // quit the loop() otherwise we would not be able to carry on
                looper.quit();

                return false;
            }

        });

        // get the message processed by the thread event loop
        Looper.loop();

        // check sync is scheduled within 30 seconds
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertTrue(isValueInsideErrorMargin(30000, nextRuntime - System.currentTimeMillis(), 5));
        final long timeBeforeWait = System.currentTimeMillis();

        // reset the processor logs
        processorLogs.clear();

        // call run() and check that nothing is performed
        mContactSyncEngine.run();
        assertEquals(0, processorLogs.size());

        // wait until we get the nextRuntime to now
        boolean isNextRuntimeNow = false;
        while (!isNextRuntimeNow) {
            try {
                long timeToWait = mContactSyncEngine.getNextRunTime();
                timeToWait = (timeToWait <= 0) ? 0 : timeToWait - System.currentTimeMillis();
                if (timeToWait > 0) {
                    synchronized (this) {
                        Log.i(LOG_TAG, "timeToWait=" + timeToWait);
                        wait(timeToWait);
                    }
                }
                if (mContactSyncEngine.getNextRunTime() < System.currentTimeMillis()) {
                    isNextRuntimeNow = true;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "testAllSyncs(): error while waiting for the runtime now");
            }
        }

        long timeAfterWait = System.currentTimeMillis();

        // check that we have waited about 30 seconds
        assertTrue(isValueInsideErrorMargin(timeAfterWait - timeBeforeWait, 30000, 5));

        // call run() until the sync is performed
        final long startTime = System.currentTimeMillis();

        while (processorLogs.size() < 7) {

            if (System.currentTimeMillis() - startTime > TEST_TIMEOUT) {
                fail("It seems that the engine is stuck, the processor logs should contain 7 objects by now!");
            }

            mContactSyncEngine.run();
        }

        // check processor calls
        assertEquals(6, processorLogs.size());
        log = processorLogs.get(0);
        assertEquals(ProcessorFactory.DOWNLOAD_SERVER_CONTACTS, log.type);
        log = processorLogs.get(1);
        assertEquals(ProcessorFactory.UPLOAD_SERVER_CONTACTS, log.type);
        log = processorLogs.get(2);
        assertEquals(ProcessorFactory.DOWNLOAD_SERVER_THUMBNAILS, log.type);
        log = processorLogs.get(3);
        assertEquals(ProcessorFactory.UPLOAD_SERVER_THUMBNAILS, log.type);
        log = processorLogs.get(4);
        assertEquals(ProcessorFactory.SYNC_ME_PROFILE, log.type);
        log = processorLogs.get(5);
        assertEquals(ProcessorFactory.UPDATE_NATIVE_CONTACTS, log.type);

        Log.i(LOG_TAG, "**** testAllSyncs() end ****");
    }

    /**
     * Tests that the native sync is scheduled and performed after a first time
     * sync then a re-instantiation of the ContactSyncEngine.
     */
    @Suppress // Breaks tests.
    public void testNativeSync_newEngineInstantiation() {

        Log.i(LOG_TAG, "**** testNativeSync_newEngineInstantiation() begin ****");

        final ArrayList<ProcessorLog> processorLogs = new ArrayList<ProcessorLog>();
        final UiEventCall uiEventCall = new UiEventCall();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);
                ProcessorLog log = new ProcessorLog();
                log.type = type;
                log.time = System.currentTimeMillis();

                processorLogs.add(log);

                return new DummySyncProcessor(mContactSyncEngine, null);
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        // set the connection to be fine
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, mContactSyncEngine.getNextRunTime());

        // force a first time sync
        mContactSyncEngine.addUiStartFullSync();

        nextRuntime = mContactSyncEngine.getNextRunTime();
        // next runtime should be now
        assertEquals(0, nextRuntime);

        // perform the first time sync
        mContactSyncEngine.run();

        // check that first time sync is completed
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.SUCCESS.ordinal());

        // destroy the engine
        mContactSyncEngine.onDestroy();
        mContactSyncEngine = null;

        // create a new ContactSyncEngine
        minimalEngineSetup(engineEventCallback, factory);

        processorLogs.clear();

        // check sync is scheduled within 30 seconds
        nextRuntime = mContactSyncEngine.getNextRunTime();
        assertTrue(isValueInsideErrorMargin(30000, nextRuntime - System.currentTimeMillis(), 5));

        final long timeBeforeWait = System.currentTimeMillis();

        // call run() and check that nothing is performed
        mContactSyncEngine.run();
        assertEquals(0, processorLogs.size());

        // wait until we get the nextRuntime to now
        boolean isNextRuntimeNow = false;
        while (!isNextRuntimeNow) {
            try {
                long timeToWait = mContactSyncEngine.getNextRunTime();
                timeToWait = (timeToWait <= 0) ? 0 : timeToWait - System.currentTimeMillis();
                if (timeToWait > 0) {
                    synchronized (this) {
                        Log.i(LOG_TAG, "timeToWait=" + timeToWait);
                        wait(timeToWait);
                    }
                }
                if (mContactSyncEngine.getNextRunTime() < System.currentTimeMillis()) {
                    isNextRuntimeNow = true;
                }
            } catch (Exception e) {
                Log
                        .e(LOG_TAG,
                                "testAllSyncsAfterFirstTimeSync(): error while waiting for the runtime now");
            }
        }

        long timeAfterWait = System.currentTimeMillis();

        // check that we have waited about 30 seconds
        assertTrue(isValueInsideErrorMargin(timeAfterWait - timeBeforeWait, 30000, 5));

        // call run() until the sync is performed
        final long startTime = System.currentTimeMillis();

        while (processorLogs.size() < 3) {

            if (System.currentTimeMillis() - startTime > TEST_TIMEOUT) {
                fail("It seems that the engine is stuck, the processor logs should contain 3 objects by now!");
            }

            mContactSyncEngine.run();
        }

        // check processor calls
        ProcessorLog log;
        assertEquals(3, processorLogs.size());
        log = processorLogs.get(0);
        assertEquals(ProcessorFactory.FETCH_NATIVE_CONTACTS, log.type);
        log = processorLogs.get(1);
        assertEquals(ProcessorFactory.UPLOAD_SERVER_CONTACTS, log.type);
        log = processorLogs.get(2);
        assertEquals(ProcessorFactory.UPDATE_NATIVE_CONTACTS, log.type);

        Log.i(LOG_TAG, "**** testNativeSync_newEngineInstantiation() end ****");
    }

    /**
     * Tests the sync is cancelled in case we remove user data.
     */
    @Suppress // Breaks tests.
    public void testCancelSync() {

        Log.i(LOG_TAG, "**** testNativeSync_newEngineInstantiation() begin ****");

        final ArrayList<ProcessorLog> processorLogs = new ArrayList<ProcessorLog>();
        final UiEventCall uiEventCall = new UiEventCall();
        final ProcessorLog processorLog = new ProcessorLog();

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase() {

            @Override
            public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {

                Log
                        .i(LOG_TAG, "onUiEvent: " + event + ", " + request + ", " + status + ", "
                                + data);

                uiEventCall.event = event.ordinal();
                uiEventCall.request = request;
                uiEventCall.status = status;
                uiEventCall.data = data;
            }

        };

        final ProcessorFactory factory = new ProcessorFactory() {

            @Override
            public BaseSyncProcessor create(int type, IContactSyncCallback callback,
                    DatabaseHelper dbHelper, Context context, ContentResolver cr) {

                Log.i(LOG_TAG, "create(), type=" + type);
                ProcessorLog log = new ProcessorLog();
                log.type = type;
                log.time = System.currentTimeMillis();

                processorLogs.add(log);

                return new BaseSyncProcessor(mContactSyncEngine, null) {

                    @Override
                    protected void doCancel() {
                        // cancel the job
                        processorLog.type = 1;
                    }

                    @Override
                    protected void doStart() {
                        // set a "timeout" to be called back immediately
                        setTimeout(0);
                        processorLog.type = 2;
                    }

                    @Override
                    public void onTimeoutEvent() {
                        // set the job as completed
                        Log.i(LOG_TAG, "onTimeoutEvent()");
                        complete(ServiceStatus.SUCCESS);
                        processorLog.type = 3;
                    }

                    @Override
                    public void processCommsResponse(Response resp) {
                        // we don't need this case in this test
                        processorLog.type = 4;
                    }
                };
            }
        };

        minimalEngineSetup(engineEventCallback, factory);

        // set the connection to be fine
        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        long nextRuntime = mContactSyncEngine.getNextRunTime();
        // should be equal to -1 because first time sync has not been yet
        // started
        assertEquals(-1, nextRuntime);

        // force a first time sync
        mContactSyncEngine.addUiStartFullSync();

        processorLog.type = 0;

        // start performing the sync
        mContactSyncEngine.run();

        // the first processor should have started
        assertTrue(processorLog.type == 2);

        // this will cancel any sync
        mContactSyncEngine.onReset();

        // get the engine to perform a cancel on the current processor
        mContactSyncEngine.run();

        assertTrue(processorLog.type == 1);

        // check that the engine cancelled the sync
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal(), uiEventCall.event);
        assertEquals(uiEventCall.status, ServiceStatus.USER_CANCELLED.ordinal());
    }

    // //////////////////////////////
    // HELPER METHODS AND CLASSES //
    // //////////////////////////////

    /**
     * Timeout value used during loops that may freeze.
     */
    private final static long TEST_TIMEOUT = 10000;

    /**
     * Tells whether or not a value is contained within an acceptable error
     * margin.
     * 
     * @return true if below the error margin, false if not
     */
    private boolean isValueInsideErrorMargin(long expected, long measured, int errorMargin) {

        long margin = expected * errorMargin / 100;
        return (Math.abs(expected - measured) < margin);
    }

    /**
     * Gets the database name via reflection feature.
     * 
     * @return the database name
     */
    private String getDatabaseName() {

        try {
            Field dbName = DatabaseHelper.class.getDeclaredField("DATABASE_NAME");
            dbName.setAccessible(true);
            return (String)dbName.get(null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "getDatabaseName(), error retrieving the database name... => " + e);
        }

        return null;
    }

    /**
     * Gets the Handler used by the ContactSyncEngine via reflection feature.
     * 
     * @param engine the ContactSyncEngine
     * @return the ContactSyncEngine Handler
     */
    private Handler getContactSyncEngineHandler(ContactSyncEngine engine) {

        try {
            Field handlerField = ContactSyncEngine.class.getDeclaredField("mDbChangeHandler");
            handlerField.setAccessible(true);
            Handler handler = (Handler)handlerField.get(engine);
            return handler;
        } catch (Exception e) {
            Log.e(LOG_TAG, "getDatabaseName(), error retrieving the database name... => " + e);
        }
        return null;
    }

    /**
     * Base class for tests using the test framework.
     */
    private static class ContactSyncFrameworkBase implements IEngineTestFrameworkObserver {

        protected ContactSyncEngine.State mState = ContactSyncEngine.State.IDLE;

        protected ContactSyncEngine mEngine;

        public void setContactSyncEngine(ContactSyncEngine engine) {
            mEngine = engine;
        }

        @Override
        public void onEngineException(Exception exp) {

        }

        @Override
        public void reportBackToEngine(int reqId, EngineId engine) {

        }
    }

    /**
     * Handles all the needed responses depending on the engine state.
     */
    private static class FirstTimeSyncFrameworkHandler extends ContactSyncFrameworkBase {

        public FirstTimeSyncFrameworkHandler() {
            mState = ContactSyncEngine.State.FETCHING_SERVER_CONTACTS;
        }

        @Override
        synchronized public void reportBackToEngine(int reqId, EngineId engine) {
            Log.d("TAG", "reportBackToEngine(), reqId=" + reqId + ", engine=" + engine);
            ResponseQueue respQueue = ResponseQueue.getInstance();
            List<BaseDataType> data = new ArrayList<BaseDataType>();

            switch (mState) {
                case FETCHING_SERVER_CONTACTS:
                    Log.d(LOG_TAG, "reportBackToEngine(): state=FETCHING_SERVER_CONTACTS");
                    // Set the next expected state
                    mState = ContactSyncEngine.State.FETCHING_NATIVE_CONTACTS;
                    // Give it an empty server contacts list
                    ContactChanges contactChanges = new ContactChanges();
                    contactChanges.mNumberOfPages = 0;
                    contactChanges.mVersionAnchor = 0;
                    data.add(contactChanges);
                    respQueue.addToResponseQueue(reqId, data, engine);
                    mEngine.onCommsInMessage();
                    break;
                case FETCHING_NATIVE_CONTACTS:
                    Log.d(LOG_TAG, "reportBackToEngine(): state=FETCHING_NATIVE_CONTACTS");
                    // Set the next expected state
                    mState = ContactSyncEngine.State.UPDATING_SERVER_CONTACTS;
                    break;
                case UPDATING_SERVER_CONTACTS:
                    ContactChanges serverContactChanges = new ContactChanges();
                    serverContactChanges.mNumberOfPages = 0;
                    serverContactChanges.mVersionAnchor = 0;
                    data.add(serverContactChanges);
                    respQueue.addToResponseQueue(reqId, data, engine);
                    mEngine.onCommsInMessage();
                    Log.d(LOG_TAG, "reportBackToEngine(): state=UPDATING_SERVER_CONTACTS");
                    break;
                case FETCHING_SERVER_THUMBNAILS:
                    Log.d(LOG_TAG, "reportBackToEngine(): state=FETCHING_SERVER_THUMBNAILS");
                    break;
                case UPDATING_SERVER_THUMBNAILS:
                    Log.d(LOG_TAG, "reportBackToEngine(): state=UPDATING_SERVER_THUMBNAILS");
                    break;
                case IDLE:
                    Log.d(LOG_TAG, "reportBackToEngine(): state=IDLE");
                    /*
                     * StatusMsg msg = new StatusMsg(); msg.mStatus = true;
                     * msg.mCode = "ok"; data.add(msg);
                     * respQueue.addToResponseQueue(reqId, data, engine);
                     * mEngine.onCommsInMessage();
                     */
                    break;
                default:
                    Log.d(LOG_TAG, "reportBackToEngine(): state=default");
            }
        }
    }

    /**
     * Dummy implementation of a Processor used by the ContactSyncEngine.
     */
    private static class DummySyncProcessor extends BaseSyncProcessor {

        protected DummySyncProcessor(IContactSyncCallback callback, DatabaseHelper db) {
            super(callback, db);

        }

        @Override
        protected void doCancel() {

            Log.d(LOG_TAG, "doCancel");
        }

        @Override
        protected void doStart() {

            Log.d(LOG_TAG, "doStart");
            complete(ServiceStatus.SUCCESS);
        }

        @Override
        public void processCommsResponse(Response resp) {

            Log.d(LOG_TAG, "processCommsResponse");
            complete(ServiceStatus.SUCCESS);
        }
    }

    /**
     * Class used to log the calls to the different methods of
     * IContactSyncObserver.
     */
    private static class ContactSyncObserver implements IContactSyncObserver {

        public static class ContactSyncStateChanged {
            Mode mode;

            State oldState;

            State newState;

            @Override
            public boolean equals(Object o) {

                if (o == null || !(o instanceof ContactSyncStateChanged))
                    return false;

                final ContactSyncStateChanged oContactSyncStateChanged = (ContactSyncStateChanged)o;
                return (oContactSyncStateChanged.mode == mode)
                        && (oContactSyncStateChanged.oldState == oldState)
                        && (oContactSyncStateChanged.newState == newState);
            }
        }

        public static class ProgressEvent {
            State currentState;

            int percent;

            @Override
            public boolean equals(Object o) {

                if (o == null || !(o instanceof ProgressEvent))
                    return false;

                final ProgressEvent oProgressEvent = (ProgressEvent)o;
                return (oProgressEvent.currentState == currentState)
                        && (oProgressEvent.percent == percent);
            }
        }

        public static class SyncComplete {
            ServiceStatus status;

            @Override
            public boolean equals(Object o) {

                if (o == null || !(o instanceof SyncComplete))
                    return false;

                final SyncComplete oSyncComplete = (SyncComplete)o;
                return oSyncComplete.status == status;
            }
        }

        public ArrayList<ContactSyncStateChanged> mCsscList = new ArrayList<ContactSyncStateChanged>();

        public ArrayList<ProgressEvent> mPeList = new ArrayList<ProgressEvent>();

        public ArrayList<SyncComplete> mScList = new ArrayList<SyncComplete>();

        /**
         * Resets all the logs.
         */
        public void reset() {
            mCsscList.clear();
            mPeList.clear();
            mScList.clear();
        }

        @Override
        public void onContactSyncStateChange(Mode mode, State oldState, State newState) {
            Log.d(LOG_TAG, "onContactSyncStateChange(" + mode + ", " + oldState + ", " + newState
                    + ")");

            ContactSyncStateChanged cssc = new ContactSyncStateChanged();
            cssc.mode = mode;
            cssc.oldState = oldState;
            cssc.newState = newState;

            mCsscList.add(cssc);
        }

        @Override
        public void onProgressEvent(State currentState, int percent) {
            Log.d(LOG_TAG, "onProgressEvent(" + currentState + ", " + percent + ")");

            ProgressEvent pe = new ProgressEvent();
            pe.currentState = currentState;
            pe.percent = percent;

            mPeList.add(pe);
        }

        @Override
        public void onSyncComplete(ServiceStatus status) {
            Log.d(LOG_TAG, "onSyncComplete(" + status + ")");

            SyncComplete sc = new SyncComplete();
            sc.status = status;

            mScList.add(sc);
        }
    }

    /**
     * Sets the expected calls during a first time sync to the provided lists.
     * 
     * @param cssc the list of expected calls to onContactSyncStateChanged
     * @param pe the list of expected calls to onProgressEvent
     * @param sc the list of expected calls to onSyncComplete
     */
    private void setupExpectedFirstTimeSyncObserverCalls(
            ArrayList<ContactSyncObserver.ContactSyncStateChanged> csscList,
            ArrayList<ContactSyncObserver.ProgressEvent> peList,
            ArrayList<ContactSyncObserver.SyncComplete> scList) {

        // setting up expected ContactSyncStateChanged
        ContactSyncObserver.ContactSyncStateChanged cssc = new ContactSyncObserver.ContactSyncStateChanged();
        cssc.mode = Mode.FULL_SYNC_FIRST_TIME;
        cssc.oldState = State.IDLE;
        cssc.newState = State.FETCHING_SERVER_CONTACTS;
        csscList.add(cssc);

        cssc = new ContactSyncObserver.ContactSyncStateChanged();
        cssc.mode = Mode.FULL_SYNC_FIRST_TIME;
        cssc.oldState = State.FETCHING_SERVER_CONTACTS;
        cssc.newState = State.FETCHING_NATIVE_CONTACTS;
        csscList.add(cssc);

        cssc = new ContactSyncObserver.ContactSyncStateChanged();
        cssc.mode = Mode.FULL_SYNC_FIRST_TIME;
        cssc.oldState = State.FETCHING_NATIVE_CONTACTS;
        cssc.newState = State.UPDATING_SERVER_CONTACTS;
        csscList.add(cssc);

        cssc = new ContactSyncObserver.ContactSyncStateChanged();
        cssc.mode = Mode.FULL_SYNC_FIRST_TIME;
        cssc.oldState = State.UPDATING_SERVER_CONTACTS;
        cssc.newState = State.IDLE;
        csscList.add(cssc);

        // setting up expected ProgressEvent
        ContactSyncObserver.ProgressEvent pe = new ContactSyncObserver.ProgressEvent();
        pe = new ContactSyncObserver.ProgressEvent();
        pe.currentState = State.FETCHING_SERVER_CONTACTS;
        pe.percent = 0;
        peList.add(pe);

        pe = new ContactSyncObserver.ProgressEvent();
        pe.currentState = State.FETCHING_SERVER_CONTACTS;
        pe.percent = 100;
        peList.add(pe);

        pe = new ContactSyncObserver.ProgressEvent();
        pe.currentState = State.FETCHING_NATIVE_CONTACTS;
        pe.percent = 0;
        peList.add(pe);

        pe = new ContactSyncObserver.ProgressEvent();
        pe.currentState = State.FETCHING_NATIVE_CONTACTS;
        pe.percent = 100;
        peList.add(pe);

        pe = new ContactSyncObserver.ProgressEvent();
        pe.currentState = State.UPDATING_SERVER_CONTACTS;
        pe.percent = 0;
        peList.add(pe);

        pe = new ContactSyncObserver.ProgressEvent();
        pe.currentState = State.UPDATING_SERVER_CONTACTS;
        pe.percent = 100;
        peList.add(pe);

        // setting up expected SyncComplete
        ContactSyncObserver.SyncComplete sc = new ContactSyncObserver.SyncComplete();
        sc.status = ServiceStatus.SUCCESS;
        scList.add(sc);
    }

    /**
     * Class holding parameters from a UI event.
     */
    private static class UiEventCall {
        int event;

        int request;

        int status;

        Object data;

        /**
         * Resets the values from the last UI event.
         */
        public void reset() {
            event = -1;
            request = -1;
            status = -1;
            data = null;
        }
    }

    /**
     * Class holding a started processor log.
     */
    private static class ProcessorLog {

        /**
         * The processor type.
         */
        int type;

        /**
         * The time when the processor was started.
         */
        long time;
    }
}
