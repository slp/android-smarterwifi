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
public class SmarterBluetooth {
    private String btmac;
    private boolean blacklisted;
    private long bldbid;

    public SmarterBluetooth() {
        bldbid = -1;
    }

    public SmarterBluetooth(String btmac, boolean blacklisted, long bldb) {
        this.btmac = btmac;
        this.blacklisted = blacklisted;
        bldbid = bldb;
    }

    public void setBtmac(String s) {
        btmac = s;
    }

    public String getBtmac() {
        return btmac;
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

    public boolean equals(SmarterBluetooth e) {
        return (btmac.equals(e.getBtmac()) && blacklisted == e.isBlacklisted());
    }

}
