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

package com.vodafone360.people.tests.database;

import java.util.Hashtable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.presence.User;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

/**
 * This class performs ContactSummaryTable tests.
 */
public class NowPlusContactSummaryTest extends ApplicationTestCase<MainApplication> {

    private static String LOG_TAG = "NowPlusContactSummaryTest";

    /**
     * Helper module to generate test content.
     */
    final TestModule mTestModule = new TestModule();

    /**
     * A simple test database.
     */
    private TestDatabase mTestDatabase;

    /**
     * Constructor.
     */
    public NowPlusContactSummaryTest() {
        super(MainApplication.class);
    }

    @Override
    protected void setUp() throws Exception {

        super.setUp();

        createApplication();

        mTestDatabase = new TestDatabase(getContext());
    }

    @Override
    protected void tearDown() throws Exception {

        mTestDatabase.close();
        getContext().deleteDatabase(TestDatabase.DATA_BASE_NAME);

        // make sure to call it at the end of the method!
        super.tearDown();
    }

    /**
     * Tests the ContactSummaryTable creation.
     */
    @SmallTest
    public void testContactSummaryTableCreation() {

        Log.i(LOG_TAG, "***** EXECUTING testContactSummaryTableCreation *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());
    }

    /**
     * Tests adding a contact to the contact summary table.
     */
    @SmallTest
    public void testAddingContactSummary() {

        Log.i(LOG_TAG, "***** EXECUTING testAddingContactSummary *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());
    }

    /**
     * Tests fetching a contact summary.
     */
    @SmallTest
    public void testFetchingContactSummary() {

        Log.i(LOG_TAG, "***** EXECUTING testFetchingContactSummary *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Fetching a ContactSummary from ContactSummaryTable");
        final ContactSummary contactSummary = new ContactSummary();
        final ServiceStatus serviceStatus = ContactSummaryTable.fetchSummaryItem(
                contact.localContactID, contactSummary, mTestDatabase.getReadableDatabase());

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        compareContactWithContactSummary(contact, contactSummary);
    }

    public void testSetAllUsersOffline() {
        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Fetching a ContactSummary from ContactSummaryTable");
        final ContactSummary contactSummary = new ContactSummary();
        final ServiceStatus serviceStatus = ContactSummaryTable.fetchSummaryItem(
                contact.localContactID, contactSummary, mTestDatabase.getReadableDatabase());

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        compareContactWithContactSummary(contact, contactSummary);

        // create a new user
        Hashtable<String, String> status = new Hashtable<String, String>();
        status.put("google", "online");
        status.put("microsoft", "online");
        status.put("mobile", "online");
        status.put("pc", "online");

        User user = new User("any", status);
        user.setLocalContactId(contactSummary.localContactID);
        // set him online
        assertEquals(ServiceStatus.SUCCESS, ContactSummaryTable.updateOnlineStatus(user));
        // fetch again

        final ContactSummary contactSummary2 = new ContactSummary();
        assertEquals(ServiceStatus.SUCCESS, ContactSummaryTable.fetchSummaryItem(user
                .getLocalContactId(), contactSummary2, mTestDatabase.getReadableDatabase()));

        // check if he's online
        assertEquals(OnlineStatus.ONLINE, contactSummary2.onlineStatus);

        // set offline
        assertEquals(ServiceStatus.SUCCESS, ContactSummaryTable.setOfflineStatus());
        // fetch again
        final ContactSummary contactSummary3 = new ContactSummary();
        assertEquals(ServiceStatus.SUCCESS, ContactSummaryTable.fetchSummaryItem(user
                .getLocalContactId(), contactSummary3, mTestDatabase.getReadableDatabase()));
        // check if it's offline
        assertEquals(OnlineStatus.OFFLINE, contactSummary3.onlineStatus);
    }

    public void testUpdateOnlineStatus() {
        Log.i(LOG_TAG, "***** EXECUTING testUpdateOnlineStatus() *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Fetching a ContactSummary from ContactSummaryTable");
        final ContactSummary contactSummary = new ContactSummary();
        final ServiceStatus serviceStatus = ContactSummaryTable.fetchSummaryItem(
                contact.localContactID, contactSummary, mTestDatabase.getReadableDatabase());

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        compareContactWithContactSummary(contact, contactSummary);

        // create a new user
        Hashtable<String, String> status = new Hashtable<String, String>();
        status.put("google", "online");
        status.put("microsoft", "online");
        status.put("mobile", "online");
        status.put("pc", "online");

        User user = new User("any", status);
        user.setLocalContactId(contactSummary.localContactID);
        // set him online
        assertEquals(ServiceStatus.SUCCESS, ContactSummaryTable.updateOnlineStatus(user));
        // fetch again

        final ContactSummary contactSummary2 = new ContactSummary();
        assertEquals(ServiceStatus.SUCCESS, ContactSummaryTable.fetchSummaryItem(user
                .getLocalContactId(), contactSummary2, mTestDatabase.getReadableDatabase()));

        // check if he's online
        assertEquals(OnlineStatus.ONLINE, contactSummary2.onlineStatus);
    }

    /**
     * Tests removing a contact summary.
     */
    @SmallTest
    public void testRemovingContactSummary() {

        Log.i(LOG_TAG, "***** EXECUTING testRemovingContactSummary *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());

        Log
                .i(LOG_TAG,
                        "Fetching a ContactSummary from ContactSummaryTable to check that it exists");
        ContactSummary contactSummary = new ContactSummary();
        ServiceStatus serviceStatus = ContactSummaryTable.fetchSummaryItem(contact.localContactID,
                contactSummary, mTestDatabase.getReadableDatabase());

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        Log.i(LOG_TAG, "Delete the contact from ContactSummaryTable");
        serviceStatus = ContactSummaryTable.deleteContact(contact.localContactID, mTestDatabase
                .getWritableDatabase());
        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        Log
                .i(
                        LOG_TAG,
                        "Try to fetching a ContactSummary from ContactSummaryTable to check that it is not possible anymore");
        contactSummary = new ContactSummary();
        serviceStatus = ContactSummaryTable.fetchSummaryItem(contact.localContactID,
                contactSummary, mTestDatabase.getReadableDatabase());

        assertTrue(ServiceStatus.SUCCESS != serviceStatus);
    }

    /**
     * Tests modifying a contact summary.
     */
    @SmallTest
    public void testModifyingingContactSummary() {

        Log.i(LOG_TAG, "***** EXECUTING testModifyingingContactSummary *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Fetching a ContactSummary from ContactSummaryTable");
        ContactSummary contactSummary = new ContactSummary();
        ServiceStatus serviceStatus = ContactSummaryTable.fetchSummaryItem(contact.localContactID,
                contactSummary, mTestDatabase.getReadableDatabase());

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        compareContactWithContactSummary(contact, contactSummary);

        Log.i(LOG_TAG, "Modify a contact");
        final Contact contact2 = copyContact(contact);
        contact2.synctophone = !contact.synctophone;

        serviceStatus = ContactSummaryTable.modifyContact(contact2, mTestDatabase
                .getWritableDatabase());
        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        Log.i(LOG_TAG, "Fetching a ContactSummary from ContactSummaryTable");
        contactSummary = new ContactSummary();
        serviceStatus = ContactSummaryTable.fetchSummaryItem(contact.localContactID,
                contactSummary, mTestDatabase.getReadableDatabase());
        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        // by doing so, we should get back to the original if it was correctly
        // modified
        contactSummary.synctophone = !contactSummary.synctophone;
        compareContactWithContactSummary(contact, contactSummary);
    }

    /**
     * Tests adding a contact detail to an existing contact.
     */
    @SmallTest
    public void testAddingContactDetails() {

        Log.i(LOG_TAG, "***** EXECUTING testAddingContactDetails *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());
        Log.i(LOG_TAG, "Create also a ContactDetailsTable");
        ContactDetailsTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = mTestModule.createDummyContactData();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ContactSummaryTable.addContact(contact, mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact detail to the previous contact");
        final ContactDetail contactDetail = new ContactDetail();
        contactDetail.localContactID = contact.localContactID;
        contactDetail.setEmail("toto@mail.co.uk", ContactDetail.DetailKeyTypes.HOME);
        assertTrue(ContactSummaryTable.updateNameAndStatus(contact, mTestDatabase
                .getWritableDatabase()) == ServiceStatus.SUCCESS);
    }

    /**
     * Tests modifying a contact detail of an existing contact.
     */
    @SmallTest
    public void testModifyingContactDetails() {

        Log.i(LOG_TAG, "***** EXECUTING testModifyingContactDetails *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());
        Log.i(LOG_TAG, "Create also a ContactDetailsTable");
        ContactDetailsTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = new Contact();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ServiceStatus serviceStatus = ContactSummaryTable.addContact(contact, mTestDatabase
                .getWritableDatabase());
        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        Log.i(LOG_TAG, "Add a contact detail to the previous contact");
        ContactDetail contactDetail = new ContactDetail();
        contactDetail.localContactID = contact.localContactID;
        contactDetail.setEmail("toto@mail.co.uk", ContactDetail.DetailKeyTypes.HOME);
        assertTrue(ContactSummaryTable.updateNameAndStatus(contact, mTestDatabase
                .getWritableDatabase()) == ServiceStatus.SUCCESS);

        Log.i(LOG_TAG, "Modify a contact detail to the previous contact");
        contactDetail = new ContactDetail();
        contactDetail.localContactID = contact.localContactID;
        contactDetail.setEmail("toto2@mail.co.uk", ContactDetail.DetailKeyTypes.HOME);
        assertTrue(ContactSummaryTable.updateNameAndStatus(contact, mTestDatabase
                .getWritableDatabase()) == ServiceStatus.SUCCESS);
    }

    /**
     * Tests deleting a contact detail of an existing contact.
     */
    @SmallTest
    public void testDeletingContactDetails() {

        Log.i(LOG_TAG, "***** EXECUTING testDeletingContactDetails *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());
        Log.i(LOG_TAG, "Create also a ContactDetailsTable");
        ContactDetailsTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
        final Contact contact = new Contact();
        contact.localContactID = new Long(10);
        contact.nativeContactId = new Integer(11);
        ServiceStatus serviceStatus = ContactSummaryTable.addContact(contact, mTestDatabase
                .getWritableDatabase());
        assertEquals(ServiceStatus.SUCCESS, serviceStatus);

        Log.i(LOG_TAG, "Add a contact detail to the previous contact");
        ContactDetail contactDetail = new ContactDetail();
        contactDetail.localContactID = contact.localContactID;
        contactDetail.setEmail("toto@mail.co.uk", ContactDetail.DetailKeyTypes.HOME);
        assertTrue(ContactSummaryTable.updateNameAndStatus(contact, mTestDatabase
                .getWritableDatabase()) == ServiceStatus.SUCCESS);

        Log.i(LOG_TAG, "Delete a contact detail from the previous contact");
        assertTrue(ContactSummaryTable.updateNameAndStatus(contact, mTestDatabase
                .getWritableDatabase()) == ServiceStatus.SUCCESS);
    }

    /**
     * Tests fetching native contact IDs.
     */
    @SmallTest
    public void testFetchingNativeContactIDs() {
        Log.i(LOG_TAG, "***** EXECUTING testFetchingNativeContactIDs *****");

        Log.i(LOG_TAG, "Create ContactSummaryTable");
        ContactSummaryTable.create(mTestDatabase.getWritableDatabase());

        Log.i(LOG_TAG, "Add a contacts to ContactSummaryTable");
        ServiceStatus serviceStatus;
        for (int i = 0; i < 100; i++) {
            final Contact contact = mTestModule.createDummyContactData();
            contact.localContactID = new Long(i);
            contact.nativeContactId = new Integer(i + 5);
            serviceStatus = ContactSummaryTable.addContact(contact, mTestDatabase
                    .getWritableDatabase());
            assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        }

        Log.i(LOG_TAG, "Fetching native IDs");
        final java.util.ArrayList<Integer> nativeIDsList = new java.util.ArrayList<Integer>();
        ContactSummaryTable.fetchNativeContactIdList(nativeIDsList, mTestDatabase
                .getReadableDatabase());

        Log.i(LOG_TAG, "Check the native IDs");
        int currentNativeID = 5;
        for (Integer id : nativeIDsList) {
            assertEquals(currentNativeID++, id.intValue());
        }
    }

    // //////////////////////////////
    // HELPER CLASSES AND METHODS //
    // //////////////////////////////

    /**
     * A simple test database.
     */
    private static class TestDatabase extends SQLiteOpenHelper {

        public final static String DATA_BASE_NAME = "TEST_DB";

        public TestDatabase(Context context) {
            super(context, DATA_BASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub

        }
    }

    /**
     * Compares a Contact summary with its original contact.
     * 
     * @param contact the original contact
     * @param contactSummary the contact summary
     */
    private void compareContactWithContactSummary(Contact contact, ContactSummary contactSummary) {

        assertNotNull(contact);
        assertNotNull(contactSummary);

        assertEquals(contact.friendOfMine.booleanValue(), contactSummary.friendOfMine);
        assertEquals(contact.synctophone.booleanValue(), contactSummary.synctophone);
        assertEquals(contact.localContactID, contactSummary.localContactID);
        assertEquals(contact.nativeContactId, contactSummary.nativeContactId);
    }

    /**
     * Creates a "light" copy of a Contact.
     * 
     * @param contact the contact to copy
     * @return the copy of the provided contact
     */
    private Contact copyContact(Contact contact) {
        final Contact newContact = new Contact();

        // using Copy() but seems deprecated, may need to be changed later
        // newContact.Copy(contact);
        newContact.friendOfMine = contact.friendOfMine;
        newContact.synctophone = contact.synctophone;
        newContact.localContactID = contact.localContactID;
        newContact.nativeContactId = contact.nativeContactId;

        return newContact;
    }
}
