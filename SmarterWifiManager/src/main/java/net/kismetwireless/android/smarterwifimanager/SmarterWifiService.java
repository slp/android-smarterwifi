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
        CONTROL_DISABLED, CONTROL_USER, CONTROL_TOWER, CONTROL_TOWERID, CONTROL_GEOFENCE,
        CONTROL_BLUETOOTH, CONTROL_TIME, CONTROL_SSIDBLACKLIST, CONTROL_AIRPLANE, CONTROL_TETHER,
        CONTROL_SLEEPPOLICY
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
    private boolean performTowerPurges = false;
    private int purgeTowerHours = 48;

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
    private boolean initialBluetoothState = false;
    private HashMap<String, SmarterBluetooth> bluetoothBlockingDevices = new HashMap<String, SmarterBluetooth>();
    private HashMap<String, SmarterBluetooth> bluetoothConnectedDevices = new HashMap<String, SmarterBluetooth>();

    private SmarterTimeRange currentTimeRange, nextTimeRange;

    private AlarmReceiver alarmReceiver;

    private boolean pendingWifiShutdown = false, pendingBluetoothShutdown = false;

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

        public void preferencesChanged() {
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

        alarmReceiver = new AlarmReceiver();

        alarmReceiver.setAlarm(context, System.currentTimeMillis() + (20 * 1000));

        // Make the notification
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_notification_idle);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        notificationBuilder.setOnlyAlertOnce(true);
        notificationBuilder.setOngoing(true);

        //Intent intent = new Intent(this, MainActivity.class);
        Intent intent = new Intent(this, ActivityQuickconfig.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        notificationBuilder.setContentIntent(pIntent);

        // Get the initial BT enable state
        initialBluetoothState = getBluetoothState() != BluetoothState.BLUETOOTH_OFF;

        updatePreferences();

        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CELL_LOCATION);

        // Kick an update
        // configureWifiState();

        // Update the time range database which also fires BT and Wifi configurations
        configureTimerangeState();

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

                int wifiIconId = R.drawable.ic_launcher_notification_ignore;
                int wifiTextResource = -1;
                int reasonTextResource = -1;

                wifiTextResource = wifiStateToTextResource(state);
                reasonTextResource = controlTypeToTextResource(type, state);

                if (state == WifiState.WIFI_IDLE) {
                    wifiIconId = R.drawable.ic_launcher_notification_idle;
                } else if (state == WifiState.WIFI_BLOCKED) {
                    wifiIconId = R.drawable.ic_launcher_notification_disabled;
                } else if (state == WifiState.WIFI_IGNORE) {
                    wifiIconId = R.drawable.ic_launcher_notification_idle;
                } else if (state == WifiState.WIFI_OFF) {
                    if (type == ControlType.CONTROL_BLUETOOTH)
                        wifiIconId = R.drawable.ic_launcher_notification_bluetooth;
                    else if (type == ControlType.CONTROL_TIME)
                        wifiIconId = R.drawable.ic_launcher_notification_clock;
                    else if (type == ControlType.CONTROL_TOWER)
                        wifiIconId = R.drawable.ic_launcher_notification_cell;
                    else
                        wifiIconId = R.drawable.ic_launcher_notification_disabled;
                } else if (state == WifiState.WIFI_ON) {
                    if (type == ControlType.CONTROL_BLUETOOTH)
                        wifiIconId = R.drawable.ic_launcher_notification_bluetooth;
                    else if (type == ControlType.CONTROL_TIME)
                        wifiIconId = R.drawable.ic_launcher_notification_clock;
                    else if (type == ControlType.CONTROL_TOWER)
                        wifiIconId = R.drawable.ic_launcher_notification_cell;
                    else
                        wifiIconId = R.drawable.ic_launcher_notification_ignore;
                }

                notificationBuilder.setSmallIcon(wifiIconId);

                if (wifiTextResource > 0) {
                    notificationBuilder.setContentTitle(getString(wifiTextResource));
                } else {
                    notificationBuilder.setContentTitle("");
                }

                if (reasonTextResource > 0) {
                    notificationBuilder.setContentText(getString(reasonTextResource));
                } else {
                    notificationBuilder.setContentText("");
                }

                // notificationBuilder.setContentTitle(wifiText);
                // notificationBuilder.setContentText(reasonText);

                if (showNotification)
                    notificationManager.notify(0, notificationBuilder.build());
            }
        });

        towerCleanupTask.run();
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

    public void updateTimeRanges() {
        ArrayList<SmarterTimeRange> ranges = getTimeRangeList();

        currentTimeRange = null;
        nextTimeRange = null;

        if (ranges == null) {
            return;
        }

        // Are we in a time range right now?  If so, figure out which one has the
        // shortest duration that we're part of.  Once we fall out of this time range
        // we'll redo this calculation, which will grab the outer time range if one
        // exists.
        for (SmarterTimeRange t : ranges) {
            if (!t.getEnabled())
                continue;

            if (!t.isInRangeNow())
                continue;

            if (currentTimeRange == null) {
                currentTimeRange = new SmarterTimeRange(t);
                continue;
            }

            if (t.getDurationMinutes() < currentTimeRange.getDurationMinutes()) {
                currentTimeRange = t;
            }
        }

        long now = System.currentTimeMillis();
        long timeUtilStart = 0;

        // Figure out the next time range we're going to be in
        for (SmarterTimeRange t : ranges) {
            if (!t.getEnabled())
                continue;

            long nextT = t.getNextStartMillis();

            // Shouldn't be possible since next will always find based on now, but can't hurt
            if (nextT < now)
                continue;

            if (timeUtilStart == 0 || nextT - now < timeUtilStart) {
                nextTimeRange = t;
            }
        }

        if (currentTimeRange == null && nextTimeRange == null) {
            Log.d("smarter", "Not in any time ranges");
            return;
        }

        if (currentTimeRange != null) {
            Log.d("smarter", "currently in a time range");
            // Is the next alarm for the end of this time range, or for an overlapping range?
            if (nextTimeRange == null ||
                    (nextTimeRange != null && currentTimeRange.getNextEndMillis() < nextTimeRange.getNextStartMillis())) {
                Log.d("smarter", "next alarm for end of this time range");
                alarmReceiver.setAlarm(this, currentTimeRange.getNextEndMillis());
            } else {
                Log.d("smarter", "next alarm for start of overlapping time range");
                alarmReceiver.setAlarm(this, nextTimeRange.getNextStartMillis());
            }

            if (currentTimeRange.getBluetoothControlled() && btAdapter != null) {
                if (currentTimeRange.getBluetoothEnabled()) {
                    btAdapter.enable();
                } else {
                    btAdapter.disable();
                }
            }
        }
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

        performTowerPurges = preferences.getBoolean(getString(R.string.prefs_item_towermaintenance), false);

        configureWifiState();

        triggerCallbackPrefsChanged();
    }

    public void shutdownService() {
        shutdown = true;
        timerHandler.removeCallbacks(towerCleanupTask);
        timerHandler.removeCallbacks(wifiDisableTask);
        telephonyManager.listen(phoneListener, 0);
    }

    private void startBluetoothEnable() {
        if (btAdapter != null) {
            Log.d("smarter", "Turning on bluetooth");
            btAdapter.enable();
        }
    }

    private void startBluetoothShutdown() {
        if (btAdapter != null) {
            Log.d("smarter", "Turning off bluetooth");
            btAdapter.disable();
        }
    }

    private void startWifiEnable() {
        pendingWifiShutdown = false;
        timerHandler.removeCallbacks(wifiEnableTask);
        timerHandler.postDelayed(wifiEnableTask, enableWaitSeconds * 1000);

        Log.d("smarter", "Starting countdown of " + enableWaitSeconds + " to enable wifi");

    }

    private void startWifiShutdown() {
        if (pendingWifiShutdown) {
            Log.d("smarter", "wifi countown in progress, not shutting down");
            return;
        }

        pendingWifiShutdown = true;
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
            pendingWifiShutdown = false;

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
                // map it and update the last seen time
                dbSource.mapTower(getCurrentSsid(), curloc.getTowerId());
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

    public void triggerCallbackPrefsChanged() {
        synchronized (callbackList) {
            for (SmarterServiceCallback cb : callbackList) {
                cb.preferencesChanged();
            }
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
                startWifiEnable();
            }
        }

        triggerCallbackWifiChanged();
    }

    public void configureBluetoothState() {
        int btstate = btAdapter.getState();
        BluetoothState targetstate = getShouldBluetoothBeEnabled();

        // Learn time range if null
        if (currentTimeRange == null) {
            initialBluetoothState = (btstate != BluetoothAdapter.STATE_OFF);
            Log.d("smarter", "learned default bt state: " + initialBluetoothState);
        }

        if (btstate == BluetoothAdapter.STATE_OFF) {
            bluetoothBlocking = false;
            bluetoothEnabled = false;
            bluetoothBlockingDevices.clear();
            bluetoothConnectedDevices.clear();

            if (targetstate == BluetoothState.BLUETOOTH_ON) {
                startBluetoothEnable();
            }
        } else if (btstate == BluetoothAdapter.STATE_ON) {
            bluetoothEnabled = true;

            if (targetstate == BluetoothState.BLUETOOTH_BLOCKED ||
                    targetstate == BluetoothState.BLUETOOTH_OFF) {
                startBluetoothShutdown();
            }
        }

        triggerCallbackBluetoothChanged();

        // We can't get a list of connected devices, only watch
    }

    public void configureTimerangeState() {
        boolean wasInRange = false;

        // Are we in a state when we started?  If not, update our bluetooth state
        if (currentTimeRange == null) {
            initialBluetoothState = getBluetoothState() != BluetoothState.BLUETOOTH_OFF;
            Log.d("smarter", "learned default bt state: " + initialBluetoothState);
        } else {
            wasInRange = true;
        }

        // Figure out if we should be in one and set future alarms
        updateTimeRanges();

        // We've transitioned out of a time range
        if (currentTimeRange != null && btAdapter != null) {
            Log.d("smarter", "transitioning out of time range, restoring bluetooth to previous state of: " + initialBluetoothState);
            if (initialBluetoothState) {
                btAdapter.enable();
            } else {
                btAdapter.disable();
            }
        }

        // Configure wifi and bluetooth
        configureWifiState();
        configureBluetoothState();
    }

    public void handleWifiP2PState(int state) {
        Log.d("smarter", "wifi p2p state changed: " + state);
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

    // Based on everything we know, should bluetooth be enabled?
    public BluetoothState getShouldBluetoothBeEnabled() {
        // We're not looking at all
        if (proctorWifi == false) {
            lastControlReason = ControlType.CONTROL_DISABLED;
            return BluetoothState.BLUETOOTH_IGNORE;
        }

        // Are we in a time range?
        if (currentTimeRange != null) {
            if (currentTimeRange.getBluetoothControlled()) {
                // Does this time range control bluetooth?
                if (currentTimeRange.getBluetoothEnabled())
                    return BluetoothState.BLUETOOTH_ON;
                else
                    return BluetoothState.BLUETOOTH_BLOCKED;
            } else {
                // Otherwise ignore it
                return BluetoothState.BLUETOOTH_IGNORE;
            }
        }

        /* Otherwise ignore */
        return BluetoothState.BLUETOOTH_IGNORE;

        /*
        // Otherwise return us to the state we were before we started this
        return initialBluetoothState ? BluetoothState.BLUETOOTH_ON : BluetoothState.BLUETOOTH_BLOCKED;
        */
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
            Log.d("smarter", "Tethering detected, ignoring wifi state");
            lastControlReason = ControlType.CONTROL_TETHER;
            return WifiState.WIFI_IGNORE;
        }

        // Airplane mode causes us to ignore the wifi entirely, do whatever the user sets it as
        if (getAirplaneMode()) {
            Log.d("smarter", "Airplane mode detected, ignoring wifi state");
            lastControlReason = ControlType.CONTROL_AIRPLANE;
            return WifiState.WIFI_IGNORE;
        }

        // If the user wants spefically to turn it on or off via the SWM UI, do so
        if (userOverrideState == WifiState.WIFI_OFF) {
            Log.d("smarter", "User-controled wifi, user wants wifi off");
            lastControlReason = ControlType.CONTROL_USER;
            return WifiState.WIFI_BLOCKED;
        }

        if (userOverrideState == WifiState.WIFI_ON) {
            Log.d("smarter", "User-controlled wifi, user wants wifi on");
            lastControlReason = ControlType.CONTROL_USER;
            return WifiState.WIFI_ON;
        }

        // If we're in a time range...
        if (currentTimeRange != null) {
            // And we control wifi...
            if (currentTimeRange.getWifiControlled()) {
                // and we're supposed to shut it down
                if (!currentTimeRange.getWifiEnabled()) {
                    // Always aggressively block when in a time range
                    Log.d("smarter", "Time range, aggressively disabling wifi");

                    lastControlReason = ControlType.CONTROL_TIME;
                    return WifiState.WIFI_BLOCKED;

                    /*
                    // Harsh or gentle?
                    if (currentTimeRange.getAggressiveManagement())
                        return WifiState.WIFI_BLOCKED;
                    else
                        return WifiState.WIFI_OFF;
                        */
                } else {
                    // We want it on..
                    Log.d("smarter", "Time range, enabling wifi");

                    lastControlReason = ControlType.CONTROL_TIME;
                    return WifiState.WIFI_ON;
                }
            }

            // Otherwise we're not managing wifi in this time range so we keep going
        }

        // Bluetooth blocks learning
        if (bluetoothBlocking) {
            Log.d("smarter", "Connected to bluetooth device, blocking wifi");
            lastControlReason = ControlType.CONTROL_BLUETOOTH;
            return WifiState.WIFI_BLOCKED;
        }

        if (curstate == WifiState.WIFI_ON && (ssid != null && ssid.isBlacklisted())) {
            Log.d("smarter", "Connected to blacklisted SSID, ignoring wifi");
            lastControlReason = ControlType.CONTROL_SSIDBLACKLIST;
            return WifiState.WIFI_IGNORE;
        }

        if (currentTowerType == TowerType.TOWER_INVALID) {
            Log.d("smarter", "Connected to invalid tower, ignoring wifi state");
            lastControlReason = ControlType.CONTROL_TOWER;
            return WifiState.WIFI_IGNORE;
        }

        if (currentTowerType == TowerType.TOWER_BLOCK) {
            Log.d("smarter", "Connected to blocked tower, turning off wifi");
            lastControlReason = ControlType.CONTROL_TOWERID;
            return WifiState.WIFI_BLOCKED;
        }

        if (currentTowerType == TowerType.TOWER_ENABLE) {
            Log.d("smarter", "Connected to enable tower, turning on wifi");
            lastControlReason = ControlType.CONTROL_TOWER;
            return WifiState.WIFI_ON;
        }

        if (currentTowerType == TowerType.TOWER_UNKNOWN && curstate == WifiState.WIFI_ON) {
            Log.d("smarter", "Connected to unknown tower, wifi is enabled, keep wifi on");
            lastControlReason = ControlType.CONTROL_TOWER;
            return WifiState.WIFI_ON;
        }

        if (currentTowerType == TowerType.TOWER_UNKNOWN &&
                curstate == WifiState.WIFI_IDLE) {
            lastControlReason = ControlType.CONTROL_TOWER;
            return WifiState.WIFI_OFF;
        }

        lastControlReason = ControlType.CONTROL_TOWER;
        return WifiState.WIFI_OFF;
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

    public int getSleepPolicy() {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
    }

    public boolean getWifiStateEnabled(WifiState state) {
        if (state == WifiState.WIFI_ON || state == WifiState.WIFI_IDLE)
            return true;

        return false;
    }

    static public int wifiStateToTextResource(WifiState s) {
        switch (s) {
            case WIFI_BLOCKED:
                return R.string.wifistate_blocked;
            case WIFI_IDLE:
                return R.string.wifistate_idle;
            case WIFI_IGNORE:
                return R.string.wifistate_ignore;
            case WIFI_ON:
                return R.string.wifistate_on;
            case WIFI_OFF:
                return R.string.wifistate_off;
        }

        return R.string.wifistate_ignore;
    }

    static public int controlTypeToTextResource(ControlType t, WifiState s) {
        switch (t) {
            case CONTROL_DISABLED:
                return R.string.explanation_wifi_management_disabled;
            case CONTROL_BLUETOOTH:
                // BT always indicates off (for now)
                return R.string.explanation_wifi_disabled_bluetooth;
            case CONTROL_TIME:
                if (s == WifiState.WIFI_OFF)
                    return R.string.explanation_wifi_time_exclude;
                return R.string.explanation_wifi_time_include;
            case CONTROL_GEOFENCE:
                if (s == WifiState.WIFI_OFF)
                    return R.string.explanation_wifi_geofence_exclude;
                return R.string.explanation_wifi_geofence_include;
            case CONTROL_TOWER:
                if (s == WifiState.WIFI_OFF)
                    return R.string.explanation_wifi_disabled_cell;
                if (s == WifiState.WIFI_IDLE)
                    return R.string.explanation_wifi_idle_disable;
                return R.string.explanation_wifi_enabled_cell;
            case CONTROL_TOWERID:
                return R.string.explanation_wifi_disabled_towerid;
            case CONTROL_USER:
                if (s == WifiState.WIFI_OFF || s == WifiState.WIFI_BLOCKED)
                    return R.string.explanation_wifi_forced_user_disabled;
                return R.string.explanation_wifi_forced_user_enabled;
            case CONTROL_SSIDBLACKLIST:
                return R.string.explanation_wifi_ignore_ssidblacklist;
            case CONTROL_AIRPLANE:
                return R.string.explanation_wifi_ignore_airplane;
            case CONTROL_TETHER:
                return R.string.explanation_wifi_ignore_tethered;
        }

        return R.string.explanation_unknown;
    }

    static public String controlTypeToText(ControlType t) {
        switch (t) {
            case CONTROL_DISABLED:
                return "Wi-Fi management disabled";
            case CONTROL_BLUETOOTH:
                return "Bluetooth";
            case CONTROL_GEOFENCE:
                return "Geofence";
            case CONTROL_TOWER:
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

    public void deleteCurrentTower() {
        if (currentCellLocation == null)
            return;

        if (currentCellLocation.getTowerId() < 0)
            return;

        dbSource.deleteSsidTowerInstance(currentCellLocation.getTowerId());

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

    public boolean getWifiAlwaysScanning() {
        boolean ret = false;
        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        for (Method method: wmMethods){
            if (method.getName().equals("isScanAlwaysAvailable")) {
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

        configureTimerangeState();
    }

    public long updateTimeRange(SmarterTimeRange r) {
        long ud =  dbSource.updateTimeRange(r);

        configureTimerangeState();

        return ud;
    }

    public long updateTimeRangeEnabled(SmarterTimeRange r) {
        long ud = dbSource.updateTimeRangeEnabled(r);

        configureTimerangeState();

        return ud;
    }

    private Runnable towerCleanupTask = new Runnable() {
        @Override
        public void run() {
            if (performTowerPurges && dbSource != null && getWifiState() == WifiState.WIFI_ON) {
                SmarterSSID ssid = getCurrentSsid();

                Log.d("smarter", "looking to see if we should purge old towers...");
                dbSource.deleteSsidTowerLastTime(ssid, purgeTowerHours * 60 * 60);

            }

            // every 10 minutes
            timerHandler.postDelayed(this, 1000 * 60 * 10);
        }
    };

}
