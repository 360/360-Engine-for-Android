package com.vodafone360.people.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
