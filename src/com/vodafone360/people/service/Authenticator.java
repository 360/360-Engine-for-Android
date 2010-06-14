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

package com.vodafone360.people.service;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.utils.LoginPreferences;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the 360 People Account space.
 */
public class Authenticator extends AbstractAccountAuthenticator {
    private final MainApplication mApplication;
    
    /**
     * Intent action to send to StartActivity activity.
     * Upon receiving this action a UI notification should be presented
     * regarding only one 360 People account being supported.
     */
    public static final String ACTION_ONE_ACCOUNT_ONLY_INTENT = 
        "com.vodafone360.people.android.account.ONE_ONLY";

    
    /**
     * Constructor
     * @param context Context passed to superclass
     * @param application MainApplication object needed to remove user data
     */
    public Authenticator(Context context, MainApplication application) {
        super(context);
        mApplication = application;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
        String accountType, String authTokenType, String[] requiredFeatures,
        Bundle options) {
        final Intent intent = new Intent();
        // In order to not use the UI package we define the class as a String
        // FIXME: Change this not to depend on the hardcoded String. Maybe use intent filter on StartActivity
        intent.setClassName(mApplication.getApplicationContext(), 
                "com.vodafone360.people.ui.StartActivity");
        if(NativeContactsApi.getInstance().isPeopleAccountCreated()) {
            intent.setAction(ACTION_ONE_ACCOUNT_ONLY_INTENT);                
        } 

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account)
            throws NetworkErrorException {
        // Remove the data from the application behind the scenes
        final String username = LoginPreferences.getUsername();
        if(!TextUtils.isEmpty(username)) {
            mApplication.removeUserData();
        }
        Bundle result = new Bundle();
        /* 
         * At this point the user was already prompted by the system for confirmation
         * Returning false here would just popup another notification 
         * saying its not possible to remove the Account without factory resetting  
         */
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
        Account account, Bundle options) {
        // Nothing to do currently
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
        String accountType) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
        Account account, String authTokenType, Bundle loginOptions) {
          // Nothing to do currently
          return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // Nothing to do currently
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
        Account account, String[] features) {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
        Account account, String authTokenType, Bundle loginOptions) {
        // Nothing to do currently
        return null;
    }

}