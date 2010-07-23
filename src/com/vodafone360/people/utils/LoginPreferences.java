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

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.SharedPreferences;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.utils.LogUtils;

/***
 * Store various preferences used during login in a preferences file. TODO:
 * Requires some refactoring to clear up underutilised variables.
 */
public class LoginPreferences {

    public final static String STORE_PROGRESS = "progress";

    private final static String PREFS_PATH = "/data/data/com.vodafone360.people/shared_prefs/";

    private final static String SIGNUP_EMAIL_ADDRESS = "SIGNUP_EMAIL_ADDRESS";

    private final static String SIGNUP_FIRSTNAME = "SIGNUP_FIRSTNAME";

    private final static String SIGNUP_LASTNAME = "SIGNUP_LASTNAME";

    private final static String SIGNUP_DOB_DAY = "SIGNUP_DOB_DAY";

    private final static String SIGNUP_DOB_MONTH = "SIGNUP_DOB_MONTH";

    private final static String SIGNUP_DOB_YEAR = "SIGNUP_DOB_YEAR";

    private final static String MOBILE_NUMBER = "MOBILE";

    private final static String USERNAME = "USERNAME";

    private final static String PASSWORD = "PASSWORD";

    private final static String LAST_LOGIN_SCREEN = "NOWPLUS_SCREEN";

    private final static String XML_FILE_EXTENSION = ".xml";

    /*
     * Account creation
     */
    // Signup
    private static String sSignupFirstName = "";

    private static String sSignupLastName = "";

    private static String sSignupEmailAddress = "";

    private static volatile GregorianCalendar sDateOfBirth = null;

    // Login
    private static String sMobileNumber = "";

    private static String sPassword = "";

    private static String sUsername = "";

    // TODO: Why are we storing this information twice?
    private static LoginDetails sLoginDetails = new LoginDetails();

    /**
     * Retrieves the name of the current login activity. Also retrieves login
     * details from the preferences so that the same screen is displayed when
     * the user re-enters the wizard.
     * 
     * @param context - Android context.
     * @return The name of the current login wizard activity
     */
    public static String getCurrentLoginActivity(Context context) {
        LogUtils.logV("ApplicationCache.getCurrentLoginActivity()");
        SharedPreferences preferences = context
                .getSharedPreferences(ApplicationCache.PREFS_FILE, 0);

        // Load sign up settings
        sSignupEmailAddress = preferences.getString(SIGNUP_EMAIL_ADDRESS, "");
        sSignupFirstName = preferences.getString(SIGNUP_FIRSTNAME, "");
        sSignupLastName = preferences.getString(SIGNUP_LASTNAME, "");
        setSignupDateOfBirth(preferences.getInt(SIGNUP_DOB_DAY, -1), preferences.getInt(
                SIGNUP_DOB_MONTH, -1), preferences.getInt(SIGNUP_DOB_YEAR, -1));

        // Load login Settings
        sMobileNumber = preferences.getString(MOBILE_NUMBER, "");
        sUsername = preferences.getString(USERNAME, "");
        sPassword = preferences.getString(PASSWORD, "");

        // Return current screen
        return preferences.getString(LAST_LOGIN_SCREEN, "");
    }

    /**
     * Stores the name of the current login activity. Also stores the login
     * details in the preferences so that they can be retrieved when the user
     * has been diverted away from the login wizard.
     * 
     * @param loginScreenName The name of the current login wizard activity.
     */
    public static void setCurrentLoginActivity(String loginScreenName, Context context) {
        LogUtils.logV("MainApplication.setCurrentLoginActivity() loginScreenName["
                + loginScreenName + "]");
        SharedPreferences.Editor editor = context.getSharedPreferences(ApplicationCache.PREFS_FILE,
                0).edit();

        // Save log in screen name.
        editor.putString(LAST_LOGIN_SCREEN, loginScreenName);

        // Save sign up settings.
        editor.putString(SIGNUP_EMAIL_ADDRESS, sSignupEmailAddress);
        editor.putString(SIGNUP_FIRSTNAME, sSignupFirstName);
        editor.putString(SIGNUP_LASTNAME, sSignupLastName);
        editor.putInt(SIGNUP_DOB_DAY, sDateOfBirth != null ? sDateOfBirth
                .get(Calendar.DAY_OF_MONTH) : -1);
        editor.putInt(SIGNUP_DOB_MONTH, sDateOfBirth != null ? sDateOfBirth.get(Calendar.MONTH)
                : -1);
        editor.putInt(SIGNUP_DOB_YEAR, sDateOfBirth != null ? sDateOfBirth.get(Calendar.YEAR) : -1);

        // Save login settings.
        editor.putString(MOBILE_NUMBER, sMobileNumber);
        editor.putString(USERNAME, sUsername);
        editor.putString(PASSWORD, sPassword);

        if (!editor.commit()) {
            throw new NullPointerException("MainApplication.setCurrentLoginActivity() Failed to"
                    + " set current login activity");
        }
    }

    /**
     * Clear cached login details.
     */
    public static void clearCachedLoginDetails() {
        // Clear sign up settings.
        sSignupEmailAddress = "";
        sSignupFirstName = "";
        sSignupLastName = "";
        sDateOfBirth = null;

        // Clear login settings.
        sMobileNumber = "";
        sUsername = "";
        sPassword = "";

        sLoginDetails = new LoginDetails();
    }

