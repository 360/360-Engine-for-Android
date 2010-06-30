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

import android.database.sqlite.SQLiteDatabase;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.ServerIdInfo;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

public class NowPlusContactsTest extends ApplicationTestCase<MainApplication> {

	private static String LOG_TAG = "NowPlusDatabaseTest";
	final static int WAIT_EVENT_TIMEOUT_MS = 30000;
	
	final static int NUM_OF_CONTACTS = 3;

	private static MainApplication mApplication = null;
	private static DatabaseHelper mDatabaseHelper = null;
	final TestModule mTestModule = new TestModule();
	private DbTestUtility mTestUtility;
	
	public NowPlusContactsTest() {
		super(MainApplication.class);
	}

	private boolean initialise() {
		mTestUtility = new DbTestUtility(getContext());
		
    	createApplication();
		mApplication = getApplication();
		
		if(mApplication == null){
			Log.e(LOG_TAG, "Unable to create main application");
			return false;
		}
		mDatabaseHelper = mApplication.getDatabase();
		if (mDatabaseHelper.getReadableDatabase() == null) {
			return false;
		}
		mTestUtility.startEventWatcher(mDatabaseHelper);
		return true;
	}

	private void shutdown() {
		mTestUtility.stopEventWatcher();
	}
	
	/***
	 * Check if contact detail was added to ContactSummary. 
	 * If contact detail is a name check if it's name in formatted state
	 * is the same as contact summary formatted name. 
	 * @param cd ContactDetail
	 * @return boolean true if ContactDetail is in ContactSummaryTable
	 */
	private boolean isContactDetailInSummary(ContactDetail cd) {
		//boolean result = true;
		ContactSummary cs = new ContactSummary();
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		ServiceStatus status = ContactSummaryTable.fetchSummaryItem(
				cd.localContactID, cs, db);
		if (status != ServiceStatus.SUCCESS) {
			return false;
		} else if (ContactDetail.DetailKeys.VCARD_NAME == cd.key
				&& !cd.getName().toString().equals(cs.formattedName)) { 
			// if ContactDetail name is different then ContactSummary name
			return false;
		} else {
			return true;
		}
	}
	
	@SmallTest
	@Suppress
	public void testMeProfiles() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Contact meProfile = mTestModule.createDummyContactData();
//		assertFalse(mDatabaseHelper.getMeProfileChanged());
		status = SyncMeDbUtils.setMeProfile(mDatabaseHelper,meProfile);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertEquals(SyncMeDbUtils.getMeProfileLocalContactId(mDatabaseHelper), meProfile.localContactID);
		
		Contact fetchedMe = new Contact();
		status = SyncMeDbUtils.fetchMeProfile(mDatabaseHelper,fetchedMe);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertTrue(TestModule.doContactsMatch(fetchedMe, meProfile));
		
