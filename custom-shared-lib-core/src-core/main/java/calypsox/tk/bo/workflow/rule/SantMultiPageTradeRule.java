package calypsox.tk.bo.workflow.rule;

import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.Vector;

public class SantMultiPageTradeRule implements WfTradeRule {

    private static final String ADDRESS_METHOD_SWIFT = "SWIFT";

    private static final String PREP_Q = "PREP";
    private static final String CONTINUATION_TAG = ":28E:";

    private static final String KEY_REVERSED = "ReversedAllocationTrade";
    public static final String KEYWORD_PREP = "PREP";
    public static final String KEYWORD_CONTINUATION = "ContinuationIndicator";
    public static final String KEYWORD_MT569_MESSAGE_ID = "MT569MessageId";
    private static final String KEY_TRIPARTY_NOT_EXPORTED = "TripartyNotExported";

    @Override
    public boolean check(
            TaskWorkflowConfig arg0,
            Trade arg1,
            Trade arg2,
            Vector arg3,
            DSConnection arg4,
            Vector arg5,
            Task arg6,
            Object arg7,
            Vector arg8) {

        return true;
    }

    @Override
    public String getDescription() {

        return "";
    }

    @Override
    public boolean update(
            TaskWorkflowConfig arg0,
            Trade trade,
            Trade arg2,
            Vector arg3,
            DSConnection dsConn,
            Vector arg5,
            Task arg6,
            Object arg7,
            Vector arg8) {

        if (trade != null
                && !Util.isEmpty(trade.getKeywordValue(KEY_REVERSED))
                && trade.getProduct() instanceof MarginCall) {
            long messageID = 0;
            int contractid = ((MarginCall) trade.getProduct()).getMarginCallId();
            messageID = loadBOMessagebyContractID(contractid);

            Log.info(this, "Retrive Swift from message: " + messageID);
            SwiftMessage swiftMsg = getSwiftMessage(messageID, dsConn);

            if (swiftMsg == null) return true;

            // Get PREP and Continuation
            String prepValue = getPrepValueFromSwift(swiftMsg, messageID);

            Log.debug(this, "prepValue: " + prepValue);
            if (prepValue == null) {
                Log.error(this, "Cannot retrive prepValue for message: " + messageID);
                return true;
            }

            String continuationValue = getContinuationValueFromSwift(swiftMsg, messageID);
            Log.debug(this, "continuationValue: " + continuationValue);

            if (continuationValue == null) {
                Log.error(this, "Cannot retrive continuationValue for message: " + messageID);
                return true;
            }

            // Set values in the trade											// 4. add PREP, ContinuationIndicator and BO Message Id
            trade.addKeyword(KEYWORD_PREP, prepValue);
            trade.addKeyword(KEYWORD_CONTINUATION, continuationValue);
            trade.addKeywordAsLong(KEYWORD_MT569_MESSAGE_ID, messageID);
            trade.addKeyword(
                    KEY_TRIPARTY_NOT_EXPORTED,
                    Boolean.toString(
                            false)); // a filter at "PENDING-->TRIPARTY_NOT_EXPORTED" transition will move the
            // trade to TRIPARTY_NOT_EXPORTED state
        }

        return true;
    }

    public int loadBOMessagebyContractID(int contractid) {
        Log.info(this, "Retriving BOMessage from ContractID: " + contractid);
        StringBuilder query = new StringBuilder();
        String isActivated = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "SantMultiPageOptimization");

        if (!Boolean.parseBoolean(isActivated)) {
            query.append("SELECT * FROM MESS_ATTRIBUTES WHERE ");
            query.append(" ATTR_NAME = 'MissingIsinContractID'");
            query.append(" AND ATTR_VALUE =" + contractid);
            query.append(" ORDER BY MESSAGE_ID DESC");
        }else {
            query.append("SELECT a.message_id ");
            query.append("FROM BO_message a ");
            query.append("JOIN mess_attributes b ON a.message_id = b.message_id ");
            query.append("WHERE a.message_type = 'INC_ALLOCATION' ");
            query.append("AND a.creation_sys_date >=  " + Util.date2SQLString(JDate.getNow().addBusinessDays(-1, Util.string2Vector("SYSTEM"))));
            query.append("AND b.attr_name = 'MissingIsinContractID' ");
            query.append("AND b.attr_value = " + contractid);
            query.append("ORDER BY a.creation_sys_date DESC ");
            query.append("FETCH FIRST 1 ROW ONLY");
        }

