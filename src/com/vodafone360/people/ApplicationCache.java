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

import java.lang.ref.SoftReference;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;

import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.contactsync.SyncStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.LoginPreferences;
import com.vodafone360.people.utils.ThirdPartyAccount;

/**
 * Caches information about the current state of the application. Stores most
 * recent activity and caches other information such as login details, so that
 * the application is returned to its most recent state when brought back to the
 * foreground or re-entered. The cached details can be cleared if required (as
 * part of 'Remove user data' for example).
 */
public class ApplicationCache {

    /** Text key for Terms of Service content. **/
    private final static String TERMS_OF_SERVICE = "TERMS_OF_SERVICE";
    /** Text key for Terms of Service last updated time. **/
    private final static String TERMS_OF_SERVICE_TIME = "TERMS_OF_SERVICE_TIME";
    /** Text key for Privacy content. **/
    private final static String PRIVACY = "PRIVACY";
    /** Text key for Privacy last updated time. **/
    private final static String PRIVACY_TIME = "PRIVACY_TIME";
    /**
     * Refresh any cached terms of service or privacy content after 10 minutes.
     */
    private final static long REFRESH_TIME = 10 * 60 * 1000;

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
    
    public static final String ADD_ACCOUNT_CLICKED = "ADD_ACCOUNT_CLICKED";

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
    
    /**
     * Text key to indicate if the intent from StartTabsActivity needs to be
     * retained.
     */
    public final static String RETAIN_INTENT = "RetainIntent";

    /**
     * Current state of the Activities engine fetching older time line logic.
     */
    private static boolean sFetchingOlderTimeline = false;
    /**
     * Current state of the Activities engine updating statuses logic.
     */
    private static boolean sUpdatingStatuses = false;
    /**
     * Current state of the Activities engine fetching newer time line logic.
     */
    private static boolean sFetchingOlderStatuses = false;

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
    
    private boolean mScanThirdPartyAccounts = true;

    private boolean mAcceptedTermsAndConditions = false;

    private int mIdentityBeingProcessed = -1;

    private ArrayList<ThirdPartyAccount> mThirdPartyAccountsCache;

    private ContactSummary mMeProfileSummary;

    private Contact mCurrentContact;

    private ContactSummary mCurrentContactSummary;

    private ServiceStatus mServiceStatus = ServiceStatus.ERROR_UNKNOWN;
    
    /**
     * The constant for storing the "Add Account" button state (hidden or shown).   
     */
    public static final String JUST_LOGGED_IN = "first_time"; 
    
    private TimelineSummaryItem mCurrentTimelineSummary;

    private long mCurrentContactFilter;
    
    /** Cached whether ThirdPartyAccountsActivity is opened. */
    private boolean mIsAddAccountActivityOpened;
    /**
     * For storing the filter type in timeline status.
     */
    private int mSelectedFilterType = 0;
    
    /**
     * For storing the filter type in timeline status of history details.
     */
    private int mSelectedHistoryFilterType;
    
    /**
     * Setter for the selected filter
     */
    public void setSelectedTimelineFilter(int filter) {
    	mSelectedFilterType = filter;
    }
    /**
     * Getter for the selected filter
     */
    public int getSelectedTimelineFilter() {
    	return mSelectedFilterType;
    }
    
    /**
     * Setter for the selected filter
     */
    public void setSelectedHistoryTimelineFilter(int filter) {
        mSelectedHistoryFilterType = filter;
    }
    /**
     * Getter for the selected filter
     */
    public int getSelectedHistoryTimelineFilter() {
        return mSelectedHistoryFilterType;
    }
    
    /***
     * GETTER Whether "add Account" activity is opened
     * 
     * @return True if "add Account" activity is opened
     */
    public boolean addAccountActivityOpened() {
        return mIsAddAccountActivityOpened;
    }

    /***
     * SETTER Whether "add Account" activity is opened.
     * 
     * @param flag if "add Account" activity is opened
     */
    public void setAddAccountActivityOpened(final boolean flag) {
        mIsAddAccountActivityOpened = flag;
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

        setBooleanValue(context, JUST_LOGGED_IN, true);
        setBooleanValue(context, ADD_ACCOUNT_CLICKED, false);
        mScanThirdPartyAccounts = true;
        mIdentityBeingProcessed = -1;
        mAcceptedTermsAndConditions = false;

        mMeProfileSummary = null;
        mCurrentContact = null;
        mCurrentContactSummary = null;

        mServiceStatus = ServiceStatus.ERROR_UNKNOWN;
        mThirdPartyAccountsCache = null;
        mSyncStatus = null;
        
        mIsAddAccountActivityOpened = false;
        
        sFetchingOlderTimeline = false;
        sUpdatingStatuses = false;
        sFetchingOlderStatuses = false;
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
        setValue(context, FACEBOOK_SUBSCRIBED, EngineManager.getInstance().getIdentityEngine().isFacebookInThirdPartyAccountList() + "");
        setValue(context, HYVES_SUBSCRIBED, EngineManager.getInstance().getIdentityEngine().isHyvesInThirdPartyAccountList() + "");
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

        boolean facebook = EngineManager.getInstance().getIdentityEngine().isFacebookInThirdPartyAccountList();
        boolean hyves =EngineManager.getInstance().getIdentityEngine().isHyvesInThirdPartyAccountList();

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

    /***
     * Set the Terms of Service content into the cache.
     * 
     * @param value Terms of Service content.
     * @param context Android context.
     */
    public static void setTermsOfService(final String value,
            final Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ApplicationCache.PREFS_FILE, 0).edit();
        editor.putString(TERMS_OF_SERVICE, value);
        editor.putLong(TERMS_OF_SERVICE_TIME, System.currentTimeMillis());
        if (!editor.commit()) {
            throw new NullPointerException(
                    "MainApplication.setTermsOfService() Failed to set Terms "
                            + "of Service with value[" + value + "]");
        }
    }

