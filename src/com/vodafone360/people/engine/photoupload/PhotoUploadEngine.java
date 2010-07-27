/*
 * CDDL HEADER START
 *
 * @author MyZenPlanet Inc.
 *
 * The contents of this file are subject to
 * the terms of the Common Development and Distribution
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * See the License for the specific language
 * governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code,
 * include this CDDL HEADER in each file and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 *  with the fields enclosed by brackets
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.engine.photoupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import com.vodafone360.people.datatypes.AddContentResult;
import com.vodafone360.people.datatypes.Album;
import com.vodafone360.people.datatypes.AlbumList;
import com.vodafone360.people.datatypes.BaseDataType;
import com.vodafone360.people.datatypes.Content;
import com.vodafone360.people.datatypes.EntityKey;
import com.vodafone360.people.datatypes.GetSharedContentResult;
import com.vodafone360.people.datatypes.GroupItem;
import com.vodafone360.people.datatypes.ListEntityKeyResultShareAlbums;
import com.vodafone360.people.datatypes.ResultAddAlbums;
import com.vodafone360.people.datatypes.ResultGetGroupsForContent;
import com.vodafone360.people.datatypes.ServerError;
import com.vodafone360.people.datatypes.SharePhotoResult;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.service.ServiceUiRequest;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.ResponseQueue.Response;
import com.vodafone360.people.service.io.api.Contents;
import com.vodafone360.people.utils.PhotoUtilsOut;
import com.vodafone360.people.utils.AlbumUtilsIn;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.PhotoUtilsIn;

/**
 * Class for Content Engine. Implements the following functionalities, - Just
 * uploads the content to the server
 */
public class PhotoUploadEngine extends BaseEngine {

    /**
     * If any UI request or response is outstanding, return 0 to indicate that
     * this engine needs to be run at immediate opportunity Otherwise, return -1
     * to indicate that this engine need not be run.
     */
    private long mNextRuntime = -1;

    /**
     * Test.
     */
    private int mPendingRequestId = -1;
    /**
     * Service Status.
     */
    private ServiceStatus mServiceStatus = ServiceStatus.SUCCESS;
    /**
     * LOCAL_LGV for logs.
     */
    private static final boolean LOCAL_LOGV = false;
    /**
     * list we got from UI.
     */
    private List<PhotoUtilsIn> listOfContentsFromUI = null;
    /**
     * Associating file name.
     */
    private String filenameAssociatedWithContentId = null;
    /**
     * Index used in getting next element.
     */
    private static long indexOflistOfContentsFromUI = 0;
    /**
     * Global contentid.
     */
    private Long gContendid = null;

    /**
     * Thumb nail extension.
     */
    private static final String THUMB_EXT = ".jpg";
    /**
     * max quality.
     */
    private static final int MAX_QUALITY = 100;
    /**
     * filename.
     */
    public String filenm = null;
    /**
     * global file name.
     */
    public String gbytesmime = null;
    /**
     * File Chunks size.
     */
    private Long MAX_FILE_SEND_IN_ONE_CHUNK = new Long(614400);

    /*
     * Vector is used to hold the file names as sent by the UI.Filenames and
     * contentid as returned from serverare put inside the object and returned
     * back to UI
     */

    private Vector<String> vectFileNameForContendIdReturnedFromServer;

    /**
     * Constructor.
     *
     * @param eventCallback input paramter.
     */
    public PhotoUploadEngine(final IEngineEventCallback eventCallback) {
        super(eventCallback);
        mEngineId = EngineId.PHOTO_UPLOAD_SHARING;
        vectFileNameForContendIdReturnedFromServer = new Vector<String>();
    }

    /**
     * onCreate function which gets called when engine is created.
     */
    @Override
    public void onCreate() {
        // Do Nothing
    }

    /**
     * onDestroy function which gets called when engine is destroyed.
     */
    @Override
    public void onDestroy() {
        // Do Nothing
    }

