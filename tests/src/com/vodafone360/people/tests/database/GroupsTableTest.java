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
import android.database.SQLException;
import android.util.Log;

import com.vodafone360.people.database.tables.GroupsTable;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.service.ServiceStatus;


public class GroupsTableTest extends NowPlusTableTestCase {
    private static final String[][] GROUPS = 
    { {"white", "1","0","true", "true", "true","true", "GroupNameOne","23451"},
        {"blue",  "2","1","true", "false","true","true", "GroupNameTwo","23451"},
        {"red",   "3","2","false","false","false","true","GroupNameThree","23451"},
        {"yellow","4","3","false","false","false","true","GroupNameFour","23451"},
        {"dark",  "5","4","true","true","false","true","GroupName5","23451"},
        {"pink",  "6","5","false","false","true","true","GroupName6","23451"},
        {"violet","7","6","false","true","false","true","GroupName7","23451"},
        {"grey",  "8","7","true","false","false","true","GroupName8","23451"},
        {"green", "9","8","false","true","false","true","GroupName9","23451"},
        {"black","10","9","true","false","false","true","GroupName10","23451"}
    };
	
	public GroupsTableTest() {
		super();
	}

//	Functions that need testing:
//
//	*addGroupList(List<GroupItem>, SQLiteDatabase)
//	*create(Context, SQLiteDatabase)
//	*deleteAllGroups(SQLiteDatabase)
//	*fetchGroupList(ArrayList<GroupItem>, SQLiteDatabase)
//	fillUpdateData(GroupItem)
//	getFullQueryList()
//	getGroupCursor(SQLiteDatabase)
//	getQueryData(Cursor)
//	getQueryStringSql(String)
//	populateSystemGroups(Context, SQLiteDatabase)
	
	
	/**
	 * Compares two group items
	 * @param gi1
	 * @param gi2
	 * @return true if GroupItems match
	 */
	private boolean doGroupItemsMatch(GroupItem gi1, GroupItem gi2) {
		if ((gi1.mColor == null && gi2.mColor != null) || gi1.mColor != null
				&& gi1.mColor.compareTo(gi2.mColor) != 0) {
			return false;
		}
		if ((gi1.mGroupType == null && gi2.mGroupType != null)
				|| gi1.mGroupType != null
				&& gi1.mGroupType.compareTo(gi2.mGroupType) != 0) {
			return false;
		}
		if ((gi1.mId == null && gi2.mId != null) || gi1.mId != null
				&& gi1.mId.compareTo(gi2.mId) != 0) {
			return false;
		}
		if ((gi1.mIsReadOnly == null && gi2.mIsReadOnly != null)
				|| gi1.mIsReadOnly != null
				&& gi1.mIsReadOnly.compareTo(gi2.mIsReadOnly) != 0) {
			return false;
		}
		if ((gi1.mIsSmartGroup == null && gi2.mIsSmartGroup != null)
				|| gi1.mIsSmartGroup != null
				&& gi1.mIsSmartGroup.compareTo(gi2.mIsSmartGroup) != 0) {
			return false;
		}
		if ((gi1.mIsSystemGroup == null && gi2.mIsSystemGroup != null)
				|| gi1.mIsSystemGroup != null
				&& gi1.mIsSystemGroup.compareTo(gi2.mIsSystemGroup) != 0) {
			return false;
		}
		if ((gi1.mRequiresLocalisation == null && gi2.mRequiresLocalisation != null)
				|| gi1.mRequiresLocalisation != null
				&& gi1.mRequiresLocalisation
						.compareTo(gi2.mRequiresLocalisation) != 0) {
			return false;
		}
		if ((gi1.mName == null && gi2.mName != null) || gi1.mName != null
				&& gi1.mName.compareTo(gi2.mName) != 0) {
			return false;
		}
		if ((gi1.mUserId == null && gi2.mUserId != null) || gi1.mUserId != null
				&& gi1.mUserId.compareTo(gi2.mUserId) != 0) {
			return false;
		}
		if ((gi1.mImageBytes == null && gi2.mImageBytes != null)
				|| gi1.mImageBytes != null
				&& gi1.mImageBytes.compareTo(gi2.mImageBytes) != 0) {
			return false;
		}
		if ((gi1.mImageMimeType == null && gi2.mImageMimeType != null)
				|| gi1.mImageMimeType != null
				&& gi1.mImageMimeType.compareTo(gi2.mImageMimeType) != 0) {
			return false;
		}

		return true;
	}
	
