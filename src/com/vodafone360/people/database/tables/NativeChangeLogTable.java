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
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the native contact change log
 * database table. This table is used to persist database changes that have not
 * yet been synced to the native Android phonebook. This class is never
 * instantiated hence all methods must be static.
 * 
 * @version %I%, %G%
 */
public abstract class NativeChangeLogTable {
    /**
     * Name of the table as it appears in the database
     */
    private static final String TABLE_NAME = "NativeChangeLog";

    /**
     * Name of a temporary table created and used to batch the deletion of sync
     * data from the change log table.
     */
    private static final String TEMP_CONTACT_CHANGE_TABLE = "SyncNativeInfo";

    /**
     * Primary key for the temporary table
     */
    private static final String TEMP_CONTACT_CHANGE_TABLE_ID = "Id";

    /**
     * Secondary key which links to the primary key of the change log table
     */
    private static final String TEMP_CONTACT_CHANGE_LOG_ID = "LogId";
    
    /**
     * Query string to get a list of deleted details for a provided type and local id.
     * 
     * @see #getDeletedDetails(long, SQLiteDatabase)
     */
    private final static String QUERY_DELETED_DETAILS = 
        "SELECT " + Field.CHANGETYPE + ", " + Field.LOCALCONTACTID + ", " + Field.NATIVECONTACTID +
        ", " + Field.LOCALDETAILID + ", " + Field.NATIVEDETAILID + ", " + Field.DETAILKEY + 
        " FROM " + TABLE_NAME + " WHERE " + Field.LOCALCONTACTID + " = ? AND " + Field.CHANGETYPE + " = ?";

    /**
     * An enumeration of all the field names in the database.
     */
    private static enum Field {
        NATIVECHANGEID("NativeChangeId"),
        CHANGETYPE("ChangeType"),
        LOCALCONTACTID("LocalContactId"),
        NATIVECONTACTID("NativeContactId"),
        LOCALDETAILID("LocalDetailId"),
        NATIVEDETAILID("NativeDetailId"),
        DETAILKEY("DetailKey"),
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
     * needs to be deleted in the native. The local Contact ID, native Contact
     * ID fields are required</li>
     * <li>DELETE_DETAIL: A contact detail has been deleted from the database
     * and needs to be deleted from the native. The local Contact ID, native
     * Contact ID, local Detail ID, native Detail ID (if the detail has one) and
     * detail key are required</li> </li>
     * </ul>
     */
    public static enum ContactChangeType {
        DELETE_CONTACT,
        DELETE_DETAIL
    }

    /**
     * Wraps up the data present in the change log table
     */
    public static class ContactChangeInfo {
        public Long mNativeChangeId = null;

        public ContactChangeType mType = null;

        public Long mLocalContactId = null;

        public Integer mNativeContactId = null;

        public Long mLocalDetailId = null;

        public Integer mNativeDetailId = null;

        public ContactDetail.DetailKeys mDetailKey = null;

        /**
         * Converts the encapsulated data into a string that can be displayed
         * for debug purposes.
         */
        @Override
        public String toString() {
            return "Contact Change ID: " + mNativeChangeId + "\n" + "Contact Change Type: " + mType
                    + "\n" + "Local Contact ID: " + mLocalContactId + "\n" + "Native Contact ID: "
                    + mNativeContactId + "\n" + "Local Detail ID: " + mLocalDetailId;
        }
    }

