package calypsox.tk.bo.document;

import calypsox.tk.bo.CAMessageFormatter;
import calypsox.tk.bo.MarginCallMessageFormatter;
import calypsox.util.FormatUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.EMAILDocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CA;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.email.MailException;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Sender class for method EMAIL and for gateway CANOTIF Corporate Event email
 * notification sender. Creates the email, attachs the documents if the incoming
 * action requires it and Sends the email. Finally changes the final action of
 * the Message CA WF (from TO_BE_SENT to SENT/ERROR_SENT).
 *
 * @author VARIOUS
 * @version 1.0
 * @date
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class EMAILGatewayCANOTIFDocumentSender extends EMAILDocumentSender {


    /**
     * Actions constants Message CA WF - CA_NOTIFICATION (to pass from
     * TO_BE_SENT to SENT/ERROR_SENT).
     */
    public static final String ERROR_SEND = "ERROR_SEND";
    public static final String EMAIL = "EMAIL";
    public static final List<String> STR_MESSAGE_TYPE = Arrays.asList("CA_NOTIF", "CA_NOTICE");
    private static final String PDF_EXT = "PDF";
    private static PricingEnv defaultPricingEnv = null;
    public static final String EMAIL_SEPARATOR = ";";


    @Override
    public boolean send(final DSConnection dsCon, final SenderConfig senderConfig, final SenderCopyConfig senderCopyConfig,
                        final long eventId, final AdviceDocument adviceDocument, final Vector paramVector1,
                        final BOMessage message, final Vector paramVector2, final String engineName, final boolean[] saved) {

        List<String> toAddress = new ArrayList<String>();
        List<String> toCcAddress = new ArrayList<String>();
        List<String> toBccAddress = new ArrayList<String>();
        String fromAddress = manageAddresses(dsCon, message, toAddress, toCcAddress, toBccAddress, paramVector2);
        String body = manageBody(dsCon, message, paramVector2);
        String subject = manageSubject(dsCon, message, paramVector2);
        String fileName = manageFilename(dsCon, message, paramVector2);

        // Build the email message
        EmailMessage email = new EmailMessage();
        email.setFrom(fromAddress);
        email.setTo(toAddress);
        email.setToCc(toCcAddress);
        email.setToBcc(toBccAddress);
        email.setSubject(subject);
        email.setText(body);
        email.addAttachment("application/pdf", fileName + ".pdf", adviceDocument.getBinaryDocument());
        if("GROUP_NOTIF".equalsIgnoreCase(message.getEventType())) {
            email.addAttachment("application/vnd.ms-excel", fileName + ".xls", createExcelFile(dsCon, message, fileName));
        }

        // Send the mail y save the message as SENT
        try {
            EmailSender.send(email);
            try {
                saveMessage(message, engineName, Action.S_SEND, "Updated by EMAILCANOTIFDocumentSender.");
            } catch (Exception e1) {
                Log.error(this, "Error while saving message with action " + Action.S_SEND, e1);
                paramVector2.add("Email was sent but error saving the message with action " + Action.S_SEND);
            }
        } catch (final MailException me) {
            Log.error(this, me);
            paramVector2.add("EMAILGatewayCANOTIFDocumentSender Error " + me.getMessage() + " " + Util.exceptionToString(me));
            try {
                saveMessage(message, engineName, ERROR_SEND, "Updated by EMAILCANOTIFDocumentSender.");
            } catch (Exception exc) {
                Log.error(this, "Error while saving message with action " + ERROR_SEND, exc);
            }
        } catch (Exception exc) {
            Log.error(this, exc);
            paramVector2.add("Unable to send the message.");
        }

        return true;
    }


    private String getISIN(BOMessage message) {
        try {
            Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(message.getTradeLongId());
            if (trade.getProduct() instanceof CA) {
                CA ca = ((CA) trade.getProduct());
                return ca.getSecurity().getSecCode("ISIN");
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Can't retrieve any trade with id: " + message.getTradeLongId(), e);
        }

        return "";
    }


    /**
     * Saves the message allowing to jump into the next state: SENT or
     * ERROR_SENT
     *
     * @param message
     * @param engineName
     * @param action
     * @param comment
     * @throws Exception
     */
    private void saveMessage(BOMessage message, String engineName, String action, String comment) throws CalypsoServiceException, CloneNotSupportedException {
        // apply the send action on the message
        BOMessage msg = (BOMessage) message.clone();
        if (isBOMessageActionApplicable(msg, Action.valueOf(action))) {
            msg.setAction(Action.valueOf(action));
            long savedId = DSConnection.getDefault().getRemoteBO().save(msg, 0, engineName, comment);
            if (savedId > 0) {
                Log.info(this, "Saved BOMessage with id=" + savedId);
            } else {
                Log.error(this, "Could not save BOMessage with id=" + msg.getLongId());
            }
        }
    }


    /**
     * Checks if the BO message action is applicable.
     *
     * @return true if sucess, false otherwise
     */
    protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
    }


    /**
     * AAP File extension FIX
     *
     * @return a file name with the right extention
     */
    private String getNotificationFileName(final AdviceDocument document) {
        String fileName = "";
        if (document != null) {
            fileName = document.getTemplateName();
            final int extentionStart = fileName.lastIndexOf('.');
            if (extentionStart > 0) {
                final MimeType mime = document.getMimeType();
                try {
                    if ((mime != null) && PDF_EXT.equalsIgnoreCase(mime.getExtension())) {
                        fileName = fileName.substring(0, extentionStart + 1) + mime.getExtension().toLowerCase();
                    }
                } catch (NullPointerException e) {
                    Log.error(this, e);
                    if ((mime != null) && PDF_EXT.equalsIgnoreCase(mime.getType())) {
                        fileName = fileName.substring(0, extentionStart + 1) + mime.getType().toLowerCase();
                    }
                }
            }
        }
        return fileName;
    }


    /**
     * AAP FIX 14.4
     *
     * @param document
     * @return the mime type to use for this advice document
     */
    private String getNotificationMimeType(final AdviceDocument document) {
        String fileMimeType = "";
        if (document != null) {
            final MimeType mime = document.getMimeType();
            if (mime != null) {
                fileMimeType = mime.getType();
                if (PDF_EXT.equalsIgnoreCase(mime.getExtension())) {
                    fileMimeType = "application/pdf";
                }
            }
        }
        return fileMimeType;
    }


    /**
     * @param message
     * @param dsCon
     * @return the email text to use for the email being sent
     * @throws Exception
     */
    private String getNotificationEmailBody(final BOMessage message, final DSConnection dsCon)
            throws CloneNotSupportedException, CalypsoServiceException, MessageFormatException {
        final BOMessage clonedMessage = (BOMessage) message.clone();
        if (message.getEventType().equals(CAMessageFormatter.NOTIF_EMAIL_TEMPLATE)
                || message.getEventType().equals(CAMessageFormatter.NOTICE_EMAIL_TEMPLATE)) {
            clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE);
        }
        clonedMessage.setFormatType(FormatterUtil.HTML);
        if (defaultPricingEnv == null) {
            final UserDefaults userDef = dsCon.getUserDefaults();
            if (userDef != null) {
                defaultPricingEnv = dsCon.getRemoteMarketData().getPricingEnv(userDef.getPricingEnvName());
            }
        }
        return MessageFormatter.format(defaultPricingEnv, clonedMessage, true, dsCon);
    }


    public String getCA_SUBTYPE(Trade trade) {
        if (trade.getProduct() instanceof CA) {
            CA ca = (CA) trade.getProduct();
            String subType = ca.getSubType();
            if ("INTEREST".equalsIgnoreCase(subType)) {
                subType = "Coupon";
            }
            subType = subType.toUpperCase().charAt(0) + subType.substring(1, subType.length()).toLowerCase();

            return subType;
        }
        return "";

    }


    private String manageSubject(DSConnection dsCon, BOMessage message, Vector paramVector2) {
        String subject = "";
        LegalEntity cpty = null;
        try {
            cpty = BOCache.getLegalEntity(dsCon, message.getReceiverId());
            if (cpty == null) {
                paramVector2.add("EMAILCANOTIFDocumentSender Unable to get reciever information for message" + message.getLongId());
            }
        } catch (final Exception e) {
            Log.error(this, e);
            paramVector2.add("EMAILCANOTIFDocumentSender Unable to get the margin call detail for message " + message.getLongId());
        }

        // Grouped Notifications
        if("CorporateEventGroupNotice.html".equalsIgnoreCase(message.getTemplateName())) {
            String msgTemplate = message.getAttribute("ReportTemplate");
            if (!Util.isEmpty(msgTemplate)) {
                String cptyName = "";
                if (cpty != null) {
                    cptyName = cpty.getName();
                }
                if ("CA_Claims_Unpaid_ALL".equalsIgnoreCase(msgTemplate)) {
                    String[] dateArray;
                    if (!Util.isEmpty(message.getAttribute("Datetime")) && message.getAttribute("Datetime").length() >= 8) {
                        dateArray = message.getAttribute("Datetime").split("/");
                        subject = "ALL PENDING CLAIMS AS OF (" + dateArray[0] + "/" + dateArray[1] + "/" + dateArray[2].substring(0,2) + ") " + cptyName + " VS BANCO SANTANDER";
                    }else{
                        subject = "ALL PENDING CLAIMS AS OF (dd/MM/yyyy) " + cptyName + " VS BANCO SANTANDER";
                    }
                } else if ("CA_Claims_Unpaid_15D".equalsIgnoreCase(msgTemplate)) {
                    subject = "D+15 PENDING CLAIMS " + cptyName + " VS BANCO SANTANDER";
                } else if ("CA_Claims_Unpaid_1M".equalsIgnoreCase(msgTemplate)) {
                    subject = "ESCALATION: D+30 PENDING CLAIMS " + cptyName + " VS BANCO SANTANDER";
                } else if ("CA_Claims_Unpaid_7D_200000EUR_REC".equalsIgnoreCase(msgTemplate)) {
                    subject = "URGENT: CLAIMS OF HIGH AMOUNT PENDING TO RECEIVE " + cptyName + " VS BANCO SANTANDER";
                }
                else{
                    subject = "Grouped Notification " + cptyName + " VS BANCO SANTANDER";
                }
            }
        }
        // Individual Notifications
        else if("CorporateEventNotice.htm".equalsIgnoreCase(message.getTemplateName()) || "CorporateEventDeliveryNotice.html".equalsIgnoreCase(message.getTemplateName()) ) {
            String eventSubType = "";
            String settleDate = "";
            String cptyCode = "";
            String cptyName = "";
            String subjectCanceledStatus = "";
            String subjectUrgent = "";
            String isin = getISIN(message);

            Trade trade = null;
            try {
                trade = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass(), "Cant retrieve any trade with id: " + message.getTradeLongId(), e);
            }

            if (null != trade) {
                eventSubType = getCA_SUBTYPE(trade);
                settleDate = FormatUtil.formatDate(trade.getSettleDate(), "dd/MM/yyyy");
                if ("CANCELED".equalsIgnoreCase(trade.getStatus().getStatus())) {
                    subjectCanceledStatus = "CANCEL ";
                }
            }
            BOTransfer xfer = null;
            try {
                xfer = dsCon.getRemoteBO().getBOTransfer(message.getTransferLongId());
                if (xfer != null) {
                    String attr = xfer.getAttribute("EstadodeGestion");
                    if (!Util.isEmpty(attr) && "Escalated".equalsIgnoreCase(attr)) {
                        subjectUrgent = "URGENT ";
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass(), "Could not retrieve the transfer with id: " + message.getTransferLongId(), e);
            }
            if (null != cpty) {
                cptyCode = cpty.getCode();
                cptyName = cpty.getName();
            }
            if ("CorporateEventNotice.htm".equalsIgnoreCase(message.getTemplateName())) {
                subject = "Id: " + trade.getLongId() + " " + subjectCanceledStatus + subjectUrgent + eventSubType + " Claim" + " Isin: " + isin + " PaymentDate: " + settleDate + " Cpty: " + cptyCode + " " + cptyName;
            } else if ("CorporateEventDeliveryNotice.html".equalsIgnoreCase(message.getTemplateName())) {
                subject = "Id: " + trade.getLongId() + " " + eventSubType + " Claim Delivery Confirmation" + "  Isin: " + isin + "  PaymentDate: " + settleDate + "  Cpty: " + cptyCode + " " + cptyName;
            }
        }
        // Default
        else {
            subject = "CA Notification";
        }
        return subject;
    }


    private String manageFilename(DSConnection dsCon, BOMessage message, Vector paramVector2) {
        String fileName = "";
        LegalEntity cpty = null;
        try {
            cpty = BOCache.getLegalEntity(dsCon, message.getReceiverId());
            if (cpty == null) {
                paramVector2.add("EMAILCANOTIFDocumentSender Unable to get reciever information for message" + message.getLongId());
            }
        } catch (final Exception e) {
            Log.error(this, e);
            paramVector2.add("EMAILCANOTIFDocumentSender Unable to get the margin call detail for message " + message.getLongId());
        }

        // Grouped Notifications
        if("CorporateEventGroupNotice.html".equalsIgnoreCase(message.getTemplateName())) {
            String msgTemplate = message.getAttribute("ReportTemplate");
            if (!Util.isEmpty(msgTemplate)) {
                String cptyName = "";
                if (cpty != null) {
                    cptyName = cpty.getName();
                }
                if ("CA_Claims_Unpaid_ALL".equalsIgnoreCase(msgTemplate)) {
                    String[] dateArray;
                    if (!Util.isEmpty(message.getAttribute("Datetime")) && message.getAttribute("Datetime").length() >= 8) {
                        dateArray = message.getAttribute("Datetime").split("/");
                        fileName = "ALL_PENDING_CLAIMS_AS_OF_(" + dateArray[0] + "_" + dateArray[1] + "_" + dateArray[2].substring(0,2) + ")_" + cptyName + "_VS_BANCO_SANTANDER";
                    }
                    else{
                        fileName = "ALL_PENDING_CLAIMS_AS_OF_(dd_MM_yyyy)" + cptyName + "_VS_BANCO_SANTANDER";
                    }
                } else if ("CA_Claims_Unpaid_15D".equalsIgnoreCase(msgTemplate)) {
                    fileName = "D+15_PENDING_CLAIMS_" + cptyName + "_VS_BANCO_SANTANDER";
                } else if ("CA_Claims_Unpaid_1M".equalsIgnoreCase(msgTemplate)) {
                    fileName = "ESCALATION_D+30_PENDING_CLAIMS_" + cptyName + "_VS_BANCO_SANTANDER";
                } else if ("CA_Claims_Unpaid_7D_200000EUR_REC".equalsIgnoreCase(msgTemplate)) {
                    fileName = "URGENT_CLAIMS_OF_HIGH_AMOUNT_PENDING_TO_RECEIVE_" + cptyName + "_VS_BANCO_SANTANDER";
                }
                else{
                    fileName = "Grouped_Notification_" + cptyName + "_VS_BANCO_SANTANDER";
                }
            }
        }
        // Individual Notifications
        else if("CorporateEventNotice.htm".equalsIgnoreCase(message.getTemplateName()) || "CorporateEventDeliveryNotice.html".equalsIgnoreCase(message.getTemplateName()) ) {
            String eventSubType = "";
            String settleDate = "";
            String cptyCode = "";
            String cptyName = "";
            String filenameCanceledStatus = "";
            String filenameUrgent = "";
            String isin = getISIN(message);

            Trade trade = null;
            try {
                trade = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass(), "Cant retrieve any trade with id: " + message.getTradeLongId(), e);
            }

            if (null != trade) {
                eventSubType = getCA_SUBTYPE(trade);
                settleDate = FormatUtil.formatDate(trade.getSettleDate(), "dd/MM/yyyy");
                if ("CANCELED".equalsIgnoreCase(trade.getStatus().getStatus())) {
                    filenameCanceledStatus = "CANCEL_";
                }
            }
            BOTransfer xfer = null;
            try {
                xfer = dsCon.getRemoteBO().getBOTransfer(message.getTransferLongId());
                if (xfer != null) {
                    String attr = xfer.getAttribute("EstadodeGestion");
                    if (!Util.isEmpty(attr) && "Escalated".equalsIgnoreCase(attr)) {
                        filenameUrgent = "URGENT_";
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass(), "Could not retrieve the transfer with id: " + message.getTransferLongId(), e);
            }
            if (null != cpty) {
                cptyCode = cpty.getCode();
                cptyName = cpty.getName();
            }
            if ("CorporateEventNotice.htm".equalsIgnoreCase(message.getTemplateName())) {
                fileName = "Id_" + trade.getLongId() + "_" + filenameCanceledStatus + filenameUrgent + eventSubType + "_Claim" + " Isin_" + isin + " PaymentDate_" + settleDate + " Cpty_" + cptyCode + "_" + cptyName;
            } else if ("CorporateEventDeliveryNotice.html".equalsIgnoreCase(message.getTemplateName())) {
                fileName = "Id_" + trade.getLongId() + "_" + eventSubType + "_ClaimId_Delivery_Confirmation" + " Isin_" + isin + " PaymentDate_" + settleDate + " Cpty_" + cptyCode + "_" + cptyName;
            }
        }
        // Default
        else {
            fileName = "CA_Notification";
        }
        return fileName;
    }


    private String manageAddresses(DSConnection dsCon, BOMessage message, List<String> toAddress, List<String> toCcAddress, List<String> toBccAddress, Vector paramVector2) {
        final LEContact senderContact = BOCache.getLegalEntityContact(dsCon, message.getSenderContactId());
        final LEContact receiverContact = BOCache.getLegalEntityContact(dsCon, message.getReceiverContactId());
        if(senderContact==null || receiverContact==null){
            paramVector2.add("Unable to send notification: Sender or Receiver Contact does not exist for message with id " + message.getLongId());
            return "";
        }
        String from = manageFromAdress(message, senderContact, toBccAddress, paramVector2);
        manageGenericToAdresses(message, receiverContact, toAddress, paramVector2);
        manageGenericCcAdresses(senderContact, toCcAddress);
        manageSpecialAdresses(dsCon, message, senderContact, receiverContact, toAddress, toCcAddress);
        return from;
    }


    private String manageFromAdress(BOMessage message, LEContact senderContact, List<String> toBccAddress, Vector paramVector2){
        String from = senderContact.getAddressCode("CORPORATE_EVENTS_FROM_EMAIL");
        if (!Util.isEmpty(from)) {
            toBccAddress.add(from);
        }
        else{
            paramVector2.add("Unable to send notification: No 'From Address' was defined for message with id " + message.getLongId());
        }
        return from;
    }


    private void manageGenericToAdresses(BOMessage message, LEContact receiverContact, List<String> toAddress, Vector paramVector2) {
        final String eMail = receiverContact.getAddressCode("EMAIL");
        if(Util.isEmpty(eMail)){
            paramVector2.add("Unable to send notification: No destination address was defined for message with id " + message.getLongId());
        }
        if(!eMail.contains(EMAIL_SEPARATOR)){
            toAddress.add(eMail);
        }
        else{
            toAddress.addAll(Arrays.asList(eMail.split(EMAIL_SEPARATOR)));
        }
        String additionalEmail = receiverContact.getAddressCode("ADDITIONAL_EMAIL");
        if (!Util.isEmpty(additionalEmail)) {
            if(!additionalEmail.contains(EMAIL_SEPARATOR)){
                toAddress.add(additionalEmail);
            }
            else{
                toAddress.addAll(Arrays.asList(additionalEmail.split(EMAIL_SEPARATOR)));
            }
        }
        String additionalEmail2 = receiverContact.getAddressCode("ADDITIONAL_EMAIL2");
        if (!Util.isEmpty(additionalEmail2)) {
            if(!additionalEmail2.contains(EMAIL_SEPARATOR)){
                toAddress.add(additionalEmail2);
            }
            else{
                toAddress.addAll(Arrays.asList(additionalEmail2.split(EMAIL_SEPARATOR)));
            }
        }
    }


    private void manageGenericCcAdresses(LEContact senderContact, List<String> toCcAddress) {
        final String eMail = senderContact.getAddressCode("EMAIL");
        if (!Util.isEmpty(eMail)) {
            if (!eMail.contains(EMAIL_SEPARATOR)) {
                toCcAddress.add(eMail);
            } else {
                toCcAddress.addAll(Arrays.asList(eMail.split(EMAIL_SEPARATOR)));
            }
        }
        String additionalEmail = senderContact.getAddressCode("ADDITIONAL_EMAIL");
        if (!Util.isEmpty(additionalEmail)) {
            if(!additionalEmail.contains(EMAIL_SEPARATOR)){
                toCcAddress.add(additionalEmail);
            }
            else{
                toCcAddress.addAll(Arrays.asList(additionalEmail.split(EMAIL_SEPARATOR)));
            }
        }
        String additionalEmail2 = senderContact.getAddressCode("ADDITIONAL_EMAIL2");
        if (!Util.isEmpty(additionalEmail2)) {
            if(!additionalEmail2.contains(EMAIL_SEPARATOR)){
                toCcAddress.add(additionalEmail2);
            }
            else{
                toCcAddress.addAll(Arrays.asList(additionalEmail2.split(EMAIL_SEPARATOR)));
            }
        }
    }


    private void manageSpecialAdresses(DSConnection dsCon, BOMessage message, LEContact senderContact, LEContact receiverContact, List<String> toAddress, List<String> toCcAddress){

        // Escalated Contacts
        Boolean escalated = false;
        String notificationType = message.getTemplateName();
        if("CorporateEventNotice.htm".equalsIgnoreCase(notificationType)){
            BOTransfer xfer = null;
            try {
                xfer = dsCon.getRemoteBO().getBOTransfer(message.getTransferLongId());
                if (xfer != null) {
                    String attr = xfer.getAttribute("EstadodeGestion");
                    escalated = (!Util.isEmpty(attr) && "Escalated".equalsIgnoreCase(attr)) ? true : false;
                }
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass(), "Could not retrieve the transfer with id: " + message.getTransferLongId(), e);
            }
        }
        else if("CorporateEventGroupNotice.html".equalsIgnoreCase(notificationType)){
            String groupTemplate = message.getAttribute("ReportTemplate");
            escalated = !Util.isEmpty(groupTemplate) && "CA_Claims_Unpaid_30D".equalsIgnoreCase(groupTemplate) ? true : false;
        }

        if(escalated){
            String escalatedEmail = receiverContact.getAddressCode("ESCALATION_EMAIL");
            if (!Util.isEmpty(escalatedEmail)) {
                if(!escalatedEmail.contains(EMAIL_SEPARATOR)){
                    toAddress.add(escalatedEmail);
                }
                else{
                    toAddress.addAll(Arrays.asList(escalatedEmail.split(EMAIL_SEPARATOR)));
                }
            }
        }

        // FO Contacts
        if("CorporateEventGroupNotice.html".equalsIgnoreCase(notificationType)){
            String groupTemplate = message.getAttribute("ReportTemplate");
            if(!Util.isEmpty(groupTemplate) && "CA_Claims_Unpaid_30D".equalsIgnoreCase(groupTemplate)) {
                String foEmail = senderContact.getAddressCode("FO_EMAIL");
                if (!Util.isEmpty(foEmail)) {
                    if (!foEmail.contains(EMAIL_SEPARATOR)) {
                        toCcAddress.add(foEmail);
                    } else {
                        toCcAddress.addAll(Arrays.asList(foEmail.split(EMAIL_SEPARATOR)));
                    }
                }
            }
        }

    }


    private String manageBody(DSConnection dsCon, BOMessage message, Vector paramVector2) {
        try {
            return getNotificationEmailBody(message, dsCon);
        } catch (final Exception e) {
            Log.error(this, e);
            paramVector2.add("EMAILCANOTIFDocumentSender Unable to get email text for message " + message.getLongId());
            return "";
        }
    }


    private String createExcelFile(DSConnection ds, BOMessage message, String fileName) {
        JDatetime valDatetime = JDatetime.valueOf(message.getAttribute("Datetime"));
        String type = message.getAttribute("ReportType");
        String templateName = message.getAttribute("ReportTemplate");
        String format = "Excel";
        String cptyId = message.getAttribute("CptyId");
        String pricingEnv = message.getAttribute("PricingEnv");
        Vector<String> holidays = null;
        String holidaysAttr = message.getAttribute("Holidays");
        if(!Util.isEmpty(holidaysAttr)) {
            holidays = new Vector<String>(Arrays.asList(holidaysAttr.replace("[", "").replace("]", "").replace(" ", "").split(",")));
        }
        String html = null;
        ReportOutput output;
        try {
            output = generateReportOutput(ds, type, templateName, valDatetime, pricingEnv, holidays, cptyId);
            if (output!=null && output.getNumberOfRows()>0) {
                html = saveReportOutput(output, format, type, fileName);
            }
        } catch (Exception e) {
            Log.error(this, "Can't save report " + type + ", please consult log file. ", e);
        }
        if (html == null) {
            Log.error(this, "Can't generate report " + type + " or generated document is empty.");
        }
        return html;
    }


    private ReportOutput generateReportOutput(DSConnection ds, String type, String templateName, JDatetime valDatetime, String pricingEnv, Vector<String> holidays, String cptyId) throws RemoteException {
        PricingEnv env = ds.getRemoteMarketData().getPricingEnv(pricingEnv, valDatetime);
        Report reportToFormat = createReport(type, templateName, env, valDatetime, cptyId);
        if (reportToFormat == null) {
            Log.error(this, "Invalid report type: " + type);
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(this, "Invalid report template: " + type);
            return null;
        }
        if (!Util.isEmpty(holidays)) {
            reportToFormat.getReportTemplate().setHolidays(holidays);
        }
        reportToFormat.getReportTemplate().setTimeZone(TimeZone.getDefault());
        return reportToFormat.load(new Vector());
    }


    private Report createReport(String type, String templateName, PricingEnv env, JDatetime valDatetime, String cptyId) throws java.rmi.RemoteException {
        Report report;
        try {
            String className = "tk.report." + type + "Report";
            report = (Report) InstantiateUtil.getInstance(className, true);
            report.setPricingEnv(env);
            report.setValuationDatetime(valDatetime);
        } catch (Exception e) {
            Log.error(this, e);
            report = null;
        }
        if ((report != null) && !Util.isEmpty(templateName)) {
            final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData().getReportTemplate(ReportTemplate.getReportName(type), templateName);
            if (template == null) {
                Log.error(this, ("Template " + templateName + " Not Found for " + type + " Report"));
            } else {
                template.put(TradeReportTemplate.GENERATE_PDF_HEADER_B, true);
                report.setReportTemplate(template);
                template.setValDate(valDatetime.getJDate(TimeZone.getDefault()));
                template.getAttributes().getAttributes().put("CptyName", cptyId);
                template.callBeforeLoad();
            }
        }
        return report;
    }


    private String saveReportOutput(final ReportOutput reportOutput, String type, final String reportName, final String fileName) {
        ((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_DELIMITER", null);
        ((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_SHOWHEADER", "false");
        ReportViewer viewer = DefaultReportOutput.getViewer("xls");
        reportOutput.format(viewer);
        return viewer.toString();
    }


}

