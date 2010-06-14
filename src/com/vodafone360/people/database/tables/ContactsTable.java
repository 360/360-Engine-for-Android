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

package com.vodafone360.people.database.tables;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.SQLKeys;
import com.vodafone360.people.database.DatabaseHelper.ServerIdInfo;
import com.vodafone360.people.database.persistenceHelper.PersistenceHelper;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.StringBufferPool;

/**
 * Contains all the functionality related to the Contacts database table. This
 * table only contains high level information for a contact. Each contact
 * contains many contact details (see {@link ContactDetailsTable}), these
 * details include name, phone number, address, etc. This class is never
 * instantiated hence all methods must be static.
 * 
 * @version %I%, %G%
 */
public abstract class ContactsTable {

    /**
     * The name of the table as it appears in the database.
     */
    public static final String TABLE_NAME = "Contacts";

    /**
     * Contains ID information used to identify a contact. Also used during sync
     * and merge operations.
     */
    public static class ContactIdInfo {

        /**
         * Local contact ID (primary key)
         */
        public long localId = 0;

        /**
         * Server contact ID (can be null)
         */
        public Long serverId = null;

        /**
         * Native contact ID used by native phonebook database (can be null)
         */
        public Integer nativeId = null;

        /**
         * True if the contact should be synced to the native phonebook, false
         * otherwise This setting is obtained from the server during sync.
         */
        public boolean syncToPhone;

        /**
         * Returns a string representation of this object which should only be
         * used for debug.
         */
        @Override
        public String toString() {
            return "LocalID: " + localId + ", ServerId: " + serverId + ", NativeId:" + nativeId;
        }
    }

    /**
     * An enumeration of all the field names in the database.
     */
    private static enum Field {
        LOCALID("LocalId"),
        SERVERID("ServerId"),
        USERID("UserId"),
        ABOUTME("AboutMe"),
        FRIEND("Friend"),
        GENDER("Gender"),
        UPDATED("Updated"),
        NATIVECONTACTID("NativeContactId"),
        SYNCTOPHONE("Synctophone");

        /**
         * The name of the field as it appears in the database
         */
        private String mField;

        /**
         * Constructor
         * 
         * @param field - The name of the field (see list above)
         */
        private Field(String field) {
            mField = field;
        }

        /**
         * @return the name of the field as it appears in the database.
         */
        public String toString() {
            return mField;
        }
    }

