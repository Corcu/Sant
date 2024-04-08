package calypsox.tk.bo.workflow.rule;

import calypsox.repoccp.ReconCCPConstants;
import calypsox.tk.util.swiftparser.ccp.BilateralIncomingSwiftMatcher;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.Vector;

/**
 * WFMessageRule UpdateSRIMessageRule Update the SettlementReferenceInstructed in the incoming MT541 and MT543 messages
 *
 * @author Ruben Garcia
 */
public class UpdateSRIMessageRule implements WfMessageRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Update the SettlementReferenceInstructed in the incoming MT541 and MT543 messages";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (dsCon != null && message != null) {
            if (transfer == null && dbCon != null) {
                transfer = BOTransferUtil.getTransfer(message, dsCon, dbCon);
            }

            String ref = message.getAttribute("Common_Reference");
            String eccProposal = message.getAttribute(BilateralIncomingSwiftMatcher.IS_NETTING_ECC_PROPOSAL);

            if (transfer != null && !Util.isEmpty(ref) && !Util.isEmpty(eccProposal) && "true".equals(eccProposal)) {
                try {
                    dsCon.getRemoteBackOffice().saveTransferAttribute(transfer.getLongId(),
                            ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST, ref);
                } catch (CalypsoServiceException e) {
                    Log.error(this, e);
                    return true;
                }
            }
        }
        return true;
    }
}
