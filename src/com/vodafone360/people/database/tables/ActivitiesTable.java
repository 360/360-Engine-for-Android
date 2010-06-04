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
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.utils.SqlUtils;
import com.vodafone360.people.datatypes.ActivityContact;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the activities database table. This
 * class is never instantiated hence all methods must be static.
 *
 * @version %I%, %G%
 */
public abstract class ActivitiesTable {
    /***
     * The name of the table as it appears in the database.
     */
    private static final String TABLE_NAME = "Activities";
    
    private static final String TABLE_INDEX_NAME = "ActivitiesIndex";

    /** Database cleanup will delete any activity older than X days. **/
    private static final int CLEANUP_MAX_AGE_DAYS = 10;

    /** Database cleanup will delete older activities after the first X. **/
    private static final int CLEANUP_MAX_QUANTITY = 200;

    /***
     * An enumeration of all the field names in the database.
     */
    public static enum Field {
        /** Local timeline id. **/
        LOCAL_ACTIVITY_ID("LocalId"),
        /** Activity ID. **/
        ACTIVITY_ID("activityid"),
        /** Timestamp. */
        TIMESTAMP("time"),
        /** Type of the event. **/
        TYPE("type"),
        /** URI. */
        URI("uri"),
        /** Title for timelines . **/
        TITLE("title"),
        /** Contents of timelines/statuses. **/
        DESCRIPTION("description"),
        /** Preview URL. **/
        PREVIEW_URL("previewurl"),
        /** Store. **/
        STORE("store"),
        /** Type of the event: status, chat messages, phone call or SMS/MMS. **/
        FLAG("flag"),
        /** Parent Activity. **/
        PARENT_ACTIVITY("parentactivity"),
        /** Has children. **/
        HAS_CHILDREN("haschildren"),
        /** Visibility. **/
        VISIBILITY("visibility"),
        /** More info. **/
        MORE_INFO("moreinfo"),
        /** Contact ID. **/
        CONTACT_ID("contactid"),
        /** User ID. **/
        USER_ID("userid"),
        /** Contact name or the alternative. **/
        CONTACT_NAME("contactname"),
        /** Other contact's localContactId. **/
        LOCAL_CONTACT_ID("contactlocalid"),
        /** @see SocialNetwork. **/
        CONTACT_NETWORK("contactnetwork"),
        /** Contact address. **/
        CONTACT_ADDRESS("contactaddress"),
        /** Contact avatar URL. **/
        CONTACT_AVATAR_URL("contactavatarurl"),
        /** Native item type. **/
        NATIVE_ITEM_TYPE("nativeitemtype"),
        /** Native item ID. **/
        NATIVE_ITEM_ID("nativeitemid"),
        /** Latest contact status. **/
        LATEST_CONTACT_STATUS("latestcontactstatus"),
        /** Native thread ID. **/
        NATIVE_THREAD_ID("nativethreadid"),
        /** For chat messages: if this message is incoming. **/
        INCOMING("incoming");

        /** Name of the field as it appears in the database. **/
        private final String mField;

        /**
         * Constructor.
         *
         * @param field - The name of the field (see list above)
         */
        private Field(final String field) {
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
     * An enumeration of supported timeline types.
     */
    public static enum TimelineNativeTypes {
        /** Call log type. **/
        CallLog,
        /** SMS log type. **/
        SmsLog,
        /** MMS log type. **/
        MmsLog,
        /** Chat log type. **/
        ChatLog
    }

    /**
     * This class encapsulates a timeline activity item.
     */
    public static class TimelineSummaryItem {

        /***
         * Enum of Timeline types.
         */
        public enum Type {
            /** Incoming type. **/
            INCOMING,
            /** Outgoing type. **/
            OUTGOING,
            /** Unsent type. **/
            UNSENT,
            /** Unknown type (do not use). **/
            UNKNOWN
        }

        /***
         * Get the Type from a given Integer value.

         * @param input Integer.ordinal value of the Type
         * @return Relevant Type or UNKNOWN if the Integer is not known.
         */
        public static Type getType(final int input) {
            if (input < 0 || input > Type.UNKNOWN.ordinal()) {
                return Type.UNKNOWN;
            } else {
                return Type.values()[input];
            }
        }

        /** Maps to the local activity ID (primary key). **/
        private Long mLocalActivityId;
        /** Maps to the activity timestamp in the table. **/
        public Long mTimestamp;
        /** Maps to the contact name in the table. **/
        public String mContactName;
        /** Set to true if there is an avatar URL stored in the table. **/
        private boolean mHasAvatar;
        /** Maps to type in the table. **/
        public ActivityItem.Type mType;
        /** Maps to local contact id in the table. **/
        public Long mLocalContactId;
        /** Maps to contact network stored in the table. **/
        public String mContactNetwork;
        /** Maps to title stored in the table. **/
        public String mTitle;
        /** Maps to description stored in the table. **/
        public String mDescription;
        /**
         * Maps to native item type in the table Can be an ordinal from the
         * {@link ActivitiesTable#TimelineNativeTypes}.
         */
        public Integer mNativeItemType;
        /**
         * Key linking to the call-log or message-log item in the native
         * database.
         */
        public Integer mNativeItemId;
        /** Server contact ID. **/
        public Long mContactId;
        /** User ID from the server. **/
        public Long mUserId;
        /** Thread ID from the native database (for messages). **/
        public Integer mNativeThreadId;
        /** Contact address (phone number or email address). **/
        public String mContactAddress;
        /** Messages can be incoming and outgoing. **/
        public Type mIncoming;

        /**
         * Returns a string describing the timeline summary item.
         *
         * @return String describing the timeline summary item.
         */
        @Override
        public final String toString() {
            return "TimeLineSummaryItem [mLocalActivityId[" + mLocalActivityId
                + "], mTimestamp[" + mTimestamp
                + "], mContactName[" + mContactName
                + "], mHasAvatar[" + mHasAvatar
                + "], mType[" + mType
                + "], mLocalContactId[" + mLocalContactId
                + "], mContactNetwork[" + mContactNetwork
                + "], mTitle[" + mTitle
                + "], mDescription[" + mDescription
                + "], mNativeItemType[" + mNativeItemType
                + "], mNativeItemId[" + mNativeItemId
                + "], mContactId[" + mContactId
                + "], mUserId[" + mUserId
                + "], mNativeThreadId[" + mNativeThreadId
                + "], mContactAddress[" + mContactAddress
                + "], mIncoming[" + mIncoming
                + "]]";
        }

        @Override
        public final boolean equals(final Object object) {
            if (TimelineSummaryItem.class != object.getClass()) {
                return false;
            }
            TimelineSummaryItem item = (TimelineSummaryItem) object;
            return mLocalActivityId.equals(item.mLocalActivityId)
            && mTimestamp.equals(item.mTimestamp)
            && mContactName.equals(item.mContactName)
            && mHasAvatar == item.mHasAvatar
            && mType.equals(item.mType)
            && mLocalContactId.equals(item.mLocalContactId)
            && mContactNetwork.equals(item.mContactNetwork)
            && mTitle.equals(item.mTitle)
            && mDescription.equals(item.mDescription)
            && mNativeItemType.equals(item.mNativeItemType)
            && mNativeItemId.equals(item.mNativeItemId)
            && mContactId.equals(item.mContactId)
            && mUserId.equals(item.mUserId)
            && mNativeThreadId.equals(item.mNativeThreadId)
            && mContactAddress.equals(item.mContactAddress)
            && mIncoming.equals(item.mIncoming);
        }
    };

    /** Number of milliseconds in a day. **/
    private static final int NUMBER_OF_MS_IN_A_DAY = 24 * 60 * 60 * 1000;
    /** Number of milliseconds in a second. **/
    private static final int NUMBER_OF_MS_IN_A_SECOND = 1000;

    /***
     * Private constructor to prevent instantiation.
     */
    private ActivitiesTable() {
        // Do nothing.
    }

    /**
     * Create Activities Table.
     *
     * @param writeableDb A writable SQLite database.
     */
    public static void create(final SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.create()");
        writeableDb.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + Field.LOCAL_ACTIVITY_ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Field.ACTIVITY_ID + " LONG, "
                + Field.TIMESTAMP + " LONG, "
                + Field.TYPE + " TEXT, "
                + Field.URI + " TEXT, "
                + Field.TITLE + " TEXT, "
                + Field.DESCRIPTION + " TEXT, "
                + Field.PREVIEW_URL + " TEXT, "
                + Field.STORE + " TEXT, "
                + Field.FLAG + " INTEGER, "
                + Field.PARENT_ACTIVITY + " LONG, "
                + Field.HAS_CHILDREN + " INTEGER, "
                + Field.VISIBILITY + " INTEGER, "
                + Field.MORE_INFO + " TEXT, "
                + Field.CONTACT_ID + " LONG, "
                + Field.USER_ID + " LONG, "
                + Field.CONTACT_NAME + " TEXT, "
                + Field.LOCAL_CONTACT_ID + " LONG, "
                + Field.CONTACT_NETWORK + " TEXT, "
                + Field.CONTACT_ADDRESS + " TEXT, "
                + Field.CONTACT_AVATAR_URL + " TEXT, "
                + Field.LATEST_CONTACT_STATUS + " INTEGER, "
                + Field.NATIVE_ITEM_TYPE + " INTEGER, "
                + Field.NATIVE_ITEM_ID + " INTEGER, "
                + Field.NATIVE_THREAD_ID + " INTEGER, "
                + Field.INCOMING + " INTEGER);");
        
        writeableDb.execSQL("CREATE INDEX " + TABLE_INDEX_NAME + " ON " + TABLE_NAME + " ( " + Field.TIMESTAMP + " )");
    }

