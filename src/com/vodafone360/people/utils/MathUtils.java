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

package com.vodafone360.people.utils;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * 
 * A utility class containing math functions e.g. for validating dates or converting bytes to an 
 * integer.
 *
 */
public class MathUtils {
	/**
	 * The smallest birth date that can be chosen in Android as defined in the DatePicker.java is
	 * 1900. The DateUtils in Android apparently have a bug that mixes up dates before 1916. Please
	 * also take a look at the MathUtilsTest.java for this bug.
	 * (check Android source code: http://tinyurl.com/39ppcfc).
	 */
	private static final int MINIMUM_BIRTH_DATE = 1916;
	
	/**	The month January as a number representation. */
	private static final int JANUARY = 1;
	
	/** The first day of the month. */
	private static final int FIRST_DAY_OF_MONTH = 1;
	
	
	/**
	 * 
	 * Shifts 4 signed bytes into a 32 bit integer in network order (big endian).
	 * 
	 * @param b1 The first byte that will be shifted by 24 bits.
	 * @param b2 The second byte that will be shifted by 16 bits.
	 * @param b3 The third byte that will be shifted by 8 bits.
	 * @param b4 The fourth byte that will remain at its position.
	 * 
	 * @return An integer containing the 4 bytes shifted in network order.
	 * 
	 */
    public static int convertBytesToInt(final byte b1, final byte b2, 
    									final byte b3, final byte b4) {
        int i = 0;
        i += b1 & 0xFF << 24;
        i += b2 & 0xFF << 16;
        i += b3 & 0xFF << 8;
        i += b4 & 0xFF << 0;
        
        return i;
    }
    
    /**
     * 
     * Validates the birth date by checking if 
     * <p>
     * a) the preferred date is before the current date or
     * b) the preferred date is before 1916
     * and correcting it if needed. E.g. the 32nd of January will be turned into the 1st of
     * February.
     * 
     * 
     * @param year The preferred year of the birth date.
     * @param monthOfYear The preferred month of the year.
     * @param dayOfMonth The preferred day of the month.
     * 
     * @return A Gregorian calendar object containing the corrected date or the date that was passed
     * if it was already in a correct format.
     * 
     */
    public static GregorianCalendar getValidatedBirthdate(final int year, final int monthOfYear, 
    		final int dayOfMonth) {
    	GregorianCalendar currentDate = new GregorianCalendar();
    	GregorianCalendar preferredDate = new GregorianCalendar(year, monthOfYear, dayOfMonth);
    	GregorianCalendar oldestDate = new GregorianCalendar(MINIMUM_BIRTH_DATE, JANUARY, 
    			FIRST_DAY_OF_MONTH);
    	
    	if (preferredDate.before(oldestDate)) { // is the birthdate before 1916 (android.DateUtils) 
    		// or 1900 (android.widget.DatePicker)? platform limitation!
    		// return the oldest possible year (1916) with the month and the date entered by the user
    		oldestDate.set(Calendar.YEAR, MINIMUM_BIRTH_DATE);
    		oldestDate.set(Calendar.MONTH, preferredDate.get(Calendar.MONTH));
    		oldestDate.set(Calendar.DAY_OF_MONTH, preferredDate.get(Calendar.DAY_OF_MONTH));
    		
    		return oldestDate;
    	} else if (currentDate.before(preferredDate)) { // is the birthdate entered in the future?
    		// enter the current date
    		return currentDate;
    	}
    	
    	return preferredDate;
    }
}
