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


    public static short sPressureThreshold = 2900;
    public static short sLeakageThreshold = 9000;

    public static int sTemperature1Threshold = 135;
    public static int sTemperature2Threshold = 135;

    public static int sTempMax = 80;
    public static int sTempMin = -40;
    public static int sPressureMax = 4000;//todo
    public static int sPressureMin = 0;//todo

    public static float sBatteryMaxVol = 4.2f;
    public static float sBatteryMinVol = 2.75f;

    public static final int BATTERY_COUNT = 6;

    /**
     * datas
     */
    private float[] mBatteryVoltages = new float[BATTERY_COUNT];//电池电压的数据需要进行除以10000的处理
    private int[] mBatteryPercents = new int[BATTERY_COUNT];
    private int mTemperature1 = 0;//温度传感器的阻值，结果需要进行除以100的处理
    private int mTemperature2 = 0;
    private long mLeakage = 0;//泄漏传感器电压的数据,需要进行除以10的处理，得到的结果其单位V，
    private long mPressure = 0;//压力传感器电容数据,需要进行除以10的处理，得到的结果其单位为nF
    private long mMaxPressure = 3000;

    private ArrayList<VoltageDataPack> mVoltageDataList = new ArrayList<>();
    private ArrayList<PressureDataPack> mPressureDataList = new ArrayList<>();

    /**
     * views
     */
    private TextView[] mBatteryVoltageTvs = new TextView[BATTERY_COUNT];
    private TextView[] mBatteryPercentTvs = new TextView[BATTERY_COUNT];
    private ProgressBar[] mBatteryProgressBars = new ProgressBar[BATTERY_COUNT];

    private CirCleProgressBar mPressureProgressBar;
    private TextView mPressureStatusTv;

    private TextView mTemperature1Tv;
    private TextView mTemperature2Tv;
    private TextView mTemperatureWarning;

    private TextView mLeakageStatus;
    private View mLeakageColor;

    private BatteryMonChart mChart1;
    private TempMonChart mChart2;
    private TextView[] mCellTvs = new TextView[BATTERY_COUNT];
    private TextView mTemp1Tv;
    private TextView mTemp2Tv;
    private TextView mPressure1Tv;

    TextView mTimeTv1, mTimeTv2, mTimeTv3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化链接参数
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
        deviceId = getIntent().getExtras().getInt("device");
        portNum = getIntent().getExtras().getInt("port");
        baudRate = getIntent().getExtras().getInt("baud");
        withIoManager = getIntent().getExtras().getBoolean("withIoManager");
        //Toast.makeText(getApplicationContext(), deviceId + portNum + baudRate + " !!", Toast.LENGTH_LONG).show();

        //初始化界面
        setContentView(R.layout.battery_monitor_activity);
        receiveText = findViewById(R.id.title);
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
                    if (dataPack1 instanceof VoltageDataPack && ((VoltageDataPack) dataPack1).voltageList != null) {
                        for (int i = 0; i < BATTERY_COUNT; i++) {
                            mCellTvs[i].setText("Cell" + (i + 1) + ": " + ((VoltageDataPack) dataPack1).voltageList[i] + "V");
                        }
                    }
                });
            }
        });
        mChart2.setTouchCallBack(new TouchCallBack() {
            @Override
            public void onTouch(DataPack dataPack1, DataPack dataPack2) {
                if (dataPack1 instanceof VoltageDataPack && ((VoltageDataPack) dataPack1).tempList != null) {
                    mTemp1Tv.setText("Temperature1:" + ((VoltageDataPack) dataPack1).tempList[0] + "°C");
                    mTemp2Tv.setText("Temperature2:" + ((VoltageDataPack) dataPack1).tempList[1] + "°C");
                }
                if (dataPack2 instanceof PressureDataPack) {
                    mPressure1Tv.setText("Pressure: " + ((PressureDataPack) dataPack2).pressure + "Bar");
                }
            }
        });

        mTimeTv1 = findViewById(R.id.time1);
        mTimeTv2 = findViewById(R.id.time2);
        mTimeTv3 = findViewById(R.id.time3);
        mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgDark));
        mTimeTv1.setOnClickListener(view -> {
            mChart2.mCount = DataConstants.DATA_COUNT_30S;
            mChart2.postInvalidate();
            mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgDark));
            mTimeTv2.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv3.setBackgroundColor(getResources().getColor(R.color.bgLight));
        });
        mTimeTv2.setOnClickListener(view -> {
            mChart2.mCount = DataConstants.DATA_COUNT_3MIN;
            mChart2.postInvalidate();
            mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv2.setBackgroundColor(getResources().getColor(R.color.bgDark));
            mTimeTv3.setBackgroundColor(getResources().getColor(R.color.bgLight));
        });
        mTimeTv3.setOnClickListener(view -> {
            mChart2.mCount = DataConstants.DATA_COUNT_10MIN;
            mChart2.postInvalidate();
            mTimeTv1.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv2.setBackgroundColor(getResources().getColor(R.color.bgLight));
            mTimeTv3.setBackgroundColor(getResources().getColor(R.color.bgDark));

        });

        setTestData();

        EventBus.getDefault().register(this);

        updateStatus();

    }

    private void setTestData() {
        //set test data
        short[] testF1 = {2, 4, 3, 3, 2, 1, 4};
        short[] testF2 = {3, 4, 2, 2, 3, 2, 3};
        short[] testF3 = {4, 2, 3, 3, 3, 3, 2};
        short[] temp1 = {22, 23};
        for (int i = 0; i < 10; i++) {
            mVoltageDataList.add(new VoltageDataPack(0, new byte[1], testF1, temp1));
            mVoltageDataList.add(new VoltageDataPack(0, new byte[1], testF2, temp1));
            mVoltageDataList.add(new VoltageDataPack(0, new byte[1], testF3, temp1));

            short pressure = (short) (Math.random() * 1000);
            short leakage = (short) (Math.random() * 1000);
            mPressureDataList.add(new PressureDataPack(0, new byte[1], pressure, leakage));
            mPressureDataList.add(new PressureDataPack(0, new byte[1], pressure, leakage));
            mPressureDataList.add(new PressureDataPack(0, new byte[1], pressure, leakage));
        }
        mChart1.setVoltageDataList(mVoltageDataList);
        mChart2.setVoltageDataList(mVoltageDataList);
        mChart2.setPressureDataList(mPressureDataList);

        mPressure = 1910;
        mTemperature1 = 21;
        mTemperature2 = 154;
        float[] testVot = {2.8f, 4.1f, 3.0f, 3.8f, 3f, 3,4f};
        int[] testP = {20, 33, 44, 32, 76, 43};

        for (int i = 0; i < BATTERY_COUNT; i++) {
            mBatteryVoltages[i] = testVot[i];
            mBatteryPercents[i] = testP[i];
        }
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
        receiveText.append(spn);
        Log.e("wtrace", "received: " + HexDump.dumpHexString(data));
    }

    private void processData(byte[] data) {
        if (data.length == DataConstants.DATA_LENGTH_VOLTAGE) {
            if (data[0] == DataConstants.FRAME_HEAD && data[data.length - 1] == DataConstants.FRAME_TAIL && data[1] == DataConstants.command_voltage) {
                //数据帧之 电压
                VoltageDataPack dataPackage = new VoltageDataPack(System.currentTimeMillis(), data);
                mVoltageDataList.add(dataPackage);
                mTemperature1 = dataPackage.tempList[0];
                mTemperature2 = dataPackage.tempList[1];
                for (int i = 0; i < BATTERY_COUNT; i++) {
                    mBatteryVoltages[i] = dataPackage.voltageList[i] / 10000f;
                }
                updateStatus();
                //Toast.makeText(getApplicationContext(), dataPackage.toString(), Toast.LENGTH_SHORT).show();
            }
        } else if (data.length == DataConstants.DATA_LENGTH_PRESSURE) {
            if (data[0] == DataConstants.FRAME_HEAD && data[data.length - 1] == DataConstants.FRAME_TAIL && data[1] == DataConstants.command_pressure) {
                //数据帧之 pressure
                PressureDataPack dataPackage = new PressureDataPack(System.currentTimeMillis(), data);
                mPressureDataList.add(dataPackage);
                mPressure = dataPackage.pressure;
                mLeakage = dataPackage.leakage;
                updateStatus();
                //Toast.makeText(getApplicationContext(), dataPackage.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
        Log.e("wtrace", "status: " + spn);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Object event) {
        if (event instanceof receivePackEvent) {
            DataPack dataPack = ((receivePackEvent) event).dataPack;
            if (dataPack instanceof VoltageDataPack) {
                //todo
                mTemperature1 = ((VoltageDataPack) dataPack).tempList[0];
                mTemperature2 = ((VoltageDataPack) dataPack).tempList[1];
                mVoltageDataList.add((VoltageDataPack) dataPack);
                updateStatus();
            } else if (dataPack instanceof PressureDataPack) {
                //todo
                mPressure = ((PressureDataPack) dataPack).pressure;
                mLeakage = ((PressureDataPack) dataPack).leakage;
                mPressureDataList.add((PressureDataPack) dataPack);
                updateStatus();
            }
        }
    }

    @SuppressLint("NewApi")
    private void updateStatus() {
        for (int i = 0; i < BATTERY_COUNT; i++) {
            mBatteryVoltageTvs[i].setText(mBatteryVoltages[i] + "V");
            mBatteryPercentTvs[i].setText(mBatteryPercents[i] + "%");
            mBatteryProgressBars[i].setProgress(mBatteryPercents[i]);
            if (!mChart1.isIsDrawLine()) {
                mCellTvs[i].setText("Cell" + (i + 1) + ": " + mBatteryVoltages[i] + "V");
            }
        }

        mTemperature1Tv.setText("Temperature1:" + mTemperature1 + "°C");
        mTemperature2Tv.setText("Temperature2:" + mTemperature2 + "°C");
        if (!mChart2.isIsDrawLine()) {
            mTemp1Tv.setText("Temperature1:" + mTemperature1 + "°C");
            mTemp2Tv.setText("Temperature2:" + mTemperature2 + "°C");
        }
        if (mTemperature1 > sTemperature1Threshold || mTemperature2 > sTemperature2Threshold) {
            mTemperatureWarning.setText("State:Warning");
        } else {
            mTemperatureWarning.setText("State:Normal");
        }

        if (mLeakage > sLeakageThreshold) {
            mLeakageStatus.setText("State:Warning");
            mLeakageColor.setBackgroundResource(R.drawable.yuanjiaobg_red);
        } else {
            mLeakageStatus.setText("State:Normal");
            mLeakageColor.setBackgroundResource(R.drawable.yuanjiaobg_green);
        }

        mPressureProgressBar.setMaxProgress(mMaxPressure);
        mPressureProgressBar.setCurrentProgress(mPressure);
        mPressureProgressBar.setCircleColor(mPressure > sPressureThreshold ? Color.parseColor("#CE0000") : Color.parseColor("#51C81C"));
        mPressureStatusTv.setText(mPressure > sPressureThreshold ?  "State:Warning" : "State:Normal");
        if (!mChart2.isIsDrawLine()) {
            mPressure1Tv.setText("Pressure: " + mPressure + "Bar");
        }
    }

    public static class DataPack {
        public static final int DATA_START = 4;

        public long timestamp;
        public byte[] dataBytes;
        public boolean isValid = true;

        public DataPack(long timestamp, byte[] dataBytes) {
            this.timestamp = timestamp;
            this.dataBytes = dataBytes;
        }

        public boolean isCheckSumOK() {
            int sum = 0;
            for (int i = 1; i < dataBytes.length - 1; i++) {
                sum += dataBytes[i];
            }
            return sum == 0;
        }
    }
    public static class VoltageDataPack extends DataPack {
        public short[] voltageList = new short[BATTERY_COUNT];
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

    public static class PressureDataPack extends DataPack {
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

    public static class receivePackEvent {
        public DataPack dataPack;
        public receivePackEvent(DataPack dataPack) {
            this.dataPack = dataPack;
        }
    }

    class ReadSerialPortThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {

            }
        }
    }
}
