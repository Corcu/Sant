package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoRateIndex;
import com.calypso.tk.publish.jaxb.CalypsoRateIndexs;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class RateIndexAckEventFilter implements EventFilter {

    @Override
    public boolean accept(PSEvent event) {
        boolean res = true;
        if (event instanceof PSEventDataUploaderAck) {
            PSEventDataUploaderAck eventAck = (PSEventDataUploaderAck) event;
            CalypsoRateIndex calypsoRateIndex = Optional.ofNullable(eventAck.getCalypsoDupAck()).map(CalypsoAcknowledgement::getCalypsoRateIndexs)
                    .map(CalypsoRateIndexs::getCalypsoRateIndex).map(indexList -> indexList.get(0)).orElse(null);
            if (calypsoRateIndex == null) {
                res = false;
            }
        }
        return res;
    }
}