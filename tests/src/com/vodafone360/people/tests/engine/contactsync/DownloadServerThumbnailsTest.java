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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import com.vodafone360.people.MainApplication;
import com.vodafone360.people.R;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.Contact;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeys;
import com.vodafone360.people.engine.BaseEngine.IEngineEventCallback;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.agent.NetworkAgent;
import com.vodafone360.people.service.agent.NetworkAgent.AgentState;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.service.io.ResponseQueue;

public class DownloadServerThumbnailsTest extends InstrumentationTestCase {

    private static final String LOG_TAG = "DownloadServerThumbnailsTest";

    /**
     * The main application handle.
     */
    private MainApplication mApplication;

    @Override
    protected void setUp() throws Exception {

        super.setUp();

        // delete the database
        getInstrumentation().getTargetContext().deleteDatabase(HelperClasses.getDatabaseName());

        // create an application instance
        mApplication = (MainApplication)Instrumentation.newApplication(MainApplication.class,
                getInstrumentation().getTargetContext());
        mApplication.onCreate();

        // setup dummy session
        AuthSessionHolder session = new AuthSessionHolder();
        session.userID = 0;
        session.sessionSecret = new String("sssh");
        session.userName = new String("bob");
        session.sessionID = new String("session");
        LoginEngine.setTestSession(session);
    }

    @Override
    protected void tearDown() throws Exception {

        if (mApplication != null) {
            mApplication.onTerminate();
        }

        mApplication = null;

        super.tearDown();
    }

    /**
     * Tests the thumbnails processor run when nothing has to be downloaded.
     */
    @Suppress
    public void testProcessorRun_nothingToDownload() {

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(
                engineEventCallback);
        // final DownloadServerThumbnails serverTumbnails = new
        // DownloadServerThumbnails(contactSyncCallback,
        // mApplication.getDatabase(), mApplication.getApplicationContext());

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // start server thumbnails sync processor
        // serverTumbnails.start();

        // processor should finish with a success
        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
    }

    /**
     * Checks the download of 1 thumbnail.
     */
    @Suppress
    // Breaks tests.
    public void testProcessorRun_simpleThumbnailDownload() {

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(
                engineEventCallback);
        // final DownloadServerThumbnails serverTumbnails = new
        // DownloadServerThumbnails(contactSyncCallback,
        // mApplication.getDatabase(), mApplication.getApplicationContext());

        final ResponseQueue respQueue = ResponseQueue.getInstance();

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // add one contact to the database so the its thumbnail will get
        // downloaded
        final List<Contact> contactList = createContactList(1);

        // add the contact to the database
        mApplication.getDatabase().syncAddContactList(contactList, true, false);

        // start server thumbnails sync processor
        // serverTumbnails.start();

        final QueueManager reqQueue = QueueManager.getInstance();
        List<Request> reqList = reqQueue.getRpgRequests();

        // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

        // processor should finish with a success
        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
    }

    /**
     * Checks the download of one full batch of thumbnails.
     */
    @Suppress
    public void testProcessorRun_oneFullBatchThumbnailsDownload() {

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(
                engineEventCallback);
        // final DownloadServerThumbnails serverTumbnails = new
        // DownloadServerThumbnails(contactSyncCallback,
        // mApplication.getDatabase(), mApplication.getApplicationContext());

        final ResponseQueue respQueue = ResponseQueue.getInstance();

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // get the number of contacts per batch
        final int count = getMaxThumbnailsCountPerBatch();

        // create a contact list of 300 contacts
        final List<Contact> contactList = createContactList(count);

        // add the contact to the database so the their thumbnails will get
        // downloaded
        mApplication.getDatabase().syncAddContactList(contactList, true, false);

        // start server thumbnails sync processor
        // serverTumbnails.start();

        // get the request queue and its request list
        final QueueManager reqQueue = QueueManager.getInstance();
        List<Request> reqList = reqQueue.getRpgRequests();

        // the request queue should have been filled up with the maximum
        // thumbnails count per batch
        assertEquals(count, reqList.size());

        // generate and process responses that contain a thumbnail
        // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

        // processor should finish with a success
        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
    }

