package calypsox.engine.task;

import com.calypso.engine.task.TaskEngine;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTask;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class SantTaskEngine extends TaskEngine {

    public SantTaskEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
        MailingTaskEngineListener mailingTaskEngineListener = new MailingTaskEngineListener();
        this.setTaskEngineListener(mailingTaskEngineListener);
    }

    @Override
    protected void handleEvent(PSEventTask event, Vector exceptions) throws Exception {
        if(_taskListener == null){
            MailingTaskEngineListener mailingTaskEngineListener = new MailingTaskEngineListener();
            this.setTaskEngineListener(mailingTaskEngineListener);
        }
        super.handleEvent(event, exceptions);
    }
}
