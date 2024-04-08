package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.MarginCallCreditRatingReportTemplate;

public class SantMarginCallCreditRatingReportTemplate extends MarginCallCreditRatingReportTemplate {
    public void setDefaults() {
        super.setDefaults();

        Vector columns = new Vector();
        columns.addElement(SantMarginCallCreditRatingReportStyle.NAME);
        columns.addElement("Priority");
        this.setColumns((String[])columns.toArray(new String[columns.size()]));
    }
}
