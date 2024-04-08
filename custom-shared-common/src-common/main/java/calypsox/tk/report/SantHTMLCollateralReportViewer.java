/**
 *
 */
package calypsox.tk.report;

import calypsox.util.SantReportFormattingUtil;
import com.calypso.tk.report.HTMLCollateralReportViewer;

/**
 * @author aela
 *
 */
public class SantHTMLCollateralReportViewer extends HTMLCollateralReportViewer {
    @Override
    public void formatHeader() {
        buffer.append("<table class=\"noticeList\">\n");
        //buffer.append("<table style=\"font-family: SansSerif; font-size: 9pt; width=100%; \" cellpadding=\"7\">\n");
        //buffer.append("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" font-size=\"9pt\" font-family=\"SansSerif\">\n");
    }

    @Override
    protected void formatTrailer() {
        buffer.append("</table>");
    }

    /**
     * @param type
     * @param value
     */
    @Override
    protected void formatCell(int type, Object value) {
        value = SantReportFormattingUtil.getInstance().formatEmptyCollectionForReporting(value);
        super.formatCell(type, value);
    }
}
