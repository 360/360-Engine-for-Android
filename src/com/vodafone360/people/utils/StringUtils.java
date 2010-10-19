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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * A utility-class for executing operations (e.g. regular expressions) on strings.
 * 
 */
public class StringUtils {
	
	/**
	 * 
	 * Matches a regular expression on the passed String. This method uses the matches()-method of
	 * Java's regex-package to match the regular expression against the whole string.
	 * 
	 * @param regex The regular expression to execute.
	 * @param matchingString The string to run the regex on.
	 * 
	 * @return True if the string matched, false otherwise.
	 * 
	 */
	public static boolean matchRegularExpression(final String regex, final String matchingString) {
		if (null == regex || null == matchingString) {
			return false;
		}
		
    	Matcher matcher = Pattern.compile(regex).matcher(matchingString);
    	return matcher.matches();
   	}
}
