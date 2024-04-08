package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;
import java.util.Vector;

public class MT564beforePaymentDateMessageRule implements WfMessageRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        String paymentDate = message.getAttribute("Payment_Date");
        if (message.getTemplateName().equalsIgnoreCase("MT564")) {
            if (paymentDate != null) {
                JDate paymentDateJD = Util.istringToJDate(paymentDate);
                JDate creationDateJD = message.getCreationDate().getJDate(TimeZone.getDefault());
                return !creationDateJD.after(paymentDateJD);
            }
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "When Creation Date of the the message is after Payment Date, return false (only for MT564)";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}
