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

package com.vodafone360.people.service.io.api;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.utils.AuthUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Implementation of Authentication APIs. These include APIs to login to an
 * existing account, sign up to a new account, retrieve terms and conditions and
 * privacy statement etc. To get access to the other API's and perform actions
 * on behalf of a user, the application has first to authenticate the user,
 * using some of these methods, so it can retrieve a User Session for
 * authorising the user.
 */
public class Auth {
    private final static String FUNCTION_GET_SESSION = "auth/getsessionbycredentials";

    private final static String FUNCTION_ACTIVATE = "auth/activate";

    private final static String FUNCTION_GET_USERNAME_STATE = "auth/getusernamestate";

    private final static String FUNCTION_REQUEST_ACTIVATION_CODE = "auth/requestactivationcode";

    private final static String FUNCTION_SIGNUP_USER_CRYPTED = "auth/signupusercrypted";

    private final static String FUNCTION_GET_PUBLIC_KEY = "auth/getpublickey";

    private final static String FUNCTION_GET_TERMS_AND_CONDITIONS = "auth/gettermsandconditions";

    private final static String FUNCTION_GET_PRIVACY_STATEMENT = "auth/getprivacystatement";

    private final static String ACCEPTED_T_AND_C = "acceptedtandc";

    private final static String BIRTHDATE = "birthdate";

    private final static String COUNTY_CODE = "countrycode";

    private final static String EMAIL = "email";

    private final static String FLAGS = "flags";

    private final static String FULLNAME = "fullname";

    private final static String LANGUAGE = "language";

    private final static String LANGUAGE_CULTURE = "languageculture";

    private final static String MOBILE_MODE_ID = "mobilemodelid";

    private final static String MOBILE_OPERATOR_ID = "mobileoperatorid";

    private final static String MSISDN = "msisdn";

    private final static String PASSWORD = "password";

    private final static String SEND_CONFIRMATION_MAIL = "sendconfirmationmail";

    private final static String SEND_CONFIRMAITON_SMS = "sendconfirmationsms";

    private final static String SUBSCRIBE_TO_NEWSLETTER = "subscribetonewsletter";

    private final static String TIMESTAMP = "timestamp";

    private final static String TIMEZONE = "timezone";

    private final static String USERNAME = "username";

    private final static String VALUE = "value";

    private final static int ACTIVATE_MOBILE_CLIENT_FLAG = 8;

