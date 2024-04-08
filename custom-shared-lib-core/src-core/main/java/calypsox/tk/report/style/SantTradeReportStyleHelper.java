/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.style;

import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.TradeReportStyle;

/**
 * No customization on column name so far
 * 
 * @author F11S1E
 * 
 */
public class SantTradeReportStyleHelper extends SantReportStyleHelper {

    @Override
    public void loadTreeList() {
	this.treeList = this.style.getNonInheritedTreeList();
    }

    @Override
    public boolean isColumnName(final String columnName) {
	return true;
    }

    @Override
    protected String getRealColumnName(final String columnName) {
	return columnName;
    }

    @Override
    protected ReportStyle getReportStyle() {
	return new TradeReportStyle();
    }

}
