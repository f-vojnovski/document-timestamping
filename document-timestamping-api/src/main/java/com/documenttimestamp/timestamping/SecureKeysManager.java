package com.documenttimestamp.timestamping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Loads the signing key and the public certificate.
 *
 * Nothing here is hard coded: every location, alias and password comes from
 * configuration, so a deployment points at its own keystore without a rebuild.
 * Locations accept any Spring resource prefix, which means "classpath:cipher/..."
 * during development and "file:/etc/..." in production.
 */
@Component
public class SecureKeysManager {
    private final ResourceLoader resourceLoader;
    private final String keystoreLocation;
    private final String keystorePassword;
    private final String keystoreType;
    private final String keyAlias;
    private final String truststoreLocation;
    private final String truststorePassword;
    private final String truststoreType;
    private final String certificateAlias;

    public SecureKeysManager(
            ResourceLoader resourceLoader,
            @Value("${timestamping.keystore.location}") String keystoreLocation,
            @Value("${timestamping.keystore.password}") String keystorePassword,
            @Value("${timestamping.keystore.type:PKCS12}") String keystoreType,
            @Value("${timestamping.keystore.key-alias}") String keyAlias,
            @Value("${timestamping.truststore.location}") String truststoreLocation,
            @Value("${timestamping.truststore.password}") String truststorePassword,
            @Value("${timestamping.truststore.type:PKCS12}") String truststoreType,
            @Value("${timestamping.truststore.certificate-alias}") String certificateAlias) {
        this.resourceLoader = resourceLoader;
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.keyAlias = keyAlias;
        this.truststoreLocation = truststoreLocation;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.certificateAlias = certificateAlias;
    }

    public PublicKey getPublicKey() throws Exception {
        KeyStore keyStore = loadKeyStore(truststoreLocation, truststoreType, truststorePassword);
        Certificate certificate = keyStore.getCertificate(certificateAlias);
        if (certificate == null) {
            throw new IllegalStateException(
                    "No certificate under alias '" + certificateAlias + "' in " + truststoreLocation);
        }
        if (certificate instanceof X509Certificate) {
            // A timestamp signed by an expired certificate is not worth issuing, so this
            // is checked on the way out rather than left for the client to discover.
            ((X509Certificate) certificate).checkValidity();
        }
        return certificate.getPublicKey();
    }

    public PrivateKey getPrivateKey() throws Exception {
        KeyStore keyStore = loadKeyStore(keystoreLocation, keystoreType, keystorePassword);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keystorePassword.toCharArray());
        if (privateKey == null) {
            throw new IllegalStateException(
                    "No private key under alias '" + keyAlias + "' in " + keystoreLocation);
        }
        return privateKey;
    }

    /**
     * The public key in the Base64 X.509 form the client verifier expects. Handing
     * the client a value read from the certificate, rather than a constant compiled
     * into the server, is what lets anyone swap in their own keystore and still have
     * the generated driver code verify.
     */
    public String getEncodedPublicKey() throws Exception {
        return Base64.getEncoder().encodeToString(getPublicKey().getEncoded());
    }

    private KeyStore loadKeyStore(String location, String type, String password) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Keystore not found at " + location
                    + ". Generate one with keytool, see the README.");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(inputStream, password.toCharArray());
            return keyStore;
        }
    }
}
