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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ContactsTable;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactChanges;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.contactsync.SyncStatus.Task;
import com.vodafone360.people.engine.contactsync.SyncStatus.TaskStatus;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.api.Contacts;
import com.vodafone360.people.utils.LogUtils;

/**
 * Handles download of contacts from People server.
 */
public class DownloadServerContacts extends BaseSyncProcessor {
    
    /**
     * Helper constant for converting between milliseconds and nanoseconds.
     */
    private static final long NANOSECONDS_IN_MS = 1000000L;

    /**
     * Enumeration which holds the various states for the download server
     * processor.
     */
    public enum InternalState {
        INITIALISING,
        FETCHING_SERVER_ID_LIST,
        FETCHING_FIRST_PAGE,
        FETCHING_NEXT_BATCH
    }

    /**
     * The maximum number of contacts that should be sent by the server for each
     * page request.
     */
    protected static final int MAX_DOWN_PAGE_SIZE = 25;

    /**
     * The number of pages that should be requested in a single RPG request
     * (using batching).
     */
    private static final int MAX_PAGES_PER_BATCH = 1;

    /**
     * Maximum number of database updates for each engine run (to ensure the
     * processor doesn't block the worker thread for too long).
     */
    private static final int MAX_CONTACT_CHANGES_PER_PAGE = 5;

    /**
     * Timeout between each run of the engine. Normally set to 0 to ensure the
     * engine will run as soon as possible. This is used to allow other engines
     * to be run between lengthy sync operations.
     */
    private static final long TIMEOUT_BETWEEN_PAGES_MS = 0;

    /**
     * Set to true for extra logcat trace (debug only)
     */
    private static final boolean EXTRA_DEBUG_TRACE = true;
    
    /**
     * This will be set to the current revision of the contacts in the NowPlus
     * database. For first time sync will be set to 0. The current revision
     * number is persisted in the database.
     */
    private Integer mFromRevision = null;

    /**
     * The revision that we require from the server. For the first page this is
     * set to -1 to indicate we want the latest revision. For subsequent pages
     * will be set to the version anchor received from the server (to ensure all
     * pages fetched are for the same version).
     */
    private Integer mToRevision = null;

    /**
     * Total number of pages we are expecting from the server. This is received
     * as part of the server response for the first page.
     */
    private Integer mTotalNoOfPages = null;

    /**
     * First page number requested in the batch. The remaining page numbers will
     * follow from this sequentially.
     */
    private int mBatchFirstPageNo;

    /**
     * Total number of pages requested by batch.
     */
    private int mBatchNoOfPages;

    /**
     * Number of pages received so far from the batch
     */
    private int mBatchPageReceivedCount;

    /**
     * Total number of pages done (used with {@link #mTotalNoOfPages} for
     * calculating progress)
     */
    private int mNoOfPagesDone = 0;

    /***
     * Number of contacts contained in the last page. This is only known once
     * the last page have been received and is a major flaw in the protocol as
     * it makes it difficult to calculate sync progress information.
     */
    private int mLastPageSize = -1;
    
    /**
     * List of contact server IDs from the NowPlus database in ascending order.
     * Used to quickly determine if a contact received from the server is a new
     * or modified contact.
     */
    private final ArrayList<Long> mOrderedServerIdList = new ArrayList<Long>();

    /**
     * List of all contacts received from server which are also present in the
     * local database. This is a list of all contacts received from the server
     * excluding new contacts.
     */
    private final ArrayList<Contact> mContactsChangedList = new ArrayList<Contact>();

    /**
     * List of all contacts received from the server that need to be added to
     * the database.
     */
    private final ArrayList<Contact> mAddContactList = new ArrayList<Contact>();

    /**
     * List of all contacts received from the server that need to be modified in
     * the database.
     */
    private final ArrayList<Contact> mModifyContactList = new ArrayList<Contact>();

    /**
     * List of all contacts received from the server that need to be deleted
     * from the database.
     */
    private final ArrayList<ContactsTable.ContactIdInfo> mDeleteContactList = new ArrayList<ContactsTable.ContactIdInfo>();

