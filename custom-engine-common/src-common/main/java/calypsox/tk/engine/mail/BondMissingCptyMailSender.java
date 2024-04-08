package calypsox.tk.engine.mail;

import calypsox.tk.event.PSEventMailingAlert;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.ArrayList;
import java.util.Vector;


public class BondMissingCptyMailSender {

    private static final String DOMAIN_NAME = "RepoMissingSDIMailTo";
    private static final String LOGCAT = "BondAllocCptyTaskEngineListener";
    private static final String STATIC_DATA_FILTER = "BONDALLOCATION_MISSINGCPTY";
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

    private static ArrayList<String> EMAIL_TO;


    public BondMissingCptyMailSender() {
        Vector<String> mailTo = LocalCache.getDomainValues(DSConnection.getDefault(), DOMAIN_NAME);
        if (!mailTo.isEmpty()) {
            EMAIL_TO = new ArrayList<>(mailTo);
        }
    }
    /**
     * Parsing of PSEventTask events
     *
     * @param event the PSEventTask event
     */
    public void sendEmail(PSEventMailingAlert event) {
        Log.info(LOGCAT, "New PSEventMailingAlert: " + event);

        Task task = event.getTask();

        StaticDataFilter sdf = StaticDataFilter.valueOf(STATIC_DATA_FILTER);
        if (sdf != null && sdf.accept(null, task)) {

            String cpty="";
            String isin="";
            String tradeDate="";
            String extRef="";
            try {
                BOMessage msg=DSConnection.getDefault().getRemoteBO().getMessage(task.getObjectLongId());
                if(msg!=null){
                    cpty=msg.getAttribute("Counterparty");
                    isin=msg.getAttribute("ISIN");
                    tradeDate=msg.getAttribute("TradeDate");
                    extRef=msg.getAttribute("UploadObjectExternalRef");
                }
            } catch (CalypsoServiceException exc) {
                Log.error(LOGCAT,exc);
            }
            String emailSubject="Setup required for ["+cpty+"] – ["+extRef+"]";
            String body= "Dear Client Data,\n" +
                    "\n" +
                    " \n" +
                    "\n" +
                    "The following fund has traded in DVP product. Please set-up the entity on downstream systems.\n" +
                    "\n" +
                    "Legal Name: ["+cpty+"]\n" +
                    "\n" +
                    "ISIN : ["+isin+"]\n" +
                    "\n" +
                    "Value Date: ["+tradeDate+"]\n" +
                    "\n" +
                    "Reference: ["+extRef+"]";

            CollateralUtilities.sendEmail(EMAIL_TO, emailSubject, body, DEFAULT_FROM_EMAIL);

        }
    }
}
