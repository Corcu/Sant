/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.core.GenericReg_EmirReport;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.util.Vector;

/**
 * SantEmirSnapshotReportStyle is the report style for Emir Snapshot report.
 * 
 * 
 */
// CAL_EMIR_026
public class GenericReg_SantEmirValuationReportStyle extends TradeReportStyle {

	/**
	 * Constant VALUATION_TYPE
	 */
	private static final String VALUATION_TYPE = "VAL";

	/**
	 * Constant DELIMITER
	 */
	private static final String DELIMITER = ",";

	private static final String FOREIGN_EXCHANGE = "ForeignExchange";
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -1L;
	/**
	 * name of the tree
	 */
	protected static final String SANT_EMIR_VALUATION = "SantEmirValuation";

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			final Vector errors) {
		Object value = "";
		final GenericReg_EmirReport item = (GenericReg_EmirReport) row
				.getProperty(SantEmirValuationReportItem.SANT_EMIR_VALUATION_ITEM);

		if (item != null) {
			final String tagName = item.getTag();

			if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.EXTERNAL_ID)) {
				value = item.getPartenonId();
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.FIXED_CODE
							.toString())) {
				value = item.getFixedCode() + DELIMITER + VALUATION_TYPE;
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.ACTION
							.toString())) {
				value = item.getAction();
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.TRANSTYPE
							.toString())) {
				value = item.getTranstype();
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.FIXED_TYPE
							.toString())) {
				value = FOREIGN_EXCHANGE;
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.TAG_NAME
							.toString())) {
				value = tagName;
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.TAG_VALUE
							.toString())) {
				value = item.getValue();
			} else if (columnName
					.equalsIgnoreCase(SantEmirSnapshotReduxReportStyle.TRANSACTIONIDPARTY1
							.toString())) {
				value = item.getTradeId();
			}

			if (value == null) {
				value = super.getColumnValue(row, columnName, errors);
			}
		}
		return value;

	}

	@Override
	public String[] getDefaultColumns() {
		return GenericReg_SantEmirSnapshotColumns.getColumns();
	}

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();

		final CalypsoTreeNode emirTreeNode = new CalypsoTreeNode(
				SANT_EMIR_VALUATION);
		treeList.add(emirTreeNode);

		final String[] columns = GenericReg_SantEmirSnapshotColumns.getColumns();
		for (int iColumn = 0; iColumn < columns.length; iColumn++) {
			treeList.add(emirTreeNode, columns[iColumn]);
		}

		return treeList;
	}
}
