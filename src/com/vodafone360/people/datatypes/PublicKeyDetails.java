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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * BaseDataType encapsulating Public key details retrieved from or issued to
 * People server.
 */
public class PublicKeyDetails extends BaseDataType implements Parcelable {
    /**
     * Tags associated with PublicKeyDetails item.
     */
    private enum Tags {
        EXPONENTIAL("exponential"),
        MODULUS("modulo"),
        KEYBASE64("keybase64"),
        KEYX509("key");

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
    }

    public byte[] mExponential = null; // Primary key in database.

    public byte[] mModulus = null;

    public byte[] mKeyX509 = null;

    public String mKeyBase64 = null;

    /**
     * Find Tags item for specified String.
     * 
     * @param tag String value to find Tags item for
     * @return Tags item for specified String, null otherwise
     */
    private Tags findTag(String tag) {
        for (Tags tags : Tags.values()) {
            if (tag.compareTo(tags.tag()) == 0) {
                return tags;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return PUBLIC_KEY_DETAILS_DATA_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("--\nPublic Key data:");
        sb.append("\n\tExponential: ").append(Arrays.toString(mExponential)).append(
                "\n\tModulus: " + Arrays.toString(mModulus)).append("\n\tBase64: ").append(
                mKeyBase64).append("\n\t X509: ").append(Arrays.toString(mKeyX509)).append(
                "\n--------------------------------------------------");
        return sb.toString();
    }

    /**
     * Create PublicKeyDetails from Hashtable.
     * 
     * @param hash Hashtable containing Public key information.
     * @return PublicKeyDetails generated from HAshtable.
     */
    public static PublicKeyDetails createFromHashtable(Hashtable<String, Object> hash) {
        PublicKeyDetails pKey = new PublicKeyDetails();
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = pKey.findTag(key);
            pKey.setValue(tag, value);
        }

        return pKey;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param value Value associated with the tag
     */
    private void setValue(Tags tag, Object value) {
        switch (tag) {
            case EXPONENTIAL:
                mExponential = (byte[])value;
                break;

            case MODULUS:
                mModulus = (byte[])value;
                break;

            case KEYBASE64:
                mKeyBase64 = (String)value;
                break;

            case KEYX509:
                mKeyX509 = (byte[])value;
                break;
            
            default:
                // Do nothing.
                break;
        }
    }

    public PublicKeyDetails() {
        // Do nothing.
    }

    /**
     * Enumeration of data items in PublicKeyDetails PArcel.
     */
    private enum MemberData {
        EXPONENTIAL,
        MODULUS,
        KEYBASE64,
        KEYX509;
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
        if (mExponential != null) {
            validDataList[MemberData.EXPONENTIAL.ordinal()] = true;
            dest.writeInt(mExponential.length);
            dest.writeByteArray(mExponential);
        }
        if (mModulus != null) {
            validDataList[MemberData.MODULUS.ordinal()] = true;
            dest.writeInt(mModulus.length);
            dest.writeByteArray(mModulus);
        }
        if (mKeyBase64 != null) {
            validDataList[MemberData.KEYBASE64.ordinal()] = true;
            dest.writeString(mKeyBase64);
        }
        if (mKeyX509 != null) {
            validDataList[MemberData.KEYX509.ordinal()] = true;
            dest.writeInt(mKeyX509.length);
            dest.writeByteArray(mKeyX509);
        }
        int currentPos = dest.dataPosition();
        dest.setDataPosition(validDataPos);
        dest.writeBooleanArray(validDataList); // Real array.
        dest.setDataPosition(currentPos);
    }
}
