package calypsox.tk.bo.document;


import calypsox.tk.emailnotif.EmailDataBuilder;
import calypsox.tk.emailnotif.EmailDataFactory;
import calypsox.util.MarginCallConstants;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.FormatterUtil;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.EMAILDocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.email.MailException;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;


public class EMAILGatewayCO2NOTIFDocumentSender extends EMAILDocumentSender {

    private final String emailSeparator = ";";
    private final String errorSend = "ERROR_SEND";
    private static final String PDF_EXT = "PDF";


    @Override
    public boolean send(final DSConnection dsCon, final SenderConfig senderConfig,
                        final SenderCopyConfig senderCopyConfig, final long eventId, final AdviceDocument adviceDocument,
                        final Vector paramVector1, final BOMessage message, final Vector paramVector2, final String engineName,
                        final boolean[] saved) {

        // build the email message
        final EmailMessage email = new EmailMessage();
        EmailDataFactory emailDataFactory = new EmailDataFactory();
        EmailDataBuilder emailData = emailDataFactory.getEmailData(message);

        ArrayList<String> racArray = new ArrayList<>();
        final String toAddress = emailData.getToAddress(message).trim();
        if (!Util.isEmpty(toAddress) && !toAddress.contains(emailSeparator)){
            racArray.add(toAddress);
        }
        else if (!Util.isEmpty(toAddress) && (toAddress.indexOf(emailSeparator) > 0)) {
            racArray = new ArrayList<>(Arrays.asList(toAddress.split(emailSeparator)));
        }

        if (Util.isEmpty(racArray)) {
            paramVector2.add("Unable to send notification: No destination address was defined for message with id " + message.getLongId());
            return false;
        }

        LegalEntity po = null;
        LegalEntity cpty = null;
        try {
            po = BOCache.getLegalEntity(dsCon, message.getSenderId());
            cpty = BOCache.getLegalEntity(dsCon, message.getReceiverId());
            if (po == null) {
                paramVector2.add("EmailGatewayCO2NOTIFDocumentSender Unable to get sender information for message" + message.getLongId());
                return false;
            }
            if (cpty == null) {
                paramVector2.add("EmailGatewayCO2NOTIFDocumentSender Unable to get reciever information for message" + message.getLongId());
                return false;
            }

        } catch (final Exception e) {
            Log.error(this, e);
            paramVector2.add("EmailGatewayCO2NOTIFDocumentSender Unable to get the margin call detail for message " + message.getLongId());
            return false;
        }

        // use the default from address, instead of the one configured on the
        // message, since this one can have more than one email.
        final String fromAddress = emailData.getFromAddress();
        // check that the email address is filled
        if (Util.isEmpty(fromAddress)) {
            paramVector2.add("Unable to send notification: No from address was defined for message with id " + message.getLongId());
            return false;
        }

        // Add receiver additional email part
        if (null != getAdditionalEmailAddress(dsCon, message)) {
            racArray.addAll(getAdditionalEmailAddress(dsCon, message));
        }

        // set the email properties
        email.setFrom(fromAddress);
        email.setTo(racArray);
        email.setToBcc(Arrays.asList(fromAddress));
        email.setSubject(emailData.getSubject());

        // get email body, to do so, we will use the calypso template framework
        String emailBody;
        if (Util.isEmpty(emailData.getBody())) {
            try {
                emailBody = getNotificationEmailBody(message, dsCon);
            } catch (final Exception e) {
                Log.error(this, e);
                paramVector2.add("EmailGatewayCO2NOTIFDocumentSender Unable to get email text for message " + message.getLongId());
                return false;
            }
        } else {
            emailBody = emailData.getBody();
        }
        email.setText(emailBody);

        if (Boolean.parseBoolean(message.getAttribute(MarginCallConstants.MESSAGE_ATTR_RESEND_MESSAGE))) {
            saveAndResendMessage(emailData, email, message, engineName, paramVector2, saved, dsCon);
        } else {
            if (emailData.getFileAttached()) {
                // set the generated notification as an attachment of the email
                email.addAttachment("application/pdf", getNotificationFileName(adviceDocument, emailData),
                        adviceDocument.getBinaryDocument());
            }
            saveAndSendMessage(email, message, engineName, paramVector2);
        }

        return true;
    }

    private ArrayList<String> getAdditionalEmailAddress(DSConnection dsCon, BOMessage message) {

        final LEContact contact = BOCache.getLegalEntityContact(dsCon, message.getReceiverContactId());
        ArrayList<String> racAddtionalArray = null;
        String additionalEmails = "";

        if ((contact != null) && !Util.isEmpty(contact.getAddressCode("ADDITIONAL_EMAIL"))) {
            additionalEmails = contact.getAddressCode("ADDITIONAL_EMAIL");
        }

        if ((contact != null) && !Util.isEmpty(contact.getAddressCode("ADDITIONAL_EMAIL2"))) {
            if (!Util.isEmpty(additionalEmails)) {
                additionalEmails = additionalEmails + ";" + contact.getAddressCode("ADDITIONAL_EMAIL2");
            } else {
                additionalEmails = contact.getAddressCode("ADDITIONAL_EMAIL2");
            }
        }
        if ((contact != null) && !Util.isEmpty(contact.getAddressCode("ADDITIONAL_EMAIL3"))) {
            if (!Util.isEmpty(additionalEmails)) {
                additionalEmails = additionalEmails + ";" + contact.getAddressCode("ADDITIONAL_EMAIL3");
            } else {
                additionalEmails = contact.getAddressCode("ADDITIONAL_EMAIL3");
            }
        }

        if (!Util.isEmpty(additionalEmails)) {
            racAddtionalArray = new ArrayList<>(Arrays.asList(additionalEmails.trim().split(emailSeparator)));
        }

        return racAddtionalArray;
    }

