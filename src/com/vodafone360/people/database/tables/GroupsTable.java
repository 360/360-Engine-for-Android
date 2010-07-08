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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.vodafone360.people.R;
import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the Groups database table. The
 * groups table stores the information about each group from the server. This
 * class is never instantiated hence all methods must be static.
 * 
 * @version %I%, %G%
 */
public abstract class GroupsTable {
    
    /**
     * The name of the table as it appears in the database.
     */
    private static final String TABLE_NAME = "ZybGroups";

    /**
     * Special ID for the ALL group
     */
    public static final long GROUP_ALL = -1000;

    /**
     * Special ID for the ONLINE contacts group (to be added later)
     */
    protected static final long GROUP_ONLINE = -1001;

    /**
     * Special ID for the phonebook contacts group
     */
    protected static final long GROUP_PHONEBOOK = -1002;

    /**
     * Special ID for the connected friends group
     */
    protected static final long GROUP_CONNECTED_FRIENDS = 2;

    /**
     * An enumeration of all the field names in the database.
     */
    public static enum Field {
        LOCALGROUPID("LocalGroupId"),
        SERVERGROUPID("ServerGroupId"),
        GROUPTYPE("GroupType"),
        ISREADONLY("IsReadOnly"),
        REQUIRESLOCALISATION("RequiresLocalisation"),
        ISSYSTEMGROUP("IsSystemGroup"),
        ISSMARTGROUP("IsSmartGroup"),
        USERID("UserId"),
        NAME("Name"),
        IMAGEMIMETYPE("ImageMimeType"),
        IMAGEBYTES("ImageBytes"),
        COLOR("Color");

        /**
         * The name of the field as it appears in the database
         */
        private final String mField;

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
     * Creates Groups Table and populate it with system groups.
     * 
     * @param context A context for reading strings from the resources
     * @param writeableDb A writable SQLite database
     * @throws SQLException If an SQL compilation error occurs
     */
    public static void create(Context context, SQLiteDatabase writableDb) throws SQLException {
        DatabaseHelper.trace(true, "GroupsTable.create()");
        writableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.LOCALGROUPID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Field.SERVERGROUPID + " LONG, "
                + Field.GROUPTYPE + " INTEGER, " + Field.ISREADONLY + " BOOLEAN, "
                + Field.REQUIRESLOCALISATION + " BOOLEAN, " + Field.ISSYSTEMGROUP + " BOOLEAN, "
                + Field.ISSMARTGROUP + " BOOLEAN, " + Field.USERID + " LONG, " + Field.NAME
                + " TEXT, " + Field.IMAGEMIMETYPE + " TEXT, " + Field.IMAGEBYTES + " BINARY, "
                + Field.COLOR + " TEXT);");
        populateSystemGroups(context, writableDb);
    }

    /**
     * Fetches the list of table fields that can be injected into an SQL query
     * statement. The {@link #getQueryData(Cursor)} method can be used to obtain
     * the data from the query.
     * 
     * @return The query string
     * @see #getQueryData(Cursor).
     */
    private static String getFullQueryList() {
        return Field.LOCALGROUPID + ", " + Field.SERVERGROUPID + ", " + Field.GROUPTYPE + ", "
                + Field.ISREADONLY + ", " + Field.REQUIRESLOCALISATION + ", " + Field.ISSYSTEMGROUP
                + ", " + Field.ISSMARTGROUP + ", " + Field.USERID + ", " + Field.NAME + ", "
                + Field.IMAGEMIMETYPE + ", " + Field.IMAGEBYTES + ", " + Field.COLOR;
    }

    /**
     * Returns a full SQL query statement to fetch a set of groups from the
     * table. The {@link #getQueryData(Cursor)} method can be used to obtain the
     * data from the query.
     * 
     * @param whereClause An SQL where clause (without the "WHERE"). Cannot be
     *            null.
     * @return The query string
     * @see #getQueryData(Cursor).
     */
    private static String getQueryStringSql(String whereClause) {
        String whereString = "";
        if (whereClause != null) {
            whereString = " WHERE " + whereClause;
        }
        return "SELECT " + getFullQueryList() + " FROM " + TABLE_NAME + whereString;
    }

    /**
     * Column indices which match the query string returned by
     * {@link #getFullQueryList()}.
     */
    private static final int LOCALGROUPID = 0;

    private static final int SERVERGROUPID = 1;

    private static final int GROUPTYPE = 2;

    private static final int ISREADONLY = 3;

    private static final int REQUIRESLOCALISATION = 4;

    private static final int ISSYSTEMGROUP = 5;

    private static final int ISSMARTGROUP = 6;

    private static final int USERID = 7;

    private static final int NAME = 8;

    private static final int IMAGEMIMETYPE = 9;

