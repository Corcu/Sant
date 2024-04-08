package calypsox.engine;

import calypsox.tk.event.PSEventDataUploaderAck;
import calypsox.tk.export.ack.DUPAckProcessor;
import calypsox.tk.export.ack.PledgeACKProcessor;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.util.IEAdapter;

import java.rmi.RemoteException;

/**
 * @author aalonsop
 */
public class PledgeExportEngine extends MultipleDestinationExportEngine{


    public PledgeExportEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        this.format="PledgeUploaderXML";
        this.engineName=this.getClass().getSimpleName();
    }

    @Override
    public boolean process(PSEvent event) {
        boolean res=true;
        if(event instanceof PSEventMessage){
            handleMessageEvent((PSEventMessage) event);
            processEvent(event);
        }else if(event instanceof PSEventDataUploaderAck){
            ((PledgeACKProcessor)this.getAckProcessor(null))
                    .processAckEvent(((PSEventDataUploaderAck) event).getCalypsoDupAck());
            processEvent(event);
        }else{
            res=super.process(event);
        }
        return res;
    }

    private void handleMessageEvent(PSEventMessage messageEvent) {
        BOMessage boMessage = messageEvent.getBoMessage();
        boMessage.setAttribute("ObjectType","Trade");
        boMessage.setAttribute("ObjectId",String.valueOf(boMessage.getTradeLongId()));
        super.exportTrade(boMessage);
    }

    @Override
    public DUPAckProcessor getAckProcessor(IEAdapter adapter) {
        return new PledgeACKProcessor();
    }


    public void processEvent(PSEvent event){
        try {
            CalypsoIDAPIUtil.eventProcessed(DSConnection.getDefault().getRemoteTrade(), CalypsoIDAPIUtil.getId(event), this.getEngineName());
        } catch (RemoteException exc) {
            Log.error(this,exc.getCause());
        }
    }
}
