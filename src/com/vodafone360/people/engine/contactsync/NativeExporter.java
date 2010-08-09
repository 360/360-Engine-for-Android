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

package com.vodafone360.people.engine.contactsync;

import com.vodafone360.people.engine.contactsync.NativeContactsApi.Account;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

public class NativeExporter {

    /**
     * The undefined result when the NativeImporter has not been run yet.
     * @see #getResult()
     */
    public final static int RESULT_UNDEFINED = -1;
    
    /**
     * The ok result when the NativeImporter has finished successfully.
     * @see #getResult()
     */
    public final static int RESULT_OK = 0;
    
    /**
     * The undefined result when the NativeImporter has not been run yet.
     * @see #getResult()
     */
    public final static int RESULT_ERROR = 1;
    
    /**
     * Number of contacts to be processed "per tick".
     * @see #tick()
     */
    private final static int MAX_CONTACTS_OPERATION_COUNT = 2;
    
    /**
     * Handler to the People Contacts API.
     */
    private PeopleContactsApi mPeopleContactsApi;
    
    /**
     * Handle to the Native Contacts API. 
     */
    private NativeContactsApi mNativeContactsApi;
    
    /**
     * Internal state representing the task to perform: gets the list of local contacts IDs to be synced to Native.
     */
    private final static int STATE_GET_CONTACT_IDS = 0;
    
    /**
     * Internal state representing the task to perform: iterates through the list of syncable contacts IDs and sync to Native side.
     */
    private final static int STATE_ITERATE_THROUGH_IDS = 1;
    
    /**
     * Internal state representing the task to perform: final state, nothing else to perform.
     */
    private final static int STATE_DONE = 2;
    
    /**
     * The current state.
     */
    private int mState = STATE_GET_CONTACT_IDS;
    
    /**
     * The list of local IDs from people contacts that need to be synced to Native.
     */
    private long[] mSyncableContactsIds;
    
    /**
     * The index in the current people id.
     * @see #mSyncableContactsIds
     */
    private int mCurrentSyncableIdIndex = 0;
    
    /**
     * The result status.
     */
    private int mResult = RESULT_UNDEFINED;
    
    /**
     * The total count of IDs to process (Native Database + People Database).
     */
    private int mTotalIds = 0;
    
    /**
     * The current count of processed IDs.
     */
    private int mProcessedIds = 0;
    
    /**
     * The Native Account where to write the Contacts. 
     */
    private Account mAccount = null;
    
    /**
     * Constructor.
     * 
     * @param pca handler to the People Contacts API
     * @param nca handler to the Native Contacts API
     */
    public NativeExporter(PeopleContactsApi pca, NativeContactsApi nca) {
        
        mPeopleContactsApi = pca;
        mNativeContactsApi = nca;
        
        if (VersionUtils.is2XPlatform()) {
            
            final Account[] accounts = mNativeContactsApi.getAccountsByType(NativeContactsApi.PEOPLE_ACCOUNT_TYPE);
            
            if (accounts != null) {
                
                mAccount = accounts[0];
            }
        }
    }
    
    /**
     * Gets the current position in the list of IDs.
     * 
     * This can be used to track the current progress.
     * @see #getCount()
     * 
     * @return the last processed id position in the list of IDs
     */
    public int getPosition() {
        
        return mProcessedIds;
    }
    
    /**
     * Gets the total number of IDs to process.
     * 
     * @return the number of IDs to process
     */
    public int getCount() {
        
        return mTotalIds;
    }
    
    /**
     * Sets the internal state to DONE with the provided result status.
     * 
     * @param result the result status to set
     */
    private void complete(int result) {
        
        mState = STATE_DONE;
        mResult = result;
    }

    
    /**
     * Tick method to call each time there is time for processing the Native Contacts Import.
     * 
     * Note: the method will block for some time to process a certain number of Contacts and then will
     *       return. It will have to be called until it returns true meaning that the Import is over.
     * 
     * @return true when the Import Task is finished, false if not
     */
    public boolean tick() {

        switch(mState) {
            case STATE_GET_CONTACT_IDS:
                getContactIds();
                break;
            case STATE_ITERATE_THROUGH_IDS:
                iterateThroughSyncableIds();
                break;
        }
        
        return mState == STATE_DONE;
    }
        
