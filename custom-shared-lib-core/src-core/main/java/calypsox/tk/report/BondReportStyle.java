/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.InstantiateUtil;
import org.jfree.util.Log;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;

public class BondReportStyle extends com.calypso.tk.report.BondReportStyle {
	private static final long serialVersionUID = -5338588358221058496L;
	protected SantProductCustomDataReportStyle bondCustomReportStyle = null;
	public static final String REDEMPTION_CCY = "Redemption Currency";

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		if (SantProductCustomDataReportStyle.isProductCustoDataColumn(columnId)) {
			return getProductCustomDataReportStyle().getColumnValue(row, columnId, errors);
		} else if(REDEMPTION_CCY.equalsIgnoreCase(columnId)){
			return Optional.ofNullable(row.getProperty(ReportRow.PRODUCT))
					.filter(Bond.class::isInstance)
					.map(Bond.class::cast)
					.map(Bond::getRedemptionCurrency).orElse("");
		}else {
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
