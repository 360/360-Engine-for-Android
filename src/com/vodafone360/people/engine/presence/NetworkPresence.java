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

package com.vodafone360.people.engine.presence;

import com.vodafone360.people.utils.ThirdPartyAccount;

/**
 * A wrapper for the user presence state on certain web or IM account
 */
public class NetworkPresence {

    private String mUserId;

    private int mNetworkId;

    private int mOnlineStatusId;

    /**
     * Constructor with parameters.
     * 
     * @param mNetworkId - the ordinal of the network name in the overall
     *            hardcoded network list.
     * @param mOnlineStatusId - the ordinal of user presence status in the
     *            overall hardcoded statuses list specifically for this web
     *            account
     */
    public NetworkPresence(String userId, int networkId, int onlineStatusId) {
        super();
        this.mUserId = userId;
        this.mNetworkId = networkId;
        this.mOnlineStatusId = onlineStatusId;
    }

    /**
     * @return the ordinal of the network name on the overall hardcoded network
     *         list.
     */
    public int getNetworkId() {
        return mNetworkId;
    }

    /**
     * @return the ordinal of the online status name on the overall hardcoded
     *         status list.
     */
    public int getOnlineStatusId() {
        return mOnlineStatusId;
    }

    /**
     * @return user account name on Nowplus or other social network
     */
    public String getUserId() {
        return mUserId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mNetworkId;
        result = prime * result + mOnlineStatusId;
        result = prime * result + ((mUserId == null) ? 0 : mUserId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NetworkPresence other = (NetworkPresence)obj;
        if (mNetworkId != other.mNetworkId)
            return false;
        if (mOnlineStatusId != other.mOnlineStatusId)
            return false;
        if (mUserId == null) {
            if (other.mUserId != null)
                return false;
        } else if (!mUserId.equals(other.mUserId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "NetworkPresence [mNetworkId=" + mNetworkId + ", mOnlineStatusId=" + mOnlineStatusId
                + ", mUserId=" + mUserId + "]";
    }

    /**
     * Hard coded networks with enable IM list
     */
    public static enum SocialNetwork {
        FACEBOOK_COM("facebook.com"),
        HYVES_NL("hyves.nl"),
        GOOGLE("google"),
        MICROSOFT("microsoft"),
        MOBILE("mobile"),
        PC("pc"),
        VODAFONE("vodafone"); // the aggregated status for "pc" and "mobile"
                              // only used by - UI - Contact Profile View
        private String mSocialNetwork; // / The name of the field as it appears
                                       // in the database

        private SocialNetwork(String field) {
            mSocialNetwork = field;
        }

        public String toString() {
            return mSocialNetwork;
        }

        protected static SocialNetwork getValue(String value) {
            try {
                return valueOf(value.replace('.', '_').toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }

        public static SocialNetwork getPresenceValue(int index) {
            if (index == VODAFONE.ordinal())
                return MOBILE;
            return values()[index];
        }

        public static SocialNetwork getChatValue(int index) {
            if (index == MOBILE.ordinal() || index == PC.ordinal() || index == VODAFONE.ordinal())
                return VODAFONE;
            return values()[index];
        }

        public static SocialNetwork getNetworkBasedOnString(String sns) {
            if (sns != null) {
                if (sns.contains(ThirdPartyAccount.SNS_TYPE_FACEBOOK)) {
                    return FACEBOOK_COM;
                } else if (sns.contains(ThirdPartyAccount.SNS_TYPE_HYVES)) {
                    return HYVES_NL;
                } else if (ThirdPartyAccount.isWindowsLive(sns)) {
                    return MICROSOFT;
                } else if (sns.contains(ThirdPartyAccount.SNS_TYPE_GOOGLE)) {
                    return GOOGLE;
                } else if (sns.contains(ThirdPartyAccount.SNS_TYPE_TWITTER)) {
                    return PC;
                } else if (ThirdPartyAccount.isVodafone(sns)) {
                    return VODAFONE;
                }
            }
            return null;
        }
    }
}
