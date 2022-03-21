package com.documenttimestamp.timestamping;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileChecksumCalculator {
    public static byte[] getFileChecksum(MessageDigest digest, MultipartFile file) throws IOException
    {
        InputStream inputStream = file.getInputStream();

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = inputStream.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        inputStream.close();

        return digest.digest();
    }
}
