package net.kismetwireless.android.smarterwifimanager;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by dragorn on 10/1/13.
 */
public class SmarterTimeRange {
    public static int REPEAT_MON = (1 << Calendar.MONDAY);
    public static int REPEAT_TUE = (1 << Calendar.TUESDAY);
    public static int REPEAT_WED = (1 << Calendar.WEDNESDAY);
    public static int REPEAT_THU = (1 << Calendar.THURSDAY);
    public static int REPEAT_FRI = (1 << Calendar.FRIDAY);
    public static int REPEAT_SAT = (1 << Calendar.SATURDAY);
    public static int REPEAT_SUN = (1 << Calendar.SUNDAY);

    private int startHour, startMinute;
    private int endHour, endMinute;
    private int days;

    private boolean controlWifi, controlBluetooth;
    private boolean wifiOn, bluetoothOn;

    // Hide the UI collapse state in here
    private boolean collapsed = true;
    // Is this alarm enabled
    private boolean enabled = true;

    private long dbid = -1;

    public SmarterTimeRange() {
        dbid = -1;
    }

    public SmarterTimeRange(int starthour, int startminute, int endhour, int endminute, int days,
                            boolean controlwifi, boolean controlbt, boolean wifion, boolean bton,
                            boolean enabled, long id) {
        startHour = starthour;
        startMinute = startminute;
        endHour = endhour;
        endMinute = endminute;
        this.days = days;
        controlWifi = controlwifi;
        controlBluetooth = controlbt;
        wifiOn = wifion;
        bluetoothOn = bton;
        this.enabled = enabled;
        dbid = id;
    }

    public boolean isInTimeRange() {
        Calendar calendar = GregorianCalendar.getInstance();

        int hr = calendar.get(Calendar.HOUR_OF_DAY);
        int mn = calendar.get(Calendar.MINUTE);
        int dy = calendar.get(Calendar.DAY_OF_WEEK);

        // Today... is a good day to wi-fi
        if ((dy & days) == 0) {
            return false;
        }

        if (hr < startHour || hr > endHour)
            return false;

        if (mn < startMinute || mn > endMinute)
            return false;

        Log.d("smarter", "Day valid, hr " + hr + " falls within " + startHour + "," + endHour + " and mn " + mn + " within " + startMinute + "," + endHour);

        return true;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int repeats) {
        days = repeats;
    }

    public void setStartTime(int starthour, int startminute) {
        startHour = starthour;
        startMinute = startminute;
    }

    public void setEndTime(int endhour, int endminute) {
        endHour = endhour;
        endMinute = endminute;
    }

    public long getDbId() {
        return dbid;
    }

    public void setDbId(long id) {
        dbid = id;
    }

    public long getNextAlarmMillis() {
        return 0;
    }

    public boolean getWifiControlled() {
        return controlWifi;
    }

    public void setWifiControlled(boolean control) {
        controlWifi = control;
    }

    public boolean getBluetoothControlled() {
        return controlBluetooth;
    }

    public void setBluetoothControlled(boolean control) {
        controlBluetooth = control;
    }

    public boolean getWifiEnabled() {
        return wifiOn;
    }

    public void setWifiEnabled(boolean en) {
        wifiOn = en;
    }

    public boolean getBluetoothEnabled() {
        return bluetoothOn;
    }

    public boolean getCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapse) {
        collapsed = collapse;
    }

    public void setBluetoothEnabled(boolean en) {
        bluetoothOn = en;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e) {
        enabled = e;
    }

    static public int getHuman12Hour(int hour) {
        // 12am
        if (hour == 0)
            return 12;

        // 1am - 12pm
        if (hour <= 12)
            return hour;

        // 1pm to 12am
        return getHuman12Hour(hour - 12);
    }

    // am = true
    // pm = false
    static public boolean getHumanAmPm(int hour) {
        // 12am - 11am
        if (hour < 12)
            return true;

        return false;
    }

    static public String getHumanDayText(Context c, int repeat) {
        StringBuilder sb = new StringBuilder();

        if ((repeat & REPEAT_MON) != 0) {
            sb.append(c.getString(R.string.range_mon));
        }
        if ((repeat & REPEAT_TUE) != 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getString(R.string.range_tue));
        }
        if ((repeat & REPEAT_WED) != 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getString(R.string.range_wed));
        }
        if ((repeat & REPEAT_THU) != 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getString(R.string.range_thu));
        }
        if ((repeat & REPEAT_FRI) != 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getString(R.string.range_fri));
        }
        if ((repeat & REPEAT_SAT) != 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getString(R.string.range_sat));
        }
        if ((repeat & REPEAT_SUN) != 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getString(R.string.range_sun));
        }

        return sb.toString();
    }

}
