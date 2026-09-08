package com.documenttimestamp.timestamping;

import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * Produces and checks RSA signatures over the document-plus-timestamp hash.
 *
 * This uses java.security.Signature with SHA512withRSA rather than running the hash
 * through a Cipher with the private key. Signature applies the padding and digest
 * rules a signature scheme is supposed to apply, and it is the interoperable form:
 * any library on any platform can verify the result.
 */
@Component
public class CipherUtility {
    public static final String SIGNATURE_ALGORITHM = "SHA512withRSA";

    private final SecureKeysManager secureKeysManager;

    public CipherUtility(SecureKeysManager secureKeysManager) {
        this.secureKeysManager = secureKeysManager;
    }

    public byte[] signDocumentHash(byte[] messageHash) throws Exception {
        PrivateKey privateKey = secureKeysManager.getPrivateKey();

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(messageHash);
        return signature.sign();
    }

    public boolean verifyDocumentHash(byte[] messageHash, byte[] signatureBytes) throws Exception {
        PublicKey publicKey = secureKeysManager.getPublicKey();

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(messageHash);
        return signature.verify(signatureBytes);
    }
}
