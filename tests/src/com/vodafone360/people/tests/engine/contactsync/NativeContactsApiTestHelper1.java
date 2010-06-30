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

import com.vodafone360.people.tests.engine.contactsync.NativeContactsApiTestHelper.IPeopleAccountChangeObserver;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;

public class NativeContactsApiTestHelper1 extends NativeContactsApiTestHelper {
	@Override
	public void populateNabContacts() {
		
	}

	@Override
	public void wipeNab() {
		wipeNabContacts();
	}

	@Override
	public void wipeNabAccounts() {
		// Empty for 1.X (no accounts)
	}

	@Override
	public void wipeNabContacts() {
        try {
        	mCr.delete(People.CONTENT_URI, null, null);
        } catch (IllegalArgumentException e) {
        	Cursor c = mCr.query(People.CONTENT_URI, new String[]{People._ID}, null, null, null);
        	while (c.moveToNext()) {
        		Uri uri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, c.getInt(0));
        		mCr.delete(uri, null, null);
        	}
        	c.close();
        }
	}

	@Override
	public void startObservingAccounts(IPeopleAccountChangeObserver observer) {
		// TODO Auto-generated method stub
	}
}
