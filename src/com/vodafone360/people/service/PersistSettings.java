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

package com.vodafone360.people.service;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class responsible for handling persistent settings within the People client.
 * These settings are stored in the State table in the People client's database.
 */
public class PersistSettings implements Parcelable {
    // These two member variables must never be null
    private Option mOption = Option.INTERNETAVAIL;

    private Object mValue = InternetAvail.ALWAYS_CONNECT;

    /**
     * Constructor
     */
    public PersistSettings() {
        putDefaultOptionData();
    }

    /**
     * Internet availability settings, options are always connect or only allow
     * manual connection
     */
    public static enum InternetAvail {
        ALWAYS_CONNECT,
        MANUAL_CONNECT;
    }

    /**
     * Definition for Settings type (boolean, string, integer, long).
     */
    public static enum OptionType {
        BOOLEAN("BOOLEAN"),
        STRING("STRING"),
        INTEGER("INTEGER"),
        LONG("LONG");

        /** Field name as stored in State table. */
        private String mDbType = null;

        /**
         * OptionType constructor.
         * 
         * @param dbType State table record name for current item.
         */
        private OptionType(String dbType) {
            mDbType = dbType;
        }

        /**
         * Return the State table field name for this item.
         * 
         * @return String containing State table field name for this item.
         */
        public String getDbType() {
            return mDbType;
        }
    };

    /**
     * Definition of a set of options handled by PersistSettings. These options
     * are; Internet availability (always available, only in home network or
     * manually activated) First time contact sync status. First time native
     * contact sync status.
     */
    public static enum Option {
        INTERNETAVAIL("internetavail", OptionType.INTEGER, InternetAvail.ALWAYS_CONNECT.ordinal()),
        FIRST_TIME_SYNC_STARTED("ftsstarted", OptionType.BOOLEAN, false),
        FIRST_TIME_MESYNC_STARTED("ftmesyncstarted", OptionType.BOOLEAN, false),
        FIRST_TIME_SYNC_COMPLETE("ftscomplete", OptionType.BOOLEAN, false),
        FIRST_TIME_MESYNC_COMPLETE("ftmesynccomplete", OptionType.BOOLEAN, false),
        FIRST_TIME_NATIVE_SYNC_COMPLETE("ftnativecomplete", OptionType.BOOLEAN, false);

        private String mFieldName;

        private Object mDefaultValue;

        private OptionType mType;

        /**
         * Constructor
         * 
         * @param fieldName Name of setting item.
         * @param type Type of field (String, boolean, integer, long).
         * @param defaultValue Default value for item.
         */
        private Option(String fieldName, OptionType type, Object defaultValue) {
            mFieldName = fieldName;
            mType = type;
            mDefaultValue = defaultValue;
        }

        /**
         * Return the default value for current setting.
         * 
         * @return the default value for current setting.
         */
        public Object defaultValue() {
            return mDefaultValue;
        }

        /**
         * Return the type of current option (i.e. String, boolean, integer,
         * long).
         * 
         * @return type of current option.
         */
        public OptionType getType() {
            return mType;
        }

        @Override
        public String toString() {
            return "\nOption info:\nID = " + super.toString() + "\n" + "TableFieldName = "
                    + mFieldName + "\n" + "Type: " + mType + "\n" + "Default: " + mDefaultValue
                    + "\n";
        }

        /**
         * Search for settings item by name.
         * 
         * @param key Option name to search for.
         * @return Option item, null if item not found.
         */
        private static Option lookupValue(String key) {
            for (Option option : Option.values()) {
                if (key.equals(option.mFieldName)) {
                    return option;
                }
            }
            return null;
        }

        /**
         * Option's State table record name
         * 
         * @return Name of the State table field corresponding to this Option.
         */
        public String tableFieldName() {
            return mFieldName;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Option: " + mOption.tableFieldName() + ", Value: " + mValue;
    }

    /**
     * Return the default (boolean) value for supplied Option.
     * 
     * @param option Option
     * @return default boolean value for specified Option or false if if the
     *         default value is null or not a boolean.
     */
    private static boolean getDefaultBoolean(Option option) {
        if (option.defaultValue() != null && option.defaultValue().getClass().equals(Boolean.class)) {
            return (Boolean)option.defaultValue();
        }
        return false;
    }

