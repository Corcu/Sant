package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.CSVPositionsBean;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import calypsox.util.GenericValidations;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.BOPositionAdjustment;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.email.MailException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * To import BOPositionAdjustment from a CSV file.
 *
 * @author Jose David Sevillano, David Porras Mart?nez & Guillermo Solano
 * @version 1.3: GD & SUSI book importation changes
 * @date 28/05/2013
 */
public class ScheduledTaskImportCSVPositions extends AbstractProcessFeedScheduledTask {

    // GSM: 07/11/2013. Adaptation to make a migration from the Batch to the
    // online.
    // only the portfolio vision of positions
    public static boolean MIGRATION_SDI = false;
    public static final String MIGRATION_MODE = "GD MIGRATION MODE";

    private static final String BOOKS_COLLATERAL = "BOOKS_COLLATERAL";
    private static final long serialVersionUID = 123L;
    private static final String ALIAS_BOOK_SYSTEM = "ALIAS_BOOK_";
    private static final String TRADE_TO_BE_CANCELED = "TRADE_TO_BE_CANCELED";
    private static final String FAILED = "FAILED";
    private static final String ACTUAL = "ACTUAL";
    private static final String SETTLED = "SETTLED";
    private static final String PENDING = "PENDING";
    private static final String THEORETICAL = "THEORETICAL";
    private static final String POSITION_STATUS = "POSITION_STATUS";
    private static final String PLEDGED = "PLEDGED";
    private static final String LOAN = "LOAN";
    private static final String REPO = "REPO";
    private static final String BUYSELL = "BUYSELL";
    private static final String COLLATERAL = "COLLATERAL";
    private static final String SECURITY = "SECURITY";
    private static final String BO_SYSTEM = "BO_SYSTEM";
    private static final String ISIN = "ISIN";
    private static final String SEPARATOR_DOMAIN_STRING = "Separator";
    private static final String SOURCE_SYSTEM = "Source System";
    private static final String TASK_INFORMATION = "Import POSITIONS from a CSV file.";
    private static final String POSITIONS = "POSITIONS";
    private static final String SUSI = "SUSI";
    private static final String GESTION_DISPONIBLE = "GD";
    private static final String BOOK_WARNING = "BOOK_WARNING";
    protected static final String STATIC_DATA_LOG = "Static Data Log";
    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    private static final String SUBJECT =
            "Log files for Import Positions on " + Defaults.getEnvName();
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    protected static final String PROCESS = "Load positions";
    private static final String SOURCE = "POSITION";
    private double finalQuantity;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private String fileName = "";
    private boolean processOK = true;
    private boolean controlMOK = true;
    private Vector<String> currencies;
    private Vector<Account> accounts;
    private static Product product;
    private static final String PRODUCT_OK = "PRODUCT_OK";
    private static final String NO_PRODUCT = "NO_PRODUCT";
    private static final String DIFFERENT_CCYS = "DIFFERENT_CCYS";
    private static final String NO_ISIN = "NO_ISIN";
    private static final String QTY = "QTY";
    private static final String SUBSTITUTABLE = "Y";
    private static final String NONE_AGENT = "NONE";
    private static final String NONE1_AGENT = "NONE1";
    private static final String REPROCESS = "Reprocess";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String BOND = "BOND";

    // log control
    protected final LogGeneric logGen = new LogGeneric();

    private DSConnection conn;
    private final Hashtable<String, String> hashAdjusType = new Hashtable<String, String>();
    private final Hashtable<String, String> hashPosType = new Hashtable<String, String>();
    private final Hashtable<String, InventorySecurityPosition> hashPositions =
            new Hashtable<String, InventorySecurityPosition>();
    private JDate valueDate = null;
    private boolean bResult = true;
    private String file = "";
    private String key;

    // added (Bean)
    private CSVPositionsBean csvPosBean;

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

        attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
        attributeList.add(attribute(SOURCE_SYSTEM));
        attributeList.add(attribute(SUMMARY_LOG));
        attributeList.add(attribute(DETAILED_LOG));
        attributeList.add(attribute(FULL_LOG));
        attributeList.add(attribute(STATIC_DATA_LOG));
        attributeList.add(attribute(REPROCESS).booleanType());
        attributeList.add(attribute(MIGRATION_MODE).booleanType());

