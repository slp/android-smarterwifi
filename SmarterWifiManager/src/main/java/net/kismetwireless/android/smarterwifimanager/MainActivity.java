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
                ActionBar.Tab tab1 = actionBar.newTab().setText(R.string.tab_main);
                ActionBar.Tab tab2 = actionBar.newTab().setText(R.string.tab_ignore);
                ActionBar.Tab tab3 = actionBar.newTab().setText(R.string.tab_learned);

                final FragmentMain fragmentMain = new FragmentMain(b);
                final FragmentSsidBlacklist fragmentSsid = new FragmentSsidBlacklist(b);
                final FragmentLearned fragmentLearned = new FragmentLearned(b);

                tab1.setTabListener(new SmarterTabsListener(fragmentMain));
                tab2.setTabListener(new SmarterTabsListener(fragmentSsid));
                tab3.setTabListener(new SmarterTabsListener(fragmentLearned));

                actionBar.addTab(tab1);
                actionBar.addTab(tab2);
                actionBar.addTab(tab3);
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
