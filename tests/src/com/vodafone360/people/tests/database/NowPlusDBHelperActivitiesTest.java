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
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineNativeTypes;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

public class NowPlusDBHelperActivitiesTest extends	ApplicationTestCase<MainApplication> {
	
	private static final  String LOG_TAG = "NowPlusDBHelperActivitiesTest";
	private static final int WAIT_EVENT_TIMEOUT_MS = 30000;
	private static final long YESTERDAY_TIME_MILLIS = System.currentTimeMillis() - 24*60*60*1000;

	private MainApplication mApplication = null;
	private DatabaseHelper mDatabase = null;
	private DbTestUtility mTestUtility;
	private TestModule mTestModule = new TestModule();
	
	public NowPlusDBHelperActivitiesTest() {
		super(MainApplication.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		initialise();
	}

	protected void tearDown() throws Exception {
		shutdown();
		super.tearDown();
	}
	
	private boolean initialise() {
		mTestUtility = new DbTestUtility(getContext());
		
    	createApplication();
		mApplication = getApplication();
		
		if (mApplication == null){
			Log.e(LOG_TAG, "Unable to create main application");
			return false;
		}
		mDatabase = mApplication.getDatabase();
		if (mDatabase.getReadableDatabase() == null) {
			return false;
		}
		mTestUtility.startEventWatcher(mDatabase);
		Log.i(LOG_TAG, "Initialised test environment and load database");
		return true;
	}

	private void shutdown() {
		mTestUtility.stopEventWatcher();
	}
	
	@MediumTest
    public void testRemoveAllRecords() {
		Log.i(LOG_TAG, "***** EXECUTING testRemoveAllRecords *****");
		Log.i(LOG_TAG, "testRemoveAllRecords checks for te DB event coming as result of records removal");
		
		mDatabase.removeUserData();
		
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		
		assertEquals(ServiceStatus.SUCCESS, status);

		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "testRemoveAllRecords has completed successfully");
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "");
    }

	@MediumTest
    public void testAddActivity() {
		
		mDatabase.removeUserData();
		
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		
		assertEquals(ServiceStatus.SUCCESS, status);
		
		Log.i(LOG_TAG, "***** EXECUTING testAddActivity *****");
		
		List<ActivityItem> activityList = mTestModule.createFakeActivitiesList();
		
//		ServiceStatus status = mDatabase.addActivities(activityList);
		
		
		status = null;
		status = mDatabase.addActivities(activityList);
		
		assertEquals(ServiceStatus.SUCCESS, status);

		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "testAddActivity has completed successfully");
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "");
    }
	
	@MediumTest
	public void testFetchActivityIds() {
		mDatabase.removeUserData();
		
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		
		assertEquals(ServiceStatus.SUCCESS, status);
		
		Log.i(LOG_TAG, "***** EXECUTING testFetchActivityIds *****");

		List<ActivityItem> activityList = mTestModule.createFakeActivitiesList();
		
		
		
		status = null;
		status = mDatabase.addActivities(activityList);
		
		assertEquals(ServiceStatus.SUCCESS, status);
		
		List<Long> idsList = new ArrayList<Long>();
		
		for (ActivityItem activity:activityList) {
			idsList.add(activity.mActivityId);
		}
		status = null;
		
		List<Long> dbIdsList = new ArrayList<Long>();
		
		status = mDatabase.fetchActivitiesIds(dbIdsList, YESTERDAY_TIME_MILLIS);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		compareActivityIds(idsList, dbIdsList);
		
		status = mDatabase.fetchActivitiesIds(dbIdsList, YESTERDAY_TIME_MILLIS);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		status = mDatabase.deleteActivities(null);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		List<Long> fetchedDBIdsList = new ArrayList<Long>();
		status = mDatabase.fetchActivitiesIds(fetchedDBIdsList, YESTERDAY_TIME_MILLIS);
		assertEquals(ServiceStatus.SUCCESS, status);
		assertEquals(0, fetchedDBIdsList.size());
		
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "testFetchActivityIds has completed successfully");
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "");
    }
	
	@MediumTest
	public void testTimelineEvents() {
		mDatabase.removeUserData();
		
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS, DbTestUtility.CONTACTS_INT_EVENT_MASK);
		
		assertEquals(ServiceStatus.SUCCESS, status);
		
		Log.i(LOG_TAG, "***** EXECUTING testTimelineEvents *****");

		ArrayList<TimelineSummaryItem> syncItemList = TestModule.generateFakeTimeLinesList();
		status =  mDatabase.addTimelineEvents(syncItemList, true);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		Cursor c = mDatabase
				.fetchTimelineEvents(YESTERDAY_TIME_MILLIS,
						new TimelineNativeTypes[] { TimelineNativeTypes.SmsLog,
								TimelineNativeTypes.MmsLog,
								TimelineNativeTypes.CallLog });
		ArrayList<TimelineSummaryItem> actualDBTimeLines = new ArrayList<TimelineSummaryItem>();
		while (c.moveToNext()) {
			actualDBTimeLines.add(ActivitiesTable.getTimelineData(c));
		}
		c.close();
		c = null;
		assertEquals(syncItemList.size(), actualDBTimeLines.size());
	
		for (TimelineSummaryItem actualItem : actualDBTimeLines) {
			for (TimelineSummaryItem syncedItem : syncItemList) {
				if (actualItem.mTimestamp == syncedItem.mTimestamp) {
					assertEquals(actualItem.mContactName, syncedItem.mContactName);
				}
			}
		}
		
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "testTimelineEvents has completed successfully");
		Log.i(LOG_TAG, "********************************");
		Log.i(LOG_TAG, "");
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
}
