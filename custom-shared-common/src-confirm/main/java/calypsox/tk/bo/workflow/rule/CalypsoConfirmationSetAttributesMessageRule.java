package calypsox.tk.bo.workflow.rule;

import calypsox.tk.confirmation.handler.CalypsoConfirmationEventHandler;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CalypsoConfirmationSetAttributesMessageRule implements WfMessageRule {

    private static final String CANCEL_PREFIX="C";
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "";
    }

    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String mxLastEvent= Optional.ofNullable(trade.getKeywordValue(CalypsoConfirmationEventHandler.KEYWORD_MX_LAST_EVENT))
              .orElse("");
        String mxContractId= Optional.ofNullable(trade.getKeywordValue(CalypsoConfirmationEventHandler.KEYWORD_MX_CONTRACT_ID))
                .orElse("");
        String mxRootContractId=Optional.ofNullable(trade.getExternalReference())
                .orElse(trade.getKeywordValue(CalypsoConfirmationEventHandler.KEYWORD_MX_ROOT_CONTRACT_ID));
        message.setAttribute(CalypsoConfirmationEventHandler.KEYWORD_MX_LAST_EVENT,mxLastEvent);
        message.setAttribute(CalypsoConfirmationEventHandler.KEYWORD_MX_CONTRACT_ID,mxContractId);
        message.setAttribute(CalypsoConfirmationEventHandler.KEYWORD_MX_ROOT_CONTRACT_ID,cropCancelPrefix(mxRootContractId));
        return true;
    }
    private String cropCancelPrefix(String rootContractId){
        return Optional.ofNullable(rootContractId)
                .filter(id->id.startsWith(CANCEL_PREFIX))
                .map(id->id.replace(CANCEL_PREFIX,""))
                .orElse(rootContractId);
    }
}
