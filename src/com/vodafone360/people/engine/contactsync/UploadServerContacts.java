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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.database.Cursor;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.DatabaseHelper.ServerIdInfo;
import com.vodafone360.people.database.tables.ContactChangeLogTable;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.ContactChangeLogTable.ContactChangeInfo;
import com.vodafone360.people.database.tables.ContactChangeLogTable.ContactChangeType;
import com.vodafone360.people.database.tables.ContactsTable.ContactIdInfo;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactDetailDeletion;
import com.vodafone360.people.datatypes.ContactListResponse;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.datatypes.ItemList;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.contactsync.SyncStatus.Task;
import com.vodafone360.people.engine.contactsync.SyncStatus.TaskStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.api.Contacts;
import com.vodafone360.people.service.io.api.GroupPrivacy;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.VersionUtils;

/**
 * Processor handling upload of contacts to the People server.
 */
public class UploadServerContacts extends BaseSyncProcessor {

    /**
     * Internal states supported by the processor.
     */
    protected enum InternalState {
        /** Internal state when processing new contacts. **/
        PROCESSING_NEW_CONTACTS,
        /** Internal state when processing modified details. **/
        PROCESSING_MODIFIED_DETAILS,
        /** Internal state when processing deleted contacts. **/
        PROCESSING_DELETED_CONTACTS,
        /** Internal state when processing deleted details. **/
        PROCESSING_DELETED_DETAILS,
        /** Internal state when processing group additions. **/
        PROCESSING_GROUP_ADDITIONS,
        /** Internal state when processing group deletions. **/
        PROCESSING_GROUP_DELETIONS
    }

    /**
     * Maximum progress percentage value.
     */
    private static final int MAX_PROGESS = 100;

    /**
     * Used for converting between nanoseconds and milliseconds.
     */
    private static final long NANOSECONDS_IN_MS = 1000000L;

    /**
     * Maximum number of contacts to send to the server in a request.
     */
    protected static final int MAX_UP_PAGE_SIZE = 25;

    /**
     * No of items which were sent in the current batch, used for updating the
     * progress bar once a particular call is complete.
     */
    private long mNoOfItemsSent;

    /**
     * Total number of items which need to be sync'ed to the server (includes
     * all types of updates). Used for updating the progress.
     */
    private int mTotalNoOfItems;

    /**
     * Total number of contacts sync'ed so far.
     */
    private int mItemsDone;

    /**
     * Contains the next page of changes from the server change log.
     */
    private final List<ContactChangeInfo> mContactChangeInfoList = new ArrayList<ContactChangeInfo>();

    /**
     * Contains the next page of contacts being sent to the server which have
     * either been added or changed.
     */
    private final List<Contact> mContactChangeList = new ArrayList<Contact>();

    /**
     * Current internal state of the processor.
     */
    private InternalState mInternalState;

    /**
     * Used for debug to monitor the database read/write time during a sync.
     */
    private long mDbSyncTime = 0;

    /**
     * Cursor used for fetching new/modified contacts from the NowPlus database.
     */
    private Cursor mContactsCursor;

    /**
     * List of server IDs which is sent to the server by the {@code
     * DeleteContacts} API.
     */
    private final List<Long> mContactIdList = new ArrayList<Long>();

    /**
     * Current list of groups being added to the contacts listed in the
     * {@link #mContactIdList}. Will only contain a single group (list is needed
     * for the
     * {@link GroupPrivacy#addContactGroupRelations(BaseEngine, List, List)}
     * API).
     */
    private final List<GroupItem> mGroupList = new ArrayList<GroupItem>();

    /**
     * Contains the main contact during the contact detail deletion process.
     */
    private Contact mDeleteDetailContact = null;

    /**
     * Contains the server ID of the group being associated with a contact.
     */
    private Long mActiveGroupId = null;

    /**
     * Processor constructor.
     * 
     * @param callback Provides access to contact sync engine callback
     *            functions.
     * @param db NowPlus Database needed for reading contact and group
     *            information
     */
    public UploadServerContacts(final IContactSyncCallback callback, final DatabaseHelper db) {
        super(callback, db);
    }

    /**
     * Called by framework to start the processor running. First sends all the
     * new contacts to the server.
     */
    @Override
    protected final void doStart() {
        setSyncStatus(new SyncStatus(0, "", Task.UPDATE_SERVER_CONTACTS));

        long startTime = System.nanoTime();
        mTotalNoOfItems = ContactChangeLogTable.fetchNoOfContactDetailChanges(null, mDb
                .getReadableDatabase())
                + ContactDetailsTable.syncServerFetchNoOfChanges(mDb.getReadableDatabase());
        mDbSyncTime += (System.nanoTime() - startTime);

        mItemsDone = 0;
        startProcessNewContacts();
    }

    /**
     * Sends the first page of new contacts to the server.
     */
    private void startProcessNewContacts() {
        mInternalState = InternalState.PROCESSING_NEW_CONTACTS;

        long startTime = System.nanoTime();
        mContactsCursor = ContactDetailsTable.syncServerFetchContactChanges(mDb
                .getReadableDatabase(), true);
        mDbSyncTime += (System.nanoTime() - startTime);

        if (mContactsCursor == null) {
            complete(ServiceStatus.ERROR_DATABASE_CORRUPT);
        } else {
            sendNextContactAdditionsPage();
        }
    }

