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

package com.vodafone360.people.tests.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;
import android.util.Log;

import com.vodafone360.people.R;
import com.vodafone360.people.datatypes.ExternalResponseObject;
import com.vodafone360.people.tests.AllTests;
import com.vodafone360.people.utils.ThumbnailUtils;

/***
 * JUnit tests for the ThumbnailUtils class.
 */
public class ThumbnailUtilsTest extends AndroidTestCase {

    /** Full device thumbnail path. **/
    private static final String FULL_PATH =
        "/data/data/com.vodafone360.people/app_np_thumbs";

    /** Dummy thumbnail ID. **/
    private static final Long DUMMY_IMG_ID = 123L;

    /** Dummy thumbnail file name. **/
    private static final String DUMMY_IMG_NAME =
        DUMMY_IMG_ID + ".png";

    /** Dummy thumbnail file name with complete path. **/
    private static final String DUMMY_IMG_PATH = FULL_PATH + "/"
        + DUMMY_IMG_NAME;

    /** Maximum image compression quality. **/
    private static final int MAX_IMAGE_QUALITY = 100;

    /***
     * JUnit test for the ThumbnailUtils.thumbnailPath() method.
     *
     * @see com.vodafone360.people.utils.ThumbnailUtils#thumbnailPath(
     *      Long thumbnailId)
     */
    public final void testThumbnailPath() {
        deleteThumbnailsDirectory();

        /** Check for the default thumbnail path. **/
        assertEquals("Expecting the default thumbnail path",
                FULL_PATH, ThumbnailUtils.thumbnailPath(null));

        /** Create the thumbnail directory. **/
        new File(FULL_PATH).mkdir();

        /** Check that a thumbnail for a given ID does not exist. **/
        assertTrue("Thumbnail should not already exist",
                ThumbnailUtils.thumbnailPath(DUMMY_IMG_ID) == null);

        /** Create a dummy thumbnail. **/
        createFile(DUMMY_IMG_PATH);

        /** Check that a thumbnail for a given ID now exists. **/
        assertEquals("Thumbnail should exist",
                DUMMY_IMG_PATH, ThumbnailUtils.thumbnailPath(DUMMY_IMG_ID));
    }

    /***
     * JUnit test for the ThumbnailUtils.thumbnailFileToWrite() method.
     *
     * @see com.vodafone360.people.utils.ThumbnailUtils#thumbnailFileToWrite(
     *      Long thumbnailId)
     */
    public final void testThumbnailFileToWrite() {
        assertEquals("Expecting a specific file name with PNG extension",
                DUMMY_IMG_NAME, ThumbnailUtils.thumbnailFileToWrite(
                        DUMMY_IMG_ID));
    }

    /***
     * JUnit test for the ThumbnailUtils.thumbnailFromFile() method.
     *
     * @see com.vodafone360.people.utils.ThumbnailUtils#thumbnailFromFile(Long
     *      thumbnailId)
     */
    public final void testThumbnailFromFile() {
        deleteThumbnailsDirectory();

        assertNull("File not not exist",
                ThumbnailUtils.thumbnailFromFile(DUMMY_IMG_ID));

        /** Create a new thumbnail folder. **/
        new File(FULL_PATH).mkdir();

        /** Create a new dummy bitmap to test. **/
        try {
            assertTrue("CopyAndCompressBitmap failed",
                    ThumbnailUtils.copyAndCompressBitmap(DUMMY_IMG_PATH,
                            BitmapFactory.decodeResource(getContext().
                                    getResources(), R.drawable.vodaphone)));
        } catch (Exception e) {
            fail("Not expecting an Exception" + e.getMessage());
        }

        assertNotNull("File should exist",
                ThumbnailUtils.thumbnailFromFile(DUMMY_IMG_ID));
    }

