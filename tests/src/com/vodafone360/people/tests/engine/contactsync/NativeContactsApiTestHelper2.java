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

import com.vodafone360.people.utils.CursorUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.Account;

public class NativeContactsApiTestHelper2 extends NativeContactsApiTestHelper {
	
	private static final String PEOPLE_ACCOUNT_TYPE = "com.vodafone360.people.android.account";
	
	private AccountsObserverThread mObserverThread = new AccountsObserverThread();
	
	class AccountsObserverThread extends Thread {
		private AccountsObserver mAccountsObserver = new AccountsObserver();
		private Handler mHandler = new Handler(); 
		
		@Override
		public void run() {
			super.run();
		}
		
		public synchronized void startObserving(IPeopleAccountChangeObserver observer) {
			AccountManager accountMan = AccountManager.get(mContext);
			accountMan.addOnAccountsUpdatedListener(mAccountsObserver, mHandler, false);
		}
		
		public synchronized void stopObserving() {
			AccountManager accountMan = AccountManager.get(mContext);
			accountMan.removeOnAccountsUpdatedListener(mAccountsObserver);
		}
	}
	
	class AccountsObserver implements OnAccountsUpdateListener {
		private IPeopleAccountChangeObserver mExternalObserver;
		
		private int mNumPeopleAccounts = 0;
		
		public void setExternalObserver(IPeopleAccountChangeObserver observer) {
			mExternalObserver = observer;
		}
		
		@Override
		public void onAccountsUpdated(Account[] accounts) {
			int numPeopleAccounts = 0;
			if(accounts != null && accounts.length > 0) {
				final int numAccounts = accounts.length;
				for(int i = 0; i < numAccounts; i++) {
					final Account account = accounts[i];
					if(account != null && account.type.equals(PEOPLE_ACCOUNT_TYPE)) {
						numPeopleAccounts++;
					}
				}
			} else {
				numPeopleAccounts = 0;
			}
			
			if(mExternalObserver == null) {
				return;
			}
			
			if(numPeopleAccounts != mNumPeopleAccounts) {
				mExternalObserver.onPeopleAccountsChanged(numPeopleAccounts);
				mNumPeopleAccounts = numPeopleAccounts;
			}
		}
	}
	
	
	@Override
	public void populateNabContacts() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void wipeNab() {
		// Removing accounts removes contacts anyway
		wipeNabAccounts();
	}

	@Override
	public void wipeNabAccounts() {
		// TODO Auto-generated method stub
        AccountManager accountMan = AccountManager.get(mContext);
        
        android.accounts.Account[] accounts = 
            accountMan.getAccountsByType(PEOPLE_ACCOUNT_TYPE);
        if(accounts != null && accounts.length > 0) {
        	int numAccounts = accounts.length;
        	while(numAccounts-- > 0) {
        		accountMan.removeAccount(accounts[numAccounts], null, null);
        		threadWait(5000);
        	} 
        }
	}

	@Override
	public void wipeNabContacts() {
        try {
        	mCr.delete(ContactsContract.Data.CONTENT_URI, null, null);
        	mCr.delete(ContactsContract.Contacts.CONTENT_URI, null, null);
        } catch (IllegalArgumentException e) {
        	Cursor c = mCr.query(ContactsContract.Data.CONTENT_URI, null, null, null, null);
        	while (c.moveToNext()) {
        		Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, c.getInt(0));
        		mCr.delete(uri, null, null);
        	}
        	c.close();
        }
	}

	@Override
	public void startObservingAccounts(IPeopleAccountChangeObserver observer) {
		mObserverThread.startObserving(observer);
	}
}
