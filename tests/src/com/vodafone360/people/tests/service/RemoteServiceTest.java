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

import java.security.InvalidParameterException;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.MainApplication;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.WorkerThread;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.interfaces.IPeopleService;
import com.vodafone360.people.service.interfaces.IWorkerThreadControl;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.IWakeupListener;
import com.vodafone360.people.service.transport.http.authentication.AuthenticationManager;
import com.vodafone360.people.tests.testutils.FrameworkUtils;
import com.vodafone360.people.tests.testutils.TestStatus;

/***
 * Test the RemoveService class.
 */
@Suppress
public class RemoteServiceTest extends ServiceTestCase<RemoteService> {

    /** Instance of the Main Application. **/
    private MainApplication mApplication = null;

    /*
     * ##########  FRAMEWORK CLASSES.  ##########
     */
    /***
     * Test cases constructor.
     */
    public RemoteServiceTest() {
        super(RemoteService.class);
    }

    /**
     * Load and start a real MainApplication instance using the current
     * context, as if it was started by the system.
     *
     * @throws Exception Anything related to starting the MainApplication
     *         class.
     */
    @Override
    protected final void setUp() throws Exception {
        /***
         * What seems to happen is this:
         * - We setup the MainApplication manually and get its context in the TestRunner thread.
         * - Somehow, a separate "main" thread gets called and creates another MainApplicaiton.
         * - Both Applications create the same singletons (I.e. native contacts API).
         */

        /** Setup the MainApplication. **/
        mApplication = (MainApplication) Instrumentation.newApplication(MainApplication.class,
                getContext());
        assertNotNull("Newly created MainApplication class should not be NULL", mApplication);
        mApplication.onCreate();
        setApplication(mApplication);

        super.setUp();
    }

    /**
     * Shuts down the Application under test.  Also makes sure all resources
     * are cleaned up and garbage collected before moving on to the next test.
     * Subclasses that override this method should make sure they call
     * super.tearDown() at the end of the overriding method.
     *
     * @throws Exception Cannot terminate the application.
     */
    @Override
    protected final void tearDown() throws Exception {
        if (mApplication != null) {
            mApplication.onTerminate();
            mApplication = null;
        }
        setApplication(null);

        super.tearDown();
    }

    /***
     * Pause the thread for the given amount of time.
     *
     * @param time Time in milliseconds to pause the current thread.
     */
    private synchronized void pause(final long time) {
        try {
            wait(time);
        } catch (InterruptedException e) {
            // Do nothing.
        }
    }
    /*
     * ##########  END FRAMEWORK CLASSES.  ##########
     */


    /**
     * Test basic startup/shutdown of Service.
     */
    @MediumTest
    public final void testStartable() {
        startService(new Intent(getContext(), RemoteService.class));
        assertNotNull("RemoteService should not be NULL", getService());
    }

    /**
     * Test starting the service with the ALARM_WORKER_THREAD Intent.
     *
     * @throws Exception Issue setting up framework.
     */
    @MediumTest
    public final void testStartAlarmWorkerThread() throws Exception {
        /** Setup test preconditions (i.e. WorkerThread not running). **/
        startService(new Intent(getContext(), RemoteService.class));
        RemoteService remoteService = getService();
        assertNotNull("RemoteService should not be NULL", remoteService);
        FrameworkUtils.set(remoteService, "mIsStarted", true);
        FrameworkUtils.set(remoteService, "mWorkerThread", null);

        /** Perform test (i.e. trigger ALARM_WORKER_THREAD). **/
        Intent alarmWorkerThreadIntent = new Intent(getContext(), RemoteService.class);
        alarmWorkerThreadIntent.putExtra(RemoteService.ALARM_KEY,
                WorkerThread.ALARM_WORKER_THREAD);
        remoteService.onStart(alarmWorkerThreadIntent, -1);

        /** Test if the kickWorkerThread() was called. **/
        WorkerThread workerThread = (WorkerThread) FrameworkUtils.get(remoteService,
                "mWorkerThread");
        assertNotNull("Expecting workerThread not to be NULL", workerThread);
        assertTrue("Expecting workerThread to be alive",
                workerThread.isAlive());
    }

