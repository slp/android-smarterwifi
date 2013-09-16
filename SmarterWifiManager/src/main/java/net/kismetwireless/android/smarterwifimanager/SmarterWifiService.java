package net.kismetwireless.android.smarterwifimanager;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;

public class SmarterWifiService extends Service {
    public enum ControlType {
        CONTROL_USER, CONTROL_RANGE, CONTROL_TOWERID, CONTROL_GEOFENCE, CONTROL_BLUETOOTH, CONTROL_TIME
    }

    public enum WifiState {
        WIFI_BLOCKED, WIFI_ON, WIFI_OFF, WIFI_IDLE, WIFI_IGNORE
    }

    public enum LearningState {
        LEARNING_OFF, LEARNING_TOWER, LEARNING_BLOCKED, LEARNING_IDLE
    }

    public enum TowerType {
        TOWER_UNKNOWN, TOWER_BLOCK, TOWER_ENABLE
    }

    public enum ControlState {
        // Hard block, ignoring, enable, soft disable
        CONTROL_BLOCK, CONTROL_IGNORE, CONTROL_ENABLE, CONTROL_DISABLE;
    }

    private boolean shutdown = false;

    SharedPreferences preferences;

    TelephonyManager telephonyManager;
    SmarterPhoneListener phoneListener;
    WifiManager wifiManager;
    ConnectivityManager connectivityManager;
    private NotificationManager notificationManager;

    private boolean proctorWifi = true;
    private int enableWaitSeconds = 1;
    private int disableWaitSeconds = 30;

    private WifiState userOverrideState = WifiState.WIFI_IGNORE;

    private String currentSsid;
    private CellLocationCommon currentCellLocation;
    private TowerType currentTowerType = TowerType.TOWER_UNKNOWN;

    private ControlType lastControlReason = ControlType.CONTROL_TOWERID;

    private Handler timerHandler = new Handler();

    private SmarterDBSource dbSource;

    private NotificationCompat.Builder notificationBuilder;

    public static abstract class SmarterServiceCallback {
        protected ControlState controlState;
        protected ControlType controlType;
        protected WifiState wifiState;
        protected String lastSsid;
        protected TowerType towerType;

        public void wifiStateChanged(String ssid, WifiState state) {
            lastSsid = ssid;
            wifiState = state;

            return;
        }

        public void targetStateChanged(ControlState target, ControlType type) {
            controlState = target;
            controlType = type;

            return;
        }

