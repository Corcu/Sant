package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;

public class PledgeAcceptReminderMcNotice extends EmailDataBuilder {
    public PledgeAcceptReminderMcNotice(BOMessage message) {
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

        String mccName = EmailDataBuilderUtil.getInstance().getMccName(getMessage());
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        String ampid = EmailDataBuilderUtil.getInstance().getAcadiaAmpId(message);

        return "*****PLEDGE ACCEPT REMINDER******* Margin Call Notice " + mccName + " - " + po + " - " + ampid;
    }

}
