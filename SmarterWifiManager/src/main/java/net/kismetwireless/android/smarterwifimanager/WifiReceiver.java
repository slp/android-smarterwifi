package net.kismetwireless.android.smarterwifimanager;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * Created by dragorn on 9/2/13.
 */
public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final SmarterWifiServiceBinder serviceBinder = new SmarterWifiServiceBinder(context);

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

        if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {

        }

    }
}
