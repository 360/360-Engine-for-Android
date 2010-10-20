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

package com.vodafone360.people.engine.presence;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;

import com.vodafone360.people.database.DatabaseHelper;
import com.vodafone360.people.database.tables.ActivitiesTable;
import com.vodafone360.people.database.tables.ContactDetailsTable;
import com.vodafone360.people.database.tables.ContactSummaryTable;
import com.vodafone360.people.database.tables.ConversationsTable;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineNativeTypes;
import com.vodafone360.people.database.tables.ActivitiesTable.TimelineSummaryItem;
import com.vodafone360.people.datatypes.ActivityItem;
import com.vodafone360.people.datatypes.ChatMessage;
import com.vodafone360.people.datatypes.ContactDetail;
import com.vodafone360.people.datatypes.ContactSummary;
import com.vodafone360.people.datatypes.VCardHelper;
import com.vodafone360.people.datatypes.ContactDetail.DetailKeys;
import com.vodafone360.people.engine.presence.NetworkPresence.SocialNetwork;
import com.vodafone360.people.service.ServiceStatus;
import com.vodafone360.people.utils.LogUtils;

/***
 * Utilities relating to Chat messages.
 */
public class ChatDbUtils {

    /**
     * User ID is formated <network>::<userId>, using this as the divider.
     */
    protected static final String COLUMNS = "::";

    /***
     * Set the user ID, network ID and local contact ID values in the given
     * ChatMessage.
     *
     * The original msg.getUserId() is formatted <network>::<userId>, meaning
     * if "::" is present the values are split, otherwise the original value is
     * used.
     *
     * @param chatMessage ChatMessage to be altered.
     * @param databaseHelper DatabaseHelper with a readable database.
     */
    public static void convertUserIds(final ChatMessage chatMessage,
            final DatabaseHelper databaseHelper) {

        /**
         * Use original User ID, in case of NumberFormatException (see
         * PAND-2356).
         */
        final String originalUserId = chatMessage.getUserId();

        int index = originalUserId.indexOf(COLUMNS);
        if (index > -1) {
            /** Parse a <networkId>::<userId> formatted user ID. **/
            chatMessage.setUserId(
                    originalUserId.substring(index + COLUMNS.length()));
            String network = originalUserId.substring(0, index);

            /** Parse the <networkId> component. **/
            SocialNetwork sn = SocialNetwork.getValue(network);
            if (sn != null) {
                chatMessage.setNetworkId(sn.ordinal());
            } else {
                chatMessage.setNetworkId(SocialNetwork.INVALID.ordinal());
                LogUtils.logE("ChatUtils.convertUserIds() Invalid Network ID ["
                        + network + "] in [" + originalUserId + "]");
            }
        }

        chatMessage.setLocalContactId(
                ContactDetailsTable.findLocalContactIdByKey(
                        SocialNetwork.getSocialNetworkValue(
                                chatMessage.getNetworkId()).toString(),
                                chatMessage.getUserId(),
                                ContactDetail.DetailKeys.VCARD_IMADDRESS,
                                databaseHelper.getReadableDatabase())
                );
    }

    /**
     * This method saves the supplied
     * 
     * @param msg
     * @param type
     * @param databaseHelper
     */
    protected static void saveChatMessageAsATimeline(ChatMessage message,
            TimelineSummaryItem.Type type, DatabaseHelper databaseHelper) {
        TimelineSummaryItem item = new TimelineSummaryItem();
        fillInContactDetails(message, item, databaseHelper, type);
        SQLiteDatabase writableDatabase = databaseHelper.getWritableDatabase();
        
        boolean isRead = true;
        if(type == TimelineSummaryItem.Type.INCOMING) {
            isRead = false;
        }
        
        if (ActivitiesTable.addChatTimelineEvent(item,
                isRead,
                writableDatabase) != -1) {
            ConversationsTable.addNewConversationId(message, writableDatabase);
        } else {
            LogUtils.logE("The msg was not saved to the ActivitiesTable");
        }
    }

