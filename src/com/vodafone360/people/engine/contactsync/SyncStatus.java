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

import com.vodafone360.people.service.ServiceStatus;

/**
 * In memory store for the current state of the Contacts sync engine.
 */
public class SyncStatus {

    /** Sync tasks, each of which corresponds to a specific processor. **/
    public enum Task {
        /** FetchNativeContacts is processing. */
        FETCH_NATIVE_CONTACTS,
        /** UploadServerContacts is processing. */
        UPDATE_SERVER_CONTACTS,
        /** DownloadServerContacts is processing. */
        DOWNLOAD_SERVER_CONTACTS,
        /** Last element is used to determine the size of the ENUM. **/
        UNKNOWN
    }
    /** Sync task status. **/
    public enum TaskStatus {
        /** Sent X of Y contacts. */
        SENT_CONTACTS,
        /** Received X of Y contacts. */
        RECEIVED_CONTACTS,
        /** Sent X of Y changes. */
        SENT_CHANGES,
        /** Do not show task status (i.e. leave blank). */
        NONE
    }

    /** ServiceStatus of sync outcome. **/
    private ServiceStatus mServiceStatus;

    /** Percentage of sync progress in current task (e.g. 53). **/
    private int mProgress;

    /** Current contact name (e.g. John Doe). **/
    private String mTextContact;

    /** Current task (e.g. Uploading server contacts). **/
    private Task mTask;

    /** Current task status (e.g. Sent 25 of 500 contacts). **/
    private TaskStatus mTaskStatus;

    /** Current task done (e.g. Sent X of 500 contacts). **/
    private int mTaskStatusDone;

    /** Current task total (e.g. Sent 25 of X contacts). **/
    private int mTaskStatusTotal;

    /**
     * Construct with only the ServiceStatus of the Contacts sync engine.
     *
     * @param serviceStatus ServiceStatus of sync outcome.
     */
    protected SyncStatus(final ServiceStatus serviceStatus) {
        mServiceStatus = serviceStatus;
    }

    /**
     * Construct with the current state of the Contacts sync engine.
     *
     * @param progress Percentage of sync progress in current task (e.g. 53).
     * @param textContact Current contact name (e.g. John Doe).
     * @param task Current task (e.g. Uploading server contacts).
     * @param taskStatus Current task status (e.g. Sent 25 of 500 contacts).
     * @param taskStatusDone Current task done (e.g. Sent X of 500 contacts).
     * @param taskStatusTotal Current task total (e.g. Sent 25 of X contacts).
     */
    public SyncStatus(final int progress,
            final String textContact, final Task task,
            final TaskStatus taskStatus, final int taskStatusDone,
            final int taskStatusTotal) {
        mProgress = progress;
        mTextContact = textContact;
        mTask = task;
        mTaskStatus = taskStatus;
        mTaskStatusDone = taskStatusDone;
        mTaskStatusTotal = taskStatusTotal;
    }

    /**
     * Construct with the current state of the Contacts sync engine, with the
     * task status set to TaskStatus.NONE.
     *
     * @param progress Percentage of sync progress in current task (e.g. 53).
     * @param textContact Current contact name (e.g. John Doe).
     * @param task Current task (e.g. Uploading server contacts).
     */
    public SyncStatus(final int progress, final String textContact,
            final Task task) {
        mProgress = progress;
        mTextContact = textContact;
        mTask = task;
        mTaskStatus = TaskStatus.NONE;
        mTaskStatusDone = 0;
        mTaskStatusTotal = 0;
    }

    /**
     * Gets the ServiceStatus of sync outcome.
     *
     * @return Sync outcome as a ServiceStatus object.
     */
    public final ServiceStatus getServiceStatus() {
        return mServiceStatus;
    }

    /**
     * Get the current sync progress percentage for the current task.
     *
     * @return Current sync progress percentage.
     */
    public final int getProgress() {
        return mProgress;
    }

    /**
     * Get the current contact name (e.g. John Doe).
     *
     * @return Current contact name.
     */
    public final String getTextContact() {
        return mTextContact;
    }

    /**
     * Get the current task (e.g. Uploading server contacts)
     *
     * @return Current task.
     */
    public final Task getTask() {
        return mTask;
    }

    /**
     * Get the current task status (e.g. Sent 25 of 500 contacts).
     *
     * @return Current task status.
     */
    public final TaskStatus getTaskStatus() {
        return mTaskStatus;
    }

    /**
     * Get the current task done (e.g. Sent X of 500 contacts).
     *
     * @return Current task done.
     */
    public final int getTaskStatusDone() {
        return mTaskStatusDone;
    }

    /**
     * Get the current task total (e.g. Sent 25 of X contacts).
     *
     * @return Current task total.
     */
    public final int getTaskStatusTotal() {
        return mTaskStatusTotal;
    }
}