	/**
	 * creates table that will be tested in this suite
	 */
	private void createTable() {
		try {
			GroupsTable.create(mContext, mTestDatabase.getWritableDatabase());
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
	
	private static List<GroupItem> generateGroupItemList(){
		List<GroupItem> groupItemList = new ArrayList<GroupItem>();
		for (int i = 0; i < GROUPS.length; i++) {
			GroupItem groupItem = new GroupItem();
			groupItem.mColor = GROUPS[i][0];
			groupItem.mGroupType = Integer.valueOf( GROUPS[i][1] );
			groupItem.mId = Long.valueOf( GROUPS[i][2] );
			groupItem.mIsReadOnly = Boolean.valueOf( GROUPS[i][3] );
			groupItem.mIsSmartGroup = Boolean.valueOf( GROUPS[i][4] );
			groupItem.mIsSystemGroup = Boolean.valueOf( GROUPS[i][5] );
			groupItem.mRequiresLocalisation = Boolean.valueOf( GROUPS[i][6] );
			groupItem.mName = GROUPS[i][7];
			groupItem.mUserId = Long.valueOf( GROUPS[i][8] );
			groupItemList.add(groupItem);
		}

		return groupItemList;
	}

	/***
	 * Test for adding, fetching and deleting items in the Groups Table.
	 */
	public final void testAddingFetchingDeletingGroups() {
		createTable();
		ArrayList<GroupItem> workingGroupItemList
		    = new ArrayList<GroupItem>();

		assertEquals("Unable to fetch groups from table",
		        ServiceStatus.SUCCESS, GroupsTable.fetchGroupList(
		                workingGroupItemList,
		                mTestDatabase.getWritableDatabase()));
		assertEquals("Fetched data from newly created table does not have "
		        + "exactly 3 system groups. Size["
		        + workingGroupItemList.size() + "]", 3,
		        workingGroupItemList.size());

		List<GroupItem> cachedGroupItemList =
		    new ArrayList<GroupItem>(workingGroupItemList);
		cachedGroupItemList.addAll(generateGroupItemList());

		if (GroupsTable.addGroupList(generateGroupItemList(),
		        mTestDatabase.getWritableDatabase())
		        != ServiceStatus.SUCCESS) {
			fail("Unable to add groups into table");
		}
		workingGroupItemList.clear();

		if (GroupsTable.fetchGroupList(workingGroupItemList,
		        mTestDatabase.getWritableDatabase())
		        != ServiceStatus.SUCCESS) {
			fail("Unable to fetch groups from table");
		}

		// comparing in and out list
		if (cachedGroupItemList.size() != workingGroupItemList.size()) {
			fail("In and out list have different sizes");
		}
		for (int i = 0; i < cachedGroupItemList.size(); i++) {
			if (!doGroupItemsMatch(cachedGroupItemList.get(i),
			        workingGroupItemList.get(i))) {
				fail("Element (" + i + ") is not the same in IN and OUT list");
			}
		}

		/** Test for cursor functions. **/
		Cursor cursor = GroupsTable.getGroupCursor(
		        mTestDatabase.getReadableDatabase());
		ArrayList<GroupItem> cursorList = new ArrayList<GroupItem>();

		if (cursor.moveToFirst()) {
			cursorList.add(GroupsTable.getQueryData(cursor));
			while (cursor.moveToNext()) {
				cursorList.add(GroupsTable.getQueryData(cursor));
			}
		}

		assertEquals("In and cursorList list have different sizes",
				cachedGroupItemList.size(), cursorList.size());

		for (int i = 0; i < cachedGroupItemList.size(); i++) {
			if (!doGroupItemsMatch(cachedGroupItemList.get(i),
			        cursorList.get(i))) {
				fail("Element (" + i + ") is not the same in IN and "
				        + "cursorList list");
			}
		}

		if (GroupsTable.deleteAllGroups(mTestDatabase.getWritableDatabase())
		        != ServiceStatus.SUCCESS) {
			fail("Unable to delete groups from table");
		}

		workingGroupItemList.clear();

		if (GroupsTable.fetchGroupList(workingGroupItemList,
		        mTestDatabase.getWritableDatabase())
		        != ServiceStatus.SUCCESS) {
			fail("Unable to fetch groups from table");
		}
		if (workingGroupItemList.size() > 0) {
			fail("There are still rows in table that should be now empty");
		}

		Log.i(LOG_TAG, "testAddingFetchingDeletingGroups PASS");
	}

	public void testErrorHandling() {
		Log.i(LOG_TAG, "GroupsTableTest start");
		createTable();
		
		Log.i(LOG_TAG, "GroupsTableTest PASS");
	}
	
}
