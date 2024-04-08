package calypsox.tk.util;

import calypsox.tk.report.StandardReportOutput;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;

import java.text.SimpleDateFormat;
import java.util.*;


public class ScheduledTaskICO_CSVREPORT extends ScheduledTaskCSVREPORT {

	public static final String ATTR_REPORT_FREQUENCY = "REPORT FREQUENCY";
	public static final String ATTR_CURRENT_MONTH = "Current Month";
	public static final String ATTR_FROM_TEMPLATE = "From Template";

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(ATTR_REPORT_FREQUENCY);
		return result;
	}

	@Override
	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
		Vector v = super.getAttributeDomain(attribute, hashtable);
		if (attribute.equals(ATTR_REPORT_FREQUENCY)) {
			v.addElement(ATTR_CURRENT_MONTH);
			v.addElement(ATTR_FROM_TEMPLATE);
		}
		return v;
	}

	@Override
	protected void modifyTemplate(Report reportToFormat) {
		// set Frequency to ReportTemplate
		reportToFormat.getReportTemplate().put(ATTR_REPORT_FREQUENCY, getAttribute(ATTR_REPORT_FREQUENCY));
	}

}