    /***
     * JUnit test for the ThumbnailUtils.thumbnailFromFile() and
     * ThumbnailUtils.copyAndCompressBitmap() methods.
     *
     * @see com.vodafone360.people.utils.ThumbnailUtils#thumbnailFromFile(Long
     *      thumbnailId)
     * @see com.vodafone360.people.utils.ThumbnailUtils#copyAndCompressBitmap(
     *      String destinationPath, Bitmap inputPhoto)
     */
    public final void testCopyAndCompressBitmap() {
        deleteThumbnailsDirectory();

        try {
            ThumbnailUtils.copyAndCompressBitmap(null, null);
            assertTrue("Expecting an InvalidParameterException", false);
        } catch (InvalidParameterException e) {
            assertTrue("Expected InvalidParameterException", true);
        } catch (Exception e) {
            fail("Not expecting an Exception" + e.getMessage());
        }

        try {
            ThumbnailUtils.copyAndCompressBitmap(DUMMY_IMG_PATH, null);
            fail("Expecting an InvalidParameterException");
        } catch (InvalidParameterException e) {
            assertTrue("Expected InvalidParameterException", true);
        } catch (Exception e) {
            fail("Not expecting an Exception" + e.getMessage());
        }

        /** Create a new dummy bitmap to test. **/
        Bitmap bitmap = BitmapFactory.decodeResource(getContext().
                getResources(), R.drawable.vodaphone);
        assertNotNull("Test bitmap should not be NULL", bitmap);

        try {
            assertTrue("Bitmap was not successfully saved",
                    ThumbnailUtils.copyAndCompressBitmap(DUMMY_IMG_PATH,
                            bitmap));
        } catch (FileNotFoundException e) {
            fail("Not expecting a FileNotFoundException" + e.getMessage());
        } catch (IOException e) {
            fail("Not expecting a IOException" + e.getMessage());
        }
    }

    /***
     * JUnit test for the ThumbnailUtils.saveExternalResponseObjectToFile()
     * method.
     *
     * @see com.vodafone360.people.utils.ThumbnailUtils#
     *      saveExternalResponseObjectToFile(Long localContactId,
     *      ExternalResponseObject ext, int MAX_QUALITY)
     */
    public final void testSaveExternalResponseObjectToFile() {
        deleteThumbnailsDirectory();

        try {
            ThumbnailUtils.saveExternalResponseObjectToFile(-1, null);
            assertTrue("Expected InvalidParameterException", false);

        } catch (InvalidParameterException e) {
            assertTrue("Expected InvalidParameterException", true);
        } catch (IOException e) {
            fail("Not expecting an IOException" + e.getMessage());
        }

        try {
            ThumbnailUtils.saveExternalResponseObjectToFile(DUMMY_IMG_ID,
                    generateExternalResponseObject());

        } catch (IOException e) {
            fail("Not expecting an IOException" + e.getMessage());
        }
    }

    /***
     * Creates a dummy ExternalResponseObject.
     *
     * @return Dummy ExternalResponseObject
     * @throws IOException Issue opening an Android resource.
     */
    private ExternalResponseObject generateExternalResponseObject()
            throws IOException {

        /** Generate image byte array. **/
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        InputStream is = getContext().getResources().openRawResource(
                R.drawable.vodaphone);
        int temp;
        while ((temp = is.read()) != -1) {
            os.write((char) temp);
        }

        /** Create a dummy ExternalResponseObject. **/
        ExternalResponseObject ero = new ExternalResponseObject();
        ero.mBody = null;
        ero.mBody = os.toByteArray();
        return ero;
    }

    /***
     * Utility to create a file with the given path (assumes that we have root
     * privileges).
     *
     * @param path Absolute path to create in the file system.
     */
    private static void createFile(final String path) {
        File dummyImage = new File(path);
        try {
            if (!dummyImage.exists()) {
                dummyImage.createNewFile();
            }
        } catch (IOException e) {
            Log.i(AllTests.LOG_TAG, "ThumbnailUtilsTest.createFile() "
                    + "IOException", e);
        }
    }

    /***
     * Utility to delete the thumbnails directory (assumes that we have root
     * privileges).
     */
    private static void deleteThumbnailsDirectory() {
        File thumbnailsDirectory = new File(FULL_PATH);
        if (thumbnailsDirectory.exists()) {
            for (File file : thumbnailsDirectory.listFiles()) {
                file.delete();
            }
            thumbnailsDirectory.delete();
        }
    }
}