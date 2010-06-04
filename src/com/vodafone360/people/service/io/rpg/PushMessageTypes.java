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

package com.vodafone360.people.service.io.rpg;

/**
 * Defines values for the RPG Push message types as defined in Now+ API -
 * Communication Protocol & Events
 */
public enum PushMessageTypes {
    // Internal RPG Events
    /**
     * <li>String 'conversation' / String conversation ID <li>String 'from' /
     * String with senders user ID or external IM identifier string (e.g.
     * google::steve@gmail.com) <li>String 'tos' / List containing Strings with
     * recipients user IDs or external IM identifier string (e.g.
     * google::hieu@gmail.com) <li>String 'body' / String with text message <br>
     * <b>Note: all formatting tags, e.g. bold, color, etc... will be stripped
     * from the message body</b>
     */
    CHAT_MESSAGE("cm"),
    START_CONVERSATION("c1"),
    CLOSED_CONVERSATION("c0"),
    /**
     * String containing the user ID or external IM identifier string as the
     * hashmap key / Map a HashMap of key/value pairs String [community] /
     * String [availability], indicating community presence information, where:
     * [community] is a community or presence id (e.g. 'pc', 'mobile', 'google',
     * 'msn'). NOTE: 'pc' or 'mobile' presence indicates a device specific NOW+
     * presence/availability state [availability] is the presence/availability
     * state, online | offline
     */
    AVAILABILITY_STATE_CHANGE("pp"),
    /** String conversation id is returned */
    CONVERSATION_END("c0"),
    // API Events
    /** No payload */
    PROFILE_CHANGE("pc"),
    /** No payload */
    CONTACTS_CHANGE("cc"),
    /** No payload */
    TIMELINE_ACTIVITY_CHANGE("atl"),
    /** No payload */
    STATUS_ACTIVITY_CHANGE("ast"),
    /** Payload = String text of the invitation message */
    FRIENDSHIP_REQUEST_RECEIVED("frr"),
    /** No payload */
    IDENTITY_CHANGE("ic"),
    // scope not clear
    SYSTEM_NOTIFICATION("sn"),
    IDENTITY_NETWORK_CHANGE("inc");

    private final String tag;

    /**
     * Construct PushMessageType from supplied String
     * 
     * @param s String contain push message type identifier.
     */
    private PushMessageTypes(String s) {
        tag = s;
    }

    /**
     * Return string value for the specified tag.
     * 
     * @return String value for tag.
     */
    protected String tag() {
        return tag;
    }
}
