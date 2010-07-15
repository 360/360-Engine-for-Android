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
 * BaseDataType encapsulating details required for Registration (sign-up) to a
 * new People account.
 */
public class RegistrationDetails implements Parcelable {
    public String mFullname = null;

    public String mUsername = null;

    public String mPassword = null;

    public String mEmail = null;

    public String mBirthdayDate = null;

    public String mMsisdn = null;

    public Boolean mAcceptedTAndC = null;

    public String mCountrycode = null;

    public String mTimezone = null;

    public String mLanguage = null;

    public Long mMobileOperatorId = null;

    public Long mMobileModelId = null;

    public Boolean mSendConfirmationMail = null;

    public Boolean mSendConfirmationSms = null;

    public Boolean mSubscribeToNewsLetter = null;

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Full Name: ");
        sb.append(mFullname);
        sb.append("\nUsername: "); sb.append(mUsername);
        sb.append("\nPassword: "); sb.append(mPassword); 
        sb.append("\nEmail: "); sb.append(mEmail); 
        sb.append("\nBirthday: "); sb.append(mBirthdayDate); 
        sb.append("\nMsisdn: "); sb.append(mMsisdn); 
        sb.append("\nAccepted Terms/Conditions: "); sb.append(mAcceptedTAndC); 
        sb.append("\nCountry Code: "); sb.append(mCountrycode); 
        sb.append("\nTime zone: "); sb.append(mTimezone); 
        sb.append("\nLanguage: "); sb.append(mLanguage);
        sb.append("\nMobile Operation Id: "); sb.append(mMobileOperatorId);
        sb.append("\nMobile Mode Id: "); sb.append(mMobileModelId); 
        sb.append("\nSend Confirmation Mail: "); sb.append(mSendConfirmationMail); 
        sb.append("\nSend Confirmation SMS: "); sb.append(mSendConfirmationSms); 
        sb.append("\nSubscribe to news letter: "); sb.append(mSubscribeToNewsLetter); 
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Copy RegistrationDetails item.
     * 
     * @param source RegistrationDetails item to copy.
     */
    public void copy(RegistrationDetails source) {
        android.os.Parcel _data = android.os.Parcel.obtain();
        source.writeToParcel(_data, 0);
        _data.setDataPosition(0);
        readFromParcel(_data);
    }

    /**
     * Default constructor.
     */
    public RegistrationDetails() {
    }

    /**
     * Enumeration of data items in RegistrationDetails Parcel.
     */
    private enum MemberData {
        FULLNAME,
        USERNAME,
        PASSWORD,
        EMAIL,
        BIRTHDAY_DATE,
        MSISDN,
        ACCEPTED_T_AND_C,
        COUNTRY_CODE,
        TIME_ZONE,
        LANGUAGE,
        MOBILE_OPERATOR_ID,
        MOBILE_MODEL_ID,
        SEND_CONFIRMATION_MAIL,
        SEND_CONFIRMATION_SMS,
        SUBSCRIBE_TO_NEWSLETTER;
    }