    /**
     * Fetches a comma separated list of table fields which can be used in an
     * SQL SELECT statement as the query projection. One of the
     * {@link #getQueryData} methods can used to fetch data from the cursor.
     *
     * @return SQL string
     */
    private static String getFullQueryList() {
        DatabaseHelper.trace(false, "DatabaseHelper.getFullQueryList()");
        return Field.LOCAL_ACTIVITY_ID + ", "
            + Field.ACTIVITY_ID + ", " + Field.TIMESTAMP + ", "
            + Field.TYPE + ", " + Field.URI + ", "
            + Field.TITLE + ", " + Field.DESCRIPTION + ", "
            + Field.PREVIEW_URL + ", " + Field.STORE + ", "
            + Field.FLAG + ", " + Field.PARENT_ACTIVITY + ", "
            + Field.HAS_CHILDREN + ", " + Field.VISIBILITY + ", "
            + Field.MORE_INFO + ", " + Field.CONTACT_ID + ", "
            + Field.USER_ID + ", " + Field.CONTACT_NAME + ", "
            + Field.LOCAL_CONTACT_ID + ", " + Field.CONTACT_NETWORK + ", "
            + Field.CONTACT_ADDRESS + ", " + Field.CONTACT_AVATAR_URL + ", "
            + Field.INCOMING;
    }

    /**
     * Fetches activities information from a cursor at the current position. The
     * {@link #getFullQueryList()} method should be used to make the query.
     *
     * @param cursor The cursor returned by the query
     * @param activityItem An empty activity object that will be filled with the
     *            result
     * @param activityContact An empty activity contact object that will be
     *            filled
     */
    public static void getQueryData(final Cursor cursor,
            final ActivityItem activityItem,
            final ActivityContact activityContact) {
        DatabaseHelper.trace(false, "DatabaseHelper.getQueryData()");

        /** Populate ActivityItem. **/
        activityItem.mLocalActivityId =
            SqlUtils.setLong(cursor, Field.LOCAL_ACTIVITY_ID.toString(), null);
        activityItem.mActivityId =
            SqlUtils.setLong(cursor, Field.ACTIVITY_ID.toString(), null);
        activityItem.mTime =
            SqlUtils.setLong(cursor, Field.TIMESTAMP.toString(), null);
        activityItem.mType =
            SqlUtils.setActivityItemType(cursor, Field.TYPE.toString());
        activityItem.mUri = SqlUtils.setString(cursor, Field.URI.toString());
        activityItem.mTitle =
            SqlUtils.setString(cursor, Field.TITLE.toString());
        activityItem.mDescription =
            SqlUtils.setString(cursor, Field.DESCRIPTION.toString());
        activityItem.mPreviewUrl =
            SqlUtils.setString(cursor, Field.PREVIEW_URL.toString());
        activityItem.mStore =
            SqlUtils.setString(cursor, Field.STORE.toString());
        activityItem.mActivityFlags =
            SqlUtils.setInt(cursor, Field.FLAG.toString(), null);
        activityItem.mParentActivity =
            SqlUtils.setLong(cursor, Field.PARENT_ACTIVITY.toString(), null);
        activityItem.mHasChildren =
            SqlUtils.setBoolean(cursor, Field.HAS_CHILDREN.toString(),
                    activityItem.mHasChildren);
        activityItem.mVisibilityFlags =
            SqlUtils.setInt(cursor, Field.VISIBILITY.toString(), null);
        // TODO: Field MORE_INFO is not used, consider deleting.

        /** Populate ActivityContact. **/
        getQueryData(cursor, activityContact);
    }

    /**
     * Fetches activities information from a cursor at the current position. The
     * {@link #getFullQueryList()} method should be used to make the query.
     *
     * @param cursor The cursor returned by the query.
     * @param activityContact An empty activity contact object that will be
     *            filled
     */
    public static void getQueryData(final Cursor cursor,
            final ActivityContact activityContact) {
        DatabaseHelper.trace(false, "DatabaseHelper.getQueryData()");

        /** Populate ActivityContact. **/
        activityContact.mContactId =
            SqlUtils.setLong(cursor, Field.CONTACT_ID.toString(), null);
        activityContact.mUserId =
            SqlUtils.setLong(cursor, Field.USER_ID.toString(), null);
        activityContact.mName =
            SqlUtils.setString(cursor, Field.CONTACT_NAME.toString());
        activityContact.mLocalContactId =
            SqlUtils.setLong(cursor, Field.LOCAL_CONTACT_ID.toString(), null);
        activityContact.mNetwork =
            SqlUtils.setString(cursor, Field.CONTACT_NETWORK.toString());
        activityContact.mAddress =
            SqlUtils.setString(cursor, Field.CONTACT_ADDRESS.toString());
        activityContact.mAvatarUrl =
            SqlUtils.setString(cursor, Field.CONTACT_AVATAR_URL.toString());
    }

    /***
     * Provides a ContentValues object that can be used to update the table.
     *
     * @param item The source activity item
     * @param contactIdx The index of the contact to use for the update, or null
     *            to exclude contact specific information.
     * @return ContentValues for use in an SQL update or insert.
     * @note Items that are NULL will be not modified in the database.
     */
    private static ContentValues fillUpdateData(final ActivityItem item,
            final Integer contactIdx) {
        DatabaseHelper.trace(false, "DatabaseHelper.fillUpdateData()");
        ContentValues activityItemValues = new ContentValues();
        ActivityContact ac = null;
        if (contactIdx != null) {
            ac = item.mContactList.get(contactIdx);
        }

        activityItemValues.put(Field.ACTIVITY_ID.toString(), item.mActivityId);
        activityItemValues.put(Field.TIMESTAMP.toString(), item.mTime);
        if (item.mType != null) {
            activityItemValues.put(Field.TYPE.toString(),
                    item.mType.getTypeCode());
        }
        if (item.mUri != null) {
            activityItemValues.put(Field.URI.toString(), item.mUri);
        }
        /** TODO: Not sure if we need this. **/
        // activityItemValues.put(Field.INCOMING.toString(), false);
        
        activityItemValues.put(Field.TITLE.toString(), item.mTitle);
        activityItemValues.put(Field.DESCRIPTION.toString(), item.mDescription);
        if (item.mPreviewUrl != null) {
            activityItemValues.put(Field.PREVIEW_URL.toString(),
                    item.mPreviewUrl);
        }
        if (item.mStore != null) {
            activityItemValues.put(Field.STORE.toString(), item.mStore);
        }
        if (item.mActivityFlags != null) {
            activityItemValues.put(Field.FLAG.toString(), item.mActivityFlags);
        }
        if (item.mParentActivity != null) {
            activityItemValues.put(Field.PARENT_ACTIVITY.toString(),
                    item.mParentActivity);
        }
        if (item.mHasChildren != null) {
            activityItemValues.put(Field.HAS_CHILDREN.toString(),
                    item.mHasChildren);
        }
        if (item.mVisibilityFlags != null) {
            activityItemValues.put(Field.VISIBILITY.toString(),
                    item.mVisibilityFlags);
        }
        if (ac != null) {
            activityItemValues.put(Field.CONTACT_ID.toString(), ac.mContactId);
            activityItemValues.put(Field.USER_ID.toString(), ac.mUserId);
            activityItemValues.put(Field.CONTACT_NAME.toString(), ac.mName);
            activityItemValues.put(Field.LOCAL_CONTACT_ID.toString(),
                    ac.mLocalContactId);
            if (ac.mNetwork != null) {
                activityItemValues.put(Field.CONTACT_NETWORK.toString(),
                        ac.mNetwork);
            }
            if (ac.mAddress != null) {
                activityItemValues.put(Field.CONTACT_ADDRESS.toString(),
                        ac.mAddress);
            }
            if (ac.mAvatarUrl != null) {
                activityItemValues.put(Field.CONTACT_AVATAR_URL.toString(),
                        ac.mAvatarUrl);
            }
        }

        return activityItemValues;
    }

