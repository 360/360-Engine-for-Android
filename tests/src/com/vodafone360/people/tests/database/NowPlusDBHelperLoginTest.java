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

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.engine.login.RSAEncryptionUtils;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.tests.TestModule;

public class NowPlusDBHelperLoginTest extends ApplicationTestCase<MainApplication> {

	private static String LOG_TAG = "NowPlusDatabaseTest";
	final static int WAIT_EVENT_TIMEOUT_MS = 30000;
	
	final static int NUM_OF_CONTACTS = 6;

	private static MainApplication mApplication = null;
	private static DatabaseHelper mDatabaseHelper = null;
	final TestModule mTestModule = new TestModule();
	private DbTestUtility mTestUtility;
	
	public NowPlusDBHelperLoginTest() {
		super(MainApplication.class);
	}

	private boolean initialise() {
		mTestUtility = new DbTestUtility(getContext());

		createApplication();
		mApplication = getApplication();

		if (mApplication == null) {
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
	
	/*
	 * Modify and fetch credentials  
	 */
	@MediumTest
	public void testModifyFetchCredentials() {
		Log.i(LOG_TAG, "***** EXECUTING testAddDeleteContactsDetails *****");
		Log.i(LOG_TAG, "Test contact functionality (add delete contacts details)");
		Log.i(LOG_TAG,"Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS,
				DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);

		RegistrationDetails registrationDetails = new RegistrationDetails();
		registrationDetails.mUsername = TestModule.generateRandomString();
		registrationDetails.mPassword = TestModule.generateRandomString();
		registrationDetails.mMsisdn = TestModule.generateRandomString();

		LoginDetails loginDetails = new LoginDetails();
		loginDetails.mUsername = registrationDetails.mUsername;
		loginDetails.mPassword = registrationDetails.mPassword;
		loginDetails.mAutoConnect = TestModule.generateRandomBoolean();
		loginDetails.mRememberMe = TestModule.generateRandomBoolean();
		loginDetails.mMobileNo = registrationDetails.mMsisdn;
		loginDetails.mSubscriberId = TestModule.generateRandomString();

		LoginDetails fetchedLogin = new LoginDetails();
		status = mDatabaseHelper.fetchLogonCredentials(fetchedLogin);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		assertNull(fetchedLogin.mUsername);
		assertNull(fetchedLogin.mPassword);

		status = mDatabaseHelper.modifyCredentials(loginDetails);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		status = mDatabaseHelper.fetchLogonCredentials(fetchedLogin);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		assertEquals(loginDetails.mUsername, fetchedLogin.mUsername);
		if (loginDetails.mRememberMe) {
			assertEquals(loginDetails.mPassword, fetchedLogin.mPassword);
		}
		assertEquals(loginDetails.mAutoConnect, fetchedLogin.mAutoConnect);
		assertEquals(loginDetails.mRememberMe, fetchedLogin.mRememberMe);
		assertEquals(loginDetails.mMobileNo, fetchedLogin.mMobileNo);
		assertEquals(loginDetails.mSubscriberId, fetchedLogin.mSubscriberId);
		
		shutdown();

		Log.i(LOG_TAG, "*************************************");
		Log.i(LOG_TAG, "testModifyFetchCredentials has completed successfully");
		Log.i(LOG_TAG, "**************************************");
	}
	
	/*
	 * Modify credentials and public key then fetch modified credentials and validate it
	 */
	@MediumTest
	public void testModifyCredentialsAndKey() {
		Log.i(LOG_TAG, "***** EXECUTING testAddDeleteContactsDetails *****");
		Log.i(LOG_TAG, "Test contact functionality (add delete contacts details)");
		Log.i(LOG_TAG,"Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS,
				DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);

		RegistrationDetails registrationDetails = new RegistrationDetails();
		registrationDetails.mUsername = TestModule.generateRandomString();
		registrationDetails.mPassword = TestModule.generateRandomString();
		registrationDetails.mMsisdn = TestModule.generateRandomString();

		LoginDetails loginDetails = new LoginDetails();
		loginDetails.mUsername = registrationDetails.mUsername;
		loginDetails.mPassword = registrationDetails.mPassword;
		loginDetails.mAutoConnect = TestModule.generateRandomBoolean();
		loginDetails.mRememberMe = TestModule.generateRandomBoolean();
		loginDetails.mMobileNo = registrationDetails.mMsisdn;
		loginDetails.mSubscriberId = TestModule.generateRandomString();
		
		PublicKeyDetails pubKeyDetails = new PublicKeyDetails();
		RSAEncryptionUtils.copyDefaultPublicKey(pubKeyDetails);
		pubKeyDetails.mKeyBase64 = TestModule.generateRandomString();
		
		LoginDetails fetchedLogin = new LoginDetails();
		PublicKeyDetails fetchedPubKey = new PublicKeyDetails();
		status = mDatabaseHelper.fetchLogonCredentialsAndPublicKey(
				fetchedLogin, fetchedPubKey);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		assertNull(fetchedLogin.mUsername);
		assertNull(fetchedLogin.mPassword);

		assertNull(fetchedPubKey.mKeyBase64);
		assertNull(fetchedPubKey.mExponential);
		assertNull(fetchedPubKey.mModulus);
		
		status = mDatabaseHelper.modifyCredentialsAndPublicKey(loginDetails, pubKeyDetails);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		status = mDatabaseHelper.fetchLogonCredentialsAndPublicKey(fetchedLogin, fetchedPubKey);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		assertEquals(loginDetails.mUsername, fetchedLogin.mUsername);
		if (loginDetails.mRememberMe) {
			assertEquals(loginDetails.mPassword, fetchedLogin.mPassword);
		}
		assertEquals(loginDetails.mAutoConnect, fetchedLogin.mAutoConnect);
		assertEquals(loginDetails.mRememberMe, fetchedLogin.mRememberMe);
		assertEquals(loginDetails.mMobileNo, fetchedLogin.mMobileNo);
		assertEquals(loginDetails.mSubscriberId, fetchedLogin.mSubscriberId);
		
		assertEquals(pubKeyDetails.mExponential.length, fetchedPubKey.mExponential.length);
		for (int i = 0; i < pubKeyDetails.mExponential.length; i++) {
			assertEquals(pubKeyDetails.mExponential[i], fetchedPubKey.mExponential[i]);
		}
		assertEquals(pubKeyDetails.mModulus.length, fetchedPubKey.mModulus.length);
		for (int i = 0; i < fetchedPubKey.mModulus.length; i++) {
			assertEquals(pubKeyDetails.mModulus[i], fetchedPubKey.mModulus[i]);
		}
		assertEquals(pubKeyDetails.mKeyBase64, fetchedPubKey.mKeyBase64);
		
		shutdown();

		Log.i(LOG_TAG, "*************************************");
		Log.i(LOG_TAG, "testModifyFetchCredentials has completed successfully");
		Log.i(LOG_TAG, "**************************************");		
	}
	
	/*
	 * set and fetch option
	 */
	public void testSetFetchOption() {
		Log.i(LOG_TAG, "***** EXECUTING testAddDeleteContactsDetails *****");
		Log.i(LOG_TAG, "Test contact functionality (add delete contacts details)");
		Log.i(LOG_TAG,"Test 1a: Initialise test environment and load database");
		assertTrue(initialise());
		Log.i(LOG_TAG, "Test 1b: Remove user data");
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS,
				DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);

		PersistSettings setting = new PersistSettings();
		setting.putInternetAvail(InternetAvail.ALWAYS_CONNECT);
		
		status = mDatabaseHelper.setOption(setting);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		PersistSettings fetchedSetting = mDatabaseHelper
				.fetchOption(PersistSettings.Option.INTERNETAVAIL);
		assertEquals(fetchedSetting.toString(),setting.toString());
		
		shutdown();

		Log.i(LOG_TAG, "*************************************");
		Log.i(LOG_TAG, "testModifyFetchCredentials has completed successfully");
		Log.i(LOG_TAG, "**************************************");		
	}
}