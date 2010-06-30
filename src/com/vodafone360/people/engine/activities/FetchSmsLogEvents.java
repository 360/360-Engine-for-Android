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

package com.vodafone360.people.engine.activities;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.engine.activities.ActivitiesEngine.ISyncHelper;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;

/**
 * Fetches SMS/MMS log events from the Native message log. These are treated as
 * Activities and displayed in Timeline UI.
 */
public class FetchSmsLogEvents implements ISyncHelper {
    protected static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");

    /**
     * the number of timeline pages to be loaded by one ISyncHelper
     */
    private static final int MAX_PAGES_TO_LOAD_AT_ONCE = 10;

    private static final int MAX_ITEMS_PER_PAGE = 2;

    private static final int MAX_ITEMS_TO_WRITE = 10;

    private static final int MAX_DESC_LENGTH = 160;

    private static final String SMS_SORT_ORDER = "date DESC";

    private static final String[] SMS_PROJECTION = new String[] {
            "_id", "date", "address", "subject", "body", "type", "thread_id"
    };

    private static final int COLUMN_SMS_ID = 0;

    private static final int COLUMN_SMS_DATE = 1;

    private static final int COLUMN_SMS_ADDRESS = 2;

    private static final int COLUMN_SMS_SUBJECT = 3;

    private static final int COLUMN_SMS_BODY = 4;

    private static final int COLUMN_SMS_TYPE = 5;

    private static final int COLUMN_SMS_THREAD_ID = 6;

    private static final int MESSAGE_TYPE_INBOX = 1;

    private static final int MESSAGE_TYPE_SENT = 2;

    /**
     * Internal states for message log sync: Idle,, fetching SMS events,
     * fetching MMS events.
     */
    private enum InternalState {
        IDLE, FETCHING_SMS_NEXT_PAGE, FETCHING_MMS_NEXT_PAGE
    }

    private Context mContext;

    private ActivitiesEngine mEngine;

    private DatabaseHelper mDb;

    private ContentResolver mCr;

    private InternalState mInternalState;

    private Cursor mSmsCursor;

    private Cursor mMmsCursor;

    private ArrayList<TimelineSummaryItem> mSyncItemList = new ArrayList<TimelineSummaryItem>();
    
    /**
     * The result of fetching the timelines: ERROR_NOT_READY, SUCCESS (no change), 
     *  UPDATED_TIMELINES_FROM_NATIVE (DB changed).
     */
    private ServiceStatus mStatus = ServiceStatus.SUCCESS; 

    /**
     * is true if newer events need to be loaded, false - if older
     */
    private boolean mRefresh;

    /**
     * the number of pages have been read
     */
    private int mPageCount;

    /**
     * the current oldest message time
     */
    private long mOldestMessage;

    /**
     * the current newest message time
     */
    private long mNewestMessage;

    /**
     * Constructor.
     * 
     * @param context Context - actually RemoteServe's Context.
     * @param engine Handle to ActivitiesEngine.
     * @param db Handle to DatabaseHelper.
     * @param refresh - true if we need to fetch new sms, false if older.
     */
    FetchSmsLogEvents(Context context, ActivitiesEngine engine, DatabaseHelper db, boolean refresh) {
        mContext = context;
        mEngine = engine;
        mDb = db;
        mCr = mContext.getContentResolver();
        mInternalState = InternalState.IDLE;
        mRefresh = refresh;
    }

    /**
     * Drive internal state machine, either start call log sync, fetch next page
     * of SMS items, next page of MMS items or complete call-log sync operation.
     */
    @Override
    public void run() {
        switch (mInternalState) {
            case IDLE:
                startSyncSms();
                break;
            case FETCHING_SMS_NEXT_PAGE:
                syncNextSmsPage();
                break;
            case FETCHING_MMS_NEXT_PAGE:
                syncNextMmsPage();
                break;
            default:
                mStatus = ServiceStatus.ERROR_NOT_READY;
                complete(mStatus);
                break;
        }
    }

