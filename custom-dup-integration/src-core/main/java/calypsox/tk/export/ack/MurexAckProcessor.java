package calypsox.tk.export.ack;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.upload.ack.CollateralTransferReturnStatus;
import calypsox.tk.util.PositionExportEngineUtil;

public class MurexAckProcessor extends TradeAckProcessor {

	public static final String KW_MUREX_ID = "MxID";
	
	public static final Object LOG_CATEGORY = "MurexAckProcessor";
	
	public Trade trade = null;
	public BOMessage boMessage = null;
	
	public static JAXBContext jaxbContext =null;
	public Unmarshaller jaxbUnmarshaller =null;
	
	static {
		try {
			jaxbContext= JAXBContext.newInstance(CollateralTransferReturnStatus.class);
		} catch (JAXBException e) {
			Log.error(LOG_CATEGORY, e);
		}
	}
	
	public MurexAckProcessor() {
		try {
			jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			Log.error(LOG_CATEGORY, e);
		}
	}
	
	public boolean doProcess(CollateralTransferReturnStatus ackStatus) {
		if(ackStatus!=null) {
			try {
				boMessage = DSConnection.getDefault().getRemoteBO().getMessage(ackStatus.getMessageId());
				if(boMessage==null) {
					Log.error(LOG_CATEGORY, "Murex Ack Message not found :" + ackStatus.getMessageId());
					return false;
				}
				trade = DSConnection.getDefault().getRemoteTrade().getTrade(boMessage.getTradeLongId());
				boolean result = updateTradeWithFOId(boMessage, ackStatus);
				if(!result)
					return false;
				String action = ackStatus.getStatus();
				boMessage.setAction(Action.valueOf(action));
				if(!Util.isEmpty(ackStatus.getErrorDescription())) {
					String trimmedString = ackStatus.getErrorDescription();
					if(trimmedString.length()>255)
						trimmedString = trimmedString.substring(0,255);
					boMessage.setAttribute(ERRORS_ATTRIBUTE, trimmedString);
				}
				result = saveBOMessage(boMessage);
				return result;
			} catch (Exception e) {
				addError(e);
				Log.error(LOG_CATEGORY, e);
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String getAckMessageDescription() {
		return "Murex Ack Message";
	}
	
	public boolean doProcess(ExternalMessage message) {

		CollateralTransferReturnStatus ackStatus = getAckStatus(message);
		return doProcess(ackStatus);

	}
	
	protected boolean updateTradeWithFOId(BOMessage message, CollateralTransferReturnStatus ackStatus) {
		try {
			if(ackStatus.getFoId()==0)
				return true;
			
			if(trade==null) {
				addError("No trade found to update " + KW_MUREX_ID);
				return false;
			}
			
			if(!PositionExportEngineUtil.isTriparty(trade)) {
				Trade existingTrade = PositionExportEngineUtil.findExistingMurexTrade(DSConnection.getDefault(), trade );
				
				if(existingTrade!=null && existingTrade.getLongId()!=trade.getLongId()) {
					existingTrade.removeKeyword(KW_MUREX_ID);
					existingTrade.setAction(Action.AMEND);
					DSConnection.getDefault().getRemoteTrade().save(existingTrade);
				}
				
				trade.addKeyword(KW_MUREX_ID, ""+ackStatus.getFoId());
				trade.setAction(Action.AMEND);
				DSConnection.getDefault().getRemoteTrade().save(trade);
				
				
			}
			

			
		} catch (Exception e) {
			addError(e);
			Log.error(LOG_CATEGORY, e);
			return false;
		}
		return true;
	}

	@Override
	public boolean doProcess(Object o) {
		if(o instanceof ExternalMessage) {
			return doProcess((ExternalMessage)o);
		}
		if(o instanceof CollateralTransferReturnStatus)
			return doProcess((CollateralTransferReturnStatus)o);
		
		if(o instanceof String) {
			return doProcess((CollateralTransferReturnStatus)getAckStatus((String)o));
		}
		
		return false;
	}
	
	
	protected CollateralTransferReturnStatus getAckStatus(String xmlString) {
		if(xmlString==null) {
			addError("empty message");
			Log.error(LOG_CATEGORY, "empty message");
			return null;
		}
		
		try {
			return (CollateralTransferReturnStatus) jaxbUnmarshaller.unmarshal(new StringReader(xmlString));
		} catch (JAXBException e) {
			addError(e);
			Log.error(LOG_CATEGORY,  e);
		}
		
		return null;
	}
	
	protected CollateralTransferReturnStatus getAckStatus(ExternalMessage message) {
		String xmlString = message.getText();
		return getAckStatus(xmlString);
	}

	@Override
	public Trade getTrade() {
		return trade;
	}

	@Override
	public BOMessage getOriginalMessage() {
		return boMessage;
	}

}
