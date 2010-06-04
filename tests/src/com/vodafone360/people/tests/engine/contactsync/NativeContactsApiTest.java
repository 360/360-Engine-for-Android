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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.engine.contactsync.NativeContactsApi.Account;
import com.vodafone360.people.engine.contactsync.NativeContactsApi.ContactsObserver;
import com.vodafone360.people.engine.contactsync.NativeContactsApi1;
import com.vodafone360.people.engine.contactsync.NativeContactsApi2;
import com.vodafone360.people.tests.engine.contactsync.NativeContactsApiTestHelper.IPeopleAccountChangeObserver;
import com.vodafone360.people.utils.VersionUtils;

import android.app.Instrumentation;
import android.database.ContentObserver;
import android.os.Handler;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

public class NativeContactsApiTest extends InstrumentationTestCase {
	private static final String LOG_TAG = "NativeContactsApiTest";
		
	NativeContactsApi mNabApi;
	NativeContactsApiTestHelper mNabApiHelper;
	
	static final int NUM_ACCOUNTS_TO_CREATE = 5;
	private static final String PEOPLE_ACCOUNT_TYPE = "com.vodafone360.people.android.account";
	private static final String PEOPLE_USERNAME = "john.doe";
	private static final Account s360PeopleAccount = new Account(
			PEOPLE_USERNAME, PEOPLE_ACCOUNT_TYPE);
	
	private final boolean mUsing2xApi = VersionUtils.is2XPlatform();
	
	private ContactsObserver mContactsObserver = new ContactsObserver() {
		@Override
		public void onChange() {
			synchronized(this) {
				sNabChanged = true;
			}
		}
	};
	
	static boolean sNabChanged = false;
	
	class AccountsObserver implements IPeopleAccountChangeObserver {

		private int mCurrentNumAccounts = 0;
		@Override
		public void onPeopleAccountsChanged(int currentNumAccounts) {
			mCurrentNumAccounts = currentNumAccounts;
			Thread.currentThread().interrupt();
		}
		
		public int getCurrentNumAccounts() {
			return mCurrentNumAccounts;
		}
	}
	
	class ObserverThread extends Thread {
		private ContactsObserver mObserver = new ContactsObserver() {
			@Override
			public void onChange() {
				synchronized(this) {
					sNabChanged = true;
				}
			}
		};
		
		public synchronized ContactsObserver getObserver() {
			return mObserver;
		}		
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		NativeContactsApi.createInstance(getInstrumentation().getTargetContext());
		mNabApi = NativeContactsApi.getInstance();
		mNabApiHelper = NativeContactsApiTestHelper.getInstance(getInstrumentation().getContext());
		//mNabApiHelper.wipeNab(mCr);
	}

	@Override
	protected void tearDown() throws Exception {
		mNabApiHelper.wipeNab();
		mNabApiHelper = null;
		NativeContactsApi.destroyInstance();
		mNabApi = null;
		super.tearDown();
	}
	

	/**
	 * Tests API functionality to do the following:
	 * - Create Instance
	 * - Get Instance
	 * - Destroy Instance
	 */
	@SmallTest
	public void testInstance() {
		// Need to destroy instance first for this test!
		NativeContactsApi.destroyInstance();
		NativeContactsApi nabApi = null;
		boolean caughtException = false;
		try {
			nabApi = NativeContactsApi.getInstance();
		} catch(InvalidParameterException ex) {
			caughtException = true;
		}
		
		assertNull(nabApi);
		assertTrue(caughtException);
		
		NativeContactsApi.createInstance(getInstrumentation().getContext());
		
		try {
			nabApi = NativeContactsApi.getInstance();
			caughtException = false;
		} catch(InvalidParameterException ex) {
		}
		
		assertFalse(caughtException);
		assertNotNull(nabApi);
		
		NativeContactsApi.destroyInstance();
		// Double Destroy
		NativeContactsApi.destroyInstance();
		
		try {
			nabApi = NativeContactsApi.getInstance();
		} catch(InvalidParameterException ex) {
			caughtException = true;
		}	
		
		assertTrue(caughtException);
		
		NativeContactsApi.createInstance(getInstrumentation().getContext());
		
		try {
			nabApi = NativeContactsApi.getInstance();
			caughtException = false;
		} catch(InvalidParameterException ex) {
		}		
		
		assertFalse(caughtException);
	
		if(mUsing2xApi) {
			assertTrue(nabApi instanceof NativeContactsApi2);
		} else {
			assertTrue(nabApi instanceof NativeContactsApi1);
		}
	}

		
	/**
	 * Tests the plain Account inner class
	 */
	@SmallTest
	public void testAccountDataType() {
		// Account with null name and type
		Account emptyAccount = new Account(null, null);
		assertNotNull(emptyAccount);
		assertNull(emptyAccount.getName());
		assertNull(emptyAccount.getType());
		emptyAccount = null;
	
		// Array of accounts
		final int NUM_OBJS_TO_CREATE = 4;
		final String baseName = "AccountBaseName";
		final String baseType = "AccountBaseType";
		for(int i = 0; i < NUM_OBJS_TO_CREATE; i++) {
			Account account = new Account(baseName+i, baseType+i);
			assertNotNull(account);
			assertNotNull(account.getName());
			assertNotNull(account.getType());
			assertNotNull(account.toString());
			assertEquals("Account: name="+baseName+i+", type="+baseType+i, account.toString());
			assertEquals(baseName+i, account.getName());
			assertEquals(baseType+i, account.getType());
			assertFalse(account.isPeopleAccount());
		}
	}
		
