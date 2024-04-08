/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.style;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public abstract class SantReportStyleHelper {

    protected final Map<String, String> columnNames = new HashMap<String, String>();

    protected final ReportStyle style = getReportStyle();

    protected abstract ReportStyle getReportStyle();

    protected TreeList treeList;

    protected SantReportStyleHelper() {
	loadTreeList();
    }

    public boolean isColumnName(final String columnName) {
	return this.columnNames.containsKey(columnName);
    }

    protected String getRealColumnName(final String columnName) {
	return this.columnNames.get(columnName);
    }

    public TreeList getTreeList() {
	return this.treeList;
    }

    protected abstract void loadTreeList();

    @SuppressWarnings("rawtypes")
    public Object getColumnValue(final ReportRow row, final String columnName,
	    final Vector errors) {
	return this.style.getColumnValue(row, getRealColumnName(columnName),
		errors);
    }
}