    /**
     * Sends the first page of contact modifications (includes new and modified
     * details) to the server.
     */
    private void startProcessModifiedDetails() {
        mInternalState = InternalState.PROCESSING_MODIFIED_DETAILS;

        /** Cleanup unused objects **/
        if (mContactsCursor != null) {
            mContactsCursor.close();
            mContactsCursor = null;
        }

        long startTime = System.nanoTime();
        mContactsCursor = ContactDetailsTable.syncServerFetchContactChanges(mDb
                .getReadableDatabase(), false);
        mDbSyncTime += (System.nanoTime() - startTime);

        sendNextDetailChangesPage();
    }

    /**
     * Sends the first page of deleted contacts to the server.
     */
    private void startProcessDeletedContacts() {
        mInternalState = InternalState.PROCESSING_DELETED_CONTACTS;

        /** Cleanup unused objects **/
        mContactChangeList.clear();
        if (mContactsCursor != null) {
            mContactsCursor.close();
            mContactsCursor = null;
        }

        sendNextDeleteContactsPage();
    }

    /**
     * Sends the deleted details of the first contact to the server.
     */
    private void startProcessDeletedDetails() {
        mInternalState = InternalState.PROCESSING_DELETED_DETAILS;
        sendNextDeleteDetailsPage();
    }

    /**
     * Sends the first contact/group relation addition request to the server.
     */
    private void startProcessGroupAdditions() {
        mInternalState = InternalState.PROCESSING_GROUP_ADDITIONS;
        sendNextAddGroupRelationsPage();
    }

    /**
     * Sends the first contact/group relation deletion request to the server.
     */
    private void startProcessGroupDeletions() {
        mInternalState = InternalState.PROCESSING_GROUP_DELETIONS;
        sendNextDelGroupRelationsPage();
    }

    /**
     * Sends the next page of new contacts to the server.
     */
    private void sendNextContactAdditionsPage() {
        long startTime = System.nanoTime();
        ContactDetailsTable.syncServerGetNextNewContactDetails(mContactsCursor, mContactChangeList,
                MAX_UP_PAGE_SIZE);
        mDbSyncTime += (System.nanoTime() - startTime);

        if (mContactChangeList.size() == 0) {
            moveToNextState();
            return;
        }
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        /** Debug output. **/
        if (Settings.ENABLED_CONTACTS_SYNC_TRACE) {
            LogUtils.logI("UploadServerContacts." + "sendNextContactAdditionsPage() New Contacts:");
            for (Contact contact : mContactChangeList) {
                for (ContactDetail detail : contact.details) {
                    LogUtils.logI("UploadServerContacts."
                            + "sendNextContactAdditionsPage() Contact: " + contact.localContactID
                            + ", Detail: " + detail.key + ", " + detail.keyType + " = "
                            + detail.value);
                }
            }
        }
        /** End debug output. **/

        mNoOfItemsSent = mContactChangeList.size();
        setReqId(Contacts.bulkUpdateContacts(getEngine(), mContactChangeList));
    }

    /**
     * Sends the next page of new/modified details to the server.
     */
    private void sendNextDetailChangesPage() {
        mContactChangeList.clear();
        long startTime = System.nanoTime();
        ContactDetailsTable.syncServerGetNextNewContactDetails(mContactsCursor, mContactChangeList,
                MAX_UP_PAGE_SIZE);
        mDbSyncTime += (System.nanoTime() - startTime);

        if (mContactChangeList.size() == 0) {
            moveToNextState();
            return;
        }

        /** Debug output. **/
        if (Settings.ENABLED_CONTACTS_SYNC_TRACE) {
            LogUtils.logI("UploadServerContacts.sendNextDetailChangesPage() "
                    + "Contact detail changes:");
            for (Contact c : mContactChangeList) {
                for (ContactDetail d : c.details) {
                    LogUtils.logI("UploadServerContacts." + "sendNextDetailChangesPage() Contact: "
                            + c.contactID + ", Detail: " + d.key + ", " + d.unique_id + " = "
                            + d.value);
                }
            }
        }
        /** End debug output. **/

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        mNoOfItemsSent = mContactChangeList.size();
        setReqId(Contacts.bulkUpdateContacts(getEngine(), mContactChangeList));
    }

