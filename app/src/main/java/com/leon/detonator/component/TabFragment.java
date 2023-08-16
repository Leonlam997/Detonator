package com.leon.detonator.component;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.util.KeyUtils;

import java.util.List;

public class TabFragment extends ListFragment {
    private DetonatorListAdapter adapter;

    public TabFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getArguments() != null && getArguments().getParcelableArrayList(KeyUtils.KEY_LIST) != null)
            adapter = new DetonatorListAdapter(context, getArguments().getParcelableArrayList(KeyUtils.KEY_LIST));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detected_list, container, false);
        if (BaseApplication.isTunnel) {
            view.findViewById(R.id.line_inside).setVisibility(View.GONE);
            view.findViewById(R.id.text_inside).setVisibility(View.GONE);
            ((TextView) view.findViewById(R.id.text_row)).setText(R.string.table_section);
            ((TextView) view.findViewById(R.id.text_hole)).setText(R.string.table_section_inside);
        }
        view.findViewById(R.id.table_title).setBackgroundColor(view.getContext().getColor(R.color.colorTableTitleBackground));
        if (adapter != null)
            setListAdapter(adapter);
        return view;
    }

    public void updateList(List<DetonatorBean> list) {
        adapter.updateList(list);
    }
}
