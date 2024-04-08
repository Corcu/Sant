package calypsox.tk.report;

import calypsox.tk.report.DatamartContractsExtractionLogic.ContractWrapper;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class DatamartContractsExtractionReportStyle extends MarginCallReportStyle {

    private static final long serialVersionUID = 123L;

    public static final String CSA_AGREEMENT = "CSA Agreement";
    public static final String MASTER_AGREEMENT = "Related Master Agreement";
    public static final String EXCHANGE_DIRECTION = "Exchange Direction";
    public static final String ELEGIBLE_COLLAT_APPLY_TO = "Elegible Collaterals Apply to";
    public static final String ELEGIBLE_COLLATERALS = "Elegible Collaterals";
    public static final String VALUATION_PERCENT = "Valuation Percentage";
    public static final String REHYPOTHETICATION = "Rehypothetication";
    public static final String PAYMENT_FREQ = "Payment Frequency";
    public static final String VALUATION_DATES = "Valuation Dates";
    public static final String TRANSF_INT_AMOUNT = "Transfer of Interest Amount";
    public static final String ELEGIBLE_CURRENCIES = "Elegible Currencies";
    public static final String INTEREST_RATE_CCY = "Interest Rate Ccy";
    public static final String INTEREST_RATE_CODE = "Interest Rate Code";
    public static final String INTEREST_RATE_SPREAD = "Interest Rate Spread";
    public static final String VAL_AGENT = "Valuation Agent";
    public static final String VAL_AGENT_ROLE = "Valuation Agent Role";
    public static final String VAL_TIME = "Valuation Time";
    public static final String NOTIF_TIME = "Notification Time";
    public static final String RESOLUTION_TIME = "Resolution Time";
    public static final String DISPUTE_METHOD = "Dispute Method";
    public static final String COLLAT_SUB = "Consent for collateral substitution";
    public static final String THRES_LANG_AVA = "Threshold Clause Language Available";
    public static final String THRES_LANG = "Threshold Clause Language";
    public static final String THRES_APPLY_TO_OWNER = "Threshold Apply to owner";
    public static final String OWNER_THRES_DEP_ON = "owner Threshold Depending on";
    public static final String OWNER_THRES_AMOUNT = "owner Threshold Amount";
    public static final String OWNER_THRES_MOODY = "owner Threshold Moody";
    public static final String OWNER_THRES_SP = "owner Threshold S&P";
    public static final String OWNER_THRES_FITCH = "owner Threshold Fitch";
    public static final String THRES_APPLY_TO_CPTY = "Threshold apply to cpty";
    public static final String CPTY_THRES_DEP_ON = "cpty Threshold depending on";
    public static final String CPTY_THRES_AMOUNT = "cpty Threshold Amount";
    public static final String CPTY_THRES_MOODY = "cpty Threshold moody";
    public static final String CPTY_THRES_SP = "cpty Threshold S&P";
    public static final String CPTY_THRES_FITCH = "cpty Threshold Fitch";
    public static final String MTA_LANG_AVA = "MTA Clause Language Available";
    public static final String MTA_LANG = "MTA Clause Language";
    public static final String MTA_APPLY_TO_OWNER = "MTA Apply to owner";
    public static final String OWNER_MTA_DEP_ON = "owner MTA Depending on";
    public static final String OWNER_MTA_AMOUNT = "owner MTA Amount";
    public static final String OWNER_MTA_MOODY = "owner MTA Moody";
    public static final String OWNER_MTA_SP = "owner MTA S&P";
    public static final String OWNER_MTA_FITCH = "owner MTA Fitch";
    public static final String MTA_APPLY_TO_CPTY = "MTA apply to cpty";
    public static final String CPTY_MTA_DEP_ON = "cpty MTA depending on";
    public static final String CPTY_MTA_AMOUNT = "cpty MTA Amount";
    public static final String CPTY_MTA_MOODY = "cpty MTA moody";
    public static final String CPTY_MTA_SP = "cpty MTA S&P";
    public static final String CPTY_MTA_FITCH = "cpty MTA Fitch";
    public static final String IA_LANG_AVA = "IA Clause Language Available";
    public static final String IA_LANG = "IA Clause Language";
    public static final String IA_APPLY_TO_OWNER = "IA Apply to owner";
    public static final String OWNER_IA_DEP_ON = "owner IA Depending on";
    public static final String OWNER_IA_AMOUNT = "owner IA Amount";
    public static final String OWNER_IA_MOODY = "owner IA Moody";
    public static final String OWNER_IA_SP = "owner IA S&P";
    public static final String OWNER_IA_FITCH = "owner IA Fitch";
    public static final String IA_APPLY_TO_CPTY = "IA apply to cpty";
    public static final String CPTY_IA_DEP_ON = "IA depending on cpty";
    public static final String CPTY_IA_AMOUNT = "cpty IA Amount";
    public static final String CPTY_IA_MOODY = "cpty IA Moody";
    public static final String CPTY_IA_SP = "cpty IA S&P";
    public static final String CPTY_IA_FITCH = "cpty IA Fitch";

    public static final String NA_VALUE = "n/a";

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        final ContractWrapper contractWrapper = row
                .getProperty(DatamartContractsExtractionReportTemplate.CONTRACT_WRAPPER);

        if (columnName.equals(CSA_AGREEMENT)) {
            return displayValue(contractWrapper.getCsaAgreement());
        } else if (columnName.equals(MASTER_AGREEMENT)) {
            return displayValue(contractWrapper.getMasterAgreement());
        } else if (columnName.equals(EXCHANGE_DIRECTION)) {
            return displayValue(contractWrapper.getExchangeDirection());
        } else if (columnName.equals(ELEGIBLE_COLLAT_APPLY_TO)) {
            return contractWrapper.getElegibleCollatApplyTo();
        } else if (columnName.equals(ELEGIBLE_COLLATERALS)) {
            return displayListOfValues(contractWrapper.getElegibleCollaterals());
        } else if (columnName.equals(VALUATION_PERCENT)) {
            return displayValue(contractWrapper.getValuationPercent());
        } else if (columnName.equals(REHYPOTHETICATION)) {
            return displayValue(contractWrapper.getRehypothecation());
        } else if (columnName.equals(PAYMENT_FREQ)) {
            return displayValue(contractWrapper.getPaymentFreq());
        } else if (columnName.equals(VALUATION_DATES)) {
            return displayValue(contractWrapper.getValuationDates());
        } else if (columnName.equals(TRANSF_INT_AMOUNT)) {
            return displayValue(contractWrapper.getTransferIntAmount());
        } else if (columnName.equals(ELEGIBLE_CURRENCIES)) {
            return displayListOfValues(contractWrapper.getElegibleCcies());
        } else if (columnName.equals(INTEREST_RATE_CCY)) {
            return displayValue(contractWrapper.getInterestRateCcy());
        } else if (columnName.equals(INTEREST_RATE_CODE)) {
            return displayValue(contractWrapper.getInterestRateCode());
        } else if (columnName.equals(INTEREST_RATE_SPREAD)) {
            return displaySpreadValue(contractWrapper.getInterestRateSpread());
        } else if (columnName.equals(VAL_AGENT)) {
            return displayValue(contractWrapper.getValuationAgent());
        } else if (columnName.equals(VAL_AGENT_ROLE)) {
            return displayValue(contractWrapper.getValuationAgentRole());
        } else if (columnName.equals(VAL_TIME)) {
            return displayValue(contractWrapper.getValuationTime());
        } else if (columnName.equals(NOTIF_TIME)) {
            return displayValue(contractWrapper.getNotificationTime());
        } else if (columnName.equals(RESOLUTION_TIME)) {
            return displayValue(contractWrapper.getResolutionTime());
        } else if (columnName.equals(DISPUTE_METHOD)) {
            return displayValue(contractWrapper.getDisputeMethod());
        } else if (columnName.equals(COLLAT_SUB)) {
            return displayValue(contractWrapper.getCollatSub());
        } else if (columnName.equals(THRES_LANG_AVA)) {
            return displayValue(contractWrapper.getThresLangAvailable());
        } else if (columnName.equals(THRES_LANG)) {
            return displayValue(contractWrapper.getThresLang());
        } else if (columnName.equals(THRES_APPLY_TO_OWNER)) {
            return contractWrapper.getThresApplyToOwner();
        } else if (columnName.equals(OWNER_THRES_DEP_ON)) {
            return displayValue(contractWrapper.getOwnerThresDependsOn());
        } else if (columnName.equals(OWNER_THRES_AMOUNT)) {
            return displayValue(contractWrapper.getOwnerThresAmount());
        } else if (columnName.equals(OWNER_THRES_MOODY)) {
            return displayValue(contractWrapper.getOwnerThresMoody());
        } else if (columnName.equals(OWNER_THRES_SP)) {
            return displayValue(contractWrapper.getOwnerThresSp());
        } else if (columnName.equals(OWNER_THRES_FITCH)) {
            return displayValue(contractWrapper.getOwnerThresFitch());
        } else if (columnName.equals(THRES_APPLY_TO_CPTY)) {
            return contractWrapper.getThresApplyToCpty();
        } else if (columnName.equals(CPTY_THRES_DEP_ON)) {
            return displayValue(contractWrapper.getCptyThresDependsOn());
        } else if (columnName.equals(CPTY_THRES_AMOUNT)) {
            return displayValue(contractWrapper.getCptyThresAmount());
        } else if (columnName.equals(CPTY_THRES_MOODY)) {
            return displayValue(contractWrapper.getCptyThresMoody());
        } else if (columnName.equals(CPTY_THRES_SP)) {
            return displayValue(contractWrapper.getCptyThresSp());
        } else if (columnName.equals(CPTY_THRES_FITCH)) {
            return displayValue(contractWrapper.getCptyThresFitch());
        } else if (columnName.equals(MTA_LANG_AVA)) {
            return displayValue(contractWrapper.getMtaLangAvailable());
        } else if (columnName.equals(MTA_LANG)) {
            return displayValue(contractWrapper.getMtaLang());
        } else if (columnName.equals(MTA_APPLY_TO_OWNER)) {
            return contractWrapper.getMtaApplyToOwner();
        } else if (columnName.equals(OWNER_MTA_DEP_ON)) {
            return displayValue(contractWrapper.getOwnerMtaDependsOn());
        } else if (columnName.equals(OWNER_MTA_AMOUNT)) {
            return displayValue(contractWrapper.getOwnerMtaAmount());
        } else if (columnName.equals(OWNER_MTA_MOODY)) {
            return displayValue(contractWrapper.getOwnerMtaMoody());
        } else if (columnName.equals(OWNER_MTA_SP)) {
            return displayValue(contractWrapper.getOwnerMtaSp());
        } else if (columnName.equals(OWNER_MTA_FITCH)) {
            return displayValue(contractWrapper.getOwnerMtaFitch());
        } else if (columnName.equals(MTA_APPLY_TO_CPTY)) {
            return contractWrapper.getMtaApplyToCpty();
        } else if (columnName.equals(CPTY_MTA_DEP_ON)) {
            return displayValue(contractWrapper.getCptyMtaDependsOn());
        } else if (columnName.equals(CPTY_MTA_AMOUNT)) {
            return displayValue(contractWrapper.getCptyMtaAmount());
        } else if (columnName.equals(CPTY_MTA_MOODY)) {
            return displayValue(contractWrapper.getCptyMtaMoody());
        } else if (columnName.equals(CPTY_MTA_SP)) {
            return displayValue(contractWrapper.getCptyMtaSp());
        } else if (columnName.equals(CPTY_MTA_FITCH)) {
            return displayValue(contractWrapper.getCptyMtaFitch());
        } else if (columnName.equals(IA_LANG_AVA)) {
            return displayValue(contractWrapper.getIaLangAvailable());
        } else if (columnName.equals(IA_LANG)) {
            return displayValue(contractWrapper.getIaLang());
        } else if (columnName.equals(IA_APPLY_TO_OWNER)) {
            return contractWrapper.getIaApplyToOwner();
        } else if (columnName.equals(OWNER_IA_DEP_ON)) {
            return displayValue(contractWrapper.getOwnerIaDependsOn());
        } else if (columnName.equals(OWNER_IA_AMOUNT)) {
            return displayValue(contractWrapper.getOwnerIaAmount());
        } else if (columnName.equals(OWNER_IA_MOODY)) {
            return displayValue(contractWrapper.getOwnerIaMoody());
        } else if (columnName.equals(OWNER_IA_SP)) {
            return displayValue(contractWrapper.getOwnerIaSp());
        } else if (columnName.equals(OWNER_IA_FITCH)) {
            return displayValue(contractWrapper.getOwnerIaFitch());
        } else if (columnName.equals(IA_APPLY_TO_CPTY)) {
            return contractWrapper.getIaApplyToCpty();
        } else if (columnName.equals(CPTY_IA_DEP_ON)) {
            return displayValue(contractWrapper.getCptyIaDependsOn());
        } else if (columnName.equals(CPTY_IA_AMOUNT)) {
            return displayValue(contractWrapper.getCptyIaAmount());
        } else if (columnName.equals(CPTY_IA_MOODY)) {
            return displayValue(contractWrapper.getCptyIaMoody());
        } else if (columnName.equals(CPTY_IA_SP)) {
            return displayValue(contractWrapper.getCptyIaSp());
        } else if (columnName.equals(CPTY_IA_FITCH)) {
            return displayValue(contractWrapper.getCptyIaFitch());
        } else {
            return super.getColumnValue(row, columnName, errors);
        }

    }

    // @Override
    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(CSA_AGREEMENT);
        treeList.add(MASTER_AGREEMENT);
        treeList.add(EXCHANGE_DIRECTION);
        treeList.add(ELEGIBLE_COLLAT_APPLY_TO);
        treeList.add(ELEGIBLE_COLLATERALS);
        treeList.add(VALUATION_PERCENT);
        treeList.add(REHYPOTHETICATION);
        treeList.add(PAYMENT_FREQ);
        treeList.add(VALUATION_DATES);
        treeList.add(TRANSF_INT_AMOUNT);
        treeList.add(ELEGIBLE_CURRENCIES);
        treeList.add(INTEREST_RATE_CCY);
        treeList.add(INTEREST_RATE_CODE);
        treeList.add(INTEREST_RATE_SPREAD);
        treeList.add(VAL_AGENT);
        treeList.add(VAL_AGENT_ROLE);
        treeList.add(VAL_TIME);
        treeList.add(NOTIF_TIME);
        treeList.add(RESOLUTION_TIME);
        treeList.add(DISPUTE_METHOD);
        treeList.add(COLLAT_SUB);

        treeList.add(THRES_LANG_AVA);
        treeList.add(THRES_LANG);
        treeList.add(THRES_APPLY_TO_OWNER);
        treeList.add(OWNER_THRES_DEP_ON);
        treeList.add(OWNER_THRES_AMOUNT);
        treeList.add(OWNER_THRES_MOODY);
        treeList.add(OWNER_THRES_SP);
        treeList.add(OWNER_THRES_FITCH);
        treeList.add(THRES_APPLY_TO_CPTY);
        treeList.add(CPTY_THRES_DEP_ON);
        treeList.add(CPTY_THRES_AMOUNT);
        treeList.add(CPTY_THRES_MOODY);
        treeList.add(CPTY_THRES_SP);
        treeList.add(CPTY_THRES_FITCH);

        treeList.add(MTA_LANG_AVA);
        treeList.add(MTA_LANG);
        treeList.add(MTA_APPLY_TO_OWNER);
        treeList.add(OWNER_MTA_DEP_ON);
        treeList.add(OWNER_MTA_AMOUNT);
        treeList.add(OWNER_MTA_MOODY);
        treeList.add(OWNER_MTA_SP);
        treeList.add(OWNER_MTA_FITCH);
        treeList.add(MTA_APPLY_TO_CPTY);
        treeList.add(CPTY_MTA_DEP_ON);
        treeList.add(CPTY_MTA_AMOUNT);
        treeList.add(CPTY_MTA_MOODY);
        treeList.add(CPTY_MTA_SP);
        treeList.add(CPTY_MTA_FITCH);

        treeList.add(IA_LANG_AVA);
        treeList.add(IA_LANG);
        treeList.add(IA_APPLY_TO_OWNER);
        treeList.add(OWNER_IA_DEP_ON);
        treeList.add(OWNER_IA_AMOUNT);
        treeList.add(OWNER_IA_MOODY);
        treeList.add(OWNER_IA_SP);
        treeList.add(OWNER_IA_FITCH);
        treeList.add(IA_APPLY_TO_CPTY);
        treeList.add(CPTY_IA_DEP_ON);
        treeList.add(CPTY_IA_AMOUNT);
        treeList.add(CPTY_IA_MOODY);
        treeList.add(CPTY_IA_SP);
        treeList.add(CPTY_IA_FITCH);

        return treeList;
    }

    private Object displayValue(String value) {
        if (Util.isEmpty(value)) {
            return NA_VALUE;
        } else {
            return value;
        }
    }

    private Object displayValue(Double value) {
        if (value == null) {
            return NA_VALUE;
        } else {
            return value;
        }
    }

    private Object displaySpreadValue(Double value) {
        if (value == null) {
            return NA_VALUE;
        } else {
            return Util.numberToSpread(value) / 100;
        }
    }

    private Object displayValue(JDatetime value) {
        if (value == null) {
            return NA_VALUE;
        } else {
            return value.toString();
        }
    }

    private Object displayListOfValues(Vector<String> listOfValues) {
        if (Util.isEmpty(listOfValues)) {
            return NA_VALUE;
        } else {
            StringBuilder strList = new StringBuilder();
            boolean isFirstValue = true;
            for (String value : listOfValues) {
                if (!isFirstValue) {
                    strList.append(",");
                    strList.append(value);
                } else {
                    strList.append(value);
                    isFirstValue = false;
                }
            }
            return strList.toString();
        }
    }

}