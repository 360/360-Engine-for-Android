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

import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType encapsulating People Session credentials returned as a result of
 * successful sign-in/sign-up.
 */
public class AuthSessionHolder extends BaseDataType {

    /**
     * Tags associated with AuthSessionHolder representing data items associated
     * with AuthSessionHolder item returned from server.
     */
    private enum Tags {
        USER_ID("userid"),
        SESSION_SECRET("sessionsecret"),
        USER_NAME("username"),
        SESSION_ID("sessionid");

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
        public String tag() {
            return tag;
        }

    }

    public long userID;

    public String sessionSecret;

    public String userName;

    public String sessionID;

    /**
     * Find Tags item for specified String.
     * 
     * @param tag String value to find in Tags items.
     * @return Tags item for specified String, null otherwise.
     */
    private Tags findTag(String tag) {
        for (Tags tags : Tags.values()) {
            if (tag.compareTo(tags.tag()) == 0) {
                return tags;
            }
        }
        return null;
    }


    public int getType() {
        return AUTH_SESSION_HOLDER_TYPE;
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = 
            new StringBuilder("Auth Session Holder: \n  userID: \t   ");
        sb.append(userID); 
        sb.append("\n  sessionSecret: "); sb.append(sessionSecret); 
        sb.append("\n  userName: \t "); sb.append(userName); 
        sb.append("\n  sessionID: \t"); sb.append(sessionID);
        return sb.toString();
    }

    /**
     * Create AuthSessionHolder from Hash-table (generated from Hessian data).
     * 
     * @param hash Hash-table containing AuthSessionHolder data.
     * @return AuthSessionHolder generated from supplied Hash-table.
     */
    public AuthSessionHolder createFromHashtable(Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = findTag(key);

            if (null != tag) {
                setValue(tag, value);
            } else {
                LogUtils.logE("Tag was null for key: " + key);
            }
        }

        return this;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag.
     * @param val Value associated with the tag.
     */
    private void setValue(Tags tag, Object value) {
        switch (tag) {
            case SESSION_ID:
                sessionID = (String)value;
                break;

            case SESSION_SECRET:
                sessionSecret = (String)value;
                break;

            case USER_ID:
                userID = ((Long)value).longValue();
                break;

            case USER_NAME:
                userName = (String)value;
                break;
                
            default:
                // Do nothing.   
                break;
        }
    }
}
