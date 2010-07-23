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

import java.util.Enumeration;
import java.util.Hashtable;

import android.os.Parcel;
import android.os.Parcelable;

import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType encapsulating Identity capability information issued to or
 * retrieved from server
 */
public class IdentityCapability extends BaseDataType implements Parcelable {
    /**
     * Enumeration of capabilities potentially supported by an Identity.
     */
    public enum CapabilityID {
        sync_contacts, // TODO: Called by tests only.
        get_friend_update,
        get_own_status,
        post_own_status, // TODO: Called by tests only.
        share_media,
        mail,
        chat;

        /**
         * Find CapabilityID for specified String.
         * 
         * @param name String containing name of required CapabilityID
         * @return CapabilityID for supplied String, null if not found.
         */
        private static CapabilityID find(String name) {
            for (CapabilityID ids : CapabilityID.values()) {
                if (name.compareTo(ids.name()) == 0) {
                    return ids;
                }
            }
            LogUtils.logD("CapabilityID not found: " + name);
            return null;
        }
    }

    /**
     * Tags associated with IdentityCapability item.
     */
    private enum Tags {
        IDENTITY_CAPABILITY_MAIN_TAG("identitycapability"),
        VALUE("value"),
        CAPABILITY_ID("capabilityid"),
        DESCRIPTION("description"),
        NAME("name");

        private final String tag;

        /**
         * Construct Tags item from supplied String.
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
         * @param tag String value find in Tags items.
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

    public Boolean mValue = null;

    public CapabilityID mCapability = null;

    public String mDescription = null;

    public String mName = null;

    /**
     * Default constructor.
     */
    public IdentityCapability() {
        // Do nothing.
    }

    /**
     * Create IdentityCapability from supplied Parcel.
     * 
     * @param in Parcel containing IdentityCapability.
     */
    private IdentityCapability(Parcel in) {
        readFromParcel(in);
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return IDENTITY_CAPABILITY_DATA_TYPE;
    }

    /**
     * Populate IdentityCapability from supplied Hashtable.
     * 
     * @param hash Hashtable containing identity capability details.
     * @return IdentityCapability instance.
     */
    public IdentityCapability createFromHashtable(Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = Tags.findTag(key);
            if (tag != null) {
                setValue(tag, value);
            }
        }

        return this;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object val) {
        switch (tag) {
            case CAPABILITY_ID:
                mCapability = CapabilityID.find((String)val);
                break;

            case DESCRIPTION:
                mDescription = (String)val;
                break;

            case IDENTITY_CAPABILITY_MAIN_TAG:
                break;

            case NAME:
                mName = (String)val;
                break;

            case VALUE:
                mValue = (Boolean)val;
                break;
                
            default:
                // Do nothing.
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("\tName:");
        sb.append(mName);
        sb.append("\n\tCapabilityID:");

        if (mCapability != null) {
            sb.append(mCapability.name());
        } else {
            sb.append(mCapability);
        }
        
        sb.append("\n\tValue:"); sb.append(mValue);
        sb.append("\n\tDescription:"); sb.append(mDescription);
        return sb.toString();
    }

    /**
     * Enumeration of data items written or read to a Parcel containing an
     * IdentityCapability item.
     */
    private enum MemberData {
        VALUE,
        CAPABILITY,
        DESCRIPTION,
        NAME;
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        boolean[] validDataList = new boolean[MemberData.values().length];
        int validDataPos = dest.dataPosition();
        dest.writeBooleanArray(validDataList); // Placeholder for real array.

        if (mValue != null) {
            validDataList[MemberData.VALUE.ordinal()] = true;
            boolean[] b = new boolean[1];
            b[0] = mValue;
            dest.writeBooleanArray(b);
        }
        if (mCapability != null) {
            validDataList[MemberData.CAPABILITY.ordinal()] = true;
            dest.writeString(mCapability.name());
        }
        if (mDescription != null) {
            validDataList[MemberData.DESCRIPTION.ordinal()] = true;
            dest.writeString(mDescription);
        }
        if (mName != null) {
            validDataList[MemberData.NAME.ordinal()] = true;
            dest.writeString(mName);
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // Real array.
        dest.setDataPosition(currentPos);

    }

    /**
     * Read IdentityCapability item from Parcel.
     * 
     * @param in Parcel containing IdentityCapability item.
     */
    private void readFromParcel(Parcel in) {
        mValue = null;
        mCapability = null;
        mDescription = null;
        mName = null;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.VALUE.ordinal()]) {
            boolean[] b = new boolean[1];
            in.readBooleanArray(b);
            mValue = b[0];
        }
        if (validDataList[MemberData.CAPABILITY.ordinal()]) {
            String cap = in.readString();
            mCapability = CapabilityID.valueOf(cap);
        }
        if (validDataList[MemberData.DESCRIPTION.ordinal()]) {
            mDescription = in.readString();
        }
        if (validDataList[MemberData.NAME.ordinal()]) {
            mName = in.readString();
        }
    }

    /**
     * Interface to allow IdentityCapability to be written and restored from a
     * Parcel.
     */
    public static final Parcelable.Creator<IdentityCapability> CREATOR = new Parcelable.Creator<IdentityCapability>() {
        public IdentityCapability createFromParcel(Parcel in) {
            return new IdentityCapability(in);
        }

        public IdentityCapability[] newArray(int size) {
            return new IdentityCapability[size];
        }
    };
}
