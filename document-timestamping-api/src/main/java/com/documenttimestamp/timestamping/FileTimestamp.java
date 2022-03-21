package com.documenttimestamp.timestamping;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;

public class FileTimestamp {
    public static byte[] hashFileWithTimestamp(MessageDigest digest, byte[] fileHash, Timestamp ts) throws Exception{
        Long timeInMillis = ts.getTime();

        ByteArrayOutputStream bytesOs = new ByteArrayOutputStream( );
        bytesOs.write(fileHash);
        bytesOs.write(timeInMillis.byteValue());

        byte[] bytes = bytesOs.toByteArray();

        digest.update(bytes);

        return digest.digest();
    }
}
