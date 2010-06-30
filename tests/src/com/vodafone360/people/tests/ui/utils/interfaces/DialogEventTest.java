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

package com.vodafone360.people.tests.ui.utils.interfaces;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.ui.utils.interfaces.DialogEvent;

public class DialogEventTest extends TestCase {

    @SmallTest
    public void testGetUiEvent() throws Exception {

        // Normal use
        assertEquals(DialogEvent.DIALOG_UPGRADE, DialogEvent
                .getDialogEvent(DialogEvent.DIALOG_UPGRADE.ordinal()));
        assertEquals(DialogEvent.UNKNOWN, DialogEvent.getDialogEvent(DialogEvent.UNKNOWN.ordinal()));

        // Border conditions
        assertEquals(DialogEvent.UNKNOWN, DialogEvent.getDialogEvent(-100));
        assertEquals(DialogEvent.UNKNOWN, DialogEvent.getDialogEvent(-1));
        assertEquals(DialogEvent.UNKNOWN, DialogEvent.getDialogEvent(ServiceUiRequest.UNKNOWN
                .ordinal() + 1));
        assertEquals(DialogEvent.UNKNOWN, DialogEvent.getDialogEvent(100));
    }
}
