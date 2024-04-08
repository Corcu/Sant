/**
 *
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.collateral.util.SantMarginCallUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aela
 *
 */
@SuppressWarnings("rawtypes")
public class SantUpdateMarginCallConfigMessageRule implements WfMessageRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                         Vector events) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
                          Vector events) {
        MarginCallEntryDTO entry = null;
        if (message != null) {
            try {
                entry = SantMarginCallUtil.getMarginCallEntryDTO(message, dsCon);
                if (entry != null) {
                    ServiceRegistry.getDefault(dsCon).getCollateralServer()
                            .save(entry, "UPDATE_TO_SENT", TimeZone.getDefault());
                }
            } catch (Exception e) {
                messages.add("Cannot get the MarginCallEnry from the message " + message.getLongId());
                Log.error(this, e);//Sonar
                return false;
            }
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Update the margin call entry status to sent";
    }
}
