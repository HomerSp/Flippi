package com.matnar.app.android.flippi.fragment.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends MainActivity.MainActivityFragment implements ViewPager.OnPageChangeListener {
    private static final String TAG = "Flippi." + MainFragment.class.getSimpleName();

    private ViewPager mView;
    private PagerAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new MainFragmentAdapter(getChildFragmentManager());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = (ViewPager) inflater.inflate(R.layout.fragment_main, container, false);
        mView.setAdapter(mAdapter);

        return mView;
    }

    @Override
    public boolean onBackPressed() {
        if (mView.getCurrentItem() > 0) {
            // Otherwise, select the previous step.
            mView.setCurrentItem(mView.getCurrentItem() - 1);
            return true;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mView != null) {
            mView.removeOnPageChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mView != null) {
            mView.addOnPageChangeListener(this);
            onPageSelected(mView.getCurrentItem());
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                super.showSearchItem(false);
                super.showAppBarSearch(false);
                super.setActionBarTitle(getString(R.string.app_name));
                super.resetActionBar();

                break;
            case 1:
                super.showSearchItem(true);
                super.setActionBarTitle(getString(R.string.search_row_filter_no));

                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class MainFragmentAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> mFragments = new ArrayList<>();

        MainFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: {
                    return new MainContentFragment();
                }
                case 1: {
                    SearchResultFragment fragment = new SearchResultFragment();

                    Bundle args = new Bundle();
                    args.putBoolean("is_category", true);

                    fragment.setArguments(args);
                    return fragment;
                }
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
