package calypsox.util;

import calypsox.ErrorCodeEnum;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Utility class for the trades integration aalonsop AAP synchronized added
 *
 * @author aela
 */
public class TradeInterfaceUtils {
    // interface fields names
    public static final String TRD_IMP_FIELD_ACTION = "ACTION";
    public static final String TRD_IMP_FIELD_FO_SYSTEM = "FO_SYSTEM";
    public static final String TRD_IMP_FIELD_NUM_FRONT_ID = "NUM_FRONT_ID";
    public static final String TRD_IMP_FIELD_OWNER = "OWNER";
    public static final String TRD_IMP_FIELD_COUNTERPARTY = "COUNTERPARTY";
    public static final String TRD_IMP_FIELD_INSTRUMENT = "INSTRUMENT";
    public static final String TRD_IMP_FIELD_PORTFOLIO = "PORTFOLIO";
    public static final String TRD_IMP_FIELD_VALUE_DATE = "VALUE_DATE";
    public static final String TRD_IMP_FIELD_TRADE_DATE = "TRADE_DATE";
    public static final String TRD_IMP_FIELD_MATURITY_DATE = "MATURITY_DATE";
    public static final String TRD_IMP_FIELD_DIRECTION = "DIRECTION";
    public static final String TRD_IMP_FIELD_NOMINAL = "NOMINAL";
    public static final String TRD_IMP_FIELD_NOMINAL_CCY = "NOMINAL_CCY";
    public static final String TRD_IMP_FIELD_CCY = "CCY";
    public static final String TRD_IMP_FIELD_MTM = "MTM";
    public static final String TRD_IMP_FIELD_MTM_CCY = "MTM_CCY";
    public static final String TRD_IMP_FIELD_MTM_DATE = "MTM_DATE";
    public static final String TRD_IMP_FIELD_BO_SYSTEM = "BO_SYSTEM";
    public static final String TRD_IMP_FIELD_BO_REFERENCE = "BO_REFERENCE";
    public static final String TRD_IMP_FIELD_UNDERLYING_TYPE = "UNDERLYING_TYPE";
    public static final String TRD_IMP_FIELD_UNDERLYING = "UNDERLYING";
    public static final String TRD_IMP_FIELD_CLOSING_PRICE = "CLOSING_PRICE";
    public static final String TRD_IMP_FIELD_STRUCTURE_ID = "STRUCTURE_ID";
    public static final String TRD_IMP_FIELD_INDEPENDENT_AMOUNT = "INDEPENDENT_AMOUNT";
    public static final String TRD_IMP_FIELD_INDEPENDENT_AMOUNT_CCY = "INDEPENDENT_AMOUNT_CCY";
    public static final String TRD_IMP_FIELD_INDEPENDENT_AMOUNT_PAY_RECEIVE = "INDEPENDENT_AMOUNT_PAY_RECEIVE";
    public static final String TRD_IMP_FIELD_CLOSING_PRICE_AT_START = "CLOSING_PRICE_AT_START";
    public static final String TRD_IMP_FIELD_NOMINAL_SEC = "NOMINAL_SEC";
    public static final String TRD_IMP_FIELD_NOMINAL_SEC_CCY = "NOMINAL_SEC_CCY";
    public static final String TRD_IMP_FIELD_HAIRCUT = "HAIRCUT";
    public static final String TRD_IMP_FIELD_HAIRCUT_DIRECTION = "HAIRCUT_DIRECTION";
    public static final String TRD_IMP_FIELD_REPO_RATE = "REPO_RATE";
    public static final String TRD_IMP_FIELD_CALL_PUT = "CALL_PUT";
    public static final String TRD_IMP_FIELD_LAST_MODIFIED = "LAST_MODIFIED";
    public static final String TRD_IMP_FIELD_TRADE_VERSION = "TRADE_VERSION";
    public static final String TRD_IMP_FIELD_CUPON_CORRIDO = "CUPON_CORRIDO";

    // PDV
    public static final String TRD_IMP_FIELD_IS_FINANCEMENT = "IS_FINANCEMENT";
    public static final String TRD_IMP_FIELD_DELIVERY_TYPE = "DELIVERY_TYPE";

    public static final String LOG_CATERGORY = "TRADE_IMPORT";

