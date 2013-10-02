package net.kismetwireless.android.smarterwifimanager;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.doomonafireball.betterpickers.timepicker.TimePickerBuilder;
import com.doomonafireball.betterpickers.timepicker.TimePickerDialogFragment;

import java.util.ArrayList;

/**
 * Created by dragorn on 10/1/13.
 */
public class FragmentTimeRange extends SmarterFragment {
    View mainView;
    Context context;
    FragmentActivity activity;

    ArrayList<SmarterTimeRange> lastTimeList = new ArrayList<SmarterTimeRange>();

    private TimeListAdapter listAdapter;
    private ListView lv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mainView = inflater.inflate(R.layout.fragment_timerange, container, false);

        activity = getActivity();
        context = activity.getApplicationContext();

        lv = (ListView) mainView.findViewById(R.id.timeRangeListView);
        // emptyView = (TextView) mainView.findViewById(R.id.textViewNoWifi);

        lastTimeList.add(new SmarterTimeRange());
        lastTimeList.add(new SmarterTimeRange());

        listAdapter = new TimeListAdapter(context, R.layout.time_entry, lastTimeList);
        lv.setAdapter(listAdapter);

        return mainView;
    }

    public class TimeListAdapter extends ArrayAdapter<SmarterTimeRange> {
        private int layoutResourceId;

        public TimeListAdapter(Context context, int textViewResourceId, ArrayList<SmarterTimeRange> items) {
            super(context, textViewResourceId, items);
            layoutResourceId = textViewResourceId;
        }

        private void collapseView(ImageView collapseIcon, LinearLayout collapsedMain,
                                  LinearLayout expandedMain, TextView daysRepeatView, boolean collapse,
                                  SmarterTimeRange item) {
            if (collapse) {
                collapseIcon.setImageResource(R.drawable.navigation_expand);
                collapsedMain.setVisibility(View.VISIBLE);
                expandedMain.setVisibility(View.GONE);
                daysRepeatView.setText("Mon, Tue, Wed, Thu, Fri, Sat");
            } else {
                collapseIcon.setImageResource(R.drawable.navigation_collapse);
                collapsedMain.setVisibility(View.GONE);
                expandedMain.setVisibility(View.VISIBLE);
                daysRepeatView.setText("");
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                final SmarterTimeRange item = getItem(position);

                View v = null;

                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                    v = inflater.inflate(layoutResourceId, null);
                } else {
                    v = convertView;
                }

                final LinearLayout timeStartContainer, timeEndContainer, expandView;
                final CheckBox wifiCb, bluetoothCb;
                final Switch wifiSwitch, bluetoothSwitch;

                final TextView startHours, startMinutes, startAmPm, endHours, endMinutes, endAmPm;

                final ImageView collapseIcon;
                final LinearLayout collapsedMain, expandedMain;
                final TextView daysRepeatView;

                final TextView repMon, repTue, repWed, repThu, repFri, repSat, repSun;

                collapseIcon = (ImageView) v.findViewById(R.id.collapseIcon);
                collapsedMain = (LinearLayout) v.findViewById(R.id.collapsedMainLayout);
                expandedMain = (LinearLayout) v.findViewById(R.id.expandedMainLayout);
                daysRepeatView = (TextView) v.findViewById(R.id.daysRepeatCollapse);

                timeStartContainer = (LinearLayout) v.findViewById(R.id.timeLayoutStart);
                timeEndContainer = (LinearLayout) v.findViewById(R.id.timeLayoutEnd);

                expandView = (LinearLayout) v.findViewById(R.id.expandView);

                startHours = (TextView) v.findViewById(R.id.timeStartHours);
                startMinutes = (TextView) v.findViewById(R.id.timeStartMinutes);
                endHours = (TextView) v.findViewById(R.id.timeEndHours);
                endMinutes = (TextView) v.findViewById(R.id.timeEndMinutes);
                startAmPm = (TextView) v.findViewById(R.id.timeStart12hr);
                endAmPm = (TextView) v.findViewById(R.id.timeEnd12hr);

                wifiCb = (CheckBox) v.findViewById(R.id.wifiCheckbox);
                wifiSwitch = (Switch) v.findViewById(R.id.wifiSwitch);

                bluetoothCb = (CheckBox) v.findViewById(R.id.bluetoothCheckbox);
                bluetoothSwitch = (Switch) v.findViewById(R.id.bluetoothSwitch);

                repMon = (TextView) v.findViewById(R.id.dayMon);
                repTue = (TextView) v.findViewById(R.id.dayTue);
                repWed = (TextView) v.findViewById(R.id.dayWed);
                repThu = (TextView) v.findViewById(R.id.dayThu);
                repFri = (TextView) v.findViewById(R.id.dayFri);
                repSat = (TextView) v.findViewById(R.id.daySat);
                repSun = (TextView) v.findViewById(R.id.daySun);

                // There are more efficient ways of doing this but it only happens in this one
                // view so...  who cares.
                int dayRep = item.getDays();

                if ((dayRep & SmarterTimeRange.REPEAT_MON) != 0) {
                    repMon.setTextColor(getResources().getColor(R.color.blue));
                    repMon.setTypeface(null, Typeface.BOLD);
                } else {
                    repMon.setTextColor(getResources().getColor(R.color.white));
                    repMon.setTypeface(null, Typeface.NORMAL);
                }
                if ((dayRep & SmarterTimeRange.REPEAT_TUE) != 0) {
                    repTue.setTextColor(getResources().getColor(R.color.blue));
                    repTue.setTypeface(null, Typeface.BOLD);
                } else {
                    repTue.setTextColor(getResources().getColor(R.color.white));
                    repTue.setTypeface(null, Typeface.NORMAL);
                }
                if ((dayRep & SmarterTimeRange.REPEAT_WED) != 0) {
                    repWed.setTextColor(getResources().getColor(R.color.blue));
                    repWed.setTypeface(null, Typeface.BOLD);
                } else {
                    repWed.setTextColor(getResources().getColor(R.color.white));
                    repWed.setTypeface(null, Typeface.NORMAL);
                }
                if ((dayRep & SmarterTimeRange.REPEAT_THU) != 0) {
                    repThu.setTextColor(getResources().getColor(R.color.blue));
                    repThu.setTypeface(null, Typeface.BOLD);
                } else {
                    repThu.setTextColor(getResources().getColor(R.color.white));
                    repThu.setTypeface(null, Typeface.NORMAL);
                }
                if ((dayRep & SmarterTimeRange.REPEAT_FRI) != 0) {
                    repFri.setTextColor(getResources().getColor(R.color.blue));
                    repFri.setTypeface(null, Typeface.BOLD);
                } else {
                    repFri.setTextColor(getResources().getColor(R.color.white));
                    repFri.setTypeface(null, Typeface.NORMAL);
                }
                if ((dayRep & SmarterTimeRange.REPEAT_SAT) != 0) {
                    repSat.setTextColor(getResources().getColor(R.color.blue));
                    repSat.setTypeface(null, Typeface.BOLD);
                } else {
                    repSat.setTextColor(getResources().getColor(R.color.white));
                    repSat.setTypeface(null, Typeface.NORMAL);
                }
                if ((dayRep & SmarterTimeRange.REPEAT_SUN) != 0) {
                    repSun.setTextColor(getResources().getColor(R.color.blue));
                    repSun.setTypeface(null, Typeface.BOLD);
                } else {
                    repSun.setTextColor(getResources().getColor(R.color.white));
                    repSun.setTypeface(null, Typeface.NORMAL);
                }

                repMon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_MON) != 0)
                            d &= ~SmarterTimeRange.REPEAT_MON;
                        else
                            d |= SmarterTimeRange.REPEAT_MON;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });
                repTue.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_TUE) != 0)
                            d &= ~SmarterTimeRange.REPEAT_TUE;
                        else
                            d |= SmarterTimeRange.REPEAT_TUE;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });
                repWed.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_WED) != 0)
                            d &= ~SmarterTimeRange.REPEAT_WED;
                        else
                            d |= SmarterTimeRange.REPEAT_WED;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });
                repThu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_THU) != 0)
                            d &= ~SmarterTimeRange.REPEAT_THU;
                        else
                            d |= SmarterTimeRange.REPEAT_THU;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });
                repFri.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_FRI) != 0)
                            d &= ~SmarterTimeRange.REPEAT_FRI;
                        else
                            d |= SmarterTimeRange.REPEAT_FRI;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });
                repSat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_SAT) != 0)
                            d &= ~SmarterTimeRange.REPEAT_SAT;
                        else
                            d |= SmarterTimeRange.REPEAT_SAT;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });
                repSun.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int d = item.getDays();

                        if ((d & SmarterTimeRange.REPEAT_SUN) != 0)
                            d &= ~SmarterTimeRange.REPEAT_SUN;
                        else
                            d |= SmarterTimeRange.REPEAT_SUN;

                        item.setDays(d);

                        listAdapter.notifyDataSetChanged();
                    }
                });

                wifiCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setWifiControlled(b);
                        if (b) {
                            wifiSwitch.setVisibility(View.VISIBLE);
                        } else {
                            wifiSwitch.setVisibility(View.GONE);
                        }

                        listAdapter.notifyDataSetChanged();
                    }
                });

                bluetoothCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setBluetoothControlled(b);
                        if (b) {
                            bluetoothSwitch.setVisibility(View.VISIBLE);
                        } else {
                            bluetoothSwitch.setVisibility(View.GONE);
                        }

                        listAdapter.notifyDataSetChanged();
                    }
                });

                wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setWifiEnabled(b);
                        listAdapter.notifyDataSetChanged();
                    }
                });

                bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setBluetoothEnabled(b);
                        listAdapter.notifyDataSetChanged();
                    }
                });


                startHours.setText(String.format("%02d", SmarterTimeRange.getHuman12Hour(item.getStartHour())));
                startMinutes.setText(String.format("%02d", item.getStartMinute()));
                startAmPm.setText(SmarterTimeRange.getHumanAmPm(item.getStartHour()) ? "AM" : "PM");

                endHours.setText(String.format("%02d", SmarterTimeRange.getHuman12Hour(item.getEndHour())));
                endMinutes.setText(String.format("%02d", item.getEndMinute()));
                endAmPm.setText(SmarterTimeRange.getHumanAmPm(item.getEndHour()) ? "AM" : "PM");

                wifiCb.setChecked(item.getWifiControlled());
                wifiSwitch.setChecked(item.getWifiEnabled());
                wifiSwitch.setVisibility(item.getWifiControlled() ? View.VISIBLE : View.GONE);

                bluetoothCb.setChecked(item.getBluetoothControlled());
                bluetoothSwitch.setChecked(item.getBluetoothEnabled());
                bluetoothSwitch.setVisibility(item.getBluetoothControlled() ? View.VISIBLE : View.GONE);

                collapseView(collapseIcon, collapsedMain, expandedMain, daysRepeatView, item.getCollapsed(), item);

                // Start and end time launch time pickers
                timeStartContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TimePickerBuilder tpb = new TimePickerBuilder();
                        tpb.setFragmentManager(activity.getSupportFragmentManager());
                        tpb.setStyleResId(R.style.BetterPickersDialogFragment);
                        tpb.addTimePickerDialogHandler(new TimePickerDialogFragment.TimePickerDialogHandler() {
                            @Override
                            public void onDialogTimeSet(int reference, int hourOfDay, int minute) {
                                item.setStartTime(hourOfDay, minute);
                                listAdapter.notifyDataSetChanged();
                            }
                        });
                        tpb.show();
                    }
                });

                timeEndContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TimePickerBuilder tpb = new TimePickerBuilder();
                        tpb.setFragmentManager(activity.getSupportFragmentManager());
                        tpb.setStyleResId(R.style.BetterPickersDialogFragment);
                        tpb.addTimePickerDialogHandler(new TimePickerDialogFragment.TimePickerDialogHandler() {
                            @Override
                            public void onDialogTimeSet(int reference, int hourOfDay, int minute) {
                                item.setEndTime(hourOfDay, minute);
                                listAdapter.notifyDataSetChanged();
                            }
                        });
                        tpb.show();
                    }
                });

                expandView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        item.setCollapsed(!item.getCollapsed());

                        collapseView(collapseIcon, collapsedMain, expandedMain, daysRepeatView, item.getCollapsed(), item);
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
    public int getTitle() {
        return R.string.tab_time;
    }
}
