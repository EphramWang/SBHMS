package com.hoho.android.usbserial.examples;

import android.content.Context;

import com.hoho.android.usbserial.model.Config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

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

    public static final int BATTERY_COUNT = 6;

    public static final float PRESSURE_MAX = 8.3f;
    public static final float PRESSURE_MIN = 0f;
    public static final float PRESSURE_Threshold = 6.0f;
    public static final float LEAKAGE_Threshold = 1.7f;

    public static final int TEMP_MAX = 85;
    public static final int TEMP_MIN = -40;

    public static final int TEMP1_Threshold = 70;//todo
    public static final int TEMP2_Threshold = 70;//todo

    public static final float Battery_MaxVol = 4.20f;
    public static final float Battery_MinVol = 2.75f;

    public static ArrayList<Config> CapacitancePressureConfig = new ArrayList<>();
    public static ArrayList<Config> BatteryQuantityConfig = new ArrayList<>();
    public static ArrayList<Config> Temperature1Config = new ArrayList<>();
    public static ArrayList<Config> Temperature2Config = new ArrayList<>();


    public static byte getCheckSumByte(byte[] byteArray) {
        int sum = 0;
        for (int i = 0; i < byteArray.length; i++) {
            sum += byteArray[i];
        }
        int checksum = 0 - sum;
        return (byte) checksum;
    }

    public static void initConfig(Context context) {
        CapacitancePressureConfig = getFromAssets(context, "Capacitance_Pressure_Conversion.txt", 1);
        BatteryQuantityConfig = getFromAssets(context, "Battery_Quantity_Calculation.txt", 0);
        Temperature1Config = getFromAssets(context, "Temperature1_Calculation.txt", 0);
        Temperature2Config = getFromAssets(context, "Temperature2_Calculation.txt", 0);
    }

    public static ArrayList<Config> getFromAssets(Context context, String fileName, int dataType){
        ArrayList<Config> configMaps = new ArrayList<>();
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName) );
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            while((line = bufReader.readLine()) != null) {
                if (line.trim().startsWith(";")) {
                    continue;//skip comments
                }
                String[] lineData = line.trim().split(":");
                if (lineData.length == 2) {
                    if (dataType == 0) {
                        //int type
                        float key = Float.parseFloat(lineData[0]);
                        int value = Integer.parseInt(lineData[1]);
                        Config config = new Config(key, 0f, value);
                        configMaps.add(config);
                    } else if (dataType == 1) {
                        //float type
                        float key = Float.parseFloat(lineData[0]);
                        float value = Float.parseFloat(lineData[1]);
                        Config config = new Config(key, value, 0);
                        configMaps.add(config);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configMaps;
    }

    public static int getIntFromConfig(ArrayList<Config> configs, float key) {
        int value = 0;
        boolean valueFound = false;
        for (int i = 0; i < configs.size() - 1; i++) {
            float key1 = configs.get(i).key;
            float key2 = configs.get(i + 1).key;
            if ((key1 <= key && key <= key2) || (key2 <= key && key <= key1)) {
                if (Math.abs(key - key1) < Math.abs(key2 - key)) {
                    value = configs.get(i).valueInt;
                } else {
                    value = configs.get(i + 1).valueInt;
                }
                valueFound = true;
            }
        }
        if (!valueFound && configs.size() > 0) {
            value = configs.get(0).valueInt;
        }
        return value;
    }
    public static float getFloatFromConfig(ArrayList<Config> configs, float key) {
        float value = 0;
        boolean valueFound = false;
        for (int i = 0; i < configs.size() - 1; i++) {
            float key1 = configs.get(i).key;
            float key2 = configs.get(i + 1).key;
            if (key1 <= key && key2 >= key) {
                if (key - key1 < key2 - key) {
                    value = configs.get(i).valueFloat;
                } else {
                    value = configs.get(i + 1).valueFloat;
                }
                valueFound = true;
            }
        }
        if (!valueFound && configs.size() > 0) {
            value = configs.get(0).valueFloat;
        }
        return value;
    }

}
