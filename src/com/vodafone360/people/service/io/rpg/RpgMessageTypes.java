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
 * RPG message types (as defined in Now+ API - Communication Protocol & Events)
 */
public class RpgMessageTypes {
    public static final int RPG_POLL_MESSAGE = 0;

    public static final int RPG_EXT_REQ = 1;

    public static final int RPG_EXT_RESP = 2;

    public static final int RPG_PUSH_MSG = 3;

    public static final int RPG_INT_REQ = 4;

    protected static final int RPG_FETCH_CONTACTS = 5;

    public static final int RPG_INT_RESP = 6;

    public static final int RPG_SET_AVAILABILITY = 7;

    public static final int RPG_SEND_IM = 8;

    public static final int RPG_CREATE_CONV = 9;

    protected static final int RPG_CLOSE_CONV = 10;

    public static final int RPG_GET_PRESENCE = 11;

    public static final int RPG_PRESENCE_RESPONSE = 12;

    public static final int RPG_TCP_HEARTBEAT = 100;

    public static final int RPG_TCP_CONNECTION_TEST = 101;
}
