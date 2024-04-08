/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

/**
 * Collateral Positions report, including cash and securities (bonds and equities). This report uses BOPosition for
 * MARGIN_CALL positions.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 30/04/2015
 * 
 */
public class SantCollateralBOPositionReportTemplate extends ReportTemplate {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 3619529560553281181L;

	private static final String COLUMNS_PREFIX = "SantCollateralPosition.";

	// Column names
	public static final String COLLATERAL_POSITION_REPORT_DATE = COLUMNS_PREFIX + "Report Date";
	public static final String COLLATERAL_POSITION_STATUS = COLUMNS_PREFIX + "Status";

	// dates
	protected JDate startDate;
	protected JDate endDate;

	/**
	 * Set Default columns
	 */
	@Override
	public void setDefaults() {

		super.setDefaults();
		Set<String> columns = new LinkedHashSet<String>(
				Arrays.asList(SantCollateralBOPositionReportStyle.DEFAULT_COLUMN_NAMES));
		columns.addAll(Arrays.asList(getColumns(false)));
		setColumns(columns.toArray(new String[0]));

	}

	/**
	 * Call before loading the report (done by the report before the load).
	 */
	@Override
	public void callBeforeLoad() {
		JDate valDate = this._valDate;
		if (valDate == null) {
			valDate = JDate.getNow();
		}

		this.startDate = com.calypso.tk.report.Report.getDate(this, valDate, TradeReportTemplate.START_DATE,
				TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

		this.endDate = Report.getDate(this, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
		if (this.endDate == null) {
			this.endDate = this.startDate.addDays(5);
		}

		setDefaults();
	}

	/**
	 * Set the start and end date to today
	 * 
	 * @param today
	 */
	public void setStartEndDate(final JDate today) {

		if (today != null) {
			this.startDate = this.endDate = today;
		}
	}

	/**
	 * Resets the columns names
	 */
	public void resetColumnsNames() {
		resetColumns();
		setColumns(getColumns(false));
	}

	/**
	 * generates the new customized columns
	 * 
	 * @param flag
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String[] getColumns(boolean flag) {
		Vector<String> columns = toVector(getColumns());

		// Remove Date Columns so we can keep the user configured columns and later we can build Date columns.
		removeDateColumns(columns);

		Vector<String> dateVector = new Vector<String>();

		JDate valDate = this._valDate;
		if (valDate == null) {
			valDate = JDate.getNow();
		}
		if (this.startDate == null) {

			this.startDate = com.calypso.tk.report.Report.getDate(this, valDate, TradeReportTemplate.START_DATE,
					TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
		}
		if (this.startDate == null) {
			if (valDate != null) {
				this.startDate = valDate;
			} else {
				this.startDate = JDate.getNow();
			}
		}

		if (this.endDate == null) {
			this.endDate = Report.getDate(this, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
					TradeReportTemplate.END_TENOR);
		}
		if (this.endDate == null) {
			this.endDate = this.startDate.addDays(5);
		}

		JDate tempDate = this.startDate;
		// builds columns names dependant on dates
		while (tempDate.lte(this.endDate)) {
			String dateString = Util.dateToMString(tempDate);
			dateVector.add(dateString + "_" + SantCollateralPositionReportStyle.FACE_AMOUNT);
			dateVector.add(dateString + "_" + SantCollateralPositionReportStyle.NET_MTM_CCY_MOVEMENT);
			dateVector.add(dateString + "_" + SantCollateralPositionReportStyle.NET_MTM_CCY_AGREE);
			tempDate = tempDate.addDays(1);
		}
		columns.addAll(dateVector);

		return Util.collection2StringArray(columns);
	}

	/**
	 * Removed added columns
	 * 
	 * @param columns
	 */
	protected void removeDateColumns(Vector<String> columns) {
		if (columns.size() > 0) {
			for (int i = (columns.size() - 1); i >= 0; i--) {
				if (columns.get(i).endsWith(SantCollateralBOPositionReportStyle.FACE_AMOUNT)
						|| columns.get(i).endsWith(SantCollateralBOPositionReportStyle.NET_MTM_CCY_MOVEMENT)
						|| columns.get(i).endsWith(SantCollateralBOPositionReportStyle.NET_MTM_CCY_AGREE)) {
					columns.remove(i);
				}

			}
		}
	}
}
