package calypsox.tk.swift.formatter.xferagent;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class XferAgentSwiftSafeAccHandler {

    private SettleDeliveryInstruction payerSdi;
    private final String sell="SELL";
    private final String buyr="BUYR";

    public XferAgentSwiftSafeAccHandler(BOTransfer xfer, PartySDIInfo partySDIInfo){
        int sdiId = findSDIId(partySDIInfo, xfer);
        if (sdiId > 0) {
            this.payerSdi = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
        }
    }

    private int findSDIId(PartySDIInfo partySDIInfo, BOTransfer xfer) {
        int sdiId = 0;
        if (buyr.equals(partySDIInfo.getIdentifier())) {
            sdiId = xfer.getReceiverSDId();
        } else if (sell.equals(partySDIInfo.getIdentifier())) {
            sdiId = xfer.getPayerSDId();
        }
        return sdiId;
    }

    public String parseSafeKeepAccount(String coreValue){
        String tagValue=coreValue;
        String accSdiAttr="External Account";
        String sdiCustomAcc=Optional.ofNullable(payerSdi).map(sdi->sdi.getAttribute(accSdiAttr))
                    .orElse("");
        if(!Util.isEmpty(sdiCustomAcc)){
            tagValue=":SAFE//"+sdiCustomAcc;
        }
        return tagValue;
    }

}
