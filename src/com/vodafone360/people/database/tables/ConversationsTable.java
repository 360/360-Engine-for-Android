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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.datatypes.ChatMessage;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.utils.CloseUtils;

public abstract class ConversationsTable {

    /***
     * The name of the table as it appears in the database. TODO: methods
     * signatures might change
     */
    private static final String TABLE_NAME = "Conversations"; // it is used in

    // the tests

    private static final String DEFAULT_ERROR_MESSAGE = "ConversationsTable: the passed in database is null!";

    private static final long THIRTY_MINUTES = 30 * 60 * 1000;

    private static final String COLUMNS = "::";

    /**
     * An enumeration of all the field names in the database, containing ID,
     * LOCAL_CONTACT_ID, USER_ID, NETWORK_ID, NETWORK_STATUS.
     */
    private static enum Field {
        /**
         * The primary key.
         */
        ID("id"), // INT
        /**
         * the conversation unique id
         */
        CONVERSATION_ID("conversationId"), // STRING
        /**
         * the other party of the conversation unique id, localContactId
         */
        LOCALCONTACT_ID("fromLocalContactId"), // LONG
        /**
         * the other party of the conversation web id: IM address, 360 userId
         */
        USER_ID("toUserId"), // STRING
        /**
         * @see SocialNetwork
         */
        NETWORK_ID("networkId"),
        /**
         * the last time a message on this conversation was sent/received
         */
        LATEST_MESSAGE("timeStamp"); // LONG the chat
        // message id

        /**
         * The name of the field as it appears in the database.
         */
        private String mField;

        /**
         * Constructor.
         * 
         * @param field - Field name
         */
        private Field(String field) {
            mField = field;
        }

        /*
         * This implementation returns the field name. (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        public String toString() {
            return mField;
        }
    }

    // private static final int ID = 0;
    private static final int CONVERSATION_ID = 1;

    // private static final int LOCAL_CONTACT_ID = 2;
    private static final int USER_ID = 3;

    // private static final int NETWORK_ID = 4;
    private static final int LATEST_MESSAGE = 5;

    /**
     * This method creates the PresenceTable.
     * 
     * @param writableDb - the writable database
     * @throws SQLException is thrown when request to create a table fails with
     *             an SQLException
     * @throws NullPointerException if the passed in database instance is null
     */
    public static void create(SQLiteDatabase writableDb) throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "Conversations.create()");
        if (writableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        String createSql = "CREATE TABLE " + TABLE_NAME + " (" + Field.ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Field.CONVERSATION_ID + " STRING, "
                + Field.LOCALCONTACT_ID + " LONG, " + Field.USER_ID + " STRING, "
                + Field.NETWORK_ID + " INTEGER, " + Field.LATEST_MESSAGE + " LONG);";
        writableDb.execSQL(createSql);
    }

