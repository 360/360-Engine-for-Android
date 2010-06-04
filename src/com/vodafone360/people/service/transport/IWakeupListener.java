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
package com.vodafone360.people.service.transport;

/**
 * An interface used for listening for wakeup alarms which are sent by the
 * Service. This is necessary to wake up the CPU from standby to resend a
 * heartbeat and keep the network connection alive.
 */
public interface IWakeupListener {

    /**
     * The thread name to send the alarm for.
     */
    public static final String ALARM_HB_THREAD = "com.vodafone360.people.service.transport.tcp.TRANSPORT_THREAD";

    /**
     * The name of the system service that is responsible for alarms.
     */
    //public static final String ALARM_SERVICE = "alarm";

    /**
     * Called whenever the Service receives a Wakeup Alarm intent. The listener
     * should react on this call by carrying out network activity, e.g. to keep
     * the connection alive.
     */
    public void notifyOfWakeupAlarm();
}
