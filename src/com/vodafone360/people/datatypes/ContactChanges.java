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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType representing Contact change information retrieved from server.
 */
public class ContactChanges extends BaseDataType {

    /**
     * Tags associated with ContactChanges item.
     */
    private enum Tags {
        CURRENT_SERVER_VERSION("currentserverrevision"),
        SERVER_REVISION_ANCHOR("serverrevisionanchor"),
        SERVER_REVISION_BEFORE("serverrevisionbefore"),
        SERVER_REVISION_AFTER("serverrevisionafter"),
        HAS_GROUP_CHANGES("hasgroupchanges"), // only in userprofilechanges
        CONTACT_LIST("contactlist"),
        USER_PROFILE_CHANGES("userprofilechanges"), // only in
        // userprofilechanges
        USER_PROFILE("userprofile"), // only setme
        CONTACT("contact"),
        NUMBER_OF_PAGES("numpages");

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
        public String tag() {
            return tag;
        }

    }

    /**
     * Find Tags item for specified String
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

    /** Array of Contact data-types generated from data returned from Server. */
    public List<Contact> mContacts = new ArrayList<Contact>();

    /** Number of pages returned. */
    public Integer mNumberOfPages = null;

    /** Current Server version. */
    public Integer mCurrentServerVersion = null;

    /** Version anchor. */
    public Integer mVersionAnchor = null;

    /** Initial Server revision. */
    public Integer mServerRevisionBefore = null;

    /** Final Server revision. */
    public Integer mServerRevisionAfter = null;

    private Boolean mHasGroupChanges = null; // only in userprofile

    public UserProfile mUserProfile = null;

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return CONTACT_CHANGES_DATA_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Contact Changes:");
        if (mCurrentServerVersion != null) {
            sb.append("\n Version on server: "); sb.append(mCurrentServerVersion.toString());
        }
        if (mServerRevisionBefore != null) {
            sb.append("\n Version on server before: "); sb.append(mServerRevisionBefore.toString());
        }
        if (mServerRevisionAfter != null) {
            sb.append("\n Version on server after: "); sb.append(mServerRevisionAfter.toString());
        }
        if (mVersionAnchor != null) {
            sb.append("\n Version anchor: "); sb.append(mVersionAnchor);
        }
        if (mNumberOfPages != null) {
            sb.append("\n Number of pages: "); sb.append(mNumberOfPages);
        }
        if (mHasGroupChanges != null) {
            sb.append("\n Has group changes: "); sb.append(mHasGroupChanges);
        }
        if (mContacts != null) {
            sb.append("\n Contacts ("); sb.append(mContacts.size()); sb.append("):\n");
            if (mContacts.size() > 0) {
                sb.append(mContacts.get(0).toString());
            }
        } else {
            sb.append("\n Contacts (0).");
        }
        if (mUserProfile != null) {
            sb.append("\n User Profile:\n"); sb.append(mUserProfile);
        }

        return sb.toString();
    }

    /**
     * Create ContactChanges from Hashtable generated by Hessian-decoder
     * 
     * @param hash Hashtable generated by Hessian-decoder
     * @return ContactChanges created from supplied Hashtable
     */
    public ContactChanges createFromHashtable(Hashtable<String, Object> hash) {
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Tags tag = findTag(key);
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
    private void setValue(Tags tag, Object value) {
        switch (tag) {
            case CURRENT_SERVER_VERSION:
                if (mCurrentServerVersion == null) {
                    mCurrentServerVersion = ((Long)value).intValue();
                }
                break;
            case SERVER_REVISION_ANCHOR:
                if (mVersionAnchor == null) {
                    mVersionAnchor = ((Long)value).intValue();
                }
                break;
            case NUMBER_OF_PAGES:
                if (mNumberOfPages == null) {
                    mNumberOfPages = (Integer)value;
                }
                break;
            case SERVER_REVISION_BEFORE:
                if (mServerRevisionBefore == null) {
                    mServerRevisionBefore = ((Long)value).intValue();
                }
                break;
            case SERVER_REVISION_AFTER:
                if (mServerRevisionAfter == null) {
                    mServerRevisionAfter = ((Long)value).intValue();
                }
                break;
            case USER_PROFILE:
            case USER_PROFILE_CHANGES:
                if (mUserProfile == null) {
                    @SuppressWarnings("unchecked")
                    Hashtable<String, Object> htValue = (Hashtable<String, Object>)value;
                    mUserProfile = UserProfile.createFromHashtable(htValue);
                }
                break;
            case HAS_GROUP_CHANGES:
                if (mHasGroupChanges == null) {
                    mHasGroupChanges = (Boolean)value;
                }
                break;
            case CONTACT_LIST:
                // add contact details from Vector;
                @SuppressWarnings("unchecked")
                Vector<Hashtable<String, Object>> contactDetails = (Vector<Hashtable<String, Object>>)value;
                if (contactDetails != null) {
                    for (Hashtable<String, Object> contDet : contactDetails) {
                        Contact c = Contact.createFromHashtable(contDet);
                        if (c != null) {
                            mContacts.add(c);
                        }
                    }
                }
                break;
            default:
                LogUtils.logE("setValue: Unknown key - " + tag + ", value: \n" + value);
                break;
        }
    }
}
