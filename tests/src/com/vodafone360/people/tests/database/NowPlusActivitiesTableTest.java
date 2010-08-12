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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineNativeTypes;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.ActivityContact;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.utils.CloseUtils;

public class NowPlusActivitiesTableTest extends	NowPlusTableTestCase {
	
	protected static final String LOG_TAG = "NowPlusActivitiesTableTest";
	private static final long YESTERDAY_TIME_MILLIS = System.currentTimeMillis() - 24*60*60*1000;


	public NowPlusActivitiesTableTest() {
		super();
	}
	
	/**
	 * The method tests database creation
	 */
	public void testCreateTable() {
		Log.i(LOG_TAG, "***** testCreateTable *****");
		ActivitiesTable.create(mTestDatabase.getWritableDatabase());
		Log.i(LOG_TAG, "***** testCreateTable SUCCEEDED*****");
	}
	
	
	/**
	 * This method deletes the tables, and releases the MainApplication
	 */
	protected void tearDown() throws Exception {
		ActivitiesTable.deleteActivities(null, mTestDatabase.getWritableDatabase());
		super.tearDown();
	}
	
	/**
	 * The method fetches Ids from the Empty table
	 */
	public void testFetchActivitiesIdsFromEmptyTable() {
		Log.i(LOG_TAG, "***** testFetchActivitiesIdsFromEmptyTable *****");
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		List<Long> actualDBIds = new ArrayList<Long>();
		ActivitiesTable.fetchActivitiesIds(actualDBIds, YESTERDAY_TIME_MILLIS, readableDataBase);
		assertEquals("The table is not empty after it has been dropped and created again!", 0, actualDBIds.size());
		Log.i(LOG_TAG, "***** SUCCEEDED testFetchActivitiesIdsFromEmptyTable *****");		
	}
	
	/**
	 *  The method adds activities into a table and check whether they are really present there
	 */
	public void testAddActivities() {
		Log.i(LOG_TAG, "***** testAddActivities *****");
		SQLiteDatabase writableDataBase = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(writableDataBase);
		
		List<ActivityItem> activitiesList = mTestModule.createFakeActivitiesList();
		ServiceStatus status  = ActivitiesTable.addActivities(activitiesList, writableDataBase, mContext);
		assertEquals("Activities not added to the table", ServiceStatus.SUCCESS, status);
		Log.i(LOG_TAG, "***** testAddActivities: activities added *****");

		// check if the records are there
		List<Long> activitiesIds = new ArrayList<Long>();
		for (ActivityItem item: activitiesList) {
			activitiesIds.add(item.activityId);
		}
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		List<Long> actualDBIds = new ArrayList<Long>();
		
		status = null;
		status = ActivitiesTable.fetchActivitiesIds(actualDBIds, YESTERDAY_TIME_MILLIS, readableDataBase);
		assertEquals("Fetching activities from the table failed", ServiceStatus.SUCCESS, status);
		Log.i(LOG_TAG, "***** testAddActivities: activities added *****");
		
		compareActivityIds(activitiesIds, actualDBIds);
		Log.i(LOG_TAG, "***** SUCCEEDED testAddActivities *****");
	}
	
   /**
    * This method fires assertion error when the supplied List are not identical 
    * @param ids - the initial ActivityItem ids 
    * @param dbIds - ActivityItem ids from the database
    */
    private void compareActivityIds(List<Long> ids, List<Long> dbIds) {
    	assertEquals(ids.size(), dbIds.size());
        final String error = "The item is absent!"; 
        for (Long id : ids) {
			assertEquals(error, true, dbIds.contains(id));
		}
    }

    /**
     * this test checks that the deleted activities are not really present in the DB
     */
	public void testDeleteActivities() {
		SQLiteDatabase db = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(db);
		
		/** Create dummy activities list. **/
		List<ActivityItem> activitiesList =
		    mTestModule.createFakeActivitiesList();
		assertEquals("activitiesList is the wrong size", 25, activitiesList.size());

	    /** Add activities list to database. **/
		assertEquals("ActivitiesTable.addActivities was unsuccessfull",
		        ServiceStatus.SUCCESS,
		        ActivitiesTable.addActivities(activitiesList, db, mContext));
	    assertEquals("activitiesList is the wrong size", 25, activitiesList.size());

		/** Check if the records are in the database. **/
	    List<Long> insertedDbIds = new ArrayList<Long>();
	    ActivitiesTable.fetchActivitiesIds(insertedDbIds, YESTERDAY_TIME_MILLIS, db);
		for (ActivityItem item: activitiesList) {
			assertNotNull("item.mActivityId should not be NULL", item.activityId);
			assertNotNull("item.mLocalActivityId should not be NULL", item.localActivityId);
			assertNotNull("item.mTitle should not be NULL", item.title);
		}
		
		/** Delete all activities regardless of flag. **/
		assertEquals(ServiceStatus.SUCCESS, ActivitiesTable.deleteActivities(null, db));
		
		/** Check that the database is now empty. **/
		List<Long> actualDBIds = new ArrayList<Long>();
		ActivitiesTable.fetchActivitiesIds(actualDBIds, YESTERDAY_TIME_MILLIS, db);
		assertEquals("Activitiess table is not empty after deletion", 0, actualDBIds.size());
	}
	
	
/////////////////////////////////////////////////TIMELINES////////////////////////////////////////////////
	
