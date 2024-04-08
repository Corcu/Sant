package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;

public class BankHolidayNotice extends EmailDataBuilder {
    public BankHolidayNotice(BOMessage message) {
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
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        return "Bank Holiday Notice - " + po;
    }


}
