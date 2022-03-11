package com.leon.detonator.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.R;
import com.leon.detonator.Util.KeyUtils;

public class DetectMethodActivity extends BaseActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_method);

        setTitle(R.string.detect_method);

        findViewById(R.id.btn_detect_barcode).setOnClickListener(this);
        findViewById(R.id.tv_detect_barcode).setOnClickListener(this);
        findViewById(R.id.btn_detect_line).setOnClickListener(this);
        findViewById(R.id.tv_detect_line).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        launch((v.getId() == R.id.btn_detect_barcode || v.getId() == R.id.tv_detect_barcode) ? KeyEvent.KEYCODE_1 : KeyEvent.KEYCODE_2);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launch(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void launch(int which) {
        Intent intent = new Intent();
        switch (which) {
            case KeyEvent.KEYCODE_1:
                intent.putExtra(KeyUtils.KEY_ACTIVITY_TITLE, R.string.detect_barcode);
                intent.setClass(DetectMethodActivity.this, DetectActivity.class);
                startActivityForResult(intent, 1);
                break;
            case KeyEvent.KEYCODE_2:
                intent.putExtra(KeyUtils.KEY_ACTIVITY_TITLE, R.string.detect_line);
                intent.setClass(DetectMethodActivity.this, DetectActivity.class);
                startActivityForResult(intent, 1);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != 0) {
            setResult(resultCode, data);
            finish();
        }
    }
}
