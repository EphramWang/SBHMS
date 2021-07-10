package com.hoho.android.usbserial.model;

import com.hoho.android.usbserial.examples.DataConstants;
import com.hoho.android.usbserial.util.Utils;

import java.util.Arrays;

public class VoltageDataPack extends DataPack {
    public short[] voltageList = new short[DataConstants.BATTERY_COUNT];
    public short[] tempList = new short[2];

    public VoltageDataPack(long timestamp, byte[] dataBytes) {
        super(timestamp, dataBytes);
        if (dataBytes.length == DataConstants.DATA_LENGTH_VOLTAGE) {
            for (int i = 0; i < voltageList.length; i++) {
                voltageList[i] = Utils.byteArrayToShort(dataBytes, DATA_START + 4 + i * 2);
            }
            tempList[0] = Utils.byteArrayToShort(dataBytes, DATA_START + 4 + 12);
            tempList[1] = Utils.byteArrayToShort(dataBytes, DATA_START + 4 + 14);
        }
    }

    public VoltageDataPack(long timestamp, byte[] dataBytes, short[] voltageList, short[] tempList) {
        super(timestamp, dataBytes);
        this.voltageList = voltageList;
        this.tempList = tempList;
    }

    @Override
    public String toString() {
        return "VoltageDataPack{" +
                "voltageList=" + Arrays.toString(voltageList) +
                ", tempList=" + Arrays.toString(tempList) +
                '}';
    }
}