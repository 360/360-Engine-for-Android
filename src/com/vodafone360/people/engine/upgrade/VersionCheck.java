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
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.utils.LogUtils;

/**
 * Manages the application version check functionality.
 */
public class VersionCheck {

    private Context mContext;

    private UpgradeStatus mUpgradeStatus;

    private long mCheckFrequencyMillis;

    private boolean mForce = false;

    /**
     * Constructor.
     * 
     * @param context - the context to use
     * @param force
     */
    public VersionCheck(Context context, boolean force) {
        LogUtils.logW("VersionCheck force[" + force + "]");
        mContext = context;
        mCheckFrequencyMillis = UpgradeUtils.getCheckFrequency(context);
        mForce = force;
    }

    /**
     * Returns the cached UpgradeStatus, if one is already known to be
     * available.
     * 
     * @return UpgradeStatus
     */
    public UpgradeStatus getCachedUpdateStatus() {
        // LogUtils.logV("VersionCheck.getCachedUpdateStatus()");

        if (SettingsManager.getProperty(Settings.UPGRADE_CHECK_URL_KEY) == null) {
            // Upgrading is not active in this build.
            return null;
        }

        mUpgradeStatus = UpgradeUtils.getCachedUpdate(mContext, mCheckFrequencyMillis);

        if (mUpgradeStatus == null) {
            // LogUtils.logV("VersionCheck.getCachedUpdateStatus() No upgrade status information available");
            return null;
        } else if (mForce) {
            // Do not throttle dialog on request
            LogUtils.logV("VersionCheck.getCachedUpdateStatus()"
                    + " Returning unthrottled upgrade status");
            return mUpgradeStatus;
        }

        SharedPreferences mSharedPreferences = mContext.getSharedPreferences(
                ApplicationCache.PREFS_NAME, Context.MODE_WORLD_WRITEABLE);
        long mLastDialogDate = mSharedPreferences.getLong(
                ApplicationCache.PREFS_LAST_DIALOG_DATE, -1);
        if ((mLastDialogDate == -1)
                || Settings.DIALOG_CHECK_FREQUENCY_MILLIS < UpgradeUtils
                        .getAgeMillis(mLastDialogDate)) {
            LogUtils.logI("VersionCheck.getCachedUpdateStatus()"
                    + " Show upgrade dialog again, as more "
                    + "than DIALOG_CHECK_FREQUENCY_MILLIS has passed ["
                    + UpgradeUtils.getAgeMillis(mLastDialogDate) + "ms] [" + mLastDialogDate + "]");

            // Update PREFS_LAST_CHECKED_DATE value
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putLong(ApplicationCache.PREFS_LAST_DIALOG_DATE, System
                    .currentTimeMillis());
            editor.commit();

            return mUpgradeStatus;
        } else {
            LogUtils.logV("VersionCheck.getCachedUpdateStatus() Do not show upgrade dialog, as "
                    + "last dialog was only ["
                    + UpgradeUtils.getAgeMillis(mLastDialogDate)
                    + "ms]"
                    + " ago, wait another ["
                    + (Settings.DIALOG_CHECK_FREQUENCY_MILLIS - UpgradeUtils
                            .getAgeMillis(mLastDialogDate)) + "ms]");
            return null;
        }
    }
}
