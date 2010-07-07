package com.vodafone360.people.tests.engine.contactsync;

import java.util.ArrayList;
import java.util.List;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.text.format.Time;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.VCardHelper.Name;
import com.vodafone360.people.datatypes.VCardHelper.Organisation;
import com.vodafone360.people.datatypes.VCardHelper.PostalAddress;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.engine.contactsync.SyncStatus;
import com.vodafone360.people.engine.contactsync.UpdateNativeContacts;
import com.vodafone360.people.engine.contactsync.NativeContactsApi.Account;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.tests.TestModule;
import com.vodafone360.people.utils.VersionUtils;

/**
 * JUnit tests for the UpdateNativeContacts processor.
 */
@Suppress
public class UpdateNativeContactsTest  extends AndroidTestCase {
    
    /**
     * The people database name.
     */
    private final String DATABASE_NAME = "peopletest.db";
    
    /**
     * The DatabaseHelper used to create the people database.
     */
    private DatabaseHelper mDatabaseHelper = null;
    
    /**
     * The 360 People account type.
     */
    private final static String PEOPLE_ACCOUNT_TYPE = "com.vodafone360.people.android.account";
    
    /**
     * A test People account.
     */
    private final static Account PEOPLE_ACCOUNT = new Account("mypeoplelogin", PEOPLE_ACCOUNT_TYPE);
    
    /**
     * A ContactSyncCallback implementation.
     */
    private ContactSyncCallback mContactSyncCallback;
    
    /**
     * The TestModule used to generate contacts.
     */
    private final TestModule mTestModule = new TestModule();
    
    @Override
    protected void setUp() throws Exception {

        super.setUp();
        createDatabase();
        NativeContactsApi.createInstance(getContext());
        if(VersionUtils.is2XPlatform()) {
            // Add Account for the case where we are in 2.X
            NativeContactsApi.getInstance().addPeopleAccount(PEOPLE_ACCOUNT.getName());
            Thread.sleep(100);
        }
        mContactSyncCallback = new ContactSyncCallback();
    }

    @Override
    protected void tearDown() throws Exception {
        
        clearDatabase(mDatabaseHelper);
        mDatabaseHelper = null;
        NativeContactsApiTestHelper.getInstance(getContext()).wipeNab();
        mContactSyncCallback = null;
        
        super.tearDown();
    }
    
