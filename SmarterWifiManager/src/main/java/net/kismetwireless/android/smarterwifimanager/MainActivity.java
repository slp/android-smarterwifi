package net.kismetwireless.android.smarterwifimanager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;


// Main icon color shifts
// 00e8d5    b8b8b8    a40000

public class MainActivity extends Activity {
    Context context;

    SmarterWifiServiceBinder serviceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        serviceBinder = new SmarterWifiServiceBinder(this);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Defer UI creation until we've bound to the service
        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            @Override
            public void run(SmarterWifiServiceBinder b) {
                ActionBar.Tab tabMain = actionBar.newTab().setText(R.string.tab_main);
                ActionBar.Tab tabIgnore = actionBar.newTab().setText(R.string.tab_ignore);
                ActionBar.Tab tabLearned = actionBar.newTab().setText(R.string.tab_learned);
                ActionBar.Tab tabBluetooth = actionBar.newTab().setText(R.string.tab_bluetooth);

                final FragmentMain fragmentMain = new FragmentMain();
                final FragmentSsidBlacklist fragmentSsid = new FragmentSsidBlacklist();
                final FragmentLearned fragmentLearned = new FragmentLearned();
                final FragmentBluetoothBlacklist fragmentBluetooth = new FragmentBluetoothBlacklist();

                tabMain.setTabListener(new SmarterTabsListener(fragmentMain));
                tabIgnore.setTabListener(new SmarterTabsListener(fragmentSsid));
                tabLearned.setTabListener(new SmarterTabsListener(fragmentLearned));
                tabBluetooth.setTabListener(new SmarterTabsListener(fragmentBluetooth));

                actionBar.addTab(tabMain);
                actionBar.addTab(tabIgnore);
                actionBar.addTab(tabBluetooth);
                actionBar.addTab(tabLearned);
            }

        });

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

    class SmarterTabsListener implements ActionBar.TabListener {
        public Fragment fragment;

        public SmarterTabsListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            //do what you want when tab is reselected, I do nothing
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.replace(R.id.fragment_placeholder, fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
        }
    }
}
