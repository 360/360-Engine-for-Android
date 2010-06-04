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

package com.vodafone360.people.tests.engine.contactsync;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.database.tables.NativeChangeLogTable.ContactChangeInfo;
import com.vodafone360.people.engine.contactsync.ContactSyncUtils;

public class ContactSyncUtilsTest extends TestCase {
	
	private static final String LOG_TAG = "ContactSyncUtilsTest";
	
	/***
	 * Test for ContactSyncUtils.findIdInOrderedList()
	 */
	@SmallTest
    public void testFindIdInOrderedList() {
		Log.i(LOG_TAG, "ContactSyncUtilsTest.testFindIdInOrderedList()");
		try {
			assertEquals("#1 Cannot find item in normal list", 
					2, ContactSyncUtils.findIdInOrderedList(3, makeList()));
			assertEquals("#2 Should not find item that is not there", 
					-1, ContactSyncUtils.findIdInOrderedList(10, makeList()));
			assertEquals("#3 Cannot find item in a list that contains NULLs at the start", 
					4, ContactSyncUtils.findIdInOrderedList(3, makeListWithNulls()));
			assertEquals("#4 Should not find item in NULL list", 
					-1, ContactSyncUtils.findIdInOrderedList(1, makeListAllNull()));	
			
		} catch (Exception e) {
			Log.e(LOG_TAG, "ContactSyncUtilsTest.testFindIdInOrderedList() Exception", e);
			assertTrue("Exception thrown during test", false);
		}
	}

	/***
	 * Generate a ContactChangeInfo list with indexed Native Contact ID value from 1 to 5.
	 * 
	 * @return ContactChangeInfo list with indexed Native Contact ID value from 1 to 5.
	 */
	private List<ContactChangeInfo> makeList() {
		List<ContactChangeInfo> list = new ArrayList<ContactChangeInfo>();
		list.add(makeContactChangeInfo(1));
		list.add(makeContactChangeInfo(2));
		list.add(makeContactChangeInfo(3));
		list.add(makeContactChangeInfo(4));
		list.add(makeContactChangeInfo(5));
		return list;
	}

	/***
	 * Generate a ContactChangeInfo list with indexed Native Contact ID value from NULL to 5.
	 * 
	 * @return ContactChangeInfo list with indexed Native Contact ID value from NULL to 5.
	 */
	private List<ContactChangeInfo> makeListWithNulls() {
		List<ContactChangeInfo> list = new ArrayList<ContactChangeInfo>();
		list.add(makeContactChangeInfo(null));
		list.add(makeContactChangeInfo(null));
		list.add(makeContactChangeInfo(1));
		list.add(makeContactChangeInfo(2));
		list.add(makeContactChangeInfo(3));
		list.add(makeContactChangeInfo(4));
		list.add(makeContactChangeInfo(5));
		return list;
	}
	
	/***
	 * Generate a ContactChangeInfo list with Native Contact ID values that are all NULL.
	 * 
	 * @return ContactChangeInfo list with Native Contact ID values that are all NULL.
	 */
	private List<ContactChangeInfo> makeListAllNull() {
		List<ContactChangeInfo> list = new ArrayList<ContactChangeInfo>();
		list.add(makeContactChangeInfo(null));
		list.add(makeContactChangeInfo(null));
		list.add(makeContactChangeInfo(null));
		list.add(makeContactChangeInfo(null));
		list.add(makeContactChangeInfo(null));
		return list;
	}
	
	/***
	 * Convenience method for creating a ContactChangeInfo object with the given nativeContactId.
	 * @param nativeContactId Native Contact ID or NULL.
	 * @return New ContactChangeInfo object.
	 */
	private ContactChangeInfo makeContactChangeInfo(Integer nativeContactId) {
		ContactChangeInfo contactChangeInfo = new ContactChangeInfo();
		contactChangeInfo.mNativeContactId = nativeContactId;
		return contactChangeInfo;
	}
}
