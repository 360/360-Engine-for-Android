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
 * You can obtain a copy of the license at
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * See the License for the specific language governing.
 *  permissions and limitations under the
 * License.
 * When distributing Covered Code,
 * include this CDDL HEADER in each file and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 * with the fields enclosed by brackets.
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of.
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
 *
 */
public class Content extends BaseDataType implements Parcelable {

    /**
     * contentid.
     */
    public Long contentid = null;
    /**
     * remoteid.
     */
    public String remoteid = null;
    /**
     * bytesmime.
     */
    public String bytesmime = null;
    /**
     * exfid.
     */
    public String extfid = null;
    /**
     * title.
     */
    public String title = null;
    /**
     * filename.
     */
    public String filename = null;
    /**
     * description.
     */
    public String description = null;
    /**
     * system.
     */
    public String system = null;
    /**
     * store.
     */
    public String store = null;
    /**
     * time.
     */
    public Long time = null;
    /**
     * previewurl.
     */
    public String previewurl = null;
    /**
     * bytes.
     */
    public byte[] bytes = null;
    /**
     * taglist.
     */
    public List<String> taglist = null;
    /**
     * maxage.
     */
    public Long maxage = null;
    /**
     * commnets.
     */
    public List<String> comments = null;
    /**
     * albums.
     */
    public List<String> albums = null;
    /**
     * tagscount.
     */
    public Integer tagscount = null;
    /**
     * commentscount.
     */
    public Integer commentscount = null;
    /**
     * uploadedviaapid.
     */
    public String uploadedviaappid = null;
    /**
     * uploadvistype.
     */
    public String uploadedviaapptype = null;
    /**
     * albumididlist.
     */
    public List<Long> albumidlist = null;
    /**
     * filelen.lenght if fiel.
     */
    public Long filelen;
    /**
     * fileuuid.recieved when file uploaded in chunks.
     */
    public String fileuuid;

    @Override
    public final int getType() {
        return CONTENT;
    }
    /**
     * override.
     * @param dest destination.
     * @param flags flags.
     *
     */
    public final void writeToParcel(final Parcel dest, final int flags) {
    }
    /**
     * tags.
     * @author mayank
     *
     */
    private enum Tags {
        /**
         * contentid.
         */
        CONTENTID("contentid"),
        /**
         * remoteid.
         */
        REMOTEID("remoteid"),
        /**
         * bytesmime.
         */
        BYTESMIME("bytesmime"),
        /**
         * extfid.
         */
        EXTFID("extfid"),
        /**
         * title.
         */
        TITLE("title"),
        /**
         * filename.
         */
        FILENAME("filename"),
        /**
         * description.
         */
        DESCRIPTION("description"),
        /**
         * system.
         */
        SYSTEM("system"),
        /**
         * store.
         */
        STORE("store"),
        /**
         * time.
         */
        TIME("time"),
        /**
         * previewurl.
         */
        PREVIEWURL("previewurl"),
        /**
         * bytes.
         */
        BYTES("bytes"),
        /**
         * taglist.
         */
        TAGLIST("taglist"),
        /**
         * maxage.
         */
        MAXAGE("maxage"),
        /**
         * commnets.
         */
        COMMENTS("comments"),
        /**
         * albums.
         */
        ALBUMS("albums"),
        /**
         * tagcount.
         */
        TAGSCOUNT("tagscount"),
        /**
         * commnetscount.
         */
        COMMENTSCOUNT("commentscount"),
        /**
         * uploadviappid.
         */
        UPLOADEDVIAAPPID("uploadedviaappid"),
        /**
         * uploadviatype.
         */
        UPLOADEDVIAAPTYPE("uploadedviaapptype"),
        /**
         * albumidlist.
         */
        ALBUMIDLIST("albumidlist"),
        /**
         * fileuuid.
         */
        FILEUUID("fileuuid");
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
     * creats hashtabel.
     * @return hasgtable
     */
    public final Hashtable<String, Object> createHashtable() {
        Hashtable<String, Object> htab = new Hashtable<String, Object>();

        if (remoteid != null) {
            htab.put(Tags.REMOTEID.tag(), remoteid);
        }
        if (contentid != null) {
            htab.put(Tags.CONTENTID.tag(), contentid);
        }
        if (bytesmime != null) {
            htab.put(Tags.BYTESMIME.tag(), bytesmime);
        }
        if (extfid != null) {
            htab.put(Tags.EXTFID.tag(), extfid);
        }
        if (title != null && title.length() > 0) {
            htab.put(Tags.TITLE.tag(), title);
        }
        if (filename != null) {
            htab.put(Tags.FILENAME.tag(), filename);
        }
        if (description != null) {
            htab.put(Tags.DESCRIPTION.tag(), description);
        }
        if (system != null) {
            htab.put(Tags.SYSTEM.tag(), system);
        }
        if (store != null) {
            htab.put(Tags.STORE.tag(), store);
        }
        if (time != null) {
            htab.put(Tags.TIME.tag(), time);
        }
        if (previewurl != null) {
            htab.put(Tags.PREVIEWURL.tag(), previewurl);
        }
        if (bytes != null) {
            htab.put(Tags.BYTES.tag(), bytes);
        }
        if (taglist != null) {
            htab.put(Tags.TAGLIST.tag(), taglist);
        }
        if (maxage != null) {
            htab.put(Tags.MAXAGE.tag(), maxage);
        }
        if (comments != null) {

        }
        if (albums != null) {

        }
        if (tagscount != null) {
            htab.put(Tags.TAGSCOUNT.tag(), tagscount);
        }
        if (commentscount != null) {
            htab.put(Tags.COMMENTSCOUNT.tag(), commentscount);
        }
        if (uploadedviaappid != null) {
            htab.put(Tags.UPLOADEDVIAAPPID.tag(), uploadedviaappid);
        }
        if (albumidlist != null) {
            htab.put(Tags.ALBUMIDLIST.tag(), albumidlist);
        }
        if (fileuuid != null) {
            htab.put(Tags.FILEUUID.tag(), fileuuid);
        }

        return htab;
    }
    /**
     *
     * @param tag input string.
     * @return tags.
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
   *
   * @param hash create from hasgtable.
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
     * @param value value to be set.
     */
    private void setValue(final Tags tag, final Object value) {
        LogUtils.logW("setValue:-Content Unknown tag - " + tag);
        switch (tag) {

        case CONTENTID:
            contentid = (Long) value;
            break;
        case TITLE:
            title = (String) value;
            break;
        case BYTES:
            bytes = (byte[]) value;
            break;
        case FILENAME:
            filename = (String) value;
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