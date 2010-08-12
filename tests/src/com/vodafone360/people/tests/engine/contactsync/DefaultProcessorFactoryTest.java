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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.Suppress;

import com.vodafone360.people.engine.contactsync.BaseSyncProcessor;
import com.vodafone360.people.engine.contactsync.DefaultProcessorFactory;
import com.vodafone360.people.engine.contactsync.DownloadServerContacts;
import com.vodafone360.people.engine.contactsync.FetchNativeContacts;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.engine.contactsync.ProcessorFactory;
import com.vodafone360.people.engine.contactsync.UpdateNativeContacts;
import com.vodafone360.people.engine.contactsync.UploadServerContacts;

/**
 * The DefaultProcessorFactoryTest handles unit testing of
 * DefaultProcessorFactory class.
 */
public class DefaultProcessorFactoryTest extends AndroidTestCase {

    /**
     * Tests the type of the created processor depending on the requested type.
     */
    
    public void testProcessorTypeCreation() {

        DefaultProcessorFactory factory = new DefaultProcessorFactory();
        BaseSyncProcessor processor;

        processor = factory.create(ProcessorFactory.DOWNLOAD_SERVER_CONTACTS, null, null, null,
                null);
        assertTrue(processor instanceof DownloadServerContacts);
        
        // Uncommenting this code will crash the test case and EngineManger instance
        // not available for the unit test.
       /* processor = factory.create(ProcessorFactory.FETCH_NATIVE_CONTACTS, null, null, null, null);
        assertTrue(processor instanceof FetchNativeContactsDummy);*/
        
        NativeContactsApi.createInstance(getContext());
        processor = factory.create(ProcessorFactory.UPDATE_NATIVE_CONTACTS, null, null, null, null);
        assertTrue(processor instanceof UpdateNativeContacts);

        processor = factory.create(ProcessorFactory.UPLOAD_SERVER_CONTACTS, null, null, null, null);
        assertTrue(processor instanceof UploadServerContacts);

    }

    /**
     * Checks that the unexpected type of processor creation is handled.
     */
    public void testUnexpectedProcessorType() {

        DefaultProcessorFactory factory = new DefaultProcessorFactory();
        Exception exception = null;

        try {
            // with type=-12, an IllegalArgumentException shall be thrown
            factory.create(-12, null, null, null, null);
        } catch (IllegalArgumentException e) {
            exception = e;
        }

        // check the exception type
        assertTrue(exception instanceof IllegalArgumentException);
    }

}
