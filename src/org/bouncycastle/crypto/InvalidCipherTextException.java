
package org.bouncycastle.crypto;

/**
 * this exception is thrown whenever we find something we don't expect in a
 * message.
 */
public class InvalidCipherTextException extends CryptoException {
    private static final long serialVersionUID = -1L;

    /**
     * base constructor.
     */
    public InvalidCipherTextException() {
    }

    /**
     * create a InvalidCipherTextException with the given message.
     * 
     * @param message the message to be carried with the exception.
     */
    public InvalidCipherTextException(String message) {
        super(message);
    }
}
