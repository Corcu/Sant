package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.mmoo.ImportMMOOUtilities;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * To import MMOO - MEFF (Garant?as depositadas)
 *
 * @author David Porras
 * @version 1.0
 **/

public class ScheduledTaskImportMMOO_MEFF_Depo extends AbstractProcessFeedScheduledTask {

    private static final long serialVersionUID = 123L;

    /**
     * ST attributes
     */
    private static final String FILEPATH = "File Path";
    private static final String STARTFILENAME = "Start of File Name";
    private static final String SEPARATOR = "Field Separator";
    private static final String MEFF_CONTRACT_NAME = "Meff Contract Name";
    private static final String LOG_DIR = "Log Directoy";
    private static final String LOG_NAME = "Log File Name";
    /**
     * Class Constants
     */
    private static final String TASK_INFORMATION = "Import MMOO Garantias Depositadas";
    private static final String BLANK = "";

    /**
     * Eurex ECAG Default Fund Collateral Contract
     */
    private CollateralConfig meffContract = null;


    @Override
    /**
     * Main process
     */
    public boolean process(final DSConnection conn, final PSConnection connPS) {

        final String path = getAttribute(FILEPATH);
        final String startFileName = getAttribute(STARTFILENAME);
        final String separator = getAttribute(SEPARATOR);
        final JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        final JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1, getHolidays());
        List<Long> tradeIds = new ArrayList<>();
        List<String> logDetails = new ArrayList<>();

        // get data file
        File dataFile = getDataFile(path, startFileName, processDate, logDetails);
        if (dataFile == null) {
            scheduledTaskLog(tradeIds, logDetails, valueDate, dataFile);
            return false;
        }

        // process file
        Vector<Vector<RecordMapper>> recordsFile = processFile(dataFile, separator, logDetails);
        if ((recordsFile == null) || logDetails.isEmpty()) {
            scheduledTaskLog(tradeIds, logDetails, valueDate, dataFile);
            return false;
        }

        // load EUREX contracts
        if (!loadMMOOMeffCollateralConfigs(logDetails)) {
            scheduledTaskLog(tradeIds, logDetails, valueDate, dataFile);
            return false;
        }

        // process cash & security records in order to obtain and save new position
        processCash(recordsFile.get(0), tradeIds, this.meffContract, processDate, logDetails);
        processSecurities(recordsFile.get(1), tradeIds, this.meffContract, processDate, logDetails);

        scheduledTaskLog(tradeIds, logDetails, valueDate, dataFile);
        ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        return true;
    }

    /**
     * @param logErrTrades
     * @return true if MMOO Meff Collateral Configs loaded successfully
     */
    private boolean loadMMOOMeffCollateralConfigs(final List<String> logErrTrades) {

        boolean retVal = true;
        String value = super.getAttribute(MEFF_CONTRACT_NAME);
        if (!Util.isEmpty(value)) {

            try {
                this.meffContract = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {

                this.meffContract = null;
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (this.meffContract == null) {
                logErrTrades.add("Could not load MEFF Collateral Config " + value
                        + ". Please, check configuration. Importation cannot be done.");
                retVal = false;
            }
        } else {
            logErrTrades.add("Could not load MEFF Collateral Config " + value
                    + ". Please, check configuration. Importation cannot be done.");
            return false;
        }

        return retVal;
    }

    /**
     * Fills the information of the log
     *
     * @param errors
     * @param dataFile
     * @param processDate
     */
    private void scheduledTaskLog(List<Long> tradeIds, List<String> errors, final JDate value,
                                  final File dataFile) {

        if (!Util.isEmpty(errors)) {
            Log.error(this, errors.toString());
        }

        String logPath = getAttribute(LOG_DIR);
        final String logName = getAttribute(LOG_NAME);
        final String startFileName = getAttribute(STARTFILENAME);
        String fileName = startFileName;
        if (dataFile != null) {
            fileName = dataFile.getName();
        }

        final String extRef = super.getExternalReference();
        if (!logPath.endsWith("/")) {
            logPath = logPath + "/";
        }
        logPath += logName.trim();
        ImportMMOOUtilities.scheduledTaskLog_Depo(tradeIds, errors, logPath, fileName, extRef, value);

    }

    /**
     * Task information
     */
    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    /**
     * ST attributes definition
     */
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(SEPARATOR));
        attributeList.add(attribute(MEFF_CONTRACT_NAME));
        attributeList.add(attribute(LOG_DIR));
        attributeList.add(attribute(LOG_NAME));

        return attributeList;
    }

