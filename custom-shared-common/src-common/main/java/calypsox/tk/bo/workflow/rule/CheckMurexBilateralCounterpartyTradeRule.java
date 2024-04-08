package calypsox.tk.bo.workflow.rule;

import calypsox.tk.util.SantBilatTradeUtil;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * It checks if for a bilateral trade of ETCMS (counterparty in domain value UpdateMarketPlace) and
 * MxElectplatf VOZ, the KW MurexBilateralCounterparty is NOT empty. If the KW MurexBialteralCounterparty
 * is empty, it raises an EX_GATEWAYMSG_ERROR task.
 *
 * @author Ruben Garcia
 */
public class CheckMurexBilateralCounterpartyTradeRule implements WfTradeRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (SantBilatTradeUtil.isBilateralETCMSTrade(dsCon, trade) && Util.isEmpty(trade.getKeywordValue(SantBilatTradeUtil.MUREX_BILATERAL_COUNTERPARTY_KW))) {
            System.err.println(this.getClass().getName());
            BOException boExcp = new BOException(trade.getLongId(), "CheckMurexBilateralCounterpartyTradeRule",
                    "Trade with ctpy " + trade.getCounterParty().getCode() +
                    " and " + SantBilatTradeUtil.Mx_Electplatf  + " KW ["
                            + trade.getKeywordValue(SantBilatTradeUtil.Mx_Electplatf) + "] has an empty "
                            + SantBilatTradeUtil.MUREX_BILATERAL_COUNTERPARTY_KW + " KW value.", BOException.INFORMATION);
            boExcp.setComment( "Trade with ctpy " + trade.getCounterParty().getCode() +
                    " and " + SantBilatTradeUtil.Mx_Electplatf  + " KW ["
                    + trade.getKeywordValue(SantBilatTradeUtil.Mx_Electplatf) + "] has an empty "
                    + SantBilatTradeUtil.MUREX_BILATERAL_COUNTERPARTY_KW + " KW value.");
            if(excps == null){
                excps = new Vector<>();
            }
            excps.addElement(boExcp);
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "It checks if for a bilateral trade of ETCMS (counterparty in domain value UpdateMarketPlace) and " +
                "MxElectplatf VOZ, the KW MurexBilateralCounterparty is NOT empty. If the KW MurexBialteralCounterparty" +
                " is empty, it raises an EX_INFORMATION task.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}
