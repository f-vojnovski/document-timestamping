package com.ib;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


// This is the code that the client does to verify a signature
// This class does not need to exist for the project to work
public class Main {
    private static final String encryptedMessageHash = "4F073432E36F51AA36A36E3D1F2AE9602363C75CF0C990CC805D7AF5153C93876F07D3983FEA59A8E93DED8DFB2D402350BE1E4A1B4AA8C07C2E964CA882F5832DFDA88A967F84C675273E543DA7D41B17BBA937F45487334DD85C89D656A0454409591799B30EF49C42D9C01021BB4CA779ACD6F3C32BDD22BD876DC6857F11423275ECD5B3CD5055267FC9AA0E4517BB213584C8CCEC7916B890041885D9AC319404D2514F445DF6D5A21DA5C0802D5F1EF87C82ED5550EE2E9E8E15F2AA165B2DB0EE917BC6F1ED790CE97B9D963BFB577261642FE185964820CB4BD9C3E662FFB066129298FAED62BEC2858D9262975620E346473E6FA22771AB23798CCA";
    private static final String keyString = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAojL8zi8cAKK/poLWWE01agKwq0dA6UJPOtQbv6u+eKzT71u63zhF0NLE+qVRT4AjhhIjfg6tBcF6LWkYOQPWKUlAPIrBU0KCB6nmPJBy5XdjiVcTLXlNLrelsx6OiB5ba9G2uWK914pu52QXsZUE9613Lnu00Ni+4ntlKTjtNsWTMy4FPsbbZPrH4SKXSvnm9xnVwbcAfZ7aC4OXOiYeWf10goSPS4FQAMyHzC+hT4wzbRuK8geikBC1J1mgue1a1mOR5Cd6ssHizATepU5EPYz3eSCwji7MNNv1hjJuJRgtC7aS4gX5hnaUPUNORZ+zJQTWCmFiA/dDnNj7UXrQqQIDAQAB";
    private static final String targetHash = "82DD01A5EC48790AA27787BB894FACD13B4DB2FC6154BFFFB6B60EE44F45D993F7164DAE6BCF989A8FFB867D4580EF3C4960209648755F34A83EEA1A0482DB9C";

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
