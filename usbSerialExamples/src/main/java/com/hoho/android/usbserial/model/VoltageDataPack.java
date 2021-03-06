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
            voltageListDisplay[0] = DataConstants.BAT1proportion * voltageList[5] / 10000f;
            voltageListDisplay[1] = DataConstants.BAT2proportion * voltageList[4] / 10000f - DataConstants.BAT1proportion * voltageList[5] / 10000f;
            voltageListDisplay[2] = DataConstants.BAT3proportion * voltageList[3] / 10000f - DataConstants.BAT2proportion * voltageList[4] / 10000f;
            voltageListDisplay[3] = DataConstants.BAT4proportion * voltageList[2] / 10000f - DataConstants.BAT3proportion * voltageList[3] / 10000f;
            voltageListDisplay[4] = DataConstants.BAT5proportion * voltageList[1] / 10000f - DataConstants.BAT4proportion * voltageList[2] / 10000f;
            voltageListDisplay[5] = DataConstants.BAT6proportion * voltageList[0] / 10000f - DataConstants.BAT5proportion * voltageList[1] / 10000f;
            for (int i = 0; i < 6; i++) {
                if (voltageListDisplay[i] > DataConstants.Battery_MaxVol) {
                    voltageListDisplay[i] = DataConstants.Battery_MaxVol;
                } else if (voltageListDisplay[i] < DataConstants.Battery_MinVol) {
                    voltageListDisplay[i] = DataConstants.Battery_MinVol;
                }
            }
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
                ", voltageListDisplay=" + Arrays.toString(voltageListDisplay) +
                ", tempListDisplay=" + Arrays.toString(tempListDisplay) +
                '}';
    }
}