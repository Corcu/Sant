/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
/**
 *
 */
package com.santander.collateral.util.email;

import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.Log;
import com.calypso.tk.util.email.MailException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * Same functionality as EmailSender, but if the security email properties have
 * been read from the calypso_mail_config.properties, it will this class will
 * add secure connection. If is not the case, it will use exactly the
 * EmailSender service.
 *
 * @see com.santander.collateral.util.email.EmailSender
 *
 * @author Guillermo Solano
 * @version 2.0, 28/08/2013. Added label to FROM and replyTo
 *
 */
public class SantanderEmailSender {

    /**
     * Properties of the sender
     */
    private SantanderMailConfig mailConfig;
    /**
     * Session
     */
    private Session session;
    /**
     * Transport instance
     */
    private Transport transport;

    /**
     * Constructor
     *
     * @throws MessagingException
     *             if we dont have the email properties file and at least server
     *             and port has been read
     */
    public SantanderEmailSender() throws MessagingException {

        this.mailConfig = null;
        this.session = null;
        this.transport = null;
        // init email configuration
        init();
    }

    /**
     * Main method to send an email
     *
     * @param messageToSend
     *            : the email elements to be sent
     * @throws MailException
     *             thrown if there is something wrong while sending an email
     */
    public void send(EmailMessage messageToSend) throws MailException {

        /* if not authentication is used, try old way */
        if ((this.mailConfig == null) || !this.mailConfig.useAuthentication()) {
            // AAP MIG V14.4 ROLLBACK TO V12
            EmailSender.send(messageToSend,null);
        } else { // AUTHENTICATION MODE SEND ENABLE:
            // connect to server
            connectTransport();
            // send using authentication
            sendAuth(messageToSend);
            // disconnect from the server
            disconnectTransport();
        }
    }

    /**
     * Main method to send an email
     *
     * @param messageToSend
     *            : the email elements to be sent
     * @param headers
     *          : the email headers to apply
     * @throws MailException
     *             thrown if there is something wrong while sending an email
     */
    public void send(EmailMessage messageToSend, HashMap<String,String> headers) throws MailException, MessagingException {

        /* if not authentication is used, try old way */
        if ((this.mailConfig == null) || !this.mailConfig.useAuthentication()) {
            // AAP MIG V14.4 ROLLBACK TO V12
            EmailSender.sendWithHeaders(messageToSend, headers);
        } else { // AUTHENTICATION MODE SEND ENABLE:
            // connect to server
            connectTransport();
            // send using authentication
            sendAuth(messageToSend, headers);
            // disconnect from the server
            disconnectTransport();
        }
    }

    /**
     * Initialize the class reading the properties and creating an email
     * session, and transport
     *
     * @return true if the object could be properly initialized
     */
    private void init() throws MessagingException {

        this.mailConfig = new SantanderMailConfig();

        if (this.mailConfig.useAuthentication()) {

            createSession();
            // connectTransport();
        }
    }

    /*
     * Creation of a secure session
     */
    private void createSession() {

        final Properties props = new Properties();
        props.put("mail.smtp.host", this.mailConfig.getSecureHostName());
        props.put("mail.smtp.auth", Boolean.toString(true));

        final int port = this.mailConfig.getSecurePort();
        // GSM: if I don't added, default port 25 is used
        if ((port > -1) && (port != 0)) {
            props.put("mail.smtp.port", port);
        }
        this.session = Session.getInstance(props);

        if (Log.isDebug()) {
            this.session.setDebug(true);
        }
    }

