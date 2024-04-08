package calypsox.tk.bo;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * Created by x379335 on 07/05/2020.
 */
public class BOEquityHandler extends com.calypso.tk.bo.BOEquityHandler{

    public BOEquityHandler(){
    }

    public Vector generateTransferRules(Trade trade, Product inst, Vector exceptions, DSConnection dsCon){
        Vector vect =super.generateTransferRules(trade,inst,exceptions,dsCon);

        if (trade.getTradeCurrency().equals(trade.getSettleCurrency())) {
            for (int i = 0; i < vect.size(); ++i) {
                TradeTransferRule transfer = (TradeTransferRule) vect.elementAt(i);

                if ("ADDITIONAL_FEE".equalsIgnoreCase(transfer.getTransferType())) {
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
        Vector vect =super.generateTransfers(trade,env,exceptions,dsCon,today,tradeDate,cashFlows);

        if (trade.getTradeCurrency().equals(trade.getSettleCurrency())) {
            for (int i = 0; i < vect.size(); ++i) {
                BOTransfer transfer = (BOTransfer) vect.elementAt(i);

                if ("ADDITIONAL_FEE".equalsIgnoreCase(transfer.getTransferType())) {
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

}
