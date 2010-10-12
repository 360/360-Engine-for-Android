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

import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.SimCard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * The SimStateReceiver is a BroadcastReceiver used to listen for SIM state changes.
 * 
 * @see INTENT_SIM_STATE_CHANGED
 */
public class SimStateReceiver extends BroadcastReceiver {

    /**
     * The Listener interface.
     */
    public interface Listener {
        
        /**
         * Callback method when SIM is ready.
         */
        void onSimReadyState();
    }
    
    /**
     * The Intent broadcasted by the Android platform when the SIM card state changes.
     */
    public final static String INTENT_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    
    /**
     * The registered Listener.
     */
    private final Listener mListener;
    
    /**
     * The SimStateReceiver constructor.
     * 
     * @param listener the registered listener
     */
    public SimStateReceiver(Listener listener) {
        
        LogUtils.logD("SimStateReceiver instance created.");
        mListener = listener;
    }
    
    /**
     * @see BroadcastReceiver#onReceive(Context, Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        
        final int simState = SimCard.getState(context);
        
        LogUtils.logD("SimStateReceiver.onReceive() - simState="+simState);
        
        if (simState == TelephonyManager.SIM_STATE_READY) {
            
            mListener.onSimReadyState();
        }
    }
}