    /**
     * Test starting the service with the ALARM_HB_THREAD Intent.
     *
     * @throws Exception Issue setting up the service.
     */
    @MediumTest
    public final void testStartAlarmHbThread() throws Exception {
        /** Setup test preconditions (i.e. WorkerThread not running). **/
        final TestStatus testStatus = new TestStatus();
        setupService();
        RemoteService remoteService = getService();
        remoteService.registerCpuWakeupListener(new IWakeupListener() {
            @Override
            public void notifyOfWakeupAlarm() {
                /*
                 * Test that the dummy mWakeListener.notifyOfWakeupAlarm() has
                 * been called, otherwise the test must fail.
                 */
                testStatus.setPass(true);
            }
        });

        /** Perform test (i.e. trigger ALARM_HB_THREAD). **/
        Intent alarmWorkerThreadIntent
            = new Intent(getContext(), RemoteService.class);
        alarmWorkerThreadIntent.putExtra(RemoteService.ALARM_KEY,
                IWakeupListener.ALARM_HB_THREAD);
        startService(alarmWorkerThreadIntent);

        /** Test if notifyOfWakeupAlarm() was called. **/
        assertTrue("Expecting the notifyOfWakeupAlarm() dummy method to have "
                + "been called", testStatus.isPass());
    }

    /**
     * Test getServiceInterface() from RemoteService.
     *
     * @throws Exception Issue setting up the service.
     */
    @MediumTest
    public final void testGetServiceInterface() throws Exception {
        /** Setup test preconditions (i.e. startService). **/
        startService(new Intent(getContext(), RemoteService.class));

        /** Perform test (i.e. getServiceInterface). **/
        IPeopleService mPeopleService = mApplication.getServiceInterface();

        /** Test if mPeopleService is correctly initialised. **/
        assertNotNull("IPeopleService should not be NULL", mPeopleService);
        IWorkerThreadControl workerThreadControl =
            (IWorkerThreadControl) FrameworkUtils.get(mPeopleService, "mWorkerThreadControl");
        assertNotNull("Expecting mWorkerThreadControl not to be NULL",
                workerThreadControl);
        RemoteService service = (RemoteService) FrameworkUtils.get(mPeopleService, "mService");
        assertNotNull("Expecting mService not to be NULL", service);
        UiAgent handlerAgent = (UiAgent) FrameworkUtils.get(mPeopleService, "mHandlerAgent");
        assertNotNull("Expecting mHandlerAgent not to be NULL", handlerAgent);
        ApplicationCache applicationCache =
            (ApplicationCache) FrameworkUtils.get(mPeopleService, "mApplicationCache");
        assertNotNull("Expecting mApplicationCache not to be NULL",
                applicationCache);
    }

    /**
     * Test binding to service with a basic Intent.
     */
    @MediumTest
    public final void testBasicBindable() {
        IBinder service = null;
        try {
            service = bindService(new Intent(getContext(),
                    RemoteService.class));
            fail("Expecting an InvalidParameterException for a bindService "
                    + "without an explicit action.");

        } catch (InvalidParameterException e) {
            assertTrue("InvalidParameterException expected", true);
        }

        assertNull("Expecting the RemoteService to return a NULL interface "
                + "when started with a basic intent.", service);
    }

    /**
     * Test binding to service with an Authenticator Intent.
     *
     * @throws RemoteException Problem getting the remote object.
     */
    @MediumTest
    public final void testAuthenticatorBindable() throws RemoteException {
        Intent authenticatorIntent = new Intent(getContext(),
                RemoteService.class);
        authenticatorIntent.setAction(
                RemoteService.ACTION_AUTHENTICATOR_INTENT);
        IBinder service = bindService(authenticatorIntent);

        assertNotNull("Expecting the RemoteService not to return a NULL "
                + "interface when started with a ACTION_AUTHENTICATOR_INTENT "
                + "intent.", service);

        assertEquals("Expecting the RemoteService to return an "
                + "AbstractAccountAuthenticator$Transport class when started "
                + "with a ACTION_AUTHENTICATOR_INTENT intent.",
                "android.accounts.AbstractAccountAuthenticator$Transport",
                service.getClass().getName());

        assertEquals("Expecting the RemoteService to return an"
                + "IAccountAuthenticator class when started with a "
                + "ACTION_AUTHENTICATOR_INTENT intent.",
                "android.accounts.IAccountAuthenticator",
                service.getInterfaceDescriptor());

        assertTrue("Expected the Binder to be alive", service.isBinderAlive());
    }

