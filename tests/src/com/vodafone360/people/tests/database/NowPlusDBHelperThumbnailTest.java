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

import android.graphics.Bitmap;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.ThumbnailInfo;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;

public class NowPlusDBHelperThumbnailTest extends ApplicationTestCase<MainApplication> {

	private static String LOG_TAG = "NowPlusDatabaseTest";
	final static int WAIT_EVENT_TIMEOUT_MS = 30000;
	
	final static int NUM_OF_CONTACTS = 3;

	private static MainApplication mApplication = null;
	private static DatabaseHelper mDatabaseHelper = null;
	final TestModule mTestModule = new TestModule();
	private DbTestUtility mTestUtility;
	
    private static final int WIDTH = 50;
    private static final int HEIGHT = 50;
    private static final int STRIDE = 64;   
	
    private static int[] createColors() {
        int[] colors = new int[STRIDE * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int r = x * 255 / (WIDTH - 1);
                int g = y * 255 / (HEIGHT - 1);
                int b = 255 - Math.min(r, g);
                int a = Math.max(r, g);
                colors[y * STRIDE + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return colors;
    }
	
	public NowPlusDBHelperThumbnailTest() {
		super(MainApplication.class);
	}

	private boolean initialise() {
		mTestUtility = new DbTestUtility(getContext());
		
    	createApplication();
		mApplication = getApplication();
		
		if(mApplication == null){
			Log.e(LOG_TAG, "Unable to create main application");
			return false;
		}
		mDatabaseHelper = mApplication.getDatabase();
		if (mDatabaseHelper.getReadableDatabase() == null) {
			return false;
		}
		mTestUtility.startEventWatcher(mDatabaseHelper);
		return true;
	}

	private void shutdown() {
		mTestUtility.stopEventWatcher();
	}
	
	@SmallTest
	public void testDBHelperThumbnail() {
		assertTrue(initialise());
		mDatabaseHelper.removeUserData();
		ServiceStatus status = mTestUtility.waitForEvent(WAIT_EVENT_TIMEOUT_MS,
				DbTestUtility.CONTACTS_INT_EVENT_MASK);
		assertEquals(ServiceStatus.SUCCESS, status);

		Log.i(LOG_TAG, "Add a contact to ContactSummaryTable");
		Contact contact = mTestModule.createDummyContactData();
		status = mDatabaseHelper.addContact(contact);
		assertEquals(ServiceStatus.SUCCESS, status);

		Bitmap testBimap = Bitmap.createBitmap(createColors(), 0, STRIDE, WIDTH, HEIGHT,
                Bitmap.Config.ARGB_8888);
		ContactDetail cd = new ContactDetail();
		cd.localContactID = contact.localContactID;
		cd.photo = testBimap;
		
		status = mDatabaseHelper.addContactDetail(cd);
		
		status = ContactSummaryTable.modifyPictureLoadedFlag(
				contact.localContactID, false, mDatabaseHelper
						.getWritableDatabase());
		assertEquals(ServiceStatus.SUCCESS, status);
		int numOfThumbnails = mDatabaseHelper.fetchThumbnailUrlCount();
		assertEquals(1, numOfThumbnails);

		List<ThumbnailInfo> thumbInfoList = new ArrayList<ThumbnailInfo>();
	
		status = mDatabaseHelper.fetchThumbnailUrls(thumbInfoList, 0, 1);
		assertEquals(ServiceStatus.SUCCESS, status);
		
		shutdown();
	}
}