package net.kismetwireless.android.smarterwifimanager;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class ActivityTimeRange extends ActionBarActivity {
    FragmentTimeRange timeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlefragment);

        if (savedInstanceState != null) {
            // t imeFragment = (FragmentTimeRange) getSupportFragmentManager().findFragmentByTag("timefragment");
            timeFragment = (FragmentTimeRange) getSupportFragmentManager().getFragment(savedInstanceState, "timecontent");
        } else  if (findViewById(R.id.fragment_container) != null) {
            if (timeFragment == null) {
                timeFragment = new FragmentTimeRange();
                timeFragment.setArguments(getIntent().getExtras());
            }

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, timeFragment, "timefragment").commit();
        }

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        getSupportFragmentManager().putFragment(outState, "timecontent", timeFragment);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.timerange, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_timerange) {
            if (timeFragment != null)
                timeFragment.addTimeRange();

            return true;
        }

        return false;
    }
}
