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

/**
 * Representation of an RPG message (RPG header + message body if one exists).
 */
public class RpgMessage {

    private RpgHeader mHeader;

    private byte[] mBody = null;

    private static final int RPG_HEADER_LENGTH = 16;

    /**
     * Constructor creating RPG message with suppled RpgHeader.
     * 
     * @param header RPG header for this message.
     */
    RpgMessage(RpgHeader header) {
        mHeader = header;
    }

    /**
     * Constructor creating RPG message with suppled RpgHeader and message body.
     * 
     * @param header RPG header for this message.
     * @param body byte array containing message body.
     */
    RpgMessage(RpgHeader header, byte[] body) {
        mHeader = header;
        mBody = body;
    }

    /**
     * Return the RPG header for this message.
     * 
     * @return Message's RPG header
     */
    public RpgHeader header() {
        return mHeader;
    }

    /**
     * Return message body as byte array
     * 
     * @return byte[] containing message body.
     */
    public byte[] body() {
        return mBody;
    }

    /**
     * Create RPG message from supplied data
     * 
     * @param payload Message body - Hessian encoded.
     * @param type RPG message type.
     * @param requestId Request ID.
     * @return byte array containing RPG message.
     */
    public static byte[] createRpgMessage(byte[] payload, int type, int requestId) {
        RpgHeader rpgHdr = new RpgHeader();

        rpgHdr.setReqType(type);
        rpgHdr.setReqId(requestId);
        rpgHdr.setPayloadLength(payload.length);
        rpgHdr.setCompression(false);

        int rpgMsgLen = RPG_HEADER_LENGTH + payload.length;
        byte[] rpgMsg = new byte[rpgMsgLen];

        System.arraycopy(rpgHdr.createHeader(), 0, rpgMsg, 0, RPG_HEADER_LENGTH);
        System.arraycopy(payload, 0, rpgMsg, RPG_HEADER_LENGTH, payload.length);

        return rpgMsg;
    }
}
