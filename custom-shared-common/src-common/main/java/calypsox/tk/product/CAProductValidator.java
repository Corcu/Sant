package calypsox.tk.product;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.apps.product.ShowProduct;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.LiquidationConfig;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.DefaultProductValidator;
import com.calypso.tk.product.corporateaction.CASwiftCodeDescription;
import com.calypso.tk.product.corporateaction.CASwiftEventCode;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TaskArray;
import com.hazelcast.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;
/**
 @author acd
 */
public class CAProductValidator extends DefaultProductValidator {

    public static final String DV_LIQUID_CONFIG = "CALiquidConfig";

    /** WHERE clause for not COMPLETE tasks */
    private static final StringBuilder WHERE_TASKS_NOT_COMPLETED = new StringBuilder(" AND ").append(" task_status NOT IN ('" + Task.COMPLETED + "') ");

    /** Code format */
    private static final String FORMAT_EXCEPTION_CA_AMOUNT_0 = "CaReference %s with amount 0. ";
    private static final String FORMAT_EXCEPTION_CA_AMOUNT_0_ISIN = "ISIN= %s. ";
    private static final String FORMAT_EXCEPTION_CA_AMOUNT_0_PAYMENTDATE = "CA's Payment Date: %s.";
    private static final String EXCEPTION_TYPE_CA_AMOUNT_0 = "EX_CA_AMOUNT_0";

    /** Task messages */
    private static final String COMMENT_TASK_COMPLETED = "Task completed.";


    public boolean isValidInput(Trade trade, Vector msgs) {
        /*        return trade.getProduct().isValidInput(trade, msgs);*/
        boolean out = trade.getProduct().isValidInput(trade, msgs);
        if (!out){
            for (int i = 0 ; i < msgs.size(); i++){
                if (("Amount is missing").equals(msgs.get(i))){
                    msgs.remove(msgs.get(i));
                    ExceptionItem e = new ExceptionItem();
                    e.setTradeIdException(trade.getProduct().getId());
                    e.setDescriptionException(EXCEPTION_TYPE_CA_AMOUNT_0);
                    // Generate Tasks to Task Station with type of exception
                    DSConnection ds = new DSConnection();
                    createTasks(ds, e, trade);
                }
            }
            return msgs.isEmpty() ? true : out;
        }
        return out;
    }

    public void applyDefaults(Trade trade) {
    }

    public boolean isValidInput(Product product, ShowProduct window, Vector messages) {
        addLiquidConfig(product);
        return product.isValidInput(messages);
    }

    /**
     * Set Liquidation Config by
     * @param product
     */
    public void addLiquidConfig(Product product){
        if(product instanceof CA){
            String swiftEventCode = Optional.ofNullable(((CA) product).getSwiftEventCode()).map(CASwiftEventCode::toString).orElse("");
            if(!Util.isEmpty(swiftEventCode) && isValidSwiftEventCode(swiftEventCode)){
                try {
                    LiquidationConfig liquidationConfig = DSConnection.getDefault().getRemoteReferenceData().getLiquidationConfig(getLiquidationConfigId(swiftEventCode));
                    ((CA) product).setLiquidationConfig(liquidationConfig);
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Error loading Liquidation Config: " + e);
                }
            }else {
                ((CA) product).setLiquidationConfig(null);
            }
        }
    }

