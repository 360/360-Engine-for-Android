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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.os.Environment;
import android.util.Log;

import com.vodafone360.people.Settings;

/**
 * Logging utility functions: Allows logging to be enabled, Logs application
 * name and current thread name prepended to logged data.
 */
public final class LogUtils {
    /** Application tag prefix for LogCat. **/
    private static final String APP_NAME_PREFIX = "People_";
    
    /** Simple date format for Logging. **/
    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    /** SD Card filename. **/
    private static final String APP_FILE_NAME = "/sdcard/people.log";

    /** SD Card log file size limit (in bytes). **/
    private static final int LOG_FILE_LIMIT = 100000;

    /** Maximum number of SD Card log files. **/
    private static final int LOG_FILE_COUNT = 50;

    /** Stores the enabled state of the LogUtils function. **/
    private static Boolean mEnabled = false;

    /** SD Card data logger. **/
    private static Logger sLogger;
    
    /** SD Card data profile logger. **/
    private static Logger sProfileLogger;    

    /***
     * Private constructor makes it impossible to create an instance of this
     * utility class.
     */
    private LogUtils() {
        // Do nothing.
    }

    /**
     * Enable logging.
     */
    public static void enableLogcat() {
        mEnabled = true;

        /** Enable the SD Card logger **/
        sLogger = Logger.getLogger(APP_NAME_PREFIX);
        try {
            FileHandler fileHandler = new FileHandler(APP_FILE_NAME,
                    LOG_FILE_LIMIT, LOG_FILE_COUNT);
            fileHandler.setFormatter(new Formatter() {

                @Override
                public String format(final LogRecord logRecord) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(DATE_FORMAT.format(
                            new Date(logRecord.getMillis())));
                    sb.append(" ");
                    sb.append(logRecord.getMessage());
                    sb.append("\n");
                    return sb.toString();
                }

            });
            sLogger.addHandler(fileHandler);
        } catch (IOException e) {
            logE("LogUtils.logToFile() IOException, data will not be logged "
                    + "to file", e);
            sLogger = null;
        }
        
        if (Settings.ENABLED_PROFILE_ENGINES) {
            /** Enable the SD Card profiler **/
            sProfileLogger = Logger.getLogger("profiler");
            try {
                FileHandler fileHandler = new FileHandler("/sdcard/engineprofiler.log",
                        LOG_FILE_LIMIT, LOG_FILE_COUNT);
                fileHandler.setFormatter(new Formatter() {

                    @Override
                    public String format(final LogRecord logRecord) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(DATE_FORMAT.format(
                                new Date(logRecord.getMillis())));
                        sb.append("|");
                        sb.append(logRecord.getMessage());
                        sb.append("\n");
                        return sb.toString();
                    }

                });
                sProfileLogger.addHandler(fileHandler);
            } catch (IOException e) {
                logE("LogUtils.logToFile() IOException, data will not be logged "
                        + "to file", e);
                sProfileLogger = null;
            }
        }
    }

    /**
     * Write info log string.
     *
     * @param data String containing data to be logged.
     */
    public static void logI(final String data) {
        if (mEnabled) {
            Thread currentThread = Thread.currentThread();
            Log.i(APP_NAME_PREFIX + currentThread.getName(), data);
        }
    }

    /**
     * Write debug log string.
     *
     * @param data String containing data to be logged.
     */
    public static void logD(final String data) {
        if (mEnabled) {
            Thread currentThread = Thread.currentThread();
            Log.d(APP_NAME_PREFIX + currentThread.getName(), data);
        }
    }

    /**
     * Write warning log string.
     *
     * @param data String containing data to be logged.
     */
    public static void logW(final String data) {
        if (mEnabled) {
            Thread currentThread = Thread.currentThread();
            Log.w(APP_NAME_PREFIX + currentThread.getName(), data);
        }
    }

    /**
     * Write error log string.
     *
     * @param data String containing data to be logged.
     */
    public static void logE(final String data) {
        // FlurryAgent.onError("Generic", data, "Unknown");
        if (mEnabled) {
            Thread currentThread = Thread.currentThread();
            Log.e(APP_NAME_PREFIX + currentThread.getName(), data);
        }
    }

    /**
     * Write verbose log string.
     *
     * @param data String containing data to be logged.
     */
    public static void logV(final String data) {
        if (mEnabled) {
            Thread currentThread = Thread.currentThread();
            Log.v(APP_NAME_PREFIX + currentThread.getName(), data);
        }
    }

    /**
     * Write info log string with specific component name.
     *
     * @param name String name string to prepend to log data.
     * @param data String containing data to be logged.
     */
    public static void logWithName(final String name, final String data) {
        if (mEnabled) {
            Log.v(APP_NAME_PREFIX + name, data);
        }
    }

    /**
     * Write error log string with Exception thrown.
     *
     * @param data String containing data to be logged.
     * @param exception Exception associated with error.
     */
    public static void logE(final String data, final Throwable exception) {
        // FlurryAgent.onError("Generic", data,
        // exception.getClass().toString());
        if (mEnabled) {
            Thread currentThread = Thread.currentThread();
            Log.e(APP_NAME_PREFIX + currentThread.getName(), data, exception);
        }
    }

    /***
     * Write a line to the SD Card log file (e.g. "Deleted contact name from
     * NAB because....").
     *
     * @param data String containing data to be logged.
     */
    public static void logToFile(final String data) {
        if (mEnabled && sLogger != null) {
            sLogger.log(new LogRecord(Level.INFO, data));
        }
    }
    
    /***
     * Write a line to the SD Card log file (e.g. "Deleted contact name from
     * NAB because....").
     *
     * @param data String containing data to be logged.
     */
    public static void profileToFile(final String data) {
        if (mEnabled && sProfileLogger != null) {
            sProfileLogger.log(new LogRecord(Level.INFO, data));
        }
    }
    
    /**
     * 
     * Writes a given byte-array to the SD card under the given file name. 
     * 
     * @param data The data to write to the SD card.
     * @param fileName The file name to write the data under.
     * 
     */
    public static void logToFile(final byte[] data, final String fileName) {
        if (Settings.sEnableSuperExpensiveResponseFileLogging) {
            FileOutputStream fos = null;
            
            try {
                File root = Environment.getExternalStorageDirectory();
                if (root.canWrite()){
                    File binaryFile = new File(root, fileName);
                    fos = new FileOutputStream(binaryFile);
                    fos.write(data);
                    fos.flush();
                }
            } catch (IOException e) {
                logE("LogUtils.logToFile() Could not write " + fileName + " to SD card!");
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        logE("LogUtils.logToFile() Could not close file output stream!");
                    }
                }
            }
        }
    }

    /***
     * Returns if the logging feature is currently enabled.
     * 
     * @return TRUE if logging is enabled, FALSE otherwise.
     */
    public static Boolean isEnabled() {
        return mEnabled;
    }
}