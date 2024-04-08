package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

public class SantStopUKSwiftMessageRule implements WfMessageRule {

    private static final String DOMAIN_NAME_SWIFT_UK_PROCESSING_ORGS = "SWIFT_UK_PROCESSING_ORGS";
    private static final String[] DEFAULT_SWIFT_UK_PROCESSING_ORGS = {"B2Q2",
            "ANTL", "1AVB", "FNR7"};

    private static final String DESCRIPTION = "Checks if a message is sent by a UK PO and its contract is not a Triparty contract. In this case method check returns false, and the message is stopped.";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message,
                         BOMessage oldMessage, Trade trade, BOTransfer transfer,
                         Vector messages, DSConnection dsCon, Vector excps, Task task,
                         Object dbCon, Vector events) {
        boolean result = true;

        boolean isUKProcessingOrg = isUKProcessingOrg(message);
        if (isUKProcessingOrg) {
            boolean isTriparty = isTriparty(message);
            if (!isTriparty) {
                result = false;
            }
        }

        return result;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message,
                          BOMessage oldMessage, Trade trade, BOTransfer transfer,
                          Vector messages, DSConnection dsCon, Vector excps, Task task,
                          Object dbCon, Vector events) {
        return true;
    }

    private boolean isUKProcessingOrg(BOMessage message) {
        boolean ukProcessingOrg = false;

        Collection<String> ukProcessingOrgs = getUKProcessingOrgs();
        String poShortName = getSenderShortName(message);
        ukProcessingOrg = ukProcessingOrgs.contains(poShortName);

        return ukProcessingOrg;
    }

    private Collection<String> getUKProcessingOrgs() {
        Collection<String> processingOrgs = null;

        try {
            processingOrgs = DSConnection.getDefault().getRemoteReferenceData()
                    .getDomainValues(DOMAIN_NAME_SWIFT_UK_PROCESSING_ORGS);
            if (processingOrgs == null || processingOrgs.size() == 0) {
                processingOrgs = Arrays
                        .asList(DEFAULT_SWIFT_UK_PROCESSING_ORGS);
            }
        } catch (CalypsoServiceException e) {
            String errorMessage = String.format(
                    "Could not retrieve domain \"%s\"",
                    DOMAIN_NAME_SWIFT_UK_PROCESSING_ORGS);
            Log.error(this, errorMessage, e);
        }

        return processingOrgs;
    }

    private String getSenderShortName(BOMessage message) {
        String shortName = null;

        int senderId = message.getSenderId();
        try {
            LegalEntity sender = DSConnection.getDefault()
                    .getRemoteReferenceData().getLegalEntity(senderId);
            if (sender != null) {
                shortName = sender.getCode();
            }
        } catch (CalypsoServiceException e) {
            String errorMessage = String.format(
                    "Cannot retrieve Legal Entity %d, sender of message %d",
                    senderId, message.getLongId());
            Log.error(this, errorMessage, e);
        }

        return shortName;
    }

    private boolean isTriparty(BOMessage message) {
        boolean isTriparty = false;

        long tradeId = message.getTradeLongId();
        try {
            Trade trade = DSConnection.getDefault().getRemoteTrade()
                    .getTrade(tradeId);
            Product product = trade.getProduct();
            if (product instanceof MarginCall) {
                MarginCall marginCall = (MarginCall) product;
                MarginCallConfig marginCallConfig = marginCall
                        .getMarginCallConfig();
                isTriparty = isTriparty(marginCallConfig);
            }
        } catch (CalypsoServiceException e) {
            String errorMessage = String.format(
                    "Cannot retrieve trade %d, taken from message %d", tradeId,
                    message.getLongId());
            Log.error(this, errorMessage, e);
        }

        return isTriparty;
    }

    private boolean isTriparty(MarginCallConfig marginCallConf) {
        CollateralConfig colConf = CacheCollateralClient.getCollateralConfig(
                DSConnection.getDefault(), marginCallConf.getId());

        return colConf.isTriParty();
    }

}
