package calypsox.engine;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import calypsox.tk.export.ack.DUPAckProcessor;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Pair;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventInventorySecPosition;
import com.calypso.tk.export.DataExportBuilder;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.upload.sqlbindnig.UploaderSQLBindVariable;
import com.calypso.tk.upload.util.UploaderSQLBindAPI;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;

import calypsox.tk.event.PSEventReprocessExportMessages;
import calypsox.tk.export.AdviceDocUploaderXMLDataExporter;
import calypsox.tk.util.PositionExportEngineUtil;
import calypsox.tk.export.ack.MurexAckProcessor;

public class PositionExportEngine extends MultipleDestinationExportEngine {

	public static final String DV_EXPORT_TRADE_KEYWORDS = "export.trade.keywords";
	public static final String MUREX_EXPORT_ERROR_KW = "MurexExportError";

	String _configName;
	HashMap<String, Properties> propertiesPerInterface = null;
	ArrayList<DataExporterConfig> dataExporterConfigs = null;
	HashSet<String> allHandledMessageTypes = new HashSet<String>();

	public static final String LOG_CATEGORY = "PositionExportEngine";

	@Override
	public String getEngineName() {
		return "PositionExportEngine";
	}

	public PositionExportEngine(DSConnection dsCon, String hostName, int port) {
		super(dsCon, hostName, port);
		this.format = "Position";
	}

	@Override
	public boolean process(PSEvent event) {

		if (event instanceof PSEventInventorySecPosition) {
			if (handlePSEventInventorySecPosition((PSEventInventorySecPosition) event))
				try {
					CalypsoIDAPIUtil.eventProcessed(DSConnection.getDefault().getRemoteTrade(),
							CalypsoIDAPIUtil.getId(event), this.getEngineName());
					return true;
				} catch (Exception arg2) {
					Log.error(LOG_CATEGORY, arg2);
					return false;
				}
			else
				return false;
		} else if (event instanceof PSEventReprocessExportMessages) {
			if (handleReprocessEvent((PSEventReprocessExportMessages) event))
				try {
					CalypsoIDAPIUtil.eventProcessed(DSConnection.getDefault().getRemoteTrade(),
							CalypsoIDAPIUtil.getId(event), this.getEngineName());
					return true;
				} catch (Exception arg2) {
					Log.error(LOG_CATEGORY, arg2);
					return false;
				}
			else
				return false;
		} else
			return super.process(event);
	}

	public Object getSourceEventFromMessage(BOMessage message) {

		String adviceDocId = message.getAttribute(AdviceDocUploaderXMLDataExporter.DATA_ADVICE_DOCID_EVENT_ATTRIBUTE);
		AdviceDocument adviceDocument;
		try {
			adviceDocument = CalypsoIDAPIUtil.getAdviceDocument(DSConnection.getDefault().getRemoteBO(),
					Long.parseLong(adviceDocId));
			return adviceDocument.getAttributes().get(AdviceDocUploaderXMLDataExporter.SOURCE_EVENT_ATTRIBUTE);
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}

		return null;

	}

	public boolean reprocessMessage(BOMessage message, boolean sendAccountClosing) {

		String gateway = message.getGateway();
		if (gateway == null)
			return true;
		DataExporterConfig exporterConfig = getExporterConfigObjects().get(message.getGateway());
		if (exporterConfig == null)
			return true;
		Object sourceEvent = getSourceEventFromMessage(message);
		if (sourceEvent == null)
			return true;
		Trade sourceTrade = null;
		try {
			sourceTrade = getDS().getRemoteTrade().getTrade(message.getTradeLongId());
			Pair<Trade, Trade> modifiedTrade = getModifiedTrade(sourceTrade, (PSEventInventorySecPosition) sourceEvent,
					sendAccountClosing);
			(new DataExportBuilder(exporterConfig, this.format)).buildExporterData(PositionExportEngineUtil
					.getObjectToExport((PSEventInventorySecPosition) sourceEvent, modifiedTrade));
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
			return createErrorBOMessage(exporterConfig, (PSEventInventorySecPosition) sourceEvent, sourceTrade, e);
		}

		return true;

	}

	public boolean handleReprocessEvent(PSEventReprocessExportMessages event) {
		if (event.isMessageReprocess()) {
			return reprocessMessage(event.getMessage(), event.isSendAccountClosing());
		} else if (event.isTradeReprocess()) {
			HashMap<String, BOMessage> lastMessages = getLastMessagePerGateway(event.getTradeLongId());
			for (BOMessage message : lastMessages.values()) {
				if (event.getStatus() != null && event.getStatus().contains(message.getStatus())) {
					message.setAction(event.getMessageAction() != null ? event.getMessageAction() : Action.SEND);
					try {
						Log.debug("EXPORTER", "Applying " + event.getMessageAction() + " Action on the message "
								+ CalypsoIDAPIUtil.getId(message));
						DSConnection.getDefault().getRemoteBO().save(message, 0L, (String) null, (String) null);
					} catch (Exception arg8) {
						Log.error("EXPORTER", "Error while processing pending message before engine starts : Messageid "
								+ CalypsoIDAPIUtil.getId(message));
					}
				}
			}
		}

		return true;
	}

