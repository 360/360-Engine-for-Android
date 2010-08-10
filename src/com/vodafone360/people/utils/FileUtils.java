/*
 * CDDL HEADER START
 *
 *@author MyZenPlanet Inc.
 *
 * The contents of this file are subject
 *  to the terms of the Common Development and Distribution
 * License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at
 * src/com/vodafone/people/VODAFONE.LICENSE.txt or
 * See the License for the specific language
 * governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL
 *  HEADER in each file and include the License
 * file at src/com/vodafone/people/VODAFONE.LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER,
 * with the fields enclosed by brackets
 * "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of
 * copyright owner]
 *
 * CDDL HEADER END
 *
 * Copyright 2009 Vodafone Sales & Services Ltd.  All rights reserved.
 * Use is subject to license terms.
 */

/**
 * File Utilities
 */


package com.vodafone360.people.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
	
	
	 /**
     * @fn getBytesFromFile(File file)
     * @param file  object
     * @return bytes form of file
     */

    public static byte[] getBytesFromFile(final File file) throws IOException {
        InputStream is = new FileInputStream(file); // Get the size of the file
        long length = file.length(); // You cannot create an array using a long

        byte[] bytes = new byte[(int) length]; // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        } 
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file ");
        }
        is.close();
        return bytes;
    }

}