    public static void addNewConversationId(ChatMessage msg, SQLiteDatabase writableDb)
            throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "PresenceTable.create()");
        if (writableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }

        if (!conversationIdExists(msg.getConversationId(), writableDb)) {

            // remove old conversation for this user if it existed

            removeOldConversationByLocalContactIdAndNetworkId(msg.getLocalContactId(), msg
                    .getNetworkId(), writableDb);

            ContentValues values = new ContentValues();
            values.put(Field.CONVERSATION_ID.toString(), msg.getConversationId());
            values.put(Field.LOCALCONTACT_ID.toString(), msg.getLocalContactId());
            values.put(Field.USER_ID.toString(), msg.getUserId());
            values.put(Field.NETWORK_ID.toString(), msg.getNetworkId());
            values.put(Field.LATEST_MESSAGE.toString(), System.currentTimeMillis());
            writableDb.insertOrThrow(TABLE_NAME, null, values);

            values.clear();
        }
    }

    /**
     * TODO: this method might need redesign for performance: don't SELECT !!*
     * 
     * @param timeLineId
     * @param readableDb
     * @return
     * @throws SQLException
     * @throws NullPointerException
     */
    public static void fillMessageInByLocalContactIdAndNetworkId(ChatMessage msg,
            SQLiteDatabase readableDb, SQLiteDatabase writableDb) throws SQLException,
            NullPointerException {
        DatabaseHelper.trace(true, "PresenceTable.create()");
        if (readableDb == null || (writableDb == null)) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }

        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE "
                    + Field.LOCALCONTACT_ID + "=" + msg.getLocalContactId() + " AND "
                    + Field.NETWORK_ID + "=" + msg.getNetworkId(), null);

            List<String> tos = null;

            // should be just one
            while (c.moveToNext()) {
                if (System.currentTimeMillis() - c.getLong(LATEST_MESSAGE) < THIRTY_MINUTES) {
                    msg.setUserId(c.getString(USER_ID));
                    msg.setConversationId(c.getString(CONVERSATION_ID));
                    tos = new ArrayList<String>();
                    // People users don't need the "network::" prefix
                    if (SocialNetwork.VODAFONE.ordinal() == msg.getNetworkId()) {
                        tos.add(msg.getUserId());
                    } else {
                        tos.add(SocialNetwork.getChatValue(msg.getNetworkId()) + COLUMNS
                                + msg.getUserId());
                    }
                } else {
                    removeOldConversationByLocalContactIdAndNetworkId(msg.getLocalContactId(), msg
                            .getNetworkId(), writableDb);
                }
                break;
            }
            msg.setTos(tos);

            // this finally part should always run, while the exception is still
            // thrown
        } finally {
            CloseUtils.close(c);
            c = null;
        }

    }

    /**
     * @param localContactId
     * @param networkId
     * @param writableDb
     * @return
     * @throws SQLException
     * @throws NullPointerException
     */
    private static int removeOldConversationByLocalContactIdAndNetworkId(long localContactId,
            int networkId, SQLiteDatabase writableDb) throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "PresenceTable.create()");
        if (writableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        return writableDb.delete(TABLE_NAME, Field.LOCALCONTACT_ID + "=" + localContactId + " AND "
                + Field.NETWORK_ID + "=" + networkId, null);
    }

    /**
     * @param conversationId
     * @param readableDb
     * @return
     * @throws SQLException
     * @throws NullPointerException
     */
    private static boolean conversationIdExists(String conversationId, SQLiteDatabase readableDb)
            throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "PresenceTable.create()");
        if (readableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        boolean exists = false;
        Cursor c = null;
        try {
            c = readableDb.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE "
                    + Field.CONVERSATION_ID + "=\"" + conversationId + "\"", null);
            if (c == null)
                return false;
            exists = c.getCount() > 0;
        } finally {
            CloseUtils.close(c);
            c = null;
        }
        return exists;
    }

    /**
     * This method removes the conversation with this id
     * 
     * @param conversationId String - the conversation id to be removed
     * @param writableDb DatabaseHelper - database
     * @return the number of affected rows
     * @throws SQLException
     * @throws NullPointerException
     */
    public static int removeConversation(String conversationId, SQLiteDatabase writableDb)
            throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "ConversationsTable.removeOldConversation():" + conversationId);
        if (writableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        return writableDb.delete(TABLE_NAME, Field.CONVERSATION_ID + "=\"" + conversationId + "\"",
                null);
    }

    /**
     * This method removes the conversation with this contact on this network
     * 
     * @param localContactId long - the contacts mLocalContactId
     * @param networkId int - the network id, @see SocialNetwork
     * @param writableDb DatabaseHelper - the database
     * @return the number of affected rows
     * @throws SQLException
     * @throws NullPointerException
     */
    public static int removeConversation(long localContactId, int network, SQLiteDatabase writableDb)
            throws SQLException, NullPointerException {
        DatabaseHelper.trace(true, "ConversationsTable.removeConversation():" + localContactId
                + " with network " + network);
        if (writableDb == null) {
            throw new NullPointerException(DEFAULT_ERROR_MESSAGE);
        }
        return writableDb.delete(TABLE_NAME, Field.LOCALCONTACT_ID + "=\"" + localContactId
                + "\" AND " + Field.NETWORK_ID + "=" + network, null);
    }
   
}
