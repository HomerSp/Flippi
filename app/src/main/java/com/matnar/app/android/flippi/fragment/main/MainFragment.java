package com.matnar.app.android.flippi.fragment.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;

public class MainFragment extends MainActivity.MainActivityFragment {
    private static final String TAG = "Flippi." + MainFragment.class.getSimpleName();

    private ViewPager mView;
    private PagerAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = (ViewPager) inflater.inflate(R.layout.fragment_main, container, false);
        mAdapter = new MainFragmentAdapter(getChildFragmentManager());
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

    private class MainFragmentAdapter extends FragmentStatePagerAdapter {
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
                    BarcodeResultFragment fragment = new BarcodeResultFragment();

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
