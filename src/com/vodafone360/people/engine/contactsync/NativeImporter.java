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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.text.TextUtils;

import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.engine.contactsync.NativeContactsApi.Account;
import com.vodafone360.people.utils.DynamicArrayLong;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * The NativeImporter class is responsible for importing contacts from the
 * native address book to the people database. To do so, it compares the native
 * address book and the people database to find new changes and update the
 * people database accordingly. Usage: - instantiate the class - call the tick()
 * method until isDone() returns true - current progress can be obtained via
 * getPosition() / getCount() - check the getResult() to get a status (OK,
 * KO...)
 */
public class NativeImporter {

    /**
     * The undefined result when the NativeImporter has not been run yet.
     * 
     * @see #getResult()
     */
    public final static int RESULT_UNDEFINED = -1;

    /**
     * The ok result when the NativeImporter has finished successfully.
     * 
     * @see #getResult()
     */
    public final static int RESULT_OK = 0;

    /**
     * The undefined result when the NativeImporter has not been run yet.
     * 
     * @see #getResult()
     */
    public final static int RESULT_ERROR = 1;

    /**
     * Number of contacts to be processed "per tick". This is the start value
     * and is adapted to get as close as possible to TARGET_TIME_PER_TICK
     * 
     * @see #tick()
     */
    private final static float CONTACTS_PER_TICK_START = 2.0F;
    
    /**
     * Ideal processing time per tick in ms
     */
    private final static float TARGET_TIME_PER_TICK = 200.0F;
    
    /**
     * Contacts to process in one tick. This float will be rounded.
     */
    private float mContactsPerTick = CONTACTS_PER_TICK_START;

    /**
     * Handler to the People Contacts API.
     */
    private PeopleContactsApi mPeopleContactsApi;

    /**
     * Handler to the Native Contacts API.
     */
    private NativeContactsApi mNativeContactsApi;

    /**
     * Internal state representing the task to perform: gets the list of native
     * contacts ids and people contacts ids.
     */
    private final static int STATE_GET_IDS_LISTS = 0;

    /**
     * Internal state representing the task to perform: iterates through the
     * list of ids to find differences (i.e. added contact, modified contact,
     * deleted contact).
     */
    private final static int STATE_ITERATE_THROUGH_IDS = 1;

    /**
     * Internal state representing the task to perform: process the list of
     * deleted contacts (i.e. delete the contacts from the people database).
     * TODO: remove this state
     */
    private final static int STATE_PROCESS_DELETED = 2;

    /**
     * Internal state representing the task to perform: final state, nothing
     * else to perform.
     */
    private final static int STATE_DONE = 3;

    /**
     * The current state.
     */
    private int mState = STATE_GET_IDS_LISTS;

    /**
     * The list of native ids from the native side.
     */
    private long[] mNativeContactsIds;

    /**
     * The list of native ids from the people side.
     */
    private long[] mPeopleNativeContactsIds;

    /**
     * The total count of ids to process (native database + people database).
     */
    private int mTotalIds = 0;

    /**
     * The current count of processed ids.
     */
    private int mProcessedIds = 0;

    /**
     * The index of the current native id.
     * 
     * @see #mNativeContactsIds
     */
    private int mCurrentNativeId = 0;

    /**
     * The index in the current people id.
     * 
     * @see #mPeopleNativeContactsIds
     */
    private int mCurrentPeopleId = 0;

    /**
     * The index of the current deleted people id.
     * 
     * @see #mDeletedIds
     */
    private int mCurrentDeletedId = 0;

    /**
     * Array to store the people ids of contacts to delete.
     */
    private DynamicArrayLong mDeletedIds = new DynamicArrayLong(10);

    /**
     * The result status.
     */
    private int mResult = RESULT_UNDEFINED;

    /**
     * Instance of a ContactChange comparator.
     */
    private NCCComparator mNCCC = new NCCComparator();

    /**
     * The array of accounts from where to import the native contacts. Note: if
     * null, will import from the platform default account.
     */
    private Account[] mAccounts = null;

