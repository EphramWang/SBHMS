package com.hoho.android.usbserial.examples;

public class DataConstants {
    public static final byte FRAME_HEAD = (byte) 0xFE;
    public static final byte FRAME_TAIL = (byte) 0xC3;

    public static final byte command_voltage = (byte) 0x81;
    public static final byte command_pressure = (byte) 0x82;

    public static final int DATA_LENGTH_VOLTAGE = 26;
    public static final int DATA_LENGTH_PRESSURE = 14;

    public static final int DATA_COUNT_30S = 30;
    public static final int DATA_COUNT_3MIN = 180;
    public static final int DATA_COUNT_10MIN = 600;


    public static byte getCheckSumByte(byte[] byteArray) {
        int sum = 0;
        for (int i = 0; i < byteArray.length; i++) {
            sum += byteArray[i];
        }
        int checksum = 0 - sum;
        return (byte) checksum;
    }

}
