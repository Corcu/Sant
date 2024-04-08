/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;


import java.util.Vector;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;


public class MaterialTermsReportStyle extends TradeReportStyle {

	
    private static final long serialVersionUID = -1690776136700779143L;
    private static final String MATERIAL_TERMS_NODE = "MaterialTerms";

    
    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {
        final MaterialTermsReportItem item = (MaterialTermsReportItem) row.getProperty(MaterialTermsReportItem.MATERIAL_TERMS_REPORT_ITEM);
        return item.getColumnValue(columnName);
    }
 

    @Override
    public String[] getDefaultColumns() {
        return MaterialTermsColumns.getColumns();
    }


    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        final CalypsoTreeNode materialTermsNode = new CalypsoTreeNode(MATERIAL_TERMS_NODE);
        treeList.add(materialTermsNode);
        final String[] columns = MaterialTermsColumns.getColumns();
        for (int iColumn = 0; iColumn < columns.length; iColumn++) {
            treeList.add(materialTermsNode, columns[iColumn]);
        }
        return treeList;
    }


}
