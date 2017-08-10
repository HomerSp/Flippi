package com.matnar.app.android.flippi.view.widget;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.matnar.app.android.flippi.activity.MainActivity;

public class AutoCompleteSearchView extends SearchView implements AdapterView.OnItemClickListener, View.OnFocusChangeListener {
    private static final String TAG = "Flippi." + AutoCompleteSearchView.class.getSimpleName();

    private SearchView.SearchAutoComplete mView;

    public AutoCompleteSearchView(Context context) {
        super(context);
        init();
    }

    public AutoCompleteSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoCompleteSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mView = (SearchAutoComplete) findViewById(android.support.v7.appcompat.R.id.search_src_text);
        mView.setOnItemClickListener(this);
        mView.setOnFocusChangeListener(this);

        SearchManager searchManager = (SearchManager)getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchableInfo info = searchManager.getSearchableInfo(new ComponentName(getContext(), MainActivity.class));
        setSearchableInfo(info);
    }

    @Override
    public final void setSuggestionsAdapter(CursorAdapter adapter) {
        // Empty
    }

    public void dismissDropDown() {
        mView.dismissDropDown();
    }

    public void setAdapter(ArrayAdapter<?> adapter) {
        mView.setAdapter(adapter);
    }

    public void setText(String text) {
        mView.setText(text);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mView.setText((String) adapterView.getItemAtPosition(i));
    }

    @Override
    public void onFocusChange(View view, boolean focus) {
        if(focus) {
            mView.showDropDown();
        } else {
            mView.dismissDropDown();
        }
    }
}
