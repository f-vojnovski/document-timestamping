package com.documenttimestamp.timestamping;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.sql.Timestamp;

public class FileTimestamp {
    /**
     * Hashes the document checksum together with the moment the server received it.
     *
     * The timestamp is written as all eight bytes of the epoch millisecond value in
     * big-endian order. Any client that recomputes this hash has to lay the bytes out
     * the same way, so the encoding is fixed here and mirrored in the verifier the web
     * client generates.
     */
    public static byte[] hashFileWithTimestamp(MessageDigest digest, byte[] fileHash, Timestamp ts) throws Exception {
        long timeInMillis = ts.getTime();

        ByteArrayOutputStream bytesOs = new ByteArrayOutputStream();
        bytesOs.write(fileHash);
        bytesOs.write(ByteBuffer.allocate(Long.BYTES).putLong(timeInMillis).array());

        byte[] bytes = bytesOs.toByteArray();

        digest.update(bytes);

        return digest.digest();
    }
}
