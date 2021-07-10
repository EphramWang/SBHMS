/**
 * Copyright 2014  XCL-Charts
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @Project XCL-Charts
 * @Description Android图表基类库
 * @author XiongChuanLiang<br/>(xcl_168@aliyun.com)
 * @license http://www.apache.org/licenses/  Apache v2 License
 * @version 1.0
 */
package com.hoho.android.usbserial.chart;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Created by ning on 17/3/15.
 * 图表触摸类
 */

public class ChartTouchHandler {

    private View mView = null;

    private ArrayList<ITouchable> touchables = new ArrayList<>();

    //单点移动前的坐标位置
    private float oldX = 0.0f, oldY = 0.0f;

    //缩放
    private float oldDist = 1.0f, newDist = 0.0f;
    private float halfDist = 0.0f, scaleRate = 0.0f;

    private boolean isInScaleMode = false;

    //滑动
    private int action = 0;
    private float newX = 0.0f, newY = 0.0f;
    private final float FIXED_RANGE = 8.0f;

    //长按
    private boolean mIsPressed = false;
    private float mDownEventX;
    private float mDownEventY;
    private int mDelayOffset = 300;
    private int mMoveRange = 10;// 在按住过程中有轻微移动视为长按而不是滑动操作
    private boolean mHasPerformedLongPress;

    private CheckForLongPress mPendingCheckForLongPress;

    private ScaleGestureDetector mScaleGestureDetector;


    public ChartTouchHandler(View view, ITouchable... computators) {
        Collections.addAll(touchables, computators);
        this.mView = view;
        mScaleGestureDetector = new ScaleGestureDetector(view.getContext(), mScaleGestureListener);
    }

    public boolean handleTouch(MotionEvent event) {
//        Log.e("www", "handleTouch   event.getPointerCount()" + event.getPointerCount() + "  event.getAction() " + event.getAction());

        switch (event.getPointerCount()) {
            case 1:
                isInScaleMode = false;
                handleTouch_PanMode(event);
                break;
            case 2:
                isInScaleMode = true;
                //handleTouch_Scale(event);
                mScaleGestureDetector.onTouchEvent(event);
                break;
            default:
                break;
        }
        return true;
    }


    private boolean handleTouch_Scale(MotionEvent event) {
        if (touchables.size() < 1) return true;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:  //单点触碰
                scaleRate = 1.0f;
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:  //多点触碰
                //两点按下时的距离
                oldDist = this.spacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                newDist = spacing(event);
                if (Float.compare(newDist, 10.0f) == 1) {
                    halfDist = newDist / 2;

                    if (Float.compare(oldDist, 0.0f) == 0) return true;
                    scaleRate = newDist / oldDist;
                    //目前是采用焦点在哪就以哪范围为中心放大缩小.
                    for (ITouchable chartComputator: touchables) {
                        chartComputator.onScale(scaleRate, scaleRate, event.getX() - halfDist, event.getY() - halfDist);
                    }

                    if (null != mView)
                        //mView.invalidate((int) chartComputator.getLeft(), (int) chartComputator.getTop(), (int) chartComputator.getRight(), (int) chartComputator.getBottom());
                        mView.invalidate();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private boolean handleTouch_PanMode(MotionEvent event) {
        action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE) {

            if (oldX > 0 && oldY > 0) {
                newX = event.getX(0);
                newY = event.getY(0);

                if (Float.compare(Math.abs(newX - oldX), FIXED_RANGE) == 1
                        || Float.compare(Math.abs(newY - oldY), FIXED_RANGE) == 1) {
                    for (ITouchable chartComputator: touchables) {
                        chartComputator.onScroll(newX, newY, oldX, oldY);
                    }
                    oldX = newX;
                    oldY = newY;
                }
            }

            if (!mHasPerformedLongPress && !isInLongPressedRange(event)) {
                // Remove any future long press checks
                removeLongPressCallback();
            }

            for (ITouchable chartComputator: touchables) {
                chartComputator.onLongPressMove(event.getX(), event.getY());
            }

            if (mView != null)
                mView.invalidate();
        } else if (action == MotionEvent.ACTION_DOWN) {
            //在第一个点被按下时触发
            oldX = event.getX(0);
            oldY = event.getY(0);

            mDownEventX = event.getX();
            mDownEventY = event.getY();
            mHasPerformedLongPress = false;
            mIsPressed = true;
            checkForLongClick();

        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            //当屏幕上已经有一个点被按住，此时再按下其他点时触发。
        } else if (action == MotionEvent.ACTION_UP  //当屏幕上唯一的点被放开时触发
                || action == MotionEvent.ACTION_POINTER_UP) {
            oldX = 0.0f;
            oldY = 0.0f;

            if (action == MotionEvent.ACTION_POINTER_UP) {
                //当屏幕上有多个点被按住，松开其中一个点时触发（即非最后一个点被放开时）。
                oldX = -1f;
                oldY = -1f;
            }

            if (!mHasPerformedLongPress) {
                // This is a tap, so remove the longpress check
                if (isInLongPressedRange(event)) {
                    for (ITouchable chartComputator: touchables) {
                        chartComputator.onSingleTap(event.getX(), event.getY());
                    }
                }
                removeLongPressCallback();
            }
            else {
                mView.getParent().requestDisallowInterceptTouchEvent(false);
                for (ITouchable chartComputator: touchables) {
                    chartComputator.onLongPressUp(event.getX(), event.getY());
                }
                if (mView != null)
                    mView.invalidate();
            }
            mIsPressed = false;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mView.getParent().requestDisallowInterceptTouchEvent(false);
            mIsPressed = false;
            removeLongPressCallback();
            //chartComputator.handleLongPressCancel();
        }

        return true;
    }


    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 长按检测
     */

    private boolean isInLongPressedRange(MotionEvent event) {
        float offsetX = Math.abs(event.getX() - mDownEventX);
        float offsetY = Math.abs(event.getY() - mDownEventY);
        if (offsetX <= mMoveRange && offsetY <= mMoveRange) {
            return true;
        }
        return false;
    }

    private void checkForLongClick() {
        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mView.postDelayed(mPendingCheckForLongPress, mDelayOffset);
    }

    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
            mView.removeCallbacks(mPendingCheckForLongPress);
        }
    }

    class CheckForLongPress implements Runnable {
        @Override
        public void run() {
            if (mIsPressed && !isInScaleMode) {
                mHasPerformedLongPress = true;
                mView.getParent().requestDisallowInterceptTouchEvent(true);
                for (ITouchable chartComputator: touchables) {
                    chartComputator.onLongPressDown(mDownEventX, mDownEventY);
                }
                if (mView != null)
                    mView.invalidate();
            }
        }
    }


    /**
     * 缩放检测
     */
    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private float lastSpanX;
        private float lastSpanY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
//            lastSpanX = ScaleGestureDetector.getCurrentSpanX();
//            lastSpanY = ScaleGestureDetector.getCurrentSpanY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

//            float spanX = ScaleGestureDetector.getCurrentSpanX();
//            float spanY = ScaleGestureDetector.getCurrentSpanY();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();

            float scaleFactor = scaleGestureDetector.getScaleFactor();

            //缩放
            for (ITouchable chartComputator: touchables) {
                chartComputator.onScale(scaleFactor, scaleFactor, focusX, focusY);
            }


            // 刷新view
            mView.invalidate();

//            lastSpanX = spanX;
//            lastSpanY = spanY;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    };
}