    /**
     * Fetches a list of status items from the given time stamp.
     *
     * @param timeStamp Time stamp in milliseconds
     * @param readableDb Readable SQLite database
     * @return A cursor (use one of the {@link #getQueryData} methods to read
     *         the data)
     */
    public static Cursor fetchStatusEventList(final long timeStamp,
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper.fetchStatusEventList()");
        return readableDb.rawQuery("SELECT " + getFullQueryList() + " FROM "
                + TABLE_NAME + " WHERE ("
                + Field.FLAG + " & " + ActivityItem.STATUS_ITEM
                + ") AND " + Field.TIMESTAMP + " > " + timeStamp
                + " ORDER BY " + Field.TIMESTAMP + " DESC", null);
    }

    /**
     * Returns a list of activity IDs already synced, in reverse chronological
     * order Fetches from the given timestamp.
     *
     * @param actIdList An empty list which will be filled with the result
     * @param timeStamp The time stamp to start the fetch
     * @param readableDb Readable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus fetchActivitiesIds(final List<Long> actIdList,
            final Long timeStamp, final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper.fetchActivitiesIds()");
        Cursor cursor = null;
        try {
            long queryTimeStamp;
            if (timeStamp != null) {
                queryTimeStamp = timeStamp;
            } else {
                queryTimeStamp = 0;
            }

            cursor = readableDb.rawQuery("SELECT " + Field.ACTIVITY_ID
                    + " FROM " + TABLE_NAME + " WHERE "
                    + Field.TIMESTAMP + " >= " + queryTimeStamp
                    + " ORDER BY " + Field.TIMESTAMP + " DESC", null);
            while (cursor.moveToNext()) {
                actIdList.add(cursor.getLong(0));
            }

        } catch (SQLiteException e) {
            LogUtils.logE("ActivitiesTable.fetchActivitiesIds()"
                    + "Unable to fetch group list", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;

        } finally {
            CloseUtils.close(cursor);
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Adds a list of activities to table. The activities added will be grouped
     * in the database, based on local contact Id, name or contact address (see
     * {@link #removeContactGroup(Long, String, Long, int,
     * TimelineNativeTypes[], SQLiteDatabase)}
     * for more information on how the grouping works.
     *
     * @param actList The list of activities
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus addActivities(final List<ActivityItem> actList,
            final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.addActivities()");
        SQLiteStatement statement =
            ContactsTable.fetchLocalFromServerIdStatement(writableDb);

            for (ActivityItem activity : actList) {          
                try {
                    writableDb.beginTransaction();
                
                    if (activity.mContactList != null) {
                        int clistSize = activity.mContactList.size();
                        for (int i = 0; i < clistSize; i++) {
                            String contactName = activity.mContactList.get(i).mName;
                            activity.mContactList.get(i).mLocalContactId =
                                ContactsTable.fetchLocalFromServerId(
                                        activity.mContactList.get(i).mContactId,
                                        statement);
                            Long localContactId =
                                activity.mContactList.get(i).mLocalContactId;
                            int latestStatusVal = removeContactGroup(
                                    localContactId, contactName, activity.mTime,
                                    activity.mActivityFlags, null, writableDb);
    
                            ContentValues cv = fillUpdateData(activity, i);
                            cv.put(Field.LATEST_CONTACT_STATUS.toString(),
                                    latestStatusVal);
                            activity.mLocalActivityId =
                                writableDb.insertOrThrow(TABLE_NAME, null, cv);
                        }
                    } else {
                        activity.mLocalActivityId = writableDb.insertOrThrow(
                                TABLE_NAME, null, fillUpdateData(activity, null));
                    }
                    if (activity.mLocalActivityId < 0) {
                        LogUtils.logE("ActivitiesTable.addActivities() "
                                + "Unable to add activity");
                        return ServiceStatus.ERROR_DATABASE_CORRUPT;
                    }
                    writableDb.setTransactionSuccessful();
                } catch (SQLException e) {
                    LogUtils.logE("ActivitiesTable.addActivities() "
                            + "Unable to add activity", e);
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                } finally {
                    writableDb.endTransaction();
                }
            }

        return ServiceStatus.SUCCESS;
    }

    /**
     * Deletes all activities from the table.
     *
     * @param flag Can be a bitmap of:
     *            <ul>
     *            <li>{@link ActivityItem#TIMELINE_ITEM} - Timeline items</li>
     *            <li>{@link ActivityItem#STATUS_ITEM} - Status item</li>
     *            <li>{@link ActivityItem#ALREADY_READ} - Items that have been
     *            read</li>
     *            <li>NULL - to delete all activities</li>
     *            </ul>
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus deleteActivities(final Integer flag,
            final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.deleteActivities()");
        try {
            String whereClause = null;
            if (flag != null) {
                whereClause = Field.FLAG + "&" + flag;
            }
            if (writableDb.delete(TABLE_NAME, whereClause, null) < 0) {
                LogUtils.logE("ActivitiesTable.deleteActivities() "
                        + "Unable to delete activities");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.deleteActivities() "
                    + "Unable to delete activities", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    public static ServiceStatus fetchNativeIdsFromLocalContactId(List<Integer > nativeItemIdList, final long localContactId, final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper.fetchNativeIdsFromLocalContactId()");
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.NATIVE_ITEM_ID
                    + " FROM " + TABLE_NAME + " WHERE "
                    + Field.LOCAL_CONTACT_ID + " = " + localContactId, null);
            while (cursor.moveToNext()) {
                nativeItemIdList.add(cursor.getInt(0));
            }

        } catch (SQLiteException e) {
            LogUtils.logE("ActivitiesTable.fetchNativeIdsFromLocalContactId()"
                    + "Unable to fetch list of NativeIds from localcontactId", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;

        } finally {
            CloseUtils.close(cursor);
        }
        return ServiceStatus.SUCCESS;
    }
    
    /**
     * Deletes specified timeline activity from the table.
     *
     * @param Context
     * @param timelineItem TimelineSummaryItem to be deleted
     * @param writableDb Writable SQLite database
     * @param readableDb Readable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus deleteTimelineActivity(final Context context, final TimelineSummaryItem timelineItem, 
            final SQLiteDatabase writableDb, final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.deleteTimelineActivity()");
        try {
            List<Integer > nativeItemIdList = new ArrayList<Integer>() ;
            
            //Delete from Native Database
            if(timelineItem.mNativeThreadId != null) {
                //Sms Native Database
                final Uri smsUri = Uri.parse("content://sms");
                context.getContentResolver().delete(smsUri , "thread_id=" + timelineItem.mNativeThreadId, null);

                //Mms Native Database
                final Uri mmsUri = Uri.parse("content://mms");
                context.getContentResolver().delete(mmsUri, "thread_id=" + timelineItem.mNativeThreadId, null);
            } else { // For CallLogs
                if(timelineItem.mLocalContactId != null) {
                    fetchNativeIdsFromLocalContactId(nativeItemIdList, timelineItem.mLocalContactId, readableDb);
                    if(nativeItemIdList.size() > 0) {
                        //CallLog Native Database
                          for(Integer nativeItemId : nativeItemIdList) {
                              context.getContentResolver().delete(Calls.CONTENT_URI, Calls._ID + "=" + nativeItemId, null);   
                          }    
                      }
                } else {
                    if(timelineItem.mContactAddress != null) {
                        context.getContentResolver().delete(Calls.CONTENT_URI, Calls.NUMBER + "=" + timelineItem.mContactAddress, null);
                    }
                }
            }
            
            String whereClause = null;
            
            //Delete from People Client database
            if(timelineItem.mLocalContactId != null) {
                if(timelineItem.mNativeThreadId == null) { // Delete CallLogs & Chat Logs
                    whereClause = Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + " AND " 
                    + Field.LOCAL_CONTACT_ID + "='" + timelineItem.mLocalContactId + "' AND "
                    + Field.NATIVE_THREAD_ID + " IS NULL;";
                } else { //Delete Sms/MmsLogs
                    whereClause = Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + " AND " 
                    + Field.LOCAL_CONTACT_ID + "='" + timelineItem.mLocalContactId + "' AND "
                    + Field.NATIVE_THREAD_ID + "=" + timelineItem.mNativeThreadId + ";";
                }
            } else if(timelineItem.mContactAddress != null) {
                if(timelineItem.mNativeThreadId == null) { // Delete CallLogs & Chat Logs
                    whereClause = Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + " AND " 
                    + Field.CONTACT_ADDRESS + "='" + timelineItem.mContactAddress + "' AND "
                    + Field.NATIVE_THREAD_ID + " IS NULL;";
                } else { //Delete Sms/MmsLogs
                    whereClause = Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + " AND " 
                    + Field.CONTACT_ADDRESS + "='" + timelineItem.mContactAddress + "' AND "
                    + Field.NATIVE_THREAD_ID + "=" + timelineItem.mNativeThreadId + ";";
                }
            }

            if (writableDb.delete(TABLE_NAME, whereClause, null) < 0) {
                LogUtils.logE("ActivitiesTable.deleteTimelineActivity() "
                        + "Unable to delete specified activity");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.deleteTimelineActivity() "
                    + "Unable to delete specified activity", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Deletes all the timeline activities for a particular contact from the table.
     *
     * @param Context
     * @param timelineItem TimelineSummaryItem to be deleted
     * @param writableDb Writable SQLite database
     * @param readableDb Readable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus deleteTimelineActivities(Context context,
            TimelineSummaryItem latestTimelineItem, SQLiteDatabase writableDb,
            SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper.deleteTimelineActivities()");
        Cursor cursor = null;
        try {
            TimelineNativeTypes[] typeList = null;
            TimelineSummaryItem timelineItem = null;
            
            //For CallLog Timeline
            typeList = new TimelineNativeTypes[] {
                    TimelineNativeTypes.CallLog
                };
            cursor = fetchTimelineEventsForContact(0L, latestTimelineItem.mLocalContactId, 
                   latestTimelineItem.mContactName, typeList, null, readableDb);
           
            if(cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                timelineItem = getTimelineData(cursor);
                if(timelineItem != null) {
                    deleteTimelineActivity(context, timelineItem, writableDb, readableDb);
                }
            }
            
            //For SmsLog/MmsLog Timeline
            typeList = new TimelineNativeTypes[] {
                    TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog
                };
            cursor = fetchTimelineEventsForContact(0L, latestTimelineItem.mLocalContactId, 
                   latestTimelineItem.mContactName, typeList, null, readableDb);
           
            if(cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                timelineItem = getTimelineData(cursor);
                if(timelineItem != null) {
                    deleteTimelineActivity(context, timelineItem, writableDb, readableDb);
                }
            }
           
            //For ChatLog Timeline
            typeList = new TimelineNativeTypes[] {
                    TimelineNativeTypes.ChatLog
                };
            cursor = fetchTimelineEventsForContact(0L, latestTimelineItem.mLocalContactId, 
                   latestTimelineItem.mContactName, typeList, null, readableDb);
           
            if(cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                timelineItem = getTimelineData(cursor);
                if(timelineItem != null) {
                    deleteTimelineActivity(context, timelineItem, writableDb, readableDb);
                }
            }
        } catch (SQLException e) {
                LogUtils.logE("ActivitiesTable.deleteTimelineActivities() "
                        + "Unable to delete timeline activities", e);
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            if(cursor != null) {
                CloseUtils.close(cursor);
            }
        }
        
        return ServiceStatus.SUCCESS;
    }
    
    /**
     * Fetches timeline events grouped by local contact ID, name or contact
     * address. Events returned will be in reverse-chronological order. If a
     * native type list is provided the result will be filtered by the list.
     *
     * @param minTimeStamp Only timeline events from this date will be returned
     * @param nativeTypes A list of native types to filter the result, or null
     *            to return all.
     * @param readableDb Readable SQLite database
     * @return A cursor containing the result. The
     *         {@link #getTimelineData(Cursor)} method should be used for
     *         reading the cursor.
     */
    public static Cursor fetchTimelineEventList(final Long minTimeStamp,
            final TimelineNativeTypes[] nativeTypes,
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper.fetchTimelineEventList() "
                + "minTimeStamp[" + minTimeStamp + "]");
        try {
            int andVal = 1;
            String typesQuery = " AND ";
            if (nativeTypes != null) {
                typesQuery += DatabaseHelper.createWhereClauseFromList(
                        Field.NATIVE_ITEM_TYPE.toString(), nativeTypes, "OR");
                typesQuery += " AND ";
                andVal = 2;
            }

            String query = "SELECT " + Field.LOCAL_ACTIVITY_ID + ","
                + Field.TIMESTAMP + "," + Field.CONTACT_NAME + ","
                + Field.CONTACT_AVATAR_URL + "," + Field.LOCAL_CONTACT_ID + ","
                + Field.TITLE + "," + Field.DESCRIPTION + ","
                + Field.CONTACT_NETWORK + "," + Field.NATIVE_ITEM_TYPE + ","
                + Field.NATIVE_ITEM_ID + "," + Field.TYPE + ","
                + Field.CONTACT_ID + "," + Field.USER_ID + ","
                + Field.NATIVE_THREAD_ID + "," + Field.CONTACT_ADDRESS + ","
                + Field.INCOMING + " FROM " + TABLE_NAME + " WHERE ("
                + Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + ")"
                + typesQuery + Field.TIMESTAMP + " > " + minTimeStamp
                + " AND ("
                + Field.LATEST_CONTACT_STATUS + " & " + andVal
                + ") ORDER BY " + Field.TIMESTAMP + " DESC";
            return readableDb.rawQuery(query, null);
        } catch (SQLiteException e) {
            LogUtils.logE("ActivitiesTable.fetchLastUpdateTime() "
                    + "Unable to fetch timeline event list", e);
            return null;
        }
    }

