package com.ib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

// Usage: java com.ib.Main [filePath]
public class Main {
    private final static String hashingAlgorithm = "SHA-512";
    private final static String signatureAlgorithm = "SHA512withRSA";
    private final static String signatureHex = "PASTE_SIGNATURE_HERE";
    private final static String keyString = "PASTE_PUBLIC_KEY_HERE";
    private final static String targetHash = "PASTE_TARGET_HASH_HERE";
    private final static Long documentTimestamp = 0L;
    private final static String filePath = "sample.pdf";

    public static void main(String args[]) {
        if (keyString.startsWith("PASTE") || documentTimestamp == 0L) {
            System.out.println("This copy has no proof in it yet.");
            System.out.println("Upload a document through the web client and use the Main.java it");
            System.out.println("generates, or fill in the signature, public key, target hash and");
            System.out.println("timestamp fields at the top of this file.");
            System.exit(1);
        }

        String path = args.length > 0 ? args[0] : filePath;
        if (!new File(path).exists()) {
            System.out.println("File not found: " + path);
            System.out.println("Usage: java com.ib.Main <filePath>");
            System.exit(1);
        }

        try {
            byte[] documentChecksum = getFileChecksum(path);
            String recomputed = bytesToHex(hashWithTimestamp(documentChecksum, documentTimestamp));
            boolean documentMatches = recomputed.equalsIgnoreCase(targetHash);

            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initVerify(getKey(keyString));
            signature.update(hexStringToByteArray(targetHash));
            boolean signatureValid = signature.verify(hexStringToByteArray(signatureHex));

            System.out.println("ALGORITHM: " + signatureAlgorithm);
            System.out.println("DOCUMENT: " + path);
            System.out.println("TARGET HASH: " + targetHash);
            System.out.println("RECOMPUTED:  " + recomputed);

            if (documentMatches) {
                System.out.println("DOCUMENT MATCHES - OK!");
            } else {
                System.out.println("DOCUMENT DOES NOT MATCH!");
            }
            if (signatureValid) {
                System.out.println("SIGNATURE VALID - OK!");
            } else {
                System.out.println("SIGNATURE NOT VALID!");
            }

            System.exit(documentMatches && signatureValid ? 0 : 1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static byte[] getFileChecksum(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(hashingAlgorithm);
        try (InputStream inputStream = new FileInputStream(path)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = inputStream.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        return digest.digest();
    }

    // The timestamp goes in as all eight bytes of the epoch millisecond value,
    // big-endian, which is the layout the server uses.
    public static byte[] hashWithTimestamp(byte[] fileHash, long timeInMillis) throws Exception {
        ByteArrayOutputStream bytesOs = new ByteArrayOutputStream();
        bytesOs.write(fileHash);
        bytesOs.write(ByteBuffer.allocate(Long.BYTES).putLong(timeInMillis).array());

        return MessageDigest.getInstance(hashingAlgorithm).digest(bytesOs.toByteArray());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static PublicKey getKey(String key) throws GeneralSecurityException {
        byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
        X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);

        return KeyFactory.getInstance("RSA").generatePublic(X509publicKey);
    }
}
