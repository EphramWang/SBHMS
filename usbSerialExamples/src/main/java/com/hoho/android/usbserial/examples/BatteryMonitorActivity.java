package com.hoho.android.usbserial.examples;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.chart.BatteryMonChart;
import com.hoho.android.usbserial.chart.TempMonChart;
import com.hoho.android.usbserial.chart.TouchCallBack;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.model.DataPack;
import com.hoho.android.usbserial.model.PressureDataPack;
import com.hoho.android.usbserial.model.VoltageDataPack;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;

public class BatteryMonitorActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {
    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final String INTENT_ACTION_GRANT_USB = BuildConfig.APPLICATION_ID + ".GRANT_USB";
    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;

    private int deviceId, portNum, baudRate;
    private boolean withIoManager;
    private BroadcastReceiver broadcastReceiver;
    private Handler mainLooper;
    private TextView receiveText;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    /**
     * datas
     */
    private float[] mBatteryVoltages = new float[DataConstants.BATTERY_COUNT];
    private int[] mBatteryPercents = new int[DataConstants.BATTERY_COUNT];
    private int mTemperature1 = 0;
    private int mTemperature2 = 0;
    private float mLeakage = 0f;
    private float mPressure = 0f;

    private ArrayList<VoltageDataPack> mVoltageDataList = new ArrayList<>();
    private ArrayList<PressureDataPack> mPressureDataList = new ArrayList<>();

    /**
     * views
     */
    private TextView[] mBatteryVoltageTvs = new TextView[DataConstants.BATTERY_COUNT];
    private TextView[] mBatteryPercentTvs = new TextView[DataConstants.BATTERY_COUNT];
    private ProgressBar[] mBatteryProgressBars = new ProgressBar[DataConstants.BATTERY_COUNT];

    private CirCleProgressBar mPressureProgressBar;
    private TextView mPressureStatusTv;

    private TextView mTemperature1Tv;
    private TextView mTemperature2Tv;
    private TextView mTemperatureWarning;

    private TextView mLeakageStatus;
    private View mLeakageColor;

    private BatteryMonChart mChart1;
    private TempMonChart mChart2;
    private TextView[] mCellTvs = new TextView[DataConstants.BATTERY_COUNT];
    private TextView mTemp1Tv;
    private TextView mTemp2Tv;
    private TextView mPressure1Tv;

    TextView mTimeTv1, mTimeTv2, mTimeTv3;

