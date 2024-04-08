package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;

public class NoActionCollateralConfirm extends EmailDataBuilder {
    public NoActionCollateralConfirm(BOMessage message) {
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
        Integer mccId = EmailDataBuilderUtil.getInstance().getMccId(message);
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        return "No Action Collateral Confirmation " + mccName + " - " + poName + " - " + mccId;

    }
}
