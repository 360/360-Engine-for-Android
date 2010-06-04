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

package com.vodafone360.people.database.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.vodafone360.people.Settings;
import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.utils.EncryptionUtils;
import com.vodafone360.people.database.utils.SqlUtils;
import com.vodafone360.people.datatypes.AuthSessionHolder;
import com.vodafone360.people.datatypes.LoginDetails;
import com.vodafone360.people.datatypes.PublicKeyDetails;
import com.vodafone360.people.service.PersistSettings;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.CloseUtils;
import com.vodafone360.people.utils.LogUtils;

/**
 * Contains all the functionality related to the state database table. This
 * table is used to persist individual setting data. The table always has only
 * one record. This class is never instantiated hence all methods must be
 * static.
 *
 * @version %I%, %G%
 */
public abstract class StateTable {

    /**
     * Name of the table as it appears in the database.
     */
    private static final String TABLE_NAME = "State";

    /**
     * The state table only has one record with the primary key set to the
     * following value.
     */
    private static final int PRIMARY_STATE_KEY_VALUE = 1;

    /**
     * An enumeration of all the field names in the database.
     */
    private static enum Field {
        /**
         * primary key.
         */
        STATEID("StateId"),
        /**
         * 360 user name.
         */
        USERNAME("Username"),
        /**
         * 360 user password.
         */
        PASSWORD("Password"),
        /**
         * 360 user mobile phone number.
         */
        MOBILENO("MobileNo"),
        /**
         * @see LoginDetails.
         */
        SUBSCIBERID("SubscriberId"),
        /**
         * @see LoginDetails.
         */
        REMEMBERME("RememberMe"),
        /**
         * @see LoginDetails.
         */
        AUTOCONNECT("AutoConnect"),
        /**
         * Contacts revision, @see CAPI contacts/getcontactchanges().
         */
        CONTACTSREVISION("ContactsRevision"),
        /**
         * TRUE if the registration/signing in process is successfully complete.
         */
        REGISTRATIONCOMPLETE("RegistrationComplete"),
        /**
         * 360 session id.
         */
        SESSIONID("sessionid"),
        /**
         * Secret number.
         */
        SESSIONSECRET("sessionsecret"),
        /**
         * Me profile 360 session user id.
         */
        SESSIONUSERID("sessionuserid"),
        /**
         * Me profile 360 session user name.
         */
        SESSIONUSERNAME("sessionusername"),
        /**
         * Me profile user local contact id.
         */
        MYCONTACTID("mycontactid"),
        /**
         * Security certificate public key exponential.
         */
        PUBLICKEYEXPONENTIAL("exponential"),
        /**
         * Security certificate public key modulus.
         */
        PUBLICKEYMODULO("modulo"),
        /**
         * Security certificate public key string.
         */
        PUBLICKEYBASE64("keyBase64"),
        /**
         * Security certificate public key X509 format.
         */
        PUBLICKEYX509("keyX509"),
        /**
         * Database state change flags.
         */
        MYCONTACTCHANGED("mycontactchanged"),
        /**
         * Contact picture needs updating flag.
         */
        MYCONTACTPICTURECHANGED("mycontactpicturechanged"),
        /**
         * Native database changes flag.
         */
        NATIVEDBCHANGED("NativeDbChanged"),
        /**
         * Newest status timestamp, @see ActivitiesEngine.
         */
        LASTSTATUSUPDATE("StatusTableUpdated"),
        /**
         * Oldest status timestamp, @see ActivitiesEngine.
         */
        OLDESTSTATUSUPDATE("OldestStatusUpdated"),
        /**
         * Newest phone call timestamp, @see FetchCallLogEvents.
         */
        LASTPHONECALLUPDATE("PhoneCallTableUpdated"),
        /**
         * Oldest phone call timestamp, @see FetchCallLogEvents.
         */
        OLDESTPHONECALL("OldestPhoneCall"),
        /**
         * Newest SMS timestamp, @see FetchSmsLogEvents.
         */
        LASTSMSUPDATE("SMSTableUpdated"),
        /**
         * Oldest SMS timestamp, @see FetchSMSLogEvents.
         */
        OLDESTSMS("OldestSMS"),
        /**
         * Newest MMS timestamp, @see FetchSMSLogEvents.
         */
        LASTMMSUPDATE("LastMMSUpdated"),
        /**
         * Oldest MMS timestamp, @see FetchSMSLogEvents.
         */
        OLDESTMMS("OldestMMS"),
        /**
         * Me profile revision number, used to call for "Me Profile" update.
         */
        MEPROFILEREVISION("MeProfileRevision"),
        /**
         * A flag to indicate the user avatar change.
         */
        MEPROFILEAVATARCHANGED("MeProfileAvatarChanged");

