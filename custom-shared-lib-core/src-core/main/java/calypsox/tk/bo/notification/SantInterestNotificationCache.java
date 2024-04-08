/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.notification;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SantInterestNotificationCache {

    private final DSConnection ds;

    // ################## FIELDS USE FOR CACHING #################//

    private Trade currentTrade = null;
    private Account account = null;
    private Boolean isAdHoc = null;
    private CollateralConfig mcc = null;
    private String interestRate = null;
    private List<InterestBearing> ibList = new ArrayList<>();
    private JDate startDate = null;
    private JDate endDate = null;

    // ####################################################//

    public SantInterestNotificationCache() {
        this.ds = DSConnection.getDefault();
    }

    private void initCache(Trade trade) {
        if ((this.currentTrade == null) || (this.currentTrade.getLongId() != trade.getLongId())) {
            this.currentTrade = trade;

            this.account = null;
            this.isAdHoc = null;
            this.mcc = null;
            this.interestRate = null;
            this.startDate = null;
            this.endDate = null;
            this.ibList.clear();

            loadCache();

        }

    }

    public DSConnection getDs() {
        return this.ds;
    }

    public Account getAccount(Trade trade) {
        initCache(trade);
        return this.account;
    }

    public Boolean isAdHoc(Trade trade) {
        initCache(trade);
        return this.isAdHoc;
    }

    public CollateralConfig getMarginCallConfig(Trade trade) {
        initCache(trade);
        return this.mcc;
    }

    public String getInterestRate(Trade trade) {
        initCache(trade);
        return this.interestRate;
    }

    public List<InterestBearing> getInterestBearings(Trade trade) {
        initCache(trade);
        return this.ibList;
    }

    public JDate getStartDate(Trade trade) {
        initCache(trade);
        return this.startDate;
    }

    public JDate getEndDate(Trade trade) {
        initCache(trade);
        return this.endDate;
    }

    private void loadCache() {
        // The following sequence has to be respected in order to initialise the cache correctly

        // Account
        this.account = fetchAccount();
        // AdHoc
        this.isAdHoc = fetchAdHoc();
        // MarginCall Contract
        this.mcc = fetchContract();
        // Interest Rate
        this.interestRate = fetchInterestRate();
        // Interest Bearings
        this.ibList = fetchInterestBearings();
        // Start Date
        this.startDate = fetchStartDate();
        // End Date
        this.endDate = fetchEndDate();

    }

    private JDate fetchEndDate() {
        InterestBearing ib = this.ibList.get(this.ibList.size() - 1);

        JDate localEndDate = ib.getEndDate();
        if (this.isAdHoc) {
            localEndDate = this.currentTrade.getSettleDate();
        }

        return localEndDate;
    }

    @SuppressWarnings("unchecked")
    private JDate fetchStartDate() {
        InterestBearing ib = this.ibList.get(0);

        JDate localStartDate = ib.getStartDate();
        if (!this.isAdHoc) {
            return localStartDate;
        }

        Vector<InterestBearingEntry> ibEntries = ib.getEntries();
        // find the next to last partial settle (=> last partial settle before the current settle date)
        // assumption - entries sorted by value date ASC
        for (InterestBearingEntry entry : ibEntries) {
            if (!InterestBearingEntry.PARTIAL_SETTLE.equals(entry.getEntryType())) {
                continue;
            }
            if (entry.getValueDate().after(localStartDate)
                    && entry.getValueDate().before(this.currentTrade.getSettleDate())) {
                localStartDate = entry.getValueDate().addDays(1);
            }
        }
        return localStartDate;
    }

    private String fetchInterestRate() {
        if (!Util.isEmpty(this.account.getAccountInterests())) {
            AccountInterests accInterests = this.account.getAccountInterests().get(0);
            final AccountInterestConfig config = BOCache.getAccountInterestConfig(this.ds, accInterests.getConfigId());
            if (!Util.isEmpty(config.getRanges())) {
                final AccountInterestConfigRange range = (AccountInterestConfigRange) config.getRanges().get(0);
                if (range != null) {
                    if (range.isFixed()) {
                        return "Fixed Rate " + new Rate(range.getFixedRate()) + "%";
                    } else {
                        if (range.getRateIndex() != null) {
                            return new StringBuilder(range.getRateIndex().toStringNoSource()).append(" + ")
                                    .append(range.getSpread()).toString();
                        }
                    }
                }
            }
        }
        return null;
    }

    private CollateralConfig fetchContract() {
        try {
            final int mccId = Integer.parseInt(this.account.getAccountProperty("MARGIN_CALL_CONTRACT"));
            return CacheCollateralClient.getCollateralConfig(this.ds, mccId);
        } catch (final Exception e) {
            Log.error(this, e);
        }
        return null;
    }

    private Boolean fetchAdHoc() {
        String adHoc = this.account.getAccountProperty("PayInterestAdHoc");
        if (Util.isEmpty(adHoc) || !adHoc.equalsIgnoreCase("true")) {
            return false;
        }
        return true;

    }

    private Account fetchAccount() {
        long accountId = ((SimpleTransfer) this.currentTrade.getProduct()).getLinkedLongId();
        //MIG V16
        if (accountId == 0 && this.currentTrade.getProduct() instanceof CustomerTransfer) {
            List<InterestBearing> ibBearing = loadSingleTrade();
            if (!Util.isEmpty(ibBearing)) {
                accountId = ibBearing.get(0).getAccountId();
            }
        }
        return BOCache.getAccount(this.ds, Math.toIntExact(accountId));
    }

    private List<InterestBearing> fetchInterestBearings() {
        if (!this.isAdHoc) {
            return loadSingleTrade();
        }

        return loadMultipleTrades();

    }

    private List<InterestBearing> loadSingleTrade() {
        List<InterestBearing> localList = new ArrayList<>();

        Trade ibTrade = null;
        try {
            ibTrade = this.ds.getRemoteTrade().getTrade(this.currentTrade.getKeywordAsLongId("INTEREST_TRANSFER_FROM"));
        } catch (Exception e) {
            Log.error(this, "Cannot load interest bearing trade", e);
        }
        if (ibTrade != null) {
            InterestBearing ib = (InterestBearing) ibTrade.getProduct();
            localList.add(ib);
        }
        return localList;
    }

    @SuppressWarnings("unchecked")
    private List<InterestBearing> loadMultipleTrades() {
        List<InterestBearing> localList = loadSingleTrade();

        Vector<InterestBearingEntry> entries = localList.get(0).getEntries();
        for (InterestBearingEntry entry : entries) {
            if (!InterestBearingEntry.PARTIAL_SETTLE.equals(entry.getEntryType())) {
                continue;
            }
            // try to find a PARTIAL_SETTLE on the same IB before settle date where there is another PARTIAL_SETTLE
            // if found then no need to retrieve the previous IB
            if (entry.getValueDate().before(this.currentTrade.getSettleDate())) {
                return localList;
            }
        }

        localList = new ArrayList<InterestBearing>();

        // Load all interest bearing trades for this account order by payment date ASC
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();

        from.append(" product_int_bearing ");

        where.append(" trade.product_id = product_int_bearing.product_id ");
        where.append(" and trade.trade_status != 'CANCELED' ");
        where.append(" and product_int_bearing.account_id = ");
        where.append(this.account.getId());

        TradeArray ibTrades = null;
        try {
            ibTrades = DSConnection.getDefault().getRemoteTrade()
                    .getTrades(from.toString(), where.toString(), "product_int_bearing.payment_date ASC", true, null);
        } catch (final RemoteException e) {
            Log.error(this, "Cannot load trades", e);
            return localList;
        }

        // Case of the first Payment AdHoc - only one PARTIAL_SETTLE (the current one)
        if (ibTrades.size() == 1) {
            InterestBearing ib1 = (InterestBearing) ibTrades.get(0).getProduct();
            localList.add(ib1);
            return localList;
        }

        // Case of 2 IB
        InterestBearing ibInit = null;
        InterestBearing ibFinal = null;

        if (ibTrades.size() == 2) {
            ibInit = (InterestBearing) ibTrades.get(0).getProduct();
            ibFinal = (InterestBearing) ibTrades.get(1).getProduct();
            localList.add(ibInit);
            localList.add(ibFinal);
            return localList;
        }

        // More than 2 IB
        ibInit = (InterestBearing) ibTrades.get(0).getProduct();
        localList.add(ibInit);

        for (int i = 1; i < (ibTrades.size() - 1); i++) {
            InterestBearing ib = (InterestBearing) this.currentTrade.getProduct();

            // Add current IB to the map
            localList.add(ib);

            entries = ib.getEntries();
            for (InterestBearingEntry entry : entries) {
                if (InterestBearingEntry.PARTIAL_SETTLE.equals(entry.getEntryType())) {
                    // PARTIAL_SETTLE found => new baseline of the map
                    // Reset the map with the current IB
                    localList.clear();
                    localList.add(ib);
                    break;
                }
            }
        }

        // Add the last one
        ibFinal = (InterestBearing) ibTrades.get(ibTrades.size() - 1).getProduct();
        localList.add(ibFinal);

        return localList;
    }

}
