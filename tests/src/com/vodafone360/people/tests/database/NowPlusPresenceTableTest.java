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

import java.util.Hashtable;

import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.database.tables.PresenceTable;
import com.vodafone360.people.engine.presence.User;
import com.vodafone360.people.utils.LogUtils;

@Suppress
public class NowPlusPresenceTableTest extends NowPlusTableTestCase {

	public NowPlusPresenceTableTest() {
		super();
	}

	public void testCreate() {
		Log.i(LOG_TAG, "***** testCreateTable *****");
		mTestDatabase.getWritableDatabase().execSQL("ATTACH DATABASE ':memory:' AS presence1_db;");
		PresenceTable.create(mTestDatabase.getWritableDatabase());
		Log.i(LOG_TAG, "***** testCreateTable SUCCEEDED*****");
	}

	public void testUpdateUser() {
		Log.i(LOG_TAG, "***** testUpdateUser *****");
		PresenceTable.create(mTestDatabase.getWritableDatabase());
		Log.i(LOG_TAG, "***** testUpdateUser: table created*****");
		
		assertTrue("The method adds a null user and returns true", PresenceTable.updateUser(null, null, mTestDatabase.getWritableDatabase())==PresenceTable.USER_NOTADDED);
		Log.i(LOG_TAG, "***** testUpdateUser: NULL test SUCCEEDED *****");
		
		Hashtable<String, String> status = new Hashtable<String, String>();
		status.put("google", "online");
		status.put("microsoft", "online");
		status.put("mobile", "online");
		status.put("pc", "online");
		User user = new User("google::meongoogletalk@gmail.com", status);
		user.setLocalContactId(12L);// fake localId
		assertTrue("the user was not added to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) != PresenceTable.USER_NOTADDED);
		Log.i(LOG_TAG, "***** testUpdateUser Good User test SUCCEEDED*****");
		user = null;
		
// 		now, update the user
		status.put("google", "offline");
		status.put("microsoft", "online");
		status.put("mobile", "online");
		status.put("pc", "offline");
		user = new User("google::meongoogletalk@gmail.com", status);
		user.setLocalContactId(12L);// fake localId
		assertTrue("the  Existing NowplusUser might be duplicated to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) == PresenceTable.USER_UPDATED);
		
		User user1 = PresenceTable.getUserPresenceByLocalContactId(12L, mTestDatabase.getReadableDatabase());
		assertTrue("the initial and fetched users are not the same!", user.equals(user1));

		Log.i(LOG_TAG, "***** testUpdateUser test SUCCEEDED*****");

	}

	public void testGetMeProfilePresenceById() {
		Log.i(LOG_TAG, "***** GetMeProfilePresenceById() *****");
		PresenceTable.create(mTestDatabase.getWritableDatabase());
		Log.i(LOG_TAG, "***** GetMeProfilePresenceById(): table created*****");
		
		Hashtable<String, String> status = new Hashtable<String, String>();
		status.put("google", "online");
		status.put("microsoft", "online");
		status.put("mobile", "online");
		status.put("pc", "online");
		User user = new User("12345678", status); //imaginary Me Profile
		user.setLocalContactId(12L);// fake localId
		
		assertTrue("the user was not added to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) == PresenceTable.USER_ADDED);
		Log.i(LOG_TAG, "***** testUpdateUser Good User test SUCCEEDED*****");
		
		User user1 = PresenceTable.getUserPresenceByLocalContactId(12L, mTestDatabase.getReadableDatabase());
		
		assertTrue("the initial and fetched users are not the same!", user.equals(user1));
		
		assertNull(PresenceTable.getUserPresenceByLocalContactId(-1L, mTestDatabase.getReadableDatabase()));
		
		Log.i(LOG_TAG, "***** GetMeProfilePresenceById() SUCCEEEDED*****");
	}
	


//	public void testDropTable() {
//		Log.i(LOG_TAG, "***** testDropTable() *****");
//		PresenceTable.create(mTestDatabase.getWritableDatabase());
//		Log.i(LOG_TAG, "***** testDropTable(): table created*****");
//		
//		Hashtable<String, String> status = new Hashtable<String, String>();
//		status.put("google", "online");
//		status.put("microsoft", "online");
//		status.put("mobile", "online");
//		status.put("pc", "online");
//		User user = new User("google::meongoogletalk@gmail.com", status);
//		assertTrue("the user was not added to DB", PresenceTable.updateUser(user, mTestDatabase.getWritableDatabase()) != PresenceTable.USER_NOTADDED);
//		user = null;
////		4
//		NowPlusPresenceDbUtilsTest.dropTable(mTestDatabase.getWritableDatabase());
//		Log.i(LOG_TAG, "***** testDropTable(): dropped table*****");
//		
//		PresenceTable.create(mTestDatabase.getWritableDatabase());
//		Log.i(LOG_TAG, "***** testDropTable(): table created again*****");
//		
//		int count = PresenceTable.setAllUsersOffline(mTestDatabase.getWritableDatabase());
//		assertTrue("The count of deleted rows is not the expected one:"+count, count == 0);
//		Log.i(LOG_TAG, "***** testDropTable() test SUCCEEDED*****");
//	}

	public void testSetAllUsersOffline() {
		Log.i(LOG_TAG, "***** testSetAllUsersOffline() *****");
		PresenceTable.create(mTestDatabase.getWritableDatabase());
		Log.i(LOG_TAG, "***** testSetAllUsersOffline(): table created*****");
		
		assertTrue("The method adds a null user and returns true", PresenceTable.updateUser(null, null, mTestDatabase.getWritableDatabase())==PresenceTable.USER_NOTADDED);
		Log.i(LOG_TAG, "***** testUpdateUser: NULL test SUCCEEDED *****");
//		1
		Hashtable<String, String> status = new Hashtable<String, String>();
		status.put("google", "online");
		status.put("microsoft", "online");
		status.put("mobile", "online");
//		status.put("pc", "online");
		User user = new User("google::meongoogletalk@gmail.com", status);
		user.setLocalContactId(12L);
		assertTrue("the user was not added to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) == PresenceTable.USER_ADDED);
		
		LogUtils.logE("User1:"+user.toString());
		user = null;
//4		
//		user = new User("UNPARSEBLE", status);
//		assertTrue("the UNPARSEBLE user was added to DB", PresenceTable.updateUser(user, mTestDatabase.getWritableDatabase())== PresenceTable.USER_NOTADDED);
//		user = null;
//4
		user = new User("12345678", status);
		user.setLocalContactId(13L);
		assertTrue("the NowplusUser user was not added to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase())== PresenceTable.USER_ADDED);
//		user = null;
//8		
//		status.put("pc", "offline");
//		user = new User("12345678", status);
		user.setLocalContactId(13L);
		LogUtils.logE("User2:"+user.toString());
		LogUtils.logE(user.toString());
		assertTrue("the  Existing NowplusUser might be duplicated to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) == PresenceTable.USER_UPDATED);
//8		
		int count = PresenceTable.setAllUsersOffline(mTestDatabase.getWritableDatabase());
		assertTrue("The count of deleted rows is not the expected one:"+count, count == 6);
		Log.i(LOG_TAG, "***** testSetAllUsersOffline() test SUCCEEDED*****");
	}

	public void testSetAllUsersOfflineExceptForMe() {
		Log.i(LOG_TAG, "***** testSetAllUsersOfflineExceptForMe() *****");
		PresenceTable.create(mTestDatabase.getWritableDatabase());
		Log.i(LOG_TAG, "***** testSetAllUsersOfflineExceptForMe(): table created*****");
		
		assertTrue("The method adds a null user and returns true", PresenceTable.updateUser(null, null, mTestDatabase.getWritableDatabase())==PresenceTable.USER_NOTADDED);
		Log.i(LOG_TAG, "***** testUpdateUser: NULL test SUCCEEDED *****");
//		1
		Hashtable<String, String> status = new Hashtable<String, String>();
		status.put("google", "online");
		status.put("microsoft", "online");
		status.put("mobile", "online");
//		status.put("pc", "online");
		User user = new User("google::meongoogletalk@gmail.com", status);
		user.setLocalContactId(12L);
		assertTrue("the user was not added to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) == PresenceTable.USER_ADDED);
		
		LogUtils.logE("User1:"+user.toString());
		user = null;
//4		
		user = new User("12345678", status);
		user.setLocalContactId(13L);
		assertTrue("the NowplusUser user was not added to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase())== PresenceTable.USER_ADDED);
//		user = null;
//8		
//		status.put("pc", "offline");
//		user = new User("12345678", status);
		user.setLocalContactId(13L);
		LogUtils.logE("User2:"+user.toString());
		LogUtils.logE(user.toString());
		assertTrue("the  Existing NowplusUser might be duplicated to DB", PresenceTable.updateUser(user, null, mTestDatabase.getWritableDatabase()) == PresenceTable.USER_UPDATED);
//8		
		int count = PresenceTable.setAllUsersOfflineExceptForMe(12L, mTestDatabase.getWritableDatabase());
		assertTrue("The count of deleted rows is not the expected one:"+count, count == 3);
		Log.i(LOG_TAG, "***** testSetAllUsersOfflineExceptForMe() test SUCCEEDED*****");
	}
	
}
