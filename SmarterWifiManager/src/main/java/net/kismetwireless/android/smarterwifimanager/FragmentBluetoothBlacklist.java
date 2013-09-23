package net.kismetwireless.android.smarterwifimanager;

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
public class FragmentBluetoothBlacklist extends SmarterFragment {
    private Context context;
    private View mainView;

    private ArrayList<SmarterBluetooth> lastBtList = new ArrayList<SmarterBluetooth>();

    private SmarterWifiServiceBinder serviceBinder;
    private BluetoothListAdapter listAdapter;
    private ListView lv;
    private TextView emptyView;

    public void updateBluetoothList() {
        lastBtList = serviceBinder.getBluetoothBlacklist();

        if (listAdapter != null) {
            listAdapter.addAll(lastBtList);

            if (lastBtList.size() == 0) {
                lv.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                lv.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mainView == null)
            mainView = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        context = getActivity().getApplicationContext();

        lv = (ListView) mainView.findViewById(R.id.bluetoothListView);
        emptyView = (TextView) mainView.findViewById(R.id.textViewNoBluetooth);

        listAdapter = new BluetoothListAdapter(context, R.layout.bluetooth_blacklist_entry);
        lv.setAdapter(listAdapter);

        serviceBinder = new SmarterWifiServiceBinder(context);
        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            @Override
            public void run(SmarterWifiServiceBinder b) {
                updateBluetoothList();
            }
        });

        return mainView;
    }

    public class BluetoothListAdapter extends ArrayAdapter<SmarterBluetooth> {
        private int layoutResourceId;

        public BluetoothListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            layoutResourceId = textViewResourceId;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                final SmarterBluetooth entry = getItem(position);

                View v = null;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    v = inflater.inflate(layoutResourceId, null);
                } else {
                    v = convertView;
                }

                TextView btView = (TextView) v.findViewById(R.id.btListDevice);
                CheckBox btCb = (CheckBox) v.findViewById(R.id.btListCheck);

                btView.setText(entry.getBtName());
                btCb.setChecked(entry.isBlacklisted());

                btCb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CheckBox cb = (CheckBox) view;

                        entry.setBlacklisted(!entry.isBlacklisted());
                        cb.setChecked(entry.isBlacklisted());

                        Log.d("smarter", "listadapter setting " + entry.getBtName() + " to " + entry.isBlacklisted());

                        serviceBinder.setBluetoothBlacklisted(entry, entry.isBlacklisted(), false);
                        listAdapter.notifyDataSetChanged();
                    }
                });

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
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceBinder != null)
            serviceBinder.doUnbindService();
    }

    @Override
    public int getTitle() {
        return R.string.tab_bluetooth;
    }

}