    /**
     * List of all contact details received from the server that need to be
     * added to the local database.
     */
    private final ArrayList<ContactDetail> mAddDetailList = new ArrayList<ContactDetail>();

    /**
     * List of all contact details received from the server that need to be
     * modified in the local database.
     */
    private final ArrayList<ContactDetail> mModifyDetailList = new ArrayList<ContactDetail>();

    /**
     * List of all contact details received from the server that need to be
     * deleted from the local database.
     */
    private final ArrayList<ContactDetail> mDeleteDetailList = new ArrayList<ContactDetail>();

    /**
     * Maintains all the request IDs when sending a batch of requests.
     */
    protected final Map<Integer, Integer> mPageReqIds = new HashMap<Integer, Integer>();

    /**
     * Flag indicating that there is some data in either the
     * {@link #mAddContactList}, {@link #mModifyContactList},
     * {@link #mDeleteContactList}, {@link #mAddDetailList},
     * {@link #mModifyDetailList} or {@link #mDeleteDetailList} list.
     */
    private boolean mSyncDataPending = false;

    /**
     * Current internal state of the processor.
     */
    protected InternalState mInternalState = InternalState.INITIALISING;

    /**
     * Used to monitor how much time is spent reading or writing to the NowPlus
     * database. This is for displaying logcat information which can be used
     * when profiling the contact sync engine.
     */
    private long mDbSyncTime = 0;

    /**
     * Set to true when there are no more pages to fetch from the server.
     */
    private boolean mIsComplete;

    /**
     * Counts the number of contacts added (for use when profiling)
     */
    private long mTotalContactsAdded;

    /**
     * Processor constructor.
     * 
     * @param callback Callback interface for accessing the contact sync engine.
     * @param db Database used for modifying contacts
     */
    protected DownloadServerContacts(IContactSyncCallback callback, DatabaseHelper db) {
        super(callback, db);
    }

    /**
     * For debug only - to show when the garbage collector destroys the
     * processor
     */
    @Override
    protected void finalize() {
        LogUtils.logD("DownloadServerContacts.finalize() - processor deleted");
    }

