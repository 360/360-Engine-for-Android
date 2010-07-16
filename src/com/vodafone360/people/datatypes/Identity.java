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

package com.vodafone360.people.datatypes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.os.Parcel;
import android.os.Parcelable;

import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType encapsulating an Identity issued to or retrieved from server
 */
public class Identity extends BaseDataType implements Parcelable {

    /**
     * Tags associated with Identity item.
     */
    private enum Tags {
        IDENTITY_MAIN_TAG("availableidentity"),
        IDENTITY_CAPABILITY_LIST("identitycapabilitylist"),
        PLUGIN_ID("pluginid"),
        NETWORK_URL("networkurl"),
        AUTH_TYPE("authtype"),
        ICON_MIME("iconmime"),
        ICON2_MIME("icon2mime"),
        ORDER("order"),
        NAME("name"),
        ICON_URL("iconurl"),
        ICON2_URL("icon2url"),
        NETWORK("network"), // Properties below are only present after
        // GetMyIdentities.
        ACTIVE("active"),
        CREATED("created"),
        IDENTITY_ID("identityid"),
        UPDATED("updated"),
        IDENTITY_TYPE("identitytype"),
        USER_ID("userid"),
        USER_NAME("username"),
        DISPLAY_NAME("displayname"),
        COUNTRY_LIST("countrylist");

        private final String tag;

        /**
         * Construct Tags item from supplied String
         * 
         * @param s String value for Tags item.
         */
        private Tags(String s) {
            tag = s;
        }

        /**
         * String value for Tags item.
         * 
         * @return String value for Tags item.
         */
        private String tag() {
            return tag;
        }

        /**
         * Find Tags item for specified String.
         * 
         * @param tag String value to find in Tags items.
         * @return Tags item for specified String, NULL otherwise.
         */
        private static Tags findTag(String tag) {
            for (Tags tags : Tags.values()) {
                if (tag.compareTo(tags.tag()) == 0) {
                    return tags;
                }
            }
            return null;
        }
    }

    public String mPluginId;

    public String mNetwork;

    public URL mNetworkUrl;

    public URL mIconUrl;

    public URL mIcon2Url;

    public String mAuthType;

    public String mIconMime;

    public int mOrder;

    public String mName;

    public List<IdentityCapability> mCapabilities;

    /** Properties below are only present after GetMyIdentities. */
    public boolean mActive;

    public long mCreated;

    public long mUpdated;

    public String mIdentityId;

    public int mUserId;

    public String mUserName;

    public String mDisplayName;

    public List<String> mCountryList;

    public String mIdentityType;
    
    private int mType;

    /**
     * Comparator class used to compare Identities retrieved from server to
     * remove duplicates from list passed to People client UI.
     */
    public static class IdentityComparator implements Comparator<Identity> {

        @Override
        public int compare(Identity object1, Identity object2) {
            return new Integer(object1.mOrder).compareTo(new Integer(object2.mOrder));
        }
    }

    /**
     * Test whether current Identity is identical to supplied Identity.
     * 
     * @param id Identity to compare against.
     * @return true if Identities match, false otherwise.
     */
    public boolean isSameAs(Identity id) {
        boolean isSame = true;
        if (!areStringValuesSame(mPluginId, id.mPluginId)
                || !areStringValuesSame(mNetwork, id.mNetwork)
                || !areStringValuesSame(mIdentityId, id.mIdentityId)
                || !areStringValuesSame(mDisplayName, id.mDisplayName)) {
            isSame = false;
        }

        if (mNetworkUrl != null && id.mNetworkUrl != null) {
            if (!mNetworkUrl.sameFile(id.mNetworkUrl)) {
                isSame = false;
            }

        } else if (mNetworkUrl == null && id.mNetworkUrl == null) {
            // Do nothing.

        } else {
            isSame = false;
        }

        if (mIconUrl != null && id.mIconUrl != null) {
            if (!mIconUrl.sameFile(id.mIconUrl)) {
                isSame = false;
            }

        } else if (mIconUrl == null && id.mIconUrl == null) {
            // Do nothing.

        } else {
            isSame = false;
        }

        return isSame;
    }

    /**
     * String values comparison
     * 
     * @param s1 First String to test.
     * @param s2 Second String to test.
     * @return true if Strings match (or both are null), false otherwise.
     */
    private boolean areStringValuesSame(String s1, String s2) {
        boolean isSame = true;
        if (s1 == null && s2 == null) {
            // Do nothing.

        } else if (s1 != null && s2 != null) {
            if (s1.compareTo(s2) != 0) {
                isSame = false;
            }

        } else {
            isSame = false;
        }

        return isSame;
    }

    /**
     * Default constructor.
     */
    public Identity() {
        // Do nothing.
    }
    
