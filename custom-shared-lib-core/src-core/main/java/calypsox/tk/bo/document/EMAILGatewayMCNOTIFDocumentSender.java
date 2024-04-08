package calypsox.tk.bo.document;

import calypsox.apps.refdata.CustomBookValidator;
import calypsox.tk.bo.MarginCallMessageFormatter;
import calypsox.tk.bo.notification.SantInterestNotificationCache;
import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.*;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.MarginCallConstants;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.EMAILDocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.email.MailException;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Sender class for method EMAIL and for gateway MCNOTIF Margin Call email
 * notification sender. Creates the email, attachs the documents if the incoming
 * action requires it and Sends the email. Finally changes the final action of
 * the Message MC WF (from TO_BE_SENT to SENT/ERROR_SENT).
 *
 * @author VARIOUS
 * @version 3.0
 * @date 28/06/2016
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class EMAILGatewayMCNOTIFDocumentSender extends EMAILDocumentSender {

	/**
	 * Actions constants Message MC WF - MC_NOTIFICATION (to pass from TO_BE_SENT to
	 * SENT/ERROR_SENT).
	 */
	public static final String ERROR_SEND = "ERROR_SEND";
	public static final String SEND_AGAIN = "SEND_AGAIN";
	public static final String EVENT_TYPE_EMIR = "SEND_EMIR_COLLATERAL";
	public static final String EVENT_TYPE_PORTFOLIO_SEND = "SEND_PORTFOLIO_COLLATERAL";
	public static final String EVENT_TYPE_PORTFOLIO_REQUEST = "PORTFOLIO_REQUEST_COLLATERAL";
	public static final String VALUATION = " - Valuation ";
	public static final String PORTFOLIOREQUEST = " - Portfolio Request";

	public static final String EVENT_TYPE_BALANCE = "SEND_BALANCE_COLLATERAL";
	private static final String PDF_EXT = "PDF";
	private static final String MCNOTIF_FILE_NET = "MCNOTIFFileNet";

	// <Notice Type> <Type of Margin Call Contract>-<Cpty> - <Owner>
	public static final MessageFormat noticeSubject = new MessageFormat("{0} {1} - {2}");
	private static PricingEnv defaultPricingEnv = null;
	public static final String EMAIL_SEPARATOR = ";";
	private static final String PORTFOLIO_TEMPLATES_DV = "REPORT.PortfolioValuationTemplates";
	private static final String SANT_PORTFOLIO_BREAKDOWN_REPORT = "SantPortfolioBreakdown";
	private static final String BOOK_BUNDLE = CustomBookValidator.BOOK_BUNDLE;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.calypso.tk.bo.document.EMAILDocumentSender#send(com.calypso.tk.
	 * service .DSConnection, com.calypso.tk.bo.document.SenderConfig,
	 * com.calypso.tk.bo.document.SenderCopyConfig, int,
	 * com.calypso.tk.bo.document.AdviceDocument, java.util.Vector,
	 * com.calypso.tk.bo.BOMessage, java.util.Vector, java.lang.String, boolean[])
	 */
	@Override
	public boolean send(final DSConnection dsCon, final SenderConfig senderConfig,
			final SenderCopyConfig senderCopyConfig, final long eventId, final AdviceDocument adviceDocument,
			final Vector paramVector1, final BOMessage message, final Vector paramVector2, final String engineName,
			final boolean[] saved) {
		String templateTitle = message.getAttribute(MarginCallConstants.TRANS_MESSAGE_ATTR_TEMP_TITLE);
		if (Util.isEmpty(templateTitle)) {
			templateTitle = MarginCallMessageFormatter.getTitleFromTemplateName(message, dsCon);
		}
		message.setAttribute(MarginCallConstants.TRANS_MESSAGE_ATTR_TEMP_TITLE, null);

		// build the email message
		final EmailMessage email = new EmailMessage();

		final String recieverAddressCode = message.getReceiverAddressCode();
		ArrayList<String> racArray = new ArrayList<>(Arrays.asList(message.getReceiverAddressCode()));
		if (!Util.isEmpty(recieverAddressCode) && (recieverAddressCode.indexOf(EMAIL_SEPARATOR) > 0)) {
			racArray = new ArrayList<>(Arrays.asList(recieverAddressCode.split(EMAIL_SEPARATOR)));
		}

		if (Util.isEmpty(racArray)) {
			paramVector2.add("Unable to send notification: No destination address was defined for message with id "
					+ message.getLongId());
			return false;
		}

		MarginCallEntryDTO mce = null;
		try {
			mce = SantMarginCallUtil.getMarginCallEntryDTO(message, dsCon);
		} catch (final Exception e) {
			Log.error(this, e);
		}
		CollateralConfig mcc = null;
		LegalEntity po = null;
		LegalEntity cpty = null;
		try {
			po = BOCache.getLegalEntity(dsCon, message.getSenderId());
			cpty = BOCache.getLegalEntity(dsCon, message.getReceiverId());
			if (po == null) {
				paramVector2.add(
						"EMAILNOTIFDocumentSender Unable to get sender information for message" + message.getLongId());
				return false;
			}
			if (cpty == null) {
				paramVector2.add("EMAILNOTIFDocumentSender Unable to get reciever information for message"
						+ message.getLongId());
				return false;
			}

			if (SimpleTransfer.CUSTOMERTRANSFER.equals(message.getProductType())
					&& ("MC_INTEREST".equals(message.getMessageType()))) {
				Trade trade = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
				if (trade == null) {
					paramVector2.add("EMAILNOTIFDocumentSender Unable to get trade " + message.getTradeLongId());
					return false;
				}
				try {
					String mccIdStr = trade.getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
					mcc = CacheCollateralClient.getCollateralConfig(dsCon, Integer.parseInt(mccIdStr));
				} catch (Exception exc) {
					Log.error(this, exc); // sonar purpose
					paramVector2.add("EMAILNOTIFDocumentSender Unable to get contract for trade " + trade.getLongId());
					return false;
				}
			} else {
				mcc = CacheCollateralClient.getCollateralConfig(dsCon, message.getStatementId());
			}
		} catch (final Exception e) {
			Log.error(this, e);
			paramVector2.add(
					"EMAILNOTIFDocumentSender Unable to get the margin call detail for message " + message.getLongId());
			return false;
		}

		// use the default from address, instead of the one configured on the
		// message, since this one can have more than one email.
		final String fromAddress = MarginCallMessageFormatter.getFromAddress(message, mcc, dsCon);
		// check that the email address is filled
		if (Util.isEmpty(fromAddress)) {
			paramVector2.add("Unable to send notification: No from address was defined for message with id "
					+ message.getLongId());
			return false;
		}

		// Add receiver additional email part
		final LEContact receiverContact = BOCache.getLegalEntityContact(dsCon, message.getReceiverContactId());
		String additionalEmails = "";
		if ((receiverContact != null) && !Util.isEmpty(receiverContact.getAddressCode("ADDITIONAL_EMAIL"))) {
			additionalEmails = receiverContact.getAddressCode("ADDITIONAL_EMAIL");
		}

		if ((receiverContact != null) && !Util.isEmpty(receiverContact.getAddressCode("ADDITIONAL_EMAIL2"))) {
			if (!Util.isEmpty(additionalEmails)) {
				additionalEmails = additionalEmails + ";" + receiverContact.getAddressCode("ADDITIONAL_EMAIL2");
			} else {
				additionalEmails = receiverContact.getAddressCode("ADDITIONAL_EMAIL2");
			}
		}
		if ((receiverContact != null) && !Util.isEmpty(receiverContact.getAddressCode("ADDITIONAL_EMAIL3"))) {
			if (!Util.isEmpty(additionalEmails)) {
				additionalEmails = additionalEmails + ";" + receiverContact.getAddressCode("ADDITIONAL_EMAIL3");
			} else {
				additionalEmails = receiverContact.getAddressCode("ADDITIONAL_EMAIL3");
			}
		}

		if (!Util.isEmpty(additionalEmails)) {
			ArrayList<String> racAddtionalArray = new ArrayList<>(
					Arrays.asList(additionalEmails.split(EMAIL_SEPARATOR)));
			if (!Util.isEmpty(racAddtionalArray)) {
				racArray.addAll(racAddtionalArray);
			}
		}

		// set the email properties
		email.setFrom(fromAddress);
		email.setTo(racArray);
		email.setToBcc(Arrays.asList(fromAddress));
		if (message.getEventType().equals(EVENT_TYPE_EMIR)) {
			if (message.getTemplateName().equals(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_EMIR_BILATERAL)) {
				email.setSubject("EMIR - PortRec bilateral - " + po.getName() + " - CSA - " + cpty.getCode());
			} else {
				email.setSubject("EMIR - PortRec unilateral - " + po.getName() + " - CSA - " + cpty.getCode());
			}

		} else {
			if (message.getTemplateName().equals(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_NOTICE)) {
				String contractID = Integer.toString(message.getStatementId());
				email.setSubject(templateTitle + " " + mcc.getName() + " - " + po.getName() + " - " + contractID);
			} else if (message.getTemplateName().equals(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_BALANCE)) {
				email.setSubject(
						noticeSubject.format(new String[] { "Collateral Position", mcc.getName(), po.getName() }));
			} else if (message.getTemplateName()
					.equals(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_INTEREST_STATEMENT_NOTICE)
					&& mcc.getContractType().equals(MarginCallMessageFormatter.CONTRACT_TYPE_CGAR)) {
				email.setSubject(noticeSubject
						.format(new String[] { "REPORTE DE INTERESES POR GARANTIAS ENTREGADAS/RECIBIDAS ? ",
								mcc.getName(), getInterestDate(dsCon, message) }));

			} else if (message.getEventType().equals(EVENT_TYPE_PORTFOLIO_SEND)) {
				email.setSubject(po.getName() + " - " + mcc.getName() + VALUATION);
			} else if (message.getEventType().equals(EVENT_TYPE_PORTFOLIO_REQUEST)) {
				email.setSubject(po.getName() + " - " + mcc.getName() + PORTFOLIOREQUEST);
			} else {
				email.setSubject(noticeSubject.format(new String[] { templateTitle, mcc.getName(), po.getName() }));
			}
		}

		// get email body
		// to do so, we will use the calypso template framework

		String emailBody;
		try {
			emailBody = getNotificationEmailBody(message, dsCon);
		} catch (final Exception e) {
			Log.error(this, e);
			paramVector2.add("EMAILNOTIFDocumentSender Unable to get email text for message " + message.getLongId());
			return false;
		}
		email.setText(emailBody);

		if (Boolean.parseBoolean(message.getAttribute(MarginCallConstants.MESSAGE_ATTR_RESEND_MESSAGE))) {
			// in this case just get the already saved documents and send them
			try {
				email.resetAttachmentList();
				final Vector adviceDocuments = dsCon.getRemoteBackOffice()
						.getAdviceDocuments("advice_document.advice_id=" + message.getLongId(), null, null);
				// Filenet Gateway messages coulg have more than one AdviceDocument version
				if (!Util.isEmpty(adviceDocuments)) {
					if (message.getGateway().equals(MCNOTIF_FILE_NET)) {

						adviceDocuments.sort(Comparator.comparing(AdviceDocument::getDatetime).reversed()); // Order the documents
																									// by descending
																									// datetime
						email.addAttachment(getNotificationMimeType((AdviceDocument) adviceDocuments.get(0)),
								getNotificationFileName((AdviceDocument) adviceDocuments.get(0)),
								((AdviceDocument) adviceDocuments.get(0)).getBinaryDocument());
						if (Boolean.parseBoolean(
								message.getAttribute(MarginCallConstants.MESSAGE_ATTR_ATTACH_PORTFOLIO))) {
							for (int i = 1; i < adviceDocuments.size(); i++) {
								final AdviceDocument ad = (AdviceDocument) adviceDocuments.get(i);
								if (ad.getTemplateName().equals("PortfolioValuation.xlsx")) {
									email.addAttachment(getNotificationMimeType(ad), getNotificationFileName(ad),
											ad.getBinaryDocument());
									break;
								}

							}

						}

						if (Boolean.parseBoolean(
								message.getAttribute(MarginCallConstants.MESSAGE_ATTR_ATTACH_POSITIONS))) {
							for (int i = 1; i < adviceDocuments.size(); i++) {
								final AdviceDocument ad = (AdviceDocument) adviceDocuments.get(i);
								if (ad.getTemplateName().equals("CollateralPositions.xlsx")) {
									email.addAttachment(getNotificationMimeType(ad), getNotificationFileName(ad),
											ad.getBinaryDocument());
									break;
								}

							}

						}

						message.setAttribute(MarginCallConstants.MESSAGE_ATTR_RESEND_MESSAGE, "false");

					} else {
						for (int i = 0; i < adviceDocuments.size(); i++) {
							final AdviceDocument ad = (AdviceDocument) adviceDocuments.get(i);
							email.addAttachment(getNotificationMimeType(ad), getNotificationFileName(ad),
									ad.getBinaryDocument());
						}
					}
					// send the email,
					EmailSender.send(email);

					// apply the send action on the message
					try {
						saveMessage(message, engineName, SEND_AGAIN, "Updated by MCEMAILDocumentSender");
					} catch (Exception e1) {
						Log.error(this, "Error while saving message with action " + SEND_AGAIN, e1);
						paramVector2.add("Email was sent but error saving the message with action " + SEND_AGAIN);
					}
				}
				// don't save the advice document
				saved[0] = true;
			} catch (final MailException e) {
				Log.error(this, e);
				paramVector2.add("Error sending Email.");
				try {
					saveMessage(message, engineName, ERROR_SEND, "Updated by MCEMAILDocumentSender");
				} catch (Exception e1) {
					Log.error(this, e1); // sonar purpose
					Log.error(this, "Error while saving message with action " + ERROR_SEND, e);
				}
			} catch (Exception exc) {
				Log.error(this, exc);
				paramVector2.add("Unable to resend the message");
			}

		} else {
            if (!message.getEventType().equals(EVENT_TYPE_EMIR) && !message.getEventType().equals(EVENT_TYPE_BALANCE) && !message.getEventType().equals(EVENT_TYPE_PORTFOLIO_SEND)) {
				// set the generated notification as an attachment of the email
                email.addAttachment("application/pdf", getNotificationFileName(adviceDocument),
                        adviceDocument.getBinaryDocument());
			}
			// get the list of the marginCall trades and their details,
            AdviceDocument portfolioValuationDocument = null;
			if (Boolean.parseBoolean(message.getAttribute(MarginCallConstants.MESSAGE_ATTR_ATTACH_PORTFOLIO))) {

				if (mce != null) {
					byte[] finalXls = null;
					if (message.getEventType().equals(EVENT_TYPE_PORTFOLIO_SEND)) {
						finalXls = getEntryUnderlyingDetailsFromPortfolioBreakdownCustom(mce);
					} else {
						finalXls = getEntryUnderlyingDetailsFromPortfolioBreakdown(mce);
					}
					if (finalXls == null) {
						final String error = "EMAILGatewayMCNOTIFDocumentSender Unable to build the portfolio valuation for the margin call entry "
								+ mce.getId();
						Log.error(this, error);
						paramVector2.add(error);
						return false;
					}

					// build an incoming document with the generated xls file
					try {
						if (portfolioValuationDocument == null) {
							portfolioValuationDocument = (AdviceDocument) adviceDocument.clone();
							portfolioValuationDocument.setId(0);
							portfolioValuationDocument.setMimeType(new MimeType("application/vnd.ms-excel"));
							portfolioValuationDocument.setDocument(null);
							if (message.getEventType().equals(EVENT_TYPE_EMIR)) {
								portfolioValuationDocument.setTemplateName("Portfolio Breakdown.xlsx");
							} else if (message.getEventType().equals(EVENT_TYPE_PORTFOLIO_SEND)) {
								portfolioValuationDocument.setTemplateName("PortfolioBreakdownNotif.xlsx");
							} else {
								portfolioValuationDocument.setTemplateName("PortfolioValuation.xlsx");

							}
							portfolioValuationDocument.setBinaryDocument(finalXls);
							// set the document datetime before the notification
							// one,
							// this way the last document generated will be the
							// notification document !!
							portfolioValuationDocument.setDatetime(portfolioValuationDocument.getDatetime().add(-500));
						}
					} catch (final CloneNotSupportedException cnse) {
						Log.error(this, cnse);
						paramVector2.add(
								"EMAILGatewayMCNOTIFDocumentSender Unable to build the portfolio valuation for the margin call entry "
										+ mce.getId() + " " + Util.exceptionToString(cnse));
						return false;
					}
				}
			}
			// GSM: 16/10/2013. New Notification (SEND_PRT_PST) DDR/development
            AdviceDocument collateralPositionsDocument = null; // get the
            // securities &
            // cash
            // positions
			if (Boolean.parseBoolean(message.getAttribute(MarginCallConstants.MESSAGE_ATTR_ATTACH_POSITIONS))) {

				if (mce != null) {

					Boolean isSendBalance;

					if ("SEND_BALANCE_COLLATERAL".equals(message.getEventType())) {
						isSendBalance = true;
					} else {
						isSendBalance = false;
					}

					final byte[] finalXls = getEntryPositionsFromCollateralPositions(mce, isSendBalance);

					// build an incoming document with the generated xls file
					try {

							collateralPositionsDocument = (AdviceDocument) adviceDocument.clone();
							collateralPositionsDocument.setId(0);
							collateralPositionsDocument.setMimeType(new MimeType("application/vnd.ms-excel"));
							collateralPositionsDocument.setDocument(null);
							collateralPositionsDocument.setTemplateName("CollateralPositions.xlsx");
							collateralPositionsDocument.setBinaryDocument(finalXls);
							// set the document datetime before the notification
                        collateralPositionsDocument.setDatetime(collateralPositionsDocument.getDatetime().add(-1000));

					} catch (final CloneNotSupportedException cnse) {
						Log.error(this, cnse);
						paramVector2.add(
								"EMAILGatewayMCNOTIFDocumentSender Unable to build the Collateral Positions for the margin call entry "
										+ mce.getId() + " " + Util.exceptionToString(cnse));
						return false;
					}
				}
			}

			// save documents , message and send the email
			try {
				if (portfolioValuationDocument != null) {
					email.addAttachment(portfolioValuationDocument.getMimeType().getType(),
							getNotificationFileName(portfolioValuationDocument),
							portfolioValuationDocument.getBinaryDocument());
				}

				if (collateralPositionsDocument != null) {
					email.addAttachment(collateralPositionsDocument.getMimeType().getType(),
							getNotificationFileName(collateralPositionsDocument),
							collateralPositionsDocument.getBinaryDocument());
				}

				// send the email,
				EmailSender.send(email);

                if (Log.isCategoryLogged(this.getClass().getSimpleName())) {
                    Log.info(this.getClass().getSimpleName(), "Sending Collateral Positions - " + getXlsData(collateralPositionsDocument));
                }

				// apply the send action on the message
				try {
					// save the attachement
                    if (portfolioValuationDocument != null) {
						DSConnection.getDefault().getRemoteBO().save(portfolioValuationDocument);
					}
                    if (collateralPositionsDocument != null) {
						DSConnection.getDefault().getRemoteBO().save(portfolioValuationDocument);
					}
                    
                    saveMessage(message, engineName, Action.S_SEND, "Updated by MCEMAILDocumentSender");
				} catch (Exception e1) {
					Log.error(this, "Error while saving message with action " + Action.S_SEND, e1);
					paramVector2.add("Email was sent but error saving the message with action " + Action.S_SEND);
				}
				// Needed to let know the engine that event was proccesed
//				saved[0] = true;

			} catch (final MailException me) {
				Log.error(this, me);
				paramVector2.add("EMAILGatewayMCNOTIFDocumentSender Error " + me.getMessage() + " "
						+ Util.exceptionToString(me));
				try {
					saveMessage(message, engineName, ERROR_SEND, "Updated by MCEMAILDocumentSender");
				} catch (Exception exc) {
					Log.error(this, "Error while saving message with action " + ERROR_SEND, exc);
				}
			} catch (Exception exc) {
				Log.error(this, exc);
				paramVector2.add("Unable to send the message");
			}

		}

		return true;
	}

	/**
	 * Get the Interest Date
	 *
	 * @param dsCon
	 * @param message
	 * @return String interest end date
	 */
	private String getInterestDate(DSConnection dsCon, BOMessage message) {
		SantInterestNotificationCache s = new SantInterestNotificationCache();
		Trade trade = null;
		try {
			trade = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());
			JDate endDate = s.getEndDate(trade);

			final Locale locale = Util.getLocale(message.getLanguage());
			final DateFormat df = new SimpleDateFormat("MMM, yyyy", locale);
			return df.format(endDate.getDate(TimeZone.getDefault())).toUpperCase();
		} catch (CalypsoServiceException e) {
			Log.error(this, e);
		}
		return "";
	}

	/**
	 * Saves the message allowing to jump into the next state: SENT or ERROR_SENT
	 *
	 * @param message
	 * @param engineName
	 * @param action
	 * @param comment
	 * @throws Exception
	 */
    protected void saveMessage(BOMessage message, String engineName, String action, String comment) throws CalypsoServiceException, CloneNotSupportedException {
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
	 * @param transfer the trade
	 * @return true if sucess, false otherwise
	 */
	protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
	}

	/**
	 * @param entry (MC Contract being calculated today)
	 * @return the portfolio breakdown report for the specific contract
	 */
    protected byte[] getEntryUnderlyingDetailsFromPortfolioBreakdown(MarginCallEntryDTO entry) {

		if (entry == null) {
			return null;
		}

		CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
				entry.getCollateralConfigId());

		// GSM 19/02/2016 - Fix SantPortfolioBreakdown, get template by DV
		// configuration based on contract type
		// AAP DOESN'T SEEM TO CRASH
		final ReportTemplate template = getSantPortfolioReportTemplate(mcc);
		Set<Long> tradeIds = new HashSet<>();
		if (Util.isEmpty(entry.getDetailEntries())) {
			tradeIds.add(Long.valueOf(-1));
		} else {
			for (MarginCallDetailEntryDTO detail : entry.getDetailEntries()) {
				tradeIds.add(detail.getTradeId());
			}
		}

		if (template != null) {
			template.put(SantPortfolioBreakdownReportTemplate.TRADE_IDS_FOR_EMAIL_GATEWAY_NOTIF, tradeIds);
			template.put(SantGenericTradeReportTemplate.AGREEMENT_ID, String.valueOf(mcc.getId()));
			template.put(TradeReportTemplate.START_PLUS, "+");
			template.put(TradeReportTemplate.START_TENOR, "0D");
			Report report = Report.getReport("SantPortfolioBreakdown");
			report.setReportTemplate(template);
			report.setValuationDatetime(entry.getValueDatetime());

			ReportOutput output = report.load(new Vector());

			SantExcelReportViewer viewer = new SantExcelReportViewer();
			output.format(viewer);

			try {
				return viewer.getContentAsBytes();
			} catch (IOException e) {
				Log.error(this, e);
			}
		}

		return null;
	}

	/**
	 * @param entry (MC Contract being calculated today)
	 * @return the portfolio breakdown report for the specific contract
	 */
	private byte[] getEntryUnderlyingDetailsFromPortfolioBreakdownCustom(MarginCallEntryDTO entry) {

		if (entry == null) {
			return null;
		}

		CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
				entry.getCollateralConfigId());

		// GSM 19/02/2016 - Fix SantPortfolioBreakdown, get template by DV
		// configuration based on contract type
		// AAP DOESN'T SEEM TO CRASH
		final ReportTemplate template = getSantPortfolioReportTemplateCustom(mcc);
		Set<Long> tradeIds = new HashSet<>();
		if (Util.isEmpty(entry.getDetailEntries())) {
			tradeIds.add(Long.valueOf(-1));
		} else {
			for (MarginCallDetailEntryDTO detail : entry.getDetailEntries()) {
				tradeIds.add(detail.getTradeId());
			}
		}

		if (template != null) {
			template.put(SantPortfolioBreakdownReportTemplate.TRADE_IDS_FOR_EMAIL_GATEWAY_NOTIF, tradeIds);
			template.put(SantGenericTradeReportTemplate.AGREEMENT_ID, String.valueOf(mcc.getId()));
			template.put(TradeReportTemplate.START_PLUS, "+");
			template.put(TradeReportTemplate.START_TENOR, "0D");
			Report report = Report.getReport("SantPortfolioBreakdown");
			report.setReportTemplate(template);
			report.setValuationDatetime(entry.getValueDatetime());

			ReportOutput output = report.load(new Vector());

			SantExcelReportViewer viewer = new SantExcelReportViewer();
			output.format(viewer);

			try {
				return viewer.getContentAsBytes();
			} catch (IOException e) {
				Log.error(this, e);
			}
		}

		return null;
	}

	/**
	 * @param mcc contract
	 * @return Sant portfolio appropiate template based on DV configuration by
	 *         contract type
	 */
	public ReportTemplate getSantPortfolioReportTemplate(CollateralConfig mcc) {

		Map<String, String> templateDomainMap = CollateralUtilities.initDomainValueComments(PORTFOLIO_TEMPLATES_DV);
		if (Util.isEmpty(templateDomainMap)) {
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class,
					"Missing DV Configuration - The EMAIL wont be sent. Please, set " + PORTFOLIO_TEMPLATES_DV);
			return null;
		}

		// GSM 15/03/2016 - Option by book bundle
		String templateName = "";
		LegalEntity poOwner = mcc.getProcessingOrg();
		final String leAttrBookBundle = CollateralUtilities.getAttributeValueFromLE(DSConnection.getDefault(), poOwner,
				BOOK_BUNDLE);

		// search template by CONTRACT_TYPE/BookBundle
		if (!Util.isEmpty(leAttrBookBundle)) {

			for (Map.Entry<String, String> entry : templateDomainMap.entrySet()) {

				final String value = entry.getKey();
				if (value.contains("/")) {

					final String attDVBookBundle = value.substring(value.lastIndexOf("/") + 1);
					final String attDVContractType = value.substring(0, value.indexOf("/"));
					if (attDVBookBundle.trim().equals(leAttrBookBundle)
							&& attDVContractType.trim().equals(mcc.getContractType())) {
						templateName = entry.getValue();
						break;
					}
				}
			}
		}

		// default, search generic templates only by contract type
		if (Util.isEmpty(templateName)) {
			templateName = templateDomainMap.get(mcc.getContractType());
		}

		if (Util.isEmpty(templateName)) {
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class, "Missing DV in " + PORTFOLIO_TEMPLATES_DV
					+ ". Contract not found for type: " + mcc.getContractType());
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class,
					"The EMAIL wont be sent. Please, set the contract type configuration & template as comment. ");
			return null;
		}

		ReportTemplateName reportTemplateName = new ReportTemplateName(templateName.trim());
		ReportTemplate template = BOCache.getReportTemplate(DSConnection.getDefault(), SANT_PORTFOLIO_BREAKDOWN_REPORT,
				reportTemplateName);

		if (template == null) {
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class,
					"The EMAIL wont be sent. Please, Template not correctly configured in DV "
							+ PORTFOLIO_TEMPLATES_DV);
			return null;
		}

		return template;
	}

	/**
	 * @param mcc contract
	 * @return Sant portfolio appropiate template based on DV configuration by
	 *         contract type
	 */
	public ReportTemplate getSantPortfolioReportTemplateCustom(CollateralConfig mcc) {

		Map<String, String> templateDomainMap = CollateralUtilities.initDomainValueComments(PORTFOLIO_TEMPLATES_DV);
		if (Util.isEmpty(templateDomainMap)) {
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class,
					"Missing DV Configuration - The EMAIL wont be sent. Please, set " + PORTFOLIO_TEMPLATES_DV);
			return null;
		}

		// GSM 15/03/2016 - Option by book bundle
		String templateName = "";
		LegalEntity poOwner = mcc.getProcessingOrg();
		final String leAttrBookBundle = CollateralUtilities.getAttributeValueFromLE(DSConnection.getDefault(), poOwner,
				BOOK_BUNDLE);

		// search template by CONTRACT_TYPE/BookBundle
		if (!Util.isEmpty(leAttrBookBundle)) {

			for (Map.Entry<String, String> entry : templateDomainMap.entrySet()) {

				final String value = entry.getKey();
				if (value.contains("/")) {

					final String attDVBookBundle = value.substring(value.lastIndexOf("/") + 1);
					final String attDVContractType = value.substring(0, value.indexOf("/"));
					if (attDVBookBundle.trim().equals(leAttrBookBundle)
							&& attDVContractType.trim().equals(mcc.getContractType())) {
						templateName = entry.getValue();
						break;
					}
				}
			}
		}

		// default, search generic templates only by contract type
		if (Util.isEmpty(templateName)) {
			templateName = templateDomainMap.get(mcc.getContractType());
		}

		if (Util.isEmpty(templateName)) {
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class, "Missing DV in " + PORTFOLIO_TEMPLATES_DV
					+ ". Contract not found for type: " + mcc.getContractType());
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class,
					"The EMAIL wont be sent. Please, set the contract type configuration & template as comment. ");
			return null;
		}

		ReportTemplateName reportTemplateName = new ReportTemplateName(templateName.trim() + "_Notif");
		ReportTemplate template = BOCache.getReportTemplate(DSConnection.getDefault(), SANT_PORTFOLIO_BREAKDOWN_REPORT,
				reportTemplateName);

		if (template == null) {
			Log.error(EMAILGatewayMCNOTIFDocumentSender.class,
					"The EMAIL wont be sent. Please, Template not correctly configured in DV "
							+ PORTFOLIO_TEMPLATES_DV);
			return null;
		}

		return template;
	}

	// GSM: 16/10/2013. New Notification (SEND_PRT_PST) DDR/development

	/**
	 * Recovers the CollateralPositions from teh SantCollateralPosition Report.
	 *
	 * @param MC Entry
	 * @return the formatted excel from the report
	 */
	protected byte[] getEntryPositionsFromCollateralPositions(MarginCallEntryDTO entry, Boolean isSendBalance) {

		if (entry == null) {
			return null;
		}

		CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
				entry.getCollateralConfigId());

		final String templateName, reportName;

		if (isSendBalance) {
			// name of the Position template for Securities
			templateName = "SantCollateralPositionSendBalanceTemplate";
			// report name
			reportName = "SantCollateralPositionSendBalance";

			final SantCollateralPositionSendBalanceReportTemplate template = new SantCollateralPositionSendBalanceReportTemplate();

			template.setTemplateName(templateName);

			// MC Contract ID
			template.put(SantGenericTradeReportTemplate.AGREEMENT_ID, String.valueOf(mcc.getId()));
			// // start plus and tenor
			template.put(TradeReportTemplate.START_PLUS, "+");
			template.put(TradeReportTemplate.START_TENOR, "0D");
			// // template.put(TradeReportTemplate.START_DATE,
			// JDate.valueOf(entry.getProcessDatetime()).toString());
			// // end plus and tenor
			template.put(TradeReportTemplate.END_PLUS, "+");
			template.put(TradeReportTemplate.END_TENOR, "0D");
			// set star and end date
			template.setStartEndDate(JDate.valueOf(entry.getProcessDatetime()));
			// this is to force the new columns names
			template.resetColumnsNames();

			// get report from DS
			Report report = Report.getReport(reportName);
			report.setReportTemplate(template);

			// Process day of the MC Entry
			report.setValuationDatetime(entry.getProcessDatetime());

			// generate report
			final ReportOutput output = report.load(new Vector());

            if (Log.isCategoryLogged(this.getClass().getSimpleName())) {
                Log.info(this.getClass().getSimpleName(), "Loading Collateral Positions - " + getBalances((DefaultReportOutput) output));
            }
			// format to excel
			final SantExcelReportViewer viewer = new SantExcelReportViewer();
			// set the sheet name
			viewer.setSheetName("CollateralPositions");
			output.format(viewer);

			try {
				return viewer.getContentAsBytes();
			} catch (IOException e) {
				Log.error(this, e);
			}

		} else {
			// name of the Position template for Securities
			templateName = "SantCollateralBOPositionTemplate";
			// report name
			reportName = "SantCollateralBOPosition";

			final ReportTemplateName reportTemplateName = new ReportTemplateName(templateName);

			final SantCollateralBOPositionReportTemplate template = (SantCollateralBOPositionReportTemplate) BOCache
					.getReportTemplate(DSConnection.getDefault(), reportName, reportTemplateName);

			// MC Contract ID
			template.put(SantGenericTradeReportTemplate.AGREEMENT_ID, String.valueOf(mcc.getId()));
			// // start plus and tenor
			template.put(TradeReportTemplate.START_PLUS, "+");
			template.put(TradeReportTemplate.START_TENOR, "0D");
			// // end plus and tenor
			template.put(TradeReportTemplate.END_PLUS, "+");
			template.put(TradeReportTemplate.END_TENOR, "0D");
			// set star and end date
			template.setStartEndDate(JDate.valueOf(entry.getProcessDatetime()));
			// this is to force the new columns names
			template.resetColumnsNames();

			// get report from DS
			Report report = Report.getReport(reportName);
			report.setReportTemplate(template);

			// Process day of the MC Entry
			report.setValuationDatetime(entry.getProcessDatetime());

			// generate report
			final ReportOutput output = report.load(new Vector());

			// format to excel
			final SantExcelReportViewer viewer = new SantExcelReportViewer();
			// set the sheet name
			viewer.setSheetName("CollateralPositions");
			output.format(viewer);

			try {
				return viewer.getContentAsBytes();
			} catch (IOException e) {
				Log.error(this, e);
			}
		}

		return null;
	}

	/**
	 * @param entry
	 * @return a xls file as a string for the detail of the entry underlying
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	// old
	@Deprecated
	private byte[] getEntryUnderlyingDetails(MarginCallEntryDTO entry) {
		byte[] result = null;

		if (entry != null) {
			SantMCPortfolioReportTemplate template = null;
			template = (SantMCPortfolioReportTemplate) ReportTemplate.getReportTemplate(SantMCPortfolioReport.TYPE);

			List<MarginCallDetailEntryDTO> entries = entry.getDetailEntries();
			if (Util.isEmpty(entries)) {
				if (entries == null) {
					entries = new ArrayList<>();
				}
				entries.add(new MarginCallDetailEntryDTO());
			}
			template.setDetailEntries(entry.getDetailEntries());

			Report report = Report.getReport(SantMCPortfolioReport.TYPE);
			report.setReportTemplate(template);
			ReportOutput output = report.load(new Vector());

			SantExcelReportViewer viewer = new SantExcelReportViewer();
			output.format(viewer);

			try {
				result = viewer.getContentAsBytes();
			} catch (IOException e) {
				Log.error(this, e);
				result = null;
			}

		}
		return result;
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
		if (message.getEventType().equals(EVENT_TYPE_EMIR)) {
			if (message.getTemplateName().equals(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_EMIR_BILATERAL)) {
				clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_EMIR_BILATERAL);
			} else {
				clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_EMIR_UNILATERAL);
			}
		} else if (message.getEventType().equals(EVENT_TYPE_BALANCE)) {
			clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_BALANCE);
		} else if (message.getEventType().equals(EVENT_TYPE_PORTFOLIO_SEND)) {
			clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE_COLLATERALDEAL);
		} else {
			clonedMessage.setTemplateName(MarginCallMessageFormatter.NOTIF_EMAIL_TEMPLATE);
			clonedMessage.setAttribute("ORIG_TEMPLATE", message.getTemplateName());
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

    private String getBalances(DefaultReportOutput output) {
        StringBuilder result = new StringBuilder();
        if (null != output && output.getNumberOfRows() > 0) {
			result.append("Contract ID, Nominal").append("\n");
            for (ReportRow row : output.getRows()) {
                SantCollateralPositionSendBalanceItem balanceItem = row.getProperty("SantCollateralPositionSendBalanceItem");
                result.append(balanceItem.getContractID()).append(", ").append(balanceItem.getNominal()).append("\n");
            }
        }
        return result.toString();
    }

    public String getXlsData(AdviceDocument collateralPositionsDocument) throws IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        StringBuilder sb = new StringBuilder("Extracting xls data...\n");
        if (null != collateralPositionsDocument) {
            InputStream inputStream = new ByteArrayInputStream(collateralPositionsDocument.getBinaryDocument());
            Workbook book = WorkbookFactory.create(inputStream);
            Iterator it = book.sheetIterator();
            while (it.hasNext()) {
                final Sheet item = (Sheet) it.next();
                Iterator it2 = item.rowIterator();
                while (it2.hasNext()) {
                    final org.apache.poi.ss.usermodel.Row item2 = (org.apache.poi.ss.usermodel.Row) it2.next();
                    sb.append(item2.getCell(0)).append(", ").append(item2.getCell(5)).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
