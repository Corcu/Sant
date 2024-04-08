package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class TripartyMarginCallNotice extends EmailDataBuilder {


    public TripartyMarginCallNotice(BOMessage message) {
        super(message);
        setFileAttached(true);
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
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);
        String mccName = EmailDataBuilderUtil.getInstance().getMccName(message);
        Integer mccId = EmailDataBuilderUtil.getInstance().getMccId(message);

        return "Triparty Margin Call Notice " + mccName + " - " + poName + " - " + mccId;
    }

    private String buildBody() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<style></style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<br>");
        sb.append(EmailDataBuilderUtil.getInstance().getNotificationSign());
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }
}
