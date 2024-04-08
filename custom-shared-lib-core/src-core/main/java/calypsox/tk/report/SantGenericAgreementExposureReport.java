/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.SantMarginCallDetailEntriesLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import org.jfree.util.Log;

import java.util.*;

public class SantGenericAgreementExposureReport extends SantReport {

    private static final long serialVersionUID = 3672641502835178986L;
    public static final String TYPE = "SantGenericAgreementExposure";

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput loadReport(final Vector errorMsgs) {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final SantMarginCallDetailEntriesLoader smcael = new SantMarginCallDetailEntriesLoader();

        if (this._reportTemplate == null) {
            return null;
        }

        // Eco sector
        final String ecoSector = (String) this._reportTemplate.get(SantGenericTradeReportTemplate.ECONOMIC_SECTOR);

        // Mature deals
        boolean matureDeals;
        if (getReportTemplate().get(SantGenericTradeReportTemplate.MATURE_DEALS).toString().equals("false")) {
            matureDeals = false;
        } else {
            matureDeals = true;
        }

        try {
            final List<SantMarginCallDetailEntry> santEntriesWithoutFilter = smcael.load(this._reportTemplate,
                    getValDate());

            List<SantMarginCallDetailEntry> santEntriesWithoutFilterAux = new ArrayList<SantMarginCallDetailEntry>();
            List<SantMarginCallDetailEntry> santEntries = new ArrayList<SantMarginCallDetailEntry>();

            // Filter Economic Sector
            if ((ecoSector != null) && !ecoSector.equals("")) {
                for (int i = 0; i < santEntriesWithoutFilter.size(); i++) {
                    if (santEntriesWithoutFilter.get(i).getMarginCallConfig().getAdditionalField("ECONOMIC_SECTOR")
                            .equals(ecoSector)) {
                        santEntriesWithoutFilterAux.add(santEntriesWithoutFilter.get(i));
                    }
                }
            } else {
                santEntriesWithoutFilterAux = santEntriesWithoutFilter;
            }

            // Filter Mature deals
            if (matureDeals) {
                santEntries = santEntriesWithoutFilterAux;
            } else {
                for (int i = 0; i < santEntriesWithoutFilterAux.size(); i++) {
                    if ((santEntriesWithoutFilterAux.get(i).getTrade().getMaturityDate() == null)
                            || !santEntriesWithoutFilterAux.get(i).getTrade().getMaturityDate()
                            .before(getProcessEndDate())) {
                        santEntries.add(santEntriesWithoutFilterAux.get(i));
                    }
                }
            }

            // GetReportRows
            final List<ReportRow> rows = getReportRows(santEntries);
            output.setRows(rows.toArray(new ReportRow[rows.size()]));
            return output;

        } catch (final Exception e) {
            String error = "Error retrieving the margin call detail entries\n";
            Log.error(this, e);
            errorMsgs.add(error + e.getMessage());

        }
        return null;
    }

