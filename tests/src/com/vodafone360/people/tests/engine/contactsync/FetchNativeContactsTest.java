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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ContactDetailsTable.Field;
import com.vodafone360.people.database.tables.ContactsTable.ContactIdInfo;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.tests.TestModule.NativeContactDetails;
import com.vodafone360.people.tests.TestModule.NativeDetail;
import com.vodafone360.people.tests.engine.EngineTestFramework;
import com.vodafone360.people.tests.engine.IEngineTestFrameworkObserver;
import com.vodafone360.people.utils.LogUtils;

@SuppressWarnings("deprecation")
public class FetchNativeContactsTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver {
    private static final String LOG_TAG = "FetchNativeContactsTest";

    private static final long MAX_PROCESSOR_TIME = 30000000;

    private static final String ADD_CONTACT_TEST_NAME = "Joe Stephen Bloggs";

    private static final String ADD_CONTACT_TEST_NOTE = "This is a test note for the first test contact";

    private static final String ADD_PHONE_TEST1 = "+441928237372";

    private static final int ADD_PHONE_TYPE_TEST1 = Contacts.Phones.TYPE_HOME;

    private static final ContactDetail.DetailKeyTypes ADD_PHONE_PEOPLE_TYPE_TEST1 = ContactDetail.DetailKeyTypes.HOME;

    private static final String ADD_PHONE_TEST2 = "019283373663";

    private static final int ADD_PHONE_TYPE_TEST2 = Contacts.Phones.TYPE_OTHER;

    private static final ContactDetail.DetailKeyTypes ADD_PHONE_PEOPLE_TYPE_TEST2 = null;

    private static final String ADD_PHONE_TEST3 = "0122-202928-2283";

    private static final int ADD_PHONE_TYPE_TEST3 = Contacts.Phones.TYPE_WORK;

    private static final ContactDetail.DetailKeyTypes ADD_PHONE_PEOPLE_TYPE_TEST3 = ContactDetail.DetailKeyTypes.WORK;

    private static final String ADD_CM_TEST1 = "kdfkddfk@skdjkddd.co.uk";

    private static final int ADD_CM_TYPE_TEST1 = Contacts.ContactMethods.TYPE_HOME;

    private static final int ADD_CM_KIND_TEST1 = 1;

    private static final ContactDetail.DetailKeyTypes ADD_CM_PEOPLE_TYPE_TEST1 = ContactDetail.DetailKeyTypes.HOME;

    private static final String ADD_CM_TEST2 = "18 Styal Road, Wilmslow, Manchester, M22 2AL, United Kingdom";

    private static final int ADD_CM_TYPE_TEST2 = Contacts.ContactMethods.TYPE_WORK;

    private static final int ADD_CM_KIND_TEST2 = 2;

    private static final String ADD_CM_TEST2_ADDRESS1 = "18 Styal Road";

    private static final String ADD_CM_TEST2_ADDRESS2 = "Wilmslow";

    private static final String ADD_CM_TEST2_ADDRESS_CITY = "Manchester";

    private static final String ADD_CM_TEST2_ADDRESS_COUNTY = "";

    private static final String ADD_CM_TEST2_ADDRESS_POSTCODE = "M22 2AL";

    private static final String ADD_CM_TEST2_ADDRESS_COUNTRY = "United Kingdom";

    private static final ContactDetail.DetailKeyTypes ADD_CM_PEOPLE_TYPE_TEST2 = ContactDetail.DetailKeyTypes.WORK;

    private static final String ADD_ORG_COMPANY_TEST1 = "Mobica Ltd";

    private static final String ADD_ORG_TITLE_TEST1 = "Software Engineer";

    private static final int ADD_ORG_TYPE_TEST1 = Contacts.Organizations.TYPE_CUSTOM;

    private static final String ADD_ORG_LABEL_TEST1 = "Custom";

    private static final ContactDetail.DetailKeyTypes ADD_ORG_PEOPLE_TYPE_TEST1 = null;

    private static final String MODIFY_CONTACT_TEST_NAME = "AnotherContactName!\"%^&*()_+";

    private static final String MODIFY_PHONE_TEST1 = "203938937373338383838";

    private static final String MODIFY_EMAIL_TEST1 = "skdfjksdfjkldskl@sdfdsksdklfjdkl.co.uk";

    private static final String MODIFY_COMPANY_TEST1 = "Motorola Ltd";

    private static final String MODIFY_PHONE_TEST2 = "+44192827367363";

    private static final int MODIFY_PHONE_TYPE_TEST2 = Contacts.Phones.TYPE_FAX_HOME;

    private static final ContactDetail.DetailKeyTypes MODIFY_PHONE_PEOPLE_TYPE_TEST2 = ContactDetail.DetailKeyTypes.FAX;

    private static final String MODIFY_EMAIL_TEST2 = "typical_email.address@email.com";

    private static final int MODIFY_EMAIL_TYPE_TEST2 = Contacts.ContactMethods.TYPE_OTHER;

    private static final ContactDetail.DetailKeyTypes MODIFY_EMAIL_PEOPLE_TYPE_TEST2 = null;

    private static final int MODIFY_EMAIL_KIND_TEST2 = 1;

    private static final String MODIFY_COMPANY_TEST2 = "sdkfsdfjklsdfkldfkjl";

    private static final String MODIFY_TITLE_TEST2 = "Database administrator";

    private static final int MODIFY_COMPANY_TYPE_TEST2 = Contacts.Organizations.TYPE_WORK;

    private static final ContactDetail.DetailKeyTypes MODIFY_COMPANY_PEOPLE_TYPE_TEST2 = ContactDetail.DetailKeyTypes.WORK;

    private static final int BULK_TEST_NO_CONTACTS = 100;

    private static final String DUP_CONTACT_NAME = "Abc";

    private static final String DUP_CONTACT_NUMBER = "800123455454";

    EngineTestFramework mEngineTester = null;

    MainApplication mApplication = null;

    DummyContactSyncEngine mEng = null;

    FetchNativeContactsProcessorTest mProcessor = null;

    ContentResolver mCr = null;

    DatabaseHelper mDb = null;

    class FetchNativeContactsProcessorTest extends FetchNativeContactsDummy {
    
        FetchNativeContactsProcessorTest(IContactSyncCallback callback, DatabaseHelper db,
                Context context, ContentResolver cr) {
            super(callback, db, context, cr);
        }
    }

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

        mCr = mApplication.getContentResolver();
        mEngineTester = new EngineTestFramework(this);
        mEng = new DummyContactSyncEngine(mEngineTester);
        mProcessor = new FetchNativeContactsProcessorTest(mEng, mApplication.getDatabase(),
                mApplication, mCr);
        mEng.setProcessor(mProcessor);
        mEngineTester.setEngine(mEng);