    /**
     * Boolean that tracks if the first time Import for 2.X is ongoing.
     */
    private boolean mIsFirstImportOn2X = false;


    /**
     * Constructor.
     * 
     * @param pca handler to the People contacts Api
     * @param nca handler to the Native contacts Api
     * @param firstTimeImport true if we import from native for the first time,
     *            false if not
     */
    public NativeImporter(PeopleContactsApi pca, NativeContactsApi nca, boolean firstTimeImport) {

        mPeopleContactsApi = pca;
        mNativeContactsApi = nca;

        initAccounts(firstTimeImport);
    }

    /**
     * Sets the accounts used to import the native contacs.
     */
    private void initAccounts(boolean firstTimeImport) {

        /**
         * In case of Android 2.X, the accounts used are different depending if
         * it's first time sync or not. On Android 1.X, we can just ignore the
         * accounts logic as not supported by the platform. At first time sync,
         * we need to import the native contacts from all the Google accounts.
         * These native contacts are then stored in the 360 People account and
         * native changes will be only detected from the 360 People account.
         */

        if (VersionUtils.is2XPlatform()) {
            // account to import from: 360 account if created, all the google
            // accounts otherwise
            if (firstTimeImport) {
                ArrayList<Account> accountList = new ArrayList<Account>();
                
                Account googleAccounts[] = mNativeContactsApi.getAccountsByType(NativeContactsApi.GOOGLE_ACCOUNT_TYPE);
                if (googleAccounts!=null ){
                  for (Account account : googleAccounts) {
                      accountList.add(account);
                  }
                }
                
                Account phoneAccounts[] = mNativeContactsApi.getAccountsByType(NativeContactsApi.PHONE_ACCOUNT_TYPE);
                if (phoneAccounts!=null){
                  for (Account account : phoneAccounts) {
                     accountList.add(account);
                  }
                }
                mAccounts = accountList.toArray(new Account[0]);
            } else {
                mAccounts = mNativeContactsApi
                        .getAccountsByType(NativeContactsApi.PEOPLE_ACCOUNT_TYPE);
            }

            if (firstTimeImport)
                mIsFirstImportOn2X = true;
        }
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
     * Tick method to call each time there is time for processing the native
     * contacts import. Note: the method will block for some time to process a
     * certain amount of contact then will return. It will have to be called
     * until it returns true meaning that the import is over.
     * 
     * @return true when the import task is finished, false if not
     */
    public boolean tick() {
    	
    	long startTime = System.currentTimeMillis();

        switch (mState) {
            case STATE_GET_IDS_LISTS:
                getIdsLists();
                break;
            case STATE_ITERATE_THROUGH_IDS:
                iterateThroughNativeIds();
                break;
            case STATE_PROCESS_DELETED:
                processDeleted();
                break;
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        float factor = TARGET_TIME_PER_TICK / processingTime;
        mContactsPerTick = Math.max(1.0F, mContactsPerTick * factor);
        
        LogUtils.logD("NativeImporter.tick(): Tick took " + processingTime + "ms, applying factor "+ factor + ". Contacts per tick: " + mContactsPerTick);

        return isDone();
    }

    /**
     * Returns the import state.
     * 
     * @return true if the import is finished, false if not
     */
    public boolean isDone() {

        return mState == STATE_DONE;
    }

    /**
     * Gets the import result.
     * 
     * @see #RESULT_OK
     * @see #RESULT_ERROR
     * @see #RESULT_UNDEFINED
     * @return the import result
     */
    public int getResult() {

        return mResult;
    }

    /**
     * Gets the current position in the list of ids. This can be used to track
     * the current progress.
     * 
     * @see #getCount()
     * @return the last processed id position in the list of ids
     */
    public int getPosition() {

        return mProcessedIds;
    }

    /**
     * Gets the total number of ids to process.
     * 
     * @return the number of ids to process
     */
    public int getCount() {

        return mTotalIds;
    }

    /**
     * Gets the list of native and people contacts ids.
     */
    private void getIdsLists() {

        LogUtils.logD("NativeImporter.getIdsLists()");

        // Get the list of native ids for the contacts
        if (mAccounts == null || 0 == mAccounts.length) {

            // default account
            LogUtils.logD("NativeImporter.getIdsLists() - using default account");
            mNativeContactsIds = mNativeContactsApi.getContactIds(null);
        } else if (mAccounts.length == 1) {

            // one account
            LogUtils.logD("NativeImporter.getIdsLists() - one account found: " + mAccounts[0]);
            mNativeContactsIds = mNativeContactsApi.getContactIds(mAccounts[0]);
        } else {

            // we need to merge the ids from different accounts and sort them
            final DynamicArrayLong allIds = new DynamicArrayLong();
            LogUtils.logD("NativeImporter.getIdsLists() - more than one account found.");

            for (int i = 0; i < mAccounts.length; i++) {

                LogUtils.logD("NativeImporter.getIdsLists() - account=" + mAccounts[i]);
                final long[] ids = mNativeContactsApi.getContactIds(mAccounts[i]);
                if (ids != null) {

                    allIds.add(ids);
                }
            }

            mNativeContactsIds = allIds.toArray();
            // sort the ids
            // TODO: as the arrays to merge are sorted, consider merging while
            // keeping the sorting
            // which is faster than sorting them afterwards
            if (mNativeContactsIds != null) {

                Arrays.sort(mNativeContactsIds);
            }
        }

        // check if we have some work to do
        if (mNativeContactsIds == null) {

            complete(RESULT_OK);
            return;
        }

        // Get a list of native ids for the contacts we have in the People
        // database
        mPeopleNativeContactsIds = mPeopleContactsApi.getNativeContactsIds();

        mTotalIds = mNativeContactsIds.length;
        if (mPeopleNativeContactsIds != null) {

            mTotalIds += mPeopleNativeContactsIds.length;
        }

        mState = STATE_ITERATE_THROUGH_IDS;
    }

    /**
     * Iterates through the list of native and People ids to detect changes.
     */
    private void iterateThroughNativeIds() {

        LogUtils.logD("NativeImporter.iterateThroughNativeIds()");

        final int limit = Math.min(mNativeContactsIds.length, mCurrentNativeId
                + Math.round(mContactsPerTick));

        // TODO: remove the deleted state / queuing to deleted ids array and
        // loop with while (mProcessedIds < limit)
        while (mCurrentNativeId < limit) {

            if (mPeopleNativeContactsIds == null) {

                // no native contacts on people side, just add it
                LogUtils.logD("NativeImporter.iterateThroughNativeIds(): found a new contact");
                addNewContact(mNativeContactsIds[mCurrentNativeId]);
                mProcessedIds++;
            } else {

                // both ids lists are ordered by ascending ids so
                // every people ids that are before the current native ids
                // are simply deleted contacts
                while ((mCurrentPeopleId < mPeopleNativeContactsIds.length)
                        && (mPeopleNativeContactsIds[mCurrentPeopleId] < mNativeContactsIds[mCurrentNativeId])) {
                    LogUtils
                            .logD("NativeImporter.iterateThroughNativeIds(): found a contact to delete");
                    mDeletedIds.add(mPeopleNativeContactsIds[mCurrentPeopleId++]);
                }

                if (mCurrentPeopleId == mPeopleNativeContactsIds.length
                        || mPeopleNativeContactsIds[mCurrentPeopleId] > mNativeContactsIds[mCurrentNativeId]) {
                    // has to be a new contact
                    LogUtils.logD("NativeImporter.iterateThroughNativeIds(): found a new contact");
                    addNewContact(mNativeContactsIds[mCurrentNativeId]);
                    mProcessedIds++;
                } else {
                    // has to be an existing contact or one that will be deleted
                    LogUtils
                            .logD("NativeImporter.iterateThroughNativeIds(): check existing contact");
                    checkExistingContact(mNativeContactsIds[mCurrentNativeId]);
                    mProcessedIds++;
                    mCurrentPeopleId++;
                }
            }

            mCurrentNativeId++;
        }

        // check if we are done with ids list from native
        if (mCurrentNativeId == mNativeContactsIds.length) {

            // we've gone through the native list, any remaining ids from the
            // people list are deleted ones
            if (mPeopleNativeContactsIds != null) {
                while (mCurrentPeopleId < mPeopleNativeContactsIds.length) {
                    LogUtils
                            .logD("NativeImporter.iterateThroughNativeIds(): found a contact to delete");
                    mDeletedIds.add(mPeopleNativeContactsIds[mCurrentPeopleId++]);
                }
            }

            if (mDeletedIds.size() != 0) {
                // Some deleted contacts to handle
                mState = STATE_PROCESS_DELETED;
            } else {
                // Nothing else to do
                complete(RESULT_OK);
            }
        }
    }

    /**
     * Deletes the contacts that were added the deleted array.
     */
    private void processDeleted() {

        LogUtils.logD("NativeImporter.processDeleted()");

        final int limit = Math.min(mDeletedIds.size(), mCurrentDeletedId
                + Math.round(mContactsPerTick));

        while (mCurrentDeletedId < limit) {

            // we now delete the contacts on people client side
            // on the 2.X platform, the contact deletion has to be synced back
            // to native once completed because the
            // contact is still there on native, just marked as deleted and
            // waiting for its explicit removal
            mPeopleContactsApi.deleteNativeContact(mDeletedIds.get(mCurrentDeletedId++),
                    VersionUtils.is2XPlatform());
            mProcessedIds++;
        }

        if (mCurrentDeletedId == mDeletedIds.size()) {

            complete(RESULT_OK);
        }
    }

    /**
     * Adds a new contact to the people database.
     */
    private void addNewContact(long nativeId) {

        // get the contact data
        final ContactChange[] contactChanges = mNativeContactsApi.getContact(nativeId);

        if (contactChanges != null) {

            if (mIsFirstImportOn2X) {

                // Override the nativeContactId with an invalid id if we are on
                // 2.X
                // and we are doing a first time import because the native id
                // does not correspond
                // to the id from the 360 People account where we will export.
                removeNativeIds(contactChanges);
            } else {

                // Force a nativeDetailId to details that have none so that the
                // comparison
                // later can be made (see computeDelta method).
                forceNabDetailId(contactChanges);
            }

            // add the contact to the People database
            if (!mPeopleContactsApi.addNativeContact(contactChanges)) {

                // TODO: Handle the error case !!! Well how should we handle it:
                // fail for all remaining contacts or skip the failing contact?
                LogUtils
                        .logE("NativeImporter.addNewContact() - failed to import native contact id="
                                + nativeId);
            }
        }
    }

    /**
     * Check changes between an existing contact on both native and people
     * database.
     * 
     * @param nativeId the native id of the contact to check
     */
    private void checkExistingContact(long nativeId) {

        // get the native version of that contact
        final ContactChange[] nativeContact = mNativeContactsApi.getContact(nativeId);

        // get the people version of that contact
        final ContactChange[] peopleContact = mPeopleContactsApi.getContact((int)nativeId);

        if (peopleContact == null) {

            // we shouldn't be in that situation but nothing to do about it
            // this means that there were some changes in the meantime between
            // getting the ids list and now
            return;
        } else if (nativeContact == null) {

            LogUtils
                    .logD("NativeImporter.checkExistingContact(): found a contact marked as deleted");
            // this is a 2.X specific case meaning that the contact is marked as
            // deleted on native side
            // and waiting for the "syncAdapter" to perform a real delete
            mDeletedIds.add(nativeId);

        } else {

            // general case, find the delta
            final ContactChange[] delta = computeDelta(peopleContact, nativeContact);

            if (delta != null) {

                // update CAB with delta changes
                mPeopleContactsApi.updateNativeContact(delta);
            }
        }

    }

    /**
     * Native ContactChange Comparator class. This class compares ContactChange
     * and tells which one is greater depending on the key and then the native
     * detail id.
     */
    private static class NCCComparator implements Comparator<ContactChange> {

        @Override
        public int compare(ContactChange change1, ContactChange change2) {

            // an integer < 0 if object1 is less than object2, 0 if they are
            // equal, and > 0 if object1 is greater than object2.
            final int key1 = change1.getKey();
            final int key2 = change2.getKey();

            if (key1 < key2) {

                return -1;
            } else if (key1 > key2) {

                return 1;
            } else {

                // the keys are identical, check the native ids
                final long id1 = change1.getNabDetailId();
                final long id2 = change2.getNabDetailId();

                if (id1 < id2) {

                    return -1;
                } else if (id1 > id2) {

                    return 1;
                } else {

                    return 0;
                }
            }
        }
    }

    /**
     * Sorts the provided array of ContactChange by key and native id. Note: the
     * method will rearrange the provided array.
     * 
     * @param changes the ContactChange array to sort
     */
    private void sortContactChanges(ContactChange[] changes) {

        if ((changes != null) && (changes.length > 1)) {

            Arrays.sort(changes, mNCCC);
        }
    }

    /**
     * Computes the difference between the provided arrays of ContactChange. The
     * delta are the changes to apply to the master ContactChange array to make
     * it similar to the new changes ContactChange array. (i.e. delete details,
     * add details or modify details) NOTE: to help the GC, some provided
     * ContactChange may be modified and returned by the method instead of
     * creating new ones.
     * 
     * @param masterChanges the master array of ContactChange (i.e. the original
     *            one)
     * @param newChanges the new ContactChange array (i.e. contains the new
     *            version of a contact)
     * @return null if there are no changes or the contact is being deleted, an
     *         array for ContactChange to apply to the master contact if
     *         differences where found
     */
    private ContactChange[] computeDelta(ContactChange[] masterChanges, ContactChange[] newChanges) {

        LogUtils.logD("NativeImporter.computeDelta()");

        // set the native contact id to details that don't have a native detail
        // id for the comparison as
        // this is also done on people database side
        forceNabDetailId(newChanges);
        // sort the changes by key then native detail id
        sortContactChanges(masterChanges);
        sortContactChanges(newChanges);

        // if the master contact is being deleted, ignore new changes
        if ((masterChanges[0].getType() & ContactChange.TYPE_DELETE_CONTACT) == ContactChange.TYPE_DELETE_CONTACT) {

            return null;
        }

        // details comparison, skip deleted master details
        final ContactChange[] deltaChanges = new ContactChange[masterChanges.length
                + newChanges.length];
        int deltaIndex = 0;
        int masterIndex = 0;
        int newIndex = 0;

        while (newIndex < newChanges.length) {

            while ((masterIndex < masterChanges.length)
                    && (mNCCC.compare(masterChanges[masterIndex], newChanges[newIndex]) < 0)) {

                final ContactChange masterChange = masterChanges[masterIndex];

                if ((masterChange.getType() & ContactChange.TYPE_DELETE_DETAIL) != ContactChange.TYPE_DELETE_DETAIL
                        && (masterChange.getNabDetailId() != ContactChange.INVALID_ID)
                        && (isContactChangeKeySupported(masterChange))) {

                    // this detail does not exist anymore, is not being deleted
                    // and was synced to native
                    // check if it can be deleted (or has to be updated)
                    setDetailForDeleteOrUpdate(masterChanges, masterIndex);
                    deltaChanges[deltaIndex++] = masterChanges[masterIndex];
                }
                masterIndex++;
            }

            if ((masterIndex < masterChanges.length)
                    && (mNCCC.compare(newChanges[newIndex], masterChanges[masterIndex]) == 0)) {

                // similar key and id, check for differences at value level and
                // flags
                final ContactChange masterDetail = masterChanges[masterIndex];
                final ContactChange newDetail = newChanges[newIndex];
                boolean different = false;

                if (masterDetail.getFlags() != newDetail.getFlags()) {
                    different = true;
                }
                if (!areContactChangeValuesEqualsPlusFix(masterChanges, masterIndex, newChanges,
                        newIndex)) {
                    different = true;
                }

                if (different) {
                    // found a detail to update
                    LogUtils.logD("NativeImporter.computeDelta() - found a detail to update");
                    newDetail.setType(ContactChange.TYPE_UPDATE_DETAIL);
                    newDetail.setInternalContactId(masterDetail.getInternalContactId());
                    newDetail.setInternalDetailId(masterDetail.getInternalDetailId());
                    deltaChanges[deltaIndex++] = newDetail;
                }

                masterIndex++;
            } else {

                LogUtils.logD("NativeImporter.computeDelta() - found a detail to add");
                // this is a new detail
                newChanges[newIndex].setType(ContactChange.TYPE_ADD_DETAIL);
                newChanges[newIndex].setInternalContactId(masterChanges[0].getInternalContactId());
                deltaChanges[deltaIndex++] = newChanges[newIndex];
            }

            newIndex++;
        }

        while (masterIndex < masterChanges.length) {

            final ContactChange masterChange = masterChanges[masterIndex];

            if ((masterChange.getType() & ContactChange.TYPE_DELETE_DETAIL) != ContactChange.TYPE_DELETE_DETAIL
                    && (masterChange.getNabDetailId() != ContactChange.INVALID_ID)
                    && (isContactChangeKeySupported(masterChange))) {

                // this detail does not exist anymore, is not being deleted and
                // was synced to native
                // check if it can be deleted (or has to be updated)
                setDetailForDeleteOrUpdate(masterChanges, masterIndex);
                deltaChanges[deltaIndex++] = masterChanges[masterIndex];
            }
            masterIndex++;
        }

        if (deltaIndex == 0) {

            // the contact has not changed
            return null;
        } else if (deltaChanges.length == deltaIndex) {

            // give the detail changes
            return deltaChanges;
        } else {

            // give the detail changes but need to trim
            final ContactChange[] trim = new ContactChange[deltaIndex];
            System.arraycopy(deltaChanges, 0, trim, 0, deltaIndex);
            return trim;
        }
    }

    /**
     * Removes the native ids in case of a first time import on the Android 2.X
     * platform.
     * 
     * @param contact the contact to clear from native ids
     */
    private void removeNativeIds(ContactChange[] contact) {

        if (contact != null) {

            final int count = contact.length;

            for (int i = 0; i < count; i++) {

                final ContactChange change = contact[i];
                change.setNabContactId(ContactChange.INVALID_ID);
                change.setNabDetailId(ContactChange.INVALID_ID);
            }
        }
    }

    /**
     * Forces a native detail id onto a detail that does not have one by setting
     * it to be the native contact id.
     * 
     * @param contact the contact to fix
     */
    private void forceNabDetailId(ContactChange[] contact) {

        if (contact != null && contact.length > 0) {

            final long nativeContactId = contact[0].getNabContactId();
            final int count = contact.length;

            for (int i = 0; i < count; i++) {

                final ContactChange change = contact[i];
                if (change.getNabDetailId() == ContactChange.INVALID_ID) {
                    change.setNabDetailId(nativeContactId);
                }
            }
        }
    }

    /*
     * Below this point, all the defined methods where added to support the
     * specific case of KEY_VCARD_ORG on Android 1.X platform. KEY_VCARD_ORG is
     * defined as followed: "company;department1;department2;...;departmentX" It
     * is a special case because this VCard key on its own is not fully
     * supported on the Android 1.X platform: only "company" information is.
     */

    /**
     * Tells whether or not a master change is equal to a new change and
     * performs some modifications on the provided new change in the specific
     * case of KEY_VCARD_ORG on Android 1.X.
     * 
     * @param masterChanges the array of master changes
     * @param masterIndex the index of the change within the array of master
     *            changes
     * @param newChanges the array of new changes
     * @param newIndex the index of the change within the array of new changes
     * @return true if the changes are equals, false otherwise
     */
    private boolean areContactChangeValuesEqualsPlusFix(ContactChange[] masterChanges,
            int masterIndex, ContactChange[] newChanges, int newIndex) {

        final ContactChange masterChange = masterChanges[masterIndex];
        final ContactChange newChange = newChanges[newIndex];

        if (VersionUtils.is2XPlatform() || masterChange.getKey() != ContactChange.KEY_VCARD_ORG) {

            // general case
            return TextUtils.equals(masterChange.getValue(), newChange.getValue());
        } else {

            // this is a special case of Android 1.X where we have to parse the
            // value
            // in case of Organization key
            final String peopleCompValue = VCardHelper.parseCompanyFromOrganization(masterChange
                    .getValue());
            final String nativeCompValue = VCardHelper.parseCompanyFromOrganization(newChange
                    .getValue());

            // on 1.X, we only need to compare the company name as
            // department is not supported by the platform
            final boolean areEquals = TextUtils.equals(peopleCompValue, nativeCompValue);

            if (!areEquals) {

                // there is a difference so master change will be updated
                // we need to preserve the master department values if any
                final String masterDepartments = VCardHelper
                        .splitDepartmentsFromOrganization(masterChange.getValue());

                final ContactChange fixedNewChange = newChange.copyWithNewValue(nativeCompValue
                        + masterDepartments);
                newChanges[newIndex] = fixedNewChange;
            }

            return areEquals;
        }
    }

    /**
     * Sets a detail as to be deleted or updated. Note: in the general case, the
     * detail has to be deleted but in the specific situation of KEY_VCARD_ORG
     * and Android 1.X, it may have to be updated instead
     * 
     * @param contactChanges the array of ContactChange
     * @param index the index of the ContactChange to set in the array of
     *            ContactChange
     */
    private void setDetailForDeleteOrUpdate(ContactChange[] contactChanges, int index) {

        final ContactChange contactChange = contactChanges[index];

        if (VersionUtils.is2XPlatform() || contactChange.getKey() != ContactChange.KEY_VCARD_ORG) {

            // general case, just set the details as deleted
            contactChange.setType(ContactChange.TYPE_DELETE_DETAIL);
            LogUtils
                    .logD("NativeImporter.checkDetailForDeleteOrUpdate() - found a detail to delete");
        } else {

            // this is a special case of Android 1.X
            // we have to set this change to an update if the change
            // contains departments
            final String masterDepartments = VCardHelper
                    .splitDepartmentsFromOrganization(contactChange.getValue());

            if (!VCardHelper.isEmptyVCardValue(masterDepartments)) {

                // delete the company name and keep the departments
                contactChange.setType(ContactChange.TYPE_UPDATE_DETAIL);
                contactChanges[index] = contactChange.copyWithNewValue(masterDepartments);

                LogUtils
                        .logD("NativeImporter.checkDetailForDeleteOrUpdate() - found a detail to update");
            } else {
                contactChange.setType(ContactChange.TYPE_DELETE_DETAIL);
                LogUtils
                        .logD("NativeImporter.checkDetailForDeleteOrUpdate() - found a detail to delete");
            }
        }
    }

    /**
     * Determines whether or not a ContactChange key is supported on native
     * side. Note: it also handle the non generic case of KEY_VCARD_ORG on
     * Android 1.X
     * 
     * @param change the ContactChange to check
     * @return true if supported, false if not
     */
    private boolean isContactChangeKeySupported(ContactChange change) {

        final boolean isSupported = mNativeContactsApi.isKeySupported(change.getKey());

        if (VersionUtils.is2XPlatform() || change.getKey() != ContactChange.KEY_VCARD_ORG) {

            return isSupported;
        } else if (isSupported) {

            // KEY_VCARD_ORG has the following value:
            // "company;department1;department2..."
            // in case of KEY_VCARD_ORG on Android 1.X, we have to check the
            // support of the key
            // at the company level of the VCard value because the departments
            // are not supported
            // so if there is only the department, we have to return false
            // instead of true.
            final String changeCompValue = VCardHelper.parseCompanyFromOrganization(change
                    .getValue());

            if (!VCardHelper.isEmptyVCardValue(changeCompValue)) {
                return true;
            }
        }

        return false;
    }
}