	/**
	 * This method checks that the added time line events are really present
	 * in the table.
	 */
	
	public void testAddTimelineEvents() {
		SQLiteDatabase database = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(database);
		
		ArrayList<TimelineSummaryItem> timelineSummaryItemList =
		    TestModule.generateFakeTimeLinesList();
		assertEquals("timelineSummaryItemList has size of 25", 25,
		        timelineSummaryItemList.size());

		ActivitiesTable.addTimelineEvents(timelineSummaryItemList, false,
		        database);
		assertEquals("timelineSummaryItemList has size of 25", 25,
		        timelineSummaryItemList.size());

		/** Check if the records are there. **/
		Cursor cursor = null;
		ArrayList<TimelineSummaryItem> actualDBTimeLines = null;
		try {
	        cursor = ActivitiesTable.fetchTimelineEventList(
	                YESTERDAY_TIME_MILLIS, new TimelineNativeTypes[]{
	                        TimelineNativeTypes.SmsLog,
	                        TimelineNativeTypes.MmsLog}, database);
	        
	        assertEquals("Cursor contains less items than expected!",
	                timelineSummaryItemList.size(), cursor.getCount());
	        
	        actualDBTimeLines = new ArrayList<TimelineSummaryItem>();
	        for (int i = 0; i < timelineSummaryItemList.size(); i++) {
	            if (cursor.moveToPosition(i)) {
	                actualDBTimeLines.add(ActivitiesTable.getTimelineData(cursor));
	            }
	        }

		} finally {
		    CloseUtils.close(cursor);
		}
			
		compareTimeLineIds(timelineSummaryItemList, actualDBTimeLines);
	}
	
	/**
	  * This method fires assertion error when the supplied List are not identical 
	  * @param ids - the initial ActivityItem ids 
	  * @param dbIds - ActivityItem ids from the database
	  */
	private void compareTimeLineIds(List<TimelineSummaryItem> times, List<TimelineSummaryItem> dbTimes) {
	   	assertEquals("The lists are of different sizes [" + times.size()
	   	        + "] vs [" + dbTimes.size() + "]", times.size(), dbTimes.size());
	    for (TimelineSummaryItem timelineSummaryItem : times) {
	    	assertEquals("The timeline item is absent! timelineSummaryItem["
	    	        + timelineSummaryItem + "] in [" + dbTimes + "]",
	    	        true, dbTimes.contains(timelineSummaryItem));
		}
	}

	/**
	 * This method checks that time line events are absent in the table
	 */
	public void testFetchTimelineEventList() {
		
		Log.i(LOG_TAG, "***** testFetchTimeLineEventlist: create table *****");
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		ActivitiesTable.create(readableDataBase);
		
		Cursor c = ActivitiesTable.fetchTimelineEventList(YESTERDAY_TIME_MILLIS,  new TimelineNativeTypes[]{TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog }, readableDataBase);
		
		assertEquals("The cursor is not empty!", 0, c.getCount());
		Log.i(LOG_TAG, "***** fetchTimeLineEventlist SUCCEEDED *****");
	}
	
