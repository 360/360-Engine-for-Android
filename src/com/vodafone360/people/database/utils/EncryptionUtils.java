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

package com.vodafone360.people.database.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.vodafone360.people.utils.LogUtils;

/**
 * EncryptionUtils... This is a utility class for credentials encrypting in
 * database
 * 
 * @author -
 */

public class EncryptionUtils {
    /**
     * the Cipher
     */
    private static Cipher sCipher;

    /**
     * secret key
     */
    private static SecretKeySpec sKeySpec;

    /**
     * error definintions
     */
    private static final String ERROR_INVALIDKEY = "EncryptionUtils.encryptPassword() InvalidKeyException";

    /**
     * error definintions
     */
    private static final String ERROR_ILLEGALBLOCK = "EncryptionUtils.encryptPassword() IllegalBlockSizeException";

    /**
     * error definintions
     */
    private static final String ERROR_BADPADDING = "EncryptionUtils.encryptPassword() BadPaddingException";

    /**
     * error definintions
     */
    private static final String ERROR_ILLEGALSTATEEXCEPTION = "EncryptionUtils.encryptPassword() IllegalStateException";

    /**
     * the hard coded key
     */
    private static final byte[] KEY_BYTES = {
            82, 45, 44, 77, -100, 44, -120, 42, 72, 5, 29, 127, -124, 22, 21, 101, 22, 33, 44, 55,
            66, 77, 88, 99, 102, -103, -44, -22, -11, -5, 10, 22,
    };

    /**
     * Encrypting the password.
     * 
     * @param password string password
     * @return byte array containing encrypted password, return NULL if supplied
     *         password Strind is NULL, or an exception is thrown.
     * @throws BadPaddingException.
     * @throws IllegalBlockSizeException.
     * @throws InvalidKeyException.
     */
    public synchronized static byte[] encryptPassword(String password) {
        if (!createCipher()) {
            return null;
        }
        try {
            if (password == null) {
                return null;
            }
            sCipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
            return sCipher.doFinal(password.getBytes());
        } catch (InvalidKeyException e) {
            LogUtils.logE(ERROR_INVALIDKEY, e);
            return null;
        } catch (IllegalBlockSizeException e) {
            LogUtils.logE(ERROR_ILLEGALBLOCK, e);
            return null;
        } catch (BadPaddingException e) {
            LogUtils.logE(ERROR_BADPADDING, e);
            return null;
        } catch (IllegalStateException e) {
            LogUtils.logE(ERROR_ILLEGALSTATEEXCEPTION, e);
            return null;
        }
    }

    /**
     * Decryption of the password.
     * 
     * @param passwordEn password in bytes
     * @return string
     */
    public synchronized static String decryptPassword(byte[] passwordEn) {
        if (!createCipher()) {
            return null;
        }
        try {
            if (passwordEn == null) {
                return null;
            }
            sCipher.init(Cipher.DECRYPT_MODE, sKeySpec);
            return new String(sCipher.doFinal(passwordEn));
        } catch (InvalidKeyException e) {
            LogUtils.logE(ERROR_INVALIDKEY, e);
            return null;
        } catch (IllegalBlockSizeException e) {
            LogUtils.logE(ERROR_ILLEGALBLOCK, e);
            return null;
        } catch (BadPaddingException e) {
            LogUtils.logE(ERROR_BADPADDING, e);
            return null;
        } catch (IllegalStateException e) {
            LogUtils.logE(ERROR_ILLEGALSTATEEXCEPTION, e);
            return null;
        }
    }

    /**
     * Creation of Cipher
     * 
     * @return true if cipher created otherwise false
     */
    private static boolean createCipher() {
        if (sCipher != null) {
            return true;
        }
        try {
            final String standard = "AES";
            sCipher = Cipher.getInstance(standard);
            sKeySpec = new SecretKeySpec(KEY_BYTES, standard);
        } catch (NoSuchAlgorithmException e) {
            LogUtils.logE("EncryptionUtils.encryptPassword() Create Cipher failed", e);
            return false;
        } catch (NoSuchPaddingException e) {
            LogUtils.logE("EncryptionUtils.encryptPassword() "
                    + "Create Cipher failed - NoSuchPaddingException", e);
            return false;
        }
        return true;
    }

}
