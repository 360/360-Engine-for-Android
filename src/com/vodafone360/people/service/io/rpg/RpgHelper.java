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

import java.util.List;

import java.io.InputStream;
import java.io.IOException;

/**
 * RPG helper functions, Splitting of RPG response data into individual RPG
 * responses, int <--> byte conversion.
 */
public class RpgHelper {

    private static final int RPG_HEADER_LENGTH = RpgHeader.HEADER_LENGTH;

    /**
     * Splits an integer into a signed byte-array.
     * 
     * @param integer The int to convert into bytes.
     * @return The byte-array containing the bytes of the int.
     */
    protected static final byte[] intToSignedBytes(int integer) {
        return new byte[] {
                (byte)(integer >> 24), (byte)(integer >> 16 & 0xFF), (byte)(integer >> 8 & 0xFF),
                (byte)(integer & 0xFF)
        };
    }

    /**
     * Generate int value from 4 signed bytes
     * 
     * @param byte1 1st (most significant) byte.
     * @param byte2 2nd byte.
     * @param byte3 3rd byte.
     * @param byte4 4th byte.
     * @return integer value generated from input bytes
     */
    protected static final int signedBytesToInt(byte byte1, byte byte2, byte byte3, byte byte4) {
        int result = 0;

        result += (byte1 & 0x000000FF) << 24;
        result += (byte2 & 0x000000FF) << 16;
        result += (byte3 & 0x000000FF) << 8;
        result += (byte4 & 0x000000FF);

        return result;
    }

    /**
     * Split single RPG response into individual RPG request responses
     * 
     * @param inputStream byte array containing RPG response
     * @param msgs array of RPG messages (consisting of RPG header and Hessian
     *            encoded message body)
     */
    public static void splitRpgResponse(InputStream inputStream, List<RpgMessage> msgs)
            throws IOException {
        int actual = 0;
        byte[] header = new byte[RPG_HEADER_LENGTH];

        while (actual != -1) {
            actual = 0;
            int offset = 0;
            while (actual != -1 && (offset != RPG_HEADER_LENGTH)) {
                // extract header info
                actual = inputStream.read(header, offset, RPG_HEADER_LENGTH - offset);
                if (actual > 0)
                    offset += actual;
            }
            if (offset != RPG_HEADER_LENGTH) {
                break;
            }
            RpgHeader hdr = new RpgHeader();
            if (!hdr.extractHeaderInfo(header)) {
                break;
            }
            int payloadLength = hdr.payloadLength();
            if (payloadLength != 0) {
                byte[] body = new byte[payloadLength];
                actual = 0;
                int pos = 0;
                while (actual != -1 && (pos != payloadLength)) {
                    actual = inputStream.read(body, pos, payloadLength - pos);
                    if (actual > 0)
                        pos += actual;
                }
                if (pos != payloadLength) {
                    break;
                }
                msgs.add(new RpgMessage(hdr, body));
            } else {
                // no message body just a header
                msgs.add(new RpgMessage(hdr));
            }
        }
    }
}
