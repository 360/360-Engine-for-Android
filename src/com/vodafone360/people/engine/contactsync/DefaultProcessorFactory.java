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
import com.vodafone360.people.utils.LogUtils;

/**
 * The <code>DefaultProcessorFactory</code> implements the
 * <code>ProcessorFactory</code> interface and is the default factory used by
 * the <code>ContactSyncEngine</code>.
 * 
 * @see <code>ProcessorFactory</code>
 * @see <code>ContactSyncEngine</code>
 */
public class DefaultProcessorFactory implements ProcessorFactory {

    /**
     * Creates a suitable processor for a required contact sync activity.
     * 
     * @see ProcessorFactory#create(int, IContactSyncCallback, DatabaseHelper,
     *      Context, ContentResolver)
     */
    @Override
    public BaseSyncProcessor create(int type, IContactSyncCallback callback,
            DatabaseHelper dbHelper, Context context, ContentResolver cr) {

        LogUtils.logD("DefaultProcessorFactory().create(" + type + ", " + callback + ", "
                + dbHelper + ", " + context + ", " + cr + ")");
        switch (type) {
            case DOWNLOAD_SERVER_CONTACTS:
                return new DownloadServerContacts(callback, dbHelper);
            case FETCH_NATIVE_CONTACTS:
                return new FetchNativeContacts(callback, dbHelper, context, cr);
            case UPDATE_NATIVE_CONTACTS:
                return new UpdateNativeContacts(callback, dbHelper, cr);
            case UPLOAD_SERVER_CONTACTS:
                return new UploadServerContacts(callback, dbHelper);
            default:
                final String errorMsg = "DefaultProcessorFactory().create(), error: unexpected type of processor! type="
                        + type;
                LogUtils.logE(errorMsg);
                throw new IllegalArgumentException(errorMsg);
        }
    }
}
