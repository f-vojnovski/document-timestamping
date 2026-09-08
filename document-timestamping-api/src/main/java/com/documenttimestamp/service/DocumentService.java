package com.documenttimestamp.service;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.repository.DocumentRepository;
import com.documenttimestamp.timestamping.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.List;
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

    public List<Document> getDocuments() {
        return documentRepository.findAll();
    }

    /**
     * Hashes the upload, binds it to the current server time, signs the result and
     * stores it. Failures propagate: a caller that cannot tell a signing error from a
     * successful timestamp has no way to know whether the proof it holds is real.
     */
    public Document hashAndStoreDocument(String title, MultipartFile file) throws Exception {
        MessageDigest shaDigest = MessageDigest.getInstance(HASHING_ALGORITHM);

        // Hash the document on its own
        byte[] messageHash = FileChecksumCalculator.getFileChecksum(shaDigest, file);

        // Bind the document to the moment it arrived
        Timestamp ts = TimestampingUtility.getCurrentTime();
        byte[] messageAndTimestampHash = FileTimestamp.hashFileWithTimestamp(shaDigest, messageHash, ts);

        // Sign the combined hash
        byte[] signature = cipherUtility.signDocumentHash(messageAndTimestampHash);

        Document d = new Document();
        d.setTitle(title);
        d.setEncryptedHash(BytesHexConverter.bytesToHex(signature));
        d.setDocumentChecksum(BytesHexConverter.bytesToHex(messageHash));
        d.setTargetHash(BytesHexConverter.bytesToHex(messageAndTimestampHash));
        d.setTimestamp(ts.getTime());
        documentRepository.save(d);

        d.setPublicKey(secureKeysManager.getEncodedPublicKey());
        d.setSignatureAlgorithm(CipherUtility.SIGNATURE_ALGORITHM);
        d.setHashingAlgorithm(HASHING_ALGORITHM);
        return d;
    }

    /**
     * Looks up a previously timestamped document by its checksum, for the case where
     * the user still holds the file but has lost the proof.
     */
    public Document verifyDocument(MultipartFile file) throws Exception {
        MessageDigest shaDigest = MessageDigest.getInstance(HASHING_ALGORITHM);
        byte[] messageHash = FileChecksumCalculator.getFileChecksum(shaDigest, file);
        String checksum = BytesHexConverter.bytesToHex(messageHash);

        Optional<Document> stored = documentRepository.findFirstByDocumentChecksum(checksum);
        if (stored.isEmpty()) {
            throw new DocumentNotFoundException(
                    "No timestamp on record for checksum " + checksum);
        }

        Document d = stored.get();
        d.setPublicKey(secureKeysManager.getEncodedPublicKey());
        d.setSignatureAlgorithm(CipherUtility.SIGNATURE_ALGORITHM);
        d.setHashingAlgorithm(HASHING_ALGORITHM);
        return d;
    }
}
