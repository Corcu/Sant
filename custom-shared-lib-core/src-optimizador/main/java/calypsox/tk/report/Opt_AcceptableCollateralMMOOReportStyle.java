package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;

public class Opt_AcceptableCollateralMMOOReportStyle extends ProductReportStyle {

	private static final long serialVersionUID = 123L;

	public static final String CPTY_NAME = "Counterparty";
	public static final String HAIRCUT_VALUE = "HaircutValue";

	public static final String[] DEFAULTS_COLUMNS = { CPTY_NAME, HAIRCUT_VALUE };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final Opt_AcceptableCollateralMMOOItem item = (Opt_AcceptableCollateralMMOOItem) row
				.getProperty(Opt_AcceptableCollateralMMOOReportTemplate.OPT_ACCEPTABLE_COLLAT_MMOO_ITEM);

		if (columnName.compareTo(CPTY_NAME) == 0) {
			return item.getCptyName();
		} else if (columnName.compareTo(HAIRCUT_VALUE) == 0) {
			return item.getHaircutValue() * 100;
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}

	@Override
	public TreeList getTreeList() {

		if (this._treeList != null) {
			return this._treeList;
		}
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		return treeList;

	}
}