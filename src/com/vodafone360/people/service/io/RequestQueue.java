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

package com.vodafone360.people.service.io;

import java.util.ArrayList;
import java.util.List;

import com.vodafone360.people.Settings;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.transport.IQueueListener;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.TimeOutWatcher;
import com.vodafone360.people.utils.LogUtils;

/**
 * Holds a queue of outgoing requests. The Requester adds Requests to the queue.
 * The transport layer gets one or more items from the queue when it is ready to
 * send more requests to the server. When a Request is added a request id is
 * generated for this request. Requests are removed from the queue on completion
 * or if an error requires us to clear any outstanding requests.
 */
public class RequestQueue {
    private final static int MILLIS_PER_SECOND = 1000;

    /**
     * The queue data, a List-array of Request items.
     */
    private final List<Request> mRequests = new ArrayList<Request>();

    /**
     * A unique ID identifying this request
     */
    private volatile int mCurrentRequestId;

    /**
     * Contains a list of listeners that will receive events when items are
     * added to the queue.
     */
    private final List<IQueueListener> mListeners = new ArrayList<IQueueListener>();

    private TimeOutWatcher mTimeOutWatcher;

    /**
     * Constructs the request queue
     */
    protected RequestQueue() {
        // Generate initial request ID based on current timestamp.
        mCurrentRequestId = (int)(System.currentTimeMillis() / MILLIS_PER_SECOND);
        mTimeOutWatcher = new TimeOutWatcher();
    }

    /**
     * Get instance of RequestQueue - we only have a single instance. If the
     * instance of RequestQueue does not yet exist it is created.
     * 
     * @return Instance of RequestQueue.
     */
    protected static RequestQueue getInstance() {
        return RequestQueueHolder.rQueue;
    }

    /**
     * Use Initialization on demand holder pattern
     */
    private static class RequestQueueHolder {
        private static final RequestQueue rQueue = new RequestQueue();
    }

    /**
     * Add listener listening for RequestQueue changes. Events are sent when
     * items are added to the queue (or in the case of batching when the last
     * item is added to the queue).
     * 
     * @param listener listener to add
     */
	protected void addQueueListener(IQueueListener listener) {
		LogUtils.logW("RequestQueue.addQueueListener() listener[" + listener
				+ "]");
		synchronized (mListeners) {
			if (mListeners != null) {
				mListeners.add(listener);
			}
		}
	}

    /**
     * Remove RequestQueue listener
     * 
     * @param listener listener to remove
     */
	protected void removeQueueListener(IQueueListener listener) {
		LogUtils.logW("RequestQueue.removeQueueListener() listener[" + listener
				+ "]");
		synchronized (mListeners) {
			if (mListeners != null) {
				mListeners.remove(listener);
			}
		}
	}

    /**
     * Fire RequestQueue state changed message
     */
	protected void fireQueueStateChanged() {
		synchronized (mListeners) {
			LogUtils.logW("RequestQueue.notifyOfItemInRequestQueue() listener["
					+ mListeners + "]");
			for (IQueueListener listener : mListeners) {
				listener.notifyOfItemInRequestQueue();
			}
		}
	}

    /**
     * Add request to queue
     * 
     * @param req Request to add to queue
     * @return request id of new request
     */
    protected int addRequestAndNotify(Request req) {
        synchronized (QueueManager.getInstance().lock) {
            int ret = addRequest(req);
            fireQueueStateChanged();
            return ret;
        }
    }

    /**
     * Adds a request to the queue without sending an event to the listeners
     * 
     * @param req The request to add
     * @return The unique request ID TODO: What is with the method naming
     *         convention?
     */
    protected int addRequest(Request req) {
        synchronized (QueueManager.getInstance().lock) {
            mCurrentRequestId++;
            req.setRequestId(mCurrentRequestId);
            mRequests.add(req);
            // add the request to the watcher thread
            if (req.getTimeout() > 0 && (!req.isFireAndForget())) {
                // TODO: maybe the expiry date should be calculated when the
                // request is actually sent?
                req.calculateExpiryDate();
                mTimeOutWatcher.addRequest(req);
            }
            
            HttpConnectionThread.logV("RequestQueue.addRequest", "Adding request to queue:\n" + req.toString());

            return mCurrentRequestId;
        }
    }

    /**
     * Adds requests to the queue.
     * 
     * @param requests The requests to add.
     * @return The request IDs generated in an integer array or null if the
     *         requests array was null. Returns NULL id requests[] is NULL.
     */
    protected int[] addRequest(Request[] requests) {
        synchronized (QueueManager.getInstance().lock) {
            if (null == requests) {
                return null;
            }

            int[] requestIds = new int[requests.length];

            for (int i = 0; i < requests.length; i++) {
                requestIds[i] = addRequest(requests[i]);
            }

            return requestIds;
        }
    }

