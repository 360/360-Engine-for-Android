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
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.SQLKeys;
import com.vodafone360.people.database.DatabaseHelper.ServerIdInfo;
import com.vodafone360.people.database.tables.ContactsTable.ContactIdInfo;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeyTypes;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeys;
import com.vodafone360.people.engine.contactsync.ContactChange;
import com.vodafone360.people.engine.presence.PresenceDbUtils;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.CursorUtils;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.StringBufferPool;

/**
 * Provides a wrapper for all the operations on the Contact Details Table. Class
 * is never instantiated, all methods and fields are static.
 */
public abstract class ContactDetailsTable {
    /**
     * Name of the table in the people database.
     */
    public static final String TABLE_NAME = "ContactDetails";
    
    /**
     * SELECT LocalContactId, DetailLocalId, NativeDetailId, NativeContactIdDup, Key, Type, StringVal, OrderNo FROM ContactDetails.
     * 
     * @see #getContactChanges(long, SQLiteDatabase)
     */
    private final static String QUERY_CONTACT_DETAILS_BY_LOCAL_ID = "SELECT " + Field.LOCALCONTACTID + ", " + Field.DETAILLOCALID + ", " + Field.NATIVEDETAILID +
                                            ", " + Field.NATIVECONTACTID + ", " + Field.TYPE + ", " + Field.ORDER + ", " + Field.KEY +
                                            ", " + Field.STRINGVAL + ", " + Field.DETAILSERVERID + " FROM " + TABLE_NAME + " WHERE " + Field.LOCALCONTACTID + " = ?";
    
    /**
     * 
     */
    private final static String QUERY_NATIVE_SYNCABLE_CONTACT_DETAILS_BY_LOCAL_ID = QUERY_CONTACT_DETAILS_BY_LOCAL_ID
                                                                                  + " AND (" + Field.NATIVESYNCCONTACTID + " IS NULL OR " + Field.NATIVESYNCCONTACTID
                                                                                  + " <> -1)";
    
    
    /**
     * SELECT DISTINCT LocalId
     * FROM ContactDetails
     * WHERE NativeSyncId IS NULL OR NativeSyncId <> -1
     */
    public final static String QUERY_NATIVE_SYNCABLE_CONTACTS_LOCAL_IDS = "SELECT " + Field.LOCALCONTACTID + " FROM " + TABLE_NAME
                                                                        + " WHERE " + Field.NATIVESYNCCONTACTID + " IS NULL OR " + Field.NATIVESYNCCONTACTID
                                                                        + " <> -1";
    

    /**
     * Equals the number of comma separated items returned by
     * {@link #getFullQueryList()}.
     * 
     * @See {@link #getQueryDataLength()}
     */
    private static final int DATA_QUERY_LENGTH = 14;

    /**
     * Associates a constant with a field string in the People database.
     */
    public static enum Field {
        DETAILLOCALID("DetailLocalId"),
        DETAILSERVERID("DetailServerId"),
        KEY("Key"),
        TYPE("Type"),
        STRINGVAL("StringVal"),
        ALT("Alt"),
        DELETED("Deleted"),
        ORDER("OrderNo"),
        BYTES("Bytes"),
        BYTESMIMETYPE("BytesMimeType"),
        PHOTOURL("PhotoUrl"),
        UPDATED("Updated"),
        LOCALCONTACTID("LocalContactId"),
        NATIVECONTACTID("NativeContactIdDup"),
        NATIVEDETAILID("NativeDetailId"),
        NATIVEDETAILVAL1("NativeValue1"),
        NATIVEDETAILVAL2("NativeValue2"),
        NATIVEDETAILVAL3("NativeValue3"),
        SERVERSYNCCONTACTID("ServerSyncContactId"),
        NATIVESYNCCONTACTID("NativeSyncContactId");
        /**
         * Name of the table field as it appears in the database.
         */
        private final String mField;

        /**
         * Constructs the enumeration value by associating it with a string.
         * 
         * @param field The string specified in the list above.
         */
        private Field(String field) {
            mField = field;
        }

        /**
         * Returns the enum value as the associated field name.
         */
        public String toString() {
            return mField;
        }
    }

    /**
     * Holds the Native Contact information that is stored in the database for a
     * contact detail. Information is used when matching a contact detail from
     * People with a detail in the Android native phonebook.
     */
    public static class NativeIdInfo {
        /**
         * Associated with the primary key (localDetailId) in the People Contact
         * Details table.
         */
        public long localId;

        /**
         * Associated with the primary key (_id) in the native People table.
         */
        public Integer nativeContactId;

        /**
         * Associated with the primary key (_id) in the native Phones,
         * ContactMethods or Organizations table (depending on contact detail).
         * Can be null for some types of detail.
         */
        public Integer nativeDetailId;

        /**
         * Detail type specific. Stores value of one of the fields in a native
         * table (Phones, ContactMethods or Organisations). Used to determine if
         * the detail in the native table has changed. Examples: 1) In case of
         * phone number, this value holds the phone number in the same format as
         * the native database. 2) In case of address, this value holds the full
         * address (all in one string). This differs from the value field which
         * stores the address in VCard format.
         */
        public String nativeVal1;

        /**
         * Detail type specific. Stores value of one of the fields in a native
         * table (Phones, ContactMethods or Organisations). Used to determine if
         * the detail in the native table has changed.
         */
        public String nativeVal2;

        /**
         * Detail type specific. Stores value of one of the fields in a native
         * table (Phones, ContactMethods or Organisations). Used to determine if
         * the detail in the native table has changed.
         */
        public String nativeVal3;

        /**
         * Can hold one of the following values:
         * <ul>
         * <li><b>NULL</b> Contact has just been added to the database</li>
         * <li><b>-1</b> Detail in People database has been synced with the
         * native</li>
         * <li><b>Positive Integer</b> Detail has been added or changed and
         * needs to be synced to the native.</li>
         * </ul>
         */
        public long syncNativeContactId = -1L;
    }