	/**
	 * Tests the GetAccounts API functionality.
	 * Now that this also makes indirect use of Add and Remove functionality
	 */
	@SmallTest
	public void testGetAccounts() {
		Account[] accounts = mNabApi.getAccounts();
		int initialNumAccounts = 0;
		if(accounts != null) {
			final int numAccounts = initialNumAccounts = accounts.length;
			for(int i = 0; i < numAccounts; i++) {
				assertFalse(accounts[i].isPeopleAccount());
			}
		} 
			
		if(mUsing2xApi) {
			for(int i = 0; i < NUM_ACCOUNTS_TO_CREATE; i++) {
				mNabApi.addPeopleAccount(PEOPLE_USERNAME+(i+1));
				threadWait(100);
				
				accounts = mNabApi.getAccounts();
				assertNotNull(accounts);
				assertEquals(accounts.length, initialNumAccounts + (i+1));
			}

			for(int j = NUM_ACCOUNTS_TO_CREATE; j > 0 ; j--) {
				mNabApi.removePeopleAccount();

				threadWait(3000);

				accounts = mNabApi.getAccounts();
				assertEquals(j > 1, accounts != null);
				int numAccounts = 0;
				if(accounts != null) {
					numAccounts = accounts.length;
				}
				assertEquals(initialNumAccounts + j-1, numAccounts);		
			}

			accounts = mNabApi.getAccounts();
			assertEquals(accounts != null, initialNumAccounts > 0);
		}
	}
	
	/**
	 * Tests getAccountsByType functionality
	 */
	@SmallTest
	public void testGetAccountsByType() {
		assertNull(mNabApi.getAccountsByType("ghewoih4oihoi"));
		assertNull(mNabApi.getAccountsByType("xpto"));
		assertNull(mNabApi.getAccountsByType(PEOPLE_ACCOUNT_TYPE));
			
		if(mUsing2xApi) {
			for(int i = 0; i < NUM_ACCOUNTS_TO_CREATE; i++) {
				mNabApi.addPeopleAccount(PEOPLE_USERNAME+(i+1));
				threadWait(100);
				
				Account[] accounts = mNabApi.getAccountsByType(PEOPLE_ACCOUNT_TYPE);
				assertNotNull(accounts);
				assertNull(mNabApi.getAccountsByType("xpto"));
				assertEquals(accounts.length, i+1);
			}

			for(int j = NUM_ACCOUNTS_TO_CREATE; j > 0 ; j--) {
				mNabApi.removePeopleAccount();

				threadWait(3000);

				Account[] accounts = mNabApi.getAccountsByType(PEOPLE_ACCOUNT_TYPE);
				assertEquals(j > 1, accounts != null);
				assertNull(mNabApi.getAccountsByType("xpto"));
				int numAccounts = 0;
				if(accounts != null) {
					numAccounts = accounts.length;
				}
				assertEquals(j-1, numAccounts);		
			}
			
			assertNull(mNabApi.getAccountsByType(PEOPLE_ACCOUNT_TYPE));
		}
	}
	
