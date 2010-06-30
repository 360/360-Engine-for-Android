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

package com.vodafone360.people;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.engine.contactsync.SyncStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.ThirdPartyAccount;
import com.vodafone360.people.utils.LoginPreferences;
import com.vodafone360.people.utils.LogUtils;

/**
 * Caches information about the current state of the application. Stores most
 * recent activity and caches other information such as login details, so that
 * the application is returned to its most recent state when brought back to the
 * foreground or re-entered. The cached details can be cleared if required (as
 * part of 'Remove user data' for example).
 */
public class ApplicationCache {

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    /**
     * Login details are stored in the preferences file so that they can be used
     * to pre-populate the edit fields if the user is interrupted in the middle
     * of login/signup
     */
    public static final String PREFS_FILE = "NOW_PLUS";

    public static final String CONTACT_DATA = "ContactData";

    public static final String CONTACT_SUMMARY = "ContactSummary";

    public static final String CONTACT_ID = "CONTACTID";

    public static final String CONTACT_NAME = "CONTACTNAME";

    public static final String CONTACT_NUMBER = "CONTACTNUMBER";

    public static final String CONTACT_NETWORK = "CONTACTNETWORK";

    public static final String CONTACT_MODIFIED = "ContactModified";

    public static final String STARTUP_LAUNCH_PATH_KEY = "LAUNCHED_FROM";

    public static final int RESULT_DELETE = -100;

    public static final int CONTACT_PROFILE_VIEW = 0;

    public static final int CONTACT_PROFILE_EDIT = 1;

    public static final int CONTACT_PROFILE_ADD = 2;

    public static final int CONTACT_PROFILE_VIEW_ME = 3;

    public static final String THIRD_PARTY_ACCOUNT_NAME_KEY = "ThirdPartyAccountName";

    public static final String THIRD_PARTY_ACCOUNT_NETWORK_KEY = "ThirdPartyNetworkName";

    public static final String THIRD_PARTY_ACCOUNT_USERNAME_KEY = "ThirdPartyAccountUsername";

    public static final String THIRD_PARTY_ACCOUNT_PASSWORD_KEY = "ThirdPartyAccountPassword";

    public static final String THIRD_PARTY_ACCOUNT_CAPABILITIES = "ThirdPartyAccountCapabilities";

    public static final String THIRD_PARTY_ACCOUNT_ERROR_CODE = "ThirdPartyAccountErrorCode";

    private final static String FACEBOOK_SUBSCRIBED = "FacebookSubscribed";

    private final static String HYVES_SUBSCRIBED = "HyvesSubscribed";

    public final static String PREFS_NAME = "NowPlus_Prefs";

    // Check settings
    public final static String PREFS_CHECK_FREQUENCY = "checkFrequency";

    // Upgrade version - these values are deleted when the latest version is not
    // new
    public final static String PREFS_LAST_DIALOG_DATE = "lastDialogDate";

    public final static String PREFS_LAST_CHECKED_DATE = "lastCheckedDate";

    public final static String PREFS_UPGRADE_LATEST_VERSION = "upgradeLatestVersion";

    public final static String PREFS_UPGRADE_URL = "upgradeUrl";

    public final static String PREFS_UPGRADE_TEXT = "upgradeText";

    public static String sWidgetProviderClassName = null;

    public static String sIsNewMessage = "isNewMessage";

    // Frequency setting descriptions and defaults
    public final static long[] FREQUENCY_SETTING_LONG = {
            -1, // Off
            7 * 24 * 60 * 60 * 1000, // Weekly
            24 * 60 * 60 * 1000, // Daily
            6 * 60 * 60 * 1000, // Every 6 hours
            1 * 60 * 60 * 1000, // Hourly
            10 * 60 * 1000
    // Every 10 minutes
    };

    /** In memory cache of the current contacts sync status. **/
    private SyncStatus mSyncStatus = null;
    
    // Cached login flags
    private boolean mFirstTimeLogin = true;

    private boolean mScanThirdPartyAccounts = true;

    private boolean mAcceptedTermsAndConditions = false;

    private int mIdentityBeingProcessed = -1;

    private ArrayList<ThirdPartyAccount> mThirdPartyAccountsCache;

    private ContactSummary mMeProfileSummary;

    private Contact mCurrentContact;

    private ContactSummary mCurrentContactSummary;

    private ServiceStatus mServiceStatus = ServiceStatus.ERROR_UNKNOWN;
    
    private TimelineSummaryItem mCurrentTimelineSummary;

    /**
     * Whether this is a first time login (on this device) for current account.
     * 
     * @return True if this is the first login for current account.
     */
    public boolean firstTimeLogin() {
        return mFirstTimeLogin;
    }

    /**
     * Set whether this is a first time login (on this device) for current
     * account. If we have not logged in on this device (or after 'Remove user
     * data') we will need to perform the first time 'full' time contact sync.
     * 
     * @param aState True if this is our 1st time sync.
     */
    public void setFirstTimeLogin(boolean state) {
        mFirstTimeLogin = state;
    }

