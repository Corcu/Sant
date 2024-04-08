/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantExcludedTradeLoader;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class SantListOfTradesWithNoMtmVariationReport extends SantReport {

    public static final String TEMPLATE_PROPERTY_PRODUCT_TYPE = "PRODUCT_TYPE";

    // On this report we use process start date, this date corresponds to the value date on the collateral manager
    protected JDate currentValueDate = null;

    private static final long serialVersionUID = -5170664608446611447L;

    private static final int MAX_ITEMS_IN_CLAUSE = 1000;

    private final Map<Integer, Double> mtmMap = new HashMap<Integer, Double>();

    private static final Vector<String> excludedProducts = Util.string2Vector("CONTRACT_IA,DISPUTE_ADJUSTMENT");

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @Override
    public ReportOutput loadReport(final Vector<String> errors) {

        DefaultReportOutput reportOutput = new DefaultReportOutput(this);
        this.currentValueDate = getProcessStartDate();

        loadPricingEnv();

        this.mtmMap.clear();
        String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);

        if (!Util.isEmpty(agreementIds)) {
            Vector<Integer> agreementIdsV = Util.string2IntVector(agreementIds);
            if (agreementIdsV.size() >= 1000) {
                errors.add("Select less than 1000 contracts");
                return reportOutput;
            }
        }

        List<Trade> trades = loadTrades(agreementIds, errors);

        if (errors.size() > 0) {
            return reportOutput;
        }

        Set<Long> tradeIds = getTradeIds(trades);

        SantExcludedTradeLoader excludeLoader = new SantExcludedTradeLoader(false, tradeIds, this.currentValueDate);
        excludeLoader.load();
        Map<Long, Boolean> excludedMap = excludeLoader.getDataAsMap();

        String productType = (String) getReportTemplate().get(
                TEMPLATE_PROPERTY_PRODUCT_TYPE);
        String agreementType = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);

        String dealOwnerIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.OWNER_DEALS);

        // 03/08/15. SBNA Multi-PO filter
        final Set<String> posIdsAllowed = new HashSet<>(Util.string2Vector(CollateralUtilities
                .filterPoIdsByTemplate(getReportTemplate())));

        Vector<Integer> dealOwnerIdsV = new Vector<>();
        if (!Util.isEmpty(dealOwnerIds)) {
            dealOwnerIdsV = Util.string2IntVector(dealOwnerIds);
        }

        List<ReportRow> rows = new ArrayList<>();

        for (Trade trade : trades) {
            ReportRow row = getRow(trade, productType, agreementType, dealOwnerIdsV, excludedMap, posIdsAllowed);
            if (row != null) {
                rows.add(row);
            }
        }

        reportOutput.setRows(rows.toArray(new ReportRow[rows.size()]));
        return reportOutput;
    }

    private void loadPricingEnv() {
        PricingEnv pricingEnv = AppUtil.loadPE("OFFICIAL", new JDatetime(this.currentValueDate, TimeZone.getDefault()));
        setPricingEnv(pricingEnv);
    }

    private Set<Long> getTradeIds(List<Trade> trades) {
        Set<Long> tradeIds = new HashSet<>();
        for (Trade trade : trades) {
            tradeIds.add(trade.getLongId());
        }
        return tradeIds;
    }

    private ReportRow getRow(Trade trade, String productType, String agreementType, Vector<Integer> dealOwnerIdsV,
                             Map<Long, Boolean> excludedMap, Set<String> posIdsAllowed) {

        if (!Util.isEmpty(productType) && !trade.getProductType().equals(productType)) {
            return null;
        }
        if ((dealOwnerIdsV.size() > 0) && !dealOwnerIdsV.contains(trade.getBook().getProcessingOrgBasedId())) {
            return null;
        }
        int mccId = 0;
        if (!Util.isEmpty(trade.getInternalReference())) {
            mccId = Integer.valueOf(trade.getInternalReference());
        }

        final MarginCallConfig mcc = BOCache.getMarginCallConfig(getDSConnection(), mccId);

        if ((mcc == null) || (!Util.isEmpty(agreementType) && !agreementType.equals(mcc.getContractType()))) {
            return null;
        }

        if (excludedProducts.contains(trade.getProductSubType())) {
            return null;
        }

        // 03/08/15. SBNA Multi-PO filter
        LegalEntity po = mcc.getProcessingOrg();
        if (!posIdsAllowed.contains("" + po.getId())) {
            return null;
        }

        boolean isExcluded = isExcluded(trade, excludedMap);

        ReportRow row = new ReportRow(trade, ReportRow.TRADE);

        row.setProperty("ProcessDate", this.currentValueDate);
        row.setProperty(ReportRow.MARGIN_CALL_CONFIG, mcc);
        Double mtm = this.mtmMap.get(trade.getLongId());
        row.setProperty("MTM", mtm);
        try {
            row.setProperty("MTM_BASE", getMTMBase(mtm, trade.getProduct().getCurrency(), mcc.getCurrency()));
        } catch (Exception exc) {
            Log.error(this, exc); //sonar
        }
        row.setProperty("Excluded", isExcluded);

        return row;
    }

    private Double getMTMBase(Double mtm, String contractCurrency, String tradeCurrency) {
        if (contractCurrency.equals(tradeCurrency)) {
            return mtm;
        }

        QuoteValue qv = null;
        try {
            qv = getPricingEnv().getFXQuote(contractCurrency, tradeCurrency, this.currentValueDate);
        } catch (MarketDataException e) {
            Log.error(this, "Cannot retrieve FX Quote for " + contractCurrency + "/" + tradeCurrency);
            Log.error(this, e); //sonar
        }
        if ((qv == null) || (qv.getClose() == Double.NaN) || (qv.getClose() == 0)) {
            return Double.NaN;
        }

        return mtm * qv.getClose();
    }

    private boolean isExcluded(Trade trade, Map<Long, Boolean> excludedMap) {
        if ((excludedMap.get(trade.getLongId()) != null) && excludedMap.get(trade.getLongId()).equals(Boolean.TRUE)) {
            return true;
        }
        if (Status.valueOf("CHECKED").equals(trade.getStatus())) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Trade> loadTrades(String agreementIds, Vector<String> errors) {
        List<Trade> trades = new ArrayList<Trade>();

        JDate previousValueDate = this.currentValueDate.addBusinessDays(-1, getReportTemplate().getHolidays());

        StringBuilder query = new StringBuilder(
                " select trade.trade_id,  mv1.mark_value from trade, product_desc, pl_mark m1, pl_mark m2,  pl_mark_value mv1, pl_mark_value mv2 ");
        query.append(" where m1.mark_id = mv1.mark_id ");
        query.append(" and m2.mark_id = mv2.mark_id ");
        query.append(" and m1.trade_id = m2.trade_id ");
        query.append(" and trade.trade_id = m1.trade_id ");
        query.append(" and trade.product_id = product_desc.product_id ");
        query.append(" and trade.trade_status not in ('CANCELED') ");
        query.append(" and (product_desc.maturity_date is null or product_desc.maturity_date >=  ")
                .append(Util.date2SQLString(this.currentValueDate)).append(" ) ");
        query.append(" and mv1.mark_name = 'NPV' ");
        query.append(" and mv2.mark_name = 'NPV' ");
        query.append(" and m1.valuation_date =  ").append(Util.date2SQLString(this.currentValueDate));
        query.append(" and m2.valuation_date =  ").append(Util.date2SQLString(previousValueDate));
        query.append(" and mv1.mark_value = mv2.mark_value ");

        if (!Util.isEmpty(agreementIds)) {
            query.append(" and trade.internal_reference in (").append(agreementIds).append(")");
        }

        try {
            Vector<?> results = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(
                    query.toString());
            if (results.size() <= 2) {
                return trades;
            }

            for (int i = 2; i < results.size(); i++) {
                Vector<?> row = (Vector<?>) results.get(i);
                Integer tradeId = Integer.valueOf((String) row.get(0));
                Double mtm = Double.valueOf((String) row.get(1));
                this.mtmMap.put(tradeId, mtm);
            }

            List<Integer> tradeIdsList = new ArrayList<Integer>(this.mtmMap.keySet());
            int start = 0;
            for (int i = 0; i <= (tradeIdsList.size() / MAX_ITEMS_IN_CLAUSE); i++) {
                int end = (i + 1) * MAX_ITEMS_IN_CLAUSE;
                if (end > tradeIdsList.size()) {
                    end = tradeIdsList.size();
                }
                final List<Integer> subList = tradeIdsList.subList(start, end);
                start = end;
                StringBuilder whereClause = new StringBuilder(" trade.trade_id  in (");
                whereClause.append(Util.collectionToString(subList));
                whereClause.append(")");

                trades.addAll(DSConnection.getDefault().getRemoteTrade()
                        .getTrades(null, whereClause.toString(), " trade.trade_id ", null).toVector());
            }

        } catch (RemoteException e) {
            Log.error(this, "Could not load trades from database", e);
            errors.add(e.getMessage());

        }
        return trades;
    }

}
