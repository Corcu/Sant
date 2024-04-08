package calypsox.tk.event;

import com.calypso.tk.bo.Task;
import com.calypso.tk.event.PSEvent;

/**
 * @author aalonsop
 */
public class PSEventMailingAlert extends PSEvent {
    private Task task;

    public PSEventMailingAlert(){
        this.setEngineName("MailingAlertEngine");
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