    /**
     * /** Completion of fetch from Native message log. Notify ActivitiesEngine
     * that message-log sync. has completed. Close Cursors.
     * 
     * @param status ServiceStatus containing result of sync.
     */
    private void complete(ServiceStatus status) {
        mEngine.onSyncHelperComplete(status);
        cancel();
        if (mMmsCursor != null) {
            mMmsCursor.close();
            mMmsCursor = null;
        }
        if (mSmsCursor != null) {
            mSmsCursor.close();
            mSmsCursor = null;
        }
    }

    /**
     * Start sync of SMS message events. Use Timeline last update time-stamp to
     * ensure we only fetch 'new' events.
     */
    private void startSyncSms() {
        mNewestMessage = StateTable.fetchLatestSmsTime(mDb.getReadableDatabase());
        mOldestMessage = StateTable.fetchOldestSmsTime(mDb.getReadableDatabase());

        // at 1st sync the StateTable contains no value: so for "refresh" set
        // value to 0, for "more" to current time
        if (mOldestMessage == 0) {
            mOldestMessage = System.currentTimeMillis();
            StateTable.modifyOldestSmsTime(mOldestMessage, mDb.getWritableDatabase());
        }

        String whereClause = mRefresh ? "date > " + mNewestMessage : "date < " + mOldestMessage;

        mSmsCursor = mCr.query(SMS_CONTENT_URI, SMS_PROJECTION, whereClause, null, SMS_SORT_ORDER);
        mInternalState = InternalState.FETCHING_SMS_NEXT_PAGE;
        syncNextSmsPage();
    }

    /**
     * Start sync. of MMS message events. Use Timeline last update time-stamp to
     * ensure we only fetch 'new' events.
     */
    private void startSyncMms() {
        mNewestMessage = StateTable.fetchLatestMmsTime(mDb.getReadableDatabase());
        mOldestMessage = StateTable.fetchOldestMmsTime(mDb.getReadableDatabase());

        // at 1st sync the StateTable contains no value: so for "refresh" set
        // value to 0, for "more" to current time
        if (mOldestMessage == 0) {
            mOldestMessage = System.currentTimeMillis();
            StateTable.modifyOldestMmsTime(mOldestMessage, mDb.getWritableDatabase());
        }
        mMmsCursor = MmsDecoder.fetchMmsListCursor(mCr, mRefresh, mNewestMessage, mOldestMessage);
        mInternalState = InternalState.FETCHING_MMS_NEXT_PAGE;
        syncNextMmsPage();
    }

    /**
     * Sync. next page of SMS events (current page size is 2).
     */
    private void syncNextSmsPage() {
        if (mSmsCursor.isAfterLast()) {
            mSmsCursor.close();
            mSmsCursor = null;
            startSyncMms();
            return;
        }
        boolean finished = false;
        if (mPageCount < MAX_PAGES_TO_LOAD_AT_ONCE) {
            int count = 0;
            int id = 0;
            long timestamp = 0;

            while (count < MAX_ITEMS_PER_PAGE && mSmsCursor.moveToNext()) {
                id = mSmsCursor.getInt(COLUMN_SMS_ID);
                timestamp = mSmsCursor.getLong(COLUMN_SMS_DATE);
                if (mRefresh) {
                    if (timestamp < mNewestMessage) {
                        finished = true;
                        break;
                    }
                } else {
                    if (timestamp > mOldestMessage) {
                        finished = true;
                        break;
                    }
                }
                addSmsData(id);
                count++;
            }
            mPageCount++;
            if ((count == MAX_ITEMS_PER_PAGE && mSyncItemList.size() == MAX_ITEMS_TO_WRITE)
                    || (count < MAX_ITEMS_PER_PAGE) || (mPageCount == MAX_PAGES_TO_LOAD_AT_ONCE)) {
                ServiceStatus status = mDb.addTimelineEvents(mSyncItemList, false);
                updateTimestamps();
                saveTimeStampSms();
                mSyncItemList.clear();
                if (ServiceStatus.SUCCESS != status) {
                    mStatus = status;
                    complete(mStatus);
                    return;
                }
            }
        } else {
            finished = true;
        }
        if (finished) {
            saveTimeStampSms();
            mPageCount = 0;
            mSmsCursor.close();
            mSmsCursor = null;
            startSyncMms();
        }
    }

