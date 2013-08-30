package net.kismetwireless.android.smarterwifimanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    public static final String TABLE_SSID_CELL_MAP = "ssidcellmap";
    public static final String COL_SCMAP_ID = "_id";
    public static final String COL_SCMAP_SSIDID = "ssidid";
    public static final String COL_SCMAP_CELLID = "cellid";

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
                    COL_CELL_TIME_S + " int " +
                    ");";

    public static final String CREATE_SSID_CELL_MAP_TABLE =
            "CREATE TABLE " + TABLE_SSID_CELL_MAP + " (" +
                    COL_SCMAP_ID + " integer primary key autoincrement, " +
                    COL_SCMAP_SSIDID + " int, " +
                    COL_SCMAP_CELLID + " int " +
                    ");";

    public static final String DATABASE_NAME = "smartermap.db";
    private static final int DATABASE_VERSION = 1;

    public SmarterWifiDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_SSID_TABLE);
        database.execSQL(CREATE_CELL_TABLE);
        database.execSQL(CREATE_SSID_CELL_MAP_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
