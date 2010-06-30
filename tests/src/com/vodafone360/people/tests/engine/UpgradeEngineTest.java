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

import android.test.AndroidTestCase;
import android.util.Log;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.upgrade.UpgradeEngine;
import com.vodafone360.people.engine.upgrade.UpgradeUtils;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;

/**
 * The UpgradeEngineTest is running unit tests on the UpgradeEngine class on its
 * own.
 */
public class UpgradeEngineTest extends AndroidTestCase implements IEngineEventCallback {

    private final static String TAG = "UpgradeEngineTest";

    /**
     * 
     */
    private final static String UPGRADE_URL_DUMMY = "http://dummy";

    /**
     * The UpgradeEngine instance used by each test.
     */
    private UpgradeEngine mUpgradeEngine;

    // private NetworkAgent mAgent;

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();

        SettingsManager.loadProperties(getContext());
        SettingsManager.setProperty(Settings.UPGRADE_CHECK_URL_KEY, UPGRADE_URL_DUMMY);
        // mAgent = new NetworkAgent(null, null, null);
        mUpgradeEngine = new UpgradeEngine(getContext(), this);
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();

        mUpgradeEngine = null;
    }

    // //////////////////
    // Tests methods //
    // //////////////////
    /**
     * Tests the engine initial states when the connection is lost.
     */
    public void testInitialStatesWithoutConnection() {

        Log.i(TAG, "testInitialStatesWithoutConnection() - begin");

        // set the connection to be lost

        NetworkAgent.setAgentState(AgentState.DISCONNECTED);

        // the engine is not connected, it shall return -1
        assertEquals(-1, mUpgradeEngine.getNextRunTime());

        // calling run() shall not change the value returned by getNextRunTime()
        mUpgradeEngine.run();
        assertEquals(-1, mUpgradeEngine.getNextRunTime());

        Log.i(TAG, "testInitialStatesWithoutConnection() - end");
    }

    /**
     * Tests the engine initial states when the connection is up.
     */
    public void testInitialStatesWithConnection() {

        Log.i(TAG, "testInitialStatesWithConnection() - begin");

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // the engine is connected, it shall return 0 as it will be executed
        // once
        assertEquals(0, mUpgradeEngine.getNextRunTime());

        Log.i(TAG, "testInitialStatesWithConnection() - end");
    }

    /**
     * Tests the engine with only one run.
     */
    public void testOneRun() {

        Log.i(TAG, "testOneRun() - begin");

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // the engine is connected, it shall return 0 as it will be executed
        // once
        assertEquals(0, mUpgradeEngine.getNextRunTime());

        // calling run() shall execute the engine
        mUpgradeEngine.run();

        Log.i(TAG, "testOneRun() - end");
    }

    /**
     * Tests when the engine will be run the next time.
     */
    public void testNextRuntime() {

        Log.i(TAG, "testNextRuntime() - begin");

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // the engine is connected, it shall return 0 as it will be executed
        // once
        assertEquals(0, mUpgradeEngine.getNextRunTime());

        // calling run() shall execute the engine
        long currentTime = System.currentTimeMillis();
        mUpgradeEngine.run();

        // check when the next runtime is supposed to happen
        long checkFrequency = UpgradeUtils.getCheckFrequency(getContext());

        Log.i(TAG, "testNextRuntime() - checkFrequency=" + checkFrequency);

        if (checkFrequency > 0) {
            // there is a programmed check, it shall be in between the
            // TIME_THRESHOLD_MS
            assertTrue((mUpgradeEngine.getNextRunTime() >= (currentTime + checkFrequency - TIME_THRESHOLD_MS / 2))
                    && (mUpgradeEngine.getNextRunTime() <= (currentTime + checkFrequency + TIME_THRESHOLD_MS / 2)));
        } else {
            // no more check for upgrade
            assertTrue(mUpgradeEngine.getNextRunTime() == -1);
        }

        Log.i(TAG, "testNextRuntime() - end");
    }

    /**
     * 
     */
    public void testAddingUiRequest() {

        Log.i(TAG, "testAddingUiRequest() - begin");

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // Push an upgrade request to the engine
        mUpgradeEngine.addUiRequestToQueue(ServiceUiRequest.UPGRADE_CHECK_NOW, null);

        // the engine shall return 0 as we are forcing an upgrade
        assertEquals(0, mUpgradeEngine.getNextRunTime());

        // run the engine
        mUpgradeEngine.run();

        // TODO: wait for onUiEvent() to be called
        // ...

        Log.i(TAG, "testAddingUiRequest() - end");
    }

    // //////////////////////////////
    // Helper methods and classes //
    // //////////////////////////////

    /**
     * The threshold that is used to verify the correctness of a time value.
     */
    private final static int TIME_THRESHOLD_MS = 5 * 1000;

    @Override
    public void kickWorkerThread() {
        // TODO Auto-generated method stub
        Log.e(TAG, "kickWorkerThread()");
    }

    @Override
    public void onUiEvent(ServiceUiRequest event, int arg1, int arg2, Object data) {
        // TODO Auto-generated method stub
        Log.e(TAG, "onUiEvent()");
    }

    @Override
    public UiAgent getUiAgent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ApplicationCache getApplicationCache() {
        // TODO Auto-generated method stub
        return null;
    }
}