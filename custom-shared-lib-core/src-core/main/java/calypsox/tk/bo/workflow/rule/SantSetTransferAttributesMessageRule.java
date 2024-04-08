package calypsox.tk.bo.workflow.rule;


import java.util.Map;
import java.util.Vector;
import calypsox.tk.core.retried.RetriedAction.RetriedActionException;
import calypsox.tk.core.retried.actions.ApplyActionToMessageAttributesRetriedAction;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;


public class SantSetTransferAttributesMessageRule implements WfMessageRule {


    /** Description of the rule. */
    private static final String DESCRIPTION = "Add attributes to the related Transfer";
    /** The number of retries. */
    private static final int NUMBER_OF_RETRIES = 1;
    /** The wait between retries. */
    private static final int WAIT_BETWEEN_RETRIES = 100;
    /** Rule Name. */
    private static final String RULE_NAME = "SantSetTransferAttributes";


    // Constructor
    public SantSetTransferAttributesMessageRule() {
    }


    /**
     * Check.
     *
     * @param wc the wc
     * @param message the message
     * @param oldMessage the old message
     * @param trade the trade
     * @param transfer the transfer
     * @param messages the messages
     * @param dsCon the ds con
     * @param excps the excps
     * @param task the task
     * @param dbCon the db con
     * @param events the events
     * @return true, if successful
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage, final Trade trade, final BOTransfer transfer,
                         final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        return true;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.bo.workflow.WfMessageRule#update(com.calypso.tk.bo. TaskWorkflowConfig,
     * com.calypso.tk.bo.BOMessage, com.calypso.tk.bo.BOMessage, com.calypso.tk.core.Trade,
     * com.calypso.tk.bo.BOTransfer, java.util.Vector, com.calypso.tk.service.DSConnection,
     * java.util.Vector, com.calypso.tk.bo.Task, java.lang.Object, java.util.Vector)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean update(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage, final Trade trade, final BOTransfer xfer,
                          final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        // Get Attribute Name and Attribute Value
        Map<String, String> mapAttrNameValue = WorkflowUtil.getAttributeToValueMap(wc, RULE_NAME);
        if (Util.isEmpty(mapAttrNameValue)) {
            mapAttrNameValue = getAttributeToValueMap(wc);
        }
        try {
            final ApplyActionToMessageAttributesRetriedAction retriedAction = new ApplyActionToMessageAttributesRetriedAction(message.getLongId(), Action.UPDATE, message.getAttributes());
            retriedAction.execute(NUMBER_OF_RETRIES, WAIT_BETWEEN_RETRIES);
        } catch (final RetriedActionException e) {
            Log.error(this, "Error in update method", e);
            final BOException ev = new BOException(message.getLongId(), this.getClass().getName(), e.getMessage());
            ev.setType(BOException.EXCEPTION);
            excps.addElement(ev);
        }
        return true;
    }


    /**
     * Get Transition Comment.
     *
     * @param wc
     * @return
     */
    public Map<String, String> getAttributeToValueMap(final TaskWorkflowConfig wc) {
        Map<String, String> map = null;
        String s = wc.getComment();
        if (!Util.isEmpty(s)) {
            s = s.trim();
            if (s.startsWith("{") || s.endsWith("}")) {
                s = s.substring(1, s.length() - 1);
                map = Util.stringToMap(s);
            }
        }
        return map;
    }


    @Override
    public String getDescription() {
        return DESCRIPTION;
    }


}
