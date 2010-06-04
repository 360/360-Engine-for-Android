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

package com.vodafone360.people.service.agent;

import java.security.InvalidParameterException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.Intents;
import com.vodafone360.people.MainApplication;
import com.vodafone360.people.engine.upgrade.UpgradeStatus;
import com.vodafone360.people.engine.upgrade.VersionCheck;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.transport.ConnectionManager;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.WidgetUtils;

/**
 * The UiAgent is aware when any "Live" Activities are currently on screen, and
 * contains business logic for sending unsolicited messages to the UI. This is
 * useful for knowing when to send chat messages, presence updates,
 * notifications, error messages, etc to an on screen Activity.
 */
public class UiAgent {
    public final static int UI_AGENT_NOTIFICATION_ID = 1;

    private MainApplication mMainApplication;

    /*
     * Store for pending UiEvents while no People Activities are currently on
     * screen.
     */
    private ServiceUiRequest mUiEventQueue = null;

    private Bundle mUiBundleQueue = null;

    /*
     * Handler object from a subscribed UI Activity.
     */
    private Handler mHandler;

    /*
     * Local ID of the contact the Handler is tracking (-1 when tracking all).
     */
    private long mLocalContactId;

    /*
     * TRUE if the subscribed UI Activity expects chat messages.
     */
    private boolean mChat;

    /*
     * TRUE if message is new and a noisy Notification message should play.
     */
    private boolean mNewMessage = false;
    
    private Context mContext = null;

    /**
     * Constructor.
     */
    public UiAgent(MainApplication mainApplication, Context context) {
        mMainApplication = mainApplication;
        mContext = context;
        mHandler = null;
        mLocalContactId = -1;
        mChat = false;
    }

    /***
     * Send an unsolicited UI Event to the UI. If there are no on screen
     * Activities, then queue the message for later. The queue is of size one,
     * so higher priority messages will simply overwrite older ones.
     * 
     * @param uiEvent Event to send.
     * @param bundle Optional Bundle to send to UI, usally set to NULL.
     * @throws InvalidParameterException UiEvent is NULL.
     */
    public void sendUnsolicitedUiEvent(ServiceUiRequest uiEvent, Bundle bundle) {
        if (uiEvent == null) {
            throw new InvalidParameterException("UiAgent.sendUnsolicitedUiEvent() "
                    + "UiEvent cannot be NULL");
        }

        LogUtils.logW("UiAgent.sendUnsolicitedUiEvent() uiEvent[" + uiEvent.name() + "]");

        if (mHandler != null) {
            /*
             * Send now.
             */
            LogUtils.logV("UiAgent.sendUnsolicitedUiEvent() Sending uiEvent[" + uiEvent + "]");
            mHandler.sendMessage(mHandler.obtainMessage(uiEvent.ordinal(), bundle));

        } else {
            /*
             * Send later.
             */
            if (mUiEventQueue == null || uiEvent.ordinal() < mUiEventQueue.ordinal()) {
                LogUtils.logV("UiAgent.sendUnsolicitedUiEvent() Sending uiEvent[" + uiEvent.name()
                        + "] later");
                mUiEventQueue = uiEvent;
                mUiBundleQueue = bundle;
            } else {
                LogUtils.logV("UiAgent.sendUnsolicitedUiEvent() Ignoring uiEvent[" + uiEvent.name()
                        + "], as highter priority UiEvent[" + mUiEventQueue.name()
                        + "] is already pending");
            }
        }
    }

    /**
     * Subscribes a UI Handler to receive unsolicited events.
     * 
     * @param handler - UI handler to receive unsolicited events.
     * @param localContactId Provide a local contact ID to receive updates for
     *            the given contact only.
     * @param localContactId Set this to -1 to receive updates for every
     *            contact.
     * @param localContactId Set this to NULL not to receive contact updates.
     * @param chat - TRUE if the Handler expects chat messages
     * @throws NullPointerException Handler must not be NULL
     */
    public void subscribe(Handler handler, Long localContactId, boolean chat) {
        LogUtils.logV("UiAgent.subscribe() handler[" + handler + "] localContactId["
                + localContactId + "] chat[" + chat + "]");
        if (handler == null) {
            throw new NullPointerException("UiAgent.subscribe() Handler cannot be NULL");
        }

        mHandler = handler;
        mLocalContactId = localContactId;
        mChat = chat;
        if (mChat) {
            updateChatNotification();
        }

        if (mUiEventQueue != null) {
            LogUtils.logV("UiAgent.subscribe() Send pending uiEvent[" + mUiEventQueue + "]");
            mHandler.sendMessage(mHandler.obtainMessage(mUiEventQueue.ordinal(), mUiBundleQueue));
            mUiEventQueue = null;
            mUiBundleQueue = null;
        }

        UpgradeStatus upgradeStatus = new VersionCheck(mMainApplication.getApplicationContext(),
                false).getCachedUpdateStatus();
        if (upgradeStatus != null) {
            sendUnsolicitedUiEvent(ServiceUiRequest.UNSOLICITED_DIALOG_UPGRADE, null);
        }

        ConnectionManager.getInstance().notifyOfUiActivity();
    }

