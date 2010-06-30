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

package com.vodafone360.people.engine.content;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.vodafone360.people.database.DatabaseHelper.ThumbnailInfo;
import com.vodafone360.people.engine.EngineManager;
import com.vodafone360.people.engine.content.ContentObject.TransferStatus;
import com.vodafone360.people.utils.LogUtils;
import com.vodafone360.people.utils.ThumbnailUtils;

/**
 * ThumbnailHandler is a handler for contents. The ContentEngine uses handlers
 * to handle transfers of ContentObjects. The handler implements the
 * TransferListener interface and registers itself in the ContentObjects before
 * sending them to the ContentEngine. The ContentEngine calls the
 * TransferListener on a ContentObject after a transfer completes. The
 * ThumbnailHandler handles the fetching of thumbnails for contacts, saving them
 * and refreshing the list.
 */
public class ThumbnailHandler implements TransferListener {

    /**
     * instance of ThumbnailHandler. Used for singleton.
     */
    private static ThumbnailHandler mThumbnailHandlerInstance;
    
    /**
     * Number of thumbnails to fetch in a single RPG request batch.
     */
    private static final int MAX_THUMBS_FETCHED_PER_PAGE = 5;

    /**
     * Queue with contact IDs to fetch thumbnails for. The contact IDs are queued here
     * when the downloadContactThumbnails is called and are then fetched batch
     * for batch. The variable MAX_THUMBS_FETCHED_PER_PAGE defines how much are
     * processed at one time
     */
    private List<Long> mContactsQueue = new LinkedList<Long>();

    /**
     * List with ContentObjects. Every time a ContentObjects is created for
     * downloading it is puted in this list. Every time it is processed, it will
     * be removed. When the list is empty, the next batch of Contacts is
     * processed.
     */
    private List<ContentObject> mContentObjects = new ArrayList<ContentObject>();

    /**
     * Factory method for creating and getting singleton instance of this
     * handler.
     * 
     * @return Instance of Thumbnailhandler
     */
    public static synchronized ThumbnailHandler getInstance() {

        if (mThumbnailHandlerInstance == null) {
            mThumbnailHandlerInstance = new ThumbnailHandler();
        }

        return mThumbnailHandlerInstance;
    }

    /**
     * Private constructor. The singleton method has to be used to obtain an
     * instance.
     */
    private ThumbnailHandler() {

    }

    /**
     * Called by ContentEngine when a Transfer is done. It saves the thumbnail
     * to file, links it to the contact and refreshes the view.
     * 
     * @param content Transfered ContentObject containing the Thumbnail
     */
    @Override
    public final void transferComplete(final ContentObject content) {
        mContentObjects.remove(content);

        Long contactId = (Long) content.getLink();
        try {
            ThumbnailUtils.saveExternalResponseObjectToFile(contactId, content
                    .getExternalResponseObject());
            ContentEngine contentEngine = EngineManager.getInstance().getContentEngine();
            contentEngine.getDatabaseHelper().modifyPictureLoadedFlag(contactId, true);

        } catch (IOException e) {
            LogUtils.logE("ThumbnailHandler.TransferComplete", e);
        }

        if (mContentObjects.size() == 0) {
            downloadThumbnails(MAX_THUMBS_FETCHED_PER_PAGE);
        }

    }

    /**
     * Called when there was an error transfering a ContentObject. It can happen
     * when a timeout occurs, the URL is not valid, the server is not responding
     * and so on.
     * 
     * @param content The failing ContentObject
     * @param exc RuntimeException explaining what happened
     */
    @Override
    public final void transferError(final ContentObject content, final RuntimeException exc) {
        mContentObjects.remove(content);

        if (mContentObjects.size() == 0) {
            downloadThumbnails(MAX_THUMBS_FETCHED_PER_PAGE);
        }

    }

    /**
     * Returns a ThumbanailInfo for a given Contact ID from a list of
     * ThumbnailInfos. When we fetch ThumbnailInfos for a big group of Contacts
     * we will get an List with available ThumbnailInfos. Because not every
     * Contact has a thumbnail mostly this List will be smaller then the given
     * list of contacts. This method is then called to determine matching
     * ThumbnailInfo for a Contact.
     * 
     * @param thumbnailInfoList List with all available Thumbnails
     * @param contactId The contact ID for which the ThumbnailInfo is to be
     *            searched for
     * @return Matching ThumbnailInfo for the given Contact
     */
    private ThumbnailInfo getThumbnailForContact(final List<ThumbnailInfo> thumbnailInfoList,
            final Long contactId) {
        for (ThumbnailInfo thumbnailInfo : thumbnailInfoList) {
            if (thumbnailInfo.localContactId.equals(contactId)) {
                return thumbnailInfo;
            }
        }
        return null;
    }

