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

package com.vodafone360.people.engine;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.utils.LogUtils;

/**
 * Base-class for all Engines implemented by the People Client.
 */
public abstract class BaseEngine {

    /**
     * All engines must set this field to a unique ID
     */
    protected EngineId mEngineId = EngineId.UNDEFINED;

    /**
     * Callback provided by {@link EngineManager}
     */
    protected IEngineEventCallback mEventCallback;

    /**
     * Current UI Request if one is active, otherwise null.
     * 
     * @see ServiceUiRequest
     */
    protected ServiceUiRequest mActiveUiRequest;

    /**
     * Current timeout based on current time in milliseconds if one is pending,
     * otherwise null.
     */
    protected volatile Long mCurrentTimeout;

    /**
     * true if a Comms response is waiting in the comms response queue for
     * processing, false otherwise.
     */
    private Boolean mCommsResponseOutstanding = false;

    /**
     * Set by the {@link #setReqId(int)} function to store the request ID when a
     * engine makes a request to the server. All responses received will then be
     * filtered by this value automatically inside the
     * {@link #processCommsInQueue()} function.
     */
    private Integer mActiveRequestId;

    /**
     * true if a UI request is waiting in the UI request queue for processing,
     * false otherwise.
     */
    private boolean mUiRequestOutstanding;

    /**
     * true if the engine is deactivated (for test purposes), false otherwise.
     */
    private boolean mDeactivated;

    /**
     * mutex for thread synchronization
     */
    private Object mMutex = new Object();
    
    /**
     * Flag set when a request to reset the engine is made and the engine has performed its reset tasks.
     */
    private boolean resetDone = false;

    /**
     * Interface which must be implemented by engine client. Provides the
     * interface for engine to return the results of requests to their clients.
     */
    public static interface IEngineEventCallback {
        /***
         * Handle an incoming UI Event.
         * 
         * @param event ServiceUiRequest - e.g. UI request complete.
         * @param request ID of associated request.
         * @param status status of request (success or error code).
         * @param data object Data associated with completed request.
         */
        void onUiEvent(ServiceUiRequest event, int request, int status, Object data);

        /***
         * Restarts the WorkerThread if it is in a sleeping or suspended state,
         * ignored otherwise. This method is called by various events including:
         * new UI or network events or a wake up alarm set by an engine
         * requiring periodic activity.
         */
        void kickWorkerThread();

        /***
         * Returns the UiAgent, for sending unsolicited messages to the UI.
         * 
         * @return UiAgent object.
         */
        public UiAgent getUiAgent();
        
        /***
         * Returns the ApplicationCache, for storing data.
         * 
         * @return Application cache object.
         */
        public ApplicationCache getApplicationCache();
    }

    /**
     * Class to encapsulate client request information.
     */
    private static class UiQueueItem {
        private ServiceUiRequest mRequestId;

        private Object mData;
    }

    /**
     * Used to implement the UI request queue.
     */
    private final ConcurrentLinkedQueue<UiQueueItem> mUiQueue = new ConcurrentLinkedQueue<UiQueueItem>();

    /**
     * Public constructor.
     * 
     * @param eventCallback This interface must be implemented by the engine
     *            client
     */
    public BaseEngine(IEngineEventCallback eventCallback) {
        mEventCallback = eventCallback;
        mDeactivated = false;
    }

    /**
     * Return the absolute time in milliseconds when the engine needs to run
     * (based on System.currentTimeMillis). (Maybe add currentTime in the future
     * to use as the current time, to enable JUnit tests to validate timeout
     * functionality).
     * 
     * @return -1 never needs to run, 0 needs to run as soon as possible,
     *         CurrentTime + 60000 to run in 1 minute, etc.
     */
    public abstract long getNextRunTime();

    /**
     * Do some work but anything that takes longer than 1 second must be broken
     * up. (Maybe add currentTime as parameter, to enable JUnit tests to
     * validate timeout functionality)
     */
    public abstract void run();

    /**
     * This will be called immediately after creation.
     */
    public abstract void onCreate();

    /**
     * This will be called just before the engine is shutdown.
     */
    public abstract void onDestroy();

