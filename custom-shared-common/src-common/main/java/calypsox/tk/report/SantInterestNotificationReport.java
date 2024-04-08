/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.loader.SantInterestNotificationLoader;
import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntry;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.List;
import java.util.Vector;

public class SantInterestNotificationReport extends SantReport {

	private static final long serialVersionUID = 7286748957863958826L;

	@Override
	public ReportOutput loadReport(final Vector<String> errorMsgs) {
		try {

			return getReportOutput();

		} catch (final Exception e) {
			String msg = "Cannot load SantInterestNotification ";
			Log.error(this, msg, e);
			errorMsgs.add(msg + e.getMessage());
		}

		return null;
	}

	protected ReportOutput getReportOutput() throws Exception {

		final DefaultReportOutput output = new DefaultReportOutput(this);
		final SantInterestNotificationLoader loader = new SantInterestNotificationLoader();

		final List<SantInterestNotificationEntry> entries = loader.load(getReportTemplate(), getProcessStartDate(),
				getProcessEndDate(), getValDate());

		final ReportRow[] rows = new ReportRow[entries.size()];

		for (int i = 0; i < entries.size(); i++) {
			final ReportRow row = new ReportRow(entries.get(i), SantInterestNotificationReportTemplate.ROW_DATA);
			rows[i] = row;
		}
		output.setRows(rows);

		return output;
	}

}
