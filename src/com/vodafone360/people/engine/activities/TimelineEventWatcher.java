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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.CallLog.Calls;

import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineNativeTypes;
import com.vodafone360.people.utils.LogUtils;

/**
 * Allow ActivitiesEngine to 'watch' for changes in the Native call and message
 * logs allowing re-sync of events if required.
 */
public class TimelineEventWatcher {
    private ActivitiesEngine mEngine;

    private ContentResolver mCr;

    /**
     * ContentObserver derived class that allows ActivitiesEngine to receive
     * notifications of changes in call and message logs - thus prompting a
     * sync. of these events.
     */
    private class TimelineContentObserver extends ContentObserver {
        /**
         * Type of the native event detected: phone call or SMS, @see TimelineNativeTypes  
         */
        private ActivitiesTable.TimelineNativeTypes mType; 
        
        public TimelineContentObserver(ActivitiesTable.TimelineNativeTypes type) {
            super(new Handler());
            mType = type;
        }

        /**
         * Notification of a change event in the content we are observing.
         * Request an Activities sync. event.
         */
        @Override
        public void onChange(boolean selfChange) {
            LogUtils.logV("TimelineEventWatcher.TimelineContentObserver.onChange()");
            if (!selfChange) {
                LogUtils.logV("TimelineEventWatcher.TimelineContentObserver.onChange() - request activity sync");
                if (mType == TimelineNativeTypes.CallLog) {
                    mEngine.addGetNewPhonesCallsRequest();    
                } else {
                    mEngine.addGetNewSMSRequest();
                }
            }
        }
    }

    private TimelineContentObserver mCallLogObserver;

    private TimelineContentObserver mSmsObserver;

    private TimelineContentObserver mMmsObserver;

    /**
     * Constructor. Creates observers to listen for changes in Native call and
     * message logs.
     * 
     * @param context Context - use RemoteService's Context.
     * @param engine Handle to ActivitiesEngine.
     */
    TimelineEventWatcher(Context context, ActivitiesEngine engine) {
        mEngine = engine;
        mCr = context.getContentResolver();
        mCallLogObserver = new TimelineContentObserver(ActivitiesTable.TimelineNativeTypes.CallLog);
        mSmsObserver = new TimelineContentObserver(ActivitiesTable.TimelineNativeTypes.SmsLog);
        mMmsObserver = new TimelineContentObserver(ActivitiesTable.TimelineNativeTypes.MmsLog);
    }

    /**
     * Register ContentObservers to listen for Call, SMS, MMS events.
     */
    void startWatching() {
        mCr.registerContentObserver(Calls.CONTENT_URI, true, mCallLogObserver);
        mCr.registerContentObserver(FetchSmsLogEvents.SMS_CONTENT_URI, true, mSmsObserver);
        mCr.registerContentObserver(MmsDecoder.MMS_CONTENT_URI, true, mMmsObserver);
    }

    /**
     * Un-register ContentObservers.
     */
    void stopWatching() {
        mCr.unregisterContentObserver(mCallLogObserver);
        mCr.unregisterContentObserver(mSmsObserver);
        mCr.unregisterContentObserver(mMmsObserver);
    }
}