package com.documenttimestamp.service;

/** Raised when hashing, signing or storing a document fails. */
public class TimestampingException extends RuntimeException {
    public TimestampingException(String message, Throwable cause) {
        super(message, cause);
    }
}
