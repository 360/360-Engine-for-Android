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

package com.vodafone360.people.engine.presence;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ChatMessage;
import com.vodafone360.people.datatypes.Conversation;
import com.vodafone360.people.datatypes.PresenceList;
import com.vodafone360.people.datatypes.PushAvailabilityEvent;
import com.vodafone360.people.datatypes.PushChatMessageEvent;
import com.vodafone360.people.datatypes.PushClosedConversationEvent;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.IContactSyncObserver;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.Mode;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine.State;
import com.vodafone360.people.engine.login.LoginEngine.ILoginEventsListener;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.service.io.api.Chat;
import com.vodafone360.people.service.io.api.Presence;
import com.vodafone360.people.utils.LogUtils;

/**
 * Handles the Presence life cycle
 */
public class PresenceEngine extends BaseEngine implements ILoginEventsListener,
        IContactSyncObserver {
    /** Check every 10 minutes. **/
    private final static long CHECK_FREQUENCY = 24 * 60 * 60 * 1000;

    /** Max attempts to try. **/
    private final static int MAX_RETRY_COUNT = 3;

    /** Reconnecting before firing offline state to the handlers. **/
    private boolean mLoggedIn = false;

    private long mNextRuntime = -1;

    private AgentState mNetworkAgentState = AgentState.CONNECTED;

    private DatabaseHelper mDbHelper;

    private int mRetryNumber;

    private final Hashtable<String, ChatMessage> mSendMessagesHash; // (to, message)

    private List<TimelineSummaryItem> mFailedMessagesList; // (to, network)

    private boolean mContObsAdded;
    
    /** The list of Users still to be processed. **/
    private List<User> mUsers = null;

    /**
     * This state indicates there are no more pending presence payload
     * information to be processed.
     **/
    private static final int IDLE = 0;

    /**
     * This state indicates there are some pending presence payload information
     * to be processed.
     **/
    private static final int UPDATE_PROCESSING_GOING_ON = 1;

    /** Timeout between each presence update processing. **/
    private static final long UPDATE_PRESENCE_TIMEOUT_MILLS = 0;

    /** The page size i.e the number of presence updates processed at a time. **/
    private static final int UPDATE_PRESENCE_PAGE_SIZE = 10;
    
    /** The number of pages after which the HandlerAgent is notified. **/
    private static final int NOTIFY_AGENT_PAGE_INTERVAL = 10;

    /** The state of the presence Engine. **/
    private int mState = IDLE;

    /**
     * Number of pages of presence Updates done. This is used to control when a
     * notification is sent to the UI.
     **/
    private int mIterations = 0;

    /**
     * 
     * @param eventCallback
     * @param databaseHelper
     */
    public PresenceEngine(IEngineEventCallback eventCallback, DatabaseHelper databaseHelper) {
        super(eventCallback);
        mEngineId = EngineId.PRESENCE_ENGINE;
        mDbHelper = databaseHelper;
        mSendMessagesHash = new Hashtable<String, ChatMessage>();
        mFailedMessagesList = new ArrayList<TimelineSummaryItem>();
        addAsContactSyncObserver();
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        if (mDbHelper != null && SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper) != null) {
            PresenceDbUtils.resetPresenceStatesAcceptForMe(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper),
                    mDbHelper);
        }
        EngineManager.getInstance().getLoginEngine().removeListener(this);
    }

    /**
     * checks the external conditions which have to be happen before the engine
     * can run
     * 
     * @return true if everything is ready
     */
    private boolean canRun() {
        return EngineManager.getInstance().getSyncMeEngine().isFirstTimeMeSyncComplete() &&
            EngineManager.getInstance().getContactSyncEngine().isFirstTimeSyncComplete();
    }

    @Override
    public long getNextRunTime() {

        if (!mContObsAdded) {
            addAsContactSyncObserver();
        }

        if (!canRun()) {
            mNextRuntime = -1;
            LogUtils.logV("PresenceEngine.getNextRunTime(): 1st contact sync is not finished:"
                    + mNextRuntime);
            return mNextRuntime;
        }

        AgentState currentState = NetworkAgent.getAgentState();
        if (currentState != mNetworkAgentState) {
            LogUtils.logV("PresenceEngine.getNextRunTime(): NetworkAgent state has just changed: "
                    + currentState);
            return 0;
        }

        /**
         * need isUiRequestOutstanding() because currently the worker thread is
         * running and finishing before PresenceEngine.setNextRuntime is called
         */
        if (isCommsResponseOutstanding() || isUiRequestOutstanding()) {
            LogUtils.logV("PresenceEngine getNextRunTime() comms response outstanding");
            return 0;
        }
        if (!mLoggedIn || (mNetworkAgentState != AgentState.CONNECTED)) {
            mNextRuntime = -1;
            return -1;
        } else if (mNextRuntime == -1) {
            LogUtils.logV("PresenceEngine getNextRunTime() Run PresenceEngine for the first time!");
            return 0;
        } else {
            return mNextRuntime;
        }
    }
    
    @Override
    public void run() {
        final AgentState currentState = NetworkAgent.getAgentState();
        boolean stateChanged = mNetworkAgentState != currentState;
        if (stateChanged) {
            mNetworkAgentState = currentState;
        }
        LogUtils.logV("PresenceEngine.run() isCommsResponseOutstanding["
                + isCommsResponseOutstanding() + "] mLoggedIn[" + mLoggedIn + "] mNextRuntime["
                + mNextRuntime + "]");
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            LogUtils.logV("PresenceEngine.run() handled processCommsInQueue()");
            return;
        }
        if (processTimeout()) {
            LogUtils.logV("PresenceEngine.run() handled processTimeout()");
            return;
        }
        if (mNetworkAgentState == AgentState.CONNECTED) {
            if (mLoggedIn && ((mNextRuntime <= System.currentTimeMillis()) || stateChanged)) {
                // TODO: move from this "if" into the "States" model
                if (canRun()) {
                    getPresenceList();
                    // Request to update the UI
                    setNextRuntime();
                } else { // check after 30 seconds
                    LogUtils.logE("Can't run PresenceEngine before the contact"
                            + " list is downloaded:3 - set next runtime in 30 seconds");
                    mNextRuntime = System.currentTimeMillis() + CHECK_FREQUENCY / 20;
                }
            }
        } else {
            LogUtils.logV("PresenceEngine.run(): AgentState.DISCONNECTED");
            setPresenceOffline();
        }

        /**
         * and the getNextRunTime must check the uiRequestReady() function and
         * return 0 if it is true
         */
        if (uiRequestReady() && processUiQueue()) {
            return;
        }
    }

    /**
     * Helper function which returns true if a UI request is waiting on the
     * queue and we are ready to process it.
     * 
     * @return true if the request can be processed now.
     */
    private boolean uiRequestReady() {
        return isUiRequestOutstanding();
    }

    private User getMyAvailabilityStatusFromDatabase() {
        return PresenceDbUtils.getMeProfilePresenceStatus(mDbHelper);
    }

    private void setNextRuntime() {
        LogUtils.logV("PresenceEngine.setNextRuntime() Run again in ["
                + (CHECK_FREQUENCY / (1000 * 60)) + "] minutes");
        mNextRuntime = System.currentTimeMillis() + CHECK_FREQUENCY;
    }

    private void setRunNow() {
        LogUtils.logV("PresenceEngine.setNextRuntime() Run again NOW");
        mNextRuntime = 0;
    }

    @Override
    public void onLoginStateChanged(boolean loggedIn) {
        LogUtils.logI("PresenceEngine.onLoginStateChanged() loggedIn[" + loggedIn + "]");
        mLoggedIn = loggedIn;
        if (mLoggedIn) {
            getPresenceList();
            setNextRuntime();
        } else {
            setPresenceOffline();
            mContObsAdded = false;
            mRetryNumber = 0;
            mFailedMessagesList.clear();
            mSendMessagesHash.clear();
        }
    }


    /***
     * Set the Global presence status to offline.
     */
    private synchronized void setPresenceOffline() {
        PresenceDbUtils.setPresenceOfflineInDatabase(mDbHelper);
        // We clear the mUsers of any pending presence updates because this
        // Offline presence update request should take the highest priority.
        mUsers = null;
        mState = IDLE;
        mEventCallback.getUiAgent().updatePresence(-1);
    }

    @Override
    protected void onRequestComplete() {
        LogUtils.logI("PresenceEngine.onRequestComplete()");
    }

    @Override
    protected void onTimeoutEvent() {
        LogUtils.logI("PresenceEngine.onTimeoutEvent()");
        switch (mState) {
            case UPDATE_PROCESSING_GOING_ON:
                updatePresenceDatabaseNextPage();
                break;
            case IDLE:
            default:
                setRunNow();
        }

    }

    @Override
    protected void processCommsResponse(Response resp) {
        handleServerResponse(resp.mDataTypes);
    }

    private void showErrorNotification(ServiceUiRequest errorEvent, ChatMessage msg) {
        UiAgent uiAgent = mEventCallback.getUiAgent();
        if (uiAgent != null && uiAgent.isSubscribedWithChat()) {
            uiAgent.sendUnsolicitedUiEvent(errorEvent, null);
        }
    }

    /**
     * Here we update the PresenceTable, and the ContactSummaryTable afterwards
     * the HandlerAgent receives the notification of presence states changes.
     * 
     * @param users List of users that require updating.
     */
    private synchronized void updatePresenceDatabase(List<User> users) {
        if (mUsers == null) {
            mUsers = users;
        } else {
            mUsers.addAll(users);
        }
        mState = UPDATE_PROCESSING_GOING_ON;
        updatePresenceDatabaseNextPage();
    }
    
    /**
     * This API makes the presence updates in pages of 10 with a timeout
     * after each page. The HandlerAgent is notified after every 10 pages.
     */
    private synchronized void updatePresenceDatabaseNextPage(){
        UiAgent uiAgent = mEventCallback.getUiAgent();
        if(mUsers == null){
            mState = IDLE;
            return;
        }
        
        int listSize = mUsers.size();
        int start = 0;
        int end = UPDATE_PRESENCE_PAGE_SIZE;
        if(listSize == 0){
            mState = IDLE;
            mUsers = null;
            return;
        } else if(listSize < end) {
            end = listSize;
        }
        List<User> userSubset = mUsers.subList(start, end);
        LogUtils.logW("PresenceEngine.updatePresenceDatabase():" + userSubset);
        if ((userSubset != null)) {
            long idListeningTo = -1;
            if (uiAgent != null) {
                uiAgent.getLocalContactId();
            }
            if (PresenceDbUtils.updateDatabase(userSubset, idListeningTo, mDbHelper)) {
                userSubset.clear();
                // Send the update notification to UI for every 100 updates.
                if (mUsers.size() == 0 || mIterations == NOTIFY_AGENT_PAGE_INTERVAL) {
                    if (uiAgent != null) {
                        uiAgent.updatePresence(idListeningTo);
                    }
                    mIterations = 0;
                } else {
                    mIterations++;
                }
            }
            this.setTimeout(UPDATE_PRESENCE_TIMEOUT_MILLS);
            
        }
        LogUtils.logW("PresenceEngine DONE!!!!" );
    }


    /**
     * Updates the database with the given ChatMessage and Type.
     * 
     * @param message ChatMessage.
     * @param type TimelineSummaryItem.Type.
     */
    private void updateChatDatabase(ChatMessage message, TimelineSummaryItem.Type type) {
        ChatDbUtils.convertUserIds(message, mDbHelper);
        LogUtils.logD("PresenceEngine.updateChatDatabase() with [" + type.name() + "] message");
        if (message.getLocalContactId() == null || message.getLocalContactId() < 0) {
            LogUtils.logE("PresenceEngine.updateChatDatabase() "
                    + "WILL NOT UPDATE THE DB! - INVALID localContactId = "
                    + message.getLocalContactId());
            return;
        }

        /** We mark all incoming messages as unread. **/
        ChatDbUtils.saveChatMessageAsATimeline(message, type, mDbHelper);

        UiAgent uiAgent = mEventCallback.getUiAgent();
        if (uiAgent != null && (message.getLocalContactId() != -1)) {
            uiAgent.updateChat(message.getLocalContactId(), true);
        }
    }

    /**
     * Here we update the PresenceTable, and the ContactSummaryTable afterwards
     * the HandlerAgent receives the notification of presence states changes.
     * 
     * @param users User that requires updating.
     */
    private void updateMyPresenceInDatabase(User myself) {
        LogUtils.logV("PresenceEnfgine.updateMyPresenceInDatabase() myself[" + myself + "]");
        UiAgent uiAgent = mEventCallback.getUiAgent();
        if (PresenceDbUtils.updateMyPresence(myself, mDbHelper)) {
            uiAgent.updatePresence(myself.getLocalContactId());
        }
    }


    /**
     * This method handles incoming presence status change push events and the
     * whole PresenceList
     * 
     * @param dataTypes
     */
    private void handleServerResponse(List<BaseDataType> dataTypes) {
        LogUtils.logE("PresenceEngine handleServerResponse(List<BaseDataType> dataTypes) IN");
        if (!canRun()) {
            LogUtils.logE("PresenceEngine.handleServerResponce(): "
                    + "Can't run PresenceEngine before the contact list is downloaded:2");
            return;
        }
        if (dataTypes != null) {
            for (BaseDataType mBaseDataType : dataTypes) {
                String name = mBaseDataType.name();
                if (name.equals(PresenceList.NAME)) {
                    handlePresenceList((PresenceList)mBaseDataType);
                } else if (name.equals(PushEvent.NAME)) {
                    handlePushEvent(((PushEvent)mBaseDataType));
                } else if (name.equals(Conversation.NAME)) {
                    // a new conversation has just started
                    handleNewConversationId((Conversation)mBaseDataType);
                } else if (name.equals(SystemNotification.class.getSimpleName())) {
                    handleSystemNotification((SystemNotification)mBaseDataType);
                } else if (name.equals(ServerError.NAME)) {
                    handleServerError((ServerError)mBaseDataType);
                } else {
                    LogUtils.logE("PresenceEngine.handleServerResponse()"
                            + ": response datatype not recognized:" + name);
                }
            }
        } else {
            LogUtils.logE("PresenceEngine.handleServerResponse(): response is null!");
        }
        LogUtils.logE("PresenceEngine handleServerResponse(List<BaseDataType> dataTypes) OUT");
    }

    private void handlePresenceList(PresenceList presenceList) {
        mRetryNumber = 0; // reset time out
        updatePresenceDatabase(presenceList.getUsers());
        initSetMyAvailabilityRequest(getMyAvailabilityStatusFromDatabase());
    }


    private void handleServerError(ServerError srvError) {

        LogUtils.logE("PresenceEngine.handleServerResponse() - Server error: " + srvError);
        ServiceStatus errorStatus = srvError.toServiceStatus();
        if (errorStatus == ServiceStatus.ERROR_COMMS_TIMEOUT) {
            LogUtils.logW("PresenceEngine handleServerResponce():"
                    + " TIME OUT IS RETURNED TO PRESENCE ENGINE:" + mRetryNumber);
            if (mRetryNumber < MAX_RETRY_COUNT) {
                getPresenceList();
                mRetryNumber++;
            } else {
                mRetryNumber = 0;
                setPresenceOffline();
            }
        }
    }


    private void handleNewConversationId(Conversation conversation) {
        if (conversation.getTos() != null) {
            mRetryNumber = 0; // reset time out
            String to = conversation.getTos().get(0);
            if (mSendMessagesHash.containsKey(to)) {
                ChatMessage message = mSendMessagesHash.get(to);
                message.setConversationId(conversation.getConversationId());
                /** Update the DB with an outgoing message. **/
                updateChatDatabase(message, TimelineSummaryItem.Type.OUTGOING);
                Chat.sendChatMessage(message);
                mSendMessagesHash.remove(to);
                // clean check if DB needs a cleaning (except for
                // the current conversation id)
                ChatDbUtils.cleanOldConversationsExceptForContact(message.getLocalContactId(),
                        mDbHelper);
            }
        }
    }

    private void handlePushEvent(PushEvent event) {
        switch (event.mMessageType) {
            case AVAILABILITY_STATE_CHANGE:
                mRetryNumber = 0; // reset time out
                PushAvailabilityEvent pa = (PushAvailabilityEvent)event;
                updatePresenceDatabase(pa.mChanges);
                break;
            case CHAT_MESSAGE:
                PushChatMessageEvent pc = (PushChatMessageEvent)event;
                // update the DB with an incoming message
                updateChatDatabase(pc.getChatMsg(), TimelineSummaryItem.Type.INCOMING);
                break;
            case CLOSED_CONVERSATION:
                PushClosedConversationEvent pcc = (PushClosedConversationEvent)event;
                // delete the conversation in DB
                if (pcc.getConversation() != null) {
                    ChatDbUtils.deleteConversationById(pcc.getConversation(), mDbHelper);
                }
                break;
            default:
                LogUtils.logE("PresenceEngine.handleServerResponse():"
                        + " push message type was not recognized:" + event.name());
        }
    }

    private void handleSystemNotification(SystemNotification sn) {
        LogUtils.logE("PresenceEngine.handleServerResponse(): " + sn);
        switch (sn.getSysCode()) {
            case SEND_MESSAGE_FAILED:
                ChatDbUtils.updateUnsentChatMessage(mDbHelper);
                showErrorNotification(ServiceUiRequest.UNSOLICITED_CHAT_ERROR_REFRESH, null);
                break;
            case CONVERSATION_NULL:
                showErrorNotification(ServiceUiRequest.UNSOLICITED_CHAT_ERROR, null);
                break;
            default:
                LogUtils.logE("PresenceEngine.handleServerResponse()"
                        + " - unhandled notification: " + sn);
        }
    }

    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        if (!canRun()) {
            LogUtils.logE("PresenceEngine.processUIRequest():"
                    + " Can't run PresenceEngine before the contact list is downloaded:1");
            return;
        }
        LogUtils.logW("PresenceEngine.processUiRequest() requestId.name[" + requestId.name() + "]");
        switch (requestId) {
            case SET_MY_AVAILABILITY:
                if (data != null) {
                    Hashtable<String, String> availability = (Hashtable<String, String>) data;
                    Presence.setMyAvailability(availability);
//                    Log.w("PresenceEngine", "processUiRequest() SET MY AVAILABILITY:"
//                                + availability);
                    completeUiRequest(ServiceStatus.SUCCESS, null);
                    setNextRuntime();
                }
                break;
            case GET_PRESENCE_LIST:
                Presence.getPresenceList(EngineId.PRESENCE_ENGINE, null);
                completeUiRequest(ServiceStatus.SUCCESS, null);
                setNextRuntime();
//                Log.w("PresenceEngine", "processUiRequest() GET_PRESENCE_LIST");
                break;

            case CREATE_CONVERSATION:
                if (data != null) {
                    List<String> tos = ((ChatMessage)data).getTos();
                    LogUtils.logW("PresenceEngine processUiRequest() CREATE_CONVERSATION with: "
                            + tos);
                    Chat.startChat(tos);
                    // Request to update the UI
                    completeUiRequest(ServiceStatus.SUCCESS, null);
                    // Request to update the UI
                    setNextRuntime();
                }
                break;
            case SEND_CHAT_MESSAGE:
                if (data != null) {
                    ChatMessage msg = (ChatMessage)data;
                    updateChatDatabase(msg, TimelineSummaryItem.Type.OUTGOING);

                    LogUtils.logW("PresenceEngine processUiRequest() SEND_CHAT_MESSAGE :" + msg);
                    Chat.sendChatMessage(msg);
                    // Request to update the UI
                    completeUiRequest(ServiceStatus.SUCCESS, null);
                    // Request to update the UI
                    setNextRuntime();
                }
                break;
            default:
                LogUtils.logE("PresenceEngine processUiRequest() Unhandled UI request ["
                        + requestId.name() + "]");
        }
    }

    /**
     * Initiate the "get presence list" request sending to server. Makes the
     * engine run asap.
     * 
     * @return
     */
    public void getPresenceList() {
//        Log.w("PresenceEngine", "PresenceEngine getPresenceList()");
        addUiRequestToQueue(ServiceUiRequest.GET_PRESENCE_LIST, null);
    }

    
    private void initSetMyAvailabilityRequest(User myself) {
        if (myself == null) {
            LogUtils.logE("PresenceEngine.initSetMyAvailabilityRequest():"
                    + " Can't send the setAvailability request due to DB reading errors");
            return;
        }
        Hashtable<String, String> availability = new Hashtable<String, String>();
        
        for (NetworkPresence presence : myself.getPayload()) {
            availability.put(SocialNetwork.getPresenceValue(presence.getNetworkId()).toString(),
                    OnlineStatus.getValue(presence.getOnlineStatusId()).toString());
        }

        if (myself.isOnline() == OnlineStatus.ONLINE.ordinal()
                && (NetworkAgent.getAgentState() != AgentState.CONNECTED)) {
            LogUtils.logD("PresenceEngine.initSetMyAvailabilityRequest():"
                    + " return NO NETWORK CONNECTION");
            return;
        }
        // set the DB values
        myself.setLocalContactId(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper));
        
        updateMyPresenceInDatabase(myself);

        addUiRequestToQueue(ServiceUiRequest.SET_MY_AVAILABILITY, availability);
    }

    /**
     * Changes the state of the engine. Also displays the login notification if
     * necessary.
     * 
     * @param accounts
     */
    public void setMyAvailability(Hashtable<String, String> myselfPresence) {
        if (myselfPresence == null) {
            LogUtils.logE("PresenceEngine setMyAvailability:"
                    + " Can't send the setAvailability request due to DB reading errors");
            return;
        }
        
        LogUtils.logV("PresenceEngine setMyAvailability() called with:" + myselfPresence);
        if (NetworkAgent.getAgentState() != AgentState.CONNECTED) {
            LogUtils.logD("PresenceEnfgine.setMyAvailability(): skip - NO NETWORK CONNECTION");
            return;
        }
        
        User myself = new User(String.valueOf(PresenceDbUtils.getMeProfileUserId(mDbHelper)),
                myselfPresence);
        
        Hashtable<String, String> availability = new Hashtable<String, String>();
        for (NetworkPresence presence : myself.getPayload()) {
            availability.put(SocialNetwork.getPresenceValue(presence.getNetworkId()).toString(),
                    OnlineStatus.getValue(presence.getOnlineStatusId()).toString());
        }
        // set the DB values for myself
        myself.setLocalContactId(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper));
        updateMyPresenceInDatabase(myself);

        // set the engine to run now
        
        addUiRequestToQueue(ServiceUiRequest.SET_MY_AVAILABILITY, availability);
    }


    private void createConversation(ChatMessage msg) {
        LogUtils.logW("PresenceEngine.createConversation():" + msg);
        // as we currently support sending msgs to just one person getting the 0
        // element of "Tos" must be acceptable
        mSendMessagesHash.put(msg.getTos().get(0), msg);
        addUiRequestToQueue(ServiceUiRequest.CREATE_CONVERSATION, msg);
    }

    /**
     * This method should be used to send a message to a contact
     * 
     * @param tos - tlocalContactId of ContactSummary items the message is
     *            intended for. Current protocol version only supports a single
     *            recipient.
     * @param body the message text
     */
    public void sendMessage(long toLocalContactId, String body, int networkId) {
        LogUtils.logW("PresenceEngine.sendMessage() to:" + toLocalContactId + ", body:" + body
                + ", at:" + networkId);
        if (NetworkAgent.getAgentState() != AgentState.CONNECTED) {
            LogUtils.logD("PresenceEnfgine.sendMessage: skip - NO NETWORK CONNECTION");
            return;
        }
        ChatMessage msg = new ChatMessage();
        msg.setBody(body);
        // TODO: remove the hard code - go to UI and check what is there
        if (networkId == SocialNetwork.MOBILE.ordinal()
                || (networkId == SocialNetwork.PC.ordinal())) {
            msg.setNetworkId(SocialNetwork.VODAFONE.ordinal());
        } else {
            msg.setNetworkId(networkId);
        }
        msg.setLocalContactId(toLocalContactId);

        ChatDbUtils.fillMessageByLocalContactIdAndNetworkId(msg, mDbHelper);

        if (msg.getConversationId() != null) {
            // TODO: re-factor this
            if (msg.getNetworkId() != SocialNetwork.VODAFONE.ordinal()) {
                String fullUserId = SocialNetwork.getChatValue(msg.getNetworkId()).toString()
                        + ChatDbUtils.COLUMNS + msg.getUserId();
                msg.setUserId(fullUserId);
            }
            addUiRequestToQueue(ServiceUiRequest.SEND_CHAT_MESSAGE, msg);
        } else {
            // if the conversation was not found that means it didn't exist,
            // need start a new one
            if (msg.getUserId() == null) {
                ChatDbUtils.findUserIdForMessageByLocalContactIdAndNetworkId(msg, mDbHelper);
                createConversation(msg);
            }
        }
    }

    /**
     * Add ActivitiesEngine as an observer of the ContactSyncEngine. Need to be
     * able to obtain a handle to the EngineManager and a handle to the
     * ContactSyncEngine.
     */
    private void addAsContactSyncObserver() {
        if (EngineManager.getInstance() != null
                && EngineManager.getInstance().getContactSyncEngine() != null) {
            EngineManager.getInstance().getContactSyncEngine().addEventCallback(this);
            mContObsAdded = true;
            LogUtils.logD("ActivityEngine contactSync observer added.");
        } else {
            LogUtils.logE("ActivityEngine can't add to contactSync observers.");
        }
    }

    @Override
    public void onContactSyncStateChange(Mode mode, State oldState, State newState) {
        LogUtils.logD("PresenceEngine onContactSyncStateChange called.");
    }

    @Override
    public void onProgressEvent(State currentState, int percent) {
        if (percent == 100) {
            switch (currentState) {
                case FETCHING_SERVER_CONTACTS:
                    LogUtils
                            .logD("PresenceEngine onProgressEvent: FETCHING_SERVER_CONTACTS is done");
                    // mDownloadServerContactsComplete = true;
                    // break;
                    // case SYNCING_SERVER_ME_PROFILE:
                    // LogUtils
                    // .logD("PresenceEngine onProgressEvent: FETCHING_SERVER_ME_PROFILE is done");
                    // mDownloadMeProfileComplete = true;
                    // break;
                default:
                    // nothing to do now
                    break;
            }
        }
    }

    @Override
    public void onSyncComplete(ServiceStatus status) {
        LogUtils.logD("PresenceEngine onSyncComplete called.");
    }
}