    /**
     * Checks the download of 20 full batches of thumbnails.
     */
    @Suppress
    // Breaks tests.
    public void testProcessorRun_twentyFullBatchesThumbnailsDownload() {

        final int maxThumbnailsCountPerBatch = getMaxThumbnailsCountPerBatch();
        final int contactCount = 300;
        final int fullBatchesCount = (contactCount / maxThumbnailsCountPerBatch);

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(
                engineEventCallback);
        // final DownloadServerThumbnails serverTumbnails = new
        // DownloadServerThumbnails(contactSyncCallback,
        // mApplication.getDatabase(), mApplication.getApplicationContext());

        final ResponseQueue respQueue = ResponseQueue.getInstance();

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // generate a contact list so their thumbnails will get downloaded
        final List<Contact> contactList = createContactList(contactCount);

        // get the request queue and its request list
        final QueueManager reqQueue = QueueManager.getInstance();
        List<Request> reqList;

        // add the contact to the database
        ServiceStatus status = mApplication.getDatabase().syncAddContactList(contactList, true,
                false);
        assertEquals(ServiceStatus.SUCCESS, status);

        // start server thumbnails sync processor
        // serverTumbnails.start();

        // the request queue should have been filled up with the maximum
        // thumbnails count per batch
        reqList = reqQueue.getRpgRequests();
        assertEquals(maxThumbnailsCountPerBatch, reqList.size());

        // generate and process responses that contain a thumbnail
        // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

        // this is not the last batch, it shall not have sent a SUCCESS already
        assertEquals(null, contactSyncCallback.mServiceStatus);

        // process the remaining batches, (fullBatchesCount-1) left
        for (int batchIndex = 0; batchIndex < fullBatchesCount - 1; batchIndex++) {

            // continue server thumbnails sync processor
            // serverTumbnails.onTimeoutEvent();

            // the request queue should have been filled up with the maximum
            // thumbnails count per batch
            reqList = reqQueue.getRpgRequests();
            assertEquals(maxThumbnailsCountPerBatch, reqList.size());

            // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

            if (batchIndex < ((contactCount / maxThumbnailsCountPerBatch) - 2)) {
                // this is not the last batch, it shall not have sent a SUCCESS
                // already
                assertEquals(null, contactSyncCallback.mServiceStatus);
            }
        }
        // processor should finish with a success
        assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
    }

    /**
     * Checks the download of 19 full batches of thumbnails plus 3 extra (last
     * batch is not full).
     */
    @Suppress
    public void testProcessorRun_nineteenFullBatchesPlusThreeThumbnailsDownload() {

        // fial int maxThumbnailsCountPerBatch =
        // getMaxThumbnailsCountPerBatch();
        final int contactCount = 285 + 3;
        // final int fullBatchesCount = (contactCount /
        // maxThumbnailsCountPerBatch);

        final IEngineEventCallback engineEventCallback = new HelperClasses.EngineCallbackBase();
        final HelperClasses.DummyContactSyncCallback contactSyncCallback = new HelperClasses.DummyContactSyncCallback(
                engineEventCallback);
        // final DownloadServerThumbnails serverTumbnails = new
        // DownloadServerThumbnails(contactSyncCallback,
        // mApplication.getDatabase(), mApplication.getApplicationContext());

        final ResponseQueue respQueue = ResponseQueue.getInstance();

        // set the connection to be fine
        NetworkAgent.setAgentState(AgentState.CONNECTED);

        // generate a contact list so their thumbnails will get downloaded
        final List<Contact> contactList = createContactList(contactCount);

        // get the request queue and its request list
        final QueueManager reqQueue = QueueManager.getInstance();
        List<Request> reqList;

        // add the contact to the database
        ServiceStatus status = mApplication.getDatabase().syncAddContactList(contactList, true,
                false);
        assertEquals(ServiceStatus.SUCCESS, status);

        // start server thumbnails sync processor
        // serverTumbnails.start();

        // the request queue should have been filled up with the maximum
        // thumbnails count per batch
        reqList = reqQueue.getRpgRequests();
        // assertEquals(maxThumbnailsCountPerBatch, reqList.size());

        // generate and process responses that contain a thumbnail
        // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

        // this is not the last batch, it shall not have sent a SUCCESS already
        assertEquals(null, contactSyncCallback.mServiceStatus);

        // process the remaining batches, (fullBatchesCount-1) left
        // for (int batchIndex = 0; batchIndex < fullBatchesCount - 1;
        // batchIndex++) {

        // continue server thumbnails sync processor
        // serverTumbnails.onTimeoutEvent();

        // the request queue should have been filled up with the maximum
        // thumbnails count per batch
        reqList = reqQueue.getRpgRequests();
        // assertEquals(maxThumbnailsCountPerBatch, reqList.size());

        // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

        // this is not the last batch, it shall not have sent a SUCCESS already
        assertEquals(null, contactSyncCallback.mServiceStatus);
    }

