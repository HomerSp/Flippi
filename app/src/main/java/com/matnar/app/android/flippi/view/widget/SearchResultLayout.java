package com.matnar.app.android.flippi.view.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class SearchResultLayout extends RelativeLayout {
    private Paint mPaint;
    private boolean mFilter = false;

    public SearchResultLayout(Context context) {
        super(context);
        init();
    }

    public SearchResultLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchResultLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void enableFilter(boolean e) {
        mFilter = e;
    }

    private void init() {
        mPaint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        mPaint.setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0x000000FF));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if(mFilter) {
            canvas.saveLayer(null, mPaint, Canvas.ALL_SAVE_FLAG);
            super.dispatchDraw(canvas);
            canvas.restore();
            return;
        }

        super.dispatchDraw(canvas);
    }
}
