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

package com.vodafone360.people.engine.upgrade;

import android.content.Context;
import android.content.SharedPreferences;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.Settings;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * Utility class for upgrade related functionality.
 */
public class UpgradeUtils {

    /***
     * Return the PREFS_CHECK_FREQUENCY Preference value.
     * 
     * @param context Context
     * @return Current value for PREFS_CHECK_FREQUENCY or the
     *         PREFS_CHECK_FREQUENCY_DEFAULT
     */
    public static long getCheckFrequency(Context context) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(
                ApplicationCache.PREFS_NAME, Context.MODE_WORLD_WRITEABLE);
        return ApplicationCache.FREQUENCY_SETTING_LONG[mSharedPreferences.getInt(
                ApplicationCache.PREFS_CHECK_FREQUENCY,
                Settings.PREFS_CHECK_FREQUENCY_DEFAULT)];
    }

    /**
     * Caches the mUpgradeStatus value if set, but clears the cache if NULL.
     * 
     * @param context - the context to use
     * @param upgradeStatus - the upgrade properties - can be null
     */
    protected static void cacheUpdate(Context context, UpgradeStatus upgradeStatus) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(
                ApplicationCache.PREFS_NAME, Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();

        if (upgradeStatus != null) {
            LogUtils.logV("UpgradeUtils.cacheUpdate() Save upgradeStatus info");
            mEditor.putInt(ApplicationCache.PREFS_UPGRADE_LATEST_VERSION, upgradeStatus
                    .getLatestVersion());
            mEditor.putString(ApplicationCache.PREFS_UPGRADE_URL, upgradeStatus
                    .getUpgradeUrl());
            mEditor.putString(ApplicationCache.PREFS_UPGRADE_TEXT, upgradeStatus
                    .getUpgradeText(context));
            mEditor.putLong(ApplicationCache.PREFS_LAST_CHECKED_DATE, System
                    .currentTimeMillis());
        } else {
            LogUtils.logV("UpgradeUtils.cacheUpdate() Remove upgradeStatus info");
            mEditor.remove(ApplicationCache.PREFS_LAST_DIALOG_DATE);
            mEditor.remove(ApplicationCache.PREFS_UPGRADE_LATEST_VERSION);
            mEditor.remove(ApplicationCache.PREFS_UPGRADE_URL);
            mEditor.remove(ApplicationCache.PREFS_UPGRADE_TEXT);
            mEditor.remove(ApplicationCache.PREFS_LAST_CHECKED_DATE);
        }

        mEditor.commit();
    }

    /**
     * Gets an UpgradeStatus object, if one is stored in the Cache.
     * 
     * @param context - the context to use
     * @param checkFrequencyMillis - the frequency with which to check
     * @return the cached UpgradeStatus or null if none available
     */
    protected static UpgradeStatus getCachedUpdate(Context context, long checkFrequencyMillis) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(
                ApplicationCache.PREFS_NAME, Context.MODE_WORLD_WRITEABLE);
        long mLastCheckedDate = mSharedPreferences.getLong(
                ApplicationCache.PREFS_LAST_CHECKED_DATE, -1);
        UpgradeStatus mUpgradeStatus;

        if ((mLastCheckedDate == -1)
                || checkFrequencyMillis < UpgradeUtils.getAgeMillis(mLastCheckedDate)) {
            // Invalid cache (do not check, to old, empty or never checked)
            // LogUtils.logV("UpgradeUtils.getCachedUpdate() No upgrade status in cache - mLastCheckedDate["+mLastCheckedDate+"]");
            return null;

        }

        mUpgradeStatus = new UpgradeStatus();
        mUpgradeStatus.setLatestVersion(mSharedPreferences.getInt(
                ApplicationCache.PREFS_UPGRADE_LATEST_VERSION, -1));
        mUpgradeStatus.setUpgradeUrl(mSharedPreferences.getString(
                ApplicationCache.PREFS_UPGRADE_URL, null));
        mUpgradeStatus.setUpgradeText(mSharedPreferences.getString(
                ApplicationCache.PREFS_UPGRADE_TEXT, null));

        if (mUpgradeStatus.getLatestVersion() == -1) {
            // LogUtils.logV("UpgradeUtils.getCachedUpdate() No cached upgrade status");
            return null;
        } else if (mUpgradeStatus.getLatestVersion() <= VersionUtils.getPackageVersionCode(context)) {
            LogUtils
                    .logW("UpgradeUtils.getCachedUpdate() Old version status should not have been cached!");
            return null;
        } else {
            return mUpgradeStatus;
        }
    }

    /**
     * Returns the age of a given System.currentTimeMillis() value.
     * 
     * @param time Time to check
     * @return Age in milliseconds
     */
    protected static long getAgeMillis(long time) {
        return System.currentTimeMillis() - time;
    }
}