    /**
     * Sends the next page of deleted contacts to the server.
     */
    private void sendNextDeleteContactsPage() {
        mContactChangeInfoList.clear();
        long startTime = System.nanoTime();
        if (!ContactChangeLogTable.fetchContactChangeLog(mContactChangeInfoList,
                ContactChangeType.DELETE_CONTACT, 0, MAX_UP_PAGE_SIZE, mDb.getReadableDatabase())) {
            LogUtils.logE("UploadServerContacts.sendNextDeleteContactsPage() "
                    + "Unable to fetch contact changes from database");
            complete(ServiceStatus.ERROR_DATABASE_CORRUPT);
            return;
        }
        mDbSyncTime += (System.nanoTime() - startTime);

        if (mContactChangeInfoList.size() == 0) {
            moveToNextState();
            return;
        }
        mContactIdList.clear();
        for (ContactChangeInfo info : mContactChangeInfoList) {
            if (info.mServerContactId != null) {
                mContactIdList.add(info.mServerContactId);
            }
        }

        if (mContactIdList.size() == 0) {
            startTime = System.nanoTime();
            mDb.deleteContactChanges(mContactChangeInfoList);
            mDbSyncTime += (System.nanoTime() - startTime);
            moveToNextState();
            return;
        }

        /** Debug output. **/
        if (Settings.ENABLED_CONTACTS_SYNC_TRACE) {
            LogUtils.logI("UploadServerContacts.sendNextDeleteContactsPage() "
                    + "Contacts deleted:");
            for (Long id : mContactIdList) {
                LogUtils.logI("UploadServerContacts." + "sendNextDeleteContactsPage() Contact Id: "
                        + id);
            }
        }
        /** Debug output. **/

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        mNoOfItemsSent = mContactIdList.size();
        setReqId(Contacts.deleteContacts(getEngine(), mContactIdList));
    }

    /**
     * Sends the next deleted details to the server. This has to be done one
     * contact at a time.
     */
    private void sendNextDeleteDetailsPage() {
        mContactChangeInfoList.clear();

        long startTime = System.nanoTime();
        List<ContactChangeInfo> groupInfoList = new ArrayList<ContactChangeInfo>();
        if (!ContactChangeLogTable.fetchContactChangeLog(groupInfoList,
                ContactChangeType.DELETE_DETAIL, 0, MAX_UP_PAGE_SIZE, mDb.getReadableDatabase())) {
            LogUtils.logE("UploadServerContacts.sendNextDeleteDetailsPage() "
                    + "Unable to fetch contact changes from database");
            complete(ServiceStatus.ERROR_DATABASE_CORRUPT);
            return;
        }
        mDbSyncTime += (System.nanoTime() - startTime);

        if (groupInfoList.size() == 0) {
            moveToNextState();
            return;
        }

        mDeleteDetailContact = new Contact();
        List<ContactChangeInfo> deleteInfoList = new ArrayList<ContactChangeInfo>();

        int i = 0;
        for (i = 0; i < groupInfoList.size(); i++) {
            ContactChangeInfo info = groupInfoList.get(i);
            if (info.mServerContactId == null) {
                info.mServerContactId = mDb.fetchServerId(info.mLocalContactId);
            }

            if (info.mServerContactId != null) {
                mDeleteDetailContact.localContactID = info.mLocalContactId;
                mDeleteDetailContact.contactID = info.mServerContactId;
                break;
            }
            deleteInfoList.add(info);
            info.mLocalContactId = null;
        }

        mDb.deleteContactChanges(deleteInfoList);
        if (mDeleteDetailContact.contactID == null) {
            moveToNextState();
            return;
        }

        mContactChangeInfoList.clear();
        for (; i < groupInfoList.size(); i++) {
            ContactChangeInfo info = groupInfoList.get(i);
            if (info.mLocalContactId != null
                    && info.mLocalContactId.equals(mDeleteDetailContact.localContactID)) {
                final ContactDetail detail = new ContactDetail();
                detail.localDetailID = info.mLocalDetailId;
                detail.key = info.mServerDetailKey;
                detail.unique_id = info.mServerDetailId;
                mDeleteDetailContact.details.add(detail);
                mContactChangeInfoList.add(info);
            }
        }

        /** Debug output. **/
        if (Settings.ENABLED_CONTACTS_SYNC_TRACE) {
            LogUtils.logI("UploadServerContacts.sendNextDeleteDetailsPage() "
                    + "Contact details for deleting:");
            for (ContactDetail detail : mDeleteDetailContact.details) {
                LogUtils.logI("UploadServerContacts." + "sendNextDeleteDetailsPage() Contact ID: "
                        + mDeleteDetailContact.contactID + ", detail = " + detail.key
                        + ", unique_id = " + detail.unique_id);
            }
        }
        /** Debug output. **/

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        mNoOfItemsSent = mDeleteDetailContact.details.size();
        setReqId(Contacts.deleteContactDetails(getEngine(), mDeleteDetailContact.contactID,
                mDeleteDetailContact.details));
    }

