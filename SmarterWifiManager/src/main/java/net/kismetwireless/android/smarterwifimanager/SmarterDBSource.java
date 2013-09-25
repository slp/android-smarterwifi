package net.kismetwireless.android.smarterwifimanager;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;

/**
 * Created by dragorn on 8/30/13.
 */
public class SmarterDBSource {
    private SQLiteDatabase dataBase;
    private SmarterWifiDBHelper dataBaseHelper;

    public SmarterDBSource(Context c) throws SQLiteException {
        dataBaseHelper = new SmarterWifiDBHelper(c);

        dataBase = dataBaseHelper.getWritableDatabase();
    }

    public long getTowerDbId(long towerid) {
        long id = -1;

        final String[] idcol = {SmarterWifiDBHelper.COL_CELL_ID};

        String compare = SmarterWifiDBHelper.COL_CELL_CELLID + " = " + towerid;

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_CELL, idcol, compare, null, null, null, null);

        c.moveToFirst();

        if (c.getCount() <= 0) {
            c.close();
            // Log.d("smarter", "getTowerDb " + towerid + " not found return -1");
            return -1;
        }

        // Log.d("smarter", "gettowerdb " + towerid + " id " + id);
        id = c.getLong(0);

        c.close();

        return id;
    }

    // Is this tower associated with any known SSID?
    public boolean queryTowerMapped(long towerid) {
        long tid = getTowerDbId(towerid);

        // If we don't know the tower...
        if (tid < 0)
            return false;

        // Do we have a SSID that uses this?
        final String[] idcol = {SmarterWifiDBHelper.COL_SCMAP_SSIDID};

        String compare = SmarterWifiDBHelper.COL_SCMAP_CELLID + " = " + tid;

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_SSID_CELL_MAP, idcol, compare, null, null, null, null);

        c.moveToFirst();

        if (c.getCount() <= 0) {
            c.close();
            return false;
        }

        c.close();

        // We map to a ssid
        return true;
    }

    // Update time or create new tower
    public long updateTower(long towerid, long tid) {
        // Log.d("smarter", "updating tower " + towerid + " local id " + tid);
        if (tid < 0)
            tid = getTowerDbId(towerid);

        ContentValues cv = new ContentValues();

        if (tid < 0) {
            cv.put(SmarterWifiDBHelper.COL_CELL_CELLID, towerid);
        }

        cv.put(SmarterWifiDBHelper.COL_CELL_TIME_S, System.currentTimeMillis() / 1000);

        String compare = SmarterWifiDBHelper.COL_CELL_ID + " = ?";
        String args[] = {Long.toString(towerid)};

        dataBase.beginTransaction();

        if (tid < 0)
            tid = dataBase.insert(SmarterWifiDBHelper.TABLE_CELL, null, cv);
        else
            dataBase.update(SmarterWifiDBHelper.TABLE_CELL, cv, compare, args);

        dataBase.setTransactionSuccessful();
        dataBase.endTransaction();

        // Log.d("smarter", "updatetower returning for " + towerid + " value " + tid);
        return tid;
    }

    public long getSsidDbId(String ssid) {
        long id = -1;

        final String[] idcol = {SmarterWifiDBHelper.COL_SSID_ID};

        String compare = SmarterWifiDBHelper.COL_SSID_SSID + "=?";
        String[] args = {ssid};

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_SSID, idcol, compare, args, null, null, null);

        c.moveToFirst();

        if (c.getCount() <= 0) {
            c.close();
            return -1;
        }

        id = c.getLong(0);

        c.close();

        return id;
    }

    // Update time or create new ssid
    public long updateSsid(String ssid, long sid) {
        if (sid < 0)
            sid = getSsidDbId(ssid);

        ContentValues cv = new ContentValues();

        if (sid < 0) {
            cv.put(SmarterWifiDBHelper.COL_SSID_SSID, ssid);
        }

        cv.put(SmarterWifiDBHelper.COL_SSID_TIME_S, System.currentTimeMillis() / 1000);

        String compare = SmarterWifiDBHelper.COL_SSID_ID + "=?";
        String args[] = {Long.toString(sid)};

        dataBase.beginTransaction();
        if (sid < 0)
            sid = dataBase.insert(SmarterWifiDBHelper.TABLE_SSID, null, cv);
        else
            dataBase.update(SmarterWifiDBHelper.TABLE_SSID, cv, compare, args);
        dataBase.setTransactionSuccessful();
        dataBase.endTransaction();

        return sid;
    }

    public long getMapId(long sid, long tid) {
        long id = -1;

        final String[] idcol = {SmarterWifiDBHelper.COL_SCMAP_ID};

        String compare = SmarterWifiDBHelper.COL_SCMAP_SSIDID + "=? AND " + SmarterWifiDBHelper.COL_SCMAP_CELLID + "=?";
        String[] args = {Long.toString(sid), Long.toString(tid)};

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_SSID_CELL_MAP, idcol, compare, args, null, null, null);

        c.moveToFirst();

        if (c.getCount() <= 0) {
            c.close();
            return -1;
        }

        id = c.getLong(0);

        c.close();

        return id;
    }

    public SmarterSSID getSsidBlacklisted(String ssid) {
        boolean bl = false;
        long id = -1;

        final String[] idcol = {SmarterWifiDBHelper.COL_SSIDBL_ID, SmarterWifiDBHelper.COL_SSIDBL_BLACKLIST};

        String compare = SmarterWifiDBHelper.COL_SSIDBL_SSID + "=?";
        String[] args = {ssid};

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_SSID_BLACKLIST, idcol, compare, args, null, null, null);

        c.moveToFirst();

        if (c.getCount() > 0) {
            id = c.getLong(0);
            bl = c.getInt(1) == 1;
        }

        c.close();

        return new SmarterSSID(ssid, bl, id);
    }

    public void setSsidBlacklisted(SmarterSSID e, boolean b) {
        if (e == null)
            return;

        // Log.d("smarter", "Blacklisting " + e.getSsid() + " in database: " + b);

        ContentValues cv = new ContentValues();
        cv.put(SmarterWifiDBHelper.COL_SSIDBL_BLACKLIST, b ? "1" : "0");

        dataBase.beginTransaction();
        if (e.getBlacklistDatabaseId() >= 0) {
            if (e.getBlacklistDatabaseId() >= 0) {
                String compare = SmarterWifiDBHelper.COL_SSIDBL_ID + "=?";
                String[] args = {Long.toString(e.getBlacklistDatabaseId())};

                dataBase.update(SmarterWifiDBHelper.TABLE_SSID_BLACKLIST, cv, compare, args);
            }

            // Log.d("smarter", "Blacklist entry updated in db");
        } else {
            cv.put(SmarterWifiDBHelper.COL_SSIDBL_SSID, e.getSsid());

            long sid = dataBase.insert(SmarterWifiDBHelper.TABLE_SSID_BLACKLIST, null, cv);

            e.setBlacklistDatabaseId(sid);

            // Log.d("smarter", "Blacklist entry added to db");
        }
        dataBase.setTransactionSuccessful();
        dataBase.endTransaction();

        e.setBlacklisted(b);
    }

    public SmarterBluetooth getBluetoothBlacklisted(BluetoothDevice dev) {
        boolean bl = false, en = false;
        long id = -1;

        final String[] idcol = {SmarterWifiDBHelper.COL_BTBL_ID, SmarterWifiDBHelper.COL_BTBL_BLACKLIST, SmarterWifiDBHelper.COL_BTBL_ENABLE};

        String compare = SmarterWifiDBHelper.COL_BTBL_MAC + "=?";
        String[] args = {dev.getAddress()};

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_BT_BLACKLIST, idcol, compare, args, null, null, null);

        c.moveToFirst();

        if (c.getCount() > 0) {
            id = c.getLong(0);
            bl = c.getInt(1) == 1;
            en = c.getInt(2) == 1;
        }

        c.close();

        return new SmarterBluetooth(dev.getAddress(), dev.getName(), bl, en, id);
    }

    public void setBluetoothBlacklisted(SmarterBluetooth e, boolean blacklist, boolean enable) {
        if (e == null)
            return;

        // Log.d("smarter", "Blacklisting bluetooth " + e.getBtmac() + " in database: " + blacklist);

        ContentValues cv = new ContentValues();
        cv.put(SmarterWifiDBHelper.COL_BTBL_BLACKLIST, blacklist ? "1" : "0");
        cv.put(SmarterWifiDBHelper.COL_BTBL_ENABLE, enable ? "1" : "0");

        dataBase.beginTransaction();
        if (e.getBlacklistDatabaseId() >= 0) {
            if (e.getBlacklistDatabaseId() >= 0) {
                String compare = SmarterWifiDBHelper.COL_BTBL_ID + "=?";
                String[] args = {Long.toString(e.getBlacklistDatabaseId())};

                dataBase.update(SmarterWifiDBHelper.TABLE_BT_BLACKLIST, cv, compare, args);
            }

            // Log.d("smarter", "Bluetooth blacklist entry updated in db");
        } else {
            cv.put(SmarterWifiDBHelper.COL_BTBL_MAC, e.getBtmac());
            cv.put(SmarterWifiDBHelper.COL_BTBL_NAME, e.getBtName());

            long sid = dataBase.insert(SmarterWifiDBHelper.TABLE_BT_BLACKLIST, null, cv);

            e.setBlacklistDatabaseId(sid);

            // Log.d("smarter", "Bluetooth blacklist entry added to db");
        }
        dataBase.setTransactionSuccessful();
        dataBase.endTransaction();

        e.setBlacklisted(blacklist);
        e.setEnabled(enable);
    }

    public void mapTower(SmarterSSID ssid, long towerid) {
        if (ssid == null)
            return;

        long sid = getSsidDbId(ssid.getSsid());
        long tid = getTowerDbId(towerid);

        sid = updateSsid(ssid.getSsid(), sid);
        tid = updateTower(towerid, tid);

        // Log.d("smarter", "got sid " + sid + " tid " + tid);

        long mid = getMapId(sid, tid);

        ContentValues cv = new ContentValues();

        if (mid < 0) {
            cv.put(SmarterWifiDBHelper.COL_SCMAP_CELLID, tid);
            cv.put(SmarterWifiDBHelper.COL_SCMAP_SSIDID, sid);
        }

        cv.put(SmarterWifiDBHelper.COL_SCMAP_TIME_S, System.currentTimeMillis() / 1000);

        String compare = SmarterWifiDBHelper.COL_SCMAP_SSIDID + "=? AND " + SmarterWifiDBHelper.COL_SCMAP_CELLID + "=?";
        String[] args = {Long.toString(sid), Long.toString(tid)};

        dataBase.beginTransaction();
        if (mid < 0) {
            // Log.d("smarter", "Update tower/ssid map for " + towerid + " / " + ssid);
            dataBase.insert(SmarterWifiDBHelper.TABLE_SSID_CELL_MAP, null, cv);
        } else {
            // Log.d("smarter", "Mapping tower " + towerid + " to ssid " + ssid);
            dataBase.update(SmarterWifiDBHelper.TABLE_SSID_CELL_MAP, cv, compare, args);
        }
        dataBase.setTransactionSuccessful();
        dataBase.endTransaction();

    }

    public SmarterSSID getMappedSsidFromBlacklist(SmarterSSID bl) {
        String[] cols = {SmarterWifiDBHelper.COL_SSID_ID, SmarterWifiDBHelper.COL_SSID_SSID};

        String compare = SmarterWifiDBHelper.COL_SSID_SSID + "=?";
        String[] args = {bl.getSsid()};

        Cursor ssidc = dataBase.query(SmarterWifiDBHelper.TABLE_SSID, cols, compare, args, null, null, null);

        ssidc.moveToFirst();

        if (ssidc.getCount() <= 0) {
            ssidc.close();
            return null;
        }

        SmarterSSID s = new SmarterSSID();

        s.setMapDbId(ssidc.getLong(0));
        s.setSsid(ssidc.getString(1));

        return s;
    }

    public int getNumTowersInSsid(long ssidid) {
        String[] cols = {SmarterWifiDBHelper.COL_SCMAP_CELLID};

        String compare = SmarterWifiDBHelper.COL_SCMAP_SSIDID + "=?";
        String[] args= {Long.toString(ssidid)};

        Cursor c = dataBase.query(SmarterWifiDBHelper.TABLE_SSID_CELL_MAP, cols, compare, args, null, null, null);

        c.moveToFirst();

        int rc = c.getCount();

        c.close();

        return rc;
    }

    public ArrayList<SmarterSSID> getMappedSSIDList() {
        ArrayList<SmarterSSID> retlist = new ArrayList<SmarterSSID>();

        String[] cols = {SmarterWifiDBHelper.COL_SSID_ID, SmarterWifiDBHelper.COL_SSID_SSID};

        Cursor ssidc = dataBase.query(SmarterWifiDBHelper.TABLE_SSID, cols, null, null, null, null, null);

        ssidc.moveToFirst();

        if (ssidc.getCount() <= 0) {
            // Log.d("smarter", "ssidc < 0 nothing in SSID table?");
            ssidc.close();
            return retlist;
        }

        while (!ssidc.isAfterLast()) {
            SmarterSSID s = new SmarterSSID();

            s.setMapDbId(ssidc.getLong(0));
            s.setSsid(ssidc.getString(1));

            s.setNumTowers(getNumTowersInSsid(s.getMapDbId()));

            // Log.d("smarter", "returning tower " + ssidc.getLong(0) + " " + ssidc.getLong(1) + " num " + s.getNumTowers());

            if (s.getNumTowers() > 0)
                retlist.add(s);

            ssidc.moveToNext();
        }

        ssidc.close();

        return retlist;
    }

    public void deleteSsidTowerMap(SmarterSSID ssid) {
        if (ssid.getMapDbId() < 0) {
            if (!ssid.getDisplaySsid().isEmpty()) {
                ssid = getMappedSsidFromBlacklist(ssid);
            }
        }

        if (ssid == null)
            return;

        String compare = SmarterWifiDBHelper.COL_SCMAP_SSIDID + "=?";
        String[] args = {Long.toString(ssid.getMapDbId())};

        dataBase.delete(SmarterWifiDBHelper.TABLE_SSID_CELL_MAP, compare, args);
    }

}