    /**
     * Adds a list of timeline events to the database. Each event is grouped by
     * contact and grouped by contact + native type.
     *
     * @param itemList List of timeline events
     * @param isCallLog true to group all activities with call logs, false to
     *            group with messaging
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error
     */
    public static ServiceStatus addTimelineEvents(
            final ArrayList<TimelineSummaryItem> itemList,
            final boolean isCallLog, final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.addTimelineEvents()");
        TimelineNativeTypes[] activityTypes;
        if (isCallLog) {
            activityTypes = new TimelineNativeTypes[] {
                TimelineNativeTypes.CallLog
            };
        } else {
            activityTypes = new TimelineNativeTypes[] {
                    TimelineNativeTypes.SmsLog, TimelineNativeTypes.MmsLog
            };
        }

        for (TimelineSummaryItem item : itemList) {            
            try {   
                writableDb.beginTransaction();
                
                if (findNativeActivity(item.mNativeItemId,
                        item.mNativeItemType, writableDb)) {
                    continue;
                }
                int latestStatusVal = 0;
                if (item.mContactName != null || item.mLocalContactId != null) {
                    latestStatusVal |= removeContactGroup(item.mLocalContactId,
                            item.mContactName, item.mTimestamp,
                            ActivityItem.TIMELINE_ITEM, null, writableDb);
                    latestStatusVal |= removeContactGroup(item.mLocalContactId,
                            item.mContactName, item.mTimestamp,
                            ActivityItem.TIMELINE_ITEM, activityTypes,
                            writableDb);
                }
                ContentValues values = new ContentValues();
                values.put(Field.CONTACT_NAME.toString(), item.mContactName);
                values.put(Field.CONTACT_ID.toString(), item.mContactId);
                values.put(Field.USER_ID.toString(), item.mUserId);
                values.put(Field.LOCAL_CONTACT_ID.toString(),
                        item.mLocalContactId);
                values.put(Field.CONTACT_NETWORK.toString(),
                        item.mContactNetwork);
                values.put(Field.DESCRIPTION.toString(), item.mDescription);
                values.put(Field.TITLE.toString(), item.mTitle);
                values.put(Field.CONTACT_ADDRESS.toString(),
                        item.mContactAddress);
                values.put(Field.FLAG.toString(), ActivityItem.TIMELINE_ITEM);
                values.put(Field.NATIVE_ITEM_ID.toString(), item.mNativeItemId);
                values.put(Field.NATIVE_ITEM_TYPE.toString(),
                        item.mNativeItemType);
                values.put(Field.TIMESTAMP.toString(), item.mTimestamp);
                if (item.mType != null) {
                    values.put(Field.TYPE.toString(),
                            item.mType.getTypeCode());
                }
                values.put(Field.LATEST_CONTACT_STATUS.toString(),
                        latestStatusVal);
                values.put(Field.NATIVE_THREAD_ID.toString(),
                        item.mNativeThreadId);
                if (item.mIncoming != null) {
                    values.put(Field.INCOMING.toString(),
                            item.mIncoming.ordinal());
                }

                item.mLocalActivityId =
                    writableDb.insert(TABLE_NAME, null, values);
                if (item.mLocalActivityId < 0) {
                    LogUtils.logE("ActivitiesTable.addTimelineEvents() "
                            + "ERROR_DATABASE_CORRUPT - Unable to add "
                            + "timeline list to database");
                    return ServiceStatus.ERROR_DATABASE_CORRUPT;
                }
                
                writableDb.setTransactionSuccessful();
            } catch (SQLException e) {
                LogUtils.logE("ActivitiesTable.addTimelineEvents() SQLException - "
                        + "Unable to add timeline list to database", e);
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            } finally {     
                writableDb.endTransaction();
            }
        }

        return ServiceStatus.SUCCESS;
    }


