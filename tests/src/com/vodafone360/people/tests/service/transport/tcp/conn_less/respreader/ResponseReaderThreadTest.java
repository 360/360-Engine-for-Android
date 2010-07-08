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

package com.vodafone360.people.tests.service.transport.tcp.conn_less.respreader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;

import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.tests.service.transport.tcp.conn_less.hb_tests.MockTcpConnectionThread;

public class ResponseReaderThreadTest extends InstrumentationTestCase {

	public void setUp() throws Exception {}
	public void tearDown() throws Exception {}
	
	@Suppress
	@MediumTest
	public void testStartConnection() {
		DecoderThread decoder = new DecoderThread();
		MockTcpConnectionThread mockThread = new MockTcpConnectionThread(decoder, null);
		MockResponseReaderThread respReader = new MockResponseReaderThread(mockThread, decoder, 
				null // QUICKFIX: Not sure about this value 
				);
		MockOTAInputStream mIs = new MockOTAInputStream(
				new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}));
		respReader.setInputStream(new BufferedInputStream(mIs));
		
		assertNull(respReader.getConnectionThread());
		
		respReader.startConnection();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		assertNotNull(respReader.getConnectionThread());
		if (null != respReader.getConnectionThread()) {
			assertTrue(respReader.getConnectionThread().isAlive());
		}
		assertTrue(respReader.getIsConnectionRunning());
	}
	
	@Suppress
	@MediumTest
	public void testStopConnection() {
		DecoderThread decoder = new DecoderThread();
		MockTcpConnectionThread mockThread = new MockTcpConnectionThread(decoder, null);
		MockResponseReaderThread respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		MockOTAInputStream mIs = new MockOTAInputStream(
				new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}));
		respReader.setInputStream(new BufferedInputStream(mIs));

		respReader.startConnection();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		Thread t = respReader.getConnectionThread();
		assertNotNull(t);
		if (null != t) {
			assertTrue(t.isAlive());
			assertTrue(respReader.getIsConnectionRunning());
		}
		
		// now comes the actual test
		respReader.stopConnection();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		assertNull(respReader.getInputStream());
		assertNull(respReader.getConnectionThread());
		assertFalse(respReader.getIsConnectionRunning());
	}
	
	@MediumTest
	public void testSetInputstream() {
		DecoderThread decoder = new DecoderThread();
		MockTcpConnectionThread mockThread = new MockTcpConnectionThread(decoder, null);
		MockResponseReaderThread respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5});
		
		respReader.setInputStream(new BufferedInputStream(bais));
		DataInputStream dis = (DataInputStream) respReader.getInputStream();
		assertNotNull(dis);
		
		// let's test all the bytes...
		if (null != dis) {
			boolean areBytesCorrect = true;
			
			for (int i = 1; i < 6; i++) {
				try {
					int j = dis.readByte();
					if (-1 == j) {
						fail("Unexpected end of the DataInputStream");
						areBytesCorrect = false;
					} else if (i != j) {
						fail("Characters differ: i: " + i + " vs. j: " + j);
						areBytesCorrect = false;
					}
				} catch (IOException e) {}
			}
			
			assertTrue(areBytesCorrect);
		}
		
		respReader.setInputStream(null);
		assertNull(respReader.getInputStream());
	}
	
	@Suppress
	@MediumTest
	public void testRun_exception() {
		DecoderThread decoder = new DecoderThread();
		MockTcpConnectionThread mockThread = new MockTcpConnectionThread(decoder, null);
		MockResponseReaderThread respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		MockOTAInputStream mIs = new MockOTAInputStream(
				new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}));
		respReader.setInputStream(new BufferedInputStream(mIs));
		
		// IO Exception test
		try {
			mIs.close();
		} catch (IOException e) {}
		
		respReader.startConnection();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		assertTrue(mockThread.getDidErrorOccur());
		respReader.stopConnection();
		mockThread = null;
		respReader = null;
		
		// NP Exception test
		mockThread = new MockTcpConnectionThread(new DecoderThread(), null);
		respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		respReader.setInputStream(null);
		
		respReader.startConnection();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		assertTrue(mockThread.getDidErrorOccur());
		respReader.stopConnection();
		mockThread = null;
		respReader = null;
		
		// EOF Exception
		mockThread = new MockTcpConnectionThread(new DecoderThread(), null);
		respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {1});
		respReader.setInputStream(new BufferedInputStream(bais));
		
		respReader.startConnection();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {}
		assertTrue(mockThread.getDidErrorOccur());
		respReader.stopConnection();
		mockThread = null;
		respReader = null;
	}

	/***
	 * Test DecoderThread.getResponse() call.
	 */
	@Suppress
	@MediumTest
	public void testReadNextResponse() {
	    
	    /** Happy path test that runs through one whole response. **/
		MockDecoderThread decoder = new MockDecoderThread();
		MockTcpConnectionThread mockThread = new MockTcpConnectionThread(decoder, null);
		MockResponseReaderThread respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		byte[] payload = new byte[] {
		        /*** RPG header. **/
				((byte) 0xFF), ((byte) 0xFF), 0x04, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 5, 0,
				/** 5 bytes of payload. **/
				1, 2, 3, 4, 5 }; 												 
		
		ByteArrayInputStream bais = new ByteArrayInputStream(payload);
		respReader.setInputStream(new BufferedInputStream(bais));
		
		respReader.startConnection();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ie) {
		    // Do nothing.
		}
		assertNotNull("The decoder response should not have been NULL", decoder.getResponse());
		assertTrue("Incorrect payload", Arrays.equals(payload, decoder.getResponse()));
		respReader.stopConnection();
		payload = null;
		respReader = null;
		mockThread = null;
		decoder = null;
		bais = null;

		
		/** Sad path test where the 2nd byte is not the delimiter. **/
		decoder = new MockDecoderThread();
		mockThread = new MockTcpConnectionThread(decoder, null);
		respReader = new MockResponseReaderThread(mockThread, decoder,
				null // QUICKFIX: Not sure about this value
				);
		payload = new byte[] { 1, ((byte) 0xFF) };
		
		respReader.startConnection();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ie) {}
		assertNull(decoder.getResponse());
		respReader.stopConnection();
		payload = null;
		respReader = null;
		mockThread = null;
		decoder = null;
		bais = null;
	}
}