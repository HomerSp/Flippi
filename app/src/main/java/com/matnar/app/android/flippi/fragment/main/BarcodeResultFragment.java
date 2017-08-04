package com.matnar.app.android.flippi.fragment.main;

import android.animation.Animator;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;
import com.matnar.app.android.flippi.barcode.BarcodeProvider;
import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;
import com.matnar.app.android.flippi.util.AnimationUtil;
import com.matnar.app.android.flippi.view.adapter.PriceCheckAdapter;
import com.matnar.app.android.flippi.view.decoration.PriceCheckDecoration;

import java.lang.ref.WeakReference;

public class BarcodeResultFragment extends MainActivity.MainActivityFragment {
    private static final String TAG = "Flippi." + BarcodeResultFragment.class.getSimpleName();

    private View mView;

    private int mShortAnimationDuration;
    private int mRevealAnimationDuration;

    private View mContainerView;
    private View mLoadingView;
    private TextView mNoResultsView;
    private View mErrorView;
    private RecyclerView mResultsView;
    private PriceCheckAdapter mResultsAdapter;

    private Snackbar mSnackbar = null;

    private String mQuery = null;
    private boolean mIsBarcode = false;
    private String mQueryPending = null;
    private PriceCheckProvider.PriceCheckItems mResults = new PriceCheckProvider.PriceCheckItems();
    private int mCurrentPage = 0;