    /**
     * The method returns the ROW_ID i.e. the INTEGER PRIMARY KEY AUTOINCREMENT
     * field value for the inserted row, i.e. LOCAL_ID.
     *
     * @param item TimelineSummaryItem.
     * @param read - TRUE if the chat message is outgoing or gets into the
     *            timeline history view for a contact with LocalContactId.
     * @param writableDb Writable SQLite database.
     * @return LocalContactID or -1 if the row was not inserted.
     */
    public static long addChatTimelineEvent(final TimelineSummaryItem item,
            final boolean read, final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.addChatTimelineEvent()");
        
        try {
            
            writableDb.beginTransaction();
            
            int latestStatusVal = 0;
            if (item.mContactName != null || item.mLocalContactId != null) {
                latestStatusVal |= removeContactGroup(item.mLocalContactId,
                        item.mContactName, item.mTimestamp,
                        ActivityItem.TIMELINE_ITEM, null, writableDb);
                latestStatusVal |= removeContactGroup(item.mLocalContactId,
                        item.mContactName, item.mTimestamp,
                        ActivityItem.TIMELINE_ITEM, new TimelineNativeTypes[] {
                            TimelineNativeTypes.ChatLog
                        }, writableDb);
            }
            ContentValues values = new ContentValues();
            values.put(Field.CONTACT_NAME.toString(), item.mContactName);
            values.put(Field.CONTACT_ID.toString(), item.mContactId);
            values.put(Field.USER_ID.toString(), item.mUserId);
            values.put(Field.LOCAL_CONTACT_ID.toString(), item.mLocalContactId);
            values.put(Field.CONTACT_NETWORK.toString(), item.mContactNetwork);
            values.put(Field.DESCRIPTION.toString(), item.mDescription);
            /** Chat message body. **/
            values.put(Field.TITLE.toString(), item.mTitle);
            values.put(Field.CONTACT_ADDRESS.toString(), item.mContactAddress);
            if (read) {
                values.put(Field.FLAG.toString(), ActivityItem.TIMELINE_ITEM
                        | ActivityItem.ALREADY_READ);
            } else {
                values.put(Field.FLAG.toString(), ActivityItem.TIMELINE_ITEM
                        | 0);
            }
            values.put(Field.NATIVE_ITEM_ID.toString(), item.mNativeItemId);
            values.put(Field.NATIVE_ITEM_TYPE.toString(), item.mNativeItemType);
            values.put(Field.TIMESTAMP.toString(), item.mTimestamp);
            if (item.mType != null) {
                values.put(Field.TYPE.toString(), item.mType.getTypeCode());
            }

            values.put(Field.LATEST_CONTACT_STATUS.toString(), latestStatusVal);
            values.put(Field.NATIVE_THREAD_ID.toString(), item.mNativeThreadId);
            /** Conversation ID for chat message. **/
            // values.put(Field.URI.toString(), item.conversationId);
            // 0 for incoming, 1 for outgoing
            if (item.mIncoming != null) {
                values.put(Field.INCOMING.toString(),
                        item.mIncoming.ordinal());
            }

            final long itemId = writableDb.insert(TABLE_NAME, null, values);
            if (itemId < 0) {
                LogUtils.logE("ActivitiesTable.addTimelineEvents() - "
                        + "Unable to add timeline list to database, index<0:"
                        + itemId);
                return -1;
            }
            writableDb.setTransactionSuccessful();
            return itemId;

        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.addTimelineEvents() SQLException - "
                    + "Unable to add timeline list to database", e);
            return -1;

        } finally {
            writableDb.endTransaction();
        }
    }

    /**
     * Clears the grouping of an activity in the database, if the given activity
     * is newer. This is so that only the latest activity is returned for a
     * particular group. Each activity is associated with two groups:
     * <ol>
     * <li>All group - Grouped by contact only</li>
     * <li>Native group - Grouped by contact and native type (call log or
     * messaging)</li>
     * </ol>
     * The group to be removed is determined by the activityTypes parameter.
     * Grouping must also work for timeline events that are not associated with
     * a contact. The following fields are used to do identify a contact for the
     * grouping (in order of priority):
     * <ol>
     * <li>localContactId - If it not null</li>
     * <li>name - If this is a valid telephone number, the match will be done to
     * ensure that the same phone number written in different ways will be
     * included in the group. See {@link #fetchNameWhereClause(Long, String)}
     * for more information.</li>
     * </ol>
     *
     * @param localContactId Local contact Id or NULL if the activity is not
     *            associated with a contact.
     * @param name Name of contact or contact address (telephone number, email,
     *            etc).
     * @param newUpdateTime The time that the given activity has occurred
     * @param flag Bitmap of types including:
     *            <ul>
     *            <li>{@link ActivityItem#TIMELINE_ITEM} - Timeline items</li>
     *            <li>{@link ActivityItem#STATUS_ITEM} - Status item</li>
     *            <li>{@link ActivityItem#ALREADY_READ} - Items that have been
     *            read</li>
     *            </ul>
     * @param activityTypes A list of native types to include in the grouping.
     *            Currently, only two groups are supported (see above). If this
     *            parameter is null the contact will be added to the
     *            "all group", otherwise the contact is added to the native
     *            group.
     * @param writableDb Writable SQLite database
     * @return The latest contact status value which should be added to the
     *         current activities grouping.
     */
    private static int removeContactGroup(final Long localContactId,
            final String name, final Long newUpdateTime, final int flag,
            final TimelineNativeTypes[] activityTypes,
            final SQLiteDatabase writableDb) {
        String whereClause = "";
        int andVal = 1;
        if (activityTypes != null) {
            whereClause = DatabaseHelper.createWhereClauseFromList(
                    Field.NATIVE_ITEM_TYPE.toString(), activityTypes, "OR");
            whereClause += " AND ";
            andVal = 2;
        }

        String nameWhereClause = fetchNameWhereClause(localContactId, name);

        if (nameWhereClause == null) {
            return 0;
        }
        whereClause += nameWhereClause;
        Long prevTime = null;
        Long prevLocalId = null;
        Integer prevLatestContactStatus = null;
        Cursor cursor = null;
        try {
            cursor = writableDb.rawQuery("SELECT " + Field.TIMESTAMP + ","
                    + Field.LOCAL_ACTIVITY_ID + ","
                    + Field.LATEST_CONTACT_STATUS + " FROM " + TABLE_NAME
                    + " WHERE " + whereClause
                    + " AND "
                    + "(" + Field.LATEST_CONTACT_STATUS + " & " + andVal
                    + ") AND ("
                    + Field.FLAG + "&" + flag
                    + ") ORDER BY " + Field.TIMESTAMP + " DESC", null);
            if (cursor.moveToFirst()) {
                prevTime = cursor.getLong(0);
                prevLocalId = cursor.getLong(1);
                prevLatestContactStatus = cursor.getInt(2);
            }
        } catch (SQLException e) {
            return 0;

        } finally {
            CloseUtils.close(cursor);
        }
        if (prevTime != null && newUpdateTime != null) {
            if (newUpdateTime >= prevTime) {
                ContentValues cv = new ContentValues();
                cv.put(Field.LATEST_CONTACT_STATUS.toString(),
                        prevLatestContactStatus & (~andVal));
                if (writableDb.update(TABLE_NAME, cv, Field.LOCAL_ACTIVITY_ID
                        + "=" + prevLocalId, null) <= 0) {
                    LogUtils.logE("ActivitiesTable.addTimelineEvents() "
                            + "Unable to update timeline as the latest");
                    return 0;
                }
                return andVal;
            } else {
                return 0;
            }
        }
        return andVal;
    }

    /**
     * Checks if an activity exists in the database.
     *
     * @param nativeId The native ID which links the activity with the record in
     *            the native table.
     * @param type The native type (An ordinal from the #TimelineNativeTypes
     *            enumeration)
     * @param readableDb Readable SQLite database
     * @return true if the activity was found, false otherwise
     */
    private static boolean findNativeActivity(final int nativeId,
            final int type, final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper.findNativeActivity()");
        Cursor cursor = null;
        boolean result = false;
        try {
            final String[] args = {
                    Integer.toString(nativeId), Integer.toString(type)
            };
            cursor = readableDb.rawQuery("SELECT " + Field.ACTIVITY_ID
                    + " FROM " + TABLE_NAME + " WHERE " + Field.NATIVE_ITEM_ID
                    + "=? AND " + Field.NATIVE_ITEM_TYPE + "=?", args);
            if (cursor.moveToFirst()) {
                result = true;
            }
        } finally {
            CloseUtils.close(cursor);
        }
        return result;
    }

    /**
     * Returns a string which can be added to the where clause in an SQL query
     * on the activities table, to filter the result for a specific contact or
     * name. The clause will prioritise in the following way:
     * <ol>
     * <li>Use localContactId - If it not null</li>
     * <li>Use name - If this is a valid telephone number, the match will be
     * done to ensure that the same phone number written in different ways will
     * be included in the group.
     * </ol>
     *
     * @param localContactId The local contact ID, or null if the contact does
     *            not exist in the People database.
     * @param name A string containing the name, or a telephone number/email
     *            identifying the contact.
     * @return The where clause string
     */
    private static String fetchNameWhereClause(final Long localContactId,
            final String name) {
        DatabaseHelper.trace(false, "DatabaseHelper.fetchNameWhereClause()");
        if (localContactId != null) {
            return Field.LOCAL_CONTACT_ID + "=" + localContactId;
        }

        if (name == null) {
            return null;
        }
        final String searchName = DatabaseUtils.sqlEscapeString(name);
        if (PhoneNumberUtils.isWellFormedSmsAddress(name)) {
            return "PHONE_NUMBERS_EQUAL(" + Field.CONTACT_NAME + ","
                + searchName + ")";
        } else {
            return Field.CONTACT_NAME + "=" + searchName;
        }
    }


    /**
     * Returns the timeline summary data from the current location of the given
     * cursor. The cursor can be obtained using
     * {@link #fetchTimelineEventList(Long, TimelineNativeTypes[],
     * SQLiteDatabase)} or
     * {@link #fetchTimelineEventsForContact(Long, Long, String,
     * TimelineNativeTypes[], SQLiteDatabase)}.
     *
     * @param cursor Cursor in the required position.
     * @return A filled out TimelineSummaryItem object
     */
    public static TimelineSummaryItem getTimelineData(final Cursor cursor) {
        DatabaseHelper.trace(false, "DatabaseHelper.getTimelineData()");
        TimelineSummaryItem item = new TimelineSummaryItem();
        item.mLocalActivityId =
            SqlUtils.setLong(cursor, Field.LOCAL_ACTIVITY_ID.toString(), null);
        item.mTimestamp =
            SqlUtils.setLong(cursor, Field.TIMESTAMP.toString(), null);
        item.mContactName =
            SqlUtils.setString(cursor, Field.CONTACT_NAME.toString());
        if (!cursor.isNull(
                cursor.getColumnIndex(Field.CONTACT_AVATAR_URL.toString()))) {
            item.mHasAvatar = true;
        }
        item.mLocalContactId =
            SqlUtils.setLong(cursor, Field.LOCAL_CONTACT_ID.toString(), null);
        item.mTitle = SqlUtils.setString(cursor, Field.TITLE.toString());
        item.mDescription =
            SqlUtils.setString(cursor, Field.DESCRIPTION.toString());
        item.mContactNetwork =
            SqlUtils.setString(cursor, Field.CONTACT_NETWORK.toString());
        item.mNativeItemType =
            SqlUtils.setInt(cursor, Field.NATIVE_ITEM_TYPE.toString(), null);
        item.mNativeItemId =
            SqlUtils.setInt(cursor, Field.NATIVE_ITEM_ID.toString(), null);
        item.mType =
            SqlUtils.setActivityItemType(cursor, Field.TYPE.toString());
        item.mContactId =
            SqlUtils.setLong(cursor, Field.CONTACT_ID.toString(), null);
        item.mUserId = SqlUtils.setLong(cursor, Field.USER_ID.toString(), null);
        item.mNativeThreadId =
            SqlUtils.setInt(cursor, Field.NATIVE_THREAD_ID.toString(), null);
        item.mContactAddress =
            SqlUtils.setString(cursor, Field.CONTACT_ADDRESS.toString());
        item.mIncoming = SqlUtils.setTimelineSummaryItemType(cursor,
                Field.INCOMING.toString());
        return item;
    }

    /**
     * Fetches timeline events for a specific contact identified by local
     * contact ID, name or address. Events returned will be in
     * reverse-chronological order. If a native type list is provided the result
     * will be filtered by the list.
     *
     * @param timeStamp Only events from this time will be returned
     * @param localContactId The local contact ID if the contact is in the
     *            People database, or null.
     * @param name The name or address of the contact (required if local contact
     *            ID is NULL).
     * @param nativeTypes A list of required native types to filter the result,
     *            or null to return all timeline events for the contact.
     * @param readableDb Readable SQLite database
     * @param networkName The name of the network the contacts belongs to
     *            (required in order to provide appropriate chat messages
     *            filtering) If the parameter is null messages from all networks
     *            will be returned. The values are the
     *            SocialNetwork.VODAFONE.toString(),
     *            SocialNetwork.GOOGLE.toString(), or
     *            SocialNetwork.MICROSOFT.toString() results.
     * @return The cursor that can be read using
     *         {@link #getTimelineData(Cursor)}.
     */
    public static Cursor fetchTimelineEventsForContact(final Long timeStamp,
            final Long localContactId, final String name,
            final TimelineNativeTypes[] nativeTypes, final String networkName,
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "fetchTimelineEventsForContact()");
        try {
            String typesQuery = " AND ";
            if (nativeTypes.length > 0) {
                StringBuffer typesQueryBuffer = new StringBuffer();
                typesQueryBuffer.append(" AND (");
                for (int i = 0; i < nativeTypes.length; i++) {
                    final TimelineNativeTypes type = nativeTypes[i];
                    typesQueryBuffer.append(Field.NATIVE_ITEM_TYPE
                            + "=" + type.ordinal());
                    if (i < nativeTypes.length - 1) {
                        typesQueryBuffer.append(" OR ");
                    }
                }
                typesQueryBuffer.append(") AND ");
                typesQuery = typesQueryBuffer.toString();
            }

            /** Filter by account. **/
            String networkQuery = "";
            String queryNetworkName = networkName;
            if (queryNetworkName != null) {
                if (queryNetworkName.equals(SocialNetwork.PC.toString())
                        || queryNetworkName.equals(
                                SocialNetwork.MOBILE.toString())) {
                    queryNetworkName = SocialNetwork.VODAFONE.toString();
                }
                networkQuery = Field.CONTACT_NETWORK + "='" + queryNetworkName
                    + "' AND ";
            }

            String whereAppend;
            if (localContactId == null) {
                whereAppend = Field.LOCAL_CONTACT_ID + " IS NULL AND "
                        + fetchNameWhereClause(localContactId, name);
                if (whereAppend == null) {
                    return null;
                }
            } else {
                whereAppend = Field.LOCAL_CONTACT_ID + "=" + localContactId;
            }
            String query = "SELECT " + Field.LOCAL_ACTIVITY_ID + ","
                + Field.TIMESTAMP + "," + Field.CONTACT_NAME + ","
                + Field.CONTACT_AVATAR_URL + "," + Field.LOCAL_CONTACT_ID + ","
                + Field.TITLE + "," + Field.DESCRIPTION + ","
                + Field.CONTACT_NETWORK + "," + Field.NATIVE_ITEM_TYPE + ","
                + Field.NATIVE_ITEM_ID + "," + Field.TYPE + ","
                + Field.CONTACT_ID + "," + Field.USER_ID + ","
                + Field.NATIVE_THREAD_ID + "," + Field.CONTACT_ADDRESS + ","
                + Field.INCOMING + " FROM " + TABLE_NAME + " WHERE ("
                + Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + ")"
                + typesQuery + networkQuery + whereAppend
                + " ORDER BY " + Field.TIMESTAMP + " ASC";
            return readableDb.rawQuery(query, null);
        } catch (SQLiteException e) {
            LogUtils.logE("ActivitiesTable.fetchTimelineEventsForContact() "
                    + "Unable to fetch timeline event for contact list", e);
            return null;
        }
    }

    /**
     * Mark the chat timeline events for a given contact as read.
     *
     * @param localContactId Local contact ID.
     * @param networkName Name of the SNS.
     * @param writableDb Writable SQLite reference.
     * @return Number of rows affected by database update.
     */
    public static int markChatTimelineEventsForContactAsRead(
            final Long localContactId, final String networkName,
            final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "fetchTimelineEventsForContact()");
        ContentValues values = new ContentValues();
        values.put(Field.FLAG.toString(),
                ActivityItem.TIMELINE_ITEM | ActivityItem.ALREADY_READ);

        String networkQuery = "";
        if (networkName != null) {
            networkQuery = " AND (" + Field.CONTACT_NETWORK + "='"
                + networkName + "')";
        }

        final String where = Field.LOCAL_CONTACT_ID + "=" + localContactId
            + " AND " + Field.NATIVE_ITEM_TYPE + "="
            + TimelineNativeTypes.ChatLog.ordinal() + " AND ("
            /**
             * If the network is null, set all messages as read for this
             * contact.
             */
            + Field.FLAG + "=" + ActivityItem.TIMELINE_ITEM + ")"
            + networkQuery;

        return writableDb.update(TABLE_NAME, values, where, null);
    }

    /**
     * Returns the timestamp for the newest status event for a given server
     * contact.
     *
     * @param local contact id Server contact ID
     * @param readableDb Readable SQLite database
     * @return The timestamp in milliseconds, or 0 if not found.
     */
    public static long fetchLatestStatusTimestampForContact(final long localContactId,
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "fetchLatestStatusTimestampForContact()");
        Cursor cursor = null;
        try {
            String query = "SELECT MAX( " + ActivitiesTable.Field.TIMESTAMP
                + ") FROM " + TABLE_NAME + " WHERE " + Field.LOCAL_CONTACT_ID
                + " = " + localContactId + " AND " + Field.TYPE + "='" + ActivityItem.Type.CONTACT_SENT_STATUS_UPDATE.toString() + "'";
            cursor = readableDb.rawQuery(query, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getLong(0);
            } else {
                return 0;
            }
        } finally {
            CloseUtils.close(cursor);
        }
    }


    /**
     * Updates timeline when a new contact is added to the People database.
     * Updates all timeline events that are not associated with a contact and
     * have a phone number that matches the oldName parameter.
     *
     * @param oldName The telephone number (since is the name of an activity
     *            that is not associated with a contact)
     * @param newName The new name
     * @param newLocalContactId The local Contact Id for the added contact.
     * @param newContactId The server Contact Id for the added contact (or null
     *            if the contact has not yet been synced).
     * @param witeableDb Writable SQLite database
     */
    public static void updateTimelineContactNameAndId(final String oldName,
            final String newName, final Long newLocalContactId,
            final Long newContactId, final SQLiteDatabase witeableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "updateTimelineContactNameAndId()");

        try {
            ContentValues values = new ContentValues();
            if (newName != null) {
                values.put(Field.CONTACT_NAME.toString(), newName);
            } else {
                LogUtils.logE("updateTimelineContactNameAndId() "
                        + "newName should never be null");
            }
            if (newLocalContactId != null) {
                values.put(Field.LOCAL_CONTACT_ID.toString(),
                        newLocalContactId);
            } else {
                LogUtils.logE("updateTimelineContactNameAndId() "
                        + "newLocalContactId should never be null");
            }
            if (newContactId != null) {
                values.put(Field.CONTACT_ID.toString(), newContactId);
            } else {
                /**
                 * newContactId will be null if adding a contact from the UI.
                 */
                LogUtils.logI("updateTimelineContactNameAndId() "
                        + "newContactId is null");
                /**
                 * We haven't got server Contact it, it means it haven't been
                 * synced yet.
                 */
            }

            String name = "";
            if (oldName != null) {
                name = oldName;
                LogUtils.logW("ActivitiesTable."
                        + "updateTimelineContactNameAndId() oldName is NULL");
            }
            String[] args = {
                    "2", name
            };

            String whereClause = Field.LOCAL_CONTACT_ID + " IS NULL AND "
                + Field.FLAG + "=? AND PHONE_NUMBERS_EQUAL("
                + Field.CONTACT_ADDRESS + ",?)";
            witeableDb.update(TABLE_NAME, values, whereClause, args);

        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.updateTimelineContactNameAndId() "
                    + "Unable update table", e);
            throw e;
        }
    }

    /**
     * Updates the timeline when a contact name is modified in the database.
     *
     * @param newName The new name.
     * @param localContactId Local contact Id which was modified.
     * @param witeableDb Writable SQLite database
     */
    public static void updateTimelineContactNameAndId(final String newName,
            final Long localContactId, final SQLiteDatabase witeableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "updateTimelineContactNameAndId()");
        if (newName == null || localContactId == null) {
            LogUtils.logE("updateTimelineContactNameAndId() newName or "
                    + "localContactId == null newName(" + newName
                    + ") localContactId(" + localContactId + ")");
            return;
        }

        try {
            ContentValues values = new ContentValues();
            Long cId = ContactsTable.fetchServerId(localContactId, witeableDb);
            values.put(Field.CONTACT_NAME.toString(), newName);
            if (cId != null) {
                values.put(Field.CONTACT_ID.toString(), cId);
            }
            String[] args = {
                localContactId.toString()
            };
            String whereClause = Field.LOCAL_CONTACT_ID + "=?";
            witeableDb.update(TABLE_NAME, values, whereClause, args);

        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.updateTimelineContactNameAndId()"
                    + " Unable update table", e);
        }
    }

    /**
     * Updates the timeline entries in the activities table to remove deleted
     * contact info.
     *
     * @param localContactId - the contact id that has been deleted
     * @param writeableDb - reference to the database
     */
    public static void removeTimelineContactData(final Long localContactId,
            final SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "removeTimelineContactData()");
        if (localContactId == null) {
            LogUtils.logE("removeTimelineContactData() localContactId == null "
                    + "localContactId(" + localContactId + ")");
            return;
        }
        try {
            String[] args = {
                localContactId.toString()
            };
            String query = "UPDATE "
                    + TABLE_NAME
                    + " SET "
                    /**
                     * TODO: AA the line below was commented out because we
                     * currently don't delete the chat messages from/to a
                     * deleted contact.
                     * - This is important in order to display the history for
                     * a contact.
                     */
                    // + Field.CONTACT_LOCAL_ID + "=NULL, "
                    + Field.CONTACT_ID + "=NULL, " + Field.CONTACT_NAME + "="
                    + Field.CONTACT_ADDRESS + " WHERE "
                    + Field.LOCAL_CONTACT_ID + "=? AND ("
                    + Field.FLAG + "&" + ActivityItem.TIMELINE_ITEM + ")";
            writeableDb.execSQL(query, args);
        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.removeTimelineContactData() Unable "
                    + "to update table: \n", e);
        }
    }

    /**
     * Removes items from the chat timeline that are not for the given contact.
     *
     * @param localContactId Given contact ID.
     * @param writeableDb Writable SQLite database.
     */
    public static void removeChatTimelineExceptForContact(
            final Long localContactId, final SQLiteDatabase writeableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "removeTimelineContactData()");
        if (localContactId == null || (localContactId == -1)) {
            LogUtils.logE("removeTimelineContactData() localContactId == null "
                    + "localContactId(" + localContactId + ")");
            return;
        }
        try {
            final long olderThan = System.currentTimeMillis()
                - Settings.HISTORY_IS_WEEK_LONG;
            final String query = Field.LOCAL_CONTACT_ID + "!=" + localContactId
                + " AND (" + Field.NATIVE_ITEM_TYPE + "="
                + TimelineNativeTypes.ChatLog.ordinal() + ") AND ("
                + Field.TIMESTAMP + "<" + olderThan + ")";
            writeableDb.delete(TABLE_NAME, query, null);
        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.removeTimelineContactData() "
                    + "Unable to update table", e);
        }
    }

    /***
     * Returns the number of users have currently have unread chat messages.
     *
     * @param readableDb Reference to a readable database.
     * @return Number of users with unread chat messages.
     */
    public static int getNumberOfUnreadChatUsers(
            final SQLiteDatabase readableDb) {
        final String query = "SELECT " + Field.LOCAL_CONTACT_ID + " FROM "
            + TABLE_NAME + " WHERE " + Field.NATIVE_ITEM_TYPE + "="
            + TimelineNativeTypes.ChatLog.ordinal() + " AND ("
                /**
                 * This condition below means the timeline is not yet marked
                 * as READ.
                 */
            + Field.FLAG + "=" + ActivityItem.TIMELINE_ITEM + ")";
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery(query, null);
            ArrayList<Long> ids = new ArrayList<Long>();
            Long id = null;
            while (cursor.moveToNext()) {
                id = cursor.getLong(0);
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids.size();

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Returns the number of unread chat messages.
     *
     * @param readableDb Reference to a readable database.
     * @return Number of unread chat messages.
     */
    public static int getNumberOfUnreadChatMessages(
            final SQLiteDatabase readableDb) {
        final String query = "SELECT " + Field.ACTIVITY_ID + " FROM "
            + TABLE_NAME + " WHERE " + Field.NATIVE_ITEM_TYPE + "="
            + TimelineNativeTypes.ChatLog.ordinal() + " AND ("
            + Field.FLAG + "=" + ActivityItem.TIMELINE_ITEM + ")";
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery(query, null);
            return cursor.getCount();

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Returns the number of unread chat messages for this contact besides this
     * network.
     *
     * @param localContactId Given contact ID.
     * @param network SNS name.
     * @param readableDb Reference to a readable database.
     * @return Number of unread chat messages.
     */
    public static int getNumberOfUnreadChatMessagesForContactAndNetwork(
            final long localContactId, final String network,
            final SQLiteDatabase readableDb) {
        final String query = "SELECT " + Field.ACTIVITY_ID + " FROM "
            + TABLE_NAME + " WHERE " + Field.NATIVE_ITEM_TYPE + "="
            + TimelineNativeTypes.ChatLog.ordinal() + " AND (" + Field.FLAG
            + "=" + ActivityItem.TIMELINE_ITEM + ") AND ("
            + Field.LOCAL_CONTACT_ID + "=" + localContactId + ") AND ("
            + Field.CONTACT_NETWORK + "!=\"" + network + "\")";
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery(query, null);
            return cursor.getCount();

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Returns the newest unread chat message.
     *
     * @param readableDb Reference to a readable database.
     * @return TimelineSummaryItem of the newest unread chat message, or NULL if
     *         none are found.
     */
    public static TimelineSummaryItem getNewestUnreadChatMessage(
            final SQLiteDatabase readableDb) {

        final String query = "SELECT " + Field.LOCAL_ACTIVITY_ID + ","
            + Field.TIMESTAMP + "," + Field.CONTACT_NAME + ","
            + Field.CONTACT_AVATAR_URL + "," + Field.LOCAL_CONTACT_ID + ","
            + Field.TITLE + "," + Field.DESCRIPTION + ","
            + Field.CONTACT_NETWORK + "," + Field.NATIVE_ITEM_TYPE + ","
            + Field.NATIVE_ITEM_ID + "," + Field.TYPE + ","
            + Field.CONTACT_ID + "," + Field.USER_ID + ","
            + Field.NATIVE_THREAD_ID + "," + Field.CONTACT_ADDRESS + ","
            + Field.INCOMING
            + " FROM " + TABLE_NAME + " WHERE "
            + Field.NATIVE_ITEM_TYPE + "="
            + TimelineNativeTypes.ChatLog.ordinal()
            + " AND (" + Field.FLAG + "=" + ActivityItem.TIMELINE_ITEM + ")";
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery(query, null);
            long max = 0;
            long time = 0;
            int index = -1;
            while (cursor.moveToNext()) {
                time = SqlUtils.setLong(cursor, Field.TIMESTAMP.toString(),
                        -1L);
                if (time > max) {
                    max = time;
                    index = cursor.getPosition();
                }
            }
            if (index != -1) {
                cursor.moveToPosition(index);
                return getTimelineData(cursor);
            } else {
                return null;
            }

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /***
     * Returns the newest outgoing chat message.
     *
     * @param readableDb Reference to a readable database.
     * @return TimelineSummaryItem of the newest unread chat message, or NULL if
     *         none are found.
     */
    public static TimelineSummaryItem getNewestOutgoingChatMessage(
            final SQLiteDatabase readableDb) {

        final String query = "SELECT " + Field.LOCAL_ACTIVITY_ID + ","
            + Field.TIMESTAMP + "," + Field.CONTACT_NAME + ","
            + Field.CONTACT_AVATAR_URL + "," + Field.LOCAL_CONTACT_ID + ","
            + Field.TITLE + "," + Field.DESCRIPTION + ","
            + Field.CONTACT_NETWORK + "," + Field.NATIVE_ITEM_TYPE + ","
            + Field.NATIVE_ITEM_ID + "," + Field.TYPE + ","
            + Field.CONTACT_ID + "," + Field.USER_ID + ","
            + Field.NATIVE_THREAD_ID + "," + Field.CONTACT_ADDRESS + ","
            + Field.INCOMING + " FROM " + TABLE_NAME + " WHERE "
            + Field.NATIVE_ITEM_TYPE + "="
            + TimelineNativeTypes.ChatLog.ordinal()
            + " AND (" + Field.INCOMING + "="
            + TimelineSummaryItem.Type.OUTGOING.ordinal()
            + ") ORDER BY " + Field.TIMESTAMP + " DESC";

        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery(query, null);
            long max = 0;
            long time = 0;
            int index = -1;
            while (cursor.moveToNext()) {
                time = SqlUtils.setLong(cursor, Field.TIMESTAMP.toString(),
                        -1L);
                if (time > max) {
                    max = time;
                    index = cursor.getPosition();
                }
            }
            if (index != -1) {
                cursor.moveToPosition(index);
                return getTimelineData(cursor);
            } else {
                return null;
            }

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /**
     * This method updates the chat message to a contact with indicated
     * localContactId on the particular networkId as unsent.
     *
     * @param localContactId long - the localContactId of the recipient
     * @param networkId String - the network id, @see SocialNetwork
     * @param writableDb SQLiteDatabase - database
     */
    public static void updateMessageAsUnsent(final long localContactId,
            final String networkId, final SQLiteDatabase writableDb) {
        if (localContactId != -1) {
            final String where = Field.LOCAL_ACTIVITY_ID + "=" + localContactId
                + " AND " + Field.CONTACT_NETWORK + "=\"" + networkId + "\"";
            ContentValues values = new ContentValues(1);
            values.put(Field.INCOMING.toString(),
                    TimelineSummaryItem.Type.UNSENT.ordinal());
            writableDb.update(TABLE_NAME, values, where, null);
        }
    }

    /***
     * Cleanup the Activity Table by deleting anything older than
     * CLEANUP_MAX_AGE_DAYS, or preventing the total size from exceeding
     * CLEANUP_MAX_QUANTITY.
     *
     * @param writableDb Reference to a writable SQLite Database.
     */
    public static void cleanupActivityTable(final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "DatabaseHelper.cleanupActivityTable()");
        try {
            /*
             * Delete any Activities older than CLEANUP_MAX_AGE_DAYS days.
             */
            if (CLEANUP_MAX_AGE_DAYS != -1) {
                if (writableDb.delete(TABLE_NAME, Field.TIMESTAMP + " < "
                        + ((System.currentTimeMillis()
                                / NUMBER_OF_MS_IN_A_SECOND)
                                - CLEANUP_MAX_AGE_DAYS * NUMBER_OF_MS_IN_A_DAY),
                                null) < 0) {
                    LogUtils.logE("ActivitiesTable.cleanupActivityTable() "
                            + "Unable to cleanup Activities table by date");
                }
            }
            /*
             * Delete oldest Activities, when total number of rows exceeds
             * CLEANUP_MAX_QUANTITY in quantity.
             */
            if (CLEANUP_MAX_QUANTITY != -1) {
                writableDb.execSQL("DELETE FROM " + TABLE_NAME + " WHERE "
                        + Field.LOCAL_ACTIVITY_ID + " IN (SELECT "
                        + Field.LOCAL_ACTIVITY_ID + " FROM " + TABLE_NAME
                        + " ORDER BY " + Field.TIMESTAMP
                        + " DESC LIMIT -1 OFFSET " + CLEANUP_MAX_QUANTITY
                        + ")");
            }
        } catch (SQLException e) {
            LogUtils.logE("ActivitiesTable.cleanupActivityTable() "
                    + "Unable to cleanup Activities table by date", e);
        }
    }

    /**
     * Returns the TimelineSummaryItem for the corresponding native thread Id.
     *
     * @param threadId native thread id
     * @param readableDb Readable SQLite database
     * @return The TimelineSummaryItem of the matching native thread id,
     * or NULL if none are found.
     */
    public static TimelineSummaryItem fetchTimeLineDataFromNativeThreadId(
            final String threadId,
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "DatabaseHelper."
                + "fetchTimeLineDataFromNativeThreadId()");
        Cursor cursor = null;

        try {
            String query = "SELECT * FROM " + TABLE_NAME + " WHERE "
            + Field.NATIVE_THREAD_ID + " = " + threadId + " ORDER BY "
            + Field.TIMESTAMP + " DESC";

            cursor = readableDb.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return getTimelineData(cursor);
            } else {
                return null;
            }
        } finally {
            CloseUtils.close(cursor);
        }
    }

}