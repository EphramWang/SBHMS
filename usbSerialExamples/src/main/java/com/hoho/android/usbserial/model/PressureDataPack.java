package com.hoho.android.usbserial.model;

import com.hoho.android.usbserial.examples.DataConstants;
import com.hoho.android.usbserial.util.Utils;

public class PressureDataPack extends DataPack {
    public short pressure;//压力传感器电容数据,需要进行除以10的处理，得到的结果其单位为nF
    public short leakage;//泄漏传感器电压的数据,需要进行除以10的处理，得到的结果其单位V，

    public float pressureDisplay;
    public float leakageDisplay;

    public PressureDataPack(long timestamp, byte[] dataBytes) {
        super(timestamp, dataBytes);
        if (dataBytes.length == DataConstants.DATA_LENGTH_PRESSURE) {
            pressure = Utils.byteArrayToShort(dataBytes, DATA_START + 4);
            leakage = Utils.byteArrayToShort(dataBytes, DATA_START + 6);
            pressureDisplay = DataConstants.getFloatFromConfig(DataConstants.CapacitancePressureConfig, pressure / 10.0f);
            leakageDisplay = leakage / 10.0f;
        }
    }

    public PressureDataPack(long timestamp, byte[] dataBytes, float pressureDisplay, float leakageDisplay) {
        super(timestamp, dataBytes);
        this.pressureDisplay = pressureDisplay;
        this.leakageDisplay = leakageDisplay;
    }

    @Override
    public String toString() {
        return "PressureDataPack{" +
                "pressure=" + pressure +
                ", leakage=" + leakage +
                ", pressureDisplay=" + pressureDisplay +
                ", leakageDisplay=" + leakageDisplay +
                '}';
    }
}