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
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;

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
import com.vodafone360.people.utils.ServiceUtils;

/**
 * Fetches call log events from the Native call log. These are treated as
 * Activities and displayed in Timeline UI.
 */
public class FetchCallLogEvents implements ISyncHelper {
    private static final int MAX_CALL_LOG_ITEMS_PER_PAGE = 2;

    private static final int MAX_ITEMS_TO_WRITE = 10;

    /**
     * the number of timeline pages to be loaded by one ISyncHelper
     */
    private static final int MAX_NUMBER_OF_PAGES = 10;

    // "-1" means number is unknown
    private static final String NATIVE_NUMBER_UNKNOWN_STRING = "-1";

    private Context mContext;

    private ContentResolver mCr;

    private Cursor mNativeCursor;

    private ActivitiesEngine mEngine;

    private DatabaseHelper mDb;

    private final ArrayList<TimelineSummaryItem> mSyncItemList = new ArrayList<TimelineSummaryItem>();
    
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
     * the current oldest phone call time
     */
    private long mOldestPhoneCall;

    /**
     * the current newest phone call time
     */
    private long mNewestPhoneCall;

    /**
     * Internal states for Call log event sync.
     */
    private static enum InternalState {
        IDLE, FETCHING_NEXT_PAGE
    }

    private InternalState mInternalState;

    /**
     * Set of Call log items to be fetched by query on Native call-log.
     */
    private static final String[] CALL_LOG_PROJECTION = new String[] {
            Calls._ID, Calls.NUMBER, Calls.DATE, Calls.TYPE,
    };

    private static final int COLUMN_CALLLOG_ID = 0;

    private static final int COLUMN_CALLLOG_PHONE = 1;

    private static final int COLUMN_CALLLOG_DATE = 2;

    private static final int COLUMN_CALLLOG_TYPE = 3;

    /**
     * Constructor.
     * 
     * @param context Context - actually RemoteServe's Context.
     * @param engine Handle to ActivitiesEngine.
     * @param db Handle to DatabaseHelper.
     * @param refresh boolean - true if new phone call need to be fetched, false
     *            - if older
     */
    FetchCallLogEvents(Context context, ActivitiesEngine engine, DatabaseHelper db, boolean refresh) {
        mContext = context;
        mEngine = engine;
        mDb = db;
        mCr = mContext.getContentResolver();
        mInternalState = InternalState.IDLE;
        mRefresh = refresh;
    }

    /**
     * Drive internal state machine, either start call log sync, fetch next page
     * of call-log items or complete call-log sync operation.
     */
    @Override
    public void run() {
        switch (mInternalState) {
            case IDLE:
                startSyncCallLog();
                break;
            case FETCHING_NEXT_PAGE:
                syncNextPage();
                break;
            default:
                mStatus = ServiceStatus.ERROR_NOT_READY;
                complete(mStatus);
                break;
        }
    }

    /**
     * Cancel fetch of call log events. Close Cursor to Native Call log. Reset
     * state to IDLE.
     */
    @Override
    public void cancel() {
        if (mNativeCursor != null) {
            mNativeCursor.close();
        }
        mInternalState = InternalState.IDLE;
    }

    /**
     * Start sync of call events from Native call log. Use Timeline last update
     * time-stamp to ensure we only fetch 'new' events.
     */
    private void startSyncCallLog() {
        mOldestPhoneCall = StateTable.fetchOldestPhoneCallTime(mDb.getReadableDatabase());
        mNewestPhoneCall = StateTable.fetchLatestPhoneCallTime(mDb.getReadableDatabase());
        // at 1st sync the StateTable contains no value: so for "refresh" set
        // value to 0, for "more" to current time
        if (mOldestPhoneCall == 0) {
            mOldestPhoneCall = System.currentTimeMillis();
            StateTable.modifyOldestPhoneCallTime(mOldestPhoneCall, mDb.getWritableDatabase());
        }
        String whereClause = mRefresh ? Calls.DATE + ">" + mNewestPhoneCall : Calls.DATE + "<"
                + mOldestPhoneCall;
        mNativeCursor = mCr.query(Calls.CONTENT_URI, CALL_LOG_PROJECTION, whereClause, null,
                Calls.DATE + " DESC");

        mInternalState = InternalState.FETCHING_NEXT_PAGE;
        syncNextPage();
    }

