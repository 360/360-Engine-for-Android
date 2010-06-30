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

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.vodafone360.people.Settings;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.utils.LogUtils;

/**
 * Implementation of Activities APIs used to send and retrieve Timeline and
 * Status events to/from the server.
 */
public class Activities {
    // private final static String FUNCTION_SET_ACTIVITIES =
    // "activities/setactivities";
    private final static String FUNCTION_GET_ACTIVITIES = "activities/getactivities";

    /**
     * Implementation of People getactvities API. The required parameters are;
     * [auth], List<Long> activityidlist [opt], Map<String, List<String>>
     * filterlist [opt]
     * 
     * @param engine Handle to ActivitiesEngine which handles requests using
     *            this API.
     * @param activityidlist List of Activity IDs to retrieve
     * @param filterlist List of filters to filter Activities request against.
     * @return Request ID generated for this request.
     */
    public static int getActivities(BaseEngine engine, List<Long> activityidlist,
            Map<String, List<String>> filterlist) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Activities.getActivities() Invalid session");
            return -1;
        }

        Request request = new Request(FUNCTION_GET_ACTIVITIES, Request.Type.COMMON, engine
                .engineId(), false, Settings.API_REQUESTS_TIMEOUT_ACTIVITIES);
        if (activityidlist != null) {
            request.addData("activityidlist", new Vector<Object>(activityidlist));
        }
        if (filterlist != null) {
            request.addData("filterlist", ApiUtils.createHashTable(filterlist));
        }

        int requestId = QueueManager.getInstance().addRequest(request);
        QueueManager.getInstance().fireQueueStateChanged();
        return requestId;
    }

}
