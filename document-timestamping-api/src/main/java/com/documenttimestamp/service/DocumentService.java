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

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final CipherUtility cipherUtility;
    private final SecureKeysManager secureKeysManager;
    private final String hashingAlgorithm = "SHA-512";

    @Autowired
    public DocumentService(DocumentRepository documentRepository,
                           CipherUtility cipherUtility,
                           SecureKeysManager secureKeysManager) {
        this.documentRepository = documentRepository;
        this.cipherUtility = cipherUtility;
        this.secureKeysManager = secureKeysManager;
    }

    public List<Document> getDocuments(){
        return documentRepository.findAll();
    }

    public Document hashAndStoreDocument(String title, MultipartFile file) {
        try {
            // digest message
            MessageDigest shaDigest = MessageDigest.getInstance(hashingAlgorithm);

            // Hash initial message
            byte[] messageHash = FileChecksumCalculator.getFileChecksum(shaDigest, file);

            // Apply timestamp
            Timestamp ts = TimestampingUtility.getCurrentTime();
            byte[] messageAndTimestampHash = FileTimestamp.hashFileWithTimestamp
                    (shaDigest, messageHash, ts);

            // Encode message
            byte[] cipheredMessage = cipherUtility.signDocumentHash(messageAndTimestampHash);

            // Write document data to database
            Document d = new Document();
            d.setTitle(title);
            d.setEncryptedHash(BytesHexConverter.bytesToHex(cipheredMessage));
            d.setDocumentChecksum(BytesHexConverter.bytesToHex(messageHash));
            d.setTargetHash(BytesHexConverter.bytesToHex(messageAndTimestampHash));
            d.setPublicKey(secureKeysManager.getEncodedPublicKey());
            d.setTimestamp(ts.getTime());
            documentRepository.save(d);

            // Return document data
            return d;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Document verifyDocument(MultipartFile file) throws Exception {
        // digest message
        MessageDigest shaDigest = MessageDigest.getInstance(hashingAlgorithm);

        // Hash initial message
        byte[] messageHash = FileChecksumCalculator.getFileChecksum(shaDigest, file);

        var d = documentRepository
                .findFirstByDocumentChecksum(BytesHexConverter.bytesToHex(messageHash));
        if (d.isEmpty()) {
            return null;
        }
        d.get().setPublicKey(secureKeysManager.getEncodedPublicKey());
        return d.get();
    }
}
