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
 * BaseDataType encapsulating a Status message returned from server
 */
public class StatusMsg extends BaseDataType implements Parcelable {

    /**
     * Enumeration of Tags for StatusMsg item.
     */
    private enum Tags {
        STATUS("status"),
        DRYRUN("dryrun"),
        STAT("stat");

        private final String tag;

        /**
         * Constructor creating Tags item for specified String.
         * 
         * @param s String value for Tags item.
         */
        private Tags(String s) {
            tag = s;
        }

        /**
         * String value associated with Tags item.
         * 
         * @return String value for Tags item.
         */
        private String tag() {
            return tag;
        }

        /**
         * Find Tags item for specified String
         * 
         * @param tag String value to find Tags item for
         * @return Tags item for specified String, null otherwise
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

    public Boolean mStatus = null;

    public Boolean mDryRun = null;

    public String mCode = null;

    public final String mError = null;

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return STATUS_MSG_DATA_TYPE;
    }

    /**
     * Populate Identity from supplied Hashtable.
     * 
     * @param hash Hashtable containing identity details
     * @return Identity instance
     */
    public StatusMsg createFromHashtable(Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Tags tag = Tags.findTag(key);
            if (tag != null)
                setValue(tag, hash.get(key));
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
            case STATUS:
                mStatus = (Boolean)val;
                break;

            case DRYRUN:
                mDryRun = (Boolean)val;
                break;

            case STAT:
                if (((String)val).compareTo("ok") == 0) {
                    mStatus = true;
                } else {
                    LogUtils.logD("Status different than ok. Status = " + val);
                    mStatus = false;
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
        final StringBuffer sb = new StringBuffer("Status Message:\n\tstatus = ");
        sb.append(mStatus);
        sb.append("\n\tdryrun = "); sb.append(mDryRun);
        sb.append("\n\tCode = "); sb.append(mCode);
        sb.append("\n\tErr = "); sb.append(mError);
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Object[] ba = new Object[2];
        ba[0] = mStatus;
        ba[1] = mDryRun;
        dest.writeArray(ba);
        dest.writeString(mCode);
        dest.writeString(mError);
    }
}
