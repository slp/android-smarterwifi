package net.kismetwireless.android.smarterwifimanager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.Switch;


// Main icon color shifts
// 00e8d5    b8b8b8    a40000

public class MainActivity extends Activity {
    SmarterWifiServiceBinder serviceBinder;
    Context context;

    Switch switchManageWifi, switchAutoLearn;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        serviceBinder = new SmarterWifiServiceBinder(context);
        serviceBinder.doBindService();

        switchManageWifi = (Switch) findViewById(R.id.switchManageWifi);
        switchAutoLearn =  (Switch) findViewById(R.id.switchAutoLearn);

        switchManageWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b == false) {
                    switchAutoLearn.setEnabled(false);
                } else {
                    switchAutoLearn.setEnabled(true);
                }

                setManageWifi(b);
            }
        });

        switchAutoLearn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setLearnWifi(b);
            }
        });

        if (sharedPreferences.getBoolean(getString(R.string.pref_enable), true)) {
            switchManageWifi.setChecked(true);
        } else {
            switchManageWifi.setChecked(false);
        }

        if (sharedPreferences.getBoolean(getString(R.string.pref_learn), true)) {
            switchAutoLearn.setChecked(true);
        } else {
            switchAutoLearn.setChecked(false);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setManageWifi(boolean b) {
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putBoolean(getString(R.string.pref_enable), b);
        e.commit();

        serviceBinder.doUpdatePreferences();
    }

    private void setLearnWifi(boolean b) {
        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putBoolean(getString(R.string.pref_learn), b);
        e.commit();
    }
    
}
