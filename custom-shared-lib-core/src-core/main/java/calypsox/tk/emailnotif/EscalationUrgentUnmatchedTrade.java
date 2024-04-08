package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;

public class EscalationUrgentUnmatchedTrade extends UrgentUnmatchedTrade {
    public EscalationUrgentUnmatchedTrade(BOMessage message) {
        super(message);
        setFileAttached(false);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    private String buildSubject() {
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        String cpty = EmailDataBuilderUtil.getInstance().getCptyName(message);
        return "**ESCALATION**URGENT**** Unmatched Trade " + poName.toUpperCase() + " vs " + cpty + " - " + message.getLongId();
    }
}
