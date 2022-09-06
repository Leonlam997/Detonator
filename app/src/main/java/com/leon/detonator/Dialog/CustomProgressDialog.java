package com.leon.detonator.Dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.leon.detonator.R;

import java.util.Locale;

public class CustomProgressDialog extends Dialog {
    private ProgressBar progressBar;
    private TextView textViewProgress;

    public void setMessage(@StringRes int messageId) {
        ((TextView) findViewById(R.id.tv_progress_dialog_message)).setText(messageId);
    }

    public void incrementProgressBy(int diff) {
        progressBar.incrementSecondaryProgressBy(diff);
        textViewProgress.setText(String.format(Locale.CHINA, "%d/%d", progressBar.getSecondaryProgress(), progressBar.getMax()));
    }

    public CustomProgressDialog(@NonNull Context context) {
        super(context, R.style.Dialog_style);
    }

    public int getMax() {
        return progressBar.getMax();
    }

    public int getProgress() {
        return progressBar.getSecondaryProgress();
    }

    public void setMax(int max) {
        progressBar.setMax(max);
        textViewProgress.setText(String.format(Locale.CHINA, "%d/%d", progressBar.getSecondaryProgress(), progressBar.getMax()));
    }

    public void setProgress(int progress) {
        progressBar.setSecondaryProgress(progress);
        textViewProgress.setText(String.format(Locale.CHINA, "%d/%d", progressBar.getSecondaryProgress(), progressBar.getMax()));
    }

    public void setSecondaryProgress(int secondaryProgress) {
        ((ProgressBar) findViewById(R.id.pb_total)).setProgress(secondaryProgress);
        ((TextView) findViewById(R.id.tv_dialog_total_progress)).setText(String.format(getContext().getResources().getString(R.string.progress_total), secondaryProgress));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_dialog_progress);
        if (getWindow() != null) {
            final WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = 280;
            params.height = 140;
            getWindow().setAttributes(params);
        }
        progressBar = findViewById(R.id.pb_scan);
        textViewProgress = findViewById(R.id.tv_dialog_progress);
    }
}
