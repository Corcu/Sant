/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.core.MarginCallConfigLight;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantCalypsoUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.AccountInterests;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class SantInterestPaymentRunnerOldReport extends SantReport {

    private static final long serialVersionUID = 8793086264360719596L;
    private static final String ACTIVE_ACCOUNT = "Active";
    private static final String INTEREST_TRANSFER_TO_KW = "INTEREST_TRANSFER_TO";
    private static final String INTEREST_TRANSFER_FROM_KW = "INTEREST_TRANSFER_FROM";
    private static final String ID_KEYWORD_SEP = ",";

    @Override
    public ReportOutput loadReport(Vector<String> errorMsgs) {

        try {

            return getReportOutput();

        } catch (final Exception e) {
            String msg = "Cannot load SantInterestPayment ";
            Log.error(this, msg, e);
            errorMsgs.add(msg + e.getMessage());
        }

        return null;
    }

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    private ReportOutput getReportOutput() throws RemoteException {

        DefaultReportOutput output = new DefaultReportOutput(this);
        ReportTemplate template = getReportTemplate();

        JDate processStartDate = getDate(template, getValDate(), TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
        if (processStartDate == null) {
            processStartDate = getValDate();
        }

        final Map<Integer, Account> accountsToProcess = getAccountsToProcess(template);
        List<Integer> accIds = new ArrayList<>(accountsToProcess.keySet());
        StringBuilder from;
        StringBuilder where;

        TradeArray custXferTrades = new TradeArray();
        TradeArray ibTrades = new TradeArray();

        final int SQL_IN_ITEM_COUNT = 999;
        int start = 0;

        for (int i = 0; i <= (accIds.size() / SQL_IN_ITEM_COUNT); i++) {
            int end = (i + 1) * SQL_IN_ITEM_COUNT;
            if (end > accIds.size()) {
                end = accIds.size();
            }
            final List<Integer> subList = accIds.subList(start, end);
            start = end;

            // Load interest bearing trades
            from = new StringBuilder();
            where = new StringBuilder();
            buildInterestBearingSQLQuery(from, where, processStartDate, subList);
            ibTrades.addAll(loadTrades(from, where));

            // Load Simple Xfer trades
            from = new StringBuilder();
            where = new StringBuilder();
            buildSimpleXFerSQLQuery(from, where, processStartDate, subList);
            custXferTrades.addAll(loadTrades(from, where));


            // Load Customer Xfer trades
            custXferTrades.addAll(getCtTrades(buildCustomerTransferIdList(ibTrades)));

        }
        output.setRows(getReportRows(processStartDate, accountsToProcess.values(), custXferTrades, ibTrades));

        return output;
    }



    /**
     * Remove Canceled Customer Transfers
     * @param ids
     * @return
     * @throws CalypsoServiceException
     */
    private TradeArray getCtTrades(long[] ids) throws CalypsoServiceException {
        final TradeArray trades = SantCalypsoUtilities.getInstance().getTradesWithTradeFilter(ids);
        TradeArray result = new TradeArray();
        result.addAll(Arrays.stream(trades.getTrades())
                .filter(trade -> !Status.CANCELED.equalsIgnoreCase(trade.getStatus().toString()))
                .collect(Collectors.toList()));
        return result;
    }


    private ReportRow[] getReportRows(JDate processStartDate, Collection<Account> accounts, TradeArray custXFerTrades,
                                      TradeArray ibTrades) {

        List<ReportRow> rowList = new ArrayList<>();
        for (Account acc : accounts) {
            SantInterestPaymentRunnerEntry entry = new SantInterestPaymentRunnerEntry();
            ReportRow row = new ReportRow(entry, SantInterestPaymentRunnerReportTemplate.ROW_DATA);
            row.setProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA, true);

            Trade ibTrade = findIbTrades(ibTrades, acc.getId());
            Trade simpleXferTrade = findCustOrSimpleXferTrade(custXFerTrades, ibTrade, acc.getId());


            entry.setIbTrade(ibTrade);
            entry.setSimpleXferTrade(simpleXferTrade);

            if(validCT(simpleXferTrade)){
                entry.setCtTrade(simpleXferTrade);
            }

            entry.setAccount(acc);
            entry.setProcessDate(processStartDate);
            entry.setPoOwner(acc.getAccountProperty("PO_OWNER"));


            //GSM 31/05/2017 - Chile requires cpty shortname in this report
            LegalEntity cpty = BOCache.getLegalEntity(getDSConnection(), acc.getLegalEntityId());
            if (cpty != null) {
                entry.setCptyName(cpty.getAuthName());
            }

            rowList.add(row);
        }
        return rowList.toArray(new ReportRow[rowList.size()]);
    }

    private boolean validCT(Trade trade){
        return null!=trade && trade.getProduct() instanceof CustomerTransfer;
    }

    /**
     * @param custXferTrades
     * @param ibTrade
     * @param accId
     * @return
     */
    private Trade findCustOrSimpleXferTrade(TradeArray custXferTrades, Trade ibTrade, int accId) {
        Trade custXferTrade = null;
        for (Trade iteratedTrade : custXferTrades.getTrades()) {
            if (isCustomerTransferMatch(iteratedTrade, ibTrade) || isSimpleTransferMatch(iteratedTrade, accId)) {
                custXferTrade = iteratedTrade;
            }
        }
        return custXferTrade;
    }

    /**
     * @param trade
     * @param ibTrade
     * @return
     */
    private boolean isCustomerTransferMatch(Trade trade, Trade ibTrade) {
        return (trade != null && ibTrade != null &&
                (trade.getLongId() == getFirstIdFromKeyword(ibTrade.getKeywordValue(INTEREST_TRANSFER_TO_KW)) || ibTrade.getLongId() == getFirstIdFromKeyword(trade.getKeywordValue(INTEREST_TRANSFER_FROM_KW))));
    }

    /**
     * @param trade
     * @param accId
     * @return
     */
    private boolean isSimpleTransferMatch(Trade trade, int accId) {
        boolean res = false;
        if (trade != null && trade.getProduct() instanceof SimpleTransfer && ((SimpleTransfer) trade.getProduct()).getLinkedLongId() == accId) {
            res = true;
        }
        return res;
    }

    /**
     * @param trades
     * @param accId
     * @return
     */
    private Trade findIbTrades(TradeArray trades, int accId) {
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            Product p = trade.getProduct();
            if (trade.getProductType().equals(InterestBearing.class.getSimpleName()) && ((InterestBearing) p).getAccountId() == accId) {
                return trade;
            }
        }
        return null;
    }

    /**
     * @param from
     * @param where
     * @return
     * @throws RemoteException
     */
    private TradeArray loadTrades(StringBuilder from, StringBuilder where) throws RemoteException {
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrades(from.toString(), where.toString(), "", true, null);
        } catch (final RemoteException exc) {
            Log.error(this, "Cannot load trades", exc);
            throw exc;
        }
    }

    private void buildSimpleXFerSQLQuery(StringBuilder from, StringBuilder where, JDate processStartDate,
                                         List<Integer> accIds) {

        from.append(" product_simplexfer ");

        where.append(" trade.product_id = product_simplexfer.product_id ");
        where.append(" and trade.trade_status != 'CANCELED' ");
        where.append(" and product_simplexfer.flow_type = 'INTEREST' ");
        where.append(" and TRUNC(trade.SETTLEMENT_DATE) = ");
        where.append(Util.date2SQLString(processStartDate));
        where.append(" and product_simplexfer.linked_id in ");
        where.append(Util.collectionToSQLString(accIds));

    }

    /**
     * @param ibTrades
     * @return
     */
    private long[] buildCustomerTransferIdList(TradeArray ibTrades) {
        long[] ibIds = new long[ibTrades.size()];
        for (int i = 0; i < ibTrades.size(); i++) {
            Trade ibTrade = ibTrades.get(i);
            if (ibTrade != null && ibTrade.getProduct() instanceof InterestBearing) {
                try {
                    long ibId = getFirstIdFromKeyword(ibTrade.getKeywordValue(INTEREST_TRANSFER_TO_KW));
                    if (ibId > 0) {
                        ibIds[i] = ibId;
                    }
                } catch (NumberFormatException exc) {
                    Log.debug(this, "Couldn't parse number");
                }
            }
        }
        return ibIds;
    }

    /**
     * @param keywordValue
     * @return
     */
    private long getFirstIdFromKeyword(String keywordValue) {
        long id = 0;
        if (!Util.isEmpty(keywordValue)) {
            if (keywordValue.contains(ID_KEYWORD_SEP)) {
                String[] croppedIds = keywordValue.split(ID_KEYWORD_SEP);
                if (!Util.isEmpty(croppedIds[0])) {
                    id = Long.valueOf(croppedIds[0]);
                }
            } else {
                id = Long.valueOf(keywordValue);
            }
        }
        return id;
    }

    private void buildInterestBearingSQLQuery(StringBuilder from, StringBuilder where, JDate processStartDate,
                                              List<Integer> accIds) {

        from.append(" product_int_bearing ");

        where.append(" trade.product_id = product_int_bearing.product_id ");
        where.append(" and trade.trade_status != 'CANCELED' ");
        where.append(" and TRUNC(product_int_bearing.payment_date) = ");
        where.append(Util.date2SQLString(processStartDate));
        where.append(" and product_int_bearing.account_id in ");
        where.append(Util.collectionToSQLString(accIds));
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Account> getAccountsToProcess(ReportTemplate template) {

        final String callAccountIds = (String) template.get(SantInterestPaymentRunnerReportTemplate.CALL_ACCOUNT_ID);
        final Vector<String> callAccountIdV = Util.string2Vector(callAccountIds);

        final String agreementIds = (String) template.get(SantInterestPaymentRunnerReportTemplate.AGREEMENT_ID);
        final Vector<String> agreementIdV = Util.string2Vector(agreementIds);

        final String agreementTypes = (String) template.get(SantInterestPaymentRunnerReportTemplate.AGREEMENT_TYPE);
        final Vector<String> agreementTypeV = Util.string2Vector(agreementTypes);

        // 03/08/15. SBNA Multi-PO filter
        // final String poNames = (String)
        // template.get(SantInterestPaymentRunnerReportTemplate.OWNER_AGR);
        final String poNames = CollateralUtilities.filterPoNamesByTemplate(template);
        final Vector<String> poNameV = Util.string2Vector(poNames);

        final String rateIndexes = (String) template.get(SantInterestPaymentRunnerReportTemplate.RATE_INDEX);
        final Vector<String> rateIndexV = Util.string2Vector(rateIndexes);

        final String currencies = (String) template.get(SantInterestPaymentRunnerReportTemplate.CURRENCY);
        final Vector<String> currencyV = Util.string2Vector(currencies);

        final Map<Integer, Account> accounts = (Map<Integer, Account>) template
                .get(SantInterestPaymentRunnerReportTemplate.ACCOUNT_MAP);
        final Map<Integer, MarginCallConfigLight> contracts = (Map<Integer, MarginCallConfigLight>) template
                .get(SantInterestPaymentRunnerReportTemplate.CONTRACT_MAP);

        Map<Integer, Account> accountsToProcess = new HashMap<Integer, Account>();

        // BAU 05/05/15: check accountActiveStatus DV has this value
        if (!Account.isActive(ACTIVE_ACCOUNT)) {
            Log.error(this, "DV accountActiveStatus not set. Cannot filter non-active accounts");
        }

        final JDate today = JDate.getNow();

        for (Account account : accounts.values()) {

            // BAU 05/05/15: GSM, check account is active
            if ((!Util.isEmpty(account.getAccountStatus()) && !account.getAccountStatus().equals(ACTIVE_ACCOUNT))
                    || ((account.getActiveTo() != null) && account.getActiveTo().before(today))) {
                continue;
            }

            // Check account ids
            if (!Util.isEmpty(callAccountIdV)) {
                if (!callAccountIdV.contains(String.valueOf(account.getId()))) {
                    continue;
                }
            }

            // Check currencies
            if (!Util.isEmpty(currencyV)) {
                if (!currencyV.contains(account.getCurrency())) {
                    continue;
                }
            }
            // Find contract id from account
            String agreementIdFromAccount = null;
            Vector<String> accountProperties = account.getAccountProperties();
            if (Util.isEmpty(accountProperties)) {
                continue;
            }
            for (int i = 0; i < accountProperties.size(); i++) {
                String prop = accountProperties.get(i);

                if (prop.equals("MARGIN_CALL_CONTRACT")) {
                    if ((i + 1) < accountProperties.size()) {
                        agreementIdFromAccount = accountProperties.get(i + 1);
                        break;
                    }
                }

            }
            if (Util.isEmpty(agreementIdFromAccount)) {
                continue;
            }
            int contractIdAsInt = 0;
            try {
                contractIdAsInt = Integer.valueOf(agreementIdFromAccount);
            } catch (Exception e) {
                Log.warn(this, e); //sonar
                continue;
            }

            // Check agreements from agreement id fields
            if (!Util.isEmpty(agreementIdV)) {
                if (!agreementIdV.contains(agreementIdFromAccount)) {
                    continue;
                }
            }

            MarginCallConfigLight mccLight = contracts.get(contractIdAsInt);
            if (mccLight == null) {
                continue;
            }

            // Check contract type
            if (!Util.isEmpty(agreementTypeV)) {
                if (!agreementTypeV.contains(mccLight.getContractType())) {
                    continue;
                }
            }

            // Check PO Name
            if (!Util.isEmpty(poNameV)) {
                if (!poNameV.contains(mccLight.getPoName())) {
                    continue;
                }
            }
            account.setAccountProperty("PO_OWNER", mccLight.getPoName());

            // Check Rate Indexes
            if (!Util.isEmpty(rateIndexV)) {
                Vector<String> rateIndexesFromAccount = getRateIndexesFromAccount(account);
                boolean isFound = false;
                for (String riFromAcc : rateIndexesFromAccount) {
                    if (rateIndexV.contains(riFromAcc)) {
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) {
                    continue;
                }
            }

            // All checks passed
            accountsToProcess.put(account.getId(), account);
        }

        return accountsToProcess;
    }

    @SuppressWarnings("unchecked")
    private Vector<String> getRateIndexesFromAccount(final Account acc) {

        Vector<String> rateIndexes = new Vector<String>();
        if ((acc.getAccountInterests() == null) || (acc.getAccountInterests().size() == 0)) {
            return rateIndexes;
        }

        for (AccountInterests accInterests : acc.getAccountInterests()) {

            final AccountInterestConfig config = BOCache.getAccountInterestConfig(DSConnection.getDefault(),
                    accInterests.getConfigId());

            if ((config.getRanges() == null) || (config.getRanges().size() == 0)) {
                continue;
            }

            Iterator<AccountInterestConfigRange> iterator = config.getRanges().iterator();

            while (iterator.hasNext()) {
                final AccountInterestConfigRange range = iterator.next();

                if ((range == null) || range.isFixed()) {
                    continue;
                }

                if (range.getRateIndex() == null) {
                    continue;
                }

                rateIndexes.add(range.getRateIndex().toStringNoSource());

            }
        }

        return rateIndexes;
    }

}
