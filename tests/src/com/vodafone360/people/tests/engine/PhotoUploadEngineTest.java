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

import java.util.List;
import java.util.Vector;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.engine.photoupload.PhotoUploadEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.AlbumUtilsIn;
import com.vodafone360.people.utils.PhotoUtilsIn;

/**
 * Tests the use casees for PhotoUploadEngine.
 * 
 * @author mayank
 * 
 */
public class PhotoUploadEngineTest extends InstrumentationTestCase implements
        IEngineTestFrameworkObserver {
    /**
     * mEngineTestFramework is used to initialise the TestFramework.
     */
    private EngineTestFramework mEngineTester = null;
    /**
     *PhotoUploadEnigne variable.
     */
    private PhotoUploadEngine mEng = null;
    /**
     * Used to test whether call back comes from the framework.
     */
    Boolean mresult = false;

    @Override
    protected final void setUp() throws Exception {
        super.setUp();
        mEngineTester = new EngineTestFramework(this);
        mEng = new PhotoUploadEngine(mEngineTester);
        // mApplication =
        // (MainApplication)Instrumentation.newApplication(MainApplication.class,
        // getInstrumentation().getTargetContext());
        mEngineTester.setEngine(mEng);
        // mState = PresenceTestState.IDLE;
    }

    @Override
    protected final void tearDown() throws Exception {

        // stop our dummy thread?
        mEngineTester.stopEventThread();
        mEngineTester = null;
        mEng = null;

        // call at the end!!!
        super.tearDown();
    }

    /**
     * Function tests the addition of album to the server. In this case it will
     * just wait for callback in function. reportBacktoengine.
     */
    public final void testaddAlbum() {
        List<AlbumUtilsIn> list = new Vector<AlbumUtilsIn>();
        AlbumUtilsIn alb = new AlbumUtilsIn();
        alb.title = "testing";
        list.add(alb);
        mEng.addAlbum(list);
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals("Expected SUCCESS, not timeout", mresult,
                new Boolean(true));
        mresult = false;
    }

    /**
     * Testing the sharing of the album.
     */
    public final void testsharePhotoWithAlbum() {
        mEng.sharePhotoWithAlbum(13123L, 1231L);
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals("Expected SUCCESS, not timeout", mresult,
                new Boolean(true));
        mresult = false;
    }

    /**
     * Will fail as test framework does not deal with. direct http This function
     * uses direct http to communicate with server. Uploads the file
     */

    public final void testuploadFile() {
        List<PhotoUtilsIn> list = new Vector<PhotoUtilsIn>();
        PhotoUtilsIn photo = new PhotoUtilsIn();
        photo.filename = "/data/data/com.vodafone360.people/a1.png"; // mention
        // correct
        // file
        // path
        photo.filePath = "/data/data/com.vodafone360.people/a1.png";
        photo.bytesmime = "image/png";
        list.add(photo);
        mEng.loadPhoto(list);
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals("Expected SUCCESS, not timeout", mresult,
                new Boolean(true));
        mresult = false;

    }

    /**
     * Tests the Sharing of album with group.
     */
    public final void testsharealbumwithgroup() {
        mEng.shareAlbum(123L, 2312L);
        ServiceStatus status = mEngineTester.waitForEvent();
        assertEquals("Expected SUCCESS, not timeout", mresult,
                new Boolean(true));
        mresult = false;

    }

    @Override
    public final void reportBackToEngine(final int reqId, final EngineId engine) {
        // TODO Auto-generated met"hod stub
        Log.v("PhotoUpload -----", "reportBackToEngine");
        mresult = true;

    }

    @Override
    public void onEngineException(final Exception exp) {
        // TODO Auto-generated method stub

    }
}