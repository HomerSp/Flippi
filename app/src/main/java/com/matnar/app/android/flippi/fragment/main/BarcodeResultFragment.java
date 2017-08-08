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

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;
import com.matnar.app.android.flippi.barcode.BarcodeProvider;
import com.matnar.app.android.flippi.db.CategoryDatabase;
import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.PriceCheckCategories;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.view.adapter.PriceCheckAdapter;
import com.matnar.app.android.flippi.view.decoration.PriceCheckDecoration;

import java.lang.ref.WeakReference;
import java.util.Random;

public class BarcodeResultFragment extends MainActivity.MainActivityFragment {
    private static final String TAG = "Flippi." + BarcodeResultFragment.class.getSimpleName();

    private View mView;

    private int mShortAnimationDuration;
    private int mRevealAnimationDuration;

    private RecyclerView mResultsView;
    private PriceCheckAdapter mResultsAdapter;

    private Snackbar mSnackbar = null;

    private PriceCheckCategories mCategories = new PriceCheckCategories();

    private String mQuery = null;
    private boolean mIsBarcode = false;
    private String mQueryPending = null;
    private PriceCheckProvider.PriceCheckItems mResults = new PriceCheckProvider.PriceCheckItems();
    private int mCurrentPage = 0;
    private String mFilter;
    private String mSort;

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
            mCategories.addAll(savedInstanceState.getParcelable("categories"));
            mFilter = savedInstanceState.getString("filter");
            mSort = savedInstanceState.getString("sort");
        } else {
            mQuery = getArguments().getString("query");
            mIsBarcode = getArguments().getBoolean("is_barcode");
        }

        if(mIsBarcode) {
            try {
                super.setSearchQuery("");
            } catch(IllegalStateException e) {
                Log.e(TAG, "Setting search error", e);
            }
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
                    new PriceCheckDatabase.UpdateTask(BarcodeResultFragment.super.getPriceCheckDatabase(), item, 0).execute();
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
                                new PriceCheckDatabase.UpdateTask(BarcodeResultFragment.super.getPriceCheckDatabase(), item, 0).execute();
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
                                new PriceCheckDatabase.UpdateTask(BarcodeResultFragment.super.getPriceCheckDatabase(), item, 0).execute();
                            } catch(IllegalStateException e) {
                                Log.e(TAG, "Undo unfavourite error", e);
                            }
                        }
                    });
                    mSnackbar.show();
                }
            }
        });

        mResultsAdapter.setOnFilterListener(new PriceCheckAdapter.OnFilterListener() {
            @Override
            public void onFilter(String filter) {
                mFilter = filter;
                doSearch(mQuery, mIsBarcode);
            }
        });

        mResultsAdapter.setOnRetryListener(new PriceCheckAdapter.OnRetryListener() {
            @Override
            public void onRetry() {
                BarcodeResultFragment.this.doSearch(mQuery, mIsBarcode);
            }
        });

        mResultsAdapter.setOnSortListener(new PriceCheckAdapter.OnSortListener() {
            @Override
            public void onSort(String type) {
                mSort = type;
                doSearch(mQuery, mIsBarcode);
            }
        });

        if(savedInstanceState != null) {
            mResultsAdapter.setHaveMoreItems(mCurrentPage < mResults.getPages());
            mResultsAdapter.setCategories(mCategories);
            mResultsAdapter.setFilter(mFilter);
        }

        mResultsView.setAdapter(mResultsAdapter);

        PriceCheckDecoration dividerItemDecoration = new PriceCheckDecoration(getContext(),
                layoutManager.getOrientation());
        mResultsView.addItemDecoration(dividerItemDecoration);

        try {
            if (savedInstanceState == null || mCurrentPage == 0) {
                if(mCategories.size() == 0) {
                    new CategoryDatabase.GetAllTask(getCategoryDatabase(), "cex")
                            .setResultListener(new CategoryDatabase.GetAllListener() {
                                @Override
                                public void onResult(PriceCheckCategories results) {
                                    mCategories.addAll(results);
                                    mResultsAdapter.setCategories(mCategories);
                                    doSearch(mQuery, mIsBarcode);
                                }
                            })
                            .execute();
                } else {
                    doSearch(mQuery, mIsBarcode);
                }
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
        outState.putParcelable("categories", mCategories);
        if(mFilter != null) {
            outState.putString("filter", mFilter);
        }
        if(mSort != null) {
            outState.putString("sort", mSort);
        }
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
        mResultsAdapter.setQuery(mQuery, mIsBarcode);
        mResultsAdapter.setLoading(true);
        mResultsAdapter.setNoResults(false);
        mResultsAdapter.setError(false);
        mResultsAdapter.notifyDataSetChanged();

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

                int previousSize = mResults.size();
                mResults.addAll(results);

                mResultsAdapter.setLoading(false);
                if(mResults.hasError()) {
                    mResultsAdapter.setError(true);
                } else {
                    mResultsAdapter.setNoResults(mResults.size() == 0);
                }

                if (results.size() > 0 && !mResults.hasError()) {
                    if(mResults.size() > 4) {
                        int r = previousSize + (((results.size() / 2) - (results.size() / 4)) + new Random().nextInt(results.size() / 2));
                        if(r >= 0 && r < mResults.size()) {
                            mResults.add(r, new PriceCheckProvider.AdItem());
                        }
                    }

                    mCurrentPage = page;
                    if(page == 0) {
                        mCurrentPage = 1;
                    }

                    update();

                    mResultsAdapter.setHaveMoreItems(mCurrentPage < results.getPages());
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
            }
        };

        try {
            if (page > 0) {
                PriceCheckProvider.getInformation(context, mQuery, page, super.getPriceCheckDatabase(), mFilter, mSort, listener);
            } else {
                PriceCheckProvider.getInformation(context, mQuery, super.getPriceCheckDatabase(), mFilter, mSort, listener);
            }
        } catch(IllegalStateException e) {
            Log.e(TAG, "Get price information error", e);
        }
    }

    private void update() {
        mResultsAdapter.notifyDataSetChanged();
        mResultsAdapter.setLoadedMore();
    }
}