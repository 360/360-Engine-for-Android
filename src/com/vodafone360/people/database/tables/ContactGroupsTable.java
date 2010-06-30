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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the contact groups database table.
 * This table stores all contact/group associations (0-many to 0-many
 * relationship). This class is never instantiated hence all methods must be
 * static.
 * 
 * @version %I%, %G%
 */
public abstract class ContactGroupsTable {
    /**
     * Name of the table as it appears in the database
     */
    protected static final String TABLE_NAME = "ContactGroupRelations";

    /**
     * Represents the data stored in a record
     */
    private static class ContactGroup {

        /**
         * The local contact ID from Contacts table
         */
        private Long mLocalContactId = null;

        /**
         * The server group ID
         */
        private Long mZybGroupId = null;
    }

    /**
     * An enumeration of all the field names in the database.
     */
    public static enum Field {
        LOCALRELATIONID("LocalRelationId"),
        LOCALCONTACTID("LocalContactId"),
        ZYBGROUPID("ZybGroupId");

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
     * Create Contact Groups Table.
     * 
     * @param writeableDb A writable SQLite database
     * @throws SQLException If an SQL compilation error occurs
     */
    public static void create(SQLiteDatabase writeableDb) throws SQLException {
        DatabaseHelper.trace(true, "ContactGroupsTable.create()");
        writeableDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Field.LOCALRELATIONID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Field.LOCALCONTACTID + " LONG, "
                + Field.ZYBGROUPID + " LONG);");
    }

    /**
     * Returns a ContentValues object that can be used to insert or update a new
     * contact/group association to the table.
     * 
     * @param contactGroup A ContactGroup object with the local Contact ID and
     *            Group Id set appropriately.
     * @return The ContactValues object
     */
    private static ContentValues fillUpdateData(ContactGroup contactGroup) {
        DatabaseHelper.trace(false, "ContactGroupsTable.fillUpdateData()");
        ContentValues contactValues = new ContentValues();
        contactValues.put(Field.LOCALCONTACTID.toString(), contactGroup.mLocalContactId);
        contactValues.put(Field.ZYBGROUPID.toString(), contactGroup.mZybGroupId);
        return contactValues;
    }

    /**
     * Associates a contact and a group
     * 
     * @param localContactId The local contact Id from the Contacts table
     * @param zybGroupId The server group ID from groups table
     * @param writeableDb A writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean addContactToGroup(long localContactId, long zybGroupId,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactGroupsTable.addContactToGroup()");
        try {
            final ContactGroup contactGroup = new ContactGroup();
            contactGroup.mLocalContactId = localContactId;
            contactGroup.mZybGroupId = zybGroupId;
            final ContentValues values = fillUpdateData(contactGroup);
            if (writableDb.insertOrThrow(TABLE_NAME, null, values) < 0) {
                LogUtils.logE("ContactGroupsTable.addContactToGroup() "
                        + "Unable to insert new contact group summary");
                return false;
            }
            return true;

        } catch (SQLException e) {
            LogUtils.logE("ContactGroupsTable.addContactToGroup() SQLException - "
                    + "Unable to insert new contact summary", e);
            return false;
        }
    }

    /**
     * Remove the association between contact and group.
     * 
     * @param localContactId The local contact Id from the Contacts table
     * @param zybGroupId The server group ID from groups table
     * @param writeableDb A writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean deleteContactFromGroup(long localContactId, long zybGroupId,
            SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactGroupsTable.deleteContactFromGroup()");
        try {
            if (writableDb.delete(TABLE_NAME, Field.LOCALCONTACTID + "=" + localContactId + " AND "
                    + Field.ZYBGROUPID + "=" + zybGroupId, null) <= 0) {
                LogUtils.logE("ContactGroupsTable.deleteContactFromGroup() "
                        + "Unable to delete contact from group");
                return false;
            }
            return true;

        } catch (SQLException e) {
            LogUtils.logE("ContactGroupsTable.deleteContactFromGroup() SQLException - "
                    + "Unable to delete contact from group", e);
            return false;
        }
    }

    /**
     * Removes all references to a contact from the table (should be called when
     * a contact is deleted).
     * 
     * @param localContactId The local contact Id from the Contacts table
     * @param writeableDb A writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean deleteContact(long localContactId, SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "ContactGroupsTable.deleteContact()");
        try {
            if (writableDb.delete(TABLE_NAME, Field.LOCALCONTACTID + "=" + localContactId, null) < 0) {
                LogUtils.logE("ContactGroupsTable.deleteContact() "
                        + "Unable to delete contact from contact groups table");
                return false;
            }
            return true;

        } catch (SQLException e) {
            LogUtils.logE("ContactGroupsTable.deleteContact() SQLException - "
                    + "Unable to delete contact from contact groups table", e);
            return false;
        }
    }

    /**
     * Fetches all the groups associated with a given contact.
     * 
     * @param localContactId The local contact Id from the Contacts table
     * @param groupIds A list that will be populated with the result.
     * @param readableDb A readable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean fetchContactGroups(long localContactId, List<Long> groupIds,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "ContactGroupsTable.fetchContactGroups() localContactId["
                + localContactId + "]");
        String[] args = {
            String.format("%d", localContactId)
        };
        Cursor c1 = null;
        groupIds.clear();
        try {
            c1 = readableDb.rawQuery("SELECT " + Field.ZYBGROUPID + " FROM " + TABLE_NAME
                    + " WHERE " + Field.LOCALCONTACTID + " = ?", args);

            while (c1.moveToNext()) {
                if (!c1.isNull(0)) {
                    groupIds.add(c1.getLong(0));
                }
            }
            c1.close();

        } catch (SQLiteException e) {
            LogUtils.logE("ContactGroupsTable.fetchContactGroups()"
                    + " Exception - Unable to fetch contact groups", e);
            return false;
        } finally {
            CloseUtils.close(c1);
            c1 = null;
        }

        return true;
    }

    /**
     * Merges the given contact group relation changes into the database.
     * 
     * @param contact the updated contact with the new group relations. If any
     *            of the group IDs are -1 the associated will be ignored.
     * @param writeableDb A writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus modifyContact(Contact contact, SQLiteDatabase db) {
        DatabaseHelper.trace(false, "ContactGroupsTable.modifyContact()");

        final List<Long> contactGroups = new ArrayList<Long>();
        final List<Long> updatedContactGroups = contact.groupList;

        // get the contact groups
        fetchContactGroups(contact.localContactID, contactGroups, db);

        // check deleted groups
        for (int i = 0; i < contactGroups.size();) {
            Long groupId = contactGroups.get(i);
            if (groupId != -1) {
                if (!updatedContactGroups.contains(groupId)) {
                    contactGroups.remove(i);
                    if (!deleteContactFromGroup(contact.localContactID, groupId, db)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }

        // check added groups
        for (Long groupId : updatedContactGroups) {
            if (groupId != -1) {
                if (!contactGroups.contains(groupId)) {
                    if (!addContactToGroup(contact.localContactID, groupId, db)) {
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                }
            }
        }

        return ServiceStatus.SUCCESS;
    }
}
