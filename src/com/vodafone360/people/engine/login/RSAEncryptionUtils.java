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

package com.vodafone360.people.engine.login;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidParameterException;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import com.vodafone360.people.datatypes.PublicKeyDetails;

/**
 * Utility class for encrypting/decrypting login and registration user
 * credentials.
 */
public class RSAEncryptionUtils {

    /**
     * Default public key which can be used to encrypt data. The server has the
     * suitable private key for decrypting the data. This is the modulo part.
     */
    private static final byte[] DEFAULT_PUBKEY_MODULO = new byte[] {
            0, -93, -23, 8, -55, 66, -20, 39, 17, 40, -86, 22, -120, -78, -13, 24, -35, 79, -49,
            -64, -104, -31, -125, -3, 39, 2, 88, 41, -70, 47, -41, -99, -15, 40, -105, -27, -95,
            -99, 99, -32, 44, 4, -18, -40, -5, -87, 82, -33, 40, -4, 122, 15, 7, 3, -73, -84, 13,
            32, -115, 41, 51, 27, 101, 95, -120, 65, 16, 16, 40, 57, -64, -12, 8, 17, 80, -80, 21,
            98, 97, 40, -100, 114, 90, 121, 24, -11, -47, -33, 85, -16, 67, -2, -87, -18, -59, 24,
            -83, 127, -123, -99, -39, -35, 111, -90, 27, -9, -64, -111, -116, -71, -82, 5, -116,
            73, -38, 117, -39, -113, -16, -115, 37, 5, -128, 68, 108, -106, 82, -44, 9
    };

    /**
     * Exponential part of the public key.
     */
    private static final byte[] DEFAULT_PUBKEY_EXPONENTIAL = new byte[] {
            1, 0, 1
    };

    /**
     * Fetches the default public key.
     * 
     * @return Public key
     */
    protected static RSAKeyParameters getDefaultPublicKey() {
        return new RSAKeyParameters(false, new BigInteger(DEFAULT_PUBKEY_MODULO), new BigInteger(
                DEFAULT_PUBKEY_EXPONENTIAL));
    }

    /**
     * Copies the default key into the given parameter.
     * 
     * @param key Where the key should be copied.
     */
    public static void copyDefaultPublicKey(PublicKeyDetails key) {
        key.mExponential = DEFAULT_PUBKEY_EXPONENTIAL;
        key.mModulus = DEFAULT_PUBKEY_MODULO;
    }

    /**
     * Composes a RSA Public Key from its components.
     * 
     * @param mod the RSA modulo.
     * @param exp the RSA exponent.
     * @return the RSA public key.
     */
    protected static RSAKeyParameters getRSAPubKey(final byte[] mod, final byte[] exp) {
        return new RSAKeyParameters(false, new BigInteger(mod), new BigInteger(exp));
    }

    /**
     * Encrypts bytes with the given RSA Public Key.
     * 
     * @param pubKey the RSA Public Key.
     * @param data the data to encrypt.
     * @return the encrypted data.
     * @throws InvalidParameterException
     * @throws InvalidCipherTextException
     */
    protected static byte[] encryptRSA(final RSAKeyParameters pubKey, final String data)
            throws InvalidCipherTextException {
        
        if (data == null) {
            throw new InvalidParameterException("RSAEncryptionUtils.encryptRSA() "
                    + "data cannot be NULL");
        }
        try {
            return rsa(true, pubKey, data.getBytes("utf-8"));
        } catch (final UnsupportedEncodingException e) {
            return rsa(true, pubKey,data.getBytes());
        }
    }

    /**
     * Ensures data length is always a multiple of 16 (by rounding up if
     * necessary).
     */
    private static final int ROUND_UP_VALUE = 16;

    /**
     * Encrypts or Decrypts bytes with the given RSA Public or Private Key.
     * 
     * @param encrypt true for encrypt, false for decrypt.
     * @param key the RSA Public or Private Key.
     * @param data the data to encrypt or decrypt.
     * @return the encrypted or decrypted data.
     */
    private static byte[] rsa(final boolean encrypt, final RSAKeyParameters key, final byte[] data)
            throws InvalidCipherTextException {
        final byte[] dataAligned = new byte[roundUp(data.length, ROUND_UP_VALUE)];
        System.arraycopy(data, 0, dataAligned, 0, data.length);
        final RSAEngine rsa = new RSAEngine();
        final AsymmetricBlockCipher pkcs1 = new PKCS1Encoding(rsa);
        pkcs1.init(encrypt, key);
        if (encrypt)
            return pkcs1.processBlock(dataAligned, 0, dataAligned.length);
        return trimZeros(pkcs1.processBlock(dataAligned, 0, dataAligned.length));
    }

    /**
     * Return the value of v rounded up to a multiple of t
     * 
     * @param v The value to round up
     * @param t The multiple to use
     * @return The rounded result
     */
    private static int roundUp(final int v, final int t) {
        if (v % t == 0)
            return v;
        return (v / ROUND_UP_VALUE + 1) * ROUND_UP_VALUE;
    }

    /**
     * Removes all the zeros from the end of the given array.
     * 
     * @param data Initial array to trim
     * @return New array which n less elements (where n = number of trailing
     *         zeros). If the given array is all zeros then the function will
     *         fail and the result will be the initial array (NULL if initial 
     *         array is NULL)
     */
    private static byte[] trimZeros(final byte[] data) {
        if (data == null)
            return null;
        if (data.length > 0 && data[data.length - 1] == 0) {
            byte[] result;
            for (int i = data.length - 1; i >= 0; i--)
                if (data[i] != 0) {
                    result = new byte[i + 1];
                    System.arraycopy(data, 0, result, 0, result.length);
                    return result;
                }
        }
        return data;
    }
}
