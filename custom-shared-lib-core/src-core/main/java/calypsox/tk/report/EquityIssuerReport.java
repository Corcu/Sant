package calypsox.tk.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.EquityReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public class EquityIssuerReport extends EquityReport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("rawtypes")
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
		StandardReportOutput standardOuput = new StandardReportOutput(this);
		standardOuput.setRows(output.getRows());
		return standardOuput;
	}
	
	@Override
	protected ReportRow[] filterRows(ReportRow[] rows) {
		ReportRow[] superFilteredRows = filterRows(rows, this._reportTemplate, false);
		Map<String,ReportRow> filteredRows = new HashMap<>();
		for(int i=0;i<superFilteredRows.length;i++) {
			ReportRow row = superFilteredRows[i];
			Equity equity = (Equity)row.getProperty(ReportRow.DEFAULT);
			String isin = equity.getSecCode(SecCode.ISIN);
			if(isin!=null && isin.length()==12) {
				String key = isin;
				if (equity.getIssuer()!=null){
					key += "|" + equity.getIssuer().getExternalRef();
				}
				filteredRows.put(key,row);
			}
		}
		ReportRow[] result = filteredRows.values().toArray(new ReportRow[filteredRows.size()]);
		return result;
	}


}
