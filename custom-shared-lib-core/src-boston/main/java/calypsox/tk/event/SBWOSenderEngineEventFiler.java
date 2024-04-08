package calypsox.tk.event;

import java.util.List;
import java.util.Vector;

import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;

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
public class SBWOSenderEngineEventFiler extends SenderEngineEventFilter {
	public static final String TOPIC = "TOPIC";
	
	/**
	 * 
	 */
	public SBWOSenderEngineEventFiler() {
	}

	public boolean accept(PSEvent psevent) {
		if ((psevent instanceof PSEventMessage)) {
			PSEventMessage pseventmessage = (PSEventMessage) psevent;
			BOMessage boMessage = pseventmessage.getBoMessage();

			List<String> domValues = CollateralUtilities
					.getDomainValues(SantDomainValuesUtil.NOTIF_SENDER_ENG_MSG_TYPE);
			String msgType = boMessage.getMessageType();
			
			String topic = getTopicAttribute(boMessage);
			if (checkIfFormateUsConfigure(topic)) {
				if (domValues != null && msgType != null && !domValues.isEmpty() && domValues.contains(msgType)) {
					return super.accept(psevent);
				}
			}
		}
		return false;
	}

	
	private boolean checkIfFormateUsConfigure(String topic){
		Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), "Notification.US");
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
				if (TOPIC.equals(legalEntityAttribute.getAttributeType())) {
					return legalEntityAttribute.getAttributeValue();
				}
			}
		}
		return rst;
	}
}