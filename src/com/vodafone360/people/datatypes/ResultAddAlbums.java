/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject.
 * to the terms of the Common Development and Distribution.
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at.
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or.
 * See the License for the specific language.
 *  governing permissions and limitations under th.
 * License.
 *
 * When distributing Covered Code,
 * include this CDDL HEADER in each file and include the License.
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 * with the fields enclosed by brackets.
 * "[]" replaced with your own identifying information.
 * : Portions Copyright [yyyy] [name of.
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
import java.util.Vector;

import android.os.Parcel;

import com.vodafone360.people.utils.LogUtils;
/**
 * Result of adding albums.
 * @author mayank
 *
 */
public class ResultAddAlbums extends BaseDataType {
    /**
     * Identifies the class.
     */
    public static final String NAME = "ResultAddAlbums";
    /**
     * tags.
     * @author mayank.
     *
     */
    private enum Tags {
        /**
         * albumidlist recieved from server.
         */
        ALBUMIDLIST("albumidlist");
        /**
         * tag.
         */
        private final String tag;

        /**
         * Constructor creating Tags item for specified String.
         *
         * @param s
         *            String value for Tags item.
         */
        private Tags(final String s) {
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
    }

    @Override
    public final int getType() {
        return RESULT_ADD_ALBUM;
    }
    /**
     * find tag.
     * @param tag input.
     * @return tags output.
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
     * list got from server.
     */
    public List<Long> listofalbumIds = null;
    /**
     * creates from hashtable recieved from server.
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
     * set the value.
     * @param tag input.
     * @param value input.
     */
    private void setValue(final Tags tag, final Object value) {
        switch (tag) {

        case ALBUMIDLIST:
            listofalbumIds = (List<Long>) value;
            break;

        default:
            LogUtils.logW("setValue: Unknown tag - " + tag + "[" + value + "]");
        }
    }

}