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

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;

/**
 * Reflection helper for testing.
 */
public abstract class NativeContactsApiTestHelper {

    private static NativeContactsApiTestHelper sInstance;
    
    protected Context mContext;
    protected ContentResolver mCr;
    
    public interface IPeopleAccountChangeObserver {
    	public void onPeopleAccountsChanged(int currentNumAccounts);
    }

    public static NativeContactsApiTestHelper getInstance(Context context) {

        if (sInstance == null) {
            String className;

        	final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        	// get around fragmentation using "4" which is the same as referring to DONUT
        	if(sdkVersion > 4) {
        		className = "NativeContactsApiTestHelper2";
        	} else {
        		className = "NativeContactsApiTestHelper1";
        	}
        	
            try {
                Class<? extends NativeContactsApiTestHelper> clazz =
                        Class.forName(NativeContactsApiTestHelper.class.getPackage().getName() + "." + className)
                                .asSubclass(NativeContactsApiTestHelper.class);
                sInstance = clazz.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            
            sInstance.mContext = context;
            sInstance.mCr = context.getContentResolver();
        }
    
        return sInstance;
    }
    
    abstract public void startObservingAccounts(IPeopleAccountChangeObserver observer);
    abstract public void wipeNab();
    abstract public void wipeNabAccounts();
    abstract public void wipeNabContacts();
    //abstract public void addDefault360Account(Context context);
    //abstract public void add360Account(Context context, String userName);   
    abstract public void populateNabContacts();
    
	/**
	 * Utility method to put the thread to sleep
	 * @param time
	 */
	protected void threadSleep(int time) {
		try {
			synchronized(this) {
				Thread.sleep(time);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Utility method to put the thread waiting
	 * @param time
	 */
	protected void threadWait(int time) {
		try {
			synchronized(this) {
				wait(time);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


