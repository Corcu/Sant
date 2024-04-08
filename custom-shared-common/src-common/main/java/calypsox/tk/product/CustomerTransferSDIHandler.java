package calypsox.tk.product;

import calypsox.util.product.CustomerTransferUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.*;

/**
 * @author aalonsop
 */
public class CustomerTransferSDIHandler {

    private static final String CALL_ACC_STR = "CallAccount";
    private static final String CONTRACT_ID_ACC_ATTR = "MARGIN_CALL_CONTRACT";

    /**
     * @param trade
     * @param contractId
     * @return true if successful assignment
     */
    public boolean assignSDIsByCallAccount(Trade trade) {
        boolean isSuccessfulAssignment = false;
        if (trade != null && trade.getProduct() instanceof CustomerTransfer) {
            Vector<SettleDeliveryInstruction> eligibleSdis = new CustomerTransferSDIFinder().findClientSdi(trade);
            if (!Util.isEmpty(eligibleSdis) && eligibleSdis.get(0).getId() > 0) {
                setCustomerTransferSDI(eligibleSdis.get(0), trade);
                isSuccessfulAssignment = true;
            }
        } else {
            throw new UnsupportedOperationException("CustomerTransferSDIHandler only accepts CustomerTransfer trades");
        }
        return isSuccessfulAssignment;
    }

    /**
     * @param sdi
     * @param trade
     */
    private void setCustomerTransferSDI(SettleDeliveryInstruction sdi, Trade trade) {
        ((CustomerTransfer) trade.getProduct()).setCustomerAccountSDI(sdi.getId());
    }

    /**
     * @param contractId
     * @param currency
     * @return
     */
    public Optional<Account> findAndFilterContractRelatedAccounts(int contractId, String currency) {
        List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), CONTRACT_ID_ACC_ATTR, String.valueOf(contractId));
        Optional<Account> targetAccount = Optional.empty();
        if (!Util.isEmpty(accounts)) {
            targetAccount = accounts.stream().filter(account -> currency.equals(account.getCurrency())).filter(Account::getCallAccountB).findAny();
        }
        return targetAccount;
    }

    /**
     * Custom inner sdiSelector implementation
     */
    private class CustomerTransferSDIFinder {

        /**
         * @param trade
         * @return
         */
        private Vector<SettleDeliveryInstruction> findClientSdi(Trade trade) {
            Vector<SettleDeliveryInstruction> clientSdis = new Vector<>();
            int callAccountId = CustomerTransferUtil.getIbAccountIdFromCustomerTransfer(trade);
            try {
                Vector<SettleDeliveryInstruction> unfilteredSdis = DSConnection.getDefault().getRemoteReferenceData().getSettleDeliveryInstructions(new SDIQueryBuilder().buildSDIQuery(callAccountId), Collections.emptyList());
                clientSdis.add(Optional.ofNullable(filterSDIs(unfilteredSdis, trade)).orElse(new SettleDeliveryInstruction()));
            } catch (CalypsoServiceException exc) {
                Log.error(this.getClass().getSimpleName(), exc.getMessage(), exc.getCause());
            }
            return clientSdis;
        }

        /**
         * @param eligibleSdis
         * @param trade
         * @param contractId
         * @return
         */
        private SettleDeliveryInstruction filterSDIs(Vector<SettleDeliveryInstruction> eligibleSdis, Trade trade) {
            SettleDeliveryInstruction filteredSdi = new SettleDeliveryInstruction();
            Vector<String> books = AccessUtil.getBooksWithProductAccess(getTradeProductType(trade));
            Iterator<SettleDeliveryInstruction> sdiIterator = eligibleSdis.iterator();
            while (sdiIterator.hasNext() && filteredSdi.getId() == 0) {
                SettleDeliveryInstruction iteratorSdi = sdiIterator.next();
                if (iteratorSdi != null && acceptBook(iteratorSdi, books) /*&& acceptSDI(iteratorSdi, trade, contractId)*/) {
                    filteredSdi = iteratorSdi;
                }
            }
            return filteredSdi;

        }

        /**
         * @param sdi
         * @param bookNames
         * @return
         */
        private boolean acceptBook(SettleDeliveryInstruction sdi, Vector<String> bookNames) {
            boolean res = false;
            if (!Util.isEmpty(sdi.getAttribute(CALL_ACC_STR)) && !Util.isEmpty(bookNames)) {
                Account acc = BOCache.getAccount(DSConnection.getDefault(), Util.toInt(sdi.getAttribute(CALL_ACC_STR)));
                if (acc != null && acc.getCallBookId() > 0) {
                    Book bk = BOCache.getBook(DSConnection.getDefault(), acc.getCallBookId());
                    if (bk != null && bookNames.contains(bk.getName())) {
                        res = true;
                    }
                }
            }
            return res;
        }

        /**
         * @return
         * @deprecated Old version
         */
        @Deprecated
        private boolean acceptSDI(SettleDeliveryInstruction sdi, Trade trade, int contractId) {
            boolean res = false;
            if (sdi.isClientSDI()) {
                Optional<Account> account = findAndFilterContractRelatedAccounts(contractId, trade.getTradeCurrency());
                if (account.isPresent()) {
                    res = sdi.getGeneralLedgerAccount() == account.get().getId();
                }
            }
            return res;
        }


        /**
         * @param trade
         * @return trade's productType
         */
        private String getTradeProductType(Trade trade) {
            String productType = "";
            if (trade != null) {
                productType = trade.getProductType();
            }
            return productType;
        }

    }

    /**
     * Builds sdi query
     */
    private class SDIQueryBuilder {

        private static final String INNER_SELECT = "SELECT SDI_ID FROM SDI_ATTRIBUTE WHERE ATTRIBUTE_NAME='" + CALL_ACC_STR + "' AND ATTRIBUTE_VALUE='?'";
        private static final String SDI_WHERE = "LE_SETTLE_DELIVERY.SDI_ID IN (INNER_SELECT) AND LE_SETTLE_DELIVERY.LE_ROLE='Client'";

        /**
         * @param callAccountId
         * @return
         */
        private String buildSDIQuery(int callAccountId) {
            String query = SDI_WHERE;
            return query.replace("INNER_SELECT", buildInnerQuery(callAccountId));
        }

        /**
         * @param callAccountId
         * @return
         */
        private String buildInnerQuery(int callAccountId) {
            return INNER_SELECT.replace("?", String.valueOf(callAccountId));
        }
    }

}
