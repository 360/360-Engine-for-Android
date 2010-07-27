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

package com.vodafone360.people.engine.login;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.InvalidCipherTextException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;

import com.vodafone360.people.ApplicationCache;
import com.vodafone360.people.Intents;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.StateTable;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.datatypes.SimpleText;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.contactsync.NativeContactsApi;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.UiAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.service.io.api.Auth;
import com.vodafone360.people.service.receivers.SmsBroadcastReceiver;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.LoginPreferences;

/**
 * Engine handling sign in to existing accounts and sign up to new accounts.
 */
public class LoginEngine extends BaseEngine {
    /**
     * List of states for LoginEngine.
     */
    private enum State {
        NOT_INITIALISED,
        NOT_REGISTERED,
        LOGGED_ON,
        LOGGED_OFF,
        LOGGED_OFF_WAITING_FOR_RETRY,
        LOGGED_OFF_WAITING_FOR_NETWORK,
        LOGIN_FAILED,
        LOGIN_FAILED_WRONG_CREDENTIALS,
        SIGNING_UP,
        FETCHING_TERMS_OF_SERVICE,
        FETCHING_PRIVACY_STATEMENT,
        FETCHING_USERNAME_STATE,
        REQUESTING_ACTIVATION_CODE,
        CREATING_SESSION_MANUAL,
        ACTIVATING_ACCOUNT,
        CREATING_SESSION_AUTO,
        RETRIEVING_PUBLIC_KEY
    }

    /**
     * Text coming from the server contains these carriage return characters,
     * which need to be exchanged with space characters to improve layout.
     */
    private static final char CARRIAGE_RETURN_CHARACTER = (char) 13;

    /**
     * Text coming from the server contains carriage return characters, which
     * need to be exchanged with these space characters to improve layout.
     */
    private static final char SPACE_CHARACTER = (char) 32;
    /**
     * used for sending unsolicited ui events
     */
    private final UiAgent mUiAgent = mEventCallback.getUiAgent();

    /**
     * mutex for thread synchronization
     */
    private final Object mMutex = new Object();
    /**
     * To convert between seconds and milliseconds
     */
    private static final int MS_IN_SECONDS = 1000;

    /**
     * Current state of the engine
     */
    private State mState = State.NOT_INITIALISED;

    /**
     * Context used for listening to SMS events
     */
    private Context mContext;

    /**
     * Database used for fetching/storing state information
     */
    private DatabaseHelper mDb;

    /**
     * Subscriber ID fetched from SIM card
     */
    private String mCurrentSubscriberId;

    /**
     * Android telephony manager used for fetching subscriber ID
     */
    private TelephonyManager mTelephonyManager;

    /**
     * Contains the authenticated session information while the user is logged
     * in
     */
    private static AuthSessionHolder sActivatedSession = null;

    /**
     * Contains user login details such as user name and password
     */
    private final LoginDetails mLoginDetails = new LoginDetails();

    /**
     * Contains a public key for encrypting login details to be sent to the
     * server
     */
    private PublicKeyDetails mPublicKey = new PublicKeyDetails();

    /**
     * Contains user registration details such as name, username, password, etc.
     */
    private final RegistrationDetails mRegistrationDetails = new RegistrationDetails();

    /**
     * Determines if current login information from database can be used to
     * establish a session. Set to false if login fails repeatedly.
     */
    private boolean mAreLoginDetailsValid = true;

    /**
     * Timeout used when waiting for the server to sent an activation SMS. If
     * this time in milliseconds is exceeded, the login will fail with SMS not
     * received error.
     */
    private static final long ACTIVATE_LOGIN_TIMEOUT = 72000;

    /**
     * Holds the activation code once an activation SMS has been received.
     */
    private String mActivationCode = null;

    /**
     * Set to true when sign in or sign up has been completed by the user. When
     * this is false the landing page will be displayed when the application is
     * launched.
     */
    private boolean mIsRegistrationComplete = false;

    /**
     * Contains a list of login engine observers which will be notified when the
     * login engine changes state.
     */
    private final ArrayList<ILoginEventsListener> mEventsListener = new ArrayList<ILoginEventsListener>();

    /**
     * Determines if the user is currently logged in with a valid session
     */
    private boolean mCurrentLoginState = false;

    /**
     * Listener interface that can be used by clients to receive login state
     * events from the engine.
     */
    public static interface ILoginEventsListener {
        void onLoginStateChanged(boolean loggedIn);
    }

    /**
     * Public constructor.
     * 
     * @param context The service Context
     * @param eventCallback Provides access to useful engine manager
     *            functionality
     * @param db The Now+ database used for fetching/storing login state
     *            information
     */
    public LoginEngine(Context context, IEngineEventCallback eventCallback, DatabaseHelper db) {
        super(eventCallback);
        LogUtils.logD("LoginEngine.LoginEngine()");
        mContext = context;
        mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mDb = db;

        mEngineId = EngineId.LOGIN_ENGINE;
    }

    /**
     * This will be called immediately after creation to perform extra
     * initialisation.
     */
    @Override
    public void onCreate() {
        LogUtils.logD("LoginEngine.OnCreate()");
        mState = State.NOT_INITIALISED;
        mCurrentSubscriberId = mTelephonyManager.getSubscriberId();
        IntentFilter filter = new IntentFilter(SmsBroadcastReceiver.ACTION_ACTIVATION_CODE);
        mContext.registerReceiver(mEventReceiver, filter);
        mIsRegistrationComplete = StateTable.isRegistrationComplete(mDb.getReadableDatabase());
    }

