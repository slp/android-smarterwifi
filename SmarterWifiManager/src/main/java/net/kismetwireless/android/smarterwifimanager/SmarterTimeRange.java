package net.kismetwireless.android.smarterwifimanager;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by dragorn on 10/1/13.
 *
 * This has to be parcelable because we can keep un-saved changes in fragments
 */
public class SmarterTimeRange implements Parcelable {
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

    private int oldStartHour, oldStartMinute, oldEndHour, oldEndMinute, oldDays;

    private boolean controlWifi, controlBluetooth;
    private boolean wifiOn, bluetoothOn;

    private boolean oldControlWifi, oldControlBluetooth, oldWifiOn, oldBluetoothOn;

    private boolean dirty = false;

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
        oldStartHour = startHour = starthour;
        oldStartMinute = startMinute = startminute;
        oldEndHour = endHour = endhour;
        oldEndMinute = endMinute = endminute;
        this.oldDays = this.days = days;
        oldControlWifi = controlWifi = controlwifi;
        oldControlBluetooth = controlBluetooth = controlbt;
        oldWifiOn = wifiOn = wifion;
        oldBluetoothOn = bluetoothOn = bton;

        this.enabled = enabled;
        dirty = false;

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

        // Log.d("smarter", "Day valid, hr " + hr + " falls within " + startHour + "," + endHour + " and mn " + mn + " within " + startMinute + "," + endHour);

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
        if (days != repeats)
            dirty = true;
        // Log.d("smarter", "setdays " + dirty);

        oldDays = days;

        days = repeats;
    }

    public void setStartTime(int starthour, int startminute) {
        if (startHour != starthour || startMinute != startminute)
            dirty = true;
        //Log.d("smarter", "setstarttime " + dirty);

        oldStartHour = startHour;
        oldStartMinute = startMinute;

        startHour = starthour;
        startMinute = startminute;
    }

    public void setEndTime(int endhour, int endminute) {
        if (endHour != endhour || endMinute != endminute)
            dirty = true;
        //Log.d("smarter", "setendtime " + dirty);

        oldEndHour = endHour;
        oldEndMinute = endMinute;

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
        if (controlWifi != control)
            dirty = true;
        //Log.d("smarter", "setwificontrolled " + dirty);

        oldControlWifi = controlWifi;

        controlWifi = control;
    }

    public boolean getBluetoothControlled() {
        return controlBluetooth;
    }

    public void setBluetoothControlled(boolean control) {
        if (controlBluetooth != control)
            dirty = true;
        //Log.d("smarter", "setbtcontrolled " + dirty);

        oldControlBluetooth = controlBluetooth;

        controlBluetooth = control;
    }

    public boolean getWifiEnabled() {
        return wifiOn;
    }

    public void setWifiEnabled(boolean en) {
        if (wifiOn != en)
            dirty = true;

        //Log.d("smarter", "setwifienabled " + dirty);

        oldWifiOn = wifiOn;

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
        if (bluetoothOn != en)
            dirty = true;

        //Log.d("smarter", "setbtenabled " + dirty);

        oldBluetoothOn = bluetoothOn;

        bluetoothOn = en;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e) {
        enabled = e;
    }

    public boolean getDirty() {
        return dirty;
    }

    public boolean getRevertable() {
        if (dirty && dbid >= 0)
            return true;

        return false;
    }

    public void revertChanges() {
        startHour = oldStartHour;
        startMinute = oldStartMinute;
        endHour = oldEndHour;
        endMinute = oldEndMinute;
        days = oldDays;
        controlWifi = oldControlWifi;
        wifiOn = oldWifiOn;
        controlBluetooth = oldControlBluetooth;
        bluetoothOn = oldBluetoothOn;

        dirty = false;
    }

    public void applyChanges() {
        oldStartHour = startHour;
        oldStartMinute = startMinute;
        oldEndHour = endHour;
        oldEndMinute = endMinute;
        oldDays = days;
        oldControlWifi = controlWifi;
        oldWifiOn = wifiOn;
        oldControlBluetooth = controlBluetooth;
        oldBluetoothOn = bluetoothOn;

        dirty = false;
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

    public SmarterTimeRange(Parcel source) {
        enabled = (source.readInt() == 1);
        collapsed = (source.readInt() == 1);

        startHour = source.readInt();
        startMinute = source.readInt();
        endHour = source.readInt();
        endMinute = source.readInt();
        days = source.readInt();
        controlWifi = (source.readInt() == 1);
        wifiOn = (source.readInt() == 1);
        controlBluetooth = (source.readInt() == 1);
        bluetoothOn = (source.readInt() == 1);
        dbid = source.readLong();

        oldStartHour = source.readInt();
        oldStartMinute = source.readInt();
        oldEndHour = source.readInt();
        oldEndMinute = source.readInt();
        oldDays = source.readInt();
        oldControlWifi = (source.readInt() == 1);
        oldWifiOn = (source.readInt() == 1);
        oldControlBluetooth = (source.readInt() == 1);
        oldBluetoothOn = (source.readInt() == 1);

    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(collapsed ? 1 : 0);

        dest.writeInt(startHour);
        dest.writeInt(startMinute);
        dest.writeInt(endHour);
        dest.writeInt(endMinute);
        dest.writeInt(days);
        dest.writeInt(controlWifi ? 1 : 0);
        dest.writeInt(wifiOn ? 1 : 0);
        dest.writeInt(controlBluetooth ? 1 : 0);
        dest.writeInt(bluetoothOn ? 1 : 0);
        dest.writeLong(dbid);

        dest.writeInt(oldStartHour);
        dest.writeInt(oldStartMinute);
        dest.writeInt(oldEndHour);
        dest.writeInt(oldEndMinute);
        dest.writeInt(oldDays);
        dest.writeInt(oldControlWifi ? 1 : 0);
        dest.writeInt(oldWifiOn ? 1 : 0);
        dest.writeInt(oldControlBluetooth ? 1 : 0);
        dest.writeInt(oldBluetoothOn ? 1 : 0);
    }

    public int describeContents() {
        return hashCode();
    }

    public class MyCreator implements Parcelable.Creator<SmarterTimeRange> {
        public SmarterTimeRange createFromParcel(Parcel source) {
            return new SmarterTimeRange(source);
        }
        public SmarterTimeRange[] newArray(int size) {
            return new SmarterTimeRange[size];
        }
    }

    // returns if time range is valid, and the resource id of why not or -1
    public int getRangeValid() {
        if (!controlWifi && !controlBluetooth)
            return R.string.range_fail_nocontrol;

        if (days == 0)
            return R.string.range_fail_nodays;

        if (startHour == endHour && startMinute == endMinute)
            return R.string.range_fail_notime;

        return -1;
    }


}
