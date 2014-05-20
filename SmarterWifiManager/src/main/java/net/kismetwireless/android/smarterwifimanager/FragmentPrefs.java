package net.kismetwireless.android.smarterwifimanager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by dragorn on 9/24/13.
 */
public class FragmentPrefs extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    ListPreference timeoutPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.main_prefs);

        timeoutPref = (ListPreference) getPreferenceScreen().findPreference(getString(R.string.prefs_item_shutdowntime));

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        setPrefsSummary();
    }

    @Override
    public void onResume() {
        super.onResume();

        setPrefsSummary();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        setPrefsSummary();
    }

    private void setPrefsSummary() {
        CharSequence pt = timeoutPref.getEntry();

        String s = "";
        if (pt != null)
            s = pt.toString() + "\n";

        timeoutPref.setSummary(s + getString(R.string.prefs_item_shutdowntime_explanation));
    }
}
