package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReportStyle;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

import static calypsox.tk.report.PdvTransferReportTemplate.*;

public class PdvTransferReportStyle extends TransferReportStyle {
    private static final String TOMADO = "TOMADO";
    private static final String PRESTADO = "PRESTADO";
    private static final String COBRO = "COBRO";
    private static final String PAGO = "PAGO";

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        if(REPORT_DATE.equalsIgnoreCase(columnName)){
            final JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
            final JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            return valDate;
        } else if(TOMADO_PRESTADO.equalsIgnoreCase(columnName)){
            Trade trade = row.getProperty("Trade");
            if (trade != null) {
                return (trade.getQuantity() > 0 ? TOMADO : PRESTADO);
            }
        }  else  if(SIGNO_COMISION.equalsIgnoreCase(columnName)) {
            return getSignoComision(row, errors);
        }  else  if(INDEMNITY_ACCRUAL.equalsIgnoreCase(columnName)) {
            JDatetime valdatetime = row.getProperty(ReportRow.VALUATION_DATETIME);
            return getIndemnityAccrual(valdatetime, row, errors);
        }
        return super.getColumnValue(row, columnName, errors);
    }

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(TOMADO_PRESTADO);
        treeList.add(SIGNO_COMISION);
        treeList.add(INDEMNITY_ACCRUAL);
        treeList.add(REPORT_DATE);
        return treeList;
    }

    private String getSignoComision(ReportRow row, Vector errors) {
        Trade trade = row.getProperty("Trade");
        if (trade != null) {
            Object value = super.getColumnValue(row, REAL_SETTLEMENT_AMOUNT, errors);
            if (value != null && value instanceof Amount) {
                if (((Amount)value).get() > 0.00) {
                    return COBRO;
                } else  {
                    return PAGO;
                }
            }
        }
        return "";
    }

    private Object getIndemnityAccrual(JDatetime valDateTime, ReportRow row, Vector errors) {
        Trade trade = row.getProperty("Trade");
        if(trade!=null) {
            return PdvInformesReportStyle.doCalculation(valDateTime, trade, new PricerMeasure(PricerMeasure.INDEMNITY_ACCRUAL), "PdvTransfer");
        }
        return null;
    }

}
