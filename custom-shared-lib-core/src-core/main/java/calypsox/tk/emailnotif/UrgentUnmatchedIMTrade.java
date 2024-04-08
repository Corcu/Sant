package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;

public class UrgentUnmatchedIMTrade extends EmailDataBuilder {


    public UrgentUnmatchedIMTrade(BOMessage message) {
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

        return "****URGENT**** Unmatched IM Trade with " + poName.toUpperCase() + " - " + message.getLongId();
    }
}
