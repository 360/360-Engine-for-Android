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
import java.util.List;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.Organizations;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.text.TextUtils;
import android.util.SparseIntArray;

import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.VCardHelper.Name;
import com.vodafone360.people.datatypes.VCardHelper.Organisation;
import com.vodafone360.people.datatypes.VCardHelper.PostalAddress;
import com.vodafone360.people.utils.CursorUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * The implementation of the NativeContactsApi for the Android 1.X platform.
 */
@SuppressWarnings("deprecation")
// Needed to hide warnings because we are using deprecated 1.X native apis
// APIs
public class NativeContactsApi1 extends NativeContactsApi {

    /**
     * Convenience Contact ID Projection.
     */
    private final static String[] CONTACTID_PROJECTION = {
        People._ID
    };

    /**
     * Convenience Projection for the NATE and NOTES from the People table
     */
    private final static String[] PEOPLE_PROJECTION = {
            People.NAME, People.NOTES
    };

    /**
     * Contact methods table kind value for email.
     */
    private static final int CONTACT_METHODS_KIND_EMAIL = 1;

    /**
     * Contact methods table kind value for postal address.
     */
    private static final int CONTACT_METHODS_KIND_ADDRESS = 2;

    /**
     * Mapping of supported {@link ContactChange} Keys.
     */
    private static final SparseIntArray sSupportedKeys;

    static {
        sSupportedKeys = new SparseIntArray(7);
        sSupportedKeys.append(ContactChange.KEY_VCARD_NAME, 0);
        sSupportedKeys.append(ContactChange.KEY_VCARD_PHONE, 0);
        sSupportedKeys.append(ContactChange.KEY_VCARD_EMAIL, 0);
        sSupportedKeys.append(ContactChange.KEY_VCARD_ADDRESS, 0);
        sSupportedKeys.append(ContactChange.KEY_VCARD_ORG, 0);
        sSupportedKeys.append(ContactChange.KEY_VCARD_TITLE, 0);
        sSupportedKeys.append(ContactChange.KEY_VCARD_NOTE, 0);
    }

    /**
     * Values used for writing to NAB
     */
    private final ContentValues mValues = new ContentValues();

    /**
     * The registered ContactsObserver.
     * 
     * @see #registerObserver(ContactsObserver)
     */
    private ContactsObserver mContactsObserver;

    /**
     * The observer for the native contacts.
     */
    private ContentObserver mNativeObserver;

    /**
     * @see NativeContactsApi#initialize()
     */
    @Override
    protected void initialize() {
        // Nothing to do
    }

