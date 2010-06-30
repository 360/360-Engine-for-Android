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

import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.format.Time;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the contact change log database
 * table. This table is used to persist database changes that have not yet been
 * synced to the server. This class is never instantiated hence all methods must
 * be static.
 * 
 * @version %I%, %G%
 */
public abstract class ContactChangeLogTable {
    /**
     * Name of the table as it appears in the database
     */
    private static final String TABLE_NAME = "ContactChangeLog";

    /**
     * Name of a temporary table created and used to batch the deletion of sync
     * data from the change log table.
     */
    private static final String TEMP_CONTACT_CHANGE_TABLE = "SyncContactInfo";

    /**
     * Primary key for the temporary table
     */
    private static final String TEMP_CONTACT_CHANGE_TABLE_ID = "Id";

    /**
     * Secondary key which links to the primary key of the change log table
     */
    private static final String TEMP_CONTACT_CHANGE_LOG_ID = "LogId";

    /**
     * An enumeration of all the field names in the database.
     */
    private static enum Field {
        CONTACTCHANGEID("ContactChangeId"),
        CHANGETYPE("ChangeType"),
        LOCALCHANGECONTACTID("LocalChangeContactId"),
        SERVERCHANGECONTACTID("ServerChangeContactId"),
        LOCALCHANGEDETAILID("LocalChangeDetailId"),
        SERVERDETAILKEY("ServerDetailKey"),
        SERVERDETAILID("ServerDetailId"),
        ZYBGROUPORRELID("ZybGroupOrRelId"),
        TIMESTAMP("Timestamp");

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
     * Enumerates the type of changes supported by this table, including:
     * <ul>
     * <li>DELETE_CONTACT: A contact has been deleted from the database and
     * needs to be deleted on the server. The local Contact ID, server Contact
     * ID fields are required</li>
     * <li>DELETE_DETAIL: A contact detail has been deleted from the database
     * and needs to be deleted on the server. The local Contact ID, server
     * Contact ID, local Detail ID, server Detail ID (if the detail has one) and
     * detail key are required</li> </li>
     * <li>ADD_GROUP_REL: A contact has been associated with a group on the
     * client and needs to be synced on the server. The local Contact ID, server
     * Contact ID and group ID are required</li>
     * <li>DELETE_GROUP_REL: A contact has been disassociated with a group on
     * the client and needs to be synced on the server. The local Contact ID,
     * server Contact ID and group ID are required</li>
     * </ul>
     */
    public static enum ContactChangeType {
        DELETE_CONTACT,
        DELETE_DETAIL,
        ADD_GROUP_REL,
        DELETE_GROUP_REL
    }

    /**
     * Wraps up the data present in the change log table
     */
    public static class ContactChangeInfo {
        public Long mContactChangeId = null;

        public ContactChangeType mType = null;

        public Long mLocalContactId = null;

        public Long mServerContactId = null;

        public Long mLocalDetailId = null;

        public ContactDetail.DetailKeys mServerDetailKey = null;

        public Long mServerDetailId = null;

        public Long mGroupOrRelId = null;

        /**
         * Converts the encapsulated data into a string that can be displayed
         * for debug purposes.
         */
        @Override
        public String toString() {
            return "Contact Change ID: " + mContactChangeId + "\n" + "Contact Change Type: "
                    + mType + "\n" + "Local Contact ID: " + mLocalContactId + "\n"
                    + "Server Contact ID: " + mServerContactId + "\n" + "Local Detail ID: "
                    + mLocalDetailId + "\n" + "Detail Key: " + mServerDetailKey + "\n"
                    + "Server Detail ID: " + mServerDetailId + "\n" + "Group/Relation ID: "
                    + mGroupOrRelId;
        }
    }

    /**
     * Create Server Change Log Table.
     * 
     * @param writeableDb A writable SQLite database
     * @throws SQLException If an SQL compilation error occurs
     */
    public static void create(SQLiteDatabase writableDb) throws SQLException {
        DatabaseHelper.trace(true, "ContactChangeLogTable.create()");
        writableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.CONTACTCHANGEID
                + " INTEGER PRIMARY KEY, " + Field.CHANGETYPE + " INTEGER NOT NULL, "
                + Field.LOCALCHANGECONTACTID + " LONG NOT NULL, " + Field.SERVERCHANGECONTACTID
                + " LONG, " + Field.LOCALCHANGEDETAILID + " LONG, " + Field.SERVERDETAILID
                + " LONG, " + Field.SERVERDETAILKEY + " INTEGER, " + Field.ZYBGROUPORRELID
                + " LONG, " + Field.TIMESTAMP + " DATE);");
    }

