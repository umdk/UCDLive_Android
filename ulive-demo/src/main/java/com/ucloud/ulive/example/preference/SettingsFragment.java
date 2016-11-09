package com.ucloud.ulive.example.preference;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.ucloud.ulive.common.util.DeviceUtils;
import com.ucloud.ulive.example.R;


public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private Preference mAboutPref;

    public static Fragment newInstance() {
        SettingsFragment f = new SettingsFragment();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAboutPref = findPreference("pref.about");
        mAboutPref.setSummary(DeviceUtils.getDeviceInfo());
        mAboutPref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "pref.about":
                new AlertDialog.Builder(getActivity())
                        .setTitle("About")
                        .setMessage(DeviceUtils.getDeviceInfo())
                        .show();
                break;
            default:
                break;
        }
        return false;
    }
}
