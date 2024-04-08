package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ReminderSetAttributeMessageRule implements WfMessageRule {


    private List<String> messageTypes = Arrays.asList("MC_INTEREST", "MC_NOTIFICATION");

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Set reminder attribute to notifications messages";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        if (!messageTypes.contains(message.getMessageType())) {
            return true;
        }
        if (message.getAction().equals(Action.RESEND)) {
            String reminder = getReminderAttribute(message);
            if (Util.isEmpty(reminder)) {
                message.setAttribute("Reminder", "FirstReminder");
            } else if ("FirstReminder".equals(reminder)) {
                message.setAttribute("Reminder", "SecondReminder");
            }
        }
        return true;
    }

    private String getReminderAttribute(BOMessage message) {
        String reminder = message.getAttribute("Reminder");
        return !Util.isEmpty(reminder) ? reminder : null;
    }
}
