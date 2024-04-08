package calypsox.tk.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOMessageHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.FormatterUtil;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.SWIFTFormatterUtil;
import com.calypso.tk.bo.TradeRoleFinder;
import com.calypso.tk.bo.swift.SWIFTFormatter;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.SwiftParserUtil;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import calypsox.util.binding.CustomBindVariablesUtil;

public class CustomFallidasReport extends com.calypso.tk.report.TransferReport {

	private static final String MSG = "MSG";

	private static final String SETTLEMENT = "Settlement";

	private static final String SECURITIES_SELECTOR = "Securities.selector";

	private static final String ENGLISH = "English";

	private static final String SWIFT = "SWIFT";

	/**
	 * Serial number asignado por Calypso
	 */
	private static final long serialVersionUID = 1L;

	private boolean countOnly = false;
	public static final String PROPERTY_POSITION = "PROPERTY_POSITION";

	/**
	 * Sobreescribirmos parte del load para recuperar el mensaje en caso que exista
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DSConnection dsConn = getDSConnection();
		JDate date = this.getDate("EndDate", "EndPlus", "EndTenor");
		Log.info(this,  "Dia ejecucion reporte: " + date.toString() + "; A la hora:" + new JDatetime().toString());
				DefaultReportOutput dro = (DefaultReportOutput) super.load(errorMsgs);


		if (!countOnly) {
			ReportRow[] rows = dro.getRows();
			for (int i = 0; i < rows.length; i++) {
				ReportRow row = rows[i];
				row = setRowData(row, dsConn);
				rows[i] = row;
			}
			dro.setRows(rows);
		}
		
		return dro;

	}

	public ReportRow setRowData(ReportRow row, DSConnection dsConn) {
		BOTransfer transfer = row.getProperty(ReportRow.TRANSFER);
		if (transfer == null) {
			transfer = row.getProperty(ReportRow.DEFAULT);
		}
		if (transfer != null) {
			Trade trade = row.getProperty(ReportRow.TRADE);
			JDatetime valDateTime = row.getProperty(ReportRow.VALUATION_DATETIME);
			if (valDateTime == null)
				valDateTime = new JDatetime();
			JDate valDate = (trade != null && trade.getBook() != null) ? trade.getBook().getJDate(valDateTime)
					: valDateTime.getJDate(TimeZone.getDefault());
			Map<String, HashMap<String, Object>> message = row.getProperty(CustomFallidasReportStyle.MESSAGES_MAP);
			if (Util.isEmpty(message)) {
				message = findPrevMTMessage(transfer, dsConn, getPricingEnv(), trade);
			}

			// Guardamos el LE_CONTACT en la row
			SettleDeliveryInstruction sdi = getSdi(transfer, dsConn);
			row.setProperty(ReportRow.LE_SDI, sdi);
			row.setProperty(ReportRow.LE_CONTACT, getContact(transfer, sdi, dsConn));

			// Guardamos el MESSAGE en la row
			if (message != null) {
				row.setProperty(CustomFallidasReportStyle.MESSAGES_MAP, message);
			}
			Double positionDbl = getPosition(transfer, valDate, dsConn);
			// Guardamos la position en la row
			row.setProperty(PROPERTY_POSITION, positionDbl);
		}
		return row;
	}

	public static Double getPosition(BOTransfer transfer, JDate valDate, DSConnection dsConn) {
		Double result = 0.0;
		InventorySecurityPositionArray secPositions = getInvPositionsArray(transfer, valDate, dsConn);
		if (secPositions != null && !secPositions.isEmpty()) {

			for (InventorySecurityPosition inventorySecurityPosition : secPositions) {
				double princ = inventorySecurityPosition.getProduct().getPrincipal(valDate);
				result += inventorySecurityPosition.getTotalSecurity() * princ;
			}
			return result;
		}
		return null;
	}

	public static InventorySecurityPositionArray getInvPositionsArray(BOTransfer transfer, JDate valDate,
			DSConnection dsConn) {
		int pId = transfer.getProductId();
		StringBuilder where = new StringBuilder();
		StringBuilder from = new StringBuilder();
		where.append(" inv_sec_balance.config_id=?");
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(0);
		where.append(" and inv_sec_balance.security_id=?");
		CustomBindVariablesUtil.addNewBindVariableToList(pId, bindVariables);
		where.append(" and inv_sec_balance.INTERNAL_EXTERNAL=?");
		CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.INTERNAL.toUpperCase(), bindVariables);
		where.append(" and inv_sec_balance.POSITION_TYPE=?");
		CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.ACTUAL.toUpperCase(), bindVariables);
		where.append(" and inv_sec_balance.DATE_TYPE=?");
		CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.SETTLE.toUpperCase(), bindVariables);
		where.append(" ");

		try {
			return dsConn.getRemoteInventory().getSecurityPositionsFromTo(from.toString(), where.toString(), valDate,
					valDate, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error(CustomFallidasReport.class, "Error getting security position from Inventory.", e);
		}
		return null;
	}

	/**
	 * @param transfer Transfer object of the report.
	 * @param dsCon    DS Connection to RODS or DS.
	 * @return Message map to use in the report.
	 */
	public static Map<String, HashMap<String, Object>> findPrevMTMessage(BOTransfer transfer, DSConnection dsCon,
			PricingEnv env, Trade trade) {
		StringBuilder where = new StringBuilder();
		where.append(" TRANSFER_ID = ?");
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(transfer.getLongId());
		where.append(" AND ADDRESS_METHOD LIKE ?");
		CustomBindVariablesUtil.addNewBindVariableToList(SWIFT, bindVariables);
		where.append(" AND TEMPLATE_NAME LIKE ?");
		CustomBindVariablesUtil.addNewBindVariableToList("MT%", bindVariables);
		where.append(" AND MESSAGE_CLASS=?");
		CustomBindVariablesUtil.addNewBindVariableToList("BOTransferMessage", bindVariables);
		String orderBy = "message_id DESC";
		HashMap<String, HashMap<String, Object>> hashMessages = new HashMap<>();

		try {
			MessageArray messages = dsCon.getRemoteBO().getMessages(null, where.toString(), orderBy, bindVariables);
			BOMessage[] mess = messages.getMessages();
			if (!Util.isEmpty(mess)) {
				for (BOMessage boMessage2 : mess) {
					String temp = boMessage2.getTemplateName();
					temp = temp.substring(0, 5);
					if (!hashMessages.containsKey(temp) && !Util.isEmpty(temp) && temp.startsWith("MT5")) {
						
						try {
							BOMessage boMessage = dsCon.getRemoteBO().getMessage(boMessage2.getLongId());
							ExternalMessage extMessage = SwiftParserUtil.getExternalMessage(boMessage, env);
							if (extMessage instanceof SwiftMessage) {
								SwiftMessage swiftMessage = (SwiftMessage) extMessage;
								HashMap<String, Object> hashMessagesBOSwift = new HashMap<>();
								hashMessagesBOSwift.put(BOMessage.class.getSimpleName(), boMessage);
								hashMessagesBOSwift.put(SwiftMessage.class.getSimpleName(), swiftMessage);
								hashMessages.put(temp, hashMessagesBOSwift);
							}
						} catch (Exception e) {
							Log.error(CustomFallidasReportStyle.class, "Error loading Messages from Transfer: " + transfer.getLongId(),
									e);
						}
						
					}
				}
			}
		} catch (CalypsoServiceException e) {
			
		}
		SwiftMessage swiftMessage = null;
		for (int i = 0; i < 4 && swiftMessage == null; i++) {
			if (hashMessages != null && hashMessages.get(CustomFallidasReportStyle.MT54 + i) != null
					&& hashMessages.get(CustomFallidasReportStyle.MT54 + i) instanceof HashMap<?, ?>) {
				Map<?, ?> hashMess = hashMessages.get(CustomFallidasReportStyle.MT54 + i);
				if (hashMess.get(SwiftMessage.class.getSimpleName()) instanceof SwiftMessage) {
					swiftMessage = (SwiftMessage) hashMess.get(SwiftMessage.class.getSimpleName());
				}

			}
		}
		if (swiftMessage == null) {

			BOMessage boMsg = getSimulatedBOMessage(transfer, trade);
			if (boMsg != null && !Util.isEmpty(boMsg.getTemplateName())) {
				SWIFTFormatter sf = SWIFTFormatterUtil.findSWIFTFormatter(boMsg);
				try {
					boMsg.setLongId(transfer.getLongId());
					swiftMessage = sf.generateSwift(env, boMsg, DSConnection.getDefault());
					HashMap<String, Object> hashMessagesBOSwift = new HashMap<>();					
					hashMessagesBOSwift.put(BOMessage.class.getSimpleName(), boMsg);
					hashMessagesBOSwift.put(SwiftMessage.class.getSimpleName()+CustomFallidasReportStyle.SIMULATED, swiftMessage);
					hashMessages.put(boMsg.getTemplateName(), hashMessagesBOSwift);
					boMsg.setLongId(0);
				} catch (Exception  e) {
					Log.error(CustomFallidasReport.class.getName(),
							"Error creating simulated swift message." + e);
				}
			}
		}
		return hashMessages;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static BOMessage getSimulatedBOMessage(BOTransfer transfer, Trade trade) {
		BOMessage boMsg = null;

		String productType2 = null;
		String productFamily = null;
		if (transfer != null) {
			productType2 = transfer.getProductType();
			productFamily = transfer.getProductFamily();

			BOMessageHandler messHandler = null;
			AdviceConfig config = new AdviceConfig();
			config.setAddressMethod(SWIFT);
			config.setFormatType(SWIFT);
			config.setLanguage(ENGLISH);
			config.setTemplateName(SECURITIES_SELECTOR);
			config.setReceiverContactType(SETTLEMENT);
			config.setSenderContactType(SETTLEMENT);
			config.setReceiverRole(transfer.getExternalRole());
			config.setMessageType(transfer.getEventType() + MSG);
			messHandler = BOMessageHandler.getHandler(productType2, productFamily, config);
			Vector legalEntities = null;
			Vector localExceptionVector = new Vector();
			legalEntities = TradeRoleFinder.getTransferRoles(transfer.getExternalRole(), transfer, trade,
					localExceptionVector, config, DSConnection.getDefault());
			LegalEntity leSender = BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getProcessingOrg());
			if (legalEntities != null && !legalEntities.isEmpty()) {
				LegalEntity leReceiver = (LegalEntity) legalEntities.elementAt(0);
				PSEventTransfer event = new PSEventTransfer(transfer, trade, null);
				MessageArray newMessages = messHandler.generateMessages(config, leReceiver, leSender, trade, transfer,
						event, localExceptionVector, DSConnection.getDefault());
				if (newMessages != null) {
					for (int j = 0; j < newMessages.size(); ++j) {
						boMsg = newMessages.elementAt(j);
						String template = FormatterUtil.getSelectedTemplate(trade, boMsg, SECURITIES_SELECTOR);
						boMsg.setTemplateName(template);
					}
				}
			}
		}
		return boMsg;
	}

