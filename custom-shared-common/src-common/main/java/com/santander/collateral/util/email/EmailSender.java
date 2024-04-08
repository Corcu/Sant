package com.santander.collateral.util.email;

import java.util.*;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.util.MailConfig;
import com.calypso.tk.util.email.MailException;

//GSM: Original class to send emails. Now it'll be used the SantanderEmailSender (similar, but using authenticated email).

/**
 * Utility class to define common email services. It will mainly defines email sending service with some special
 * features such as sending email with attachment (may be several attachments)</br> By default, the email service
 * configuration will be retrieved from the calypso configuration file (calypso_mail.properties using MailConfig
 * service).</br> Method send(EmailMessage messageToSend, Properties emailConfig) can be used to specify a different
 * configuration from the one specified in calypso_mail.properties file,
 *
 * @author aela
 */
public class EmailSender {

	/**
	 * Main method to send an email
	 *
	 * @param messageToSend
	 *            : the email elements to be sent
	 * @throws MailException
	 *             thrown if there is something wrong while sending an email
	 */
	public static void send(EmailMessage messageToSend) throws MailException {

		// send the email, new authenticated version
		try {
			SantanderEmailSender authServer = new SantanderEmailSender();
			authServer.send(messageToSend);

		} catch (Exception exc) {
			throw new MailException(new CalypsoException(exc), exc.getLocalizedMessage());
		}
	}

	/**
	 * Main method to send an email
	 *
	 * @param messageToSend
	 *            : the email elements to be sent
	 * @throws MailException
	 *             thrown if there is something wrong while sending an email
	 */
	public static void send(EmailMessage messageToSend, Properties emailConfig) throws MailException {
		try {
			Message email = buildEmailMimeMessage(messageToSend, emailConfig);
			// send the message
			Transport.send(email);

		}catch (MessagingException mex) {
			throw new MailException(new CalypsoException(mex), "Could not send the email");
		}
	}

	/**
	 * Main method to send an email if using headers
	 *
	 * @param messageToSend
	 *            : the email elements to be sent
	 * @param headers
	 * 			: key value pair of header attributes to apply to mail
	 * @throws MailException
	 *             thrown if there is something wrong while sending an email
	 */
	public static void sendWithHeaders(EmailMessage messageToSend, HashMap<String,String> headers) throws MailException, AddressException {
		try {
			Message email = buildEmailMimeMessage(messageToSend, headers);

			// send the message
			Transport.send(email);

		}catch (MessagingException mex) {
			throw new MailException(new CalypsoException(mex), "Could not send the email");
		}

	}

	/**
	 * generates the Email container with the data included in the email message and emailconfig properties
	 *
	 * @param messageToSend
	 *            : the email elements to be sent
	 */
	private static Message buildEmailMimeMessage(EmailMessage messageToSend, Properties emailConfig) throws MessagingException {
		// if a new email configuration is specified then use it, otherwise use
		// the default configuration
		Properties mailConf = emailConfig;
		if (mailConf == null) {
			mailConf = getMailConfProperties();
		}
		// initiate the email configuration
		Session session = Session.getDefaultInstance(mailConf);
		// create the email
		final MimeMessage email = new MimeMessage(session);

		// set from property
		email.setFrom(new InternetAddress(messageToSend.getFrom()));
		// set to property

		InternetAddress[] toAddress = getAdresses(messageToSend.getTo());
		InternetAddress[] ccAddress = getAdresses(messageToSend.getToCc());
		InternetAddress[] bCcAddress = getAdresses(messageToSend.getToBcc());

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
	}

	/**
	 * generates the Email container with the data included in the email message and emailconfig properties
	 *
	 * @param messageToSend
	 *            : the email elements to be sent
	 * @param headers
	 * 			: the email header to apply
	 */
	private static Message buildEmailMimeMessage(EmailMessage messageToSend, HashMap<String,String> headers) throws MessagingException {
		Message message = buildEmailMimeMessage(messageToSend, (Properties) null);
		for(String headerKey : headers.keySet()){
			message.setHeader(headerKey,headers.get(headerKey));
		}
		return message;
	}

	public static InternetAddress[] getAdresses(List<String> emailAddresses) throws AddressException {
		if (Util.isEmpty(emailAddresses)) {
			return null;
		}
		List<InternetAddress> internetAddress = new ArrayList<InternetAddress>();
		for (String addr : emailAddresses) {
			if (!Util.isEmpty(addr)) {
				internetAddress.add(new InternetAddress(addr));
			}

		}
		InternetAddress[] address = new InternetAddress[internetAddress.size()];
		internetAddress.toArray(address);
		return address;
	}

	/**
	 * @return the email configuration as Properties structure
	 */
	private static Properties getMailConfProperties() {
		Properties props = new Properties();
		props.setProperty("mail.smtp.host", MailConfig.getHostName());
		return props;
	}

}
