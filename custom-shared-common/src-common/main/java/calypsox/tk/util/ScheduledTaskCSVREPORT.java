/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag.es)
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.report.StandardReportOutput;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.email.MailException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Custom version to run reports as batches. Adapted to format core reports too
 * and to use POs filters (by core combo or attribute)
 *
 * @version 3.0
 * @date 23/07/2015
 */
public class ScheduledTaskCSVREPORT extends com.calypso.tk.util.ScheduledTaskREPORT {

    private static final long serialVersionUID = 123L;

    private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyyMMdd");
    public static final String DELIMITEUR = "CSV Delimiter";
    public static final String REPORT_FORMAT = "REPORT FORMAT";
    public static final String FEED_NAME = "Feed Name for TLM";
    public static final String FILE_ID = "File id for TLM";
    public static final String PO_NAME = "PO";
    public static final String SHOWHEADINGS = "Show Headings";
    public static final String USE_PIVOT_TABLE = "CSV Use Pivot Table";
    public static final String START_HEADER = "Start Header";
    public static final String FOOTER = "Footer";
    public static final String CTRL_LINE = "Control Line";
    public static final String CONTRACT_TYPE = "Contract Type";
    public static final String DISCRIMINATE_CONTRACT_TYPE = "Exclude Contract Type";
    public static final String SEPARATOR_PROCESSING_ORG = "Separator for several POs";
    public static final String PO_LIST = "PO List for export LE";
    public static final String CUSTOM_EMAIL_LIST="CUSTOM SEND EMAIL TO";
    public static final String CUSTOM_SEND_MAIL_SUBJECT="CUSTOM SEND MAIL SUBJECT";
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";


    private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name"; // CAL_COLLAT_REPORT_0119
    // for datamart extraction full or dialy
    public static final String DATAMART_FULL_EXTRACTION = "Full extraction to Datamart";
    // GSM: 06/05/2013. Instead of contract type, LEs relations have to be
    // excluded using the product subtype.
    public static final String SEPARATOR_PRODUCT_TYPES = "Separator for products types";
    public static final String PRODUCT_LIST = "Product list to export";
    public static final String PRODUCT_LIST_DOMAIN_VALUE = "productType";
    //private static final String COLLATERAL_PRODUCT_COMMENT = "CollateralProduct";
    // GSM: 17/05/2013. To choose entered quote date instead of quote date.
    public static final String USE_ENTERED_QUOTE_DATE = "Use Entered Quote Date TLM";
    // GSM: 09/01/2014. Agreements types DV name
    private static final String MC_CONTRACTS = "legalAgreementType";
    private int checkDelim = 0;
    // GSM 23/07/15. SBNA Multi-PO filter
    public static final String PROCESS_BY_ST = "ReportProcessedByST";
    public static final String COMPRESSED_FILE_ATTR="Compressed File";
    public static final String TXT_FORMAT = "txt";
    public static final String DAT_FORMAT = "dat";
    private static final String GZ_FORMAT="gz";

