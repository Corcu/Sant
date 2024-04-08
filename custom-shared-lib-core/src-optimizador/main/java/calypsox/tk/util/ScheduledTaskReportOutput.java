/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag.es) 
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.report.StandardReportOutput;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InstantiateUtil;

public class ScheduledTaskReportOutput extends com.calypso.tk.util.ScheduledTaskREPORT {

	private static final long serialVersionUID = 123L;

	private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyyMMdd");
	public static final String DELIMITEUR = "CSV Delimiter";
	public static final String REPORT_FORMAT = "REPORT FORMAT";
	// public static final String DATE_EXPORT = "Date to export";
	// public static final String FORMAT_EXPORT = "Format date to export";
	public static final String FEED_NAME = "Feed Name for TLM";
	public static final String FILE_ID = "File id for TLM";
	public static final String PO_NAME = "PO";
	// Indicate if the headings will be visible in first line(true for visible).
	public static final String SHOWHEADINGS = "Show Headings";
	public static final String START_HEADER = "Start Header";
	public static final String FOOTER = "Footer";
	public static final String CTRL_LINE = "Control Line";
	public static final String CONTRACT_TYPE = "Contract Type";
	public static final String DISCRIMINATE_CONTRACT_TYPE = "Exclude Contract Type";
	public static final String SEPARATOR_PROCESSING_ORG = "Separator for several POs";
	public static final String PO_LIST = "PO List for export LE";
	private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name"; // CAL_COLLAT_REPORT_0119
	public static final String DATAMART_FULL_EXTRACTION = "Full extraction to Datamart"; // for datamart extraction
																						 // full or dialy
	private int checkDelim = 0;
	/*
	 * GSM: 06/05/2013. Instead of contract type, LEs relations have to be excluded using the product subtype.
	 */
	public static final String SEPARATOR_PRODUCT_TYPES = "Separator for products types";
	public static final String PRODUCT_LIST = "Product list to export";

	public static final String PRODUCT_LIST_DOMAIN_VALUE = "productType";
	private static final String COLLATERAL_PRODUCT_COMMENT = "CollateralProduct";

	/*
	 * GSM: 17/05/2013. To choose entered quote date instead of quote date.
	 */
	public static final String USE_ENTERED_QUOTE_DATE = "Use Entered Quote Date TLM";

	/*
	 * GSM: 09/01/2014. Agreements types DV name
	 */
	private static final String MC_CONTRACTS = "legalAgreementType";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getDomainAttributes() {
		final Vector result = super.getDomainAttributes();
		result.add(DELIMITEUR);
		result.add(SHOWHEADINGS);
		// result.add(DATE_EXPORT);
		// result.add(FORMAT_EXPORT);
		result.add(FEED_NAME);
		result.add(FILE_ID);
		result.add(USE_ENTERED_QUOTE_DATE);
		result.add(PO_NAME);
		result.add(START_HEADER);
		result.add(FOOTER);
		result.add(CTRL_LINE);
		result.add(PRODUCT_LIST);
		result.add(SEPARATOR_PRODUCT_TYPES);
		result.add(PO_LIST);
		result.add(SEPARATOR_PROCESSING_ORG);
		result.add(DISCRIMINATE_CONTRACT_TYPE);
		result.add(CONTRACT_TYPE);
		result.add(QUOTENAME_DOMAIN_STRING); // CAL_COLLAT_REPORT_0119
		result.add(DATAMART_FULL_EXTRACTION);
		return result;
	}

