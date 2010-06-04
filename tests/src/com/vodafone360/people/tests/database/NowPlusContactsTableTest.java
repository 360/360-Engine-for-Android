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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.database.DatabaseHelper.ServerIdInfo;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ContactsTable.ContactIdInfo;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

public class NowPlusContactsTableTest extends NowPlusTableTestCase {
	final TestModule mTestModule = new TestModule();
	final static int NUM_OF_CONTACTS = 50;
	private int mTestStep = 0;
	private static String LOG_TAG = "NowPlusContactsTableTest";

	public NowPlusContactsTableTest() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private void startSubTest(String function, String description) {
		Log.i(LOG_TAG, function + " - step " + mTestStep + ": " + description);
		mTestStep++;
	}

	private void createTable() {
		try {
			ContactsTable.create(mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the table: " + e);
		}
	}

	@SmallTest
	public void testCreate() {
		Log.i(LOG_TAG, "***** EXECUTING testCreate *****");
		final String fnName = "testCreate";
		mTestStep = 1;

		startSubTest(fnName, "Creating table");
		createTable();

		Log.i(LOG_TAG, "*************************************");
		Log.i(LOG_TAG, "testCreate has completed successfully");
		Log.i(LOG_TAG, "**************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
	public void testAddFetchContact() {
		final String fnName = "testAddContact";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Adds and fetches a contact from the contacts table, validating all the way");

		startSubTest(fnName, "Add Contact before creating a table");
		Contact contact = mTestModule.createDummyContactData();
		ServiceStatus status = ContactsTable.addContact(contact, mTestDatabase
				.getReadableDatabase());
		assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);
		startSubTest(fnName, "Fetch Contact before creating a table");
		status = ContactsTable.fetchContact(-1L, contact, mTestDatabase
				.getReadableDatabase());
		assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Add Contact");
		status = ContactsTable.addContact(contact, mTestDatabase
				.getReadableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);

		startSubTest(fnName, "Fetch added contact");
		Contact fetchedContact = new Contact();
		status = ContactsTable.fetchContact(contact.localContactID,
				fetchedContact, mTestDatabase.getReadableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);
		assertTrue(TestModule.doContactsFieldsMatch(contact, fetchedContact));

		startSubTest(fnName, "Fetch contact with wrong id");
		status = ContactsTable.fetchContact(-1L, fetchedContact, mTestDatabase
				.getReadableDatabase());
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);

		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
	public void testDeleteContact() {
		final String fnName = "testDeleteContact";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Deltes a contact from the contacts table, validating all the way");
		// Try to delete contact before creating a table
		SQLiteDatabase db = mTestDatabase.getWritableDatabase();
		ServiceStatus status = ContactsTable.deleteContact(-1L, db);
		assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Add Contact");
		Contact contact = mTestModule.createDummyContactData();
		status = ContactsTable.addContact(contact, mTestDatabase
				.getReadableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);

		status = ContactsTable.deleteContact(contact.localContactID - 1, db);
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);

		status = ContactsTable.deleteContact(contact.localContactID, db);
		assertEquals(ServiceStatus.SUCCESS, status);

		startSubTest(fnName, "Fetch deleted contact");
		Contact fetchedContact = new Contact();
		status = ContactsTable.fetchContact(contact.localContactID,
				fetchedContact, db);
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);

		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");

	}

	@MediumTest
	public void testModifyContact() {
		final String fnName = "testModifyContact";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Modifies a contact in the contacts table, validating all the way");
		// Try to modify contact before creating a table
		SQLiteDatabase db = mTestDatabase.getWritableDatabase();
		Contact contact = mTestModule.createDummyContactData();
		ServiceStatus status = ContactsTable.modifyContact(contact, db);
		assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

		startSubTest(fnName, "Creating table");
		createTable();

		// Try to modify contact before adding to a table
		status = ContactsTable.modifyContact(contact, db);
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);

		startSubTest(fnName, "Add Contact");

		status = ContactsTable.addContact(contact, mTestDatabase
				.getReadableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);

		Contact modifiedContact = mTestModule.createDummyContactData();
		modifiedContact.localContactID = contact.localContactID;

		status = ContactsTable.modifyContact(modifiedContact, db);
		assertEquals(ServiceStatus.SUCCESS, status);