    /**
     * This method goes through the event list and updates the current newest
     * and oldest message timestamps.
     */
    private void updateTimestamps() {
        if (mSyncItemList.size() > 0) {
            long max = ((TimelineSummaryItem)mSyncItemList.get(0)).mTimestamp;
            long min = max;
            for (TimelineSummaryItem item : mSyncItemList) {
                if (item.mTimestamp > max) {
                    max = item.mTimestamp;
                }
                if (item.mTimestamp < min) {
                    min = item.mTimestamp;
                }
            }
            if (mNewestMessage < max) {
                mNewestMessage = max;
            }
            if (mOldestMessage > min) {
                mOldestMessage = min;
            }
        }
    }

    /**
     * This method updates the newest and oldest MMS timestamps in the database.
     */
    private void saveTimeStampMms() {
        long saved = StateTable.fetchOldestMmsTime(mDb.getReadableDatabase());
        if (mOldestMessage < saved) {
            StateTable.modifyOldestMmsTime(mOldestMessage, mDb.getWritableDatabase());
            LogUtils.logD("FetchMMSEvents saveTimestamp: oldest timeline update set to = "
                    + mOldestMessage);
            mStatus = ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE;
        }
        saved = StateTable.fetchLatestMmsTime(mDb.getReadableDatabase());
        if (mNewestMessage > saved) {
            StateTable.modifyLatestMmsTime(mNewestMessage, mDb.getWritableDatabase());
            LogUtils.logD("FetchMMSEvents saveTimestamp: newest timeline update set to = "
                    + mNewestMessage);
            mStatus = ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE;
        }
    }

    /**
     * This method updates the newest and oldest SMS timestamps in the database.
     */
    private void saveTimeStampSms() {
        long saved = StateTable.fetchOldestSmsTime(mDb.getReadableDatabase());
        if (mOldestMessage < saved) {
            StateTable.modifyOldestSmsTime(mOldestMessage, mDb.getWritableDatabase());
            LogUtils.logD("FetchSMSEvents saveTimestamp: oldest timeline update set to = "
                    + mOldestMessage);
            mStatus = ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE;
        }
        saved = StateTable.fetchLatestSmsTime(mDb.getReadableDatabase());
        if (mNewestMessage > saved) {
            StateTable.modifyLatestSmsTime(mNewestMessage, mDb.getWritableDatabase());
            LogUtils.logD("FetchSMSEvents saveTimestamp: newest timeline update set to = "
                    + mNewestMessage);
            mStatus = ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE;
        }
    }

    /**
     * Sync. next page of MMS events (current page size is 2).
     */
    private void syncNextMmsPage() {
        if (mMmsCursor.isAfterLast()) {
            complete(mStatus);
            return;
        }
        boolean finished = false;
        if (mPageCount < MAX_PAGES_TO_LOAD_AT_ONCE) {
            int count = 0;
            while (count < MAX_ITEMS_PER_PAGE && mMmsCursor.moveToNext()) {
                final long timestamp = MmsDecoder.getTimestamp(mMmsCursor);
                if (mRefresh) {
                    if (timestamp < mNewestMessage) {
                        finished = true;
                        break;
                    }
                } else {
                    if (timestamp > mOldestMessage) {
                        finished = true;
                        break;
                    }
                }
                TimelineSummaryItem item = new TimelineSummaryItem();
                if (MmsDecoder.getMmsData(mContext, mCr, mMmsCursor, item, mDb, MAX_DESC_LENGTH)) {
                    LogUtils.logD("FetchSmsLogEvents.syncNextMmsPage(): id = " + item.mNativeItemId
                            + ", name = " + item.mContactName + ", date = " + item.mTimestamp
                            + ", title = " + item.mTitle + ", desc = " + item.mDescription + "\n");
                    mSyncItemList.add(item);
                }
                count++;
            }
            mPageCount++;
            if ((count == MAX_ITEMS_PER_PAGE && mSyncItemList.size() == MAX_ITEMS_TO_WRITE)
                    || (count < MAX_ITEMS_PER_PAGE) || (mPageCount == MAX_PAGES_TO_LOAD_AT_ONCE)) {
                ServiceStatus status = mDb.addTimelineEvents(mSyncItemList, false);
                updateTimestamps();
                mSyncItemList.clear();
                if (ServiceStatus.SUCCESS != status) {
                    complete(mStatus);
                    return;
                }
            }
        } else {
            finished = true;
        }

        if (finished) {
            saveTimeStampMms();
            complete(mStatus);
        }
    }

