package net.kismetwireless.android.smarterwifimanager;

/**
 * Created by dragorn on 9/18/13.
 */
public class SmarterSSID {
    private String ssid;
    private boolean blacklisted;
    private long dbid;

    public SmarterSSID(String s, boolean b, long db) {
        ssid = s;
        blacklisted = b;
        dbid = db;
    }

    public String getSsid() {
        return ssid;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public long getDatabaseId() {
        return dbid;
    }

    public void setBlacklisted(boolean b) {
        blacklisted = b;
    }

    public void setDatabaseId(long i) {
        dbid = i;
    }

    public boolean equals(SmarterSSID e) {
        return (ssid.equals(e.getSsid()) && blacklisted == e.isBlacklisted());
    }

}
