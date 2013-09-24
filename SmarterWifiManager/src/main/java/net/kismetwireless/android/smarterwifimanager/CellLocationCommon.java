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
            // Log.d("smarter", "Looks like a GSM location");
            setGsmLocation((GsmCellLocation) l);
        } else if (l instanceof CdmaCellLocation) {
            setCdmaLocation((CdmaCellLocation) l);
        }
    }

    public void setGsmLocation(GsmCellLocation gsm) {
        // Log.d("smarter", "gsm lac " + gsm.getLac() + " cid " + gsm.getCid() + " psc " + gsm.getPsc());

        if (gsm.getLac() < 0 || gsm.getCid() < 0) {
            // Log.d("smarter", "lac or cid negative, discarding");
            valid = false;
        }

        // Combine lac and cid for track purposes
        towerId = ((long) gsm.getLac() << 32) + (long) gsm.getCid();

        // Log.d("smarter", "towerid " + towerId);
    }

    public CellLocationCommon(GsmCellLocation gsm) {
        setGsmLocation(gsm);
    }

    public void setCdmaLocation(CdmaCellLocation cdma) {
        if (cdma.getNetworkId() < 0 || cdma.getSystemId() < 0 || cdma.getBaseStationId() < 0)
            valid = false;

        // Network 16 bit, system 15bit, basestation 16 bit
        towerId = ((long) cdma.getNetworkId() << 32) + ((long) cdma.getSystemId() << 16) + (long) cdma.getBaseStationId();

        // Don't track BSID, it changes a lot w/ no real extra data
        // towerId = (cdma.getNetworkId() << 32) + (cdma.getSystemId() << 16);
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
