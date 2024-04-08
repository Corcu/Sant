package calypsox.tk.emailnotif;

import calypsox.tk.bo.MarginCallMessageFormatter;
import com.calypso.tk.bo.BOMessage;

public class EscalationReminderInterestStatement extends MarginCallInterestStatement {
    public EscalationReminderInterestStatement(BOMessage message) {
        super(message);
        setFileAttached(true);
        setFileName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_INTEREST_STATEMENT_NOTICE);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    private String buildSubject() {

        String mccName = EmailDataBuilderUtil.getInstance().getMccName(getMessage());
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        return "**ESCALATION**REMINDER********* Interest Statement " + mccName + " - " + po;
    }
}
