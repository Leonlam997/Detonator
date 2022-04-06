package com.leon.detonator.Base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CoordinateView extends View {
    private int[] data;
    private final Paint paint= new Paint();

    public void setData(int[] data) {
        this.data = data;
    }

    public CoordinateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null!=data && data.length==512){
            int max, min;
            float fy, k, tmp, aver;
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
            //画出白色的坐标线
            k = getHeight() / 2f;
            canvas.drawColor(Color.rgb(0, 0, 0));
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1);
            paint.setColor(Color.rgb(255, 255, 255));
            canvas.drawLine(5, 0, 5, getHeight(), paint);
            canvas.drawLine(5, k, getWidth() - 5, k, paint);
            tmp = (max - aver) / 10;
            for (int i = 1; i <= 9; i++) {
                fy = k / 10f * i;
                paint.setColor(Color.rgb(8, 27, 57));
                canvas.drawLine(0, k - fy, getWidth() - 5, k - fy, paint);
                canvas.drawLine(0, k + fy, getWidth() - 5, k + fy, paint);
                paint.setColor(Color.rgb(255, 255, 255));
                canvas.drawText((int) (tmp * i) + "", getWidth() - 40, k - fy, paint);
                canvas.drawText((int) (-1 * tmp * i) + "", getWidth() - 40, k + fy, paint);
            }
        }
    }
}
