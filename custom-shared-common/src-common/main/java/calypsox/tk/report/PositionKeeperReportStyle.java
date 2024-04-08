package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import calypsox.apps.reporting.PositionKeeperUtilCustom;

public class PositionKeeperReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;
	public static PositionKeeperUtilCustom positionKeeperUtilCustom = new PositionKeeperUtilCustom();
	Vector<String> allColumns = positionKeeperUtilCustom.getAllColumnNames();
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		return ((HashMap<String,Object>)row.getProperty(ReportRow.DEFAULT)).get(columnName);
	}
	
	@Override
	public String[] getDefaultColumns() {
		return (String[])allColumns.toArray(new String[allColumns.size()]) ;
	}
	
	@Override
	 public String[] getPossibleColumnNames() {
		return (String[])allColumns.toArray(new String[allColumns.size()]) ;
	 }
	
    @SuppressWarnings("deprecation")
	@Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        for(String columnName : (Vector<String>)allColumns) {
        	treeList.add(columnName);
        }
        return treeList;
    }

}
