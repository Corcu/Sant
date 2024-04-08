package calypsox.tk.bo.workflow.rule;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubFormatterUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.UpdateTransferMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class SantSetMsgTrnInXferAttrMessageRule extends UpdateTransferMessageRule {


    /** Description of the rule */
    protected static final String DESCRIPTION = "Set the messageTrn Attribute associted to the message.";
    /** Rule Name. */
    private static final String RULE_NAME = "SantSetMsgTrnInXferAttrMessageRule";


    // Constructor
    public SantSetMsgTrnInXferAttrMessageRule() {
        super();
        ruleName = RULE_NAME;
    }


    @Override
    public String getDescription() {
        return DESCRIPTION;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return super.check(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected boolean updateTransfer(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage,
                                     final Trade trade, final BOTransfer selectedTransfer, final BOTransfer transfer, final Vector messages,
                                     final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        boolean isXferUpdated = false;
        // Get Map Attribute Name/Value
        final Map<String, String> map = getAttributeToKeywordMap(wc, message, oldMessage, trade, selectedTransfer,
                transfer, messages, dsCon, excps, task, dbCon, events);
        // if not empty, update transfer
        if (!Util.isEmpty(map)) {
            map.forEach((attrName, attrValue) -> selectedTransfer.setAttribute(attrName, attrValue));
            isXferUpdated = true;
        }
        return isXferUpdated;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Map<String, String> getAttributeToKeywordMap(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage, final Trade trade,
                                                        final BOTransfer selectedTransfer, final BOTransfer transfer, final Vector messages,
                                                        final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        final Map<String, String> mapXferAttributes = new HashMap<String, String>();
        mapXferAttributes.put(PHConstants.XFER_ATTR_PH_MESSAGE_TRN, PaymentsHubFormatterUtil.buildTRN(message,trade, transfer));
        return mapXferAttributes;
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected boolean checkAttributes(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer selectedTransfer,
                                      BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }


}