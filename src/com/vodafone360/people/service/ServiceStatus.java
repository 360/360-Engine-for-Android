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

/**
 * ServiceStatus contains the set of return codes associated with requests made
 * to the People client's remote service.
 */
public enum ServiceStatus {
    SUCCESS,
    UPDATED_TIMELINES_FROM_NATIVE,
    USER_CANCELLED,
    ERROR_COMMS,
    ERROR_BAD_SERVER_PARAMETER,
    ERROR_INVALID_PUBLIC_KEY, // when the public key has to be reloaded from
                              // server
    ERROR_COMMS_TIMEOUT,
    ERROR_IN_USE,
    ERROR_SERVICE_DISCONNECTED,
    ERROR_ACCOUNT_ACTIVATION_FAILED,
    ERROR_DATE_OF_BIRTH_INVALID,
    ERROR_USERNAME_IN_USE,
    ERROR_NOT_READY,
    ERROR_AUTHENTICATION_FAILED,
    ERROR_NOT_LOGGED_IN,
    ERROR_NO_INTERNET,
    ERROR_ROAMING_INTERNET_NOT_ALLOWED,
    ERROR_NO_AUTO_CONNECT, // Either auto connect is off or roaming with global
                           // or local setting is off
    ERROR_INVALID_SESSION,
    ERROR_USER_NOT_FOUND,
    ERROR_USERNAME_MISSING,
    ERROR_USERNAME_BLACKLISTED,
    ERROR_USERNAME_FORBIDDEN,
    ERROR_USERNAME_INVALID,
    ERROR_FULLNAME_MISSING,
    ERROR_INVALID_PASSWORD,
    ERROR_PASSWORD_MISSING,
    ERROR_ACCEPT_TC_MISSING,
    ERROR_EMAIL_MISSING,
    ERROR_COUNTRY_INVALID,
    ERROR_MSISDN_MISSING,
    ERROR_MSISDN_INVALID,
    ERROR_TIMEZONE_MISSING,
    ERROR_TIMEZONE_INVALID,
    ERROR_MOBILE_OPERATOR_INVALID,
    ERROR_MOBILE_MODEL_INVALID,
    ERROR_LANGUAGE_INVALID,
    ERROR_INVALID_CODE,
    ERROR_NO_SERVICE_RESPONSE,
    ERROR_NOT_IMPLEMENTED,
    ERROR_UNEXPECTED_RESPONSE,
    ERROR_DATABASE_CORRUPT,
    ERROR_COMMS_BAD_RESPONSE,
    ERROR_SMS_CODE_NOT_RECEIVED,
    ERROR_NOT_FOUND,
    ERROR_SYNC_FAILED,
    ERROR_ALREADY_EXISTS,
    ERROR_OUT_OF_MEMORY,
    ERROR_INTERNAL_SERVER_ERROR,
    ERROR_UNKNOWN,
    ERROR_CHAT_MESSAGE_NOT_SENT;

    /**
     * Generate ServiceStatus from Integer value
     * 
     * @param val Integer requiring conversion to Servicestatus
     * @return ServiceStatus corresponding supplied Integer value
     */
    public static ServiceStatus fromInteger(Integer val) {
        if (val == null || val.intValue() >= ServiceStatus.values().length) {
            return ServiceStatus.ERROR_UNKNOWN;
        }
        return ServiceStatus.values()[val.intValue()];
    }
}
