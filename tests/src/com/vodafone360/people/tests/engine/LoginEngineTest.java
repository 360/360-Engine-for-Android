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

package com.vodafone360.people.tests.engine;

import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.RegistrationDetails;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SimpleText;
import com.vodafone360.people.datatypes.StatusMsg;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.engine.login.LoginEngine.ILoginEventsListener;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.io.ResponseQueue;
import com.vodafone360.people.tests.TestModule;

@Suppress
public class LoginEngineTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver, ILoginEventsListener {

    /**
     * States for test harness
     */
    enum LoginTestState {
        IDLE,
        ON_CREATE,
        ON_DESTROY,
        LOGIN_REQUEST,
        LOGIN_REQUEST_VALID,
        REMOVE_USER_DATA_REQ,
        LOGOUT_REQ,
        REGISTRATION,
        REGISTRATION_ERROR,
        GET_T_AND_C,
        FETCH_PRIVACY,
        USER_NAME_STATE,
        GET_NEXT_RUN_TIME,
        SERVER_ERROR,
        SMS_RESPONSE_SIGNIN,
        ON_REMOVE_USERDATA
    }

    EngineTestFramework mEngineTester = null;

    LoginEngine mEng = null;

    LoginTestState mState = LoginTestState.IDLE;

    MainApplication mApplication = null;

