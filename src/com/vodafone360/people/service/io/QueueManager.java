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

import java.util.List;

import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.transport.IQueueListener;
import com.vodafone360.people.service.utils.TimeOutWatcher;

/**
 * A facade class used for adding and removing from the request and response
 * queues. The methods used in this class are thread safe and should be used
 * instead of using the queues directly.
 * 
 * @author Rudy Norff (rudy.norff@vodafone.com)
 */
public class QueueManager {
    public final Object lock = new Object();


    private RequestQueue mRequestQueue;

    private ResponseQueue mResponseQueue;

    /**
     * Returns a single instance of the RequestResponseManager which holds
     * the request and response queues. Uses IDOH idiom.
     * 
     * @return The RequestResponseManager object to use for adding and removing
     *         requests from the request and response queues.
     */
    public static QueueManager getInstance() {
		return QueueManagerHolder.rQueue;
        }

    /**
     * Use Initialization on demand holder pattern
     */
    private static class QueueManagerHolder {
        private static final QueueManager rQueue = new QueueManager();
    }

    private QueueManager() {
        mRequestQueue = RequestQueue.getInstance();
        mResponseQueue = ResponseQueue.getInstance();
    }

    /**
     * Adds a response to the response queue.
     * 
     * @param response The response to add to the queue.
     */
    public void addResponse(DecodedResponse response) {
        synchronized (lock) {
            mResponseQueue.addToResponseQueue(response);
        }
    }

    /**
     * Returns the next response in the response queue for the given engine.
     * That way an engine can easily get a response that it is responsible for.
     * 
     * @param sourceEngine The source engine
     * @return The next response for the given source engine.
     */
    /*
    public Response getNextResponse(EngineId sourceEngine) {
        synchronized (lock) {
            return mResponseQueue.getNextResponse(sourceEngine);
        }
    }
*/
    
    /**
     * Clears all request timeouts that were added to the timeout watcher.
     */
    public void clearRequestTimeouts() {
        mRequestQueue.clearTheTimeOuts();
    }

    /**
     * Returns a timeout-watcher of requests from the request queue.
     * 
     * @return The timeout watcher inside the request queue.
     */
    public TimeOutWatcher getRequestTimeoutWatcher() {
        return mRequestQueue.getTimeoutWatcher();
    }

    /**
     * Clears all requests from the request queue and puts null responses on the
     * response queue to tell the engines that they have been cleared. This
     * should be called from the connection thread as soon as it is stopped.
     */
    public void clearAllRequests() {
        synchronized (lock) {
            mRequestQueue.clearAllRequests();
        }
    }

    /**
     * Clear active requests (i.e add dummy response to response queue).
     * 
     * @param rpgOnly If true only RPG requests will be cleared.
     */
    public void clearActiveRequests(boolean rpgOnly) {
        synchronized (lock) {
            mRequestQueue.clearActiveRequests(rpgOnly);
        }
    }
    
    /**
     * Removes the request for the given request ID from the queue and searches
     * the queue for requests older than
     * Settings.REMOVE_REQUEST_FROM_QUEUE_MILLIS and removes them as well.
     * 
     * @param requestId - the id of the request in he queue.
     * @return Returns the removed request, can be null if the request with the given Id wasn't found.
     */
    public Request removeRequest(int requestId) {
        synchronized (lock) {
            return mRequestQueue.removeRequest(requestId);
        }
    }

    /**
     * Return Request from specified request ID. Only used for unit tests.
     * 
     * @param requestId Request Id of required request
     * @return Request with or null if request does not exist
     */
    public Request getRequest(int requestId) {
        return mRequestQueue.getRequest(requestId);
    }

    /**
     * Returns all requests from the queue needing the API or both to work.
     * 
     * @return List of all requests needing the API or both (API or RPG) to
     *         function properly.
     */
    public List<Request> getApiRequests() {
        synchronized (lock) {
            return mRequestQueue.getApiRequests();
        }
    }

    /**
     * Returns all requests from the queue needing the RPG or both to work.
     * 
     * @return List of all requests needing the RPG or both (API or RPG) to
     *         function properly.
     */
    public List<Request> getRpgRequests() {
        synchronized (lock) {
            return mRequestQueue.getRpgRequests();
        }
    }

    /**
     * Returns all requests from the queue. Regardless if they need to
     * 
     * @return List of all requests.
     */
    public List<Request> getAllRequests() {
        synchronized (lock) {
            return mRequestQueue.getAllRequests();
        }
    }

    /**
     * Adds requests to the queue.
     * 
     * @param requests The requests to add.
     * @return The request IDs generated in an integer array or null if the
     *         requests array was null.
     */
    public int[] addRequest(Request[] requests) {
        synchronized (lock) {
            return mRequestQueue.addRequest(requests);
        }
    }

    /**
     * Adds a request to the queue without sending an event to the listeners
     * 
     * @param request The request to add
     * @return The unique request ID TODO: What is with the method naming
     *         convention?
     */
    public int addRequest(Request request) {
        synchronized (lock) {
            return mRequestQueue.addRequest(request);
        }
    }

    /**
     * Add request to queue and notify the queue listener.
     * 
     * @param request The request to add to the queue.
     * @return The request ID of the added request.
     */
    public int addRequestAndNotify(Request request) {
        synchronized (lock) {
            return mRequestQueue.addRequestAndNotify(request);
        }
    }

    /**
     * Fire a manual queue state changed event to notify the queue listener that
     * a request is on the request queue.
     */
    public void fireQueueStateChanged() {
        mRequestQueue.fireQueueStateChanged();
    }

    /**
     * Adds a listener listening for RequestQueue changes. Events are sent when
     * items are added to the queue (or in the case of batching when the last
     * item is added to the queue).
     * 
     * @param listener Listener to add to the list of request queue listeners.
     */
    public void addQueueListener(IQueueListener listener) {
        mRequestQueue.addQueueListener(listener);
    }

    /**
     * Remove RequestQueue listener from the list.
     * 
     * @param listener The listener to remove.
     */
    public void removeQueueListener(IQueueListener listener) {
        mRequestQueue.removeQueueListener(listener);
    }
}
