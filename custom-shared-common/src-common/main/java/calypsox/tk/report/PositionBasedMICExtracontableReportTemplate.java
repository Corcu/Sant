package calypsox.tk.report;

import com.calypso.tk.report.BOSecurityPositionReportTemplate;

/**
 * @author aalonsop
 */
public class PositionBasedMICExtracontableReportTemplate extends BOSecurityPositionReportTemplate {

    @Override
    public void setDefaultDateColumns() {
    }


    public String[] getColumns(boolean forConfig) {
        return this.getColumns();
    }
}
