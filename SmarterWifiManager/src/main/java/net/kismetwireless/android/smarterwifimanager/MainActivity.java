package net.kismetwireless.android.smarterwifimanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


// Main icon color shifts
// 00e8d5    b8b8b8    a40000

public class MainActivity extends ActionBarActivity {
    Context context;

    private static int PREFS_REQ = 1;

    private SmarterWifiServiceBinder serviceBinder;
    // private SmarterPagerAdapter pagerAdapter;
    // private ViewPager viewPager;
    private ActionBar actionBar;

    private ArrayList<Integer[]> drawerContent = new ArrayList<Integer[]>();

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private DualDrawerListAdapter listAdapter;

    private FragmentMain mainFragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        serviceBinder = new SmarterWifiServiceBinder(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerContent.add(new Integer[] {R.string.nav_learned, R.drawable.ic_action_save});
        drawerContent.add(new Integer[] {R.string.nav_ignore, R.drawable.ic_action_bad});
        drawerContent.add(new Integer[] {R.string.nav_bluetooth, R.drawable.ic_action_bluetooth_connected});
        drawerContent.add(new Integer[] {R.string.nav_time, R.drawable.ic_action_time});

        listAdapter = new DualDrawerListAdapter(this, R.layout.drawer_list_item, drawerContent);

        drawerList.setAdapter(listAdapter);
        drawerList.setOnItemClickListener(new DrawerItemClickListener());


        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        // actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // actionBar.setDisplayHomeAsUpEnabled(false);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        if (savedInstanceState != null) {
            mainFragment = (FragmentMain) getSupportFragmentManager().findFragmentByTag("mainfragment");
        } else if (findViewById(R.id.fragment_container) != null) {
            mainFragment = new FragmentMain();
            mainFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainFragment, "mainfragment").commit();
        }

        /*
        viewPager = (ViewPager) findViewById(R.id.pager);

        // Defer UI creation until we've bound to the service
        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            @Override
            public void run(SmarterWifiServiceBinder b) {
                List<Fragment> fragments = new Vector<Fragment>();

                fragments.add(Fragment.instantiate(context, FragmentMain.class.getName()));
                // fragments.add(Fragment.instantiate(context, FragmentSsidBlacklist.class.getName()));
                fragments.add(Fragment.instantiate(context, FragmentLearned.class.getName()));
                // fragments.add(Fragment.instantiate(context, FragmentBluetoothBlacklist.class.getName()));
                // fragments.add(Fragment.instantiate(context, FragmentTimeRange.class.getName()));

                pagerAdapter = new SmarterPagerAdapter(getSupportFragmentManager(), fragments);

                for (int x = 0; x < pagerAdapter.getCount(); x++) {
                    SmarterFragment sf = (SmarterFragment) pagerAdapter.getItem(x);

                    ActionBar.Tab t = actionBar.newTab().setText(getString(sf.getTitle()));
                    t.setTabListener(new SmarterTabsListener(sf));
                    actionBar.addTab(t);
                }

                viewPager.setAdapter(pagerAdapter);

                viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        actionBar.setSelectedNavigationItem(position);
                    }
                });

                if (savedInstanceState != null) {
                    actionBar.setSelectedNavigationItem(savedInstanceState.getInt("tabposition", 0));
                }
            }

        });
        */

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /*
        try {
            outState.putInt("tabposition", actionBar.getSelectedTab().getPosition());
        } catch (NullPointerException npe) {
            Log.d("smarter", "tried to save sate but got a null in getSelectedTab(): " + npe);
        }
        */
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceBinder != null)
            serviceBinder.doUnbindService();
    }

      @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
          return true;
        }

        if (item.getItemId() == R.id.action_settings) {
            startActivityForResult(new Intent(MainActivity.this, ActivityPrefs.class), PREFS_REQ);
            return true;
        }

        if (item.getItemId() == R.id.action_about) {
            showAbout();
            return true;
        }

        return true;
    }

    /*
    public class SmarterTabsListener implements ActionBar.TabListener {
        public Fragment fragment;

        public SmarterTabsListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // ignore
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // show tab
            viewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // hide
        }
    }

    public class SmarterPagerAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> fragments = new ArrayList<Fragment>();

        public SmarterPagerAdapter(FragmentManager fm, List<Fragment> frags) {
            super(fm);

            fragments = frags;
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
    */

    public void showAbout() {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        WebView wv = new WebView(this);

        wv.loadUrl("file:///android_asset/html_no_copy/about.html");

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

                return true;
            }
        });

        alert.setView(wv);

        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        alert.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFS_REQ) {
            serviceBinder.doUpdatePreferences();
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            Integer[] item = listAdapter.getItem(position);

            switch (item[0]) {
                case R.string.nav_learned:
                    startActivity(new Intent(MainActivity.this, ActivitySsidLearned.class));
                    drawerLayout.closeDrawer(drawerList);
                    break;
                case R.string.nav_ignore:
                    startActivity(new Intent(MainActivity.this, ActivitySsidBlacklist.class));
                    drawerLayout.closeDrawer(drawerList);
                    break;
                case R.string.nav_bluetooth:
                    startActivity(new Intent(MainActivity.this, ActivityBluetoothBlacklist.class));
                    drawerLayout.closeDrawer(drawerList);
                    break;
                case R.string.nav_time:
                    startActivity(new Intent(MainActivity.this, ActivityTimeRange.class));
                    drawerLayout.closeDrawer(drawerList);
                    break;
            }
        }
    }

    public class DualDrawerListAdapter extends ArrayAdapter<Integer[]> {
        private int layoutResourceId;

        public DualDrawerListAdapter(Context context, int textViewResourceId, ArrayList<Integer[]> items) {
            super(context, textViewResourceId, items);
            layoutResourceId = textViewResourceId;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                final Integer[] entry = getItem(position);

                View v = null;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    v = inflater.inflate(layoutResourceId, null);
                } else {
                    v = convertView;
                }

                TextView text1 = (TextView) v.findViewById(R.id.text1);
                ImageView img1 = (ImageView) v.findViewById(R.id.image1);

                text1.setText(getString(entry[0]));
                img1.setImageResource(entry[1]);

                return v;
            } catch (Exception ex) {
                Log.e("smarter", "error", ex);
                return null;
            }
        }
    }
}
