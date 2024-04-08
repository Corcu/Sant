package calypsox.tk.product;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.DefaultProductValidator;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

/**
 * @author acd
 */
public class TransferAgentProductValidator extends DefaultProductValidator {

    @Override
    public boolean isValidInput(Trade trade, Vector msgs) {
        return super.isValidInput(trade, msgs) && validateBlockingAccounts(trade,msgs);
    }

    /**
     *
     * @param trade
     * @param msgs
     * @return return false if "from" and "to" have blocking accounts.
     */
    private boolean validateBlockingAccounts(Trade trade, Vector msgs){
        TransferAgent transferAgent = Optional.ofNullable(trade.getProduct()).filter(TransferAgent.class::isInstance).map(TransferAgent.class::cast).orElse(new TransferAgent());
        Account toSDIAccount = getAccountFromSdi(transferAgent.getToSdiId());
        Account fromSDIAccount = getAccountFromSdi(transferAgent.getFromSdiId());
        boolean isFromToBlockingAccount = isBlockingAccount(toSDIAccount) && isBlockingAccount(fromSDIAccount);
        if(isFromToBlockingAccount){
            msgs.addElement("'Form' and 'To' accounts cannot be blocking accounts.");
        }

        return !isFromToBlockingAccount;
    }

    /**
     * @param sdiId
     * @return
     */
    protected Account getAccountFromSdi(int sdiId){
        SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
        int accountId = Optional.ofNullable(settleDeliveryInstruction).map(SettleDeliveryInstruction::getGeneralLedgerAccount).orElse(0);
        return BOCache.getAccount(DSConnection.getDefault(), accountId);
    }

    /**
     * @param account
     * @return
     */
    private boolean isBlockingAccount(Account account){
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Bloqueo")).filter("true"::equalsIgnoreCase).isPresent();
    }
}
