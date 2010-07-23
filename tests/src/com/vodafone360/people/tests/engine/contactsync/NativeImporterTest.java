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

package com.vodafone360.people.tests.engine.contactsync;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import android.util.Log;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.engine.contactsync.NativeImporter;
import com.vodafone360.people.engine.contactsync.PeopleContactsApi;
import com.vodafone360.people.engine.contactsync.NativeContactsApi.Account;
import com.vodafone360.people.utils.VersionUtils;

import junit.framework.TestCase;


public class NativeImporterTest extends TestCase {
    

    /**
     * 360 client account type.
     */
    protected static final int PEOPLE_ACCOUNT_TYPE = 1;

    /**
     * Google account type.
     */
    protected static final int GOOGLE_ACCOUNT_TYPE = 2;

    /**
     * Vendor specific type.
     */
    protected static final int PHONE_ACCOUNT_TYPE = 3;

    /**
     * Account type for 360 People in the Native Accounts. 
     * MUST be a copy of type in 'res/xml/authenticator.xml' 
     */
    protected static final String PEOPLE_ACCOUNT_TYPE_STRING = "com.vodafone360.people.android.account";

    /**
     * Google account, there can be more than one of these
     */
    protected static final String GOOGLE_ACCOUNT_TYPE_STRING = "com.google";    
    
    /*
    * A third party account type.
    */
   private final static String THIRD_PARTY_ACCOUNT_TYPE_STRING = "com.thirdparty";
    /**
     * The internal max operation count of the NativeImporter.
     * @see NativeImporter#MAX_CONTACTS_OPERATION_COUNT
     */
    private final static int NATIVE_IMPORTER_MAX_OPERATION_COUNT = (Integer)getField("MAX_CONTACTS_OPERATION_COUNT", com.vodafone360.people.engine.contactsync.NativeImporter.class);
    
    /**
     * A count of operations below the maximum.
     * @see NativeImporter#MAX_CONTACTS_OPERATION_COUNT
     */
    private final static int NATIVE_CONTACTS_COUNT_BELOW_MAX_OPERATION_COUNT = NATIVE_IMPORTER_MAX_OPERATION_COUNT / 2;
    
    /**
     * A count of operations above the maximum.
     * @see NativeImporter#MAX_CONTACTS_OPERATION_COUNT
     */
    private final static int NATIVE_CONTACTS_COUNT_OVER_MAX_OPERATION_COUNT = (NATIVE_IMPORTER_MAX_OPERATION_COUNT * 5) + (NATIVE_IMPORTER_MAX_OPERATION_COUNT / 2);
    
    /**
     * A test Gmail account.
     */
    public final static Account GMAIL_ACCOUNT_1 = new Account("mylogin@gmail.com", GOOGLE_ACCOUNT_TYPE_STRING);
    
    /**
     * A test Gmail account.
     */
    public final static Account GMAIL_ACCOUNT_2 = new Account("mylogin2@googlemail.com", GOOGLE_ACCOUNT_TYPE_STRING);
    
    /**
     * A test People account.
     */
    public final static Account PEOPLE_ACCOUNT = new Account("mypeoplelogin", PEOPLE_ACCOUNT_TYPE_STRING);
    
    /**
     * A test third party account.
     */
    public final static Account THIRD_PARTY_ACCOUNT = new Account("mythirdpartylogin", THIRD_PARTY_ACCOUNT_TYPE_STRING);
    
