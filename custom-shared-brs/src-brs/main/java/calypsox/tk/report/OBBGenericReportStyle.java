package calypsox.tk.report;

import calypsox.tk.bo.obb.OBBGenericBean;
import com.calypso.tk.report.PostingReportStyle;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class OBBGenericReportStyle extends PostingReportStyle {

    public static final String LINE = "Line";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        if(null!=row){
            OBBGenericBean bean = row.getProperty("Default");
            if(LINE.equalsIgnoreCase(columnId)){
                return bean.toString();
            }else {
                row.setProperty("Trade",bean.getTrade());
                row.setProperty("BOPosting",bean.getBoPosting());
            }
        }
        return super.getColumnValue(row, columnId, errors);
    }
}
