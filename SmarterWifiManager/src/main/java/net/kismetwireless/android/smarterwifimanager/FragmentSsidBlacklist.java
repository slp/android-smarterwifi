package net.kismetwireless.android.smarterwifimanager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dragorn on 9/17/13.
 */
public class FragmentSsidBlacklist extends SmarterFragment {
    private Context context;
    private View mainView;

    private ArrayList<SmarterSSID> lastSsidList = new ArrayList<SmarterSSID>();

    private SmarterWifiServiceBinder serviceBinder;
    private SsidListAdapter listAdapter;
    private ListView lv;
    private TextView emptyView;

    private SmarterWifiService.WifiState wifiState = SmarterWifiService.WifiState.WIFI_IGNORE;

    public void updateSsidList() {
        lastSsidList = serviceBinder.getSsidBlacklist();

        if (listAdapter != null) {
            listAdapter.addAll(lastSsidList);

            if (lastSsidList.size() == 0) {
                lv.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                lv.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    private SmarterWifiService.SmarterServiceCallback callback = new SmarterWifiService.SmarterServiceCallback() {
        @Override
        public void wifiStateChanged(final SmarterSSID ssid, final SmarterWifiService.WifiState state, final SmarterWifiService.ControlType type) {
            super.wifiStateChanged(ssid, state, type);

            Activity ma = getActivity();

            if (ma != null) {
                ma.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ((state == SmarterWifiService.WifiState.WIFI_ON || state == SmarterWifiService.WifiState.WIFI_IDLE) &&
                                (wifiState == SmarterWifiService.WifiState.WIFI_OFF || wifiState == SmarterWifiService.WifiState.WIFI_BLOCKED)) {

                            lastSsidList = serviceBinder.getSsidBlacklist();
                            listAdapter.addAll(lastSsidList);

                            if (lastSsidList.size() == 0) {
                                lv.setVisibility(View.GONE);
                                emptyView.setVisibility(View.VISIBLE);
                            } else {
                                lv.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                            }

                        }
                    }
                });
            }
            wifiState = state;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mainView == null)
            mainView = inflater.inflate(R.layout.fragment_ssidblacklist, container, false);

        context = getActivity().getApplicationContext();

        lv = (ListView) mainView.findViewById(R.id.ssidBlacklistListview);
        emptyView = (TextView) mainView.findViewById(R.id.textViewNoWifi);

        listAdapter = new SsidListAdapter(context, R.layout.ssid_blacklist_entry);
        lv.setAdapter(listAdapter);

        serviceBinder = new SmarterWifiServiceBinder(context);

        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            @Override
            public void run(SmarterWifiServiceBinder b) {
                serviceBinder.addCallback(callback);
                updateSsidList();
            }
        });

        return mainView;
    }

    public class SsidListAdapter extends ArrayAdapter<SmarterSSID> {
        private int layoutResourceId;

        public SsidListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            layoutResourceId = textViewResourceId;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                final SmarterSSID entry = getItem(position);

                View v = null;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    v = inflater.inflate(layoutResourceId, null);
                } else {
                    v = convertView;
                }

                TextView ssidView = (TextView) v.findViewById(R.id.ssidListSsid);
                CheckBox ssidCb = (CheckBox) v.findViewById(R.id.ssidListCheck);

                ssidView.setText(entry.getDisplaySsid());
                ssidCb.setChecked(entry.isBlacklisted());

                ssidCb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CheckBox cb = (CheckBox) view;

                        entry.setBlacklisted(!entry.isBlacklisted());
                        cb.setChecked(entry.isBlacklisted());

                        Log.d("smarter", "listadapter setting " + entry.getSsid() + " to " + entry.isBlacklisted());
                        serviceBinder.setSsidBlacklisted(entry, entry.isBlacklisted());
                        listAdapter.notifyDataSetChanged();

                    }
                });
                /*
                ssidCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        serviceBinder.setSsidBlacklisted(entry, b);
                        entry.setBlacklisted(b);
                    }
                });
                */

                return v;
            } catch (Exception ex) {
                Log.e("smarter", "error", ex);
                return null;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (serviceBinder != null)
            serviceBinder.removeCallback(callback);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceBinder != null)
            serviceBinder.addCallback(callback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceBinder != null)
            serviceBinder.doUnbindService();
    }

    @Override
    public int getTitle() {
        return R.string.tab_ignore;
    }
}
