package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericKPIMtmReport;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;

import java.util.ArrayList;
import java.util.List;

public class SantKPIMtmByAgreementReport extends SantGenericKPIMtmReport {

    private static final long serialVersionUID = 1L;

    @Override
    public String buildQuery() {

        final String agreementPO = (String) this._reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

        String query = "select mc.process_org_id contractOwnerID, "
                + " (select short_name from legal_entity where legal_entity_id=mc.process_org_id) contractOwner, "
                + " le.SHORT_NAME DealOwner, t.Trade_id, mc.mrg_call_def, mc.description, mc.currency_code, det.collateral_measures , ent.id  "
                + " from trade t, product_desc pd, mrgcall_config mc, book b, legal_entity le, MARGIN_CALL_ENTRIES ent, MARGIN_CALL_DETAIL_ENTRIES det "
                + " where t.product_id=pd.product_id and pd.product_sub_type not in ('DISPUTE_ADJUSTMENT', 'CONTRACT_IA') AND "
                + " t.book_id=b.book_id AND b.LEGAL_ENTITY_ID=le.LEGAL_ENTITY_ID "
                + " AND mc.mrg_call_def= ent.MCC_ID AND t.trade_id=det.TRADE_ID and det.is_excluded=0 and ent.id=det.MC_ENTRY_ID "
                + " and trunc(ent.PROCESS_DATE)=" + Util.date2SQLString(getProcessStartDate());

        if (!Util.isEmpty(agreementPO)) {
            query = query + " AND mc.process_org_id=" + agreementPO;
        }

        final String orderBy = " order by  mc.mrg_call_def ";

        return query + orderBy;
    }

    @Override
    public List<KPIMtmReportItem> buildKPIMtmReportItems(final List<KPIMtmIndividualItem> individualItems)
            throws MarketDataException {

        final List<KPIMtmReportItem> kpiMtmReportItems = new ArrayList<KPIMtmReportItem>();
        String prevAgrOwner = "";
        String prevDealOwner = "";
        int agreementId = 0;
        String agreementName = "";
        double USDSum = 0.0;
        double EURSum = 0.0;
        if ((individualItems != null) && (individualItems.size() > 0)) {
            for (final KPIMtmIndividualItem indItem : individualItems) {
                if (prevAgrOwner.length() == 0) {
                    prevAgrOwner = indItem.getAgrOwner();
                    prevDealOwner = indItem.getDealOwner();
                    agreementId = indItem.getAgreementId();
                    agreementName = indItem.getAgreementName();
                }

                if (!prevAgrOwner.equals(indItem.getAgrOwner()) || !prevDealOwner.equals(indItem.getDealOwner())
                        || !(agreementId == indItem.getAgreementId())) {
                    final KPIMtmReportItem reportItem = new KPIMtmReportItem();
                    reportItem.setAgrOwner(prevAgrOwner);
                    reportItem.setDealOwner(prevDealOwner);
                    reportItem.setAgreementId(agreementId);
                    reportItem.setAgreementName(agreementName);
                    reportItem.setEurMTMSum(EURSum);
                    reportItem.setUsdMTMSum(USDSum);
                    kpiMtmReportItems.add(reportItem);
                    // }

                    // reset values
                    EURSum = 0.0;
                    USDSum = 0.0;

                    agreementId = indItem.getAgreementId();
                    agreementName = indItem.getAgreementName();

                    prevAgrOwner = indItem.getAgrOwner();
                    prevDealOwner = indItem.getDealOwner();
                }

                if ((indItem.getMtmValue() == null) || Double.isNaN(indItem.getMtmValue())) {
                    Log.info(SantKPIMtmByAgreementReport.class, "NPV_BASE is null for Trade=" + indItem.getTradeId()
                            + ", MCEntryId=" + indItem.getMcEntryId());
                } else {
                    if (indItem.getMtmCurrency().equals("USD")) {
                        USDSum += indItem.getMtmValue();
                    } else {
                        final double convertedAmount = CollateralUtilities.convertCurrency(indItem.getMtmCurrency(),
                                indItem.getMtmValue(), "USD", getValDate(), getPricingEnv());
                        USDSum += convertedAmount;
                    }

                    if (indItem.getMtmCurrency().equals("EUR")) {
                        EURSum += indItem.getMtmValue();
                    } else {
                        final double convertedAmount = CollateralUtilities.convertCurrency(indItem.getMtmCurrency(),
                                indItem.getMtmValue(), "EUR", getValDate(), getPricingEnv());
                        EURSum += convertedAmount;
                    }
                }

            }

            // Add the last row Item
            // if ((EURSum != 0.0) && (USDSum != 0.0)) {
            KPIMtmReportItem reportItem = new KPIMtmReportItem();
            reportItem.setAgrOwner(prevAgrOwner);
            reportItem.setDealOwner(prevDealOwner);
            reportItem.setAgreementId(agreementId);
            reportItem.setAgreementName(agreementName);
            reportItem.setEurMTMSum(EURSum);
            reportItem.setUsdMTMSum(USDSum);
            kpiMtmReportItems.add(reportItem);
            // }
        }

        return kpiMtmReportItems;
    }

}