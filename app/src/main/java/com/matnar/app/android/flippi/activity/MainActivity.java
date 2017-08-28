package com.matnar.app.android.flippi.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.util.IabHelper;
import com.google.android.vending.licensing.util.IabResult;
import com.google.android.vending.licensing.util.Inventory;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.db.CategoryDatabase;
import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.fragment.main.BarcodeScanFragment;
import com.matnar.app.android.flippi.fragment.main.MainFragment;
import com.matnar.app.android.flippi.fragment.main.SavedListFragment;
import com.matnar.app.android.flippi.fragment.main.SearchResultFragment;
import com.matnar.app.android.flippi.fragment.main.SettingsFragment;
import com.matnar.app.android.flippi.view.adapter.SavedSearchesAdapter;
import com.matnar.app.android.flippi.view.widget.AutoCompleteFocusTextView;
import com.matnar.app.android.flippi.view.widget.FooterBarLayout;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Flippi." + MainActivity.class.getSimpleName();

    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArLGikFct+yG55/usMKwo/QM8/nRT7UVz753cWblRvJbb634POG8JwZ3UI8bXz2n72XHlk+/hnbXw//BUh8qk6ZMe1rkWR1Zavn64hFysilt4HtFRAOcqsIwg3Ic7eAJjIqssw3HlhIDAUAJnZ6j44Xy8WnoPuzColDkYBEaNP3Li9qcstLrj+bZ3owv6PyKJDQeB4V2qNXsTDRtKDGfcqtAOsoGzBx4pTGhBDco1HLW3fZ4Bl6N/5tLpvVQ7vxYdW3WusQ+Y8jlxcvyXrBdHwd4V5kgnLEhxVVOqmMEFvPtXDQP63eHo89hUAZFet6+cnxcTmoKJ4Qe9IAAB9iCbrwIDAQAB";

    private MainActivityHelper mHelper;

    private int mMediumAnimationDuration;

    private IabHelper mBillingHelper;
    private Handler mLicenseHandler;
    private boolean mLicenseChecked = false;
    private boolean mAdFreeLicense = false;
    private List<OnLicenseCheckListener> mLicenseCheckListeners = new ArrayList<>();

    private CoordinatorLayout mCoordinatorLayout;
    private AppBarLayout mAppBarLayout;

    private View mAppBarSearchContainer;
    private AutoCompleteFocusTextView mAppBarSearchTextView;
    private ImageView mAppBarSearchGoButtonView;

    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private FloatingActionButton mFAB;
    private FooterBarLayout mFooter;

    private MenuItem mSearchItem = null;
    private MenuItem mClearFavoritesItem = null;

    private boolean mSearchItemVisible = false;
    private boolean mClearFavoritesItemVisible = false;

    private SavedSearchesAdapter mSearchAdapter;

    private PriceCheckDatabase mPriceCheckDatabase;
    private CategoryDatabase mCategoryDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Intialise Picasso downloader
        boolean initialisePicasso = true;
        try {
            Picasso.setSingletonInstance(null);
        } catch(IllegalStateException e) {
            initialisePicasso = false;
        }

        if(initialisePicasso) {
            try {
                Picasso.setSingletonInstance(new Picasso.Builder(this)
                        .downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE))
                        .build()
                );
            } catch(IllegalStateException e) {
                Log.e(TAG, "Could not initialise Picasso", e);
            }
        }

        mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mSearchAdapter = new SavedSearchesAdapter(this);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.mainCoordinatorLayout);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mAppBarSearchContainer = findViewById(R.id.search_container);
        mAppBarSearchGoButtonView = (ImageView) mAppBarSearchContainer.findViewById(R.id.search_query_go);

        mAppBarSearchTextView = (AutoCompleteFocusTextView) mAppBarSearchContainer.findViewById(R.id.search_query);
        mAppBarSearchTextView.setAdapter(mSearchAdapter);
        mAppBarSearchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mAppBarSearchGoButtonView.performClick();
                }

                return true;
            }
        });

        View searchBackButton = mAppBarSearchContainer.findViewById(R.id.search_query_back);
        searchBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && currentFragment instanceof SearchResultFragment && currentFragment.isVisible()) {
                    getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                getHelper().showAppBarSearch(false);
            }
        });

        mAppBarSearchGoButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = mAppBarSearchTextView.getText().toString();
                if(s.isEmpty()) {
                    return;
                }

                mSearchAdapter.add(s);
                mSearchAdapter.notifyDataSetChanged();

                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                int cx  = (int)view.getX() + (view.getWidth() / 2);
                int cy = (int)view.getY() + (view.getHeight() / 2);

                mAppBarSearchTextView.clearFocus();
                final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mAppBarSearchTextView.getWindowToken(), 0);

                getHelper().setSearchQuery(s);
                getHelper().doSearch(s, false, cx, cy);
            }
        });

        mFAB = (FloatingActionButton) findViewById(R.id.fab);
        mFAB.setTag(R.drawable.ic_fab_camera);
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(currentFragment != null && currentFragment instanceof BarcodeScanFragment && currentFragment.isVisible()) {
                ((BarcodeScanFragment)currentFragment).toggleTorch();
                return;
            }

            if(!getSupportFragmentManager().popBackStackImmediate("fragment_barcode_scan", 0)) {
                BarcodeScanFragment fragment = new BarcodeScanFragment();

                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);

                Bundle bundle = new Bundle();
                bundle.putInt("cx", rect.left + (view.getWidth() / 2));
                bundle.putInt("cy", rect.top + (view.getHeight() / 2));
                fragment.setArguments(bundle);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack("fragment_barcode_scan");
                transaction.commit();
            }
            }
        });

        mFooter = (FooterBarLayout) findViewById(R.id.footer_view);

        if(savedInstanceState != null) {
            int res = savedInstanceState.getInt("fabimgres");
            mFAB.setImageResource(res);
            mFAB.setTag(res);

            mAppBarSearchTextView.setText(savedInstanceState.getString("search_query"));
            if(savedInstanceState.getBoolean("search_expanded")) {
                getHelper().showAppBarSearch(true, false, savedInstanceState.getBoolean("search_focus"));
            }
        }

        mPriceCheckDatabase = new PriceCheckDatabase(this);
        mCategoryDatabase = new CategoryDatabase(this);

        mLicenseHandler = new Handler();
        mBillingHelper = new IabHelper(this, BASE64_PUBLIC_KEY);
        mBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if(!result.isSuccess()) {
                    mLicenseChecked = true;
                    mAdFreeLicense = false;
                    for(OnLicenseCheckListener listener: mLicenseCheckListeners) {
                        listener.onLicenseCheck(mAdFreeLicense);
                    }
                    mLicenseCheckListeners.clear();
                } else {
                    mBillingHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            mLicenseChecked = true;
                            mAdFreeLicense = result.isSuccess() && inv.hasPurchase("adfree");
                            for(OnLicenseCheckListener listener: mLicenseCheckListeners) {
                                listener.onLicenseCheck(mAdFreeLicense);
                            }
                            mLicenseCheckListeners.clear();
                        }
                    });
                }
            }
        });

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && !(currentFragment instanceof SearchResultFragment) && currentFragment.isVisible() && mFAB.getVisibility() != View.VISIBLE) {
                    mFAB.show();
                }
            }
        });

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            MainFragment firstFragment = new MainFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment, "fragment_main").commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDrawer.removeDrawerListener(mDrawerToggle);

        mPriceCheckDatabase.close();

        try {
            if (mBillingHelper != null) {
                mBillingHelper.dispose();
                mBillingHelper = null;
            }
        } catch(IllegalArgumentException e) {
            // Empty
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("fabimgres", (Integer) findViewById(R.id.fab).getTag());
        outState.putString("search_query", mAppBarSearchTextView.getText().toString());
        outState.putBoolean("search_expanded", mAppBarSearchContainer.getVisibility() == View.VISIBLE);
        outState.putBoolean("search_focus", mAppBarSearchTextView.hasFocus());
    }

    @Override
    public void onPause() {
        super.onPause();

        mSearchAdapter.save();
    }

    @Override
    public void onResume() {
        super.onResume();

        mSearchAdapter.update();
        mSearchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(currentFragment != null && currentFragment instanceof SearchResultFragment && currentFragment.isVisible()) {
                super.onBackPressed();
                getHelper().showAppBarSearch(false);
                return;
            }

            if(mAppBarSearchContainer.getVisibility() == View.VISIBLE) {
                getHelper().showAppBarSearch(false);
                return;
            }

            if(currentFragment != null && currentFragment instanceof MainActivityFragment) {
                if(((MainActivityFragment) currentFragment).onBackPressed()) {
                    return;
                }
            }

            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        mSearchItem = menu.findItem(R.id.action_search);
        mSearchItem.setVisible(mSearchItemVisible);

        mClearFavoritesItem = menu.findItem(R.id.action_favorites_clear);
        mClearFavoritesItem.setVisible(mClearFavoritesItemVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_search: {
                getHelper().showAppBarSearch(true);
                break;
            }
            case R.id.action_favorites_clear: {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment != null && currentFragment instanceof SavedListFragment && currentFragment.isVisible()) {
                    ((SavedListFragment) currentFragment).clearFavorites();
                }

                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.nav_home: {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && currentFragment instanceof MainFragment && currentFragment.isVisible()) {
                    break;
                }

                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
            }
            case R.id.nav_saveditems: {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && currentFragment instanceof SavedListFragment && currentFragment.isVisible()) {
                    ((SavedListFragment)currentFragment).doQuery();
                    break;
                }

                getSupportFragmentManager().popBackStackImmediate("fragment_saveditems", FragmentManager.POP_BACK_STACK_INCLUSIVE);

                SavedListFragment fragment = new SavedListFragment();

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack("fragment_saveditems");
                transaction.commit();

                break;
            }
            case R.id.nav_settings: {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && currentFragment instanceof SettingsFragment && currentFragment.isVisible()) {
                    break;
                }

                getSupportFragmentManager().popBackStackImmediate("fragment_settings", FragmentManager.POP_BACK_STACK_INCLUSIVE);

                SettingsFragment fragment = new SettingsFragment();

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack("fragment_settings");
                transaction.commit();

                break;
            }
            case R.id.nav_feedback: {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"mathias@matnar.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Flippi - CeX Scanner feedback");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }

                break;
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private MainActivityHelper getHelper() {
        if(mHelper == null) {
            mHelper = new MainActivityHelper(this);
        }

        return mHelper;
    }

    public static class MainActivityHelper {
        private MainActivity mActivity;

        MainActivityHelper(MainActivity activity) {
            mActivity = activity;
        }

        public void addOnLicenseCheckListener(OnLicenseCheckListener listener) {
            if(mActivity == null) {
                return;
            }

            if (mActivity.mLicenseChecked) {
                listener.onLicenseCheck(mActivity.mAdFreeLicense);
                return;
            }

            mActivity.mLicenseCheckListeners.add(listener);
        }

        public void doSearch(String query, boolean isBarcode) {
            doSearch(query, isBarcode, 0, 0);
        }

        public void doSearch(String query, boolean isBarcode, int cx, int cy) {
            if(mActivity == null) {
                return;
            }

            Fragment currentFragment = mActivity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null && currentFragment instanceof SearchResultFragment && currentFragment.isVisible()) {
                ((SearchResultFragment) currentFragment).doSearch(query, isBarcode);
            } else {
                mActivity.getSupportFragmentManager().popBackStackImmediate("fragment_search_result", FragmentManager.POP_BACK_STACK_INCLUSIVE);

                SearchResultFragment fragment = new SearchResultFragment();

                Bundle args = new Bundle();
                args.putString("query", query);
                args.putBoolean("is_barcode", isBarcode);

                if (cx != 0 || cy != 0) {
                    args.putInt("cx", cx);
                    args.putInt("cy", cy);
                }

                fragment.setArguments(args);

                FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack("fragment_search_result");
                transaction.commit();
            }
        }

        public String getDeviceID() {
            if(mActivity == null) {
                return "";
            }

            return PreferenceManager.getDefaultSharedPreferences(mActivity).getString("uuid", UUID.randomUUID().toString());
        }

        public PriceCheckDatabase getPriceCheckDatabase() {
            if(mActivity == null) {
                return null;
            }

            return mActivity.mPriceCheckDatabase;
        }

        public CategoryDatabase getCategoryDatabase() {
            if(mActivity == null) {
                return null;
            }

            return mActivity.mCategoryDatabase;
        }

        public SavedSearchesAdapter getSearchAdapter() {
            if(mActivity == null) {
                return null;
            }

            return mActivity.mSearchAdapter;
        }

        public void resetActionBar() {
            if(mActivity == null) {
                return;
            }

            if (mActivity.mAppBarLayout.getY() < 0) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mActivity.mAppBarLayout.getLayoutParams();
                AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
                if (behavior != null) {
                    behavior.onNestedFling(mActivity.mCoordinatorLayout, mActivity.mAppBarLayout, null, 0, mActivity.mAppBarLayout.getY(), true);
                }
            }
        }

        public void setFabIcon(final int res) {
            if(mActivity == null) {
                return;
            }

            if (res == 0) {
                mActivity.mFAB.hide();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mActivity.mFAB.animate().withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if ((Integer) mActivity.mFAB.getTag() != res && mActivity.mFAB.getVisibility() == View.VISIBLE) {
                            mActivity.mFAB.setTag(res);
                            mActivity.mFAB.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                                @Override
                                public void onHidden(FloatingActionButton fab) {
                                    fab.setImageResource((Integer) fab.getTag());
                                    fab.show();
                                }
                            });
                        } else {
                            mActivity.mFAB.setImageResource(res);
                            mActivity.mFAB.show();
                        }
                    }
                }).start();
            } else {
                mActivity.mFAB.setImageResource(res);
                mActivity.mFAB.setTag(res);
                mActivity.mFAB.show();
            }
        }

        public void setSearchQuery(String q) {
            if(mActivity == null) {
                return;
            }

            if (q == null) {
                mActivity.mAppBarSearchTextView.setText("");
                showAppBarSearch(false);
            } else {
                mActivity.mAppBarSearchTextView.setText(q);
            }

            mActivity.mAppBarSearchTextView.clearFocus();
            mActivity.mAppBarSearchTextView.dismissDropDown();
        }

        public View setFooter(int resId) {
            if(mActivity == null) {
                return null;
            }

            TypedArray arr = mActivity.getTheme().obtainStyledAttributes(new int[]{R.attr.actionBarSize});
            final float translationY = -arr.getDimension(0, 0.0f);

            if (resId != 0) {
                View view = mActivity.getLayoutInflater().inflate(resId, mActivity.mFooter, false);

                mActivity.mFooter.setVisibility(View.INVISIBLE);
                mActivity.mFooter.removeAllViews();
                mActivity.mFooter.addView(view);
                mActivity.mFooter.setTranslationY(-translationY);
                mActivity.mFooter.setVisibility(View.VISIBLE);
                mActivity.mFooter.animate().cancel();
                mActivity.mFooter.animate()
                        .translationY(0.0f)
                        .setListener(null)
                        .start();

                return view;
            } else {
                mActivity.mFooter.animate().cancel();
                mActivity.mFooter.animate()
                        .translationY(-translationY)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mActivity.mFooter.removeAllViews();
                                mActivity.mFooter.setVisibility(View.GONE);
                            }
                        })
                        .start();

                return null;
            }
        }

        public void setActionBarTitle(String str) {
            if(mActivity == null) {
                return;
            }

            ActionBar actionBar = mActivity.getSupportActionBar();
            if (actionBar == null) {
                return;
            }

            actionBar.setTitle(str);
        }

        public void showClearFavorites(boolean show) {
            if(mActivity == null) {
                return;
            }

            if (mActivity.mClearFavoritesItem == null) {
                mActivity.mClearFavoritesItemVisible = show;
                return;
            }

            mActivity.mClearFavoritesItem.setVisible(show);
        }

        public void showSearchItem(boolean show) {
            if(mActivity == null) {
                return;
            }

            if (mActivity.mSearchItem == null) {
                mActivity.mSearchItemVisible = show;
                return;
            }

            mActivity.mSearchItem.setVisible(show);
        }

        public void showAppBarSearch(boolean show) {
            showAppBarSearch(show, true, true);
        }

        public void showAppBarSearch(boolean show, boolean animate, final boolean focus) {
            if(mActivity == null) {
                return;
            }

            if (!animate) {
                mActivity.mAppBarSearchContainer.setVisibility((show) ? View.VISIBLE : View.GONE);

                return;
            }

            if (show && mActivity.mAppBarSearchContainer.getVisibility() != View.VISIBLE) {
                mActivity.mAppBarSearchContainer.setVisibility(View.INVISIBLE);
                mActivity.mAppBarSearchContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mActivity.mAppBarSearchContainer.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (focus) {
                                    mActivity.mAppBarSearchContainer.findViewById(R.id.search_query).requestFocus();
                                    mActivity.mAppBarSearchContainer.findViewById(R.id.search_query).performClick();
                                }
                            }
                        };

                        mActivity.mAppBarSearchContainer.animate().cancel();

                        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            int cx = (int) mActivity.mAppBarSearchGoButtonView.getX() + (mActivity.mAppBarSearchGoButtonView.getWidth() / 2);
                            int cy = (int) mActivity.mAppBarSearchGoButtonView.getY() + (mActivity.mAppBarSearchGoButtonView.getHeight() / 2);
                            float radius = (float) Math.hypot(cx, cy);

                            try {
                                Animator anim = ViewAnimationUtils.createCircularReveal(mActivity.mAppBarSearchContainer, cx, cy, 0, radius);
                                anim.addListener(listener);
                                anim.setDuration(mActivity.mMediumAnimationDuration);
                                anim.start();
                            } catch(IllegalStateException e) {
                                mActivity.mAppBarSearchContainer.setVisibility(View.VISIBLE);
                            }
                        } else {
                            mActivity.mAppBarSearchContainer.animate()
                                    .alpha(1.0f)
                                    .setListener(listener)
                                    .setDuration(mActivity.mMediumAnimationDuration)
                                    .start();
                        }
                    }
                });
            } else if (!show && mActivity.mAppBarSearchContainer.getVisibility() != View.GONE) {
                mActivity.mAppBarSearchContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mActivity.mAppBarSearchContainer.setVisibility(View.GONE);
                                mActivity.mAppBarSearchTextView.setText("");
                            }
                        };

                        mActivity.mAppBarSearchContainer.animate().cancel();

                        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            int cx = (int) mActivity.mAppBarSearchGoButtonView.getX() + (mActivity.mAppBarSearchGoButtonView.getWidth() / 2);
                            int cy = (int) mActivity.mAppBarSearchGoButtonView.getY() + (mActivity.mAppBarSearchGoButtonView.getHeight() / 2);
                            float radius = (float) Math.hypot(cx, cy);

                            try {
                                Animator anim = ViewAnimationUtils.createCircularReveal(mActivity.mAppBarSearchContainer, cx, cy, radius, 0);
                                anim.addListener(listener);
                                anim.setDuration(mActivity.mMediumAnimationDuration);
                                anim.start();
                            } catch(IllegalStateException e) {
                                mActivity.mAppBarSearchContainer.setVisibility(View.GONE);
                            }
                        } else {
                            mActivity.mAppBarSearchContainer.animate()
                                    .alpha(0.0f)
                                    .setListener(listener)
                                    .setDuration(mActivity.mMediumAnimationDuration)
                                    .start();
                        }
                    }
                });
            }
        }
    }

    @SuppressWarnings("unused")
    private class AppLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }

            postResult(true);
        }

        public void dontAllow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }

            postResult(false);
        }

        public void applicationError(int errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }

            postResult(false);
        }

        private void postResult(final boolean result) {
            mLicenseHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLicenseChecked = true;
                    for(OnLicenseCheckListener listener: mLicenseCheckListeners) {
                        listener.onLicenseCheck(result);
                    }

                    mLicenseCheckListeners.clear();
                }
            });
        }
    }

    public interface OnLicenseCheckListener {
        void onLicenseCheck(boolean result);
    }

    @SuppressWarnings("unused")
    public abstract static class MainActivityFragment extends Fragment {
        private MainActivityHelper mHelper;

        public MainActivityFragment() {
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            if(getActivity() instanceof MainActivity) {
                mHelper = ((MainActivity) getActivity()).getHelper();
            }
        }

        protected boolean onBackPressed() {
            return false;
        }

        protected MainActivityHelper getMainHelper() {
            if(mHelper == null) {
                return new MainActivityHelper(null);
            }

            return mHelper;
        }
    }

    @SuppressWarnings("unused")
    public abstract static class MainActivityPreferenceFragment extends PreferenceFragmentCompat {
        private MainActivityHelper mHelper;

        public MainActivityPreferenceFragment() {
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            if(getActivity() instanceof MainActivity) {
                mHelper = ((MainActivity) getActivity()).getHelper();
            }
        }

        protected boolean onBackPressed() {
            return false;
        }

        protected MainActivityHelper getHelper() {
            if(mHelper == null) {
                return new MainActivityHelper(null);
            }

            return mHelper;
        }
    }
}