    /**
     * Sends next add contact/group relation request to the server. Many
     * contacts can be added by a single request.
     */
    private void sendNextAddGroupRelationsPage() {
        ContactChangeInfo info = null;
        mActiveGroupId = null;
        mContactIdList.clear();
        mContactChangeInfoList.clear();
        List<ContactChangeInfo> groupInfoList = new ArrayList<ContactChangeInfo>();

        long startTime = System.nanoTime();
        if (!ContactChangeLogTable.fetchContactChangeLog(groupInfoList,
                ContactChangeType.ADD_GROUP_REL, 0, MAX_UP_PAGE_SIZE, mDb.getReadableDatabase())) {
            LogUtils.logE("UploadServerContacts."
                    + "sendNextAddGroupRelationsPage() Unable to fetch add "
                    + "group relations from database");
            complete(ServiceStatus.ERROR_DATABASE_CORRUPT);
            return;
        }
        mDbSyncTime += (System.nanoTime() - startTime);

        if (groupInfoList.size() == 0) {
            moveToNextState();
            return;
        }

        mContactChangeInfoList.clear();
        List<ContactChangeInfo> deleteInfoList = new ArrayList<ContactChangeInfo>();

        for (int i = 0; i < groupInfoList.size(); i++) {
            info = groupInfoList.get(i);

            if (info.mServerContactId == null) {
                info.mServerContactId = mDb.fetchServerId(info.mLocalContactId);
            }
            if (info.mServerContactId != null && info.mGroupOrRelId != null) {
                if (mActiveGroupId == null) {
                    mActiveGroupId = info.mGroupOrRelId;
                }
                if (info.mGroupOrRelId.equals(mActiveGroupId)) {
                    mContactIdList.add(info.mServerContactId);
                    mContactChangeInfoList.add(info);
                }
                continue;
            }
            LogUtils.logE("UploadServerContact.sendNextAddGroupRelationsPage() "
                    + "Invalid add group change: SID = " + info.mServerContactId + ", gid="
                    + info.mGroupOrRelId);
            deleteInfoList.add(info);
        }
        mDb.deleteContactChanges(deleteInfoList);
        if (mActiveGroupId == null) {
            moveToNextState();
            return;
        }

        mGroupList.clear();
        GroupItem group = new GroupItem();
        group.mId = mActiveGroupId;
        mGroupList.add(group);

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        mNoOfItemsSent = mGroupList.size();
        setReqId(GroupPrivacy.addContactGroupRelations(getEngine(), mContactIdList, mGroupList));
    }

    /**
     * Sends next delete contact/group relation request to the server.
     */
    private void sendNextDelGroupRelationsPage() {
        ContactChangeInfo info = null;
        mActiveGroupId = null;
        mContactChangeInfoList.clear();
        List<ContactChangeInfo> groupInfoList = new ArrayList<ContactChangeInfo>();
        long startTime = System.nanoTime();
        if (!ContactChangeLogTable.fetchContactChangeLog(groupInfoList,
                ContactChangeType.DELETE_GROUP_REL, 0, MAX_UP_PAGE_SIZE, mDb.getReadableDatabase())) {
            LogUtils.logE("UploadServerContacts."
                    + "sendNextDelGroupRelationsPage() Unable to fetch delete "
                    + "group relations from database");
            complete(ServiceStatus.ERROR_DATABASE_CORRUPT);
            return;
        }
        mDbSyncTime += (System.nanoTime() - startTime);
        if (groupInfoList.size() == 0) {
            moveToNextState();
            return;
        }

        mContactChangeInfoList.clear();
        List<ContactChangeInfo> deleteInfoList = new ArrayList<ContactChangeInfo>();
        for (int i = 0; i < groupInfoList.size(); i++) {
            info = groupInfoList.get(i);
            if (info.mServerContactId == null) {
                info.mServerContactId = mDb.fetchServerId(info.mLocalContactId);
            }
            if (info.mServerContactId != null && info.mGroupOrRelId != null) {
                if (mActiveGroupId == null) {
                    mActiveGroupId = info.mGroupOrRelId;
                }
                if (mActiveGroupId.equals(info.mGroupOrRelId)) {
                    mContactIdList.add(info.mServerContactId);
                    mContactChangeInfoList.add(info);
                }
                continue;
            }
            LogUtils.logE("UploadServerContact.sendNextDelGroupRelationsPage() "
                    + "Invalid delete group change: SID = " + info.mServerContactId + ", gid="
                    + info.mGroupOrRelId);
            deleteInfoList.add(info);
        }

        mDb.deleteContactChanges(deleteInfoList);
        if (mActiveGroupId == null) {
            moveToNextState();
            return;
        }

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            complete(ServiceStatus.ERROR_COMMS);
            return;
        }

