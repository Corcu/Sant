package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;

public class QuarterlyNoticeIMSegregation extends EmailDataBuilder {
    public QuarterlyNoticeIMSegregation(BOMessage message) {
        super(message);
        setFileAttached(true);
        setFileName("Quarterly Notice IM Segregation Santander.html");
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

        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        String cpty = EmailDataBuilderUtil.getInstance().getCptyName(message);

        return po + " - " + "IM Segregation Quarterly Notice" + " - " + cpty;
    }

    private String buildBody() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html>");
        sb.append("<html>")
        .append("<head>")
        .append("<style></style>")
        .append("</head>")
        .append("<body>")
        .append("Dear Counterparty,\n" +
                "Please find enclosed Quarterly informative Notification related to the requirements for non-segregated margin, pursuant to the CFTC Rule 23.704.\n")
        .append("<br><br>")
        .append(EmailDataBuilderUtil.getInstance().getNotificationSign())
        .append("</body>")
        .append("</html>");
        return sb.toString();
    }
}