    public Identity(int type) {
    	mType = type;
    }

    /**
     * Create Identity from Parcel.
     * 
     * @param in Parcel containing Identity.
     */
    private Identity(Parcel in) {
        readFromParcel(in);
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return mType;
    }

    /**
     * Populate Identity from supplied Hashtable.
     * 
     * @param hash Hashtable containing identity details.
     * @return Identity instance.
     */
    public Identity createFromHashtable(Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = Tags.findTag(key);
            if (tag != null)
                setValue(tag, value);
        }

        return this;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag.
     * @param val Value associated with the tag.
     */
    private void setValue(Tags tag, Object val) {
        switch (tag) {
            case AUTH_TYPE:
                mAuthType = (String)val;
                break;

            case ICON_MIME:
                mIconMime = (String)val;
                break;

            case ICON2_MIME:
                // TODO: Remove TAG value?
                // mIcon2Mime = (String)val;
                break;

            case ICON_URL:
                try {
                    mIconUrl = new URL((String)val);
                } catch (MalformedURLException e) {
                    LogUtils.logE("Wrong icon url: '" + val + "'");
                    mIconUrl = null;
                }
                break;

            case ICON2_URL:
                try {
                    mIcon2Url = new URL((String)val);
                } catch (MalformedURLException e) {
                    LogUtils.logE("Wrong icon url: '" + val + "'");
                    mIcon2Url = null;
                }
                break;

            case IDENTITY_CAPABILITY_LIST:
                /** Create id capability list. */
                @SuppressWarnings("unchecked")
                Vector<Hashtable<String, Object>> v = (Vector<Hashtable<String, Object>>)val;
                if (mCapabilities == null) {
                    mCapabilities = new ArrayList<IdentityCapability>();
                }
                for (Hashtable<String, Object> obj : v) {
                    IdentityCapability cap = new IdentityCapability();
                    cap.createFromHashtable(obj);

                    mCapabilities.add(cap);
                }
                break;

            case IDENTITY_MAIN_TAG:
                // Not currently handled.
                break;

            case NAME:
                mName = (String)val;
                break;

            case NETWORK:
                mNetwork = (String)val;
                break;

            case NETWORK_URL:
                try {
                    mNetworkUrl = new URL((String)val);
                } catch (MalformedURLException e) {
                    LogUtils.logE("Wrong network url: '" + val + "'");
                    mNetworkUrl = null;
                }
                break;

            case ORDER:
                mOrder = (Integer)val;
                break;

            case PLUGIN_ID:
                mPluginId = (String)val;
                break;

            case ACTIVE:
                mActive = (Boolean)val;
                break;

            case CREATED:
                mCreated = (Long)val;
                break;

            case DISPLAY_NAME:
                mDisplayName = (String)val;
                break;

            case IDENTITY_ID:
                mIdentityId = (String)val;
                break;

            case IDENTITY_TYPE:
                mIdentityType = (String)val;
                break;

            case UPDATED:
                mUpdated = (Long)val;
                break;

            case USER_ID:
                mUserId = ((Long)val).intValue();
                break;

            case USER_NAME:
                mUserName = (String)val;
                break;

            case COUNTRY_LIST:
                if (mCountryList == null) {
                    mCountryList = new ArrayList<String>();
                }
                break;
                
            default:
                // Do nothing.
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Name:");
        sb.append(mName);
        sb.append("\nPluginID:"); sb.append(mPluginId);
        sb.append("\nNetwork:"); sb.append(mNetwork);
        sb.append("\nNetworkURL:"); sb.append(mNetworkUrl);
        sb.append("\nAuthType:"); sb.append(mAuthType);
        sb.append("\nIcon mime:"); sb.append(mIconMime);
        sb.append("\nIconURL:"); sb.append(mIconUrl);
        sb.append("\nOrder:"); sb.append(mOrder);
        sb.append("\nActive:"); sb.append(mActive);
        sb.append("\nCreated:"); sb.append(mCreated);
        sb.append("\nUpdated:"); sb.append(mUpdated);
        sb.append("\nIdentityId:"); sb.append(mIdentityId);
        sb.append("\nUserId:"); sb.append(mUserId);
        sb.append("\nUserName:"); sb.append(mUserName);
        sb.append("\nDisplayName:"); sb.append(mDisplayName);
        sb.append("\nIdentityType:"); sb.append(mIdentityType);

        if (mCountryList != null) {
            sb.append("\nCountry List: ("); 
            sb.append(mCountryList.size());
            sb.append(") = [");
            for (int i = 0; i < mCountryList.size(); i++) {
                sb.append(mCountryList.get(i));
                if (i < mCountryList.size() - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }

        if (mCapabilities != null) {
            sb.append("\nCapabilities ("); 
            sb.append(mCapabilities.size());
            sb.append(")");
            for (int i = 0; i < mCapabilities.size(); i++) {
                sb.append("\n" + mCapabilities.get(i).toString());
                if (i < mCapabilities.size() - 1) {
                    sb.append("\n\t---");
                }
            }
        }
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 1;
    }

    /**
     * Enumeration containing items contained within Identity Parcel.
     */
    private enum MemberData {
        PLUGIN_ID,
        NETWORK,
        NETWORK_URL,
        ICON_URL,
        AUTH_TYPE,
        ICON_MIME,
        ORDER,
        NAME;
    }

    /**
     * Read Identity item from Parcel.
     * 
     * @param in Parcel containing Identity information.
     */
    private void readFromParcel(Parcel in) {
        mPluginId = null;
        mNetwork = null;
        mNetworkUrl = null;
        mIconUrl = null;
        mAuthType = null;
        mIconMime = null;
        mOrder = -1;
        mName = null;
        mCapabilities = null;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.PLUGIN_ID.ordinal()]) {
            mPluginId = in.readString();
        }

        if (validDataList[MemberData.NETWORK.ordinal()]) {
            mNetwork = in.readString();
        }

        if (validDataList[MemberData.NETWORK_URL.ordinal()]) {
            try {
                mNetworkUrl = new URL(in.readString());
            } catch (MalformedURLException e) {
                LogUtils.logW("Identity.readFromParcel() "
                        + "MalformedURLException on MemberData.NETWORK_URL");
            }
        }

        if (validDataList[MemberData.ICON_URL.ordinal()]) {
            try {
                mIconUrl = new URL(in.readString());
            } catch (MalformedURLException e) {
                LogUtils.logW("Identity.readFromParcel() "
                        + "MalformedURLException on MemberData.ICON_URL");
            }
        }

        if (validDataList[MemberData.AUTH_TYPE.ordinal()]) {
            mAuthType = in.readString();
        }

        if (validDataList[MemberData.ICON_MIME.ordinal()]) {
            mIconMime = in.readString();
        }

        if (validDataList[MemberData.ORDER.ordinal()]) {
            mOrder = in.readInt();
        }

        if (validDataList[MemberData.NAME.ordinal()]) {
            mName = in.readString();
        }

        int noOfCapabilities = in.readInt();
        if (noOfCapabilities > 0) {
            mCapabilities = new ArrayList<IdentityCapability>(noOfCapabilities);
            for (int i = 0; i < noOfCapabilities; i++) {
                IdentityCapability cap = IdentityCapability.CREATOR.createFromParcel(in);
                mCapabilities.add(cap);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        boolean[] validDataList = new boolean[MemberData.values().length];
        int validDataPos = dest.dataPosition();
        dest.writeBooleanArray(validDataList); // Placeholder for real array.

        if (mPluginId != null) {
            validDataList[MemberData.PLUGIN_ID.ordinal()] = true;
            dest.writeString(mPluginId);
        }

        if (mNetwork != null) {
            validDataList[MemberData.NETWORK.ordinal()] = true;
            dest.writeString(mNetwork);
        }

        if (mNetworkUrl != null) {
            validDataList[MemberData.NETWORK_URL.ordinal()] = true;
            dest.writeString(mNetworkUrl.toString());
        }

        if (mIconUrl != null) {
            validDataList[MemberData.ICON_URL.ordinal()] = true;
            dest.writeString(mIconUrl.toString());
        }

        if (mAuthType != null) {
            validDataList[MemberData.AUTH_TYPE.ordinal()] = true;
            dest.writeString(mAuthType);
        }

        if (mIconMime != null) {
            validDataList[MemberData.ICON_MIME.ordinal()] = true;
            dest.writeString(mIconMime);
        }

        if (mOrder != -1) {
            validDataList[MemberData.ORDER.ordinal()] = true;
            dest.writeInt(mOrder);
        }

        if (mName != null) {
            validDataList[MemberData.NAME.ordinal()] = true;
            dest.writeString(mName);
        }

        if (mCapabilities != null) {
            dest.writeInt(mCapabilities.size());
            for (IdentityCapability cap : mCapabilities) {
                cap.writeToParcel(dest, 0);
            }
        } else {
            dest.writeInt(0);
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // Real array.
        dest.setDataPosition(currentPos);
    }

    /** Interface to allow Identity to be written and restored from a Parcel. */
    public static final Parcelable.Creator<Identity> CREATOR = new Parcelable.Creator<Identity>() {
        public Identity createFromParcel(Parcel in) {
            return new Identity(in);
        }

        public Identity[] newArray(int size) {
            return new Identity[size];
        }
    };
}
