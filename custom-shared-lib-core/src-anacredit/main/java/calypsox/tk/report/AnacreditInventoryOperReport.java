package calypsox.tk.report;

import com.calypso.tk.report.ReportRow;

import java.util.List;
import java.util.Vector;

public class AnacreditInventoryOperReport extends AnacreditInventoryAbstractReport {

    @Override
    protected List<ReportRow> extendReportRows(List<ReportRow> allRows, Vector<String> errors) {
        // For Operaciones nothing to do bacause the data is the list itself
        return allRows;
    }

}
