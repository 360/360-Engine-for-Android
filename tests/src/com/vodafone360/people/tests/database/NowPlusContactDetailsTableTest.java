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
import android.database.sqlite.SQLiteDatabase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.ServerIdInfo;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ContactDetailsTable.NativeIdInfo;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

public class NowPlusContactDetailsTableTest extends NowPlusTableTestCase {
	final TestModule mTestModule = new TestModule();
	private int mTestStep = 0;
	private static int NUM_OF_CONTACTS = 3;
	private static final int MAX_MODIFY_NOWPLUS_DETAILS_COUNT = 100;
	private static String LOG_TAG = "NowPlusContactsTableTest";

	public NowPlusContactDetailsTableTest() {
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
			ContactDetailsTable.create(mTestDatabase.getWritableDatabase());
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
	}

	@MediumTest
	public void testAddFetchContactDetail() {
		final String fnName = "testAddFetchContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Adds a contact details to the contacts details table, validating all the way");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		ContactDetail detail = new ContactDetail();
		mTestModule.createDummyDetailsData(detail);
		// try to add a detail before creating a table
		ServiceStatus status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);

		// try to fetch detail before creating a table
		ContactDetail fetchedDetail = ContactDetailsTable.fetchDetail(
				TestModule.generateRandomLong(), readableDb);
		assertEquals(null, fetchedDetail);
		
		startSubTest(fnName, "Creating table");
		createTable();

		// try to add detail with localContactID that is null
		status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);
		
		// try to add detail with localContactID that is set
		detail.localContactID = TestModule.generateRandomLong();
		status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		fetchedDetail = ContactDetailsTable.fetchDetail(detail.localDetailID, readableDb);
		assertTrue(DatabaseHelper.doDetailsMatch(detail, fetchedDetail));
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}

	@MediumTest
	public void testDeleteContactDetail() {
		final String fnName = "testDeleteContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Deltes a contact detail from the contacts detail table, validating all the way");
		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		ContactDetail detail = new ContactDetail();
		mTestModule.createDummyDetailsData(detail);
		detail.localContactID = TestModule.generateRandomLong();

		// try to delete contact by contact id before creating a table
		ServiceStatus status = ContactDetailsTable.deleteDetailByContactId(
				detail.localContactID, writeableDb);
		assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

		// try to delete contact by detail id before creating a table
		assertFalse(ContactDetailsTable.deleteDetailByDetailId(TestModule
				.generateRandomLong(), writeableDb));
		
		startSubTest(fnName, "Creating table");
		createTable();

		// try to add detail with localContactID that is set
		status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);

		// delete previously added contact
		status = ContactDetailsTable.deleteDetailByContactId(
				detail.localContactID, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// fetch deleted detail (should be null)
		ContactDetail fetchedDetail = ContactDetailsTable.fetchDetail(
				detail.localDetailID, readableDb);
		assertEquals(null, fetchedDetail);
		// try to add detail with localContactID that is set
		status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// try to delete contact by detail id
		assertTrue(ContactDetailsTable.deleteDetailByDetailId(
				detail.localDetailID, writeableDb));		

		// fetch deleted detail (should be null)
		fetchedDetail = ContactDetailsTable.fetchDetail(detail.localDetailID,
				readableDb);
		assertEquals(null, fetchedDetail);
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}

	@MediumTest
	public void testModifyContactDetail() {
		final String fnName = "testModifyContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Modifies a contact detail in the contacts detail table, validating all the way");
		
		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		ContactDetail detail = new ContactDetail();
		mTestModule.createDummyDetailsData(detail);
		detail.localContactID = TestModule.generateRandomLong();
		Long serverId = TestModule.generateRandomLong();
		// try to modify detail before creating a table
		ServiceStatus status = ContactDetailsTable.modifyDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);
		
		assertFalse(ContactDetailsTable.syncSetServerId(TestModule
				.generateRandomLong(), serverId, writeableDb));
		
		startSubTest(fnName, "Creating table");
		createTable();

		// try to modify detail before adding it
		status = ContactDetailsTable.modifyDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);
		
		// try to add detail with localContactID that is set
		status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);

		mTestModule.modifyDummyDetailsData(detail);
		status = ContactDetailsTable.modifyDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// fetch modified detail
		ContactDetail fetchedDetail = ContactDetailsTable.fetchDetail(
				detail.localDetailID, readableDb);
		assertTrue(DatabaseHelper.doDetailsMatch(detail, fetchedDetail));
		
		assertTrue(ContactDetailsTable.syncSetServerId(
				detail.localDetailID, serverId, writeableDb));
		ContactDetail modServIdDetail = ContactDetailsTable.fetchDetail(
				detail.localDetailID, readableDb); 
		assertEquals(serverId,modServIdDetail.unique_id);
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}

	@MediumTest
	public void testContactsMatch() {
		final String fnName = "testContactsMatch";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates doContactsMatch method");
		
		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		Contact contact = mTestModule.createDummyContactData();
		contact.localContactID = TestModule.generateRandomLong();
		
		startSubTest(fnName, "Creating table");
		createTable();
		
		List<NativeIdInfo> detailIdList = new ArrayList<NativeIdInfo>();
		assertFalse(ContactDetailsTable.doContactsMatch(contact,
				contact.localContactID, detailIdList, readableDb));
		
		ServiceStatus status;
		for (ContactDetail cd : contact.details) {
			cd.localContactID = contact.localContactID;
			status = ContactDetailsTable.addContactDetail(cd, true, true, writeableDb);
			assertEquals(ServiceStatus.SUCCESS, status);
		}

//		assertTrue(ContactDetailsTable.doContactsMatch(contact,
//				contact.localContactID, detailIdList, readableDb));
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}

	@MediumTest
	public void testfetchPreferredDetail() {
		final String fnName = "testPreferredDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Adds a contact details to the contacts details table, validating all the way");

		
		startSubTest(fnName, "Creating table");
		createTable();
		
		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		ContactDetail detail = new ContactDetail();
		
		detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		detail.setTel("07967 123456", ContactDetail.DetailKeyTypes.CELL);
		detail.order = 50; // not preferred detail
		detail.localContactID = TestModule.generateRandomLong();
		ServiceStatus  status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);

		ContactDetail preferredDetail = new ContactDetail();
		preferredDetail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		preferredDetail.setTel("07967 654321", ContactDetail.DetailKeyTypes.CELL);
		preferredDetail.localContactID = detail.localContactID;
		preferredDetail.order = 0;
		status = ContactDetailsTable.addContactDetail(preferredDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		ContactDetail altDetail = new ContactDetail();		

		assertTrue(ContactDetailsTable.fetchPreferredDetail(preferredDetail.localContactID.longValue(),
				ContactDetail.DetailKeys.VCARD_PHONE.ordinal(), altDetail, readableDb));
		
		// detail is preferred so should have the same fields
		assertEquals(preferredDetail.localDetailID, altDetail.localDetailID);
		assertEquals(preferredDetail.keyType, altDetail.keyType);
		assertEquals(preferredDetail.value, altDetail.value);
		assertEquals(preferredDetail.order, altDetail.order);
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}

	@MediumTest
	public void testfixPreferredDetail() {
		final String fnName = "testfixPreferredDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Adds a contact details to the contacts details table, validating all the way");

		
		startSubTest(fnName, "Creating table");
		createTable();
		
		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		ContactDetail detail = new ContactDetail();
		
		detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		detail.setTel("07967 123456", ContactDetail.DetailKeyTypes.CELL);
		detail.order = 50; // not preferred detail
		detail.localContactID = TestModule.generateRandomLong();
		ServiceStatus  status = ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);

		status = ContactDetailsTable.fixPreferredValues(detail.localContactID,
				writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// fetch deleted detail (should be null)
		ContactDetail fetchedDetail = ContactDetailsTable.fetchDetail(detail.localDetailID,
				readableDb);
		assertEquals(0, fetchedDetail.order.intValue()); // detail is now preferred
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	@MediumTest
	public void testFetchContactInfo() {
		final String fnName = "testAddFetchContactDetail";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG,
				"Adds a contact details to the contacts details table, validating all the way");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
		
		// create and add phoneDetail
		ContactDetail phoneDetail = new ContactDetail();
		phoneDetail.localContactID = TestModule.generateRandomLong();
		phoneDetail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		String number = "07967 123456";
		phoneDetail.setTel(number, ContactDetail.DetailKeyTypes.CELL);
		ServiceStatus status = ContactDetailsTable.addContactDetail(phoneDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// create and add phoneDetail
		ContactDetail nameDetail = mTestModule.createDummyDetailsName();
		nameDetail.localContactID = phoneDetail.localContactID;
		status = ContactDetailsTable.addContactDetail(nameDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);


		ContactDetail fetchName = new ContactDetail();
		ContactDetail fetchPhone = new ContactDetail();
		status = ContactDetailsTable.fetchContactInfo(number, fetchPhone, fetchName, readableDb);
		assertTrue(DatabaseHelper.doDetailsMatch(phoneDetail, fetchPhone));
		assertTrue(DatabaseHelper.doDetailsMatch(nameDetail, fetchName));
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	@MediumTest
	public void testFindNativeContact() {
		final String fnName = "testFindNativeContact";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb =  mTestDatabase.getReadableDatabase();
		startSubTest(fnName, "Creating table");
		createTable();
		
		// create contact 
		Contact contact = new Contact();
		contact.synctophone = true;
		// add contact to to the ContactsTable
		ContactsTable.create(mTestDatabase.getWritableDatabase());
		ServiceStatus status = ContactsTable.addContact(contact,readableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		// add contact to ContactSummaryTable
		ContactSummaryTable.create(mTestDatabase.getWritableDatabase());
		status = ContactSummaryTable.addContact(contact, readableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		// create and add NameDetail
		ContactDetail nameDetail = mTestModule.createDummyDetailsName();
		ContactDetail nicknameDetail = mTestModule.createDummyDetailsNickname(nameDetail);
		nicknameDetail.localContactID = contact.localContactID;
		status = ContactDetailsTable.addContactDetail(nicknameDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		contact.details.add(nicknameDetail);
		
		// create and add phoneDetail
		ContactDetail phoneDetail = new ContactDetail();
		phoneDetail.localContactID = contact.localContactID;
		phoneDetail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		String number = "07967 123456";
		phoneDetail.setTel(number, ContactDetail.DetailKeyTypes.CELL);
		status = ContactDetailsTable.addContactDetail(phoneDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		contact.details.add(phoneDetail);
		
		// create and add phoneDetail
		ContactDetail emailDetail = new ContactDetail();
		emailDetail.localContactID = contact.localContactID;
		emailDetail.key = ContactDetail.DetailKeys.VCARD_EMAIL;
		emailDetail.setEmail(
				TestModule.generateRandomString() + "@mail.co.uk",
				ContactDetail.DetailKeyTypes.HOME);
		status = ContactDetailsTable.addContactDetail(emailDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		contact.details.add(emailDetail);
		
		assertTrue(ContactDetailsTable.findNativeContact(contact, writeableDb));
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	public void testFetchContactDetailsForNative() {
		final String fnName = "testFetchContactDetailsForNative";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
		
		Contact contact = new Contact();
		contact.localContactID = TestModule.generateRandomLong();
		// create and add NameDetail
		ContactDetail nameDetail = mTestModule.createDummyDetailsName();
		ContactDetail nicknameDetail = mTestModule.createDummyDetailsNickname(nameDetail);
		nicknameDetail.localContactID = contact.localContactID;
		nicknameDetail.nativeContactId = TestModule.generateRandomInt();
		ServiceStatus status = ContactDetailsTable.addContactDetail(nicknameDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		List<ContactDetail> addedDetails = new ArrayList<ContactDetail>();
		addedDetails.add(nicknameDetail);
		
		// create and add phoneDetail
		ContactDetail phoneDetail = new ContactDetail();
		phoneDetail.localContactID = contact.localContactID;
		phoneDetail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		String number = "07967 123456";
		phoneDetail.setTel(number, ContactDetail.DetailKeyTypes.CELL);
		//phoneDetail.nativeContactId = mTestModule.GenerateRandomInt();
		phoneDetail.nativeContactId = nicknameDetail.nativeContactId;
		status = ContactDetailsTable.addContactDetail(phoneDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		addedDetails.add(phoneDetail);
		
		// create and add phoneDetail
		ContactDetail emailDetail = new ContactDetail();
		emailDetail.localContactID = contact.localContactID;
		emailDetail.key = ContactDetail.DetailKeys.VCARD_EMAIL;
		emailDetail.setEmail(
				TestModule.generateRandomString() + "@mail.co.uk",
				ContactDetail.DetailKeyTypes.HOME);
		//emailDetail.nativeContactId = mTestModule.GenerateRandomInt();
		emailDetail.nativeContactId = nicknameDetail.nativeContactId;
		status = ContactDetailsTable.addContactDetail(emailDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		addedDetails.add(emailDetail);
		
		ContactDetail.DetailKeys[] keyList = {
				ContactDetail.DetailKeys.VCARD_NICKNAME,
				ContactDetail.DetailKeys.VCARD_PHONE,
				ContactDetail.DetailKeys.VCARD_EMAIL };
		List<ContactDetail> detailList = new ArrayList<ContactDetail>();
		assertTrue(ContactDetailsTable.fetchContactDetailsForNative(detailList, keyList, false, 0, 
				MAX_MODIFY_NOWPLUS_DETAILS_COUNT, readableDb));
		
		for (int i = 0; i < detailList.size(); i++) {
			assertTrue(DatabaseHelper.doDetailsMatch(detailList.get(i),
					addedDetails.get(i)));
		}
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	
	public void testFindLocalContactIdByKey() {
		final String fnName = "testFindLocalContactIdByKey";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
		// create local detail
		ContactDetail localDetail = new ContactDetail();
		localDetail.localContactID = TestModule.generateRandomLong();
		// populate native data
		localDetail.nativeContactId = TestModule.generateRandomInt();
		localDetail.nativeDetailId = TestModule.generateRandomInt();
		localDetail.nativeVal1 = "nativeVal1";
		localDetail.nativeVal2 = "nativeVal2";
		localDetail.nativeVal3 = "nativeVal3";
		
		localDetail.key = ContactDetail.DetailKeys.VCARD_IMADDRESS;
		localDetail.alt = "google";
		String imAddress = "meon@google.com";
		localDetail.setValue(imAddress, ContactDetail.DetailKeys.VCARD_IMADDRESS, null);
		
		ServiceStatus status = ContactDetailsTable.addContactDetail(localDetail, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// create local detail
		ContactDetail localDetail2 = new ContactDetail();
		localDetail2.localContactID = TestModule.generateRandomLong();
		// populate native data
		localDetail2.nativeContactId = TestModule.generateRandomInt();
		localDetail2.nativeDetailId = TestModule.generateRandomInt();
		localDetail2.nativeVal1 = "nativeVal1";
		localDetail2.nativeVal2 = "nativeVal2";
		localDetail2.nativeVal3 = "nativeVal3";
		
		localDetail2.key = ContactDetail.DetailKeys.VCARD_IMADDRESS;
		String imAddress2 = "meon@google.com";
		localDetail2.setValue(imAddress2, ContactDetail.DetailKeys.VCARD_IMADDRESS, null);
		
		ServiceStatus status2 = ContactDetailsTable.addContactDetail(localDetail2, true, true, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status2);
		
		long result = ContactDetailsTable.findLocalContactIdByKey("google", "meon@google.com", ContactDetail.DetailKeys.VCARD_IMADDRESS, readableDb);
		
		assertTrue("The contact ids don't match: expected=" + localDetail.localContactID + ", actual="+ result,result == localDetail.localContactID);
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	@MediumTest
	public void testsyncSetServerIds() {
		final String fnName = "testsyncSetServerIds";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates syncSetServerIds details");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		
		startSubTest(fnName, "Creating table");
		createTable();
				
		List<ServerIdInfo> detailServerIdList = new ArrayList<ServerIdInfo>();
		List<ContactDetail> detailsList = new ArrayList<ContactDetail>();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail detail = new ContactDetail();
			detail.localContactID = TestModule.generateRandomLong();
			mTestModule.createDummyDetailsData(detail);
			ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
			ServerIdInfo serverInfo = new ServerIdInfo();
			serverInfo.localId = detail.localDetailID;
			serverInfo.serverId = TestModule.generateRandomLong();
			detailServerIdList.add(serverInfo);
			detailsList.add(detail);
		}
		
		ServiceStatus status = ContactDetailsTable.syncSetServerIds(
				detailServerIdList, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail fetchedDetail = ContactDetailsTable.fetchDetail(
					detailsList.get(i).localDetailID, readableDb);
			assertNotNull(fetchedDetail);
			assertEquals(detailServerIdList.get(i).serverId, fetchedDetail.unique_id);
		}
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}

	
	/*
	 * test syncNativeIds
	 */
	public void testSyncSetNativeIds() {
		final String fnName = "testSyncSetNativeIds";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates syncSetNativeIds details");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		
		startSubTest(fnName, "Creating table");
		createTable();
				
		List<NativeIdInfo> nativeIdList = new ArrayList<NativeIdInfo>();
		List<ContactDetail> detailsList = new ArrayList<ContactDetail>();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail detail = new ContactDetail();
			detail.localContactID = TestModule.generateRandomLong();
			mTestModule.createDummyDetailsData(detail);
			ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
			NativeIdInfo nativeIdInfo = new NativeIdInfo();
			nativeIdInfo.nativeContactId = TestModule.generateRandomInt();
			nativeIdInfo.localId = detail.localDetailID;
			nativeIdList.add(nativeIdInfo);
			detailsList.add(detail);
		}
		
		ServiceStatus status = ContactDetailsTable.syncSetNativeIds(
				nativeIdList, writeableDb);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail fetchedDetail = ContactDetailsTable.fetchDetail(
					nativeIdList.get(i).localId, readableDb);
			assertNotNull(fetchedDetail);
			assertEquals(nativeIdList.get(i).nativeContactId, fetchedDetail.nativeContactId);
		}
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	/*
	 * test syncServerFetchContactChanges
	 */
	public void testSyncServerFetchContactChanges() {
		final String fnName = "testSyncServerFetchContactChanges";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates syncServerFetchContactChanges details");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
				
		List<ContactDetail> detailsList = new ArrayList<ContactDetail>();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail detail = new ContactDetail();
			detail.localContactID = TestModule.generateRandomLong();
			mTestModule.createDummyDetailsData(detail);
			ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
			detailsList.add(detail);
		}
		
		Cursor cursor = ContactDetailsTable.syncServerFetchContactChanges(
				readableDb, true);
		assertEquals(NUM_OF_CONTACTS, cursor.getCount());
		
		Cursor cursorOldContacts = ContactDetailsTable.syncServerFetchContactChanges(
				readableDb, false);
		assertEquals(0, cursorOldContacts.getCount());
		
		cursorOldContacts.close();
		
		List<Contact> contactList = new ArrayList<Contact>();
		ContactDetailsTable.syncServerGetNextNewContactDetails(cursor, contactList, NUM_OF_CONTACTS);

		for (int i = 0; i < contactList.size(); i++) {
			assertTrue(DatabaseHelper.doDetailsMatch(detailsList.get(i),
					contactList.get(i).details.get(0)));
		}
		
		cursor.close();
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
		
	}
	
	/*
	 * test syncServerFetchNoOfChanges
	 */
	public void testSyncServerFetchNoOfChanges() {
		final String fnName = "testSyncServerFetchNoOfChanges";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates syncServerFetchContactChanges details");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
				
		List<ContactDetail> detailsList = new ArrayList<ContactDetail>();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail detail = new ContactDetail();
			detail.localContactID = TestModule.generateRandomLong();
			detail.serverContactId = TestModule.generateRandomLong();
			mTestModule.createDummyDetailsData(detail);
			ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
			detailsList.add(detail);
		}
		
		Cursor cursor = ContactDetailsTable.syncServerFetchContactChanges(
				readableDb, false);
		assertEquals(cursor.getCount(), ContactDetailsTable.syncServerFetchNoOfChanges(readableDb));
		
		for (ContactDetail contactDetail : detailsList) {
			Long serverContactId = TestModule.fetchSyncServerId(
					contactDetail.localDetailID, readableDb);
			assertEquals(contactDetail.serverContactId, serverContactId);
		}
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
	
	/*
	 * test Sync Native Contact Changes
	 */
	public void testSyncNativeContactChanges() {
		final String fnName = "testSyncNativeContactChanges";
		mTestStep = 1;

		Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
		Log.i(LOG_TAG, "Validates sync server native contact details");

		SQLiteDatabase writeableDb = mTestDatabase.getWritableDatabase();
		SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
		
		startSubTest(fnName, "Creating table");
		createTable();
				
		List<ContactDetail> detailsList = new ArrayList<ContactDetail>();
		for (int i = 0; i < NUM_OF_CONTACTS; i++) {
			ContactDetail detail = new ContactDetail();
			detail.localContactID = TestModule.generateRandomLong();
			detail.nativeContactId = TestModule.generateRandomInt();
			detail.nativeVal1 = TestModule.generateRandomString();
			detail.nativeVal2 = TestModule.generateRandomString();
			detail.nativeVal3 = TestModule.generateRandomString();
			
			mTestModule.createDummyDetailsData(detail);
			ContactDetailsTable.addContactDetail(detail, true, true, writeableDb);
			detailsList.add(detail);
		}
		
		Cursor nat = ContactDetailsTable.syncNativeFetchContactChanges(
				readableDb, true);
		assertEquals(3, nat.getCount()); 
		nat.close();
		
		Cursor cursor = ContactDetailsTable.syncNativeFetchContactChanges(
				readableDb, false);
		assertEquals(cursor.getCount(), ContactDetailsTable.syncNativeFetchNoOfChanges(readableDb));

		List<Contact> contactList = new ArrayList<Contact>();
		ContactDetailsTable.syncNativeGetNextNewContactDetails(cursor, contactList, NUM_OF_CONTACTS);

		for (int i = 0; i < contactList.size(); i++) {
			assertTrue(DatabaseHelper.doDetailsMatch(detailsList.get(i),
					contactList.get(i).details.get(0)));
		}		
		
		cursor.close();
		
		Log.i(LOG_TAG, "***********************************************");
		Log.i(LOG_TAG, fnName + " has completed successfully");
		Log.i(LOG_TAG, "***********************************************");
	}
}
