package net.kismetwireless.android.smarterwifimanager;

import android.telephony.CellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

/**
 * Created by dragorn on 8/30/13.
 *
 * Mangle various cell location types
 */
public class CellLocationCommon {
    private long towerId;
    private boolean valid = true;

    public CellLocationCommon(CellLocation l) {
        if (l == null) {
            valid = false;
        } else if (l instanceof GsmCellLocation) {
            // LogAlias.d("smarter", "Looks like a GSM location");
            setGsmLocation((GsmCellLocation) l);
        } else if (l instanceof CdmaCellLocation) {
            setCdmaLocation((CdmaCellLocation) l);
        }
    }

    public void setGsmLocation(GsmCellLocation gsm) {
        // LogAlias.d("smarter", "gsm lac " + gsm.getLac() + " cid " + gsm.getCid() + " psc " + gsm.getPsc());

        if (gsm.getLac() < 0 && gsm.getCid() < 0) {
            LogAlias.d("smarter", "gsm tower lac or cid negative, discarding");
            valid = false;
            towerId = -1;
            return;
        }

        // Combine lac and cid for track purposes
        towerId = ((long) gsm.getLac() << 32) + (long) gsm.getCid();

        if (towerId < 0) {
            LogAlias.d("smarter", "gsm tower problem:  valid tower lac " + gsm.getLac() + " cid " + gsm.getCid() + " but negative result, kluging to positive");
            towerId = Math.abs(towerId);
            valid = true;
        }

        // LogAlias.d("smarter", "towerid " + towerId);
    }

    public CellLocationCommon(GsmCellLocation gsm) {
        setGsmLocation(gsm);
    }

    public void setCdmaLocation(CdmaCellLocation cdma) {
        if (cdma.getNetworkId() < 0 && cdma.getSystemId() < 0 && cdma.getBaseStationId() < 0) {
            LogAlias.d("smarter", "cdma nid/sid/bsid negative, discarding");
            valid = false;
            towerId = -1;
            return;
        }

        // Network 16 bit, system 15bit, basestation 16 bit
        towerId = ((long) cdma.getNetworkId() << 32) + ((long) cdma.getSystemId() << 16) + (long) cdma.getBaseStationId();

        if (towerId < 0) {
            LogAlias.d("smarter", "cdma tower problem:  valid tower nid " + cdma.getNetworkId() + " sid " + cdma.getSystemId() + " bsid " + cdma.getBaseStationId() + " but negative result, kluging to positive");
            towerId = Math.abs(towerId);
            valid = true;
        }

    }

    public CellLocationCommon(CdmaCellLocation cdma) {
        setCdmaLocation(cdma);
    }

    public long getTowerId() {
        if (valid)
            return towerId;

        return -1;
    }

    public boolean equals(CellLocationCommon c) {
        return (c.getTowerId() == getTowerId());
    }

}
