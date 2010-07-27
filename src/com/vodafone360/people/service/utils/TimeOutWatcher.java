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

package com.vodafone360.people.service.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.utils.LogUtils;

/**
 * TimeOutWatcher utility... This is a utility class that is intended to
 * dispatch time-outs for each sent request individually. The thread should be
 * managing only the requests which can expire, i.e. don't add request with
 * timeout "-1" TODO: Consider using the WorkerThread to check for timeouts,
 * that would save us one thread! !!! Also ResponseQueue, RequestQueue and
 * TimeOutWatcher should be managed together and used via a common mutex !!!
 */
public class TimeOutWatcher implements Runnable {

    /**
     * Flag to determine whether or not the watcher is running.
     */
    private boolean mIsRunning = false;

    /**
     * The list of watched requests sorted by their expiry dates in ascending
     * order.
     */
    private LinkedList<Request> mRequests;

    /**
     * The thread watching for timed out requests.
     */
    private Thread mThread;

    /**
     * Constructor.
     */
    public TimeOutWatcher() {
        LogUtils.logV("TimeOutWatcher.TimeOutWatcher() => TimeOutWatcher constructor called.");
    }

    /**
     * TimeOutWatcher thread that checks for the next time to run, sleeps when
     * not busy and sends timeouts.
     */
    @Override
    public void run() {

        long nextRuntime, currentTime;

        // need to synchronize it all but calling wait() will release the lock
        synchronized (QueueManager.getInstance().lock) {
            while (mIsRunning) {
                nextRuntime = getNextRuntime();
                LogUtils.logV("TimeOutWatcher.run(): nextRuntime=" + nextRuntime);
                if (nextRuntime < 0) {
                    // nothing to watch at the moment, let's wait for a
                    // notification for "synchronized(this)"
                    try {
                        LogUtils.logV("TimeOutWatcher.run(): nothing to watch, calling wait()");
                        QueueManager.getInstance().lock.wait();
                    } catch (InterruptedException e) {
                        LogUtils.logW("TimeOutWatcher.run(): "
                                + "InterruptedException whithin this.wait() => " + e);
                    }
                } else {
                    // one or more requests need to be watched
                    currentTime = System.currentTimeMillis();
                    nextRuntime = nextRuntime - currentTime;
                    if (nextRuntime > 0) {
                        // no request timed out yet, let's wait for the next
                        // timeout
                        try {
                            LogUtils.logV("TimeOutWatcher.run(): no time out yet, calling wait("
                                    + nextRuntime + ")");
                            QueueManager.getInstance().lock.wait(nextRuntime);
                        } catch (InterruptedException e) {
                            LogUtils.logW("TimeOutWatcher.run(): "
                                    + "InterruptedException within this.wait(nextRuntime) => " + e);
                        }
                    } else {
                        // one or more request have timed out, send them a time
                        // out event
                        sendTimeoutEvent(currentTime);
                    }
                }
            }
            mThread = null;
        }
    }

