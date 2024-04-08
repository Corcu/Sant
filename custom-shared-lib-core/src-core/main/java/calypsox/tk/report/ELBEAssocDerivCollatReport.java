package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.util.ControlMErrorLogger;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.report.*;
import com.calypso.tk.risk.forwardladder.scheme.view.loader.ReportPlan;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.ui.component.condition.ConditionTree;
import com.calypso.ui.component.condition.ConditionTreeNode;
import org.mvel2.Operator;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * L22
 *
 * @author David Porras
 */
public class ELBEAssocDerivCollatReport extends TradeReport {

    private static final long serialVersionUID = 8644061330623086854L;
    public static final String ELBE_ASSOC_DERIV_COLLAT_REPORT = "ELBEAssocDerivCollatReport";
    public static final String DATE_EXPORT = "Date to export";
    public static final String FORMAT_EXPORT = "Format date to export";
    public static final String PO = "PO";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<>();
        final DSConnection dsConn = getDSConnection();
        final ReportTemplate reportTemplate = getReportTemplate();
        JDate jdate = null;

        try {
            // Get date
            jdate = reportTemplate.getValDate();
            Vector holidays = reportTemplate.getHolidays();
            String poids = reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

            if (!Util.isEmpty(poids)) {
                TradeArray tradeArray = dsConn.getRemoteTrade().getTrades("trade, mrgcall_config, product_desc",
                        " product_desc.product_type  = 'CollateralExposure' "
                                + "AND product_desc.product_sub_type NOT IN ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT', 'SECURITY_LENDING', 'MMOO.CUENTAPROPIA', 'MMOO.CUENTATERCEROS') "
                                +" AND REGEXP_LIKE(trade.INTERNAL_REFERENCE,"
                                + "'^[[:digit:]]+$')"
                                + "AND trade.INTERNAL_REFERENCE = mrgcall_config.MRG_CALL_DEF "
                                + "AND mrgcall_config.PROCESS_ORG_ID IN (" + poids + ")"
                                + "AND (trunc(product_desc.maturity_date) >= "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "OR product_desc.maturity_date is NULL) "
                                + "AND trunc(trade.trade_date_time)  <=  "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "AND trade.PRODUCT_ID = product_desc.PRODUCT_ID "
                                + "AND (trade.trade_status = 'VERIFIED' OR  "
                                + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value where pl_mark.MARK_ID = pl_mark_value.MARK_ID "
                                + "AND trade.trade_id = pl_mark.trade_id "
                                + "AND pl_mark_value.mark_name = 'NPV_BASE' "
                                + "AND pl_mark_value.mark_value != 0 "
                                + "AND trunc(pl_mark.valuation_date) = " + Util.date2SQLString(jdate.addBusinessDays(-1, holidays)) + ")))"
                        , "trade.trade_id", null);

                tradeArray.addAll( dsConn.getRemoteTrade().getTrades("trade, mrgcall_config, product_desc, trade_keyword",
                        " product_desc.product_type  = 'PerformanceSwap' "
                                + "AND (trade.trade_id = trade_keyword.trade_id "
                                + "AND ((trade_keyword.keyword_name = 'MC_CONTRACT_NUMBER')"
                                + "AND (trade_keyword.keyword_value = mrgcall_config.MRG_CALL_DEF)))"
                                + "AND mrgcall_config.PROCESS_ORG_ID IN (" + poids + ")"
                                + "AND (trunc(product_desc.maturity_date) >= "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "OR product_desc.maturity_date is NULL) "
                                + "AND trunc(trade.trade_date_time)  <=  "
                                + Util.date2SQLString(jdate.addBusinessDays(-1, holidays))
                                + "AND trade.PRODUCT_ID = product_desc.PRODUCT_ID "
                                + "AND (trade.trade_status = 'VERIFIED' OR  "
                                + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value where pl_mark.MARK_ID = pl_mark_value.MARK_ID "
                                + "AND trade.trade_id = pl_mark.trade_id "
                                + "AND pl_mark_value.mark_name = 'NPV_BASE' "
                                + "AND pl_mark_value.mark_value != 0 "
                                + "AND trunc(pl_mark.valuation_date) = " + Util.date2SQLString(jdate.addBusinessDays(-1, holidays)) + ")))"
                        , "trade.trade_id", null));

                for (Trade trade : tradeArray.getTrades()) {
                    final ELBEAssocDerivCollatItem elbeAssocDerivCollatItem = ELBEAssocDerivCollatLogic
                            .getReportRows(trade, getActualDate(), jdate, dsConn, errorMsgsP);
                    final ReportRow repRow = new ReportRow(elbeAssocDerivCollatItem);
                    repRow.setProperty(ReportRow.TRADE, trade);
                    reportRows.add(repRow);
                }
                output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            }

            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ReposTradeReport - " + e.getMessage());
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated " + e);// CONTROL-M
            // ERROR
        }
        return null;
    }

    public static String getActualDate() {

        final Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        final String stringDate = sdf.format(date);
        return stringDate;

    }

}
