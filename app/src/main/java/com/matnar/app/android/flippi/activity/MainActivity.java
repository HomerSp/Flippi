package com.matnar.app.android.flippi.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.fragment.main.BarcodeResultFragment;
import com.matnar.app.android.flippi.fragment.main.BarcodeScanFragment;
import com.matnar.app.android.flippi.fragment.main.MainFragment;
import com.matnar.app.android.flippi.db.PriceCheckDatabase;
import com.matnar.app.android.flippi.fragment.main.SavedListFragment;
import com.matnar.app.android.flippi.fragment.main.SettingsFragment;
import com.matnar.app.android.flippi.util.IabHelper;
import com.matnar.app.android.flippi.util.IabResult;
import com.matnar.app.android.flippi.util.Inventory;
import com.matnar.app.android.flippi.view.widget.FooterBarLayout;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Flippi." + MainActivity.class.getSimpleName();

    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArLGikFct+yG55/usMKwo/QM8/nRT7UVz753cWblRvJbb634POG8JwZ3UI8bXz2n72XHlk+/hnbXw//BUh8qk6ZMe1rkWR1Zavn64hFysilt4HtFRAOcqsIwg3Ic7eAJjIqssw3HlhIDAUAJnZ6j44Xy8WnoPuzColDkYBEaNP3Li9qcstLrj+bZ3owv6PyKJDQeB4V2qNXsTDRtKDGfcqtAOsoGzBx4pTGhBDco1HLW3fZ4Bl6N/5tLpvVQ7vxYdW3WusQ+Y8jlxcvyXrBdHwd4V5kgnLEhxVVOqmMEFvPtXDQP63eHo89hUAZFet6+cnxcTmoKJ4Qe9IAAB9iCbrwIDAQAB";

    // Generate your own 20 random bytes, and put them here.
    private static final byte[] SALT = new byte[] {
            20, -32, 127, -50, -10, 108, 100, -128, 41, 32, -65, -12, 67, -125, 35, 9, 12, -76, -54,
            98
    };

    private IabHelper mBillingHelper;
    private Handler mLicenseHandler;
    private boolean mLicenseChecked = false;
    private boolean mAdFreeLicense = false;
    private List<OnLicenseCheckListener> mLicenseCheckListeners = new ArrayList<>();

    private NavigationView mNavBar;
    private AppBarLayout mAppBar;
    private Toolbar mToolbar;
    private FloatingActionButton mFAB;
    private FooterBarLayout mFooter;

    private MenuItem mSearchItem = null;
    private MenuItem mClearFavoritesItem = null;

    private boolean mSearchItemVisible = false;
    private boolean mClearFavoritesItemVisible = false;

    private SearchView mSearchView = null;
    private String mSearchQuery = null;
    private boolean mSearchExpanded = false;

    private PriceCheckDatabase mPriceCheckDatabase;
    private Handler mUIHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppBar = (AppBarLayout) findViewById(R.id.appbar_layout);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

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

            mSearchQuery = savedInstanceState.getString("search_query");
            mSearchExpanded = savedInstanceState.getBoolean("search_expanded");
        }

        mPriceCheckDatabase = new PriceCheckDatabase(this);

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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && !(currentFragment instanceof BarcodeResultFragment) && currentFragment.isVisible() && mFAB.getVisibility() != View.VISIBLE) {
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

        mPriceCheckDatabase.close();

        if (mBillingHelper != null) {
            mBillingHelper.dispose();
            mBillingHelper = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("fabimgres", (Integer) findViewById(R.id.fab).getTag());
        if(mSearchView != null) {
            outState.putString("search_query", mSearchView.getQuery().toString());
            outState.putBoolean("search_expanded", !mSearchView.isIconified());
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        mSearchItem = menu.findItem(R.id.action_search);
        mSearchItem.setVisible(mSearchItemVisible);
        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when collapsed
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                //get focus
                item.getActionView().clearFocus();
                //get input method
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                return true;  // Return true to expand action view
            }
        });

        mClearFavoritesItem = menu.findItem(R.id.action_favorites_clear);
        mClearFavoritesItem.setVisible(mClearFavoritesItemVisible);
        mClearFavoritesItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(currentFragment != null && currentFragment instanceof SavedListFragment && currentFragment.isVisible()) {
                    ((SavedListFragment)currentFragment).clearFavorites();
                }

                return true;
            }
        });

        mSearchView = (SearchView) mSearchItem.getActionView();
        mSearchView.setQueryHint(getString(R.string.search_hint));
        if(mSearchQuery != null && (mSearchExpanded || mSearchQuery.length() > 0)) {
            mSearchView.setQuery(mSearchQuery, false);
            mSearchView.setIconified(false);
        } else {
            mSearchView.setIconified(!mSearchExpanded);
        }

        mSearchView.clearFocus();
        mSearchView.setMaxWidth(Integer.MAX_VALUE);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                try {
                    MainActivity.this.doSearch(s, false, 0, 0);
                } catch(IllegalStateException e) {
                    Log.e(TAG, "Create options error", e);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch(id) {
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

    protected void addOnLicenseCheckListener(OnLicenseCheckListener listener) {
        if(mLicenseChecked) {
            listener.onLicenseCheck(mAdFreeLicense);
            return;
        }

        mLicenseCheckListeners.add(listener);
    }

    protected void doSearch(String query, boolean isBarcode, int cx, int cy) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(currentFragment != null && currentFragment instanceof BarcodeResultFragment && currentFragment.isVisible()) {
            ((BarcodeResultFragment)currentFragment).doSearch(query, isBarcode);
        } else {
            getSupportFragmentManager().popBackStackImmediate("fragment_barcode_result", FragmentManager.POP_BACK_STACK_INCLUSIVE);

            BarcodeResultFragment fragment = new BarcodeResultFragment();

            Bundle args = new Bundle();
            args.putString("query", query);
            args.putBoolean("is_barcode", isBarcode);

            if(cx != 0 || cy != 0) {
                args.putInt("cx", cx);
                args.putInt("cy", cy);
            }

            fragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack("fragment_barcode_result");
            transaction.commit();
        }
    }

    protected String getDeviceID() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString("uuid", UUID.randomUUID().toString());
    }

    protected PriceCheckDatabase getPriceCheckDatabase() {
        return mPriceCheckDatabase;
    }

    protected void showFab(boolean show) {
        if(show && mFAB.getVisibility() != View.VISIBLE) {
            mFAB.show();
        } else if(!show && mFAB.getVisibility() == View.VISIBLE) {
            mFAB.hide();
        }
    }

    protected void setToolbarScroll(boolean enable) {
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags((enable) ? (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS) : 0);
        mToolbar.setLayoutParams(params);
    }

    protected void setFabIcon(final int res) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mFAB.animate().withEndAction(new Runnable() {
                @Override
                public void run() {
                    if((Integer) mFAB.getTag() != res) {
                        mFAB.setTag(res);
                        mFAB.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                            @Override
                            public void onHidden(FloatingActionButton fab) {
                                fab.setImageResource((Integer) fab.getTag());
                                fab.show();
                            }
                        });
                    } else {
                        mFAB.setImageResource(res);
                    }
                }
            }).start();
        } else {
            mFAB.setImageResource(res);
            mFAB.setTag(res);
        }
    }

    protected void setSearchQuery(String q) {
        if(mSearchView == null) {
            return;
        }

        mSearchView.setQuery(q, false);
        mSearchView.setIconified(false);
        mSearchView.clearFocus();

        if(mSearchItem != null && q.length() == 0) {
            mSearchItem.collapseActionView();
        }
    }

    protected View setFooter(int resId) {
        TypedArray arr = getTheme().obtainStyledAttributes(new int[] {R.attr.actionBarSize});
        final float translationY = -arr.getDimension(0, 0.0f);

        if(resId != 0) {
            View view = getLayoutInflater().inflate(resId, mFooter, false);

            mFooter.setVisibility(View.INVISIBLE);
            mFooter.removeAllViews();
            mFooter.addView(view);
            mFAB.clearAnimation();
            mFAB.animate()
                    .translationY(translationY)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFAB.setTranslationY(translationY);
                        }
                    })
                    .start();
            mFooter.setTranslationY(-translationY);
            mFooter.setVisibility(View.VISIBLE);
            mFooter.animate()
                    .translationY(0.0f)
                    .setListener(null)
                    .start();

            return view;
        } else {
            mFooter.animate().cancel();
            mFooter.animate()
                    .translationY(-translationY)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFooter.removeAllViews();
                            mFooter.setVisibility(View.GONE);
                        }
                    })
                    .start();

            mFAB.clearAnimation();
            mFAB.animate()
                    .translationY(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFAB.setTranslationY(0.0f);
                        }
                    })
                    .start();
            return null;
        }
    }

    protected void showClearFavorites(boolean show) {
        if(mClearFavoritesItem == null) {
            mClearFavoritesItemVisible = show;
            return;
        }

        mClearFavoritesItem.setVisible(show);
    }

    protected void showSearchItem(boolean show) {
        if(mSearchItem == null) {
            mSearchItemVisible = show;
            return;
        }

        mSearchItem.setVisible(show);
    }

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

    public abstract static class MainActivityFragment extends Fragment {
        public MainActivityFragment() {
        }

        protected void addOnLicenseCheckListener(OnLicenseCheckListener listener) {
            getMainActivity().addOnLicenseCheckListener(listener);
        }

        protected void doSearch(String query, boolean isBarcode) {
            getMainActivity().doSearch(query, isBarcode, 0, 0);
        }

        protected void doSearch(String query, boolean isBarcode, int cx, int cy) {
            getMainActivity().doSearch(query, isBarcode, cx, cy);
        }

        protected String getDeviceID() {
            return getMainActivity().getDeviceID();
        }

        protected PriceCheckDatabase getPriceCheckDatabase() {
            return getMainActivity().getPriceCheckDatabase();
        }

        protected void setToolbarScroll(boolean enable) {
            getMainActivity().setToolbarScroll(enable);
        }

        protected void showFab(boolean show) {
            getMainActivity().showFab(show);
        }

        protected void setFabIcon(int res) {
            getMainActivity().setFabIcon(res);
        }

        protected void setSearchQuery(String q) {
            getMainActivity().setSearchQuery(q);
        }

        protected View setFooter(int resId) {
            return getMainActivity().setFooter(resId);
        }

        protected void showClearFavorites(boolean show) {
            getMainActivity().showClearFavorites(show);
        }

        protected void showSearchItem(boolean show) {
           getMainActivity().showSearchItem(show);
        }

        private MainActivity getMainActivity() throws IllegalStateException {
            if(getActivity() instanceof MainActivity) {
                return (MainActivity) getActivity();
            }

            throw(new IllegalStateException("Activity is null!"));
        }
    }

    public abstract static class MainActivityPreferenceFragment extends PreferenceFragmentCompat {
        public MainActivityPreferenceFragment() {
        }

        protected void addOnLicenseCheckListener(OnLicenseCheckListener listener) {
            getMainActivity().addOnLicenseCheckListener(listener);
        }

        protected void doSearch(String query, boolean isBarcode) {
            getMainActivity().doSearch(query, isBarcode, 0, 0);
        }

        protected void doSearch(String query, boolean isBarcode, int cx, int cy) {
            getMainActivity().doSearch(query, isBarcode, cx, cy);
        }

        protected String getDeviceID() {
            return getMainActivity().getDeviceID();
        }

        protected PriceCheckDatabase getPriceCheckDatabase() {
            return getMainActivity().getPriceCheckDatabase();
        }

        protected void setToolbarScroll(boolean enable) {
            getMainActivity().setToolbarScroll(enable);
        }

        protected void showFab(boolean show) {
            getMainActivity().showFab(show);
        }

        protected void setFabIcon(int res) {
            getMainActivity().setFabIcon(res);
        }

        protected void setSearchQuery(String q) {
            getMainActivity().setSearchQuery(q);
        }

        protected View setFooter(int resId) {
            return getMainActivity().setFooter(resId);
        }

        protected void showClearFavorites(boolean show) {
            getMainActivity().showClearFavorites(show);
        }

        protected void showSearchItem(boolean show) {
            getMainActivity().showSearchItem(show);
        }

        private MainActivity getMainActivity() throws IllegalStateException {
            if(getActivity() instanceof MainActivity) {
                return (MainActivity) getActivity();
            }

            throw(new IllegalStateException("Activity is null!"));
        }
    }
}