		startSubTest(fnName, "Fetch modified contact");
		Contact fetchedContact = new Contact();
		status = ContactsTable.fetchContact(modifiedContact.localContactID,
				fetchedContact, db);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertTrue(TestModule.doContactsFieldsMatch(modifiedContact,
				fetchedContact));

		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");

	}

	@MediumTest
	public void testValidateContactId() {
		final String fnName = "testValidateContactId";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates a contact id");
		SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getWritableDatabase();

		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Add Contact");

		Contact contact = mTestModule.createDummyContactData();
		Long serverId = TestModule.generateRandomLong();
		ServiceStatus status = ContactsTable.addContact(contact, readableDb);
		assertEquals(ServiceStatus.SUCCESS, status);

		assertTrue(ContactsTable.modifyContactServerId(contact.localContactID,
				serverId, contact.userID, writableDb));

		ContactIdInfo idInfo = ContactsTable.validateContactId(
				contact.localContactID, readableDb);
		assertEquals(serverId, idInfo.serverId);
		assertTrue(contact.localContactID == idInfo.localId);

		Long fetchedServerId = ContactsTable.fetchServerId(
				contact.localContactID, readableDb);
		assertEquals(serverId, fetchedServerId);

		SQLiteStatement statement = ContactsTable.fetchLocalFromServerIdStatement(readableDb);
		assertTrue(statement != null);
		
		Long localId = ContactsTable.fetchLocalFromServerId(serverId,
				statement);
		assertEquals(Long.valueOf(idInfo.localId), localId);

		localId = ContactsTable.fetchLocalFromServerId(serverId,
				null);
		assertEquals(localId, null);

		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");

	}

	@MediumTest
	public void testServerSyncMethods() {
		final String fnName = "testServerSyncMethods";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates server sync methods");
		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

		startSubTest(fnName, "Creating table");
		
		createTable();
		// add contacts and populate contactServerIdList
		List<ServerIdInfo> contactServerIdList = new ArrayList<ServerIdInfo>();
		final long contactIdBase = TestModule.generateRandomLong();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			Contact c = mTestModule.createDummyContactData();
			if (i == 2) {
				// Add duplicate server ID in database
				c.contactID = contactIdBase;
			}
			c.userID = TestModule.generateRandomLong();
			ServiceStatus status = ContactsTable.addContact(c, writeableDb);
			assertEquals(ServiceStatus.SUCCESS, status);
			ServerIdInfo serverInfo = new ServerIdInfo();
			serverInfo.localId = c.localContactID;
			serverInfo.serverId = contactIdBase + i;
			serverInfo.userId = c.userID;
			contactServerIdList.add(serverInfo);
		}

		// Add duplicate server ID in list from server
		Contact duplicateContact = mTestModule.createDummyContactData();
		duplicateContact.userID = TestModule.generateRandomLong();
		ServiceStatus status = ContactsTable.addContact(duplicateContact, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);

		ServerIdInfo serverInfo = new ServerIdInfo();
		serverInfo.localId = duplicateContact.localContactID;
		serverInfo.serverId = contactIdBase + 1;
		serverInfo.userId = duplicateContact.userID;
		contactServerIdList.add(serverInfo);

		List<ContactIdInfo> dupList = new ArrayList<ContactIdInfo>();
		status = ContactsTable.syncSetServerIds(contactServerIdList, dupList,
				writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		// fetch server ids
		ArrayList<Long> serverIds = new ArrayList<Long>();
		status = ContactsTable.fetchContactServerIdList(serverIds, readableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		// validate if lists have the same sizes
		assertEquals(2, dupList.size());
		assertEquals(contactServerIdList.size() - 2, serverIds.size());

		final ListIterator<Long> serverIdsIt = serverIds.listIterator();
		assertEquals(Long.valueOf(dupList.get(0).localId), contactServerIdList.get(0).localId);
		assertEquals(contactServerIdList.get(0).serverId, dupList.get(0).serverId);
		assertEquals(duplicateContact.localContactID, Long.valueOf(dupList.get(1).localId));
		assertEquals(Long.valueOf(contactIdBase + 1), dupList.get(1).serverId);
		
		for (int i = 1; i < contactServerIdList.size() - 1; i++) {
			Long actServerId = serverIdsIt.next();
			assertEquals(contactServerIdList.get(i).serverId, actServerId);
		}

		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
	public void testFetchContactFormNativeId() {
		final String fnName = "testFetchContactFormNativeId";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "tests fetchContactFormNativeId a contact id");
		SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();

		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Add Contact");

		Contact contact = mTestModule.createDummyContactData();
		contact.userID = TestModule.generateRandomLong();
		contact.nativeContactId = TestModule.generateRandomInt();
		Long serverId = TestModule.generateRandomLong();
		ServiceStatus status = ContactsTable.addContact(contact, writableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		assertTrue(ContactsTable.modifyContactServerId(contact.localContactID,
				serverId, contact.userID, writableDb));

		ContactIdInfo idInfo = ContactsTable.fetchContactIdFromNative(
				contact.nativeContactId, writableDb);
		assertTrue(idInfo.localId == contact.localContactID);
		assertEquals(serverId, idInfo.serverId);
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");
	}
	
	@MediumTest
	public void testSyncSetNativeIds() {
		final String fnName = "testSyncSetNativeIds";
		mTestStep = 1;
		
		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
		
		// add contacts and populate contactServerIdList
		ServiceStatus status;
		List<ContactIdInfo> contactIdList = new ArrayList<ContactIdInfo>();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			Contact c = mTestModule.createDummyContactData();
			c.userID = TestModule.generateRandomLong();
			status = ContactsTable.addContact(c, writableDb);
			assertEquals(ServiceStatus.SUCCESS, status);
			ContactIdInfo contactInfo = new ContactIdInfo();
			contactInfo.localId = c.localContactID;
			contactInfo.nativeId = TestModule.generateRandomInt(); 
			contactIdList.add(contactInfo);
		}
		
		status = ContactsTable.syncSetNativeIds(contactIdList, writableDb); 
		assertEquals(ServiceStatus.SUCCESS, status);
		
		for (ContactIdInfo contactIdInfo : contactIdList) {
			Contact fetchedContact = new Contact();
			status = ContactsTable.fetchContact(contactIdInfo.localId,
					fetchedContact, readableDb);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertEquals(contactIdInfo.nativeId, fetchedContact.nativeContactId);
		}
	}
}
