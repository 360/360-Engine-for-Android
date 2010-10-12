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

package com.vodafone360.people.service.aidl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.presence.PresenceDbUtils;
import com.vodafone360.people.engine.presence.User;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.service.agent.NetworkAgentState;
import com.vodafone360.people.service.interfaces.IPeopleService;
import com.vodafone360.people.utils.LogUtils;

/**
 * This is an experimental AIDL API, which is currently unsupported by Vodafone
 * and disabled by default.
 *
 * Service is used for all third party interaction with the 360 Engines.  Usage
 * is best explained with a code example, but the short story is that this
 * class (once bound) tries to provide all the methods available from the
 * IPeopleService.
 */
public class IdlPeopleInterface extends Service {

    /** Reference to MainApplication. **/
    private MainApplication mApplication;
    /** Reference to IPeopleService. **/
    private IPeopleService mPeopleService;
    /**
     * Map of IDatabaseSubscriber listeners.  We store a list of listeners with
     * unique string identifiers, as there are problems removing these from the
     * list otherwise.
     **/
    private Map<String, IDatabaseSubscriber> mListeners;
    /**
     * Need a direct handle on the database for certain lookup functions.
     */
    private SQLiteDatabase mDatabase;

    @Override
    public final void onCreate() {
        super.onCreate();
        LogUtils.logI("IdlPeopleInterface.onCreate() "
                + "Initialising IPC for com.vodafone360.people");

        if (!SettingsManager.getBooleanProperty(Settings.ENABLE_AIDL_KEY)) {
            LogUtils.logW("IdlPeopleInterface.onCreate() "
                    + "AIDL is disabled at built time");
            return;
        }

        /** Start the Service. **/
        startService(new Intent(this, RemoteService.class));

        mApplication = (MainApplication) getApplication();
        mPeopleService = mApplication.getServiceInterface();
        if (mPeopleService == null) {
            LogUtils.logV("IdlPeopleInterface.onCreate() "
                    + "Waiting for service to load");
            mApplication.registerServiceLoadHandler(mServiceLoadedHandler);
        }

        if (mListeners == null) {
            mListeners = new HashMap<String, IDatabaseSubscriber>();
        }
    }


