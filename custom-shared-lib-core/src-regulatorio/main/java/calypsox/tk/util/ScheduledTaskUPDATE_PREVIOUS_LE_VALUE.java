package calypsox.tk.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.DataMigrationException;
import com.calypso.tk.core.InvalidClassException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

/**
 * This scheduled task is in charge of reading the audit report and storing the
 * old legal entity attribute into a specific trade keyword amongst every trade
 * that has the entity as a counterparty.<br>
 * <br>
 * The ST was generalized to be able to select any origin legal entity attribute
 * and any destination trade keyword.<br>
 * <br>
 * As a side note, the first time a legal entity attribute is informed, it has
 * no previous value, so we must store a default value; for this there are two
 * options:<br>
 * - <b>DEFAULT_VALUE_IS_METHOD</b> is false: <b>DEFAULT_VALUE</b> stores a
 * static default value <br>
 * - <b>DEFAULT_VALUE_IS_METHOD</b> is true: <b>DEFAULT_VALUE</b> stores a
 * method name, which is invoked by reflection and its return value stored in a
 * string (ideal for get methods)<br>
 * <br>
 * 
 * @author vfedorut
 * @version 1.1.1
 */
public class ScheduledTaskUPDATE_PREVIOUS_LE_VALUE extends ScheduledTask {

	/** The Constant serialVersionUID. */
	static final long serialVersionUID = -1L;

	/** The legal entity attribute to scan for. */
	public static final String LEGAL_ENTITY_ATTRIBUTE = "LEGAL_ENTITY_ATTRIBUTE";

	/**
	 * The target keyword on which the previous value of the legal entity
	 * attribute will be stored.
	 */
	public static final String TARGET_KEYWORD = "TARGET_KEYWORD";

	/**
	 * If the defaultValue is a method of legal entity class (true), ores a
	 * static value (false).
	 */
	public static final String DEFAULT_VALUE_IS_METHOD = "DEFAULT_VALUE_IS_METHOD";

	/**
	 * Default value to store in case the legal entity attribute is new, the
	 * previous value of the keyword would be this value.
	 */
	public static final String DEFAULT_VALUE = "DEFAULT_VALUE";

	/** String to store action to apply to trades. */
	public static final String ACTION = "ACTION";

	/**
	 * The number of times to exclude a trade from the filter on error and retry
	 * to apply the ACTION.
	 */
	//public static final String NUMBER_OF_TIMES_TO_RETRY = "NUMBER_OF_TIMES_TO_RETRY";

	/** Log category to print traces in logs */
	public static final String LOGCAT = "ScheduledTask";

	/** Number of execution threads */
	public static final int NUM_THREAD = 4;

	/**
	 * Process.
	 *
	 * @param dsConnection
	 *            the ds connection
	 * @param psConnection
	 *            the ps connection
	 * @return true, if successful
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.calypso.tk.util.ScheduledTask#process(com.calypso.tk.service.DSConnection
	 * , com.calypso.tk.event.PSConnection)
	 */
	@Override
	public boolean process(DSConnection dsConnection, PSConnection psConnection) {
		boolean ret = true;

		if (this._publishB || this._sendEmailB) {
			ret = super.process(dsConnection, psConnection);
		}

		TaskArray tasks = new TaskArray();
		Task task = new Task();
		task.setObjectLongId((long) this.getId());
		task.setEventClass("Exception");
		task.setNewDatetime(this.getDatetime());
		task.setUnderProcessingDatetime(this.getDatetime());
		task.setUndoTradeDatetime(this.getDatetime());
		task.setDatetime(this.getDatetime());
		task.setPriority(1);
		task.setId(0L);
		task.setStatus(0);
		task.setEventType("EX_INFORMATION");
		task.setSource(this.getType());
		task.setComment(this.toString());
		tasks.add(task);

		boolean error = false;
		if (this._executeB) {
			String e = null;
			try {
				e = this.storeLEAtrribute(dsConnection, psConnection);
			} catch (DataMigrationException | CalypsoServiceException
					| InvalidClassException e1) {
				Log.error(LOGCAT, e1.getMessage());
			}

			if (e == null) {
				task.setCompletedDatetime(new JDatetime());
				task.setStatus(2);
			} else {
				task.setEventType("EX_EXCEPTION");
				task.setComment(e);
				error = true;
			}
		}

		try {
			getReadWriteDS(dsConnection).getRemoteBO().saveAndPublishTasks(
					tasks, 0, (String) null);
		} catch (Exception exception) {
			Log.error(this, exception);
		}

		return !error && ret;
	}