    private boolean isValidSwiftEventCode(String swiftEventCode){
        Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(),DV_LIQUID_CONFIG);
        if(!Util.isEmpty(domainValues)){
            return Arrays.stream(domainValues.toArray()).map(String::valueOf).anyMatch(swiftEventCode::equalsIgnoreCase);
        }
        return false;
    }

    private int getLiquidationConfigId(String swiftEventCode){
        String liquidConfigId = LocalCache.getDomainValueComment(DSConnection.getDefault(), DV_LIQUID_CONFIG, swiftEventCode);
        try{
            return Integer.parseInt(liquidConfigId);
        }catch (Exception e){
            Log.error(this,"Error parsing id: " +e);
        }
        return -1;
    }

    /**
     * createTasks if is necessary
     *
     * @param ds
     * @param e
     */
    protected void createTasks(final DSConnection ds, ExceptionItem e, Trade trade) {

        final Long tradeId = e.getTradeIdException();
        final String descriptionException = e.getDescriptionException();
        // Check if exists active Tasks state != COMPLETED
        final TaskArray tasksNotCompleted = getActiveTasksEquityException(ds, tradeId, descriptionException, WHERE_TASKS_NOT_COMPLETED.toString());
        if (tasksNotCompleted != null && !tasksNotCompleted.isEmpty()) {
            completeAndSaveTask(ds, tasksNotCompleted, COMMENT_TASK_COMPLETED);
        }
        // Create new task exception
        createTaskException(ds, tradeId, e, trade);
    }

    /**
     * Create Task Exception.
     *
     * @param ds
     * @param tradeId
     * @param item
     */
    protected void createTaskException(final DSConnection ds, final long tradeId, final ExceptionItem item, final Trade trade) {
        if(trade==null) {
            Log.error(this, "Trade is null");
            return;
        }

        String message = String.format(FORMAT_EXCEPTION_CA_AMOUNT_0, generateCARefConci(trade))
                + String.format(FORMAT_EXCEPTION_CA_AMOUNT_0_ISIN, ((CA) trade.getProduct()).getUnderlyingProduct().getSecCode("ISIN"))
                + String.format(FORMAT_EXCEPTION_CA_AMOUNT_0_PAYMENTDATE, ((CA) trade.getProduct()).getValueDate());
        Task taskException = new Task();
        taskException.setStatus(Task.NEW);
        taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        taskException.setEventType(item.getDescriptionException());
        taskException.setComment(message);
        taskException.setTradeId(tradeId);
        taskException.setDatetime(JDate.getNow().getJDatetime());

        TaskArray task = new TaskArray();
        task.add(taskException);
        try {
            ds.getRemoteBackOffice().saveAndPublishTasks(task,0L,null);
        }
        catch (CalypsoServiceException e) {
            Log.error(this, "Could not save the exception task.");
        }

    }

    /**
     * Get tasks the trade have just created a Task previously
     *
     * @param tradeId
     * @return
     */
    protected TaskArray getActiveTasksEquityException(final DSConnection ds, final long tradeId, final String descriptionException, final String addClause) {
        TaskArray tasks = new TaskArray();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade_id = ").append(tradeId);
        whereClause.append(" AND ");
        whereClause.append("event_class = 'Exception'");
        whereClause.append(" AND ");
        whereClause.append("event_type = '").append(descriptionException).append("'");

        if (addClause != null && !addClause.isEmpty()) {
            whereClause.append(addClause);
        }

        try {
            tasks = ds.getRemoteBO().getTasks(whereClause.toString(), null);
        }
        catch (final RemoteException e) {
            Log.error(this, String.format("Error retrieving tasks from BBDD."), e);
        }

        return tasks;
    }

    /**
     * complete tasks and save it
     *
     * @param tasks
     *            task to complete
     * @param comment
     *            comment
     * @throws RemoteException
     */
    public void completeAndSaveTask(final DSConnection ds, final TaskArray tasks, final String comment) {

        if (tasks != null) {
            for (int i = 0; i < tasks.size(); i++) {
                final Task task = tasks.get(i);
                task.setOwner(ds.getUser());
                task.setCompletedDatetime(new JDatetime());
                task.setStatus(Task.COMPLETED);
                task.setUserComment(comment);
                saveTask(ds, task);
            }
        }
    }

    /**
     * Save one task
     *
     * @param task
     */
    protected void saveTask(final DSConnection ds, final Task task) {
        try {
            ds.getRemoteBO().save(task);
        } catch (final RemoteException e) {
            Log.error(this, e);
        }
    }

    private String generateCARefConci(Trade trade){
        String key = "";
        if (null != trade && trade.getProduct() instanceof CA) {
            String swiftEventCodeName = Optional.ofNullable(((CA) trade.getProduct()).getSwiftEventCode())
                    .map(CASwiftEventCode::getSwiftCodeDescription)
                    .map(CASwiftCodeDescription::toString)
                    .orElse("");
            int id = trade.getProduct().getId();
            if(!Util.isEmpty(swiftEventCodeName)){
                key= swiftEventCodeName+id;
            }
        }
        return key;
    }

    class ExceptionItem {

        long tradeIdException;
        String descriptionException;

        public ExceptionItem() {
            tradeIdException = 0;
            descriptionException = "";
        }

        public long getTradeIdException() {
            return tradeIdException;
        }

        public String getDescriptionException() {
            return descriptionException;
        }

        public void setTradeIdException(long tradeIdException) {
            this.tradeIdException = tradeIdException;
        }

        public void setDescriptionException(String descriptionException) {
            this.descriptionException = descriptionException;
        }

    }

}
