/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

/**
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallAllocationReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

/**
 * Adaptation of the MarginCallAllocationReport to use multiple POs
 * 
 * @author Guillermo Solano
 * @version 1.0
 */
public class SantMarginCallAllocationReport extends MarginCallAllocationReport {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -2411747581779068491L;

	/**
	 * Basic constructor
	 */
	public SantMarginCallAllocationReport() {
		super();
	}

	/**
	 * Main methods. calls the report output & manages possible errors to show to the user.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(final Vector errorMsgsP) {

		try {

			return getReportOutput(errorMsgsP);

		} catch (OutOfMemoryError e2) {
			Log.error("Not enough memory to run this report.", e2);

		} catch (Exception e3) {
			Log.error("Error generating SantMarginCallAllocationEntryReport.", e3);
		}

		return null;
	}

	/**
	 * 
	 * @param errorMsgsP
	 * @return the report output with the rows contracts filtered by PO
	 */
	private ReportOutput getReportOutput(Vector<String> errorMsgs) {

		StandardReportOutput output = new StandardReportOutput(this);
		ReportRow[] rows = ((DefaultReportOutput) super.load(errorMsgs)).getRows();
		List<ReportRow> rowsList = new ArrayList<ReportRow>();

		for (ReportRow row : rows) {

			if (row == null) {
				continue;
			}

			final MarginCallAllocation allocation = (MarginCallAllocation) row.getProperty("Default");

			if (allocation == null) {
				continue;
			}

			CollateralConfig contract = allocation.getCollateralConfig();

			if (contract == null) {
				continue;
			}

			// filter by PO
			if (CollateralUtilities.filterPoByTemplate(getReportTemplate(), contract)) {
				continue;
			}

			// otherwise, add row
			rowsList.add(row);
		}

		output.setRows(rowsList.toArray(new ReportRow[0]));
		return output;
	}

}
