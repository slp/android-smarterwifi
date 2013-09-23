package net.kismetwireless.android.smarterwifimanager;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by dragorn on 9/17/13.
 */
public class FragmentMain extends Fragment {
    View mainView;

    SmarterWifiServiceBinder serviceBinder;
    Context context;

    ImageView mainIcon;
    TextView headlineText, smallText;

    Switch switchManageWifi, switchAutoLearn;

    SharedPreferences sharedPreferences;

    private SmarterWifiService.SmarterServiceCallback guiCallback = new SmarterWifiService.SmarterServiceCallback() {
        @Override
        public void wifiStateChanged(final SmarterSSID ssid, final SmarterWifiService.WifiState state, final SmarterWifiService.ControlType type) {
            super.wifiStateChanged(ssid, state, type);

            Activity ma = getActivity();

            if (ma == null)
                return;

            ma.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
                        case WIFI_IGNORE:
                            wifiIconId = R.drawable.custom_wifi_enabled;
                            wifiText = "Wi-Fi management disabled";
                            break;

                        default:
                            wifiIconId = R.drawable.custom_wifi_inactive;
                    }

                    mainIcon.setImageResource(wifiIconId);
                    headlineText.setText(wifiText);
                    smallText.setText(SmarterWifiService.controlTypeToText(controlType));
                }
            });
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mainView == null)
            mainView = inflater.inflate(R.layout.fragment_main, container, false);

        context = getActivity().getApplicationContext();

        serviceBinder = new SmarterWifiServiceBinder(context);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mainIcon = (ImageView) mainView.findViewById(R.id.imageWifiStatus);
        headlineText = (TextView) mainView.findViewById(R.id.textViewMain);
        smallText = (TextView) mainView.findViewById(R.id.textViewMinor);

        switchManageWifi = (Switch) mainView.findViewById(R.id.switchManageWifi);
        switchAutoLearn =  (Switch) mainView.findViewById(R.id.switchAutoLearn);

        // Defer main setup until we've bound
        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            @Override
            public void run(SmarterWifiServiceBinder b) {
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

                serviceBinder.addCallback(guiCallback);

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
        });

        return mainView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (serviceBinder != null)
            serviceBinder.removeCallback(guiCallback);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceBinder != null)
            serviceBinder.addCallback(guiCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceBinder != null)
            serviceBinder.doUnbindService();
    }

    private boolean setManageWifi(boolean b) {
        if (serviceBinder == null)
            return false;

        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putBoolean(getString(R.string.pref_enable), b);
        e.commit();

        serviceBinder.doUpdatePreferences();

        return true;
    }

    private boolean setLearnWifi(boolean b) {
        if (serviceBinder == null)
            return false;

        SharedPreferences.Editor e = sharedPreferences.edit();
        e.putBoolean(getString(R.string.pref_learn), b);
        e.commit();

        return true;
    }
}