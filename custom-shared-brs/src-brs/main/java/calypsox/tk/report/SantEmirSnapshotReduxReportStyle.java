/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.core.GenericReg_EmirReport;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.util.Vector;

/**
 * GenericReg_SantEmirSnapshotReduxReportStyle is the report style for Emir
 * Snapshot Redux report.
 * 
 */
public class SantEmirSnapshotReduxReportStyle extends
        TradeReportStyle {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -1L;

    /**
     * name of the tree
     */
    private static final String SANT_EMIR_SNAPSHOT = "SantEmirSnapshotRedux";

    public static final String EXTERNAL_ID = "ExternalID";
    public static final String FIXED_CODE = "FIXED_CODE";
    public static final String FIXED_SNP = "FIXED_SNP";

    public static final String ACTION = "ACTION";
    public static final String TRANSTYPE = "TRANSTYPE";
    public static final String FIXED_TYPE = "FIXED_TYPE";
    public static final String TAG_NAME = "TAG_NAME";
    public static final String TAG_VALUE = "TAG_VALUE";
    public static final String TRANSACTIONIDPARTY1 = "TRANSACTIONIDPARTY1";

    public static final String[] DEFAULTS_COLUMNS = { EXTERNAL_ID, FIXED_CODE, FIXED_SNP,
            ACTION, TRANSTYPE, FIXED_TYPE, TAG_NAME, TAG_VALUE,
            TRANSACTIONIDPARTY1 };

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
                                 final Vector errors) {
        Object value = EmirSnapshotReduxConstants.EMPTY_SPACE;
        final GenericReg_EmirReport item = (GenericReg_EmirReport) row
                .getProperty(SantEmirSnapshotReportItem.SANT_EMIR_SNAPSHOT_ITEM);

        if (columnName.equalsIgnoreCase(EXTERNAL_ID)) {
            value = item.getMurexTradeId();
        } else if (columnName.equalsIgnoreCase(FIXED_CODE)) {
            value = item.getFixedCode();
        } else if (columnName.equalsIgnoreCase(FIXED_SNP)) {
            value =  EmirSnapshotReduxConstants.SNAPSHOT_TYPE;
        } else if (columnName.equalsIgnoreCase(ACTION)) {
            value = item.getAction();
        } else if (columnName.equalsIgnoreCase(TRANSTYPE)) {
            value = item.getTranstype();
        } else if (columnName.equalsIgnoreCase(FIXED_TYPE)) {
            value = EmirSnapshotReduxConstants.CREDIT_TOTAL_RETURN_SWAP;
        } else if (columnName.equalsIgnoreCase(TAG_NAME)) {
            value = item.getTag();
        } else if (columnName.equalsIgnoreCase(TAG_VALUE)) {
            value = item.getValue();
        } else if (columnName.equalsIgnoreCase(TRANSACTIONIDPARTY1)) {
            value = item.getTradeId();
        }

        if (value == null) {
            value = super.getColumnValue(row, columnName, errors);
        }

        return value;

    }

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();

        final CalypsoTreeNode emirTreeNode = new CalypsoTreeNode(
                SANT_EMIR_SNAPSHOT);
        treeList.add(emirTreeNode);

        final String[] columns = DEFAULTS_COLUMNS;
        for (int iColumn = 0; iColumn < columns.length; iColumn++) {
            treeList.add(emirTreeNode, columns[iColumn]);
        }

        return treeList;
    }


}
