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

package com.vodafone360.people.service;

/**
 * The list of requests that the People Client UI can issue to the People Client
 * service. These requests are handled by the appropriate engine.
 */
public enum ServiceUiRequest {

    /*
     * UiAgent: Each request is an unsolicited message sent to the UI via the
     * UiAgent. Requests with a higher priority can overwrite lower priority
     * requests waiting in the UiAgent queue.
     */

    /** HIGH PRIRITY: Go to landing page, explain why in the Bundle. **/
    UNSOLICITED_GO_TO_LANDING_PAGE,
    UI_REQUEST_COMPLETE,
    DATABASE_CHANGED_EVENT,
    SETTING_CHANGED_EVENT,
    /** Update in the terms and conditions or privacy text. **/
    TERMS_CHANGED_EVENT,
    /***
     * Update the contact sync progress bar, currently used only in the
     * SyncingYourAddressBookActivity.
     */
    UPDATE_SYNC_STATE,
    UNSOLICITED_CHAT,
    UNSOLICITED_PRESENCE,
    UNSOLICITED_CHAT_ERROR,
    UNSOLICITED_CHAT_ERROR_REFRESH,
    UNSOLICITED_PRESENCE_ERROR,
    /** LOW PRIORITY Show the upgrade dialog. **/
    UNSOLICITED_DIALOG_UPGRADE,

    /*
     * Non-UiAgent: Each request is handled by a specific Engine.
     */

    /**
     * Login to existing account, handled by LoginEngine.
     * 
     * @see com.vodafone360.people.engine.login.LoginEngine
     */
    LOGIN,
    /**
     * Sign-up a new account, handled by LoginEngine.
     * 
     * @see com.vodafone360.people.engine.login.LoginEngine
     */
    REGISTRATION,
    /**
     * Fetch user-name availability state, handled by LoginEngine.
     * 
     * @see com.vodafone360.people.engine.login.LoginEngine
     */
    USERNAME_AVAILABILITY,
    /**
     * Handled by LoginEngine.
     * 
     * @see com.vodafone360.people.engine.login.LoginEngine
     */
    FETCH_TERMS_OF_SERVICE,
    /**
     * Handled by LoginEngine.
     * 
     * @see com.vodafone360.people.engine.login.LoginEngine
     */
    FETCH_PRIVACY_STATEMENT,
    /**
     * Fetch list of available 3rd party accounts, handled by IdentitiesEngine.
     * 
     * @see com.vodafone360.people.engine.identitys.IdentityEngine
     */
    GET_AVAILABLE_IDENTITIES,
    /**
     * Validate credentials for specified 3rd party account, handled by
     * IdentitiesEngine.
     * 
     * @see com.vodafone360.people.engine.identitys.IdentityEngine
     */
    VALIDATE_IDENTITY_CREDENTIALS,
    /**
     * Set required capabilities for specified 3rd party account, handled by
     * IdentitiesEngine.
     * 
     * @see com.vodafone360.people.engine.identitys.IdentityEngine
     */
    SET_IDENTITY_CAPABILITY_STATUS,
    /**
     * Get list of 3rd party accounts for current user, handled by
     * IdentitiesEngine.
     * 
     * @see com.vodafone360.people.engine.identitys.IdentityEngine
     */
    GET_MY_IDENTITIES,
    /**
     * Get list of 3rd party accounts for current user that support chat,
     * handled by IdentitiesEngine.
     * 
     * @see com.vodafone360.people.engine.identitys.IdentityEngine
     */
    GET_MY_CHATABLE_IDENTITIES,
    /**
     * Fetch older activities from Server, handled by ActivitiesEngine.
     * 
     * @see com.vodafone360.people.engine.activities.ActivitiesEngine
     */
    FETCH_STATUSES,
    /**
     * Fetch latest statuses from Server ("refresh" button, or push event),
     * handled by ActivitiesEngine.
     * 
     * @see com.vodafone360.people.engine.identitys.AvtivitiesEngine
     */
    UPDATE_STATUSES,
    /**
     * Fetch timelines from the native (invoked by "show older" button)
     * 
     * @see com.vodafone360.people.engine.identitys.AvtivitiesEngine
     */
    FETCH_TIMELINES,
    /**
     * Update the timeline list (invoked by NAB event)
     * 
     * @see com.vodafone360.people.engine.identitys.AvtivitiesEngine
     */
    UPDATE_PHONE_CALLS,
    /**
     * Update the phone calls in the list of timelines (invoked by NAB event)
     * 
     * @see com.vodafone360.people.engine.identitys.AvtivitiesEngine
     */
    UPDATE_SMS,
    
