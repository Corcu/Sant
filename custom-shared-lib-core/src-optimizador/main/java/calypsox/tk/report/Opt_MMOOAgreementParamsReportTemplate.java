package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

/**
 * Template for the Report of MMOO Agreements relation with their quote Set
 * 
 * @author Guillermo Solano
 * @version 1.0
 *
 */
public class Opt_MMOOAgreementParamsReportTemplate extends MarginCallReportTemplate {

	/**
	 * Serial UUID
	 */
	private static final long serialVersionUID = -6535782779686374854L;
	/**
	 * Constants
	 */
	public static final String MMOO_CONTRACT = "mmooMCContract";
	public static final String QUOTE_SET_NAME = "quoteSetName";

	/**
	 * Default columns
	 */
	@Override
	public void setDefaults() {
		
		super.setDefaults();
		setColumns(Opt_MMOOAgreementParamsReportStyle.DEFAULTS_COLUMNS);
	}
}
