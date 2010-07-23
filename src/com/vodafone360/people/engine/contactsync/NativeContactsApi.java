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

package com.vodafone360.people.engine.contactsync;

import java.security.InvalidParameterException;

import android.content.ContentResolver;
import android.content.Context;
import android.text.TextUtils;

import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * Class that provides an abstraction layer for accessing the Native Contacts API.
 * The underlying API to be used should be the most suitable for the SDK version of the device.
 */
public abstract class NativeContactsApi { 

    /**
     * 360 client account type.
     */
    protected static final int PEOPLE_ACCOUNT_TYPE = 1;

    /**
     * Google account type.
     */
    protected static final int GOOGLE_ACCOUNT_TYPE = 2;

    /**
     * Vendor specific type.
     */
    protected static final int PHONE_ACCOUNT_TYPE = 3;

    /**
     * Account type for 360 People in the Native Accounts. 
     * MUST be a copy of type in 'res/xml/authenticator.xml' 
     */
    protected static final String PEOPLE_ACCOUNT_TYPE_STRING = "com.vodafone360.people.android.account";

    /**
     * Google account, there can be more than one of these
     */
    protected static final String GOOGLE_ACCOUNT_TYPE_STRING = "com.google";

    /**
     * There are devices with custom contact applications. For them we need
     * special handling of contacts.
     */
    protected static final String[] VENDOR_SPECIFIC_ACCOUNTS = {
            "com.htc.android.pcsc", "vnd.sec.contact.phone",
            "com.motorola.blur.service.bsutils.MOTHER_USER_CREDS_TYPE"
    };
    
    /**
     * {@link NativeContactsApi} the singleton instance providing access to the correct Contacts API interface
     */    
    private static NativeContactsApi sInstance;
    
    /**
     * {@link Context} to be used by the Instance
     */
    protected Context mContext;

    /**
     * {@link ContentResolver} be used by the Instance
     */
    protected ContentResolver mCr;

    /**
     * Sadly have to have this so that only one organization may be read from a
     * NAB Contact
     */
    protected boolean mHaveReadOrganization = false;

    /**
     * Sadly have to have this because Organization detail is split into two
     * details in CAB
     */
    protected int mMarkedOrganizationIndex = -1;

    /**
     * Sadly have to have this because Organization detail is split into two
     * details in CAB
     */
    protected int mMarkedTitleIndex = -1;

    /**
     * Create NativeContactsApi singleton instance for later usage. The instance
     * can retrieved by calling getInstance(). * The instance can be destroyed
     * by calling destroyInstance()
     * 
     * @see NativeContactsApi#getInstance()
     * @see NativeContactsApi#destroyInstance()
     * @param context The context to be used by the singleton
     */
    public static void createInstance(Context context) {
        LogUtils.logW("NativeContactsApi.createInstance()");
        String className;

        if (VersionUtils.is2XPlatform()) {
            className = "NativeContactsApi2";
            LogUtils.logD("Using 2.X Native Contacts API");
        } else {
            className = "NativeContactsApi1";
            LogUtils.logD("Using 1.X Native Contacts API");
        }

        try {
            Class<? extends NativeContactsApi> clazz = Class.forName(
                    NativeContactsApi.class.getPackage().getName() + "." + className).asSubclass(
                    NativeContactsApi.class);
            sInstance = clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("NativeContactsApi.createInstance()"
                    + "Error creating a subclass of NativeContactsApi", e);
        }

        sInstance.mContext = context;
        sInstance.mCr = context.getContentResolver();
        // Initialize the instance now (got Context and Content Resolver)
        sInstance.initialize();
    }

    /**
     * Destroy NativeContactsApi singleton instance if created. The instance can
     * be recreated by calling createInstance()
     * 
     * @see NativeContactsApi#createInstance()
     */
    public static void destroyInstance() {
        if (sInstance != null) {
            sInstance.mCr = null;
            sInstance.mContext = null;
            sInstance = null;
        }
    }

    /**
     * Retrieves singleton instance providing access to the native contacts api.
     * 
     * @return {@link NativeContactsApi} appropriate subclass instantiation
     */
    public static NativeContactsApi getInstance() {
        if (sInstance == null) {
            throw new InvalidParameterException("Please call "
                    + "NativeContactsApi.createInstance() "
                    + "before NativeContactsApi.getInstance()");
        }

        return sInstance;
    }

    /**
     * This Account class represents an available account on the device where
     * the native synchronization can be performed.
     */
    public static class Account {

        /**
         * The name of the account.
         */
        private String mName;

        /**
         * The type of the account.
         */
        private String mType;

        /**
         * The Constructor.
         * 
         * @param name the account name
         * @param type the account type
         */
        public Account(String name, String type) {
            mName = name;
            mType = type;
        }

        /**
         * Gets the name of the account.
         * 
         * @return the account name
         */
        public String getName() {
            return mName;
        }

        /**
         * Gets the type of the accounts.
         * 
         * @return the account type
         */
        public String getType() {
            return mType;
        }

