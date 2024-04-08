/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.util.InstantiateUtil;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Vector;

public class SantKPIDailyTaskReportStyle extends ReportStyle {

    private static final long serialVersionUID = -5148532455585583634L;

    public static final String CONTRACT_ID = "CONTRACT ID";
    public static final String OWNER = "OWNER";
    public static final String ID = "ID";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String TYPE = "TYPE";
    public static final String FREQUENCY = "FREQUENCY";
    public static final String EVENT = "EVENT";
    public static final String MARGIN_CALL_SITUATION = "MARGIN CALL SITUATION";
    public static final String STATUS = "STATUS";
    public static final String EXCHANGE_DIRECTION = "EXCHANGE DIRECTION";
    public static final String ELIGIBLE_ASSET_TYPE = "ELIGIBLE ASSET TYPE";
    public static final String EFFECTIVE_ASSET_TYPE = "EFFECTIVE ASSET TYPE";
    public static final String AGREEMENT_INPUT_DATE = "AGREEMENT INPUT DATE";
    public static final String EVENT_DATE = "EVENT DATE";
    public static final String INPUT_DATE = "INPUT DATE";
    public static final String BASE_CURRENCY = "BASE CURRENCY";
    public static final String EFFECTIVE_CURRENCY = "EFFECTIVE CURRENCY";
    public static final String OWNER_SNP_RATING = "OWNER S&P RATING";
    public static final String OWNER_MOODYS_RATING = "OWNER MOODYS RATING";
    public static final String COUNTERPARTY_SNP_RATING = "COUNTERPARTY S&P RATING";
    public static final String COUNTERPARTY_MOODYS_RATING = "COUNTERPARTY MOODYS RATING";
    public static final String EXPOSURE = "EXPOSURE";
    public static final String EXPOSURE_COUNTERPARTY = "EXPOSURE COUNTERPARTY";
    public static final String INDEPENDENT = "INDEPENDENT";
    public static final String THRESHOLD = "THRESHOLD";
    public static final String MTA = "MTA";
    public static final String LAST_BALANCE = "LAST BALANCE";
    public static final String MARGIN_CALL = "MARGIN CALL";
    public static final String COLLATERAL_IN_TRANSIT = "COLLATERAL IN TRANSIT";
    public static final String BALANCE = "BALANCE";
    public static final String DISPUTE = "DISPUTE";
    public static final String DISPUTE_TYPE = "DISPUTE TYPE";
    public static final String DISPUTE_EXPOSURE = "% DISPUTE / EXPOSURE";
    public static final String DISPUTE_DATE = "DISPUTE DATE";
    public static final String MARGIN_CALL_CALCULATION = "MARGIN CALL CALCULATION";
    public static final String DEALS = "DEALS";
    public static final String DIFF_MTM = "DIFF MTM";
    public static final String TIPO_DE_DISCREPANCIA = "TIPO DE DISCREPANCIA";
    public static final String LIMIT_AMOUNT = "LIMIT AMOUNT";
    public static final String AVAILABLE_LIMIT = "AVAILABLE LIMIT";
    public static final String OWNER_FITCH_RATING = "OWNER FITCH RATING";
    public static final String COUNTERPARTY_FITCH_RATING = "COUNTERPARTY FITCH RATING";

    //required for SA-CCR - GSM 17/01/2017
    private CollateralConfigReportStyle collateralConfigReportStyle = null;
    public static String MARGIN_CALL_CONFIG_PREFIX = "MarginCallConfig.";
    private static final String COLLATERAL_CONFIG_TYPE = "MarginCallConfig";

    public static final String[] DEFAULT_COLUMNS = {OWNER, ID, DESCRIPTION, TYPE, FREQUENCY, EVENT,
            MARGIN_CALL_SITUATION, STATUS, EXCHANGE_DIRECTION, ELIGIBLE_ASSET_TYPE, EFFECTIVE_ASSET_TYPE,
            AGREEMENT_INPUT_DATE, EVENT_DATE, INPUT_DATE, BASE_CURRENCY, EFFECTIVE_CURRENCY, OWNER_SNP_RATING,
            OWNER_MOODYS_RATING, COUNTERPARTY_SNP_RATING, COUNTERPARTY_MOODYS_RATING, EXPOSURE, EXPOSURE_COUNTERPARTY,
            INDEPENDENT, THRESHOLD, MTA, LAST_BALANCE, MARGIN_CALL, COLLATERAL_IN_TRANSIT, BALANCE, DISPUTE,
            DISPUTE_TYPE, DISPUTE_EXPOSURE, DISPUTE_DATE, MARGIN_CALL_CALCULATION, DEALS, DIFF_MTM,
            TIPO_DE_DISCREPANCIA, LIMIT_AMOUNT, AVAILABLE_LIMIT, OWNER_FITCH_RATING, COUNTERPARTY_FITCH_RATING};

    public TreeList getTreeList() {

        @SuppressWarnings("deprecation") final TreeList treeList = super.getTreeList();

        if (collateralConfigReportStyle == null) {
            collateralConfigReportStyle = getMarginCallConfigReportStyle();
        }

        // add CollateralConfig tree
        if (collateralConfigReportStyle != null) {
            addSubTreeList(treeList, new Vector<String>(), MARGIN_CALL_CONFIG_PREFIX,
                    collateralConfigReportStyle.getTreeList());
        }

        return treeList;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        Map<String, Object> columnMap = (Map<String, Object>) row.getProperty("SantKPIDailyTask");

        //Collateral Config columns names
        if (getMarginCallConfigReportStyle().isMarginCallConfigColumn(MARGIN_CALL_CONFIG_PREFIX, columnName)) {
            //required for SA-CCR - GSM 17/01/2017 - Added collateral config style
            final CollateralConfig contract = (CollateralConfig) columnMap.get(MARGIN_CALL_CONFIG_PREFIX);
            if (contract != null) {
                ReportRow newRow = row.clone();
                newRow.setProperty(COLLATERAL_CONFIG_TYPE, contract);
                String name = getMarginCallConfigReportStyle().getRealColumnName(MARGIN_CALL_CONFIG_PREFIX, columnName);
                return getMarginCallConfigReportStyle().getColumnValue(newRow, name, errors);
            }
        }

        return columnMap.get(columnName);

    }

    /**
     * @return custom CollateralConfigReportStyle. If custom code is not found, it will retrieve the custom version
     */
    private CollateralConfigReportStyle getMarginCallConfigReportStyle() {
        try {
            if ((this.collateralConfigReportStyle == null)) {
                String className = "calypsox.tk.report.CollateralConfigReportStyle";

                this.collateralConfigReportStyle = (calypsox.tk.report.CollateralConfigReportStyle) InstantiateUtil
                        .getInstance(className, true, true);

            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.collateralConfigReportStyle;
    }

}