    /**
     * This will be called just before the engine is shutdown. Cleans up any
     * resources used.
     */
    @Override
    public void onDestroy() {
        LogUtils.logD("LoginEngine.onDestroy()");

        Intent intent = new Intent();
        intent.setAction(Intents.HIDE_LOGIN_NOTIFICATION);
        mContext.sendBroadcast(intent);

        removeAllListeners();
        mContext.unregisterReceiver(mEventReceiver);
    }

    /**
     * Called by the INowPlusService implementation to start a manual login.
     * Adds a manual login UI request to the queue and kicks the worker thread.
     * 
     * @param loginDetails The login information entered by the user
     */
    public void addUiLoginRequest(LoginDetails loginDetails) {
        LogUtils.logD("LoginEngine.addUiLoginRequest()");
        addUiRequestToQueue(ServiceUiRequest.LOGIN, loginDetails);
    }

    /**
     * Called by the INowPlusService implementation to remove user data Issues a
     * UI request to effectively log out.
     */
    public void addUiRemoveUserDataRequest() {
        LogUtils.logD("LoginEngine.addUiRemoveUserDataRequest()");
        addUiRequestToQueue(ServiceUiRequest.REMOVE_USER_DATA, null);
    }

    /**
     * Called by the INowPlusService implementation to start the sign-up
     * process. Adds a registration UI request to the queue and kicks the worker
     * thread.
     * 
     * @param details The registration details entered by the user
     */
    public void addUiRegistrationRequest(RegistrationDetails details) {
        LogUtils.logD("LoginEngine.addUiRegistrationRequest()");
        addUiRequestToQueue(ServiceUiRequest.REGISTRATION, details);
    }

    /**
     * Called by the INowPlusService implementation to fetch terms of service
     * text for the UI. Adds a fetch terms of service UI request to the queue
     * and kicks the worker thread.
     */
    public void addUiFetchTermsOfServiceRequest() {
        LogUtils.logD("LoginEngine.addUiFetchTermsOfServiceRequest()");
        addUiRequestToQueue(ServiceUiRequest.FETCH_TERMS_OF_SERVICE, null);
    }

    /**
     * Called by the INowPlusService implementation to privacy statement for the
     * UI. Adds a fetch privacy statement UI request to the queue and kicks the
     * worker thread.
     */
    public void addUiFetchPrivacyStatementRequest() {
        LogUtils.logD("LoginEngine.addUiFetchPrivacyStatementRequest()");
        addUiRequestToQueue(ServiceUiRequest.FETCH_PRIVACY_STATEMENT, null);
    }

    /**
     * Called by the INowPlusService implementation to check if a username is
     * available for registration. Adds a get username state UI request to the
     * queue and kicks the worker thread.
     * 
     * @param username Username to fetch the state of
     * TODO: Not currently used by UI.
     */
    public void addUiGetUsernameStateRequest(String username) {
        LogUtils.logD("LoginEngine.addUiGetUsernameStateRequest()");
        addUiRequestToQueue(ServiceUiRequest.USERNAME_AVAILABILITY, username);
    }

    /**
     * Return the absolute time in milliseconds when the engine needs to run
     * (based on System.currentTimeMillis).
     * 
     * @return -1 never needs to run, 0 needs to run as soon as possible,
     *         CurrentTime + 60000 to run in 1 minute, etc.
     */
    @Override
    public long getNextRunTime() {
        if (isCommsResponseOutstanding()) {
            return 0;
        }
        if (uiRequestReady()) {
            return 0;
        }
        switch (mState) {
            case NOT_INITIALISED:
                return 0;
            case NOT_REGISTERED:
            case LOGGED_ON:
            case LOGIN_FAILED:
            case LOGIN_FAILED_WRONG_CREDENTIALS:
                return -1;
            case REQUESTING_ACTIVATION_CODE:
            case SIGNING_UP:
                if (mActivationCode != null) {
                    return 0;
                }
                break;
            case LOGGED_OFF:
                return 0;
            case LOGGED_OFF_WAITING_FOR_NETWORK:
                if (NetworkAgent.getAgentState() == AgentState.CONNECTED) {
                    return 0;
                }
                break;
            case RETRIEVING_PUBLIC_KEY:
                return 0;
            default:
                // Do nothing.
                break;
        }
        return getCurrentTimeout();
    }

    /**
     * Do some work but anything that takes longer than 1 second must be broken
     * up. The type of work done includes:
     * <ul>
     * <li>If a comms response from server is outstanding, process it</li>
     * <li>If a timeout is pending, process it</li>
     * <li>If an SMS activation code has been received, process it</li>
     * <li>Retry auto login if necessary</li>
     * </ul>
     */
    @Override
    public void run() {
        LogUtils.logD("LoginEngine.run()");
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }
        if (processTimeout()) {
            return;
        }
        switch (mState) {
            case NOT_INITIALISED:
                initialiseEngine();
                return;
            case REQUESTING_ACTIVATION_CODE:
            case SIGNING_UP:
                if (mActivationCode != null) {
                    handleSmsResponse();
                    return;
                }
                break;
            case RETRIEVING_PUBLIC_KEY:
                break;
            case LOGGED_OFF:
            case LOGGED_OFF_WAITING_FOR_NETWORK:
                // AA mCurrentNoOfRetries = 0;
                if (retryAutoLogin()) {
                    return;
                }
            default: // do nothing.
                break;
        }
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
        if (!isUiRequestOutstanding()) {
            return false;
        }
        switch (mState) {
            case NOT_REGISTERED:
            case LOGGED_OFF:
            case LOGGED_ON:
            case LOGIN_FAILED:
            case CREATING_SESSION_AUTO:
            case LOGIN_FAILED_WRONG_CREDENTIALS:
            case LOGGED_OFF_WAITING_FOR_RETRY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Determines if the user is currently logged in with a valid session.
     * 
     * @return true if logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return mState == State.LOGGED_ON;
    }

