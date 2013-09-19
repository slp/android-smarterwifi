package net.kismetwireless.android.smarterwifimanager;

import android.app.Fragment;
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
public class FragmentSsidBlacklist extends Fragment {
    Context context;
    View mainView;

    ArrayList<SsidBlacklistEntry> lastSsidList = new ArrayList<SsidBlacklistEntry>();

    SmarterWifiServiceBinder serviceBinder;
    SsidListAdapter listAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mainView == null)
            mainView = inflater.inflate(R.layout.fragment_ssidblacklist, container, false);

        context = getActivity().getApplicationContext();

        listAdapter = new SsidListAdapter(context, R.layout.ssid_list_entry);
        ListView lv = (ListView) mainView.findViewById(R.id.ssidBlacklistListview);
        lv.setAdapter(listAdapter);

        serviceBinder = new SmarterWifiServiceBinder(context);

        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            public void run(SmarterWifiService s) {
                lastSsidList = serviceBinder.getSsidBlacklist();
                listAdapter.addAll(lastSsidList);
            }
        });

        return mainView;
    }

    public class SsidListAdapter extends ArrayAdapter<SsidBlacklistEntry> {
        private int layoutResourceId;

        public SsidListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            layoutResourceId = textViewResourceId;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                SsidBlacklistEntry entry = getItem(position);

                View v = null;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    v = inflater.inflate(layoutResourceId, null);
                } else {
                    v = convertView;
                }

                TextView ssidView = (TextView) v.findViewById(R.id.ssidListSsid);
                CheckBox ssidCb = (CheckBox) v.findViewById(R.id.ssidListCheck);

                ssidView.setText(entry.getSsid());
                ssidCb.setChecked(entry.isBlacklisted());

                return v;
            } catch (Exception ex) {
                Log.e("smarter", "error", ex);
                return null;
            }
        }
    }

}
