package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;


public class SantPassOnlyEventFilter implements EventFilter {
	@Override
	public boolean accept(PSEvent event) {
		SantOnlyEFUtil santOnlyEF = new SantOnlyEFUtil(event, false);
		return santOnlyEF.accept();
	}
}
