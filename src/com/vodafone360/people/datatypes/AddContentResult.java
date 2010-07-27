/* package com.vodafone360.people.datatypes;
 *
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 * The contents of this file are subject to the.
 * terms of the Common Development and Distribution.
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at.
 * src/com/vodafone/people/VODAFONE.LICENSE.txt.
 *  or
 *See the License for the specific language.
 * governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code,
 * include this CDDL HEADER in each file.
 * and include the License.
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below.
 * this CDDL HEADER, with the fields enclosed by brackets.
 * "[]" replaced with your own identifying information.
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
import com.vodafone360.people.utils.LogUtils;

/*
 * Class is used for getting the result in hessian decoder
 */

/**
 * BaseDataType encapsulating list of content ids received from Content server.
 */
public class AddContentResult extends BaseDataType {
    /**
     * Used to identify the class.
     */
    public static final String NAME = "AddContentResult";

    /**
     * Tags for fields associated with AddContentResult items.
     */

    private enum Tags {
        /**
         * contentidlist list of contents. returned from server.
         */
        CONTENTIDLIST("contentidlist"),
        /**
         * fileuuid recieved from server.
         */
        FILEUUID("fileuuid");
        /**
         * tag used to identiy returns from server.
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

    /**
     * list of long to be filled in set value.
     */
    public List<Long> list = null;
    /**
     * fileuuid to be filled in setvale.
     */
    public String fileuuid = null;

    /**
     *
     * Find Tags item for specified String.
     *
     * @param tag
     *            String value to search for in Tag
     * @return Tags item for specified String, NULL otherwise.
     */
    private Tags findTag(final String tag) {
        for (Tags tags : Tags.values()) {
            if (tag.compareTo(tags.tag()) == 0) {
                return tags;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public final int getType() {
        return RESULT_ADD_CONTENT;

    }

    /** {@inheritDoc} */

    /**
     * Create AddContentResult from Hashtable.
     *
     * @param hash
     *            Hashtable containing AddContentResults information.
     * @return AddContentResults generated from Hashtable.
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
                LogUtils.logD("Unhandled Key " + "= " + key + "value = "
                        + value.toString());
            }
        }

        return;
    }

    /**
     * Sets the value of the member data. item associated with the specified
     * tag.
     *
     * @param tag
     *            Current tag
     * @param value
     *            Value associated with the tag
     */
    @SuppressWarnings("unchecked")
    private void setValue(final Tags tag, final Object value) {

        switch (tag) {

        case CONTENTIDLIST:
            list = (List<Long>) value;
            break;
        case FILEUUID:
            fileuuid = (String) value;
            break;
        default:
            LogUtils.logW("setValue: Unknown tag - " + value);
        }
    }

}