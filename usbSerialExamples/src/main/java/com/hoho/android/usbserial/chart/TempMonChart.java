package com.hoho.android.usbserial.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import com.hoho.android.usbserial.examples.BatteryMonitorActivity;
import com.hoho.android.usbserial.examples.R;

import java.util.ArrayList;

import androidx.annotation.Nullable;

import static com.hoho.android.usbserial.examples.BatteryMonitorActivity.sPressureMax;
import static com.hoho.android.usbserial.examples.BatteryMonitorActivity.sPressureMin;
import static com.hoho.android.usbserial.examples.BatteryMonitorActivity.sTempMax;
import static com.hoho.android.usbserial.examples.BatteryMonitorActivity.sTempMin;


public class TempMonChart extends ChartBase {

    public static final int TEMP_COUNT = 2;//温度数量

    Path[] pathList = {
            new Path(),
            new Path(),
            new Path()
    };

    int[] colorList = {R.color.battery1, R.color.battery2, R.color.battery3, R.color.battery4, R.color.battery5, R.color.battery6};

    public TempMonChart(Context context) {
        super(context);
    }

    public TempMonChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TempMonChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private ArrayList<BatteryMonitorActivity.VoltageDataPack> voltageDataList;
    private ArrayList<BatteryMonitorActivity.PressureDataPack> pressureDataList;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //draw backgroud
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.bgDark));
        canvas.drawRect(mainRect, paint);

        //draw line
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);

        //reset paths
        for (int i = 0; i < pathList.length; i++) {
            pathList[i].reset();
        }

        float x = mainRect.right;
        float y = (mainRect.top + mainRect.bottom) / 2f;
        float xDiff = mainRect.width() / (mCount - 1);
        for (int i = voltageDataList.size() - 1; i >= 0; i--) {
            BatteryMonitorActivity.VoltageDataPack voltageDataPack = voltageDataList.get(i);
            for (int j = 0; j < TEMP_COUNT; j++) {
                y = mainRect.top + mainRect.height() * (sTempMax - voltageDataPack.tempList[j]) / (sTempMax - sTempMin);
                if (pathList[j].isEmpty()) {
                    pathList[j].moveTo(x, y);
                } else {
                    pathList[j].lineTo(x, y);
                }
            }
            x = x - xDiff;
        }

        x = mainRect.right;
        for (int i = pressureDataList.size() - 1; i >=0; i--) {
            BatteryMonitorActivity.PressureDataPack pressureDataPack = pressureDataList.get(i);
            y = mainRect.top + mainRect.height() * (sPressureMax - pressureDataPack.pressure) / (sPressureMax - sPressureMin);
            if (pathList[2].isEmpty()) {
                pathList[2].moveTo(x, y);
            } else {
                pathList[2].lineTo(x, y);
            }
            x = x - xDiff;
        }

        for (int j = 0; j < 3; j++) {
            paint.setColor(getResources().getColor(colorList[j]));
            paint.setStrokeWidth(2);
            canvas.drawPath(pathList[j], paint);
        }

        //draw touch line
        if (mIsDrawLine) {
            paint.setColor(Color.WHITE);
            canvas.drawLine(mTouchLineX, mainRect.top, mTouchLineX, mainRect.bottom, paint);
        }
    }

    public void setVoltageDataList(ArrayList<BatteryMonitorActivity.VoltageDataPack> voltageDataList) {
        this.voltageDataList = voltageDataList;
        postInvalidate();
    }

    public void setPressureDataList(ArrayList<BatteryMonitorActivity.PressureDataPack> pressureDataList) {
        this.pressureDataList = pressureDataList;
        postInvalidate();
    }

    private int getIndexByX(float x) {
        float xDiff = mainRect.width() / (mCount - 1);
        float num = x / xDiff;
        return Math.round(num);
    }

    @Override
    public void onLongPressUp(float x, float y) {
        super.onLongPressUp(x, y);
        try {
            mTouchCallBack.onTouch(voltageDataList.get(voltageDataList.size() - 1), pressureDataList.get(pressureDataList.size() - 1));
        } catch (Exception e) {
        }
    }

    @Override
    public void onLongPressMove(float x, float y) {
        super.onLongPressMove(x, y);
        try {
            int index = voltageDataList.size() - mCount + getIndexByX(x);
            int index2 = pressureDataList.size() - mCount + getIndexByX(x);
            mTouchCallBack.onTouch(voltageDataList.get(index), pressureDataList.get(index2));
        } catch (Exception e) {
        }
    }

    @Override
    public void onLongPressDown(float x, float y) {
        super.onLongPressDown(x, y);
        try {
            int index = voltageDataList.size() - mCount + getIndexByX(x);
            int index2 = pressureDataList.size() - mCount + getIndexByX(x);
            mTouchCallBack.onTouch(voltageDataList.get(index), pressureDataList.get(index2));
        } catch (Exception e) {
        }
    }
}