    /**
     * Sync next page of call-log events (page-size is 2).
     */
    private void syncNextPage() {
        if (mNativeCursor.isAfterLast()) {
            complete(mStatus);
            return;
        }

        boolean finished = false;
        if (mPageCount < MAX_NUMBER_OF_PAGES) {
            int id = 0;
            int count = 0;
            long timestamp = 0;
            while (count < MAX_CALL_LOG_ITEMS_PER_PAGE && mNativeCursor.moveToNext()) {
                id = mNativeCursor.getInt(COLUMN_CALLLOG_ID);
                timestamp = mNativeCursor.getLong(COLUMN_CALLLOG_DATE);
                if (mRefresh) {
                    if (timestamp < mNewestPhoneCall) {
                        finished = true;
                        break;
                    }
                } else {
                    if (timestamp > mOldestPhoneCall) {
                        finished = true;
                        break;
                    }
                }
                addCallLogData(id);
                count++;
            }
            mPageCount++;
            if ((count == MAX_CALL_LOG_ITEMS_PER_PAGE && mSyncItemList.size() == MAX_ITEMS_TO_WRITE)
                    || (count < MAX_CALL_LOG_ITEMS_PER_PAGE) || (mPageCount == MAX_NUMBER_OF_PAGES)) {
                ServiceStatus status = updateDatabase();
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
            saveTimestamp();
            complete(mStatus);
        }
    }

    private ServiceStatus updateDatabase() {
        ServiceStatus status = mDb.addTimelineEvents(mSyncItemList, true);
        updateTimeStamps();
        saveTimestamp();
        mSyncItemList.clear();

        return status;
    }


    /**
     * This method goes through the event list and updates the current newest
     * and oldest phone call timestamps.
     */
    private void updateTimeStamps() {
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
            if (mNewestPhoneCall < max) {
                mNewestPhoneCall = max;
            }
            if (mOldestPhoneCall > min) {
                mOldestPhoneCall = min;
            }
        }
    }

    /**
     * This method updates the newest and oldest phone calls timestamps in the
     * database.
     */
    private void saveTimestamp() {
        long saved = StateTable.fetchOldestPhoneCallTime(mDb.getReadableDatabase());
        if (mOldestPhoneCall < saved) {
            StateTable.modifyOldestPhoneCallTime(mOldestPhoneCall, mDb.getWritableDatabase());
            LogUtils.logD("FetchCallLogEvents saveTimestamp: oldest timeline update set to = "
                    + mOldestPhoneCall);
            mStatus = ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE;
        }
        saved = StateTable.fetchLatestPhoneCallTime(mDb.getReadableDatabase());
        if (mNewestPhoneCall > saved) {
            StateTable.modifyLatestPhoneCallTime(mNewestPhoneCall, mDb.getWritableDatabase());
            LogUtils.logD("FetchCallLogEvents saveTimestamp: newest timeline update set to = "
                    + mNewestPhoneCall);
            mStatus = ServiceStatus.UPDATED_TIMELINES_FROM_NATIVE;
        }
    }

    /**
     * Create TimelineSummaryItem from Native call-log item.
     * 
     * @param id ID of item from Native log.
     */
    private void addCallLogData(int id) {
        TimelineSummaryItem item = new TimelineSummaryItem();

        final String phoneNo = mNativeCursor.getString(COLUMN_CALLLOG_PHONE);

        item.mNativeItemId = id;
        item.mNativeItemType = ActivitiesTable.TimelineNativeTypes.CallLog.ordinal();
        item.mType = nativeTypeToNpType(mNativeCursor.getInt(COLUMN_CALLLOG_TYPE));
        item.mTimestamp = mNativeCursor.getLong(COLUMN_CALLLOG_DATE);
        item.mTitle = DateFormat.getDateInstance().format(new Date(item.mTimestamp));
        item.mDescription = null;

        if (phoneNo.compareToIgnoreCase(NATIVE_NUMBER_UNKNOWN_STRING) != 0) {
            item.mContactName = PhoneNumberUtils.formatNumber(phoneNo);
            item.mContactAddress = phoneNo;
        } else {
            item.mContactName = null;
            item.mContactAddress = null;
        }

        Contact c = new Contact();
        ContactDetail phoneDetail = new ContactDetail();
        ServiceStatus status = mDb.fetchContactInfo(phoneNo, c, phoneDetail);
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
                            item.mContactName = name.toString();
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
            item.mDescription = ServiceUtils.getDetailTypeString(mContext.getResources(),
                    phoneDetail.keyType)
                    + " " + phoneNo;
        }
        if (item.mContactId == null) {
            LogUtils.logI("FetchCallLogEvents.addCallLogData: id " + item.mNativeItemId + ", time "
                    + item.mTitle + ", number " + item.mContactName);
        } else {
            LogUtils.logI("FetchCallLogEvents.addCallLogData: id " + item.mNativeItemId
                    + ", name = " + item.mContactName + ", time " + item.mTitle + ", number "
                    + item.mDescription);
        }
        mSyncItemList.add(item);
    }

    /**
     * Completion of fetch from Native call log. Notify ActivitiesEngine that
     * call-log sync. has completed.
     * 
     * @param status ServiceStatus containing result of sync.
     */
    private void complete(ServiceStatus status) {
        mEngine.onSyncHelperComplete(status);
        cancel();
    }

    /**
     * Convert native call type to type stored in People's ActivityItem.
     * 
     * @param nativeType Native call type.
     * @return People's ActivityItem call type.
     */
    private static ActivityItem.Type nativeTypeToNpType(int nativeType) {
        switch (nativeType) {
            case Calls.INCOMING_TYPE:
                return ActivityItem.Type.CALL_RECEIVED;
            case Calls.MISSED_TYPE:
                return ActivityItem.Type.CALL_MISSED;
            case Calls.OUTGOING_TYPE:
                return ActivityItem.Type.CALL_DIALED;
            default:
                LogUtils.logW("FetchCallLogEvents.nativeTypeToNpType() "
                        + "Unknown type[" + nativeType + "]");
                return null;
        }
    }
}