    /**
     * Add a listener which will receive events whenever the login state
     * changes.
     * 
     * @param listener The callback interface
     */
    public synchronized void addListener(ILoginEventsListener listener) {
        LogUtils.logD("LoginEngine.addListener()");
        if (!mEventsListener.contains(listener)) {
            mEventsListener.add(listener);
        }
    }

    /**
     * Remove a listener added by the addListener function.
     * 
     * @param listener The same callback interface passed in to the add function
     */
    public synchronized void removeListener(ILoginEventsListener listener) {
        LogUtils.logD("LoginEngine.removeListener()");
        mEventsListener.remove(listener);
    }

    /**
     * Remove all ILoginStateChangeListener (done as part of cleanup).
     */
    private synchronized void removeAllListeners() {
        LogUtils.logD("LoginEngine.removeAllListeners()");
        if (mEventsListener != null) {
            mEventsListener.clear();
        }
    }

    /**
     * Once the engine has finished processing a user request, this function is
     * called to restore the state back to an appropriate value (based on the
     * user login state).
     */
    private synchronized void restoreLoginState() {
        LogUtils.logD("LoginEngine.restoreLoginState");
        if (mIsRegistrationComplete) {
            if (sActivatedSession != null) {
                newState(State.LOGGED_ON);
            } else {
                if (mAreLoginDetailsValid) {
                    newState(State.LOGGED_OFF);
                } else {
                    newState(State.LOGIN_FAILED);
                }
            }
        } else {
            newState(State.NOT_REGISTERED);
        }
    }

    /**
     * Called when a server response is received, processes the response based
     * on the engine state.
     * 
     * @param resp Response data from server
     */
    @Override
    protected void processCommsResponse(ResponseQueue.DecodedResponse resp) {
        LogUtils.logD("LoginEngine.processCommsResponse() - resp = " + resp);
        switch (mState) {
            case SIGNING_UP:
                handleSignUpResponse(resp.mDataTypes);
                break;
            case RETRIEVING_PUBLIC_KEY:
                handleNewPublicKeyResponse(resp.mDataTypes);
                break;
            case CREATING_SESSION_MANUAL:
                handleCreateSessionManualResponse(resp.mDataTypes);
                break;
            case CREATING_SESSION_AUTO:
                handleCreateSessionAutoResponse(resp.mDataTypes);
                break;
            case REQUESTING_ACTIVATION_CODE:
                handleRequestingActivationResponse(resp.mDataTypes);
                break;
            case ACTIVATING_ACCOUNT:
                handleActivateAccountResponse(resp.mDataTypes);
                break;
            case FETCHING_TERMS_OF_SERVICE:
            case FETCHING_PRIVACY_STATEMENT:
            case FETCHING_USERNAME_STATE:
                handleServerSimpleTextResponse(resp.mDataTypes, mState);
                break;
            default: // do nothing.
                break;
        }
    }

    /**
     * Called when a UI request is ready to be processed. Handlers the UI
     * request based on the type.
     * 
     * @param requestId UI request type
     * @param data Interpretation of this data depends on the request type
     */
    @Override
    protected void processUiRequest(ServiceUiRequest requestId, Object data) {
        LogUtils.logD("LoginEngine.processUiRequest() - reqID = " + requestId);
        switch (requestId) {
            case LOGIN:
                startManualLoginProcess((LoginDetails)data);
                break;
            case REGISTRATION:
                startRegistrationProcessCrypted((RegistrationDetails)data);
                break;
            case REMOVE_USER_DATA:
                startLogout();
                // Remove NAB Account at this point (does nothing on 1.X)
                NativeContactsApi.getInstance().removePeopleAccount();
                super.onReset(); // Sets the reset flag as done
                break;
            case LOGOUT:
                startLogout();
                break;
            case FETCH_TERMS_OF_SERVICE:
                startFetchTermsOfService();
                break;
            case FETCH_PRIVACY_STATEMENT:
                startFetchPrivacyStatement();
                break;
            case USERNAME_AVAILABILITY:
                startFetchUsernameState((String)data);
                break;
            default:
                completeUiRequest(ServiceStatus.ERROR_NOT_FOUND, null);
        }
    }

    /**
     * Called by the run() function the first time it is executed to perform
     * non-trivial initialisation such as auto login.
     */
    private void initialiseEngine() {
        LogUtils.logD("LoginEngine.initialiseEngine()");
        if (ServiceStatus.SUCCESS == mDb.fetchLogonCredentialsAndPublicKey(mLoginDetails,
                mPublicKey)) {
            if (mLoginDetails.mSubscriberId != null
                    && !mLoginDetails.mSubscriberId.equals(mCurrentSubscriberId)) {
                LogUtils.logW("SIM card has changed.  Login session invalid");
            } else {
                sActivatedSession = StateTable.fetchSession(mDb.getReadableDatabase());
            }
        }
        mAreLoginDetailsValid = true;
        restoreLoginState();
        clearTimeout();
    }

