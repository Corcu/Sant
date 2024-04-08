package calypsox.tk.report;

import com.calypso.tk.report.MarginCallEntryReportStyle;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class InitialMarginContractConfigReportStyle extends MarginCallEntryReportStyle
        implements InitialMarginContractConfigConstants {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -6602889808584555613L;

    public static final String SEND_FIELD = "Send";

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
            throws InvalidParameterException {

        if (row == null) {
            return null;
        } else if (columnName.equals(SEND_FIELD)) {
            if (Boolean.TRUE.equals(row.getProperty(QEF_PROPERTY))) {
                return Boolean.TRUE;
            }
            if (Boolean.FALSE.equals(row.getProperty(QEF_PROPERTY))) {
                return Boolean.FALSE;
            }
            if (SELECT_TRUE.equals(this.getProperty(InitialMarginContractConfigConstants.QEF_SELECT_ALL))) {
                return Boolean.TRUE;
            } else if (SELECT_TRUE.equals(this.getProperty(InitialMarginContractConfigConstants.QEF_UNSELECT_ALL))) {
                return Boolean.FALSE;
            } else {
                return Boolean.FALSE;
            }
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }

}
