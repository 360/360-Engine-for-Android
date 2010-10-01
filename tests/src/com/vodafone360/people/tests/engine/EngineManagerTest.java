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

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ServiceTestCase;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.Settings;
import com.vodafone360.people.SettingsManager;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.activities.ActivitiesEngine;
import com.vodafone360.people.engine.contactsync.ContactSyncEngine;
import com.vodafone360.people.engine.content.ContentEngine;
import com.vodafone360.people.engine.groups.GroupsEngine;
import com.vodafone360.people.engine.identities.IdentityEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.engine.meprofile.SyncMeEngine;
import com.vodafone360.people.engine.presence.PresenceEngine;
import com.vodafone360.people.engine.upgrade.UpgradeEngine;
import com.vodafone360.people.service.RemoteService;

public class EngineManagerTest extends ServiceTestCase<RemoteService> implements
		IEngineTestFrameworkObserver {

	//private static final String LOG_TAG = "EngineManagerTest";
	private EngineTestFramework mEngineTester = null;
	private EngineManager mEngineManager = null;
	private MainApplication mApplication = null;

	/***
	 * Test cases constructor.
	 */
	public EngineManagerTest() {
		super(RemoteService.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	
		mApplication = (MainApplication) Instrumentation.newApplication(
				MainApplication.class, getContext());
		
		mApplication.onCreate();
		setApplication(mApplication);

		// We start the Remote Service which in-turn calls the EngineManager and
		// creates its instance. 
		getContext().startService(new Intent(getContext(), RemoteService.class));
	
		mEngineTester = new EngineTestFramework(this);
		mEngineTester.waitForEvent(10000);
		

	}

	@Override
	protected void tearDown() throws Exception {

		
		if (mApplication != null) {
			mApplication.onTerminate();
		    mApplication = null;
		}
		setApplication(null);

		mEngineTester.stopEventThread();
		mEngineTester = null;

		// call at the end!!!
		super.tearDown();
	}

	public void testCreateEngineManager() {

        mEngineManager = EngineManager.getInstance();
		assertTrue("Engine Manager should not be NULL", mEngineManager != null);
		
		getContext().stopService(new Intent(getContext(), RemoteService.class));
		mEngineTester.waitForEvent(10000);

	}

	public void testGetterForEngines() {
		mEngineManager = EngineManager.getInstance();
		assertTrue("Engine Manager should not be NULL", mEngineManager != null);

		LoginEngine loginEngine = mEngineManager.getLoginEngine();
		assertTrue("LoginEngine should not be NULL", loginEngine != null);

		if (SettingsManager.getProperty(Settings.UPGRADE_CHECK_URL_KEY) != null) {
			UpgradeEngine upgradeEngine = mEngineManager.getUpgradeEngine();
			assertTrue("UpgradeEngine should not be NULL", upgradeEngine != null);
		}

		ActivitiesEngine activitiesEngine = mEngineManager.getActivitiesEngine();
		assertTrue("ActivitiesEngine should not be NULL", activitiesEngine != null);

		SyncMeEngine syncMeEngine = mEngineManager.getSyncMeEngine();
		assertTrue("SyncMeEngine should not be NULL", syncMeEngine != null);

		PresenceEngine presenceEngine = mEngineManager.getPresenceEngine();
		assertTrue("PresenceEngine should not be NULL", presenceEngine != null);

		IdentityEngine identityEngine = mEngineManager.getIdentityEngine();
		assertTrue("IdentityEngine should not be NULL", identityEngine != null);

		ContentEngine contentEngine = mEngineManager.getContentEngine();
		assertTrue("ContentEngine should not be NULL", contentEngine != null);

		ContactSyncEngine contactSyncEngine = mEngineManager.getContactSyncEngine();
		assertTrue("ContactSyncEngine should not be NULL", contactSyncEngine != null);

		GroupsEngine groupsEngine = mEngineManager.getGroupsEngine();
		assertTrue("GroupsEngine should not be NULL", groupsEngine != null);

	}
	
	public void testResetAllEngines()
	{
		mEngineManager = EngineManager.getInstance();
		assertTrue("Engine Manager should not be NULL", mEngineManager != null);
		mEngineManager.resetAllEngines();
	}
	
	@Override
	public void reportBackToEngine(int reqId, EngineId engine) {

	}

	@Override
	public void onEngineException(Exception exp) {
		// TODO Auto-generated method stub

	}

}
