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
import java.util.ListIterator;

import android.app.Instrumentation;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactChangeLogTable;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactDetailDeletion;
import com.vodafone360.people.datatypes.ContactListResponse;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.datatypes.ItemList;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.engine.contactsync.UploadServerContacts;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.tests.engine.EngineTestFramework;
import com.vodafone360.people.tests.engine.IEngineTestFrameworkObserver;

public class UploadServerContactsTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver {
    private static final String LOG_TAG = "UploadServerContactsTest";

    private static final long MAX_PROCESSOR_TIME = 3000000;

    private static final long MAX_WAIT_FOR_REQ_ID = 5000;

    private static final int NO_OF_CONTACTS_TO_ADD = 55;

    private static final String NEW_DETAIL_VALUE = "0123456789";

    private static final ContactDetail.DetailKeys NEW_DETAIL_KEY = ContactDetail.DetailKeys.VCARD_PHONE;

    private static final ContactDetail.DetailKeyTypes NEW_DETAIL_TYPE = ContactDetail.DetailKeyTypes.CELL;

    private static final String MOD_DETAIL_VALUE = ";Test;One";

    private static final ContactDetail.DetailKeys MOD_DETAIL_KEY = ContactDetail.DetailKeys.VCARD_NAME;

    private static final ContactDetail.DetailKeyTypes MOD_DETAIL_TYPE = null;

    private static final long TEST_GROUP_1 = 860909;

    private static final long TEST_GROUP_2 = 860910;

    enum State {
        IDLE,
        ADD_CONTACT_LIST,
        MODIFY_CONTACT_LIST,
        DELETE_CONTACT_LIST,
        DELETE_CONTACT_DETAIL_LIST,
        ADD_NEW_GROUP_LIST,
        DELETE_GROUP_LIST
    }

    EngineTestFramework mEngineTester = null;

    MainApplication mApplication = null;

    DummyContactSyncEngine mEng = null;

    class UploadServerContactProcessorTest extends UploadServerContacts {

        public UploadServerContactProcessorTest(IContactSyncCallback callback, DatabaseHelper db) {
            super(callback, db);
        }

        public void testFetchContactChangeList(List<Contact> contactChangeList) {
            contactChangeList.clear();
            contactChangeList.addAll(getContactChangeList());
        }

        public void testFetchAddGroupLists(List<Long> contactIdList, List<GroupItem> groupList) {
            contactIdList.clear();
            groupList.clear();
            contactIdList.addAll(getContactIdList());
            groupList.addAll(getGroupList());
        }

        public void testFetchContactDeleteList(List<Long> contactIdList) {
            contactIdList.clear();
            contactIdList.addAll(getContactIdList());
        }

        public void testFetchContactDetailDeleteList(Contact deleteDetailContact) {
            android.os.Parcel _data = android.os.Parcel.obtain();
            getDeleteDetailContact().writeToParcel(_data, 0);
            _data.setDataPosition(0);
            deleteDetailContact.readFromParcel(_data);
        }

        public Long testFetchDeleteGroupList(List<Long> contactIdList) {
            contactIdList.clear();
            contactIdList.addAll(getContactIdList());
            return getActiveGroupId();
        }

        public int testGetPageSize() {
            return MAX_UP_PAGE_SIZE;
        }

        public void verifyNewContactsState() {
            assertEquals(InternalState.PROCESSING_NEW_CONTACTS, getInternalState());
        }

        public void verifyModifyDetailsState() {
            assertEquals(InternalState.PROCESSING_MODIFIED_DETAILS, getInternalState());
        }

        public void verifyDeleteContactsState() {
            assertEquals(InternalState.PROCESSING_DELETED_CONTACTS, getInternalState());
        }

        public void verifyDeleteDetailsState() {
            assertEquals(InternalState.PROCESSING_DELETED_DETAILS, getInternalState());
        }

        public void verifyGroupAddsState() {
            assertEquals(InternalState.PROCESSING_GROUP_ADDITIONS, getInternalState());
        }

        public void verifyGroupDelsState() {
            assertEquals(InternalState.PROCESSING_GROUP_DELETIONS, getInternalState());
        }
    };