    /**
     * Fetches a comma separated list of table fields which can be used in an
     * SQL SELECT statement as the query projection. The
     * {@link #getQueryData(Cursor)} method can used to fetch data from the
     * cursor.
     * 
     * @param whereClause A where clause (without the where) to filter the
     *            result. Cannot be null.
     * @return SQL string
     */
    private static String getQueryStringSql(String whereClause) {
        DatabaseHelper.trace(false, "ContactChangeLogTable.getQueryStringSql()");
        return "SELECT " + Field.CONTACTCHANGEID + ", " + Field.CHANGETYPE + ", "
                + Field.LOCALCHANGECONTACTID + ", " + Field.SERVERCHANGECONTACTID + ", "
                + Field.LOCALCHANGEDETAILID + ", " + Field.SERVERDETAILID + ", "
                + Field.SERVERDETAILKEY + ", " + Field.ZYBGROUPORRELID + ", " + Field.TIMESTAMP
                + " FROM " + TABLE_NAME + " WHERE " + whereClause;
    }

    /**
     * Column indices for database queries
     */
    private static final int CONTACTCHANGEID = 0;

    private static final int TYPE = 1;

    private static final int LOCALCONTACTID = 2;

    private static final int SERVERCONTACTID = 3;

    private static final int LOCALDETAILID = 4;

    private static final int SERVERDETAILID = 5;

    private static final int SERVERDETAILKEY = 6;

    private static final int GROUPORRELID = 7;

    /**
     * Fetches change log information from a cursor at the current position. The
     * {@link #getQueryStringSql(String)} method should be used to make the
     * query.
     * 
     * @param c The cursor from the query
     * @return A filled in ContactChangeInfo object
     */
    private static ContactChangeInfo getQueryData(Cursor c) {
        DatabaseHelper.trace(false, "ContactChangeLogTable.getQueryData()");
        ContactChangeInfo info = new ContactChangeInfo();

        if (!c.isNull(CONTACTCHANGEID)) {
            info.mContactChangeId = c.getLong(CONTACTCHANGEID);
        }
        if (!c.isNull(TYPE)) {
            final int typeIdx = c.getInt(TYPE);
            if (typeIdx < ContactChangeType.values().length) {
                info.mType = ContactChangeType.values()[typeIdx];
            }
        }
        if (!c.isNull(LOCALCONTACTID)) {
            info.mLocalContactId = c.getLong(LOCALCONTACTID);
        }
        if (!c.isNull(SERVERCONTACTID)) {
            info.mServerContactId = c.getLong(SERVERCONTACTID);
        }
        if (!c.isNull(LOCALDETAILID)) {
            info.mLocalDetailId = c.getLong(LOCALDETAILID);
        }
        if (!c.isNull(SERVERDETAILID)) {
            info.mServerDetailId = c.getLong(SERVERDETAILID);
        }
        if (!c.isNull(SERVERDETAILKEY)) {
            final int keyIdx = c.getInt(SERVERDETAILKEY);
            if (keyIdx < ContactDetail.DetailKeys.values().length) {
                info.mServerDetailKey = ContactDetail.DetailKeys.values()[keyIdx];
            }
        }
        if (!c.isNull(GROUPORRELID)) {
            info.mGroupOrRelId = c.getLong(GROUPORRELID);
        }
        // Ignore timestamp
        return info;
    }