	/**
	 * Gets the start time.
	 *
	 * @param jdate
	 *            the jdate
	 * @return the start time
	 */
	@SuppressWarnings("deprecation")
	private static JDatetime getStartTime(JDatetime jdate) {
		return new JDatetime(jdate.getJDate(), 0, 0, 0, TimeZone.getDefault());
	}

	/**
	 * Gets the end time.
	 *
	 * @param jdate
	 *            the jdate
	 * @return the end time
	 */
	@SuppressWarnings("deprecation")
	private static JDatetime getEndTime(JDatetime jdate) {
		return new JDatetime(jdate.getJDate(), 23, 59, 59, TimeZone.getDefault());
	}

	/**
	 * Stores the old legal entity attribute into a specific trade keyword
	 * amongst every trade that has the entity as a counterparty. <br>
	 * .
	 *
	 * @param dsConnection
	 *            the dataserver connection
	 * @param psConnection
	 *            the PS connection
	 * @return the string
	 * @throws DataMigrationException
	 *             the data migration exception
	 * @throws CalypsoServiceException
	 *             the calypso service exception
	 * @throws InvalidClassException
	 *             the invalid class exception
	 */
	@SuppressWarnings("unchecked")
	protected String storeLEAtrribute(DSConnection dsConnection,
			PSConnection psConnection) throws DataMigrationException,
			CalypsoServiceException, InvalidClassException {

		String errorMessage = null;

		String leAttribute = this.getAttribute(LEGAL_ENTITY_ATTRIBUTE);

		String targetKeyword = this.getAttribute(TARGET_KEYWORD);

		boolean defaultIsMethod = Boolean.valueOf(this
				.getAttribute(DEFAULT_VALUE_IS_METHOD));

		String defaultValue = this.getAttribute(DEFAULT_VALUE);

		TradeFilter tradeFilter = dsConnection.getRemoteReferenceData()
				.getTradeFilter(this.getTradeFilter());

		String action = this.getAttribute(ACTION);

		/*
		 * int retryTimes = Integer.parseInt(this
		 * .getAttribute(NUMBER_OF_TIMES_TO_RETRY));
		 */

		// Map to store the list of legal entities which trades need the keyword
		// modification
		HashMap<Integer, String> leChanged = new HashMap<Integer, String>();

		// Filter out the audit report
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("entity_class_name = 'LegalEntityAttribute'");
		whereClause.append(" AND modif_date>="
				+ Util.datetime2SQLString(getStartTime(getValuationDatetime())));
		whereClause.append(" AND modif_date<="
				+ Util.datetime2SQLString(getEndTime(getValuationDatetime())));
		whereClause
				.append(" AND entity_field_name in ('_attributeValue','_CREATE_')");
		whereClause.append(" AND entity_name = '" + leAttribute + "'");

		// Get the list of the audit report changes based on the where condition
		// above
		Vector<AuditValue> auditReport = dsConnection.getRemoteTrade()
				.getAudit(whereClause.toString(), "modif_date DESC", null);

		// For each entry of the audit report
		for (AuditValue auditEntry : auditReport) {
			Log.system(LOGCAT, "Evaluating entity_id: " + auditEntry.getEntityId());

			// Get the specific legal entity attribute object
			@SuppressWarnings("deprecation")
			Vector<LegalEntityAttribute> leAttrList = dsConnection
					.getRemoteReferenceData().getLegalEntityAttributes(
							"LE_ATTRIBUTE_ID = " + auditEntry.getEntityId());
			if (leAttrList.size() > 0) {
				if (!leChanged.containsKey(leAttrList.firstElement()
						.getLegalEntityId())) {

					// Retrieve the legal entity which the attribute belongs to
					LegalEntity le = dsConnection.getRemoteReferenceData()
							.getLegalEntity(
									leAttrList.firstElement()
											.getLegalEntityId());

					if (auditEntry.getFieldName().equalsIgnoreCase(
							"_attributeValue")) {

						Log.info(LOGCAT,
								"The value to propagate for LEI _attributeValue is "
										+ auditEntry.getField().getOldValue());

						// Store the old value to propagate it into each trade's
						// keyword
						leChanged.put(leAttrList.firstElement()
								.getLegalEntityId(), auditEntry.getField()
								.getOldValue());

						// The field must be _CREATE_ then
					} else {
						// The value to store in the map is defaultValue by
						// default
						String valueToStore = defaultValue;

						// If the defaultValue stores the name of a method which
						// returns a string based in the legal entity
						if (defaultIsMethod) {
							java.lang.reflect.Method method = null;
							try {
								// Retrieve the method given by defaultValue
								method = le.getClass().getMethod(defaultValue);
							} catch (SecurityException exception) {
								Log.error(LOGCAT, exception.getMessage());
							} catch (NoSuchMethodException exception) {
								Log.error(LOGCAT, exception.getMessage());
							}

							try {
								// Execute the method given by defaultValue and
								// store the return value
								valueToStore = (String) method.invoke(le);
							} catch (IllegalArgumentException exception) {
								Log.error(LOGCAT, exception.getMessage());
							} catch (IllegalAccessException exception) {
								Log.error(LOGCAT, exception.getMessage());
							} catch (InvocationTargetException exception) {
								Log.error(LOGCAT, exception.getMessage());
							}
						}

						Log.info(LOGCAT,
								"The value to propagate for LEI _CREATE_ is : "
										+ valueToStore);
						leChanged.put(leAttrList.firstElement()
								.getLegalEntityId(), valueToStore);
					}
				}
			}

		}

		if (!Util.isEmpty(leChanged)) {
			Log.system(LOGCAT, "Changed legal entities attributes: "
					+ leChanged.keySet().toString());
		}
		
		// For loop to iterate over each legal entity
		for (Integer leId : leChanged.keySet()) {

			Log.system(
					LOGCAT,
					"Changing trades associated with the LE " + leId
							+ " and the LEI keyword to store is : "
							+ leChanged.get(leId));

			// Reset the restriction
			tradeFilter.removeCriterion("CounterPartyId");
			
			// Restrict the trade filter to filter by counter party id as well
			tradeFilter.addCriterion("CounterPartyId", leId.toString());

			// Get all trades given the trade filter
			TradeArray tradesToUpdate = dsConnection.getRemoteTrade()
					.getTrades(tradeFilter, null);

			List<Trade> tradeList = tradesToUpdate.asList();

			// For each trade
			for (int i = 0; i < tradeList.size(); i++) {
				boolean exists = false;
				Trade t = tradeList.get(i);
				String currentValue = t.getKeywordValue(targetKeyword);
				if ((currentValue != null)) {
					if (currentValue.equals(leChanged.get(leId))) {
						Log.info(LOGCAT, "Trade with id " + t.getLongId()
								+ " already has the keyword [" + targetKeyword
								+ "] with value [" + currentValue
								+ "], removing from list of trades to update");
						tradeList.remove(i);
						i--;
						exists = true;
					}
				}
				if (!exists) {
					t.addKeyword(targetKeyword, leChanged.get(leId));
					t.setAction(Action.valueOf(action));
				}

			}

			Log.system(LOGCAT,
					"The total size of trades to save for LE "+leId+" is : " + tradeList.size());
			if (tradeList.size() > 0) {

				Log.system(LOGCAT, "Starting saving operation for LE "+leId+"...");

				final ConcurrentLinkedQueue<Trade> listTrades = new ConcurrentLinkedQueue<Trade>();

				listTrades.addAll(tradeList);

				// Runnable object for the thread execution
				final Runnable runnableSave = new Runnable() {
					@Override
					public void run() {

						Trade trade = null;
						Log.info(LOGCAT, "Starting saving thread " + Thread.currentThread().getId());
						while (null != (trade = listTrades.poll())) {

							try {
								dsConnection.getRemoteTrade().save(trade);

							} catch (final Exception exception) {
								Log.error(
										LOGCAT,
										">> Error saving trade " + trade == null ? ""
												: trade.getLongId()
														+ " :"
														+ exception
																.getMessage());
							}
						}
						
						Log.info(LOGCAT, "Saving thread " + Thread.currentThread().getId() +" finished");
					}
				};

				final Thread[] threads = new Thread[NUM_THREAD];
				for (int i = 0; i < NUM_THREAD; i++) {

					threads[i] = new Thread(runnableSave);
					threads[i].start();
				}

				// TimeOut
				for (int i = 0; i < NUM_THREAD; i++) {
					try {
						threads[i].join(3600 * 4 * 1000);
					} catch (final InterruptedException exception) {
						Log.error(LOGCAT, exception.getMessage());
					}
				}

				Log.system(LOGCAT, "Saving operation finished for LE "+leId);

		
			}
		}

		return errorMessage;
	}

