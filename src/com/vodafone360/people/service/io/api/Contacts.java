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
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.utils.LogUtils;

/**
 * Implementation of People server APIs associated with handling of Contacts
 */
public class Contacts {

    private final static String FUNCTION_BULK_UPDATE_CONTACTS = "contacts/bulkupdatecontacts";

    private final static String FUNCTION_DELETE_CONTACT_DETAILS = "contacts/deletecontactdetails";

    private final static String FUNCTION_DELETE_CONTACTS = "contacts/deletecontacts";

    private final static String FUNCTION_GET_CONTACT_CHANGES = "contacts/getcontactschanges";

    private final static String FUNCTION_GET_ME = "contacts/getme";

    private final static String FUNCTION_GET_MY_CHANGES = "contacts/getmychanges";

    private final static String FUNCTION_SET_ME = "contacts/setme";

    /**
     * Implementation of contacts/deletecontactdetails API. Parameters are;
     * [auth], Long contactid, List<ContactDetail> detaillist
     * 
     * @param engine Handle to ContactSync engine
     * @param contactid
     * @param detaillist
     * @return request id generated for this request.
     */
    public static int deleteContactDetails(BaseEngine engine, Long contactid,
            List<ContactDetail> detaillist) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.deleteContactDetails() Invalid session, return -1");
            return -1;
        }
        if (contactid == null) {
            LogUtils.logE("Contacts.deleteContactDetails() contactidlist cannot be NULL");
            return -1;
        }
        if (detaillist == null) {
            LogUtils.logE("Contacts.deleteContactDetails() detaillist cannot be NULL");
            return -1;
        }

        Request request = new Request(FUNCTION_DELETE_CONTACT_DETAILS,
                Request.Type.CONTACT_DETAIL_DELETE, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_CONTACTS);
        request.addData("contactid", contactid);
        request.addData("detaillist", ApiUtils.createVectorOfContactDetail(detaillist));

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of contacts/deletecontacts API. Parameters are; [auth],
     * List<Long> contactidlist
     * 
     * @param engine Handle to ContactSync engine
     * @param contactidlist List of contact ids to be deleted.
     * @return request id generated for this request.
     */
    public static int deleteContacts(BaseEngine engine, List<Long> contactidlist) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.deleteContacts() Invalid session, return -1");
            return -1;
        }
        if (contactidlist == null) {
            LogUtils.logE("Contacts.deleteContacts() contactidlist cannot be NULL");
            return -1;
        }

        Request request = new Request(FUNCTION_DELETE_CONTACTS, Request.Type.CONTACT_DELETE, engine
                .engineId(), false, Settings.API_REQUESTS_TIMEOUT_CONTACTS);
        request.addData("contactidlist", new Vector<Object>(contactidlist));

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of contacts/bulkupdatecontacts API. Parameters are;
     * [auth], List<Contact> contactlist
     * 
     * @param engine Handle to ContactSync engine
     * @param contactlist List containing the contacts to be updated on server.
     * @return request id generated for this request.
     */
    public static int bulkUpdateContacts(BaseEngine engine, List<Contact> contactlist) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.bulkUpdateContacts() Invalid session, return -1");
            return -1;
        }
        if (contactlist == null) {
            LogUtils.logE("Contacts.bulkUpdateContacts() contactidlist cannot be NULL");
            return -1;
        }
        
        Request request = new Request(FUNCTION_BULK_UPDATE_CONTACTS,
                Request.Type.CONTACT_CHANGES_OR_UPDATES, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_CONTACTS);
        request.addData("contactlist", ApiUtils.createVectorOfContact(contactlist));

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of contacts/getcontactschanges API. Parameters are;
     * [auth], Integer pagenumber, Integer maxpagesize, Long fromrevision, Long
     * torevision
     * 
     * @param engine Handle to ContactSync engine
     * @param pagenumber Page number to request contact changes for.
     * @param maxpagesize Maximum number of contacts retrieved per page.
     * @param fromrevision Starting revision number.
     * @param torevision Final revision number.
     * @param batchRequest If true, this API call will not send the request
     *            until the connection thread is kicked. This allows batching of
     *            requests.
     * @return request id generated for this request.
     */
    public static int getContactsChanges(BaseEngine engine, Integer pagenumber,
            Integer maxpagesize, Long fromrevision, Long torevision, boolean batchRequest) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.getContactsChanges() Invalid session, return -1");
            return -1;
        }
        if (pagenumber == null) {
            LogUtils.logE("Contacts.getContactsChanges() pagenumber cannot be NULL");
            return -1;
        }
        if (maxpagesize == null) {
            LogUtils.logE("Contacts.getContactsChanges() maxpagesize cannot be NULL");
            return -1;
        }
        if (fromrevision == null) {
            LogUtils.logE("Contacts.getContactsChanges() fromrevision cannot be NULL");
            return -1;
        }
        if (torevision == null) {
            LogUtils.logE("Contacts.getContactsChanges() torevision cannot be NULL");
            return -1;
        }

        Request request = new Request(FUNCTION_GET_CONTACT_CHANGES,
                Request.Type.CONTACT_CHANGES_OR_UPDATES, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_CONTACTS);
        request.addData("pagenumber", pagenumber);
        request.addData("maxpagesize", maxpagesize);
        request.addData("fromrevision", fromrevision);
        request.addData("torevision", torevision);
        // XXX check if this has any implications
        // request.setKickConnectionThread(!batchRequest);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of contacts/getme API. Parameters are; [auth]
     * 
     * @param engine Handle to ContactSync engine
     * @return Request ID generated for this request.
     */
    public static int getMe(BaseEngine engine) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.GetMe() Invalid session, return -1");
            return -1;
        }

        Request request = new Request(FUNCTION_GET_ME, Request.Type.COMMON, engine.engineId(),
                false, Settings.API_REQUESTS_TIMEOUT_CONTACTS);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of contacts/getmychanges. Parameters are; [auth], Long
     * fromrevision
     * 
     * @param engine Handle to ContactSync engine
     * @param fromrevision Start revision.
     * @return request id generated for this request.
     */
    public static int getMyChanges(BaseEngine engine, Long fromrevision) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.getMyChanges() Invalid session, return -1");
            return -1;
        }

        Request request = new Request(FUNCTION_GET_MY_CHANGES,
                Request.Type.CONTACT_CHANGES_OR_UPDATES, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_CONTACTS);
        request.addData("fromrevision", fromrevision.toString());

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of contacts/setme API. Parameters are; [auth],
     * List<ContactDetail> detaillist, String aboutme [opt]
     * 
     * @param engine Handle to ContactSync engine
     * @param detaillist List of ContactDetails for the Me profile.
     * @param aboutme AboutMe string.
     * @param gender - gender.
     * @return request id generated for this request.
     */
    public static int setMe(BaseEngine engine, List<ContactDetail> detaillist, String aboutme,
            Integer gender) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Contacts.SetMe() Invalid session, return -1");
            return -1;
        }

        Request request = new Request(FUNCTION_SET_ME, Request.Type.CONTACT_CHANGES_OR_UPDATES,
                engine.engineId(), false, Settings.API_REQUESTS_TIMEOUT_CONTACTS);
        if (aboutme != null) {
            request.addData("aboutme", aboutme);
        }
        if (detaillist != null) {
            request.addData("detaillist", ApiUtils.createVectorOfContactDetail(detaillist));
        }
        if (gender != null) {
            request.addData("gender", gender);
        }

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }
}
