package calypsox.tk.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.CustomClientCacheAdapter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventInventorySecPosition;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.InventorySecurityPositionArray;

import calypsox.tk.report.CustomFallidasReport;
import calypsox.tk.report.CustomFallidasReportStyle;

/**
 * The Class CustomReportRowFallidasCacheAdapter.
 */
public class CustomFallidasfallidasReportClientCacheAdapter extends CustomClientCacheAdapter
		implements ReporRowCacheAdapterInterface {

	// Hashmap XFER
	private ConcurrentHashMap<Long, ReportRow> transferListCache = new ConcurrentHashMap<>();
	// Hashmap trade
	private ConcurrentHashMap<Long, ArrayList<Long>> tradeListCache = new ConcurrentHashMap<>();
//	HashMap Inventory position
	private ConcurrentHashMap<Object, ArrayList<Long>> inventoryPosListCache = new ConcurrentHashMap<>();
	// Hashmap message
	private ConcurrentHashMap<Long, Long> messageListCache = new ConcurrentHashMap<>();

	private PricingEnv env = null;

	// constante reporte /** Longitud del bic. */
	public static final int MAX_SIZE_BIC = 8;

	/**
	 * Initialize cache.
	 */
	public void preloadCache(DefaultReportOutput defaultReportOutput) throws CalypsoServiceException {

		// Lista con todos los tradeIds del reporte
		Set<Long> uniqueTradeIdsList = new HashSet<>();
		Log.debug(this.getClass().getName(), "Method preloadCache.");
		DSConnection dsConn = getDSConnection();
		if (dsConn != null) {
			// Recupera filas del reporte
			ReportRow[] rows = defaultReportOutput.getRows();
			Log.debug(this.getClass().getName(), "Rows: " + rows.length);

			// Recorre las Rows del reporte que ya existe cargado en Calypso
			for (int i = 0; i < rows.length; i++) {
				// Recupera fila original
				Log.debug(this.getClass().getName(), "Recorre Fila: " + rows[i].toString());
				// Recupera properties originales
				BOTransfer transfer = rows[i].getProperty(ReportRow.TRANSFER);
				Trade trade = rows[i].getProperty(ReportRow.TRADE);
				JDatetime valDateTime = rows[i].getProperty(ReportRow.VALUATION_DATETIME);
				if (valDateTime == null)
					valDateTime = new JDatetime();
				JDate valDate = (trade != null && trade.getBook() != null) ? trade.getBook().getJDate(valDateTime)
						: valDateTime.getJDate(TimeZone.getDefault());
				Map<String, HashMap<String, Object>> messages = rows[i]
						.getProperty(CustomFallidasReportStyle.MESSAGES_MAP);

				LEContact contact = rows[i].getProperty(ReportRow.LE_CONTACT);
				Double posicionDisponible = getPosition(transfer, valDate, dsConn);
				rows[i].setProperty(CustomFallidasReport.PROPERTY_POSITION, posicionDisponible);

				SettleDeliveryInstruction sdi = rows[i].getProperty(ReportRow.LE_SDI);
				if (sdi == null) {
					sdi = CustomFallidasReport.getSdi(transfer, dsConn);
				}
				if (contact == null) {
					contact = CustomFallidasReport.getContact(transfer, sdi, dsConn);
				}
				Log.debug(this.getClass().getName(), "Tranfer: " + transfer.toString());
				// Nueva row para el reporte en bocache
				ReportRow newRow = new ReportRow(transfer);
				newRow.setUniqueKey(transfer.getKey());
				newRow.setProperty(ReportRow.TRANSFER, transfer);
				newRow.setProperty(ReportRow.TRADE, trade);
				newRow.setProperty(CustomFallidasReportStyle.MESSAGES_MAP, messages);
				newRow.setProperty(ReportRow.LE_SDI, sdi);
				newRow.setProperty(ReportRow.LE_CONTACT, contact);
				newRow.setProperty(CustomFallidasReport.PROPERTY_POSITION, posicionDisponible);

				if (messages == null) {
					messages = CustomFallidasReport.findPrevMTMessage(transfer, dsConn,
							getPricingEnv(defaultReportOutput),trade);
					if (!Util.isEmpty(messages)) {
						newRow.setProperty(CustomFallidasReportStyle.MESSAGES_MAP, messages);
					}
				}
				// Insertamos en Hashmap XFER
				transferListCache.put(transfer.getLongId(), newRow);
				Log.debug(this.getClass().getName(), "Se incluye a transferListCache" + newRow.toString());
				// Insertamos en Hashmap MESSAGE
				if (messages != null) {
					for (HashMap<String, Object> message : messages.values()) {
						BOMessage mess = (BOMessage) message.get(BOMessage.class.getSimpleName());
						messageListCache.put(mess.getLongId(), mess.getTransferLongId());
					}
				}
				// Hashmap Trade:lista para generar el hashmap TRADE despues
				if (trade != null) {
					uniqueTradeIdsList.add(trade.getLongId());
				}
			}
			// Recorremos la lista de trades anadiendo sus transfersid
			final Iterator<Long> itUniquetradeIdsList = uniqueTradeIdsList.iterator();
			// Arraylist para listas tradeIds
			ArrayList<Long> trasferIdArrayList = null;

			while (itUniquetradeIdsList.hasNext()) {
				final Long uniqueTradeId = itUniquetradeIdsList.next();
				trasferIdArrayList = new ArrayList<>();

				// Recorremos la lista de rows recuperando las transfers de cada trade
				for (int i = 0; i < rows.length; i++) {
					Trade trade = rows[i].getProperty(ReportRow.TRADE);
					BOTransfer transfer = rows[i].getProperty(ReportRow.TRANSFER);
					if (trade != null && uniqueTradeId == trade.getLongId()) {
						Long trasferId = transfer.getLongId();
						trasferIdArrayList.add(trasferId);
					}
				}
				// Insertamos en Hashmap TRADE
				tradeListCache.put(uniqueTradeId, trasferIdArrayList);
			}
		}
	}

	@Override
	public void clear() {
		super.clear();
		transferListCache.clear();
		tradeListCache.clear();
		messageListCache.clear();
		inventoryPosListCache.clear();
	}

	/**
	 * @param trade
	 */
	public void eventosTrade(Trade trade) {
		boolean extistTradeId = tradeListCache.containsKey(trade.getLongId());
		// Get value ArrayList from Trade Hashmap with all transfer_id´s

		// If exists, for each transfer_id get value from Transfer Hashmap to get
		// related ReportRow
		if (extistTradeId) {
			ArrayList<Long> trasferIdArrayList = tradeListCache.get(trade.getLongId());
			// Create an iterator for the listusing iterator() method
			ArrayList<Long> toRemove = new ArrayList<>();
			for (Iterator<Long> iter = trasferIdArrayList.iterator(); iter.hasNext();) {
				Long transferId = iter.next();
				ReportRow relatedRow = transferListCache.get(transferId);
				if (relatedRow != null) {
					// If exists, update object trade from the property of ReportRow
					transferListCache.get(transferId).setProperty(ReportRow.TRADE, trade);
					// Check report filters with this ReportRow
					boolean acceptFilter = checkReportFilters(transferListCache.get(transferId));
					// If not exists, remove entry from ArrayList of Trade Hashmap. If ArrayList is
					// empty remove key from Trade Hashmap
					if (!acceptFilter) {
						// Remove ReportRow from Transfer Hashmap.
						transferListCache.remove(transferId);
						toRemove.add(transferId);
					}
				}
			}
			for (Long trasferIds : toRemove) {
				trasferIdArrayList.remove(trasferIds);
			}

			if (trasferIdArrayList.isEmpty()) {
				tradeListCache.remove(trade.getLongId());
			}
		} // If not exists, discard event

	}

	/**
	 * @param transfer
	 */
	public void eventosTransfer(BOTransfer transfer) {

		// Get transfer,trade and message from Psvent/DSConnection and generate
		// ReportRow
		Trade trade = null;
		Map<String, HashMap<String, Object>> messages = null;

		ReportRow newRow = new ReportRow(transfer);
		DSConnection dsConn = getDSConnection();
		// Trade
		try {
			trade = dsConn.getRemoteTrade().getTrade(transfer.getTradeLongId());
		} catch (CalypsoServiceException e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error en el tratamiento de eventosTransfer. No se puede recuperar el trade de la transfer: "
							+ transfer.getLongId(),
					e);
		}

		messages = CustomFallidasReport.findPrevMTMessage(transfer, dsConn, getPricingEnv(null),trade);

		// Hashmap XFER geeneracion nuevo Row
		newRow.setUniqueKey(transfer.getKey());
		newRow.setProperty(ReportRow.TRANSFER, transfer);
		newRow.setProperty(ReportRow.TRADE, trade);
		newRow.setProperty(CustomFallidasReportStyle.MESSAGES_MAP, messages);
		// CONTACT*
		SettleDeliveryInstruction sdi = null;

		sdi = CustomFallidasReport.getSdi(transfer, dsConn);
		newRow.setProperty(ReportRow.LE_SDI, sdi);
		newRow.setProperty(ReportRow.LE_CONTACT, CustomFallidasReport.getContact(transfer, sdi, dsConn));

		JDatetime valDateTime = null;
		if (valDateTime == null)
			valDateTime = new JDatetime();
		JDate valDate = (trade != null && trade.getBook() != null) ? trade.getBook().getJDate(valDateTime)
				: valDateTime.getJDate(TimeZone.getDefault());

		// Check report filters with this ReportRow, If OK:
		boolean acceptFilter = checkReportFilters(newRow);
		// If OK:Insert/Update transfer, trade and message objects
		if (acceptFilter) {
			boolean extistTransferId = transferListCache.containsKey(transfer.getLongId());
			if (extistTransferId) {
				// update
				transferListCache.get(transfer.getLongId()).setProperty(ReportRow.TRANSFER, transfer);
				transferListCache.get(transfer.getLongId()).setProperty(ReportRow.TRADE, trade);
				transferListCache.get(transfer.getLongId()).setProperty(CustomFallidasReportStyle.MESSAGES_MAP,
						messages);
				LEContact contact = null;

				sdi = CustomFallidasReport.getSdi(transfer, dsConn);
				contact = CustomFallidasReport.getContact(transfer, sdi, dsConn);

				transferListCache.get(transfer.getLongId()).setProperty(ReportRow.LE_SDI, sdi);
				transferListCache.get(transfer.getLongId()).setProperty(ReportRow.LE_CONTACT, contact);
				transferListCache.get(transfer.getLongId()).setProperty(CustomFallidasReport.PROPERTY_POSITION,
						getPosition(transfer, valDate,getDSConnection()));

			} else {
				newRow.setProperty(CustomFallidasReport.PROPERTY_POSITION, getPosition(transfer, valDate,getDSConnection()));
				// Insert
				transferListCache.put(transfer.getLongId(), newRow);

				// Insertamos en Hashmap TRADE
				// Arraylist para listas
				if (trade != null) {
					boolean extistTradeId = tradeListCache.containsKey(trade.getLongId());
					// Get value ArrayList from Trade Hashmap with all transfer_id´s
					if (!extistTradeId) {
						ArrayList<Long> trasferIdArrayList = null;
						trasferIdArrayList = new ArrayList<>();
						trasferIdArrayList.add(transfer.getLongId());
						tradeListCache.put(trade.getLongId(), trasferIdArrayList);
					} else {
						tradeListCache.get(trade.getLongId()).add(transfer.getLongId());
					}
				}
				// Insertamos en Hashmap MESSAGE
				if (messages != null) {
					for (HashMap<String, Object> message : messages.values()) {
						BOMessage mess = (BOMessage) message.get(BOMessage.class.getSimpleName());
						messageListCache.put(mess.getLongId(), mess.getTransferLongId());
					}
				}
			}

		} else// Check report filters with this ReportRow, If not OK:
		{

			boolean extistTransferId = transferListCache.containsKey(transfer.getLongId());
			// Remove ReportRow if it exists in Hashmap Transfer.
			if (extistTransferId) {
				transferListCache.remove(transfer.getLongId());
			}

			// ArrayList on Trade hashmap should be updated or removed.
			if (trade != null) {
				boolean extistTradeId = tradeListCache.containsKey(trade.getLongId());
				if (extistTradeId) {
					ArrayList<Long> trasferIdArrayList = tradeListCache.get(trade.getLongId());
					ArrayList<Long> toRemove = new ArrayList<>();
					for (Iterator<Long> iter = trasferIdArrayList.iterator(); iter.hasNext();) {
						Long trasferId = iter.next();
						if (trasferId.equals(transfer.getLongId())) {
							toRemove.add(trasferId);
						}
					}
					for (Long trasferIds : toRemove) {
						trasferIdArrayList.remove(trasferIds);
					}

					if (trasferIdArrayList.isEmpty()) {
						tradeListCache.remove(trade.getLongId());
					}
				}
			}

			// Message hasmap entry should be removed.
			if (messages != null) {
				for (HashMap<String, Object> message : messages.values()) {
					BOMessage mess = (BOMessage) message.get(BOMessage.class.getSimpleName());
					messageListCache.remove(mess.getLongId());
				}
			}
			InventorySecurityPositionArray secArray = CustomFallidasReport.getInvPositionsArray(transfer, valDate,
					dsConn);
			for (InventorySecurityPosition inventorySecurityPosition : secArray) {
				Object objKey = inventorySecurityPosition.getKey();
				ArrayList<Long> xferList = inventoryPosListCache.get(objKey);
				if (!Util.isEmpty(xferList)) {
					ArrayList<Long> xferListUpd = new ArrayList<>();
					xferListUpd.addAll(xferList);
					boolean update = false;
					for (Long xferLongId : xferList) {
						if (xferLongId == transfer.getLongId()) {
							xferListUpd.remove(xferLongId);
							update = true;
						}
					}
					if (Util.isEmpty(xferListUpd)) {
						inventoryPosListCache.remove(objKey);
					} else if (update) {
						inventoryPosListCache.put(objKey, xferListUpd);
					}
				}
			}

		}
	}

	/**
	 * @param message
	 */
	public void eventosMessage(BOMessage message) {
		// Get value (transfer_id) from Message Hashmap
		boolean extistMessageId = messageListCache.containsKey(message.getLongId());

		// If not exists, discard event
		if (extistMessageId) {
			// If exists get value from Transfer Hashmap to get related ReportRow
			Long transferId = messageListCache.get(message.getLongId());
			// If exists, update object message from the property of ReportRow
			ReportRow row = transferListCache.get(transferId);
			if (row != null) {
				BOTransfer trans = row.getProperty(ReportRow.TRANSFER);
				eventosTransfer(trans);
			}
		}
	}

	/**
	 * @param trade
	 */
	public void eventosInventory(InventorySecurityPosition posicionDisponible) {
		if (posicionDisponible != null) {
			Object objKey = posicionDisponible.getKey();
			ArrayList<Long> xferList = inventoryPosListCache.get(objKey);
			if (!Util.isEmpty(xferList)) {
				Double pos = null;
				for (Long xferLongId : xferList) {
					ReportRow row = transferListCache.get(xferLongId);
					pos = updatePositionInRow(row, pos, getDSConnection());
				}
			}
		}
	}

	public Double updatePositionInRow(ReportRow row, Double pos, DSConnection dsConn) {
		if (row != null) {
			if (pos == null) {
				BOTransfer trans = row.getProperty(ReportRow.TRANSFER);
				Trade trade = row.getProperty(ReportRow.TRADE);
				JDatetime valDateTime = row.getProperty(ReportRow.VALUATION_DATETIME);
				if (valDateTime == null)
					valDateTime = new JDatetime();
				JDate valDate = (trade != null && trade.getBook() != null) ? trade.getBook().getJDate(valDateTime)
						: valDateTime.getJDate(TimeZone.getDefault());
				pos = getPosition(trans, valDate, dsConn);
			}
			row.setProperty(CustomFallidasReport.PROPERTY_POSITION, pos);
		}
		return pos;
	}

	/**
	 * @param relatedRow
	 * @return
	 */
	private static Boolean checkReportFilters(ReportRow relatedRow) {

		ReportTemplate template = null;
		String reportType = "CustomFallidas";
		String reportTemplate = "fallidasReport";
		ReportStyle reportStyle = new CustomFallidasReportStyle();
		DSConnection dsConn = getDSConnection();
		Report report = null;
		JDatetime valDateTime = new JDatetime();

		try {
			String className = "tk.report." + reportType + "Report";
			report = (Report) InstantiateUtil.getInstance(className, true);
			report.setValuationDatetime(valDateTime);
			report.setUndoDatetime(null);
			report.setForceUndo(false);

		} catch (Exception e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error en checkReportFilters al cargar el reporte: " + e.getCause());

			report = null;
		}
		if (report != null && !Util.isEmpty(reportTemplate)) {

			try {
				template = dsConn.getRemoteReferenceData().getReportTemplate(ReportTemplate.getReportName(reportType),
						reportTemplate);
			} catch (CalypsoServiceException e) {
				Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
						"Error en checkReportFilters al cargar la template: ", e);
			}
		}
		if (template != null) {
			return template.accept(reportStyle, relatedRow);
		} else {
			return false;
		}
	}

	/**
	 * @return ReportRow[]
	 */
	@Override
	public ReportRow[] getReportRows() {

		ReportRow[] rows = new ReportRow[transferListCache.values().size()];
		rows = transferListCache.values().toArray(rows);

		return rows;
	}

	private PricingEnv getPricingEnv(DefaultReportOutput defaultReportOutput) {
		if (env == null && defaultReportOutput != null) {
			env = defaultReportOutput.getPricingEnv();
		}
		return env;
	}

	public Double getPosition(BOTransfer transfer, JDate valDate, DSConnection dsConn) {
		Double result = 0.0;
		InventorySecurityPositionArray secPositions = CustomFallidasReport.getInvPositionsArray(transfer, valDate,
				dsConn);
		if (secPositions != null && !secPositions.isEmpty()) {
			for (InventorySecurityPosition inventorySecurityPosition : secPositions) {
				// Hashmap Inventory: lista para generar hashmap del inventory.
				Object key = inventorySecurityPosition.getKey();
				ArrayList<Long> lstXferId = inventoryPosListCache.get(key);
				if (lstXferId == null) {
					lstXferId = new ArrayList<>();
				}
				if (!lstXferId.contains(transfer.getLongId())) {
					ArrayList<Long> lstXferNew = new ArrayList<>();
					lstXferNew.addAll(lstXferId);
					lstXferNew.add(transfer.getLongId());
					inventoryPosListCache.put(key, lstXferNew);
				}
				double princ = inventorySecurityPosition.getProduct().getPrincipal(valDate);
				result += inventorySecurityPosition.getTotalSecurity() * princ;
			}
			return result;
		}
		return null;
	}

	public static DSConnection getDSConnection() {
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

	@Override
	public boolean newEvent(PSEvent event, String engineName) throws CalypsoServiceException {
		// Procesado de eventos segun tipo
		if (event instanceof PSEventTrade) {
			Log.debug(this.getClass().getName(),
					"Process method . PSEventTrade " + " Trade event: " + event.toString());
			Trade trade = ((PSEventTrade) event).getTrade();
			this.eventosTrade(trade);
		} else if (event instanceof PSEventTransfer) {
			Log.debug(this.getClass().getName(),
					"Process method . PSEventTransfer " + " Xfer event: " + event.toString());
			BOTransfer transfer = ((PSEventTransfer) event).getBoTransfer();
			this.eventosTransfer(transfer);
		} else if (event instanceof PSEventMessage) {
			Log.debug(this.getClass().getName(),
					"Process method . PSEventMessage " + " BOMessage event: " + event.toString());
			PSEventMessage em = (PSEventMessage) event;
			BOMessage message = em.getBoMessage();
			this.eventosMessage(message);

		} else if (event instanceof PSEventInventorySecPosition) {
			Log.debug(this.getClass().getName(),
					"Process method . PSEventInventorySecPosition " + " Inventory event: " + event.toString());
			PSEventInventorySecPosition eI = ((PSEventInventorySecPosition) event);
			InventorySecurityPosition isp = eI.getPosition();
			if (isp != null) {
				this.eventosInventory(isp);
			}
		} else {
			Log.info(this.getClass().getName(), "Event type not supported. Discarding event.");
		}
		return true;
	}

}