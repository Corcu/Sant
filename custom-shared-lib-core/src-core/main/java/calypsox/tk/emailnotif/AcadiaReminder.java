package calypsox.tk.emailnotif;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.FormatterUtil;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.service.DSConnection;


public class AcadiaReminder extends EmailDataBuilder {

    public AcadiaReminder(BOMessage message) {
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
        String acadiaAmpId = EmailDataBuilderUtil.getInstance().getAcadiaAmpId(message);
        return "*****ACADIA REMINDER******* Margin Call Notice " + mccName + " - " + po + " - " + acadiaAmpId;
    }


}
