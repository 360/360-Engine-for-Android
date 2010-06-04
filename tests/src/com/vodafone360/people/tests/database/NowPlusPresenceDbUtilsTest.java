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

import android.database.sqlite.SQLiteDatabase;

import com.vodafone360.people.database.tables.PresenceTable;

public class NowPlusPresenceDbUtilsTest /*extends NowPlusTableTestCase*/ {

	/**
	 * Deletes the table
	 * @param writableDatabase
	 */
	public static void dropTable(SQLiteDatabase writableDatabase) {
		writableDatabase.execSQL("DROP TABLE IF EXISTS " + PresenceTable.TABLE_NAME);
	}
	
	/*private DatabaseHelper mDatabaseHelper;
	private static MainApplication mApplication;

	public NowPlusPresenceDbUtilsTest() {
		super();
		createApplication();
		mApplication = getApplication();
		mDatabaseHelper = mApplication.getDatabase();
	}

	public void testConvertUserIds() {
		Hashtable<String, String> status = new Hashtable<String, String>();
		status.put("google", "online");
		status.put("microsoft", "online");
		status.put("mobile", "online");
		status.put("pc", "online");
		User user = new User("12345678", status); //imaginary Me Profile
		
		assertTrue(PresenceDbUtils.convertUserIds(user, mDatabaseHelper));
		assertTrue("the localContactid is not -1:" + user.getLocalContactId(), user.getLocalContactId() == -1);
	}

	public void testGetMeProfileUserId() {
		
		Long meProfileUserId = PresenceDbUtils.getMeProfileUserId(mDatabaseHelper);
		assertTrue("the localContactid is not -1:" + meProfileUserId, meProfileUserId == -1);
	}

	public void testGetMeProfilePresenceStatus() {
		fail("Not yet implemented");
	}

	public void testUpdateDatabase() {
		fail("Not yet implemented");
	}

	public void testSetPresenceOfflineInDatabase() {
		fail("Not yet implemented");
	}*/

}
