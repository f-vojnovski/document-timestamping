package com.documenttimestamp.timestamping;


import java.sql.Timestamp;

public class TimestampingUtility {
    public static Timestamp getCurrentTime() {
        long currTimeMillis = System.currentTimeMillis();
        return new Timestamp(currTimeMillis);
    }
}
