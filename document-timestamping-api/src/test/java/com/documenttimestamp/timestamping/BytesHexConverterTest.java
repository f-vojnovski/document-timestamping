package com.documenttimestamp.timestamping;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class BytesHexConverterTest {
    @Test
    void roundTripsArbitraryBytes() {
        byte[] original = new byte[256];
        new SecureRandom().nextBytes(original);
        assertArrayEquals(original,
                BytesHexConverter.hexStringToByteArray(BytesHexConverter.bytesToHex(original)));
    }

    @Test
    void usesUppercaseTwoDigitHex() {
        assertEquals("000F10FF",
                BytesHexConverter.bytesToHex(new byte[]{0x00, 0x0F, 0x10, (byte) 0xFF}));
    }

    @Test
    void handlesAnEmptyArray() {
        assertEquals("", BytesHexConverter.bytesToHex(new byte[0]));
        assertEquals(0, BytesHexConverter.hexStringToByteArray("").length);
    }
}
