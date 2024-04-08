/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallEntryFacade;
import com.calypso.tk.core.Amount;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.InstantiateUtil;
import org.jfree.util.Log;

import java.util.Vector;

public class MarginCallReportStyle extends com.calypso.tk.report.MarginCallReportStyle {

    public final static String SANT_MC_OF_OVERALLEXPOSURE = "Sant_ProportionMarginCall_Of_OverallExposure";
    protected CollateralConfigReportStyle collateralConfigReportStyle = null;

    private static final long serialVersionUID = 30462352961211451L;

    @Override
    public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors) {
        if (row == null) {
            return null;
        }

        if ("MarginCallConfig.LE Threshold Type".equalsIgnoreCase(columnName)) {
            MarginCallEntry entry = row.getProperty("MarginCallEntry");
            if (entry == null) {
                return null;
            }
            return entry.getCollateralConfig().getLeNewThresholdType();
        } else if ("MarginCallConfig.PO Threshold Type".equalsIgnoreCase(columnName)) {
            MarginCallEntry entry = row.getProperty("MarginCallEntry");
            if (entry == null) {
                return null;
            }
            return entry.getCollateralConfig().getPoNewThresholdType();
        } else if (SANT_MC_OF_OVERALLEXPOSURE.equalsIgnoreCase(columnName)) {
            MarginCallEntryFacade entry = row.getProperty("Default");
            if (entry == null) {
                return null;
            }
            double globalRequiredMargin = entry.getGlobalRequiredMargin();
            double marginRequired = entry.getMarginRequired();
            double result = globalRequiredMargin / marginRequired;
            if (Double.isNaN(result)) {
                return "";
            }

            return new Amount(result * 100, 5);
        } else {
            if (getCollateralConfigReportStyle() != null && columnName.startsWith("MarginCallConfig.")) {

                String realColumnName = getCollateralConfigReportStyle().getRealColumnName("MarginCallConfig.",
                        columnName);
                CollateralConfig config = row.getProperty("MarginCallConfig");
                if (config == null) {
                    MarginCallEntry entry = row.getProperty("MarginCallEntry");
                    CollateralConfig collateralConfig = entry.getCollateralConfig();
                    row.setProperty("MarginCallConfig", collateralConfig);
                }

                Object value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(config, columnName, getCollateralConfigReportStyle());
                if (value != null) {
                    return value;
                }
                value = getCollateralConfigReportStyle().getColumnValue(row, realColumnName, errors);
                if (value != null) {
                    return value;
                }
            }
            return super.getColumnValue(row, columnName, errors);

        }
    }


    protected CollateralConfigReportStyle getCollateralConfigReportStyle() {
        try {
            if (this.collateralConfigReportStyle == null) {
                String className = "calypsox.tk.report.CollateralConfigReportStyle";

                this.collateralConfigReportStyle = (CollateralConfigReportStyle) InstantiateUtil.getInstance(className,
                        true, true);

            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return this.collateralConfigReportStyle;
    }

}
