package calypsox.tk.event;

import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoTrades;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class PledgeDataExporterAckEngineEventFilter extends DataExporterAckEngineEventFilter{


    /**
     *
     * @param event
     * @return True in case of being a trade ack event.
     * Pledge is the only product that uses DATAEXPORTERMSG and PSEventDataUploaderAck events by the moment.
     * If more trade product's are implemented in the future this method must be updated to filter out non Pledge trades.
     */
    @Override
    protected boolean acceptAckEvent(PSEventDataUploaderAck event){
        return Optional.ofNullable(event.getCalypsoDupAck()).map(CalypsoAcknowledgement::getCalypsoTrades)
                .map(CalypsoTrades::getCalypsoTrade).map(trades -> trades.get(0)).isPresent();
    }
}
