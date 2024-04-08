package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.ScheduledTask;

public class ScheduledTaskGENERATE_TRANSFER_EVENT extends ScheduledTask {
	
	public static final String REPORT_TEMPLATE_NAME = "TransferReport";
	public static final String REPORT_TYPE = "Transfer";
	public static final String EVENT_TYPE = "Event Type";
	
	@Override
	public String getTaskInformation() {
		return "Generate psEventTransfers with specific event type for accounting purpose";
	}
	
	public Vector<String> getDomainAttributes() {
		Vector<String> v = new Vector<String>();
		v.addElement(REPORT_TEMPLATE_NAME);
		v.addElement(EVENT_TYPE);
		return v;
	}
	
	@SuppressWarnings("rawtypes")
	public Vector<String> getAttributeDomain(String attr, Hashtable currentAttr) {
		Vector<String> v = new Vector<String>();

				if (attr.equals(REPORT_TEMPLATE_NAME)) {
					if (currentAttr == null) {
						return v;
					}

					String type = REPORT_TYPE;
					if (type == null) {
						return v;
					}

					type = ReportTemplate.getReportName(type);
					Vector<ReportTemplateName> names = BOCache.getReportTemplateNames(DSConnection.getDefault(), type, (String) null);

					for (int i = 0; i < names.size(); ++i) {
						ReportTemplateName r = (ReportTemplateName) names.elementAt(i);
						v.add(r.getTemplateName());
					}

					return v;
				}

			return v;
	}
	

	protected ReportOutput generateReportOutput(String type, String templateName, JDatetime valDatetime,
			DSConnection ds) throws RemoteException {
		PricingEnv env = ds.getRemoteMarketData().getPricingEnv(this._pricingEnv, valDatetime);
		Report reportToFormat = this.createReport(type, templateName, env);
		if (reportToFormat == null) {
			Log.error(this, "Invalid report type: " + type);
			return null;
		} else if (reportToFormat.getReportTemplate() == null) {
			Log.error(this, "Invalid report template: " + type);
			return null;
		} else {
			Vector<String> holidays = this.getHolidays();
			if (!Util.isEmpty(holidays)) {
				reportToFormat.getReportTemplate().setHolidays(holidays);
			}

			if (this.getTimeZone() != null) {
				reportToFormat.getReportTemplate().setTimeZone(this.getTimeZone());
			}

			Vector<String> errorMsgs = new Vector<String>();
			return reportToFormat.load(errorMsgs);
		}
	}

	protected Report createReport(String type, String templateName, PricingEnv env)
			throws RemoteException {
		Report report;
		try {
			String template = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(template, true);
			report.setPricingEnv(env);
			report.setFilterSet(this._tradeFilter);
			report.setValuationDatetime(this.getValuationDatetime());
		} catch (Exception arg7) {
			Log.error(this, arg7);
			report = null;
		}

		if (report != null && !Util.isEmpty(templateName)) {
			ReportTemplate template1 = DSConnection.getDefault().getRemoteReferenceData()
					.getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template1 == null) {
				Log.error(this, "Template " + templateName + " Not Found for " + type + " Report");
			} else {
				report.setReportTemplate(template1);
				template1.setValDate(this.getValuationDatetime().getJDate(this._timeZone));
				template1.callBeforeLoad();
			}
		}

		return report;
	}
	
	private BOTransfer getTransfer(ReportRow row) {
		BOTransfer xfer = null;
		Object o = row.getProperty(BOTransfer.class.getSimpleName());
		if (o instanceof BOTransfer) {
			xfer = (BOTransfer) o;
		}

		return xfer;
	}

	public boolean process(DSConnection ds, PSConnection ps) {

		try {
			String templateName = this.getAttribute(REPORT_TEMPLATE_NAME);
			JDatetime valDatetime = this.getModValuationDatetime();
			DefaultReportOutput output = (DefaultReportOutput)generateReportOutput(REPORT_TYPE, templateName, valDatetime, ds);
			ReportRow[] rows = output.getRows();
			for(int i=0; i<rows.length;i++) {
				PSEventTransfer event = createEventTransfer(getTransfer(rows[i]));
				ds.getRemoteTrade().saveAndPublish(event);
			}
		} catch (RemoteException e) {
			Log.error(LOG_CATEGORY, e);
		}
		return true;
		
	}
	
	private JDatetime getModValuationDatetime() {
		JDatetime valDatetime = null;
		int oldValue = this.getValuationTime();
		if (oldValue == 0) {
			JDatetime currentDatetime = this.getDatetime();
			int hours = currentDatetime.getField(11, this.getTimeZone());
			int min = currentDatetime.getField(12, this.getTimeZone());
			this.setValuationTime(100 * hours + min);
			valDatetime = this.getValuationDatetime();
			this.setValuationTime(oldValue);
		} else {
			valDatetime = this.getValuationDatetime();
		}

		return valDatetime;
	}

	public PSEventTransfer createEventTransfer(BOTransfer transfer) {

		try {
			Trade trade = getDSConnection().getRemoteTrade().getTrade(transfer.getTradeLongId());
			PSEventTransfer eventTransfer = new PSEventTransfer(transfer, trade, null);
			eventTransfer.setStatus(Status.valueOf(getAttribute(EVENT_TYPE)));
			return eventTransfer;
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CATEGORY, e);
		}

		return null;
	}

}
