package calypsox.tk.engine.mail;

import calypsox.tk.event.PSEventMailingAlert;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.email.MailException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Sends missing SDIs mails for Repo and Bonds.
 * Class and variable names were unchanged from Repo only initial version, feel free to do it.
 */
public class RepoMissingSDIMailSender {

    private static final String DOMAIN_NAME = "RepoMissingSDIMailTo";
    private static final String LOGCAT = "RepoSDITaskEngineListener";
    private static final String STATIC_DATA_FILTER = "REPO_MISSINGSDI";
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

    private static ArrayList<String> EMAIL_TO;

    /**
     * Constructor overriding default EMAIL_TO argument, with vector of mails to fetch as parameter
     */
    public RepoMissingSDIMailSender() {
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
        DSConnection dsCon = DSConnection.getDefault();

        Log.info(LOGCAT, "New PSEventMailingAlert: " + event);

        Task task = event.getTask();

        StaticDataFilter sdf = StaticDataFilter.valueOf(STATIC_DATA_FILTER);
        if (sdf != null && sdf.accept(null, task)) {

            Trade trade = null;
            try {
                trade = dsCon.getRemoteTrade().getTrade(task.getTradeLongId());
            } catch (CalypsoServiceException e) {
                Log.error(LOGCAT, "Could not load trade " + task.getTradeLongId());
            }

            //If trade was found for task, and its product is Repo
            if (trade != null && isProductAccepted(trade.getProduct())) {
                buildAndSendEmail(trade,task);
            }
        }
    }

    private void buildAndSendEmail(Trade trade, Task task){
        //Get all the EX_MISSING_SI tasks
        TaskArray ta = null;
        try {
            //only check task with status 'NEW'
            ta = DSConnection.getDefault().getRemoteBackOffice().getTasks("TRADE_ID = '" + trade.getLongId() + "' AND EVENT_TYPE='EX_MISSING_SI' AND TASK_STATUS=0", null);
        } catch (CalypsoServiceException e) {
            Log.error(LOGCAT, "Could not load tasks from trade " + task.getTradeLongId());
        }

        boolean isCash = false;
        boolean isSecurity = false;
        if (ta != null && !ta.isEmpty()) {
            //Check of what type of missing SDI are they, cash, security or both
            for (Task subtask : ta.getTasks()) {
                if (subtask.getComment().contains("CASH")) {
                    isCash = true;
                } else if (subtask.getComment().contains("SECURITY")) {
                    isSecurity = true;
                }

                if (isCash && isSecurity) {
                    break;
                }
            }

            StringBuilder sdiTypes = new StringBuilder();
            sdiTypes.append(" SDI - ");
            sdiTypes.append(trade.getCounterParty().getAuthName());
            sdiTypes.append(" ");
            if (isCash && isSecurity) {
                sdiTypes.insert(0, "Security & Cash");
                sdiTypes.append(trade.getSettleCurrency());
                sdiTypes.append(" ");
                sdiTypes.append(getMarketSectorDescription(trade.getProduct()));
                sdiTypes.append(" ");
                sdiTypes.append(getIsin(trade.getProduct()));
                sdiTypes.append(" ");
                sdiTypes.append(getSecName(trade.getProduct()));
                sdiTypes.append(" ");
                sdiTypes.append(getSecFinanceStartDate(trade.getProduct()));
            } else if (isCash) {
                sdiTypes.insert(0, "Cash");
                sdiTypes.append(trade.getSettleCurrency());
            } else if (isSecurity) {
                sdiTypes.insert(0, "Security");
                sdiTypes.append(getMarketSectorDescription(trade.getProduct()));
                sdiTypes.append(" ");
                sdiTypes.append(getIsin(trade.getProduct()));
                sdiTypes.append(" ");
                sdiTypes.append(getSecName(trade.getProduct()));
                sdiTypes.append(" ");
                sdiTypes.append(getSecFinanceStartDate(trade.getProduct()));
            }
            sdiTypes.append(" - ");

            String subject = getProductClassName(trade.getProduct())
                    + " MxId " + trade.getExternalReference() + " / " +
                    "Calypso Id " + trade.getLongId() + " / " +
                    "Entity " + trade.getBook().getLegalEntity().getCode() + " / " +
                    "Settle Date " + trade.getSettleDate() + " / Pending " +
                    sdiTypes + task.getDatetime().format(TimeZone.getDefault());

            JDate now = JDate.getNow();
            //If todays date is settle date, send mail with high priority
            if (now.equals(trade.getSettleDate())) {
                HashMap<String, String> headers = new HashMap<String, String>() {{
                    put("X-Priority", "1");
                }};
                try {
                    CollateralUtilities.sendEmail(EMAIL_TO, subject, "", DEFAULT_FROM_EMAIL, headers);

                } catch (MailException e) {
                    Log.error(LOGCAT, e);
                }
            } else {
                CollateralUtilities.sendEmail(EMAIL_TO, subject, "", DEFAULT_FROM_EMAIL);
            }
            try {
                trade.setAction(Action.AMEND);
                trade.addKeyword("MailSDISent","true");
                DSConnection.getDefault().getRemoteTrade().save(trade);
            } catch (CalypsoServiceException e) {
                Log.error(LOGCAT, e);
            }
        }

    }

    /**
     * @return true if Repo or Bond
     */
    private boolean isProductAccepted(Product product) {
        return product instanceof Repo || product instanceof Bond;
    }

    private String getIsin(Product product) {
        return getSecurityFromProduct(product).getSecCode("ISIN");
    }

    private String getMarketSectorDescription(Product product) {
        return getSecurityFromProduct(product).getSecCode("IS MARKET SECTOR DESCRIPTION");
    }

    private String getSecName(Product product) {
        return getSecurityFromProduct(product).getName();
    }

    private Product getSecurityFromProduct(Product product) {
        Product secProduct = product;
        if (product instanceof SecFinance) {
            secProduct = ((SecFinance) product).getSecurity();
        }
        return secProduct;
    }

    private String getSecFinanceStartDate(Product product) {
        String parsedStartDate = "";
        if (product instanceof SecFinance) {
            parsedStartDate = Util.dateToString(((SecFinance) product).getStartDate());
        }
        return parsedStartDate;
    }

    private String getProductClassName(Product product) {
        return product.getClass().getSimpleName();
    }
}
