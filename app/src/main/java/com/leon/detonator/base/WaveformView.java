package com.leon.detonator.base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

public class WaveformView extends View {
    private int[] data;
    private final Paint paint = new Paint();
    private int scale;

    public void setData(int[] data) {
        this.data = data;
    }

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getScale() {
        return scale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null != data && data.length == 512) {
            int max, min;
            float kx, fx, fy, fx1, fy1, k, tmp, aver;
            max = 0;
            min = 65535;
            tmp = 0;
            k = 0;
            for (int i = 1; i < 511; i++) {
                if (data[i] > max)
                    max = data[i];
                if (data[i] > 10) {
                    if (data[i] < min)
                        min = data[i];
                    tmp = tmp + data[i];
                    k = k + 1;
                }
            }
            aver = tmp / k;
            Log.d("ZBEST", max + "," + min + "," + aver + "," + k + "," + getWidth() + "," + getHeight());
            //画出白色的坐标线
            k = getHeight() / 2f;
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(scale);
            //用红色线画出ADC值
            paint.setColor(Color.rgb(5, 114, 254));
            fx1 = 5;
            fy1 = k;
            kx = (getWidth() - 10) / 512f;
            for (int i = 1; i < 511; i++) {
                fy = k + (aver - data[i]) / (max - aver) * k;
                fx = 5 + i * kx;
                canvas.drawLine(fx1, fy1, fx, fy, paint);
                fx1 = fx;
                fy1 = fy;
            }
        }
    }
}
