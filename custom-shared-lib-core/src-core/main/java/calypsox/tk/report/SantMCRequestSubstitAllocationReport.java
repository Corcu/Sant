package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public class SantMCRequestSubstitAllocationReport extends SantMCSubstitAllocationReport {

	public static final String TYPE = "SantMCRequestSubstitAllocation";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
		// get the allocation and filter those that are flagged as substitution and have an execution id equal to 0
		DefaultReportOutput output = new DefaultReportOutput(this);
		DefaultReportOutput out = (DefaultReportOutput) super.load(errorMsgs);
		List<ReportRow> substitRows = new ArrayList<ReportRow>();
		ReportRow[] rows = out.getRows();
		if ((rows != null) && (rows.length > 0)) {
			for (int i = 0; i < rows.length; i++) {
				MarginCallAllocationDTO alloc = (MarginCallAllocationDTO) rows[i].getProperty(ReportRow.DEFAULT);
				if (alloc.getExecutionId() == 0) {
					substitRows.add(rows[i]);
				}
			}
		}
		output.setRows(substitRows.toArray(new ReportRow[substitRows.size()]));
		return output;
	}

}
