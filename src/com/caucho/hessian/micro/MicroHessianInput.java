/*
 * Copyright (c) 2001-2006 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson 
 * 
 */

package com.caucho.hessian.micro;

import java.io.*;
import java.util.*;


import com.vodafone360.people.utils.LogUtils;

/**
 * Input stream for Hessian requests, compatible with microedition Java. It only
 * uses classes and types available to J2ME. In particular, it does not have any
 * support for the &lt;double> type.
 * <p>
 * MicroHessianInput does not depend on any classes other than in J2ME, so it
 * can be extracted independently into a smaller package.
 * <p>
 * MicroHessianInput is unbuffered, so any client needs to provide its own
 * buffering.
 * 
 * <pre>
 * InputStream is = ...; // from http connection
 * MicroHessianInput in = new MicroHessianInput(is);
 * String value;
 * in.startReply();         // read reply header
 * value = in.readString(); // read string value
 * in.completeReply();      // read reply footer
 * </pre>
 */
public class MicroHessianInput {
    protected InputStream is;

    /**
     * Creates a new Hessian input stream, initialized with an underlying input
     * stream.
     * 
     * @param is the underlying input stream.
     */
    public MicroHessianInput(InputStream is) {
        init(is);
    }

    /**
     * Creates an uninitialized Hessian input stream.
     */
    public MicroHessianInput() {
    }

    /**
     * Initialize the hessian stream with the underlying input stream.
     */
    public void init(InputStream is) {
        this.is = is;
    }

    /**
     * Starts reading the reply
     * <p>
     * A successful completion will have a single value:
     * 
     * <pre>
     * r x01 x00
     * </pre>
     */
    public void startReply() throws IOException {
        int tag = is.read();

        if (tag != 'r')
            throw protocolException("expected hessian reply");

        // remove some bits from the input stream
        is.read();
        is.read();

    }

    /**
     * Completes reading the call
     * <p>
     * A successful completion will have a single value:
     * 
     * <pre>
     * z
     * </pre>
     */
    public void completeReply() throws IOException {
        int tag = is.read();

        if (tag != 'z')
            throw protocolException("expected end of reply");
    }

    /**
     * Reads a boolean
     * 
     * <pre>
     * T
     * F
     * </pre>
     */
    public boolean readBoolean() throws IOException {
        int tag = is.read();

        switch (tag) {
            case 'T':
                return true;
            case 'F':
                return false;
            default:
                throw expect("boolean", tag);
        }
    }

    /**
     * Reads an integer
     * 
     * <pre>
     * I b32 b24 b16 b8
     * </pre>
     */
    public int readInt() throws IOException {
        int tag = is.read();
        return readInt(tag);
    }
    
    public int readInt(int tag) throws IOException {
        if (tag != 'I')
            throw expect("integer", tag);

        int b32 = is.read();
        int b24 = is.read();
        int b16 = is.read();
        int b8 = is.read();

        return (b32 << 24) + (b24 << 16) + (b16 << 8) + b8;
    }
    

    /**
     * Reads a long
     * 
     * <pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    public long readLong() throws IOException {
        int tag = is.read();
        return readLong(tag);
    }
    
    private long readLong(int tag) throws IOException {
        if (tag != 'L')
            throw protocolException("expected long");

        long b64 = is.read();
        long b56 = is.read();
        long b48 = is.read();
        long b40 = is.read();
        long b32 = is.read();
        long b24 = is.read();
        long b16 = is.read();
        long b8 = is.read();

        return ((b64 << 56) + (b56 << 48) + (b48 << 40) + (b40 << 32) + (b32 << 24) + (b24 << 16)
                + (b16 << 8) + b8);
    }

    /**
     * Reads a date.
     * 
     * <pre>
     * T b64 b56 b48 b40 b32 b24 b16 b8
     * </pre>
     */
    public long readUTCDate() throws IOException {
        int tag = is.read();

        if (tag != 'd')
            throw protocolException("expected date");

        long b64 = is.read();
        long b56 = is.read();
        long b48 = is.read();
        long b40 = is.read();
        long b32 = is.read();
        long b24 = is.read();
        long b16 = is.read();
        long b8 = is.read();

        return ((b64 << 56) + (b56 << 48) + (b48 << 40) + (b40 << 32) + (b32 << 24) + (b24 << 16)
                + (b16 << 8) + b8);
    }

