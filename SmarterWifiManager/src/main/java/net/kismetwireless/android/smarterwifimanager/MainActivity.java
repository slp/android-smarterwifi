package net.kismetwireless.android.smarterwifimanager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


// Main icon color shifts
// 00e8d5    b8b8b8    a40000

public class MainActivity extends Activity {
    Context context;

    private static int PREFS_REQ = 1;

    SmarterWifiServiceBinder serviceBinder;
    SmarterPagerAdapter pagerAdapter;
    ViewPager viewPager;
    ActionBar actionBar;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        serviceBinder = new SmarterWifiServiceBinder(this);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);

        viewPager = (ViewPager) findViewById(R.id.pager);

        // Defer UI creation until we've bound to the service
        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            @Override
            public void run(SmarterWifiServiceBinder b) {
                List<Fragment> fragments = new Vector<Fragment>();

                fragments.add(Fragment.instantiate(context, FragmentMain.class.getName()));
                fragments.add(Fragment.instantiate(context, FragmentSsidBlacklist.class.getName()));
                fragments.add(Fragment.instantiate(context, FragmentLearned.class.getName()));
                fragments.add(Fragment.instantiate(context, FragmentBluetoothBlacklist.class.getName()));

                pagerAdapter = new SmarterPagerAdapter(getFragmentManager(), fragments);

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

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (actionBar != null)
            outState.putInt("tabposition", actionBar.getSelectedTab().getPosition());

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivityForResult(new Intent(MainActivity.this, PrefsActivity.class), PREFS_REQ);
            return true;
        }

        if (item.getItemId() == R.id.action_about) {
            showAbout();
            return true;
        }

        return true;
    }

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

}
