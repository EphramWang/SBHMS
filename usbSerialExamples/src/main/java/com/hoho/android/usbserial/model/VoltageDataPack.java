package com.hoho.android.usbserial.model;

import com.hoho.android.usbserial.examples.DataConstants;
import com.hoho.android.usbserial.util.Utils;

import java.util.Arrays;

public class VoltageDataPack extends DataPack {
    public short[] voltageList = new short[DataConstants.BATTERY_COUNT];//电池电压的数据需要进行除以10000的处理, 由测量电压到实际电池电压的计算配置文件，测量得到的6路电压不是实际对应的六节电池的电压，需要根据该配置文件的公式进行计算分别得到每节电池对应的电压值
    public short[] tempList = new short[2];//温度传感器的阻值，结果需要进行除以100的处理

    public float[] voltageListDisplay = new float[DataConstants.BATTERY_COUNT];
    public int[] tempListDisplay = new int[2];

    public VoltageDataPack(long timestamp, byte[] dataBytes) {
        super(timestamp, dataBytes);
        if (dataBytes.length == DataConstants.DATA_LENGTH_VOLTAGE) {
            for (int i = 0; i < voltageList.length; i++) {
                voltageList[i] = Utils.byteArrayToShort(dataBytes, DATA_START + 4 + i * 2);
            }
            //转换电压
            voltageListDisplay[0] = 2.0f * voltageList[5] / 1000f;
            voltageListDisplay[1] = 3.5f * voltageList[4] / 1000f - voltageListDisplay[0];
            voltageListDisplay[2] = 4.3f * voltageList[3] / 1000f - voltageListDisplay[1];
            voltageListDisplay[3] = 6.1f * voltageList[2] / 1000f - voltageListDisplay[2];
            voltageListDisplay[4] = 8.5f * voltageList[1] / 1000f - voltageListDisplay[3];
            voltageListDisplay[5] = 11.0f * voltageList[0] / 1000f - voltageListDisplay[4];
            //转换温度
            tempList[0] = Utils.byteArrayToShort(dataBytes, DATA_START + 4 + 12);
            tempList[1] = Utils.byteArrayToShort(dataBytes, DATA_START + 4 + 14);
            tempListDisplay[0] = DataConstants.getIntFromConfig(DataConstants.Temperature1Config, tempList[0] / 100f);
            tempListDisplay[1] = DataConstants.getIntFromConfig(DataConstants.Temperature2Config, tempList[1] / 100f);
        }
    }

    public VoltageDataPack(long timestamp, byte[] dataBytes, float[] voltageListDisplay, int[] tempListDisplay) {
        super(timestamp, dataBytes);
        this.voltageListDisplay = voltageListDisplay;
        this.tempListDisplay = tempListDisplay;
    }

    @Override
    public String toString() {
        return "VoltageDataPack{" +
                "voltageList=" + Arrays.toString(voltageList) +
                ", tempList=" + Arrays.toString(tempList) +
                '}';
    }
}