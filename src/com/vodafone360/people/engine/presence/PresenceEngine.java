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

import android.text.TextUtils;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.ChatMessage;
import com.vodafone360.people.datatypes.Conversation;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.PresenceList;
import com.vodafone360.people.datatypes.PushAvailabilityEvent;
import com.vodafone360.people.datatypes.PushChatMessageEvent;
import com.vodafone360.people.datatypes.PushClosedConversationEvent;
import com.vodafone360.people.datatypes.PushEvent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SystemNotification;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.datatypes.SystemNotification.Tags;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine.ILoginEventsListener;
import com.vodafone360.people.engine.meprofile.SyncMeDbUtils;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.io.ResponseQueue.DecodedResponse;
import com.vodafone360.people.service.io.api.Chat;
import com.vodafone360.people.service.io.api.Presence;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.service.transport.tcp.ITcpConnectionListener;
import com.vodafone360.people.utils.HardcodedUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Handles the Presence life cycle
 */
public class PresenceEngine extends BaseEngine implements ILoginEventsListener,
        ITcpConnectionListener {
    /** Check every 24 hours **/
    private final static long CHECK_FREQUENCY = 24 * 60 * 60 * 1000;

    /** Reconnecting before firing offline state to the handlers. **/
    private boolean mLoggedIn = false;

    private DatabaseHelper mDbHelper;

    private final Hashtable<String, ChatMessage> mSendMessagesHash; // (to, message)

    private List<TimelineSummaryItem> mFailedMessagesList; // (to, network)
    
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
    private static final int NOTIFY_AGENT_PAGE_INTERVAL = 5;

    /** The state of the presence Engine. **/
    private int mState = IDLE;

    /**
     * Number of pages of presence Updates done. This is used to control when a
     * notification is sent to the UI.
     **/
    private int mIterations = 0;
    
    /**
     * True if the engine runs for the 1st time.
     */
    private boolean firstRun = true;

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
     * Checks if the SyncMe and ContactSync Engines have both completed first time sync.
     * 
     * @return true if both engines have completed first time sync
     */
    private boolean isFirstTimeSyncComplete() {
        return EngineManager.getInstance().getSyncMeEngine().isFirstTimeMeSyncComplete() &&
            EngineManager.getInstance().getContactSyncEngine().isFirstTimeSyncComplete();
    }

    @Override
    public long getNextRunTime() {
        if (ConnectionManager.getInstance().getConnectionState() != STATE_CONNECTED || !mLoggedIn) {
            return -1;
        }

        if (!isFirstTimeSyncComplete()) {
            LogUtils.logV("PresenceEngine.getNextRunTime(): 1st contact sync is not finished:");
            return -1;
        }

        /**
         * need isUiRequestOutstanding() because currently the worker thread is
         * running and finishing before PresenceEngine.setNextRuntime is called
         */
        if (isCommsResponseOutstanding() || isUiRequestOutstanding()) {
            LogUtils.logV("PresenceEngine getNextRunTime() comms response outstanding");
            return 0;
        }
        if (firstRun) {
            getPresenceList();
            initSetMyAvailabilityRequest(getMyAvailabilityStatusFromDatabase());
            firstRun = false;
        }
        return getCurrentTimeout();
    }
    
    @Override
    public void run() {
        LogUtils.logV("PresenceEngine.run() isCommsResponseOutstanding["
                + isCommsResponseOutstanding() + "] mLoggedIn[" + mLoggedIn + "] mNextRuntime["
                + getCurrentTimeout() + "]");
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            LogUtils.logV("PresenceEngine.run() handled processCommsInQueue()");
            return;
        }
        if (processTimeout()) {
            LogUtils.logV("PresenceEngine.run() handled processTimeout()");
            return;
        }
        if (ConnectionManager.getInstance().getConnectionState() != STATE_CONNECTED) {
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

    @Override
    public void onLoginStateChanged(boolean loggedIn) {
        LogUtils.logI("PresenceEngine.onLoginStateChanged() loggedIn[" + loggedIn + "]");
        mLoggedIn = loggedIn;
        if (mLoggedIn) {
            if (isFirstTimeSyncComplete()) {
                getPresenceList();
                initSetMyAvailabilityRequest(getMyAvailabilityStatusFromDatabase());
                setTimeout(CHECK_FREQUENCY);    
            }
        } else {
            firstRun = true;
            mFailedMessagesList.clear();
            mSendMessagesHash.clear();
            
            setPresenceOffline();
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
        mEventCallback.getUiAgent().updatePresence(UiAgent.ALL_USERS);
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
                if (mLoggedIn) {
                    if (isFirstTimeSyncComplete()) {
                        getPresenceList();
                        initSetMyAvailabilityRequest(getMyAvailabilityStatusFromDatabase());
                        // Request to update the UI
                        setTimeout(CHECK_FREQUENCY);
                    } else { // check after 30 seconds
                        LogUtils.logE("Can't run PresenceEngine before the contact"
                                + " list is downloaded:3 - set next runtime in 30 seconds");
                        setTimeout(CHECK_FREQUENCY / 20);
                    }    
                }
        }
    }

    @Override
    protected void processCommsResponse(DecodedResponse resp) {
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
        if (users == null || users.size() == 0) {
            LogUtils.logW("PresenceEngine.updatePresenceDatabase() users is NULL or zero size");
        } else {
            if (mUsers == null) {
                mUsers = users;
            } else {
                // Doing an add one by one is not an efficient way but then
                // there is an issue in the addAll API. It crashes sometimes.
                // And the java code for the AddAll API seems to be erroneous.
                // More investigation is needed on this.
                for (User user : users) {
                    mUsers.add(user);
                }
            }
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
        if ((userSubset != null)) {
            long idListeningTo = UiAgent.ALL_USERS;
            if (uiAgent != null) {
                idListeningTo = uiAgent.getLocalContactId();
            }      
            
            boolean updateUI = PresenceDbUtils.updateDatabase(userSubset, idListeningTo, mDbHelper);
            userSubset.clear();
            // Send the update notification to UI for every UPDATE_PRESENCE_PAGE_SIZE*NOTIFY_AGENT_PAGE_INTERVAL updates.
            if (updateUI) {
                if (mUsers.size() == 0 || NOTIFY_AGENT_PAGE_INTERVAL == mIterations) {
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
        if (PresenceDbUtils.updateMyPresence(myself, mDbHelper)) {
        	mEventCallback.getUiAgent().updatePresence(myself.getLocalContactId());
        }
    }


    /**
     * This method handles incoming presence status change push events and the
     * whole PresenceList
     * 
     * @param dataTypes
     */
    private void handleServerResponse(List<BaseDataType> dataTypes) {

        if (dataTypes != null) {
            for (BaseDataType mBaseDataType : dataTypes) {
                int type = mBaseDataType.getType();
                if (type == BaseDataType.PRESENCE_LIST_DATA_TYPE) {
                    handlePresenceList((PresenceList)mBaseDataType);
                } else if (type == BaseDataType.PUSH_EVENT_DATA_TYPE) {
                    handlePushEvent(((PushEvent)mBaseDataType));
                } else if (type == BaseDataType.CONVERSATION_DATA_TYPE) {
                    // a new conversation has just started
                    handleNewConversationId((Conversation)mBaseDataType);
                } else if (type == BaseDataType.SYSTEM_NOTIFICATION_DATA_TYPE) {
                    handleSystemNotification((SystemNotification)mBaseDataType);
                } else if (type == BaseDataType.SERVER_ERROR_DATA_TYPE) {
                    handleServerError((ServerError)mBaseDataType);
                } else {
                    LogUtils.logE("PresenceEngine.handleServerResponse()"
                            + ": response datatype not recognized:" + type);
                }
            }
        } else {
            LogUtils.logE("PresenceEngine.handleServerResponse(): response is null!");
        }
    }

    private void handlePresenceList(PresenceList presenceList) {
        updatePresenceDatabase(presenceList.getUsers());
    }


    private void handleServerError(ServerError srvError) {

        LogUtils.logE("PresenceEngine.handleServerResponse() - Server error: " + srvError);
        ServiceStatus errorStatus = srvError.toServiceStatus();
        if (errorStatus == ServiceStatus.ERROR_COMMS_TIMEOUT) {
            LogUtils.logW("PresenceEngine handleServerResponce(): TIME OUT IS RETURNED TO PRESENCE ENGINE.");
        }
    }


    private void handleNewConversationId(Conversation conversation) {
        if (conversation.getTos() != null) {
            String to = conversation.getTos().get(0);
            if (mSendMessagesHash.containsKey(to)) {
                ChatMessage message = mSendMessagesHash.get(to);
                message.setConversationId(conversation.getConversationId());
                /** Update the DB with an outgoing message. **/
                updateChatDatabase(message, TimelineSummaryItem.Type.OUTGOING);
                Chat.sendChatMessage(message);
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
                        + " push message type was not recognized:" + event.getType());
        }
    }

    private void handleSystemNotification(SystemNotification sn) {
        LogUtils.logE("PresenceEngine.handleServerResponse(): " + sn);
        switch (sn.getSysCode()) {
            case SEND_MESSAGE_FAILED:
                ChatMessage msg = mSendMessagesHash.get(sn.getInfo().get(Tags.TOS.toString()));
                 if (msg != null) {
                     ChatDbUtils.deleteUnsentMessage(mDbHelper, msg);
                 }
                showErrorNotification(ServiceUiRequest.UNSOLICITED_CHAT_ERROR_REFRESH, null);
                break;
            case CONVERSATION_NULL:
                if (!mSendMessagesHash.isEmpty()) {
                    mSendMessagesHash.remove(sn.getInfo().get(Tags.TOS.toString()));
                }
                showErrorNotification(ServiceUiRequest.UNSOLICITED_CHAT_ERROR, null);
                break;
            case COMMUNITY_LOGOUT_SUCCESSFUL:
                break;    
                
            default:
                LogUtils.logE("PresenceEngine.handleServerResponse()"
                        + " - unhandled notification: " + sn);
        }
    }

    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        if (!isFirstTimeSyncComplete()) {
            LogUtils.logE("PresenceEngine.processUIRequest():"
                    + " Can't run PresenceEngine before the contact list is downloaded:1");
            return;
        }
        
        LogUtils.logW("PresenceEngine.processUiRequest() requestId.name[" + requestId.name() + "]");
        
        if(data == null && requestId != ServiceUiRequest.GET_PRESENCE_LIST) {
            LogUtils.logW("PresenceEngine.processUiRequest() skipping processing for request with no data!");
            return;
        }
        
        switch (requestId) {
            case SET_MY_AVAILABILITY:
                Presence.setMyAvailability((Hashtable<String,String>)data);
                break;
            case GET_PRESENCE_LIST:
                Presence.getPresenceList(EngineId.PRESENCE_ENGINE, null);
                break;
            case CREATE_CONVERSATION:
                List<String> tos = ((ChatMessage)data).getTos();
                LogUtils.logW("PresenceEngine processUiRequest() CREATE_CONVERSATION with: "
                        + tos);
                Chat.startChat(tos);
                break;
            case SEND_CHAT_MESSAGE:
                ChatMessage msg = (ChatMessage)data;
                updateChatDatabase(msg, TimelineSummaryItem.Type.OUTGOING);

                LogUtils.logW("PresenceEngine processUiRequest() SEND_CHAT_MESSAGE :" + msg);
                //cache the message (necessary for failed message sending failures)
                mSendMessagesHash.put(msg.getTos().get(0), msg);
                
                Chat.sendChatMessage(msg);
                break;
            default:
                LogUtils.logE("PresenceEngine processUiRequest() Unhandled UI request ["
                        + requestId.name() + "]");
                return; // don't complete with success and schedule the next runtime
        }
        
        completeUiRequest(ServiceStatus.SUCCESS, null);
        setTimeout(CHECK_FREQUENCY);
    }

    /**
     * Initiate the "get presence list" request sending to server. Makes the
     * engine run asap.
     * 
     * @return
     */
    public void getPresenceList() {
        addUiRequestToQueue(ServiceUiRequest.GET_PRESENCE_LIST, null);
    }

    /**
     * Method used to jump start setting the availability. 
     * This is primarily used for reacting to login/connection state changes.
     * @param me Our User
     */
    private void initSetMyAvailabilityRequest(User me) {
        if (me == null) {
            LogUtils.logE("PresenceEngine.initSetMyAvailabilityRequest():"
                    + " Can't send the setAvailability request due to DB reading errors");
            return;
        }

        if ((me.isOnline() == OnlineStatus.ONLINE.ordinal() &&
                ConnectionManager.getInstance().getConnectionState() != STATE_CONNECTED) 
                || !isFirstTimeSyncComplete()) {
          LogUtils.logD("PresenceEngine.initSetMyAvailabilityRequest():"
                  + " return NO NETWORK CONNECTION or not ready");
          return;
        }
        
        Hashtable<String, String> availability = new Hashtable<String, String>();
        
        for (NetworkPresence presence : me.getPayload()) {
            availability.put(SocialNetwork.getPresenceValue(presence.getNetworkId()).toString(),
                    OnlineStatus.getValue(presence.getOnlineStatusId()).toString());
        }

        // set the DB values
        me.setLocalContactId(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper));
        
        updateMyPresenceInDatabase(me);

        addUiRequestToQueue(ServiceUiRequest.SET_MY_AVAILABILITY, availability);
    }

    /**
     * Changes the user's availability and therefore the state of the engine. 
     * Also displays the login notification if necessary.
     * 
     * @param status Availability to set for all identities we have.
     */
    public void setMyAvailability(OnlineStatus status) {
        if (status == null) {
            LogUtils.logE("PresenceEngine setMyAvailability:"
                    + " Can't send the setAvailability request due to DB reading errors");
            return;
        }
        
        LogUtils.logV("PresenceEngine setMyAvailability() called with status:"+status.toString());
        if (ConnectionManager.getInstance().getConnectionState() != STATE_CONNECTED) {
            LogUtils.logD("PresenceEnfgine.setMyAvailability(): skip - NO NETWORK CONNECTION");
            return;
        }
        
        // Get presences
        // TODO: Fill up hashtable with identities and online statuses
        Hashtable<String, String> presenceList = HardcodedUtils.createMyAvailabilityHashtable(status);
        
        User me = new User(String.valueOf(PresenceDbUtils.getMeProfileUserId(mDbHelper)),
                presenceList);
        
        // set the DB values for myself
        me.setLocalContactId(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper));
        updateMyPresenceInDatabase(me);

        // set the engine to run now
        
        addUiRequestToQueue(ServiceUiRequest.SET_MY_AVAILABILITY, presenceList);
    }
        
    /**
     * Changes the user's availability and therefore the state of the engine. 
     * Also displays the login notification if necessary.
     * 
     * @param presence Network-presence to set
     */
    public void setMyAvailability(NetworkPresence presence) {
        if (presence == null) {
            LogUtils.logE("PresenceEngine setMyAvailability:"
                    + " Can't send the setAvailability request due to DB reading errors");
            return;
        }
        
        LogUtils.logV("PresenceEngine setMyAvailability() called with network presence:"+presence.toString());
        if (ConnectionManager.getInstance().getConnectionState() != STATE_CONNECTED) {
            LogUtils.logD("PresenceEnfgine.setMyAvailability(): skip - NO NETWORK CONNECTION");
            return;
        }
        
        ArrayList<NetworkPresence> presenceList = new ArrayList<NetworkPresence>();
        presenceList.add(presence);
        User me = new User(String.valueOf(PresenceDbUtils.getMeProfileUserId(mDbHelper)),
                null);
        me.setPayload(presenceList);
        
        // set the DB values for myself
        me.setLocalContactId(SyncMeDbUtils.getMeProfileLocalContactId(mDbHelper));
        updateMyPresenceInDatabase(me);

        // set the engine to run now
        
        addUiRequestToQueue(ServiceUiRequest.SET_MY_AVAILABILITY, presenceList);
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
        if (ConnectionManager.getInstance().getConnectionState() != STATE_CONNECTED) {
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

    @Override
    public void onConnectionStateChanged(int state) {
        if (mLoggedIn && isFirstTimeSyncComplete()) {
            switch (state) {
                case STATE_CONNECTED:
                    getPresenceList();
                    initSetMyAvailabilityRequest(getMyAvailabilityStatusFromDatabase());
                    break;
                case STATE_CONNECTING:
                case STATE_DISCONNECTED:
                    setPresenceOffline();
                    mFailedMessagesList.clear();
                    mSendMessagesHash.clear();
                    break;
            }    
        }
    }
        

    /**
     * This method gets the availability information for Me Profile from the Presence
     * table and updates the same to the server.
     */
    public final void setMyAvailability() {
        initSetMyAvailabilityRequest(getMyAvailabilityStatusFromDatabase());
    }

    /**
     * Convenience method.
     * Constructs a Hash table object containing My identities mapped against the provided status.
     * @param status Presence status to set for all identities
     * @return The resulting Hash table, is null if no identities are present
     */
    public Hashtable<String, String> getPresencesForStatus(OnlineStatus status) {
        // Get cached identities from the presence engine 
        ArrayList<Identity> identities = 
            EngineManager.getInstance().getIdentityEngine().getMy360AndThirdPartyChattableIdentities();
    
        if(identities == null) {
            // No identities, just return null
            return null;
        }
    
        Hashtable<String, String> presences = new Hashtable<String, String>();
    
        String statusString = status.toString();
        for(Identity identity : identities) {
                presences.put(identity.mNetwork, statusString);
        }
        
        return presences;
    }

}
