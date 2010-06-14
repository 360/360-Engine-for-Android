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

/**
 * The <code>ProcessorFactory</code> interface provides a way to create
 * processors.
 * 
 * @see <code>DefaultProcessorFactory</code> for a standard implementation used
 *      by the <code>ContactSyncEngine</code>.
 */
public interface ProcessorFactory {
    /**
     * The type id of the processor responsible of contacts downloading from the
     * server.
     */
    static final int DOWNLOAD_SERVER_CONTACTS = 1;

    /**
     * The type id of the processor responsible of thumbnails downloading from
     * the server.
     */
    static final int DOWNLOAD_SERVER_THUMBNAILS = 2;

    /**
     * The type id of the processor responsible of retrieving native contacts
     * from the device address book.
     */
    static final int FETCH_NATIVE_CONTACTS = 3;

    /**
     * The type id of the processor responsible of synchronizing Me profile
     * information between the device and the server.
     */
    static final int SYNC_ME_PROFILE = 4;

    /**
     * 
     */
    static final int SET_ME_PROFILE_STATUS = 5;

    /**
     * The type id of the processor responsible of updating the native contacts
     * of the device address book.
     */
    static final int UPDATE_NATIVE_CONTACTS = 6;

    /**
     * The type id of the processor responsible of uploading contacts to the
     * server.
     */
    static final int UPLOAD_SERVER_CONTACTS = 7;

    /**
     * The type id of the processor responsible of uploading thumbnails to the
     * server.
     */
    static final int UPLOAD_SERVER_THUMBNAILS = 8;

    /**
     * Creates a processor from the provided type.
     * 
     * @param type the type of the processor to create
     * @param callback the observer of processor state
     * @param dbHelper the database helper to use
     * @param context the context to use
     * @param cr the content resolver to use
     * @return a <code>BaseSyncProcessor</code> implementation of the requested
     *         type
     * @throws IllegalArgumentException if the type of processor is not known
     */
    BaseSyncProcessor create(int type, IContactSyncCallback callback, DatabaseHelper dbHelper,
            Context context, ContentResolver cr);
}
