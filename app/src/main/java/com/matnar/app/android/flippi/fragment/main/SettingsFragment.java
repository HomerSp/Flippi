package com.matnar.app.android.flippi.fragment.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.ListPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.matnar.app.android.flippi.R;
import com.matnar.app.android.flippi.activity.MainActivity;

public class SettingsFragment extends MainActivity.MainActivityPreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Flippi." + SettingsFragment.class.getSimpleName();

    public SettingsFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        getHelper().setFooter(0);
        getHelper().setFabIcon(0);
        getHelper().showClearFavorites(false);
        getHelper().showSearchItem(false);
        getHelper().setActionBarTitle(getContext().getString(R.string.app_name));

        return view;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);

        onSharedPreferenceChanged(getPreferenceManager().getSharedPreferences(), "region");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(getContext() == null || getPreferenceScreen() == null) {
            return;
        }

        if(key.equals("region")) {
            ListPreference preference = (ListPreference) getPreferenceScreen().findPreference(key);
            int index = preference.findIndexOfValue(preference.getValue());
            if(index < 0) {
                return;
            }

            String value = preference.getValue();
                if(value.equals("uk")) {
                preference.setIcon(R.drawable.ic_region_uk);
            } else if(value.equals("us")) {
                preference.setIcon(R.drawable.ic_region_us);
            }
        }
    }
}
