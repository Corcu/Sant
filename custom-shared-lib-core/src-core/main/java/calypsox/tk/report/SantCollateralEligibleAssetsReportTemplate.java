package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 *
 */
public class SantCollateralEligibleAssetsReportTemplate extends ReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = -533022971671548615L;

	public static final String COL_ELIGIBLE_ASSETS = "COL_ELIGIBLE_ASSETS";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.report.ReportTemplate#setDefaults()
	 */
	@Override
	public void setDefaults() {
		// super.setDefaults();
		setColumns(SantCollateralEligibleAssetsReportStyle.DEFAULTS_COLUMNS);
	}

}
