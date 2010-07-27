/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject to the .
 * terms of the Common Development and Distribution
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at .
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or.
 * See the License for the specific language governing permissions
 * and limitations under the
 * License.
 *
 * When distributing Covered Code,
 * include this CDDL HEADER in each file
 *  and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 *  with the fields enclosed by brackets.
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.datatypes;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import android.os.Parcel;
import android.os.Parcelable;

import com.vodafone360.people.utils.LogUtils;

/**
 * Class is used to send data to the server.
 */

public class EntityKey extends BaseDataType implements Parcelable {

    /**
     * enitity list.
     */
    public Long entityid = null;
    /**
     * userid.
     */
    public Long userid = null;
    /**
     * entitytype.
     */
    public String entitytype = null;

    @Override
    public final int getType() {
        return ENTITY_KEY;
    }
    /**
     * writetoparcel.
     * @param dest destination.
     * @param flags flags.
     */
    public final void writeToParcel(
            final Parcel dest, final int flags) {
    }

    /**
     * tags.
     * @author mayank.
     *
     */
    private enum Tags {
        /**
         * entityid.
         */
        ENTITYID("entityid"),
        /**
         * userid.
         */
        USERID("userid"),
        /**
         * entitytype.
         */
        ENTITYTYPY("entitytype");
        /**
         * tag.
         */
        private final String tag;

        /**
         * Constructor for Tags item.
         *
         * @param s
         *  String value associated with Tag.
         */
        private Tags(final String s) {
            tag = s;
         }

        /**
         * String value associated with Tags item.
         *
         * @return String value associated with Tags item.
         */
        private String tag() {
            return tag;
        }

    };
    /**
     *
     * @return hashtable
     */
    public final Hashtable<String, Object> createHashtable() {
        Hashtable<String, Object> htab = new Hashtable<String, Object>();

        if (entityid != null) {
            htab.put(Tags.ENTITYID.tag(), entityid);
        }
        if (userid != null) {
            htab.put(Tags.USERID.tag(), userid);
        }
        if (entitytype != null) {
            htab.put(Tags.ENTITYTYPY.tag(), entitytype);
        }
        return htab;
    }
    /**
     *
     * @param tag as input.
     * @return Tag for input.
     */
    private Tags findTag(final String tag) {
        for (Tags tags : Tags.values()) {
            if (tag.compareTo(tags.tag()) == 0) {
                return tags;
            }
        }
        return null;
    }
    /**
     * creates from hashtable ie from server.
     * @param hash input
     */
    public final void createFromHashtable(
            final Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = findTag(key);
            if (tag != null) {
                setValue(tag, value);
            } else {
                LogUtils.logD("Unhandled Key = " + key + "value = "
                        + value.toString());
            }
        }

        return;
    }
    /**
     *
     * @param tag input.
     * @param value input.
     */
    private void setValue(final Tags tag, final Object value) {
        LogUtils.logW("setValue:-Content Unknown tag - " + tag);
        switch (tag) {

        case ENTITYID:
            entityid = (Long) value;
            break;
        case ENTITYTYPY:
            entitytype = (String) value;
            break;

        default:
            LogUtils.logW("setValue:-Content Unknown tag - " + tag + "["
                    + value + "]");
        }
    }

    @Override
    public final int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }
}