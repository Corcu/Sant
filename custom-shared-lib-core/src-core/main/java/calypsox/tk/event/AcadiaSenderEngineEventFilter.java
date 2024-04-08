package calypsox.tk.event;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;

import java.util.Optional;

/**
 * @author jriquell
 *
 */
public class AcadiaSenderEngineEventFilter extends SenderEngineEventFilter {
	public static final String ACADIA = "ACADIA";
	
	/**
	 * 
	 */
	public AcadiaSenderEngineEventFilter() {
	}

	public boolean accept(PSEvent psevent) {
		if ((psevent instanceof PSEventMessage)) {
			PSEventMessage pseventmessage = (PSEventMessage) psevent;
			BOMessage boMessage = pseventmessage.getBoMessage();

			if (isAcadiaMessage(boMessage)) {
				return super.accept(psevent);
			}
		}
		return false;
	}
	
	public boolean isAcadiaMessage(BOMessage boMessage) {
		return Optional.ofNullable(boMessage).map(BOMessage::getGateway).filter(ACADIA::equalsIgnoreCase).isPresent();
	}
}