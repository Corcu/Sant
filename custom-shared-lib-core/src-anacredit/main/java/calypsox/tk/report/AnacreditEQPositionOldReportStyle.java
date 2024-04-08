package calypsox.tk.report;

import calypsox.tk.anacredit.api.reportstyle.AnacreditReportStyle;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditEQPositionOldReportStyle extends ReportStyle {

    public static final String AGGREGO = "AGGREGO";

    private AnacreditReportStyle _anacreditReportStyle = new AnacreditReportStyle();


    public AnacreditEQPositionOldReportStyle() {
        super();
    }
    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        if (columnId.equals(AGGREGO)) {
            return row.getProperty(AnacreditEQPositionReport.PROPERTY_AGGREGO);
        }

        Object value = _anacreditReportStyle.getColumnValue(row, columnId, errors);
        return value;
    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(_anacreditReportStyle.getTreeList());
        return  treeList;

    }
}
