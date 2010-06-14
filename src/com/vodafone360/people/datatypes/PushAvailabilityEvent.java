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

import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.presence.User;
import com.vodafone360.people.service.io.rpg.RpgPushMessage;

/**
 * BaseDataType encapsulating PushAvailabilityEvent. This is a Push message
 * received from People server containing Availability (Presence) change
 * information.
 */
public class PushAvailabilityEvent extends PushEvent {

    /**
     * TODO: This should be Hashtable <String userid, Hashtable<String
     * community, String status>>
     */
    public List<User> mChanges;

    public PushAvailabilityEvent() {
        super();
    }

    /**
     * Create PushAvailablityEvent from RpgPushMessage.
     * 
     * @param msg RpgPushMessage
     * @param engId EngineId associated with message
     * @return PushAvailabilityEvent created from supplied data
     */
    public static BaseDataType createPushEvent(RpgPushMessage msg, EngineId engId) {
        PushAvailabilityEvent push = new PushAvailabilityEvent();
        push.mMessageType = msg.mType;
        push.mEngineId = engId;
        push.mChanges = convertToList(msg.mHash);
        return push;
    }

    @SuppressWarnings("unchecked")
    private static List<User> convertToList(Hashtable<String, Object> table) {
        ArrayList<User> users = new ArrayList<User>();
        for (Enumeration<String> en = table.keys(); en.hasMoreElements();) {
            String userId = en.nextElement();
            users.add(new User(userId, (Hashtable<String, String>)table.get(userId)));
        }
        return users;
    }
}
