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

package com.vodafone360.people.datatypes;

import java.util.Enumeration;
import java.util.Hashtable;

import com.vodafone360.people.engine.EngineManager.EngineId;
import com.vodafone360.people.utils.LogUtils;

/**
 * BaseDataType encapsulating a System notification message received from server
 * This message contains a code and error test and should be routed to the
 * approproate engine(s).
 */
public class SystemNotification extends PushEvent {
    /**
     * <String> "code" / <String> message code <String> "message" / <String>
     * message to the user Note: Currently not in scope but should be discussed,
     * system notifications would provide system messages, error conditions, etc
     */

    /**
     * Enumeration of System Notification codes that can be returned from
     * Server.
     */
    public enum SysNotificationCode {
        AUTH_INVALID("101"),
        COMMUNITY_AUTH_INVALID("102"),
        COMMUNITY_AUTH_VALID("103"),
        COMMUNITY_LOGOUT_FAILED("110"),
        COMMUNITY_LOGOUT_SUCCESSFUL("111"),
        COMMUNITY_NETWORK_LOGOUT("112"),
        SEND_MESSAGE_FAILED("201"),
        GENERIC("1000"),
        UNKNOWN("1001"),
        UNKNOWN_USER("1002"),
        FRIENDS_LIST_NULL("1003"),
        UNKNOWN_EVENT("1004"),
        UNKNOWN_MESSAGE_TYPE("1005"),
        CONVERSATION_NULL("1006"),
        SEND_MESSAGE_PARAMS_INVALID("1007"),
        SET_AVAILABILITY_PARAMS_INVALID("1008"),
        TOS_NULL("1009"),
        SMS_WAKEUP_FAILED("1010"),
        SMS_FAILED("1011"),
        UNKNOWN_CHANNEL("1012"),
        INVITATIONS_ACCEPT_ERROR("1013"),
        INVITATIONS_DENY_ERROR("1014"),
        CONTACTS_UPDATE_FAILED("1015"),
        MOBILE_REQUEST_PAYLOAD_PARSE_ERROR("1101"),
        EXTERNAL_HTTP_ERROR("1102"),
        MOBILE_EXTERNAL_PROXY("1103"),
        MOBILE_INTERNAL_PROXY("1104"),
        CHAT_HISTORY_NULL("1900"),
        CHAT_SUMMARY_NULL("1901");

        private final String tag;

        /**
         * Constructor creating SysNotificationCode item for specified String.
         * 
         * @param s String value for Tags item.
         */
        private SysNotificationCode(String s) {
            tag = s;
        }

        /**
         * String value associated with SysNotificationCode item.
         * 
         * @return String value for SysNotificationCode item.
         */
        private String tag() {
            return tag;
        }

        /**
         * Find SysNotificationCode item for specified String.
         * 
         * @param tag String value to find Tags item for.
         * @return SysNotificationCode item for specified String, null
         *         otherwise.
         */
        private static SysNotificationCode findTag(String tag) {
            for (SysNotificationCode tags : SysNotificationCode.values()) {
                if (tag.compareTo(tags.tag()) == 0) {
                    return tags;
                }
            }
            return null;
        }
    }

    /**
     * Tags associated with SystemNotification item.
     */
    private enum Tags {
        CODE("code"),
        MESSAGE("message");

        private final String tag;

        /**
         * String value associated with Tags item.
         * 
         * @return String value for Tags item.
         */
        private Tags(String s) {
            tag = s;
        }

        /**
         * String value associated with Tags item.
         * 
         * @return String value for Tags item.
         */
        private String tag() {
            return tag;
        }

        /**
         * Find Tags item for specified String
         * 
         * @param tag String value to find Tags item for
         * @return Tags item for specified String, null otherwise
         */
        private static Tags findTag(String tag) {
            for (Tags tags : Tags.values()) {
                if (tag.compareTo(tags.tag()) == 0) {
                    return tags;
                }
            }
            return null;
        }
    }

    private String code = null;

    private String message = null;

    private SysNotificationCode mSysCode;

    /** {@inheritDoc} */
    @Override
    public String name() {
        return "SystemNotification";
    }

    /**
     * Create SystemNotification message from hashtable generated by
     * Hessian-decoder.
     * 
     * @param hash Hashtable containing SystemNotification parameters.
     * @param engId ID for engine this message should be routed to.
     * @return SystemNotification created from hashtable.
     */
    static public SystemNotification createFromHashtable(Hashtable<String, Object> hash,
            EngineId engId) {
        SystemNotification sn = new SystemNotification();
        sn.mEngineId = engId;
        Enumeration<String> e = hash.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            Object value = hash.get(key);
            Tags tag = Tags.findTag(key);
            sn.setValue(tag, value);
        }
        sn.setEngine();

        return sn;
    }

    /**
     * Sets the value of the member data item associated with the specified tag.
     * 
     * @param tag Current tag
     * @param val Value associated with the tag
     */
    private void setValue(Tags tag, Object value) {
        if (tag == null)
            return;

        switch (tag) {
            case CODE:
                code = (String)value;
                mSysCode = SysNotificationCode.findTag((String)value);
                break;
            case MESSAGE:
                message = (String)value;
                break;
            default:
                // Do nothing.
                break;
        }
    }

    /**
     * Set EngineId of Engine that needs to handle the System Notification.
     */
    private void setEngine() {
        if (mSysCode != null) {
            switch (mSysCode) {
                case SEND_MESSAGE_FAILED:
                case CONVERSATION_NULL:
                case SEND_MESSAGE_PARAMS_INVALID:
                case SET_AVAILABILITY_PARAMS_INVALID:
                case TOS_NULL:
                case CHAT_HISTORY_NULL:
                case CHAT_SUMMARY_NULL:
                case COMMUNITY_AUTH_VALID:
                    mEngineId = EngineId.PRESENCE_ENGINE;
                    LogUtils.logE("SYSTEM_NOTIFICATION:" + mSysCode + ", message:" + message);
                    break;

                case EXTERNAL_HTTP_ERROR:
                    mEngineId = EngineId.CONTACT_SYNC_ENGINE;
                    LogUtils.logE("SYSTEM_NOTIFICATION:" + mSysCode + ", message:" + message);
                    break;

                case UNKNOWN_EVENT:
                case UNKNOWN_MESSAGE_TYPE:
                case GENERIC:
                case UNKNOWN:
                    LogUtils.logE("SYSTEM_NOTIFICATION:" + mSysCode + ", message:" + message);
                    break;

                default:
                    LogUtils.logE("SYSTEM_NOTIFICATION UNHANDLED:" + code + ", message:" + message);
            }
        } else {
            LogUtils.logE("UNEXPECTED UNHANDLED SYSTEM_NOTIFICATION:" + code + ", message:"
                    + message);
        }

    }

    /**
     * Get current System Notification code.
     * 
     * @return current System Notification code.
     */
    public SysNotificationCode getSysCode() {
        return mSysCode;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "SystemNotification [code=" + code + ", mSysCode=" + mSysCode + ", message="
                + message + "]";
    }
}
