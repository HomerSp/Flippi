package com.matnar.app.android.flippi.view.widget;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.ListPopupWindow;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.matnar.app.android.flippi.view.adapter.PriceCheckFilterAdapter;

import java.lang.reflect.Field;

public class PriceCheckFilterSpinner extends AppCompatSpinner implements PriceCheckFilterAdapter.OnItemClickListener {
    private static final String TAG = "Flippi." + PriceCheckFilterSpinner.class.getSimpleName();

    private OnItemSelectedListener mOnItemSelectedListener;

    public PriceCheckFilterSpinner(Context context) {
        super(context);
        init();
    }
    public PriceCheckFilterSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public PriceCheckFilterSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public PriceCheckFilterSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
        init();
    }
    public PriceCheckFilterSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode, Resources.Theme popupTheme) {
        super(context, attrs, defStyleAttr, mode, popupTheme);
        init();
    }

    @Override
    public void onItemClick(int pos, String name) {
        super.setSelection(pos);
        dismiss();

        if(mOnItemSelectedListener != null) {
            mOnItemSelectedListener.onItemSelected(name);
        }
    }

    @Override
    public void onNothingSelected() {
        super.setSelection(-1);
        dismiss();

        if(mOnItemSelectedListener != null) {
            mOnItemSelectedListener.onNothingSelected();
        }
    }

    @Override
    final public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        // Do nothing
    }

    final public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public void setAdapter(PriceCheckFilterAdapter adapter) {
        super.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }
    public void dismiss() {
        super.onDetachedFromWindow();
    }

    private void init() {
    }

    public interface OnItemSelectedListener {
        void onItemSelected(String name);
        void onNothingSelected();
    }
}