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
import java.util.Random;

import android.database.SQLException;
import android.util.Log;

import com.vodafone360.people.database.tables.ContactSourceTable;

public class ContactSourceTableTest extends NowPlusTableTestCase{
	private static final String LOG_TAG = "ContactSourceTableTest";
	private static final int NO_OF_CONTACT_IDS_TO_TEST = 10;
	private final String[] SOURCES = {"Zyb","Facebook","WindowsLiveWith very long name"};
	
	public ContactSourceTableTest() {
		super();
	}

	/**
	 * creates table that will be tested in this suite
	 */
	private void createTable() {
		try {
			ContactSourceTable.create(mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the table: " + e);
		}
	}
	
	public void testCreateContactSourceTable(){
		Log.i(LOG_TAG, "testCreateContactSourceTable start");
		try{
			createTable();
		}catch (Exception e) {
			fail("An exception occurred when testCreate: " + e);
		}
		Log.i(LOG_TAG, "testCreateContactSourceTable PASS");
	}
	
//	*addContactSource(long, String, SQLiteDatabase)
//	*create(SQLiteDatabase)
//	deleteAllContactSources(long, SQLiteDatabase)
//	*fetchContactSources(long, List<String>, SQLiteDatabase)
//	*private fillUpdateData(ContactSource)
	
	public void testTable(){
		Log.i(LOG_TAG, "testTable start");
		createTable();
		Random rand = new Random(System.currentTimeMillis());
		ArrayList<Long> localContactIdList = new ArrayList<Long>();
		for(int i=0;i<NO_OF_CONTACT_IDS_TO_TEST;i++){
			localContactIdList.add(rand.nextLong());
		}
		
		//test for adding and fetchting
		for(Long localContactId: localContactIdList){
			ArrayList<String> testList = new ArrayList<String>();
			for(String source: SOURCES){
				testList.add(source);
				assertTrue("addContactSource failed", 
				ContactSourceTable.addContactSource(localContactId, source, mTestDatabase.getWritableDatabase()) );
			}
			
			List<String> list = new ArrayList<String>(); 
			assertTrue("fetchContactSources failed", 
			ContactSourceTable.fetchContactSources(localContactId, list, mTestDatabase.getWritableDatabase()) );
			
			if(list.size() != SOURCES.length){
				fail("IN/OUT tables have diferent lengths");
			}
			if(!list.containsAll(testList)||!testList.containsAll(list)){
				fail("IN/OUT tables have diferent elements");
			}
		}
		
		//test for deleting and fetching
		for(Long localContactId: localContactIdList){
			assertTrue("deleteAllContactSources failed",
			ContactSourceTable.deleteAllContactSources(localContactId, mTestDatabase.getWritableDatabase()) );
			
			List<String> list = new ArrayList<String>(); 
			assertTrue("fetchContactSources failed",
			ContactSourceTable.fetchContactSources(localContactId, list, mTestDatabase.getWritableDatabase()) );
			
			if(list.size() != 0){
				fail("table size >0 after delete all sources");
			}
		}
		
		Log.i(LOG_TAG, "testTable PASS");
		
	}
}
