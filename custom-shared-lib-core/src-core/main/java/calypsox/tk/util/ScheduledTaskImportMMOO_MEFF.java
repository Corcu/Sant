package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.mmoo.ImportMMOOUtilities;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * To import MMOO - MEFF
 *
 * @author David Porras
 * @version 1.2
 **/
// some modifications done by Guillermo Solano

public class ScheduledTaskImportMMOO_MEFF extends AbstractProcessFeedScheduledTask {

    private static final long serialVersionUID = 123L;

    /**
     * ST attributes
     */
    private static final String FILEPATH = "File Path";
    private static final String STARTFILENAME = "Start of File Name";
    private static final String STARTFILENAME2 = "Start of File Name 2";
    private static final String SEPARATOR = "Field Separator";
    private static final String CHOOSE_TRADE_EXT_REF_TYPE_GENERATION = "Trade ID type";
    private static final String MAN_DEFAULT_EXT_REF = "Ext. Ref. MEFF Trade";
    private static final String MEFF_CONTRACT_NAME = "Meff Contract Name";
    // private static final String MEFF_LEGAL_ENT = "MEFF LE";
    // private static final String MEFF_BOOK = "MEFF Book";
    private static final String MEFF_CUENTA_PROPIA_TYPE = "Cuenta Propia sub-type";
    private static final String MEFF_CUENTA_TERCEROS_TYPE = "Cuenta Terceros sub-type";
    private static final String LOG_DIR = "Log Directoy";
    private static final String LOG_NAME = "Log File Name";
    /**
     * Class Constants
     */
    private final static String TRADE_ID_GENERATED = "Auto-generated";
    private final static String TRADE_EXT_REF_MANUAL = "Specific Trade ID Name";
    private static final String TASK_INFORMATION = "Import MMOO trades.";

