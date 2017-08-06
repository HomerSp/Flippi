package com.matnar.app.android.flippi.fragment.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;
import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;
import com.matnar.app.android.flippi.view.adapter.PriceCheckAdapter;
import com.matnar.app.android.flippi.view.decoration.PriceCheckDecoration;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SavedListFragment extends MainActivity.MainActivityFragment {
    private static final String TAG = "Flippi." + SavedListFragment.class.getSimpleName();

    private View mView;
    private View mFooter;

    private int mShortAnimationDuration;

    private RecyclerView mResultsView;
    private PriceCheckAdapter mResultsAdapter;

    private PriceCheckProvider.PriceCheckItems mResults = new PriceCheckProvider.PriceCheckItems();
    private boolean mHaveResults = false;
    private String mSort;

    private Snackbar mSnackbar = null;

    public SavedListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            mFooter = super.setFooter(R.layout.saved_list_footer);
            super.setFabIcon(R.drawable.ic_fab_camera);
            super.showClearFavorites(true);
            super.showSearchItem(false);
            super.setToolbarScroll(true);
        } catch(IllegalStateException e) {
            Log.e(TAG, "Create view error", e);
            return null;
        }

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mView = inflater.inflate(R.layout.fragment_main_saved_list, container, false);

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
                    if(dy > 0) {
                        SavedListFragment.super.showFab(false);
                    } else if(dy < 0) {
                        SavedListFragment.super.showFab(true);
                    }
                } catch(IllegalStateException e) {
                    Log.e(TAG, "Show fab error", e);
                }
            }
        });

        if(savedInstanceState != null) {
            mResults.addAll(savedInstanceState.getParcelable("results"));
            mSort = savedInstanceState.getString("sort");
            mHaveResults = savedInstanceState.getBoolean("have_results");
        }

        mResultsAdapter = new PriceCheckAdapter(mResultsView, mResults, true);
        mResultsAdapter.setOnStarredListener(new PriceCheckAdapter.OnStarredListener() {
            @Override
            public void onStarred(final PriceCheckProvider.PriceCheckItem item, final boolean starred) {
                item.setSaved(starred);
                mResultsAdapter.notifyItemChanged(item);

                try {
                    new PriceCheckDatabase.UpdateTask(SavedListFragment.super.getPriceCheckDatabase(), item, 0).execute();
                } catch(IllegalStateException e) {
                    Log.e(TAG, "Set favourite error", e);
                }

                if(starred) {
                    if(mSnackbar != null) {
                        mSnackbar.dismiss();
                    }

                    mSnackbar = Snackbar.make(getActivity().findViewById(R.id.mainCoordinatorLayout), getActivity().getString(R.string.search_row_starred_undo), Snackbar.LENGTH_LONG);
                    mSnackbar.setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                item.setSaved(false);
                                mResultsAdapter.notifyItemChanged(item);
                                new PriceCheckDatabase.UpdateTask(SavedListFragment.super.getPriceCheckDatabase(), item, 0).execute();
                            } catch(IllegalStateException e) {
                                Log.e(TAG, "Undo set favourite error", e);
                            }
                        }
                    });

                    mSnackbar.show();
                } else {
                    if(mSnackbar != null) {
                        mSnackbar.dismiss();
                    }

                    mSnackbar = Snackbar.make(getActivity().findViewById(R.id.mainCoordinatorLayout), getActivity().getString(R.string.search_row_unstarred_undo), Snackbar.LENGTH_LONG);
                    mSnackbar.setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                item.setSaved(true);
                                mResultsAdapter.notifyItemChanged(item);
                                new PriceCheckDatabase.UpdateTask(SavedListFragment.super.getPriceCheckDatabase(), item, 0).execute();
                            } catch(IllegalStateException e) {
                                Log.e(TAG, "Undo remove favourite error", e);
                            }
                        }
                    });

                    Snackbar.Callback cb = new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, @DismissEvent int event) {
                            if(item.isSaved()) {
                                return;
                            }

                            if(mResults.size() <= 1) {
                                mResultsAdapter.setNoResults(true);
                            }

                            mResultsAdapter.notifyItemRemoved(item);
                            mResults.remove(item);
                        }
                    };
                    mSnackbar.addCallback(cb);
                    mSnackbar.show();
                }

                updateTotals();
            }
        });
        mResultsAdapter.setOnSortListener(new PriceCheckAdapter.OnSortListener() {
            @Override
            public void onSort(final String type) {
                mSort = type;
                mResults.sort(type);
                mResultsAdapter.notifyDataSetChanged();
            }
        });

        if(mSort != null) {
            mResultsAdapter.setSort(mSort);
        }

        mResultsView.setAdapter(mResultsAdapter);

        PriceCheckDecoration dividerItemDecoration = new PriceCheckDecoration(getContext(),
                layoutManager.getOrientation());
        mResultsView.addItemDecoration(dividerItemDecoration);

        if(savedInstanceState == null || !mHaveResults) {
            doQuery();
        } else {
            update();
        }

        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("results", mResults);
        if(mSort != null) {
            outState.putString("sort", mSort);
        }
        outState.putBoolean("have_results", mHaveResults);
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    public void clearFavorites() {
        try {
            new PriceCheckDatabase.ClearTask(super.getPriceCheckDatabase(), 0)
                    .setResultListener(new PriceCheckDatabase.ClearListener() {
                        @Override
                        public void onResult(boolean result) {
                            if (!result && mResults.size() > 0) {
                                Log.e(TAG, "Could not clear favorites");
                                return;
                            }

                            if (mResults.size() > 0) {
                                for (PriceCheckProvider.PriceCheckItem item : mResults) {
                                    item.setSaved(false);
                                    mResultsAdapter.notifyItemChanged(item);
                                }

                                updateTotals();

                                if (mSnackbar != null) {
                                    mSnackbar.dismiss();
                                }

                                mSnackbar = Snackbar.make(getActivity().findViewById(R.id.mainCoordinatorLayout), getActivity().getString(R.string.search_row_unstarred_clear_undo), Snackbar.LENGTH_LONG);
                                mSnackbar.setAction(R.string.undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        try {
                                            for (PriceCheckProvider.PriceCheckItem item : mResults) {
                                                item.setSaved(true);
                                                mResultsAdapter.notifyItemChanged(item);
                                            }

                                            updateTotals();

                                            new PriceCheckDatabase.UpdateTask(SavedListFragment.super.getPriceCheckDatabase(), mResults, 0).execute();
                                        } catch (IllegalStateException e) {
                                            Log.e(TAG, "Undo clear favourites error", e);
                                        }
                                    }
                                });


                                Snackbar.Callback cb = new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar transientBottomBar, @DismissEvent int event) {
                                        List<PriceCheckProvider.PriceCheckItem> removals = new ArrayList<>();
                                        for(PriceCheckProvider.PriceCheckItem item: mResults) {
                                            if(item.isSaved()) {
                                                continue;
                                            }

                                            removals.add(item);
                                        }

                                        if(removals.size() == mResults.size()) {
                                            mResultsAdapter.setNoResults(true);
                                        }

                                        for(PriceCheckProvider.PriceCheckItem item: removals) {
                                            mResultsAdapter.notifyItemRemoved(item);
                                            mResults.remove(item);
                                        }
                                    }
                                };
                                mSnackbar.addCallback(cb);
                                mSnackbar.show();
                            }
                        }
                    })
                    .execute();
        } catch(IllegalStateException e) {
            Log.e(TAG, "Clear favorites error", e);
        }
    }

    public void doQuery() {
        mHaveResults = false;
        mResults.clear();
        mResultsAdapter.setHaveMoreItems(false);
        mResultsAdapter.setLoading(true);
        mResultsAdapter.setNoResults(false);
        mResultsAdapter.setError(false);
        mResultsAdapter.notifyDataSetChanged();

        try {
            new PriceCheckDatabase.GetAllTask(super.getPriceCheckDatabase(), 0)
                    .setResultListener(new PriceCheckDatabase.GetAllListener() {
                        @Override
                        public void onResult(PriceCheckProvider.PriceCheckItems results) {
                            if (getContext() == null) {
                                return;
                            }

                            if (results.size() > 0) {
                                mResults.addAll(results);
                            }

                            mResultsAdapter.setLoading(false);
                            if(mResults.hasError()) {
                                mResultsAdapter.setError(true);
                            } else {
                                mResultsAdapter.setNoResults(mResults.size() == 0);
                            }

                            update();
                        }
                    })
                    .execute();
        } catch(IllegalStateException e) {
            Log.e(TAG, "Retrieve favourites error", e);
        }
    }

    private void update() {
        updateTotals();

        mResultsAdapter.notifyDataSetChanged();
        mResultsAdapter.setLoadedMore();
        mHaveResults = true;
    }

    private void updateTotals() {
        double totalPrice = 0.0f;
        double totalVoucherPrice = 0.0f;
        for(PriceCheckProvider.PriceCheckItem item: mResults) {
            if(!item.isSaved()) {
                continue;
            }

            totalPrice += item.getBuyPrice();
            totalVoucherPrice += item.getBuyVoucherPrice();
        }

        TextView totalPriceView = (TextView) mFooter.findViewById(R.id.search_total_cashprice);
        totalPriceView.setText(PriceCheckRegion.getPrice(getContext(), totalPrice));

        TextView totalVoucherPriceView = (TextView) mFooter.findViewById(R.id.search_total_voucherprice);
        totalVoucherPriceView.setText(PriceCheckRegion.getPrice(getContext(), totalVoucherPrice));
    }
}