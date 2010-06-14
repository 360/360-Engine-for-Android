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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.vodafone360.people.datatypes.ExternalResponseObject;

/**
 * Set of static utility functions for handling of thumb-nails within the People
 * client. Thumb-nails are stored in a specific sub-directory ('app_np_thumbs')
 * of the People application directory. Thumb-nail names are based on a supplied
 * ID (local Contact ID).
 */
public final class ThumbnailUtils {
    /** Full path for the thumbnails. **/
    public static final String THUMBNAIL_FILE_LOCATION = "/data/data/com.vodafone360.people/app_np_thumbs/";

    /** JPEG file extension. **/
    private static final String JPG_EXTENSION = ".jpg";

    /** PNG file extension. **/
    public static final String PNG_EXTENSION = ".png";

    /** Thumbnail request URI. **/
    public static final String REQUEST_THUMBNAIL_URI = "?encoding=png&size=50x50!&wait";

    /** Thumbnail file compression quality. **/
    private static final int PHOTO_QUALITY = 100;

    /** Height of thumb-nails stored by People client. */
    private static final int THUMBNAIL_HEIGHT = 50;

    /** Width of thumb-nails stored by People client. */
    private static final int THUMBNAIL_WIDTH = 50;

    /***
     * Private constructor to prevent the utility class from being instantiated.
     */
    private ThumbnailUtils() {
        // Do nothing.
    }

    /**
     * Return path to file containing specified thumb-nail.
     * 
     * @param thumbnailId ID of thumb-nail.
     * @return path to thumb-nail file, NULL if it does not exist.
     */
    public static String thumbnailPath(final Long thumbnailId) {
        if (thumbnailId == null) {
            return THUMBNAIL_FILE_LOCATION.substring(0, THUMBNAIL_FILE_LOCATION.length() - 1);
        }

        /*
         * We switched from JPEG to PNG so we have to check both file formats
         * for some time. Later only PNG should be checked.
         */
        String path = THUMBNAIL_FILE_LOCATION + thumbnailId + PNG_EXTENSION;
        if (new File(path).exists()) {
            return path;
        } else {
            path = THUMBNAIL_FILE_LOCATION + thumbnailId + JPG_EXTENSION;
            if (new File(path).exists()) {
                return path;
            }
        }

        return null;
    }

    /**
     * Generate filename for thumb-nail. This consists of the thumb-nail ID and
     * the .JPG extension.
     * 
     * @param thumbnailId ID of thumb-nail (e.g. local Contact ID).
     * @return String containing generated filename.
     */
    public static String thumbnailFileToWrite(final Long thumbnailId) {
        return thumbnailId + PNG_EXTENSION;
    }

