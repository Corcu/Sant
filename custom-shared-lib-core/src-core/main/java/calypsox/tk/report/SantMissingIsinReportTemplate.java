package calypsox.tk.report;

import com.calypso.tk.report.TaskReportTemplate;

//Project: MISSING_ISIN

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 * @author Carlos Humberto Cejudo Bermejo <c.cejudo.bermejo@accenture.com >
 *
 */
public class SantMissingIsinReportTemplate extends TaskReportTemplate {

    /**
     * 
     */
    private static final long serialVersionUID = -533022971671548615L;

    /*
     * (non-Javadoc)
     * 
     * @see com.calypso.tk.report.ReportTemplate#setDefaults()
     */
    @Override
    public void setDefaults() {
        setColumns(SantMissingIsinReportStyle.DEFAULTS_COLUMNS);
    }

}
