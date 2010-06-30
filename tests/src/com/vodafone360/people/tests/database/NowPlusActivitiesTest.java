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
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

public class NowPlusActivitiesTest extends ApplicationTestCase<MainApplication> {

	private static String LOG_TAG = "NowPlusActivitiesTest";
	final static int WAIT_EVENT_TIMEOUT_MS = 30000;
	
	final static int NUM_OF_CONTACTS = 100;

	private static MainApplication mApplication = null;
	private static DatabaseHelper mDatabase = null;
	final TestModule mTestModule = new TestModule();
	private DbTestUtility mTestUtility;
	
	public NowPlusActivitiesTest() {
		super(MainApplication.class);
	}

	public void setUp() {
		mTestUtility = new DbTestUtility(getContext());
		
    	createApplication();
		mApplication = getApplication();
		
		if(mApplication == null){
			throw(new RuntimeException("Unable to create main application"));
		}
		mDatabase = mApplication.getDatabase();
		mTestUtility.startEventWatcher(mDatabase);
	}

	public void tearDown() {
		mTestUtility.stopEventWatcher();
		mDatabase.getReadableDatabase().close();
	}

	@MediumTest
    public void test1() {
		Log.i(LOG_TAG, "***** EXECUTING test1 *****");
		Log.i(LOG_TAG, "<description here>");
		
		Log.i(LOG_TAG, "Test 1a: Remove user data");
		mDatabase.removeUserData();
		mDatabase.fetchThumbnailUrlCount();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		if (ServiceStatus.SUCCESS != status) {
			throw(new RuntimeException("No database change event received after removing user data, error: " + status));
		}
		
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "test1 has completed successfully");
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "");
    }
}
		

