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
import android.database.Cursor;
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
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.UpdateNativeContacts;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.tests.engine.EngineTestFramework;
import com.vodafone360.people.tests.engine.IEngineTestFrameworkObserver;
import com.vodafone360.people.utils.LogUtils;

@SuppressWarnings("deprecation")
public class UpdateNativeContactsTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver {
    private static final String LOG_TAG = "UpdateNativeContactsTest";

    private static final long MAX_PROCESSOR_TIME = 30000000;

    private static final String TEST_PHONE_NO = "+441928237362";

    private static final String TEST_EMAIL = "sddkf_jsdk1232@sdkfdjk.dksdfjkjffj.co.uk";

    private static final String TEST_ADDRESS_LINE1 = "394 sdfdlsfdk rd";

    private static final String TEST_ADDRESS_LINE2 = "sdlfdksdlfkfkff";

    private static final String TEST_ADDRESS_CITY = "sksdkdfkfdk";

    private static final String TEST_ADDRESS_COUNTY = "394 sdfdlsfdk rd";

    private static final String TEST_ADDRESS_POSTCODE = "394 sdfdlsfdk rd";

    private static final String TEST_ADDRESS_COUNTRY = "United Kingdom";

    private static final String TEST_ORG_NAME = "Ddsdksdlffjkdfk";

    private static final String TEST_ORG_TITLE = "Lskdsdfdklfsdjkl";

    private static final String TEST_MOD_NICKNAME = "ThisIsAModified';Nickname";

    private static final String TEST_MOD_PHONE = "+81828384 85868678 7912872";

    private static final String TEST_MOD_FIRSTNAME = "Bill";

    private static final String TEST_MOD_SURNAME = "Jones";

    private static final String TEST_NEW_NOTE1 = "This is the first note to test";

    private static final String TEST_NEW_NOTE2 = "This is the second note to test";

    private static final int BULK_TEST_NO_CONTACTS = 100;

    EngineTestFramework mEngineTester = null;

    MainApplication mApplication = null;

    DummyContactSyncEngine mEng = null;

    UpdateNativeContacts mProcessor = null;

    ContentResolver mCr = null;