    /*
     * Connect to smtp transport
     */
    private void connectTransport() {

        try {
            // if already connected, avoid this step
            if (transportIsConnected()) {
                return;
            }
            // othercase, connect to its
            this.transport = this.session.getTransport("smtp");
            // try to use port read from attributes
            final int port = this.mailConfig.getSecurePort();
            if ((port > -1) && (port != 0)) {
                // using port specified by attribute
                this.transport.connect(this.mailConfig.getSecureHostName(), port, this.mailConfig.getUser(),
                        this.mailConfig.getPassword());
            } else { // generic port 25
                this.transport.connect(this.mailConfig.getSecureHostName(), this.mailConfig.getUser(),
                        this.mailConfig.getPassword());
            }

        } catch (final AuthenticationFailedException ex) {
            Log.error(this, "Authentication failed with SMTP server: " + ex.getMessage());
            Log.error(this, ex); //sonar
        } catch (final MessagingException e) {
            Log.error(this, "Can not connect with SMTP server: " + e.getMessage());
            Log.error(this, e); //sonar
        }
    }

    /*
     * Verify if we are connected already
     */
    private boolean transportIsConnected() {

        return ((this.transport != null) && this.transport.isConnected());
    }

    /*
     * Disconnect email client from the server
     */
    private void disconnectTransport() {

        if (this.transport != null) {

            try {
                this.transport.close();

            } catch (final MessagingException e) {
                Log.error(this, "Exception closing transport: " + e.getMessage());
                Log.error(this, e); //sonar
            }
        }
    }

    /**
     * Sends the email message using smtp authenticate server
     *
     * @param messageToSend
     *            containing the full email
     * @throws MailException
     */
    private void sendAuth(EmailMessage messageToSend) throws MailException {

        // get the message in mime format
        final Message emailMessage = buildEmailMimeMessage(messageToSend);

        // send using authentication
        this.send(emailMessage);
    }

    /**
     * Sends the email message using smtp authenticate server
     *
     * @param messageToSend
     *            containing the full email
     * @param headers
     *            containing the map of headers to add to the email
     * @throws MailException
     * @throws MessagingException
     */
    private void sendAuth(EmailMessage messageToSend, HashMap<String,String> headers) throws MailException, MessagingException {

        // get the message in mime format
        final Message emailMessage = buildEmailMimeMessage(messageToSend, headers);

        // send using authentication
        this.send(emailMessage);
    }

    /**
     * send an email message using authenticate smtp protocol
     *
     * @param message
     *            message to be send
     * @return true if the message was sent, false in case of error
     * @throws MailException
     */
    public void send(final Message message) throws MailException {

        final String error = "Could not send the email: ";

        // in case it is not connected, try to connect first
        if (!transportIsConnected()) {
            connectTransport();
        }

        if (this.transport != null) {
            try {
                this.transport.sendMessage(message, message.getAllRecipients());
                Log.info(this, "Email subject " + message.getSubject() + " SENT.");
            } catch (final MessagingException ex) {

                Log.error(this, error + ex.getMessage());
                throw new MailException(new CalypsoException(ex), "Could not send the email");

            }
        }
    }