    /**
     * Set whether application should re-scan 3rd party accounts.
     * 
     * @param state true if application should re-scan 3rd party accounts.
     */
    public void setScanThirdPartyAccounts(boolean state) {
        mScanThirdPartyAccounts = state;
    }

    /**
     * Return whether application should re-scan 3rd party accounts.
     * 
     * @return true if application should re-scan 3rd party accounts.
     */
    public boolean getScanThirdPartyAccounts() {
        return mScanThirdPartyAccounts;
    }

    /**
     * Set index of Identity currently being processed.
     * 
     * @param index Index of Identity currently being processed.
     */
    public void setIdentityBeingProcessed(int index) {
        mIdentityBeingProcessed = index;
    }

    /**
     * Return index of the Identity currently being processed.
     * 
     * @return index of the Identity currently being processed.
     */
    public int getIdentityBeingProcessed() {
        return mIdentityBeingProcessed;
    }

    /**
     * Set whether user has accepted the Terms and Conditions on sign-up.
     * 
     * @param state true if user has accepted terms and conditions.
     */
    public void setAcceptedTermsAndConditions(boolean state) {
        mAcceptedTermsAndConditions = state;
    }

    /**
     * Return whether user has accepted the Terms and Conditions on sign-up.
     * 
     * @return true if user has accepted terms and conditions.
     */
    public boolean getAcceptedTermsAndConditions() {
        return mAcceptedTermsAndConditions;
    }

    /**
     * Clear all cached data currently stored in People application.
     */
    protected void clearCachedData(Context context) {
        LoginPreferences.clearPreferencesFile(context);
        LoginPreferences.clearCachedLoginDetails();

        mFirstTimeLogin = true;
        mScanThirdPartyAccounts = true;
        mIdentityBeingProcessed = -1;
        mAcceptedTermsAndConditions = false;

        mMeProfileSummary = null;
        mCurrentContact = null;
        mCurrentContactSummary = null;

        mServiceStatus = ServiceStatus.ERROR_UNKNOWN;
        mThirdPartyAccountsCache = null;
        mSyncStatus = null;
    }

    /**
     * Gets the ME profile object.
     * 
     * @return The ME profile object.
     */
    public ContactSummary getMeProfile() {
        return mMeProfileSummary;
    }

    /**
     * Sets the ME profile object.
     * 
     * @param summary ContyactSummary for Me profile.
     */
    public void setMeProfile(ContactSummary summary) {
        mMeProfileSummary = summary;
    }

    /**
     * Gets the contact currently being viewed in the UI.
     * 
     * @return The currently view contact.
     */
    public Contact getCurrentContact() {
        return mCurrentContact;
    }

    /**
     * Sets the contact currently being viewed in the UI.
     * 
     * @param contact The currently viewed contact.
     */
    public void setCurrentContact(Contact contact) {
        mCurrentContact = contact;
    }

    /**
     * Gets the summary information of the contact currently being viewed in the
     * UI.
     * 
     * @return Contact summary information.
     */
    public ContactSummary getCurrentContactSummary() {
        return mCurrentContactSummary;
    }

    /**
     * Sets the summary information of the contact currently being viewed in the
     * UI.
     * 
     * @param contactSummary Contact summary information.
     */
    public void setCurrentContactSummary(ContactSummary contactSummary) {
        mCurrentContactSummary = contactSummary;
    }

    /**
     * Return status of request issued to People service.
     * 
     * @return status of request issued to People service.
     */
    public ServiceStatus getServiceStatus() {
        return mServiceStatus;
    }

    /**
     * Set status of request issued to People service.
     * 
     * @param status of request issued to People service.
     */
    public void setServiceStatus(ServiceStatus status) {
        mServiceStatus = status;
    }

    /**
     * Cache list of 3rd party accounts (Identities) associated with current
     * login.
     * 
     * @param list List of ThirdPartyAccount items retrieved from current login.
     */
    public void storeThirdPartyAccounts(Context context, ArrayList<ThirdPartyAccount> list) {
        setValue(context, FACEBOOK_SUBSCRIBED, isFacebookInThirdPartyAccountList(list) + "");
        setValue(context, HYVES_SUBSCRIBED, isHyvesInThirdPartyAccountList(list) + "");
        mThirdPartyAccountsCache = list;
    }

    /**
     * Return cached list of 3rd party accounts (Identities) associated with
     * current login.
     * 
     * @return List of ThirdPartyAccount items retrieved from current login.
     */
    public ArrayList<ThirdPartyAccount> getThirdPartyAccounts() {
        return mThirdPartyAccountsCache;
    }

    /***
     * Return TRUE if the given ThirdPartyAccount contains a Facebook account.
     * 
     * @param list List of ThirdPartyAccount objects, can be NULL.
     * @return TRUE if the given ThirdPartyAccount contains a Facebook account.
     */
    private static boolean isFacebookInThirdPartyAccountList(ArrayList<ThirdPartyAccount> list) {
        if (list != null) {
            for (ThirdPartyAccount thirdPartyAccount : list) {
                if (thirdPartyAccount.getDisplayName().toLowerCase().startsWith("facebook")) {
                    if (thirdPartyAccount.isChecked()) {
                        LogUtils.logV("ApplicationCache."
                                + "isFacebookInThirdPartyAccountList() Facebook is checked");
                        return true;
                    } else {
                        LogUtils.logV("ApplicationCache."
                                + "isFacebookInThirdPartyAccountList() Facebook is unchecked");
                        return false;
                    }
                }
            }
        }
        LogUtils.logV("ApplicationCache."
                + "isFacebookInThirdPartyAccountList() Facebook not found in list");
        return false;
    }