    /**
     * Helper function for use by the derived engine class to add a UI request
     * to the queue. This will be run from the UI thread
     */
    public void addUiRequestToQueue(ServiceUiRequest request, Object data) {
        if (mDeactivated) {
            onUiRequestComplete(request, ServiceStatus.ERROR_NOT_IMPLEMENTED, null);
            return;
        }
        UiQueueItem item = new UiQueueItem();
        item.mRequestId = request;
        item.mData = data;
        synchronized (mUiQueue) {
            mUiQueue.add(item);
            mUiRequestOutstanding = true;
        }
        mEventCallback.kickWorkerThread();
    }

    /**
     * Return id for this engine.
     * 
     * @return EngineId identifying concrete engine
     */
    public EngineId engineId() {
        return mEngineId;
    }

    /**
     * Helper function for use by the derived engine class to fetch the next UI
     * request from the queue. Returns null if the queue is empty.
     */
    private UiQueueItem fetchNextUiRequest() {
        UiQueueItem item = null;
        synchronized (mUiQueue) {
            item = mUiQueue.poll();
            if (mUiQueue.isEmpty()) {
                mUiRequestOutstanding = false;
            }
        }
        return item;
    }

    /**
     * Helper function to determine if there is any work outstanding in the UI
     * request queue. Returns false if the queue is empty.
     */
    protected boolean isUiRequestOutstanding() {
        synchronized (mUiQueue) {
            // mActiveUiRequest must not be null if its not
            // and there is more than one request in a queue
            // engine might go into endless loop
            if (mActiveUiRequest != null) {
                return false;
            }
            return mUiRequestOutstanding;
        }
    }

    /**
     * Helper function which must be called to complete a UI request. Normally
     * this will not be called directly by the derived engine implementation.
     * Instead the completeUiRequest function should be used.
     * 
     * @param request The request Id to complete
     * @param status The ServiceStatus code
     * @param data Response data (object type is request specific)
     */
    private void onUiRequestComplete(ServiceUiRequest request, ServiceStatus status, Object data) {
        mEventCallback.onUiEvent(ServiceUiRequest.UI_REQUEST_COMPLETE, request.ordinal(), status
                .ordinal(), data);
    }

    /**
     * The derived engine implementation must call the processCommsInQueue()
     * function (normally from within the run() implementation), otherwise this
     * will not be called. This function is called for each Comms response that
     * arrives on the in queue.
     * 
     * @param resp The comms response
     */
    protected abstract void processCommsResponse(ResponseQueue.DecodedResponse resp);

    /**
     * The derived engine implementation must call the processUiQueue() function
     * (normally from within the run() implementation), otherwise this will not
     * be called. This function is called for each UI request that arrives on
     * the queue. It should start processing the request. If this function takes
     * longer than 1 second to complete, it should be broken up. Once a request
     * is finished the processUiRequest function must be called.
     * 
     * @param requestId The UI request ID
     * @param data Request data (object type is request specific)
     */
    protected abstract void processUiRequest(ServiceUiRequest requestId, Object data);

    /**
     * The derived engine implementation must call the processTimeout() function
     * (normally from within the run() implementation), otherwise this will not
     * be called. This function will be called when a timeout occurs (started by
     * setTimeout and cancelled by clearTimeout). If this function takes longer
     * than 1 second to complete it should be broken up.
     */
    protected abstract void onTimeoutEvent();

    /**
     * Called by the EngineManager when a comms response is received. Will set
     * the response outstanding flag and kick the worker thread.
     */
    public void onCommsInMessage() {
        synchronized (mMutex) {
            mCommsResponseOutstanding = true;
        }
        mEventCallback.kickWorkerThread();
    }

    /**
     * Should be called by the getNextRunTime() and run() functions to check if
     * there are any comms responses waiting to be processed.
     * 
     * @return true if there are 1 or more responses to process.
     */
    protected boolean isCommsResponseOutstanding() {
        synchronized (mMutex) {
            return mCommsResponseOutstanding;
        }
    }

