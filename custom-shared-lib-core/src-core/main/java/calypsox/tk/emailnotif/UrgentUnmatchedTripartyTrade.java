package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;

public class UrgentUnmatchedTripartyTrade  extends EmailDataBuilder {


    public UrgentUnmatchedTripartyTrade(BOMessage message) {
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

        return "****URGENT**** Unmatched Triparty Trade with " + poName.toUpperCase() + " - " + message.getLongId();
    }
}