    /**
     * Reads a byte array
     * 
     * @return byte[] array extracted from Hessian stream, NULL if 'N' specified in data.
     * @throws IOException.
     */
    public byte[] readBytes() throws IOException {
        int tag = is.read();
        return readBytes(tag);
    }
    
    private byte[] readBytes(int tag) throws IOException {
        if (tag == 'N')
            return null;

        if (tag != 'B')
            throw expect("bytes", tag);

        int b16 = is.read();
        int b8 = is.read();

        int len = (b16 << 8) + b8;

        byte[] bytes = new byte[len];
        is.read(bytes);
        return bytes;
    }

    /**
     * Reads an arbitrary object the input stream.
     */
    public Object readObject(Class<?> expectedClass) throws IOException {
        int tag = is.read();

        switch (tag) {
            case 'N':
                return null;

            case 'T':
                return true;

            case 'F':
                return false;

            case 'I': {
                int b32 = is.read();
                int b24 = is.read();
                int b16 = is.read();
                int b8 = is.read();

                return ((b32 << 24) + (b24 << 16) + (b16 << 8) + b8);
            }

            case 'L': {
                long b64 = is.read();
                long b56 = is.read();
                long b48 = is.read();
                long b40 = is.read();
                long b32 = is.read();
                long b24 = is.read();
                long b16 = is.read();
                long b8 = is.read();

                return ((b64 << 56) + (b56 << 48) + (b48 << 40) + (b40 << 32) + (b32 << 24)
                        + (b24 << 16) + (b16 << 8) + b8);
            }

            case 'd': {
                long b64 = is.read();
                long b56 = is.read();
                long b48 = is.read();
                long b40 = is.read();
                long b32 = is.read();
                long b24 = is.read();
                long b16 = is.read();
                long b8 = is.read();

                return new Date((b64 << 56) + (b56 << 48) + (b48 << 40) + (b40 << 32) + (b32 << 24)
                        + (b24 << 16) + (b16 << 8) + b8);
            }

            case 'S':
            case 'X': {
                int b16 = is.read();
                int b8 = is.read();

                int len = (b16 << 8) + b8;

                return readStringImpl(len);
            }

            case 'B': {
                if (tag != 'B')
                    throw expect("bytes", tag);

                int b16 = is.read();
                int b8 = is.read();

                int len = (b16 << 8) + b8;

                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                for (int i = 0; i < len; i++)
                    bos.write(is.read());

                return bos.toByteArray();
            }
            default:
                throw new IOException("unknown code:" + (char)tag);
        }
    }

    public Vector<Object> readVector() throws IOException {
        int tag = is.read();
        return readVector(tag);
    }
    
    private Vector<Object> readVector(int tag) throws IOException {
        if (tag == 'N')
            return null;

        if (tag != 'V')
            throw expect("vector", tag);
        
        Vector<Object> v = new Vector<Object>();
        Object o = decodeTag();

        if (o instanceof End)
            return v;

        if (o instanceof Type)
            o = decodeTag();

        if (o instanceof End)
            return v;

        int len = 0;
        if (o instanceof Integer) {
            len = ((Integer)o);
            o = decodeTag();
        }

        for (int i = 0; i < len; i++) {
            v.addElement(o);
            o = decodeTag();
        }
        return v;
    }
    
    public Fault readFault() throws IOException {
        decodeTag();
        int tag = is.read();
        if (tag == 'S') {
            return new Fault(readString(tag));
        }
        return null;
    }
    
    public Object decodeTag() throws IOException {
        int tag = is.read();
        // HessianUtils.printTagValue(tag);
        return decodeType(tag);
    }
    