	/**
	 *
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Map<?, ?> getPotentialSize() {
		try {
			this._potentialSize = new HashMap<>();
			this.countOnly = true;
			super.getPotentialSize();
		} finally {
			this.countOnly = false;

		}
		return this._potentialSize;
	}

	/**
	 * @param transfer
	 * @return
	 * @throws CalypsoServiceException
	 */
	public static LEContact getContact(BOTransfer transfer, SettleDeliveryInstruction sdi2, DSConnection dsConn) {
		SettleDeliveryInstruction sdi = sdi2;
		if (sdi == null) {
			sdi = getSdi(transfer, dsConn);
		}
		if (sdi != null) {

			LEContact contact = BOCache.getContact(DSConnection.getDefault(), "Agent",
					BOCache.getLegalEntity(DSConnection.getDefault(), sdi.getAgent().getPartyId()),
					sdi.getAgent().getPartyContactType(), transfer.getProductType(), transfer.getProcessingOrg());
			if (contact != null && !Util.isEmpty(contact.getSwift())) {
				return contact;
			}
		}
		return null;
	}

	/**
	 * @param transfer
	 * @return
	 */
	public static SettleDeliveryInstruction getSdi(BOTransfer transfer, DSConnection dsConn) {
		SettleDeliveryInstruction sdi = null;
		int sdiId;

		try {
			if (transfer != null) {
				// Recuperamos SDI
				if (transfer.getPayReceive().equals("Receive")) {
					sdiId = transfer.getPayerSDId();
					if (String.valueOf(sdiId) != null && !String.valueOf(sdiId).equals("")
							&& !String.valueOf(sdiId).equals("0")) {

						sdi = dsConn.getRemoteReferenceData().getSettleDeliveryInstruction(sdiId);

					}
				} else {
					sdiId = transfer.getReceiverSDId();
					if (String.valueOf(sdiId) != null && !String.valueOf(sdiId).equals("")
							&& !String.valueOf(sdiId).equals("0")) {
						sdi = dsConn.getRemoteReferenceData().getSettleDeliveryInstruction(sdiId);
					}

				}
				return sdi;

			}
		} catch (CalypsoServiceException e) {
			Log.error(CustomFallidasReportStyle.class, "Error getting SDIs for field Custodio");
		}
		return null;
	}

	@Override
	public DSConnection getDSConnection() {
		DSConnection dsConn = null;
		try {
			dsConn = DSConnection.getDefault().getReadOnlyConnection();
		} catch (ConnectException e1) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class, "Error getting DSConnection. ", e1);
		}
		if (dsConn == null) {
			dsConn = DSConnection.getDefault();
		}
		return dsConn;
	}
}