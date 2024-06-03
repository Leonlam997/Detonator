package com.leon.detonator.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

import com.leon.detonator.base.BaseApplication;

public class SliderImageView extends AppCompatImageView {
    private final Handler moveSliderHandler = new Handler();
	private boolean touchMoving = false;
    private boolean keyMoving = false;
    private int lastX;
    private OnSliderTouchListener onSliderTouchListener;

    public SliderImageView(Context context) {
        super(context);
    }

    private final Runnable moveSliderRunnable = new Runnable() {
        @Override
        public void run() {
            final int moveX = 25;
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

    public SliderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SliderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!keyMoving && isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchMoving = true;
                    lastX = getLeft();
                    BaseApplication.writeFile("触屏开始滑动");
                    onSliderTouchListener.OnStartMove();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) event.getX() - lastX;
                    int left = getLeft() + dx;
                    int right = getRight() + dx;
                    if (left < lastX) {
                        left = lastX;
                        right = left + getWidth();
                    }
                    if (right >= 458) {
                        right = 458;
                        left = right - getWidth();
                        onSliderTouchListener.OnMoveToEnd();
                        BaseApplication.writeFile("触屏滑动到末端");
                    } else {
                        onSliderTouchListener.OnMoveToOthers();
                    }
                    layout(left, getTop(), right, getBottom());
                    break;
                case MotionEvent.ACTION_UP:
                    touchMoving = false;
                    layout(lastX, getTop(), lastX + getWidth(), getBottom());
                    onSliderTouchListener.OnStopMove();
                    break;
                default:
                    break;
            }
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled){
            touchMoving = false;
            startMove(false);
        }
        super.setEnabled(enabled);
    }

    public void setOnSliderTouchListener(OnSliderTouchListener onSliderTouchListener) {
        this.onSliderTouchListener = onSliderTouchListener;
    }

    public void startMove(boolean start) {
        if (!touchMoving) {
            keyMoving = start;
            if (start) {
                lastX = getLeft();
                moveSliderHandler.post(moveSliderRunnable);
            } else {
                layout(lastX, getTop(), lastX + getWidth(), getBottom());
                invalidate();
                moveSliderHandler.removeCallbacks(moveSliderRunnable);
            }
        }
    }

    public interface OnSliderTouchListener {
        void OnStartMove();

        void OnMoveToOthers();

        void OnMoveToEnd();

        void OnStopMove();
    }


}