        /**
         * Checks if this account is a People Account
         * 
         * @return true if this account is a People Account, false if not
         */
        public boolean isPeopleAccount() {
            return TextUtils.equals(mType, PEOPLE_ACCOUNT_TYPE_STRING);
        }

        /**
         * Returns a String representation of the account.
         */
        public String toString() {
            return "Account: name=" + mName + ", type=" + mType;
        }
    }

    /**
     * The Observer interface to receive notifications about changes in the
     * native address book.
     */
    public static interface ContactsObserver {

        /**
         * Call-back to notify that a change happened in the native address
         * book.
         */
        void onChange();
    }

    /**
     * Method meant to be called only just after createInstance() is invoked.
     * This method effectively acts as a replacement for the constructor because
     * of the use of reflection.
     */
    protected abstract void initialize();

    /**
     * Registers a content observer. Note: the method only supports one observer
     * at a time.
     * 
     * @param observer ContactsObserver currently observing native address book
     *            changes
     * @throws RuntimeException if a new observer is being registered without
     *             having unregistered the previous one
     */
    public abstract void registerObserver(ContactsObserver observer);

    /**
     * Unregister the previously registered content observer.
     */
    public abstract void unregisterObserver();

    /**
     * Fetches all the existing Accounts on the device. Only supported on 2.X.
     * The 1.X implementation always returns null.
     * 
     * @return An array containing all the Accounts on the device, or null if
     *         none exist
     */
    public abstract Account[] getAccounts();

    /**
     * Fetches all the existing Accounts on the device corresponding to the
     * provided Type Only supported on 2.X. The 1.X implementation always
     * returns null.
     * 
     * @param type The Type of Account to fetch
     * @return An array containing all the Accounts of the provided Type, or
     *         null if none exist
     */
    public abstract Account[] getAccountsByType(int type);

    /**
     * Adds the currently logged in user account to the NAB accounts. Only
     * supported on 2.X. The 1.X implementation does nothing.
     * 
     * @return true if successful, false if not
     */
    public abstract boolean addPeopleAccount(String username);

    /**
     * Checks if there is a People Account in the NAB Accounts. Only supported
     * on 2.X The 1.X implementation does nothing.
     * 
     * @return true if an account exists, false if not.
     */
    public abstract boolean isPeopleAccountCreated();

    /**
     * Removes the (first found) People Account from the NAB accounts. Only
     * supported on 2.X. The 1.X implementation does nothing.
     */
    public abstract void removePeopleAccount();

    /**
     * Retrieves a list of contact IDs for a specific account. In 1.X devices
     * only the null account is supported, i.e., a non null account always uses
     * null as a return value.
     * 
     * @param account The account to get contact IDs from (may be null)
     * @return List of contact IDs from the native address book
     */
    public abstract long[] getContactIds(Account account);

    /**
     * Gets data for one Contact.
     * 
     * @param nabContactId Native ID for the contact
     * @return A {@link ContactChange} array with contact's data or null
     */
    public abstract ContactChange[] getContact(long nabContactId);

    /**
     * Adds a contact. Note that the returned ID data will be the same size of
     * ccList plus one change containing the NAB Contact ID (at the first
     * position). The remaining IDs correspond to the details in the original
     * change list. Null IDs among these remaining IDs are possible when these
     * correspond to unsupported details. On 1.X Devices only a null account
     * parameter is expected.
     * 
     * @param account Account to be associated with the added contact (may be
     *            null).
     * @param ccList The Contact data as a {@link ContactChange} array
     * @return A {@link ContactChange} array with contact's new ID data or null
     *         in case of failure.
     */
    public abstract ContactChange[] addContact(Account account, ContactChange[] ccList);

    /**
     * Updates an existing contact. The returned ID data will be the same size
     * of the ccList. However, if a null or empty ccList is passed as an
     * argument then null is returned. Null IDs among the ID data are also
     * possible when these correspond to unsupported details.
     * 
     * @param ccList The Contact update data as a {@link ContactChange} array
     * @return A {@link ContactChange} array with the contact's new ID data or
     *         null
     */
    public abstract ContactChange[] updateContact(ContactChange[] ccList);

    /**
     * Removes a contact
     * 
     * @param nabContactId Native ID of the contact to remove
     */
    public abstract void removeContact(long nabContactId);

    /**
     * Checks whether or not a {@link ContactChange} key is supported. Results
     * may vary in 1.X and 2.X
     * 
     * @param key Key to check for support
     * @return true if supported, false if not
     */
    public abstract boolean isKeySupported(int key);
    /**
     * Checks if this account is a vendor specific one. All vendor specific
     * accounts are held in the VENDOR_SPECIFIC_ACCOUNTS array.
     * 
     * @param type to checks for
     * @return true if vendor specific account, false otherwise
     */
    public boolean isVendorSpecificAccount(String type) {
        for (String vendorSpecificType : VENDOR_SPECIFIC_ACCOUNTS) {
            if (vendorSpecificType.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
