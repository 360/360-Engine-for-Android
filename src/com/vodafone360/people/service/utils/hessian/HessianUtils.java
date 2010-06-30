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

package com.vodafone360.people.service.utils.hessian;

import java.io.IOException;
import java.io.InputStream;

import com.vodafone360.people.utils.CloseUtils;

public class HessianUtils {
    private static final int MAX_SIZE = 1200;

    public static String getInHessian(InputStream is, boolean isRequest) throws IOException {
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();

        int readBytes = 0;
        int tag = 0;
        try {
            while (((tag = is.read()) != -1) && (readBytes++ < MAX_SIZE)) {
                char hessChar = ((char)tag);

                if (((hessChar >= 'a') && (hessChar <= 'z'))
                        || ((hessChar >= 'A') && (hessChar <= 'Z'))
                        || ((hessChar >= '0') && (hessChar <= '9'))) {
                    sb1.append(" " + hessChar + "  ");
                } else {
                    sb1.append(" .  ");
                }

                if (tag < 10) {
                    sb2.append(" " + tag + "  ");
                } else if (tag < 100) {
                    sb2.append("" + tag + "  ");
                } else {
                    sb2.append("" + tag + " ");
                }
            }
        } finally {
            CloseUtils.close(is);
        }

        char preChar = ((isRequest) ? '>' : '<');

        return "\n  " + preChar + "     " + sb1.toString() + "\n  " + preChar + "     "
                + sb2.toString();
    }
}
