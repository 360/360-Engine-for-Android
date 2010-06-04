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

package com.vodafone360.people.tests.engine.contactsync;

import java.lang.reflect.Field;

import android.util.Log;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.contactsync.IContactSyncCallback;
import com.vodafone360.people.engine.contactsync.SyncStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.ResponseQueue.Response;

public class HelperClasses {

    private static final String LOG_TAG = "HelperClasses";

    /**
     * Base class used for engine event callbacks.
     */
    public static class EngineCallbackBase implements IEngineEventCallback {

        @Override
        public void kickWorkerThread() {
        }

        @Override
        public void onUiEvent(ServiceUiRequest event, int request, int status, Object data) {
        }

        @Override
        public UiAgent getUiAgent() {
            return null;
        }

        @Override
        public ApplicationCache getApplicationCache() {
            return null;
        }
    }

    /**
     * Dummy BaseEngine implementation.
     */
    public static class DummyBaseEngine extends BaseEngine {

        public DummyBaseEngine(IEngineEventCallback eventCallback) {
            super(eventCallback);
        }

        @Override
        public long getNextRunTime() {
            return 0;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDestroy() {

        }

        @Override
        protected void onRequestComplete() {

        }

        @Override
        protected void onTimeoutEvent() {

        }

        @Override
        protected void processCommsResponse(Response resp) {

        }

        @Override
        protected void processUiRequest(ServiceUiRequest requestId, Object data) {

        }

        @Override
        public void run() {

        }

    }

    /**
     * A simple IContactSyncCallback implementation.
     */
    public static class DummyContactSyncCallback implements IContactSyncCallback {

        IEngineEventCallback mEngineEventCallback = null;

        ServiceStatus mServiceStatus = null;

        public DummyContactSyncCallback(IEngineEventCallback engineEventCallback) {
            mEngineEventCallback = engineEventCallback;
        }

        @Override
        public BaseEngine getEngine() {
            return new HelperClasses.DummyBaseEngine(mEngineEventCallback);
        }

        @Override
        public void onDatabaseChanged() {

        }

        @Override
        public void onProcessorComplete(ServiceStatus status, String failureList, Object data) {

            Log
                    .i(LOG_TAG, "onProcessorComplete(" + status + ", " + failureList + ", " + data
                            + ")");

            mServiceStatus = status;
        }

        @Override
        public void setActiveRequestId(int reqId) {

        }

        @Override
        public void setSyncStatus(SyncStatus syncStatus) {
            
        }

        @Override
        public void setTimeout(long timeout) {

        }
    }

    /**
     * Gets the database name via reflection feature.
     * 
     * @return the database name
     */
    public static String getDatabaseName() {

        try {
            Field dbName = DatabaseHelper.class.getDeclaredField("DATABASE_NAME");
            dbName.setAccessible(true);
            return (String)dbName.get(null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "getDatabaseName(), error retrieving the database name... => " + e);
        }

        return null;
    }
}
