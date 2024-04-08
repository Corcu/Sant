package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReportStyle;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;


public class ICOPdvReportStyle extends TransferReportStyle {

    public static final String REPORT_DATE = "Report Date";
    public static final String STATUS_1 = "Status 1";
    public static final String STATUS_2 = "Status 2";
    public static final String SUB_O = "Sub. O.";
    public static final String PRODUCT = "Product";

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        BOTransfer transfer = (BOTransfer) row.getProperty(ReportRow.TRANSFER);

        if(REPORT_DATE.equalsIgnoreCase(columnName)){
            final JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
            final JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
            return valDate;
        } else if (STATUS_1.equalsIgnoreCase(columnName)) {
            if (transfer.getStatus().equals(Status.S_SETTLED)) {
                return "OK//OK";
            } else {
                return transfer.getAttribute("Matching_Reason");
            }
        } else if (STATUS_2.equalsIgnoreCase(columnName)) {
            if (transfer.getStatus().equals(Status.S_SETTLED)) {
                return "OK//OK";
            } else {
                return transfer.getAttribute("Matching_Status");
            }
        } else if (PRODUCT.equalsIgnoreCase(columnName)){
            return "RF FIRME";
        } else if (SUB_O.equalsIgnoreCase(columnName)) {

            if (transfer.getStatus().equals(Status.S_SETTLED)) {
                return "LQ";
            } else if (transfer.getStatus().equals(Status.S_PENDING)) {
                return "PQPC";
            } else if (transfer.getStatus().equals(Status.S_CANCELED)) {
                return "BAAN";
            } else if (transfer.getStatus().equals(Status.S_FAILED)) {
                return "FAILED";
            } else if (transfer.getStatus().equals(Status.S_VERIFIED)) {
                return "VERIFIED";
            }

        } else if (OTHER_AMOUNT.equalsIgnoreCase(columnName)
                || TRANSFER_AMOUNT.equalsIgnoreCase(columnName) ) {
            SignedAmount amt = (SignedAmount) super.getColumnValue(row, columnName, errors);

            if (amt != null) {
                //rst = new BigDecimal(selectedfee.getAmount()).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
                decimalFormat.setNegativePrefix("-");
                decimalFormat.applyPattern("0.00");
                String s =  decimalFormat.format(amt.get());
                return s;
            }
        }

        return super.getColumnValue(row, columnName, errors);

    }


    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(REPORT_DATE);
        treeList.add(STATUS_1);
        treeList.add(STATUS_2);
        treeList.add(SUB_O);
        treeList.add(PRODUCT);
        return treeList;
    }



}
