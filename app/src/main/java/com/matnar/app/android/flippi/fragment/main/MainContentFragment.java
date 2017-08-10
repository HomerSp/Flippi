package com.matnar.app.android.flippi.fragment.main;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;
import com.matnar.app.android.flippi.view.adapter.SavedSearchesAdapter;

public class MainContentFragment extends MainActivity.MainActivityFragment {
    private static final String TAG = "Flippi." + MainContentFragment.class.getSimpleName();

    private View mView;
    private AppCompatAutoCompleteTextView mSearchView;

    private AdView mAdView;

    private String mQuery;

    public MainContentFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            mQuery = savedInstanceState.getString("search_query", "");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            super.setFooter(0);
            super.setFabIcon(R.drawable.ic_fab_camera);
            super.showClearFavorites(false);
            super.showSearchItem(false);
            super.setToolbarScroll(true);
            super.setActionBarTitle(getString(R.string.app_name));
        } catch(IllegalStateException e) {
            Log.e(TAG, "Create view error", e);
            return null;
        }

        mView = inflater.inflate(R.layout.fragment_main_content, container, false);

        TextView appnameview = (TextView) mView.findViewById(R.id.main_fragment_appname);
        Typeface typeface = Typeface.createFromAsset(mView.getContext().getAssets(), "font/Lobster_1_4.otf");
        appnameview.setTypeface(typeface);

        final ImageView searchButton = (ImageView) mView.findViewById(R.id.search_button);
        mSearchView = (AppCompatAutoCompleteTextView) mView.findViewById(R.id.search_text);

        final SavedSearchesAdapter searchesAdapter = getSearchAdapter();
        mSearchView.setAdapter(searchesAdapter);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchesAdapter.add(mSearchView.getText().toString());
                searchesAdapter.notifyDataSetChanged();

                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                int cx  = rect.left + (view.getWidth() / 2);
                int cy = rect.top + (view.getHeight() / 2);

                mSearchView.clearFocus();
                final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

                try {
                    MainContentFragment.super.setSearchQuery(mSearchView.getText().toString());
                    MainContentFragment.super.doSearch(mSearchView.getText().toString(), false, cx, cy);
                } catch(IllegalStateException e) {
                    Log.e(TAG, "Search button click error", e);
                }
            }
        });

        mSearchView.setText(mQuery);

        mSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchButton.performClick();
                }

                return true;
            }
        });

        mAdView = (AdView) mView.findViewById(R.id.adView);

        try {
            addOnLicenseCheckListener(new MainActivity.OnLicenseCheckListener() {
                @Override
                public void onLicenseCheck(boolean result) {
                    if (!result) {
                        AdRequest adRequest = new AdRequest.Builder()
                                .addTestDevice("3C441A6A7C61691FFC3105E9E09B4122")
                                .addTestDevice("BE14CD0E5EDE94F247ED0588622D2B8E")
                                .build();
                        mAdView.loadAd(adRequest);
                    }
                }
            });
        } catch(IllegalStateException e) {
            Log.e(TAG, "Could not set listener", e);
        }

        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("search_query", mSearchView.getText().toString());
    }
}