    /**
     * @see NativeContactsApi#registerObserver(ContactsObserver)
     */
    @Override
    public void registerObserver(ContactsObserver observer) {

        if (mContactsObserver != null) {
            throw new RuntimeException(
                    "NativeContactsApi1.registerObserver(): current implementation only supports one observer"
                            + " at a time... Please unregister first.");
        }

        mContactsObserver = observer;

        mNativeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mContactsObserver.onChange();
            }
        };

        mCr.registerContentObserver(Contacts.CONTENT_URI, true, mNativeObserver);
    }

    /**
     * @see NativeContactsApi#unregisterObserver()
     */
    @Override
    public void unregisterObserver() {
        if (mContactsObserver != null) {
            mContactsObserver = null;
            mCr.unregisterContentObserver(mNativeObserver);
            mNativeObserver = null;
        }
    }

    /**
     * @see NativeContactsApi#getAccounts()
     */
    @Override
    public Account[] getAccounts() {
        // No accounts on 1.X NAB
        return null;
    }

    /**
     * @see NativeContactsApi#getAccountsByType(String)
     */
    @Override
    public Account[] getAccountsByType(int type) {
        // No accounts on 1.X NAB
        return null;
    }

    /**
     * @see NativeContactsApi#addPeopleAccount(String)
     */
    @Override
    public boolean addPeopleAccount(String username) {
        // No People Accounts on 1.X NAB
        return false;
    }

    /**
     * @see NativeContactsApi#isPeopleAccountCreated()
     */
    @Override
    public boolean isPeopleAccountCreated() {
        // No People Accounts on 1.X NAB
        return false;
    }

    /**
     * @see NativeContactsApi#removePeopleAccount()
     */
    @Override
    public void removePeopleAccount() {
        // No People Accounts on 1.X NAB
    }

    /**
     * @see NativeContactsApi#getContactIds(Account)
     */
    @Override
    public long[] getContactIds(Account account) {
        if (account != null) {
            // Accounts not supported in 1.X, just return null
            LogUtils.logE("NativeContactsApi1.getContactIds() Unexpected non-null Account(\""
                    + account.toString() + "\"");
            return null;
        }

        long[] ids = null;
        final Cursor cursor = mCr.query(People.CONTENT_URI, CONTACTID_PROJECTION, null, null,
                People._ID);
        try {
            final int idCount = cursor.getCount();
            if (idCount > 0) {
                ids = new long[idCount];
                int index = 0;
                while (cursor.moveToNext()) {
                    ids[index] = cursor.getLong(0);
                    index++;
                }
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }
        return ids;
    }

    /**
     * @see NativeContactsApi#getContact(long)
     */
    @Override
    public ContactChange[] getContact(long nabContactId) {
        final List<ContactChange> ccList = new ArrayList<ContactChange>();
        final Cursor cursor = mCr.query(ContentUris
                .withAppendedId(People.CONTENT_URI, nabContactId), PEOPLE_PROJECTION, null, null,
                null);
        try {
            if (cursor.moveToNext()) {
                final String displayName = CursorUtils.getString(cursor, People.NAME);
                if (!TextUtils.isEmpty(displayName)) {
                    // TODO: Remove if really not necessary
                    // final Name name = parseRawName(displayName);
                    final Name name = new Name();
                    name.firstname = displayName;
                    final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_NAME,
                            VCardHelper.makeName(name), ContactChange.FLAG_NONE);
                    cc.setNabContactId(nabContactId);
                    ccList.add(cc);
                }

                // Note
                final String note = CursorUtils.getString(cursor, People.NOTES);
                if (!TextUtils.isEmpty(note)) {
                    final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_NOTE, note,
                            ContactChange.FLAG_NONE);
                    cc.setNabContactId(nabContactId);
                    ccList.add(cc);
                }
                // Remaining contact details
                readContactPhoneNumbers(ccList, nabContactId);
                readContactMethods(ccList, nabContactId);
                readContactOrganizations(ccList, nabContactId);
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }
        return ccList.toArray(new ContactChange[ccList.size()]);
    }

    /**
     * @see NativeContactsApi#addContact(Account, ContactChange[])
     */
    @Override
    public ContactChange[] addContact(Account account, ContactChange[] ccList) {
        if (account != null) {
            // Accounts not supported in 1.X, just return null
            return null;
        }

        /*
         * These details need special treatment: Name and Note (written to
         * people table); Organization/Title (written as one detail in NAB).
         */
        mMarkedOrganizationIndex = mMarkedTitleIndex = -1;

        preprocessContactToAdd(ccList);

        Uri uri = mCr.insert(People.CONTENT_URI, mValues);

        if (uri != null) {
            // Change List to hold resulting changes for the add contact
            // (including +1 for contact id)
            final ContactChange[] idChangeList = new ContactChange[ccList.length + 1];
            final long nabContactId = ContentUris.parseId(uri);
            idChangeList[0] = ContactChange.createIdsChange(ccList[0],
                    ContactChange.TYPE_UPDATE_NAB_CONTACT_ID);
            idChangeList[0].setNabContactId(nabContactId);
            writeDetails(ccList, idChangeList, nabContactId);

            processOrganization(ccList, idChangeList, nabContactId);

            return idChangeList;
        }

        return null;
    }

    /**
     * @see NativeContactsApi#updateContact(ContactChange[])
     */
    @Override
    public ContactChange[] updateContact(ContactChange[] ccList) {
        if (ccList == null || ccList.length == 0) {
            LogUtils.logW("NativeContactsApi1.updateContact() nothing to update - empty ccList!");
            return null;
        }

        mMarkedOrganizationIndex = mMarkedTitleIndex = -1;

        final long nabContactId = ccList[0].getNabContactId();

        final int ccListSize = ccList.length;

        final ContactChange[] idCcList = new ContactChange[ccListSize];

        for (int i = 0; i < ccListSize; i++) {
            final ContactChange cc = ccList[i];
            if (cc.getKey() == ContactChange.KEY_VCARD_ORG) {
                if (mMarkedOrganizationIndex < 0) {
                    mMarkedOrganizationIndex = i;
                }
                continue;
            }

            if (cc.getKey() == ContactChange.KEY_VCARD_TITLE) {
                if (mMarkedTitleIndex < 0) {
                    mMarkedTitleIndex = i;
                }
                continue;
            }

            switch (cc.getType()) {
                case ContactChange.TYPE_ADD_DETAIL:
                    final int key = cc.getKey();
                    if (key != ContactChange.KEY_VCARD_NAME && key != ContactChange.KEY_VCARD_NOTE) {
                        final long nabDetailId = insertDetail(cc, nabContactId);
                        if (nabDetailId != ContactChange.INVALID_ID) {
                            idCcList[i] = ContactChange.createIdsChange(cc,
                                    ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                            idCcList[i].setNabDetailId(nabDetailId);
                        }
                    } else {
                        // Name and Note are not inserted but updated because
                        // they are always there!
                        updateDetail(cc);
                    }

                    break;
                case ContactChange.TYPE_UPDATE_DETAIL:
                    updateDetail(cc);
                    break;
                case ContactChange.TYPE_DELETE_DETAIL:
                    deleteDetail(cc.getKey(), nabContactId, cc.getNabDetailId());
                    break;
                default:
                    break;
            }
        }

        final long orgDetailId = updateOrganization(ccList, nabContactId);

        if (orgDetailId != ContactChange.INVALID_ID) {
            if (mMarkedOrganizationIndex >= 0
                    && ccList[mMarkedOrganizationIndex].getType() == ContactChange.TYPE_ADD_DETAIL) {
                idCcList[mMarkedOrganizationIndex] = ContactChange.createIdsChange(
                        ccList[mMarkedOrganizationIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idCcList[mMarkedOrganizationIndex].setNabDetailId(orgDetailId);
            }

            if (mMarkedTitleIndex >= 0
                    && ccList[mMarkedTitleIndex].getType() == ContactChange.TYPE_ADD_DETAIL) {
                idCcList[mMarkedTitleIndex] = ContactChange.createIdsChange(
                        ccList[mMarkedTitleIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idCcList[mMarkedTitleIndex].setNabDetailId(orgDetailId);
            }
        }

        return idCcList;
    }

    /**
     * @see NativeContactsApi#removeContact(long)
     */
    @Override
    public void removeContact(long nabContactId) {
        Uri rawContactUri = ContentUris.withAppendedId(People.CONTENT_URI, nabContactId);
        mCr.delete(rawContactUri, null, null);
    }

    /**
     * @see NativeContactsApi#isKeySupported(int)
     */
    @Override
    public boolean isKeySupported(int key) {
        return sSupportedKeys.indexOfKey(key) >= 0;
    }

    /**
     * Reads Phone Number details from a Contact into the provided
     * {@link ContactChange} List
     * 
     * @param ccList {@link ContactChange} list to add details into
     * @param nabContactId ID of the NAB Contact
     */
    private void readContactPhoneNumbers(List<ContactChange> ccList, long nabContactId) {
        final Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, nabContactId);
        final Uri phoneUri = Uri.withAppendedPath(contactUri, People.Phones.CONTENT_DIRECTORY);

        final Cursor cursor = mCr.query(phoneUri, null, null, null, null);

        if (cursor == null) {
            return;
        }

        try {
            while (cursor.moveToNext()) {
                final ContactChange cc = readContactPhoneNumber(cursor);

                if (cc != null) {
                    cc.setNabContactId(nabContactId);
                    ccList.add(cc);
                }
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }
    }

    /**
     * Reads one Phone Number Detail from the supplied cursor into a
     * {@link ContactChange}
     * 
     * @param cursor Cursor to read from
     * @return Read Phone Number Detail or null
     */
    private ContactChange readContactPhoneNumber(Cursor cursor) {
        ContactChange cc = null;

        final String phoneNumber = CursorUtils.getString(cursor, Phones.NUMBER);
        if (!TextUtils.isEmpty(phoneNumber)) {
            final long nabDetailId = CursorUtils.getLong(cursor, Phones._ID);
            final boolean isPrimary = CursorUtils.getInt(cursor, Phones.ISPRIMARY) != 0;
            final int rawType = CursorUtils.getInt(cursor, Phones.TYPE);
            int flags = mapFromNabPhoneType(rawType);
            if (isPrimary) {
                flags |= ContactChange.FLAG_PREFERRED;
            }
            cc = new ContactChange(ContactChange.KEY_VCARD_PHONE, phoneNumber, flags);
            cc.setNabDetailId(nabDetailId);
        }

        return cc;
    }

    /**
     * Reads Contact Method details from a Contact into the provided
     * {@link ContactChange} List
     * 
     * @param ccList {@link ContactChange} list to add details into
     * @param nabContactId ID of the NAB Contact
     */
    private void readContactMethods(List<ContactChange> ccList, long nabContactId) {
        Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, nabContactId);
        Uri contactMethodUri = Uri.withAppendedPath(contactUri,
                People.ContactMethods.CONTENT_DIRECTORY);
        final Cursor cursor = mCr.query(contactMethodUri, null, null, null, null);

        if (cursor == null) {
            return;
        }

        try {
            while (cursor.moveToNext()) {
                final ContactChange cc = readContactMethod(cursor);

                if (cc != null) {
                    cc.setNabContactId(nabContactId);
                    ccList.add(cc);
                }
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }
    }

    /**
     * Reads one Contact Method Detail from the supplied cursor into a
     * {@link ContactChange}
     * 
     * @param cursor Cursor to read from
     * @return Read Contact Method Detail or null
     */
    private ContactChange readContactMethod(Cursor cursor) {
        ContactChange cc = null;

        final String value = CursorUtils.getString(cursor, ContactMethods.DATA);

        if (!TextUtils.isEmpty(value)) {
            final long nabDetailId = CursorUtils.getLong(cursor, ContactMethods._ID);
            final boolean isPrimary = CursorUtils.getInt(cursor, ContactMethods.ISPRIMARY) != 0;
            final int kind = CursorUtils.getInt(cursor, ContactMethods.KIND);
            final int type = CursorUtils.getInt(cursor, ContactMethods.TYPE);
            int flags = mapFromNabContactMethodType(type);
            if (isPrimary) {
                flags |= ContactChange.FLAG_PREFERRED;
            }

            if (kind == CONTACT_METHODS_KIND_EMAIL) {
                cc = new ContactChange(ContactChange.KEY_VCARD_EMAIL, value, flags);
                cc.setNabDetailId(nabDetailId);

            } else if (kind == CONTACT_METHODS_KIND_ADDRESS) {
                if (!TextUtils.isEmpty(value)) {
                    cc = new ContactChange(ContactChange.KEY_VCARD_ADDRESS, VCardHelper
                            .makePostalAddress(parseRawAddress(value)), flags);
                    cc.setNabDetailId(nabDetailId);
                }
            }
        }

        return cc;
    }

    /**
     * Reads Organization details from a Contact into the provided
     * {@link ContactChange} List
     * 
     * @param ccList {@link ContactChange} list to add details into
     * @param nabContactId ID of the NAB Contact
     */
    private void readContactOrganizations(List<ContactChange> ccList, long nabContactId) {
        final Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, nabContactId);
        final Uri contactOrganizationsUri = Uri.withAppendedPath(contactUri,
                Contacts.Organizations.CONTENT_DIRECTORY);
        final Cursor cursor = mCr.query(contactOrganizationsUri, null, null, null,
                Organizations._ID);

        if (cursor == null) {
            return;
        }

        try {
            // Only loops while there is not Organization read (CAB limitation!)
            while (!mHaveReadOrganization && cursor.moveToNext()) {
                readContactOrganization(cursor, ccList, nabContactId);
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }

        // Reset the boolean flag
        mHaveReadOrganization = false;
    }

    /**
     * Reads one Organization Detail from the supplied cursor into a
     * {@link ContactChange} Note that this method may actually read up to 2
     * into Contact Changes. However, Organization and Title may only be read if
     * a boolean flag is not set
     * 
     * @param cursor Cursor to read from
     * @param ccList {@link ContactChange} list to add details to
     * @param nabContactId ID of the NAB Contact
     * @return Read Contact Method Detail or null
     */
    private void readContactOrganization(Cursor cursor, List<ContactChange> ccList,
            long nabContactId) {
        final long nabDetailId = CursorUtils.getLong(cursor, Organizations._ID);
        final boolean isPrimary = CursorUtils.getInt(cursor, Organizations.ISPRIMARY) != 0;
        final int type = CursorUtils.getInt(cursor, Organizations.TYPE);
        int flags = mapFromNabOrganizationType(type);
        if (isPrimary) {
            flags |= ContactChange.FLAG_PREFERRED;
        }

        if (!mHaveReadOrganization) {
            // Company
            final String company = CursorUtils.getString(cursor, Organizations.COMPANY);
            if (!TextUtils.isEmpty(company)) {
                // Escaping the value (no need to involve the VCardHelper just
                // for the company)
                final String escapedCompany = company.replace(";", "\\;");

                final ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_ORG,
                        escapedCompany, flags);
                cc.setNabContactId(nabContactId);
                cc.setNabDetailId(nabDetailId);
                ccList.add(cc);
                mHaveReadOrganization = true;
            }

            // Title
            final String title = CursorUtils.getString(cursor, Organizations.TITLE);
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
     * "Pre-processing" of {@link ContactChange} list before it can be added to
     * NAB. This needs to be done because of issues on the 1.X NAB and our CAB
     * In 1.X NAB: Name and Note which are stored differently from other details
     * In our CAB: Organization and Title are separate details contrary to the
     * NAB
     * 
     * @param ccList {@link ContactChange} list for the add operation
     */
    private void preprocessContactToAdd(ContactChange[] ccList) {
        final int ccListSize = ccList.length;
        boolean nameFound = false, noteFound = false;
        mValues.clear();
        for (int i = 0; i < ccListSize; i++) {
            final ContactChange cc = ccList[i];
            if (cc != null) {
                if (!nameFound && cc.getKey() == ContactChange.KEY_VCARD_NAME) {
                    final Name name = VCardHelper.getName(cc.getValue());
                    mValues.put(People.NAME, name.toString());
                    nameFound = true;
                }

                if (!noteFound && cc.getKey() == ContactChange.KEY_VCARD_NOTE) {
                    mValues.put(People.NOTES, cc.getValue());
                    noteFound = true;
                }

                if (mMarkedOrganizationIndex < 0 && cc.getKey() == ContactChange.KEY_VCARD_ORG) {
                    mMarkedOrganizationIndex = i;
                }

                if (mMarkedTitleIndex < 0 && cc.getKey() == ContactChange.KEY_VCARD_TITLE) {
                    mMarkedTitleIndex = i;
                }
            }
        }
    }

    /**
     * Writes new details from a provided {@link ContactChange} list into the
     * NAB. The resulting detail IDs are put in another provided
     * {@link ContactChange} list.
     * 
     * @param ccList {@link ContactChange} list to write from
     * @param idChangeList {@link ContatChange} list where IDs for the written
     *            details are put
     * @param nabContactId ID of the NAB Contact
     */
    private void writeDetails(ContactChange[] ccList, ContactChange[] idChangeList,
            long nabContactId) {
        final int ccListSize = ccList.length;
        for (int i = 0; i < ccListSize; i++) {
            final ContactChange cc = ccList[i];
            if (cc != null) {
                long nabDetailId = insertDetail(cc, nabContactId);
                if (nabDetailId != ContactChange.INVALID_ID) {
                    // The +1 assumes prior addition of the contact id at
                    // position 0
                    idChangeList[i + 1] = ContactChange.createIdsChange(cc,
                            ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                    idChangeList[i + 1].setNabDetailId(nabDetailId);
                }
            }
        }
    }

    /**
     * Inserts a new detail to NAB
     * 
     * @param cc {@link ContactChange} to read data from
     * @param nabContactId ID of the NAB Contact
     * @return The created detail's ID
     */
    private long insertDetail(ContactChange cc, long nabContactId) {
        mValues.clear();
        Uri contentUri = null;
        switch (cc.getKey()) {
            case ContactChange.KEY_VCARD_PHONE:
                putPhoneValues(cc, nabContactId);
                contentUri = Phones.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_EMAIL:
                putContactMethodValues(cc, CONTACT_METHODS_KIND_EMAIL, nabContactId);
                contentUri = ContactMethods.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_ADDRESS:
                putContactMethodValues(cc, CONTACT_METHODS_KIND_ADDRESS, nabContactId);
                contentUri = ContactMethods.CONTENT_URI;
                break;
        }

        if (contentUri != null) {
            Uri uri = mCr.insert(contentUri, mValues);
            if (uri != null) {
                return ContentUris.parseId(uri);
            }
        }
        return ContactChange.INVALID_ID;
    }

    /**
     * Updates a detail.
     * 
     * @param cc {@link ContactChange} to read data from
     * @param nabContactId ID of the NAB Contact
     * @return true if the detail was updated, false if not
     */
    private boolean updateDetail(ContactChange cc) {
        mValues.clear();
        Uri contentUri = null;
        long nabDetailId = cc.getNabDetailId();
        final long nabContactId = cc.getNabContactId();
        switch (cc.getKey()) {
            case ContactChange.KEY_VCARD_PHONE:
                putPhoneValues(cc, nabContactId);
                contentUri = Phones.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_EMAIL:
                putContactMethodValues(cc, CONTACT_METHODS_KIND_EMAIL, nabContactId);
                contentUri = ContactMethods.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_ADDRESS:
                putContactMethodValues(cc, CONTACT_METHODS_KIND_ADDRESS, nabContactId);
                contentUri = ContactMethods.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_NAME:
                final Name name = VCardHelper.getName(cc.getValue());
                mValues.put(People.NAME, name.toString());
                contentUri = People.CONTENT_URI;
                nabDetailId = nabContactId;
                break;
            case ContactChange.KEY_VCARD_NOTE:
                mValues.put(People.NOTES, cc.getValue());
                contentUri = People.CONTENT_URI;
                nabDetailId = nabContactId;
                break;
        }

        if (contentUri != null) {
            Uri uri = ContentUris.withAppendedId(contentUri, nabDetailId);
            return mCr.update(uri, mValues, null, null) > 0;
        }

        return false;
    }

    /**
     * Deletes a detail from NAB
     * 
     * @param key The detail key
     * @param nabContactId ID of the NAB Contact
     * @param nabDetailId ID of the NAB Detail
     * @return true if the detail was deleted, false if not
     */
    private boolean deleteDetail(int key, long nabContactId, long nabDetailId) {
        mValues.clear();
        Uri contentUri = null;
        switch (key) {
            case ContactChange.KEY_VCARD_PHONE:
                contentUri = Phones.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_EMAIL:
            case ContactChange.KEY_VCARD_ADDRESS:
                contentUri = ContactMethods.CONTENT_URI;
                break;
            case ContactChange.KEY_VCARD_NAME:
                mValues.putNull(People.NAME);
                contentUri = People.CONTENT_URI;
                nabDetailId = nabContactId;
                break;
            case ContactChange.KEY_VCARD_NOTE:
                mValues.putNull(People.NOTES);
                contentUri = People.CONTENT_URI;
                nabDetailId = nabContactId;
                break;
        }

        if (contentUri != null) {
            final Uri uri = ContentUris.withAppendedId(contentUri, nabDetailId);
            if (mValues.size() > 0) {
                return mCr.update(uri, mValues, null, null) > 0;
            } else {
                return mCr.delete(uri, null, null) > 0;
            }
        }

        return false;
    }

    /**
     * Put Phone detail into the values
     * 
     * @param cc
     * @param nabContactId ID of the NAB Contact
     */
    private void putPhoneValues(ContactChange cc, long nabContactId) {
        mValues.put(Phones.PERSON_ID, nabContactId);
        mValues.put(Phones.NUMBER, cc.getValue());
        int flags = cc.getFlags();
        mValues.put(Phones.TYPE, mapToNabPhoneType(flags));
        mValues.put(Phones.ISPRIMARY, flags & ContactChange.FLAG_PREFERRED);
        /*
         * Forcing the label field to be null because we don't support custom
         * labels and in case we replace from a custom label to home or private
         * type without setting label to null, mCr.update() will throw a SQL
         * constraint exception.
         */
        mValues.put(Phones.LABEL, (String)null);
    }

    /**
     * Put Contact Methods detail into the values
     * 
     * @param cc {@link ContactChange} to read values from
     * @param kind The kind of contact method (email or address)
     * @param nabContactId ID of the NAB Contact
     */
    private void putContactMethodValues(ContactChange cc, int kind, long nabContactId) {
        mValues.put(ContactMethods.PERSON_ID, nabContactId);
        if (kind == CONTACT_METHODS_KIND_EMAIL) {
            mValues.put(ContactMethods.DATA, cc.getValue());
        } else {
            // Must be Address, once again need to use VCardHelper to extract
            // address
            PostalAddress address = VCardHelper.getPostalAddress(cc.getValue());
            mValues.put(ContactMethods.DATA, address.toString());
        }
        mValues.put(ContactMethods.KIND, kind);
        int flags = cc.getFlags();
        mValues.put(ContactMethods.TYPE, mapToNabContactMethodType(flags));
        mValues.put(ContactMethods.ISPRIMARY, flags & ContactChange.FLAG_PREFERRED);
    }

    /**
     * Ugly method to process organization for writing to NAB
     * 
     * @param ccList {@link ContactChange} list to fetch organization from
     * @param idChangeList {@link ContatChange} list where IDs for the written
     *            organization are put
     * @param nabContactId ID of the NAB Contact
     */
    private void processOrganization(ContactChange[] ccList, ContactChange[] idChangeList,
            long nabContactId) {

        final long organizationId = writeOrganization(ccList, nabContactId);

        if (organizationId != ContactChange.INVALID_ID) {
            if (mMarkedOrganizationIndex >= 0) {
                idChangeList[mMarkedOrganizationIndex + 1] = ContactChange.createIdsChange(
                        ccList[mMarkedOrganizationIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChangeList[mMarkedOrganizationIndex + 1].setNabDetailId(organizationId);
            }

            if (mMarkedTitleIndex >= 0) {
                idChangeList[mMarkedTitleIndex + 1] = ContactChange.createIdsChange(
                        ccList[mMarkedTitleIndex], ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                idChangeList[mMarkedTitleIndex + 1].setNabDetailId(organizationId);
            }
        }
    }

    /**
     * Write Organization detail to NAB
     * 
     * @param ccList {@link ContactChange} list where Organization and Title are
     *            found
     * @param nabContactId ID of the NAB Contact
     * @return ID of the NAB Contact
     */
    private long writeOrganization(ContactChange[] ccList, long nabContactId) {
        mValues.clear();
        int flags = ContactChange.FLAG_NONE;
        if (mMarkedOrganizationIndex >= 0) {
            final ContactChange cc = ccList[mMarkedOrganizationIndex];
            final Organisation organization = VCardHelper.getOrg(cc.getValue());
            if (!TextUtils.isEmpty(organization.name)) {
                mValues.put(Organizations.COMPANY, organization.name);
                flags |= cc.getFlags();
            }
        }

        if (mMarkedTitleIndex >= 0) {
            final ContactChange cc = ccList[mMarkedTitleIndex];
            // No need to check for empty values as there is only one
            flags |= cc.getFlags();
            mValues.put(Organizations.TITLE, cc.getValue());
        }

        if (mValues.size() > 0) {
            mValues.put(Organizations.PERSON_ID, nabContactId);
            mValues.put(Organizations.TYPE, mapToNabOrganizationType(flags));

            final Uri uri = mCr.insert(Organizations.CONTENT_URI, mValues);
            if (uri != null) {
                return ContentUris.parseId(uri);
            }
        }

        return ContactChange.INVALID_ID;
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
     * @return In the Organization insertion case this should contain the new ID
     *         and in the update case should contain the existing ID
     */
    private long updateOrganization(ContactChange[] ccList, long nabContactId) {
        if (mMarkedOrganizationIndex < 0 && mMarkedTitleIndex < 0) {
            // no organization or title to update - do nothing
            return ContactChange.INVALID_ID;
        }

        long detailId = ContactChange.INVALID_ID;

        int flags = ContactChange.FLAG_NONE;

        String company = null;
        String title = null;

        final Uri organizationUri = Uri.withAppendedPath(ContentUris.withAppendedId(
                People.CONTENT_URI, nabContactId), Contacts.Organizations.CONTENT_DIRECTORY);

        final Cursor cursor = mCr.query(organizationUri, null, null, null, Organizations._ID);

        // First retrieve the values that are already present,
        // assuming that the lowest id is the one in CAB
        try {
            if (cursor != null && cursor.moveToNext()) {
                company = CursorUtils.getString(cursor, Organizations.COMPANY);
                title = CursorUtils.getString(cursor, Organizations.TITLE);
                detailId = CursorUtils.getLong(cursor, Organizations._ID);
                flags = mapFromNabOrganizationType(CursorUtils.getInt(cursor, Organizations.TYPE));
                final boolean isPrimary = CursorUtils.getInt(cursor, Organizations.ISPRIMARY) > 0;
                if (isPrimary) {
                    flags |= ContactChange.FLAG_PREFERRED;
                }
            }
        } finally {
            CursorUtils.closeCursor(cursor);
        }

        if (mMarkedOrganizationIndex >= 0) {
            final ContactChange cc = ccList[mMarkedOrganizationIndex];
            if (cc.getType() != ContactChange.TYPE_DELETE_DETAIL) {
                final String value = cc.getValue();
                if (value != null) {
                    final VCardHelper.Organisation organization = VCardHelper.getOrg(value);
                    if (!TextUtils.isEmpty(organization.name)) {
                        company = organization.name;
                    }
                }
                flags = cc.getFlags();
            } else { // Delete case
                company = null;
            }
        }

        if (mMarkedTitleIndex >= 0) {
            final ContactChange cc = ccList[mMarkedTitleIndex];
            title = cc.getValue();
            if (cc.getType() != ContactChange.TYPE_DELETE_DETAIL) {
                flags = cc.getFlags();
            }
        }

        if (company != null || title != null) {
            mValues.clear();
            /*
             * Forcing the label field to be null because we don't support
             * custom labels and in case we replace from a custom label to home
             * or private type without setting label to null, mCr.update() will
             * throw a SQL constraint exception.
             */
            mValues.put(Organizations.LABEL, (String)null);
            mValues.put(Organizations.COMPANY, company);
            mValues.put(Organizations.TITLE, title);
            mValues.put(Organizations.TYPE, mapToNabOrganizationType(flags));
            mValues.put(Organizations.ISPRIMARY, flags & ContactChange.FLAG_PREFERRED);

            final Uri existingUri = ContentUris.withAppendedId(Contacts.Organizations.CONTENT_URI,
                    detailId);

            if (detailId != ContactChange.INVALID_ID) {
                mCr.update(existingUri, mValues, null, null);
            } else {
                // insert
                final Uri idUri = mCr.insert(organizationUri, mValues);
                if (idUri != null) {
                    return ContentUris.parseId(idUri);
                }
            }
        } else if (detailId != ContactChange.INVALID_ID) {
            final Uri existingUri = ContentUris.withAppendedId(Contacts.Organizations.CONTENT_URI,
                    detailId);
            mCr.delete(existingUri, null, null);
        } else {
            mMarkedOrganizationIndex = mMarkedTitleIndex = -1;
        }

        // Updated detail id or ContactChange.INVALID_ID if deleted
        return detailId;
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
            case Phones.TYPE_HOME:
                return ContactChange.FLAG_HOME;
            case Phones.TYPE_MOBILE:
                return ContactChange.FLAG_CELL;
            case Phones.TYPE_WORK:
                return ContactChange.FLAG_WORK;
            case Phones.TYPE_FAX_HOME:
                return ContactChange.FLAGS_HOME_FAX;
            case Phones.TYPE_FAX_WORK:
                return ContactChange.FLAGS_WORK_FAX;
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
        if ((flags & ContactChange.FLAGS_HOME_FAX) == ContactChange.FLAGS_HOME_FAX) {
            return Phones.TYPE_FAX_HOME;
        }

        if ((flags & ContactChange.FLAGS_WORK_FAX) == ContactChange.FLAGS_WORK_FAX) {
            return Phones.TYPE_FAX_WORK;
        }

        if ((flags & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            return Phones.TYPE_HOME;
        }

        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return Phones.TYPE_WORK;
        }

        if ((flags & ContactChange.FLAG_CELL) == ContactChange.FLAG_CELL) {
            return Phones.TYPE_MOBILE;
        }

        return Phones.TYPE_OTHER;
    }

    /**
     * Maps a method type from the native value into the {@link ContactChange}
     * flags
     * 
     * @param nabType Native method type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabContactMethodType(int nabType) {
        switch (nabType) {
            case ContactMethods.TYPE_HOME:
                return ContactChange.FLAG_HOME;
            case ContactMethods.TYPE_WORK:
                return ContactChange.FLAG_WORK;
        }

        return ContactChange.FLAG_NONE;
    }

    /**
     * Maps {@link ContactChange} flags into the native method type.
     * 
     * @param flags {@link ContactChange} flags
     * @return Native method type
     */
    private static int mapToNabContactMethodType(int flags) {
        if ((flags & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            return ContactMethods.TYPE_HOME;
        }

        if ((flags & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            return ContactMethods.TYPE_WORK;
        }

        return ContactMethods.TYPE_OTHER;
    }

    /**
     * Maps a organization type from the native value into the
     * {@link ContactChange} flags
     * 
     * @param nabType Given native organization type
     * @return {@link ContactChange} flags
     */
    private static int mapFromNabOrganizationType(int nabType) {
        if (nabType == Organizations.TYPE_WORK) {
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
            return Organizations.TYPE_WORK;
        }

        return Organizations.TYPE_OTHER;
    }

    /**
     * Utility method used to parse a raw display Address into a
     * {@link VCardHelper.PostalAddress} object. The purpose of this method is
     * simply to remove '\n' separators. It does not aim to correctly map
     * address components to the right place, e.g. it is not guaranteed that
     * 'PostalAddress.Country' will really be the Country.
     */
    private static PostalAddress parseRawAddress(String rawAddress) {
        final PostalAddress address = new PostalAddress();
        final String[] tokens = rawAddress.trim().split("\n");
        final int numTokens = tokens.length;

        address.addressLine1 = tokens[0];

        if (numTokens > 1) {
            address.addressLine2 = tokens[1];
        }

        if (numTokens > 2) {
            address.city = tokens[2];
        }

        if (numTokens > 3) {
            address.county = tokens[3];
        }

        if (numTokens > 4) {
            address.postCode = tokens[4];
        }

        if (numTokens > 5) {
            address.country = tokens[5];
        }

        if (numTokens > 6) {
            final StringBuilder sb = new StringBuilder();
            sb.append(tokens[6]);
            for (int i = 7; i < numTokens; i++) {
                sb.append(' ');
                sb.append(tokens[i]);
            }

            address.postOfficeBox = sb.toString();
        }

        return address;
    }
}