    private void clear() {
        mVoltageDataList.clear();
        mPressureDataList.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //?????????????????????
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    usbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connect();
                }
            }
        };
        mainLooper = new Handler(Looper.getMainLooper());
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            deviceId = getIntent().getExtras().getInt("device");
            portNum = getIntent().getExtras().getInt("port");
            baudRate = getIntent().getExtras().getInt("baud");
            withIoManager = getIntent().getExtras().getBoolean("withIoManager");
        }
        if (deviceId == 0 && portNum == 0 && baudRate ==0) {
            deviceId = 1002;
            portNum = 0;
            baudRate = 115200;
            withIoManager = true;
        }
        //Toast.makeText(getApplicationContext(), "device:" + deviceId + " port:" + portNum + " baud:" + baudRate + "  connected!!", Toast.LENGTH_LONG).show();

        DataConstants.initConfig(getApplicationContext());
        //???????????????
        setContentView(R.layout.battery_monitor_activity);
        //receiveText = findViewById(R.id.title);
        mBatteryVoltageTvs[0] = (TextView) findViewById(R.id.voltage1);
        mBatteryVoltageTvs[1] = (TextView) findViewById(R.id.voltage2);
        mBatteryVoltageTvs[2] = (TextView) findViewById(R.id.voltage3);
        mBatteryVoltageTvs[3] = (TextView) findViewById(R.id.voltage4);
        mBatteryVoltageTvs[4] = (TextView) findViewById(R.id.voltage5);
        mBatteryVoltageTvs[5] = (TextView) findViewById(R.id.voltage6);
        mBatteryPercentTvs[0] = (TextView) findViewById(R.id.percent1);
        mBatteryPercentTvs[1] = (TextView) findViewById(R.id.percent2);
        mBatteryPercentTvs[2] = (TextView) findViewById(R.id.percent3);
        mBatteryPercentTvs[3] = (TextView) findViewById(R.id.percent4);
        mBatteryPercentTvs[4] = (TextView) findViewById(R.id.percent5);
        mBatteryPercentTvs[5] = (TextView) findViewById(R.id.percent6);
        mBatteryProgressBars[0] = (ProgressBar) findViewById(R.id.progress1);
        mBatteryProgressBars[1] = (ProgressBar) findViewById(R.id.progress2);
        mBatteryProgressBars[2] = (ProgressBar) findViewById(R.id.progress3);
        mBatteryProgressBars[3] = (ProgressBar) findViewById(R.id.progress4);
        mBatteryProgressBars[4] = (ProgressBar) findViewById(R.id.progress5);
        mBatteryProgressBars[5] = (ProgressBar) findViewById(R.id.progress6);

        mTemperature1Tv = (TextView) findViewById(R.id.temperature1);
        mTemperature2Tv = (TextView) findViewById(R.id.temperature2);
        mTemperatureWarning = (TextView) findViewById(R.id.temperature_warning);

        mLeakageStatus = (TextView) findViewById(R.id.leakage_status);
        mLeakageColor = findViewById(R.id.leakage_color);

        mPressureProgressBar = (CirCleProgressBar) findViewById(R.id.pressure_progress);
        mPressureStatusTv = (TextView) findViewById(R.id.pressure_info);

        mCellTvs[0] = findViewById(R.id.cell1);
        mCellTvs[1] = findViewById(R.id.cell2);
        mCellTvs[2] = findViewById(R.id.cell3);
        mCellTvs[3] = findViewById(R.id.cell4);
        mCellTvs[4] = findViewById(R.id.cell5);
        mCellTvs[5] = findViewById(R.id.cell6);
        mTemp1Tv = findViewById(R.id.temp1);
        mTemp2Tv = findViewById(R.id.temp2);
        mPressure1Tv = findViewById(R.id.pressure1);

        mChart1 = findViewById(R.id.chart1);
        mChart2 = findViewById(R.id.chart2);
        mChart1.setTouchCallBack(new TouchCallBack() {
            @Override
            public void onTouch(DataPack dataPack1, DataPack dataPack2) {
                mainLooper.post(() -> {
                    if (dataPack1 instanceof VoltageDataPack && ((VoltageDataPack) dataPack1).voltageListDisplay != null) {
                        for (int i = 0; i < DataConstants.BATTERY_COUNT; i++) {
                            float vol = ((VoltageDataPack) dataPack1).voltageListDisplay[i];
                            String volStr = String.format("%.4f", vol);
                            mCellTvs[i].setText("Cell" + (i + 1) + ": " + volStr + "V");
                        }
                    }
                });
            }
        });
        mChart2.setTouchCallBack(new TouchCallBack() {
            @Override
            public void onTouch(DataPack dataPack1, DataPack dataPack2) {
                if (dataPack1 instanceof VoltageDataPack && ((VoltageDataPack) dataPack1).tempListDisplay != null) {
                    mTemp1Tv.setText("Temperature1:" + ((VoltageDataPack) dataPack1).tempListDisplay[0] + "??C");
                    mTemp2Tv.setText("Temperature2:" + ((VoltageDataPack) dataPack1).tempListDisplay[1] + "??C");
                }
                if (dataPack2 instanceof PressureDataPack) {
                    mPressure1Tv.setText("Pressure: " + ((PressureDataPack) dataPack2).pressureDisplay + "Bar");
                }
            }
        });

        mTimeTv1 = findViewById(R.id.time1);
        mTimeTv2 = findViewById(R.id.time2);
        mTimeTv3 = findViewById(R.id.time3);
        mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgDark));
        mTimeTv1.setOnClickListener(view -> {
            mChart1.mCount = DataConstants.DATA_COUNT_30S;
            mChart2.mCount = DataConstants.DATA_COUNT_30S;
            mChart1.postInvalidate();
            mChart2.postInvalidate();
            mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgDark));
            mTimeTv2.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv3.setBackgroundColor(getResources().getColor(R.color.bgLight));
        });
        mTimeTv2.setOnClickListener(view -> {
            mChart1.mCount = DataConstants.DATA_COUNT_3MIN;
            mChart2.mCount = DataConstants.DATA_COUNT_3MIN;
            mChart1.postInvalidate();
            mChart2.postInvalidate();
            mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv2.setBackgroundColor(getResources().getColor(R.color.bgDark));
            mTimeTv3.setBackgroundColor(getResources().getColor(R.color.bgLight));
        });
        mTimeTv3.setOnClickListener(view -> {
            mChart1.mCount = DataConstants.DATA_COUNT_10MIN;
            mChart2.mCount = DataConstants.DATA_COUNT_10MIN;
            mChart1.postInvalidate();
            mChart2.postInvalidate();
            mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv2.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv3.setBackgroundColor(getResources().getColor(R.color.bgDark));

        });

        mChart1.setVoltageDataList(mVoltageDataList);
        mChart2.setVoltageDataList(mVoltageDataList);
        mChart2.setPressureDataList(mPressureDataList);

        //setTestData();
        //startTestThread();

        EventBus.getDefault().register(this);

        updateStatus();

    }

    private void setTestData() {
        //set test data
        float[] testF1 = {2.1f, 4.2f, 3.0f, 3.1f, 2.3f, 1.9f, 4.2f};
        float[] testF2 = {3.8f, 4.1f, 2.4f, 2.5f, 3.1f, 2.2f, 3.8f};
        float[] testF3 = {4.0f, 2.1f, 3.4f, 3.1f, 3.9f, 3.8f, 2.9f};

        for (int i = 0; i < 10; i++) {
            int[] temp1 = {22, 66};
            temp1[0] = (int) (Math.random() * 50);
            int[] temp2 = {22, 66};
            temp1[0] = (int) (Math.random() * 60);
            mVoltageDataList.add(new VoltageDataPack(0, new byte[1], testF1, temp1));
            mVoltageDataList.add(new VoltageDataPack(0, new byte[1], testF2, temp2));
            mVoltageDataList.add(new VoltageDataPack(0, new byte[1], testF3, temp1));

            float pressure = (short) (Math.random() * 8.3f);
            float leakage = (short) (Math.random() * 10f);
            mPressureDataList.add(new PressureDataPack(0, new byte[1], pressure, leakage));
            mPressureDataList.add(new PressureDataPack(0, new byte[1], pressure, leakage));
            mPressureDataList.add(new PressureDataPack(0, new byte[1], pressure, 8.3f));
        }
        mChart1.setVoltageDataList(mVoltageDataList);
        mChart2.setVoltageDataList(mVoltageDataList);
        mChart2.setPressureDataList(mPressureDataList);

        mPressure = 8.3f;
        mLeakage = 1.7f;
        mTemperature1 = 21;
        mTemperature2 = 85;
        float[] testVot = {2.8f, 4.1f, 3.0f, 3.8f, 3f, 3,4f};
        //int[] testP = {20, 33, 44, 32, 76, 43};

        for (int i = 0; i < DataConstants.BATTERY_COUNT; i++) {
            mBatteryVoltages[i] = testVot[i];
            //mBatteryPercents[i] = testP[i];
            mBatteryPercents[i] = DataConstants.getIntFromConfig(DataConstants.BatteryQuantityConfig, mBatteryVoltages[i]);
        }
    }

    void startTestThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    mainLooper.post(new Runnable() {
                        @Override
                        public void run() {
                            if (Math.random() > 0.5f) {
                                //???????????? pressure
                                float pressure = (short) (Math.random() * 8.3f);
                                float leakage = (short) (Math.random() * 10f);
                                PressureDataPack dataPackage = new PressureDataPack(System.currentTimeMillis(), new byte[1], pressure, leakage);
                                mPressureDataList.add(dataPackage);
                                mPressure = dataPackage.pressureDisplay;
                                mLeakage = dataPackage.leakageDisplay;
                                updateStatus();
                            } else {
                                //???????????? ??????
                                int[] temp1 = {22, 66};
                                temp1[0] = (int) (Math.random() * 50);
                                temp1[1] = (int) (Math.random() * 30);
                                float[] testF1 = {2.1f, 4.2f, 3.0f, 3.1f, 2.3f, 1.9f, 4.2f};
                                VoltageDataPack dataPackage = new VoltageDataPack(System.currentTimeMillis(), new byte[1], testF1, temp1);
                                mVoltageDataList.add(dataPackage);
                                mTemperature1 = dataPackage.tempListDisplay[0];
                                mTemperature2 = dataPackage.tempListDisplay[1];
                                for (int i = 0; i < DataConstants.BATTERY_COUNT; i++) {
                                    mBatteryVoltages[i] = dataPackage.voltageListDisplay[i];
                                    mBatteryPercents[i] = DataConstants.getIntFromConfig(DataConstants.BatteryQuantityConfig, mBatteryVoltages[i]);
                                }
                                updateStatus();
                            }
                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(INTENT_ACTION_GRANT_USB));
        if(usbPermission == UsbPermission.Unknown || usbPermission == UsbPermission.Granted)
            mainLooper.post(this::connect);
    }

    @Override
    protected void onPause() {
        if(connected) {
            status("disconnected");
            disconnect();
        }
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
    /*
     * Serial
     */
    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> {
            //receive(data);
            processData(data);
        });

    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(() -> {
            status("connection lost: " + e.getMessage());
            disconnect();
        });
    }

    /*
     * Serial + UI
     */
    private void connect() {
        //???????????????????????????
        clear();

        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        for(UsbDevice v : usbManager.getDeviceList().values())
            if(v.getDeviceId() == deviceId)
                device = v;
        if(device == null) {
            status("connection failed: device not found");
            return;
        }
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if(driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if(driver == null) {
            status("connection failed: no driver for device");
            return;
        }
        if(driver.getPorts().size() < portNum) {
            status("connection failed: not enough ports at device");
            return;
        }
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_ACTION_GRANT_USB), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                status("connection failed: permission denied");
            else
                status("connection failed: open failed");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);
            if(withIoManager) {
                usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
                usbIoManager.start();
            }
            status("connected");
            connected = true;
            //controlLines.start();
        } catch (Exception e) {
            status("connection failed: " + e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        connected = false;
        //controlLines.stop();
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    private void send(String str) {
        if(!connected) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str + '\n').getBytes();
            SpannableStringBuilder spn = new SpannableStringBuilder();
            spn.append("send " + data.length + " bytes\n");
            spn.append(HexDump.dumpHexString(data)).append("\n");
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (receiveText != null)
                receiveText.append(spn);
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            onRunError(e);
        }
    }

    private void read() {
        if(!connected) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] buffer = new byte[8192];
            int len = usbSerialPort.read(buffer, READ_WAIT_MILLIS);
            receive(Arrays.copyOf(buffer, len));
        } catch (IOException e) {
            // when using read with timeout, USB bulkTransfer returns -1 on timeout _and_ errors
            // like connection loss, so there is typically no exception thrown here on error
            status("connection lost: " + e.getMessage());
            disconnect();
        }
    }

    private void receive(byte[] data) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        spn.append("receive " + data.length + " bytes\n");
        if(data.length > 0)
            spn.append(HexDump.dumpHexString(data)).append("\n");
        if (receiveText != null)
            receiveText.append(spn);
        Log.e("wtrace", "received: " + HexDump.dumpHexString(data));
    }

    private void processData(byte[] data) {
        if (data.length == DataConstants.DATA_LENGTH_VOLTAGE) {
            if (data[0] == DataConstants.FRAME_HEAD && data[data.length - 1] == DataConstants.FRAME_TAIL && data[1] == DataConstants.command_voltage) {
                //???????????? ??????
                VoltageDataPack dataPackage = new VoltageDataPack(System.currentTimeMillis(), data);
                mVoltageDataList.add(dataPackage);
                mTemperature1 = dataPackage.tempListDisplay[0];
                mTemperature2 = dataPackage.tempListDisplay[1];
                for (int i = 0; i < DataConstants.BATTERY_COUNT; i++) {
                    mBatteryVoltages[i] = dataPackage.voltageListDisplay[i];
                    mBatteryPercents[i] = DataConstants.getIntFromConfig(DataConstants.BatteryQuantityConfig, mBatteryVoltages[i]);
                }
                updateStatus();
                //Toast.makeText(getApplicationContext(), dataPackage.toString(), Toast.LENGTH_SHORT).show();
            }
        } else if (data.length == DataConstants.DATA_LENGTH_PRESSURE) {
            if (data[0] == DataConstants.FRAME_HEAD && data[data.length - 1] == DataConstants.FRAME_TAIL && data[1] == DataConstants.command_pressure) {
                //???????????? pressure
                PressureDataPack dataPackage = new PressureDataPack(System.currentTimeMillis(), data);
                mPressureDataList.add(dataPackage);
                mPressure = dataPackage.pressureDisplay;
                mLeakage = dataPackage.leakageDisplay;
                updateStatus();
                //Toast.makeText(getApplicationContext(), dataPackage.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (receiveText != null)
            receiveText.append(spn);
        Log.e("wtrace", "status: " + spn);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Object event) {
        if (event instanceof receivePackEvent) {
            DataPack dataPack = ((receivePackEvent) event).dataPack;
            if (dataPack instanceof VoltageDataPack) {
                //todo
                mTemperature1 = ((VoltageDataPack) dataPack).tempListDisplay[0];
                mTemperature2 = ((VoltageDataPack) dataPack).tempListDisplay[1];
                mVoltageDataList.add((VoltageDataPack) dataPack);
                updateStatus();
            } else if (dataPack instanceof PressureDataPack) {
                //todo
                mPressure = ((PressureDataPack) dataPack).pressureDisplay;
                mLeakage = ((PressureDataPack) dataPack).leakageDisplay;
                mPressureDataList.add((PressureDataPack) dataPack);
                updateStatus();
            }
        }
    }

    @SuppressLint("NewApi")
    private void updateStatus() {
        for (int i = 0; i < DataConstants.BATTERY_COUNT; i++) {
            String vol = String.format("%.4f", mBatteryVoltages[i]);
            mBatteryVoltageTvs[i].setText(vol + "V");
            mBatteryPercentTvs[i].setText(mBatteryPercents[i] + "%");
            mBatteryProgressBars[i].setProgress(mBatteryPercents[i]);
            if (!mChart1.isIsDrawLine()) {
                mCellTvs[i].setText("Cell" + (i + 1) + ": " + vol + "V");
            }
        }

        mTemperature1Tv.setText("Temperature1:" + mTemperature1 + "??C");
        mTemperature2Tv.setText("Temperature2:" + mTemperature2 + "??C");
        if (!mChart2.isIsDrawLine()) {
            mTemp1Tv.setText("Temperature1:" + mTemperature1 + "??C");
            mTemp2Tv.setText("Temperature2:" + mTemperature2 + "??C");
        }
        if (mTemperature1 >= DataConstants.TEMP1_Threshold || mTemperature2 >= DataConstants.TEMP2_Threshold) {
            mTemperatureWarning.setText("State:Warning");
        } else {
            mTemperatureWarning.setText("State:Normal");
        }

        if (mLeakage >= DataConstants.LEAKAGE_Threshold) {
            mLeakageStatus.setText("State:Warning");
            mLeakageColor.setBackgroundResource(R.drawable.yuanjiaobg_red);
        } else {
            mLeakageStatus.setText("State:Normal");
            mLeakageColor.setBackgroundResource(R.drawable.yuanjiaobg_green);
        }

        mPressureProgressBar.setMaxProgress(DataConstants.PRESSURE_MAX);
        mPressureProgressBar.setCurrentProgress(mPressure);
        mPressureProgressBar.setCircleColor(mPressure >= DataConstants.PRESSURE_Threshold ? Color.parseColor("#CE0000") : Color.parseColor("#51C81C"));
        mPressureStatusTv.setText(mPressure >= DataConstants.PRESSURE_Threshold ?  "State:Warning" : "State:Normal");
        if (!mChart2.isIsDrawLine()) {
            mPressure1Tv.setText("Pressure: " + mPressure + "Bar");
        }

        if (!mChart1.isIsDrawLine())
            mChart1.invalidate();
        if (!mChart2.isIsDrawLine())
            mChart2.invalidate();
    }

    public static class receivePackEvent {
        public DataPack dataPack;
        public receivePackEvent(DataPack dataPack) {
            this.dataPack = dataPack;
        }
    }

}
