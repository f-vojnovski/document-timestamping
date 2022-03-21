package com.documenttimestamp.timestamping;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

public final class CipherUtility {
    private static final String password = "securepassword";

    public static byte[] signDocumentHash(byte[] messageHash) throws Exception{
        PrivateKey privateKey = SecureKeysManager.getPrivateKey();

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return cipher.doFinal(messageHash);
    }

    public static byte[] getDecryptedDocumentHash(byte[] encryptedMessageHash) throws Exception {
        PublicKey publicKey = SecureKeysManager.getPublicKey();

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(encryptedMessageHash);
    }
}