	/**
	 * This method checks that status events are present in the table
	 */
	public void testFetchStatusEventList() {
		Log.i(LOG_TAG, "***** testFetchStatusEventList: create table *****");
	
		SQLiteDatabase writableDataBase = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(writableDataBase);
		
		List<ActivityItem> activitiesList = mTestModule.createFakeStatusEventList();
		ServiceStatus status  = ActivitiesTable.addActivities(activitiesList, writableDataBase, mContext);
		assertEquals("Activities not added to the table", ServiceStatus.SUCCESS, status);
		Log.i(LOG_TAG, "***** testFetchStatusEventList: activities added *****");

		// check if the records are there
		List<Long> activitiesIds = new ArrayList<Long>();
		for (ActivityItem item: activitiesList) {
			activitiesIds.add(item.activityId);
		}
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		List<Long> actualDBIds = new ArrayList<Long>();
		Cursor c = ActivitiesTable.fetchStatusEventList(YESTERDAY_TIME_MILLIS, readableDataBase);

		while (c.moveToNext()) {
			ActivityItem ai = new ActivityItem();
    		ActivityContact ac = new ActivityContact();
    		ActivitiesTable.getQueryData(c, ai, ac);
    		if (ac.mContactId != null) {
    			ai.contactList = new ArrayList<ActivityContact>();
    			ai.contactList.add(ac);
    		}
    		actualDBIds.add(ai.activityId);
		}
 		c.close();
 		compareActivityIds(activitiesIds, actualDBIds);
		Log.i(LOG_TAG, "***** fetchStatusEventlist SUCCEEDED *****");
	}

//TODO: this method is tested in testAddTimelineEvents(), 
//	public void testGetTimelineData() {
//		Log.i(LOG_TAG, "***** EXECUTING testActivitiesTableCreation *****");
//		SQLiteDatabase writableDataBase = mTestDatabase.getWritableDatabase();
//		ActivitiesTable.create(writableDataBase);
//		
//		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
//		
//		ActivitiesTable.getTimelineData(c);
//		
//		fail("Not yet implemented");
//	}
//	public void testFillUpdateData() {
//	
//		Log.i(LOG_TAG, "***** EXECUTING testActivitiesTableCreation *****");
//		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
//		ActivitiesTable.create(readableDataBase);
//		Log.i(LOG_TAG, "***** EXECUTING addTimelineEvents , not call log though *****");
//		ActivitiesTable.fillUpdateData(item, contactIdx)
//
//	fail("Not yet implemented");	
//}
	
	/**
	 * This method checks that timeline events are present in the table
	 */
	public void testFetchTimelineEventsForContact() {
		Log.i(LOG_TAG, "***** testFetchTimelineEventsForContact():create table *****");
		SQLiteDatabase dataBase = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(dataBase);
		Log.i(LOG_TAG, "***** testFetchLatestStatusTimestampForContact , not call log though *****");
		
		ArrayList<TimelineSummaryItem> timeLines = TestModule.generateFakeTimeLinesList();
		ActivitiesTable.addTimelineEvents(timeLines, false, dataBase);
		
		// check if the records are there
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		Cursor c = ActivitiesTable.fetchTimelineEventList(YESTERDAY_TIME_MILLIS, new TimelineNativeTypes[]{TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog }, readableDataBase);
		ArrayList<TimelineSummaryItem> actualDBTimeLines = new ArrayList<TimelineSummaryItem>();
		for (int i = 0; i < timeLines.size(); i++) {
			if (c.moveToPosition(i)) {
				actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
			}
		}
		c.close();
		c = null;
		compareTimeLineIds(timeLines, actualDBTimeLines);
		
		for (TimelineSummaryItem timeLineSummary: actualDBTimeLines) {
			TimelineNativeTypes[] typeList = {TimelineNativeTypes.CallLog, 
					TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog};
			c = ActivitiesTable.fetchTimelineEventsForContact(YESTERDAY_TIME_MILLIS, 
					timeLineSummary.mLocalContactId, timeLineSummary.mContactName, typeList, null, readableDataBase);
			assertEquals("the cursor is empty!", false, c.getCount() == 0);
			while (c.moveToNext()) {
				TimelineSummaryItem summary = ActivitiesTable.getTimelineData(c);
				assertEquals("the timeline is not found!", true, actualDBTimeLines.contains(summary));
			}
			c.close();
			c = null;
		}
		Log.i(LOG_TAG, "***** testFetchTimelineEventsForContact() SUCCEEDED *****");
	}
	
	/**
	 * this method checks the time stamps in the initial time line list are the same as in the database 
	 */
	@Suppress
	public void testFetchLatestStatusTimestampForContact() {
		Log.i(LOG_TAG, "***** testFetchLatestStatusTimestampForContact: create table *****");
		SQLiteDatabase dataBase = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(dataBase);
		Log.i(LOG_TAG, "***** testFetchLatestStatusTimestampForContact , not call log though *****");
		
		ArrayList<TimelineSummaryItem> timeLines = TestModule.generateFakeTimeLinesList();
		ActivitiesTable.addTimelineEvents(timeLines, false, dataBase);
		
		// check if the records are there
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		Cursor c = ActivitiesTable.fetchTimelineEventList(YESTERDAY_TIME_MILLIS, new TimelineNativeTypes[]{TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog }, readableDataBase);
		ArrayList<TimelineSummaryItem> actualDBTimeLines = new ArrayList<TimelineSummaryItem>();		
		for (int i = 0; i < timeLines.size(); i++) {
			if (c.moveToPosition(i)) {
				actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
			}
		}
		c.close();
		
		compareTimeLineIds(timeLines, actualDBTimeLines);
		
		for (TimelineSummaryItem timeLineSummary: actualDBTimeLines) {
			// Earlier the return type was Long and now modified to ActivityItem
            // TODO check the AssertEqual and if needed modify appropriately.
            ActivityItem actualDBTimeStamp = ActivitiesTable.getLatestStatusForContact(timeLineSummary.mContactId, readableDataBase);
			assertEquals("the timestamps are not equal!", timeLineSummary.mTimestamp, actualDBTimeStamp);
		}
		Log.i(LOG_TAG, "***** restFetchLatestStatusTimestampForContact SUCCEEDED *****");
	}

