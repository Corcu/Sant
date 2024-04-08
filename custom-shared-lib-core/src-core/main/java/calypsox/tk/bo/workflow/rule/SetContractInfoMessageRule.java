/**
 *
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.MarginCallMessageFormatter;
import calypsox.util.MarginCallConstants;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Vector;

/**
 * @author aela
 *
 */
@SuppressWarnings("rawtypes")
public class SetContractInfoMessageRule implements WfMessageRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "This rule will set some information about the margin call contract on the generated message";
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                          Vector events) {

        CollateralConfig mcc = null;
        try {
            mcc = CacheCollateralClient.getCollateralConfig(dsCon, message.getStatementId());
        } catch (Exception e) {
            Log.error(this, e);
            messages.add("Unable to get the margin call detail for message" + message.getLongId());
        }
        // set contract info
        if (mcc != null) {
            message.setAttribute(MarginCallConstants.MESSAGE_ATTR_MCC_ID, Util.numberToString(mcc.getId()));
            message.setAttribute(MarginCallConstants.MESSAGE_ATTR_MCC_NAME, mcc.getName());
            message.setAttribute(MarginCallConstants.MC_VALIDATION,
                    mcc.getAdditionalField(MarginCallConstants.MC_VALIDATION));

        }
        // set notification type
        message.setAttribute(MarginCallConstants.MESSAGE_ATTR_NOTIF_TYPE,
                MarginCallMessageFormatter.getTitleFromTemplateName(message, dsCon));
        return true;
    }

}