	/**
	 * Gets the domain attributes.
	 *
	 * @return the domain attributes
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.ScheduledTask#getDomainAttributes()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Vector getDomainAttributes() {
		Vector attributes = new Vector();

		attributes.addElement(LEGAL_ENTITY_ATTRIBUTE);

		attributes.addElement(TARGET_KEYWORD);

		attributes.addElement(DEFAULT_VALUE_IS_METHOD);

		attributes.addElement(DEFAULT_VALUE);

		attributes.addElement(ACTION);

		//attributes.addElement(NUMBER_OF_TIMES_TO_RETRY);

		return attributes;
	}

	/**
	 * Gets the attribute domain.
	 *
	 * @param attr
	 *            the attr
	 * @param currentAttr
	 *            the current attr
	 * @return the attribute domain
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.calypso.tk.util.ScheduledTask#getAttributeDomain(java.lang.String,
	 * java.util.Hashtable)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getAttributeDomain(String attr, Hashtable currentAttr) {

		Vector values = new Vector();

		if (attr.equals(LEGAL_ENTITY_ATTRIBUTE)) {
			Vector<String> legalEntityAttributes = new Vector<String>();
			try {
				legalEntityAttributes = DSConnection.getDefault()
						.getRemoteReferenceData()
						.getDomainValues("leAttributeType");
			} catch (CalypsoServiceException e) {
				Log.error(LOGCAT, e);
			}
			values.addAll(legalEntityAttributes);
		} else if (attr.equals(TARGET_KEYWORD)) {
			Vector<String> targetKeywords = new Vector<String>();
			try {
				targetKeywords = DSConnection.getDefault()
						.getRemoteReferenceData()
						.getDomainValues("tradeKeyword");
			} catch (CalypsoServiceException e) {
				Log.error(LOGCAT, e);
			}
			values.addAll(targetKeywords);
		}
		if (attr.equals(DEFAULT_VALUE_IS_METHOD)) {
			values.add(Boolean.TRUE);
			values.add(Boolean.FALSE);
		} else if (attr.equals(ACTION)) {
			Vector<String> actions = new Vector<String>();
			try {
				actions = DSConnection.getDefault().getRemoteReferenceData()
						.getDomainValues("tradeAction");
			} catch (CalypsoServiceException exception) {
				Log.error(LOGCAT, exception);
			}
			values.addAll(actions);
		}

		return values;
	}

	/**
	 * Checks if is valid input.
	 *
	 * @param messages
	 *            the messages
	 * @return true, if is valid input
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.ScheduledTask#isValidInput(java.util.Vector)
	 */
	@Override
	public boolean isValidInput(Vector<String> messages) {

		boolean ret = true;

		String tradeFilter;
		tradeFilter = this.getTradeFilter();
		if (Util.isEmpty(tradeFilter)) {
			messages.addElement("Selecting a TRADE FILTER is mandatory in order to execute this scheduled task.");
			ret = false;
		}

		String legalEntityAttribute;
		legalEntityAttribute = this.getAttribute(LEGAL_ENTITY_ATTRIBUTE);
		if (Util.isEmpty(legalEntityAttribute)) {
			messages.addElement(LEGAL_ENTITY_ATTRIBUTE
					+ ": Please select a legal entity attribute to store on the "
					+ TARGET_KEYWORD);
			ret = false;
		}

		String tradeKeyword;
		tradeKeyword = this.getAttribute(TARGET_KEYWORD);
		if (Util.isEmpty(tradeKeyword)) {
			messages.addElement(TARGET_KEYWORD
					+ ": Please input a keyword name on which "
					+ LEGAL_ENTITY_ATTRIBUTE + " is stored");
			ret = false;
		}

		return ret;
	}

	/**
	 * Gets the task information.
	 *
	 * @return the task information
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.util.ScheduledTask#getTaskInformation()
	 */
	@Override
	public String getTaskInformation() {
		return "This scheduled task is in charge of reading the audit report and storing the old legal entity attribute into a specific trade keyword amongst every trade that has the entity as a counterparty.";
	}
}