	/**
	 * Tests the following API functionality around manipulating Native People Accounts.
	 * - Add a People Account
	 * - Remove a People Account
	 * It is not possible to test adding/removing other types of accounts explicitly
	 */
	@SmallTest
	public void testAddRemoveAccount() {
		verifyPeopleAccountPresence(0);
		
		// Try delete here just for good measure
		mNabApi.removePeopleAccount();
		verifyPeopleAccountPresence(0);

		final int numAccountsToCreate = 5;
		// *** ADD ***
		if(mUsing2xApi) {	
			for(int i = 0 ; i < numAccountsToCreate; i++) {
				assertTrue(mNabApi.addPeopleAccount(PEOPLE_USERNAME+(i+1)));
				threadWait(100);
				// Double add, should fail
				assertFalse(mNabApi.addPeopleAccount(PEOPLE_USERNAME+(i+1)));
				threadWait(100);
				verifyPeopleAccountPresence(i+1);
			}			
		} else {
			// Try on 1.X just in case
			assertFalse(mNabApi.addPeopleAccount(PEOPLE_USERNAME));
			verifyPeopleAccountPresence(0);
		}
		
		// *** DELETE ***		
		if(mUsing2xApi) {
			for(int j = numAccountsToCreate; j > 0 ; j--) {
				mNabApi.removePeopleAccount();
				threadWait(3000);
				verifyPeopleAccountPresence(j-1);
			}
		} else {
			// Try on 1.X just in case
			verifyPeopleAccountPresence(0);
			mNabApi.removePeopleAccount();
			verifyPeopleAccountPresence(0);
		}
	}
	
	/**
	 * Tests registering and unregistering a ContactsObserver.
	 * 
	 * Note: The code that checks if change events are sent is commented out
	 *       and needs more development time to be completed.
	 */
	@SmallTest
	public void testContactsChangeObserver() {

		//ObserverThread observerThread = new ObserverThread();
	    
	    /**
	     * The following code could be used to make the current thread process all the
	     * pending messages in the queue and then check that the change event has been called
	     * on the provided ContactsObserver.
	     * 
	     * final Looper looper = Looper.myLooper();
         * final MessageQueue queue = Looper.myQueue();
         * queue.addIdleHandler(new MessageQueue.IdleHandler() {
         * 
         *  @Override
         *  public boolean queueIdle() {
         *      // message has been processed and the looper is now in idle
         *      // state
         *      // quit the loop() otherwise we would not be able to carry on
         *      looper.quit();
         *
         *      return false;
         *  }
         *
         * });
         *
         * // get the message processed by the thread event loop
         * Looper.loop();
         * 
	     */
		
		boolean caughtException = false;
		
		mNabApi.unregisterObserver();
		
		try {
			mNabApi.registerObserver(mContactsObserver);
		} catch(RuntimeException ex) {
			caughtException = true;
		}
		
		assertFalse(caughtException);
		
		try {
			mNabApi.registerObserver(mContactsObserver);
		} catch(RuntimeException ex) {
			caughtException = true;
		}
		
		assertTrue(caughtException);
		
		mNabApi.unregisterObserver();
		
		try {
			mNabApi.registerObserver(mContactsObserver);
			caughtException = false;
		} catch(RuntimeException ex) {
			
		}		
		
		assertFalse(caughtException);
		
//		Account account = null;
//		if(mUsing2xApi) {
//			// Add Account for the case where we are in 2.X
//			mNabApi.addPeopleAccount(PEOPLE_USERNAME);
//			account = s360PeopleAccount;
//			threadWait(100);
//		}
//		
//		final int numRandomContacts = 10;
//		long ids[] = null;
//		boolean unregister = false;
//		for(int i = 0 ; i < numRandomContacts; i++) {
//			
//			if(unregister) {
//			   mNabApi.unregisterObserver(observerThread.getObserver());
//			}
			
//			assertFalse(sNabChanged);
//			final ContactChange[] newContactCcList = ContactChangeHelper.randomContact(-1, -1, -1);
//			mNabApi.addContact(account, newContactCcList);
			//threadWait(10000);
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			assertTrue(unregister ^ sNabChanged);
//			sNabChanged = false;
//			ids = getContactIdsForAllAccounts();
//			assertNotNull(ids);
//			assertEquals(1, ids.length);
//			mNabApi.removeContact(ids[0]);
//			threadWait(500);
//			assertTrue(unregister ^ sNabChanged);
//			sNabChanged = false;
//			unregister = !unregister;
			
//			if(!unregister) {
//				mNabApi.registerObserver(observerThread.getObserver());
//			}
//		}
		
		mNabApi.unregisterObserver();
	}
	
	
	@MediumTest
	public void testAddGetRemoveContacts() {
		Account account = null;
		if(mUsing2xApi) {
			// Add Account for the case where we are in 2.X
			mNabApi.addPeopleAccount(PEOPLE_USERNAME);
			account = s360PeopleAccount;
			threadWait(100);
		}
		
		long[] ids = getContactIdsForAllAccounts();
		assertNull(ids);
		
		final int numRandomContacts = 10;
		
		long expectedContactId = ContactChange.INVALID_ID;
		for(int i = 0; i < numRandomContacts; i++) {
			long id = i;
			final ContactChange[] newContactCcList = ContactChangeHelper.randomContact(id, id, -1);
			final ContactChange[] newIds = mNabApi.addContact(account, newContactCcList);
			// expected num ids is + 1 for contact id
			verifyNewContactIds(expectedContactId, newContactCcList, newIds);

			expectedContactId = newIds[0].getNabContactId() + 1;
			// GET CONTACTS AND COMPARE
			ids = getContactIdsForAllAccounts();
			assertNotNull(ids);
			assertEquals(i+1, ids.length);
			final ContactChange[] fetchedContactCcList = mNabApi.getContact(ids[i]);
			assertNotNull(fetchedContactCcList);
			if(!ContactChangeHelper.areChangeListsEqual(newContactCcList, fetchedContactCcList, false)) {
				Log.e(LOG_TAG, "ADD FAILED: Print of contact to be added follows:");
				ContactChangeHelper.printContactChangeList(newContactCcList);
				Log.e(LOG_TAG, "ADD FAILED: Print of contact fetched follows:");
				ContactChangeHelper.printContactChangeList(fetchedContactCcList);
				// fail test at this point
				assertFalse(true);
			}
		}
		
		// DELETE
		final int idCount = ids.length;
		for(int i = 0; i < idCount; i++) {
			mNabApi.removeContact(ids[i]);
		}
		
		ids = getContactIdsForAllAccounts();
		assertNull(ids);
	}
	
