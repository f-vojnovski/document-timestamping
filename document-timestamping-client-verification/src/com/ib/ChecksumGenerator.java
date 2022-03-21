package com.ib;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;

public class ChecksumGenerator {
    private final static String hashingAlgorithm = "SHA-512";
    private final static Long documentTimestamp = 1647824028289L;
    private final static String filePath = "sample.pdf";

    public static void main(String args[]) {
        try {
            // digest message
            MessageDigest shaDigest = MessageDigest.getInstance(hashingAlgorithm);
            InputStream inputStream = new FileInputStream(filePath);

            // Hash initial message
            byte[] messageHash = getFileChecksum(shaDigest, inputStream);

            // Apply timestamp
            Timestamp timestamp = new Timestamp(documentTimestamp);

            // Hash file with timestamp
            byte[] encryptedMessageHash = hashFileWithTimestamp(shaDigest, messageHash, timestamp);
            System.out.println(bytesToHex(encryptedMessageHash));
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

    public static byte[] hashFileWithTimestamp(MessageDigest digest, byte[] fileHash, Timestamp ts) throws Exception{
        Long timeInMillis = ts.getTime();

        ByteArrayOutputStream bytesOs = new ByteArrayOutputStream( );
        bytesOs.write(fileHash);
        bytesOs.write(timeInMillis.byteValue());

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
