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

import java.io.IOException;
import java.util.Hashtable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.gsm.SmsMessage;

import com.vodafone360.people.service.utils.hessian.HessianDecoder;
import com.vodafone360.people.utils.LogUtils;

/**
 * Handle SMS activation messages sent from server to activate an account after
 * sign-up/sign-in. The SMS should contain a Hessian encoded body which has an
 * activation code used to activate the account.
 */
@SuppressWarnings("deprecation")
// Sms message is deprecated, don't show warning
public class SmsBroadcastReceiver extends BroadcastReceiver {

    /**
     * Identifier for SMS activation/wake-up messages sent to People client
     */
    public static final String ACTION_ACTIVATION_CODE = "com.vodafone360.people.service.ACTIVATION";

    /**
     * Implementation of the {@link BroadcastReceiver#onReceive} function.
     * Decodes Hessian from SMS text and broadcasts the data to the engines
     * 
     * @param context The current context of the People application
     * @param intent Intent containing the SMS data
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.logD("SmsBroadcastReceiver.onReceive - Begin of sms receiver. \n"
                + intent.toString());
        Bundle bundle = intent.getExtras();

        Object messages[] = (Object[])bundle.get("pdus");
        SmsMessage smsMessage[] = new SmsMessage[messages.length];
        for (int n = 0; n < messages.length; n++) {
            try {
                smsMessage[n] = SmsMessage.createFromPdu((byte[])messages[n]);
                LogUtils.logD("SmsBroadcastReceiver.onReceive - SMS no [" + n + "]bytes: "
                        + new String(smsMessage[n].getUserData()));
                HessianDecoder hd = new HessianDecoder();
                Hashtable<String, Object> ht = hd.decodeHessianByteArrayToHashtable(smsMessage[n]
                        .getUserData());

                if (ht != null) {
                    LogUtils.logD("SmsBroadcastReceiver.onReceive - Decoded hashtable: "
                            + ht.toString());
                    Hashtable<?, ?> codeTable = (Hashtable<?, ?>)ht.get("e");
                    String code = (String)codeTable.get("r");
                    LogUtils.logD("SmsBroadcastReceiver.onReceive - Activation code: " + code);
                    Intent in = new Intent(ACTION_ACTIVATION_CODE);
                    in.putExtra("code", code);
                    context.sendBroadcast(in);
                } else {
                    String code = "No code in SMS";
                    LogUtils.logD("SmsBroadcastReceiver.onReceive - Code not in sms");
                    Intent in = new Intent(ACTION_ACTIVATION_CODE);
                    in.putExtra("code", code);
                    context.sendBroadcast(in);
                }
            } catch (IOException e) {
                LogUtils.logE("SmsBroadcastReceiver.onReceive() "
                        + "IOException while decoding SMS Message.", e);
            }
        }
    }
}
