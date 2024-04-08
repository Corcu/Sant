package calypsox.tk.report;

import calypsox.apps.reporting.ConcatenatedReportTemplatePanel;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

public class ConcatenatedReport extends Report {

	/**
	 *
	 */
	public static final String ROW_REPORT_STYLE = "ROW_REPORT_STYLE";
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(Vector errorMsgs) {

		String templateType1 = this.getReportTemplate().get(ConcatenatedReportTemplatePanel.TEMPLATE_TYPE_1);
		String templateChoice1 = this.getReportTemplate().get(ConcatenatedReportTemplatePanel.TEMPLATE_CHOICE_1);
		String templateChoice2 = this.getReportTemplate().get(ConcatenatedReportTemplatePanel.TEMPLATE_CHOICE_2);

		try {

			// generate reports

			Report reportToFormat1 = this.createReport(templateType1, templateChoice1, this.getPricingEnv());
			Report reportToFormat2 = this.createReport(templateType1, templateChoice2, this.getPricingEnv());

			ReportOutput output1 = generateReportOutput(reportToFormat1, this.getValuationDatetime(), errorMsgs);
			ReportOutput output2 = generateReportOutput(reportToFormat2, this.getValuationDatetime(), errorMsgs);

			ArrayList<ReportRow> mergedRows = new ArrayList<>();

			ReportStyle reportStyle = ReportStyle.getReportStyle(reportToFormat1.getType());

			// Append rows
			if(output1 instanceof DefaultReportOutput && output2 instanceof DefaultReportOutput) {
				DefaultReportOutput defaultReportOutput1 = (DefaultReportOutput)output1;
				DefaultReportOutput defaultReportOutput2 = (DefaultReportOutput)output2;

				List<ReportRow> list1 = Arrays.asList(defaultReportOutput1.getRows());
				List<ReportRow> list2 = Arrays.asList(defaultReportOutput2.getRows());

				mergedRows.addAll(list1);
				mergedRows.addAll(list2);
			}

			mergedRows = (ArrayList<ReportRow>) mergedRows.stream().filter(Objects::nonNull).collect(Collectors.toList());

			for (ReportRow row : mergedRows) {
				row.setProperty(ROW_REPORT_STYLE, reportStyle);
			}

			this.getReportTemplate().setColumns(reportToFormat1.getReportTemplate().getColumnNames());
			StandardReportOutput standardOutput = new StandardReportOutput(this);

			standardOutput.setRows(mergedRows.toArray(new ReportRow[mergedRows.size()]));

			return standardOutput;
		} catch (RemoteException e) {
			Log.error(this, e);
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	private ReportOutput generateReportOutput(Report reportToFormat, JDatetime valDatetime,
											  Vector errorMsgs) throws RemoteException {

		Vector holidays = reportToFormat.getReportTemplate().getHolidays();
		if (!Util.isEmpty(holidays)) {
			reportToFormat.getReportTemplate().setHolidays(holidays);
		}

		if (this.getReportTemplate().getTimeZone() != null) {
			reportToFormat.getReportTemplate().setTimeZone(this.getReportTemplate().getTimeZone());
		}

		return reportToFormat.load(errorMsgs);

	}

	protected Report createReport(String type, String templateName, PricingEnv env)
			throws RemoteException {
		Report report;
		try {
			String className = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(className, true);
			report.setPricingEnv(env);
			report.setValuationDatetime(this.getValuationDatetime());
		} catch (Exception var8) {
			Log.error(this, var8);
			report = null;
		}

		if (report != null && !Util.isEmpty(templateName)) {
			ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
					.getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template == null) {
				Log.error(this, "Template " + templateName + " Not Found for " + type + " Report");
			} else {
				report.setReportTemplate(template);
				template.setValDate(this.getValuationDatetime().getJDate(this.getReportTemplate().getTimeZone()));
				template.callBeforeLoad();
			}
		}

		return report;
	}



}