    /**
     * Should be called by the run() function to process the comms in queue.
     * Calling this function will result in the processCommsResponse function
     * being called once. The derived engine implementation should do its
     * processing of each response in that function. If the engine set the
     * request ID using the setReqId function then messages which don't match
     * will be taken off the queue and deleted.
     * 
     * @return true if a response was taken from the queue and processed.
     */
    protected boolean processCommsInQueue() {
        final ResponseQueue queue = ResponseQueue.getInstance();
        if (queue != null) {
            final ResponseQueue.DecodedResponse resp = queue.getNextResponse(mEngineId);
            if (resp == null) {
                synchronized (mMutex) {
                    mCommsResponseOutstanding = false;
                }
                return false;
            }
            boolean processResponse = false;
            if (resp.mReqId == null || mActiveRequestId == null) {
                processResponse = true;
            } else if (mActiveRequestId.equals(resp.mReqId)) {
                mActiveRequestId = null;
                processResponse = true;
            }
            if (processResponse) {
                processCommsResponse(resp);
            }
            return processResponse;
        } else {
            throw new RuntimeException(
                    "BaseEngine.processCommsInQueue - ResponseQueue cannot be null");
        }
    }

    /**
     * A helper function for the derived engine implementation to use. It checks
     * the returned comms response data against the expected type and handles
     * all common error cases.
     * 
     * @param requiredResp The expected type
     * @param data The data received from the comms response
     * @return SUCCESS if the first element in the list is of the expected type,
     *         ERROR_COMMS if the first element in the list is an unexpected
     *         type, ERROR_COMMS_BAD_RESPONSE if data is not valid for specyfied
     *         type or null otherwise if the data is of type ZError, a suitable
     *         error code.
     */
    public static ServiceStatus getResponseStatus(int requiredResponseType,
            List<BaseDataType> data) {
        ServiceStatus errorStatus = ServiceStatus.ERROR_COMMS;
        if (data != null) {
            if (data.size() == 0 || data.get(0).getType() == requiredResponseType) {
                if (requiredResponseType == BaseDataType.CONTACT_CHANGES_DATA_TYPE && 
                        data.size() == 0) {
                    errorStatus = ServiceStatus.ERROR_COMMS_BAD_RESPONSE;
                } else {
                    errorStatus = ServiceStatus.SUCCESS;
                }
            } else if (data.get(0).getType() == BaseDataType.SERVER_ERROR_DATA_TYPE) {
                final ServerError error = (ServerError)data.get(0);
                LogUtils.logE("Server error: " + error);
                errorStatus = error.toServiceStatus();
            } else {
                LogUtils.logD(
                        "BaseEngine.genericHandleResponse: Unexpected type [" + requiredResponseType
                        + "] but received [" + data.get(0).getType() + "]");
            }
        } else {
            errorStatus = ServiceStatus.ERROR_COMMS_BAD_RESPONSE;
        }
        return errorStatus;
    }

    /**
     * Should be called by the run() function to process the UI request queue.
     * Calling this function will result in the processUiRequest function being
     * called once and this will be set to the active request. The derived
     * engine implementation should do its processing of each request in that
     * function. Note the engine must not process any more requests until the
     * current one has been completed.
     * 
     * @return true if a response was taken from the queue and processed.
     */
    protected boolean processUiQueue() {
        if (mActiveUiRequest != null) {
            return false;
        }
        final UiQueueItem uiItem = fetchNextUiRequest();
        if (uiItem != null) {
            mActiveUiRequest = uiItem.mRequestId;
            processUiRequest(uiItem.mRequestId, uiItem.mData);
            return true;
        }
        return false;
    }

    /**
     * A helper function that can be called by the derived engine implementation
     * to complete the current UI request.
     * 
     * @param status The result of the request
     */
    protected void completeUiRequest(ServiceStatus status) {
        completeUiRequest(status, null);
    }

    /**
     * This function must be implemented in the derived engine implementation.
     * It can do any post-request complete cleanup.
     */
    protected abstract void onRequestComplete();