    /***
     * Set the Privacy content into the cache.
     * 
     * @param value Privacy content.
     * @param context Android context.
     */
    public static void setPrivacyStatemet(final String value,
            final Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                ApplicationCache.PREFS_FILE, 0).edit();
        editor.putString(PRIVACY, value);
        editor.putLong(PRIVACY_TIME, System.currentTimeMillis());
        if (!editor.commit()) {
            throw new NullPointerException(
                    "MainApplication.setPrivacyStatemet() Failed to set Terms "
                            + "of Service with value[" + value + "]");
        }
    }

    /***
     * Get the Terms of Service content from the cache. Will return NULL if
     * there is no content, or it is over REFRESH_TIME ms old.
     * 
     * @param context Android context.
     * @return Terms of Service content
     */
    public static String getTermsOfService(final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                ApplicationCache.PREFS_FILE, 0);
        long time = sharedPreferences.getLong(TERMS_OF_SERVICE_TIME, -1);
        if (time == -1 || time < System.currentTimeMillis() - REFRESH_TIME) {
            return null;
        } else {
            return sharedPreferences.getString(TERMS_OF_SERVICE, null);
        }
    }

    /***
     * Get the Privacy content from the cache. Will return NULL if there is no
     * content, or it is over REFRESH_TIME ms old.
     * 
     * @param context Android context.
     * @return Privacy content
     */
    public static String getPrivacyStatement(final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                ApplicationCache.PREFS_FILE, 0);
        long time = sharedPreferences.getLong(PRIVACY_TIME, -1);
        if (time == -1 || time < System.currentTimeMillis() - REFRESH_TIME) {
            return null;
        } else {
            return sharedPreferences.getString(PRIVACY, null);
        }
    }

    private static ServiceStatus sStatus = ServiceStatus.SUCCESS;

    public static void setTermsStatus(final ServiceStatus status) {
        sStatus = status;
    }

    public static ServiceStatus getTermsStatus() {
        return sStatus;
    }

    /**
     * @param currentContactFilter the mCurrentContactFilter to set
     */
    public final void setCurrentContactFilter(final long currentContactFilter) {
        mCurrentContactFilter = currentContactFilter;
    }

    /**
     * @return the mCurrentContactFilter
     */
    public final long getCurrentContactFilter() {
        return mCurrentContactFilter;
    }

    /** Background thread for caching Thumbnails in memory. **/
    private SoftReference<ThumbnailCache> mThumbnailCache;

    /***
     * Get or create a background thread for caching Thumbnails in memory.
     * Note: This object can be used by multiple activities.
     */
    public synchronized ThumbnailCache getThumbnailCache() {
        ThumbnailCache local = null;
        if (mThumbnailCache == null || mThumbnailCache.get() == null) {
            local = new ThumbnailCache();
            mThumbnailCache =new SoftReference<ThumbnailCache>(local);
        }
        return mThumbnailCache.get();
    }

    /***
     * TRUE if the Activities engine is currently fetching older time line
     * data.
     *
     * @return TRUE if the Activities engine is currently fetching older time
     *          line data.
     */
    public static boolean isFetchingOlderTimeline() {
        return sFetchingOlderTimeline;
    }

    /***
     * Set if the Activities engine is currently fetching older time line
     * data.
     *
     * @param fetchingOlderTimeline Specific current state.
     */
    public static void setFetchingOlderTimeline(
            final boolean fetchingOlderTimeline) {
        sFetchingOlderTimeline = fetchingOlderTimeline;
    }

    /***
     * TRUE if the Activities engine is currently updating status data.
     *
     * @return TRUE if the Activities engine is currently updating status data.
     */
    public static boolean isUpdatingStatuses() {
        return sUpdatingStatuses;
    }

    /***
     * Set if the Activities engine is currently updating status data.
     *
     * @param updatingStatuses Specific current state.
     */
    public static void setUpdatingStatuses(final boolean updatingStatuses) {
        sUpdatingStatuses = updatingStatuses;
    }
    /***
     * TRUE if the Activities engine is currently fetching older time line
     * statuses.
     *
     * @return TRUE if the Activities engine is currently fetching older time
     *          line data.
     */
    public static boolean isFetchingOlderStatuses() {
        return sFetchingOlderStatuses;
    }

    /***
     * Set if the Activities engine is currently fetching older time line
     * statuses.
     *
     * @param fetchingOlderStatuses Specific current state.
     */
    public static void setFetchingOlderStatuses(
            final boolean fetchingOlderStatuses) {
        sFetchingOlderStatuses = fetchingOlderStatuses;
    }
}
