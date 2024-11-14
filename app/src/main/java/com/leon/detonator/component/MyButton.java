package com.leon.detonator.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatButton;

import com.leon.detonator.R;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.ConstantUtils;

import java.util.Locale;

public class MyButton extends AppCompatButton {
    private String keyCode;
    private long lastClickTime;

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
        if (isInEditMode()) {
            setTextColor(context.getColor(isEnabled() ? R.color.colorButtonEnabled : R.color.colorButtonDisabled));
            setBackground(AppCompatResources.getDrawable(context, R.drawable.btn_style));
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyButton);
            keyCode = a.getString(R.styleable.MyButton_keyCode);
            if (keyCode != null && !keyCode.isEmpty())
                setText(String.format(Locale.getDefault(), "%s.%s", keyCode, getText()));
            a.recycle();
            return;
        }
        lastClickTime = 0;
        setBackground(AppCompatResources.getDrawable(context, BaseApplication.settings.isTunnel() ? R.drawable.btn_style : R.drawable.btn_style2));
        setTextColor(context.getColor(isEnabled() ? R.color.colorButtonEnabled : R.color.colorButtonDisabled));
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MyButton);
        keyCode = a.getString(R.styleable.MyButton_keyCode);
        if (keyCode != null && !keyCode.isEmpty())
            setText(String.format(Locale.getDefault(), "%s.%s", keyCode, getText()));
        a.recycle();
    }

    @Override
    public void setEnabled(boolean enabled) {
        setTextColor(getContext().getColor(enabled ? R.color.colorButtonEnabled : R.color.colorButtonDisabled));
        super.setEnabled(enabled);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        OnClickListener listener = v -> {
            if (l != null && System.currentTimeMillis() - lastClickTime > ConstantUtils.FAST_CLICK_DELAY_TIME) {
                lastClickTime = System.currentTimeMillis();
                l.onClick(v);
            }
        };
        super.setOnClickListener(listener);
    }

    public void setTextId(@StringRes int id) {
        if (keyCode != null)
            setText(String.format(Locale.getDefault(), "%s.%s", keyCode, getContext().getString(id)));
        else
            setText(id);
    }
}