	protected HashSet<String> getAllHandledMessageTypes() {
		if (allHandledMessageTypes == null) {

			allHandledMessageTypes = new HashSet<String>();
			for (Properties props : getPropertiesPerGateway().values()) {
				String messageType = props.getProperty("MessageType");
				if (messageType != null) {
					allHandledMessageTypes.add(messageType);
				}
			}
		}
		return allHandledMessageTypes;
	}

	public HashMap<String, BOMessage> getLastMessagePerGateway(long tradeId) {
		MessageArray messages = null;
		ArrayList<UploaderSQLBindVariable> bindVariableList = new ArrayList<UploaderSQLBindVariable>();
		StringBuilder whereClause = new StringBuilder();
		Set<String> gateways = getPropertiesPerGateway().keySet();
		if (gateways.size() > 0) {
			whereClause.append("bo_message.gateway in ")
					.append(DataUploaderUtil.collectionToPreparedInString(gateways, bindVariableList));
			whereClause.append("and bo_message.trade_id = ")
					.append(DataUploaderUtil.valueToPreparedString(tradeId, bindVariableList));
		}
		try {
			messages = UploaderSQLBindAPI.getRemoteBOMessages(null, whereClause.toString(),
					"bo_message.message_id desc", bindVariableList);
		} catch (Exception arg10) {
			Log.error("EXPORTER", "Exception while getting messages based on criteria " + arg10.getMessage());
			return null;
		}

		if (messages == null)
			return null;

		HashMap<String, BOMessage> result = new HashMap<String, BOMessage>();
		for (int i = 0; i < messages.size() && result.size() < gateways.size(); i++) {
			BOMessage message = messages.get(i);
			if (result.get(message.getGateway()) == null) {
				result.put(message.getGateway(), message);
			}
		}

		return result;
	}

	public Collection<Trade> getTradesFromTransfers(long[] transferIds, PSEventInventorySecPosition event) {
		HashMap<Long, Trade> trades = new HashMap<Long, Trade>();

		for (int i = 0; i < transferIds.length; i++) {
			long transferId = transferIds[i];
			try {
				BOTransfer transfer = getDS().getRemoteBackOffice().getBOTransfer(transferId);
				if (!PositionExportEngineUtil.isSplit(transfer, event)) {
					long tradeId = transfer.getTradeLongId();
					if (!trades.containsKey(tradeId)) {
						trades.put(tradeId, getDS().getRemoteTrade().getTrade(tradeId));
					}
				}
			} catch (CalypsoServiceException e) {
				Log.error(LOG_CATEGORY, e);

			}
		}

		return trades.values();
	}

	public Collection<Pair<Trade, Trade>> getModifiedTradesToExport(PSEventInventorySecPosition event,
			boolean performAccountClosing) throws Exception {
		ArrayList<Pair<Trade, Trade>> trades = new ArrayList<Pair<Trade, Trade>>();

		for (Trade trade : getTradesFromTransfers(event.getTransferIds(), event)) {
			Pair<Trade, Trade> modifiedTrade = null;

			try {
				modifiedTrade = getModifiedTrade(trade, event, performAccountClosing);
			} catch (Exception e) {
				this.handleProcessingError(event, trade, e);
			}
			if (modifiedTrade != null)
				trades.add(modifiedTrade);
		}
		return trades;
	}

	public Pair<Trade, Trade> getModifiedTrade(Trade trade, PSEventInventorySecPosition event,
			boolean performAccountClosing) throws Exception {
		if (PositionExportEngineUtil.isExportTrade(event, trade)) {
			Trade modifiedTrade = (Trade) trade.clone();
			if (PositionExportEngineUtil.updateTrade(getDS(), modifiedTrade, event, performAccountClosing)) {
				filterTradeKeywords(modifiedTrade);
				return new Pair<Trade, Trade>(trade, modifiedTrade);
			}
		}
		return null;
	}

	public void handleProcessingError(PSEventInventorySecPosition event, Trade trade, Exception e) throws Exception {

		for (DataExporterConfig exporterConfig : getExporterConfigObjects().values()) {
			if (!createErrorBOMessage(exporterConfig, event, trade, e)) {
				String errorMessage = PositionExportEngineUtil.getErrorMessage(e);

				trade.addKeyword(MUREX_EXPORT_ERROR_KW,
						errorMessage.substring(0, Math.min(errorMessage.length(), 255)));
				trade.setAction(Action.AMEND);
				try {
					DSConnection.getDefault().getRemoteTrade().save(trade);
				} catch (CalypsoServiceException e1) {
					Log.error(LOG_CATEGORY, e1);
				}
				throw e;
			}

		}

	}

