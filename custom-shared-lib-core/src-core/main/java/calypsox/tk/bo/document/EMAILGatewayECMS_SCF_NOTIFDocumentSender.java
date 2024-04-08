package calypsox.tk.bo.document;

import calypsox.tk.emailnotif.EmailDataBuilder;
import calypsox.tk.emailnotif.EmailDataFactory;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.EMAILDocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.email.MailException;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class EMAILGatewayECMS_SCF_NOTIFDocumentSender extends EMAILDocumentSender {

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

        final String recieverAddressCode = message.getReceiverAddressCode().trim();
        ArrayList<String> racArray = new ArrayList<>(Arrays.asList(emailData.getToAddress(message)));
        if (!Util.isEmpty(recieverAddressCode) && (recieverAddressCode.indexOf(emailSeparator) > 0)) {
            racArray = new ArrayList<>(Arrays.asList(recieverAddressCode.split(emailSeparator)));
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
                paramVector2.add("EmailGatewayECMS_SCF_NOTIFDocumentSender Unable to get sender information for message" + message.getLongId());
                return false;
            }
            if (cpty == null) {
                paramVector2.add("EmailGatewayECMS_SCF_NOTIFDocumentSender Unable to get reciever information for message" + message.getLongId());
                return false;
            }

        } catch (final Exception e) {
            Log.error(this, e);
            paramVector2.add("EmailGatewayECMS_SCF_NOTIFDocumentSender Unable to get the margin call detail for message " + message.getLongId());
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
                paramVector2.add("EmailGatewayECMS_SCF_NOTIFDocumentSender Unable to get email text for message " + message.getLongId());
                return false;
            }
        } else {
            emailBody = emailData.getBody();
        }
        email.setText(emailBody);


        if (emailData.getFileAttached()) {
            // set the generated notification as an attachment of the email
            email.addAttachment("application/pdf", getNotificationFileName(adviceDocument, emailData),
                    adviceDocument.getBinaryDocument());
        }
        saveAndSendMessage(email, message, engineName, paramVector2);

        return true;
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

    private void saveAndSendMessage(EmailMessage email, BOMessage message, String engineName, final Vector paramVector2) {
        try {
            // send the email,
            EmailSender.send(email);

            // apply the send action on the message
            try {
                saveMessage(message, engineName, Action.S_SEND, "Updated by EmailGatewayECMS_SCF_NOTIFDocumentSender");

            } catch (Exception e1) {
                Log.error(this, "Error while saving message with action " + Action.S_SEND, e1);
                paramVector2.add("Email was sent but error saving the message with action " + Action.S_SEND);
            }
            //Needed to let know the engine that event was proccesed
//				saved[0] = true;

        } catch (final MailException me) {
            Log.error(this, me);
            paramVector2.add("EmailGatewayECMS_SCF_NOTIFDocumentSender Error " + me.getMessage() + " " + Util.exceptionToString(me));
            try {
                saveMessage(message, engineName, errorSend, "Updated by EmailGatewayECMS_SCF_NOTIFDocumentSender");
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

    private String getFileName(final AdviceDocument document, EmailDataBuilder emailData) {
        if (!Util.isEmpty(emailData.getFileName())) {
            return emailData.getFileName();
        } else {
            return document.getTemplateName();
        }
    }

}