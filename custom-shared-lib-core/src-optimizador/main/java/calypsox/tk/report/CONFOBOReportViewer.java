package calypsox.tk.report;

import java.text.DecimalFormat;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.CSVUtil;

public class CONFOBOReportViewer extends CSVReportViewer {

	private static final String NOM_COLM_NAME = "Nominal Column Name";

	@Override
	protected void formatCell(int type, Object value) {

		if (value != null && value instanceof Amount) {
			// Only for FOBO IM Cash/Security Conciliations Reports
			try {
				double val = ((Amount) value).get();
				DecimalFormat format = new DecimalFormat("0.0");
				String result = format.format(val);
				if (result.contains(",")) {
					result = result.replace(",", ".");
				}
				this.buffer.append(CSVUtil.escape(result, this.delimiter)).append(this.delimiter);
			} catch (Exception e) {
				Log.error(this, "Error apply format: " + e);
				super.formatCell(type, value);
			}

		} else {
			super.formatCell(type, value);
		}

	}

	@Override
	public void setHeading(String[] headings) {
		String nominalColumnsName = (String) template.get(NOM_COLM_NAME);
		if(!Util.isEmpty(nominalColumnsName)) {
			for (int i = 0; i < headings.length; i++) {
				if (Util.MStringToDate(headings[i], true) != null) {
					headings[i] = nominalColumnsName;
				}
			}
		}
		super.setHeading(headings);
	}

}
