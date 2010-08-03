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

package com.vodafone360.people;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import android.content.Context;

import com.vodafone360.people.R;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Handles default application settings. Stores default values for: - Server
 * URLS (Direct API and RPG) - Upgrade URL - Feature flags for
 * enabling/disabling functionality These values are overwritten by during the
 * build configuration files to allow different lifecycles to enable specific
 * features, and to target specific back end environments. Property defaults are
 * set in the Settings.java file.
 */
public final class SettingsManager {

    /** Comma string. **/
    private static final String COMMA = ",";

    /** Static settings HashTable. **/
    private static Hashtable<String, Object> sSettings;

    /**
     * Private constructor to prevent instantiation.
     */
    private SettingsManager() {
        // Do nothing.
    }

    /**
     * Load properties from build configuration files.
     *
     * @param context Android Context.
     * @throws NullPointerException Configuration file not found.
     * @throws NullPointerException context is NULL.
     */
    public static synchronized void loadProperties(final Context context) {
        if (context == null) {
            throw new NullPointerException(
                    "SettingsManager.loadProperties() context cannot be NULL");
        }
        if (sSettings != null) {
            return;
        }

        sSettings = new Hashtable<String, Object>();
        sSettings.put(Settings.APP_KEY_ID, "");
        sSettings.put(Settings.APP_SECRET_KEY, "");
        sSettings.put(Settings.ENABLE_LOGCAT_KEY,
                Settings.DEFAULT_ENABLE_LOGCAT);
        sSettings.put(Settings.ENABLE_RPG_KEY, Settings.DEFAULT_ENABLE_RPG);
        sSettings.put(Settings.ENABLE_SNS_RESOURCE_ICON_KEY,
                Settings.DEFAULT_ENABLE_SNS_RESOURCE_ICON);
        sSettings.put(Settings.RPG_SERVER_KEY, Settings.DEFAULT_RPG_SERVER);
        sSettings.put(Settings.SERVER_URL_HESSIAN_KEY,
                Settings.DEFAULT_SERVER_URL_HESSIAN);
        sSettings.put(Settings.DEACTIVATE_ENGINE_LIST_KEY, new String[] {});

        InputStream mFile =
            context.getResources().openRawResource(R.raw.config);
        if (mFile == null) {
            throw new NullPointerException("SettingsManager.loadProperties() "
                    + "Config file not present in build");
        }

        Properties mProps = new Properties();
        try {
            mProps.load(mFile);
            for (Enumeration<Object> mEnum =
                    mProps.keys(); mEnum.hasMoreElements();) {
                String mKey = (String) mEnum.nextElement();
                String mValue = mProps.getProperty(mKey);
                sSettings.put(mKey.trim(), mValue.trim());
                LogUtils.logV("SettingsManager.loadProperties() key["
                        + mKey.trim() + "] " + "value[" + mValue.trim() + "]");
            }
            if (sSettings.get(Settings.DEACTIVATE_ENGINE_LIST_KEY)
                    instanceof String) {
                /** Parse the array. **/
                sSettings.put(Settings.DEACTIVATE_ENGINE_LIST_KEY,
                        getStringArray((String) sSettings.get(
                                Settings.DEACTIVATE_ENGINE_LIST_KEY)));
            }

        } catch (IOException e) {
            LogUtils.logE("SettingsManager.loadProperties() IOException", e);

        } finally {
            mProps.clear();
            CloseUtils.close(mFile);
        }

        if (getBooleanProperty(Settings.ENABLE_LOGCAT_KEY)) {
            LogUtils.enableLogcat();
        }
    }

    /**
     * Get property value for a specific key.
     *
     * @param key Key for required property.
     * @return property value.
     */
    public static synchronized String getProperty(final String key) {
        if (key != null && sSettings != null) {
            return (String) sSettings.get(key);
        } else {
            return null;
        }
    }

    /**
     * Get boolean property value for key.
     *
     * @param key Key for required property.
     * @return boolean key value.
     */
    public static boolean getBooleanProperty(final String key) {
        String mRes = (String) getProperty(key);
        boolean mRet = false;
        if (mRes != null) {
            mRet = Boolean.parseBoolean(mRes);
        }
        return mRet;
    }

    /**
     * Gets an array of strings for a specific key.
     *
     * @param key Key for required property.
     * @return String array property value, NULL if key is NULL or HashTable of
     *         settings is NULL.
     */
    public static synchronized String[] getStringArrayProperty(
            final String key) {
        if (key != null && sSettings != null) {
            return (String[]) sSettings.get(key);
        } else {
            return null;
        }
    }

    /**
     * Get a String array out of the given comma separated values.
     *
     * @param value Comma separated values
     * @return String array from given comma separated values.
     */
    private static String[] getStringArray(final String value) {
        if (value == null) {
            return new String[] {};
        }
        ArrayList<String> list = new ArrayList<String>();
        int len = value.length();
        int read = 0;
        int index = 0;
        while (read < len) {
            index = value.indexOf(COMMA, read);
            if (index > 0) {
                list.add(value.substring(read, index));
                read = index + 1;
            } else if (index < 0) {
                list.add(value.substring(read, len));
                read = len + 1;
            } else {
                read = len + 1;
            }
        }
        int size = list.size();
        String[] ret;
        if (size > 0) {
            ret = new String[size];
        } else {
            ret = new String[] {};
        }
        for (int i = 0; i < size; i++) {
            ret[i] = list.get(i);
            LogUtils.logW("DEACTIVATE ENGINE: " + ret[i]);
        }

        return ret;
    }

    /**
     * Sets a property made of a key and its associated value. Note: Currently
     * only used for JUnit testing to force specific values.
     *
     * @param key the key
     * @param value the value associated with the key
     * @throws NullPointerException loadProperties() has not yet been called.
     */
    public static synchronized void setProperty(final String key,
            final String value) {
        if (sSettings != null) {
            sSettings.put(key, value);
        } else {
            throw new NullPointerException("SettingsManager.setProperty() "
                    + "The methoid SettingsManager.loadProperties() has not "
                    + "yet been called.");
        }
    }
}