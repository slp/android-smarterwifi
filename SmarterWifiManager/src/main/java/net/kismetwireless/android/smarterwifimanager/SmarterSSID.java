package net.kismetwireless.android.smarterwifimanager;

/**
 * Created by dragorn on 9/18/13.
 *
 * Conflation of SSID data for blacklist, etc
 *
 * Slightly bad behavior - if not loaded from blacklist, does not contain valid blacklist data,
 * blacklist SSIDs are stored quotes-buffered because of the wifi API
 *
 */
public class SmarterSSID {
    private String ssid;
    private boolean blacklisted;
    private long bldbid;
    private int numtowers;
    private long mapdbid;

    public SmarterSSID() {
        mapdbid = -1;
        bldbid = -1;
    }

    public SmarterSSID(String ssid, boolean blacklisted, long bldb) {
        this.ssid = ssid;
        this.blacklisted = blacklisted;
        bldbid = bldb;

        mapdbid = -1;
    }

    public SmarterSSID(String ssid, int numtowers, long mapdbid) {
        this.ssid = ssid;
        this.numtowers = numtowers;
        this.mapdbid = mapdbid;

        bldbid = -1;
    }

    public void setSsid(String s) {
        ssid = s;
    }

    public String getSsid() {
        return ssid;
    }

    public String getDisplaySsid() {
        if (ssid.length() > 1) {
           if (ssid.charAt(0) == '\"' && ssid.charAt(ssid.length() - 1) == '\"')
               return ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
    }

    public void setNumTowers(int nt) {
        numtowers = nt;
    }

    public void setMapDbId(long id) {
        mapdbid = id;
    }

    public int getNumTowers() {
        return numtowers;
    }

    public long getMapDbId() {
        return mapdbid;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public long getBlacklistDatabaseId() {
        return bldbid;
    }

    public void setBlacklisted(boolean b) {
        blacklisted = b;
    }

    public void setBlacklistDatabaseId(long i) {
        bldbid = i;
    }

    public boolean equals(SmarterSSID e) {
        return (ssid.equals(e.getSsid()) && blacklisted == e.isBlacklisted());
    }

}
