package com.hoho.android.usbserial.chart;

import com.hoho.android.usbserial.examples.BatteryMonitorActivity;

public interface TouchCallBack {
    void onTouch(BatteryMonitorActivity.DataPack dataPack1, BatteryMonitorActivity.DataPack dataPack2);
}
