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
 * UpdateCounterPartySDIMessageRule class
 * Add message attribute CounterPartySDIID in bilateral trade (MurexBilateralCounterparty not empty)
 * SDI ID from native filter and custom filter Agent;Intermediary;Intermediary2
 */
public class UpdateCounterPartySDIMessageRule implements WfMessageRule {

    /**
     * Trade MurexBilateralCounterparty keyword
     */
    public static final String MX_BILT_CTPY = "MurexBilateralCounterparty";

    /**
     * The CounterPartySDIID message attribute
     */
    public static final String CTPY_SDI_ID = "CounterPartySDIID";


    /**
     * The CustomSettlementDetailIteratorSDIFilter domain name
     * Counterparty;ProductType
     * Agent;Intermediary;Intermediary2
     */
    private static final String FILTER_CTPY_SDI = "CustomSettlementDetailIteratorSDIFilter";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (trade != null && message != null && dsCon != null && !Util.isEmpty(message.getTemplateName()) &&
                ("MT541BILAT".equalsIgnoreCase(message.getTemplateName()) ||
                        "MT543BILAT".equalsIgnoreCase(message.getTemplateName())
                        || "MT515BILAT".equalsIgnoreCase(message.getTemplateName()))) {
            if(Util.isEmpty(trade.getKeywordValue(MX_BILT_CTPY))){
                messages.add("The trade [" + trade.getLongId() + "] has an empty " + MX_BILT_CTPY + " keyword. Cannot select Counterparty SDI.");
                return false;
            }
            return !Util.isEmpty(MessageSDIUtil.getSDIByCustomFilter(trade, transfer, dsCon, messages, trade.getKeywordValue(MX_BILT_CTPY), "CounterParty", FILTER_CTPY_SDI));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Rule that updates the CounterPartySDIID message attribute with the ID of the SDI associated with the MurexBilateralCounterParty KW.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (trade != null && message != null && dsCon != null && !Util.isEmpty(message.getTemplateName()) &&
                ("MT541BILAT".equalsIgnoreCase(message.getTemplateName()) ||
                        "MT543BILAT".equalsIgnoreCase(message.getTemplateName())
                        || "MT515BILAT".equalsIgnoreCase(message.getTemplateName()))) {
            String sdiId = MessageSDIUtil.getSDIByCustomFilter(trade, transfer, dsCon, messages, trade.getKeywordValue(MX_BILT_CTPY), "CounterParty", FILTER_CTPY_SDI);
            if (!Util.isEmpty(sdiId)) {
                message.setAttribute(CTPY_SDI_ID, sdiId);
                return true;
            }
            return false;
        }
        return true;
    }

}
