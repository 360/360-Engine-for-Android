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
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.engine.contactsync.NativeContactsApi2;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.IContactSyncObserver;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.Mode;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.State;
import com.vodafone360.people.service.PersistSettings.InternetAvail;

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
    
    /**
     * Delay when checking our Sync Setting when there is a authority auto-sync setting change.
     * This waiting time is necessary because in case it is our sync adapter authority setting 
     * that changes we cannot query in the callback because the value is not yet changed!
     */
    private static final int SYNC_SETTING_CHECK_DELAY = 500;
    
    /**
     * Same as ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS
     * The reason we have this is just because the constant is not publicly defined before 2.2.
     */
    private static final int SYNC_OBSERVER_TYPE_SETTINGS = 1;
    
    /**
     * Application object instance
     */
    private final MainApplication mApplication;
    
    /**
     * Broadcast receiver used to listen for changes in the Master Auto Sync setting
     * intent: com.android.sync.SYNC_CONN_STATUS_CHANGED
     */
    private final BroadcastReceiver mAutoSyncChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setConnectionSettingForAutoSyncSetting();
        }
    };
    
    /**
     * Observer for the global sync status setting. 
     * There is no known way to only observe our sync adapter's setting.
     */
    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            mHandler.postDelayed(mRunnable, SYNC_SETTING_CHECK_DELAY);
        }
    };
    
    /**
     * Handler used to post to a runnable in order to wait 
     * for a short time before checking the sync adapter 
     * authority sync setting after a global change occurs.
     */
    private final Handler mHandler = new Handler();
    
    /**
     * Cached Account we use to query this Sync Adapter instance's Auto-sync setting.
     */
    private Account mAccount;
    
    /**
     * Runnable used to post to a runnable in order to wait 
     * for a short time before checking the sync adapter 
     * authority sync setting after a global change occurs.
     * The reason we use this kind of mechanism is because:
     * a) There is an intent(com.android.sync.SYNC_CONN_STATUS_CHANGED) 
     * we can listen to for the Master Auto-sync but,
     * b) The authority auto-sync observer pattern using ContentResolver 
     * listens to EVERY sync adapter setting on the device AND 
     * when the callback is received the value is not yet changed so querying for it is useless.
     */
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            setConnectionSettingForAutoSyncSetting();
        }   
    };
            
    public SyncAdapter(Context context, MainApplication application) {
        // No automatic initialization (false)
        super(context, false);
        mApplication = application;
        context.registerReceiver(mAutoSyncChangeBroadcastReceiver, new IntentFilter(
            "com.android.sync.SYNC_CONN_STATUS_CHANGED"));
        ContentResolver.addStatusChangeListener(
                SYNC_OBSERVER_TYPE_SETTINGS, mSyncStatusObserver);
        // Necessary in case of Application udpate
        forceSyncSettingsInCaseOfAppUpdate();

        // Register for sync event callbacks
        // TODO: RE-ENABLE SYNC VIA SYSTEM
        // mSyncEngine.addEventCallback(this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
        if(extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false)) {
            initializeSyncAdapter(account, authority);
            return;
        } 

        setConnectionSettingForAutoSyncSetting();

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
    
    /**
     * Initializes Sync settings for this Sync Adapter
     * @param account The account associated with the initialization
     * @param authority The authority of the content
     */
    private void initializeSyncAdapter(Account account, String authority) {
        mAccount = account; // caching
        ContentResolver.setIsSyncable(account, authority, 1);
        ContentResolver.setSyncAutomatically(account, authority, true);
    }
      
    /**
     * Checks if this Sync Adapter is allowed to Sync Automatically
     * Basically just checking if the Master and its own Auto-sync are on.
     * The Master Auto-sync takes precedence over the authority Auto-sync.
     * @return true if the settings are enabled, false otherwise
     */
    private boolean canSyncAutomatically() {
        if(!ContentResolver.getMasterSyncAutomatically()) {
            return false;
        }
        return mAccount != null && 
            ContentResolver.getSyncAutomatically(mAccount, ContactsContract.AUTHORITY);
    }
    
    /**
     * Sets the application data connection setting depending on the Auto-Sync Setting.
     * If Auto-sync is enabled then connection is to online ("always connect")
     * Otherwise connection is set to offline ("manual connect")
     */
    private synchronized void setConnectionSettingForAutoSyncSetting() {
        if(canSyncAutomatically()) {
            // Enable data connection
            mApplication.setInternetAvail(InternetAvail.ALWAYS_CONNECT);
        } else {
            // Disable data connection
            mApplication.setInternetAvail(InternetAvail.MANUAL_CONNECT);
        }
    }
    
    /**
     * This method is essentially needed to force the sync settings 
     * to a consistent state in case of an Application Update.
     * This is because old versions of the client do not set 
     * the sync adapter to syncable for the contacts authority.
     */
    private void forceSyncSettingsInCaseOfAppUpdate() {
        NativeContactsApi2 nabApi = (NativeContactsApi2) NativeContactsApi.getInstance();
        nabApi.setSyncable(true);
        mAccount = nabApi.getPeopleAccount();
        nabApi.setSyncAutomatically(
                mApplication.getInternetAvail() == InternetAvail.ALWAYS_CONNECT);
    }
}

