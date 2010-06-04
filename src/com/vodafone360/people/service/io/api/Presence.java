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

package com.vodafone360.people.service.io.api;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.vodafone360.people.Settings;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.utils.LogUtils;

/**
 * Implementation of People server Presence APIs.
 */
public class Presence {

    private final static String EMPTY = ""; // TODO: What is the name of this
                                            // function?

    /**
     * Retrieve current presence list
     * 
     * @param engineId ID for Presence engine.
     * @param recipientUserIdList List of user IDs.
     */
    public static void getPresenceList(EngineId engineId,
            Map<String, List<String>> recipientUserIdList) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Presence.getPresenceList() No session, return");
            return;
        }
        Request request = new Request(EMPTY, Request.Type.PRESENCE_LIST, engineId, false,
                Settings.API_REQUESTS_TIMEOUT_PRESENCE_LIST);
        if (recipientUserIdList != null) {
            // If not specified, then all presence information will be returned
            request.addData("tos", ApiUtils.createHashTable(recipientUserIdList));
        }

        QueueManager.getInstance().addRequest(request);
        QueueManager.getInstance().fireQueueStateChanged();
    }

    /**
     * API to set my availability
     * 
     * @param engineId ID of presence engine.
     * @param status Hash table containing status information.
     */
    public static void setMyAvailability(Hashtable<String, String> status) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Presence.setAvailability() No session, so return");
            return;
        }
        Request request = new Request(EMPTY, Request.Type.AVAILABILITY, EngineId.UNDEFINED, true,
                Settings.API_REQUESTS_TIMEOUT_PRESENCE_SET_AVAILABILITY);
        request.addData("availability", status);

        QueueManager.getInstance().addRequest(request);
        QueueManager.getInstance().fireQueueStateChanged();
    }
}