        // See PersistSettings.Options for more fields

        /**
         * The name of the field as it appears in the database.
         */
        private String mField;

        /**
         * Constructor.
         *
         * @param field - The name of the field (see list above)
         */
        private Field(final String field) {
            mField = field;
        }

        /**
         * @return the name of the field as it appears in the database.
         */
        public String toString() {
            return mField;
        }
    }

    /**
     * Create Settings Table and add a record with default setting values.
     *
     * @param writableDb A writable SQLite database.
     */
    public static void create(final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "StateTable.create()");
        String createSql = "CREATE TABLE " + TABLE_NAME + " ("
                + Field.STATEID + " INTEGER PRIMARY KEY, "
                + Field.USERNAME + " TEXT, "
                + Field.PASSWORD + " BINARY, "
                + Field.MOBILENO + " TEXT, "
                + Field.SUBSCIBERID + " TEXT, "
                + Field.REMEMBERME + " BOOLEAN, "
                + Field.AUTOCONNECT + " BOOLEAN, "
                + Field.REGISTRATIONCOMPLETE + " BOOLEAN,"
                + Field.SESSIONID + " TEXT,"
                + Field.SESSIONSECRET + " TEXT,"
                + Field.SESSIONUSERID + " LONG,"
                + Field.SESSIONUSERNAME + " TEXT,"
                + Field.CONTACTSREVISION + " LONG,"
                + Field.MYCONTACTID + " LONG,"
                /** AA added fields to store the public key. **/
                + Field.PUBLICKEYEXPONENTIAL + " BINARY,"
                + Field.PUBLICKEYMODULO + " BINARY,"
                + Field.PUBLICKEYBASE64 + " TEXT,"
                + Field.PUBLICKEYX509 + " BINARY,"
                /** End added fields to store the public key. **/
                + Field.MYCONTACTCHANGED + " BOOLEAN,"
                + Field.MYCONTACTPICTURECHANGED + " BOOLEAN,"
                + Field.NATIVEDBCHANGED + " BOOLEAN,"
                + Field.LASTSTATUSUPDATE + " LONG,"
                + Field.OLDESTSTATUSUPDATE + " LONG,"
                + Field.LASTPHONECALLUPDATE + " LONG,"
                + Field.OLDESTPHONECALL + " LONG,"
                + Field.LASTSMSUPDATE + " LONG,"
                + Field.OLDESTSMS + " LONG,"
                + Field.LASTMMSUPDATE + " LONG,"
                + Field.OLDESTMMS + " LONG,"
                + Field.MEPROFILEREVISION + " LONG,"
                + Field.MEPROFILEAVATARCHANGED + " BOOLEAN,";

        // Add additional settings from the PersistSettings object
        for (PersistSettings.Option option : PersistSettings.Option.values()) {
            createSql += option.tableFieldName() + " "
            + option.getType().getDbType() + ",";
        }
        createSql = createSql.substring(0, createSql.length() - 1);
        createSql += ");";
        writableDb.execSQL(createSql);

