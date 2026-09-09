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
 *
 * The keystore is decrypted from disk once and the extracted key and certificate
 * are cached. Signing a document reads both, so without the cache every request
 * paid for two PKCS#12 loads. requireValidCertificate re-checks the expiry on each
 * call, so an expiry that passes while the process runs is caught.
 *
 * The cache lives for the process, so rotating the keystore on disk takes effect
 * on the next restart rather than immediately. That suits a service that restarts
 * to rotate; a long-running deployment that swaps keys in place would reload the
 * cache on a timer or when the file's timestamp changes.
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

    private volatile PrivateKey cachedPrivateKey;
    private volatile Certificate cachedCertificate;

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
        return loadCertificate().getPublicKey();
    }

    // Not called when retrieving an old proof: a signature made while the certificate was
    // valid stays valid after it expires.
    public void requireValidCertificate() throws Exception {
        Certificate certificate = loadCertificate();
        if (certificate instanceof X509Certificate) {
            ((X509Certificate) certificate).checkValidity();
        }
    }

    public PrivateKey getPrivateKey() throws Exception {
        PrivateKey local = cachedPrivateKey;
        if (local == null) {
            synchronized (this) {
                local = cachedPrivateKey;
                if (local == null) {
                    KeyStore keyStore = loadKeyStore(keystoreLocation, keystoreType, keystorePassword);
                    local = (PrivateKey) keyStore.getKey(keyAlias, keystorePassword.toCharArray());
                    if (local == null) {
                        throw new IllegalStateException(
                                "No private key under alias '" + keyAlias + "' in " + keystoreLocation);
                    }
                    cachedPrivateKey = local;
                }
            }
        }
        return local;
    }

    private Certificate loadCertificate() throws Exception {
        Certificate local = cachedCertificate;
        if (local == null) {
            synchronized (this) {
                local = cachedCertificate;
                if (local == null) {
                    KeyStore keyStore = loadKeyStore(truststoreLocation, truststoreType, truststorePassword);
                    local = keyStore.getCertificate(certificateAlias);
                    if (local == null) {
                        throw new IllegalStateException(
                                "No certificate under alias '" + certificateAlias + "' in " + truststoreLocation);
                    }
                    cachedCertificate = local;
                }
            }
        }
        return local;
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
