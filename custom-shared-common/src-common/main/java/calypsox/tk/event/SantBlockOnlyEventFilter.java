package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;


public class SantBlockOnlyEventFilter implements EventFilter {
	@Override
	public boolean accept(PSEvent event) {
		SantOnlyEFUtil santOnlyEF = new SantOnlyEFUtil(event, true);
		return santOnlyEF.accept();
	}
}
