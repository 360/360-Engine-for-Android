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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ContactsTable.ContactIdInfo;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.contactsync.PeopleContactsApi;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;

public class PeopleContactsApiTest extends AndroidTestCase {

    /**
     * The people database name.
     */
    private final String DATABASE_NAME = "peopletest.db";
    
    /**
     * The DatabaseHelper used to create the people database.
     */
    private DatabaseHelper mDatabaseHelper = null;
    
    @Override
    protected void setUp() throws Exception {

        super.setUp();
        createDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        
        clearDatabase(mDatabaseHelper);
        mDatabaseHelper = null;
        
        super.tearDown();
    }
    
    /**
     * Creates the database with all the tables.
     */
    private void createDatabase() {

        mDatabaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME);
    }
    
    /**
     * Clears the people database.
     */
    private void clearDatabase(DatabaseHelper dbh) {

        dbh.close();
        getContext().deleteDatabase(DATABASE_NAME);
    }

    /**
     * Tests the constructor.
     */
    public void testConstructor() {
        
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
    }
    
    /**
     * Tests the getNativeContactsIds() method.
     */
    public void testGetNativeContactsIds() {
        
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
    }
    
    /**
     * Tests the addNativeContact() method.
     */
    public void testAddNativeContact() {
        
        final long NATIVE_ID = 15;
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
        
        // let's add a contact
        final ContactChange[] contact = ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, NATIVE_ID);
        assertTrue(pca.addNativeContact(contact));
        
        // check that it exists
        final long[] ids = pca.getNativeContactsIds();
        assertEquals(1, ids.length);
        assertEquals(NATIVE_ID, ids[0]);
    }
    
    /**
     * Tests the deleteNativeContact() method.
     */
    public void testDeleteNativeContact() {
        
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // check invalid id
        assertFalse(pca.deleteNativeContact(-1, false));
        
        // check non existing id
        assertFalse(pca.deleteNativeContact(10, false));
    }
    
    /**
     * Tests the getContact() method.
     */
    public void testGetContact() {
        
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any contact
        assertNull(pca.getContact(10));
    }
    
    /**
     * Tests the methods sequence addNativeContact() then deleteNativeContact().
     */
    public void testAddDeleteNativeContact() {
        
        final long NATIVE_ID = 15;
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
        
        // let's add a contact
        final ContactChange[] contact = ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, NATIVE_ID);
        assertTrue(pca.addNativeContact(contact));
        
        // check that it exists
        long[] ids = pca.getNativeContactsIds();
        assertEquals(1, ids.length);
        assertEquals(NATIVE_ID, ids[0]);
        
        // now let's remove it
        assertTrue(pca.deleteNativeContact(NATIVE_ID, false));
        
        // check that it no longer exists
        ids = pca.getNativeContactsIds();
        assertNull(ids);
    }
    
    /**
     * Tests the methods sequence addNativeContact() then getNativeContact().
     */
    @Suppress
    public void testAddGetNativeContact() {
        
        final long NATIVE_ID = 15;
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
        
        // let's add a contact
        ContactChange[] contact = filterContactChanges(ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, NATIVE_ID));
        assertTrue(pca.addNativeContact(contact));
        
        // check that it exists
        long[] ids = pca.getNativeContactsIds();
        assertEquals(1, ids.length);
        assertEquals(NATIVE_ID, ids[0]);
        
        // get the contact back
        final ContactChange[] savedContact = pca.getContact(NATIVE_ID);
        assertNotNull(savedContact);
        
        // compare with the original one
        assertTrue(ContactChangeHelper.areChangeListsEqual(contact, savedContact, false));
    }
    
    /**
     * Tests the getNativeContactsIds() method when a merge has to be performed.
     */
    public void testGetNativeContactsIdsMerge() {
        
        final long[] NATIVE_IDS = { 10, 85, 12, 103, 44, 38 };
        final long[] DELETED_IDS = { NATIVE_IDS[0], NATIVE_IDS[5], NATIVE_IDS[3] };
        
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
        
        // let's add the contacts
        for (int i = 0; i < NATIVE_IDS.length; i++) {
            
            ContactChange[] contact = filterContactChanges(ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, NATIVE_IDS[i]));
            assertTrue(pca.addNativeContact(contact));
        }
        
        // check that they exist and in the right order (ascending)
        long[] ids = pca.getNativeContactsIds();
        assertEquals(NATIVE_IDS.length, ids.length);
        assertTrue(checkExistAndAscendingOrder(NATIVE_IDS, ids));
        
        // delete them from CAB as it was coming from backend or user
        // this has to be done without PeopleContactsApi wrapper as it does not support
        // operations for Contacts not related to the native address book
        List<ContactIdInfo> ciiList = new ArrayList<ContactIdInfo>(DELETED_IDS.length);
        final SQLiteDatabase readableDb = mDatabaseHelper.getReadableDatabase();
        
        try {
            
            final SQLiteStatement nativeToLocalId = ContactsTable.fetchLocalFromNativeIdStatement(readableDb);
            for (int i = 0; i < DELETED_IDS.length; i++) {

                final ContactIdInfo cii = new ContactIdInfo();
                cii.localId = ContactsTable.fetchLocalFromNativeId((int)DELETED_IDS[i], nativeToLocalId);
                cii.nativeId = (int)DELETED_IDS[i];
                ciiList.add(cii);
            }
        }
        finally {
            
            readableDb.close();
        }

        mDatabaseHelper.syncDeleteContactList(ciiList, false, true);
        
        // check the returned list of ids which should be similar as before
        ids = pca.getNativeContactsIds();
        assertEquals(NATIVE_IDS.length, ids.length);
        assertTrue(checkExistAndAscendingOrder(NATIVE_IDS, ids));
        
        // check that the deleted ids have the deleted flag
        for (int i = 0; i < DELETED_IDS.length; i++) {
            
            final ContactChange[] cc = pca.getContact(DELETED_IDS[i]);
            assertEquals(cc[0].getType(), ContactChange.TYPE_DELETE_CONTACT);
        }
    }
    
    /**
     * Tests the updateNativeContact() and getContact() methods.
     */
    @Suppress
    public void testUpdateGetNativeContact() {
        
        final long NATIVE_ID = 15;
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
        
        // let's add a contact
        ContactChange[] contact = filterContactChanges(ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, NATIVE_ID));
        assertTrue(pca.addNativeContact(contact));
        
        // check that it exists
        long[] ids = pca.getNativeContactsIds();
        assertEquals(1, ids.length);
        assertEquals(NATIVE_ID, ids[0]);
        
        // perform some updates to the contact
        final ContactChange[] updatedDetails  = ContactChangeHelper.randomContactUpdate(contact);
        pca.updateNativeContact(updatedDetails);
        
        // get the contact back
        final ContactChange[] savedContact = pca.getContact(NATIVE_ID);
        assertNotNull(savedContact);
        
        // check that the needed updates have been performed
        checkContactUpdates(contact, updatedDetails, savedContact);
    }
    
    /**
     * Tests the updateNativeContact() method when a detail is deleted outside.
     */
    public void testUpdateGetNativeContactWithDelete() {
        
        final long NATIVE_ID = 15;
        PeopleContactsApi pca = new PeopleContactsApi(mDatabaseHelper);
        
        // the database is empty, it shall not return any ids
        assertNull(pca.getNativeContactsIds());
        
        // let's add a contact
        ContactChange[] contact = filterContactChanges(ContactChangeHelper.randomContact(ContactChange.INVALID_ID, ContactChange.INVALID_ID, NATIVE_ID));
        assertTrue(pca.addNativeContact(contact));
        
        // check that it exists
        long[] ids = pca.getNativeContactsIds();
        assertEquals(1, ids.length);
        assertEquals(NATIVE_ID, ids[0]);
        
        // get the contact back
        ContactChange[] savedContact = pca.getContact(NATIVE_ID);
        assertNotNull(savedContact);
        
        // let's add a detail
        ContactChange addedDetail = new ContactChange(ContactChange.KEY_VCARD_PHONE, "+3300000", ContactChange.FLAG_HOME);
        addedDetail.setNabContactId(NATIVE_ID);
        addedDetail.setType(ContactChange.TYPE_ADD_DETAIL);
        addedDetail.setInternalContactId(savedContact[0].getInternalContactId());
        ContactChange[] updates = { addedDetail };
        pca.updateNativeContact(updates);
        
        // get the contact back
        savedContact = pca.getContact(NATIVE_ID);
        assertNotNull(savedContact);
        assertEquals(contact.length + 1, savedContact.length);
        
        // find the localId of the detail to delete
        int index = findContactChangeIndex(savedContact, addedDetail);
        assertTrue(index != -1);
        addedDetail.setInternalDetailId(savedContact[index].getInternalDetailId());
        
        // remove the detail as if coming from user or server (i.e. not yet synced to native)
        ArrayList<ContactDetail> detailList = new ArrayList<ContactDetail>(1);
        detailList.add(mDatabaseHelper.convertContactChange(addedDetail));
        mDatabaseHelper.syncDeleteContactDetailList(detailList, false, true);
        
        // get the contact back
        savedContact = pca.getContact(NATIVE_ID);
        assertNotNull(savedContact);
        // the deleted detail shall be given
        assertEquals(contact.length + 1, savedContact.length);
        
        // check that one contact has the deleted flag
        int deletedIndex = -1;
        int deletedCount = 0;
        for (int i = 0; i < savedContact.length; i++) {
            
            if (savedContact[i].getType() == ContactChange.TYPE_DELETE_DETAIL) {
                
                deletedIndex = i;
                deletedCount++;
            }
        }
        
        // there shall be only one deleted detail
        assertEquals(1, deletedCount);
        assertEquals(addedDetail.getInternalDetailId(), savedContact[deletedIndex].getInternalDetailId());
    }
    
    /**
     * Removes the nickname ContactChange if any as we don't support it currently.
     * 
     * @param changes the array of ContactChange to filter
     * @return the filtered array of ContactChange
     */
    private ContactChange[] filterContactChanges(ContactChange[] changes) {
        
        ArrayList<ContactChange> filtered = new ArrayList<ContactChange>(changes.length); 
        
        for (int i = 0; i < changes.length; i++) {
            
            final ContactChange cc = changes[i];
            filterFlags(cc);
            if (cc.getKey() != ContactChange.KEY_VCARD_NICKNAME) {
                filtered.add(cc);
            }
        }
        
        if (filtered.size() == changes.length) {
            
            return changes;
        } else {
            
            return filtered.toArray(new ContactChange[filtered.size()]);
        }
    }
    
    /**
     * Removes unsupported flags combinations from a ContactChange.
     * 
     * @param change the ContactChange to filter
     */
    private void filterFlags(ContactChange change) {
        
        if ((change.getFlags() & ContactChange.FLAGS_HOME_CELL) == ContactChange.FLAGS_HOME_CELL) {
            
            change.setFlags(change.getFlags() ^ ContactChange.FLAG_HOME);
        } else if ((change.getFlags() & ContactChange.FLAGS_HOME_FAX) == ContactChange.FLAGS_HOME_CELL) {
            
            change.setFlags(change.getFlags() ^ ContactChange.FLAG_HOME);
        } else if ((change.getFlags() & ContactChange.FLAGS_WORK_CELL) == ContactChange.FLAGS_HOME_CELL) {
            
            change.setFlags(change.getFlags() ^ ContactChange.FLAG_CELL);
        } else if ((change.getFlags() & ContactChange.FLAGS_WORK_FAX) == ContactChange.FLAGS_HOME_CELL) {
            
            change.setFlags(change.getFlags() ^ ContactChange.FLAG_WORK);
        } 
    }
    
    /**
     * Verifies that the array to check contains exactly the original ids and that it is sorted in ascending order.
     * 
     * @param originalIds the array of ids that need to be in the other array
     * @param idsToCheck the array of ids to check 
     * @return true if correct, false if not
     */
    private boolean checkExistAndAscendingOrder(long [] originalIds, long[] idsToCheck) {
        
        Arrays.sort(originalIds);
        return Arrays.equals(originalIds, idsToCheck);
    }
    
    /**
     * Checks that the updated ContactChange array matches the original ContactChange
     * array with the performed ContactChange updates.
     *  
     * @param original the original ContactChange array
     * @param updates the array of ContacChange updates that have been applied
     * @param updatedContact the array of ContactChange representing the original contact after having performed the updates
     * @return true if the new ContactChange reflect the updates, false if not
     */
    private void checkContactUpdates(ContactChange[] original, ContactChange[] updates, ContactChange[] updatedContact) {
        
        int addedCount = 0;
        int deletedCount = 0;
        int updatedCount = 0;
        
        int index;
        for (int i = 0; i < updates.length; i++) {
            
            final ContactChange cc = updates[i];
            switch(cc.getType()) {
                case ContactChange.TYPE_ADD_DETAIL:
                    index = findContactChangeIndex(updatedContact, cc);
                    assertTrue(index != -1);
                    addedCount++;
                    break;
                case ContactChange.TYPE_UPDATE_DETAIL:
                    index = findContactChangeIndex(updatedContact, cc);
                    assertTrue(index != -1);
                    updatedCount++;
                    break;
                case ContactChange.TYPE_DELETE_DETAIL:
                    index = findContactChangeIndex(updatedContact, cc);
                    assertTrue(index == -1);
                    deletedCount++;
                    break;
            }
        }
        
        assertEquals(original.length + addedCount - deletedCount, updatedContact.length);
    }
    
    /**
     * Finds the index of a ContactChange within a ContactChange array.
     * 
     * @param array the ContactChange array where to search
     * @param changeToFind the ContactChange to find
     * @return the index of the equivalent ContactChange found, -1 if not found
     */
    private int findContactChangeIndex(ContactChange[] array, ContactChange changeToFind) {
        
        for (int i = 0; i < array.length; i++) {
            
            if (ContactChangeHelper.areChangesEqual(array[i], changeToFind, false)) {
                return i;
            }
        }
        
        return -1;
    }
}