    /**
     * Starts the TimeOutWatcher thread and performs initialization. Note: this
     * method shall be called within synchronized(this) block.
     */
    private void startThread() {
        mIsRunning = true;
        mRequests = new LinkedList<Request>();
        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * Stops the TimeOutWatcher thread and releases the memory. Note: this
     * method shall be called within synchronized(this) block.
     */
    private void stopThread() {

        if (mIsRunning) {
            mRequests.clear();
            mRequests = null;
            // let the thread die
            mIsRunning = false;
            QueueManager.getInstance().lock.notify();
        }
    }

    /**
     * Finds the closest time to perform a new check on timeouts.
     * 
     * @return the next time when a timeout check is needed, -1 if no nothing to
     *         perform
     */
    private long getNextRuntime() {
        // just get the first one as the list of requests is sorted
        final Request request = mRequests.peek();

        if (request == null) {
            return -1;
        }
        return request.getExpiryDate();
    }

    /**
     * Sends a timeout event for all the expired requests.
     * 
     * @param currentTime the current time until when a timeout event needs to
     *            be sent
     */
    private void sendTimeoutEvent(long currentTime) {
        LogUtils.logV("TimeOutWatcher.sendTimeoutEvent(" + currentTime + ")");

        while (mRequests.size() > 0) {
            final Request request = mRequests.get(0);
            if (request.getExpiryDate() <= currentTime) {
                LogUtils.logW("TimeOutWatcher.sendTimeoutEvent(): "
                        + "Expired request found with reqId=[" + request.getRequestId()
                        + "], type=["+request.mType+"] and timeout=" + request.getTimeout() + " milliseconds");
                fireRequestExpired(request);
                // no need to remove the request, this happened during previous
                // method call... (removeRequest is called when adding a
                // response)
            } else {
                // the list is ordered by expiry date, no need to check the rest
                // of it
                break;
            }
        }
    }

    /**
     * Inserts a request in the requests list while maintaining it sorted by
     * ascending order of expiry date.
     * 
     * @param request the request to insert
     */
    private int insertRequestByExpiryDate(Request request) {
        for (int i = 0; i < mRequests.size(); i++) {
            final Request currentRequest = mRequests.get(i);
            if (currentRequest.getExpiryDate() > request.getExpiryDate()) {
                // add the request before the current one
                mRequests.add(i, request);
                return i;
            }
        }
        // the request is either the first one or the last one
        mRequests.add(request);
        if (mRequests.size() == 1) {
            // was the only one
            return 0;
        }
        // was added at the end
        return mRequests.size() - 1;
    }

    /**
     * Creates a TimeOut event and adds it to the response queue. FIXME: this
     * assumes that adding a request to the response queue will trigger a
     * synchronous removeRequest() call with the same thread.
     * 
     * @param request the request that has timed out
     */
    private void fireRequestExpired(Request request) {
        // create a list with a server error containing a timeout
        final List<BaseDataType> data = new ArrayList<BaseDataType>(1);
        final ServerError timeoutError = new ServerError(ServerError.ErrorType.REQUEST_TIMEOUT);
        timeoutError.errorDescription = "TimeOutWatcher detected that the request id=["
                + request.getRequestId() + "] has timed out.";
        data.add(timeoutError);
        // set the request as expired
        request.expired = true;
        // add the timeout error to the response queue
        LogUtils.logW("TimeOutWatcher.fireRequestExpired(): "
                + "adding a timeout error to the response queue for reqId=["
                + request.getRequestId() + "]");
        QueueManager.getInstance().addResponse(
                new DecodedResponse(request.getRequestId(), data, request.mEngineId, DecodedResponse.ResponseType.SERVER_ERROR.ordinal()));
    }

    /**
     * Adds a request to be watched for timeouts. Note: it is assumed that the
     * request expiry date is calculated.
     * 
     * @param request the request to add
     */
    public void addRequest(Request request) {
        synchronized (QueueManager.getInstance().lock) {
            // make sure to add requests with a valid timeout
            if ((request != null) && (request.getExpiryDate() >= 0)) {
                if (!mIsRunning) {
                    // start the thread if not already existing
                    startThread();
                }
                // insert the request in the sorted requests list
                final int index = insertRequestByExpiryDate(request);
                // check if request is added in front of the others. If not, no
                // need to notify, we can still sleep!
                if (index == 0) {
                    LogUtils.logV("TimeOutWatcher.addRequest(): wake up the thread");
                    QueueManager.getInstance().lock.notify();
                }
            }
        }
    }

    /**
     * Removes a request from being watched for timeouts.
     * 
     * @param request the request to remove
     */
    public void removeRequest(Request request) {

        // The TimeOutWatcher is not initialized or has been stopped already,
        // just ignore the request
        if (!mIsRunning)
            return;

        synchronized (QueueManager.getInstance().lock) {
            if ((request != null) && (request.getExpiryDate() >= 0) && mRequests != null) {
                int index = -1;
                for (int i = 0; i < mRequests.size(); i++) {
                    final Request currentRequest = mRequests.get(i);
                    if (currentRequest == request) {
                        index = i;
                        mRequests.remove(i);
                        break;
                    }
                }
                // check if the first request was removed. If not, no need to
                // notify, we can still sleep!
                if (index == 0) {
                    LogUtils.logV("TimeOutWatcher.removeRequest(): wake up the thread");
                    QueueManager.getInstance().lock.notify();
                }
            }
        }
    }

    /**
     * Kills the TimeOutWatcher (releases memory and running thread).
     */
    public void kill() {
        synchronized (QueueManager.getInstance().lock) {
            stopThread();
        }
    }

    /**
     * Sends a timeout event for all the requests.
     */
    public void invalidateAllRequests() {
        synchronized (QueueManager.getInstance().lock) {
            LogUtils.logV("TimeOutWatcher.invalidateAllRequests()");

            if (mRequests == null)
                return;

            while (mRequests.size() > 0) {
                final Request request = mRequests.get(0);
                LogUtils.logV("TimeOutWatcher.invalidateAllRequests(): "
                        + "forcing a timeout for reqId=[" + request.getRequestId()
                        + "] and timeout=" + request.getTimeout() + " milliseconds");
                fireRequestExpired(request);
                // no need to remove the request, this happened during previous
                // method call... (removeRequest is called when adding a
                // response)
            }
        }
    }

    // ////////////////
    // TEST METHODS //
    // ////////////////

    // TODO: should have package access only but requires to modify the test
    // package to have the same package name

    /**
     * Gets the current number of requests being watched for timeouts.
     */
    public int getRequestsCount() {
        synchronized (QueueManager.getInstance().lock) {
            if (mRequests != null) {
                return mRequests.size();
            }

            return 0;
        }
    }

    /**
     * Gets an array containing all the requests being watched for timeouts.
     * 
     * @return array containing all requests, NULL if list of requests
     *         maintained internally is NLL or empty.
     */
    public Request[] getRequestsArray() {
        synchronized (QueueManager.getInstance().lock) {
            if (mRequests != null && mRequests.size() > 0) {
                final Request[] requests = new Request[mRequests.size()];
                return mRequests.toArray(requests);
            }

            return null;
        }
    }
}
