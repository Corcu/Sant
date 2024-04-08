package calypsox.tk.report;

import calypsox.tk.anacredit.api.reportstyle.AnacreditReportStyle;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditTradeReportStyle extends TradeReportStyle {

    AnacreditReportStyle _anacreditReportStyle = new AnacreditReportStyle();

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {


        Object value = _anacreditReportStyle.getColumnValue(row, columnId, errors);
        if (value == null) {
            value = super.getColumnValue(row, columnId, errors);
        }
        return value;
    }

    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(_anacreditReportStyle.getTreeList());
        return  treeList;

    }

}