    /**
     * Return the default (integer) value for supplied Option.
     * 
     * @param option Option
     * @return default integer value for specified Option or false if if the
     *         default value is null or not a integer.
     */
    private static int getDefaultInt(Option option) {
        if (option.defaultValue() != null && option.defaultValue().getClass().equals(Integer.class)) {
            return (Integer)option.defaultValue();
        }
        return 0;
    }

    /**
     * Set the default value stored in PersistDettings for specified Option.
     * 
     * @param option Option the default value held by PersistSettings is the
     *            value set in the supplied Option.
     */
    public void putDefaultOptionData(Option option) {
        if (option != null) {
            mOption = option;
            putDefaultOptionData();
        }
    }

    /**
     * Set the default value stored in PersistDettings for current Option.
     */
    public void putDefaultOptionData() {
        mValue = mOption.mDefaultValue;
    }

    /**
     * Set the default value for the specified Option
     * 
     * @param option Option to set default data for.
     * @param data Value for default setting.
     */
    public void putOptionData(Option option, Object data) {
        mOption = option;
        if (data != null) {
            mValue = data;
        } else {
            putDefaultOptionData();
        }
    }

    /**
     * Add setting from supplied PersistSettings instance to supplied
     * ContentValues instance.
     * 
     * @param contentValues ContentValue to update.
     * @param setting PersistSettings instance containing settings value.
     * @return true (cannot return false!).
     */
    public static boolean addToContentValues(ContentValues contentValues, PersistSettings setting) {
        PersistSettings.Option option = setting.getOption();
        switch (option.getType()) {
            case BOOLEAN:
                contentValues.put(option.tableFieldName(), (Boolean)setting.getValue());
                break;
            case STRING:
                contentValues.put(option.tableFieldName(), (String)setting.getValue());
                break;
            case INTEGER:
                contentValues.put(option.tableFieldName(), (Integer)setting.getValue());
                break;
            case LONG:
                contentValues.put(option.tableFieldName(), (Long)setting.getValue());
                break;
            default:
                contentValues.put(option.tableFieldName(), setting.getValue().toString());
                break;
        }
        return true;
    }

    /**
     * Fetch Object from database Cursor.
     * 
     * @param c Database Cursor pointing to item of interest.
     * @param colIndex Column index within item.
     * @param key Key used to obtain required Option item.
     * @return Value obtained for Cursor, null if option does not exist or key
     *         does not match valid Option.
     */
    public static Object fetchValueFromCursor(Cursor c, int colIndex, String key) {
        PersistSettings.Option option = PersistSettings.Option.lookupValue(key);
        if (option == null || c.isNull(colIndex)) {
            return null;
        }
        switch (option.getType()) {
            case BOOLEAN:
                return (c.getInt(colIndex) == 0 ? false : true);
            case STRING:
                return c.getString(colIndex);
            case INTEGER:
                return c.getInt(colIndex);
            case LONG:
                return c.getLong(colIndex);
            default:
                return c.getString(colIndex);
        }
    }

    /**
     * Set Internet availability value.
     * 
     * @param value InternetAvail.
     */
    public void putInternetAvail(InternetAvail value) {
        mOption = Option.INTERNETAVAIL;
        if (value != null) {
            mValue = (Integer)value.ordinal();
        } else {
            mValue = InternetAvail.values()[getDefaultInt(mOption)];
        }
    }

    /**
     * Get current InternetAvail value
     * 
     * @return current InternetAvail value.
     */
    public InternetAvail getInternetAvail() {
        if (mOption == Option.INTERNETAVAIL && mValue.getClass().equals(Integer.class)) {
            int val = (Integer)mValue;
            if (val < InternetAvail.values().length) {
                return InternetAvail.values()[val];
            }
        }
        return InternetAvail.values()[getDefaultInt(Option.INTERNETAVAIL)];
    }

