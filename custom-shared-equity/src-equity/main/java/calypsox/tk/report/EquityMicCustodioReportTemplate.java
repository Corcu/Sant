package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.core.Util;

public class EquityMicCustodioReportTemplate extends com.calypso.tk.report.BOSecurityPositionReportTemplate  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	public String[] getColumns(boolean forConfig) {
		Vector<String> columns = toVector(this.getColumns());
		return Util.collection2StringArray(columns);
	}

}
