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

import android.app.Instrumentation;
import android.content.Context;
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
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.DownloadServerContacts;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.tests.engine.EngineTestFramework;
import com.vodafone360.people.tests.engine.IEngineTestFrameworkObserver;

public class DownloadServerContactsTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver {
    private static final String LOG_TAG = "DownloadServerContactsTest";

    private static final long MAX_PROCESSOR_TIME = 30000000;

    private static final int CURRENT_SERVER_VERSION = 50;

    private static final long FIRST_MODIFIED_CONTACT_ID = 293822;

    private static final String MODIFIED_NICKNAME_STRING = "ModNickname";

    private static final String MODIFIED_PHONE_STRING = "441292832827";

    private static final ContactDetail.DetailKeyTypes MODIFIED_PHONE_TYPE = ContactDetail.DetailKeyTypes.MOBILE;

    private static final Integer MODIFIED_PHONE_ORDER = 60;

    private static final String NEW_PHONE_STRING = "10203040498";

    private static final ContactDetail.DetailKeyTypes NEW_PHONE_TYPE = null;

    private static final Integer NEW_PHONE_ORDER = 0;

    private static final String NEW_EMAIL_STRING = "AddEmail";

    private static final ContactDetail.DetailKeyTypes NEW_EMAIL_TYPE = ContactDetail.DetailKeyTypes.WORK;

    private static final Integer NEW_EMAIL_ORDER = 40;

    private static final String DEL_EMAIL_STRING2 = "1239383333";

    private static final Long NEW_EMAIL_DETAIL_ID = 202033L;

    private static final Long NEW_PHONE_DETAIL_ID = 301020L;

    private static final Long OLD_PHONE_DETAIL_ID = 502292L;

    private static final Long ALT_PHONE_DETAIL_ID = 602292L;

    private static final String OLD_PHONE_DETAIL_VALUE = "OldPhoneValue";

    private static final long WAIT_FOR_PAGE_MS = 100;

    private static final long MAX_WAIT_FOR_PAGE_MS = 10000L;

    private static final int BULK_TEST_NO_CONTACTS = 100;

    enum State {
        IDLE,
        RUN_WITH_NO_CHANGES,
        RUN_WITH_NEW_CONTACTS,
        RUN_WITH_DELETED_CONTACTS,
        RUN_WITH_MODIFIED_CONTACTS,
        RUN_WITH_DELETED_DETAILS
    }

    EngineTestFramework mEngineTester = null;

    MainApplication mApplication = null;

    DummyContactSyncEngine mEng = null;

    Context mContext;

    class DownloadServerContactProcessorTest extends DownloadServerContacts {
        DownloadServerContactProcessorTest(IContactSyncCallback callback, DatabaseHelper db) {
            super(callback, db);
        }