    /**
     * generates the Email container with the data included in the email message
     *
     * @param messageToSend
     *            : the email elements to be sent
     * @throws MailException
     *             thrown if there is something wrong while generating the
     *             MimeMessage
     */
    private Message buildEmailMimeMessage(EmailMessage messageToSend) throws MailException {

        try {
            // create the email
            final Message email = new MimeMessage(this.session);
            // set from property read from produban
            // GSM: 14/09/2013.
            /*
             * FROM from the user is replaced by the the sender FROM read from
             * the attributes. The FROM of the user is now added as a label
             */
            final String userFROM = messageToSend.getFrom().trim();
            final String realFROM = this.mailConfig.getFrom().trim();

            try {
                // real from + user from as a "Label"
                email.setFrom(new InternetAddress(realFROM, userFROM));
            } catch (UnsupportedEncodingException e) {

                Log.error(this, "FROM has an incorrect incoding." + e.getMessage());
                Log.error(this, e); //sonar
            }
            // GSM: 14/09/2013. Set replyTo - User FROM that will be the same as
            // the Label
            InternetAddress[] replyTo = EmailSender.getAdresses(Arrays.asList(userFROM));
            email.setReplyTo(replyTo);
            // set to property

            InternetAddress[] toAddress = EmailSender.getAdresses(messageToSend.getTo());
            InternetAddress[] ccAddress = EmailSender.getAdresses(messageToSend.getToCc());
            InternetAddress[] bCcAddress = EmailSender.getAdresses(messageToSend.getToBcc());

            email.setRecipients(Message.RecipientType.TO, toAddress);
            if ((ccAddress != null) && (ccAddress.length > 0)) {
                email.setRecipients(Message.RecipientType.CC, ccAddress);
            }
            if ((bCcAddress != null) && (bCcAddress.length > 0)) {
                email.setRecipients(Message.RecipientType.BCC, bCcAddress);
            }
            // set the subjet
            email.setSubject(messageToSend.getSubject());
            // set the email body text
            MimeBodyPart messageText = new MimeBodyPart();
            messageText.setContent(messageToSend.getText(), "text/html");
            // create the Multipart and add its parts to it
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(messageText);
            // set the attachments
            for (EmailMessage.Attachment attach : messageToSend.getAttachments()) {
                MimeBodyPart mbpAttach = new MimeBodyPart();
                // if the attachment has the final file to attach then use it
                // directly
                if (attach.getFile() != null) {
                    FileDataSource fds = new FileDataSource(attach.getFile());
                    mbpAttach.setDataHandler(new DataHandler(fds));
                } else {
                    mbpAttach.setDataHandler(new DataHandler(attach.getContent(), attach.getMimeType()));
                }
                mbpAttach.setFileName(attach.getAttachmentName());
                // attach the file to the message
                mp.addBodyPart(mbpAttach);
            }

            // add the Multipart to the message
            email.setContent(mp);
            // set the Date: header
            email.setSentDate(new Date());

            return email;

        } catch (MessagingException mex) {
            throw new MailException(new CalypsoException(mex),
                    "Could not send the email. Error generating MimeMessage");
        }
    }


    /**
     * generates the Email container with the data included in the email message
     *
     * @param messageToSend
     *            : the email elements to be sent
     * @param headers
     *          : the email headers to apply
     * @throws MailException
     *             thrown if there is something wrong while generating the
     *             MimeMessage
     */
    private Message buildEmailMimeMessage(EmailMessage messageToSend, HashMap<String,String> headers) throws MailException, MessagingException {
        Message message = buildEmailMimeMessage(messageToSend);
        for(String headerKey : headers.keySet()){
            message.setHeader(headerKey,headers.get(headerKey));
        }
        return message;
    }

    // some tests...
    // public boolean testEmail(String From, String etiqueta, String mensaje) {
    // try {
    // final Message email = new MimeMessage(this.session);
    // email.setFrom(new InternetAddress(From.trim(), etiqueta.trim()));
    // // Set replyTo - User from
    // InternetAddress[] replyTo =
    // EmailSender.getAdresses(Arrays.asList("pepitogrillo37@gmail.com"));
    // email.setReplyTo(replyTo);
    //
    // InternetAddress[] toAddress = EmailSender.getAdresses(Arrays.asList(
    // "guillermo.solano@servexternos.isban.es", "gusmartinez@produban.com"));
    // email.setRecipients(Message.RecipientType.TO, toAddress);
    // email.setSubject("prueba Correo");
    //
    // MimeBodyPart messageText = new MimeBodyPart();
    // messageText.setContent(mensaje, "text/html");
    // Multipart mp = new MimeMultipart();
    // mp.addBodyPart(messageText);
    // email.setContent(mp);
    //
    // try {
    // SantanderEmailSender t = new SantanderEmailSender();
    // t.send(email);
    // disconnectTransport();
    // return true;
    //
    // } catch (final Exception e) {
    // return false;
    // }
    // } catch (Exception e) {
    // }
    // return false;
    // }
    // end test
}
