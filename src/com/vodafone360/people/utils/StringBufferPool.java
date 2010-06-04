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

import java.util.ArrayList;

/**
 * This is a static utility class representing the StringBuffer,
 * pool. StringBuffer objects can be retrieved from the pool
 * and released back to the pool when necessary. 
 * This class must to be used anywhere where String
 * concatenation operations are intensively performed
 * to avoid too many objects being generated and GC
 * slowing down the performance.
 */
public class StringBufferPool {
    /**
     * Synchronization.
     */
    private static Object mutex = new Object();
    /**
     * The list of free StringBuffers. 
     */
    private static ArrayList<StringBuffer> free = new ArrayList<StringBuffer>(5);
    
    /**
     * The constructor. 
     */
    private StringBufferPool() {}
    
    
    private static StringBuffer allocStringBuffer(String str) {
        return new StringBuffer(str);
    }
    
    private static StringBuffer allocStringBuffer() {
        return new StringBuffer();
    }
    
    private static StringBuffer getFreeStringBuffer() {
        StringBuffer retValue = null;
        if (!free.isEmpty()) {
            retValue = (StringBuffer)free.get(free.size() - 1);
            free.remove(free.size() - 1);
        }
        return retValue;
    }
    
    /**
     * This method returns a StringBuffer object from the pool...
     * @param str String - the string to initialize the StringBuffer with.
     * @return StringBuffer object from the pool
     */
    public static StringBuffer getStringBuffer(String str) {
        synchronized (mutex) {
            StringBuffer sb = getFreeStringBuffer();
            if (sb == null) 
                sb = allocStringBuffer(str);
            else 
                sb.append(str);
            return sb;
        } 
    }
    
    /**
     * This method returns a StringBuffer object from the pool...
     * @return StringBuffer object from the pool.
     */
    public static StringBuffer getStringBuffer() {
        synchronized (mutex) {
            StringBuffer sb = getFreeStringBuffer();
            if (sb == null) sb = allocStringBuffer();
            return sb;
        } 
    }   
    
    /**
     * This method calls releases the provided StringBuffer to the pool...
     * @param sb StringBuffer - the StringBuffer to be released.
     * @return StringBuffer object from the pool.
     */
    public static void releaseStringBuffer(StringBuffer sb) {
        if (sb != null) {
            sb.setLength(0);
             synchronized (mutex) {
                free.add(sb);
            }
        }
    }
    
    /**
     * This method calls toString() method on the provided
     * StringBuffer object and releases it to the pool...
     * @param sb StringBuffer - the StringBuffer to be released.
     * @return String the toString() method result called on
     * the provided StringBuffer.
     */
    public static String toStringThenRelease(StringBuffer sb) {
        String str = sb.toString();
        releaseStringBuffer(sb);
        return str;
    }
}
