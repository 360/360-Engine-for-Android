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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import junit.framework.TestCase;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.contactsync.NativeExporter;
import com.vodafone360.people.engine.contactsync.NativeContactsApi.Account;
import com.vodafone360.people.tests.engine.contactsync.NativeImporterTest.NativeContactsApiMockup;
import com.vodafone360.people.utils.VersionUtils;

public class NativeExporterTest extends TestCase {

    /**
     * Tests multiple exports to native with only new contacts.
     */
    public void testMultipleExportsToNativeWithNewContacts() {
        
        final int NEW_CONTACTS_COUNT = 30;
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts syncable contact on People side
        feedPeopleContactsApiWithNewSyncableContacts(peopleMockup, NEW_CONTACTS_COUNT);
        long[] nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(null, nativeIdsFromPeople); // shall be null because our PeopleContactsApiMockup will add them once synced
        
        long[] localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT, localIds.length);
        
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(null, nativeIds);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT, nativeIdsFromPeople.length);
        
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests multiple exports to native with deleted contacts.
     */
    public void testMultipleExportsToNativeWithDeletedContacts() {
        
        // add contacts on People side and sync them to native side
        final int NEW_CONTACTS_COUNT = 30;
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts syncable contact on People side
        feedPeopleContactsApiWithNewSyncableContacts(peopleMockup, NEW_CONTACTS_COUNT);
        long[] nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(null, nativeIdsFromPeople); // shall be null because our PeopleContactsApiMockup will add them once synced
        
        long[] localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT, localIds.length);
        
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(null, nativeIds);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT, nativeIdsFromPeople.length);
        
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // delete half of contacts on People side
        peopleMockup.setSyncableDeletedContact(1);
        peopleMockup.setSyncableDeletedContact(2);
        peopleMockup.setSyncableDeletedContact(5);
        peopleMockup.setSyncableDeletedContact(8);
        peopleMockup.setSyncableDeletedContact(9);
        peopleMockup.setSyncableDeletedContact(13);
        peopleMockup.setSyncableDeletedContact(18);
        peopleMockup.setSyncableDeletedContact(19);
        peopleMockup.setSyncableDeletedContact(22);
        peopleMockup.setSyncableDeletedContact(23);
        peopleMockup.setSyncableDeletedContact(24);
        peopleMockup.setSyncableDeletedContact(25);
        peopleMockup.setSyncableDeletedContact(26);
        peopleMockup.setSyncableDeletedContact(28);
        peopleMockup.setSyncableDeletedContact(30);
        
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT/2, nativeIdsFromPeople.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT/2, localIds.length);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT/2, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests export to native with added details to a existing contacts.
     */
    public void testExportToNativeWithUpdatedContacts_addedDetails() {
        
        // add contacts on People side and sync them to native side
        final int NEW_CONTACTS_COUNT = 30;
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts syncable contact on People side
        feedPeopleContactsApiWithNewSyncableContacts(peopleMockup, NEW_CONTACTS_COUNT);
        long[] nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(null, nativeIdsFromPeople); // shall be null because our PeopleContactsApiMockup will add them once synced
        
        long[] localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT, localIds.length);
        
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(null, nativeIds);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT, nativeIdsFromPeople.length);
        
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // pick few contacts and add details to them
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 1));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 2));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 5));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 10));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 12));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 18));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 19));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 21));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 23));
        peopleMockup.updateNativeContact(getNewDetails(peopleMockup, 24));
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are equivalent
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests export to native with updated details to existing contacts.
     */
    public void testExportToNativeWithUpdatedContacts_updatedDetails() {
        
        // add contacts on People side and sync them to native side
        final int NEW_CONTACTS_COUNT = 30;
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts syncable contact on People side
        feedPeopleContactsApiWithNewSyncableContacts(peopleMockup, NEW_CONTACTS_COUNT);
        long[] nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(null, nativeIdsFromPeople); // shall be null because our PeopleContactsApiMockup will add them once synced
        
        long[] localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT, localIds.length);
        
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(null, nativeIds);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT, nativeIdsFromPeople.length);
        
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // pick contacts and modify all their details
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 1));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 2));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 5));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 10));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 12));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 18));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 19));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 21));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 23));
        peopleMockup.updateNativeContact(getUpdatedDetails(peopleMockup, 24));
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are equivalent
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests export to native with added details to a existing contacts.
     */
    public void testExportToNativeWithUpdatedContacts_deletedDetails() {
        
        // add contacts on People side and sync them to native side
        final int NEW_CONTACTS_COUNT = 30;
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        // add new contacts syncable contact on People side
        feedPeopleContactsApiWithNewSyncableContacts(peopleMockup, NEW_CONTACTS_COUNT);
        long[] nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(null, nativeIdsFromPeople); // shall be null because our PeopleContactsApiMockup will add them once synced
        
        long[] localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT, localIds.length);
        
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(null, nativeIds);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT, nativeIdsFromPeople.length);
        
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // pick few contacts and add details to them
        ContactChange[][] addedDetails = new ContactChange[3][];
        
        addedDetails[0] = getNewDetails(peopleMockup, 1);
        peopleMockup.updateNativeContact(addedDetails[0]);
        addedDetails[1] = getNewDetails(peopleMockup, 2);
        peopleMockup.updateNativeContact(addedDetails[1]);
        addedDetails[2] = getNewDetails(peopleMockup, 5);
        peopleMockup.updateNativeContact(addedDetails[2]);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are equivalent
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        
        // remove the previously added details
        for (int i = 0; i < addedDetails.length; i++) {
            
            forceDetailsAsDeleteType(addedDetails[i]);
            peopleMockup.updateNativeContact(addedDetails[i]);
        }
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are equivalent
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
    }
    
    /**
     * Tests that the export is performed on the correct account depending on the Android platform.
     */
    public void testExportAccount() {
        
        final int NEW_CONTACTS_COUNT = 30;
        final NativeContactsApiMockup nativeMockup = new NativeContactsApiMockup();
        final PeopleContactsApiMockup peopleMockup = new PeopleContactsApiMockup(null);
        
        nativeMockup.feedAccount(NativeImporterTest.GMAIL_ACCOUNT_1);
        nativeMockup.feedAccount(NativeImporterTest.GMAIL_ACCOUNT_2);
        nativeMockup.feedAccount(NativeImporterTest.THIRD_PARTY_ACCOUNT);
        nativeMockup.feedAccount(NativeImporterTest.PEOPLE_ACCOUNT);
        
        // add new contacts syncable contact on People side
        feedPeopleContactsApiWithNewSyncableContacts(peopleMockup, NEW_CONTACTS_COUNT);
        long[] nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(null, nativeIdsFromPeople); // shall be null because our PeopleContactsApiMockup will add them once synced
        
        long[] localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(NEW_CONTACTS_COUNT, localIds.length);
        
        long[] nativeIds = nativeMockup.getContactIds(null);
        assertEquals(null, nativeIds);
        
        // perform the export to native
        runNativeExporter(nativeMockup, peopleMockup);
        
        // check both sides are updated correctly
        nativeIdsFromPeople = peopleMockup.getNativeContactsIds();
        assertEquals(NEW_CONTACTS_COUNT, nativeIdsFromPeople.length);
        
        nativeIds = nativeMockup.getContactIds(null);
        assertEquals(NEW_CONTACTS_COUNT, nativeIds.length);
        
        localIds = peopleMockup.getNativeSyncableContactIds();
        assertEquals(null, localIds);
        
        if (VersionUtils.is2XPlatform()) {
            
            // on Android 2.X, the contacts shall be present in the People Account
            final Account[] accounts = { NativeImporterTest.PEOPLE_ACCOUNT };
            assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, accounts, peopleMockup));   
        } else {
            // on Android 1.X, the contacts shall be present in the "null" account
            assertTrue(NativeImporterTest.compareNativeAndPeopleContactsList(nativeMockup, null, peopleMockup));
        }
    }
    
    /**
     * Sets the ContactChange.TYPE_DELETE_DETAIL to all the provided ContactChange
     * @param changes the array of ContactChange
     */
    private void forceDetailsAsDeleteType(ContactChange[] changes) {
        
        for (int i = 0; i < changes.length; i++) {
            
            changes[0].setType(ContactChange.TYPE_DELETE_DETAIL);
        }
    }
    
    /**
     * Runs the NativeExporter until it finishes synchronizing.
     * 
     * @param ncam the NativeContactsApiMockup instance
     * @param pcam the PeopleContactsApiMockup instance
     */
    private void runNativeExporter(NativeContactsApiMockup ncam, PeopleContactsApiMockup pcam) {
        
        final NativeExporter nativeExporter = new NativeExporter(pcam, ncam);
        
        // run the NativeExporter until the export to native is over
        while (!nativeExporter.tick());
    }
    
    /**
     * Gets an array for ContatChange updates for the provided Contact
     * @param pcam the PeopleContactsApiMockup instance
     * @param nativeContactId the contact native id
     * @return an array for ContatChange updates
     */
    private ContactChange[] getUpdatedDetails(PeopleContactsApiMockup pcam, long nativeContactId) {
        
        final ContactChange[] contact = pcam.getContact(nativeContactId);
        final ContactChange[] copy = new ContactChange[contact.length];
        
        for (int i = 0; i < contact.length; i++) {
            
            copy[i] = NativeImporterTest.alterContactChangeValue(contact[i], contact[i].getValue() + "x9x");
            copy[i].setType(ContactChange.TYPE_UPDATE_DETAIL);
        }
        
        return copy;
    }
    
    /**
     * Gets an array of ContatChange new details  for the provided Contact
     * @param pcam the PeopleContactsApiMockup instance
     * @param nativeContactId the contact native id
     * @return an array for ContatChange updates
     */
    private ContactChange[] getNewDetails(PeopleContactsApiMockup pcam, long nativeContactId) {
        
        final ContactChange[] contact = pcam.getContact(nativeContactId);

        final long localContactId = contact[0].getInternalContactId();
        final ContactChange[] addedDetails = new ContactChange[2];
        int index = 0;
        
        
        // add extra details
        addedDetails[index] = new ContactChange(ContactChange.KEY_VCARD_EMAIL, "xxxxx@xxxxx.xx", ContactChange.FLAG_WORK);
        addedDetails[index].setNabContactId(nativeContactId);
        addedDetails[index].setInternalContactId(localContactId);
        addedDetails[index].setType(ContactChange.TYPE_ADD_DETAIL);
        index++;
        
        addedDetails[index] = new ContactChange(ContactChange.KEY_VCARD_PHONE, "+9912345678", ContactChange.FLAG_WORK);
        addedDetails[index].setNabContactId(nativeContactId);
        addedDetails[index].setInternalContactId(localContactId);
        addedDetails[index].setType(ContactChange.TYPE_ADD_DETAIL);
        index++;
        
        return addedDetails;
    }
    
    /**
     * Feeds the PeopleContactApiMockup with new Contacts to sync.
     * @param pcam the PeopleContactsApiMockup instance
     * @param contactsCount the number of new contacts
     */
    private void feedPeopleContactsApiWithNewSyncableContacts(PeopleContactsApiMockup pcam, int contactsCount) {
        
        for (int i = 0; i < contactsCount; i++) {
            
            final ContactChange[] contact = ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, ContactChange.INVALID_ID);
            pcam.addSyncableContact(contact);
        }
    }
    
    /**
     * A specifically modified version of the PeopleContactsApiMockup for the NativeExporter tests.
     */
    private static class PeopleContactsApiMockup extends com.vodafone360.people.tests.engine.contactsync.NativeImporterTest.PeopleContactsApiMockup {

        /**
         * Holds all the syncable changes.
         */
        protected Hashtable<Long, ContactChange[]> mPeopleContactsToSync = new Hashtable<Long, ContactChange[]>();
        
        public PeopleContactsApiMockup(DatabaseHelper dbh) {
            super(dbh);
            // TODO Auto-generated constructor stub
        }
        
        public void addSyncableContact(ContactChange[] contact) {
            
            contact[0].setType(ContactChange.TYPE_ADD_CONTACT);
            
            // set the local ids
            for (int i = 0; i < contact.length; i++) {
                
                contact[i].setInternalContactId(mLocalContactId);
                contact[i].setInternalDetailId(mLocalDetailId++);
            }
            
            mLocalContactId++;
            
            // add the contact to the hashtable of contacts to sync
            mPeopleContactsToSync.put(contact[0].getInternalContactId(), contact);
        }
        
        /**
         * Deletes a contact and adds it to the syncable hashtable
         * 
         * @param nativeContactId
         */
        public void setSyncableDeletedContact(long nativeContactId) {
            
            ContactChange[] contact = mPeopleContacts.get(nativeContactId);
            
            if (contact != null) {
                
                mPeopleContacts.remove(nativeContactId);
                final ContactChange[] change = { contact[0] };
                change[0].setType(ContactChange.TYPE_DELETE_CONTACT);
                
                mPeopleContactsToSync.put(change[0].getInternalContactId(), change);
            }
        }
        
        @Override
        public ContactChange[] getNativeSyncableContactChanges(long localId) {

            // return a copy, the NativeExporter may mess with it
            final ContactChange[] original = mPeopleContactsToSync.get(localId);
            ContactChange[] copy = null;
            
            copy = new ContactChange[original.length];
            
            for (int i = 0; i < original.length; i++) {
                copy[i] = NativeImporterTest.copyContactChange(original[i]);
            }
             
            return copy;
        }

        @Override
        public long[] getNativeSyncableContactIds() {

            if (mPeopleContactsToSync.size() > 0) {
                
                final Enumeration<ContactChange[]> e = mPeopleContactsToSync.elements();
                final long[] ids = new long[mPeopleContactsToSync.size()];
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
        public void updateNativeContact(ContactChange[] contact) {
            
            
            super.updateNativeContact(contact);
            
            // add the updates to the syncable hashtable
            mPeopleContactsToSync.put(contact[0].getInternalContactId(), contact);
        }

        @Override
        public boolean syncBackDeletedNativeContact(ContactChange deletedContact) {
            
            mPeopleContactsToSync.remove(deletedContact.getInternalContactId());
            
            return true;
        }

        @Override
        public boolean syncBackNewNativeContact(ContactChange[] contact, ContactChange[] nativeIds) {

            mPeopleContactsToSync.remove(contact[0].getInternalContactId());
            
            // set the native ids
            final long nativeContactId = nativeIds[0].getNabContactId();
            for (int i = 0; i < contact.length; i++) {
                
                contact[i].setNabContactId(nativeContactId);
                contact[i].setNabDetailId(nativeIds[i+1] != null ? nativeIds[i+1].getNabDetailId() : nativeContactId);
            }
            
            mPeopleContacts.put(contact[0].getNabContactId(), contact);
            return true;
        }

        @Override
        public boolean syncBackUpdatedNativeContact(ContactChange[] contact, ContactChange[] nativeIds) {
            
            mPeopleContactsToSync.remove(contact[0].getInternalContactId());
            
            return true;
        }
    }
}
