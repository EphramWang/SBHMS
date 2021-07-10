package com.hoho.android.usbserial.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import com.hoho.android.usbserial.examples.R;
import com.hoho.android.usbserial.model.VoltageDataPack;

import java.util.ArrayList;

import androidx.annotation.Nullable;

import static com.hoho.android.usbserial.examples.BatteryMonitorActivity.sBatteryMaxVol;
import static com.hoho.android.usbserial.examples.BatteryMonitorActivity.sBatteryMinVol;


public class BatteryMonChart extends ChartBase {

    public static final int BAT_COUNT = 6;//电池数量

    Path[] pathList = {
            new Path(),
            new Path(),
            new Path(),
            new Path(),
            new Path(),
            new Path()
    };

    int[] colorList = {R.color.battery1, R.color.battery2, R.color.battery3, R.color.battery4, R.color.battery5, R.color.battery6};

    private ArrayList<VoltageDataPack> voltageDataList;

    public BatteryMonChart(Context context) {
        super(context);
    }

    public BatteryMonChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryMonChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

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
            VoltageDataPack voltageDataPack = voltageDataList.get(i);
            for (int j = 0; j < BAT_COUNT; j++) {
                y = mainRect.top + mainRect.height() * (sBatteryMaxVol - voltageDataPack.voltageList[j]) / (sBatteryMaxVol - sBatteryMinVol);
                if (pathList[j].isEmpty()) {
                    pathList[j].moveTo(x, y);
                } else {
                    pathList[j].lineTo(x, y);
                }
            }
            x = x - xDiff;

        }

        for (int j = 0; j < BAT_COUNT; j++) {
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

    private int getIndexByX(float x) {
        float xDiff = mainRect.width() / (mCount - 1);
        float num = x / xDiff;
        return Math.round(num);
    }

    public void setVoltageDataList(ArrayList<VoltageDataPack> voltageDataList) {
        this.voltageDataList = voltageDataList;
        postInvalidate();
    }

    @Override
    public void onLongPressUp(float x, float y) {
        super.onLongPressUp(x, y);
        try {
            mTouchCallBack.onTouch(voltageDataList.get(voltageDataList.size() - 1), null);
        } catch (Exception e) {
        }
    }

    @Override
    public void onLongPressMove(float x, float y) {
        super.onLongPressMove(x, y);
        try {
            int index = voltageDataList.size() - mCount + getIndexByX(x);
            mTouchCallBack.onTouch(voltageDataList.get(index), null);
        } catch (Exception e) {
        }
    }

    @Override
    public void onLongPressDown(float x, float y) {
        super.onLongPressDown(x, y);
        try {
            int index = voltageDataList.size() - mCount + getIndexByX(x);
            mTouchCallBack.onTouch(voltageDataList.get(index), null);
        } catch (Exception e) {
        }
    }
}
