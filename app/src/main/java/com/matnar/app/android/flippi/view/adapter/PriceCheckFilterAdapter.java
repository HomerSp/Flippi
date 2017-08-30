package com.matnar.app.android.flippi.view.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.pricecheck.PriceCheckCategories;

import java.util.ArrayList;
import java.util.List;

public class PriceCheckFilterAdapter extends BaseAdapter {
    private static final String TAG = "Flippi." + PriceCheckFilterAdapter.class.getSimpleName();

    private static class FilterItemViewHolder {
        private TextView mName;

        FilterItemViewHolder(View view) {
            mName = (TextView) view.findViewById(R.id.header_name);
        }

        void setItem(FilterItem item) {
            if(item == null) {
                mName.setText(R.string.search_row_filter_no);
                return;
            }

            mName.setText(item.getName());
        }

        void setEnabled(boolean b) {
            mName.setEnabled(b);
        }
    }

    private static class FilterItemRowViewHolder {
        private View mHeader;
        private View mItem;

        private TextView mHeaderName;
        private TextView mItemName;

        private View mFilterExpand;

        private int mPosition;

        FilterItemRowViewHolder(View view) {
            mHeader = view.findViewById(R.id.header_layout);
            mItem = view.findViewById(R.id.row_layout);

            mHeaderName = (TextView) mHeader.findViewById(R.id.header_name);
            mItemName = (TextView) mItem.findViewById(R.id.filter_name);

            mFilterExpand = mItem.findViewById(R.id.filter_expand);
        }

        int getPosition() {
            return mPosition;
        }

        void setPosition(int pos) {
            mPosition = pos;
        }

        void setHeader(FilterItem item) {
            mHeader.setVisibility(View.VISIBLE);
            mItem.setVisibility(View.GONE);
            mHeaderName.setText(item.getName());
            mFilterExpand.setVisibility(View.GONE);
        }

        public void setItem(FilterItem item) {
            mHeader.setVisibility(View.GONE);
            mItem.setVisibility(View.VISIBLE);
            mItemName.setText(item.getName());
            mFilterExpand.setVisibility((item.getChildSize() > 0) ? View.VISIBLE : View.GONE);
        }
    }

    private Context mContext;
    private AppCompatSpinner mSpinner;
    private List<FilterItem> mItems = new ArrayList<>();
    private FilterItem mCurrent = null;
    private FilterItem mSelected = null;
    private boolean mEnabled = true;

    private OnItemClickListener mOnItemClickListener = null;

    public PriceCheckFilterAdapter(Context context, AppCompatSpinner spinner) {
        mContext = context;
        mSpinner = spinner;
        mItems.add(new FilterItem(mContext.getString(R.string.search_row_filter_clear), 0, null, true));
    }

    @Override
    public int getCount() {
        return (mCurrent != null) ? (mCurrent.getChildSize() + 1) : mItems.size();
    }

    @Override
    public Object getItem(int pos) {
        try {
            if (mCurrent == null) {
                return mItems.get(pos);
            }

            return (pos > 0) ? mCurrent.getChild(pos - 1) : mCurrent;
        } catch(IndexOutOfBoundsException e) {
            Log.e(TAG, "getItem " + pos, e);
        }

        return null;
    }

    @Override
    public long getItemId(int pos) {
        Object o = getItem(pos);
        return (o == null) ? 0 : o.hashCode();
    }

