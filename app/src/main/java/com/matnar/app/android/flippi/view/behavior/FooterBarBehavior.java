package com.matnar.app.android.flippi.view.behavior;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import com.matnar.app.android.flippi.view.widget.FooterBarLayout;

public class FooterBarBehavior extends CoordinatorLayout.Behavior<FooterBarLayout> {
    private static final String TAG = "Flippi." + FooterBarBehavior.class.getSimpleName();

    public FooterBarBehavior() {
    }

    public FooterBarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent,
                                   FooterBarLayout child,
                                   View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent,
                                          FooterBarLayout child,
                                          View dependency) {
        return false;
    }

    @Override
    public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams lp) {
        if (lp.insetEdge == Gravity.NO_GRAVITY) {
            lp.insetEdge = Gravity.BOTTOM;
        }
    }
}