    /**
     * Gets the Import result.
     * 
     * @see #RESULT_OK
     * @see #RESULT_ERROR
     * @see #RESULT_UNDEFINED
     * 
     * @return the Import result
     */
    public int getResult() {
        
        return mResult;
    }
    
    /**
     * Gets the list of local contacts IDs that need to be synced to Native.
     */
    private void getContactIds() {
        
        LogUtils.logD("NativeExporter.getIdList()");
        
        mSyncableContactsIds = mPeopleContactsApi.getNativeSyncableContactIds();
        
        // check if we have some work to do
        if (mSyncableContactsIds != null) {
            mState = STATE_ITERATE_THROUGH_IDS;    
        } else {
            complete(RESULT_OK);
        }
    }
    
    /**
     * Iterates through the list of syncable contacts IDs and sync to Native side.
     */
    private void iterateThroughSyncableIds() {
        
        LogUtils.logD("NativeExporter.iterateThroughSyncableIds()");
        
        final int limit = Math.min(mSyncableContactsIds.length, mCurrentSyncableIdIndex + MAX_CONTACTS_OPERATION_COUNT);
        
        while (mCurrentSyncableIdIndex < limit) {
            
            final ContactChange[] changes = mPeopleContactsApi.getNativeSyncableContactChanges(mSyncableContactsIds[mCurrentSyncableIdIndex]);
            
            if (changes != null) {
                
                exportContactChanges(changes);
            }

            mCurrentSyncableIdIndex++;
        }
        
        if (mCurrentSyncableIdIndex == mSyncableContactsIds.length) {
            // Nothing else to do
            complete(RESULT_OK);
        }
    }
    
    /**
     * Exports the contact changes to the native address book.
     * 
     * @param changes the array of ContactChange that represent a full contact, a deleted contact or an updated contact
     */
    private void exportContactChanges(ContactChange[] changes) {
        
        // the ContactChange array that we'll get back from native
        ContactChange[] nativeResponse = null;
        
        switch(changes[0].getType()) {
            
            case ContactChange.TYPE_ADD_CONTACT:
                // add the contact on Native side
                
                //the account can be null (theoretically)
                if (mAccount != null) {
                    nativeResponse = mNativeContactsApi.addContact(mAccount, changes);
                    // sync back the native IDs on People side
                    if (!mPeopleContactsApi.syncBackNewNativeContact(changes, nativeResponse)) {
                        LogUtils.logE("NativeExporter.exportContactChanges() - Add Contact failed!");
                    }    
                }
                break;
            case ContactChange.TYPE_DELETE_CONTACT:
                // delete the contact on Native side
                mNativeContactsApi.removeContact(changes[0].getNabContactId());
                // acknowledge the people side about deletion
                if (!mPeopleContactsApi.syncBackDeletedNativeContact(changes[0])) {
                    LogUtils.logE("NativeExporter.exportContactChanges() - Syncing back Contact deletion to Client side failed!");
                }
                break;
            case ContactChange.TYPE_UPDATE_CONTACT:
            case ContactChange.TYPE_ADD_DETAIL:
            case ContactChange.TYPE_DELETE_DETAIL:
            case ContactChange.TYPE_UPDATE_DETAIL:
                // update the contact on Native side
                nativeResponse = mNativeContactsApi.updateContact(changes);
                // acknowledge People side about deleted details and added details Native IDs
                if (!mPeopleContactsApi.syncBackUpdatedNativeContact(changes, nativeResponse)) {
                    LogUtils.logE("NativeExporter.exportContactChanges() - Update Contact failed!");
                }
                break;
            default:
                LogUtils.logE("NativeExporter.exportContactChanges() - Aborted exporting because of unknown type("+changes[0].getType()+")!");
                break;
        }
    }
}