    /**
     * Class static predefined data
     */
    // default external reference names
    private static final String CAMARA = "MEFF";
    private static String TRADE_ID_MEFF = "XXX_MMOO_" + CAMARA;
    // default cuenta propia exposure subtype
    private static String TRADE_TYPE_CUENTA_PROPIA = "MMOO.CUENTAPROPIA";
    // default cuenta terceros exposure subtype
    private static String TRADE_TYPE_CUENTA_TERCEROS = "MMOO.CUENTATERCEROS";
    // default LE
    // private static String MEFF_LE = "5MSR";

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
        final String startFileName1 = getAttribute(STARTFILENAME);
        final String startFileName2 = getAttribute(STARTFILENAME2);
        final String separator = getAttribute(SEPARATOR);
        final JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        final JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault()).addBusinessDays(-1, getHolidays());
        ArrayList<String> errors = new ArrayList<String>();
        long tradeId1, tradeId2;
        tradeId1 = tradeId2 = 0;

        // get data files
        List<File> dataFiles = getDataFiles(path, startFileName1, startFileName2, processDate, errors);
        if (dataFiles.size() != 2) {
            scheduledTaskLog(tradeId1, tradeId2, errors, valueDate, dataFiles);
            return false;
        }

        // load EUREX contracts
        if (!loadMMOOMeffCollateralConfigs(errors)) {
            scheduledTaskLog(tradeId1, tradeId2, errors, valueDate, dataFiles);
            return false;
        }

        // process files
        Vector<Vector<RecordMapper>> recordsFile1 = processFile(dataFiles.get(0), separator, errors);
        Vector<Vector<RecordMapper>> recordsFile2 = processFile(dataFiles.get(1), separator, errors);
        if (errors.size() > 0) {
            scheduledTaskLog(tradeId1, tradeId2, errors, valueDate, dataFiles);
            return false;
        }

        // read optional values
        fillOptionalVariables();

        // set the MMOO book

        // get trade amount (MtM) from valid records
        recordsFile1.get(0).addAll(recordsFile2.get(0)); // group by
        // cuentapropia
        recordsFile1.get(1).addAll(recordsFile2.get(1)); // group by
        // cuentaterceros
        Double mtmCuentaPropia = -getMtmFromRecords(recordsFile1.get(0));
        Double mtmCuentaTerceros = -getMtmFromRecords(recordsFile1.get(1));

        // get trades
        final String meffLegalEntity = this.meffContract.getLegalEntity().getCode(); // shortname
        Trade tradeCuentaPropia = ImportMMOOUtilities.getTrade(CAMARA, TRADE_TYPE_CUENTA_PROPIA, TRADE_ID_MEFF + "_1",
                meffLegalEntity, valueDate, this.meffContract.getBook());
        Trade tradeCuentaTerceros = ImportMMOOUtilities.getTrade(CAMARA, TRADE_TYPE_CUENTA_TERCEROS,
                TRADE_ID_MEFF + "_2", meffLegalEntity, valueDate, this.meffContract.getBook());

        // update trades & plmarks (using Mtm calculated before)
        tradeId1 = ImportMMOOUtilities.updateTradeAndPLMark(tradeCuentaPropia, mtmCuentaPropia, valueDate,
                getPricingEnv(), errors);
        tradeId2 = ImportMMOOUtilities.updateTradeAndPLMark(tradeCuentaTerceros, mtmCuentaTerceros, valueDate,
                getPricingEnv(), errors);

        // logging
        scheduledTaskLog(tradeId1, tradeId2, errors, valueDate, dataFiles);

        if (errors.size() > 0) {
            return false;
        }

        ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        return true;
    }

    /**
     * @param logErrTrades
     * @return true if MMOO Meff Collateral Configs loaded successfully
     */
    private boolean loadMMOOMeffCollateralConfigs(final ArrayList<String> logErrTrades) {

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
     * @param tradeId1
     * @param tradeId2
     * @param errors
     * @param dataFiles
     * @param dataFiles
     * @param processDate
     */
    private void scheduledTaskLog(long tradeId1, long tradeId2, ArrayList<String> errors, final JDate value,
                                  final List<File> dataFiles) {

        if ((errors != null) && (errors.size() > 0)) {
            Log.error(this, errors.toString());
        }

        String logPath = getAttribute(LOG_DIR);
        final String logName = getAttribute(LOG_NAME);
        final String startFileName1 = getAttribute(STARTFILENAME);
        final String startFileName2 = getAttribute(STARTFILENAME2);
        String filesNames = startFileName1 + ", " + startFileName2;
        if ((dataFiles != null) && (dataFiles.size() == 2)) {
            filesNames = dataFiles.get(0).getName() + ", " + dataFiles.get(1).getName();
        }

        final String extRef = super.getExternalReference();
        if (!logPath.endsWith("/")) {
            logPath = logPath + "/";
        }
        logPath += logName.trim();
        ImportMMOOUtilities.scheduledTaskLog(tradeId1, tradeId2, errors, logPath, filesNames, extRef, value);
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

        attributeList.add(attribute(STARTFILENAME));
        attributeList.add(attribute(SEPARATOR));
        attributeList.add(attribute(MEFF_CONTRACT_NAME));
        attributeList.add(attribute(CHOOSE_TRADE_EXT_REF_TYPE_GENERATION)
                .domain(Arrays.asList(new String[]{TRADE_ID_GENERATED, TRADE_EXT_REF_MANUAL})));
        attributeList.add(attribute(MAN_DEFAULT_EXT_REF));
        attributeList.add(attribute(MEFF_CUENTA_PROPIA_TYPE));
        attributeList.add(attribute(MEFF_CUENTA_TERCEROS_TYPE));
        attributeList.add(attribute(LOG_DIR));
        attributeList.add(attribute(LOG_NAME));

        return attributeList;
    }
    // /**
    // * ST attributes definition
    // */
    // @SuppressWarnings("unchecked")
    // @Override
    // public Vector<String> getDomainAttributes() {
    // final Vector<String> attr = super.getDomainAttributes();
    // attr.add(STARTFILENAME2);
    // attr.add(SEPARATOR);
    // attr.addElement(MEFF_CONTRACT_NAME);
    // attr.addElement(CHOOSE_TRADE_EXT_REF_TYPE_GENERATION);
    // attr.addElement(MAN_DEFAULT_EXT_REF);
    // attr.addElement(MEFF_CUENTA_PROPIA_TYPE);
    // attr.addElement(MEFF_CUENTA_TERCEROS_TYPE);
    // attr.addElement(LOG_DIR);
    // attr.addElement(LOG_NAME);
    //
    // return attr;
    // }

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
    // if (attribute.equals(CHOOSE_TRADE_EXT_REF_TYPE_GENERATION)) {
    //
    // vector.add(TRADE_ID_GENERATED);
    // vector.add(TRADE_EXT_REF_MANUAL);
    //
    // } else {
    //
    // vector = super.getAttributeDomain(attribute, hashtable);
    // }
    // return vector;
    // }

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
        String attribute = super.getAttribute(CHOOSE_TRADE_EXT_REF_TYPE_GENERATION);

        if (Util.isEmpty(attribute)) {

            messages.addElement(CHOOSE_TRADE_EXT_REF_TYPE_GENERATION + " MANDATORY attribute not specified. ");
            return false;

        }

        if (attribute.equals(TRADE_EXT_REF_MANUAL)) {

            attribute = super.getAttribute(MAN_DEFAULT_EXT_REF);
            if (Util.isEmpty(attribute)) {
                messages.addElement("If " + TRADE_EXT_REF_MANUAL + " is selected, " + MAN_DEFAULT_EXT_REF
                        + " attribute cannot be empty ");
                retVal = false;
            }
        }

        // check Meff contract associated
        attribute = super.getAttribute(MEFF_CONTRACT_NAME);
        if (!Util.isEmpty(attribute)) {

            try {
                this.meffContract = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfigByCode("NAME", attribute.trim());
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

        return retVal;
    }

    /**
     * This method overrides optional configurations like: default LE, External
     * reference, Trades subtypes for cuenta propia and cuenta terceros
     */
    private void fillOptionalVariables() {

        String attribute = super.getAttribute(CHOOSE_TRADE_EXT_REF_TYPE_GENERATION);
        String value = "";

        // add manual external ref initial & default fund trade ext reference
        if (!(Util.isEmpty(attribute) && attribute.equals(TRADE_EXT_REF_MANUAL))) {

            value = super.getAttribute(MAN_DEFAULT_EXT_REF);

            if (!(Util.isEmpty(value))) {
                TRADE_ID_MEFF = value.trim();
            }
        }

        // sub type cuenta propia name
        attribute = super.getAttribute(MEFF_CUENTA_PROPIA_TYPE);
        if (!(Util.isEmpty(attribute))) {
            TRADE_TYPE_CUENTA_PROPIA = attribute.trim();
        }
        // sub type cuenta terceros name
        attribute = super.getAttribute(MEFF_CUENTA_TERCEROS_TYPE);
        if (!(Util.isEmpty(attribute))) {
            TRADE_TYPE_CUENTA_TERCEROS = attribute.trim();
        }
    }

    // ***** FILES STUFF *** //

    /**
     * Get data files from path and specific date
     *
     * @param path
     * @param startFileName1
     * @param startFileName2
     * @param valueDate
     * @return
     */
    public List<File> getDataFiles(String path, String startFileName1, String startFileName2, JDate valueDate,
                                   ArrayList<String> errors) {

        List<File> dataFiles = new ArrayList<File>();

        // get file 1
        File file1 = CollateralUtilities.getFile(path, startFileName1, valueDate);
        if (file1 != null) {
            dataFiles.add(file1);
        } else {
            errors.add("Error getting file with start name " + startFileName1);
        }

        // get file 2
        File file2 = CollateralUtilities.getFile(path, startFileName2, valueDate);
        if (file2 != null) {
            dataFiles.add(file2);
        } else {
            errors.add("Error getting file with start name " + startFileName2);
        }

        return dataFiles;

    }

    // ***** PROCESS LOGIC STUFF ***** //

    /**
     * Process file getting valid records to take in account (parsed in
     * RecordMapper object)
     *
     * @param file
     * @param separator
     * @param errors
     * @return
     */
    public Vector<Vector<RecordMapper>> processFile(File file, String separator, ArrayList<String> errors) {

        String line = null;
        Vector<Vector<RecordMapper>> records = new Vector<Vector<RecordMapper>>();
        Vector<RecordMapper> cuentaPropiaRecords = new Vector<RecordMapper>();
        Vector<RecordMapper> cuentaTercerosRecords = new Vector<RecordMapper>();
        BufferedReader inputFileStream = null;
        @SuppressWarnings("unused")
        int i = 0;

        try {
            inputFileStream = new BufferedReader(new FileReader(file));
            for (i = 0; ((line = inputFileStream.readLine()) != null); i++) {
                // get record
                RecordMapper rm = new RecordMapper(line, separator);
                if (isCuentaPropia(rm, file)) {
                    cuentaPropiaRecords.add(rm);
                } else {
                    cuentaTercerosRecords.add(rm);
                }
            }
        } catch (FileNotFoundException e) {
            Log.error(this, e); //sonar
            errors.add("Error getting file: " + e.getMessage() + "\n");
        } catch (IOException e) {
            Log.error(this, e); //sonar
            errors.add("Error reading file: " + e.getMessage() + "\n");
        } finally {
            if (inputFileStream != null) {
                try {
                    inputFileStream.close();
                } catch (final IOException e) {
                    Log.error(this, e); //sonar
                    errors.add("Error closing file: " + e.getMessage() + "\n");
                }
            }
        }

        records.add(cuentaPropiaRecords);
        records.add(cuentaTercerosRecords);

        return records;

    }

    /**
     * Check if record cuentapropia o cuentaterceros
     *
     * @param rm
     * @param fileType
     * @return
     */
    public boolean isCuentaPropia(RecordMapper rm, File file) {

        boolean isValid = false;

        // Fichero 1
        if (file.getName().contains("C2")) {
            isValid = rm.getLiqMember().trim().equals("A911") && rm.getAccount().trim().equals("00P");
        }

        // Fichero 2
        if (file.getName().contains("C7")) {
            isValid = rm.getLiqMember().trim().equals("A911") && rm.getAccount().trim().equals("POI");
        }

        return isValid;

    }

    /**
     * Get sum of records amount
     *
     * @param validRecords
     * @return
     */
    public Double getMtmFromRecords(Vector<RecordMapper> records) {

        Double totalAmount = 0.0;
        for (RecordMapper rm : records) {
            totalAmount += rm.getAmount();
        }

        return totalAmount;

    }

    // ************************************* RECORD MAPPER CLASS
    // ************************************** //
    // ***** Class used to parse file records taking in account only fields we
    // are interested in ***** //
    // **********************************************************************************************
    // //
    private class RecordMapper {

        private final String liqMember;
        private final String account;
        private final Double amount;

        public RecordMapper(String record, String separator) {

            String[] values = record.split("\\" + separator, -1);

            // only take values we are interest on
            this.liqMember = values[2];
            this.account = values[3];
            // this.amount = Util.istringToDouble(values[5]);
            this.amount = getValue(values[5]);

        }

        public String getLiqMember() {
            return this.liqMember;
        }

        public String getAccount() {
            return this.account;
        }

        public Double getAmount() {
            return this.amount;
        }

        private Double getValue(String str) {
            final NumberFormat numberFormatter = new DecimalFormat("#0.00");
            Double value = 0.00;
            try {
                value = numberFormatter.parse(str).doubleValue();
            } catch (ParseException e) {
                Log.error(this, "Cannot parse number = " + str + ".\n", e);
            }
            return value;
        }

    }

    @Override
    public String getFileName() {
        return null;
    }

}