    /**
     * This method ends the UI Handler's subscription. This will have no effect
     * if a different handler is currently subscribed.
     * 
     * @param handler - UI handler to no longer receive unsolicited events.
     * @throws NullPointerException Handler must not be NULL
     */
    public void unsubscribe(Handler handler) {
        if (handler == null) {
            throw new NullPointerException("UiAgent.unsubscribe() Handler cannot be NULL.");
        }
        if (handler != mHandler) {
            LogUtils.logW("UiAgent.unsubscribe() Activity is trying to unsubscribe with a "
                    + "different handler");
        } else {
            mHandler = null;
            mLocalContactId = -1;
            mChat = false;
        }
    }

    /**
     * This method returns the local ID of the contact the HandlerAgent is
     * tracking.
     * 
     * @return LocalContactId of the contact the HandlerAgent is tracking
     */
    public long getLocalContactId() {
        return mLocalContactId;
    }

    /**
     * Returns TRUE if an Activity is currently listening out for unsolicited
     * events (i.e. a "Live" activity is currently on screen).
     * 
     * @return TRUE if any subscriber is listening the presence state/chat
     *         events
     */
    public boolean isSubscribed() {
        return mHandler != null;
    }

    /**
     * Returns TRUE if an Activity is currently listening out for unsolicited
     * events (i.e. a "Live" activity is currently on screen).
     * 
     * @return TRUE if any subscriber is listening the presence chat events
     */
    public boolean isSubscribedWithChat() {
        return mHandler != null && mChat;
    }

    /**
     * This method is called by the Presence engine to notify a subscribed
     * Activity of updates.
     * 
     * @param contactId Update an Activity that shows this contact ID only.
     * @param contactId Set this to -1 to send updates relevant to all contacts.
     */
    public void updatePresence(long contactId) {
        WidgetUtils.kickWidgetUpdateNow(mMainApplication);

        if (mHandler != null) {
            if (mLocalContactId == -1 || mLocalContactId == contactId) {
                mHandler.sendMessage(mHandler.obtainMessage(ServiceUiRequest.UNSOLICITED_PRESENCE
                        .ordinal(), null));
            } else {
                LogUtils.logV("UiAgent.updatePresence() No Activities are interested in "
                        + "contactId[" + contactId + "]");
            }
        } else {
            LogUtils.logW("UiAgent.updatePresence() No subscribed Activities");
        }
    }

    /***
     * Notifies an on screen Chat capable Activity of a relevant update. If the
     * wrong Activity is on screen, this update will be shown as a Notification.
     * 
     * @param contactId Update an Activity that shows Chat information for this
     *            localContact ID only.
     */
    public void updateChat(long contactId) {
        if (mHandler != null && mChat && mLocalContactId == contactId) {
            LogUtils.logV("UiAgent.updateChat() Send message to UI (i.e. update the screen)");
            mHandler.sendMessage(mHandler.obtainMessage(
                    ServiceUiRequest.UNSOLICITED_CHAT.ordinal(), null));
            /*
             * Note: Do not update the chat notification at this point, as the
             * UI must update the database (e.g. mark messages as read) before
             * calling subscribe() to trigger a refresh.
             */

        } else if (mHandler != null && mChat && mLocalContactId == -1) {
            LogUtils.logV("UiAgent.updateChat() Send message to UI (i.e. update the screen) and "
                    + "do a noisy notification");
            /*
             * Note: this was added because TimelineListActivity listens to all
             * localIds (i.e. localContactId = -1)
             */
            mHandler.sendMessage(mHandler.obtainMessage(
                    ServiceUiRequest.UNSOLICITED_CHAT.ordinal(), null));
            mNewMessage = true;
            updateChatNotification();

        } else {
            LogUtils.logV("UiAgent.updateChat() Do a noisy notification only");
            mNewMessage = true;
            updateChatNotification();
        }
    }

    /***
     * Update the Notification bar with Chat information directly from the
     * database.
     */
    private void updateChatNotification() {
        
        Intent i = new Intent();
        
        i.setAction(Intents.NEW_CHAT_RECEIVED);
        i.putExtra(ApplicationCache.sIsNewMessage, mNewMessage);

        mContext.sendBroadcast(i);
    }

}
