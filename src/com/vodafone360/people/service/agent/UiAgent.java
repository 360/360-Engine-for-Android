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
    /** UI Notification identifier. **/
    public static final int UI_AGENT_NOTIFICATION_ID = 1;

    /** Pointer to MainApplication. **/
    private MainApplication mMainApplication;

    /**
     * Store for pending UiEvents while no People Activities are currently on
     * screen.
     */
    private ServiceUiRequest mUiEventQueue = null;

    /**
     * Store for pending UiEvent Bundles while no People Activities are
     * currently on screen.
     */
    private Bundle mUiBundleQueue = null;

    /**
     * Handler object from a subscribed UI Activity.  Note: This object is
     * nullified by a separate thread and is thereby declared volatile.
     */
    private volatile Handler mHandler;

    /**
     * Local ID of the contact the Handler is tracking (-1 when tracking all).
     */
    private long mLocalContactId;

    /** TRUE if the subscribed UI Activity expects chat messages. **/
    private boolean mShouldHandleChat;

    /** Reference to Android Context. **/
    private Context mContext = null;
    
    /**
     * This constant is  used to indicate the UiAgent is now subscribed 
     * to refresh events related to all local contact ids. 
     */
    public static final int ALL_USERS = -1;
    

    /**
     * Constructor.
     *
     * @param mainApplication Pointer to MainApplication.
     * @param context Reference to Android Context.
     */
    public UiAgent(final MainApplication mainApplication,
            final Context context) {
        mMainApplication = mainApplication;
        mContext = context;
        mHandler = null;
        mLocalContactId = -1;
        mShouldHandleChat = false;
    }

    /***
     * Send an unsolicited UI Event to the UI. If there are no on screen
     * Activities, then queue the message for later. The queue is of size one,
     * so higher priority messages will simply overwrite older ones.
     *
     * @param uiEvent Event to send.
     * @param bundle Optional Bundle to send to UI, usually set to NULL.
     */
    public final void sendUnsolicitedUiEvent(final ServiceUiRequest uiEvent,
            final Bundle bundle) {
        if (uiEvent == null) {
            throw new InvalidParameterException("UiAgent."
                    + "sendUnsolicitedUiEvent() UiEvent cannot be NULL");
        }

        LogUtils.logW("UiAgent.sendUnsolicitedUiEvent() uiEvent["
                + uiEvent.name() + "]");

        if (mHandler != null) {
            /*
             * Send now.
             */
            try {
                mHandler.sendMessage(mHandler.obtainMessage(uiEvent.ordinal(),
                        bundle));
                LogUtils.logV("UiAgent.sendUnsolicitedUiEvent() "
                        + "Sending uiEvent[" + uiEvent + "]");

            } catch (NullPointerException e) {
                LogUtils.logW("UiAgent.sendUnsolicitedUiEvent() Caught a race "
                        + "condition where mHandler was set to NULL after "
                        + "being explicitly checked");
                /** Send later anyway. **/
                addUiEventToQueue(uiEvent, bundle);
            }

        } else {
            /*
             * Send later.
             */
            addUiEventToQueue(uiEvent, bundle);
        }
    }

    /***
     * Add the given UiEvent and Bundle pair to the send later queue.
     *
     * @param uiEvent ServiceUiRequest to queue.
     * @param bundle Bundle to queue.
     */
    private void addUiEventToQueue(final ServiceUiRequest uiEvent,
            final Bundle bundle) {
        if (mUiEventQueue == null
                || uiEvent.ordinal() < mUiEventQueue.ordinal()) {
            LogUtils.logV("UiAgent.sendUnsolicitedUiEvent() Sending uiEvent["
                    + uiEvent.name() + "] later");
            mUiEventQueue = uiEvent;
            mUiBundleQueue = bundle;
        } else {
            LogUtils.logV("UiAgent.sendUnsolicitedUiEvent() Ignoring uiEvent["
                    + uiEvent.name() + "], as highter priority UiEvent["
                    + mUiEventQueue.name() + "] is already pending");
        }
    }

    /**
     * Subscribes a UI Handler to receive unsolicited events.
     *
     * @param handler UI handler to receive unsolicited events.
     * @param localContactId Provide a local contact ID to receive updates for
     *            the given contact only, or set this to -1 to receive updates
     *            for every contact, or set this to NULL not to receive contact
     *            updates.
     * @param shouldHandleChat TRUE if the Handler expects chat messages.
     */
    public final void subscribe(final Handler handler,
            final Long localContactId, final boolean shouldHandleChat) {
        LogUtils.logV("UiAgent.subscribe() handler[" + handler
                + "] localContactId[" + localContactId + "] chat[" + shouldHandleChat
                + "]");
        if (handler == null) {
            throw new NullPointerException("UiAgent.subscribe()"
                    + "Handler cannot be NULL");
        }

        mHandler = handler;
        mLocalContactId = localContactId;
        mShouldHandleChat = shouldHandleChat;
        if (mShouldHandleChat) {
            updateChatNotification(false);
        }

        if (mUiEventQueue != null) {
            LogUtils.logV("UiAgent.subscribe() Send pending uiEvent["
                    + mUiEventQueue + "]");
            mHandler.sendMessage(mHandler.obtainMessage(
                    mUiEventQueue.ordinal(), mUiBundleQueue));
            mUiEventQueue = null;
            mUiBundleQueue = null;
        }

        UpgradeStatus upgradeStatus = new VersionCheck(
                mMainApplication.getApplicationContext(),
                false).getCachedUpdateStatus();
        if (upgradeStatus != null) {
            sendUnsolicitedUiEvent(ServiceUiRequest.UNSOLICITED_DIALOG_UPGRADE,
                    null);
        }

        ConnectionManager.getInstance().notifyOfUiActivity();
    }

    /**
     * This method ends the UI Handler's subscription. This will have no effect
     * if a different handler is currently subscribed.
     *
     * @param handler - UI handler to no longer receive unsolicited events.
     */
    public final void unsubscribe(final Handler handler) {
        if (handler == null) {
            throw new NullPointerException("UiAgent.unsubscribe() Handler"
                    + "cannot be NULL.");
        }
        if (handler != mHandler) {
            LogUtils.logW("UiAgent.unsubscribe() Activity is trying to "
                    + "unsubscribe with a different handler");
        } else {
            mHandler = null;
            mLocalContactId = -1;
            mShouldHandleChat = false;
        }
    }

    /**
     * This method returns the local ID of the contact the HandlerAgent is
     * tracking.
     *
     * @return LocalContactId of the contact the HandlerAgent is tracking
     */
    public final long getLocalContactId() {
        return mLocalContactId;
    }

    /**
     * Returns TRUE if an Activity is currently listening out for unsolicited
     * events (i.e. a "Live" activity is currently on screen).
     *
     * @return TRUE if any subscriber is listening the presence state/chat
     *         events
     */
    public final boolean isSubscribed() {
        return mHandler != null;
    }

    /**
     * Returns TRUE if an Activity is currently listening out for unsolicited
     * events (i.e. a "Live" activity is currently on screen).
     *
     * @return TRUE if any subscriber is listening the presence chat events
     */
    public final boolean isSubscribedWithChat() {
        return mHandler != null && mShouldHandleChat;
    }

    /**
     * This method is called by the Presence engine to notify a subscribed
     * Activity of updates.
     *
     * @param contactId Update an Activity that shows this contact ID only, or
     *        set this to ALL_USERS to send updates relevant to all contacts.
     */
    public final void updatePresence(final long contactId) {
        WidgetUtils.kickWidgetUpdateNow(mMainApplication);

        if (mHandler != null) {
            if (mLocalContactId == -1 || mLocalContactId == contactId) {
                mHandler.sendMessage(mHandler.obtainMessage(
                        ServiceUiRequest.UNSOLICITED_PRESENCE.ordinal(),
                        null));
            } else {
                LogUtils.logV("UiAgent.updatePresence() No Activities are "
                        + "interested in contactId[" + contactId + "]");
            }
        } else {
            LogUtils.logW("UiAgent.updatePresence() No subscribed Activities");
        }
    }

    /**
     * <p>Notifies an on screen Chat capable Activity of a relevant update. If the
     * wrong Activity is on screen, this update will be shown as a Notification.</p>
     * 
     * <p><b>Please note that this method takes care of notification for adding AND 
     * removing chat message notifications</b></p>!
     *
     * @param contactId Update an Activity that shows Chat information for this
     *            localContact ID only.
     * @param isNewChatMessage True if this is a new chat message that we are notifying
     * for. False if we are for instance just deleting a notification.
     */
    public final void updateChat(final long contactId, final boolean isNewChatMessage) {
        if (mHandler != null && mShouldHandleChat && mLocalContactId == contactId) {
            LogUtils.logV("UiAgent.updateChat() Send message to UI (i.e. "
                    + "update the screen)");
            mHandler.sendMessage(mHandler.obtainMessage(
                    ServiceUiRequest.UNSOLICITED_CHAT.ordinal(), null));
            /*
             * Note: Do not update the chat notification at this point, as the
             * UI must update the database (e.g. mark messages as read) before
             * calling subscribe() to trigger a refresh.
             */

        } else if (mHandler != null && mShouldHandleChat && mLocalContactId == -1) {
            LogUtils.logV("UiAgent.updateChat() Send message to UI (i.e. "
                    + "update the screen) and do a noisy notification");
            /*
             * Note: this was added because TimelineListActivity listens to all
             * localIds (i.e. localContactId = -1)
             */
            mHandler.sendMessage(mHandler.obtainMessage(
                    ServiceUiRequest.UNSOLICITED_CHAT.ordinal(), null));
            updateChatNotification(isNewChatMessage);

        } else {
            LogUtils.logV("UiAgent.updateChat() Do a noisy notification only");
            updateChatNotification(isNewChatMessage);
        }
    }

    /***
     * Update the Notification bar with Chat information directly from the
     * database.
     * 
     * @param isNewChatMessage True if we have a new chat message and want to
     * display a noise and notification, false if we for instance just delete a
     * chat message or update a multiple-user notification silently.
     * 
     */
    private void updateChatNotification(final boolean isNewChatMessage) {
        mContext.sendBroadcast(new Intent(Intents.NEW_CHAT_RECEIVED).putExtra(
                ApplicationCache.sIsNewMessage, isNewChatMessage));
    }
}
