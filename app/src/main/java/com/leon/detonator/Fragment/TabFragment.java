package com.leon.detonator.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.leon.detonator.Activity.DetonateStep2Activity;
import com.leon.detonator.Adapter.DetonatorListAdapter;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.R;

import java.util.ArrayList;
import java.util.List;

public class TabFragment extends Fragment {
    private final DetonatorListAdapter adapter;
    private boolean checkedHint = false;
    private CheckBox cbSelected;

    public TabFragment(DetonatorListAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detected_list, container, false);
        boolean isTunnel = true;
        if (null != getActivity()) {
            isTunnel = ((BaseApplication) getActivity().getApplication()).isTunnel();
        }
        if (isTunnel) {
            view.findViewById(R.id.line_row).setVisibility(View.GONE);
            view.findViewById(R.id.text_row).setVisibility(View.GONE);
            ((TextView) view.findViewById(R.id.text_hole)).setText(R.string.table_section);
            ((TextView) view.findViewById(R.id.text_inside)).setText(R.string.table_section_inside);
        }
        view.findViewById(R.id.table_title).setBackgroundColor(view.getContext().getColor(R.color.colorTableTitleBackground));
        cbSelected = view.findViewById(R.id.cb_selected);
        cbSelected.setVisibility(View.GONE);
        view.findViewById(R.id.v_selected).setVisibility(View.GONE);
        view.findViewById(R.id.rl_cb).setVisibility(View.GONE);
        ListView listView = view.findViewById(R.id.lv_detect_list);
        if (adapter != null) {
            listView.setAdapter(adapter);
            if (adapter.isCanSelect()) {
                view.findViewById(R.id.rl_cb).setVisibility(View.VISIBLE);
                view.findViewById(R.id.cb_selected).setVisibility(View.VISIBLE);
                view.findViewById(R.id.v_selected).setVisibility(View.VISIBLE);
                checkBoxStatus();
                cbSelected.setOnClickListener(v -> selected(-1));
                listView.setOnItemClickListener((parent, view1, position, id) -> selected(position));
            }
        }
        return view;
    }

    private void selected(final int pos) {
        if (checkedHint) {
            if (null != getActivity()) {
                getActivity().runOnUiThread(() -> new AlertDialog.Builder(getActivity(), R.style.AlertDialog)
                        .setTitle(R.string.progress_title)
                        .setMessage(R.string.dialog_clear_detected)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> checked(pos))
                        .setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
                            if (pos >= adapter.getList().size()) {
                                List<DetonatorInfoBean> list = new ArrayList<>(adapter.getList());
                                DetonatorInfoBean item = list.get(pos - adapter.getList().size());
                                item.setSelected(!item.isSelected());
                                checkBoxStatus();
                                adapter.updateList(list);
                            }
                        })
                        .setOnCancelListener(dialog -> {
                            if (pos >= adapter.getList().size()) {
                                List<DetonatorInfoBean> list = new ArrayList<>(adapter.getList());
                                DetonatorInfoBean item = list.get(pos - adapter.getList().size());
                                item.setSelected(!item.isSelected());
                                checkBoxStatus();
                                adapter.updateList(list);
                            }
                        })
                        .create().show());
            }
        } else {
            checked(pos);
        }
    }

    private void checked(int pos) {
        List<DetonatorInfoBean> list = new ArrayList<>(adapter.getList());
        if (pos >= 0) {
            if (pos < list.size()) {
                DetonatorInfoBean item = list.get(pos);
                item.setSelected(!item.isSelected());
            }
            checkBoxStatus();
        } else
            for (DetonatorInfoBean bean : list) {
                bean.setSelected(cbSelected.isChecked());
            }
        adapter.updateList(list);
        if (null != getActivity() && getActivity().getClass().equals(DetonateStep2Activity.class))
            ((DetonateStep2Activity) getActivity()).onChangedSelectedList();
    }

    private void checkBoxStatus() {
        if (null != getActivity()) {
            getActivity().runOnUiThread(() -> {
                List<DetonatorInfoBean> list = new ArrayList<>(adapter.getList());
                cbSelected.setChecked(true);
                for (DetonatorInfoBean bean : list) {
                    if (!bean.isSelected()) {
                        cbSelected.setChecked(false);
                        break;
                    }
                }
            });
        }
    }

    public void setCheckedHint(boolean checkedHint) {
        this.checkedHint = checkedHint;
    }
}
