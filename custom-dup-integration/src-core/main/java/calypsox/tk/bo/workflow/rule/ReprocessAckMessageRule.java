package calypsox.tk.bo.workflow.rule;

import java.rmi.RemoteException;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.util.InstantiateUtil;

import calypsox.tk.export.AdviceDocUploaderXMLDataExporter;
import calypsox.tk.export.ack.TradeAckProcessor;

public class ReprocessAckMessageRule implements WfMessageRule {
	
	public static String LOG_CATEGORY = "ReprocessAckMessageRule";
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Reprocess ack message for the PositionExportEngine";
	}
	
	public TradeAckProcessor getAckProcessor(BOMessage message) {
		String processorClassName = message.getAttribute(TradeAckProcessor.ACK_PROCESSOR_ATTRIBUTE);
		
		try {
			TradeAckProcessor processor = (TradeAckProcessor)InstantiateUtil.getInstance(processorClassName);
			processor.setGateway(message.getGateway());
			processor.setMessageType(message.getMessageType());
			return processor;
		} catch (InstantiationException e) {
			Log.error(LOG_CATEGORY, e);
		} catch (IllegalAccessException e) {
			Log.error(LOG_CATEGORY, e);
		}
		
		return null;
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		
		String adviceDocId = message.getAttribute(AdviceDocUploaderXMLDataExporter.DATA_ADVICE_DOCID_EVENT_ATTRIBUTE);

		try {
			AdviceDocument adviceDocument = CalypsoIDAPIUtil.getAdviceDocument(DSConnection.getDefault().getRemoteBO(), Long.parseLong(adviceDocId));
			TradeAckProcessor ackProcessor = this.getAckProcessor(message);
			ackProcessor.doProcess(adviceDocument.getDocument().toString());
			ackProcessor.updateAckMessage(message);
			messages.add(ackProcessor.getErrorMessagesAsString());			
			if(ackProcessor.getErrorMessages().size()>0)
				message.setAttribute(TradeAckProcessor.ERRORS_ATTRIBUTE, ackProcessor.getErrorMessagesAsString());
			message.setAttribute(TradeAckProcessor.ERROR_ATTRIBUTE, ""+ackProcessor.isError());
			if(ackProcessor.isError()) {
				messages.add(ackProcessor.getErrorMessagesAsString());
			}
			return !ackProcessor.isError();
		} catch (NumberFormatException e) {
			excps.add(e);
			Log.error(LOG_CATEGORY, e);
			return false;
		} catch (RemoteException e) {
			excps.add(e);
			Log.error(LOG_CATEGORY, e);
			return false;
		}
	}


}
