package com.leon.detonator.activity;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.base.CoordinateView;
import com.leon.detonator.base.WaveformView;
import com.leon.detonator.R;
import com.leon.detonator.util.KeyUtils;

public class WaveformActivity extends AppCompatActivity {
    private WaveformView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waveform);
        view = findViewById(R.id.view_waveform);
        CoordinateView coordinateView = findViewById(R.id.view_coordinate);
        String received = getIntent().getStringExtra(KeyUtils.KEY_WAVEFORM_DATA);
        int[] data = new int[received.length() / 4];
        try {
            for (int i = 0; i < received.length(); i += 4) {
                data[i / 4] = Integer.parseInt(received.substring(i + 2, i + 4) + received.substring(i, i + 2), 16);
            }
            int max = 0, min = Integer.MAX_VALUE;
            for (int i = 0; i < 512; i++) {
                if (data[i] > max)
                    max = data[i];
                if (data[i] > 10 && data[i] < min)
                    min = data[i];
            }
            if ((max == 0) || (max <= min)) {
                ((BaseApplication) getApplication()).myToast(this, R.string.message_waveform_data_error);
                finish();
            } else {
                view.setScale(1);
                view.setData(data);
                coordinateView.setData(data);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = 480;
                params.height = 320;
                view.setLayoutParams(params);
                coordinateView.invalidate();
                view.invalidate();
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            ((BaseApplication) getApplication()).myToast(this, R.string.message_waveform_data_error);
            finish();
        }
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> {
            if (view.getScale() < 4) {
                view.setScale(view.getScale() + 1);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = 480 * view.getScale();
                params.height = 320;
                view.setLayoutParams(params);
                coordinateView.invalidate();
                view.invalidate();
            }
        });
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> {
            if (view.getScale() > 1) {
                view.setScale(view.getScale() - 1);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = 480 * view.getScale();
                params.height = 320;
                view.setLayoutParams(params);
                coordinateView.invalidate();
                view.invalidate();
            }
        });
    }
}