        return attributeList;
    }

    //	@SuppressWarnings("unchecked")
    //	@Override
    //	public Vector<String> getDomainAttributes() {
    //		final Vector<String> vectorAttr = super.getDomainAttributes();
    //		vectorAttr.add(SEPARATOR_DOMAIN_STRING);
    //		vectorAttr.add(SOURCE_SYSTEM);
    //		vectorAttr.add(SUMMARY_LOG);
    //		vectorAttr.add(DETAILED_LOG);
    //		vectorAttr.add(FULL_LOG);
    //		vectorAttr.add(STATIC_DATA_LOG);
    //		vectorAttr.add(REPROCESS);
    //		vectorAttr.add(MIGRATION_MODE);
    //		return vectorAttr;
    //	}
    //
    //	@SuppressWarnings({ "rawtypes", "unchecked" })
    //	@Override
    //	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
    //		Vector vector = new Vector();
    //		if (attribute.equals(REPROCESS) || attribute.equals(MIGRATION_MODE)) {
    //			vector.addElement(TRUE);
    //			vector.addElement(FALSE);
    //		} else {
    //			vector = super.getAttributeDomain(attribute, hashtable);
    //		}
    //		return vector;
    //	}

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {
        this.currencies = LocalCache.getCurrencies();
        final Date d = new Date();
        String time = "";
        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        // fix
        this.logGen.decrementTotal();

        // Generation for the log files.
        this.logGen.generateFiles(
                getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG), time);

        final String path = getAttribute(FILEPATH);
        final String startFileName = getAttribute(STARTFILENAME);
        this.file = getAttribute(STARTFILENAME);
        this.conn = conn;

        // We fill the hashtable with the different types for the adjustments.
        this.hashAdjusType.put(BUYSELL, "Buy/Sell");
        this.hashAdjusType.put(REPO, "Repo");
        this.hashAdjusType.put(LOAN, "SecurityLending");
        this.hashAdjusType.put(PLEDGED, "Pledge");

        // We fill the hastable with the values for the position type attribute.
        this.hashPosType.put(THEORETICAL, THEORETICAL);
        this.hashPosType.put(SETTLED, ACTUAL);
        this.hashPosType.put(PENDING, FAILED);

        // we add the header and assign the fileWriter to the logs files.
        try {

            // Initialization for the logs files.
            this.logGen.initializeFiles(PROCESS);

            // We check all the files kept into the path specified in the
            // configuration for the Scheduled Task.
            final ArrayList<String> files = CollateralUtilities.getListFiles(path, this.file);

            // We check if the number of matches is 1.
            if (files.size() == 1) {
                this.file = files.get(0);
                this.fileName = files.get(0);

                this.logGen.initilizeStaticDataLog(this.file, "LINE");

                final String filePath = path + this.file;
                final String separator = getAttribute(SEPARATOR_DOMAIN_STRING);

                try {
                    if (feedPreProcessSusiGBO(filePath)) {

                        // GSM: 14/09/2013. Just after file verifications, this
                        // method will make a copy into the
                        // ./import/copy/ directory
                        FileUtility.copyFileToDirectory(filePath, path + "/copy/");

                        // GSM: 07/11/2013. Adaptation to make a migration from
                        // the Batch to the online.
                        // only the portfolio vision of positions
                        if (!Util.isEmpty(getAttribute(MIGRATION_MODE))
                                && getAttribute(MIGRATION_MODE).equals(TRUE)) {
                            MIGRATION_SDI = true;
                        }

                        // process
                        readPositionAdjustment(filePath, separator);
                    } else {
                        this.logGen.incrementRecordErrors();
                        this.logGen.setErrorNumberOfLines(SOURCE, this.file);
                        ControlMErrorLogger.addError(
                                ErrorCodeEnum.ControlLine,
                                "Number of lines in file does not match with number of lines in control record");
                        this.controlMOK = false;
                        this.logGen.feedFullLog(0);
                        this.logGen.feedDetailedLog(0);

                        this.processOK = false;
                        this.bResult = false;
                    }
                } catch (final Exception e) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString(), e);
                    this.bResult = false;
                    if (e.getMessage() != null) {
                        if (e.getMessage().equals("Error in the number of lines.")) {
                            // error number 3
                            this.logGen.incrementRecordErrors();
                            this.logGen.setErrorNumberOfLines(SOURCE, this.file);
                            ControlMErrorLogger.addError(
                                    ErrorCodeEnum.ControlLine,
                                    "Number of lines in file does not match with number of lines in control record");
                            this.controlMOK = false;
                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);

                            this.processOK = false;
                        } else if (e.getMessage().equals("Error in date of the file.")) {
                            // error number 41
                            this.logGen.incrementRecordErrors();
                            this.logGen.setErrorDateNotValid(
                                    SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()), "", "");

                            ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "Date not valid");
                            this.controlMOK = false;
                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);

                            this.processOK = false;
                        } else {
                            // error number 2
                            this.logGen.setErrorOpeningFile(SOURCE, this.fileName, "0");

                            ControlMErrorLogger.addError(
                                    ErrorCodeEnum.IOException, "Unexpected error opening the file");
                            this.controlMOK = false;
                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);

                            this.processOK = false;
                        }
                    }
                }
            } else {
                Log.error(
                        LOG_CATEGORY_SCHEDULED_TASK,
                        "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");

                // error number
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorNumberOfFiles(SOURCE, this.file);
                ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
                this.controlMOK = false;
                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);

                this.bResult = false;
                this.processOK = false;
            }
        } catch (final IOException e2) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files. \n" + e2); // sonar

            this.logGen.incrementRecordErrors();
            this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
            ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Error creating log files");
            // this.controlMOK = false;
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);

            this.bResult = false;
            this.processOK = false;
        }

        try {
            this.feedPostProcess(this.bResult);

            this.logGen.closeLogFiles();

            String sumLog = "";
            if (this.file.equals("")) {
                sumLog =
                        this.logGen.feedGenericLogProcess(
                                startFileName, getAttribute(SUMMARY_LOG), PROCESS, this.logGen.getNumberTotal());
            } else {
                sumLog =
                        this.logGen.feedGenericLogProcess(
                                this.file, getAttribute(SUMMARY_LOG), PROCESS, this.logGen.getNumberTotal());
            }
            try {
                if (!sumLog.equals("")) {
                    final List<String> to = conn.getRemoteReferenceData().getDomainValues(POSITIONS);
                    final ArrayList<String> attachments = new ArrayList<String>();
                    attachments.add(sumLog);

                    attachments.add(this.logGen.getStringDetailedLog());
                    attachments.add(this.logGen.getStringFullLog());
                    attachments.add(this.logGen.getStringStaticDataLog());

                    CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
                }
            } catch (final MailException me) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, me);
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorSentEmail(SOURCE, startFileName);

                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);

                this.processOK = false;
                this.bResult = false;
            }

        } catch (final Exception e) {
            Log.error(
                    LOG_CATEGORY_SCHEDULED_TASK, "Error moving historic files and creating bad file.\n");
            Log.error(this, e); // sonar
            this.bResult = false;
            this.processOK = false;
            this.logGen.incrementRecordErrors();
            this.logGen.setErrorMovingFile(SOURCE, startFileName);
            ControlMErrorLogger.addError(
                    ErrorCodeEnum.InputFileCanNotBeMoved, "Error moving historic files");
            this.controlMOK = false;
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
        }

        // Close log files.
        try {
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
            Log.error(this, e); // sonar
        }

        if (this.controlMOK) {
            ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
        }
        return this.processOK;
    }

    private void batchError(int nline, String line, String field) {

        this.bResult = false;
        // warning number 17
        this.logGen.incrementWarning();
        this.logGen.setWarningRequiredFieldNotPresentNotValid(
                SOURCE,
                this.fileName,
                String.valueOf(nline),
                "17",
                "PORTFOLIO",
                field,
                this.csvPosBean.getIsin(),
                line);
    }

    /**
     * To validate if exists a book in the system using the internal mapping in Calypso. For instance,
     * we need to do a mapping between the book obtained from SUSI and the Calypso one.
     *
     * @param sourceBook Book provided by the source system.
     * @param alias      Alias used to identify the source system, to do the mapping.
     * @return TRUE if exists, FALSE if does not exist.
     * @throws RemoteException Problem accessing to the database.
     */
    public boolean validatePortfolioMapped(
            final String sourceBook, final String alias, final String bo_system) throws RemoteException {

        String bookMapped = null;

        // We get the BOOK from the system.
        if (bo_system.equalsIgnoreCase(SUSI)) { // SUSI

            // GSM: 26/06/2013. Short/long Portfolio Development. Returns first
            // short name, if not tries long

            final String shortAlias = ALIAS_BOOK_SYSTEM + getAttribute(SOURCE_SYSTEM);
            final String longAlias = shortAlias + "_LONG";

            if (GenericValidations.validatePortfolioMapped(
                    this.csvPosBean.getPortfolio(), longAlias, shortAlias)) {

                bookMapped =
                        CollateralUtilities.getBookMappedName(
                                this.csvPosBean.getPortfolio(), ALIAS_BOOK_SYSTEM + getAttribute(SOURCE_SYSTEM));
            }
        } else if (bo_system.equalsIgnoreCase(GESTION_DISPONIBLE)) {

            // if GD -> we take directly the name of the Book
            bookMapped = this.csvPosBean.getPortfolio().trim();

        } else {
            bookMapped = null;
        }

        if ((null != bookMapped) && !"".equals(bookMapped) && !bookMapped.startsWith(BOOK_WARNING)) {
            // We get the BOOK from the system.
            final Book book = DSConnection.getDefault().getRemoteReferenceData().getBook(bookMapped);
            if (book == null) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Method to read the CSV file specified in the related attribute (Start File Name), and call the
     * methods to create or update the quantity for the SecurityTransfer.
     *
     * @param filePath  File path + start file name attributes.
     * @param separator Separator specified for the CSV file read.
     * @return TRUE if the process is correct, else FALSE.
     * <p>MODIFIED (Bean)
     */
    @SuppressWarnings({"unused", "unchecked"})
    private void readPositionAdjustment(final String filePath, final String separator) {
        BufferedReader inputFileStream = null; // Input stream to read the CSV
        // file.
        String line = null; // Lines of the file.
        String[] values = null; // Values separates for each field of each line.
        InventorySecurityPosition invSecPos;
        int flag = 0, flag2 = 0;

        try {
            inputFileStream = new BufferedReader(new FileReader(filePath));

            // We check if the book in the file is in the DomainValues to
            // exclude it.
            boolean isCollateralBook = false;
            String booksCollQuery = "";
            final Vector<String> booksCollat =
                    this.conn.getRemoteReferenceData().getDomainValues(BOOKS_COLLATERAL);
            // We create the String to be used in the query to get the
            // positions from the system.
            for (int numBooks = 0; numBooks < booksCollat.size(); numBooks++) {
                Book colBook = this.conn.getRemoteReferenceData().getBook(booksCollat.get(numBooks));
                if (null != colBook) {
                    booksCollQuery = booksCollQuery + colBook.getId();
                }

                if (numBooks < (booksCollat.size() - 1)) {
                    booksCollQuery = booksCollQuery + ",";
                }
            }

            // We read the file.
            for (int i = 0; ((line = inputFileStream.readLine()) != null) && (flag == 0); i++) {
                flag2 = 0;

                if (!line.startsWith("*****")) {
                    this.logGen.incrementTotal();
                    this.logGen.initializeError();
                    this.logGen.initializeWarning();

                    this.logGen.initializeErrorLine();
                    this.logGen.initializeWarningLine();
                    this.logGen.initializeOkLine();

                    if ((CollateralUtilities.checkFields(line, '|', 12))
                            || (CollateralUtilities.checkFields(line, '|', 14))) {

                        values = CollateralUtilities.splitMejorado(countFileds(line, '|'), "|", true, line);
                        for (int ii = 0; ii < values.length; ii++) {
                            values[ii] = values[ii].trim();
                        }

                        this.csvPosBean = new CSVPositionsBean(values);
                        this.key = "";

                        // GSM: 07/11/2013. Adaptation to migrate from GD batch
                        // to online (portfolio vision)
                        if (MIGRATION_SDI) {
                            this.csvPosBean.setCustodian("DUMMY_AGENT");
                            this.csvPosBean.setAccount("1@DUMMY");
                            // String accountName = this.csvPosBean.getAccount()
                            // + '@'+ this.csvPosBean.getCustodian();
                            // DUMMY_AGENT|1@DUMMY
                        }

                        // First of all, we have to check if the balance in the
                        // line is zero. If so, we ignore the data,
                        // but if not, we do the process as usual.
                        if (!"0".equals(this.csvPosBean.getQtyNominalSec())
                                && !"0.0".equals(this.csvPosBean.getQtyNominalSec())) {
                            // We check if the data in the field identified by
                            // AGENT info is related with a Legal Entity
                            // with role Agent in Calypso.

                            String custodian = this.csvPosBean.getCustodian();
                            if ((null != custodian) && !"".equals(custodian)) {
                                LegalEntity leCustodian =
                                        this.conn.getRemoteReferenceData().getLegalEntity(custodian);
                                if (null != leCustodian) {
                                    Vector<String> rolesCustodian = leCustodian.getRoleList();
                                    if ((null != rolesCustodian) && !rolesCustodian.isEmpty()) {
                                        if (rolesCustodian.contains("Agent")) {
                                            // New modification for managing the
                                            // Collateral Books (to avoid
                                            // touching
                                            // them).
                                            String validateIsinReturn =
                                                    GenericValidations.validateIsin(
                                                            BOND,
                                                            this.csvPosBean.getIsin(),
                                                            this.csvPosBean.getQtyNominalSecCcy());

                                            if (validateIsinReturn.contains(PRODUCT_OK)) {
                                                final Vector<Product> products =
                                                        DSConnection.getDefault()
                                                                .getRemoteProduct()
                                                                .getProductsByCode(ISIN, this.csvPosBean.getIsin());
                                                product =
                                                        products.get(
                                                                Integer.valueOf(validateIsinReturn.substring(PRODUCT_OK.length())));

                                                // -----------------------------------------------------------------//
                                                /*
                                                 * GSM: FIX. This import should
                                                 * work for SUSI positions and
                                                 * GD. Susi maps the book using
                                                 * the Alias, but GD uses
                                                 * directly the same
                                                 * book(portfolio) included in
                                                 * the system, without alias.
                                                 * Based on the Source System,
                                                 * the book is gathered from the
                                                 * mapping book (SUSI) or we use
                                                 * the book directly read from
                                                 * GD
                                                 */

                                                /*
                                                 * We do the mapping between the
                                                 * book sent from the source
                                                 * system and our book (in
                                                 * Calypso).
                                                 */
                                                final String bo_system = getAttribute(SOURCE_SYSTEM);
                                                if ((bo_system == null) || bo_system.isEmpty()) {

                                                    batchError(i + 1, line, this.csvPosBean.getPortfolio());
                                                    continue;
                                                }
                                                String bookMapped = null;
                                                // We do the mapping between the
                                                // book sent from
                                                // the source system and our
                                                // book (in Calypso).
                                                if (validatePortfolioMapped(
                                                        this.csvPosBean.getPortfolio(),
                                                        ALIAS_BOOK_SYSTEM + getAttribute(SOURCE_SYSTEM),
                                                        bo_system)) {

                                                    // first for susi

                                                    if (bo_system.equalsIgnoreCase(SUSI)) { // SUSI

                                                        // GSM: 26/06/2013.
                                                        // Short/long Portfolio
                                                        // Development. Returns
                                                        // first short name, if
                                                        // not tries long

                                                        // bookMapped =
                                                        // CollateralUtilities.getBookMapped(
                                                        // this.csvPosBean.getPortfolio(),
                                                        // ALIAS_BOOK_SYSTEM
                                                        // +
                                                        // getAttribute(SOURCE_SYSTEM));

                                                        bookMapped =
                                                                CollateralUtilities.getBookMappedName(
                                                                        this.csvPosBean.getPortfolio(),
                                                                        ALIAS_BOOK_SYSTEM + getAttribute(SOURCE_SYSTEM));

                                                    } else if (bo_system.equalsIgnoreCase(GESTION_DISPONIBLE)) {
                                                        // if GD -> we take
                                                        // directly the name of
                                                        // the Book
                                                        bookMapped = this.csvPosBean.getPortfolio().trim();

                                                    } else {

                                                        bookMapped = null;
                                                    }
                                                    // We get the BOOK from the
                                                    // system.
                                                    final Book book =
                                                            (bookMapped == null)
                                                                    ? null
                                                                    : this.conn.getRemoteReferenceData().getBook(bookMapped);

                                                    if ((book == null) || (bookMapped == null)) {
                                                        batchError(i + 1, line, this.csvPosBean.getPortfolio());
                                                        continue;
                                                    }

                                                    // final Book book =
                                                    // this.conn.getRemoteReferenceData().getBook(
                                                    // bookMapped);
                                                    /*
                                                     * GSM: End hotfix -> book
                                                     * based on SUSI or GD
                                                     */
                                                    // We get the account.
                                                    this.accounts = null;
                                                    // GSM: 07/11/2013.
                                                    // Adaptation to migrate
                                                    // from GD batch to online
                                                    // (portfolio vision)
                                                    String accountName = "";
                                                    if (!MIGRATION_SDI) {
                                                        accountName =
                                                                this.csvPosBean.getAccount() + '@' + this.csvPosBean.getCustodian();
                                                    } else {
                                                        accountName = this.csvPosBean.getAccount().trim();
                                                    }

                                                    String whereAcc =
                                                            "acc_account_name = '"
                                                                    + accountName
                                                                    + "' AND le_id = "
                                                                    + leCustodian.getId();
                                                    this.accounts = this.conn.getRemoteAccounting().getAccounts(whereAcc, null);

                                                    if ((this.accounts != null) && (this.accounts.size() > 0)) {
                                                        // We generate the key
                                                        // for the Hashmap.
                                                        this.key =
                                                                product.getId()
                                                                        + ";"
                                                                        + book.getId()
                                                                        + ";"
                                                                        + leCustodian.getId()
                                                                        + ";"
                                                                        + this.accounts.get(0).getId();

                                                        // To check the
                                                        // Collateral Books.
                                                        if ((null != booksCollat) && !booksCollat.isEmpty()) {
                                                            if (booksCollat.contains(bookMapped)) {
                                                                isCollateralBook = true;
                                                            }
                                                        }

                                                        // If COLLATERAL or the
                                                        // book is in the
                                                        // DomainValues to
                                                        // exclude it, we pass
                                                        // to the next line.
                                                        if (!COLLATERAL.equals(this.csvPosBean.getPosType())
                                                                && !isCollateralBook) {
                                                            this.csvPosBean.setPortfolio(bookMapped);

                                                            // Reset the value
                                                            // for the
                                                            // variable
                                                            // 'finalQuantity' &
                                                            // the trade.
                                                            this.finalQuantity = 0;
                                                            invSecPos = null;

                                                            // if it's first
                                                            // time, get all
                                                            // positions
                                                            if (this.hashPositions.size() == 0) {
                                                                // get date
                                                                final int day =
                                                                        Integer.parseInt(
                                                                                this.csvPosBean.getValueDate().substring(0, 2));
                                                                final int month =
                                                                        Integer.parseInt(
                                                                                this.csvPosBean.getValueDate().substring(3, 5));
                                                                final int year =
                                                                        Integer.parseInt(this.csvPosBean.getValueDate().substring(6));

                                                                this.valueDate = JDate.valueOf(year, month, day);

                                                                getInventorySecPositions(this.valueDate, booksCollQuery);
                                                            }

                                                            // get quantity for
                                                            // actual position
                                                            if (this.hashPositions.get(this.key) != null) {
                                                                this.finalQuantity = this.hashPositions.get(this.key).getTotal();
                                                            }

                                                            try {
                                                                final Bond bond = (Bond) product;

                                                                // comprobar
                                                                // bono vencido
                                                                if (JDate.diff(
                                                                        JDate.valueOf(this.csvPosBean.getValueDate()),
                                                                        bond.getMaturityDate())
                                                                        > 0) {

                                                                    // get file
                                                                    // quantity
                                                                    double diff = 0.0;
                                                                    if ((this.csvPosBean.getQtyNom() != null)
                                                                            && (!this.csvPosBean.getQtyNom().equals(""))
                                                                            && ((this.csvPosBean.getQtyNom().equals("QTY"))
                                                                            || (this.csvPosBean.getQtyNom().equals("NOM")))) {

                                                                        try {
                                                                            Double.parseDouble(this.csvPosBean.getQtyNominalSec());
                                                                        } catch (Exception e) {
                                                                            this.bResult = false;
                                                                            Log.error(this, e); // sonar
                                                                            this.logGen.incrementError();
                                                                            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                                                                    SOURCE,
                                                                                    this.file,
                                                                                    String.valueOf(i + 1),
                                                                                    "47",
                                                                                    "QTY_NOM_SEC",
                                                                                    this.csvPosBean.getIsin(),
                                                                                    line);
                                                                        }

                                                                        if ("NOM".equals(this.csvPosBean.getQtyNom().toUpperCase())) {
                                                                            if (bond.getFaceValue() != 0) {
                                                                                diff =
                                                                                        (Double.parseDouble(this.csvPosBean.getQtyNominalSec())
                                                                                                / bond.getFaceValue())
                                                                                                - this.finalQuantity;
                                                                            } else {
                                                                                Log.error(
                                                                                        LOG_CATEGORY_SCHEDULED_TASK,
                                                                                        "Face value for the bond:"
                                                                                                + bond.getName()
                                                                                                + " is zero.");
                                                                                this.bResult = false;

                                                                                this.logGen.incrementError();
                                                                                this.logGen.setErrorFaceValue(
                                                                                        SOURCE,
                                                                                        this.file,
                                                                                        String.valueOf(i + 1),
                                                                                        this.csvPosBean.getIsin(),
                                                                                        line);

                                                                                flag2 = 1;
                                                                            }
                                                                        } else {
                                                                            diff =
                                                                                    Double.parseDouble(this.csvPosBean.getQtyNominalSec())
                                                                                            - this.finalQuantity;
                                                                        }
                                                                    } else {
                                                                        this.bResult = false;
                                                                        this.logGen.incrementError();
                                                                        this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                                                                SOURCE,
                                                                                this.file,
                                                                                String.valueOf(i + 1),
                                                                                "46",
                                                                                "QTY_NOM",
                                                                                this.csvPosBean.getIsin(),
                                                                                line);
                                                                    }

                                                                    if (flag2 == 0) {
                                                                        if (diff == 0.0) {
                                                                            this.logGen.setOkLine(
                                                                                    SOURCE,
                                                                                    this.fileName,
                                                                                    this.logGen.getNumberTotal(),
                                                                                    String.valueOf(0));
                                                                        } else {
                                                                            this.csvPosBean.setQtyNominalSec(String.valueOf(diff));

                                                                            insertMovement(this.csvPosBean, String.valueOf(false), line);
                                                                        }
                                                                    }
                                                                } else {
                                                                    this.bResult = false;
                                                                    this.logGen.incrementWarning();
                                                                    // GSM:
                                                                    // 28/05/2013.
                                                                    // I_066.
                                                                    // MaturedBond
                                                                    // as
                                                                    // Warning,
                                                                    // not error
                                                                    // this.logGen.incrementError();
                                                                    this.logGen.setWarningMaturedBond(
                                                                            SOURCE,
                                                                            this.fileName,
                                                                            String.valueOf(i + 1),
                                                                            this.csvPosBean.getIsin(),
                                                                            line);
                                                                }
                                                            } catch (final Exception e) {
                                                                Log.error(this, e); // sonar
                                                                if (e.getMessage() != null) {
                                                                    if (e.getMessage().equals("Cannot save the trade")) {

                                                                        flag = 1;
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Log.error(
                                                                    LOG_CATEGORY_SCHEDULED_TASK, "Kind of trade is COLLATERAL.");
                                                            this.bResult = false;
                                                            // error number 62
                                                            this.logGen.incrementError();
                                                            this.logGen.setErrorCollateral(
                                                                    SOURCE,
                                                                    this.file,
                                                                    String.valueOf(i + 1),
                                                                    this.csvPosBean.getIsin(),
                                                                    line);
                                                        }
                                                    } else {
                                                        this.logGen.incrementError();
                                                        this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                                                SOURCE,
                                                                this.fileName,
                                                                String.valueOf(this.logGen.getNumberTotal()),
                                                                "37",
                                                                "ACCOUNT",
                                                                this.csvPosBean.getIsin(),
                                                                line);

                                                        this.bResult = false;
                                                    }

                                                } else {
                                                    if (CollateralUtilities.getBookMapped(
                                                            this.csvPosBean.getPortfolio(),
                                                            ALIAS_BOOK_SYSTEM + getAttribute(SOURCE_SYSTEM))
                                                            .startsWith("BOOK_WARNING_MORE_BOOKS")) {
                                                        this.bResult = false;
                                                        this.logGen.incrementError();
                                                        this.logGen.setErrorPortfolioManyReferences(
                                                                SOURCE,
                                                                this.file,
                                                                String.valueOf(i + 1),
                                                                this.csvPosBean.getIsin(),
                                                                line);
                                                    } else {
                                                        this.bResult = false;
                                                        // warning number 17
                                                        this.logGen.incrementWarning();
                                                        this.logGen.setWarningRequiredFieldNotPresentNotValid(
                                                                SOURCE,
                                                                this.fileName,
                                                                String.valueOf(i + 1),
                                                                "17",
                                                                "PORTFOLIO",
                                                                this.csvPosBean.getPortfolio(),
                                                                this.csvPosBean.getIsin(),
                                                                line);
                                                    }
                                                }
                                            } else if (validateIsinReturn.equals(DIFFERENT_CCYS)) {
                                                this.bResult = false;
                                                this.logGen.incrementWarning();
                                                this.logGen.setWarningNoMatchCcyBond(
                                                        SOURCE,
                                                        this.file,
                                                        String.valueOf(i + 1),
                                                        this.csvPosBean.getIsin(),
                                                        line);
                                            } else if (validateIsinReturn.equals(NO_PRODUCT)) {
                                                this.bResult = false;
                                                this.logGen.incrementError();
                                                this.logGen.setErrorGettingBond(
                                                        SOURCE,
                                                        this.file,
                                                        String.valueOf(i + 1),
                                                        this.csvPosBean.getIsin(),
                                                        line);
                                            } else if (validateIsinReturn.equals(NO_ISIN)) {
                                                this.bResult = false;
                                                this.logGen.incrementWarning();
                                                this.logGen.setWarningRequiredFieldNotPresentNotValid(
                                                        SOURCE,
                                                        this.fileName,
                                                        String.valueOf(this.logGen.getNumberTotal()),
                                                        "22",
                                                        "ISIN",
                                                        this.csvPosBean.getIsin(),
                                                        line);
                                            }
                                        } else {
                                            this.bResult = false;
                                            this.logGen.incrementWarning();
                                            this.logGen.setWarningCustodianRol(
                                                    SOURCE, this.fileName, String.valueOf(i + 1), custodian, line);
                                        }
                                    } else {
                                        this.logGen.incrementWarning();
                                        this.logGen.setWarningRequiredFieldNotPresentNotValid(
                                                SOURCE,
                                                this.fileName,
                                                String.valueOf(this.logGen.getNumberTotal()),
                                                "36",
                                                "CUSTODIAN",
                                                this.csvPosBean.getIsin(),
                                                line);

                                        this.bResult = false;
                                    }
                                } else {
                                    this.logGen.incrementWarning();
                                    this.logGen.setWarningRequiredFieldNotPresentNotValid(
                                            SOURCE,
                                            this.fileName,
                                            String.valueOf(this.logGen.getNumberTotal()),
                                            "36",
                                            "CUSTODIAN",
                                            this.csvPosBean.getIsin(),
                                            line);

                                    this.bResult = false;
                                }

                            } else {
                                this.logGen.incrementError();
                                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                        SOURCE,
                                        this.fileName,
                                        String.valueOf(this.logGen.getNumberTotal()),
                                        "36",
                                        "CUSTODIAN",
                                        this.csvPosBean.getIsin(),
                                        line);

                                this.bResult = false;
                            }
                        } else {
                            this.logGen.setOkLine(
                                    SOURCE, this.fileName, this.logGen.getNumberTotal(), String.valueOf(0));
                        }
                    } else {
                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
                        this.bResult = false;
                        // error number 5
                        this.logGen.incrementError();
                        this.logGen.setErrorBadRecordFormat(
                                SOURCE, this.file, String.valueOf(i + 1), "", line, this.csvPosBean.getIsin());
                    }

                    if (this.logGen.getNumberError() > 0) {
                        this.logGen.incrementRecordErrors();
                    }
                    if ((this.logGen.getNumberWarning() > 0) && (this.logGen.getNumberError() == 0)) {
                        this.logGen.incrementRecordWarning();
                        try {
                            String sourceSys = getAttribute(SOURCE_SYSTEM);
                            if (sourceSys == null) {
                                sourceSys = "";
                            }

                            this.logGen.feedStaticDataLog(
                                    String.valueOf(this.logGen.getNumberTotal()), sourceSys);
                        } catch (final Exception e) {
                            Log.error(
                                    LOG_CATEGORY_SCHEDULED_TASK,
                                    "Error. Error writing in log files. \n" + e); // sonar
                            this.logGen.setErrorWritingLog(SOURCE, this.fileName, String.valueOf(i + 1));

                            this.logGen.feedFullLog(0);
                            this.logGen.feedDetailedLog(0);
                            this.bResult = false;
                        }
                        this.addBadLine(line, "Error in line");
                    }

                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);

                    // We remove from the HashTable the position read to avoid
                    // put to zero later.
                    this.hashPositions.remove(this.key);
                }
            }

            // PONER POSICIONES QUE HAYAN QUEDADO EN EL HASHMAP A 0
            String isReprocess = getAttribute(REPROCESS);
            if (!Util.isEmpty(isReprocess) && isReprocess.equals(FALSE)) {
                setPositionsToZero();
            }

        } catch (final FileNotFoundException e1) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file: " + getFileName(), e1);
            this.bResult = false;
            // error number 1
            this.logGen.incrementRecordErrors();
            this.processOK = false;
            this.logGen.setErrorNumberOfFiles(SOURCE, this.fileName);
            ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
            this.controlMOK = false;
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
        } catch (final IOException e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file: " + getFileName(), e);
            this.bResult = false;
            // error number 2
            this.logGen.incrementRecordErrors();
            this.processOK = false;
            this.logGen.setErrorOpeningFile(SOURCE, this.fileName, "0");

            ControlMErrorLogger.addError(ErrorCodeEnum.IOException, "Unexpected error opening the file");
            this.controlMOK = false;
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
        } finally {
            if (inputFileStream != null) {
                try {
                    inputFileStream.close();
                    // this.bResult = true && !hasBadEntries();
                } catch (final IOException e) {
                    Log.error(
                            LOG_CATEGORY_SCHEDULED_TASK,
                            "Error while trying close input stream for the CSV file <"
                                    + getFileName()
                                    + "> open previously",
                            e);
                    this.bResult = false;
                    this.processOK = false;
                }
            }
        }
    }

    /**
     * Method to do the operation.
     *
     * @param csvPosBean2       CSVPositionsBean with the values for each line read.
     * @param tradeToBeCanceled String parameter that indicates if the Trade is to be canceled in the
     *                          future or not (TRUE or FALSE).
     * @throws Exception
     * @throws CloneNotSupportedException MODIFIED (Bean)
     */
    private void insertMovement(
            final CSVPositionsBean csvPosBean2, final String tradeToBeCanceled, final String strLine)
            throws Exception {
        Trade newTrade = null;
        BOPositionAdjustment boPosAdjust = null;

        // Fill the information for the product & trade.
        try {
            boPosAdjust = loadProductData(csvPosBean2, strLine);
            newTrade = loadTradeData(boPosAdjust, csvPosBean2, tradeToBeCanceled, strLine);
        } catch (final Exception e) {
            Log.error(this, e); // sonar
            if (e.getMessage() != null) {
                if (e.getMessage().equals("Required field ISIN")) {
                    throw new Exception("Required field ISIN");
                }
            }
            if (e.getMessage() != null) {
                if (e.getMessage().equals("Required field PORTFOLIO")) {
                    throw new Exception("Required field PORTFOLIO");
                }
            }
        }
        // Save the new Trade & Product, when the newTrade variable is not null.
        if (null != newTrade) {
            newTrade.setAction(Action.NEW);
        }

        // Not save trade if there was any error/warning before
        if ((this.logGen.getNumberError() > 0) || (this.logGen.getNumberWarning() > 0)) {
            return;
        }

        try {
            long tradeId = this.conn.getRemoteTrade().save(newTrade);
            Log.debug(
                    LOG_CATEGORY_SCHEDULED_TASK,
                    "Trade saved. Isin=" + csvPosBean2.getIsin() + ", TradeId=" + tradeId);
        } catch (final RemoteException e) {
            // Cannot save the trade. Error 5
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving trades", e);
            this.logGen.incrementError();
            this.logGen.setErrorSavingTrade(
                    SOURCE,
                    this.fileName,
                    String.valueOf(this.logGen.getNumberTotal()),
                    this.csvPosBean.getIsin(),
                    strLine);
            // ControlMErrorLogger.addError(ErrorCodeEnum.InvalidData,
            // "Cannot save the trade");
            this.bResult = false;
            this.processOK = false;
            throw new Exception("Cannot save the trade");
        }
        this.logGen.setOkLine(
                SOURCE, this.fileName, this.logGen.getNumberTotal(), String.valueOf(newTrade.getLongId()));
    }

    /**
     * To load the information related to a product specified into the CSV file.
     *
     * @param csvPosBean2 CSVPositionsBean with the values for each line read.
     * @return Product loaded or a new one created.
     * @throws Exception
     * @throws RemoteException If it doesn't exist the Legal Entity read from the CSV file in the
     *                         DataBase.
     *                         <p>MODIFIED (Bean)
     */
    private BOPositionAdjustment loadProductData(
            final CSVPositionsBean csvPosBean2, final String strLine) throws Exception {
        final BOPositionAdjustment boPosAux = new BOPositionAdjustment();

        if (null != csvPosBean2) {
            // PO. We retrieve the ID for the PO passed as a parameter.
            try {

                if (GenericValidations.validateLegalEntity(csvPosBean2.getProcessingOrg())) {
                    final LegalEntity legalEntity =
                            this.conn.getRemoteReferenceData().getLegalEntity(csvPosBean2.getProcessingOrg());
                    boPosAux.setOrdererLeId(legalEntity.getId());
                    boPosAux.setOrdererRole(LegalEntity.PROCESSINGORG);

                } else {
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresent(
                            SOURCE,
                            this.fileName,
                            String.valueOf(this.logGen.getNumberTotal()),
                            "14",
                            "PROCESSING_ORG",
                            csvPosBean2.getIsin(),
                            strLine);

                    this.bResult = false;
                }
            } catch (final Exception e) {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(
                        SOURCE,
                        this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "14",
                        "PROCESSING_ORG",
                        csvPosBean2.getIsin(),
                        strLine);
                Log.error(this, e); // sonar
                this.bResult = false;
            }

            // Security Description

            if (!csvPosBean2.getSecurityDesc().equals("")) {
                product.setComment(csvPosBean2.getSecurityDesc());
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresent(
                        SOURCE,
                        this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "56",
                        "SECURITY_DESCRIPTION",
                        csvPosBean2.getIsin(),
                        strLine);

                this.bResult = false;
            }
            boPosAux.setSecurity(product);

            // We fill all the fields for the BOPositionAdjustment object.
            boPosAux.setFlowType(SECURITY);

            if (!Util.isEmpty(csvPosBean2.getQtyNominalSecCcy())
                    && this.currencies.contains(csvPosBean2.getQtyNominalSecCcy())) {
                boPosAux.setCurrencyCash(csvPosBean2.getQtyNominalSecCcy());
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(
                        SOURCE,
                        this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "48",
                        "QTY_NOMINAL_SEC_CCY",
                        csvPosBean2.getIsin(),
                        strLine);
                this.bResult = false;
            }
        }

        return boPosAux;
    }

    /**
     * To load the information for the Trade.
     *
     * @param productAdjustment Product to associate as the security.
     * @param csvPosBean2       CSVPositionsBean with the values for each line read.
     * @param tradeToBeCanceled String parameter that indicates if the Trade is to be canceled in the
     *                          future or not (TRUE or FALSE).
     * @return New trade created.
     * @throws Exception
     */
    @SuppressWarnings({"unchecked"})
    private Trade loadTradeData(
            final BOPositionAdjustment productAdjustment,
            final CSVPositionsBean csvPosBean2,
            final String tradeToBeCanceled,
            final String strLine)
            throws Exception {
        final Trade localTrade = new Trade();
        JDate jdate = null;

        if (csvPosBean2 != null) {
            // Portfolio
            final Book paramBook = this.conn.getRemoteReferenceData().getBook(csvPosBean2.getPortfolio());
            if (paramBook != null) {
                localTrade.setBook(paramBook);

                // Value Date
                try {
                    // check format
                    synchronized (dateFormat) {
                        dateFormat.setLenient(false);
                        dateFormat.parse(csvPosBean2.getValueDate());
                    }

                    // create jdate
                    final int day = Integer.parseInt(csvPosBean2.getValueDate().substring(0, 2));
                    final int month = Integer.parseInt(csvPosBean2.getValueDate().substring(3, 5));
                    final int year = Integer.parseInt(csvPosBean2.getValueDate().substring(6));

                    jdate = JDate.valueOf(year, month, day);
                    if (jdate == null) {
                        this.bResult = false;
                        this.logGen.incrementError();
                        this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                SOURCE,
                                this.file,
                                String.valueOf(this.logGen.getNumberTotal()),
                                "19",
                                "VALUE_DATE",
                                "",
                                strLine);
                    }
                } catch (final Exception e) {
                    this.bResult = false;
                    this.logGen.incrementError();
                    this.logGen.setErrorRequiredFieldNotPresentNotValid(
                            SOURCE,
                            this.file,
                            String.valueOf(this.logGen.getNumberTotal()),
                            "19",
                            "VALUE_DATE",
                            "",
                            strLine);
                    Log.error(this, e); // sonar
                }
                if (jdate == null) {

                } else {
                    localTrade.setSettleDate(jdate);
                    // We put the time to 00.00 hours (86340000 = (23hours *
                    // 60min * 60sec * 1000milisec) + (59min * 60sec *
                    // 1000milsec)).
                    localTrade.setTradeDate(jdate.getJDatetime(TimeZone.getDefault()).add(-86340000));

                    // We specify the PO as Counterparty, because we don't need
                    // this information to import the movement.

                    if (GenericValidations.validateLegalEntity(csvPosBean2.getProcessingOrg())) {
                        final LegalEntity legalEntity =
                                this.conn.getRemoteReferenceData().getLegalEntity(csvPosBean2.getProcessingOrg());
                        localTrade.setCounterParty(legalEntity);
                        localTrade.setRole(LegalEntity.PROCESSINGORG);

                        // We retrieve the Bond from the BOPositionAdjustment
                        // (retrieving the security).
                        final Bond bond = (Bond) productAdjustment.getSecurity();

                        // Quantity previously calculated.
                        localTrade.setQuantity(Double.parseDouble(csvPosBean2.getQtyNominalSec()));

                        // Global operations.
                        productAdjustment.setPrincipal(bond.getFaceValue());
                        productAdjustment.computeNominal(localTrade);

                        // Nominal Security Currency //ERROR?
                        final String qtyNomCcy = csvPosBean2.getQtyNominalSecCcy();

                        if ((qtyNomCcy != null)
                                && (!qtyNomCcy.equals(""))
                                && (LocalCache.getCurrencyDefault(qtyNomCcy) != null)) {
                            localTrade.setSettleCurrency(csvPosBean2.getQtyNominalSecCcy());
                            localTrade.setTradeCurrency(csvPosBean2.getQtyNominalSecCcy());
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                    SOURCE,
                                    this.fileName,
                                    String.valueOf(this.logGen.getNumberTotal()),
                                    "48",
                                    "QTY_NOMINAL_SEC_CCY",
                                    csvPosBean2.getIsin(),
                                    strLine);

                            this.bResult = false;
                        }

                        // Position Type
                        final String posType = csvPosBean2.getPosType();
                        if ((posType != null)
                                && (!posType.equals(""))
                                && ((posType.equals(BUYSELL))
                                || (posType.equals(REPO))
                                || (posType.equals(LOAN))
                                || (posType.equals(PLEDGED)))) {
                            localTrade.setAdjustmentType(
                                    this.hashAdjusType.get(csvPosBean2.getPosType().toUpperCase()));
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                    SOURCE,
                                    this.fileName,
                                    String.valueOf(this.logGen.getNumberTotal()),
                                    "50",
                                    "POSITION_TYPE",
                                    csvPosBean2.getIsin(),
                                    strLine);

                            this.bResult = false;
                        }

                        // Reusable
                        if ((csvPosBean2.getReusable() != "") && (null != csvPosBean2.getReusable())) {
                            if (csvPosBean2.getReusable().equals("Y")) {
                                localTrade.setSubstitutableFlag(true);
                            } else {
                                localTrade.setSubstitutableFlag(false);
                            }
                        } else {
                            localTrade.setSubstitutableFlag(false);
                        }

                        final LegalEntity legEntityAgent =
                                this.conn.getRemoteReferenceData().getLegalEntity(csvPosBean2.getCustodian());

                        if (null != legEntityAgent) {

                            // Custodian
                            localTrade.setInventoryAgent(legEntityAgent.getAuthName());

                            // Account
                            localTrade.setAccountNumber(csvPosBean2.getAccount());
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresentNotValid(
                                    SOURCE,
                                    this.fileName,
                                    String.valueOf(this.logGen.getNumberTotal()),
                                    "36",
                                    "CUSTODIAN",
                                    csvPosBean2.getIsin(),
                                    strLine);

                            this.bResult = false;
                        }

                        // BO System
                        if (localTrade.getKeywords() == null) {
                            localTrade.setKeywords(new Hashtable<String, String>());
                        }

                        // control BO_SYSTEM
                        final String boSystem = csvPosBean2.getBoSystem();
                        if ((boSystem != null) && (!boSystem.equals(""))) {
                            localTrade.getKeywords().put(BO_SYSTEM, csvPosBean2.getBoSystem());
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresent(
                                    SOURCE,
                                    this.fileName,
                                    String.valueOf(this.logGen.getNumberTotal()),
                                    "7",
                                    "BO_SYSTEM",
                                    csvPosBean2.getIsin(),
                                    strLine);

                            this.bResult = false;
                        }

                        // Position Status
                        if ((csvPosBean2.getPosStatus().equals(THEORETICAL))
                                || (csvPosBean2.getPosStatus().equals(PENDING))
                                || (csvPosBean2.getPosStatus().equals(SETTLED))) {

                            final String positionType = csvPosBean2.getPosStatus().toUpperCase();
                            localTrade.addKeyword(POSITION_STATUS, positionType);
                            // GSM: 18/06/2013. Positions status keyword isn't
                            // inserting properly
                            // localTrade.getKeywords().put(POSITION_STATUS,
                            // this.hashPosType.get(csvPosBean2.getPosStatus().toUpperCase()));
                        } else {
                            this.logGen.incrementError();
                            this.logGen.setErrorRequiredFieldNotPresent(
                                    SOURCE,
                                    this.fileName,
                                    String.valueOf(this.logGen.getNumberTotal()),
                                    "X",
                                    "POSITION_STATUS",
                                    csvPosBean2.getIsin(),
                                    strLine);

                            this.bResult = false;
                        }

                        // Keyword to identify if the trade is to be canceled in
                        // the future or not.
                        localTrade.addKeyword(TRADE_TO_BE_CANCELED, tradeToBeCanceled);

                        // We specify the product for the new trade created.
                        localTrade.setProduct(productAdjustment);
                    } else {
                        // Do nothing.
                    }
                }
            } else {
                this.bResult = false;
                // warning number 17
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(
                        SOURCE,
                        this.fileName,
                        String.valueOf(this.logGen.getNumberTotal()),
                        "17",
                        "PORTFOLIO",
                        csvPosBean2.getPortfolio(),
                        csvPosBean2.getIsin(),
                        strLine);

                throw new Exception("Required field PORTFOLIO");
            }
        }

        return localTrade;
    }

    @Override
    public String getFileName() {
        return this.file;
    }

    public int countFileds(final String str, final char separator) {
        int counter = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == separator) {
                counter++;
            }
        }
        return counter;
    }

    private void getInventorySecPositions(JDate today, String booksCollQuery) throws RemoteException {
        StringBuilder where = new StringBuilder();
        StringBuilder from = new StringBuilder();
        where.append(" inv_secposition.internal_external = 'INTERNAL' ");
        where.append(" AND inv_secposition.date_type = 'TRADE' ");
        where.append(" AND inv_secposition.position_type = 'THEORETICAL'");
        where.append(" AND inv_secposition.security_id = product_desc.product_id");
        where.append(" AND product_desc.product_family = 'Bond'");
        if (!Util.isEmpty(booksCollQuery)) {
            where.append(" AND inv_secposition.book_id NOT IN (" + booksCollQuery + ")");
        }
        where.append(" AND inv_secposition.position_date = ");
        where.append(" ("); // BEGIN SELECT
        where.append(" select MAX(temp.position_date) from inv_secposition temp ");
        where.append(" WHERE inv_secposition.internal_external = temp.internal_external ");
        where.append(" AND inv_secposition.date_type = temp.date_type ");
        where.append(" AND inv_secposition.position_type = temp.position_type ");
        where.append(" AND inv_secposition.account_id = temp.account_id ");
        where.append(" AND inv_secposition.security_id = temp.security_id ");
        where.append(" AND inv_secposition.agent_id = temp.agent_id ");
        where.append(" AND inv_secposition.book_id = temp.book_id ");
        where
                .append(" AND TRUNC(temp.position_date) <= ")
                .append(com.calypso.tk.core.Util.date2SQLString(today));
        where.append(" )"); // END SELECT

        from.append("product_desc");

        InventorySecurityPositionArray secPositions =
                DSConnection.getDefault()
                        .getRemoteBackOffice()
                        .getInventorySecurityPositions(from.toString(), where.toString(), null);

        if (secPositions != null) {
            for (int i = 0; i < secPositions.size(); i++) {
                InventorySecurityPosition position = secPositions.get(i);
                if (position != null) {

                    String positionId =
                            position.getSecurityId()
                                    + ";"
                                    + position.getBookId()
                                    + ";"
                                    + position.getAgentId()
                                    + ";"
                                    + position.getAccountId();
                    this.hashPositions.put(positionId, position);
                }
            }
        }

        return;
    }

    @SuppressWarnings("rawtypes")
    private String[] generateData(final InventorySecurityPosition invSecPos) throws RemoteException {
        final String[] cancelValues = new String[14];

        Book bookCancel = invSecPos.getBook();
        // We check if the book in the file is in the DomainValues to exclude
        // it, because it's a Collateral Book.
        final Vector booksCollat = this.conn.getRemoteReferenceData().getDomainValues(BOOKS_COLLATERAL);
        if ((null != booksCollat) && !booksCollat.isEmpty() && (null != bookCancel)) {
            if (booksCollat.contains(bookCancel.getAuthName())) {
                return null;
            }
        }

        // BO_SYSTEM.
        cancelValues[0] = getAttribute(SOURCE_SYSTEM);

        bookCancel = invSecPos.getBook();
        if (null != bookCancel) {
            // Processing Org.
            cancelValues[1] = bookCancel.getLegalEntity().getAuthName();
            // Book.
            cancelValues[2] = bookCancel.getAuthName();
        }

        final Product product = invSecPos.getProduct();
        if (null != product) {
            // ISIN.
            cancelValues[3] = product.getSecCode(ISIN);
            // SEC_DESCR.
            cancelValues[4] = product.getDescription();
        }

        // VALUE_DATE.

        cancelValues[5] = this.valueDate.toString();
        // QTY_NOM.
        cancelValues[6] = QTY;
        // VALUE_NOM_QTY.
        cancelValues[7] = String.valueOf(0 - invSecPos.getTotal());
        // CURR.
        cancelValues[8] = invSecPos.getSettleCurrency();
        // POSITION_TYPE.
        cancelValues[9] = invSecPos.getPositionType();
        // ADJUSTMENT_TYPE.
        cancelValues[10] = BUYSELL;
        // REUSABLE.
        // TODO TO REVIEW
        cancelValues[11] = SUBSTITUTABLE;
        // CUSTODIAN.
        final LegalEntity agent = invSecPos.getAgent();
        if (null != agent) {
            cancelValues[12] = agent.getAuthName();
        }
        // ACCOUNT.
        final Account acc = invSecPos.getAccount();
        if (null != acc) {
            String accName = acc.getAuthName();
            if ((null != accName) && !Util.isEmpty(accName)) {
                int posAt = accName.indexOf("@");
                if (posAt >= 0) {
                    cancelValues[13] = accName.substring(0, posAt);
                }
            }
        }

        return cancelValues;
    }

    private void insertMovementToZero(final CSVPositionsBean csvPosBean2) throws Exception {
        Trade newTrade = null;
        BOPositionAdjustment boPosAdjust = null;

        // Fill the information for the product & trade.
        boPosAdjust = loadProductData(csvPosBean2);
        // We check if the security in the product is a Bond or not.
        if ((boPosAdjust != null) && (boPosAdjust.getSecurity() instanceof Bond)) {
            newTrade = loadTradeData(boPosAdjust, csvPosBean2);

            // Save the new Trade & Product.
            newTrade.setAction(Action.NEW);
            this.conn.getRemoteTrade().save(newTrade);
        }
    }

    @SuppressWarnings("unchecked")
    private BOPositionAdjustment loadProductData(final CSVPositionsBean csvPosBean2)
            throws RemoteException {
        final BOPositionAdjustment boPosAux = new BOPositionAdjustment();

        if (null != csvPosBean2) {
            // PO. We retrieve the ID for the PO passed as a parameter.
            final LegalEntity legalEntity =
                    this.conn.getRemoteReferenceData().getLegalEntity(csvPosBean2.getProcessingOrg());
            if (null != legalEntity) {
                boPosAux.setOrdererLeId(legalEntity.getId());
                boPosAux.setOrdererRole(LegalEntity.PROCESSINGORG);
            } else {
                throw new RemoteException(
                        "LegalEntity <" + csvPosBean2.getProcessingOrg() + "> doesn't exist");
            }

            // Security Description
            // final Product product =
            // this.conn.getRemoteProduct().getProductByCode(ISIN,
            // csvPosBean2.getIsin());
            String validateIsinReturn =
                    GenericValidations.validateIsin(
                            BOND, csvPosBean2.getIsin(), csvPosBean2.getQtyNominalSecCcy());

            if (validateIsinReturn.contains(PRODUCT_OK)) {
                final Vector<Product> products =
                        DSConnection.getDefault()
                                .getRemoteProduct()
                                .getProductsByCode(ISIN, csvPosBean2.getIsin());
                final Product product =
                        products.get(Integer.valueOf(validateIsinReturn.substring(PRODUCT_OK.length())));
                if (null != product) {
                    boPosAux.setSecurity(product);
                } else {
                    throw new RemoteException(
                            "Product <" + csvPosBean2.getIsin() + ">, specified in the file, doesn't exist");
                }

                // We fill all the fields for the BOPositionAdjustment object.
                boPosAux.setFlowType(SECURITY);
                boPosAux.setCurrencyCash(csvPosBean2.getQtyNominalSecCcy());
            }
        }

        return boPosAux;
    }

    @SuppressWarnings("unchecked")
    private Trade loadTradeData(
            final BOPositionAdjustment productAdjustment, final CSVPositionsBean csvPosBean2)
            throws Exception {
        final Trade localTrade = new Trade();

        if (csvPosBean2 != null) {
            // Portfolio
            final Book paramBook = this.conn.getRemoteReferenceData().getBook(csvPosBean2.getPortfolio());
            localTrade.setBook(paramBook);

            // Value Date
            final int day = Integer.parseInt(csvPosBean2.getValueDate().substring(0, 2));
            final int month = Integer.parseInt(csvPosBean2.getValueDate().substring(3, 5));
            final int year = Integer.parseInt(csvPosBean2.getValueDate().substring(6));
            localTrade.setSettleDate(JDate.valueOf(year, month, day));
            // We put the time to 00.00 hours (86340000 = (23hours * 60min *
            // 60sec * 1000milisec) + (59min * 60sec * 1000milsec)).
            localTrade.setTradeDate(
                    JDate.valueOf(year, month, day).getJDatetime(TimeZone.getDefault()).add(-86340000));

            // We specify the PO as Counterparty, because we don't need this
            // information to import the movement.
            final LegalEntity legalEntity =
                    this.conn.getRemoteReferenceData().getLegalEntity(csvPosBean2.getProcessingOrg());
            localTrade.setCounterParty(legalEntity);
            localTrade.setRole(LegalEntity.PROCESSINGORG);

            // We retrieve the Bond from the BOPositionAdjustment (retrieving
            // the security).
            final Bond bond = (Bond) productAdjustment.getSecurity();

            // Quantity previously calculated.
            localTrade.setQuantity(Double.parseDouble(csvPosBean2.getQtyNominalSec()));

            // Global operations.
            productAdjustment.setPrincipal(bond.getFaceValue());
            productAdjustment.computeNominal(localTrade);

            // Nominal Security Currency
            localTrade.setSettleCurrency(csvPosBean2.getQtyNominalSecCcy());
            localTrade.setTradeCurrency(csvPosBean2.getQtyNominalSecCcy());

            // Position Type
            localTrade.setAdjustmentType(this.hashAdjusType.get(csvPosBean2.getPosType().toUpperCase()));

            // Reusable
            if ((csvPosBean2.getReusable() != "") && (null != csvPosBean2.getReusable())) {
                if (csvPosBean2.getReusable().equals(SUBSTITUTABLE)) {
                    localTrade.setSubstitutableFlag(true);
                } else {
                    localTrade.setSubstitutableFlag(false);
                }
            } else {
                localTrade.setSubstitutableFlag(false);
            }

            // Custodian
            localTrade.setInventoryAgent(csvPosBean2.getCustodian());

            // Account
            localTrade.setAccountNumber(csvPosBean2.getAccount());

            // BO System
            if (localTrade.getKeywords() == null) {
                localTrade.setKeywords(new Hashtable<String, String>());
            }
            if (null != csvPosBean2.getBoSystem()) {
                localTrade.getKeywords().put(BO_SYSTEM, csvPosBean2.getBoSystem());
            }

            // Position Status
            if (null != csvPosBean2.getPosStatus()) {
                localTrade
                        .getKeywords()
                        .put(POSITION_STATUS, this.hashPosType.get(csvPosBean2.getPosStatus().toUpperCase()));
            }

            // Keyword to identify if the trade is to be canceled in the future
            // or not.
            // localTrade.addKeyword(TRADE_TO_BE_CANCELED,
            // String.valueOf(false));
        }

        // We specify the product for the new trade created.
        localTrade.setProduct(productAdjustment);

        return localTrade;
    }

    public boolean isForbiddenPosition(InventorySecurityPosition invSecPos) {

        LegalEntity agent = invSecPos.getAgent();
        if ((agent != null) && !Util.isEmpty(agent.getAuthName())) {
            if ((agent.getAuthName().equals(NONE_AGENT)) || (agent.getAuthName().equals(NONE1_AGENT))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("rawtypes")
    public void setPositionsToZero() {
        if (this.hashPositions.size() > 0) {
            Collection positionsToZero = this.hashPositions.values();
            Iterator it = positionsToZero.iterator();
            while (it.hasNext()) {
                InventorySecurityPosition posToZero = (InventorySecurityPosition) it.next();
                if (!isForbiddenPosition(posToZero)) {
                    try {
                        // If we have any value in the array to clean the old
                        // positions, we populate the fields and
                        // create the movement.
                        final String[] cancelValues = generateData(posToZero);
                        if (null != cancelValues) {
                            final CSVPositionsBean positionsToCancel = new CSVPositionsBean(cancelValues);
                            // We put this condition to avoid
                            // NullPointerExceptions creating the
                            // BOPositionAdjustment.
                            if (positionsToCancel.getIsin() != null) {
                                insertMovementToZero(positionsToCancel);
                            }
                        }
                    } catch (final Exception ex) {
                        Log.error(ex, "Error setting positions to zero.");
                        this.bResult = false;
                    }
                }
            }
        }
    }
}
