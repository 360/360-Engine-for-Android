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

package com.vodafone360.people.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.Settings;
import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.ContactChangeLogTable;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactGroupsTable;
import com.vodafone360.people.database.tables.ContactSourceTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ConversationsTable;
import com.vodafone360.people.database.tables.GroupsTable;
import com.vodafone360.people.database.tables.MePresenceCacheTable;
import com.vodafone360.people.database.tables.MyIdentitiesCacheTable;
import com.vodafone360.people.database.tables.NativeChangeLogTable;
import com.vodafone360.people.database.tables.PresenceTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.database.tables.ContactChangeLogTable.ContactChangeInfo;
import com.vodafone360.people.database.tables.ContactDetailsTable.Field;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeyTypes;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeys;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.presence.PresenceDbUtils;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.interfaces.IPeopleService;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.StringBufferPool;
import com.vodafone360.people.utils.ThumbnailUtils;
import com.vodafone360.people.utils.WidgetUtils;

/**
 * The main interface to the client database.
 * <p>
 * The {@link #DATABASE_VERSION} field must be increased each time any change is
 * made to the database schema. This includes any changes to the table name or
 * fields in table classes and any change to persistent settings.
 * <p>
 * All database functionality should be implemented in one of the table Table or
 * Utility sub classes
 * 
 * @version %I%, %G%
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = Settings.LOG_TAG + "Database";

    /**
     * The name of the database file.
     */
    private static final String DATABASE_NAME = "people.db";

    /**
     * The name of the presence database file which is in memory.
     */
    public static final String DATABASE_PRESENCE = "presence1_db";

    /**
     * Contains the database version. Must be increased each time the schema is
     * changed.
     **/
    private static final int DATABASE_VERSION = 64;

    private final List<Handler> mUiEventCallbackList = new ArrayList<Handler>();

    private Context mContext;

    private boolean mMeProfileAvatarChangedFlag;

    private boolean mDbUpgradeRequired = false;
    
    /**
     * Time period in which the sending of database change events to the UI is delayed.
     * During this time period duplicate event types are discarded to avoid clogging the
     * event queue (esp. during first time sync).
     */
    private static final long DATABASE_EVENT_DELAY = 1000; // ms
    
    /**
     * Timer to implement a wait before sending database change events to the UI in
     * order to prevent clogging the queue with duplicate events.
     */
    private final Timer mDbEventTimer = new Timer();
    
    /**
     * SELECT DISTINCT LocalId FROM NativeChangeLog UNION SELECT DISTINCT
     * LocalId FROM ContactDetails WHERE NativeSyncId IS NULL OR NativeSyncId <>
     * -1 ORDER BY 1
     */
    private final static String QUERY_NATIVE_SYNCABLE_CONTACTS_LOCAL_IDS = 
    		NativeChangeLogTable.QUERY_MODIFIED_CONTACTS_LOCAL_IDS_NO_ORDERBY 
    		+ " UNION "
            + ContactDetailsTable.QUERY_NATIVE_SYNCABLE_CONTACTS_LOCAL_IDS
            + " ORDER BY 1";

    /**
     * Datatype holding a database change event. This datatype is used to collect unique
     * events for a certain period before sending them to the UI to avoid clogging of the
     * event queue.
     */
    private class DbEventType {
        @Override
        public boolean equals(Object o) {
            boolean isEqual = false;
            
            if (o instanceof DbEventType) {
                DbEventType event = (DbEventType) o;
                
                if (  (event.ordinal == this.ordinal)
                    &&(event.isExternal == this.isExternal)) {
                    isEqual = true;
                }
            }            
            return isEqual;
        }
        
        int ordinal;
        boolean isExternal;
    }
    
    /**
     * List of database change events which needs to be sent to the UI as soon as the a
     * certain amount of time has passed.
     */
    private final List<DbEventType> mDbEvents = new ArrayList<DbEventType>();
    
    /**
     * Timer task which implements the actualy sending of all stored database change events
     * to the UI.
     */
    private class DbEventTimerTask extends TimerTask {
        public void run() {
            synchronized (mDbEvents) {
                for (DbEventType event:mDbEvents ) {
                
                    fireEventToUi(ServiceUiRequest.DATABASE_CHANGED_EVENT, event.ordinal,
                            (event.isExternal ? 1 : 0), null);
                }
                mDbEvents.clear();
            }
        }
    };

    /**
     * Used for passing server contact IDs around.
     */
    public static class ServerIdInfo {
        public Long localId;

        public Long serverId;

        public Long userId;
    }

    /**
     * Used for passing contact avatar information around.
     * 
     * @see #fetchThumbnailUrls
     */
    public static class ThumbnailInfo {
        public Long localContactId;

        public String photoServerUrl;
    }

    /**
     * An instance of this enum is passed to database change listeners to define
     * the database change type.
     */
    public static enum DatabaseChangeType {
        CONTACTS,
        ACTIVITIES,
        ME_PROFILE,
        ME_PROFILE_PRESENCE_TEXT
    }

    /***
     * Public Constructor.
     * 
     * @param context Android context
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;

        /*
         * // Uncomment the next line to reset the database //
         * context.deleteDatabase(DATABASE_NAME); // copyDatabaseToSd();
         */
    }

    /**
     * Constructor.
     * 
     * @param context the Context where to create the database
     * @param name the name of the database
     */
    public DatabaseHelper(Context context, String name) {

        super(context, name, null, DATABASE_VERSION);
        mContext = context;
    }

    /**
     * Called the first time the database is generated to create all tables.
     * 
     * @param db An open SQLite database object
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            ContactsTable.create(db);
            ContactDetailsTable.create(db);
            ContactSummaryTable.create(db);
            StateTable.create(db);
            ContactChangeLogTable.create(db);
            NativeChangeLogTable.create(db);
            GroupsTable.create(mContext, db);
            ContactGroupsTable.create(db);
            ContactSourceTable.create(db);
            ActivitiesTable.create(db);
            ConversationsTable.create(db);
        } catch (SQLException e) {
            LogUtils.logE("DatabaseHelper.onCreate() SQLException: Unable to create DB table", e);
        }
    }

    /**
     * Called whenever the database is opened.
     * 
     * @param db An open SQLite database object
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Adding the creation code for the MePresenceCacheTable here because this older
        // versions of the client do not contain this table
        MePresenceCacheTable.create(db);
        db.execSQL("ATTACH DATABASE ':memory:' AS " + DATABASE_PRESENCE + ";");
        PresenceTable.create(db);
        MyIdentitiesCacheTable.create(db); // will be created if not existing
    }

    /***
     * Delete and then recreate a newer database structure.
     *
     * @param db An open SQLite database object
     * @param oldVersion The current database version on the device
     * @param newVersion The required database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
        	trace(true, "DatabaseHelper.onUpgrade() Upgrading database version from [" + oldVersion
                    + "] to [" + newVersion + "]");

        	mDbUpgradeRequired = true;
        	removeUserData();
        	//Clearing the ApplicationCache.
        	((MainApplication) mContext).getCache().clearCachedData(mContext);

        } catch (SQLException e) {
            LogUtils.logE("DatabaseHelper.onUpgrade() SQLException: Unable to upgrade database", e);
        }
    }

    /***
     * Deletes the database and then fires a Database Changed Event to the UI.
     */
    private void deleteDatabase() {
        trace(true, "DatabaseHelper.deleteDatabase()");
		synchronized (this) {
			//The condition is put so that getReadableDatabse() will only be called at time
			//of ChangeUser and not at the time of DbUpgrade. Because if we call it when
			//creating or upgrading the Db, that will lead to "IllegalStateException,
			//getReadableDatabase() called recursively".
			if (!mDbUpgradeRequired) {
				getReadableDatabase().close();
			}
			mContext.deleteDatabase(DATABASE_NAME);
		}
        fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
    }

    /***
     * Called when the Application is first started.
     */
    public void start() {
        SQLiteDatabase db = getReadableDatabase();
        if (mDbUpgradeRequired) {
            mDbUpgradeRequired = false;
            db.close();
            db = getReadableDatabase();
        }

        mMeProfileAvatarChangedFlag = StateTable.fetchMeProfileAvatarChangedFlag(db);
    }

    /***
     * Adds a contact to the database and fires an internal database change
     * event.
     * 
     * @param contact A {@link Contact} object which contains the details to be
     *            added
     * @return SUCCESS or a suitable error code
     * @see #deleteContact(long)
     * @see #addContactDetail(ContactDetail)
     * @see #modifyContactDetail(ContactDetail)
     * @see #deleteContactDetail(long)
     * @see #addContactToGroup(long, long)
     * @see #deleteContactFromGroup(long, long)
     */
    public ServiceStatus addContact(Contact contact) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.addContact() contactID[" + contact.contactID
                    + "] nativeContactId[" + contact.nativeContactId + "]");
        }
        List<Contact> contactList = new ArrayList<Contact>();
        contactList.add(contact);
        ServiceStatus status = syncAddContactList(contactList, true, true);
        if (ServiceStatus.SUCCESS == status) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
        }
        return status;
    }

    /***
     * Deletes a contact from the database and fires an internal database change
     * event.
     * 
     * @param localContactID The local ID of the contact to delete
     * @return SUCCESS or a suitable error code
     * @see #addContact(Contact)
     * @see #addContactDetail(ContactDetail)
     * @see #modifyContactDetail(ContactDetail)
     * @see #deleteContactDetail(long)
     * @see #addContactToGroup(long, long)
     * @see #deleteContactFromGroup(long, long)
     */
    public ServiceStatus deleteContact(long localContactID) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.deleteContact() localContactID[" + localContactID + "]");
        }

        if (SyncMeDbUtils.getMeProfileLocalContactId(this) != null
                && SyncMeDbUtils.getMeProfileLocalContactId(this).longValue() == localContactID) {
            LogUtils.logW("DatabaseHelper.deleteContact() Can not delete the Me profile contact");
            return ServiceStatus.ERROR_NOT_FOUND;
        }

        ContactsTable.ContactIdInfo contactIdInfo = ContactsTable.validateContactId(
                localContactID, getWritableDatabase());
        List<ContactsTable.ContactIdInfo> idList = new ArrayList<ContactsTable.ContactIdInfo>();
        idList.add(contactIdInfo);
        ServiceStatus status = syncDeleteContactList(idList, true, true);
        if (ServiceStatus.SUCCESS == status) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
        }
        return status;
    }

    /***
     * Adds a contact detail to the database and fires an internal database
     * change event.
     * 
     * @param detail A {@link ContactDetail} object which contains the detail to
     *            add
     * @return SUCCESS or a suitable error code
     * @see #modifyContactDetail(ContactDetail)
     * @see #deleteContactDetail(long)
     * @see #addContact(Contact)
     * @see #deleteContact(long)
     * @see #addContactToGroup(long, long)
     * @see #deleteContactFromGroup(long, long)
     * @throws NullPointerException When detail is NULL
     */
    public ServiceStatus addContactDetail(ContactDetail detail) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.addContactDetail() name[" + detail.getName() + "]");
        }
        if (detail == null) {
            throw new NullPointerException(
                    "DatabaseHelper.addContactDetail() detail should not be NULL");
        }
        boolean isMeProfile = (SyncMeDbUtils.getMeProfileLocalContactId(this) != null
                && detail.localContactID != null && detail.localContactID.equals(SyncMeDbUtils
                .getMeProfileLocalContactId(this)));

        List<ContactDetail> detailList = new ArrayList<ContactDetail>();
        detailList.add(detail);
        ServiceStatus status = syncAddContactDetailList(detailList, !isMeProfile, !isMeProfile);
        if (status == ServiceStatus.SUCCESS) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
            if (isMeProfile) {
                WidgetUtils.kickWidgetUpdateNow(mContext);
            }
        }
        return status;
    }

    /***
     * Modifies an existing contact detail in the database. Also fires an
     * internal database change event.
     * 
     * @param detail A {@link ContactDetail} object which contains the detail to add    
     *         
     * @return SUCCESS or a suitable error code
     * @see #addContactDetail(ContactDetail)
     * @see #deleteContactDetail(long)
     * @see #addContact(Contact)
     * @see #deleteContact(long)
     * @see #addContactToGroup(long, long)
     * @see #deleteContactFromGroup(long, long)
     */
    public ServiceStatus modifyContactDetail(ContactDetail detail) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.modifyContactDetail() name[" + detail.getName() + "]");
        }
        boolean isMeProfile = SyncMeDbUtils.isMeProfile(this, detail.localContactID);
        List<ContactDetail> detailList = new ArrayList<ContactDetail>();
        detailList.add(detail);
        ServiceStatus status = syncModifyContactDetailList(detailList, !isMeProfile, !isMeProfile);
        if (ServiceStatus.SUCCESS == status) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
            if (isMeProfile) {
                WidgetUtils.kickWidgetUpdateNow(mContext);
            }
        }
        return status;
    }

    /***
     * Deletes a contact detail from the database. Also fires an internal
     * database change event.
     * 
     * @param localContactDetailID The local ID of the detail to delete
     * @return SUCCESS or a suitable error code
     * @see #addContactDetail(ContactDetail)
     * @see #modifyContactDetail(ContactDetail)
     * @see #addContact(Contact)
     * @see #deleteContact(long)
     * @see #addContactToGroup(long, long)
     * @see #deleteContactFromGroup(long, long)
     */
    public ServiceStatus deleteContactDetail(long localContactDetailID) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.deleteContactDetail() localContactDetailID["
                    + localContactDetailID + "]");
        }
        SQLiteDatabase db = getReadableDatabase();
        ContactDetail detail = ContactDetailsTable.fetchDetail(localContactDetailID, db);
        if (detail == null) {
            LogUtils.logE("Database.deleteContactDetail() Unable to find detail for deletion");
            return ServiceStatus.ERROR_NOT_FOUND;
        }
        boolean isMeProfile = false;
        if (detail.localContactID.equals(SyncMeDbUtils.getMeProfileLocalContactId(this))) {
            isMeProfile = true;
        }
        List<ContactDetail> detailList = new ArrayList<ContactDetail>();
        detailList.add(detail);
        ServiceStatus status = syncDeleteContactDetailList(detailList, true, !isMeProfile);
        if (ServiceStatus.SUCCESS == status) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
            if (isMeProfile) {
                WidgetUtils.kickWidgetUpdateNow(mContext);
            }
        }
        return status;
    }

    /***
     * Modifies the server Contact Id and User ID stored in the database for a
     * specific contact.
     * 
     * @param localId The local Id of the contact to modify
     * @param serverId The new server Id
     * @param userId The new user Id
     * @return true if successful
     * @see #fetchContactByServerId(Long, Contact)
     * @see #fetchServerId(long)
     */
    public boolean modifyContactServerId(long localId, Long serverId, Long userId) {
        trace(false, "DatabaseHelper.modifyContactServerId() localId[" + localId + "] "
                + "serverId[" + serverId + "] userId[" + userId + "]");
        final SQLiteDatabase db = getWritableDatabase();

        try {

            db.beginTransaction();

            if (!ContactsTable.modifyContactServerId(localId, serverId, userId, db)) {
                return false;
            }

            db.setTransactionSuccessful();
        } finally {

            db.endTransaction();
        }
        return true;
    }

    /***
     * Sets the Server Id for a contact detail and flags it as synchronized
     * with the server.
     * 
     * @param localDetailId The local Id of the contact detail to modify
     * @param serverDetailId The new server Id
     * @return true if successful
     */
    public boolean syncContactDetail(Long localDetailId, Long serverDetailId) {
        trace(false, "DatabaseHelper.modifyContactDetailServerId() localDetailId[" + localDetailId
                + "]" + " serverDetailId[" + serverDetailId + "]");
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();
            if (!ContactDetailsTable.syncSetServerId(localDetailId, serverDetailId, db)) {
                return false;
            }
            db.setTransactionSuccessful();
        } finally {

            db.endTransaction();
        }
        return true;
    }

    /***
     * Fetches the user's logon credentials from the database.
     * 
     * @param details An empty LoginDetails object which will be filled on
     *            return
     * @return SUCCESS or a suitable error code
     * @see #fetchLogonCredentialsAndPublicKey(LoginDetails, PublicKeyDetails)
     * @see #modifyCredentials(LoginDetails)
     * @see #modifyCredentialsAndPublicKey(LoginDetails, PublicKeyDetails)
     */
    public ServiceStatus fetchLogonCredentials(LoginDetails details) {
        return StateTable.fetchLogonCredentials(details, getReadableDatabase());
    }

    /***
     * Fetches the user's logon credentials and public key information from the
     * database.
     * 
     * @param details An empty LoginDetails object which will be filled on
     *            return
     * @param pubKeyDetails An empty PublicKeyDetails object which will be
     *            filled on return
     * @return SUCCESS or a suitable error code
     * @see #fetchLogonCredentials(LoginDetails)
     * @see #modifyCredentials(LoginDetails)
     * @see #modifyCredentialsAndPublicKey(LoginDetails, PublicKeyDetails)
     */
    public ServiceStatus fetchLogonCredentialsAndPublicKey(LoginDetails details,
            PublicKeyDetails pubKeyDetails) {
        return StateTable.fetchLogonCredentialsAndPublicKey(details, pubKeyDetails,
                getReadableDatabase());
    }

    /***
     * Modifies the user's logon credentials. Note: Only called from tests.
     * 
     * @param details The login details to store
     * @return SUCCESS or a suitable error code
     * @see #fetchLogonCredentials(LoginDetails)
     * @see #fetchLogonCredentialsAndPublicKey(LoginDetails, PublicKeyDetails)
     * @see #modifyCredentialsAndPublicKey(LoginDetails, PublicKeyDetails)
     */
    public ServiceStatus modifyCredentials(LoginDetails details) {
        return StateTable.modifyCredentials(details, getWritableDatabase());
    }

    /***
     * Modifies the user's logon credentials and public key details.
     * 
     * @param details The login details to store
     * @param pubKeyDetails The public key details to store
     * @return SUCCESS or a suitable error code
     * @see #fetchLogonCredentials(LoginDetails)
     * @see #fetchLogonCredentialsAndPublicKey(LoginDetails, PublicKeyDetails)
     * @see #modifyCredentials(LoginDetails)
     */
    public ServiceStatus modifyCredentialsAndPublicKey(LoginDetails details,
            PublicKeyDetails pubKeyDetails) {
        return StateTable.modifyCredentialsAndPublicKey(details, pubKeyDetails,
                getWritableDatabase());
    }

    /***
     * Remove contact changes from the change log. This will be called once the
     * changes have been sent to the server.
     * 
     * @param changeInfoList A list of changeInfoIDs (none of the other fields
     *            in the {@link ContactChangeInfo} object are required).
     * @return true if successful
     */
    public boolean deleteContactChanges(List<ContactChangeLogTable.ContactChangeInfo> changeInfoList) {
        return ContactChangeLogTable.deleteContactChanges(changeInfoList, getWritableDatabase());
    }

    /***
     * Fetches a setting from the database.
     * 
     * @param option The option required.
     * @return A {@link PersistSettings} object which contains the setting data
     *         if successful, null otherwise
     * @see #setOption(PersistSettings)
     */
    public PersistSettings fetchOption(PersistSettings.Option option) {
        PersistSettings setting = StateTable.fetchOption(option, getWritableDatabase());
        if (setting == null) {
            setting = new PersistSettings();
            setting.putDefaultOptionData();
        }
        return setting;
    }

    /***
     * Modifies a setting in the database.
     * 
     * @param setting A {@link PersistSetting} object which is populated with an
     *            option set to a value.
     * @return SUCCESS or a suitable error code
     * @see #fetchOption(com.vodafone360.people.service.PersistSettings.Option)
     */
    public ServiceStatus setOption(PersistSettings setting) {
        ServiceStatus status = StateTable.setOption(setting, getWritableDatabase());
        if (ServiceStatus.SUCCESS == status) {
            fireSettingChangedEvent(setting);
        }
        return status;
    }

    /***
     * Removes all groups from the database.
     * 
     * @return SUCCESS or a suitable error code
     */
    public ServiceStatus deleteAllGroups() {
        SQLiteDatabase db = getWritableDatabase();
        ServiceStatus status = GroupsTable.deleteAllGroups(db);
        if (ServiceStatus.SUCCESS == status) {
            status = GroupsTable.populateSystemGroups(mContext, db);
        }
        return status;
    }

    /***
     * Fetches Avatar URLs from the database for all contacts which have an
     * Avatar and have not yet been loaded.
     * 
     * @param thumbInfoList An empty list where the {@link ThumbnailInfo}
     *            objects will be stored containing the URLs
     * @param firstIndex The 0-based index of the first item to fetch from the
     *            database
     * @param count The maximum number of items to fetch
     * @return SUCCESS or a suitable error code
     * @see ThumbnailInfo
     * @see #fetchThumbnailUrlCount()
     */
    public ServiceStatus fetchThumbnailUrls(List<ThumbnailInfo> thumbInfoList, int firstIndex,
            int count) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.fetchThumbnailUrls() firstIndex[" + firstIndex + "] "
                    + "count[" + count + "]");
        }
        Cursor cursor = null;
        try {
            thumbInfoList.clear();
            cursor = getReadableDatabase().rawQuery(
                    "SELECT " + ContactDetailsTable.TABLE_NAME + "."
                            + ContactDetailsTable.Field.LOCALCONTACTID + "," + Field.STRINGVAL
                            + " FROM " + ContactDetailsTable.TABLE_NAME + " INNER JOIN "
                            + ContactSummaryTable.TABLE_NAME + " WHERE "
                            + ContactDetailsTable.TABLE_NAME + "."
                            + ContactDetailsTable.Field.LOCALCONTACTID + "="
                            + ContactSummaryTable.TABLE_NAME + "."
                            + ContactSummaryTable.Field.LOCALCONTACTID + " AND "
                            + ContactSummaryTable.Field.PICTURELOADED + " =0 " + " AND "
                            + ContactDetailsTable.Field.KEY + "="
                            + ContactDetail.DetailKeys.PHOTO.ordinal() + " LIMIT " + firstIndex
                            + "," + count, null);

            ArrayList<String> urls = new ArrayList<String>();
            ThumbnailInfo thumbnailInfo = null;
            while (cursor.moveToNext()) {
                thumbnailInfo = new ThumbnailInfo();
                if (!cursor.isNull(0)) {
                    thumbnailInfo.localContactId = cursor.getLong(0);
                }
                thumbnailInfo.photoServerUrl = cursor.getString(1);
                if (!urls.contains(thumbnailInfo.photoServerUrl)) {
                    urls.add(thumbnailInfo.photoServerUrl);
                    thumbInfoList.add(thumbnailInfo);
                }
            }
            // LogUtils.logWithName("THUMBNAILS:","urls:\n" + urls);
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Fetches Avatar URLs from the database for all contacts from contactList
     * which have an Avatar and have not yet been loaded.
     * 
     * @param thumbInfoList An empty list where the {@link ThumbnailInfo}
     *            objects will be stored containing the URLs
     * @param contactList list of contacts to fetch the thumbnails for
     * @return SUCCESS or a suitable error code
     * @see ThumbnailInfo
     * @see #fetchThumbnailUrlCount()
     */

    public ServiceStatus fetchThumbnailUrlsForContacts(List<ThumbnailInfo> thumbInfoList,
            final List<Long> contactList) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.fetchThumbnailUrls()");
        }
        StringBuilder localContactIdList = new StringBuilder();
        localContactIdList.append("(");
        Long localContactId = -1l;
        for (Long contactId : contactList) {
            if (localContactId != -1) {
                localContactIdList.append(",");
            }
            localContactId = contactId;
            localContactIdList.append(contactId);
        }
        localContactIdList.append(")");
        
        Cursor cursor = null;
        try {
            thumbInfoList.clear();
            cursor = getReadableDatabase().rawQuery(
                    "SELECT " + ContactDetailsTable.TABLE_NAME + "."
                            + ContactDetailsTable.Field.LOCALCONTACTID + ","
                            + ContactDetailsTable.Field.STRINGVAL + " FROM "
                            + ContactDetailsTable.TABLE_NAME + " INNER JOIN "
                            + ContactSummaryTable.TABLE_NAME + " WHERE "
                            + ContactDetailsTable.TABLE_NAME + "."
                            + ContactDetailsTable.Field.LOCALCONTACTID + " in "
                            + localContactIdList.toString() + " AND "
                            + ContactSummaryTable.Field.PICTURELOADED + " =0 " + " AND "
                            + ContactDetailsTable.Field.KEY + "="
                            + ContactDetail.DetailKeys.PHOTO.ordinal(), null);

            HashSet<String> urlSet = new HashSet<String>();
            ThumbnailInfo thumbnailInfo = null;
            while (cursor.moveToNext()) {
                thumbnailInfo = new ThumbnailInfo();
                if (!cursor.isNull(cursor.getColumnIndexOrThrow(
                		ContactDetailsTable.Field.LOCALCONTACTID.toString()))) {
                    thumbnailInfo.localContactId = cursor.getLong(cursor.getColumnIndexOrThrow(
                    		ContactDetailsTable.Field.LOCALCONTACTID.toString()));
                }
                thumbnailInfo.photoServerUrl = cursor.getString(cursor.getColumnIndexOrThrow(
                		ContactDetailsTable.Field.STRINGVAL.toString()));
                
                // TODO: Investigate if this is really needed
                if (urlSet.add(thumbnailInfo.photoServerUrl)) {
                    thumbInfoList.add(thumbnailInfo);
                }
            }

            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(cursor);
        }
    }
    
    /**
     * Fetches the list of all the contactIds for which the Thumbnail still needs to
     * be downloaded. Firstly, the list of all the contactIds whose picture_loaded
     * flag is set to false is retrieved from the ContactSummaryTable. Then these contactids
     * are further filtered based on whether they have a photo URL assigned to them
     * in the ContactDetails table.
     * @param contactIdList An empty list where the retrieved contact IDs are stored.
     * @return SUCCESS or a suitable error code
     */
    public ServiceStatus fetchContactIdsWithThumbnails(List<Long> contactIdList) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cr = null;
        try {
            String sql = "SELECT " + ContactSummaryTable.Field.LOCALCONTACTID + " FROM "
                    + ContactSummaryTable.TABLE_NAME + " WHERE "
                    + ContactSummaryTable.Field.PICTURELOADED + " =0 AND "
                    + ContactSummaryTable.Field.LOCALCONTACTID + " IN (SELECT "
                    + ContactDetailsTable.Field.LOCALCONTACTID + " FROM "
                    + ContactDetailsTable.TABLE_NAME + " WHERE " + ContactDetailsTable.Field.KEY
                    + "=" + ContactDetail.DetailKeys.PHOTO.ordinal() + ")";
                    

            cr = db.rawQuery(sql, null);
            
            Long localContactId = -1L;
            while (cr.moveToNext()) {
                if (!cr
                        .isNull(cr
                                .getColumnIndexOrThrow(ContactDetailsTable.Field.LOCALCONTACTID
                                        .toString()))) {
                    localContactId = cr.getLong(cr
                            .getColumnIndexOrThrow(ContactDetailsTable.Field.LOCALCONTACTID
                                    .toString()));
                    contactIdList.add(localContactId);
                }
            }
            return ServiceStatus.SUCCESS;
        } catch (SQLException e) {
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(cr);
        }
    }

    /***
     * Fetches the number of Contact Avatars which have not yet been loaded.
     * 
     * @return The number of Avatars
     * @see ThumbnailInfo
     * @see #fetchThumbnailUrls(List, int, int)
     */
    public int fetchThumbnailUrlCount() {
        trace(false, "DatabaseHelper.fetchThumbnailUrlCount()");
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(
                    "SELECT COUNT(" + ContactSummaryTable.Field.SUMMARYID + ") FROM "
                            + ContactSummaryTable.TABLE_NAME + " WHERE "
                            + ContactSummaryTable.Field.PICTURELOADED + " =0 ", null);
            if (cursor.moveToFirst()) {
                if (!cursor.isNull(0)) {
                    return cursor.getInt(0);
                }
            }
            return 0;
        } catch (SQLException e) {
            return 0;
        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Modifies the Me Profile Avatar Changed Flag. When this flag is set to
     * true, it indicates that the avatar needs to be synchronised with the
     * server.
     * 
     * @param avatarChanged true to set the flag, false to clear the flag
     * @return SUCCESS or a suitable error code
     */
    public ServiceStatus modifyMeProfileAvatarChangedFlag(boolean avatarChanged) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.modifyMeProfileAvatarChangedFlag() avatarChanged["
                    + avatarChanged + "]");
        }
        if (avatarChanged == mMeProfileAvatarChangedFlag) {
            return ServiceStatus.SUCCESS;
        }
        ServiceStatus result = StateTable.modifyMeProfileChangedFlag(avatarChanged,
                getWritableDatabase());
        if (ServiceStatus.SUCCESS == result) {
            mMeProfileAvatarChangedFlag = avatarChanged;
        }
        return result;
    }

    /***
     * Fetches a cursor which can be used to iterate through the main contact
     * list.
     * <p>
     * The ContactSummaryTable.getQueryData static method can be used on the
     * cursor returned by this method to create a ContactSummary object.
     * 
     * @param groupFilterId The local ID of a group to filter, or null if no
     *            filter is required
     * @param constraint A search string to filter the contact name, or null if
     *            no filter is required
     * @return The cursor result
     */
    public synchronized Cursor openContactSummaryCursor(Long groupFilterId, CharSequence constraint) {
        return ContactSummaryTable.openContactSummaryCursor(groupFilterId, constraint,
                SyncMeDbUtils.getMeProfileLocalContactId(this), getReadableDatabase());
    }

    public synchronized Cursor openContactsCursor() {
        return ContactsTable.openContactsCursor(getReadableDatabase());
    }

    /***
     * Fetches a contact from the database by its localContactId. The method
     * {@link #fetchBaseContact(long, Contact)} should be used if the contact
     * details properties are not required.
     * 
     * @param localContactId Local ID of the contact to fetch.
     * @param contact Empty {@link Contact} object which will be populated with
     *            data.
     * @return SUCCESS or a suitable ServiceStatus error code.
     */
    public synchronized ServiceStatus fetchContact(long localContactId, Contact contact) {
        SQLiteDatabase db = getReadableDatabase();
        ServiceStatus status = fetchBaseContact(localContactId, contact, db);
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }
        status = ContactDetailsTable.fetchContactDetails(localContactId, contact.details, db);
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Fetches a contact detail from the database.
     * 
     * @param localDetailId The local ID of the detail to fetch
     * @param detail A empty {@link ContactDetail} object which will be filled
     *            with the data
     * @return SUCCESS or a suitable error code
     */
    public synchronized ServiceStatus fetchContactDetail(long localDetailId, ContactDetail detail) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.fetchContactDetail() localDetailId[" + localDetailId + "]");
        }
        Cursor cursor = null;
        try {
            try {
                String[] args = {
                    String.format("%d", localDetailId)
                };
                cursor = getReadableDatabase()
                        .rawQuery(
                                ContactDetailsTable
                                        .getQueryStringSql(ContactDetailsTable.Field.DETAILLOCALID
                                                + " = ?"), args);
            } catch (SQLiteException e) {
                LogUtils.logE("DatabaseHelper.fetchContactDetail() Unable to fetch contact detail",
                        e);
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
            if (!cursor.moveToFirst()) {
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            detail.copy(ContactDetailsTable.getQueryData(cursor));
            return ServiceStatus.SUCCESS;
        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Searches the database for a contact with a given phone number.
     * 
     * @param phoneNumber The telephone number to find
     * @param contact An empty Contact object which will be filled if a contact
     *            is found
     * @param phoneDetail An empty {@link ContactDetail} object which will be
     *            filled with the matching phone number detail
     * @return SUCCESS or a suitable error code
     */
    public synchronized ServiceStatus fetchContactInfo(String phoneNumber, Contact contact,
            ContactDetail phoneDetail) {
        ServiceStatus status = ContactDetailsTable.fetchContactInfo(phoneNumber, phoneDetail,
                null, getReadableDatabase());
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }
        return fetchContact(phoneDetail.localContactID, contact);
    }

    /***
     * Puts a contact into a group.
     * 
     * @param localContactId The local Id of the contact
     * @param groupId The local group Id
     * @return SUCCESS or a suitable error code
     * @see #deleteContactFromGroup(long, long)
     */
    public ServiceStatus addContactToGroup(long localContactId, long groupId) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.addContactToGroup() localContactId[" + localContactId
                    + "] " + "groupId[" + groupId + "]");
        }
        SQLiteDatabase db = getWritableDatabase();
        List<Long> groupIds = new ArrayList<Long>();
        ContactGroupsTable.fetchContactGroups(localContactId, groupIds, db);
        if (groupIds.contains(groupId)) {
            // group is already in db than it's ok
            return ServiceStatus.SUCCESS;
        }
        boolean syncToServer = true;
        boolean isMeProfile = false;
        if (SyncMeDbUtils.getMeProfileLocalContactId(this) != null
                && SyncMeDbUtils.getMeProfileLocalContactId(this).longValue() == localContactId) {
            isMeProfile = true;
            syncToServer = false;
        }
        Contact contact = new Contact();
        ServiceStatus status = fetchContact(localContactId, contact);
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }

        try {
            db.beginTransaction();

            if (!ContactGroupsTable.addContactToGroup(localContactId, groupId, db)) {
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
            if (syncToServer) {
                if (!ContactChangeLogTable.addGroupRel(localContactId, contact.contactID, groupId,
                        db)) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
            }
            db.setTransactionSuccessful();
        } finally {

            db.endTransaction();
        }
        if (syncToServer && !isMeProfile) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Removes a group from a contact.
     * 
     * @param localContactId The local Id of the contact
     * @param groupId The local group Id
     * @return SUCCESS or a suitable error code
     * @see #addContactToGroup(long, long)
     */
    public ServiceStatus deleteContactFromGroup(long localContactId, long groupId) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.deleteContactFromGroup() localContactId[" + localContactId
                    + "] groupId[" + groupId + "]");
        boolean syncToServer = true;
        boolean meProfile = false;
        if (SyncMeDbUtils.getMeProfileLocalContactId(this) != null
                && SyncMeDbUtils.getMeProfileLocalContactId(this).longValue() == localContactId) {
            meProfile = true;
            syncToServer = false;
        }
        Contact contact = new Contact();
        ServiceStatus status = fetchContact(localContactId, contact);
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }
        if (contact.contactID == null) {
            return ServiceStatus.ERROR_NOT_READY;
        }

        SQLiteDatabase db = getWritableDatabase();
        try {

            db.beginTransaction();
            boolean result = ContactGroupsTable.deleteContactFromGroup(localContactId, groupId,
                    db);
            if (result && syncToServer) {
                if (!ContactChangeLogTable.deleteGroupRel(localContactId, contact.contactID,
                        groupId, db)) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
            }
            db.setTransactionSuccessful();
        } finally {

            db.endTransaction();
        }
        if (syncToServer && !meProfile) {
            fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, false);
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Removes all the status or timeline activities from the database. Note:
     * Only called from tests.
     * 
     * @param flag The type of activity to delete or null to delete all
     * @return SUCCESS or a suitable error code
     * @see #addActivities(List)
     * @see #fetchActivitiesIds(List, Long)
     */
    public ServiceStatus deleteActivities(Integer flag) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.deleteActivities() flag[" + flag + "]");
        ServiceStatus status = ActivitiesTable.deleteActivities(flag, getWritableDatabase());
        if (ServiceStatus.SUCCESS == status) {
            if (flag == null || flag.intValue() == ActivityItem.TIMELINE_ITEM) {
                StateTable.modifyLatestPhoneCallTime(System.currentTimeMillis(),
                        getWritableDatabase());
            }
        }
        fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, true);
        return status;
    }

    /***
     * Removes the selected timeline activity from the database.
     * 
     * @param application The MainApplication
     * @param timelineItem TimelineSummaryItem to be deleted
     * @return SUCCESS or a suitable error code
     */
    public ServiceStatus deleteTimelineActivity(MainApplication application,
            TimelineSummaryItem timelineItem, boolean isTimelineAll) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.deleteTimelineActivity()");

        ServiceStatus status = ServiceStatus.SUCCESS;

        if (isTimelineAll) {
            status = ActivitiesTable.deleteTimelineActivities(mContext, timelineItem,
                    getWritableDatabase(), getReadableDatabase());
        } else {
            status = ActivitiesTable.deleteTimelineActivity(mContext, timelineItem,
                    getWritableDatabase(), getReadableDatabase());
        }

        if (status == ServiceStatus.SUCCESS) {
            // Update Notifications in the Notification Bar
            IPeopleService peopleService = application.getServiceInterface();
            long localContactId = 0L;
            if (timelineItem.mLocalContactId != null) {
                localContactId = timelineItem.mLocalContactId;
            }
            peopleService.updateChatNotification(localContactId);

        }
        fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, true);
        return status;
    }

    /**
     * Add a list of new activities to the Activities table.
     * 
     * @param activityList contains the list of activity item
     * @return SUCCESS or a suitable error code
     * @see #deleteActivities(Integer)
     */
    public ServiceStatus addActivities(List<ActivityItem> activityList) {
        SQLiteDatabase writableDb = getWritableDatabase();
        ServiceStatus status = ActivitiesTable.addActivities(activityList, writableDb, mContext);
        ActivitiesTable.cleanupActivityTable(writableDb);
        fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, true);
        return status;
    }

    /***
     * Fetches a list of activity IDs from a given time.
     * 
     * @param activityIdList an empty list to be populated
     * @param timeStamp The oldest time that should be included in the list
     * @return SUCCESS or a suitable error code
     */
    public synchronized ServiceStatus fetchActivitiesIds(List<Long> activityIdList, Long timeStamp) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.fetchActivitiesIds() timeStamp[" + timeStamp + "]");
        }
        activityIdList.clear();
        ActivitiesTable.fetchActivitiesIds(activityIdList, timeStamp, getReadableDatabase());
        return ServiceStatus.SUCCESS;
    }

    /***
     * Fetches fires a database change event to the listeners.
     * 
     * @param type The type of database change (contacts, activity, etc)
     * @param isExternal true if this change came from the server, false if the
     *            change is from the client
     * @see #addEventCallback(Handler)
     * @see #removeEventCallback(Handler)
     * @see #fireSettingChangedEvent(PersistSettings)
     */
    public void fireDatabaseChangedEvent(DatabaseHelper.DatabaseChangeType type, boolean isExternal) {
        DbEventType event = new DbEventType();
        event.ordinal = type.ordinal();
        event.isExternal = isExternal;
        
        synchronized (mDbEvents) {
            if (mDbEvents.size() == 0) {
                // Creating a DbEventTimerTask every time because of preemptive-ness
                DbEventTimerTask dbEventTask = new DbEventTimerTask();                
                mDbEventTimer.schedule(dbEventTask, DATABASE_EVENT_DELAY);
            }
            
            if (!mDbEvents.contains(event)) {
                mDbEvents.add(event);
            }
        }
    }

    /***
     * Add a database change listener. The listener will be notified each time
     * the database is changed.
     * 
     * @param uiHandler The handler which will be notified
     * @see #fireDatabaseChangedEvent(DatabaseChangeType, boolean)
     * @see #fireSettingChangedEvent(PersistSettings)
     */
    public synchronized void addEventCallback(Handler uiHandler) {
        if (!mUiEventCallbackList.contains(uiHandler)) {
            mUiEventCallbackList.add(uiHandler);
        }
    }

    /***
     * Removes a database change listener. This must be called before UI
     * activities are destroyed.
     * 
     * @param uiHandler The handler which will be notified
     * @see #addEventCallback(Handler)
     */
    public synchronized void removeEventCallback(Handler uiHandler) {
        if (mUiEventCallbackList != null) {
            mUiEventCallbackList.remove(uiHandler);
        }
    }

    /***
     * Internal function to fire a setting changed event to listeners.
     * 
     * @param setting The setting that has changed with the new data
     * @see #addEventCallback(Handler)
     * @see #removeEventCallback(Handler)
     * @see #fireDatabaseChangedEvent(DatabaseChangeType, boolean)
     */
    private synchronized void fireSettingChangedEvent(PersistSettings setting) {
        fireEventToUi(ServiceUiRequest.SETTING_CHANGED_EVENT, 0, 0, setting);
    }

    /***
     * Internal function to send an event to all the listeners.
     * 
     * @param event The type of event
     * @param arg1 This value depends on the type of event
     * @param arg2 This value depends on the type of event
     * @param data This value depends on the type of event
     * @see #fireDatabaseChangedEvent(DatabaseChangeType, boolean)
     * @see #fireSettingChangedEvent(PersistSettings)
     */
    private void fireEventToUi(ServiceUiRequest event, int arg1, int arg2, Object data) {
        for (Handler handler : mUiEventCallbackList) {
            Message message = handler.obtainMessage(event.ordinal(), data);
            message.arg1 = arg1;
            message.arg2 = arg2;
            handler.sendMessage(message);
        }
    }

    /***
     * Function used by the contact sync engine to add a list of contacts to the
     * database.
     * 
     * @param contactList The list of contacts received from the server
     * @param syncToServer true if the contacts need to be sent to the server
     * @param syncToNative true if the contacts need to be added to the native
     *            phonebook
     * @return SUCCESS or a suitable error code
     * @see #addContact(Contact)
     */
    public ServiceStatus syncAddContactList(List<Contact> contactList, boolean syncToServer,
            boolean syncToNative) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.syncAddContactList() syncToServer[" + syncToServer
                    + "] syncToNative[" + syncToNative + "]");
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            syncToServer = false;
        }
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            syncToNative = false;
        }

        SQLiteDatabase writableDb = getWritableDatabase();
        
        boolean needFireDbUpdate = false;

        for (Contact contact : contactList) {
            contact.deleted = null;
            contact.localContactID = null;
            if (syncToNative) {
                contact.nativeContactId = null;
            }
            if (syncToServer) {
                contact.contactID = null;
                contact.updated = null;
                contact.synctophone = true;
            }

            try {

                writableDb.beginTransaction();
                ServiceStatus status = ContactsTable.addContact(contact, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    LogUtils.logE("DatabaseHelper.syncAddContactList() Unable to add contact to contacts table, due to a database error");
                    return status;
                }

                List<ContactDetail.DetailKeys> orderList = new ArrayList<ContactDetail.DetailKeys>();
                for (int i = 0; i < contact.details.size(); i++) {
                    final ContactDetail detail = contact.details.get(i);
                    
                    detail.localContactID = contact.localContactID;
                    detail.localDetailID = null;
                    if (syncToServer) {
                        detail.unique_id = null;
                    }
                    if (detail.order != null
                            && (detail.order.equals(ContactDetail.ORDER_PREFERRED))) {
                        if (orderList.contains(detail.key)) {
                            detail.order = ContactDetail.ORDER_NORMAL;
                        } else {
                            orderList.add(detail.key);
                        }
                    }
                    status = ContactDetailsTable.addContactDetail(detail, syncToServer,
                            (syncToNative && contact.synctophone), writableDb);
                    if (ServiceStatus.SUCCESS != status) {
                        LogUtils.logE("DatabaseHelper.syncAddContactList() Unable to add contact detail (for new contact), " +
                        			  "due to a database error. Contact ID[" + contact.localContactID + "]");
                        return status;
                    }
                }
                // AA: added the check to make sure that contacts with empty
                // contact
                // details are not stored
                if (!contact.details.isEmpty()) {
                    status = ContactSummaryTable.addContact(contact, writableDb);
                    if (ServiceStatus.SUCCESS != status) {

                        return status;
                    }
                }

                if (contact.groupList != null) {
                    for (Long groupId : contact.groupList) {
                        if (groupId != -1
                                && !ContactGroupsTable.addContactToGroup(contact.localContactID,
                                        groupId, writableDb)) {
                            return ServiceStatus.ERROR_DATABASE_CORRUPT;
                        }
                    }
                }

                if (contact.sources != null) {
                    for (String source : contact.sources) {
                        if (!ContactSourceTable.addContactSource(contact.localContactID, source,
                                writableDb)) {
                            return ServiceStatus.ERROR_DATABASE_CORRUPT;
                        }
                    }
                }

                if (syncToServer) {
                    if (contact.groupList != null) {
                        for (Long groupId : contact.groupList) {
                            if (!ContactChangeLogTable.addGroupRel(contact.localContactID,
                                    contact.contactID, groupId, writableDb)) {
                                return ServiceStatus.ERROR_DATABASE_CORRUPT;
                            }
                        }
                    }
                }

                /*
                 * FIXME: Hacking a check for me profile here using syncToNative and syncToServer
                 * The me contact does not use a static local contact id 
                 * which is ridiculous. Basically we have to check the syncToNative and syncToServer
                 * flags together with isMeProfile
                 * because luckily as of yet the they are only both false when its me profile 
                 * in case that's the contact being added.
                 */
                String displayName = 
                    updateContactNameInSummary(writableDb, 
                                               contact.localContactID, 
                                               (!syncToNative && !syncToServer) || 
                                               SyncMeDbUtils.isMeProfile(this, contact.localContactID));
                if (null == displayName) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
                // updating timeline
                for (ContactDetail detail : contact.details) {
                // we already have name, don't need to get it again
                    if (detail.key != ContactDetail.DetailKeys.VCARD_NAME) {
                        detail.localContactID = contact.localContactID;
                        detail.nativeContactId = contact.nativeContactId;
                        if (updateTimelineNames(detail, displayName, contact.contactID, writableDb)) {
                            needFireDbUpdate = true;
                        }
                    }
                }
                writableDb.setTransactionSuccessful();
            } finally {
                writableDb.endTransaction();
            }
        }
        if (needFireDbUpdate) {
            fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, false);
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Function used by the contact sync engine to modify a list of contacts in
     * the database.
     * 
     * @param contactList The list of contacts received from the server
     * @param syncToServer true if the contacts need to be sent to the server
     * @param syncToNative true if the contacts need to be modified in the
     *            native phonebook
     * @return SUCCESS or a suitable error code
     */
    public ServiceStatus syncModifyContactList(List<Contact> contactList, boolean syncToServer,
            boolean syncToNative) {
    	
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.syncModifyContactList() syncToServer[" + syncToServer
                    + "] syncToNative[" + syncToNative + "]");
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            syncToServer = false;
        }
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            syncToNative = false;
        }
        SQLiteDatabase writableDb = getWritableDatabase();
        boolean needFireDbUpdate = false;

        for (Contact contact : contactList) {
            if (syncToServer) {
                contact.updated = null;
            }

            try {
                writableDb.beginTransaction();
                ServiceStatus status = ContactsTable.modifyContact(contact, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    LogUtils.logE("DatabaseHelper.syncModifyContactList() Unable to modify contact, due to a database error");
                    return status;
                }

                status = ContactSummaryTable.modifyContact(contact, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }

                if (contact.groupList != null) {
                    status = ContactGroupsTable.modifyContact(contact, writableDb);
                    if (ServiceStatus.SUCCESS != status) {
                        return status;
                    }
                }

                if (contact.sources != null) {
                    if (!ContactSourceTable.deleteAllContactSources(contact.localContactID, writableDb)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                    for (String source : contact.sources) {
                        if (!ContactSourceTable.addContactSource(contact.localContactID, source,
                                writableDb)) {
                            return ServiceStatus.ERROR_DATABASE_CORRUPT;
                        }
                    }
                }

                // END updating timeline events

                // Update the summary with the new contact
                String displayName = updateContactNameInSummary(writableDb, 
                        contact.localContactID, 
                        SyncMeDbUtils.isMeProfile(this, contact.localContactID));
                if (null == displayName) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
                // updating phone no
                for (ContactDetail detail : contact.details) {
                    detail.localContactID = contact.localContactID;
                    detail.nativeContactId = contact.nativeContactId;
                    if (updateTimelineNames(detail, displayName, contact.contactID, writableDb)) {
                        needFireDbUpdate = true;
                    }
                }
                writableDb.setTransactionSuccessful();
            } finally {
                writableDb.endTransaction();
            }
        }
        
        if (needFireDbUpdate) {
            fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, false);
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Function used by the contact sync engine to delete a list of contacts
     * from the database.
     * 
     * @param contactIdList The list of contact IDs received from the server (at
     *            least localId should be set)
     * @param syncToServer true if the contacts need to be deleted from the
     *            server
     * @param syncToNative true if the contacts need to be deleted from the
     *            native phonebook
     * @return SUCCESS or a suitable error code
     * @see #deleteContact(long)
     */
    public ServiceStatus syncDeleteContactList(List<ContactsTable.ContactIdInfo> contactIdList,
            boolean syncToServer, boolean syncToNative) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.syncDeleteContactList() syncToServer[" + syncToServer
                    + "] syncToNative[" + syncToNative + "]");
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            syncToServer = false;
        }
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            syncToNative = false;
        }

        SQLiteDatabase writableDb = getWritableDatabase();

        for (ContactsTable.ContactIdInfo contactIdInfo : contactIdList) {

            try {
                writableDb.beginTransaction();
                if (syncToNative && contactIdInfo.mergedLocalId == null) {
                    if (!NativeChangeLogTable.addDeletedContactChange(contactIdInfo.localId,
                            contactIdInfo.nativeId, writableDb)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                }

                if (syncToServer) {
                    if (!ContactChangeLogTable.addDeletedContactChange(contactIdInfo.localId,
                            contactIdInfo.serverId, syncToServer, writableDb)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                }
                if (!ContactGroupsTable.deleteContact(contactIdInfo.localId, writableDb)) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
                if (SyncMeDbUtils.getMeProfileLocalContactId(this) != null
                        && SyncMeDbUtils.getMeProfileLocalContactId(this).longValue() == contactIdInfo.localId) {
                    ServiceStatus status = StateTable.modifyMeProfileID(null, writableDb);
                    if (ServiceStatus.SUCCESS != status) {
                        return status;
                    }
                    SyncMeDbUtils.setMeProfileId(null);
                    PresenceDbUtils.resetMeProfileIds();
                }
                ServiceStatus status = ContactSummaryTable.deleteContact(contactIdInfo.localId, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    return status;
                }
                status = ContactDetailsTable.deleteDetailByContactId(contactIdInfo.localId, writableDb);
                if (ServiceStatus.SUCCESS != status && ServiceStatus.ERROR_NOT_FOUND != status) {
                    return status;
                }
                status = ContactsTable.deleteContact(contactIdInfo.localId, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    return status;
                }

                if (!deleteThumbnail(contactIdInfo.localId))
                    LogUtils.logE("Not able to delete thumbnail for: " + contactIdInfo.localId);

                // timeline
                ActivitiesTable.removeTimelineContactData(contactIdInfo.localId, writableDb);

                writableDb.setTransactionSuccessful();
            } finally {
                writableDb.endTransaction();
            }
        }

        return ServiceStatus.SUCCESS;
    }
    
    
    /***
     * Function used by the contact sync engine to merge contacts which are
     * marked as duplicate by the server. This involves moving native
     * information from one contact to the other, before deleting it.
     * 
     * @param contactIdList The list of contact IDs (localId, serverId and
     *            mergedLocalId should be set)
     * @return SUCCESS or a suitable error code
     */
    public ServiceStatus syncMergeContactList(List<ContactsTable.ContactIdInfo> contactIdList) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.syncMergeContactList()");
        
        List<ContactDetail> detailInfoList = new ArrayList<ContactDetail>();
        SQLiteDatabase writableDb = getWritableDatabase();
        SQLiteStatement contactStatement = null, contactSummaryStatement = null;
        
        try {
            contactStatement = ContactsTable.mergeContactStatement(writableDb);
            contactSummaryStatement = ContactSummaryTable.mergeContactStatement(writableDb);
            
            writableDb.beginTransaction();
            for (int i = 0; i < contactIdList.size(); i++) {
                ContactsTable.ContactIdInfo contactIdInfo = contactIdList.get(i);
                if (contactIdInfo.mergedLocalId != null) {
                    contactIdInfo.nativeId = ContactsTable.fetchNativeFromLocalId(writableDb, contactIdInfo.localId);
                    
                    LogUtils.logI("DatabaseHelper.syncMergeContactList - Copying native Ids from duplicate to original contact: Dup ID "
                                    + contactIdInfo.localId
                                    + ", Org ID "
                                    + contactIdInfo.mergedLocalId
                                    + ", Nat ID "
                                    + contactIdInfo.nativeId);
    
                    ServiceStatus status = ContactsTable.mergeContact(contactIdInfo, contactStatement);
                    if(status != ServiceStatus.SUCCESS) {
                        return status;
                    }
                    
                    status = ContactSummaryTable.mergeContact(contactIdInfo, contactSummaryStatement);
                    if(status != ServiceStatus.SUCCESS) {
                        return status;
                    }

                    status = ContactDetailsTable.fetchNativeInfo(contactIdInfo.localId, detailInfoList, writableDb);
                    if(status != ServiceStatus.SUCCESS) {
                        return status;
                    }
                    
                    status = ContactDetailsTable.mergeContactDetails(contactIdInfo, detailInfoList, writableDb);
                    if(status != ServiceStatus.SUCCESS) {
                        return status;
                    }
                }
            }
            writableDb.setTransactionSuccessful();
        } 
        finally {
            writableDb.endTransaction();
            if(contactStatement != null) {
                contactStatement.close();
                contactStatement = null;
            }
            
            if(contactSummaryStatement != null) {
                contactSummaryStatement.close();
                contactSummaryStatement = null;
            }
        }
      
        LogUtils.logI("DatabaseHelper.syncMergeContactList - Deleting duplicate contacts");
        return syncDeleteContactList(contactIdList, false, true);
    }
    

    /***
     * Function used by the contact sync engine to add a list of contact details
     * to the database.
     * 
     * @param detailList The list of details received from the server
     * @param syncToServer true if the details need to be sent to the server
     * @param syncToNative true if the contacts need to be added to the native
     *            phonebook
     * @return SUCCESS or a suitable error code
     * @see #addContactDetail(ContactDetail)
     */
    public ServiceStatus syncAddContactDetailList(List<ContactDetail> detailList,
            boolean syncToServer, boolean syncToNative) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            trace(false, "DatabaseHelper.syncAddContactDetailList() syncToServer[" + syncToServer
                    + "] syncToNative[" + syncToNative + "]");
        }
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            syncToServer = false;
        }
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            syncToNative = false;
        }

        boolean needFireDbUpdate = false; 
        SQLiteDatabase db = getWritableDatabase();

        for (ContactDetail contactDetail : detailList) {
            
            contactDetail.localDetailID = null;
            if (syncToServer) {
                contactDetail.unique_id = null;
            }
            if (syncToNative) {
                contactDetail.nativeDetailId = null;
            }
            if (contactDetail.localContactID == null) {
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            try {
                db.beginTransaction();
                ContactsTable.ContactIdInfo contactIdInfo = ContactsTable.validateContactId(
                        contactDetail.localContactID, db);
                if (contactIdInfo == null) {
                    return ServiceStatus.ERROR_NOT_FOUND;
                }
                contactDetail.serverContactId = contactIdInfo.serverId;
                if (contactIdInfo.syncToPhone) {
                    contactDetail.syncNativeContactId = contactIdInfo.nativeId;
                } else {
                    contactDetail.syncNativeContactId = -1;
                }
                if (contactDetail.order != null
                        && contactDetail.order.equals(ContactDetail.ORDER_PREFERRED)) {
                    ContactDetailsTable.removePreferred(contactDetail.localContactID,
                            contactDetail.key, db);
                }
                ServiceStatus status = ContactDetailsTable.addContactDetail(contactDetail,
                        syncToServer, syncToNative, db);
                if (ServiceStatus.SUCCESS != status) {
                    return status;
                }

                // Whenever the photo URL is updated, the photoloaded flag in
                // ContactSummaryTable should be reset to 0 so that when the
                // thumbnails are downloaded later on, the new thumbnail shall
                // also be downloaded.
                // When the picture is being from the client we don't need to set the flag to "TRUE",
                // in order not to override the new picture before it is uploaded.
                if (contactDetail.key == ContactDetail.DetailKeys.PHOTO && TextUtils.isEmpty(contactDetail.photo_url)) {
                        ContactSummaryTable.modifyPictureLoadedFlag(contactDetail.localContactID,
                                false, db);
                }
                String displayName = updateContactNameInSummary(db,
                        contactDetail.localContactID, 
                        SyncMeDbUtils.isMeProfile(this, contactDetail.localContactID));
                if (null == displayName) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }

                if (updateTimelineNames(contactDetail, displayName, null, db)) {
                    needFireDbUpdate = true;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        
        if (needFireDbUpdate) {
            fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, false);
        }

        return ServiceStatus.SUCCESS;
    }


    /**
     * Updates the contents of the activities table when a contact detail
     * changes.
     * 
     * @param cd The new or modified contact detail
     * @param contactFriendlyName Name of contact (if known)
     * @param serverId if known
     * @param db Writable SQLite database for the update
     * @return true if the Activities table was updated, false otherwise
     */
    private boolean updateTimelineNames(ContactDetail cd, String contactFriendlyName, Long serverId,
            SQLiteDatabase db) {
        if (cd.key == ContactDetail.DetailKeys.VCARD_NAME) {
            if (contactFriendlyName != null) {
                ActivitiesTable.updateTimelineContactNameAndId(contactFriendlyName,
                        cd.localContactID, db);
                return true;    
            }
        }
        if (cd.key == ContactDetail.DetailKeys.VCARD_PHONE) {
            if (contactFriendlyName == null) {
                ContactSummary cs = new ContactSummary();
                if (ContactSummaryTable.fetchSummaryItem(cd.localContactID, cs, db) == ServiceStatus.SUCCESS) {
                    contactFriendlyName = cs.formattedName;
                }
            }
            if (contactFriendlyName != null) {
                Long cId = serverId;
                if (cId == null) {
                    cId = ContactsTable.fetchServerId(cd.localContactID, db);
                }
                ActivitiesTable.updateTimelineContactNameAndId(cd.getTel(), contactFriendlyName,
                        cd.localContactID, cId, db);
                return true;
            } else {
                LogUtils.logE("updateTimelineNames() failed to fetch summary Item");
            }
        }
        return false;
    }

    /***
     * Function used by the contact sync engine to modify a list of contact
     * details in the database.
     * 
     * @param contactDetailList The list of details received from the server
     * @param serverIdList A list of server IDs if known, or null
     * @param syncToServer true if the details need to be sent to the server
     * @param syncToNative true if the contacts need to be added to the native
     *            phonebook
     * @return SUCCESS or a suitable error code
     * @see #modifyContactDetail(ContactDetail)
     */
    public ServiceStatus syncModifyContactDetailList(List<ContactDetail> contactDetailList,
            boolean syncToServer, boolean syncToNative) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.syncModifyContactDetailList() syncToServer["
                    + syncToServer + "] syncToNative[" + syncToNative + "]");
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            syncToServer = false;
        }
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            syncToNative = false;
        }

        boolean needFireDbUpdate = false;
        SQLiteDatabase db = getWritableDatabase();

        for (ContactDetail contactDetail : contactDetailList) {
            ContactsTable.ContactIdInfo contactIdInfo = ContactsTable.validateContactId(
                    contactDetail.localContactID, db);
            if (contactIdInfo == null) {
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            contactDetail.serverContactId = contactIdInfo.serverId;
            if (contactIdInfo.syncToPhone) {
                contactDetail.syncNativeContactId = contactIdInfo.nativeId;
            } else {
                contactDetail.syncNativeContactId = -1;
            }
            try {

                db.beginTransaction();
                if (contactDetail.order != null
                        && contactDetail.order.equals(ContactDetail.ORDER_PREFERRED)) {
                    ContactDetailsTable.removePreferred(contactDetail.localContactID,
                            contactDetail.key, db);
                }
                ServiceStatus status = ContactDetailsTable.modifyDetail(contactDetail,
                        syncToServer, syncToNative, db);
                if (ServiceStatus.SUCCESS != status) {
                    return status;
                }

                // Whenever the photo URL is updated, the photoloaded flag in
                // ContactSummaryTable should be reset to 0 so that when the
                // thumbnails are downloaded later on, the new thumbnail shall
                // also be downloaded.
                // When the picture is being from the client we don't need to set the flag to "TRUE",
                // in order not to override the new picture before it is uploaded.
                if (ContactDetail.DetailKeys.PHOTO == contactDetail.key && TextUtils.isEmpty(contactDetail.photo_url)) {
                    ContactSummaryTable.modifyPictureLoadedFlag(contactDetail.localContactID,
                            false, db);
                }

                String displayName = updateContactNameInSummary(db,
                        contactDetail.localContactID, 
                        SyncMeDbUtils.isMeProfile(this, contactDetail.localContactID));
                if (null == displayName) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
                if (updateTimelineNames(contactDetail, displayName, null, db)) {
                    needFireDbUpdate = true;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        
        if (needFireDbUpdate) {
            fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, false);
        }

        return ServiceStatus.SUCCESS;
    }

    /***
     * Function used by the contact sync engine to delete a list of contact
     * details from the database.
     * 
     * @param contactDetailList The list of details which has been deleted on
     *            the server
     * @param serverIdList A list of server IDs if known, or null
     * @param syncToServer true if the details need to be sent to the server
     * @param syncToNative true if the contacts need to be added to the native
     *            phonebook
     * @param meProfile - TRUE if the added contact is Me profile.  
     * @return SUCCESS or a suitable error code
     * @see #deleteContactDetail(long)
     */
    public ServiceStatus syncDeleteContactDetailList(List<ContactDetail> contactDetailList,
            boolean syncToServer, boolean syncToNative) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.syncDeleteContactDetailList() syncToServer["
                    + syncToServer + "] syncToNative[" + syncToNative + "]");
        if (!Settings.ENABLE_SERVER_CONTACT_SYNC) {
            syncToServer = false;
        }
        if (!Settings.ENABLE_UPDATE_NATIVE_CONTACTS) {
            syncToNative = false;
        }

        SQLiteDatabase db = getWritableDatabase();
        boolean needFireDbUpdate = false;
        
        for (ContactDetail contactDetail : contactDetailList) {
            if ((contactDetail.serverContactId == null) || (contactDetail.serverContactId == -1)) {
                ContactsTable.ContactIdInfo contactIdInfo = ContactsTable.validateContactId(
                        contactDetail.localContactID, db);
                if (contactIdInfo == null) {
                    return ServiceStatus.ERROR_NOT_FOUND;
                }
                contactDetail.nativeContactId = contactIdInfo.nativeId;
                contactDetail.serverContactId = contactIdInfo.serverId;
            }

            try {
                db.beginTransaction();
                if (syncToNative) {
                    if (!NativeChangeLogTable.addDeletedContactDetailChange(contactDetail, db)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                }
                if (syncToServer) {
                    if (!ContactChangeLogTable.addDeletedContactDetailChange(contactDetail,
                            syncToServer, db)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                }
                if (!ContactDetailsTable.deleteDetailByDetailId(contactDetail.localDetailID, db)) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }

                // Whenever the photo URL is updated, the photoloaded flag in
                // ContactSummaryTable should be reset to 0 so that when the
                // thumbnails are downloaded later on, the new thumbnail shall
                // also be downloaded.
                // When the picture is being from the client we don't need to set the flag to "TRUE",
                // in order not to override the new picture before it is uploaded.
                if (contactDetail.key == ContactDetail.DetailKeys.PHOTO && TextUtils.isEmpty(contactDetail.photo_url)) {
                    ContactSummaryTable.modifyPictureLoadedFlag(contactDetail.localContactID,
                            false, db);
                    deleteThumbnail(contactDetail.localContactID);
                }

                String displayName = updateContactNameInSummary(db,
                        contactDetail.localContactID,
                        SyncMeDbUtils.isMeProfile(this, contactDetail.localContactID));
                if (displayName == null) {
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
                
                if (updateTimelineNames(contactDetail, displayName, contactDetail.localContactID, db)) {
                    needFireDbUpdate = true;
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (needFireDbUpdate) {
            fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, false);
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Fetches the outer contact object information (no details, groups or
     * sources are included).
     * 
     * @param localContactId The local ID of the contact to fetch
     * @param baseContact An empty Contact object which will be filled with the
     *            data
     * @return SUCCESS or a suitable error code
     * @see #fetchContact(long, Contact)
     */
    private ServiceStatus fetchBaseContact(long localContactId, Contact baseContact,
            SQLiteDatabase db) {
        ServiceStatus status = ContactsTable.fetchContact(localContactId, baseContact, db);
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }
        if (baseContact.groupList == null) {
            baseContact.groupList = new ArrayList<Long>();
        }
        if (!ContactGroupsTable.fetchContactGroups(localContactId, baseContact.groupList, db)) {
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        if (baseContact.sources == null) {
            baseContact.sources = new ArrayList<String>();
        }
        if (!ContactSourceTable.fetchContactSources(localContactId, baseContact.sources, db)) {
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /***
     * Fetches the server Id of a contact.
     * 
     * @param localContactId The local ID of the contact
     * @return The server Id of the contact, or null the contact has not yet
     *         been synchronised
     * @see #fetchContactByServerId(Long, Contact)
     * @see #modifyContactServerId(long, Long, Long)
     */
    public Long fetchServerId(long localContactId) {
        trace(false, "DatabaseHelper.fetchServerId() localContactId[" + localContactId + "]");
        ContactsTable.ContactIdInfo info = ContactsTable.validateContactId(localContactId,
                getReadableDatabase());
        if (info == null) {
            return null;
        }
        return info.serverId;
    }

    /***
     * Remove all user data (Thumbnails, Database, Flags) from the device and
     * notifies the engine manager.
     */
    public void removeUserData() {
        trace(false, "DatabaseHelper.removeUserData()");

        String thumbnailPath = ThumbnailUtils.thumbnailPath(null);
        deleteDirectory(new File(thumbnailPath));
        deleteDatabase();

        SyncMeDbUtils.setMeProfileId(null);

        PresenceDbUtils.resetMeProfileIds();

    }

    /***
     * Deletes a given Thumbnail
     * 
     * @param localContactID The local Id of the contact with the Thumbnail
     */
    private boolean deleteThumbnail(Long localContactID) {
        trace(false, "DatabaseHelper.deleteThumbnail() localContactID[" + localContactID + "]");
        String thumbnailPath = ThumbnailUtils.thumbnailPath(localContactID);
        if (thumbnailPath != null) {
            File file = new File(thumbnailPath);
            if (file.exists()) {
                return file.delete();
            }
        }
        // if the file was not there the deletion was also correct
        return true;
    }

    /***
     * Fetches a contact, given a server Id.
     * 
     * @param contactServerId The server ID of the contact to fetch
     * @param contact An empty Contact object which will be filled with the data
     * @return SUCCESS or a suitable error code
     * @see #modifyContactServerId(long, Long, Long)
     * @see #fetchServerId(long)
     */
    public ServiceStatus fetchContactByServerId(Long contactServerId, Contact contact) {
        final SQLiteStatement statement = ContactsTable
                .fetchLocalFromServerIdStatement(getReadableDatabase());
        Long localId = ContactsTable.fetchLocalFromServerId(contactServerId, statement);
        if (localId == null) {
            return ServiceStatus.ERROR_NOT_FOUND;
        }
        
        if(statement != null) {
            statement.close();
        }
        return fetchContact(localId, contact);
    }

    /***
     * Utility function which compares two contact details to determine if they
     * refer to the same detail (the values may be different). TODO: Move to
     * utility class
     * 
     * @param d1 The first contact detail to compare
     * @param d2 The second contact detail to compare
     * @return true if they are the same
     * @see #hasDetailChanged(ContactDetail, ContactDetail)
     */
    public static boolean doDetailsMatch(ContactDetail d1, ContactDetail d2) {
        if (d1.key == null || !d1.key.equals(d2.key)) {
            return false;
        }
        if (d1.unique_id == null && d2.unique_id == null) {
            return true;
        }
        if (d1.unique_id != null && d1.unique_id.equals(d2.unique_id)) {
            return true;
        }
        return false;
    }

    /***
     * Utility function which compares two contact details to determine if they
     * have the same value. TODO: Move to utility class
     * 
     * @param oldDetail The first contact detail to compare
     * @param newDetail The second contact detail to compare
     * @return true if they have the same value
     * @see #doDetailsMatch(ContactDetail, ContactDetail)
     */
    public static boolean hasDetailChanged(ContactDetail oldDetail, ContactDetail newDetail) {
        if (newDetail.value != null && !newDetail.value.equals(oldDetail.value)) {
            return true;
        }
        if (newDetail.alt != null && !newDetail.alt.equals(oldDetail.alt)) {
            return true;
        }
        if (newDetail.keyType != null && !newDetail.keyType.equals(oldDetail.keyType)) {
            return true;
        }
        if (newDetail.location != null && !newDetail.location.equals(oldDetail.location)) {
            return true;
        }
        if (newDetail.order != null && !newDetail.order.equals(oldDetail.order)) {
            return true;
        }
        if (newDetail.photo != null && !newDetail.photo.equals(oldDetail.photo)) {
            return true;
        }
        if (newDetail.photo_mime_type != null
                && !newDetail.photo_mime_type.equals(oldDetail.photo_mime_type)) {
            return true;
        }
        if (newDetail.photo_url != null && !newDetail.photo_url.equals(oldDetail.photo_url)) {
            return true;
        }
        return false;

    }

    /***
     * Add timeline events to the database.
     * 
     * @param syncItemList The list of items to be added
     * @param isCallLog true if the list has come from the call-log, false
     *            otherwise
     * @return SUCCESS or a suitable error code
     * @see #deleteActivities(Integer)
     * @see #fetchActivitiesIds(List, Long)
     */
    public ServiceStatus addTimelineEvents(ArrayList<TimelineSummaryItem> syncItemList,
            boolean isCallLog) {
        if (Settings.ENABLED_DATABASE_TRACE)
            trace(false, "DatabaseHelper.addTimelineEvents() isCallLog[" + isCallLog + "]");
        SQLiteDatabase writableDb = getWritableDatabase();
        ServiceStatus status = ActivitiesTable.addTimelineEvents(syncItemList, isCallLog,
                writableDb);
        
        if (ServiceStatus.SUCCESS == status) {
            fireDatabaseChangedEvent(DatabaseChangeType.ACTIVITIES, true);
        }
        return status;
    }

    /***
     * Utility function to create a where clause string from a list of
     * conditions. TODO: Move to utility class
     * 
     * @param field The name of the table field to be compared
     * @param itemList The list of items to be compared against the field
     * @param clause This can be "AND", "OR" or any other SQL clause
     * @return The WHERE clause string (without the WHERE)
     */
    public static String createWhereClauseFromList(String field, Object[] itemList, String clause) {
        if (itemList == null || itemList.length == 0) {
            return "";
        }
        StringBuffer whereClause = new StringBuffer();
        whereClause.append("(");
        final boolean isEnum = (itemList[0].getClass().getEnumConstants() != null);
        for (int i = 0; i < itemList.length; i++) {
            Object item = itemList[i];
            if (isEnum) {
                item = ((Enum<?>)itemList[i]).ordinal();
            }
            whereClause.append(field + "=" + item.toString());
            if (i < itemList.length - 1) {
                whereClause.append(" " + clause + " ");
            }
        }
        whereClause.append(")");
        return whereClause.toString();
    }

    /**
     * Determines if the me profile avatar needs to be uploaded onto the server.
     * 
     * @return true if the avatar has changed and needs to be uploaded
     * @see #modifyMeProfileAvatarChangedFlag(boolean)
     */
    public boolean isMeProfileAvatarChanged() {
        return mMeProfileAvatarChangedFlag;
    }

    /***
     * Logs Database activity when the Settings.ENABLED_DATABASE_TRACE flag is
     * set to true.
     * 
     * @param write true if this is debug trace, false otherwise
     * @param input String to Log at Info level
     */
    public static void trace(boolean write, String input) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            if (write) {
                Log.i(LOG_TAG, input);
            } else {
                Log.d(LOG_TAG, input);
            }
        }
    }

    /***
     * Copies a snapshot of the database to the SD Card - Used for testing only.
     * 
     * @return A string which contains a description of the result
     */
    public String copyDatabaseToSd(String info) {
        String fileName = "/sdcard/people_" + info + "_" + System.currentTimeMillis() + ".db";
        close();

        InputStream in = null;
        OutputStream out = null;

        try {
            File sourceFile = mContext.getDatabasePath(DATABASE_NAME);
            File targetFile = new File(fileName);
            in = new FileInputStream(sourceFile);
            out = new FileOutputStream(targetFile);
            final int size = 1024;
            byte[] buf = new byte[size];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return "DatabaseHelper.copyDatabaseToSd() Database copied to SD Card as [" + fileName
                    + "]";
        } catch (FileNotFoundException ex) {
            return "DatabaseHelper.copyDatabaseToSd() File not found [" + ex.getMessage()
                    + "]' in the specified directory.";
        } catch (IOException e) {
            return "DatabaseHelper.copyDatabaseToSd() IOException[" + e.getMessage() + "]";
        } finally {
            CloseUtils.close(in);
            CloseUtils.close(out);
        }
    }

    /**
     * Deletes a directory and all its contents including sub-directories.
     * 
     * @param path file location
     * @return true if directory deleted otherwise false
     */
    private static boolean deleteDirectory(final File path) {
        boolean isDeletionSuccess = true;
        
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (!deleteDirectory(files[i])) {
                        isDeletionSuccess = false;
                    }
                } else {
                    if (!files[i].delete()) {
                        isDeletionSuccess = false;
                    }

                }
            }
        }
        if (isDeletionSuccess) {
            return (path.delete());
        } else {
            return isDeletionSuccess;
        }
    }

    /**
     * find the native contact in the database.
     * 
     * @param c contact
     * @return contact details of the particular contact
     */
    public boolean findNativeContact(Contact c) {
        return ContactDetailsTable.findNativeContact(c, getWritableDatabase());
    }

    /***
     * Stores a flag in the database indicating that the me profile avatar has
     * changed. The avatar will be uploaded to the server shortly.
     */
    public void markMeProfileAvatarChanged() {
        modifyMeProfileAvatarChangedFlag(true);
        // fireDatabaseChangedEvent(DatabaseChangeType.ME_PROFILE, false);
    }

    /**
     * Updates the ContactSummary table with the new/changed Contact.
     * @param writableDatabase - SQLiteDatabase writable database.
     * @param localContactId - long contact local id.
     * @param isMeProfile - boolean that indicates if the localContactId belongs to Me Profile.
	 * @return The updated name, may be null in failure situations
     */
    public String updateContactNameInSummary(SQLiteDatabase writableDatabase,
            long localContactId, boolean isMeProfile) {

        Contact contact = new Contact();
        ServiceStatus status = fetchBaseContact(localContactId, contact, writableDatabase);
        if (ServiceStatus.SUCCESS != status) {
            return null;
        }
        status = ContactDetailsTable.fetchContactDetails(localContactId, contact.details,
                writableDatabase);
        if (ServiceStatus.SUCCESS != status) {
            return null;
        }
        return ContactSummaryTable.updateContactDisplayName(contact, writableDatabase, isMeProfile);
    }

    public List<Contact> fetchContactList() {
        return ContactsTable.fetchContactList(getReadableDatabase());
    }

    /**
     * Adds a native contact to the people database and makes sure that the
     * related tables are updated (Contact, ContactDetail, ContactSummary and
     * Activities).
     * 
     * @param contact the contact to add
     * @return true if successful, false otherwise
     */
    public boolean addNativeContact(ContactChange[] contact) {

        if (contact == null || contact.length <= 0)
            return false;

        final SQLiteDatabase wdb = getWritableDatabase();

        try {

            wdb.beginTransaction();

            // add the contact in the Contacts table
            final ContentValues values = ContactsTable.getNativeContentValues(contact[0]);
            final long internalContactId = ContactsTable.addContact(values, wdb);

            if (internalContactId != -1) {

                // sets the newly created internal contact id to all the
                // ContactChange
                setInternalContactId(contact, internalContactId);

                // the contact was created in the contacts table, now add the
                // details
                if (!ContactDetailsTable.addNativeContactDetails(contact, wdb)) {

                    return false;
                }

                // from this point, legacy code will be called...
                final Contact legacyContact = convertNativeContactChanges(contact);

                // ...update timeline and contact summary with legacy code...
                if (!updateTimelineAndContactSummaryWithLegacyCode(legacyContact, wdb)) {

                    return false;
                }

            } else {

                return false;
            }

            wdb.setTransactionSuccessful();

            return true;
        } catch (Exception e) {

            LogUtils.logE("addNativeContact() - Error:" + e);
        } finally {

            if (wdb != null) {
                wdb.endTransaction();
            }
        }

        return false;
    }

    /**
     * Updates the Timeline and ContactSummary tables with a new contact. Note:
     * this method assumes that it being called within a transaction
     * 
     * @param contact the contact to take info from
     * @param writableDb the db to use to write the updates
     * @return true if successful, false otherwise
     */
    private boolean updateTimelineAndContactSummaryWithLegacyCode(Contact contact,
            SQLiteDatabase writableDb) {

        if (!contact.details.isEmpty()) {
            final ServiceStatus status = ContactSummaryTable.addContact(contact, writableDb);
            if (ServiceStatus.SUCCESS != status) {
                return false;
            }
        }
        // update the summary with the new contact, pass "false" as Me Profile can't be a native contact
        String displayName = updateContactNameInSummary(writableDb, contact.localContactID, false);
        if (displayName == null) {
            return false;
        }
        for (int i = 0; i < contact.details.size(); i++) {
             final ContactDetail detail = contact.details.get(i);
            // updating timeline
             if (detail.key != ContactDetail.DetailKeys.VCARD_NAME) {
                 detail.localContactID = contact.localContactID;
                 detail.nativeContactId = contact.nativeContactId;
                 updateTimelineNames(detail, displayName, contact.contactID, writableDb);
             }
        }
        return true;
    }

    /**
     * Sets the internalContactId for all the ContactChange provided.
     * 
     * @param contact the array of ContactChange to update
     * @param internalContactId the id to set
     */
    private void setInternalContactId(ContactChange[] contact, long internalContactId) {

        for (int i = 0; i < contact.length; i++) {

            contact[i].setInternalContactId(internalContactId);
        }
    }

    /**
     * Converts an array of ContactChange into a Contact object.
     * 
     * @see ContactChange
     * @see Contact
     * @param contactChanges the array of ContactChange to convert
     * @return the equivalent Contact
     */
    private Contact convertNativeContactChanges(ContactChange[] contactChanges) {

        if (contactChanges == null || contactChanges.length <= 0)
            return null;

        final Contact contact = new Contact();

        contact.localContactID = contactChanges[0].getInternalContactId();
        // coming from native
        contact.nativeContactId = new Integer((int)contactChanges[0].getNabContactId());
        contact.synctophone = true;

        // fill the contact with all the details
        for (int i = 0; i < contactChanges.length; i++) {

            final ContactDetail detail = convertContactChange(contactChanges[i]);
            // setting it to -1 means that it does not need to be synced back to
            // native
            detail.syncNativeContactId = -1;
            contact.details.add(detail);
        }

        return contact;
    }

    /**
     * Converts a ContactChange object into an equivalent ContactDetail object.
     * 
     * @see ContactChange
     * @see ContactDetail
     * @param change the ContactChange to convert
     * @return the equivalent ContactDetail
     */
    public ContactDetail convertContactChange(ContactChange change) {

        final ContactDetail detail = new ContactDetail();
        final int flag = change.getFlags();
        // conversion is not straightforward, needs a little tweak
        final int key = ContactDetailsTable.mapContactChangeKeyToInternalKey(change.getKey());

        detail.localContactID = change.getInternalContactId() != ContactChange.INVALID_ID ? change
                .getInternalContactId() : null;
        detail.localDetailID = change.getInternalDetailId() != ContactChange.INVALID_ID ? change
                .getInternalDetailId() : null;
        detail.nativeContactId = change.getNabContactId() != ContactChange.INVALID_ID ? new Integer(
                (int)change.getNabContactId())
                : null;
        detail.nativeDetailId = change.getNabDetailId() != ContactChange.INVALID_ID ? new Integer(
                (int)change.getNabDetailId()) : null;
        detail.unique_id = change.getBackendDetailId() != ContactChange.INVALID_ID ? new Long(
                change.getBackendDetailId()) : null;
        detail.key = DetailKeys.values()[key];
        detail.keyType = DetailKeyTypes.values()[ContactDetailsTable
                .mapContactChangeFlagToInternalType(flag)];
        detail.value = change.getValue();
        detail.order = ContactDetailsTable.mapContactChangeFlagToInternalOrder(flag);

        return detail;
    }

    /**
     * Gets the local IDs of the Contacts that are syncable to native.
     *  
     * @return an array of local contact IDs
     */
    public long[] getNativeSyncableContactsLocalIds() {

        long[] ids = null;
        Cursor cursor = null;

        try {

            final int LOCAL_ID_INDEX = 0;
            final SQLiteDatabase readableDb = getReadableDatabase();

            cursor = readableDb.rawQuery(QUERY_NATIVE_SYNCABLE_CONTACTS_LOCAL_IDS, null);

            if (cursor.getCount() > 0) {

                int i = 0;
                ids = new long[cursor.getCount()];

                while (cursor.moveToNext()) {
                    ids[i++] = cursor.getInt(LOCAL_ID_INDEX);
                }
            } else {

                return null;
            }
        } catch (Exception e) {

            if (Settings.ENABLED_DATABASE_TRACE) {
                DatabaseHelper.trace(true, "getModifiedContactsNativeIds(): " + e);
            }
        } finally {

            CloseUtils.close(cursor);
            cursor = null;
        }

        return ids;
    }

    /**
     * Sets the picture loaded flag and fires a databaseChanged event.
     * 
     * @param localContactId Local contact id of the contact where to set the
     *            flag
     * @param value Value of the flag
     * @return true in case everything went fine, false otherwise
     */
    public final boolean modifyPictureLoadedFlag(final Long localContactId, final Boolean value) {
        ServiceStatus serviceStatus = ContactSummaryTable.modifyPictureLoadedFlag(localContactId,
                value, getWritableDatabase());
        if (ServiceStatus.SUCCESS != serviceStatus) {
            return false;
        }
        fireDatabaseChangedEvent(DatabaseChangeType.CONTACTS, true);
        return true;
    }
    
    /**
     * This API checks if the thumbnail is downloaded for the contact or not.
     * 
     * @param localContactId the contactId for which a check needs to be done if
     *            the thumbnail is loaded or not
     * @return true if the thumbnail is downloaded for the contact.
     */
    public boolean isPictureLoaded(final Long localContactId) {
        if(localContactId == null) {
            return false;
        }
        boolean isPictureLoaded = false;
        Cursor cr = null;
        final SQLiteDatabase db = getReadableDatabase();
        StringBuffer query = StringBufferPool.getStringBuffer(SQLKeys.SELECT);
        query.append(ContactSummaryTable.Field.PICTURELOADED.toString()).append(SQLKeys.FROM)
                .append(ContactSummaryTable.TABLE_NAME).append(SQLKeys.WHERE).append(
                        ContactSummaryTable.Field.LOCALCONTACTID.toString()).append(SQLKeys.EQUALS)
                .append(localContactId);
        try {
            cr = db.rawQuery(StringBufferPool.toStringThenRelease(query), null);
            
            if (cr.moveToFirst()
                    && !cr.isNull(cr.getColumnIndexOrThrow(ContactSummaryTable.Field.PICTURELOADED
                            .toString()))) {
                int picLoaded = cr.getInt(cr
                        .getColumnIndexOrThrow(ContactSummaryTable.Field.PICTURELOADED.toString()));
                isPictureLoaded = picLoaded > 0 ? true : false;
            }
        } catch (SQLiteException e) {
            LogUtils.logE("DatabaseHelper.isPictureLoaded() exception", e);
        } finally {
            CloseUtils.close(cr);
        }
        return isPictureLoaded;
    }
    
    /**
     * This method updates the timeline entries.
     * for the contact when new Phone number is added.
     * @param oldPhoneNumber Phone number for which timeline entries
     * need to be updated.
     * @param localContactID Given contact ID
     *
     */

         public final void updateTimelineForPhoneNumberChange(
        		           final String oldPhoneNumber,
			               final Long localContactID) {
		final SQLiteDatabase db = getWritableDatabase();
		ActivitiesTable.updateTimelineForPhoneNumberChange(
                                                  oldPhoneNumber,
                                                  localContactID,
				                                  db);
	}

	/**
     * This method updates the timeline event for the contact for the provided.
     * Phone number.This function separates the deleted phone number entry
     * @param oldPhoneNumber Phone number for which timeline entries need to be separated.
     * @param localContactID Given contact ID.
     */

	public final void updateTimelineForPhoneNumberDeletion(
                                            final String oldPhoneNumber,
			                                final Long localContactID) {
		             final SQLiteDatabase db = getWritableDatabase();
		             ActivitiesTable.updateTimelineForPhoneNumberDeletion(
		            		                    oldPhoneNumber,
		            		                    localContactID,
		            		                    db);
	}
}

