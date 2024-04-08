package calypsox.tk.bo.workflow.rule;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.Vector;

public class RepoSMMDUpdatePTITradeRule implements WfTradeRule {

    private static final String[] ACTIONS = { "MANUAL_ACCEPT","ACCEPT","REPRICE","RERATE","PARTIAL_RETURN"};

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Set keyword SMMDUpdatePTI";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        JDateFormat format = new JDateFormat("ddMMyyyy");
        JDate now = JDate.getNow();
        String eventDate = format.format(now);
        Action possibleAction = wc.getPossibleAction();

        String pti = getPrtryTradeid(trade);
        if(Arrays.asList(ACTIONS).contains(possibleAction.toString()) ||
                ("AMEND".equalsIgnoreCase(possibleAction.toString())
                        && "mxContractEventIRESTRUCTURE".equalsIgnoreCase(trade.getKeywordValue("MxLastEvent")))){
            trade.addKeyword("SMMDUpdatePTI",pti+eventDate);
        }

        return true;
    }

    public String getPrtryTradeid(Trade trade){
        StringBuilder prtryId = new StringBuilder();
        if(null!=trade){
            String po =  trade.getBook().getLegalEntity().getCode();
            if("BSTE".equalsIgnoreCase(po)){
                prtryId.append("0049");
            }else if("BDSD".equalsIgnoreCase(po)){
                prtryId.append("0306");
            }
            String murexRootContract = trade.getKeywordValue("MurexRootContract");
            prtryId.append(murexRootContract);
        }
        return prtryId.toString();
    }
}
