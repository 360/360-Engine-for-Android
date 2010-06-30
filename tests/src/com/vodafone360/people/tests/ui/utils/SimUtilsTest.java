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

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.vodafone360.people.ui.utils.SimUtils;

public class SimUtilsTest extends TestCase {

	@SmallTest
	public void testGetVerifyedMsisdn() throws Exception {
		assertEquals("", SimUtils.getVerifyedMsisdn(null, null, null, null));
		assertEquals("", SimUtils.getVerifyedMsisdn("", null, null, null));
		assertEquals("", SimUtils.getVerifyedMsisdn("1234567890", null, null, null));
		assertEquals("+1234567890", SimUtils.getVerifyedMsisdn("+1234567890", null, null, null));
		assertEquals("+4900000000", SimUtils.getVerifyedMsisdn("004900000000", null, null, null));
		
		//Real data tests
		assertEquals("", SimUtils.getVerifyedMsisdn("", "de", "Vodafone.de", "26202"));
		assertEquals("", SimUtils.getVerifyedMsisdn("", "au", "Vodafone.au", "26202"));
		assertEquals("+41700000000", SimUtils.getVerifyedMsisdn("+41700000000", "ch", "Swisscom", "22801"));
		assertEquals("+491720000000", SimUtils.getVerifyedMsisdn("01720000000", "de", "Vodafone.de", "26202"));
		assertEquals("+447960000000", SimUtils.getVerifyedMsisdn("07960000000", "gb", "Orange", "23433"));
		assertEquals("+31600000000", SimUtils.getVerifyedMsisdn("+31600000000", "nl", "vodafone NL", "20404"));
		assertEquals("+46700000000", SimUtils.getVerifyedMsisdn("+46700000000", "se", "Sweden3G", "24005"));
		assertEquals("+886988000000", SimUtils.getVerifyedMsisdn("0988000000", "tw", "????", "46692"));
		assertEquals("+15550000000", SimUtils.getVerifyedMsisdn("15550000000", "us", "Android", "310260")); //Emulator
		assertEquals("+15500000000", SimUtils.getVerifyedMsisdn("+15500000000", "us", "Android", "310260")); //Emulator
	}

	@SmallTest
	public void testGetAnonymisedMsisdn() throws Exception {
		assertEquals("", SimUtils.getAnonymisedMsisdn(null));
		assertEquals("", SimUtils.getAnonymisedMsisdn(""));
		assertEquals("1", SimUtils.getAnonymisedMsisdn("1"));
		assertEquals("12", SimUtils.getAnonymisedMsisdn("12"));
		assertEquals("123", SimUtils.getAnonymisedMsisdn("123"));
		assertEquals("1234", SimUtils.getAnonymisedMsisdn("1234"));
		assertEquals("12340", SimUtils.getAnonymisedMsisdn("12345"));
		assertEquals("1234000000", SimUtils.getAnonymisedMsisdn("1234567890"));
		assertEquals("+1230000000", SimUtils.getAnonymisedMsisdn("+1234567890"));
	}
}