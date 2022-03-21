package com.documenttimestamp.timestamping;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class SecureKeysManager {
    private static final String password = "securepassword";

    public static PublicKey getPublicKey() throws Exception {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream receiverKeystoreInputStream = classloader.getResourceAsStream("cipher/receiver_keystore.p12");

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(receiverKeystoreInputStream, password.toCharArray());
        Certificate certificate = keyStore.getCertificate("receiverKeyPair");
        return certificate.getPublicKey();
    }

    public static PrivateKey getPrivateKey() throws Exception {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream senderKeystoreInputStream = classloader.getResourceAsStream("cipher/sender_keystore.p12");

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(senderKeystoreInputStream, password.toCharArray());
        return (PrivateKey) keyStore.getKey("senderKeyPair", password.toCharArray());
    }
}
