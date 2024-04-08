package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableBean;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;


public class MarginCallCashMICExtracontableReportStyle extends PositionBasedMICExtracontableReportStyle {

    public static final String ACCOUNTID = "ACCOUNTID";
    public static final String CDREOPIN = "CDREOPIN";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) {
        Object columnValue;
        MICExtracontableBean bean = row.getProperty(MICExtracontableReport.ROW_PROP_NAME);
        if (ACCOUNTID.equalsIgnoreCase(columnId)) {
            columnValue = bean.getAccountId();
        } else if (CDNUOPBA.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdnuopba();
        } else if (CDREOPIN.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCdreopin();
        } else if (CDSECTOR.equalsIgnoreCase(columnId)) {
            columnValue = bean.getCodSector();
        } else {
            columnValue = super.getColumnValue(row, columnId, errors);
        }
        return columnValue;
    }

}
