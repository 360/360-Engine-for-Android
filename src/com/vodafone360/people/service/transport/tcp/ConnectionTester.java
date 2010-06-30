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
package com.vodafone360.people.service.transport.tcp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.rpg.RpgHeader;
import com.vodafone360.people.service.io.rpg.RpgMessageTypes;
import com.vodafone360.people.service.transport.http.HttpConnectionThread;
import com.vodafone360.people.service.utils.hessian.HessianEncoder;

public class ConnectionTester {
    /**
     * We wait at least 59 seconds for a response from the server. After that we
     * return false.
     */
    private static final int MAX_TEST_RESPONSE_WAIT_TIME_MILLIS = 59 * 1000; // 59 seconds


    private DataInputStream mIs;

    private OutputStream mOs;

    public ConnectionTester(InputStream is, OutputStream os) {
        mIs = new DataInputStream(is);
        mOs = os;
    }

    /**
     * Starts a test by sending a test message to the RPG, which the RPG should
     * reply to.
     * 
     * @return True if the test succeeded, false if the RPG did not respond in a
     *         correct manner.
     */
    public boolean runTest() {
        if ((null == mIs) || (null == mOs)) {
            return false;
        }

        if (!sendTestMessage()) {
            HttpConnectionThread.logE("ConnectionTester.runTest()", "Could not send test message.",
                    null);
            return false;
        }

        if (!receiveTestResponse()) {
            HttpConnectionThread.logE("ConnectionTester.runTest()",
                    "Did not receive test response.", null);
            return false;
        }

        return true;
    }

    /**
     * Attempts to send a TCP test message (type 101) to the RPG.
     * 
     * @return True if sending happened without any exceptions false otherwise.
     */
    private boolean sendTestMessage() {
        try {
            HttpConnectionThread.logI("ConnectionTester.sentTestMessage()", "Sending TCP test request...");
            byte[] payload = getConnectionTestHessianPayload();
            mOs.write(payload);
        } catch (IOException ioe) {
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            mOs = null; // clear reference
        }

        return true;
    }

    /**
     * Attempts to read any valid RPG response (even if it is not the requested TCP test
     * response)
     * 
     * @return True if we were able to receive an RPG response, otherwise false will 
     * be returned.
     */
    private boolean receiveTestResponse() {
        int waitingTimeMillis = 0;

        try {
            // wait until we have bytes available or we have waited for MAX_WAIT_TIME_MILLIS
            while (mIs.available() <= 0) {
                if (waitingTimeMillis <= MAX_TEST_RESPONSE_WAIT_TIME_MILLIS) {
                    try {
                        Thread.sleep(200);
                        waitingTimeMillis += 200;
                    } catch (InterruptedException ie) {
                    }
                } else {
                    return false;   // we timed out waiting for bytes
                }
            }
        } catch (IOException ioe) {
            return false;
        } finally {
            mIs = null; // clear reference
        }

        HttpConnectionThread.logI("ConnectionTester.receiveTestResponse()", "Connection test successful!!");
        return true;
    }

    /**
     * Returns a byte-array containing the data needed for sending a connection
     * test to the RPG.
     * 
     * @throws IOException If there was an exception serializing the hash map to
     *             a hessian byte array.
     * @return A byte array representing the connection test request.
     */
    private byte[] getConnectionTestHessianPayload() throws IOException {
        // hash table for parameters to Hessian encode
        final Hashtable<String, Object> ht = new Hashtable<String, Object>();

        final AuthSessionHolder auth = LoginEngine.getSession();
        ht.put("userid", auth.userID);

        // do Hessian encoding
        final byte[] payload = HessianEncoder.createHessianByteArray("", ht);
        payload[1] = (byte)1;
        payload[2] = (byte)0;

        final int reqLength = RpgHeader.HEADER_LENGTH + payload.length;

        final RpgHeader rpgHeader = new RpgHeader();
        rpgHeader.setPayloadLength(payload.length);
        rpgHeader.setReqType(RpgMessageTypes.RPG_TCP_CONNECTION_TEST);

        final byte[] rpgMsg = new byte[reqLength];
        System.arraycopy(rpgHeader.createHeader(), 0, rpgMsg, 0, RpgHeader.HEADER_LENGTH);

        if (null != payload) {
            System.arraycopy(payload, 0, rpgMsg, RpgHeader.HEADER_LENGTH, payload.length);
        }

        return rpgMsg;
    }
}
