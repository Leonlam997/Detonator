package com.leon.detonator.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfflineControlFragment extends Fragment {
    private MyButton btnNew, btnAdd;
    private TextView tvEnd;
    private EditText etStart, etAmount;
    private View.OnClickListener clickAdd, clickNew;
    private boolean newEnabled;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offline_control, container, false);
        btnNew = view.findViewById(R.id.btn_new);
        btnAdd = view.findViewById(R.id.btn_add);
        tvEnd = view.findViewById(R.id.tv_end);
        etStart = view.findViewById(R.id.et_start);
        etAmount = view.findViewById(R.id.et_amount);
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean enabled = false;
                if (checkNumber()) {
                    try {
                        String text = etStart.getText().toString().substring(0, 8);
                        long i = Long.parseLong(etStart.getText().toString().substring(8)) + (etAmount.getText().toString().isEmpty() ? 100 : Long.parseLong(etAmount.getText().toString())) - 1;
                        if (Long.toString(i).length() > 5) {
                            if (null != getActivity())
                                ((BaseApplication) getActivity().getApplication()).myToast(getActivity(), R.string.message_amount_input_error);
                            etAmount.requestFocus();
                        } else {
                            text += String.format(Locale.CHINA, "%05d", i);
                            tvEnd.setText(text);
                            enabled = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (null != getActivity())
                            ((BaseApplication) getActivity().getApplication()).myToast(getActivity(), R.string.message_amount_input_error);
                        etAmount.requestFocus();
                    }
                }
                btnAdd.setEnabled(enabled);
            }
        };
        etStart.addTextChangedListener(textWatcher);
        etAmount.addTextChangedListener(textWatcher);
        btnAdd.setEnabled(false);
        return view;
    }

    public void setClickAdd(View.OnClickListener listener) {
        this.clickAdd = listener;
        new Handler(message -> {
            btnAdd.setOnClickListener(clickAdd);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    public void setClickNew(View.OnClickListener listener) {
        this.clickNew = listener;
        new Handler(message -> {
            btnNew.setOnClickListener(clickNew);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    public List<DetonatorInfoBean> getList() {
        List<DetonatorInfoBean> list = new ArrayList<>();
        int j = etAmount.getText().toString().isEmpty() ? 100 : Integer.parseInt(etAmount.getText().toString());

        for (int i = 0; i < j; i++) {
            DetonatorInfoBean bean = new DetonatorInfoBean(etStart.getText().toString().substring(0, 8)
                    + String.format(Locale.CHINA, "%05d", Integer.parseInt(etStart.getText().toString().substring(8)) + i));
            bean.setDownloaded(false);
            list.add(bean);
        }
        return list;
    }

    public void setNewButtonEnabled(boolean enabled) {
        newEnabled = enabled;
        new Handler(message -> {
            btnNew.setEnabled(newEnabled);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    private boolean checkNumber() {
        if (13 == etStart.getText().toString().length()) {
            try {
                long i = Long.parseLong(etStart.getText().toString().substring(0, 7));
                if (i >= 0) {
                    i = Long.parseLong(etStart.getText().toString().substring(8));
                    if (i >= 0 && !etAmount.getText().toString().isEmpty()) {
                        i = Long.parseLong(etAmount.getText().toString());
                        if (i < 0 && null != getActivity())
                            ((BaseApplication) getActivity().getApplication()).myToast(getActivity(), R.string.message_detonator_input_error);
                    } else if (i < 0 && null != getActivity())
                        ((BaseApplication) getActivity().getApplication()).myToast(getActivity(), R.string.message_detonator_input_error);
                    return i >= 0;
                } else if (null != getActivity())
                    ((BaseApplication) getActivity().getApplication()).myToast(getActivity(), R.string.message_detonator_input_error);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            etStart.requestFocus();
        }
        return false;
    }
}
