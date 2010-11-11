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

package com.vodafone360.people;


/**
 * Intents for Broadcast Receiver
 */
public class Intents {
    
    public static final String UPDATE_CHAT_NOTIFICATION = "com.vodafone360.people.Intents.UPDATE_CHAT_NOTIFICATION";
    
    public static final String ROAMING_ON = "com.vodafone360.people.Intents.ROAMING_ON";
    
    public static final String ROAMING_OFF = "com.vodafone360.people.Intents.ROAMING_OFF";

    public static final String START_LOGIN_ACTIVITY =  "com.vodafone360.people.Intents.START_LOGIN_ACTIVITY";

    public static final String UPDATE_WIDGET =  "com.vodafone360.people.Intents.UPDATE_WIDGET";
    
    public static final String CLEARALL_NOTIFICATION =  "com.vodafone360.people.Intents.CLEAR_ALL";
    
    public static final String OPEN_STATUS = "com.vodafone360.people.Intents.OPEN_STATUS";
    
    public static final String DELETE_CONTACT_NOTIFICATION = "com.vodafone360.people.Intents.Delete_Contact_NOTIFICATION";
    
    /**
     * This is a boolean extra for the Intent, which indicates the Intent has been dismissed.
     */
    public static final String EXTRA_DISMISSED = "dismissed"; 
}
