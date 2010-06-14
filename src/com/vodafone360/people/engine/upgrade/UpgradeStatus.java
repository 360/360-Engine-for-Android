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

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import com.vodafone360.people.R;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * Represents a parsed remote upgrade status text file.
 */
public class UpgradeStatus {

    private static final String LATEST_VERSION = "LatestVersion";

    private static final String UPGRADE_URL = "UpgradeUrl";

    private static final String UPGRADE_TEXT = "UpgradeText";

    private int mLatestVersion;

    private String mUpgradeUrl;

    private String mUpgradeText;

    /**
     * Add a setting.
     * 
     * @param input Single line from the update descriptor file
     */
    protected void addSetting(String input) {
        int mPos = input.indexOf(":");
        if (mPos != -1) {
            String key = input.substring(0, mPos);
            String value = input.substring(mPos + 1, input.length());
            if (key.equalsIgnoreCase(LATEST_VERSION)) {
                setLatestVersion(Integer.valueOf(value));
            } else if (key.equalsIgnoreCase(UPGRADE_URL)) {
                setUpgradeUrl(value);
            } else if (key.equalsIgnoreCase(UPGRADE_TEXT)) {
                setUpgradeText(value);
            }
        } else {
            LogUtils.logW("UpgradeStatus.addSetting() Invalid input[" + input + "]");
        }
    }

    /**
     * Get the latest version.
     * 
     * @return the latest version
     */
    public int getLatestVersion() {
        return mLatestVersion;
    }

    /**
     * Sets the latest version.
     * 
     * @param latestVersion - the latest version
     */
    public void setLatestVersion(int latestVersion) {
        mLatestVersion = latestVersion;
    }

    /**
     * Gets the upgrade URL.
     * 
     * @return the upgrade URL
     */
    public String getUpgradeUrl() {
        return mUpgradeUrl;
    }

    /**
     * Sets the new upgrade URL.
     * 
     * @param upgradeUrl - the URL to use
     */
    public void setUpgradeUrl(String upgradeUrl) {
        mUpgradeUrl = upgradeUrl;
    }

    /**
     * Get the upgrade text.
     * 
     * @return the updgrade text
     */
    public String getUpgradeText(Context context) {
        if (mUpgradeText != null && !mUpgradeText.trim().equals("")) {
            return mUpgradeText;
        } else {
            return context.getString(R.string.UpgradeStatus_Message_default);
        }
    }

    /**
     * Set the upgrade text.
     * 
     * @param upgradeText - the new text
     */
    public void setUpgradeText(String upgradeText) {
        mUpgradeText = upgradeText;
    }

    private static Map<String, String> sMap = new HashMap<String, String>();

    /**
     * Set the upgrade version.
     * 
     * @param context Android context.
     */
    public void setUpgradeVersion(final Context context) {
        sMap.put("CurrentVersion", "" + VersionUtils.getPackageVersionCode(context));
        sMap.put("LatestVersion", "" + getLatestVersion());
        // FlurryAgent.onEvent("ShowUpgradeDialog", sMap);
    }
}