    /*
     * Get number of items currently in the list of requests
     * @return number of request items
     */
    private int requestCount() {
        return mRequests.size();
    }

    /**
     * Returns all requests from the queue. Regardless if they need to
     * 
     * @return List of all requests.
     */
    protected List<Request> getAllRequests() {
        synchronized (QueueManager.getInstance().lock) {
            return mRequests;
        }
    }

    /**
     * Returns all requests from the queue needing the API or both to work.
     * 
     * @return List of all requests needing the API or both (API or RPG) to
     *         function properly.
     */
    protected List<Request> getApiRequests() {
        synchronized (QueueManager.getInstance().lock) {
            return this.getRequests(false);
        }
    }

    /**
     * Returns all requests from the queue needing the RPG or both to work.
     * 
     * @return List of all requests needing the RPG or both (API or RPG) to
     *         function properly.
     */
    protected List<Request> getRpgRequests() {
        return this.getRequests(true);
    }

    /**
     * Returns a list of either requests needing user authentication or requests
     * not needing user authentication depending on the flag passed to this
     * method.
     * 
     * @param needsUserAuthentication If true only requests that need to have a
     *            valid user authentication will be returned. Otherwise methods
     *            requiring application authentication will be returned.
     * @return A list of requests with the need for application authentication
     *         or user authentication.
     */
    private List<Request> getRequests(boolean needsRpgForRequest) {
        synchronized (QueueManager.getInstance().lock) {
            List<Request> requests = new ArrayList<Request>();
            if (null == mRequests) {
                return requests;
            }

            Request request = null;
            for (int i = 0; i < mRequests.size(); i++) {
                request = mRequests.get(i);
                if ((null == request) || (request.isActive())) {
                    LogUtils.logD("Skipping active or null request in request queue.");
                    continue;
                }

                HttpConnectionThread.logD("RequestQueu.getRequests()",
                        "Request Auth Type (USE_API=1, USE_RPG=2, USE_BOTH=3): "
                                + request.getAuthenticationType());
                // all api and rpg requests
                if (request.getAuthenticationType() == Request.USE_BOTH) {
                    requests.add(request);
                } else if ((!needsRpgForRequest)
                        && (request.getAuthenticationType() == Request.USE_API)) {
                    requests.add(request);
                } else if ((needsRpgForRequest)
                        && (request.getAuthenticationType() == Request.USE_RPG)) {
                    requests.add(request); // all rpg requests
                }
            }

            return requests;
        }
    }

    /**
     * Return Request from specified request ID. Only used for unit tests.
     * 
     * @param requestId Request Id of required request
     * @return Request with or null if request does not exist
     */
    protected Request getRequest(int requestId) {
        Request req = null;
        int reqCount = requestCount();
        for (int i = 0; i < reqCount; i++) {
            Request tmp = mRequests.get(i);
            if (tmp.getRequestId() == requestId) {
                req = tmp;
                break;
            }
        }
        return req;
    }
    
    /**
     * Removes the request for the given response (request) ID from the queue and searches
     * the queue for requests older than
     * Settings.REMOVE_REQUEST_FROM_QUEUE_MILLIS and removes them as well.
     * 
     * @param responseId The response object id.
     * @return Returns the removed request, can be null if the request was not found.
     */
    protected Request removeRequest(int responseId) {
        synchronized (QueueManager.getInstance().lock) {

            for (int i = 0; i < requestCount(); i++) {
                Request request = mRequests.get(i);
                // the request we were looking for
                if (request.getRequestId() == responseId) {
                    // reassure the engine id is set (important for SystemNotifications) 
                    mRequests.remove(i--);

                    // remove the request from the watcher (the request not
                    // necessarily times out before)
                    if (request.getExpiryDate() > 0) {
                        mTimeOutWatcher.removeRequest(request);
                        LogUtils
                                .logV("RequestQueue.removeRequest() Request expired after ["
                                        + (System.currentTimeMillis() - request.getAuthTimestamp())
                                        + "ms]");
                    } else {
                        LogUtils
                                .logV("RequestQueue.removeRequest() Request took ["
                                        + (System.currentTimeMillis() - request.getAuthTimestamp())
                                        + "ms]");
                    }

                    return request;
                } else if ((System.currentTimeMillis() - request.getCreationTimestamp()) > Settings.REMOVE_REQUEST_FROM_QUEUE_MILLIS) { // request
                    // is older than 15 minutes
                    mRequests.remove(i--);

                    ResponseQueue.getInstance().addToResponseQueue(new DecodedResponse(request.getRequestId(), null, request.mEngineId, 
                            DecodedResponse.ResponseType.TIMED_OUT_RESPONSE.ordinal()));

                    // remove the request from the watcher (the request not
                    // necessarily times out before)
                    if (request.getExpiryDate() > 0) {
                        mTimeOutWatcher.removeRequest(request);
                        LogUtils
                                .logV("RequestQueue.removeRequest() Request expired after ["
                                        + (System.currentTimeMillis() - request.getAuthTimestamp())
                                        + "ms]");
                    } else {
                        LogUtils
                                .logV("RequestQueue.removeRequest() Request took ["
                                        + (System.currentTimeMillis() - request.getAuthTimestamp())
                                        + "ms]");
                    }
                }

            }

            return null;
        }
    }

    
    
