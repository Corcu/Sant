package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.List;

public abstract class EmailDataBuilder {

    private final String emailSeparator = ";";
    protected BOMessage message ;
    private boolean fileAttached ;
    private String fileName;

    public EmailDataBuilder(BOMessage message) {
        this.message = message;
    }

    public String getFromAddress() {
        String fromAddress = LocalCache.getDomainValueComment(DSConnection.getDefault(), "domainName", "CSA_FROM_EMAIL");
        if (!Util.isEmpty(fromAddress)) {
            List<String> fromAdresses = Arrays.asList(fromAddress.trim().split(emailSeparator));
            return fromAdresses.get(0);
        }
        return "";
    }

    public String getToAddress(final BOMessage message) {
        return message.getReceiverAddressCode();
    }


    public BOMessage getMessage() {
        return message;
    }

    public boolean getFileAttached() {
        return fileAttached;
    }

    void setFileAttached(boolean fileAttached) {
        this.fileAttached = fileAttached;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public abstract String getSubject();
    public abstract String getBody();
}
