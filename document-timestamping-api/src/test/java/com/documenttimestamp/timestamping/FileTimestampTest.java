package com.documenttimestamp.timestamping;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class FileTimestampTest {
    private static final byte[] FILE_HASH = new byte[64];

    private byte[] hashAt(long millis) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        return FileTimestamp.hashFileWithTimestamp(digest, FILE_HASH, new Timestamp(millis));
    }

    @Test
    void producesASha512SizedDigest() throws Exception {
        assertEquals(64, hashAt(1_647_824_028_289L).length);
    }

    @Test
    void isDeterministicForTheSameInputs() throws Exception {
        assertArrayEquals(hashAt(1_647_824_028_289L), hashAt(1_647_824_028_289L));
    }

    /**
     * Regression test. An earlier version appended Long.byteValue() of the epoch
     * milliseconds, which kept one byte of eight, so any two timestamps 256 ms apart
     * collided and the signature bound the document to only 1 of 256 possible times.
     */
    @Test
    void timestampsExactly256MillisApartDoNotCollide() throws Exception {
        long base = 1_647_824_028_289L;
        assertFalse(java.util.Arrays.equals(hashAt(base), hashAt(base + 256L)),
                "timestamps 256 ms apart must not produce the same hash");
    }

    @Test
    void everyByteOfTheTimestampAffectsTheHash() throws Exception {
        long base = 1_647_824_028_289L;
        for (int byteIndex = 0; byteIndex < Long.BYTES; byteIndex++) {
            long shifted = base ^ (1L << (byteIndex * 8));
            assertFalse(java.util.Arrays.equals(hashAt(base), hashAt(shifted)),
                    "flipping a bit in byte " + byteIndex + " of the timestamp must change the hash");
        }
    }

    @Test
    void differentDocumentsWithTheSameTimestampDiffer() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] otherHash = new byte[64];
        otherHash[0] = 1;
        byte[] a = hashAt(1_647_824_028_289L);
        byte[] b = FileTimestamp.hashFileWithTimestamp(digest, otherHash, new Timestamp(1_647_824_028_289L));
        assertFalse(java.util.Arrays.equals(a, b));
    }
}