    /**
     * getNextRunTime function which gets called by engine manager to determine.
     * next runtime of this engine
     *
     * @return long integer.
     */
    @Override
    public final long getNextRunTime() {
        /**
         * If any UI request or response is outstanding, return 0 to indicate
         * that this engine needs to be run at immediate opportunity Otherwise,
         * return -1 to indicate that this engine need not be run
         */
        if (isUiRequestOutstanding() || isCommsResponseOutstanding()) {
            mNextRuntime = 0;
        } else {
            mNextRuntime = -1; // No need to run
        }
        return mNextRuntime;
    }

    @Override
    public final void run() {

        if (LOCAL_LOGV) {
            LogUtils.logV("ContentsEngine.Run()");
        }
        /**
         * If response is outstanding process the response and return
         */
        if (isCommsResponseOutstanding() && processCommsInQueue()) {
            return;
        }

        if (processTimeout()) {
            return;
        }

        /**
         * If UI request is outstanding process the request and return
         */
        if (isUiRequestOutstanding() && processUiQueue()) {
            return;
        }
    }

    @Override
    protected final void onRequestComplete() {
        if (LOCAL_LOGV) {
            LogUtils.logV("ContentEngine.onRequestComplete()");
        }
    }

    @Override
    protected final void onTimeoutEvent() {
        if (LOCAL_LOGV) {
            LogUtils.logV("ContentEngine.onTimeoutEvent()");
        }
        completeUiRequest(ServiceStatus.ERROR_COMMS_TIMEOUT, null);
    }

    /**
     * This method handles the response Currently when ever file is uploaded
     * content id is returned from server Object is send to UI which contain one
     * contentid and file name.
     *
     * @param dataTypes
     *            input.
     */
    private void handleServerResponse(final List<BaseDataType> dataTypes) {

        if (dataTypes != null) {
            for (BaseDataType mBaseDataType : dataTypes) {
                int type = mBaseDataType.getType();
                if (type == BaseDataType.SERVER_ERROR_DATA_TYPE) {
                    final ServerError srvError = (ServerError) mBaseDataType;
                    mServiceStatus = srvError.toServiceStatus();
                    LogUtils.logE("ContentEngine.handleServerResponse()"
                            + " - Service status: " + mServiceStatus);
                    completeUiRequest(mServiceStatus, null);
                } else if (type == BaseDataType.RESULT_ADD_CONTENT) {

                    final AddContentResult result = (AddContentResult) mBaseDataType;
                    if (result.list != null) {
                        List<PhotoUtilsOut> contentlistbacktoUI = new Vector<PhotoUtilsOut>();
                        // result.list.size() always = 1,bcs only one file is
                        // loaded at time.
                        for (int j = 0; j < result.list.size(); j++) {
                            LogUtils
                                    .logE("ContentEngine.handleServerResponse() "
                                            + "Service status:--mayank"
                                            + result.list.get(j));
                            // Mapping of file name to content id.filenames
                            // stored in vector while uploading,contentid
                            // recieved from server.
                            PhotoUtilsOut objfilenamecontentid = new PhotoUtilsOut();
                            objfilenamecontentid.filename = filenameAssociatedWithContentId;
                            objfilenamecontentid.contentid = result.list.get(j);
                            contentlistbacktoUI.add(objfilenamecontentid);
                        }

                        completeUiRequest(ServiceStatus.SUCCESS,
                                contentlistbacktoUI);

                        /*
                         * After Completion of loading one file,we upload the
                         * second file.
                         */

                        if (listOfContentsFromUI.size() > indexOflistOfContentsFromUI) { // checks
                            // whether
                            // there
                            // are
                            // more
                            // elements

                            loadContentOneAtTime(listOfContentsFromUI
                                    .get((int) indexOflistOfContentsFromUI));
                        }
                    } else if (result.fileuuid != null) {
                        String mfileuuid = result.fileuuid;
                        Content mcontentDataType = new Content();
                        mcontentDataType.fileuuid = mfileuuid;
                        mcontentDataType.bytesmime = gbytesmime;
                        mPendingRequestId = Contents.uploadFile(this,
                                mcontentDataType);
                    }

                } else if (type == BaseDataType.ALBUM_LIST) {

                    final AlbumList result = (AlbumList) mBaseDataType;
                    List<Long> listofalbumid = new Vector<Long>();
                    int j = 0;
                    for (; j < result.albumlist.size(); j++) {
                        if (result.albumlist.get(j).title
                                .equals("360 Photo Shares")) {
                            break;
                        }
                    }
                    // No Call To UI.Engine handles itslef
                    sharePhotoWithAlbum(result.albumlist.get(j).albumid,
                            gContendid);

                } else if (type == BaseDataType.RESULT_SHARE_PHOTO) {
                    final SharePhotoResult result = (SharePhotoResult) mBaseDataType;
                    List<Long> contentidlist = result.listShareContent;
                    Long error = new Long(-1);
                    if (contentidlist.contains(error)) {
                        completeUiRequest(ServiceStatus.ERROR_COMMS,
                                result.listShareContent);
                    } else {
                        completeUiRequest(ServiceStatus.SUCCESS,
                                result.listShareContent);

                    }
                    // deprecated
                } else if (type == BaseDataType.RESULT_ADD_ALBUM) {

                    ResultAddAlbums res = (ResultAddAlbums) mBaseDataType;
                    List<Long> albumidlist = res.listofalbumIds;
                    Long error = new Long(-1);
                    Boolean result = true;
                    for (int j = 0; j < albumidlist.size(); j++) {
                        if (albumidlist.get(0).compareTo(error) == 0) {
                            result = false;
                            break;
                        }
                    }
                    if (result == true) {
                        LogUtils
                                .logE("ContentEngine.handleServerResponse() Service status: AddAlbums list "
                                        + albumidlist.get(0));
                        completeUiRequest(ServiceStatus.SUCCESS, albumidlist);
                    } else {
                        completeUiRequest(ServiceStatus.ERROR_COMMS,
                                albumidlist);
                    }

                } else if (type == BaseDataType.RESULT_LIST_ENTITY_KEY_SHARE_ALBUM) {

                    ListEntityKeyResultShareAlbums obj = (ListEntityKeyResultShareAlbums) mBaseDataType;
                    List<EntityKey> entitylist = obj.listentitykey;
                    Long error = new Long(-1);
                    Boolean result = true;
                    for (int j = 0; j < entitylist.size(); j++) {
                        if (entitylist.get(0) == null) {
                            result = false;
                            break;
                        }
                    }
                    if (result == true && entitylist.size() > 0) {
                        LogUtils
                                .logE("ContentEngine.handleServerResponse() Service status: AddAlbums list ");
                        completeUiRequest(ServiceStatus.SUCCESS, entitylist);
                    } else {
                        completeUiRequest(ServiceStatus.ERROR_COMMS, entitylist);
                    }

                }

            }
        }

    }

