package com.documenttimestamp;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.repository.DocumentRepository;
import com.documenttimestamp.timestamping.SecureKeysManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.CertificateExpiredException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

// Runs against a real PKCS#12 whose certificate expired a year ago, so the expiry check
// itself is exercised rather than stubbed.
@SpringBootTest
@AutoConfigureMockMvc
class ExpiredCertificateTest {

    private static final String PASSWORD = "testonly";
    private static Path directory;

    @Autowired private MockMvc mockMvc;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private SecureKeysManager secureKeysManager;

    @DynamicPropertySource
    static void expiredKeystore(DynamicPropertyRegistry registry) throws Exception {
        directory = Files.createTempDirectory("expired-cert-test");
        directory.toFile().deleteOnExit();

        keytool("-genkeypair", "-alias", "senderKeyPair", "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=Expired", "-startdate", "-400d", "-validity", "1",
                "-storetype", "PKCS12", "-keystore", path("sender.p12"), "-storepass", PASSWORD);
        keytool("-exportcert", "-alias", "senderKeyPair", "-keystore", path("sender.p12"),
                "-storepass", PASSWORD, "-file", path("sender.cer"));
        keytool("-importcert", "-alias", "receiverKeyPair", "-keystore", path("receiver.p12"),
                "-storetype", "PKCS12", "-storepass", PASSWORD, "-file", path("sender.cer"), "-noprompt");

        registry.add("timestamping.keystore.location", () -> "file:" + path("sender.p12"));
        registry.add("timestamping.keystore.password", () -> PASSWORD);
        registry.add("timestamping.keystore.key-alias", () -> "senderKeyPair");
        registry.add("timestamping.truststore.location", () -> "file:" + path("receiver.p12"));
        registry.add("timestamping.truststore.password", () -> PASSWORD);
        registry.add("timestamping.truststore.certificate-alias", () -> "receiverKeyPair");
    }

    private static String path(String name) {
        return directory.resolve(name).toString();
    }

    private static void keytool(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        System.arraycopy(arguments, 0, command, 1, arguments.length);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (process.waitFor() != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
    }

    private MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "doc.txt", "text/plain", content.getBytes());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Test
    void theExpiryCheckRejectsTheCertificate() {
        assertThrows(CertificateExpiredException.class,
                () -> secureKeysManager.requireValidCertificate());
    }

    @Test
    void theKeyRemainsReadable() throws Exception {
        assertNotNull(secureKeysManager.getEncodedPublicKey(),
                "an expired certificate still holds the key that verifies proofs made before it expired");
    }

    @Test
    void noTimestampIsIssuedAndNothingIsStored() throws Exception {
        long before = documentRepository.count();

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/")
                        .file(file("expired"))
                        .param("title", "Signed under an expired certificate"))
                .andReturn();

        assertEquals(500, result.getResponse().getStatus());
        assertEquals(before, documentRepository.count(),
                "nothing may be signed or persisted once the certificate is known to be expired");
    }

    @Test
    void verifyStillReturnsADocumentAlreadyOnRecord() throws Exception {
        String content = "stored-while-the-certificate-was-valid";
        Document row = new Document();
        row.setTitle("Earlier proof");
        row.setDocumentChecksum(toHex(MessageDigest.getInstance("SHA-512").digest(content.getBytes())));
        row.setTargetHash("AA");
        row.setEncryptedHash("BB");
        row.setTimestamp(1_700_000_000_000L);
        documentRepository.save(row);

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/verify")
                        .file(file(content)))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
                "an expired certificate must not break recovery of a proof already issued");
    }
}