    /**
     * Create Server Change Log Table.
     * 
     * @param writeableDb A writable SQLite database
     * @throws SQLException If an SQL compilation error occurs
     */
    public static void create(SQLiteDatabase writableDb) throws SQLException {
        DatabaseHelper.trace(true, "NativeChangeLogTable.create()");
        writableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.NATIVECHANGEID
                + " INTEGER PRIMARY KEY, " + Field.CHANGETYPE + " INTEGER NOT NULL, "
                + Field.LOCALCONTACTID + " LONG NOT NULL, " + Field.NATIVECONTACTID + " LONG, "
                + Field.LOCALDETAILID + " LONG, " + Field.NATIVEDETAILID + " INTEGER, "
                + Field.DETAILKEY + " INTEGER, " + Field.TIMESTAMP + " DATE);");
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
        DatabaseHelper.trace(false, "NativeChangeLogTable.getQueryStringSql()");
        String whereClauseFull = "";
        if (whereClause != null) {
            whereClauseFull = " WHERE " + whereClause;
        }
        return "SELECT " + Field.NATIVECHANGEID + ", " + Field.CHANGETYPE + ", "
                + Field.LOCALCONTACTID + ", " + Field.NATIVECONTACTID + ", " + Field.LOCALDETAILID
                + ", " + Field.NATIVEDETAILID + ", " + Field.DETAILKEY + ", " + Field.TIMESTAMP
                + " FROM " + TABLE_NAME + whereClauseFull;
    }

    /**
     * Column indices for database queries
     */
    private static final int NATIVECHANGEID = 0;

    private static final int TYPE = 1;

    private static final int LOCALCONTACTID = 2;

    private static final int NATIVECONTACTID = 3;

    private static final int LOCALDETAILID = 4;

    private static final int NATIVEDETAILID = 5;

    private static final int DETAILKEY = 6;

    /**
     * Fetches change log information from a cursor at the current position. The
     * {@link #getQueryStringSql(String)} method should be used to make the
     * query.
     * 
     * @param c The cursor from the query
     * @return A filled in ContactChangeInfo object
     */
    public static ContactChangeInfo getQueryData(Cursor c) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.getQueryData()");
        ContactChangeInfo info = new ContactChangeInfo();
        if (!c.isNull(NATIVECHANGEID)) {
            info.mNativeChangeId = c.getLong(NATIVECHANGEID);
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
        if (!c.isNull(NATIVECONTACTID)) {
            info.mNativeContactId = c.getInt(NATIVECONTACTID);
        }
        if (!c.isNull(LOCALDETAILID)) {
            info.mLocalDetailId = c.getLong(LOCALDETAILID);
        }
        if (!c.isNull(NATIVEDETAILID)) {
            info.mNativeDetailId = c.getInt(NATIVEDETAILID);
        }
        if (!c.isNull(DETAILKEY)) {
            final int keyIdx = c.getInt(DETAILKEY);
            if (keyIdx < ContactDetail.DetailKeys.values().length) {
                info.mDetailKey = ContactDetail.DetailKeys.values()[keyIdx];
            }
        }
        // Ignore timestamp
        return info;
    }

