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
import java.util.HashMap;
import java.util.List;

import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.text.TextUtils;
import android.util.SparseArray;

import com.vodafone360.people.R;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.VCardHelper.Name;
import com.vodafone360.people.datatypes.VCardHelper.Organisation;
import com.vodafone360.people.datatypes.VCardHelper.PostalAddress;
import com.vodafone360.people.utils.CursorUtils;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * The implementation of the NativeContactsApi for the Android 2.X platform.
 */
public class NativeContactsApi2 extends NativeContactsApi {
    /**
     * Convenience Projection to fetch only a Raw Contact's ID and Native
     * Account Type
     */
    private static final String[] CONTACTID_PROJECTION = new String[] {
            RawContacts._ID, RawContacts.ACCOUNT_TYPE
    };

    /**
     * Raw ID Column for the CONTACTID_PROJECTION Projection
     */
    private static final int CONTACTID_PROJECTION_RAW_ID = 0;

    /**
     * Account Type Column for the CONTACTID_PROJECTION Projection
     */
    private static final int CONTACTID_PROJECTION_ACCOUNT_TYPE = 1;

    /**
     * Group ID Projection
     */
    private static final String[] GROUPID_PROJECTION = new String[] {
        Groups._ID
    };

    /**
     * Vendor specific account. Only used in 2.x API.
     */
    private static Account sPhoneAccount = null;

    /**
     * Regular expression for a date that can be handled by the People Client at
     * present. Matches the following cases: N-n-n n-n-N Where: - 'n'
     * corresponds to one or two digits - 'N' corresponds to two or 4 digits
     */
    private static final String COMPLETE_DATE_REGEX = "(?:^\\d{2,4}-\\d{1,2}-\\d{1,2}$|^\\d{1,2}-\\d{1,2}-\\d{2,4}$)";

    /**
     * 'My Contacts' System group where clause
     */
    private static final String MY_CONTACTS_GROUP_WHERE_CLAUSE = Groups.SYSTEM_ID + "=\"Contacts\"";

    /**
     * 'My Contacts' System Group Membership where in clause (Multiple 'My
     * Contacts' IDs)
     */
    private static final String MY_CONTACTS_MULTI_GROUP_MEMBERSHIP = Data.MIMETYPE + "=\""
            + GroupMembership.CONTENT_ITEM_TYPE + "\" AND " + Data.DATA1 + " IN (";

    /**
     * Selection where clause for a NULL Account
     */
    private static final String NULL_ACCOUNT_WHERE_CLAUSE = RawContacts.ACCOUNT_NAME
            + " IS NULL AND " + RawContacts.ACCOUNT_TYPE + " IS NULL";

    /**
     * Selection where clause for an Organization detail.
     */
    private static final String ORGANIZATION_DETAIL_WHERE_CLAUSE = RawContacts.Data.MIMETYPE
            + "=\"" + Organization.CONTENT_ITEM_TYPE + "\"";

    /**
     * The list of Uri that need to be listened to for contacts changes on
     * native side.
     */
    private static final Uri[] sUri = {
            ContactsContract.RawContacts.CONTENT_URI, ContactsContract.Data.CONTENT_URI
    };

    /**
     * Holds mapping from a NAB type (MIME) to a VCard Key
     */
    private static final HashMap<String, Integer> sFromNabContentTypeToKeyMap;

    /**
     * Holds mapping from a VCard Key to a NAB type (MIME)
     */
    private static final SparseArray<String> sFromKeyToNabContentTypeArray;

    static {
        sFromNabContentTypeToKeyMap = new HashMap<String, Integer>(9, 1);
        sFromNabContentTypeToKeyMap.put(StructuredName.CONTENT_ITEM_TYPE,
                ContactChange.KEY_VCARD_NAME);
        sFromNabContentTypeToKeyMap.put(Nickname.CONTENT_ITEM_TYPE,
                ContactChange.KEY_VCARD_NICKNAME);
        sFromNabContentTypeToKeyMap.put(Phone.CONTENT_ITEM_TYPE, ContactChange.KEY_VCARD_PHONE);
        sFromNabContentTypeToKeyMap.put(Email.CONTENT_ITEM_TYPE, ContactChange.KEY_VCARD_EMAIL);
        sFromNabContentTypeToKeyMap.put(StructuredPostal.CONTENT_ITEM_TYPE,
                ContactChange.KEY_VCARD_ADDRESS);
        sFromNabContentTypeToKeyMap
                .put(Organization.CONTENT_ITEM_TYPE, ContactChange.KEY_VCARD_ORG);
        sFromNabContentTypeToKeyMap.put(Website.CONTENT_ITEM_TYPE, ContactChange.KEY_VCARD_URL);
        sFromNabContentTypeToKeyMap.put(Note.CONTENT_ITEM_TYPE, ContactChange.KEY_VCARD_NOTE);
        sFromNabContentTypeToKeyMap.put(Event.CONTENT_ITEM_TYPE, ContactChange.KEY_VCARD_DATE);
        // sFromNabContentTypeToKeyMap.put(
        // Photo.CONTENT_ITEM_TYPE, ContactChange.KEY_PHOTO);

        sFromKeyToNabContentTypeArray = new SparseArray<String>(10);
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_NAME,
                StructuredName.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_NICKNAME,
                Nickname.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray
                .append(ContactChange.KEY_VCARD_PHONE, Phone.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray
                .append(ContactChange.KEY_VCARD_EMAIL, Email.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_ADDRESS,
                StructuredPostal.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_ORG,
                Organization.CONTENT_ITEM_TYPE);
        // Special case: VCARD_TITLE maps to the same NAB type as
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_TITLE,
                Organization.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray
                .append(ContactChange.KEY_VCARD_URL, Website.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_NOTE, Note.CONTENT_ITEM_TYPE);
        sFromKeyToNabContentTypeArray.append(ContactChange.KEY_VCARD_DATE, Event.CONTENT_ITEM_TYPE);
        // sFromKeyToNabContentTypeArray.append(
        // ContactChange.KEY_PHOTO, Photo.CONTENT_ITEM_TYPE);
    }

    /**
     * The observer registered by the upper layer.
     */
    private ContactsObserver mAbstractedObserver;

    /**
     * Content values used for writing to NAB.
     */
    private final ContentValues mValues = new ContentValues();

    /**
     * The content observers that listens for native contacts changes.
     */
    private final ContentObserver[] mContentObservers = new ContentObserver[sUri.length];

    /**
     * Array of row ID in the groups table to the "My Contacts" System Group.
     * The reason for this being an array is because there may be multiple
     * "My Contacts" groups (Platform Bug!?).
     */
    private long[] mMyContactsGroupRowIds = null;

    /**
     * Arguably another example where Organization and Title force us into extra
     * effort. We use this value to pass the correct detail ID when an 'add
     * detail' is done for one the two although the other is already present.
     * Make sure to reset this value for every UpdateContact operation
     */
    private long mExistingOrganizationId = ContactChange.INVALID_ID;

    /**
     * Flag to check if we have already read a Birthday detail
     */
    private boolean mHaveReadBirthday = false;

    /**
     * Yield value for our ContentProviderOperations.
     */
    private boolean mYield = true;

    /**
     * Batch used for Contact Writing operations.
     */
    private BatchOperation mBatch = new BatchOperation();

    /**
     * Inner class for applying batches. TODO: Move to own class if batches
     * become supported in other areas
     */
    private class BatchOperation {
        // List for storing the batch mOperations
        ArrayList<ContentProviderOperation> mOperations;

        /**
         * Default constructor
         */
        public BatchOperation() {
            mOperations = new ArrayList<ContentProviderOperation>();
        }

        /**
         * Current size of the batch
         * 
         * @return Size of the batch
         */
        public int size() {
            return mOperations.size();
        }

        /**
         * Adds a new operation to the batch
         * 
         * @param cpo The
         */
        public void add(ContentProviderOperation cpo) {
            mOperations.add(cpo);
        }

        /**
         * Clears all operations in the batch Effectively resets the batch.
         */
        public void clear() {
            mOperations.clear();
        }

        public ContentProviderResult[] execute() {
            ContentProviderResult[] resultArray = null;

            if (mOperations.size() > 0) {
                // Apply the mOperations to the content provider
                try {
                    resultArray = mCr.applyBatch(ContactsContract.AUTHORITY, mOperations);
                } catch (final OperationApplicationException e1) {
                    LogUtils.logE("storing contact data failed", e1);
                } catch (final RemoteException e2) {
                    LogUtils.logE("storing contact data failed", e2);
                }
                mOperations.clear();
            }

            return resultArray;
        }
    }

    /**
     * Convenience interface to map the generic DATA column names to the People
     * profile detail column names.
     */
    private interface PeopleProfileColumns {
        /**
         * 360 People profile MIME Type
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.vodafone360.people.profile";

        /**
         * 360 People Contact's profile ID (Corresponds to Contact's Internal
         * Contact ID)
         */
        public static final String DATA_PID = Data.DATA1;

        /**
         * 360 People Contact profile Summary
         */
        public static final String DATA_SUMMARY = Data.DATA2;

        /**
         * 360 People Contact profile detail
         */
        public static final String DATA_DETAIL = Data.DATA3;
    }

    /**
     * This class holds a native content observer that will be notified in case
     * of changes on the registered URI.
     */
    private class NativeContentObserver extends ContentObserver {

        public NativeContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            LogUtils.logI("NativeContactsApi2.NativeContentObserver.onChange(" + selfChange + ")");
            mAbstractedObserver.onChange();
        }
    }

