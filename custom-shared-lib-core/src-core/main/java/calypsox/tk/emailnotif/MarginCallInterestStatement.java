package calypsox.tk.emailnotif;

import calypsox.tk.bo.MarginCallMessageFormatter;
import calypsox.tk.bo.notification.SantInterestNotificationCache;
import calypsox.util.MarginCallConstants;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.FormatterUtil;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.service.DSConnection;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class MarginCallInterestStatement extends EmailDataBuilder {
    public static final MessageFormat noticeSubject = new MessageFormat("{0} {1} - {2}");


    public MarginCallInterestStatement(BOMessage message) {
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

        String templateTitle = message.getAttribute(MarginCallConstants.TRANS_MESSAGE_ATTR_TEMP_TITLE);
        if (Util.isEmpty(templateTitle)) {
            templateTitle = MarginCallMessageFormatter.getTitleFromTemplateName(message, DSConnection.getDefault());
        }
        templateTitle = getReminderAttribute(message) + templateTitle;
        message.setAttribute(MarginCallConstants.TRANS_MESSAGE_ATTR_TEMP_TITLE, null);

        MarginCallConfig mcc = EmailDataBuilderUtil.getInstance().getMcc(message);
        String po = EmailDataBuilderUtil.getInstance().getProcessingOrgName(message);

        if (message.getTemplateName()
                .equals(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_INTEREST_STATEMENT_NOTICE)
                && mcc.getContractType().equals(MarginCallMessageFormatter.CONTRACT_TYPE_CGAR)) {
            return noticeSubject.format(new String[]{getReminderAttribute(message) + "REPORTE DE INTERESES POR GARANTIAS ENTREGADAS/RECIBIDAS ? ",
                    mcc.getName(), getInterestDate(DSConnection.getDefault(), message)});

        } else {
            return noticeSubject.format(new String[]{templateTitle, mcc.getName(), po});
        }
    }

    private String buildBody() {
        final BOMessage clonedMessage;
        try {
            clonedMessage = (BOMessage) message.clone();
            clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE);
            clonedMessage.setAttribute("ORIG_TEMPLATE", message.getTemplateName());

            clonedMessage.setFormatType(FormatterUtil.HTML);
            PricingEnv defaultPricingEnv = null;

            final UserDefaults userDef = DSConnection.getDefault().getUserDefaults();
            if (userDef != null) {
                defaultPricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(userDef.getPricingEnvName());
                if (defaultPricingEnv == null) {
                    defaultPricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("DirtyPrice");
                }
            }
            return MessageFormatter.format(defaultPricingEnv, clonedMessage, true, DSConnection.getDefault());

        } catch (CloneNotSupportedException | CalypsoServiceException | MessageFormatException e) {
            Log.error(this.getClass().getName() + "cant create body to message: " + message.getLongId(), e);
        }
        return null;

    }

    /**
     * Get the Interest Date
     *
     * @param dsCon
     * @param message
     * @return String interest end date
     */
    private String getInterestDate(DSConnection dsCon, BOMessage message) {
        SantInterestNotificationCache s = new SantInterestNotificationCache();
        Trade trade = null;
        try {
            trade = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
            JDate endDate = s.getEndDate(trade);

            final Locale locale = Util.getLocale(message.getLanguage());
            final DateFormat df = new SimpleDateFormat("MMM, yyyy", locale);
            return df.format(endDate.getDate(TimeZone.getDefault())).toUpperCase();
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
        return "";
    }

    private String getReminderAttribute(BOMessage message) {
        String reminder = message.getAttribute("Reminder");
        if (!Util.isEmpty(reminder)) {
            if (reminder.equals("FirstReminder")) {
                return "*********REMINDER********* ";
            } else if (reminder.equalsIgnoreCase("SecondReminder")) {
                return "*********2nd REMINDER********* ";
            }
        }
        return "";
    }
}
