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

package com.vodafone360.people.engine.contactsync;

import android.content.ContentResolver;
import android.content.Context;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.contactsync.SyncStatus.Task;
import com.vodafone360.people.engine.contactsync.SyncStatus.TaskStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.utils.LogUtils;

/**
 * Processor handling retrieval of contacts from the native database. These
 * contacts will be synced to the People server if necessary.
 */
public class FetchNativeContacts extends BaseSyncProcessor {
        
    /**
     * The NativeImporter instance that will take care of fetching contacts
     * from the native address book.
     */
    private NativeImporter mNativeImporter;

    /**
     * Processor constructor.
     * 
     * @param callback Provides access to contact sync engine callback functions
     * @param db Provides access to People database
     * @param context Context needed for accessing native database
     * @param cr ContentResolver used throughout people service
     */
    protected FetchNativeContacts(IContactSyncCallback callback, DatabaseHelper db,
            Context context, ContentResolver cr) {
        super(callback, db);
        
        final boolean firstTimeSync = !EngineManager.getInstance().getContactSyncEngine().isFirstTimeSyncComplete();
        
        mNativeImporter = new NativeImporter(new PeopleContactsApi(db), NativeContactsApi.getInstance(), firstTimeSync);
    }

    @Override
    protected void doStart() {
        LogUtils.logD("FetchNativeContacts.doStart()");
        
        onTimeoutEvent();
    }

    @Override
    protected void doCancel() {
        
    }

    @Override
    public void processCommsResponse(DecodedResponse resp) {
        
        // not needed
    }
    
    @Override
    public void onTimeoutEvent() {
        LogUtils.logD("FetchNativeContacts.onTimeoutEvent()");
        
        // call the tick method of the NativeImporter
        final boolean isDone = mNativeImporter.tick();
        // get the index of the last processed id
        final int position = mNativeImporter.getPosition();
        // get the total count of ids to process
        final int total = mNativeImporter.getCount(); 
        // get the percentage out of position and total count
        final int percentage = (total != 0) ? ((position * 100) / total) : 100;
        
        LogUtils.logD("FetchNativeContacts.onTimeoutEvent() - pos="+position+", total="+total+", percentage="+percentage);
        
        // check the NativeImporter progress
        if (!isDone) {
            
            // report the current progress
            // pass an empty name as currently the last processed contact name by the NativeImporter can't be known
            setProgress("", percentage, position, total);
            // yield some time to the other engines and request to be called back immediately
            setTimeout(0);
        } else {
            
            final ServiceStatus status = mNativeImporter.getResult() == NativeImporter.RESULT_OK ? ServiceStatus.SUCCESS : ServiceStatus.ERROR_UNKNOWN; 
            LogUtils.logD("FetchNativeContacts.onTimeoutEvent() - complete("+status+")");
            setProgress("", percentage, position, total);
            complete(status);
        }
    }
    
    /**
     * Helper function to report progress to the engine.
     * 
     * @param contactName the name of the last contact
     * @param progress the progression as a percentage
     * @param processed the number of processed contacts
     * @param totla the total count of contacts to process
     */
    public void setProgress(String contactName, int progress, int processed, int total) {
        
        LogUtils.logD("FetchNativeContacts.setProgress() contactName["
                + contactName + "] progress[" + progress + "] processed["
                + processed + "] total[" + total + "]");
        
        if (total > 0) {
            setSyncStatus(new SyncStatus(progress, contactName,
                    Task.FETCH_NATIVE_CONTACTS,
                    TaskStatus.RECEIVED_CONTACTS, processed, total));
            
        } else {
            setSyncStatus(new SyncStatus(progress, contactName, Task.FETCH_NATIVE_CONTACTS));
        }
    }
}