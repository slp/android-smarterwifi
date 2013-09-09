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
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;

public class SmarterWifiService extends Service {
    private boolean shutdown = false;

    SharedPreferences preferences;

    TelephonyManager telephonyManager;
    SmarterPhoneListener phoneListener;
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;

    private boolean disableWifi = true;
    private int enableWaitSeconds = 30;

    private String currentSsid;
    private CellLocationCommon currentCellLocation;

    private boolean wifiEnabled = false;
    private boolean networkConnected = false;

    private boolean learningCell = false;
    private boolean inActiveCell = false;

    private Handler timerHandler = new Handler();

    private SmarterDBSource dbSource;

    public static abstract class SmarterServiceCallback {
        public void handleLearningMode(boolean learning) {
            return;
        }

        public void handleActiveMode(boolean active) {
            return;
        }
    }

    ArrayList<SmarterServiceCallback> callbackList = new ArrayList<SmarterServiceCallback>();

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

        context = this;

        currentSsid = new String("");
        currentCellLocation = new CellLocationCommon((CellLocation) null);

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

        phoneListener = new SmarterPhoneListener();

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CELL_LOCATION);

        // Kick an update
        setWifiRunning(wifiManager.isWifiEnabled());
        setNetworkConfigured(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected());

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
        return START_STICKY;
    }

    // Wifi is running, set timers to turn it off if we haven't gotten a configuration
    public void setWifiRunning(boolean running) {
        // Nothing to do here, we're already running
        if (running && learningCell) return;

        // If we've been enabled, start our countdown for turning off
        if (!wifiEnabled && running) {
            startWifiCountdown();
        }

        if (!running) {
            networkConnected = false;

            setLearningCell(false);
            wifiEnabled = false;
            currentSsid = null;

            // If wifi is shutting down and we're in an active cell, force it back on
            if (inActiveCell) {
                if (getAirplaneMode()) {
                    Log.d("smarter", "We think we're in an active cell but we're also in airplane mode");
                } else {
                    Log.d("smarter", "we're in an active cell, turning wifi back on");
                    wifiManager.setWifiEnabled(true);
                }
            }
        }
    }

    // Network is configured, we have a valid wifi state
    public void setNetworkConfigured(boolean configured) {
        // If we haven't connected and we weren't connected, do nothing.
        if (!networkConnected && !configured) {
            return;
        }

        // If we've connected to a network, cancel any wifi killer we have running, and
        // enable learning new cell
        if (!networkConnected && configured) {
            networkConnected = true;

            timerHandler.removeCallbacks(wifiDisableTask);

            // Wifi must be enabled...
            wifiEnabled = true;

            setLearningCell(true);

            WifiInfo wi = wifiManager.getConnectionInfo();

            if (wi == null) {
                Log.d("smarter", "Something weird - wifiinfo null but network connected?");
                return;
            }

            currentSsid = wi.getSSID();

            Log.d("smarter", "We've connected to wifi " + currentSsid);

            // Kick a forced update
            handleCellLocation(null);
            return;
        }

        // Otherwise we've lost the connection we had
        if (networkConnected && !configured) {
            Log.d("smarter", "We've lost the network connection");

            networkConnected = false;

            // We're not learning anymore since we're not connected
            setLearningCell(false);

            currentSsid = "";

            // Start counting down to turning off - this can be overridden if we're still in an active cell
            if (!inActiveCell)
                startWifiCountdown();

            return;
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
        // If we're not proctoring wifi...
        if (!disableWifi) return;

        // If we've moved into a cell we're supposed to be active in
        if (inActiveCell) return;

        // If wifi isn't turned on at all
        if (!wifiEnabled) return;

        // If we're actually connected to a network again
        if (networkConnected) return;

        // Restart the countdown incase we got called while a countdown was already running
        timerHandler.removeCallbacks(wifiDisableTask);

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

        if (clc.getTowerId() < 0) {
            Log.d("smarter", "Got invalid tower id - out of range or something is up");
            setActiveCell(false);
            return;
        }

        boolean movedactive = false;

        // If we're in an active area...
        if (dbSource.queryTowerMapped(clc.getTowerId())) {
            setActiveCell(true);
            movedactive = true;

            // If wifi isn't turned on, and we're not in the middle of trying to turn it on... turn it on
            if (!wifiEnabled && wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) {
                if (getAirplaneMode()) {
                    Log.d("smarter", "Would try to activate wifi but we're in airplane mode");
                } else {
                    Log.d("smarter", "We're in range of " + clc.getTowerId() + " and we think we should turn on wifi");
                    wifiManager.setWifiEnabled(true);
                }
            }
        }

        // If we're learning, update this tower in the database - learningCell means we've got
        // an active network and we're recording cells.  Learn new towers and update existing ones
        if (learningCell && !clc.equals(currentCellLocation)) {
            setActiveCell(true);
            movedactive = true;
            dbSource.mapTower(currentSsid, clc.getTowerId());
        }

        currentCellLocation = clc;

        if (!movedactive) {
            // We're not learning about where we are (so we're not online), we don't know this tower
            // (so we shouldn't stay on)... So start the countdown to shut off wifi
            setActiveCell(false);
            Log.d("smarter", "No wifi connection, unknown tower " + clc.getTowerId() + " starting timer to shut down");
            startWifiCountdown();
        }
    }

    private class SmarterPhoneListener extends PhoneStateListener {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            handleCellLocation(location);
        }
    }

    public void addCallback(SmarterServiceCallback cb) {
        synchronized (callbackList) {
            callbackList.add(cb);
        }

        // Call our CBs immediately for setup
        cb.handleLearningMode(learningCell);
        cb.handleActiveMode(inActiveCell);
    }

    public void removeCallback(SmarterServiceCallback cb) {
        synchronized (callbackList) {
            callbackList.remove(cb);
        }
    }

    private void setLearningCell(boolean learn) {
        learningCell = learn;

        synchronized (callbackList) {
            for (SmarterServiceCallback cb : callbackList) {
                cb.handleLearningMode(learn);
            }
        }
    }

    private void setActiveCell(boolean active) {
        inActiveCell = active;

        synchronized (callbackList) {
            for (SmarterServiceCallback cb : callbackList) {
                cb.handleActiveMode(active);
            }
        }
    }

    public boolean getLearningCell() {
        return learningCell;
    }

    public boolean getActiveCell() {
        return inActiveCell;
    }

    public boolean getAirplaneMode() {
        return Settings.System.getInt(context.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }
}