    private List<ReportRow> getReportRows(final List<SantMarginCallDetailEntry> santEntries) {
        final Collection<Long> tradeIds = new HashSet<>();
        final ArrayList<ReportRow> reportRows = new ArrayList<>();
        for (int i = 0; i < santEntries.size(); i++) {

            if ((tradeIds.isEmpty())
                    || ((santEntries.get(i).getTrade() != null) && !tradeIds.contains(santEntries.get(i).getTrade()
                    .getLongId()))) {

                for (int j = i + 1; j < santEntries.size(); j++) {
                    if ((santEntries.get(i).getMarginCallConfig().getId() == santEntries.get(j).getMarginCallConfig()
                            .getId())
                            && (santEntries.get(i).getTrade() != null)
                            && (santEntries.get(j).getTrade() != null)
                            && (santEntries.get(i).getTrade().getLongId() == santEntries.get(j).getTrade().getLongId())) {

                        // Create row for 2 SantEntries
                        createRow(reportRows, santEntries.get(i), santEntries.get(j));
                        tradeIds.add(santEntries.get(i).getTrade().getLongId());
                        j = santEntries.size();

                    } else if ((j == (santEntries.size() - 1)) && (santEntries.get(i).getTrade() != null)
                            && (santEntries.get(j).getTrade() != null)
                            && (santEntries.get(i).getTrade().getLongId() != santEntries.get(j).getTrade().getLongId())) {

                        // Create row con solo un valor! S?lo con i. Tengo que
                        // saber si es Previous o current
                        if (santEntries.get(i).getDetailEntry().getProcessDatetime().getJDate(TimeZone.getDefault())
                                .equals(getProcessStartDate())) {
                            createRow(reportRows, santEntries.get(i), null);
                            tradeIds.add(santEntries.get(i).getTrade().getLongId());
                        } else if (santEntries.get(i).getDetailEntry().getProcessDatetime().getJDate(TimeZone.getDefault())
                                .equals(getProcessEndDate())) {
                            createRow(reportRows, null, santEntries.get(i));
                            tradeIds.add(santEntries.get(i).getTrade().getLongId());
                        }
                    }
                }
                if (i == (santEntries.size() - 1)) {
                    if ((tradeIds.isEmpty())
                            || ((santEntries.get(i).getTrade() != null) && !tradeIds.contains(santEntries.get(i)
                            .getTrade().getLongId()))) {
                        createRow(reportRows, null, santEntries.get(i));
                        tradeIds.add(santEntries.get(i).getTrade().getLongId());
                    }
                }
            }
        }

        return reportRows;
    }

