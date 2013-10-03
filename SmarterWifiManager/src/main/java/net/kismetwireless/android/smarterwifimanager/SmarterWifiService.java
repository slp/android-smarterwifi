package net.kismetwireless.android.smarterwifimanager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SmarterWifiService extends Service {
    // Unique combinations:
    // WIFISTATE IGNORE + CONTROL RANGE - no valid towers no other conditions

    public enum ControlType {
        CONTROL_DISABLED, CONTROL_USER, CONTROL_RANGE, CONTROL_TOWERID, CONTROL_GEOFENCE, CONTROL_BLUETOOTH, CONTROL_TIME,
        CONTROL_SSIDBLACKLIST, CONTROL_AIRPLANE, CONTROL_TETHER
    }

    public enum WifiState {
        // Hard blocked, on, off, idle, ignore
        WIFI_BLOCKED, WIFI_ON, WIFI_OFF, WIFI_IDLE, WIFI_IGNORE
    }

    public enum BluetoothState {
        BLUETOOTH_BLOCKED, BLUETOOTH_ON, BLUETOOTH_OFF, BLUETOOTH_IDLE, BLUETOOTH_IGNORE
    }

    public enum TowerType {
        TOWER_UNKNOWN, TOWER_BLOCK, TOWER_ENABLE, TOWER_INVALID
    }

    private boolean shutdown = false;

    SharedPreferences preferences;

    TelephonyManager telephonyManager;
    SmarterPhoneListener phoneListener;
    WifiManager wifiManager;
    BluetoothAdapter btAdapter;
    ConnectivityManager connectivityManager;
    private NotificationManager notificationManager;

    private boolean proctorWifi = true;
    private boolean learnWifi = true;

    private int enableWaitSeconds = 1;
    private int disableWaitSeconds = 30;
    private boolean showNotification = true;

    private WifiState userOverrideState = WifiState.WIFI_IGNORE;

    private CellLocationCommon currentCellLocation;
    private TowerType currentTowerType = TowerType.TOWER_UNKNOWN;

    private ControlType lastControlReason = ControlType.CONTROL_TOWERID;

    private Handler timerHandler = new Handler();

    private SmarterDBSource dbSource;

    private NotificationCompat.Builder notificationBuilder;

    private long lastTowerMap = 0;

    private boolean bluetoothEnabled = false;
    private boolean bluetoothBlocking = false;
    private HashMap<String, SmarterBluetooth> bluetoothBlockingDevices = new HashMap<String, SmarterBluetooth>();
    private HashMap<String, SmarterBluetooth> bluetoothConnectedDevices = new HashMap<String, SmarterBluetooth>();

    public static abstract class SmarterServiceCallback {
        protected ControlType controlType;
        protected WifiState wifiState, controlState;
        protected SmarterSSID lastSsid;
        protected TowerType towerType;
        protected BluetoothState lastBtState;

        public void wifiStateChanged(final SmarterSSID ssid, final WifiState state,
                                     final WifiState controlstate, final ControlType type) {
            lastSsid = ssid;
            wifiState = state;
            controlType = type;
            controlState = controlstate;

            return;
        }

        public void towerStateChanged(final long towerid, final TowerType type) {
            towerType = type;

            return;
        }

        public void bluetoothStateChanged(final BluetoothState state) {
            lastBtState = state;

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

        context = this.getApplicationContext();

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        try {
            dbSource = new SmarterDBSource(context);
        } catch (SQLiteException e) {
            Log.d("smarter", "failed to open database: " + e);
        }

        // Default network state
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        phoneListener = new SmarterPhoneListener();

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationBuilder = new NotificationCompat.Builder(context);

        // Make the notification
        notificationBuilder.setSmallIcon(R.drawable.custom_wifi_enabled);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setOngoing(true);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        notificationBuilder.setContentIntent(pIntent);

        updatePreferences();

        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CELL_LOCATION);

        // Kick an update
        configureWifiState();

        if (showNotification)
            notificationManager.notify(0, notificationBuilder.build());


        addCallback(new SmarterServiceCallback() {
            WifiState lastState = WifiState.WIFI_IDLE;
            ControlType lastControl = ControlType.CONTROL_DISABLED;

            @Override
            public void wifiStateChanged(SmarterSSID ssid, WifiState state, WifiState controlstate, ControlType type) {
                super.wifiStateChanged(ssid, state, controlstate, type);

                if (state == lastState && type == lastControl)
                    return;

                lastState = state;
                lastControl = type;

                int wifiIconId = R.drawable.custom_wifi_inactive;
                String wifiText = "";
                String reasonText = "";

                switch (state) {
                    case WIFI_IDLE:
                        wifiIconId = R.drawable.custom_wifi_inactive;
                        wifiText = "Wi-Fi idle / disconnected";
                        break;
                    case WIFI_BLOCKED:
                        wifiIconId = R.drawable.custom_wifi_disabled_tower;
                        wifiText = "Wi-Fi ";
                        break;
                    case WIFI_ON:
                        wifiIconId = R.drawable.custom_wifi_enabled;
                        wifiText = "Wi-Fi enabled";
                        break;
                    case WIFI_OFF:
                        wifiIconId = R.drawable.custom_wifi_inactive;
                        wifiText = "Wi-Fi turned off";

                        if (type == ControlType.CONTROL_RANGE) {
                            reasonText = "Not in a known location";
                        } else if (lastControlReason == ControlType.CONTROL_BLUETOOTH) {
                            wifiIconId = R.drawable.custom_wifi_disabled_bluetooth;
                        }

                        break;
                    case WIFI_IGNORE:
                        wifiIconId = R.drawable.custom_wifi_enabled;
                        wifiText = "Wi-Fi management disabled";

                        if (lastControlReason == ControlType.CONTROL_RANGE) {
                            reasonText = "No cell signal";
                        }

                        break;

                    default:
                        wifiIconId = R.drawable.custom_wifi_inactive;
                }

                if (reasonText.isEmpty())
                    reasonText = SmarterWifiService.controlTypeToText(type);

                notificationBuilder.setSmallIcon(wifiIconId);
                notificationBuilder.setContentTitle(wifiText);
                notificationBuilder.setContentText(reasonText);

                if (showNotification)
                    notificationManager.notify(0, notificationBuilder.build());
            }
        });
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
        learnWifi = preferences.getBoolean(getString(R.string.pref_learn), true);
        proctorWifi = preferences.getBoolean(getString(R.string.pref_enable), true);

        disableWaitSeconds = Integer.parseInt(preferences.getString(getString(R.string.prefs_item_shutdowntime), "30"));

        if (disableWaitSeconds < 30)
            disableWaitSeconds = 30;

        showNotification = preferences.getBoolean(getString(R.string.prefs_item_notification), true);

        if (!showNotification)
            notificationManager.cancel(0);
        else
            notificationManager.notify(0, notificationBuilder.build());

        configureWifiState();
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
        configureWifiState();
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

        if (curloc.getTowerId() < 0)
            currentTowerType = TowerType.TOWER_INVALID;
        else
            currentTowerType = TowerType.TOWER_UNKNOWN;

        if (curloc.getTowerId() > 0 && learnWifi) {
            SmarterSSID ssid = getCurrentSsid();

            // If we know this tower already, set type to enable
            if (dbSource.queryTowerMapped(curloc.getTowerId())) {
                Log.d("smarter", "Found known tower");
                currentTowerType = TowerType.TOWER_ENABLE;
            }

            // If we're associated to a wifi, map the tower.
            // Don't map towers while we're tethered.
            if (getWifiState() == WifiState.WIFI_ON && !getWifiTethered() && currentTowerType != TowerType.TOWER_ENABLE) {
                if (ssid != null && ssid.isBlacklisted()) {
                    // We don't learn anything based on this ssid
                } else {
                    Log.d("smarter", "New tower, Wi-Fi enabled, learning tower");
                    dbSource.mapTower(getCurrentSsid(), curloc.getTowerId());
                    lastTowerMap = System.currentTimeMillis();
                    currentTowerType = TowerType.TOWER_ENABLE;
                }
            }

            triggerCallbackTowerChanged();
            // configureWifiState();

            return;
        }

        triggerCallbackTowerChanged();
    }

    public void addCallback(SmarterServiceCallback cb) {
        try {
            if (cb == null) {
                Log.e("smarter", "Got a null callback?");
                return;
            }

            synchronized (callbackList) {
                callbackList.add(cb);
            }

            // Call our CBs immediately for setup
            cb.towerStateChanged(currentCellLocation.getTowerId(), currentTowerType);
            cb.wifiStateChanged(getCurrentSsid(), getWifiState(), getShouldWifiBeEnabled(), lastControlReason);
            cb.bluetoothStateChanged(getBluetoothState());
        } catch (NullPointerException npe) {
            Log.e("smarter", "Got NPE in addcallback, caught, but not sure what happened");
        }

    }

    public void removeCallback(SmarterServiceCallback cb) {
        if (cb == null) {
            Log.e("smarter", "Got a null callback?");
            return;
        }

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
                cb.wifiStateChanged(getCurrentSsid(), getWifiState(), getShouldWifiBeEnabled(), lastControlReason);
            }
        }
    }

    public void triggerCallbackBluetoothChanged() {
        synchronized (callbackList) {
            for (SmarterServiceCallback cb: callbackList) {
                cb.bluetoothStateChanged(getBluetoothState());
            }
        }
    }

    public void configureWifiState() {
        WifiState curstate = getWifiState();
        WifiState targetstate = getShouldWifiBeEnabled();

        // If we're on, we probably just turned on - probe the tower so we start learning
        if (curstate == WifiState.WIFI_ON && targetstate == WifiState.WIFI_ON) {
            // Directly call setcurrenttower to get the current tower; handleCellLocation would loop us
            setCurrentTower(null);
        }

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

    public void configureBluetoothState() {
        int btstate = btAdapter.getState();

        if (btstate == BluetoothAdapter.STATE_OFF) {
            bluetoothBlocking = false;
            bluetoothEnabled = false;
            bluetoothBlockingDevices.clear();
            bluetoothConnectedDevices.clear();
        } else if (btstate == BluetoothAdapter.STATE_ON) {
            bluetoothEnabled = true;
        }

        triggerCallbackBluetoothChanged();

        // We can't get a list of connected devices, only watch
    }

    public void handleBluetoothDeviceState(BluetoothDevice d, int state) {
        if (state == BluetoothAdapter.STATE_CONNECTED) {
            SmarterBluetooth sbd = dbSource.getBluetoothBlacklisted(d);

            bluetoothConnectedDevices.put(d.getAddress(), sbd);

            if (sbd.isBlacklisted()) {
                Log.d("smarter", "blocking bt on device " + d.getAddress() + " " + d.getName());
                bluetoothBlockingDevices.put(d.getAddress(), sbd);

                if (!bluetoothBlocking) {
                    bluetoothBlocking = true;
                    configureWifiState();
                }
            }
        } else {
            bluetoothConnectedDevices.remove(d.getAddress());
            bluetoothBlockingDevices.remove(d.getAddress());

            if (bluetoothBlockingDevices.size() <= 0) {
                bluetoothBlocking = false;
                configureWifiState();
            }
        }
    }

    // Based on everything we know, should wifi be enabled?
    // WIFI_ON - Turn it on
    // WIFI_OFF - Start a shutdown timer
    // WIFI_BLOCKED - Kill it immediately
    // WIFI_IDLE - Do nothing
    public WifiState getShouldWifiBeEnabled() {
        WifiState curstate = getWifiState();
        SmarterSSID ssid = getCurrentSsid();
        boolean tethered = getWifiTethered();

        // We're not looking at all
        if (proctorWifi == false) {
            lastControlReason = ControlType.CONTROL_DISABLED;
            return WifiState.WIFI_IGNORE;
        }

        // Tethering overrides almost everything
        if (tethered) {
            lastControlReason = ControlType.CONTROL_TETHER;
            return WifiState.WIFI_IGNORE;
        }

        // Airplane mode causes us to ignore the wifi entirely, do whatever the user sets it as
        if (getAirplaneMode()) {
            lastControlReason = ControlType.CONTROL_AIRPLANE;
            return WifiState.WIFI_IGNORE;
        }

        // If the user wants spefically to turn it on or off via the SWM UI, do so
        if (userOverrideState == WifiState.WIFI_OFF) {
            lastControlReason = ControlType.CONTROL_USER;
            return WifiState.WIFI_BLOCKED;
        }

        if (userOverrideState == WifiState.WIFI_ON) {
            lastControlReason = ControlType.CONTROL_USER;
            return WifiState.WIFI_ON;
        }

        // Bluetooth blocks learning
        if (bluetoothBlocking) {
            lastControlReason = ControlType.CONTROL_BLUETOOTH;
            return WifiState.WIFI_BLOCKED;
        }

        if (curstate == WifiState.WIFI_ON && (ssid != null && ssid.isBlacklisted())) {
            lastControlReason = ControlType.CONTROL_SSIDBLACKLIST;
            return WifiState.WIFI_IGNORE;
        }

        if (learnWifi) {
            if (currentTowerType == TowerType.TOWER_INVALID) {
                lastControlReason = ControlType.CONTROL_RANGE;
                return WifiState.WIFI_IGNORE;
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

        return WifiState.WIFI_IGNORE;
    }

    public BluetoothState getBluetoothState() {
        if (btAdapter == null)
            return BluetoothState.BLUETOOTH_OFF;

        int s =  btAdapter.getState();

        if (s == BluetoothAdapter.STATE_ON)
            return BluetoothState.BLUETOOTH_ON;

        return BluetoothState.BLUETOOTH_OFF;
    }

    public WifiState getWifiState() {
        if (!proctorWifi) {
            lastControlReason = ControlType.CONTROL_DISABLED;
            return WifiState.WIFI_IGNORE;
        }

        int rawstate = wifiManager.getWifiState();

        boolean rawwifienabled = false;

        if (rawstate == WifiManager.WIFI_STATE_ENABLED || rawstate == WifiManager.WIFI_STATE_ENABLING)
            rawwifienabled = true;

        NetworkInfo rawni = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        boolean rawnetenabled = false;

        if (rawni != null && rawni.isConnected())
            rawnetenabled = true;

        if (rawwifienabled && rawnetenabled) {
            return WifiState.WIFI_ON;
        }

        if (rawwifienabled && !rawnetenabled) {
            return WifiState.WIFI_IDLE;
        }

        return WifiState.WIFI_OFF;
    }

    public SmarterSSID getCurrentSsid() {
        SmarterSSID curssid = null;

        if (getWifiState() == WifiState.WIFI_ON)
            curssid = dbSource.getSsidBlacklisted(wifiManager.getConnectionInfo().getSSID());

        return curssid;
    }

    public Long getCurrentTower() {
        return currentCellLocation.getTowerId();
    }

    public boolean getAirplaneMode() {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    public boolean getWifiStateEnabled(WifiState state) {
        if (state == WifiState.WIFI_ON || state == WifiState.WIFI_IDLE)
            return true;

        return false;
    }

    static public String controlTypeToText(ControlType t) {
        switch (t) {
            case CONTROL_DISABLED:
                return "Wi-Fi management disabled";
            case CONTROL_BLUETOOTH:
                return "Bluetooth";
            case CONTROL_GEOFENCE:
                return "Geofence";
            case CONTROL_RANGE:
                return "Auto-learned location";
            case CONTROL_TIME:
                return "Time range";
            case CONTROL_TOWERID:
                return "Tower ID";
            case CONTROL_USER:
                return "User override";
            case CONTROL_SSIDBLACKLIST:
                return "SSID blacklisted";
            case CONTROL_AIRPLANE:
                return "Airplane mode";
            case CONTROL_TETHER:
                return "Tethering";
        }

        return "Unknown";
    }

    public ArrayList<SmarterBluetooth> getBluetoothBlacklist() {
        ArrayList<SmarterBluetooth> btlist = new ArrayList<SmarterBluetooth>();

        if (btAdapter == null)
            return btlist;

        Set<BluetoothDevice> btset = btAdapter.getBondedDevices();

        for (BluetoothDevice d : btset) {
            SmarterBluetooth sbt = dbSource.getBluetoothBlacklisted(d);

            btlist.add(sbt);
        }

        return btlist;
    }

    public void setBluetoothBlacklist(SmarterBluetooth device, boolean blacklist, boolean enable) {
        dbSource.setBluetoothBlacklisted(device, blacklist, enable);

        if (blacklist) {
            if (!bluetoothBlockingDevices.containsKey(device.getBtmac())) {
                if (bluetoothConnectedDevices.containsKey(device.getBtmac())) {
                    bluetoothBlockingDevices.put(device.getBtmac(), device);
                    bluetoothBlocking = true;

                    Log.d("smarter", "after adding " + device.getBtName() + " blocking bluetooth");

                    configureWifiState();
                }
            }
        } else {
            if (bluetoothBlockingDevices.containsKey(device.getBtmac())) {
                bluetoothBlockingDevices.remove(device.getBtmac());

                if (bluetoothBlockingDevices.size() <= 0) {
                    Log.d("smarter", "after removing " + device.getBtName() + " nothing blocking in bluetooth");
                    bluetoothBlocking = false;
                    configureWifiState();
                }
            }
        }

    }

    public ArrayList<SmarterSSID> getSsidBlacklist() {
        ArrayList<SmarterSSID> blist = new ArrayList<SmarterSSID>();
        List<WifiConfiguration> wic = wifiManager.getConfiguredNetworks();

        if (wic == null)
            return blist;

        for (WifiConfiguration w : wic) {
            blist.add(dbSource.getSsidBlacklisted(w.SSID));
        }

        return blist;
    }

    public void setSsidBlacklist(SmarterSSID ssid, boolean blacklisted) {
        Log.d("smarter", "service backend setting ssid " + ssid.getSsid() + " blacklist " + blacklisted);
        dbSource.setSsidBlacklisted(ssid, blacklisted);
        handleCellLocation(null);
        configureWifiState();
    }

    public ArrayList<SmarterSSID> getSsidTowerlist() {
        return dbSource.getMappedSSIDList();
    }

    public void deleteSsidTowerMap(SmarterSSID ssid) {
        dbSource.deleteSsidTowerMap(ssid);

        handleCellLocation(null);
    }

    public long getLastTowerMap() {
        return lastTowerMap;
    }

    public boolean getWifiTethered() {
        boolean ret = false;
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method: wmMethods){
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    ret = (Boolean) method.invoke(wifiManager);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        }

        // Log.d("smarter", "tethering: " + ret);
        return ret;
    }

    public ArrayList<SmarterTimeRange> getTimeRangeList() {
        return dbSource.getTimeRangeList();
    }

    public void deleteTimeRange(SmarterTimeRange r) {
        dbSource.deleteTimeRange(r);
    }

    public long updateTimeRange(SmarterTimeRange r) {
        return dbSource.updateTimeRange(r);
    }

}