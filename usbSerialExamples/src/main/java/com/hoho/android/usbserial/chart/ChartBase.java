package com.hoho.android.usbserial.chart;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hoho.android.usbserial.examples.DataConstants;

import androidx.annotation.Nullable;

public class ChartBase extends View implements ITouchable {

    public int mCount = DataConstants.DATA_COUNT_30S;

    protected RectF mainRect;
    protected ChartTouchHandler mChartTouchHandler;
    protected float mTouchLineX;
    protected TouchCallBack mTouchCallBack;
    protected boolean mIsDrawLine = false;

    protected Paint paint = new Paint();


    public ChartBase(Context context) {
        super(context);
    }

    public ChartBase(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ChartBase(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width > 0 && height > 0) {
            mainRect = new RectF(0, 0, width, height);
            mChartTouchHandler = new ChartTouchHandler(this, this);
        }
    }

    public boolean isIsDrawLine() {
        return mIsDrawLine;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mChartTouchHandler != null) {
            return mChartTouchHandler.handleTouch(event);
        }
        return super.onTouchEvent(event);
    }

    public void setTouchCallBack(TouchCallBack mTouchCallBack) {
        this.mTouchCallBack = mTouchCallBack;
    }


    @Override
    public void onScroll(float newX, float newY, float oldX, float oldY) {

    }

    @Override
    public void onScale(float scaleX, float scaleY, float pointX, float pointY) {

    }

    @Override
    public void onLongPressDown(float x, float y) {
        mIsDrawLine = true;
        mTouchLineX = x;
    }

    @Override
    public void onLongPressUp(float x, float y) {
        mIsDrawLine = false;
        mTouchLineX = -1;
    }

    @Override
    public void onLongPressMove(float x, float y) {
        mTouchLineX = x;
    }

    @Override
    public void onSingleTap(float x, float y) {
        Log.e("wwww", "onSingleTap: " + x + " " + y);
    }
}
