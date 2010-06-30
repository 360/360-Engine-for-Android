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

import android.database.Cursor;
import android.database.SQLException;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.database.tables.NativeChangeLogTable;
import com.vodafone360.people.database.tables.NativeChangeLogTable.ContactChangeInfo;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.tests.TestModule;

public class NowPlusNativeChangeTableTest extends NowPlusTableTestCase {
	private int mTestStep = 0;
	private static String LOG_TAG = "NowPlusNativeChangeTableTest";
	public NowPlusNativeChangeTableTest() {
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
			NativeChangeLogTable.create(mTestDatabase.getWritableDatabase());
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
		Log.i(LOG_TAG, fnName + " has completed successfully");
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
		long noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add new contact with null local ID to change log (error expected)");
		assertFalse(NativeChangeLogTable.addNewContactChange(null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add new contact to change log");
		final List<Long> changeIdxList = new ArrayList<Long>();
		Long localContactID = mTestModule.GenerateRandomLong();
		assertTrue(NativeChangeLogTable.addNewContactChange(localContactID, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Checking change log now has 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching new contact change log");
		Cursor cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking new contact log data");
		ContactChangeInfo info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, localContactID);
		assertTrue(info.mLocalDetailId == null);
		assertTrue(info.mNativeContactId == null);
		assertTrue(info.mNativeDetailId == null);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.NEW_CONTACT);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();
		
		startSubTest(fnName, "Clear new contact from change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
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
		long noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact deletion with null local ID to change log (error expected)");
		assertFalse(NativeChangeLogTable.addDeletedContactChange(null, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact deletion to change log with null native ID (when new contact is not in list) (should do nothing)");
		final List<Long> changeIdxList = new ArrayList<Long>();
		final Long localContactId = TestModule.generateRandomLong();
		assertTrue(NativeChangeLogTable.addDeletedContactChange(localContactId, null, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log is unchanged");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Put delete contact in empty change log with native Id");
		final Integer nativeId = TestModule.generateRandomInt();
		assertTrue(NativeChangeLogTable.addDeletedContactChange(localContactId, nativeId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Verify change log has 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Put delete contact in change log with native ID again");
		assertTrue(NativeChangeLogTable.addDeletedContactChange(localContactId, nativeId, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Verify change log still has 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		Cursor cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		ContactChangeInfo info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, localContactId);
		assertTrue(info.mLocalDetailId == null);
		assertEquals(info.mNativeContactId, nativeId);
		assertTrue(info.mNativeDetailId == null);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.DELETE_CONTACT);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
		
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(NativeChangeLogTable.addDeletedContactChange(localContactId, nativeId, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by contact deleted again");
		assertTrue(NativeChangeLogTable.addDeletedContactChange(localContactId, nativeId, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, localContactId);
		assertTrue(info.mLocalDetailId == null);
		assertEquals(info.mNativeContactId, nativeId);
		assertTrue(info.mNativeDetailId == null);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.DELETE_CONTACT);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
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
		long noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail with null local contact ID to change log (error expected)");
		final Integer nativeId = mTestModule.GenerateRandomInt();
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		ContactDetail testDetail2 = new ContactDetail();
		testDetail.localDetailID = mTestModule.GenerateRandomLong();
		testDetail.nativeContactId = nativeId;
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = testDetail.key;
		testDetail2.nativeContactId = testDetail.nativeContactId;
		assertFalse(NativeChangeLogTable.addNewContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null local detail ID to change log (error expected)");
		testDetail.localContactID = mTestModule.GenerateRandomLong();
		testDetail2.localDetailID = null;
		assertFalse(NativeChangeLogTable.addNewContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null key to change log (error expected)");
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = null;
		assertFalse(NativeChangeLogTable.addNewContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log is still empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add valid contact detail to change log");
		final List<Long> changeIdxList = new ArrayList<Long>();
		assertTrue(NativeChangeLogTable.addNewContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		Cursor cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		ContactChangeInfo info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mNativeContactId, testDetail.nativeContactId);
		assertEquals(info.mNativeDetailId, testDetail.nativeDetailId);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.NEW_DETAIL);
		assertTrue(testDetail.nativeDetailId == null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail with null native ID to change log when add contact is not in list (should add a new contact change)");
		assertTrue(NativeChangeLogTable.addNewContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mNativeContactId, testDetail.nativeContactId);
		assertTrue(info.mNativeDetailId == null);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.NEW_DETAIL);
		assertTrue(testDetail.nativeDetailId == null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add new contact log...");
		assertTrue(NativeChangeLogTable.addNewContactChange(testDetail.localContactID, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by new detail log with no server ID");
		assertTrue(NativeChangeLogTable.addNewContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 2);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(NativeChangeLogTable.addDeletedContactChange(testDetail.localContactID, nativeId, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by new detail log (error expected)");
		assertFalse(NativeChangeLogTable.addNewContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
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
		long noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail with null local contact ID to change log (error expected)");
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		ContactDetail testDetail2 = new ContactDetail();
		testDetail.localDetailID = mTestModule.GenerateRandomLong();
		final int nativeId = mTestModule.GenerateRandomInt();
		testDetail.nativeContactId = nativeId;
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = testDetail.key;
		testDetail2.nativeContactId = testDetail.nativeContactId;
		assertFalse(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null local detail ID to change log (error expected)");
		testDetail.localContactID = mTestModule.GenerateRandomLong();
		testDetail2.localDetailID = null;
		assertFalse(NativeChangeLogTable.addModifiedContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail with null key to change log (error expected)");
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = null;
		assertFalse(NativeChangeLogTable.addModifiedContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log is still empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add modified contact detail change to change log with no native detail id");
		final List<Long> changeIdxList = new ArrayList<Long>();
		assertTrue(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		Cursor cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.MODIFY_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		ContactChangeInfo info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mNativeContactId, testDetail.nativeContactId);
		assertEquals(info.mNativeDetailId, testDetail.nativeDetailId);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.MODIFY_DETAIL);
		assertTrue(testDetail.nativeDetailId == null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add modified contact detail change to change log with native detail ID");
		testDetail.nativeDetailId = mTestModule.GenerateRandomInt();
		assertTrue(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.MODIFY_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mNativeContactId, testDetail.nativeContactId);
		assertEquals(info.mNativeDetailId, testDetail.nativeDetailId);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.MODIFY_DETAIL);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact detail with null native contact ID to change log when add contact is not in list (should add a new contact change)");
		testDetail.nativeContactId = null;
		assertTrue(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add new contact log...");
		assertTrue(NativeChangeLogTable.addNewContactChange(testDetail.localContactID, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by modified detail log with no native ID");
		testDetail.nativeContactId = null;
		testDetail.nativeDetailId = null;
		assertTrue(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 2 entries");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 2);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.MODIFY_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertTrue(info.mNativeContactId == null);
		assertTrue(info.mNativeDetailId == null);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.MODIFY_DETAIL);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.NEW_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		assertTrue(NativeChangeLogTable.addDeletedContactChange(testDetail.localContactID, nativeId, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by modify detail log");
		testDetail.nativeContactId = nativeId;
		assertFalse(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail deleted log...");
		assertTrue(NativeChangeLogTable.addDeletedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by modify detail log (error expected)");
		assertFalse(NativeChangeLogTable.addModifiedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();

		startSubTest(fnName, "Verify change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
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
		long noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		ContactDetail testDetail = mTestModule.createDummyDetailsName();
		ContactDetail testDetail2 = new ContactDetail();
		final int nativeId = TestModule.generateRandomInt();
		testDetail.nativeContactId = nativeId;
		testDetail.localContactID = TestModule.generateRandomLong();
		testDetail.localDetailID = TestModule.generateRandomLong();
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = testDetail.key;
		testDetail2.nativeContactId = nativeId;

		startSubTest(fnName, "Add contact detail deletion with null local contact ID to change log (error expected)");
		assertFalse(NativeChangeLogTable.addDeletedContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail deletion with null local detail ID to change log (error expected)");
		testDetail2.localContactID = testDetail.localContactID;
		testDetail2.localDetailID = null;
		assertFalse(NativeChangeLogTable.addDeletedContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add contact detail deletion with null key to change log (error expected)");
		testDetail2.localDetailID = testDetail.localDetailID;
		testDetail2.key = null;
		assertFalse(NativeChangeLogTable.addDeletedContactDetailChange(testDetail2, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Add deleted contact detail change to change log with no unique ID");
		assertTrue(NativeChangeLogTable.addDeletedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		Cursor cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		final List<Long> changeIdxList = new ArrayList<Long>();
		ContactChangeInfo info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mNativeContactId, testDetail.nativeContactId);
		assertTrue(info.mNativeDetailId == null);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.DELETE_DETAIL);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add deleted contact detail change to change log with unique ID");
		testDetail.nativeDetailId = TestModule.generateRandomInt();
		assertTrue(NativeChangeLogTable.addDeletedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		assertEquals(info.mLocalDetailId, testDetail.localDetailID);
		assertEquals(info.mNativeContactId, testDetail.nativeContactId);
		assertEquals(info.mNativeDetailId, testDetail.nativeDetailId);
		assertEquals(info.mType, NativeChangeLogTable.ContactChangeType.DELETE_DETAIL);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);

		startSubTest(fnName, "Add contact deleted log...");
		testDetail.nativeContactId = nativeId;
		assertTrue(NativeChangeLogTable.addDeletedContactChange(testDetail.localContactID, testDetail.nativeContactId, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by delete detail log (error expected)");
		assertFalse(NativeChangeLogTable.addDeletedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_CONTACT, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();
			
		startSubTest(fnName, "Verfy change log is now empty");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 0);
		
		startSubTest(fnName, "Add contact detail deleted log...");
		assertTrue(NativeChangeLogTable.addDeletedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));
		
		startSubTest(fnName, "Followed by detail deleted log again");
		assertFalse(NativeChangeLogTable.addDeletedContactDetailChange(testDetail, mTestDatabase.getWritableDatabase()));

		startSubTest(fnName, "Check change log has now 1 entry");
		noOfChanges = NativeChangeLogTable.fetchNoOfChanges(null, mTestDatabase.getReadableDatabase());
		assertTrue(noOfChanges == 1);

		startSubTest(fnName, "Fetching change log");
		cursor = NativeChangeLogTable.fetchContactChangeLogCursor(NativeChangeLogTable.ContactChangeType.DELETE_DETAIL, mTestDatabase.getReadableDatabase());
		assertTrue(cursor != null);
		assertTrue(cursor.getCount() == 1);
		assertTrue(cursor.moveToFirst());

		startSubTest(fnName, "Checking log data");
		info = NativeChangeLogTable.getQueryData(cursor);
		assertTrue(info.mNativeChangeId != null);
		assertEquals(info.mLocalContactId, testDetail.localContactID);
		changeIdxList.add(info.mNativeChangeId);
		cursor.close();

		startSubTest(fnName, "Clear change log");
		assertTrue(NativeChangeLogTable.syncDeleteNativeChangeLog(changeIdxList, mTestDatabase.getWritableDatabase()));
		changeIdxList.clear();

		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "*************************************************************************");
		Log.i(LOG_TAG, "");
	}
}
