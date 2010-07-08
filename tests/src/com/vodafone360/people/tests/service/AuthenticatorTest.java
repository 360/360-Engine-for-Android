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

package com.vodafone360.people.tests.service;

import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.service.Authenticator;
import com.vodafone360.people.tests.testutils.TestStatus;

/***
 * Test the Authenticator class.
 */
public class AuthenticatorTest extends AndroidTestCase  {

    /** Instance of the Main Application. **/
    private MainApplication mApplication = null;

    /*
     * ##########  FRAMEWORK CLASSES.  ##########
     */
    /**
     * Load and start a real MainApplication instance using the current
     * context, as if it was started by the system.
     *
     * @throws Exception Anything related to starting the MainApplication
     *         class.
     */
    @Override
    protected final void setUp() throws Exception {
        /** Setup the MainApplication. **/
        mApplication = (MainApplication) Instrumentation.newApplication(MainApplication.class,
                getContext());
        assertNotNull("Newly created MainApplication class should not be NULL", mApplication);
        mApplication.onCreate();

        super.setUp();
    }

    /**
     * Shuts down the Application under test.  Also makes sure all resources
     * are cleaned up and garbage collected before moving on to the next test.
     * Subclasses that override this method should make sure they call
     * super.tearDown() at the end of the overriding method.
     *
     * @throws Exception Cannot terminate the application.
     */
    @Override
    protected final void tearDown() throws Exception {
        if (mApplication != null) {
            mApplication.onTerminate();
            mApplication = null;
        }

        super.tearDown();
    }
    /*
     * ##########  END FRAMEWORK CLASSES.  ##########
     */

    /***
     * Test the Authenticator constructor.
     */
    @SmallTest
    public final void testAuthenticator() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        assertNotNull("Authenticator should not be NULL", authenticator);
    }

    /***
     * Test the ACTION_ONE_ACCOUNT_ONLY_INTENT string.
     */
    @SmallTest
    public final void testActionOneAccountOnlyIntent() {
        assertEquals("ACTION_ONE_ACCOUNT_ONLY_INTENT returns the wrong value",
                "com.vodafone360.people.android.account.ONE_ONLY",
                Authenticator.ACTION_ONE_ACCOUNT_ONLY_INTENT);
    }

    /***
     * Test the getParcelable() method.
     */
    @SmallTest
    public final void testAddAccount() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        NativeContactsApi.createInstance(getContext());        
        Bundle bundle = authenticator.addAccount(null, null, null, null, null);
        Intent intent = (Intent) bundle.getParcelable(AccountManager.KEY_INTENT);
        assertEquals("Expected a StartActivity Intent",
                intent.getComponent().getClassName(),
                "com.vodafone360.people.ui.StartActivity");
        assertNull("Expected a ACTION_ONE_ACCOUNT_ONLY_INTENT action to be NULL",
                intent.getAction());
    }

    /***
     * Test the getAccountRemovalAllowed() method.
     */
    @SmallTest
    @Suppress
    public final void testGetAccountRemovalAllowed() {
        final TestStatus testStatus = new TestStatus();
        Authenticator authenticator = new Authenticator(getContext(), new MainApplication() {
            public void removeUserData() {
                /*
                 * Test that the dummy mWakeListener.notifyOfWakeupAlarm() has
                 * been called, otherwise the test must fail.
                 */
                testStatus.setPass(true);
            }
        });

        Bundle bundle = null;
        try {
            bundle = authenticator.getAccountRemovalAllowed(null, null);
        } catch (NetworkErrorException e) {
            fail("Unexpected NetworkErrorException");
        }

        /** Test if removeUserData() was called. **/
        assertTrue("Expecting the removeUserData() dummy method to have "
                + "been called", testStatus.isPass());
        assertEquals("Expected a KEY_BOOLEAN_RESULT boolean", true,
                bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
    }

    /***
     * Test the confirmCredentials() method.
     */
    @SmallTest
    public final void testConfirmCredentials() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        assertNull("Expected confirmCredentials() to return NULL",
                authenticator.confirmCredentials(null, null, null));
    }

    /***
     * Test the editProperties() method.
     */
    @SmallTest
    public final void testEditProperties() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        try {
            authenticator.editProperties(null, null);
            fail("Expected editProperties() to throw an UnsupportedOperationException");

        } catch (UnsupportedOperationException e) {
            assertTrue("UnsupportedOperationException thrown as expected", true);

        } catch (Exception e) {
            fail("Expected editProperties() to throw an UnsupportedOperationException");
        }
    }

    /***
     * Test the getAuthToken() method.
     */
    @SmallTest
    public final void testGetAuthToken() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        assertNull("Expected getAuthToken() to return NULL",
                authenticator.getAuthToken(null, null, null, null));
    }

    /***
     * Test the getAuthTokenLabel() method.
     */
    @SmallTest
    public final void testGetAuthTokenLabel() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        assertNull("Expected getAuthTokenLabel() to return NULL",
                authenticator.getAuthTokenLabel(null));
    }

    /***
     * Test the hasFeatures() method.
     */
    @SmallTest
    public final void testHasFeatures() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        assertFalse("Expected hasFeatures() to return bundle boolean KEY_BOOLEAN_RESULT as FALSE",
                authenticator.hasFeatures(null, null, null)
                .getBoolean(AccountManager.KEY_BOOLEAN_RESULT));
    }

    /***
     * Test the updateCredentials() method.
     */
    @SmallTest
    public final void testUpdateCredentials() {
        Authenticator authenticator = new Authenticator(getContext(), mApplication);
        assertNull("Expected updateCredentials() to return NULL",
                authenticator.updateCredentials(null, null, null, null));
    }
}
