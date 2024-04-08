/**
 * 
 */
package calypsox.tk.event;

import com.calypso.tk.core.Trade;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;

/**
 * Accept trades with CHECKED status
 * 
 * @author aela
 * 
 */
public class SantOnlyCheckedTradesEventFilter implements EventFilter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.event.EventFilter#accept(com.calypso.tk.event.PSEvent)
	 */
	@Override
	public boolean accept(PSEvent event) {
		if (event instanceof PSEventTrade) {
			Trade trade = ((PSEventTrade) event).getTrade();
			if ((trade != null) && "CHECKED".equals(trade.getStatus().toString())) {
				return true;
			}
		}
		return false;
	}

}