        try {
            mCr.delete(People.CONTENT_URI, null, null);
        } catch (IllegalArgumentException e) {
            Cursor c = mCr.query(People.CONTENT_URI, new String[] {
                People._ID
            }, null, null, null);
            while (c.moveToNext()) {
                Uri uri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, c.getInt(0));
                mCr.delete(uri, null, null);
            }
            c.close();
        }
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

    private void runProcessor() {
        mEng.mProcessorCompleteFlag = false;
        mProcessor.start();
        ServiceStatus status = mEng.waitForProcessorComplete(MAX_PROCESSOR_TIME);
        assertEquals(ServiceStatus.SUCCESS, status);
    }

    private void startSubTest(String function, String description) {
        Log.i(LOG_TAG, function + " - step " + mTestStep + ": " + description);
        mTestStep++;
    }

    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        Log.d("TAG", "FetchNativeContactsTest.reportBackToEngine");
    }

    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub
    }

    private Integer fetchSyncNativeId(Long localDetailID) {
        Integer value = null;
        SQLiteDatabase readableDb = mDb.getReadableDatabase();
        try {
            Cursor c = readableDb.rawQuery("SELECT "
                    + ContactDetailsTable.Field.NATIVESYNCCONTACTID + " FROM "
                    + ContactDetailsTable.TABLE_NAME + " WHERE " + Field.DETAILLOCALID + "="
                    + localDetailID, null);
            if (c.moveToFirst()) {
                if (!c.isNull(0)) {
                    value = c.getInt(0);
                }
            }
            c.close();
        } catch (SQLException e) {
        }
        return value;
    }

    @SmallTest
    
    // Breaks tests
    public void testRunWithNoContactChanges() {
        final String fnName = "testRunWithNoContactChanges";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
            People._ID
        }, null, null, null);
        assertEquals(nativeCursor.getCount(), 0);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking native database is still empty");
        nativeCursor.requery();
        assertEquals(nativeCursor.getCount(), 0);

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithOneNewContact() {
        final String fnName = "testRunWithOneNewContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add one dummy native contact");
        ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME, ADD_CONTACT_TEST_NAME);
        peopleValues.put(Contacts.People.NOTES, ADD_CONTACT_TEST_NOTE);
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId = ContentUris.parseId(personUri1);
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST1);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST1);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        final long phoneId1 = (int)ContentUris.parseId(phoneUri1);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST2);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST2);
        phoneValues.put(Contacts.Phones.ISPRIMARY, 1);
        Uri phoneUri2 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 2", phoneUri2 != null);
        final long phoneId2 = ContentUris.parseId(phoneUri2);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST3);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST3);
        phoneValues.remove(Contacts.Phones.ISPRIMARY);
        Uri phoneUri3 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 3", phoneUri3 != null);
        final long phoneId3 = ContentUris.parseId(phoneUri3);
        ContentValues cmValues = new ContentValues();
        cmValues.put(Contacts.ContactMethods.PERSON_ID, personId);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST1);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST1);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST1);
        Uri cmUri1 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 1", cmUri1 != null);
        final long cmId1 = ContentUris.parseId(cmUri1);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST2);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST2);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST2);
        cmValues.put(Contacts.ContactMethods.ISPRIMARY, 1);
        Uri cmUri2 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 2", cmUri2 != null);
        final long cmId2 = ContentUris.parseId(cmUri2);

        ContentValues orgValues = new ContentValues();
        orgValues.put(Contacts.Organizations.PERSON_ID, personId);
        orgValues.put(Contacts.Organizations.COMPANY, ADD_ORG_COMPANY_TEST1);
        orgValues.put(Contacts.Organizations.TITLE, ADD_ORG_TITLE_TEST1);
        orgValues.put(Contacts.Organizations.TYPE, ADD_ORG_TYPE_TEST1);
        orgValues.put(Contacts.Organizations.LABEL, ADD_ORG_LABEL_TEST1);
        Uri orgUri = mCr.insert(Contacts.Organizations.CONTENT_URI, orgValues);
        assertTrue("Unable to insert contact into native organizations table 1", orgUri != null);
        final long orgId1 = ContentUris.parseId(orgUri);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();

        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        Contact newContact = new Contact();
        ServiceStatus status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        boolean doneName = false;
        boolean doneNickname = false;
        boolean doneNote = false;
        boolean donePhone1 = false;
        boolean donePhone2 = false;
        boolean donePhone3 = false;
        boolean doneCm1 = false;
        boolean doneCm2 = false;
        boolean doneOrg1 = false;
        boolean doneTitle1 = false;
        assertTrue(newContact.synctophone);
        assertEquals(personId, newContact.nativeContactId.longValue());
        assertEquals(personId, summary.nativeContactId.longValue());
        for (ContactDetail detail : newContact.details) {
            assertEquals(personId, detail.nativeContactId.longValue());
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            assertTrue(detailId != null);
            switch (detail.key) {
                case VCARD_NAME:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(ADD_CONTACT_TEST_NAME, detail.nativeVal1);
                    VCardHelper.Name name = detail.getName();
                    assertTrue(name != null);
                    assertEquals(ADD_CONTACT_TEST_NAME, name.toString());
                    doneName = true;
                    break;
                case VCARD_NICKNAME:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(ADD_CONTACT_TEST_NAME, detail.nativeVal1);
                    assertEquals(ADD_CONTACT_TEST_NAME, detail.getValue());
                    doneNickname = true;
                    break;
                case VCARD_NOTE:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(ADD_CONTACT_TEST_NOTE, detail.nativeVal1);
                    assertEquals(ADD_CONTACT_TEST_NOTE, detail.getValue());
                    doneNote = true;
                    break;
                case VCARD_PHONE:
                    if (detailId.longValue() == phoneId1) {
                        donePhone1 = true;
                        assertEquals(ADD_PHONE_TEST1, detail.nativeVal1);
                        assertEquals(ADD_PHONE_TEST1, detail.getValue());
                        assertEquals(ADD_PHONE_PEOPLE_TYPE_TEST1, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_NORMAL), detail.order);
                    } else if (detailId.longValue() == phoneId2) {
                        donePhone2 = true;
                        assertEquals(ADD_PHONE_TEST2, detail.nativeVal1);
                        assertEquals(ADD_PHONE_TEST2, detail.getValue());
                        assertEquals(ADD_PHONE_PEOPLE_TYPE_TEST2, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_PREFERRED), detail.order);
                    } else if (detailId.longValue() == phoneId3) {
                        donePhone3 = true;
                        assertEquals(ADD_PHONE_TEST3, detail.nativeVal1);
                        assertEquals(ADD_PHONE_TEST3, detail.getValue());
                        assertEquals(ADD_PHONE_PEOPLE_TYPE_TEST3, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_NORMAL), detail.order);
                    } else {
                        fail("Unknown phone number in people contact: ID:" + detailId
                                + " does not match " + phoneId1 + "," + phoneId2 + "," + phoneId3);
                    }
                    break;
                case VCARD_EMAIL:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == cmId1) {
                        doneCm1 = true;
                        assertEquals(ADD_CM_TEST1, detail.nativeVal1);
                        assertEquals(String.valueOf(ADD_CM_TYPE_TEST1), detail.nativeVal2);
                        assertEquals(String.valueOf(ADD_CM_KIND_TEST1), detail.nativeVal3);
                        assertEquals(ADD_CM_TEST1, detail.getValue());
                        assertEquals(ADD_CM_PEOPLE_TYPE_TEST1, detail.keyType);
                    } else {
                        fail("Unknown email in people contact");
                    }
                    break;
                case VCARD_ADDRESS:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == cmId2) {
                        doneCm2 = true;
                        assertEquals(ADD_CM_TEST2, detail.nativeVal1);
                        assertEquals(String.valueOf(ADD_CM_TYPE_TEST2), detail.nativeVal2);
                        assertEquals(String.valueOf(ADD_CM_KIND_TEST2), detail.nativeVal3);
                        VCardHelper.PostalAddress address = detail.getPostalAddress();
                        assertTrue(address != null);
                        assertEquals(ADD_CM_TEST2_ADDRESS1, address.addressLine1);
                        assertEquals(ADD_CM_TEST2_ADDRESS2, address.addressLine2);
                        assertEquals(ADD_CM_TEST2_ADDRESS_CITY, address.city);
                        assertEquals(ADD_CM_TEST2_ADDRESS_COUNTY, address.county);
                        assertEquals(ADD_CM_TEST2_ADDRESS_POSTCODE, address.postCode);
                        assertEquals(ADD_CM_TEST2_ADDRESS_COUNTRY, address.country);
                        assertEquals(ADD_CM_PEOPLE_TYPE_TEST2, detail.keyType);
                    } else {
                        fail("Unknown address in people contact");
                    }
                    break;
                case VCARD_ORG:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == orgId1) {
                        doneOrg1 = true;
                        assertEquals(ADD_ORG_COMPANY_TEST1, detail.nativeVal1);
                        assertEquals(String.valueOf(ADD_ORG_TYPE_TEST1), detail.nativeVal3);
                        VCardHelper.Organisation org = detail.getOrg();
                        assertTrue(org != null);
                        assertEquals(0, org.unitNames.size());
                        assertEquals(ADD_ORG_COMPANY_TEST1, org.name);
                        assertEquals(ADD_ORG_PEOPLE_TYPE_TEST1, detail.keyType);
                    } else {
                        fail("Unknown organisation in people contact");
                    }
                    break;
                case VCARD_TITLE:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == orgId1) {
                        doneTitle1 = true;
                        assertEquals(ADD_ORG_TITLE_TEST1, detail.nativeVal1);
                        assertEquals(ADD_ORG_TITLE_TEST1, detail.getValue());
                    } else {
                        fail("Unknown title in people contact");
                    }
                    break;
                default:
                    fail("Unexpected detail in people contact: " + detail.key);
            }
        }

        assertTrue("Name was missing", doneName);
        assertTrue("Nickname was missing", doneNickname);
        assertTrue("Note was missing", doneNote);
        assertTrue("Phone1 was missing", donePhone1);
        assertTrue("Phone2 was missing", donePhone2);
        assertTrue("Phone3 was missing", donePhone3);
        assertTrue("Email was missing", doneCm1);
        assertTrue("Address was missing", doneCm2);
        assertTrue("Organisation was missing", doneOrg1);
        assertTrue("Title was missing", doneTitle1);

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithOneNoNameContact() {
        final String fnName = "testRunWithOneNoNameContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add one dummy native contact");
        ContentValues peopleValues = new ContentValues();
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId = ContentUris.parseId(personUri1);

        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST1);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST1);
        phoneValues.put(Contacts.Phones.ISPRIMARY, 1);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        final long phoneId = (int)ContentUris.parseId(phoneUri1);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();

        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        Contact newContact = new Contact();
        ServiceStatus status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertTrue(newContact.synctophone);
        assertEquals(personId, newContact.nativeContactId.longValue());
        assertEquals(personId, summary.nativeContactId.longValue());
        boolean donePhone = false;
        for (ContactDetail detail : newContact.details) {
            assertEquals(personId, detail.nativeContactId.longValue());
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            switch (detail.key) {
                case VCARD_PHONE:
                    assertTrue(detailId != null);
                    assertTrue(detail.nativeDetailId != null);
                    assertEquals(phoneId, detail.nativeDetailId.longValue());
                    assertFalse(donePhone);
                    donePhone = true;
                    assertEquals(ADD_PHONE_TEST1, detail.nativeVal1);
                    assertEquals(ADD_PHONE_TEST1, detail.getValue());
                    assertEquals(Integer.valueOf(ContactDetail.ORDER_PREFERRED), detail.order);
                    break;
                default:
                    fail("Unexpected detail in people contact: " + detail.key);
            }
        }
        assertTrue("Phone number was missing", donePhone);

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests.
    public void testRunWithOneModifiedContact() {
        final String fnName = "testRunWithOneModifiedContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add one dummy native contact");
        ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME, ADD_CONTACT_TEST_NAME);
        peopleValues.put(Contacts.People.NOTES, ADD_CONTACT_TEST_NOTE);
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId = ContentUris.parseId(personUri1);
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST1);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST1);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        final long phoneId1 = (int)ContentUris.parseId(phoneUri1);
        ContentValues cmValues = new ContentValues();
        cmValues.put(Contacts.ContactMethods.PERSON_ID, personId);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST1);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST1);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST1);
        cmValues.put(Contacts.ContactMethods.ISPRIMARY, 1);
        Uri cmUri1 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 1", cmUri1 != null);
        final long cmId1 = ContentUris.parseId(cmUri1);
        ContentValues orgValues = new ContentValues();
        orgValues.put(Contacts.Organizations.PERSON_ID, personId);
        orgValues.put(Contacts.Organizations.COMPANY, ADD_ORG_COMPANY_TEST1);
        orgValues.put(Contacts.Organizations.TITLE, ADD_ORG_TITLE_TEST1);
        orgValues.put(Contacts.Organizations.TYPE, ADD_ORG_TYPE_TEST1);
        orgValues.put(Contacts.Organizations.LABEL, ADD_ORG_LABEL_TEST1);
        Uri orgUri = mCr.insert(Contacts.Organizations.CONTENT_URI, orgValues);
        assertTrue("Unable to insert contact into native organizations table 1", orgUri != null);
        final long orgId1 = ContentUris.parseId(orgUri);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        startSubTest(fnName, "Modifying name");
        peopleValues.clear();
        peopleValues.put(Contacts.People.NAME, MODIFY_CONTACT_TEST_NAME);
        int rowsUpdated = mCr.update(personUri1, peopleValues, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Modifying phone number");
        phoneValues.clear();
        phoneValues.put(Contacts.Phones.NUMBER, MODIFY_PHONE_TEST1);
        rowsUpdated = mCr.update(phoneUri1, phoneValues, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Modifying email address");
        cmValues.clear();
        cmValues.put(Contacts.ContactMethods.DATA, MODIFY_EMAIL_TEST1);
        rowsUpdated = mCr.update(cmUri1, cmValues, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Modifying organization");
        orgValues.clear();
        orgValues.put(Contacts.Organizations.COMPANY, MODIFY_COMPANY_TEST1);
        rowsUpdated = mCr.update(orgUri, orgValues, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Adding phone number");
        phoneValues.clear();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, MODIFY_PHONE_TEST2);
        phoneValues.put(Contacts.Phones.TYPE, MODIFY_PHONE_TYPE_TEST2);
        phoneValues.put(Contacts.Phones.ISPRIMARY, 1);
        Uri phoneUri2 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 2", phoneUri2 != null);
        final long phoneId2 = (int)ContentUris.parseId(phoneUri2);

        startSubTest(fnName, "Adding email address");
        cmValues.clear();
        cmValues.put(Contacts.ContactMethods.PERSON_ID, personId);
        cmValues.put(Contacts.ContactMethods.DATA, MODIFY_EMAIL_TEST2);
        cmValues.put(Contacts.ContactMethods.TYPE, MODIFY_EMAIL_TYPE_TEST2);
        cmValues.put(Contacts.ContactMethods.KIND, MODIFY_EMAIL_KIND_TEST2);
        Uri cmUri2 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 1", cmUri2 != null);
        final long cmId2 = (int)ContentUris.parseId(cmUri2);

        startSubTest(fnName, "Adding organization");
        orgValues.clear();
        orgValues.put(Contacts.Organizations.PERSON_ID, personId);
        orgValues.put(Contacts.Organizations.COMPANY, MODIFY_COMPANY_TEST2);
        orgValues.put(Contacts.Organizations.TITLE, MODIFY_TITLE_TEST2);
        orgValues.put(Contacts.Organizations.TYPE, MODIFY_COMPANY_TYPE_TEST2);
        orgValues.put(Contacts.Organizations.ISPRIMARY, 1);
        Uri orgUri2 = mCr.insert(Contacts.Organizations.CONTENT_URI, orgValues);
        assertTrue("Unable to insert contact into native contact methods table 1", orgUri2 != null);
        final long orgId2 = (int)ContentUris.parseId(orgUri2);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking still 1 contact");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        Contact newContact = new Contact();
        ServiceStatus status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        boolean doneName = false;
        boolean doneNickname = false;
        boolean doneNote = false;
        boolean donePhone1 = false;
        boolean donePhone2 = false;
        boolean doneCm1 = false;
        boolean doneCm2 = false;
        boolean doneOrg1 = false;
        boolean doneOrg2 = false;
        boolean doneTitle1 = false;
        boolean doneTitle2 = false;
        assertTrue(newContact.synctophone);
        assertEquals(personId, newContact.nativeContactId.longValue());
        assertEquals(personId, summary.nativeContactId.longValue());
        for (ContactDetail detail : newContact.details) {
            assertEquals(Integer.valueOf((int)personId), detail.nativeContactId);
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            assertTrue(detailId != null);
            switch (detail.key) {
                case VCARD_NAME:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(MODIFY_CONTACT_TEST_NAME, detail.nativeVal1);
                    VCardHelper.Name name = detail.getName();
                    assertTrue(name != null);
                    assertEquals(MODIFY_CONTACT_TEST_NAME, name.toString());
                    doneName = true;
                    break;
                case VCARD_NICKNAME:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(MODIFY_CONTACT_TEST_NAME, detail.nativeVal1);
                    assertEquals(MODIFY_CONTACT_TEST_NAME, detail.getValue());
                    doneNickname = true;
                    break;
                case VCARD_NOTE:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(ADD_CONTACT_TEST_NOTE, detail.nativeVal1);
                    assertEquals(ADD_CONTACT_TEST_NOTE, detail.getValue());
                    doneNote = true;
                    break;
                case VCARD_PHONE:
                    if (detailId.longValue() == phoneId1) {
                        donePhone1 = true;
                        assertEquals(MODIFY_PHONE_TEST1, detail.nativeVal1);
                        assertEquals(MODIFY_PHONE_TEST1, detail.getValue());
                        assertEquals(ADD_PHONE_PEOPLE_TYPE_TEST1, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_NORMAL), detail.order);
                    } else if (detailId.longValue() == phoneId2) {
                        donePhone2 = true;
                        assertEquals(MODIFY_PHONE_TEST2, detail.nativeVal1);
                        assertEquals(MODIFY_PHONE_TEST2, detail.getValue());
                        assertEquals(MODIFY_PHONE_PEOPLE_TYPE_TEST2, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_PREFERRED), detail.order);
                    } else {
                        fail("Unknown phone number in people contact: ID:" + detailId
                                + " does not match " + phoneId1 + "," + phoneId2);
                    }
                    break;
                case VCARD_EMAIL:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == cmId1) {
                        doneCm1 = true;
                        assertEquals(MODIFY_EMAIL_TEST1, detail.nativeVal1);
                        assertEquals(MODIFY_EMAIL_TEST1, detail.getValue());
                        assertEquals(String.valueOf(ADD_CM_TYPE_TEST1), detail.nativeVal2);
                        assertEquals(String.valueOf(ADD_CM_KIND_TEST1), detail.nativeVal3);
                        assertEquals(ADD_CM_PEOPLE_TYPE_TEST1, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_PREFERRED), detail.order);
                    } else if (detailId.longValue() == cmId2) {
                        doneCm2 = true;
                        assertEquals(MODIFY_EMAIL_TEST2, detail.nativeVal1);
                        assertEquals(MODIFY_EMAIL_TEST2, detail.getValue());
                        assertEquals(String.valueOf(MODIFY_EMAIL_KIND_TEST2), detail.nativeVal2);
                        assertEquals(String.valueOf(MODIFY_EMAIL_TYPE_TEST2), detail.nativeVal3);
                        assertEquals(MODIFY_EMAIL_PEOPLE_TYPE_TEST2, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_NORMAL), detail.order);
                    } else {
                        fail("Unknown email in people contact: ID:" + detailId + " does not match "
                                + phoneId1 + "," + phoneId2);
                    }
                    break;
                case VCARD_ORG:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == orgId1) {
                        doneOrg1 = true;
                        assertEquals(MODIFY_COMPANY_TEST1, detail.nativeVal1);
                        assertEquals(String.valueOf(ADD_ORG_TYPE_TEST1), detail.nativeVal3);
                        VCardHelper.Organisation org = detail.getOrg();
                        assertTrue(org != null);
                        assertEquals(0, org.unitNames.size());
                        assertEquals(MODIFY_COMPANY_TEST1, org.name);
                        assertEquals(ADD_ORG_PEOPLE_TYPE_TEST1, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_NORMAL), detail.order);
                    } else if (detailId.longValue() == orgId2) {
                        doneOrg2 = true;
                        assertEquals(MODIFY_COMPANY_TEST2, detail.nativeVal1);
                        assertEquals(String.valueOf(MODIFY_COMPANY_TYPE_TEST2), detail.nativeVal3);
                        VCardHelper.Organisation org = detail.getOrg();
                        assertTrue(org != null);
                        assertEquals(0, org.unitNames.size());
                        assertEquals(MODIFY_COMPANY_TEST2, org.name);
                        assertEquals(MODIFY_COMPANY_PEOPLE_TYPE_TEST2, detail.keyType);
                        assertEquals(Integer.valueOf(ContactDetail.ORDER_PREFERRED), detail.order);
                    } else {
                        fail("Unknown organisation in people contact");
                    }
                    break;
                case VCARD_TITLE:
                    assertTrue(detailId != null);
                    if (detailId.longValue() == orgId1) {
                        doneTitle1 = true;
                        assertEquals(ADD_ORG_TITLE_TEST1, detail.nativeVal1);
                        assertEquals(ADD_ORG_TITLE_TEST1, detail.getValue());
                    } else if (detailId.longValue() == orgId2) {
                        doneTitle2 = true;
                        assertEquals(MODIFY_TITLE_TEST2, detail.nativeVal1);
                        assertEquals(MODIFY_TITLE_TEST2, detail.getValue());
                    } else {
                        fail("Unknown title in people contact");
                    }
                    break;
                default:
                    fail("Unexpected detail in people contact: " + detail.key);
            }
        }

        assertTrue("Name was missing", doneName);
        assertTrue("Nickname was missing", doneNickname);
        assertTrue("Note was missing", doneNote);
        assertTrue("Phone1 was missing", donePhone1);
        assertTrue("Phone2 was missing", donePhone2);
        assertTrue("Email1 was missing", doneCm1);
        assertTrue("Email2 was missing", doneCm2);
        assertTrue("Organisation1 was missing", doneOrg1);
        assertTrue("Organisation2 was missing", doneOrg2);
        assertTrue("Title1 was missing", doneTitle1);
        assertTrue("Title2 was missing", doneTitle2);

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithDeletedDetails() {
        final String fnName = "testRunWithDeletedDetails";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add one dummy native contact");
        ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME, ADD_CONTACT_TEST_NAME);
        peopleValues.put(Contacts.People.NOTES, ADD_CONTACT_TEST_NOTE);
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId = ContentUris.parseId(personUri1);
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST1);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST1);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        final long phoneId1 = (int)ContentUris.parseId(phoneUri1);
        ContentValues cmValues = new ContentValues();
        cmValues.put(Contacts.ContactMethods.PERSON_ID, personId);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST1);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST1);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST1);
        Uri cmUri1 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 1", cmUri1 != null);
        final long cmId1 = ContentUris.parseId(cmUri1);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST2);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST2);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST2);
        cmValues.put(Contacts.ContactMethods.ISPRIMARY, 1);
        Uri cmUri2 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 2", cmUri2 != null);
        final long cmId2 = ContentUris.parseId(cmUri2);

        ContentValues orgValues = new ContentValues();
        orgValues.put(Contacts.Organizations.PERSON_ID, personId);
        orgValues.put(Contacts.Organizations.COMPANY, ADD_ORG_COMPANY_TEST1);
        orgValues.put(Contacts.Organizations.TITLE, ADD_ORG_TITLE_TEST1);
        orgValues.put(Contacts.Organizations.TYPE, ADD_ORG_TYPE_TEST1);
        orgValues.put(Contacts.Organizations.LABEL, ADD_ORG_LABEL_TEST1);
        Uri orgUri = mCr.insert(Contacts.Organizations.CONTENT_URI, orgValues);
        assertTrue("Unable to insert contact into native organizations table 1", orgUri != null);
        final long orgId1 = ContentUris.parseId(orgUri);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        Contact newContact = new Contact();
        ServiceStatus status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        boolean doneNote = false;
        boolean donePhone1 = false;
        boolean doneCm1 = false;
        boolean doneCm2 = false;
        boolean doneOrg1 = false;
        boolean doneTitle1 = false;
        assertTrue(newContact.synctophone);
        assertEquals(personId, newContact.nativeContactId.longValue());
        assertEquals(personId, summary.nativeContactId.longValue());
        for (ContactDetail detail : newContact.details) {
            assertEquals(personId, detail.nativeContactId.longValue());
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            assertTrue(detailId != null);
            switch (detail.key) {
                case VCARD_NAME:
                case VCARD_NICKNAME:
                    assertEquals(personId, detailId.longValue());
                    break;
                case VCARD_NOTE:
                    assertEquals(personId, detailId.longValue());
                    doneNote = true;
                    mDb.deleteContactDetail(detail.localDetailID);
                    break;
                case VCARD_PHONE:
                    if (detailId.longValue() == phoneId1) {
                        donePhone1 = true;
                        mDb.deleteContactDetail(detail.localDetailID);
                    } else {
                        fail("Unknown phone number in people contact: ID:" + detailId
                                + " does not match " + phoneId1);
                    }
                    break;
                case VCARD_EMAIL:
                    if (detailId.longValue() == cmId1) {
                        doneCm1 = true;
                        mDb.deleteContactDetail(detail.localDetailID);
                    } else {
                        fail("Unknown email in people contact");
                    }
                    break;
                case VCARD_ADDRESS:
                    if (detailId.longValue() == cmId2) {
                        doneCm2 = true;
                        mDb.deleteContactDetail(detail.localDetailID);
                    } else {
                        fail("Unknown address in people contact");
                    }
                    break;
                case VCARD_ORG:
                    if (detailId.longValue() == orgId1) {
                        doneOrg1 = true;
                        mDb.deleteContactDetail(detail.localDetailID);
                    } else {
                        fail("Unknown organisation in people contact");
                    }
                    break;
                case VCARD_TITLE:
                    if (detailId.longValue() == orgId1) {
                        doneTitle1 = true;
                        mDb.deleteContactDetail(detail.localDetailID);
                    } else {
                        fail("Unknown title in people contact");
                    }
                    break;
                default:
                    fail("Unexpected detail in people contact: " + detail.key);
            }
        }

        assertTrue("Note was missing", doneNote);
        assertTrue("Phone1 was missing", donePhone1);
        assertTrue("Email was missing", doneCm1);
        assertTrue("Address was missing", doneCm2);
        assertTrue("Organisation was missing", doneOrg1);
        assertTrue("Position was missing", doneTitle1);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        newContact = new Contact();
        status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertTrue(newContact.synctophone);
        assertEquals(personId, newContact.nativeContactId.longValue());
        assertEquals(personId, summary.nativeContactId.longValue());

        boolean doneName = false;
        boolean doneNickname = false;

        for (ContactDetail detail : newContact.details) {
            assertEquals(personId, detail.nativeContactId.longValue());
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            assertTrue(detailId != null);
            switch (detail.key) {
                case VCARD_NAME:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(ADD_CONTACT_TEST_NAME, detail.nativeVal1);
                    VCardHelper.Name name = detail.getName();
                    assertTrue(name != null);
                    assertEquals(ADD_CONTACT_TEST_NAME, name.toString());
                    doneName = true;
                    break;
                case VCARD_NICKNAME:
                    assertEquals(personId, detailId.longValue());
                    assertEquals(ADD_CONTACT_TEST_NAME, detail.nativeVal1);
                    assertEquals(ADD_CONTACT_TEST_NAME, detail.getValue());
                    doneNickname = true;
                    break;
                default:
                    fail("Unexpected contact detail: " + detail.key);
            }
        }

        assertTrue("Name was missing", doneName);
        assertTrue("Nickname was missing", doneNickname);

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests
    public void testRunWithDeletedContact() {
        final String fnName = "testRunWithDeletedContact";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add one dummy native contact");
        ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME, ADD_CONTACT_TEST_NAME);
        peopleValues.put(Contacts.People.NOTES, ADD_CONTACT_TEST_NOTE);
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId = ContentUris.parseId(personUri1);
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST1);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST1);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        ContentValues cmValues = new ContentValues();
        cmValues.put(Contacts.ContactMethods.PERSON_ID, personId);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST1);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST1);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST1);
        Uri cmUri1 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 1", cmUri1 != null);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST2);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST2);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST2);
        cmValues.put(Contacts.ContactMethods.ISPRIMARY, 1);
        Uri cmUri2 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 2", cmUri2 != null);
        ContentValues orgValues = new ContentValues();
        orgValues.put(Contacts.Organizations.PERSON_ID, personId);
        orgValues.put(Contacts.Organizations.COMPANY, ADD_ORG_COMPANY_TEST1);
        orgValues.put(Contacts.Organizations.TITLE, ADD_ORG_TITLE_TEST1);
        orgValues.put(Contacts.Organizations.TYPE, ADD_ORG_TYPE_TEST1);
        orgValues.put(Contacts.Organizations.LABEL, ADD_ORG_LABEL_TEST1);
        Uri orgUri = mCr.insert(Contacts.Organizations.CONTENT_URI, orgValues);
        assertTrue("Unable to insert contact into native organizations table 1", orgUri != null);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        int rows = mCr.delete(personUri1, null, null);
        assertEquals(1, rows);

        startSubTest(fnName, "Running processor again");
        runProcessor();

        startSubTest(fnName, "Checking contact has been deleted");
        peopleCursor.requery();
        assertEquals(0, peopleCursor.getCount());

        SQLiteDatabase sqlDb = mDb.getReadableDatabase();
        Cursor contactsCountCursor = sqlDb.rawQuery("SELECT COUNT(*) FROM "
                + ContactsTable.TABLE_NAME, null);
        assertTrue(contactsCountCursor.moveToFirst());
        assertEquals(0, contactsCountCursor.getInt(0));
        contactsCountCursor.close();
        Cursor detailCountCursor = sqlDb.rawQuery("SELECT COUNT(*) FROM "
                + ContactDetailsTable.TABLE_NAME, null);
        assertTrue(detailCountCursor.moveToFirst());
        assertEquals(0, detailCountCursor.getInt(0));
        detailCountCursor.close();
        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    
    // Breaks tests.
    @Suppress
    public void testRunConflictTests() {
        final String fnName = "testRunConflictTests";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add one dummy native contact");
        ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME, ADD_CONTACT_TEST_NAME);
        peopleValues.put(Contacts.People.NOTES, ADD_CONTACT_TEST_NOTE);
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId = ContentUris.parseId(personUri1);
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST1);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST1);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        final long phoneId1 = (int)ContentUris.parseId(phoneUri1);
        phoneValues.put(Contacts.Phones.NUMBER, ADD_PHONE_TEST2);
        phoneValues.put(Contacts.Phones.TYPE, ADD_PHONE_TYPE_TEST2);
        phoneValues.put(Contacts.Phones.ISPRIMARY, 1);
        Uri phoneUri2 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 2", phoneUri2 != null);
        final long phoneId2 = ContentUris.parseId(phoneUri2);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has been synced to people");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        Contact orgContact = new Contact();
        ServiceStatus status = mDb.fetchContact(summary.localContactID, orgContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Modifying phone detail in people and native");
        phoneValues.clear();
        phoneValues.put(Contacts.Phones.NUMBER, MODIFY_PHONE_TEST1);
        int rowsUpdated = mCr.update(phoneUri1, phoneValues, null, null);
        assertEquals(1, rowsUpdated);

        boolean donePhone1 = false;
        for (ContactDetail detail : orgContact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_PHONE
                    && detail.nativeDetailId.longValue() == phoneId1) {
                detail.setTel(MODIFY_PHONE_TEST2, MODIFY_PHONE_PEOPLE_TYPE_TEST2);
                status = mDb.modifyContactDetail(detail);
                assertEquals(ServiceStatus.SUCCESS, status);
                donePhone1 = true;
                break;
            }
        }

        assertTrue("Phone was not synced", donePhone1);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has not been changed");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        Contact newContact = new Contact();
        status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        donePhone1 = false;
        boolean donePhone2 = false;
        for (ContactDetail detail : newContact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_PHONE) {
                if (detail.nativeDetailId.longValue() == phoneId1) {
                    assertEquals(MODIFY_PHONE_TEST2, detail.getTel());
                    assertEquals(MODIFY_PHONE_PEOPLE_TYPE_TEST2, detail.keyType);
                    donePhone1 = true;
                } else {
                    mDb.deleteContactDetail(detail.localDetailID);
                    donePhone2 = true;
                }
            }
        }

        assertTrue("Phone detail 1 is now missing", donePhone1);
        assertTrue("Phone detail 2 is now missing", donePhone2);

        startSubTest(fnName, "Modifying phone detail in native when it has been deleted in people");
        phoneValues.clear();
        phoneValues.put(Contacts.Phones.NUMBER, MODIFY_PHONE_TEST1);
        rowsUpdated = mCr.update(phoneUri2, phoneValues, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking contact has not been changed");
        peopleCursor.requery();
        assertEquals(1, peopleCursor.getCount());

        assertTrue(peopleCursor.moveToFirst());
        summary = ContactSummaryTable.getQueryData(peopleCursor);

        assertTrue(summary != null);
        newContact = new Contact();
        status = mDb.fetchContact(summary.localContactID, newContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        donePhone2 = false;
        for (ContactDetail detail : newContact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_PHONE
                    && detail.nativeDetailId.longValue() == phoneId2) {
                assertEquals(ADD_PHONE_TEST2, detail.getTel());
                assertEquals(ADD_PHONE_PEOPLE_TYPE_TEST2, detail.keyType);
                donePhone2 = true;
                break;
            }
        }

        startSubTest(fnName, "Deleting contact in people");
        status = mDb.deleteContact(summary.localContactID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking people contact list is empty");
        peopleCursor.requery();
        assertEquals(0, peopleCursor.getCount());

        startSubTest(fnName, "Try adding detail in native");
        ContentValues cmValues = new ContentValues();
        cmValues.put(Contacts.ContactMethods.PERSON_ID, personId);
        cmValues.put(Contacts.ContactMethods.DATA, ADD_CM_TEST1);
        cmValues.put(Contacts.ContactMethods.TYPE, ADD_CM_TYPE_TEST1);
        cmValues.put(Contacts.ContactMethods.KIND, ADD_CM_KIND_TEST1);
        Uri cmUri1 = mCr.insert(Contacts.ContactMethods.CONTENT_URI, cmValues);
        assertTrue("Unable to insert contact into native contact methods table 1", cmUri1 != null);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking people contact list is still empty");
        peopleCursor.requery();
        assertEquals(0, peopleCursor.getCount());

        startSubTest(fnName, "Try modifying detail in native");
        phoneValues.clear();
        phoneValues.put(Contacts.Phones.NUMBER, MODIFY_PHONE_TEST1);
        rowsUpdated = mCr.update(phoneUri2, phoneValues, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking people contact list is still empty");
        peopleCursor.requery();
        assertEquals(0, peopleCursor.getCount());

        startSubTest(fnName, "Try deleting detail in native");
        rowsUpdated = mCr.delete(phoneUri1, null, null);
        assertEquals(1, rowsUpdated);

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking people contact list is still empty");
        peopleCursor.requery();
        assertEquals(0, peopleCursor.getCount());

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @MediumTest
    @Suppress
    // Breaks tests.
    public void testRunDuplicateContactTest() {
        final String fnName = "testRunConflictTests";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add first dummy native contact to native");
        ContentValues peopleValues = new ContentValues();
        peopleValues.put(Contacts.People.NAME, DUP_CONTACT_NAME);
        Uri personUri1 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri1 != null);
        final long personId1 = ContentUris.parseId(personUri1);
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(Contacts.Phones.PERSON_ID, personId1);
        phoneValues.put(Contacts.Phones.NUMBER, DUP_CONTACT_NUMBER);
        phoneValues.put(Contacts.Phones.TYPE, Contacts.Phones.TYPE_MOBILE);
        Uri phoneUri1 = mCr.insert(Contacts.Phones.CONTENT_URI, phoneValues);
        assertTrue("Unable to insert contact into native phone table 1", phoneUri1 != null);
        final long phoneId1 = (int)ContentUris.parseId(phoneUri1);

        startSubTest(fnName, "Add second dummy native contact to native");
        peopleValues.clear();
        peopleValues.put(Contacts.People.NAME, DUP_CONTACT_NAME);
        Uri personUri2 = mCr.insert(Contacts.People.CONTENT_URI, peopleValues);
        assertTrue("Unable to insert contact into native people table", personUri2 != null);
        final long personId2 = ContentUris.parseId(personUri2);

        Contact testContact1 = new Contact();
        ContactDetail testDetail1 = new ContactDetail();
        VCardHelper.Name name1 = new VCardHelper.Name();
        name1.firstname = DUP_CONTACT_NAME;
        testDetail1.setName(name1);
        ContactDetail testDetail2 = new ContactDetail();
        testDetail2.setValue(name1.toString(), ContactDetail.DetailKeys.VCARD_NICKNAME, null);
        ContactDetail testDetail3 = new ContactDetail();
        testDetail3.setTel(DUP_CONTACT_NUMBER, ContactDetail.DetailKeyTypes.CELL);
        testContact1.details.add(testDetail1);
        testContact1.details.add(testDetail2);
        testContact1.details.add(testDetail3);
        ServiceStatus status = mDb.addContact(testContact1);
        assertEquals(ServiceStatus.SUCCESS, status);

        Contact testContact2 = new Contact();
        ContactDetail testDetail4 = new ContactDetail();
        VCardHelper.Name name2 = new VCardHelper.Name();
        name2.firstname = DUP_CONTACT_NAME;
        testDetail4.setName(name2);
        ContactDetail testDetail5 = new ContactDetail();
        testDetail5.setValue(name2.toString(), ContactDetail.DetailKeys.VCARD_NICKNAME, null);
        testContact2.details.add(testDetail4);
        testContact2.details.add(testDetail5);
        status = mDb.addContact(testContact2);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        runProcessor();

        Contact updatedContact1 = new Contact();
        status = mDb.fetchContact(testContact1.localContactID, updatedContact1);
        assertEquals(ServiceStatus.SUCCESS, status);
        Contact updatedContact2 = new Contact();
        status = mDb.fetchContact(testContact2.localContactID, updatedContact2);
        assertEquals(ServiceStatus.SUCCESS, status);

        assertEquals(personId1, updatedContact1.nativeContactId.longValue());
        assertEquals(personId2, updatedContact2.nativeContactId.longValue());

        boolean doneName = false;
        boolean doneNickname = false;
        boolean donePhone = false;
        for (ContactDetail detail : updatedContact1.details) {
            assertEquals(personId1, detail.nativeContactId.longValue());
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            assertTrue(detailId != null);
            switch (detail.key) {
                case VCARD_NAME:
                    assertEquals(DUP_CONTACT_NAME, detail.nativeVal1);
                    assertEquals(DUP_CONTACT_NAME, detail.getName().toString());
                    assertEquals(personId1, detailId.longValue());
                    doneName = true;
                    break;
                case VCARD_NICKNAME:
                    assertEquals(DUP_CONTACT_NAME, detail.nativeVal1);
                    assertEquals(DUP_CONTACT_NAME, detail.getValue());
                    assertEquals(personId1, detailId.longValue());
                    doneNickname = true;
                    break;
                case VCARD_PHONE:
                    assertEquals(DUP_CONTACT_NUMBER, detail.nativeVal1);
                    assertEquals(DUP_CONTACT_NUMBER, detail.getValue());
                    assertEquals(ContactDetail.DetailKeyTypes.CELL, detail.keyType);
                    assertEquals(phoneId1, detailId.longValue());
                    donePhone = true;
                    break;
            }
        }

        assertTrue(doneName);
        assertTrue(doneNickname);
        assertTrue(donePhone);

        doneName = false;
        doneNickname = false;
        for (ContactDetail detail : updatedContact2.details) {
            assertEquals(personId2, detail.nativeContactId.longValue());
            detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
            assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = " + detail.key,
                    Integer.valueOf(-1), detail.syncNativeContactId);
            Integer detailId = detail.nativeDetailId;
            assertTrue(detailId != null);
            switch (detail.key) {
                case VCARD_NAME:
                    assertEquals(DUP_CONTACT_NAME, detail.nativeVal1);
                    assertEquals(DUP_CONTACT_NAME, detail.getName().toString());
                    assertEquals(personId2, detailId.longValue());
                    doneName = true;
                    break;
                case VCARD_NICKNAME:
                    assertEquals(DUP_CONTACT_NAME, detail.nativeVal1);
                    assertEquals(DUP_CONTACT_NAME, detail.getValue());
                    assertEquals(personId2, detailId.longValue());
                    doneNickname = true;
                    break;
                default:
                    fail("Unexpected detail: " + detail.key);
            }
        }

        assertTrue(doneName);
        assertTrue(doneNickname);

        nativeCursor.close();
        peopleCursor.close();

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

        startSubTest(fnName, "Checking people database is empty");
        Cursor peopleCursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, peopleCursor.getCount());
        assertTrue(Settings.ENABLE_UPDATE_NATIVE_CONTACTS);

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
                People._ID, People.NAME, People.NOTES
        }, null, null, null);
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Add " + BULK_TEST_NO_CONTACTS + " dummy native contacts");

        List<NativeContactDetails> nativeContactList = new ArrayList<NativeContactDetails>();
        for (int i = 0; i < BULK_TEST_NO_CONTACTS; i++) {
            NativeContactDetails ncd = new NativeContactDetails();
            ncd = mTestModule.addNativeContact(mCr, generateNameType(i), (i & 15) == 5, (i & 3),
                    (i & 3), (i & 3), (i & 3));
            assertTrue(ncd != null);
            nativeContactList.add(ncd);
        }

        startSubTest(fnName, "Running processor");
        runProcessor();

        startSubTest(fnName, "Checking people contact list");
        peopleCursor.requery();
        assertEquals(BULK_TEST_NO_CONTACTS, peopleCursor.getCount());

        checkContacts(nativeContactList);

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    private void checkContacts(List<NativeContactDetails> nativeContactList) {
        for (int i = 0; i < nativeContactList.size(); i++) {
            NativeContactDetails ncd = nativeContactList.get(i);
            ContactIdInfo info = ContactsTable.fetchContactIdFromNative(ncd.mId, mDb
                    .getReadableDatabase());
            Contact contact = new Contact();
            ServiceStatus status = mDb.fetchContact(info.localId, contact);
            assertEquals(ServiceStatus.SUCCESS, status);

            LogUtils.logI("Checking contact local ID " + info.localId + ", native ID " + ncd.mId);
            boolean nameDone = false;
            boolean nicknameDone = false;
            boolean noteDone = false;
            int phonesDone = 0;
            int emailsDone = 0;
            int addressesDone = 0;
            int orgsDone = 0;
            int titlesDone = 0;

            for (ContactDetail detail : contact.details) {
                assertEquals(ncd.mId, detail.nativeContactId);
                detail.syncNativeContactId = fetchSyncNativeId(detail.localDetailID);
                assertEquals("No sync marker, ID = " + detail.nativeDetailId + ", key = "
                        + detail.key, Integer.valueOf(-1), detail.syncNativeContactId);
                switch (detail.key) {
                    case VCARD_NAME:
                        nameDone = true;
                        assertEquals(ncd.mName.toString(), detail.nativeVal1);
                        break;
                    case VCARD_NICKNAME:
                        nicknameDone = true;
                        assertEquals(ncd.mName.toString(), detail.nativeVal1);
                        break;
                    case VCARD_NOTE:
                        noteDone = true;
                        assertEquals(ncd.mNote, detail.nativeVal1);
                        break;
                    case VCARD_PHONE:
                        phonesDone++;
                        for (NativeDetail nd : ncd.mPhoneList) {
                            if (nd.mId.equals(detail.localDetailID)) {
                                assertEquals(nd.mValue1, detail.nativeVal1);
                                assertEquals(nd.mValue2, detail.nativeVal2);
                                assertEquals(nd.mValue3, detail.nativeVal3);
                                break;
                            }
                        }
                        break;
                    case VCARD_EMAIL:
                        emailsDone++;
                        for (NativeDetail nd : ncd.mEmailList) {
                            if (nd.mId.equals(detail.localDetailID)) {
                                assertEquals(nd.mValue1, detail.nativeVal1);
                                assertEquals(nd.mValue2, detail.nativeVal2);
                                assertEquals(nd.mValue3, detail.nativeVal3);
                                break;
                            }
                        }
                        break;
                    case VCARD_ADDRESS:
                        addressesDone++;
                        for (NativeDetail nd : ncd.mAddressList) {
                            if (nd.mId.equals(detail.localDetailID)) {
                                assertEquals(nd.mValue1, detail.nativeVal1);
                                assertEquals(nd.mValue2, detail.nativeVal2);
                                assertEquals(nd.mValue3, detail.nativeVal3);
                                break;
                            }
                        }
                        break;
                    case VCARD_ORG:
                        orgsDone++;
                        for (NativeDetail nd : ncd.mOrgList) {
                            if (nd.mId.equals(detail.localDetailID)) {
                                assertEquals(nd.mValue1, detail.nativeVal1);
                                assertEquals(nd.mValue2, detail.nativeVal2);
                                assertEquals(nd.mValue3, detail.nativeVal3);
                                break;
                            }
                        }
                        break;
                    case VCARD_TITLE:
                        titlesDone++;
                        for (NativeDetail nd : ncd.mTitleList) {
                            if (nd.mId.equals(detail.localDetailID)) {
                                assertEquals(nd.mValue1, detail.nativeVal1);
                                assertEquals(nd.mValue2, detail.nativeVal2);
                                assertEquals(nd.mValue3, detail.nativeVal3);
                                break;
                            }
                        }
                        break;
                    default:
                        fail("Unexpected detail: " + detail.key);
                }
            }

            String nameString = ncd.mName.toString();
            if (nameString.length() > 0) {
                assertTrue("Name was not done", nameDone);
                assertTrue("Nickname was not done", nicknameDone);
            }
            if (ncd.mNote != null && ncd.mNote.length() > 0) {
                assertTrue("Note was not done", noteDone);
            }
            assertEquals(ncd.mPhoneList.size(), phonesDone);
            assertEquals(ncd.mEmailList.size(), emailsDone);
            assertEquals(ncd.mAddressList.size(), addressesDone);
            assertEquals(ncd.mOrgList.size(), orgsDone);
            assertEquals(ncd.mTitleList.size(), titlesDone);
        }
    }

    private TestModule.NativeNameType generateNameType(int val) {
        int idx = val % (TestModule.NativeNameType.values().length);
        return TestModule.NativeNameType.values()[idx];
    }
}
