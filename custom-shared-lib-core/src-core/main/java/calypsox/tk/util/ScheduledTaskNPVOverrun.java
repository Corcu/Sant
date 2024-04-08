/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.util.bean.SantTradeBean;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import calypsox.util.NPVTradeUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ST that simulates de overrun procedure: due a file with external ref trades,
 * saves the new Mtm, calculates affected contracts & builds and email attaching
 * the SantTradeBrowser report.
 *
 * @author Guillermo Solano
 * @version 1.0
 * @date 03/06/2015
 */
public class ScheduledTaskNPVOverrun extends ScheduledTask {

    /**
     * unique class id, important to avoid problems
     */
    private static final long serialVersionUID = 3733375722377754L;

    /**
     * CONSTANTS DEFINITION COPY FULL FILE SCHEDULED TASK This section includes
     * the constants to define the different attributes in the schedule task
     * domain, as the possible values.
     */
    /**
     * Enum containing the domain attributes constants.
     */
    private enum DOMAIN_ATTRIBUTES {

        FILENAME("File Name Start", attribute("File Name Start")), // 1
        IMPORT_DIR("Directory Path", attribute("Directory Path")), // 2
        SEPARATOR("File Separator", attribute("File Separator")), // 3
        POST_PROCESS_FILE("Activate Post-Process", attribute("Activate Post-Process").booleanType()), // 4
        RECALCULATE_MC("Contracts Re-calculation", attribute("Contracts Re-calculation").booleanType()), // 5
        SEND_EMAIL("Activate Send Email", attribute("Activate Send Email").booleanType()), // 6
        EMAIL_ADDRESS("Email Addresses", attribute("Email Addresses")), // 7
        NOTIFICATION_TEMPLATE("Email notification template",
                attribute("Email notification template").domain(getReportTemplateNames())), // 8
        LOG_FILENAME("Log File Name", attribute("Log File Name")), // 9
        LOG_DIR("Log Directory Path", attribute("Log Directory Path")), // 10
        USE_CONTRACT_CCY("Use contract currency", attribute("Use contract currency").booleanType()),
        FILE_SEARCH_CRITERIA("Input file criteria search",
                attribute("Input file criteria search").domain(Arrays.asList(CRIT_LIST))); // 11

        private final String desc;
        private final AttributeDefinition def;

        // add description
        private DOMAIN_ATTRIBUTES(String d, AttributeDefinition def) {
            this.desc = d;
            this.def = def;
        }

        // return the description
        public String getDesc() {
            return this.desc;
        }

        // return the definition
        public AttributeDefinition getDef() {
            return this.def;
        }

        // list with domain values definitions
        public static List<AttributeDefinition> getDomainDef() {
            ArrayList<AttributeDefinition> a = new ArrayList<AttributeDefinition>(DOMAIN_ATTRIBUTES.values().length);
            for (DOMAIN_ATTRIBUTES domain : DOMAIN_ATTRIBUTES.values()) {
                a.add(domain.getDef());
            }
            return a;

        }
    } // end ENUM DOMAIN_ATTRIBUTES

    /**
     * @return a vector with all the domain attributes for this schedule task
     */

    private static List<String> getReportTemplateNames() {
        List<String> templateNames = new ArrayList<String>();
        Vector<ReportTemplateName> templates = BOCache.getReportTemplateNames(DSConnection.getDefault(),
                NPVTradeUtilities.TRADE_REPORT_NAME, null);
        for (ReportTemplateName template : templates)
            templateNames.add(template.getTemplateName());
        return templateNames;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        return DOMAIN_ATTRIBUTES.getDomainDef();
    }