        public void towerStateChanged(long towerid, TowerType type) {
            towerType = type;

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

    private SmarterServiceCallback notifcationCallback = new SmarterServiceCallback() {
        @Override
        public void wifiStateChanged(String ssid, WifiState state) {
            int wifiIconId = R.drawable.custom_wifi_inactive;
            String wifiText = "";

            switch (state) {
                case WIFI_IDLE:
                    wifiIconId = R.drawable.custom_wifi_inactive;
                    wifiText = "Wi-Fi idle / disconnected";
                    break;
                case WIFI_BLOCKED:
                    wifiIconId = R.drawable.custom_wifi_disabled_tower;
                    wifiText = "Wi-Fi disabled by location";
                    break;
                case WIFI_ON:
                    wifiIconId = R.drawable.custom_wifi_enabled;
                    wifiText = "Wi-Fi enabled";
                    break;
                case WIFI_OFF:
                    wifiIconId = R.drawable.custom_wifi_inactive;
                    wifiText = "Wi-Fi turned off";
                    break;

                default:
                    wifiIconId = R.drawable.custom_wifi_inactive;
            }

            notificationBuilder.setSmallIcon(wifiIconId);
            notificationBuilder.setContentText(wifiText);

            notificationManager.notify(0, notificationBuilder.build());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        updatePreferences();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        try {
            dbSource = new SmarterDBSource(context);
        } catch (SQLiteException e) {
            Log.d("smarter", "failed to open database: " + e);
        }

        // Default network state
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        phoneListener = new SmarterPhoneListener();

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CELL_LOCATION);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Kick an update
        setCurrentTower(new CellLocationCommon((CellLocation) null));

        notificationBuilder = new NotificationCompat.Builder(context);

        // Make the notification
        notificationBuilder.setSmallIcon(R.drawable.custom_wifi_enabled);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setOngoing(true);

        notificationManager.notify(0, notificationBuilder.build());

        addCallback(notifcationCallback);

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

    public void updatePreferences() {

    }

    public void shutdownService() {
        shutdown = true;
        timerHandler.removeCallbacks(wifiDisableTask);
        telephonyManager.listen(phoneListener, 0);
    }

    private void startWifiEnable() {
        // Restart the enable
        timerHandler.removeCallbacks(wifiEnableTask);

        timerHandler.postDelayed(wifiEnableTask, enableWaitSeconds * 1000);

        Log.d("smarter", "Starting countdown of " + enableWaitSeconds + " to enable wifi");

    }

    private void startWifiShutdown() {
        // Restart the countdown incase we got called while a countdown was already running
        timerHandler.removeCallbacks(wifiDisableTask);

        // Set a timer - if we haven't connected to a network by the time this
        // expires, shut down wifi again
        timerHandler.postDelayed(wifiDisableTask, disableWaitSeconds * 1000);

        Log.d("smarter", "Starting countdown of " + disableWaitSeconds + " to shut down wifi");
    }

    private Runnable wifiEnableTask = new Runnable() {
        public void run() {
            if (shutdown) return;

            if (!proctorWifi) return;

            Log.d("smarter", "enabling wifi");
            wifiManager.setWifiEnabled(true);
        }
    };

    private Runnable wifiDisableTask = new Runnable() {
        public void run() {
            if (shutdown) return;

            // If we're not proctoring wifi...
            if (!proctorWifi) return;

            if (getWifiState() == WifiState.WIFI_ON) {
                Log.d("smarter", "We were going to shut down wifi, but it's connected now");
                return;
            }

            Log.d("smarter", "Shutting down wi-fi, we haven't gotten a link");
            wifiManager.setWifiEnabled(false);
        }
    };

    // Set current tower or fetch current tower
    private void handleCellLocation(CellLocation location) {
        if (location == null) {
            location = telephonyManager.getCellLocation();
        }

        setCurrentTower(new CellLocationCommon(location));
    }

    private class SmarterPhoneListener extends PhoneStateListener {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            handleCellLocation(location);
        }
    }

    // Set the current tower and figure out what our tower state is
    private void setCurrentTower(CellLocationCommon curloc) {
        if (curloc == null) {
            curloc = new CellLocationCommon(telephonyManager.getCellLocation());
        }

        currentCellLocation = curloc;

        currentTowerType = TowerType.TOWER_UNKNOWN;

        if (curloc.getTowerId() > 0) {
            // If we know this tower already, set type to enable
            if (dbSource.queryTowerMapped(curloc.getTowerId())) {
                Log.d("smarter", "Found known tower");
                currentTowerType = TowerType.TOWER_ENABLE;
            }

            // If we're associated to a wifi, map the tower
            // TODO add blacklisting of towers, etc, here
            if (getWifiState() == WifiState.WIFI_ON) {
                Log.d("smarter", "New tower, Wi-Fi enabled, learning tower");
                dbSource.mapTower(getCurrentSsid(), curloc.getTowerId());
                currentTowerType = TowerType.TOWER_ENABLE;
            }

            triggerCallbackTowerChanged();
            configureWifiState();

            return;
        }

        triggerCallbackTowerChanged();
    }

    public void addCallback(SmarterServiceCallback cb) {
        synchronized (callbackList) {
            callbackList.add(cb);
        }

        // Call our CBs immediately for setup
        cb.towerStateChanged(currentCellLocation.getTowerId(), currentTowerType);
        cb.wifiStateChanged(getCurrentSsid(), getWifiState());

    }

    public void removeCallback(SmarterServiceCallback cb) {
        synchronized (callbackList) {
            callbackList.remove(cb);
        }
    }

    public void triggerCallbackTowerChanged() {
       synchronized (callbackList) {
           for (SmarterServiceCallback cb : callbackList) {
               cb.towerStateChanged(currentCellLocation.getTowerId(), currentTowerType);
           }
       }
    }

    public void triggerCallbackWifiChanged() {
        synchronized (callbackList) {
            for (SmarterServiceCallback cb: callbackList) {
                cb.wifiStateChanged(getCurrentSsid(), getWifiState());
            }
        }
    }

    public void configureWifiState() {
        WifiState curstate = getWifiState();
        WifiState targetstate = getShouldWifiBeEnabled();

        if (curstate == WifiState.WIFI_ON || curstate == WifiState.WIFI_IDLE) {
            if (targetstate == WifiState.WIFI_BLOCKED) {
                Log.d("smarter", "Target state: Blocked, shutting down wifi now, " + controlTypeToText(lastControlReason));
                timerHandler.removeCallbacks(wifiEnableTask);
                wifiManager.setWifiEnabled(false);
            } else if (targetstate == WifiState.WIFI_OFF) {
                Log.d("smarter", "Target state: Off, scheduling shutdown, " + controlTypeToText(lastControlReason));
                timerHandler.removeCallbacks(wifiEnableTask);
                startWifiShutdown();
            }
        } else {
            if (targetstate == WifiState.WIFI_ON) {
                Log.d("smarter", "Target state: On, scheduling bringup, " + controlTypeToText(lastControlReason));
                timerHandler.removeCallbacks(wifiDisableTask);
                startWifiEnable();
            }
        }

        triggerCallbackWifiChanged();
    }

    // Based on everything we know, should wifi be enabled?
    // WIFI_ON - Turn it on
    // WIFI_OFF - Start a shutdown timer
    // WIFI_BLOCKED - Kill it immediately
    // WIFI_IDLE - Do nothing
    public WifiState getShouldWifiBeEnabled() {
        WifiState curstate = getWifiState();

        // User requests override everything
        if (userOverrideState == WifiState.WIFI_OFF) {
            lastControlReason = ControlType.CONTROL_USER;
            return WifiState.WIFI_BLOCKED;
        }

        if (userOverrideState == WifiState.WIFI_ON) {
            lastControlReason = ControlType.CONTROL_USER;
            return WifiState.WIFI_ON;
        }

        if (currentTowerType == TowerType.TOWER_BLOCK) {
            lastControlReason = ControlType.CONTROL_TOWERID;
            return WifiState.WIFI_BLOCKED;
        }

        if (currentTowerType == TowerType.TOWER_ENABLE) {
            lastControlReason = ControlType.CONTROL_RANGE;
            return WifiState.WIFI_ON;
        }

        if (currentTowerType == TowerType.TOWER_UNKNOWN &&
                curstate == WifiState.WIFI_ON) {
            lastControlReason = ControlType.CONTROL_RANGE;
            return WifiState.WIFI_ON;
        }

        if (currentTowerType == TowerType.TOWER_UNKNOWN &&
                curstate == WifiState.WIFI_IDLE) {
            lastControlReason = ControlType.CONTROL_RANGE;
            return WifiState.WIFI_OFF;
        }

        lastControlReason = ControlType.CONTROL_RANGE;
        return WifiState.WIFI_OFF;
    }

    public WifiState getWifiState() {
        int rawstate = wifiManager.getWifiState();

        boolean rawwifienabled = false;

        if (rawstate == WifiManager.WIFI_STATE_ENABLED || rawstate == WifiManager.WIFI_STATE_ENABLING)
            rawwifienabled = true;

        NetworkInfo rawni = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        boolean rawnetenabled = false;

        if (rawni != null && rawni.isConnected())
            rawnetenabled = true;

        if (rawwifienabled && rawnetenabled) {
            currentSsid = wifiManager.getConnectionInfo().getSSID();
            return WifiState.WIFI_ON;
        }

        if (rawwifienabled && !rawnetenabled) {
            currentSsid = null;
            return WifiState.WIFI_IDLE;
        }

        return WifiState.WIFI_OFF;
    }

    public String getCurrentSsid() {
        if (currentSsid == null && getWifiState() == WifiState.WIFI_ON)
            currentSsid = wifiManager.getConnectionInfo().getSSID();

        return currentSsid;
    }

    public Long getCurrentTower() {
        return currentCellLocation.getTowerId();
    }

    public boolean getAirplaneMode() {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    static public String controlStateToText(ControlState s) {
        switch (s) {
            case CONTROL_BLOCK:
                return "Block";
            case CONTROL_ENABLE:
                return "Enable";
            case CONTROL_IGNORE:
                return "Ignore";
            case CONTROL_DISABLE:
                return "Disable";
        }

        return "Unknown";
    }

    static public String controlTypeToText(ControlType t) {
        switch (t) {
            case CONTROL_BLUETOOTH:
                return "Bluetooth";
            case CONTROL_GEOFENCE:
                return "Geofence";
            case CONTROL_RANGE:
                return "Tower range";
            case CONTROL_TIME:
                return "Time range";
            case CONTROL_TOWERID:
                return "Tower ID";
            case CONTROL_USER:
                return "User";
        }

        return "Unknown";
    }

}