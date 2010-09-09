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

package com.vodafone360.people.service.aidl;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.ContactChangeLogTable;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactGroupsTable;
import com.vodafone360.people.database.tables.ContactSourceTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ConversationsTable;
import com.vodafone360.people.database.tables.GroupsTable;
import com.vodafone360.people.database.tables.NativeChangeLogTable;
import com.vodafone360.people.database.tables.PresenceTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.utils.LogUtils;

/***
 * This is an experimental AIDL API, which is currently unsupported by Vodafone
 * and disabled by default.
 *
 * This class provides outside access to the entire 360-Engine database
 * for 3rd party processes. Currently, the only access control is the
 * on/off switch set in config.properties at build time, but we plan
 * to implement a new model for this. This is a basic content provider.
 *
 * Since the DatabaseProvider can be called by any other process
 * (and thus any other thread), we need to implement methods accessible
 * from outside as thread-safe.
 */
public class DatabaseProvider extends ContentProvider {

    /** Content URI. **/
    public static final Uri CONTENT_URI = Uri.parse("content://"
            + Intents.DATABASE_URI);
    /**
     * List of table names, used to check whether the incoming query matches to
     * any table in the database.
     */
    private static final String [] TABLE_NAMES = {
        ActivitiesTable.TABLE_NAME,
        ContactChangeLogTable.TABLE_NAME,
        ContactDetailsTable.TABLE_NAME,
        ContactGroupsTable.TABLE_NAME,
        ContactSourceTable.TABLE_NAME,
        ContactsTable.TABLE_NAME,
        ContactSummaryTable.TABLE_NAME,
        ConversationsTable.TABLE_NAME,
        GroupsTable.TABLE_NAME,
        NativeChangeLogTable.TABLE_NAME,
        PresenceTable.TABLE_NAME,
        StateTable.TABLE_NAME
    };
    /** Android context. **/
    private Context mContext;
    /** Reference to DatabaseHelper. **/
    private DatabaseHelper mDatabaseHelper;

    /***
     * Delete entries in the database. Not yet supported.
     *
     * @param uri Not yet supported.
     * @param selection Not yet supported.
     * @param selectionArgs Not yet supported.
     * @return Not yet supported.
     */
    @Override
    public final int delete(final Uri uri, final String selection,
            final String[] selectionArgs) {
        return 0;
    }

    @Override
    public final String getType(final Uri uri) {
        return "unsupported";
    }

    /***
     * Insert entries in the database. Not yet supported.
     *
     * @param uri Not yet supported.
     * @param values Not yet supported.
     * @return Not yet supported.
     */
    @Override
    public final Uri insert(final Uri uri, final ContentValues values) {
        return null;
    }

    /***
     * Create the DatabaseProvider.
     *
     * @return TRUE if creation was successful.
     */
    @Override
    public final boolean onCreate() {
        mContext = getContext();
        mDatabaseHelper = new DatabaseHelper(mContext);

        return mDatabaseHelper != null;
    }

    /**
     * Allows direct access to the 360 Service database. A properly formulated
     * query will allow you to get any data you want from the service. Simply
     * use the table name (available from the documentation, or checking the
     * array at the top of this class).
     *
     * Example query:
     * "content://com.vodafone360.people.service.aidl.databaseaccess/Activities"
     *
     * This method is synchronised, as we need it to be thread safe.  I.e.
     * several concurrent queries could be made from multiple processes.
     *
     * @param uri Database URI.
     * @param projection Database query projection.
     * @param selection Database query selection.
     * @param selectionArgs Database query selectionArgs.
     * @param sortOrder Database query sortOrder.
     * @return Cursor containing database query result.
     */
    @Override
    public final Cursor query(final Uri uri, final String[] projection,
            final String selection, final String[] selectionArgs,
            final String sortOrder) {
        LogUtils.logI("DatabaseProvider.query() Received incoming query uri["
                + uri + "]");

        if (!SettingsManager.getBooleanProperty(Settings.ENABLE_AIDL_KEY)) {
            LogUtils.logI("DatabaseProvider.query() 360 Engine database not "
                    + "available to third parties (set at build time)");
            throw new RuntimeException("Looks like you tried to query the "
                    + "com.vodafone360.people database, but access was "
                    + "disabled at build time.");
        }

        /**
         * Return the result of a direct query to the database on the
         * appropriate table (set by URI).
         */
        String queriedTable = uri.getPath();
        if (queriedTable.startsWith("/")) {
            queriedTable = queriedTable.replaceFirst("/", "");
        }

        final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        boolean matchedATable = false;
        for (String table : TABLE_NAMES) {
            if (table.equals(queriedTable)) {
                LogUtils.logI("DatabaseProvider.query() Matched incoming URI "
                        + "query to the [" + table + "] table.");
                qBuilder.setTables(queriedTable);
                matchedATable = true;
                break;
            }
        }

        /*
         * If we found a table, perform the query as expected; otherwise throw
         * an exception. This might crash the client process, but
         * com.vodafone360.people is safe, i.e. the Exception gets parceled up
         * and passed out.
         */
        if (matchedATable) {
            return qBuilder.query(mDatabaseHelper.getReadableDatabase(),
                    projection, selection, selectionArgs, null, null,
                    sortOrder);
        } else {
            LogUtils.logE("DatabaseProvider.query() Oops! Someone tried to "
                    + "query a database table that wasn't there; passing out "
                    + "an exception");
            throw new IllegalArgumentException("DatabaseProvider.query() "
                    + "Couldn't find table [" + queriedTable + "], did you "
                    + "properly form your query?");
        }
    }

    /***
     * Updates entries in the database. Not yet supported.
     *
     * @param uri Not yet supported.
     * @param values Not yet supported.
     * @param selection Not yet supported.
     * @param selectionArgs Not yet supported.
     * @return Not yet supported.
     */
    @Override
    public final int update(final Uri uri, final ContentValues values,
            final String selection, final String[] selectionArgs) {
        return 0;
    }
}
