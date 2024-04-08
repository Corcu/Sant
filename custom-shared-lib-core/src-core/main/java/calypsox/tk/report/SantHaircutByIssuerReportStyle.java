package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class SantHaircutByIssuerReportStyle extends MarginCallReportStyle {

    private static final long serialVersionUID = 123L;

    public static final String ISSUER_SHORT_NAME = "Issuer Short Name";
    public static final String ISSUER_FULL_NAME = "Issuer Full Name";
    public static final String ISSUER_COUNTRY = "Issuer Country";
    public static final String ISSUER_TENOR = "Tenor";
    public static final String ISSUER_HAIRCUT = "Haircut";

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        LegalEntity issuer = row.getProperty(ReportRow.LEGAL_ENTITY);

        SantHaircutByIssuerItem haircutItem = row
                .getProperty(SantHaircutByIssuerItem.SANT_HAIRCUT_BY_ISSUER_ITEM);

        if (columnName.equals(ISSUER_SHORT_NAME)) {
            return issuer.getCode();
        } else if (columnName.equals(ISSUER_FULL_NAME)) {
            return issuer.getName();
        } else if (columnName.equals(ISSUER_COUNTRY)) {
            return issuer.getCountry();
        } else if (columnName.equals(ISSUER_TENOR)) {
            return (haircutItem.getTenor() == -1) ? "OPEN" : (haircutItem.getTenor() / 360 + "Y");
        } else if (columnName.equals(ISSUER_HAIRCUT)) {
            return haircutItem.getValue() * 100;
        }
        return super.getColumnValue(row, columnName, errors);

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
