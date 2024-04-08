package calypsox.tk.emailnotif;

import calypsox.tk.bo.MarginCallMessageFormatter;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.FormatterUtil;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.AdviceDocumentBuilder;
import com.calypso.tk.bo.document.util.AdviceDocumentUtil;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import org.jfree.util.Log;

public class InitialMarginCallNotice extends EmailDataBuilder {


    public InitialMarginCallNotice(BOMessage message) {
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

        String mccName = EmailDataBuilderUtil.getInstance().getMccName(getMessage());
        Integer mccId = EmailDataBuilderUtil.getInstance().getMccId(message);
        String poName = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        return "Initial Margin Call Notice " + mccName + " - " + poName + " - " + mccId;

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
