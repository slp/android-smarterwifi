package net.kismetwireless.android.smarterwifimanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by dragorn on 8/30/13.
 */
public class SmarterWifiDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_SSID = "ssid";
    public static final String COL_SSID_ID = "_id";
    public static final String COL_SSID_SSID = "ssid";
    public static final String COL_SSID_TIME_S = "timesec";

    public static final String TABLE_CELL = "cell";
    public static final String COL_CELL_ID = "_id";
    public static final String COL_CELL_CELLID = "cellid";
    public static final String COL_CELL_TIME_S = "timesec";
    public static final String COL_CELL_TIME_LAST_S = "lasttimesec";

    public static final String TABLE_SSID_CELL_MAP = "ssidcellmap";
    public static final String COL_SCMAP_ID = "_id";
    public static final String COL_SCMAP_SSIDID = "ssidid";
    public static final String COL_SCMAP_CELLID = "cellid";
    public static final String COL_SCMAP_TIME_S = "timesec";
    public static final String COL_SCMAP_TIME_LAST_S = "lasttimesec";

    public static final String TABLE_SSID_BLACKLIST = "ssidblacklist";
    public static final String COL_SSIDBL_ID = "_id";
    public static final String COL_SSIDBL_SSID = "ssid";
    public static final String COL_SSIDBL_BLACKLIST = "blacklist";

    public static final String TABLE_BT_BLACKLIST = "btblacklist";
    public static final String COL_BTBL_ID = "_id";
    public static final String COL_BTBL_MAC = "btmac";
    public static final String COL_BTBL_NAME = "btname";
    public static final String COL_BTBL_BLACKLIST = "blacklist";
    public static final String COL_BTBL_ENABLE = "enable";

    public static final String TABLE_TIMERANGE = "timerange";
    public static final String COL_TIMERANGE_ID = "_id";
    public static final String COL_TIMERANGE_ENABLED = "enabled";
    public static final String COL_TIMERANGE_START_HR = "starthr";
    public static final String COL_TIMERANGE_START_MIN = "startmin";
    public static final String COL_TIMERANGE_END_HR = "endhr";
    public static final String COL_TIMERANGE_END_MIN = "endmin";
    public static final String COL_TIMERANGE_REPEAT = "repeat";
    public static final String COL_TIMERANGE_CONTROL_WIFI = "controlwifi";
    public static final String COL_TIMERANGE_ENABLE_WIFI = "enablewifi";
    public static final String COL_TIMERANGE_CONTROL_BT = "controlbt";
    public static final String COL_TIMERANGE_ENABLE_BT = "enablebt";

    public static final String CREATE_SSID_TABLE =
            "CREATE TABLE " + TABLE_SSID + " (" +
                    COL_SSID_ID + " integer primary key autoincrement, " +
                    COL_SSID_SSID + " text, " +
                    COL_SSID_TIME_S + " int " +
                    ");";

    public static final String CREATE_CELL_TABLE =
            "CREATE TABLE " + TABLE_CELL + " (" +
                    COL_CELL_ID + " integer primary key autoincrement, " +
                    COL_CELL_CELLID + " int, " +
                    COL_CELL_TIME_S + " int," +
                    COL_CELL_TIME_LAST_S + " int " +
                    ");";

    public static final String CREATE_SSID_CELL_MAP_TABLE =
            "CREATE TABLE " + TABLE_SSID_CELL_MAP + " (" +
                    COL_SCMAP_ID + " integer primary key autoincrement, " +
                    COL_SCMAP_SSIDID + " int, " +
                    COL_SCMAP_CELLID + " int, " +
                    COL_SCMAP_TIME_S + " int," +
                    COL_SCMAP_TIME_LAST_S + " int" +
                    ");";

    public static final String CREATE_SSID_BLACKLIST_TABLE =
            "CREATE TABLE " + TABLE_SSID_BLACKLIST + " (" +
                    COL_SSIDBL_ID + " integer primary key autoincrement, " +
                    COL_SSIDBL_SSID + " text," +
                    COL_SSIDBL_BLACKLIST +  " int" +
                    ");";

    public static final String CREATE_BLUETOOTH_BLACKLIST_TABLE =
            "CREATE TABLE " + TABLE_BT_BLACKLIST + " (" +
                    COL_BTBL_ID + " integer primary key autoincrement, " +
                    COL_BTBL_MAC + " text, " +
                    COL_BTBL_NAME + " text, " +
                    COL_BTBL_BLACKLIST + " int," +
                    COL_BTBL_ENABLE + " int" +
                    ");";

    public static final String CREATE_TIMERANGE_TABLE =
            "CREATE TABLE " + TABLE_TIMERANGE + " (" +
                    COL_TIMERANGE_ID + " integer primary key autoincrement, " +
                    COL_TIMERANGE_ENABLED + " int, " +
                    COL_TIMERANGE_START_HR + " int, " +
                    COL_TIMERANGE_START_MIN + " int, " +
                    COL_TIMERANGE_END_HR + " int, " +
                    COL_TIMERANGE_END_MIN + " int, " +
                    COL_TIMERANGE_REPEAT + " int, " +
                    COL_TIMERANGE_CONTROL_WIFI + " int, " +
                    COL_TIMERANGE_ENABLE_WIFI + " int, " +
                    COL_TIMERANGE_CONTROL_BT + " int, " +
                    COL_TIMERANGE_ENABLE_BT + " int" +
                    ");";

    public static final String DATABASE_NAME = "smartermap.db";
    private static final int DATABASE_VERSION = 10;

    public SmarterWifiDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_SSID_TABLE);
        database.execSQL(CREATE_CELL_TABLE);
        database.execSQL(CREATE_SSID_CELL_MAP_TABLE);
        database.execSQL(CREATE_SSID_BLACKLIST_TABLE);
        database.execSQL(CREATE_BLUETOOTH_BLACKLIST_TABLE);
        database.execSQL(CREATE_TIMERANGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE " + TABLE_SSID_BLACKLIST);
            db.execSQL(CREATE_SSID_BLACKLIST_TABLE);
        }

        if (oldVersion < 7) {
            try {
                db.execSQL("DROP TABLE " + TABLE_BT_BLACKLIST);
            } catch (SQLiteException e) {
                Log.e("smarter", "failed to drop old table, soldiering on: " + e);
            }

            db.execSQL(CREATE_BLUETOOTH_BLACKLIST_TABLE);
        }

        if (oldVersion < 8) {
            Log.d("smarter", "Purging old cell tower format");
            db.execSQL("DELETE FROM " + TABLE_SSID_CELL_MAP);
            db.execSQL("DELETE FROM " + TABLE_CELL);
        }

        if (oldVersion < 9) {
            Log.d("smarter", "creating new timerange table");
            db.execSQL(CREATE_TIMERANGE_TABLE);
        }

        if (oldVersion < 10) {
            Log.d("smarter", "adding last timesec column");
            db.execSQL("ALTER TABLE " + TABLE_CELL + " ADD COLUMN " + COL_CELL_TIME_LAST_S + " int;");
            db.execSQL("ALTER TABLE " + TABLE_SSID_CELL_MAP + " ADD COLUMN " + COL_SCMAP_TIME_LAST_S + " int;");
        }

    }

}
