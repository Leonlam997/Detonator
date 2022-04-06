package com.leon.detonator.Fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.leon.detonator.Activity.PropsSettingsActivity;

public class PropsFragment extends Fragment {
    private final int layoutId;
    private final Context mContext;
    private final int page;

    public PropsFragment(int layoutId, Context mContext, int page) {
        this.layoutId = layoutId;
        this.mContext = mContext;
        this.page = page;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(layoutId, container, false);
        ((PropsSettingsActivity) mContext).fillElement(v, page);
        return v;
    }
}
