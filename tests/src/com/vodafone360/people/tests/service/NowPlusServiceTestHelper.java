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

package com.vodafone360.people.tests.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.interfaces.IPeopleService;

class NowPlusServiceTestHelper {

    // private static String LOG_TAG = "NowPlusServiceTestHelper";
    // private static final int SERVICE_SYNC_TIMEOUT = 6000000; // If no
    // response is received by this time it is probably never coming back
    // private static final int SERVICE_TIMER_SYNC_RES = 10000; // Resolution to
    // check the timeout
    final static int WAIT_EVENT_TIMEOUT_MS = 30000;

    private IPeopleService mPeopleService = null;

    private final Object mUiRequestLock = new Object();

    private Integer mUiRequestId = null;

    // private Integer mUiRequestStatusId = null;
    // private Bundle mUiRequestData = null;
    // private boolean mDbChanged = false;

    private boolean mActive = false;

    private Thread mEventWatcherThread = null;

    boolean mEventThreadStarted = false;

    private Handler mHandler;

    private ServiceStatus mStatus = null;

    // private int mRequestdId;
    // private ServiceUiRequest mEventType = null;

    NowPlusServiceTestHelper(IPeopleService service) {
        mPeopleService = service;

        mEventWatcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                eventWatcherThreadMain();
            }
        });
        mEventWatcherThread.start();
        while (!mEventThreadStarted) {
            try {
                wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // add service handler
        mPeopleService.addEventCallback(mHandler);
    }

    protected void eventWatcherThreadMain() {
        Looper.prepare();
        synchronized (this) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    mActive = false;
                    synchronized (this) {
                        notifyAll();
                        processMsg(msg);
                    }
                }
            };
            mEventThreadStarted = true;
            notifyAll();
        }
        Looper.loop();

    }

    protected void processMsg(Message msg) {
        // mRequestdId = msg.arg1;
        mStatus = ServiceStatus.fromInteger(msg.arg2);
        // mUiRequestData = (Bundle) msg.obj;
    }

    /**
     * Start a synchronous ui request method
     * 
     * @param requestId
     * @return SUCCESS or one of the ServiceStatus error codes. Comment: Must be
     *         run inside the same synchronized(mUiRequestLock) block as
     *         finishSynchronousRequest
     */
    private ServiceStatus startSynchronousRequest(ServiceUiRequest requestId) {
        if (mUiRequestId != null) {
            return ServiceStatus.ERROR_IN_USE;
        }
        mUiRequestId = requestId.ordinal();
        // mUiRequestStatusId = null;
        // mUiRequestData = null;

        mActive = true;

        return ServiceStatus.SUCCESS;
    }

    private synchronized ServiceStatus waitForEvent(long timeout, int bitmask) {
        long timeToFinish = System.nanoTime() + (timeout * 1000000);
        long remainingTime = timeout;
        do {
            try {
                wait();
            } catch (InterruptedException e) {
            }
            remainingTime = (System.nanoTime() - timeToFinish) / 1000000;
        } while (mActive == true && remainingTime > 0);
        if (remainingTime <= 0) {
            return ServiceStatus.ERROR_UNKNOWN;
        }
        return mStatus;
    }

    /**
     * Synchronously fetches the state of the user name from the NOW+ server
     * 
     * @param username The user name to check (entered during registration)
     * @param data On success fills the given bundle with a key called
     *            "UsernameState.mBundleKey" and string data which is one of the
     *            UsernameState enum values.
     * @return SUCCESS or one of the ServiceStatus error codes.
     */
    public ServiceStatus fetchUsernameState(String username, Bundle data) {
        ServiceStatus status = ServiceStatus.SUCCESS;
        synchronized (mUiRequestLock) {
            status = startSynchronousRequest(ServiceUiRequest.USERNAME_AVAILABILITY);
            mPeopleService.fetchUsernameState(username);

            status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, 0);
        }

        return status;
    }

    /**
     * Retrieve the terms of service from the NOW+ server
     * 
     * @param result A bundle with the string result contained in the
     *            ServiceStatus.mGeneralResultBundleKey key.
     * @return SUCCESS or one of the ServiceStatus error codes.
     */
    public ServiceStatus fetchTermsOfService(Bundle result) {
        ServiceStatus status = ServiceStatus.SUCCESS;
        synchronized (mUiRequestLock) {
            status = startSynchronousRequest(ServiceUiRequest.FETCH_TERMS_OF_SERVICE);

            mPeopleService.fetchTermsOfService();

            status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, 0);
        }

        return status;
    }

    /**
     * Retrieve the privacy statement from the NOW+ server
     * 
     * @param result A bundle with the string result contained in the
     *            ServiceStatus.mGeneralResultBundleKey key.
     * @return SUCCESS or one of the ServiceStatus error codes.
     */
    public ServiceStatus fetchPrivacyStatement(Bundle result) {
        ServiceStatus status = ServiceStatus.SUCCESS;
        synchronized (mUiRequestLock) {
            status = startSynchronousRequest(ServiceUiRequest.FETCH_PRIVACY_STATEMENT);
            mPeopleService.fetchPrivacyStatement();
            status = waitForEvent(WAIT_EVENT_TIMEOUT_MS, 0);
        }

        return status;
    }

    /*
     * private IRemoteServiceCallback mCallback = new
     * IRemoteServiceCallback.Stub() {
     * @Override public void uiRequestComplete(int requestId, int statusId,
     * Bundle data) throws RemoteException { synchronized(mUiRequestLock) { if
     * (mUiRequestId != null && mUiRequestId.intValue() == requestId) {
     * mUiRequestStatusId = statusId; mUiRequestData = data;
     * mUiRequestLock.notifyAll(); } } }
     * @Override public void onContactSyncEvent(boolean syncActive, Bundle
     * bundleInfo) { Log.d(LOG_TAG, "Sync notification event received: " +
     * syncActive + ", info = " + bundleInfo); }
     * @Override public void onDatabaseChangeEvent(int changeTypeVal) throws
     * RemoteException { Database.DatabaseChangeType type = null; if
     * (changeTypeVal < Database.DatabaseChangeType.values().length) { type =
     * Database.DatabaseChangeType.values()[changeTypeVal]; } Log.d(LOG_TAG,
     * "Database change event received: " + type); mDbChanged = true; }
     * @Override public void onSettingChangeEvent(PersistSettings setting)
     * throws RemoteException { Log.d(LOG_TAG, "Setting change event received: "
     * + setting); } };
     */
    //	
    // public void Disconnect() {
    // Log.i (LOG_TAG, "Disconnect() called");
    // }
    //	
    //	
    // public ServiceStatus LogOnTest(LoginDetails details) {
    // ServiceStatus status = ServiceStatus.SUCCESS;
    // synchronized(mUiRequestLock) {
    // status = startSynchronousRequest(ServiceUiRequest.LOGIN);
    // try {
    // mRemoteService.logOn(details);
    // } catch (RemoteException e) {
    // status = ServiceStatus.ERROR_SERVICE_DISCONNECTED;
    // }
    // status = finishSynchronousRequest(status, null);
    // }
    //	    
    // Log.i(LOG_TAG, "Log on result: " + status);
    // return status;
    // }
    //
    // public boolean runDatabaseTests() {
    // Log.i (LOG_TAG, "runDatabaseTests ()");
    //		
    // ArrayList<ContactSummary> contactSummaryList = new
    // ArrayList<ContactSummary>();
    // Log.i(LOG_TAG, "Fetching number of contacts...");
    // int count = fetchNoOfContacts();
    // Log.i(LOG_TAG, "No of contacts = " + count);
    //		
    // if (count < 10) {
    // Log.i (LOG_TAG,"Fewer than 10 contacts found");
    //			
    // Log.i(LOG_TAG, "Populating database with dummy contacts...");
    //		
    // for (int i = 0 ; i < 1 ; i++) {
    // Log.i(LOG_TAG, "Populating group " + i + " please wait...");
    // if (!populateContacts()) {
    // Log.e(LOG_TAG, "Unable to add dummy contacts");
    // return false;
    // }
    // Log.i (LOG_TAG,"Database successfully populated");
    // count = fetchNoOfContacts();
    // }
    // }
    // else
    // {
    // Log.i (LOG_TAG, "Enough contacts already exist, not adding any more");
    // }
    //
    // int startSummaryIndex = 0;
    // for (int i = 0 ; i < count ; i+= 500) {
    // Log.i(LOG_TAG, "Fetching contact summary list starting at " + i);
    // startSummaryIndex = i;
    // if (!fetchContactSummaryList(contactSummaryList, startSummaryIndex, 500))
    // {
    // Log.e(LOG_TAG, "Unable to fetch summary list");
    // return false;
    // }
    //	
    // for (ContactSummary s : contactSummaryList) {
    // Log.i(LOG_TAG, s.toString());
    // }
    // }
    //		
    // Log.i (LOG_TAG, "Creating new contact ...");
    //		
    // Contact contact = new Contact();
    // contact.aboutMe = "Test information\"`!$%^&*()_-+=[]{};:'@#~,<.>/?\\|";
    // contact.contactID = 123L;
    // contact.deleted = false;
    // contact.friendOfMine = false;
    // contact.profilePath = "///sdcard/test/path/picture.jpg";
    // contact.updated = 0L;
    // contact.userID = "982";
    // {
    // Log.i (LOG_TAG, "Creating contacts details #1 ...");
    // ContactDetail detail = new ContactDetail();
    // detail.key = ContactDetail.detailKeys.VCARD_NAME;
    // detail.value = "Scott Kennedy";
    // contact.details.add(detail);
    //			
    // Log.i (LOG_TAG, "Details Key   : " + detail.key);
    // Log.i (LOG_TAG, "Details Value : " + detail.value);
    // }
    // Log.i (LOG_TAG, "Contact details ...");
    // Log.i (LOG_TAG, "aboutMe      : " + contact.aboutMe);
    // Log.i (LOG_TAG, "contactId    : " + contact.contactID);
    // Log.i (LOG_TAG, "deleted      : " + contact.deleted);
    // Log.i (LOG_TAG, "friendOfMine : " + contact.friendOfMine);
    // Log.i (LOG_TAG, "profilePath  : " + contact.profilePath);
    // Log.i (LOG_TAG, "updated      : " + contact.updated);
    // Log.i (LOG_TAG, "userID       : " + contact.userID);
    //		
    //		
    // {
    // Log.i (LOG_TAG, "Creating contact details #2 ...");
    // ContactDetail detail = new ContactDetail();
    // detail.key = ContactDetail.detailKeys.PRESENCE_TEXT;
    // detail.value = "test status 1";
    // contact.details.add(detail);
    //			
    // Log.i (LOG_TAG, "Details Key      : " + detail.key);
    // Log.i (LOG_TAG, "Details Value    : " + detail.value);
    // }
    //
    // {
    // Log.i (LOG_TAG, "Creating contact details #3 ...");
    // ContactDetail detail = new ContactDetail();
    // detail.key = ContactDetail.detailKeys.PRESENCE_TEXT;
    // detail.value = "Test presence text";
    // contact.details.add(detail);
    // contact.aboutMe = detail.value;
    //			
    // Log.i (LOG_TAG, "Details Key     : " + detail.key);
    // Log.i (LOG_TAG, "Details Value   : " + detail.value);
    // Log.i (LOG_TAG, "Contact AboutMe : " + contact.aboutMe);
    // }
    //
    // Log.i(LOG_TAG, "Adding test contact...");
    // if (!addContact(contact)) {
    // Log.e(LOG_TAG, "Unable to add contact " + contact.toString());
    // return false;
    // }
    // Log.i(LOG_TAG, "Added Contact " + contact.toString());
    //
    // Contact contact2 = new Contact();
    // Log.i(LOG_TAG, "Fetching contact...");
    // if (!fetchContact(contact.localContactID, contact2)) {
    // Log.e(LOG_TAG, "Unable to fetch added contact " +
    // contact.localContactID);
    // return false;
    // }
    // Log.i(LOG_TAG, "Fetched Contact " + contact2.toString());
    //		
    // Log.i(LOG_TAG, "Deleting contact...");
    // ServiceStatus status = deleteContact(contact.localContactID);
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Unable to delete contact " + contact.localContactID);
    // return false;
    // }
    //
    // Log.i(LOG_TAG, "Fetching deleted contact...");
    // if (fetchContact(contact.localContactID, contact2)) {
    // Log.e(LOG_TAG, "Fetched Contact - it was not deleted");
    // return false;
    // }
    //
    // Log.i(LOG_TAG, "SummaryList size = "+ contactSummaryList.size());
    //		
    // if (contactSummaryList.size() > 0)
    // {
    // Log.i(LOG_TAG, "Adding contact detail...");
    // ContactDetail detail = new ContactDetail();
    //			
    // detail.key = ContactDetail.detailKeys.VCARD_PHONE;
    // detail.keyType = ContactDetail.detailKeyTypes.CELL;
    // detail.value = "+447955123456";
    // long testContactID = contactSummaryList.get(0).localContactID;
    // detail.localContactID = testContactID;
    //			
    // Log.i (LOG_TAG, "Details Key     : " + detail.key);
    // Log.i (LOG_TAG, "Detail Key Type : " + detail.keyType);
    // Log.i (LOG_TAG, "Detail Value    : " + detail.value);
    // if (!addContactDetail(detail)) {
    // Log.e(LOG_TAG, "Unable to add contact detail to contact " +
    // contact.localContactID);
    // return false;
    // }
    // Log.i (LOG_TAG, "Contact detail added successfully");
    //
    // if (!displayContactDetail(testContactID, detail.localDetailID)) {
    // Log.e(LOG_TAG, "Unable to find new detail after fetch");
    // return false;
    // }
    //			
    // Log.i (LOG_TAG, "New details etrieved successfully");
    // Log.i(LOG_TAG, "Modifying contact detail...");
    // detail.value = "+4479654321";
    //			
    // Log.i (LOG_TAG, "Modifying detail value to " + detail.value);
    // if (!modifyContactDetail(detail)) {
    // Log.e(LOG_TAG, "Unable to modify contact detail " +
    // detail.localDetailID);
    // return false;
    // }
    //			
    // Log.i (LOG_TAG, "Contact detail modified successfully");
    //
    // if (!displayContactDetail(testContactID, detail.localDetailID)) {
    // Log.e(LOG_TAG, "Unable to find modified detail after fetch");
    // return false;
    // }
    // Log.i (LOG_TAG, "Modified details found successfully");
    //			
    // Log.i(LOG_TAG, "Deleting contact detail...");
    // if (!removeContactDetail(detail.localDetailID)) {
    // Log.e(LOG_TAG, "Unable to delete contact detail " +
    // detail.localDetailID);
    // return false;
    // }
    // Log.i (LOG_TAG, "Contact detail deleted successfully");
    //			
    // if (displayContactDetail(testContactID, detail.localDetailID)) {
    // Log.e(LOG_TAG, "Contact detail has been fetched after delete");
    // return false;
    // }
    // Log.i (LOG_TAG,"Correctly, unable to get non-existent contact detail");
    //			
    // Log.i(LOG_TAG, "Fetching contact (2)...");
    // if (!fetchContact(testContactID, contact2)) {
    // Log.e(LOG_TAG, "Unable to fetch contact before adding detail " +
    // testContactID);
    // return false;
    // }
    //			
    // Log.i (LOG_TAG,"Fetched contact successfully before adding detail");
    //
    // for (ContactDetail d : contact2.details ) {
    // if (d.key == ContactDetail.detailKeys.PRESENCE_TEXT ) {
    // Log.i(LOG_TAG, "Removing presence status detail...");
    // if (!removeContactDetail(d.localDetailID)) {
    // Log.e(LOG_TAG, "Unable to remove presence status detail " +
    // d.localDetailID);
    // return false;
    // }
    // Log.i (LOG_TAG, "Presence status detail successfully removed");
    // }
    // }
    //
    // {
    // Log.i(LOG_TAG, "Fetching contact summary for test contact...");
    // final ArrayList<ContactSummary> tempList = new
    // ArrayList<ContactSummary>();
    // if (!fetchContactSummaryList(tempList, startSummaryIndex, 1) ||
    // tempList.size() < 1) {
    // Log.e(LOG_TAG, "Unable to fetch contact summary after deleting detail " +
    // startSummaryIndex);
    // return false;
    // }
    // if (tempList.get(0).statusText != null) {
    // Log.e(LOG_TAG,
    // "There is no presence text contact detail so \"ContactSummary.statusText\" field should be null, but ContactSummary.statusText="
    // + tempList.get(0).statusText);
    // return false;
    // }
    // }
    //
    // Log.i(LOG_TAG, "Adding contact detail (4)...");
    // detail.key = ContactDetail.detailKeys.PRESENCE_TEXT;
    // detail.keyType = ContactDetail.detailKeyTypes.UNKNOWN;
    // detail.value = "This is my new presence status";
    // detail.localContactID = testContactID;
    //			
    // Log.i (LOG_TAG, "Detail Key              : " + detail.key);
    // Log.i (LOG_TAG, "Detail KeyType          : " + detail.keyType);
    // Log.i (LOG_TAG, "Detail Value            : " + detail.value);
    // Log.i (LOG_TAG, "Details local contactID : " + detail.localContactID);
    // if (!addContactDetail(detail)) {
    // Log.e(LOG_TAG, "Unable to add contact detail to contact " +
    // contact.localContactID);
    // return false;
    // }
    // Log.i (LOG_TAG, "Contact detail added successfully");
    //			
    // {
    // Log.i(LOG_TAG, "Fetching contact summary for test contact...");
    // final ArrayList<ContactSummary> tempList = new
    // ArrayList<ContactSummary>();
    // if (!fetchContactSummaryList(tempList, startSummaryIndex, 1) ||
    // tempList.size() < 1) {
    // Log.e(LOG_TAG, "Unable to fetch contact summary after deleting detail " +
    // startSummaryIndex);
    // return false;
    // }
    // if (!tempList.get(0).localContactID.equals(detail.localContactID)) {
    // Log.e(LOG_TAG,
    // "Contact ID mismatch - the test code has failed (Expected id = " +
    // detail.localContactID + ", actual ID = " + tempList.get(0).localContactID
    // + ")");
    // return false;
    // }
    // if (tempList.get(0).statusText == null ||
    // !detail.value.equals(tempList.get(0).statusText)) {
    // Log.e(LOG_TAG,
    // "The ContactSummary.statusText field was not updated when status text contact detail was added: Expected value: \""
    // + detail.value + "\", actual value: \"" + tempList.get(0).statusText +
    // "\"");
    // return false;
    // }
    // Log.i (LOG_TAG, "Status text field was updated successfully with \"" +
    // tempList.get(0).statusText + "\"");
    // }
    // }
    //		
    // return true;
    // }
    //
    // public boolean displayContactDetail(long contactID, long detailID) {
    // Contact contact = new Contact();
    //		
    // Log.i (LOG_TAG, "Calling displayContactDetail ()");
    // Log.i (LOG_TAG, "contactID: " + contactID+ ", detailID : " + detailID);
    // if (!fetchContact(contactID, contact)) {
    // Log.e(LOG_TAG, "Unable to fetch contact with modified detail");
    // return false;
    // }
    // Log.i (LOG_TAG, "Fetched contact with modified details successfully");
    //		
    // for (ContactDetail d : contact.details) {
    // if (d.localDetailID == detailID) {
    // Log.i(LOG_TAG, "Fetched Contact Detail" + d.toString());
    // return true;
    // }
    // }
    // Log.e (LOG_TAG, "displayContactDetail() has failed!!");
    // return false;
    // }
    //
    // public boolean addContact(Contact contact) {
    // Log.i(LOG_TAG, "Calling addContact ()");
    //		
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.addContact(contact));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Add contact failed: " + status);
    // return false;
    // }
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG, "Remote exception has occurred while adding contact");
    // return false;
    // }
    // return true;
    // }
    //	
    // public ServiceStatus deleteContact(long localContactID) {
    // Log.i (LOG_TAG, "Calling deletedContact ()");
    // Log.i (LOG_TAG, "localContactID : "+ localContactID);
    //		
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.deleteContact(localContactID));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Delete Contact Failed: " + status);
    // return status;
    // }
    // Log.i (LOG_TAG, "Contact deleted successfully");
    //			
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG, "Remote exception has occurred while deleting contact");
    // return ServiceStatus.ERROR_UNKNOWN;
    // }
    // return ServiceStatus.SUCCESS;
    // }
    //
    // public boolean populateContacts() {
    // Log.i (LOG_TAG,"Calling populateContacts ()");
    //		
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.testPopulate());
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Test populate failed: " + status);
    // return false;
    // }
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG, "Remote exception has occurred while populating");
    // return false;
    // }
    // return true;
    // }
    //	
    // public boolean fetchContact(long localContactID, Contact contact) {
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.fetchContact(localContactID,
    // contact));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Fetch contact failed: " + status);
    // return false;
    // }
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG, "Remote exception has occurred while fetching contact");
    // return false;
    // }
    // return true;
    // }
    //
    // public int fetchNoOfContacts() {
    // Log.i (LOG_TAG,"Calling fetchNoOfContacts ()");
    // try {
    // int count = mRemoteService.fetchNoOfContacts();
    // Log.i (LOG_TAG,"Number Of Contacts Fetched = "+ count);
    // return count;
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG, "Unable to fetch number of contacts");
    // return 0;
    // }
    //		
    // }
    // public boolean fetchContactSummaryList(ArrayList<ContactSummary> list,
    // int first, int count) {
    // Log.i (LOG_TAG, "Calling fetchContactSummaryList ()");
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.fetchContactList(list, first,
    // count));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Fetch contact list failed: " + status);
    // return false;
    // }
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG,
    // "Remote exception has occurred while fetching contact list");
    // return false;
    // }
    // return true;
    // }
    //	
    // public boolean addContactDetail(ContactDetail detail) {
    // Log.i (LOG_TAG, "Calling addContactDetail ()");
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.addContactDetail(detail));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Add contact detail failed: " + status);
    // return false;
    // }
    // Log.i (LOG_TAG, "Contact detail added successfully");
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG,
    // "Remote exception has occurred while adding contact detail");
    // return false;
    // }
    // return true;
    // }
    //
    // public boolean removeContactDetail(long localDetailID) {
    // Log.i (LOG_TAG, "Calling removeContactDetail ()");
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.removeContactDetail(localDetailID));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Remove contact detail failed: " + status);
    // return false;
    // }
    // Log.i (LOG_TAG, "Contact detail removed successfully");
    // } catch (RemoteException e) {
    // Log.e(LOG_TAG,
    // "Remote exception has occurred while removing contact detail");
    // return false;
    // }
    // return true;
    // }
    //
    // public boolean modifyContactDetail(ContactDetail detail) {
    // Log.i (LOG_TAG, "Calling modifyContactDetail ()");
    // try {
    // ServiceStatus status =
    // ServiceStatus.fromInteger(mRemoteService.modifyContactDetail(detail));
    // if (ServiceStatus.SUCCESS != status) {
    // Log.e(LOG_TAG, "Modify contact detail failed: " + status);
    // return false;
    // }
    // Log.i (LOG_TAG, "Modify contact details successful");
    // } catch (RemoteException e) {
    // Log.e("NowPlusServiceTestHelper",
    // "Remote exception has occurred while modifying contact detail");
    // return false;
    // }
    // return true;
    // }
    //	
    public boolean register(RegistrationDetails details) {
        ServiceStatus status = ServiceStatus.SUCCESS;
        synchronized (mUiRequestLock) {
            status = startSynchronousRequest(ServiceUiRequest.REGISTRATION);

            mPeopleService.register(details);

            // status = finishSynchronousRequest(status, null);
        }

        Log.i("NowPlusServiceTestHelper", "Registration result: " + status);
        return (status.equals(ServiceStatus.SUCCESS));
    }

    //	
    // public ServiceStatus fetchIdentities(Bundle filter, List<Identity>
    // identityList) {
    // ServiceStatus status = ServiceStatus.SUCCESS;
    // synchronized(mUiRequestLock) {
    // status = startSynchronousRequest(ServiceUiRequest.FETCH_IDENTITIES);
    // try {
    // mRemoteService.fetchAvailableIdentities(filter);
    // } catch (RemoteException e) {
    // status = ServiceStatus.ERROR_SERVICE_DISCONNECTED;
    // }
    // status = finishSynchronousRequest(status, null);
    // }
    //	    
    // return status;
    // }
    //
    // /**
    // * Run a full contact sync with the NOW+ server
    // * @return SUCCESS or one of the ServiceStatus error codes.
    // */
    // public ServiceStatus runContactSync() {
    // mDbChanged = false;
    // if (mRemoteService == null) {
    // return ServiceStatus.ERROR_SERVICE_DISCONNECTED;
    // }
    // ServiceStatus status = ServiceStatus.SUCCESS;
    // synchronized(mUiRequestLock) {
    // status = startSynchronousRequest(ServiceUiRequest.NOWPLUSSYNC);
    // try {
    // mRemoteService.startContactsSync();
    // } catch (RemoteException e) {
    // status = ServiceStatus.ERROR_SERVICE_DISCONNECTED;
    // }
    // status = finishSynchronousRequest(status, null);
    // }
    //	    
    // return status;
    // }
    // public boolean hasDbChanged() {
    // return mDbChanged;
    // }
    // public boolean checkSummary(Contact contact, ContactSummary summary) {
    // if (!checkContactDetail(contact, ContactDetail.detailKeys.VCARD_NAME,
    // summary.formattedName)) {
    // return false;
    // }
    // if (!checkContactDetail(contact, ContactDetail.detailKeys.PRESENCE_TEXT,
    // summary.statusText)) {
    // return false;
    // }
    // return true;
    // }
    // public boolean checkContactDetail(Contact c, ContactDetail.detailKeys
    // key, String value) {
    // for (ContactDetail d : c.details) {
    // if (d.key.equals(key)) {
    // if (value != null) {
    // if (d.key == ContactDetail.detailKeys.VCARD_NAME &&
    // value.equals(d.getName().toString())) {
    // return true;
    // } else if (value.equals(d.value)) {
    // return true;
    // } else {
    // Log.e(LOG_TAG, "Comparison failed - Summary value " + value +
    // " is different for key " + key + ", value " + d.value);
    // return false;
    // }
    // }
    // }
    // }
    // if (value != null) {
    // Log.e(LOG_TAG, "Comparison failed - Summary value " + value +
    // " is different for key " + key + " which was not found");
    // return false;
    // }
    // return true;
    // }
    // public boolean areContactsIdentical(Contact contact1, Contact contact2) {
    // Log.i(LOG_TAG, "Comparing contacts...");
    // if (!areObjectsEqual(contact1.aboutMe, contact2.aboutMe)) {
    // Log.e(LOG_TAG, "Comparison failed - About me is different: 1=" +
    // contact1.aboutMe + ", 2=" + contact2.aboutMe);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.contactID, contact2.contactID)) {
    // Log.e(LOG_TAG, "Comparison failed - contactID is different: 1=" +
    // contact1.contactID + ", 2=" + contact2.contactID);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.deleted, contact2.deleted)) {
    // Log.e(LOG_TAG, "Comparison failed - deleted is different: 1=" +
    // contact1.deleted + ", 2=" + contact2.deleted);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.friendOfMine, contact2.friendOfMine)) {
    // Log.e(LOG_TAG, "Comparison failed - friendOfMine is different: 1=" +
    // contact1.friendOfMine + ", 2=" + contact2.friendOfMine);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.gender, contact2.gender)) {
    // Log.e(LOG_TAG, "Comparison failed - gender is different: 1=" +
    // contact1.gender + ", 2=" + contact2.gender);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.localContactID, contact2.localContactID)) {
    // Log.e(LOG_TAG, "Comparison failed - localContactID is different: 1=" +
    // contact1.localContactID + ", 2=" + contact2.localContactID);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.nativeContactId, contact2.nativeContactId))
    // {
    // Log.e(LOG_TAG, "Comparison failed - nativeContactId is different: 1=" +
    // contact1.nativeContactId + ", 2=" + contact2.nativeContactId);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.profilePath, contact2.profilePath)) {
    // Log.e(LOG_TAG, "Comparison failed - profilePath is different: 1=" +
    // contact1.profilePath + ", 2=" + contact2.profilePath);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.updated, contact2.updated)) {
    // Log.e(LOG_TAG, "Comparison failed - updated is different: 1=" +
    // contact1.updated + ", 2=" + contact2.updated);
    // return false;
    // }
    // if (!areObjectsEqual(contact1.userID, contact2.userID)) {
    // Log.e(LOG_TAG, "Comparison failed - userID is different: 1=" +
    // contact1.userID + ", 2=" + contact2.userID);
    // return false;
    // }
    // if (contact1.details.size() != contact2.details.size()) {
    // Log.e(LOG_TAG, "Comparison failed - details are different length: 1=" +
    // contact1.details.size() + ", 2=" + contact2.details.size());
    // return false;
    // }
    //    	
    // for (ContactDetail detail1 : contact1.details) {
    // boolean found = false;
    // for (ContactDetail detail2 : contact2.details) {
    // if (detail1.key == detail2.key) {
    // if (areObjectsEqual(detail1.unique_id, detail2.unique_id)) {
    // if (!areObjectsEqual(detail1.value, detail2.value)) {
    // Log.e(LOG_TAG, "Comparison failed - contact detail " + detail1.key + ", "
    // + detail1.unique_id + " is different: 1=" + detail1.value + ", 2=" +
    // detail2.value);
    // }
    // found = true;
    // break;
    // }
    // }
    // }
    // if (!found) {
    // Log.e(LOG_TAG, "Comparison failed - contact detail " + detail1.key + ", "
    // + detail1.unique_id + " could not be found");
    // return false;
    // }
    // }
    // return true;
    // }
    //    
    // public boolean areObjectsEqual(Object obj1, Object obj2) {
    // if (obj1 == null && obj2 == null) {
    // return true;
    // }
    // if (obj1 == null || obj2 == null) {
    // return false;
    // }
    // return (obj1.equals(obj2));
    // }
    //
    // public boolean deleteDatabase() {
    // try {
    // Log.i(LOG_TAG, "*** Deleting Database ***");
    // mRemoteService.deleteDatabase();
    // return true;
    // } catch (RemoteException e) {
    // e.printStackTrace();
    // return false;
    // }
    // }
    // public boolean login() {
    // Log.i(LOG_TAG, "Trying to login...");
    // LoginDetails details = new LoginDetails();
    // details.mUsername = "scottkennedy1111";
    // details.mPassword = "mobicatest";
    // details.mMobileNo = "+447775128930";
    // details.mAutoConnect = true;
    // details.mRememberMe = true;
    // ServiceStatus status = LogOnTest(details);
    // if (ServiceStatus.SUCCESS == status) {
    // Log.i(LOG_TAG, "LogOnTest (SUCCESS)");
    // } else {
    // Log.e(LOG_TAG, "doLoginTest Failed - Failed to logon status = " +
    // status);
    // return false;
    // }
    // Log.i(LOG_TAG, "**** doLoginTest (SUCCESS) ****\n");
    // return true;
    // }
}
