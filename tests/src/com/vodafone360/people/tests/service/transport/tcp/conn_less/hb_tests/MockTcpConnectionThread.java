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

import com.vodafone360.people.service.RemoteService;
import com.vodafone360.people.service.transport.DecoderThread;
import com.vodafone360.people.service.transport.tcp.TcpConnectionThread;

public class MockTcpConnectionThread extends TcpConnectionThread {
	private boolean didErrorOccur;
	
	public MockTcpConnectionThread(DecoderThread decoder, RemoteService service) {
		super(decoder, service);
		didErrorOccur = false;
	}
	
	@Override
	public void notifyOfNetworkProblems() {
		didErrorOccur = true;
	}
	
	public boolean getDidErrorOccur() {
		return didErrorOccur;
	}
}
