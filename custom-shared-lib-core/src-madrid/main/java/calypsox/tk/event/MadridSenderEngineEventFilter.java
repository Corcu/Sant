package calypsox.tk.event;

import java.util.List;
import java.util.Vector;



import calypsox.tk.util.SantDomainValuesUtilMadrid;
import calypsox.util.CollateralUtilitiesMadrid;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * @author jriquell
 *
 */
public class MadridSenderEngineEventFilter extends SenderEngineEventFilter {
	public static final String TOPIC = "TOPIC";
	public static final String ACADIA = "ACADIA";
	public static final String FILENET = "MCNOTIFFileNet";
	
	/**
	 * 
	 */
	public MadridSenderEngineEventFilter() {
	}

	public boolean accept(PSEvent psevent) {
		if ((psevent instanceof PSEventMessage)) {
			PSEventMessage pseventmessage = (PSEventMessage) psevent;
			BOMessage boMessage = pseventmessage.getBoMessage();
			
			List<String> domValues = CollateralUtilitiesMadrid
					.getDomainValues(SantDomainValuesUtilMadrid.NOTIF_SENDER_ENG_MSG_TYPE);
			String msgType = boMessage.getMessageType();

			String topic = getTopicAttribute(boMessage);
			if (!isAcadiaMessage(boMessage)&&checkIfFormateSpainConfigure(topic) && !isFilenetMessage(boMessage)) {
				if (domValues != null && msgType != null && !domValues.isEmpty() && domValues.contains(msgType)) {
					return super.accept(psevent);
				}
			}
		}
		return false;
	}
	
	public boolean isAcadiaMessage(BOMessage boMessage) {
		if(boMessage.getGateway()==null)
			return false;
		return boMessage.getGateway().equals(ACADIA);	
	}
	
	public boolean isFilenetMessage(BOMessage boMessage) {
		if(boMessage.getGateway()==null)
			return false;
		return boMessage.getGateway().equals(FILENET);
	}
	
	private boolean checkIfFormateSpainConfigure(String topic){
		Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), "Notification.ES");
		if(domainValues != null){
			for(String valueDomain: domainValues){
				if(valueDomain.equalsIgnoreCase(topic)){
					return true;
				}
			}
		}
		return false;		
	}

	
	public static String getTopicAttribute(BOMessage boMessage) {
		String rst = "";
		Vector<LegalEntityAttribute> attrs = BOCache.getLegalEntityAttributes(DSConnection.getDefault(), boMessage.getSenderId());
		if (!Util.isEmpty(attrs)) {
			for (LegalEntityAttribute legalEntityAttribute : attrs) {
				if ("TOPIC".equals(legalEntityAttribute.getAttributeType())) {
					return legalEntityAttribute.getAttributeValue();
				}
			}
		}
		return rst;
	}
}