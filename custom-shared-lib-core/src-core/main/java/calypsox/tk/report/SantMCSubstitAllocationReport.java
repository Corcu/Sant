package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public class SantMCSubstitAllocationReport extends SantMCAllocationReport {

	public static final String TYPE = "SantMCSubstitAllocation";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
		// get the allocation and filter those that are flagged as substitution
		DefaultReportOutput output = new DefaultReportOutput(this);
		DefaultReportOutput out = (DefaultReportOutput) super.load(errorMsgs);
		List<ReportRow> finalSubstitRows = new ArrayList<ReportRow>();
		TreeMap<Integer, List<ReportRow>> allocationsByExecId = new TreeMap<Integer, List<ReportRow>>();

		ReportRow[] rows = out.getRows();
		if ((rows != null) && (rows.length > 0)) {
			for (int i = 0; i < rows.length; i++) {
				MarginCallAllocationDTO alloc = (MarginCallAllocationDTO) rows[i].getProperty(ReportRow.DEFAULT);
				if ("Substitution".equals(alloc.getAttribute("AllocationMode"))) {
					List<ReportRow> substitRows = allocationsByExecId.get(alloc.getExecutionId());
					if (substitRows == null) {
						substitRows = new ArrayList<ReportRow>();
						allocationsByExecId.put(alloc.getExecutionId(), substitRows);
					}
					substitRows.add(rows[i]);
				}
			}
			// get the allocation with no execution id
			if (allocationsByExecId.get(0) != null) {
				finalSubstitRows.addAll(allocationsByExecId.get(0));
			}
			// get the allocation with the biggest execution id
			if ((allocationsByExecId.size() > 0)) {
				Integer lastKey = allocationsByExecId.lastKey();
				if (lastKey != 0) {
					finalSubstitRows.addAll(allocationsByExecId.get(lastKey));
				}
			}

		}
		output.setRows(finalSubstitRows.toArray(new ReportRow[finalSubstitRows.size()]));
		return output;
	}
}