    /**
     * Creates the contact detail table.
     * 
     * @param writeableDb The SQLite database
     * @throws SQLException If the table already exists or the database is
     *             corrupt
     */
    public static void create(SQLiteDatabase writeableDb) throws SQLException {
        DatabaseHelper.trace(true, "ContactDetailsTable.create()");
        writeableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.DETAILLOCALID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Field.DETAILSERVERID + " LONG, "
                + Field.KEY + " INTEGER, " + Field.TYPE + " INTEGER, " + Field.STRINGVAL
                + " TEXT, " + Field.ALT + " TEXT, " + Field.DELETED + " BOOLEAN, " + Field.ORDER
                + " INTEGER, " + Field.BYTES + " BINARY, " + Field.BYTESMIMETYPE + " TEXT, "
                + Field.PHOTOURL + " TEXT, " + Field.UPDATED + " INTEGER, " + Field.LOCALCONTACTID
                + " LONG NOT NULL, " + Field.NATIVECONTACTID + " INTEGER, " + Field.NATIVEDETAILID
                + " INTEGER," + Field.NATIVEDETAILVAL1 + " TEXT," + Field.NATIVEDETAILVAL2
                + " TEXT," + Field.NATIVEDETAILVAL3 + " TEXT," + Field.SERVERSYNCCONTACTID
                + " LONG," + Field.NATIVESYNCCONTACTID + " INTEGER);");
    }

    /**
     * Fetches the list of table fields that can be injected into an SQL query
     * statement. The {@link #getQueryData(Cursor)} method can be used to obtain
     * the data from the query.
     * 
     * @return The query string
     * @Note Constant {@link #DATA_QUERY_LENGTH} must equal the number of comma
     *       separated items returned by this function.
     */
    private static String getFullQueryList() {
        return Field.DETAILLOCALID + ", " + Field.DETAILSERVERID + ", " + Field.LOCALCONTACTID
                + ", " + Field.KEY + ", " + Field.TYPE + ", " + Field.STRINGVAL + ", " + Field.ALT
                + ", " + Field.ORDER + ", " + Field.UPDATED + ", " + Field.NATIVECONTACTID + ", "
                + Field.NATIVEDETAILID + ", " + Field.NATIVEDETAILVAL1 + ", "
                + Field.NATIVEDETAILVAL2 + ", " + Field.NATIVEDETAILVAL3 + ", "
                + Field.SERVERSYNCCONTACTID + ", " + Field.NATIVESYNCCONTACTID;
    }

    /**
     * Returns the SQL for doing a raw query on the contact details table. The
     * {@link #getQueryData(Cursor)} method can be used for fetching the data
     * from the resulting cursor.
     * 
     * @param whereClause The SQL constraint (cannot be empty or null)
     * @return The SQL query string
     */
    public static String getQueryStringSql(String whereClause) {
        return "SELECT " + getFullQueryList() + " FROM " + TABLE_NAME + " WHERE " + whereClause;
    }

    /**
     * Returns the number of comma separated items returned by
     * {@link #getFullQueryList()}.
     * 
     * @See {@link #DATA_QUERY_LENGTH}
     * @return The number of items
     */
    private static int getQueryDataLength() {
        return DATA_QUERY_LENGTH;
    }

    /**
     * Identifies the columns of the data returned by getFullQueryList()
     */
    private static final int COLUMN_LOCALDETAILID = 0;

    private static final int COLUMN_SERVERDETAILID = 1;

    private static final int COLUMN_LOCALCONTACTID = 2;

    private static final int COLUMN_KEY = 3;

    private static final int COLUMN_KEYTYPE = 4;

    private static final int COLUMN_VALUE = 5;

    private static final int COLUMN_ALT = 6;

    private static final int COLUMN_ORDER = 7;

    private static final int COLUMN_UPDATED = 8;

    private static final int COLUMN_NATIVECONTACTID = 9;

    private static final int COLUMN_NATIVEDETAILID = 10;

    private static final int COLUMN_NATIVEVAL1 = 11;

    private static final int COLUMN_NATIVEVAL2 = 12;

    private static final int COLUMN_NATIVEVAL3 = 13;

    private static final int COLUMN_SERVER_SYNC = 14;

    private static final int COLUMN_NATIVE_SYNC_CONTACT_ID = 15;

    /**
     * Returns the contact detail for the current record in the cursor. Query to
     * produce the cursor must be obtained using the {@link #getFullQueryList()}
     * .
     * 
     * @param The cursor obtained from doing the query
     * @return The contact detail object.
     */
    public static ContactDetail getQueryData(Cursor c) {
        ContactDetail detail = new ContactDetail();
        if (!c.isNull(COLUMN_LOCALDETAILID)) {
            detail.localDetailID = c.getLong(COLUMN_LOCALDETAILID);
        }
        if (!c.isNull(COLUMN_SERVERDETAILID)) {
            detail.unique_id = c.getLong(COLUMN_SERVERDETAILID);
        }
        if (!c.isNull(COLUMN_LOCALCONTACTID)) {
            detail.localContactID = c.getLong(COLUMN_LOCALCONTACTID);
        }
        detail.key = ContactDetail.DetailKeys.values()[c.getInt(COLUMN_KEY)];
        if (!c.isNull(COLUMN_KEYTYPE)) {
            detail.keyType = ContactDetail.DetailKeyTypes.values()[c.getInt(COLUMN_KEYTYPE)];
        }
        detail.value = c.getString(COLUMN_VALUE);
        if (!c.isNull(COLUMN_ALT)) {
            detail.alt = c.getString(COLUMN_ALT);
        }
        if (!c.isNull(COLUMN_ORDER)) {
            detail.order = c.getInt(COLUMN_ORDER);
        }
        if (!c.isNull(COLUMN_UPDATED)) {
            detail.updated = c.getLong(COLUMN_UPDATED);
        }
        if (!c.isNull(COLUMN_NATIVECONTACTID)) {
            detail.nativeContactId = c.getInt(COLUMN_NATIVECONTACTID);
        }
        if (!c.isNull(COLUMN_NATIVEDETAILID)) {
            detail.nativeDetailId = c.getInt(COLUMN_NATIVEDETAILID);
        }
        if (!c.isNull(COLUMN_NATIVEVAL1)) {
            detail.nativeVal1 = c.getString(COLUMN_NATIVEVAL1);
        }
        if (!c.isNull(COLUMN_NATIVEVAL2)) {
            detail.nativeVal2 = c.getString(COLUMN_NATIVEVAL2);
        }
        if (!c.isNull(COLUMN_NATIVEVAL3)) {
            detail.nativeVal3 = c.getString(COLUMN_NATIVEVAL3);
        }
        if (!c.isNull(COLUMN_SERVER_SYNC)) {
            detail.serverContactId = c.getLong(COLUMN_SERVER_SYNC);
        }
        if (!c.isNull(COLUMN_NATIVE_SYNC_CONTACT_ID)) {
            detail.syncNativeContactId = c.getInt(COLUMN_NATIVE_SYNC_CONTACT_ID);
        }
        return detail;
    }

    /**
     * Extracts the suitable content values from a contact detail which can be
     * used to update or insert a record in the table.
     * 
     * @param detail The contact detail
     * @param syncToServer If true the detail needs to be synced to the server,
     *            otherwise no sync required
     * @param syncToNative If true the detail needs to be synced with the
     *            native, otherwise no sync required
     * @return The ContentValues to use for the update or insert.
     * @note If any given field values are NULL they will NOT be added to the
     *       content values data.
     */
    private static ContentValues fillUpdateData(ContactDetail detail, boolean syncToServer,
            boolean syncToNative) {
        ContentValues contactDetailValues = new ContentValues();
        if (detail.key != null) {
            contactDetailValues.put(Field.KEY.toString(), detail.key.ordinal());
        }
        if (detail.keyType != null) {
            contactDetailValues.put(Field.TYPE.toString(), detail.keyType.ordinal());
        }
        if (detail.localDetailID != null) {
            contactDetailValues.put(Field.DETAILLOCALID.toString(), detail.localDetailID);
        }
        contactDetailValues.put(Field.STRINGVAL.toString(), detail.value);
        if (detail.alt != null
                || (detail.key != null && detail.key == ContactDetail.DetailKeys.PRESENCE_TEXT)) {
            contactDetailValues.put(Field.ALT.toString(), detail.alt);
        }
        if (detail.unique_id != null) {
            contactDetailValues.put(Field.DETAILSERVERID.toString(), detail.unique_id);
        }
        if (detail.order != null) {
            contactDetailValues.put(Field.ORDER.toString(), detail.order);
        }
        if (detail.updated != null) {
            contactDetailValues.put(Field.UPDATED.toString(), detail.updated);
        }
        contactDetailValues.put(Field.LOCALCONTACTID.toString(), detail.localContactID);
        if (detail.deleted != null) {
            contactDetailValues.put(Field.DELETED.toString(), false);
        }
        // contactDetailValues.put(Field.BYTES.toString(), (byte[])null); //
        // TODO: Needs implementation
        if (detail.photo_mime_type != null) {
            contactDetailValues.put(Field.BYTESMIMETYPE.toString(), detail.photo_mime_type);
        }
        if (detail.photo_url != null) {
            contactDetailValues.put(Field.PHOTOURL.toString(), detail.photo_url);
        }
        if (detail.nativeContactId != null) {
            contactDetailValues.put(Field.NATIVECONTACTID.toString(), detail.nativeContactId);
        }
        if (detail.nativeDetailId != null) {
            contactDetailValues.put(Field.NATIVEDETAILID.toString(), detail.nativeDetailId);
        }
        if (syncToServer) {
            contactDetailValues.put(Field.SERVERSYNCCONTACTID.toString(), detail.serverContactId);
        } else {
            contactDetailValues.put(Field.SERVERSYNCCONTACTID.toString(), -1);
        }
        if (syncToNative) {
            contactDetailValues.put(Field.NATIVESYNCCONTACTID.toString(),
                    detail.syncNativeContactId);
        } else {
            contactDetailValues.put(Field.NATIVESYNCCONTACTID.toString(), -1);
            if (detail.nativeVal1 != null) {
                contactDetailValues.put(Field.NATIVEDETAILVAL1.toString(), detail.nativeVal1);
            }
            if (detail.nativeVal2 != null) {
                contactDetailValues.put(Field.NATIVEDETAILVAL2.toString(), detail.nativeVal2);
            }
            if (detail.nativeVal3 != null) {
                contactDetailValues.put(Field.NATIVEDETAILVAL3.toString(), detail.nativeVal3);
            }
        }
        return contactDetailValues;
    }

    /**
     * Fetches a contact detail from the table
     * 
     * @param localDetailId The local ID of the required detail
     * @param readableDb The readable SQLite database object
     * @return ContactDetail object or NULL if the detail was not found
     */
    public static ContactDetail fetchDetail(long localDetailId, SQLiteDatabase readableDb) {
        String[] mArgs = {
            String.format("%d", localDetailId)
        };
        ContactDetail detail = null;
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery(getQueryStringSql(Field.DETAILLOCALID + " = ?"), mArgs);
            if (cursor.moveToFirst()) {
                detail = getQueryData(cursor);
            }
        } catch (SQLiteException e) {
            LogUtils.logE(
                    "ContactDetailsTable.fetchDetail() Exception - Unable to fetch contact detail",
                    e);
            return null;
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }

        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactDetailsTable.fetchDetail() localDetailId["
                    + localDetailId + "] unique_id[" + detail.unique_id + "] value[" + detail.value
                    + "]");
        }
        return detail;
    }

    /**
     * Removes a contact detail from the table
     * 
     * @param localDetailId The local ID identifying the detail
     * @param writeableDb A writable SQLite database object.
     * @return true if the delete was successful, false otherwise
     */
    public static boolean deleteDetailByDetailId(long localDetailId, SQLiteDatabase writeableDb) {
        try {
            String[] mArgs = {
                String.format("%d", localDetailId)
            };
            writeableDb.delete(TABLE_NAME, Field.DETAILLOCALID + "=?", mArgs);
            DatabaseHelper.trace(true,
                    "ContactDetailsTable.deleteDetailByDetailId() Deleted localDetailId["
                            + localDetailId + "]");
            return true;

        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.fetchDetail() SQLException - Unable to delete contact detail with localDetailId["
                                    + localDetailId + "]", e);
            return false;
        }
    }

    /**
     * Deletes all the contact details associated with a contact
     * 
     * @param localContactId The local contact ID identifying the contact
     * @param writeableDb A writable SQLite database object.
     * @return true if the delete was successful, false otherwise
     */
    public static ServiceStatus deleteDetailByContactId(long localContactId,
            SQLiteDatabase writeableDb) {
        try {
            String[] args = {
                String.format("%d", localContactId)
            };
            writeableDb.delete(TABLE_NAME, Field.LOCALCONTACTID + "=?", args);
            DatabaseHelper.trace(true,
                    "ContactDetailsTable.deleteDetailByContactId() Deleted localContactId["
                            + localContactId + "]");
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.deleteDetailByContactId() SQLException - Unable to delete contact detail with localContactId["
                                    + localContactId + "]", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
    }

    /**
     * Adds a new contact detail to the table.
     * 
     * @param detail The new detail
     * @param syncToServer Mark the new detail so it will be synced to the
     *            server
     * @param syncToNative Mark the new detail so it will be synced to the
     *            native database
     * @param writeableDb A writable SQLite database object.
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus addContactDetail(ContactDetail detail, boolean syncToServer,
            boolean syncToNative, SQLiteDatabase writeableDb) {
        try {
            if (detail.localContactID == null) {
                LogUtils
                        .logE("ContactDetailsTable.addContactDetail() Unable to add contact detail - invalid parameter");
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            detail.localDetailID = null;
            ContentValues cv = fillUpdateData(detail, syncToServer, syncToNative);
            detail.localDetailID = writeableDb.insertOrThrow(TABLE_NAME, null, cv);
            if (detail.localDetailID < 0) {
                LogUtils
                        .logE("ContactDetailsTable.addContactDetail() Unable to add contact detail");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
            DatabaseHelper.trace(true,
                    "ContactDetailsTable.addContactDetail() Added localDetailID["
                            + detail.localDetailID + "]");
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.addContactDetail() SQLException - Unable to add contact detail",
                            e);

            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
    }

    /**
     * Updates an existing contact detail in the table.
     * 
     * @param detail The modified detail.
     * @param syncToServer Mark the new detail so it will be synced to the
     *            server
     * @param syncToNative Mark the new detail so it will be synced to the
     *            native database
     * @param writeableDb A writable SQLite database object.
     * @return SUCCESS or a suitable error code.
     * @note If any given field values in the contact detail are NULL they will
     *       NOT be modified in the database.
     */
    public static ServiceStatus modifyDetail(ContactDetail detail, boolean syncToServer,
            boolean syncToNative, SQLiteDatabase writeableDb) {
        try {
            ContentValues contactDetailValues = fillUpdateData(detail, syncToServer, syncToNative);
            if (writeableDb.update(TABLE_NAME, contactDetailValues, Field.DETAILLOCALID + " = "
                    + detail.localDetailID, null) <= 0) {
                LogUtils
                        .logE("ContactDetailsTable.modifyDetail() Unable to update contact detail , localDetailID["
                                + detail.localDetailID + "]");
                return ServiceStatus.ERROR_NOT_FOUND;
            }
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.modifyDetail() SQLException - Unable to modify contact detail",
                            e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        DatabaseHelper.trace(true, "ContactDetailsTable.modifyDetail() localContactID["
                + detail.localContactID + "] localDetailID[" + detail.localDetailID + "]");
        return ServiceStatus.SUCCESS;
    }

    /**
     * Updates the detail server ID stored with the record
     * 
     * @param localId The local detail ID identifying the record
     * @param serverId The new server ID
     * @param writeableDb A writable SQLite database object.
     * @return true if the update was successful, false otherwise
     */
    public static boolean modifyDetailServerId(long localId, Long serverId,
            SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(true, "ContactDetailsTable.modifyDetailServerId()");
        try {
            ContentValues cv = new ContentValues();
            cv.put(Field.DETAILSERVERID.toString(), serverId);
            String[] args = {
                String.format("%d", localId)
            };
            if (writeableDb.update(TABLE_NAME, cv, Field.DETAILLOCALID + "=?", args) <= 0) {
                LogUtils
                        .logE("ContactDetailsTable.modifyDetailServerId() Unable to update contact detail server ID");
                return false;
            }
            return true;
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.modifyDetailServerId() SQLException - Unable to modify contact detail server ID",
                            e);
            return false;
        }
    }

    /**
     * Updates the detail native contact information stored with the record
     * 
     * @param detail The contact detail which contacts the new native
     *            information and the local ID of the detail to be modified.
     * @param writeableDb A writable SQLite database object.
     * @return true if the update was successful, false otherwise
     */
    public static boolean modifyDetailNativeId(ContactDetail detail, SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(true, "ContactDetailsTable.modifyDetailNativeId()");
        try {
            ContentValues cv = new ContentValues();
            cv.put(Field.NATIVECONTACTID.toString(), detail.nativeContactId);
            cv.put(Field.NATIVEDETAILID.toString(), detail.nativeDetailId);
            cv.put(Field.NATIVEDETAILVAL1.toString(), detail.nativeVal1);
            cv.put(Field.NATIVEDETAILVAL2.toString(), detail.nativeVal2);
            cv.put(Field.NATIVEDETAILVAL3.toString(), detail.nativeVal3);
            String[] args = {
                String.format("%d", detail.localDetailID)
            };
            if (writeableDb.update(TABLE_NAME, cv, Field.DETAILLOCALID + "=?", args) <= 0) {
                LogUtils
                        .logE("ContactDetailsTable.modifyDetailNativeId() Unable to update contact detail native ID");
                return false;
            }
            return true;
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.modifyDetailNativeId() SQLException - Unable to modify contact detail native ID",
                            e);
            return false;
        }
    }

    /**
     * Fetches all contact details that need to be synced with the native
     * contacts database
     * 
     * @param detailList A list that will be populated with the contact details.
     * @param keyList A list of keys to filter the result
     * @param byDetailId true to order the details by native detail ID, false to
     *            order by native contact ID
     * @param firstIndex The index of the first record to fetch
     * @param count The number of records to fetch (or -1 to fetch all)
     * @param readableDb A readable SQLite database object.
     * @return true if the operation was successful, false otherwise
     */
    public static boolean fetchContactDetailsForNative(List<ContactDetail> detailList,
            DetailKeys[] keyList, boolean byDetailId, int firstIndex, int count,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactDetailsTable.fetchContactDetailsForNative()");
        detailList.clear();
        Cursor c = null;
        try {
            StringBuilder sb1 = new StringBuilder();
            for (int i = 0; i < keyList.length; i++) {
                sb1.append(Field.KEY + " = " + keyList[i].ordinal());
                if (i < keyList.length - 1) {
                    sb1.append(" OR ");
                }
            }
            String orderByText = null;
            if (byDetailId) {
                orderByText = Field.NATIVEDETAILID.toString();
            } else {
                orderByText = Field.NATIVECONTACTID.toString();
            }
            c = readableDb.rawQuery("SELECT " + getFullQueryList() + ", "
                    + Field.NATIVESYNCCONTACTID + " FROM " + TABLE_NAME + " WHERE "
                    + Field.NATIVECONTACTID + " IS NOT NULL AND (" + sb1 + ") ORDER BY "
                    + orderByText + " LIMIT " + firstIndex + "," + count, null);
            while (c.moveToNext()) {
                ContactDetail detail = getQueryData(c);
                final int fieldIdx = getQueryDataLength();
                if (!c.isNull(fieldIdx)) {
                    detail.syncNativeContactId = c.getInt(fieldIdx);
                }
                detailList.add(detail);
            }
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            CloseUtils.close(c);
        }
    }

    /**
     * Fetches the first contact detail found for a contact and key.
     * 
     * @param localContactId The local contact ID
     * @param key The contact detail key value
     * @param readableDb A readable SQLite database object.
     * @return The contact detail, or NULL if it could not be found.
     */
    public static ContactDetail fetchDetail(long localContactId, DetailKeys key,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactDetailsTable.fetchDetail()");
        String[] args = {
                String.format("%d", localContactId), String.format("%d", key.ordinal())
        };
        ContactDetail detail = null;
        Cursor c = null;
        try {
            c = readableDb.rawQuery(getQueryStringSql(Field.LOCALCONTACTID + "=? AND " + Field.KEY
                    + "=?"), args);
            if (c.moveToFirst()) {
                detail = getQueryData(c);
            }
        } catch (SQLiteException e) {
            LogUtils.logE(
                    "ContactDetailsTable.fetchDetail() Exception - Unable to fetch contact detail",
                    e);
            return null;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return detail;
    }

    /**
     * Finds a phone contact detail which matches a given telephone number. Uses
     * the native Android functionality for matching the numbers.
     * 
     * @param phoneNumber The number to find
     * @param phoneDetail An empty contact detail where the resulting phone
     *            contact detail will be stored.
     * @param nameDetail An empty contact detail where the resulting name
     *            contact detail will be stored.
     * @param readableDb A readable SQLite database object.
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus fetchContactInfo(String phoneNumber, ContactDetail phoneDetail,
            ContactDetail nameDetail, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactDetailsTable.fetchContactInfo() phoneNumber["
                + phoneNumber + "]");
        if (phoneNumber == null) {
            return ServiceStatus.ERROR_NOT_FOUND;
        }
        Cursor c2 = null;
        final String searchNumber = DatabaseUtils.sqlEscapeString(phoneNumber);
        try {
            String[] args = {
                String.format("%d", ContactDetail.DetailKeys.VCARD_PHONE.ordinal())
            };
            c2 = readableDb.rawQuery(getQueryStringSql(Field.KEY + "=? AND PHONE_NUMBERS_EQUAL("
                    + Field.STRINGVAL + "," + searchNumber + ")"), args);
            if (!c2.moveToFirst()) {
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            phoneDetail.copy(getQueryData(c2));
            if (nameDetail != null) {
                ContactDetail fetchedNameDetail = fetchDetail(phoneDetail.localContactID,
                        ContactDetail.DetailKeys.VCARD_NAME, readableDb);
                if (fetchedNameDetail != null) {
                    nameDetail.copy(fetchedNameDetail);
                }
            }
        } catch (SQLiteException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.fetchContactInfo() Exception - Unable to fetch contact detail",
                            e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(c2);
            c2 = null;
        }

        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetch details for a given contact
     * 
     * @param localContactId The local ID of the contact
     * @param detailList A list which will be populated with the details
     * @param readableDb A readable SQLite database object.
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus fetchContactDetails(Long localContactId,
            List<ContactDetail> detailList, SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactDetailsTable.fetchContactDetails() localContactId["
                + localContactId + "]");

        String[] args = {
            String.format("%d", localContactId)
        };
        Cursor c = null;
        try {
            c = readableDb.rawQuery(ContactDetailsTable
                    .getQueryStringSql(ContactDetailsTable.Field.LOCALCONTACTID + " = ?"), args);
            detailList.clear();
            while (c.moveToNext()) {
                detailList.add(ContactDetailsTable.getQueryData(c));
            }
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.fetchContactDetails() Exception - Unable to fetch contact details for contact",
                            e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(c);
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Set contact detail server ID for all those details which require a server
     * ID. In any case, the server sync contact ID flag is set to -1 to indicate
     * that the detail has been fully synced with the server.
     * 
     * @param serverIdList The list of contact details. This list should include
     *            all details even the ones which don't have server IDs.
     * @param writableDb A writable SQLite database object
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus syncSetServerIds(List<ServerIdInfo> serverIdList,
            SQLiteDatabase writableDb) {
        final int STATEMENT1_COLUMN_SERVERID = 1;
        final int STATEMENT1_COLUMN_LOCALID = 2;
        final int STATEMENT2_COLUMN_LOCALID = 1;

        DatabaseHelper.trace(true, "ContactDetailsTable.syncSetServerIds()");
        if (serverIdList.size() == 0) {
            return ServiceStatus.SUCCESS;
        }
        
        try {
            writableDb.beginTransaction();
            SQLiteStatement statement1 = null;
            SQLiteStatement statement2 = null;
            for (int i = 0; i < serverIdList.size(); i++) {
                final ServerIdInfo info = serverIdList.get(i);
                if (info.serverId != null) {
                    if (statement1 == null) {
                        statement1 = writableDb.compileStatement("UPDATE " + TABLE_NAME + " SET "
                                + Field.DETAILSERVERID + "=?," + Field.SERVERSYNCCONTACTID
                                + "=-1 WHERE " + Field.DETAILLOCALID + "=?");
                    }
                    statement1.bindLong(STATEMENT1_COLUMN_SERVERID, info.serverId);
                    statement1.bindLong(STATEMENT1_COLUMN_LOCALID, info.localId);
                    statement1.execute();
                } else {
                    if (statement2 == null) {
                        statement2 = writableDb.compileStatement("UPDATE " + TABLE_NAME + " SET "
                                + Field.SERVERSYNCCONTACTID + "=-1 WHERE " + Field.DETAILLOCALID
                                + "=?");
                    }
                    statement2.bindLong(STATEMENT2_COLUMN_LOCALID, info.localId);
                    statement2.execute();
                }
            }
            writableDb.setTransactionSuccessful();
            return ServiceStatus.SUCCESS;
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.syncSetServerIds() SQLException - Unable to update contact detail server Ids",
                            e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            writableDb.endTransaction();
        }
    }

    /**
     * Set native detail ID for all those details which require an ID. In any
     * case, the native sync contact ID flag is set to -1 to indicate that the
     * detail has been fully synced with the native contacts database.
     * 
     * @param serverIdList The list of contact details. This list should include
     *            all details even the ones which don't have native IDs.
     * @param writableDb A writable SQLite database object
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus syncSetNativeIds(List<NativeIdInfo> nativeIdList,
            SQLiteDatabase writableDb) {
        final int STATEMENT1_COLUMN_NATIVECONTACTID = 1;
        final int STATEMENT1_COLUMN_NATIVEDETAILID = 2;
        final int STATEMENT1_COLUMN_NATIVEVAL1 = 3;
        final int STATEMENT1_COLUMN_NATIVEVAL2 = 4;
        final int STATEMENT1_COLUMN_NATIVEVAL3 = 5;
        final int STATEMENT1_COLUMN_SYNCNATIVECONTACTID = 6;
        final int STATEMENT1_COLUMN_LOCALID = 7;

        DatabaseHelper.trace(true, "ContactDetailsTable.syncSetNativeIds()");
        if (nativeIdList.size() == 0) {
            return ServiceStatus.SUCCESS;
        }
        
        try {
            writableDb.beginTransaction();
            SQLiteStatement statement = writableDb.compileStatement("UPDATE " + TABLE_NAME
                    + " SET " + Field.NATIVECONTACTID + "=?," + Field.NATIVEDETAILID + "=?,"
                    + Field.NATIVEDETAILVAL1 + "=?," + Field.NATIVEDETAILVAL2 + "=?,"
                    + Field.NATIVEDETAILVAL3 + "=?," + Field.NATIVESYNCCONTACTID + "=? WHERE "
                    + Field.DETAILLOCALID + "=?");
            for (int i = 0; i < nativeIdList.size(); i++) {
                final NativeIdInfo info = nativeIdList.get(i);
                statement.clearBindings();
                if (info.nativeContactId != null) {
                    statement.bindLong(STATEMENT1_COLUMN_NATIVECONTACTID, info.nativeContactId);
                }
                if (info.nativeDetailId != null) {
                    statement.bindLong(STATEMENT1_COLUMN_NATIVEDETAILID, info.nativeDetailId);
                }
                if (info.nativeVal1 != null) {
                    statement.bindString(STATEMENT1_COLUMN_NATIVEVAL1, info.nativeVal1);
                }
                if (info.nativeVal2 != null) {
                    statement.bindString(STATEMENT1_COLUMN_NATIVEVAL2, info.nativeVal2);
                }
                if (info.nativeVal3 != null) {
                    statement.bindString(STATEMENT1_COLUMN_NATIVEVAL3, info.nativeVal3);
                }
                statement.bindLong(STATEMENT1_COLUMN_SYNCNATIVECONTACTID, info.syncNativeContactId);
                statement.bindLong(STATEMENT1_COLUMN_LOCALID, info.localId);
                statement.execute();
            }
            writableDb.setTransactionSuccessful();
            return ServiceStatus.SUCCESS;
        } catch (SQLException e) {
            LogUtils
                    .logE(
                            "ContactDetailsTable.syncSetNativeIds() SQLException - Unable to update contact detail native Ids",
                            e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            writableDb.endTransaction();
        }
    }

    /**
     * Fetches the preferred detail to use in the contact summary when the name
     * or status field are blank.
     * 
     * @param localContactID The local contact ID
     * @param keyVal The key to fetch (normally either VCARD_PHONE or
     *            VCARD_EMAIL).
     * @param altDetail A contact detail object where the result will be stored.
     * @param readableDb A readable SQLite database object
     * @return true if successful, false otherwise.
     */
    public static boolean fetchPreferredDetail(long localContactID, int keyVal,
            ContactDetail altDetail, SQLiteDatabase readableDb) {
        final int QUERY_COLUMN_LOCALDETAILID = 0;
        final int QUERY_COLUMN_TYPE = 1;
        final int QUERY_COLUMN_VAL = 2;
        final int QUERY_COLUMN_ORDER = 3;
        DatabaseHelper.trace(false, "ContactDetailsTable.fetchPreferredDetail()");
        Cursor c = null;
        try {
            boolean found = false;
            c = readableDb.rawQuery("SELECT " + Field.DETAILLOCALID + "," + Field.TYPE + ","
                    + Field.STRINGVAL + "," + Field.ORDER + " FROM " + TABLE_NAME + " WHERE "
                    + Field.LOCALCONTACTID + "=" + localContactID + " AND " + Field.KEY + "="
                    + keyVal + " AND " + Field.ORDER + "=" + "(SELECT MIN(" + Field.ORDER
                    + ") FROM " + TABLE_NAME + " WHERE " + Field.LOCALCONTACTID + "="
                    + localContactID + " AND " + Field.KEY + "=" + keyVal + ")", null);
            if (c.moveToFirst()) {
                if (!c.isNull(QUERY_COLUMN_LOCALDETAILID)) {
                    altDetail.localDetailID = c.getLong(QUERY_COLUMN_LOCALDETAILID);
                }
                if (!c.isNull(QUERY_COLUMN_TYPE)) {
                    altDetail.keyType = ContactDetail.DetailKeyTypes.values()[c
                            .getInt(QUERY_COLUMN_TYPE)];
                }
                if (!c.isNull(QUERY_COLUMN_VAL)) {
                    altDetail.value = c.getString(QUERY_COLUMN_VAL);
                }
                if (!c.isNull(QUERY_COLUMN_ORDER)) {
                    altDetail.order = c.getInt(QUERY_COLUMN_ORDER);
                }
                found = true;
            }
            return found;
        } catch (SQLException e) {
            LogUtils.logE("ContactDetailsTable.fetchPreferredDetail() SQLException "
                    + "- Unable to fetch preferred detail", e);
            return false;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }

    /**
     * Fixes the phone numbers and emails of a contact to ensure that at least
     * one of each is a preferred detail.
     * 
     * @param localContactId The local Id of the contact
     * @param writableDb A writable SQLite database object
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus fixPreferredValues(long localContactId, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(false, "ContactDetailsTable.fixPreferredValues()");
        ServiceStatus status = fixPreferredDetail(localContactId,
                ContactDetail.DetailKeys.VCARD_PHONE, writableDb);
        if (ServiceStatus.SUCCESS != status) {
            return status;
        }
        return fixPreferredDetail(localContactId, ContactDetail.DetailKeys.VCARD_EMAIL, writableDb);
    }

    /**
     * Ensures that for a given key there is at least one preferred detail.
     * Modifying the database if necessary.
     * 
     * @param localContactId The local ID of the contact.
     * @param key The key to fix.
     * @param writableDb A writable SQLite database object
     * @return SUCCESS or a suitable error code.
     */
    private static ServiceStatus fixPreferredDetail(long localContactId,
            ContactDetail.DetailKeys key, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(false, "ContactDetailsTable.fixPreferredDetail()");
        ContactDetail altDetail = new ContactDetail();
        if (fetchPreferredDetail(localContactId, key.ordinal(), altDetail, writableDb)) {
            if (altDetail.order > 0) {
                altDetail = fetchDetail(altDetail.localDetailID, writableDb);
                if (altDetail != null) {
                    altDetail.order = 0;
                    boolean syncWithServer = (null != altDetail.serverContactId)
                            && (altDetail.serverContactId > -1);
                    boolean syncWithNative = (null != altDetail.syncNativeContactId)
                            && (altDetail.syncNativeContactId > -1);

                    return modifyDetail(altDetail, syncWithServer, syncWithNative, writableDb);
                }
                return ServiceStatus.ERROR_NOT_FOUND;
            }
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetches a list of contact details with only the native sync information
     * filled in. This method is used in preference to
     * {@link #fetchContactDetails(Long, List, SQLiteDatabase)} during contact
     * sync operations to improve performance.
     * 
     * @param localContactId Local Id of the contact (in the database).
     * @param nativeInfoList A list which will be filled with contact detail
     *            objects.
     * @param writableDb A writable SQLite database object
     * @return SUCCESS or a suitable error code.
     */
    /*
     * public static ServiceStatus fetchNativeInfo(long localContactId,
     * List<ContactDetail> nativeInfoList, SQLiteDatabase writableDb) { final
     * int QUERY_COLUMN_LOCALDETAILID = 0; final int QUERY_COLUMN_KEY = 1; final
     * int QUERY_COLUMN_SERVERDETAILID = 2; final int
     * QUERY_COLUMN_NATIVECONTACTID = 3; final int QUERY_COLUMN_NATIVEDETAILID =
     * 4; final int QUERY_COLUMN_NATIVEVAL1 = 5; final int
     * QUERY_COLUMN_NATIVEVAL2 = 6; final int QUERY_COLUMN_NATIVEVAL3 = 7; final
     * int QUERY_COLUMN_NATIVESYNCCONTACTID = 8; DatabaseHelper.trace(true,
     * "ContactDetailsTable.fetchNativeInfo()"); Cursor c = null; try { String[]
     * args = { String.valueOf(localContactId) }; c =
     * writableDb.rawQuery("SELECT " + Field.DETAILLOCALID + "," + Field.KEY +
     * "," + Field.DETAILSERVERID + "," + Field.NATIVECONTACTID + "," +
     * Field.NATIVEDETAILID + "," + Field.NATIVEDETAILVAL1 + "," +
     * Field.NATIVEDETAILVAL2 + "," + Field.NATIVEDETAILVAL3 + "," +
     * Field.NATIVESYNCCONTACTID + " FROM " + TABLE_NAME + " WHERE " +
     * Field.LOCALCONTACTID + "=?", args); nativeInfoList.clear(); while
     * (c.moveToNext()) { ContactDetail detailInfo = new ContactDetail(); if
     * (!c.isNull(QUERY_COLUMN_LOCALDETAILID)) { detailInfo.localDetailID =
     * c.getLong(QUERY_COLUMN_LOCALDETAILID); } if (!c.isNull(QUERY_COLUMN_KEY))
     * { detailInfo.key =
     * ContactDetail.DetailKeys.values()[c.getInt(QUERY_COLUMN_KEY)]; } if
     * (!c.isNull(QUERY_COLUMN_SERVERDETAILID)) { detailInfo.unique_id =
     * c.getLong(QUERY_COLUMN_SERVERDETAILID); } if
     * (!c.isNull(QUERY_COLUMN_NATIVECONTACTID)) { detailInfo.nativeContactId =
     * c.getInt(QUERY_COLUMN_NATIVECONTACTID); } if
     * (!c.isNull(QUERY_COLUMN_NATIVEDETAILID)) { detailInfo.nativeDetailId =
     * c.getInt(QUERY_COLUMN_NATIVEDETAILID); } if
     * (!c.isNull(QUERY_COLUMN_NATIVEVAL1)) { detailInfo.nativeVal1 =
     * c.getString(QUERY_COLUMN_NATIVEVAL1); } if
     * (!c.isNull(QUERY_COLUMN_NATIVEVAL2)) { detailInfo.nativeVal2 =
     * c.getString(QUERY_COLUMN_NATIVEVAL2); } if
     * (!c.isNull(QUERY_COLUMN_NATIVEVAL3)) { detailInfo.nativeVal3 =
     * c.getString(QUERY_COLUMN_NATIVEVAL3); } if
     * (!c.isNull(QUERY_COLUMN_NATIVESYNCCONTACTID)) {
     * detailInfo.syncNativeContactId =
     * c.getInt(QUERY_COLUMN_NATIVESYNCCONTACTID); }
     * nativeInfoList.add(detailInfo); } } catch (SQLException e) {
     * LogUtils.logE("ContactDetailsTable.fetchNativeInfo() - error:\n", e);
     * return ServiceStatus.ERROR_DATABASE_CORRUPT; } finally {
     * CloseUtils.close(c); c = null; } return ServiceStatus.SUCCESS; }
     */

    /**
     * Returns a cursor of all the contact details that match a specific key and
     * value. Used by the {@link #findNativeContact(Contact, SQLiteDatabase)}
     * method to find all the contacts by name, phone number or email.
     * 
     * @param value The string value to match.
     * @param key The key to match.
     * @param readableDb A readable SQLite database object
     * @return A cursor containing the local detail ID and local contact ID for
     *         each result.
     */
    private static Cursor findDetailByKey(String value, ContactDetail.DetailKeys key,
            SQLiteDatabase readableDb) {
        try {
            if (value == null || key == null) {
                return null;
            }
            final String searchValue = DatabaseUtils.sqlEscapeString(value);
            return readableDb.rawQuery("SELECT " + Field.DETAILLOCALID + "," + Field.LOCALCONTACTID
                    + " FROM " + TABLE_NAME + " WHERE " + Field.KEY + "=" + key.ordinal() + " AND "
                    + Field.STRINGVAL + "=" + searchValue + " AND " + Field.NATIVECONTACTID
                    + " IS NULL", null);
        } catch (SQLException e) {
            LogUtils.logE("ContactDetailsTable.findDetailByKey() SQLException - "
                    + "Unable to search for native contact ", e);
            return null;
        }

    }

    /**
     * This method finds the localContactId corresponding to the wanted Key and
     * StringVal fields.
     * 
     * @param value - StringVal
     * @param key - Key
     * @param readableDb A readable SQLite database object
     * @return - localContactId (it is unique)
     */
    public static long findLocalContactIdByKey(String networkName, String value,
            ContactDetail.DetailKeys key, SQLiteDatabase readableDb) throws SQLException {
        long localContactId = -1;
        Cursor c = null;
        try {
            if (value == null || key == null) {
                return localContactId;
            }
            value = DatabaseUtils.sqlEscapeString(value);
            networkName = DatabaseUtils.sqlEscapeString(networkName);

            StringBuffer query = StringBufferPool.getStringBuffer(SQLKeys.SELECT);
            query.append(Field.LOCALCONTACTID).append(SQLKeys.FROM).append(TABLE_NAME).append(
                    SQLKeys.WHERE).append(Field.KEY).append(SQLKeys.EQUALS).append(key.ordinal())
                    .append(SQLKeys.AND).append(Field.STRINGVAL).append(SQLKeys.EQUALS).append(
                            value);

            if (PresenceDbUtils.notNullOrBlank(networkName)) {
                query.append(SQLKeys.AND).append(Field.ALT).append(SQLKeys.EQUALS).append(
                        networkName);
            }
            c = readableDb.rawQuery(StringBufferPool.toStringThenRelease(query), null);
            while (c.moveToNext()) {
                if (!c.isNull(0)) {
                    localContactId = c.getLong(0);
                    break;
                }
            }
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return localContactId;
    }

    /**
     * This method finds the chat id corresponding to the wanted Alt,
     * LocalContactId and Key fields
     * 
     * @param networkName - network name (google,MSN)
     * @param localContactId - localContactId of the contcat
     * @param readableDb A readable SQLite database object
     * @return - chatId for this network(it is unique)
     */
    public static String findChatIdByLocalContactIdAndNetwork(String networkName,
            long localContactId, SQLiteDatabase readableDb) throws NullPointerException,
            SQLException {
        if (readableDb == null) {
            throw new NullPointerException(
                    "ContactDetailsTable.findChatIdByLocalContactIdAndNetwork(): The database passed in was null!");
        }
        String chatId = null;
        Cursor c = null;
        try {
            if (localContactId == -1 || networkName == null) {
                return chatId;
            }
            networkName = DatabaseUtils.sqlEscapeString(networkName);
            String query = "SELECT "
                    + Field.STRINGVAL
                    + " FROM "
                    + TABLE_NAME
                    + " WHERE "
                    + Field.KEY
                    + "="
                    + ContactDetail.DetailKeys.VCARD_IMADDRESS.ordinal()
                    + " AND "
                    + Field.LOCALCONTACTID
                    + "="
                    + (PresenceDbUtils.notNullOrBlank(networkName) ? localContactId + " AND "
                            + Field.ALT + "=" + networkName : String.valueOf(localContactId));
            c = readableDb.rawQuery(query, null);
            while (c.moveToNext()) {
                if (!c.isNull(0)) {
                    chatId = c.getString(0);
                    break;
                }
            }
        } finally {
            CloseUtils.close(c);
        }
        return chatId;
    }

    /**
     * Compares the details given with a specific contact in the database. If
     * the contacts match a list of native sync information is returned.
     * 
     * @param c Contact to compare
     * @param localContactId Contact in the database to compare with
     * @param detailIdList A list which will be populated if a match is found.
     * @param readableDb A readable SQLite database object
     * @return true if a match is found, false otherwise
     */
    public static boolean doContactsMatch(Contact c, long localContactId,
            List<NativeIdInfo> detailIdList, SQLiteDatabase readableDb) {
        List<ContactDetail> srcDetails = new ArrayList<ContactDetail>();
        ServiceStatus status = fetchContactDetails(localContactId, srcDetails, readableDb);
        if (ServiceStatus.SUCCESS != status) {
            return false;
        }
        detailIdList.clear();
        for (int i = 0; i < c.details.size(); i++) {
            final ContactDetail destDetail = c.details.get(i);
            Long foundDetailId = null;
            for (int j = 0; j < srcDetails.size(); j++) {
                final ContactDetail srcDetail = srcDetails.get(j);
                if (srcDetail.changeID == null && srcDetail.key != null
                        && srcDetail.key.equals(destDetail.key)) {
                    if (srcDetail.value != null && srcDetail.value.equals(destDetail.value)) {
                        foundDetailId = srcDetail.localDetailID;
                        srcDetail.changeID = 1L;
                        break;
                    }
                    if (srcDetail.value == null && destDetail.value == null) {
                        foundDetailId = srcDetail.localDetailID;
                        srcDetail.changeID = 1L;
                        break;
                    }
                    if (srcDetail.key == ContactDetail.DetailKeys.VCARD_NAME
                            && srcDetail.value != null
                            && srcDetail.value.indexOf(VCardHelper.LIST_SEPARATOR) < 0) {
                        VCardHelper.Name name1 = srcDetail.getName();
                        VCardHelper.Name name2 = destDetail.getName();
                        if (name1 != null && name2 != null
                                && name1.toString().equals(name2.toString())) {
                            foundDetailId = srcDetail.localDetailID;
                            srcDetail.changeID = 1L;
                        }
                    }
                    if (srcDetail.key == ContactDetail.DetailKeys.VCARD_ADDRESS
                            && srcDetail.value != null
                            && srcDetail.value.indexOf(VCardHelper.LIST_SEPARATOR) < 0) {
                        VCardHelper.PostalAddress addr1 = srcDetail.getPostalAddress();
                        VCardHelper.PostalAddress addr2 = destDetail.getPostalAddress();
                        if (addr1 != null && addr2 != null
                                && addr1.toString().equals(addr2.toString())) {
                            foundDetailId = srcDetail.localDetailID;
                            srcDetail.changeID = 1L;
                        }
                    }
                }
            }
            if (foundDetailId == null) {
                if (destDetail.value != null && destDetail.value.length() > 0) {
                    LogUtils.logD("ContactDetailTable.doContactsMatch - The detail "
                            + destDetail.key + ", <" + destDetail.value + "> was not found");
                    for (int j = 0; j < srcDetails.size(); j++) {
                        final ContactDetail srcDetail = srcDetails.get(j);
                        if (srcDetail.key != null && srcDetail.key.equals(destDetail.key)) {
                            LogUtils.logD("ContactDetailTable.doContactsMatch - No Match Key: "
                                    + srcDetail.key + ", Value: <" + srcDetail.value + ">, Used: "
                                    + srcDetail.changeID);
                        }
                    }
                    return false;
                }
            } else {
                NativeIdInfo nativeIdInfo = new NativeIdInfo();
                nativeIdInfo.localId = foundDetailId;
                nativeIdInfo.nativeContactId = c.nativeContactId;
                nativeIdInfo.nativeDetailId = destDetail.nativeDetailId;
                nativeIdInfo.nativeVal1 = destDetail.nativeVal1;
                nativeIdInfo.nativeVal2 = destDetail.nativeVal2;
                nativeIdInfo.nativeVal3 = destDetail.nativeVal3;
                detailIdList.add(nativeIdInfo);
            }
        }
        for (int j = 0; j < srcDetails.size(); j++) {
            final ContactDetail srcDetail = srcDetails.get(j);
            if (srcDetail.nativeContactId == null) {
                boolean found = false;
                for (int i = 0; i < detailIdList.size(); i++) {
                    NativeIdInfo info = detailIdList.get(i);
                    if (info.localId == srcDetail.localDetailID.longValue()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    NativeIdInfo nativeIdInfo = new NativeIdInfo();
                    nativeIdInfo.localId = srcDetail.localDetailID;
                    nativeIdInfo.nativeContactId = srcDetail.nativeContactId;
                    nativeIdInfo.nativeDetailId = srcDetail.nativeDetailId;
                    nativeIdInfo.nativeVal1 = srcDetail.nativeVal1;
                    nativeIdInfo.nativeVal2 = srcDetail.nativeVal2;
                    nativeIdInfo.nativeVal3 = srcDetail.nativeVal3;
                    nativeIdInfo.syncNativeContactId = c.nativeContactId;
                    detailIdList.add(nativeIdInfo);
                }
            }
        }
        return true;
    }

    /**
     * Searches the contact details table for a contact from the native
     * phonebook. If a match is found, the native sync information is transfered
     * from the given contact into the matching database contact. Tries to match
     * in the following sequence: 1) If there is a name, match by name 2)
     * Otherwise, if there is a phone number, match by number 3) Otherwise, if
     * there is an email, match by email 4) Otherwise return false For a match
     * to occur, all given contact details must be identical to those in the
     * database. There may be more details in the database but this won't change
     * the result.
     * 
     * @param c The contact to match
     * @param writableDb A writable SQLite database object
     * @return true if the contact was found, false otherwise
     */
    public static boolean findNativeContact(Contact c, SQLiteDatabase writableDb) {
        String name = null;
        String phone = null;
        String email = null;
        List<ContactIdInfo> contactIdList = new ArrayList<ContactIdInfo>();
        List<NativeIdInfo> detailIdList = new ArrayList<NativeIdInfo>();
        for (int i = 0; i < c.details.size(); i++) {
            if (c.details.get(i).key == ContactDetail.DetailKeys.VCARD_NICKNAME) {
                name = c.details.get(i).getValue();
                if (name != null && name.length() > 0) {
                    break;
                }
            }
            if (c.details.get(i).key == ContactDetail.DetailKeys.VCARD_PHONE) {
                if (phone == null || phone.length() > 0) {
                    phone = c.details.get(i).getValue();
                }
            }
            if (c.details.get(i).key == ContactDetail.DetailKeys.VCARD_EMAIL) {
                if (email == null || email.length() > 0) {
                    email = c.details.get(i).getValue();
                }
            }
        }
        Cursor candidateListCursor = null;
        if (name != null && name.length() > 0) {
            LogUtils.logD("ContactDetailsTable.findNativeContact - "
                    + "Searching for contact called " + name);
            candidateListCursor = findDetailByKey(name, ContactDetail.DetailKeys.VCARD_NICKNAME,
                    writableDb);
        } else if (phone != null && phone.length() > 0) {
            LogUtils.logD("ContactDetailsTable.findNativeContact - "
                    + "Searching for contact with phone " + phone);
            candidateListCursor = findDetailByKey(phone, ContactDetail.DetailKeys.VCARD_PHONE,
                    writableDb);
        } else if (email != null && email.length() > 0) {
            LogUtils.logD("ContactDetailsTable.findNativeContact - "
                    + "Searching for contact with email " + email);
            candidateListCursor = findDetailByKey(email, ContactDetail.DetailKeys.VCARD_EMAIL,
                    writableDb);
        }
        List<NativeIdInfo> tempDetailIdList = new ArrayList<NativeIdInfo>();
        List<NativeIdInfo> currentDetailIdList = new ArrayList<NativeIdInfo>();
        Integer minNoOfDetails = null;
        Long chosenContactId = null;
        if (candidateListCursor != null) {
            while (candidateListCursor.moveToNext()) {
                long localContactId = candidateListCursor.getLong(1);
                tempDetailIdList.clear();
                if (doContactsMatch(c, localContactId, tempDetailIdList, writableDb)) {
                    if (minNoOfDetails == null
                            || minNoOfDetails.intValue() > tempDetailIdList.size()) {
                        if (ContactsTable.fetchSyncToPhone(localContactId, writableDb)) {
                            minNoOfDetails = tempDetailIdList.size();
                            chosenContactId = localContactId;
                            currentDetailIdList.clear();
                            currentDetailIdList.addAll(tempDetailIdList);
                        }
                    }
                }
            }
            candidateListCursor.close();
            if (chosenContactId != null) {
                LogUtils.logD("ContactDetailsTable.findNativeContact - "
                        + "Found contact (no need to add)");
                ContactIdInfo contactIdInfo = new ContactIdInfo();
                contactIdInfo.localId = chosenContactId;
                contactIdInfo.nativeId = c.nativeContactId;
                contactIdList.add(contactIdInfo);
                detailIdList.addAll(currentDetailIdList);
                // Update contact IDs of the contacts which are already in the
                // database
                ServiceStatus status = ContactsTable.syncSetNativeIds(contactIdList, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    return false;
                }
                status = ContactSummaryTable.syncSetNativeIds(contactIdList, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    return false;
                }
                status = ContactDetailsTable.syncSetNativeIds(detailIdList, writableDb);
                if (ServiceStatus.SUCCESS != status) {
                    return false;
                }
                return true;
            }
        }
        LogUtils.logD("ContactDetailsTable.findNativeContact - Contact not found (will be added)");
        return false;
    }

    /**
     * Fetches all the details that have changed and need to be synced with the
     * server. Details associated with new contacts are returned separately,
     * this is determined by a parameter. The
     * {@link #syncSetServerIds(List, SQLiteDatabase)} method is used to mark
     * the details as up to date, once the sync has completed.
     * 
     * @param readableDb A readable SQLite database object
     * @param newContacts true if details associated with new contacts should be
     *            returned, otherwise modified details are returned.
     * @return A cursor which can be passed into the
     *         {@link #syncServerGetNextNewContactDetails(Cursor, List, int)}
     *         method.
     */
    public static Cursor syncServerFetchContactChanges(SQLiteDatabase readableDb,
            boolean newContacts) {
        try {
            String statusMatch = "";
            if (newContacts) {
                statusMatch = Field.SERVERSYNCCONTACTID + " IS NULL";
            } else {
                /** Note this won't return NULLs. **/
                statusMatch = Field.SERVERSYNCCONTACTID + "<>-1";
            }
            return readableDb.rawQuery("SELECT " + Field.LOCALCONTACTID + ","
                    + Field.SERVERSYNCCONTACTID + "," + Field.DETAILLOCALID + ","
                    + Field.DETAILSERVERID + "," + Field.KEY + "," + Field.TYPE + ","
                    + Field.STRINGVAL + "," + Field.ORDER + "," + Field.PHOTOURL + " FROM "
                    + TABLE_NAME + " WHERE " + statusMatch, null);
        } catch (SQLException e) {
            LogUtils.logE("ContactDetailsTable.syncServerFetchContactChanges() SQLException - "
                    + "Unable to search for native contact ", e);
            return null;
        }
    }

    /**
     * Returns the next batch of contacts which need to be added on the server.
     * The {@link #syncServerFetchContactChanges(SQLiteDatabase, boolean)}
     * method is used to retrieve the cursor initially, then this function can
     * be called many times until all the contacts have been fetched. When the
     * list returned from this method is empty the cursor has reached the end
     * and the sync is complete.
     * 
     * @param c The cursor (see description above)
     * @param contactList Will be filled with contacts that need to be added to
     *            the server
     * @param maxContactsToFetch Maximum number of contacts to return in the
     *            list. The function can be called in a loop until all the
     *            contacts have been retrieved.
     */
    public static void syncServerGetNextNewContactDetails(Cursor c, List<Contact> contactList,
            int maxContactsToFetch) {
        final int QUERY_COLUMN_LOCALCONTACTID = 0;
        final int QUERY_COLUMN_SERVERSYNCCONTACTID = 1;
        final int QUERY_COLUMN_LOCALDETAILID = 2;
        final int QUERY_COLUMN_SERVERDETAILID = 3;
        final int QUERY_COLUMN_KEY = 4;
        final int QUERY_COLUMN_KEYTYPE = 5;
        final int QUERY_COLUMN_VAL = 6;
        final int QUERY_COLUMN_ORDER = 7;
        final int QUERY_COLUMN_PHOTOURL = 8;
        contactList.clear();
        Contact currentContact = null;
        while (c.moveToNext()) {
            final ContactDetail detail = new ContactDetail();
            if (!c.isNull(QUERY_COLUMN_LOCALCONTACTID)) {
                detail.localContactID = c.getLong(QUERY_COLUMN_LOCALCONTACTID);
            }
            if (!c.isNull(QUERY_COLUMN_SERVERSYNCCONTACTID)) {
                detail.serverContactId = c.getLong(QUERY_COLUMN_SERVERSYNCCONTACTID);
            }
            if (currentContact == null
                    || !currentContact.localContactID.equals(detail.localContactID)) {
                if (contactList.size() >= maxContactsToFetch) {
                    if (currentContact != null) {
                        c.moveToPrevious();
                    }
                    break;
                }
                currentContact = new Contact();
                currentContact.localContactID = detail.localContactID;
                if (detail.serverContactId == null) {
                    currentContact.synctophone = true;
                }
                currentContact.contactID = detail.serverContactId;
                contactList.add(currentContact);
            }
            if (!c.isNull(QUERY_COLUMN_LOCALDETAILID)) {
                detail.localDetailID = c.getLong(QUERY_COLUMN_LOCALDETAILID);
            }
            if (!c.isNull(QUERY_COLUMN_SERVERDETAILID)) {
                detail.unique_id = c.getLong(QUERY_COLUMN_SERVERDETAILID);
            }
            detail.key = ContactDetail.DetailKeys.values()[c.getInt(QUERY_COLUMN_KEY)];
            if (!c.isNull(QUERY_COLUMN_KEYTYPE)) {
                detail.keyType = ContactDetail.DetailKeyTypes.values()[c
                        .getInt(QUERY_COLUMN_KEYTYPE)];
            }
            detail.value = c.getString(QUERY_COLUMN_VAL);
            if (!c.isNull(QUERY_COLUMN_ORDER)) {
                detail.order = c.getInt(QUERY_COLUMN_ORDER);
            }
            if (!c.isNull(QUERY_COLUMN_PHOTOURL)) {
                detail.photo_url = c.getString(QUERY_COLUMN_PHOTOURL);
            }
            currentContact.details.add(detail);
        }
    }

    /**
     * Retrieves the total number of details that have changed and need to be
     * synced with the server. Includes both new and modified contacts.
     * 
     * @param db Readable SQLiteDatabase object.
     * @return The number of details.
     */
    public static int syncServerFetchNoOfChanges(final SQLiteDatabase db) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactDetailsTable." + "syncServerFetchNoOfChanges()");
        }

        Cursor cursor = null;
        try {
            /** Return all values from this table (including new contacts) **/
            cursor = db.rawQuery("SELECT COUNT(distinct " + Field.LOCALCONTACTID + ") FROM "
                    + TABLE_NAME + " WHERE " + Field.SERVERSYNCCONTACTID + "<>-1 OR "
                    + Field.SERVERSYNCCONTACTID + " IS NULL", null);
            if (cursor.moveToFirst()) {
                int result = cursor.getInt(0);
                return result;
            } else {
                LogUtils.logE("ContactDetailsTable." + "syncServerFetchNoOfChanges() COUNT(*) "
                        + "should not return an empty cursor, returning 0");
                return 0;
            }

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /**
     * Fetches all the details that have changed and need to be synced with the
     * native. Details associated with new contacts are returned separately,
     * this is determined by a parameter. The
     * {@link #syncSetNativeIds(List, SQLiteDatabase)} method is used to mark
     * the details as up to date, once the sync has completed.
     * 
     * @param readableDb A readable SQLite database object
     * @param newContacts true if details associated with new contacts should be
     *            returned, otherwise modified details are returned.
     * @return A cursor which can be passed into the
     *         {@link #syncNativeGetNextNewContactDetails(Cursor, List, int)}
     *         method.
     */
    public static Cursor syncNativeFetchContactChanges(SQLiteDatabase readableDb,
            boolean newContacts) {
        try {
            String statusMatch = "";
            if (newContacts) {
                statusMatch = Field.NATIVESYNCCONTACTID + " IS NULL";
            } else {
                /** Note this won't return NULLs. **/
                statusMatch = Field.NATIVESYNCCONTACTID + "<>-1";
            }
            return readableDb.rawQuery("SELECT " + Field.LOCALCONTACTID + ","
                    + Field.NATIVECONTACTID + "," + Field.NATIVESYNCCONTACTID + ","
                    + Field.DETAILLOCALID + "," + Field.NATIVEDETAILID + "," + Field.KEY + ","
                    + Field.TYPE + "," + Field.STRINGVAL + "," + Field.ORDER + ","
                    + Field.NATIVEDETAILVAL1 + "," + Field.NATIVEDETAILVAL2 + ","
                    + Field.NATIVEDETAILVAL3 + " FROM " + TABLE_NAME + " WHERE " + statusMatch
                    + " ORDER BY " + Field.LOCALCONTACTID, null);
        } catch (SQLException e) {
            LogUtils.logE("ContactDetailsTable.findDetailByKey() SQLException - "
                    + "Unable to search for native contact ", e);
            return null;
        }
    }

    /**
     * Returns the next batch of contacts which need to be added on the native
     * database. The
     * {@link #syncNativeFetchContactChanges(SQLiteDatabase, boolean)} method is
     * used to retrieve the cursor initially, then this function can be called
     * many times until all the contacts have been fetched. When the list
     * returned from this method is empty the cursor has reached the end and the
     * sync is complete.
     * 
     * @param c The cursor (see description above)
     * @param contactList Will be filled with contacts that need to be added to
     *            the native
     * @param maxContactsToFetch Maximum number of contacts to return in the
     *            list. The function can be called in a loop until all the
     *            contacts have been retrieved.
     * @return true if successful, false if an error occurred (there seems to be
     *         a defect in Android which occasionally causes this to fail due to
     *         bad cursor state. The workaround is to retry the operation).
     */
    public static boolean syncNativeGetNextNewContactDetails(Cursor c, List<Contact> contactList,
            int maxContactsToFetch) {
        final int QUERY_COLUMN_LOCALCONTACTID = 0;
        final int QUERY_COLUMN_NATIVECONTACTID = 1;
        final int QUERY_COLUMN_NATIVESYNCCONTACTID = 2;
        final int QUERY_COLUMN_LOCALDETAILID = 3;
        final int QUERY_COLUMN_NATIVEDETAILID = 4;
        final int QUERY_COLUMN_KEY = 5;
        final int QUERY_COLUMN_KEYTYPE = 6;
        final int QUERY_COLUMN_VAL = 7;
        final int QUERY_COLUMN_ORDER = 8;
        final int QUERY_COLUMN_NATIVEVAL1 = 9;
        final int QUERY_COLUMN_NATIVEVAL2 = 10;
        final int QUERY_COLUMN_NATIVEVAL3 = 11;
        try {
            contactList.clear();
            Contact currentContact = null;
            while (c.moveToNext()) {
                final ContactDetail detail = new ContactDetail();
                detail.localContactID = c.getLong(QUERY_COLUMN_LOCALCONTACTID);
                if (!c.isNull(QUERY_COLUMN_NATIVECONTACTID)) {
                    detail.nativeContactId = c.getInt(QUERY_COLUMN_NATIVECONTACTID);
                } else {
                    detail.nativeContactId = null;
                }
                if (currentContact == null
                        || !currentContact.localContactID.equals(detail.localContactID)) {
                    if (contactList.size() >= maxContactsToFetch) {
                        if (currentContact != null) {
                            c.moveToPrevious();
                        }
                        break;
                    }
                    currentContact = new Contact();
                    currentContact.localContactID = detail.localContactID;
                    currentContact.nativeContactId = detail.nativeContactId;
                    contactList.add(currentContact);
                }
                if (!c.isNull(QUERY_COLUMN_NATIVESYNCCONTACTID)) {
                    detail.syncNativeContactId = c.getInt(QUERY_COLUMN_NATIVESYNCCONTACTID);
                } else {
                    detail.syncNativeContactId = null;
                }
                if (!c.isNull(QUERY_COLUMN_LOCALDETAILID)) {
                    detail.localDetailID = c.getLong(QUERY_COLUMN_LOCALDETAILID);
                }
                if (!c.isNull(QUERY_COLUMN_NATIVEDETAILID)) {
                    detail.nativeDetailId = c.getInt(QUERY_COLUMN_NATIVEDETAILID);
                } else {
                    detail.nativeDetailId = null;
                }
                detail.key = ContactDetail.DetailKeys.values()[c.getInt(QUERY_COLUMN_KEY)];
                if (!c.isNull(QUERY_COLUMN_KEYTYPE)) {
                    detail.keyType = ContactDetail.DetailKeyTypes.values()[c
                            .getInt(QUERY_COLUMN_KEYTYPE)];
                }
                detail.value = c.getString(QUERY_COLUMN_VAL);
                if (!c.isNull(QUERY_COLUMN_ORDER)) {
                    detail.order = c.getInt(QUERY_COLUMN_ORDER);
                }
                if (!c.isNull(QUERY_COLUMN_NATIVEVAL1)) {
                    detail.nativeVal1 = c.getString(QUERY_COLUMN_NATIVEVAL1);
                }
                if (!c.isNull(QUERY_COLUMN_NATIVEVAL2)) {
                    detail.nativeVal2 = c.getString(QUERY_COLUMN_NATIVEVAL2);
                }
                if (!c.isNull(QUERY_COLUMN_NATIVEVAL3)) {
                    detail.nativeVal3 = c.getString(QUERY_COLUMN_NATIVEVAL3);
                }
                currentContact.details.add(detail);
            }
            return true;
        } catch (IllegalStateException e) {
            LogUtils.logE("ContactDetailsTable.syncNativeGetNextNewContactDetails - "
                    + "Unable to fetch modified contacts from people\n" + e);
            c.requery();
            return false;
        }
    }

    /**
     * Retrieves the total number of details that have changed and need to be
     * synced with the native database. Does not include new contacts.
     * 
     * @param readableDb A readable SQLite database object
     * @return The number of details.
     */
    public static int syncNativeFetchNoOfChanges(SQLiteDatabase readableDb) throws SQLException {
        if (Settings.ENABLED_DATABASE_TRACE)
            DatabaseHelper.trace(false, "ContactDetailsTable.fetchNoOfContactDetailChanges()");
        int noOfChanges = 0;
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE "
                    + Field.NATIVESYNCCONTACTID + "<>-1", null);
            if (c.moveToFirst()) {
                noOfChanges = c.getInt(0);
            }
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return noOfChanges;
    }

    /**
     * Remove all preferred details from contact for a particular key.
     * 
     * @param localContactID Identifies the contact in the database
     * @param key The contact detail key (phone, email, address, etc.)
     * @param writableDb A writable SQLite database object
     */
    public static boolean removePreferred(Long localContactID, DetailKeys key,
            SQLiteDatabase writableDb) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(Field.ORDER.toString(), ContactDetail.ORDER_NORMAL);
            String[] args = {
                    String.valueOf(localContactID), String.valueOf(key.ordinal())
            };
            writableDb.update(TABLE_NAME, cv, Field.LOCALCONTACTID + "=? AND " + Field.KEY + "=?",
                    args);
            return true;
        } catch (SQLException e) {
            LogUtils.logE("ContactDetailsTable.removePreferred - "
                    + "Unable to clear preferred details, error:\n" + e);
            return false;
        }
    }
    
    /**
     * Maps a ContactChange flag to a ContactDetail key type.
     * 
     * @see ContactChange#FLAG_XXX
     * @see ContactDetail#DetailKeyTypes
     * 
     * @param flag the ContactChange flag to convert
     * @return the ContactDetail key type equivalent
     */
    public static int mapContactChangeFlagToInternalType(int flag) {
       
        // FIXME: We may be losing data at this stage because when the type is
        // a mix of HOME and FAX, it gets converted into FAX...
        
        int internalType = DetailKeyTypes.UNKNOWN.ordinal();
        
        if ((flag & ContactChange.FLAG_HOME) == ContactChange.FLAG_HOME) {
            
            internalType = DetailKeyTypes.HOME.ordinal();
            
        } else if ((flag & ContactChange.FLAG_CELL) == ContactChange.FLAG_CELL) {
            
            internalType = DetailKeyTypes.CELL.ordinal();
            
        } else if ((flag & ContactChange.FLAG_WORK) == ContactChange.FLAG_WORK) {
            
            internalType = DetailKeyTypes.WORK.ordinal();

        } else if ((flag & ContactChange.FLAG_FAX) == ContactChange.FLAG_FAX) {
            
            internalType = DetailKeyTypes.FAX.ordinal();

        } else if ((flag & ContactChange.FLAG_BIRTHDAY) == ContactChange.FLAG_BIRTHDAY) {
            
            internalType = DetailKeyTypes.BIRTHDAY.ordinal();

        }
        
        return internalType;
    }
    
    /**
     * Maps a ContactChange flag to a ContactDetail order.
     * 
     * @see ContactChange#FLAG_XXX
     * @see ContactDetail#ORDER_XXX
     * 
     * @param flag the ContactChange flag to convert
     * @return the ContactDetail order equivalent
     */
    public static int mapContactChangeFlagToInternalOrder(int flag) {
        
        if ((flag & ContactChange.FLAG_PREFERRED) == ContactChange.FLAG_PREFERRED) {
            return ContactDetail.ORDER_PREFERRED;
        }
        
        return ContactDetail.ORDER_NORMAL;
    }
    
    /**
     * Maps a ContactChange key to a ContactDetail key.
     * 
     * @see ContactChange#KEY_XXX
     * @see ContactDetail#DetailKeys
     * 
     * @param key the ContactChange key to convert
     * @return the ContactDetail key equivalent
     */
    public static int mapContactChangeKeyToInternalKey(int key) {
        
        return key == ContactChange.KEY_UNKNOWN ? ContactDetail.DetailKeys.UNKNOWN.ordinal() : key - 1;
    }
    
    /**
     * Maps ContactDetail key to ContactChange key.
     * 
     * @see ContactChange#KEY_XXX
     * @see ContactDetail#DetailKeys
     * 
     * @param key the ContactDetail key to convert
     * @return the ContactChange key equivalent
     */
    public static int mapInternalKeyToContactChangeKey(int key) {
        
        return key == ContactDetail.DetailKeys.UNKNOWN.ordinal() ? ContactChange.KEY_UNKNOWN : key + 1;
    }
    
    /**
     * Maps a ContactDetail type and an order to the ContactChange flag equivalent.
     * 
     * @see ContactChange#FLAG_XXX
     * @see ContactDetail#ORDER_XXX
     * @see ContactDetail#DetailKeyTypes
     * 
     * @param type the ContactDetail type
     * @param order the ContactDetail order
     * @return the ContactChange flag equivalent
     */
    public static int mapInternalTypeAndOrderToContactChangeFlag(int type, int order) {
        
        int flags = ContactChange.FLAG_NONE;
        
        if (order == ContactDetail.ORDER_PREFERRED) {
            
            flags |= ContactChange.FLAG_PREFERRED;
        }
        
        if (type == ContactDetail.DetailKeyTypes.CELL.ordinal()
         || type == ContactDetail.DetailKeyTypes.MOBILE.ordinal()) {
            
            flags |= ContactChange.FLAG_CELL;
        } else if (type == ContactDetail.DetailKeyTypes.HOME.ordinal()) {
            
            flags |= ContactChange.FLAG_HOME;
        } else if (type == ContactDetail.DetailKeyTypes.WORK.ordinal()) {
            
            flags |= ContactChange.FLAG_WORK;
        } else if (type == ContactDetail.DetailKeyTypes.BIRTHDAY.ordinal()) {
            
            flags |= ContactChange.FLAG_BIRTHDAY;
        } else if (type == ContactDetail.DetailKeyTypes.FAX.ordinal()) {
            
            flags |= ContactChange.FLAG_FAX;
        }

        return flags;
    }
    
    /**
     * Fills a ContentValues object with the provided ContactChange to later be used for database insert.
     * 
     * @param contactChange the ContactChange to get the values from
     * @param values the ContentValues object to fill
     */
    public static void prepareNativeContactDetailInsert(ContactChange contactChange, ContentValues values) {
        
        final long nativeContactId = contactChange.getNabContactId();
        
        // add the key
        values.put(Field.KEY.toString(), mapContactChangeKeyToInternalKey(contactChange.getKey()));
        
        // add the type
        values.put(Field.TYPE.toString(), mapContactChangeFlagToInternalType(contactChange.getFlags()));
        
        // add the string value
        values.put(Field.STRINGVAL.toString(), contactChange.getValue());
        
        // add the order number
        values.put(Field.ORDER.toString(), mapContactChangeFlagToInternalOrder(contactChange.getFlags()));
        
        // add the local contact id
        values.put(Field.LOCALCONTACTID.toString(), contactChange.getInternalContactId());
        
        // add the native ids if valid
        if (nativeContactId != ContactChange.INVALID_ID) {
        
            // add the native contact id
            values.put(Field.NATIVECONTACTID.toString(), nativeContactId);
            
            // add the native detail id
            values.put(Field.NATIVEDETAILID.toString(), contactChange.getNabDetailId());
            
            // set the NativeSyncContactId to -1 (means no need to sync that row to native)
            values.put(Field.NATIVESYNCCONTACTID.toString(), -1);
        }
    }
    
    /**
     * Adds the provided contact details to the ContactDetail table.
     * 
     * Note: the provided ContactChange are modified with the corresponding internalDetailId from the
     *       database insertion.
     * 
     * @see ContactChange
     * 
     * @param contactChange the contact details
     * @param writeableDb the db where to write
     * @return true if successful, false otherwise
     */
    public static boolean addNativeContactDetails(ContactChange[] contactChange, SQLiteDatabase writeableDb) {
        
        try {
            final ContentValues values = new ContentValues();
            ContactChange currentChange;
            
            // go through the ContactChange and add their details to the ContactDetails table
            for (int i = 0; i < contactChange.length; i++) {
                
                currentChange = contactChange[i];
                values.clear();
                prepareNativeContactDetailInsert(currentChange, values);
                currentChange.setInternalDetailId(writeableDb.insertOrThrow(TABLE_NAME, null, values));
            }
        }
        catch(Exception e) {
            
            return false;
        }
        
        return true;
    }

    /**
     * Gets an array of ContactChange from the contact's local id.
     * 
     * @see ContactChange
     * 
     * @param localId the local id of the contact to get
     * @return an array of ContactChange
     */
    public static ContactChange[] getContactChanges(long localId, boolean nativeSyncableOnly, SQLiteDatabase readableDb) {
        
        final String[] SELECTION = { String.valueOf(localId) };
        final String QUERY_STRING = nativeSyncableOnly ? QUERY_NATIVE_SYNCABLE_CONTACT_DETAILS_BY_LOCAL_ID : QUERY_CONTACT_DETAILS_BY_LOCAL_ID;
        Cursor cursor = null;
        
        try {
            
            cursor = readableDb.rawQuery(QUERY_STRING , SELECTION );
            
            if (cursor.getCount() > 0) {
                
                final ContactChange[] changes = new ContactChange[cursor.getCount()];
                int index = 0;
                
                while (cursor.moveToNext()) {
                    
                    // fill the ContactChange class with contact detail data if not empty
                    // StringVal=7
                    String value = cursor.getString(7);
                    if (value == null) value = ""; // prevent null pointer (however should the detail be even stored in People DB if null?)
                    // Key=6
                    final int key = cursor.isNull(6) ? ContactChange.KEY_UNKNOWN : mapInternalKeyToContactChangeKey(cursor.getInt(6));
                    // Type=4, OrderNo=5
                    final int flags = mapInternalTypeAndOrderToContactChangeFlag(cursor.isNull(4) ? 0 : cursor.getInt(4), cursor.getInt(5));
                    // create the change
                    final ContactChange change = new ContactChange(key, value, flags);
                    changes[index++] = change;
                    // LocalContactId=0
                    change.setInternalContactId(cursor.isNull(0) ? ContactChange.INVALID_ID : cursor.getLong(0));
                    // DetailLocalId=1
                    change.setInternalDetailId(cursor.isNull(1) ? ContactChange.INVALID_ID : cursor.getInt(1));
                    // NativeDetailId=2
                    change.setNabDetailId(cursor.isNull(2) ? ContactChange.INVALID_ID : cursor.getLong(2));
                    // NativeContactIdDup=3
                    change.setNabContactId(cursor.isNull(3) ? ContactChange.INVALID_ID : cursor.getLong(3));
                    // DetailServerId=8
                    change.setBackendDetailId(cursor.isNull(8) ? ContactChange.INVALID_ID : cursor.getLong(8));
                    
                    if (nativeSyncableOnly) {
                        
                        // in this mode, we have to tell if the detail is new or updated
                        if (change.getNabDetailId() == ContactChange.INVALID_ID) {
                            change.setType(ContactChange.TYPE_ADD_DETAIL);
                        } else {
                            change.setType(ContactChange.TYPE_UPDATE_DETAIL);
                        }
                    }
                }
                
                if (index == changes.length) {
                    
                    return changes;
                } else if (index > 0) {
                    
                    // there were some empty details, need to trim the array
                    final ContactChange[] trimmed = new ContactChange[index];
                    System.arraycopy(changes, 0, trimmed, 0, index);
                    
                    return trimmed;
                }
            }
            
        } catch (Exception e) {
            
            // what else can we do?
            LogUtils.logE("ContactDetailsTable.getContactChanges(): " + e);
        }
        finally {
            
            CursorUtils.closeCursor(cursor);
        }
        
        return null;
    }
    
    /**
     * LocalId = ?
     */
    private final static String SQL_STRING_LOCAL_ID_EQUAL_QUESTION_MARK = Field.DETAILLOCALID + " = ?";
    
    /**
     * Sets the detail as synchronized with native side.
     * 
     * @param localContactId the local id of the detail
     * @param nativeContactId the native contact (only needed for added details i.e. isNewlySynced = true)
     * @param nativeDetailId the native detail id (only needed for added details i.e. isNewlySynced = true)
     * @param writableDb the db where to write
     * @param isNewlySynced true if the detail as not been synchronized before, false if this is an update
     * @return true if sucessful, false otherwise
     */
    public static boolean setDetailSyncedWithNative(long localDetailId, long nativeContactId, long nativeDetailId, boolean isNewlySynced, SQLiteDatabase writableDb) {
        
        final ContentValues values = new ContentValues();
        
        if (isNewlySynced) {
            
            // in the isNewlySynced mode, we need to sync the native ids as well
            values.put(Field.NATIVECONTACTID.toString(), nativeContactId);
            values.put(Field.NATIVEDETAILID.toString(), nativeDetailId);
        }
        // set the sync with native flag to done (i.e. -1)
        values.put(Field.NATIVESYNCCONTACTID.toString(), -1);
        
        try {
            
            if (writableDb.update(TABLE_NAME, values, SQL_STRING_LOCAL_ID_EQUAL_QUESTION_MARK, new String[] { Long.toString(localDetailId) }) == 1) {
                
                return true;
            }
            
        } catch (Exception e) {
            
            LogUtils.logE("ContactsTable.setNativeContactId() Exception - " + e);
        }
        
        return false;
    }
}
