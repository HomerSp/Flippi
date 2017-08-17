package com.matnar.app.android.flippi.view.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;

public class AutoCompleteFocusTextView extends AppCompatAutoCompleteTextView implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final String TAG = "Flippi." + AutoCompleteFocusTextView.class.getSimpleName();

    public AutoCompleteFocusTextView(Context context) {
        super(context);
        init();
    }
    public AutoCompleteFocusTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public AutoCompleteFocusTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(this);
        setOnItemClickListener(this);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isPopupShowing()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                    performClick();
                    break;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onClick(View view) {
        if(view == this) {
            view.requestFocus();
            showDropDown();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        super.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
    }
}
