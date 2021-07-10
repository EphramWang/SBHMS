package com.hoho.android.usbserial.model;

import com.hoho.android.usbserial.examples.DataConstants;
import com.hoho.android.usbserial.util.Utils;

public class PressureDataPack extends DataPack {
    public short pressure;
    public short leakage;

    public PressureDataPack(long timestamp, byte[] dataBytes) {
        super(timestamp, dataBytes);
        if (dataBytes.length == DataConstants.DATA_LENGTH_PRESSURE) {
            pressure = Utils.byteArrayToShort(dataBytes, DATA_START + 4);
            leakage = Utils.byteArrayToShort(dataBytes, DATA_START + 6);
        }
    }

    public PressureDataPack(long timestamp, byte[] dataBytes, short pressure, short leakage) {
        super(timestamp, dataBytes);
        this.pressure = pressure;
        this.leakage = leakage;
    }

    @Override
    public String toString() {
        return "PressureDataPack{" +
                "pressure=" + pressure +
                ", leakage=" + leakage +
                '}';
    }
}