    /**
     * Return Bitmap containing thumb-nail matching the supplied ID. A 50 x 50
     * Bitmap is generated if a file for the specified ID exists.
     * 
     * @param thumbnailId ID of thumb-nail to retrieve.
     * @return Bitmap of the requested thumb-nail, NULL if Bitmap cannot be
     *         generated.
     */
    public static Bitmap thumbnailFromFile(final Long thumbnailId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = THUMBNAIL_HEIGHT;
        options.outWidth = THUMBNAIL_WIDTH;

        /*
         * We switched from JPEG to PNG so we have to check both file formats
         * for some time. Later only PNG should be checked.
         */
        String path = THUMBNAIL_FILE_LOCATION + thumbnailId + PNG_EXTENSION;
        if (!new File(path).exists()) {
            path = THUMBNAIL_FILE_LOCATION + thumbnailId + JPG_EXTENSION;
        }

        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * Checks if the thumbnail folder exists and creates it if not.
     */
    private static void ensureThumbnailFolderExists() {
        File path = new File(THUMBNAIL_FILE_LOCATION);
        if (!path.exists()) {
            if (!path.mkdir()) {
                LogUtils.logE("ThumbnailUtils.ensureThumbnailFolderExists()");
            }
        }
    }

    /***
     * Copies and compresses the given bitmap image.
     * 
     * @param destinationPath Path to save new image.
     * @param inputPhoto Input Bitmap to save.
     * @return TRUE if the photo was successfully saved, FALSE if otherwise.
     * @throws IOException Issue with photoPath in the file system.
     * @throws FileNotFoundException Issue with photoPath in the file system.
     * @throws InvalidParameterException destinationPath is empty.
     * @throws InvalidParameterException inputPhoto is NULL.
     */
    public static boolean copyAndCompressBitmap(final String destinationPath,
            final Bitmap inputPhoto) throws IOException {
        if (destinationPath == null || destinationPath.equals("")) {
            throw new InvalidParameterException("ThumbnailUtils."
                    + "copyAndCompressBitmap() destinationPath cannot be " + "empty");
        }
        if (inputPhoto == null) {
            throw new InvalidParameterException("ThumbnailUtils."
                    + "copyAndCompressBitmap() inputPhoto cannot be " + "NULL");
        }

        ensureThumbnailFolderExists();

        /** Create target file. **/
        File file = new File(destinationPath);
        if (file.exists()) {
            if (!file.delete()) {
                LogUtils.logE("ThumbnailUtils.copyAndCompressBitmap()" + "Failed to delete file");
                return false;
            }
        }
        if (!file.createNewFile()) {
            LogUtils.logE("ThumbnailUtils.copyAndCompressBitmap() "
                    + "Failed to create file for bitmap stream");
            return false;
        }

        /** Save Bitmap to file. **/
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            if (stream == null) {
                LogUtils.logE("ThumbnailUtils.copyAndCompressBitmap() "
                        + "Failed to open stream for " + "file[" + file + "]");
                return false;
            }

            if (!inputPhoto.compress(Bitmap.CompressFormat.PNG, PHOTO_QUALITY, stream)) {
                LogUtils.logE("ThumbnailUtils.copyAndCompressBitmap() "
                        + "Failed to store bitmap stream");
                return false;
            }
        } finally {
            CloseUtils.close(stream);
        }
        return true;
    }

    /***
     * Saves an image in an ExternalResponseObject to the file system.
     * 
     * @param localContactId Local contact ID if the new Thumbnail files.
     * @param ext Given ExternalResponseObject to convert.
     * @param quality Compression quality of saved image.
     * @throws IOException File system issues.
     */
    public static void saveExternalResponseObjectToFile(final long localContactId,
            final ExternalResponseObject ext, final int quality) throws IOException {
        LogUtils.logI("DownloadServerThumbnails." + "saveExternalResponseObjectToFile() "
                + "Trying to save thumbnail.");

        if (localContactId < 0) {
            throw new InvalidParameterException("DownloadServerThumbnails."
                    + "saveExternalResponseObjectToFile() LocalContactId must "
                    + "be above 0, was[" + localContactId + "]");
        }
        if (ext == null) {
            throw new InvalidParameterException("DownloadServerThumbnails."
                    + "saveExternalResponseObjectToFile() "
                    + "ExternalResponseObject must not be NULL");
        }
        LogUtils.logI("DownloadServerthumbnails." + "saveExternalResponseObjectToFile() mimeType["
                + ext.mMimeType + "]");
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(makeFile(localContactId));
            fOut.write(ext.mBody);
            LogUtils.logI("DownloadServerThumbnails."
                    + "saveExternalResponseObjectToFile() Saved thumbnail.");
        } catch (Exception e) {
            LogUtils.logE("Exception in DownloadServerThumbnails."
                    + "saveExternalResponseObjectToFile() " + e);
        } finally {
            CloseUtils.flush(fOut);
            CloseUtils.close(fOut);
        }
    }

    /***
     * Creates a new File for the given Local Contact ID.
     * 
     * @param localContactId Local contact ID if the new Thumbnail files.
     * @return Newly created file.
     * @throws IOException File system issue.
     * @throws InvalidParameterException localContactId must be above 0.
     */
    private static File makeFile(final long localContactId) throws IOException {
        if (localContactId < 0) {
            throw new InvalidParameterException("DownloadServerThumbnails."
                    + "makeFile() LocalContactId must be above 0, was[" + localContactId + "]");
        }
        ensureThumbnailFolderExists();
        File file = new File(ThumbnailUtils.THUMBNAIL_FILE_LOCATION + localContactId
                + PNG_EXTENSION);

        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("DownloadServerThumbnails.makeFile() "
                        + "Unable to create new file");
            }
        }
        return file;
    }
}
