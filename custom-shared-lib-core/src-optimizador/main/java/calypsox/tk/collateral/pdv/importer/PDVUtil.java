package calypsox.tk.collateral.pdv.importer;

import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.util.SantReportingUtil;
import calypsox.util.TradeImportStatus;
import calypsox.util.TradeImportTracker;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

public class PDVUtil implements PDVConstants {

    public static enum EnumProductType {
        SECURITY_LENDING, SECURITY_BORROWING;
    }

    private static HashMap<String, String> SL_DVP_FOP_MAPPING = new HashMap<String, String>();

    static {
        SL_DVP_FOP_MAPPING.put("DVP", "Delivery vs Payment");
        SL_DVP_FOP_MAPPING.put("FOP", "Non-DVP or Free of Payment");

    }

    private static HashMap<String, String> SB_DVP_FOP_MAPPING = new HashMap<String, String>();

    static {
        SB_DVP_FOP_MAPPING.put("DVP", "Receipt vs Payment");
        SB_DVP_FOP_MAPPING.put("FOP", "Non-RVP or Free of Payment");
    }

    private static HashMap<EnumProductType, HashMap<String, String>> PRODUCT_TYPE_DVP_FOP_MAPPING = new HashMap<EnumProductType, HashMap<String, String>>();

    static {
        PRODUCT_TYPE_DVP_FOP_MAPPING.put(EnumProductType.SECURITY_LENDING,
                SL_DVP_FOP_MAPPING);
        PRODUCT_TYPE_DVP_FOP_MAPPING.put(EnumProductType.SECURITY_BORROWING,
                SB_DVP_FOP_MAPPING);
    }

    private static final Map<String, String> myMap;

    static {
        myMap = new HashMap<String, String>();
        myMap.put("a", "b");
        myMap.put("c", "d");
    }

    public static enum EnumMessageType {
        TRADE_MESSAGE, COLLAT_MESSAGE, LIQUIDATION_MESSAGE;
    }

    // MESSAGE SEPARATOR
    private static final String MESSAGE_SEPARATOR = "\\|";

    // PDV TRADE FIELDS POSITION
    private static final int TRADE_INSTRUMENT_POSITION = 5;
    private static final int TRADE_NUM_FRONT_ID_POSITION = 2;

    // PDV COLLAT FIELDS POSITION

    private static final int COLLAT_ACTION_POSITION = 0;
    private static final int COLLAT_FO_SYSTEM_POSITION = 1;
    private static final int COLLAT_NUM_FRONT_ID_POSITION = 2;
    private static final int COLLAT_COLLAT_ID_POSITION = 3;
    private static final int COLLAT_OWNER_POSITION = 4;
    private static final int COLLAT_COUNTERPARTY_POSITION = 5;
    private static final int COLLAT_INSTRUMENT_POSITION = 6;
    private static final int COLLAT_PORTFOLIO_POSITION = 7;
    private static final int COLLAT_VALUE_DATE_POSITION = 8;
    private static final int COLLAT_TRADE_DATE_POSITION = 9;
    private static final int COLLAT_DIRECTION_POSITION = 10;
    private static final int COLLAT_AMOUNT_POSITION = 11;
    private static final int COLLAT_AMOUNT_CCY_POSITION = 12;
    private static final int COLLAT_UNDERLYING_TYPE_POSITION = 13;
    private static final int COLLAT_UNDERLYING_POSITION = 14;
    private static final int COLLAT_CLOSING_PRICE_POSITION = 15;
    private static final int SLB_BUNDLE_POSITION = 16;//or 17

    // PDV LIQUIDATION FIELDS POSITION
    private static final int LIQUIDATION_ID_MESSAGE_POSITION = 0;
    private static final int LIQUIDATION_FO_SYSTEM_POSITION = 1;
    private static final int LIQUIDATION_ID_MUREX_POSITION = 2;
    private static final int LIQUIDATION_SETTLEMENT_DATE_POSITION = 3;
    private static final int LIQUIDATION_SETTLEMENT_STATUS_POSITION = 4;
    private static final int LIQUIDATION_COMMENT_POSITION = 5;

    private static HashMap<String, Integer> TRADE_FIELD_POSITION_MAPPING = new HashMap<String, Integer>();

    static {
        TRADE_FIELD_POSITION_MAPPING.put(TRADE_INSTRUMENT_FIELD,
                TRADE_INSTRUMENT_POSITION);
        TRADE_FIELD_POSITION_MAPPING.put(TRADE_NUM_FRONT_ID_FIELD,
                TRADE_NUM_FRONT_ID_POSITION);

    }

