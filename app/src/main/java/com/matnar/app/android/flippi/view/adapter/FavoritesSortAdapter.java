package com.matnar.app.android.flippi.view.adapter;

import android.content.Context;
import android.support.annotation.ArrayRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.matnar.app.android.flippi.R;

public class FavoritesSortAdapter extends ArrayAdapter<CharSequence> {
    private String mSelected = null;

    public FavoritesSortAdapter(@NonNull Context context, @LayoutRes int resource, CharSequence[] strings) {
        super(context, resource, strings);
    }

    @NonNull
    @Override
    public View getView(final int pos, @Nullable View view, @NonNull ViewGroup viewGroup) {
        if(view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.pricecheck_filter_row, viewGroup, false);
            view.setTag(new SortViewHolder(view));
        }

        SortViewHolder vh = (SortViewHolder) view.getTag();
        vh.setItem(mSelected);

        return view;
    }

    public void setSelected(String selected) {
        mSelected = selected;
    }

    public static @NonNull FavoritesSortAdapter createFromResource(@NonNull Context context,
                                                                   @ArrayRes int textArrayResId, @LayoutRes int textViewResId) {
        final CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
        return new FavoritesSortAdapter(context, textViewResId, strings);
    }


    private class SortViewHolder {
        private TextView mName;

        SortViewHolder(View view) {
            mName = (TextView) view.findViewById(R.id.header_name);
        }

        public void setItem(String name) {
            if(mSelected == null) {
                mName.setText(R.string.saved_row_sort_no);
                return;
            }

            mName.setText(name);
        }
    }
}
