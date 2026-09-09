package com.documenttimestamp;

import com.documenttimestamp.timestamping.SecureKeysManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {

    private static final String UPLOAD = "/api/v1/documents/";
    private static final String VERIFY = "/api/v1/documents/verify";

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper json = new ObjectMapper();

    @TestConfiguration
    static class InMemoryKeys {
        @Bean
        @Primary
        SecureKeysManager inMemorySecureKeysManager() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            final KeyPair keyPair = generator.generateKeyPair();

            return new SecureKeysManager(new DefaultResourceLoader(),
                    "unused", "unused", "PKCS12", "unused",
                    "unused", "unused", "PKCS12", "unused") {
                @Override
                public PublicKey getPublicKey() {
                    return keyPair.getPublic();
                }

                @Override
                public PrivateKey getPrivateKey() {
                    return keyPair.getPrivate();
                }
            };
        }
    }

    private MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "doc.txt", "text/plain", content);
    }

    private MockMultipartFile file(String content) {
        return file(content.getBytes());
    }

    private String uniqueContent(String label) {
        return label + "-" + System.nanoTime();
    }

    private Map<String, Object> upload(String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart(UPLOAD)
                        .file(file(content))
                        .param("title", title))
                .andReturn();
        assertEquals(201, result.getResponse().getStatus(),
                "upload should answer 201 Created");
        return read(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> read(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), Map.class);
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }


    @Test
    void uploadReturnsEveryDocumentedProofField() throws Exception {
        Map<String, Object> body = upload("Ducks research paper", uniqueContent("fields"));

        for (String field : new String[]{"id", "title", "encryptedHash", "documentChecksum",
                "targetHash", "timestamp", "publicKey", "signatureAlgorithm", "hashingAlgorithm"}) {
            assertNotNull(body.get(field), "response is missing the documented field '" + field + "'");
        }

        assertEquals("Ducks research paper", body.get("title"));
        assertEquals("SHA512withRSA", body.get("signatureAlgorithm"));
        assertEquals("SHA-512", body.get("hashingAlgorithm"));
        assertEquals(128, ((String) body.get("documentChecksum")).length(),
                "SHA-512 is 64 bytes, so 128 hex characters");
        assertEquals(128, ((String) body.get("targetHash")).length());
        assertEquals(512, ((String) body.get("encryptedHash")).length(),
                "a 2048-bit RSA signature is 256 bytes, so 512 hex characters");
        assertTrue(((Number) body.get("timestamp")).longValue() > 0L);
    }

    @Test
    void theProofVerifiesUnderThePublicKeyFromTheSameResponse() throws Exception {
        Map<String, Object> body = upload("Verifiable", uniqueContent("verifiable"));

        PublicKey publicKey = java.security.KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode((String) body.get("publicKey"))));

        Signature signature = Signature.getInstance((String) body.get("signatureAlgorithm"));
        signature.initVerify(publicKey);
        signature.update(hexToBytes((String) body.get("targetHash")));

        assertTrue(signature.verify(hexToBytes((String) body.get("encryptedHash"))),
                "the returned publicKey must verify the returned encryptedHash");
    }

    @Test
    void targetHashReproducesOfflineFromTheDocumentAndTimestamp() throws Exception {
        String content = uniqueContent("offline");
        Map<String, Object> body = upload("Reproducible", content);

        // Recomputed the way the standalone ChecksumGenerator does.
        byte[] checksum = MessageDigest.getInstance("SHA-512").digest(content.getBytes());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(checksum);
        buffer.write(ByteBuffer.allocate(Long.BYTES)
                .putLong(((Number) body.get("timestamp")).longValue()).array());
        byte[] targetHash = MessageDigest.getInstance("SHA-512").digest(buffer.toByteArray());

        assertEquals(body.get("documentChecksum"), toHex(checksum));
        assertEquals(body.get("targetHash"), toHex(targetHash),
                "targetHash must stay reproducible offline, or every generated verifier breaks");
    }

    @Test
    void aBlankTitleIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(multipart(UPLOAD)
                        .file(file(uniqueContent("blank-title")))
                        .param("title", "   "))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertDocumentedErrorBody(read(result), 400);
    }

    @Test
    void anEmptyFileIsRejectedByBothEndpoints() throws Exception {
        MvcResult upload = mockMvc.perform(multipart(UPLOAD)
                        .file(file(new byte[0]))
                        .param("title", "Empty"))
                .andReturn();
        assertEquals(400, upload.getResponse().getStatus());
        assertDocumentedErrorBody(read(upload), 400);

        MvcResult verify = mockMvc.perform(multipart(VERIFY)
                        .file(file(new byte[0])))
                .andReturn();
        assertEquals(400, verify.getResponse().getStatus());
        assertDocumentedErrorBody(read(verify), 400);
    }

    // Literals, not TITLE_MAX_LENGTH: reading the constant would keep this green if it changed.
    @Test
    void aTitleTooLongForTheColumnIsRejectedInsteadOfFailingTheInsert() throws Exception {
        String longest = new String(new char[255]).replace('\0', 'a');
        MvcResult accepted = mockMvc.perform(multipart(UPLOAD)
                        .file(file(uniqueContent("title-255")))
                        .param("title", longest))
                .andReturn();
        assertEquals(201, accepted.getResponse().getStatus(),
                "a 255-character title fits the column and must be accepted");

        String tooLong = new String(new char[256]).replace('\0', 'a');
        MvcResult refused = mockMvc.perform(multipart(UPLOAD)
                        .file(file(uniqueContent("title-256")))
                        .param("title", tooLong))
                .andReturn();
        assertEquals(400, refused.getResponse().getStatus(),
                "a 256-character title must be refused up front, not surface as a failed insert");
        assertDocumentedErrorBody(read(refused), 400);
    }

    @Test
    void aMissingMultipartPartIsAClientError() throws Exception {
        MvcResult noFile = mockMvc.perform(multipart(UPLOAD).param("title", "No file")).andReturn();
        assertEquals(400, noFile.getResponse().getStatus());
        assertDocumentedErrorBody(read(noFile), 400);

        MvcResult noTitle = mockMvc.perform(multipart(UPLOAD)
                        .file(file(uniqueContent("no-title"))))
                .andReturn();
        assertEquals(400, noTitle.getResponse().getStatus());
        assertDocumentedErrorBody(read(noTitle), 400);
    }

    // 400 not 415: the multipart params fail to resolve before content-type negotiation.
    @Test
    void aJsonBodyIsAClientErrorNotAServerError() throws Exception {
        MvcResult result = mockMvc.perform(post(UPLOAD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
        assertDocumentedErrorBody(read(result), 400);
    }

    @Test
    void theWrongHttpMethodIsRejectedWithTheDocumentedBody() throws Exception {
        for (String path : new String[]{UPLOAD, VERIFY}) {
            MvcResult result = mockMvc.perform(get(path)).andReturn();
            assertEquals(405, result.getResponse().getStatus(),
                    "GET " + path + " must answer 405, not a server error");
            assertDocumentedErrorBody(read(result), 405);
        }
    }


    @Test
    void verifyFindsAPreviouslyTimestampedDocument() throws Exception {
        String content = uniqueContent("round-trip");
        Map<String, Object> uploaded = upload("Round trip", content);

        MvcResult result = mockMvc.perform(multipart(VERIFY).file(file(content))).andReturn();
        assertEquals(200, result.getResponse().getStatus());

        Map<String, Object> found = read(result);
        assertEquals(uploaded.get("documentChecksum"), found.get("documentChecksum"));
        assertEquals(uploaded.get("targetHash"), found.get("targetHash"));
        assertEquals(uploaded.get("timestamp"), found.get("timestamp"));
        assertNotNull(found.get("publicKey"), "verify must attach the public key too");
    }

    @Test
    void verifyReturns404WithTheDocumentedErrorBodyForAnUnknownDocument() throws Exception {
        MvcResult result = mockMvc.perform(multipart(VERIFY)
                        .file(file(uniqueContent("never-uploaded"))))
                .andReturn();

        assertEquals(404, result.getResponse().getStatus());
        assertDocumentedErrorBody(read(result), 404);
    }


    @Test
    void theConfiguredOriginIsAllowedAndOthersAreRefused() throws Exception {
        MvcResult allowed = mockMvc.perform(options(UPLOAD)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn();
        assertEquals(200, allowed.getResponse().getStatus(),
                "the configured origin must survive preflight");
        assertEquals(ALLOWED_ORIGIN,
                allowed.getResponse().getHeader("Access-Control-Allow-Origin"));

        MvcResult refused = mockMvc.perform(options(UPLOAD)
                        .header("Origin", "https://not-configured.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andReturn();
        assertEquals(403, refused.getResponse().getStatus(),
                "an unlisted origin must be refused");
        assertNull(refused.getResponse().getHeader("Access-Control-Allow-Origin"),
                "no Allow-Origin header may be sent to an unlisted origin");
    }

    @Test
    void anAllowedOriginGetsTheHeaderOnARealUpload() throws Exception {
        MvcResult result = mockMvc.perform(multipart(UPLOAD)
                        .file(file(uniqueContent("cors-post")))
                        .param("title", "Cross origin")
                        .header("Origin", ALLOWED_ORIGIN))
                .andReturn();

        assertEquals(201, result.getResponse().getStatus());
        assertEquals(ALLOWED_ORIGIN, result.getResponse().getHeader("Access-Control-Allow-Origin"));
    }


    private void assertDocumentedErrorBody(Map<String, Object> body, int expectedStatus) {
        assertEquals(expectedStatus, ((Number) body.get("status")).intValue());
        assertNotNull(body.get("error"), "error responses must carry an 'error' field");
        Object message = body.get("message");
        assertNotNull(message, "error responses must carry a 'message' field");
        assertTrue(!((String) message).isBlank(), "the 'message' field must not be blank");
    }

    @Test
    void jsonUsesTheDocumentedContentType() throws Exception {
        MvcResult result = mockMvc.perform(multipart(UPLOAD)
                        .file(file(uniqueContent("content-type")))
                        .param("title", "Content type"))
                .andReturn();

        assertTrue(MediaType.parseMediaType(result.getResponse().getContentType())
                        .isCompatibleWith(MediaType.APPLICATION_JSON),
                "the proof must be served as JSON");
    }
}
