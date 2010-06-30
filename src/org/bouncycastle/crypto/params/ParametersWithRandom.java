
package org.bouncycastle.crypto.params;

import org.bouncycastle.crypto.CipherParameters;
import java.security.SecureRandom;

public class ParametersWithRandom implements CipherParameters {
    private SecureRandom random;

    private CipherParameters parameters;

    public SecureRandom getRandom() {
        return random;
    }

    public CipherParameters getParameters() {
        return parameters;
    }
}
