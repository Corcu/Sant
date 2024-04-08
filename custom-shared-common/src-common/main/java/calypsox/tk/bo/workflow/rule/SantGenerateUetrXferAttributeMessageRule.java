package calypsox.tk.bo.workflow.rule;


import calypsox.tk.core.KeywordConstantsUtil;
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
import java.util.UUID;
import java.util.Vector;


public class SantGenerateUetrXferAttributeMessageRule extends UpdateTransferMessageRule {


    /**
     * Description of the rule
     */
    protected static final String DESCRIPTION = "Generate the UETR Attribute to the Transfer with a UUID code.";
    /** Rule Name. */
    private static final String RULE_NAME = "SantGenerateUetrXferAttribute";
    protected static final String XFER_ATTR_UETR = KeywordConstantsUtil.TRANSFER_ATTRIBUTE_UETR;
    private static final String MAPPING_XFER_STATUS = "SantGenerateUetrXferAttributeFilterXferStatus";
    private boolean isFilteredXfer = false;


    // Constructor
    public SantGenerateUetrXferAttributeMessageRule() {
        super();
        ruleName = RULE_NAME;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        // Filter Transfer
        final BOTransfer selectedTransfer = getTransfer(wc, message, oldMessage, trade, transfer, messages, dsCon, excps,
                task, dbCon, events);
        if (filterTransfer(selectedTransfer, wc, dsCon)) {
            isFilteredXfer = true;
            return true; // Do not process the Xfer in this MessageRule
        }
        isFilteredXfer = false;
        return super.check(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return (isFilteredXfer) ? isFilteredXfer : super.update(wc, message, oldMessage, trade, transfer, messages, dsCon,
                excps, task, dbCon, events);
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
        final Map<String, String> mapXferAttributes = new HashMap<String, String>();
        // Only generate UUID when the Attribute UETR is not available.
        if (Util.isEmpty(selectedTransfer.getAttribute(XFER_ATTR_UETR))) {
            // Get UUID
            final UUID uuid = UUID.randomUUID();
            // Add attribute
            mapXferAttributes.put(XFER_ATTR_UETR, uuid.toString());
        }
        return mapXferAttributes;
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected boolean checkAttributes(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                                      BOTransfer selectedTransfer, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task,
                                      Object dbCon, Vector events) {
        return true;
    }


    @Override
    public String getDescription() {
        return DESCRIPTION;
    }


    /**
     * Filter Transfers that will not be processed in this MessageRule.
     *
     * @param selectedTransfer
     * @param taskWfConfig
     * @param dsCon
     *
     * @return
     */
    private boolean filterTransfer(final BOTransfer selectedTransfer, final TaskWorkflowConfig taskWfConfig,
                                   final DSConnection dsCon) {
        if (selectedTransfer == null) {
            return false; // Handle exception.
        }
        // Xfer Status
        final String xferStatus = selectedTransfer.getStatus().getStatus().toUpperCase();
        // Get mapping from RuleParams
        final Map<String, String> mapping = getMapping(dsCon, taskWfConfig, ruleName);
        if (mapping != null && !Util.isEmpty(mapping) && !Util.isEmpty(mapping.get(MAPPING_XFER_STATUS))) {
            final String[] filterStatus = mapping.get(MAPPING_XFER_STATUS).split(";");
            for (final String aStatus : filterStatus) {
                if (xferStatus.equalsIgnoreCase(aStatus)) {
                    return true; // Not Process Xfer.
                }
            }
        }
        return false; // Process Xfer
    }


}