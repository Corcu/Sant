package calypsox.tk.bo;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * Custom SDISelector for margin call trades
 *
 * @author aela
 */
@SuppressWarnings("rawtypes")
public class SimpleTransferSDISelector extends com.calypso.tk.bo.SimpleTransferSDISelector {

    @Override
    public Vector getValidSDIList(Trade trade, TradeTransferRule tradeTransferRule, JDate settleDate,
                                  String legalEntity, String legalEntityRole, Vector exceptions, boolean includeNotPreferred,
                                  DSConnection dsCon) {

        Vector validSDI = super.getValidSDIList(trade, tradeTransferRule, settleDate, legalEntity, legalEntityRole,
                exceptions, includeNotPreferred, dsCon);
        //

        if (Util.isEmpty(validSDI)) {
            return validSDI;
        }

        Product product = trade.getProduct();

        if (!(product instanceof SimpleTransfer)) {
            return validSDI;
        }

        // This is SimpleXfer
        SimpleTransfer simpleXfer = (SimpleTransfer) product;

        // 1- For SimpleXfer coming from Interest - the only SDI to use must have attribute call account = account id
        // stored in linked id of the SimpleXfer
        if ("InterestBearing".equals(trade.getKeywordValue(TRADE_SOURCE))) {
            long accountId = simpleXfer.getLinkedLongId();
            for (int i = (validSDI.size() - 1); i >= 0; i--) {
                SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) validSDI.get(i);
                if ((sdi != null) && sdi.getDirectRelationship()
                        && tradeTransferRule.getCounterPartyRole().equals("Client")
                        && (accountId != sdi.getGeneralLedgerAccount())) {
                    validSDI.remove(sdi);

                }
            }

            return validSDI;
        }

        // 2- SimpleXfer coming from BOAdjustement GestionDisponible
        if ("GestionDisponible".equals(trade.getKeywordValue("BO_SYSTEM"))) {

            String inventoryAgent = trade.getKeywordValue(Trade.INVENTORY_AGENT);
            String accountNumber = trade.getKeywordValue(Trade.ACCOUNT_NUMBER);

            LegalEntity agent = BOCache.getLegalEntity(dsCon, inventoryAgent);
            LegalEntity le = BOCache.getLegalEntity(dsCon, legalEntity);

            if ((le == null) || (agent == null)) {
                validSDI.removeAllElements();
            }

            // GSM: 03/12/13 - try different possible accounts (PO the LE or ANY, CCY the product CCY or ANY)
            Account account = getAccount(simpleXfer, dsCon, accountNumber, le);

            if (account == null) {
                validSDI.removeAllElements();
            }

            for (int i = (validSDI.size() - 1); i >= 0; i--) {
                SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) validSDI.get(i);

                if (sdi == null) {
                    continue;
                }
                if (!tradeTransferRule.getCounterPartyRole().equals("ProcessingOrg")) {
                    validSDI.remove(sdi);
                    continue;
                }
                if (account != null && sdi.getGeneralLedgerAccount() != account.getId()) {
                    validSDI.remove(sdi);
                    continue;
                }
                if (agent != null && sdi.getAgentId() != agent.getId()) {
                    validSDI.remove(sdi);
                }

            }
            return validSDI;
        }

        return validSDI;
    }

    /**
     * @param simpleXfer    Product
     * @param dsCon         Connection to DS
     * @param accountNumber
     * @param le            Processing Org LE
     * @return a valid account
     */
    private Account getAccount(final SimpleTransfer simpleXfer, final DSConnection dsCon, final String accountNumber,
                               final LegalEntity le) {

        // account for PO=LE & CCY=ANY
        Account account = BOCache.getAccount(dsCon, accountNumber, le.getId(), Account.ANY);

        // GSM: 03/12/13 - try account for multiple POs -> LE = 0 & CCY=ANY
        if (account == null) {
            account = BOCache.getAccount(dsCon, accountNumber, 0, Account.ANY);

            // account for PO multiple POs -> LE = 0 & CCY=Product CCY
            if (account == null) {
                account = BOCache.getAccount(dsCon, accountNumber, 0, simpleXfer.getCurrency());

                // account for PO=LE & CCY=Product CCY
                if (account == null) {
                    account = BOCache.getAccount(dsCon, accountNumber, le.getId(), simpleXfer.getCurrency());
                }
            }
        }

        return account;
    }
}
