/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @author aela
 */
public class SantCollateralConfigReportStyle extends com.calypso.tk.report.CollateralConfigReportStyle {

    private static final long serialVersionUID = 1L;
    public static final String SantLastAllocationCurrency = "Last allocation currency";
    public static final String SantProcessDate = "Process date";
    public static final String OWNER_NAME = "Owner Name";
    public static final String LE_FULL_NAME = "LE full name";
    public static final String ADDITIONAL_LE = "Additional Legal Entities";
    public static final String MTA_CPTY = "MTA Cpty";
    public static final String LE_MTA_CURRENCY = "LE MTA Currency";
    public static final String THRESHOLD_CPTY = "Threshold Cpty";
    public static final String LE_THRESHOLD_AMOUNT = "LE Threshold Amount";
    public static final String LE_THRESHOLD_CURRENCY = "LE Threshold Currency";
    public static final String LE_MTA_AMOUNT = "LE MTA Amount";
    public static final String MTA_OWNER = "MTA Owner";
    public static final String PO_MTA_CURRENCY = "PO MTA Currency";
    public static final String THRESHOLD_OWNER = "Threshold Owner";
    public static final String PO_THRESHOLD_AMOUNT = "PO Threshold Amount";
    public static final String PO_THRESHOLD_CURRENCY = "PO Threshold Currency";
    public static final String PO_MTA_AMOUNT = "PO MTA Amount";
    public static final String INITIAL_MARGIN = "Initial Margin";
    public static final String CALC_PERIOD = "Calc Period";
    public static final String ASSET_TYPE = "Asset Type";
    public static final String ONE_WAY = "One Way";
    public static final String HEAD_CLONE = "Head Clone";
    public static final String MASTER_SIGNED_DATE = "Master Signed Date";
    public static final String INDEPENDENT_AMOUNT_OWNER = "Independent Amount Owner";
    public static final String INDEPENDENT_AMOUNT_CPTY = "Independent Amount Cpty";
    public static final String ACC_INTEREST = "AccInterest";
    public static final String PO_ELIGIBLE_CCY = "PO Eligible Ccy";
    public static final String LE_ELIGIBLE_CCY = "LE Eligible Ccy";
    public static final String DV_SANT_COLLATERAL_CONFIG = "SantCollateralConfig";
    public static final String NAME = "Name";
    public static final String RATE_INDEX = "RateIndex";
    public static final String SPREAD = "Spread";
    public static final String IS_FIXED = "IsFixed";
    public static final String FIXED_RATE = "FixedRate";
    public static final String IS_FLOOR = "IsFloor";
    public static final String FLOOR_VALUE = "FloorValue";

    public static final String PREFIX_STRING = "MarginCallConfig.";
    public static final String Sant_MCC_ConcentrationRules_ID = "Sant_MCC_ConcentrationRules_ID";
    public static final String Sant_MCC_ConcentrationRules_RuleNames = "Sant_MCC_ConcentrationRules_RuleNames";
    public static final String Sant_MCC_ConcentrationRules_Descriptions = "Sant_MCC_ConcentrationRules_Descriptions";
    public static final String Sant_MCC_ConcentrationRules_NumberOfRules = "Sant_MCC_ConcentrationRules_NumberOfRules";
    public static final String Sant_MCC_Valuation_Agent_Type = "Sant_MCC_Valuation_Agent_Type";
    public static final String Sant_MCC_CreditRatingPO = "Sant_MCC_CreditRatingPO";
    public static final String Sant_MCC_CreditRatingLE = "Sant_MCC_CreditRatingLE";
    public static final String EXCLUDE_FROM_OPTIMIZER = "Exclude From Optimizer";

    public static final String MARGIN_TYPE = "Margin Type";

    public static final String CURRENCIES = "Currencies";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        if (row == null) {
            return null;
        }

