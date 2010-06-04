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

package com.vodafone360.people.tests.service.transport.tcp.conn_less.hb_tests;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;


import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.transport.DecoderThread;

import android.test.InstrumentationTestCase;

public class HeartbeatSenderThreadTest extends InstrumentationTestCase {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	@MediumTest
	public void testSendHeartbeat_valid() {
		byte[] header = {	(byte) 0xFF,
							(byte) 0xFF, 
							(byte) 0x64};
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		MockTcpConnectionThread connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		MockHeartbeatSenderThread hbSender = new MockHeartbeatSenderThread(connThread, null,
				null //QUICKFIX: Not sure about this value
				);
		
		AuthSessionHolder authHolder = new AuthSessionHolder();
		authHolder.userID = 007;
		authHolder.sessionID = "SESS_ID";
		authHolder.sessionSecret = "SESS_SECRET";
		authHolder.userName = "James_Bond";
		
		LoginEngine.setTestSession(authHolder);
		hbSender.setOutputStream(baos);
		
		try {
			hbSender.sendHeartbeat();
		} catch (Exception e) {
			fail("sendHeartbeatTest() should not throw an Exception here " + e.toString());
		}
		
		try {
			baos.flush();
			baos.close();			
		} catch (IOException ioe) {}

		byte[] payload = baos.toByteArray();
		if (null != payload) {
			boolean isHeaderOk = false,
					isEndingOk = false,
					isPayloadLenOk = false;
			
			if ((header[0] == payload[0]) &&
				(header[1] == payload[1]) &&
				(header[2] == payload[2])) {
				isHeaderOk = true;
			} else {
				fail("RPG Header was malformed!");
			}
			
			if (payload[payload.length -1] == ((byte) 0x7A)) {
					isPayloadLenOk = true;
			} else {
				fail("Message End was malformed! Char was: " + payload[payload.length -1]);
			}
			
			int payloadSize = byteArrayToInt(
							new byte[] {payload[11], payload[12], payload[13], payload[14]}, 0);
			if (payloadSize == (payload.length - 16)) {
				isEndingOk = true;
			} else {
				fail("Payload length is not okay: " + payloadSize + 
										" vs. " + (payload.length - 16));
			}
			
			assertTrue((isHeaderOk && isEndingOk && isPayloadLenOk));
		} else {
			fail("HB-Payload was null!");
		}
	}
	
	
	@MediumTest
	public void testSendHeartbeat_exception() {
		MockByteArrayOutputStream baos = new MockByteArrayOutputStream();		
		MockTcpConnectionThread connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		MockHeartbeatSenderThread hbSender = new MockHeartbeatSenderThread(connThread, null, 
				null //QUICKFIX: Not sure about this value
				);
		
		AuthSessionHolder authHolder = new AuthSessionHolder();
		authHolder.userID = 007;
		authHolder.sessionID = "SESS_ID";
		authHolder.sessionSecret = "SESS_SECRET";
		authHolder.userName = "James_Bond";
		
		LoginEngine.setTestSession(authHolder);
		hbSender.setOutputStream(baos);
		try {
			baos.close();
		} catch (IOException ioe) {}
		
		
		// IOException Test
		try {
			hbSender.sendHeartbeat();
		} catch (IOException ioe) {
			assertTrue(true);	// we succeed because we expect an IOE to be thrown here!
		} catch (Exception e) {
			fail("We should not have received a generic exception, but an IOException!");
		}
		baos = null;
		
		
		// NullPointerException Test
		hbSender.setOutputStream(null);
		try {
			hbSender.sendHeartbeat();
		} catch (IOException ioe) {
			fail("We should not have received an IOException, but a generic (NP-)Exception!");
		} catch (Exception e) {
			assertTrue(true);	// we succeed because we expect an IOE to be thrown here!
		}
		
	}
	
	@MediumTest
	public void testSetOutputStream() {
		MockTcpConnectionThread connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		MockHeartbeatSenderThread hbSender = new MockHeartbeatSenderThread(connThread, null,
				null //QUICKFIX: Not sure about this value
				);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		hbSender.setOutputStream(baos);
		assertEquals(baos, hbSender.getOutputStream());
	}
	
	@Suppress
	@MediumTest
	public void testStartConnection() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MockTcpConnectionThread connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		MockHeartbeatSenderThread hbSender = new MockHeartbeatSenderThread(connThread, null, 
				null //QUICKFIX: Not sure about this value
				);
		hbSender.setOutputStream(baos);
		
		hbSender.startConnection();
		
		assertTrue(hbSender.getIsActive());
		assertNotNull(hbSender.getConnectionThread());
		
		if (null != hbSender.getConnectionThread()) {
			assertTrue(hbSender.getConnectionThread().isAlive());
		}
		
		hbSender.stopConnection();
	}
	
	@Suppress
	@MediumTest
	public void testStopConnection() {
		DataOutputStream dos = new DataOutputStream(new ByteArrayOutputStream());
		MockTcpConnectionThread connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		MockHeartbeatSenderThread hbSender = new MockHeartbeatSenderThread(connThread, null,
				null //QUICKFIX: Not sure about this value
				);
		hbSender.setOutputStream(dos);
		
		hbSender.startConnection();
		try {
			Thread.sleep(500);
		} catch (Exception e) {}
		hbSender.stopConnection();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		
		assertNotNull(hbSender.getConnectionThread());
		if (null != hbSender.getConnectionThread()) {
			assertFalse(hbSender.getConnectionThread().isAlive());
			
			try {
				dos.write(1);
				fail("Should not be able to write here!");
			} catch (IOException e) {
				assertTrue(true);
			}
		}
	}
	
	@Suppress
	@MediumTest
	public void testRun_exception() {
		// IO Exception test
		MockByteArrayOutputStream baos = new MockByteArrayOutputStream();
		MockTcpConnectionThread connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		MockHeartbeatSenderThread hbSender = new MockHeartbeatSenderThread(connThread, null,
				null //QUICKFIX: Not sure about this value
				);
		hbSender.setOutputStream(baos);
		
		try {
			baos.close();
		} catch (IOException e) {}
		
		hbSender.startConnection();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		assertTrue(connThread.getDidErrorOccur());
		hbSender.stopConnection();
		connThread = null;
		hbSender = null;
		
		// NP Exception test
		connThread = new MockTcpConnectionThread(new DecoderThread(), null);
		hbSender = new MockHeartbeatSenderThread(connThread, null, 
				null //QUICKFIX: Not sure about this value
				);
		hbSender.setOutputStream(null);
		
		hbSender.startConnection();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		assertTrue(connThread.getDidErrorOccur());
		hbSender.stopConnection();
		connThread = null;
		hbSender = null;
	}
	
	public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }
}