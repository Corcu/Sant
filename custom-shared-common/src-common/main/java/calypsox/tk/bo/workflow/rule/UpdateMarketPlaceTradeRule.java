package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;


import static calypsox.tk.util.SantBilatTradeUtil.*;


/**
 * Rule that updates the Market Place trade field, based on the domain value UpdateMarketPlace and the trade keyword
 * MurexBilateralCounterparty not empty
 *
 * @author Ruben Garcia
 */
public class UpdateMarketPlaceTradeRule implements WfTradeRule {


    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                         Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Rule that updates the Market Place trade field, based on the domain value UpdateMarketPlace and the " +
                "trade keyword MXElectplatf VOZ.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
                          Vector excps, Task task, Object dbCon, Vector events) {
        int marketPlace;
        if (isBilateralETCMSTrade(dsCon, trade)) {
            marketPlace = getMarketPlaceCode(dsCon, trade);
            if (marketPlace != NO_MARKET_PLACE) {
                trade.setExchangeId(marketPlace);
            } else if (isBilateralETCMSTrade(dsCon, oldTrade)) {
                //Check if trade updated through this rule. If updated remove Market Place.
                marketPlace = getMarketPlaceCode(dsCon, oldTrade);
                if (marketPlace != NO_MARKET_PLACE) {
                    trade.setExchangeId(0);
                }
            }
        } else if (isBilateralETCMSTrade(dsCon, oldTrade)) {
            //Check if trade updated through this rule. If updated remove Market Place.
            marketPlace = getMarketPlaceCode(dsCon, oldTrade);
            if (marketPlace != NO_MARKET_PLACE) {
                trade.setExchangeId(0);
            }
        }

        return true;
    }

}
