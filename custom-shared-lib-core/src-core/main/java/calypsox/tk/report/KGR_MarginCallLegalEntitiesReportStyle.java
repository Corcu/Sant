/**
 *
 */
package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.MarginCallPositionBaseReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 *
 */
public class KGR_MarginCallLegalEntitiesReportStyle extends CollateralConfigReportStyle {

    /**
     *
     */
    private static final long serialVersionUID = 8959480803742146192L;
    protected static final String DEFAULT_String = "Default";

    protected static final String CONTRACT = "MarginCallConfig";
    protected static final String POSITION = "Position";
    protected static final String HAIRCUT_PROP = "Haircut";
    protected static final String HEAD_CLONE = "HEAD_CLONE";
    protected static final char fieldSeparator = 28;
    /*
     * Column names Strings
     */
    protected static final String HEADER = "Header";
    protected static final String TRANSACTIONID = "Transaction ID";
    protected static final String TRANSACTION_DATE = "Transaction Date";
    protected static final String TRANSACTION_TYPE = "Transaction Type";
    protected static final String MATURITY_DATE = "Maturity Date";
    protected static final String BOND_MATURITY_DATE = "Bond Maturity Date";
    protected static final String RECEIVED = "Received";
    protected static final String CONTRACT_INDEPENDENT_AMOUNT = "CONTRACT_INDEPENDENT_AMOUNT";
    protected static final String COLLATOBLIGATIONAMOUNT = "Collateral Obligation Amount";
    protected static final String COLLATBONDNOMINAL = "Collateral Bond Nominal";
    protected static final String ACTION = "Action";
    protected static final String OFFICE = "Office";
    protected static final String CURRENCY = "Currency";
    protected static final String RECONCILIATIONTYPE = "Reconciliation Type";
    protected static final String AGREEMENTTYPE = "Agreement Type";
    protected static final String AGREEMENTID = "Agreement Id";
    protected static final String ISIN = "ISIN";
    protected static final String ISSUER = "Issuer";
    protected static final String HAIRCUT = "Haircut";
    protected static final String SOURCE = "Source";
    protected static final String CONCILIA = "Conciliation Field";
    protected static final String HEADCLONECOLUMN = "Head Clone";
    /**
     * Column values Strings
     */
    protected static final String HEADER_VALUE = "D01StdI#BATCHGLCSDMIL  225";
    protected static final String TRANSACTION_TYPE_SEC = "COLLBOND";
    protected static final String RECONCILIATION_TYPE = "P";
    protected static final String ACTION_VALUE = "A";
    protected static final String TRANSACTION_TYPE_CASH = "COLL";
    protected static final String SOURCE_VALUE = "266:CALYPSO";
    protected static final String CONCILIA_FIELD = "221:MAD_CAL_COL";

    private MarginCallPositionBaseReportStyle positionStyle = new MarginCallPositionBaseReportStyle();

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        Object value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(row, columnName, this);
        if (value != null) {
            return value;
        }
        if (HEADCLONECOLUMN.equals(columnName)) {
            value = row.getProperty(HEAD_CLONE);
        } else if (row.getProperty(DEFAULT_String) instanceof MarginCallPositionDTO) {
            value = getPositionColumnValues(columnName, row, errors);
        } else {
            value = super.getColumnValue(row, columnName, errors);
        }
        return value;
    }

    /**
     * Used in position related kgr templates
     * @param columnName
     * @param row
     * @param errors
     * @return
     */
    private Object getPositionColumnValues(String columnName, ReportRow row, Vector<?> errors) {
        Object value = "";
        if (HEADER.equals(columnName))
            value = HEADER_VALUE;
        else if (TRANSACTIONID.equals(columnName))
            value = "1: " + ((CollateralConfig) row.getProperty(CONTRACT)).getId()
                    + ((MarginCallPositionDTO) row.getProperty(POSITION)).getCurrency();
        else if (TRANSACTION_TYPE.equals(columnName))
            value = "2:" + ((MarginCallPositionDTO) row.getProperty(POSITION)).getType();
        else if (ACTION.equals(columnName))
            value = ("3:" + ACTION_VALUE);
        else if (RECONCILIATIONTYPE.equals(columnName))
            value = ("4:" + RECONCILIATION_TYPE);
        else if (TRANSACTION_DATE.equals(columnName))
            value = "10:" + ((MarginCallPositionDTO) row.getProperty(POSITION)).getPositionDate();
            // One week later than position date
        else if (MATURITY_DATE.equals(columnName))
            value = "16:" + (((MarginCallPositionDTO) row.getProperty(POSITION)).getPositionDate()).addDays(7);
        else if (BOND_MATURITY_DATE.equals(columnName)) {
            value = "201:" + ((SecurityPositionDTO) row.getProperty(POSITION)).getProduct().getMaturityDate();
        } else if (OFFICE.equals(columnName)) {
            String aliasEntityKGR = CollateralUtilities.getAliasEntityKGR(DSConnection.getDefault(),
                    ((CollateralConfig) row.getProperty(CONTRACT)).getProcessingOrg().getEntityId());
            value = getKGROffice(aliasEntityKGR, row);
        } else if (RECEIVED.equals(columnName))
            value = "93:" + SantCollateralConfigUtil.getContractDirectionV14Value(row.getProperty(CONTRACT));
        else if (CURRENCY.equals(columnName))
            value = "20:" + ((MarginCallPositionDTO) row.getProperty(POSITION)).getCurrency();
        else if (COLLATOBLIGATIONAMOUNT.equals(columnName) || COLLATBONDNOMINAL.equals(columnName))
            value = getCollateralAmount(((MarginCallPositionDTO) row.getProperty(POSITION)).getContractValue());
        else if (ISIN.equals(columnName)) {
            Product security = ((SecurityPositionDTO) row.getProperty(POSITION)).getProduct();
            value = "62:" + ((Bond) security).getSecCode(ISIN);
        } else if (ISSUER.equals(columnName)) {
            Product security = ((SecurityPositionDTO) row.getProperty(POSITION)).getProduct();
            value = "72:" + ((Bond) security).getIssuerId();
        } else if (HAIRCUT.equals(columnName))
            value = "61:" + row.getProperty(HAIRCUT_PROP);
        else if (AGREEMENTTYPE.equals(columnName))
            value = "265:" + ((CollateralConfig) row.getProperty(CONTRACT)).getContractType();
        else if (AGREEMENTID.equals(columnName))
            value = "271:" + ((CollateralConfig) row.getProperty(CONTRACT)).getName();
        else if (SOURCE.equals(columnName))
            value = SOURCE_VALUE;
        else if (CONCILIA.equals(columnName))
            value = CONCILIA_FIELD;
        else
            value = positionStyle.getColumnValue(row, columnName, errors);
        // Adds field separator
        return value.toString() + fieldSeparator;

    }

    private String getCollateralAmount(Double amount) {
        String value = CollateralUtilities.formatNumber(Math.abs(amount));
        if (value.contains(",")) {
            return "21:" + value.replace(',', '.');
        } else {
            return "21:" + value;
        }
    }

    private String getKGROffice(String aliasEntityKGR, ReportRow row) {
        if (aliasEntityKGR.equals("")) {
            return "7:" + ((CollateralConfig) row.getProperty(CONTRACT)).getProcessingOrg().getName(); // office
        } else {
            return "7:" + aliasEntityKGR; // office
        }
    }
}
