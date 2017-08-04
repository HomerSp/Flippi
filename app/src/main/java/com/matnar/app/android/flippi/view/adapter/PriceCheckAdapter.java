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
import com.matnar.app.android.flippi.pricecheck.PriceCheckProvider;
import com.matnar.app.android.flippi.pricecheck.PriceCheckRegion;
import com.matnar.app.android.flippi.view.widget.SearchResultLayout;
import com.squareup.picasso.Picasso;

public class PriceCheckAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "Flippi." + PriceCheckAdapter.class.getSimpleName();

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;

    private PriceCheckProvider.PriceCheckItems mList;
    private boolean mIsSavedList;
    private OnLoadMoreListener mLoadMoreListener;
    private OnStarredListener mOnStarredListener;
    private OnSortListener mOnSortListener;

    private boolean mHaveMoreItems = true;
    private boolean mIsLoading = true;
    private int mVisibleThreshold = 5;
    private int mLastVisibleItem, mTotalItemCount;

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        private boolean mIsSavedList;

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
            }
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
                if (mHaveMoreItems && !mIsLoading && mTotalItemCount <= (mLastVisibleItem + mVisibleThreshold)) {
                    if (mLoadMoreListener != null) {
                        mLoadMoreListener.onLoadMore();
                    }

                    mIsLoading = true;
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
        }

        throw new RuntimeException();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof PriceCheckViewHolder && position >= 1 && position < mList.size() + 1) {
            PriceCheckProvider.PriceCheckItem info = mList.get(position - 1);
            ((PriceCheckViewHolder) holder).setInfo(info);
        } else if(holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).setLoading(mIsLoading);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size() + 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
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

    public void setHaveMoreItems(boolean b) {
        mHaveMoreItems = b;
    }

    public void setLoaded() {
        mIsLoading = false;

        for(int i = 0; i < getItemCount(); i++) {
            if(getItemViewType(i) == TYPE_FOOTER) {
                notifyItemChanged(i);
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

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnStarredListener {
        void onStarred(PriceCheckProvider.PriceCheckItem item, boolean starred);
    }

    public interface OnSortListener {
        void onSort(String type);
    }

}
