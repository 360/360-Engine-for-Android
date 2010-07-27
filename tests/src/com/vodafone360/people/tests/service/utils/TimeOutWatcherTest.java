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

package com.vodafone360.people.tests.service.utils;

import android.app.Instrumentation;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.Request.Type;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.utils.TimeOutWatcher;

/**
 * JUnit tests for the TimeOutWatcher class.
 */
public class TimeOutWatcherTest extends InstrumentationTestCase {

    private final static long TIMEOUT_2000_MS = 2000;
    private final static long TIMEOUT_10000_MS = 10000;
    
    private TimeOutWatcher mWatcher = null;
    private MainApplication mApplication;
    
    protected void setUp() throws Exception {
        super.setUp();
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class, getInstrumentation().getTargetContext());
        mApplication.onCreate();
        mWatcher = new TimeOutWatcher();
    }

    protected void tearDown() throws Exception {
        mWatcher.kill();
        super.tearDown();
    }
    
    /**
     * Tests adding and removing requests with different cases:
     * -same expiry dates
     * -increasing dates
     * -decreasing dates
     */
    public void testAddingAndRemovingRequests() {

        Log.i("testAddingAndRemovingRequests()", "-begin");
        
        Request[] requests = new Request[10];

        assertEquals(0, mWatcher.getRequestsCount());
        
        // requests with same expiry date
        Log.i("testAddingAndRemovingRequests()", "-checking requests with same expiry date");
        for(int i = 0; i < requests.length; i++) {
            requests[i] = createRequestWithTimeout(TIMEOUT_2000_MS);
            requests[i].calculateExpiryDate();
            mWatcher.addRequest(requests[i]);
        }
        
        assertEquals(requests.length, mWatcher.getRequestsCount());
        assertTrue(compareRequestArraysIdentical(requests, mWatcher.getRequestsArray()));

        for(int i = 0; i < requests.length; i++) {
            mWatcher.removeRequest(requests[i]);
        }
        
        assertEquals(0, mWatcher.getRequestsCount());
        
        Log.i("testAddingAndRemovingRequests()", "-checking requests that don't wake up the thread each time they are added (but wake up the thread when removed with the same order)");
        for(int i = 0; i < requests.length; i++) {
            requests[i] = createRequestWithTimeout(TIMEOUT_10000_MS * (i+1));
            requests[i].calculateExpiryDate();
            mWatcher.addRequest(requests[i]);
        }
        assertEquals(requests.length, mWatcher.getRequestsCount());
        assertTrue(compareRequestArraysIdentical(requests, mWatcher.getRequestsArray()));
        
        for(int i = 0; i < requests.length; i++) {
            mWatcher.removeRequest(requests[i]);
        }
        assertEquals(0, mWatcher.getRequestsCount());
        
        Log.i("testAddingAndRemovingRequests()", "-checking requests that wake up the thread each time they are added (but don't wake up the thread when removed with the same order");
        for(int i = 0; i < requests.length; i++) {
            requests[i] = createRequestWithTimeout(TIMEOUT_10000_MS * (requests.length-i));
            requests[i].calculateExpiryDate();
            mWatcher.addRequest(requests[i]);
        }
        
        assertEquals(requests.length, mWatcher.getRequestsCount());
        assertTrue(compareRequestArraysSameContent(requests, mWatcher.getRequestsArray()));
        
        for(int i = 0; i < requests.length; i++) {
            mWatcher.removeRequest(requests[i]);
        }
        assertEquals(0, mWatcher.getRequestsCount());
        
        Log.i("testAddingAndRemovingRequests()", "-end");
    }
    
    /**
     * Tests the invalidateAllRequests() method.
     */
    @Suppress
    public void testTimingOutAllRequests() {
        Log.i("testTimingOutAllRequests()", "-begin");
        
        Request[] requests = new Request[10];

        assertEquals(0, QueueManager.getInstance().getRequestTimeoutWatcher().getRequestsCount());

        for(int i = 0; i < requests.length; i++) {
            requests[i] = createRequestWithTimeout(TIMEOUT_2000_MS);
            QueueManager.getInstance().addRequest(requests[i]);
        }
        
        assertEquals(requests.length, QueueManager.getInstance().getRequestTimeoutWatcher().getRequestsCount());
        assertTrue(compareRequestArraysIdentical(requests, QueueManager.getInstance().getRequestTimeoutWatcher().getRequestsArray()));

        QueueManager.getInstance().getRequestTimeoutWatcher().invalidateAllRequests();
        
        assertEquals(0, QueueManager.getInstance().getRequestTimeoutWatcher().getRequestsCount());
        Log.i("testTimingOutAllRequests()", "-end");
    }
    
    /**
     * Tests that when requests are added, they are stored in an ascending ordered list. 
     */
    public void testRequestsAreSorted() {
        Log.i("testRequestsAreSorted()", "-begin");
        
        assertEquals(0, mWatcher.getRequestsCount());
        
        final int[] order = { 7, 10, 9, 6, 2, 8, 1, 5, 3, 4 };
        
        // add the requests with following timeout order: 7, 10, 9, 6, 2, 8, 1, 5, 3, 4
        for (int i = 0; i < order.length; i++) {
            final Request request = createRequestWithTimeout(TIMEOUT_10000_MS * order[i]);
            request.calculateExpiryDate();
            mWatcher.addRequest(request);
        }
        
        final Request[] requests = mWatcher.getRequestsArray();
        assertEquals(order.length, requests.length);
        // check that the requests are given back in the ascending order: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        for (int i = 0; i < requests.length; i++) {
            final Request request = requests[i];
            assertEquals((i+1), request.getTimeout() / TIMEOUT_10000_MS);
        }
        
        Log.i("testRequestsAreSorted()", "-end");
    }
    
    
    /**
     * Tests that a timeout error is thrown if the response of the request has not been received after the timeout period. 
     */
    @Suppress
    public void testThrowingTimeout() {
        
        Log.i("testThrowingTimeout()", "-begin");
        
        final Request request = createRequestWithTimeout(TIMEOUT_2000_MS);
        
        // check that the response queue is empty for the engine with EngineId.UNDEFINED id (we use this one for the test)
        DecodedResponse response = ResponseQueue.getInstance().getNextResponse(EngineId.UNDEFINED);
        assertNull(response);
        
        // adding the request to the queue should add it to the TimeOutWatcher
        final int reqId = QueueManager.getInstance().addRequest(request);
        
        // check that the response queue is still empty 
        response = ResponseQueue.getInstance().getNextResponse(EngineId.UNDEFINED);
        assertNull(response);

        // let's give at least 2 times the timeout to the system before checking
        long stopTime = System.currentTimeMillis() + (TIMEOUT_2000_MS * 2);
        while (System.currentTimeMillis() < stopTime) {
            try {
                Thread.sleep(TIMEOUT_2000_MS);
            }
            catch(InterruptedException ie) {
                Log.i("testThrowingTimeout()", "Error while sleeping: "+ie);
            }
        }
        
        // check that the response is still empty 
        response = ResponseQueue.getInstance().getNextResponse(EngineId.UNDEFINED);
        assertNotNull(response);
        
        // check response request id is the same as the request id
        assertEquals(reqId, response.mReqId.intValue());
        // check the timeout error returned is as expected
        assertNotNull(response.mDataTypes);
        assertEquals(1, response.mDataTypes.size());
        BaseDataType error = response.mDataTypes.get(0);
        assertTrue(error instanceof ServerError);
        ServerError srvError = (ServerError)error;
        assertEquals(ServerError.ErrorType.REQUEST_TIMEOUT, srvError.getType());
        Log.i("testThrowingTimeout()", "-end");
    }
    
    ////////////////////
    // HELPER METHODS //
    ////////////////////
    
    /**
     * Creates a request for the "undefined" engine.
     * @param timeout the timeout for the request in milliseconds
     */
    private Request createRequestWithTimeout(long timeout) {
        final Request request = new Request("", Type.PRESENCE_LIST, EngineId.UNDEFINED, false, timeout);
        request.setActive(true);
        
        return request;
    }
    
    /**
     * Checks if two arrays are exactly the same.
     * @param array1 the first array to compare
     * @param array2 the second array to compare
     * @return true if identical, false otherwise
     */
    private boolean compareRequestArraysIdentical(Request[] array1, Request[] array2) {
        if (array1 == null || array2 == null) {
            throw new NullPointerException();
        }
        if (array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if two arrays have the same content but not necessarily in the same order.
     * @param array1 the first array to compare
     * @param array2 the second array to compare
     * @return true if both arrays have the same content, false otherwise
     */
    private boolean compareRequestArraysSameContent(Request[] array1, Request[] array2) {
        if (array1 == null || array2 == null) {
            throw new NullPointerException();
        }
        if (array1.length != array2.length) {
            return false;
        }
        
        int count;
        for (int i = 0; i < array1.length; i++) {
            count = 0;
            for (int j = 0; j < array1.length; j++) {
                if (array1[i] == array2[j]) {
                    count++;
                    // more than one
                    if (count > 1) return false;
                }
            }
            // none
            if (count == 0) return false;
        }
        
        // arrays contain the same requests
        return true;
    }
}
