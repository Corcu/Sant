/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader.interestnotif;

import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.SantInterestNotificationReportTemplate;
import calypsox.tk.report.loader.CallAccountLoader;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.BOPositionUtil;
import com.calypso.tk.util.TradeArray;

import java.util.*;
import java.util.Map.Entry;

public class SantInterestNotificationEntryBuilder {

    protected final List<SantInterestNotificationEntry> santInterestEntries = new ArrayList<SantInterestNotificationEntry>();

    public List<SantInterestNotificationEntry> getEntries() {
        return this.santInterestEntries;
    }

    @SuppressWarnings("unchecked")
    public void build(final TradeArray trades, final JDate processStartDate, final JDate processEndDate, JDate valDate,
                      final ReportTemplate template) {

        Map<Integer, MarginCallConfigLight> mccLights = (Map<Integer, MarginCallConfigLight>) template
                .get(SantInterestNotificationReportTemplate.CONTRACT_MAP);
        Map<Integer, Account> accounts = (Map<Integer, Account>) template
                .get(SantInterestNotificationReportTemplate.ACCOUNT_MAP);
        // final String rateIndexes = (String) template.get(SantInterestNotificationReportTemplate.RATE_INDEX);
        // final Vector<String> rateIndexV = Util.string2Vector(rateIndexes);
        final String contractTypes = (String) template.get(SantInterestNotificationReportTemplate.AGREEMENT_TYPE);
        final Vector<String> contractTypeV = Util.string2Vector(contractTypes);
        final String poNames = (String) template.get(SantInterestNotificationReportTemplate.OWNER_AGR);
        final Vector<String> poNameV = Util.string2Vector(poNames);
        final String currencies = (String) template.get(SantInterestNotificationReportTemplate.CURRENCY);
        final Vector<String> currencyV = Util.string2Vector(currencies);

        // AdHoc Interest Bearing entries aggregated by account
        Map<Account, List<InterestBearingEntryWrapper>> interestBearingAdHocEntriesByAccount = new HashMap<Account, List<InterestBearingEntryWrapper>>();
        // Generic Interest Bearing aggregated by trade
        Map<Trade, List<InterestBearingEntryWrapper>> interestBearingByTrade = new HashMap<Trade, List<InterestBearingEntryWrapper>>();

        //Reload all
        CallAccountLoader loader = new CallAccountLoader();
        loader.load();
        accounts = loader.get();


        //Reload all
        final MarginCallConfigLightLoader mccLightLoader = new MarginCallConfigLightLoader();
        mccLightLoader.load();
        mccLights = mccLightLoader.get();


        for (int i = 0; i < trades.size(); i++) {
            final Trade trade = trades.get(i);
            buildInterestBearingentryMap(interestBearingAdHocEntriesByAccount, interestBearingByTrade, trade,
                    mccLights, accounts, contractTypeV, poNameV, currencyV);
        }

        // Process generic
        for (Entry<Trade, List<InterestBearingEntryWrapper>> entry : interestBearingByTrade.entrySet()) {
            this.santInterestEntries
                    .addAll(buildNotificationEntries(processStartDate, processEndDate, entry.getValue()));
        }

        // Process AdHoc
        for (Entry<Account, List<InterestBearingEntryWrapper>> entry : interestBearingAdHocEntriesByAccount.entrySet()) {
            this.santInterestEntries
                    .addAll(buildNotificationEntries(processStartDate, processEndDate, entry.getValue()));
        }

    }

