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

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.report.MarginCallAllocationEntryReport;
import com.calypso.tk.report.ReportTemplate;

/**
 * Adaptation of the MarginCallAllocationEntryReport to use multiple POs from ST
 * 
 * @author Guillermo Solano
 * @version 1.0
 */
public class SantMarginCallAllocationEntryReport extends MarginCallAllocationEntryReport {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 3944833421692046363L;

	/**
	 * Basic constructor
	 */
	public SantMarginCallAllocationEntryReport() {
		super();
	}

	/**
	 * override buildQuery. It adds the day to process (-1) and PO filter.
	 */

	@Override
	public String buildQuery(ReportTemplate template) {

		if (CollateralUtilities.isFilteredByST(template)) {

			// if read from ST, put process date as ST ValuationDate - 1
			if (getDate("ProcessStartDate", "ProcessStartPlus", "ProcessStartTenor") == null) {
				final JDate processDate = getValDate().addBusinessDays(-1, Util.string2Vector("SYSTEM"));
				final String processDateString = processDate.toString();

				template.put("ProcessStartDate", processDateString);
				template.put("ProcessStartPlus", null);
				template.put("ProcessStartTenor", null);

				template.put("ProcessEndDate", processDateString);
				template.put("ProcessEndPlus", null);
				template.put("ProcessEndTenor", null);
			}

			// and adapt POs filter from ST
			final String poIds = CollateralUtilities.filterPoIdsByTemplate(template);
			if (!Util.isEmpty(poIds)) {
				template.put("PROCESSING_ORG_IDS", Util.string2Vector(poIds));
			}
		}

		return super.buildQuery(template);
	}

}
