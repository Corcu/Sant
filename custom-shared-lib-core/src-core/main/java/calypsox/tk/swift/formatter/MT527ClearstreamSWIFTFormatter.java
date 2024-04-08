package calypsox.tk.swift.formatter;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class MT527ClearstreamSWIFTFormatter extends com.calypso.tk.swift.formatter.MT527ClearstreamSWIFTFormatter {


    @Override
    public String parseEXPOSURE_TYPE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        TradeTransferRule tRule = getTradeTransferRule(trade, transferRules, dsCon);
        SettleDeliveryInstruction sd = BOCache.getSettleDeliveryInstruction(dsCon, tRule.getProcessingOrgSDId());
        String ret = sd.getAttribute("TripartyMT527ExposureTypeIndicator");

        if(Util.isEmpty(ret)){
            ret = super.parseEXPOSURE_TYPE(message, trade, sender, rec, transferRules, transfer, dsCon);
        }

        return ret;
    }
}
