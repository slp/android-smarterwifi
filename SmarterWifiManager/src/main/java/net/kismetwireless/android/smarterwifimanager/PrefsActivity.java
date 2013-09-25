package net.kismetwireless.android.smarterwifimanager;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by dragorn on 9/24/13.
 */
public class PrefsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new FragmentPrefs()).commit();
    }
}