    /**
     * @see NativeContactsApi#registerObserver(ContactsObserver)
     */
    @Override
    public void registerObserver(ContactsObserver observer) {
        LogUtils.logI("NativeContactsApi2.registerObserver()");
        if (mAbstractedObserver != null) {
            throw new RuntimeException(
                    "Only one observer at a time supported... Please unregister first.");
        }

        mAbstractedObserver = observer;

        final ContentResolver cr = mContext.getContentResolver();
        for (int i = 0; i < mContentObservers.length; i++) {
            mContentObservers[i] = new NativeContentObserver();
            cr.registerContentObserver(sUri[i], true, mContentObservers[i]);
        }
    }

    /**
     * @see NativeContactsApi#unregisterObserver(ContactsObserver)
     */
    @Override
    public void unregisterObserver() {
        LogUtils.logI("NativeContactsApi2.unregisterObserver()");
        if (mCr != null) {
            mAbstractedObserver = null;
            for (int i = 0; i < mContentObservers.length; i++) {
                if (mContentObservers[i] != null) {
                    mCr.unregisterContentObserver(mContentObservers[i]);
                    mContentObservers[i] = null;
                }
            }
        }
    }

    /**
     * @see NativeContactsApi#initialize()
     */
    @Override
    protected void initialize() {
        // perform here any one time initialization
        sPhoneAccount = getVendorSpecificAccount();
    }

    /**
     * @see NativeContactsApi#getAccounts()
     */
    @Override
    public Account[] getAccounts() {
        AccountManager accountManager = AccountManager.get(mContext);
        final android.accounts.Account[] accounts2xApi = accountManager.getAccounts();

        Account[] accounts = null;

        if (accounts2xApi.length > 0) {
            accounts = new Account[accounts2xApi.length];
            for (int i = 0; i < accounts2xApi.length; i++) {
                accounts[i] = new Account(accounts2xApi[i].name, accounts2xApi[i].type);
            }
        }

        return accounts;
    }

    /**
     * Reads vendor specific accounts from settings and through accountmanager.
     * Some phones with custom ui like sense have additional account that we
     * have to take care of. This method tryes to read them
     * 
     * @return Account object if vendor specific account is found, null
     *         otherwise
     */
    public Account getVendorSpecificAccount() {

        // first read the settings
        String[] PROJECTION = {
                Settings.ACCOUNT_NAME, Settings.ACCOUNT_TYPE, Settings.UNGROUPED_VISIBLE
        };

        // Get a cursor with all people
        Cursor cursor = mCr.query(Settings.CONTENT_URI, PROJECTION, null, null, null);
        // Got no cursor? Return with null!
        if (null == cursor) {
            return null;
        }

        try {
            String[] values = new String[cursor.getCount()];
            for (int i = 0; i < values.length; i++) {
                cursor.moveToNext();

                if (isVendorSpecificAccount(cursor.getString(1))) {
                    return new Account(cursor.getString(0), cursor.getString(1));
                }
            }
        } catch (Exception exc) {
            return null;
        } 
        CursorUtils.closeCursor(cursor);

        // nothing found in the settings? try accountmanager
        Account[] accounts = getAccounts();
        if (accounts == null) {
            return null;
        }
        for (Account account : accounts) {
            if (isVendorSpecificAccount(account.getType())) {
                return account;
            }

        }
        return null;
    }

    /**
     * @see NativeContactsApi2#getAccountsByType(String)
     */
    @Override
    public Account[] getAccountsByType(int type) {
        switch (type) {

            // people and google type lead to the same block the difference is
            // then handled inside in two if statements.
            // Otherwise we would have a lot of redundant code,
            case PEOPLE_ACCOUNT_TYPE:
            case GOOGLE_ACCOUNT_TYPE: {
                AccountManager accountMan = AccountManager.get(mContext);
                android.accounts.Account[] accounts2xApi = null;
                // For people account set the people account type string...
                if (PEOPLE_ACCOUNT_TYPE == type) {
                    accounts2xApi = accountMan.getAccountsByType(PEOPLE_ACCOUNT_TYPE_STRING);
                }
                // .. and for google the same only with google string.
                if (GOOGLE_ACCOUNT_TYPE == type) {
                    accounts2xApi = accountMan.getAccountsByType(GOOGLE_ACCOUNT_TYPE_STRING);
                }

                final int numAccounts = accounts2xApi.length;
                if (numAccounts > 0) {
                    Account[] accounts = new Account[numAccounts];
                    for (int i = 0; i < numAccounts; i++) {
                        accounts[i] = new Account(accounts2xApi[i].name, accounts2xApi[i].type);
                    }
                    return accounts;
                } else {
                    return null;
                }
            }
            case PHONE_ACCOUNT_TYPE: {
                if (sPhoneAccount == null) {
                    return null;
                }
                return new Account[] {
                    sPhoneAccount
                };
            }
            default:
                return null;
        }

    }

    /**
     * @see NativeContactsApi#addPeopleAccount(String)
     */
    @Override
    public boolean addPeopleAccount(String username) {
        boolean isAdded = false;
        try {
            android.accounts.Account account = new android.accounts.Account(username,
                    PEOPLE_ACCOUNT_TYPE_STRING);
            AccountManager accountMan = AccountManager.get(mContext);
            isAdded = accountMan.addAccountExplicitly(account, null, null);
            if (isAdded) {
                if (VersionUtils.isHtcSenseDevice(mContext)) {
                    createSettingsEntryForAccount(username);
                }
                // TODO: RE-ENABLE SYNC VIA SYSTEM
                ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 0);
                // ContentResolver.setSyncAutomatically(account,
                // ContactsContract.AUTHORITY, true);
            }
        } catch (Exception ex) {
            LogUtils.logE("People Account creation failed because of exception:\n", ex);
        }
        if (isAdded) {
            // Updating MyContacts Group IDs here for now because it needs to be
            // done one time just before first time sync.
            // In the future, this code may change if we modify
            // the way we retrieve contact IDs.
            fetchMyContactsGroupRowIds();
        }