    /**
     * Implementation of "getsessionbycredentials" API. Used to login to an
     * existing VF360 account.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @param username User-name for account.
     * @param password Password for account.
     * @param more Additional login details or NULL.
     * @return Request ID generated for this request.
     * @throws NullPointerException when engine is NULL
     * @throws NullPointerException when username is NULL
     * @throws NullPointerException when password is NULL
     */
    public static int getSessionByCredentials(BaseEngine engine, String username, String password,
            Map<String, List<String>> more) {
        if (engine == null) {
            throw new NullPointerException("Auth.getSessionByCredentials() engine cannot be NULL");
        }
        if (username == null) {
            throw new NullPointerException("Auth.getSessionByCredentials() username cannot be NULL");
        }
        if (password == null) {
            throw new NullPointerException("Auth.getSessionByCredentials() password cannot be NULL");
        }

        String ts = "" + (System.currentTimeMillis() / 1000);

        Request request = new Request(FUNCTION_GET_SESSION, Request.Type.SIGN_IN,
                engine.engineId(), false, Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData("hash", AuthUtils.getMd5Hash(SettingsManager.getProperty(
                Settings.APP_SECRET_KEY).toLowerCase()
                + "&" + ts + "&" + username.toLowerCase() + "&" + password.toLowerCase()));
        request.addData(TIMESTAMP, ts);
        request.addData(USERNAME, username.toLowerCase());

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of "auth/activate" API.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @param code Activation code for this account
     * @return Request ID generated for this request.
     * @return -1 when code is NULL.
     * @return -1 when LoginEngine.getSession() is NULL.
     * @throws NullPointerException when engine is NULL
     */
    public static int activate(BaseEngine engine, String code) {
        if (engine == null) {
            throw new NullPointerException("Auth.activate() engine cannot be NULL");
        }
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Auth.activate() Invalid session, return -1");
            return -1;
        }

        if (code == null) {
            LogUtils.logE("Auth.activate() Code must not be NULL, return -1");
            return -1;
        }

        Request request = new Request(FUNCTION_ACTIVATE, Request.Type.STATUS, engine.engineId(),
                false, Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData("code", code);
        request.addData(FLAGS, ACTIVATE_MOBILE_CLIENT_FLAG);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of "auth/getusernamestate" API.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @param username User name for this account.
     * @return Request id generated for this request.
     * @return -1 when username is NULL.
     * @throws NullPointerException when engine is NULL
     */
    public static int getUsernameState(BaseEngine engine, String username) {
        if (engine == null) {
            throw new NullPointerException("Auth.getUsernameState() engine cannot be NULL");
        }
        if (username == null) {
            LogUtils.logE("Auth.GetUsernameState() username must not be NULL, return -1");
            return -1;
        }

        Request request = new Request(FUNCTION_GET_USERNAME_STATE, Request.Type.TEXT_RESPONSE_ONLY,
                engine.engineId(), false, Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData(USERNAME, username);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of "auth/requestactivationcode" API.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @param username User-name for this account.
     * @param mobileNumber Mobile number for this activation code to be sent.
     * @return Request ID generated for this request.
     * @return -1 when username is NULL.
     * @throws NullPointerException when engine is NULL
     * @throws NullPointerException when mobileNumber is NULL
     */
    public static int requestActivationCode(BaseEngine engine, String username, String mobileNumber) {
        if (engine == null) {
            throw new NullPointerException("Auth.requestActivationCode() engine cannot be NULL");
        }
        if (username == null) {
            LogUtils.logE("Auth.requestActivationCode() username must be specified");
            return -1;
        }
        if (mobileNumber == null) {
            throw new NullPointerException(
                    "Auth.requestActivationCode() mobileNumber cannot be NULL");
        }

        Request request = new Request(FUNCTION_REQUEST_ACTIVATION_CODE, Request.Type.STATUS, engine
                .engineId(), false, Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData(USERNAME, username);
        request.addData(VALUE, mobileNumber);
        request.addData(FLAGS, ACTIVATE_MOBILE_CLIENT_FLAG);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of "auth/signupusercrypted" API. Encrypted signup to new
     * account.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @param fullname Full name of user
     * @param username Username chosen for new account
     * @param password Password chosen for new account
     * @param timestamp Current timestamp.
     * @param email Email address associated with this account.
     * @param birthDate Birthday of user signing up (there is a minimum age
     *            limit for users).
     * @param msisdn MSIDN for user.
     * @param acceptedTAndC Whether user has accepted terms and conditions.
     * @param countryCode String identifier identifying country.
     * @param timezone Timezone identifier.
     * @param language Lauguage identifier.
     * @param mobileOperatorId ID of mobile operator.
     * @param mobileModelId Mobile device ID.
     * @param sendConfirmationMail True if confirmation email to be sent to
     *            user.
     * @param sendConfirmationSms True if confirmation SMS to be sent to user.
     * @param subscribeToNewsletter True if user wishes to subscribe to
     *            newsletter.
     * @return Request ID generated for this request.
     * @throws NullPointerException when engine is NULL
     * @throws NullPointerException when acceptedTAndC is NULL
     * @throws NullPointerException when mobileOperatorId is NULL
     * @throws NullPointerException when mobileModelId is NULL
     * @throws NullPointerException when sendConfirmationMail is NULL
     * @throws NullPointerException when sendConfirmationSms is NULL
     * @throws NullPointerException when subscribeToNewsletter is NULL
     */
    public static int signupUserCrypted(BaseEngine engine, String fullname, String username,
            byte[] password, long timestamp, String email, String birthDate, String msisdn,
            Boolean acceptedTAndC, String countryCode, String timezone, String language,
            Long mobileOperatorId, Long mobileModelId, Boolean sendConfirmationMail,
            Boolean sendConfirmationSms, Boolean subscribeToNewsletter) {
        if (engine == null) {
            throw new NullPointerException("Auth.signupUserCrypted() engine cannot be NULL");
        }
        if (acceptedTAndC == null) {
            throw new NullPointerException("Auth.signupUserCrypted() acceptedTAndC cannot be NULL");
        }
        if (mobileOperatorId == null) {
            throw new NullPointerException(
                    "Auth.signupUserCrypted() mobileOperatorId cannot be NULL");
        }
        if (mobileModelId == null) {
            throw new NullPointerException("Auth.signupUserCrypted() mobileModelId cannot be NULL");
        }
        if (sendConfirmationMail == null) {
            throw new NullPointerException(
                    "Auth.signupUserCrypted() sendConfirmationMail cannot be NULL");
        }
        if (sendConfirmationSms == null) {
            throw new NullPointerException(
                    "Auth.signupUserCrypted() sendConfirmationSms cannot be NULL");
        }
        if (subscribeToNewsletter == null) {
            throw new NullPointerException(
                    "Auth.signupUserCrypted() subscribeToNewsletter cannot be NULL");
        }

        String ts = "" + (System.currentTimeMillis() / 1000);

        Request request = new Request(FUNCTION_SIGNUP_USER_CRYPTED, Request.Type.SIGN_UP, engine
                .engineId(), false, Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData(FULLNAME, fullname);
        request.addData(USERNAME, username);
        request.addData(PASSWORD, password);
        request.addData(EMAIL, email);
        request.addData(BIRTHDATE, birthDate);
        request.addData(MSISDN, msisdn);
        request.addData(ACCEPTED_T_AND_C, acceptedTAndC.toString());
        request.addData(COUNTY_CODE, countryCode);
        request.addData(TIMESTAMP, ts);
        request.addData(TIMEZONE, timezone);
        request.addData(LANGUAGE, language);
        request.addData(MOBILE_OPERATOR_ID, mobileOperatorId.toString());
        request.addData(MOBILE_MODE_ID, mobileModelId.toString());
        request.addData(SEND_CONFIRMATION_MAIL, sendConfirmationMail.toString());
        request.addData(SEND_CONFIRMAITON_SMS, sendConfirmationSms.toString());
        request.addData(SUBSCRIBE_TO_NEWSLETTER, subscribeToNewsletter.toString());

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of "auth/getpublickey" API.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @return Request ID generated for this request.
     * @throws NullPointerException when engine is NULL
     */
    public static int getPublicKey(BaseEngine engine) {
        if (engine == null) {
            throw new NullPointerException("Auth.getPublicKey() engine cannot be NULL");
        }

        Request request = new Request(FUNCTION_GET_PUBLIC_KEY, Request.Type.RETRIEVE_PUBLIC_KEY,
                engine.engineId(), false, Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData("type", "all");

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of "auth/gettermsandconditions" API.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @return request ID generated for this request.
     * @throws NullPointerException when engine is NULL
     */
    public static int getTermsAndConditions(BaseEngine engine) {
        if (engine == null) {
            throw new NullPointerException("Auth.getPublicKey() engine cannot be NULL");
        }

        Request request = new Request(FUNCTION_GET_TERMS_AND_CONDITIONS,
                Request.Type.TEXT_RESPONSE_ONLY, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData(LANGUAGE_CULTURE, getLocalString());
        request.addData(FLAGS, "0");

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Request Privacy statement from back-end.
     * 
     * @param engine Handle to LoginEngine which handles requests using this
     *            API.
     * @return request ID generated for this request.
     * @throws NullPointerException when engine is NULL
     */
    public static int getPrivacyStatement(BaseEngine engine) {
        if (engine == null) {
            throw new NullPointerException("Auth.getPublicKey() engine cannot be NULL");
        }

        Request request = new Request(FUNCTION_GET_PRIVACY_STATEMENT,
                Request.Type.TEXT_RESPONSE_ONLY, engine.engineId(), false,
                Settings.API_REQUESTS_TIMEOUT_AUTH);
        request.addData(LANGUAGE_CULTURE, getLocalString());
        request.addData(FLAGS, "0");

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /***
     * Returns the Local string in a formation suitable for the back end.
     * 
     * @return Local as a String
     */
    private static String getLocalString() {
        return Locale.getDefault().toString().replace('_', '-');
    }
}
