package com.leon.detonator.component;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

public class SliderImageView extends AppCompatImageView {
    private int lastX;
    private boolean keyMoving;
    private boolean touchMoving;
    private OnSliderTouchListener onSliderTouchListener;
    private final Handler moveSliderHandler = new Handler(msg -> {
        int moveX = 35;
        int left = getLeft() + moveX;
        int right = getRight() + moveX;
        if (right >= 703) {
            right = 703;
            left = right - getWidth();
        }
        layout(left, getTop(), right, getBottom());
        invalidate();
        if (right != 703)
            msg.getTarget().sendEmptyMessageDelayed(1, 20);
        else
            onSliderTouchListener.OnMoveToEnd();
        return false;
    });

    public SliderImageView(Context context) {
        super(context);
    }

    public SliderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SliderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnSliderTouchListener(OnSliderTouchListener onSliderTouchListener) {
        this.onSliderTouchListener = onSliderTouchListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!keyMoving) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = getLeft();
                    touchMoving = true;
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
                    if (right >= 703) {
                        right = 703;
                        left = right - getWidth();
                        onSliderTouchListener.OnMoveToEnd();
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

    public void startMove(boolean start) {
        if (!touchMoving) {
            if (start) {
                keyMoving = true;
                lastX = getLeft();
                moveSliderHandler.sendEmptyMessageDelayed(1, 20);
            } else if (lastX != 0) {
                keyMoving = false;
                layout(lastX, getTop(), lastX + getWidth(), getBottom());
                invalidate();
                moveSliderHandler.removeMessages(1);
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
