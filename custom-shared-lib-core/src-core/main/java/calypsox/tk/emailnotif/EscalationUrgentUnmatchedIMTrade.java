package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;

public class EscalationUrgentUnmatchedIMTrade extends UrgentUnmatchedIMTrade {
    public EscalationUrgentUnmatchedIMTrade(BOMessage message) {
        super(message);
        setFileAttached(false);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    private String buildSubject() {
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        return "**ESCALATION**URGENT**** Unmatched IM Trade with " + poName.toUpperCase() + " - " + message.getLongId();
    }
}