    /**
     * Remove hard code
     * 
     * @param msg
     * @param item
     * @param databaseHelper
     * @param incoming
     */
    private static void fillInContactDetails(ChatMessage msg, TimelineSummaryItem item,
            DatabaseHelper databaseHelper, TimelineSummaryItem.Type incoming) {
        item.mTimestamp = System.currentTimeMillis();
//        here we set the time stamp back into the chat message 
//        in order to be able to remove it from the chat history by time stamp in case its delivery fails
        msg.setTimeStamp(item.mTimestamp);
        
        item.mType = ActivityItem.Type.MESSAGE_IM_CONVERSATION;
        item.mDescription = msg.getBody();
        item.mTitle = DateFormat.getDateInstance().format(new Date(item.mTimestamp));

        // we store sender's localContactId for incoming msgs and recipient's
        // localContactId for outgoing msgs
        item.mLocalContactId = msg.getLocalContactId();
        if (item.mLocalContactId != null && item.mLocalContactId != -1) {
            ContactDetail cd = ContactDetailsTable.fetchDetail(item.mLocalContactId,
                    DetailKeys.VCARD_NAME, databaseHelper.getReadableDatabase());
            if (cd == null || cd.getName() == null) {
                // if we don't get any details, we have to check the summary
                // table because gtalk contacts
                // without name will be otherwise show as unknown
                ContactSummary contactSummary = new ContactSummary();
                ServiceStatus error = ContactSummaryTable.fetchSummaryItem(item.mLocalContactId,
                        contactSummary, databaseHelper.getReadableDatabase());
                if (error == ServiceStatus.SUCCESS) {
                    item.mContactName = (contactSummary.formattedName != null) ? contactSummary.formattedName
                            : ContactDetail.UNKNOWN_NAME;
                } else {
                    item.mContactName = ContactDetail.UNKNOWN_NAME;
                }
            } else {
                /** Get name from contact details. **/
                VCardHelper.Name name = cd.getName();
                item.mContactName = (name != null) ? name.toString() : ContactDetail.UNKNOWN_NAME;
            }
        }
        item.mIncoming = incoming;
        item.mContactNetwork = SocialNetwork.getSocialNetworkValue(msg.getNetworkId()).toString();
        item.mNativeItemType = TimelineNativeTypes.ChatLog.ordinal();
    }


    /**
     * This method copies the conversation id and user id into the supplied
     * ChatMessage based on its mNetworkId and mLocalContactId
     * 
     * @param chatMessage ChatMessage
     * @param databaseHelper Databasehelper
     */
    protected static void fillMessageByLocalContactIdAndNetworkId(ChatMessage chatMessage,
            DatabaseHelper databaseHelper) {
        ConversationsTable.fillMessageInByLocalContactIdAndNetworkId(chatMessage, databaseHelper
                .getReadableDatabase(), databaseHelper.getWritableDatabase());
    }

    /**
     * This method finds the user id (360 UserId or 3rd-party network id) and
     * sets it into the supplied chat message
     * 
     * @param msg ChatMessage - the supplied chat message
     * @param databaseHelper DatabaseHelper - the database
     */
    protected static void findUserIdForMessageByLocalContactIdAndNetworkId(ChatMessage msg,
            DatabaseHelper databaseHelper) {
        List<String> tos = new ArrayList<String>();
        msg.setUserId(ContactDetailsTable.findChatIdByLocalContactIdAndNetwork(SocialNetwork
                .getSocialNetworkValue(msg.getNetworkId()).toString(), msg.getLocalContactId(),
                databaseHelper.getReadableDatabase()));
        String fullUserId = SocialNetwork.getSocialNetworkValue(msg.getNetworkId()).toString() + COLUMNS
                + msg.getUserId();
        tos.add(fullUserId);
        msg.setUserId(fullUserId);
        msg.setTos(tos);
    }

    /**
     * This method deletes the conversation with the given id from the
     * ConversationsTable
     * 
     * @param conversationId String - the conversation id
     * @param dbHelper DatabaseHelper - the database
     */
    protected static void deleteConversationById(String conversationId, DatabaseHelper dbHelper) {
        ConversationsTable.removeConversation(conversationId, dbHelper.getWritableDatabase());
    }

    /**
     * This method deletes conversations older than 1 week except for those with
     * current contact
     * 
     * @param localContactId long- current contact mLocalContactId
     * @param dbHelper DatabaseHelper - the database
     */
    protected static void cleanOldConversationsExceptForContact(long localContactId,
            DatabaseHelper dbHelper) {
        ActivitiesTable.removeChatTimelineExceptForContact(localContactId, dbHelper
                .getWritableDatabase());
    }

    /**
     * This method returns the number of unread chat messages for this contact
     * 
     * @param localContactId long - the contact's mLocalContactId
     * @param network String - the specified network, @see SocialNetwork
     * @param dbHelper Database - the database
     * @return int - the number of unread chat messages for the specified
     *         contact
     */
    public static int getNumberOfUnreadChatMessagesForContactAndNetwork(long localContactId,
            String network, DatabaseHelper dbHelper) {
        return ActivitiesTable.getNumberOfUnreadChatMessagesForContactAndNetwork(localContactId,
                network, dbHelper.getReadableDatabase());
    }

    /**
     * This method deletes the last outgoing chat message in
     * ActivitiesTable, and removes the conversation id of this message from the
     * ConversationsTable. So that next time when user tries to send a message a
     * new conversation id will be requested. The message sending might fail
     * because the conversation id might have expired. 
     * 
     * Currently there's no reliable way to get the reason of message delivery failure, so
     * we assume that an expired conversation id might cause it as well, and remove it.
     *
     * In case the conversation id is valid the "get conversation" call will return the old id.
     * 
     * @param dbHelper DatabaseHelper - database
     * @param message ChatMessage - the chat message which has not been sent and needs to be deleted from the history.
     */
   protected static void deleteUnsentMessage(DatabaseHelper dbHelper, ChatMessage message) {
        ConversationsTable.removeConversation(message.getConversationId(), dbHelper.getWritableDatabase());
        ActivitiesTable.deleteUnsentChatMessageForContact(message.getLocalContactId(), message.getTimeStamp(), dbHelper.getWritableDatabase());
    }
}
