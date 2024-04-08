package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.MarginCallAllocationDTOReportTemplate;

/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
public class SantMCAllocationReportTemplate extends MarginCallAllocationDTOReportTemplate {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	public void setDefaults() {
		super.setDefaults();
		// Set default columns
		Vector<String> columns = new Vector<String>();

		columns.addElement(SantMCAllocationReportStyle.POSITION_ACTION);
		columns.addElement(SantMCAllocationReportStyle.VALUE_DATE);
		columns.addElement(SantMCAllocationReportStyle.ASSET);
		columns.addElement(SantMCAllocationReportStyle.CURRENCY);
		columns.addElement(SantMCAllocationReportStyle.NOMINAL);
		columns.addElement(SantMCAllocationReportStyle.UNIT_PRICE);
		columns.addElement(SantMCAllocationReportStyle.MARKET_VALUE);
		columns.addElement(SantMCAllocationReportStyle.VALUATION_PERCENTAGE);
		columns.addElement(SantMCAllocationReportStyle.ADJUSTED_VALUE);

		setColumns((String[]) columns.toArray(new String[columns.size()]));
	}

}
