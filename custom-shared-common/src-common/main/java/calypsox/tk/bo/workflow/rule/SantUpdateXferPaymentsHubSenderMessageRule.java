package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.UpdateTransferMessageRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class SantUpdateXferPaymentsHubSenderMessageRule extends UpdateTransferMessageRule {


    /**
     * Description of the rule
     */
    private static final String DESCRIPTION = "Updates attributes and applies action to the Transfer.";

    /** Rule Name. */
    private static final String RULE_NAME = "SantUpdateXferPaymentsHubSender";

    /** 'MsgSentbyPaymentHub' Attribute */
    private static final String XFER_ATTR_MSG_SENT_BY_PAYMENTS_HUB = "MsgSentbyPaymentHub";

    /** 'PH_PaymentSystemResponseHeader' Attribute */
    private static final String MSG_ATTR_PH_PAY_SYST_RESP_HEADER = "PH_PaymentSystemResponseHeader";

    // PaymentsHub
    public static final String PAYMENTS_HUB_SYSTEM = "PAYMENTS_HUB";


    // Constructor
    public SantUpdateXferPaymentsHubSenderMessageRule() {
        super();
        ruleName = RULE_NAME;
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
    public Map<String, String> getAttributeToKeywordMap(final TaskWorkflowConfig wc, final BOMessage message,
                                                        final BOMessage oldMessage, final Trade trade, final BOTransfer selectedTransfer, final BOTransfer transfer,
                                                        final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon,
                                                        final Vector events) {
        // Get BOTransfer Attributes to Update.
        Map<String, String> mapXferAttributes = this.getMapping(dsCon, wc, ruleName);
        if (Util.isEmpty(mapXferAttributes)) {
            mapXferAttributes = new HashMap<String, String>(1);
        }
        // Get 'PH_PaymentSystemResponseHeader' Attribute value
        final String paySystemValue = message.getAttribute(MSG_ATTR_PH_PAY_SYST_RESP_HEADER);
        if (paySystemValue != null) {
            // Add 'MsgSentbyPaymentHub' attribute value
            addMsgSentPaymentHubAttribute(mapXferAttributes, paySystemValue);
        } else {
            final String debug = String.format(
                    "The attribute [%s] is empty or null. It will not be updated into BOTransfer [%s]",
                    MSG_ATTR_PH_PAY_SYST_RESP_HEADER, String.valueOf(transfer.getLongId()));
            Log.debug(SantUpdateXferPaymentsHubSenderMessageRule.class, debug);
        }
        return mapXferAttributes;
    }


    /**
     * Add 'PHSettlementStatus' attribute.
     *
     * @param mapXferAttributes
     * @param xPaymentSystemValue
     */
    private static void addMsgSentPaymentHubAttribute(final Map<String, String> mapXferAttributes,
                                                      final String xPaymentSystemValue) {
        if (!mapXferAttributes.containsKey(XFER_ATTR_MSG_SENT_BY_PAYMENTS_HUB)) {
            final String value = (xPaymentSystemValue != null && (xPaymentSystemValue.trim()
                    .contains(PAYMENTS_HUB_SYSTEM))) ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
            // Add attribute value
            mapXferAttributes.put(XFER_ATTR_MSG_SENT_BY_PAYMENTS_HUB, value);
        }
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected boolean checkAttributes(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                                      BOTransfer selectedTransfer, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task,
                                      Object dbCon, Vector events) {
        return true; // Nothing to check
    }


    @Override
    public String getDescription() {
        return DESCRIPTION;
    }


}
