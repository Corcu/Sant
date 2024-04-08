package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Warrant;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.swiftparser.MT537MessageProcessor;
import com.calypso.tk.util.swiftparser.SecurityMatcher;

import java.util.Arrays;
import java.util.Vector;

public class SantMatchSecStmtItemMessageRule implements WfMessageRule {
    private final static String[] MSG_TYPES = {"MT536", "MT537"};

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (Arrays.stream(MSG_TYPES).noneMatch(t -> message.getTemplateName().startsWith(t)))
            return true;

        if (transfer == null)
            return false;

        if ("TRPO".equals(message.getAttribute("Trade_Type"))) {
            return matchTriParty(message, transfer, messages);
        } else {
            String messageFunction = message.getAttribute("Message_Function");

            if (!Util.isEmpty(messageFunction)) {
                switch (messageFunction) {
                    case "CANC":
                    case "PENA":
                        return true;
                }
            }

            String status = message.getAttribute("Processing_Status");
            boolean isCanceled = !Util.isEmpty(status)&&status.contains("//CAN");

            double settleAmount = Util.istringToNumber(message.getAttribute("Nominal Amount"));
            double cashAmount = Util.istringToNumber(message.getAttribute("Cash Amount"));
            String isin = message.getAttribute("Security Code");
            String ccy = message.getAttribute("Ccy");
            if (!transfer.isSecurity() && cashAmount != 0.0 && settleAmount == 0.0) {
                settleAmount = cashAmount;
                cashAmount = 0.0;
            }

            Product security = SecurityMatcher.getSecurity(transfer, dsCon);
            double transferAmount = transfer.getSettlementAmount() * security.getPrincipal(transfer.getValueDate());

            if (!transfer.isSecurity()) {
                messages.add("Transfer is not Security");
                return false;
            }

            if (Util.isEmpty(isin)) {
                messages.add("Message Security Code is Empty.");
                return false;
            }

            if (!isin.equals(security.getSecCode("ISIN"))) {
                messages.add(String.format("Security code mismatch, message %s, transfer %s.", isin, security.getSecCode("ISIN")));
                return false;
            }

            String delType = message.getAttribute("Del_Type");
            if (!Util.isEmpty(delType)) {
                if (!delType.startsWith(getDelRec(transfer))) {
                    messages.add(String.format("Mismatch in  delivery type, transfer %s, message %s.", getDelRec(transfer), delType));
                    return false;
                }
            }

            if (security instanceof Bond) {
                Bond bd = (Bond) security;
                if (bd.getQuoteType() != null && bd.getQuoteType().equals("Price") || Util.isTrue(security.getSecCode("UNIT_SWIFT_FORMAT"))) {
                    transferAmount = transfer.getSettlementAmount();

                }
            } else if (security instanceof Warrant) {
                Warrant bd = (Warrant) security;
                if (bd.getQuoteType() != null && bd.getQuoteType().equals("Price")) {
                    transferAmount = transfer.getSettlementAmount();

                }
            }


            transferAmount = SwiftUtil.roundAmount(transferAmount, security);


            String cur = transfer.getSettlementCurrency();


            int otherdigits = CurrencyUtil.getRoundingUnit(cur);

            if (!withTolerance(transferAmount, settleAmount, Math.pow(10, -otherdigits))) {
                messages.add(String.format("Nominal/Quantity mismatch, transfer %s, statement %s ", new Amount(Math.abs(transferAmount), otherdigits), new Amount(Math.abs(settleAmount),otherdigits)));
                return false;
            }


            if (!withTolerance(transfer.getRealCashAmount(), cashAmount, Math.pow(10, -otherdigits))) {
                messages.add(String.format("Cash Amount mismatch, transfer %s, statement %s.",  new Amount(Math.abs(transfer.getRealCashAmount()), otherdigits), new Amount(Math.abs(cashAmount),otherdigits)));
                return false;
            }

            if (cashAmount != 0 && !transfer.getSettlementCurrency().equals(ccy)) {
                messages.add(String.format("Currency mismatch  Amount mismatch, transfer %s, statement %s.", transfer.getSettlementCurrency(), ccy));
                return false;
            }

            if ("MT536".equals(message.getTemplateName())) {
                if (!transfer.getSettleDate().equals(message.getSettleDate())) {
                    messages.add(String.format("Settlement date mismatch, transfer %s, statement %s.", transfer.getSettleDate(), message.getSettleDate()));
                    return false;
                }
            } else {

                if (!isCanceled && !transfer.getValueDate().equals(message.getSettleDate())) {
                    messages.add(String.format("Contractual settle date mismatch, transfer %s, statement %s.", transfer.getValueDate(), message.getSettleDate()));
                    return false;
                }

                if (isCanceled && transfer.getSettleDate().after(message.getSettleDate())) {
                    messages.add(String.format("Contractual settle date mismatch, transfer %s, statement %s.", transfer.getValueDate(), message.getSettleDate()));
                    return false;
                }
            }
           if ( isCanceled ) {
               if (!Status.isCanceled(transfer.getStatus())) {
                   messages.add(String.format("Wrong transfer status %s, expected cancelled.", transfer.getStatus()));
                   return false;
               }
           } else  {
               if (Status.S_SPLIT.equals(transfer.getStatus()) || Status.isCanceled(transfer.getStatus())) {
                   messages.add("Transfer was cancelled/split.");
                   return false;
               }

               if (message.getTemplateName().startsWith("MT536") && !isSettled(transfer)) {
                   messages.add(String.format("Wrong transfer status %s, expected settled.", transfer.getStatus()));
                   return false;
               }
               if (message.getTemplateName().startsWith("MT537") && isSettled(transfer)) {
                   messages.add(String.format("Wrong transfer status %s, expected pending.", transfer.getStatus()));
                   return false;
               }
           }
            return true;
        }
    }

    private boolean isSettled(BOTransfer xfer) {
        return Status.S_SETTLED.equals(xfer.getStatus());
    }

    private boolean withTolerance(double amt1, double amt2, double tol) {
        return Math.abs(Math.abs(Math.abs(amt1) - Math.abs(amt2))) < tol;
    }

    private boolean matchTriParty(BOMessage message, BOTransfer transfer, Vector<String> messages) {

        int len = messages.size();
        String delType = message.getAttribute("Del_Type");
        if (!Util.isEmpty(delType)) {
            if (!delType.startsWith(getDelRec(transfer))) {
                messages.add(String.format("Mismatch in  delivery type, transfer %s, message %s.", getDelRec(transfer), delType));
            }
        }
        String ccy = message.getAttribute("Ccy");
        if (!Util.isEmpty(ccy)) {
            if (!ccy.equals(transfer.getSettlementCurrency())) {
                messages.add(String.format("Mismatch in currency, transfer %s, message %s.", transfer.getSettlementCurrency(), ccy));
            }
        }
        return messages.size() == len;
    }

    private String getDelRec(BOTransfer transfer) {
        if ("SECURITY".equals(transfer.getTransferType()))
            return "PAY".equals(transfer.getPayReceive()) ? "D" : "R";
        else
            return "PAY".equals(transfer.getPayReceive()) ? "R" : "D";

    }

    @Override
    public String getDescription() {
        return "Matches selected details of TriParty Message";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}
