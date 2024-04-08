package calypsox.tk.report;

import static calypsox.tk.report.Opt_MMOOAgreementParamsReportTemplate.MMOO_CONTRACT;
import static calypsox.tk.report.Opt_MMOOAgreementParamsReportTemplate.QUOTE_SET_NAME;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.MarginCallConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

/**
 * Style for the Report of MMOO Agreements relation with their quote Set
 * 
 * @author Guillermo Solano
 * @version 1.0
 *
 */
public class Opt_MMOOAgreementParamsReportStyle extends ReportStyle {


	/**
	 * Constants
	 */
	private static final long serialVersionUID = 8391747494595306953L;
	public static final String CONTRACT = "Contrato";
	public static final String QUOTE_SET = "Quote_Set";


	/**
	 * Default columns
	 */
	public static final String[] DEFAULTS_COLUMNS = { CONTRACT, QUOTE_SET };

	/**
	 * Style main method
	 */
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, @SuppressWarnings("rawtypes") final Vector errors)
			throws InvalidParameterException {
		
		final String contract = (String) row .getProperty(MMOO_CONTRACT);
		final String quoteSet = (String) row .getProperty(QUOTE_SET_NAME);

		if (columnName.equals(CONTRACT)) {
			return contract;
			
		} else if (columnName.equals(QUOTE_SET)){			
			return quoteSet;		
		} 

		final MarginCallConfigReportStyle mcConfigStyle = new MarginCallConfigReportStyle();
		return mcConfigStyle.getColumnValue(row, columnName, errors);
	}
			


	@Override
	public TreeList getTreeList() {

		if (this._treeList != null) {
			return this._treeList;
		}
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		final MarginCallConfigReportStyle mcConfigStyle = new MarginCallConfigReportStyle();
		treeList.add(mcConfigStyle.getTreeList());
		return treeList;
	}
	

}
