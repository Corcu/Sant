package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.AuditUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.FX;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class SantCheckMulticurrencyTradeRule implements WfTradeRule {
    /**
     * Audit domain name for Trade CheckMulticurrency
     */
	private static final double tolerance = 25.0;

	public static final String ACCEPT = "ACCEPT";
	public static final String PENDING_DUAL_CCY = "PENDING_DUAL_CCY";
	public static final String CANCELED = "CANCELED";

	/** Code format */
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_EFFECTIVE_AMOUNT = "Trade %s has different effective amount of the trade %s.";
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_KEYWORD_MX_GLOBAL_ID = "Trade %s with value %s, has different keyword Mx Global ID than the trade %s, with value %s.";
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_TRADE_CURRENCY = "Trade %s with value %s, has different trade currency than the trade %s, with value %s.";
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_TRADE_DATE = "Trade %s with value %s, has different trade date than the trade %s, with value %s.";
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_SETTLE_DATE = "Trade %s with value %s, has different settle date than the trade %s, with value %s.";
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_COUNTERPARTY = "Trade %s with value %s, has different counter party than the trade %s, with value %s.";
	private static final String FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_BOOK = "Trade %s with value %s, has different book than the trade %s, with value %s.";

	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_EFFECTIVE_AMOUNT = "EX_DUAL_CCY_DIFFERENT_EFFECTIVE_AMOUNT";
	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_KEYWORD_MX_GLOBAL_ID = "EX_DUAL_CCY_DIFFERENT_KEYWORD_MX_GLOBAL_ID";
	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_TRADE_CURRENCY = "EX_DUAL_CCY_DIFFERENT_TRADE_CURRENCY";
	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_TRADE_DATE = "EX_DUAL_CCY_DIFFERENT_TRADE_DATE";
	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_SETTLE_DATE = "EX_DUAL_CCY_DIFFERENT_SETTLE_DATE";
	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_COUNTERPARTY = "EX_DUAL_CCY_DIFFERENT_COUNTERPARTY";
	private static final String EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_BOOK = "EX_DUAL_CCY_DIFFERENT_BOOK";

	/** WHERE clause for not COMPLETE tasks */
	private static final StringBuilder WHERE_TASKS_NOT_COMPLETED = new StringBuilder(" AND ").append(" task_status NOT IN ('" + Task.COMPLETED + "') ");

	/** Task messages */
	private static final String COMMENT_TASK_COMPLETED = "Task completed.";

	private long tradeId = 0;
	private long tradeIdDual = 0;

    /**
     * Workflow filter, update is executed if condition is verified
     * 
     * @param wc
     *            workflow transition configuration
     * @param trade
     *            involved Trade
     * @param oldTrade
     *            involved old Trade
     * @param messages
     *            error messages to publish
     * @param ds
     *            Data Server connection
     * @param excps
     *            exceptions to publish
     * @param task
     *            generated if exists
     * @param db
     *            database connection
     * @param events
     *            PSEvent list
     * @return true if changed Trade attributes are in white list
     * @see AuditUtil
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean check(final TaskWorkflowConfig wc, Trade trade,
	    final Trade oldTrade, final Vector messages, final DSConnection ds,
	    final Vector excps, final Task task, final Object db,
	    final Vector events) {

		Log.info(this, "public class SantCheckMulticurrencyTradeRule implements WfTradeRule {\n start");
		boolean validateOperational = false;
		//Avoid full logic if is a children trade
		if(isAllocatedBond(trade)){
			return true;
		}
		if (Boolean.valueOf(trade.getKeywordValue("Dual_CCY"))){
			try {
				TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTradesByKeywordNameAndValue("Mx Global ID", customMxGlobalID(trade));
				long tradeID = DSConnection.getDefault().getRemoteAccess().allocateLongSeed("trade", 1);
				trade.setAllocatedLongSeed(tradeID);

				for (int i = 0; i < trades.size(); i++) {
					if(!CANCELED.equalsIgnoreCase(String.valueOf(trades.get(i).getStatus())))
						if (trades.get(i).getProduct() instanceof FX ? trade.getProduct() instanceof Bond ? true : false : false){
							validateOperational = validateOperational(trade, trades.get(i));
							if (validateOperational){
								workDualTrade(trades.get(i), trade, ACCEPT);
								addBondDualCCYAttributes(trade, trades.get(i));
							}
						}
						if(trades.get(i).getProduct() instanceof Bond ? trade.getProduct() instanceof FX ? true : false : false) {
							validateOperational = validateOperational(trades.get(i), trade);
							if (validateOperational){
								workDualTrade(trades.get(i), trade, ACCEPT);
							}
						}
				}
			} catch (CalypsoServiceException e) {
				throw new RuntimeException(e);
			}
		}
		Log.info(this, "SantCheckMulticurrencyTradeRule ends with: " + validateOperational);
		return validateOperational;
    }

	private boolean isAllocatedBond(Trade trade) {
		return trade.getProduct() instanceof Bond && trade.getKeywordValue("AllocatedFrom")!=null
			&& !trade.getKeywordValue("AllocatedFrom").isEmpty();
	}

    /**
     *
     * @param trade
     * @return
     */
    public static String customMxGlobalID(Trade trade) {
        String mxGlobalIdFormat = trade.getKeywordValue("Mx Global ID");
        String mxGlobalId = mxGlobalIdFormat != null ? mxGlobalIdFormat : "";
        if (StringUtils.startsWith(mxGlobalId, "TOMS") && trade.getProduct() instanceof FX){
            mxGlobalIdFormat = StringUtils.substring(mxGlobalId, 0, mxGlobalId.length() - 4);
        } else if (StringUtils.startsWith(mxGlobalId, "TOMS") && trade.getProduct() instanceof Bond){
            mxGlobalIdFormat = mxGlobalId + "SPOT";
        }
        return mxGlobalIdFormat;
    }

    /**
     * @return Workflow rule description
     */
    @Override
    public String getDescription() {
		return "-true when no amendment on Trade is made. No update on Trade is authorized by that Check Rule";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade,
	    final Trade oldTrade, final Vector messages,
	    final DSConnection dsCon, final Vector excps, final Task task,
	    final Object dbCon, final Vector events) {
		return true;
    }

	/**
	 * Validate operational multicurrency
	 *
	 * @param trade1
	 * @param trade2
	 *
	 * @return
	 */
	private boolean validateOperational(final Trade trade1, final Trade trade2) {
		boolean out = true;
		final List<ExceptionItem> result = new ArrayList<>();
		//Bond Buy
		if(1 == trade1.getProduct().getBuySell(trade1)){
			if (trade2.getQuantity() >= 0){
				if(!validateAmount(trade1, trade2, true)) {
					out = false;
					result.add(createTask(1, trade1, trade2));
				}
				if(!trade1.getTradeCurrency().equals(trade2.getNegotiatedCurrency())) {
					out = false;
					result.add(createTask(3, trade1, trade2));
				}
			} else{
				if(!validateAmount(trade1, trade2, false)) {
					out = false;
					result.add(createTask(1, trade1, trade2));
				}
				if(!trade1.getTradeCurrency().equals(trade2.getTradeCurrency())) {
					out = false;
					result.add(createTask(3, trade1, trade2));
				}
			}
		}
		//Bond Sell
		else{
			if (trade2.getQuantity() < 0){
				if(!validateAmount(trade1, trade2, true)) {
					out = false;
					result.add(createTask(1, trade1, trade2));
				}
				if(!trade1.getTradeCurrency().equals(trade2.getNegotiatedCurrency())) {
					out = false;
					result.add(createTask(3, trade1, trade2));
				}
			} else{
				if(!validateAmount(trade1, trade2, false)) {
					out = false;
					result.add(createTask(1, trade1, trade2));
				}
				if(!trade1.getTradeCurrency().equals(trade2.getTradeCurrency())) {
					out = false;
					result.add(createTask(3, trade1, trade2));
				}
			}
		}
		if(!trade1.getTradeDate().getJDate().equals(trade2.getTradeDate().getJDate())) {
			out = false;
			result.add(createTask(4, trade1, trade2));
		}
		if(!trade1.getSettleDate().equals(trade2.getSettleDate())) {
			out = false;
			result.add(createTask(5, trade1, trade2));
		}
		if(!trade1.getCounterParty().equals(trade2.getCounterParty())) {
			out = false;
			result.add(createTask(6, trade1, trade2));
		}
		if(!trade1.getBook().equals(trade2.getBook())) {
			out = false;
			result.add(createTask(7, trade1, trade2));
		}
		createTasks(DSConnection.getDefault(), result, trade1, trade2);
		return out;
	}

	/**
	 * Work in dual Trade
	 *
	 * @param trade
	 * @param action
	 */
	protected void workDualTrade(Trade trade, Trade tradeFX, String action) {
		try {
			Trade tradeClone = (Trade) trade.cloneIfImmutable();
			if (PENDING_DUAL_CCY.equalsIgnoreCase(String.valueOf(trade.getStatus()))) {
				if(trade.getProduct() instanceof Bond){
					if(!trade.getTradeCurrency().equalsIgnoreCase(tradeFX.getSettleCurrency()))
						tradeClone.setSettleCurrency(tradeFX.getSettleCurrency());
					else
						tradeClone.setSettleCurrency(tradeFX.getNegotiatedCurrency());
					CurrencyPair currencyPair = DSConnection.getDefault().getRemoteReferenceData().getCurrencyPair(tradeFX.getNegotiatedCurrency(), tradeFX.getSettleCurrency());
					if (currencyPair.getIsPairPositionRefB()) {
						tradeClone.setSplitBasePrice(tradeFX.getTradePrice());
					} else {
						tradeClone.setSplitBasePrice(1 / tradeFX.getTradePrice());
					}
				}
				tradeClone.setAction(Action.valueOf(action));
				try {
					DSConnection.getDefault().getRemoteTrade().save(tradeClone);
				} catch (CalypsoServiceException e) {
					throw new RuntimeException(e);
				}
			}
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (CalypsoServiceException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create Task
	 *
	 * @param i
	 * @param trade1
	 * @param trade2
	 * @return
	 */
	private ExceptionItem createTask(int i, Trade trade1, Trade trade2) {
		ExceptionItem e = new ExceptionItem();
		e.setTradeIdException(trade1.getLongId());
		e.setTradeIdLinkedException(trade2.getLongId());
		if (trade1.getLongId()<=0 ) {
			e.setTradeIdException(trade1.getAllocatedLongSeed());
		}
		if (trade2.getLongId()<=0 ) {
			e.setTradeIdLinkedException(trade2.getAllocatedLongSeed());
		}
		e.setDescriptionException(createDescriptionException(i));
		e.setI(i);

		return e;
	}

	/**
	 * Create Description Exception
	 *
	 * @param i
	 * @return
	 */
	private String createDescriptionException(int i) {
		String description = null;
		switch (i){
			case 1:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_EFFECTIVE_AMOUNT;
				break;
			case 2:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_KEYWORD_MX_GLOBAL_ID;
				break;
			case 3:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_TRADE_CURRENCY;
				break;
			case 4:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_TRADE_DATE;
				break;
			case 5:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_SETTLE_DATE;
				break;
			case 6:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_COUNTERPARTY;
				break;
			case 7:
				description = EXCEPTION_TYPE_DUAL_CCY_DIFFERENT_BOOK;
				break;
		}

		return description;
	}

	/**
	 * createTasks if is necessary
	 *
	 * @param ds
	 * @param listTradeException
	 */
	protected void createTasks(final DSConnection ds, final List<ExceptionItem> listTradeException, Trade trade, Trade tradeDual) {
		if (!listTradeException.isEmpty()) {
			for (int j = 0 ; j < listTradeException.size(); j++) {
				final String descriptionException = listTradeException.get(j).getDescriptionException();
				// Check if exists active Tasks state != COMPLETED
				final TaskArray tasksNotCompleted = getActiveTasksEquityException(ds, tradeId, descriptionException, WHERE_TASKS_NOT_COMPLETED.toString());
				if (tasksNotCompleted != null && !tasksNotCompleted.isEmpty()) {
					completeAndSaveTask(ds, tasksNotCompleted, COMMENT_TASK_COMPLETED);
				}
				// Create new task exception
				createTaskException(ds, trade, tradeDual, listTradeException.get(j));
			}
		}
	}

	/**
	 * Create Task Exception.
	 *
	 * @param ds
	 * @param trade
	 * @param tradeDual
	 * @param item
	 */
	protected void createTaskException(final DSConnection ds, final Trade trade, final Trade tradeDual, final ExceptionItem item) {
		if(trade==null || tradeDual==null) {
			Log.error(this, "Trade is null");
			return;
		}

		long tradeId = item.getTradeIdException();
		long tradeIdDual = item.getTradeIdLinkedException();
		String message = null;

		switch (item.getI()){
			case 1:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_EFFECTIVE_AMOUNT, tradeId, tradeIdDual);
				break;
			case 2:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_KEYWORD_MX_GLOBAL_ID, tradeId, tradeDual.getKeywordValue("Mx Global ID"), tradeIdDual, tradeDual.getKeywordValue("Mx Global ID"));
				break;
			case 3:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_TRADE_CURRENCY, tradeId, trade.getTradeCurrency(), tradeIdDual, tradeDual.getNegotiatedCurrency());
				break;
			case 4:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_TRADE_DATE, tradeId, trade.getTradeDate(), tradeIdDual, tradeDual.getTradeDate());
				break;
			case 5:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_SETTLE_DATE, tradeId, trade.getSettleDate(), tradeIdDual, tradeDual.getSettleDate());
				break;
			case 6:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_COUNTERPARTY, tradeId, trade.getCounterParty().getCode(), tradeIdDual, tradeDual.getCounterParty().getCode());
				break;
			case 7:
				message = String.format(FORMAT_EXCEPTION_DUAL_CCY_DIFFERENT_BOOK, tradeId, trade.getBook().getName(), tradeIdDual, tradeDual.getBook().getName());
				break;
		}

		Task taskException = new Task();
		taskException.setStatus(Task.NEW);
		taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
		taskException.setEventType(item.getDescriptionException());
		taskException.setComment(message);
		taskException.setTradeId(tradeId);
		taskException.setBookId(trade.getBookId());
		taskException.setPriority(Task.PRIORITY_HIGH);

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
	private void completeAndSaveTask(final DSConnection ds, final TaskArray tasks, final String comment) {

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

	/**
	 * Validate Amount
	 *
	 * @param trade1
	 * @param trade2
	 * @return
	 */
	private boolean validateAmount(final Trade trade1, final Trade trade2, final Boolean first) {
		Double secondaryLegFX = first ? Math.abs(trade2.getQuantity()) : Math.abs(trade2.getAccrual());
		return calculateAmount(trade1) - tolerance <= secondaryLegFX && calculateAmount(trade1) + tolerance >= secondaryLegFX ? true : false;
	}


	/**
	 *
	 * @param tradeBond
	 * @return
	 */
	private double calculateAmount(Trade  tradeBond) {
		final Bond bond = (Bond) tradeBond.getProduct();
		Trade finalTradeBond = tradeBond;
		Double nominal = Math.abs(Optional.ofNullable(bond).map(b -> b.computeNominal(finalTradeBond))
				.orElse(0.0D));
		Double dirtyPrice = Optional.ofNullable(tradeBond).map(Trade::getNegociatedPrice).orElse(0.0D);

		return nominal * dirtyPrice;
	}

	/**
	 *
	 * @param tradeBond
	 * @param tradeFX
	 * @throws CalypsoServiceException
	 */
	private void addBondDualCCYAttributes(Trade  tradeBond, Trade tradeFX) throws CalypsoServiceException {
		if(!tradeBond.getTradeCurrency().equalsIgnoreCase(tradeFX.getSettleCurrency()))
			tradeBond.setSettleCurrency(tradeFX.getSettleCurrency());
		else
			tradeBond.setSettleCurrency(tradeFX.getNegotiatedCurrency());
		CurrencyPair currencyPair = DSConnection.getDefault().getRemoteReferenceData().getCurrencyPair(tradeFX.getNegotiatedCurrency(), tradeFX.getSettleCurrency());
			if (currencyPair.getIsPairPositionRefB()) {
				tradeBond.setSplitBasePrice(tradeFX.getTradePrice());
			} else {
				tradeBond.setSplitBasePrice(1 / tradeFX.getTradePrice());
			}
	}

	class ExceptionItem {

		long tradeIdException;
		long tradeIdLinkedException;
		String descriptionException;
		int i;

		public ExceptionItem() {
			tradeIdException = 0;
			tradeIdLinkedException = 0;
			descriptionException = "";
			i = 0;
		}

		public long getTradeIdException() {
			return tradeIdException;
		}

		public long getTradeIdLinkedException() {
			return tradeIdLinkedException;
		}

		public String getDescriptionException() {
			return descriptionException;
		}

		public int getI() {
			return i;
		}

		public void setTradeIdException(long tradeIdException) {
			this.tradeIdException = tradeIdException;
		}

		public void setTradeIdLinkedException(long tradeIdLinkedException) {
			this.tradeIdLinkedException = tradeIdLinkedException;
		}

		public void setDescriptionException(String descriptionException) {
			this.descriptionException = descriptionException;
		}

		public void setI(int i) {
			this.i = i;
		}

	}

}
