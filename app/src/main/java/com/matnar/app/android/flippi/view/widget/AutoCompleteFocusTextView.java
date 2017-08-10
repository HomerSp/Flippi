package com.matnar.app.android.flippi.view.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class AutoCompleteFocusTextView extends AppCompatAutoCompleteTextView implements View.OnClickListener {
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
    }

    @Override
    public boolean enoughToFilter() {
        return hasFocus();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onClick(View view) {
        if(view == this) {
            showDropDown();
        }
    }
}
