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

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.vodafone360.people.service.SyncAdapter;

/***
 * Test the SyncAdapter class.
 */
public class SyncAdapterTest extends TestCase {

    /***
     * Test the SyncAdapter constructor.
     */
    @SmallTest
    public final void testSyncAdapter() {
        SyncAdapter syncAdapter = new SyncAdapter(null, true);
        assertNotNull("SyncAdapter should not be NULL", syncAdapter);
    }

    /***
     * Test the onPerformSync() method.
     */
    @SmallTest
    public final void testOnPerformSync() {
        SyncAdapter syncAdapter = new SyncAdapter(null, true);
        syncAdapter.onPerformSync(null, null, null, null, null);
        assertTrue("No errors so far", true);
    }

    /***
     * Test the onContactSyncStateChange() method.
     */
    @SmallTest
    public final void testOnContactSyncStateChange() {
        SyncAdapter syncAdapter = new SyncAdapter(null, true);
        syncAdapter.onContactSyncStateChange(null, null, null);
        assertTrue("No errors so far", true);
    }

    /***
     * Test the onProgressEvent() method.
     */
    @SmallTest
    public final void testOnProgressEvent() {
        SyncAdapter syncAdapter = new SyncAdapter(null, true);
        syncAdapter.onProgressEvent(null, 0);
        assertTrue("No errors so far", true);
    }

    /***
     * Test the onSyncComplete() method.
     */
    @SmallTest
    public final void testOnSyncComplete() {
        SyncAdapter syncAdapter = new SyncAdapter(null, true);
        syncAdapter.onSyncComplete(null);
        assertTrue("No errors so far", true);
    }
}