	protected boolean createErrorBOMessage(DataExporterConfig exporterConfig, PSEventInventorySecPosition sourceEvent,
			Trade trade, Exception e) {

		try {

			String messageType = exporterConfig.getProperty("messagetype") != null
					? exporterConfig.getProperty("messagetype").toString()
					: "GATEWAYMSG";
			String gateway = exporterConfig.getProperty("gateway") != null
					? exporterConfig.getProperty("gateway").toString()
					: "GATEWAYMSG";

			BOMessage message = GatewayUtil.createBOMessage(trade, (LegalEntity) null, (LegalEntity) null,
					(String) null, gateway, messageType);
			long allocatedSeed = CalypsoIDAPIUtil.allocateSeed(DSConnection.getDefault().getRemoteAccess(), "message",
					1);
			CalypsoIDAPIUtil.setAllocatedSeed(message, allocatedSeed);
			message.setAttribute("ObjectId", String.valueOf(trade.getLongId()));
			message.setAttribute("SourceName", this.getEngineName());
			message.setAttribute("OriginalEventId", String.valueOf(sourceEvent.getLongId()));
			CalypsoIDAPIUtil.setTradeId(message, trade.getLongId());
			message.setMessageType(messageType);
			message.setProductFamily(trade.getProductFamily());
			message.setTradeLongId(trade.getLongId());
			message.setProductType(trade.getProductType());
			message.setAction(AdviceDocUploaderXMLDataExporter.FAIL_SEND_ACTION);
			message.setAttribute(AdviceDocUploaderXMLDataExporter.ERROR_ATTRIBUTE, e.getMessage());
			message.setStatus(Status.S_SENT);

			String errorMessage = PositionExportEngineUtil.getErrorMessage(e);

			AdviceDocument aDocument = GatewayUtil.createAdviceDocument(message,
					"<Exception><![CDATA[" + errorMessage + "]]></Exception>");
			if (sourceEvent != null)
				aDocument.getAttributes().add(AdviceDocUploaderXMLDataExporter.SOURCE_EVENT_ATTRIBUTE, sourceEvent);
			long adviceDocId = DSConnection.getDefault().getRemoteBO().save(aDocument);
			message.setAttribute(AdviceDocUploaderXMLDataExporter.DATA_ADVICE_DOCID_EVENT_ATTRIBUTE,
					String.valueOf(adviceDocId));
			message.setAttribute(AdviceDocUploaderXMLDataExporter.UPLOAD_ADVICE_DOCID_EVENT_ATTRIBUTE,
					String.valueOf(adviceDocId));

			MessageArray messageArray = new MessageArray();
			messageArray.add(message);

			DSConnection.getDefault().getRemoteBO().saveMessages(0L, (String) null, messageArray, new TaskArray());

			return true;
		} catch (CalypsoException ex) {
			Log.error(LOG_CATEGORY, ex);
			return false;
		} catch (CalypsoServiceException ex) {
			Log.error(LOG_CATEGORY, ex);
			return false;
		} catch (RemoteException ex) {
			Log.error(LOG_CATEGORY, ex);
			return false;
		}
	}

	public boolean handlePSEventInventorySecPosition(PSEventInventorySecPosition event) {
		try {
			for (Pair<Trade, Trade> trades : getModifiedTradesToExport(event, true)) {
				exportTrade(PositionExportEngineUtil.getObjectToExport(event, trades));
			}
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void filterTradeKeywords(Trade trade) {

		Vector<String> keywords = LocalCache.getDomainValues(DSConnection.getDefault(),
				getEngineName() + "." + DV_EXPORT_TRADE_KEYWORDS);
		if (!Util.isEmpty(keywords)) {
			Enumeration<String> tradeKeyword = trade.getKeywords().keys();
			while (tradeKeyword.hasMoreElements()) {
				String keywordName = tradeKeyword.nextElement();
				if (!keywords.contains(keywordName))
					trade.removeKeyword(keywordName);
			}
		}
	}


	@Override
	public DUPAckProcessor getAckProcessor(IEAdapter adapter) {
		Object messageType = adapter.getIEAdapterConfig().getProperties().get("messagetype");
		Object gateway = adapter.getIEAdapterConfig().getProperties().get("gateway");

		MurexAckProcessor ackProcessor = new MurexAckProcessor();

		if (messageType != null) {
			ackProcessor.setMessageType(messageType.toString() + "_ACK");
		}

		if (gateway != null) {
			ackProcessor.setGateway(gateway.toString());
		}

		return ackProcessor;
	}

}
