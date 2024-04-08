package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class UpdateCSDRSettleDateTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return Optional.ofNullable(trade)
                .map(Trade::getProduct).filter(p->p instanceof SimpleTransfer)
                .map(p-> "PENALTY".equals(((SimpleTransfer) p).getFlowType()))
                .orElse(false);
    }

    @Override
    public String getDescription() {
        return "Gets agent's PenaltySettleDate attribute with DateRule's name to calculate the needed SettleDate";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        int leId=trade.getCounterParty().getId();
        JDate penaltySettleDate=calculateDateFromDateRule(getAgentLEAttr(leId),trade.getTradeDate().getJDate(TimeZone.getDefault()));
        trade.setSettleDate(penaltySettleDate);
        return true;
    }

    private String getAgentLEAttr(int leId){
        LegalEntityAttribute attr=BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, leId, "ALL", "PenaltySettleDate");
        return Optional.ofNullable(attr).map(LegalEntityAttribute::getAttributeValue).orElse("");
    }

    private JDate calculateDateFromDateRule(String dateRuleName, JDate valueDate){
        JDate rolledDate=valueDate;
        DateRule rule=BOCache.getDateRule(DSConnection.getDefault(),dateRuleName);
        if(rule!=null) {
            rolledDate=rule.next(valueDate);
        }
        return rolledDate;
    }
}
