package calypsox.tk.util;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class ScheduledTaskSANTCSVREPORT extends ScheduledTaskCSVREPORT {
    public static final String DV_GROUP_MAIL = "DVGroupMail";

    private static final String DV_GROUP_LIST = "DVGroupList";
    private static final String SAN_CSV_REPORT_GROUP_MAIL_LIST = "San_CSVReport_GroupMailList";


    private String getValueFromComment(String attribute) {

        Vector<String> values = LocalCache.getDomainValues(getDSConnection(), SAN_CSV_REPORT_GROUP_MAIL_LIST);
        for (String value : values) {
            String comment = LocalCache.getDomainValueComment(getDSConnection(), SAN_CSV_REPORT_GROUP_MAIL_LIST, value);
            if (attribute.equals(comment)) {
                return value;
            }

        }
        return null;
    }


    private String getEmailsFromDV(String domainName) {

        StringBuilder sb = new StringBuilder();
        Vector<String> values = LocalCache.getDomainValues(getDSConnection(), domainName);
        for (String value : values) {
            sb.append(value).append(",");
        }

        return sb.substring(0, sb.length() - 1);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Vector getDomainAttributes() {
        @SuppressWarnings("unchecked") final Vector<String> result = super.getDomainAttributes();

        result.add(DV_GROUP_MAIL);
        return result;
    }


    @Override
    protected void sendEmail(final String filePath) {

        // Search for the emails in Domain Values
        if (DV_GROUP_LIST.equals(getAttribute(CUSTOM_EMAIL_LIST))) {

            String groupMailAttr = getAttribute(DV_GROUP_MAIL);
            if (Util.isEmpty(groupMailAttr)) {
                Log.error(this, "Empty field: " + DV_GROUP_MAIL);
                return;
            }

            String domainName = getValueFromComment(groupMailAttr);
            if (Util.isEmpty(domainName)) {
                Log.error(this, "Unable to find Value in Domain Name '" + SAN_CSV_REPORT_GROUP_MAIL_LIST +
                        "' with comment '" + groupMailAttr + "'.");
                return;
            }

            String emailList = getEmailsFromDV(domainName);

            setAttribute(CUSTOM_EMAIL_LIST, emailList);
        }

        super.sendEmail(filePath);
    }


    @Override
    protected boolean validateEmailAddress(String emails, Vector messages) {
        if (DV_GROUP_LIST.equals(emails)) {
            return true;
        }

        return super.validateEmailAddress(emails, messages);
    }
}