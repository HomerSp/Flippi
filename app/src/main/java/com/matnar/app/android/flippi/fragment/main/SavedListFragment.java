package com.matnar.app.android.flippi.fragment.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;
import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;
import com.matnar.app.android.flippi.util.AnimationUtil;
import com.matnar.app.android.flippi.view.adapter.PriceCheckAdapter;
import com.matnar.app.android.flippi.view.decoration.PriceCheckDecoration;

import java.text.DecimalFormat;

public class SavedListFragment extends MainActivity.MainActivityFragment {
    private static final String TAG = "Flippi." + SavedListFragment.class.getSimpleName();

    private View mView;
    private View mFooter;

    private int mShortAnimationDuration;

    private View mContainerView;
    private View mLoadingView;
    private TextView mNoResultsView;
    private RecyclerView mResultsView;
    private PriceCheckAdapter mResultsAdapter;

    private PriceCheckProvider.PriceCheckItems mResults = new PriceCheckProvider.PriceCheckItems();
    private boolean mHaveResults = false;

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

        AppCompatSpinner spinner = (AppCompatSpinner) mView.findViewById(R.id.saved_row_header_sort);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mView.getContext(),
                R.array.favorites_sort, R.layout.saved_list_row_header_sort);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        mContainerView = mView.findViewById(R.id.search_container);
        mLoadingView = mContainerView.findViewById(R.id.search_loading_progress);
        mNoResultsView = (TextView)mContainerView.findViewById(R.id.search_noresults_text);
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
            mHaveResults = savedInstanceState.getBoolean("have_results");
        }

        mResultsAdapter = new PriceCheckAdapter(mResultsView, mResults, true);
        mResultsAdapter.setOnStarredListener(new PriceCheckAdapter.OnStarredListener() {
            @Override
            public void onStarred(final PriceCheckProvider.PriceCheckItem item, final boolean starred) {
                item.setSaved(starred);
                mResultsAdapter.notifyItemChanged(item);

                try {
                    new PriceCheckDatabase.PriceCheckUpdateTask(SavedListFragment.super.getPriceCheckDatabase(), item, 0).execute();
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
                                new PriceCheckDatabase.PriceCheckUpdateTask(SavedListFragment.super.getPriceCheckDatabase(), item, 0).execute();
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
                                new PriceCheckDatabase.PriceCheckUpdateTask(SavedListFragment.super.getPriceCheckDatabase(), item, 0).execute();
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

                            mResultsAdapter.notifyItemRemoved(item);
                            mResults.remove(item);
                            if(mResults.size() == 0) {
                                update();
                            }
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
                if(mResultsView.getVisibility() == View.VISIBLE) {
                    mResultsView.clearAnimation();
                    mResultsView.animate()
                            .alpha(0.0f)
                            .setDuration(mShortAnimationDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mResults.sort(type);
                                    mResultsAdapter.notifyDataSetChanged();
                                    mResultsView.clearAnimation();
                                    mResultsView.animate()
                                            .alpha(1.0f)
                                            .setDuration(mShortAnimationDuration)
                                            .setListener(null);
                                }
                            });
                } else {
                    mResults.sort(type);
                    mResultsAdapter.notifyDataSetChanged();
                }
            }
        });

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
            new PriceCheckDatabase.PriceCheckClearTask(super.getPriceCheckDatabase(), 0)
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

                                            new PriceCheckDatabase.PriceCheckUpdateTask(SavedListFragment.super.getPriceCheckDatabase(), mResults, 0).execute();
                                        } catch (IllegalStateException e) {
                                            Log.e(TAG, "Undo clear favourites error", e);
                                        }
                                    }
                                });


                                Snackbar.Callback cb = new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar transientBottomBar, @DismissEvent int event) {
                                        int i = 0;
                                        do {
                                            PriceCheckProvider.PriceCheckItem item = mResults.get(i);
                                            if(item.isSaved()) {
                                                continue;
                                            }

                                            mResultsAdapter.notifyItemRemoved(item);
                                            mResults.remove(item);
                                            i--;
                                        } while(++i < mResults.size());

                                        update();
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

        AnimationUtil.animateHide(mResultsView, mShortAnimationDuration);
        AnimationUtil.animateHide(mNoResultsView, mShortAnimationDuration);
        AnimationUtil.animateShow(mLoadingView, mShortAnimationDuration);
        AnimationUtil.animateShow(mContainerView, mShortAnimationDuration);

        try {
            new PriceCheckDatabase.PriceCheckGetAllTask(super.getPriceCheckDatabase(), 0)
                    .setResultListener(new PriceCheckDatabase.GetAllListener() {
                        @Override
                        public void onResult(PriceCheckProvider.PriceCheckItems results) {
                            if (getContext() == null) {
                                return;
                            }

                            if (results.size() > 0) {
                                mResults.addAll(results);
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
        if(mResults.size() > 0) {
            StringBuilder s = new StringBuilder();

            AnimationUtil.animateHide(mContainerView, mShortAnimationDuration);
            AnimationUtil.animateHide(mLoadingView, mShortAnimationDuration);
            AnimationUtil.animateHide(mNoResultsView, mShortAnimationDuration);
            AnimationUtil.animateShow(mResultsView, mShortAnimationDuration);
        } else {
            mNoResultsView.setText(R.string.savedlist_noresults);

            AnimationUtil.animateHide(mLoadingView, mShortAnimationDuration);
            AnimationUtil.animateHide(mResultsView, mShortAnimationDuration);
            AnimationUtil.animateShow(mNoResultsView, mShortAnimationDuration);
            AnimationUtil.animateShow(mContainerView, mShortAnimationDuration);
        }

        updateTotals();

        mResultsAdapter.notifyDataSetChanged();
        mResultsAdapter.setLoaded();
        mHaveResults = true;
    }

    private void updateTotals() {
        double totalPrice = 0.0f;
        double totalVoucherPrice = 0.0f;
        for(PriceCheckProvider.PriceCheckItem item: mResults) {
            if(!item.isSaved()) {
                continue;
            }

            totalPrice += Double.parseDouble(item.getBuyPrice());
            totalVoucherPrice += Double.parseDouble(item.getBuyVoucherPrice());
        }

        TextView totalPriceView = (TextView) mFooter.findViewById(R.id.search_total_cashprice);
        totalPriceView.setText(PriceCheckRegion.getPrice(getContext(), new DecimalFormat("0.00").format(totalPrice)));

        TextView totalVoucherPriceView = (TextView) mFooter.findViewById(R.id.search_total_voucherprice);
        totalVoucherPriceView.setText(PriceCheckRegion.getPrice(getContext(), new DecimalFormat("0.00").format(totalVoucherPrice)));
    }
}