//	/**
//	 * ST attributes definition
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(SEPARATOR);
//		attr.addElement(MEFF_CONTRACT_NAME);
//		attr.addElement(LOG_DIR);
//		attr.addElement(LOG_NAME);
//
//		return attr;
//	}
//
//	/**
//	 * @param attribute
//	 *            name
//	 * @param hastable
//	 *            with the attributes declared
//	 * @return a vector with the values for the attribute name
//	 */
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
//
//		Vector<String> vector = new Vector<String>();
//
//		vector = super.getAttributeDomain(attribute, hashtable);
//
//		return vector;
//
//	}

    /**
     * Ensures that the attributes have a value introduced by who has setup the schedule task
     *
     * @return if the attributes are ok
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector messages) {

        boolean retVal = super.isValidInput(messages);

        String value = super.getAttribute(LOG_DIR);
        if (Util.isEmpty(value)) {

            messages.addElement(LOG_DIR + " MANDATORY attribute not specified.");
            retVal = false;
        }

        value = super.getAttribute(LOG_NAME);
        if (Util.isEmpty(value)) {

            messages.addElement(LOG_NAME + " MANDATORY attribute not specified.");
            retVal = false;
        }

        // check Meff contract associated
        value = super.getAttribute(MEFF_CONTRACT_NAME);
        if (!Util.isEmpty(value)) {

            try {
                this.meffContract = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", value.trim());
            } catch (RemoteException e) {
                this.meffContract = null;
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
            }

            if (this.meffContract == null) {
                messages.addElement("Meff Contract Name does not exist in the System. Please check attribute "
                        + MEFF_CONTRACT_NAME);
                retVal = false;
            }
        }

        return retVal;

    }

    /**
     * This method overrides optional configurations like: default LE, External reference, Trades subtypes for cuenta
     * propia and cuenta terceros
     */
    // private void fillOptionalVariables() {
    //
    // // add LE associated
    // this.MEFF_LE = this.meffContract.getLegalEntity().getCode(); // shortname
    //
    // }

    // ***** FILES STUFF *** //

    /**
     * Get data file from path and specific date
     *
     * @param path
     * @param startFileName
     * @param valueDate
     * @return
     */
    public File getDataFile(String path, String startFileName, JDate valueDate, List<String> errors) {

        File dataFile = null;

        // get file
        dataFile = CollateralUtilities.getFile(path, startFileName, valueDate);
        if (dataFile == null) {
            errors.add("Error getting file with start name " + startFileName);
        }

        return dataFile;

    }

    // ***** PROCESS LOGIC STUFF ***** //

    /**
     * Process file getting valid records to take in account (parsed in RecordMapper object)
     *
     * @param file
     * @param separator
     * @param logDetails
     * @return
     */
    public Vector<Vector<RecordMapper>> processFile(File file, String separator, List<String> logDetails) {

        String line = null;
        Vector<Vector<RecordMapper>> records = new Vector<>();
        Vector<RecordMapper> cashRecords = new Vector<>();
        Vector<RecordMapper> securityRecords = new Vector<>();
        BufferedReader inputFileStream = null;
        int i = 0;

        try {
            inputFileStream = new BufferedReader(new FileReader(file));
            inputFileStream.readLine(); // salto cabecera
            for (i = 0; ((line = inputFileStream.readLine()) != null); i++) {
                // get record
                //StringBuilder to MIG V16
                StringBuilder slashAndSeparator = new StringBuilder("\\");
                slashAndSeparator.append(separator);
                String[] values = line.split(slashAndSeparator.toString());
                if (!isValidData(values, i + 2, logDetails)) {
                    continue;
                }
                RecordMapper rm = new RecordMapper(values, separator, i + 2);
                if (isCashRecord(rm, file)) {
                    cashRecords.add(rm);
                } else if (isSecurityRecord(rm, logDetails, file)) {
                    securityRecords.add(rm);
                }
            }
        } catch (FileNotFoundException e) {
            Log.error(this, e); //sonar
            logDetails.add("Error getting file: " + e.getMessage() + "\n");
            return null;
        } catch (IOException e) {
            Log.error(this, e); //sonar
            logDetails.add("Error reading file: " + e.getMessage() + "\n");
            return null;
        } finally {
            if (inputFileStream != null) {
                try {
                    inputFileStream.close();
                } catch (final IOException e) {
                    Log.error(this, e); //sonar
                    logDetails.add("Error closing file: " + e.getMessage() + "\n");
                }
            }
        }

        records.add(cashRecords);
        records.add(securityRecords);

        return records;

    }

    /**
     * Check if record is CASH
     *
     * @param rm
     * @param fileType
     * @return
     */
    public boolean isCashRecord(RecordMapper rm, File file) {

        String clearingMember = rm.getClearingMember();
        String member = rm.getMember();
        String holder = rm.getHolder();
        String cashSec = rm.getCashSec();

        return (clearingMember.equals("A911")) && ((member.equals(BLANK)) || (member.equals("A911")))
                && ((holder.equals(BLANK)) || (holder.equals("00P")) || (holder.equals("PG3")))
                && (cashSec.equals("C"));

    }

    /**
     * Check if record is SECURITY
     *
     * @param rm
     * @param fileType
     * @return
     */
    public boolean isSecurityRecord(RecordMapper rm, List<String> errors, File file) {

        boolean isValid = false;
        String clearingMember = rm.getClearingMember();
        String member = rm.getMember();
        String holder = rm.getHolder();
        String cashSec = rm.getCashSec();
        String productIsin = rm.getProductIsin();

        if ((clearingMember.equals("A911")) && ((member.equals(BLANK)) || (member.equals("A911")))
                && ((holder.equals(BLANK)) || (holder.equals("00P")) || (holder.equals("PG3")))
                && (cashSec.equals("P"))) {

            // check bond
            Product product = BOCache.getExchangeTradedProductByKey(getDSConnection(), "ISIN", productIsin);
            if (product == null) {
                errors.add("Line " + rm.getRecordLine() + ": ISIN = " + productIsin + " does not exist.\n");
            } else {
                isValid = true;
            }

        }

        return isValid;

    }

    /**
     * Make cash position adjustment through a marginCallTrade
     *
     * @param cashRecords
     * @param tradeIds
     * @param contract
     * @param processDate
     * @param logDetails
     */
    public void processCash(Vector<RecordMapper> cashRecords, List<Long> tradeIds, CollateralConfig contract,
                            JDate processDate, List<String> logDetails) {

        Double newCashPosition, delta;
        newCashPosition = delta = 0.00;

        // get actual position
        Double actualCashPosition = ImportMMOOUtilities.fecthTodayCashMarginCallPosition(processDate, contract,
                logDetails);

        // get delta to adjust position
        if (!Util.isEmpty(cashRecords)) {
            newCashPosition = -getAmountFromRecords(cashRecords);
            delta = newCashPosition - actualCashPosition;
        } else {
            // if there's no cash record set position to zero
            delta = -actualCashPosition;
        }

        // get trade & save it
        Trade mcCashTrade = ImportMMOOUtilities.createCashMarginCallTrade(delta, contract, processDate);
        if (mcCashTrade != null) {
            tradeIds.add(ImportMMOOUtilities.saveTrade(mcCashTrade, -1, logDetails));
        }

    }

    /**
     * @param secRecords
     * @param tradeIds
     * @param contract
     * @param valueDate
     * @param logDetails
     */
    public void processSecurities(Vector<RecordMapper> secRecords, List<Long> tradeIds,
                                  CollateralConfig contract, JDate valueDate, List<String> logDetails) {

        // process each security
        for (RecordMapper rm : secRecords) {

            // get product
            Product product = BOCache.getExchangeTradedProductByKey(DSConnection.getDefault(), "ISIN",
                    rm.getProductIsin());

            // get actual position
            Double actualSecPosition = ImportMMOOUtilities.fecthTodayBondMarginCallPosition(valueDate, contract,
                    product, logDetails);
            // get new position
            Double newSecPosition = -rm.getAmount();

            // get trade & save it
            Trade mcSecTrade = ImportMMOOUtilities.createSecMarginCallTrade(product,
                    newSecPosition - actualSecPosition, contract, valueDate);
            if (mcSecTrade != null) {
                tradeIds.add(ImportMMOOUtilities.saveTrade(mcSecTrade, rm.getRecordLine(), logDetails));
            }
        }

    }

    /**
     * Get sum of records amount
     *
     * @param validRecords
     * @return
     */
    public Double getAmountFromRecords(Vector<RecordMapper> records) {

        Double totalAmount = 0.0;
        for (RecordMapper rm : records) {
            totalAmount += rm.getAmount();
        }

        return totalAmount;

    }

    /**
     * Check if data contained in the record is valid to be processed
     *
     * @param values
     * @param line
     * @param logDetails
     * @return
     */
    public boolean isValidData(String[] values, int line, List<String> logDetails) {

        boolean isValid = true;

        if (values.length < 14) {
            logDetails.add("Line " + (line) + ": Missing required fields.\n");
            isValid = false;
        } else {
            if (getDoubleValue(values[13]) == null) {
                logDetails.add("Line " + (line) + ": Invalid amount.\n");
                isValid = false;
            }
        }

        return isValid;

    }

    /**
     * Get double from string value
     *
     * @param str
     * @return
     */
    private Double getDoubleValue(String str) {
        final NumberFormat numberFormatter = new DecimalFormat("#0.00");
        Double value = null;
        try {
            value = numberFormatter.parse(str).doubleValue();
        } catch (ParseException e) {
            Log.error(this, e); //sonar
        }
        return value;
    }

    // ************************************* RECORD MAPPER CLASS ************************************** //
    // ***** Class used to parse file records taking in account only fields we are interested in ***** //
    // ********************************************************************************************** //
    private class RecordMapper {

        private final String clearingMember;
        private final String member;
        private final String holder;
        private final String cashSec;
        private final String productIsin;
        private final Double amount;
        private final int recordLine;

        public RecordMapper(String[] values, String separator, int line) {

            this.recordLine = line;

            // only take values we are interest on
            this.clearingMember = formatString(values[2]);
            this.member = formatString(values[3]);
            this.holder = formatString(values[4]);
            this.cashSec = formatString(values[6]);
            this.productIsin = formatString(values[5]);
            this.amount = getDoubleValue(values[13]);

        }

        public String getClearingMember() {
            return this.clearingMember;
        }

        public String getMember() {
            return this.member;
        }

        public String getHolder() {
            return this.holder;
        }

        public String getCashSec() {
            return this.cashSec;
        }

        public String getProductIsin() {
            return this.productIsin;
        }

        public Double getAmount() {
            return this.amount;
        }

        public int getRecordLine() {
            return this.recordLine;
        }

        private String formatString(String s) {
            return s.replaceAll("\"", BLANK);
        }

    }

    @Override
    public String getFileName() {
        return null;
    }

}
