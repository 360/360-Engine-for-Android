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

package com.vodafone360.people.utils;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Extended BufferedInputStream with a possibility to reset it and reuse it.
 * This way one can avoid GC issues that happen to come with the BufferedInputStream 
 * which allways has to be recreated for new use.
 * @author admin
 *
 */
public class ResetableBufferedInputStream extends BufferedInputStream {

    /**
     * This method takes a new InputStream and sets all counts and markers back to initial values.
     * The buffer will not be touched so it can be reused
     * @param in New InputStream to use 
     */
     public void reset(InputStream in){
        this.in=in;
        count=0;
        marklimit=0;
        markpos = -1;
        pos=0;
    }
     
    /**
    * Constructs a new {@code ResetableBufferedInputStream} on the {@link InputStream}
    * {@code in}. The default buffer size (8 KB) is allocated and all reads
    * can now be filtered through this stream.
    *
    * @param in
    *            the InputStream the buffer reads from.
    */
    public ResetableBufferedInputStream(InputStream in) {
        super(in);
    }

    /**
     * Constructs a new {@code ResetableBufferedInputStream} on the {@link InputStream}
     * {@code in}. The buffer size is specified by the parameter {@code size}
     * and all reads are now filtered through this stream.
     *
     * @param in
     *            the input stream the buffer reads from.
     * @param size
     *            the size of buffer to allocate.
     * @throws IllegalArgumentException
     *             if {@code size < 0}.
     */
     public ResetableBufferedInputStream(InputStream in, int size) {
        super(in,size);
     }

}
