package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class SantHaircutConfigurationReportStyle extends MarginCallReportStyle {

    private static final long serialVersionUID = 1234L;

    public static final String CONTRACT_NAME = "Contract Name";
    public static final String PO_NAME = "PO";
    public static final String CCY_1 = "Ccy 1";
    public static final String CCY_2 = "Ccy 2";
    public static final String VALUE = "Haircut Value";

    public static final String[] DEFAULTS_COLUMNS = {CONTRACT_NAME, PO_NAME, CCY_1, CCY_2, VALUE};

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        SantHaircutConfigurationItem item = row
                .getProperty(SantHaircutConfigurationReportTemplate.HAIRCUT_CONF_ITEM);

        if (columnName.equals(CONTRACT_NAME)) {
            return item.getContractName();
        } else if (columnName.equals(PO_NAME)) {
            return item.getProcessingOrgCode();
        } else if (columnName.equals(CCY_1)) {
            return item.getCurrency1();
        } else if (columnName.equals(CCY_2)) {
            return item.getCurrency2();
        } else if (columnName.equals(VALUE)) {
            return item.getHaircutValue();
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }

    @Override
    public TreeList getTreeList() {

        if (this._treeList != null) {
            return this._treeList;
        }
        final TreeList treeList = super.getTreeList();
        return treeList;

    }
}
