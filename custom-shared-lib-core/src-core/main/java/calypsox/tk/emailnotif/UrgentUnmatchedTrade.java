package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;

public class UrgentUnmatchedTrade extends EmailDataBuilder {


    public UrgentUnmatchedTrade(BOMessage message) {
        super(message);
        setFileAttached(false);
    }

    @Override
    public String getSubject() {
        return buildSubject();
    }

    @Override
    public String getBody() {
        return null;
    }

    private String buildSubject() {
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        String cpty = EmailDataBuilderUtil.getInstance().getCptyName(message);
        return "****URGENT**** Unmatched Trade " + poName.toUpperCase() + " vs " + cpty + " - " + message.getLongId();
    }
}
