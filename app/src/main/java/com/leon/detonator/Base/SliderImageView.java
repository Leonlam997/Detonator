package com.leon.detonator.Base;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class SliderImageView extends AppCompatImageView {
    private final Handler moveSliderHandler = new Handler();
    private int lastX;
    private OnSliderTouchListener onSliderTouchListener;
    private final Runnable moveSliderRunnable = new Runnable() {
        @Override
        public void run() {
            int moveX = 25;
            int left = getLeft() + moveX;
            int right = getRight() + moveX;
            moveSliderHandler.removeCallbacks(moveSliderRunnable);
            if (right >= 458) {
                right = 458;
                left = right - getWidth();
                onSliderTouchListener.OnMoveToEnd();
            } else {
                moveSliderHandler.postDelayed(moveSliderRunnable, 20);
            }
            layout(left, getTop(), right, getBottom());
            invalidate();
        }
    };

    public SliderImageView(Context context) {
        super(context);
    }

    public SliderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SliderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (!keyMoving) {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    lastX = getLeft();
//                    touchMoving = true;
//                    onSliderTouchListener.OnStartMove();
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    int dx = (int) event.getX() - lastX;
//
//                    int left = getLeft() + dx;
//                    int right = getRight() + dx;
//                    if (left < lastX) {
//                        left = lastX;
//                        right = left + getWidth();
//                    }
//                    if (right >= 458) {
//                        right = 458;
//                        left = right - getWidth();
//                        onSliderTouchListener.OnMoveToEnd();
//                    } else {
//                        onSliderTouchListener.OnMoveToOthers();
//                    }
//                    layout(left, getTop(), right, getBottom());
//                    break;
//                case MotionEvent.ACTION_UP:
//                    touchMoving = false;
//                    layout(lastX, getTop(), lastX + getWidth(), getBottom());
//                    onSliderTouchListener.OnStopMove();
//                    break;
//                default:
//                    break;
//            }
//            invalidate();
//        }
//        return super.onTouchEvent(event);
//    }

    public void setOnSliderTouchListener(OnSliderTouchListener onSliderTouchListener) {
        this.onSliderTouchListener = onSliderTouchListener;
    }

    public void startMove(boolean start) {
//        if (!touchMoving) {
        if (start) {
            lastX = getLeft();
            moveSliderHandler.post(moveSliderRunnable);
        } else if (lastX != 0) {
            layout(lastX, getTop(), lastX + getWidth(), getBottom());
            invalidate();
            moveSliderHandler.removeCallbacks(moveSliderRunnable);
        }
//        }
    }

    public interface OnSliderTouchListener {
        void OnStartMove();

        void OnMoveToOthers();

        void OnMoveToEnd();

        void OnStopMove();
    }
}
