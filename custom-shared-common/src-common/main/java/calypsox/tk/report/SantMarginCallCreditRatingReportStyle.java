package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Rate;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.collateral.CollateralUtil;
import com.calypso.tk.core.collateral.DisplayMarginCallCreditRatingPriority;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.report.MarginCallCreditRatingReportStyle;
import com.calypso.tk.report.ReportRow;

public class SantMarginCallCreditRatingReportStyle extends MarginCallCreditRatingReportStyle {
    public static final String NAME = "Name";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        MarginCallCreditRating mcCreditRating = row.getProperty("MarginCallCreditRatingThreshold");
        MarginCallCreditRatingConfiguration mcCreditRatingConf = row.getProperty("MarginCallCreditRatingConfiguration");
        GlobalRatingConfiguration globalRatingConf = row.getProperty("GlobalRatingConfiguration");

        if (mcCreditRating == null || mcCreditRatingConf == null || globalRatingConf == null) {
            return null;
        }

        if (columnName.equals("Name")) {
            return mcCreditRatingConf.getName();
        } else if (columnName.equals("Priority")) {
            return new DisplayMarginCallCreditRatingPriority((double)mcCreditRating.getPriority());
        } else if (columnName.equals("Threshold Amount")) {
            return this.toDisplay(mcCreditRating.getThreshold(), mcCreditRatingConf.getThresholdCurrency());
        } else if (columnName.equals("Threshold Percent")) {
            return new Rate(mcCreditRating.getThresholdPercent());
        } else if (columnName.equals("Threshold Percent Basis")) {
            return mcCreditRating.getThresholdPercentBasis();
        } else if (columnName.equals("Threshold Currency")) {
            return mcCreditRatingConf.getThresholdCurrency();
        } else if (columnName.equals("Threshold Type")) {
            return mcCreditRating.getThresholdType();
        } else if (columnName.equals("Minimum Transfer Amount")) {
            return this.toDisplay(mcCreditRating.getMta(), mcCreditRatingConf.getMtaCurrency());
        } else if (columnName.equals("MTA Percent")) {
            return new Rate(mcCreditRating.getMtaPercent());
        } else if (columnName.equals("MTA Percent Basis")) {
            return mcCreditRating.getMtaPercentBasis();
        } else if (columnName.equals("MTA Currency")) {
            return mcCreditRatingConf.getMtaCurrency();
        } else if (columnName.equals("MTA Type")) {
            return mcCreditRating.getMtaType();
        } else if (columnName.equals("Independent Amount")) {
            return this.toDisplay(mcCreditRating.getIndependentAmount(), mcCreditRatingConf.getIaCurrency());
        } else if (columnName.equals("IA Percent")) {
            return new Rate(mcCreditRating.getIaPercent());
        } else if (columnName.equals("IA Percent Basis")) {
            return mcCreditRating.getIaPercentBasis();
        } else if (columnName.equals("IA Currency")) {
            return mcCreditRatingConf.getIaCurrency();
        } else if (columnName.equals("IA Type")) {
            return mcCreditRating.getIaType();
        } else if (columnName.equals("Date")) {
            return mcCreditRating.getAsOfDate();
        } else if (columnName.contains(".")) {
            int pointLocation = columnName.lastIndexOf(".");
            if (pointLocation >= 0) {
                String agency = columnName.substring(0, pointLocation);
                String seniority = columnName.substring(pointLocation + 1);
                return globalRatingConf.getRatingValue(mcCreditRatingConf.getRatingType(), agency, seniority, mcCreditRating.getPriority());
            }
        } else if (mcCreditRating.getPriority() < 0) {
            return "N/A";
        }

        return super.getColumnValue(row, columnName, errors);
    }


    private String toDisplay(String input, String ccy) {
        return !Util.isEmpty(input) && !"INFINITY".equalsIgnoreCase(input) ? (new Amount(CollateralUtil.repairNumber(input), ccy)).toString() : input;
    }
}