        return isAdded;
    }

    /**
     * @see NativeContactsApi#isPeopleAccountCreated()
     */
    @Override
    public boolean isPeopleAccountCreated() {
        AccountManager accountMan = AccountManager.get(mContext);
        android.accounts.Account[] accounts = accountMan
                .getAccountsByType(PEOPLE_ACCOUNT_TYPE_STRING);
        return accounts != null && accounts.length > 0;
    }

    /**
     * @see NativeContactsApi#removePeopleAccount()
     */
    @Override
    public void removePeopleAccount() {
        AccountManager accountMan = AccountManager.get(mContext);
        android.accounts.Account[] accounts = accountMan
                .getAccountsByType(PEOPLE_ACCOUNT_TYPE_STRING);
        if (accounts != null && accounts.length > 0) {
            accountMan.removeAccount(accounts[0], null, null);
        }
    }

    /**
     * @see NativeContactsApi#getContactIds(Account)
     */
    @Override
    public long[] getContactIds(Account account) {
        // Need to construct a where clause
        if (account != null) {
            final StringBuffer clauseBuffer = new StringBuffer();

            clauseBuffer.append(RawContacts.ACCOUNT_NAME);
            clauseBuffer.append("=\"");
            clauseBuffer.append(account.getName());
            clauseBuffer.append("\" AND ");
            clauseBuffer.append(RawContacts.ACCOUNT_TYPE);
            clauseBuffer.append("=\"");
            clauseBuffer.append(account.getType());
            clauseBuffer.append('\"');

            return getContactIds(clauseBuffer.toString());
        } else {
            return getContactIds(NULL_ACCOUNT_WHERE_CLAUSE);
        }
    }

    /**
     * @see NativeContactsApi#getContact(long)
     */
    @Override
    public ContactChange[] getContact(long nabContactId) {
        // Reset the boolean flags
        mHaveReadOrganization = false;
        mHaveReadBirthday = false;

        final Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, nabContactId);
        final Uri dataUri = Uri.withAppendedPath(rawContactUri, RawContacts.Data.CONTENT_DIRECTORY);
        final Cursor cursor = mCr.query(dataUri, null, null, null, null);
        try {
            if (cursor != null && cursor.getCount() > 0) {
                final List<ContactChange> ccList = new ArrayList<ContactChange>();
                while (cursor.moveToNext()) {
                    readDetail(cursor, ccList, nabContactId);
                }

                return ccList.toArray(new ContactChange[ccList.size()]);
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }

        return null;
    }

    /**
     * @see NativeContactsApi#addContact(Account, ContactChange[])
     */
    @Override
    public ContactChange[] addContact(Account account, ContactChange[] ccList) {
        // Make sure to reset all the member variables we need
        mYield = true;
        mMarkedOrganizationIndex = mMarkedTitleIndex = -1;
        mValues.clear();
        mBatch.clear();

        mValues.put(RawContacts.ACCOUNT_TYPE, account.getType());
        mValues.put(RawContacts.ACCOUNT_NAME, account.getName());

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI)).withYieldAllowed(mYield)
                .withValues(mValues);
        mBatch.add(builder.build());

         // according to the calling method ccList here has at least 1 element, ccList[0] is not null
        final int ccListSize = ccList.length;
        for (int i = 0; i < ccListSize; i++) {
            final ContactChange cc = ccList[i];
            if (cc != null) {
                final int key = cc.getKey();
                if (key == ContactChange.KEY_VCARD_ORG && mMarkedOrganizationIndex < 0) {
                    // Mark for later writing
                    mMarkedOrganizationIndex = i;
                    continue;
                }
                if (key == ContactChange.KEY_VCARD_TITLE && mMarkedTitleIndex < 0) {
                    // Mark for later writing
                    mMarkedTitleIndex = i;
                    continue;
                }
                putDetail(cc);
                addValuesToBatch(ContactChange.INVALID_ID);
            }
        }

        // Special case for the organization/title if it was flagged in the
        // putDetail call
        putOrganization(ccList);
        addValuesToBatch(ContactChange.INVALID_ID);

        // Add 360 profile detail
         addProfileAction(ccList[0].getInternalContactId());

        // Execute the batch and Generate ID changes from it
        return executeNewContactBatch(ccList);
    }

    /**
     * @see NativeContactsApi#updateContact(ContactChange[])
     */
    @Override
    public ContactChange[] updateContact(ContactChange[] ccList) {
        if (ccList == null || ccList.length == 0) {
            LogUtils.logW("NativeContactsApi2.updateContact() nothing to update - empty ccList!");
            return null;
        }

        // Make sure to reset all the member variables we need
        mYield = true;
        mBatch.clear();
        mValues.clear();
        mMarkedOrganizationIndex = mMarkedTitleIndex = -1;
        mExistingOrganizationId = ContactChange.INVALID_ID;

        final long nabContactId = ccList[0].getNabContactId();

        if (nabContactId == ContactChange.INVALID_ID) {
            LogUtils
                    .logW("NativeContactsApi2.updateContact() Ignoring update request because of invalid NAB Contact ID. Internal Contact ID is "
                            + ccList[0].getInternalContactId());
            return null;
        }

        final int ccListSize = ccList.length;

        for (int i = 0; i < ccListSize; i++) {
            final ContactChange cc = ccList[i];
            final int key = cc.getKey();
            if (key == ContactChange.KEY_VCARD_ORG && mMarkedOrganizationIndex < 0) {
                // Mark for later writing
                mMarkedOrganizationIndex = i;
                continue;
            }
            if (key == ContactChange.KEY_VCARD_TITLE && mMarkedTitleIndex < 0) {
                // Mark for later writing
                mMarkedTitleIndex = i;
                continue;
            }

            switch (cc.getType()) {
                case ContactChange.TYPE_ADD_DETAIL:
                    putDetail(cc);
                    addValuesToBatch(nabContactId); // not a new contact
                    break;
                case ContactChange.TYPE_UPDATE_DETAIL:
                    if (cc.getNabDetailId() != ContactChange.INVALID_ID) {
                        putDetail(cc);
                        addUpdateValuesToBatch(cc.getNabDetailId());
                    } else {
                        LogUtils
                                .logW("NativeContactsApi2.updateContact() Ignoring update to detail for "
                                        + cc.getKeyToString()
                                        + " because of invalid NAB Detail ID. Internal Contact is "
                                        + cc.getInternalContactId()
                                        + ", Internal Detail Id is "
                                        + cc.getInternalDetailId());
                    }
                    break;
                case ContactChange.TYPE_DELETE_DETAIL:
                    if (cc.getNabDetailId() != ContactChange.INVALID_ID) {
                        addDeleteDetailToBatch(cc.getNabDetailId());
                    } else {
                        LogUtils
                                .logW("NativeContactsApi2.updateContact() Ignoring detail deletion for "
                                        + cc.getKeyToString()
                                        + " because of invalid NAB Detail ID. Internal Contact is "
                                        + cc.getInternalContactId()
                                        + ", Internal Detail Id is "
                                        + cc.getInternalDetailId());
                    }
                    break;
                default:
                    break;
            }
        }

        updateOrganization(ccList, nabContactId);

        // Execute the batch and Generate ID changes from it
        return executeUpdateContactBatch(ccList);
    }

    /**
     * @see NativeContactsApi#removeContact(long)
     */
    @Override
    public void removeContact(long nabContactId) {
        mCr.delete(addCallerIsSyncAdapterParameter(ContentUris.withAppendedId(
                RawContacts.CONTENT_URI, nabContactId)), null, null);
    }

    /**
     * @see NativeContactsApi#isKeySupported(int)
     */
    @Override
    public boolean isKeySupported(int key) {
        // Only supported if in the SparseArray
        return sFromKeyToNabContentTypeArray.indexOfKey(key) >= 0;
    }

    /**
     * Inner (private) method for getting contact ids. Allows a cleaner
     * separation the public API method.
     * 
     * @param selection The where clause for the operation
     * @return An array containing the Contact IDs that have been found
     */
    private long[] getContactIds(final String selection) {
        // Store ids in a temporary array because of possible null values
        long[] tempIds = null;
        int idCount = 0;
        final Cursor cursor = mCr.query(RawContacts.CONTENT_URI, CONTACTID_PROJECTION, selection,
                null, null);

        if (cursor == null) {
            return null;
        }

        try {
            final int cursorCount = cursor.getCount();
            if (cursorCount > 0) {
                tempIds = new long[cursorCount];

                while (cursor.moveToNext()) {
                    final long id = cursor.getLong(CONTACTID_PROJECTION_RAW_ID);
                    final String accountType = cursor.getString(CONTACTID_PROJECTION_ACCOUNT_TYPE);
                    // TODO: Remove hardcoding (if statement)
                    if (TextUtils.isEmpty(accountType)
                            || accountType.equals(NativeContactsApi.PEOPLE_ACCOUNT_TYPE_STRING)
                            || isVendorSpecificAccount(accountType)
                            || isContactInMyContactsGroup(id)) {
                        tempIds[idCount] = id;
                        idCount++;
                    }
                }
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }

        if (idCount > 0) {
            // Here we copy the array to strip any eventual nulls at the end of
            // the tempIds array
            final long[] ids = new long[idCount];
            System.arraycopy(tempIds, 0, ids, 0, idCount);
            return ids;
        }
        return null;
    }

    /**
     * Fetches the IDs corresponding to the "My Contacts" System Group The
     * reason why we return an array is because there may be multiple
     * "My Contacts" groups.
     * 
     * @return Array containing all IDs of the "My Contacts" group or null if
     *         none exist
     */
    private void fetchMyContactsGroupRowIds() {
        final Cursor cursor = mCr.query(Groups.CONTENT_URI, GROUPID_PROJECTION,
                MY_CONTACTS_GROUP_WHERE_CLAUSE, null, null);

        if (cursor == null) {
            return;
        }

        try {
            final int count = cursor.getCount();
            if (count > 0) {
                mMyContactsGroupRowIds = new long[count];
                for (int i = 0; i < count; i++) {
                    cursor.moveToNext();
                    mMyContactsGroupRowIds[i] = cursor.getLong(0);
                }
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }
    }

    /**
     * Checks if a Contact is in the "My Contacts" System Group
     * 
     * @param nabContactId ID of the Contact to check
     * @return true if the Contact is in the Group, false if not
     */
    private boolean isContactInMyContactsGroup(long nabContactId) {
        boolean belongs = false;
        if (mMyContactsGroupRowIds != null) {
            final Uri dataUri = Uri.withAppendedPath(ContentUris.withAppendedId(
                    RawContacts.CONTENT_URI, nabContactId), RawContacts.Data.CONTENT_DIRECTORY);

            // build query containing row ID values
            final StringBuilder sb = new StringBuilder();
            sb.append(MY_CONTACTS_MULTI_GROUP_MEMBERSHIP);
            final int rowIdCount = mMyContactsGroupRowIds.length;
            for (int i = 0; i < rowIdCount; i++) {
                sb.append(mMyContactsGroupRowIds[i]);
                if (i < rowIdCount - 1) {
                    sb.append(',');
                }
            }

            sb.append(')');
            final Cursor cursor = mCr.query(dataUri, null, sb.toString(), null, null);
            try {
                belongs = cursor != null && cursor.getCount() > 0;
            } finally {
                CursorUtils.closeCursor(cursor);
            }
        }
        return belongs;
    }

    /**
     * Reads a Contact Detail from a Cursor into the supplied Contact Change
     * List.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to read Detail into
     * @param nabContactId NAB ID of the Contact
     */
    private void readDetail(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        final String mimetype = CursorUtils.getString(cursor, Data.MIMETYPE);
        final Integer key = sFromNabContentTypeToKeyMap.get(mimetype);
        if (key != null) {
            switch (key.intValue()) {
                case ContactChange.KEY_VCARD_NAME:
                    readName(cursor, ccList, nabContactId);
                    break;
                case ContactChange.KEY_VCARD_NICKNAME:
                    readNickname(cursor, ccList, nabContactId);
                    break;
                case ContactChange.KEY_VCARD_PHONE:
                    readPhone(cursor, ccList, nabContactId);
                    break;
                case ContactChange.KEY_VCARD_EMAIL:
                    readEmail(cursor, ccList, nabContactId);
                    break;
                case ContactChange.KEY_VCARD_ADDRESS:
                    readAddress(cursor, ccList, nabContactId);
                    break;
                case ContactChange.KEY_VCARD_ORG:
                    // Only one Organization can be read (CAB limitation!)
                    if (!mHaveReadOrganization) {
                        readOrganization(cursor, ccList, nabContactId);
                    }
                    break;
                case ContactChange.KEY_VCARD_URL:
                    readWebsite(cursor, ccList, nabContactId);
                    break;
                case ContactChange.KEY_VCARD_DATE:
                    if (!mHaveReadBirthday) {
                        readBirthday(cursor, ccList, nabContactId);
                    }
                    break;
                default:
                    // The default case is also a valid key
                    final String value = CursorUtils.getString(cursor, Data.DATA1);
                    if (!TextUtils.isEmpty(value)) {
                        final long nabDetailId = CursorUtils.getLong(cursor, Data._ID);
                        final ContactChange cc = new ContactChange(key, value,
                                ContactChange.FLAG_NONE);
                        cc.setNabContactId(nabContactId);
                        cc.setNabDetailId(nabDetailId);
                        ccList.add(cc);
                    }
                    break;
            }
        }
    }

    /**
     * Reads an name detail as a {@link ContactChange} from the provided cursor.
     * For this type of detail we need to use a VCARD (semicolon separated)
     * value.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readName(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        // Using display name only to check if there is a valid name to read
        final String displayName = CursorUtils.getString(cursor, StructuredName.DISPLAY_NAME);
        if (!TextUtils.isEmpty(displayName)) {
            final long nabDetailId = CursorUtils.getLong(cursor, StructuredName._ID);
            // VCard Helper data type (CAB)
            final Name name = new Name();
            // NAB: Given name -> CAB: First name
            name.firstname = CursorUtils.getString(cursor, StructuredName.GIVEN_NAME);
            // NAB: Family name -> CAB: Surname
            name.surname = CursorUtils.getString(cursor, StructuredName.FAMILY_NAME);
            // NAB: Prefix -> CAB: Title
            name.title = CursorUtils.getString(cursor, StructuredName.PREFIX);
            // NAB: Middle name -> CAB: Middle name
            name.midname = CursorUtils.getString(cursor, StructuredName.MIDDLE_NAME);
            // NAB: Suffix -> CAB: Suffixes
            name.suffixes = CursorUtils.getString(cursor, StructuredName.SUFFIX);

            // NOTE: Ignoring Phonetics (DATA7, DATA8 and DATA9)!

            // TODO: Need to get middle name and concatenate into value
            final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_NAME, VCardHelper
                    .makeName(name), ContactChange.FLAG_NONE);
            cc.setNabContactId(nabContactId);
            cc.setNabDetailId(nabDetailId);
            ccList.add(cc);
        }
    }

    /**
     * Reads an nickname detail as a {@link ContactChange} from the provided
     * cursor.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readNickname(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        final String value = CursorUtils.getString(cursor, Nickname.NAME);
        if (!TextUtils.isEmpty(value)) {
            final long nabDetailId = CursorUtils.getLong(cursor, Nickname._ID);
            /*
             * TODO: Decide what to do with nickname: Can only have one in VF360
             * but NAB Allows more than one!
             */
            final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_NICKNAME, value,
                    ContactChange.FLAG_NONE);
            cc.setNabContactId(nabContactId);
            cc.setNabDetailId(nabDetailId);
            ccList.add(cc);
        }
    }

    /**
     * Reads an phone detail as a {@link ContactChange} from the provided
     * cursor.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readPhone(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        final String value = CursorUtils.getString(cursor, Phone.NUMBER);
        if (!TextUtils.isEmpty(value)) {
            final long nabDetailId = CursorUtils.getLong(cursor, Phone._ID);
            final int type = CursorUtils.getInt(cursor, Phone.TYPE);

            int flags = mapFromNabPhoneType(type);

            final boolean isPrimary = CursorUtils.getInt(cursor, Phone.IS_PRIMARY) != 0;

            if (isPrimary) {
                flags |= ContactChange.FLAG_PREFERRED;
            }
            // assuming raw value is good enough for us
            final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_PHONE, value, flags);
            cc.setNabContactId(nabContactId);
            cc.setNabDetailId(nabDetailId);
            ccList.add(cc);
        }
    }

    /**
     * Reads an email detail as a {@link ContactChange} from the provided
     * cursor.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readEmail(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        final String value = CursorUtils.getString(cursor, Email.DATA);
        if (!TextUtils.isEmpty(value)) {
            final long nabDetailId = CursorUtils.getLong(cursor, Email._ID);
            final int type = CursorUtils.getInt(cursor, Email.TYPE);
            int flags = mapFromNabEmailType(type);

            final boolean isPrimary = CursorUtils.getInt(cursor, Email.IS_PRIMARY) != 0;

            if (isPrimary) {
                flags |= ContactChange.FLAG_PREFERRED;
            }
            // assuming raw value is good enough for us
            final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_EMAIL, value, flags);
            cc.setNabContactId(nabContactId);
            cc.setNabDetailId(nabDetailId);
            ccList.add(cc);
        }
    }

    /**
     * Reads an address detail as a {@link ContactChange} from the provided
     * cursor. For this type of detail we need to use a VCARD (semicolon
     * separated) value.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readAddress(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        // Using formatted address only to check if there is a valid address to
        // read
        final String formattedAddress = CursorUtils.getString(cursor,
                StructuredPostal.FORMATTED_ADDRESS);
        if (!TextUtils.isEmpty(formattedAddress)) {
            final long nabDetailId = CursorUtils.getLong(cursor, StructuredPostal._ID);
            final int type = CursorUtils.getInt(cursor, StructuredPostal.TYPE);
            int flags = mapFromNabAddressType(type);

            final boolean isPrimary = CursorUtils.getInt(cursor, StructuredPostal.IS_PRIMARY) != 0;

            if (isPrimary) {
                flags |= ContactChange.FLAG_PREFERRED;
            }

            // VCard Helper data type (CAB)
            final PostalAddress address = new PostalAddress();
            // NAB: Street -> CAB: AddressLine1
            address.addressLine1 = CursorUtils.getString(cursor, StructuredPostal.STREET);
            // NAB: PO Box -> CAB: postOfficeBox
            address.postOfficeBox = CursorUtils.getString(cursor, StructuredPostal.POBOX);
            // NAB: Neighborhood -> CAB: AddressLine2
            address.addressLine2 = CursorUtils.getString(cursor, StructuredPostal.NEIGHBORHOOD);
            // NAB: City -> CAB: City
            address.city = CursorUtils.getString(cursor, StructuredPostal.CITY);
            // NAB: Region -> CAB: County
            address.county = CursorUtils.getString(cursor, StructuredPostal.REGION);
            // NAB: Post code -> CAB: Post code
            address.postCode = CursorUtils.getString(cursor, StructuredPostal.POSTCODE);
            // NAB: Country -> CAB: Country
            address.country = CursorUtils.getString(cursor, StructuredPostal.COUNTRY);

            final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_ADDRESS, VCardHelper
                    .makePostalAddress(address), flags);
            cc.setNabContactId(nabContactId);
            cc.setNabDetailId(nabDetailId);
            ccList.add(cc);
        }
    }

    /**
     * Reads an organization detail as a {@link ContactChange} from the provided
     * cursor. For this type of detail we need to use a VCARD (semicolon
     * separated) value. In reality two different changes may be read if a title
     * is also present.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readOrganization(Cursor cursor, List<ContactChange> ccList, long nabContactId) {

        final int type = CursorUtils.getInt(cursor, Organization.TYPE);

        int flags = mapFromNabOrganizationType(type);

        final boolean isPrimary = CursorUtils.getInt(cursor, Organization.IS_PRIMARY) != 0;

        if (isPrimary) {
            flags |= ContactChange.FLAG_PREFERRED;
        }

        final long nabDetailId = CursorUtils.getLong(cursor, Organization._ID);

        if (!mHaveReadOrganization) {
            // VCard Helper data type (CAB)
            final Organisation organization = new Organisation();

            // Company
            organization.name = CursorUtils.getString(cursor, Organization.COMPANY);

            // Department
            final String department = CursorUtils.getString(cursor, Organization.DEPARTMENT);
            if (!TextUtils.isEmpty(department)) {
                organization.unitNames.add(department);
            }

            if ((organization.unitNames != null && organization.unitNames.size() > 0)
                    || !TextUtils.isEmpty(organization.name)) {
                final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_ORG, VCardHelper
                        .makeOrg(organization), flags);
                cc.setNabContactId(nabContactId);
                cc.setNabDetailId(nabDetailId);
                ccList.add(cc);
                mHaveReadOrganization = true;
            }

            // Title
            final String title = CursorUtils.getString(cursor, Organization.TITLE);
            if (!TextUtils.isEmpty(title)) {
                final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_TITLE, title,
                        flags);
                cc.setNabContactId(nabContactId);
                cc.setNabDetailId(nabDetailId);
                ccList.add(cc);
                mHaveReadOrganization = true;
            }
        }
    }

    /**
     * Reads an Website detail as a {@link ContactChange} from the provided
     * cursor.
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    private void readWebsite(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        final String url = CursorUtils.getString(cursor, Website.URL);
        if (!TextUtils.isEmpty(url)) {
            final long nabDetailId = CursorUtils.getLong(cursor, Website._ID);
            final int type = CursorUtils.getInt(cursor, Website.TYPE);
            int flags = mapFromNabWebsiteType(type);

            final boolean isPrimary = CursorUtils.getInt(cursor, Website.IS_PRIMARY) != 0;

            if (isPrimary) {
                flags |= ContactChange.FLAG_PREFERRED;
            }

            final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_URL, url, flags);
            cc.setNabContactId(nabContactId);
            cc.setNabDetailId(nabDetailId);
            ccList.add(cc);
        }
    }

    /**
     * Reads an Birthday detail as a {@link ContactChange} from the provided
     * cursor. Note that since the Android Type is the Generic "Event", it may
     * be the case that nothing is read if this is not actually a Birthday
     * 
     * @param cursor Cursor to read from
     * @param ccList List of Contact Changes to add read detail data
     * @param nabContactId ID of the NAB Contact
     */
    public void readBirthday(Cursor cursor, List<ContactChange> ccList, long nabContactId) {
        final int type = CursorUtils.getInt(cursor, Event.TYPE);
        if (type == Event.TYPE_BIRTHDAY) {
            final String date = CursorUtils.getString(cursor, Event.START_DATE);
            // Ignoring birthdays without year, day and month!
            // FIXME: Remove this check when/if the backend becomes able to
            // handle incomplete birthdays
            if (date != null && date.matches(COMPLETE_DATE_REGEX)) {
                final long nabDetailId = CursorUtils.getLong(cursor, Event._ID);
                final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_DATE, date,
                        ContactChange.FLAG_BIRTHDAY);
                cc.setNabContactId(nabContactId);
                cc.setNabDetailId(nabDetailId);
                ccList.add(cc);
                mHaveReadBirthday = true;
            }
        }
    }

    /**
     * Adds current values to the batch.
     * 
     * @param nabContactId The existing NAB Contact ID if it is an update or an
     *            invalid id if a new contact
     */
    private void addValuesToBatch(long nabContactId) {
        // Add to batch
        if (mValues.size() > 0) {
            final boolean isNewContact = nabContactId == ContactChange.INVALID_ID;
            if (!isNewContact) {
                // Updating a Contact, need to add the ID to the Values
                mValues.put(Data.RAW_CONTACT_ID, nabContactId);
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                    addCallerIsSyncAdapterParameter(Data.CONTENT_URI)).withYieldAllowed(mYield)
                    .withValues(mValues);

            if (isNewContact) {
                // New Contact needs Back Reference
                builder.withValueBackReference(Data.RAW_CONTACT_ID, 0);
            }
            mYield = false;
            mBatch.add(builder.build());
        }
    }

    /**
     * Adds current update values to the batch.
     * 
     * @param nabDetailId The NAB ID of the detail to update
     */
    private void addUpdateValuesToBatch(long nabDetailId) {
        if (mValues.size() > 0) {
            final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, nabDetailId);
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                    addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(mYield).withValues(
                    mValues);
            mYield = false;
            mBatch.add(builder.build());
        }
    }

    /**
     * Adds a delete detail operation to the batch.
     * 
     * @param nabDetailId The NAB Id of the detail to delete
     */
    private void addDeleteDetailToBatch(long nabDetailId) {
        final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, nabDetailId);
        ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(
                addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(mYield);
        mYield = false;
        mBatch.add(builder.build());
    }

    /**
     * Adds the profile action detail to a (assumed) pending new Contact Batch
     * operation.
     * 
     * @param internalContactId The Internal Contact ID used as the Profile ID
     */
    private void addProfileAction(long internalContactId) {
        mValues.clear();
        mValues.put(Data.MIMETYPE, PeopleProfileColumns.CONTENT_ITEM_TYPE);
        mValues.put(PeopleProfileColumns.DATA_PID, internalContactId);
        mValues.put(PeopleProfileColumns.DATA_SUMMARY, mContext
                .getString(R.string.android_contact_profile_summary));
        mValues.put(PeopleProfileColumns.DATA_DETAIL, mContext
                .getString(R.string.android_contact_profile_detail));
        addValuesToBatch(ContactChange.INVALID_ID);
    }

    // PRESENCE TEXT NOT USED
    // /**
    // * Returns the Data id for a sample SyncAdapter contact's profile row, or
    // 0
    // * if the sample SyncAdapter user isn't found.
    // *
    // * @param resolver a content resolver
    // * @param userId the sample SyncAdapter user ID to lookup
    // * @return the profile Data row id, or 0 if not found
    // */
    // private long lookupProfile(long internalContactId) {
    // long profileId = -1;
    // final Cursor c =
    // mCr.query(Data.CONTENT_URI, ProfileQuery.PROJECTION,
    // ProfileQuery.SELECTION, new String[] {String.valueOf(internalContactId)},
    // null);
    // try {
    // if (c != null && c.moveToFirst()) {
    // profileId = c.getLong(ProfileQuery.COLUMN_ID);
    // }
    // } finally {
    // if (c != null) {
    // c.close();
    // }
    // }
    // return profileId;
    // }
    //
    // /**
    // * Constants for a query to find a contact given a sample SyncAdapter user
    // * ID.
    // */
    // private interface ProfileQuery {
    // public final static String[] PROJECTION = new String[] {Data._ID};
    //
    // public final static int COLUMN_ID = 0;
    //
    // public static final String SELECTION =
    // Data.MIMETYPE + "='" + PeopleProfileColumns.CONTENT_ITEM_TYPE
    // + "' AND " + PeopleProfileColumns.DATA_PID + "=?";
    // }
    //  
    // private void addPresence(long internalContactId, String presenceText) {
    // long profileId = lookupProfile(internalContactId);
    // if(profileId > -1) {
    // mValues.clear();
    // mValues.put(StatusUpdates.DATA_ID, profileId);
    // mValues.put(StatusUpdates.STATUS, presenceText);
    // mValues.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM);
    // mValues.put(StatusUpdates.IM_ACCOUNT, LoginPreferences.getUsername());
    // mValues.put(StatusUpdates.STATUS_ICON, R.drawable.pt_launcher_icon);
    // mValues.put(StatusUpdates.STATUS_RES_PACKAGE, mContext.getPackageName());
    //            
    // ContentProviderOperation.Builder builder =
    // ContentProviderOperation.newInsert(
    // addCallerIsSyncAdapterParameter(StatusUpdates.CONTENT_URI)).
    // withYieldAllowed(mYield).withValues(mValues);
    // BatchOperation batch = new BatchOperation();
    // batch.add(builder.build());
    // batch.execute();
    // }
    // }

    /**
     * Put values for a detail from a {@link ContactChange} into pending values.
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putDetail(ContactChange cc) {
        mValues.clear();
        switch (cc.getKey()) {
            case ContactChange.KEY_VCARD_PHONE:
                putPhone(cc);
                break;
            case ContactChange.KEY_VCARD_EMAIL:
                putEmail(cc);
                break;
            case ContactChange.KEY_VCARD_NAME:
                putName(cc);
                break;
            case ContactChange.KEY_VCARD_NICKNAME:
                putNickname(cc);
                break;
            case ContactChange.KEY_VCARD_ADDRESS:
                putAddress(cc);
                break;
            case ContactChange.KEY_VCARD_URL:
                putWebsite(cc);
                break;
            case ContactChange.KEY_VCARD_NOTE:
                putNote(cc);
                break;
            case ContactChange.KEY_VCARD_DATE:
                // Date only means Birthday currently
                putBirthday(cc);
            default:
                break;
        }
    }

    /**
     * Put Name detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putName(ContactChange cc) {
        final Name name = VCardHelper.getName(cc.getValue());

        if (name == null) {
            // Nothing to do
            return;
        }

        mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);

        mValues.put(StructuredName.GIVEN_NAME, name.firstname);

        mValues.put(StructuredName.FAMILY_NAME, name.surname);

        mValues.put(StructuredName.PREFIX, name.title);

        mValues.put(StructuredName.MIDDLE_NAME, name.midname);

        mValues.put(StructuredName.SUFFIX, name.suffixes);
    }

    /**
     * Put Nickname detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putNickname(ContactChange cc) {
        mValues.put(Nickname.NAME, cc.getValue());
        mValues.put(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
    }

    /**
     * Put Phone detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putPhone(ContactChange cc) {
        mValues.put(Phone.NUMBER, cc.getValue());
        final int flags = cc.getFlags();
        mValues.put(Phone.TYPE, mapToNabPhoneType(flags));
        mValues.put(Phone.IS_PRIMARY, flags & ContactChange.FLAG_PREFERRED);
        mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
    }

    /**
     * Put Email detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putEmail(ContactChange cc) {
        mValues.put(Email.DATA, cc.getValue());
        final int flags = cc.getFlags();
        mValues.put(Email.TYPE, mapToNabEmailType(flags));
        mValues.put(Email.IS_PRIMARY, flags & ContactChange.FLAG_PREFERRED);
        mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
    }

    /**
     * Put Address detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putAddress(ContactChange cc) {
        final PostalAddress address = VCardHelper.getPostalAddress(cc.getValue());

        if (address == null) {
            // Nothing to do
            return;
        }

        mValues.put(StructuredPostal.STREET, address.addressLine1);

        mValues.put(StructuredPostal.POBOX, address.postOfficeBox);

        mValues.put(StructuredPostal.NEIGHBORHOOD, address.addressLine2);

        mValues.put(StructuredPostal.CITY, address.city);

        mValues.put(StructuredPostal.REGION, address.county);

        mValues.put(StructuredPostal.POSTCODE, address.postCode);

        mValues.put(StructuredPostal.COUNTRY, address.country);

        final int flags = cc.getFlags();
        mValues.put(StructuredPostal.TYPE, mapToNabAddressType(flags));

        mValues.put(StructuredPostal.IS_PRIMARY, flags & ContactChange.FLAG_PREFERRED);
        mValues.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
    }

    /**
     * Put Website detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putWebsite(ContactChange cc) {
        mValues.put(Website.URL, cc.getValue());
        final int flags = cc.getFlags();
        mValues.put(Website.TYPE, mapToNabWebsiteType(flags));
        mValues.put(Website.IS_PRIMARY, flags & ContactChange.FLAG_PREFERRED);
        mValues.put(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE);
    }

    /**
     * Put Note detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putNote(ContactChange cc) {
        mValues.put(Note.NOTE, cc.getValue());
        mValues.put(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE);
    }

    /**
     * Put Birthday detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putBirthday(ContactChange cc) {
        if ((cc.getFlags() & ContactChange.FLAG_BIRTHDAY) == ContactChange.FLAG_BIRTHDAY) {
            mValues.put(Event.START_DATE, cc.getValue());
            mValues.put(Event.TYPE, Event.TYPE_BIRTHDAY);
            mValues.put(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
        }
    }

    // PHOTO NOT USED
    // /**
    // * Do a GET request and retrieve up to maxBytes bytes
    // *
    // * @param url
    // * @param maxBytes
    // * @return
    // * @throws IOException
    // */
    // public static byte[] doGetAndReturnBytes(URL url, int maxBytes) throws
    // IOException {
    // HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // conn.setRequestMethod("GET");
    // InputStream istr = null;
    // try {
    // int rc = conn.getResponseCode();
    // if (rc != 200) {
    // throw new IOException("code " + rc + " '" + conn.getResponseMessage() +
    // "'");
    // }
    // istr = new BufferedInputStream(conn.getInputStream(), 512);
    // ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // copy(istr, baos, maxBytes);
    // return baos.toByteArray();
    // } finally {
    // if (istr != null) {
    // istr.close();
    // }
    // }
    // }
    //    
    // /**
    // * Copy maxBytes from an input stream to an output stream.
    // * @param in
    // * @param out
    // * @param maxBytes
    // * @return
    // * @throws IOException
    // */
    // private static int copy(InputStream in, OutputStream out, int maxBytes)
    // throws IOException {
    // byte[] buf = new byte[512];
    // int bytesRead = 1;
    // int totalBytes = 0;
    // while (bytesRead > 0) {
    // bytesRead = in.read(buf, 0, Math.min(512, maxBytes - totalBytes));
    // if (bytesRead > 0) {
    // out.write(buf, 0, bytesRead);
    // totalBytes += bytesRead;
    // }
    // }
    // return totalBytes;
    // }
    //  
    // /**
    // * Put Photo detail into the values
    // * @param cc {@link ContactChange} to read values from
    // */
    // private void putPhoto(ContactChange cc) {
    // try {
    // // File file = new File(cc.getValue());
    // // InputStream is = new FileInputStream(file);
    // // byte[] bytes = new byte[(int) file.length()];
    // // is.read(bytes);
    // // is.close();
    // final URL url = new URL(cc.getValue());
    // byte[] bytes = doGetAndReturnBytes(url, 1024 * 100);
    // mValues.put(Photo.PHOTO, bytes);
    // mValues.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
    // } catch(Exception ex) {
    // LogUtils.logE("Unable to put Photo detail because of exception:"+ex);
    // }
    // }

    /**
     * Put Organization detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     */
    private void putOrganization(ContactChange[] ccList) {
        mValues.clear();

        int flags = ContactChange.FLAG_NONE;
        if (mMarkedOrganizationIndex > -1) {
            final ContactChange cc = ccList[mMarkedOrganizationIndex];
            flags |= cc.getFlags();
            final Organisation organization = VCardHelper.getOrg(cc.getValue());
            if (organization != null) {
                mValues.put(Organization.COMPANY, organization.name);
                if (organization.unitNames.size() > 0) {
                    // Only considering one unit name (department) as that's all
                    // we support
                    mValues.put(Organization.DEPARTMENT, organization.unitNames.get(0));
                } else {
                    mValues.putNull(Organization.DEPARTMENT);
                }
            }
        }

        if (mMarkedTitleIndex > -1) {
            final ContactChange cc = ccList[mMarkedTitleIndex];
            flags |= cc.getFlags();
            // No need to check for empty values as there is only one
            mValues.put(Organization.TITLE, cc.getValue());
        }

        if (mValues.size() > 0) {
            mValues.put(Organization.LABEL, (String)null);
            mValues.put(Organization.TYPE, mapToNabOrganizationType(flags));
            mValues.put(Organization.IS_PRIMARY, flags & ContactChange.FLAG_PREFERRED);
            mValues.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
        }
    }

    /**
     * Updates the Organization detail in the context of a Contact Update
     * operation. The end of result of this is that the Organization may be
     * inserted, updated or deleted depending on the update data. For example,
     * if the title is deleted but there is also a company name then the
     * Organization is just updated. However, if there was no company name then
     * the detail should be deleted altogether.
     * 
     * @param ccList {@link ContactChange} list where Organization and Title may
     *            be found
     * @param nabContactId The NAB ID of the Contact
     */
    private void updateOrganization(ContactChange[] ccList, long nabContactId) {
        if (mMarkedOrganizationIndex < 0 && mMarkedTitleIndex < 0) {
            // no organization or title to update - do nothing
            return;
        }

        // First we check if there is an existing Organization detail in NAB
        final Uri uri = Uri.withAppendedPath(ContentUris.withAppendedId(RawContacts.CONTENT_URI,
                nabContactId), RawContacts.Data.CONTENT_DIRECTORY);

        Cursor cursor = mCr.query(uri, null, ORGANIZATION_DETAIL_WHERE_CLAUSE, null,
                RawContacts.Data._ID);
        String company = null;
        String department = null;
        String title = null;
        int flags = ContactChange.FLAG_NONE;
        try {
            if (cursor != null && cursor.moveToNext()) {
                // Found an organization detail
                company = CursorUtils.getString(cursor, Organization.COMPANY);
                department = CursorUtils.getString(cursor, Organization.DEPARTMENT);
                title = CursorUtils.getString(cursor, Organization.TITLE);
                flags = mapFromNabOrganizationType(CursorUtils.getInt(cursor, Organization.TYPE));
                final boolean isPrimary = CursorUtils.getInt(cursor, Organization.IS_PRIMARY) > 0;
                if (isPrimary) {
                    flags |= ContactChange.FLAG_PREFERRED;
                }
                mExistingOrganizationId = CursorUtils.getLong(cursor, Organization._ID);
            }
        } finally {
            CursorUtils.closeCursor(cursor);
            cursor = null; // make it a candidate for the GC
        }

        if (mMarkedOrganizationIndex >= 0) {
            // Have an Organization (Company + Department) to update
            final ContactChange cc = ccList[mMarkedOrganizationIndex];
            if (cc.getType() != ContactChange.TYPE_DELETE_DETAIL) {
                final String value = cc.getValue();
                if (value != null) {
                    final Organisation organization = VCardHelper.getOrg(value);
                    company = organization.name;
                    if (organization.unitNames.size() > 0) {
                        department = organization.unitNames.get(0);
                    }
                }

                flags = cc.getFlags();
            } else { // Delete case
                company = null;
                department = null;
            }
        }

        if (mMarkedTitleIndex >= 0) {
            // Have a Title to update
            final ContactChange cc = ccList[mMarkedTitleIndex];
            title = cc.getValue();
            if (cc.getType() != ContactChange.TYPE_UPDATE_DETAIL) {
                flags = cc.getFlags();
            }
        }

        if (company != null || department != null || title != null) {
            /*
             * If any of the above are present we assume a insert or update is
             * needed.
             */
            mValues.clear();
            mValues.put(Organization.LABEL, (String)null);
            mValues.put(Organization.COMPANY, company);
            mValues.put(Organization.DEPARTMENT, department);
            mValues.put(Organization.TITLE, title);
            mValues.put(Organization.TYPE, mapToNabOrganizationType(flags));
            mValues.put(Organization.IS_PRIMARY, flags & ContactChange.FLAG_PREFERRED);
            mValues.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
            if (mExistingOrganizationId != ContactChange.INVALID_ID) {
                // update is needed
                addUpdateValuesToBatch(mExistingOrganizationId);
            } else {
                // insert is needed
                addValuesToBatch(nabContactId); // not a new contact
            }
        } else if (mExistingOrganizationId != ContactChange.INVALID_ID) {
            /*
             * Had an Organization but now all values are null, delete is in
             * order.
             */
            addDeleteDetailToBatch(mExistingOrganizationId);
        }
    }

    /**
     * Executes a pending Batch Operation related to adding a new Contact.
     * 
     * @param ccList The original {@link ContactChange} for the new Contact
     * @return {@link ContactChange} array containing new IDs (may contain some
     *         null elements)
     */
    private ContactChange[] executeNewContactBatch(ContactChange[] ccList) {
        if (mBatch.size() == 0) {
            // Nothing to execute
            return null;
        }
        final ContentProviderResult[] results = mBatch.execute();

        if (results == null || results.length == 0) {
            LogUtils.logE("NativeContactsApi2.executeNewContactBatch()"
                    + "Batch execution result is null or empty!");
            return null;
        }
        // -1 because we skip the Profile detail
        final int resultsSize = results.length - 1;

        if (results[0].uri == null) {
            // Contact was not created
            LogUtils.logE("NativeContactsApi2.executeNewContactBatch()"
                    + "NAB Contact ID not found for created contact");
            return null;
        }

        final long nabContactId = ContentUris.parseId(results[0].uri);

        final ContactChange[] idChangeList = new ContactChange[ccList.length + 1];
        // Update NAB Contact ID CC
        idChangeList[0] = ContactChange.createIdsChange(ccList[0],
                ContactChange.TYPE_UPDATE_NAB_CONTACT_ID);
        idChangeList[0].setNabContactId(nabContactId);

        // Start after contact id in the results index
        int resultsIndex = 1, ccListIndex = 0;
        final boolean haveOrganization = mMarkedOrganizationIndex != -1 || mMarkedTitleIndex != -1;
        while (resultsIndex < resultsSize) {
            if (ccListIndex == mMarkedOrganizationIndex || ccListIndex == mMarkedTitleIndex) {
                ccListIndex++;
                continue;
            }

            if (results[resultsIndex].uri == null) {
                throw new RuntimeException("NativeContactsApi2.executeNewContactBatch()"
                        + "Unexpected null URI for NAB Contact:" + nabContactId);
            }

            if (resultsIndex == resultsSize - 1 && haveOrganization) {
                // for readability we leave Organization/Title for outside the
                // loop
                break;
            }
            final ContactChange cc = ccList[ccListIndex];
            // Check if the key is one that is supported in the 2.X NAB
            if (sFromKeyToNabContentTypeArray.indexOfKey(cc.getKey()) >= 0) {
                final long nabDetailId = ContentUris.parseId(results[resultsIndex].uri);
                final ContactChange idChange = ContactChange.createIdsChange(ccList[ccListIndex],
                        ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChange.setNabContactId(nabContactId);
                idChange.setNabDetailId(nabDetailId);
                idChangeList[ccListIndex + 1] = idChange;
                resultsIndex++;
            }
            ccListIndex++;
        }

        if (haveOrganization) {
            final long nabDetailId = ContentUris.parseId(results[resultsIndex].uri);
            if (mMarkedOrganizationIndex > -1) {
                final ContactChange idChange = ContactChange.createIdsChange(
                        ccList[mMarkedOrganizationIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChange.setNabContactId(nabContactId);
                idChange.setNabDetailId(nabDetailId);
                idChangeList[mMarkedOrganizationIndex + 1] = idChange;
            }

            if (mMarkedTitleIndex > -1) {
                final ContactChange idChange = ContactChange.createIdsChange(
                        ccList[mMarkedTitleIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChange.setNabContactId(nabContactId);
                idChange.setNabDetailId(nabDetailId);
                idChangeList[mMarkedTitleIndex + 1] = idChange;
            }
        }

        return idChangeList;
    }

    /**
     * Executes a pending Batch Operation related to updating a Contact.
     * 
     * @param ccList The original {@link ContactChange} for the Contact update
     * @return {@link ContactChange} array containing new IDs (may contain some
     *         null elements)
     */
    private ContactChange[] executeUpdateContactBatch(ContactChange[] ccList) {
        if (mBatch.size() == 0) {
            // Nothing to execute
            return null;
        }
        final ContentProviderResult[] results = mBatch.execute();
        final int resultsSize = results.length;
        if (results == null || resultsSize == 0) {
            // Assuming this can happen in case of no added details
            return null;
        }

        // Start after contact id in the results index
        int resultsIndex = 0, ccListIndex = 0;
        final boolean haveOrganization = mMarkedOrganizationIndex != -1 || mMarkedTitleIndex != -1;
        final ContactChange[] idChangeList = new ContactChange[ccList.length];
        while (resultsIndex < resultsSize) {
            if (ccListIndex == mMarkedOrganizationIndex || ccListIndex == mMarkedTitleIndex) {
                ccListIndex++;
                continue;
            }

            if (results[resultsIndex].uri == null) {
                // Assuming detail was updated or deleted (not added)
                resultsIndex++;
                continue;
            }

            if (resultsIndex == resultsSize - 1 && haveOrganization) {
                // for readability we leave Organization/Title for outside the
                // loop
                break;
            }

            final ContactChange cc = ccList[ccListIndex];
            // Check if the key is one that is supported in the 2.X NAB
            if (sFromKeyToNabContentTypeArray.indexOfKey(cc.getKey()) >= 0
                    && cc.getType() == ContactChange.TYPE_ADD_DETAIL) {
                final long nabDetailId = ContentUris.parseId(results[resultsIndex].uri);
                final ContactChange idChange = ContactChange.createIdsChange(ccList[ccListIndex],
                        ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChange.setNabDetailId(nabDetailId);
                idChangeList[ccListIndex] = idChange;
                resultsIndex++;
            }
            ccListIndex++;
        }

        if (haveOrganization) {
            long nabDetailId = ContactChange.INVALID_ID;
            if (mExistingOrganizationId != nabDetailId) {
                nabDetailId = mExistingOrganizationId;
            } else if (results[resultsIndex].uri != null) {
                nabDetailId = ContentUris.parseId(results[resultsIndex].uri);
            } else {
                throw new RuntimeException("NativeContactsApi2.executeUpdateContactBatch()"
                        + "Unexpected null Organization URI for NAB Contact:"
                        + ccList[0].getNabContactId());
            }

            if (mMarkedOrganizationIndex > -1
                    && ccList[mMarkedOrganizationIndex].getType() == ContactChange.TYPE_ADD_DETAIL) {
                final ContactChange idChange = ContactChange.createIdsChange(
                        ccList[mMarkedOrganizationIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChange.setNabDetailId(nabDetailId);
                idChangeList[mMarkedOrganizationIndex] = idChange;
            }

            if (mMarkedTitleIndex > -1
                    && ccList[mMarkedTitleIndex].getType() == ContactChange.TYPE_ADD_DETAIL) {
                final ContactChange idChange = ContactChange.createIdsChange(
                        ccList[mMarkedTitleIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChange.setNabDetailId(nabDetailId);
                idChangeList[mMarkedTitleIndex] = idChange;
            }
        }

        return idChangeList;
    }

    /**
     * Static utility method that adds the Sync Adapter flag to the provided URI
     * 
     * @param uri URI to add the flag to
     * @return URI with the flag
     */
    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
    }

    /**
     * Utility method used to create an entry in the ContactsContract.Settings
     * database table for the 360 People Account. This method only exists to
     * compensate for HTC devices such as Legend or Desire that don't create the
     * entry automatically like the Nexus One.
     * 
     * @param username The username of the 360 People Account.
     */
    private void createSettingsEntryForAccount(String username) {
        mValues.clear();
        mValues.put(Settings.ACCOUNT_NAME, username);
        mValues.put(Settings.ACCOUNT_TYPE, PEOPLE_ACCOUNT_TYPE_STRING);
        mValues.put(Settings.UNGROUPED_VISIBLE, true);
        mValues.put(Settings.SHOULD_SYNC, false); // TODO Unsupported for now
        mCr.insert(Settings.CONTENT_URI, mValues);
    }

    /**
     * Maps a phone type from the native value to the {@link ContactChange}
     * flags.
     * 
     * @param nabType Given native phone number type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabPhoneType(int nabType) {
        switch (nabType) {
            case Phone.TYPE_HOME:
                return ContactChange.FLAG_HOME;
            case Phone.TYPE_MOBILE:
                return ContactChange.FLAG_CELL;
            case Phone.TYPE_WORK:
                return ContactChange.FLAG_WORK;
            case Phone.TYPE_WORK_MOBILE:
                return ContactChange.FLAGS_WORK_CELL;
            case Phone.TYPE_FAX_WORK:
                return ContactChange.FLAGS_WORK_FAX;
            case Phone.TYPE_FAX_HOME:
                return ContactChange.FLAGS_HOME_FAX;
            case Phone.TYPE_OTHER_FAX:
                return ContactChange.FLAG_FAX;
        }

        return ContactChange.FLAG_NONE;
    }

    /**
     * Maps {@link ContactChange} flags into the native phone type.
     * 
     * @param flags {@link ContactChange} flags
     * @return Native phone type
     */
    private static int mapToNabPhoneType(int flags) {
        if ((flags & ContactChange.FLAGS_WORK_CELL) == ContactChange.FLAGS_WORK_CELL) {
            return Phone.TYPE_WORK_MOBILE;
        }

        if ((flags & ContactChange.FLAGS_HOME_FAX) == ContactChange.FLAGS_HOME_FAX) {
            return Phone.TYPE_FAX_HOME;
        }

        if ((flags & ContactChange.FLAGS_WORK_FAX) == ContactChange.FLAGS_WORK_FAX) {
            return Phone.TYPE_FAX_WORK;
        }

        if ((flags & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            return Phone.TYPE_HOME;
        }

        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return Phone.TYPE_WORK;
        }

        if ((flags & ContactChange.FLAG_CELL) == ContactChange.FLAG_CELL) {
            return Phone.TYPE_MOBILE;
        }

        if ((flags & ContactChange.FLAG_FAX) == ContactChange.FLAG_FAX) {
            return Phone.TYPE_OTHER_FAX;
        }

        return Phone.TYPE_OTHER;
    }

    /**
     * Maps a email type from the native value into the {@link ContactChange}
     * flags
     * 
     * @param nabType Native email type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabEmailType(int nabType) {
        switch (nabType) {
            case Email.TYPE_HOME:
                return ContactChange.FLAG_HOME;
            case Email.TYPE_MOBILE:
                return ContactChange.FLAG_CELL;
            case Email.TYPE_WORK:
                return ContactChange.FLAG_WORK;
        }

        return ContactChange.FLAG_NONE;
    }

    /**
     * Maps {@link ContactChange} flags into the native email type.
     * 
     * @param flags {@link ContactChange} flags
     * @return Native email type
     */
    private static int mapToNabEmailType(int flags) {
        if ((flags & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            return Email.TYPE_HOME;
        }

        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return Email.TYPE_WORK;
        }

        return Email.TYPE_OTHER;
    }

    /**
     * Maps a address type from the native value into the {@link ContactChange}
     * flags
     * 
     * @param nabType Native email type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabAddressType(int nabType) {
        switch (nabType) {
            case StructuredPostal.TYPE_HOME:
                return ContactChange.FLAG_HOME;
            case StructuredPostal.TYPE_WORK:
                return ContactChange.FLAG_WORK;
        }

        return ContactChange.FLAG_NONE;
    }

    /**
     * Maps {@link ContactChange} flags into the native address type.
     * 
     * @param flags {@link ContactChange} flags
     * @return Native address type
     */
    private static int mapToNabAddressType(int flags) {
        if ((flags & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            return StructuredPostal.TYPE_HOME;
        }

        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return StructuredPostal.TYPE_WORK;
        }

        return StructuredPostal.TYPE_OTHER;
    }

    /**
     * Maps a organization type from the native value into the
     * {@link ContactChange} flags
     * 
     * @param nabType Given native organization type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabOrganizationType(int nabType) {
        if (nabType == Organization.TYPE_WORK) {
            return ContactChange.FLAG_WORK;
        }

        return ContactChange.FLAG_NONE;
    }

    /**
     * Maps {@link ContactChange} flags into the native organization type.
     * 
     * @param flags {@link ContactChange} flags
     * @return Native Organization type
     */
    private static int mapToNabOrganizationType(int flags) {
        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return Organization.TYPE_WORK;
        }

        return Organization.TYPE_OTHER;
    }

    /**
     * Maps a Website type from the native value into the {@link ContactChange}
     * flags
     * 
     * @param nabType Native email type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabWebsiteType(int nabType) {
        switch (nabType) {
            case Website.TYPE_HOME:
                return ContactChange.FLAG_HOME;
            case Website.TYPE_WORK:
                return ContactChange.FLAG_WORK;
        }

        return ContactChange.FLAG_NONE;
    }

    /**
     * Maps {@link ContactChange} flags into the native Website type.
     * 
     * @param flags {@link ContactChange} flags
     * @return Native Website type
     */
    private static int mapToNabWebsiteType(int flags) {
        if ((flags & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            return Website.TYPE_HOME;
        }

        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return Website.TYPE_WORK;
        }

        return Website.TYPE_OTHER;
    }
}
