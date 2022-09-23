package com.leon.detonator.activity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;
import com.leon.detonator.util.ConstantUtils;

public class SoundSettingsActivity extends BaseActivity {
    private SeekBar sbSound;
    private CheckBox cbVibrate;
    private LocalSettingBean settings;
    private SoundPool soundSample;
    private BaseApplication myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_settings);

        setTitle(R.string.settings_sound);
        myApp = (BaseApplication) getApplication();
        sbSound = findViewById(R.id.sb_sound);
        cbVibrate = findViewById(R.id.cb_vibrate);
        sbSound.setMax(ConstantUtils.MAX_VOLUME);
        settings = BaseApplication.readSettings();
        sbSound.setProgress(settings.getVolume());
        cbVibrate.setChecked(settings.isVibrate());
        cbVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.cancel();
                        vibrator.vibrate(500);
                    }
                }
            }
        });
        sbSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i <= 0) {
                    soundSample.stop(1);
                } else if (i <= ConstantUtils.MAX_VOLUME) {
                    AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (mAudioManager != null)
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0);
                    soundSample.stop(1);
                    soundSample.play(1, i / (ConstantUtils.MAX_VOLUME * 1.0f), i / (ConstantUtils.MAX_VOLUME * 1.0f), 0, 0, 1);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        findViewById(R.id.iv_sound1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sbSound.setProgress(0);
            }
        });
        findViewById(R.id.iv_sound2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sbSound.setProgress(5);
            }
        });
        initSound();
    }

    private void initSound() {
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        builder.setAudioAttributes(attrBuilder.build());
        soundSample = builder.build();
        soundSample.load(this, R.raw.sample, 1);
    }

    @Override
    protected void onDestroy() {
        if (settings.isVibrate() != cbVibrate.isChecked() || settings.getVolume() != sbSound.getProgress()) {
            settings.setVibrate(cbVibrate.isChecked());
            settings.setVolume(sbSound.getProgress());
            myApp.saveSettings(settings);
        }
        super.onDestroy();
    }
}