    protected void buildInterestBearingentryMap(
            Map<Account, List<InterestBearingEntryWrapper>> interestBearingAdHocEntriesByAccount,
            Map<Trade, List<InterestBearingEntryWrapper>> interestBearingByTrade, final Trade trade,
            final Map<Integer, MarginCallConfigLight> mccLights, final Map<Integer, Account> accounts,
            final Vector<String> contractTypesV, Vector<String> poNameV, Vector<String> currencyV) {

        if(Util.isEmpty(accounts)){
            return; //TODO load all accounts again
        }
        final InterestBearing interestBearing = (InterestBearing) trade.getProduct();
        final Account acc = accounts.get(interestBearing.getAccountId());
        if (acc == null) {
            return;
        }
        if (!Util.isEmpty(currencyV) && !currencyV.contains(acc.getCurrency())) {
            return;
        }
        final String contractId = acc.getAccountProperty("MARGIN_CALL_CONTRACT");
        if (Util.isEmpty(contractId)) {
            return;
        }
        final MarginCallConfigLight mccLight = mccLights.get(Integer.valueOf(contractId));

        if (!Util.isEmpty(contractTypesV) && !contractTypesV.contains(mccLight.getContractType())) {
            return;
        }
        if (!Util.isEmpty(poNameV) && !poNameV.contains(mccLight.getPoName())) {
            return;
        }

        boolean isAdHoc = getAdHoc(acc);

        if (isAdHoc) {
            if (!interestBearingAdHocEntriesByAccount.containsKey(acc)) {
                interestBearingAdHocEntriesByAccount.put(acc, new ArrayList<InterestBearingEntryWrapper>());
            }
            interestBearingAdHocEntriesByAccount.get(acc).addAll(
                    buildInterestBearingEntryWrapperList(trade, interestBearing, mccLight, acc, isAdHoc));
        } else {
            interestBearingByTrade.put(trade,
                    buildInterestBearingEntryWrapperList(trade, interestBearing, mccLight, acc, isAdHoc));
        }

    }

    @SuppressWarnings("unchecked")
    private List<InterestBearingEntryWrapper> buildInterestBearingEntryWrapperList(Trade trade,
                                                                                   InterestBearing interestBearing, MarginCallConfigLight mccLight, Account acc, boolean isAdHoc) {
        List<InterestBearingEntryWrapper> entriesWrapped = new ArrayList<InterestBearingEntryWrapper>();

        List<InterestBearingEntry> entries = new ArrayList<InterestBearingEntry>(interestBearing.getEntries());

        for (InterestBearingEntry entry : entries) {
            entriesWrapped.add(new InterestBearingEntryWrapper(trade, interestBearing, entry, mccLight, acc, isAdHoc));
        }
        return entriesWrapped;
    }

