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

package com.vodafone360.people.tests.engine.contactsync;

import com.vodafone360.people.engine.contactsync.ContactChange;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

public class ContactChangeTest extends InstrumentationTestCase {

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
	}
	
	@SmallTest
	public void testSettersAndGetters() {
		// at the same time test the initial values of the default constructor
		ContactChange cc = new ContactChange();
		
		assertEquals(ContactChange.DESTINATION_FLAG_NONE, cc.getDestinations());
		cc.setDestinations(ContactChange.DESTINATIONS_CAB_NAB_RPG);
		assertEquals(ContactChange.DESTINATIONS_CAB_NAB_RPG, cc.getDestinations());
		
		assertEquals(ContactChange.FLAG_NONE, cc.getFlags());
		cc.setFlags(ContactChange.FLAGS_HOME_CELL);
		assertEquals(ContactChange.FLAGS_HOME_CELL, cc.getFlags());
		
		assertEquals(ContactChange.TYPE_UNKNOWN, cc.getType());
		cc.setType(ContactChange.TYPE_UPDATE_BACKEND_CONTACT_ID);
		assertEquals(ContactChange.TYPE_UPDATE_BACKEND_CONTACT_ID, cc.getType());
				
		assertEquals(-1L, cc.getBackendContactId());
		cc.setBackendContactId(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, cc.getBackendContactId());
		
		assertEquals(-1L, cc.getBackendDetailId());
		cc.setBackendDetailId(Long.MIN_VALUE);
		assertEquals(Long.MIN_VALUE, cc.getBackendDetailId());
		
		assertEquals(-1L, cc.getInternalContactId());
		cc.setInternalContactId(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, cc.getInternalContactId());
		
		assertEquals(-1L, cc.getInternalDetailId());
		cc.setInternalDetailId(Long.MIN_VALUE);
		assertEquals(Long.MIN_VALUE, cc.getInternalDetailId());
		
		assertEquals(-1L, cc.getNabContactId());
		cc.setNabContactId(Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, cc.getNabContactId());
		
		assertEquals(-1L, cc.getNabDetailId());
		cc.setNabDetailId(Long.MIN_VALUE);
		assertEquals(Long.MIN_VALUE, cc.getNabDetailId());
		
		assertEquals(null, cc.getValue());
		assertEquals(ContactChange.KEY_UNKNOWN, cc.getKey());
	}
	
	@SmallTest
	public void testConstructors() {
		String value = new String("value");
		int type = ContactChange.TYPE_ADD_CONTACT;
		int destination = ContactChange.DESTINATIONS_CAB_RPG;
		long nabContactId = 100;
		long internalContactId = 200;
		long backendContactId = 300;
		
		//----------------------------------------
		ContactChange cc = new ContactChange(ContactChange.KEY_VCARD_PHONE, value, ContactChange.FLAG_HOME);
		
		assertEquals(ContactChange.KEY_VCARD_PHONE, cc.getKey());
		assertEquals("vcard.phone", cc.getKeyToString());
		assertEquals(value, cc.getValue());
		assertEquals(ContactChange.FLAG_HOME, cc.getFlags());
		
		assertEquals(ContactChange.DESTINATION_FLAG_NONE, cc.getDestinations());
		assertEquals(ContactChange.TYPE_UNKNOWN, cc.getType());
		assertEquals(-1L, cc.getBackendContactId());
		assertEquals(-1L, cc.getBackendDetailId());
		assertEquals(-1L, cc.getInternalContactId());
		assertEquals(-1L, cc.getInternalDetailId());
		assertEquals(-1L, cc.getNabContactId());		
		assertEquals(-1L, cc.getNabDetailId());

		//----------------------------------------
		cc = new ContactChange(type);
		
		assertEquals(type, cc.getType());
		
		assertEquals(ContactChange.DESTINATION_FLAG_NONE, cc.getDestinations());
		assertEquals(ContactChange.FLAG_NONE, cc.getFlags());
		assertEquals(-1L, cc.getBackendContactId());
		assertEquals(-1L, cc.getBackendDetailId());
		assertEquals(-1L, cc.getInternalContactId());
		assertEquals(-1L, cc.getInternalDetailId());
		assertEquals(-1L, cc.getNabContactId());		
		assertEquals(-1L, cc.getNabDetailId());
		assertEquals(null, cc.getValue());
		assertEquals(ContactChange.KEY_UNKNOWN, cc.getKey());
		
		//----------------------------------------
		cc = new ContactChange(Long.MAX_VALUE);

		assertEquals(Long.MAX_VALUE, cc.getNabContactId());
		
		assertEquals(ContactChange.DESTINATION_FLAG_NONE, cc.getDestinations());
		assertEquals(ContactChange.FLAG_NONE, cc.getFlags());
		assertEquals(ContactChange.TYPE_UNKNOWN, cc.getType());
		assertEquals(-1L, cc.getBackendContactId());
		assertEquals(-1L, cc.getBackendDetailId());
		assertEquals(-1L, cc.getInternalContactId());
		assertEquals(-1L, cc.getInternalDetailId());
		assertEquals(-1L, cc.getNabDetailId());
		assertEquals(null, cc.getValue());
		assertEquals(ContactChange.KEY_UNKNOWN, cc.getKey());

		//----------------------------------------
		cc = new ContactChange(Long.MAX_VALUE, Long.MIN_VALUE);

		assertEquals(Long.MAX_VALUE, cc.getNabContactId());
		assertEquals(Long.MIN_VALUE, cc.getNabDetailId());
		
		assertEquals(ContactChange.DESTINATION_FLAG_NONE, cc.getDestinations());
		assertEquals(ContactChange.FLAG_NONE, cc.getFlags());
		assertEquals(ContactChange.TYPE_UNKNOWN, cc.getType());
		assertEquals(-1L, cc.getBackendContactId());
		assertEquals(-1L, cc.getBackendDetailId());
		assertEquals(-1L, cc.getInternalContactId());
		assertEquals(-1L, cc.getInternalDetailId());
		assertEquals(null, cc.getValue());
		assertEquals(ContactChange.KEY_UNKNOWN, cc.getKey());
		
		//----------------------------------------
		cc = new ContactChange(destination, type);
		
		assertEquals(type, cc.getType());
		assertEquals(destination, cc.getDestinations());
		
		assertEquals(ContactChange.FLAG_NONE, cc.getFlags());
		assertEquals(-1L, cc.getBackendContactId());
		assertEquals(-1L, cc.getBackendDetailId());
		assertEquals(-1L, cc.getInternalContactId());
		assertEquals(-1L, cc.getInternalDetailId());
		assertEquals(-1L, cc.getNabContactId());		
		assertEquals(-1L, cc.getNabDetailId());
		assertEquals(null, cc.getValue());
		assertEquals(ContactChange.KEY_UNKNOWN, cc.getKey());
		
		//----------------------------------------
		ContactChange original = new ContactChange();
		original.setNabContactId(nabContactId);
		original.setBackendContactId(backendContactId);
		original.setInternalContactId(internalContactId);
		
		cc = ContactChange.createIdsChange(original, type);

		assertEquals(nabContactId, cc.getNabContactId());
		assertEquals(backendContactId, cc.getBackendContactId());
		assertEquals(internalContactId, cc.getInternalContactId());
		assertEquals(type, cc.getType());
		
		assertEquals(ContactChange.DESTINATION_FLAG_NONE, cc.getDestinations());
		assertEquals(ContactChange.FLAG_NONE, cc.getFlags());
		assertEquals(-1L, cc.getBackendDetailId());
		assertEquals(-1L, cc.getInternalDetailId());
		assertEquals(-1L, cc.getNabDetailId());
		assertEquals(null, cc.getValue());
		assertEquals(ContactChange.KEY_UNKNOWN, cc.getKey());
	}
	
}
