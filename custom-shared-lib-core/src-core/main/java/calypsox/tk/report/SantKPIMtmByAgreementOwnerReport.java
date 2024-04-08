package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericKPIMtmReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SantKPIMtmByAgreementOwnerReport extends SantGenericKPIMtmReport {

    private static final long serialVersionUID = 1L;

    /**
     * This method converts the NPV values to EUR and USD seperately, does the sums and then build KPIMtmReportItem to
     * display in the report
     *
     * @param individualItems
     * @return
     * @throws MarketDataException
     */
    @Override
    public List<KPIMtmReportItem> buildKPIMtmReportItems(final List<KPIMtmIndividualItem> individualItems)
            throws MarketDataException {

        final List<KPIMtmReportItem> kpiMtmReportItems = new ArrayList<KPIMtmReportItem>();
        String prevAgrOwner = "";
        String prevDealOwner = "";
        double USDSum = 0.0;
        double EURSum = 0.0;
        int tradeCount = 0;
        if ((individualItems != null) && (individualItems.size() > 0)) {
            for (final KPIMtmIndividualItem indItem : individualItems) {
                if (prevAgrOwner.length() == 0) {
                    prevAgrOwner = indItem.getAgrOwner();
                    prevDealOwner = indItem.getDealOwner();
                }

                if (!prevAgrOwner.equals(indItem.getAgrOwner()) || !prevDealOwner.equals(indItem.getDealOwner())) {
                    // if ((EURSum != 0.0) && (USDSum != 0.0)) {
                    final KPIMtmReportItem reportItem = new KPIMtmReportItem();
                    reportItem.setAgrOwner(prevAgrOwner);
                    reportItem.setDealOwner(prevDealOwner);
                    reportItem.setTradeCount(tradeCount);
                    reportItem.setEurMTMSum(EURSum);
                    reportItem.setUsdMTMSum(USDSum);
                    kpiMtmReportItems.add(reportItem);
                    // }

                    // reset values
                    EURSum = 0.0;
                    USDSum = 0.0;
                    tradeCount = 0;
                    prevAgrOwner = indItem.getAgrOwner();
                    prevDealOwner = indItem.getDealOwner();
                }

                if ((indItem.getMtmValue() == null) || Double.isNaN(indItem.getMtmValue())) {
                    Log.info(
                            SantKPIMtmByAgreementOwnerReport.class,
                            "NPV_BASE is null for Trade=" + indItem.getTradeId() + ", MCEntryId="
                                    + indItem.getMcEntryId());
                } else {
                    if (indItem.getMtmCurrency().equals("USD")) {
                        USDSum += indItem.getMtmValue();
                    } else {
                        final double convertedAmount = CollateralUtilities.convertCurrency(indItem.getMtmCurrency(),
                                indItem.getMtmValue(), "USD", getValueDate(), getPricingEnv());
                        if (!Double.isNaN(convertedAmount)) {
                            USDSum += convertedAmount;
                        }
                    }

                    if (indItem.getMtmCurrency().equals("EUR")) {
                        EURSum += indItem.getMtmValue();
                    } else {
                        final double convertedAmount = CollateralUtilities.convertCurrency(indItem.getMtmCurrency(),
                                indItem.getMtmValue(), "EUR", getValueDate(), getPricingEnv());
                        EURSum += convertedAmount;
                    }
                }
                tradeCount++;
            }

            // Add the last row Item
            final KPIMtmReportItem reportItem = new KPIMtmReportItem();
            reportItem.setAgrOwner(prevAgrOwner);
            reportItem.setDealOwner(prevDealOwner);
            reportItem.setTradeCount(tradeCount);
            reportItem.setEurMTMSum(EURSum);
            reportItem.setUsdMTMSum(USDSum);
            kpiMtmReportItems.add(reportItem);

        }

        return kpiMtmReportItems;
    }

    @Override
    public String buildQuery() {

        // GSM 03/08/15. SBNA Multi-PO filter. Adaptation to ST filter
        final String agreePOsIDs = CollateralUtilities.filterPoIdsByTemplate(this._reportTemplate);
        Vector<String> agreeVecIds = Util.string2Vector(agreePOsIDs);
        // (String) this._reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

        String query = "select mc.process_org_id contractOwnerID, "
                + "(select short_name from legal_entity where legal_entity_id=mc.process_org_id) contractOwner, "
                + "le.SHORT_NAME DealOwner, t.Trade_id, mc.currency_code, det.collateral_measures , ent.id "
                + "from trade t, product_desc pd, mrgcall_config mc, book b, legal_entity le, MARGIN_CALL_ENTRIES ent, MARGIN_CALL_DETAIL_ENTRIES det "
                + "where t.product_id=pd.product_id and pd.product_sub_type not in ('DISPUTE_ADJUSTMENT', 'CONTRACT_IA') "
                + "and t.book_id=b.book_id AND b.LEGAL_ENTITY_ID=le.LEGAL_ENTITY_ID "
                + "AND mc.mrg_call_def= ent.MCC_ID AND t.trade_id=det.TRADE_ID and det.is_excluded=0 and trunc(ent.PROCESS_DATE)="
                + Util.date2SQLString(getProcessStartDate()) + " and ent.id=det.MC_ENTRY_ID ";

        // GSM 20/07/15. SBNA Multi-PO filter
        if (!Util.isEmpty(agreeVecIds)) {
            query = query + " AND mc.process_org_id IN" + Util.collectionToSQLString(agreeVecIds);
        }

        // if (!Util.isEmpty(agreementPO)) {
        // query = query + " AND mc.process_org_id=" + agreementPO;
        // }

        final String orderBy = " order by  mc.process_org_id , le.SHORT_NAME";

        return query + orderBy;
    }
}