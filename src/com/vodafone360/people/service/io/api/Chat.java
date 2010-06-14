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

package com.vodafone360.people.service.io.api;

import java.util.List;

import com.vodafone360.people.Settings;
import com.vodafone360.people.datatypes.ChatMessage;
import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.service.io.QueueManager;
import com.vodafone360.people.service.io.Request;
import com.vodafone360.people.utils.LogUtils;

public class Chat {

    private static final String EMPTY = ""; // TODO: What is the name of this
                                            // function?

    /**
     * Sends chat message to server
     * 
     * @param msg - ChatMessage to be sent
     * @return request id
     */
    public static int sendChatMessage(ChatMessage msg) {
        if (msg.getTos() == null) {
            LogUtils.logE("Chat.sentChatMessage() msg.mTos can't be null");
            return -1;
        }
        if (msg.getBody() == null) {
            LogUtils.logE("Chat.sentChatMessage() msg.mBody can't be null");
            return -1;
        }
        if (msg.getConversationId() == null) {
            LogUtils.logE("Chat.sentChatMessage() msg.mConversationId can't be null");
            return -1;
        }

        Request request = new Request(EMPTY, Request.Type.SEND_CHAT_MESSAGE,
                EngineId.PRESENCE_ENGINE, true, Settings.API_REQUESTS_TIMEOUT_CHAT_SEND_MESSAGE);
        request.addData(ChatMessage.Tags.BODY.tag(), msg.getBody());
        request.addData(ChatMessage.Tags.RECIPIENTS_LIST.tag(), msg.getTos());
        request.addData(ChatMessage.Tags.CONVERSATION_ID.tag(), msg.getConversationId());

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }

    /**
     * Starts new chat session
     * 
     * @param recipients - list of UserIds
     * @return request id
     */
    public static int startChat(List<String> recipients) {
        if (recipients == null) {
            LogUtils.logE("Chat.sentChatMessage() recipients can't be null");
            return -1;
        }

        Request request = new Request(EMPTY, Request.Type.CREATE_CONVERSATION,
                EngineId.PRESENCE_ENGINE, false, Settings.API_REQUESTS_TIMEOUT_CHAT_CREATE_CONV);
        request.addData(ChatMessage.Tags.RECIPIENTS_LIST.tag(), recipients);

        QueueManager queue = QueueManager.getInstance();
        int requestId = queue.addRequest(request);
        queue.fireQueueStateChanged();
        return requestId;
    }
}