        mNoOfItemsSent = mContactIdList.size();
        setReqId(GroupPrivacy.deleteContactGroupRelationsExt(getEngine(), mActiveGroupId,
                mContactIdList));
    }

    /**
     * Changes the state of the processor to the next internal state. Is called
     * when the current state has finished.
     */
    private void moveToNextState() {
        switch (mInternalState) {
            case PROCESSING_NEW_CONTACTS:
                startProcessModifiedDetails();
                break;

            case PROCESSING_MODIFIED_DETAILS:
                startProcessDeletedContacts();
                break;

            case PROCESSING_DELETED_CONTACTS:
                startProcessDeletedDetails();
                break;

            case PROCESSING_DELETED_DETAILS:
                startProcessGroupAdditions();
                break;

            case PROCESSING_GROUP_ADDITIONS:
                startProcessGroupDeletions();
                break;

            case PROCESSING_GROUP_DELETIONS:
                LogUtils.logV("UploadServerContacts.moveToNextState() " + "Total DB access time = "
                        + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms, no of changes = "
                        + mTotalNoOfItems);
                complete(ServiceStatus.SUCCESS);
                break;

            default:
                complete(ServiceStatus.ERROR_NOT_READY);
                break;
        }
    }

    /**
     * Called by framework to cancel contact sync. No implementation required,
     * the server response will be ignored and the contact sync will be repeated
     * if necessary.
     */
    @Override
    protected void doCancel() {
        // Do nothing.
    }

    /**
     * Called by framework when a response to a server request is received.
     * Processes response based on the current internal state.
     * 
     * @param resp Response data
     */
    @Override
    public final void processCommsResponse(final DecodedResponse resp) {
        switch (mInternalState) {
            case PROCESSING_NEW_CONTACTS:
                processNewContactsResp(resp);
                break;

            case PROCESSING_MODIFIED_DETAILS:
                processModifiedDetailsResp(resp);
                break;

            case PROCESSING_DELETED_CONTACTS:
                processDeletedContactsResp(resp);
                break;

            case PROCESSING_DELETED_DETAILS:
                processDeletedDetailsResp(resp);
                break;

            case PROCESSING_GROUP_ADDITIONS:
                processGroupAdditionsResp(resp);
                break;

            case PROCESSING_GROUP_DELETIONS:
                processGroupDeletionsResp(resp);
                break;

            default:
                // Do nothing.
                break;
        }
    }

    /**
     * Called when a server response is received during a new contact sync. The
     * server ID, user ID and contact detail unique IDs are extracted from the
     * response and the NowPlus database updated. Possibly server errors are
     * also handled.
     * 
     * @param resp Response from server.
     */
    private void processNewContactsResp(final DecodedResponse resp) {
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_CHANGES_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactChanges contactChanges = (ContactChanges)resp.mDataTypes.get(0);
            ListIterator<Contact> itContactSrc = contactChanges.mContacts.listIterator();
            ListIterator<Contact> itContactDest = mContactChangeList.listIterator();
            List<ServerIdInfo> contactServerIdList = new ArrayList<ServerIdInfo>();
            List<ServerIdInfo> detailServerIdList = new ArrayList<ServerIdInfo>();

            while (itContactSrc.hasNext()) {
                if (!itContactDest.hasNext()) {
                    /**
                     * The response should contain the same number of contacts
                     * as was supplied but must handle the error.
                     */
                    status = ServiceStatus.ERROR_COMMS_BAD_RESPONSE;
                    break;
                }
                Contact contactSrc = itContactSrc.next();
                Contact contactDest = itContactDest.next();
                if (Settings.ENABLED_CONTACTS_SYNC_TRACE) {
                    String name = null;
                    String sns = null;
                    for (ContactDetail detail : contactDest.details) {
                        if (detail.key == ContactDetail.DetailKeys.VCARD_NAME) {
                            if (detail.value != null) {
                                VCardHelper.Name nameObj = detail.getName();
                                if (nameObj != null) {
                                    name = nameObj.toString();
                                }
                            }
                        }
                        if (detail.key == ContactDetail.DetailKeys.VCARD_INTERNET_ADDRESS) {
                            sns = detail.alt;
                        }
                    }
                    LogUtils.logV("UploadServerContacts."
                            + "processNewContactsResp() Contact uploaded: SID" + " = "
                            + contactSrc.contactID + ", name = " + name + ", sns = " + sns
                            + ", no of details = " + contactDest.details.size() + ", deleted="
                            + contactSrc.deleted);
                }
                if (contactSrc.contactID != null && contactSrc.contactID.longValue() != -1L) {
                    if (contactDest.contactID == null
                            || !contactDest.contactID.equals(contactSrc.contactID)) {
                        ServerIdInfo info = new ServerIdInfo();
                        info.localId = contactDest.localContactID;
                        info.serverId = contactSrc.contactID;
                        info.userId = contactSrc.userID;
                        contactServerIdList.add(info);
                    }
                } else {
                    LogUtils.logE("UploadServerContacts."
                            + "processNewContactsResp() The server failed to "
                            + "add the following contact: " + contactDest.localContactID
                            + ", server ID = " + contactDest.contactID);
                    mFailureList += "Failed to add contact: " + contactDest.localContactID + "\n";
                    for (ContactDetail d : contactDest.details) {
                        LogUtils.logV("Failed Contact Info: " + contactDest.localContactID
                                + ", Detail: " + d.key + ", " + d.keyType + " = " + d.value);
                    }
                }
                status = handleUploadDetailChanges(contactSrc, contactDest, detailServerIdList);
            }
            if (status != ServiceStatus.SUCCESS) {
                /** Something is going wrong - cancel the update **/
                complete(status);
                return;
            }

            long startTime = System.nanoTime();
            List<ContactIdInfo> dupList = new ArrayList<ContactIdInfo>();
            status = ContactsTable.syncSetServerIds(contactServerIdList, dupList, mDb
                    .getWritableDatabase());
            if (status != ServiceStatus.SUCCESS) {
                complete(status);
                return;
            }
            status = ContactDetailsTable.syncSetServerIds(detailServerIdList, mDb
                    .getWritableDatabase());
            if (status != ServiceStatus.SUCCESS) {
                complete(status);
                return;
            }
            if (dupList.size() > 0) {
                LogUtils.logV("UploadServerContacts.processNewContactsResp() Found "
                        +dupList.size()+ " duplicate contacts. Trying to remove them...");
                if(VersionUtils.is2XPlatform()) {
                    // This is a very important distinction for 2.X devices!
                    // the NAB IDs from the contacts we first import are stripped away
                    // So we won't have the correct ID if syncMergeContactList() is executed
                    // This is critical because a chain reaction will cause a Contact Delete in the end
                    // Instead we can syncDeleteContactList() which should be safe on 2.X!
                    status = mDb.syncDeleteContactList(dupList, false, true);   
                } else {
                    status = mDb.syncMergeContactList(dupList);
                }
                if (status != ServiceStatus.SUCCESS) {
                    complete(status);
                    return;
                }
                markDbChanged();
            }
            mDbSyncTime += (System.nanoTime() - startTime);

            while (itContactDest.hasNext()) {
                Contact contactDest = itContactDest.next();
                LogUtils.logE("UploadServerContacts.processNewContactsResp() "
                        + "The server failed to add the following contact (not "
                        + "included in returned list): " + contactDest.localContactID);
                mFailureList += "Failed to add contact (missing from return " + "list): "
                        + contactDest.localContactID + "\n";
            }
            updateProgress();
            sendNextContactAdditionsPage();
            return;
        }
        complete(status);
    }

    /**
     * Called when a server response is received during a modified contact sync.
     * The server ID, user ID and contact detail unique IDs are extracted from
     * the response and the NowPlus database updated if necessary. Possibly
     * server errors are also handled.
     * 
     * @param resp Response from server.
     */
    private void processModifiedDetailsResp(final DecodedResponse resp) {
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_CHANGES_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactChanges contactChanges = (ContactChanges)resp.mDataTypes.get(0);
            ListIterator<Contact> itContactSrc = contactChanges.mContacts.listIterator();
            ListIterator<Contact> itContactDest = mContactChangeList.listIterator();
            List<ServerIdInfo> detailServerIdList = new ArrayList<ServerIdInfo>();
            while (itContactSrc.hasNext()) {
                if (!itContactDest.hasNext()) {
                    /*
                     * The response should contain the same number of contacts
                     * as was supplied but must handle the error.
                     */
                    status = ServiceStatus.ERROR_COMMS_BAD_RESPONSE;
                    break;
                }
                status = handleUploadDetailChanges(itContactSrc.next(), itContactDest.next(),
                        detailServerIdList);
            }
            if (status != ServiceStatus.SUCCESS) {
                /** Something is going wrong - cancel the update. **/
                complete(status);
                return;
            }

            long startTime = System.nanoTime();
            status = ContactDetailsTable.syncSetServerIds(detailServerIdList, mDb
                    .getWritableDatabase());
            if (status != ServiceStatus.SUCCESS) {
                complete(status);
                return;
            }
            mDb.deleteContactChanges(mContactChangeInfoList);
            mDbSyncTime += (System.nanoTime() - startTime);

            mContactChangeInfoList.clear();
            updateProgress();
            sendNextDetailChangesPage();
            return;
        }
        LogUtils.logE("UploadServerContacts.processModifiedDetailsResp() "
                + "Error requesting contact changes, error = " + status);
        complete(status);
    }

    /**
     * Called when a server response is received during a deleted contact sync.
     * The server change log is updated. Possibly server errors are also
     * handled.
     * 
     * @param resp Response from server.
     */
    private void processDeletedContactsResp(final DecodedResponse resp) {
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_LIST_RESPONSE_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactListResponse result = (ContactListResponse)resp.mDataTypes.get(0);
            ListIterator<ContactChangeInfo> infoIt = mContactChangeInfoList.listIterator();
            for (Integer contactID : result.mContactIdList) {
                if (!infoIt.hasNext()) {
                    complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                    return;
                }
                ContactChangeInfo info = infoIt.next();
                if (contactID == null || contactID.intValue() == -1) {
                    LogUtils.logE("UploadServerContacts."
                            + "processDeletedContactsResp() The server failed "
                            + "to delete the following contact: LocalId = " + info.mLocalContactId
                            + ", ServerId = " + info.mServerContactId);
                    mFailureList += "Failed to delete contact: " + info.mLocalContactId + "\n";
                }
            }

            long startTime = System.nanoTime();
            mDb.deleteContactChanges(mContactChangeInfoList);
            mDbSyncTime += (System.nanoTime() - startTime);

            mContactChangeInfoList.clear();
            updateProgress();
            sendNextDeleteContactsPage();
            return;
        }
        LogUtils.logE("UploadServerContacts.processModifiedDetailsResp() "
                + "Error requesting contact changes, error = " + status);
        complete(status);
    }

    /**
     * Called when a server response is received during a deleted contact detail
     * sync. The server change log is updated. Possibly server errors are also
     * handled.
     * 
     * @param resp Response from server.
     */
    private void processDeletedDetailsResp(final DecodedResponse resp) {
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_DETAIL_DELETION_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactDetailDeletion result = (ContactDetailDeletion)resp.mDataTypes.get(0);
            if (result.mDetails != null) {
                LogUtils.logV("UploadServerContacts."
                        + "processDeletedDetailsResp() Deleted details " + result.mDetails.size());
            }

            ListIterator<ContactChangeInfo> infoIt = mContactChangeInfoList.listIterator();
            if (result.mContactId == null || result.mContactId == -1) {
                boolean first = true;
                while (infoIt.hasNext()) {
                    ContactChangeInfo info = infoIt.next();
                    if (first) {
                        first = false;
                        LogUtils.logE("UploadServerContacts."
                                + "processDeletedDetailsResp() The server "
                                + "failed to delete detail from the following "
                                + "contact: LocalId = " + info.mLocalContactId + ", ServerId = "
                                + info.mServerContactId);
                    }
                    mFailureList += "Failed to delete detail: " + info.mLocalDetailId + "\n";
                }
            } else if (result.mDetails != null) {
                for (ContactDetail d : result.mDetails) {
                    if (!infoIt.hasNext()) {
                        complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                        return;
                    }
                    ContactChangeInfo info = infoIt.next();
                    if (!d.key.equals(info.mServerDetailKey)) {
                        LogUtils.logE("UploadServerContacts."
                                + "processDeletedDetailsResp() The server "
                                + "failed to delete the following detail: " + "LocalId = "
                                + info.mLocalContactId + ", " + "ServerId = "
                                + info.mServerContactId + ", key = " + info.mServerDetailKey
                                + ", detail ID = " + info.mServerDetailId);
                        mFailureList += "Failed to delete detail: " + info.mLocalDetailId + "\n";
                    }
                }
            }

            long startTime = System.nanoTime();
            mDb.deleteContactChanges(mContactChangeInfoList);
            mDbSyncTime += (System.nanoTime() - startTime);

            mContactChangeInfoList.clear();
            updateProgress();
            sendNextDeleteDetailsPage();
            return;
        }
        LogUtils.logE("UploadServerContacts.processModifiedDetailsResp() "
                + "Error requesting contact changes, error = " + status);
        complete(status);
    }

    /**
     * Called when a server response is received during a group/contact add
     * relation sync. The server change log is updated. Possibly server errors
     * are also handled.
     * 
     * @param resp Response from server.
     */
    private void processGroupAdditionsResp(final DecodedResponse resp) {
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.ITEM_LIST_DATA_TYPE, resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            if (resp.mDataTypes.size() == 0) {
                LogUtils.logE("UploadServerContacts." + "processGroupAdditionsResp() "
                        + "Item list cannot be empty");
                complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                return;
            }
            ItemList itemList = (ItemList)resp.mDataTypes.get(0);
            if (itemList.mItemList == null) {
                LogUtils.logE("UploadServerContacts." + "processGroupAdditionsResp() "
                        + "Item list cannot be NULL");
                complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                return;
            }

            // TODO: Check response
            long startTime = System.nanoTime();
            mDb.deleteContactChanges(mContactChangeInfoList);
            mDbSyncTime += (System.nanoTime() - startTime);

            mContactChangeInfoList.clear();
            updateProgress();
            sendNextAddGroupRelationsPage();
            return;
        }
        LogUtils.logE("UploadServerContacts.processGroupAdditionsResp() "
                + "Error adding group relations, error = " + status);
        complete(status);
    }

    /**
     * Called when a server response is received during a group/contact delete
     * relation sync. The server change log is updated. Possibly server errors
     * are also handled.
     * 
     * @param resp Response from server.
     */
    private void processGroupDeletionsResp(final DecodedResponse resp) {
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.STATUS_MSG_DATA_TYPE, resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            if (resp.mDataTypes.size() == 0) {
                LogUtils.logE("UploadServerContacts." + "processGroupDeletionsResp() "
                        + "Response cannot be empty");
                complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                return;
            }
            StatusMsg result = (StatusMsg)resp.mDataTypes.get(0);
            if (!result.mStatus.booleanValue()) {
                LogUtils.logE("UploadServerContacts."
                        + "processGroupDeletionsResp() Error deleting group "
                        + "relation, error = " + result.mError);
                complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
            }
            LogUtils.logV("UploadServerContacts." + "processGroupDeletionsResp() Deleted relation");

            long startTime = System.nanoTime();
            mDb.deleteContactChanges(mContactChangeInfoList);
            mDbSyncTime += (System.nanoTime() - startTime);

            mContactChangeInfoList.clear();
            updateProgress();
            sendNextDelGroupRelationsPage();
            return;
        }
        LogUtils.logE("UploadServerContacts.processGroupDeletionsResp() "
                + "Error deleting group relation, error = " + status);
        complete(status);
    }

    /**
     * Called when handling the server response from a new contact or modify
     * contact sync. Updates the unique IDs for all the details if necessary.
     * 
     * @param contactSrc Contact received from server.
     * @param contactDest Contact from database.
     * @param detailServerIdList List of contact details with updated unique id.
     * @return ServiceStatus object.
     */
    private ServiceStatus handleUploadDetailChanges(final Contact contactSrc,
            final Contact contactDest, final List<ServerIdInfo> detailServerIdList) {
        if (contactSrc.contactID == null || contactSrc.contactID.longValue() == -1L) {
            LogUtils.logE("UploadServerContacts.handleUploadDetailChanges() "
                    + "The server failed to modify the following contact: "
                    + contactDest.localContactID);
            mFailureList += "Failed to add contact: " + contactDest.localContactID + "\n";
            return ServiceStatus.SUCCESS;
        }
        ListIterator<ContactDetail> itContactDetailSrc = contactSrc.details.listIterator();
        ListIterator<ContactDetail> itContactDetailDest = contactDest.details.listIterator();
        while (itContactDetailSrc.hasNext()) {
            if (!itContactDetailDest.hasNext()) {
                /*
                 * The response should contain the same number of details as was
                 * supplied but must handle the error.
                 */
                return ServiceStatus.ERROR_COMMS_BAD_RESPONSE;
            }
            ContactDetail contactDetailSrc = itContactDetailSrc.next();
            ContactDetail contactDetailDest = itContactDetailDest.next();
            ServerIdInfo info = new ServerIdInfo();
            info.localId = contactDetailDest.localDetailID;
            if (contactDetailSrc.unique_id != null && contactDetailSrc.unique_id.longValue() == -1L) {
                LogUtils.logE("UploadServerContacts."
                        + "handleUploadDetailChanges() The server failed to "
                        + "modify the following contact detail: LocalDetailId " + "= "
                        + contactDetailDest.localDetailID + ", Key = " + contactDetailDest.key
                        + ", value = " + contactDetailDest.value);
                mFailureList += "Failed to modify contact detail: "
                        + contactDetailDest.localDetailID + ", for contact "
                        + contactDetailDest.localContactID + "\n";
                info.serverId = null;
            } else {
                info.serverId = contactDetailSrc.unique_id;
            }
            detailServerIdList.add(info);
        }
        while (itContactDetailDest.hasNext()) {
            ContactDetail contactDetailDest = itContactDetailDest.next();
            mFailureList += "Failed to modify contact detail (not in return " + "list):"
                    + contactDetailDest.localDetailID + ", for contact "
                    + contactDetailDest.localContactID + "\n";
            LogUtils.logE("UploadServerContacts.handleUploadDetailChanges() "
                    + "The server failed to modify the following contact detail "
                    + "(not found in returned list): LocalDetailId = "
                    + contactDetailDest.localDetailID + ", Key = " + contactDetailDest.key
                    + ", value = " + contactDetailDest.value);
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Helper method to update the progress UI.
     */
    private void updateProgress() {
        mItemsDone += mNoOfItemsSent;
        if (mTotalNoOfItems > 0) {
            setSyncStatus(new SyncStatus((int)(mItemsDone * MAX_PROGESS / mTotalNoOfItems), "",
                    Task.UPDATE_SERVER_CONTACTS, TaskStatus.SENT_CONTACTS, mItemsDone,
                    mTotalNoOfItems));

        } else {
            setSyncStatus(new SyncStatus(0, "", SyncStatus.Task.UPDATE_SERVER_CONTACTS));
        }
    }

    /**
     * Returns the current internal state of the processor (for testing only).
     * 
     * @return InternalState of processor.
     */
    public final InternalState getInternalState() {
        return mInternalState;
    }

    /**
     * Returns a list of server IDs which is sent to the server by the {@code
     * DeleteContacts} API (for testing only).
     * 
     * @return List of server IDs.
     */
    public final List<Long> getContactIdList() {
        return mContactIdList;
    }

    /**
     * Return the current list of groups being added to the contacts listed in
     * the {@link #mContactIdList}. Will only contain a single group (list is
     * needed for the
     * {@link GroupPrivacy#addContactGroupRelations(BaseEngine, List, List)}
     * API) (for testing only).
     * 
     * @return List of groups.
     */
    public final List<GroupItem> getGroupList() {
        return mGroupList;
    }

    /**
     * Returns the next page of contacts being sent to the server which have
     * either been added or changed (for testing only).
     * 
     * @return List of contacts.
     */
    public final List<Contact> getContactChangeList() {
        return mContactChangeList;
    }

    /**
     * Returns the main contact during the contact detail deletion process (for
     * testing only).
     * 
     * @return Main contact.
     */
    public final Contact getDeleteDetailContact() {
        return mDeleteDetailContact;
    }

    /**
     * Returns the server ID of the group being associated with a contact (for
     * testing only).
     * 
     * @return Server ID.
     */
    public final Long getActiveGroupId() {
        return mActiveGroupId;
    }
    
    @Override
    protected void complete(ServiceStatus status) {

        // do our internal cleanup before completing
        if (mContactsCursor != null) {
            
            mContactsCursor.close();
            mContactsCursor = null;
        }
        
        // call the base class implementation 
        super.complete(status);
    }

}