	@Override
	protected String saveReportOutput(final ReportOutput reportOutput, String type, final String reportName,
			final String[] errors, final StringBuffer notifications) {
		final String delimiteur = getAttribute(DELIMITEUR);
		final String showheadings = getAttribute(SHOWHEADINGS);
		final String ctrlLine = getAttribute(CTRL_LINE);
		final String fileFormat = getAttribute(REPORT_FORMAT);

		boolean bShowHeadings = false;
		String type2 = "";
		// default will be showHeadings=false
		if ((showheadings != null) && showheadings.equals("true")) {
			bShowHeadings = true;
		} else {
			bShowHeadings = false;
		}

		Log.debug(Log.CALYPSOX, "Entering ScheduledTaskReport::reportViewer");

		if ((delimiteur == null) && !"Excel".equals(fileFormat) && (reportOutput instanceof StandardReportOutput)) {
			((StandardReportOutput) reportOutput).setDelimiteur("@");
			this.checkDelim = 1;
		}

		if ((reportOutput instanceof StandardReportOutput) && (delimiteur != null) && !delimiteur.equals("")) {
			((StandardReportOutput) reportOutput).setDelimiteur(delimiteur);
		}
		if (reportOutput instanceof StandardReportOutput) {
			((StandardReportOutput) reportOutput).setShowHeadings(bShowHeadings);
		}
		if (type.equals("txt")) {
			type2 = "txt"; // for KGR export
		}
		if (type.equals("dat")) {
			type2 = "dat"; // for KGR export
		}
		if (type.equals("txt") || type.equals("dat")) {
			type = "csv";
		}

		String reportStr = super.saveReportOutput(reportOutput, type, reportName, errors, notifications);

		// set extension
		String fileName = getFileName();
		if (fileName.startsWith("file://")) {
			fileName = fileName.substring(7);
		}

		if (type2.equals("txt")) {
			final String str1 = fileName.substring(0, fileName.lastIndexOf('.'));
			fileName = str1.concat(".txt");
		}

		if (type2.equals("dat")) {
			final String str1 = fileName.substring(0, fileName.lastIndexOf('.'));
			fileName = str1.concat(".dat");
		}

		// delete control separator for concrete cases (KGR)
		if (this.checkDelim == 1) {
			reportStr = removeDelimiteurs(reportStr, '@');
		}

		// add header and footer if is required
		if ((getAttribute(START_HEADER) != null) && (!getAttribute(START_HEADER).equals(""))) {
			reportStr = getAttribute(START_HEADER) + "\n" + reportStr;
		}

		if ((getAttribute(FOOTER) != null) && (!getAttribute(FOOTER).equals(""))) {
			reportStr = reportStr + getAttribute(FOOTER);
		}

		// generate report file, with line control if is required
		if ((ctrlLine != null) && (ctrlLine.equals("false"))) {
			return generateReportFile(reportOutput, reportStr, fileName, false);
		} else {
			return generateReportFile(reportOutput, reportStr, fileName, true);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
		Vector vector = new Vector();

		if (attribute.equals(QUOTENAME_DOMAIN_STRING)) { // CAL_COLLAT_REPORT_0119
			try {
				vector.addAll(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames());
			} catch (final RemoteException e) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving quotes name", e);
			}
		} else if (attribute.equals(CONTRACT_TYPE)) {
			// vector.addElement("CSA");
			// vector.addElement("ISMA");
			// vector.addElement("OSLA");
			vector.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), MC_CONTRACTS));
			vector.addElement("");
		} else if (attribute.equals(DISCRIMINATE_CONTRACT_TYPE)) {
			vector.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), MC_CONTRACTS));
			vector.addElement("");
		} else if (attribute.equals(SHOWHEADINGS)) {
			vector.addElement("true");
			vector.addElement("false");
		} else if (attribute.equals(REPORT_FORMAT)) {
			vector = super.getAttributeDomain(attribute, hashtable);
			vector.addElement("txt");
			vector.addElement("dat");
		} else if (attribute.equals(CTRL_LINE)) {
			vector = super.getAttributeDomain(attribute, hashtable);
			vector.addElement("true");
			vector.addElement("false");
		} else if (attribute.equals(USE_ENTERED_QUOTE_DATE)) {
			vector = super.getAttributeDomain(attribute, hashtable);
			vector.addElement("true");
			vector.addElement("false");

		} else if (attribute.equals(PRODUCT_LIST)) {
			addProductsToDomain(vector);

		} else if (attribute.equals(DATAMART_FULL_EXTRACTION)) {
			vector.addElement("true");
			vector.addElement("false");

		} else {
			vector = super.getAttributeDomain(attribute, hashtable);
		}
		return vector;
	}

	/*
	 * Reads product types from the domain values and adds them in the attributes vector
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addProductsToDomain(Vector vector) {

		Vector<String> domainValueProductTypes = LocalCache.getDomainValues(DSConnection.getDefault(),
				PRODUCT_LIST_DOMAIN_VALUE);

		Vector<String> optional = new Vector<String>(domainValueProductTypes.size());

		if ((domainValueProductTypes != null) && (domainValueProductTypes.size() > 0)) {

			for (int i = 0; i < domainValueProductTypes.size(); i++) {

				final String domainValue = domainValueProductTypes.get(i);

				if (!Util.isEmpty(domainValue)) {

					optional.add(domainValue);

					if (domainValue != null) {
						final String domainComment = LocalCache.getDomainValueComment(DSConnection.getDefault(),
								PRODUCT_LIST_DOMAIN_VALUE, domainValue);

						if (domainComment.equals(COLLATERAL_PRODUCT_COMMENT)) {
							vector.add(domainValue);
						}
					}
				}
			}

			if (vector.size() == 0) {
				vector.addAll(optional);
			}
		}
	}

	/**
	 * To generate the final control line
	 * 
	 * @param reportOutput
	 *            Output for the report.
	 * @return The text control line
	 */
	private String generateControlLine(final ReportOutput reportOutput) {
		String controlLine = "*****";
		controlLine = controlLine + String.format("%08d", reportOutput.getNumberOfRows())
				+ DATEFORMAT.format(getValuationDatetime().getJDate(TimeZone.getDefault()).getDate(this._timeZone));
		return controlLine;
	}

	/**
	 * To generate the report file
	 * 
	 * @param reportOutput
	 *            Output for the report.
	 * @param reportString
	 *            String with report data.
	 * @param fileName
	 *            String with report file name.
	 * @param ctrlLine
	 *            true for include control line, false for not include.
	 * @return The report text
	 */
	private String generateReportFile(final ReportOutput reportOutput, String reportString, final String fileName,
			final boolean ctrlLine) {
		if (ctrlLine) {
			final String controlLine = generateControlLine(reportOutput);
			reportString = reportString + controlLine;
		}
		try {
			final FileWriter writer = new FileWriter(fileName);
			writer.write(reportString);
			writer.close();
		} catch (final FileNotFoundException e) {
			Log.error(this, "The filename is not valid. Please configure the scheduled task with a valid filename: "
					+ fileName, e);
		} catch (final IOException e) {
			Log.error(this, "An error ocurred while writing the files: " + fileName, e);
		}
		return reportString;
	}

	String removeDelimiteurs(final String cadena, final char delimiteur) {
		// COL_OUT_016
		// Carlos Cejudo: This method has been changed to use StringBuilder class instead of a series of sums of
		// Strings. This saves memory in the system and improves the performance of the process.
		StringBuilder strToReturn = new StringBuilder();
		boolean valid = true;

		for (int i = 0; i < cadena.length(); i++) {
			valid = true;
			if (cadena.charAt(i) == delimiteur) {
				valid = false;
			}
			if (valid) {
				strToReturn.append(cadena.charAt(i));
			}
		}

		return strToReturn.toString();
	}

	@Override
	protected Report createReport(String type, String templateName, StringBuffer sb, PricingEnv env)
			throws java.rmi.RemoteException {

		Report report;

		try {
			String className = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(className, true);
			report.setPricingEnv(env);
			report.setFilterSet(this._tradeFilter);
			report.setValuationDatetime(getValuationDatetime());
			report.setUndoDatetime("true".equalsIgnoreCase(getAttribute(UNDO)) ? getUndoDatetime() : null);
		} catch (Exception e) {
			Log.error(this, e);
			report = null;
		}

		if ((report != null) && !Util.isEmpty(templateName)) {
			ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
					.getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template == null) {
				sb.append("Template " + templateName + " Not Found for " + type + " Report");
				Log.error(this, ("Template " + templateName + " Not Found for " + type + " Report"));
			} else {
				String generateHeaderStr = getAttribute(GENERATE_PDF_HEADER_B);
				if (Util.isEmpty(generateHeaderStr) || !generateHeaderStr.equalsIgnoreCase("false")) {
					generateHeaderStr = "true";
				}
				template.put(TradeReportTemplate.GENERATE_PDF_HEADER_B, generateHeaderStr);

				report.setReportTemplate(template);
				template.setValDate(getValuationDatetime().getJDate(this._timeZone));

				// Set Custom data to the Template here
				setTemplateCustomData(template);

				template.callBeforeLoad();
			}
		}

		return report;
	}

	/**
	 * Sets the custom data to the template
	 * 
	 * @param template
	 */
	public void setTemplateCustomData(ReportTemplate template) {

		if (null == template) {
			return;
		}

		template.setValDate(getValuationDatetime().getJDate(TimeZone.getDefault()));

		template.put(FEED_NAME, getAttribute(FEED_NAME));
		template.put(FILE_ID, getAttribute(FILE_ID));
		template.put(USE_ENTERED_QUOTE_DATE, getAttribute(USE_ENTERED_QUOTE_DATE));
		template.put(PO_NAME, getAttribute(PO_NAME));
		template.put(START_HEADER, getAttribute(START_HEADER));
		template.put(FOOTER, getAttribute(FOOTER));
		template.put(PRODUCT_LIST, getAttribute(PRODUCT_LIST));
		template.put(SEPARATOR_PRODUCT_TYPES, getAttribute(SEPARATOR_PRODUCT_TYPES));
		template.put(PO_LIST, getAttribute(PO_LIST));
		template.put(SEPARATOR_PROCESSING_ORG, getAttribute(SEPARATOR_PROCESSING_ORG));
		template.put(DISCRIMINATE_CONTRACT_TYPE, getAttribute(DISCRIMINATE_CONTRACT_TYPE));
		template.put(CONTRACT_TYPE, getAttribute(CONTRACT_TYPE));
		template.put(QUOTENAME_DOMAIN_STRING, getAttribute(QUOTENAME_DOMAIN_STRING)); // CAL_COLLAT_REPORT_0119
		template.put(DATAMART_FULL_EXTRACTION, getAttribute(DATAMART_FULL_EXTRACTION));
	}
}
