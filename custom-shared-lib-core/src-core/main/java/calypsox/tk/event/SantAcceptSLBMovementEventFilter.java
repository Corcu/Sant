/**
 * 
 */
package calypsox.tk.event;

import java.util.List;

import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;

/**
 * @author fperezur
 *
 */
public class SantAcceptSLBMovementEventFilter extends SenderEngineEventFilter {
	
	/**
	 * 
	 */
	public SantAcceptSLBMovementEventFilter() {
	}


	public boolean accept(PSEvent psevent) {
		if ((psevent instanceof PSEventMessage)) {
			PSEventMessage pseventmessage = (PSEventMessage) psevent;
			BOMessage boMessage = pseventmessage.getBoMessage();

			List<String> domValues = CollateralUtilities
					.getDomainValues(SantDomainValuesUtil.SLB_MOV_SENDER_ENG_MSG_TYPE);
			String msgType = boMessage.getMessageType();
			
			if (domValues != null && msgType != null && !domValues.isEmpty() && domValues.contains(msgType)) {
				return super.accept(psevent);
			}
		}

		return false;
	}

}