    /**
     * Return the current (i.e. most recently generated) request id.
     * 
     * @return the current request id.
     */
    /*
     * public synchronized int getCurrentId(){ return mCurrentRequestId; }
     */

    /**
     * Clear active requests (i.e add dummy response to response queue).
     * 
     * @param rpgOnly
     */
    protected void clearActiveRequests(boolean rpgOnly) {
        synchronized (QueueManager.getInstance().lock) {
            ResponseQueue rQ = ResponseQueue.getInstance();

            for (int i = 0; i < mRequests.size(); i++) {
                Request request = mRequests.get(i);

                if (request.isActive() && (!rQ.responseExists(request.getRequestId()))) {
                    if (!rpgOnly
                            || (rpgOnly && ((request.getAuthenticationType() == Request.USE_RPG) || (request
                                    .getAuthenticationType() == Request.USE_BOTH)))) {
                        LogUtils.logE("RequestQueue.clearActiveRequests() Deleting request "
                                + request.getRequestId());
                        mRequests.remove(i);
                        // AA: I added the line below
                        // remove the request from the watcher (the request not
                        // necessarily times out before)
                        if (request.getExpiryDate() > 0) {
                            mTimeOutWatcher.removeRequest(request);
                        }
                        i--;
                        rQ.addToResponseQueue(new DecodedResponse(request.getRequestId(), null, 
                        		request.mEngineId, DecodedResponse.ResponseType.TIMED_OUT_RESPONSE.ordinal()));
                    }
                }
            }
        }
    }
    
    /**
     * Clears all requests from the queue and puts null responses on the
     * response queue to tell the engines that they have been cleared. This
     * should be called from the connection thread as soon as it is stopped.
     */
    protected void clearAllRequests() {
        synchronized (QueueManager.getInstance().lock) {
            ResponseQueue responseQueue = ResponseQueue.getInstance();

            for (int i = 0; i < mRequests.size(); i++) {
                Request request = mRequests.get(i);

                LogUtils.logE("RequestQueue.clearActiveRequests() Deleting request "
                        + request.getRequestId());
                mRequests.remove(i--);

                // remove the request from the watcher (the request not
                // necessarily times out before)
                if (request.getExpiryDate() > 0) {
                    mTimeOutWatcher.removeRequest(request);
                }

                responseQueue.addToResponseQueue(new DecodedResponse(request.getRequestId(), null, request.mEngineId, 
                		DecodedResponse.ResponseType.TIMED_OUT_RESPONSE.ordinal()));
            }
        }
    }

    /**
     * Return handle to TimeOutWatcher.
     * 
     * @return handle to TimeOutWatcher.
     */
    protected TimeOutWatcher getTimeoutWatcher() {
        return mTimeOutWatcher;
    }

    /**
     * Removes all items that are being watched for timeouts
     */
    protected void clearTheTimeOuts() {
        if (mTimeOutWatcher != null) {
            mTimeOutWatcher.kill();
        }
    }

    /**
     * Overrides the toString() method of Object and gives detailed infos which
     * objects are on the queue and whether they are active or not.
     */
    @Override
    public String toString() {
        if (null == mRequests) {
            return "";
        }

        final StringBuffer sb = new StringBuffer("Queue Size: "); 
        sb.append(mRequests.size());
        sb.append("; Request method-name [isActive]: ");

        for (int i = 0; i < mRequests.size(); i++) {
            Request request = mRequests.get(i);

            if (null == request) {
                sb.append("null request");
            } else {
                sb.append(request.getApiMethodName()); 
                sb.append(" ["); 
                sb.append(request.isActive()); 
                sb.append("]");
            }

            if (i < (mRequests.size() - 1)) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
}
