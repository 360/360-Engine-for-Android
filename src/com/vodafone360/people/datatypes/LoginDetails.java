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

/**
 * BaseDataType encapsulating login details.
 */
public class LoginDetails implements Parcelable {

    public String mUsername = "";

    public String mPassword = "";

    public String mMobileNo = "";

    public boolean mRememberMe = true;

    public boolean mAutoConnect = true;

    public String mSubscriberId = "";

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Name: ");
        sb.append(mUsername); 
        sb.append("\nPassword: "); sb.append(mPassword);
        sb.append("Mobile No: "); sb.append(mMobileNo); 
        sb.append("\nRemember Me: ");  sb.append(mRememberMe);
        sb.append("\nAuto Connect: "); sb.append(mAutoConnect); 
        sb.append("\nSubscriber ID: "); sb.append(mSubscriberId);
        return sb.toString();
    }

    /**
     * Copy LoginDetails item.
     * 
     * @param source LoginDetails item to copy.
     */
    public void copy(LoginDetails source) {
        android.os.Parcel _data = android.os.Parcel.obtain();
        source.writeToParcel(_data, 0);
        _data.setDataPosition(0);
        readFromParcel(_data);
    }

    /**
     * Default constructor.
     */
    public LoginDetails() {
    }

    /**
     * Enumeration of data items in LoginDetails Parcel.
     */
    private enum MemberData {
        USERNAME,
        PASSWORD,
        MOBILE_NO,
        SUBSCRIBER_ID;
    }

    /**
     * Read LoginDetails item from supplied Parcel.
     * 
     * @param in Parcel containing LoginDetails.
     */
    private void readFromParcel(Parcel in) {
        mUsername = null;
        mPassword = null;
        mMobileNo = null;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.USERNAME.ordinal()]) {
            mUsername = in.readString();
        }

        if (validDataList[MemberData.PASSWORD.ordinal()]) {
            mPassword = in.readString();
        }

        if (validDataList[MemberData.MOBILE_NO.ordinal()]) {
            mMobileNo = in.readString();
        }

        if (validDataList[MemberData.SUBSCRIBER_ID.ordinal()]) {
            mSubscriberId = in.readString();
        }

        mRememberMe = (in.readByte() == 0 ? false : true);
        mAutoConnect = (in.readByte() == 0 ? false : true);
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
        dest.writeBooleanArray(validDataList); // Placeholder for real array

        if (mUsername != null) {
            validDataList[MemberData.USERNAME.ordinal()] = true;
            dest.writeString(mUsername);
        }

        if (mPassword != null) {
            validDataList[MemberData.PASSWORD.ordinal()] = true;
            dest.writeString(mPassword);
        }

        if (mMobileNo != null) {
            validDataList[MemberData.MOBILE_NO.ordinal()] = true;
            dest.writeString(mMobileNo);
        }

        if (mSubscriberId != null) {
            validDataList[MemberData.SUBSCRIBER_ID.ordinal()] = true;
            dest.writeString(mSubscriberId);
        }

        dest.writeByte((byte)(mRememberMe ? 1 : 0));
        dest.writeByte((byte)(mAutoConnect ? 1 : 0));

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // real array
        dest.setDataPosition(currentPos);
    }
}
