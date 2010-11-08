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

package com.vodafone360.people.service.utils;

import android.content.Context;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.receivers.SimStateReceiver;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.LoginPreferences;
import com.vodafone360.people.utils.SimCard;

/**
 * The UserDataProtection is responsible of putting in place mechanisms that will guarantee
 * the User data safety.
 * 
 * The case to watch are the following:
 * -User uses SIM card A to log in 360 then restarting the device with SIM card B or without a SIM
 *  card will automatically log out from the 360 client.
 * -User uses no SIM card to log in 360 then restarting the device with a SIM card will automatically
 *  log out from the 360 client.
 */
public class UserDataProtection implements SimStateReceiver.Listener {
    
    /**
     * The Service context.
     */
    private Context mContext;
    
    /**
     * The DatabaseHelper used for our database access.
     */
    private DatabaseHelper mDatabaseHelper;
    
    /**
     * The BroadcastReceiver listening for SIM states changes.
     */
    private SimStateReceiver mSimStateReceiver;
    
    /**
     * The UserDataProtection constructor.
     * 
     * @param context the Service context
     * @param databaseHelper the DatabaseHelper
     */
    public UserDataProtection(Context context, DatabaseHelper databaseHelper) {
        
        mContext = context;
        mDatabaseHelper = databaseHelper;
    }
    
    /**
     * Performs the checks needed when the 360 Service is started.
     * 
     * This method checks if the user has changed and if the SIM card id cannot be read, sets a
     * SIM state changes listener.
     */
    public void performStartupChecks() {
        
        LogUtils.logD("UserDataProtection.performStartupChecks()");
            
        final int simState = SimCard.getState(mContext);
            
        if (simState == TelephonyManager.SIM_STATE_ABSENT || simState == TelephonyManager.SIM_STATE_READY) {
                
            processUserChanges();
        } else {
                
            LogUtils.logD("UserDataProtection.performStartupChecks() - SIM_STATE_UNKNOWN, register a SimStateReceiver.");
            // SIM is not ready, register a listener for Sim state changes to check
            // the subscriber id when possible
            mSimStateReceiver = new SimStateReceiver(this);
            mContext.registerReceiver(mSimStateReceiver, new IntentFilter(SimStateReceiver.INTENT_SIM_STATE_CHANGED));
        }
    }
    
    /**
     * Requests to log out from 360 if the user has changed.
     */
    public void processUserChanges() {

        LogUtils.logD("UserDataProtection.checkUserChanges()");
        
        final LoginEngine loginEngine = EngineManager.getInstance().getLoginEngine();
        
        if (hasUserChanged()) {

             if (loginEngine.isLoggedIn()) {
                 // User has changed, log out
                LogUtils.logD("UserDataProtection.checkUserChanges() - User has changed! Request logout.");
                loginEngine.addUiRemoveUserDataRequest();
            } else {
                LoginPreferences.clearPreferencesFile(mContext);
            }
            
        }
    }
    
    /**
     * Unregister the SIM state changes receiver.
     */
    public void unregisterSimStateReceiver() {
        
        if (mSimStateReceiver != null) {
            
            LogUtils.logD("UserDataProtection.checkUserChanges() - unregister the SimStateReceiver");
            mContext.unregisterReceiver(mSimStateReceiver);
        }
    }
    
    /**
     * Check wether or not the User has changed.
     * 
     * @return true if the current User is different, false otherwise
     */
    public boolean hasUserChanged() {
        
        final String loginSubscriberId = getSubscriberIdForLogin();
        final String currentSuscriberId = SimCard.getSubscriberId(mContext);
        return !TextUtils.equals(loginSubscriberId, currentSuscriberId);
    }
    
    /**
     * Gets the Subscriber Id used to log in 360.
     * 
     * @param databaseHelper the DatabaseHelper
     * @return the Subscriber Id used to log in 360, null if there was a problem while retrieving it
     */
    public String getSubscriberIdForLogin() {
        
        final LoginDetails mLoginDetails = new LoginDetails();
        final ServiceStatus mServiceStatus = mDatabaseHelper.fetchLogonCredentials(mLoginDetails);

        if (mServiceStatus == ServiceStatus.SUCCESS) {
            return mLoginDetails.mSubscriberId;
        }
        
        return null;
    }

    /**
     * @see SimStateReceiver.Listener#onSimReadyState()
     */
    @Override
    public void onSimReadyState() {
        
        processUserChanges();
        unregisterSimStateReceiver();
    }
}
