package calypsox.tk.emailnotif;

import calypsox.tk.bo.MarginCallMessageFormatter;
import com.calypso.tk.bo.BOMessage;


public class FirstReminderMcNotice extends EmailDataBuilder {

    public FirstReminderMcNotice(BOMessage message) {
        super(message);
        setFileAttached(true);
        setFileName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_NOTICE);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    @Override
    public String getBody() {
        return buildBody();
    }

    private String buildSubject() {

        String mccName = EmailDataBuilderUtil.getInstance().getMccName(getMessage());
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        Integer mccId = EmailDataBuilderUtil.getInstance().getMccId(message);

        return "*********REMINDER********* Margin Call Notice " + mccName + " - " + po + " - " + mccId;
    }

    private String buildBody() {

        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        StringBuilder sb = new StringBuilder("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<style></style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("Please find attached a Margin Call Notice from ").append(po).append("<br>").append("<br>")
                .append(EmailDataBuilderUtil.getInstance().getNotificationSign());
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }

}