    /**
     * Tests that the number of ticks needed to perform the native import is as expected when the count of contacts
     * is below MAX_CONTACTS_OPERATION_COUNT.
     */
    public void testRequiredTicksForBelowMaxOperationCountImport() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);

        performNativeImport(nativeMockup, peopleMockup, NATIVE_CONTACTS_COUNT_BELOW_MAX_OPERATION_COUNT);
    }
    
    /**
     * Tests that the number of ticks needed to perform the native import is as expected when the count of contacts
     * is over MAX_CONTACTS_OPERATION_COUNT.
     */
    public void testRequiredTicksForOverMaxOperationCountImport() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        performNativeImport(nativeMockup, peopleMockup, NATIVE_CONTACTS_COUNT_OVER_MAX_OPERATION_COUNT);
    }
    
    /**
     * Tests multiple imports from native with only new contacts.
     */
    public void testMultipleImportsFromNativeWithNewContacts() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 15, null);
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(15, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 15, null);
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(30, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 15, null);
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(45, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests multiple imports from native with deleted contacts.
     */
    public void testMultipleImportsFromNativeWithDeletedContacts() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 15, null);
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(15, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // delete some contacts on native side
        nativeMockup.removeContact(1);
        nativeMockup.removeContact(8);
        nativeMockup.removeContact(14);
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(12, nativeIds.length);
        
        // sync with people side
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests multiple imports from native with an updated contact via added details.
     */
    public void testMultipleImportsFromNativeWithUpdatedContact_addedDetails() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 20, null);
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(20, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // pick an existing contact and add details
        final ContactChange[] contact = nativeMockup.getContact(10);
        nativeMockup.setContact(10, addDetails(nativeMockup, contact));
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests multiple imports from native with an updated contact via deleted details.
     */
    public void testMultipleImportsFromNativeWithUpdatedContact_deletedDetails() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 20, null);
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(20, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // pick an existing contact and delete details
        final ContactChange[] originalContact = nativeMockup.getContact(10);
        nativeMockup.setContact(10, addDetails(nativeMockup, originalContact));
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // remove the added details by setting the contact back to its previous state
        nativeMockup.setContact(10, originalContact);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests multiple imports from native with an updated contact via updated details.
     */
    public void testMultipleImportsFromNativeWithUpdatedContact_updatedDetails() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts on native side
        feedNativeContactsApi(nativeMockup, 20, null);
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(20, nativeIds.length);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // pick an existing contact
        final ContactChange[] originalContact = nativeMockup.getContact(10);

        // modify its details
        for (int i = 0; i < originalContact.length; i++) {
            
            final ContactChange originalChange = originalContact[i];
            originalContact[i] = alterContactChangeValue(originalChange, originalChange.getValue()+"x9x");
        }
        nativeMockup.setContact(10, originalContact);
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests that the correct accounts are used during the first time import and later on.
     * 
     * Here there are no Google accounts at all so the import shall be performed from the
     * "null" account on both Android platforms (1.X and 2.X).
     */
    public void testAccounts_firstTimeImport_noGoogleAccounts() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup2(null);
        
        // feed contacts on native side
        feedNativeContactsApi(nativeMockup, 20, null);
        
        final long[] thirdPartyIds = nativeMockup.getContactIds(null);
        assertEquals(20, thirdPartyIds.length);
        
        // import the new contacts
        runNativeImporterFirstTimeImport(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests that the correct accounts are used during the first time import and later on.
     * 
     * -On Android 1.X platform: it is expected that the import is always performed from the "null" account (default)
     * -On Android 2.X platform: it is expected that the import is first performed from all the Google accounts or
     *                           the "null" account if no Google account are set
     */
    public void testAccounts_firstTimeImport_2GoogleAccounts() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup2(null);
        
        final boolean is2XPlatform = VersionUtils.is2XPlatform();
        
        if (is2XPlatform) {
            
            // add contact in 2 Google accounts
            nativeMockup.feedAccount(GMAIL_ACCOUNT_1);
            feedNativeContactsApi(nativeMockup, 20, GMAIL_ACCOUNT_1);
            
            nativeMockup.feedAccount(GMAIL_ACCOUNT_2);
            feedNativeContactsApi(nativeMockup, 20, GMAIL_ACCOUNT_2);
            
            // add extra contacts in the third party account (they shall be ignored by the importer)
            nativeMockup.feedAccount(THIRD_PARTY_ACCOUNT);
            feedNativeContactsApi(nativeMockup, 20, THIRD_PARTY_ACCOUNT);
            
            // add extra contacts in the People account (they shall be ignored by the importer)
            nativeMockup.feedAccount(PEOPLE_ACCOUNT);
            feedNativeContactsApi(nativeMockup, 20, PEOPLE_ACCOUNT);
            
            final long[] thirdPartyIds = nativeMockup.getContactIds(THIRD_PARTY_ACCOUNT);
            assertEquals(20, thirdPartyIds.length);
            
            final long[] gmail1Ids = nativeMockup.getContactIds(GMAIL_ACCOUNT_1);
            assertEquals(20, gmail1Ids.length);
            
            final long[] gmail2Ids = nativeMockup.getContactIds(GMAIL_ACCOUNT_2);
            assertEquals(20, gmail2Ids.length);
            
            final long[] peopleIds = nativeMockup.getContactIds(PEOPLE_ACCOUNT);
            assertEquals(20, peopleIds.length);
            
            // import the new contacts
            runNativeImporterFirstTimeImport(nativeMockup, peopleMockup);
            
            // check that only the Google contacts have been imported
            final Account[] accounts = new Account[2];
            accounts[0] = GMAIL_ACCOUNT_1;
            accounts[1] = GMAIL_ACCOUNT_2;
            assertTrue(compareNativeAndPeopleContactsList(nativeMockup, accounts, peopleMockup));
        } else {
        
            // this test does not apply to Android 1.X so it just passes
        }
    }
    
    /**
     * Tests that the correct account for the next imports (i.e. not the first time import)
     * is used depending on the Android platform.
     * 
     * -On Android 1.X: the import shall be done from the "null" account
     * -On Android 2.X: the import shall be done from the People account
     */
    public void testAccounts_nextTimeImport() {
        
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        final boolean is2XPlatform = VersionUtils.is2XPlatform();
        
        // add new contacts on native side
        if (is2XPlatform) {
            
            // in the People account for Android 2.X
            nativeMockup.feedAccount(PEOPLE_ACCOUNT);
            feedNativeContactsApi(nativeMockup, 20, PEOPLE_ACCOUNT);
            
        } else {
            
            // in the "null" account for Android 1.X
            feedNativeContactsApi(nativeMockup, 20, null);
            long[] nativeIds = nativeMockup.getContactIds(null);
            assertEquals(20, nativeIds.length);
        }
        
        // import the new contacts
        runNativeImporter(nativeMockup, peopleMockup);
        
        // compare contacts on both sides
        assertTrue(compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Performs a native import and checks that the needed ticks are as expected.
     * 
     * @param ncam the NativeContactsApiMockup instance
     * @param pcam the PeopleContactsApiMockup instance
     * @param contactsCount the number of native contacts to setup and import
     */
    private void performNativeImport(NativeContactsApiMockup ncam, PeopleContactsApiMockup pcam, int contactsCount) {
        
        final NativeImporter nativeImporter = new NativeImporter(pcam, ncam, false);
        final int requiredTicks = 1 + (contactsCount / NATIVE_IMPORTER_MAX_OPERATION_COUNT) + (contactsCount % NATIVE_IMPORTER_MAX_OPERATION_COUNT > 0 ? 1 : 0);
        
        // feed the native side
        feedNativeContactsApi(ncam, contactsCount, null);
        final long[] nativeIds = ncam.getContactIds(null);
        assertEquals(contactsCount, nativeIds.length);
        
        // setup the people client side
        long[] peopleIds = pcam.getNativeContactsIds();
        assertNull(peopleIds);
        
        for (int i = 0; i < requiredTicks; i++) {
            
            // check the importer state
            assertEquals(NativeImporter.RESULT_UNDEFINED, nativeImporter.getResult());
            assertTrue(!nativeImporter.isDone());
            
            // perform an import tick
            nativeImporter.tick();
        }
        
        // check the importer state, it shall have finished the import
        assertEquals(NativeImporter.RESULT_OK, nativeImporter.getResult());
        assertTrue(nativeImporter.isDone());
        
        // check that all the native contacts are on the people client side
        peopleIds = pcam.getNativeContactsIds();
        assertEquals(contactsCount, peopleIds.length);
        
        for (int i = 0; i < peopleIds.length; i++) {
            
            final ContactChange[] peopleContact = pcam.getContact(peopleIds[i]);
            final ContactChange[] nativeContact = pcam.getContact(nativeIds[i]);
            
            assertTrue(ContactChangeHelper.areChangeListsEqual(peopleContact, nativeContact, false));
        }
    }
    
    /**
     * Runs the NativeImporter until it finishes synchronizing.
     * 
     * @param ncam the NativeContactsApiMockup instance
     * @param pcam the PeopleContactsApiMockup instance
     */
    private void runNativeImporter(NativeContactsApiMockup ncam, PeopleContactsApiMockup pcam) {
        
        final NativeImporter nativeImporter = new NativeImporter(pcam, ncam, false);
        
        while (!nativeImporter.isDone()) {
            
            // run the NativeImporter until the import from native is over
            nativeImporter.tick();
        }
    }
    
    /**
     * Runs the NativeImporter for a first time import until it finishes synchronizing.
     * 
     * @param ncam the NativeContactsApiMockup instance
     * @param pcam the PeopleContactsApiMockup instance
     */
    private void runNativeImporterFirstTimeImport(NativeContactsApiMockup ncam, PeopleContactsApiMockup pcam) {
        
        final NativeImporter nativeImporter = new NativeImporter(pcam, ncam, true);
        
        while (!nativeImporter.isDone()) {
            
            // run the NativeImporter until the import from native is over
            nativeImporter.tick();
        }
    }
    
    /**
     * Compares the contacts stored on native and people side.
     * 
     * @param ncam the handle to the NativeContactsApiMockup instance
     * @param pcam the handle to the PeopleContactsApiMockup instance
     * @return true if both sides contains exactly the same contacts, false otherwise
     */
    public static boolean compareNativeAndPeopleContactsList(NativeContactsApiMockup ncam, Account[] accounts, PeopleContactsApiMockup pcam) {
        
        long[] nativeIds = null;
        long[] peopleIds = pcam.getNativeContactsIds();
        
        // get the native ids
        if (accounts != null) {
            
            // get the ids for all the provided accounts and merge them into one array
            long[][] nativeIdsPerAccount = null;
            int totalNativeIds = 0;
            nativeIdsPerAccount = new long[accounts.length][];
            for (int i = 0; i < nativeIdsPerAccount.length; i++) {
                nativeIdsPerAccount[i] = ncam.getContactIds(accounts[i]);
                totalNativeIds += nativeIdsPerAccount[i].length;
            }
            
            nativeIds = new long[totalNativeIds];
            int index = 0;
            for (int i = 0; i < nativeIdsPerAccount.length; i++) {
                
                System.arraycopy(nativeIdsPerAccount[i], 0, nativeIds, index, nativeIdsPerAccount[i].length);
                index += nativeIdsPerAccount[i].length;
            }
            
            Arrays.sort(nativeIds);
            
        } else {
            
            // get all the ids
            nativeIds = ncam.getContactIds(null);
        }
        
        // compare contacts from both sides
        if (nativeIds == null && peopleIds == null) {
            
            // both sides are empty
            return true;
        } else if (nativeIds == null || peopleIds == null) {
            
            return false;
        } else {
            
            if (nativeIds.length != peopleIds.length) {
                
                return false;
            } else {
                
                for (int i = 0; i < nativeIds.length; i++) {
                    
                    if (nativeIds[i] != peopleIds[i]) {
                        
                        return false;
                    }
                    
                    final ContactChange[] nativeContact = ncam.getContact(nativeIds[i]);
                    final ContactChange[] peopleContact = pcam.getContact(peopleIds[i]);
                    
                    if (nativeContact == null && peopleContact == null) {
                        
                        continue;
                    } else if (nativeContact == null || peopleContact == null) {
                        
                        return false;
                    } else {
                        
                        if (!ContactChangeHelper.areChangeListsEqual(nativeContact, peopleContact, false)) {
                            
                            return false;
                        }
                    }
                }
                
                return true;
            }
        }
    }

    /**
     * Feeds contacts to the native side.
     * 
     * @param ncam the handle to the NativeContactsApiMockup instance
     * @param contactsCount the number of contacts to add
     * @param account the account where to add the contact
     */
    private void feedNativeContactsApi(NativeContactsApiMockup ncam, int contactsCount, Account account) {
        
        for (int i = 0; i < contactsCount; i++) {
            
            final ContactChange[] contact = ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, ContactChange.INVALID_ID);
            ncam.addContact(account, contact);
        }
    }
    
    /**
     * Adds details to the provided contact.
     * 
     * @param contact
     * @return
     */
    private ContactChange[] addDetails(NativeContactsApiMockup ncam, ContactChange[] contact) {
        
        long nativeId = contact[0].getNabContactId();
        int index = contact.length;
        
        // create the new Contact
        ContactChange[] newContact = new ContactChange[contact.length + 2];
        // copy original info into it
        System.arraycopy(contact, 0, newContact, 0, contact.length);
        
        // add extra details
        newContact[index] = new ContactChange(ContactChange.KEY_VCARD_EMAIL, "xxxxx@xxxxx.xx", ContactChange.FLAG_WORK);
        newContact[index].setNabContactId(nativeId);
        newContact[index].setNabDetailId(ncam.getAndIncNabDetailId());
        index++;
        
        newContact[index] = new ContactChange(ContactChange.KEY_VCARD_PHONE, "+9912345678", ContactChange.FLAG_WORK);
        newContact[index].setNabContactId(nativeId);
        newContact[index].setNabDetailId(ncam.getAndIncNabDetailId());
        index++;
        
        return newContact;
    }

    /**
     * Mocks up the PeopleContactsApi.
     */
    public static class PeopleContactsApiMockup extends PeopleContactsApi {
        
        protected Hashtable<Long, ContactChange[]> mPeopleContacts = new Hashtable<Long, ContactChange[]>();
        
        protected int mLocalContactId = 1;
        protected int mLocalDetailId = 1;

        public PeopleContactsApiMockup(DatabaseHelper dbh) {
            
            super(null);
        }
        
        public int getAndIncLocalDetailId() {
            
            return mLocalDetailId++;
        }
        
        @Override
        public long[] getNativeContactsIds() {
            
            if (mPeopleContacts.size() > 0) {
                
                final Enumeration<ContactChange[]> e = mPeopleContacts.elements();
                final long[] ids = new long[mPeopleContacts.size()];
                int idIndex = 0;
                
                while (e.hasMoreElements()) {
                    
                    final ContactChange[] contact = e.nextElement();
                    ids[idIndex++] = contact[0].getNabContactId();
                }
                
                Arrays.sort(ids);
                
                return ids;
            } else {
                
                return null;
            }
            
        }
        
        @Override
        public boolean addNativeContact(ContactChange[] contact) {
            
            // set the local ids
            for (int i = 0; i < contact.length; i++) {
                
                contact[i].setInternalContactId(mLocalContactId);
                contact[i].setInternalDetailId(mLocalDetailId++);
            }
            
            mLocalContactId++;
            
            // add the contact via its native id !!!
            mPeopleContacts.put(contact[0].getNabContactId(), contact);
            
            return true;
        }

        @Override
        public boolean deleteNativeContact(long nativeId, boolean syncToNative) {

            if (mPeopleContacts.containsKey(nativeId)) {
                
                mPeopleContacts.remove(nativeId);
            }
            return false;
        }

        @Override
        public ContactChange[] getContact(long nativeId) {

            // return a copy, the NativeImporter may mess with it
            final ContactChange[] original = mPeopleContacts.get(nativeId);
            ContactChange[] copy = null;
            
            copy = new ContactChange[original.length];
            
            for (int i = 0; i < original.length; i++) {
                copy[i] = copyContactChange(original[i]);
            }
             
            return copy;
        }

        @Override
        public void updateNativeContact(ContactChange[] contact) {

            // get the original contact
            final ContactChange[] originalContact = getContact(contact[0].getNabContactId());
            final ArrayList<ContactChange> contactArrayList = new ArrayList<ContactChange>();

            // put it in an ArrayList so we can easily manipulate it 
            for (int i = 0; i < originalContact.length; i++) {
                
                contactArrayList.add(originalContact[i]);
            }
            
            // apply the updates
            for (int i = 0; i < contact.length; i++) {
                
                final ContactChange change = copyContactChange(contact[i]);
                final int type = change.getType(); 
                switch(type) {
                    
                    case ContactChange.TYPE_ADD_DETAIL:
                        change.setInternalDetailId(mLocalDetailId++);
                        change.setType(ContactChange.TYPE_UNKNOWN);
                        contactArrayList.add(change);
                        break;
                    case ContactChange.TYPE_DELETE_DETAIL:
                    case ContactChange.TYPE_UPDATE_DETAIL:
                        for (int j = 0; j < contactArrayList.size(); j++) {
                            
                            final ContactChange changeInList = contactArrayList.get(j);
                            if (changeInList.getNabDetailId() == change.getNabDetailId()) {
                                if (type == ContactChange.TYPE_DELETE_DETAIL) {
                                    contactArrayList.remove(j);
                                } else {
                                    change.setType(ContactChange.TYPE_UNKNOWN);
                                    contactArrayList.set(j, change);
                                }
                                break;
                            }
                        }
                        break;
                }
            }
            
            // set the updated contact back
            final ContactChange[] newContactChanges = new ContactChange[contactArrayList.size()];
            contactArrayList.toArray(newContactChanges);
            
            mPeopleContacts.put(newContactChanges[0].getNabContactId(), newContactChanges);
        }
    }
    
    /**
     * Mocks up the NativeContactsApi.
     */
    public static class NativeContactsApiMockup extends NativeContactsApi {
        
        /**
         * A Hashtable of contacts where the key is the contact id.
         */
        private Hashtable<Long, ContactChange[]> mNativeContacts = new Hashtable<Long, ContactChange[]>();
        
        /**
         * An Hashtable of accounts associated to an array of contact ids.
         */
        private Hashtable<Account, ArrayList<Long>> mAccounts = new Hashtable<Account, ArrayList<Long>>();
        
        /**
         * The counter used to generate new contact ids.
         */
        private int mNabId = 1;
        
        /**
         * The counter used to generate new detail ids.
         */
        private int mNabDetailId = 1;

        public int getAndIncNabDetailId() {
            
            return mNabDetailId++;
        }
        
        @Override
        public ContactChange[] getContact(long nabContactId) {

            // return a copy, the NativeImporter may mess with it
            final ContactChange[] original = mNativeContacts.get(nabContactId);
            ContactChange[] copy = null;
            
            if (original != null) {
                copy = new ContactChange[original.length];
                
                for (int i = 0; i < original.length; i++) {
                    copy[i] = copyContactChange(original[i]);
                }
            }
             
            return copy;
        }

        @Override
        public long[] getContactIds(Account account) {
            
            if (account == null) {
                
                // return all the ids
                if (mNativeContacts.size() > 0) {
                    
                    final Enumeration<ContactChange[]> e = mNativeContacts.elements();
                    final long[] ids = new long[mNativeContacts.size()];
                    int idIndex = 0;
                    
                    while (e.hasMoreElements()) {
                        
                        final ContactChange[] contact = e.nextElement();
                        ids[idIndex++] = contact[0].getNabContactId();
                    }
                    
                    Arrays.sort(ids);
                    
                    return ids;
                } else {
                    
                    return null;
                } 
            } else {
                
                // return the ids depending on the account
                final ArrayList<Long> idsArray = mAccounts.get(account);
                
                if (idsArray != null) {
                    
                    final long[] ids = new long[idsArray.size()];
                    for (int i = 0; i < ids.length; i++) {
                        
                        ids[i] = idsArray.get(i);
                    }
                    
                    Arrays.sort(ids);
                    
                    return ids;
                }
            }
            
            return null;
        }
        
        /**
         * 
         */
        public void feedAccount(Account account) {
            
            mAccounts.put(account, new ArrayList<Long>());
        }
        
        /**
         * 
         * @param nativeId
         * @param newContactChange
         */
        public void setContact(long nativeId, ContactChange[] newContactChange) {
            
            mNativeContacts.put(nativeId, newContactChange);
        }
        
        @Override
        public ContactChange[] addContact(Account account, ContactChange[] ccList) {
            
            // set the native ids
            for (int i = 0; i < ccList.length; i++) {
                
                ccList[i].setNabContactId(mNabId);
                ccList[i].setNabDetailId(mNabDetailId++);
            }
            
            mNabId++;
            
            mNativeContacts.put(ccList[0].getNabContactId(), ccList);
            
            if (account != null) {
                // add the contact id in the account as well
                final ArrayList<Long> ids = mAccounts.get(account);
                ids.add(ccList[0].getNabContactId());
            }
            
            // we have to return an array of native ids
            ContactChange[] copy = null;
            
            copy = new ContactChange[ccList.length + 1];
            
            copy[0] = copyContactChange(ccList[0]);
            
            for (int i = 0; i < ccList.length; i++) {
                copy[i+1] = copyContactChange(ccList[i]);
            }
            
            return copy;
        }

        @Override
        public boolean addPeopleAccount(String username) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Account[] getAccounts() {
            
            if (mAccounts.size() == 0) {
                
                return null;
            } else {
                
                int index = 0;
                Account[] accounts = new Account[mAccounts.size()];
                
                Enumeration<Account> enumeration = mAccounts.keys();
                
                while (enumeration.hasMoreElements()) {
                    accounts[index++] = enumeration.nextElement();
                }
                    
                return accounts;
            }
        }
        
        

        @Override
		public Account[] getAccountsByType(int type) {
			switch (type){
			    case GOOGLE_ACCOUNT_TYPE:
			        return filterAccountsByType(getAccounts(), GOOGLE_ACCOUNT_TYPE_STRING);
			        
			    case PEOPLE_ACCOUNT_TYPE:
                    return filterAccountsByType(getAccounts(), PEOPLE_ACCOUNT_TYPE_STRING);
                    
			    default:
                    return null;
			}
            
		}

		@Override
        protected void initialize() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isPeopleAccountCreated() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void registerObserver(ContactsObserver observer) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void removeContact(long nabContactId) {
            
            final Account[] accounts = getAccounts();
            
            if (accounts != null) {
                
                for (int i = 0; i < accounts.length; i++) {
                    final ArrayList<Long> ids = mAccounts.get(accounts[i]); 
                    ids.remove(nabContactId);
                }
            }
            
            mNativeContacts.remove(nabContactId);
        }

        @Override
        public void removePeopleAccount() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void unregisterObserver() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public ContactChange[] updateContact(ContactChange[] ccList) {
            
            // get the original contact
            final ContactChange[] originalContact = getContact(ccList[0].getNabContactId());
            final ArrayList<ContactChange> contactArrayList = new ArrayList<ContactChange>();

            // put it in an ArrayList so we can easily manipulate it 
            for (int i = 0; i < originalContact.length; i++) {
                
                contactArrayList.add(originalContact[i]);
            }
            
            final ContactChange[] returnedChanges = new ContactChange[ccList.length];
            
            // apply the updates
            for (int i = 0; i < ccList.length; i++) {
                
                final ContactChange change = copyContactChange(ccList[i]);
                final int type = change.getType(); 
                switch(type) {
                    
                    case ContactChange.TYPE_ADD_DETAIL:
                        returnedChanges[i] = copyContactChange(ccList[i]);
                        returnedChanges[i].setNabDetailId(mNabDetailId);
                        returnedChanges[i].setType(ContactChange.TYPE_UPDATE_NAB_DETAIL_ID);
                        change.setInternalDetailId(mNabDetailId++);
                        change.setType(ContactChange.TYPE_UNKNOWN);
                        contactArrayList.add(change);
                        break;
                    case ContactChange.TYPE_DELETE_DETAIL:
                    case ContactChange.TYPE_UPDATE_DETAIL:
                        for (int j = 0; j < contactArrayList.size(); j++) {
                            
                            final ContactChange changeInList = contactArrayList.get(j);
                            if (changeInList.getNabDetailId() == change.getNabDetailId()) {
                                if (type == ContactChange.TYPE_DELETE_DETAIL) {
                                    contactArrayList.remove(j);
                                } else {
                                    change.setType(ContactChange.TYPE_UNKNOWN);
                                    contactArrayList.set(j, change);
                                }
                                break;
                            }
                        }
                        returnedChanges[i] = null;
                        break;
                }
            }
            
            // set the updated contact back
            final ContactChange[] newContactChanges = new ContactChange[contactArrayList.size()];
            contactArrayList.toArray(newContactChanges);
            
            mNativeContacts.put(newContactChanges[0].getNabContactId(), newContactChanges);
            
            return ccList;
        }

        @Override
        public boolean isKeySupported(int key) {

            return true;
        }
    }
    
    /**
     * A second version of PeopleContactsApiMockup only used for testing accounts.
     * 
     * Note: it is a modified version of PeopleContactsApiMockup which adds a trick
     *       on the id used to store the contact in order to easily test the account features
     *       without writing a more sophisticated mockup of PeopleContactsApi.
     */
    public class PeopleContactsApiMockup2 extends PeopleContactsApiMockup {

        public PeopleContactsApiMockup2(DatabaseHelper dbh) {
            super(null);
        }
        
        @Override
        public long[] getNativeContactsIds() {
            
            if (mPeopleContacts.size() > 0) {
                
                final Enumeration<ContactChange[]> e = mPeopleContacts.elements();
                final long[] ids = new long[mPeopleContacts.size()];
                int idIndex = 0;
                
                while (e.hasMoreElements()) {
                    
                    final ContactChange[] contact = e.nextElement();
                    ids[idIndex++] = contact[0].getInternalContactId();
                }
                
                Arrays.sort(ids);
                
                return ids;
            } else {
                
                return null;
            }
            
        }
        
        @Override
        public boolean addNativeContact(ContactChange[] contact) {
            
            // set the local ids
            for (int i = 0; i < contact.length; i++) {
                
                contact[i].setInternalContactId(mLocalContactId);
                contact[i].setInternalDetailId(mLocalDetailId++);
            }
            
            mLocalContactId++;
            
            // add the contact via its local id because on first time import, the native ids are
            // removed on 2.X platform because the export will be done to the People account
            // so they will get different native ids.
            mPeopleContacts.put(contact[0].getInternalContactId(), contact);
            
            return true;
        }
    }
    
    /**
     * Gets the value of a field via reflection.
     * 
     * @param fieldName the name of the field to retrieve
     * @param zClass the class where to look for the field
     * @return the field value
     */
    private static Object getField(String fieldName, Class zClass) {

        try {
            Field fName = zClass.getDeclaredField(fieldName);
            fName.setAccessible(true);
            return fName.get(null);
        } catch (Exception e) {
            Log.e("NativeImporterTest", "getField(), error retrieving the field value... => " + e);
        }

        return null;
    }
    
    /**
     * Alter the given ContactChange value.
     * 
     * @param change the change to modify
     * @param newValue the new value to set to the provided ContactChange
     * @return a new ContactChange with the modified value
     */
    public static ContactChange alterContactChangeValue(ContactChange change, String newValue) {
        
        final ContactChange alteredContactChange = new ContactChange(change.getKey(), newValue, change.getFlags());
        alteredContactChange.setBackendContactId(change.getBackendContactId());
        alteredContactChange.setBackendDetailId(change.getBackendDetailId());
        alteredContactChange.setInternalContactId(change.getInternalContactId());
        alteredContactChange.setInternalDetailId(change.getInternalDetailId());
        alteredContactChange.setNabContactId(change.getNabContactId());
        alteredContactChange.setNabDetailId(change.getNabDetailId());
        
        return alteredContactChange;
    }
    
    /**
     * Copies a ContactChange.
     * 
     * @param change the change to copy
     * @return a deep copy of the ContactChange 
     */
    public static ContactChange copyContactChange(ContactChange change) {
        
        final ContactChange copy = new ContactChange(change.getKey(), change.getValue(), change.getFlags());
        copy.setBackendContactId(change.getBackendContactId());
        copy.setBackendDetailId(change.getBackendDetailId());
        copy.setInternalContactId(change.getInternalContactId());
        copy.setInternalDetailId(change.getInternalDetailId());
        copy.setNabContactId(change.getNabContactId());
        copy.setNabDetailId(change.getNabDetailId());
        copy.setType(change.getType());
        copy.setDestinations(change.getDestinations());
        
        return copy;
    }
    
    /**
     * Filters the given array of accounts by the given type.
     * 
     * @param accounts the array of accounts to filter 
     * @param type the type of accounts to filter
     * @return an array containing the filtered accounts or null if none
     */
    private static Account[] filterAccountsByType(Account[] accounts, String type) {
        
        if (accounts != null) {
            
            final int length = accounts.length;
            final ArrayList<Account> matchingAccounts = new ArrayList<Account>(length); 
            
            // find the corresponding accounts
            for (int i = 0; i < length; i++) {
                
                final Account account = accounts[i];
                if (type.equals(account.getType())) {
                    
                    matchingAccounts.add(account);
                }
            }
            
            if (matchingAccounts.size() > 0) {
                
                accounts = new Account[matchingAccounts.size()];
                matchingAccounts.toArray(accounts);
                
                return accounts;
            }
        }
        
        return null;
    }
}