    protected Collection<SantInterestNotificationEntry> buildNotificationEntries(final JDate processStartDate,
                                                                               final JDate processEndDate, List<InterestBearingEntryWrapper> ibEntriesWrapperList) {

        final List<SantInterestNotificationEntry> localEntries = new ArrayList<SantInterestNotificationEntry>();

        double previousPositionAmount = 0.0d;
        double totalAccrual = 0.0;

        Map<JDate, InterestPosEntryWrapped> mergedWrappedEntries = new HashMap<JDate, InterestPosEntryWrapped>();
        for (InterestBearingEntryWrapper ibWrapper : ibEntriesWrapperList) {
            mergeWrappedEntry(mergedWrappedEntries, ibWrapper);

        }

        final SortedSet<JDate> sortedEntriesDates = new TreeSet<JDate>(mergedWrappedEntries.keySet());

        boolean firstRow = true;
        double movementAmount = 0;

        for (final JDate d : sortedEntriesDates) {
            if (d.after(processEndDate)) {
                continue;
            }
            final InterestPosEntryWrapped entry = mergedWrappedEntries.get(d);

            final InterestBearingEntryWrapper interestEntryWrapped = entry.getInterest();
            final InterestBearingEntryWrapper positionEntryWrapped = entry.getPos();
            final InterestBearingEntryWrapper adjustmentEntryWrapped = entry.getAdjustment();

            // Previous
            final InterestPosEntryWrapped previousEntry = mergedWrappedEntries.get(d.addDays(-1));

            InterestBearingEntryWrapper partialSettleEntry = null;
            if (previousEntry != null) {
                partialSettleEntry = previousEntry.getPartialSettle();
            }
            double interestAmount = 0.0d;
            double positionAmount = 0.0d;
            double adjustmentAmount = 0.0d;
            if (interestEntryWrapped != null) {
                interestAmount = interestEntryWrapped.getEntry().getAmount()
                        + interestEntryWrapped.getEntry().getAdjustment();
            }
            if (positionEntryWrapped != null) {
                positionAmount = positionEntryWrapped.getEntry().getAmount();
            }
            if (adjustmentEntryWrapped != null) {
                adjustmentAmount = adjustmentEntryWrapped.getEntry().getAmount();
            }
            if (partialSettleEntry != null) {
                totalAccrual = 0.0d;
            }

            if (d.before(processStartDate)) {
                totalAccrual += interestAmount;
                totalAccrual += adjustmentAmount;
                continue;
            }

            if ((interestEntryWrapped == null) || (positionEntryWrapped == null)) {
                continue;
            }

            Account account = interestEntryWrapped.getAccount();
            Trade trade = interestEntryWrapped.getTrade();
            MarginCallConfigLight mccLight = interestEntryWrapped.getMccLight();

            if (firstRow) {
                @SuppressWarnings("deprecation") final InventoryCashPosition invCashPosition = BOPositionUtil.getCashPosition(account.getCurrency(),
                        BOPositionUtil.CLIENT, BOPositionUtil.ACTUAL, BOPositionUtil.SETTLE_DATE, d, 0,
                        account.getId(), DSConnection.getDefault(), null);
                if (invCashPosition != null) {
                    if (invCashPosition.getPositionDate().equals(d)) {
                        movementAmount = invCashPosition.getDailyChange();
                    } else {
                        movementAmount = 0;
                    }
                } else {
                    movementAmount = 0;
                }
            } else {
                movementAmount = positionAmount - previousPositionAmount;
            }

            String indexName = getEntryIndexName(interestEntryWrapped.getEntry());

            final SantInterestNotificationEntry santEntry = new SantInterestNotificationEntry();
            santEntry.setTradeId(trade.getLongId());
            santEntry.setIndexName(indexName);
            // GSM: Call account name fix
            santEntry.setCallAccountName(account.getExternalName()); // getName()); old
            santEntry.setCallAccountId(account.getId());
            santEntry.setAdHoc(interestEntryWrapped.isAdHoc());
            santEntry.setContractName(mccLight.getDescription());
            santEntry.setPoName(mccLight.getPoName());
            santEntry.setContractId(mccLight.getId());
            santEntry.setDate(interestEntryWrapped.getEntry().getEntryDate());
            santEntry.setMovement(movementAmount);
            santEntry.setCurrency(account.getCurrency());
            santEntry.setPrincipal(positionEntryWrapped.getEntry().getAmount());
            santEntry.setRate(interestEntryWrapped.getEntry().getRate());
            santEntry.setSpread(interestEntryWrapped.getEntry().getSpread());
            santEntry.setAdjustedRate(interestEntryWrapped.getEntry().getRate()
                    + interestEntryWrapped.getEntry().getSpread());
            santEntry.setDailyAccrual(interestEntryWrapped.getEntry().getAmount() + interestEntryWrapped.getEntry().getAdjustment());

            santEntry.setWatchInterest(account.getAccountProperty("WatchInterest"));

            santEntry.setCounterparty(trade.getCounterParty());
            santEntry.setContractType(mccLight.getContractType());

            totalAccrual += interestAmount;
            totalAccrual += adjustmentAmount;

            santEntry.setTotalAccrual(totalAccrual);

            previousPositionAmount = positionAmount;
            firstRow = false;

            localEntries.add(santEntry);
        }

        return localEntries;
    }

