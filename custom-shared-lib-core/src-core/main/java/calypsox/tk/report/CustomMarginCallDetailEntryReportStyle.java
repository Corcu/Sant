package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.report.MarginCallDetailEntryReportStyle;
import com.calypso.tk.report.ReportRow;

public class CustomMarginCallDetailEntryReportStyle extends MarginCallDetailEntryReportStyle {

	private static final long serialVersionUID = -1946603214552382930L;

	private static String BOND_EQUITY = "Bond/Equity";
	
    // Default columns.
    private static final String[] DEFAULTS_COLUMNS = { BOND_EQUITY };
    
    @Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(BOND_EQUITY);
		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {
		
		if (columnName.compareTo(BOND_EQUITY) == 0) {
			MarginCallDetailEntryDTO entry = (MarginCallDetailEntryDTO) row.getProperty("Default");
				if (entry.getDescription().contains("Bond")){
					return "Bond";
				}else if (entry.getDescription().contains("Equity")) {
					return "Equity";
				} else{
					return "";
				}
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}
}
