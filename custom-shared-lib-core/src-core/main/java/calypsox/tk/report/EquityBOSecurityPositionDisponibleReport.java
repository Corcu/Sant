package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Vector;

import calypsox.tk.report.BOSecurityPositionReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class EquityBOSecurityPositionDisponibleReport extends BOSecurityPositionReport {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String IM_CSD_TYPE = "MarginCallConfig.ADDITIONAL_FIELD.IM_CSD_TYPE";

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
		ArrayList<ReportRow> selectedRows = new ArrayList<ReportRow>();
		
		ReportStyle reportStyle = ReportStyle.getReportStyle(this.getType());
		if (output == null || output.getRows()==null)
			return output;
		for(int i =0; i<output.getRows().length;i++) {

			ReportRow row = output.getRows()[i];
			
			Object mccContractType = reportStyle.getColumnValue(row, BOSecurityPositionReportStyle.MCC_CONTRACT_TYPE, errorMsgs);
			Object csdType = reportStyle.getColumnValue(row, IM_CSD_TYPE, errorMsgs);
			
			if(!("CSD".equals(mccContractType) && "CPTY".equals(csdType))) {
				selectedRows.add(row);
			}
			
			
		}
		
		output.setRows(selectedRows.toArray(new ReportRow[selectedRows.size()]));
		
		return output;
	}
	

}
