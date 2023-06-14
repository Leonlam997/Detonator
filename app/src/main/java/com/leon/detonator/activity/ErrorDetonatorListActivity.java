package com.leon.detonator.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DetonatorListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.util.KeyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ErrorDetonatorListActivity extends BaseActivity {
    private String[] tabTitle;
    private List<List<DetonatorBean>> lists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_detonator_list);

        setTitle(R.string.error_detonator);
        lists = new ArrayList<>();
        List<DetonatorBean> list = getIntent().getParcelableArrayListExtra(KeyUtils.KEY_ERROR_BLACK_LIST);
        lists.add(list);
        list = getIntent().getParcelableArrayListExtra(KeyUtils.KEY_ERROR_USED_LIST);
        lists.add(list);
        list = getIntent().getParcelableArrayListExtra(KeyUtils.KEY_ERROR_NOT_FOUND_LIST);
        lists.add(list);
        initPager();
    }

    private void initPager() {
        tabTitle = new String[]{getString(R.string.tab_title_black_list), getString(R.string.tab_title_used_list), getString(R.string.tab_title_no_authorize_list)};
        ViewPager viewPager = findViewById(R.id.view_pager);
        ((PagerTabStrip) findViewById(R.id.pager_tab_strip)).setTabIndicatorColor(getColor(BaseApplication.settings.isTunnel() ? R.color.colorActionbarBackground : R.color.colorOpenAirBackground));
        viewPager.setAdapter(new ListPagerAdapter());
    }

    private class ListPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return lists.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            // 最简单解决 notifyDataSetChanged() 页面不刷新问题的方法
            return POSITION_NONE;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.layout_detonator_listview, container, false);
            ListView listView = view.findViewById(R.id.list);
            listView.setAdapter(new DetonatorListAdapter(container.getContext(), lists.get(position)));
            container.addView(listView);
            return listView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return String.format(Locale.getDefault(), "%s(%d)", tabTitle[position], lists.get(position).size());
        }
    }
}
