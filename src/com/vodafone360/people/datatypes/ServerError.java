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

import com.vodafone360.people.service.ServiceStatus;

/**
 * BaseDataType representing an error returned from Server
 */
public class ServerError extends BaseDataType {
    /**
     * Enumeration of Server error types.
     */
    public enum ErrorType {
        REQUEST_TIMEOUT,
        HTTP_TIMEOUT,
        AUTH_USER_NOT_FOUND,
        AUTH_INVALID_CREDENTIALS,
        INVALID_AUTHENTICATION,
        INVALID_PARAMETER,
        INVALID_SESSION,
        DATEOFBIRTHINVALID,
        PASSWORDINVALID,
        INVALIDUSERNAME,
        USERNAMEINUSE,
        USERNAMEMISSING,
        USERNAMEBLACKLISTED,
        USERNAMEFORBIDDEN,
        FULLNAMEMISSING,
        PASSWORDMISSING,
        ACCEPTTCMISSING,
        EMAILMISSING,
        COUNTRYINVALID,
        MSISDNMISSING,
        MSISDNINVALID,
        MSISDNDIALCODEINVALID,
        TIMEZONEMISSING,
        TIMEZONEINVALID,
        MOBILEOPERATORINVALID,
        MOBILEMODELINVALID,
        LANGUAGEINVALID,
        INTERNALERROR,
        INTERNAL_ERROR,
        INVALIDCODE,
        UNKNOWN,
        INVALID_KEY; // server again
    }

    /**
     * Type of error
     */
    public ErrorType errorType;

    /**
     * Description of error
     */
    public String errorDescription;
    
    /**
     * Constructor
     * @param errorType The error type
     */
    public ServerError(ErrorType errorType) {
        this.errorType = errorType;
    }
    
    /**
     * Constructor
     * @param errorType The error Type in String form  
     */
    public ServerError(String errorType) {
        try {
            this.errorType = ErrorType.valueOf(errorType);
        } catch (Exception ex) {
            this.errorType = ErrorType.UNKNOWN;
        }        
    }
    

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return SERVER_ERROR_DATA_TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Server Error: \n");
        sb.append(errorType.name());
        sb.append("("); sb.append(errorDescription); sb.append(")");
        return sb.toString();
    }

    /**
     * Convert current error to ServiceStatus type.
     * 
     * @return ServiceStatus type matching current error type, return
     *         ServiceStatus.ERROR_UNKNOWN if current type is null.
     */
    public ServiceStatus toServiceStatus() {
        switch (errorType) {
            case REQUEST_TIMEOUT:
            case HTTP_TIMEOUT:
                return ServiceStatus.ERROR_COMMS_TIMEOUT;

            case AUTH_USER_NOT_FOUND:
                return ServiceStatus.ERROR_USER_NOT_FOUND;

            case AUTH_INVALID_CREDENTIALS:
                return ServiceStatus.ERROR_INVALID_PASSWORD;

            case INVALID_AUTHENTICATION:
                return ServiceStatus.ERROR_AUTHENTICATION_FAILED;

            case INVALID_PARAMETER:
                return ServiceStatus.ERROR_BAD_SERVER_PARAMETER;

            case INVALID_SESSION:
                return ServiceStatus.ERROR_INVALID_SESSION;

            case DATEOFBIRTHINVALID:
                return ServiceStatus.ERROR_DATE_OF_BIRTH_INVALID;

            case PASSWORDINVALID:
                return ServiceStatus.ERROR_INVALID_PASSWORD;

            case INVALIDUSERNAME:
                return ServiceStatus.ERROR_USERNAME_INVALID;

            case USERNAMEINUSE:
                return ServiceStatus.ERROR_USERNAME_IN_USE;

            case USERNAMEMISSING:
                return ServiceStatus.ERROR_USERNAME_MISSING;

            case USERNAMEBLACKLISTED:
                return ServiceStatus.ERROR_USERNAME_BLACKLISTED;

            case USERNAMEFORBIDDEN:
                return ServiceStatus.ERROR_USERNAME_FORBIDDEN;

            case FULLNAMEMISSING:
                return ServiceStatus.ERROR_FULLNAME_MISSING;

            case PASSWORDMISSING:
                return ServiceStatus.ERROR_PASSWORD_MISSING;

            case ACCEPTTCMISSING:
                return ServiceStatus.ERROR_ACCEPT_TC_MISSING;

            case EMAILMISSING:
                return ServiceStatus.ERROR_EMAIL_MISSING;

            case COUNTRYINVALID:
                return ServiceStatus.ERROR_COUNTRY_INVALID;

            case MSISDNMISSING:
                return ServiceStatus.ERROR_MSISDN_MISSING;

            case MSISDNINVALID:
            case MSISDNDIALCODEINVALID:
                return ServiceStatus.ERROR_MSISDN_INVALID;

            case TIMEZONEMISSING:
                return ServiceStatus.ERROR_TIMEZONE_MISSING;

            case TIMEZONEINVALID:
                return ServiceStatus.ERROR_TIMEZONE_INVALID;

            case MOBILEOPERATORINVALID:
                return ServiceStatus.ERROR_MOBILE_OPERATOR_INVALID;

            case MOBILEMODELINVALID:
                return ServiceStatus.ERROR_MOBILE_MODEL_INVALID;

            case LANGUAGEINVALID:
                return ServiceStatus.ERROR_LANGUAGE_INVALID;

            case INTERNALERROR:
            case INTERNAL_ERROR:
                return ServiceStatus.ERROR_INTERNAL_SERVER_ERROR;

            case INVALIDCODE:
                return ServiceStatus.ERROR_INVALID_CODE;

            case INVALID_KEY:
                /**
                 * When the public key changes on server, it has to be reloaded.
                 */
                return ServiceStatus.ERROR_INVALID_PUBLIC_KEY;
        }
        return ServiceStatus.ERROR_UNKNOWN;
    }
}
