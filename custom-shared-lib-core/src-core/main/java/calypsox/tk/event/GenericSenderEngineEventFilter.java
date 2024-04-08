package calypsox.tk.event;

import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;

import java.util.List;

/**
 * @author
 *
 */
public class GenericSenderEngineEventFilter extends SenderEngineEventFilter {

	public GenericSenderEngineEventFilter() {
	}
	
	public boolean accept(PSEvent psevent) {
		if ((psevent instanceof PSEventMessage)) {
			PSEventMessage pseventmessage = (PSEventMessage) psevent;
			BOMessage boMessage = pseventmessage.getBoMessage();

			try {
				List<String> domNames = CollateralUtilities.getDomainValues(SantDomainValuesUtil.SENDER_ENG_MSG_TYPE);

				String msgType = null!=boMessage ? boMessage.getMessageType() : "";
				if(!Util.isEmpty(msgType) && !Util.isEmpty(domNames)){
					for(String value : domNames){
						if(value.equalsIgnoreCase(msgType)){
							return super.accept(psevent);
						}
					}
				}
			} catch (Exception e) {
				Log.error(this, e);
			}
		}

		return false;
	}

}
