package com.leon.detonator.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.R;
import com.leon.detonator.Util.KeyUtils;

import org.jetbrains.annotations.NotNull;

public class WifiConnectActivity extends BaseActivity {
    private MyButton btn_Confirm;
    private final Handler setConfirmButton = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (msg.what != 0) {
                btn_Confirm.setEnabled(msg.what == 1);
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_connect);

        setTitle(getIntent().getStringExtra(KeyUtils.KEY_WIFI_CONNECT_SSID));
        ((CheckBox) findViewById(R.id.cbDispPsw)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            EditText etPsw = findViewById(R.id.etWifiPsw);
            etPsw.setInputType(isChecked ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) :
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            etPsw.setSelection(etPsw.getText().length());
        });
        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });


        btn_Confirm = findViewById(R.id.btnConfirm);
        btn_Confirm.setEnabled(false);
        ((EditText) findViewById(R.id.etWifiPsw)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                setConfirmButton.sendEmptyMessage(s.length() >= 8 ? 1 : 2);
            }
        });

        btn_Confirm.setOnClickListener(v -> {
            if (((EditText) findViewById(R.id.etWifiPsw)).getText().length() > 0)
                setResult(RESULT_OK, new Intent().putExtra(KeyUtils.KEY_WIFI_CONNECT_PASSWORD, ((EditText) findViewById(R.id.etWifiPsw)).getText().toString()));
            else
                setResult(RESULT_CANCELED);
            finish();
        });
    }
}
