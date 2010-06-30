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

import java.io.File;
import java.net.URL;

import com.vodafone360.people.datatypes.ExternalResponseObject;

/**
 * ContentObject is the class for transferring contents It contains the URL for
 * downloads or a link for uploads, the protocol, the transfer direction and
 * status. After it is created it will be processed by the ContentEngine which
 * will, after the transfer is done, pass it over to the Handler implementing
 * the TranferListener.
 */
public class ContentObject {

    /**
     * id can be used for some unique ids.
     */
    private String mId;

    /**
     * Url for the download.
     */
    private URL mUrl;

    /**
     * additional params for the transfer.
     */
    private String mUrlParams;

    /**
     * File to upload or path for saving downloaded content Depends on
     * TransferDirection.
     */
    private File mPath;

    /**
     * Actual status of the ContentObject.
     */
    private TransferStatus mStatus;

    /**
     * TransferLister to be called after transfer for further processing.
     */
    private TransferListener mTransferListener;

    /**
     * Member holding the TransferDirection.
     */
    private TransferDirection mTransferDirection;

    /**
     * Link to an Object. If downloading thumbnails for contacts this field
     * would contain the Contact object for which the thumbnail should be
     * downloaded.
     */
    private Object mLink;

    /**
     * Member containing the protocol to be used.
     */
    private Protocol mProtocol;

    /**
     * Answer from the Server.
     */
    private ExternalResponseObject mExtResponse;

    /**
     * TransferDirection determines if the ContentObject is to be uploaded or
     * downloaded.
     */
    public enum TransferDirection {
        /** Direction download. **/
        DOWNLOAD,
        /** Direction upload. **/
        UPLOAD
    };

    /**
     * The TransferStatus of the ContentObject.
     */
    public enum TransferStatus {
        /**
         * First status. the object has been created and the transfer has not
         * started yet.
         **/
        INIT,
        /** The transfer has started and is in progress. **/
        TRANSFERRING,
        /** The transfer completed with an error. **/
        ERROR,
        /** The transfer completed with an error. **/
        DONE
    };

    /**
     * Protocol used for the Transfer.
     */
    public enum Protocol {
        /** Lets the Transport decide what to use. **/
        AUTO,
        /** Use the RPG protocol. Best for batch-sue **/
        RPG,
        /** Use HTTP protocol. **/
        HTTP,
        /** Use FTP. Mostly for downloading files like pictures or videos **/
        FTP,
        /** Use Real Time Streaming protocol. Mostly for video streaming **/
        RTSP,
        /** Use the Secure Copy protocol. **/
        SCP
    }

    /**
     * Constructor for the ContentObject. Only sets the parameters.
     * 
     * @param id The unique id, if needed
     * @param link Link to a object
     * @param transferListener TranferListener for this Handler
     * @param transferDirection Upload or download
     * @param protocol Protocol to be used for transfer
     */
    ContentObject(final String id, final Object link, final TransferListener transferListener,
            final TransferDirection transferDirection, final Protocol protocol) {
        this.mId = id;
        this.mLink = link;
        this.mTransferListener = transferListener;
        this.mTransferDirection = transferDirection;
        this.mStatus = TransferStatus.INIT;
        this.mProtocol = protocol;
    }

    /**
     * Getter for the UrlParams.
     * 
     * @return the urlParams
     */
    public final String getUrlParams() {
        return mUrlParams;
    }

    /**
     * Setter for the UrlParamas
     * 
     * @param urlParams the urlParams to set.
     */
    public final void setUrlParams(final String urlParams) {
        this.mUrlParams = urlParams;
    }

    /**
     * Setter for the ExternalResponse.
     * 
     * @param extResponse to be set
     */
    public final void setExtResponse(ExternalResponseObject extResponse) {
        this.mExtResponse = extResponse;
    }

    /**
     * Getter for the ExternalResponseObject.
     * 
     * @return ExternalResponseObject
     */
    public final ExternalResponseObject getExternalResponseObject() {
        return mExtResponse;
    }

    /**
     * Getter for the Protocol.
     * 
     * @return Protocol used for transfer
     */
    public final Protocol getProtocol() {
        return mProtocol;
    }

    /**
     * Getter for the id.
     * 
     * @return id
     */
    public final String getId() {
        return mId;
    }

    /**
     * Getter for the URL.
     * 
     * @return URL for the transfer
     */
    public final URL getUrl() {
        return mUrl;
    }

    /**
     * Setter for the URL.
     * 
     * @param url URL for the transfer
     */
    public final void setUrl(final URL url) {
        this.mUrl = url;
    }

    /**
     * Getter for the path.
     * 
     * @return Instance of File. Points to a place where the downloaded file
     *         should be stored or the to be uploaded file can be found
     */
    public final File getPath() {
        return mPath;
    }

    /**
     * Setter for the path.
     * 
     * @param path Instance of File. Points to a place where the downloaded file
     *            should be stored or the to be uploaded file can be found
     */
    public final void setPath(final File path) {
        this.mPath = path;
    }

    /**
     * Getter for the TransferStatus.
     * 
     * @return The TransferStatus
     */
    public final TransferStatus getStatus() {
        return mStatus;
    }

    /**
     * Setter for the TransferStatus.
     * 
     * @param status The TransferStatus to set for this object
     */
    public final void setTransferStatus(final TransferStatus status) {
        this.mStatus = status;
    }

    /**
     * Getter for the TransferListener.
     * 
     * @return TransferListener for this handler
     */
    public final TransferListener getTransferListener() {
        return mTransferListener;
    }

    /**
     * Getter for the Link.
     * 
     * @return then Link
     */
    public final Object getLink() {
        return mLink;
    }

    /**
     * Getter for the TransferDirection.
     * 
     * @return transferDirection
     */
    public final TransferDirection getDirection() {
        return mTransferDirection;
    }
}
