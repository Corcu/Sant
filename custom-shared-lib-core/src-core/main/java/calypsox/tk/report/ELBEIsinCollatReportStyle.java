/* Actualizado por David Porras Mart?nez 23-11-11 */

package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.report.MarginCallReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;


public class ELBEIsinCollatReportStyle extends MarginCallReportStyle {

    private static final String COD_LAYOUT = "CodLayout";
    private static final String EXTRACT_DATE = "ExtractDate";
    private static final String POS_TRANS_DATE = "PosTransDate";
    private static final String SOURCE_APP = "SourceApplication";

    // COL_OUT_013
    private static final String FRONT_ID = "frontID";
    private static final String CLAVE_COLAT = "claveColat";
    private static final String ISIN = "isinTitulo";
    private static final String SENAL = "senal";
    private static final String BALANCE = "balanTituloDIVISA";
    private static final String MONEDA_BASE = "monedaBase";
    private static final String GROSS_EXP = "grossExposureDIVISA";
    private static final String DIVISA = "divisa";
    private static final String ASSET_TYPE = "Asset Type";

    private static final String MARGIN_TYPE = "Margin Type";
    private static final String LE_SHORT_NAME = "Legal Entity short name";

    // Default columns.
    public static final String[] DEFAULTS_COLUMNS = {COD_LAYOUT, EXTRACT_DATE, POS_TRANS_DATE, SOURCE_APP, FRONT_ID,
            CLAVE_COLAT, ISIN, SENAL, BALANCE, MONEDA_BASE, GROSS_EXP, DIVISA, ASSET_TYPE, MARGIN_TYPE, LE_SHORT_NAME};
    private static final long serialVersionUID = 1571825408920486782L;

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(COD_LAYOUT);
        treeList.add(EXTRACT_DATE);
        treeList.add(POS_TRANS_DATE);
        treeList.add(SOURCE_APP);
        // COL_OUT_013
        treeList.add(FRONT_ID);
        treeList.add(CLAVE_COLAT);
        treeList.add(ISIN);
        treeList.add(SENAL);
        treeList.add(BALANCE);
        treeList.add(MONEDA_BASE);
        treeList.add(GROSS_EXP);
        treeList.add(DIVISA);
        treeList.add(ASSET_TYPE);
        treeList.add(MARGIN_TYPE);
        treeList.add(LE_SHORT_NAME);

        return treeList;
    }

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
                                 final Vector errors) {
        final ELBEIsinCollatItem item = row.getProperty(ELBEIsinCollatItem.ELBE_ISIN_COLLAT_ITEM);

        if (columnName.compareTo(COD_LAYOUT) == 0) {
            return item.getCodLayout();
        } else if (columnName.compareTo(EXTRACT_DATE) == 0) {
            return item.getExtractDate();
        } else if (columnName.compareTo(POS_TRANS_DATE) == 0) {
            return item.getPosTransDate();
        } else if (columnName.compareTo(SOURCE_APP) == 0) {
            return item.getSourceApp();
        }
        // COL_OUT_013
        else if (columnName.compareTo(FRONT_ID) == 0) {
            return item.getFrontId();
        } else if (columnName.compareTo(CLAVE_COLAT) == 0) {
            return item.getClaveColat();
        } else if (columnName.compareTo(ISIN) == 0) {
            return item.getIsinTitulo();
        } else if (columnName.compareTo(SENAL) == 0) {
            return item.getSenal();
        } else if (columnName.compareTo(BALANCE) == 0) {
            return item.getBalanTitulosDivisa();
        } else if (columnName.compareTo(MONEDA_BASE) == 0) {
            return item.getMonedaBase();
        } else if (columnName.compareTo(GROSS_EXP) == 0) {
            return item.getGrossExpoDivisa();
        } else if (columnName.compareTo(DIVISA) == 0) {
            return item.getDivisa();
            // GSM: 04/07/14. Added Asset type for MMOO
        } else if (columnName.compareTo(ASSET_TYPE) == 0) {
            return item.getAssetType();
        } else if (columnName.compareTo(MARGIN_TYPE) == 0) {
            return item.getMarginType();
        } else if (columnName.compareTo(LE_SHORT_NAME) == 0) {
            return item.getLegalEntityShortName();

        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }
}