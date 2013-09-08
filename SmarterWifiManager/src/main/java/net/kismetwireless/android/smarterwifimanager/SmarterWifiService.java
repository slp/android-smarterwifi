package net.kismetwireless.android.smarterwifimanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SmarterWifiService extends Service {
    private boolean shutdown = false;

    SharedPreferences preferences;

    TelephonyManager telephonyManager;
    SmarterPhoneListener phoneListener;
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;

    private boolean disableWifi = true;
    private int enableWaitSeconds = 25;

    private String currentSsid;

    private boolean wifiEnabled;
    private boolean networkConnected;

    private boolean learningCell;
    private boolean inActiveCell;

    private Handler timerHandler = new Handler();

    private SmarterDBSource dbSource;

    // Minimal in the extreme binder which returns the service for direct calling
    public class ServiceBinder extends Binder {
        SmarterWifiService getService() {
            return SmarterWifiService.this;
        }
    }

    private ServiceBinder serviceBinder = new ServiceBinder();
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager == null) {
            Log.d("smarter", "No telephony manager!?");
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        shutdown = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        updatePreferences();

        try {
            dbSource = new SmarterDBSource(context);
        } catch (SQLiteException e) {
            Log.d("smarter", "failed to open database: " + e);
        }

        // Default wifi state
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        setWifiRunning(wifiManager.isWifiEnabled());

        // Default network state
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        setNetworkConfigured(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected());

        phoneListener = new SmarterPhoneListener();

        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CELL_LOCATION);

        // Kick an update
        setWifiRunning(wifiManager.isWifiEnabled());

        return START_STICKY;
    }

    // Wifi is running, set timers to turn it off if we haven't gotten a configuration
    public void setWifiRunning(boolean running) {
        Log.d("smarter", "setwifirunning " + running);
        // If we've been enabled, start our countdown for turning off
        if (!wifiEnabled && running) {
            startWifiCountdown();
        }

        if (!running) {
            networkConnected = false;
            learningCell = false;
            wifiEnabled = false;
            currentSsid = null;
            return;
        }

        // If wifi is shutting down and we're in an active cell, force it back on
        if (!running && inActiveCell) {
            wifiManager.setWifiEnabled(true);
        }
    }

    // Network is configured, we have a valid wifi state
    public void setNetworkConfigured(boolean configured) {
        networkConnected = configured;

        // If we've connected to a network, cancel any wifi killer we have running, and
        // enable learning new cell
        if (networkConnected) {
            timerHandler.removeCallbacks(wifiDisableTask);

            wifiEnabled = true;
            learningCell = true;

            WifiInfo wi = wifiManager.getConnectionInfo();

            if (wi == null) {
                Log.d("smarter", "Something weird - wifiinfo null but network connected?");
                return;
            }

            currentSsid = wi.getSSID();

            Log.d("smarter", "We're connected to wifi " + currentSsid);

            // Kick the update
            handleCellLocation(null);
        }
    }

    public void updatePreferences() {

    }

    public void shutdownService() {
        shutdown = true;
        timerHandler.removeCallbacks(wifiDisableTask);
        telephonyManager.listen(phoneListener, 0);
    }

    private void startWifiCountdown() {
        if (!wifiEnabled) return;

        if (networkConnected) return;

        // If we're not proctoring wifi...
        if (!disableWifi) return;

        // If we've moved into a cell we're active in
        if (inActiveCell) return;

        // Set a timer - if we haven't connected to a network by the time this
        // expires, shut down wifi again
        timerHandler.postDelayed(wifiDisableTask, enableWaitSeconds * 1000);

        Log.d("smarter", "Starting countdown of " + enableWaitSeconds + " for wifi to be enabled");
    }

    private Runnable wifiDisableTask = new Runnable() {
        public void run() {
            if (shutdown) return;

            // If we're not proctoring wifi...
            if (!disableWifi) return;

            // Don't shut down wifi if we have a network connection
            if (networkConnected) return;

            Log.d("smarter", "Shutting down wi-fi, we haven't gotten a link");
            wifiManager.setWifiEnabled(false);
        }
    };

    private void handleCellLocation(CellLocation location) {
        if (location == null) {
            location = telephonyManager.getCellLocation();
        }

        CellLocationCommon clc = new CellLocationCommon(location);

        Log.d("smarter", "clc " + clc.getTowerId());

        if (clc.getTowerId() < 0)
            return;

        if (learningCell) {
            // If we're learning, update this tower in the database
            dbSource.mapTower(currentSsid, clc.getTowerId());
        } else  if (dbSource.queryTowerMapped(clc.getTowerId())) {
            // Otherwise, if we exist in the mapping, we should turn on... force staying on
            // while in this cell...
            inActiveCell = true;

            // And on wifi, it'll call back to us when it enables and start the countdown
            if (!wifiEnabled) {
                wifiManager.setWifiEnabled(true);
            }
        }
    }

    private class SmarterPhoneListener extends PhoneStateListener {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            handleCellLocation(location);
        }
    }
}