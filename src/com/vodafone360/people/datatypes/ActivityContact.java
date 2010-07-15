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

import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;

import com.vodafone360.people.datatypes.ContactDetail.DetailKeyTypes;

/**
 * BaseDataType encapsulating an Activity Contact which contains contact
 * information associated with an Activity response retrieved from the server.
 */
public class ActivityContact extends BaseDataType {

    /**
     * Tags for fields associated with Activity Contacts.
     */
    private enum Tags {
        CONTACT_ID("contactid"),
        USER_ID("userid"),
        NAME("name"),
        ADDRESS("address"),
        TYPE("type"),
        NETWORK("network"),
        AVATAR("avatar"),
        AVATAR_MIME("avatarmime"),
        AVATAR_URL("avatarurl");

        private final String tag;

        /**
         * Constructor for Tags item.
         * 
         * @param s String value associated with Tags item.
         */
        private Tags(final String s) {
            tag = s;
        }

        /**
         * String value associated with Tags item
         * 
         * @return String associated with Tags item
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

    /** The contact server id if the activity can be related to a contact. */
    public Long mContactId = null;

    /** Local id for a Contact if activity can be related to a Contact. */
    public Long mLocalContactId = null;

    /**
     * The user id if the activity can be related to a user, e.g. a message from
     * a VF user but non-contact contains userid but not contactid, whereas a
     * message from a contact that is also a VF user contains both contactid and
     * userid.
     */
    public Long mUserId = null;

    /**
     * Contains the name of the other user. When contactid is present, the name
     * from the addressbook. When only userid is present, the name on the user's
     * profile. When none is present, the name captured from the outside, if
     * possible, e.g. the name on the email From header. In a case like a SMS
     * received from an unknown number, there is no name.
     */
    public String mName = null;

    /**
     * Optional for non-messages. Contains the MSISDN, email, etc, needed to
     * reply back.
     */
    public String mAddress = null;

    /**
     * Optional for non-messages. Contains the type (mobile, work, etc.)
     * captured from the addressbook, if available.
     */
    private ContactDetail.DetailKeyTypes mType = null;

    /**
     * This field contains information about the network (e.g. phone, flickr,
     * google).
     */
    public String mNetwork = null;

    /**
     * Defines the binary data for the user's icon. The type of the binary data
     * is defined into the avatarmime field.
     */
    private ByteBuffer mAvatar = null;

    /** Defines the MIME type of the avatar binary data. */
    private String mAvatarMime = null;

    /**
     * Defines an http url that the client can use to retrieve the avatar binary
     * data. Can be used to embed the url into an IMG HTML tag.
     */
    public String mAvatarUrl = null;

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return ACTIVITY_CONTACT_DATA_TYPE;
    }

    /**
     * Create ActivityContact from Hashtable (generated from Hessian encoded
     * response).
     * 
     * @param hash Hashtable containing ActivityContact data
     * @return ActivityContact created from supplied Hashtable.
     */
    public static ActivityContact createFromHashTable(Hashtable<String, Object> hash) {
        final ActivityContact zcon = new ActivityContact();
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = Tags.findTag(key);
            zcon.setValue(tag, value);
        }
        return zcon;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object val) {
        if (tag != null) {
            switch (tag) {
                case ADDRESS:
                    mAddress = (String)val;
                    break;

                case AVATAR:
                    byte[] avabytes = (byte[])val;
                    mAvatar = ByteBuffer.allocate(avabytes.length);
                    mAvatar.put(avabytes);
                    break;

                case AVATAR_MIME:
                    mAvatarMime = (String)val;
                    break;

                case AVATAR_URL:
                    mAvatarUrl = (String)val;
                    break;

                case CONTACT_ID:
                    mContactId = (Long)val;
                    break;

                case NAME:
                    mName = (String)val;
                    break;

                case NETWORK:
                    mNetwork = (String)val;
                    break;

                case TYPE:
                    mType = DetailKeyTypes.findKey((String)val);
                    break;

                case USER_ID:
                    mUserId = (Long)val;
                    break;

                default:
                    // Do nothing.
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = 
            new StringBuffer("ActivityContact:\n\t\tContact Id = ");
        sb.append(mContactId);
        sb.append("\n\t\tLocal contact Id = "); sb.append(mLocalContactId);
        sb.append("\n\t\tUser Id = "); sb.append(mUserId);
        sb.append("\n\t\tName = "); sb.append(mName);
        sb.append("\n\t\tAddress = "); sb.append(mAddress);
        sb.append("\n\t\tType = "); sb.append(mType);
        sb.append("\n\t\tNetwork = "); sb.append(mNetwork);
        if (mAvatar != null) {
            sb.append("\n\t\tAvatar = "); sb.append(String.valueOf(mAvatar));
        }
        sb.append("\n\t\tAvatar mime = "); sb.append(mAvatarMime);
        sb.append("\n\t\tAvatar URL = "); sb.append(mAvatarUrl);
        return sb.toString();
    }
}