	@MediumTest
	public void testUpdateContacts() {
		Account account = null;
		if(mUsing2xApi) {
			// Add Account for the case where we are in 2.X
			mNabApi.addPeopleAccount(PEOPLE_USERNAME);
			account = s360PeopleAccount;
			threadWait(100);
		}
		
		long[] ids = getContactIdsForAllAccounts();
		assertNull(ids);
		
		final int numRandomContacts = 10;
		
		for(int i = 0; i < numRandomContacts; i++) {
			long id = i;
			final ContactChange[] newContactCcList = ContactChangeHelper.randomContact(id, id, -1);
			mNabApi.addContact(account, newContactCcList);

			//expectedContactId = newIds[0].getNabContactId() + 1;
			// GET CONTACT
			ids = getContactIdsForAllAccounts();
			assertNotNull(ids);
			assertEquals(i+1, ids.length);
			final ContactChange[] fetchedContactCcList = mNabApi.getContact(ids[i]);
			assertNotNull(fetchedContactCcList);
			
			// UPDATE
			final ContactChange[] updateCcList = ContactChangeHelper.randomContactUpdate(fetchedContactCcList);
			
			assertNotNull(updateCcList);
			assertTrue(updateCcList.length > 0);
						
			final ContactChange[] updatedIdsCcList = mNabApi.updateContact(updateCcList);
			
			verifyUpdateContactIds(fetchedContactCcList, updateCcList, updatedIdsCcList);
			
			final ContactChange[] updatedContactCcList = ContactChangeHelper.generatedUpdatedContact(newContactCcList, updateCcList);
			
			ids = getContactIdsForAllAccounts();
			assertNotNull(ids);
			assertEquals(i+1, ids.length);
			final ContactChange[] fetchedUpdatedContactCcList = mNabApi.getContact(ids[i]);
			assertNotNull(fetchedUpdatedContactCcList);
			if(!ContactChangeHelper.areUnsortedChangeListsEqual(updatedContactCcList, fetchedUpdatedContactCcList, false)) {
				// Print update 
				Log.e(LOG_TAG, "UPDATE FAILED: Print of initial contact follows");
				ContactChangeHelper.printContactChangeList(fetchedContactCcList);
				Log.e(LOG_TAG, "UPDATE FAILED: Print of failed update follows:");
				ContactChangeHelper.printContactChangeList(updateCcList);
				// fail test at this point
				assertFalse(true);
			}
		}
		
		// DELETE
		final int idCount = ids.length;
		for(int i = 0; i < idCount; i++) {
			mNabApi.removeContact(ids[i]);
		}
		
		ids = getContactIdsForAllAccounts();
		assertNull(ids);
	}

	
	@SmallTest
	public void testIsKeySupported() {
		for(int i = ContactChange.KEY_UNKNOWN; i < ContactChange.KEY_EXTERNAL; i++) {
			switch(i) {
				case ContactChange.KEY_VCARD_NAME:
				case ContactChange.KEY_VCARD_PHONE:
				case ContactChange.KEY_VCARD_EMAIL:
				case ContactChange.KEY_VCARD_ADDRESS:
		        case ContactChange.KEY_VCARD_ORG:
		        case ContactChange.KEY_VCARD_TITLE:
		        case ContactChange.KEY_VCARD_NOTE:
		        	assertEquals(true, mNabApi.isKeySupported(i));
		        	break;
				case ContactChange.KEY_VCARD_NICKNAME:
				case ContactChange.KEY_VCARD_DATE:
				case ContactChange.KEY_VCARD_URL:
					if(VersionUtils.is2XPlatform()) {
			        	assertEquals(true, mNabApi.isKeySupported(i));
			        	break;						
					}
				default:
					assertEquals(false, mNabApi.isKeySupported(i));				
			}
		}
	}

