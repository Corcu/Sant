/**
 *
 */
package calypsox.tk.report;

import calypsox.util.SantReportFormattingUtil;
import com.calypso.tk.core.JDate;

import java.util.Date;

/**
 * @author aalonsop Overrides core XLSReport to avoid the 14.4 bug when this
 *         superclass receives a column value extending java.util.Date
 */
public class XLSReportViewer extends com.calypso.tk.report.XLSReportViewer {

    @Override
    protected void formatCell(int type, Object value) {
            value = SantReportFormattingUtil.getInstance().formatEmptyCollectionForReporting(value);
            if (value instanceof Date)
                super.formatCell(type, parseDateToJDate((Date) value));
            else
                super.formatCell(type, value);
    }

    private JDate parseDateToJDate(Date date) {
        return JDate.valueOf(date);
    }
}
