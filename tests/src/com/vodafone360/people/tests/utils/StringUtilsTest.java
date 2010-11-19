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

package com.vodafone360.people.tests.utils;

import com.vodafone360.people.utils.StringUtils;

import junit.framework.TestCase;

/**
 * 
 * A class for executing tests on the StringUtils-class used for matching regular expressions
 * on strings. 
 *
 */
public class StringUtilsTest extends TestCase {
	private static final String VALID_EMAIL_REGEX = ".+@.+\\.[a-z]+";
	
	/**
	 * 
	 * Tests the regular expression matching method in StringUtils. 
	 * 
	 * NOTE: The tests in this method are heavily email specific which is used in the SignUp-
	 * validation. Once we have UI unit tests we should move these tests to the UI tests.
	 * 
	 */
	public void testMatchRegularExpression() {
		// TODO move this to a SignUpActivityTest as soon as we have UI Unit Tests
		
		boolean doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, "info@vodafone.com");
		assertTrue(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, "xxxxxxxxxxxxxxxxx");
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(null, "info@vodafone.com");
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, null);
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(null, "info@vodafone.com");
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, "info@.com");
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, "infovodafone.com");
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, "info@vodafonecom");
		assertFalse(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, 
				"i_n-f0123456789@vodafone1234567890_test-one.com");
		assertTrue(doesMatch);
		
		doesMatch = StringUtils.matchRegularExpression(VALID_EMAIL_REGEX, "info@123.123.123.123");
		assertFalse(doesMatch);
	}
}