        try {
            Vector<?> results =
                    SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                            .executeSelectSQL(query.toString());
            if (!Util.isEmpty(results)
                    && results.size() > 2
                    && !Util.isEmpty((Vector<?>) results.get(2))) {
                Vector<?> values = (Vector<?>) results.get(2);
                return Integer.valueOf(String.valueOf(values.get(0)));
            }

        } catch (RemoteException e) {
            Log.error(this, "Cannot execute query: " + query.toString());
        } catch (NumberFormatException e) {
            Log.error(this, "Cannot cast to number");
        }

        return 0;
    }

    public SwiftMessage getSwiftMessage(long swiftMsgID, DSConnection dsConn) {

        SwiftMessage swiftMessage = new SwiftMessage();

        if (swiftMsgID != 0) {
            Log.info(this, "Loading advice document from message: " + swiftMsgID);
            Vector adviceDocuments = null;
            try {
                adviceDocuments =
                        dsConn
                                .getRemoteBackOffice()
                                .getAdviceDocuments("advice_document.advice_id=" + swiftMsgID, null, null);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Cannot load Advice Documents for Swift message id " + swiftMsgID);
            }

            if (Util.isEmpty(adviceDocuments))
                Log.warn(this, "No Advice Documents found for Swift message id " + swiftMsgID);
            else {
                for (Object docObj : adviceDocuments) {
                    AdviceDocument document = (AdviceDocument) docObj;
                    if (document != null
                            && document.getAddressMethod().equalsIgnoreCase(ADDRESS_METHOD_SWIFT)) {
                        Log.info(
                                this, "generate swiftMessage from Advice Document id: " + document.getAdviceId());

                        if (!swiftMessage.parseSwiftText(document.getDocument().toString(), false)) {
                            Log.error(this, "Can't parse raw records from Triparty Margin Detail");
                            return null;
                        }
                        return swiftMessage;
                    }
                }
                Log.warn(
                        this,
                        " Advice documents found for Swift message id "
                                + swiftMsgID
                                + ", but none of them are Swift messages");
            }
        }

        return null;
    }

    /**
     * TAGS: Several (probably: :98A:,:98C:,:98E:) Qualifier: PREP
     *
     * @param swiftMsg {@link SwiftMessage}
     * @return Returns the PREP value. Returns null if error. If the field exists but the value is
     * empty, it is considered an error, so null will be returned.
     */
    public String getPrepValueFromSwift(SwiftMessage swiftMsg, long messageID) {
        SwiftFieldMessage prepField = null;
        for (SwiftFieldMessage field : swiftMsg.getFields()) {
            if (field.getValue().indexOf(PREP_Q + "//") >= 0) {
                prepField = field;
                break;
            }
        }

        if (prepField == null) {
            Log.error(this, "Cannot get field " + PREP_Q + " from Swift message id " + messageID);
        } else {
            String[] values =
                    prepField.getValue().split("//"); // example of value:  :PREP//20100819050000
            if (values.length == 1
                    || values[1]
                    .isEmpty()) // split() discards trailing empty strings, so values.length() could be 1
                Log.error(
                        this,
                        "Field "
                                + PREP_Q
                                + " contains an empty value after '//', in Swift message with id "
                                + messageID);
            else return values[1];
        }

        return null;
    }

    /**
     * TAG (:28E:) Qualifier (1)
     *
     * @param swiftMsg {@link SwiftMessage}
     * @return Returns the Continuation value. Returns null if error. If the field exists but the
     * value is empty, it is considered an error, so null will be returned.
     */
    public String getContinuationValueFromSwift(SwiftMessage swiftMsg, long messageID) {
        SwiftFieldMessage contiField = swiftMsg.getSwiftField(CONTINUATION_TAG, null, null);

        if (contiField == null)
            Log.error(this, "Cannot get Continuation field from Swift message with id " + messageID);
        else if (!contiField.getValue().contains("/"))
            Log.error(
                    this, "Continuation field doesn't contain '/' in Swift message with id " + messageID);
        else {
            String[] values = contiField.getValue().split("/");
            if (values.length == 1 || values[1].isEmpty())
                Log.error(
                        this,
                        "Continuation field contains an empty value after '/', in Swift message with id "
                                + messageID);
            else return values[1];
        }

        return null;
    }
}