        /*
         * Insert a setting record with default values into the table
         */
        final ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        final PersistSettings setting = new PersistSettings();
        for (PersistSettings.Option option : PersistSettings.Option.values()) {
            setting.putDefaultOptionData(option);
            PersistSettings.addToContentValues(values, setting);
        }
        writableDb.insertOrThrow(TABLE_NAME, null, values);
    }

    /***
     * Private constructor to prevent instantiation.
     */
    private StateTable() {
        // Do nothing.
    }

    /**
     * Fetches cached user login credentials (without encryption information).
     *
     * @param details An empty LoginDetails object to be filled
     * @param readableDb Readable SQLite database for fetching the information
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus fetchLogonCredentials(
            final LoginDetails details, final SQLiteDatabase readableDb) {
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT "
                    + Field.USERNAME + ","
                    + Field.PASSWORD + ","
                    + Field.MOBILENO + ","
                    + Field.SUBSCIBERID + ","
                    + Field.REMEMBERME + ","
                    + Field.AUTOCONNECT
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID
                    + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (!cursor.moveToFirst()) {
                LogUtils.logE("StateTable.fetchLogonCredentials() Unable to "
                        + "fetch logon credentials: State record not found");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }

            details.mUsername =
                SqlUtils.setString(cursor, Field.USERNAME.toString());
            details.mPassword = EncryptionUtils.decryptPassword(
                    SqlUtils.setBlob(cursor, Field.PASSWORD.toString()));
            details.mMobileNo =
                SqlUtils.setString(cursor, Field.MOBILENO.toString());
            details.mSubscriberId =
                SqlUtils.setString(cursor, Field.SUBSCIBERID.toString());
            details.mRememberMe =
                SqlUtils.setBoolean(cursor, Field.REMEMBERME.toString(),
                        details.mRememberMe);
            details.mAutoConnect =
                SqlUtils.setBoolean(cursor, Field.AUTOCONNECT.toString(),
                        details.mRememberMe);

        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.fetchLogonCredentials() Exception - "
                    + "Unable to fetch logon credentials", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;

        } finally {
            CloseUtils.close(cursor);
        }

        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "StateTable.fetchLogonCredentials() "
                    + "username[" + details.mUsername + "] "
                    + "mobileNo[" + details.mMobileNo + "] "
                    + "subscriberId[" + details.mSubscriberId + "]");
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetches cached user login credentials (with encryption information).
     *
     * @param details An empty LoginDetails object to be filled
     * @param pubKeyDetails An empty PublicKeyDetails object to be filled
     * @param readableDb Readable SQLite database for fetching the information
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus fetchLogonCredentialsAndPublicKey(
            final LoginDetails details, final PublicKeyDetails pubKeyDetails,
            final SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "StateTable.fetchLogonCredentials() "
                    + "username[" + details.mUsername + "]");
        }
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT "
                    + Field.USERNAME + ","
                    + Field.PASSWORD + ","
                    + Field.MOBILENO + ","
                    + Field.SUBSCIBERID + ","
                    + Field.REMEMBERME + ","
                    + Field.AUTOCONNECT + ","
                    + Field.PUBLICKEYEXPONENTIAL + ","
                    + Field.PUBLICKEYMODULO + ","
                    + Field.PUBLICKEYBASE64 + ","
                    + Field.PUBLICKEYX509
                    + " FROM " + TABLE_NAME + " WHERE "
                    + Field.STATEID + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (!cursor.moveToFirst()) {
                LogUtils.logE("StateTable.fetchLogonCredentials() Unable to "
                        + "fetch logon credentials: State record not found");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }

            details.mUsername =
                SqlUtils.setString(cursor, Field.USERNAME.toString());
            details.mPassword = EncryptionUtils.decryptPassword(
                    SqlUtils.setBlob(cursor, Field.PASSWORD.toString()));
            details.mMobileNo =
                SqlUtils.setString(cursor, Field.MOBILENO.toString());
            details.mSubscriberId =
                SqlUtils.setString(cursor, Field.SUBSCIBERID.toString());
            details.mRememberMe =
                SqlUtils.setBoolean(cursor, Field.REMEMBERME.toString(),
                        details.mRememberMe);
            details.mAutoConnect =
                SqlUtils.setBoolean(cursor, Field.AUTOCONNECT.toString(),
                        details.mAutoConnect);

            /** Add the public key data here. **/
            if (pubKeyDetails != null) { // check what if it's null
                pubKeyDetails.mExponential = SqlUtils.setBlob(cursor,
                        Field.PUBLICKEYEXPONENTIAL.toString());
                pubKeyDetails.mModulus = SqlUtils.setBlob(cursor,
                        Field.PUBLICKEYMODULO.toString());
                pubKeyDetails.mKeyBase64 = SqlUtils.setString(cursor,
                        Field.PUBLICKEYBASE64.toString());
                pubKeyDetails.mKeyX509 = SqlUtils.setBlob(cursor,
                        Field.PUBLICKEYX509.toString());
            }

        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.fetchLogonCredentials() Exception - "
                    + "Unable to fetch logon credentials", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        } finally {
            CloseUtils.close(cursor);
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modify cached user credentials settings (without encryption information).
     *
     * @param details The new credentials.
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus modifyCredentials(final LoginDetails details,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyCredentials() "
                    + "username[" + details.mUsername + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        values.put(Field.USERNAME.toString(), details.mUsername);
        if (!details.mRememberMe) {
            values.put(Field.PASSWORD.toString(), (String) null);
        } else {
            values.put(Field.PASSWORD.toString(), EncryptionUtils
                    .encryptPassword(details.mPassword));
        }
        values.put(Field.MOBILENO.toString(), details.mMobileNo);
        values.put(Field.SUBSCIBERID.toString(), details.mSubscriberId);
        values.put(Field.REMEMBERME.toString(), details.mRememberMe);
        values.put(Field.AUTOCONNECT.toString(), details.mAutoConnect);

        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.modifyCredentials() "
                        + "Unable to modify login credentials");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
        } catch (SQLiteException e) {
            LogUtils.logE(
                    "StateTable.modifyCredentials() Exception - "
                    + "Unable to modify credentials", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modify cached user credentials settings (without encryption information).
     *
     * @param details The new credentials.
     * @param pubKeyDetails The new key details.
     * @param writableDb Writable SQLite database
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus modifyCredentialsAndPublicKey(
            final LoginDetails details, final PublicKeyDetails pubKeyDetails,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable."
                    + "modifyCredentialsAndPublicKey() username["
                    + details.mUsername + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        values.put(Field.USERNAME.toString(), details.mUsername);
        if (!details.mRememberMe) {
            values.put(Field.PASSWORD.toString(), (String) null);
        } else {
            values.put(Field.PASSWORD.toString(), EncryptionUtils
                    .encryptPassword(details.mPassword));
        }
        values.put(Field.MOBILENO.toString(), details.mMobileNo);
        values.put(Field.SUBSCIBERID.toString(), details.mSubscriberId);
        values.put(Field.REMEMBERME.toString(), details.mRememberMe);
        values.put(Field.AUTOCONNECT.toString(), details.mAutoConnect);

        // add the public key data here
        if (pubKeyDetails != null) { // check what if it's null
            values.put(Field.PUBLICKEYEXPONENTIAL.toString(),
                    pubKeyDetails.mExponential);
            values.put(Field.PUBLICKEYMODULO.toString(),
                    pubKeyDetails.mModulus);
            values.put(Field.PUBLICKEYBASE64.toString(),
                    pubKeyDetails.mKeyBase64);
            values.put(Field.PUBLICKEYX509.toString(), pubKeyDetails.mKeyX509);
        }

        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.modifyCredentialsAndPublicKey() "
                        + "Unable to modify login credentials");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.modifyCredentialsAndPublicKey() "
                    + "Exception. Unable to modify credentials and public key",
                    e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Set registration complete flag.
     *
     * @param complete The flag value (true or false)
     * @param writableDb Writable SQLite database
     * @return true if successful, false otherwise
     */
    public static boolean setRegistrationComplete(final boolean complete,
            final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "StateTable.setRegistrationComplete() "
                + "complete[" + complete + "]");
        ContentValues values = new ContentValues();
        if (complete) {
            values.put(Field.REGISTRATIONCOMPLETE.toString(), 1);
        } else {
            values.put(Field.REGISTRATIONCOMPLETE.toString(), 0);
        }

        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.setRegistrationComplete()"
                        + "Unable to modify registration complete flag");
                return false;
            }
        } catch (SQLException e) {
            LogUtils.logE("StateTable.setRegistrationComplete() Exception - "
                    + "Unable to set registration complete", e);
            return false;
        }
        return true;
    }

    /**
     * Fetch value of registration complete flag from database.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return true if registration is complete, false if registration is not
     *         complete or a database error has occurred.
     */
    public static boolean isRegistrationComplete(
            final SQLiteDatabase readableDb) {
        boolean mResult = false;
        Cursor mCursor = null;
        try {
            mCursor = readableDb.rawQuery("SELECT "
                    + Field.REGISTRATIONCOMPLETE + " FROM " + TABLE_NAME
                    + " WHERE "
                    + Field.STATEID + " = " + PRIMARY_STATE_KEY_VALUE,
                    null);
            if (mCursor.moveToFirst() && !mCursor.isNull(0)) {
                mResult = (mCursor.getInt(0) != 0);
            }
        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.isRegistrationComplete() Exception - "
                    + "Unable to select registration complete", e);
            return false;
        } finally {
            CloseUtils.close(mCursor);
            mCursor = null;
        }
        LogUtils.logE("StateTable.isRegistrationComplete() is " + mResult);
        DatabaseHelper.trace(false, "StateTable.isRegistrationComplete() "
                + "Return[" + mResult + "]");
        return mResult;
    }

    /**
     * Fetches the current contact revision for the server sync.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The revision number or null if an error occurs.
     */
    public static Integer fetchContactRevision(
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "StateTable.fetchContactRevision()");
        Integer value = null;
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.CONTACTSREVISION
                    + " FROM " + TABLE_NAME
                    + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null);
            if (!c.moveToFirst() || c.isNull(0)) {
                return null;
            }
            value = c.getInt(0);
            return value;

        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.fetchContactRevision() Exception -"
                    + " Unable to fetch contact revision", e);
            return null;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }

    /**
     * Stores a new server contact revision in the database.
     *
     * @param revision New revision number
     * @param writableDb Writable SQLite database for storing the information
     * @return true if successful, false otherwise
     */
    public static boolean modifyContactRevision(final Integer revision,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyContactRevision() "
                    + "revision[" + revision + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        values.put(Field.CONTACTSREVISION.toString(), revision);
        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.modifyContactRevision() "
                        + "Unable to modify contact revision state");
                return false;
            }
        } catch (SQLException e) {
            LogUtils.logE("StateTable.modifyContactRevision() "
                    + "Exception - Unable to modify contact revision", e);
            return false;
        }
        return true;
    }

    /**
     * Fetches an option from the settings table.
     *
     * @param option Specifies which option is required
     * @param readableDb Readable SQLite database for fetching the information
     * @return A PersistSettings object containing the option data if
     *         successful, null otherwise.
     */
    public static PersistSettings fetchOption(
            final PersistSettings.Option option,
            final SQLiteDatabase readableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(false, "StateTable.fetchOption() name["
                    + option.tableFieldName() + "] value["
                    + option.defaultValue() + "] type["
                    + option.getType() + "]");
        }
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + option.tableFieldName()
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null);
            if (!c.moveToFirst()) {
                LogUtils.logE("StateTable.fetchOption() Unable to find option "
                        + "in the database, option[" + option + "]");
                return null;
            }
            final PersistSettings setting = new PersistSettings();
            Object data = null;
            if (!c.isNull(0)) {
                data = PersistSettings.fetchValueFromCursor(c, 0,
                        c.getColumnName(0));
            }
            setting.putOptionData(option, data);
            LogUtils.logD("StateTable.fetchOption() Fetched option[" + option
                    + "]");
            return setting;

        } catch (SQLException e) {
            LogUtils.logE("StateTable.fetchOption() SQLException - Unable to "
                    + "fetch options from database", e);
            return null;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
    }

    /**
     * Stores a persist setting option in the database.
     *
     * @param setting The setting to store. Must have an option set with data.
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code
     */
    public static ServiceStatus setOption(final PersistSettings setting,
            final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "StateTable.setOption()");

        try {
            ContentValues values = new ContentValues();
            if (!PersistSettings.addToContentValues(values, setting)) {
                LogUtils.logE("StateTable.setOption() Unknown option["
                        + setting.getOption() + "]");
                return ServiceStatus.ERROR_NOT_FOUND;
            }
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.setOption() Unable to modify fields "
                        + "in settings table, values[" + values + "]");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            LogUtils.logE("StateTable.setOption() SQLException - "
                    + "Unable to set options in database", e);
            return null;
        }
    }

    /**
     * Fetches the current session from the database.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The session object or null if the session was not available.
     */
    public static AuthSessionHolder fetchSession(
            final SQLiteDatabase readableDb) {
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT "
                    + Field.SESSIONID + ","
                    + Field.SESSIONSECRET
                    + "," + Field.SESSIONUSERID + ","
                    + Field.SESSIONUSERNAME
                    + " FROM " + TABLE_NAME + " WHERE "
                    + Field.STATEID + " = " + PRIMARY_STATE_KEY_VALUE,
                    null);
            if (!cursor.moveToFirst()) {
                LogUtils.logE("StateTable.fetchSession() "
                        + "Unable to find session info in the database");
                return null;
            }

            if (cursor.isNull(cursor.getColumnIndex(Field.SESSIONID.toString()))
                    || cursor.isNull(cursor.getColumnIndex(
                            Field.SESSIONSECRET.toString()))
                    || cursor.isNull(cursor.getColumnIndex(
                            Field.SESSIONUSERID.toString()))
                    || cursor.isNull(cursor.getColumnIndex(
                            Field.SESSIONUSERNAME.toString()))) {
                LogUtils.logE("StateTable.fetchSession() "
                        + "Unable to find session data in the database");
                return null;
            }

            final AuthSessionHolder session = new AuthSessionHolder();
            session.sessionID = SqlUtils.setString(cursor,
                    Field.SESSIONID.toString());
            session.sessionSecret = SqlUtils.setString(cursor,
                    Field.SESSIONSECRET.toString());
            session.userID = SqlUtils.setLong(cursor,
                    Field.SESSIONUSERID.toString(), -1L);
            session.userName = SqlUtils.setString(cursor,
                    Field.SESSIONUSERNAME.toString());

            DatabaseHelper.trace(false, "StateTable.fetchSession() "
                    + "Fetched session[" + session.sessionID + "]");
            return session;

        } catch (SQLException e) {
            LogUtils.logE("StateTable.fetchSession() SQLException - "
                    + "Unable to fetch session from database", e);
            return null;

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /**
     * Stores the latest session in the database.
     *
     * @param session The session object to store.
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus setSession(final AuthSessionHolder session,
            final SQLiteDatabase writableDb) {
        DatabaseHelper.trace(true, "StateTable.setSession()");
        try {
            ContentValues values = new ContentValues();
            if (session != null) {
                values.put(Field.SESSIONID.toString(), session.sessionID);
                values.put(Field.SESSIONSECRET.toString(),
                        session.sessionSecret);
                values.put(Field.SESSIONUSERID.toString(), session.userID);
                values.put(Field.SESSIONUSERNAME.toString(), session.userName);
            } else {
                values.putNull(Field.SESSIONID.toString());
                values.putNull(Field.SESSIONSECRET.toString());
                values.putNull(Field.SESSIONUSERID.toString());
                values.putNull(Field.SESSIONUSERNAME.toString());
            }
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.setSession() Unable to modify "
                        + "fields in settings table, values[" + values + "]");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
            return ServiceStatus.SUCCESS;

        } catch (SQLException e) {
            LogUtils.logE("StateTable.setSession() SQLException - "
                    + "Unable to set session in database", e);
            return null;
        }
    }

    /**
     * Fetches the local contact ID for the me profile (which is stored in the
     * contacts table with the other contacts).
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The local contact ID value or null if the user profile has not
     *         yet been synced from the server.
     */
    public static Long fetchMeProfileId(final SQLiteDatabase readableDb) {
        Long mValue = null;
        Cursor mCursor = null;
        try {
            mCursor = readableDb.rawQuery("SELECT " + Field.MYCONTACTID
                    + " FROM " + TABLE_NAME + " WHERE "
                    + Field.STATEID + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (!mCursor.moveToFirst() || mCursor.isNull(0)) {
                DatabaseHelper.trace(false, "StateTable.fetchMeProfileId() "
                        + "Return NULL");
                return null;
            }
            if (!mCursor.isNull(0)) {
                mValue = mCursor.getLong(0);
            }
            DatabaseHelper.trace(false, "StateTable.fetchMeProfileId() "
                    + "Return[" + mValue + "]");
            return mValue;

        } catch (SQLiteException e) {
            LogUtils.logE(
                    "StateTable.fetchMeProfileId() Exception - "
                    + "Unable to fetch my contact ID", e);
            return null;
        } finally {
            CloseUtils.close(mCursor);
            mCursor = null;
        }
    }

    /**
     * Sets the me profile ID. This should only be called once when the me
     * profile is first synced from the server.
     *
     * @param myContactID The local contact ID of the me profile, or null if the
     *            me profile is deleted (the latter is currently possibly by the
     *            UI).
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyMeProfileID(final Long myContactID,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyMeProfileID() "
                    + "myContactID[" + myContactID + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        values.put(Field.MYCONTACTID.toString(), myContactID);
        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.modifyMeProfileID() "
                        + "Unable to modify my contact ID");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
        } catch (SQLException e) {
            LogUtils.logE("StateTable.modifyMeProfileID() SQLException - "
                    + "Unable to modifyMeProfileID in database", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetches the "me profile changed" flag from the database. This flag is
     * used to trigger a contact sync with the server.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return true if the me profile has changed, false if the me profile
     *         hasn't changed or a database error has occurred.
     */
    public static boolean fetchMeProfileChangedFlag(
            final SQLiteDatabase readableDb) {
        boolean mValue = false;
        Cursor mCursor = null;
        try {
            mCursor = readableDb.rawQuery("SELECT " + Field.MYCONTACTCHANGED
                    + " FROM " + TABLE_NAME + " WHERE "
                    + Field.STATEID + " = " + PRIMARY_STATE_KEY_VALUE,
                    null);
            if (!mCursor.moveToFirst() || mCursor.isNull(0)) {
                DatabaseHelper.trace(false, "StateTable."
                        + "fetchMeProfileChangedFlag() Return FALSE");
                return false;
            }
            if (!mCursor.isNull(0)) {
                mValue = (mCursor.getInt(0) != 0);
            }
            DatabaseHelper.trace(false, "StateTable."
                    + "fetchMeProfileChangedFlag() Return[" + mValue + "]");
            return mValue;

        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.fetchMeProfileChangedFlag() "
                    + "Exception - Unable to fetch my contact changed", e);
            return false;

        } finally {
            CloseUtils.close(mCursor);
            mCursor = null;
        }
    }

    /**
     * Sets the "me profile changed" flag in the database to the given value.
     *
     * @param myContactChanged The new value for the flag
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyMeProfileChangedFlag(
            final boolean myContactChanged, final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable."
                    + "modifyMeProfileChangedFlag() myContactChanged["
                    + myContactChanged + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        values.put(Field.MYCONTACTCHANGED.toString(), myContactChanged);
        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.modifyMeProfileChangedFlag() "
                        + "Unable to modify my contact changed flag");
                return ServiceStatus.ERROR_DATABASE_CORRUPT;
            }
        } catch (SQLException e) {
            LogUtils.logE("StateTable.modifyMeProfileChangedFlag "
                    + "SQLException - Unable to modifyMeProfileChangedFlag in "
                    + "database", e);
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetches the timestamp of the latest status activity that was synced. Used
     * to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information.
     * @return The timestamp in milliseconds, or 0 if the is a problem.
     */
    public static long fetchLatestStatusUpdateTime(
            final SQLiteDatabase readableDb) {
        Cursor cursor = null;
        long value = 0;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.LASTSTATUSUPDATE
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID
                    + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchLatestStatusUpdate() "
                    + "Returning value[" + value + "]");

        } finally {
            CloseUtils.close(cursor);
        }
        return value;
    }

    /**
     * Fetches the timestamp of the oldest status activity that was synced. Used
     * to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds
     */
    public static long fetchOldestStatusUpdate(
            final SQLiteDatabase readableDb) {
        Cursor cursor = null;
        long value = 0;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.OLDESTSTATUSUPDATE
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID
                    + " = " + PRIMARY_STATE_KEY_VALUE,
                    null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchOldestStatusUpdate()"
                    + " [" + value + "]");
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }
        return value;
    }

    /**
     * Fetches the timestamp of the oldest SMS that was synced. Used to keep
     * track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds
     */
    public static long fetchOldestSmsTime(final SQLiteDatabase readableDb) {
        Cursor cursor = null;
        long value = 0;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.OLDESTSMS + " FROM "
                    + TABLE_NAME + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchOldestSMS [" + value
                    + "]");
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }
        return value;
    }

    /**
     * Fetches the timestamp of the oldest MMS status activity that was synced.
     * Used to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds
     */
    public static long fetchOldestMmsTime(final SQLiteDatabase readableDb) {
        Cursor cursor = null;
        long value = 0;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.OLDESTMMS + " FROM "
                    + TABLE_NAME + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchOldestMMS() Return ["
                    + value + "]");
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }
        return value;
    }

    /**
     * Fetches the timestamp of the latest phone call timeline activity that was
     * synced. Used to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds, or 0 if there was a problem.
     */
    public static long fetchLatestPhoneCallTime(
            final SQLiteDatabase readableDb) {
        long value = 0;
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.LASTPHONECALLUPDATE
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchLatestPhoneCallTime()"
                    + " [" + value + "]");
            return value;

        } finally {
            CloseUtils.close(cursor);
        }
    }

    /**
     * Fetches the timestamp of the latest SMS timeline activity that was
     * synced. Used to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds
     */
    public static long fetchLatestSmsTime(final SQLiteDatabase readableDb) {
        long value = 0;
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.LASTSMSUPDATE
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchLatestSMSTime() "
                    + "mValue[" + value + "]");
            return value;
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }
    }

    /**
     * Fetches the timestamp of the latest MMS timeline activity that was
     * synced. Used to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds
     */
    public static long fetchLatestMmsTime(final SQLiteDatabase readableDb) {
        long value = 0;
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.LASTMMSUPDATE
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID
                    + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchLastMMSUpdate() "
                    + "mValue[" + value + "]");
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }
        return value;
    }

    /**
     * Fetches the timestamp of the oldest phone call timeline activity that was
     * synced. Used to keep track of which activities have already been added.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The timestamp in milliseconds
     */
    public static long fetchOldestPhoneCallTime(
            final SQLiteDatabase readableDb) {
        long value = 0;
        Cursor cursor = null;
        try {
            cursor = readableDb.rawQuery("SELECT " + Field.OLDESTPHONECALL
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID
                    + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                value = (cursor.getLong(0));
            }
            DatabaseHelper.trace(false, "StateTable.fetchOldestPhoneCallTime()"
                    + " [" + value + "]");
        } finally {
            CloseUtils.close(cursor);
            cursor = null;
        }
        return value;
    }

    /**
     * Modifies the timestamp for the last status activity that has been synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyLatestStatusUpdateTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyLatestStatusUpdate() "
                    + "[" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.LASTSTATUSUPDATE.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyLatestStatusUpdate() "
                    + "Unable to modify last status update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }

        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the oldest status activity that has been
     * synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyOldestStatusTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyOldestStatusUpdate()"
                    + " [" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.OLDESTSTATUSUPDATE.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyOldestStatusUpdate() "
                    + "Unable to modify oldest status update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the oldest SMS activity that has been synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyOldestSmsTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyOldestSMSTime() "
                    + "value[" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.OLDESTSMS.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyOldestSMSTime() "
                    + "Unable to modify oldest sms update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the oldest MMS activity that has been synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyOldestMmsTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyOldestMMSTime() "
                    + "value[" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.OLDESTMMS.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyOldestMMSTime() "
                    + "Unable to modify oldest mms update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the last phone call activity that has been
     * synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyLatestPhoneCallTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable."
                    + "modifyLatestPhoneCallTime() [" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.LASTPHONECALLUPDATE.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyLatestPhoneCallTime() "
                    + "Unable to modify last timeline update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }

        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the last SMS timeline activity that has been
     * synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyLatestSmsTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyLatestSMSUpdate() ["
                    + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.LASTSMSUPDATE.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyLatestSMSUpdate() "
                    + "Unable to modify last sms update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the last MMS timeline activity that has been
     * synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyLatestMmsTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyLatestMMSTime() "
                    + "value[" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.LASTMMSUPDATE.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyLatestMMSTime() "
                    + "Unable to modify last mms update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Modifies the timestamp for the oldest phone call timeline activity that
     * has been synced.
     *
     * @param value The new timestamp value (in milliseconds)
     * @param writableDb Writable SQLite database for storing the information
     * @return SUCCESS or a suitable error code.
     */
    public static ServiceStatus modifyOldestPhoneCallTime(final long value,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyLastTimelineUpdate()"
                    + " [" + value + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.OLDESTPHONECALL.toString(), value);
        if (writableDb.update(TABLE_NAME, values, null, null) <= 0) {
            LogUtils.logE("StateTable.modifyOldestPhoneCallTime() "
                    + "Unable to modify last timeline update");
            return ServiceStatus.ERROR_DATABASE_CORRUPT;
        }
        return ServiceStatus.SUCCESS;
    }

    /**
     * Fetches the current me profile revision for the server sync.
     *
     * @param readableDb Readable SQLite database for fetching the information
     * @return The revision number or null if an error occurs.
     */
    public static long fetchMeProfileRevision(
            final SQLiteDatabase readableDb) {
        DatabaseHelper.trace(false, "StateTable.fetchMeProfileRevision()");
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT " + Field.MEPROFILEREVISION
                    + " FROM " + TABLE_NAME + " WHERE " + Field.STATEID
                    + " = " + PRIMARY_STATE_KEY_VALUE, null);
            if (!c.moveToFirst() || c.isNull(0)) {
                return 0;
            }
            return c.getLong(0);

        } catch (SQLiteException e) {
            LogUtils.logE("StateTable.fetchMeProfileRevision Exception - "
                    + "Unable to fetch me profile revision", e);
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return 0;
    }

    /**
     * Stores a new server me profile revision in the database.
     *
     * @param revision New revision number
     * @param writableDb Writable SQLite database for storing the information
     * @return true if successful, false otherwise
     */
    public static boolean modifyMeProfileRevision(final long revision,
            final SQLiteDatabase writableDb) {
        if (Settings.ENABLED_DATABASE_TRACE) {
            DatabaseHelper.trace(true, "StateTable.modifyMeProfileRevision() "
                    + "revision[" + revision + "]");
        }
        ContentValues values = new ContentValues();
        values.put(Field.STATEID.toString(), PRIMARY_STATE_KEY_VALUE);
        values.put(Field.MEPROFILEREVISION.toString(), revision);
        try {
            if (writableDb.update(TABLE_NAME, values, Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE, null) <= 0) {
                LogUtils.logE("StateTable.modifyMeProfileRevision() "
                        + "Unable to modify me profile revision state");
                return false;
            }
        } catch (SQLException e) {
            LogUtils.logE("StateTable.modifyMeProfileRevision Exception - "
                    + "Unable to modify profile revision", e);
            return false;
        }
        return true;
    }

    /**
     * Fetches the "me profile avatar changed flag" from the state table.
     *
     * @param readableDb The SQLite database with read access
     * @return true if the avatar has changed, false otherwise
     */
    public static boolean fetchMeProfileAvatarChangedFlag(
            final SQLiteDatabase readableDb) {
        boolean mValue = false;
        Cursor mCursor = null;
        try {
            mCursor = readableDb.rawQuery("SELECT "
                    + Field.MEPROFILEAVATARCHANGED + " FROM " + TABLE_NAME
                    + " WHERE " + Field.STATEID + " = "
                    + PRIMARY_STATE_KEY_VALUE,
                    null);
            if (!mCursor.moveToFirst() || mCursor.isNull(0)) {
                DatabaseHelper.trace(false, "StateTable."
                        + "fetchMeProfileAvatarChangedFlag() Return FALSE");
                return false;
            }
            mValue = (mCursor.getInt(0) != 0);
            DatabaseHelper.trace(false, "StateTable."
                    + "fetchMeProfileAvatarChangedFlag() Return[" + mValue
                    + "]");
            return mValue;

        } catch (SQLException e) {
            LogUtils.logE("StateTable.fetchMeProfileAvatarChangedFlag() "
                    + "Exception", e);
            return false;
        } finally {
            CloseUtils.close(mCursor);
            mCursor = null;
        }
    }
}