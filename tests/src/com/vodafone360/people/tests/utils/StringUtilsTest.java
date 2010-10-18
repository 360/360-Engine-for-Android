package com.vodafone360.people.tests.utils;

import com.vodafone360.people.utils.StringUtils;

import junit.framework.TestCase;

public class StringUtilsTest extends TestCase {
	private static final String VALID_EMAIL_REGEX = ".+@.+\\.[a-z]+";
	
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