    /**
     * Inserts "Delete Contact" operation into the change log.
     * 
     * @param localContactId Local contact ID from the Contacts table
     * @param nativeContactId Native contact ID. If this is null it is assumed
     *            that the contact hasn't yet been synced with the native
     *            phonebook, so the new contact change is removed from the log
     *            and the function returns.
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addDeletedContactChange(Long localContactId, Integer nativeContactId,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.addDeletedContactChange()");
        if (localContactId == null) {
            LogUtils.logE("NativeChangeLogTable.addDeletedContactChange() Invalid parameter");
            return false;
        }
        boolean addToLog = true;
        if (nativeContactId == null || nativeContactId == -1 || nativeContactId == 0) {
            // FIXME: invalid native id, no need to add it to the table. Why is this happening???
            addToLog = false;
        }
        removeContactChanges(localContactId, writableDb);
        if (!addToLog) {
            return true;
        }

        ContactChangeInfo info = new ContactChangeInfo();
        info.mType = ContactChangeType.DELETE_CONTACT;
        info.mLocalContactId = localContactId;
        info.mNativeContactId = nativeContactId;
        if (!addContactChange(info, writableDb)) {
            return false;
        }
        return true;
    }

    /**
     * Inserts "Delete Contact Detail" operation into the change log.
     * 
     * @param detail Must have a valid local contact ID, local detail ID and
     *            key. Will not be added to the change log if the contact native
     *            ID is null.
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addDeletedContactDetailChange(ContactDetail detail,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.addDeletedContactDetailChange()");

        if (detail.localContactID == null || detail.localDetailID == null || detail.key == null) {
            LogUtils.logE("NativeChangeLogTable.addDeletedContactDetailChange() Invalid parameter");
            return false;
        }
        if (isContactChangeInList(detail.localContactID, ContactChangeType.DELETE_CONTACT,
                writableDb)) {
            LogUtils
                    .logE("NativeChangeLogTable.addDeletedContactDetailChange() Associated contact has already been deleted (#1)");
            return false;
        }
        if (isContactDetailChangeInList(detail.localDetailID, ContactChangeType.DELETE_DETAIL,
                writableDb)) {
            LogUtils
                    .logE("NativeChangeLogTable.addDeletedContactDetailChange() Associated contact detail has already been deleted (#2)");
            return false;
        }
        removeContactDetailChanges(detail.localDetailID, writableDb);
        ContactChangeInfo info = new ContactChangeInfo();
        info.mLocalContactId = detail.localContactID;
        info.mNativeContactId = detail.nativeContactId;
        info.mLocalDetailId = detail.localDetailID;
        info.mType = ContactChangeType.DELETE_DETAIL;
        info.mDetailKey = detail.key;
        info.mNativeDetailId = detail.nativeDetailId;

        if (info.mNativeContactId == null || info.mNativeContactId == -1) {

            // FIXME: this shall not happen (has been potentially fixed) but currently guarding against it...
            // This line will be removed later when the root cause is completely identified
            LogUtils.logE("==========================================================================================================================");
            LogUtils.logE("NativeChangeLogTable.addDeletedContactDetailChange(): a detail without a native contact id can't be added to the table!!!!");
            LogUtils.logE("==========================================================================================================================");
            return true;
        }

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
        DatabaseHelper.trace(true, "NativeChangeLogTable.addContactChange()");
        try {
            ContentValues changeValues = new ContentValues();
            changeValues.put(Field.CHANGETYPE.toString(), info.mType.ordinal());
            changeValues.put(Field.LOCALCONTACTID.toString(), info.mLocalContactId);
            if (info.mDetailKey != null) {
                changeValues.put(Field.DETAILKEY.toString(), info.mDetailKey.ordinal());
            }
            if (info.mNativeContactId != null) {
                changeValues.put(Field.NATIVECONTACTID.toString(), info.mNativeContactId);
            }
            if (info.mNativeDetailId != null) {
                changeValues.put(Field.NATIVEDETAILID.toString(), info.mNativeDetailId);
            }
            if (info.mLocalDetailId != null) {
                changeValues.put(Field.LOCALDETAILID.toString(), info.mLocalDetailId);
            }
            Time time = new Time();
            time.setToNow();
            changeValues.put(Field.TIMESTAMP.toString(), time.format2445());
            long id = writableDb.insertOrThrow(TABLE_NAME, null, changeValues);
            if (id < 0) {
                LogUtils
                        .logE("NativeChangeLogTable.addContactChange() Unable to add contact change to log table - a database error has occurred");
                return false;
            }
            info.mNativeChangeId = id;
            return true;
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "NativeChangeLogTable.addContactChange() SQLException - Unable to add contact change to log table",
                            e);
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
    public static boolean isContactChangeInList(Long localContactId, ContactChangeType type,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.isContactChangeInList()");
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.LOCALCONTACTID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALCONTACTID + "=" + localContactId + " AND "
                    + Field.CHANGETYPE + "=" + type.ordinal(), null);
            if (c.moveToFirst()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }
    
    /**
     * 
     * @param nativeContactId
     * @param type
     * @param readableDb
     * @return
     */
    public static boolean isContactChangeInList(long nativeContactId, ContactChangeType type,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.isContactChangeInList()");
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.LOCALCONTACTID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.NATIVECONTACTID + "=" + nativeContactId + " AND "
                    + Field.CHANGETYPE + "=" + type.ordinal(), null);
            if (c.moveToFirst()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
        } finally {
            CloseUtils.close(c);
            c = null;
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
        DatabaseHelper.trace(false, "NativeChangeLogTable.isContactDetailChangeInList()");
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.LOCALDETAILID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALDETAILID + "=" + localDetailId + " AND "
                    + Field.CHANGETYPE + "=" + type.ordinal(), null);
            if (c.moveToFirst()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            return false;
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
    public static boolean removeContactChanges(Long localContactId, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "NativeChangeLogTable.removeContactChanges()");
        try {
            int result = writableDb.delete(TABLE_NAME, Field.LOCALCONTACTID + "=" + localContactId,
                    null);
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
    public static boolean removeContactDetailChanges(Long localDetailId, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "NativeChangeLogTable.removeContactDetailChanges()");
        try {
            if (writableDb.delete(TABLE_NAME, Field.LOCALDETAILID + "=" + localDetailId, null) < 0) {
                return false;
            }
            // When 0 rows are deleted we still should return success
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Fetches the number of changes listed in the table.
     * 
     * @param type The type of change to count, if null all change types are
     *            included
     * @param readableDb Readable SQLite database
     * @return The number of records
     */
    public static long fetchNoOfChanges(ContactChangeType type, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.fetchNoOfChanges()");
        long noOfChanges = 0;
        Cursor c = null;
        try {
            String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
            if (type != null) {
                query += " WHERE " + Field.CHANGETYPE.toString() + "=" + type.ordinal();
            }
            c = readableDb.rawQuery(query, null);
            if (c.moveToFirst()) {
                noOfChanges = c.getLong(0);
            }
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "NativeChangeLogTable.fetchNoOfChanges() SQLException - Unable to fetch changes",
                            e);
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return noOfChanges;
    }

    /**
     * Removes a list of changes from the change log table. This method is used
     * once the native has been successfully updated by the client.
     * 
     * @param changeIdList A list of contact change IDs (can be obtained from
     *            {@link ContactChangeInfo#mNativeChangeId}.
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean syncDeleteNativeChangeLog(List<Long> changeIdList,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "NativeChangeLogTable.syncDeleteNativeChangeLog()");
        
        try {
            writableDb.beginTransaction();
            writableDb.execSQL("DROP TABLE IF EXISTS " + TEMP_CONTACT_CHANGE_TABLE);
            writableDb.execSQL("CREATE TEMPORARY TABLE " + TEMP_CONTACT_CHANGE_TABLE + "("
                    + TEMP_CONTACT_CHANGE_TABLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + TEMP_CONTACT_CHANGE_LOG_ID + " LONG);");
            ContentValues values = new ContentValues();
            for (Long changeId : changeIdList) {
                values.put(TEMP_CONTACT_CHANGE_LOG_ID, changeId);
                if (writableDb.insertOrThrow(TEMP_CONTACT_CHANGE_TABLE, null, values) < 0) {
                    return false;
                }
            }
            writableDb.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + Field.NATIVECHANGEID
                    + " IN (SELECT " + TEMP_CONTACT_CHANGE_LOG_ID + " FROM "
                    + TEMP_CONTACT_CHANGE_TABLE + ");");
            writableDb.setTransactionSuccessful();
            return true;
        } catch (SQLException e) {
            LogUtils.logE("NativeChangeLogTable.syncDeleteNativeChangeLog() "
                    + "SQLException - Unable to remove contact detail change from log", e);
            return false;
        } finally {
            writableDb.endTransaction();
        }
    }

    /**
     * Fetches a list of changes from the table for a specific type. The list is
     * ordered by local contact ID.
     * 
     * @param type The type of change to return (cannot be null)
     * @param readableDb Readable SQLite database
     * @return A cursor for use with {@link #getQueryData(Cursor)} if
     *         successful, null otherwise
     */
    public static Cursor fetchContactChangeLogCursor(ContactChangeType type,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.fetchContactChangeLogCursor()");
        try {
            return readableDb.rawQuery(getQueryStringSql(Field.CHANGETYPE + "=" + type.ordinal()
                    + " ORDER BY " + Field.LOCALCONTACTID), null);
        } catch (SQLiteException e) {
            LogUtils.logE("NativeChangeLogTable.fetchContactChangeLogCursor() "
                    + "Exception - Unable to fetch native change log cursor", e);
            return null;
        }
    }

    /**
     * Fetches a list of changes from the table for a specific type. The list is
     * ordered by local contact ID.
     * 
     * @param type The type of change to return
     * @param contactChangeList The list to be populated
     * @param readableDb Readable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean fetchContactChangeLog(ContactChangeType type,
            List<ContactChangeInfo> nativeInfoList, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "NativeChangeLogTable.fetchContactChangeLog()");
        Cursor c = null;
        try {
            nativeInfoList.clear();
            String whereClause = null;
            if (type != null) {
                whereClause = Field.CHANGETYPE + "=" + type.ordinal();
            }

            c = readableDb.rawQuery(getQueryStringSql(whereClause) + " ORDER BY "
                    + Field.NATIVECONTACTID, null);
            while (c.moveToNext()) {
                nativeInfoList.add(getQueryData(c));
            }
            return true;
        } catch (SQLiteException e) {
            LogUtils.logE("NativeChangeLogTable.fetchContactChangeLog() "
                    + "Exception - Unable to fetch native change log", e);
            return false;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }
    
    /**
     * SELECT NativeContactId FROM NativeChangeLog WHERE ChangeType = 0 ORDER BY NativeContactId
     */
    public final static String QUERY_MODIFIED_CONTACTS_LOCAL_IDS = "SELECT DISTINCT " + Field.LOCALCONTACTID + " FROM " + TABLE_NAME
                                                                  + " ORDER BY " + Field.LOCALCONTACTID;
    /**
     * 
     */
    public final static String QUERY_MODIFIED_CONTACTS_LOCAL_IDS_NO_ORDERBY = "SELECT " + Field.LOCALCONTACTID + " FROM " + TABLE_NAME;
    
    /**
     * 
     * @param readableDb
     * @return
     */
    public static long[] getModifiedContactsLocalIds(SQLiteDatabase readableDb) {
        
        long[] ids = null;
        Cursor cursor = null;
        
        try {
            
            final int LOCAL_ID_INDEX = 0;
            
            cursor = readableDb.rawQuery(QUERY_MODIFIED_CONTACTS_LOCAL_IDS, null);
            
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
                DatabaseHelper.trace(false, "getModifiedContactsNativeIds(): "+e);
            }
        } finally {
            
            CloseUtils.close(cursor);
            cursor = null;
        }
        
        return ids;
    }
    
    /**
     * SELECT NativeContactId FROM NativeChangeLog WHERE ChangeType = 0 ORDER BY NativeContactId
     */
    private final static String QUERY_DELETED_CONTACTS_NATIVE_IDS = "SELECT " + Field.NATIVECONTACTID + " FROM " + TABLE_NAME + " WHERE "
                                                                  + Field.CHANGETYPE + " = " + ContactChangeType.DELETE_CONTACT.ordinal()
                                                                  + " ORDER BY " + Field.NATIVECONTACTID;
    
    /**
     * 
     */
    private final static String QUERY_DELETED_CONTACT_NATIVE_ID = "SELECT " + Field.NATIVECONTACTID + " FROM " + TABLE_NAME + " WHERE "
                                                                  + Field.CHANGETYPE + " = " + ContactChangeType.DELETE_CONTACT.ordinal()
                                                                  + " AND " + Field.LOCALCONTACTID + " = ?";
    /**
     * Gets the list of native ids for the deleted contacts.
     *
     * @param readableDb the db to query from
     * @return an array of long ids of deleted native contacts, null if none
     */
    public static long[] getDeletedContactsNativeIds(SQLiteDatabase readableDb) {
        
        long[] ids = null;
        Cursor cursor = null;
        
        try {
            
            final int NATIVE_ID_INDEX = 0;
            
            cursor = readableDb.rawQuery(QUERY_DELETED_CONTACTS_NATIVE_IDS, null);
            
            if (cursor.getCount() > 0) {
                
                int i = 0;
                ids = new long[cursor.getCount()];
                
                while (cursor.moveToNext()) {
                    ids[i++] = cursor.getInt(NATIVE_ID_INDEX);
                }
            } else {
                
                return null;
            }
        } catch (Exception e) {
            
            if (Settings.ENABLED_DATABASE_TRACE) {
                DatabaseHelper.trace(false, "NativeChangeLogTable.getDeletedContactsNativeIds(): "+e);
            }
        } finally {
            
            CloseUtils.close(cursor);
            cursor = null;
        }
        
        return ids;
    }
    
    /**
     * Gets the list of deleted details as a ContactChange array.
     * 
     * Note: the ContactChange object will have the type ContactChange.TYPE_DELETE_DETAIL
     * 
     * @see ContactChange
     * 
     * @param localContactId the local contact id to query
     * @param readableDb the db to query from
     * @return an array of ContactChange, null if no details where found
     */
    public static ContactChange[] getDeletedDetails(long localContactId, SQLiteDatabase readableDb) {
        
        final String[] SELECTION = { String.valueOf(localContactId), Integer.toString(ContactChangeType.DELETE_DETAIL.ordinal()) };
        Cursor cursor = null;
        
        try {
            
            cursor = readableDb.rawQuery(QUERY_DELETED_DETAILS, SELECTION);
            
            if (cursor.getCount() > 0) {
                
                final ContactChange[] deletedDetails = new ContactChange[cursor.getCount()];
                int index = 0;
                while (cursor.moveToNext()) {
                    
                    // fill the ContactChange class with contact detail data
                    final ContactChange change = new ContactChange();
                    deletedDetails[index++] = change;
                    // set the ContactChange as a deleted contact detail
                    change.setType(ContactChange.TYPE_DELETE_DETAIL);
                    // LocalContactId=1
                    change.setInternalContactId(cursor.isNull(1) ? ContactChange.INVALID_ID : cursor.getLong(1));
                    // NativeContactId=2
                    change.setNabContactId(cursor.isNull(2) ? ContactChange.INVALID_ID : cursor.getLong(2));
                    // DetailLocalId=3
                    change.setInternalDetailId(cursor.isNull(3) ? ContactChange.INVALID_ID : cursor.getInt(3));
                    // NativeDetailId=4
                    change.setNabDetailId(cursor.isNull(4) ? ContactChange.INVALID_ID : cursor.getLong(4));
                    // DetailKey=5
                    change.setKey(cursor.isNull(5) ? ContactChange.KEY_UNKNOWN : ContactDetailsTable.mapInternalKeyToContactChangeKey(cursor.getInt(5)));
                    
                    if (change.getNabContactId() == ContactChange.INVALID_ID) {
                        
                        LogUtils.logE("NativeChangeLogTable.getDeletedDetails(): the native contact id shall not be null! cc.internalContactId="+change.getInternalContactId()+", cc.key="+change.getKey());
                    }
                }
                
                return deletedDetails;
            }
        } catch (Exception e) {
            
            if (Settings.ENABLED_DATABASE_TRACE) {
                DatabaseHelper.trace(false, "NativeChangeLogTable.getDeletedContactsNativeIds(): "+e);
            }
        } finally {
            
            CloseUtils.close(cursor);
            cursor = null;
        }
        
        return null;
    }
    
    /**
     * 
     * @param localContactId
     * @param readableDb
     * @return
     */
    public static long getDeletedContactNativeId(long localContactId, SQLiteDatabase readableDb) {
        
        final String[] SELECTION = { String.valueOf(localContactId) };
        Cursor cursor = null;
        
        try {
            
            cursor = readableDb.rawQuery(QUERY_DELETED_CONTACT_NATIVE_ID, SELECTION);
            
            if (cursor.getCount() > 0) {
                
                cursor.moveToNext();
                final Long nativeId = cursor.getLong(0);
                
                return nativeId == null ? -1 : nativeId;
            }
        } catch (Exception e) {
            
            if (Settings.ENABLED_DATABASE_TRACE) {
                DatabaseHelper.trace(false, "NativeChangeLogTable.getDeletedContactsNativeIds(): "+e);
            }
        } finally {
            
            CloseUtils.close(cursor);
            cursor = null;
        }
        
        return -1;
    }
}
