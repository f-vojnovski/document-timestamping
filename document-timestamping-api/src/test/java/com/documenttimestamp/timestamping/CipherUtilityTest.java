package com.documenttimestamp.timestamping;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class CipherUtilityTest {
    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;
    private static CipherUtility cipherUtility;

    /** Serves an in-memory key pair so the test needs no keystore on disk. */
    private static class InMemoryKeys extends SecureKeysManager {
        private final KeyPair keyPair;

        InMemoryKeys(KeyPair keyPair) {
            super(new DefaultResourceLoader(), "unused", "unused", "PKCS12", "unused",
                    "unused", "unused", "PKCS12", "unused");
            this.keyPair = keyPair;
        }

        @Override
        public PublicKey getPublicKey() {
            return keyPair.getPublic();
        }

        @Override
        public PrivateKey getPrivateKey() {
            return keyPair.getPrivate();
        }
    }

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();
        cipherUtility = new CipherUtility(new InMemoryKeys(keyPair));
    }

    private byte[] hash(byte fill) {
        byte[] h = new byte[64];
        java.util.Arrays.fill(h, fill);
        return h;
    }

    @Test
    void signatureVerifiesAgainstTheMatchingPublicKey() throws Exception {
        byte[] target = hash((byte) 0x11);
        byte[] signature = cipherUtility.signDocumentHash(target);
        assertTrue(cipherUtility.verifyDocumentHash(target, signature));
    }

    @Test
    void aTamperedHashFailsVerification() throws Exception {
        byte[] target = hash((byte) 0x11);
        byte[] signature = cipherUtility.signDocumentHash(target);
        byte[] tampered = hash((byte) 0x11);
        tampered[0] ^= 0x01;
        assertFalse(cipherUtility.verifyDocumentHash(tampered, signature));
    }

    @Test
    void aSignatureFromAnotherKeyIsRejected() throws Exception {
        byte[] target = hash((byte) 0x22);
        CipherUtility impostor = new CipherUtility(new InMemoryKeys(otherKeyPair));
        byte[] forged = impostor.signDocumentHash(target);
        assertFalse(cipherUtility.verifyDocumentHash(target, forged));
    }

    @Test
    void producesA256ByteSignatureForA2048BitKey() throws Exception {
        assertEquals(256, cipherUtility.signDocumentHash(hash((byte) 0x33)).length);
    }

    @Test
    void encodedPublicKeyRoundTripsThroughBase64() throws Exception {
        String encoded = new InMemoryKeys(keyPair).getEncodedPublicKey();
        byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
        assertArrayEquals(keyPair.getPublic().getEncoded(), decoded);
    }
}
