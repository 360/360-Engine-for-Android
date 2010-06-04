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

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.tests.TestModule;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.ApplicationTestCase;

public abstract class NowPlusTableTestCase extends	ApplicationTestCase<MainApplication> {

	protected static String LOG_TAG = "NowPlusTableTestCase";

	/**
	 * Helper module to generate test content.
	 */
	protected final TestModule mTestModule = new TestModule();
	
	/**
	 * A simple test database.
	 */
	protected TestDatabase mTestDatabase;
	
	public NowPlusTableTestCase() {
		super(MainApplication.class);
	}
	
	
	/**
	 * the method sets up the MainApplication instance, the test database instance, and test module
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		createApplication();
	    
	    mTestDatabase = new TestDatabase(getContext());
	}

	/**
	 * This method deletes the tables, and releases the MainApplication
	 */
	protected void tearDown() throws Exception {
		mTestDatabase.close();
		
	    getContext().deleteDatabase(TestDatabase.DATA_BASE_NAME);
	        
	    // make sure to call it at the end of the method!
		super.tearDown();
	}

    ////////////////////////////////
    // HELPER CLASSES AND METHODS //
    ////////////////////////////////
    
    /**
     * A simple test database.
     */
    protected static class TestDatabase extends SQLiteOpenHelper {
        
        public final static String DATA_BASE_NAME = "TEST_DB";

        public TestDatabase(Context context) {
            super(context, DATA_BASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            
        }
    }

}
