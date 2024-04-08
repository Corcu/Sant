package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.sql.BOMessageSQL;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.*;

import static calypsox.tk.util.swiftparser.MT558MessageProcessor.CINS_VALUE;

/**
 * MatchCinsIncomingTripartyRepoMessageRule update transfers status if MT558 has 70E::CINS not empty
 *
 * @author Ruben Garcia
 */
public class MatchCinsIncomingTripartyRepoMessageRule extends IncomingMT558AbstractMessageRule {

    /**
     * Domian Value that contains the value of the tag 70E::CINS and the action that you want to apply to the transfer
     */
    private static final String DV_MATCH_CINS = "MatchCinsIncomingTripartyRepo";


    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer,
                         Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (message != null) {
            if (Util.isEmpty(message.getTemplateName())) {
                messages.add("Incoming Message has no template defined");
                return false;
            } else if (!message.getMatchingB()) {
                messages.add("Message not Matched");
                return false;
            } else if (message.getLinkedLongId() == 0L) {
                messages.add("Incoming Message is not linked to an outgoing Calypso Message Id");
                return false;
            } else if (!message.getTemplateName().equals("MT558")) {
                messages.add("Incoming Message is not MT558");
                return false;
            } else if (Util.isEmpty(message.getAttribute(CINS_VALUE))) {
                messages.add("Incoming Message has " + CINS_VALUE + " attribute empty");
                return false;
            } else if (Util.isEmpty(LocalCache.getDomainValueComment(DSConnection.getDefault(), DV_MATCH_CINS,
                    message.getAttribute(CINS_VALUE)))) {
                messages.add("No action in Domain Value " + DV_MATCH_CINS + " for " + CINS_VALUE + " attribute value: " +
                        message.getAttribute(CINS_VALUE));
                return false;
            }else{
                try {
                    BOMessage outgoing;
                    if (dbCon != null) {
                        outgoing = BOMessageSQL.getMessage(message.getLinkedLongId(), (Connection) dbCon);
                    } else {
                        outgoing = dsCon.getRemoteBO().getMessage(message.getLinkedLongId());
                    }

                    if (outgoing == null) {
                        messages.add("Cannot find Message " + message.getLinkedLongId());
                        return false;
                    }

                    if (outgoing.getTradeLongId() <= 0) {
                        messages.add("Outgoing message  " + message.getLongId() + " does not have a linked trade.");
                        return false;
                    }
                    Trade linkedTrade;
                    if (dbCon != null) {
                        linkedTrade = TradeSQL.getTrade(outgoing.getTradeLongId());
                    } else {
                        linkedTrade = dsCon.getRemoteTrade().getTrade(outgoing.getTradeLongId());
                    }

                    if (linkedTrade == null) {
                        messages.add("Cannot find Trade " + outgoing.getTradeLongId());
                        return false;
                    }

                    TransferArray tradeTransfers = loadTradeTransfers(outgoing.getTradeLongId(), dsCon, (Connection) dbCon);


                    if(Util.isEmpty(tradeTransfers)) {
                        messages.add("Cannot find transfers for update: " + outgoing.getTradeLongId());
                        return false;
                    }else{
                        String actionS = LocalCache.getDomainValueComment(DSConnection.getDefault(), DV_MATCH_CINS,
                                message.getAttribute(CINS_VALUE));
                        Action action = Action.valueOf(actionS);
                        if(action == null){
                            messages.add("Invalid action " + actionS);
                            return false;
                        }
                        for(BOTransfer t: tradeTransfers){
                            BOTransfer clonedXfer = (BOTransfer) t.clone();
                            if(isTransferEligibleForUpdate(clonedXfer) &&
                                    BOTransferWorkflow.isTransferActionApplicable(clonedXfer, linkedTrade,
                                            action, dsCon, dbCon)){
                                return true;
                            }
                        }
                        messages.add("There are no transfers in the trade " + outgoing.getTradeLongId() + " to apply the action " + actionS);
                    }
                    return false;

                } catch (Exception e) {
                    Log.error(this, e);
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Checks the " + CINS_VALUE + " message attribute and according to the mapping in the Domain Value" + DV_MATCH_CINS +
                "  applies the action on the transfers (CINS_VALUE/TRANSFER_ACTION).";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer,
                          Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (message != null && message.getLinkedLongId() > 0 && !Util.isEmpty(message.getAttribute(CINS_VALUE))
                && !Util.isEmpty(message.getTemplateName()) && "MT558".equals(message.getTemplateName())) {
            String actionS = LocalCache.getDomainValueComment(DSConnection.getDefault(), DV_MATCH_CINS,
                    message.getAttribute(CINS_VALUE));
            if (!Util.isEmpty(actionS)) {
                Action action = Action.valueOf(actionS);
                if(action == null){
                    messages.add("Error in action " + actionS);
                    return false;
                }
                BOMessage outgoing;
                Trade linkedTrade;
                TransferArray tradeTransfers;
                try {
                    if (dbCon != null) {
                        outgoing = BOMessageSQL.getMessage(message.getLinkedLongId(), (Connection) dbCon);
                    } else {
                        outgoing = dsCon.getRemoteBO().getMessage(message.getLinkedLongId());
                    }

                    if (outgoing == null) {
                        messages.add("Cannot find Message " + message.getLinkedLongId());
                        return false;
                    }

                    if (outgoing.getTradeLongId() <= 0) {
                        messages.add("Outgoing message  " + message.getLongId() + " does not have a linked trade.");
                        return false;
                    }
                    if (dbCon != null) {
                        linkedTrade = TradeSQL.getTrade(outgoing.getTradeLongId());
                    } else {
                        linkedTrade = dsCon.getRemoteTrade().getTrade(outgoing.getTradeLongId());
                    }

                    if (linkedTrade == null) {
                        messages.add("Cannot find Trade " + outgoing.getTradeLongId());
                        return false;
                    }

                    tradeTransfers = loadTradeTransfers(outgoing.getTradeLongId(), dsCon, (Connection) dbCon);


                    if (!Util.isEmpty(tradeTransfers)) {
                        for(BOTransfer t: tradeTransfers){
                            BOTransfer clonedXfer = (BOTransfer) t.clone();
                            if(isTransferEligibleForUpdate(clonedXfer) &&
                                    BOTransferWorkflow.isTransferActionApplicable(clonedXfer, linkedTrade, action, dsCon, dbCon)){
                                saveTransfer(t, linkedTrade, tradeTransfers, action, events,
                                        (Connection) dbCon, dsCon);
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.error(this, e);
                    return false;
                }

            }
        }
        return true;
    }


}
