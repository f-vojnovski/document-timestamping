export default function generateDriverCode(encryptedHash, pk, targetHash) {
return `package com.ib;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


// This is the code that the client does to verify a signature
// This class does not need to exist for the project to work
public class Main {
    private static final String encryptedMessageHash = "${encryptedHash}";
    private static final String keyString = "${pk}";
    private static final String targetHash = "${targetHash}";

    public static void main (String args[]) {
        try {
            var publicKey = getKey(keyString);
            byte[] encryptedBytes = hexStringToByteArray(encryptedMessageHash);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] decryptedMessageHash = cipher.doFinal(encryptedBytes);

            String decryptedStr = bytesToHex(decryptedMessageHash);
            System.out.println("DECRYPTED HASH:");
            System.out.println(bytesToHex(decryptedMessageHash));
            if (decryptedStr.equals(targetHash))  {
                System.out.println("KEYS MATCH - OK!");
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
