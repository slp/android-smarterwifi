package net.kismetwireless.android.smarterwifimanager;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dragorn on 9/17/13.
 */
public class FragmentLearned extends Fragment {
    Context context;
    View mainView;

    ArrayList<SmarterSSID> lastSsidList = new ArrayList<SmarterSSID>();

    SmarterWifiServiceBinder serviceBinder;
    LearnedSsidListAdapter listAdapter;
    ListView lv;
    TextView emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mainView == null)
            mainView = inflater.inflate(R.layout.fragment_learnedssid, container, false);

        context = getActivity().getApplicationContext();

        serviceBinder = new SmarterWifiServiceBinder(context);

        lv = (ListView) mainView.findViewById(R.id.learnedListView);
        emptyView = (TextView) mainView.findViewById(R.id.textViewNoneLearned);

        listAdapter = new LearnedSsidListAdapter(context, R.layout.ssid_learnlist_entry);
        lv.setAdapter(listAdapter);

        serviceBinder.doCallAndBindService(new SmarterWifiServiceBinder.BinderCallback() {
            public void run(SmarterWifiService s) {
                lastSsidList = serviceBinder.getSsidTowerlist();
                listAdapter.addAll(lastSsidList);

                if (lastSsidList.size() == 0) {
                    lv.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    lv.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            }
        });

        return mainView;
    }

    public class LearnedSsidListAdapter extends ArrayAdapter<SmarterSSID> {
        private int layoutResourceId;

        public LearnedSsidListAdapter(Context context, int textViewResourceId) {
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
                TextView towerView = (TextView) v.findViewById(R.id.ssidListTower);
                ImageView trashImage = (ImageView) v.findViewById(R.id.ssidListDelete);

                ssidView.setText(entry.getDisplaySsid());
                towerView.setText("Learned " + Integer.toString(entry.getNumTowers()) + " towers");

                trashImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        builder.setTitle(R.string.delete_dialog_title);
                        builder.setMessage(R.string.delete_learned_message);

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });

                        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                serviceBinder.deleteSsidTowerMap(entry);

                                lastSsidList = serviceBinder.getSsidTowerlist();
                                listAdapter.clear();
                                listAdapter.addAll(lastSsidList);
                            }
                        });

                        builder.create().show();

                    }
                });

                return v;
            } catch (Exception ex) {
                Log.e("smarter", "error", ex);
                return null;
            }
        }
    }

}
