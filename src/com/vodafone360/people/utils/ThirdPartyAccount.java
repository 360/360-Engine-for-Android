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

import android.content.Context;
import android.graphics.Bitmap;

import com.vodafone360.people.R;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.utils.LogUtils;

/**
 * Holds data about a third party account i.e. facebook, msn, google.
 */
public class ThirdPartyAccount {
    private Identity mIdentity;

    private Bitmap mBitmap;

    private boolean mIsVerified = false;

    private boolean mChecked = true;

    private boolean mShouldBeProcessed = false;

    private String mDisplayName;

    /** username */
    private String mIdentityID;

    public static final String SNS_TYPE_FACEBOOK = "facebook";

    private static final String SNS_TYPE_MICROSOFT = "microsoft";

    private static final String SNS_TYPE_MSN = "msn";

    private static final String SNS_TYPE_WINDOWS = "windows";

    private static final String SNS_TYPE_LIVE = "live";

    public static final String SNS_TYPE_GOOGLE = "google";

    public static final String SNS_TYPE_VODAFONE = "vodafone";

    private static final String SNS_TYPE_NOWPLUS = "nowplus";

    private static final String SNS_TYPE_ZYB = "zyb";

    public static final String SNS_TYPE_TWITTER = "twitter";

    public static final String SNS_TYPE_HYVES = "hyves";
    
    public static final String SNS_TYPE_STUDIVZ = "studivz";


    /**
     * Create a new third party account object.
     * 
     * @param userName - the username for the account.
     * @param identity - Identity details retrieved from server.
     * @param checkedByDefault - true for the account to be enabled in the list.
     * @param isVerified -
     */
    public ThirdPartyAccount(String userName, Identity identity, boolean checkedByDefault,
            boolean isVerified) {
        mIdentityID = userName;
        mChecked = checkedByDefault;
        mIdentity = identity;
        mIsVerified = isVerified;
        mDisplayName = identity.mName;
    }

    /**
     * Create a new third party account object.
     *
     * @param userName - the username for the account.
     * @param identity - Identity details retrieved from server.
     * @param isVerified -
     */
    public ThirdPartyAccount(final String userName, final Identity identity,
            final boolean isVerified) {

        /**In ui-refresh will not have flag status of mChecked==false.
         * Because ui-refresh remove checkBox UI of this flag.
         * */
        this(userName, identity, /*checkedByDefault*/true, isVerified);

        /** Always TRUE for UI-Refresh. */
        mShouldBeProcessed = true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ThirdPartyAccount: \n\tmUsername = ");
        sb.append(getIdentityID());
        sb.append("\n\tmDisplayName = "); sb.append(getDisplayName()); 
        sb.append("\n\tmCheckedByDefault = "); sb.append(isChecked());
        sb.append("\n\tmIsVerified = "); sb.append(isVerified()); 
        sb.append("\n\tmShouldBeProcesed = "); sb.append(isShouldBeProcessed());
        return sb.toString();
    }

    /*
     * Checks if the sns string contains text to identify it as Windows live sns
     * @param sns - the text to check
     * @return true if this is a Windows Live sns
     */
    public static boolean isWindowsLive(String sns) {
        String snsLower = sns.toLowerCase();
        return (snsLower.contains(SNS_TYPE_MSN) || snsLower.contains(SNS_TYPE_LIVE)
                || snsLower.contains(SNS_TYPE_MICROSOFT) || snsLower.contains(SNS_TYPE_WINDOWS));
    }

    /*
     * Checks if the sns string contains text to identify it as Vodafone sns
     * @param sns - the text to check
     * @return true if this is a Vodafone sns
     */
    public static boolean isVodafone(String sns) {
        String snsLower = sns.toLowerCase();
        return (snsLower.contains(SNS_TYPE_VODAFONE) || snsLower.contains(SNS_TYPE_NOWPLUS) || snsLower
                .contains(SNS_TYPE_ZYB));
    }

    /**
     * Gets the Localised string for the given SNS.
     * 
     * @param sns - text of the sns type
     * @return Localised string for the given SNS.
     */
    public static String getSnsString(Context context, String sns) {
        if (sns == null || isVodafone(sns)) {
            return context.getString(R.string.Utils_sns_name_vodafone);
        } else if (sns.contains(SNS_TYPE_FACEBOOK)) {
            return context.getString(R.string.Utils_sns_name_facebook);
        } else if (sns.contains(SNS_TYPE_GOOGLE)) {
            return context.getString(R.string.Utils_sns_name_google);
        } else if (isWindowsLive(sns)) {
            return context.getString(R.string.Utils_sns_name_msn);
        } else if (sns.contains(SNS_TYPE_TWITTER)) {
            return context.getString(R.string.Utils_sns_name_twitter);
        } else if (sns.startsWith(SNS_TYPE_HYVES)) {
            return context.getString(R.string.Utils_sns_name_hyves);
        } else if (sns.startsWith(SNS_TYPE_STUDIVZ)) {
            return context.getString(R.string.Utils_sns_name_studivz);
        } else {
            LogUtils.logE("SNSIconUtils.getSNSStringResId() SNS String[" + sns + "] is not of a "
                    + "known type, so returning empty string value");
            return "";
        }
    }

    /**
     * @param mBitmap the mBitmap to set
     */
    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }

    /**
     * @return the mBitmap
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * @param mIdentity the mIdentity to set
     */
    public void setIdentity(Identity mIdentity) {
        this.mIdentity = mIdentity;
    }

    /**
     * @return the mIdentity
     */
    public Identity getIdentity() {
        return mIdentity;
    }

    /**
     * @param mIsVerified the mIsVerified to set
     */
    public void setIsVerified(boolean mIsVerified) {
        this.mIsVerified = mIsVerified;
    }

    /**
     * @return the mIsVerified
     */
    public boolean isVerified() {
        return mIsVerified;
    }

    /**
     * @param mDisplayName the mDisplayName to set
     */
    public void setDisplayName(String mDisplayName) {
        this.mDisplayName = mDisplayName;
    }

    /**
     * @return the mDisplayName
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @param mIdentityID the mIdentityID to set
     */
    public void setIdentityID(String mIdentityID) {
        this.mIdentityID = mIdentityID;
    }

    /**
     * @return the mIdentityID
     */
    public String getIdentityID() {
        return mIdentityID;
    }

    /**
     * @param mChecked the mChecked to set
     */
    public void setChecked(boolean mChecked) {
        this.mChecked = mChecked;
    }

    /**
     * @return the mChecked
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * @param mShouldBeProcessed the mShouldBeProcessed to set
     */
    public void setShouldBeProcessed(boolean mShouldBeProcessed) {
        this.mShouldBeProcessed = mShouldBeProcessed;
    }

    /**
     * @return the mShouldBeProcessed
     */
    public boolean isShouldBeProcessed() {
        return mShouldBeProcessed;
    }

}