    /**
     * Called when a server response is received, processes the response based
     * on the engine state.
     * 
     * @param resp
     *            Response data from server
     */
    @Override
    public final void processCommsResponse(final Response resp) {
        // Complete the request
        if (mPendingRequestId == resp.mReqId) {
            handleServerResponse(resp.mDataTypes);
            // mPendingRequestId = -1; /*chaning mPendingRequestId in
            // handleserverresponse*/
        } else {
            LogUtils
                    .logD("ContentEngine.processCommsResponse() Unexpected response request Id = "
                            + resp.mReqId);
        }
    }

    /**
     * used internally.
     * 
     * @param contentsfromUI
     *            input.
     * @return converted input.
     */

    final Content convertContentUIToContentDataType(
            final PhotoUtilsIn contentsfromUI) {

        Content contentData = new Content();
        File mfile = new File(contentsfromUI.filename);
        if (mfile.length() < MAX_FILE_SEND_IN_ONE_CHUNK) {
            try {

                contentData.bytes = getBytesFromFile(new File(
                        contentsfromUI.filename));
            } catch (IOException e) {
                LogUtils.logV("ContentEngine.getBytes could not open file = "
                        + contentsfromUI.filename);
            }
        } else {
            contentData.bytes = null;
        }

        contentData.filelen = mfile.length();
        contentData.filename = contentsfromUI.filename;
        contentData.remoteid = contentsfromUI.remoteid;
        contentData.maxage = contentsfromUI.maxage;
        contentData.bytesmime = contentsfromUI.bytesmime;
        gbytesmime = contentsfromUI.bytesmime;
        contentData.tagscount = contentsfromUI.tagscount;
        contentData.description = contentsfromUI.description;
        return contentData;
    }