    private static final int IMAGEBYTES = 10;

    private static final int COLOR = 11;

    /**
     * Fetches the group data from the current record of the given cursor.
     * 
     * @param c Cursor returned by one of the {@link #getFullQueryList()} based
     *            query methods.
     * @return Filled in GroupItem object
     */
    public static GroupItem getQueryData(Cursor c) {
        GroupItem group = new GroupItem();

        if (!c.isNull(LOCALGROUPID)) {
            group.mLocalGroupId = c.getLong(LOCALGROUPID);
        }
        if (!c.isNull(SERVERGROUPID)) {
            group.mId = c.getLong(SERVERGROUPID);
        }
        if (!c.isNull(GROUPTYPE)) {
            group.mGroupType = c.getInt(GROUPTYPE);
        }
        if (!c.isNull(ISREADONLY)) {
            group.mIsReadOnly = (c.getShort(ISREADONLY) == 0 ? false : true);
        }
        if (!c.isNull(REQUIRESLOCALISATION)) {
            group.mRequiresLocalisation = (c.getShort(REQUIRESLOCALISATION) == 0 ? false : true);
        }
        if (!c.isNull(ISSYSTEMGROUP)) {
            group.mIsSystemGroup = (c.getShort(ISSYSTEMGROUP) == 0 ? false : true);
        }
        if (!c.isNull(ISSMARTGROUP)) {
            group.mIsSmartGroup = (c.getShort(ISSMARTGROUP) == 0 ? false : true);
        }
        if (!c.isNull(USERID)) {
            group.mUserId = c.getLong(USERID);
        }
        if (!c.isNull(NAME)) {
            group.mName = c.getString(NAME);
        }
        if (!c.isNull(IMAGEMIMETYPE)) {
            group.mImageMimeType = c.getString(IMAGEMIMETYPE);
        }
        if (!c.isNull(IMAGEBYTES)) {
            group.mImageBytes = ByteBuffer.wrap(c.getBlob(IMAGEBYTES));
        }
        if (!c.isNull(COLOR)) {
            group.mColor = c.getString(COLOR);
        }
        return group;
    }

    /**
     * Returns a ContentValues object that can be used to insert or modify a
     * group in the table.
     * 
     * @param group The source GroupItem object
     * @return The ContentValues object containing the data.
     * @note NULL fields in the given group will not be included in the
     *       ContentValues
     */
    private static ContentValues fillUpdateData(GroupItem group) {
        ContentValues contactDetailValues = new ContentValues();
        if (group.mLocalGroupId != null) {
            contactDetailValues.put(Field.LOCALGROUPID.toString(), group.mLocalGroupId);
        }
        if (group.mId != null) {
            contactDetailValues.put(Field.SERVERGROUPID.toString(), group.mId);
        }
        if (group.mGroupType != null) {
            contactDetailValues.put(Field.GROUPTYPE.toString(), group.mGroupType);
        }
        if (group.mIsReadOnly != null) {
            contactDetailValues.put(Field.ISREADONLY.toString(), group.mIsReadOnly);
        }
        if (group.mRequiresLocalisation != null) {
            contactDetailValues.put(Field.REQUIRESLOCALISATION.toString(),
                    group.mRequiresLocalisation);
        }
        if (group.mIsSystemGroup != null) {
            contactDetailValues.put(Field.ISSYSTEMGROUP.toString(), group.mIsSystemGroup);
        }
        if (group.mIsSmartGroup != null) {
            contactDetailValues.put(Field.ISSMARTGROUP.toString(), group.mIsSmartGroup);
        }
        if (group.mUserId != null) {
            contactDetailValues.put(Field.USERID.toString(), group.mUserId);
        }
        if (group.mName != null) {
            contactDetailValues.put(Field.NAME.toString(), group.mName);
        }
        if (group.mImageMimeType != null) {
            contactDetailValues.put(Field.IMAGEMIMETYPE.toString(), group.mImageMimeType);
        }
        if (group.mImageBytes != null) {
            contactDetailValues.put(Field.IMAGEBYTES.toString(), group.mImageBytes.array());
        }
        if (group.mColor != null) {
            contactDetailValues.put(Field.COLOR.toString(), group.mColor);
        }
        return contactDetailValues;
    }

