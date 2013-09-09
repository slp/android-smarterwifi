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
            setGsmLocation((GsmCellLocation) l);
        } else if (l instanceof CdmaCellLocation) {
            setCdmaLocation((CdmaCellLocation) l);
        }
    }

    public void setGsmLocation(GsmCellLocation gsm) {
        if (gsm.getLac() < 0 || gsm.getCid() < 0)
            valid = false;

        // Combine lac and cid for track purposes
        towerId = (gsm.getLac() << 16) + gsm.getCid();
    }

    public CellLocationCommon(GsmCellLocation gsm) {
        setGsmLocation(gsm);
    }

    public void setCdmaLocation(CdmaCellLocation cdma) {
        if (cdma.getNetworkId() < 0 || cdma.getSystemId() < 0 || cdma.getBaseStationId() < 0)
            valid = false;

        // Network 16 bit, system 15bit, basestation 16 bit
        towerId = (cdma.getNetworkId() << 32) + (cdma.getSystemId() << 16) + cdma.getBaseStationId();

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
