package net.kismetwireless.android.smarterwifimanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by dragorn on 9/2/13.
 */
public class NetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);

        if (!p.getBoolean("start_boot", true)) {
            if (!SmarterWifiServiceBinder.isServiceRunning(context)) {
                Log.d("smarter", "Would have done something but service isn't running and we're not autostarting");
                return;
            }
        }

        final SmarterWifiServiceBinder serviceBinder = new SmarterWifiServiceBinder(context);

        try {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
                    public void run(SmarterWifiServiceBinder b) {
                        b.configureWifiState();
                        serviceBinder.doUnbindService();
                    }
                });
            }

            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                final NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (ni.getType() != ConnectivityManager.TYPE_WIFI)
                    return;

                serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
                    public void run(SmarterWifiServiceBinder b) {
                        b.configureWifiState();
                        serviceBinder.doUnbindService();
                    }
                });

            }
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
                    public void run(SmarterWifiServiceBinder b) {
                        b.configureBluetoothState();
                    }
                });
            }

            if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                final BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);

                serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
                    public void run(SmarterWifiServiceBinder b) {
                        b.handleBluetoothDeviceState(bluetoothDevice, state);
                    }
                });

                // Log.d("smarter", "bcast rx got bt device " + bluetoothDevice.getAddress() + " " + bluetoothDevice.getName() + " state " + state);
            }

            Log.d("smarter", "bcase rx: " + intent.getAction());

            if (intent.getAction().equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                final int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

                Log.d("smarter", "got wifi p2p state " + state);

                if (state != -1) {
                    serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
                        public void run(SmarterWifiServiceBinder b) {
                            b.handleWifiP2PState(state);
                        }
                    });
                }

            }

        } catch (NullPointerException npe) {
            // Don't care
        }

    }
}