	/**
	 * Utility method to get contact ids for all existing accounts.
	 */
	private long[] getContactIdsForAllAccounts() {
		Account[] accounts = mNabApi.getAccounts();
		List<Long> idsArray = new ArrayList<Long>();
		if(accounts != null) {
			final int NUM_ACCOUNTS = accounts.length;
			for(int i = 0; i < NUM_ACCOUNTS; i++) {
				Account account = accounts[i];
				if(account != null) {
					long ids[] = mNabApi.getContactIds(account);
					if(ids != null) {
						final int NUM_IDS = ids.length;
						for(int j=0; j < NUM_IDS; j++) {
							idsArray.add(Long.valueOf(ids[j]));
						}
					}
				}
			}
		}
		
		long nullAccountIds[] = mNabApi.getContactIds(null);
		if(nullAccountIds != null) {
			final int NUM_IDS = nullAccountIds.length;
			for(int i = 0; i < NUM_IDS; i++) {
				idsArray.add(Long.valueOf(nullAccountIds[i]));
			}
		}
		
		final int idsCount = idsArray.size();
		if(idsCount > 0 ) {
			long[] ids = new long[idsCount];
			for(int i = 0; i < idsCount; i++) {
				ids[i] = idsArray.get(i);
			}
			
			return ids;
		}
		
		return null;
	}
	
