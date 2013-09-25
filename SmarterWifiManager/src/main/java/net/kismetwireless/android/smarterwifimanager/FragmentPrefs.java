package net.kismetwireless.android.smarterwifimanager;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Created by dragorn on 9/24/13.
 */
public class FragmentPrefs extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("smarter", "Loading prefs xml");
        addPreferencesFromResource(R.xml.main_prefs);
    }
}
