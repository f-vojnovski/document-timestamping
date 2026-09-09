package com.documenttimestamp.model;

public record ProofResponse(
        Long id,
        String title,
        String encryptedHash,
        String documentChecksum,
        String targetHash,
        Long timestamp,
        String publicKey,
        String signatureAlgorithm,
        String hashingAlgorithm) {

    public static ProofResponse of(Document document, String publicKey,
                                   String signatureAlgorithm, String hashingAlgorithm) {
        return new ProofResponse(
                document.getId(),
                document.getTitle(),
                document.getEncryptedHash(),
                document.getDocumentChecksum(),
                document.getTargetHash(),
                document.getTimestamp(),
                publicKey,
                signatureAlgorithm,
                hashingAlgorithm);
    }
}
