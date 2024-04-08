package calypsox.tk.bo.workflow.rule;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class CheckVKSDITransferRule implements WfTransferRule {


    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        if (BOTransfer.TRANSFER_ASSIGNED.equals(transfer.getExternalSDStatus()) || BOTransfer.TRANSFER_ASSIGNED.equals(transfer.getInternalSDStatus()))
            return true;

        if (!Util.isEmpty(transfer.getAttribute(BOTransfer.BUSINESS_REASON)))
            return true;

        if (trade != null && !SecFinanceTradeUtil.isVoighKampffSettlement(trade, transfer))
            return true;

        try {
            if (transfer.getNettedTransfer()) { //netted multiple trades

                Collection<BOTransfer> underlyings = Util.isEmpty(transfer.getUnderlyingTransfers()) ? getUnderlings(transfer.getLongId(), dsCon, dbCon) : transfer.getUnderlyingTransfers();
                final Map<Long, Trade> trades = new HashMap<>();
                if (trade != null)
                    trades.put(trade.getLongId(), trade);

                Optional<BOTransfer> tradeXfer = underlyings.stream().filter(x -> {
                    try {
                        Trade t = trades.get(x.getTradeLongId());
                        if (t == null) {
                            t = dbCon != null ? TradeSQL.getTrade(x.getTradeLongId()) : dsCon.getRemoteTrade().getTrade(x.getTradeLongId());
                            trades.put(t.getLongId(), t);
                        }
                        return ("SECURITY".equals(x.getTransferType()) || "PRINCIPAL".equals(x.getTransferType()))
                                && t.getProduct() instanceof SecFinance
                                && SecFinanceTradeUtil.isVoighKampffSettlement(t, x)
                                && (t.getSettleDate().equals(x.getValueDate()) || (t.getMaturityDate() != null && t.getMaturityDate().equals(x.getValueDate())))
                                && hasDifferentSDI(t, x, dsCon, messages);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).findFirst();

                return !tradeXfer.isPresent();

            } else {
                if (trade == null) {
                    if (transfer.getTradeLongId() == 0) {
                        messages.add(String.format("Error, gross transfer %s without trade Id.", transfer));
                        return false;
                    }
                    trade = dbCon == null ? dsCon.getRemoteTrade().getTrade(transfer.getTradeLongId()) : TradeSQL.getTrade(transfer.getTradeLongId(), (Connection) dbCon);
                }
                return "SECURITY".equals(transfer.getTransferType()) || "PRINCIPAL".equals(transfer.getTransferType()) && hasDifferentSDI(trade, transfer, dsCon, messages);
            }

        } catch (Exception e) {
            messages.add(String.format("%s: %s.", e.getClass().getSimpleName(), e.getMessage()));
            return false;
        }
    }

    private boolean hasDifferentSDI(Trade trade, BOTransfer xfer, DSConnection dsCon, Vector<String> messages) {
        SecFinance prod = (SecFinance) trade.getProduct();
        TradeTransferRule openingRule = trade.getSettleDate().equals(xfer.getValueDate()) ? xfer.toTradeTransferRule() : null;
        TradeTransferRule closingRule = prod.getEndDate() != null && prod.getEndDate().equals(xfer.getValueDate()) ? xfer.toTradeTransferRule() : null;

        if (openingRule == null) {
            if (closingRule == null) {
                messages.add(String.format("Cannot find opening rule for trade %s", trade));
                return true;
            }
            Vector<String> ruleErrors = new Vector<>();
            openingRule = getTransferRule(trade, prod, trade.getSettleDate(), xfer.getTransferType(), dsCon, ruleErrors);

            if (!Util.isEmpty(ruleErrors)) {
                messages.addAll(ruleErrors);
                return true;
            }

            if (openingRule == null) {
                messages.add(String.format("Cannot find opening rule for trade %s", trade));
                return true;
            }
        }

        if (closingRule == null) {
            JDate endDate = prod.isOpen() ? JDate.getNow().addBusinessDays(prod.getNoticeDays(), prod.getHolidays()) : prod.getEndDate();
            Vector<String> ruleErrors = new Vector<>();
            closingRule = getTransferRule(trade, prod, endDate, xfer.getTransferType(), dsCon, ruleErrors);
            if (!Util.isEmpty(ruleErrors)) {
                messages.addAll(ruleErrors);
                return true;
            }
            if (closingRule == null) {
                messages.add(String.format("Cannot find closing rule for trade %s", trade));
                return true;
            }
        }

        int openExtSdiId = "PAY".equals(openingRule.getPayReceive()) ? openingRule.getReceiverSDId() : openingRule.getPayerSDId();
        int openIntSdiId = "PAY".equals(openingRule.getPayReceive()) ? openingRule.getPayerSDId() : openingRule.getReceiverSDId();

        int closingExtSdiId = "PAY".equals(closingRule.getPayReceive()) ? closingRule.getReceiverSDId() : closingRule.getPayerSDId();
        int closingIntSdiId = "PAY".equals(closingRule.getPayReceive()) ? closingRule.getPayerSDId() : closingRule.getReceiverSDId();

        if (openExtSdiId != closingExtSdiId) {
            messages.add(String.format("Different cpty SDIs %d & %d on trade %s", openExtSdiId, closingExtSdiId, trade));
            return true;
        }

        if (openIntSdiId != closingIntSdiId) {
            messages.add(String.format("Different PO SDIs %d & %d on trade %s", openIntSdiId, closingIntSdiId, trade));
            return true;
        }

        return false;
    }

    private TradeTransferRule getTransferRule(Trade trade, SecFinance prod, JDate valDate, String type, DSConnection dsCon, Vector<?> ruleErrors) {

        if (trade.getCustomTransferRuleB()) {
            Vector<TradeTransferRule> rules = trade.getTransferRules();
            if (!Util.isEmpty(rules)) {
                TradeTransferRule rule = rules.stream().filter(r ->
                        type.equals(r.getTransferType())
                                && valDate.equals(r.getSettleDate())).findFirst().orElse(null);

                if (rule != null)
                    return null;
            }
        }

        BOProductHandler handler = BOProductHandler.getHandler(prod);
        Vector<TradeTransferRule> rules = handler.generateTransferRules(trade, prod, ruleErrors, dsCon);
        return rules.stream().filter(r ->
                type.equals(r.getTransferType())
                        && valDate.equals(r.getSettleDate())).findFirst().orElse(null);

    }

    private List<BOTransfer> getUnderlings(long nettedTransferId, DSConnection dsCon, Object dbCon) throws CalypsoServiceException, PersistenceException {
        TransferArray underlings = dbCon != null ? BOTransferSQL.getNettedTransfers(nettedTransferId, (Connection) dbCon) :
                dsCon.getRemoteBO().getNettedTransfers(nettedTransferId);
        return underlings.stream().filter(x -> !Status.S_CANCELED.equals(x.getStatus()) && !Status.S_SPLIT.equals(x.getStatus())).collect(Collectors.toList());
    }

    @Override
    public String getDescription() {
        return "Compares repo opening and closing SDIs \n" +
                "FALSE is SDIs are different \n" +
                "TRU otherwise";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        //no update
        return true;
    }

}
