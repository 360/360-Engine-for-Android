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

import java.security.InvalidParameterException;

import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.service.ServiceStatus;

/**
 * Observer interface used within Contact sync engine to allow processor to
 * report status back to the main Contact sync engine.
 */
public interface IContactSyncCallback {
    /**
     * Called by processor when it has finished running.
     * 
     * @param status SUCCESS or an error code from the processor
     * @param failureList A description of any errors that occurred during the
     *            sync or an empty string.
     * @param data Allows processors to send custom data to the contact sync
     *            engine.
     */
    void onProcessorComplete(ServiceStatus status, String failureList, Object data);

    /**
     * Called by processor when a change is made to the local database.
     */
    void onDatabaseChanged();

    /**
     * Called by processor to fetch the contact sync engine (needed for sending
     * requests to the server).
     * 
     * @return The contact sync engine reference.
     */
    BaseEngine getEngine();

    /**
     * Used by processors to set a timeout. The
     * {@link BaseSyncProcessor#onTimeoutEvent()} function will be called by the
     * contact sync engine when the timeout event occurs.
     * 
     * @param timeout Timeout based on current time in milliseconds.
     */
    void setTimeout(long timeout);
   
    /**
     * Used by processors to provide an indication of their current progress.
     * 
     * @param SyncStatus Status of the processor, must not be NULL.
     * @throws InvalidParameterException when SyncStatus is NULL.
     */
    void setSyncStatus(final SyncStatus syncStatus);

    /**
     * Used when a processor sends a request to the server to notify the
     * {@link BaseEngine} class of the request ID. If used, the
     * {@link BaseEngine} will only send responses which match the request.
     * 
     * @param reqId Request ID received from one of the API functions.
     */
    void setActiveRequestId(int reqId);
}