    /**
     * Store current login details.
     * 
     * @param loginDetails Current login details.
     */
    public static void setLoginDetails(LoginDetails loginDetails) {
        sLoginDetails = loginDetails;
    }

    /**
     * Re-initialise login details currently held in application cache.
     */
    public static void initLoginDetails() {
        sLoginDetails = new LoginDetails();
    }

    /**
     * Gets the current set of login parameters.
     * 
     * @return current login details.
     */
    public static LoginDetails getLoginDetails() {
        return sLoginDetails;
    }

    /**
     * Gets the current login password.
     * 
     * @return The current user entered login password.
     */
    public static String getPassword() {
        return sPassword;
    }

    /**
     * Sets the current login password.
     * 
     * @param aPassword login password
     */
    public static void setPassword(String aPassword) {
        sPassword = aPassword;
    }

    /**
     * Gets the current login user name.
     * 
     * @return The current user entered login user name.
     */
    public static String getUsername() {
        return sUsername;
    }

    /**
     * Sets the current login user name.
     * 
     * @param username login user name
     */
    public static void setUsername(String username) {
        sUsername = username;
    }

    /**
     * Gets the current login mobile number.
     * 
     * @return The current user entered login password
     */
    public static String getMobileNumber() {
        return sMobileNumber;
    }

    /**
     * Sets the current login mobile number.
     * 
     * @param mobileNumber login mobile number
     */
    public static void setMobileNumber(String mobileNumber) {
        sMobileNumber = mobileNumber;
    }

    /**
     * Sets the current account creation email address.
     * 
     * @param aEmailAddress account signup email address
     */
    public static void setSignupEmailAddress(String aEmailAddress) {
        sSignupEmailAddress = aEmailAddress;
    }

    /**
     * Retrieves the current account creation email address.
     * 
     * @return Currently stored account creation email address
     */
    public static String getSignupEmailAddress() {
        return sSignupEmailAddress;
    }

    /**
     * Sets the current account creation first name.
     * 
     * @param aFirstName account creation first name
     */
    public static void setSignupFirstName(String aFirstName) {
        sSignupFirstName = aFirstName;
    }

    /**
     * Sets the current account creation last name.
     * 
     * @param aLastName account creation last name
     */
    public static void setSignupLastName(String aLastName) {
        sSignupLastName = aLastName;
    }

    /**
     * Retrieves the current account creation first name.
     * 
     * @return Current account creation first name
     */
    public static String getSignupFirstName() {
        return sSignupFirstName;
    }

    /**
     * Retrieves the current account creation last name.
     * 
     * @return Current account creation last name
     */
    public static String getSignupLastName() {
        return sSignupLastName;
    }

    /**
     * Return the date of birth from current account details.
     * 
     * @return Date containing date of birth
     */
    public static GregorianCalendar getSignupDateOfBirth() {
        if (sDateOfBirth != null) {
            LogUtils.logV("MainApplication.getSignupDateOfBirth() mDateOfBirth["
                    + sDateOfBirth.toString() + "]");
        } else {
            LogUtils.logV("MainApplication.getSignupDateOfBirth() mDateOfBirth is NULL");
        }
        return sDateOfBirth;
    }

    /**
     * Set date of birth for current account.
     * 
     * @param day Date of birth within month.
     * @param month Month of birth
     * @param year Year of birth
     */
    public static void setSignupDateOfBirth(int day, int month, int year) {
        LogUtils.logV("MainApplication.setSignupDateOfBirth() DOB: " + day + " " + month + " "
                + year);
        if (day < 0 || month < 0 || year < 0) {
            return;
        }
        
        if (sDateOfBirth == null) {
            sDateOfBirth = new GregorianCalendar(year, month, day);
        }
      
        sDateOfBirth.set(year, month, day);
    }

    /**
     * reset object of GregorianCalendar to be null when user input invalid
     * birthday date in signup screen.
     */
    public static void resetSignupDateOfBirth() {
        sDateOfBirth = null;
    }

    /**
     * Clears all details stored in the preferences.
     */
    public static void clearPreferencesFile(Context context) {
        LogUtils.logV("MainApplication.clearPreferencesFile()");
        SharedPreferences.Editor editor = context.getSharedPreferences(ApplicationCache.PREFS_FILE,
                0).edit();
        editor.clear();
        if (editor.commit()) {
            LogUtils.logV("MainApplication.clearPreferencesFile() All preferences cleared");
        } else {
            LogUtils.logE("MainApplication.clearPreferencesFile() Failed to clear preferences");
        }

        // TODO: Is is necessary to delete the original file?
        // Login details are stored in the preferences file below so that
        // they can be used to pre-populate the edit fields if the
        // user is interrupted in the middle of login/signup
        // Delete this file as this function is called by remove user data
        // functionality
        if (!new File(PREFS_PATH + ApplicationCache.PREFS_FILE + XML_FILE_EXTENSION).delete()) {
            LogUtils.logE("LoginPreferences.clearPreferencesFile(context) failed");
        }
    }
}
