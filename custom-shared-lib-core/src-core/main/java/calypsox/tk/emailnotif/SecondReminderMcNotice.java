package calypsox.tk.emailnotif;

import calypsox.tk.bo.MarginCallMessageFormatter;
import com.calypso.tk.bo.BOMessage;

public class SecondReminderMcNotice extends FirstReminderMcNotice {

    SecondReminderMcNotice(BOMessage message) {
        super(message);
        setFileAttached(true);
        setFileName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_NOTICE);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    private String buildSubject() {

        String mccName = EmailDataBuilderUtil.getInstance().getMccName(getMessage());
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        Integer mccId = EmailDataBuilderUtil.getInstance().getMccId(message);
        return "*********2nd REMINDER********* Margin Call Notice " + mccName + " - " + po + " - " + mccId;
    }

}
