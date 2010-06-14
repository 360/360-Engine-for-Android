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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.database.Cursor;

/**
 * Utility class for closing various Java objects without throwing Exceptions
 */
public class CloseUtils {

    /**
     * Close BufferedReader, ignoring any Exceptions thrown.
     * 
     * @param input BufferedReader to close.
     */
    public static void close(BufferedReader input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * Close Cursor, ignoring any Exceptions thrown.
     * 
     * @param input Cursor to close.
     */
    public static void close(Cursor input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    /**
     * Close ByteArrayOutputStream, ignoring any Exceptions thrown.
     * 
     * @param input ByteArrayOutputStream to close.
     */
    public static void close(ByteArrayOutputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    /**
     * Close InputStream handle, ignoring any Exceptions thrown.
     * 
     * @param input InputStream handle to close.
     */
    public static void close(InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    /**
     * Close FileOutputStream handle, ignoring any Exceptions thrown.
     * 
     * @param input FileOutputStream handle to close.
     */
    public static void close(FileOutputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    public static void flush(FileOutputStream input) {
        try {
            if (input != null) {
                input.flush();
            }
        } catch (Exception e) {
            LogUtils.logE("CloseUtils.flush() Exception", e);
        }
    }
    
    /**
     * Close OutputStream handle, ignoring any Exceptions thrown.
     * 
     * @param input OutputStream handle to close.
     */
    public static void close(OutputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (Exception e) {
            // Do nothing
        }
    }
}
