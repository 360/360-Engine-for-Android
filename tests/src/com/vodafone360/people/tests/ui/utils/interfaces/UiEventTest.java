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

public class UiEventTest extends TestCase {

    @SmallTest
    public void testGetUiEvent() throws Exception {

        // Normal use
        assertEquals(ServiceUiRequest.UI_REQUEST_COMPLETE, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UI_REQUEST_COMPLETE.ordinal()));
        assertEquals(ServiceUiRequest.DATABASE_CHANGED_EVENT, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.DATABASE_CHANGED_EVENT.ordinal()));
        assertEquals(ServiceUiRequest.SETTING_CHANGED_EVENT, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.SETTING_CHANGED_EVENT.ordinal()));
        assertEquals(ServiceUiRequest.UNSOLICITED_CHAT, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UNSOLICITED_CHAT.ordinal()));
        assertEquals(ServiceUiRequest.UNSOLICITED_PRESENCE, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UNSOLICITED_PRESENCE.ordinal()));
        assertEquals(ServiceUiRequest.UNSOLICITED_GO_TO_LANDING_PAGE, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UNSOLICITED_GO_TO_LANDING_PAGE.ordinal()));
        assertEquals(ServiceUiRequest.UNSOLICITED_CHAT_ERROR, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UNSOLICITED_CHAT_ERROR.ordinal()));
        assertEquals(ServiceUiRequest.UNSOLICITED_CHAT_ERROR_REFRESH, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UNSOLICITED_CHAT_ERROR_REFRESH.ordinal()));
        assertEquals(ServiceUiRequest.UNSOLICITED_PRESENCE_ERROR, ServiceUiRequest
                .getUiEvent(ServiceUiRequest.UNSOLICITED_PRESENCE_ERROR.ordinal()));
        assertEquals(ServiceUiRequest.UNKNOWN, ServiceUiRequest.getUiEvent(ServiceUiRequest.UNKNOWN
                .ordinal()));

        // Border conditions
        assertEquals(ServiceUiRequest.UNKNOWN, ServiceUiRequest.getUiEvent(-100));
        assertEquals(ServiceUiRequest.UNKNOWN, ServiceUiRequest.getUiEvent(-1));
        assertEquals(ServiceUiRequest.UNKNOWN, ServiceUiRequest.getUiEvent(ServiceUiRequest.UNKNOWN
                .ordinal() + 1));
        assertEquals(ServiceUiRequest.UNKNOWN, ServiceUiRequest.getUiEvent(100));
    }
}
