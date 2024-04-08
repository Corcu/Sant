package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.ConcentrationLimit;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.report.ConcentrationReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

//Project: Concentration Limits

public class SantConcentrationLimitsReportStyle
        extends ConcentrationReportStyle {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Columns showing the id of the contract.
     */
    public static final String COLUMN_NAME_CONTRACT_ID = "Contract ID";

    /**
     * Column showing the name of the contract.
     */
    public static final String COLUMN_NAME_CONTRACT_NAME = "Contract Name";

    /**
     * Column showing the type of the contract.
     */
    public static final String COLUMN_NAME_CONTRACT_TYPE = "Contract Type";

    /**
     * Column showing the ISIN of the security used in this concentration.
     */
    public static final String COLUMN_NAME_ISIN = "ISIN";

    /**
     * Name of the Static Data Filter element that checks for the ISIN of a
     * Security.
     */
    private static final String SDF_ELEMENT_NAME_ISIN = "SEC_CODE.ISIN";

    @Override
    public Object getColumnValue(ReportRow row, String columnName,
                                 Vector errors) {
        Object value = null;

        if (COLUMN_NAME_CONTRACT_ID.equals(columnName)) {
            value = getContractId(row);
        } else if (COLUMN_NAME_CONTRACT_NAME.equals(columnName)) {
            value = getContractName(row);
        } else if (COLUMN_NAME_CONTRACT_TYPE.equals(columnName)) {
            value = getContractType(row);
        } else if (COLUMN_NAME_ISIN.equals(columnName)) {
            value = getIsin(row);
        } else {
            value = super.getColumnValue(row, columnName, errors);
        }

        return value;
    }

    /**
     * Retrieves a contract from within a ReportRow.
     *
     * @param row The ReportRow to get the contract from.
     * @return The contract in the given row.
     */
    private CollateralConfig getContract(ReportRow row) {
        CollateralConfig contract = null;

        Object rawContract = row.getProperty(
                SantConcentrationLimitsReport.ROW_PROPERTY_COLLATERAL_CONFIG);
        if (rawContract instanceof CollateralConfig) {
            contract = (CollateralConfig) rawContract;
        }

        return contract;
    }

    /**
     * Retrieves the id of the contract in the given row.
     *
     * @param row The Report Row.
     * @return The id of the contract.
     */
    private Integer getContractId(ReportRow row) {
        Integer contractId = null;

        CollateralConfig contract = getContract(row);
        if (contract != null) {
            contractId = contract.getId();
        }

        return contractId;
    }

    /**
     * Retrieves the name of the contract in the given row.
     *
     * @param row The Report Row.
     * @return The name of the contract.
     */
    private String getContractName(ReportRow row) {
        String contractName = null;

        CollateralConfig contract = getContract(row);
        if (contract != null) {
            contractName = contract.getName();
        }

        return contractName;
    }

    /**
     * Retrieves the type of the contract in the given row.
     *
     * @param row The Report Row.
     * @return The name of the contract.
     */
    private String getContractType(ReportRow row) {
        String contractType = null;

        CollateralConfig contract = getContract(row);
        if (contract != null) {
            contractType = contract.getContractType();
        }

        return contractType;
    }

    /**
     * Retrieves the Concentration Limit stored in the given Report Row.
     *
     * @param row The Report Row.
     * @return The Concentration Limit.
     */
    private ConcentrationLimit getConcentrationLimit(ReportRow row) {
        ConcentrationLimit limit = null;

        Object rawLimit = row.getProperty(
                SantConcentrationLimitsReport.ROW_PROPERTY_CONCENTRATION_LIMIT);
        if (rawLimit instanceof ConcentrationLimit) {
            limit = (ConcentrationLimit) rawLimit;
        }

        return limit;
    }

    /**
     * Retrieves the ISIN code the concentration limit stored in the given row
     * is checking for. If the row contains a concentration limit that is using
     * a Static Data Filter that is checking if a security has a certain ISIN,
     * this method returns that ISIN code. Otherwise it returns an empty string.
     *
     * @param row The report row.
     * @return The value of the ISIN in the filter, or an empty string if the
     * filter is not checking for any ISIN code.
     */
    private String getIsin(ReportRow row) {
        String isin = "";

        ConcentrationLimit limit = getConcentrationLimit(row);
        if (limit != null) {
            StaticDataFilter sdf = BOCache.getStaticDataFilter(DSConnection.getDefault(), limit.getFilterName());
            for (StaticDataFilterElement element : sdf.getElements()) {
                if (SDF_ELEMENT_NAME_ISIN.equals(element.getName()) && !Util.isEmpty(element.getValues())) {
                    Object rawIsin = element.getValues().get(0);
                    if (rawIsin instanceof String) {
                        isin = (String) rawIsin;
                    }
                }
            }
        }

        return isin;
    }

}