    /**
     * Creates the database with all the tables.
     */
    private void createDatabase() {

        mDatabaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME);
    }
    
    /**
     * Clears the people database.
     */
    private void clearDatabase(DatabaseHelper dbh) {

        dbh.close();
        getContext().deleteDatabase(DATABASE_NAME);
    }
    
    /**
     * Tests the instantiation of the UpdateNativeContacts class.
     */
    public void testConstructor() {
        
        UpdateNativeContacts processor = new UpdateNativeContacts(mContactSyncCallback, mDatabaseHelper, getContext().getContentResolver()); 
    }
    
    /**
     * Tests running the processor with an empty database.
     */
    public void testRunWithEmptyDatabase() {
        
        UpdateNativeContacts processor = new UpdateNativeContacts(mContactSyncCallback, mDatabaseHelper, getContext().getContentResolver());
        processor.start();
        assertEquals(1, mContactSyncCallback.mProcessorComplete.size());
        ContactSyncCallback.ProcessorComplete pc = mContactSyncCallback.mProcessorComplete.get(0);
        assertEquals(ServiceStatus.SUCCESS, pc.status);
    }

    /**
     * Tests the export of new syncable contacts.
     */
    public void testExportingNewContacts() {
        
        final int CONTACTS_COUNT = 10;
        
        UpdateNativeContacts processor = new UpdateNativeContacts(mContactSyncCallback, mDatabaseHelper, getContext().getContentResolver());
        
        // check that there are no contacts to sync
        long[] syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
        
        // put some contacts that need to be synchronized to native side
        feedSyncableContactsInDatabase(CONTACTS_COUNT);

        // check the count of the contacts to sync
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(CONTACTS_COUNT, syncableIds.length);
        
        // run the UpdateNativeContacts processor until it finishes or it times out
        runUpdateNativeContactsProcessor(processor);
        
        // check the contacts count on native side
        final NativeContactsApi nca = NativeContactsApi.getInstance();
        long[] ids = null;
        if (VersionUtils.is2XPlatform()) {
            
            ids = nca.getContactIds(PEOPLE_ACCOUNT);
        } else {
            ids = nca.getContactIds(null);
        }
        assertEquals(CONTACTS_COUNT, ids.length);
        
        // check that no more contact is syncable as the export is done
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
    }
    
    /**
     * Tests the export of new syncable contacts.
     */
    @Suppress
    public void testExportingDeletedContacts() {
        
        final int CONTACTS_COUNT = 30;
        
        UpdateNativeContacts processor = new UpdateNativeContacts(mContactSyncCallback, mDatabaseHelper, getContext().getContentResolver());
        
        // check that there are no contacts to sync
        long[] syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
        
        // put some contacts that need to be synchronized to native side
        feedSyncableContactsInDatabase(CONTACTS_COUNT);

        // check the count of the contacts to sync
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(CONTACTS_COUNT, syncableIds.length);
        
        // run the UpdateNativeContacts processor until it finishes or it times out
        runUpdateNativeContactsProcessor(processor);
        
        // check the contacts count on native side
        final NativeContactsApi nca = NativeContactsApi.getInstance();
        long[] ids = null;
        if (VersionUtils.is2XPlatform()) {
            
            ids = nca.getContactIds(PEOPLE_ACCOUNT);
        } else {
            ids = nca.getContactIds(null);
        }
        assertEquals(CONTACTS_COUNT, ids.length);
        
        // check that no more contact is syncable as the export is done
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
        
        // delete 6 of the exported contacts
        mDatabaseHelper.deleteContact(1);
        mDatabaseHelper.deleteContact(3);
        mDatabaseHelper.deleteContact(5);
        mDatabaseHelper.deleteContact(15);
        mDatabaseHelper.deleteContact(20);
        mDatabaseHelper.deleteContact(26);
        
        // check the count of the contacts to sync
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(6, syncableIds.length);
        
        // run the UpdateNativeContacts processor until it finishes or it times out
        processor = new UpdateNativeContacts(mContactSyncCallback, mDatabaseHelper, getContext().getContentResolver());
        mContactSyncCallback.mProcessorComplete.clear();
        runUpdateNativeContactsProcessor(processor);
        
        // check the contacts count on native side
        if (VersionUtils.is2XPlatform()) {
            
            ids = nca.getContactIds(PEOPLE_ACCOUNT);
        } else {
            ids = nca.getContactIds(null);
        }
        assertEquals(CONTACTS_COUNT-6, ids.length);
        
        // check that no more contact is syncable as the export is done
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
    }
    
    /**
     * Tests the export of a new syncable contact with all the possible details combinations.
     */
    public void testExportingContactAllDetails() {
        
        UpdateNativeContacts processor = new UpdateNativeContacts(mContactSyncCallback, mDatabaseHelper, getContext().getContentResolver());
        
        // check that there are no contacts to sync
        long[] syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
        
        // put a contact that need to be synchronized to native side and that contains
        // all the possible details that can be synchronized
        final Contact contact = feedOneSyncableContact();

        // check the count of the contacts to sync
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(1, syncableIds.length);
        
        // run the UpdateNativeContacts processor until it finishes or it times out
        runUpdateNativeContactsProcessor(processor);
        
        // check that the contact on native side is equivalent
        final NativeContactsApi nca = NativeContactsApi.getInstance();
        long[] ids = null;
        if (VersionUtils.is2XPlatform()) {
            
            ids = nca.getContactIds(PEOPLE_ACCOUNT);
        } else {
            ids = nca.getContactIds(null);
        }
        assertEquals(1, ids.length);
        
        // check that no more contact is syncable as the export is done
        syncableIds = mDatabaseHelper.getNativeSyncableContactsLocalIds();
        assertEquals(null, syncableIds);
        
        final ContactChange[] contactChanges = nca.getContact(ids[0]);
        
        assertTrue(compareContactWithContactChange(contact, contactChanges));
    }
    
    /**
     * Compares a Contact details with an array of ContactChange.
     * 
     * @param contact the Contact to compare
     * @param contactChange the array of ContactChange to compare
     * @return true if the Contact is equivalent to the array of ContactChange, false otherwise
     */
    private boolean compareContactWithContactChange(Contact contact, ContactChange[] contactChanges) {
        
        final NativeContactsApi nca = NativeContactsApi.getInstance();
        final List<ContactDetail> details = contact.details;
        
        for (int i = 0; i < details.size(); i++) {
            
            final ContactDetail detail = details.get(i); 
            if (nca.isKeySupported(ContactDetailsTable.mapInternalKeyToContactChangeKey(detail.key.ordinal()))) {
                
                // check if there is a corresponding ContactChange
                if (!hasEquivalentContactChange(contactChanges, detail)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a ContactDetail has an equivalent ContactChange.
     * 
     * @param contactChanges the array of ContactChange where to search
     * @param detail the ContactDetail to for which we have to find an equivalent
     * @return true if an equivalent ContactChange is found, false otherwise
     */
    private boolean hasEquivalentContactChange(ContactChange[] contactChanges, ContactDetail detail) {
        
        for (int i = 0; i < contactChanges.length; i++) {
            
            final ContactChange change = contactChanges[i];
            
            if (change.getValue().equals(detail.value)) {
                
                boolean same = true;
                // the corresponding ContactChange is found
                // check also the keys
                if (ContactDetailsTable.mapContactChangeKeyToInternalKey(change.getKey()) != detail.key.ordinal()) {
                    same = false;
                }
                // check the type
                if (ContactDetailsTable.mapContactChangeFlagToInternalType(change.getFlags()) != detail.keyType.ordinal()) {
                    same = false;
                }
                // check the preferred flag
                if (ContactDetailsTable.mapContactChangeFlagToInternalOrder(change.getFlags()) != detail.order) {
                    same = false;
                }
                
                return same;
            }
        }
        
        return false;
    }
    
    /**
     * Runs the UpdateNativeContacts processor until it completes its task.
     * 
     * @param processor the UpdateNativeContacts to run
     */
    private void runUpdateNativeContactsProcessor(UpdateNativeContacts processor) {
        
        final long TIMEOUT = 60 * 1000; // 1 minute
        final long startTime = System.currentTimeMillis();
        final ArrayList<ContactSyncCallback.ProcessorComplete> pc = mContactSyncCallback.mProcessorComplete;
        
        processor.start();
        
        while (pc.size() == 0) {
            
            if (System.currentTimeMillis() - startTime > TIMEOUT) {
                
                fail();
            }
            processor.onTimeoutEvent();
        }
        
        assertEquals(ServiceStatus.SUCCESS, pc.get(0).status);
    }
    
    /**
     * Feeds the People database with random contacts.
     * 
     * @param contactsCount the number of contact to create
     */
    private void feedSyncableContactsInDatabase(int contactsCount) {
        
        for (int i = 0; i < contactsCount; i++) {
            
            final Contact contact = mTestModule.createDummyContactData();
            contact.synctophone = true;
            mDatabaseHelper.addContact(contact);
        }
    }
    
    /**
     * Feeds the People database with a contact containing all the possible details
     * that can be synced on native side.
     * 
     * @return the created Contact
     */
    private Contact feedOneSyncableContact() {
        
        final Contact contact = new Contact();
        contact.synctophone = true; // set it syncable to native side
        
        contact.aboutMe = "xxx xxyyyy";
        contact.friendOfMine = false;
        contact.gender = 1;
        
        // add a VCARD_NAME
        ContactDetail detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_NAME;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        Name name = new Name();
        name.firstname = "Firstname";
        name.midname = "Midname";
        name.surname = "Surname";
        name.suffixes = "Suffixes";
        name.title = "Title";
        detail.value = VCardHelper.makeName(name); // a VCARD_NAME
        contact.details.add(detail);
        
        // add a VCARD_NICKNAME
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_NICKNAME;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "My Nickname";
        contact.details.add(detail);
        
        // add a birthday VCARD_DATE + BIRTHDAY type 
        detail = new ContactDetail();
        detail.order = ContactDetail.ORDER_NORMAL;
        Time time = new Time();
        time.set(1, 1, 2010);
        detail.setDate(time, ContactDetail.DetailKeyTypes.BIRTHDAY);
        contact.details.add(detail);
        
        // add emails (Work, Home, Other)
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_EMAIL;
        detail.keyType = ContactDetail.DetailKeyTypes.HOME;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "email@home.test";
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_EMAIL;
        detail.keyType = ContactDetail.DetailKeyTypes.WORK;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "email@work.test";
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_EMAIL;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "email@other.test";
        contact.details.add(detail);
        
        // add phones VCARD_PHONE (Home, Cell, Work, Fax, Other)
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.HOME;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "+33111111";
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.CELL;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "+33222222";
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.WORK;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "+33333333";
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.FAX;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "+33444444";
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "+33555555";
        contact.details.add(detail);
        
        // add a preferred detail since all the others are set to normal
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_PHONE;
        detail.keyType = ContactDetail.DetailKeyTypes.HOME;
        detail.order = ContactDetail.ORDER_PREFERRED;
        detail.value = "+33666666";
        contact.details.add(detail);
        
        // add VCARD_ADDRESS (Home, Work, Other)
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_ADDRESS;
        detail.keyType = ContactDetail.DetailKeyTypes.HOME;
        detail.order = ContactDetail.ORDER_NORMAL;
        PostalAddress address = new PostalAddress();
        address.addressLine1 = "home address line 1";
        address.addressLine2 = "home address line 2";
        address.city = "home city";
        address.country = "home country";
        address.county = "home county";
        address.postCode = "home postcode";
        address.postOfficeBox = "home po box";
        detail.value = VCardHelper.makePostalAddress(address);
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_ADDRESS;
        detail.keyType = ContactDetail.DetailKeyTypes.WORK;
        detail.order = ContactDetail.ORDER_NORMAL;
        address = new PostalAddress();
        address.addressLine1 = "work address line 1";
        address.addressLine2 = "work address line 2";
        address.city = "work city";
        address.country = "work country";
        address.county = "work county";
        address.postCode = "work postcode";
        address.postOfficeBox = "work po box";
        detail.value = VCardHelper.makePostalAddress(address);
        contact.details.add(detail);
        
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_ADDRESS;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        address = new PostalAddress();
        address.addressLine1 = "other address line 1";
        address.addressLine2 = "other address line 2";
        address.city = "other city";
        address.country = "other country";
        address.county = "other county";
        address.postCode = "other postcode";
        address.postOfficeBox = "other po box";
        detail.value = VCardHelper.makePostalAddress(address);
        contact.details.add(detail);
        
        // add a VCARD_URL
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_URL;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "http://myurl.test";
        contact.details.add(detail);
        
        // add a VCARD_NOTE
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_NOTE;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "a note";
        contact.details.add(detail);
        
        // add a VCARD_ORG
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_ORG;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        Organisation org = new Organisation();
        org.name = "company name";
        org.unitNames.add("department");
        detail.value = VCardHelper.makeOrg(org);
        contact.details.add(detail);
        
        // add a VCARD_TITLE
        detail = new ContactDetail();
        detail.key = ContactDetail.DetailKeys.VCARD_TITLE;
        detail.keyType = ContactDetail.DetailKeyTypes.UNKNOWN;
        detail.order = ContactDetail.ORDER_NORMAL;
        detail.value = "title";
        contact.details.add(detail);
        
        mDatabaseHelper.addContact(contact);
        
        return contact;
    }
    
    /**
     * A ContactSyncCallback implementation that stores all the calls to onProcessorComplete(). 
     */
    private static class ContactSyncCallback implements IContactSyncCallback {
        
        /**
         * holds the parameters from onProcessorComplete() call
         */
        public static class ProcessorComplete {
            
            public ProcessorComplete(ServiceStatus status, String failureList, Object data) {
                
                this.status = status;
                this.failureList = failureList;
                this.data = data;
            }
            
            public ServiceStatus status;
            public String failureList;
            public Object data;
        }
        
        /**
         * List of all the calls to onProcessorComplete() with the parameters.
         */
        public ArrayList<ProcessorComplete> mProcessorComplete = new ArrayList<ProcessorComplete>(1);
        
        @Override
        public BaseEngine getEngine() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public void onDatabaseChanged() {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public void onProcessorComplete(ServiceStatus status, String failureList, Object data) {
            
            mProcessorComplete.add(new ProcessorComplete(status, failureList, data));
        }
    
        @Override
        public void setActiveRequestId(int reqId) {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public void setSyncStatus(SyncStatus syncStatus) {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public void setTimeout(long timeout) {
            // TODO Auto-generated method stub
            
        }
    }
}