    public BarcodeResultFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            super.setFooter(0);
            super.setFabIcon(R.drawable.ic_fab_camera);
            super.showClearFavorites(false);
            super.showSearchItem(true);
            super.setToolbarScroll(true);
        } catch(IllegalStateException e) {
            Log.e(TAG, "Create view error", e);
            return null;
        }

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mRevealAnimationDuration = getResources().getInteger(R.integer.reveal_anim_duration);

        mView = inflater.inflate(R.layout.fragment_main_search_result, container, false);

        if(savedInstanceState == null && getArguments().containsKey("cx")) {
            mView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                                           int oldRight, int oldBottom) {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        return;
                    }

                    v.removeOnLayoutChangeListener(this);
                    int cx = getArguments().getInt("cx");
                    int cy = getArguments().getInt("cy");

                    // get the hypothenuse so the radius is from one corner to the other
                    int radius = (int) Math.hypot(right, bottom);

                    Animator reveal = null;
                    reveal = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, radius);
                    reveal.setInterpolator(new DecelerateInterpolator(2f));
                    reveal.setDuration(mRevealAnimationDuration);
                    reveal.start();
                }
            });
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        mContainerView = mView.findViewById(R.id.search_container);
        mLoadingView = mContainerView.findViewById(R.id.search_loading_progress);
        mNoResultsView = (TextView) mContainerView.findViewById(R.id.search_noresults_text);
        mErrorView = mContainerView.findViewById(R.id.search_error_layout);
        mErrorView.findViewById(R.id.search_error_retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BarcodeResultFragment.this.doSearch(mQuery, mIsBarcode);
            }
        });

        mResultsView = (RecyclerView) mView.findViewById(R.id.search_results);
        mResultsView.setLayoutManager(layoutManager);
        mResultsView.setItemAnimator(new DefaultItemAnimator());
        mResultsView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(getContext() == null) {
                    return;
                }

                try {
                    if (dy > 0) {
                        BarcodeResultFragment.super.showFab(false);
                    } else if (dy < 0) {
                        BarcodeResultFragment.super.showFab(true);
                    }
                } catch(IllegalStateException e) {
                    Log.e(TAG, "Show fab error", e);
                }
            }
        });

        if(savedInstanceState != null) {
            mQuery = savedInstanceState.getString("query");
            mIsBarcode = savedInstanceState.getBoolean("is_barcode");
            mQueryPending = savedInstanceState.getString("query_pending", null);
            mResults.addAll(savedInstanceState.getParcelable("results"));
            mCurrentPage = savedInstanceState.getInt("current_page");
        } else {
            mQuery = getArguments().getString("query");
            mIsBarcode = getArguments().getBoolean("is_barcode");
        }

        mResultsAdapter = new PriceCheckAdapter(mResultsView, mResults, false);
        mResultsAdapter.setOnLoadMoreListener(new PriceCheckAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if(mCurrentPage < mResults.getPages()) {
                    doSearchPage(mCurrentPage + 1);
                }
            }
        });

        mResultsAdapter.setOnStarredListener(new PriceCheckAdapter.OnStarredListener() {
            @Override
            public void onStarred(final PriceCheckProvider.PriceCheckItem item, final boolean starred) {
                try {
                    item.setSaved(starred);
                    new PriceCheckDatabase.PriceCheckUpdateTask(BarcodeResultFragment.super.getPriceCheckDatabase(), item, 0).execute();
                } catch(IllegalStateException e) {
                    Log.e(TAG, "Setting favourites error", e);
                    return;
                }

                if(starred) {
                    if(mSnackbar != null) {
                        mSnackbar.dismiss();
                        mSnackbar = null;
                    }

                    mSnackbar = Snackbar.make(getActivity().findViewById(R.id.mainCoordinatorLayout), getActivity().getString(R.string.search_row_starred_undo), Snackbar.LENGTH_LONG);
                    mSnackbar.setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                item.setSaved(false);
                                mResultsAdapter.notifyItemChanged(item);
                                new PriceCheckDatabase.PriceCheckUpdateTask(BarcodeResultFragment.super.getPriceCheckDatabase(), item, 0).execute();
                            } catch(IllegalStateException e) {
                                Log.e(TAG, "Undo favourite error", e);
                            }
                        }
                    });

                    mSnackbar.show();
                } else {
                    if(mSnackbar != null) {
                        mSnackbar.dismiss();
                        mSnackbar = null;
                    }

                    mSnackbar = Snackbar.make(getActivity().findViewById(R.id.mainCoordinatorLayout), getActivity().getString(R.string.search_row_unstarred_undo), Snackbar.LENGTH_LONG);
                    mSnackbar.setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                item.setSaved(true);
                                mResultsAdapter.notifyItemChanged(item);
                                new PriceCheckDatabase.PriceCheckUpdateTask(BarcodeResultFragment.super.getPriceCheckDatabase(), item, 0).execute();
                            } catch(IllegalStateException e) {
                                Log.e(TAG, "Undo unfavourite error", e);
                            }
                        }
                    });
                    mSnackbar.show();
                }
            }
        });

        if(savedInstanceState != null) {
            mResultsAdapter.setHaveMoreItems(mCurrentPage < mResults.getPages());
        }

        mResultsView.setAdapter(mResultsAdapter);

        PriceCheckDecoration dividerItemDecoration = new PriceCheckDecoration(getContext(),
                layoutManager.getOrientation());
        mResultsView.addItemDecoration(dividerItemDecoration);

        try {
            if (savedInstanceState == null || mCurrentPage == 0) {
                doSearch(mQuery, mIsBarcode);
            } else {
                update();
            }
        } catch(IllegalStateException e) {
            Log.e(TAG, "Create view error", e);
        }

        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("query", mQuery);
        outState.putBoolean("is_barcode", mIsBarcode);
        if(mQueryPending != null) {
            outState.putString("query_string", mQueryPending);
        }

        outState.putParcelable("results", mResults);
        outState.putInt("current_page", mCurrentPage);
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    public void doSearch(final String query, final boolean isBarcode) {
        mQuery = query;
        mIsBarcode = isBarcode;
        mQueryPending = null;
        mCurrentPage = 0;
        mResults.clear();
        mResultsAdapter.setHaveMoreItems(true);

        AnimationUtil.animateHide(mResultsView, mShortAnimationDuration);
        AnimationUtil.animateHide(mErrorView, mShortAnimationDuration);
        AnimationUtil.animateHide(mNoResultsView, mShortAnimationDuration);
        AnimationUtil.animateShow(mLoadingView, mShortAnimationDuration);
        AnimationUtil.animateShow(mContainerView, mShortAnimationDuration);

        doSearchPage(mCurrentPage);
    }

    private void doSearchPage(final int page) {
        final WeakReference<Context> context = new WeakReference<>(getContext());
        PriceCheckProvider.PriceCheckListener listener = new PriceCheckProvider.PriceCheckListener() {
            @Override
            public void onResult(PriceCheckProvider.PriceCheckItems results) {
                if(getContext() == null) {
                    return;
                }

                mResults.addAll(results);
                if (results.size() > 0) {
                    mCurrentPage = page;
                    if(page == 0) {
                        mCurrentPage = 1;
                    }

                    update();
                } else {
                    BarcodeProvider.getInformation(context, mQuery, new BarcodeProvider.BarcodeListener() {
                        @Override
                        public void onResult(BarcodeProvider.BarcodeInformation result) {
                            if (result != null) {
                                mQueryPending = result.getName();
                                BarcodeResultFragment.super.setSearchQuery(mQueryPending);
                                BarcodeResultFragment.this.doSearch(mQueryPending, false);
                            } else {
                                update();
                            }
                        }
                    });
                }

                mResultsAdapter.setHaveMoreItems(mCurrentPage < results.getPages());
            }
        };

        try {
            if (page > 0) {
                PriceCheckProvider.getInformation(context, mQuery, page, super.getPriceCheckDatabase(), listener);
            } else {
                PriceCheckProvider.getInformation(context, mQuery, super.getPriceCheckDatabase(), listener);
            }
        } catch(IllegalStateException e) {
            Log.e(TAG, "Get price information error", e);
        }
    }

    private void update() {
        if(mResults.size() > 0) {
            AnimationUtil.animateHide(mContainerView, mShortAnimationDuration);
            AnimationUtil.animateHide(mResultsView, mShortAnimationDuration);
            AnimationUtil.animateHide(mLoadingView, mShortAnimationDuration);
            AnimationUtil.animateHide(mNoResultsView, mShortAnimationDuration);
            AnimationUtil.animateShow(mErrorView, mShortAnimationDuration);
            AnimationUtil.animateShow(mResultsView, mShortAnimationDuration);
        } else {
            boolean error = mResults.hasError();
            String noResultsText;
            if (mIsBarcode) {
                noResultsText = getContext().getString((error) ? R.string.search_error_barcode : R.string.search_noresults_barcode, mQuery);
            } else {
                noResultsText = getContext().getString((error) ? R.string.search_error : R.string.search_noresults, mQuery);
            }

            if(error) {
                ((TextView) mErrorView.findViewById(R.id.search_error_text)).setText(noResultsText);
            } else {
                ((TextView) mNoResultsView.findViewById(R.id.search_noresults_text)).setText(noResultsText);
            }

            mNoResultsView.findViewById(R.id.search_noresults_text).setVisibility((error) ? View.GONE : View.VISIBLE);

            AnimationUtil.animateHide(mResultsView, mShortAnimationDuration);
            AnimationUtil.animateHide(mLoadingView, mShortAnimationDuration);
            if(error) {
                AnimationUtil.animateHide(mNoResultsView, mShortAnimationDuration);
                AnimationUtil.animateShow(mErrorView, mShortAnimationDuration);
            } else {
                AnimationUtil.animateHide(mErrorView, mShortAnimationDuration);
                AnimationUtil.animateShow(mNoResultsView, mShortAnimationDuration);
            }
            AnimationUtil.animateShow(mContainerView, mShortAnimationDuration);
        }

        mResultsAdapter.notifyDataSetChanged();
        mResultsAdapter.setLoaded();
    }
}