		// Modify me Profile  
		Contact meProfile2 = mTestModule.createDummyContactData();
		status = SyncMeDbUtils.setMeProfile(mDatabaseHelper,meProfile2);
		assertEquals(ServiceStatus.SUCCESS, status);
//		assertTrue(mDatabaseHelper.getMeProfileChanged());
		shutdown();
	}
	
	@SmallTest
	public void testAddContactToGroup() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Contact addedContact = mTestModule.createDummyContactData();
		status = mDatabaseHelper.addContact(addedContact);
		assertEquals(ServiceStatus.SUCCESS, status);

		long groupId = 1L;
		status = mDatabaseHelper.addContactToGroup(addedContact.localContactID.longValue(), groupId);
		assertEquals(ServiceStatus.SUCCESS, status);

		status = mDatabaseHelper.deleteContactFromGroup(addedContact.localContactID.longValue(), groupId);
		assertEquals(ServiceStatus.ERROR_NOT_READY, status);
		shutdown();
	}
	
	@SmallTest
	public void testSyncSetServerIds() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Contact addedContact = mTestModule.createDummyContactData();
		status = mDatabaseHelper.addContact(addedContact);
		assertEquals(ServiceStatus.SUCCESS, status);
		long serverId = addedContact.localContactID + 1;
		long userId = addedContact.localContactID + 2;
		List<ServerIdInfo> serverIdList = new ArrayList<ServerIdInfo>();
		ServerIdInfo info = new ServerIdInfo();
		info.localId = addedContact.localContactID;
		info.serverId = serverId;
		info.userId = userId;
		serverIdList.add(info);
		status = ContactsTable.syncSetServerIds(serverIdList, null, mDatabaseHelper.getWritableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);
		addedContact.contactID = serverId;
		addedContact.userID = userId;
		Contact fetchedContact = new Contact();
		status = mDatabaseHelper.fetchContactByServerId(serverId, fetchedContact);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertTrue(TestModule.doContactsMatch(addedContact, fetchedContact));
		
		shutdown();
	}
	
	@SmallTest
	@Suppress
	public void testAddContactDetail() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Contact addedContact = new Contact();
		status = mDatabaseHelper.addContact(addedContact);
		assertEquals(ServiceStatus.SUCCESS, status);
		ContactDetail detail = new ContactDetail();
		
		mTestModule.createDummyDetailsData(detail);
		detail.localContactID = addedContact.localContactID;
		
		status = mDatabaseHelper.addContactDetail(detail);
		assertEquals(ServiceStatus.SUCCESS, status);
		ContactDetail fetchedDetail = new ContactDetail();
		status = mDatabaseHelper.fetchContactDetail(detail.localDetailID, fetchedDetail);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertTrue(DatabaseHelper.doDetailsMatch(detail, fetchedDetail));
		assertTrue(!DatabaseHelper.hasDetailChanged(detail, fetchedDetail));
		assertEquals(ServiceStatus.SUCCESS, mDatabaseHelper.deleteAllGroups());
		List<ServerIdInfo> serverIdList = new ArrayList<ServerIdInfo>();
		ServerIdInfo info = new ServerIdInfo();
		info.localId = fetchedDetail.localDetailID;
		info.serverId = fetchedDetail.localDetailID + 1;
		serverIdList.add(info);
		status = ContactDetailsTable.syncSetServerIds(serverIdList, mDatabaseHelper.getWritableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);
		status = mDatabaseHelper.fetchContactDetail(detail.localDetailID, fetchedDetail);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertEquals(info.serverId, fetchedDetail.unique_id);
		shutdown();
	}
	
	@MediumTest
    public void testAddModifyContacts() {
		Log.i(LOG_TAG, "***** EXECUTING testAddModifyContacts *****");
		Log.i(LOG_TAG, "Test contact functionality (add/modify details contacts)");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Log.i(LOG_TAG, "Test 1c: Add " + NUM_OF_CONTACTS + " random contacts");
		// add contacts and check if added contacts are the same as fetched 
		Contact []  inputContacts = new Contact[NUM_OF_CONTACTS];
		Contact addedContact = new Contact();
		for (int i = 0 ; i < NUM_OF_CONTACTS; i++) {
			inputContacts[i] = mTestModule.createDummyContactData();
			status = mDatabaseHelper.addContact(inputContacts[i]);
			assertEquals(ServiceStatus.SUCCESS, status);
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, addedContact);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertTrue(TestModule.doContactsMatch(addedContact, inputContacts[i]));
		}
		
		Log.i(LOG_TAG, "Test 1d: Modify contacts and check if modification was correct");
		for (int i = 0; i < inputContacts.length; i++) {
			for (int j = 0; j < inputContacts[i].details.size() ; j++) {
				ContactDetail detail = inputContacts[i].details.get(j);
				mTestModule.modifyDummyDetailsData(detail);
				status = mDatabaseHelper.modifyContactDetail(detail);
				assertEquals(ServiceStatus.SUCCESS, status);
				assertTrue(isContactDetailInSummary(detail));
			}
			// check if modifyContactDatail works good 
			Contact modifiedContact = new Contact();
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, modifiedContact);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertTrue(TestModule.doContactsMatch(modifiedContact, inputContacts[i]));
		}
		
		Log.i(LOG_TAG, "Test 1d: contacts and check if modification was correct");
		for (int i = 0; i < inputContacts.length; i++) {
			for (int j = 0; j < inputContacts[i].details.size() ; j++) {
				ContactDetail detail = inputContacts[i].details.get(j);
				mTestModule.modifyDummyDetailsData(detail);
				status = mDatabaseHelper.modifyContactDetail(detail);
				assertEquals(ServiceStatus.SUCCESS, status);
			}
			// check if modifyContactDatail works good 
			Contact modifiedContact = new Contact();
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, modifiedContact);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertTrue(TestModule.doContactsMatch(modifiedContact, inputContacts[i]));
		}
		
		shutdown();
    }
	
	@MediumTest
    public void testAddDeleteContactsDetails() {
		Log.i(LOG_TAG, "***** EXECUTING testAddDeleteContactsDetails *****");
		Log.i(LOG_TAG, "Test contact functionality (add delete contacts details)");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Log.i(LOG_TAG, "Test 1c: Add " + NUM_OF_CONTACTS + " random contacts");
		// add contacts and check if added contacts are the same as fetched 
		Contact []  inputContacts = new Contact[NUM_OF_CONTACTS];
		Contact addedContact = new Contact();
		for (int i = 0 ; i < NUM_OF_CONTACTS; i++) {
			inputContacts[i] = mTestModule.createDummyContactData();
			status = mDatabaseHelper.addContact(inputContacts[i]);
			assertEquals(ServiceStatus.SUCCESS, status);
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, addedContact);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertTrue(TestModule.doContactsMatch(addedContact, inputContacts[i]));
		}
		
		Log.i(LOG_TAG, "Test 1d: Delete contacts detatils and check if deletion was correct");
		for (int i = 0; i < inputContacts.length; i++) {
			for (int j = 0; j < inputContacts[i].details.size() ; j++) {
				ContactDetail detail = inputContacts[i].details.get(j);
				status = mDatabaseHelper.deleteContactDetail(detail.localDetailID);
				assertEquals(ServiceStatus.SUCCESS, status);
			}

			// check if deletion works good 
			Contact modifiedContact = new Contact();
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, modifiedContact);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertTrue(TestModule.doContactsMatch(modifiedContact, inputContacts[i]));
		}

		shutdown();
    }
	
	@MediumTest
    public void testAddDeleteContacts() {
		Log.i(LOG_TAG, "***** EXECUTING testAddDeleteContacts *****");
		Log.i(LOG_TAG, "Test contact functionality (add delete contacts)");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		Log.i(LOG_TAG, "Test 1c: Add " + NUM_OF_CONTACTS + " random contacts");
		// add contacts and check if added contacts are the same as fetched 
		Contact []  inputContacts = new Contact[NUM_OF_CONTACTS];
		Contact addedContact = new Contact();
		for (int i = 0 ; i < NUM_OF_CONTACTS; i++) {
			inputContacts[i] = mTestModule.createDummyContactData();
			status = mDatabaseHelper.addContact(inputContacts[i]);
			assertEquals(ServiceStatus.SUCCESS, status);
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, addedContact);
			assertEquals(ServiceStatus.SUCCESS, status);
			assertTrue(TestModule.doContactsMatch(addedContact, inputContacts[i]));
		}
		
		Log.i(LOG_TAG, "Test 1d: Delete contacts and check if deletion was correct");
		for (int i = 0; i < inputContacts.length; i++) {
			// check if deletion works good 
			status = mDatabaseHelper.deleteContact(inputContacts[i].localContactID);
			assertEquals(ServiceStatus.SUCCESS, status);
			Contact removedContact = new Contact();
			status = mDatabaseHelper.fetchContact(inputContacts[i].localContactID, removedContact);
			assertEquals(ServiceStatus.ERROR_NOT_FOUND, status);	// contact was deleted so it shouldn't be found
		}
		shutdown();
    }
	
	@SmallTest
    public void testSyncAddContactDetailList() {
		Log.i(LOG_TAG, "***** EXECUTING testSyncAddContactDetailList *****");
		Log.i(LOG_TAG, "Test contact add sync contact detail list");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		assertEquals(ServiceStatus.SUCCESS, mTestUtility.waitForEvent(
		        WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK));
		
		// add contacts and check if added contacts are the same as fetched 
		Contact addedContact = new Contact();
		assertEquals(ServiceStatus.SUCCESS, mDatabaseHelper.addContact(addedContact));
		
		ContactDetail cd = new ContactDetail();
		mTestModule.createDummyDetailsData(cd);
		cd.localContactID = addedContact.localContactID;
		cd.nativeContactId = TestModule.generateRandomInt();
		cd.nativeDetailId = TestModule.generateRandomInt();
		cd.nativeVal1 = TestModule.generateRandomString();
		cd.nativeVal2 = TestModule.generateRandomString();
		cd.nativeVal3 = TestModule.generateRandomString();
		
		List<ContactDetail> detailList = new ArrayList<ContactDetail>();
		detailList.add(cd);
		assertEquals(ServiceStatus.SUCCESS,
		        mDatabaseHelper.syncAddContactDetailList(detailList, false, false));
		
		Contact modifiedContact = new Contact();
		assertEquals(ServiceStatus.SUCCESS,
		        mDatabaseHelper.fetchContact(addedContact.localContactID, modifiedContact));
		
		for (ContactDetail fetchedDetail : modifiedContact.details) {
			for (ContactDetail contactDetail : detailList) {
				if (fetchedDetail.key == contactDetail.key) {
					assertEquals(contactDetail.nativeVal1, fetchedDetail.nativeVal1);
					assertEquals(contactDetail.nativeVal2, fetchedDetail.nativeVal2);
					assertEquals(contactDetail.nativeVal3, fetchedDetail.nativeVal3);
				}
			}
		}
		
		shutdown();
    }
	
	
	@SmallTest
    public void testFetchContactInfo() {
		Log.i(LOG_TAG, "***** EXECUTING testFetchContactInfo *****");
		Log.i(LOG_TAG, "Test contact add sync contact detail list");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);

		// add contacts and check if added contacts are the same as fetched 
		Contact addedContact = new Contact();
		status = mDatabaseHelper.addContact(addedContact);
		assertEquals(ServiceStatus.SUCCESS, status);
	
		// create and add phoneDetail
		ContactDetail phoneDetail = new ContactDetail();
		phoneDetail.localContactID = addedContact.localContactID;
		phoneDetail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		String number = "07967 123456";
		phoneDetail.setTel(number, ContactDetail.DetailKeyTypes.CELL);
		status = mDatabaseHelper.addContactDetail(phoneDetail);
		assertEquals(ServiceStatus.SUCCESS, status);

		Contact c = new Contact();
		ContactDetail fetchedPhoneDetail = new ContactDetail();
        status = mDatabaseHelper.fetchContactInfo(number, c, fetchedPhoneDetail);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertTrue(DatabaseHelper.doDetailsMatch(phoneDetail, fetchedPhoneDetail));
		
		shutdown();
    }
	
	@SmallTest
	public void testFindNativeContact() {
		Log.i(LOG_TAG, "***** EXECUTING testFetchContactInfo *****");
		Log.i(LOG_TAG, "Test Find Native Contact");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);

		// add contacts and check if added contacts are the same as fetched 
		Contact nativeContact = new Contact();
		nativeContact.synctophone = true;
		status = mDatabaseHelper.addContact(nativeContact);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// create and add NameDetail
		ContactDetail nameDetail = mTestModule.createDummyDetailsName();
		ContactDetail nicknameDetail = mTestModule.createDummyDetailsNickname(nameDetail);
		nicknameDetail.localContactID = nativeContact.localContactID;
		status = mDatabaseHelper.addContactDetail(nicknameDetail);
		assertEquals(ServiceStatus.SUCCESS, status);

		// create and add phoneDetail
		ContactDetail phoneDetail = new ContactDetail();
		phoneDetail.localContactID = nativeContact.localContactID;
		phoneDetail.key = ContactDetail.DetailKeys.VCARD_PHONE;
		String number = "07967 123456";
		phoneDetail.setTel(number, ContactDetail.DetailKeyTypes.CELL);
		status = mDatabaseHelper.addContactDetail(phoneDetail);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		// create and add phoneDetail
		ContactDetail emailDetail = new ContactDetail();
		emailDetail.localContactID = nativeContact.localContactID;
		emailDetail.key = ContactDetail.DetailKeys.VCARD_EMAIL;
		emailDetail.setEmail(
				TestModule.generateRandomString() + "@mail.co.uk",
				ContactDetail.DetailKeyTypes.HOME);
		status = mDatabaseHelper.addContactDetail(emailDetail);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		Contact fetchedContact = new Contact();
		status = mDatabaseHelper.fetchContact(nativeContact.localContactID, fetchedContact);
		assertTrue(mDatabaseHelper.findNativeContact(fetchedContact));
		
		Contact c = new Contact();
		assertFalse(mDatabaseHelper.findNativeContact(c));
		
		shutdown();
	}
	
	@SmallTest
	public void testModifyContactServerId() {
		Log.i(LOG_TAG, "***** EXECUTING testModifyContactServerId *****");
		Log.i(LOG_TAG, "Test Modify Contact ServerId");
		
		Log.i(LOG_TAG, "Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);

		// create and add contact 
		Contact c = mTestModule.createDummyContactData();
		status = mDatabaseHelper.addContact(c);
		assertEquals(ServiceStatus.SUCCESS, status);
		Long serverId = TestModule.generateRandomLong();
		assertTrue(mDatabaseHelper.modifyContactServerId(c.localContactID, serverId, c.userID));
		Contact fetchedContact = new Contact();
		status = mDatabaseHelper.fetchContact(c.localContactID, fetchedContact);
		assertEquals(serverId, fetchedContact.contactID);
		
		shutdown();
	}
	
	@SmallTest
	@Suppress
	public void testModifyDetailServerId() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();

		assertEquals(ServiceStatus.SUCCESS, mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK));

		Contact addedContact = new Contact();
		assertEquals(ServiceStatus.SUCCESS, mDatabaseHelper.addContact(addedContact));
		
	    ContactDetail detail = new ContactDetail();
		mTestModule.createDummyDetailsData(detail);
		detail.localContactID = addedContact.localContactID;
		assertEquals(ServiceStatus.SUCCESS, mDatabaseHelper.addContactDetail(detail));

		Long serverDetailId = detail.localContactID + TestModule.generateRandomLong();
		assertTrue(mDatabaseHelper.modifyContactDetailServerId(detail.localDetailID, serverDetailId));

		ContactDetail fetchedDetail = new ContactDetail();
		assertEquals(ServiceStatus.SUCCESS, mDatabaseHelper.fetchContactDetail(detail.localDetailID, fetchedDetail));
		assertEquals(serverDetailId, fetchedDetail.unique_id);
		
		shutdown();
	}
	
	@SmallTest
	public void testOnUpgrade() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS,
				DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);
		int oldVersion = TestModule.generateRandomInt();
		int newVersion = TestModule.generateRandomInt();
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		mDatabaseHelper.onUpgrade(db, oldVersion, newVersion);
		shutdown();
	}
	
}