    //v14 - AAP
    @SuppressWarnings("unchecked")
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(CUSTOM_EMAIL_LIST));
        attributeList.add(attribute(CUSTOM_SEND_MAIL_SUBJECT));
        attributeList.add(attribute(DELIMITEUR));
        attributeList.add(attribute(SHOWHEADINGS).booleanType());
        attributeList.add(attribute(USE_PIVOT_TABLE).booleanType());
        attributeList.add(attribute(FEED_NAME));
        attributeList.add(attribute(FILE_ID));
        attributeList.add(attribute(USE_ENTERED_QUOTE_DATE).booleanType());
        attributeList.add(attribute(PO_NAME));
        attributeList.add(attribute(START_HEADER));
        attributeList.add(attribute(FOOTER));
        attributeList.add(attribute(CTRL_LINE).booleanType());
        attributeList.add(attribute(PRODUCT_LIST).domainName(PRODUCT_LIST_DOMAIN_VALUE));
        attributeList.add(attribute(SEPARATOR_PRODUCT_TYPES));
        attributeList.add(attribute(PO_LIST));
        attributeList.add(attribute(SEPARATOR_PROCESSING_ORG));
        attributeList.add(attribute(DISCRIMINATE_CONTRACT_TYPE).domainName(MC_CONTRACTS));
        attributeList.add(attribute(CONTRACT_TYPE).domainName(MC_CONTRACTS));
        attributeList.add(attribute(COMPRESSED_FILE_ATTR).booleanType());
        try {
            attributeList.add(attribute(QUOTENAME_DOMAIN_STRING)
                    .domain(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames()));
        } catch (CalypsoServiceException e) {
            Log.error(ScheduledTask.LOG_CATEGORY, "Error while retrieving quotes name", e);
        }
        attributeList.add(attribute(DATAMART_FULL_EXTRACTION).booleanType());
        return attributeList;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
        Vector vector = new Vector();
        vector = super.getAttributeDomain(attribute, hashtable);
        if (attribute.equals(REPORT_FORMAT)) {
            vector = super.getAttributeDomain(attribute, hashtable);
            vector.addElement(TXT_FORMAT);
            vector.addElement(DAT_FORMAT);
        }
        return vector;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Vector getDomainAttributes() {
        final Vector result = super.getDomainAttributes();
        result.add(CUSTOM_EMAIL_LIST);
        result.add(CUSTOM_SEND_MAIL_SUBJECT);
        result.add(COMPRESSED_FILE_ATTR);
        result.add(DELIMITEUR);
        result.add(SHOWHEADINGS);
        result.add(USE_PIVOT_TABLE);
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

    /**
     * Call BEFORE creating the report.
     */
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

            final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                    .getReportTemplate(ReportTemplate.getReportName(type), templateName);

            if (template == null) {

                sb.append("Template " + templateName + " Not Found for " + type + " Report");
                Log.error(this, ("Template " + templateName + " Not Found for " + type + " Report"));

            } else {
				addScheduledTaskHolidays(template);
                // GSM 23/07/15. SBNA Multi-PO filter
                addPOsAttributesReportTemplate(template);

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

	protected void addScheduledTaskHolidays(ReportTemplate template) {
		Log.info(ScheduledTask.LOG_CATEGORY, "Not defined method addScheduledTaskHolidays");

	}

    /**
     * Inserts the list of POs to filter. If core combo is used, it will filter
     * by the PO selected, otherwise, by the set selected in the attribute PO
     *
     * @param reportTemplate
     */
    private void addPOsAttributesReportTemplate(final ReportTemplate reportTemplate) {

        final StringBuffer sbPosListCodes = new StringBuffer();
        final StringBuffer sbPosListIds = new StringBuffer();
        final LegalEntity po = getProcessingOrg();
        // using core filter
        if (po != null) {
            sbPosListCodes.append(po.getCode());
            sbPosListIds.append(po.getId());
            // for some core reports. E.a. CollateralConfig or Message
            reportTemplate.put("Processing Org", po.getCode());
            reportTemplate.put("POName", po.getCode());

        } else {
            // otherwise, use parameters
            String posAttribute = super.getAttribute(PO_NAME);
            if (!Util.isEmpty(posAttribute)) {

                posAttribute = posAttribute.replaceAll(" ", "");
                posAttribute = posAttribute.replaceAll(";", ",");
                sbPosListCodes.append(posAttribute);
                Vector<String> v = Util.string2Vector(posAttribute);

                for (String poString : v) {

                    LegalEntity legalentity = BOCache.getLegalEntity(DSConnection.getDefault(), poString);

                    if (legalentity != null) {
                        sbPosListIds.append(legalentity.getId()).append(",");

                    } else {
                        Log.error(ScheduledTask.LOG_CATEGORY, "NOT FOUND PO in attribute " + v);
                    }
                }

                if (sbPosListIds.length() > 0) {
                    sbPosListIds.deleteCharAt(sbPosListIds.length() - 1);
                }
            }
        }
        // mark to indicate it has been processed by this ST
        reportTemplate.put(PROCESS_BY_ST, PROCESS_BY_ST);
        reportTemplate.put("ScheduledTaskExternalReference", this.getExternalReference());

        if (sbPosListCodes.length() > 0) {
            reportTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG, sbPosListCodes.toString());
            reportTemplate.put("OWNER_AGR_IDS", sbPosListIds.toString());
            Log.info(ScheduledTask.LOG_CATEGORY, "Added to template filter by PO: " + sbPosListCodes.toString());
        }
    }
    
    /**
     * Call AFTER generating the Report
     */
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

        // a silly workaround to convey the delimiter and the showheader info to
        // the CSV viewer!!!!
        ((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_DELIMITER", delimiteur);
        ((DefaultReportOutput) reportOutput).getReport().getReportTemplate().put("SANTCSV_SHOWHEADER",
                "" + bShowHeadings);

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

    public int getCheckDelim() {
        return this.checkDelim;
    }

    public void setCheckDelim(int checkDelim) {
        this.checkDelim = checkDelim;
    }


    /**
     * To generate the final control line
     *
     * @param reportOutput Output for the report.
     * @return The text control line
     */
    protected String generateControlLine(final ReportOutput reportOutput) {
        String controlLine = "*****";
        controlLine = controlLine + String.format("%08d", reportOutput.getNumberOfRows())
                + DATEFORMAT.format(getValuationDatetime().getJDate(TimeZone.getDefault()).getDate(this._timeZone));
        return controlLine;
    }

    protected String generateReportFile(final ReportOutput reportOutput, String reportString, final String fileName,
                                        final boolean ctrlLine) {
        if (ctrlLine) {
            final String controlLine = generateControlLine(reportOutput);
            reportString = reportString + controlLine;
        }
        String finalFilename=getFinalFilename(fileName);
        boolean sendMail=true;
        try (OutputStreamWriter writer = new OutputStreamWriter(getOutputStream(finalFilename), StandardCharsets.UTF_8)) {
            writer.write(reportString);
        } catch (final FileNotFoundException e) {
            Log.error(this,
                    "The filename is not valid. Please configure the scheduled task with a valid filename: " + fileName,
                    e);
            sendMail=false;
        } catch (final IOException e) {
            Log.error(this, "An error ocurred while writing the files: " + fileName, e);
            sendMail=false;
        }
        if(sendMail) {
            sendEmail(finalFilename);
        }
        return reportString;
    }

    private String getFinalFilename(String fileName){
        String finalName=fileName;
        if(Boolean.parseBoolean(this.getAttribute(COMPRESSED_FILE_ATTR))){
            finalName=fileName+"."+GZ_FORMAT;
        }
        return finalName;
    }
    private OutputStream getOutputStream(String fileName) throws IOException {
        OutputStream outputStream;
        if(Boolean.parseBoolean(this.getAttribute(COMPRESSED_FILE_ATTR))){
            outputStream=new GZIPOutputStream(new FileOutputStream(fileName));
        }else{
            outputStream=new FileOutputStream(fileName);
        }
        return outputStream;
    }


    protected String removeDelimiteurs(final String cadena, final char delimiteur) {
        // COL_OUT_016
        // Carlos Cejudo: This method has been changed to use StringBuilder
        // class instead of a series of sums of
        // Strings. This saves memory in the system and improves the performance
        // of the process.
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
        template.put(USE_PIVOT_TABLE, getAttribute(USE_PIVOT_TABLE));
    }

    /**
     * send The email in oder to send it.
     *
     * @param filePath
     *            DataServer connection.
     * @throws Exception
     */
    protected void sendEmail(final String filePath) {
            final List<String> emailAddressesTo = Util.string2Vector(getAttribute(CUSTOM_EMAIL_LIST));
            if (!Util.isEmpty(emailAddressesTo)) {
                Log.debug(this, "Start SEND EMAIL.");
                // Get the body
                String emailSubject = "Automatic Scheduled Task from Calypso : " + this.getExternalReference();
                if(!Util.isEmpty(this.getAttribute(CUSTOM_SEND_MAIL_SUBJECT))){
                    emailSubject=buildSubject();
                }
                final String body="This email has been automatically sent to you because of the following Calypso Scheduled Task:" + this.getType() + "/" + "You will find your report in attachment of the present email " + "and at the following location:\r\n\r\n" + filePath + "\r\n\r\nBest Regards\r\n";

                final ArrayList<String> attachments = new ArrayList<>();
                attachments.add(filePath);
                Log.debug(this, "Create email message");
                try {
                    CollateralUtilities.sendEmail(emailAddressesTo, emailSubject, body,DEFAULT_FROM_EMAIL, attachments);
                } catch (MailException | IOException exc) {
                   Log.error(this.getClass().getSimpleName(),"Couldn't send report by email",exc.getCause());
                }
                Log.info(this, "Email sent to " + emailAddressesTo.toString());
            }
    }

    private String buildSubject(){
        String emailSubject=this.getExternalReference()+" -- "+this.getAttribute(CUSTOM_SEND_MAIL_SUBJECT);
        SimpleDateFormat formatter=new SimpleDateFormat("dd-MM-yyyy");
        String date=formatter.format(this.getValuationDatetime());
        emailSubject=emailSubject+" -- "+date;
        return emailSubject;
    }
}
