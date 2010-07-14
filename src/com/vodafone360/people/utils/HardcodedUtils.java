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
package com.vodafone360.people.utils;

import java.util.Hashtable;

import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.presence.NetworkPresence;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;

/**
 * @author timschwerdtner
 * Collection of hardcoded and duplicated code as a first step for refactoring.
 */
public class HardcodedUtils {
    
    /**
     * To be used with IPeopleService.setAvailability until identity handling
     * has been refactored.
     * @param Desired availability state
     * @return A hashtable for the set availability call
     */
    public static Hashtable<String, String> createMyAvailabilityHashtable(OnlineStatus onlineStatus) {
        Hashtable<String, String> availability = new Hashtable<String, String>();

        LogUtils.logD("Setting Availability to: " + onlineStatus.toString());
        // TODO: REMOVE HARDCODE setting everything possible to currentStatus
        availability.put(NetworkPresence.SocialNetwork.GOOGLE.toString(), onlineStatus.toString());
        availability.put(NetworkPresence.SocialNetwork.MICROSOFT.toString(), onlineStatus.toString());
        availability.put(NetworkPresence.SocialNetwork.MOBILE.toString(), onlineStatus.toString());
        availability.put(NetworkPresence.SocialNetwork.HYVES_NL.toString(), onlineStatus.toString());
        availability.put(NetworkPresence.SocialNetwork.FACEBOOK_COM.toString(), onlineStatus.toString());

        return availability;
    }
    
    /**
     * The static list of supported TPC accounts.
     */    
    public static final int[] THIRD_PARTY_CHAT_ACCOUNTS = new int[]{SocialNetwork.FACEBOOK_COM.ordinal(), 
                             SocialNetwork.GOOGLE.ordinal(), SocialNetwork.HYVES_NL.ordinal(), 
                             SocialNetwork.MICROSOFT.ordinal()};
}
