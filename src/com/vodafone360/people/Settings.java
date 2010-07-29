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

package com.vodafone360.people;

/**
 * All application settings.
 */
public final class Settings {

    /*
     * LogCat.
     */
    /** LogCat TAG prefix. **/
    public static final String LOG_TAG = "People_";


    /*
     * Transport.
     */
    /** Milliseconds until HTTP connection will time out. */
    public static final int HTTP_CONNECTION_TIMEOUT = 30000;

    /** Maximum number of HTTP connection attempts. */
    public static final int HTTP_MAX_RETRY_COUNT = 3;

    /** HTTP header content type. */
    public static final String HTTP_HEADER_CONTENT_TYPE = "application/binary";

    /** Number of empty RPG poll responses before stopping RPG poll. */
    public static final int BLANK_RPG_HEADER_COUNT = 4;

    /** TCP Heartbeat interval (10 seconds). */
    public static final long TCP_VF_HEARTBEAT_INTERVAL = 10 * 60 * 1000;

    /** TCP retry interval if we have lost the connection (30 seconds). */
    public static final long TCP_RETRY_BROKEN_CONNECTION_INTERVAL
        = 30 * 60 * 1000;

    /** TCP socket read time out for the read-operation. */
    public static final int TCP_SOCKET_READ_TIMEOUT = 10 * 60 * 1000;


    /*
     * Notifications.
     */
    /** LED colour. */
    public static final int NOTIFICATION_LED_COLOR = 0xff00ff00; // Green

    /** LED on time. */
    public static final int NOTIFICATION_LED_OFF_TIME = 300;

    /** LED on time. */
    public static final int NOTIFICATION_LED_ON_TIME = 1000;

    /*
     * Upgrade Engine
     */
    /**
     * See UpdateSettingsActivity.FREQUENCY_SETTING_LONG array for meaning (i.e.
     * Every 6 hours)
     */
    public static final int PREFS_CHECK_FREQUENCY_DEFAULT = 3;

    /** Show the upgrade dialog box every 10 minutes. */
    public static final int DIALOG_CHECK_FREQUENCY_MILLIS = 10 * 60 * 1000;

    /**
     * Component trace flags (always checked in as false, not part of build
     * script).
     */
    /** Trace output for engine components. **/
    public static final boolean ENABLED_ENGINE_TRACE = false;

    /** Trace output for database components. **/
    public static final boolean ENABLED_DATABASE_TRACE = false;

    /** Trace output for transport (i.e. network IO) components. **/

    public static final boolean ENABLED_TRANSPORT_TRACE = false;

    /** Trace output for contact synchronisation components. **/
    public static final boolean ENABLED_CONTACTS_SYNC_TRACE = false;

    /** Log engine runtime information to file for profiling. **/
    public static final boolean ENABLED_PROFILE_ENGINES = false;    
    
    /**
     * This is a list of strings containing the names of engines to be
     * deactivated in the build. A de-activated engine is constructed but will
     * never be run (nor will onCreate or onDestroy be called). Any UI requests
     * will be automatically completed by the framework with a
     * ServiceStatus.ERROR_NOT_IMPLEMENTED error.
     */
    public static final String DEACTIVATE_ENGINE_LIST_KEY
        = "deactivated-engines-list";


    /*
     * Hard coded settings
     */
    /** Enable dialler cheat code. **/
    public static final boolean DIALER_CHEATCODES_ENABLED = true;

    /** Enable SMS account activation. **/
    public static final boolean ENABLE_ACTIVATION = false;

    /**
     * Enable SIM inserted check. Always set to TRUE.  Makes the application
     * unusable if there is no valid SIM card inserted into the device.
     */
    public static final boolean ENABLE_SIM_CHECK = true;

    /** Enable fetching native contacts. **/
    public static final boolean ENABLE_FETCH_NATIVE_CONTACTS = true;

    /** Enable fetching native contacts on change. **/
    public static final boolean ENABLE_FETCH_NATIVE_CONTACTS_ON_CHANGE = true;

    /** Enable ME profile synchronisation. **/
    public static final boolean ENABLE_ME_PROFILE_SYNC = true;

    /** Enable server contact synchronisation. **/
    public static final boolean ENABLE_SERVER_CONTACT_SYNC = true;

    /** Enable server thumbnail synchronisation. **/
    public static final boolean ENABLE_THUMBNAIL_SYNC = true;

    /** Enable update native contacts synchronisation. **/
    public static final boolean ENABLE_UPDATE_NATIVE_CONTACTS = true;

    /** Enable hiding of connected friends group. **/
    public static final boolean HIDE_CONNECTED_FRIENDS_GROUP = true;


    /*
     * Keys for properties that can be changed at build time.
     */
    /** Key for application ID setting. **/
    public static final String APP_KEY_ID = "app-key-id";

    /** Key for application secret key setting. **/
    public static final String APP_SECRET_KEY = "app-secret-key";