    public Object decodeType(int tag) throws IOException {        
        // LogUtils.logD("HessianDecoder.decodeType() tag["+tag+"]");
        switch (tag) {
            case 't': // tag
                is.read();
                is.read();
                Type type = new Type();
                return type;
            case 'l': // length
                int i = 0;

                i += (is.read() << 24);
                i += (is.read() << 16);
                i += (is.read() << 8);
                i += is.read();

                Integer len = i;
                return len;
            case 'z': // end
                End end = new End();
                return end;
            case 'N': // null
                return null;
            case 'r':
                // reply startReply should have retrieved this?
                return null;
            case 'M':
                return readHashMap(tag);
            case 'V': // array/Vector
                return readVector(tag);
            case 'T': // boolean true
                return true;
            case 'F': // boolean false
                return false;
            case 'I': // integer
                return readInt(tag);
            case 'L': // read long
                return readLong(tag);
            case 'd': // UTC date
                return null;
            case 'S': // String
                return readString(tag);
            case 'B': // read byte array
                return readBytes(tag);
            case 'f':
                return readFault();
            default:
                LogUtils.logE("HessianDecoder.decodeType() Unknown type");
                return null;
        }
    }
    
    /**
     * Reads a string
     * 
     * <pre>
     * S b16 b8 string value
     * </pre>
     */
    public String readString() throws IOException {
        int tag = is.read();
        return readString(tag);
    }
    
    private String readString(int tag) throws IOException {
        if (tag == 'N')
            return null;

        if (tag != 'S')
            throw expect("string", tag);

        int b16 = is.read();
        int b8 = is.read();

        int len = (b16 << 8) + b8;

        return readStringImpl(len);
    }
    
    /**
     * Reads a string from the underlying stream.
     */
    private String readStringImpl(int length) throws IOException {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < length; i++) {
            int ch = is.read();

            if (ch < 0x80)
                sb.append((char)ch);
            else if ((ch & 0xe0) == 0xc0) {
                int ch1 = is.read();
                int v = ((ch & 0x1f) << 6) + (ch1 & 0x3f);

                sb.append((char)v);
            } else if ((ch & 0xf0) == 0xe0) {
                int ch1 = is.read();
                int ch2 = is.read();
                int v = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);

                sb.append((char)v);
            } else if ((ch & 0xff) >= 0xf0 && (ch & 0xff) <= 0xf4) { // UTF-4
                final byte[] b = new byte[4];
                b[0] = (byte)ch;
                b[1] = (byte)is.read();
                b[2] = (byte)is.read();
                b[3] = (byte)is.read();
                sb.append(new String(b, "utf-8"));
                i++;
            } else
                throw new IOException("bad utf-8 encoding");
        }

        return sb.toString();
    }
    
    public Hashtable<String, Object> readHashMap() throws IOException {
        // read map type
        int tag = is.read();
        return readHashMap(tag);
    }
    
    public Hashtable<String, Object> readHashMap(int tag) throws IOException {
        // read map type
        
        if (tag == 'N')
            return null;

        if (tag != 'M')
            throw expect("map", tag);
        
        Hashtable<String, Object> ht = new Hashtable<String, Object>();
        Object obj = decodeTag();
        if (obj instanceof Type) {
            // get following object
            obj = decodeTag();
        }

        Object obj1 = null;

        while (obj != null && !(obj instanceof End)) // 'z' = list-end
        {
            obj1 = decodeTag();
            ht.put(obj.toString(), obj1);
            obj = decodeTag();
        }
        return ht;
    }

    protected IOException expect(String expect, int ch) {
        if (ch < 0)
            return protocolException("expected " + expect + " at end of file");
        else
            return protocolException("expected " + expect + " at " + (char)ch);
    }

    protected IOException protocolException(String message) {
        return new IOException(message);
    }
    
    /**
     * Place-holder class for End tag 'z'
     */
    private static class End {
    }

    /**
     * Place-holder class for Type tag 't'
     */
    private static class Type {
    }

    /**
     * Class holding error string returned during Hessian decoding
     */
    public static class Fault {
        private String mErrString = null;

        private Fault(String eString) {
            mErrString = eString;
        }

        public String errString() {
            return mErrString;
        }
    }
}