    /**
     * Fetches a list of all the available groups.
     * 
     * @param groupList A list that will be populated with the result.
     * @param readableDb Readable SQLite database
     * @return SUCCESS or a suitable error
     */
    public static ServiceStatus fetchGroupList(ArrayList<GroupItem> groupList,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "GroupsTable.fetchGroupList()");
        groupList.clear();
        Cursor c = null;
        try {
            String query = "SELECT " + getFullQueryList() + " FROM " + TABLE_NAME;
            c = readableDb.rawQuery(query, null);
            while (c.moveToNext()) {
                groupList.add(getQueryData(c));
            }
        } catch (SQLiteException e) {
            LogUtils.logE("GroupsTable.fetchGroupList() Exception - Unable to fetch group list", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Adds list of groups to the table
     * 
     * @param groupList The list to add
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus addGroupList(List<GroupItem> groupList, SQLiteDatabase writableDb) {
        try {
            writableDb.beginTransaction();
            for (GroupItem mGroupItem : groupList) {
                if (Settings.ENABLED_DATABASE_TRACE) {
                    DatabaseHelper.trace(true, "GroupsTable.addGroupList() mName["
                            + mGroupItem.mName + "]");
                }
                mGroupItem.mLocalGroupId = writableDb.insertOrThrow(TABLE_NAME, null,
                        fillUpdateData(mGroupItem));
                if (mGroupItem.mLocalGroupId < 0) {
                    LogUtils.logE("GroupsTable.addGroupList() Unable to add group - mName["
                            + mGroupItem.mName + "");
                    writableDb.endTransaction();
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
            }
            writableDb.setTransactionSuccessful();

        } catch (SQLException e) {
            LogUtils.logE("GroupsTable.addGroupList() SQLException - Unable to add group", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;

        } finally {
            if (writableDb != null) {
                writableDb.endTransaction();
            }
        }

        return ServiceStatus.SUCCESS;
    }

    /**
     * Removes all groups from the table. The
     * {@link #populateSystemGroups(Context, SQLiteDatabase)} function should be
     * called afterwards to ensure the system groups are restored.
     * 
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus deleteAllGroups(SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "GroupsTable.deleteAllGroups()");
        try {
            if (writableDb.delete(TABLE_NAME, null, null) < 0) {
                LogUtils.logE("GroupsTable.deleteAllGroups() Unable to delete all groups");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }

        } catch (SQLException e) {
            LogUtils.logE(
                    "GroupsTable.deleteAllGroups() SQLException - Unable to delete all groups", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetches a cursor that can be used for browsing the groups. The
     * {@link #getQueryData(Cursor)} method can be used to fetch the data of a
     * particular record in the cursor.
     * 
     * @param readableDb Readable SQLite database
     * @return The cursor, or null if an error occurs
     */
    public static Cursor getGroupCursor(SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "GroupsTable.getGroupCursor()");
        try {
            return readableDb.rawQuery(getQueryStringSql("NAME != 'Private'"), null);

        } catch (SQLiteException e) {
            LogUtils.logE("GroupsTable.getGroupCursor() Exception - Unable to fetch group cursor",
                    e);
            return null;
        }
    }

    /**
     * Fetches a cursor that can be used for browsing the groups exluding the
     * Connected friends item. The {@link #getQueryData(Cursor)} method can be
     * used to fetch the data of a particular record in the cursor.
     * 
     * @param readableDb Readable SQLite database
     * @return The cursor, or null if an error occurs
     */
    public static Cursor getGroupCursorExcludeConnectedFriends(
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false,
                "GroupsTable.getGroupCursorExcludeConnectedFriends()");
        try {
            return readableDb.rawQuery(
                    getQueryStringSql("NAME != 'Private' AND "
                            + Field.SERVERGROUPID + " != "
                            + GROUP_CONNECTED_FRIENDS),
                    null);

        } catch (SQLiteException e) {
            LogUtils
                    .logE(
                            "GroupsTable.getGroupCursorExcludeConnectedFriends()"
                                    + " Exception - Unable to fetch group"
                                    + " cursor", e);
            return null;
        }
    }

    /**
     * Populates the table if system groups that are specified in the resources.
     * 
     * @param context The context for reading the app resources
     * @param writableDb Writable SQLite database for updating the table
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus populateSystemGroups(Context context, SQLiteDatabase writableDb) {
        final List<GroupItem> groupList = new ArrayList<GroupItem>();
        GroupItem all = new GroupItem();
        all.mName = context.getString(R.string.ContactListActivity_group_all);
        all.mIsReadOnly = true;
        all.mId = GROUP_ALL;
        groupList.add(all);

        GroupItem online = new GroupItem();
        online.mName = context.getString(R.string.ContactListActivity_group_online);
        online.mIsReadOnly = true;
        online.mId = GROUP_ONLINE;
        groupList.add(online);

        GroupItem phonebook = new GroupItem();
        phonebook.mName = context.getString(R.string.ContactListActivity_group_phonebook);
        phonebook.mIsReadOnly = true;
        phonebook.mId = GROUP_PHONEBOOK;
        groupList.add(phonebook);
        return addGroupList(groupList, writableDb);
    }
}
