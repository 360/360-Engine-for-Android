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

import com.vodafone360.people.service.aidl.IDatabaseSubscriber;
import android.os.Bundle;
import com.vodafone360.people.datatypes.Identity;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.engine.presence.User;
import com.vodafone360.people.engine.presence.NetworkPresence;
import com.vodafone360.people.datatypes.RegistrationDetails;


interface IDatabaseSubscriptionService {
 
	/**
	 * This as close as possible to a straight copy of the IPeopleService
	 * interface, with an extra help function or two thrown in.  This list
	 * needs to be updated whenever changes are made to the IPeopleService.
	 */
	void checkForUpdates();
	void deleteIdentity(in String network, in String identityId);
	void downloadMeProfileFirstTime();
	void fetchPrivacyStatement();
	void fetchTermsOfService();
	void fetchUsernameState(in String username);
	List<Identity> getAvailableThirdPartyIdentities();
	List<Identity> getMy360AndThirdPartyChattableIdentities();
	List<Identity> getMyThirdPartyIdentities();
	boolean getLoginRequired();
	void getMoreTimelines();
	void getOlderStatuses();
	int getPresence(long localContactID);
	void getPresenceList(long contactId);
	boolean getRoamingDeviceSetting();
	int getRoamingNotificationType();
	void getStatuses();
    User getUserPresenceStatusByLocalContactId(long localContactId);
    void logon(in LoginDetails loginDetails);
    /** Using INTs instead of ENUMs. **/
    void notifyDataSettingChanged(int internetAvail);
    void pingUserActivity();
	void register(in RegistrationDetails details);
	void sendMessage(long toLocalContactId, in String body, int socialNetworkId);
	void setAvailability(int status);
	void setIdentityStatus(in String network, in String identityId, boolean identityStatus);
	void setNewUpdateFrequency();
	void setShowRoamingNotificationAgain(boolean showAgain);
	void startBackgroundContactSync(long delay);
	void startContactSync();
	void startStatusesSync();
	void updateChatNotification(long localContactId);
	void uploadMeProfile();
	void uploadMyStatus(in String statusText);
	void validateIdentityCredentials(boolean dryRun, String network, String username,
                String password,in Bundle identityCapabilityStatus);

	/**
	 * Use these methods to subscribe or unsubscribe to get callbacks from the
	 * service when stuff happens.
	 */
	boolean subscribe(String identifier, IDatabaseSubscriber subscriber);
	boolean unsubscribe(String identifier);
	
}