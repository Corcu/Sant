package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;

public class EscalationUrgentUnmatchedTripartyTrade extends UrgentUnmatchedTripartyTrade {
    public EscalationUrgentUnmatchedTripartyTrade(BOMessage message) {
        super(message);
        setFileAttached(false);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    private String buildSubject() {
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        return "**ESCALATION**URGENT**** Unmatched Triparty Trade with " + poName.toUpperCase() + " - " + message.getLongId();
    }
}
