package net.kismetwireless.android.smarterwifimanager;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * Created by dragorn on 9/24/13.
 */
public class ActivityPrefs extends Activity {
    SmarterWifiServiceBinder binder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binder = new SmarterWifiServiceBinder(this);
        binder.doBindService();

        ActionBar ab = getActionBar();

        ab.setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new FragmentPrefs()).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Try to update prefs as we go out
        if (binder != null) {
            binder.doUpdatePreferences();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
