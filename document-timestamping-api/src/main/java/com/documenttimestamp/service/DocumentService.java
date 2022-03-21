package com.documenttimestamp.service;

import com.documenttimestamp.model.Document;
import com.documenttimestamp.repository.DocumentRepository;
import com.documenttimestamp.timestamping.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final String hashingAlgorithm = "SHA-512";
    private final String pkString = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAojL8zi8cAKK/poLWWE01agKwq0dA6UJPOtQbv6u+eKzT71u63zhF0NLE+qVRT4AjhhIjfg6tBcF6LWkYOQPWKUlAPIrBU0KCB6nmPJBy5XdjiVcTLXlNLrelsx6OiB5ba9G2uWK914pu52QXsZUE9613Lnu00Ni+4ntlKTjtNsWTMy4FPsbbZPrH4SKXSvnm9xnVwbcAfZ7aC4OXOiYeWf10goSPS4FQAMyHzC+hT4wzbRuK8geikBC1J1mgue1a1mOR5Cd6ssHizATepU5EPYz3eSCwji7MNNv1hjJuJRgtC7aS4gX5hnaUPUNORZ+zJQTWCmFiA/dDnNj7UXrQqQIDAQAB";

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
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
            byte[] cipheredMessage = CipherUtility.signDocumentHash(messageAndTimestampHash);

            // Write document data to database
            Document d = new Document();
            d.setTitle(title);
            d.setEncryptedHash(BytesHexConverter.bytesToHex(cipheredMessage));
            d.setDocumentChecksum(BytesHexConverter.bytesToHex(messageHash));
            d.setTargetHash(BytesHexConverter.bytesToHex(messageAndTimestampHash));
            d.setPublicKey(pkString);
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
        d.get().setPublicKey(pkString);
        return d.get();
    }
}