    DatabaseHelper mDb = null;

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
        mProcessor = new UpdateNativeContacts(mEng, mApplication.getDatabase(), mCr);
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
        Log.d("TAG", "UpdateNativeContactsTest.reportBackToEngine");
    }

    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub
    }

    void checkNativeContact(Cursor peopleCursor, Contact contact, boolean newContact) {
        Integer id = peopleCursor.getInt(0);
        assertEquals(id, contact.nativeContactId);
        String contactName = peopleCursor.getString(1);
        String notes = peopleCursor.getString(2);
        boolean doneName = false;
        boolean doneNote = false;
        for (ContactDetail detail : contact.details) {
            switch (detail.key) {
                case VCARD_NAME:
                    assertEquals(contactName, detail.nativeVal1);
                    VCardHelper.Name name = detail.getName();
                    if (name == null) {
                        assertTrue(contactName == null || contactName.length() == 0);
                    } else {
                        assertEquals(name.toString(), contactName);
                    }
                    doneName = true;
                    break;
                case VCARD_NICKNAME:
                    if (newContact) {
                        assertEquals(detail.nativeVal1, contactName);
                        assertEquals(detail.getValue(), contactName);
                    }
                    break;
                case VCARD_NOTE:
                    if (notes == null || notes.length() == 0) {
                        assertTrue("Note has not been synced: contact id " + id + ", note = "
                                + detail.getValue(), detail.value.length() == 0);
                    }
                    if (!doneNote && detail.nativeContactId != null) {
                        assertEquals(detail.nativeVal1, notes);
                        assertEquals(detail.getValue(), notes);
                        doneNote = true;
                    }
            }
        }
        if (!doneName && contactName != null && contactName.length() > 0) {
            fail("The native contact should not have a name");
        }
        if (!doneNote && notes != null && notes.length() > 0) {
            fail("The native contact should not have a note");
        }
        checkNativePhones(id, contact);
        checkNativeContactMethods(id, contact);
        checkNativeOrgs(id, contact);
    }

    void checkNativePhones(int id, Contact contact) {
        Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, id);
        Uri phoneUri = Uri.withAppendedPath(contactUri, People.Phones.CONTENT_DIRECTORY);
        Cursor phonesCursor = mCr.query(phoneUri, new String[] {
                Contacts.Phones._ID, Contacts.Phones.NUMBER, Contacts.Phones.TYPE,
                Contacts.Phones.ISPRIMARY
        }, null, null, null);
        int npPhones = 0;
        for (ContactDetail detail : contact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_PHONE) {
                npPhones++;
            }
        }
        while (phonesCursor.moveToNext()) {
            int expId = phonesCursor.getInt(0);
            boolean done = false;
            for (ContactDetail detail : contact.details) {
                if (detail.key == ContactDetail.DetailKeys.VCARD_PHONE
                        && detail.nativeDetailId != null && detail.nativeDetailId.equals(expId)) {
                    String number = phonesCursor.getString(1);
                    int type = phonesCursor.getInt(2);
                    assertEquals(Integer.valueOf(id), detail.nativeContactId);
                    assertEquals(detail.nativeVal1, number);
                    assertEquals(detail.getValue(), number);
                    assertEquals(detail.nativeVal2, String.valueOf(type));
                    assertEquals(NpToNativePhoneType(detail.keyType), type);
                    boolean isPrimary = (phonesCursor.getInt(3) != 0);
                    if (detail.order == 0) {
                        assertTrue(isPrimary);
                    } else {
                        assertFalse(isPrimary);
                    }
                    done = true;
                    break;
                }
            }
            assertTrue("Native Contact " + id + " has extra phone number " + expId + ", no="
                    + phonesCursor.getString(1), done);
            npPhones--;
        }
        assertEquals(0, npPhones);
        phonesCursor.close();
    }

    void checkNativeContactMethods(int id, Contact contact) {
        Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, id);
        Uri cmUri = Uri.withAppendedPath(contactUri,
                Contacts.People.ContactMethods.CONTENT_DIRECTORY);
        Cursor cmCursor = mCr.query(cmUri, new String[] {
                Contacts.ContactMethods._ID, Contacts.ContactMethods.DATA,
                Contacts.ContactMethods.KIND, Contacts.ContactMethods.TYPE,
                Contacts.ContactMethods.ISPRIMARY
        }, null, null, null);
        int npCms = 0;
        for (ContactDetail detail : contact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_EMAIL
                    || detail.key == ContactDetail.DetailKeys.VCARD_ADDRESS) {
                npCms++;
            }
        }
        while (cmCursor.moveToNext()) {
            int expId = cmCursor.getInt(0);
            int kind = cmCursor.getInt(2);
            boolean done = false;
            if (kind != TestModule.CONTACT_METHODS_KIND_EMAIL
                    && kind != TestModule.CONTACT_METHODS_KIND_ADDRESS) {
                continue;
            }
            for (ContactDetail detail : contact.details) {
                if (detail.nativeDetailId != null && detail.nativeDetailId.equals(expId)) {
                    if (detail.key == ContactDetail.DetailKeys.VCARD_EMAIL
                            || detail.key == ContactDetail.DetailKeys.VCARD_ADDRESS) {
                        String data = cmCursor.getString(1);
                        int type = cmCursor.getInt(3);
                        assertEquals(Integer.valueOf(id), detail.nativeContactId);
                        assertEquals(detail.nativeVal1, data);
                        assertEquals(detail.nativeVal2, String.valueOf(kind));
                        if (kind == TestModule.CONTACT_METHODS_KIND_EMAIL) {
                            assertEquals(ContactDetail.DetailKeys.VCARD_EMAIL, detail.key);
                            assertEquals(detail.getValue(), data);
                        } else {
                            assertEquals(ContactDetail.DetailKeys.VCARD_ADDRESS, detail.key);
                            assertEquals(detail.getPostalAddress().toString(), data);
                        }
                        assertEquals(detail.nativeVal3, String.valueOf(type));
                        assertEquals(NpToNativeCmType(detail.keyType), type);
                        boolean isPrimary = (cmCursor.getInt(4) != 0);
                        if (detail.order == ContactDetail.ORDER_PREFERRED) {
                            assertTrue(isPrimary);
                        } else {
                            assertFalse(isPrimary);
                        }
                        done = true;
                        break;
                    }
                }
            }
            assertTrue("Native Contact " + id + " has extra contact method " + expId + ", no="
                    + cmCursor.getString(1), done);
            npCms--;
        }
        assertEquals(0, npCms);
        cmCursor.close();
    }

    void checkNativeOrgs(int id, Contact contact) {
        Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, id);
        Uri orgUri = Uri.withAppendedPath(contactUri, Contacts.Organizations.CONTENT_DIRECTORY);
        Cursor orgCursor = mCr.query(orgUri, new String[] {
                Contacts.Organizations._ID, Contacts.Organizations.COMPANY,
                Contacts.Organizations.TITLE, Contacts.Organizations.TYPE,
                Contacts.Organizations.ISPRIMARY
        }, null, null, null);
        int npOrgs = 0;
        for (ContactDetail detail : contact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_ORG) {
                npOrgs++;
            }
        }
        while (orgCursor.moveToNext()) {
            int expId = orgCursor.getInt(0);
            boolean done = false;
            if (orgCursor.getString(1).length() > 0) {
                Log.i(LOG_TAG, "Searching for org " + expId + ", name = " + orgCursor.getString(1));
                for (ContactDetail detail : contact.details) {
                    Log.i(LOG_TAG, "detail key = " + detail.key + ", native detail id = "
                            + detail.nativeDetailId);
                    if (detail.key == ContactDetail.DetailKeys.VCARD_ORG
                            && detail.nativeDetailId != null && detail.nativeDetailId.equals(expId)) {
                        String company = orgCursor.getString(1);
                        assertEquals(Integer.valueOf(id), detail.nativeContactId);
                        assertEquals(detail.nativeVal1, company);
                        VCardHelper.Organisation org = detail.getOrg();
                        assertTrue(org != null);
                        assertEquals(org.name, company);
                        assertEquals(0, org.unitNames.size());
                        int type = orgCursor.getInt(3);
                        assertEquals(detail.nativeVal3, String.valueOf(type));
                        assertEquals(NpToNativeOrgType(detail.keyType), type);
                        boolean isPrimary = (orgCursor.getInt(4) != 0);
                        if (detail.order == 0) {
                            assertTrue(isPrimary);
                        } else {
                            LogUtils.logE("Order = " + detail.order + " and no of orgs = " + npOrgs
                                    + " for org " + detail.nativeVal1);
                            assertFalse(isPrimary);
                        }
                        done = true;
                        break;
                    }
                }
                assertTrue("Native Contact " + id + " has extra org " + expId + ", no="
                        + orgCursor.getString(1), done);
                npOrgs--;
            }
        }
        assertEquals(0, npOrgs);
        orgCursor.close();
    }

    void checkNativeTitles(int id, Contact contact) {
        Uri contactUri = ContentUris.withAppendedId(People.CONTENT_URI, id);
        Uri orgUri = Uri.withAppendedPath(contactUri, Contacts.Organizations.CONTENT_DIRECTORY);
        Cursor orgCursor = mCr.query(orgUri, new String[] {
                Contacts.Organizations._ID, Contacts.Organizations.COMPANY,
                Contacts.Organizations.TITLE, Contacts.Organizations.TYPE,
                Contacts.Organizations.ISPRIMARY
        }, null, null, null);
        int npTitles = 0;
        for (ContactDetail detail : contact.details) {
            if (detail.key == ContactDetail.DetailKeys.VCARD_TITLE) {
                npTitles++;
            }
        }
        while (orgCursor.moveToNext()) {
            int expId = orgCursor.getInt(0);
            boolean done = false;
            if (orgCursor.getString(2).length() > 0) {
                for (ContactDetail detail : contact.details) {
                    if (detail.key == ContactDetail.DetailKeys.VCARD_TITLE
                            && detail.nativeDetailId != null && detail.nativeDetailId.equals(expId)) {
                        String title = orgCursor.getString(2);
                        assertEquals(Integer.valueOf(id), detail.nativeContactId);
                        assertEquals(detail.nativeVal1, title);
                        assertEquals(detail.getValue(), title);
                        done = true;
                        break;
                    }
                }
                assertTrue("Native Contact " + id + " has extra title " + expId + ", no="
                        + orgCursor.getString(1), done);
                npTitles--;
            }
        }
        assertEquals(0, npTitles);
        orgCursor.close();
    }

    private int NpToNativePhoneType(ContactDetail.DetailKeyTypes type) {
        if (type == null) {
            return Contacts.Phones.TYPE_OTHER;
        }
        switch (type) {
            case HOME:
                return Contacts.Phones.TYPE_HOME;
            case WORK:
                return Contacts.Phones.TYPE_WORK;
            case MOBILE:
            case CELL:
                return Contacts.Phones.TYPE_MOBILE;
            case FAX:
                return Contacts.Phones.TYPE_FAX_HOME;
            default:
                return Contacts.Phones.TYPE_OTHER;
        }
    }

    private int NpToNativeCmType(ContactDetail.DetailKeyTypes type) {
        if (type == null) {
            return Contacts.ContactMethods.TYPE_OTHER;
        }
        switch (type) {
            case HOME:
                return Contacts.ContactMethods.TYPE_HOME;
            case WORK:
                return Contacts.ContactMethods.TYPE_WORK;
            default:
                return Contacts.ContactMethods.TYPE_OTHER;
        }
    }

    private int NpToNativeOrgType(ContactDetail.DetailKeyTypes type) {
        if (type == null) {
            return Contacts.Organizations.TYPE_OTHER;
        }
        switch (type) {
            case WORK:
                return Contacts.Organizations.TYPE_WORK;
            default:
                return Contacts.Organizations.TYPE_OTHER;
        }
    }

    @SmallTest
    @Suppress
    // Breaks tests
    public void testRunWithNoContactChanges() {
        final String fnName = "testRunWithNoContactChanges";
        Log.i(LOG_TAG, "***** EXECUTING " + fnName + " *****");
        mTestStep = 1;

        startSubTest(fnName, "Checking people database is empty");
        Cursor cursor = mDb.openContactSummaryCursor(null, null);
        assertEquals(0, cursor.getCount());

        startSubTest(fnName, "Checking native database is empty");
        Cursor nativeCursor = mCr.query(People.CONTENT_URI, new String[] {
            People._ID
        }, null, null, null);
        assertEquals(nativeCursor.getCount(), 0);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database is still empty");
        nativeCursor.requery();
        assertEquals(nativeCursor.getCount(), 0);

        nativeCursor.close();
        cursor.close();

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

        startSubTest(fnName, "Add one dummy people contact");
        Contact testContact = mTestModule.createDummyContactData();
        ContactDetail detail1 = new ContactDetail();
        detail1.setValue(TEST_PHONE_NO, ContactDetail.DetailKeys.VCARD_PHONE,
                ContactDetail.DetailKeyTypes.CELL);
        ContactDetail detail2 = new ContactDetail();
        detail2.setValue(TEST_EMAIL, ContactDetail.DetailKeys.VCARD_EMAIL, null);
        ContactDetail detail3 = new ContactDetail();
        VCardHelper.PostalAddress address = new VCardHelper.PostalAddress();
        address.addressLine1 = TEST_ADDRESS_LINE1;
        address.addressLine2 = TEST_ADDRESS_LINE2;
        address.city = TEST_ADDRESS_CITY;
        address.county = TEST_ADDRESS_COUNTY;
        address.city = TEST_ADDRESS_CITY;
        address.postCode = TEST_ADDRESS_POSTCODE;
        address.country = TEST_ADDRESS_COUNTRY;
        detail3.setPostalAddress(address, null);
        for (int i = 0; i < testContact.details.size(); i++) {
            ContactDetail tmpDetail = testContact.details.get(i);
            if (tmpDetail.key == ContactDetail.DetailKeys.VCARD_ORG
                    || tmpDetail.key == ContactDetail.DetailKeys.VCARD_TITLE) {
                testContact.details.remove(i);
                i--;
            }
        }
        ContactDetail detail4 = new ContactDetail();
        VCardHelper.Organisation org = new VCardHelper.Organisation();
        org.name = TEST_ORG_NAME;
        detail4.setOrg(org, null);
        ContactDetail detail5 = new ContactDetail();
        detail5.setValue(TEST_ORG_TITLE, ContactDetail.DetailKeys.VCARD_TITLE, null);
        testContact.details.add(detail1);
        testContact.details.add(detail2);
        testContact.details.add(detail3);
        testContact.details.add(detail4);
        testContact.details.add(detail5);
        mTestModule.fixPreferred(testContact);
        ServiceStatus status = mDb.addContact(testContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact");
        Contact postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking native database now has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches people contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, true);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database still has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

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

        startSubTest(fnName, "Add one dummy people contact");
        Contact testContact = new Contact();
        ContactDetail detail1 = new ContactDetail();
        detail1.setValue(TEST_PHONE_NO, ContactDetail.DetailKeys.VCARD_PHONE,
                ContactDetail.DetailKeyTypes.CELL);
        testContact.details.add(detail1);
        mTestModule.fixPreferred(testContact);
        ServiceStatus status = mDb.addContact(testContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        Contact postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking native database now has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches people contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, true);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database still has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

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
    public void testRunWithNoSyncContact() {
        final String fnName = "testRunWithNoSyncContact";
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

        startSubTest(fnName, "Add one dummy (no sync) people contact");
        Contact testContact = mTestModule.createDummyContactData();
        ContactDetail detail1 = new ContactDetail();
        detail1.setValue(TEST_PHONE_NO, ContactDetail.DetailKeys.VCARD_PHONE,
                ContactDetail.DetailKeyTypes.CELL);
        ContactDetail detail2 = new ContactDetail();
        detail2.setValue(TEST_EMAIL, ContactDetail.DetailKeys.VCARD_EMAIL, null);
        testContact.details.add(detail1);
        testContact.details.add(detail2);
        mTestModule.fixPreferred(testContact);
        testContact.synctophone = false;
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(testContact);
        ServiceStatus status = mDb.syncAddContactList(contactList, false, true);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        Contact postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking native database has no contacts");
        nativeCursor.requery();
        assertEquals(0, nativeCursor.getCount());

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

        startSubTest(fnName, "Add one dummy people contact");
        Contact testContact = mTestModule.createDummyContactData();
        ContactDetail detail1 = testContact.details.get(0); // Name
        ContactDetail detail2 = testContact.details.get(1); // Nickname
        ContactDetail detail3 = new ContactDetail();
        detail3.setValue(TEST_PHONE_NO, ContactDetail.DetailKeys.VCARD_PHONE,
                ContactDetail.DetailKeyTypes.CELL);
        testContact.details.add(detail3);
        mTestModule.fixPreferred(testContact);
        ServiceStatus status = mDb.addContact(testContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        Contact postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking native database now has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches people contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, true);

        startSubTest(fnName, "Modifying Name");
        VCardHelper.Name name = new VCardHelper.Name();
        name.firstname = TEST_MOD_FIRSTNAME;
        name.surname = TEST_MOD_SURNAME;
        detail1.setName(name);
        status = mDb.modifyContactDetail(detail1);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Modifying Nickname");
        detail2.setValue(TEST_MOD_NICKNAME, ContactDetail.DetailKeys.VCARD_NICKNAME, null);
        status = mDb.modifyContactDetail(detail2);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Modifying Phone");
        detail3.setValue(TEST_MOD_PHONE, ContactDetail.DetailKeys.VCARD_PHONE, null);
        status = mDb.modifyContactDetail(detail3);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding Phone");
        ContactDetail detail4 = new ContactDetail();
        detail4.localContactID = testContact.localContactID;
        detail4.setValue(TEST_PHONE_NO, ContactDetail.DetailKeys.VCARD_PHONE, null);
        detail4.order = ContactDetail.ORDER_NORMAL;
        status = mDb.addContactDetail(detail4);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding Email");
        ContactDetail detail5 = new ContactDetail();
        detail5.localContactID = testContact.localContactID;
        detail5.setValue(TEST_EMAIL, ContactDetail.DetailKeys.VCARD_EMAIL,
                ContactDetail.DetailKeyTypes.HOME);
        detail5.order = ContactDetail.ORDER_PREFERRED;
        for (ContactDetail tempDetail : postContact.details) {
            if (tempDetail.key == ContactDetail.DetailKeys.VCARD_EMAIL) {
                detail5.order = ContactDetail.ORDER_NORMAL;
                break;
            }
        }
        status = mDb.addContactDetail(detail5);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding Address");
        ContactDetail detail6 = new ContactDetail();
        detail6.localContactID = testContact.localContactID;
        VCardHelper.PostalAddress address = new VCardHelper.PostalAddress();
        address.addressLine1 = TEST_ADDRESS_LINE1;
        address.addressLine2 = TEST_ADDRESS_LINE2;
        address.city = TEST_ADDRESS_CITY;
        address.county = TEST_ADDRESS_COUNTY;
        address.city = TEST_ADDRESS_CITY;
        address.postCode = TEST_ADDRESS_POSTCODE;
        address.country = TEST_ADDRESS_COUNTRY;
        detail6.setPostalAddress(address, null);
        detail6.order = ContactDetail.ORDER_PREFERRED;
        for (ContactDetail tempDetail : postContact.details) {
            if (tempDetail.key == ContactDetail.DetailKeys.VCARD_ADDRESS) {
                detail6.order = ContactDetail.ORDER_NORMAL;
                break;
            }
        }
        status = mDb.addContactDetail(detail6);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding Note 1");
        ContactDetail detail7 = new ContactDetail();
        detail7.localContactID = testContact.localContactID;
        detail7.setValue(TEST_NEW_NOTE1, ContactDetail.DetailKeys.VCARD_NOTE, null);
        status = mDb.addContactDetail(detail7);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Adding Note 2");
        ContactDetail detail8 = new ContactDetail();
        detail8.localContactID = testContact.localContactID;
        detail8.setValue(TEST_NEW_NOTE2, ContactDetail.DetailKeys.VCARD_NOTE, null);
        status = mDb.addContactDetail(detail8);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        int noOfDetails = postContact.details.size();

        startSubTest(fnName, "Checking native database still has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches modified contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, false);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database still has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Fetching contact again");
        postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        assertEquals(noOfDetails, postContact.details.size());

        startSubTest(fnName, "Checking native contact matches modified contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, false);

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

        startSubTest(fnName, "Add one dummy people contact");
        Contact testContact = mTestModule.createDummyContactData();
        ContactDetail detail1 = new ContactDetail();
        detail1.setValue(TEST_PHONE_NO, ContactDetail.DetailKeys.VCARD_PHONE,
                ContactDetail.DetailKeyTypes.CELL);
        testContact.details.add(detail1);
        ContactDetail detail2 = new ContactDetail();
        detail2.setValue(TEST_NEW_NOTE1, ContactDetail.DetailKeys.VCARD_NOTE, null);
        testContact.details.add(detail2);
        mTestModule.fixPreferred(testContact);
        ServiceStatus status = mDb.addContact(testContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        Contact postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking native database now has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches people contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, true);

        startSubTest(fnName, "Deleting phone number detail");
        status = mDb.deleteContactDetail(detail1.localDetailID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Deleting note");
        status = mDb.deleteContactDetail(detail2.localDetailID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        int noOfDetails = postContact.details.size();

        startSubTest(fnName, "Checking native database still has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches contact with deleted details");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, false);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database still has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Fetching contact again");
        postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);
        assertEquals(noOfDetails, postContact.details.size());

        startSubTest(fnName, "Checking native contact matches modified contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, false);

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

        startSubTest(fnName, "Add one dummy people contact");
        Contact testContact = mTestModule.createDummyContactData();
        ServiceStatus status = mDb.addContact(testContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Fetching contact again");
        Contact postContact = new Contact();
        status = mDb.fetchContact(testContact.localContactID, postContact);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Checking native database now has one contact");
        nativeCursor.requery();
        assertEquals(1, nativeCursor.getCount());

        startSubTest(fnName, "Checking native contact matches people contact");
        assertTrue(nativeCursor.moveToFirst());
        checkNativeContact(nativeCursor, postContact, true);

        startSubTest(fnName, "Deleting contact");
        status = mDb.deleteContact(testContact.localContactID);
        assertEquals(ServiceStatus.SUCCESS, status);

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database now has no contacts");
        nativeCursor.requery();
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database now has no contacts");
        nativeCursor.requery();
        assertEquals(0, nativeCursor.getCount());

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }

    @LargeTest
    @Suppress
    // Breaks tests
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

        startSubTest(fnName, "Adding " + BULK_TEST_NO_CONTACTS + " dummy people contacts");
        for (int i = 0; i < BULK_TEST_NO_CONTACTS; i++) {
            Contact testContact = mTestModule.createDummyContactData();
            ServiceStatus status = mDb.addContact(testContact);
            assertEquals(ServiceStatus.SUCCESS, status);
        }

        startSubTest(fnName, "Running processor");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database now has " + BULK_TEST_NO_CONTACTS
                + " contacts");
        nativeCursor.requery();
        assertEquals(BULK_TEST_NO_CONTACTS, nativeCursor.getCount());

        startSubTest(fnName, "Fetching contacts");
        peopleCursor.requery();
        while (peopleCursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);
            Contact postContact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, postContact);
            assertEquals(ServiceStatus.SUCCESS, status);

            startSubTest(fnName, "Checking native contact matches people contact "
                    + summary.localContactID);
            nativeCursor.moveToPosition(-1);
            boolean found = false;
            while (nativeCursor.moveToNext()) {
                if (nativeCursor.getInt(0) == postContact.nativeContactId.intValue()) {
                    found = true;
                    break;
                }
            }
            assertTrue("Unable to find contact: Local ID = " + postContact.localContactID
                    + ", native ID = " + postContact.nativeContactId, found);
            checkNativeContact(nativeCursor, postContact, true);

            startSubTest(fnName, "Modifying Name");
            for (ContactDetail tempDetail : postContact.details) {
                if (tempDetail.key == ContactDetail.DetailKeys.VCARD_NAME) {
                    VCardHelper.Name name = new VCardHelper.Name();
                    name.firstname = TEST_MOD_FIRSTNAME;
                    name.surname = TEST_MOD_SURNAME;
                    tempDetail.setName(name);
                    status = mDb.modifyContactDetail(tempDetail);
                    assertEquals(ServiceStatus.SUCCESS, status);
                    break;
                }
            }

            startSubTest(fnName, "Adding Email");
            ContactDetail detail = new ContactDetail();
            detail.localContactID = summary.localContactID;
            detail.setValue(TEST_EMAIL, ContactDetail.DetailKeys.VCARD_EMAIL,
                    ContactDetail.DetailKeyTypes.HOME);
            detail.order = ContactDetail.ORDER_PREFERRED;
            for (ContactDetail tempDetail : postContact.details) {
                if (tempDetail.key == ContactDetail.DetailKeys.VCARD_EMAIL) {
                    detail.order = ContactDetail.ORDER_NORMAL;
                    break;
                }
            }
            status = mDb.addContactDetail(detail);
        }

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database still has " + BULK_TEST_NO_CONTACTS
                + " contacts");
        nativeCursor.requery();
        assertEquals(BULK_TEST_NO_CONTACTS, nativeCursor.getCount());

        peopleCursor.moveToPosition(-1);
        while (peopleCursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);
            Contact postContact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, postContact);
            assertEquals(ServiceStatus.SUCCESS, status);

            startSubTest(fnName, "Checking native contact matches people contact "
                    + summary.localContactID);
            nativeCursor.moveToPosition(-1);
            boolean found = false;
            while (nativeCursor.moveToNext()) {
                if (nativeCursor.getInt(0) == postContact.nativeContactId.intValue()) {
                    found = true;
                    break;
                }
            }
            assertTrue("Unable to find contact: Local ID = " + postContact.localContactID
                    + ", native ID = " + postContact.nativeContactId, found);
            checkNativeContact(nativeCursor, postContact, false);

            for (ContactDetail detail : postContact.details) {
                switch (detail.key) {
                    case VCARD_NOTE:
                    case VCARD_PHONE:
                        status = mDb.deleteContactDetail(detail.localDetailID);
                        break;
                }
            }
        }

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database still has " + BULK_TEST_NO_CONTACTS
                + " contacts");
        nativeCursor.requery();
        assertEquals(BULK_TEST_NO_CONTACTS, nativeCursor.getCount());

        peopleCursor.moveToPosition(-1);
        while (peopleCursor.moveToNext()) {
            ContactSummary summary = ContactSummaryTable.getQueryData(peopleCursor);
            Contact postContact = new Contact();
            ServiceStatus status = mDb.fetchContact(summary.localContactID, postContact);
            assertEquals(ServiceStatus.SUCCESS, status);

            startSubTest(fnName, "Checking native contact matches people contact "
                    + summary.localContactID);
            nativeCursor.moveToPosition(-1);
            boolean found = false;
            while (nativeCursor.moveToNext()) {
                if (nativeCursor.getInt(0) == postContact.nativeContactId.intValue()) {
                    found = true;
                    break;
                }
            }
            assertTrue("Unable to find contact: Local ID = " + postContact.localContactID
                    + ", native ID = " + postContact.nativeContactId, found);
            checkNativeContact(nativeCursor, postContact, false);

            status = mDb.deleteContact(summary.localContactID);
        }

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database has no contacts");
        nativeCursor.requery();
        assertEquals(0, nativeCursor.getCount());

        startSubTest(fnName, "Running processor again");
        mProcessor.start();
        runProcessor();

        startSubTest(fnName, "Checking native database has no contacts");
        nativeCursor.requery();
        assertEquals(0, nativeCursor.getCount());

        nativeCursor.close();
        peopleCursor.close();

        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, fnName + " has completed successfully");
        Log.i(LOG_TAG, "*************************************************************************");
        Log.i(LOG_TAG, "");
    }
}
