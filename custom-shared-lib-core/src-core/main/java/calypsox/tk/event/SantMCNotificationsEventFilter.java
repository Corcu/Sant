/**
 * 
 */
package calypsox.tk.event;

import calypsox.util.MarginCallNotifType;

import com.calypso.tk.core.Log;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventStatement;

/**
 * Filter to use with the SantMCNotificationEngine to prevent re-consuming event just typed by this engine 
 * 
 * @author aela
 *
 */
public class SantMCNotificationsEventFilter implements EventFilter {

	/* (non-Javadoc)
	 * @see com.calypso.tk.event.EventFilter#accept(com.calypso.tk.event.PSEvent)
	 */
	@Override
	public boolean accept(PSEvent event) {
		if (event instanceof PSEventStatement) {
			try {
				PSEventStatement statament=(PSEventStatement)event;
				MarginCallNotifType type = MarginCallNotifType.valueOf(statament.getMessageType());
				if(type !=null) {
					return false;
				}
				return true;
			} catch (IllegalArgumentException e) {
				Log.info(this, e); //sonar
				// that means that the message type is not set yet so it should be consumed
				return true;
			}
		} 
		return false;
	}

}
