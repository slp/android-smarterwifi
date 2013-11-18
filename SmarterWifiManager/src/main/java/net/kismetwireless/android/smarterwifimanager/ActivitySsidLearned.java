package net.kismetwireless.android.smarterwifimanager;

import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;

public class ActivitySsidLearned extends ActionBarActivity {
    FragmentLearned ssidLearned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlefragment);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            ssidLearned = (FragmentLearned) getSupportFragmentManager().findFragmentByTag("learnedfragment");
        } else if (findViewById(R.id.fragment_container) != null) {
            ssidLearned = new FragmentLearned();
            ssidLearned.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, ssidLearned, "learnedfragment").commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.ssid_blacklist, menu);
        return true;
    }

}
