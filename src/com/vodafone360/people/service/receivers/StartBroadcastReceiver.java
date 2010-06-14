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

package com.vodafone360.people.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.utils.LogUtils;

/**
 * People client BroadcastReceiver. Responsible for auto-start of People's
 * service.
 */
public class StartBroadcastReceiver extends BroadcastReceiver {

    /**
     * Implementation of the {@link BroadcastReceiver#onReceive} function.
     * Starts service once the phone has booted
     * 
     * @param context The current context of the People application
     * @param intent Intent data
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.logI("StartBroadcastReceiver.onReceive() Received intent only when the system"
                + " boot is completed");
        context.startService(new Intent(context, RemoteService.class));
    }
}
