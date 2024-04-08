package calypsox.tk.report;

import java.util.Vector;

public class SantCashAllocationReportTemplate extends SantTradeBrowserReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		final Vector<String> columns = new Vector<String>();

		columns.addElement(SantCashAllocationReportStyle.REPORT_DATE);
		columns.addElement(SantCashAllocationReportStyle.COLL_SEC);
		columns.addElement(SantCashAllocationReportStyle.MOVEMENT_AMOUNT);
		columns.addElement(SantCashAllocationReportStyle.MTM_CCY_AGREE);

		setColumns(columns.toArray(new String[columns.size()]));
	}
}
