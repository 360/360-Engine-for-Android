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

import android.content.Context;
import android.os.IBinder;

import com.vodafone360.people.MainApplication;

/**
 * Small class created solely for holding 
 * the Authenticator and Sync Adapter objects required 
 * for the ability to have a 360 People Account on Native side.
 * We hold the objects this way to make it so that the code also loads on 1.X devices.
 */
public class NativeAccountObjectsHolder {
    /**
     * "Hidden" Authenticator object
     */
    private static Object sAuthenticator;
    /**
     * "Hidden" Sync Adapter object
     */
    private static Object sSyncAdapter;
    
    public NativeAccountObjectsHolder(MainApplication application) {
        Context context = application.getApplicationContext();
        sAuthenticator = new Authenticator(context, application);
        sSyncAdapter = new SyncAdapter(context, application);
    }
    
    /**
     * Shortcut method to get the Binder Interface from the Authenticator object.
     * @return IBinder object for the Authenticator
     */
    public IBinder getAuthenticatorBinder() {
        return ((Authenticator) sAuthenticator).getIBinder();
    }
    
    /**
     * Shortcut method to get the Binder Interface from the Sync Adapter object.
     * @return IBinder object for the Sync Adapter
     */
    public IBinder getSyncAdapterBinder() {
        return ((SyncAdapter)sSyncAdapter).getSyncAdapterBinder();
    }
}
