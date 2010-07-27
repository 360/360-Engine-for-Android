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

package com.vodafone360.people.service.interfaces;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.presence.NetworkPresence;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.NetworkAgentState;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.PhotoUtilsIn;
import com.vodafone360.people.utils.AlbumUtilsIn;

/***
 * @see com.vodafone360.people.engine.BaseEngine.IEngineEventCallback
 * @see com.vodafone360.people.service.interfaces.IPeopleService
 */
public class IPeopleServiceImpl implements IPeopleService, IEngineEventCallback {
    private final List<Handler> mUiEventCallbackList = new ArrayList<Handler>();

    private IWorkerThreadControl mWorkerThreadControl;

    private RemoteService mService;

    private NetworkAgent mNetworkAgent;

    private UiAgent mHandlerAgent;

    private ApplicationCache mApplicationCache;

    /**
     * Initialises the object, creating the UiAgent.
     * 
     * @param workerThreadControl Provides access to worker thread control
     *            functions.
     * @param service Provides access to remote service functions (mainly used
     *            to retrieve context).
     */
    public IPeopleServiceImpl(final IWorkerThreadControl workerThreadControl, final RemoteService service) {
        mWorkerThreadControl = workerThreadControl;
        mService = service;
        mHandlerAgent = new UiAgent((MainApplication)service.getApplication(), service);
        mApplicationCache = ((MainApplication)service.getApplication()).getCache();
    }

    /***
     * Sets the ServiceAgent, as this needs to be called after the constructor.
     * 
     * @param agent Handle to ServiceAgent.
     */
    public final void setNetworkAgent(final NetworkAgent agent) {
        mNetworkAgent = agent;
    }

    /***
     * @see com.vodafone360.people.engine.BaseEngine.IEngineEventCallback#onUiEvent(UiEvent,
     *      int, int, Object)
     */
    @Override
	public final void onUiEvent(final ServiceUiRequest event, final int arg1, final int arg2, final Object data) {
        synchronized (mUiEventCallbackList) {
            for (Handler handler : mUiEventCallbackList) {
                Message msg = handler.obtainMessage(event.ordinal(), data);
                msg.arg1 = arg1;
                msg.arg2 = arg2;
                if (!handler.sendMessage(msg)) {
                    LogUtils.logE("IPeopleServiceImpl.onUiEvent() Sending msg FAILED");
                }
            }
        }
    }

