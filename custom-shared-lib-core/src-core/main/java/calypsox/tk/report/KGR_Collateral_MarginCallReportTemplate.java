package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

public class KGR_Collateral_MarginCallReportTemplate extends MarginCallReportTemplate {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -430029178563676822L;

	// GSM 15/03/2016 - Add Source System for IRIS
	public static String SOURCE_SYSTEM = "IRIS_source_system";
	//AAP 03/05/2016 - MaturityDate Offset
	public static String MATURITY_OFFSET="MaturityOffset";

	@Override
    public void setDefaults() {
		super.setDefaults();
		setColumns(KGR_Collateral_MarginCallReportStyle.DEFAULTS_COLUMNS);
    }
}
