/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

/**
 * SantEmirCVMReportStyle is the report style for Collateral Value Message.
 * 
 * 
 * @author xIS16241
 * 
 */
public class SantEmirCVMReportStyle extends TradeReportStyle {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -1L;
	/**
	 * name of the tree
	 */
	private static final String SANT_EMIR_CVM = "SantEmirCVM";

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String fieldName, final Vector errors) {
		Object value = "";
		final SantEmirRow item = (SantEmirRow) row.getProperty(SantEmirCVMReportItem.ID_SantEmirCVMReportItem);

		if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.EXTERNAL_ID.toString())) {
			value = !Util.isEmpty(String.valueOf(item.getExternalId())) ? item.getExternalId() : "CHAR 20";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.SOURCE_SYSTEM.toString())) {
			value = !Util.isEmpty(item.getSourceSystem()) ? item.getSourceSystem() : "NUMBER";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.MESSAGE_TYPE.toString())) {
			value = !Util.isEmpty(item.getMessageType()) ? item.getMessageType() : "CHAR 3";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.ACTIVITY.toString())) {
			value = !Util.isEmpty(item.getActivity()) ? item.getActivity() : "CHAR 3";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.TRANSACTION_TYPE.toString())) {
			value = !Util.isEmpty(item.getTransactionType()) ? item.getTransactionType() : "CHAR 3";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.PRODUCT.toString())) {
			value = !Util.isEmpty(item.getProduct()) ? item.getProduct() : "CHAR 20";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.TAG.toString())) {
			value = !Util.isEmpty(item.getTag()) ? item.getTag() : "";
		} else if (fieldName.equalsIgnoreCase(SantEmirCVMReportTemplate.VALUE.toString())) {
			value = !Util.isEmpty(item.getValue()) ? item.getValue() : "";
		}

		if (value == null) {
			value = super.getColumnValue(row, fieldName, errors);
		}

		return value;

	}

	@Override
	public String[] getDefaultColumns() {
		return SantEmirCVMColumns.getColumns();
	}

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();

		final CalypsoTreeNode emirTreeNode = new CalypsoTreeNode(SANT_EMIR_CVM);
		treeList.add(emirTreeNode);

		final String[] columns = SantEmirCVMReportTemplate.getColumns();
		for (int iColumn = 0; iColumn < columns.length; iColumn++) {
			treeList.add(emirTreeNode, columns[iColumn]);
		}

		return treeList;
	}
}
