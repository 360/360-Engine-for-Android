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
package com.vodafone360.people.engine.groups;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.GroupsTable;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.datatypes.ItemList;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.service.io.api.GroupPrivacy;
import com.vodafone360.people.utils.LogUtils;

public class GroupsEngine extends BaseEngine {
    /**
     * Max number of groups to fetch from server in one request.
     */
    private static final int MAX_DOWN_PAGE_SIZE = 24;

    /**
     * Current page number being fetched.
     */
    private int mPageNo;

    /**
     * Total number of groups fetched from server.
     */
    private int mNoOfGroupsFetched;
    
    /**
     * 
     */
    private DatabaseHelper mDb;
    
    
    
    public GroupsEngine(Context context, IEngineEventCallback eventCallback, DatabaseHelper db) {
        super(eventCallback);
        mEngineId = EngineId.GROUPS_ENGINE;
        mDb = db;
    }
    
    @Override
    public long getNextRunTime() {
        // we only run if we have a request or a response in the queue
        if (isUiRequestOutstanding() || isCommsResponseOutstanding()) {
            return 0;
        }
        
        return -1;
    }

    @Override
    public void onCreate() {
        // nothing needed
    }

    @Override
    public void onDestroy() {
        // nothing needed
    }

    @Override
    protected void onRequestComplete() {
        // nothing needed
    }

    @Override
    protected void onTimeoutEvent() {
    }

    @Override
    protected void processCommsResponse(Response resp) {
        LogUtils.logD("DownloadGroups.processCommsResponse()");
        ServiceStatus status = BaseEngine.genericHandleResponseType("ItemList", resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            final List<GroupItem> tempGroupList = new ArrayList<GroupItem>();
            for (int i = 0; i < resp.mDataTypes.size(); i++) {
                ItemList itemList = (ItemList)resp.mDataTypes.get(i);
                if (itemList.mType != ItemList.Type.group_privacy) {
                    completeUiRequest(ServiceStatus.ERROR_UNEXPECTED_RESPONSE);
                    return;
                }
                for (int j = 0; j < itemList.mItemList.size(); j++) {
                    tempGroupList.add((GroupItem)itemList.mItemList.get(j));
                }
            }
            LogUtils.logI("DownloadGroups.processCommsResponse() - No of groups "
                    + tempGroupList.size());
            if (mPageNo == 0) {
                mDb.deleteAllGroups(); // clear old groups if we request the first groups page
            }
            status = GroupsTable.addGroupList(tempGroupList, mDb.getWritableDatabase());
            if (ServiceStatus.SUCCESS != status) {
                completeUiRequest(status);
                return;
            }
            mNoOfGroupsFetched += tempGroupList.size();
            if (tempGroupList.size() < MAX_DOWN_PAGE_SIZE) {
                completeUiRequest(ServiceStatus.SUCCESS);
                return;
            }
            mPageNo++;
            requestNextGroupsPage();
            return;
        }
        LogUtils
                .logE("DownloadGroups.processCommsResponse() - Error requesting Zyb groups, error = "
                        + status);
        completeUiRequest(status);
    }

    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        // for now we only serve the get groups call. later we might need to 
        // differentiate between multiple ServiceUiRequests
        switch (requestId) {
            case GET_GROUPS:
                requestFirstGroupsPage();
                break;
        }
    }

    @Override
    public void run() {
        if (isUiRequestOutstanding() && processUiQueue()) {
            return;
        }
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }
        if (processTimeout()) {
            return;
        }
    }

    /**
     * 
     * Adds a request to get groups from the backend that are associated with
     * the server contacts.
     * 
     */
    public void addUiGetGroupsRequest() {
        LogUtils.logI("GroupsEngine.addUiGetGroupsRequest()");
        addUiRequestToQueue(ServiceUiRequest.GET_GROUPS, null);
    }
    
    /**
     * Requests the first group page.
     */
    private void requestFirstGroupsPage() {
        mPageNo = 0;
        mNoOfGroupsFetched = 0;
        requestNextGroupsPage();
    }
    
    /**
     * Requests the next page of groups from the server.
     */
    private void requestNextGroupsPage() {
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS);
            return;
        }
        int reqId = GroupPrivacy.getGroups(this, mPageNo, MAX_DOWN_PAGE_SIZE);
        setReqId(reqId);
    }
}
