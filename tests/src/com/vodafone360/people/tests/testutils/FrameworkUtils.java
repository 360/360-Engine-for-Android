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
package com.vodafone360.people.tests.testutils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;

import android.os.Bundle;
import android.os.Handler;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.datatypes.ContactSummary.OnlineStatus;
import com.vodafone360.people.engine.IEngineEventCallback;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.PersistSettings.InternetAvail;
import com.vodafone360.people.service.agent.NetworkAgentState;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.interfaces.IPeopleService;

/***
 * Testing framework utilities.
 */
public final class FrameworkUtils {

    /***
     * Private constructor to prevent instantiation.
     */
    private FrameworkUtils() {
        // Do nothing.
    }

    /***
     * Dummy IEngineEventCallback.
     *
     * @return Dummy IEngineEventCallback object.
     */
    public static IEngineEventCallback createDummyIEngineEventCallback() {
        return new IEngineEventCallback() {
            @Override
            public ApplicationCache getApplicationCache() {
                return null;
            }

            @Override
            public UiAgent getUiAgent() {
                return null;
            }

            @Override
            public void kickWorkerThread() {
                // Do nothing.
            }

            @Override
            public void onUiEvent(final ServiceUiRequest event,
                    final int request, final int status, final Object data) {
                // Do nothing.
            }
        };
    }

    /***
     * Dummy IEngineEventCallback.
     *
     * @param loginRequired Defines how getLoginRequired() will return.
     * @return Dummy IEngineEventCallback object.
     */
    public static IPeopleService createDummyIPeopleService(
            final boolean loginRequired) {
        return new IPeopleService() {
            /** Store the state of any subscribe unsubscribe calls. **/
            private Handler mHandler = null;
            /** Store the state of any fetchPrivacyStatement() calls. **/
            private boolean mFetchPrivacyStatement = false;
            /** Store the state of any fetchTermsOfService() calls. **/
            private boolean mFetchTermsOfService = false;
            @Override
            public void addEventCallback(final Handler uiHandler) {
            }
            @Override
            public void checkForUpdates() {
            }
            @Override
            public void downloadMeProfileFirstTime() {
            }
            @Override
            public void fetchPrivacyStatement() {
                mFetchPrivacyStatement = true;
            }
            @Override
            public void fetchTermsOfService() {
                mFetchTermsOfService = true;
            }
            @Override
            public void fetchUsernameState(final String username) {
            }
            @Override
            public boolean getLoginRequired() {
                return loginRequired;
            }
            @Override
            public void getMoreTimelines() {
            }
            @Override
            public NetworkAgentState getNetworkAgentState() {
                return null;
            }
            @Override
            public void getOlderStatuses() {
            }
            @Override
            public void getPresenceList(final long contactId) {
            }
            @Override
            public boolean getRoamingDeviceSetting() {
                return false;
            }
            @Override
            public int getRoamingNotificationType() {
                return 0;
            }
            @Override
            public void getStatuses() {
            }
            @Override
            public void logon(final LoginDetails loginDetails) {
            }
            @Override
            public void notifyDataSettingChanged(
                    final InternetAvail internetAvail) {
            }
            @Override
            public void pingUserActivity() {
            }
            @Override
            public void register(final RegistrationDetails details) {
            }
            @Override
            public void removeEventCallback(final Handler uiHandler) {
            }
            @Override
            public void sendMessage(final long toLocalContactId,
                    final String body, final int socialNetworkId) {
            }
            @Override
            public void setIdentityStatus(final String network,
                    final String identityId, final boolean identityStatus) {
            }
            @Override
            public void setNetworkAgentState(final NetworkAgentState state) {
            }
            @Override
            public void setNewUpdateFrequency() {
            }
            @Override
            public void setShowRoamingNotificationAgain(
                    final boolean showAgain) {
            }
            @Override
            public void startContactSync() {
            }
            @Override
            public void startStatusesSync() {
            }
            @Override
            public void subscribe(final Handler handler, final Long contactId, final boolean chat) {
                mHandler = handler;
            }
            @Override
            public void unsubscribe(final Handler handler) {
                mHandler = null;
            }
            @Override
            public void updateChatNotification(final long localContactId) {
            }
            @Override
            public void uploadMeProfile() {
            }
            @Override
            public void uploadMyStatus(final String statusText) {
            }
            @Override
            public void validateIdentityCredentials(final boolean dryRun,
                    final String network, final String username,
                    final String password,
                    final Bundle identityCapabilityStatus) {
            }
            @Override
            public ArrayList<Identity> getAvailableThirdPartyIdentities() {
                return null;
            }
            @Override
            public ArrayList<Identity> getMyThirdPartyIdentities() {
                return null;
            }
            @Override
            public ArrayList<Identity> getMy360AndThirdPartyChattableIdentities() {
                return null;
            }
			@Override
			public void deleteIdentity(String network, String identityId) {
			}
            @Override
            public void setAvailability(OnlineStatus status) {
            }
			@Override
			public void setAvailability(Hashtable<String, String> status) {
			}
			@Override
			public void setAvailability(SocialNetwork network, OnlineStatus status) {
			}
        };
    }

    /***
     * Set a specific field in the class via reflection.
     *
     * @param remoteService Instance of the class.
     * @param fieldName Name of the field to set via reflection.
     * @param value Value to set the field via reflection.
     * @throws Exception Any kind of mapping exception.
     */
    public static void set(final Object remoteService,
            final String fieldName, final Object value) throws Exception {
        Field field = remoteService.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(remoteService, value);
    }

    /***
     * Get the value of a specific field in the class via reflection.
     *
     * @param remoteService Instance of the class.
     * @param fieldName Name of the field to set via reflection.
     * @throws Exception Any kind of mapping exception.
     * @return Value of object via reflection.
     */
    public static Object get(final Object remoteService,
            final String fieldName) throws Exception {
        Field field = remoteService.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(remoteService);
    }
}