    /**
     * Called when a UI request is ready to be processed. Handles the UI request
     * based on the type.
     *
     * @param requestId
     *            UI request type
     * @param data
     *            Interpretation of this data depends on the request type
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final void processUiRequest(final ServiceUiRequest requestId,
            final Object data) {

        if (LOCAL_LOGV) {
            LogUtils.logV("ContentEngine.processUiRequest() requestId = "
                    + requestId);
        }

        switch (requestId) {

        case UPLOAD_PHOTO: {
            PhotoUtilsIn mcontents = (PhotoUtilsIn) data;
            Content mcontentDataType = convertContentUIToContentDataType(mcontents);
            mPendingRequestId = Contents.uploadFile(this, mcontentDataType);
            break;
        }
        case GET_DEFAULT_ALBUM_360: {
            Long contentid = (Long) data;
            mPendingRequestId = Contents.getDefaultAlbumId360(this);
            break;
        }
        case SHARE_PHOTO_WITH_ALBUM: {
            List<Long> list = (List<Long>) data;
            mPendingRequestId = Contents.sharePhotoWithAlbum(this, list);
            break;
        }

        case ADD_ALBUMS: {
            List<AlbumUtilsIn> list = (List<AlbumUtilsIn>) data;
            List<Album> listalbum = createAlbumListFromUI(list);
            mPendingRequestId = Contents.addAlbums(this, listalbum);
            break;
        }
        case SHARE_ALBUM: {
            List<Long> list = (List<Long>) data;
            mPendingRequestId = Contents.shareAlbums(this, list);
            break;
        }

        default: {
            if (LOCAL_LOGV) {
                LogUtils
                        .logV("ContentEngine.processUiRequest() - Unhandled request");
            }
            break;
        }
        } /* switch (requestId) { */

