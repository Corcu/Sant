package calypsox.tk.report;

import java.util.Vector;

/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
public class SantMCSubstitAllocationReportTemplate extends SantMCAllocationReportTemplate {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public void setDefaults() {
		super.setDefaults();
		// Set default columns
		Vector<String> columns = new Vector<String>();

		columns.addElement(SantMCSubstitAllocationReportStyle.PARTY);
		columns.addElement(SantMCSubstitAllocationReportStyle.VALUE_DATE);
		columns.addElement(SantMCSubstitAllocationReportStyle.ELIGIBLE_COLLATERAL);
		columns.addElement(SantMCSubstitAllocationReportStyle.ELIGIBLE_CURRENCY);
		columns.addElement(SantMCSubstitAllocationReportStyle.NOMINAL);
		columns.addElement(SantMCSubstitAllocationReportStyle.UNIT_PRICE);
		columns.addElement(SantMCSubstitAllocationReportStyle.MARKET_VALUE);
		columns.addElement(SantMCSubstitAllocationReportStyle.VALUATION_PERCENTAGE);
		columns.addElement(SantMCSubstitAllocationReportStyle.ADJUSTED_VALUE);

		setColumns((String[]) columns.toArray(new String[columns.size()]));
	}

}
