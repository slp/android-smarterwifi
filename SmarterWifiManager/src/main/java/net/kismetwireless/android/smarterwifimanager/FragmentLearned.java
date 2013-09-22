package net.kismetwireless.android.smarterwifimanager;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dragorn on 9/17/13.
 */
public class FragmentLearned extends Fragment {
    private Context context;
    private View mainView;

    private ArrayList<SmarterSSID> lastSsidList = new ArrayList<SmarterSSID>();
    private HashMap<Long, Integer> lastSsidDbToArraylist = new HashMap<Long, Integer>();

    private SmarterWifiServiceBinder serviceBinder;

    private LearnedSsidListAdapter listAdapter;
    private ListView lv;
    private TextView emptyView;

    private long lastTowerUpdate = 0;

    private Handler timeHandler = new Handler();

    public FragmentLearned(SmarterWifiServiceBinder binder) {
        serviceBinder = binder;
    }

    private void updateTowerList() {
        if (serviceBinder == null)
            return;

        ArrayList<SmarterSSID> ssids = serviceBinder.getSsidTowerlist();

        for (SmarterSSID s : ssids) {
            if (!lastSsidDbToArraylist.containsKey(s.getMapDbId())) {
                listAdapter.add(s);
                lastSsidList.add(s);
                lastSsidDbToArraylist.put(s.getMapDbId(), lastSsidList.size() - 1);
            } else {
                lastSsidList.get(lastSsidDbToArraylist.get(s.getMapDbId())).setNumTowers(s.getNumTowers());
            }
        }

        listAdapter.notifyDataSetChanged();
    }

    private Runnable updateTowerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTowerList();

            timeHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (mainView == null)
            mainView = inflater.inflate(R.layout.fragment_learnedssid, container, false);

        context = getActivity().getApplicationContext();

        lv = (ListView) mainView.findViewById(R.id.learnedListView);
        emptyView = (TextView) mainView.findViewById(R.id.textViewNoneLearned);

        listAdapter = new LearnedSsidListAdapter(context, R.layout.ssid_learnlist_entry);
        lv.setAdapter(listAdapter);

        updateTowerRunnable.run();

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

    @Override
    public void onPause() {
        super.onPause();

        timeHandler.removeCallbacks(updateTowerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateTowerRunnable.run();
    }

}
