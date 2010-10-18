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

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.vodafone360.people.utils.MathUtils;

import junit.framework.TestCase;

/**
 * 
 * A test case class testing the MathUtils-class.
 *
 */
public class MathUtilsTest extends TestCase {
	
	/**
	 * 
	 * Tests the getValidatedBirthdate-method.
	 * 
	 */
	public void testGetValidatedBirthdate() {
		// we set a date older than todays date. this should return the old date instead of today's
		GregorianCalendar cal = MathUtils.getValidatedBirthdate(1970, Calendar.APRIL, 5);
		assertEquals(1970, cal.get(Calendar.YEAR));
		assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH));
		assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
		cal = null;
		
		// we set a date younger than today's. this should result in today's date being returned
		Calendar today = Calendar.getInstance();
		cal = MathUtils.getValidatedBirthdate(today.get(Calendar.YEAR) + 200, Calendar.NOVEMBER, 5);
		assertEquals(today.get(Calendar.YEAR), cal.get(Calendar.YEAR));
		assertEquals(today.get(Calendar.MONTH), cal.get(Calendar.MONTH));
		assertEquals(today.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));
		cal = null;
		
		// we set the day in the month may to the 32nd. this should result in June 1st.
		cal = MathUtils.getValidatedBirthdate(1970, Calendar.MAY, 32);
		assertEquals(1970, cal.get(Calendar.YEAR));
		assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
		assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
	}
}