    private void createRow(final ArrayList<ReportRow> reportRows,
                           final SantMarginCallDetailEntry previousSantMarginCallDetailEntry,
                           final SantMarginCallDetailEntry currentSantMarginCallDetailEntry) {

        if ((previousSantMarginCallDetailEntry != null) && (currentSantMarginCallDetailEntry != null)) {
            double currentMtm = 0;
            double previousMtm = 0;
            if (currentSantMarginCallDetailEntry.getDetailEntry().getMeasure(
                    SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)) != null) {
                currentMtm = currentSantMarginCallDetailEntry.getDetailEntry()
                        .getMeasure(SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)).getValue();
            }
            if (previousSantMarginCallDetailEntry.getDetailEntry().getMeasure(
                    SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)) != null) {
                previousMtm = previousSantMarginCallDetailEntry.getDetailEntry()
                        .getMeasure(SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)).getValue();
            }
            setReportRows(currentSantMarginCallDetailEntry, previousMtm, currentMtm, reportRows);

        } else if ((previousSantMarginCallDetailEntry != null) && (currentSantMarginCallDetailEntry == null)) {
            double currentMtm = 0;
            double previousMtm = 0;
            if (previousSantMarginCallDetailEntry.getDetailEntry().getMeasure(
                    SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)) != null) {
                previousMtm = previousSantMarginCallDetailEntry.getDetailEntry()
                        .getMeasure(SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)).getValue();
            }
            if (getProcessEndDate().equals(getProcessStartDate())) {
                currentMtm = previousMtm;
            }

            setReportRows(previousSantMarginCallDetailEntry, previousMtm, currentMtm, reportRows);

        } else if ((previousSantMarginCallDetailEntry == null) && (currentSantMarginCallDetailEntry != null)) {
            double currentMtm = 0;
            double previousMtm = 0;
            if (currentSantMarginCallDetailEntry.getDetailEntry().getMeasure(
                    SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)) != null) {
                currentMtm = currentSantMarginCallDetailEntry.getDetailEntry()
                        .getMeasure(SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE)).getValue();
            }
            if (getProcessEndDate().equals(getProcessStartDate())) {
                previousMtm = currentMtm;
            }

            setReportRows(currentSantMarginCallDetailEntry, previousMtm, currentMtm, reportRows);
        }
    }

    private void setReportRows(final SantMarginCallDetailEntry santMarginCallDetailEntry, final double previousMtm,
                               final double currentMtm, final ArrayList<ReportRow> reportRows) {

        // final ReportRow row = new ReportRow(this.gaei);
        final ReportRow row = new ReportRow(santMarginCallDetailEntry);
        row.setProperty(SantGenericAgreementExposureReportStyle.COLLATERAL_AGREEMENT, santMarginCallDetailEntry
                .getMarginCallConfig().getName());

        row.setProperty(SantGenericAgreementExposureReportStyle.TRADE_ID, santMarginCallDetailEntry.getTrade().getLongId());

        row.setProperty(SantGenericAgreementExposureReportStyle.FRONT_ID, santMarginCallDetailEntry.getTrade()
                .getExternalReference());

        row.setProperty(SantGenericAgreementExposureReportStyle.PORTFOLIO, santMarginCallDetailEntry
                .getMarginCallConfig().getBook());

        row.setProperty(SantGenericAgreementExposureReportStyle.REPORT_DATE, getValDate());

        row.setProperty(SantGenericAgreementExposureReportStyle.CURRENT_DATE, getProcessEndDate());

        row.setProperty(SantGenericAgreementExposureReportStyle.PREVIOUS_DATE, getProcessStartDate());

        row.setProperty(SantGenericAgreementExposureReportStyle.TRADE_DATE,
                getTradeDate(santMarginCallDetailEntry.getTrade()));

        if (santMarginCallDetailEntry.getTrade().getProductType().equals(Product.REPO)) {
            row.setProperty(SantDealAgreementExposureReportStyle.INSTRUMENT,
                    CollateralStaticAttributes.INSTRUMENT_TYPE_REPO);
        } else if (santMarginCallDetailEntry.getTrade().getProductType().equals(Product.SEC_LENDING)) {
            row.setProperty(SantDealAgreementExposureReportStyle.INSTRUMENT,
                    CollateralStaticAttributes.INSTRUMENT_TYPE_SEC_LENDING);
        } else if (santMarginCallDetailEntry.getTrade().getProductType().equals(Product.PERFORMANCESWAP)) {
            row.setProperty(SantDealAgreementExposureReportStyle.INSTRUMENT,
                    CollateralStaticAttributes.INSTRUMENT_TYPE_PERFORMANCESWAP);
        }else{
            row.setProperty(SantDealAgreementExposureReportStyle.INSTRUMENT, santMarginCallDetailEntry.getTrade()
                    .getProductSubType());
        }

        row.setProperty(SantGenericAgreementExposureReportStyle.STRUCTURE, santMarginCallDetailEntry.getTrade()
                .getKeywordValue("STRUCTURE_ID"));

        row.setProperty(SantGenericAgreementExposureReportStyle.COLLATERAL_AGREEMENT_TYPE, santMarginCallDetailEntry
                .getMarginCallConfig().getContractType());

        row.setProperty(SantGenericAgreementExposureReportStyle.COLLATERAL_AGREEMENT_BASE_CCY,
                santMarginCallDetailEntry.getMarginCallConfig().getCurrency());

        row.setProperty(SantGenericAgreementExposureReportStyle.CURRENT_MTM_CCY_AGREE, CollateralUtilities
                .formatAmount(currentMtm, santMarginCallDetailEntry.getMarginCallConfig().getCurrency()));

        row.setProperty(SantGenericAgreementExposureReportStyle.PREVIOUS_MTM_BASE_CCY, CollateralUtilities
                .formatAmount(previousMtm, santMarginCallDetailEntry.getMarginCallConfig().getCurrency()));

        row.setProperty(SantGenericAgreementExposureReportStyle.ECONOMIC_SECTOR, santMarginCallDetailEntry.getTrade()
                .getKeywordValue("ECONOMIC_SECTOR"));

        row.setProperty(SantGenericAgreementExposureReportStyle.COUNTERPARTY, santMarginCallDetailEntry
                .getMarginCallConfig().getLegalEntity().getAuthName());

        row.setProperty(SantGenericAgreementExposureReportStyle.MATURITY, santMarginCallDetailEntry.getTrade()
                .getMaturityDate());

        reportRows.add(row);

    }

    private Object getTradeDate(Trade trade) {
        if (trade.getProductType().equals(Product.SEC_LENDING)) {
            String kw = trade.getKeywordValue("REAL_TRADE_DATE");
            if (!Util.isEmpty(kw)) {
                return JDate.valueOf(kw);
            }
        } else {
            return trade.getTradeDate().getJDate(TimeZone.getDefault());
        }
        return null;
    }
}