    /**
     * A helper function that can be called by the derived engine implementation
     * to complete the current UI request.
     * 
     * @param status The result of the request
     * @param data Response data (object type is request specific)
     */
    protected void completeUiRequest(ServiceStatus status, Object data) {
        onRequestComplete();
        if (mActiveUiRequest != null) {
            onUiRequestComplete(mActiveUiRequest, status, data);
            mActiveUiRequest = null;
        }
    }

    /**
     * This method was added to provide a the engine a way to notify UI of
     * unsolicited incoming messages/errors
     * 
     * @param request - the result of the request
     * @param status - SUCCESS or ERROR
     * @param data Response data (object type is request specific)
     */
    /*
     * protected void completeUnsolicitedUiRequest(ServiceUiRequest request,
     * ServiceStatus status, Object data) { onRequestComplete();
     * onUiRequestComplete(request, status, data); mActiveUiRequest = null; //
     * TODO: check if we need to set it null }
     */

    /**
     * Helper function that can be called by the derived engine implementation
     * to start an asynchronous timer. This will only work when: 1)
     * getCurrentTimeout() is called inside the getNextRunTime() function. 2)
     * The processTimeout() is called in the run() function 3) The
     * onTimeoutEvent() is implemented to handle the timeout event.
     * 
     * @param timeoutVal The timeout value (in milliseconds)
     */
    protected void setTimeout(long timeoutVal) {
        mCurrentTimeout = System.currentTimeMillis() + timeoutVal;
    }

    /**
     * Cancels the current timer (has no effect if the timer was not active).
     */
    protected void clearTimeout() {
        mCurrentTimeout = null;
    }

    /**
     * The result of this function must be returned by getNextRunTime() instead
     * of -1 for the timer to work.
     * 
     * @return The required next run time (in milliseconds) or -1 if no timer is
     *         active
     */
    protected long getCurrentTimeout() {
        if (mCurrentTimeout != null) {
            return mCurrentTimeout;
        }
        return -1;
    }

    /**
     * This function must be called by run() in the derived engine
     * implementation for the timer to work.
     * 
     * @return true if the timeout was processed
     */
    protected boolean processTimeout() {
        if (mCurrentTimeout != null) {
            long currentTimeMs = System.currentTimeMillis();
            if (currentTimeMs >= mCurrentTimeout) {
                mCurrentTimeout = null;
                onTimeoutEvent();
                return true;
            }
        }
        return false;
    }

    /**
     * Called by the framework to deactivate the engine. This allows engines to
     * be deactivated by modifying the Settings (so features can be disabled in
     * the build).
     */
    protected void deactivateEngine() {
        mDeactivated = true;
    }

    /**
     * Called by the framework to determine if the engine should run.
     */
    public boolean isDeactivated() {
        return mDeactivated;
    }

    /**
     * Called by engines when an API call is made to ensure that the response
     * processed matches the request. If this function is not called the
     * framework will send all responses to the engine.
     */
    protected boolean setReqId(int reqId) {
        if (reqId == -1) {
            return false;
        }
        mActiveRequestId = reqId;
        return true;
    }

    /**
     * Called by engines in the UI thread to cancel all UI requests. The engine
     * should ensure that the active UI request is completed (if necessary)
     * before calling this function.
     */
    protected void emptyUiRequestQueue() {
        synchronized (mUiQueue) {
            mUiQueue.clear();
            mUiRequestOutstanding = false;
            mActiveUiRequest = null;
        }
    }

    /**
     * Engines can override this function to do any special handling when a reset is needed.
     * Note: if overriden, the engine shall call the super implementation when the reset is done.
     */
    public void onReset() {
        // default implementation of the reset, sets the reset flag to true
        setReset(true);
    }
    
    /**
     * Sets the reset flag.
     *
     * @param value the reset state of the engine, true if reset, false if not.
     */
    private void setReset(boolean value) {
        resetDone = value;
    }
    
    /**
     * Gets the reset flag.
     *
     * @return true if the engine is reset, false if not
     */
    boolean getReset() {
        return resetDone;
    }
    
    /**
     * Clears the reset flag. 
     */
    void clearReset() {
        setReset(false);
    }
}