    TestModule mTestModule = new TestModule();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
                getInstrumentation().getTargetContext());
        mApplication.onCreate();

        mEngineTester = new EngineTestFramework(this);
        mEng = new LoginEngine(getInstrumentation().getTargetContext(), mEngineTester, mApplication
                .getDatabase());
        mEngineTester.setEngine(mEng);
        mState = LoginTestState.IDLE;

        mEng.addListener(this);
    }

    @Override
    protected void tearDown() throws Exception {

        // stop our dummy thread?
        mEngineTester.stopEventThread();
        mEngineTester = null;

        mEng.removeListener(this);
        mEng = null;

        // call at the end!!!
        super.tearDown();
    }

    @MediumTest
    public void testOnCreate() {
        boolean testPassed = true;
        mState = LoginTestState.ON_CREATE;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            mEng.onCreate();
        } catch (Exception e) {
            testPassed = false;
        }

        assertTrue(testPassed == true);
    }

    @MediumTest
    public void testOnDestroy() {
        boolean testPassed = true;
        mState = LoginTestState.ON_DESTROY;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);

        try {
            mEng.onCreate();
        } catch (Exception e) {
            testPassed = false;
        }

        try {
            mEng.onDestroy();
        } catch (Exception e) {
            Log.d("TAG", e.toString());
            testPassed = false;
        }

        // expect failure as we've not
        assertTrue(testPassed == true);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testLoginNullDetails() {
        mState = LoginTestState.LOGIN_REQUEST;

        LoginDetails loginDetails = null;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            synchronized (mEngineTester) {

                mEng.addUiLoginRequest(loginDetails);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testLoginNullDetails test 1 failed with exception");
        }

        loginDetails = new LoginDetails();

        try {
            synchronized (mEngineTester) {
                mEng.addUiLoginRequest(loginDetails);

                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_COMMS, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testLoginNullDetails test 2 failed with exception");
        }
    }

    private void displayException(Exception e) {
        String strExceptionInfo = e + "\n";
        for (int i = 0; i < e.getStackTrace().length; i++) {
            StackTraceElement v = e.getStackTrace()[i];
            strExceptionInfo += "\t" + v + "\n";
        }

        Log.e("TAG", "General exception occurred State = " + mState + "\n" + strExceptionInfo);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testLoginValidDetails() {
        mState = LoginTestState.LOGIN_REQUEST;
        LoginDetails loginDetails = new LoginDetails();
        loginDetails.mMobileNo = "123456789";
        loginDetails.mUsername = "bob";
        loginDetails.mPassword = "ajob";

        try {
            synchronized (mEngineTester) {
                mEng.addUiLoginRequest(loginDetails);

                ServiceStatus status = mEngineTester.waitForEvent(120000);
                assertEquals(ServiceStatus.ERROR_SMS_CODE_NOT_RECEIVED, status);
            }

            // test actually receiving the SMS
        } catch (Exception e) {
            displayException(e);
            fail("testLoginValidDetails test 1 failed with exception");
        }
    }

    @MediumTest
    public void testRemoveUserData() {
        boolean testPassed = true;
        mState = LoginTestState.REMOVE_USER_DATA_REQ;
        try {
            synchronized (mEngineTester) {
                mEng.addUiRemoveUserDataRequest();

                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("SUCCESS"));
                }
            }

            // test actually receiving the SMS
        } catch (Exception e) {
            testPassed = false;
        }

        try {
            synchronized (mEngineTester) {
                mEng.onReset();

                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("SUCCESS"));
                }
            }
            // test actually receiving the SMS
        } catch (Exception e) {
            testPassed = false;
        }

        assertTrue(testPassed == true);
    }

    @MediumTest
    public void testLogout() {
        boolean testPassed = true;
        mState = LoginTestState.LOGOUT_REQ;
        try {
            synchronized (mEngineTester) {
                mEng.addUiRequestToQueue(ServiceUiRequest.LOGOUT, null);
                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("SUCCESS"));
                }
            }
            // test actually receiving the SMS
        } catch (Exception e) {
            testPassed = false;
        }

        assertTrue(testPassed == true);
    }

    /*
     * addUiRegistrationRequest(non-Javadoc)
     * @seecom.vodafone360.people.tests.engine.IEngineTestFrameworkObserver#
     * reportBackToEngine(int,
     * com.vodafone360.people.engine.EngineManager.EngineId)
     */

    @MediumTest
    @Suppress // Breaks tests.
    public void testRegistration() {
        mState = LoginTestState.REGISTRATION;

        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);

        RegistrationDetails details = null;
        try {
            synchronized (mEngineTester) {
                mEng.addUiRegistrationRequest(details);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testRegistration test 1 failed with exception");
        }

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            synchronized (mEngineTester) {
                mEng.addUiRegistrationRequest(details);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testRegistration test 2 failed with exception");
        }

        details = new RegistrationDetails();
        try {
            synchronized (mEngineTester) {
                mEng.addUiRegistrationRequest(details);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testRegistration test 3 failed with exception");
        }

        details.mUsername = "bwibble";
        details.mPassword = "qqqqqq";

        try {
            synchronized (mEngineTester) {
                mEng.addUiRegistrationRequest(details);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_BAD_SERVER_PARAMETER, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testRegistration test 4 failed with exception");
        }

        details.mFullname = "Billy Wibble";
        details.mBirthdayDate = "12345678";
        details.mEmail = "billy@wibble.com";
        details.mMsisdn = "123456";
        details.mAcceptedTAndC = true;
        details.mCountrycode = "uk";
        details.mTimezone = "gmt";
        details.mLanguage = "english";
        details.mSendConfirmationMail = true;
        details.mSendConfirmationSms = true;
        details.mSubscribeToNewsLetter = true;
        details.mMobileOperatorId = new Long(1234);
        details.mMobileModelId = new Long(12345);

        try {
            synchronized (mEngineTester) {
                mEng.addUiRegistrationRequest(details);
                ServiceStatus status = mEngineTester.waitForEvent(120000);
                assertEquals(ServiceStatus.ERROR_SMS_CODE_NOT_RECEIVED, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testRegistration test 5 failed with exception");
        }
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testGetTermsAndConditions() {
        boolean testPassed = true;
        mState = LoginTestState.GET_T_AND_C;
        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);
        try {
            synchronized (mEngineTester) {
                mEng.addUiFetchTermsOfServiceRequest();
                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("Expected SUCCESS"));
                }
            }
        } catch (Exception e) {
            testPassed = false;
        }

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            synchronized (mEngineTester) {
                mEng.addUiFetchTermsOfServiceRequest();
                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("Expected SUCCESS"));
                }
            }
        } catch (Exception e) {
            testPassed = false;
        }

        assertTrue(testPassed == true);
    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testFetchPrivacy() {

        boolean testPassed = true;

        mState = LoginTestState.FETCH_PRIVACY;
        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);

        try {
            synchronized (mEngineTester) {
                mEng.addUiFetchPrivacyStatementRequest();
                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("Expected SUCCESS"));
                }
            }
        } catch (Exception e) {
            testPassed = false;
        }

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        try {
            synchronized (mEngineTester) {
                mEng.addUiFetchPrivacyStatementRequest();
                ServiceStatus status = mEngineTester.waitForEvent();
                if (status != ServiceStatus.SUCCESS) {
                    throw (new RuntimeException("Expected SUCCESS"));
                }
            }
        } catch (Exception e) {
            testPassed = false;
        }
        assertTrue(testPassed == true);

    }

    @MediumTest
    @Suppress // Breaks tests.
    public void testFetchUserNameState() {
        mState = LoginTestState.USER_NAME_STATE;

        String userName = null;

        try {
            synchronized (mEngineTester) {
                mEng.addUiGetUsernameStateRequest(userName);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.ERROR_COMMS, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testFetchUserNameState test 1 failed with exception");
        }

        userName = "bwibble";

        try {
            synchronized (mEngineTester) {
                mEng.addUiGetUsernameStateRequest(userName);
                ServiceStatus status = mEngineTester.waitForEvent();
                assertEquals(ServiceStatus.SUCCESS, status);
            }
        } catch (Exception e) {
            displayException(e);
            fail("testFetchUserNameState test 2 failed with exception");
        }
    }

    @MediumTest
    public void testGetNextRuntime() {
        boolean testPassed = true;
        mState = LoginTestState.GET_NEXT_RUN_TIME;

        long nt = mEng.getNextRunTime();
        if (nt != 0) {
            testPassed = false;
        }

        assertTrue(testPassed == true);
    }

    // @MediumTest
    // public void testSmsResponse(){
    // LoginDetails loginDetails = new LoginDetails();
    // loginDetails.mMobileNo = "123456789";
    // loginDetails.mUsername = "bob";
    // loginDetails.mPassword = "ajob";
    //		
    // mState = LoginTestState.LOGIN_REQUEST;
    // NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
    // synchronized(mEngineTester){
    // mEng.addUiLoginRequest(loginDetails);
    //			
    // ServiceStatus status = mEngineTester.waitForEvent();
    // assertEquals(ServiceStatus.SUCCESS, status);
    // }
    // // test actually receiving the SMS
    // mState = LoginTestState.SMS_RESPONSE_SIGNIN;
    //		
    // mEng.handleSmsResponse();
    //		
    // ServiceStatus status = mEngineTester.waitForEvent(30000);
    // assertEquals(ServiceStatus.SUCCESS, status);
    // }

    @MediumTest
    public void testPublicFunctions() {

        mEng.getLoginRequired();

        NetworkAgent.setAgentState(NetworkAgent.AgentState.DISCONNECTED);
        mEng.getNewPublicKey();

        NetworkAgent.setAgentState(NetworkAgent.AgentState.CONNECTED);
        mEng.getNewPublicKey();

        mEng.isDeactivated();

        mEng.setActivatedSession(new AuthSessionHolder());
    }

    @Override
    public void reportBackToEngine(int reqId, EngineId engine) {
        Log.d("TAG", "LoginEngineTest.reportBackToEngine");
        ResponseQueue respQueue = ResponseQueue.getInstance();
        List<BaseDataType> data = new ArrayList<BaseDataType>();

        // Request Activation code

        switch (mState) {
            case IDLE:
                break;
            case LOGIN_REQUEST:
            case SMS_RESPONSE_SIGNIN:
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine FETCH ids");
                StatusMsg msg = new StatusMsg();
                data.add(msg);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                mState = LoginTestState.LOGIN_REQUEST_VALID;
                break;
            case LOGIN_REQUEST_VALID:
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine FETCH ids");
                AuthSessionHolder sesh = new AuthSessionHolder();
                data.add(sesh);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                mState = LoginTestState.SMS_RESPONSE_SIGNIN;
                break;
            case REGISTRATION:
                Log.d("TAG", "IdentityEngineTest.reportBackToEngine Registration");
                data.add(mTestModule.createDummyContactData());
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case REGISTRATION_ERROR:
                data.add(new ServerError());
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case GET_T_AND_C:
            case FETCH_PRIVACY:
            case USER_NAME_STATE:
                SimpleText txt = new SimpleText();
                txt.addText("Simple text");
                data.add(txt);
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            case SERVER_ERROR:
                data.add(new ServerError());
                respQueue.addToResponseQueue(reqId, data, engine);
                mEng.onCommsInMessage();
                break;
            default:
        }

    }

    @Override
    public void onLoginStateChanged(boolean loggedIn) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onEngineException(Exception exp) {
        // TODO Auto-generated method stub

    }

}
