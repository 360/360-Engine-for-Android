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
import java.util.Vector;

import com.vodafone360.people.Settings;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.utils.LogUtils;

/**
 * Implementation of Now+ Group privacy APIs
 */
public class GroupPrivacy {

    private final static String FUNCTION_ADD_CONTACT_GROUP_RELATIONS = "groupprivacy/addcontactgrouprelations";

    private final static String FUNCTION_DELETE_CONTACT_GROUP_RELATIONS_EXT = "groupprivacy/deletecontactgrouprelationsext";

    private final static String FUNCTION_GET_GROUPS = "groupprivacy/getgroups";

    /**
     * Implementation of groupprivacy/addcontactgrouprelations API. Parameters
     * are; [auth], List<Long> contactidlist, List<Group> grouplist
     * 
     * @param engine handle to ContactSyncEngine
     * @param contactidlist List of contacts ids associated with this request.
     * @param grouplist List of groups associated with this request.
     * @return request id generated for this request
     */
    public static int addContactGroupRelations(BaseEngine engine, List<Long> contactidlist,
            List<GroupItem> grouplist) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("GroupPrivacy.addContactGroupRelations() Invalid session, return -1");
            return -1;
        }
        if (contactidlist == null) {
            LogUtils.logE("GroupPrivacy.addContactGroupRelations() contactidlist cannot be NULL");
            return -1;
        }
        if (grouplist == null) {
            LogUtils.logE("GroupPrivacy.addContactGroupRelations() grouplist cannot be NULL");
            return -1;
        }

        Request request = new Request(FUNCTION_ADD_CONTACT_GROUP_RELATIONS,
                Request.Type.CONTACT_GROUP_RELATIONS, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_GROUP_PRIVACY);
        request.addData("contactidlist", new Vector<Object>(contactidlist));
        request.addData("grouplist", ApiUtils.createVectorOfGroup(grouplist));

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of groupprivacy/deletecontactgrouprelationsext API.
     * Parameters are; [auth], Long groupid, List<Long> contactidlist
     * 
     * @param engine handle to IdentitiesEngine
     * @param groupid Group ID.
     * @param contactidlist List of contact IDs to delete group relations.
     * @return request id generated for this request
     */
    public static int deleteContactGroupRelationsExt(BaseEngine engine, Long groupid,
            List<Long> contactidlist) {
        if (LoginEngine.getSession() == null) {
            LogUtils
                    .logE("GroupPrivacy.deleteContactGroupRelationsExt() Invalid session, return -1");
            return -1;
        }
        if (groupid == null) {
            LogUtils.logE("GroupPrivacy.deleteContactGroupRelationsExt() groupid cannot be NULL");
            return -1;
        }
        if (contactidlist == null) {
            LogUtils
                    .logE("GroupPrivacy.deleteContactGroupRelationsExt() contactidlist cannot be NULL");
            return -1;
        }
        if (contactidlist.size() == 0) {
            LogUtils
                    .logE("GroupPrivacy.deleteContactGroupRelationsExt() contactidlist.size cannot be 0");
            return -1;
        }

        Request request = new Request(FUNCTION_DELETE_CONTACT_GROUP_RELATIONS_EXT,
                Request.Type.STATUS, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_GROUP_PRIVACY);
        request.addData("groupid", groupid);
        request.addData("contactidlist", new Vector<Object>(contactidlist));

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of groupprivacy/getgroups API. Parameters are; [auth],
     * Integer pageindex [opt], Integer pagesize [opt]
     * 
     * @param engine handle to IdentitiesEngine
     * @param pageindex Page index.
     * @param pagesize PAge size.
     * @return request id generated for this request
     */
    public static int getGroups(BaseEngine engine, Integer pageindex, Integer pagesize) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("GroupPrivacy.GetGroups() Invalid session, return -1");
            return -1;
        }
        Request request = new Request(FUNCTION_GET_GROUPS, Request.Type.GROUP_LIST, engine
                .engineId(), false, Settings.API_REQUESTS_TIMEOUT_GROUP_PRIVACY);
        if (pageindex != null) {
            request.addData("pageindex", pageindex);
        }
        if (pagesize != null) {
            request.addData("pagesize", pagesize);
        }

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }
}