    @Override
    public View getView(final int pos, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.pricecheck_filter_row, viewGroup, false);
            view.setTag(new FilterItemViewHolder(view));
        }

        FilterItemViewHolder vh = (FilterItemViewHolder) view.getTag();
        vh.setItem(mSelected);
        vh.setEnabled(mEnabled);

        return view;
    }

    @Override
    public View getDropDownView(int pos, @Nullable View view,
                                @NonNull ViewGroup viewGroup) {
        if(view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.pricecheck_filter_dropdown_row, viewGroup, false);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FilterItemRowViewHolder vh = (FilterItemRowViewHolder) view.getTag();
                    mRowClickListener.onRowClick(vh.getPosition());
                }
            });
            view.setTag(new FilterItemRowViewHolder(view));
        }

        FilterItem item = (FilterItem) getItem(pos);
        if(item == null) {
            return view;
        }

        FilterItemRowViewHolder vh = (FilterItemRowViewHolder) view.getTag();
        vh.setPosition(pos);

        if(mCurrent != null && pos == 0) {
            vh.setHeader(item);
        } else {
            vh.setItem(item);
        }

        return view;
    }

    public void setItems(PriceCheckCategories categories) {
        List<PriceCheckFilterAdapter.FilterItem> items = new ArrayList<>();
        for(PriceCheckCategories.PriceCheckCategory c: categories) {
            PriceCheckFilterAdapter.FilterItem item = new PriceCheckFilterAdapter.FilterItem(c.getName(), c.getCategoryID(), null);
            if(c.getChildren().size() > 0) {
                toFilter(item, c.getChildren());
            }

            items.add(item);
        }

        mItems.clear();
        mItems.add(new FilterItem(mContext.getString(R.string.search_row_filter_clear), 0, null, true));
        mItems.addAll(items);
    }

    void setFilter(String filter) {
        for(FilterItem item: mItems) {
            updateFilter(filter, item);
        }
    }

    public void setEnabled(boolean b) {
        mEnabled = b;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private boolean updateFilter(String filter, FilterItem item) {
        if(item.getChildSize() == 0 && item.getName().equals(filter)) {
            mSelected = item;
            mCurrent = item.getParent();
            return true;
        }

        for(int i = 0; i < item.getChildSize(); i++) {
            if(updateFilter(filter, item.getChild(i))) {
                break;
            }
        }

        return false;
    }

    private void toFilter(PriceCheckFilterAdapter.FilterItem item, PriceCheckCategories categories) {
        for(PriceCheckCategories.PriceCheckCategory c: categories) {
            PriceCheckFilterAdapter.FilterItem newItem = new PriceCheckFilterAdapter.FilterItem(c.getName(), c.getCategoryID(), item);
            if(c.getChildren().size() > 0) {
                toFilter(newItem, c.getChildren());
            }

            item.addChild(newItem);
        }
    }

    private RowClickListener mRowClickListener = new RowClickListener() {
        @Override
        public void onRowClick(int pos) {
            FilterItem item = (FilterItem) getItem(pos);
            if(item != null && item.getChildSize() == 0) {
                mSelected = item;
                mSpinner.performItemClick(mSpinner.getSelectedView(), pos, getItemId(pos));

                if(mOnItemClickListener != null) {
                    if(item.isClear()) {
                        mSelected = null;
                        mCurrent = null;
                        notifyDataSetChanged();

                        mOnItemClickListener.onNothingSelected();
                    } else {
                        mOnItemClickListener.onItemClick(pos, item.getName(), item.getID());
                    }
                }

                return;
            }

            mSpinner.performItemClick(mSpinner.getSelectedView(), pos, getItemId(pos));

            if(mCurrent == null) {
                mCurrent = mItems.get(pos);
            } else if(pos > 0) {
                mCurrent = mCurrent.getChild(pos - 1);
            } else {
                if(mCurrent.getParent() != null) {
                    mCurrent = mCurrent.getParent();
                } else {
                    mCurrent = null;
                }
            }

            notifyDataSetChanged();
        }
    };

    public interface OnItemClickListener {
        void onItemClick(int pos, String name, long id);
        void onNothingSelected();
    }

    interface RowClickListener {
        void onRowClick(int pos);
    }

    private static class FilterItem {
        private String mName;
        private long mID;
        private List<FilterItem> mChildren = new ArrayList<>();
        private FilterItem mParent;
        private boolean mIsClear;

        FilterItem(String name, long id, FilterItem parent) {
            this(name, id, parent, false);
        }

        FilterItem(String name, long id, FilterItem parent, boolean isClear) {
            mName = name;
            mID = id;
            mParent = parent;
            mIsClear = isClear;
        }

        void addChild(FilterItem item) {
            mChildren.add(item);
        }

        int getChildSize() {
            return mChildren.size();
        }

        FilterItem getChild(int pos) {
            return mChildren.get(pos);
        }

        String getName() {
            return mName;
        }

        long getID() {
            return mID;
        }

        FilterItem getParent() {
            return mParent;
        }

        boolean isClear() {
            return mIsClear;
        }
    }
}