    // collateral context properties tow legs
    public static final String COL_CTX_PROP_OWNER = "OWNER";
    public static final String COL_CTX_PROP_DIRECTION_1 = "DIRECTION_1";
    public static final String COL_CTX_PROP_NOMINAL_1 = "NOMINAL_1";
    public static final String COL_CTX_PROP_CCY_1 = "CCY_1";
    public static final String COL_CTX_PROP_MTM_1 = "MTM_1";
    public static final String COL_CTX_PROP_MTM_CCY_1 = "MTM_CCY_1";
    public static final String COL_CTX_PROP_UNDERLYING_TYPE_1 = "UNDERLYING_TYPE_1";
    public static final String COL_CTX_PROP_UNDERLYING_1 = "UNDERLYING_1";
    public static final String COL_CTX_PROP_CLOSING_PRICE_1 = "CLOSING_PRICE_1";
    public static final String COL_CTX_PROP_DIRECTION_2 = "DIRECTION_2";
    public static final String COL_CTX_PROP_NOMINAL_2 = "NOMINAL_2";
    public static final String COL_CTX_PROP_CCY_2 = "CCY_2";
    public static final String COL_CTX_PROP_MTM_2 = "MTM_2";
    public static final String COL_CTX_PROP_MTM_CCY_2 = "MTM_CCY_2";
    public static final String COL_CTX_PROP_UNDERLYING_TYPE_2 = "UNDERLYING_TYPE_2";
    public static final String COL_CTX_PROP_UNDERLYING_2 = "UNDERLYING_2";
    public static final String COL_CTX_PROP_CLOSING_PRICE_2 = "CLOSING_PRICE_2";

    public static final String COL_CTX_PROP_UNDERLYING_TYPE = "UNDERLYING_TYPE";
    public static final String COL_CTX_PROP_UNDERLYING = "UNDERLYING";
    public static final String COL_CTX_PROP_CLOSING_PRICE = "CLOSING_PRICE";
    public static final String COL_CTX_PROP_NOMINAL_SEC = "NOMINAL_SEC";
    public static final String COL_CTX_PROP_NOMINAL_SEC_CCY = "NOMINAL_SEC_CCY";
    public static final String COL_CTX_PROP_HAIRCUT = "HAIRCUT";
    public static final String COL_CTX_PROP_HAIRCUT_DIRECTION = "HAIRCUT_DIRECTION";
    public static final String COL_CTX_PROP_REPO_RATE = "REPO_RATE";
    public static final String COL_CTX_PROP_CALL_PUT = "CALL_PUT";

    // trade keywords
    public static final String TRADE_KWD_BO_REFERENCE = "BO_REFERENCE";
    public static final String TRADE_KWD_BO_SYSTEM = "BO_SYSTEM";
    public static final String TRADE_KWD_FO_SYSTEM = "FO_SYSTEM";
    public static final String TRADE_KWD_STRUCTURE_ID = "STRUCTURE_ID";
    public static final String TRADE_KWD_NUM_FRONT_ID = "NUM_FRONT_ID";
    public static final String TRADE_KWD_ORIG_SOURCE_BOOK = "ORIG_SOURCE_BOOK";
    public static final String TRADE_KWD_IMPORT_REASON = "IMPORT_REASON";
    public static final String TRADE_KWD_RIG_CODE = "RIG_CODE";
    public static final String TRADE_MATURITY_DATE = "TRADE_MATURITY_DATE";
    // trade keywords for DFA and EMIR
    public static final String TRADE_KWD_USI_REFERENCE = "USI_REFERENCE";
    public static final String TRADE_KWD_STM_LCH_REFERENCE = "STM_REFERENCE";
    public static final String TRADE_KWD_SD_MSP = "SD_MSP";
    public static final String TRADE_KWD_US_PARTY = "US_PARTY";
    public static final String TRADE_KWD_DFA = "DFA_APPLICABLE";
    public static final String TRADE_KWD_FC_NFC = "FC_NFC";
    public static final String TRADE_KWD_EMIR = "EMIR_APPLICABLE";
    // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
    public static final String TRADE_KWD_UTI = "UTI_REFERENCE";
    //ACD 17/05/2016 IM
    public static final String TRADE_KWD_UPI = "UPI_REFERENCE";

