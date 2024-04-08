package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.*;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.product.UnavailabilityTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.TradeServerImpl;

import java.sql.Connection;
import java.util.Optional;
import java.util.Vector;

/**
 * @author acd
 */
public class SantCreateUnavailabilityTransferTradeTransferRule implements SantUnavailabilityTransferTradeTransferRule {

    @Override
    public String getDescription() {
        return "Generate a new UnavailabilityTransfer from the Transfer whit a defined blocking account";
    }

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (isUnavailabilityTransferTradeActionValid(trade, transfer)) {
            return this.createUnavailabilityTrade(trade, transfer, events, excps, dsCon, dbCon);
        }
        return true;
    }

    protected boolean createUnavailabilityTrade(Trade trade, BOTransfer transfer, Vector events, Vector excps, DSConnection dsCon, Object dbCon) {

        TransferAgent transferAgent = Optional.ofNullable(trade.getProduct())
                .filter(TransferAgent.class::isInstance)
                .map(TransferAgent.class::cast).orElse(null);

        boolean isFromBlockingAccount = false;
        boolean isFromPignoracionAccount = false;
        boolean isToPignoracionAccount = false;
        Account account = BOCache.getAccount(dsCon, transfer.getGLAccountNumber());

        boolean isToBlockingAccount = isBlockingAccount(account);
        if(!isToBlockingAccount){
            isToPignoracionAccount = isPignoracionAccount(account);

            if(!isToPignoracionAccount){
                int externalSettleDeliveryId = transfer.getExternalSettleDeliveryId();
                account = getAccountFromSdi(externalSettleDeliveryId, dsCon);
                isFromBlockingAccount = isBlockingAccount(account);
                if(!isFromBlockingAccount){
                    isFromPignoracionAccount = isPignoracionAccount(account);
                }
            }
        }

        boolean isBloqueo = isToBlockingAccount || isFromBlockingAccount;
        boolean isPignoracion = isToPignoracionAccount || isFromPignoracionAccount;

        if(null!=transferAgent && (isBloqueo || isPignoracion)){
            try {
                Product security = transferAgent.getSecurity();
                JDatetime tradeDate = trade.getTradeDate();
                JDate settleDate = trade.getSettleDate();
                double quantity = -trade.getQuantity();
                int agentId = transfer.getInternalAgentId();

                if(isFromBlockingAccount || isFromPignoracionAccount){
                    quantity = trade.getQuantity();
                    agentId = transfer.getExternalAgentId();

                }

                String currency = null!=security ? security.getCurrency() : "";
                Trade newTrade = new Trade();
                UnavailabilityTransfer unavailabilityTransfer = new UnavailabilityTransfer();
                unavailabilityTransfer.setSecurity(security);
                unavailabilityTransfer.setQuantity(quantity);
                unavailabilityTransfer.setPrincipal(security.getPrincipal());
                unavailabilityTransfer.setStartDate(settleDate);
                unavailabilityTransfer.setEndDate((JDate)null);
                unavailabilityTransfer.setIsOpenTerm(true);
                newTrade.setProduct(unavailabilityTransfer);
                newTrade.setQuantity(quantity);
                newTrade.setTradeCurrency(currency);
                newTrade.setSettleCurrency(currency);
                newTrade.setTradeDate(tradeDate);
                newTrade.setSettleDate(settleDate);
                newTrade.setTraderName("NONE");
                newTrade.setSalesPerson("NONE");
                Book book = trade.getBook();
                if (book != null) {
                    newTrade.setBook(book);
                    LegalEntity processingOrg = BOCache.getProcessingOrg(dsCon, book);
                    newTrade.setCounterParty(processingOrg);
                    newTrade.setRole("ProcessingOrg");

                    newTrade.addKeyword(ATTR_UNAVAILABILITYTRANSFER_ORIGIN_TRADE_ID, String.valueOf(trade.getLongId()));
                    if(isBloqueo){
                        newTrade.setUnavailabilityReason(UNAVALABILTITY_REASON_BLOQUEO);
                    }else {
                        newTrade.setUnavailabilityReason(UNAVALABILTITY_REASON_PIGNORACION);
                    }
                    LegalEntity le = BOCache.getLegalEntity(dsCon, agentId);
                    if (le != null) {
                        newTrade.setInventoryAgent(le.getCode());
                    }
                    if (account != null) {
                        newTrade.setAccountNumber(account.getName());
                    }
                    newTrade.setAction(Action.NEW);

                    long unavailabilityTransferTradeId = TradeServerImpl.saveTrade(newTrade, (Connection)dbCon, events);
                    transfer.setAttribute(ATTR_UNAVAILABILITYTRANSFER_TRADE_ID, String.valueOf(unavailabilityTransferTradeId));
                    return true;
                } else {
                    Log.error(this.getClass().getSimpleName(), "Book is missing.");
                    return false;
                }

            } catch (Exception var21) {
                Log.error(this, var21);
                return false;
            }
        }
        return true;
    }

    protected Account getAccountFromSdi(int sdiId, DSConnection dsCon){
        SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
        int accountId = Optional.ofNullable(settleDeliveryInstruction).map(SettleDeliveryInstruction::getGeneralLedgerAccount).orElse(0);
        return BOCache.getAccount(dsCon, accountId);
    }

    /**
     * @param account
     * @return
     */
    private boolean isBlockingAccount(Account account){
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Bloqueo")).filter("true"::equalsIgnoreCase).isPresent();
    }

    private boolean isPignoracionAccount(Account account){
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Pignoracion")).filter("true"::equalsIgnoreCase).isPresent();
    }

}
