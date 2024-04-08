package calypsox.tk.csdr;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class CSDRXferPSETHandler {

    public String getPSETBIC(BOTransfer transfer, Trade trade){
        String psetBIC="";
        SettleDeliveryInstruction externalSDI= BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(),transfer.getExternalSettleDeliveryId());
        if(externalSDI!=null) {
            int psetId=externalSDI.getIntermediary2Id();
            String psetContactType=externalSDI.getIntermediary2ContactType();
            if(psetId==0) {
                if(externalSDI.getIntermediaryId()!=0) {
                    psetId = externalSDI.getIntermediaryId();
                    psetContactType = externalSDI.getIntermediaryContactType();
                }else{
                    psetId=externalSDI.getAgentId();
                    psetContactType=externalSDI.getAgentContactType();
                }
            }
            psetBIC = SwiftUtil
                    .formatParty("P", psetId, null,"Agent"
                            , psetContactType, transfer.getProductType(), externalSDI.getProcessingOrgBasedId()
                            , trade, transfer, null, DSConnection.getDefault(), null);
        }
        return psetBIC;
    }


    public boolean isElegiblePSET(BOTransfer xfer, Trade trade){
        boolean res=false;
        if(xfer!=null){
            String psetBic= getPSETBIC(xfer,trade);
            res=checkPSETInDV(psetBic);
        }
        return res;
    }

    private boolean checkPSETInDV(String pset){
        boolean res=false;
        Vector<String> enabledPsets= LocalCache.getDomainValues(DSConnection.getDefault(),"EnabledPsetForCSDRPenaltyCalculation");
        for(String enabledPset:enabledPsets){
            res=enabledPset.equalsIgnoreCase(pset);
            if(res){
                break;
            }
        }
        return res;
    }
}
