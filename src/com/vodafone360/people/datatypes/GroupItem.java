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

import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * BaseDataType encapsulating a Group item issued to or retrieved from server.
 */
public class GroupItem extends BaseDataType implements Parcelable {

    /**
     * Tags associated with GroupItem item.
     */
    private enum Tags {
        GROUP_TYPE("grouptype"),
        USER_ID("userid"),
        IS_READ_ONLY("isreadonly"),
        IS_SMART_GROUP("issmartgroup"),
        NAME("name"),
        REQUIRESLOCALISATION("requireslocalisation"),
        ID("id"),
        IS_SYSTEM_GROUP("issystemgroup"),
        COLOR("color");

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

    public Integer mGroupType = null;

    public Boolean mIsReadOnly = null;

    public Boolean mRequiresLocalisation = null;

    public Boolean mIsSystemGroup = null;

    public Boolean mIsSmartGroup = null;

    public Long mLocalGroupId = null;

    /** Main fields defined in documentation, used for SeGroups */
    public Long mId = null;

    public Long mUserId = null;

    public String mName = null;

    public String mImageMimeType = null;

    public ByteBuffer mImageBytes = null;

    public String mColor = null;

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return GROUP_ITEM_DATA_TYPE;
    }

    /**
     * Populate GroupItem from supplied Hashtable.
     * 
     * @param hash Hashtable containing identity capability details
     * @return GroupItem instance
     */
    public GroupItem createFromHashtable(Hashtable<String, Object> hash) {
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
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object val) {
        switch (tag) {
            case GROUP_TYPE:
                mGroupType = (Integer)val;
                break;

            case ID:
                mId = (Long)val;
                break;

            case IS_READ_ONLY:
                mIsReadOnly = (Boolean)val;
                break;

            case IS_SYSTEM_GROUP:
                mIsSystemGroup = (Boolean)val;
                break;

            case NAME:
                mName = (String)val;
                break;

            case REQUIRESLOCALISATION:
                mRequiresLocalisation = (Boolean)val;
                break;

            case USER_ID:
                mUserId = (Long)val;
                break;

            case COLOR:
                mColor = (String)val;
                break;

            case IS_SMART_GROUP:
                mIsSmartGroup = (Boolean)val;
                break;
                
            default:
                // Do nothing.
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = 
            new StringBuffer("Group Item:\n\tGroup type:"); 
        sb.append(mGroupType);
        sb.append("\n\tUser ID:"); sb.append(mUserId);
        sb.append("\n\tIs read only:"); sb.append(mIsReadOnly);
        sb.append("\n\tName:"); sb.append(mName);
        sb.append("\n\tID:"); sb.append(mId);
        sb.append("\n\tRequaries Localisation:"); sb.append(mGroupType);
        sb.append("\n\tIs system group:"); sb.append(mIsSystemGroup);
        sb.append("\n\tIs smart group:"); sb.append(mIsSmartGroup);
        sb.append("\n\tColor:"); sb.append(mColor);
        return sb.toString();
    }

    /**
     * Create Hashtable from GroupItem parameters.
     * 
     * @return Hashtable generated from GroupItem parameters.
     */
    public Hashtable<String, Object> createHashtable() {
        Hashtable<String, Object> htab = new Hashtable<String, Object>();

        if (mId != null) {
            htab.put(Tags.ID.tag(), mId);
        }
        if (mUserId != null) {
            htab.put(Tags.USER_ID.tag(), mUserId);
        }
        if (mName != null) {
            htab.put(Tags.NAME.tag(), mName);
        }
        if (mImageMimeType != null) {
            htab.put("imagemimetype", mImageMimeType);
        }
        if (mImageBytes != null) {
            htab.put("imagebytes", mImageBytes);
        }
        if (mColor != null) {
            htab.put(Tags.COLOR.tag(), mColor);
        }

        return htab;
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 1;
    }

    /**
     * Enumeration containing items contained within GroupItem Parcel.
     */
    private enum MemberData {
        GROUP_TYPE,
        IS_READ_ONLY,
        REQUIRES_LOCALISATION,
        IS_SYSTEM_GROUP,
        IS_SMART_GROUP,
        LOCAL_GROUP_ID,
        ID,
        USER_ID,
        NAME,
        IMAGE_MIME_TYPE,
        IMAGE_BYTES,
        COLOR;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        boolean[] validDataList = new boolean[MemberData.values().length];
        int validDataPos = dest.dataPosition();
        dest.writeBooleanArray(validDataList); // Placeholder for real array.
        if (mGroupType != null) {
            validDataList[MemberData.GROUP_TYPE.ordinal()] = true;
            dest.writeInt(mGroupType);
        }
        final Boolean[] boolArray = new Boolean[4];
        if (mIsReadOnly != null) {
            validDataList[MemberData.IS_READ_ONLY.ordinal()] = true;
            boolArray[0] = mIsReadOnly;
        }
        if (mRequiresLocalisation != null) {
            validDataList[MemberData.REQUIRES_LOCALISATION.ordinal()] = true;
            boolArray[1] = mRequiresLocalisation;
        }
        if (mIsSystemGroup != null) {
            validDataList[MemberData.IS_SYSTEM_GROUP.ordinal()] = true;
            boolArray[2] = mIsSystemGroup;
        }
        if (mIsSmartGroup != null) {
            validDataList[MemberData.IS_SMART_GROUP.ordinal()] = true;
            boolArray[3] = mIsSmartGroup;
        }
        dest.writeArray(boolArray);
        if (mLocalGroupId != null) {
            validDataList[MemberData.LOCAL_GROUP_ID.ordinal()] = true;
            dest.writeLong(mLocalGroupId);
        }
        if (mId != null) {
            validDataList[MemberData.ID.ordinal()] = true;
            dest.writeLong(mId);
        }
        if (mUserId != null) {
            validDataList[MemberData.USER_ID.ordinal()] = true;
            dest.writeLong(mUserId);
        }
        if (mName != null) {
            validDataList[MemberData.NAME.ordinal()] = true;
            dest.writeString(mName);
        }
        if (mImageMimeType != null) {
            validDataList[MemberData.IMAGE_MIME_TYPE.ordinal()] = true;
            dest.writeString(mImageMimeType);
        }
        if (mImageBytes != null) {
            validDataList[MemberData.IMAGE_BYTES.ordinal()] = true;
            dest.writeByteArray(mImageBytes.array());
        }
        if (mColor != null) {
            validDataList[MemberData.COLOR.ordinal()] = true;
            dest.writeString(mColor);
        }
        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // Real array.
        dest.setDataPosition(currentPos);
    }
}