    /**
     * Test binding to service with a basic Intent.
     *
     * @throws RemoteException Problem getting the remote object.
     */
    @MediumTest
    public final void testSyncAdapterBindable() throws RemoteException {
        Intent syncAdapterIntent = new Intent(getContext(),
                RemoteService.class);
        syncAdapterIntent.setAction(RemoteService.ACTION_SYNC_ADAPTER_INTENT);
        IBinder service = bindService(syncAdapterIntent);

        assertNotNull("Expecting the RemoteService not to return a NULL "
                + "interface when started with a ACTION_SYNC_ADAPTER_INTENT "
                + "intent.", service);

        assertEquals("Expecting the RemoteService to return an "
                + "AbstractThreadedSyncAdapter$ISyncAdapterImpl class when "
                + "started with a ACTION_SYNC_ADAPTER_INTENT intent.",
                "android.content.AbstractThreadedSyncAdapter$ISyncAdapterImpl",
                service.getClass().getName());

        assertEquals("Expecting the RemoteService to return an"
                + "ISyncAdapter class when started with a "
                + "ACTION_SYNC_ADAPTER_INTENT intent.",
                "android.content.ISyncAdapter",
                service.getInterfaceDescriptor());

        assertTrue("Expected the Binder to be alive", service.isBinderAlive());
    }

    /**
     * Test kickWorkerThread() from RemoteService.
     *
     * @throws Exception Issue setting up framework.
     */
    @MediumTest
    public final void testKickWorkerThread() throws Exception {
        /** Setup test preconditions (i.e. WorkerThread not running). **/
        startService(new Intent(getContext(), RemoteService.class));
        RemoteService remoteService = getService();
        assertNotNull("RemoteService should not be NULL", remoteService);
        FrameworkUtils.set(remoteService, "mIsStarted", false);
        FrameworkUtils.set(remoteService, "mWorkerThread", null);

        /** Perform test (i.e. trigger ALARM_WORKER_THREAD). **/
        remoteService.kickWorkerThread();

        /**
         * Test if kickWorkerThread() returned because mIsStarted was FALSE.
         */
        WorkerThread workerThread = (WorkerThread)FrameworkUtils.get(remoteService,
                "mWorkerThread");
        assertNull("Expecting workerThread to be NULL", workerThread);

        /** Setup test preconditions (i.e. WorkerThread not running). **/
        FrameworkUtils.set(remoteService, "mIsStarted", true);
        FrameworkUtils.set(remoteService, "mWorkerThread", null);

        /** Perform test (i.e. trigger ALARM_WORKER_THREAD). **/
        remoteService.kickWorkerThread();

        /** Test if kickWorkerThread() created a new WorkerThread. **/
        workerThread = (WorkerThread)FrameworkUtils.get(remoteService, "mWorkerThread");
        assertNotNull("Expecting workerThread not to be NULL", workerThread);
        assertTrue("Expecting workerThread to be alive", workerThread.isAlive());
    }

