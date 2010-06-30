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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.caucho.hessian.micro.MicroHessianOutput;
import com.vodafone360.people.utils.CloseUtils;

/**
 * Produce Hessian encoded byte array using Caucho Hessian implementation
 */
public class HessianEncoder {

    /**
     * Create Hessian encoded byte array from Hashtable
     * 
     * @param ht Hashtable to encode
     * @return encoded byte array
     * @throws IOException
     */
    public static byte[] createHessianByteArray(String function, Hashtable<String, Object> ht)
            throws IOException {

        // write our hash table to Hessian format
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MicroHessianOutput mho = new MicroHessianOutput(bos);

        mho.startCall(function);
        writeHashtable(ht, mho);
        mho.completeCall();
        CloseUtils.close(bos);
        return bos.toByteArray();
    }

    private static void writeHashtable(Hashtable<String, Object> ht, MicroHessianOutput mho)
            throws IOException {
        mho.writeMapBegin(null);
        Enumeration<String> e = ht.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object value = ht.get(key);
            mho.writeObject(key);
            if (value instanceof byte[]) {
                mho.writeBytes((byte[])value);
            } else if (value instanceof Hashtable<?, ?>) {
                @SuppressWarnings("unchecked")
                Hashtable<String, Object> table = (Hashtable<String, Object>)value;
                writeHashtable(table, mho);
            } else if (value instanceof ArrayList<?>) {
                @SuppressWarnings("unchecked")
                ArrayList<String> list = (ArrayList<String>)value;
                writeStringArrayList(list, mho);
            } else {
                mho.writeObject(value);
            }
        }
        mho.writeMapEnd();
    }

    private static void writeStringArrayList(ArrayList<String> list, MicroHessianOutput mho)
            throws IOException {
        mho.writeListBegin(list.size(), null);
        for (String to : list) {
            mho.writeString(to);
        }
        mho.writeListEnd();
    }
}
