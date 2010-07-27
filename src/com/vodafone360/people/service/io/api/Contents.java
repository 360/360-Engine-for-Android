/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject to the terms of.
 * the Common Development and Distribution.
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * See the License for the specific language
 *  governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this
 *  CDDL HEADER in each file and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 * with the fields enclosed by brackets
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

package com.vodafone360.people.service.io.api;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.vodafone360.people.datatypes.Album;
import com.vodafone360.people.datatypes.Content;
import com.vodafone360.people.datatypes.EntityKey;
import com.vodafone360.people.engine.BaseEngine;
import com.vodafone360.people.engine.login.LoginEngine;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.utils.LogUtils;

/**
 * class is used by photouploadengine.
 * to put request in queue.
 * @author mayank
 *
 */
public class Contents {

    /**
     * Function to add content.
     */
    private final static String FUNCTION_ADD_CONTENT = "content/addcontent";
    /**
     * Function to get albums.
     */
    private final static String FUNCTION_GET_ALBUMS = "content/getalbums";
    /**
     * Function for sharing photo with album.
     */
    private final static String FUNCTION_SHARE_PHOTO_WITH_ALBUM = "content/addcontenttoalbum";
    /**
     * Function to get shared content.
     */
    private final static String FUNCTION_GET_SHARE_CONTENT = "content/getsharedcontent";
    /**
     * Function for updating content.
     */
    private final static String FUNCTION_UPDATE_CONTENT = "content/updatecontent";
    /**
     * Function to upload file in chunks.
     */
    private final static String FUNCTION_UPLOAD_FILE = "upload/uploadstart";
    /**
     * Function to add albums.
     */
    private final static String FUNCTION_ADD_ALBUM = "content/addalbums";
    /**
     * Functions to share albumwith group.
     */
    private final static String FUNCTION_SHARE_ALBUM = "share/sharewithgroup";
    /**
     * system.
     */
    static final String SYSTEM = "system";
    /**
     * quickpost.
     */
    static final String QUICKPOST = "quickpost";

    /**
     * Implementation of loadContent API. Parameters are; [auth], list of
     * contentipdatatype
     *
     * @param engine  Handle to Base engine.
     * @param content Contents.
     * @return request id generated for this request.
     */
    public static int uploadFile(
            final BaseEngine engine, final Content content) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Content.getsession() Invalid session, return -1");
            return -1;
        }
        if (content == null) {
            LogUtils.logE("Content.contentlist cannot be NULL");
            return -1;
        }
        Request request = new Request(FUNCTION_ADD_CONTENT,
                Request.Type.UPLOAD_PHOTO, engine.engineId(), false, 90000000);

        List<Content> listOfContent = new Vector<Content>();
        listOfContent.add(content);
        request.addData("contentlist", ApiUtils
                .createVectorOfContentIPDataType(listOfContent));
        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        request.mfileSize = content.filelen;
        request.mfileName = content.filename;
        queue.fireQueueStateChanged();
        return requestId;

    }

    /**
     * Implementation of loadContent API. Parameters are; [auth], list of
     * contentipdatatype
     *
     * @param engine
     *            Handle to Base engine.
     * @param content Contents.
     * @return request id generated for this request.
     */
    public static int loadContents(
            final BaseEngine engine, final Content content) {

        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Content.getsession() Invalid session, return -1");
            return -1;
        }
        if (content == null) {
            LogUtils.logE("Content.contentlist cannot be NULL");
            return -1;
        }
        Request request = new Request(FUNCTION_ADD_CONTENT,
                Request.Type.UPLOAD_PHOTO, engine.engineId(), false, 90000000);

        List<Content> listOfContent = new Vector<Content>();
        listOfContent.add(content);
        request.addData("contentlist", ApiUtils
                .createVectorOfContentIPDataType(listOfContent));
        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;

    }

    /**
     * Implementation of getDefaultAlbumId360 API. Parameters are; [auth],
     *
     * @param engine
     *            Handle to Base engine.
     * @return request id generated for this request.
     */
    public static int getDefaultAlbumId360(
            final BaseEngine engine) {
        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Content.getsession() Invalid session, return -1");
            return -1;
        }
        Request request = new Request(FUNCTION_GET_ALBUMS,
                Request.Type.GET_DEFAULT_ALBUM360, engine.engineId(), false,
                160000);

        Hashtable hashtable = new Hashtable();
        List<String> listFilter = new Vector<String>();
        listFilter.add(QUICKPOST);
        hashtable.put(SYSTEM, listFilter);
        request.addData("filterlist", hashtable);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of shareContent API. Parameters are; [auth],list of
     * albumid,contentid
     *
     * @param engine
     *            Handle to Base engine.
     * @param list list.
     * @return request id generated for this request.
     */
    public static int sharePhotoWithAlbum(
            final BaseEngine engine, final List<Long> list) {

        if (LoginEngine.getSession() == null) {
            LogUtils.logE("Content.getsession() Invalid session, return -1");
            return -1;
        }
        Request request = new Request(FUNCTION_SHARE_PHOTO_WITH_ALBUM,
                Request.Type.SHARE_PHOTO_WITH_ALBUM, engine.engineId(), false,
                160000);

        // TODO:Must be extended for multiple contents
        List<Long> listofContents = new Vector<Long>();
        listofContents.add(list.get(1));

        request.addData("contentidlist", new Vector<Object>(listofContents));
        request.addData("albumid", list.get(0));

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of getshareContent API. Parameters are; [auth],
     *
     * @param engine
     *            Handle to Base engine
     * @param listalbum list of album.
     * @return request id generated for this request.
     */
    public static int addAlbums(
            final BaseEngine engine, final List<Album> listalbum) {
        Request request = new Request(FUNCTION_ADD_ALBUM,
                Request.Type.ADD_ALBUMS, engine.engineId(), false, 90000000);

        request.addData("albumlist", ApiUtils
                .createVectorOfAlbumType(listalbum));
        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Implementation of sharealbum API. Parameters are; [auth],
     *
     * @param engine
     *            Handle to Base engine
     * @param listalbum list of album.
     * @return request id generated for this request.
     */
    public static int shareAlbums(
            final BaseEngine engine, final List<Long> listalbum) {
        Request request = new Request(FUNCTION_SHARE_ALBUM,
                Request.Type.SHARE_ALBUMS, engine.engineId(), false, 90000000);

        Long groupid = listalbum.get(0);
        request.addData("groupid", groupid);
        Long albumid = listalbum.get(1);
        EntityKey ent = new EntityKey();
        ent.entityid = albumid;
        ent.entitytype = "ALBUM";
        List<EntityKey> listentitykey = new Vector<EntityKey>();
        listentitykey.add(ent);
        request.addData("entitykeylist", ApiUtils
                .createVectorOfEntity(listentitykey));
        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

}