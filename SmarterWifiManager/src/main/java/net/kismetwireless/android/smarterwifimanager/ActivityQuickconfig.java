package net.kismetwireless.android.smarterwifimanager;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;

/**
 * Created by dragorn on 10/19/13.
 */
public class ActivityQuickconfig extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        QuickDialog qDialog = new QuickDialog();
        qDialog.show(fm, "fragment_edit_name");

        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.delete_dialog_title);
        builder.setMessage(R.string.delete_learned_message);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ActivityQuickconfig.this.finish();
            }
        });

        builder.create().show();
        */

    }

    class QuickDialog extends DialogFragment {
        View dialogView;
        SharedPreferences sharedPreferences;
        WifiManager wifiManager;
        BluetoothAdapter btAdapter;

        private BroadcastReceiver bcastRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION) ||
                        intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    ActivityQuickconfig.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            configureView();
                        }
                    });
                }
            }
        };

        public QuickDialog() {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityQuickconfig.this);

            wifiManager = (WifiManager) ActivityQuickconfig.this.getSystemService(Context.WIFI_SERVICE);
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        private void configureView() {
            if (dialogView == null) {
                return;
            }

            CompoundButton wifiSwitch = (CompoundButton) dialogView.findViewById(R.id.quickSwitchWifi);
            CompoundButton btSwitch = (CompoundButton) dialogView.findViewById(R.id.quickSwitchBt);
            CompoundButton enableSwitch = (CompoundButton) dialogView.findViewById(R.id.quickSwitchEnable);

            if (sharedPreferences.getBoolean(getString(R.string.pref_enable), true)) {
                enableSwitch.setChecked(true);
            } else {
                enableSwitch.setChecked(false);
            }

            enableSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean checked = ((CompoundButton) view).isChecked();
                    setManageWifi(checked);
                }
            });

            int ws = wifiManager.getWifiState();
            if (ws == WifiManager.WIFI_STATE_ENABLED || ws == WifiManager.WIFI_STATE_ENABLING) {
                wifiSwitch.setChecked(true);
            } else {
                wifiSwitch.setChecked(false);
            }

            int bs = btAdapter.getState();
            if (bs == BluetoothAdapter.STATE_ON || bs == BluetoothAdapter.STATE_TURNING_ON) {
                btSwitch.setChecked(true);
            } else {
                btSwitch.setChecked(false);
            }

            wifiSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean checked = ((CompoundButton) view).isChecked();

                    wifiManager.setWifiEnabled(checked);
                }
            });

            btSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean checked = ((CompoundButton) view).isChecked();

                    if (checked) {
                        btAdapter.enable();
                    } else {
                        btAdapter.disable();
                    }

                }
            });

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_quickconfig, container);

            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

            Button closeButton = (Button) view.findViewById(R.id.quickerButtonClose);
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickDialog.this.getDialog().dismiss();
                }
            });

            View settingsView = (View) view.findViewById(R.id.quickerSettings);
            settingsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    QuickDialog.this.getDialog().dismiss();

                    startActivity(i);
                }
            });

            View swmView = (View) view.findViewById(R.id.quickerManager);
            swmView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(ActivityQuickconfig.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    QuickDialog.this.getDialog().dismiss();

                    startActivity(i);
                }
            });

            dialogView = view;

            IntentFilter intf = new IntentFilter();
            intf.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            intf.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(bcastRx, intf);

            configureView();

            return view;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            unregisterReceiver(bcastRx);
            ActivityQuickconfig.this.finish();
        }

        private boolean setManageWifi(boolean b) {
            SharedPreferences.Editor e = sharedPreferences.edit();
            e.putBoolean(getString(R.string.pref_enable), b);
            e.commit();

            SmarterWifiServiceBinder binder = new SmarterWifiServiceBinder(ActivityQuickconfig.this);
            binder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
                @Override
                public void run(SmarterWifiServiceBinder b) {
                    b.doUpdatePreferences();
                }
            });

            return true;
        }
    }

}
