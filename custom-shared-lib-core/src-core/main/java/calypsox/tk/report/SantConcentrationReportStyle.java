/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.concentration.ConcentrationReportItem;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

public class SantConcentrationReportStyle extends com.calypso.tk.report.BOSecurityPositionReportStyle {

	private static final long serialVersionUID = 6881928559332741996L;

	// private static final String ISIN = "Isin";
	public static final String SDFILTER_NAME = "SDFIlter Name";
	public static final String MOVEMENT_TYPE = "Movement Type";
	public static final String DATE = "Date";
	public static final String BOOKS = "Books";

	public static final String COMPLETE_POSITION = "Complete Position";
	public static final String LIMITED_POSITION = "Limited Position";

	public static final String PERCENTAGE = "Percentage";
	public static final String CALCULATED_PERCENTAGE = "Calculated Percentage";
	public static final String CRITERIA = "Criteria";

	public static final String[] DEFAULT_COLUMNS = { CRITERIA, SDFILTER_NAME, MOVEMENT_TYPE, LIMITED_POSITION,
			COMPLETE_POSITION, PERCENTAGE, CALCULATED_PERCENTAGE, BOOKS };

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

		ConcentrationReportItem reportItem = (ConcentrationReportItem) row
				.getProperty(SantConcentrationReportTemplate.CONCENTRATION_REPORT_ITEM);
		if (reportItem == null) {
			throw new InvalidParameterException("Invalid row " + row + ". Cannot locate Inventory object");
		}

		if (CRITERIA.equals(columnId)) {
			return reportItem.getCriteria();
		} else if (SDFILTER_NAME.equals(columnId)) {
			return reportItem.getFilterName();
		} else if (MOVEMENT_TYPE.equals(columnId)) {
			return reportItem.getMovementType();
		} else if (LIMITED_POSITION.equals(columnId)) {
			return CollateralUtilities.formatAmount(reportItem.getLimitedPosValue(), "EUR");
		} else if (COMPLETE_POSITION.equals(columnId)) {
			return CollateralUtilities.formatAmount(reportItem.getGlobalPosValue(), "EUR");
		} else if (BOOKS.equals(columnId)) {
			return reportItem.getBookList();
		} else if (PERCENTAGE.equals(columnId)) {
			return reportItem.getPercentage();
		} else if (CALCULATED_PERCENTAGE.equals(columnId)) {
			return CollateralUtilities.formatAmount(reportItem.getCalculatedPercentage(), "EUR");
			// return reportItem.getCalculatedPercentage();
		}
		return null;
	}

	@Override
	public boolean containsPricingEnvDependentColumns(ReportTemplate template) {
		return true;
	}

}