    // @SuppressWarnings("rawtypes")
    // @Override
    // public Vector getDomainAttributes() {
    //
    // final Vector<String> result = new
    // Vector<String>(DOMAIN_ATTRIBUTES.values().length);
    // result.addAll(DOMAIN_ATTRIBUTES.getDomainDescr());
    // return result;
    // }
    //
    // /**
    // * @param attribute
    // * name
    // * @param hastable
    // * with the attributes declared
    // * @return a vector with the values for the attribute name
    // */
    // @SuppressWarnings({ "rawtypes", "unchecked" })
    // @Override
    // public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
    //
    // Vector<String> vector = new Vector<String>();
    //
    // if (attribute.equals(DOMAIN_ATTRIBUTES.SEND_EMAIL.getDesc())
    // || attribute.equals(DOMAIN_ATTRIBUTES.RECALCULATE_MC.getDesc())
    // || attribute.equals(DOMAIN_ATTRIBUTES.POST_PROCESS_FILE.getDesc())) {
    //
    // vector.addAll(Arrays.asList(BOOLEANS));
    //
    // } else if
    // (attribute.equals(DOMAIN_ATTRIBUTES.FILE_SEARCH_CRITERIA.getDesc())) {
    //
    // vector.add(CRIT_NAME);
    // vector.add(CRIT_DATE);
    // vector.add(CRIT_MTMDATE_FILENAME);
    // vector.add(CRIT_PROCESSDATE_FILENAME);
    //
    // } else if
    // (attribute.equals(DOMAIN_ATTRIBUTES.NOTIFICATION_TEMPLATE.getDesc())) {
    //
    // Vector<ReportTemplateName> templates =
    // BOCache.getReportTemplateNames(DSConnection.getDefault(),
    // NPVTradeUtilities.TRADE_REPORT_NAME, null);
    //
    // for (int i = 0; i < templates.size(); i++) {
    // ReportTemplateName reporttemplatename = templates.elementAt(i);
    // vector.add(reporttemplatename.getTemplateName());
    // }
    //
    // } else {
    //
    // vector = super.getAttributeDomain(attribute, hashtable);
    // }
    //
    // return vector;
    // }

    /**
     * Valid email pattern
     */
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Search file criterias
     */
    private final static String CRIT_NAME = "Name Match";
    private final static String CRIT_DATE = "Most Recently";
    private final static String CRIT_MTMDATE_FILENAME = "MtM date filename";
    private final static String CRIT_PROCESSDATE_FILENAME = "Process date filename";
    private final static String[] CRIT_LIST = {CRIT_NAME, CRIT_DATE, CRIT_MTMDATE_FILENAME,
            CRIT_PROCESSDATE_FILENAME};

    /**
     * Name of this Schedule task
     */
    private final static String TASK_INFORMATION = "Overrun NPV from external system, re-calculate contracts & send notification";
    private static final String EMAILS_SEPARATOR_PATTERN = ";";

    // CLASS VARIABLES
    /**
     * NPV bean to be imported
     */
    private final Map<String, SantTradeBean> npvTradesBeanMap = new HashMap<String, SantTradeBean>();
    /**
     * Log historical data
     */
    private final Collection<String> currentLog = new ArrayList<String>();

    /**
     * Log data
     */
    private final StringBuffer generalLog = new StringBuffer();

    /**
     * MTM Reading file input
     */
    private File fileInput = null;

    // //////////////////////////////////////////////
    // //////// OVERRIDE PUBLIC METHODS ////////////
    // ////////////////////////////////////////////

    /**
     * Main method to be executed in this Scheduled task
     *
     * @param connection to DS
     * @param connection to PS
     * @return result of the process
     */
    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {

        boolean status = true;

        // read the NPV file
        status = readNPVFile() && fileHasTradesToProcess();

        if (status) {
            // import the new MtM
            importNewNPV();

            // re-calculate the affected contracts
            calculateContracts();

            // send result email
            sendEmail();
        }

        // build log
        saveLogFile();

        if (status) {

            // move to OK folder if everything went right
            filePostProcess();
            // everything went OK.
            return true;
        }
        // there was an error
        return false;
    }