    // the request queue should have been filled up with the remaining
    // thumbnails
    // reqList = reqQueue.getRpgRequests();
    // assertEquals(contactCount - (fullBatchesCount *
    // maxThumbnailsCountPerBatch), reqList.size());

    // generate and process responses that contain a thumbnail
    // sendAndProcessResponses(reqList, respQueue, serverTumbnails);

    // processor should finish with a success
    // assertEquals(ServiceStatus.SUCCESS, contactSyncCallback.mServiceStatus);
    // }

    // //////////////////
    // HELPER METHODS //
    // //////////////////

    /**
     * Generates and processes responses that contain a thumbnail.
     * 
     * @param reqList the request list
     * @param respQueue the response queue
     * @param serverTumbnails the tumbnail processor
     */
    /*
     * private void sendAndProcessResponses(List<Request> reqList, ResponseQueue
     * respQueue, DownloadServerThumbnails serverTumbnails) { final
     * List<BaseDataType> data = new ArrayList<BaseDataType>(); final
     * Enumeration<Request> e = Collections.enumeration(reqList);
     * while(e.hasMoreElements()) { // set the response from the server to be
     * containing an image ExternalResponseObject extResp = new
     * ExternalResponseObject(); extResp.mMimeType = "image/gif"; extResp.mBody
     * = getSampleImage(); data.add(extResp);
     * respQueue.addToResponseQueue(e.nextElement().getRequestId(), data,
     * EngineId.CONTACT_SYNC_ENGINE);
     * serverTumbnails.processCommsResponse(respQueue
     * .getNextResponse(EngineId.CONTACT_SYNC_ENGINE)); } }
     */
    /**
     * Creates a list of contact.
     * 
     * @param count the number of contacts to create
     * @return the generated contact list
     */
    private List<Contact> createContactList(int count) {

        final List<Contact> contactList = new ArrayList<Contact>();
        long contactID = 1;

        for (int contactIndex = 0; contactIndex < count; contactIndex++) {
            final Contact contact = new Contact();
            contact.localContactID = contactID++;
            contact.aboutMe = "aboutMe";
            contact.gender = 0;
            final ContactDetail contactDetail = new ContactDetail();
            contactDetail.key = DetailKeys.PHOTO;
            contactDetail.value = "foo";
            contact.details.add(contactDetail);
            contactList.add(contact);
        }

        return contactList;
    }

    /**
     * Loads a sample image and returns it as a byte array.
     */
    private byte[] getSampleImage() {

        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            is = getInstrumentation().getTargetContext().getResources().openRawResource(
                    R.drawable.contact_avatar_default_blue);
            baos = new ByteArrayOutputStream();

            int b;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            final String msg = "getSampleImage(), error while reading the image: " + e;
            Log.e(LOG_TAG, msg);
            fail(LOG_TAG + " - " + msg);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    final String msg = "getSampleImage(), error while closing the InputStream: "
                            + ioe;
                    Log.e(LOG_TAG, msg);
                    fail(LOG_TAG + " - " + msg);
                }
                is = null;
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ioe) {
                    final String msg = "getSampleImage(), error while closing the ByteArrayOutputStream: "
                            + ioe;
                    Log.e(LOG_TAG, msg);
                    fail(LOG_TAG + " - " + msg);
                }
                baos = null;
            }
        }

        return null;
    }

    /**
     * Gets the thumbnails count per batch.
     * 
     * @return the thumbnails count
     */
    private static int getMaxThumbnailsCountPerBatch() {

        try {
            // Field field =
            // DownloadServerThumbnails.class.getDeclaredField("MAX_THUMBS_FETCHED_PER_PAGE");
            // field.setAccessible(true);
            // return field.getInt(null);
        } catch (Exception e) {
            Log.e(LOG_TAG,
                    "getMaxThumbnailsCountPerBatch(), error retrieving the thumbnails count per batch... => "
                            + e);
        }

        return 0;
    }
}