    /**
     * Update the SMSs in the list of timelines (invoked by push event)
     * 
     * @see com.vodafone360.people.engine.identitys.AvtivitiesEngine
     */
    UPDATE_TIMELINES,
    /**
     * Start contacts sync, handled by ContactSyncEngine.
     * 
     * @see com.vodafone360.people.engine.contactsync.ContactSyncEngine
     */
    NOWPLUSSYNC,
    /**
     * Starts me profile download, handled by SyncMeEngine.
     * 
     * @see com.vodafone360.people.engine.meprofile.SyncMeEngine
     */
    GET_ME_PROFILE,
    /**
     * Starts me profile upload, handled by SyncMeEngine.
     * 
     * @see com.vodafone360.people.engine.meprofile.SyncMeEngine
     */
    UPDATE_ME_PROFILE,
    /**
     * Starts me profile status upload. 
     * 
     * @see com.vodafone360.people.engine.meprofile.SyncMeEngine
     */
    UPLOAD_ME_STATUS,
    /**
     * Starts me profile thumbnail download, handled by SyncMeEngine.
     * 
     * @see com.vodafone360.people.engine.meprofile.SyncMeEngine
     */
    DOWNLOAD_THUMBNAIL,
    /** Remove all user data. */
    REMOVE_USER_DATA,
    /**
     * Logout from account, handled by LoginEngine. For debug/development use
     * only.
     * 
     * @see com.vodafone360.people.engine.login.LoginEngine
     */
    LOGOUT,
    /**
     * Request UpgradeEngine to check if an updated version of application is
     * available.
     * 
     * @see com.vodafone360.people.engine.upgrade.UpgradeEngine
     */
    UPGRADE_CHECK_NOW,
    /**
     * Set frequency for upgrade check in UpgradeEngine.
     * 
     * @see com.vodafone360.people.engine.upgrade.UpgradeEngine
     */
    UPGRADE_CHANGE_FREQUENCY,
    /**
     * Request list of current 'presence status'of contacts, handled by
     * PresenceEngine.
     */
    GET_PRESENCE_LIST,
    /**
     * Request to set the presence availability status. 
     */
    SET_MY_AVAILABILITY,
    /** Start a chat conversation. */
    CREATE_CONVERSATION,
    /** Send chat message. */
    SEND_CHAT_MESSAGE,
    /**
     * UI might need to display a progress bar, or other background process
     * indication
     **/
    UPDATING_UI,
    /**
     * UI might need to remove a progress bar, or other background process
     * indication
     **/
    UPDATING_UI_FINISHED,
    /**
     * Gets the groups for the contacts that are retrieved from the backend.
     */
    GET_GROUPS,

/**
*Gets the default 360 album.
*/
    GET_DEFAULT_ALBUM_360,
/**
*Shares the contents with albumid.
*/
    SHARE_PHOTO_WITH_ALBUM, //SHARE_CONTENT,
/**
 * For uploading the photo.
 */
    UPLOAD_PHOTO, //START_UPLOADFILE,
/**
 * For adding the albums.
 */
    ADD_ALBUMS,
    /**
     * For sharing the album.
     */

    SHARE_ALBUM,
     /* Do not handle this message.
     */
    UNKNOWN;

    /***
     * Get the UiEvent from a given Integer value.
     * 
     * @param input Integer.ordinal value of the UiEvent
     * @return Relevant UiEvent or UNKNOWN if the Integer is not known.
     */
    public static ServiceUiRequest getUiEvent(int input) {
        if (input < 0 || input > UNKNOWN.ordinal()) {
            return UNKNOWN;
        } else {
            return ServiceUiRequest.values()[input];
        }
    }
}