    /**
     * Create Contacts Table.
     * 
     * @param writeableDb A writable SQLite database
     * @throws SQLException If an SQL compilation error occurs
     */
    public static void create(SQLiteDatabase writeableDb) throws SQLException {
        DatabaseHelper.trace(true, "ContactsTable.create()");
        writeableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.LOCALID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Field.SERVERID + " LONG UNIQUE, "
                + Field.USERID + " LONG, " + Field.ABOUTME + " TEXT, " + Field.FRIEND
                + " BOOLEAN, " + Field.GENDER + " TEXT, " + Field.UPDATED + " INTEGER, "
                + Field.NATIVECONTACTID + " INTEGER, " + Field.SYNCTOPHONE + " BOOLEAN);");
    }

    /**
     * Fetches the list of table fields that can be injected into an SQL query
     * statement. The {@link #getQueryData(Cursor, Contact)} method can be used
     * to obtain the data from the query.
     * 
     * @return The query string
     */
    private static String getFullQueryList() {
        return Field.LOCALID + ", " + Field.SERVERID + ", " + Field.USERID + ", " + Field.ABOUTME
                + ", " + Field.FRIEND + ", " + Field.GENDER + ", " + Field.UPDATED + ", "
                + Field.NATIVECONTACTID + ", " + Field.SYNCTOPHONE;
    }

    /**
     * Returns a full SQL query statement to fetch the contact information. The
     * {@link #getQueryData(Cursor, Contact)} method can be used to obtain the
     * data from the query.
     * 
     * @param whereClause An SQL where clause (without the "WHERE"). Cannot be
     *            null.
     * @return The query string
     */
    private static String getQueryStringSql(String whereClause) {
        return "SELECT " + getFullQueryList() + " FROM " + TABLE_NAME + " WHERE " + whereClause;
    }

    /**
     * Column indices which match the query string returned by
     * {@link #getQueryStringSql(String)}.
     */
    private static final int LOCALID = 0;

    private static final int CONTACTID = 1;

    private static final int USERID = 2;

    private static final int ABOUTME = 3;

    private static final int FRIEND = 4;

    private static final int GENDER = 5;

    private static final int UPDATED = 6;

    private static final int NATIVE_CONTACTID = 7;

    private static final int SYNCTOPHONE = 8;

    /**
     * Reads a cursor at its current position to populate a Contact object
     * 
     * @param c A cursor returned from a SELECT query on the projection returned
     *            by {@link #getFullQueryList()}.
     * @param contact An newly constructed Contact object that will be filled
     *            in.
     */
    private static void getQueryData(Cursor c, Contact contact) {
        if (!c.isNull(LOCALID)) {
            contact.localContactID = c.getLong(LOCALID);
        }
        if (!c.isNull(CONTACTID)) {
            contact.contactID = c.getLong(CONTACTID);
        }
        if (!c.isNull(USERID)) {
            contact.userID = c.getLong(USERID);
        }
        if (!c.isNull(ABOUTME)) {
            contact.aboutMe = c.getString(ABOUTME);
        }
        if (!c.isNull(FRIEND)) {
            contact.friendOfMine = (c.getInt(FRIEND) == 0 ? false : true);
        }
        if (!c.isNull(GENDER)) {
            contact.gender = c.getInt(GENDER);
        }
        if (!c.isNull(UPDATED)) {
            contact.updated = c.getLong(UPDATED);
        }
        if (!c.isNull(NATIVE_CONTACTID)) {
            contact.nativeContactId = c.getInt(NATIVE_CONTACTID);
        }
        if (!c.isNull(SYNCTOPHONE)) {
            contact.synctophone = (c.getInt(SYNCTOPHONE) == 0 ? false : true);
        }
    }

    /**
     * Returns a ContentValues object that can be used to insert or modify a
     * contact in the table.
     * 
     * @param contact The source contact object
     * @return The ContentValues object containing the data.
     * @note NULL fields in the given contact will not be included in the
     *       ContentValues
     */
    private static ContentValues fillUpdateData(Contact contact) {
        ContentValues contactValues = new ContentValues();
        if (contact.contactID != null) {
            contactValues.put(Field.SERVERID.toString(), contact.contactID);
        }
        if (contact.userID != null) {
            contactValues.put(Field.USERID.toString(), contact.userID);
        }
        if (contact.friendOfMine != null) {
            contactValues.put(Field.FRIEND.toString(), contact.friendOfMine);
        }
        if (contact.gender != null) {
            contactValues.put(Field.GENDER.toString(), contact.gender);
        }
        if (contact.updated != null) {
            contactValues.put(Field.UPDATED.toString(), contact.updated);
        }
        if (contact.aboutMe != null) {
            contactValues.put(Field.ABOUTME.toString(), contact.aboutMe);
        }
        if (contact.nativeContactId != null) {
            contactValues.put(Field.NATIVECONTACTID.toString(), contact.nativeContactId);
        }
        if (contact.synctophone != null) {
            contactValues.put(Field.SYNCTOPHONE.toString(), contact.synctophone);
        }
        return contactValues;
    }

    /**
     * Validates a contact exists and returns the relevant contact id
     * information.
     * 
     * @param localContactId The local Id of the contact to find
     * @param readableDb Readable database object
     * @return A ContactIdInfo object if successful, or NULL if the contact does
     *         not exist
     */
    public static ContactIdInfo validateContactId(long localContactId, SQLiteDatabase readableDb) {
        ContactIdInfo info = null;
        String[] args = {
            String.format("%d", localContactId)
        };
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.SERVERID + "," + Field.NATIVECONTACTID + ","
                    + Field.SYNCTOPHONE + " FROM " + TABLE_NAME + " WHERE " + Field.LOCALID
                    + " = ?", args);
            if (c.moveToFirst()) {
                info = new ContactIdInfo();
                if (!c.isNull(0)) {
                    info.serverId = c.getLong(0);
                }
                if (!c.isNull(1)) {
                    info.nativeId = c.getInt(1);
                }
                if (!c.isNull(2)) {
                    info.syncToPhone = (c.getInt(2) == 0 ? false : true);
                }
                info.localId = localContactId;
            }
        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.validateContactId() Exception - "
                    + "Unable to validate contact ID", e);
            return null;
        } finally {
            CloseUtils.close(c);
            c = null;
        }

        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactsTable.validateContactId() localContactId["
                    + localContactId + "] serverId[" + info.serverId + "] nativeId["
                    + info.nativeId + "]");
        }
        return info;
    }

    /**
     * Deletes a contact from the Contacts table.
     * 
     * @param localContactID The primary key ID of the contact
     * @param writeableDb Writeable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus deleteContact(long localContactID, SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(true, "ContactsTable.deleteContact() localContactID[" + localContactID
                + "]");
        try {
            String[] args = {
                String.format("%d", localContactID)
            };
            if (writeableDb.delete(TABLE_NAME, Field.LOCALID + "=?", args) <= 0) {
                LogUtils.logE("ContactsTable.deleteContact() Unable to delete contact");
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.deleteContact() SQLException - "
                    + "Unable to delete contact", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
    }

    /**
     * Modifies the contact server ID and user ID stored in the contacts table.
     * 
     * @param localContactId The primary key ID of the contact
     * @param serverId The new server ID
     * @param userId The nwe user ID
     * @param writeableDb Writeable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static boolean modifyContactServerId(long localContactId, Long serverId, Long userId,
            SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(true, "ContactsTable.modifyContactServerId() localContactId["
                + localContactId + "] serverId[" + serverId + "] userId[" + userId + "]");
        try {
            ContentValues cv = new ContentValues();
            cv.put(Field.SERVERID.toString(), serverId);
            String[] args = {
                String.format("%d", localContactId)
            };
            if (writeableDb.update(TABLE_NAME, cv, Field.LOCALID + "=?", args) <= 0) {
                LogUtils.logE("ContactsTable.modifyContactServerId() "
                        + "Unable to update contact server ID");
                return false;
            }

        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.modifyContactServerId() SQLException - "
                    + "Unable to update contact server ID", e);
            return false;
        }
        return true;
    }

    /**
     * Searches for a contact based on native ID and if found, returns the ID
     * information
     * 
     * @param nativeContactId The native contact ID to search for
     * @param readableDb Readable SQLite database
     * @return A ContactIdInfo object if found, otherwise null.
     */
    public static ContactIdInfo fetchContactIdFromNative(int nativeContactId,
            SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactsTable.fetchContactIdFromNative() nativeContactId["
                    + nativeContactId + "]");
        }
        String[] args = {
            String.format("%d", nativeContactId)
        };
        Cursor c1 = null;
        ContactIdInfo info = null;
        try {
            c1 = readableDb.rawQuery("SELECT " + Field.LOCALID + "," + Field.SERVERID + " FROM "
                    + TABLE_NAME + " WHERE " + Field.NATIVECONTACTID + " = ?", args);
            if (!c1.moveToFirst()) {
                return null;
            }
            info = new ContactIdInfo();
            if (!c1.isNull(0)) {
                info.localId = c1.getLong(0);
            }
            if (!c1.isNull(1)) {
                info.serverId = c1.getLong(1);
            }

        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchContactIdFromNative() "
                    + "Exception - Unable to fetch contact", e);
            return null;
        } finally {
            CloseUtils.close(c1);
            c1 = null;
        }
        return info;
    }

    /**
     * Adds a contact to the table. See {@link #fillUpdateData(Contact)} for
     * which fields from the Contact object are read.
     * 
     * @param contact The source Contact object
     * @param writeableDb Writeable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus addContact(Contact contact, SQLiteDatabase writeableDb) {
        try {
            contact.localContactID = writeableDb.insertOrThrow(ContactsTable.TABLE_NAME,
                    Field.SERVERID.toString(), ContactsTable.fillUpdateData(contact));
            if (contact.localContactID < 0) {
                contact.localContactID = null;
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
            if (Settings.ENABLED_DATABASE_TRACE) {
                DatabaseHelper.trace(true, "ContactsTable.addContact() localContactID["
                        + contact.localContactID + "]");
            }
            return ServiceStatus.SUCCESS;
        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.addContact() SQLException - Unable to add contact", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
    }

    /**
     * Finds the server ID associated with a contact
     * 
     * @param localContactId The primary key ID of the contact to find
     * @param readableDb Readable SQLite database
     * @return The server ID if found, otherwise null.
     */
    public static Long fetchServerId(Long localContactId, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactsTable.fetchServerId() localContactId["
                + localContactId + "]");
        Long serverId = null;
        String[] args = {
            String.format("%d", localContactId)
        };
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.SERVERID + " FROM " + TABLE_NAME + " WHERE "
                    + Field.LOCALID + " = ?", args);
            if (c.moveToFirst() && !c.isNull(0)) {
                serverId = c.getLong(0);
            }

        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchServerId() "
                    + "Exception - Unable to validate contact ID", e);
            return null;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return serverId;
    }

    /**
     * Updates contact information. See {@link #fillUpdateData(Contact)} for
     * which fields from the Contact object are read.
     * 
     * @param contact The source contact object
     * @param writeableDb Writeable SQLite database
     * @return SUCCESS or a suitable error code
     * @note Fields in the given contact that are NULL will not be left
     *       unchanged
     */
    public static ServiceStatus modifyContact(Contact contact, SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "ContactsTable.modifyContact() contact["
                    + contact.localContactID + "]");
        }
        try {
            contact.deleted = null;
            String[] args = {
                String.format("%d", contact.localContactID)
            };
            if (writableDb.update(ContactsTable.TABLE_NAME, ContactsTable.fillUpdateData(contact),
                    Field.LOCALID + "=?", args) <= 0) {
                LogUtils.logE("ContactsTable.modifyContact() Unable to modify contact - not found");
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.modifyContact() SQLException - Unable to modify contact",
                    e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
    }

    /**
     * Fetches a contact from the database.
     * 
     * @param localContactId The primary key ID of the contact to find
     * @param contact A newly created contact object that will be populated by
     *            this function.
     * @param readableDb Readable SQLite database
     * @return SUCCESS, ERROR_NOT_FOUND or another suitable error code
     */
    public static ServiceStatus fetchContact(long localContactId, Contact contact,
            SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactsTable.fetchContact() localContactId["
                    + localContactId + "]");
        }
        String[] args = {
            String.format("%d", localContactId)
        };
        Cursor c = null;
        try {
            c = readableDb.rawQuery(ContactsTable.getQueryStringSql(ContactsTable.Field.LOCALID
                    + " = ?"), args);
            if (!c.moveToFirst()) {
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            ContactsTable.getQueryData(c, contact);
        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchContact() Unable to fetch contact");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(c);
            c = null;
        }

        return ServiceStatus.SUCCESS;
    }

    /**
     * Returns a complete list of server IDs from the Contacts table in
     * ascending order.
     * 
     * @param orderedServerIdList A list that will be populated with the ordered
     *            server IDs
     * @param readableDb Readable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus fetchContactServerIdList(ArrayList<Long> orderedServerIdList,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactsTable.fetchContactServerIdList()");
        Cursor c = null;
        try {
            orderedServerIdList.clear();
            c = readableDb.rawQuery("SELECT " + Field.SERVERID + " FROM " + TABLE_NAME + " WHERE "
                    + Field.SERVERID + " IS NOT NULL ORDER BY " + Field.SERVERID, null);
            while (c.moveToNext()) {
                if (!c.isNull(0)) {
                    orderedServerIdList.add(c.getLong(0));
                }
            }
            return ServiceStatus.SUCCESS;

        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchContactServerIdList() "
                    + "Exception - Unable to fetch contact server ID list", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }

    /**
     * Provides a statement that can be used to find a contact server ID in the
     * table
     * 
     * @param readableDb Readable SQLite database
     * @return The SQLite statement
     * @see #fetchLocalFromServerId(Long, SQLiteStatement)
     */
    public static SQLiteStatement fetchLocalFromServerIdStatement(SQLiteDatabase readableDb) {
        try {
            return readableDb.compileStatement("SELECT " + Field.LOCALID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.SERVERID + "=?");
        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.fetchLocalFromServerIdStatement() "
                    + "Exception - Compile error:\n", e);
            return null;
        }
    }

    /**
     * Searches the table for a contact server ID and if found, returns the
     * local ID
     * 
     * @param contactServerId The server ID
     * @param statement The statement returned by
     *            {@link #fetchLocalFromServerIdStatement(SQLiteDatabase)}
     * @return The local contact ID or NULL if it server ID was not found.
     * @see #fetchLocalFromServerIdStatement(SQLiteDatabase)
     */
    public static Long fetchLocalFromServerId(Long contactServerId, SQLiteStatement statement) {
        DatabaseHelper.trace(false, "ContactsTable.fetchLocalFromServerId() contactServerId["
                + contactServerId + "]");
        if (statement == null || contactServerId == null) {
            return null;
        }
        try {
            statement.bindLong(1, contactServerId);
            return statement.simpleQueryForLong();
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Updates the server and user IDs for a list of contacts. Also prepares a
     * list of duplicates which will be filled with the Ids for contacts already
     * present in the table (i.e. server ID has already been used). In the case
     * that a duplicate is found, the ContactIdInfo object will also include the
     * local ID of the original contact (see {@link ContactIdInfo#mergedLocalId}
     * ).
     * 
     * @param serverIdList A list of ServerIdInfo objects. For each object, the
     *            local ID must match a local contact ID in the table. The
     *            Server ID and User ID will be used for the update.
     * @param dupList On return this will be populated with a list of contacts
     *            which have server IDs already present in the table.
     * @param writeableDb Writeable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus syncSetServerIds(List<ServerIdInfo> serverIdList,
            List<ContactIdInfo> dupList, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactsTable.syncSetServerIds()");
        if (serverIdList.size() == 0) {
            return ServiceStatus.SUCCESS;
        }

        try {
            writableDb.beginTransaction();
            SQLiteStatement statement1 = null;
            SQLiteStatement statement2 = null;
            for (int i = 0; i < serverIdList.size(); i++) {
                final ServerIdInfo info = serverIdList.get(i);
                try {
                    if (info.serverId != null) {
                        if (info.userId == null) {
                            if (statement2 == null) {
                                statement2 = writableDb.compileStatement("UPDATE " + TABLE_NAME
                                        + " SET " + Field.SERVERID + "=? WHERE " + Field.LOCALID
                                        + "=?");
                            }
                            statement2.bindLong(1, info.serverId);
                            statement2.bindLong(2, info.localId);
                            statement2.execute();
                        } else {
                            if (statement1 == null) {
                                statement1 = writableDb.compileStatement("UPDATE " + TABLE_NAME
                                        + " SET " + Field.SERVERID + "=?," + Field.USERID
                                        + "=? WHERE " + Field.LOCALID + "=?");
                            }
                            statement1.bindLong(1, info.serverId);
                            statement1.bindLong(2, info.userId);
                            statement1.bindLong(3, info.localId);
                            statement1.execute();
                        }
                    }
                } catch (SQLiteConstraintException e) {
                    // server ID is not unique
                    ContactIdInfo contactInfo = new ContactIdInfo();
                    contactInfo.localId = info.localId;
                    contactInfo.serverId = info.serverId;

                    final SQLiteStatement contactFetchNativeIdStatement = ContactsTable
                            .fetchNativeFromLocalIdStatement(writableDb);
                    contactInfo.nativeId = ContactsTable.fetchNativeFromLocalId(info.localId,
                            contactFetchNativeIdStatement);

                    dupList.add(contactInfo);
                } catch (SQLException e) {
                    LogUtils.logE("ContactsTable.syncSetServerIds() SQLException - "
                            + "Unable to update contact server Ids", e);
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
            }
            writableDb.setTransactionSuccessful();
        } finally {
            writableDb.endTransaction();
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Updates the native IDs for a list of contacts.
     * 
     * @param contactIdList A list of ContactIdInfo objects. For each object,
     *            the local ID must match a local contact ID in the table. The
     *            Native ID will be used for the update. Other fields are
     *            unused.
     * @param writeableDb Writeable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus syncSetNativeIds(List<ContactIdInfo> contactIdList,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactsTable.syncSetNativeIds()");
        if (contactIdList.size() == 0) {
            return ServiceStatus.SUCCESS;
        }

        try {
            writableDb.beginTransaction();
            final SQLiteStatement statement1 = writableDb.compileStatement("UPDATE " + TABLE_NAME
                    + " SET " + Field.NATIVECONTACTID + "=? WHERE " + Field.LOCALID + "=?");
            for (int i = 0; i < contactIdList.size(); i++) {
                final ContactIdInfo info = contactIdList.get(i);
                try {
                    if (info.nativeId == null) {
                        statement1.bindNull(1);
                    } else {
                        statement1.bindLong(1, info.nativeId);
                    }
                    statement1.bindLong(2, info.localId);
                    statement1.execute();
                } catch (SQLException e) {
                    LogUtils.logE("ContactsTable.syncSetNativeIds() SQLException - "
                            + "Unable to update contact native Ids", e);
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
            }
            writableDb.setTransactionSuccessful();
        } finally {
            writableDb.endTransaction();
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Provides a statement that can be used to merge the native sync
     * information from one contact to another.
     * 
     * @param writeableDb Writeable SQLite database
     * @return The SQLite statement
     * @see #mergeContact(ContactIdInfo, SQLiteStatement)
     */
    /*
     * public static SQLiteStatement mergeContactStatement(SQLiteDatabase
     * writableDb) { if (Settings.ENABLED_DATABASE_TRACE) {
     * DatabaseHelper.trace(true, "ContactsTable.mergeContactStatement()"); }
     * try { return writableDb.compileStatement("UPDATE " + TABLE_NAME + " SET "
     * + Field.NATIVECONTACTID + "=? WHERE " + Field.LOCALID + "=?"); } catch
     * (SQLException e) {LogUtils.logE(
     * "ContactsTable.mergeContactStatement() SQLException - compile error:\n",
     * e); return null; } }
     */

    /**
     * Provides a statement that can be used to find a contact native ID in the
     * table
     * 
     * @param readableDb Readable SQLite database
     * @return The SQLite statement
     * @see #fetchLocalFromNativeId(Integer, SQLiteStatement)
     */
    public static SQLiteStatement fetchLocalFromNativeIdStatement(SQLiteDatabase readableDb) {
        try {
            return readableDb.compileStatement("SELECT " + Field.LOCALID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.NATIVECONTACTID + "=?");
        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.fetchLocalFromNativeIdStatement() "
                    + "Exception - Compile error:\n", e);
            return null;
        }
    }

    /**
     * Finds a native ID in the table
     * 
     * @param nativeContactId The native ID to find
     * @param statement The statement returned by
     *            {@link #fetchLocalFromNativeIdStatement(SQLiteDatabase)}.
     * @return The local contact ID if the contact is found, or NULL.
     * @see #fetchLocalFromNativeIdStatement(SQLiteDatabase)
     */
    public static Long fetchLocalFromNativeId(Integer nativeContactId, SQLiteStatement statement) {
        DatabaseHelper.trace(false, "ContactsTable.fetchLocalFromNativeId() nativeContactId["
                + nativeContactId + "]");
        if (statement == null || nativeContactId == null) {
            return null;
        }
        try {
            statement.bindLong(1, nativeContactId);
            return statement.simpleQueryForLong();
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Fetches the syncToPhone flag for a particular contact.
     * 
     * @param localContactId The primary key ID of the contact to find
     * @param readableDb Readable SQLite database
     * @return The state of the syncToPhone flag (true or false). Defaults to
     *         false if the contact is not found or a database error occurs.
     */
    protected static boolean fetchSyncToPhone(long localContactId, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactsTable.fetchSyncToPhone()");
        Cursor c = null;
        try {
            boolean syncToPhone = false;
            c = readableDb.rawQuery("SELECT " + Field.SYNCTOPHONE + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALID + "=" + localContactId, null);
            while (c.moveToNext()) {
                if (!c.isNull(0)) {
                    syncToPhone = (c.getInt(0) == 0 ? false : true);
                }
            }
            return syncToPhone;
        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchSyncToPhone() Exception - Unable to run query:\n", e);
            return false;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }

    /**
     * Provides a statement that can be used to find a contact native ID in the
     * table
     * 
     * @param readableDb Readable SQLite database
     * @return The SQLite statement
     * @see #fetchNativeFromLocalId(Long, SQLiteStatement)
     */
    private static SQLiteStatement fetchNativeFromLocalIdStatement(SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactsTable.fetchNativeFromLocalIdStatement()");
        try {
            return readableDb.compileStatement("SELECT " + Field.NATIVECONTACTID + " FROM "
                    + TABLE_NAME + " WHERE " + Field.LOCALID + "=?");
        } catch (SQLException e) {
            LogUtils.logE("ContactsTable.fetchNativeFromLocalIdStatement() "
                    + "Exception - Compile error:\n", e);
            return null;
        }
    }

    /**
     * Returns the native ID associated with a contact
     * 
     * @param localContactId The primary key ID of the contact to find
     * @param statement The statement provided by
     *            {@link #fetchNativeFromLocalIdStatement(SQLiteDatabase)}.
     * @return Native Contact ID or NULL if the contact was not found.
     * @see #fetchNativeFromLocalIdStatement(SQLiteDatabase)
     */
    private static Integer fetchNativeFromLocalId(Long localContactId, SQLiteStatement statement) {
        DatabaseHelper.trace(false, "ContactsTable.fetchNativeFromLocalId() localContactId["
                + localContactId + "]");
        if (statement == null || localContactId == null) {
            return null;
        }
        try {
            statement.bindLong(1, localContactId);
            return Long.valueOf(statement.simpleQueryForLong()).intValue();
        } catch (SQLException e) {
            return null;
        }
    }

    public static long fetchLocalIdFromUserId(Long userId, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactsTable.fetchSyncToPhone()");
        long localContactId = -1;
        Cursor c = null;
        try {
            StringBuffer query = StringBufferPool.getStringBuffer(SQLKeys.SELECT);
            query.append(Field.LOCALID).append(SQLKeys.FROM).append(TABLE_NAME).append(
                    SQLKeys.WHERE).append(Field.USERID).append(SQLKeys.EQUALS).append(userId);
            c = readableDb.rawQuery(StringBufferPool.toStringThenRelease(query), null);

            while (c.moveToNext()) {
                if (!c.isNull(0)) {
                    localContactId = c.getLong(0);
                }
            }

        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchSyncToPhone() Exception - Unable to run query:\n", e);
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return localContactId;

    }

    public static long fetchUserIdFromLocalContactId(Long localContactId, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactsTable.fetchSyncToPhone()");
        long userId = -1;
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.USERID + " FROM " + TABLE_NAME + " WHERE "
                    + Field.LOCALID + "=" + localContactId, null);
            while (c.moveToNext()) {
                if (!c.isNull(0)) {
                    userId = c.getLong(0);
                }
            }

        } catch (SQLiteException e) {
            LogUtils.logE("ContactsTable.fetchSyncToPhone() Exception - Unable to run query:\n", e);
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return userId;

    }

    /**
     * Selects all contacts from the contacts table
     * 
     * @param readableDb A readable Database
     * @return Cursor for the select
     */
    public static Cursor openContactsCursor(SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactSummeryTable.fetchContactList() ");
        }
        try {

            return readableDb.rawQuery("SELECT " + ContactsTable.getFullQueryList() + " FROM "
                    + ContactsTable.TABLE_NAME, null);

        } catch (SQLException e) {
            LogUtils.logE("ContactSummeryTable.fetchContactList() "
                    + "SQLException - Unable to fetch filtered summary cursor", e);
            return null;
        }
    }

    /**
     * Fetches a List with Contacts from Database Uses the persistence helper to
     * build the objects The Contactdetails are not read, only the contacts
     * 
     * @param readableDB A readable Database
     * @return java.util.list<Contact> with contacts
     */
    public static List<Contact> fetchContactList(SQLiteDatabase readableDB) {
        ArrayList<Contact> contactList = new ArrayList<Contact>();
        Cursor cursor = openContactsCursor(readableDB);
        if (null == cursor) {
            throw new RuntimeException("no cursor returned");
        }
        try {
            while (cursor.moveToNext()) {
                Contact contact = new Contact();

                PersistenceHelper.mapCursorToObject(contact, cursor);
                contactList.add(contact);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            cursor.close();
        }
        return contactList;
    }

    /**
     * SELECT NativeContactId FROM ContactsTable WHERE NativeContactId IS NOT
     * NULL ORDER BY NativeContactId
     */
    private final static String QUERY_NATIVE_CONTACTS_IDS = "SELECT " + Field.NATIVECONTACTID
            + " FROM " + TABLE_NAME + " WHERE " + Field.NATIVECONTACTID + " IS NOT NULL ORDER BY "
            + Field.NATIVECONTACTID;

    /**
     * Gets a list of native ids for the contacts.
     * 
     * @param readableDb the people database to query from
     * @return an ordered array containing all the found native ids, null if
     *         none
     */
    public static long[] getNativeContactsIds(SQLiteDatabase readableDb) {

        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactsTable.getNativeContactsIds()");
        }

        long[] ids;
        Cursor cursor = null;

        try {

            final int NATIVE_ID_INDEX = 0;

            cursor = readableDb.rawQuery(QUERY_NATIVE_CONTACTS_IDS, null);

            if (cursor.getCount() > 0) {

                int i = 0;
                ids = new long[cursor.getCount()];

                while (cursor.moveToNext()) {
                    ids[i++] = cursor.getInt(NATIVE_ID_INDEX);
                }
            } else {

                return null;
            }
        } catch (SQLException e) {

            return null;
        } finally {

            CloseUtils.close(cursor);
            cursor = null;
        }

        return ids;
    }

    /**
     * Gets the native related ContentValues for the provided ContactChange.
     * 
     * @param contactChange the ContactChange from which to extract the native
     *            values
     * @return the native ContentValues
     */
    public static ContentValues getNativeContentValues(ContactChange contactChange) {

        final ContentValues values = new ContentValues();
        final long nativeId = contactChange.getNabContactId();

        // add the native contact id
        if (nativeId != ContactChange.INVALID_ID) {
            values.put(Field.NATIVECONTACTID.toString(), nativeId);
        }

        // add the synctophone flag to true
        values.put(Field.SYNCTOPHONE.toString(), true);

        return values;
    }

    /**
     * Adds the provided contact.
     * 
     * @param contact the ContentValues representing the contact to add
     * @return the contact id if successful, -1 if the insertion failed
     */
    public static long addContact(ContentValues contact, SQLiteDatabase writeableDb) {

        long contactId = -1;

        try {

            contactId = writeableDb.insertOrThrow(TABLE_NAME, null, contact);
        } catch (Exception e) {

            return -1;
        }

        return contactId;
    }

    /**
     * SQL query string to get the contact native id from its local id.
     */
    private final static String QUERY_CONTACT_NATIVE_ID_FROM_LOCAL_ID = "SELECT "
            + Field.NATIVECONTACTID + " FROM " + TABLE_NAME + " WHERE " + Field.LOCALID + " = ?";

    /**
     * Gets the native contact id.
     * 
     * @param localContactId the local id of the contact
     * @param readDatabase the database to read from
     * @return the native contact id, -1 otherwise
     */
    public static long getNativeContactId(long localContactId, SQLiteDatabase readableDb) {

        final String[] args = {
            Long.toString(localContactId)
        };
        Cursor cursor = null;

        try {

            cursor = readableDb.rawQuery(QUERY_CONTACT_NATIVE_ID_FROM_LOCAL_ID, args);

            if (cursor.getCount() > 0) {

                cursor.moveToNext();
                if (!cursor.isNull(0)) {

                    return cursor.getLong(0);
                }
            }
        } catch (Exception e) {

            return -1;
        } finally {

            if (cursor != null) {

                cursor.close();
                cursor = null;
            }
        }

        return -1;
    }

    /**
     * String equal to "LocalId = ?" for SQL queries.
     */
    private final static String SQL_STRING_LOCAL_ID_EQUAL_QUESTION_MARK = Field.LOCALID + " = ?";

    /**
     * Sets the native contact id for a contact.
     * 
     * @param localContactId the local contact id
     * @param nativeContactId the native contact id to set
     * @param writableDb the database where to write
     * @return true if successful, false otherwise
     */
    public static boolean setNativeContactId(long localContactId, long nativeContactId,
            SQLiteDatabase writableDb) {

        final ContentValues values = new ContentValues();

        values.put(Field.NATIVECONTACTID.toString(), nativeContactId);

        try {

            if (writableDb.update(TABLE_NAME, values, SQL_STRING_LOCAL_ID_EQUAL_QUESTION_MARK,
                    new String[] {
                        Long.toString(localContactId)
                    }) == 1) {

                return true;
            }

        } catch (Exception e) {

            LogUtils.logE("ContactsTable.setNativeContactId() Exception - " + e);
        }

        return false;
    }
}
