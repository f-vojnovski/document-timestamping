package com.documenttimestamp.service;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.model.ProofResponse;
import com.documenttimestamp.repository.DocumentRepository;
import com.documenttimestamp.timestamping.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Optional;

@Service
public class DocumentService {
    public static final String HASHING_ALGORITHM = "SHA-512";

    private final DocumentRepository documentRepository;
    private final CipherUtility cipherUtility;
    private final SecureKeysManager secureKeysManager;

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
                           CipherUtility cipherUtility,
                           SecureKeysManager secureKeysManager) {
        this.documentRepository = documentRepository;
        this.cipherUtility = cipherUtility;
        this.secureKeysManager = secureKeysManager;
    }

    public ProofResponse hashAndStoreDocument(String title, MultipartFile file) {
        try {
            secureKeysManager.requireValidCertificate();
            String encodedPublicKey = secureKeysManager.getEncodedPublicKey();

            MessageDigest shaDigest = MessageDigest.getInstance(HASHING_ALGORITHM);
            byte[] messageHash = FileChecksumCalculator.getFileChecksum(shaDigest, file);

            Timestamp ts = TimestampingUtility.getCurrentTime();
            byte[] messageAndTimestampHash = FileTimestamp.hashFileWithTimestamp(shaDigest, messageHash, ts);

            byte[] signature = cipherUtility.signDocumentHash(messageAndTimestampHash);

            Document d = new Document();
            d.setTitle(title);
            d.setEncryptedHash(BytesHexConverter.bytesToHex(signature));
            d.setDocumentChecksum(BytesHexConverter.bytesToHex(messageHash));
            d.setTargetHash(BytesHexConverter.bytesToHex(messageAndTimestampHash));
            d.setTimestamp(ts.getTime());
            documentRepository.save(d);

            return ProofResponse.of(d, encodedPublicKey,
                    CipherUtility.SIGNATURE_ALGORITHM, HASHING_ALGORITHM);
        } catch (Exception e) {
            throw new TimestampingException("Could not timestamp the document", e);
        }
    }

    public ProofResponse verifyDocument(MultipartFile file) {
        String checksum;
        try {
            MessageDigest shaDigest = MessageDigest.getInstance(HASHING_ALGORITHM);
            checksum = BytesHexConverter.bytesToHex(
                    FileChecksumCalculator.getFileChecksum(shaDigest, file));
        } catch (Exception e) {
            throw new TimestampingException("Could not hash the document", e);
        }

        Optional<Document> stored = documentRepository.findFirstByDocumentChecksumOrderByTimestampAsc(checksum);
        if (stored.isEmpty()) {
            throw new DocumentNotFoundException(
                    "No timestamp on record for checksum " + checksum);
        }

        try {
            return ProofResponse.of(stored.get(), secureKeysManager.getEncodedPublicKey(),
                    CipherUtility.SIGNATURE_ALGORITHM, HASHING_ALGORITHM);
        } catch (Exception e) {
            throw new TimestampingException("Could not read the public key", e);
        }
    }
}
