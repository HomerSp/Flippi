package com.matnar.app.android.flippi.fragment.main;

import android.Manifest;
import android.animation.Animator;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;

import java.util.List;

public class BarcodeScanFragment extends MainActivity.MainActivityFragment implements BarcodeCallback, DecoratedBarcodeView.TorchListener {
    private static final String TAG = "Flippi." + BarcodeScanFragment.class.getSimpleName();

    private int mRevealAnimationDuration = 0;

    public BarcodeScanFragment() {

    }

    private DecoratedBarcodeView mView;
    private boolean mTorch = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRevealAnimationDuration = getResources().getInteger(R.integer.reveal_anim_duration);

        if(savedInstanceState != null) {
            mTorch = savedInstanceState.getBoolean("torch");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            super.setFooter(0);
            super.setFabIcon(R.drawable.ic_fab_torch_off);
            super.showClearFavorites(false);
            super.showSearchItem(true);
            super.setToolbarScroll(false);
        } catch(IllegalStateException e) {
            Log.e(TAG, "Create view error", e);
            return null;
        }

        View rootView = inflater.inflate(R.layout.fragment_main_barcode_scanner, container, false);
        mView = (DecoratedBarcodeView)rootView.findViewById(R.id.barcode_scanner_view);
        mView.setTorchListener(this);

        if(mTorch) {
            mView.setTorchOn();
        } else {
            mView.setTorchOff();
        }

        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
        } else {
            mView.decodeSingle(this);
        }

        if(savedInstanceState == null) {
            mView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                                           int oldRight, int oldBottom) {
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                        return;
                    }

                    v.removeOnLayoutChangeListener(this);
                    int cx = getArguments().getInt("cx");
                    int cy = getArguments().getInt("cy");
                    int radius = (int) Math.hypot(right, bottom);

                    Animator reveal = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, radius);
                    reveal.setInterpolator(new DecelerateInterpolator(2f));
                    reveal.setDuration(mRevealAnimationDuration);
                    reveal.start();
                }
            });
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("torch", mTorch);
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(mView != null) {
            if (isVisibleToUser) {
                mView.resume();
            } else {
                mView.pauseAndWait();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mView.pauseAndWait();
    }

    @Override
    public void onResume() {
        super.onResume();
        mView.resume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1 && permissions.length == 1 && grantResults.length == 1 && permissions[0].equals(Manifest.permission.CAMERA)) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mView.decodeSingle(this);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void barcodeResult(BarcodeResult result) {
        mView.pauseAndWait();

        try {
            super.doSearch(result.getText(), true);
        } catch(IllegalStateException e) {
            Log.e(TAG, "Barcode result error", e);
        }
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {

    }

    public void toggleTorch() {
        mTorch = !mTorch;
        if(mTorch) {
            mView.setTorchOn();
        } else {
            mView.setTorchOff();
        }
    }

    @Override
    public void onTorchOn() {
        try {
            super.setFabIcon(R.drawable.ic_fab_torch_on);
        } catch(IllegalStateException e) {
            Log.e(TAG, "Set fab icon error", e);
        }
    }

    @Override
    public void onTorchOff() {
        try {
            super.setFabIcon(R.drawable.ic_fab_torch_off);
        } catch(IllegalStateException e) {
            Log.e(TAG, "Set fab icon error", e);
        }
    }
}