package com.documenttimestamp.timestamping;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class CipherUtility {
    private static final String TRANSFORMATION = "RSA";

    private final SecureKeysManager secureKeysManager;

    public CipherUtility(SecureKeysManager secureKeysManager) {
        this.secureKeysManager = secureKeysManager;
    }

    public byte[] signDocumentHash(byte[] messageHash) throws Exception {
        PrivateKey privateKey = secureKeysManager.getPrivateKey();

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return cipher.doFinal(messageHash);
    }

    public byte[] getDecryptedDocumentHash(byte[] encryptedMessageHash) throws Exception {
        PublicKey publicKey = secureKeysManager.getPublicKey();

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(encryptedMessageHash);
    }
}
