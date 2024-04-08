/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.InstantiateUtil;

@SuppressWarnings("rawtypes")
public class EquityReportStyle extends com.calypso.tk.report.EquityReportStyle {

	private static final long serialVersionUID = 3451122691508745358L;

	protected SantProductCustomDataReportStyle bondCustomReportStyle = null;

	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		if (SantProductCustomDataReportStyle.isProductCustoDataColumn(columnId)) {
			return getProductCustomDataReportStyle().getColumnValue(row, columnId, errors);
		} else {
			return super.getColumnValue(row, columnId, errors);
		}
	}

	@Override
	public TreeList getTreeList() {
		if (this._treeList != null) {
			return this._treeList;
		}
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		if (this.bondCustomReportStyle == null) {
			this.bondCustomReportStyle = getProductCustomDataReportStyle();
		}
		if (this.bondCustomReportStyle != null) {
			treeList.add(this.bondCustomReportStyle.getNonInheritedTreeList());
		}
		return treeList;
	}

	protected SantProductCustomDataReportStyle getProductCustomDataReportStyle() {
		try {
			if (this.bondCustomReportStyle == null) {
				String className = "calypsox.tk.report.SantProductCustomDataReportStyle";
				this.bondCustomReportStyle = (SantProductCustomDataReportStyle) InstantiateUtil.getInstance(className,
						true, true);
			}
		} catch (Exception e) {
			Log.error(this, e);
		}
		return this.bondCustomReportStyle;
	}

}