    /**
     * Called by framework to start the processor running. Requests the first
     * page of contact changes from the server.
     */
    @Override
    protected void doStart() {
        setSyncStatus(new SyncStatus(0, "", Task.DOWNLOAD_SERVER_CONTACTS));
        
        mIsComplete = false;
        mSyncDataPending = false;
        mFromRevision = StateTable.fetchContactRevision(mDb.getReadableDatabase());
        if (mFromRevision == null) {
            mFromRevision = 0; // Use base
        }
        mToRevision = -1; // Sync with head revision
        mTotalNoOfPages = null;
        mBatchNoOfPages = 1;
        mBatchFirstPageNo = 0;
        mBatchPageReceivedCount = 0;
        mDbSyncTime = 0;
        mNoOfPagesDone = 0;
        mTotalContactsAdded = 0;
        mInternalState = InternalState.FETCHING_SERVER_ID_LIST;
        long startTime = System.nanoTime();
        ServiceStatus status = ContactsTable.fetchContactServerIdList(mOrderedServerIdList, mDb
                .getReadableDatabase());
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
        }
        mDbSyncTime += (System.nanoTime() - startTime);
        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
    }

    /**
     * Requests first page of contact changes from the server.
     * 
     * @return SUCCESS or a suitable error code.
     */
    private ServiceStatus fetchFirstBatch() {
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            return ServiceStatus.ERROR_COMMS;
        }
        mInternalState = InternalState.FETCHING_FIRST_PAGE;
        LogUtils.logD("DownloadServerContacts.fetchFirstBatch - from rev " + mFromRevision
                + ", to rev " + mToRevision + ", page size " + MAX_DOWN_PAGE_SIZE);
        int reqId = Contacts.getContactsChanges(getEngine(), 0, MAX_DOWN_PAGE_SIZE, mFromRevision
                .longValue(), mToRevision.longValue(), false);
        setReqId(reqId);
        return ServiceStatus.SUCCESS;
    }

    /**
     * Requests the next batch of contact change pages from the server
     * 
     * @return SUCCESS or a suitable error code
     */
    private ServiceStatus fetchNextBatch() {
        mBatchNoOfPages = Math.min(mTotalNoOfPages - mBatchFirstPageNo, MAX_PAGES_PER_BATCH);
        if (mBatchNoOfPages == 0) {
            return ServiceStatus.SUCCESS;
        }
        mBatchPageReceivedCount = 0;
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            return ServiceStatus.ERROR_COMMS;
        }

        mPageReqIds.clear();
        LogUtils.logD("DownloadServerContacts.fetchNextBatch - from rev " + mFromRevision
                + ", to rev " + mToRevision + ", page size " + MAX_DOWN_PAGE_SIZE);
        for (int i = 0; i < mBatchNoOfPages; i++) {
            int reqId = Contacts.getContactsChanges(getEngine(), i + mBatchFirstPageNo,
                    MAX_DOWN_PAGE_SIZE, mFromRevision.longValue(), mToRevision.longValue(), true);
            mPageReqIds.put(reqId, i + mBatchFirstPageNo);
        }
        // AA: see if we can do that inside the Queue
        QueueManager.getInstance().fireQueueStateChanged();
        return ServiceStatus.SUCCESS;
    }

    /**
     * Called by framework when contact sync is cancelled. Not used, the server
     * response will simply be ignored.
     */
    @Override
    protected void doCancel() {
        LogUtils.logD("DownloadServerContacts.doCancel()");
    }

    /**
     * Called by framework when a response is received from the server. In case
     * of the first page this will only be called if the request ID matches.
     * Processes the page and if the page is the last one in the batch, sends a
     * new batch of requests to the server.
     * 
     * @param response from server
     */
    @Override
    public void processCommsResponse(DecodedResponse resp) {
        Integer pageNo = 0;
        if (mInternalState == InternalState.FETCHING_NEXT_BATCH) {
            pageNo = mPageReqIds.remove(resp.mReqId);
            if (pageNo == null) {
                LogUtils.logD("DownloadServerContacts.processCommsResponse: Req ID not known");
                return;
            }
        }
        LogUtils.logD("DownloadServerContacts.processCommsResponse() - Page " + pageNo);
        ServiceStatus status = BaseEngine.getResponseStatus(BaseDataType.CONTACT_CHANGES_DATA_TYPE,
                resp.mDataTypes);
        if (status == ServiceStatus.SUCCESS) {
            ContactChanges contactChanges = (ContactChanges)resp.mDataTypes.get(0);
            LogUtils.logI("DownloadServerContacts.processCommsResponse - No of contacts = "
                    + contactChanges.mContacts.size());
            if (contactChanges.mContacts.size() == 0 && pageNo > 0) {
                LogUtils.logW("DownloadServerContacts.processCommsResponse - "
                        + "Error a page with 0 contacts was received");
                LogUtils.logW("DownloadServerContacts.processCommsResponse - Changes = "
                        + contactChanges);
            }
            mBatchPageReceivedCount++;
            if (mBatchPageReceivedCount == mBatchNoOfPages) {
                mLastPageSize = contactChanges.mContacts.size();
                
                // Page batch is now complete
                if (mInternalState == InternalState.FETCHING_FIRST_PAGE) {
                    mTotalNoOfPages = contactChanges.mNumberOfPages;
                    mToRevision = contactChanges.mVersionAnchor;
                    mInternalState = InternalState.FETCHING_NEXT_BATCH;
                }
                mBatchFirstPageNo += mBatchNoOfPages;
                if (mTotalNoOfPages == null || mToRevision == null) {
                    complete(ServiceStatus.ERROR_COMMS_BAD_RESPONSE);
                    return;
                }
                if (mBatchFirstPageNo < mTotalNoOfPages.intValue()) {
                    status = fetchNextBatch();
                    if (ServiceStatus.SUCCESS != status) {
                        complete(status);
                        return;
                    }
                } else {
                    mIsComplete = true;
                }
            }
            
  
            
            status = syncContactChangesPage(contactChanges);
            mNoOfPagesDone++;
            LogUtils.logI("DownloadServerContacts.processCommsResponse() - Contact changes page "
                    + mNoOfPagesDone + "/" + mTotalNoOfPages + " received, no of contacts = "
                    + contactChanges.mContacts.size());
            if (ServiceStatus.SUCCESS != status) {
                LogUtils.logE("DownloadServerContacts.processCommsResponse() - Error syncing page: " + status);
                complete(status);
                return;
            }
            if (mIsComplete && mContactsChangedList.size() == 0 && !mSyncDataPending) {
                downloadSyncSuccessful();
            }
            return;
        }
        complete(status);
    }
    
    /***
     * Check the given contact to see if it has a valid name, and if so send it
     * to be shown in the contacts sync progress UI.
     * 
     * @param contact Contact which has been downloaded.
     * @param incOfCurrentPage Number of contacts that have so far been
     *            downloaded for the current page.
     */
    private void updateProgressUi(Contact contact, int incOfCurrentPage) {
        String name = "";
        
        if (contact != null) {
            ContactDetail contactDetail = contact.getContactDetail(ContactDetail.DetailKeys.VCARD_NAME);
            if (contactDetail != null) {
                VCardHelper.Name vCardHelperName = contactDetail.getName();
                if (vCardHelperName != null) {
                    name = vCardHelperName.toString();
                }
            }
        }

        int totalNumberOfContacts = ((mTotalNoOfPages*10)-1)*MAX_DOWN_PAGE_SIZE/10;    
        if (mLastPageSize != -1) {
            totalNumberOfContacts = (mTotalNoOfPages-1) * MAX_DOWN_PAGE_SIZE + mLastPageSize;
        }

        int progress = (incOfCurrentPage + (mNoOfPagesDone * MAX_DOWN_PAGE_SIZE))*100 / totalNumberOfContacts;
        setSyncStatus(new SyncStatus(progress, name,
                Task.DOWNLOAD_SERVER_CONTACTS,
                TaskStatus.RECEIVED_CONTACTS,
                incOfCurrentPage + mNoOfPagesDone * MAX_DOWN_PAGE_SIZE,
                totalNumberOfContacts));
    }

    /**
     * Processes the response from the server. First separates new contacts from
     * existing ones, this is to optimise adding new contacts during first time
     * sync.
     * 
     * @param changes The contact change information received from the server
     * @return SUCCESS or a suitable error code
     */
    private ServiceStatus syncContactChangesPage(ContactChanges changes) {
        mContactsChangedList.clear();
        
        int i = 0;
        for (Contact contact : changes.mContacts) {
            i += 1;
            updateProgressUi(contact, i);
            
            if (Settings.ENABLED_CONTACTS_SYNC_TRACE) {
                if (contact.details.size() == 0 || contact.deleted != null) {
                    LogUtils.logD("Contact In: " + contact.contactID + ", Del: " + contact.deleted
                            + ", details: " + contact.details.size());
                }
                for (ContactDetail detail : contact.details) {
                    LogUtils.logD("Contact In: " + contact.contactID + ", Detail: " + detail.key + ", "
                            + detail.unique_id + ", " + detail.keyType + " = " + detail.value + ", del = "
                            + detail.deleted);
                }
            }
            if (findIdInOrderedList(contact.contactID, mOrderedServerIdList) > -1) {
                mContactsChangedList.add(contact);
                
            } else {
                if (contact.deleted != null && contact.deleted.booleanValue()) {
                    if (EXTRA_DEBUG_TRACE) {
                        LogUtils.logD("DownloadServerContacts.syncDownContact() "
                                + "Delete Contact (nothing to do) " + contact.contactID);
                    }
                } else if (contact.details.size() > 0) {
                    if (EXTRA_DEBUG_TRACE) {
                        LogUtils.logD("DownloadServerContacts.syncDownContact() Adding "
                                + contact.contactID);
                    }
                    mAddContactList.add(contact);
                    mSyncDataPending = true;
                } else {
                    if (EXTRA_DEBUG_TRACE) {
                        LogUtils.logD("DownloadServerContacts.syncDownContact() Empty "
                                + contact.contactID);
                    }
                }
            }
        }
        if (mSyncDataPending || mContactsChangedList.size() > 0) {
            setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Processes next page of modified contacts in the
     * {@link #mContactsChangedList}.
     */
    private void processContactChangesNextPage() {
        final int count = Math.min(mContactsChangedList.size(), MAX_CONTACT_CHANGES_PER_PAGE);
        for (int i = 0; i < count; i++) {
            final Contact srcContact = mContactsChangedList.get(0);
            mContactsChangedList.remove(0);
            final Contact destContact = new Contact();
            long startTime = System.nanoTime();
            ServiceStatus status = mDb.fetchContactByServerId(srcContact.contactID, destContact);
            if (ServiceStatus.SUCCESS != status) {
                LogUtils
                        .logE("DownloadServerContacts.processContactChangesNextPage() - Error syncing page: "
                                + status);
                complete(status);
                return;
            }
            mDbSyncTime += (System.nanoTime() - startTime);
            status = syncDownContact(srcContact, destContact);
            if (ServiceStatus.SUCCESS != status) {
                LogUtils
                        .logE("DownloadServerContacts.processContactChangesNextPage() - Error syncing page: "
                                + status);
                complete(status);
                return;
            }
        }
        if (mContactsChangedList.size() == 0 && !mSyncDataPending) {
            if (mIsComplete) {
                downloadSyncSuccessful();
            }
            return;
        }

        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
    }

    /**
     * Modifies a contact from the NowPlus database to match the one received
     * from the server
     * 
     * @param srcContact Contact received from the server
     * @param destContact Contact as stored in the database
     * @return SUCCESS or a suitable error code.
     */
    private ServiceStatus syncDownContact(Contact srcContact, Contact destContact) {
        final long localContactId = destContact.localContactID;
        final Integer nativeContactId = destContact.nativeContactId;
        if (srcContact.deleted != null && srcContact.deleted.booleanValue()) {
            if (EXTRA_DEBUG_TRACE) {
                LogUtils.logD("DownloadServerContacts.syncDownContact - Deleting contact "
                        + srcContact.contactID);
            }
            ContactsTable.ContactIdInfo idInfo = new ContactsTable.ContactIdInfo();
            idInfo.localId = localContactId;
            idInfo.serverId = srcContact.contactID;
            idInfo.nativeId = nativeContactId;
            mDeleteContactList.add(idInfo);
            mSyncDataPending = true;
            return ServiceStatus.SUCCESS;
        }

        if (checkContactMods(srcContact, destContact)) {
            if (EXTRA_DEBUG_TRACE) {
                LogUtils.logD("DownloadServerContacts.syncDownContact - Modifying contact "
                        + srcContact.contactID);
            }
            srcContact.localContactID = localContactId;
            srcContact.nativeContactId = nativeContactId;
            mModifyContactList.add(srcContact);
            mSyncDataPending = true;
        }

        if (srcContact.details == null) {
            if (EXTRA_DEBUG_TRACE) {
                LogUtils.logD("DownloadServerContacts.syncDownContact - No details changed "
                        + srcContact.contactID);
            }
            return ServiceStatus.SUCCESS;
        }

        for (ContactDetail srcDetail : srcContact.details) {
            srcDetail.localContactID = localContactId;
            srcDetail.nativeContactId = nativeContactId;
            boolean detailFound = false;
            for (ContactDetail destDetail : destContact.details) {
                if (DatabaseHelper.doDetailsMatch(srcDetail, destDetail)) {
                    detailFound = true;
                    srcDetail.localDetailID = destDetail.localDetailID;
                    srcDetail.nativeContactId = destDetail.nativeContactId;
                    srcDetail.nativeDetailId = destDetail.nativeDetailId;
                    srcDetail.serverContactId = srcContact.contactID;
                    if (srcDetail.deleted != null && srcDetail.deleted.booleanValue()) {
                        if (EXTRA_DEBUG_TRACE) {
                            LogUtils
                                    .logD("DownloadServerContacts.syncDownContact - Deleting detail "
                                            + srcDetail.key
                                            + " contact ID = "
                                            + srcContact.contactID);
                        }
                        mDeleteDetailList.add(srcDetail);
                        mSyncDataPending = true;
                    } else if (DatabaseHelper.hasDetailChanged(destDetail, srcDetail)) {
                        if (EXTRA_DEBUG_TRACE) {
                            LogUtils
                                    .logD("DownloadServerContacts.syncDownContact - Modifying detail "
                                            + srcDetail.key
                                            + " local detail ID = "
                                            + srcDetail.localDetailID);
                        }
                        srcDetail.serverContactId = srcContact.contactID;
                        mModifyDetailList.add(srcDetail);
                        mSyncDataPending = true;
                    }
                    break;
                }
            }
            if (!detailFound) {
                if (srcDetail.deleted == null || !srcDetail.deleted.booleanValue()) {
                    if (EXTRA_DEBUG_TRACE) {
                        LogUtils
                                .logD("DownloadServerContacts.syncDownContact - Adding detail "
                                        + srcDetail.key + " local contact ID = "
                                        + srcDetail.localContactID);
                    }
                    mAddDetailList.add(srcDetail);
                    mSyncDataPending = true;
                } else {
                    if (EXTRA_DEBUG_TRACE) {
                        LogUtils
                                .logD("DownloadServerContacts.syncDownContact - Detail already deleted (nothing to do) "
                                        + srcDetail.key
                                        + " local detail ID = "
                                        + srcDetail.localDetailID);
                    }
                }
            }
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Binary search function to find a contact server ID in an ordered list.
     * 
     * @param id The server ID to find
     * @param list An ordered list of server IDs from the database
     * @return index of server ID if found, otherwise -1.
     */
    private static int findIdInOrderedList(long id, List<Long> list) {
        // Binary search through list
        if (list.size() == 0) {
            return -1;
        }
        int i = 0;
        int j = list.size() - 1;
        while (i < j) {
            int m = (i + j) >> 1;
            if (id > list.get(m).longValue()) {
                i = m + 1;
            } else {
                j = m;
            }
        }
        if (id == list.get(i).longValue()) {
            for (; i >= 0; i--) {
                if (id != list.get(i).longValue()) {
                    return i + 1;
                }
            }
            return 0;
        }
        return -1;
    }

    /**
     * Compares two contacts to check for any differences. Implemented to avoid
     * updating the database when nothing has changed (to improve performance).
     * 
     * @param srcContact Contact received from the server
     * @param destContact Contact as it is in the database.
     * @return true if the contacts are different, false otherwise.
     */
    private boolean checkContactMods(Contact srcContact, Contact destContact) {
        boolean contactModified = false;
        if (srcContact.friendOfMine != null
                && !srcContact.friendOfMine.equals(destContact.friendOfMine)) {
            destContact.friendOfMine = srcContact.friendOfMine;
            contactModified = true;
        }
        if (srcContact.gender != null && !srcContact.gender.equals(destContact.gender)) {
            destContact.gender = srcContact.gender;
            contactModified = true;
        }
        if (srcContact.sources != null) {
            boolean changed = false;
            if (srcContact.sources.size() != destContact.sources.size()) {
                changed = true;
            } else {
                for (int i = 0; i < srcContact.sources.size(); i++) {
                    if (srcContact.sources.get(i) != null
                            && !srcContact.sources.get(i).equals(destContact.sources.get(i))) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                destContact.sources.clear();
                destContact.sources.addAll(srcContact.sources);
                contactModified = true;
            }
        }
        if (srcContact.userID != null && !srcContact.userID.equals(destContact.userID)) {
            destContact.userID = srcContact.userID;
            contactModified = true;
        }
        if (srcContact.aboutMe != null && !srcContact.aboutMe.equals(destContact.aboutMe)) {
            destContact.aboutMe = srcContact.aboutMe;
            contactModified = true;
        }
        if (srcContact.groupList != null) {
            boolean changed = false;
            if (srcContact.groupList.size() != destContact.groupList.size()) {
                changed = true;
            } else {
                for (int i = 0; i < srcContact.groupList.size(); i++) {
                    if (srcContact.groupList.get(i) != null
                            && !srcContact.groupList.get(i).equals(destContact.groupList.get(i))) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                destContact.groupList.clear();
                destContact.groupList.addAll(srcContact.groupList);
                contactModified = true;
            }
        }
        return contactModified;
    }

    /**
     * Called by framework when a timeout expires. Timeouts are used by this
     * processor to break up lengthy database update tasks. A timeout of 0 is
     * normally used just to give other engines a chance to run.
     */
    public void onTimeoutEvent() {
        switch (mInternalState) {
            case FETCHING_SERVER_ID_LIST:
                ServiceStatus status = fetchFirstBatch();
                if (ServiceStatus.SUCCESS != status) {
                    complete(status);
                    return;
                }
                break;
            case FETCHING_FIRST_PAGE:
            case FETCHING_NEXT_BATCH:
                if (mContactsChangedList.size() > 0) {
                    processContactChangesNextPage();
                    return;
                }
                if (mSyncDataPending) {
                    if (addContactList()) {
                        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
                        return;
                    }
                    if (modifyContactList()) {
                        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
                        return;
                    }
                    if (deleteContactList()) {
                        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
                        return;
                    }
                    if (addDetailList()) {
                        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
                        return;
                    }
                    if (modifyDetailList()) {
                        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
                        return;
                    }
                    if (deleteDetailList()) {
                        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
                        return;
                    }

                    mSyncDataPending = false;
                    if (mIsComplete) {
                        downloadSyncSuccessful();
                        return;
                    }
                }
                break;
            default:
                // do nothing.
                break;
        }
    }

    /**
     * Adds contacts received from the server (listed in the
     * {@link #mAddContactList} list) into the local database.
     * 
     * @return true if > 0 contacts were added, false otherwise.
     */
    private boolean addContactList() {
        if (mAddContactList.size() == 0) {
            return false;
        }
        LogUtils.logI("DownloadServerContacts.addContactList " + mAddContactList.size()
                + " contacts...");
        long startTime = System.nanoTime();
        ServiceStatus status = mDb.syncAddContactList(mAddContactList, false, true);
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
            return true;
        }
        markDbChanged();
        long timeDiff = System.nanoTime() - startTime;
        mDbSyncTime += timeDiff;
        LogUtils.logI("DownloadServerContacts.addContactList - time = "
                + (timeDiff / NANOSECONDS_IN_MS) + "ms, total = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms");
        setTimeout(TIMEOUT_BETWEEN_PAGES_MS);
        mTotalContactsAdded += mAddContactList.size();
        mAddContactList.clear();
        return true;
    }

    /**
     * Updates the local database with contacts received from the server (listed
     * in the {@link #mModifyContactList} list).
     * 
     * @return true if > 0 contacts were modified, false otherwise.
     */
    private boolean modifyContactList() {
        if (mModifyContactList.size() == 0) {
            return false;
        }
        LogUtils.logI("DownloadServerContacts.modifyContactList " + mModifyContactList.size()
                + " contacts...");
        long startTime = System.nanoTime();
        ServiceStatus status = mDb.syncModifyContactList(mModifyContactList, false, true);
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
            return true;
        }
        markDbChanged();
        long timeDiff = System.nanoTime() - startTime;
        mDbSyncTime += timeDiff;
        LogUtils.logI("DownloadServerContacts.modifyContactList - time = "
                + (timeDiff / NANOSECONDS_IN_MS) + "ms, total = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms");
        mModifyContactList.clear();
        return true;
    }

    /**
     * Deletes contacts from the local database based on changes received from
     * the server (listed in the {@link #mDeleteContactList} list).
     * 
     * @return true if > 0 contacts were deleted, false otherwise.
     */
    private boolean deleteContactList() {
        if (mDeleteContactList.size() == 0) {
            return false;
        }
        LogUtils.logI("DownloadServerContacts.deleteContactList " + mDeleteContactList.size()
                + " contacts...");
        long startTime = System.nanoTime();
        ServiceStatus status = mDb.syncDeleteContactList(mDeleteContactList, false, true);
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
            return true;
        }
        markDbChanged();
        long timeDiff = System.nanoTime() - startTime;
        mDbSyncTime += timeDiff;
        LogUtils.logI("DownloadServerContacts.deleteContactList - time = "
                + (timeDiff / NANOSECONDS_IN_MS) + "ms, total = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms");
        mDeleteContactList.clear();
        return true;
    }

    /**
     * Adds contact details to the local database based on changes received from
     * the server (listed in the {@link #mAddDetailList} list).
     * 
     * @return true if > 0 contact details were added, false otherwise.
     */
    private boolean addDetailList() {
        if (mAddDetailList.size() == 0) {
            return false;
        }

        LogUtils.logI("DownloadServerContacts.addDetailList " + mAddDetailList.size()
                + " contact details...");
        long startTime = System.nanoTime();
        ServiceStatus status = mDb.syncAddContactDetailList(mAddDetailList, false, true);
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
            return true;
        }
        markDbChanged();
        long timeDiff = System.nanoTime() - startTime;
        mDbSyncTime += timeDiff;
        LogUtils.logI("DownloadServerContacts.addDetailList - time = "
                + (timeDiff / NANOSECONDS_IN_MS) + "ms, total = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms");
        mAddDetailList.clear();
        return true;
    }

    /**
     * Modifies contact details in the local database based on changes received
     * from the server (listed in the {@link #mModifyDetailList} list).
     * 
     * @return true if > 0 contact details were modified, false otherwise.
     */
    private boolean modifyDetailList() {
        if (mModifyDetailList.size() == 0) {
            return false;
        }

        LogUtils.logI("DownloadServerContacts.modifyDetailList " + mModifyDetailList.size()
                + " contact details...");
        long startTime = System.nanoTime();
        ServiceStatus status = mDb.syncModifyContactDetailList(mModifyDetailList, false, true);
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
            return true;
        }
        markDbChanged();
        long timeDiff = System.nanoTime() - startTime;
        mDbSyncTime += timeDiff;
        LogUtils.logI("DownloadServerContacts.modifyDetailList - time = "
                + (timeDiff / NANOSECONDS_IN_MS) + "ms, total = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms");
        mModifyDetailList.clear();
        return true;
    }

    /**
     * Deletes contact details from the local database based on changes received
     * from the server (listed in the {@link #mDeleteDetailList} list).
     * 
     * @return true if > 0 contact details were deleted, false otherwise.
     */
    private boolean deleteDetailList() {
        if (mDeleteDetailList.size() == 0) {
            return false;
        }
        LogUtils.logI("DownloadServerContacts.deleteDetailList " + mDeleteDetailList.size()
                + " contact details...");
        long startTime = System.nanoTime();
        ServiceStatus status = mDb.syncDeleteContactDetailList(mDeleteDetailList, false, true);
        if (ServiceStatus.SUCCESS != status) {
            complete(status);
            return true;
        }
        markDbChanged();
        long timeDiff = System.nanoTime() - startTime;
        mDbSyncTime += timeDiff;
        LogUtils.logI("DownloadServerContacts.deleteDetailList - time = "
                + (timeDiff / NANOSECONDS_IN_MS) + "ms, total = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms");
        mDeleteDetailList.clear();
        return true;
    }

    /**
     * Called when the processor has finished downloading the contact changes.
     * Notifies the contact sync engine.
     */
    private void downloadSyncSuccessful() {
        LogUtils.logI("DownloadServerContacts.downloadSyncSuccessful() - Total DB access time = "
                + (mDbSyncTime / NANOSECONDS_IN_MS) + "ms, no of contacts added = "
                + mTotalContactsAdded);
        StateTable.modifyContactRevision(mToRevision, mDb.getWritableDatabase());
        complete(ServiceStatus.SUCCESS);
    }
}