    /** Key for enable logging setting. **/
    protected static final String ENABLE_LOGCAT_KEY = "enable-logcat";

    /** Key for enable RPG setting. **/
    public static final String ENABLE_RPG_KEY = "use-rpg";

    /** Key for enable SNS resource icon setting. **/
    public static final String ENABLE_SNS_RESOURCE_ICON_KEY
        = "enable-sns-resource-icon";

    /** Key for RPG server URL setting. **/
    public static final String RPG_SERVER_KEY = "rpg-url";

    /** Key for Hessian URL setting. **/
    public static final String SERVER_URL_HESSIAN_KEY = "hessian-url";


    /*
     * Keys without defaults.
     */
    /** Key for TCP server URL setting. **/
    public static final String TCP_RPG_URL_KEY = "rpg-tcp-url";

    /** Key for TCP port setting. **/
    public static final String TCP_RPG_PORT_KEY = "rpg-tcp-port";

    /** Key for upgrade check URL setting. **/
    public static final String UPGRADE_CHECK_URL_KEY = "upgrade-url";


    /*
     * Default for properties that can be changed at build time.
     */
    /** Default for logging enabled setting. **/
    protected static final String DEFAULT_ENABLE_LOGCAT = "true";

    /** Default for RPG enabled setting. **/
    protected static final String DEFAULT_ENABLE_RPG = "true";

    /** Default for SNS resource icon setting. **/
    protected static final String DEFAULT_ENABLE_SNS_RESOURCE_ICON = "true";

    /** Default for RPG server URL setting. **/
    protected static final String DEFAULT_RPG_SERVER
        = "http://rpg.vodafone360.com/rpg/mcomet/";

    /** Default for Hessian URL setting. **/
    protected static final String DEFAULT_SERVER_URL_HESSIAN
        = "http://api.vodafone360.com/services/hessian/";


    /*
     * Request timeouts for all engines and requests, except for fire and
     * forget calls to RPG: SET_AVAILABILITY and SEND_CHAT_MSG.
     */
    /** Do not handle timeouts for this API. **/
    private static final long DONT_HANDLE_TIMEOUTS = -1;

    /** Generic timeout for requests. **/
    private static final long ALL_ENGINES_REQUESTS_TIMEOUT = 60000;

    /** Timeout for all presence API requests. **/
    private static final long PRESENCE_ENGINE_REQUESTS_TIMEOUT = 120000;

    /** Number of milliseconds in a week. **/
    public static final long HISTORY_IS_WEEK_LONG = 7 * 24 * 60 * 60 * 1000;

    /** Timeout for request waiting in queue. **/
    public static final long REMOVE_REQUEST_FROM_QUEUE_MILLIS = 15 * 60 * 1000;


    /*
     * The timeouts in milliseconds for the different APIs.
     */
    /** Timeout for activities API. **/
    public static final long API_REQUESTS_TIMEOUT_ACTIVITIES
        = ALL_ENGINES_REQUESTS_TIMEOUT;

    /** Timeout for authentication API. **/
    public static final long API_REQUESTS_TIMEOUT_AUTH
        = ALL_ENGINES_REQUESTS_TIMEOUT;

    /** Timeout for chat create conversation API. **/
    public static final long API_REQUESTS_TIMEOUT_CHAT_CREATE_CONV
        = ALL_ENGINES_REQUESTS_TIMEOUT;

    /** Timeout for chat send message API. **/
    public static final long API_REQUESTS_TIMEOUT_CHAT_SEND_MESSAGE
        = DONT_HANDLE_TIMEOUTS;

    /** Timeout for contacts API. **/
    public static final long API_REQUESTS_TIMEOUT_CONTACTS
        = ALL_ENGINES_REQUESTS_TIMEOUT;

    /** Timeout for group privacy API. **/
    public static final long API_REQUESTS_TIMEOUT_GROUP_PRIVACY
        = ALL_ENGINES_REQUESTS_TIMEOUT;

    /** Timeout for identities API. **/
    public static final long API_REQUESTS_TIMEOUT_IDENTITIES
        = ALL_ENGINES_REQUESTS_TIMEOUT;

    /** Timeout for presence list API. **/
    public static final long API_REQUESTS_TIMEOUT_PRESENCE_LIST
        = PRESENCE_ENGINE_REQUESTS_TIMEOUT;

    /** Timeout for presence set availability API. **/
    public static final long API_REQUESTS_TIMEOUT_PRESENCE_SET_AVAILABILITY
        = PRESENCE_ENGINE_REQUESTS_TIMEOUT;

    /** Enable Facebook chat. **/
    public static boolean sEnableFacebookChat = true;
    
    /**
     * Danger! Only set to true if you know what you are doing! This logs each
     * response no matter if gzipped or not to the SD card under the given
     * request ID.
     */
    public static boolean sEnableSuperExpensiveResponseFileLogging
        = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private Settings() {
    }
}