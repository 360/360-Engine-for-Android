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

package com.vodafone360.people.tests.ui.utils;

import com.vodafone360.people.ui.utils.SimNetwork;
import com.vodafone360.people.utils.LogUtils;

import junit.framework.TestCase;

public class SimNetworkTest extends TestCase {

	public SimNetworkTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
		
	public void testGetNetwork() {

		assertEquals("NULL country didn't return SimNetwork.UNKNOWN", SimNetwork.UNKNOWN, SimNetwork.getNetwork(null));
		assertEquals("Non existing country didn't return SimNetwork.UNKNOWN",SimNetwork.UNKNOWN, SimNetwork.getNetwork("non existing country"));
		assertEquals("Empty country didn't return SimNetwork.UNKNOWN", SimNetwork.UNKNOWN, SimNetwork.getNetwork(""));
		assertEquals("Blank country didn't return SimNetwork.UNKNOWN", SimNetwork.UNKNOWN, SimNetwork.getNetwork(" "));
		
		LogUtils.logV("CORRUPTED DATA TESTS FINISHED");
		
		assertEquals(SimNetwork.CH, SimNetwork.getNetwork(SimNetwork.CH.iso()));
		assertEquals(SimNetwork.DE, SimNetwork.getNetwork(SimNetwork.DE.iso()));
		assertEquals(SimNetwork.FR, SimNetwork.getNetwork(SimNetwork.FR.iso()));
		assertEquals(SimNetwork.GB, SimNetwork.getNetwork(SimNetwork.GB.iso()));
		assertEquals(SimNetwork.IE, SimNetwork.getNetwork(SimNetwork.IE.iso()));
		assertEquals(SimNetwork.IT, SimNetwork.getNetwork(SimNetwork.IT.iso()));
		assertEquals(SimNetwork.NL, SimNetwork.getNetwork(SimNetwork.NL.iso()));
		assertEquals(SimNetwork.SE, SimNetwork.getNetwork(SimNetwork.SE.iso()));
		assertEquals(SimNetwork.TR, SimNetwork.getNetwork(SimNetwork.TR.iso()));
		assertEquals(SimNetwork.TW, SimNetwork.getNetwork(SimNetwork.TW.iso()));
		assertEquals(SimNetwork.US, SimNetwork.getNetwork(SimNetwork.US.iso()));
		assertEquals(SimNetwork.ES, SimNetwork.getNetwork(SimNetwork.ES.iso()));
		
		LogUtils.logV("SETTINGS DATA TESTS FINISHED");
	}
}