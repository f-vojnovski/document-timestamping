package com.ib;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


// This is the code that the client does to verify a signature
// This class does not need to exist for the project to work
public class Main {
    private static final String encryptedMessageHash = "05A28818AB2391E62620F6C4A3279BB6385EC89C69BCD66A2D19254935D43A07457E785EB6A51B99029D7DCB73E87B76E6701D76EC6822CF2D935DA80174A52D595ACD22CA405B07CC79E9BADB785C3B27132F850BA73D1ADD465AF1C4F11E08C686E8218F1FE391158711FAB355DCB5B38BB3A70FC14A464763D8A93DB68F3AC64CD23D78AFCC90E7E0B0E3CE94FBDE7F00BE713E642CD5E04DF4EFEB1DE6E2F0BBB8AF590BC2E9FC09850CD9D1AD73EB0C9505304D3B18E2C1A3FB52192E55432822FF476069949BD42476E799367CBD35662FDFEC521F41AB3DF8502E6EC62DDD88AFE7C38AC694F770B8BC169838C37D288B7E992AF3DAA3CBDADF85B171";
    private static final String keyString = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAojL8zi8cAKK/poLWWE01agKwq0dA6UJPOtQbv6u+eKzT71u63zhF0NLE+qVRT4AjhhIjfg6tBcF6LWkYOQPWKUlAPIrBU0KCB6nmPJBy5XdjiVcTLXlNLrelsx6OiB5ba9G2uWK914pu52QXsZUE9613Lnu00Ni+4ntlKTjtNsWTMy4FPsbbZPrH4SKXSvnm9xnVwbcAfZ7aC4OXOiYeWf10goSPS4FQAMyHzC+hT4wzbRuK8geikBC1J1mgue1a1mOR5Cd6ssHizATepU5EPYz3eSCwji7MNNv1hjJuJRgtC7aS4gX5hnaUPUNORZ+zJQTWCmFiA/dDnNj7UXrQqQIDAQAB";
    private static final String targetHash = "04807C9C01238BB9636F1617CF78FAE78C8156E2A8B9E2790ED060BB1A85F7859BE7E6B5D75B81EB89F82B1DAB86E774AABF01A3023CA4C484FCA08ED53CD827";

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
