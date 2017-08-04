package com.matnar.app.android.flippi.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Log;
import android.view.View;

public class AnimationUtil {
    private static final String TAG = "Flippi." + AnimationUtil.class.getSimpleName();

    public static void animateShow(final View view, int duration) {
        view.setTag(1);
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1.0f)
                .setDuration(duration)
                .setListener(null);
    }

    public static void animateHide(final View view, int duration) {
        view.setTag(2);
        view.clearAnimation();
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(0.0f)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        boolean hiding = (Integer) view.getTag() == 2;
                        view.setTag(0);
                        if(hiding) {
                            view.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
