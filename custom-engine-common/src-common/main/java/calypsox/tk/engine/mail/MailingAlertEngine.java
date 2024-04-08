package calypsox.tk.engine.mail;

import calypsox.tk.event.PSEventMailingAlert;
import com.calypso.engine.Engine;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;


/**
 * @author aalonsop
 * Send emails for the given input PSEventMailingAlert events
 */
public class MailingAlertEngine extends Engine {

    MailingEngineHandler mailingHandler;
    public MailingAlertEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        this.mailingHandler=new MailingEngineHandler();
    }

    @Override
    public boolean process(PSEvent event) {
        if(event instanceof PSEventMailingAlert){
            processMailingEvent((PSEventMailingAlert) event);
        }
        return true;
    }

    public void processMailingEvent(PSEventMailingAlert mailingEvent){
        try {
            this.mailingHandler.processEvent(mailingEvent);
            this._ds.getRemoteTrade().eventProcessed(mailingEvent.getLongId(), this.getEngineName());
        } catch (CalypsoServiceException exc) {
            Log.error(this.logCategory,"Error while processing MailingEvent",exc.getCause());
        }
    }
}
