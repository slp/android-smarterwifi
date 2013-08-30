package net.kismetwireless.android.smarterwifimanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

class SmarterWifiService extends Service {
    private boolean shutdown = false;
    SharedPreferences preferences;

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

        return START_STICKY;
    }



    public void updatePreferences() {

    }

    public void shutdownService() {

    }
}