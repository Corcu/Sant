package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.util.ControlMErrorLogger;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * L49
 *
 * @author David Porras
 */
public class ELBEAssocReposSecLendCollatReport extends TradeReport {

    private static final long serialVersionUID = 5393164080916268067L;
    public static final String ELBE_ASSOC_REPOS_SECLEN_COLLAT_ITEM = "ELBEAssocReposSecLendCollatItem";
    public static final String DATE_EXPORT = "Date to export";
    public static final String FORMAT_EXPORT = "Format date to export";
    public static final String PO = "PO";
    public static final String STATUS = "MATURED";
    public static final String MC_CONTRACT_NUMBER = "MC_CONTRACT_NUMBER";

    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<>();
        final DSConnection dsConn = getDSConnection();
        final ReportTemplate reportTemp = getReportTemplate();
        JDate jdate = null;

        try {
            jdate = reportTemp.getValDate();
            Vector holidays = reportTemp.getHolidays();
            String poids = reportTemp.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

            if (!Util.isEmpty(poids)) {
                TradeArray trades = dsConn.getRemoteTrade().getTrades("trade, mrgcall_config, product_desc",
                        " product_desc.product_type IN ('Repo','SecLending') "
                                +" AND REGEXP_LIKE(trade.INTERNAL_REFERENCE,"
                                + "'^[[:digit:]]+$')"
                                + "AND trade.INTERNAL_REFERENCE = mrgcall_config.MRG_CALL_DEF "
                                + "AND mrgcall_config.PROCESS_ORG_ID IN (" + poids + ")"
                                + "AND (trunc(product_desc.maturity_date) >= "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "OR product_desc.maturity_date is NULL) " + "AND trunc(trade.trade_date_time)  <=  "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "AND trade.PRODUCT_ID = product_desc.PRODUCT_ID "
                                + "AND (trade.trade_status = 'VERIFIED' OR  "
                                + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value where pl_mark.MARK_ID = pl_mark_value.MARK_ID "
                                + "AND trade.trade_id = pl_mark.trade_id " + "AND pl_mark_value.mark_name = 'NPV_BASE' "
                                + "AND pl_mark_value.mark_value != 0 " + "AND trunc(pl_mark.valuation_date) = "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays)) + ")))",
                        "trade.trade_id", null);

                trades.addAll(dsConn.getRemoteTrade().getTrades("trade, mrgcall_config, product_desc",
                        " product_desc.product_type  = 'CollateralExposure' "
                                + "AND product_desc.product_sub_type IN ('SECURITY_LENDING') "
                                + "AND trade.INTERNAL_REFERENCE = mrgcall_config.MRG_CALL_DEF "
                                + "AND mrgcall_config.PROCESS_ORG_ID IN (" + poids + ")"
                                + "AND (trunc(product_desc.maturity_date) >= "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "OR product_desc.maturity_date is NULL) " + "AND trunc(trade.trade_date_time)  <=  "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "AND trade.PRODUCT_ID = product_desc.PRODUCT_ID "
                                + "AND (trade.trade_status = 'VERIFIED' OR  "
                                + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value where pl_mark.MARK_ID = pl_mark_value.MARK_ID "
                                + "AND trade.trade_id = pl_mark.trade_id " + "AND pl_mark_value.mark_name = 'NPV_BASE' "
                                + "AND pl_mark_value.mark_value != 0 " + "AND trunc(pl_mark.valuation_date) = "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays)) + ")))",
                        "trade.trade_id", null));

                    for (Trade trade : trades.getTrades()) {
                        if (!"FICTICIO".equalsIgnoreCase(trade.getKeywordValue("SecLendingTrade")) && isNotIntragroup(trade)){
                        ELBEAssocReposSecLendCollatItem elbeAssocReposSecLendCollatItem = ELBEAssocReposSecLendCollatLogic
                                .getReportRows(trade, getActualDate(), jdate, dsConn, errorMsgsP);
                        final ReportRow repRow = new ReportRow(elbeAssocReposSecLendCollatItem);
                        repRow.setProperty(ReportRow.TRADE, trade);
                        reportRows.add(repRow);
                    }
                }
                output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            }

            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ReposTradeReport - " + e);
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
            // ERROR:
        }

        return null;
    }

    /**
     * @param trade
     * @return true if cpty Attribute INTRAGROUP is not 'YES' or 'S'
     */
    private boolean isNotIntragroup(Trade trade){
        LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0,trade.getCounterParty().getId(),"ALL", "INTRAGROUP");

        return (attr == null || attr.getAttributeValue() == null || attr.getAttributeValue().equalsIgnoreCase("N")
                || attr.getAttributeValue().equalsIgnoreCase("NO"));
    }

    public static String getActualDate() {
        final Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        return sdf.format(date);

    }
}