    // transient trade keywords
    public static final String TRANS_TRADE_KWD_MTM = "MTM";
    public static final String TRANS_TRADE_KWD_MTM_CCY = "MTM_CCY";
    public static final String TRANS_TRADE_KWD_MTM_DATE = "MTM_DATE";
    public static final String TRANS_TRADE_KWD_MTM_IA = "MTM_IA";
    public static final String TRANS_TRADE_KWD_MTM_IA_CCY = "MTM_IA_CCY";
    public static final String TRANS_TRADE_KWD_MTM_IA_DATE = "MTM_IA_DATE";

    // trade actions
    public static final String TRADE_ACTION_NEW = "NEW";
    public static final String TRADE_ACTION_MTM = "MTM";
    public static final String TRADE_ACTION_CANCEL = "CANCEL";
    public static final String TRADE_ACTION_AMEND = "AMEND";
    public static final String TRADE_ACTION_MATURE = "MATURE";
    public static final String TRADE_ACTION_MATURITY = "MATURITY";

    private static HashMap<String, ErrorCodeMsg> fieldErrorCode = new HashMap<String, ErrorCodeMsg>();
    // private static HashMap<Integer, ErrorCodeCtrM> criticalErrorToControlM =
    // new HashMap<Integer, ErrorCodeCtrM>();
    private static HashMap<Integer, ErrorCodeEnum> criticalErrorToControlM = new HashMap<Integer, ErrorCodeEnum>();

