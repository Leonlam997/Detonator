package com.leon.detonator.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.leon.detonator.R;
import com.leon.detonator.adapter.OfflineListAdapter;
import com.leon.detonator.base.MyButton;
import com.leon.detonator.bean.DetonatorInfoBean;

public class OfflineListFragment extends Fragment {
    private ListView listView;
    private OfflineListAdapter adapter;
    private CheckBox cbSelected;
    private MyButton btnDownload;
    private MyButton btnDelete;
    private MyButton btnImport;
    private View.OnClickListener clickDownload;
    private View.OnClickListener clickDelete;
    private View.OnClickListener clickImport;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offline_list, container, false);
        view.findViewById(R.id.table_title).setBackgroundColor(view.getContext().getColor(R.color.colorTableTitleBackground));
        listView = view.findViewById(R.id.lv_offline_list);
        cbSelected = view.findViewById(R.id.cb_selected);
        btnDownload = view.findViewById(R.id.btn_offline_download);
        btnDelete = view.findViewById(R.id.btn_delete_selected);
        btnImport = view.findViewById(R.id.btn_import);
        if (adapter != null) {
            listView.setAdapter(adapter);
            btnImport.setEnabled(adapter.getCount() > 0);
        }
        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            ((DetonatorInfoBean) adapter.getItem(i)).setSelected(!((DetonatorInfoBean) adapter.getItem(i)).isSelected());
            adapter.notifyDataSetChanged();
            checkStatus(false);
            cbSelected.setChecked(true);
            for (int j = 0; j < adapter.getCount(); j++) {
                if (!((DetonatorInfoBean) adapter.getItem(j)).isSelected()) {
                    cbSelected.setChecked(false);
                    break;
                }
            }
        });
        cbSelected.setOnClickListener(v -> {
            if (adapter.getCount() > 0) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    ((DetonatorInfoBean) adapter.getItem(i)).setSelected(cbSelected.isChecked());
                }
                adapter.notifyDataSetChanged();
                checkStatus(false);
            } else
                cbSelected.setChecked(false);
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

    public void setImportEnabled(boolean enabled) {
        new Handler(message -> {
            btnImport.setEnabled(enabled);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    private boolean checkSelected() {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (((DetonatorInfoBean) adapter.getItem(i)).isSelected())
                return true;
        }
        return false;
    }

    public void setCheckAll(boolean checked) {
        if (cbSelected != null)
            cbSelected.setChecked(checked);
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

    public View.OnClickListener getClickImport() {
        return clickImport;
    }

    public void setClickImport(View.OnClickListener listener) {
        this.clickImport = listener;
        new Handler(message -> {
            btnImport.setOnClickListener(clickImport);
            return false;
        }).sendEmptyMessageDelayed(1, 100);
    }

    public boolean getImportEnabled(){
        return btnImport.isEnabled();
    }

    public boolean getDeleteEnabled(){
        return btnDelete.isEnabled();
    }

    public boolean getDownloadEnabled(){
        return  btnDownload.isEnabled();
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
