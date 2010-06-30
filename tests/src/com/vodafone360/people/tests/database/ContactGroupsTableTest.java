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
import java.util.Random;

import android.database.SQLException;
import android.util.Log;

import com.vodafone360.people.database.tables.ContactGroupsTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.service.ServiceStatus;

public abstract class ContactGroupsTableTest extends NowPlusTableTestCase {
	private static final String LOG_TAG = "ContactGroupsTableTest";
	
	private final Long[][] testTable = { {1L,2L}, {13L,21L}, {31L,22L}, {1L,23L},
								   {12L,21L}, {21L,52L}, {31L,92L}, {1L,25L},
								   {12L,22L}, {21L,26L}, {41L,28L}, {1L,23L},
								   {12L,23L}, {21L,28L}, {51L,24L}, {1L,26L} };
	
	public ContactGroupsTableTest() {
		super();
	}
	
	private ArrayList<Long> getUniqueIdListFromTestTable(){
		ArrayList<Long> list = new ArrayList<Long>();
		
		for(int i=0; i<testTable.length;i++){
			if( !list.contains(testTable[i][0]) ){
				list.add(testTable[i][0]);
			}
		}
		
		return list;
	}
	
	private ArrayList<Long> getEntriesWithId(Long id){
		ArrayList<Long> ret = new ArrayList<Long>();
		for(int i=0;i<testTable.length;i++){
			if(testTable[i][0].equals(id)){
				ret.add(testTable[i][1]);
			}
		}
		return ret;
	}
	
	private int countEntriesWithId(Long id){
		int ret = 0;
		for(int i=0;i<testTable.length;i++){
			if(testTable[i][0].equals(id)){
				ret++;
			}
		}
		return ret;
	}
	
	/**
	 * creates table that will be tested in this suite
	 */
	private void createTable() {
		try {
			ContactGroupsTable.create( mTestDatabase.getWritableDatabase());
		} catch (SQLException e) {
			fail("An exception occurred when creating the table: " + e);
		}
	}
	
	public void testCreateGroupsTable(){
		Log.i(LOG_TAG, "testCreateGroupsTable start");
		try{
			createTable();
		}catch (Exception e) {
			fail("An exception occurred when testCreate: " + e);
		}
		Log.i(LOG_TAG, "testCreateGroupsTable PASS");
	}
	
	public void testAddContactsToGroups(){
		Log.i(LOG_TAG, "testAddContactsToGroups start");
		createTable();
		for(int i=0; i<testTable.length;i++){
			assertTrue("Failed adding contact to group at step "+i, 
					ContactGroupsTable.addContactToGroup(testTable[i][0], testTable[i][1], mTestDatabase.getWritableDatabase()) );
		}
		ArrayList<Long> list = getUniqueIdListFromTestTable();
		ArrayList<Long> testList = new ArrayList<Long>(); 
		for(Long l:list){
			testList.clear();
			assertTrue("Failed fetching contact groups for id "+l, 
			ContactGroupsTable.fetchContactGroups(l, testList, mTestDatabase.getWritableDatabase()) );
			if(testList.size()!=countEntriesWithId(l)){
				fail("In and Out table are different");
			}
			ArrayList<Long> compareList = getEntriesWithId(l);
			if(!testList.containsAll(compareList)|| !compareList.containsAll(testList)){
				fail("Collection of IDs aren't the same");
			}
		}
		
		//deleting contacts from groups
		ArrayList<Long> groupList = null;
		Long ContactId = null;
		for(Long l: list){
			groupList = getEntriesWithId(l);
			if(groupList.size()>1){
				ContactId = l;
				break;
			}
		}
		if(ContactId == null){
			fail("Test data does not containg user with more than one group.");
		}
		testList.clear();
		assertTrue("Failed deleteContactFromGroup", 
				ContactGroupsTable.deleteContactFromGroup(ContactId, groupList.get(0), mTestDatabase.getWritableDatabase() ));
		assertTrue("Failed fetchContactGroups after deleteContactFromGroup",
				ContactGroupsTable.fetchContactGroups(ContactId, testList, mTestDatabase.getWritableDatabase()) );
		if(testList.contains(groupList.get(0))){
			fail("Database contains contact for group relation which has been deleted");
		}
		
		//deleting contacts
		testList.clear();
		Long id = list.get(0);
		assertTrue("Failed deleteContact",
				ContactGroupsTable.deleteContact(id, mTestDatabase.getWritableDatabase()) );
		assertTrue("Failed fetchContactGroups after deleteContact",
				ContactGroupsTable.fetchContactGroups(id, testList, mTestDatabase.getWritableDatabase()) );
		if(testList.size()> 0 ){
			fail("Contact should be deleted");
		}
		
		Log.i(LOG_TAG, "testAddContactsToGroups PASS");
	}
	
	public void testModyficationsContactsToGroups(){
		Log.i(LOG_TAG, "testModyficationsContactsToGroups start");
		createTable();
		for(int i=0; i<testTable.length;i++){
			assertTrue("Failed adding contact to group at step "+i, 
					ContactGroupsTable.addContactToGroup(testTable[i][0], testTable[i][1], mTestDatabase.getWritableDatabase()) );
		}
		
		ArrayList<Long> list = getUniqueIdListFromTestTable();
		ArrayList<Long> groupList = null;
		Long ContactId = null;
		for(Long l: list){
			groupList = getEntriesWithId(l);
			if(groupList.size()>1){
				ContactId = l;
				break;
			}
		}
		if(ContactId == null){
			fail("Test data does not containg user with more than one group.");
		}
		Random r = new Random();
		r.setSeed(System.currentTimeMillis());
		
		groupList.add( r.nextLong() );
		groupList.add( r.nextLong() );
		
		Contact c= new Contact();
		c.localContactID = ContactId;
		c.groupList = groupList;
		
		ServiceStatus status = ContactGroupsTable.modifyContact(c, mTestDatabase.getWritableDatabase());
		if(status != ServiceStatus.SUCCESS){
			fail("Faild on modifyContact");
		}
		
		ArrayList<Long> testList = new ArrayList<Long>(); 
		assertTrue("Failed fetchContactGroups after modifyContact",
				ContactGroupsTable.fetchContactGroups(ContactId, testList, mTestDatabase.getWritableDatabase()) );
		if( !testList.containsAll(groupList)|| !groupList.containsAll(testList) ){
			fail("List after modyfyContact are different");
		}
		
		Log.i(LOG_TAG, "testModyficationsContactsToGroups PASS");
	}
	
}