    static {
        // init critical errors
        criticalErrorToControlM.put(1, ErrorCodeEnum.IOException);
        criticalErrorToControlM.put(2, ErrorCodeEnum.IOException);
        criticalErrorToControlM.put(3, ErrorCodeEnum.IOException);

        fieldErrorCode.put(TRADE_KWD_UPI, new ErrorCodeMsg(60, "Field UPI not present"));
        // initi fields errors codes,
        fieldErrorCode.put(TRADE_KWD_BO_SYSTEM, new ErrorCodeMsg(7, "Required field BO_SYSTEM not present"));
        fieldErrorCode.put(TRADE_KWD_BO_REFERENCE, new ErrorCodeMsg(8, "Required field BO_REFERENCE not present"));
        fieldErrorCode.put(TRD_IMP_FIELD_OWNER,
                new ErrorCodeMsg(14, "Required field PROCESSING_ORG {0} not present or not valid"));
        fieldErrorCode.put(TRD_IMP_FIELD_COUNTERPARTY,
                new ErrorCodeMsg(15, "Required field COUNTERPARTY {0} not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_INSTRUMENT,
                new ErrorCodeMsg(16, "Required field INSTRUMENT {0} not present."));
        fieldErrorCode.put(TRD_IMP_FIELD_PORTFOLIO,
                new ErrorCodeMsg(17, "Required field PORTFOLIO {0} not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_TRADE_DATE,
                new ErrorCodeMsg(18, "Required field TRADE_DATE not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_VALUE_DATE,
                new ErrorCodeMsg(19, "Required field VALUE_DATE not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_MATURITY_DATE,
                new ErrorCodeMsg(20, "Required field MATURITY_DATE not present."));
        fieldErrorCode.put(TRD_IMP_FIELD_DIRECTION,
                new ErrorCodeMsg(21, "Required field DIRECTION {0} not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_CLOSING_PRICE_AT_START,
                new ErrorCodeMsg(23, "Required field CLOSING_PRICE_AT_START not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_NOMINAL,
                new ErrorCodeMsg(24, "Required field NOMINAL not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_NOMINAL_CCY,
                new ErrorCodeMsg(25, "Required field NOMINAL_CCY {0} not present or not valid."));

        fieldErrorCode.put(COL_CTX_PROP_HAIRCUT,
                new ErrorCodeMsg(26, "Required field HAIRCUT not present or not valid."));
        fieldErrorCode.put(COL_CTX_PROP_HAIRCUT_DIRECTION,
                new ErrorCodeMsg(27, "Required field HAIRCUT_DIRECTION {0} not present or not valid."));
        // fieldErrorCode.put(COL_CTX_PROP_REPO_AMOUNT, new ErrorCodeMsg(28,
        // "Required field REPO_AMOUNT not present or not valid"));
        fieldErrorCode.put(COL_CTX_PROP_REPO_RATE,
                new ErrorCodeMsg(29, "Required field REPO_RATE not present or not valid"));

        fieldErrorCode.put(TRD_IMP_FIELD_CCY, new ErrorCodeMsg(30, "Required field CCY {0} not present or not valid"));
        fieldErrorCode.put(TRD_IMP_FIELD_MTM_DATE,
                new ErrorCodeMsg(31, "Required field MTM_DATE not present or not valid"));
        fieldErrorCode.put(TRD_IMP_FIELD_CLOSING_PRICE,
                new ErrorCodeMsg(32, "Required field CLOSING_PRICE not present or not valid"));
        // fieldErrorCode.put(TRD_IMP_FIELD_REPO_CASH_VAL , new
        // ErrorCodeMsg(33,"Required field REPO_CASH_VAL not present or not
        // valid"));
        fieldErrorCode.put(TRD_IMP_FIELD_MTM, new ErrorCodeMsg(34, "Required field MTM not present or not valid"));
        fieldErrorCode.put(TRD_IMP_FIELD_MTM_CCY,
                new ErrorCodeMsg(35, "Required field MTM_CCY {0} not present or not valid"));

        fieldErrorCode.put(TRD_IMP_FIELD_OWNER + TRD_IMP_FIELD_PORTFOLIO,
                new ErrorCodeMsg(57, "The PROCESSING_ORG {0} is not valid or is different from the book one"));
        fieldErrorCode.put(TRD_IMP_FIELD_NUM_FRONT_ID, new ErrorCodeMsg(52, "Required field NUM_FRONT_ID not present"));
        fieldErrorCode.put(TRD_IMP_FIELD_FO_SYSTEM, new ErrorCodeMsg(51, "Required field FO_SYSTEM not present"));

        fieldErrorCode.put(TRD_IMP_FIELD_INDEPENDENT_AMOUNT_CCY,
                new ErrorCodeMsg(54, "Required field INDEPENDENT_AMOUNT_CCY {0} not present or not valid."));

        // PDV
        fieldErrorCode.put(TRD_IMP_FIELD_IS_FINANCEMENT,
                new ErrorCodeMsg(58, "Required field IS_FINANCEMENT {0} not present or not valid."));
        fieldErrorCode.put(TRD_IMP_FIELD_DELIVERY_TYPE,
                new ErrorCodeMsg(59, "Required field DELIVERY_TYPE {0} not present or not valid."));

    }

    public static HashMap<String, String> interfaceTradeActionToCalypsoAction = new HashMap<String, String>();

    static {
        interfaceTradeActionToCalypsoAction.put(TRADE_ACTION_NEW, TRADE_ACTION_NEW);
        interfaceTradeActionToCalypsoAction.put(TRADE_ACTION_AMEND, TRADE_ACTION_AMEND);
        interfaceTradeActionToCalypsoAction.put(TRADE_ACTION_CANCEL, TRADE_ACTION_CANCEL);
        interfaceTradeActionToCalypsoAction.put(TRADE_ACTION_MTM, TRADE_ACTION_AMEND);
        interfaceTradeActionToCalypsoAction.put(TRADE_ACTION_MATURE, TRADE_ACTION_MATURE);
    }

    /**
     * @param errorCode
     * @return the ErrorCodeEnum to use for ControlM messages
     */
    public static ErrorCodeEnum getCtrlMErrorFromSchedTaskErrorCode(int errorCode) {
        return criticalErrorToControlM.get(errorCode);
        // return new TradeImportStatus(codeMsg.getErrorCode(),
        // codeMsg.getErrorMsg());
    }

    /**
     * @param errorCode
     * @return true if the given error code is considered as critical
     */
    public static boolean isCriticalSchedTaskErrorCode(int errorCode) {
        return criticalErrorToControlM.containsKey(errorCode);
        // return new TradeImportStatus(codeMsg.getErrorCode(),
        // codeMsg.getErrorMsg());
    }

    /**
     * @param action
     * @return the Calypso trade action to use for the incoming action
     */
    public static String mapIncomingTradeAction(String action) {
        return interfaceTradeActionToCalypsoAction.get(action);
    }

    public static TradeImportStatus getErrorForFieldName(String fieldName) {
        ErrorCodeMsg codeMsg = fieldErrorCode.get(fieldName);
        return new TradeImportStatus(codeMsg.getErrorCode(), codeMsg.getErrorMsg());
    }

    public static TradeImportStatus getErrorForFieldName(String fieldName, Object fieldValue) {
        ErrorCodeMsg codeMsg = fieldErrorCode.get(fieldName);
        String msg = codeMsg.getErrorMsg();

        if (fieldValue != null) {
            msg = MessageFormat.format(codeMsg.getErrorMsg(), new Object[]{fieldValue});
        }
        return new TradeImportStatus(codeMsg.getErrorCode(), msg);
    }

    static class ErrorCodeMsg {
        protected int errorCode;
        protected String errorMsg;

        public ErrorCodeMsg(int code, String msg) {
            this.errorCode = code;
            this.errorMsg = msg;
        }

        /**
         * @return the errorCode
         */
        public int getErrorCode() {
            return this.errorCode;
        }

        /**
         * @param errorCode the errorCode to set
         */
        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        /**
         * @return the errorMsg
         */
        public String getErrorMsg() {
            return this.errorMsg;
        }

        /**
         * @param errorMsg the errorMsg to set
         */
        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

    }

    static class ErrorCodeCtrM {
        protected ErrorCodeEnum errorCode;
        protected String errorMsg;

        public ErrorCodeCtrM(ErrorCodeEnum code, String msg) {
            this.errorCode = code;
            this.errorMsg = msg;
        }

        /**
         * @return the errorCode
         */
        public ErrorCodeEnum getErrorCode() {
            return this.errorCode;
        }

        /**
         * @param errorCode the errorCode to set
         */
        public void setErrorCode(ErrorCodeEnum errorCode) {
            this.errorCode = errorCode;
        }

        /**
         * @return the errorMsg
         */
        public String getErrorMsg() {
            return this.errorMsg;
        }

        /**
         * @param errorMsg the errorMsg to set
         */
        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

    }

    /**
     * @param boSystem    Back office system of the received trade
     * @param boReference Back office reference of the received trade
     * @return the trade(s) with the given bo_system and bo_reference
     */
    public synchronized static TradeArray getTradeByBORefAndBOSystem(String boSystem, String boReference) {
        return getTradeByBORefAndBOSystem(boSystem, boReference, true);
    }

    /**
     * @param boSystem    Back office system of the received trade
     * @param boReference Back office reference of the received trade
     * @return the trade(s) with the given bo_system and bo_reference
     * @deprecated In MIG14.4 to SLOW
     */
    public synchronized static TradeArray getTradeByBORefAndBOSystem(String boSystem, String boReference,
                                                                     boolean excludeCanceled) {
        TradeArray existingTrades = null;
        try {
            existingTrades = DSConnection.getDefault().getRemoteTrade().getTrades(
                    "trade, trade_keyword kwd1, trade_keyword kwd2",
                    "trade.trade_id=kwd1.trade_id and trade.trade_id=kwd2.trade_id and "
                            + (excludeCanceled ? " trade.trade_status<>'CANCELED' and " : " ")
                            + "kwd1.keyword_name='BO_REFERENCE' and kwd1.keyword_value='" + boReference + "'"
                            + " and kwd2.keyword_name='BO_SYSTEM' and kwd2.keyword_value='" + boSystem + "'",
                    null, null);
        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }
        return existingTrades;
    }

    /**
     * @param boReference Back office reference of the received trade
     * @return the trade(s) with the given bo_reference
     */
    public synchronized static TradeArray getTradeByBORef(String boReference) {
        TradeArray existingTrades = null;
        try {
            existingTrades = DSConnection.getDefault().getRemoteTrade()
                    .getTrades("trade, trade_keyword kwd",
                            "trade.trade_id=kwd.trade_id and  " + " trade.trade_status<>'CANCELED' and "
                                    + "kwd.keyword_name='BO_REFERENCE' and kwd.keyword_value='" + boReference + "'",
                            null, null);
        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }
        return existingTrades;
    }

    /**
     * Optimization to v14 to recover trades during CSA importation AAP ONLY FOR
     * TESTING PURPOSES AAP
     *
     * @param boSystem
     * @param boReference
     * @return set of trades for the unique bo reference + bo system
     */
    public synchronized static TradeArray getTradeV14ByBORefAndBOSystemNew(final String boSystem,
                                                                           final String boReference) {

        TradeArray existingTrades = null;
        final String BO_REFERENCE = "BO_REFERENCE";
        final String BO_SYSTEM = "BO_SYSTEM";
        try {
            // Refactor
            long[] idsBO_REFERENCE = DSConnection.getDefault().getRemoteTrade()
                    .getTradeIdsByKeywordNameAndValue(BO_REFERENCE, boReference);
            if ((idsBO_REFERENCE != null) && (idsBO_REFERENCE.length > 0)) {
                existingTrades = new TradeArray();
                for (long id : idsBO_REFERENCE) {
                    Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(id);
                    if (trade.getKeywordValue(BO_SYSTEM).equals(boSystem)
                            && !trade.getStatus().getStatus().equals(Status.CANCELED)) {
                        existingTrades.add(trade);
                    }
                }

            }
        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e.getMessage());
            Log.error(TradeInterfaceUtils.class, e); //sonar
            existingTrades = null;
        }
        return existingTrades;
    }

    /**
     * Optimization to v14 to recover trades during CSA importation Returns all
     * the Trades from a BO_SYSTEM
     *
     * @param boSystem
     * @param boReference
     * @return set of trades for the unique bo reference + bo system
     */
    public synchronized static TradeArray getTradeV14ByBOSystem(final String boSystem) {

        TradeArray existingTrades = null;
        try {
            // Gets trades by BO_SYSTEM
            StringBuffer where2 = new StringBuffer("kwd2.keyword_name='BO_SYSTEM' and kwd2.keyword_value='" + boSystem);
            existingTrades = DSConnection.getDefault().getRemoteTrade().getTrades("trade, trade_keyword kwd2",
                    where2.toString(), null, null);
        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e.getMessage());
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }
        return existingTrades;
    }

    /**
     * Optimization to v14 to recover trades during CSA importation
     *
     * @param boSystem
     * @param boReference
     * @return set of trades for the unique bo reference + bo system
     * @author GSM
     */
    public synchronized static TradeArray getTradeV14ByBORefAndBOSystem(final String boSystem,
                                                                        final String boReference) {

        TradeArray existingTrades = null;
        try {
            // Query 1:
            // SELECT DISTINCT trade.trade_id, trade.version_num FROM trade,
            // trade_keyword kwd1 WHERE trade.trade_id=kwd1.trade_id
            // and trade.trade_status<>'CANCELED' and
            // kwd1.keyword_name='BO_REFERENCE'and
            // kwd1.keyword_value='6136090.21'
            // Gets trades by BO_REFERENCE
            final String where = "trade.trade_id=kwd1.trade_id and trade.trade_status<>'CANCELED'"
                    + "and kwd1.keyword_name='BO_REFERENCE' and kwd1.keyword_value='" + boReference + "'";
            // Check if there is a method to get only the ids
            existingTrades = DSConnection.getDefault().getRemoteTrade().getTrades("trade, trade_keyword kwd1", where,
                    null, null);

            // no data in first one, we return empty
            if ((existingTrades != null) && (existingTrades.size() > 0)) {

                // query 2
                // SELECT trade_id FROM trade_keyword kwd2 WHERE
                // kwd2.keyword_name='BO_SYSTEM'
                // and kwd2.keyword_value='MDR - Madrid' and trade_id =
                // @trade_Id

                List<Long> listIds = new ArrayList<>(existingTrades.size());
                for (Trade t : existingTrades.getTrades()) {
                    listIds.add(t.getLongId());
                }
                // Gets trades by BO_SYSTEM
                StringBuffer where2 = new StringBuffer(
                        "kwd2.keyword_name='BO_SYSTEM' and kwd2.keyword_value='" + boSystem + "' and ");
                where2.append(" trade.trade_id IN (");
                where2.append(Util.collectionToString(listIds));
                where2.append(" ) ");
                existingTrades = DSConnection.getDefault().getRemoteTrade().getTrades("trade, trade_keyword kwd2",
                        where2.toString(), null, null);
            }

        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }
        return existingTrades;
    }

    /**
     * split into pieces with a max
     *
     * @param array
     * @param max
     * @return list of arrays in blocks of max
     */
    // Example use: List<Integer[]> list = TradeInterfaceUtils.splitArray(arr, MAX_NUMBER_ELEMENS);
    public static <T extends Object> List<T[]> splitArray(T[] array, int max) {

        int x = array.length / max;
        int r = (array.length % max); // remainder
        int lower = 0;
        int upper = 0;

        List<T[]> list = new ArrayList<T[]>();
        int i = 0;

        for (i = 0; i < x; i++) {

            upper += max;
            list.add(Arrays.copyOfRange(array, lower, upper));
            lower = upper;
        }

        if (r > 0) {

            list.add(Arrays.copyOfRange(array, lower, (lower + r)));
        }

        return list;
    }
}
