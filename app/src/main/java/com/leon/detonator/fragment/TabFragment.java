package com.leon.detonator.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.adapter.ICheckedListener;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;

public class TabFragment extends Fragment {
    private final DetonatorListAdapter adapter;
    private CheckBox cbSelected;
    private final ICheckedListener listener;
    private boolean isTunnel;

    public TabFragment(DetonatorListAdapter adapter, ICheckedListener listener) {
        this.adapter = adapter;
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        isTunnel = ((BaseApplication) ((BaseActivity) context).getApplication()).isTunnel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detected_list, container, false);
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
                if (listener != null) {
                    cbSelected.setOnClickListener(v -> listener.checked(-1));
                    listView.setOnItemClickListener((parent, view1, position, id) -> listener.checked((position)));
                }
            }
        }
        return view;
    }

    public void allChecked(boolean checked) {
        cbSelected.setChecked(checked);
    }

    public boolean isAllChecked() {
        return cbSelected.isChecked();
    }
}
