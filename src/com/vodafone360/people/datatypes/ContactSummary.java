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

import android.os.Parcel;
import android.os.Parcelable;

import com.vodafone360.people.datatypes.ContactDetail.DetailKeyTypes;

/**
 * BaseDataType encapsulating ContactSummary.
 */
public class ContactSummary implements Parcelable {
    /**
     * Enumeration of online (Presence) status values.
     */
    public enum OnlineStatus {
        /**
         * The order of items below is important, 
         * because the decision upon setting user overall presence status is taken 
         * based on the "strongest" (biggest) presence status of all user TPC networks.
         * I.e. if google=offline, mobile=invisible, hyves=online -> overall: online.
         * There's normally a "for" loopto find the max element.
         */
        OFFLINE("offline"), // the "lowest" status goes first
        INVISIBLE("invisible"),
        IDLE("idle"),
        ONLINE("online"); // the "highest" status


        /** The name of the field as it appears in the database. **/
        private String mOnlineStatus;

        /**
         * Construct OnlineStatus item from supplied String
         * 
         * @param s String value for OnlineStatus item.
         */
        private OnlineStatus(String field) {
            mOnlineStatus = field;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return mOnlineStatus;
        }

        /**
         * Obtain OnlineStatus value for supplied String.
         * 
         * @param String identifying online status.
         * @return OnlineStatus value for supplied String, null if OnlineStatus
         *         does not exist.
         */
        public static OnlineStatus getValue(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Obtain OnlineStatus value for supplied index.
         * 
         * @index Index of item in array array of OnlineStatus items.
         * @return OnlineStatus value for supplied index, null if OnlineStatus
         *         does not exist.
         */
        public static OnlineStatus getValue(int index) {
            try {
                return values()[index];
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Enumeration of types available for ContactSummary's 'alt' field.
     */
    public enum AltFieldType {
        UNUSED,
        NAME,
        STATUS
    }

    public Long summaryID = null;

    public Long localContactID = null;

    public String formattedName = null;

    public String statusText = null;

    public AltFieldType altFieldType = null;

    public DetailKeyTypes altDetailType = null;

    public Integer nativeContactId = null;

    public OnlineStatus onlineStatus = null;

    public boolean friendOfMine = false;

    public boolean pictureLoaded = false;

    /** Name of Social Networking Site this Contact is associated with. */
    public String sns = null;

    /**
     * Sync to phone flag indicates whether Contact is sync'ed to Phonebook
     * (i.e. Native)
     */
    public boolean synctophone = false;

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SummaryID: ");
        sb.append(summaryID);
        sb.append("ContactID: "); sb.append(localContactID);
        sb.append("\nName: "); sb.append(formattedName); 
        sb.append("\nStatus: "); sb.append(statusText); 
        sb.append("\nAlt Field Type: "); sb.append(altFieldType); 
        sb.append("\nAlt Detail Type: "); sb.append(altDetailType);
        sb.append("\nNative Contact ID: "); sb.append(nativeContactId); 
        sb.append("\nOnline status: "); sb.append(onlineStatus); 
        sb.append("\nFriend of mine: "); sb.append(friendOfMine); 
        sb.append("\nProfile picture loaded: "); sb.append(pictureLoaded); 
        sb.append("\nSocial Networking Site: "); sb.append(sns); 
        sb.append("\nSynctophone: "); sb.append(synctophone);
        return sb.toString();
    }

    /**
     * Enumeration of items written to ContactSummmary Parcel.
     */
    private enum MemberData {
        LOCAL_CONTACT_ID,
        FORMATTED_NAME,
        STATUS_TEXT,
        ALT_FIELD_TYPE,
        ALT_DETAIL_TYPE,
        NATIVE_ID,
        ONLINE_STATUS,
        SNS;
    }

    private final static int NO_OF_BOOLEANS = 3;

    /**
     * Read ContactSummary from Parcel
     * 
     * @param in Parcel containing ContactSummary.
     */
    private void readFromParcel(Parcel in) {
        localContactID = null;
        formattedName = null;
        statusText = null;
        altFieldType = null;
        altDetailType = null;
        nativeContactId = null;
        friendOfMine = false;
        pictureLoaded = false;
        synctophone = false;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.LOCAL_CONTACT_ID.ordinal()]) {
            localContactID = in.readLong();
        }

        if (validDataList[MemberData.FORMATTED_NAME.ordinal()]) {
            formattedName = in.readString();
        }

        if (validDataList[MemberData.STATUS_TEXT.ordinal()]) {
            statusText = in.readString();
        }

        if (validDataList[MemberData.ALT_FIELD_TYPE.ordinal()]) {
            final int val = in.readInt();
            if (val < AltFieldType.values().length) {
                altFieldType = AltFieldType.values()[val];
            }
        }

        if (validDataList[MemberData.ALT_DETAIL_TYPE.ordinal()]) {
            final int val = in.readInt();
            if (val < ContactDetail.DetailKeyTypes.values().length) {
                altDetailType = ContactDetail.DetailKeyTypes.values()[val];
            }
        }

        if (validDataList[MemberData.NATIVE_ID.ordinal()]) {
            nativeContactId = in.readInt();
        }

        if (validDataList[MemberData.ONLINE_STATUS.ordinal()]) {
            final int val = in.readInt();
            if (val < OnlineStatus.values().length) {
                onlineStatus = OnlineStatus.values()[val];
            }
        }
        boolean[] boolArray = new boolean[NO_OF_BOOLEANS];
        in.readBooleanArray(boolArray);
        friendOfMine = boolArray[0];
        pictureLoaded = boolArray[1];
        synctophone = boolArray[2];
        if (validDataList[MemberData.SNS.ordinal()]) {
            sns = in.readString();
        }
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
        dest.writeBooleanArray(validDataList); // placeholder for real array

        if (localContactID != null) {
            validDataList[MemberData.LOCAL_CONTACT_ID.ordinal()] = true;
            dest.writeLong(localContactID);
        }

        if (formattedName != null) {
            validDataList[MemberData.FORMATTED_NAME.ordinal()] = true;
            dest.writeString(formattedName);
        }

        if (statusText != null) {
            validDataList[MemberData.STATUS_TEXT.ordinal()] = true;
            dest.writeString(statusText);
        }

        if (altFieldType != null) {
            validDataList[MemberData.ALT_FIELD_TYPE.ordinal()] = true;
            dest.writeInt(altFieldType.ordinal());
        }

        if (altDetailType != null) {
            validDataList[MemberData.ALT_DETAIL_TYPE.ordinal()] = true;
            dest.writeInt(altDetailType.ordinal());
        }

        if (nativeContactId != null) {
            validDataList[MemberData.NATIVE_ID.ordinal()] = true;
            dest.writeInt(nativeContactId);
        }

        if (onlineStatus != null) {
            validDataList[MemberData.ONLINE_STATUS.ordinal()] = true;
            dest.writeInt(onlineStatus.ordinal());
        }

        boolean[] boolArray = {
                friendOfMine, pictureLoaded, synctophone
        };
        dest.writeBooleanArray(boolArray);

        if (sns != null) {
            validDataList[MemberData.SNS.ordinal()] = true;
            dest.writeString(sns);
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // real array
        dest.setDataPosition(currentPos);
    }

    /**
     * Make a copy of supplied ContactSummary
     * 
     * @param source ContactSummary item to copy.
     */
    public void copy(ContactSummary source) {
        android.os.Parcel _data = android.os.Parcel.obtain();
        source.writeToParcel(_data, 0);
        _data.setDataPosition(0);
        readFromParcel(_data);
    }
}
