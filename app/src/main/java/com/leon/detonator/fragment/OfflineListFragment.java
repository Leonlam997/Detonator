package com.leon.detonator.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.leon.detonator.adapter.OfflineListAdapter;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.R;

public class OfflineListFragment extends Fragment {
    private ListView listView;
    private OfflineListAdapter adapter;
    private CheckBox cbSelected;
    private MyButton btnDownload, btnDelete;
    private View.OnClickListener clickDownload, clickDelete;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offline_list, container, false);
        view.findViewById(R.id.table_title).setBackgroundColor(view.getContext().getColor(R.color.colorTableTitleBackground));
        listView = view.findViewById(R.id.lv_offline_list);
        cbSelected = view.findViewById(R.id.cb_selected);
        btnDownload = view.findViewById(R.id.btn_offline_download);
        btnDelete = view.findViewById(R.id.btn_delete_selected);
        if (adapter != null) {
            listView.setAdapter(adapter);
        }
        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            ((DetonatorInfoBean) adapter.getItem(i)).setSelected(!((DetonatorInfoBean) adapter.getItem(i)).isSelected());
            adapter.notifyDataSetChanged();
            checkStatus(false);
        });
        cbSelected.setOnClickListener(v -> {
            for (int i = 0; i < adapter.getCount(); i++) {
                ((DetonatorInfoBean) adapter.getItem(i)).setSelected(cbSelected.isChecked());
            }
            adapter.notifyDataSetChanged();
            checkStatus(false);
        });
        checkStatus(false);
        return view;
    }

    public void checkStatus(boolean disabled) {
        if (disabled) {
            new Handler(message -> {
                btnDownload.setEnabled(false);
                btnDelete.setEnabled(false);
                return false;
            }).sendEmptyMessage(1);

        } else {
            new Handler(message -> {
                btnDownload.setEnabled(adapter.getCount() > 0);
                btnDelete.setEnabled(checkSelected());
                return false;
            }).sendEmptyMessage(1);
        }
    }

    private boolean checkSelected() {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (((DetonatorInfoBean) adapter.getItem(i)).isSelected())
                return true;
        }
        return false;
    }

    public View.OnClickListener getClickDownload() {
        return clickDownload;
    }

    public void setClickDownload(View.OnClickListener listener) {
        this.clickDownload = listener;
        new Handler(message -> {
            btnDownload.setOnClickListener(clickDownload);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    public View.OnClickListener getClickDelete() {
        return clickDelete;
    }

    public void setClickDelete(View.OnClickListener listener) {
        this.clickDelete = listener;
        new Handler(message -> {
            btnDelete.setOnClickListener(clickDelete);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    public void setListAdapter(OfflineListAdapter listAdapter) {
        adapter = listAdapter;
        new Handler(message -> {
            if (listView != null)
                listView.setAdapter(adapter);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }
}