    /**
     * @param message
     * @param dsCon
     * @return the email text to use for the email being sent
     * @throws Exception
     */
    private String getNotificationEmailBody(final BOMessage message, final DSConnection dsCon)
            throws CloneNotSupportedException, CalypsoServiceException, MessageFormatException {

        PricingEnv defaultPricingEnv = null;
        final BOMessage clonedMessage = (BOMessage) message.clone();
        clonedMessage.setTemplateName(message.getTemplateName());
        clonedMessage.setFormatType(FormatterUtil.HTML);

        final UserDefaults userDef = dsCon.getUserDefaults();
        if (userDef != null) {
            defaultPricingEnv = dsCon.getRemoteMarketData().getPricingEnv(userDef.getPricingEnvName());
        }

        return MessageFormatter.format(defaultPricingEnv, clonedMessage, true, dsCon);
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
     * @param message
     * @param action
     * @return true if sucess, false otherwise
     */
    private boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
    }

    private void saveAndResendMessage(EmailDataBuilder emailData, EmailMessage email, BOMessage message, String engineName, final Vector paramVector2, final boolean[] saved, DSConnection dsCon) {
        // in this case just get the already saved documents and send them
        try {
            email.resetAttachmentList();
            if (emailData.getFileAttached()) {

                final Vector adviceDocuments = dsCon.getRemoteBackOffice()
                        .getAdviceDocuments("advice_document.advice_id=" + message.getLongId(), null, null);
                if (!Util.isEmpty(adviceDocuments)) {
                    for (int i = 0; i < adviceDocuments.size(); i++) {
                        final AdviceDocument ad = (AdviceDocument) adviceDocuments.get(i);
                        email.addAttachment(getNotificationMimeType(ad), getNotificationFileName(ad, emailData),
                                ad.getBinaryDocument());
                    }
                }
            }
            // send the email,
            EmailSender.send(email);

            // apply the send action on the message
            String sendAgain = "SEND_AGAIN";
            try {
                saveMessage(message, engineName, sendAgain, "Updated by EmailGatewayCO2NOTIFDocumentSender");
            } catch (Exception e1) {
                Log.error(this, "Error while saving message with action " + sendAgain, e1);
                paramVector2.add("Email was sent but error saving the message with action " + sendAgain);
            }
            // don't save the advice document
            saved[0] = true;
        } catch (final MailException e) {
            Log.error(this, e);
            paramVector2.add("Error sending Email.");
            try {
                saveMessage(message, engineName, errorSend, "Updated by EmailGatewayCO2NOTIFDocumentSender");
            } catch (Exception e1) {
                Log.error(this, e1); //sonar purpose
                Log.error(this, "Error while saving message with action " + errorSend, e);
            }
        } catch (Exception exc) {
            Log.error(this, exc);
            paramVector2.add("Unable to resend the message");
        }
    }

    private void saveAndSendMessage(EmailMessage email, BOMessage message, String engineName, final Vector paramVector2) {
        try {
            // send the email,
            EmailSender.send(email);

            // apply the send action on the message
            try {
                saveMessage(message, engineName, Action.S_SEND, "Updated by EmailGatewayCO2NOTIFDocumentSender");

            } catch (Exception e1) {
                Log.error(this, "Error while saving message with action " + Action.S_SEND, e1);
                paramVector2.add("Email was sent but error saving the message with action " + Action.S_SEND);
            }
            //Needed to let know the engine that event was proccesed
//				saved[0] = true;

        } catch (final MailException me) {
            Log.error(this, me);
            paramVector2.add("EmailGatewayCO2NOTIFDocumentSender Error " + me.getMessage() + " " + Util.exceptionToString(me));
            try {
                saveMessage(message, engineName, errorSend, "Updated by EmailGatewayCO2NOTIFDocumentSender");
            } catch (Exception exc) {
                Log.error(this, "Error while saving message with action " + errorSend, exc);
            }
        } catch (Exception exc) {
            Log.error(this, exc);
            paramVector2.add("Unable to send the message");
        }
    }

    /**
     * AAP File extension FIX
     *
     * @return a file name with the right extention
     */
    private String getNotificationFileName(final AdviceDocument document, EmailDataBuilder emailData) {
        String fileName = "";
        if (document != null) {

            fileName = getFileName(document, emailData);
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

    private String getFileName(final AdviceDocument document, EmailDataBuilder emailData) {
        if (!Util.isEmpty(emailData.getFileName())) {
            return emailData.getFileName();
        } else {
            return document.getTemplateName();
        }
    }

}