    /***
     * @see com.vodafone360.people.engine.BaseEngine.IEngineEventCallback#kickWorkerThread()
     */
    @Override
	public final void kickWorkerThread() {
        mWorkerThreadControl.kickWorkerThread();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#addEventCallback(Handler)
     */
    @Override
	public final void addEventCallback(final Handler uiHandler) {
        synchronized (mUiEventCallbackList) {
            if (!mUiEventCallbackList.contains(uiHandler)) {
                mUiEventCallbackList.add(uiHandler);
            }
        }
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#removeEventCallback(Handler)
     */
    @Override
	public final void removeEventCallback(final Handler uiHandler) {
        synchronized (mUiEventCallbackList) {
            mUiEventCallbackList.remove(uiHandler);
        }
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#checkForUpdates()
     */
    @Override
	public final void checkForUpdates() {
        EngineManager.getInstance().getUpgradeEngine().checkForUpdates();
    }

    /**
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getMyThirdPartyIdentities()
     */
    public final ArrayList<Identity> getMyThirdPartyIdentities() {
    	return EngineManager.getInstance().getIdentityEngine().getMyThirdPartyIdentities();
    }

    /**
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getMy360AndThirdPartyIdentities()
     */
    public final ArrayList<Identity> getAvailableThirdPartyIdentities() {
    	return EngineManager.getInstance().getIdentityEngine().getAvailableThirdPartyIdentities();
    }
    
    /**
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getMy360AndThirdPartyChattableIdentities()
     */
    public final ArrayList<Identity> getMy360AndThirdPartyChattableIdentities() {
    	return EngineManager.getInstance().getIdentityEngine().getMy360AndThirdPartyChattableIdentities();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#fetchPrivacyStatement()
     */
    @Override
	public final void fetchPrivacyStatement() {
        EngineManager.getInstance().getLoginEngine().addUiFetchPrivacyStatementRequest();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#fetchTermsOfService()
     */
    @Override
	public final void fetchTermsOfService() {
        EngineManager.getInstance().getLoginEngine().addUiFetchTermsOfServiceRequest();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#fetchUsernameState(String)
     */
    @Override
	public final void fetchUsernameState(final String userName) {
        EngineManager.getInstance().getLoginEngine().addUiGetUsernameStateRequest(userName);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getLoginRequired()
     */
    @Override
	public final boolean getLoginRequired() {
        EngineManager manager = EngineManager.getInstance();
        return manager.getLoginEngine().getLoginRequired();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getRoamingNotificationType()
     */
    @Override
	public final int getRoamingNotificationType() {
        return mService.getNetworkAgent().getRoamingNotificationType();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getRoamingDeviceSetting()
     */
    @Override
	public final boolean getRoamingDeviceSetting() {
        return mService.getNetworkAgent().getRoamingDeviceSetting();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#notifyDataSettingChanged(InternetAvail)
     */
    @Override
	public final void notifyDataSettingChanged(final InternetAvail val) {
        mService.getNetworkAgent().notifyDataSettingChanged(val);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#logon(LoginDetails)
     */
    @Override
	public final void logon(final LoginDetails loginDetails) {
        EngineManager manager = EngineManager.getInstance();
        manager.getLoginEngine().addUiLoginRequest(loginDetails);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#register(RegistrationDetails)
     */
    @Override
	public final void register(final RegistrationDetails details) {
        EngineManager.getInstance().getLoginEngine().addUiRegistrationRequest(details);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#setNewUpdateFrequency()
     */
    @Override
	public final void setNewUpdateFrequency() {
        EngineManager.getInstance().getUpgradeEngine().setNewUpdateFrequency();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#setShowRoamingNotificationAgain(boolean)
     */
    @Override
	public final void setShowRoamingNotificationAgain(final boolean showAgain) {
        mService.getNetworkAgent().setShowRoamingNotificationAgain(showAgain);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#startContactSync()
     */
    @Override
	public final void startContactSync() {
        EngineManager.getInstance().getGroupsEngine().addUiGetGroupsRequest();
        EngineManager.getInstance().getContactSyncEngine().addUiStartFullSync();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#startBackgroundContactSync(long)
     */
    @Override
	public final void startBackgroundContactSync(final long delay) {
        EngineManager.getInstance().getGroupsEngine().addUiGetGroupsRequest();
        EngineManager.getInstance().getContactSyncEngine().addUiStartServerSync(delay);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#pingUserActivity()
     */
    @Override
	public final void pingUserActivity() {
        EngineManager.getInstance().getContactSyncEngine().pingUserActivity();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#validateIdentityCredentials(boolean,
     *      String, String, String, Bundle)
     */
    @Override
	public final void validateIdentityCredentials(final boolean dryRun, final String network, final String username,
            final String password, final Bundle identityCapabilityStatus) {
        EngineManager.getInstance().getIdentityEngine().addUiValidateIdentityCredentials(dryRun,
                network, username, password, identityCapabilityStatus);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#startStatusesSync()
     */
    @Override
	public final void startStatusesSync() {
        EngineManager.getInstance().getActivitiesEngine().addStatusesSyncRequest();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getNetworkAgentState()
     */
    @Override
	public final NetworkAgentState getNetworkAgentState() {
        return mNetworkAgent.getNetworkAgentState();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#setNetowrkAgentState(NetworkAgentState)
     */
    @Override
	public final void setNetworkAgentState(final NetworkAgentState state) {
        mNetworkAgent.setNetworkAgentState(state);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#getPresenceList(long)
     */
    @Override
	public final void getPresenceList(final long contactId) {
        EngineManager.getInstance().getPresenceEngine().getPresenceList();
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#setAvailability(OnlineStatus)
     */    
    @Override
	public final void setAvailability(final OnlineStatus status) {
        EngineManager.getInstance().getPresenceEngine().setMyAvailability(status);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#setAvailability(NetworkPresence)
     */    
    @Override
	public final void setAvailability(final NetworkPresence presence) {
        EngineManager.getInstance().getPresenceEngine().setMyAvailability(presence);
    }
    

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#subscribe(Handler,
     *      long, boolean)
     */
    @Override
	public final void subscribe(final Handler handler, final Long contactId, final boolean chat) {
        mHandlerAgent.subscribe(handler, contactId, chat);
    }

    /***
     * @see com.vodafone360.people.service.interfaces.IPeopleService#unsubscribe(Handler)
     */
    @Override
	public final void unsubscribe(final Handler handler) {
        mHandlerAgent.unsubscribe(handler);
    }

    /**
     * @see com.vodafone360.people.engine.BaseEngine.IEngineEventCallback#getUiAgent()
     */
    @Override
	public final UiAgent getUiAgent() {
        return mHandlerAgent;
    }

    /**
     * @see com.vodafone360.people.engine.BaseEngine.IEngineEventCallback#getApplicationCache()
     */
    @Override
	public final ApplicationCache getApplicationCache() {
        return mApplicationCache;
    }

    /**
     * @see com.vodafone360.people.service.interfaces.IPeopleService#sendMessage(long,
     *      String, int)
     */
    @Override
	public final void sendMessage(final long localContactId, final String body, final int networkId) {
        EngineManager.getInstance().getPresenceEngine()
                .sendMessage(localContactId, body, networkId);
    }

    @Override
	public final void setIdentityStatus(final String network, final String identityId, final boolean identityStatus) {
        EngineManager.getInstance().getIdentityEngine().addUiSetIdentityStatus(network, identityId,
                identityStatus);
    }

    @Override
	public final void getStatuses() {
        EngineManager.getInstance().getActivitiesEngine().addStatusesSyncRequest();
    }

    @Override
	public final void getMoreTimelines() {
        EngineManager.getInstance().getActivitiesEngine().addOlderTimelinesRequest();
    }

    @Override
	public final void getOlderStatuses() {
        EngineManager.getInstance().getActivitiesEngine().addGetOlderStatusesRequest();
    }

    @Override
	public final void uploadMeProfile() {
        EngineManager.getInstance().getSyncMeEngine().addUpdateMeProfileContactRequest();
    }

    @Override
	public final void uploadMyStatus(final String statusText) {
        EngineManager.getInstance().getSyncMeEngine().addUpdateMyStatusRequest(statusText);
    }

    @Override
	public final void downloadMeProfileFirstTime() {
        EngineManager.getInstance().getSyncMeEngine().addGetMeProfileContactFirstTimeRequest();
    }

    @Override
	public final void updateChatNotification(final long localContactId) {
        mHandlerAgent.updateChat(localContactId, false);
        
    }
    
    /**
     *com.vodafone360.people.service.interfaces.
     *IPeopleService#StartContentUpload.
     *uploads files.
     */

      public final void startPhotoUpload(final List<PhotoUtilsIn> listfilecontent)
      {
         EngineManager.getInstance().getPhotoUploadEngine().loadPhoto(listfilecontent);
      }

       /**
       *com.vodafone360.people.service.interfaces.
       *IPeopleService#cancelContentEngineUploadRequest files.
       */
      public final void cancelPhotoUploadEngineRequest() {
          EngineManager.getInstance().getPhotoUploadEngine().cancelRequests();
      }
      /**
       * com.vodafone360.people.service.interfaces.
       * IPeopleService#Shares content with connected group(default 360).
       * @param contentid for contents.
       */

      public final void shareContentWith360Album(final Long contentid) {
        EngineManager.getInstance().getPhotoUploadEngine().shareContentWith360Album(contentid);
      }
      /**
       * Add Albums to the server.
       * @param list of albums to be added.
       */

     public final void addAlbums(final List<AlbumUtilsIn> list) {

          EngineManager.getInstance().getPhotoUploadEngine().addAlbum(list);
      }
     /**
      * Share the contents.
      * @param albumid album with which sharing needs to be done.
      * @param contentid the contents to be shared.
      */
      public final void shareContents(final Long albumid,
    		               final Long contentid) {
         EngineManager.getInstance().getPhotoUploadEngine().
                    sharePhotoWithAlbum(albumid, contentid);
      }
      /**
       * Share the album.
       * @param groupid Groupid group with which album to be shared.
       * @param albumid Album top be shared.
       */
      public final void shareAlbum(final Long groupid,
    		                       final Long albumid) {
         EngineManager.getInstance().
             getPhotoUploadEngine().shareAlbum(groupid, albumid);
      }

}