    /**
     * Create TimelineSummaryItem from Native message-log item.
     * 
     * @param id ID of item from Native log.
     */
    private void addSmsData(int id) {
        ActivityItem.Type type =
            nativeToNpTypeConvert(mSmsCursor.getInt(COLUMN_SMS_TYPE));
        if (type == null) {
            return;
        }

        TimelineSummaryItem item = new TimelineSummaryItem();
        String address = null;
        /* Francisco: Unknown contact SMS sending bug resolved here
         * I am keeping previous case SN_MESSAGE_RECEIVED besides MESSAGE_SMS_RECEIVED just to be safe.
         */
        if (type == ActivityItem.Type.SN_MESSAGE_RECEIVED || 
                type == ActivityItem.Type.MESSAGE_SMS_RECEIVED) {
            item.mIncoming = TimelineSummaryItem.Type.INCOMING;
        } else {
            item.mIncoming = TimelineSummaryItem.Type.OUTGOING;
        }
        item.mNativeItemId = id;
        item.mNativeItemType = ActivitiesTable.TimelineNativeTypes.SmsLog.ordinal();
        item.mType = type;
        item.mTimestamp = mSmsCursor.getLong(COLUMN_SMS_DATE);
        item.mTitle = DateFormat.getDateInstance().format(new Date(item.mTimestamp));
        item.mNativeThreadId = mSmsCursor.getInt(COLUMN_SMS_THREAD_ID);
        item.mDescription = null;
        if (!mSmsCursor.isNull(COLUMN_SMS_SUBJECT)) {
            item.mDescription = mSmsCursor.getString(COLUMN_SMS_SUBJECT);
        }
        if (item.mDescription == null || item.mDescription.length() == 0) {
            if (!mSmsCursor.isNull(COLUMN_SMS_BODY)) {
                item.mDescription = mSmsCursor.getString(COLUMN_SMS_BODY);
            }
        }
        if (!mSmsCursor.isNull(COLUMN_SMS_ADDRESS)) {
            address = mSmsCursor.getString(COLUMN_SMS_ADDRESS);
        }
        item.mContactName = address;
        item.mContactAddress = address;

        Contact c = new Contact();
        ContactDetail phoneDetail = new ContactDetail();
        ServiceStatus status = mDb.fetchContactInfo(address, c, phoneDetail);
        if (ServiceStatus.SUCCESS == status) {
            item.mLocalContactId = c.localContactID;
            item.mContactId = c.contactID;
            item.mUserId = c.userID;
            item.mContactName = null;
            for (ContactDetail d : c.details) {
                switch (d.key) {
                    case VCARD_NAME:
                        final VCardHelper.Name name = d.getName();
                        if (name != null) {
                            item.mContactName = d.getName().toString();
                        }
                        break;
                    case VCARD_IMADDRESS:
                        item.mContactNetwork = d.alt;
                        break;
                    default:
                        // do nothing
                        break;
                }
            }
        }
        if (item.mContactId == null) {
            LogUtils.logI("FetchSmsLogEvents.addSmsData: id " + item.mNativeItemId + ", time "
                    + item.mTitle + ", description " + item.mDescription);
        } else {
            LogUtils.logI("FetchSmsLogEvents.addSmsData: id " + item.mNativeItemId + ", name = "
                    + item.mContactName + ", time " + item.mTitle + ", description "
                    + item.mDescription);
        }
        mSyncItemList.add(item);
    }

    /**
     * Convert Native message type (Inbox, Sent) to corresponding ActivityItem
     * type.
     * 
     * @param type Native message type.
     * @return ActivityItem type.
     */
    private ActivityItem.Type nativeToNpTypeConvert(int type) {
        switch (type) {
            case MESSAGE_TYPE_INBOX:
                return ActivityItem.Type.MESSAGE_SMS_RECEIVED;
            case MESSAGE_TYPE_SENT:
                return ActivityItem.Type.MESSAGE_SMS_SENT;
            default:
                return null;
        }
    }

    /**
     * Cancel message log sync.
     */
    @Override
    public void cancel() {
        mInternalState = InternalState.IDLE;
    }

}