    /**
     * @return this task information, gathered from the constant
     * TASK_INFORMATION
     */
    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    /**
     * Ensures that the attributes have a value introduced by who has setup the
     * schedule task
     *
     * @return if the attributes are ok
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messages) {

        boolean retVal = super.isValidInput(messages);

        for (DOMAIN_ATTRIBUTES attribute : DOMAIN_ATTRIBUTES.values()) {

            if (attribute.equals(DOMAIN_ATTRIBUTES.EMAIL_ADDRESS)
                    || attribute.equals(DOMAIN_ATTRIBUTES.FILE_SEARCH_CRITERIA)
                    || attribute.equals(DOMAIN_ATTRIBUTES.POST_PROCESS_FILE)) {
                continue;
            }
            final String value = super.getAttribute(attribute.getDesc());
            if (Util.isEmpty(value)) {
                messages.addElement(attribute.getDesc() + " attribute is not specified.");
                retVal = false;
            }
        }

        // File dir = new
        // File(super.getAttribute(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc()));
        // if ((dir == null) || !dir.isDirectory()) {
        // messages.addElement(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc() + "
        // directory doesn't exist");
        // retVal = false;
        // }
        //
        // dir = new
        // File(super.getAttribute(DOMAIN_ATTRIBUTES.LOG_DIR.getDesc()));
        // if ((dir == null) || !dir.isDirectory()) {
        // messages.addElement(DOMAIN_ATTRIBUTES.LOG_DIR.getDesc() + " directory
        // doesn't exist");
        // retVal = false;
        // }

        return retVal;
    }

    // /////////////////////////////////////////////
    // //////// PRIVATE METHODS ///////////////////
    // ///////////////////////////////////////////

    /**
     * @return true if the MtM in file has been processed OK and at least 1
     * trade must be modified
     */
    private boolean fileHasTradesToProcess() {

        return !this.npvTradesBeanMap.isEmpty();
    }

