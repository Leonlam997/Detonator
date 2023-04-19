package com.leon.detonator.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;

public class DisplaySettingsActivity extends BaseActivity {
    private static final int mMaxBrightness = 255;
    private static final int mMinBrightness = 20;
    private final int[] sleepTimeList = {15, 30, 60, 120, 300, 600, 1800};
    private CheckBox cbAuto;
    private SeekBar sbLight;
    private TextView tvSleepTime, tvFontScale;
    private int selectedItem;
    private LocalSettingBean settingBean;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_settings);

        setTitle(R.string.settings_disp);
        myApp = (BaseApplication) getApplication();
        cbAuto = findViewById(R.id.cbAuto);
        sbLight = findViewById(R.id.sbLight);
        tvSleepTime = findViewById(R.id.tvSleepTime);
        tvFontScale = findViewById(R.id.tvFontScale);
        settingBean = BaseApplication.readSettings();
        final String[] fontScaleList = {getString(R.string.choice_normal),
                getString(R.string.choice_big),
                getString(R.string.choice_large),
                getString(R.string.choice_xlarge)
        };
        final String[] sleepTimeStringList = {getString(R.string.choice_15_seconds),
                getString(R.string.choice_30_seconds),
                getString(R.string.choice_1_minute),
                getString(R.string.choice_2_minutes),
                getString(R.string.choice_5_minutes),
                getString(R.string.choice_10_minutes),
                getString(R.string.choice_30_minutes),
                getString(R.string.choice_never)
        };
        if (settingBean.getFontScale() > 0 && settingBean.getFontScale() < fontScaleList.length) {
            tvFontScale.setText(fontScaleList[settingBean.getFontScale()]);
        } else {
            tvFontScale.setText(fontScaleList[0]);
        }
        findViewById(R.id.rlSleepTime).setOnClickListener(v -> {
            int i;
            for (i = 0; i < sleepTimeStringList.length; i++)
                if (sleepTimeStringList[i].equals(tvSleepTime.getText().toString()))
                    break;
            new AlertDialog.Builder(DisplaySettingsActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_select_sleep_time)
                    .setSingleChoiceItems(sleepTimeStringList, i < sleepTimeStringList.length ? i : 1, (dialog, which) -> selectedItem = which)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                                selectedItem < sleepTimeList.length ? sleepTimeList[selectedItem] * 1000 : Integer.MAX_VALUE);
                        runOnUiThread(() -> tvSleepTime.setText(sleepTimeStringList[selectedItem]));
                    })
                    .setNegativeButton(R.string.btn_cancel, null).create().show();
        });

        findViewById(R.id.rlFontScale).setOnClickListener(v -> {
            int i;
            for (i = 0; i < fontScaleList.length; i++)
                if (fontScaleList[i].equals(tvFontScale.getText().toString()))
                    break;
            new AlertDialog.Builder(DisplaySettingsActivity.this, R.style.AlertDialog)
                    .setTitle(R.string.dialog_title_select_scale)
                    .setSingleChoiceItems(fontScaleList, settingBean.getFontScale() < fontScaleList.length ? settingBean.getFontScale() : 0, (dialog, which) -> selectedItem = which)
                    .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                        if (selectedItem != settingBean.getFontScale()) {
                            settingBean.setFontScale(selectedItem);
                            myApp.saveBean(settingBean);
                            runOnUiThread(() -> tvFontScale.setText(fontScaleList[selectedItem]));
                            myApp.initFontScale();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null).create().show();
        });

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        try {
            cbAuto.setChecked(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC == Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE));
            sbLight.setEnabled(!cbAuto.isChecked());
            sbLight.setProgress((Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) - mMinBrightness) / (mMaxBrightness - mMinBrightness) * 100);
            int screenOffTime = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT) / 1000;
            if (screenOffTime > sleepTimeList[sleepTimeList.length - 1])
                tvSleepTime.setText(sleepTimeStringList[sleepTimeStringList.length - 1]);
            else
                for (int i = 0; i < sleepTimeList.length; i++) {
                    if (screenOffTime == sleepTimeList[i]) {
                        tvSleepTime.setText(sleepTimeStringList[i]);
                        break;
                    }
                }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        cbAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sbLight.setEnabled(!cbAuto.isChecked());
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        });

        sbLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Toast.makeText(DisplaySettingsActivity.this,progress+"",Toast.LENGTH_SHORT).show();
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        progress * (mMaxBrightness - mMinBrightness) / 100 + mMinBrightness);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }
}
