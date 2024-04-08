package calypsox.tk.event;

import calypsox.ctm.model.IONTradeAck;
import com.calypso.tk.event.PSEvent;

/**
 * @author aalonsop
 */
public class PSEventIONAck extends PSEvent {

    private final IONTradeAck ionAckData;

    public PSEventIONAck(IONTradeAck ionAckData){
        this.ionAckData=ionAckData;
    }

    public IONTradeAck getIonAckData() {
        return ionAckData;
    }

}
