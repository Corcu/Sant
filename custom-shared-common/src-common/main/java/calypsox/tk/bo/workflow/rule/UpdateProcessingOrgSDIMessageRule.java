package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.util.MessageSDIUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * UpdateProcessingOrgSDIMessageRule class
 * Add message attribute ProcessingOrgSDIID in bilateral trade (MurexBilateralCounterparty not empty)
 * SDI ID from native filter and custom filter Agent;Intermediary;Intermediary2
 */
public class UpdateProcessingOrgSDIMessageRule implements WfMessageRule {


    /**
     * The ProcessingSDIID message attribute
     */
    public static final String PO_SDI_ID = "ProcessingOrgSDIID";


    /**
     * The CustomSettlementDetailIteratorSDIFilter domain name
     * Counterparty;ProductType
     * Agent;Intermediary;Intermediary2
     */
    private static final String FILTER_PO_SDI = "UpdateProcessingOrgSDIMessageRuleFilter";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (trade != null && trade.getBook() != null && trade.getBook().getLegalEntity() != null && message != null && dsCon != null && !Util.isEmpty(message.getTemplateName()) &&
                ("MT541BILAT".equalsIgnoreCase(message.getTemplateName()) ||
                        "MT543BILAT".equalsIgnoreCase(message.getTemplateName())
                        || "MT515BILAT".equalsIgnoreCase(message.getTemplateName()))) {
            return !Util.isEmpty(MessageSDIUtil.getSDIByCustomFilter(trade, transfer, dsCon, messages, trade.getBook().getLegalEntity().getCode(), "ProcessingOrg", FILTER_PO_SDI));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Rule that updates the ProcessingOrgSDIID message attribute with the ID of the SDI.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (trade != null && trade.getBook() != null && trade.getBook().getLegalEntity() != null && message != null && dsCon != null && !Util.isEmpty(message.getTemplateName()) &&
                ("MT541BILAT".equalsIgnoreCase(message.getTemplateName()) ||
                        "MT543BILAT".equalsIgnoreCase(message.getTemplateName())
                        || "MT515BILAT".equalsIgnoreCase(message.getTemplateName()))) {
            String sdiId = MessageSDIUtil.getSDIByCustomFilter(trade, transfer, dsCon, messages, trade.getBook().getLegalEntity().getCode(), "ProcessingOrg", FILTER_PO_SDI);
            if (!Util.isEmpty(sdiId)) {
                message.setAttribute(PO_SDI_ID, sdiId);
                return true;
            }
            return false;
        }
        return true;
    }

}
