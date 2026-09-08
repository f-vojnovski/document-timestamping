package com.ib;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.sql.Timestamp;

// Recomputes the document-plus-timestamp hash offline, the same way the server did.
// Usage: java com.ib.ChecksumGenerator [filePath] [timestampMillis]
public class ChecksumGenerator {
    private final static String hashingAlgorithm = "SHA-512";
    private final static Long documentTimestamp = 1647824028289L;
    private final static String filePath = "sample.pdf";

    public static void main(String args[]) {
        try {
            String path = args.length > 0 ? args[0] : filePath;
            long ts = args.length > 1 ? Long.parseLong(args[1]) : documentTimestamp;

            // digest message
            MessageDigest shaDigest = MessageDigest.getInstance(hashingAlgorithm);
            InputStream inputStream = new FileInputStream(path);

            // Hash initial message
            byte[] messageHash = getFileChecksum(shaDigest, inputStream);

            // Apply timestamp
            Timestamp timestamp = new Timestamp(ts);

            // Hash file with timestamp
            byte[] messageAndTimestampHash = hashFileWithTimestamp(shaDigest, messageHash, timestamp);

            System.out.println("DOCUMENT CHECKSUM:");
            System.out.println(bytesToHex(messageHash));
            System.out.println("DOCUMENT + TIMESTAMP HASH:");
            System.out.println(bytesToHex(messageAndTimestampHash));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getFileChecksum(MessageDigest digest, InputStream inputStream) throws IOException
    {
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = inputStream.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        inputStream.close();

        return digest.digest();
    }

    // The timestamp goes in as all eight bytes of the epoch millisecond value,
    // big-endian, which is the layout the server uses.
    public static byte[] hashFileWithTimestamp(MessageDigest digest, byte[] fileHash, Timestamp ts) throws Exception{
        long timeInMillis = ts.getTime();

        ByteArrayOutputStream bytesOs = new ByteArrayOutputStream( );
        bytesOs.write(fileHash);
        bytesOs.write(ByteBuffer.allocate(Long.BYTES).putLong(timeInMillis).array());

        byte[] bytes = bytesOs.toByteArray();

        digest.update(bytes);

        return digest.digest();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
