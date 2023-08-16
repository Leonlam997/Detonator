package com.leon.detonator.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.component.TabFragment;
import com.leon.detonator.util.KeyUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ErrorDetonatorListActivity extends BaseActivity {
    private final int[] title = {R.string.tab_title_black_list, R.string.tab_title_used_list, R.string.tab_title_no_authorize_list};
    private List<Fragment> fragments;
    private TabLayout tabList;
    private ViewPager pagerList;
    private int[] listSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_detonator_list);

        setTitle(R.string.error_detonator);
        initPager();
    }

    private void initPager() {
        tabList = findViewById(R.id.tab_title);
        pagerList = findViewById(R.id.view_pager);
        fragments = new ArrayList<>();
        String[] list = new String[]{
                KeyUtils.KEY_ERROR_BLACK_LIST,
                KeyUtils.KEY_ERROR_USED_LIST,
                KeyUtils.KEY_ERROR_NOT_FOUND_LIST
        };
        listSize = new int[title.length];
        for (int i = 0; i < title.length; i++) {
            Bundle bundle = new Bundle();
            listSize[i] = getIntent().getParcelableArrayListExtra(list[i]).size();
            bundle.putParcelableArrayList(KeyUtils.KEY_LIST, getIntent().getParcelableArrayListExtra(list[i]));
            TabFragment tabFragment = new TabFragment();
            tabFragment.setArguments(bundle);
            fragments.add(tabFragment);
        }
        pagerList.setAdapter(new ListPagerAdapter(getSupportFragmentManager()));
        tabList.setupWithViewPager(pagerList);

        resetTabTitle(true);
        tabList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                resetTabTitle(false);
                pagerList.setCurrentItem(tab.getPosition());
                //adapterList.get(tab.getPosition()).notifyDataSetChanged();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void resetTabTitle(boolean init) {
        for (int i = 0; i < title.length; i++) {
            TextView tv = (TextView) LayoutInflater.from(ErrorDetonatorListActivity.this).inflate(R.layout.layout_tab_textview, tabList, false);
            tv.setText(String.format(Locale.getDefault(), "%s(%d)", getString(title[i]), listSize[i]));
            TabLayout.Tab tab = tabList.getTabAt(i);
            if (tab != null) {
                if (tab.isSelected())
                    tv.setTextColor(getColor(R.color.text_blue));
                else
                    tv.setTextColor(getColor(R.color.text_black));
                if (!init && tab.getCustomView() != null) {
                    final ViewParent customParent = tab.getCustomView().getParent();
                    if (customParent != null) {
                        ((ViewGroup) customParent).removeView(tab.getCustomView());
                    }
                }
                tab.setCustomView(tv);
            }
        }
    }


    private class ListPagerAdapter extends FragmentPagerAdapter {
        ListPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

    }
}
