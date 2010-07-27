
/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject.
 * to the terms of the Common Development and Distribution
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license
 * at src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * ###TODO:URL_PLACEHOLDER###
 * See the License for the specific
 *  language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code,
 *  include this CDDL HEADER in each file and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 *  with the fields enclosed by brackets
 * "[]" replaced with your own identifying information:
 *  Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

/*
 * Class is used to send data to the server.
 *
 */

package com.vodafone360.people.datatypes;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.os.Parcel;
import android.os.Parcelable;

import com.vodafone360.people.utils.LogUtils;

/**
 * Getting groups from server.
 * @author mayank
 *
 */
public class ResultGetGroupsForContent extends BaseDataType implements
        Parcelable {
    /**
     * items.
     */
    public Long items = null;
    /**
     * groupitem.
     */
    public List<GroupItem> itemlist = new Vector<GroupItem>();
    /**
     * name.
     */
    Long fails = null;
    /**
     * identifies the class.
     */
    public final static String NAME = "ResultGetGroupsForContent";

    @Override
    public final int getType() {
        return RESULT_GET_GROUPS;
    }
    @Override
    public final void writeToParcel(final Parcel dest, final int flags) {
    }
    /**
     * tags.
     * @author mayank
     *
     */
    private enum Tags {
        /**
         * items.
         */
        ITEMS("items"),
        /**
         * itemlist.
         */
        ITEMLIST("itemlist"),
        /**
         * fails.
         */
        FAILS("fails");
       /**
        * tags.
        */
        private final String tag;

        /**
         * Constructor for Tags item.
         *
         * @param s
         *            String value associated with Tag.
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
     * find tags.
     * @param tag input.
     * @return Tags output.
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
     * creates from hashtable.
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
     * set value.
     * @param tag tag.
     * @param value value.
     */
    private void setValue(final Tags tag, final Object value) {
        LogUtils.logW("setValue:-Content Unknown tag - " + tag);
        switch (tag) {

        case ITEMS:
            items = (Long) value;
            break;
        case ITEMLIST:
            Vector<Object> v = (Vector<Object>) value;
            for (Object obj : v) {
                GroupItem entobj = new GroupItem();
                Hashtable<String, Object> hash = (Hashtable<String, Object>) obj;
                entobj.createFromHashtable(hash);
                itemlist.add(entobj);
            }
            break;
        case FAILS:
            fails = (Long) value;
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
