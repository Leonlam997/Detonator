package com.leon.detonator.base;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.leon.detonator.R;

import java.util.Locale;

public class MyButton extends AppCompatButton {
    private final int keyCode;
    private final Context mContext;

    public MyButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (isEnabled()) {
            setTextColor(ContextCompat.getColor(context, R.color.colorButtonEnabled));
        } else {
            setTextColor(ContextCompat.getColor(context, R.color.colorButtonDisabled));
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyButton);
        setBackground(ContextCompat.getDrawable(context, a.getBoolean(R.styleable.MyButton_bigButton, true) ? R.drawable.btn_big_button_style : R.drawable.btn_small_button_style));
        keyCode = a.getInt(R.styleable.MyButton_keyCode, -1);
        if (keyCode != -1) {
            setText(String.format(Locale.CHINA, "%d.%s", keyCode, getText()));
        }
        a.recycle();
    }

    public MyButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        if (isEnabled()) {
            setTextColor(ContextCompat.getColor(context, R.color.colorButtonEnabled));
        } else {
            setTextColor(ContextCompat.getColor(context, R.color.colorButtonDisabled));
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyButton);
        setBackground(ContextCompat.getDrawable(context, a.getBoolean(R.styleable.MyButton_bigButton, true) ? R.drawable.btn_big_button_style : R.drawable.btn_small_button_style));
        keyCode = a.getInt(R.styleable.MyButton_keyCode, -1);
        if (keyCode != -1) {
            setText(String.format(Locale.CHINA, "%d.%s", keyCode, getText()));
        }
        a.recycle();
    }

    public int getKeyCode() {
        return keyCode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            setTextColor(ContextCompat.getColor(getContext(), R.color.colorButtonEnabled));
        } else {
            setTextColor(ContextCompat.getColor(getContext(), R.color.colorButtonDisabled));
        }
        super.setEnabled(enabled);
    }

    public void setTextId(@StringRes int id) {
        setText(String.format(Locale.CHINA, "%d.%s", keyCode, mContext.getString(id)));
    }
}
