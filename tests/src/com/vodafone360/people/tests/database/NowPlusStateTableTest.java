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

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.engine.login.RSAEncryptionUtils;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.tests.TestModule;

public class NowPlusStateTableTest extends NowPlusTableTestCase {
    final TestModule mTestModule = new TestModule();

    private int mTestStep = 0;

    private static String LOG_TAG = "NowPlusStateTableTest";

    public NowPlusStateTableTest() {
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
            StateTable.create(mTestDatabase.getWritableDatabase());
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

    /*
     * Modify credentials then fetch modified credentials and validate it
     */
    @MediumTest
    public void testModifyCredentials() {
        final String fnName = "testModifyCredentials";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "modfiy and fetchmodified credentials");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        LoginDetails loginDetails = new LoginDetails();

        RegistrationDetails registrationDetails = new RegistrationDetails();
        registrationDetails.mUsername = TestModule.generateRandomString();
        registrationDetails.mPassword = TestModule.generateRandomString();
        registrationDetails.mMsisdn = TestModule.generateRandomString();

        loginDetails.mUsername = registrationDetails.mUsername;
        loginDetails.mPassword = registrationDetails.mPassword;
        loginDetails.mAutoConnect = true;
        loginDetails.mRememberMe = true;
        loginDetails.mMobileNo = registrationDetails.mMsisdn;
        loginDetails.mSubscriberId = TestModule.generateRandomString();

        // try to modify credentials before creating a table causes uncaught
        // exception
        ServiceStatus status = StateTable.modifyCredentials(loginDetails, writableDb);
        assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

        createTable();

        // positive test modify credentials after table was created
        status = StateTable.modifyCredentials(loginDetails, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);
        LoginDetails fetchedLoginDetails = new LoginDetails();
        status = StateTable.fetchLogonCredentials(fetchedLoginDetails, readableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertEquals(loginDetails.mUsername, fetchedLoginDetails.mUsername);
        assertEquals(loginDetails.mPassword, fetchedLoginDetails.mPassword);
        assertEquals(loginDetails.mAutoConnect, fetchedLoginDetails.mAutoConnect);
        assertEquals(loginDetails.mRememberMe, fetchedLoginDetails.mRememberMe);
        assertEquals(loginDetails.mMobileNo, fetchedLoginDetails.mMobileNo);
        assertEquals(loginDetails.mSubscriberId, fetchedLoginDetails.mSubscriberId);

        loginDetails.mRememberMe = false;
        status = StateTable.modifyCredentials(loginDetails, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);
        LoginDetails modifedLoginDetails = new LoginDetails();
        status = StateTable.fetchLogonCredentials(modifedLoginDetails, readableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertEquals(loginDetails.mUsername, modifedLoginDetails.mUsername);
        assertNull(modifedLoginDetails.mPassword); // mRemberme was set to false
        // so password should be null
        assertEquals(loginDetails.mAutoConnect, modifedLoginDetails.mAutoConnect);
        assertEquals(loginDetails.mRememberMe, modifedLoginDetails.mRememberMe);
        assertEquals(loginDetails.mMobileNo, modifedLoginDetails.mMobileNo);
        assertEquals(loginDetails.mSubscriberId, modifedLoginDetails.mSubscriberId);

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "testModifyCredentials has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * Modify credentials and public key then fetch modified credentials and
     * validate it
     */
    @MediumTest
    public void testModifyCredentialsAndKey() {
        final String fnName = "testModifyCredentialsAndKey";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "modfiy and fetchmodified credentials");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        LoginDetails loginDetails = new LoginDetails();

        RegistrationDetails registrationDetails = new RegistrationDetails();
        registrationDetails.mUsername = TestModule.generateRandomString();
        registrationDetails.mPassword = TestModule.generateRandomString();
        registrationDetails.mMsisdn = TestModule.generateRandomString();

        loginDetails.mUsername = registrationDetails.mUsername;
        loginDetails.mPassword = registrationDetails.mPassword;
        loginDetails.mAutoConnect = true;
        loginDetails.mRememberMe = true;
        loginDetails.mMobileNo = registrationDetails.mMsisdn;
        loginDetails.mSubscriberId = TestModule.generateRandomString();

        PublicKeyDetails pubKeyDetails = new PublicKeyDetails();
        RSAEncryptionUtils.copyDefaultPublicKey(pubKeyDetails);
        // try to modify credentials before creating a table causes uncaught
        // exception
        ServiceStatus status = StateTable.modifyCredentialsAndPublicKey(loginDetails,
                pubKeyDetails, writableDb);
        assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

        createTable();

        // positive test modify credentials after table was created
        status = StateTable.modifyCredentialsAndPublicKey(loginDetails, pubKeyDetails, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);
        PublicKeyDetails fetchedPubKey = new PublicKeyDetails();
        LoginDetails fetchedLoginDetails = new LoginDetails();
        status = StateTable.fetchLogonCredentialsAndPublicKey(fetchedLoginDetails, fetchedPubKey,
                readableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertEquals(loginDetails.mUsername, fetchedLoginDetails.mUsername);
        assertEquals(loginDetails.mPassword, fetchedLoginDetails.mPassword);
        assertEquals(loginDetails.mAutoConnect, fetchedLoginDetails.mAutoConnect);
        assertEquals(loginDetails.mRememberMe, fetchedLoginDetails.mRememberMe);
        assertEquals(loginDetails.mMobileNo, fetchedLoginDetails.mMobileNo);
        assertEquals(loginDetails.mSubscriberId, fetchedLoginDetails.mSubscriberId);

        assertEquals(pubKeyDetails.mExponential.length, fetchedPubKey.mExponential.length);
        for (int i = 0; i < pubKeyDetails.mExponential.length; i++) {
            assertEquals(pubKeyDetails.mExponential[i], fetchedPubKey.mExponential[i]);
        }

        assertEquals(pubKeyDetails.mModulus.length, fetchedPubKey.mModulus.length);
        for (int i = 0; i < fetchedPubKey.mModulus.length; i++) {
            assertEquals(pubKeyDetails.mModulus[i], fetchedPubKey.mModulus[i]);
        }

        loginDetails.mRememberMe = false;
        pubKeyDetails.mKeyBase64 = TestModule.generateRandomString();
        status = StateTable.modifyCredentialsAndPublicKey(loginDetails, pubKeyDetails, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);
        LoginDetails modifedLoginDetails = new LoginDetails();
        PublicKeyDetails modifiedPubKey = new PublicKeyDetails();
        status = StateTable.fetchLogonCredentialsAndPublicKey(modifedLoginDetails, modifiedPubKey,
                readableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertEquals(loginDetails.mUsername, modifedLoginDetails.mUsername);
        assertNull(modifedLoginDetails.mPassword); // mRemberme was set to false
        // so password should be null
        assertEquals(loginDetails.mAutoConnect, modifedLoginDetails.mAutoConnect);
        assertEquals(loginDetails.mRememberMe, modifedLoginDetails.mRememberMe);
        assertEquals(loginDetails.mMobileNo, modifedLoginDetails.mMobileNo);
        assertEquals(loginDetails.mSubscriberId, modifedLoginDetails.mSubscriberId);

        assertEquals(pubKeyDetails.mExponential.length, modifiedPubKey.mExponential.length);
        for (int i = 0; i < pubKeyDetails.mExponential.length; i++) {
            assertEquals(pubKeyDetails.mExponential[i], fetchedPubKey.mExponential[i]);
        }
        assertEquals(pubKeyDetails.mModulus.length, modifiedPubKey.mModulus.length);
        for (int i = 0; i < fetchedPubKey.mModulus.length; i++) {
            assertEquals(pubKeyDetails.mModulus[i], fetchedPubKey.mModulus[i]);
        }
        assertEquals(pubKeyDetails.mKeyBase64, modifiedPubKey.mKeyBase64);

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "testModifyCredentialsAndKey has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * tests for Registration Complete methods
     */
    @SmallTest
    public void testRegistrationComplete() {
        final String fnName = "testModifyCredentialsAndKey";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "modfiy and fetchmodified credentials");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        // try to setRegistrationComplete before creating a table
        boolean isRegistrationComplete = false;
        assertFalse(StateTable.setRegistrationComplete(isRegistrationComplete, writableDb));
        assertFalse(StateTable.isRegistrationComplete(readableDb));

        createTable();

        assertTrue(StateTable.setRegistrationComplete(isRegistrationComplete, writableDb));
        assertEquals(isRegistrationComplete, StateTable.isRegistrationComplete(readableDb));

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "testRegistrationComplete has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * modify and fetch contact revision
     */
    @SmallTest
    public void testContactRevision() {
        final String fnName = "testContactRevision";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "test setting and fetching contact revision");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        Integer revision = TestModule.generateRandomInt();
        assertFalse(StateTable.modifyContactRevision(revision, writableDb));
        assertNull(StateTable.fetchContactRevision(readableDb));

        createTable();

        assertNull(StateTable.fetchContactRevision(readableDb));
        assertTrue(StateTable.modifyContactRevision(revision, writableDb));
        assertEquals(revision, StateTable.fetchContactRevision(readableDb));

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "testContactRevision has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * set and fetch option
     */
    public void testSetFetchOption() {
        final String fnName = "testContactRevision";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "test setting and fetching option");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        PersistSettings setting = new PersistSettings();
        setting.putInternetAvail(InternetAvail.ALWAYS_CONNECT);

        // try to set and fetch option before creating a table
        ServiceStatus status = StateTable.setOption(setting, writableDb);
        assertNull(status);

        PersistSettings fetchedSetting = StateTable.fetchOption(
                PersistSettings.Option.INTERNETAVAIL, readableDb);
        assertNull(fetchedSetting);

        createTable();

        status = StateTable.setOption(setting, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);
        fetchedSetting = StateTable.fetchOption(PersistSettings.Option.INTERNETAVAIL, readableDb);
        assertEquals(fetchedSetting.toString(), setting.toString());

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "testSetFetchOption has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * set and fetch session
     */
    public void testSetFetchSession() {
        final String fnName = "testSetFetchSession";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "test setting and fetching session");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        AuthSessionHolder session = new AuthSessionHolder();
        session.sessionID = TestModule.generateRandomString();
        session.sessionSecret = TestModule.generateRandomString();
        session.userID = TestModule.generateRandomInt();
        session.userName = TestModule.generateRandomString();

        ServiceStatus status = StateTable.setSession(session, writableDb);
        assertNull(status);

        AuthSessionHolder fetchedSession = StateTable.fetchSession(readableDb);
        assertNull(fetchedSession);
        createTable();

        status = StateTable.setSession(session, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        fetchedSession = StateTable.fetchSession(readableDb);
        assertEquals(session.toString(), fetchedSession.toString());

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "testSetFetchSession has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * modify and fetch MeProfileId
     */
    public void testMeProfileId() {
        final String fnName = "testMeProfileId";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "modify and fetching MeProfileId");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        Long myContactID = TestModule.generateRandomLong();
        ServiceStatus status = StateTable.modifyMeProfileID(myContactID, writableDb);
        assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

        Long fetchedMeProfileId = StateTable.fetchMeProfileId(readableDb);
        assertNull(fetchedMeProfileId);

        createTable();

        fetchedMeProfileId = StateTable.fetchMeProfileId(readableDb);
        assertNull(fetchedMeProfileId);

        status = StateTable.modifyMeProfileID(myContactID, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        fetchedMeProfileId = StateTable.fetchMeProfileId(readableDb);

        assertEquals(myContactID, fetchedMeProfileId);

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "modify and fetching MeProfileId has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * modify and fetch Me profile change flag
     */
    public void testMeProfileChangeFlag() {
        final String fnName = "testMeProfileChangeFlag";
        mTestStep = 1;

        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "modify and fetching change flag");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        boolean myContactChanged = TestModule.generateRandomBoolean();
        ServiceStatus status = StateTable.modifyMeProfileChangedFlag(myContactChanged, writableDb);
        assertEquals(ServiceStatus.ERROR_DATABASE_CORRUPT, status);

        boolean fetchMeProfileChangedFlag = StateTable.fetchMeProfileChangedFlag(readableDb);
        assertFalse(fetchMeProfileChangedFlag);

        createTable();

        fetchMeProfileChangedFlag = StateTable.fetchMeProfileChangedFlag(readableDb);
        assertFalse(fetchMeProfileChangedFlag);

        status = StateTable.modifyMeProfileChangedFlag(myContactChanged, writableDb);
        assertEquals(ServiceStatus.SUCCESS, status);

        fetchMeProfileChangedFlag = StateTable.fetchMeProfileChangedFlag(readableDb);

        assertEquals(myContactChanged, fetchMeProfileChangedFlag);

        Log.i(LOG_TAG, "*************************************");
        Log.i(LOG_TAG, "modify and fetching MeProfile change flag  has completed successfully");
        Log.i(LOG_TAG, "**************************************");
    }

    /*
     * Test fetch and modify last status update.
     */
    public void testLastStatusUpdate() {
        mTestStep = 1;
        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        long myLastStatusUpdate = TestModule.generateRandomLong();
        
        try {
            StateTable.modifyLatestStatusUpdateTime(myLastStatusUpdate, writableDb);
            fail("Expecting a SQLiteException, because the table has not yet been created");
        } catch (SQLiteException e) {
            assertTrue(true);
        }

        try {
            StateTable.fetchLatestStatusUpdateTime(readableDb);
            fail("Expecting a SQLiteException, because the table has not yet been created");
        } catch (SQLiteException e) {
            assertTrue(true);
        }

        createTable();

        assertEquals(0, StateTable.fetchLatestStatusUpdateTime(readableDb));
        assertEquals(ServiceStatus.SUCCESS, StateTable.modifyLatestStatusUpdateTime(myLastStatusUpdate, writableDb));
        assertEquals(myLastStatusUpdate, StateTable.fetchLatestStatusUpdateTime(readableDb));
    }

    /*
     * Test fetch and modify LastTimelineUpdate.
     */
    public void testLastTimelineUpdate() {
        mTestStep = 1;
        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();
        long myLastTimelineUpdate = TestModule.generateRandomLong();

        try {
            StateTable.modifyLatestPhoneCallTime(myLastTimelineUpdate, writableDb);
            fail("Expecting a SQLiteException, because the table has not yet been created");
        } catch (SQLiteException e) {
            assertTrue(true);
        }
        
        try {
            StateTable.fetchLatestPhoneCallTime(readableDb);
            fail("Expecting a SQLiteException, because the table has not yet been created");
        } catch (SQLiteException e) {
            assertTrue(true);
        }

        createTable();

        assertEquals(0, StateTable.fetchLatestPhoneCallTime(readableDb));
        assertEquals(ServiceStatus.SUCCESS, StateTable.modifyLatestPhoneCallTime(myLastTimelineUpdate, writableDb));
        assertEquals(myLastTimelineUpdate, StateTable.fetchLatestPhoneCallTime(readableDb));
    }

    /*
     * test fetch and modify profile revision
     */
    @Suppress
    public void testMeProfileRevision() {
        final String fnName = "testMeProfileRevision";
        mTestStep = 1;
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + "*****");
        Log.i(LOG_TAG, "modify and fetching me profile revision");

        SQLiteDatabase writableDb = mTestDatabase.getWritableDatabase();
        SQLiteDatabase readableDb = mTestDatabase.getReadableDatabase();

        int meProfileRevision = TestModule.generateRandomInt();
        assertFalse(StateTable.modifyMeProfileRevision(meProfileRevision, writableDb));

        assertNull(StateTable.fetchMeProfileRevision(readableDb));

        createTable();

        assertNull(StateTable.fetchMeProfileRevision(readableDb));

        assertTrue(StateTable.modifyMeProfileRevision(meProfileRevision, writableDb));

        long fetchedMeProfileRevision = StateTable.fetchMeProfileRevision(readableDb);
        assertEquals(meProfileRevision, fetchedMeProfileRevision);
    }
}