    private static HashMap<String, Integer> COLLAT_FIELD_POSITION_MAPPING = new HashMap<String, Integer>();

    static {
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_ACTION_FIELD,
                COLLAT_ACTION_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_FO_SYSTEM_FIELD,
                COLLAT_FO_SYSTEM_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_NUM_FRONT_ID_FIELD,
                COLLAT_NUM_FRONT_ID_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_COLLAT_ID_FIELD,
                COLLAT_COLLAT_ID_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_OWNER_FIELD,
                COLLAT_OWNER_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_COUNTERPARTY_FIELD,
                COLLAT_COUNTERPARTY_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_INSTRUMENT_FIELD,
                COLLAT_INSTRUMENT_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_PORTFOLIO_FIELD,
                COLLAT_PORTFOLIO_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_VALUE_DATE_FIELD,
                COLLAT_VALUE_DATE_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_TRADE_DATE_FIELD,
                COLLAT_TRADE_DATE_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_DIRECTION_FIELD,
                COLLAT_DIRECTION_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_AMOUNT_FIELD,
                COLLAT_AMOUNT_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_AMOUNT_CCY_FIELD,
                COLLAT_AMOUNT_CCY_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_UNDERLYING_TYPE_FIELD,
                COLLAT_UNDERLYING_TYPE_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_UNDERLYING_FIELD,
                COLLAT_UNDERLYING_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(COLLAT_CLOSING_PRICE_FIELD,
                COLLAT_CLOSING_PRICE_POSITION);
        COLLAT_FIELD_POSITION_MAPPING.put(SLB_BUNDLE_FIELD,
                SLB_BUNDLE_POSITION);


    }

    private static HashMap<String, Integer> LIQUIDATION_FIELD_POSITION_MAPPING = new HashMap<String, Integer>();

    static {
        LIQUIDATION_FIELD_POSITION_MAPPING.put(LIQUIDATION_ID_MESSAGE,
                LIQUIDATION_ID_MESSAGE_POSITION);
        LIQUIDATION_FIELD_POSITION_MAPPING.put(LIQUIDATION_FO_SYSTEM,
                LIQUIDATION_FO_SYSTEM_POSITION);
        LIQUIDATION_FIELD_POSITION_MAPPING.put(LIQUIDATION_ID_MUREX,
                LIQUIDATION_ID_MUREX_POSITION);
        LIQUIDATION_FIELD_POSITION_MAPPING.put(LIQUIDATION_SETTLEMENT_DATE,
                LIQUIDATION_SETTLEMENT_DATE_POSITION);
        LIQUIDATION_FIELD_POSITION_MAPPING.put(LIQUIDATION_SETTLEMENT_STATUS,
                LIQUIDATION_SETTLEMENT_STATUS_POSITION);
        LIQUIDATION_FIELD_POSITION_MAPPING.put(LIQUIDATION_COMMENT,
                LIQUIDATION_COMMENT_POSITION);
    }