    /**
     * Starts the sign-up process where the password is RSA encrypted. A setting
     * determines if this function is used in preference to
     * {@link #startRegistrationProcess(RegistrationDetails)} function.
     * 
     * @param details Registration details received from the UI request
     */
    private void startRegistrationProcessCrypted(RegistrationDetails details) {
        LogUtils.logD("startRegistrationCrypted");
        if (details == null || details.mUsername == null || details.mPassword == null) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, null);
            return;
        }

        setRegistrationComplete(false);
        setActivatedSession(null);
        mRegistrationDetails.copy(details);
        mLoginDetails.mUsername = mRegistrationDetails.mUsername;
        mLoginDetails.mPassword = mRegistrationDetails.mPassword;

        try {
            final long timestampInSeconds = System.currentTimeMillis() / MS_IN_SECONDS;
            final byte[] theBytes = prepareBytesForSignup(timestampInSeconds);
            mLoginDetails.mAutoConnect = true;
            mLoginDetails.mRememberMe = true;
            mLoginDetails.mMobileNo = mRegistrationDetails.mMsisdn;
            mLoginDetails.mSubscriberId = mCurrentSubscriberId;
            mDb.modifyCredentialsAndPublicKey(mLoginDetails, mPublicKey);

            startSignUpCrypted(theBytes, timestampInSeconds);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encrypts the sign-up data ready for sending to the server
     * 
     * @param timeStampInSeconds Current time in milliseconds
     * @return Raw data that can be sent to the server
     */
    private byte[] prepareBytesForSignup(long timeStampInSeconds) throws InvalidCipherTextException {
        byte[] theBytes = null;
        if (mPublicKey != null) {
            if (mPublicKey.mExponential != null && (mPublicKey.mModulus != null)) {
                theBytes = RSAEncryptionUtils.encryptRSA(RSAEncryptionUtils.getRSAPubKey(
                        mPublicKey.mModulus, mPublicKey.mExponential), makeSecurePassword(
                        mLoginDetails.mUsername, mLoginDetails.mPassword, timeStampInSeconds));
            }
        }
        if (theBytes == null) {
            RSAEncryptionUtils.copyDefaultPublicKey(mPublicKey);// we'll store
            // the default
            // public key
            // into the db
            theBytes = RSAEncryptionUtils.encryptRSA(RSAEncryptionUtils.getDefaultPublicKey(),
                    makeSecurePassword(mLoginDetails.mUsername, mLoginDetails.mPassword,
                            timeStampInSeconds));
        }
        return theBytes;
    }

    /**
     * Concatenates the username, password, current time and some other data
     * into a string which can be encrypted and sent to the server.
     * 
     * @param userName User name for sign-in or sign-up
     * @param password Password as entered by user
     * @param ts Current time in milliseconds
     * @return Concatenated data
     */
    private static String makeSecurePassword(String userName, String password, long ts) {
        String appSecret = SettingsManager.getProperty(Settings.APP_SECRET_KEY);// RSAEncrypter.testAppSecretThrottled;
        final char amp = '&';
        if (ts <= 0 || //
                userName == null || userName.trim().length() == 0 || //
                password == null || password.trim().length() == 0 || //
                // set application key somewhere
                appSecret == null || appSecret.trim().length() == 0)
            return null;
        final String passwordT = password.trim();
        final String userNameT = userName.trim();
        final StringBuffer sb = new StringBuffer();
        sb.append(appSecret).append(amp).append(Long.toString(ts)).append(amp).append(userNameT)
                .append(amp).append(passwordT);
        return sb.toString();
    }

    /**
     * Puts the engine into the signing up state and sends an encrypted sign-up
     * request to the server.
     * 
     * @param theBytes byte-array containing encrypted password data.
     * @param timestamp Current timestamp.
     */
    private void startSignUpCrypted(byte[] theBytes, long timestamp) {
        LogUtils.logD("startSignUpCrypted()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        mActivationCode = null;
        newState(State.SIGNING_UP);

        if (!validateRegistrationDetails()) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, null);
            return;
        }
        int reqId = Auth.signupUserCrypted(
                this,
                mRegistrationDetails.mFullname,
                mRegistrationDetails.mUsername,
                theBytes, // what is encrypted
                timestamp, mRegistrationDetails.mEmail, mRegistrationDetails.mBirthdayDate,
                mRegistrationDetails.mMsisdn, mRegistrationDetails.mAcceptedTAndC,
                mRegistrationDetails.mCountrycode, mRegistrationDetails.mTimezone,
                mRegistrationDetails.mLanguage, mRegistrationDetails.mMobileOperatorId,
                mRegistrationDetails.mMobileModelId, mRegistrationDetails.mSendConfirmationMail,
                mRegistrationDetails.mSendConfirmationSms,
                mRegistrationDetails.mSubscribeToNewsLetter);
        if (!setReqId(reqId)) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Basic check to determine if the registration details given by the user
     * are valid
     * 
     * @return true if the details are valid, false otherwise.
     */
    private boolean validateRegistrationDetails() {
        if (mRegistrationDetails.mFullname == null || mRegistrationDetails.mEmail == null
                || mRegistrationDetails.mBirthdayDate == null
                || mRegistrationDetails.mMsisdn == null
                || mRegistrationDetails.mAcceptedTAndC == null
                || mRegistrationDetails.mCountrycode == null
                || mRegistrationDetails.mTimezone == null || mRegistrationDetails.mLanguage == null
                || mRegistrationDetails.mMobileOperatorId == null
                || mRegistrationDetails.mMobileModelId == null
                || mRegistrationDetails.mSendConfirmationMail == null
                || mRegistrationDetails.mSendConfirmationSms == null
                || mRegistrationDetails.mSubscribeToNewsLetter == null) {
            return false;
        }
        return true;
    }

    /**
     * Requests a new Public Key from the server.
     */
    public void getNewPublicKey() {
        LogUtils.logD("LoginEngine.getNewPublicKey");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        mActivationCode = null;
        newState(State.RETRIEVING_PUBLIC_KEY);
        if (!setReqId(Auth.getPublicKey(this))) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Sends a fetch terms of service request to the server.
     */
    private void startFetchTermsOfService() {
        LogUtils.logD("LoginEngine.startFetchTermsOfService()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            updateTermsState(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        newState(State.FETCHING_TERMS_OF_SERVICE);
        if (!setReqId(Auth.getTermsAndConditions(this))) {
            updateTermsState(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Sends a fetch privacy statement request to the server.
     */
    private void startFetchPrivacyStatement() {
        LogUtils.logD("LoginEngine.startFetchPrivacyStatement()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            updateTermsState(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        newState(State.FETCHING_PRIVACY_STATEMENT);
        if (!setReqId(Auth.getPrivacyStatement(this))) {
            updateTermsState(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Sends a fetch user-name state request to the server.
     * 
     * @param username, the user-name to retrieve information for.
     */
    private void startFetchUsernameState(String username) {
        LogUtils.logD("LoginEngine.startFetchUsernameState()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        mLoginDetails.mUsername = username;
        newState(State.FETCHING_USERNAME_STATE);
        if (!setReqId(Auth.getUsernameState(this, username))) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Sets registration complete flag to false then starts a new sign-in
     * request.
     * 
     * @param details Login details received from the UI request
     */
    private void startManualLoginProcess(LoginDetails details) {
        LogUtils.logD("LoginEngine.startManualLoginProcess()");
        setRegistrationComplete(false);
        setActivatedSession(null);
        if (details == null) {
            completeUiRequest(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, null);
            return;
        }
        mLoginDetails.copy(details);
        mLoginDetails.mSubscriberId = mCurrentSubscriberId;
        mDb.modifyCredentialsAndPublicKey(mLoginDetails, mPublicKey);

        if (Settings.ENABLE_ACTIVATION) {
            startRequestActivationCode();
        } else {
            startGetSessionManual();
        }
    }

    /**
     * Sends a request activation code request to the server.
     */
    private void startRequestActivationCode() {
        LogUtils.logD("LoginEngine.startRequestActivationCode()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        mActivationCode = null;
        newState(State.REQUESTING_ACTIVATION_CODE);
        if (!setReqId(Auth.requestActivationCode(this, mLoginDetails.mUsername,
                mLoginDetails.mMobileNo))) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Sends a get session by credentials request to the server.
     */
    private void startGetSessionManual() {
        LogUtils.logD("LoginEngine.startGetSessionManual()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        newState(State.CREATING_SESSION_MANUAL);
        if (!setReqId(Auth.getSessionByCredentials(this, mLoginDetails.mUsername,
                mLoginDetails.mPassword, null))) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
    }

    /**
     * Sends a activate account request to the server.
     */
    private void startActivateAccount() {
        LogUtils.logD("LoginEngine.startActivateAccount()");
        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            completeUiRequest(ServiceStatus.ERROR_COMMS, null);
            return;
        }
        if (Settings.ENABLE_ACTIVATION) {
            newState(State.ACTIVATING_ACCOUNT);
            if (!setReqId(Auth.activate(this, mActivationCode))) {
                completeUiRequest(ServiceStatus.ERROR_COMMS, null);
                return;
            }
        } else {
            setRegistrationComplete(true);
            completeUiRequest(ServiceStatus.SUCCESS, null);
        }
    }

    /**
     * Clears the session and registration complete information so that the user
     * will need to manually login again to use the now+ services. Does not
     * currently send a request to the server to log out.
     */
    private void startLogout() {
        LogUtils.logD("LoginEngine.startLogout()");
        setRegistrationComplete(false);
        setActivatedSession(null);
        completeUiRequest(ServiceStatus.SUCCESS, null);
    }

    /**
     * Clears the session and registration complete information so that the user
     * will need to manually login again to use the now+ services. Does not
     * currently send a request to the server to log out.
     */
    public final void logoutAndRemoveUser() {

        LogUtils.logD("LoginEngine.startLogout()");
        addUiRemoveUserDataRequest();
        LoginPreferences.clearPreferencesFile(mContext);
        mUiAgent.sendUnsolicitedUiEvent(ServiceUiRequest.UNSOLICITED_GO_TO_LANDING_PAGE, null);
    }

    /**
     * Retries to log the user in based on credential information stored in the
     * database.
     * 
     * @return true if the login process was able to start
     */
    public boolean retryAutoLogin() {
        LogUtils.logD("LoginEngine.retryAutoLogin()");
        setActivatedSession(null);
        if (ServiceStatus.SUCCESS != mDb.fetchLogonCredentialsAndPublicKey(mLoginDetails,
                mPublicKey)) {
            LogUtils
                    .logE("LoginEngine.retryAutoLogin() - Unable to fetch credentials from database");
            mAreLoginDetailsValid = false;
            newState(State.LOGIN_FAILED);
            return false;
        }

        // AA: commented the condition out if (Settings.ENABLE_ACTIVATION) {
        // AA: the old version if (mCurrentSubscriberId == null ||
        // !mCurrentSubscriberId.equals(mLoginDetails.mSubscriberId)) { //
        // logging off/fail will be done in another way according to bug 8288
        if (mCurrentSubscriberId != null
                && !mCurrentSubscriberId.equals(mLoginDetails.mSubscriberId)) {
            LogUtils.logV("LoginEngine.retryAutoLogin() -"
                    + " SIM card has changed or is missing (old subId = "
                    + mLoginDetails.mSubscriberId + ", new subId = " + mCurrentSubscriberId + ")");
            mAreLoginDetailsValid = false;
            newState(State.LOGIN_FAILED);
            return false;
        }
        // }

        if (mLoginDetails.mUsername == null || mLoginDetails.mPassword == null
                || mLoginDetails.mMobileNo == null) {
            LogUtils.logV("LoginEngine.retryAutoLogin() - Username, password "
                    + "or mobile number are missing (old username = " + mLoginDetails.mUsername
                    + ", mobile no = " + mLoginDetails.mMobileNo + ")");
            mAreLoginDetailsValid = false;
            newState(State.LOGIN_FAILED);
            return false;
        }
        mAreLoginDetailsValid = true;

        if (NetworkAgent.getAgentState() != NetworkAgent.AgentState.CONNECTED) {
            newState(State.LOGGED_OFF_WAITING_FOR_NETWORK);
            LogUtils.logV("LoginEngine.retryAutoLogin() - Internet connection down.  "
                    + "Will try again when connection is available");
            return false;
        }

        newState(State.CREATING_SESSION_AUTO);
        if (!setReqId(Auth.getSessionByCredentials(this, mLoginDetails.mUsername,
                mLoginDetails.mPassword, null))) {
            return false;
        }
        return true;
    }

    /**
     * Helper function to set the registration complete flag and update the
     * database with the new state.
     * 
     * @param value true if registration is completed
     */
    private void setRegistrationComplete(boolean value) {
        LogUtils.logD("LoginEngine.setRegistrationComplete(" + value + ")");
        if (value != mIsRegistrationComplete) {
            StateTable.setRegistrationComplete(value, mDb.getWritableDatabase());
            mIsRegistrationComplete = value;
            if (mIsRegistrationComplete) {
                // Create NAB Account at this point (does nothing on 1.X
                // devices)
                final NativeContactsApi nabApi = NativeContactsApi.getInstance();
                if (!nabApi.isPeopleAccountCreated()) {
                    // TODO: React upon failure to create account
                    nabApi.addPeopleAccount(LoginPreferences.getUsername());
                }
            }
        }
    }

    /**
     * Changes the state of the engine. Also displays the login notification if
     * necessary.
     * 
     * @param newState The new state
     */
    private void newState(State newState) {
        State oldState = mState;
        synchronized (mMutex) {
            if (newState == mState) {
                return;
            }
            mState = newState;
        }
        LogUtils.logV("LoginEngine.newState: " + oldState + " -> " + mState);
        Intent intent = null;
        // Update notification
        switch (mState) {
            case LOGIN_FAILED_WRONG_CREDENTIALS:
                intent = new Intent();
                intent.setAction(Intents.START_LOGIN_ACTIVITY);
                mContext.sendBroadcast(intent);

                setRegistrationComplete(false);
                break;
            // here should be no break
            case NOT_REGISTERED:
            case LOGIN_FAILED:
                intent = new Intent();
                intent.setAction(Intents.LOGIN_FAILED);
                mContext.sendBroadcast(intent);

                setRegistrationComplete(false);
                // startLogout();
                // mDb.removeUserData();
                // sending user to login screen again
                // should be done by UI itself because
                // when it's done from here it cause problems when user tries to
                // login
                // giving wrong credentials, ui flow will be broken

                break;
            case LOGGED_OFF:
            case LOGGED_OFF_WAITING_FOR_NETWORK:
            case LOGGED_OFF_WAITING_FOR_RETRY:
            case LOGGED_ON:
                intent = new Intent();
                intent.setAction(Intents.HIDE_LOGIN_NOTIFICATION);
                mContext.sendBroadcast(intent);
                break;
            default:// do nothing
                break;
        }

        // Update listeners with any state changes
        switch (mState) {
            case NOT_REGISTERED:
            case LOGIN_FAILED:
            case LOGIN_FAILED_WRONG_CREDENTIALS:
            case LOGGED_OFF:
                onLoginStateChanged(false);
                break;
            case LOGGED_ON:
                onLoginStateChanged(true);
                break;
            default: // do nothing.
                break;
        }
    }

    /**
     * Called when the engine transitions between the logged in and logged out
     * states. Notifies listeners.
     * 
     * @param loggedIn true if the user is now logged in, false otherwise.
     */
    private synchronized void onLoginStateChanged(boolean loggedIn) {
        LogUtils.logD("LoginEngine.onLoginStateChanged() Login state changed to "
                + (loggedIn ? "logged in." : "logged out."));
        if (loggedIn == mCurrentLoginState) {
            return;
        }
        mCurrentLoginState = loggedIn;
        for (ILoginEventsListener listener : mEventsListener) {
            listener.onLoginStateChanged(loggedIn);
        }
    }

    /**
     * A helper function which determines which activity should be displayed
     * when the UI is loaded.
     * 
     * @return true if landing page should be displayed, false otherwise
     */
    public synchronized boolean getLoginRequired() {
        LogUtils.logD("LoginEngine.getLoginRequired() - " + !mIsRegistrationComplete);
        return !mIsRegistrationComplete;
    }

    /**
     * Retrieves the active comms session.
     * 
     * @return The session or NULL if the user is logged out.
     */
    public static AuthSessionHolder getSession() {
        return sActivatedSession;
    }

    /**
     * Helper function to store the new session in the database and inform
     * clients that the session has changed.
     * 
     * @param session The new session or NULL if the user has logged off.
     */
    public synchronized void setActivatedSession(AuthSessionHolder session) {
        LogUtils.logD("LoginEngine.setActivatedSession() session[" + session + "]");
        sActivatedSession = session;
        if (session != null) {
            LogUtils.logD("LoginEngine.setActivatedSession() Login successful");
        } else {
            LogUtils.logW("LoginEngine.setActivatedSession() "
                    + "Login unsuccessful, the session is NULL");
        }
        StateTable.setSession(session, mDb.getWritableDatabase());
    }

    /**
     * Called when a response to the sign-up API is received. In case of success
     * sets a timeout value waiting for the activation SMS to arrive.
     * 
     * @param data The received data
     */
    private void handleSignUpResponse(List<BaseDataType> data) {
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.CONTACT_DATA_TYPE, data);
        LogUtils.logD("LoginEngine.handleSignUpResponse() errorStatus[" + errorStatus.name() + "]");
        if (errorStatus == ServiceStatus.SUCCESS) {
            LogUtils.logD("LoginEngine.handleSignUpResponse() - Registration successful");
            if (!Settings.ENABLE_ACTIVATION) {
                startGetSessionManual();
            } else {
                // Now waiting for SMS...
                setTimeout(ACTIVATE_LOGIN_TIMEOUT);
            }
            // AA
        } else if (errorStatus == ServiceStatus.ERROR_INVALID_PUBLIC_KEY) {
            // start new key retrieval and make the new cycle
            getNewPublicKey();

        } else {
            completeUiRequest(errorStatus, null);
        }
    }

    /**
     * Called when a response to the server fetch public key request is
     * received. Validates the response and stores the new public key details.
     * 
     * @param mDataTypes Response data from server.
     */
    private void handleNewPublicKeyResponse(List<BaseDataType> mDataTypes) {
        LogUtils.logD("LoginEngine.handleNewPublicKeyResponse()");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.PUBLIC_KEY_DETAILS_DATA_TYPE, mDataTypes);
        if (errorStatus == ServiceStatus.SUCCESS) {
            LogUtils.logD("LoginEngine.handleNewPublicKeyResponse() - Succesfully retrieved");
            // AA
            // 1. save to DB; save the flag that we aren't using default and
            // have to use one from DB
            // 2. start registration again
            mPublicKey = (PublicKeyDetails)mDataTypes.get(0);
            // done in startRegistrationProcessCrypted already
            // mDb.modifyCredentialsAndPublicKey(mLoginDetails, mPublicKey);
            startRegistrationProcessCrypted(mRegistrationDetails);
        } else {
            completeUiRequest(errorStatus, null);
        }
    }

    /**
     * Called when a response to the GetSessionByCredentials API is received
     * (manual login). In case of success, tries to activate the account using
     * the activation code received by SMS.
     * 
     * @param data The received data
     */
    private void handleCreateSessionManualResponse(List<BaseDataType> data) {
        LogUtils.logD("LoginEngine.handleCreateSessionManualResponse()");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.AUTH_SESSION_HOLDER_TYPE, data);
        if (errorStatus == ServiceStatus.SUCCESS && (data.size() > 0)) {
            setActivatedSession((AuthSessionHolder)data.get(0));
            startActivateAccount();
            return;
        }
        completeUiRequest(errorStatus, null);
    }

    /**
     * Called when a response to the GetSessionByCredentials API is received
     * (auto login). In case of success, moves to the logged in state
     * 
     * @param data The received data
     */
    private void handleCreateSessionAutoResponse(List<BaseDataType> data) {
        LogUtils.logD("LoginEngine.handleCreateSessionResponse()");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.AUTH_SESSION_HOLDER_TYPE, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            clearTimeout();
            setActivatedSession((AuthSessionHolder)data.get(0));
            newState(State.LOGGED_ON);
        } else {
            LogUtils.logE("LoginEngine.handleCreateSessionResponse() - Auto Login Failed Error "
                    + errorStatus);
            // AA:the 1st retry failed, just go to the start page,
            // if (loginAttemptsRemaining()
            // && errorStatus!=ServiceStatus.ERROR_INVALID_PASSWORD) {
            // newState(State.LOGGED_OFF_WAITING_FOR_RETRY);
            // setTimeout(LOGIN_RETRY_TIME);
            // } else {
            // mAreLoginDetailsValid = false;
            if (errorStatus == ServiceStatus.ERROR_INVALID_PASSWORD) {
                mAreLoginDetailsValid = false;
                newState(State.LOGIN_FAILED_WRONG_CREDENTIALS);
            } else {
                newState(State.LOGIN_FAILED);
            }
            // }
        }
    }

    /**
     * Called when a response to the RequestActivationCode API is received
     * (manual login). In case of success, tries to fetch a login session from
     * the server.
     * 
     * @param data The received data
     */
    private void handleRequestingActivationResponse(List<BaseDataType> data) {
        LogUtils.logD("LoginEngine.handleRequestingActivationResponse()");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.STATUS_MSG_DATA_TYPE, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            // Now waiting for SMS...
            setTimeout(ACTIVATE_LOGIN_TIMEOUT);
            return;
        }
        completeUiRequest(errorStatus, null);
    }

    /**
     * Called when a response to the Activate API is received (manual login or
     * sign-up). In case of success, moves to the logged in state and completes
     * the manual login or sign-up request.
     * 
     * @param data The received data
     */
    private void handleActivateAccountResponse(List<BaseDataType> data) {
        LogUtils.logD("LoginEngine.handleActivateAccountResponse()");
        ServiceStatus errorStatus = getResponseStatus(BaseDataType.STATUS_MSG_DATA_TYPE, data);
        if (errorStatus == ServiceStatus.SUCCESS) {
            LogUtils
                    .logD("LoginEngine.handleActivateAccountResponse: ** Mobile number activated **");
            setRegistrationComplete(true);
            completeUiRequest(ServiceStatus.SUCCESS, null);
            return;
        }
        setActivatedSession(null);
        completeUiRequest(ServiceStatus.ERROR_ACCOUNT_ACTIVATION_FAILED, null);
    }

    /**
     * Called when a response to the GetTermsAndConditions, GetPrivacyStatement
     * and GetUsernameState APIs are received. In case of success, completes the
     * request and passes the response data to the UI.
     * 
     * @param data The received data
     */
    private void handleServerSimpleTextResponse(List<BaseDataType> data, State type) {
        LogUtils.logV("LoginEngine.handleServerSimpleTextResponse()");
        ServiceStatus serviceStatus = getResponseStatus(BaseDataType.SIMPLE_TEXT_DATA_TYPE, data);

        String result = null;
        if (serviceStatus == ServiceStatus.SUCCESS) {
            result = ((SimpleText) data.get(0)).mValue.toString().replace(
                    CARRIAGE_RETURN_CHARACTER, SPACE_CHARACTER);

            switch (type) {
                case FETCHING_TERMS_OF_SERVICE:
                    LogUtils.logD("LoginEngine.handleServerSimpleTextResponse() Terms of Service");
                    ApplicationCache.setTermsOfService(result, mContext);
                    break;
                case FETCHING_PRIVACY_STATEMENT:
                    LogUtils.logD("LoginEngine.handleServerSimpleTextResponse() Privacy Statemet");
                    ApplicationCache.setPrivacyStatemet(result, mContext);
                    break;
                case FETCHING_USERNAME_STATE:
                    // TODO: Unused by UI.
                    break;
            }
        }

        updateTermsState(serviceStatus, result);
    }

    /***
     * Informs the UI to update any terms which are being shown on screen.
     *
     * @param serviceStatus Current ServiceStatus.
     * @param messageText Legacy call for old UI (TODO: remove after UI-Refresh
     *          merge).  NULL when combined with a ServiceStatus of
     *          ERROR_COMMS, or contains the Privacy or Terms and Conditions
     *          text to be displayed in the UI.
     */
    private void updateTermsState(ServiceStatus serviceStatus, String messageText) {
        ApplicationCache.setTermsStatus(serviceStatus);

        /** Trigger UiAgent. **/
        mUiAgent.sendUnsolicitedUiEvent(ServiceUiRequest.TERMS_CHANGED_EVENT, null);

        /** Clear this request from the UI queue. **/
        completeUiRequest(serviceStatus, messageText);
    }

    /**
     * A broadcast receiver which is used to receive notifications when a data
     * SMS arrives.
     */
    private final BroadcastReceiver mEventReceiver = new BroadcastReceiver() {
        /**
         * Called when an SMS arrives. The activation code is extracted from the
         * given intent and the worker thread kicked.
         * 
         * @param context Context from which the intent was broadcast
         * @param intent Will only process the SMS which the action is
         *            {@link SmsBroadcastReceiver#ACTION_ACTIVATION_CODE}.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.logD("LoginEngine.BroadcastReceiver.onReceive - Processing data sms");
            if (intent.getAction().equals(SmsBroadcastReceiver.ACTION_ACTIVATION_CODE)) {
                String activationCode = intent.getStringExtra("code");
                LogUtils
                        .logD("LoginEngine.BroadcastReceiver.onReceive - Activation code Received: "
                                + activationCode);
                synchronized (LoginEngine.this) {
                    mActivationCode = activationCode;
                }
                mEventCallback.kickWorkerThread();
            }
        }

    };

    /**
     * Called when an SMS is received during sign-up or manual login. Starts
     * requesting a session from the server.
     */
    private void handleSmsResponse() {
        LogUtils.logD("LoginEngine.handleSmsResponse(" + mActivationCode + ")");
        clearTimeout();
        startGetSessionManual();
    }

    /**
     * Called by the base engine implementation whenever a UI request is
     * completed to do any necessary cleanup. We use it to restore our state to
     * a suitable value.
     */
    @Override
    protected void onRequestComplete() {
        LogUtils.logD("LoginEngine.onRequestComplete()");
        restoreLoginState();
    }

    /**
     * Handles timeouts for SMS activation and auto login retries.
     */
    protected synchronized void onTimeoutEvent() {
        LogUtils.logD("LoginEngine.onTimeoutEvent()");
        switch (mState) {
            case REQUESTING_ACTIVATION_CODE:
            case SIGNING_UP:
                completeUiRequest(ServiceStatus.ERROR_SMS_CODE_NOT_RECEIVED, null);
                break;
            case LOGGED_OFF_WAITING_FOR_RETRY:
                retryAutoLogin();
                break;
            default: // do nothing.
                break;
        }
    }

    /**
     * Called by the framework before a remove user data operation takes place.
     * Initiates a suitable UI request which will kick the worker thread.
     */
    @Override
    public void onReset() {
        addUiRemoveUserDataRequest();
    }

    /**
     * Set 'dummy' auth session for test purposes only.
     * 
     * @param session 'dummy' session supplied to LoginEngine
     */
    public static void setTestSession(AuthSessionHolder session) {
        sActivatedSession = session;
    }
}