	/**
	 * this method checks the updated contacts are present in the database 
	 */
	public void testUpdateTimelineContactNameAndId5() {
		
		Log.i(LOG_TAG, "***** testUpdateTimelineContactNameAndId5: create table *****");
		SQLiteDatabase writableDataBase = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(writableDataBase);
		Log.i(LOG_TAG, "***** testUpdateTimelineContactNameAndId5, not call log though *****");
		
		ArrayList<TimelineSummaryItem> timeLines = TestModule.generateFakeTimeLinesList();
		ActivitiesTable.addTimelineEvents(timeLines, false, writableDataBase);
		
		// check if the records are there
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		Cursor c = ActivitiesTable.fetchTimelineEventList(YESTERDAY_TIME_MILLIS, new TimelineNativeTypes[]{TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog }, readableDataBase);
		ArrayList<TimelineSummaryItem> actualDBTimeLines = new ArrayList<TimelineSummaryItem>();
		
		while (c.moveToNext()) {
			actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
		}
			
		compareTimeLineIds(timeLines, actualDBTimeLines);
		timeLines = actualDBTimeLines;
		
		final String NAME = "New";
		for (TimelineSummaryItem timeLineSummary: timeLines) {
			ActivitiesTable.updateTimelineContactNameAndId(timeLineSummary.mContactName, 
					timeLineSummary.mContactName += NAME, 
					timeLineSummary.mLocalContactId, 
					TestModule.generateRandomLong(), 
					writableDataBase);
		}
		
		c.requery();
		actualDBTimeLines.clear();
		c.moveToFirst();
		for (int i = 0, count = c.getCount(); i < count; i++, c.moveToNext()) {
			actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
		}
		compareTimeLineIds(timeLines, actualDBTimeLines);
	
		Log.i(LOG_TAG, "***** testUpdateTimelineContactNameAndId5 SUCCEEDED *****");
	}

	/**
	 * this method checks the updated contacts are present in the database 
	 */
	
	public void testUpdateTimelineContactNameAndId3() {
		Log.i(LOG_TAG, "***** testUpdateTimelineContactNameAndId3: create table *****");
		SQLiteDatabase writableDataBase = mTestDatabase.getWritableDatabase();
		ActivitiesTable.create(writableDataBase);
		Log.i(LOG_TAG, "***** testUpdateTimelineContactNameAndId3 , not call log though *****");
		
		ArrayList<TimelineSummaryItem> timeLines = TestModule.generateFakeTimeLinesList();
		ActivitiesTable.addTimelineEvents(timeLines, false, writableDataBase);
		
		// check if the records are there
		SQLiteDatabase readableDataBase = mTestDatabase.getReadableDatabase();
		Cursor c = ActivitiesTable.fetchTimelineEventList(YESTERDAY_TIME_MILLIS, new TimelineNativeTypes[]{TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog }, readableDataBase);
		ArrayList<TimelineSummaryItem> actualDBTimeLines = new ArrayList<TimelineSummaryItem>();
		for (int i = 0; i < timeLines.size(); i++) {
			if (c.moveToPosition(i)) {
				actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
			}
		}
			
		compareTimeLineIds(timeLines, actualDBTimeLines);
		
		final String NAME = "New";
		for (TimelineSummaryItem timeLineSummary: timeLines) {
			timeLineSummary.mContactName += NAME;
			ActivitiesTable.updateTimelineContactNameAndId(timeLineSummary.mContactName, timeLineSummary.mLocalContactId, writableDataBase);
		}
		
		c.requery();
		actualDBTimeLines.clear();
		for (int i = 0; i < timeLines.size(); i++) {
			if (c.moveToPosition(i)) {
				actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
			}
		}
		compareTimeLineIds(timeLines, actualDBTimeLines);
		
		Log.i(LOG_TAG, "***** testUpdateTimelineContactNameAndId3 SUCCEEDED *****");
	}
	
}
