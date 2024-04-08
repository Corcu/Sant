package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

/**
 * Template for the report of MMOO Haircuts quotes
 * 
 * @author Guillermo Solano
 * @version 1.0
 *
 */
public class Opt_MMOOHaircutDefinitionReportTemplate extends MarginCallReportTemplate {

	/**
	 * Serial UUID
	 */
	private static final long serialVersionUID = -6535782779686374854L;
	/**
	 * Constants
	 */
	public static final String ISIN = "ISIN";
	public static final String CURRENCY = "ccy";
	public static final String QUOTE = "quoteMMOO";

	/**
	 * Default columns
	 */
	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_MMOOHaircutDefinitionReportStyle.DEFAULTS_COLUMNS);
	}
}
