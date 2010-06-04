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

package com.vodafone360.people.service.io.rpg;

import com.vodafone360.people.utils.LogUtils;

/**
 * Representation of contents of RPG header (as defined in Now+ API -
 * Communication Protocol & Events) The header contains: the message type, the
 * message request id the message payload length whether the payload is
 * compressed
 */
public class RpgHeader {
    /** RPG header length. */
    public final static int HEADER_LENGTH = 16;

    /** RPG header delimiter byte. */
    public static final int DELIMITER_BYTE = 0xFF;

    private final static byte PADDING_BYTE = (byte)0xFF;

    private int mReqType;

    private int mReqId;

    private int mPayloadLength;

    private boolean mCompression = false;

    private byte[] mRpqHeader = new byte[HEADER_LENGTH];

    /**
     * Default constructor.
     */
    public RpgHeader() {
    }

    /**
     * Extract header info from byte array into RpgHeader structure
     * 
     * @param headerdata byte array containing header info
     * @return true if we have a valid header, false otherwise.
     */
    protected boolean extractHeaderInfo(byte[] headerdata) {

        if (headerdata[0] != PADDING_BYTE && headerdata[1] != PADDING_BYTE)
            return false;

        // req type
        mReqType = (int)headerdata[2];

        if (!validateHeader(mReqType))
            return false;

        mReqId = RpgHelper.signedBytesToInt(headerdata[3], headerdata[4], headerdata[5],
                headerdata[6]);

        mPayloadLength = RpgHelper.signedBytesToInt(headerdata[11], headerdata[12], headerdata[13],
                headerdata[14]);

        if (headerdata[15] == 1)
            mCompression = true;

        return true;
    }

    /**
     * Validate RPG header based on whether it specifies a valid message type.
     * 
     * @param type RPG message type
     * @return true if a valid and supported type is supplied, false otherwise.
     */
    private boolean validateHeader(int type) {
        boolean ret = false;
        switch (type) {
            case RpgMessageTypes.RPG_PUSH_MSG:
            case RpgMessageTypes.RPG_INT_RESP:
            case RpgMessageTypes.RPG_EXT_RESP:

            case RpgMessageTypes.RPG_CLOSE_CONV:
            case RpgMessageTypes.RPG_CREATE_CONV:
                // case RpgMessageTypes.RPG_EXT_REQ:
            case RpgMessageTypes.RPG_FETCH_CONTACTS:
                // case RpgMessageTypes.RPG_GET_PRESENCE:
            case RpgMessageTypes.RPG_TCP_HEARTBEAT:
                // case RpgMessageTypes.RPG_INT_REQ:
            case RpgMessageTypes.RPG_POLL_MESSAGE:
            case RpgMessageTypes.RPG_PRESENCE_RESPONSE:
                // case RpgMessageTypes.RPG_SEND_IM:
                // case RpgMessageTypes.RPG_SET_AVAILABILITY:

                ret = true;
                break;
            default:
                LogUtils.logE("POLLTIMETEST Header: repnseType is not recognizable:" + mReqType);
        }
        return ret;
    }

    /**
     * Get RPG request type from header
     * 
     * @return RPG request type
     */
    public final int reqType() {
        return mReqType;
    }

    /**
     * Get request ID from header
     * 
     * @return request ID
     */
    public final int reqId() {
        return mReqId;
    }

    /**
     * Get RPG message body data length
     * 
     * @return Size of RPG message
     */
    protected final int payloadLength() {
        return mPayloadLength;
    }

    /**
     * Get RPG request type from header
     * 
     * @return true if RPG response is compressed false otherwise
     */
    public final boolean compression() {
        return mCompression;
    }

    /**
     * Set RPG request type
     * 
     * @param reqType RPG request type
     */
    public void setReqType(int reqType) {
        mReqType = reqType;
    }

    /**
     * Add request id to RPG header information
     * 
     * @param reqId requrest id
     */
    public void setReqId(int reqId) {
        mReqId = reqId;
    }

    /**
     * Add message data length to RPG header information
     * 
     * @param payloadLength length of message data
     */
    public void setPayloadLength(int payloadLength) {
        mPayloadLength = payloadLength;
    }

    /**
     * Set whether compression is used
     * 
     * @param compression
     */
    public void setCompression(boolean compression) {
        mCompression = compression;
    }

    /**
     * Generate RPG header as byte array
     * 
     * @return byte array containing RPG header
     */
    public byte[] createHeader() {
        mRpqHeader[0] = PADDING_BYTE;
        mRpqHeader[1] = PADDING_BYTE;

        mRpqHeader[2] = (byte)mReqType;

        byte[] reqIdBytes = RpgHelper.intToSignedBytes(mReqId);
        mRpqHeader[3] = reqIdBytes[0];
        mRpqHeader[4] = reqIdBytes[1];
        mRpqHeader[5] = reqIdBytes[2];
        mRpqHeader[6] = reqIdBytes[3];

        byte[] reqLengthBytes = RpgHelper.intToSignedBytes(mPayloadLength);
        mRpqHeader[11] = reqLengthBytes[0];
        mRpqHeader[12] = reqLengthBytes[1];
        mRpqHeader[13] = reqLengthBytes[2];
        mRpqHeader[14] = reqLengthBytes[3];

        mRpqHeader[15] = 0;

        return mRpqHeader;
    }
}
