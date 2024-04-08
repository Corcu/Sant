package calypsox.tk.report;

import calypsox.apps.reporting.PositionKeeperUtilCustom;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class PositionKeeperReport extends Report {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		
		PositionKeeperUtilCustom positionKeeperJFrame = new PositionKeeperUtilCustom();
		
		//  Compute positions and gets the result
		ArrayList<HashMap<String, Object>> result = positionKeeperJFrame.getPositionKeeperContent(this);
		
		// build rows from result
		ReportRow[] rows = new ReportRow[result.size()];
		int i=0;
		for(HashMap<String,Object> line : result ) {
			ReportRow row = new ReportRow(line);
			rows[i++]=row;
		}
		DefaultReportOutput reportOutput = new DefaultReportOutput(this);
		reportOutput.setRows(rows);
		return reportOutput;
	}

}
