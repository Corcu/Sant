package calypsox.tk.report;

import com.calypso.tk.core.FieldModification;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallDetailEntryReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ExportSantEmirCLMReportStyle extends MarginCallDetailEntryReportStyle {

    /**
     * Considered CANCEL status.
     */
    private static final String CANCEL_STATUS = "CANCELED TERMINATED MATURED CHECKED";

    public static final String COMMENT = "Comment";
    public static final String VERSION = "Version";
    public static final String MESSAGE_TYPE = "Message Type";
    public static final String DATA_SUBMITTER_MESSAGE_ID = "Data Submitter Message ID";
    public static final String ACTION = "Action";
    public static final String DATA_SUBMITTER_PRFIX = "Data Submitter Prefix";
    public static final String DATA_SUBMITTER_VALUE = "Data Submitter Value";
    public static final String TRADE_PARTY_PRFIX = "Trade Party Prefix";
    public static final String TRADE_PARTY_VALUE = "Trade Party Value";

    public static final String DATA_SUBMITTER_PRFIXnew = "Data Submitter Prefix new";
    public static final String DATA_SUBMITTER_VALUEnew = "Data Submitter Value new";
    public static final String TRADE_PARTY_PRFIXnew = "Trade Party Prefix new";
    public static final String TRADE_PARTY_VALUEnew = "Trade Party Value new";

    public static final String EXECUTION_AGENT_PARTY_PRFIX = "Execution Agent Party Prefix";
    public static final String EXECUTION_AGENT_PARTY_VALUE = "Execution Agent Party Value";
    public static final String UTI_PRFIX = "UTI Prefix";
    public static final String UTI_VALUE = "UTI Value";
    public static final String USI_PRFIX = "USI Prefix";
    public static final String USI_VALUE = "USI Value";
    public static final String TRADE_PARTY_TRANSACTION_ID = "Trade Party Transaction Id";
    public static final String COLLATERAL_PORTFOLIO_CODE = "Collateral portfolio code";
    public static final String COLLATERALIZED = "Collateralized";
    public static final String SENDTO = "sendTo";
    public static final String PARTY_1_REPORTING_OBLIGATION = "Party 1 Reporting Obligation";
    public static final String ACTIVITY = "Activity";

    public static List<String> DEFAULT_COLUMNS = Arrays.asList(COMMENT,
            VERSION, MESSAGE_TYPE, DATA_SUBMITTER_MESSAGE_ID, ACTION,
            DATA_SUBMITTER_PRFIX, DATA_SUBMITTER_VALUE, TRADE_PARTY_PRFIX,
            TRADE_PARTY_VALUE, EXECUTION_AGENT_PARTY_PRFIX,
            EXECUTION_AGENT_PARTY_VALUE, UTI_PRFIX, UTI_VALUE, USI_PRFIX,
            USI_VALUE, TRADE_PARTY_TRANSACTION_ID, COLLATERAL_PORTFOLIO_CODE,
            COLLATERALIZED, SENDTO, PARTY_1_REPORTING_OBLIGATION, ACTIVITY,
            DATA_SUBMITTER_PRFIXnew, DATA_SUBMITTER_VALUEnew,
            TRADE_PARTY_PRFIXnew, TRADE_PARTY_VALUEnew);

    /**
     *
     */
    private static final long serialVersionUID = 4520253382654395619L;

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnId,
                                 final Vector errors) {

        if (row == null) {
            return null;
        }

        if (!(row.getProperty("Default") instanceof SantEmirCLM)) {
            return null;
        }

        @SuppressWarnings("unused")
        SantEmirCLM santEMIRCLM = (SantEmirCLM) row.getProperty("Default");

        @SuppressWarnings("unused")
        JDatetime valDatetime = (JDatetime) row.getProperty("ValuationDatetime");

        SantEmirCLMReportItem item = (SantEmirCLMReportItem) row.getProperty("SantEmirCLMReportItem"); //createItem(santEMIRCLM.getTrade(), santEMIRCLM.getMarginCallDetailEntyDTO(), CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), santEMIRCLM.getMarginCallDetailEntyDTO().getMarginCallConfigId()), valDatetime);

        if (columnId.equalsIgnoreCase(COMMENT)) {
            return item.getFieldValue(SantEmirCLMColumns.COMMENT.toString());
        }
        if (columnId.equalsIgnoreCase(VERSION)) {
            return item.getFieldValue(SantEmirCLMColumns.VERSION.toString());
        }
        if (columnId.equalsIgnoreCase(MESSAGE_TYPE)) {
            return item.getFieldValue(SantEmirCLMColumns.MESSAGETYPE.toString());
        }
        if (columnId.equalsIgnoreCase(DATA_SUBMITTER_MESSAGE_ID)) {
            return item.getFieldValue(SantEmirCLMColumns.MESSAGEID.toString());
        }
        if (columnId.equalsIgnoreCase(ACTION)) {
            if (row.getProperty("StatusChange") != null) {
                FieldModification fm = (FieldModification) row.getProperty("StatusChange");
                if ((CANCEL_STATUS.contains(fm.getNewValue())) && "VERIFIED".equals(fm.getOldValue())) {
                    return "Cancel";
                }
            } else if (row.getProperty("ContratChange") != null) {
                FieldModification fm = (FieldModification) row.getProperty("ContratChange");
                if (!fm.getNewValue().equals(fm.getOldValue())) {
                    return "Cancel";
                }
            }
            return item.getFieldValue(SantEmirCLMColumns.ACTION.toString());
        }
        //OLD
        if (columnId.equalsIgnoreCase(DATA_SUBMITTER_PRFIX)) {
            return item.getFieldValue(SantEmirCLMColumns.LEIPREFIX.toString());
        }
        if (columnId.equalsIgnoreCase(DATA_SUBMITTER_VALUE)) {
            return item.getFieldValue(SantEmirCLMColumns.LEIVALUE.toString());
        }
        if (columnId.equalsIgnoreCase(TRADE_PARTY_PRFIX)) {
            return item.getFieldValue(SantEmirCLMColumns.TRADEPARTYPREF1.toString());
        }
        if (columnId.equalsIgnoreCase(TRADE_PARTY_VALUE)) {
            return item.getFieldValue(SantEmirCLMColumns.TRADEPARTYVAL1.toString());
        }
        //NEW
        if (columnId.equalsIgnoreCase(DATA_SUBMITTER_PRFIXnew)) {
            return item.getFieldValue(SantEmirCLMColumns.LEIPREFIXnew.toString());
        }
        if (columnId.equalsIgnoreCase(DATA_SUBMITTER_VALUEnew)) {
            return item.getFieldValue(SantEmirCLMColumns.LEIVALUEnew.toString());
        }
        if (columnId.equalsIgnoreCase(TRADE_PARTY_PRFIXnew)) {
            return item.getFieldValue(SantEmirCLMColumns.TRADEPARTYPREF1new.toString());
        }
        if (columnId.equalsIgnoreCase(TRADE_PARTY_VALUEnew)) {
            return item.getFieldValue(SantEmirCLMColumns.TRADEPARTYVAL1new.toString());
        }
        //------

        if (columnId.equalsIgnoreCase(EXECUTION_AGENT_PARTY_PRFIX)) {
            return item.getFieldValue(SantEmirCLMColumns.EXECUTIONAGENTPARTY1PREFIX.toString());
        }
        if (columnId.equalsIgnoreCase(EXECUTION_AGENT_PARTY_VALUE)) {
            return item.getFieldValue(SantEmirCLMColumns.EXECUTIONAGENTPARTYVALUE1.toString());
        }
        if (columnId.equalsIgnoreCase(UTI_PRFIX)) {
            return item.getFieldValue(SantEmirCLMColumns.UTIPREFIX.toString());
        }
        if (columnId.equalsIgnoreCase(UTI_VALUE)) {
            return item.getFieldValue(SantEmirCLMColumns.UTI.toString());
        }
        if (columnId.equalsIgnoreCase(USI_PRFIX)) {
            return item.getFieldValue(SantEmirCLMColumns.USIPREFIX.toString());
        }
        if (columnId.equalsIgnoreCase(USI_VALUE)) {
            return item.getFieldValue(SantEmirCLMColumns.USIVALUE.toString());
        }
        if (columnId.equalsIgnoreCase(TRADE_PARTY_TRANSACTION_ID)) {
            String id = (String) item.getFieldValue(SantEmirCLMColumns.TRADEPARTYTRANSACTIONID1.toString());
            if (id.contains("-")) {
                id = id.replace("-", "");
            }
            return id;
        }
        if (columnId.equalsIgnoreCase(COLLATERAL_PORTFOLIO_CODE)) {
            if (row.getProperty("ContratChange") != null) {
                FieldModification fm = (FieldModification) row.getProperty("ContratChange");
                if (!fm.getNewValue().equals(fm.getOldValue())) {
                    return fm.getOldValue();
                }
            }
            return item.getFieldValue(SantEmirCLMColumns.COLLATERALPORTFOLIOCODE.toString());
        }
        if (columnId.equalsIgnoreCase(COLLATERALIZED)) {
            return item.getFieldValue(SantEmirCLMColumns.COLLATERALIZED.toString());
        }
        if (columnId.equalsIgnoreCase(SENDTO)) {
            return item.getFieldValue(SantEmirCLMColumns.SENDTO.toString());
        }
        if (columnId.equalsIgnoreCase(PARTY_1_REPORTING_OBLIGATION)) {
            return item.getFieldValue(SantEmirCLMColumns.PARTYREPOBLIGATION1.toString());
        }
//		if (columnId.equalsIgnoreCase(MarginCallDetailEntryReportStyle.TRADE_ID)) {
//			return santEMIRCLM.getTrade() != null ? santEMIRCLM.getTrade().getLongId() : "";
//		}

        if (columnId.equalsIgnoreCase(ACTIVITY)) {
            if (row.getProperty("StatusChange") != null) {
                FieldModification fm = (FieldModification) row.getProperty("StatusChange");
                if ((CANCEL_STATUS.contains(fm.getNewValue())) && "VERIFIED".equals(fm.getOldValue())) {
                    return "CAN";
                }
            } else if (row.getProperty("ContratChange") != null) {
                FieldModification fm = (FieldModification) row.getProperty("ContratChange");
                if (!fm.getNewValue().equals(fm.getOldValue())) {
                    return "CAN";
                }
            }
            return item.getFieldValue(SantEmirCLMColumns.ACTIVITY.toString());
        }
        return "";
        //row.setProperty("Default", santEMIRCLM.getMarginCallDetailEntyDTO());
        //return super.getColumnValue(row, columnId, errors);
    }


    @Override
    public String[] getDefaultColumns() {
        return Util.collection2StringArray(DEFAULT_COLUMNS);
    }
}
