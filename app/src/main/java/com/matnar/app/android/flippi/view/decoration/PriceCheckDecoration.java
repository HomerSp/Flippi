package com.matnar.app.android.flippi.view.decoration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.matnar.app.android.flippi.view.adapter.PriceCheckAdapter;

public class PriceCheckDecoration extends DividerItemDecoration {
    private static final String TAG = "Flippi." + PriceCheckDecoration.class.getSimpleName();

    private static final int[] ATTRS = new int[]{ android.R.attr.listDivider };

    private Drawable mDivider;
    private final Rect mBounds = new Rect();

    public PriceCheckDecoration(Context context, int orientation) {
        super(context, orientation);

        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDivider = a.getDrawable(0);
        a.recycle();
        setOrientation(orientation);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        canvas.save();
        final int left;
        final int right;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && parent.getClipToPadding()) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
            canvas.clipRect(left, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        } else {
            left = 0;
            right = parent.getWidth();
        }

        RecyclerView.Adapter adapter = parent.getAdapter();
        final int adapterCount = adapter.getItemCount();

        final int childCount = parent.getChildCount();
        for (int i = 1; i < childCount - 1; i++) {
            // Don't draw items that are outside of the adapter,
            // this happens when a changed item is fading out for example.
            if(i >= adapterCount - 1) {
                break;
            }

            final View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            final int bottom = mBounds.bottom + Math.round(ViewCompat.getTranslationY(child));
            final int top = bottom - mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(canvas);
        }
        canvas.restore();
    }
}