    /**
     * Inserts "Add contact to group" operation into the change log.
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param serverContactId Server contact ID or null if the contact has not
     *            yet been synced.
     * @param groupId server group ID
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addGroupRel(long localContactId, Long serverContactId, Long groupId,
            SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false, "ContactChangeLogTable.addZybGroupRel() localContactId["
                    + localContactId + "] serverContactId[" + serverContactId + "] groupId["
                    + groupId + "]");
        if (groupId == null) {
            LogUtils.logE("ContactChangeLogTable.addGroupRel() Invalid parameter");
            return false;
        }
        if (isContactChangeInList(localContactId, ContactChangeType.DELETE_CONTACT, writableDb)) {
            LogUtils
                    .logE("ContactChangeLogTable.addGroupRel() Associated contact has already been deleted");
            return false;
        }
        ContactChangeInfo info = new ContactChangeInfo();
        info.mType = ContactChangeType.ADD_GROUP_REL;
        info.mLocalContactId = localContactId;
        info.mServerContactId = serverContactId;
        info.mGroupOrRelId = groupId;
        if (!addContactChange(info, writableDb)) {
            return false;
        }
        return true;
    }

    /**
     * Inserts "Remove contact from group" operation into the change log.
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param serverContactId Server contact ID or null if the contact has not
     *            yet been synced.
     * @param groupId server group ID
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean deleteGroupRel(long localContactId, Long serverContactId, Long groupId,
            SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false, "ContactChangeLogTable.deleteGroupRel() localContactId["
                    + localContactId + "] serverContactId[" + serverContactId + "] groupId["
                    + groupId + "]");
        if (groupId == null) {
            LogUtils.logE("ContactChangeLogTable.deleteGroupRel() Invalid parameter");
            return false;
        }
        if (isContactChangeInList(localContactId, ContactChangeType.DELETE_CONTACT, writableDb)) {
            LogUtils
                    .logE("ContactChangeLogTable.deleteGroupRel() Associated contact has already been deleted");
            return false;
        }
        Long addGroupId = findContactGroup(localContactId, groupId, writableDb);
        if (addGroupId != null) {
            return removeContactChange(addGroupId, writableDb);
        }
        if (isContactChangeInList(localContactId, ContactChangeType.ADD_GROUP_REL, writableDb)) {
            removeContactChanges(localContactId, writableDb);
            return true;
        }
        ContactChangeInfo info = new ContactChangeInfo();
        info.mType = ContactChangeType.DELETE_GROUP_REL;
        info.mLocalContactId = localContactId;
        info.mServerContactId = serverContactId;
        info.mGroupOrRelId = groupId;
        if (!addContactChange(info, writableDb)) {
            return false;
        }
        return true;
    }

    /**
     * Inserts "Delete Contact" operation into the change log.
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param serverContactId Server contact ID. If this is null it is assumed
     *            that the contact hasn't yet been synced with the server, so
     *            the new contact change is removed from the log and the
     *            function returns.
     * @param addToLog False - removes all changes associated with the contact
     *            from the change log True - to also insert deletion in change
     *            log.
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addDeletedContactChange(Long localContactId, Long serverContactId,
            boolean addToLog, SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false,
                    "ContactChangeLogTable.addDeletedContactChange() localContactId["
                            + localContactId + "] serverContactId[" + serverContactId
                            + "] addToLog[" + addToLog + "]");
        if (localContactId == null) {
            LogUtils.logE("ContactChangeLogTable.addDeletedContactChange() Invalid parameter");
            return false;
        }
        if (addToLog) {
            if (serverContactId == null) {
                addToLog = false;
            }
        }
        removeContactChanges(localContactId, writableDb);

        if (!addToLog) {
            return true;
        }
        ContactChangeInfo info = new ContactChangeInfo();
        info.mType = ContactChangeType.DELETE_CONTACT;
        info.mLocalContactId = localContactId;
        info.mServerContactId = serverContactId;
        if (!addContactChange(info, writableDb)) {
            return false;
        }
        return true;
    }

    /**
     * Inserts "Delete Contact Detail" operation into the change log.
     * 
     * @param detail Must have a valid local contact ID, local detail ID and
     *            key. Will not be added to the change log if the contact server
     *            ID is null.
     * @param addToLog False - removes all changes associated with the detail
     *            from the change log True - to also insert deletion in change
     *            log.
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addDeletedContactDetailChange(ContactDetail detail, boolean addToLog,
            SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false,
                    "ContactChangeLogTable.addDeletedContactDetailChange() addToLog[" + addToLog
                            + "]");

        if (detail.localContactID == null || detail.localDetailID == null || detail.key == null) {
            LogUtils
                    .logE("ContactChangeLogTable.addDeletedContactDetailChange() Invalid parameter");
            return false;
        }
        if (addToLog) {
            if (isContactChangeInList(detail.localContactID, ContactChangeType.DELETE_CONTACT,
                    writableDb)) {
                LogUtils
                        .logE("ContactChangeLogTable.addDeletedContactDetailChange() Associated contact has already been deleted");
                return false;
            }
            if (isContactDetailChangeInList(detail.localDetailID, ContactChangeType.DELETE_DETAIL,
                    writableDb)) {
                LogUtils
                        .logE("ContactChangeLogTable.addDeletedContactDetailChange() Associated contact detail has already been deleted");
                return false;
            }
        }
        removeContactDetailChanges(detail.localDetailID, writableDb);
        if (!addToLog || detail.serverContactId == null) {
            return true;
        }
        ContactChangeInfo info = new ContactChangeInfo();
        info.mLocalContactId = detail.localContactID;
        info.mServerContactId = detail.serverContactId;
        info.mLocalDetailId = detail.localDetailID;
        info.mServerDetailKey = detail.key;
        info.mServerDetailId = detail.unique_id;
        info.mType = ContactChangeType.DELETE_DETAIL;
        if (!addContactChange(info, writableDb)) {
            return false;
        }
        return true;
    }

    /**
     * Inserts a contact change into the table.
     * 
     * @param info contact change info. This must have a valid type field and
     *            depending on type other fields may also be required (see
     *            {@link ContactChangeType}).
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    private static boolean addContactChange(ContactChangeInfo info, SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(true, "ContactChangeLogTable.addContactChange()");
        try {
            ContentValues changeValues = new ContentValues();
            changeValues.put(Field.CHANGETYPE.toString(), info.mType.ordinal());
            changeValues.put(Field.LOCALCHANGECONTACTID.toString(), info.mLocalContactId);
            if (info.mServerContactId != null) {
                changeValues.put(Field.SERVERCHANGECONTACTID.toString(), info.mServerContactId);
            }
            if (info.mLocalDetailId != null) {
                changeValues.put(Field.LOCALCHANGEDETAILID.toString(), info.mLocalDetailId);
            }
            if (info.mGroupOrRelId != null) {
                changeValues.put(Field.ZYBGROUPORRELID.toString(), info.mGroupOrRelId);
            }
            if (info.mServerDetailKey != null) {
                changeValues.put(Field.SERVERDETAILKEY.toString(), info.mServerDetailKey.ordinal());
            }
            if (info.mServerDetailId != null) {
                changeValues.put(Field.SERVERDETAILID.toString(), info.mServerDetailId);
            }
            Time time = new Time();
            time.setToNow();
            changeValues.put(Field.TIMESTAMP.toString(), time.format2445());
            long id = writableDb.insertOrThrow(TABLE_NAME, null, changeValues);
            if (id < 0) {
                LogUtils
                        .logE("ContactChangeLogTable.addContactChange() Unable to add contact change to log table - a database error has occurred");
                return false;
            }
            info.mContactChangeId = id;
            return true;

        } catch (SQLException e) {
            LogUtils.logE("ContactChangeLogTable.addContactChange() SQLException"
                    + "- Unable to add contact change to log table", e);
            return false;
        }
    }

    /**
     * Determines if a specific contact and change type exists in the change
     * log.
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param type The change type to find
     * @param readableDb Readable SQLite database
     * @return true if the change is found, false otherwise
     */
    private static boolean isContactChangeInList(Long localContactId, ContactChangeType type,
            SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false,
                    "ContactChangeLogTable.isContactChangeInList() localContactId["
                            + localContactId + "]");
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.LOCALCHANGECONTACTID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALCHANGECONTACTID + "=" + localContactId + " AND "
                    + Field.CHANGETYPE + "=" + type.ordinal(), null);
            if (c.moveToFirst()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        } finally {
            CloseUtils.close(c);
        }
    }

    /**
     * Determines if a specific contact detail and change type exists in the
     * change log.
     * 
     * @param localDetailId Local contact detail ID from ContactDetail table
     * @param type The change type to find
     * @param readableDb Readable SQLite database
     * @return true if the change is found, false otherwise
     */
    private static boolean isContactDetailChangeInList(Long localDetailId, ContactChangeType type,
            SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false,
                    "ContactChangeLogTable.isContactDetailChangeInList() localDetailId["
                            + localDetailId + "]");
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.LOCALCHANGEDETAILID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALCHANGEDETAILID + "=" + localDetailId + " AND "
                    + Field.CHANGETYPE + "=" + type.ordinal(), null);
            if (c.moveToFirst()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        } finally {
            CloseUtils.close(c);
        }
    }

    /**
     * Searches the change log for an association between contact and group
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param groupId Server group ID
     * @param readableDb Readable SQLite database
     * @return Contact Change ID (primary key) of the record found, or NULL if
     *         no such logs exist in the table.
     */
    private static Long findContactGroup(long localContactId, long groupId,
            SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false, "ContactChangeLogTable.findAddGroup() localContactId["
                    + localContactId + "] groupId[" + groupId + "]");
        Cursor c = null;
        try {
            Long id = null;
            c = readableDb.rawQuery("SELECT " + Field.CONTACTCHANGEID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALCHANGECONTACTID + "=" + localContactId + " AND "
                    + Field.ZYBGROUPORRELID + "=" + groupId + " AND " + Field.CHANGETYPE + "="
                    + ContactChangeType.ADD_GROUP_REL.ordinal(), null);
            if (c.moveToFirst()) {
                id = c.getLong(0);
            }

            return id;
        } catch (SQLException e) {
            return null;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }

    /**
     * Removes all changes from the change log associated with a particular
     * contact.
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    private static boolean removeContactChanges(Long localContactId, SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(true,
                    "ContactChangeLogTable.removeContactChanges() localContactId[" + localContactId
                            + "]");
        try {
            int result = writableDb.delete(TABLE_NAME, Field.LOCALCHANGECONTACTID + "="
                    + localContactId, null);
            if (result <= 0) {
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Deletes a specific contact change record from the table
     * 
     * @param ContactChangeId The primary ID identifying the record
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    private static boolean removeContactChange(long ContactChangeId, SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(true,
                    "ContactChangeLogTable.removeContactChange() ContactChangeId["
                            + ContactChangeId + "]");
        try {
            int result = writableDb.delete(TABLE_NAME, Field.CONTACTCHANGEID + "="
                    + ContactChangeId, null);
            if (result <= 0) {
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Removes all changes from the change log associated with a particular
     * contact detail.
     * 
     * @param localDetailId Local contact detail ID from the Contact details
     *            table
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    private static boolean removeContactDetailChanges(Long localDetailId, SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(true,
                    "ContactChangeLogTable.removeContactDetailChanges() localDetailId["
                            + localDetailId + "]");
        try {
            if (writableDb
                    .delete(TABLE_NAME, Field.LOCALCHANGEDETAILID + "=" + localDetailId, null) < 0) {
                return false;
            }
            // We 0 rows are deleted we still should return success
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Fetches the number of changes listed in the table.
     *
     * @param type The ContactChangeType to count, if NULL then all change types
     *            are included.
     * @param readableDb Readable SQLite database
     * @return Total number of records
     */
    public static int fetchNoOfContactDetailChanges(
            final ContactChangeType type, final SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactChangeLogTable."
                    + "fetchNoOfContactDetailChanges()");
        }
        Cursor cursor = null;
        try {
            String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
            if (type != null) {
                query += " WHERE " + Field.CHANGETYPE.toString() + "="
                    + type.ordinal();
            }
            cursor = readableDb.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                LogUtils.logE("ContactChangeLogTable."
                    + "fetchNoOfContactDetailChanges() COUNT(*) "
                    + "should not return an empty cursor, returning 0");
                return 0;
            }

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /**
     * Removes a list of changes from the change log table. This method is used
     * once the server has been successfully synced with the client.
     * 
     * @param changeInfoList A list of {@link ContactChangeInfo} objects. Note
     *            that only the mContactChangeId field of this object is
     *            actually used. The object is passed in as a convenience.
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean deleteContactChanges(List<ContactChangeInfo> changeInfoList,
            SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false,
                    "ContactChangeLogTable.deleteContactChanges() Change Log List for deletion:");
            for (ContactChangeLogTable.ContactChangeInfo info : changeInfoList) {
                DatabaseHelper.trace(false,
                        "ContactChangeLogTable.deleteContactChanges() mContactChangeId["
                                + info.mContactChangeId + "]");
            }
        }
        try {
            writableDb.beginTransaction();
            writableDb.execSQL("DROP TABLE IF EXISTS " + TEMP_CONTACT_CHANGE_TABLE);
            writableDb.execSQL("CREATE TEMPORARY TABLE " + TEMP_CONTACT_CHANGE_TABLE + "("
                    + TEMP_CONTACT_CHANGE_TABLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + TEMP_CONTACT_CHANGE_LOG_ID + " LONG);");
            ContentValues values = new ContentValues();
            for (ContactChangeInfo info : changeInfoList) {
                values.put(TEMP_CONTACT_CHANGE_LOG_ID, info.mContactChangeId);
                if (writableDb.insertOrThrow(TEMP_CONTACT_CHANGE_TABLE, null, values) < 0) {
                    return false;
                }
            }
            writableDb.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + Field.CONTACTCHANGEID
                    + " IN (SELECT " + TEMP_CONTACT_CHANGE_LOG_ID + " FROM "
                    + TEMP_CONTACT_CHANGE_TABLE + ");");
            writableDb.setTransactionSuccessful();
            return true;
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactChangeLogTable.deleteContactChanges() SQLException - Unable to remove contact detail change from log",
                            e);
            
            return false;
        } finally {
            writableDb.endTransaction();
        }
    }

    /**
     * Fetches a list of changes from the table for a specific type. The list is
     * ordered by local contact ID.
     * 
     * @param contactChangeList The list to be populated
     * @param type The type of change to return
     * @param firstIndex An index of the first record to return (0 based)
     * @param count The number of records to return (or -1 to return all)
     * @param readableDb Readable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean fetchContactChangeLog(List<ContactChangeInfo> contactChangeList,
            ContactChangeType type, long firstIndex, long count, SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false, "ContactChangeLogTable.fetchContactChangeLog() "
                    + "firstIndex[" + firstIndex + "] count[" + count + "]");
        Cursor c1 = null;
        try {
            c1 = readableDb.rawQuery(getQueryStringSql(Field.CHANGETYPE + "=" + type.ordinal()
                    + " ORDER BY " + Field.LOCALCHANGECONTACTID + " LIMIT " + firstIndex + ","
                    + count), null);
            contactChangeList.clear();
            while (c1.moveToNext()) {
                contactChangeList.add(getQueryData(c1));
            }
        } catch (SQLiteException e) {
            LogUtils
                    .logE(
                            "ContactChangeLogTable.fetchContactChangeLog() throw e; - Unable to fetch main contact change log",
                            e);
            return false;
        } finally {
            CloseUtils.close(c1);
        }
        return true;
    }

    /**
     * Fetches a list of changes on the contact details for the me-profile.
     * 
     * @param changeList The list to populate with contact changes.
     * @param type The type of change to fetch from the contact.
     * @param readableDb The database to read from.
     * @param meProfileServerId The server id of the me-profile.
     * @return True if fetching succeeded, false otherwise.
     */
    public static boolean fetchMeProfileChangeLog(List<ContactChangeInfo> changeList,
            ContactChangeType type, SQLiteDatabase readableDb, Long meProfileServerId) {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false, "ContactChangeLogTable.fetchMeProfileChangeLog()");

        Cursor c1 = null;
        try {
            c1 = readableDb.rawQuery(getQueryStringSql(Field.CHANGETYPE + "=" + type.ordinal()
                    + " AND " + Field.SERVERCHANGECONTACTID + "=" + meProfileServerId.longValue()),
                    null);

            while (c1.moveToNext()) {
                changeList.add(getQueryData(c1));
            }
        } catch (SQLiteException e) {
            LogUtils.logE("ContactChangeLogTable.fetchMeProfileChangeLog() throw e; - "
                    + "Unable to fetch main contact change log", e);
            return false;
        } finally {
            CloseUtils.close(c1);
        }

        return true;
    }

}