    protected void mergeWrappedEntry(Map<JDate, InterestPosEntryWrapped> mergedWrappedEntries,
                                   InterestBearingEntryWrapper ibWrapper) {

        String entryType = ibWrapper.getEntry().getEntryType();
        JDate entryDate = ibWrapper.getEntry().getEntryDate();

        InterestPosEntryWrapped intPosWrapped = null;

        if (InterestBearingEntry.INTEREST.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setInterest(ibWrapper);
        } else if (InterestBearingEntry.POSITION.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setPos(ibWrapper);
        } else if (InterestBearingEntry.ADJUSTMENT.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setAdjustment(ibWrapper);
        } else if (InterestBearingEntry.PARTIAL_SETTLE.equals(entryType)) {
            intPosWrapped = mergedWrappedEntries.get(entryDate);
            if (intPosWrapped == null) {
                intPosWrapped = new InterestPosEntryWrapped(entryDate);
                mergedWrappedEntries.put(intPosWrapped.getEntryDate(), intPosWrapped);
            }
            intPosWrapped.setPartialSettle(ibWrapper);
        }
    }

    private boolean getAdHoc(Account account) {
        String adHoc = account.getAccountProperty("PayInterestAdHoc");
        if (Util.isEmpty(adHoc)) {
            return false;
        }

        if (adHoc.equalsIgnoreCase("true")) {
            return true;
        }

        return false;

    }

    protected String getEntryIndexName(InterestBearingEntry entry) {
        final AccountInterestConfig config = BOCache.getAccountInterestConfig(DSConnection.getDefault(),
                entry.getConfigId());

        if ((config.getRanges() != null)) {
            for (int i = 0; i < config.getRanges().size(); i++) {
                AccountInterestConfigRange range = (AccountInterestConfigRange) config.getRanges().get(i);
                if ((range == null) || range.isFixed() || (range.getId() != entry.getRangeId())) {
                    continue;
                }
                if (range.getRateIndex() == null) {
                    continue;
                }
                return range.getRateIndex().toStringNoSource();
            }
        }
        return null;
    }

    protected class InterestBearingEntryWrapper {

        private final InterestBearingEntry entry;
        private final Trade trade;
        private final MarginCallConfigLight mccLight;
        private final Account account;
        private final InterestBearing interestBearing;
        private final boolean isAdHoc;

        public InterestBearingEntryWrapper(Trade trade, InterestBearing interestBearing, InterestBearingEntry entry,
                                           MarginCallConfigLight mccLight, Account account, boolean isAdHoc) {
            this.trade = trade;
            this.interestBearing = interestBearing;
            this.mccLight = mccLight;
            this.account = account;
            this.entry = entry;
            this.isAdHoc = isAdHoc;

        }

        public boolean isAdHoc() {
            return this.isAdHoc;
        }

        public InterestBearingEntry getEntry() {
            return this.entry;
        }

        public Trade getTrade() {
            return this.trade;
        }

        public MarginCallConfigLight getMccLight() {
            return this.mccLight;
        }

        public Account getAccount() {
            return this.account;
        }

        public InterestBearing getInterestBearing() {
            return this.interestBearing;
        }
    }

    protected class InterestPosEntryWrapped {

        private InterestBearingEntryWrapper pos;
        private InterestBearingEntryWrapper interest;
        private InterestBearingEntryWrapper adjustment;
        private InterestBearingEntryWrapper partialSettle;
        private final JDate entryDate;

        public InterestPosEntryWrapped(JDate entryDate) {
            this.entryDate = entryDate;
        }

        public JDate getEntryDate() {
            return this.entryDate;
        }

        public InterestBearingEntryWrapper getPos() {
            return this.pos;
        }

        public void setPos(final InterestBearingEntryWrapper pos) {
            this.pos = pos;
        }

        public InterestBearingEntryWrapper getInterest() {
            return this.interest;
        }

        public void setInterest(final InterestBearingEntryWrapper interest) {
            this.interest = interest;
        }

        public void setAdjustment(InterestBearingEntryWrapper adjustment) {
            this.adjustment = adjustment;
        }

        public InterestBearingEntryWrapper getAdjustment() {
            return this.adjustment;
        }

        public InterestBearingEntryWrapper getPartialSettle() {
            return this.partialSettle;
        }

        public void setPartialSettle(InterestBearingEntryWrapper partialSettle) {
            this.partialSettle = partialSettle;
        }
    }
}