    /**
     * Reads the NPV file specified in the attributes of the ST and saves each
     * trade to update de MtM/NPv.
     *
     * @return if was successfully read
     */
    private boolean readNPVFile() {

        Log.info(this, "Reading File");
        File dir = new File(super.getAttribute(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc()));
        if (dir.isDirectory()) {

            // recover in file based on criteria
            final File[] listFiles = recoverFile(dir);

            if (listFiles.length == 1) {

                Integer lineNumber = 1;
                final String separator = super.getAttribute(DOMAIN_ATTRIBUTES.SEPARATOR.getDesc());

                if (!Util.isEmpty(separator)) {
                    NPVTradeUtilities.setSEPARATOR(separator.trim());
                }
                try {
                    this.currentLog.add("\nStarting reading the file");
                    this.fileInput = listFiles[0];

                    final BufferedReader br = new BufferedReader(new FileReader(this.fileInput));
                    String line = br.readLine();
                    // check header (doesn't contain any number)
                    if (line.matches("[a-zA-Z" + separator.trim() + "]+")) {
                        line = br.readLine();
                    }
                    // process all NPV lines
                    while (line != null) {

                        final SantTradeBean bean = NPVTradeUtilities.buildNPVTradeBean(lineNumber++, line,
                                this.currentLog);

                        if ((bean != null) && !bean.isEmpty()) {
                            this.npvTradesBeanMap.put(bean.getExternalReference(), bean);
                        }

                        line = br.readLine();
                    }
                    br.close();
                    this.currentLog.add("\nTrades have been successfully read from the file");
                    // start log data
                    attachLogHeader();
                    attachLogStatus();
                    return true;

                } catch (FileNotFoundException e) {
                    Log.error(this, e); //sonar
                    this.currentLog.add("File " + getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc())
                            + "not found. Check Attribute ");
                } catch (IOException e) {
                    Log.error(this, e); //sonar
                    this.currentLog.add("IO Error processing " + getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc())
                            + "not found. Repeat Batch Process ");
                }
            } else {
                if (listFiles.length == 0) {
                    this.currentLog.add("File " + getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc())
                            + "not found. Check Attribute ");
                } else if (listFiles.length > 1) {
                    this.currentLog.add("File " + getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc())
                            + "found more than once. Check server directory ");
                }
            }
        }
        return false;
    }

    /**
     * Makes a map of trades recovered by its external reeference, saves PlMarks
     * changes and
     */
    private void importNewNPV() {

        Log.info(this, "building trade map");
        // inits a map with the trades, stores some data in the beans
        Map<String, List<Trade>> tradeMap = NPVTradeUtilities.initTradeMap(this.npvTradesBeanMap, this.currentLog);

        Log.info(this, "save PlMarks");
        // checks trade is valid, then PlMarks
        boolean useContractCCY = Util.isTrue(super.getAttribute(DOMAIN_ATTRIBUTES.USE_CONTRACT_CCY.getDesc()), false);

        HashSet<Trade> tradesToSave = NPVTradeUtilities.processPlMarks(tradeMap, this.npvTradesBeanMap,
                useContractCCY, this.currentLog);

        Log.info(this, "save amend trades");
        // We also need to save trades modified and unsettled Trades
        NPVTradeUtilities.saveModifiedTrades(tradesToSave, this.currentLog);

        // save MtM process to log
        attachLogStatus();
    }

    /**
     * Is attribute is true, it will make a copy in the \copy directory
     *
     * @param fileToCopy
     * @param dir
     */
    private void filePostProcess() {

        String postProcess = super.getAttribute(DOMAIN_ATTRIBUTES.POST_PROCESS_FILE.getDesc());

        if ((postProcess != null) && Boolean.valueOf(postProcess)) {
            return; // nothing to do
        }

        File importDir = null;
        try {
            importDir = new File(super.getAttribute(DOMAIN_ATTRIBUTES.IMPORT_DIR.getDesc()));
        } catch (Exception e) {
            Log.error(this, "Cannot instantiate import file. Post Process cancelled", e);
            return;
        }
        // everything OK
        final File copyDir = FileUtility.findDirectoryIn(importDir, "copy");

        if ((copyDir != null) && copyDir.isDirectory()) {
            // make the copy to copy dir
            FileUtility.copyFileToDirectory(this.fileInput, copyDir);

            // delete old file
            if (this.fileInput.delete()) {
                Log.info(this, "NPV file has been processed successfully.");
            } else {
                Log.info(this, "Failed to delete file " + this.fileInput);
            }
        }
    }

    // final File okDir = FileUtility.findDirectoryIn(importDir, "ok");
    // if ((okDir != null) && okDir.isDirectory()) {
    // // make the copy to OK
    // FileUtility.copyFileToDirectory(this.fileInput, okDir);
    // }

    /**
     * @param dir
     * @return the NPV IN file based on the criteria by name, adding the
     * timestamp to the name or the last one modified matching a name.
     */
    private File[] recoverFile(final File dir) {

        // filter to gather the files in each directory that starts with a
        // concrete name
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File directory, String fileName) {
                final String startName = getAttribute(DOMAIN_ATTRIBUTES.FILENAME.getDesc());
                return fileName.startsWith(startName.trim());
            }
        };

        final String criteria = super.getAttribute(DOMAIN_ATTRIBUTES.FILE_SEARCH_CRITERIA.getDesc());
        if (criteria.equals(CRIT_MTMDATE_FILENAME)) {

            filter = new FilenameFilter() {

                @Override
                public boolean accept(File directory, String fileName) {
                    final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");
                    final JDate prevBisDay = getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1,
                            Util.string2Vector("SYSTEM"));
                    final String date = timeFormat.format(prevBisDay.getJDatetime(getTimeZone()));
                    // searchs in FileName YYYYmmDD with DD - 1
                    return fileName.contains(date);
                }
            };

        } else if (criteria.equals(CRIT_PROCESSDATE_FILENAME)) {

            filter = new FilenameFilter() {

                @Override
                public boolean accept(File directory, String fileName) {
                    final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");
                    final JDate process = getValuationDatetime().getJDate(TimeZone.getDefault());
                    final String date = timeFormat.format(process.getJDatetime(getTimeZone()));
                    // searchs in FileName YYYYmmDD
                    return fileName.contains(date);
                }
            };

        } else if (criteria.equals(CRIT_DATE)) {

            return new File[]{NPVTradeUtilities.getLastModifiedFile(dir, filter)};
        }

        return dir.listFiles(filter);
    }

    /**
     * Saves the full log into a file
     */
    private void saveLogFile() {

        final String logPath = getAttribute(DOMAIN_ATTRIBUTES.LOG_DIR.getDesc());
        final String logName = getAttribute(DOMAIN_ATTRIBUTES.LOG_FILENAME.getDesc());
        if (Util.isEmpty(logName) || Util.isEmpty(logPath)) {
            Log.error(this, "Empty parameters LOG FOLDER or LOG NAME");
            return;
        }
        String filePath = logPath;
        if (!logPath.endsWith("/")) {
            filePath = logPath + "/";
        }
        // append date to the log file
        filePath += logName.trim() + getDateFormatted() + ".txt";

        try {
            FileWriter fileW = new FileWriter(filePath);
            PrintWriter pw = new PrintWriter(fileW);
            pw.write(this.generalLog.toString());
            pw.close();
            fileW.close();

        } catch (IOException e) {
            Log.error(this, e);
        }
    }

    /**
     * @returns a String with the format
     */
    private String getDateFormatted() {

        final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd");
        return timeFormat.format(new Date());
    }

    /**
     * builds the report with the results and send an email to the emails
     * proposed
     */
    private void sendEmail() {

        final String sendEmail = super.getAttribute(DOMAIN_ATTRIBUTES.SEND_EMAIL.getDesc());

        if ((sendEmail != null) && sendEmail.equals("No")) {
            this.currentLog
                    .add("\nEmail notification desactivated in attribute:" + DOMAIN_ATTRIBUTES.SEND_EMAIL.getDesc());
            return;
        }

        this.currentLog.add("\n\nStarting email notification.");
        // read emails & validate format
        List<String> emailsToSend = validateAndBuildEmails();

        if (emailsToSend.isEmpty()) {
            // nothing to do
            attachLogStatus();
            return;
        }

        // result of the automatic overrung
        final OverrunResult result = tradesProcessedData();

        Log.info(this, "building report");
        final String template = super.getAttribute(DOMAIN_ATTRIBUTES.NOTIFICATION_TEMPLATE.getDesc());
        // generate the report with this data (one report per each chunk of 999
        // trades processed)
        List<byte[]> report = NPVTradeUtilities.buildBinaryReport(result, template, super.getValuationDatetime(),
                this.currentLog);

        Log.info(this, "send email");
        // attach result and send emails
        NPVTradeUtilities.sendEmail(emailsToSend, report, this.currentLog, result);
        this.currentLog.add("\nEmail Service Succesfully Finished.");

        // attach to log
        attachLogStatus();
    }

    /**
     * @return a list of valid emails addresses
     */
    private List<String> validateAndBuildEmails() {

        String emails = super.getAttribute(DOMAIN_ATTRIBUTES.EMAIL_ADDRESS.getDesc());

        if (Util.isEmpty(emails)) {
            this.currentLog.add("\nEmail notification activated in attribute but no emails specified in"
                    + DOMAIN_ATTRIBUTES.EMAIL_ADDRESS.getDesc());
        }

        // ub case the user added several email accounts separated by comma
        // instead of ;
        emails = emails.replaceAll(",", EMAILS_SEPARATOR_PATTERN);

        String[] emailsStr = emails.split(EMAILS_SEPARATOR_PATTERN);
        List<String> emailsToSend = new ArrayList<String>(emailsStr.length);
        for (String e : emailsStr) {
            // validates correct forma each proposed email
            if (validate(e.trim())) {
                emailsToSend.add(e.trim());
            } else {
                this.currentLog.add("\nEmail address " + e.trim() + " not valid. Check configuration.");
            }
        }
        if (emailsToSend.isEmpty()) {
            this.currentLog.add("\nEmail notification activated not valid emails addresses in attribute "
                    + DOMAIN_ATTRIBUTES.EMAIL_ADDRESS.getDesc());
        }

        return emailsToSend;
    }

    /**
     * Internal class, info of the process
     */
    public class OverrunResult {

        private List<Long> tradesOk;
        private List<String> tradesKO;
        private Map<String, List<Long>> moreThan1Trade;
        private Map<String, SantTradeBean> beansMap;

        /**
         * @return the tradesOk
         */
        public List<Long> getTradesOk() {
            return this.tradesOk;
        }

        /**
         * @return the tradesKO
         */
        public List<String> getTradesKO() {
            return this.tradesKO;
        }

        /**
         * @return the moreThan1Trade
         */
        public Map<String, List<Long>> getMoreThan1Trade() {
            return this.moreThan1Trade;
        }

        /**
         * @return the beansMap
         */
        public Map<String, SantTradeBean> getBeansMap() {
            return this.beansMap;
        }

    } // end class

    /**
     * Process the result object
     */
    private OverrunResult tradesProcessedData() {

        OverrunResult result = new OverrunResult();
        List<Long> trades = new ArrayList<>(this.npvTradesBeanMap.size());
        List<String> tradesKO = new ArrayList<>();
        Map<String, List<Long>> extRefDuplicates = new HashMap<>();

        for (Map.Entry<String, SantTradeBean> entry : this.npvTradesBeanMap.entrySet()) {

            final SantTradeBean bean = entry.getValue();

            if (bean.moreTrades4ExtRef()) {
                extRefDuplicates.put(bean.getExternalReference(), bean.getReferencedList());

            } else if (bean.hasError()) {
                tradesKO.add(bean.getExternalReference());

            } else {
                trades.add(bean.getTrade().getLongId());
            }
        }
        result.moreThan1Trade = extRefDuplicates;
        result.tradesKO = tradesKO;
        result.tradesOk = trades;
        result.beansMap = this.npvTradesBeanMap;
        return result;
    }

    /**
     * Calculates the MC contracts affected by MtM changes
     */

    private void calculateContracts() {

        Log.info(this, "Calculate Contracts");
        String calculateContracts = super.getAttribute(DOMAIN_ATTRIBUTES.RECALCULATE_MC.getDesc());
        if ((calculateContracts != null) && !Boolean.valueOf(calculateContracts)) {
            this.currentLog.add(
                    "\nContracts calculation desactivated in attribute" + DOMAIN_ATTRIBUTES.RECALCULATE_MC.getDesc());
            return;
        }

        final ArrayList<String> errorMessages = new ArrayList<String>();
        final HashSet<Integer> contractSet = new HashSet<Integer>();
        this.currentLog.add("\nStarting Contracts Calculation");

        for (Map.Entry<String, SantTradeBean> entry : this.npvTradesBeanMap.entrySet()) {

            final SantTradeBean bean = entry.getValue();
            if ((bean != null) && (bean.getConfigId() != null)) {
                contractSet.add(entry.getValue().getConfigId());
            }
        }
        // Calculate Contracts.
        if (contractSet.size() > 0) {
            // Now calculate valDate and processDate for contract
            JDate contractValDate = super.getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1,
                    Util.string2Vector("SYSTEM"));
            final int calculationOffSet = ServiceRegistry.getDefaultContext().getValueDateDays() * 1;
            final JDate contractProcessDate = Holiday.getCurrent().addBusinessDays(contractValDate,
                    DSConnection.getDefault().getUserDefaults().getHolidays(), calculationOffSet);
            CollateralUtilities.calculateContracts(contractSet, new JDatetime(contractProcessDate, TimeZone.getDefault()), errorMessages);

            // Display Error messages after messages have been calculated
            if (!Util.isEmpty(errorMessages)) {
                this.currentLog.add("Error Calculating contracts" + contractSet);
                StringBuffer sb = new StringBuffer("Error Messages");
                for (String e : errorMessages) {
                    sb.append(e);
                }
                this.currentLog.add("\n" + sb.toString());
                return;
            } else {
                this.currentLog.add("Contracts: " + contractSet);
                this.currentLog.add(
                        "\nContracts have been successfully re-calculated for process date " + contractProcessDate);
            }
        } else {
            this.currentLog.add("\n No contracts re-calculated.");
        }

        // attach to log
        attachLogStatus();
    }

    /**
     * Generates the Header log
     */
    private void attachLogHeader() {

        Date date = new Date();
        // Date & DS server
        this.generalLog.append(date.toString() + ". DS Host: " + DSConnection.getDefault().getServiceURL());
        // Process info
        this.generalLog.append("\n Process: " + super.getDescription());
        // File processed
        this.generalLog.append("\n Input File: " + this.fileInput.getAbsoluteFile());
        this.generalLog.append("\n");
    }

    /**
     * Attach the current log to the general log
     */
    private void attachLogStatus() {

        // copy to the general log
        for (String s : this.currentLog) {
            this.generalLog.append(s);
        }
        this.generalLog.append("\n");
        // clear this phase errors logs
        this.currentLog.clear();
    }

    /**
     * @param emailStr
     * @return true is param is a valid email address
     */
    private static boolean validate(final String emailStr) {

        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

}
