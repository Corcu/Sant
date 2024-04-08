package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.ReportRow;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

public class SantConcentrationLimitsConfigurationReportStyle extends MarginCallReportStyle {

    private static final long serialVersionUID = 12345L;

    public static final String CONTRACT_NAME = "Contract Name";
    public static final String PO_NAME = "PO";
    public static final String RULE_TYPE = "Rule Type";
    public static final String MIN_MAX = "Min/Max";
    public static final String RULE_PARAMETER = "Rule parameter";
    public static final String PARAM_VALUE = "Parameter value";

    public static final String[] DEFAULTS_COLUMNS = {CONTRACT_NAME, PO_NAME, RULE_TYPE, MIN_MAX, RULE_PARAMETER, PARAM_VALUE};

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        SantConcentrationLimitsConfigurationItem item = row
                .getProperty(SantConcentrationLimitsConfigurationReportTemplate.CONC_LIMITS_ITEM);

        if (columnName.equals(CONTRACT_NAME)) {
            return item.getContract().getName();
        } else if (columnName.equals(PO_NAME)) {
            return item.getProcessingOrgCode();
        } else if (columnName.equals(RULE_TYPE)) {
            return item.getRuleType();
        } else if (columnName.equals(MIN_MAX)) {
            return item.getMinMax();
        } else if (columnName.equals(RULE_PARAMETER)) {
            return item.getRuleParameter();
        } else if (columnName.equals(PARAM_VALUE)) {
            return format(item.getParameterValue());
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }

    /**
     * Format a double value to use a period to separate the thousands and a comma for the decimals.
     *
     * @param parameterValue
     * @return
     */
    private String format(double parameterValue) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("###,###.###");
        return df.format(parameterValue);
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