    UploadServerContactProcessorTest mProcessor;

    DatabaseHelper mDb = null;

    State mState = State.IDLE;

    Contact mReplyContact = null;

    int mInitialCount;

    int mInitialContactGroupCount;

    int mItemCount;

    boolean mBulkContactTest;

    TestModule mTestModule = new TestModule();

    int mTestStep;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
                getInstrumentation().getTargetContext());
        mApplication.onCreate();
        mDb = mApplication.getDatabase();
        mDb.removeUserData();

        mEngineTester = new EngineTestFramework(this);
        mEng = new DummyContactSyncEngine(mEngineTester);
        mProcessor = new UploadServerContactProcessorTest(mEng, mApplication.getDatabase());
        mEng.setProcessor(mProcessor);
        mEngineTester.setEngine(mEng);
        mState = State.IDLE;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mBulkContactTest = false;
    }

    @Override
    protected void tearDown() throws Exception {
        // stop our dummy thread?
        mEngineTester.stopEventThread();
        mEngineTester = null;
        mEng = null;
        SQLiteDatabase db = mDb.getReadableDatabase();
        if (db.inTransaction()) {
            db.endTransaction();
        }
        db.close();
        super.tearDown();
    }

    private void startSubTest(String function, String description) {
        Log.i(LOG_TAG, function + " - step " + mTestStep + ": " + description);
        mTestStep++;
    }

    private void runProcessor(int count, State state) {
        mInitialCount = count;
        nextState(state);
        mEng.mProcessorCompleteFlag = false;
        mProcessor.start();
        ServiceStatus status = mEng.waitForProcessorComplete(MAX_PROCESSOR_TIME);
        assertEquals(ServiceStatus.SUCCESS, status);
    }

    private void nextState(State state) {
        mItemCount = mInitialCount;
        if (mItemCount == 0) {
            mState = State.IDLE;
        } else {
            mState = state;
        }
    }

    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        Log.d(LOG_TAG, "reportBackToEngine");
        ResponseQueue respQueue = ResponseQueue.getInstance();
        List<BaseDataType> data = new ArrayList<BaseDataType>();
        try {
            assertEquals(mEng.engineId(), engine);
            synchronized (mEng.mWaitForReqIdLock) {
                if (mEng.mActiveReqId == null || mEng.mActiveReqId.intValue() != reqId) {
                    try {
                        mEng.mWaitForReqIdLock.wait(MAX_WAIT_FOR_REQ_ID);
                    } catch (InterruptedException e) {
                    }
                    assertEquals(Integer.valueOf(reqId), mEng.mActiveReqId);
                }
            }
            switch (mState) {
                case ADD_CONTACT_LIST:
                    reportBackAddContactSuccess(reqId, data);
                    break;
                case MODIFY_CONTACT_LIST:
                    reportModifyContactSuccess(reqId, data);
                    break;
                case DELETE_CONTACT_LIST:
                    reportDeleteContactSuccess(reqId, data);
                    break;
                case DELETE_CONTACT_DETAIL_LIST:
                    reportDeleteContactDetailSuccess(reqId, data);
                    break;
                case ADD_NEW_GROUP_LIST:
                    reportBackAddGroupSuccess(reqId, data);
                    break;
                case DELETE_GROUP_LIST:
                    reportDeleteGroupListSuccess(reqId, data);
                    break;
                default:
                    fail("Unexpected request from processor");
            }
        } catch (Throwable err) {
            ServerError serverError = new ServerError(ServerError.ErrorType.INTERNALERROR);
            serverError.errorDescription = err + "\n";
            for (int i = 0; i < err.getStackTrace().length; i++) {
                StackTraceElement v = err.getStackTrace()[i];
                serverError.errorDescription += "\t" + v + "\n";
            }
            Log.e(LOG_TAG, "Exception:\n" + serverError.errorDescription);
            data.clear();
            data.add(serverError);
        }
        respQueue.addToResponseQueue(new DecodedResponse(reqId, data, engine, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
        mEng.onCommsInMessage();
        Log.d(LOG_TAG, "reportBackToEngine - message added to response queue");
    }

    private void reportBackAddContactSuccess(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackAddContactSuccess");
        mProcessor.verifyNewContactsState();
        List<Contact> contactChangeList = new ArrayList<Contact>();
        mProcessor.testFetchContactChangeList(contactChangeList);

        assertEquals(Math.min(mItemCount, mProcessor.testGetPageSize()), contactChangeList.size());
        ContactChanges contactChanges = new ContactChanges();
        contactChanges.mServerRevisionAfter = 1;
        contactChanges.mServerRevisionBefore = 0;
        data.add(contactChanges);
        for (int i = 0; i < contactChangeList.size(); i++) {
            Contact actualContact = contactChangeList.get(i);
            Contact expectedContact = new Contact();
            ServiceStatus status = mDb.fetchContact(actualContact.localContactID, expectedContact);
            assertEquals(ServiceStatus.SUCCESS, status);
            assertEquals(expectedContact.details.size(), actualContact.details.size());
            assertEquals(null, actualContact.aboutMe);
            assertEquals(null, actualContact.profilePath);
            assertEquals(null, actualContact.contactID);
            assertEquals(null, actualContact.deleted);
            assertEquals(null, actualContact.friendOfMine);
            assertEquals(null, actualContact.gender);
            assertEquals(null, actualContact.groupList);
            assertEquals(null, actualContact.sources);
            assertEquals(expectedContact.synctophone, actualContact.synctophone);
            assertEquals(null, actualContact.updated);
            assertEquals(null, actualContact.userID);

            final ListIterator<ContactDetail> itActDetails = actualContact.details.listIterator();
            for (ContactDetail expDetail : expectedContact.details) {
                ContactDetail actDetail = itActDetails.next();
                assertTrue(DatabaseHelper.doDetailsMatch(expDetail, actDetail));
                assertFalse(DatabaseHelper.hasDetailChanged(expDetail, actDetail));
            }
            generateReplyContact(expectedContact);
            contactChanges.mContacts.add(mReplyContact);
        }

        mItemCount -= contactChangeList.size();
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            mInitialCount = mInitialContactGroupCount;
            nextState(State.ADD_NEW_GROUP_LIST);
        }
    }

    private void reportBackAddGroupSuccess(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackAddGroupSuccess");
        mProcessor.verifyGroupAddsState();
        final List<Long> contactIdList = new ArrayList<Long>();
        final List<GroupItem> groupList = new ArrayList<GroupItem>();
        mProcessor.testFetchAddGroupLists(contactIdList, groupList);
        assertEquals(1, groupList.size());
        Long activeGroupId = groupList.get(0).mId;
        assertTrue(activeGroupId != null);

        ItemList itemList = new ItemList(ItemList.Type.contact_group_relations);
        data.add(itemList);
        for (Long contactServerId : contactIdList) {
            Contact expectedContact = new Contact();
            ServiceStatus status = mDb.fetchContactByServerId(contactServerId, expectedContact);
            assertEquals(ServiceStatus.SUCCESS, status);
            boolean found = false;
            for (Long groupId : expectedContact.groupList) {
                if (groupId.equals(activeGroupId)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Contact " + contactServerId + " has been added to group " + activeGroupId
                    + " which is not in the database", found);
            mItemCount--;
        }
        Log.i(LOG_TAG, "Groups/contacts remaining = " + mItemCount);
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportModifyContactSuccess(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportModifyContactSuccess");
        mProcessor.verifyModifyDetailsState();
        List<Contact> contactChangeList = new ArrayList<Contact>();
        mProcessor.testFetchContactChangeList(contactChangeList);

        assertEquals(Math.min(mItemCount, mProcessor.testGetPageSize()), contactChangeList.size());
        ContactChanges contactChanges = new ContactChanges();
        contactChanges.mServerRevisionAfter = 1;
        contactChanges.mServerRevisionBefore = 0;
        data.add(contactChanges);
        for (int i = 0; i < contactChangeList.size(); i++) {
            Contact actualContact = contactChangeList.get(i);
            assertTrue(actualContact.contactID != null);
            assertEquals(null, actualContact.aboutMe);
            assertEquals(null, actualContact.profilePath);
            assertEquals(null, actualContact.deleted);
            assertEquals(null, actualContact.friendOfMine);
            assertEquals(null, actualContact.gender);
            assertEquals(null, actualContact.groupList);
            assertEquals(null, actualContact.sources);
            assertEquals(null, actualContact.synctophone);
            assertEquals(null, actualContact.updated);
            assertEquals(null, actualContact.userID);

            assertEquals(2, actualContact.details.size());
            ContactDetail modDetail = actualContact.details.get(0); // Modified
                                                                    // detail
                                                                    // always
                                                                    // first
            ContactDetail newDetail = actualContact.details.get(1);

            assertEquals(NEW_DETAIL_VALUE, newDetail.value);
            assertEquals(NEW_DETAIL_KEY, newDetail.key);
            assertEquals(NEW_DETAIL_TYPE, newDetail.keyType);
            assertEquals(MOD_DETAIL_VALUE, modDetail.value);
            assertEquals(MOD_DETAIL_KEY, modDetail.key);
            assertEquals(MOD_DETAIL_TYPE, modDetail.keyType);

            mReplyContact = new Contact();
            mReplyContact.contactID = actualContact.contactID;
            mReplyContact.userID = generateTestUserID(mReplyContact.contactID);
            ContactDetail replyDetail1 = new ContactDetail();
            ContactDetail replyDetail2 = new ContactDetail();
            replyDetail1.key = modDetail.key;
            replyDetail1.unique_id = modDetail.unique_id;
            replyDetail2.key = newDetail.key;
            replyDetail2.unique_id = newDetail.localDetailID + 2;
            mReplyContact.details.add(replyDetail1);
            mReplyContact.details.add(replyDetail2);
            contactChanges.mContacts.add(mReplyContact);
        }

        mItemCount -= contactChangeList.size();
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }

    }

    private void reportDeleteContactSuccess(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportDeleteContactSuccess");
        mProcessor.verifyDeleteContactsState();
        List<Long> contactIdList = new ArrayList<Long>();
        mProcessor.testFetchContactDeleteList(contactIdList);

        assertEquals(Math.min(mItemCount, mProcessor.testGetPageSize()), contactIdList.size());
        ContactListResponse contactListResponse = new ContactListResponse();
        contactListResponse.mServerRevisionAfter = 1;
        contactListResponse.mServerRevisionBefore = 0;
        data.add(contactListResponse);
        contactListResponse.mContactIdList = new ArrayList<Integer>();
        for (Long serverID : contactIdList) {
            assertTrue(serverID != null);
            contactListResponse.mContactIdList.add(Integer.valueOf(serverID.intValue()));
        }

        mItemCount -= contactIdList.size();
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportDeleteContactDetailSuccess(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportDeleteContactDetailSuccess");
        mProcessor.verifyDeleteDetailsState();
        Contact contact = new Contact();
        mProcessor.testFetchContactDetailDeleteList(contact);

        assertEquals(2, contact.details.size());
        assertEquals(ContactDetail.DetailKeys.VCARD_NAME, contact.details.get(0).key);
        assertEquals(ContactDetail.DetailKeys.VCARD_PHONE, contact.details.get(1).key);

        ContactDetailDeletion contactDetailDeletion = new ContactDetailDeletion();
        contactDetailDeletion.mServerVersionAfter = 1;
        contactDetailDeletion.mServerVersionBefore = 0;
        data.add(contactDetailDeletion);
        contactDetailDeletion.mContactId = contact.contactID.intValue();
        contactDetailDeletion.mDetails = new ArrayList<ContactDetail>();
        for (ContactDetail detail : contact.details) {
            ContactDetail tempDetail = new ContactDetail();
            tempDetail.key = detail.key;
            tempDetail.unique_id = detail.unique_id;
            contactDetailDeletion.mDetails.add(tempDetail);
        }

        mItemCount--;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportDeleteGroupListSuccess(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportDeleteGroupListSuccess");
        mProcessor.verifyGroupDelsState();
        final List<Long> contactIdList = new ArrayList<Long>();
        Long activeGroupId = mProcessor.testFetchDeleteGroupList(contactIdList);

        if (mItemCount == 1) {
            assertEquals(Long.valueOf(TEST_GROUP_2), activeGroupId);
        } else if (mItemCount == 2) {
            assertEquals(Long.valueOf(TEST_GROUP_1), activeGroupId);
        } else {
            fail("Unexpected number of groups in delete group list");
        }
        StatusMsg statusMsg = new StatusMsg();
        statusMsg.mStatus = true;
        data.add(statusMsg);

        mItemCount--;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private Long generateTestUserID(Long contactID) {
        if (contactID == null) {
            return null;
        }
        if ((contactID & 15) == 0) {
            return null;
        }
        return contactID + 5;
    }

    private void generateReplyContact(Contact expContact) {
        mReplyContact = new Contact();
        mReplyContact.contactID = expContact.localContactID + 1;
        if ((expContact.localContactID & 15) != 0) {
            mReplyContact.userID = expContact.localContactID + 2;
        }
        for (ContactDetail detail : expContact.details) {
            ContactDetail newDetail = new ContactDetail();
            generateReplyDetail(newDetail, detail);
            mReplyContact.details.add(newDetail);
        }
    }

    private void generateReplyDetail(ContactDetail replyDetail, ContactDetail expDetail) {
        replyDetail.key = expDetail.key;
        switch (replyDetail.key) {
            case VCARD_NAME:
            case VCARD_NICKNAME:
            case VCARD_DATE:
            case VCARD_TITLE:
            case PRESENCE_TEXT:
            case PHOTO:
            case LOCATION:
                break;
            default:
                replyDetail.unique_id = expDetail.localDetailID + 3;
                break;
        }
    }

    @Override
    public void onEngineException(Exception e) {
        mEng.onProcessorComplete(ServiceStatus.ERROR_UNKNOWN, "", e);
    }

    @SmallTest
    @Suppress
    // Breaks tests
    public void testRunWithNoContactChanges() {
        final String fnName = "testRunWithNoContactChanges";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        ServiceStatus status = mEng.waitForProcessorComplete(MAX_PROCESSOR_TIME);
        assertEquals(ServiceStatus.SUCCESS, status);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithNewContactChange() {
        final String fnName = "testRunWithNewContactChange";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database");
        Contact contact = mTestModule.createDummyContactData();
        ServiceStatus status = mDb.addContact(contact);
        assertEquals(ServiceStatus.SUCCESS, status);
        mInitialContactGroupCount = contact.groupList.size();

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.ADD_CONTACT_LIST);

        startSubTest(fnName, "Fetching contact after sync");
        Contact syncContact = mTestModule.createDummyContactData();
        status = mDb.fetchContact(contact.localContactID, syncContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Check contact server ID is correct");
        assertEquals(syncContact.contactID, mReplyContact.contactID);

        startSubTest(fnName, "Check detail server IDs are correct");
        final ListIterator<ContactDetail> itReplyDetails = mReplyContact.details.listIterator();
        for (ContactDetail syncDetail : syncContact.details) {
            ContactDetail replyDetail = itReplyDetails.next();
            assertEquals(replyDetail.key, syncDetail.key);
            assertEquals(replyDetail.unique_id, syncDetail.unique_id);
        }

        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        assertEquals(State.IDLE, mState);
        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithDetailChanges() {
        final String fnName = "testRunWithDetailChanges";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database");
        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = TestModule.generateRandomLong();
        contact.userID = generateTestUserID(contact.contactID);
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding new detail to contact");
        ContactDetail newDetail = new ContactDetail();
        newDetail.value = NEW_DETAIL_VALUE;
        newDetail.key = NEW_DETAIL_KEY;
        newDetail.keyType = NEW_DETAIL_TYPE;
        newDetail.localContactID = contact.localContactID;
        status = mDb.addContactDetail(newDetail);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Modifying detail in contact");
        ContactDetail modDetail = contact.details.get(0);
        modDetail.value = MOD_DETAIL_VALUE;
        modDetail.key = MOD_DETAIL_KEY;
        modDetail.keyType = MOD_DETAIL_TYPE;
        status = mDb.modifyContactDetail(modDetail);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.MODIFY_CONTACT_LIST);

        startSubTest(fnName, "Fetching detail after sync");
        Contact syncContact = new Contact();
        status = mDb.fetchContact(contact.localContactID, syncContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Check contact server ID is correct");
        boolean done = false;
        for (ContactDetail detail : syncContact.details) {
            if (detail.localDetailID.equals(newDetail.localDetailID)) {
                assertEquals(detail.unique_id, Long.valueOf(detail.localDetailID + 2));
                done = true;
                break;
            }
        }
        assertTrue(done);
        assertEquals(syncContact.contactID, mReplyContact.contactID);
        assertEquals(syncContact.userID, mReplyContact.userID);
        assertEquals(State.IDLE, mState);

        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithContactDeletion() {
        final String fnName = "testRunWithContactDeletion";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database - no sync");
        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = TestModule.generateRandomLong();
        contact.userID = generateTestUserID(contact.contactID);
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting contact");
        status = mDb.deleteContact(contact.localContactID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.DELETE_CONTACT_LIST);

        assertEquals(State.IDLE, mState);

        startSubTest(fnName, "Checking change list is empty");
        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithContactDetailDeletion() {
        final String fnName = "testRunWithContactDetailDeletion";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database - no sync");
        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = TestModule.generateRandomLong();
        contact.userID = generateTestUserID(contact.contactID);
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding test contact detail to database - no sync");
        ContactDetail detail = new ContactDetail();
        detail.value = NEW_DETAIL_VALUE;
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.CELL;
        detail.localContactID = contact.localContactID;
        List<ContactDetail> contactDetailList = new ArrayList<ContactDetail>();
        contactDetailList.add(detail);
        status = mDb.syncAddContactDetailList(contactDetailList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting contact detail without unique ID");
        status = mDb.deleteContactDetail(contact.details.get(0).localDetailID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting contact detail with unique ID");
        status = mDb.deleteContactDetail(detail.localDetailID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.DELETE_CONTACT_DETAIL_LIST);

        assertEquals(State.IDLE, mState);

        startSubTest(fnName, "Checking change list is empty");
        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithGroupRemoval() {
        final String fnName = "testRunWithGroupRemoval";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database - no sync");
        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = TestModule.generateRandomLong();
        contact.userID = generateTestUserID(contact.contactID);
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        contact.groupList.clear();
        contact.groupList.add(TEST_GROUP_1);
        contact.groupList.add(TEST_GROUP_2);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting group 1");
        status = mDb.deleteContactFromGroup(contact.localContactID, TEST_GROUP_1);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting group 2");
        status = mDb.deleteContactFromGroup(contact.localContactID, TEST_GROUP_2);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(2, State.DELETE_GROUP_LIST);

        assertEquals(State.IDLE, mState);

        startSubTest(fnName, "Checking change list is empty");
        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @LargeTest
    @Suppress
    // Breaks tests
    public void testRunWithManyContacts() {
        final String fnName = "testRunWithManyContacts";
        mBulkContactTest = true;
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding " + NO_OF_CONTACTS_TO_ADD + " test contact to database");
        mInitialContactGroupCount = 0;
        for (int i = 0; i < NO_OF_CONTACTS_TO_ADD; i++) {
            Contact contact = mTestModule.createDummyContactData();
            ServiceStatus status = mDb.addContact(contact);
            assertEquals(ServiceStatus.SUCCESS, status);
            mInitialContactGroupCount += contact.groupList.size();

            startSubTest(fnName, "Adding new detail to contact");
            ContactDetail newDetail = new ContactDetail();
            newDetail.value = NEW_DETAIL_VALUE;
            newDetail.key = NEW_DETAIL_KEY;
            newDetail.keyType = NEW_DETAIL_TYPE;
            newDetail.localContactID = contact.localContactID;
            status = mDb.addContactDetail(newDetail);
            assertEquals(ServiceStatus.SUCCESS, status);

            startSubTest(fnName, "Modifying detail in contact");
            ContactDetail modDetail = contact.details.get(0);
            modDetail.value = MOD_DETAIL_VALUE;
            modDetail.key = MOD_DETAIL_KEY;
            modDetail.keyType = MOD_DETAIL_TYPE;
            status = mDb.modifyContactDetail(modDetail);
            assertEquals(ServiceStatus.SUCCESS, status);
        }
        Log.i(LOG_TAG, "Total number of groups = " + mItemCount + " out of "
                + NO_OF_CONTACTS_TO_ADD + " contacts");

        startSubTest(fnName, "Running processor");
        runProcessor(NO_OF_CONTACTS_TO_ADD, State.ADD_CONTACT_LIST);

        startSubTest(fnName, "Fetching contacts after sync");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertTrue(cursor != null);
        while (cursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(cursor);
            Contact testContact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, testContact);
            assertEquals(ServiceStatus.SUCCESS, status);
            assertEquals(Long.valueOf(summary.localContactID + 1), testContact.contactID);
            if ((testContact.localContactID & 15) != 0) {
                assertEquals(Long.valueOf(summary.localContactID + 2), testContact.userID);
            } else {
                assertEquals(null, testContact.userID);
            }
            for (ContactDetail detail : testContact.details) {
                ContactDetail replyDetail = new ContactDetail();
                generateReplyDetail(replyDetail, detail);
                assertEquals(replyDetail.unique_id, detail.unique_id);
                Long syncServerId = TestModule.fetchSyncServerId(detail.localDetailID, mDb
                        .getReadableDatabase());
                assertEquals(Long.valueOf(-1), syncServerId);
            }
        }
        cursor.close();
        assertEquals(State.IDLE, mState);

        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        mState = State.IDLE;
        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithAddDeleteContactChange() {
        final String fnName = "testRunWithAddDeleteContactChange";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database");
        Contact contact = mTestModule.createDummyContactData();
        ServiceStatus status = mDb.addContact(contact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting contact from database");
        status = mDb.deleteContact(contact.localContactID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(0, State.IDLE);
        assertEquals(State.IDLE, mState);

        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @Suppress
    // Breaks tests
    public void testRunWithAddDeleteContactDetailChange() {
        final String fnName = "testRunWithAddDeleteContactDetailChange";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database");
        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = TestModule.generateRandomLong();
        contact.userID = generateTestUserID(contact.contactID);
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding new detail to contact");
        ContactDetail newDetail = new ContactDetail();
        newDetail.value = NEW_DETAIL_VALUE;
        newDetail.key = NEW_DETAIL_KEY;
        newDetail.keyType = NEW_DETAIL_TYPE;
        newDetail.localContactID = contact.localContactID;
        status = mDb.addContactDetail(newDetail);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting contact detail without unique ID");
        status = mDb.deleteContactDetail(contact.details.get(0).localDetailID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting contact detail with unique ID");
        status = mDb.deleteContactDetail(newDetail.localDetailID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.DELETE_CONTACT_DETAIL_LIST);
        assertEquals(State.IDLE, mState);

        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @Suppress
    // Breaks tests
    public void testRunWithAddDeleteGroupChange() {
        final String fnName = "testRunWithAddDeleteGroupChange";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking change list is empty");
        long noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Adding test contact to database");
        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = TestModule.generateRandomLong();
        contact.userID = generateTestUserID(contact.contactID);
        List<Contact> contactList = new ArrayList<Contact>();
        contact.groupList.clear();
        contactList.add(contact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding group to contact");
        status = mDb.addContactToGroup(contact.localContactID, TEST_GROUP_1);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Removing group from contact");
        status = mDb.deleteContactFromGroup(contact.localContactID, TEST_GROUP_1);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(0, State.IDLE);
        assertEquals(State.IDLE, mState);

        noOfChanges = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase());
        assertEquals(0, noOfChanges);
        noOfChanges = ContactDetailsTable.syncNativeFetchNoOfChanges(mDb.getReadableDatabase());
        assertEquals(0, noOfChanges);

        startSubTest(fnName, "Running processor with no contact changes");
        runProcessor(0, State.IDLE);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }
}