    /**
     * Handler so we know when the IPeopleService has been started.
     */
    private Handler mServiceLoadedHandler = new Handler() {
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            mApplication.unRegisterServiceLoadHandler();
            onServiceLoaded();
        }
    };

    /***
     * Initialise now that the service has finally loaded.
     */
    private void onServiceLoaded() {
        LogUtils.logV("IdlPeopleInterface.onServiceLoaded() Service loaded");
        if (!SettingsManager.getBooleanProperty(Settings.ENABLE_AIDL_KEY)) {
            LogUtils.logI("IdlPeopleInterface.onStart() "
                    + "AIDL disabled at build time");
            return;
        }
        mPeopleService = mApplication.getServiceInterface();
        mDatabase = mApplication.getDatabase().getReadableDatabase();
        onStartListenerActivity();
    }

    /***
     * Destroy the interface.
     *
     * TODO: Notify listeners that the interface has been destroyed.
     */
    @Override
    public final void onDestroy() {
        super.onDestroy();

        mPeopleService.unsubscribe(mHandler);
        mPeopleService.removeEventCallback(mHandler);
    }

    /***
     * Start the IdlPeopleInterface service.
     *
     * @param intent Starting intent.
     * @param startId Starting identifier.
     */
    @Override
    public final void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);

        if (!SettingsManager.getBooleanProperty(Settings.ENABLE_AIDL_KEY)) {
            LogUtils.logI("IdlPeopleInterface.onStart() "
                    + "AIDL disabled at build time");
            return;
        }

        if (mPeopleService != null) {
            onStartListenerActivity();
        } else {
            LogUtils.logW("IdlPeopleInterface.onStart() "
                    + "People service should not be NULL, waiting for it to "
                    + "start");
            mApplication.registerServiceLoadHandler(mServiceLoadedHandler);
        }
    }

    /***
     * Subscribe listener activities, so we get notifications whenever
     * anything happens in the engine.
     */
    private void onStartListenerActivity() {
        mPeopleService.subscribe(mHandler, -1L, true);
        mPeopleService.addEventCallback(mHandler);

        LogUtils.logI("IdlPeopleInterface.startListenerActivity() Notifying "
                + "listeners that the service is now ready to receive AIDL "
                + "calls");

        for (Iterator<String> subscribersIterator
                = mListeners.keySet().iterator();
        subscribersIterator.hasNext();) {
            final String listenerIdentifier = subscribersIterator.next();
            final IDatabaseSubscriber listener
            = mListeners.get(listenerIdentifier);
            try {
                listener.onServiceReady();
            } catch (RemoteException e) {
                LogUtils.logW("IdlPeopleInterface.startListenerActivity() "
                        + "Got a RemoteException trying to contact ["
                        + listenerIdentifier + "][" + listener
                        + "] Removing from list of listeners");
                subscribersIterator.remove();
            }
        }
    }

    /**
     * Handler for incoming Engine messages.  All messages are immediately
     * passed on to listening third parties.
     */
    private Handler mHandler = new Handler() {
        /**
         *  We want to handle incoming events from the main engine code by
         *  passing them out to all 3rdParty listeners via AIDL.
         *
         *  Two things to note:
         *  <li> This method is Synchronized - there are potentially many messages
         *  coming in from different engines. At the moment, they come from a single
         *  thread, but for maintenance reasons this handling is thread-safe, so that
         *  if a threaded mechanism is implemented in future we shouldn't have
         *  problem here.
         *  <li> The code is actually very simple - it runs through the list of
         *  listeners and directly passes out the Message. This is possible since
         *  the Message class implements the Parcelable interface, and so is meant
         *  to be very happy to be passed around in the way. One problem with this
         *  process as it stands (and this accounts for the presence of the third
         *  catch clause below) is that somewhere we seem to be putting Non-Parcelable
         *  objects into these Messages - so while we are "guaranteed" the ability
         *  to pass Messages directly via AIDL by the Parcelable interface, this
         *  isn't always the case.
         *
         *  @param message - the message being sent to us by the engine to notify
         *  us of some event.
         */
        @Override
        public synchronized void handleMessage(final Message message) {
            super.handleMessage(message);
            LogUtils.logI("IdlPeopleInterface.handleMessage() Got a new "
                    + "service message, sending it to subscribers - Message["
                    + message.toString() + "]");

            /*
             * Also synchronise over the list of listeners, as the put/remove
             * code for this list can be called from any number of external
             * threads via AIDL.
             */
            synchronized (mListeners) {
                for (Iterator<String> subscribersIterator = mListeners.keySet().iterator(); 
                subscribersIterator.hasNext();) {
                    /**
                     * Can't use iterator shorthand, as we might need to remove
                     * things from the list.
                     */
                    final String listenerIdentifier = subscribersIterator.next();
                    final IDatabaseSubscriber listener = mListeners.get(listenerIdentifier);


                    LogUtils.logI("IdlPeopleInterface.handleMessage() "
                            + "Sending message to [" + listenerIdentifier + "]");
                    try {
                        //Pass the message out via IDL
                        listener.handleEvent(message);

                    } catch (DeadObjectException e) {
                        LogUtils.logE("IdlPeopleInterface.handleMessage() "
                                + "DeadObjectException while trying to contact ["
                                + listenerIdentifier + "][" + listener
                                + "] - Removing from list of listeners", e);
                        subscribersIterator.remove();
                    } catch (RemoteException e){
                        LogUtils.logE("IdlPeopleInterface.handleMessage() "
                                + "RemoteException while contacting listener", e);
                    } catch (NullPointerException e){
                        LogUtils.logE("IdlPeopleInterface.handleMessage() "
                                + "NullPointerException while contacting" +
                                " listenter. Possibly they have the wrong" +
                                " dependencies.", e);
                    } catch (java.lang.RuntimeException e) {
                        /** Something in the message was not Parcelable. **/
                        if ("Can't marshal non-Parcelable objects across processes."
                                .equals( e.getLocalizedMessage() )) {
                            /**
                             * Someone has packed something into the message
                             * that isn't Parcelable.
                             * TODO: Address why we're putting non-Parcelable
                             * objects into a Message (which is meant to
                             * implement the Parcelable class).
                             **/
                            LogUtils.logE("IdlPeopleInterface.handleMessage() "
                                    + "Tried to martial non-parcelable object", e);
                        } else {
                            /** Throw the Exception anyway. **/
                            LogUtils.logE("IdlPeopleInterface.handleMessage() "
                                    + "RuntimeException", e);
                            throw(e);
                        }
                    }
                }
            }
        }
    };

    @Override
    public final boolean onUnbind(final Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * onBind is the function called when you try to get a handle on
     * and AIDL service.
     *
     * The documentation at:
     * http://developer.android.com/guide/topics/fundamentals.html
     *
     * recommends that onBind be implemented in a thread-safe manner,
     * so the method is synchronized.
     *
     * @param intent Calling Intent.
     */
    @Override
    public final synchronized IBinder onBind(final Intent intent) {
        LogUtils.logV("IdlPeopleInterface.onBind()");

        if (SettingsManager.getBooleanProperty(Settings.ENABLE_AIDL_KEY)) {
            LogUtils.logV("IdlPeopleInterface.onBind() "
                    + "AIDL interface enabled, so passing a "
                    + "IPeopleSubscriptionService back to binder");
            return new IPeopleSubscriptionService();

        } else {
            LogUtils.logV("IdlPeopleInterface.onBind() "
                    + "AIDL service disabled at build time");
            return null;
        }
    }

    /**
     * The idea is that what we're exposing via AIDL should be as close as
     * possible to the IPeopleService interface.  Clients should be able to
     * access the services as if they were internal.
     *
     * This is set this up to "implement" the IPeopleService - of course it
     * does this simply by forwarding requests to a IPeopleService service
     * which takes the action.  The reason for this model is that this service
     * must track any and all changes made to the original service, so that
     * clients can continue to integrate with the latest changes.
     * 
     * This class is defined in four sections:
     * <li> Functions currently implemented & available through AIDL.
     * <li> Functions implemented, but available in a slightly different
     *          form via AIDL.
     * <li> Functions not yet implemented.
     * <li> Functions that will not be implemented in AIDL.
     *
     * Except where otherwise noted, all methods perform exactly the same
     * operations as the equivalents in
     * com.vodafone360.people.interfaces.IPeopleService
     */

    public class IPeopleSubscriptionService extends
    IDatabaseSubscriptionService.Stub implements IPeopleService,
    ThirdPartyUtils {

        /**
         * Subscribe to find out about any events the 360 Services would like
         * the UI to know about (and thus we'd like the client to know about).
         * This provides a push notification system for clients to hear about
         * interesting things going on.
         *
         * Returns:
         * <li> true if the service is ready to receive requests,
         * <li> false otherwise. Subscribers will be notified when the service
         *  is ready with a call to their "onServiceReady()" function.
         *
         * @param identifier Unique subscriber ID.
         * @param subscriber Subscriber IDatabaseSubscriber interface.
         * @return TRUE if the PeopleService is already ready.
         */
        @Override
        public final boolean subscribe(final String identifier,
                final IDatabaseSubscriber subscriber) {
            LogUtils.logV("IdlPeopleInterface.subscribe() "
                    + "Adding subscriber to get push notifications");
            
            if (identifier == null || subscriber == null) {
                // Null is not a valid option for either argument
                return false;
            }

            boolean boundAndReady = false;

            // In case anyone else is also adding/removing/iterating
            synchronized (mListeners) {
                mListeners.put(identifier, subscriber);
            }
            if (mPeopleService == null) {
                LogUtils.logW("IdlPeopleInterface.subscribe() "
                        + "PeopleService was NULL while while trying to add ["
                        + subscriber + "] to the list of subscribers.");
                LogUtils.logW("IdlPeopleInterface.subscribe() Starting service"
                        + " - will call back the listener when ready.");
                mApplication.registerServiceLoadHandler(mServiceLoadedHandler);
                boundAndReady = false;
            } else {
                LogUtils.logI("IdlPeopleInterface.subscribe() Service ready. "
                        + "It's safe for [" + identifier + "] to make calls.");

                /**
                 * Subscribe every time because IPeopleService is currently
                 * only capable of handling one subscriber.
                 */
                mPeopleService.subscribe(mHandler, -1L, true);
                boundAndReady = true;
            }

            return boundAndReady;

        }

        /**
         * Always call this when you want to finish, otherwise we have a memory
         * leak (among other things).
         *
         * @param identifier Some unique identifier for your subscriber.
         * @return TRUE if unsubscribe was successful.  Only really returns
         *          FALSE if you couldn't be found in the list of subscribers.
         */
        @Override
        public final boolean unsubscribe(final String identifier) {
            boolean managedToUnsubscribe = false;

            /**
             * Code is synchronised in case anyone else is also adding,
             * removing or iterating.
             */
            synchronized (mListeners) {
                if (mListeners.remove(identifier) != null) {
                    LogUtils.logI("IdlPeopleInterface.unsubscribe()"
                            + "Successfully unsubscribed");
                    managedToUnsubscribe = true;
                } else {
                    LogUtils.logI("IdlPeopleInterface.unsubscribe() "
                            + "Got unsubscribe call, but did not find the "
                            + "identifier in the list");
                    managedToUnsubscribe = false;
                }
            }

            return managedToUnsubscribe;
        }

        /**
         * TODO: Currently doing a NullPointer check. Need to file a bug report
         * about this;  occasionally getting NullPointer exceptions when this
         * is called, and it's not obvious why. Considering adding similar
         * checks elsewhere since exceptions aren't propagated via AIDL - they
         * just crash the service.
         */
        @Override
        public final void checkForUpdates() {
            LogUtils.logI("IdlPeopleInterface.checkForUpdates()");
            try {
                mPeopleService.checkForUpdates();
            } catch (NullPointerException e) {
                LogUtils.logE("IdlPeopleInterface.checkForUpdates()"
                        + "NullPointerException mPeopleService["
                        + mPeopleService + "]", e);
            }
        }

        /**
         * @see com.vodafone360.people.service.interfaces.deleteIdentity()
         */
        @Override
        public final void deleteIdentity(final String network,
                final String identityId) {
            mPeopleService.deleteIdentity(network, identityId);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.downloadMeProfileFirstTime()
         */
        @Override
        public final void downloadMeProfileFirstTime() {
            mPeopleService.downloadMeProfileFirstTime();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.fetchPrivacyStatementy()
         */
        @Override
        public final void fetchPrivacyStatement() {
            mPeopleService.fetchPrivacyStatement();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.fetchTermsOfService()
         */
        @Override
        public final void fetchTermsOfService() {
            mPeopleService.fetchTermsOfService();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.fetchUsernameState()
         */
        @Override
        public final void fetchUsernameState(final String username) {
            mPeopleService.fetchUsernameState(username);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getAvailableThirdPartyIdentities()
         */
        @Override
        public final ArrayList<Identity> getAvailableThirdPartyIdentities() {
            return mPeopleService.getAvailableThirdPartyIdentities();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getLoginRequired()
         */
        @Override
        public final boolean getLoginRequired()  {
            return mPeopleService.getLoginRequired();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getMoreTimelines()
         */
        @Override
        public final void getMoreTimelines() {
            mPeopleService.getMoreTimelines();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getMy360AndThirdPartyChattableIdentities()
         */
        @Override
        public final ArrayList<Identity> getMy360AndThirdPartyChattableIdentities() {
            return mPeopleService.getMy360AndThirdPartyChattableIdentities();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getMyThirdPartyIdentities()
         */
        @Override
        public final ArrayList<Identity> getMyThirdPartyIdentities() {
            return mPeopleService.getMyThirdPartyIdentities();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getOlderStatuses()
         */
        @Override
        public final void getOlderStatuses() {
            mPeopleService.getOlderStatuses();
        }
        
        /**
         * @see com.vodafone360.people.database.tables
         */
        public int getPresence(final long localContactID){
            return com.vodafone360.people.database.tables.ContactSummaryTable.
                        getPresence(localContactID).ordinal();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getPresenceList()
         */
        @Override
        public final void getPresenceList(final long contactId) {
            mPeopleService.getPresenceList(contactId);

        }

        /**
         * @see com.vodafone360.people.service.interfaces.getRoamingDeviceSetting()
         */
        @Override
        public final boolean getRoamingDeviceSetting() {
            return mPeopleService.getRoamingDeviceSetting();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getRoamingNotificationType()
         */
        @Override
        public final int getRoamingNotificationType() {
            return mPeopleService.getRoamingNotificationType();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.getStatuses()
         */
        @Override
        public final void getStatuses() {
            mPeopleService.getStatuses();
        }

        /**
         * Return user presence status.
         * Extra method provide to allow easy grabbing of user presence info.
         *
         * @param localContactId ID for contact.
         * @return User presence.
         */
        public User getUserPresenceStatusByLocalContactId(long localContactId) {
            LogUtils.logV("IdlPeopleInterface.getUserPresenceStatusByLocalContactId() "
                    + "(localContactId: " + localContactId + ")");
            return PresenceDbUtils.getUserPresenceStatusByLocalContactId(
                    localContactId, mApplication.getDatabase());
        }

        /***
         * @param loginDetails for user
         * @see com.vodafone360.people.service.interfaces.logon()
         */
        @Override
        public final void logon(final LoginDetails loginDetails) {
            mPeopleService.logon(loginDetails);
        }

        /***
         * This is a workaround - The client must pass in the ordinal of the
         * appropriate ENUM from PersistSettings.InternetAvail().  However the
         * internal classes and AIDL don't work together correctly.
         */
        public void notifyDataSettingChanged(int internetAvail) {
            notifyDataSettingChanged(
                    PersistSettings.InternetAvail.values()[internetAvail]);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.pingUserActivity()
         */
        @Override
        public final void pingUserActivity() {
            mPeopleService.pingUserActivity();
        }

        /***
         * @see com.vodafone360.people.service.interfaces.register()
         */
        @Override
        public final void register(final RegistrationDetails details) {
            mPeopleService.register(details);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.sendMessage()
         */
        @Override
        public final void sendMessage(final long toLocalContactId,
                final String body, final int socialNetworkId) {
            LogUtils.logI("IdlPeopleInterface.sendMessage()");
            try {
                mPeopleService.sendMessage(toLocalContactId, body, socialNetworkId);
            } catch (ArrayIndexOutOfBoundsException e) {
                /*
                 * If the user passes in an invalid socialNetworkId then it can
                 * cause this exception in 
                 * NetworkPresence.SocialNetwork.getPresenceValue(int)
                 * 
                 * (No stack trace as the user doesn't need to know about the
                 * internals in this instance).
                 */
                LogUtils.logE("Tried to send message to invalid social network.");
            }
        }

        /***
         * This is a workaround - The client must pass in the ordinal of the
         * appropriate ENUM from ContactSummary.OnlineStatus().  However the
         * internal classes and AIDL don't work together correctly.
         */
        public void setAvailability(final int status) {
            setAvailability(ContactSummary.OnlineStatus.values()[status]);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setAvailability()
         * Not yet supported, as function overloading is not supported in AIDL.
         */
        @Override
        public final void setAvailability(
                final Hashtable<String, String> status) {
            mPeopleService.setAvailability(status);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setAvailability()
         * Not yet supported, as function overloading is not supported in AIDL.
         */
        public void setAvailability(int network, int status) {
            setAvailability(SocialNetwork.values()[network],
                    OnlineStatus.values()[status]);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setIdentityStatus()
         */
        @Override
        public final void setIdentityStatus(final String network,
                final String identityId, final boolean identityStatus) {
            mPeopleService.setIdentityStatus(network, identityId,
                    identityStatus);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setNewUpdateFrequency()
         */
        @Override
        public final void setNewUpdateFrequency() {
            mPeopleService.setNewUpdateFrequency();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setShowRoamingNotificationAgain()
         */
        @Override
        public final void setShowRoamingNotificationAgain(
                final boolean showAgain) {
            mPeopleService.setShowRoamingNotificationAgain(showAgain);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.startBackgroundContactSync()
         */
        @Override
        public final void startBackgroundContactSync(final long delay) {
        }

        /**
         * @see com.vodafone360.people.service.interfaces.startContactSync()
         */
        @Override
        public final void startContactSync() {
            mPeopleService.startContactSync();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.startStatusesSync()
         */
        @Override
        public final void startStatusesSync() {
            mPeopleService.startStatusesSync();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.updateChatNotification()
         */
        @Override
        public final void updateChatNotification(final long localContactId) {
            mPeopleService.updateChatNotification(localContactId);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.uploadMeProfile()
         */
        @Override
        public final void uploadMeProfile() {
            mPeopleService.uploadMeProfile();
        }

        /**
         * @see com.vodafone360.people.service.interfaces.uploadMyStatus()
         */
        @Override
        public final void uploadMyStatus(final String statusText) {
            mPeopleService.uploadMyStatus(statusText);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.validateIdentityCredentials()
         */
        @Override
        public final void validateIdentityCredentials(final boolean dryRun,
                final String network, final String username,
                final String password, final Bundle identityCapabilityStatus) {
            mPeopleService.validateIdentityCredentials(dryRun, network, username,
                    password, identityCapabilityStatus);
        }

        /*
         * These are methods that we'd like to expose, but are difficult
         * through AIDL (see "substitute" versions above).  The main problem is
         * internal classes which require further investigation.
         */
        @Override
        public final void notifyDataSettingChanged(
                final InternetAvail internetAvail) {
            mPeopleService.notifyDataSettingChanged(internetAvail);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setAvailability()
         */
        @Override
        public final void setAvailability(final OnlineStatus status) {
            mPeopleService.setAvailability(status);
        }

        /**
         * @see com.vodafone360.people.service.interfaces.setAvailability()
         * Not currently working as function overloading is not supported in
         * AIDL.
         */
        public final void setAvailability(final SocialNetwork network,
                final OnlineStatus  status) {
            mPeopleService.setAvailability(network, status);
        }

        /*
         * Below are the methods I've not yet exposed - mostly because either
         * they have non-primitive return types, or non-primitive arguments.
         * Since most of the data types used appear to be parcelable, I'll be
         * moving forward on this hopefully soon.
         *
         * An outstanding issue is that some classes implement the "Parcelable"
         * interface, but fail to provide a "CREATOR" field - which is
         * necessary for them to be dealt with properly in the AIDL.  This
         * functionality could be added to get these methods working.
         */

        /***
         * NetworkAgentState does not implement the Parcelable interface.
         * Unless we change this, there is no chance for this function to be
         * exposed via AIDL in its current form.
         */
        @Override
        public final NetworkAgentState getNetworkAgentState() {
            // Not exposed via AIDL.
            return null;
        }


        /***
         * NetworkAgentState does not implement the Parcelable interface.
         * Unless we change this, there is no chance for this function to be
         * exposed via AIDL in its current form.
         */
        @Override
        public void setNetworkAgentState(final NetworkAgentState state) {
            // Not exposed via AIDL.
        }

        /***
         * Because of the different mechanism for subscription/unsubscription
         * implemented by this service, these methods will remain unimplemented.
         * (When a client "subscribes" they get *all* notifications).
         */
        @Override
        public void addEventCallback(final Handler uiHandler) {
            // Not exposed via AIDL.
        }

        @Override
        public void removeEventCallback(final Handler uiHandler) {
            // Not exposed via AIDL.
        }

        @Override
        public void subscribe(final Handler handler, final Long contactId,
                final boolean chat) {
            // Not exposed via AIDL.
        }

        @Override
        public void unsubscribe(final Handler handler) {
            // Not exposed via AIDL.
        }
    }
}
