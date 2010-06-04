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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.vodafone360.people.engine.contactsync.ContactSyncEngine.IContactSyncObserver;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.Mode;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.State;

/**
 * SyncAdapter implementation which basically just ties in with
 * the old Contacts Sync Engine code for the moment and waits for the sync to be finished.
 * In the future we may want to improve this, particularly if the sync
 * is actually be done in this thread which would also enable disable sync altogether.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter implements IContactSyncObserver {
 // TODO: RE-ENABLE SYNC VIA SYSTEM
//    /**
//     * Direct access to Sync Engine stored for convenience
//     */
//    private final ContactSyncEngine mSyncEngine = EngineManager.getInstance().getContactSyncEngine();
//    
//    /**
//     * Boolean used to remember if we have requested a sync.
//     * Useful to ignore events 
//     */
//    private boolean mPerformSyncRequested = false;
//    
//    /**
//     * Time to suspend the thread between pools to the Sync Engine.
//     */
//
//    private final int POOLING_WAIT_INTERVAL = 1000;
    
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        // Register for sync event callbacks
     // TODO: RE-ENABLE SYNC VIA SYSTEM
//        mSyncEngine.addEventCallback(this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
     // TODO: RE-ENABLE SYNC VIA SYSTEM
//        try {
//          synchronized(this) {
//              mPerformSyncRequested = true;
//              if(!mSyncEngine.isSyncing()) {
//                  mSyncEngine.startFullSync();
//              }
//              
//              while(mSyncEngine.isSyncing()) {
//                  wait(POOLING_WAIT_INTERVAL);
//              }
//              mPerformSyncRequested = false;
//          }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * @see IContactSyncObserver#onContactSyncStateChange(Mode, State, State)
     */
    @Override
    public void onContactSyncStateChange(Mode mode, State oldState, State newState) {
     // TODO: RE-ENABLE SYNC VIA SYSTEM
//        synchronized(this) {
//            /*
//             * This check is done so that we can also update the native UI 
//             * when the client devices to sync on it own
//             */
//            if(!mPerformSyncRequested && 
//                    mode != Mode.NONE) {
//                mPerformSyncRequested = true;
//                Account account = new Account(
//                        LoginPreferences.getUsername(), 
//                        NativeContactsApi2.PEOPLE_ACCOUNT_TYPE);
//                ContentResolver.requestSync(account, ContactsContract.AUTHORITY, new Bundle());
//            }
//        }
    }

    /**
     * @see IContactSyncObserver#onProgressEvent(State, int)
     */
    @Override
    public void onProgressEvent(State currentState, int percent) {
        // Nothing to do
    }

    /**
     * @see IContactSyncObserver#onSyncComplete(ServiceStatus)
     */
    @Override
    public void onSyncComplete(ServiceStatus status) {
        // Nothing to do
    }
}

