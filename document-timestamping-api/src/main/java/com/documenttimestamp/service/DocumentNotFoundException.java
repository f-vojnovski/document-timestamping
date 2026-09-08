package com.documenttimestamp.service;

/** Raised when a document checksum has never been timestamped by this server. */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) {
        super(message);
    }
}
