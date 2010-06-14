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

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the contact source database table.
 * This table stores a list of source information for each contact. This class
 * is never instantiated hence all methods must be static.
 * 
 * @version %I%, %G%
 */
public abstract class ContactSourceTable {
    /**
     * Name of the table as it appears in the database
     */
    private static final String TABLE_NAME = "ContactSources";

    /**
     * Represents the data stored in a record
     */
    private static class ContactSource {

        /**
         * The local contact ID which maps to contacts table
         */
        private Long mLocalContactId;

        /**
         * The source string
         */
        private String mSource;
    }

    /**
     * An enumeration of all the field names in the database.
     */
    private static enum Field {
        SOURCEID("SourceId"),
        LOCALCONTACTID("LocalContactId"),
        SOURCE("Source");

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
     * Create Contact Source Table.
     * 
     * @param writeableDb A writable SQLite database
     * @throws SQLException If an SQL compilation error occurs
     */
    public static void create(SQLiteDatabase writeableDb) throws SQLException {
        DatabaseHelper.trace(true, "ContactSourceTable.onCreate()");
        writeableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.SOURCEID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Field.LOCALCONTACTID + " LONG, "
                + Field.SOURCE + " STRING);");
    }

    /**
     * Returns a ContentValues object that can be used to insert or update a
     * contact source in the table.
     * 
     * @param contactSource A ContactSource object with the local Contact ID and
     *            source set appropriately.
     * @return The ContactValues object
     */
    private static ContentValues fillUpdateData(ContactSource contactSource) {
        DatabaseHelper.trace(false, "ContactSourceTable.fillUpdateData()");
        ContentValues contactValues = new ContentValues();
        contactValues.put(Field.LOCALCONTACTID.toString(), contactSource.mLocalContactId);
        contactValues.put(Field.SOURCE.toString(), contactSource.mSource);
        return contactValues;
    }

    /**
     * Add contact source to contact
     * 
     * @param localContactId The local Contact ID from Contacts table
     * @param source The source string from the server
     * @param writeableDb A writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addContactSource(long localContactId, String source,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactSourceTable.addContactSource()");
        try {
            final ContactSource contactSource = new ContactSource();
            contactSource.mLocalContactId = localContactId;
            contactSource.mSource = source;
            final ContentValues values = fillUpdateData(contactSource);
            if (writableDb.insertOrThrow(TABLE_NAME, null, values) < 0) {
                LogUtils.logE("ContactSourceTable.addContactSource() "
                        + "Unable to insert new contact source");
                return false;
            }
            return true;

        } catch (SQLException e) {
            LogUtils.logE("ContactSourceTable.addContactSource() SQLException - "
                    + "Unable to insert new contact source", e);
            return false;
        }
    }

    /**
     * Removes all sources associated with a specific contact.
     * 
     * @param localContactId The local Contact ID from Contacts table
     * @param writeableDb A writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean deleteAllContactSources(long localContactId, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactSourceTable.deleteAllContactSources()");
        try {
            if (writableDb.delete(TABLE_NAME, Field.LOCALCONTACTID + "=" + localContactId, null) < 0) {
                LogUtils.logE("ContactSourceTable.deleteAllContactSources() "
                        + "Unable to delete contact sources");
                return false;
            }
            return true;

        } catch (SQLException e) {
            LogUtils.logE("ContactSourceTable.deleteAllContactSources() SQLException - "
                    + "Unable to delete all contact sources", e);
            return false;
        }
    }

    /**
     * Fetches all the sources for the specified contact
     * 
     * @param localContactId The local Contact ID from Contacts table
     * @param sourceList A list that will be populated with the source strings
     * @param readableDb A readable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean fetchContactSources(long localContactId, List<String> sourceList,
            SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "ContactSourceTable.fetchContactSources() localContactId["
                    + localContactId + "]");
        }
        String[] args = {
            String.format("%d", localContactId)
        };
        Cursor c1 = null;
        sourceList.clear();
        try {
            c1 = readableDb.rawQuery("SELECT " + Field.SOURCE + " FROM " + TABLE_NAME + " WHERE "
                    + Field.LOCALCONTACTID + " = ?", args);
            while (c1.moveToNext()) {
                if (!c1.isNull(0)) {
                    sourceList.add(c1.getString(0));
                }
            }
        } catch (SQLiteException e) {
            LogUtils.logE("ContactSourceTable.fetchContactSources() "
                    + "Exception - Unable to fetch contact sources", e);
            return false;
        } finally {
            CloseUtils.close(c1);
            c1 = null;
        }

        return true;
    }
}