    public static List<CollateralConfig> getCollateralConfig(Trade trade) {
        List<CollateralConfig> collateralConfigs = new ArrayList<CollateralConfig>();
        ArrayList<CollateralConfig> eligibleMarginCallConfigs;
        try {
            eligibleMarginCallConfigs = SantReportingUtil
                    .getSantReportingService(DSConnection.getDefault())
                    .getEligibleMarginCallConfigs(trade);

            if (!Util.isEmpty(eligibleMarginCallConfigs)) {
                for (CollateralConfig collatConfig : eligibleMarginCallConfigs) {
                    if (collatConfig != null) {
                        collateralConfigs.add(collatConfig);
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(PDVUtil.class.getName(), e);
        }
        return collateralConfigs;
    }

    public static Map<String, String> getFieldValues(
            EnumMessageType messageType, String message, List<String> fields) {
        return getFieldValues(messageType, message, fields, MESSAGE_SEPARATOR);
    }

    /**
     * @param messageType : COLLAT_MESSAGE, LIQUIDATION_MESSAGE
     * @param message     : message to parse
     * @param fields      : fields to retrieve
     * @param separator   : fields separator
     * @return
     */
    public static Map<String, String> getFieldValues(
            EnumMessageType messageType, String message, List<String> fields,
            String separator) {
        Map<String, String> retValues = new HashMap<String, String>();
        if (!Util.isEmpty(message)) {
            String[] values = message
                    .split(Util.isEmpty(separator) ? MESSAGE_SEPARATOR
                            : separator);
            for (String fieldName : fields) {
                retValues.put(fieldName,
                        getFieldValue(messageType, values, fieldName));
            }
        }
        return retValues;
    }

    /**
     * @param messageType
     * @param values
     * @param fieldName
     * @return
     */
    private static String getFieldValue(EnumMessageType messageType,
                                        String[] values, String fieldName) {
        int pos = EnumMessageType.TRADE_MESSAGE.equals(messageType) ? TRADE_FIELD_POSITION_MAPPING
                .get(fieldName) : (EnumMessageType.COLLAT_MESSAGE
                .equals(messageType) ? COLLAT_FIELD_POSITION_MAPPING
                .get(fieldName) : LIQUIDATION_FIELD_POSITION_MAPPING
                .get(fieldName));
        if (pos >= values.length) {
            return "";
        } else {
            return values[pos];
        }
    }

    /**
     * Generate a Task
     *
     * @param comment    comment related to the task
     * @param tradeId    trade id if the exception is related to a trade
     * @param eventType  task event type
     * @param eventClass task event class
     * @return a new Task
     */
    public static Task buildTask(String comment, String sourceName,
                                 String eventType, String eventClass, long objectId, long tradeId) {
        Task task = new Task();
        task.setObjectLongId(objectId);
        task.setTradeLongId(tradeId);
        task.setEventClass(eventClass);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(eventType);
        task.setSource(sourceName);
        task.setComment(comment);
        return task;
    }

    public static Task buildTask(String internalReference, String comment,
                                 String sourceName, String eventType, String eventClass,
                                 long objectId, long linkId, long tradeId) {
        Task task = buildTask(comment, sourceName, eventType, eventClass,
                objectId, tradeId);
        task.setInternalReference(internalReference);
        task.setLinkId(linkId);
        return task;
    }

    public static Task buildTask(String comment, String sourceName,
                                 String eventType, String eventClass) {
        return buildTask(comment, sourceName, eventType, eventClass, 0, 0);
    }

    /**
     * @param tradeImportTracker
     * @return
     */
    public static Vector<Task> getTasksToPublish(
            TradeImportTracker tradeImportTracker) {
        Vector<Task> tasks = new Vector<Task>();
        if (tradeImportTracker != null) {
            for (TradeImportStatus tradeImport : tradeImportTracker
                    .getPDVProcessingErrors()) {
                tasks.add(PDVUtil.buildTask("[NUM_FRONT_ID] "
                                + tradeImport.getTradeBean().getNumFrontId() + ": "
                                + tradeImport.getErrorMessage(), "PDV_TRADE_IMPORT",
                        PDVConstants.PDV_TRADE_EXCEPTION_TYPE, "Collateral"));
            }
        }
        return tasks;
    }

    public static String getDeliveryType(EnumProductType product, String dvpFop) {
        return PRODUCT_TYPE_DVP_FOP_MAPPING.get(product).get(dvpFop);
    }

    public static String getDvpFopValue(String deliveryType) {
        return LocalCache.getDomainValueComment(DSConnection.getDefault(),
                PDVConstants.MC_ALLOC_DELIVERY_TYPE, deliveryType);
    }

    /**
     * Get EX_PDV_ALLOC_FUT tasks for given valDate
     *
     * @param valDate
     * @return
     */
    public static TaskArray getAllocFutureTasksToPublish(JDate valDate) {
        TaskArray list = null;
        StringBuffer str = new StringBuffer();
        str.append("event_type = '" + PDVConstants.PDV_ALLOC_FUT_EXCEPTION_TYPE
                + "'");
        str.append(" AND task_status = 0 ");
        str.append(" AND object_date = " + Util.date2SQLString(valDate));
        try {
            list = DSConnection.getDefault().getRemoteBO()
                    .getTasks(str.toString(), null, " task_id ASC", null);
        } catch (RemoteException e) {
            Log.error(PDVUtil.class.getName(), e);
        }
        return list;
    }

    /**
     * Generate a task from future collat
     *
     * @param msg
     * @param collatId
     * @param date
     * @return
     */
    public static Task buildTaskAllocFuture(String msg, String action,
                                            String collatId, JDate date) {
        Long collateralId = Long.valueOf(collatId);
        Task task = buildTask(msg, PDV_ALLOC_FUT_EXCEPTION_TYPE,
                PDV_ALLOC_FUT_EXCEPTION_TYPE, "Collateral", collateralId, 0);
        task.setObjectDate(date);
        task.setAttribute(action);
        return task;
    }

    /**
     * Complete tasks with same collatId and action
     *
     * @param collatId
     * @param taskId
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<Task> completeAllocFutureTasks(Long collatId,
                                                      JDate valDate, Long taskId) {
        StringBuffer where = new StringBuffer();
        where.append("event_type = '"
                + PDVConstants.PDV_ALLOC_FUT_EXCEPTION_TYPE + "'");
        where.append(" AND task_status = 0 ");
        where.append(" AND object_id = " + collatId);
        where.append(" AND object_date > " + Util.date2SQLString(valDate));
        if (taskId != null) {
            where.append(" AND task_id != " + taskId);
        }

        TaskArray tasksList = null;
        try {
            tasksList = DSConnection.getDefault().getRemoteBO()
                    .getTasks(where.toString(), null);
        } catch (RemoteException e) {
            Log.error(PDVUtil.class.getName(), "Error while getting tasks: "
                    + where.toString(), e);
        }
        List<Task> newList = new ArrayList<Task>();
        Vector<Task> vectorTasks = tasksList.toVector();
        for (Task currentTask : vectorTasks) {
            if (currentTask.getObjectDate().equals(valDate)
                    && PDVConstants.PDV_ACTION.SLA.toString().equals(
                    currentTask.getAttribute())) {
                continue;
            }
            currentTask.setStatus(Task.COMPLETED);
            newList.add(currentTask);
        }
        return newList;
    }

    /**
     * Returns true if Task is an EX_PDV_ALLOC task for alloc whose trades has
     * multiple eligible contracts
     *
     * @param task
     * @return
     */
    public static boolean isMoreThanOneEligibleContract(Task task) {
        return PDVConstants.PDV_ALLOC_EXCEPTION_TYPE
                .equals(task.getEventType())
                && !Util.isEmpty(task.getComment())
                && task.getComment().startsWith(
                PDVConstants.MORE_THAN_ONE_ELIGIBLE_CONTRACT)
                && task.getTradeLongId() > 0;
    }

    /**
     * Complete future tasks
     *
     * @param action
     * @param collatId
     * @param valDate
     * @param taskid
     * @param tasks
     */
    public static void completeAllocFutureTasks(String action, Long collatId,
                                                JDate valDate, Long taskid, List<Task> tasks) {
        // For ACCOUNT_CLOSING and CANCEL events processed, purge events
        // occurring later on
        if (PDV_ACTION.ACCLOSING.toString().equals(action)
                || PDV_ACTION.CANCEL.toString().equals(action)) {
            tasks.addAll(PDVUtil.completeAllocFutureTasks(collatId, valDate,
                    taskid));
        }
    }

    public static boolean checkExistsMarginCallTrade(
            ExternalAllocationBean allocBean) {
        TradeArray tradeArray = PDVUtil.getMarginCallTrade(
                allocBean.getAttributes().get(PDVUtil.COLLAT_FO_SYSTEM_FIELD),
                allocBean.getAttributes().get(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD),
                allocBean.getAttributes().get(PDVUtil.COLLAT_COLLAT_ID_FIELD), true);

        if (tradeArray != null && tradeArray.size() >= 1) {
            return true;
        }
        return false;
    }

    /**
     * @param boSystem   Back office system of the received trade
     * @param numFrontId Front office reference reference of the received trade
     * @param collatId   Collat reference of the received trade
     * @return the trade(s) with the given boSystem and numFrontId and collatId
     */
    public static TradeArray getMarginCallTrade(String boSystem, String numFrontId, String collatId, boolean excludeCanceled) {
        TradeArray existingTrades = null;
        try {
            existingTrades = DSConnection
                    .getDefault()
                    .getRemoteTrade()
                    .getTrades(
                            "trade, product_desc, trade_keyword kwd1, trade_keyword kwd2, trade_keyword kwd3",
                            "trade.product_id=product_desc.product_id and trade.trade_id=kwd1.trade_id"
                                    + " and trade.trade_id=kwd2.trade_id and trade.trade_id=kwd3.trade_id and "
                                    + (excludeCanceled ? " trade.trade_status<>'CANCELED' and " : " ")
                                    + "kwd1.keyword_name='" + TRADE_NUM_FRONT_ID_FIELD + "' and kwd1.keyword_value='" + numFrontId + "'"
                                    + " and kwd2.keyword_name='BO_SYSTEM' and kwd2.keyword_value='" + boSystem + "'"
                                    + " and kwd3.keyword_name='" + COLLAT_ID_TRADE_KEYWORD + "' and kwd3.keyword_value='" + collatId + "'"
                                    + " and product_desc.product_type='MarginCall'", null, null);
        } catch (RemoteException e) {
            Log.error(TradeInterfaceUtils.class, e);
            existingTrades = null;
        }
        return existingTrades;
    }
}
