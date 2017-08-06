package com.matnar.app.android.flippi.view.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.pricecheck.PriceCheckCategories;
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;
import com.matnar.app.android.flippi.view.widget.PriceCheckFilterSpinner;
import com.squareup.okhttp.internal.framed.Header;
import com.squareup.picasso.Picasso;

public class PriceCheckAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "Flippi." + PriceCheckAdapter.class.getSimpleName();

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;
    private static final int TYPE_LOADING = 3;
    private static final int TYPE_NORESULTS = 4;
    private static final int TYPE_ERROR = 5;

    private PriceCheckProvider.PriceCheckItems mList;
    private boolean mIsSavedList;

    private PriceCheckCategories mCategories = null;

    private OnLoadMoreListener mLoadMoreListener;
    private OnStarredListener mOnStarredListener;
    private OnSortListener mOnSortListener;
    private OnFilterListener mOnFilterListener;
    private OnRetryListener mOnRetryListener;

    private String mTempFilter;

    private String mQuery;
    private boolean mIsBarcode = false;
    private boolean mIsLoading = false;
    private boolean mNoResults = false;
    private boolean mError = false;

    private boolean mHaveMoreItems = true;
    private boolean mIsLoadingMore = false;
    private int mVisibleThreshold = 5;
    private int mLastVisibleItem, mTotalItemCount;

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private boolean mIsSavedList;

        private PriceCheckFilterSpinner mSpinner;
        private PriceCheckFilterAdapter mCategoriesAdapter;

        HeaderViewHolder(View itemView, boolean isSavedList) {
            super(itemView);

            mIsSavedList = isSavedList;
            if(isSavedList) {
                final String[] sortKeys = itemView.getResources().getStringArray(R.array.favorites_sort_key);

                AppCompatSpinner spinner = (AppCompatSpinner) itemView.findViewById(R.id.saved_row_header_sort);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(itemView.getContext(),
                        R.array.favorites_sort, R.layout.saved_list_row_header_sort);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                        if(mOnSortListener != null) {
                            mOnSortListener.onSort(sortKeys[pos]);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            } else {
                mSpinner = (PriceCheckFilterSpinner) itemView.findViewById(R.id.search_row_filter);

                mCategoriesAdapter = new PriceCheckFilterAdapter(itemView.getContext(), mSpinner);
                mSpinner.setAdapter(mCategoriesAdapter);
                mSpinner.setOnItemSelectedListener(new PriceCheckFilterSpinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(String name) {
                        if(mOnFilterListener != null) {
                            mOnFilterListener.onFilter(name);
                        }
                    }

                    @Override
                    public void onNothingSelected() {
                        if(mOnFilterListener != null) {
                            mOnFilterListener.onFilter("");
                        }
                    }
                });
            }
        }

        void setCategories(PriceCheckCategories categories) {
            if(categories == null) {
                return;
            }

            mCategoriesAdapter.setItems(categories);
            mCategoriesAdapter.notifyDataSetChanged();
        }

        void setFilter(String filter) {
            mCategoriesAdapter.setFilter(filter);
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        private View mLoadingView;
        private boolean mIsSavedList;

        FooterViewHolder(View itemView, boolean isSavedList) {
            super(itemView);

            mIsSavedList = isSavedList;
            if(!mIsSavedList) {
                mLoadingView = itemView.findViewById(R.id.footer_loading);
            }
        }

        void setLoading(boolean b) {
            if(!mIsSavedList) {
                mLoadingView.setVisibility((b) ? View.VISIBLE : View.GONE);
            }
        }
    }

    private class PriceCheckViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private PriceCheckProvider.PriceCheckItem mInfo;
        private FrameLayout mView;
        private boolean mIsSavedList;

        private TextView mTitleView;
        private ImageView mImageView;
        private TextView mCategoryView;
        private TextView mSellPriceView;
        private TextView mCashPriceView;
        private TextView mVoucherPriceView;
        private ImageView mStarView;

        PriceCheckViewHolder(View itemView, boolean isSavedList) {
            super(itemView);

            mView = (FrameLayout) itemView;
            mIsSavedList = isSavedList;

            mTitleView = (TextView) itemView.findViewById(R.id.search_row_title);
            mImageView = (ImageView) itemView.findViewById(R.id.search_row_image);
            mCategoryView = (TextView) itemView.findViewById(R.id.search_row_category);
            mSellPriceView = (TextView) itemView.findViewById(R.id.search_row_sellprice);
            mCashPriceView = (TextView) itemView.findViewById(R.id.search_row_cashprice);
            mVoucherPriceView = (TextView) itemView.findViewById(R.id.search_row_voucherprice);
            mStarView = (ImageView) itemView.findViewById(R.id.search_row_star);
        }

        public void setInfo(PriceCheckProvider.PriceCheckItem info) {
            Context context = mTitleView.getContext();

            mInfo = info;

            if(mIsSavedList) {
                mView.setForeground((info.isSaved()) ? new ColorDrawable(Color.argb(0, 0, 0, 0)) : new ColorDrawable(Color.argb(170, 240, 240, 240)));
            }

            mTitleView.setText(info.getName());
            mCategoryView.setText(info.getCategory());

            PriceCheckRegion.Region region = mInfo.getRegion();
            mSellPriceView.setText(context.getString(R.string.search_row_sellprice, PriceCheckRegion.getPrice(region, info.getSellPrice())));
            mCashPriceView.setText(PriceCheckRegion.getPrice(region, info.getBuyPrice()));
            mVoucherPriceView.setText(PriceCheckRegion.getPrice(region, info.getBuyVoucherPrice()));

            mStarView.setTag(info.isSaved());
            mStarView.setImageResource((info.isSaved()) ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
            mStarView.setOnClickListener(this);

            Picasso.with(context).load(info.getImage()).into(mImageView);
        }

        @Override
        public void onClick(View view) {
            if(view.equals(mStarView)) {
                boolean starred = !(Boolean) mStarView.getTag();
                mStarView.setTag(starred);
                mStarView.setImageResource((starred) ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
                if(mOnStarredListener != null) {
                    mOnStarredListener.onStarred(mInfo, starred);
                }
            }
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class NoResultsViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        NoResultsViewHolder(View itemView) {
            super(itemView);

            mTextView = (TextView) itemView.findViewById(R.id.search_noresults_text);
        }

        void setQuery() {
            mTextView.setText(R.string.savedlist_noresults);
        }

        void setQuery(String query, boolean isBarcode) {
            mTextView.setText(mTextView.getContext().getString((isBarcode) ? R.string.search_noresults_barcode : R.string.search_noresults, query));
        }
    }

    private static class ErrorViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        ErrorViewHolder(View itemView, final OnRetryListener listener) {
            super(itemView);

            mTextView = (TextView) itemView.findViewById(R.id.search_error_text);
            itemView.findViewById(R.id.search_error_retry).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null) {
                        listener.onRetry();
                    }
                }
            });
        }

        void setQuery() {
            mTextView.setText(R.string.savedlist_error);
        }

        void setQuery(String query, boolean isBarcode) {
            mTextView.setText(mTextView.getContext().getString((isBarcode) ? R.string.search_error_barcode : R.string.search_error, query));
        }
    }

    public PriceCheckAdapter(final RecyclerView recyclerView, PriceCheckProvider.PriceCheckItems list, boolean isSavedList) {
        mList = list;
        mIsSavedList = isSavedList;

        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mTotalItemCount = linearLayoutManager.getItemCount();
                mLastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (mList.size() > 0 && mHaveMoreItems && !mIsLoadingMore && mTotalItemCount <= (mLastVisibleItem + mVisibleThreshold)) {
                    if (mLoadMoreListener != null) {
                        mLoadMoreListener.onLoadMore();
                    }

                    mIsLoadingMore = true;
                    for(int i = 0; i < getItemCount(); i++) {
                        if(getItemViewType(i) == TYPE_FOOTER) {
                            final int index = i;
                            recyclerView.post(new Runnable() {
                                @Override
                                public void run() {
                                    notifyItemChanged(index);
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_ITEM) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_row, parent, false);

            return new PriceCheckViewHolder(itemView, mIsSavedList);
        } else if(viewType == TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate((mIsSavedList) ? R.layout.saved_list_row_header : R.layout.search_result_row_header, parent, false);

            return new HeaderViewHolder(itemView, mIsSavedList);
        } else if(viewType == TYPE_FOOTER) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate((mIsSavedList) ? R.layout.saved_list_row_footer : R.layout.search_result_row_footer, parent, false);

            return new FooterViewHolder(itemView, mIsSavedList);
        } else if(viewType == TYPE_LOADING) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_row_loading, parent, false);

            return new LoadingViewHolder(itemView);
        } else if(viewType == TYPE_NORESULTS) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_row_noresults, parent, false);

            return new NoResultsViewHolder(itemView);
        } else if(viewType == TYPE_ERROR) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_row_error, parent, false);

            return new ErrorViewHolder(itemView, mOnRetryListener);
        }

        throw new RuntimeException();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof PriceCheckViewHolder && position >= 1 && position < mList.size() + 1) {
            PriceCheckProvider.PriceCheckItem info = mList.get(position - 1);
            ((PriceCheckViewHolder) holder).setInfo(info);
        } else if(holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).setCategories(mCategories);
            if(mTempFilter != null) {
                ((HeaderViewHolder) holder).setFilter(mTempFilter);
                mTempFilter = null;
            }
        } else if(holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).setLoading(mIsLoadingMore);
        } else if(holder instanceof LoadingViewHolder) {
            // Nothing
        } else if(holder instanceof NoResultsViewHolder) {
            if(mIsSavedList) {
                ((NoResultsViewHolder) holder).setQuery();
            } else {
                ((NoResultsViewHolder) holder).setQuery(mQuery, mIsBarcode);
            }
        } else if(holder instanceof ErrorViewHolder) {
            if(mIsSavedList) {
                ((ErrorViewHolder) holder).setQuery();
            } else {
                ((ErrorViewHolder) holder).setQuery(mQuery, mIsBarcode);
            }
        }
    }

    @Override
    public int getItemCount() {
        if(mIsLoading || mNoResults || mError) {
            return 3;
        }

        return mList.size() + 2;
    }

    @Override
    public int getItemViewType(int position) {
        if(mIsLoading && position == 1) {
            return TYPE_LOADING;
        } else if(mNoResults && position == 1) {
            return TYPE_NORESULTS;
        } else if(mError && position == 1) {
            return TYPE_ERROR;
        } else if (position == 0) {
            return TYPE_HEADER;
        } else if(position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }

        return TYPE_ITEM;
    }

    public void notifyItemChanged(PriceCheckProvider.PriceCheckItem item) {
        super.notifyItemChanged(mList.indexOf(item) + 1);
    }

    public void notifyItemRemoved(PriceCheckProvider.PriceCheckItem item) {
        super.notifyItemRemoved(mList.indexOf(item) + 1);
    }

    public void setQuery(String query, boolean isBarcode) {
        mQuery = query;
        mIsBarcode = isBarcode;
    }

    public void setLoading(boolean l) {
        mIsLoading = l;
    }

    public void setNoResults(boolean b)  {
        mNoResults = b;
    }

    public void setError(boolean b) {
        mError = b;
    }

    public void setHaveMoreItems(boolean b) {
        mHaveMoreItems = b;
    }

    public void setLoadedMore() {
        mIsLoadingMore = false;

        for(int i = 0; i < getItemCount(); i++) {
            if(getItemViewType(i) == TYPE_FOOTER) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setCategories(PriceCheckCategories categories) {
        mCategories = categories;

        for(int i = 0; i < getItemCount(); i++) {
            if(getItemViewType(i) == TYPE_HEADER) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setFilter(String filter) {
        mTempFilter = filter;
        for(int i = 0; i < getItemCount(); i++) {
            if(getItemViewType(i) == TYPE_HEADER) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mLoadMoreListener = listener;
    }
    public void setOnStarredListener(OnStarredListener listener) {
        mOnStarredListener = listener;
    }
    public void setOnSortListener(OnSortListener listener) {
        mOnSortListener = listener;
    }
    public void setOnFilterListener(OnFilterListener listener) {
        mOnFilterListener = listener;
    }
    public void setOnRetryListener(OnRetryListener listener) {
        mOnRetryListener = listener;
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnStarredListener {
        void onStarred(PriceCheckProvider.PriceCheckItem item, boolean starred);
    }

    public interface OnSortListener {
        void onSort(String type);
    }

    public interface OnFilterListener {
        void onFilter(String filter);
    }

    public interface OnRetryListener {
        void onRetry();
    }
}