    /**
     * Puts the contactlist in to a queue and starts downloading the thumbnails
     * for them.
     */
    public final void downloadContactThumbnails() {
        List<Long> contactIdList = new ArrayList<Long>();
        ContentEngine contentEngine = EngineManager.getInstance().getContentEngine();
        contentEngine.getDatabaseHelper().fetchContactIdsWithThumbnails(contactIdList);
        for (Long contactId : contactIdList) {
            if (!mContactsQueue.contains(contactId)) {
                mContactsQueue.add(contactId);
            }
        }
        LogUtils.logI("Downloading " + mContactsQueue.size() + " thumbnails");
        downloadThumbnails(MAX_THUMBS_FETCHED_PER_PAGE);
    }

    /**
     * Download the next bunch of contacts from the queue. The method uses the
     * ContentEngine to download the thumbnails and sets this class as a handler
     * 
     * @param thumbsPerPage Indicates the number of thumbnails to be downloaded
     *            in this page
     */
    private void downloadThumbnails(final int thumbsPerPage) {
        List<Long> contactList = new ArrayList<Long>();
        for (int i = 0; i < thumbsPerPage; i++) {
            if (mContactsQueue.size() == 0) {
                break;
            }
            contactList.add((Long) mContactsQueue.remove(0));
        }

        // nothing to do? exit!
        if (contactList.size() == 0) {
            LogUtils.logI("Thumbnail download finished");
            return;
        }

        // get the contentengine, so we can access the database
        ContentEngine contentEngine = EngineManager.getInstance().getContentEngine();
        // list for holding the fetched ThumbnailURLs
        ArrayList<ThumbnailInfo> thumbnailInfoList = new ArrayList<ThumbnailInfo>();
        // fetches the URLs of all thumbnails that are not downloaded by now
        contentEngine.getDatabaseHelper().fetchThumbnailUrlsForContacts(thumbnailInfoList,
                contactList);

        // This list is needed because of following usecase: We have started the
        // thumbnail sync. 5 thumbnails are requested and we have got the
        // response for 3 of them. At this point, the thumbnail sync starts
        // again(maybe because somethign got changed in the server). This
        // function gets called again. And if we don't use this temporary
        // contentList, those contentObjects for which we haven't got the
        // response yet are also added into the queue of the COntent Engine.
        List<ContentObject> contentList = new ArrayList<ContentObject>();

        // iterate over the given contactList
        for (Long contactId : contactList) {

            // find an ThumbnailUrl for a contact
            ThumbnailInfo thumbnailInfo = getThumbnailForContact(thumbnailInfoList, contactId);
            // not every contact has a thumbnail, so continue in this case
            if (thumbnailInfo == null) {
                continue;
            }
            try {
                // create a ContentObject for downloading the particular
                // Thumbnail...
                ContentObject contentObject = new ContentObject(null, contactId, this,
                        ContentObject.TransferDirection.DOWNLOAD, ContentObject.Protocol.RPG);
                // ... set the right URL and params...
                contentObject.setUrl(new URL(thumbnailInfo.photoServerUrl));
                contentObject.setUrlParams(ThumbnailUtils.REQUEST_THUMBNAIL_URI);
                contentObject.setTransferStatus(TransferStatus.INIT);
                contentList.add(contentObject);
                // ... and put it to the list
                mContentObjects.add(contentObject);

            } catch (MalformedURLException e) {
                LogUtils.logE("ThumbanailHandler.downloadContactThumbnails: "
                        + thumbnailInfo.photoServerUrl + " is not a valid URL");
            }
        }
        // if the list is not empty, let the ContentEngine process them
        if (mContentObjects.size() > 0) {
            contentEngine.processContentObjects(contentList);
        }

    }

    /**
     * Dummy method to replace the dummy processor.
     */
    public void uploadServerThumbnails() {

    }

}
