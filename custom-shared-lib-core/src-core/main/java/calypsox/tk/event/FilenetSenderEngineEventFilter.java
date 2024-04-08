package calypsox.tk.event;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;

/**
 * 
 * @author x957355
 *
 */
public class FilenetSenderEngineEventFilter extends SenderEngineEventFilter {

	public static final String FILENET_GATEWAY = "MCNOTIFFileNet";

	public FilenetSenderEngineEventFilter() {
	}

	public boolean accept(PSEvent psevent) {
		if ((psevent instanceof PSEventMessage)) {

			PSEventMessage pseventmessage = (PSEventMessage) psevent;
			BOMessage boMessage = pseventmessage.getBoMessage();

			if (isFilenetMessage(boMessage)) {
				return super.accept(pseventmessage);
			}
		}
		return false;
	}

	public boolean isFilenetMessage(BOMessage boMessage) {
		if (boMessage.getGateway() == null)
			return false;
		return boMessage.getGateway().equals(FILENET_GATEWAY);
	}
}
