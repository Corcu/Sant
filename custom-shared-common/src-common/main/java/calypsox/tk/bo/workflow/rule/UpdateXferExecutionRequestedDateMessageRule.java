package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.sql.BOMessageSQL;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.Vector;

import static calypsox.tk.util.swiftparser.MT558MessageProcessor.EXQR;

/**
 * UpdateXferExecutionRequestedDateMessageRule
 * <p>
 * Update transfer attribute Execution Requested Date with MT558 attribute value if transfer Value Date is equals to
 * :98A::EXRQ MT558 value
 *
 * @author Ruben Garcia
 */
public class UpdateXferExecutionRequestedDateMessageRule extends IncomingMT558AbstractMessageRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer,
                         Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Link transfers by Value Date equals to " + EXQR + " message attribute value :98A::EXRQ";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer,
                          Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (message != null && message.getLinkedLongId() > 0
                && !Util.isEmpty(message.getTemplateName()) && "MT558".equals(message.getTemplateName())) {
            if (!Util.isEmpty(message.getAttribute(EXQR))) {
                JDate exqr = Util.istringToJDate(message.getAttribute(EXQR));
                if (exqr != null) {
                    BOMessage outgoing;
                    TransferArray tradeTransfers;
                    try {
                        if (dbCon != null) {
                            outgoing = BOMessageSQL.getMessage(message.getLinkedLongId(), (Connection) dbCon);
                        } else {
                            outgoing = dsCon.getRemoteBO().getMessage(message.getLinkedLongId());
                        }
                        if (outgoing != null && outgoing.getTradeLongId() > 0) {
                            tradeTransfers = loadTradeTransfers(outgoing.getTradeLongId(), dsCon, (Connection) dbCon);
                            if (!Util.isEmpty(tradeTransfers)) {
                                int updated = 0;
                                for (BOTransfer t : tradeTransfers) {
                                    if (isTransferEligibleForUpdate(t) && t.getValueDate() != null && t.getValueDate().equals(exqr)) {
                                        if (dbCon != null) {
                                            BOTransferSQL.saveAttributes(new long[]{t.getLongId()}, EXQR, message.getAttribute(EXQR), (Connection) dbCon);
                                        } else {
                                            dsCon.getRemoteBackOffice().saveTransferAttribute(t.getLongId(), EXQR, message.getAttribute(EXQR));
                                        }
                                        updated++;
                                    }
                                }
                                if (updated == 0) {
                                    messages.add("No transfers have been found for trade ID: " + outgoing.getTradeLongId() +
                                            " whose Valuation Date is equal to " + EXQR + " message date: " + message.getAttribute(EXQR));
                                }
                            } else {
                                messages.add("No transfers found for the trade ID: " + outgoing.getTradeLongId());
                            }
                        }
                    } catch (Exception e) {
                        Log.error(this, e);
                        return false;
                    }
                }
            } else {
                messages.add("Message MT558 does not have the " + EXQR + " attribute reported.");
            }
        }
        return true;
    }
}
