package calypsox.engine;

import calypsox.ctm.camel.CTMRouteBuilder;
import calypsox.ctm.rx.RxAllocationAdapter;
import calypsox.ctm.rx.RxDatauploaderAdapter;
import calypsox.ctm.rx.RxIONAckAdapter;
import calypsox.tk.camel.AbstractCamelRouteBuilder;
import com.calypso.tk.event.*;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CTMConnectionEngine extends RXDataUploaderConnectionEngine{


    public CTMConnectionEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    @Override
    public boolean process(PSEvent event) {
        boolean res=true;
       if(event instanceof PSEventTrade){
            res=processPSEventTrade((PSEventTrade) event);
        }else if(event instanceof PSEventMessage){
            res=processIONCompletedAckEvent((PSEventMessage) event);
        }else{
           super.process(event);
       }
        return res;
    }

    private boolean processIONCompletedAckEvent(PSEventMessage event){
        return Optional.of(event)
                .map(ev -> RxIONAckAdapter.sendIONCompletedAck(ev,this.getEngineName()))
                .orElse(true);
    }


    private boolean processPSEventTrade(PSEventTrade event) {
        Optional.of(event)
                .ifPresent(ev ->{
                    RxAllocationAdapter.reprocessPendingAllocationMsgs(ev.getTrade());
                    RxDatauploaderAdapter.markEventAsProcessed(this.getEngineName(),ev.getLongId());
                });
        return true;
    }

    @Override
    public AbstractCamelRouteBuilder getCamelRouteBuilder() {
        return new CTMRouteBuilder();
    }
}