	/**
	 * Utility method to put the thread waiting
	 * @param time
	 */
	private void threadWait(int time) {
		try {
			synchronized(this) {
				wait(time);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Utility method to put the thread to sleep
	 * @param time
	 */
	private void threadSleep(int time) {
		try {
			synchronized(this) {
				Thread.sleep(time);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		

	/**
	 * Utility method to verify the presence of people accounts
	 * @param numExpectedAccounts The number of expected Accounts
	 */
	private void verifyPeopleAccountPresence(final int numExpectedAccounts) {
		if(numExpectedAccounts > 0) {
			assertTrue(mNabApi.isPeopleAccountCreated());
		} else {
			assertFalse(mNabApi.isPeopleAccountCreated());
		}
		
		int peopleAccountsFound = 0;
		final Account[] accounts = mNabApi.getAccounts();
		if(accounts != null) {
			final int numAccounts = accounts.length;
			for(int i = 0; i < numAccounts; i++) {
				if(accounts[i] != null && accounts[i].isPeopleAccount()) {
					peopleAccountsFound++;
				}
			}
		}
		
		assertEquals(numExpectedAccounts, peopleAccountsFound);
	}
	
	/**
	 * Utility method to verify that received Ids for a new Contact are as expected 
	 */
	private void verifyNewContactIds(long expectedNabId, ContactChange[] originalCcList, ContactChange[] ids) {
		assertNotNull(ids);
		final int idsCount = ids.length;
		final int originalCount = originalCcList.length; 
		assertEquals(originalCount + 1, idsCount);
		
		final long expectedInternalContactId = originalCcList[0].getInternalContactId();
		long previousDetailId = ContactChange.INVALID_ID;
		long orgAndTitleNabDetailId = ContactChange.INVALID_ID;
		for(int i= 0; i < idsCount; i++) {
			final ContactChange idCc = ids[i];
			if(!VersionUtils.is2XPlatform() && i > 0) { 
				final int originalKey = originalCcList[i-1].getKey();
				if(originalKey == ContactChange.KEY_VCARD_NAME ||
					originalKey == ContactChange.KEY_VCARD_NOTE ||
					originalKey == ContactChange.KEY_VCARD_NICKNAME ||
					originalKey == ContactChange.KEY_VCARD_URL) {
					// These fields don't get written to 1.X NAB and so correspond to a null idCc
					assertNull(idCc);
					continue;
				}
			}
			assertNotNull(idCc);
			// Internal Contact ID check
			assertEquals(expectedInternalContactId, 
					idCc.getInternalContactId());
			
			
			if(i > 0) {
				// Internal Detail ID check
				final ContactChange originalCc = originalCcList[i-1];
				
				assertEquals(originalCcList[i-1].getInternalDetailId(), 
						ids[i].getInternalDetailId());					

				// NAB Detail ID check
				assertEquals(ContactChange.TYPE_UPDATE_NAB_DETAIL_ID, ids[i].getType());
				long currentDetailId = ids[i].getNabDetailId();

				if(previousDetailId != ContactChange.INVALID_ID) {					
					if(originalCc.getKey() != ContactChange.KEY_VCARD_ORG && 
							originalCc.getKey() != ContactChange.KEY_VCARD_TITLE) {
						if(VersionUtils.is2XPlatform()) {
							// Only checking on 2.X because 1.X does not guarantee sequential ids 
							// for the details (diff tables)
							assertEquals(previousDetailId+1, currentDetailId);
						}
					} else {
						// Org and title share nab detail id on both 1.X and 2.X!
						if(orgAndTitleNabDetailId == ContactChange.INVALID_ID) {
							orgAndTitleNabDetailId = currentDetailId;
						} else {
							assertEquals(orgAndTitleNabDetailId, currentDetailId);
						}
					}
				}
				previousDetailId = currentDetailId;
			} else {
				assertEquals(ContactChange.TYPE_UPDATE_NAB_CONTACT_ID, ids[i].getType());
				// NAB Contact ID check
				if(expectedNabId != ContactChange.INVALID_ID) {
					assertEquals(expectedNabId, ids[0].getNabContactId());
				}
			}
		}
	}
	
	/**
	 * Utility method to verify that received Ids for a update Contact are as expected 
	 */
	private void verifyUpdateContactIds(ContactChange[] contactCcList, ContactChange[] updateCcList, ContactChange[] ids) {
		if(updateCcList == null || updateCcList.length == 0) {
			return;
		}
		assertNotNull(ids);
		final int idsSize = ids.length;
		assertTrue(idsSize > 0);
		final int updatedCcListSize = updateCcList.length;
		assertEquals(updatedCcListSize, idsSize);
		final long nabContactId = updateCcList[0].getNabContactId();
		for(int i = 0 ; i < updatedCcListSize; i++) {
			if(updateCcList[i].getType() == ContactChange.TYPE_ADD_DETAIL) {
				final int key = updateCcList[i].getKey();
				if(VersionUtils.is2XPlatform() || 
						(key != ContactChange.KEY_VCARD_NAME && 
								key != ContactChange.KEY_VCARD_NOTE)) {
				assertNotNull(ids[i]);
				assertEquals(ContactChange.TYPE_UPDATE_NAB_DETAIL_ID, ids[i].getType());
				assertTrue(ids[i].getNabDetailId() != ContactChange.INVALID_ID);
				assertEquals(nabContactId, ids[i].getNabContactId());
				}
			} else {
				assertNull(ids[i]);
			}
		}		
	}
	
	private void waitForAccountsChange(AccountsObserver observer, int expectedNumAccounts) {
		final int maxTimeSleep = 1000000;
		int timeslept = 0;
		while(timeslept < maxTimeSleep) {
			try {
				Thread.sleep(100);
				timeslept+=100;
			} catch (InterruptedException e) {
				assertEquals(expectedNumAccounts, observer.getCurrentNumAccounts());
			}
		}
	}
}
