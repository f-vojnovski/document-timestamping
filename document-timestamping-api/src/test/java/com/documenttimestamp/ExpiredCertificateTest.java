package com.documenttimestamp;

import com.documenttimestamp.repository.DocumentRepository;
import com.documenttimestamp.timestamping.SecureKeysManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
@AutoConfigureMockMvc
class ExpiredCertificateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @TestConfiguration
    static class ExpiredKeys {
        @Bean
        @Primary
        SecureKeysManager expiredSecureKeysManager() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            final KeyPair keyPair = generator.generateKeyPair();

            return new SecureKeysManager(new DefaultResourceLoader(),
                    "unused", "unused", "PKCS12", "unused",
                    "unused", "unused", "PKCS12", "unused") {
                @Override
                public PublicKey getPublicKey() throws Exception {
                    throw new CertificateExpiredException("NotAfter: Wed Aug 06 07:52:37 CEST 2025");
                }

                @Override
                public PrivateKey getPrivateKey() {
                    return keyPair.getPrivate();
                }
            };
        }
    }

    @Test
    void anExpiredCertificateIssuesNoProofAndStoresNothing() throws Exception {
        long before = documentRepository.count();

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/")
                        .file(new MockMultipartFile("file", "doc.txt", "text/plain", "expired".getBytes()))
                        .param("title", "Signed under an expired certificate"))
                .andReturn();

        assertEquals(500, result.getResponse().getStatus(),
                "an expired certificate must fail the request");
        assertEquals(before, documentRepository.count(),
                "nothing may be signed or persisted once the certificate is known to be expired");
    }
}
