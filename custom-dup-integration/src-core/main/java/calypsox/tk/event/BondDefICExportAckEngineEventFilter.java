package calypsox.tk.event;

import java.util.Optional;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoProducts;

public class BondDefICExportAckEngineEventFilter implements EventFilter{
    public boolean accept(PSEvent event) {
    	if (event instanceof PSEventProduct) {
    		return true;
    	}
    	else if (event instanceof PSEventDataUploaderAck) {
    		PSEventDataUploaderAck eventDUPAck = (PSEventDataUploaderAck)event;
    		return Optional.ofNullable(eventDUPAck.getCalypsoDupAck()).map(CalypsoAcknowledgement::getCalypsoProducts)
                    .map(CalypsoProducts::getCalypsoProduct).map(products -> products.get(0)).isPresent();
    	}
    	
    	return false;
    }
}
