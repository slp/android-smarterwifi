package net.kismetwireless.android.smarterwifimanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
    private TextView emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mainView = inflater.inflate(R.layout.fragment_timerange, container, false);

        activity = getActivity();
        context = activity.getApplicationContext();

        lv = (ListView) mainView.findViewById(R.id.timeRangeListView);
        emptyView = (TextView) mainView.findViewById(R.id.textViewNoTime);

        listAdapter = new TimeListAdapter(context, R.layout.time_entry, lastTimeList);
        lv.setAdapter(listAdapter);

        return mainView;
    }

    public void addTimeRange() {
        lastTimeList.add(new SmarterTimeRange());

        if (listAdapter != null)
            listAdapter.notifyDataSetChanged();
    }

    public class TimeListAdapter extends ArrayAdapter<SmarterTimeRange> {
        private int layoutResourceId;

        public TimeListAdapter(Context context, int textViewResourceId, ArrayList<SmarterTimeRange> items) {
            super(context, textViewResourceId, items);
            layoutResourceId = textViewResourceId;
        }

        // Ugly call so we can pass finals
        private void collapseView(LinearLayout collapsedMain, LinearLayout expandedMain,
                                  LinearLayout collapseView, LinearLayout expandView,
                                  boolean collapse, SmarterTimeRange item) {

            // Extract from the main views
            TextView daysRepeatView = (TextView) expandView.findViewById(R.id.daysRepeatCollapse);

            TextView summaryView = (TextView) collapsedMain.findViewById(R.id.rangeSummaryText);

            if (!item.getEnabled()) {
                summaryView.setText(R.string.timerange_disabled_text);
            } else {
                if (item.getDays() == 0) {
                    summaryView.setText(R.string.timerange_no_days);
                } else if (!item.getBluetoothControlled() && !item.getWifiControlled()) {
                    summaryView.setText(context.getString(R.string.timerange_no_effect));
                } else {
                    StringBuilder sb = new StringBuilder();

                    if (item.getWifiControlled()) {
                        sb.append(context.getString(R.string.timerange_control_wifi));
                        sb.append(" ");
                        if (item.getWifiEnabled())
                            sb.append(context.getString(R.string.timerange_control_on));
                        else
                            sb.append(context.getString(R.string.timerange_control_off));
                    }

                    if (item.getBluetoothControlled()) {
                        if (sb.length() > 0)
                            sb.append(", ");

                        sb.append(context.getString(R.string.timerange_control_bluetooth));
                        sb.append(" ");
                        if (item.getBluetoothEnabled())
                            sb.append(context.getString(R.string.timerange_control_on));
                        else
                            sb.append(context.getString(R.string.timerange_control_off));
                    }

                    summaryView.setText(sb.toString());
                }
            }

            if (collapse) {
                collapseView.setVisibility(View.GONE);
                expandView.setVisibility(View.VISIBLE);

                collapsedMain.setVisibility(View.VISIBLE);
                expandedMain.setVisibility(View.GONE);

                daysRepeatView.setText(SmarterTimeRange.getHumanDayText(context, item.getDays()));
            } else {
                collapseView.setVisibility(View.VISIBLE);
                expandView.setVisibility(View.GONE);

                collapsedMain.setVisibility(View.GONE);
                expandedMain.setVisibility(View.VISIBLE);
            }
        }

        private void toggleImageViewEnable(ImageView v, boolean b) {
            // If we dont' leave the background view clickable, we can use this to disable the buttons
            v.setEnabled(b);
            v.setClickable(b);

            if (b) {
                AlphaAnimation alpha = new AlphaAnimation(1.0F, 1.0F);
                alpha.setDuration(0);
                alpha.setFillAfter(true);
                v.startAnimation(alpha);

                /* We don't need this if we dont' use the whole background row as a clickable collapse; leave for reference
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    TypedArray a = context.obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    v.setBackground(context.getResources().getDrawable(resource));
                }
                */
            } else {
                AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
                alpha.setDuration(0);
                alpha.setFillAfter(true);
                v.startAnimation(alpha);
                // v.setBackground(null);
            }
        }

        private void deleteDialog(final SmarterTimeRange item) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.timerange_dialog_delete_title);
            builder.setMessage(R.string.timerange_dialog_delete_text);

            builder.setNegativeButton(R.string.timerange_dialog_delete_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });

            builder.setPositiveButton(R.string.timerange_dialog_delete_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // serviceBinder.deleteSsidTowerMap(entry);

                    lastTimeList.remove(item);
                    listAdapter.notifyDataSetChanged();
                }
            });

            builder.create().show();
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

                // Containers we pass to the hide/show function
                final LinearLayout timeStartContainer, timeEndContainer, expandView, collapseView;

                final CheckBox wifiCb, bluetoothCb;
                final CompoundButton wifiSwitch, bluetoothSwitch, enableSwitch;

                // Clock
                final TextView startHours, startMinutes, startAmPm, endHours, endMinutes, endAmPm;

                // Expand/collapse are two layouts for simplicity
                final LinearLayout collapsedMain, expandedMain;

                // Main image buttons
                final ImageView deleteCollapse, deleteExpand, undoExpand, saveExpand;

                // Blue/bold day picker text
                final TextView repMon, repTue, repWed, repThu, repFri, repSat, repSun;

                collapsedMain = (LinearLayout) v.findViewById(R.id.collapsedMainLayout);
                expandedMain = (LinearLayout) v.findViewById(R.id.expandedMainLayout);
                expandView = (LinearLayout) v.findViewById(R.id.expandView);
                collapseView = (LinearLayout) v.findViewById(R.id.collapseView);

                deleteCollapse = (ImageView) v.findViewById(R.id.timeRangeDeleteCollapse);
                deleteExpand = (ImageView) v.findViewById(R.id.timeRangeDelete);
                undoExpand = (ImageView) v.findViewById(R.id.timeRangeUndo);
                saveExpand= (ImageView) v.findViewById(R.id.timeRangeSave);

                timeStartContainer = (LinearLayout) v.findViewById(R.id.timeLayoutStart);
                timeEndContainer = (LinearLayout) v.findViewById(R.id.timeLayoutEnd);

                startHours = (TextView) v.findViewById(R.id.timeStartHours);
                startMinutes = (TextView) v.findViewById(R.id.timeStartMinutes);
                endHours = (TextView) v.findViewById(R.id.timeEndHours);
                endMinutes = (TextView) v.findViewById(R.id.timeEndMinutes);
                startAmPm = (TextView) v.findViewById(R.id.timeStart12hr);
                endAmPm = (TextView) v.findViewById(R.id.timeEnd12hr);

                wifiCb = (CheckBox) v.findViewById(R.id.wifiCheckbox);
                wifiSwitch = (CompoundButton) v.findViewById(R.id.wifiSwitch);

                bluetoothCb = (CheckBox) v.findViewById(R.id.bluetoothCheckbox);
                bluetoothSwitch = (CompoundButton) v.findViewById(R.id.bluetoothSwitch);

                enableSwitch = (CompoundButton) v.findViewById(R.id.timeRangeToggle);

                repMon = (TextView) v.findViewById(R.id.dayMon);
                repTue = (TextView) v.findViewById(R.id.dayTue);
                repWed = (TextView) v.findViewById(R.id.dayWed);
                repThu = (TextView) v.findViewById(R.id.dayThu);
                repFri = (TextView) v.findViewById(R.id.dayFri);
                repSat = (TextView) v.findViewById(R.id.daySat);
                repSun = (TextView) v.findViewById(R.id.daySun);

                if (item.getDirty()) {
                    if (item.getRevertable())
                        toggleImageViewEnable(undoExpand, true);
                    else
                        toggleImageViewEnable(undoExpand, false);

                    toggleImageViewEnable(saveExpand, true);
                } else {
                    toggleImageViewEnable(undoExpand, false);
                    toggleImageViewEnable(saveExpand, false);
                }

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

                enableSwitch.setChecked(item.getEnabled());

                collapseView(collapsedMain, expandedMain, expandView, collapseView, item.getCollapsed(), item);

                enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        item.setEnabled(b);

                        // Disable and open, close
                        if (!b) {
                            timeStartContainer.setClickable(false);
                            timeEndContainer.setClickable(false);
                            timeStartContainer.setEnabled(false);
                            timeEndContainer.setEnabled(false);

                            if (!item.getCollapsed()) {
                                item.setCollapsed(true);
                                collapseView(collapsedMain, expandedMain, expandView, collapseView, item.getCollapsed(), item);
                            }
                        }

                        // Enable and closed, open
                        if (b) {
                            timeStartContainer.setClickable(true);
                            timeEndContainer.setClickable(true);
                            timeStartContainer.setEnabled(true);
                            timeEndContainer.setEnabled(true);

                            if (item.getCollapsed()) {
                                item.setCollapsed(false);
                                collapseView(collapsedMain, expandedMain, expandView, collapseView, item.getCollapsed(), item);
                            }
                        }

                        listAdapter.notifyDataSetChanged();
                    }
                });

                // Start and end time launch time pickers
                timeStartContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        item.setCollapsed(false);
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
                        listAdapter.notifyDataSetChanged();
                    }
                });

                timeEndContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        item.setCollapsed(false);
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
                        listAdapter.notifyDataSetChanged();
                    }
                });

                /*
                expandView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // We can't expand if we're not enabled
                        if (!item.getEnabled())
                            return;

                        item.setCollapsed(!item.getCollapsed());

                        collapseView(collapsedMain, expandedMain, expandView, collapseView, item.getCollapsed(), item);
                    }
                });
                */

                collapseView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        item.setCollapsed(!item.getCollapsed());

                        collapseView(collapsedMain, expandedMain, expandView, collapseView, item.getCollapsed(), item);
                    }
                });

                deleteCollapse.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteDialog(item);
                    }
                });

                deleteExpand.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteDialog(item);
                    }
                });

                undoExpand.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!item.getRevertable())
                            return;

                        item.revertChanges();

                        listAdapter.notifyDataSetChanged();
                    }
                });

                saveExpand.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!item.getDirty())
                            return;

                        // Do save
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
    public int getTitle() {
        return R.string.tab_time;
    }
}
