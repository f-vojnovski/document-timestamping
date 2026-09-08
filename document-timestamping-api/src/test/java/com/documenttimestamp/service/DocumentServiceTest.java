package com.documenttimestamp.service;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.repository.DocumentRepository;
import com.documenttimestamp.timestamping.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the whole pipeline the way the controller does, then verifies the result
 * the way the standalone client does, with no server and no database involved.
 */
class DocumentServiceTest {
    private KeyPair keyPair;
    private DocumentService service;
    private CipherUtility cipherUtility;

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

    /** Minimal in-memory stand-in so the test needs no database. */
    private static class InMemoryRepository implements java.lang.reflect.InvocationHandler {
        final List<Document> saved = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            switch (method.getName()) {
                case "save":
                    saved.add((Document) args[0]);
                    return args[0];
                case "findFirstByDocumentChecksum":
                    return saved.stream()
                            .filter(d -> d.getDocumentChecksum().equals(args[0]))
                            .findFirst();
                case "findAll":
                    return saved;
                default:
                    return null;
            }
        }
    }

    private InMemoryRepository repositoryState;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        SecureKeysManager keys = new InMemoryKeys(keyPair);
        cipherUtility = new CipherUtility(keys);
        repositoryState = new InMemoryRepository();
        DocumentRepository repository = (DocumentRepository) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{DocumentRepository.class}, repositoryState);

        service = new DocumentService(repository, cipherUtility, keys);
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "doc.txt", "text/plain", content.getBytes());
    }

    @Test
    void storesAProofThatVerifiesWithThePublicKeyAlone() throws Exception {
        Document d = service.hashAndStoreDocument("Ducks research paper", file("hello world"));

        assertNotNull(d.getEncryptedHash());
        assertEquals(128, d.getDocumentChecksum().length(), "SHA-512 is 64 bytes, 128 hex chars");
        assertEquals(128, d.getTargetHash().length());
        assertEquals(CipherUtility.SIGNATURE_ALGORITHM, d.getSignatureAlgorithm());
        assertEquals(DocumentService.HASHING_ALGORITHM, d.getHashingAlgorithm());

        // What the standalone verifier does: check the signature over targetHash.
        assertTrue(cipherUtility.verifyDocumentHash(
                BytesHexConverter.hexStringToByteArray(d.getTargetHash()),
                BytesHexConverter.hexStringToByteArray(d.getEncryptedHash())));
    }

    @Test
    void targetHashIsReproducibleOfflineFromTheDocumentAndTimestamp() throws Exception {
        Document d = service.hashAndStoreDocument("Ducks research paper", file("hello world"));

        // Recompute exactly as ChecksumGenerator does on the client side.
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] checksum = digest.digest("hello world".getBytes());
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        buffer.write(checksum);
        buffer.write(ByteBuffer.allocate(Long.BYTES).putLong(d.getTimestamp()).array());
        byte[] recomputed = MessageDigest.getInstance("SHA-512").digest(buffer.toByteArray());

        assertEquals(d.getDocumentChecksum(), BytesHexConverter.bytesToHex(checksum));
        assertEquals(d.getTargetHash(), BytesHexConverter.bytesToHex(recomputed));
    }

    @Test
    void aModifiedDocumentNoLongerMatchesTheProof() throws Exception {
        Document d = service.hashAndStoreDocument("Ducks research paper", file("hello world"));

        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] tamperedChecksum = digest.digest("hello w0rld".getBytes());

        assertNotEquals(d.getDocumentChecksum(), BytesHexConverter.bytesToHex(tamperedChecksum));
    }

    @Test
    void verifyFindsAPreviouslyTimestampedDocument() throws Exception {
        service.hashAndStoreDocument("Ducks research paper", file("hello world"));
        Document found = service.verifyDocument(file("hello world"));
        assertEquals("Ducks research paper", found.getTitle());
        assertNotNull(found.getPublicKey());
    }

    @Test
    void verifyRejectsAnUnknownDocument() {
        assertThrows(DocumentNotFoundException.class,
                () -> service.verifyDocument(file("never uploaded")));
    }
}