        if (SantLastAllocationCurrency.equals(columnName)) {
            return row.getProperty("LastAllocationCurrency");
        } else if (SantProcessDate.equals(columnName)) {
            return row.getProperty("ProcessDate");
        } else if (OWNER_NAME.equals(columnName)) {
            return row.getProperty(OWNER_NAME);
        } else if (LE_FULL_NAME.equals(columnName)) {
            return row.getProperty(LE_FULL_NAME);
        } else if (ADDITIONAL_LE.equals(columnName)) {
            return row.getProperty(ADDITIONAL_LE);
            //   } else if (MTA_CPTY.equals(columnName)) {
            //       return row.getProperty(MTA_CPTY);
        } else if (LE_MTA_CURRENCY.equals(columnName)) {
            return row.getProperty(LE_MTA_CURRENCY);
        } else if (LE_MTA_AMOUNT.equals(columnName)) {
            return row.getProperty(LE_MTA_AMOUNT);
            //  } else if (MTA_OWNER.equals(columnName)) {
            //      return row.getProperty(MTA_OWNER);
        } else if (PO_MTA_CURRENCY.equals(columnName)) {
            return row.getProperty(PO_MTA_CURRENCY);
        } else if (PO_MTA_AMOUNT.equals(columnName)) {
            return row.getProperty(PO_MTA_AMOUNT);
        } else if (LE_THRESHOLD_AMOUNT.equals(columnName)) {
            return row.getProperty(LE_THRESHOLD_AMOUNT);
        } else if (PO_THRESHOLD_AMOUNT.equals(columnName)) {
            return row.getProperty(PO_THRESHOLD_AMOUNT);
            // } else if (THRESHOLD_OWNER.equals(columnName)) {
            //     return row.getProperty(THRESHOLD_OWNER);
            // } else if (THRESHOLD_CPTY.equals(columnName)) {
            //    return row.getProperty(THRESHOLD_CPTY);
        } else if (LE_THRESHOLD_CURRENCY.equals(columnName)) {
            return row.getProperty(LE_THRESHOLD_CURRENCY);
        } else if (PO_THRESHOLD_CURRENCY.equals(columnName)) {
            return row.getProperty(PO_THRESHOLD_CURRENCY);
        } else if (INITIAL_MARGIN.equals(columnName)) {
            return row.getProperty(INITIAL_MARGIN);
        } else if (CALC_PERIOD.equals(columnName)) {
            return row.getProperty(CALC_PERIOD);
        } else if (ASSET_TYPE.equals(columnName)) {
            return row.getProperty(ASSET_TYPE);
        } else if (ONE_WAY.equals(columnName)) {
            return row.getProperty(ONE_WAY);
        } else if (HEAD_CLONE.equals(columnName)) {
            return row.getProperty(HEAD_CLONE);
        } else if (MASTER_SIGNED_DATE.equals(columnName)) {
            return row.getProperty(MASTER_SIGNED_DATE);
        } else if (INDEPENDENT_AMOUNT_OWNER.equals(columnName)) {
            return row.getProperty(INDEPENDENT_AMOUNT_OWNER);
        } else if (INDEPENDENT_AMOUNT_CPTY.equals(columnName)) {
            return row.getProperty(INDEPENDENT_AMOUNT_CPTY);
        } else if (MARGIN_TYPE.equals(columnName)) {
            return row.getProperty(MARGIN_TYPE);
        } else if (PO_ELIGIBLE_CCY.equals(columnName)) {
            return getEligibleCcys(row, columnName);
        } else if (LE_ELIGIBLE_CCY.equals(columnName)) {
            return getEligibleCcys(row, columnName);
        } else if (columnName.contains(ACC_INTEREST)) {
            return getAccountInterestInfo(row, columnName);
        } else if (columnName.contains(CURRENCIES)) {
            return getCurrencies(row, columnName);
        } else {
            CollateralConfigReportStyle collConfigReportStyle = new CollateralConfigReportStyle();
            return collConfigReportStyle.getColumnValue(row, columnName, errors);
        }
    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(SantCollateralConfigReportTemplate.LAST_USED_CCY);
        treeList.add(MARGIN_TYPE);
        Vector<String> values = LocalCache.getDomainValues(DSConnection.getDefault(), DV_SANT_COLLATERAL_CONFIG);
        for (String value : values) {
            treeList.add(value);
        }
        return treeList;
    }

    public String getEligibleCcys(ReportRow row, String columnName) {
        StringBuilder ret = new StringBuilder();
        List<CollateralConfigCurrency> eligibleCcys = row.getProperty(columnName);
        if (eligibleCcys != null && !eligibleCcys.isEmpty()) {
            Iterator<CollateralConfigCurrency> iterator = eligibleCcys.iterator();
            while (iterator.hasNext()) {
                ret.append(iterator.next().getCurrency());
                if (iterator.hasNext()) {
                    ret.append(",");
                }
            }
        }
        return ret.toString();
    }

    public Object getAccountInterestInfo(ReportRow row, String columnName) {
        Object ret = null;
        final String[] parts = columnName.split("-");
        final String num = parts[0];
        final String valueName = parts[1];

        AccountInterestConfigRange range = row.getProperty(ACC_INTEREST + num.charAt(num.length()-1));
        if (range != null) {
            ret = getAccountInterestValue(range, valueName);
        }
        return ret;
    }

    public Object getAccountInterestValue(AccountInterestConfigRange range, String valueName) {
        Object ret = null;
        if (NAME.equals(valueName)) {
            ret = range.getAuthName();
        } else if (RATE_INDEX.equals(valueName) && range.getRateIndex() != null) {
            ret = range.getRateIndex().toString();
        } else if (SPREAD.equals(valueName)) {
            ret = range.getSpread();
        } else if (IS_FIXED.equals(valueName)) {
            ret = range.isFixed();
        } else if (FIXED_RATE.equals(valueName)) {
            ret = range.getFixedRate();
        } else if (IS_FLOOR.equals(valueName)) {
            ret = range.isFloor();
        } else if (FLOOR_VALUE.equals(valueName)) {
            ret = range.getFloor();
        }
        return ret;
    }

    private String getCurrencies (ReportRow row, String columnName){
        List currencies = row.getProperty(columnName);
        String sb = "";
        if(currencies!=null && !currencies.isEmpty()) {
            currencies = row.getProperty(columnName);
            sb = StringUtils.join(currencies, '|');
        }
        else
            sb="ANY";

        return sb;
    }
}
