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

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.utils.LogUtils;



/**
 * Processor handling the update of contacts in the Native database based on
 * changes received during sync with People server.
 */
public class UpdateNativeContacts extends BaseSyncProcessor {
 
    /**
     * The NativeExporter instance that will take care of updating contacts
     * on the native address book.
     */
    private final NativeExporter mNativeExporter;

    /**
     * Processor constructor.
     * 
     * @param callback Provides access to contact sync engine processor
     *            functions.
     * @param db Database for reading contacts for sync and fetching change log
     * @param cr {@link ContentResolver} for updating the native database.
     */
    public UpdateNativeContacts(IContactSyncCallback callback, DatabaseHelper db, ContentResolver cr) {
        super(callback, db);

        mNativeExporter = new NativeExporter(new PeopleContactsApi(db), NativeContactsApi.getInstance());
    }

    /**
     * @see BaseSyncProcessor#doCancel()
     */
    @Override
    protected void doCancel() {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see BaseSyncProcessor#doStart()
     */
    @Override
    protected void doStart() {
        
        LogUtils.logD("UpdateNativeContacts.doStart()");
        
        onTimeoutEvent();
    }

    /**
     * @see BaseSyncProcessor#processCommsResponse(DecodedResponse)
     */
    @Override
    public void processCommsResponse(DecodedResponse resp) {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * @see BaseSyncProcessor#onTimeoutEvent()
     */
    @Override
    public void onTimeoutEvent() {
        LogUtils.logD("UpdateNativeContacts.onTimeoutEvent()");
        
        // call the tick method of the NativeExporter
        final boolean isDone = mNativeExporter.tick();
        // get the index of the last processed id
        final int position = mNativeExporter.getPosition();
        // get the total count of ids to process
        final int total = mNativeExporter.getCount(); 
        // get the percentage out of position and total count
        final int percentage = (total != 0) ? ((position * 100) / total) : 100;
        
        LogUtils.logD("UpdateNativeContacts.onTimeoutEvent() - pos="+position+", total="+total+", percentage="+percentage);
        
        // check the NativeExporter progress
        if (!isDone) {
            
            // yield some time to the other engines and request to be called back immediately
            setTimeout(0);
        } else {
            // FIXME: More useful error reporting beyond just ERROR_UNKNOWN
            final ServiceStatus status = mNativeExporter.getResult() == NativeImporter.RESULT_OK ? ServiceStatus.SUCCESS : ServiceStatus.ERROR_UNKNOWN; 
            LogUtils.logD("FetchNativeContacts.onTimeoutEvent() - complete("+status+")");
            complete(status);
        }
    }
}
