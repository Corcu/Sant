package calypsox.tk.bo;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Created by x379335 on 07/05/2020.
 */
public class BOBondHandler extends com.calypso.tk.bo.BOBondHandler{

    public BOBondHandler(){
    }

    @Override
    public void addFeesTransferRules(Trade trade, Vector exceptions, DSConnection dsCon, Vector transferRules) {
        super.addFeesTransferRules(trade, exceptions, dsCon, transferRules);
    }

    public Vector generateTransferRules(Trade trade, Product inst, Vector exceptions, DSConnection dsCon){
        Vector vect =super.generateTransferRules(trade,inst,exceptions,dsCon);

        if (trade.getTradeCurrency().equals(trade.getSettleCurrency())) {
            for (int i = 0; i < vect.size(); ++i) {
                TradeTransferRule transfer = (TradeTransferRule) vect.elementAt(i);

                if (isFeeXferType(transfer.getTransferType())) {
                    transfer.setDeliveryType("DFP");
                } else {
                    if ("DAP".equals(trade.getKeywords().get("DeliveryType"))) {
                        transfer.setDeliveryType("DAP");
                    } else if ("DFP".equals(trade.getKeywords().get("DeliveryType"))) {
                        transfer.setDeliveryType("DFP");
                    }
                }
            }
        }
        return vect;
    }

    public Vector generateTransfers(Trade trade, PricingEnv env, Vector exceptions, DSConnection dsCon, JDate today, JDate tradeDate, CashFlowSet cashFlows){
        Vector vect = super.generateTransfers(trade,env,exceptions,dsCon,today,tradeDate,cashFlows);

        if(isBondForward(trade) && !Util.isEmpty(vect)){
            vect = Arrays.stream(vect.toArray()).filter(BOTransfer.class::isInstance).map(BOTransfer.class::cast)
                    .filter(this::filterXfersByType).collect(Collectors.toCollection(Vector::new));
        }

        for (int i = 0; i < vect.size(); ++i) {
            BOTransfer transfer = (BOTransfer) vect.elementAt(i);
            if ("FWD_CASH_FIXING".equalsIgnoreCase(transfer.getTransferType())) {
                transfer.setSettleDate(trade.getSettleDate());
                transfer.setValueDate(trade.getSettleDate());
            }
        }

        if (trade.getTradeCurrency().equals(trade.getSettleCurrency())) {
            for (int i = 0; i < vect.size(); ++i) {
                BOTransfer transfer = (BOTransfer) vect.elementAt(i);

                if (isFeeXferType(transfer.getTransferType())) {
                    transfer.setDeliveryType("DFP");
                } else {
                    if ("DAP".equals(trade.getKeywords().get("DeliveryType"))) {
                        transfer.setDeliveryType("DAP");
                    } else if ("DFP".equals(trade.getKeywords().get("DeliveryType"))) {
                        transfer.setDeliveryType("DFP");
                    }
                }
            }
        }
        return vect;
    }

    @Override
    public void setDAPFlags(Trade trade, Vector rules, DSConnection dsCon){
    }

    /**
     * Return True on BondForwardType : Cash and BondForward : true
     * @param trade
     * @return
     */
    private boolean isBondForward(Trade trade){
        return Optional.ofNullable(trade).filter(t -> "true".equalsIgnoreCase(t.getKeywordValue("BondForward")) && "Cash".equalsIgnoreCase(t.getKeywordValue("BondForwardType"))).isPresent();
    }

    /**
     * Xfer to filter add new if needed
     */
    enum XfersToFilter { SECURITY, PRINCIPAL};

    /**
     * Remove all xfer by XferTypes
     * @param xfer
     * @return
     */
    private boolean filterXfersByType(BOTransfer xfer){
        return Arrays.stream(XfersToFilter.values()).noneMatch( v -> v.name().equalsIgnoreCase(Optional.ofNullable(xfer).map(BOTransfer::getTransferType).orElse("")));
    }

    /**
     * @param xferType
     * @return
     */
    private boolean isFeeXferType(String xferType){
        return LocalCache.getFeeTypes().stream().anyMatch(xferType::equalsIgnoreCase);
    }


}
