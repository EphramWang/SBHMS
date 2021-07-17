package com.hoho.android.usbserial.examples;

import android.content.Context;

import com.hoho.android.usbserial.model.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DataConstants {
    public static final byte FRAME_HEAD = (byte) 0xFE;
    public static final byte FRAME_TAIL = (byte) 0xC3;

    public static final byte command_voltage = (byte) 0x81;
    public static final byte command_pressure = (byte) 0x82;

    public static final int DATA_LENGTH_VOLTAGE = 26;
    public static final int DATA_LENGTH_PRESSURE = 14;

    public static final int DATA_COUNT_30S = 240;
    public static final int DATA_COUNT_3MIN = 1440;
    public static final int DATA_COUNT_10MIN = 4800;

    public static final int BATTERY_COUNT = 6;

    public static float PRESSURE_MAX = 8.3f;
    public static float PRESSURE_MIN = 0f;
    public static float PRESSURE_Threshold = 6f;
    public static float LEAKAGE_MAX= 3.3f;
    public static float LEAKAGE_MIN = 0f;
    public static float LEAKAGE_Threshold = 1.7f;

    public static int TEMP_MAX = 85;
    public static int TEMP_MIN = -40;
    public static int TEMP1_MAX = 85;
    public static int TEMP1_MIN = -40;
    public static int TEMP2_MAX = 85;
    public static int TEMP2_MIN = -40;

    public static int TEMP1_Threshold = 65;
    public static int TEMP2_Threshold = 65;

    public static float BAT1proportion = 2f;
    public static float BAT2proportion = 3.5f;
    public static float BAT3proportion = 4.3f;
    public static float BAT4proportion = 6.1f;
    public static float BAT5proportion = 8.5f;
    public static float BAT6proportion = 11f;

    public static float Battery_MaxVol = 4.20f;
    public static float Battery_MinVol = 2.70f;

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
        boolean readLimits = false;
        File fileLimits = new File("/sdcard/serialConfig/Limits_of_Sensor.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(fileLimits);
            readLimits = readThreshhold(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!readLimits) {
            try {
                readThreshhold(context.getResources().getAssets().open("Limits_of_Sensor.txt"));
            } catch (Exception e) {
            }
        }

        boolean readThreshHold = false;
        File file = new File("/sdcard/serialConfig/Threshold_of_Sensor.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            readThreshHold = readThreshhold(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!readThreshHold) {
            try {
                readThreshhold(context.getResources().getAssets().open("Threshold_of_Sensor.txt"));
            } catch (Exception e) {
            }
        }
        TEMP_MIN = Math.min(TEMP1_MIN, TEMP2_MIN);
        TEMP_MAX = Math.max(TEMP1_MAX, TEMP2_MAX);

        boolean readBat = false;
        File fileBat = new File("/sdcard/serialConfig/Battery_Voltage_Calculation.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(fileBat);
            readBat = readBATproportion(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!readBat) {
            try {
                readBATproportion(context.getResources().getAssets().open("Battery_Voltage_Calculation.txt"));
            } catch (Exception e) {
            }
        }

        File fileCapacitance = new File("/sdcard/serialConfig/Capacitance_Pressure_Conversion.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(fileCapacitance);
            CapacitancePressureConfig = getFromStream(context, fileInputStream, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (CapacitancePressureConfig == null || CapacitancePressureConfig.size() == 0) {
            CapacitancePressureConfig = getFromAssets(context, "Capacitance_Pressure_Conversion.txt", 1);
        }

        File fileBattery = new File("/sdcard/serialConfig/Battery_Quantity_Calculation.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(fileBattery);
            BatteryQuantityConfig = getFromStream(context, fileInputStream, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (BatteryQuantityConfig == null || BatteryQuantityConfig.size() == 0) {
            BatteryQuantityConfig = getFromAssets(context, "Battery_Quantity_Calculation.txt", 0);
        }

        File fileTemperature1 = new File("/sdcard/serialConfig/Temperature1_Calculation.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(fileTemperature1);
            Temperature1Config = getFromStream(context, fileInputStream, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Temperature1Config == null || Temperature1Config.size() == 0) {
            Temperature1Config = getFromAssets(context, "Temperature1_Calculation.txt", 0);
        }
        File fileTemperature2 = new File("/sdcard/serialConfig/Temperature2_Calculation.txt");
        try {
            FileInputStream fileInputStream = new FileInputStream(fileTemperature2);
            Temperature2Config = getFromStream(context, fileInputStream, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Temperature2Config == null || Temperature1Config.size() == 0) {
            Temperature2Config = getFromAssets(context, "Temperature2_Calculation.txt", 0);
        }
    }

    public static boolean readBATproportion(InputStream inputStream) {
        try {
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            while((line = bufReader.readLine()) != null) {
                String trimedLine = line.trim();
                if (trimedLine.startsWith(";") || trimedLine.startsWith("[")) {
                    continue;//skip comments
                }
                if (trimedLine.startsWith("BAT1.proportion")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        BAT1proportion = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("BAT2.proportion")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        BAT2proportion = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("BAT3.proportion")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        BAT3proportion = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("BAT4.proportion")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        BAT4proportion = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("BAT5.proportion")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        BAT5proportion = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("BAT6.proportion")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        BAT6proportion = Float.parseFloat(datas[1].trim());
                    }
                }
            }
            bufReader.close();
            inputReader.close();
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public static boolean readThreshhold(InputStream inputStream) {
        try {
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            while((line = bufReader.readLine()) != null) {
                String trimedLine = line.trim();
                if (trimedLine.startsWith(";") || trimedLine.startsWith("[")) {
                    continue;//skip comments
                }
                if (trimedLine.startsWith("Threshold.Pressure")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        PRESSURE_Threshold = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Threshold.Leakage")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        LEAKAGE_Threshold = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Threshold.Temperature1")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        TEMP1_Threshold = Integer.parseInt(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Threshold.Temperature2")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        TEMP2_Threshold = Integer.parseInt(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Voltage.min")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        Battery_MinVol = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Voltage.max")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        Battery_MaxVol = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Temperature1.min")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        TEMP1_MIN = Integer.parseInt(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Temperature1.max")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        TEMP1_MAX = Integer.parseInt(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Temperature2.min")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        TEMP2_MIN = Integer.parseInt(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Temperature2.max")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        TEMP2_MAX = Integer.parseInt(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Pressure.min")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        PRESSURE_MIN = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Pressure.max")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        PRESSURE_MAX = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Leakage.min")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        LEAKAGE_MIN = Float.parseFloat(datas[1].trim());
                    }
                } else if (trimedLine.startsWith("Limit.Leakage.max")) {
                    String[] datas = trimedLine.split("=");
                    if (datas.length == 2) {
                        LEAKAGE_MAX = Float.parseFloat(datas[1].trim());
                    }
                }
            }
            bufReader.close();
            inputReader.close();
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public static ArrayList<Config> getFromAssets(Context context, String fileName, int dataType) {
        try {
            return getFromStream(context, context.getResources().getAssets().open(fileName), dataType);
        } catch (Exception e) {
        }
        return null;
    }

    public static ArrayList<Config> getFromStream(Context context, InputStream inputStream, int dataType){
        ArrayList<Config> configMaps = new ArrayList<>();
        try {
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line;
            while((line = bufReader.readLine()) != null) {
                String trimedLine = line.trim();
                if (trimedLine.startsWith(";") || trimedLine.startsWith("[")) {
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
            bufReader.close();
            inputReader.close();
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
            if (configs.get(0).key > configs.get(configs.size() - 1).key) {
                if (key > configs.get(0).key) {
                    value = configs.get(0).valueInt;
                } else if (key < configs.get(configs.size() - 1).key)
                    value = configs.get(configs.size() - 1).valueInt;
            } else if (configs.get(0).key < configs.get(configs.size() - 1).key) {
                if (key < configs.get(0).key) {
                    value = configs.get(0).valueInt;
                } else if (key > configs.get(configs.size() - 1).key) {
                    value = configs.get(configs.size() - 1).valueInt;
                }
            }
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
            if (configs.get(0).key > configs.get(configs.size() - 1).key) {
                if (key > configs.get(0).key) {
                    value = configs.get(0).valueFloat;
                } else if (key < configs.get(configs.size() - 1).key)
                    value = configs.get(configs.size() - 1).valueFloat;
            } else if (configs.get(0).key < configs.get(configs.size() - 1).key) {
                if (key < configs.get(0).key) {
                    value = configs.get(0).valueFloat;
                } else if (key > configs.get(configs.size() - 1).key) {
                    value = configs.get(configs.size() - 1).valueFloat;
                }
            }
        }
        return value;
    }

}