        if (LOCAL_LOGV) {
            LogUtils
                    .logV("ContentEngine.processUiRequest() return requestId = "
                            + mPendingRequestId);
        }
    }

    /**
     * @fn getBytesFromFile(File fil)
     * @param file
     *            object
     * @return bytes form of file
     */

    public static byte[] getBytesFromFile(final File file) throws IOException {
        InputStream is = new FileInputStream(file); // Get the size of the file
        long length = file.length(); // You cannot create an array using a long
        // type. // It needs to be an int type.
        // // Before converting to an int type,
        // check // to ensure that file is not
        // larger than Integer.MAX_VALUE.

        byte[] bytes = new byte[(int) length]; // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        } // Ensure all the bytes have been read in if (offset < bytes.length) {
        // throw new
        // IOException("Could not completely read file "+file.getName()); }
        // // Close the input stream and return bytes is.close(); return
        // bytes; }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file ");
        }
        is.close();
        return bytes;
    }

    /**
     * loads list of photos given.
     * 
     * @param listcontent
     */
    public void loadPhoto(final List<PhotoUtilsIn> listcontent) {

        /*
         * if (EngineManager.getInstance().getLoginEngine().isLoggedIn() ==
         * false) { LogUtils
         * .logE("ContentsEngine.loadContent() - Error - Not logged In");
         * completeUiRequest(ServiceStatus.ERROR_NOT_LOGGED_IN, null); return; }
         */
        indexOflistOfContentsFromUI = 0; // loads first position
        // Sends files one by one to server.
        // So if there is list of files,we need to send second file in
        // handleserver response when we reciev
        // e response from server for uploading first file
        // Potential error condition:-If first response is not returned to UI,we
        // get second response.
        if (listcontent.size() > 0) {
            loadContentOneAtTime(listcontent
                    .get((int) indexOflistOfContentsFromUI));
            // To be used in handleserver response for sending files one by one.
            listOfContentsFromUI = listcontent;

        } else {
            LogUtils
                    .logE("ContentsEngine.loadContent() - Error - List of photos to be uploaded are empty");
        }
    }

    /**
     * loads one photo at time.
     * 
     * @param content
     *            .
     */
    public void loadContentOneAtTime(PhotoUtilsIn content) {
        // File name associated with one contentid recieved from server,sent
        // back to UI.
        filenameAssociatedWithContentId = content.filename;
        // index of the files uploaded.
        indexOflistOfContentsFromUI++;
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.UPLOAD_PHOTO, content);

    }

    /**
     * cancel request.
     */

    public void cancelRequests() {
        QueueManager queue = QueueManager.getInstance();

        // Remove request from the Request queue
        boolean status = queue.removeRequest(mPendingRequestId);

        if (status) {
            LogUtils.logV("ContentEngine.cancelRequests() Request Id = "
                    + mPendingRequestId + " is cancelled");
        } else {
            LogUtils.logV("ContentEngine.cancelRequests() Request Id = "
                    + mPendingRequestId + " cancel failed");
        }

        if (status) {
            emptyUiRequestQueue();
            mPendingRequestId = -1;
        }
    }

    /**
     * share album with 360album on server.
     *
     * @param contentid
     */
    public void shareContentWith360Album(Long contentid) {

        // First get the album id of default360album of vodafone server
        // Share the content
        if (EngineManager.getInstance().getLoginEngine().isLoggedIn() == false) {
            LogUtils
                    .logE("ContentsEngine.loadContent() - Error - Not logged In");
            completeUiRequest(ServiceStatus.ERROR_NOT_LOGGED_IN, null);
            return;
        }
        gContendid = contentid;
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.GET_DEFAULT_ALBUM_360, contentid);

    }

    /**
     * share album with group.
     * 
     * @param groupid
     *            inoput.
     * @param albumid
     *            input.
     */
    public void shareAlbum(Long groupid, Long albumid) {

        /*
         *
         * if (EngineManager.getInstance().getLoginEngine().isLoggedIn() ==
         * false) { LogUtils
         * .logE("ContentsEngine.loadContent() - Error - Not logged In");
         * completeUiRequest(ServiceStatus.ERROR_NOT_LOGGED_IN, null); return; }
         */

        List<Long> list = new Vector<Long>();
        list.add(groupid);
        list.add(albumid);
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.SHARE_ALBUM, list);
    }

    /**
     * sharing photo with album.
     *
     * @param albumid
     *            input.
     * @param contentid
     *            input.
     */
    public void sharePhotoWithAlbum(Long albumid, Long contentid) {

        /*
         * if (EngineManager.getInstance().getLoginEngine().isLoggedIn() ==
         * false) { LogUtils
         * .logE("ContentsEngine.loadContent() - Error - Not logged In");
         * completeUiRequest(ServiceStatus.ERROR_NOT_LOGGED_IN, null); return; }
         */
        List<Long> list = new Vector<Long>();
        list.add(albumid);
        list.add(contentid);
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.SHARE_PHOTO_WITH_ALBUM, list);
    }

    /**
     * adds album to server.
     * 
     * @param list
     */
    public void addAlbum(List<AlbumUtilsIn> list) {
        /*
         * if (EngineManager.getInstance().getLoginEngine().isLoggedIn() ==
         * false) { LogUtils
         * .logE("ContentsEngine.loadContent() - Error - Not logged In");
         * completeUiRequest(ServiceStatus.ERROR_NOT_LOGGED_IN, null); return; }
         */
        emptyUiRequestQueue();
        addUiRequestToQueue(ServiceUiRequest.ADD_ALBUMS, list);
    }

    /**
     *
     * @param listFromUI
     *            .
     * @return list of album.
     */
    public List<Album> createAlbumListFromUI(List<AlbumUtilsIn> listFromUI) {

        List<Album> list = new Vector<Album>();
        for (int i = 0; i < listFromUI.size(); i++) {
            Album alb = new Album();
            alb.title = listFromUI.get(i).title;
            list.add(alb);
        }
        return list;
    }

} /* Class ContentEngine */