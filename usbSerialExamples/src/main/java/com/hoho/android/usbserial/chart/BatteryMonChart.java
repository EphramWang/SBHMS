package com.hoho.android.usbserial.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import com.hoho.android.usbserial.examples.DataConstants;
import com.hoho.android.usbserial.examples.R;
import com.hoho.android.usbserial.model.VoltageDataPack;
import com.hoho.android.usbserial.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.Nullable;


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
        paint.setColor(getResources().getColor(R.color.bgLight));
        float center = (mainRect.top + mainRect.bottom) / 2f;
        canvas.drawLine(mainRect.left, center, mainRect.right, center, paint);;

        //reset paths
        for (int i = 0; i < pathList.length; i++) {
            pathList[i].reset();
        }

        //maxmin
        float maxV = DataConstants.Battery_MinVol;
        float minV = DataConstants.Battery_MaxVol;

        float x = mainRect.right;
        float y = (mainRect.top + mainRect.bottom) / 2f;
        float xDiff = mainRect.width() / (mCount - 1);
        if (voltageDataList == null) {
            return;
        }
        for (int i = voltageDataList.size() - 1; i >= 0; i--) {
            VoltageDataPack voltageDataPack = voltageDataList.get(i);
            for (int j = 0; j < BAT_COUNT; j++) {
                float data = voltageDataPack.voltageListDisplay[j];
                if (data < minV) {
                    minV = data;
                }
                if (data > maxV) {
                    maxV = data;
                }
            }
            x = x - xDiff;
            if (x < mainRect.left) {
                break;
            }
        }
        if (minV > maxV) {
            minV = DataConstants.Battery_MinVol;
            maxV = DataConstants.Battery_MaxVol;
        }
        x = mainRect.right;
        for (int i = voltageDataList.size() - 1; i >= 0; i--) {
            VoltageDataPack voltageDataPack = voltageDataList.get(i);
            for (int j = 0; j < BAT_COUNT; j++) {
                y = mainRect.top + mainRect.height() * (maxV - voltageDataPack.voltageListDisplay[j]) / (maxV - minV);
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

        for (int j = 0; j < BAT_COUNT; j++) {
            paint.setColor(getResources().getColor(colorList[j]));
            paint.setStrokeWidth(2);
            canvas.drawPath(pathList[j], paint);
        }

        //draw legend
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.textColorDark));
        paint.setAntiAlias(true);
        paint.setTextSize(Utils.dp2px(getContext(), 12));
        canvas.drawText(maxV + "V", mainRect.left, mainRect.top + Utils.dp2px(getContext(), 12), paint);
        canvas.drawText(minV + "V", mainRect.left, mainRect.bottom - Utils.dp2px(getContext(), 12), paint);
        String centerV = String.format("%.4f", (maxV + minV) / 2);
        canvas.drawText(centerV + "V", mainRect.left, center, paint);
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
