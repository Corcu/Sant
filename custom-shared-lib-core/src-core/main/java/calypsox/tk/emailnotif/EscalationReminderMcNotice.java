package calypsox.tk.emailnotif;

import calypsox.tk.bo.MarginCallMessageFormatter;
import com.calypso.tk.bo.BOMessage;

public class EscalationReminderMcNotice extends SecondReminderMcNotice {
    EscalationReminderMcNotice(BOMessage message) {
        super(message);
        setFileName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_NOTICE);
        setFileAttached(true);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    private String buildSubject() {

        String mccName = EmailDataBuilderUtil.getInstance().getMccName(getMessage());
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        Integer mccId = EmailDataBuilderUtil.getInstance().getMccId(message);
        return "**ESCALATION**REMINDER********* Margin Call Notice " + mccName + " - " + po + " - " + mccId;
    }
}