    /**
     * Read RegistrationDetails item from supplied Parcel.
     * 
     * @param in Parcel containing RegistrationDetails.
     */
    private void readFromParcel(Parcel in) {
        mFullname = null;
        mUsername = null;
        mPassword = null;
        mEmail = null;
        mBirthdayDate = null;
        mMsisdn = null;
        mAcceptedTAndC = null;
        mCountrycode = null;
        mTimezone = null;
        mLanguage = null;
        mMobileOperatorId = null;
        mMobileModelId = null;
        mSendConfirmationMail = null;
        mSendConfirmationSms = null;
        mSubscribeToNewsLetter = null;

        boolean[] validDataList = new boolean[MemberData.values().length];
        in.readBooleanArray(validDataList);

        if (validDataList[MemberData.FULLNAME.ordinal()]) {
            mFullname = in.readString();
        }

        if (validDataList[MemberData.USERNAME.ordinal()]) {
            mUsername = in.readString();
        }

        if (validDataList[MemberData.PASSWORD.ordinal()]) {
            mPassword = in.readString();
        }

        if (validDataList[MemberData.EMAIL.ordinal()]) {
            mEmail = in.readString();
        }

        if (validDataList[MemberData.BIRTHDAY_DATE.ordinal()]) {
            mBirthdayDate = in.readString();
        }

        if (validDataList[MemberData.MSISDN.ordinal()]) {
            mMsisdn = in.readString();
        }

        if (validDataList[MemberData.ACCEPTED_T_AND_C.ordinal()]) {
            mAcceptedTAndC = (in.readByte() == 0 ? false : true);
        }

        if (validDataList[MemberData.COUNTRY_CODE.ordinal()]) {
            mCountrycode = in.readString();
        }

        if (validDataList[MemberData.TIME_ZONE.ordinal()]) {
            mTimezone = in.readString();
        }

        if (validDataList[MemberData.LANGUAGE.ordinal()]) {
            mLanguage = in.readString();
        }

        if (validDataList[MemberData.MOBILE_OPERATOR_ID.ordinal()]) {
            mMobileOperatorId = in.readLong();
        }

        if (validDataList[MemberData.MOBILE_MODEL_ID.ordinal()]) {
            mMobileModelId = in.readLong();
        }

        if (validDataList[MemberData.SEND_CONFIRMATION_MAIL.ordinal()]) {
            mSendConfirmationMail = (in.readByte() == 0 ? false : true);
        }

        if (validDataList[MemberData.SEND_CONFIRMATION_SMS.ordinal()]) {
            mSendConfirmationSms = (in.readByte() == 0 ? false : true);
        }

        if (validDataList[MemberData.SUBSCRIBE_TO_NEWSLETTER.ordinal()]) {
            mSubscribeToNewsLetter = (in.readByte() == 0 ? false : true);
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
        dest.writeBooleanArray(validDataList); // Placeholder for real array.

        if (mFullname != null) {
            validDataList[MemberData.FULLNAME.ordinal()] = true;
            dest.writeString(mFullname);
        }

        if (mUsername != null) {
            validDataList[MemberData.USERNAME.ordinal()] = true;
            dest.writeString(mUsername);
        }

        if (mPassword != null) {
            validDataList[MemberData.PASSWORD.ordinal()] = true;
            dest.writeString(mPassword);
        }

        if (mEmail != null) {
            validDataList[MemberData.EMAIL.ordinal()] = true;
            dest.writeString(mEmail);
        }

        if (mBirthdayDate != null) {
            validDataList[MemberData.BIRTHDAY_DATE.ordinal()] = true;
            dest.writeString(mBirthdayDate);
        }

        if (mMsisdn != null) {
            validDataList[MemberData.MSISDN.ordinal()] = true;
            dest.writeString(mMsisdn);
        }

        if (mAcceptedTAndC != null) {
            validDataList[MemberData.ACCEPTED_T_AND_C.ordinal()] = true;
            dest.writeByte((byte)(mAcceptedTAndC ? 1 : 0));
        }

        if (mCountrycode != null) {
            validDataList[MemberData.COUNTRY_CODE.ordinal()] = true;
            dest.writeString(mCountrycode);
        }

        if (mTimezone != null) {
            validDataList[MemberData.TIME_ZONE.ordinal()] = true;
            dest.writeString(mTimezone);
        }

        if (mLanguage != null) {
            validDataList[MemberData.LANGUAGE.ordinal()] = true;
            dest.writeString(mLanguage);
        }

        if (mMobileOperatorId != null) {
            validDataList[MemberData.MOBILE_OPERATOR_ID.ordinal()] = true;
            dest.writeLong(mMobileOperatorId);
        }

        if (mMobileModelId != null) {
            validDataList[MemberData.MOBILE_MODEL_ID.ordinal()] = true;
            dest.writeLong(mMobileModelId);
        }

        if (mSendConfirmationMail != null) {
            validDataList[MemberData.SEND_CONFIRMATION_MAIL.ordinal()] = true;
            dest.writeByte((byte)(mSendConfirmationMail ? 1 : 0));
        }

        if (mSendConfirmationSms != null) {
            validDataList[MemberData.SEND_CONFIRMATION_SMS.ordinal()] = true;
            dest.writeByte((byte)(mSendConfirmationSms ? 1 : 0));
        }

        if (mSubscribeToNewsLetter != null) {
            validDataList[MemberData.SUBSCRIBE_TO_NEWSLETTER.ordinal()] = true;
            dest.writeByte((byte)(mSubscribeToNewsLetter ? 1 : 0));
        }

        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // Real array.
        dest.setDataPosition(currentPos);
    }
}
