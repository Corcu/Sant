package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.JDate;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class Opt_HaircutDefinitionReportStyle extends MarginCallReportStyle {

    private static final long serialVersionUID = 123L;

    public static final String COLLATERAL_AGREEMENT = "Collateral Agreement";
    public static final String FILTER_NAME = "StaticDataFilter";
    public static final String TENOR = "Tenor";
    public static final String MATURITY_START = "MaturityStart";
    public static final String MATURITY_END = "MaturityEnd";
    public static final String VALUE = "HaircutValue";
    public static final String OWNER = "Owner";
    public static final String OWNER_CODE = "Owner Code";

    public static final String[] DEFAULTS_COLUMNS = {COLLATERAL_AGREEMENT, OWNER_CODE, FILTER_NAME, TENOR, MATURITY_START, MATURITY_END, OWNER, VALUE};

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        Opt_HaircutDefinitionItem item = row
                .getProperty(Opt_HaircutDefinitionReportTemplate.OPT_HAIRCUT_DEF_ITEM);

        if (columnName.equals(COLLATERAL_AGREEMENT)) {
            return item.getCollateralAgreement();
        } else if (columnName.equals(FILTER_NAME)) {
            return item.getFilterName();
        } else if (columnName.equals(TENOR)) {
            return item.getTenor();
        } else if (columnName.equals(MATURITY_START)) {
            return JDate.getNow().addTenor(item.getMaturityStartDate());
        } else if (columnName.equals(MATURITY_END)) {
            return JDate.getNow().addTenor(item.getMaturityEndDate());
        } else if (columnName.equals(VALUE)) {
            return item.getHaircutValue() * 100;
        } else if (columnName.equals(OWNER)) {
            return item.getOwner();
        } else if (columnName.equals(OWNER_CODE)) {
            return item.getOwnerCode();
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
