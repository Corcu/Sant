package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.TemplateSelector;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Trade;

public class NotificationsTemplateSelector implements TemplateSelector {
    @Override
    public String getTemplate(Trade trade, BOMessage message, String name) {

        if (null != trade) {
            if (message.getMessageType().equals("MC_INTEREST")) {
                if (trade.getAction().equals(Action.valueOf("REMINDER"))) {
                    return "FirstReminderInterestStatement.html";
                } else if (trade.getAction().equals(Action.valueOf("2ND_REMINDER"))) {
                    return "SecondReminderInterestStatement.html";
                }else{
                    return "MarginCallInterestStatement.htm";
                }
            }
        }
        return name;
    }
}