    /***
     * Return TRUE if the given ThirdPartyAccount contains a Hyves account.
     * 
     * @param list List of ThirdPartyAccount objects, can be NULL.
     * @return TRUE if the given ThirdPartyAccount contains a Hyves account.
     */
    private static boolean isHyvesInThirdPartyAccountList(ArrayList<ThirdPartyAccount> list) {
        if (list != null) {
            for (ThirdPartyAccount thirdPartyAccount : list) {
                if (thirdPartyAccount.getDisplayName().toLowerCase().startsWith("hyves")) {
                    if (thirdPartyAccount.isChecked()) {
                        LogUtils
                                .logV("ApplicationCache.isHyvesInThirdPartyAccountList() Hyves is checked");
                        return true;
                    } else {
                        LogUtils
                                .logV("ApplicationCache.isHyvesInThirdPartyAccountList() Hyves is unchecked");
                        return false;
                    }
                }
            }
        }
        LogUtils.logV("ApplicationCache.isHyvesInThirdPartyAccountList() Hyves not found in list");
        return false;
    }

    /**
     * Get list of IDs of Home-screen widgets.
     * 
     * @param context Current context.
     * @return list of IDs of Home-screen widgets.
     */
    public int[] getWidgetIdList(Context context) {
        if(sWidgetProviderClassName != null) {
        return AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, sWidgetProviderClassName));
        }
        
        return null;
    }

    /***
     * Set a value in the preferences file.
     * 
     * @param context Android context.
     * @param key Preferences file parameter key.
     * @param value Preference value.
     */
    private static void setValue(Context context, String key, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(ApplicationCache.PREFS_FILE,
                0).edit();
        editor.putString(key, value);
        if (!editor.commit()) {
            throw new NullPointerException("MainApplication.setValue() Failed to set key[" + key
                    + "] with value[" + value + "]");
        }
        LogUtils.logV("ApplicationCache.setValue() key [" + key + "] value [" + value
                + "] saved to properties file");
    }

    /***
     * Gets the current sync state, or NULL if the state has not been set in
     * this JVM instance.
     * 
     * @return SyncStatus or NULL.
     */
    public SyncStatus getSyncStatus() {
        return mSyncStatus;
    }

    /***
     * Sets the current sync status.
     * 
     * @param syncStatus New sync status.
     */
    public void setSyncStatus(SyncStatus syncStatus) {
        mSyncStatus = syncStatus;
    }

    /***
     * Get a value from the preferences file.
     * 
     * @param context Android context.
     * @param key Preferences file parameter key.
     * @param defaultValue Preference value.
     * @return
     */
    private static String getValue(Context context, String key, String defaultValue) {
        return context.getSharedPreferences(ApplicationCache.PREFS_FILE, 0).getString(key,
                defaultValue);
    }

    /***
     * Return the resource ID for the SNS Subscribed warning (e.g.
     * facebook/hyves/etc posting), or -1 if no warning is necessary.
     * 
     * @param context Android context.
     * @return Resource ID for textView or -1 is warning is not required.
     * @throws InvalidParameterException when context is NULL.
     */
    public static int getSnsSubscribedWarningId(Context context) {
        if (context == null) {
            throw new InvalidParameterException("ApplicationCache.getSnsSubscribedWarningId() "
                    + "context cannot be NULL");
        }

        boolean facebook = getValue(context, FACEBOOK_SUBSCRIBED, "").equals("true");
        boolean hyves = getValue(context, HYVES_SUBSCRIBED, "").equals("true");

        if (facebook && hyves) {
            return R.string.ContactStatusListActivity_update_status_on_hyves_and_facebook;
        } else if (facebook) {
            return R.string.ContactStatusListActivity_update_status_on_facebook;
        } else if (hyves) {
            return R.string.ContactStatusListActivity_update_status_on_hyves;
        } else {
            return -1;
        }
    }

    public static boolean isBooleanValue(Context context, String key) {
        return TRUE.equals(getValue(context, key, FALSE));
    }

    public static void setBooleanValue(Context context, String key, boolean value) {
        setValue(context, key, value ? TRUE : FALSE);
    }
    
    
    /**
     * Gets the summary information of the Timeline currently being viewed in the
     * UI.
     * 
     * @return Timeline summary information.
     */
    public TimelineSummaryItem getCurrentTimelineSummary() {
        return mCurrentTimelineSummary;
    }

    /**
     * Sets the summary information of the Timeline Item currently being viewed in the
     * UI.
     * 
     * @param timelineSummary Timeline summary information.
     */
    public void setCurrentTimelineSummary(TimelineSummaryItem timelineSummary) {
        mCurrentTimelineSummary = timelineSummary;
    }
}