        public Integer testGetPageFromReqId(int reqId) {
            switch (mInternalState) {
                case FETCHING_NEXT_BATCH:
                    long endTime = System.nanoTime() + (MAX_WAIT_FOR_PAGE_MS * 1000000);
                    while (mPageReqIds.get(reqId) == null && System.nanoTime() < endTime) {
                        try {
                            synchronized (this) {
                                wait(WAIT_FOR_PAGE_MS);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return mPageReqIds.get(reqId);
                case FETCHING_FIRST_PAGE:
                    return 0;
                default:
                    return null;
            }
        }

        public int getDownloadPageSize() {
            return MAX_DOWN_PAGE_SIZE;
        }
    }

    DownloadServerContactProcessorTest mProcessor;

    DatabaseHelper mDb = null;

    State mState = State.IDLE;

    int mInitialCount;

    int mItemCount;

    int mPageCount;

    Contact mLastNewContact;

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
        mProcessor = new DownloadServerContactProcessorTest(mEng, mApplication.getDatabase());
        mEng.setProcessor(mProcessor);
        mEngineTester.setEngine(mEng);
        mState = State.IDLE;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
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
        mPageCount = 0;
        nextState(state);
        mLastNewContact = null;
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
            switch (mState) {
                case RUN_WITH_NO_CHANGES:
                    reportBackWithNoChanges(reqId, data);
                    break;
                case RUN_WITH_NEW_CONTACTS:
                    reportBackWithNewContacts(reqId, data);
                    break;
                case RUN_WITH_DELETED_CONTACTS:
                    reportBackWithDeletedContacts(reqId, data);
                    break;
                case RUN_WITH_MODIFIED_CONTACTS:
                    reportBackWithModifiedContacts(reqId, data);
                    break;
                case RUN_WITH_DELETED_DETAILS:
                    reportBackWithDeletedDetails(reqId, data);
                    break;
                default:
                    fail("Unexpected request rom processor");
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

    private void reportBackWithNoChanges(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackWithNoChanges");
        Integer pageNo = mProcessor.testGetPageFromReqId(reqId);
        assertTrue(pageNo != null);
        assertEquals(Integer.valueOf(0), pageNo);

        ContactChanges contactChanges = new ContactChanges();
        data.add(contactChanges);

        contactChanges.mCurrentServerVersion = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionBefore = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionAfter = CURRENT_SERVER_VERSION;
        contactChanges.mVersionAnchor = CURRENT_SERVER_VERSION;
        contactChanges.mNumberOfPages = 1;

        mItemCount--;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportBackWithNewContacts(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackWithNewContacts");
        Integer pageNo = mProcessor.testGetPageFromReqId(reqId);
        int pageSize = mProcessor.getDownloadPageSize();

        assertTrue(pageNo != null);
        assertEquals(Integer.valueOf(mPageCount), pageNo);

        ContactChanges contactChanges = new ContactChanges();
        data.add(contactChanges);
        contactChanges.mCurrentServerVersion = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionBefore = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionAfter = CURRENT_SERVER_VERSION;
        contactChanges.mVersionAnchor = CURRENT_SERVER_VERSION;
        assertTrue(pageSize > 0);
        if (pageSize > 0) {
            contactChanges.mNumberOfPages = 1 + mPageCount + (mItemCount / pageSize);
        }

        int noOfContacts = Math.min(pageSize, mItemCount);
        for (int i = 0; i < noOfContacts; i++) {
            Contact newContact = mTestModule.createDummyContactData();
            if (mLastNewContact == null) {
                mLastNewContact = newContact;
            }
            newContact.contactID = FIRST_MODIFIED_CONTACT_ID + mItemCount - 1;
            newContact.userID = generateTestUserID(newContact.contactID);
            ContactDetail detail1 = new ContactDetail();
            detail1.key = ContactDetail.DetailKeys.VCARD_PHONE;
            detail1.unique_id = OLD_PHONE_DETAIL_ID + mItemCount - 1;
            detail1.value = OLD_PHONE_DETAIL_VALUE;
            newContact.details.add(detail1);
            for (int j = 0; j < newContact.details.size(); j++) {
                ContactDetail detail = newContact.details.get(j);
                switch (detail.key) {
                    case VCARD_PHONE:
                    case VCARD_EMAIL:
                        if (detail.unique_id == null) {
                            detail.unique_id = ALT_PHONE_DETAIL_ID + j;
                        }
                        break;
                }
            }
            contactChanges.mContacts.add(newContact);
            mItemCount--;
        }

        mPageCount++;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportBackWithDeletedContacts(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackWithDeletedContacts");
        Integer pageNo = mProcessor.testGetPageFromReqId(reqId);
        int pageSize = mProcessor.getDownloadPageSize();

        assertTrue(pageNo != null);
        assertEquals(Integer.valueOf(mPageCount), pageNo);

        ContactChanges contactChanges = new ContactChanges();
        data.add(contactChanges);
        contactChanges.mCurrentServerVersion = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionBefore = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionAfter = CURRENT_SERVER_VERSION;
        contactChanges.mVersionAnchor = CURRENT_SERVER_VERSION;
        assertTrue(pageSize > 0);
        if (pageSize > 0) {
            contactChanges.mNumberOfPages = 1 + mPageCount + (mItemCount / pageSize);
        }

        int noOfContacts = Math.min(pageSize, mItemCount);
        for (int i = 0; i < noOfContacts; i++) {
            Contact deletedContact = new Contact();
            deletedContact.contactID = FIRST_MODIFIED_CONTACT_ID + mItemCount - 1;
            deletedContact.deleted = true;
            contactChanges.mContacts.add(deletedContact);
            mItemCount--;
        }

        mPageCount++;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportBackWithModifiedContacts(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackWithModifiedContacts");
        Integer pageNo = mProcessor.testGetPageFromReqId(reqId);
        int pageSize = mProcessor.getDownloadPageSize();

        assertTrue(pageNo != null);
        assertEquals(Integer.valueOf(mPageCount), pageNo);

        ContactChanges contactChanges = new ContactChanges();
        data.add(contactChanges);
        contactChanges.mCurrentServerVersion = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionBefore = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionAfter = CURRENT_SERVER_VERSION;
        contactChanges.mVersionAnchor = CURRENT_SERVER_VERSION;
        assertTrue(pageSize > 0);
        if (pageSize > 0) {
            contactChanges.mNumberOfPages = 1 + mPageCount + (mItemCount / pageSize);
        }

        int noOfContacts = Math.min(pageSize, mItemCount);
        for (int i = 0; i < noOfContacts; i++) {
            Contact modifiedContact = new Contact();
            modifiedContact.contactID = FIRST_MODIFIED_CONTACT_ID + mItemCount - 1;
            // Modified details
            ContactDetail detail1 = new ContactDetail();
            detail1.key = ContactDetail.DetailKeys.VCARD_NICKNAME;
            detail1.value = generateModifiedString(MODIFIED_NICKNAME_STRING, mItemCount - 1);
            modifiedContact.details.add(detail1);
            ContactDetail detail2 = new ContactDetail();
            detail2.key = ContactDetail.DetailKeys.VCARD_PHONE;
            detail2.keyType = MODIFIED_PHONE_TYPE;
            detail2.order = MODIFIED_PHONE_ORDER;
            detail2.value = generateModifiedString(MODIFIED_PHONE_STRING, mItemCount - 1);
            detail2.unique_id = OLD_PHONE_DETAIL_ID + mItemCount - 1;
            modifiedContact.details.add(detail2);
            // New details
            ContactDetail detail3 = new ContactDetail();
            detail3.key = ContactDetail.DetailKeys.VCARD_PHONE;
            detail3.keyType = NEW_PHONE_TYPE;
            detail3.order = NEW_PHONE_ORDER;
            detail3.value = generateModifiedString(NEW_PHONE_STRING, mItemCount - 1);
            detail3.unique_id = NEW_PHONE_DETAIL_ID + mItemCount - 1;
            modifiedContact.details.add(detail3);
            ContactDetail detail4 = new ContactDetail();
            detail4.key = ContactDetail.DetailKeys.VCARD_EMAIL;
            detail4.keyType = NEW_EMAIL_TYPE;
            detail4.order = NEW_EMAIL_ORDER;
            detail4.value = generateModifiedString(NEW_EMAIL_STRING, mItemCount - 1);
            detail4.unique_id = NEW_EMAIL_DETAIL_ID + mItemCount - 1;
            modifiedContact.details.add(detail4);
            Log.d(LOG_TAG, "Contact " + modifiedContact.contactID + " has details "
                    + detail1.unique_id + ", " + detail2.unique_id + ", " + detail3.unique_id
                    + ", " + detail4.unique_id);
            contactChanges.mContacts.add(modifiedContact);
            mItemCount--;
        }

        mPageCount++;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private void reportBackWithDeletedDetails(int reqId, List<BaseDataType> data) {
        Log.d(LOG_TAG, "reportBackWithDeletedDetails");
        Integer pageNo = mProcessor.testGetPageFromReqId(reqId);
        int pageSize = mProcessor.getDownloadPageSize();

        assertTrue(pageNo != null);
        assertEquals(Integer.valueOf(mPageCount), pageNo);

        ContactChanges contactChanges = new ContactChanges();
        data.add(contactChanges);
        contactChanges.mCurrentServerVersion = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionBefore = CURRENT_SERVER_VERSION;
        contactChanges.mServerRevisionAfter = CURRENT_SERVER_VERSION;
        contactChanges.mVersionAnchor = CURRENT_SERVER_VERSION;
        assertTrue(pageSize > 0);
        if (pageSize > 0) {
            contactChanges.mNumberOfPages = 1 + mPageCount + (mItemCount / pageSize);
        }

        int noOfContacts = Math.min(pageSize, mItemCount);
        for (int i = 0; i < noOfContacts; i++) {
            Contact curContact = new Contact();
            curContact.contactID = FIRST_MODIFIED_CONTACT_ID + mItemCount - 1;
            contactChanges.mContacts.add(curContact);
            ContactDetail delDetail1 = new ContactDetail();
            delDetail1.key = ContactDetail.DetailKeys.VCARD_NAME;
            delDetail1.deleted = true;
            ContactDetail delDetail2 = new ContactDetail();
            delDetail2.key = ContactDetail.DetailKeys.VCARD_NICKNAME;
            delDetail2.deleted = true;
            ContactDetail delDetail3 = new ContactDetail();
            delDetail3.key = ContactDetail.DetailKeys.VCARD_EMAIL;
            delDetail3.unique_id = NEW_EMAIL_DETAIL_ID + mItemCount - 1;
            delDetail3.deleted = true;
            curContact.details.add(delDetail1);
            curContact.details.add(delDetail2);
            curContact.details.add(delDetail3);
            mItemCount--;
        }

        mPageCount++;
        assertTrue(mItemCount >= 0);
        if (mItemCount == 0) {
            nextState(State.IDLE);
        }
    }

    private String generateModifiedString(String template, int index) {
        return template + "," + index;
    }

    private Long generateTestUserID(Long contactID) {
        if (contactID == null) {
            return null;
        }
        if ((contactID & 3) == 0) {
            return null;
        }
        return contactID + 5;
    }

    @Override
    public void onEngineException(Exception e) {
        mEng.onProcessorComplete(ServiceStatus.ERROR_UNKNOWN, "", e);
    }

    @SmallTest
    
    // Breaks tests.
    public void testRunWithNoContactChanges() {
        final String fnName = "testRunWithNoContactChanges";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.RUN_WITH_NO_CHANGES);

        assertEquals(State.IDLE, mState);
        cursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests.
    public void testRunWithOneNewContact() {
        final String fnName = "testRunWithOneNewContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.RUN_WITH_NEW_CONTACTS);

        startSubTest(fnName, "Checking database now has one contact");
        cursor.requery();
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());

        ContactSummary summary = ContactSummaryTable.getQueryData(cursor);
        Contact contact = new Contact();
        ServiceStatus status = mDb.fetchContact(summary.localContactID, contact);
        assertEquals(ServiceStatus.SUCCESS, status);

        Long userId = generateTestUserID(contact.contactID);
        assertTrue(contact.contactID != null);
        assertEquals(userId, contact.userID);
        assertTrue(mLastNewContact != null);
        assertTrue(TestModule.doContactsMatch(contact, mLastNewContact));
        assertTrue(TestModule.doContactsFieldsMatch(contact, mLastNewContact));
        assertEquals(State.IDLE, mState);
        cursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests.
    public void testRunWithOneDeletedContact() {
        final String fnName = "testRunWithOneDeletedContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        Contact contact = mTestModule.createDummyContactData();
        contact.contactID = FIRST_MODIFIED_CONTACT_ID;
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.RUN_WITH_DELETED_CONTACTS);

        startSubTest(fnName, "Checking database now has no contacts");
        cursor.requery();
        assertEquals(0, cursor.getCount());
        cursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    
    // Breaks tests.
    public void testRunWithOneModifiedContact() {
        final String fnName = "testRunWithOneModifiedContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        Contact orgContact = mTestModule.createDummyContactData();
        orgContact.contactID = FIRST_MODIFIED_CONTACT_ID;
        for (int j = 0; j < orgContact.details.size(); j++) {
            ContactDetail detail = orgContact.details.get(j);
            switch (detail.key) {
                case VCARD_PHONE:
                case VCARD_EMAIL:
                    detail.unique_id = ALT_PHONE_DETAIL_ID + j;
                    break;
            }
        }
        ContactDetail detail1 = new ContactDetail();
        detail1.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail1.unique_id = OLD_PHONE_DETAIL_ID;
        detail1.value = OLD_PHONE_DETAIL_VALUE;
        orgContact.details.add(detail1);
        List<Contact> contactList = new ArrayList<Contact>();
        int originalNoOfDetails = orgContact.details.size();
        contactList.add(orgContact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.RUN_WITH_MODIFIED_CONTACTS);

        startSubTest(fnName, "Checking database still has one contact");
        cursor.requery();
        assertEquals(1, cursor.getCount());
        cursor.close();

        Contact modContact = new Contact();
        status = mDb.fetchContactByServerId(FIRST_MODIFIED_CONTACT_ID, modContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        assertEquals(originalNoOfDetails + 2, modContact.details.size());

        for (ContactDetail detail : modContact.details) {
            switch (detail.key) {
                case VCARD_NICKNAME:
                    assertEquals(generateModifiedString(MODIFIED_NICKNAME_STRING, 0), detail.value);
                    break;
                case VCARD_PHONE:
                    if (detail.unique_id != null && detail.unique_id.equals(OLD_PHONE_DETAIL_ID)) {
                        assertEquals(generateModifiedString(MODIFIED_PHONE_STRING, 0), detail.value);
                        assertEquals(MODIFIED_PHONE_TYPE, detail.keyType);
                        assertEquals(MODIFIED_PHONE_ORDER, detail.order);
                    } else if (detail.unique_id != null
                            && detail.unique_id.equals(NEW_PHONE_DETAIL_ID)) {
                        assertEquals(generateModifiedString(NEW_PHONE_STRING, 0), detail.value);
                        assertEquals(NEW_PHONE_TYPE, detail.keyType);
                        assertEquals(NEW_PHONE_ORDER, detail.order);
                    }
                    break;
                case VCARD_EMAIL:
                    if (detail.unique_id != null && detail.unique_id.equals(NEW_EMAIL_DETAIL_ID)) {
                        assertEquals(generateModifiedString(NEW_EMAIL_STRING, 0), detail.value);
                        assertEquals(NEW_EMAIL_TYPE, detail.keyType);
                        assertEquals(NEW_EMAIL_ORDER, detail.order);
                        assertEquals(NEW_EMAIL_DETAIL_ID, detail.unique_id);
                        break;
                    }
                    // Fall through
                default:
                    boolean done = false;
                    for (int j = 0; j < orgContact.details.size(); j++) {
                        if (orgContact.details.get(j).localDetailID.equals(detail.localDetailID)) {
                            assertTrue(DatabaseHelper.doDetailsMatch(orgContact.details.get(j),
                                    detail));
                            assertFalse(DatabaseHelper.hasDetailChanged(orgContact.details.get(j),
                                    detail));
                            done = true;
                            break;
                        }
                    }
                    assertTrue(done);
            }
        }

        assertEquals(State.IDLE, mState);

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    
    // Breaks tests.
    public void testRunWithOneDeletedDetail() {
        final String fnName = "testRunWithOneDeletedDetail";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        Contact orgContact = mTestModule.createDummyContactData();
        orgContact.contactID = FIRST_MODIFIED_CONTACT_ID;
        ContactDetail detail1 = new ContactDetail();
        detail1.key = ContactDetail.DetailKeys.VCARD_EMAIL;
        detail1.keyType = NEW_EMAIL_TYPE;
        detail1.value = NEW_EMAIL_STRING;
        detail1.unique_id = NEW_EMAIL_DETAIL_ID;
        orgContact.details.add(detail1);
        ContactDetail detail2 = new ContactDetail();
        detail2.key = ContactDetail.DetailKeys.VCARD_EMAIL;
        detail2.keyType = NEW_EMAIL_TYPE;
        detail2.value = DEL_EMAIL_STRING2;
        detail2.unique_id = NEW_EMAIL_DETAIL_ID + 1;
        orgContact.details.add(detail2);
        int originalNoOfDetails = orgContact.details.size();
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(orgContact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, false);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor(1, State.RUN_WITH_DELETED_DETAILS);

        startSubTest(fnName, "Checking database still has 1 contact");
        cursor.requery();
        assertEquals(1, cursor.getCount());
        cursor.close();

        Contact modContact = new Contact();
        status = mDb.fetchContactByServerId(FIRST_MODIFIED_CONTACT_ID, modContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        assertEquals(originalNoOfDetails - 3, modContact.details.size());

        for (ContactDetail detail : modContact.details) {
            switch (detail.key) {
                case VCARD_NAME:
                case VCARD_NICKNAME:
                    fail("Unexpected detail after deletion: " + detail.key);
                    break;
                case VCARD_EMAIL:
                    if (detail.unique_id != null && detail.unique_id.equals(NEW_EMAIL_DETAIL_ID)) {
                        fail("Unexpected detail after deletion: " + detail.key);
                        break;
                    }
                    // Fall through
                default:
                    boolean done = false;
                    for (int j = 0; j < orgContact.details.size(); j++) {
                        if (orgContact.details.get(j).localDetailID.equals(detail.localDetailID)) {
                            assertTrue(DatabaseHelper.doDetailsMatch(orgContact.details.get(j),
                                    detail));
                            assertFalse(DatabaseHelper.hasDetailChanged(orgContact.details.get(j),
                                    detail));
                            done = true;
                            break;
                        }
                    }
                    assertTrue(done);
            }
        }

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @LargeTest
    @Suppress
    // Breaks tests.
    public void testRunBulkTest() {
        final String fnName = "testRunBulkTest";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        startSubTest(fnName, "Running processor");
        runProcessor(BULK_TEST_NO_CONTACTS, State.RUN_WITH_NEW_CONTACTS);

        startSubTest(fnName, "Checking database now has " + BULK_TEST_NO_CONTACTS + " contacts");
        cursor.requery();
        assertEquals(BULK_TEST_NO_CONTACTS, cursor.getCount());
        int[] detailsCountArray = new int[BULK_TEST_NO_CONTACTS];
        while (cursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(cursor);
            Contact contact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, contact);
            assertEquals(ServiceStatus.SUCCESS, status);

            int idx = (int)(contact.contactID - FIRST_MODIFIED_CONTACT_ID);
            Long userId = generateTestUserID(contact.contactID);
            assertTrue(contact.contactID != null);
            assertEquals(userId, contact.userID);
            detailsCountArray[idx] = contact.details.size();
            Log.d(LOG_TAG, "Org Contact " + contact.contactID + " has " + contact.details.size()
                    + " details");
            for (ContactDetail detail : contact.details) {
                Log.d(LOG_TAG, "	Detail " + detail.key + ", unique_id=" + detail.unique_id
                        + ", value=" + detail.value);
            }
        }

        assertEquals(State.IDLE, mState);

        startSubTest(fnName, "Running processor");
        runProcessor(BULK_TEST_NO_CONTACTS, State.RUN_WITH_MODIFIED_CONTACTS);

        assertEquals(State.IDLE, mState);
        cursor.requery();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(cursor);
            Contact contact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, contact);
            assertEquals(ServiceStatus.SUCCESS, status);
            int idx = (int)(contact.contactID - FIRST_MODIFIED_CONTACT_ID);
            assertEquals(detailsCountArray[idx] + 2, contact.details.size());
            int currentVal = (int)(contact.contactID - FIRST_MODIFIED_CONTACT_ID);
            boolean foundModNickname = false;
            boolean foundModPhone = false;
            boolean foundNewPhone = false;
            boolean foundNewEmail = false;
            for (ContactDetail detail : contact.details) {
                switch (detail.key) {
                    case VCARD_NICKNAME:
                        assertEquals(generateModifiedString(MODIFIED_NICKNAME_STRING, currentVal),
                                detail.value);
                        foundModNickname = true;
                        break;
                    case VCARD_PHONE:
                        if (detail.unique_id != null
                                && detail.unique_id.longValue() == OLD_PHONE_DETAIL_ID + currentVal) {
                            assertEquals(generateModifiedString(MODIFIED_PHONE_STRING, currentVal),
                                    detail.value);
                            assertEquals(MODIFIED_PHONE_TYPE, detail.keyType);
                            assertEquals(MODIFIED_PHONE_ORDER, detail.order);
                            foundModPhone = true;
                        } else if (detail.unique_id != null
                                && detail.unique_id.longValue() == NEW_PHONE_DETAIL_ID + currentVal) {
                            assertEquals(generateModifiedString(NEW_PHONE_STRING, currentVal),
                                    detail.value);
                            assertEquals(NEW_PHONE_TYPE, detail.keyType);
                            assertEquals(NEW_PHONE_ORDER, detail.order);
                            assertEquals(Long.valueOf(NEW_PHONE_DETAIL_ID + currentVal),
                                    detail.unique_id);
                            foundNewPhone = true;
                        }
                        break;
                    case VCARD_EMAIL:
                        if (detail.unique_id != null
                                && detail.unique_id.longValue() == NEW_EMAIL_DETAIL_ID + currentVal) {
                            assertEquals(generateModifiedString(NEW_EMAIL_STRING, currentVal),
                                    detail.value);
                            assertEquals(NEW_EMAIL_TYPE, detail.keyType);
                            assertEquals(NEW_EMAIL_ORDER, detail.order);
                            foundNewEmail = true;
                        }
                        break;
                }
            }
            assertTrue(foundModNickname);
            assertTrue(foundModPhone);
            assertTrue(foundNewPhone);
            assertTrue(foundNewEmail);
        }

        startSubTest(fnName, "Running processor");
        runProcessor(BULK_TEST_NO_CONTACTS, State.RUN_WITH_DELETED_DETAILS);

        assertEquals(State.IDLE, mState);
        cursor.requery();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(cursor);
            Contact contact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, contact);
            assertEquals(ServiceStatus.SUCCESS, status);
            int idx = (int)(contact.contactID - FIRST_MODIFIED_CONTACT_ID);
            Log.d(LOG_TAG, "Del Contact " + contact.contactID + " has " + contact.details.size()
                    + " details");
            for (ContactDetail detail : contact.details) {
                Log.d(LOG_TAG, "	Detail " + detail.key + ", unique_id=" + detail.unique_id
                        + ", value=" + detail.value);
            }
            assertEquals(detailsCountArray[idx] - 1, contact.details.size());
        }

        startSubTest(fnName, "Running processor");
        runProcessor(BULK_TEST_NO_CONTACTS, State.RUN_WITH_DELETED_CONTACTS);

        assertEquals(State.IDLE, mState);
        cursor.requery();
        assertEquals(0, cursor.getCount());
        cursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }
}
