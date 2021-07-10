package com.hoho.android.usbserial.model;

public class DataPack {
    public static final int DATA_START = 4;

    public long timestamp;
    public byte[] dataBytes;
    public boolean isValid = true;

    public DataPack(long timestamp, byte[] dataBytes) {
        this.timestamp = timestamp;
        this.dataBytes = dataBytes;
    }

    public boolean isCheckSumOK() {
        int sum = 0;
        for (int i = 1; i < dataBytes.length - 1; i++) {
            sum += dataBytes[i];
        }
        return sum == 0;
    }
}