package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;

public class Opt_AcceptableCollateralReportStyle extends ProductReportStyle {

	private static final long serialVersionUID = 123L;

	public static final String FILTER_NAME = "StaticDataFilterName";

	public static final String[] DEFAULTS_COLUMNS = { FILTER_NAME };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final Opt_AcceptableCollateralItem item = (Opt_AcceptableCollateralItem) row
				.getProperty(Opt_AcceptableCollateralReportTemplate.OPT_ACCEPTABLE_COLLAT_ITEM);

		if (columnName.compareTo(FILTER_NAME) == 0) {
			return item.getFilterName();
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