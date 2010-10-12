package com.vodafone360.people.engine;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;

/**
 * Interface which must be implemented by engine client. Provides the
 * interface for engine to return the results of requests to their clients.
 */
public interface IEngineEventCallback {
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

