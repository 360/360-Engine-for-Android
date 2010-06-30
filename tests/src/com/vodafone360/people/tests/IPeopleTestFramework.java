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

package com.vodafone360.people.tests;

import com.vodafone360.people.service.transport.IConnection;
import com.vodafone360.people.engine.EngineManager.EngineId;

/**
 * Interface to allow communication between test-code elements
 * i.e. dummy connection thread and 
 *
 */
public interface IPeopleTestFramework {
	
	/**
	 * Return handle to dummy connection thread used by test-code
	 * @return handle to dummy connection which is derived from IRPGConnection
	 */
	IConnection testConnectionThread();
	
	/**
	 * Allows dummy connection thread to report back to test element making use of it
	 * @param reqId id of request
	 * @param engine ID of engine associated with the request
	 */
	void reportBackToFramework(int reqId, EngineId engine);
}
