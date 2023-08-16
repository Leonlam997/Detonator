package com.leon.detonator.component;

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
    private int keyCode;

    public MyButton(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public MyButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MyButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        setTextColor(ContextCompat.getColor(context, isEnabled() ? R.color.colorButtonEnabled : R.color.colorButtonDisabled));
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyButton);
        setBackground(ContextCompat.getDrawable(context, a.getBoolean(R.styleable.MyButton_bigButton, false) ? R.drawable.btn_big_button_style : R.drawable.btn_small_button_style));
        keyCode = a.getInt(R.styleable.MyButton_keyCode, -1);
        if (keyCode != -1)
            setText(String.format(Locale.getDefault(), "%d.%s", keyCode, getText()));
        a.recycle();
    }

    public void setBigButton(boolean bigButton) {
        setBackground(ContextCompat.getDrawable(getContext(), bigButton ? R.drawable.btn_big_button_style : R.drawable.btn_small_button_style));
        invalidate();
    }

    public int getKeyCode() {
        return keyCode;
    }

    @Override
    public void setEnabled(boolean enabled) {
        setTextColor(ContextCompat.getColor(getContext(), enabled ? R.color.colorButtonEnabled : R.color.colorButtonDisabled));
        super.setEnabled(enabled);
    }

    public void setTextId(@StringRes int id) {
        if (keyCode != -1)
            setText(String.format(Locale.getDefault(), "%d.%s", keyCode, getContext().getString(id)));
        else
            setText(id);
    }
}