    /**
     * Test signalConnectionManager() from RemoteService.
     *
     * @throws Exception Any kind of mapping exception
     */
    @MediumTest
    public final void testSignalConnectionManager() throws Exception {
        /** Setup test preconditions (i.e. remoteService running). **/
        startService(new Intent(getContext(), RemoteService.class));
        RemoteService remoteService = getService();
        assertNotNull("RemoteService should not be NULL", remoteService);
        assertTrue("Expected mIsConnected to be TRUE",
                (Boolean) FrameworkUtils.get(remoteService, "mIsConnected"));
        assertNotNull("Expected mDecoder to be NULL",
                FrameworkUtils.get(ConnectionManager.getInstance(), "mDecoder"));
        assertNotNull("Expected mConnection to be NULL",
                FrameworkUtils.get(ConnectionManager.getInstance(), "mConnection"));
        assertTrue("Expected mDecoder to be running",
                ((DecoderThread) FrameworkUtils.get(ConnectionManager.getInstance(),
                        "mDecoder")).getIsRunning());
        assertEquals("Expected mConnection to be AuthenticationManager class",
                AuthenticationManager.class, FrameworkUtils.get(ConnectionManager.getInstance(),
                        "mConnection").getClass());

        /** Perform test (i.e. disconnect). **/
        remoteService.signalConnectionManager(false);

        /**
         * Test if signalConnectionManager() called
         * ConnectionManager.disconnect() and set mIsConnected to false.
         */
        assertFalse("Expecting mIsConnected to be FALSE",
                (Boolean) FrameworkUtils.get(remoteService, "mIsConnected"));
        assertNotNull("Expected mDecoder not to be NULL",
                FrameworkUtils.get(ConnectionManager.getInstance(), "mDecoder"));
        assertNotNull("Expected mConnection not to be NULL",
                FrameworkUtils.get(ConnectionManager.getInstance(), "mConnection"));
        assertFalse("Expected mDecoder not to be running",
                ((DecoderThread) FrameworkUtils.get(ConnectionManager.getInstance(),
                        "mDecoder")).getIsRunning());
        FrameworkUtils.set(remoteService, "mWorkerThread", null);

        /** Perform second test (i.e. connect). **/
        remoteService.signalConnectionManager(true);

        /**
         * Test if signalConnectionManager() called
         * ConnectionManager.connect(), kickWorkerThread() and set mIsConnected
         * to true.
         */
        assertTrue("Expecting mIsConnected to be TRUE",
                (Boolean) FrameworkUtils.get(remoteService, "mIsConnected"));
        assertNotNull("Expected mDecoder not to be NULL",
                FrameworkUtils.get(ConnectionManager.getInstance(), "mDecoder"));
        assertTrue("Expected mDecoder to be running",
                ((DecoderThread) FrameworkUtils.get(ConnectionManager.getInstance(),
                        "mDecoder")).getIsRunning());
        assertNotNull("Expecting workerThread not to be NULL",
                FrameworkUtils.get(remoteService, "mWorkerThread"));
    }

    /**
     * Test getNetworkAgent() from RemoteService.
     */
    @MediumTest
    public final void testGetNetworkAgent() {
        /** Setup test preconditions (i.e. remoteService running). **/
        startService(new Intent(getContext(), RemoteService.class));
        RemoteService remoteService = getService();
        assertNotNull("RemoteService should not be NULL", remoteService);

        /** Perform test (i.e. disconnect). **/
        NetworkAgent networkAgent = remoteService.getNetworkAgent();

        /** Test NetworkAgent object. **/
        assertNotNull("NetworkAgent should not be NULL", networkAgent);
        assertEquals("NetworkAgent RoamingDeviceSetting should be FALSE",
                false, networkAgent.getRoamingDeviceSetting());
        assertEquals("NetworkAgent RoamingNotificationType should be ROAMING_DIALOG_GLOBAL_OFF",
                NetworkAgent.ROAMING_DIALOG_GLOBAL_OFF,
                networkAgent.getRoamingNotificationType());
        assertEquals("NetworkAgent AgentState should be CONNECTED",
                AgentState.CONNECTED, NetworkAgent.getAgentState());
    }

    /**
     * Test setAlarm() from RemoteService.
     */
    @MediumTest
    public final void testSetAlarm() {
        /** Setup test preconditions (i.e. remoteService running). **/
        startService(new Intent(getContext(), RemoteService.class));
        RemoteService remoteService = getService();

        /** Set an alarm that will never go off. **/
        remoteService.setAlarm(true, System.currentTimeMillis() + 10000);

        /** Cancel alarm before it goes off. **/
        remoteService.setAlarm(false, System.currentTimeMillis() + 10000);

        /** Set alarm that will go off now. **/
        remoteService.setAlarm(true, 0);

        /** Allow RemoteService to be created. **/
        pause(1000);

        /**
         * Warning: Unable to test if the alarm actually goes off, as the
         * AlarmManager creates a new apparently unreachable instance of the
         * RemoteService.
         */
    }
}