    /**
     * Return value indicating whether first time native contact sync has
     * completed.
     * 
     * @return stored boolean value indicating whether first time native contact
     *         sync has completed.
     */
    public boolean getFirstTimeNativeSyncComplete() {
        if (mOption == Option.FIRST_TIME_NATIVE_SYNC_COMPLETE
                && mValue.getClass().equals(Boolean.class)) {
            return (Boolean)mValue;
        }
        return getDefaultBoolean(Option.FIRST_TIME_NATIVE_SYNC_COMPLETE);

    }

    /**
     * Store value indicating whether first time native contact sync has
     * completed.
     * 
     * @param value value indicating whether first time native contact sync has
     *            completed.
     */
    public void putFirstTimeNativeSyncComplete(boolean value) {
        mOption = Option.FIRST_TIME_NATIVE_SYNC_COMPLETE;
        mValue = (Boolean)value;
    }

    /**
     * Store value indicating whether first time contact sync has completed.
     * 
     * @param value value indicating whether first time native contact sync has
     *            completed.
     */
    public void putFirstTimeSyncComplete(boolean value) {
        mOption = Option.FIRST_TIME_SYNC_COMPLETE;
        mValue = (Boolean)value;
    }

    public void putFirstTimeMeSyncComplete(boolean value) {
        mOption = Option.FIRST_TIME_MESYNC_COMPLETE;
        mValue = (Boolean)value;
    }

    /**
     * Return value indicating whether first time native contact sync has
     * completed.
     * 
     * @return value indicating whether first time native contact sync has
     *         completed.
     */
    public boolean getFirstTimeSyncComplete() {
        if (mOption == Option.FIRST_TIME_SYNC_COMPLETE && mValue.getClass().equals(Boolean.class)) {
            return (Boolean)mValue;
        }
        return getDefaultBoolean(Option.FIRST_TIME_SYNC_COMPLETE);
    }

    public boolean getFirstTimeMeSyncComplete() {
        if (mOption == Option.FIRST_TIME_MESYNC_COMPLETE && mValue.getClass().equals(Boolean.class)) {
            return (Boolean)mValue;
        }
        return getDefaultBoolean(Option.FIRST_TIME_MESYNC_COMPLETE);
    }

    /**
     * Store value indicating whether first time native contact sync has
     * started.
     * 
     * @param value value indicating whether first time native contact sync has
     *            started.
     */
    public void putFirstTimeSyncStarted(boolean value) {
        mOption = Option.FIRST_TIME_SYNC_STARTED;
        mValue = (Boolean)value;
    }

    public void putFirstTimeMeSyncStarted(boolean value) {
        mOption = Option.FIRST_TIME_MESYNC_STARTED;
        mValue = (Boolean)value;
    }

    /**
     * Return value indicating whether first time native contact sync has
     * started.
     * 
     * @return value indicating whether first time native contact sync has
     *         started.
     */
    public boolean getFirstTimeSyncStarted() {
        if (mOption == Option.FIRST_TIME_SYNC_STARTED && mValue.getClass().equals(Boolean.class)) {
            return (Boolean)mValue;
        }
        return getDefaultBoolean(Option.FIRST_TIME_SYNC_STARTED);
    }

    public boolean getFirstTimeMeSyncStarted() {
        if (mOption == Option.FIRST_TIME_MESYNC_STARTED && mValue.getClass().equals(Boolean.class)) {
            return (Boolean)mValue;
        }
        return getDefaultBoolean(Option.FIRST_TIME_MESYNC_STARTED);
    }

    /**
     * Get Option associated with this PersistSettings.
     * 
     * @return Option associated with this PersistSettings.
     */
    public Option getOption() {
        return mOption;
    }

    /**
     * Get value associated with this PersistSettings.
     * 
     * @return Object representing value associated with this PersistSettings.
     */
    private Object getValue() {
        return mValue;
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mOption.ordinal());
        switch (mOption.getType()) {
            case BOOLEAN:
                dest.writeByte((byte)(((Boolean)mValue) ? 1 : 0));
                break;
            case STRING:
                dest.writeString((String)mValue);
                break;
            case INTEGER:
                dest.writeInt((Integer)mValue);
                break;
            case LONG:
                dest.writeLong((Long)mValue);
                break;
            default:
                break;
        }
    }
}
