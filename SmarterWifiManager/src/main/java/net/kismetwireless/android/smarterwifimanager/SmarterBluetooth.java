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
    private String btmac, btname;
    private boolean blacklisted, enabled;
    private long bldbid;

    public SmarterBluetooth() {
        bldbid = -1;
    }

    public SmarterBluetooth(String btmac, String name, boolean blacklisted, boolean enabled, long bldb) {
        this.btmac = btmac;
        this.blacklisted = blacklisted;
        this.enabled = enabled;
        this.btname = name;
        bldbid = bldb;
    }

    public void setBtName(String s) {
        btname = s;
    }

    public String getBtName() {
        return btname;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e) {
        enabled = e;
    }

    public boolean equals(SmarterBluetooth e) {
        return (btmac.equals(e.getBtmac()) && blacklisted == e.isBlacklisted() && enabled == e.isEnabled());
    }

}
