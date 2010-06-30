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

import android.database.SQLException;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.database.tables.ContactChangeLogTable;
import com.vodafone360.people.database.tables.ContactChangeLogTable.ContactChangeInfo;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.tests.TestModule;

public class NowPlusChangeLogTest extends NowPlusTableTestCase {
	private int mTestStep = 0;
	private static final String LOG_TAG = "NowPlusChangeLogTest";
//	private static final int BULK_TEST_MAX_CONTACTS = 510;
//	private static final int BULK_TEST_MAX_CONTACTS_PER_PAGE = 50;
	
	public NowPlusChangeLogTest() {
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
			ContactChangeLogTable.create(mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the table: " + e);
		}
	}
/*
	private void compareDetails(List<ContactDetail> list1, List<ContactDetail> list2) {
		assertEquals(list1.size(), list2.size());
		for (ContactDetail d1 : list1) {
			boolean done = false;
			for (ContactDetail d2 : list2) {
				if (d1.localDetailID.equals(d2.localDetailID)) {
					assertEquals(d1.localContactID, d2.localContactID);
					assertTrue(DatabaseHelper.doDetailsMatch(d1, d2));
					assertFalse(DatabaseHelper.hasDetailChanged(d1, d2));
					done = true;
					break;
				}
			}
			assertTrue(done);
		}
	}
*/	
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
/*
	@MediumTest
    public void testServerAddContact() {
		final String fnName = "testServerAddContact";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Adds a contact to the change log, validating all the way");
		
		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add new contact with null local ID to change log (error expected)");
		assertFalse(ContactChangeLogTable.addNewContactChange(null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add new contact to change log");
		final Long localContactId = mTestModule.GenerateRandomLong();
		assertTrue(ContactChangeLogTable.addNewContactChange(localContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Checking change log now has 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new contact change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking new contact log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, localContactId);
		assertTrue(info.mLocalDetailId == null);
		assertTrue(info.mServerContactId == null);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_CONTACT);

		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, "");
	}
*/
	@MediumTest
    public void testServerDeleteContact() {
		final String fnName = "testServerDeleteContact";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Deletes a contact to the change log, validating all the way");
		
		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact deletion with null local ID to change log (error expected) - part 1");
		assertFalse(ContactChangeLogTable.addDeletedContactChange(null, null, false, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact deletion with null local ID to change log (error expected) - part 2");
		assertFalse(ContactChangeLogTable.addDeletedContactChange(null, null, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Contact deletion without adding to change log - null server ID (when new contact is not in list)");
		final Long localContactId = TestModule.generateRandomLong();
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, null, false, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is unchanged");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deletion to change log with null server ID (when new contact is not in list) (should do nothing)");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, null, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is unchanged");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Delete contact when not adding to change log (empty change log)");
		final Long serverContactId = TestModule.generateRandomLong();
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, false, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is unchanged");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Delete contact when adding to change log (empty change log)");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log has 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Delete contact when adding to change log with server ID (empty change log)");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Verify change log still has 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking deleted contact log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, localContactId);
		assertTrue(info.mLocalDetailId == null);
		assertEquals(info.mServerContactId , serverContactId);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by contact deleted again");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
    }
/*
	@MediumTest
    public void testServerAddContactDetail() {
		final String fnName = "testServerAddContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Add new contact details to the change log, validating all the way");
		
		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail with null local contact ID to change log (error expected)");
		final Long serverContactId = mTestModule.GenerateRandomLong();
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		ContactDetail testDetail2 = new ContactDetail();
		testDetail.localDetailID = mTestModule.GenerateRandomLong();
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = testDetail.key;
		assertFalse(ContactChangeLogTable.addNewContactDetailChange(testDetail2, serverContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null local detail ID to change log (error expected)");
		testDetail.localContactID = mTestModule.GenerateRandomLong();
		testDetail2.localDetailID = null;
		assertFalse(ContactChangeLogTable.addNewContactDetailChange(testDetail2, serverContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null key to change log (error expected)");
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = null;
		assertFalse(ContactChangeLogTable.addNewContactDetailChange(testDetail2, serverContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log is still empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add valid contact detail to change log");
		assertTrue(ContactChangeLogTable.addNewContactDetailChange(testDetail, serverContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new detail change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertTrue(info.mServerDetailId == null);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_DETAIL);

		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail with null server contact ID to change log when add contact is not in list (should add a new contact change)");
		assertTrue(ContactChangeLogTable.addNewContactDetailChange(testDetail, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new detail change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);
		assertEquals(contactChangeList.get(0).mLocalContactId, testDetail.localContactID);
		
		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add new contact log...");
		assertTrue(ContactChangeLogTable.addNewContactChange(testDetail.localContactID, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by new detail log with no server ID");
		assertTrue(ContactChangeLogTable.addNewContactDetailChange(testDetail, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 2 entries");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 2);

		startSubTest(fnName, "Fetching new detail change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertTrue(info.mServerContactId == null);
		assertEquals(info.mServerDetailId, testDetail.unique_id);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_DETAIL);

		startSubTest(fnName, "Clear new detail from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Fetching new contact change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(testDetail.localContactID, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by new detail log (error expected)");
		assertFalse(ContactChangeLogTable.addNewContactDetailChange(testDetail, serverContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
    }

	@MediumTest
    public void testServerModifyContactDetail() {
		final String fnName = "testServerModifyContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Add contact details modifications to the change log, validating all the way");
		
		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail with null local contact ID to change log (error expected)");
		final Long serverContactId = mTestModule.GenerateRandomLong();
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		ContactDetail testDetail2 = new ContactDetail();
		testDetail.localDetailID = mTestModule.GenerateRandomLong();
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = testDetail.key;
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, false, mTestDatabase.getWritableDatabase()));
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null local detail ID to change log (error expected)");
		testDetail.localContactID = mTestModule.GenerateRandomLong();
		testDetail2.localDetailID = null;
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail2, serverContactId, false, mTestDatabase.getWritableDatabase()));
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail2, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null key to change log (error expected)");
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = null;
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail2, serverContactId, false, mTestDatabase.getWritableDatabase()));
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail2, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log is still empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add modified contact detail change to change log with no unique ID");
		assertTrue(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, false, mTestDatabase.getWritableDatabase()));
		assertTrue(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new detail change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertTrue(info.mServerDetailId == null);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add modified contact detail change to change log with unique ID");
		testDetail.unique_id = mTestModule.GenerateRandomLong();
		assertTrue(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, false, mTestDatabase.getWritableDatabase()));
		assertTrue(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new detail change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertEquals(info.mServerDetailId, testDetail.unique_id);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact detail with null server contact ID to change log when add contact is not in list (should add a new contact change)");
		assertTrue(ContactChangeLogTable.addNewContactDetailChange(testDetail, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new detail change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);
		assertEquals(contactChangeList.get(0).mLocalContactId, testDetail.localContactID);
		
		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add new contact log...");
		assertTrue(ContactChangeLogTable.addNewContactChange(testDetail.localContactID, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by modified detail log with no server ID");
		assertTrue(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, null, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 2 entries");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 2);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertTrue(info.mServerContactId == null);
		assertEquals(info.mServerDetailId, testDetail.unique_id);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL);

		startSubTest(fnName, "Clear modify detail from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Fetching new contact change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.NEW_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(testDetail.localContactID, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by modify detail log");
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by modify detail log (error expected)");
		assertFalse(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact detail from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
    }
*/
	@MediumTest
    public void testServerDeleteContactDetail() {
		final String fnName = "testServerDeleteContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Add contact details deletions to the change log, validating all the way");

		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail deletion with null local contact ID to change log (error expected)");
		final Long serverContactId = TestModule.generateRandomLong();
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		ContactDetail testDetail2 = new ContactDetail();
		testDetail.localDetailID = TestModule.generateRandomLong();
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = testDetail.key;
		testDetail2.serverContactId = serverContactId;
		testDetail.serverContactId = serverContactId;
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail2, false, mTestDatabase.getWritableDatabase()));
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail2, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail deletion with null local detail ID to change log (error expected)");
		testDetail.localContactID = TestModule.generateRandomLong();
		testDetail2.localContactID = testDetail.localContactID;
		testDetail2.localDetailID = null;
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail2, false, mTestDatabase.getWritableDatabase()));
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail2, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail deletion with null key to change log (error expected)");
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = null;
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail2, false, mTestDatabase.getWritableDatabase()));
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail2, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log is still empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add deleted contact detail change to change log with no unique ID");
		assertTrue(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, false, mTestDatabase.getWritableDatabase()));
		assertTrue(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertTrue(info.mServerDetailId == null);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.DELETE_DETAIL);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add deleted contact detail change to change log with unique ID");
		testDetail.unique_id = TestModule.generateRandomLong();
		assertTrue(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, false, mTestDatabase.getWritableDatabase()));
		assertTrue(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new detail change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertEquals(info.mServerDetailId, testDetail.unique_id);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.DELETE_DETAIL);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(testDetail.localContactID, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by delete detail log (error expected)");
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by detail deleted log again");
		assertFalse(ContactChangeLogTable.addDeletedContactDetailChange(testDetail, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact detail from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
    public void testServerAddGroupRel() {
		final String fnName = "testServerAddGroupRel";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Add group relations to the change log, validating all the way");
		
		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Check change log is still empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add valid contact detail to change log without group Id (error expected)");
		final Long localContactId = TestModule.generateRandomLong();
		final Long serverContactId = TestModule.generateRandomLong();
		assertFalse(ContactChangeLogTable.addGroupRel(localContactId, serverContactId, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add valid contact detail to change log (with group Id)");
		final List<Long> groupList = new ArrayList<Long>();
		mTestModule.addRandomGroup(groupList);
		final Long groupId = groupList.get(0);
		assertTrue(ContactChangeLogTable.addGroupRel(localContactId, serverContactId, groupId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.ADD_GROUP_REL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertEquals(info.mGroupOrRelId, groupId);
		assertEquals(info.mLocalContactId, localContactId);
		assertTrue(info.mLocalDetailId == null);
		assertEquals(info.mServerContactId, serverContactId);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.ADD_GROUP_REL);

		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by new group rel (error expected)");
		assertFalse(ContactChangeLogTable.addGroupRel(localContactId, serverContactId, groupId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
    public void testServerDeleteGroupRel() {
		final String fnName = "testServerDeleteGroupRel";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Delete group relations from the change log, validating all the way");
		
		startSubTest(fnName, "Creating table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Check change log is empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add group rel deletion to change log with group ID");
		final Long localContactId = TestModule.generateRandomLong();
		final List<Long> groupList = new ArrayList<Long>();
		mTestModule.addRandomGroup(groupList);
		final Long groupId = groupList.get(0);
		final Long serverContactId = TestModule.generateRandomLong();
		assertTrue(ContactChangeLogTable.deleteGroupRel(localContactId, serverContactId, groupId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_GROUP_REL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking change log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertEquals(info.mGroupOrRelId, groupId);
		assertEquals(info.mLocalContactId, localContactId);
		assertTrue(info.mLocalDetailId == null);
		assertEquals(info.mServerContactId, serverContactId);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.DELETE_GROUP_REL);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add deleted contact detail change to change log with no group ID (error expected)");
		assertFalse(ContactChangeLogTable.deleteGroupRel(localContactId, serverContactId, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log is empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(ContactChangeLogTable.addDeletedContactChange(localContactId, serverContactId, true, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by delete group rel (error expected)");
		assertFalse(ContactChangeLogTable.deleteGroupRel(localContactId, serverContactId, groupId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.DELETE_CONTACT, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}
/*	
	@MediumTest
    public void testServerFetchDetailChanges() {
		final String fnName = "testServerFetchDetailChanges";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Fetch contact detail changes...");
		
		startSubTest(fnName, "Creating change log table");
		createTable();
		
		startSubTest(fnName, "Creating contact details table");
		try {
			ContactDetailsTable.create(mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the contact details table: " + e);
		}

		startSubTest(fnName, "Fetching detail changes (should be empty)");
		List<ContactDetail> contactDetailList = new ArrayList<ContactDetail>();
		List<ContactChangeInfo> infoList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactDetailChanges(contactDetailList, infoList, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactDetailList.size() == 0);
		assertTrue(infoList.size() == 0);

		startSubTest(fnName, "Add dummy detail to contact detail table");
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		testDetail.localContactID = mTestModule.GenerateRandomLong();
		final Long contactServerId = mTestModule.GenerateRandomLong();
		testDetail.unique_id = mTestModule.GenerateRandomLong();
		assertTrue(ServiceStatus.SUCCESS == ContactDetailsTable.addContactDetail(testDetail, true, mTestDatabase.getWritableDatabase()));
		assertTrue(testDetail.localDetailID != null);
		
		startSubTest(fnName, "Put add dummy detail in change log");
		assertTrue(ContactChangeLogTable.addNewContactDetailChange(testDetail, contactServerId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Fetch detail changes from change log");
		assertTrue(ContactChangeLogTable.fetchContactDetailChanges(contactDetailList, infoList, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactDetailList.size() == 1);
		assertTrue(infoList.size() == 1);

		startSubTest(fnName, "Checking detail data");
		ContactDetail d1 = contactDetailList.get(0);
		assertEquals(testDetail.localDetailID, d1.localDetailID);
		assertEquals(testDetail.localContactID, d1.localContactID);
		assertTrue(DatabaseHelper.doDetailsMatch(testDetail, d1));
		assertFalse(DatabaseHelper.hasDetailChanged(testDetail, d1));
		
		startSubTest(fnName, "Checking change log data");
		ContactChangeInfo info = infoList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, contactServerId);
		assertEquals(info.mServerDetailId, testDetail.unique_id);
		assertEquals(info.mServerDetailKey, testDetail.key);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_DETAIL);

		startSubTest(fnName, "Clear delete contact from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(infoList, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Clear detail from detail table");
		assertTrue(ContactDetailsTable.deleteDetailByDetailId(testDetail.localDetailID, mTestDatabase.getWritableDatabase()));

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
    public void testServerFetchNewContactsCursor() {
		final String fnName = "testServerFetchNewContactsCursor";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Fetch new contact changes test...");
		
		startSubTest(fnName, "Creating change log table");
		createTable();
		
		startSubTest(fnName, "Creating contact details table");
		try {
			ContactDetailsTable.create(mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the contact details table: " + e);
		}

		startSubTest(fnName, "Fetch new contacts changes with empty change log...");
		Cursor cursor = ContactChangeLogTable.fetchNewContactChangesCursor(mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertFalse(cursor.moveToFirst());
		cursor.close();
		
		startSubTest(fnName, "Creating dummy contact 1");
		Contact contact1 = mTestModule.createDummyContactData();
		contact1.localContactID = mTestModule.GenerateRandomLong();
		
		startSubTest(fnName, "Add dummy contact 1 to contact detail table");
		for (ContactDetail detail : contact1.details) {
			detail.localContactID = contact1.localContactID;
			assertTrue(ServiceStatus.SUCCESS == ContactDetailsTable.addContactDetail(detail, true, mTestDatabase.getWritableDatabase()));
			assertTrue(detail.localDetailID != null);
		}
		
		startSubTest(fnName, "Put new contact 1 in change log");
		assertTrue(ContactChangeLogTable.addNewContactChange(contact1.localContactID, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Creating dummy contact 2");
		Contact contact2 = mTestModule.createDummyContactData();
		contact2.localContactID = contact1.localContactID + 1;
		
		startSubTest(fnName, "Add dummy contact 2 to contact detail table");
		for (ContactDetail detail : contact2.details) {
			detail.localContactID = contact2.localContactID;
			assertTrue(ServiceStatus.SUCCESS == ContactDetailsTable.addContactDetail(detail, true, mTestDatabase.getWritableDatabase()));
			assertTrue(detail.localDetailID != null);
		}

		startSubTest(fnName, "Put new contact 2 in change log");
		assertTrue(ContactChangeLogTable.addNewContactChange(contact2.localContactID, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Creating dummy contact 3");
		Contact contact3 = mTestModule.createDummyContactData();
		contact3.localContactID = contact2.localContactID + 1;

		startSubTest(fnName, "Add dummy contact 3 to contact detail table");
		for (ContactDetail detail : contact3.details) {
			detail.localContactID = contact3.localContactID;
			assertTrue(ServiceStatus.SUCCESS == ContactDetailsTable.addContactDetail(detail, true, mTestDatabase.getWritableDatabase()));
			assertTrue(detail.localDetailID != null);
		}

		startSubTest(fnName, "Put new contact 3 in change log");
		assertTrue(ContactChangeLogTable.addNewContactChange(contact3.localContactID, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Checking change log now has 3 entries");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 3);

		startSubTest(fnName, "Checking change log now has 3 entries for new_contact and none for other types");
		for (ContactChangeLogTable.ContactChangeType type : ContactChangeLogTable.ContactChangeType.values()) {
			noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(type, mTestDatabase.getReadableDatabase());
			if (type == ContactChangeLogTable.ContactChangeType.NEW_CONTACT) {
				assertTrue(noOfChanges == 3);
			} else {
				assertTrue(noOfChanges == 0);
			}
		}
		
		startSubTest(fnName, "Fetch new contacts changes...");
		cursor = ContactChangeLogTable.fetchNewContactChangesCursor(mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		int totalCount = contact1.details.size() + contact2.details.size() + contact3.details.size();
		assertTrue(cursor.getCount() == totalCount);
		
		startSubTest(fnName, "Getting data for 1st contact from cursor");
		final List<Contact> contactList = new ArrayList<Contact>();
		final List<ContactChangeInfo> infoList = new ArrayList<ContactChangeInfo>();
		final List<ContactChangeInfo> infoListMain = new ArrayList<ContactChangeInfo>();
		ContactChangeLogTable.fetchNewContactChanges(cursor, contactList, infoList, 1, mTestDatabase.getReadableDatabase());
		assertTrue(contactList.size() == 1);
		assertTrue(infoList.size() == 1);
		infoListMain.addAll(infoList);
		
		startSubTest(fnName, "Checking contact data");
		Contact contact = contactList.get(0);
		compareDetails(contact.details, contact1.details);
		
		startSubTest(fnName, "Checking change log data for contact 1");
		ContactChangeInfo info = infoList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, contact1.localContactID);
		assertTrue(info.mLocalDetailId == null);
		assertTrue(info.mServerContactId == null);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_CONTACT);

		startSubTest(fnName, "Getting data for the remaining changes from cursor");
		ContactChangeLogTable.fetchNewContactChanges(cursor, contactList, infoList, 100, mTestDatabase.getReadableDatabase());
		assertTrue(contactList.size() == 2);
		assertTrue(infoList.size() == 2);
		infoListMain.addAll(infoList);
		
		startSubTest(fnName, "Checking contact data for contact 2");
		contact = contactList.get(0);
		compareDetails(contact.details, contact2.details);

		startSubTest(fnName, "Checking change log data for contact 2");
		info = infoList.get(0);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, contact2.localContactID);
		assertTrue(info.mLocalDetailId == null);
		assertTrue(info.mServerContactId == null);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_CONTACT);

		startSubTest(fnName, "Checking contact data for contact 3");
		contact = contactList.get(1);
		compareDetails(contact.details, contact3.details);

		startSubTest(fnName, "Checking change log data for contact 3");
		info = infoList.get(1);
		assertTrue(info.mContactChangeId != null);
		assertTrue(info.mGroupOrRelId == null);
		assertEquals(info.mLocalContactId, contact3.localContactID);
		assertTrue(info.mLocalDetailId == null);
		assertTrue(info.mServerContactId == null);
		assertTrue(info.mServerDetailId == null);
		assertTrue(info.mServerDetailKey == null);
		assertEquals(info.mType, ContactChangeLogTable.ContactChangeType.NEW_CONTACT);

		cursor.close();

		startSubTest(fnName, "Clear delete contacts from change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(infoListMain, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Clear details from detail table");
		for (ContactDetail detail : contact1.details) {
			assertTrue(ContactDetailsTable.deleteDetailByDetailId(detail.localDetailID, mTestDatabase.getWritableDatabase()));
		}
		for (ContactDetail detail : contact2.details) {
			assertTrue(ContactDetailsTable.deleteDetailByDetailId(detail.localDetailID, mTestDatabase.getWritableDatabase()));
		}
		for (ContactDetail detail : contact3.details) {
			assertTrue(ContactDetailsTable.deleteDetailByDetailId(detail.localDetailID, mTestDatabase.getWritableDatabase()));
		}

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}
		
	@MediumTest
    public void testServerModifyIds() {
		final String fnName = "testServerModifyIds";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Test modify contact server ID and contact detail server ID functions...");
		
		startSubTest(fnName, "Creating change log table");
		createTable();

		startSubTest(fnName, "Check change log is empty");
		long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Put modify contact detail in change log");
		final ContactDetail testDetail = mTestModule.createDummyDetailsName();
		testDetail.localDetailID = mTestModule.GenerateRandomLong();
		testDetail.localContactID = mTestModule.GenerateRandomLong();
		testDetail.unique_id = mTestModule.GenerateRandomLong();
		Long serverContactId = mTestModule.GenerateRandomLong();
		assertTrue(ContactChangeLogTable.addModifiedContactDetailChange(testDetail, serverContactId, true, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Checking change log now has 1 entry");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		final List<ContactChangeInfo> contactChangeList = new ArrayList<ContactChangeInfo>();
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking contact log data");
		ContactChangeInfo info = contactChangeList.get(0);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertEquals(info.mServerDetailId, testDetail.unique_id);

		startSubTest(fnName, "Modifying contact server ID");
		serverContactId++;
		assertTrue(ContactChangeLogTable.modifyContactServerID(testDetail.localContactID, serverContactId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Modifying contact detail server ID");
		testDetail.unique_id++;
		assertTrue(ContactChangeLogTable.modifyDetailServerId(testDetail.localDetailID, testDetail.unique_id, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Fetching change log");
		assertTrue(ContactChangeLogTable.fetchContactChangeLog(contactChangeList, ContactChangeLogTable.ContactChangeType.MODIFY_DETAIL, 0, 1000, mTestDatabase.getReadableDatabase()));
		assertTrue(contactChangeList.size() == 1);

		startSubTest(fnName, "Checking contact log data");
		info = contactChangeList.get(0);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mServerContactId, serverContactId);
		assertEquals(info.mServerDetailId, testDetail.unique_id);

		startSubTest(fnName, "Clear change log");
		assertTrue(ContactChangeLogTable.deleteContactChanges(contactChangeList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}

	@MediumTest
    public void testBulkOperations() {
		final String fnName = "testBulkOperations";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Test bulk operations on the table including access time...");
		
		startSubTest(fnName, "Creating change log table");
		createTable();

		startSubTest(fnName, "Creating contact details table");
		try {
			ContactDetailsTable.create(mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the contact details table: " + e);
		}

		startSubTest(fnName, "Add details for " + BULK_TEST_MAX_CONTACTS + " contacts...");
		int totalDetails = 0;
		List<Contact> orgContactChangeList = new ArrayList<Contact>();
		boolean fetchingFirstPage = true;
		for (int i = 0 ; i < BULK_TEST_MAX_CONTACTS ; i++ ) {
			Contact contact = mTestModule.createDummyContactData();
			contact.localContactID = (long)(i + 1);
			for (ContactDetail d : contact.details) {
				d.localContactID = contact.localContactID;
				ServiceStatus status = ContactDetailsTable.addContactDetail(d, true, mTestDatabase.getWritableDatabase());
				assertTrue(status == ServiceStatus.SUCCESS);
				totalDetails++;
			}
			assertTrue(ContactChangeLogTable.addNewContactChange(contact.localContactID, mTestDatabase.getWritableDatabase()));
			if (fetchingFirstPage) {
				orgContactChangeList.add(contact);
			}
			if ((i % BULK_TEST_MAX_CONTACTS_PER_PAGE) == BULK_TEST_MAX_CONTACTS_PER_PAGE - 1) {
				Log.i(LOG_TAG, "Created and added details for " + i + " contacts...");
				fetchingFirstPage = false;
			}
		}
		
		startSubTest(fnName, "Fetching changes cursor...");
		long timeStart = System.nanoTime();
		Cursor newCursor = ContactChangeLogTable.fetchNewContactChangesCursor(mTestDatabase.getReadableDatabase());
		long fetchTime = System.nanoTime() - timeStart;
		Log.i(LOG_TAG, "Total time to fetch cursor for " + BULK_TEST_MAX_CONTACTS + " contacts = " + (fetchTime / 1000000) + "ms");
		
		startSubTest(fnName, "Fetching first batch");
		List<Contact> contactChangeList = new ArrayList<Contact>();
		List<ContactChangeInfo> infoList = new ArrayList<ContactChangeInfo>();
		timeStart = System.nanoTime();
		ContactChangeLogTable.fetchNewContactChanges(newCursor, contactChangeList, infoList, orgContactChangeList.size(), mTestDatabase.getReadableDatabase());
		fetchTime = System.nanoTime() - timeStart;
		Log.i(LOG_TAG, "Total time to fetch first batch of " + orgContactChangeList.size() + " contacts = " + (fetchTime / 1000000) + "ms");
		assertEquals(contactChangeList.size(), orgContactChangeList.size());
		assertEquals(infoList.size(), orgContactChangeList.size());
		
		startSubTest(fnName, "Check batch is correct");
		int totalDetailsFetched = 0;
		for (Contact c1 : orgContactChangeList) {
			boolean done = false;
			for (Contact c2 : contactChangeList) {
				if (c1.localContactID.equals(c2.localContactID)) {
					totalDetailsFetched += c1.details.size();
					compareDetails(c1.details, c2.details);
					done = true;
					break;
				}
			}
			assertTrue(done);
		}

		int totalContactsFetched = contactChangeList.size();
		for (int i = 0 ; i <= BULK_TEST_MAX_CONTACTS / BULK_TEST_MAX_CONTACTS_PER_PAGE ; i++) {
			List<ServerIdInfo> detailServerIdList = new ArrayList<ServerIdInfo>();
			timeStart = System.nanoTime();
			ContactChangeLogTable.fetchNewContactChanges(newCursor, contactChangeList, infoList, BULK_TEST_MAX_CONTACTS_PER_PAGE, mTestDatabase.getReadableDatabase());
			fetchTime = System.nanoTime() - timeStart;
			Log.i(LOG_TAG, "Total time to fetch next batch of " + contactChangeList.size() + " contacts = " + (fetchTime / 1000000) + "ms");
			totalContactsFetched += contactChangeList.size();
			assertEquals(contactChangeList.size(), infoList.size());
			for (Contact c : contactChangeList) {
				totalDetailsFetched += c.details.size();
				for (ContactDetail d : c.details) {
					ServerIdInfo serverIdInfo = new ServerIdInfo();
					serverIdInfo.localId = d.localDetailID;
					serverIdInfo.serverId = mTestModule.GenerateRandomLong();
					detailServerIdList.add(serverIdInfo);
				}
			}
			timeStart = System.nanoTime();
			assertEquals(ContactDetailsTable.syncSetServerIds(detailServerIdList, mTestDatabase.getWritableDatabase()), ServiceStatus.SUCCESS);
			fetchTime = System.nanoTime() - timeStart;
			Log.i(LOG_TAG, "Total time to set detail IDs " + (fetchTime / 1000000) + "ms");

			if (totalContactsFetched < BULK_TEST_MAX_CONTACTS) {
				assertEquals(contactChangeList.size(), BULK_TEST_MAX_CONTACTS_PER_PAGE);
			} else {
				break;
			}
		}
		assertEquals(totalContactsFetched, BULK_TEST_MAX_CONTACTS);
		assertEquals(totalDetailsFetched, totalDetails);
		newCursor.close();
		
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}
*/
}
		

