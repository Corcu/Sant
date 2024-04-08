package calypsox.tk.bo.workflow.rule;

import java.util.Properties;
import java.util.Vector;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MailConfig;
import com.santander.collateral.util.email.SantanderMailConfig;

/**
 * Rule to send Threshold information mail
 *
 * @author mgarcsan
 */
public class SantSendMailGRMCollateralRule extends SantCheckGRMCollateralRule {

	private static final String THRESHOLD_GRM_MAIL_TO = "thresholdGRM_mailTo";
	private static final boolean SMTP_AUTHENTICATED = true;

	/**
	 * Returns the rule description
	 *
	 * @return
	 */
	@Override
	public String getDescription() {
		return "This rule send a mail if the threshold limit has been violated creates an information task";
	}

	/**
	 * Applies the action sending the information mail
	 */
	protected WorkflowResult apply(TaskWorkflowConfig taskConfig,
			MarginCallEntry entry, DSConnection dsCon) {
		Log.info(SantCheckGlobalMTACollateralRule.class,
				"SantSendMailGRMCollateralRule apply - Start");

		WorkflowResult wfr = new WorkflowResult();

		if (checkThreshold(entry)) {

			try {
				SantanderMailConfig config = new SantanderMailConfig();
				Properties properties = getMailProperties(config);
				MimeMessage message = createEmail(entry, taskConfig, properties);

				Session session = Session.getInstance(properties);
				Transport transport = session.getTransport("smtp");

				transport.connect(config.getUser(), config.getPassword());

				transport.sendMessage(message, message.getAllRecipients());

				transport.close();

			} catch (MessagingException exception) {
				Log.error(SantSendMailGRMCollateralRule.class,
						exception.getMessage());
				exception.printStackTrace();
			}
		}

		wfr.success();
		return wfr;

	}

	/**
	 * Create Email
	 */
	private MimeMessage createEmail(MarginCallEntry entry,
			TaskWorkflowConfig config, Properties properties)
			throws AddressException, MessagingException {
		Log.info(this, "SantSendMailGRMCollateralRule.createEmail");

		final Session session = Session.getDefaultInstance(properties);

		MimeMessage message = new MimeMessage(session);

		// from
		message.setFrom(new InternetAddress(MailConfig.getFromMailAddress()));
		// to
		setTo(message, RecipientType.TO);

		// Email Subject with Valuation Date
		message.setSubject("Threshold limit violation");

		// Create Email Body
		message.setContent(createBody(entry));

		return message;

	}

	/**
	 * Sets the to/bcc/cc.
	 *
	 * @param emailAddresses
	 *            the new to
	 * @throws AddressException
	 *             the address exception
	 * @throws MessagingException
	 *             the messaging exception
	 */
	public void setTo(MimeMessage message, final RecipientType recipientType)
			throws AddressException, MessagingException {

		Vector<String> mailDestinations = LocalCache.getDomainValues(
				DSConnection.getDefault(), THRESHOLD_GRM_MAIL_TO);

		if (null != mailDestinations && !mailDestinations.isEmpty()) {

			for (final String addr : mailDestinations) {
				final InternetAddress iAddr = new InternetAddress(addr.trim());
				message.addRecipient(recipientType, iAddr);

			}

		}
	}

	/**
	 * Gets the mail properties.
	 *
	 * @return the mail properties
	 */
	private Properties getMailProperties(SantanderMailConfig config) {
		Log.info(this, "SantSendMailGRMCollateralRule.getMailProperties");

		Properties props = new Properties();

		props.put("mail.smtp.host", config.getSecureHostName());
		props.put("mail.smtp.port", config.getSecurePort() + "");
		props.put("mail.smtp.mail.sender",
				SantanderMailConfig.getFromMailAddress());
		props.put("mail.smtp.user", config.getUser());
		props.put("mail.smtp.auth", SMTP_AUTHENTICATED);

		return props;
	}

	/**
	 * Creates the mail body.
	 *
	 * @param path
	 */
	public Multipart createBody(MarginCallEntry entry)
			throws MessagingException {
		Log.info(this, "SantSendMailGRMCollateralRule.createBody");

		// Create the message part
		final Multipart multipart = new MimeMultipart();
		final MimeBodyPart htmlPart = new MimeBodyPart();

		htmlPart.setContent(
				"<p>Margin call " + entry.getCollateralConfigId()
						+ " Global Required Margin ("
						+ Math.abs(entry.getGlobalRequiredMargin())
						+ ") has passed the threshold amount ("
						+ Math.abs(entry.getThresholdAmount()) + ")</p>",
				"text/html; charset=utf-8");

		multipart.addBodyPart(htmlPart);

		return multipart;

	}
	
}