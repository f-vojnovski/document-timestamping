export default function generateDriverCode(signatureHex, pk, targetHash, signatureAlgorithm) {
return `package com.ib;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


// Checks the server's signature over the document-plus-timestamp hash, using nothing
// but the public key. Run ChecksumGenerator first to confirm targetHash really is the
// hash of your document at the claimed time.
public class Main {
    private static final String signatureAlgorithm = "${signatureAlgorithm || "SHA512withRSA"}";
    private static final String signatureHex = "${signatureHex}";
    private static final String keyString = "${pk}";
    private static final String targetHash = "${targetHash}";

    public static void main (String args[]) {
        try {
            PublicKey publicKey = getKey(keyString);

            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initVerify(publicKey);
            signature.update(hexStringToByteArray(targetHash));

            boolean valid = signature.verify(hexStringToByteArray(signatureHex));

            System.out.println("ALGORITHM: " + signatureAlgorithm);
            System.out.println("TARGET HASH: " + targetHash);
            if (valid)  {
                System.out.println("SIGNATURE VALID - OK!");
            } else {
                System.out.println("DOCUMENT NOT VALID!");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

    public static PublicKey getKey(String key){
        try{
            byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(X509publicKey);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }
}
`}
