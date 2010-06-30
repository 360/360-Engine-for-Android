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
import com.vodafone360.people.database.tables.ContactsTable;
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

public class ChatDbUtils {

    protected static final String COLUMNS = "::";

    protected static void convertUserIds(ChatMessage msg, DatabaseHelper databaseHelper) {
        String userId = msg.getUserId();
        // TODO: remove hardcode
        String network = "vodafone";
        int columnsIndex = userId.indexOf(COLUMNS);
        if (columnsIndex > -1) {
            network = userId.substring(0, columnsIndex);
            userId = userId.substring(columnsIndex + COLUMNS.length());
        }
        SocialNetwork sn = SocialNetwork.getValue(network);
        if (sn != null) {
            msg.setNetworkId(sn.ordinal());
        } else {
            throw new RuntimeException("ChatUtils.convertUserIds: Invalid network : " + network);
        }
        msg.setUserId(userId);
        int networkId = msg.getNetworkId();
        if (networkId == SocialNetwork.VODAFONE.ordinal()) {
            msg.setLocalContactId(ContactsTable.fetchLocalIdFromUserId(Long
                    .valueOf(msg.getUserId()), databaseHelper.getReadableDatabase()));
        } else {
            msg
                    .setLocalContactId(ContactDetailsTable.findLocalContactIdByKey(SocialNetwork
                            .getChatValue(networkId).toString(), msg.getUserId(),
                            ContactDetail.DetailKeys.VCARD_IMADDRESS, databaseHelper
                                    .getReadableDatabase()));
        }
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
        item.mContactNetwork = SocialNetwork.getChatValue(msg.getNetworkId()).toString();
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
        if (msg.getNetworkId() == SocialNetwork.VODAFONE.ordinal()) {
            msg.setUserId(String.valueOf(ContactsTable.fetchUserIdFromLocalContactId(msg
                    .getLocalContactId(), databaseHelper.getReadableDatabase())));
            tos.add(msg.getUserId());
        } else {
            msg.setUserId(ContactDetailsTable.findChatIdByLocalContactIdAndNetwork(SocialNetwork
                    .getChatValue(msg.getNetworkId()).toString(), msg.getLocalContactId(),
                    databaseHelper.getReadableDatabase()));
            String fullUserId = SocialNetwork.getChatValue(msg.getNetworkId()).toString() + COLUMNS
                    + msg.getUserId();
            tos.add(fullUserId);
            msg.setUserId(fullUserId);
        }
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
