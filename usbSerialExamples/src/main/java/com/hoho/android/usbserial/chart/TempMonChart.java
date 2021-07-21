package com.hoho.android.usbserial.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import com.hoho.android.usbserial.examples.DataConstants;
import com.hoho.android.usbserial.examples.R;
import com.hoho.android.usbserial.model.PressureDataPack;
import com.hoho.android.usbserial.model.VoltageDataPack;
import com.hoho.android.usbserial.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.Nullable;


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

    private ArrayList<VoltageDataPack> voltageDataList;
    private ArrayList<PressureDataPack> pressureDataList;

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
        if (voltageDataList != null) {
            for (int i = voltageDataList.size() - 1; i >= 0; i--) {
                VoltageDataPack voltageDataPack = voltageDataList.get(i);
                for (int j = 0; j < TEMP_COUNT; j++) {
                    y = mainRect.top + mainRect.height() * (DataConstants.TEMP_MAX - voltageDataPack.tempListDisplay[j]) / (DataConstants.TEMP_MAX - DataConstants.TEMP_MIN);
                    if (pathList[j].isEmpty()) {
                        pathList[j].moveTo(x, y);
                    } else {
                        pathList[j].lineTo(x, y);
                    }
                }
                x = x - xDiff;
                if (x < mainRect.left) {
                    break;
                }
            }
        }

        if (pressureDataList != null) {
            x = mainRect.right;
            for (int i = pressureDataList.size() - 1; i >= 0; i--) {
                PressureDataPack pressureDataPack = pressureDataList.get(i);
                y = mainRect.top + mainRect.height() * (DataConstants.PRESSURE_MAX - pressureDataPack.pressureDisplay) / (DataConstants.PRESSURE_MAX - DataConstants.PRESSURE_MIN);
                if (pathList[2].isEmpty()) {
                    pathList[2].moveTo(x, y);
                } else {
                    pathList[2].lineTo(x, y);
                }
                x = x - xDiff;
                if (x < mainRect.left) {
                    break;
                }
            }
        }

        for (int j = 0; j < 3; j++) {
            paint.setColor(getResources().getColor(colorList[j]));
            paint.setStrokeWidth(2);
            canvas.drawPath(pathList[j], paint);
        }

        //draw legend
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.textColorDark));
        paint.setAntiAlias(true);
        paint.setTextSize(Utils.dp2px(getContext(), 12));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(DataConstants.TEMP_MAX + "°C", mainRect.left, mainRect.top + Utils.dp2px(getContext(), 12), paint);
        canvas.drawText(DataConstants.TEMP_MIN + "°C", mainRect.left, mainRect.bottom - Utils.dp2px(getContext(), 12), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(DataConstants.PRESSURE_MAX + "Bar", mainRect.right, mainRect.top + Utils.dp2px(getContext(), 12), paint);
        if (voltageDataList.size() > 0) {
            try {
                long lastTime = voltageDataList.get(voltageDataList.size() - 1).timestamp;
                long firstTime;
                if (voltageDataList.size() <= mCount) {
                    firstTime = voltageDataList.get(0).timestamp;
                } else {
                    firstTime = voltageDataList.get(voltageDataList.size() - mCount).timestamp;
                }
                String strDateFormat = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(sdf.format(new Date(lastTime)), mainRect.right, mainRect.bottom - Utils.dp2px(getContext(), 0), paint);
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(sdf.format(new Date(firstTime)), mainRect.left, mainRect.bottom - Utils.dp2px(getContext(), 0), paint);
            } catch (Exception e) {
            }
        }

        //draw touch line
        if (mIsDrawLine) {
            paint.setColor(Color.WHITE);
            canvas.drawLine(mTouchLineX, mainRect.top, mTouchLineX, mainRect.bottom, paint);
        }
    }

    public void setVoltageDataList(ArrayList<VoltageDataPack> voltageDataList) {
        this.voltageDataList = voltageDataList;
        postInvalidate();
    }

    public void setPressureDataList(ArrayList<PressureDataPack> pressureDataList) {
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
        VoltageDataPack voltageDataPack = null;
        PressureDataPack pressureDataPack = null;
        try {
            int index = voltageDataList.size() - mCount + getIndexByX(x);
            voltageDataPack = voltageDataList.get(index);
        } catch (Exception e) {
        }
        try {
            int index2 = pressureDataList.size() - mCount + getIndexByX(x);
            pressureDataPack = pressureDataList.get(index2);
        } catch (Exception e) {
        }
        mTouchCallBack.onTouch(voltageDataPack, pressureDataPack);
    }

    @Override
    public void onLongPressDown(float x, float y) {
        super.onLongPressDown(x, y);
        VoltageDataPack voltageDataPack = null;
        PressureDataPack pressureDataPack = null;
        try {
            int index = voltageDataList.size() - mCount + getIndexByX(x);
            voltageDataPack = voltageDataList.get(index);
        } catch (Exception e) {
        }
        try {
            int index2 = pressureDataList.size() - mCount + getIndexByX(x);
            pressureDataPack = pressureDataList.get(index2);
        } catch (Exception e) {

        }
        mTouchCallBack.onTouch(voltageDataPack, pressureDataPack);
    }
}
